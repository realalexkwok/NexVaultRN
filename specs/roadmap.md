# spec: roadmap — 阶段路线（顺序权威）

> Status: active | Date: 2026-10-14 | Constitution: yes
> 纪律：**Roadmap 顺序权威**（AGENTS.md B.2-2）——不得跳项、不得并行开两个 feature；认为顺序有误时先修本文件（=修宪）再动工。
> 关闭纪律（2026-10-24，AGENTS.md B.3-5）：**任何条目（含其 feature 分支）在 owner 于当前对话明确下令前一律不得关闭**——不得合并、push 主干、删除分支、在本文件/公告板标注「已关闭」或把 goal 标 complete；顺序权威不构成关闭授权。

## 阶段 0 — 预研（✅ 已完成，2026-10-14）

选型、密钥架构、热更方案、威胁模型、成本与合规全部定案并交叉验证通过。

- 产出：`doc/research-report.md`（预研报告）、`doc/dependencies.md`（依赖决策清单）、AGENTS.md（宪法）、`doc/review-report.md`（独立交叉校验）。
- 状态：已完成；`doc/` 自此为只读存档。

## 阶段 1 — 三天原型（当前阶段）

72 小时产出可演示三端原型（面试用途），小时级排期与每日验收门槛见 **`plans/3-day-implementation-plan.md`**（唯一排期权威）：

- Day 1：工程基建 + vault-core Android native 闭环（关键路径），G1 门槛。
- Day 2：iOS 移植 + 三端核心流程 + 安全 MVP 六项（集成联调与对照演示子集），G2 门槛。
- Day 3：EAS Update 签名 OTA + EAS Build + 演示打磨，G3 门槛。
- **2026-10-19 修宪（owner 裁定 route A / scope A）**：在 Day 1 末的 **T2.1 之后插入子条目 T2.1a — 视觉对齐原仓库**（T2.1 的后续条目：原 Kotlin 工程设计系统全量移植—暗色 M3 色板、Inter、NexVaultCard/Button/TextField/TopBar 原语、透明居中 TopBar、5-tab 底部导航）。**T2.1 保持关闭不重开**；**T2.2 编号与内容不变**（viem + polyfill + Zustand/Query + 余额只读，排在 T2.1a 之后）。排期与验收细节见 `plans/3-day-implementation-plan.md` 修订记录 v1.3；以原仓库 **实现代码**（`/home/superguo/Projects/NexVault` @ `3c7487f`）为准，其 `doc/03` 的紫 + Space Grotesk 草案不作依据。
- 验收纪律：双侧验证（命令+观察结果 / 真机走查），每日 22:00–23:00 收尾，Handoff 清单逐条裁定。

## 阶段 2 — 加固（roadmap，按序）

> 来源：doc/research-report.md 第 4 章（§4.2 各项「3 天工作量」溢出、§4.3 Roadmap 方案要点、§7.3 demo vs 真实防护级表）与各 spec §7。顺序即优先级，变更须修宪。

1. **检测链深化**（§4.2 溢出项，合计已评估 4.5–6 人日）：Root 检测完整向量（mounts/mountinfo 交叉全覆盖、行为链）、Frida inline hook 序言自检扩展、iOS Dopamine 对抗面研究、bundle 哈希 CI 两段构建回填流水线。
2. **vault-core NDK C++ 化**：BIP32/39/44 + libsecp256k1 v0.7.0 编译为共享 C++ 核心经 JSI/TurboModule 直连（§4.3 / spec: vault-core §7-1），Kotlin/Swift 退为薄壳；随后做常数时间保证与第三方密码学审计。
3. **APK v2/v3 签名块深度自解析**（§4.3.1）：v3 lineage 密钥轮换、Play App Signing 双指纹白名单、多 signer/v3.1 source stamp。
4. **o-mvll 控制流混淆与符号剥离**（§4.3.2）：仅检测/密钥 so 启用 `-fpass-plugin`；配合 `-fvisibility=hidden` + strip + RegisterNatives。
5. **关键函数 VMP 化**（§4.3.3 分层策略第二层）：仅签名材料拼装/检测裁决聚合 1–2 个函数，启动代价 <100ms。
6. **商用加固壳（触发条件制采购）**（§4.3.3 第三层）：触发条件=外部渗透测试/审计硬性要求或规模化盗版重打包事件；优先 Promon SHIELD（post-compile）或 DexGuard+iXGuard；梆梆/娜迦已排除。
7. **设备证明服务端化**：Play Integrity `decodeIntegrityToken` 服务端验签、Key Attestation 证书链校验 + 吊销列表、App Attest 服务端验证库定案。
8. **Secure Enclave P-256 包裹（iOS v2）**、StrongBox/Key Attestation 硬件归属绑定、Samsung Blockchain Keystore 可选增强。
9. **native UI 词格键盘完整版**（Compose/SwiftUI，JS 零接触助记词）、BIP-39 passphrase、多链扩展评估。
10. **Google Android Developer Verification 注册行动项**（半天，建议立即做，§4.4/§5.6 末）：注册官网分发包名 + 提交签名证书 SHA-256 备 2027 全球扩展。

## 阶段 3 — 工程化演进（持续）

- xprem 自建 OTA 迁移（触发条件：拒付 Production $199/月 或数据主权要求，§5.1）；Fastlane 商店 metadata 自动化（§5.5.2）；密钥轮换演练制度化（spec: ota-update §7-3）；EAS 档位随 MAU 演进（$0/≈$54/≈$449 三档，§5.4）。
- **无障碍验收（TalkBack / VoiceOver）**：MAN-018 的 Android TalkBack + iOS VoiceOver 两腿（`Invalid address` 恰好一次播报；T2.2 AC-10 / TC-UI-011）。owner 2026-10-23 裁定：整体 defer 至本阶段收尾（T3.1 H10 / H-T3.1-6）；设备到位即可执行，runbook 见 T3.1 validation。
