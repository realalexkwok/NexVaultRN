# 《NexVault RN/Web 跨端移植预研报告》大纲

最终产物：/mnt/agents/output/nexvault-rn-sdd/doc/research-report.md
语言：中文（代码/接口命名英文）。引用：[^N^] 行内标注 + 章末来源定义，合并时去重为统一参考表。
铁律：所有安全承诺三级标注 [P]已实现防护 / [C]仅提高攻击成本 / [S]占位接口；3 天不可验证项一律 roadmap；库选型含维护活跃度+CVE 检查；合规注记（不面向中国大陆）。

## sec01 摘要与核心结论（~1500 字，Round 2 写）
- 一页结论：选型定案、密钥架构定案、三端信任分级、MVP/roadmap 分界、成本结论
- 七大跨维度洞察（来自 nexvault-rn_insight.md）

## sec02 技术栈选型（~4000 字，输入 dim01-04）
- 2.1 Expo SDK 57 vs bare RN 对比表 + 定案（CNG + Dev Build，含 Expo Go 边界）
- 2.2 六项原生能力逐项验证表（Keystore/NDK/FLAG_SECURE/Application/签名读取/Enclave → expo-module 或 config plugin 路径，3 天可行性标注）
- 2.3 数据层（TanStack Query 5.104 + Zustand 5.0）/ 路由（Expo Router 57，TanStack Router 排除依据）/ UI（NativeWind v4 定案，Tamagui 备选，gluestack 排除）对比表
- 2.4 Monorepo：pnpm workspace（apps/mobile + packages/vault-core）vs 单工程
- 2.5 Web3 层：viem 2.56 vs ethers v6 对比表 + polyfill 清单（get-random-values@2.0.0 + fastestsmallesttextencoderdecoder）+ RN Web 差异 + noble/curves 降级路径

## sec03 密钥安全架构（核心章节 ~6000 字，输入 dim05-08 + 仓库侦察）
- 3.1 三端信任分级总表（Android/iOS 全功能、Web 只读）与红线「私钥不出 native、Web 不持私钥」
- 3.2 Android：Keystore 不支持 k1 的实证 + AES-256-GCM wrap key（StrongBox→TEE 降级）+ per-use 生物识别绑定方案定案；setUnlockedDeviceRequired API 12–34 bug 门控
- 3.3 iOS：expo-secure-store 定案（react-native-keychain 停滞排除）+ AES 密钥生物识别 ACL 存 Keychain；SE P-256 包裹列 v2
- 3.4 Web：只读观察钱包 + WalletConnect(Reown) 桥接论证（供应链投毒实证、无 attestation、无法 pinning）
- 3.5 native 闭环：expo local module 四方法（importMnemonic/deriveAddress/signDigest/destroy）+ ArrayBuffer/opaque handle + zeroize；libsecp256k1 v0.7.0；wallet-core 为备选（CVE-2025-66692）；noble/curves 仅联调降级
- 3.6 内存安全清单：禁 String、buffer 清零、mlock（iOS 无保证如实标注）、FLAG_SECURE（expo-screen-capture）、安全键盘、禁剪贴板
- 3.7 威胁案例防御论证：「重打包+明文上报 C&C」木马逐层失效分析
- 3.8 设备证明双轨：Play Integrity 主（三级 verdict + 服务端验 JWT）+ Key Attestation 降级（无 GMS/模拟器）；@expo/app-integrity（alpha 锁版）
- 3.9 分级处置决策表（L0–L5 × 动作矩阵，不硬退出的论证，MASVS-R 依据）

## sec04 防篡改与反逆向（~6000 字，输入 dim09、dim10、dim12 平台层）
- 4.1 Hermes=明文前提（hermes-decomp 实证）与代码分层原则
- 4.2 MVP 六项逐项：native 签名校验（SHA-256 指纹嵌入 .so）、JS bundle 哈希自检、Root/Magisk/KernelSU/Zygisk/Shamiko 检测（RootBeer 0.1.2 向量过时注记）、iOS 越狱（IOSSecuritySuite + Dopamine rootless /var/jb）、反调试（ptrace/PT_DENY_ATTACH 审核现状）、Frida 多信号交叉；每项标 [P]/[C]/[S]
- 4.3 Roadmap：APK v2 签名块 native 自解析、o-mvll 混淆、商用加固对比表（梆梆/娜迦排除理由、DexGuard ~$44k/年、Promon、iXGuard、启动代价与「只 VMP 核心 so」分层）、反 Frida 深化/inline hook 自检
- 4.4 平台层：Android Developer Verification（2026-09-30 四国已生效、官网直链豁免但 2027 须注册）与本地校验互补论证
- 4.5 网络层 MVP：@bam.tech/react-native-app-security 双 SPKI pin + 轮换；Web 边界；HMAC+nonce+时间戳防重放（区分 API 层与 EIP-155 链上层）
- 4.6 性能红线：冷/热路径分级表、检测异步化、native 签名+乐观 UI、Reanimated 4.7/FlashList v2、120Hz

## sec05 热更新与工程化（~5000 字，输入 dim11、dim12）
- 5.1 EAS Update 托管论证 + 自建备选（xprem 673★ 活跃为首选迁移路径；hot-updater 需换客户端 SDK 不推荐；Capgo 不适用）
- 5.2 EAS Update 落地清单：fingerprint runtimeVersion 定案、代码签名（rsa-v1_5-sha256、私钥离线、锁 Production 档红线）、channel=环境/branch 切指、--rollout-percentage 原生灰度 + 设备 ID 哈希补充、checkAutomatically/fallbackToCacheTimeout 首屏规避、bsdiff 默认（-58%）
- 5.3 热更禁区判定规则（native 依赖/权限/expo-updates 版本变更必须发新版）+ iOS 2.5.2/DPLA 3.3.1(B) 合规边界
- 5.4 成本表：1K=$0 / 10K≈$54 / 100K≈$449 每月（假设 4 次更新×2MB，注明假设与裁决过程）
- 5.5 构建：EAS Build profile（dev/preview/prod + remote credentials）、定价、Fastlane roadmap、「构建层与热更层解耦」
- 5.6 三端 CI/CD 最小流水线 + 回滚现实（OTA 秒级 vs 商店不可回滚 → staged rollout）+ 官网 APK 合规操作
- 5.7 供应链红线：ignore-scripts、lockfile、min-release-age、Socket；Shai-Hulud/Bybit 案例

## sec06 威胁模型与攻击-防御映射（~4000 字，输入 dim05/06/08/09/10/12 + cross_verification）
- 6.1 威胁建模范围与资产定义（STRIDE 简化 + 钱包特化）
- 6.2 攻击-防御映射总表（攻击向量 × 防御层 × [P]/[C]/[S] × 残余风险）
- 6.3 安全承诺分级总表（全系统所有机制的诚实标注）
- 6.4 合规注记：不面向中国大陆（禁令）、geo-fence、隐私披露、海外地区（旅行规则等简注）

## sec07 3 天工期计划总览与风险（~3000 字，Round 2 写，引用 plans/ 产物）
- 7.1 小时级排期概览（Day1/2/3 验收标准），细节指向 plans/
- 7.2 demo 级 vs 真实防护级标注表
- 7.3 风险预案矩阵（expo-modules 封装降级路径等）
