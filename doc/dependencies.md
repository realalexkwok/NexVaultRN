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
| nativewind | v4 稳定线（4.2.7，2026-09-14） | Tailwind 样式层，三端编译期 | apps/mobile | 高（v4 维护 + v5 积极开发中） | 自身无 CVE；**v5 RC 钉住 worklets 0.10.1（带 CVE-2026-97724）→ 裁决：不选 v5 RC** | 信任依据：官方文档明示 "V4 remains the stable release"。风险点：v5 RC「最新」诱惑。缓解：锁 v4；v5 转正后走修宪再评估（C5 裁决：最新 ≠ 最安全）。[^6^] |
| react-native-reanimated | 4.7（实测 4.7.1） | UI 线程 worklet 动画，60/120fps 基线 | apps/mobile | 高（Software Mansion） | 历史 CVE-2022-24373（<3.0，不适用）；4.6.0 `__proto__` 崩溃 issue 已关 | 信任依据：SDK 56/57 官方钉子版本。风险点：与 Hermes v1 的内存回归（57.0.9 已修）。缓解：配合 expo ≥57.0.21。[^7^] |
| react-native-worklets | ≥0.12.2（当前 0.13.0） | reanimated 4 的必需 peer | apps/mobile | 高 | **CVE-2026-97724：<0.12.2 原型链污染→远程 DoS；0.12.2 已修 → 锁 ≥0.12.2** | 信任依据：OpenCVE/Rapid7 记录。缓解：package.json 下限 + CI 解析结果断言。[^7^] |
| @shopify/flash-list | v2（2.3.3，2026-10-01） | 交易记录/地址簿长列表；免 estimatedItemSize | apps/mobile | 高（Shopify 生产验证） | 无已知公告 | 信任依据：新架构重写、官方文档完善。风险点：v2 要求新架构（SDK 55+ 强制，天然满足）。缓解：<200 行静态页退回 FlatList。[^8^] |

## 4. Web3 / 链交互（apps/mobile + packages/vault-core）

| 包名 | 版本 | 用途 | 安装位置 | 维护活跃度 | 已知 CVE 与处理 | 安全审计备注 |
|---|---|---|---|---|---|---|
| viem | 2.56.x（实测 2.56.9，2026-09-24） | 唯一 EVM 链交互库：构造→serializeTransaction→keccak256→native 签名→sendRawTransaction | packages/vault-core + apps/mobile | 极高（近 52 周 1038 commits，日发布节奏） | 本体无 CVE/GHSA | 信任依据：polyfill 面窄、~35kB vs ethers ~200kB、外部签名流为一等 API。风险点：生态仿冒包（`verify-contract-viem` 等 typosquat）。缓解：只装官方包名 + lockfile（R4）；#4002 typed-array 边界列入真机验证项。[^9^] |
| react-native-get-random-values | 2.0.0 | `crypto.getRandomValues` polyfill（native 随机源），入口第一行 import | apps/mobile | 稳定（小而精，2.0 起仅新架构，最低 RN 0.81） | 无已知 CVE | 风险点：**Chrome 远程调试下退化 Math.random** → R8 红线配套。缓解：入口 polyfills.ts 固定首行；release 走查禁调试。[^10^] |
| fastestsmallesttextencoderdecoder | 1.0.20 | TextEncoder polyfill（viem 官方推荐，防御性） | apps/mobile | 低频但稳定、纯 JS 无安全面 | 无 | 信任依据：viem 官方 compatibility 页推荐。备注：`text-encoding` 已被 npm 标记 DEPRECATED，禁用。[^10^] |
| @noble/curves | 2.4.x | **仅 devDependency**：联调/测试向量对拍用地址派生校验 | packages/vault-core（dev） | 极高（paulmillr；4 次第三方审计：Trail of Bits / Kudelski / Cure53 等，口径以 report §2.5.3 / dim04 为准） | 无已知未修 | 信任依据：viem 底层即 noble，零新增供应链面。风险点：误入生产密钥路径（JS 不可常数时间、不可可靠清零，作者官方明示）。缓解：devDependency 限定 + R1 grep 排除生产引用。[^11^] |
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

## 7. 工程化（root / CI）

| 包名 | 版本 | 用途 | 安装位置 | 维护活跃度 | 已知 CVE 与处理 | 安全审计备注 |
|---|---|---|---|---|---|---|
| eas-cli | CI 钉版（以 tech-stack.md 为准） | EAS Build/Update/Submit CLI | CI 环境（非运行时依赖） | 高（Expo 官方） | 无 | 信任依据：官方。风险点：OTA 签名私钥经 CLI 本地签名，私钥永不入 CI/仓库（R2）。缓解：`--private-key-path` 仅指向离线介质。[^21^] |
| expo-updates | ~57（随 SDK） | OTA 更新客户端；代码签名校验（rsa-v1_5-sha256）；fingerprint runtimeVersion | apps/mobile | 高（官方） | 无 | 风险点：其自身版本变更即触发 R3 热更禁区。缓解：fingerprint 策略自动拦截不兼容更新；签名验证失败拒绝加载。[^21^] |
| jest + jest-expo | 随 SDK 57 模板钉版 | 单元测试；BIP-39/32 官方向量对拍在 jest/instrumentation 双侧 | root / apps/mobile（dev） | 高 | 无 | 备注：安全路径测试向量回归为质量门禁（B.6-3）；版本随 SDK 模板锁定，单独升级走修宪。 |
| eslint（eslint-config-expo） | 随 SDK 57 模板钉版 | 静态检查；`no-restricted-imports`、R5 日志启发式规则 | root（dev） | 高 | 无 | 备注：严格规则集为零 error 门禁（B.6-2）。 |
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
[^23^]: 原 NexVault 仓库 `/home/superguo/Projects/NexVault/gradle/libs.versions.toml` 与 `specs/tech-stack.md`（JUnit 4.13.2 / androidx.test 1.3.0 / 1.7.0）；本仓库安装的 `expo-modules-core@57.0.22` `android/build.gradle` 测试配置；T1.2 owner 批准（2026-10-14）。
