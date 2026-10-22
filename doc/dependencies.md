# doc/dependencies.md — NexVault RN/Expo 骨架代码依赖决策清单

- 决策日期：**2026-10-14**（所有版本号以此日期的调研裁决为准；时效性事实须在引入前复核）。
- T1.2 R4 修宪：2026-10-14 owner 批准为 `modules/vault-core` 加入 JUnit/AndroidX 测试依赖（见 §6a）；本清单与 `specs/tech-stack.md` 同步修订。
- 效力：本清单是 `specs/tech-stack.md` 的决策依据与附录。加依赖 = 修宪（AGENTS.md R4）；**禁止**安装本清单与 `specs/tech-stack.md` 之外的包。
- 安装位置：`apps/mobile`（唯一 Expo app）/ `packages/vault-core`（纯 TS）/ `root`（workspace 根，devDependency）/ `modules/vault-core`（原生侧，非 npm）。
- 备注列统一为「信任依据 / 风险点 / 缓解措施」三段。
- 供应链通用门禁（适用于全部行）：lockfile 精确钉版 + `pnpm install --frozen-lockfile`；`ignore-scripts=true`；min-release-age ≥7 天；CI 跑 `pnpm audit` + Socket。[^12^]

## 1. 核心运行时（apps/mobile）

| 包名 | 版本 | 用途 | 安装位置 | 维护活跃度 | 已知 CVE 与处理 | 安全审计备注 |
|---|---|---|---|---|---|---|
| expo | ≥57.0.21（~57.0.23+） | Expo SDK 57 基座（RN 0.86 / React 19.2.3） | apps/mobile | 极高（官方，2026-06-30 发 SDK 57） | 无未修公告；SDK 56/早期 57 的 Hermes v1 内存回归已修于 57.0.9/57.0.17，官方再提示 ≥57.0.21 | 信任依据：官方 changelog 与版本表一手核实[^1^]。风险点：低于 57.0.21 携带 Hermes v1 回归。缓解：版本下限写入 tech-stack.md，CI 断言。 |
| react-native | 0.86（0.86.3） | 跨端运行时；随 SDK 57 钉版 | apps/mobile | 极高（Meta + 社区） | 无适用公告 | 信任依据：SDK 57 官方对齐。风险点：Hermes v1 仍标 experimental（上游）。缓解：不单独升级 RN，只随 SDK 走。 |
| react | 19.2.3 | UI 框架；RN 0.86.3 精确锁定 | apps/mobile | 极高 | 无适用公告 | 风险点：monorepo 中 react/react-dom 双拷贝会运行时崩溃。缓解：pnpm workspace 顶层统一钉版，三端精确一致。[^2^] |
| react-native-web | ~0.21.0（最新 0.21.4） | Web 端渲染层；Metro web 官方别名 | apps/mobile | 中（社区 necolas 仓库维护，非 Meta/Expo 核心） | 无已知公告 | 信任依据：SDK 57 模板默认锁定。风险点：API 面仍在演进（官方自述未来或移除 StyleSheet）。缓解：UI 层隔离，升级走修宪流程。[^3^] |
| expo-router | ~57（实测 57.0.26） | 文件式三端路由；版本自 SDK 55 起与 SDK 对齐 | apps/mobile | 高（Expo 官方） | 无已知未修公告（React Router 的 CVE 不适用） | 信任依据：官方唯一推荐；TanStack Router 官方明确不支持 RN，已排除。风险点：SDK 56 起与 React Navigation 解耦。缓解：`no-restricted-imports` 禁 `@react-navigation/*` 直接导入。[^4^] |
| typescript | ~5.9.x（以 tech-stack.md 钉版为准） | 类型系统；strict 模式强制 | root（dev） | 极高（Microsoft） | 无 | 备注：调研未逐版本核实，引入前按 R4 流程钉版并复核；`strict: true` 为质量门禁（AGENTS.md B.6）。 |

## 2. 状态与数据层（apps/mobile / packages/vault-core）

| 包名 | 版本 | 用途 | 安装位置 | 维护活跃度 | 已知 CVE 与处理 | 安全审计备注 |
|---|---|---|---|---|---|---|
| @tanstack/react-query | 5.104（实测 5.104.2，2026-10-11） | 服务端状态：余额/行情轮询（refetchInterval + staleTime） | apps/mobile | 高（2026-10-11 仍在发布） | 无直接漏洞；2026-05 TanStack 供应链事件仅波及 Router/Start 仓库，官方 postmortem 确认 Query 未受影响（GHSA-g7cv-rxg3-hmpx） | 信任依据：官方声明支持 RN。风险点：持久化插件误存敏感数据。缓解：query-async-storage-persister 仅缓存非敏感行情数据（R5）。[^5^] |
| zustand | 5.0（实测 5.0.15） | 客户端状态（UI 态/会话态） | apps/mobile | 高 | Snyk：无直接漏洞 | 信任依据：1.1KB、下载量第一。风险点：误把密钥语义状态放进 store。缓解：私钥/助记词永不进 store（R1），eslint 规则辅助。[^5^] |

## 3. UI / 动画 / 列表（apps/mobile）

| 包名 | 版本 | 用途 | 安装位置 | 维护活跃度 | 已知 CVE 与处理 | 安全审计备注 |
|---|---|---|---|---|---|---|
| nativewind | v4 稳定线（4.2.7，2026-09-14） | Tailwind 样式层，三端编译期 | apps/mobile | 高（v4 维护 + v5 积极开发中） | 自身无 CVE；**v5 RC 钉住 worklets 0.10.1（带 CVE-2026-97724）→ 裁决：不选 v5 RC** | 信任依据：官方文档明示 "V4 remains the stable release"。风险点：v5 RC「最新」诱惑。缓解：锁 v4；v5 转正后走修宪再评估（C5 裁决：最新 ≠ 最安全）。**2026-10-18 T2.1 R4 修宪（owner 批准）**：必需 peer `tailwindcss` 精确钉 3.4.17 为 companion 行（v4 稳定线 = Tailwind v3；Tailwind v4 属 NativeWind v5 RC 路线，排除）；其运行时 `react-native-css-interop` 0.2.7 提升为直接行（D6，见下）。[^6^] |
| tailwindcss | 3.4.17（精确钉版；2024-12-17 发布，冷静期天然满足） | NativeWind v4 必需 peer（>3.3.0，官方安装文档钉 ^3.4.17）；编译期样式引擎（Tailwind v3 线） | apps/mobile（devDependency） | 高（Tailwind Labs 官方包） | 无已知 CVE（v3 线） | 信任依据：npm 官方包 + NativeWind v4 官方安装文档钉版；**2026-10-18 T2.1 R4 修宪（owner 批准，规划轮 D3）**。风险点：latest 4.3.3 为 Tailwind v4 主线，与 v3 线混淆。缓解：精确钉 3.4.17；升级 Tailwind v4 必须与 NativeWind v5 RC 迁移整单元评估（后者被排除，见上行）。[^6^] |
| react-native-css-interop | 0.2.7（精确钉版；= nativewind 4.2.7 自身 `dependencies` 钉的同一版本） | NativeWind v4 `jsxImportSource` 的 className 运行时（JSX 被改写为 `react-native-css-interop/jsx-runtime`） | apps/mobile（dependencies；**2026-10-18 T2.1 R4 修宪，owner 批准 D6**） | 高（nativewind 官方仓库组件，随 nativewind 发版） | 无已知 CVE | 信任依据：nativewind 4.2.7 package.json 精确钉 0.2.7。风险点：pnpm isolated 布局下仅在 nativewind 自身上下文可解析，app 代码直接引用其子路径失败（T2.1 web export 首轮实证）。缓解：提升为直接行（依赖图不变，lockfile 已含），钉死 0.2.7 随 nativewind 修宪升级。[^6^] |
| react-native-reanimated | 4.7（实测 4.7.1） | UI 线程 worklet 动画，60/120fps 基线 | apps/mobile | 高（Software Mansion） | 历史 CVE-2022-24373（<3.0，不适用）；4.6.0 `__proto__` 崩溃 issue 已关 | 信任依据：SDK 56/57 官方钉子版本。风险点：与 Hermes v1 的内存回归（57.0.9 已修）。缓解：配合 expo ≥57.0.21。[^7^] |
| react-native-worklets | ≥0.12.2（当前 0.13.0） | reanimated 4 的必需 peer | apps/mobile | 高 | **CVE-2026-97724：<0.12.2 原型链污染→远程 DoS；0.12.2 已修 → 锁 ≥0.12.2** | 信任依据：OpenCVE/Rapid7 记录。缓解：package.json 下限 + CI 解析结果断言。[^7^] |
| @shopify/flash-list | v2（2.3.3，2026-10-01） | 交易记录/地址簿长列表；免 estimatedItemSize | apps/mobile | 高（Shopify 生产验证） | 无已知公告 | 信任依据：新架构重写、官方文档完善。风险点：v2 要求新架构（SDK 55+ 强制，天然满足）。缓解：<200 行静态页退回 FlatList。[^8^] |

## 4. Web3 / 链交互（apps/mobile + packages/vault-core）

| 包名 | 版本 | 用途 | 安装位置 | 维护活跃度 | 已知 CVE 与处理 | 安全审计备注 |
|---|---|---|---|---|---|---|
| viem | 2.56.x（实测 2.56.9，2026-09-24） | 唯一 EVM 链交互库：构造→serializeTransaction→keccak256→native 签名→sendRawTransaction | **packages/web3-adapter + apps/mobile（2026-10-22 更正；原记 vault-core）** | 极高（近 52 周 1038 commits，日发布节奏） | 本体无 CVE/GHSA | 信任依据：polyfill 面窄、~35kB vs ethers ~200kB、外部签名流为一等 API。风险点：生态仿冒包（`verify-contract-viem` 等 typosquat）。缓解：只装官方包名 + lockfile（R4）；#4002 typed-array 边界列入真机验证项。[^9^] |
| react-native-get-random-values | 2.0.0 | `crypto.getRandomValues` polyfill（native 随机源），入口第一行 import | apps/mobile | 稳定（小而精，2.0 起仅新架构，最低 RN 0.81） | 无已知 CVE | 风险点：**Chrome 远程调试下退化 Math.random** → R8 红线配套。缓解：入口 polyfills.ts 固定首行；release 走查禁调试。[^10^] |
| fastestsmallesttextencoderdecoder | 1.0.20 | TextEncoder polyfill（viem 官方推荐，防御性） | apps/mobile | 低频但稳定、纯 JS 无安全面 | 无 | 信任依据：viem 官方 compatibility 页推荐。备注：`text-encoding` 已被 npm 标记 DEPRECATED，禁用。[^10^] |
| @noble/curves / @noble/hashes / @scure/bip32 / @scure/bip39 | 测试直用 2.4.x（vault-core dev）；**viem 传递生产副本：curves 1.9.1 / hashes 1.8.0 / bip32 1.7.0 / bip39 1.6.0** | vault-core 测试向量对拍；viem 的派生/签名图（**native only**） | packages/vault-core（dev）+ packages/web3-adapter（viem 传递） | 极高（paulmillr；4 次第三方审计：Trail of Bits / Kudelski / Cure53 等） | 无已知未修 | 信任依据：viem 底层即 noble，零新增供应链面。**2026-10-22 更正（review 轮 1 MINOR-3）**：不再是「仅 devDependency」——原生 bundle 含该图属预期；**web chunk 经 R7 split 后为 0**（fetch JSON-RPC 读路径），见 T2.2 validation §Task 5 行 5b。风险点：误入 web 密钥路径（JS 不可常数时间/清零，作者明示）。缓解：web split + R7 全矩阵 grep。[^11^] |
| ~~web3.js~~ | —（**禁用**） | — | — | 已终止：ChainSafe 2025-01-15 宣布 sunset，2025-03-04 仓库归档，最终版 4.16.0 | 项目终止即最大风险 | 处理：列入禁用表；官方指引迁移至 ethers/viem。[^11^] |

## 5. 安全能力（apps/mobile / modules）

| 包名 | 版本 | 用途 | 安装位置 | 维护活跃度 | 已知 CVE 与处理 | 安全审计备注 |
|---|---|---|---|---|---|---|
| expo-secure-store | ~57（随 SDK） | Keystore/Keychain 封装的 KV 密文存储（存 wrap key/凭据，不存 k1 私钥） | apps/mobile | 高（Expo 官方随 SDK 发版） | 无已知公告 | 信任依据：官方模块。风险点：Web 不支持（架构上 Web 本不持密钥）；值上限 ~2048B；生物识别在 Expo Go 不可用。缓解：Development Build 起步；k1 一律走 vault-core native（R1）。[^13^] |
| expo-screen-capture | ~57.0.4 | Android FLAG_SECURE 防截屏/最近任务空白；iOS app switcher 模糊 | apps/mobile | 高（官方） | 无 CVE；已知 bug expo/expo#48985（iOS 重复 prevent 黑屏） | 缓解：统一默认 key、幂等调用。安全级别：[C]（提高肩窥/截图成本）。[^14^] |
| @expo/app-integrity | alpha（**锁版使用**） | Play Integrity / App Attest 官方封装，双端设备证明 | apps/mobile | 官方但 alpha，频繁 breaking change | 无已知公告 | 信任依据：官方。风险点：alpha 破坏性变更。缓解：**钉死精确版本**，升级走修宪；STRONG verdict 不硬卡（误伤旧补丁设备）；服务端验 JWT。[^15^] |
| @bam.tech/react-native-app-security | 1.0.0 | TLS 双 SPKI pinning（iOS TrustKit / Android OkHttp CertificatePinner），带 config plugin | apps/mobile | 中（225★，2026-04 发 1.0） | OSV 查询未发现包级直接漏洞 | 信任依据：3 天窗口内风险/工程量最平衡。风险点：社区体量小、Web 端无法应用层 pinning。缓解：pin 轮换预案 + [C] 级标注。[^16^] |
| ~~react-native-keychain~~ | —（**禁用**） | — | — | **停滞**：10.0.0（2025-03-23）后近 6 个月仅 1 次 chore 提交；存在未修生物识别 ACL issue | 无 CVE，但维护停滞不符合 R4 准入 | 处理：列入禁用表；曾被 MetaMask/Rainbow 使用不构成豁免；expo-secure-store 为定案替代。[^17^] |
| ~~react-native-google-play-integrity~~ | —（**禁用**） | — | — | **停滞**：Snyk 判定 INACTIVE（v1.1.0，2 年未更新，周下载 ~1.5k） | — | 处理：列入禁用表；以 @expo/app-integrity 替代。[^15^] |

## 6. 本地原生模块（modules/，非 npm）

| 模块 | 版本/基线 | 用途 | 安装位置 | 维护活跃度 | 已知 CVE 与处理 | 安全审计备注 |
|---|---|---|---|---|---|---|
| modules/vault-core（自写） | expo local module（Kotlin + Swift） | 四方法闭环：importMnemonic/deriveAddress/signDigest/destroy；JS↔native 仅 ArrayBuffer + opaque handle；全链 zeroize | modules/vault-core | 自研 | — | 信任依据：dim07 定案（3 天可行）；密钥不出 native（R1）。风险点：iOS mlock 无语义保证（如实标注为已知限制，真机实测项）；JS 侧 buffer 清零不可依赖。缓解：BIP39/32 官方向量 instrumentation 回归 + memory-hygiene 压测。[^18^] |
| libsecp256k1（bitcoin-core/secp256k1） | v0.7.0（MIT，~1.3MB/ABI，CMake 正式支持） | **roadmap**：vault-core 的签名原语 C++ 化（ECDSA/Schnorr 常数时间实现） | modules/vault-core（CMake） | 极高（Bitcoin Core 级，半年一版） | 无已知未修 CVE | 信任依据：官方 README 给出 NDK CMake 交叉编译命令；生产级常数时间设计。风险点：README 自带 WIP 免责声明。缓解：钉 v0.7.0+，引入时按 R4 复核 GitHub releases。[^19^] |
| ~~wallet-core（Trust Wallet）~~ | —（**不预装，仅备选**） | — | — | 活跃（4.8.4） | **CVE-2025-66692（<4.4.0 缓冲区越界读）**；且助记词以 String 形态驻留 JVM/Swift 堆，无法可靠清零 | 处理：不预装。仅在 vault-core 自研路线失败时作为备选启用，届时强制 ≥4.4.0 + 实测二进制体积（数十 MB 量级）+ 重写 binding 避免 String 出 native。[^20^] |

## 6a. Android 原生测试工具（modules/vault-core，T1.2 R4 修宪）

> **修宪记录：** 2026-10-14 owner 批准（T1.2 owner-approval item）。仅
> `modules/vault-core/android` 的 `test`/`androidTest` 配置；非生产运行时依赖，不影响
> R1/R7 边界。版本参照原 NexVault 仓库
> `/home/superguo/Projects/NexVault/gradle/libs.versions.toml`（JUnit 4.13.2 为实际标准）
> 与 Expo SDK 57 自身测试用法（`expo-modules-core@57.0.22` android/build.gradle 的
> `androidx.test` 1.7.0 family）。[^23^]

| 包名 | 版本 | 用途 | 安装位置 | 维护活跃度 | 已知 CVE 与处理 | 安全审计备注 |
|---|---|---|---|---|---|---|
| junit:junit | 4.13.2 | JVM 单测；BIP-39 前 4 向量自动门禁（`Bip39Test`） | modules/vault-core/android `testImplementation` | 高（JUnit 4 稳定线） | 无未修公告 | 信任依据：原 NexVault 仓库全模块实际使用版本；Expo SDK 57 自身测试同版。风险点：JUnit 4 已停止新特性。缓解：仅测试配置，不影响生产路径。 |
| androidx.test.ext:junit | 1.3.0 | `AndroidJUnit4` instrumentation smoke | modules/vault-core/android `androidTestImplementation` | 高（Google AndroidX） | 无未修公告 | 信任依据：原 NexVault 仓库版本；AndroidX 官方。风险点：真机/模拟器可用性。缓解：instrumentation 为 best-effort，JVM 向量门禁为主（T1.2 Q5）。 |
| androidx.test:runner | 1.7.0 | Instrumentation runner | modules/vault-core/android `androidTestImplementation` | 高（Google AndroidX） | 无未修公告 | 信任依据：Expo SDK 57 `expo-modules-core` 自身 androidTest 同版 1.7.0。风险点：无。 |
| androidx.test:rules | 1.7.0 | Instrumentation test rules（按需） | modules/vault-core/android `androidTestImplementation` | 高（Google AndroidX） | 无未修公告 | 信任依据：同 runner 1.7.0 family；原 NexVault 仓库同版。 |
| androidx.test:core | 1.7.0 | `ApplicationProvider`/context（按需） | modules/vault-core/android `androidTestImplementation` | 高（Google AndroidX） | 无未修公告 | 信任依据：Expo SDK 57 `expo-modules-core` 自身同版 1.7.0。 |

## 6b. Android 生产密码学依赖（modules/vault-core，T1.3 R4 修宪）

> **修宪记录：** 2026-10-15 owner 批准（T1.3 GATE-R4-BC）。仅 `modules/vault-core/android` 的
> `implementation` 配置；生产运行时依赖，用于 BIP-32/44 HD 派生（secp256k1）与 pre-NIST
> Keccak-256。依据 `specs/vault-core.md` §2.2/§6「MVP 用打包新版 BouncyCastle」定案。[^24^]

| 包名 | 版本 | 用途 | 安装位置 | 维护活跃度 | 已知 CVE 与处理 | 安全审计备注 |
|---|---|---|---|---|---|---|
| org.bouncycastle:bcprov-jdk18on | 1.86（精确钉版；Maven Central `<lastUpdated>` 2026-09-11，jar last-modified Fri, 11 Sep 2026 02:42:07 GMT） | BIP-32/44 HD 派生（secp256k1 点乘、HMAC-SHA512 CKD）、pre-NIST Keccak-256；78 字节扩展密钥序列化与 Base58Check **由测试侧自写**，BC 仅提供 `SHA256Digest` 等原语（1.86 无 `HDKeyDerivation`/`HDKey`/`Base58` 类；2026-10-16 T1.3 代码评审更正同一修宪记录的措辞） | modules/vault-core/android `implementation` | 极高（BouncyCastle 官方，1.8x 线持续维护） | **OSV 查询 1.86 → 0 条受影响公告**；该包历史 12 条公告全部在 ≤1.85 修复（最新含 GHSA-9pwp-9qqc-pr26、GHSA-qp49-qgx5-5m26 = 1.85） | 信任依据：官方 Maven Central 制品，jar sha256 `2af190b300cbb0b35e248ccf5f4a06b6072030aeb3da7a98ec73abe5b4cb371f` 与官方 `.sha256` 一致；生态广泛（bitcoinj 等）。风险点：JCE Provider 与 Android 平台内置 BC 残留冲突；助记词/私钥经 BC `BigInteger` 中间态（不可清零，[C] 级，见 T1.3 validation）。缓解：**仅用 lightweight API**（`org.bouncycastle.crypto.*`），**禁止注册 `BouncyCastleProvider`**；版本钉死 1.86，升级走 R4。[^24^] |

## 6c. iOS 生产密码学依赖（modules/vault-core，T1.5 R4 修宪）

> **修宪记录：** 2026-10-16 owner 批准（T1.5 GATE-R4-SWIFTSECP，规划轮 Q1）。仅
> `modules/vault-core/ios` 的 podspec `s.dependency`；生产运行时依赖，用于 BIP-32/44 HD
> 派生所需的 secp256k1 曲线运算与 RFC6979 可恢复 ECDSA 签名。依据 `specs/vault-core.md`
> §2.3「secp256k1 用 Blockstream swift-secp256k1（SPM，集成时锁版本）」定案；集成路线在
> 规划轮被裁定为 **CocoaPods pod**（vendored xcframework 与 SPM-via-app-target 均被否，
> 理由见 T1.5 validation Task 1）；项目/维护方 2026 年更名为 21-DOT-DEV（R4-5 包名核对）。[^25^]
>
> **2026-10-17 路线修订（Mac 会话；2026-10-18 owner 追认——T2.1 规划轮 D1，T1.5-H6 关闭）。** 上条「CocoaPods 预编译
> xcframework」路线在 Xcode 26.5 / Swift 6.3.2 上**无法消费**，两个独立缺陷均已复现并留证：
> (a) 制品 interface 第 10 行 `import libsecp256k1`，但 pod/zip 不含该 Clang 模块与头文件
> （`unzip -l P256K.xcframework.zip` 仅 `P256K.xcframework/**`；两版 interface 内 `secp256k1_`
> 命中数 = 0）；(b) 补足该 import 后 interface 仍无法加载——`UInt256.Words` 以
> `P256K._UInt256Words` 限定，而 `P256K` 被同模块内同名 `enum P256K` 遮蔽（66 条诊断，
> 冷模块缓存复现，非缓存伪影）。0.23.2 为 CocoaPods trunk（2026-05-23）与 GitHub Releases
> 的最新版本，不存在修复制品。因此改为**同一钉版的源码构建**：`spm_dependency`（RN 提供、
> podspec 可调用）声明 `exactVersion 0.23.2` + product `P256K`，由 SwiftPM 编译 tag `0.23.2`
> （commit `e70a10e036a55fffea31568f0af92d69b6d449cd`）；本机 `git clone --branch 0.23.2` +
> `swift build` → `Build complete! (13.79s)`，上游 README 亦明示「Source (SwiftPM) builds are
> unaffected」。**版本钉版、上游仓库/commit、许可证与全部向量/测试不变**；副作用是包内
> build-tool plugin（`SharedSourcesPlugin`）需 `-skipPackagePluginValidation -skipMacroValidation`
> 或等价信任步骤（T1.5-H7）。证据：`specs/features/2026-10-16-T1.5-vault-core-ios-port/validation.md`
> §Mac session（含两个缺陷的复现命令与 `VaultCore-Unit-Tests` 17/17 结果）。

| 包名 | 版本 | 用途 | 安装位置 | 维护活跃度 | 已知 CVE 与处理 | 安全审计备注 |
|---|---|---|---|---|---|---|
| swift-secp256k1（21-DOT-DEV，ex-Blockstream） | **0.23.2 精确钉版**（GitHub tag `0.23.2`；release published 2026-05-23T07:46:28Z，≥7 天冷静期；CocoaPods trunk 已收录 0.23.2） | BIP-32/44 HD 派生所需的 secp256k1 公钥运算（`P256K.Recovery.PrivateKey.publicKey`）与 **RFC6979 可恢复 ECDSA 签名**（`secp256k1_ecdsa_sign_recoverable`，noncefp = NULL；low-s 归一化与 recid 翻转由库内完成，与 Android T1.4 BLOCKER-1 修复后的语义一致） | modules/vault-core/ios `VaultCoreCore.podspec`（core，iOS 15.1）/ `VaultCore.podspec`（Expo bridge，16.4）→ `spm_dependency(s, …, requirement: { kind: 'exactVersion', version: '0.23.2' }, products: ['P256K'])`（**2026-10-17 路线修订**：原 `s.dependency` 预编译 xcframework 路线作废，理由与证据见本节顶部） | 活跃（repo pushed 2026-10-16；160 stars；非 fork、未归档；MIT） | **未发现已知 CVE**；供应链核对：CocoaPods trunk podspec 的 `source` 指向 GitHub release 制品 `P256K.xcframework.zip`，sha256 `0084b98982506a85dfbdf4568236c7c95cc77f10c2b8cdc0e567647844c00f77`（podspec 内钉死） | 信任依据：官方 21-DOT-DEV 仓库 + CocoaPods trunk 收录 + 制品 sha256 钉死；**库自述 pre-1.0**（docs.21.dev 明示生产前须读 Security Considerations）→ 记录为成熟度备注，与本工程 `[C]/[P]` 标注纪律一致；风险点：**pod 平台下限 core 15.1 / bridge 16.4**（`app.json` 同步 `ios.deploymentTarget = 18.0`）；内置 nonce 生成使 k 字节位于库内部内存、无法 `memset_s` 清零 → 清零评审记为 **[C]**（owner 2026-10-16 Q2 裁定）。缓解：版本钉死 0.23.2，升级走 R4；BIP-32 CKD 胶水/Keccak-256/RIPEMD-160 仍自写并由官方向量门禁。[^25^]
## 6d. Expo Router 传输/构建配套（apps/mobile，T2.1 R4 补记）

> **补记说明（2026-10-19，T2.1 代码评审 MAJOR-1）：** 下列六包在 T2.1 Task 1 经 `expo install` 装入
> `apps/mobile/package.json` 并已通过 lockfile 精确锁定，但当时漏记入本文件与 `specs/tech-stack.md`；
> 本补记按 R4 §1 补齐记录（所有包均为第一方 Expo/Meta 制品、SDK 57 解析、无版本手选）。
> **2026-10-19 T2.1a 补记（规划轮 D1，owner 批准）：** expo-font 行同此理由入表（SDK 绑定单元；lockfile 已含 ^57.0.4；直接行仅新增 importer 条目）。

| 包名 | 版本 | 用途 | 安装位置 | 维护活跃度 | 已知 CVE 与处理 | 安全审计备注 |
|---|---|---|---|---|---|---|
| expo-linking | ~57.0.13（SDK 解析） | 深层链接（scheme `nexvault`；路由 deep-link smoke 依赖） | apps/mobile | 高（Expo 官方） | 无 | expo-router **57.0.28** 必需 peer（`^57.0.13`）。信任依据：第一方制品随 SDK 发布；lockfile 精确。[^2^] |
| expo-constants | ~57.0.22（SDK 解析） | 应用常量/配置注入 | apps/mobile | 高（Expo 官方） | 无 | expo-router **57.0.28** 必需 peer（`^57.0.22`）。[^2^] |
| expo-status-bar | ~57.0.1（SDK 解析） | 状态栏样式（暗色主题 light 图标） | apps/mobile | 高（Expo 官方） | 无 | expo-router 手动安装文档钉版。[^2^] |
| react-native-safe-area-context | ~5.7.0（SDK 解析） | 安全区（SafeAreaView） | apps/mobile | 高（官方维护、Expo 生态标准） | 无 | expo-router 57 peer（`>=5.4.0`）。[^2^] |
| react-native-screens | ~4.26.2（SDK 解析） | 原生屏幕容器（Stack） | apps/mobile | 高（Software Mansion） | 无 | expo-router 57 peer（`^4.26.0`）。[^2^] |
| babel-preset-expo | ~57.0.15（devDependency，SDK 解析） | Babel 预设（NativeWind v4 babel preset 前置） | apps/mobile | 高（Expo 官方） | 无 | SDK 57 绑定单元（与 expo 57.0.30 同线）；仅在构建期使用。[^2^] |
| @babel/plugin-transform-react-jsx | 7.29.7（精确；构建期） | Babel JSX 转换；Gradle release 打包（`export:embed`）在 pnpm isolated 下按 app 根解析插件，缺此直接行会报 `Cannot find module` | apps/mobile（devDependency） | 高（Babel 官方） | 无 | 与 babel-preset-expo@57.0.15 的传递依赖同版；提升为直接行仅新增 importer 条目、零新包下载；构建期专用，不进运行时 bundle；**2026-10-19 T2.1a R4 修宪（owner 批准）**。[^2^] |
| expo-font | ~57.0.4（SDK 解析） | 字体加载（useFonts）：Inter 4 权重 TTF（regular/medium/semibold/bold）——原仓库 core/core-ui/src/main/res/font 复制 + res/raw/inter_ofl_license.txt 随包（OFL）；T2.1a 视觉对齐 | apps/mobile | 高（Expo 官方） | 无 | SDK 57 绑定单元：expo 基座依赖 ^57.0.4 已在 lockfile（L597），提升为直接行仅新增 importer 条目、依赖图不变；**2026-10-19 T2.1a 规划轮 R4 修宪（owner 批准，D1）**。[^2^] |
## 7. 工程化（root / CI）

| 包名 | 版本 | 用途 | 安装位置 | 维护活跃度 | 已知 CVE 与处理 | 安全审计备注 |
|---|---|---|---|---|---|---|
| eas-cli | CI 钉版（以 tech-stack.md 为准） | EAS Build/Update/Submit CLI | CI 环境（非运行时依赖） | 高（Expo 官方） | 无 | 信任依据：官方。风险点：OTA 签名私钥经 CLI 本地签名，私钥永不入 CI/仓库（R2）。缓解：`--private-key-path` 仅指向离线介质。[^21^] |
| expo-updates | ~57（随 SDK） | OTA 更新客户端；代码签名校验（rsa-v1_5-sha256）；fingerprint runtimeVersion | apps/mobile | 高（官方） | 无 | 风险点：其自身版本变更即触发 R3 热更禁区。缓解：fingerprint 策略自动拦截不兼容更新；签名验证失败拒绝加载。[^21^] |
| jest + jest-expo | **29.7.0 / 57.0.6（精确钉版）** | 单元测试；BIP-39/32 官方向量对拍在 jest/instrumentation 双侧 | apps/mobile（dev；2026-10-22 更正） | 高 | **sprintf-js@1.0.3（moderate，dev-only，无补丁版本）**；braces（high，既有，jest 增一条路径） | 备注：安全路径测试向量回归为质量门禁（B.6-3）；版本单独升级走修宪。**T2.2（2026-10-22，owner 批准「Add Jest tests now」）**：新增安装；lockfile `packages` **647 → 862（+215）**（`pnpm audit --json` → totalDependencies 862）；`pnpm audit` = 2 high（node-forge、braces）+ 4 moderate（uuid、decode-uri-component、**sprintf-js**、postcss-selector-parser）。**更正（review round 6 MAJOR-1）**：sprintf-js@1.0.3 在 `6ca3885` **不存在**，由 jest 路径引入：`@jest/transform → babel-plugin-istanbul → @istanbuljs/load-nyc-config → js-yaml → argparse → sprintf-js`（dev-only，无补丁版本）；braces 既有（tailwind/chokidar），jest 另增 `@jest/core → micromatch → braces` 路径；此前「均不在 jest 依赖路径」的表述**错误**。**处置**：接受为 **dev-only 残余风险**（不进入 web/native 产物，jest 仅开发/CI 运行），依据 owner 2026-10-22「Add Jest tests now」批准；如需零容忍则 revert jest（评审选项 c，未采纳）。 |
| @jest/globals | 29.7.0（精确；package.json 已钉 `29.7.0`） | 测试文件显式导入 describe/it/expect（类型 + 运行时）；jest 工具链成员 | apps/mobile（dev） | 高 | 无已知 CVE | 信任依据：jest 官方包（jestjs/jest），由 jest-expo 传递解析；直接 importer 仅用于测试代码（`src/**/*.test.ts`）；2026-10-22 owner 批准「Add Jest tests now」时一并纳入；lockfile 中 6ca3885 不存在 → 现 5 处引用。 |
| eslint（eslint-config-expo） | 随 SDK 57 模板钉版（**尚未安装**） | 静态检查；`no-restricted-imports`、R5 日志启发式规则 | apps/mobile（dev，待安装） | 高 | 无 | 备注：严格规则集为零 error 门禁（B.6-2），但 **2026-10-22 复核确认该依赖当前不在任何 package.json / lockfile 中，B.6-2 尚未接线**；安装需按 R4 提案并在 apps/mobile 落地后更新本行。 |
| pnpm（包管理） | ≥9（isolated 安装） | workspace monorepo；lockfile 钉版 | 工具链 | 高 | — | 风险点：hoisted 模式下其他 app 的原生依赖被 autolinking 误扫。缓解：默认 isolated；遇阻回退 `nodeLinker: hoisted` 并配 autolinking.exclude + Metro resolveRequest 双层屏蔽。[^22^] |

## 8. 全局禁用表（摘要，详表以 tech-stack.md 为准）

| 包 | 禁用理由 | 证据 |
|---|---|---|
| web3.js | 项目已归档终止（2025-03-04） | [^11^] |
| react-native-keychain | 维护停滞（近 6 个月仅 1 次 chore 提交）+ 未修 ACL issue | [^17^] |
| react-native-google-play-integrity | Snyk 判定 INACTIVE，2 年未更新 | [^15^] |
| nativewind v5 RC | 钉住带 CVE-2026-97724 的 worklets 0.10.1 | [^6^][^7^] |
| gluestack-ui v5 | 2026-06 供应链 RAT 投毒事件 + 官方放弃 Web/Next.js（与三端目标冲突） | [^6^] |
| TanStack Router | 官方明确不支持 RN | [^4^] |
| wallet-core（预装形态） | CVE-2025-66692 + String 助记词无法清零 + 体积 | [^20^] |
| text-encoding | npm 官方标记 DEPRECATED | [^10^] |
| @trustwallet/wallet-core（npm WASM） | WASM 内存不可清零，密钥进 JS 运行时，违反 R1 | [^20^] |
| @ronickg/react-native-bip39 / react-native-quick-bip39 | 助记词以 JS string 出 native，结构性违反 R1；低活跃 | [^18^] |

---

## 来源定义

[^1^]: Expo 官方 Changelog SDK 57（2026-06-30）与 https://expo.dev/sdk 版本表；`research/nexvault-rn_dim01.md` §1。
[^2^]: `research/nexvault-rn_dim01.md` §4.3（React 版本三端精确一致）。
[^3^]: GitHub necolas/react-native-web releases + npm 实测；`research/nexvault-rn_dim01.md` §4.1。
[^4^]: TanStack Router 官方安装文档（直读声明不支持 RN）；Expo 官方博客 "Expo Router v56"；`research/nexvault-rn_dim03.md` §2。
[^5^]: npm registry 实测（2026-10-14）+ Snyk；TanStack 官方 postmortem https://tanstack.com/blog/npm-supply-chain-compromise-postmortem ；`research/nexvault-rn_dim03.md` §1/§5。
[^6^]: NativeWind 官方文档（"V4 remains the stable release"）https://www.nativewind.dev/v5/getting-started/installation ；gluestack 供应链事件汇编；`research/nexvault-rn_dim03.md` §3。
[^7^]: OpenCVE/Rapid7 CVE-2026-97724（worklets <0.12.2 原型链污染）；npm 实测 reanimated 4.7.1 / worklets 0.13.0；`research/nexvault-rn_dim03.md` §4。
[^8^]: Shopify FlashList 官方 v2 文档 https://shopify.github.io/flash-list/docs/v2-changes/ ；`research/nexvault-rn_dim03.md` §4。
[^9^]: viem 官方文档 serializers/compatibility + wevm/viem#4002/#785；`research/nexvault-rn_dim04.md` §0/§1/§4。
[^10^]: GitHub LinusU/react-native-get-random-values releases（2.0.0 仅新架构）+ viem 官方 compatibility 页；`research/nexvault-rn_dim04.md` §2。
[^11^]: ChainSafe 官方博客 web3.js sunset（2025-03-04 归档）；paulmillr.com 审计史；`research/nexvault-rn_dim04.md` §5。
[^12^]: `research/nexvault-rn_dim12.md`（供应链事件与 npm 加固）；`research/nexvault-rn_cross_verification.md` §四-4。
[^13^]: Expo 官方 SecureStore 文档（Web: Not supported；~2048B 上限）；`research/nexvault-rn_dim02.md` §1/§6-1。
[^14^]: Expo 官方 ScreenCapture 文档 + expo/expo#48985；`research/nexvault-rn_dim02.md` §1-③。
[^15^]: Expo 官方 @expo/app-integrity 文档（alpha 警示）+ Snyk react-native-google-play-integrity INACTIVE；`research/nexvault-rn_dim08.md` C17。
[^16^]: `research/nexvault-rn_dim10.md`（@bam.tech/react-native-app-security 1.0.0 定案，双 SPKI pin）。
[^17^]: npm registry + GitHub API 实测 react-native-keychain 10.0.0 停滞；`research/nexvault-rn_dim06.md` E5。
[^18^]: `research/nexvault-rn_dim07.md` §0/§3/§7（四方法定案、禁用库盘点、测试向量）。
[^19^]: bitcoindev 邮件列表 v0.7.0 官宣（2025-07-22）+ openSUSE RPM 元数据（MIT/~1.3MB）+ bitcoin/secp256k1 README NDK CMake 命令；`research/nexvault-rn_dim07.md` §2。
[^20^]: NVD/cvefeed CVE-2025-66692 + Snyk wallet-core 条目 + wallet-core README（无官方 RN binding、String 助记词）；`research/nexvault-rn_dim07.md` §1。
[^21^]: docs.expo.dev/eas-update/code-signing 与 runtime-versions（一手核实 2026-10-14）；`research/nexvault-rn_dim11.md` §2。
[^22^]: Expo 官方 monorepo 指南 https://docs.expo.dev/guides/monorepos/ + SDK 55 pnpm 实战案例；`research/nexvault-rn_dim02.md` §4。
[^24^]: Maven Central `org.bouncycastle:bcprov-jdk18on/1.86`（`maven-metadata.xml` lastUpdated 20260911045244；jar sha256 `2af190b300cbb0b35e248ccf5f4a06b6072030aeb3da7a98ec73abe5b4cb371f`）+ OSV API 查询（1.86 受影响公告 0 条，全包 12 条均在 ≤1.85 修复）+ `specs/vault-core.md` §2.2/§6；T1.3 owner 批准（2026-10-15）。
[^23^]: 原 NexVault 仓库 `/home/superguo/Projects/NexVault/gradle/libs.versions.toml` 与 `specs/tech-stack.md`（JUnit 4.13.2 / androidx.test 1.3.0 / 1.7.0）；本仓库安装的 `expo-modules-core@57.0.22` `android/build.gradle` 测试配置；T1.2 owner 批准（2026-10-14）。
