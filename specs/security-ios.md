# spec: security-ios

> Status: draft | Date: 2026-10-14 | Evidence: doc/research-report.md §3.3、§3.8、§4.2.d–f（调研依据 nexvault-rn_dim06、dim08、dim09、dim07）

## 1. 目标（Goal 与范围）

**Goal**：iOS 端密钥静态保护（Keychain 生物识别 ACL + AES-256-GCM 包裹 k1）、越狱/调试/插桩检测（[C] 级诚实标注）、App Attest 设备证明（+DeviceCheck 兜底）。与 vault-core 的运行时闭环配合：vault-core 管「使用中的密钥」，本模块管「静止的密钥」与「环境可信度」。

范围内：
- expo-secure-store 落盘包裹（Keychain 条目 + 生物识别 ACL，ACL 校验由 Secure Enclave 硬件强制 [^1^]）
- AES-256-GCM wrap/unwrap k1（随机 32B KEK 入 Keychain，密文可存文件系统）
- IOSSecuritySuite + 自补向量的越狱检测（Dopamine rootless `/var/jb` 路径族）
- PT_DENY_ATTACH 反调试（审核安全形态）
- App Attest（@expo/app-integrity，alpha 锁版）+ DeviceCheck 兜底
- 与 Android 一致的 L0–L5 分级处置语义

Out of scope：
- Secure Enclave 内 secp256k1 签名（SE 仅 P-256 且不可导入外部密钥，物理上不可能 [^2^]）
- SE P-256 包裹增强层（v2，~50 行 Swift 薄模块，`SecKeyCreateRandomKey(kSecAttrTokenIDSecureEnclave)`）
- react-native-keychain 迁移（该库 10.0.0 后停滞 ~19 个月、存在未修复生物识别 ACL issue #783，不作关键路径 [^3^]）

## 2. 接口定义

### 2.1 TS 面

```ts
// modules/security-ios/index.ts（模块目录名与 spec 文件名一致；API 与 security-android 对齐，平台分叉实现）
export interface KeychainWrapInfo {
  backend: 'KEYCHAIN_BIOMETRIC_ACL';      // MVP 唯一后端
  aclEnforcedBySecureEnclave: true;       // 生物识别 ACL 校验由 SE 硬件强制
}
export function ensureKek(): Promise<KeychainWrapInfo>;          // 生成/确认随机 32B KEK 已入 Keychain（ACL: requireAuthentication + WHEN_UNLOCKED_THIS_DEVICE_ONLY）
export function wrapKey(plaintextKey: ArrayBuffer, aad: ArrayBuffer): Promise<ArrayBuffer>;  // AES-256-GCM，触发 Face ID/密码
export function unwrapKey(blob: ArrayBuffer, aad: ArrayBuffer): Promise<ArrayBuffer>;        // 同上；明文仅驻留 native 签名窗口
export function deleteKek(): Promise<void>;

export function runDetection(scope: 'fast' | 'full'): Promise<{
  jailbreakSignals: string[]; debuggerAttached: boolean; fridaSignals: string[]; evaluatedAt: number;
}>;
export function getCachedRiskLevel(): 'L0'|'L1'|'L2'|'L3'|'L4'|'L5';
export function assertSigningAllowed(): Promise<void>;           // fail-closed 门控，失败抛 E_SEC_GATE_DENIED

export function attestDevice(serverChallenge: ArrayBuffer): Promise<{ keyId: string; attestationBase64: string }>; // App Attest，每装一次
export function generateAssertion(serverChallenge: ArrayBuffer, payloadHash: ArrayBuffer): Promise<string>;        // 敏感请求逐次
```

> **2026-10-22 T3.1 交付偏差（owner D2 批准）**：T3.1 的 JS 面仅 `ensureKek`/`deleteKek`（`src/index.ts`，expo-secure-store 57.0.4）；`wrapKey`/`unwrapKey` 为 native-internal Swift（CryptoKit AES.GCM + Keychain KEK），随 T3.2 的 vault-core 耦合上移。KEK 以 **hex 字符串**存于 expo-secure-store（`requireAuthentication: true` + `WHEN_UNLOCKED_THIS_DEVICE_ONLY`）；条目坐标取自安装依赖 `SecureStoreModule.swift`：service = `(keychainService ?? "app") + ":auth"`、account = `kSecAttrGeneric = Data(key.utf8)`。完整 KEK 走查（TC-SI-009）随 T3.2 JS 面，见 T3.1 Handoff。

### 2.2 Keychain 条目配置（expo-secure-store 调用面）

```ts
import * as SecureStore from 'expo-secure-store';
const OPTS: SecureStore.SecureStoreOptions = {
  requireAuthentication: true,                                  // 读取触发 Face ID/设备密码；生物特征变更自动失效 [^4^]
  keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY, // 阻断 iCloud/备份迁移；不用默认 AFTER_FIRST_UNLOCK
  // synchronizable 默认 false，禁止开启
};
```

约束：单值 ≤2048 字节硬上限（32B KEK 远低于限）[^4^]；KEK 读出后立即用于 native AES-GCM 并在同调用内清零；k1 密文 blob 格式 `version(1B) ‖ IV(12B) ‖ ciphertext ‖ tag` 与 Android 一致，AAD 绑定账户地址+userId。

### 2.3 Swift 侧关键签名（检测子模块，自研薄模块）

```swift
enum JailbreakDetector {
  static func collectSignals() -> [String]   // IOSSecuritySuite + 自补：/var/jb 路径族、dyld 可疑库
                                             // （systemhook.dylib/Ellekit/FridaGadget/Shadow）、sysctl、
                                             // kern.boottime 异常、bind mount 异常
  static func denyDebuggerAttach()           // dlsym(RTLD_DEFAULT, "ptrace")(PT_DENY_ATTACH=31)
                                             // 必须 dlsym 动态解析（直接链接受 App Store 审核风险）[^5^]
  static func isDebuggerAttached() -> Bool   // sysctl(KERN_PROC pid) 查 P_TRACED
}
```

### 2.4 错误码表

| code | 含义 |
|---|---|
| `E_SEC_IOS_AUTH_FAILED` / `E_SEC_IOS_AUTH_CANCELLED` | 生物识别/密码认证失败或取消 |
| `E_SEC_IOS_KEYCHAIN_INACCESSIBLE` | 设备锁定态读取 ThisDeviceOnly 条目 |
| `E_SEC_IOS_KEK_MISSING` | KEK 条目缺失（生物特征变更致失效）→ 走助记词重派生恢复流 |
| `E_SEC_IOS_BLOB_CORRUPT` | GCM tag 校验失败（密文被篡改） |
| `E_SEC_IOS_ATTEST_UNSUPPORTED` | `isSupported()==false`——本身视为风险信号 [^6^] |
| `E_SEC_IOS_ATTEST_FAILED` | Apple 服务端瞬时失败 → 优雅降级，不硬阻断 [^7^] |
| `E_SEC_GATE_DENIED` | fail-closed 门控拦截 |
| `E_SEC_UNSUPPORTED_OPERATION` | Web/模拟器无 SE 路径 |

## 3. 威胁模型

**资产**：C1 k1 密文 blob；C2 KEK（Keychain 内）；C3 检测裁决可信度；C4 App Attest key（SE 内，attestation 专用）。

| # | 攻击向量 | 防御机制 | 级别 |
|---|---|---|---|
| T-SI-1 | 越狱后 keychain_dumper 静态脱库 | KEK 条目带生物识别 ACL：ACL 校验由 Secure Enclave 硬件强制，攻击者需实时通过生物识别才能释放条目，无法静默离线 dump [^1^] | **[P]**（对静态脱库） |
| T-SI-2 | Frida hook `SecItemCopyMatching` 在用户解锁后截获明文 | 缩短明文窗口（unwrap→签名→清零同一 native 调用内完成，vault-core 闭环）；越狱检测信号进风险评分 | [C]（运行时钩挂为 RN 桥方案固有残余风险 [^8^]） |
| T-SI-3 | Dopamine 2.4+「Hide Jailbreak」（按需不注入 + 卸载 /var/jb，进程级「真干净」） | 诚实定位：客户端检测**必然漏报**，ISS 官方 TODO 亦未完成 hidejb 检测 [^9^]；越狱判定不作签名硬前置（误伤+被绕双输），仅作服务端风险评分输入；兜底=fail-closed 门控 + App Attest | [C]（隐藏模式下客户端无解） |
| T-SI-4 | 调试器附加 | PT_DENY_ATTACH（dlsym 动态解析）+ sysctl P_TRACED 配套检测 | [C]（二进制 patch/Frida 可绕 [^5^]） |
| T-SI-5 | 重打包/伪造客户端请求服务端 | App Attest：attestKey（每装一次，x5c 链至 Apple App Attest Root，RP ID=TeamID.BundleID）+ 敏感请求 generateAssertion（counter 防重放）；**fmt=apple-appattest 需专门服务端实现，@simplewebauthn/server 不能直接验** [^6^]；DeviceCheck 兜底 legacy | [C]（不证明系统未越狱；密钥面靠 fail-closed 兜底） |
| T-SI-6 | 内存取证/swap 泄露 | **iOS 无磁盘 swap**（内存压缩代替）；XNU mlock 沙盒语义无文档保证、商业 SDK 单源声称可用——**如实标注为未解决项**（cross_verification C6），补偿=短时驻留+显式清零+Keychain 落盘；真机实测列入 Roadmap | [C]，待真机验证 |
| T-SI-7 | 备份/迁移泄露 | `WHEN_UNLOCKED_THIS_DEVICE_ONLY` + synchronizable=false | [P] |

L0–L5 处置语义与 spec: security-android §3 完全一致（同一动作面矩阵）；iOS 侧 L0 判据替换为「attested key + assertion counter 正常」。App Attest 拒绝时按 Apple 官方建议优雅降级、限流监控，不硬阻断 [^7^]。

## 4. 验收标准

| AC | 内容 | 绑定用例 | 验证方式 |
|---|---|---|---|
| AC-1 | KEK 条目创建带 `requireAuthentication + WHEN_UNLOCKED_THIS_DEVICE_ONLY`；设备锁定态读取抛 `E_SEC_IOS_KEYCHAIN_INACCESSIBLE`；iCloud 备份后条目不随迁移 | TC-SI-001 | 真机走查（含锁屏态读取、备份恢复演练） |
| AC-2 | 生物特征变更（新增指纹/面容）后 unwrap 抛 `E_SEC_IOS_KEK_MISSING`，恢复流可从助记词重建 | TC-SI-002 | 真机走查 |
| AC-3 | 篡改 blob 任一字节 → GCM tag 校验失败抛 `E_SEC_IOS_BLOB_CORRUPT`，不输出任何明文 | TC-SI-003 | XCTest |
| AC-4 | Dopamine rootless（非隐藏模式）环境至少命中 /var/jb 或 dyld 向量之一；隐藏模式下漏报时风险评估正确降级为「信号缺失」而非「判定安全」 | TC-SI-004 | 真机走查（越狱测试机） |
| AC-5 | PT_DENY_ATTACH 经 dlsym 调用（CI grep 禁止直接链接 ptrace 符号）；lldb 附加被拒 | TC-SI-005 | CI grep + 真机走查 |
| AC-6 | App Attest 闭环：attestKey→服务端 stub 验链/nonce/RP ID；assertion counter 复用被拒；`isSupported=false` 计入风险信号而非崩溃 | TC-SI-006 | 真机走查 + stub 服务端 |
| AC-7 | @expo/app-integrity 锁版：package.json 精确版本（无 ^/~），SDK 升级需走修宪流程评估 breaking change | TC-SI-007 | CI grep（lockfile 断言） |
| AC-8 | 检测不阻塞首屏：首帧前无 dyld 遍历/文件扫描/网络 attestation | TC-SI-008 | Xcode Instruments trace |

## 5. 测试用例

### TC-SI-001 Keychain ACL 与锁定态
```gherkin
Given 真机完成 ensureKek() 并完成一次 wrap
When 锁屏后尝试 unwrapKey
Then 抛 E_SEC_IOS_KEYCHAIN_INACCESSIBLE
When 解锁后 unwrapKey 并完成 Face ID
Then 返回明文（native 窗口内）且用完即清零
```

### TC-SI-002 生物特征变更失效
```gherkin
Given 已 wrap 的 k1 blob
When 系统设置新增一枚面容
Then 下一次 ensureKek/unwrapKey 抛 E_SEC_IOS_KEK_MISSING
And 恢复流引导重新导入助记词（vault-core）并重建 KEK 与 blob
```

### TC-SI-003 密文完整性负例
```gherkin
Given 合法 blob
When 翻转 ciphertext 或 tag 的任意 1 字节
Then unwrapKey 抛 E_SEC_IOS_BLOB_CORRUPT
And error.message 不含任何密钥/明文片段
```

### TC-SI-004 越狱检测正负例
```gherkin
Given 未越狱真机 / Dopamine 非隐藏模式真机 / Dopamine 2.4 Hide Jailbreak 开启的真机
When runDetection('full')
Then 分别得到：无信号；命中 /var/jb 路径族或 dyld 可疑库；可能无信号（已知限制，风险台账记「检测不可信」而非「干净」）
```

### TC-SI-005 反调试
```gherkin
Given release build
When 尝试 lldb attach
Then 附加失败（PT_DENY_ATTACH）
And CI grep 断言二进制中 ptrace 仅经 dlsym 动态解析（无直接符号引用）
```

### TC-SI-006 App Attest 闭环
```gherkin
Given stub 服务端下发 ≥16 字节 challenge
When attestDevice(challenge) 后上送 attestation 对象
Then stub 服务端验 x5c 链至 Apple App Attest Root、nonce、RP ID 通过
When 同一 assertion counter 重放
Then 服务端拒绝
When 模拟 isSupported=false（模拟器）
Then 记 E_SEC_IOS_ATTEST_UNSUPPORTED 并进入降级风险档，不崩溃
```

### TC-SI-007 alpha 依赖锁版（CI grep）
```gherkin
Given package.json 与 lockfile
When 扫描 @expo/app-integrity 版本声明
Then 为精确版本且与 tech-stack.md 登记一致；任何 bump 需修宪记录
```

### TC-SI-008 首屏零阻塞（trace）
```gherkin
Given 冷启动 Instruments trace
When 检查首帧前主线程
Then 无 dyld 遍历、无 /var/jb stat、无 App Attest 网络调用
```

### TC-SI-009 Keychain KEK 生命周期（真机/Mac）

```gherkin
Given 合格 iPhone 与 expo-secure-store 条目（requireAuthentication + WHEN_UNLOCKED_THIS_DEVICE_ONLY）
When ensureKek → wrap → unwrap → deleteKek
Then 条目经生物识别放出 KEK；roundtrip 一致；deleteKek 后读取抛 E_SEC_IOS_KEK_MISSING
```

### TC-SI-010 跨平台 blob 结构 parity（XCTest）

```gherkin
Given 与 Android TC-SA-010 相同的布局向量（version/IV/ciphertext/tag）
When CryptoKit AES.GCM seal/open
Then 布局一致、AAD 绑定一致
And 错误 AAD / 篡改 / 截断均抛 E_SEC_IOS_BLOB_CORRUPT
```

### TC-SI-011 KEK 坐标契约（测试侧，Mac/simulator）

```gherkin
Given 已安装的 expo-secure-store 57.0.4（requireAuthentication 条目坐标 service="app:auth"、account=Data(key.utf8)）
When 断言 KeychainKek.service == "app:auth" 且 account == "nexvault.kek.v1"
And 在 test-only 坐标写入同一编码的 hex 值并经 loadKek(service:account:) 读回
Then 字面量契约与 Data-account 编码/hex 解码均被验证（不触碰生产 KEK 条目）
```

## 6. 依赖与红线

- **R1 + R6**：fail-closed 门控同 Android——越狱/调试信号不作签名硬前置（必然误伤+可绕），而是风险评分输入；签名材料释放由门控裁决。宪法依据：R1（密钥材料不出 native，检测不过则不释放）+ R6（fail-closed 耦合为唯一 [P] 级端上口径，其余检测一律 [C]）。
- **R6**：§3 全部检测机制标 [C]；唯一 [P] 为「生物识别 ACL 防静态脱库」与「备份阻断」。Dopamine 隐藏模式漏报、iOS mlock 无保证必须如实呈现，禁止拔高。
- **R5**：KEK/k1 明文禁止日志、禁止未加密落盘；`WHEN_UNLOCKED_THIS_DEVICE_ONLY` + 禁 iCloud 同步为强制项。
- **R4**：expo-secure-store 为定案（Expo 官方维护）；react-native-keychain 停滞（10.0.0 后 19 个月无新版 + issue #783）禁止进入关键路径 [^3^]；`@expo/app-integrity` alpha 锁版 [^10^]；RN 生态 SE 签名库（react-native-secure-sign 等，0 dependents 个人维护）禁止作为关键路径依赖。
- **R1 联动**：unwrap 出的明文只进 vault-core 签名调用窗口，JS 不可见。
- 流程纪律：双侧验证（XCTest + 真机走查，模拟器无 SE 的结论一律无效）；Agents 不得 commit/push。

## 7. Roadmap 项（3 天外）

1. **SE P-256 包裹增强层（v2）**：~50 行 Swift 薄模块，`SecKeyCreateRandomKey(kSecAttrTokenIDSecureEnclave)` + ECDH 派生 KEK，实现「解密密钥永不进内存」；必须真机测试（模拟器无 SE）；需付费开发者账号。
2. **iOS mlock 真机实测**（cross_verification C6 未解决项）：结论回写威胁模型。
3. App Attest 服务端验证库选型定案（node-app-attest vs 自实现；注意 `fmt: apple-appattest` ≠ WebAuthn `fmt: apple`）；WWDC26 新增 authenticator-data 扩展与 receipt-based fraud metric 评估 [^7^]。
4. Dopamine-RootHide 随机化路径对抗：行为异常检测与硬件背书 attestation 补位研究。
5. expo-secure-store `requireAuthentication` 在新架构/新 iOS 版本的行为回归（参照 react-native-keychain #783 同类问题）。

---
### 来源
[^1^]: SimpaLabs 渗透测试博客 — 越狱机 keychain_dumper 可 dump 无 ACL 条目；kSecAccessControlBiometryCurrentSet 强制 SE 校验后才释放（2026-07）。
[^2^]: Apple Security 头文件注释（SecItem.h）— SE 仅支持 256-bit EC（P-256）且不可导入预生成密钥。
[^3^]: npm registry + GitHub API 实测 — react-native-keychain latest=10.0.0（2025-03-23），近 6 个月仅 1 次 chore 提交；issue #783 生物识别 ACL 失效未修。
[^4^]: tessl.io Expo 文档镜像 — expo-secure-store 2048B 上限、requireAuthentication 行为、生物特征变更自动失效。
[^5^]: HackMag — PT_DENY_ATTACH 非文档化，直接链接有审核风险，标准做法 dlsym 动态解析；可被二进制 patch/Frida 绕过（2026-06）。
[^6^]: dim08 C30/C33 — App Attest 官方流程；fmt=apple-appattest 与 WebAuthn apple 格式不同，@simplewebauthn/server 不能直验。
[^7^]: Apple WWDC26 Session 201 — attestation/assertion 被拒应优雅降级、评估风险再考虑阻断；macOS 27 扩展与 fraud metric。
[^8^]: Expensify/App issue #81912（Group-IB 审计）— JS 桥方案私钥短暂驻留内存可被 userspace 恶意代码读出。
[^9^]: securing/IOSSecuritySuite README TODO（Dopamine hidejb detector 未完成）；Digital.ai 2025-11 — Dopamine 2.4 Hide Jailbreak 使成熟检测库报「未越狱」。
[^10^]: docs.expo.dev/versions/latest/sdk/app-integrity —「This library is currently in alpha and will frequently experience breaking changes」。
