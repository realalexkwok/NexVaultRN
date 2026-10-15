# spec: vault-core

> Status: draft | Date: 2026-10-14 | Evidence: doc/research-report.md §3.5、§3.6（调研依据 nexvault-rn_dim07、dim02、cross_verification H11/C6）
> 修订：2026-10-15（T1.3 / H2，owner 批准）——§2.1 `VaultHandle` 由 `number` 改为品牌化 `string`（对齐 T1.2 冻结实现：native `UUID.randomUUID().toString()`）；§2.2/§2.3 双端签名 `handle: Int` → `handle: String`；§2.4 合并 `E_VAULT_CHECKSUM_MISMATCH` 入 `E_VAULT_INVALID_MNEMONIC`；TC-VC-002 同步。证据：specs/features/2026-10-15-T1.3-vault-core-android-bip32/validation.md Task 1。

## 1. 目标（Goal 与范围）

**Goal**：在 Expo local module（Kotlin + Swift）内实现「导入助记词 → 派生地址 → 签名 → 清零」的 native 闭环。JS 层（Hermes 字节码视为明文 [^9^]）永不接触 seed、私钥、链码；JS↔native 边界只传 `ArrayBuffer` 与 opaque handle [^1^][^2^]。这是三端信任分级中「私钥不出 native」红线（AGENTS.md R1）的唯一落地模块。

范围内：
- BIP-39 校验与 seed 派生（PBKDF2-HMAC-SHA512，2048 轮）
- BIP-32/secp256k1 HD 派生 + BIP-44 路径解析（EVM 仅 `m/44'/60'/a'/0/i`）
- RFC6979 确定性 ECDSA 签名（输入 32 字节 digest，输出 65 字节 `r‖s‖recid`）
- 全链显式清零（mnemonic bytes → seed → 各级 privkey/chaincode）
- Android 侧 mlock 防 zRAM swap [^7^]；iOS 侧 best-effort mlock + 无 swap 依赖（已知限制，如实标注）
- BIP-39/BIP-32 官方测试向量 + BIP-44 已知地址回归

Out of scope：
- Web 端实现（Web 不持私钥，见 spec: web3-adapter §6 / AGENTS.md R7）
- C++ 共享核心（libsecp256k1 经 JSI 直连）、native UI 词格键盘、多链（BTC/Solana）、BIP-39 passphrase（第 25 词）——全部列入 §7 Roadmap
- 密钥静态存储（at-rest 包裹属 spec: security-android / security-ios）
- StrongBox/Secure Enclave 内签名（两平台 TEE 均不支持 secp256k1 [^8^]，仅可作存储加密层）

## 2. 接口定义

### 2.1 JS/TS 侧（`modules/vault-core/index.ts`）

```ts
/**
 * opaque 句柄，仅作 ID，不携带任何密钥语义。
 * 2026-10-15（T1.3/H2 修订）：实现冻结为 native 侧 `UUID.randomUUID().toString()`，
 * 故类型由 `number` 改为品牌化 string，与 modules/vault-core 的 T1.2 实现一致。
 */
export type VaultHandle = string & { readonly __brand: 'VaultHandle' };

/**
 * 导入助记词（ArrayBuffer：24 词 UTF-8 以 0x20 连接）。
 * 调用方必须在 await 返回后立即对自身 buffer 执行 fill(0)（尽力而为，见 §3 T-VC-2）。
 */
export function importMnemonic(mnemonic: ArrayBuffer): Promise<VaultHandle>;

/** 派生 EVM 地址（EIP-55 checksum）。path 例："m/44'/60'/0'/0/0"。派生所得公钥不出 native 前先做地址化。 */
export function deriveAddress(handle: VaultHandle, path: string): Promise<string>;

/**
 * 对 32 字节 digest（keccak256 后的交易序列化哈希，由 web3-adapter 提供）做 RFC6979 ECDSA 签名。
 * 返回 65 字节 ArrayBuffer：r(32)‖s(32)‖recid(1, 取值 0/1)。
 * EIP-155 的 v/yParity 编码由 web3-adapter 完成，本模块只做曲线运算。
 */
export function signDigest(handle: VaultHandle, path: string, digest: ArrayBuffer): Promise<ArrayBuffer>;

/** 销毁句柄并对该句柄关联的全部密钥材料执行多层清零。幂等。 */
export function destroy(handle: VaultHandle): Promise<void>;

export class VaultError extends Error { readonly code: VaultErrorCode; }
```

### 2.2 Android（Kotlin，Expo Modules API）

```kotlin
class VaultCoreModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("VaultCore")
    AsyncFunction("importMnemonic") { mnemonic: ArrayBuffer -> String }       // -> VaultHandle（UUID 字符串）
    AsyncFunction("deriveAddress") { handle: String, path: String -> String }
    AsyncFunction("signDigest") { handle: String, path: String, digest: ArrayBuffer -> ArrayBuffer } // 65B
    AsyncFunction("destroy") { handle: String -> Unit }
  }
}
```

内部约束：`importMnemonic` 在同步段内将入参拷贝至 native 自有 ByteArray 后可覆写入参；禁止 `String.join`/`String` 中间态（逐词写入 CharArray）；中间产物用 `java.util.Arrays.fill(arr, 0)` 清零，密钥页 `mmap + mlock`，用完 `explicit_bzero + munlock`，mlock 失败降级记录不中断 [^7^]。secp256k1 点运算禁止自实现：MVP 用打包新版 BouncyCastle，Roadmap 切 libsecp256k1 v0.7.0（MIT，~1.3MB/ABI）[^3^][^4^]。

### 2.3 iOS（Swift，Expo Modules API）

```swift
public class VaultCoreModule: Module {
  public func definition() -> ModuleDefinition {
    Name("VaultCore")
    AsyncFunction("importMnemonic") { (mnemonic: ArrayBuffer) async throws -> String }
    AsyncFunction("deriveAddress") { (handle: String, path: String) async throws -> String }
    AsyncFunction("signDigest") { (handle: String, path: String, digest: ArrayBuffer) async throws -> ArrayBuffer }
    AsyncFunction("destroy") { (handle: String) async throws -> Void }
  }
}
```

内部约束：秘密用 `Data`/`[UInt8]`，清零用 `resetBytes(in:)` / `memset_s`；PBKDF2 用 CommonCrypto `CCKeyDerivationPBKDF`，secp256k1 用 Blockstream `swift-secp256k1`（SPM，集成时锁版本）；薄封装 <300 行 [^5^]。

### 2.4 错误码表

| code | 含义 | 触发条件 | 备注 |
|---|---|---|---|
| `E_VAULT_INVALID_MNEMONIC` | 词不在 BIP-39 词表 / 词数非法 / checksum 不匹配（2026-10-15 T1.3/H2：原 `E_VAULT_CHECKSUM_MISMATCH` 已合并，实现侧 fail-closed 单一错误码） | importMnemonic | 错误消息不得回显任何词 [^6^] |
| `E_VAULT_INVALID_PATH` | 路径语法/范围非法 | deriveAddress/signDigest | 仅回显路径语法错误，不回显密钥 |
| `E_VAULT_INVALID_DIGEST_LENGTH` | digest ≠ 32 字节 | signDigest | — |
| `E_VAULT_HANDLE_NOT_FOUND` | handle 不存在或已销毁 | 全部 | 幂等 destroy 不抛此错 |
| `E_VAULT_SECURITY_GATE` | 环境检测 fail-closed 门控拦截 | importMnemonic/signDigest | 与 spec: security-android/ios 联动（AGENTS.md R1 密钥材料不出 native 的前置门控；fail-closed 标注口径见 R6） |
| `E_VAULT_CRYPTO_INTERNAL` | 底层密码原语失败 | 全部 | 不得携带密钥片段 |
| `E_VAULT_UNSUPPORTED_PLATFORM` | Web/不支持平台调用 | 全部 | Web 端模块以 stub 编译，恒抛此错 |

## 3. 威胁模型

**资产**：A1 助记词字节；A2 seed（64B）；A3 各级私钥/链码；A4 handle 表完整性。

| # | 攻击向量 | 目标资产 | 防御机制 | 级别 |
|---|---|---|---|---|
| T-VC-1 | 逆向 Hermes 字节码 / JS 层注入读取密钥 | A1–A3 | 架构控制：密钥永不以 string/JS 对象形态存在；JS↔native 仅 ArrayBuffer + opaque handle；助记词输入走自定义词格键盘（MVP 为 ArrayBuffer 直送，完整键盘在 Roadmap） | **[P]**（对 JS 层窃取面） |
| T-VC-2 | GC 复制/堆残留读取废弃密钥字节 | A1–A3 | 禁 String；ByteArray/Data 显式 `fill(0)`/`resetBytes`；缩短生命周期；JS 侧 buffer 用后即 `fill(0)`（尽力而为，Hermes GC 不可控，如实标注） | [C]（ART compacting GC 可能留有副本） |
| T-VC-3 | Android zRAM swap / core dump 泄露密钥页 | A2/A3 | `mmap` 页 + `mlock` + 用完 `explicit_bzero`+`munlock`；RLIMIT_MEMLOCK 探测与降级记录 [^7^] | [P]（成功锁页时）/[C]（锁页失败降级） |
| T-VC-3i | iOS 侧 swap/内存取证 | A2/A3 | iOS 无磁盘 swap（内存压缩代替）；**mlock 在 iOS 沙盒语义无文档保证，属未解决项**（cross_verification C6），补偿=短时驻留+显式清零+Keychain 落盘 | [C]，真机验证项 |
| T-VC-4 | 日志/异常消息回显密钥片段 | A1–A3 | 错误码表化，error message 白名单 [^6^]；CI grep 禁 log 调用出现在模块内（AGENTS.md R5） | [P] |
| T-VC-5 | root + 精确时机 dump 进程内存 | A3 | 明文仅存于签名调用窗口（<百毫秒）；per-use 生物识别前置（security-android）压缩可调用时机；残余风险为 wrapped 模式固有上限，无法根除 [^8^] | [C] |
| T-VC-6 | 重打包后篡改 JS 调用 signDigest 签任意 digest | A4 | 本模块不防伪调用——防御在 security-android（签名校验 fail-closed → `E_VAULT_SECURITY_GATE`）+ ui-wallet（native 地址显示红线）+ 用户逐笔确认 | [S]→联动后 [C] |

**论证基准**：「重打包后上报私钥」木马失效的本质是私钥不出 native + 签名在 native 闭环 + 重打包者无法通过签名/attestation 验真（cross_verification 全局一致性 #2），而非检测层。

## 4. 验收标准

| AC | 内容 | 绑定用例 | 验证方式 |
|---|---|---|---|
| AC-1 | BIP-39 官方 24 词向量全集通过（含 checksum 拒绝负例） | TC-VC-001/002 | Android instrumentation + iOS XCTest（CI 常绿） |
| AC-2 | BIP-44 已知地址一致：`abandon×23 about`（空 passphrase）`m/44'/60'/0'/0/0` → `0x9858EfFD232B4033E47d90003D41EC34EcaEda94` | TC-VC-003 | instrumentation/XCTest |
| AC-3 | 签名确定性：同一 handle+path+digest 两次签名输出逐字节相同（RFC6979）；recid ∈ {0,1}；65 字节 | TC-VC-004 | instrumentation/XCTest + jest（经 dev build） |
| AC-4 | 内存卫生：连续 import/destroy 1000 次无泄漏崩溃（Instruments/Profiler 驻留稳定）；destroy 后旧 handle 调用抛 `E_VAULT_HANDLE_NOT_FOUND` | TC-VC-005 | 真机走查（Pixel 6a + iPhone）+ 内存 profiler 截图归档 |
| AC-5 | 边界纪律：模块公开 API 的参数/返回类型不含 string 密钥语义；JS 侧调用 importMnemonic 后立即 fill(0) 的封装 hook 存在 | TC-VC-006 | CI grep（禁止签名出现敏感 string 参数）+ 代码审查 |
| AC-6 | 错误消息白名单：构造全部错误路径，断言 message 不含输入词/字节片段 | TC-VC-007 | jest（桥接 dev build）+ 单元测试 |
| AC-7 | Web 端：web 构建产物中不存在 vault-core 实现，`Platform.OS==='web'` 调用恒抛 `E_VAULT_UNSUPPORTED_PLATFORM` | TC-VC-008 | CI grep（`npx expo export --platform web` 产物扫描） |
| AC-8 | Android mlock：密钥页 lock 成功/失败均有遥测记录，失败不中断且降级标记可见 | TC-VC-009 | 真机走查 + 日志审计 |

## 5. 测试用例

### TC-VC-001 BIP-39 官方向量（正向）
```gherkin
Given 官方测试向量集（"abandon"×23 + "about" 等 24 组）
When 逐条调用 importMnemonic(wordsBytes)
Then 全部返回合法 handle，且派生 seed 与向量一致（native 内部断言，不导出 seed）
```
向量锚点：`abandon×23 about` + passphrase `"TREZOR"` → seed `c55257c360c07c72029aebc1b53c05ed0362ada38ead3e3e9efa3708e53495531f09a6987599d18264c1e1c92f2cf141630c7a3c4ab7c81b2f001698e7463b04`（仅测试代码内断言比对，不出 native）。

### TC-VC-002 助记词负例
```gherkin
Given 含词表外单词的 24 词输入 / checksum 被翻转 1 bit 的合法词输入
When 调用 importMnemonic
Then 均抛 E_VAULT_INVALID_MNEMONIC（2026-10-15 T1.3/H2：checksum 码已合并）
And error.message 不包含输入中的任何单词
```

### TC-VC-003 BIP-44 已知地址
```gherkin
Given handle = importMnemonic("abandon ... about" 的 UTF-8 字节)
When deriveAddress(handle, "m/44'/60'/0'/0/0")
Then 返回 "0x9858EfFD232B4033E47d90003D41EC34EcaEda94"（EIP-55）
When deriveAddress(handle, "m/44'/60'/0'/0/1")
Then 返回该向量对应第 2 地址（以 @scure/bip32 对拍值为准——@scure 仅用于测试对拍，不进生产密钥路径）
```

### TC-VC-004 签名确定性与格式
```gherkin
Given 有效 handle 与 32 字节 digest = keccak256("nexvault-test-vector")
When 连续两次 signDigest(handle, "m/44'/60'/0'/0/0", digest)
Then 两次输出逐字节相等，长度 65，第 65 字节 ∈ {0,1}
And 用 @noble/curves 在测试侧从已知私钥恢复签名比对 r‖s 一致
```

### TC-VC-005 内存卫生压测
```gherkin
Given 循环 importMnemonic → deriveAddress → signDigest → destroy 共 1000 轮
When 在真机上执行并记录进程 PSS/RSS 曲线
Then 无 OOM/崩溃，驻留内存无单调增长（±5% 容差）
And destroy 后对旧 handle 的任意调用抛 E_VAULT_HANDLE_NOT_FOUND
```

### TC-VC-006 边界静态检查（CI grep）
```gherkin
Given CI 规则扫描 modules/vault-core 的公开函数签名
When 存在以 string 类型承载 mnemonic/seed/privateKey 语义的参数或返回值
Then 构建失败
And JS 入口第一行 import 链中不存在将助记词拼为 string 的调用（String.join 等）
```

### TC-VC-007 错误消息白名单
```gherkin
Given 逐条触发 §2.4 全部错误码
When 捕获 error.message
Then message 与白名单模板匹配，且不含输入助记词的任何词、digest 的任何字节
```

### TC-VC-008 Web 产物禁带（CI grep）
```gherkin
Given npx expo export --platform web 产物
When 扫描 bundle 是否包含 vault-core native 绑定或 secp256k1 签名路径
Then 不包含；且调用任一 vault-core API 抛 E_VAULT_UNSUPPORTED_PLATFORM
```

### TC-VC-009 mlock 遥测（真机走查）
```gherkin
Given Android 真机执行签名 20 次
When 检查遥测日志中 mlock 成功/失败计数
Then 记录存在；失败路径下功能正常且有降级标记（不静默）
```

## 6. 依赖与红线

本模块特有红线（引用 AGENTS.md）：
- **R1（最高优先）**：JS/TS 层不得以 string/Uint8Array 持有或传递私钥；助记词仅允许以 ArrayBuffer 单次过境并立即 fill(0)。CI grep 强制（TC-VC-006）。
- **R5**：模块内禁止任何 log 调用；错误消息白名单制（TC-VC-007）；密钥材料永不未加密落盘（落盘包裹属 security-android/ios 职责，经独立别名）。
- **R6**：§3 每条防御的 [P]/[C]/[S] 标注禁止拔高；iOS mlock 无保证必须如实呈现（cross_verification C6 未解决项）。
- **R7**：Web 构建产物不得包含本模块任何密钥处理路径（TC-VC-008）。
- **R8**：真钱路径禁止远程 JS 调试——`react-native-get-random-values` 在 Chrome 远程调试下降级为 Math.random [^10^]，CI 与运行时双重检查。
- **R4**：依赖白名单——BouncyCastle（打包新版，Android MVP）、swift-secp256k1（SPM 锁版，iOS MVP）、libsecp256k1 v0.7.0（MIT，**roadmap**：C++ 化时经 CMake 引入）[^3^]；**禁止** wallet-core 进关键路径（助记词走 String 无法清零 + CVE-2025-66692 <4.4.0 [^11^]）；**禁止** `@ronickg/react-native-bip39`、`react-native-quick-bip39`（助记词以 JS string 出 native）；`@scure/bip39`、`@scure/bip32`、`@noble/curves` 仅允许出现在 `*.test.*` 对拍代码中。
- 流程纪律：本模块所有 `[x]` 状态需自动验证（命令+观察结果+构件路径）+ 真机走查双侧证据；Agents 不得 commit/push。

## 7. Roadmap 项（3 天外）

1. **C++ 共享核心**：BIP32/39/44 + libsecp256k1 编译为共享 C++ 核心经 JSI/TurboModule 直连（Expo 官方明示 C++ 主体走 Turbo Modules [^1^]），Kotlin/Swift 退为薄壳；消除双端实现分叉与对拍成本。
2. **native UI 词格键盘**：Compose/SwiftUI 自定义 2048 词前缀匹配键盘，助记词字节直送 native buffer、禁系统 IME，使 JS 完全零接触助记词（当前 IME 标志仅为 hint 非强制 [^12^]）。
3. **iOS mlock 真机实测**（cross_verification C6）：一行代码实测沙盒语义，结论回写威胁模型。
4. **BIP-39 passphrase（第 25 词）**与多账户路径管理。
5. **Schnorr/BIP-340**（libsecp256k1 `--enable-module-schnorrsig`）与未来 BTC（@scure/btc-signer）支持评估。
6. **StrongBox/Secure Enclave 角色评估**：仅可作存储加密层（两平台均不支持 secp256k1 曲线 [^8^]），与 security-android 的 wrap key 体系对齐。

---
### 来源
[^1^]: docs.expo.dev — Modules overview / workflow/customizing（local module 推荐、C++ 主体建议 Turbo Modules），2026-09 快照。
[^2^]: expo-modules-core CHANGELOG —「Improved support for ArrayBuffers…creating ArrayBuffers in native modules」，2026-10-14 读取。
[^3^]: bitcoindev 邮件列表 — libsecp256k1 v0.7.0 released，2025-07-22；openSUSE RPM 元数据（MIT，~1.3MB/ABI）。
[^4^]: bitcoin/secp256k1 README — 官方 Android NDK CMake 交叉编译命令。
[^5^]: nexvault-rn_dim07 §4.3/§7 — iOS 薄封装与 PBKDF2/CommonCrypto 路径。
[^6^]: ethereum-cryptography README —「Sensitive information can be leaked via error messages」。
[^7^]: LWN Articles/876288（RLIMIT_MEMLOCK 64KB）+ polariscode mlock 工程模式（失败降级记录不中断），2026-07。
[^8^]: AOSP `SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE` 源码白名单 + source.android.com Keystore features（无 secp256k1）；CertiK 钱包密钥保护分级（wrapped 模式内存窗口残余风险）。
[^9^]: SymbioticSec — hermes-decomp 支持 Hermes bytecode v40–v99 结构化反编译，2026-04（Hermes=明文实证）。
[^10^]: openapps.pro — react-native-get-random-values 在 Chrome 远程调试下退化 Math.random 并告警。
[^11^]: NVD/cvefeed — CVE-2025-66692，wallet-core `PublicKey::verify()` 越界读，影响 <4.4.0。
[^12^]: termai.sh —「IME_FLAG_NO_PERSONALIZED_LEARNING is a hint to the IME, not an enforcement boundary」，2026-08。
