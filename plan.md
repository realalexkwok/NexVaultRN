# NexVault RN/Web 跨端移植预研 + SDD 工程文件 — 执行计划

## 目标交付物（全部写入 /mnt/agents/output/nexvault-rn-sdd/）
1. `doc/research-report.md` — 完整预研报告（选型对比表、威胁模型、攻击-防御映射、成本表，结论标注来源）
2. `AGENTS.md` — 适配 RN/Expo 跨端工程的新版（目录约定、代码规范、安全红线可执行检查规则）
3. `specs/` — 6 个 SDD 规格文件（vault-core / security-android / security-ios / web3-adapter / ota-update / ui-wallet）
4. `plans/` — 3 天小时级实施计划（粒度 ≤ 半天、依赖关系、风险预案）
5. 骨架代码决策清单（依赖表：包名+版本+用途+安全审计备注）

## 当前时间基准：2026-10-14（所有"最新版本/价格/新规"均以此为准做时效性核查）

---

## Stage 0 — 原仓库侦察（explore 子代理）
- 读取 https://github.com/realalexkwok/NexVault 的 AGENTS.md、specs/、plans/、模块结构，输出工程纪律摘要供 Stage 2/3 对齐风格。
- 不加载技能，用 web 工具完成。

## Stage 1 — 深度调研（加载技能：deep-research-swarm）
并行 4 个调研子代理（Route A 宽搜索 + 定点深挖）：
- **R1 技术栈选型**：Expo 最新稳定 SDK（核实时效）、Prebuild/Dev Build、expo/metro web、expo-modules/config plugin 对 Keystore/NDK/FLAG_SECURE/签名读取的覆盖；TanStack Query+Zustand、Expo Router vs TanStack Router、Tamagui/NativeWind/gluestack；单工程 vs pnpm workspace；ethers v6 vs viem + Hermes polyfill。
- **R2 密钥安全架构**：Android Keystore/StrongBox/TEE Key Attestation；secp256k1 包裹方案；iOS Secure Enclave（react-native-keychain / expo-secure-store 能力边界）；Web 降级只读观察钱包论证；BIP32/39/44 native 闭环（expo-modules/JSI、Trust wallet-core、libsecp256k1）；内存安全；Play Integrity 三级 verdict + 服务端验 JWT；分级处置策略。
- **R3 防篡改反逆向**：native 签名校验、JS bundle 哈希自检、Root/Magisk/Zygisk/Shamiko 检测、IOSSecuritySuite（Dopamine rootless）、反调试/Frida 多信号检测；Android Developer Verification 新规时效核查；梆梆/娜迦 vs DexGuard/iXGuard vs Promon 对比；TLS pinning 库；性能分级。
- **R4 热更新工程化**：EAS Update 最新 API 与定价（核实时效：1K/10K/100K MAU 成本表）、代码签名、runtimeVersion 策略、channel/branch、灰度、首屏影响；自建 xprem/expo-open-ota、hot-updater 评估；EAS Build vs Fastlane；iOS 3.3.2 合规；开源库维护活跃度与 CVE 检查贯穿全部课题。
- 产出：4 份带来源的调研简报 → Orchestrator 合并为《调研摘要 digest.md》。

## Stage 2 — 报告撰写（加载技能：report-writing）
- Writer A：基于 digest 撰写 `doc/research-report.md`（对比表、威胁模型、攻击-防御映射、成本表、来源标注、合规注记、MVP vs roadmap 分级）。

## Stage 3 — SDD 工程文件（并行 3 个 Writer，输入同一 digest + 报告结论）
- Writer B：`AGENTS.md` + 骨架依赖决策清单
- Writer C：`specs/` 6 个模块规格（目标/接口/威胁模型/验收标准/测试用例）
- Writer D：`plans/` 3 天小时级计划 + 依赖 + 风险预案

## Stage 4 — 交叉校验与集成
- Reviewer 子代理校验：安全承诺分级一致性（已实现/提高成本/占位）、3 天工期硬约束、引用可核实性、文件间一致性（AGENTS.md 红线 ↔ specs 验收标准 ↔ plans 排期）。
- 不通过则回炉对应 Writer。
- 最终交付目录 + 汇总说明。
