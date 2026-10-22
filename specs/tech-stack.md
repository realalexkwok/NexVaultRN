# spec: tech-stack — 批准依赖清单（宪法化）

> Status: active | Date: 2026-10-14 | Constitution: yes
> 本文件是依赖准入的唯一仲裁文件（AGENTS.md R4）。逐行决策依据（版本、用途、CVE 处置、信任依据/风险点/缓解）见 **`doc/dependencies.md`**（本文件的附录与决策依据，只读存档）；两份文件必须同步修订。

## 1. 修宪规则（binding）

1. **加依赖 = 修宪**：新增/升级/移除任何依赖，必须先修改本文件 + `doc/dependencies.md`（版本、用途、CVE 处置、信任依据），owner 批准后方可安装。**Never add a dependency outside this file.**
2. **版本钉死**：`pnpm-lock.yaml` 入库，CI 一律 `pnpm install --frozen-lockfile`；密钥/签名/网络相关依赖 `package.json` 用精确版本、不用 `^`；「最新 ≠ 最安全」，禁止因「有新版本」而升级。
3. **配套版本锁（不可单独升级）**：与 Kotlin↔KSP 必须同版本锁同理——**Expo SDK 与其钉住的 react-native / react / expo-router / expo-updates / reanimated / worklets 版本构成一个绑定单元**，任何成员升级必须整单元评估并修宪（dependencies.md §1/§3 矩阵）。
4. **供应链门禁**（适用于全部行，dependencies.md §头）：`ignore-scripts=true`；min-release-age ≥7 天；CI 跑 `pnpm audit` + Socket 扫描，高危即阻断；web3/crypto 生态包装前人工逐字核对官方包名。

## 2. 批准清单（摘要；完整行级备注以 doc/dependencies.md 为准）

| 包/模块 | 钉版 | 安装位置 | 备注 |
|---|---|---|---|
| expo（SDK 57 基座） | ≥57.0.21 | apps/mobile | 修 Hermes v1 回归的下限；SDK↔RN 0.86 / React 19.2.3 绑定单元 |
| react-native / react | 0.86（0.86.3）/ 19.2.3 | apps/mobile | 随 SDK 钉版，不单独升级 |
| react-native-web | ~0.21.0 | apps/mobile | Web 端渲染层 |
| expo-router | ~57（57.0.28） | apps/mobile | 版本与 SDK 对齐；禁 `@react-navigation/*` 直接导入；括注 2026-10-19 同步实际解析版本（T2.1-H9 关闭，T2.1a 规划轮 D4） |
| expo-linking | ~57.0.13 | apps/mobile | expo-router **57.0.28** 必需 peer（`^57.0.13`）；深层链接；**2026-10-19 T2.1 评审 MAJOR-1 R4 补记**（T2.1 Task 1 首装，SDK 解析、lockfile 精确）；NIT-B：引文按实际安装版本核对 |
| expo-constants | ~57.0.22 | apps/mobile | expo-router **57.0.28** 必需 peer（`^57.0.22`）；同上补记；NIT-B：引文按实际安装版本核对 |
| expo-font | ~57.0.4 | apps/mobile | SDK 57 绑定单元（expo 基座依赖 ^57.0.4 已在 lockfile）；提升为直接行仅新增 importer 条目、依赖图不变；Inter 4 权重 TTF（OFL；原仓库 core/core-ui/src/main/res/font 复制 + res/raw/inter_ofl_license.txt 随包）；**2026-10-19 T2.1a 规划轮 R4 修宪（owner 批准，D1）** |
| expo-status-bar | ~57.0.1 | apps/mobile | expo-router 手动安装文档钉版；状态栏样式；同上补记 |
| react-native-safe-area-context | ~5.7.0 | apps/mobile | expo-router 57 peer（`>=5.4.0`）；安全区；同上补记 |
| react-native-screens | ~4.26.2 | apps/mobile | expo-router 57 peer（`^4.26.0`）；原生屏幕容器；同上补记 |
| babel-preset-expo | ~57.0.15（devDependency） | apps/mobile | SDK 57 绑定单元；NativeWind v4 babel preset 前置；同上补记 |
| @babel/plugin-transform-react-jsx | 7.29.7（精确） | apps/mobile（devDependency） | babel-preset-expo 已含同版传递依赖；pnpm isolated 布局下 Gradle release 打包（`export:embed`）按 app 根解析 Babel 插件 → 首轮 release 构建报 `Cannot find module`；提升为直接行仅新增 importer 条目、零新包下载；**构建期专用**；**2026-10-19 T2.1a R4 修宪（owner 批准）** |
| @tanstack/react-query | 5.104（5.104.2） | apps/mobile | 仅缓存非敏感数据（R5） |
| zustand | 5.0（5.0.15） | apps/mobile | 密钥语义状态永不进 store（R1） |
| nativewind | v4（4.2.7） | apps/mobile | **锁 v4**；v5 RC 钉带 CVE 的 worklets，排除；必需 peer `tailwindcss`（v4 稳定线 = Tailwind v3，companion 行见下，**2026-10-18 T2.1 R4 修宪，owner 批准**） |
| tailwindcss | 3.4.17（精确钉版；2024-12-17 发布） | apps/mobile（devDependency） | NativeWind v4 必需 peer（>3.3.0，官方安装文档钉 ^3.4.17）；**2026-10-18 T2.1 R4 修宪（owner 批准，规划轮 D3）**；v4 稳定线用 Tailwind v3（latest 4.3.3 = Tailwind v4 线，随 NativeWind v5 RC 一并排除） |
| react-native-css-interop | 0.2.7（精确钉版；nativewind 4.2.7 自身依赖的同一版本） | apps/mobile（dependencies） | NativeWind v4 `jsxImportSource` 运行时的解析载体——pnpm isolated 布局下 app 代码无法从 nativewind 上下文解析 `react-native-css-interop/jsx-runtime`（首轮 web export 实证失败），提升为直接行仅改 package.json 行、**不改变依赖图**（本就在 lockfile）；**2026-10-18 T2.1 R4 修宪（owner 批准，D6）** |
| react-native-reanimated | 4.7（4.7.1） | apps/mobile | 需 react-native-worklets ≥0.12.2（CVE-2026-97724 下限） |
| react-native-worklets | ≥0.12.2（0.13.0） | apps/mobile | CI 断言解析结果 |
| @shopify/flash-list | v2（2.3.3） | apps/mobile | 新架构前提已满足 |
| viem | 2.56.x（2.56.9） | packages/web3-adapter + apps/mobile | 唯一 EVM 链交互库 |
| react-native-get-random-values | 2.0.0 | apps/mobile | 入口 polyfill 首行；远程调试退化 Math.random（R8） |
| fastestsmallesttextencoderdecoder | 1.0.20 | apps/mobile | TextEncoder 防御性 polyfill（viem 官方推荐） |
| @noble/curves / @noble/hashes / @scure/bip39 / @scure/bip32 | vault-core 测试直用 2.4.x；**viem 2.56.9 传递生产副本 = @noble/curves 1.9.1 / @noble/hashes 1.8.0 / @scure/bip32 1.7.0 / @scure/bip39 1.6.0** | packages/vault-core（dev 测试）+ packages/web3-adapter（viem 传递，** native only**） | 信任依据：viem 官方依赖（paulmillr 审计）。**2026-10-22 更正**：不再是「仅 devDependency」——原生 bundle 含 viem 图（预期）；**web chunk 经 R7 split 后全矩阵 0**（`@noble*`/`@scure*`/`secp256k1`/wallet actions，见 T2.2 validation §Task 5 行 5b），web 读路径为 fetch JSON-RPC |
| expo-secure-store | ~57（随 SDK） | apps/mobile | KV 密文存储，不存 k1 私钥 |
| expo-screen-capture | ~57.0.4 | apps/mobile | FLAG_SECURE / app switcher 模糊 |
| @expo/app-integrity | alpha（**精确锁版**） | apps/mobile | 升级走修宪 |
| @bam.tech/react-native-app-security | 1.0.0 | apps/mobile | TLS 双 SPKI pinning |
| expo-updates | ~57（随 SDK） | apps/mobile | 版本变更即触发 R3 热更禁区 |
| eas-cli | CI 钉版 | CI 环境 | 签名私钥永不入 CI/仓库（R2） |
| typescript | ~6.0.3 | apps/mobile（dev） | strict 模式与 B.6-1 门禁；实测 6.0.3 |
| jest + jest-expo + @jest/globals | **29.7.0 / 57.0.6 / 29.7.0（精确钉版）** | apps/mobile（dev；2026-10-22 更正，原记 root） | **2026-10-22 owner 批准「Add Jest tests now」**（B.6-3）；lockfile `packages` **647 → 862（+215）**；`pnpm --filter mobile exec jest` **2 suites / 13 tests**；audit：**sprintf-js@1.0.3 为新引入（moderate，dev-only，无补丁）**、braces 既有（jest 另增一条 `@jest/core→micromatch→braces` 路径）、其余既有——详见 doc/dependencies.md jest / @jest/globals 两行 |
| eslint-config-expo | 随 SDK 模板（**未安装**） | apps/mobile（dev，待安装） | **B.6-2 lint 门禁尚未接线**：package.json 无该依赖、lockfile 无记录；待 R4 提案/安装后更新本行（2026-10-22 评审发现） |
| pnpm | ≥9（isolated 安装） | 工具链 | 异常时回退 hoisted + 双层屏蔽 |
| modules/vault-core | 自写（Kotlin+Swift） | modules/ | 四方法闭环；roadmap：libsecp256k1 v0.7.0 经 CMake |
| junit / androidx.test（modules/vault-core 测试） | junit 4.13.2；androidx.test.ext:junit 1.3.0；androidx.test:runner / rules / core 1.7.0 | modules/vault-core/android（test + androidTest） | **T1.2 R4 修宪（owner 2026-10-14 批准）**；BIP39 前 4 向量自动门禁；版本参照原 NexVault 仓库 + Expo SDK 57 自身测试用法（dependencies.md §6a） |
| org.bouncycastle:bcprov-jdk18on | 1.86（精确钉版） | modules/vault-core/android（`implementation`） | **T1.3 R4 修宪（owner 2026-10-15 批准）**；BIP-32/44 secp256k1 派生 + pre-NIST Keccak-256 + 78 字节扩展密钥序列化与 Base58Check **由测试侧自写**（BC 1.86 无 `HDKeyDerivation`/`HDKey`/`Base58` 类，仅提供 `SHA256Digest` 等原语；2026-10-16 T1.3 代码评审更正措辞）；仅 lightweight API（`org.bouncycastle.crypto.*`），禁止注册 JCE Provider；OSV 1.86 零已知漏洞（dependencies.md §6b） |
| swift-secp256k1（21-DOT-DEV，ex-Blockstream） | 0.23.2（精确钉版） | modules/vault-core/ios（**2026-10-17 路线修订：SwiftPM 源码集成**（RN `spm_dependency`）；原 CocoaPods 预编译 xcframework 路线作废，见行末） | **T1.5 R4 修宪（owner 2026-10-16 批准，GATE-R4-SWIFTSECP / 规划轮 Q1）**；secp256k1 曲线运算（BIP-32 公钥派生）+ **RFC6979 可恢复 ECDSA 签名**（`secp256k1_ecdsa_sign_recoverable`，noncefp = NULL → 库内置 RFC6979；low-s + recid 翻转由库内完成，与 Android BLOCKER-1 修复后的语义一致）；MIT；tag published 2026-05-23（≥7 天冷静期）；repo 活跃（2026-10-16 push，160 stars）；发布制品 `P256K.xcframework.zip` sha256 `0084b98982506a85dfbdf4568236c7c95cc77f10c2b8cdc0e567647844c00f77`（podspec 内钉死）；**core pod（VaultCoreCore）平台下限 iOS 15.1、bridge（VaultCore）16.4、app 18.0**（→ `app.json` `ios.deploymentTarget = 18.0`）；库自述 **pre-1.0**（成熟度备注见 validation）；Blockstream→21-DOT-DEV 更名已记录（R4-5）；**禁止**改用其他 secp256k1 包或自写曲线运算（spec: vault-core §2.2）；详见 dependencies.md §6c。**2026-10-17 Mac 会话路线修订（2026-10-18 owner 追认——T2.1 规划轮 D1，T1.5-H6 关闭）**：CocoaPods 预编译制品在 Xcode 26.5 / Swift 6.3.2 上**不可消费**——(a) 制品 `.swiftinterface` 第 10 行 `import libsecp256k1`，而 pod/zip 不含该 Clang 模块或头文件（`unzip -l` 仅 `P256K.xcframework/**`，interface 内 `secp256k1_` 出现 0 次）；(b) 补足该 import 后 interface 仍无法加载：`UInt256.Words = P256K._UInt256Words` 中 `P256K` 被同模块内同名 `enum P256K` 遮蔽（66 条诊断，冷模块缓存复现）；0.23.2 为 trunk/GitHub 最新发布，无修复制品。改为**同版本源码构建**（tag `0.23.2` = commit `e70a10e`；本机 `swift build` 通过；上游 README 明示源码路径不受影响）：podspec 以 RN `spm_dependency` 声明 `exactVersion 0.23.2`，product `P256K`。**版本钉版、上游仓库/commit、许可证、全部向量与测试均未改变**；Mac 侧 `VaultCore-Unit-Tests` **17/17 通过**（iPhone 17 / iOS 26.5 模拟器）。证据：`specs/features/2026-10-16-T1.5-vault-core-ios-port/validation.md` §Mac session |
| libsecp256k1（bitcoin-core/secp256k1） | v0.7.0（roadmap） | modules/vault-core（CMake） | **roadmap 项**，引入时按 R4 复核 GitHub releases |

## 3. 全局禁用表（摘要；详表以 doc/dependencies.md §8 为准）

- **web3.js**：项目已归档终止（2025-03-04）。
- **react-native-keychain**：维护停滞（10.0.0 后 ~19 个月无新版 + 未修 ACL issue）。
- **react-native-google-play-integrity**：Snyk 判定 INACTIVE。
- **nativewind v5 RC**：钉住带 CVE-2026-97724 的 worklets 0.10.1。
- **gluestack-ui v5**：2026-06 供应链 RAT 事件 + 放弃 Web/Next.js。
- **TanStack Router**：官方明确不支持 RN。
- **wallet-core 预装形态 / @trustwallet/wallet-core（WASM）**：CVE-2025-66692 + String 助记词/WASM 内存无法清零（违反 R1）。
- **text-encoding**：npm 官方 DEPRECATED（TextEncoder 用 fastestsmallesttextencoderdecoder）。
- **@ronickg/react-native-bip39 / react-native-quick-bip39**：助记词以 JS string 出 native（违反 R1）。

## 4. 应用身份（永久标识，binding）

- **Android `applicationId` = iOS `bundleIdentifier` = `io.github.realalexkwok.nexvault`**（决策日 2026-10-14，owner 确认）。
- 弃用 `com.nexvault.wallet` 的原因：该 applicationId 已被原 Kotlin Android 项目（github.com/realalexkwok/NexVault）占用；applicationId 与签名证书绑定后终身不可复用，RN 工程必须使用独立身份。
- 该标识**上架后终身不可改**（Play Store / App Store Connect 均锁定），禁止以任何理由新增变体后缀（如 `.rn`/`.debug` 正式产物；debug 构建允许 `.debug` suffix 且仅限 dev/preview profile）。
- 选段规则备查：applicationId 每段须为合法 Java 标识符（禁连字符）；bundle ID 仅允许字母数字+连字符+点（禁下划线）——本定案双端合法。GitHub 反域名天然唯一且归属可证明，无需持有自有域名。
- 绑定使用处：Developer Verification 注册（roadmap 阶段 2 第 10 项）、native 签名校验白名单（spec: security-android）、EAS project/app.json（spec: ota-update §2.1）、Play Integrity / Key Attestation 的 `attestationApplicationId` 校验。
