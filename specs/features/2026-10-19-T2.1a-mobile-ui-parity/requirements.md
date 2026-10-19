# T2.1a — mobile UI visual parity — requirements

> Status: 规划轮完成（2026-10-19）；实施（Task 2–8）由下一会话执行（模型切换点，见 plan.md §11）| Constitution: AGENTS.md C.1/B.2/B.4 + 本 feature SKILL.md（sha256 `67892de8792e1e40815a198e81ebd5a213621b1b478f728af3ab286227b66e80`，`.agents/skills/nexvault-t2-1a-mobile-ui-parity/SKILL.md`，提交 `e2ac943`）
> 上位文件：`specs/roadmap.md` 阶段 1（T2.1a 修宪子弹，2026-10-19 route A/scope A）；`plans/3-day-implementation-plan.md` 修订记录 v1.3；`specs/ui-wallet.md`（§2.1/§2.3 本期修订）；`specs/tech-stack.md` §2（expo-font 行，D1）
> 参照权威：原仓库 `/home/superguo/Projects/NexVault` @ `3c7487f`（**实现代码为准**；`doc/03` 的紫 #6C5CE7 + Space Grotesk 草案作废）

## Goal

把 T2.1 的四页壳（Expo Router 57 Stack + NativeWind v4）视觉层对齐原 Kotlin 工程设计系统：暗色 M3 色板、Inter 排版、NexVaultCard/Button/TextField/TopBar 四原语、透明居中 TopBar（Home 无 bar）、5-tab 底部导航（Home/History/DApp/NFT/Settings；后三为 R5 "Not configured" 占位）、现有四页 + not-found 重皮。**零发明**：每个视觉值带源码证据注释；转录错误按 T1.3 向量转录发现同级处理。**T2.1 保持关闭不重开；T2.2 编号与内容不变。**

## Owner decisions（规划轮 Q/A 2026-10-19，ask_user_question ×5，全部答复）

| # | 问题 | 裁定 |
|---|---|---|
| D1 | GATE-R4-EXPOFONT：expo-font ~57.0.4 入 tech-stack §2 + dependencies §6d？ | **批准**——SDK 57 绑定单元（expo 基座依赖 ^57.0.4 已在 lockfile L597，store 已解析 4 个 peer 变体）；提升为直接行仅新增 importer 条目、依赖图不变；用途 = Inter 4 权重 TTF（OFL）加载 |
| D2 | ui-wallet §2.3 令牌块整体替换为原仓库暗色 M3 色板 + Inter/M3 字阶/形状/间距？ | **批准（按提案原文）**——每值带 file:line 证据；修订文本见 plan.md §4.2 |
| D3 | ui-wallet §2.1 路由表增 (tabs) 组（Home/History/DApp/NFT/Settings；后三占位）？ | **批准（按提案原文）**——其余路由表页保持 T4.x；修订文本见 plan.md §4.1 |
| D4 | T2.1-H3…H9 逐条裁定？ | **全部批准**——见 §Handoff re-read；H5 拆分 implement/replan；H9 经括注同步关闭 |
| D5 | MAN-016 视觉走查参考侧 = 原仓库 APK 截图（同机 Pixel 6a），agent 备截图 + owner 裁决？ | **批准**——APK 已存在（`/home/superguo/Projects/NexVault/app/build/outputs/apk/debug/app-debug.apk`，2026-10-18，60 MB）；若安装/解锁受阻按 skill 回退 code+inventory 转录并记录所用路径 |

## Handoff re-read（B.7-2 逐条裁定，上一条目 T2.1 开放项 H1–H9）

| Handoff | T2.1 状态 | 本规划轮裁定（D4，2026-10-19） | 落点 |
|---|---|---|---|
| T2.1-H1 | [x] closed（MAN-014 通过） | 无动作 | — |
| T2.1-H2 | [x] closed（MAN-015 通过） | 无动作 | — |
| T2.1-H3 真实数据（FlashList 真列表、Zustand、Query Provider） | [ ] open | **replan（维持）** | T2.2 |
| T2.1-H4 自定入口 polyfill 槽（index.ts：get-random-values + TextEncoder） | [ ] open | **replan（维持）** | T2.2 |
| T2.1-H5 tabs 导航 + 其余路由表页 | [ ] open | **implement（5-tab 底部导航，本期）+ replan（其余路由表页）** | 本期 / T4.x |
| T2.1-H6 iOS 路由 smoke | [ ] open | **保持** | T1.5-H1 / T4.1 |
| T2.1-H7 eslint/jest | [ ] open | **保持** | T1.1 closure |
| T2.1-H8 expo-env.d.ts 被 SDK 57 CLI gitignore → nativewind-env.d.ts 自持 `*.css` 声明 | 已规避（[x] 记录） | 无动作（复核确认） | — |
| T2.1-H9 expo-router 57.0.28 vs tech-stack 括注 57.0.26 | 已记录（备注） | **本期关闭**：R4 修订同步括注 → 57.0.28 | tech-stack.md |

约束：T2.1a 禁止任何屏幕 import vault-core（R7/R1）；无真实数据（R5）；无新依赖除 expo-font（R4）。

## 功能需求（FR）

- **FR-1 参考权威**：所有视觉值取自原仓库实现代码 @3c7487f（theme 六文件 + 四原语 + MainTab/MainScreen + HomeScreen 等，逐值 file:line 注释）；`doc/03` 作废；`/tmp/nexvault-original-ui-inventory.md` 仅作盘点摘要，值以代码为准。转录错误按 T1.3 向量转录发现同级处置。
- **FR-2 依赖（R4）**：仅新增 expo-font `~57.0.4`（D1 批准后安装）；Inter 4 TTF + OFL 许可证自原仓库 `core/core-ui/src/main/res/{font,raw}` 复制到 `apps/mobile/assets/fonts/`；`git diff package.json pnpm-lock.yaml` 仅此一行。NativeWind 锁 v4。
- **FR-3 令牌层**：`src/theme/tokens.ts` 重写为 ui-wallet §2.3 新块（色/字阶/形状/间距/尺寸/字体全表，逐值证据注释 `// Color.kt:DarkPrimary @3c7487f` 格式）；`tailwind.config.js` theme.extend 全量映射新键；`global.css` 背景改 CSS var（`--nv-bg`，接 tokens）；移除全部 T2.1 暖色值；WCAG 对比对全量计算，失败对**报告 owner、不改值**（色板固定）。
- **FR-4 原语组件**：`src/components/NexVaultCard.tsx`（fillMaxWidth、radius 16、cardSurface、1px cardBorder、elevation 0、内边距 16）、`NexVaultButton.tsx`（fillMaxWidth、h52、radius 16；primary/secondary/disabled/loading 态按 NexVaultButton.kt；label = labelLarge）、`NexVaultTextField.tsx`（h56、radius 12；聚焦/未聚焦/错误/禁用态按 NexVaultTextField.kt；error 文案 bodySmall）、`NexVaultTopBar.tsx`（透明居中 titleLarge 18/26 SemiBold；opaqueBackground 变体；可选 back；高度不强制——自测量）。零阴影/零 elevation。可交互原语带 accessibilityRole/accessibilityState。
- **FR-5 chrome 与导航**：`src/app/(tabs)/_layout.tsx` NativeTabs（以 expo-router v57 版本文档为准）：五 tab（图标 = MainTab.kt 的 filled/outlined 对）；容器 surface #121829、选中 primary、未选中 onSurfaceVariant、indicator primaryContainer、label labelSmall、高度 64（MainScreen.kt 证据）；若 NativeTabs 破坏 web 静态导出 → styled JS Tabs 回退（记录，不静默）。Home **无** TopBar（链选择器行领起，仅视觉、R5）。其余页面透明居中 TopBar。
- **FR-6 页面**：`index`（onboarding：身份文案 + 禁用 Import/Create，无任何助记词 UI）、`(tabs)/home`（自 `home.tsx` 迁入；无 bar；链选择器行 + 占位卡片/FlashList 段，原顺序；gutter 16）、`(tabs)/history|dapp|nft|settings`（"Not configured" 占位）、`receive`/`send`（重皮 + Not configured 占位）、`+not-found`（重皮）。GradientBackground 按原仓库用法镜像（grep 使用点）。StatusBar light、背景 = bg token（原 NexVaultTheme 强制 darkTheme=true，状态栏/导航栏 = background）。
- **FR-7 规格修订（规划轮执行）**：ui-wallet §2.1/§2.3（D2/D3）；tech-stack §2 expo-font 行 + expo-router 括注 57.0.28（D1/H9）；doc/dependencies.md §6d 行；MAN-016 登记；T2.1 validation.md H3–H9 行更新（D4）。
- **FR-8 证据纪律（B.4）**：全部门禁命令原文 + 退出码/观察结果 + 构件绝对路径入 validation.md；Android/Web 双侧证据；`*.log` 需 force-add（根 .gitignore）；证据转录持久化在 `evidence/`。

## Out of scope（本条目不做）

真实数据/polyfill（T2.2）、Zustand/TanStack Query、18 屏全量（T4.x）、动画/motion、eslint/jest（T1.1 H6）、任何其他依赖、iOS 构建（T1.5-H7/H8 → T5.3）、viem/链交互、T2.1 重开、doc/03 内容。

## 验收标准（AC ↔ 门禁 ↔ 用例）

| AC | 内容 | 绑定用例/门禁 | 验证方式（B.4 双侧） |
|---|---|---|---|
| AC-1 | `pnpm --filter mobile exec tsc --noEmit` exit 0 | B.6-1 | 命令 + 退出码 |
| AC-2 | `npx expo export --platform web -o /tmp/t21a-web` exit 0（≈9 静态路由）；产物 JS grep 无 `signDigest|importMnemonic|vault-core` | R7 / TC-UI-001 子集 | 命令 + 退出码 + 产物路径 + grep 观察结果 |
| AC-3 | R1 grep（apps/mobile）与 R5 console 启发式 grep 零命中 | R1/R5 门禁 | grep 观察结果 |
| AC-4 | 依赖漂移 = 仅两行 owner 批准（`expo-font ~57.0.4` + 构建期 `@babel/plugin-transform-react-jsx 7.29.7`）（`git diff apps/mobile/package.json pnpm-lock.yaml`） | R4 门禁 | diff 输出 |
| AC-5 | Android 路由 smoke：tabs ×5 + receive + send + not-found deep-link 0 crash | TC-UI-001 子集 / MAN-016 前奏 | adb 命令 + UI dump + 截图路径 |
| AC-6 | Web 路由 smoke：静态导出 curl 全路由 200 | TC-UI-009 子集 | curl 转录 |
| AC-7 | tokens.ts == ui-wallet §2.3 快照；zero-hardcode 漂移审计（hex/rgba 字面量、魔法间距、临场字号、重复组件）file:line 全清零或登记 | TC-UI-007 | 逐值比对 + 审计报告 |
| AC-8 | **MAN-016** 视觉走查：原 APK 截图 vs RN shell 并排（同机 Pixel 6a），owner 裁决 | TC-UI-007 人工侧 | 双侧截图 + owner 裁决记录 |
| AC-9 | §2.1/§2.3 修订、expo-font 行、H3–H9 裁定全部落盘（本文件 + 各 spec） | D2/D3/D4 | grep 复核 |

## 测试编号登记

- **MAN-016**：视觉对齐走查（Gherkin 见 `specs/pending-manual-test-cases.md`）；TC-UI-007 人工侧。
- TC-UI-001/007/009 子集以本条目门禁承载；完整 TC-UI-* 由后续 UI 条目闭合。
