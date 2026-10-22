# spec: security-android

> Status: draft | Date: 2026-10-14 | Evidence: doc/research-report.md §3.2、§3.8、§4.2.a–c、§4.4、§6.2（调研依据 nexvault-rn_dim05、dim08、dim09）

## 1. 目标（Goal 与范围）

**Goal**：Android 端密钥静态保护（Keystore AES-256-GCM wrap，StrongBox→TEE 降级，per-use 生物识别）+ 端上完整性/环境检测（签名校验、JS bundle 自检、Root/隐藏框架检测、反调试、Frida 多信号）+ 设备证明双轨（Play Integrity 主 / Key Attestation 降级）+ 分级处置 L0–L5。

范围内：
- wrap key 生命周期（生成/降级/失效恢复）与 k1 密文 blob 格式
- 检测信号采集（native，集中在混淆 sealed 模块）与 fail-closed 门控输出（供 vault-core `E_VAULT_SECURITY_GATE`）
- Play Integrity Standard/Classic 双路径客户端采集 + 服务端 verdict 消费接口约定
- L0–L5 分级处置矩阵的端上执行

Out of scope：
- 服务端 verdict 验证服务实现（本文件只定接口契约）
- Samsung Blockchain Keystore 增强、o-mvll 混淆、商用加固壳（Roadmap）
- iOS 对应物（spec: security-ios）、TLS pinning（dim10 定案 @bam.tech/react-native-app-security，ui-wallet/网络层规格承接）

## 2. 接口定义

### 2.1 密钥包裹子模块（TS 面）

```ts
// modules/security-android/index.ts（模块目录名与 spec 文件名一致）
export type SecurityLevel = 'STRONGBOX' | 'TEE' | 'SOFTWARE_DEBUG'; // SOFTWARE 仅模拟器/debug
export interface WrapKeyInfo {
  alias: string;                 // 'nexvault.wrap.v1'，别名含版本号支持轮换
  securityLevel: SecurityLevel;
  strongBoxBacked: boolean;      // KeyInfo.isInsideSecureHardware() 校验值
  attested: boolean;             // 生成时是否带 attestation challenge
}
export function getCapabilities(): Promise<{
  hasStrongBox: boolean; hasGms: boolean; isDeviceSecure: boolean; isEmulator: boolean;
}>;
export function ensureWrapKey(opts?: { attestationChallenge?: ArrayBuffer }): Promise<WrapKeyInfo>;
/** k1 密文 blob：version(1B) ‖ IV(12B) ‖ ciphertext ‖ GCM tag；AAD = 账户地址 ‖ userId */
export function wrapKey(plaintextKey: ArrayBuffer, aad: ArrayBuffer): Promise<ArrayBuffer>;   // 触发 BiometricPrompt(CryptoObject)
export function unwrapKey(blob: ArrayBuffer, aad: ArrayBuffer): Promise<ArrayBuffer>;         // 触发 BiometricPrompt(CryptoObject)，明文仅驻留 native 签名窗口
export function deleteWrapKey(): Promise<void>;
```

> **2026-10-22 T3.1 交付偏差（owner D1/D2 批准）**：§2.1 的 TS 面为 T3.2 目标；T3.1 只交付最小 JS 面（`getCapabilities` / `ensureWrapKey` / `deleteWrapKey`），`wrapKey`/`unwrapKey` 为 native-internal（T3.2 与 vault-core 耦合后上移，见 `specs/features/2026-10-22-T3.1-keystore-wrap-biometric/`）。实现值：`SecurityLevel = STRONGBOX | TRUSTED_ENVIRONMENT | SOFTWARE_DEBUG`（旧写法 `TEE` 即 `TRUSTED_ENVIRONMENT`）；`strongBoxBacked` = 生成路径来源（StrongBox 尝试成功），不是 KeyInfo 读取——keystore2 只对私钥暴露 KeyInfo（2026-10-22 设备实证），AES 密钥的硬件归属不可事后查询，`TRUSTED_ENVIRONMENT` 是**未验证下限（unverified floor）而非 attestation 断言**：软件后端的真实设备也可能被标为 TEE（D9 裁定 a：接受来源模型），验证属 T5.x attestation。**2026-10-22 设备发现**：Pixel 6a / Android 17 的 StrongBox per-use AES 密钥可生成，但每次带认证的使用都在 Titan M2 KeyMint（citadel）失败（`KEY_USER_NOT_AUTHENTICATED`，StrongBox 要求硬件 auth token）；T3.1 的 wrap key 因此走 **TEE** 路径（§2.4「StrongBox 优先」的偏差，StrongBox 复测入 T5.x Handoff）。`getCapabilities.hasGms/isEmulator` 与 `ensureWrapKey(opts.attestationChallenge)` 属 T3.2/T5.x。

### 2.2 完整性/环境检测子模块（TS 面，全部异步、结果入 SecurityStateCache）

```ts
export type RiskLevel = 'L0' | 'L1' | 'L2' | 'L3' | 'L4' | 'L5';
export interface DetectionReport {
  signatureMatch: boolean;        // native 签名块自解析证书 SHA-256 == 编译期嵌入指纹
  bundleHashMatch: boolean;       // assets/index.android.bundle SHA-256 自检
  rootSignals: string[];          // 命中的向量 ID（不落明文细节）
  fridaSignals: string[];
  debuggerAttached: boolean;
  evaluatedAt: number;
}
export function runDetection(scope: 'fast' | 'full'): Promise<DetectionReport>; // fast=首帧后最小集；full=后台
export function getCachedRiskLevel(): RiskLevel;          // 热路径只读缓存，<1ms
/** fail-closed 门控：供 vault-core 在 importMnemonic/signDigest 前调用；不通过则不释放密钥材料 */
export function assertSigningAllowed(): Promise<void>;    // 失败抛 E_SEC_GATE_DENIED
```

### 2.3 设备证明（TS 面）

```ts
export function requestIntegrityTokenStandard(requestHash: string): Promise<string>;   // 日常校验
export function requestIntegrityTokenClassic(serverNonce: string): Promise<string>;    // 高价值操作
export function generateAttestedDeviceKey(serverNonce: string): Promise<{
  publicKeyPem: string; certificateChainBase64: string[];                              // Key Attestation 降级路径
}>;
```

### 2.4 Kotlin 侧关键签名

```kotlin
object WrapKeyManager {
  fun ensureKey(challenge: ByteArray?): WrapKeyInfo            // StrongBoxUnavailableException → TEE 重试；绝不静默落软件
  fun wrap(plain: ByteArray, aad: ByteArray, crypto: BiometricPrompt.CryptoObject): ByteArray
  fun unwrap(blob: ByteArray, aad: ByteArray, crypto: BiometricPrompt.CryptoObject): ByteArray // 用完 Arrays.fill(0)
}
// KeyGenParameterSpec 四联 flag：
// setUserAuthenticationRequired(true)
// setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)  // 窗口=0，per-use；D6 裁定生物识别-only
//   （2026-10-22：设备凭据成功的 cryptoObject 为 null，PIN 回退无法 unwrap，故不纳入）
// setInvalidatedByBiometricEnrollment(true)
// setIsStrongBoxBacked(true)  // 失败降级 TEE
// setUnlockedDeviceRequired(true) 仅 API ≥35；API 28–34 因官方明示 bug 默认关 [^2^]
internal object SignatureSelfCheck { fun verifyApkSignatureEmbedded(): Boolean }   // .so 内，不经 PackageManager
internal object BundleSelfCheck { fun verifyHermesBundleHash(): Boolean }
```

### 2.5 错误码表

| code | 含义 |
|---|---|
| `E_SEC_NO_LOCKSCREEN` | 无安全锁屏（user-auth key 生成前置条件不满足）[^1^] |
| `E_SEC_STRONGBOX_UNAVAILABLE` | StrongBox 生成失败（内部自动 TEE 降级；仅遥测用，不向 JS 冒泡为失败） |
| `E_SEC_KEY_INVALIDATED` | `KeyPermanentlyInvalidatedException`：生物特征变更/锁屏移除 → 走助记词重派生恢复流 |
| `E_SEC_BIOMETRIC_CANCELLED` / `E_SEC_BIOMETRIC_LOCKOUT` | 用户取消 / 生物识别锁定 |
| `E_SEC_SOFTWARE_KEYSTORE` | 仅软件后端（模拟器/debug）；生产包禁止 |
| `E_SEC_SIG_MISMATCH` / `E_SEC_BUNDLE_MISMATCH` | 签名/JS bundle 自检失败 |
| `E_SEC_GATE_DENIED` | fail-closed 门控拦截签名材料释放 |
| `E_SEC_INTEGRITY_UNAVAILABLE` | Play Integrity 不可用（无 GMS/网络）→ 降级 Key Attestation，不阻断读操作 |
| `E_SEC_UNSUPPORTED_OPERATION` | Web 端调用 |
| `E_SEC_BLOB_CORRUPT` | wrap blob 版本/长度/GCM tag/AAD 校验失败（零明文输出）；2026-10-22 T3.1 补登（实现已在用；TC-SA-010/011/013） |

## 3. 威胁模型

**资产**：B1 k1 私钥密文 blob；B2 wrap key（Keystore 内）；B3 应用完整性（防重打包）；B4 检测裁决可信度。

| # | 攻击向量 | 防御机制 | 级别 |
|---|---|---|---|
| T-SA-1 | 静态提取 k1 明文（脱库/备份） | Keystore AES-256-GCM wrap（StrongBox 优先 [^3^]）；blob 仅密文落私有目录；`allowBackup=false` | [P]（非 root 场景） |
| T-SA-2 | 「不可导出但可被调用」：恶意代码以 app 身份调 Keystore 签名 [^4^] | per-use 生物绑定（窗口=0，BiometricPrompt+CryptoObject）+ `setInvalidatedByBiometricEnrollment(true)`；新增生物特征即密钥失效 | **[P]**（密钥使用与真实用户在场密码学绑定） |
| T-SA-3 | apktool/MT Manager 重打包自签分发 | native 签名块自解析（EOCD→"APK Sig Block 42"→ID 0x7109871a/0xF05368C0→首个 signer X.509→SHA-256 与 .so 内嵌指纹比对）+ fstat inode 与 /proc/self/maps 交叉校验防 open hook 重定向 [^5^][^6^] | [C]（对工具党近似 [P]） |
| T-SA-4 | Hermes bundle 篡改（hbctool/hermes-decomp）| JS bundle SHA-256 自检（native 读 assets，编译期嵌入哈希，两段构建回填）；v2/v3 整包签名联动兜底 [^7^] | [C] |
| T-SA-5 | Root 隐藏栈：Magisk DenyList / Zygisk Next / Shamiko / SUSFS(SukiSU) [^8^] | 自研 native 检测（不引 RootBeer 作唯一依据）：mounts/mountinfo 多源交叉、mount/PID namespace 失配、syscall errno 偏离、APatch /debug_ramdisk 特征 [^9^]；检测聚合于混淆 sealed 模块，命中即 fail-closed、不日志不提示 | [C]（LKM 级隐藏承认漏报） |
| T-SA-6 | ptrace/gdb 调试 | TRACEME self-attach + TracerPid + task stat 组合；检测点分散、结果污染化（不直接 kill，避免给攻击者精准 hook 点 [^10^]） | [C] |
| T-SA-7 | Frida 插桩 | 多信号并发交叉计分：端口 27042/D-Bus、maps/fd/线程名、rwxp 段、**inline hook 序言字节自检**（ARM64 `B`=0x14xxxxxx）[^11^] | [C] |
| T-SA-8 | attestation 中继（root 机转发 challenge 到干净设备）[^12^] | 服务端校验 `attestationApplicationId` + 会话内持续签名质询 + 同设备地理/频率聚类 | [C] |
| T-SA-9 | Play Integrity 绕过（Tricky Store/keybox）或停机 | 双轨：Key Attestation 降级（无频限、可脱 GMS）[^13^]；verdict 仅作风险信号，密钥面 fail-closed 兜底；STRONG 不硬卡（Android 13+ 需 12 个月内补丁，防误伤）[^14^] | [C] |

**L0–L5 分级处置**（动作面：A 只读浏览 / B 收款地址展示 / C 签名交易 / D 导出密钥 / E 改安全设置）[^15^]：

| 级别 | 信号 | A | B | C | D | E |
|---|---|---|---|---|---|---|
| L0 绿 | STRONG + PLAY_RECOGNIZED + LICENSED | ✅ | ✅ | ✅ | ✅ | ✅ |
| L1 绿- | DEVICE 无 STRONG / Key Attestation 通过（TEE+locked+VERIFIED） | ✅ | ✅ | ✅+二次确认 | ✅+密码+延时 | ✅+二次确认 |
| L2 黄 | 仅 BASIC / Software 级 attestation / 服务瞬时不可用 | ✅+横幅 | ✅ | ⚠️限额+额外认证 | ❌ | ❌ |
| L3 橙 | UNRECOGNIZED_VERSION / appAccessRisk 检出 CAPTURING/CONTROLLING/OVERLAYS | ✅+强警告 | ✅ | ❌ | ❌ | ❌ |
| L4 红 | 空 verdict（root+hooking 迹象）/ HIGH_RISK / rootOfTrust=unlocked | ✅ 只读 | ⚠️标红提醒 | ❌ | ❌ | ❌ |
| L5 服务不可用 | Play/网络不可达且重试失败 | ✅ | ✅ | ⚠️限额+队列复核 | ❌ | ❌ |

冷钱包特别条款：链上交易不可撤回，C/D/E 列是真正资产防线；L4 下不删密钥、不硬退出（保留用户自救通道），但 Keystore 密钥使用被门控（`E_SEC_GATE_DENIED`）；全部判定写风控台账；**判决与 token 验签永远在服务端** [^16^]。

## 4. 验收标准

| AC | 内容 | 绑定用例 | 验证方式 |
|---|---|---|---|
| AC-1 | wrap key 生成：StrongBox 设备 `strongBoxBacked=true`；无 StrongBox 设备降级 TEE 且 `isInsideSecureHardware()==true`；模拟器仅 SOFTWARE_DEBUG 且界面有「测试环境」水印 | TC-SA-001 | 真机走查（Pixel 6a + 一台仅 TEE 中端机）+ 模拟器 |
| AC-2 | per-use 绑定：连续两次 unwrap 均触发 BiometricPrompt；新增指纹后旧 key 抛 `E_SEC_KEY_INVALIDATED`，恢复流可从助记词重派生重建 | TC-SA-002 | 真机走查 |
| AC-3 | `setUnlockedDeviceRequired` 门控：API≥35 开；API 28–34 关（构建变体断言）；无锁屏设备 ensureWrapKey 抛 `E_SEC_NO_LOCKSCREEN` | TC-SA-003 | 多 API 模拟器矩阵（28/34/35）instrumentation |
| AC-4 | 重打包检测：apktool 重打包+自签的 APK 启动后 `signatureMatch=false` 且签名材料不释放 | TC-SA-004 | CI（重打包流水线脚本）+ 真机走查 |
| AC-5 | bundle 篡改检测：改动 index.android.bundle 1 字节重签名后自检失败 | TC-SA-005 | CI |
| AC-6 | 检测向量冒烟：Magisk+Shamiko 隐藏环境下至少命中 mount namespace/PID namespace 类向量之一；结果不产生用户可见提示、不产生密钥材料 | TC-SA-006 | 真机走查（root 测试机） |
| AC-7 | Play Integrity 双路径：Standard 返回 token 含 requestHash；Classic 含服务端 nonce；无 GMS 模拟器走 Key Attestation 降级且不阻断只读 | TC-SA-007 | 真机 + 模拟器走查（服务端用 stub 验证器） |
| AC-8 | L 级处置：注入 mock verdict 逐级验证 L2 限额、L3 拦 C/D/E、L4 只读、L5 fail-open 读/fail-closed 写 | TC-SA-008 | detox E2E（mock 层） |
| AC-9 | 检测不阻塞首屏：首帧渲染完成前无文件扫描/网络 attestation（trace 证据） | TC-SA-009 | Perfetto trace 真机采集 [^17^] |

## 5. 测试用例

### TC-SA-001 能力探测与降级矩阵
```gherkin
Given 三台设备：Pixel（StrongBox）/ 中端 TEE 机 / 模拟器
When 调用 getCapabilities() 与 ensureWrapKey()
Then 分别得到 STRONGBOX / TEE / SOFTWARE_DEBUG 三级 WrapKeyInfo
And 任何路径不静默落入软件级（SOFTWARE_DEBUG 仅 debug build 可得）
```

### TC-SA-002 生物识别失效与恢复
```gherkin
Given 已创建 wrap key 且完成一次 wrap
When 用户在系统设置新增一枚指纹
Then 下一次 unwrapKey 抛 E_SEC_KEY_INVALIDATED
And App 进入「密钥失效」恢复流：重新 importMnemonic（vault-core）→ 重新 wrap
And 全程界面无密钥明文显示
```

### TC-SA-003 setUnlockedDeviceRequired 版本门控
```gherkin
Given API 28 / 34 / 35 三个模拟器实例
When ensureWrapKey()
Then API 35 生成参数的 KeyInfo 含 unlockedDeviceRequired=true；API 28/34 为 false
And 无安全锁屏的实例上抛 E_SEC_NO_LOCKSCREEN（不生成密钥，防 API 12–34 已知 bug 丢密钥=丢资产 [^2^]）
```

### TC-SA-004 重打包负例
```gherkin
Given CI 对 release APK 执行 apktool d → 改 1 处资源 → b → 随机密钥自签
When 安装并启动该 APK
Then runDetection('fast') 返回 signatureMatch=false
And vault-core.signDigest 前置门控抛 E_SEC_GATE_DENIED（fail-closed）
```

### TC-SA-005 bundle 篡改负例
```gherkin
Given CI 修改 assets/index.android.bundle 任意 1 字节并重签名（绕过签名校验的对照组）
When 启动后执行 BundleSelfCheck
Then bundleHashMatch=false 且风险等级 ≥L3
```

### TC-SA-006 Root 隐藏环境检测
```gherkin
Given 安装 Magisk + Zygisk + Shamiko 并启用隐藏的测试机
When runDetection('full')
Then rootSignals 至少命中 mounts/mountinfo 失配或 PID namespace 失配之一
And 无任何用户可见「已 root」提示（对抗性静默）；判定仅进入风险台账与门控
```

### TC-SA-007 设备证明双路径
```gherkin
Given GMS 真机与无 GMS 环境
When 分别执行 Standard(requestHash) / Classic(nonce) / generateAttestedDeviceKey(nonce)
Then GMS 机两 token 均可被 stub 服务端验出 requestHash/nonce 绑定
And 无 GMS 环境返回 E_SEC_INTEGRITY_UNAVAILABLE 并自动进入 Key Attestation 证书链输出
```

### TC-SA-008 L 级处置矩阵（detox）
```gherkin
Given mock verdict 注入层
When 依次注入 L1/L2/L3/L4/L5 信号
Then UI 动作面符合 §3 矩阵（L2 限额弹二次认证；L3 起 C/D/E 按钮置禁用态；L4 仅只读页可达）
```

### TC-SA-009 首屏零阻塞（trace）
```gherkin
Given 冷启动 Perfetto trace
When 检查首帧前主线程与后台线程
Then 无文件 I/O 扫描、无网络 attestation 调用；首个检测任务出现在首帧之后
```

### TC-SA-010 wrap blob 结构（JVM）

```gherkin
Given 软件 AES-256-GCM 测试密钥与固定 IV
When AesGcmEnvelope.seal/open 一轮
Then blob = version(1B)=1 ‖ IV(12B) ‖ ciphertext ‖ tag(16B)，IV 与注入值一致
And 版本字节错误或截断 blob 抛 E_SEC_BLOB_CORRUPT
```

### TC-SA-011 wrap 完整性（AAD 绑定 / IV 唯一 / 篡改负例，JVM）

```gherkin
Given 同一 plaintext 与 AAD
When 两次 seal（随机 IV）后分别 open
Then 两次密文不同、roundtrip 字节一致
And AAD 篡改或 ciphertext/tag 翻转 1 字节均抛 E_SEC_BLOB_CORRUPT 且零明文输出
And 0 字节明文可 roundtrip（29 字节 blob，与 iOS TC-SI-010 同布局）
```

### TC-SA-012 wrap 输入清零（JVM）

```gherkin
Given 32 字节 plaintext 缓冲区
When seal 返回
Then 输入缓冲区全 0（AC-7；unwrap 输出由 native 签名窗口负责清零）
```

### TC-SA-013 错误模板白名单（JVM + 真机）

```gherkin
Given 七个 E_SEC_* 错误
When 构造并检查 code/message
Then code 与 §2.5 一致，message 为固定模板、不含密钥/AAD/密文片段
```

### TC-SA-014 设备级 wrap key 生命周期与门禁（instrumentation + MAN-019）

```gherkin
Given Pixel 6a（安全锁屏 + 已录入指纹）
When ensureWrapKey → wrap/unwrap（BiometricPrompt CryptoObject）
Then securityLevel ∈ {STRONGBOX, TRUSTED_ENVIRONMENT}；roundtrip 字节一致
And 连续 3 次错误指纹 fail-closed（E_SEC_BIOMETRIC_CANCELLED、无明文、下次重新弹窗）
And 删除后 encrypt/decrypt 抛 E_SEC_KEY_INVALIDATED
```

### TC-SA-015 生物特征变更失效（instrumentation + MAN-020）

```gherkin
Given 已创建 wrap key
When 系统设置新增 1 枚指纹后再初始化加密 cipher
Then 抛 E_SEC_KEY_INVALIDATED（KeyPermanentlyInvalidatedException）
And delete + ensure 恢复硬件级密钥
```

## 6. 依赖与红线

- **R1 + R6（本模块核心）**：安全判定 fail-closed——检测不通过则不释放签名材料（vault-core 门控 `E_SEC_GATE_DENIED`），而非「检测到就弹窗」；假设一切客户端判定可被伪造。这是 R1「密钥材料不出 native」在门控上的执行形态，也是 AGENTS.md R6 口径下唯一 [P] 级端上控制（cross_verification 全局一致性 #1），其余检测一律 [C]，禁止拔高（**R6**）。
- **R5**：检测命中不落可读日志、不弹技术性提示；密钥材料永不未加密落盘；wrap key 能力不可用时界面显示「Not configured」，禁止造假可用态。
- **R4**：不引入 RootBeer 作为唯一检测依据（向量陈旧 [^8^]）；新增任何检测/Keystore 库视为修宪；`@expo/app-integrity` 为 alpha，锁版本使用 [^18^]。
- **R1 联动**：unwrap 明文仅存在于 native 签名窗口，unwrap→sign→清零由 vault-core 同一调用内完成，JS 不可见明文。
- **R2 联动**：本模块产出的 attestation/integrity 证据随 OTA 发布与更新通道的安全叙事一致（OTA 包本身必须代码签名）。
- 门控纪律：L4 不删密钥不硬退出（防误杀与对抗升级）；恢复流（助记词重派生）必须始终可用。
- 流程纪律：双侧验证（自动+真机走查）；Agents 不得 commit/push。

## 7. Roadmap 项（3 天外）

1. APK v3 lineage（密钥轮换）与多 signer 支持；Play App Signing 双指纹白名单（上传密钥 vs Google 分发证书）[^19^]。
2. o-mvll 控制流混淆 + 符号剥离（仅检测/密钥 so）；「只 VMP 1–2 个核心裁决函数」，启动代价 <100ms；商用壳（DexGuard ~$44k/年 / Promon post-compile）作触发条件制升级项 [^20^]。
3. Key Attestation 华为海外版实机覆盖矩阵；HarmonyOS NEXT（无 AOSP）另行评估（HMS SafetyDetect 或放弃该档）。
4. deviceRecall（beta）与 recentDeviceActivity 纳入风控台账。
5. Samsung Blockchain Keystore 可选增强（TEE 内 secp256k1，仅指定 Galaxy 机型，供应商锁定风险明示）。
6. Play Console 提额申请与配额告警；上线后 ≥2 周只观测 verdict 分布再开强制策略。

---
### 来源
[^1^]: Android API Reference — KeyGenParameterSpec.setUserAuthenticationRequired（锁屏移除即密钥不可逆失效）。
[^2^]: developer.android.com KeyGenParameterSpec.Builder —「Be careful using setUnlockedDeviceRequired(true) on Android 14 (API 34) and lower…bugs existed in Android 12 through 14」。
[^3^]: PoPETS 2026（arXiv:2507.07927）— 45% Android 设备具备 SE；32B 载荷远低于 StrongBox ~0.2MiB 阈值。
[^4^]: Critical Security — Revolut Business 绕过演示（未把密钥密码学绑定到生物识别的实现缺陷）。
[^5^]: 看雪 thread-278982 / E2（dim09）— signing block 自解析完整 C 实现；Talsec — v2=0x7109871a、v3=0xF05368C0。
[^6^]: Guardsquare — hook libc open 重定向到 assets 内原包的标准绕过；fstat inode 交叉校验防御（dim09 E3/E4）。
[^7^]: SymbioticSec hermes-decomp（v40–v99）/ HackTricks hbctool 重打包 — Hermes=明文与 bundle 篡改实证。
[^8^]: LRFP-Team 编年 — 2026 对抗栈 SukiSU-Ultra+Zygisk Next+SUSFS；RootBeer README「no root detection code can ever be 100% effective」。
[^9^]: dim09 E12/E13 — Shamiko 下 mount namespace 失配/PID namespace 仍有效；APatch /debug_ramdisk tmpfs 命中特征。
[^10^]: kiwisec 梆梆企业版绕过复盘 — 固定点位+标准 syscall=精准 hook 点；检测点分散、结果污染化。
[^11^]: AppsOnAir RASP — ARM64 被 hook 函数序言以 `B`（0x14xxxxxx）开头为一阶可靠信号；brida 重命名废掉名字类检测（dim09 E21/E22）。
[^12^]: Quarkslab 2026 — attestation relay 攻击演示；缓解=校验 attestationApplicationId+会话内签名质询。
[^13^]: GrapheneOS attestation compatibility guide — Key Attestation 无频限、不依赖 Google 在线服务。
[^14^]: Android Developers Blog 2024-12 — Android 13+ 的 STRONG 追加 12 个月内安全补丁要求。
[^15^]: OWASP MASVS-RESILIENCE-1（alert 或 terminate 皆合规）+ OneSpan/Verihubs 分级实践（dim08 §3 决策表）。
[^16^]: agiledroid / dim08 C28 — 执行决策在服务端，客户端拦截响应无法改判。
[^17^]: Bugsnag changelog 2026-10-06 —「Moved root/jailbreak checks off the startup path and cached the result」。
[^18^]: docs.expo.dev/versions/latest/sdk/app-integrity —「currently in alpha and will frequently experience breaking changes」。
[^19^]: MASTG-KNOW-0003 — 证书 pin 必须考虑 lineage 而非单一证书哈希，否则合法轮换误杀。
[^20^]: Vendr — DexGuard 成交均值 ~$44k/年；kiwisec 2026 横评 — 梆梆冷启动 +800–1500ms（排除理由）。
