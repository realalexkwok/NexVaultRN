# T2.1 — mobile UI shell — requirements

> Status: planning (2026-10-18) | Constitution: AGENTS.md C.1/B.2/B.4 + 本 feature SKILL.md（sha256 `133e4069e47d86c5710a30b81841015275f9dcd5338a3f8c60991ce0fb5e21bc`，`.agents/skills/nexvault-t2-1-mobile-ui-shell/SKILL.md`，提交 `407d5ae`）
> 上位文件：`plans/3-day-implementation-plan.md` T2.1（并行 UI）；`specs/ui-wallet.md`（路由表/设计令牌/红线）；`specs/tech-stack.md` §2（四行 UI 钉版）

## Goal

将空白 Expo 模板（`apps/mobile`：`App.tsx` 入口、零 UI 依赖）改造为四页导航壳：根 **Stack** 路由（Expo Router 57）+ **NativeWind v4** 设计令牌样式 + **Reanimated 4.7.1 / worklets 0.13.0 / FlashList 2.3.3** 钉版接入。**仅空壳**：无钱包逻辑、无真实数据、无动画；所有本需配置/RPC 的面一律显示 **"Not configured"**（R5）。

四页（owner 2026-10-18 定案，见 skill Scope）：`/`（index = onboarding 空壳）、`home`、`receive`、`send`；外加根 `_layout.tsx` 与 `+not-found.tsx`。

## Owner decisions（规划轮 Q/A，2026-10-18，全部已答复）

| # | 问题 | 裁定 |
|---|---|---|
| D1 | T1.5-H6：swift-secp256k1 0.23.2 集成路线（预编译 xcframework → SwiftPM 源码构建）追认？ | **追认（ratify）**——tech-stack.md 行 + dependencies.md §6c 措辞定稿、摘除"待 owner 追认"标记；T1.5-H6 关闭 |
| D2 | 四条 Handoff 处置（§Handoff re-read） | **全部确认** |
| D3 | R4 修宪：tailwindcss 3.4.17（nativewind 4.2.7 必需 peer） | **批准**——精确钉版，apps/mobile devDependency |
| D4 | 路由 smoke 载体（Android deep-link + 人工点走 / Web curl + 浏览器走查） | **载体确认；人工侧由 owner 执行并记录** |
| D5 | 主题 | **按 ui-wallet §2.3 暗色令牌**；`app.json userInterfaceStyle: "dark"` |
| D6 | 实施中 R4 补充（web export 实证失败后）：`react-native-css-interop` 提升为直接行 | **批准**——精确 0.2.7（= nativewind 4.2.7 自身钉版），依赖图不变，pnpm isolated 解析修复 |

## Handoff re-read（B.7-2 逐条裁定，上一条目 T1.5 与 T1.3 开放项）

| Handoff | 裁定 | 落点 |
|---|---|---|
| T1.3-H8（JS→native 桥接调用未被行使；owner 2026-10-16 已裁定 replan） | **replan（重申）** | T4.1 dev-build 页面 smoke（导入助记词 → `deriveAddress` 走 AsyncFunction/JS 桥接） |
| T1.5-H2（iOS 桥接调用） | **replan** | T4.1 |
| T1.5-H1（iOS 真机门） | **保持开放**——文档化环境阻塞：无合格真机（iPad mini 4 / iOS 15.8.8 低于 app 18.0 下限；Xcode 26.5 无法对 15.8.8 跑 XCTest）；`run-device-tests.sh` 就绪 | T4.1 真机运行，随后 MAN-012/013 |
| T1.5-H6（SPM 路线追认） | **implement（D1 追认）**，本规划轮以 spec 修订执行 | 本文件 Task 0 |
| T1.5-H7（EAS 信任旗标 `-skipPackagePluginValidation -skipMacroValidation`） | **replan** | 首个 EAS iOS build（T5.3） |
| T1.5-H8（Release/device pod 装配，`PRODUCTION=1 pod install`） | **replan** | 首个 Release/EAS iOS build（T5.3） |
| T1.5-H3/H4（at-rest 包裹 / mlock 探针） | 维持原记录（→ security-ios / 真机 MAN-013） | security-ios / 真机运行 |
| T1.1 H6（eslint/jest 脚手架债） | **留在 T1.1 closure**（T2.1 不加） | T1.1 closure |

约束：T2.1 **禁止**任何屏幕 import vault-core（R7/R1）；T4.1 才是桥接 smoke 载体。

## 功能需求（FR）

- **FR-1 路由接线**：`package.json main = "expo-router/entry"`；路由目录 `src/app/`（apps/mobile/AGENTS.md 覆盖官方 `app/`）；根 `_layout.tsx` Stack（`expo-router`）；typed routes 开启（`experiments.typedRoutes: true`）；scheme 保持 `nexvault`；`+not-found.tsx`。删除 `App.tsx`/`index.ts`（T2.2 的 polyfill 自定入口路径记入 Handoff）。禁止直接导入 `@react-navigation/*`（B.6-2）。
- **FR-2 四页空壳**：index（onboarding：应用身份文案 + 禁用态 Import/Create 占位按钮，**无任何助记词 UI/逻辑**）、home（FlashList 静态占位行，标注 Not configured，无假余额）、receive（Not configured 占位，地址现取是 T4.1 的事）、send（Not configured 占位 + 禁用表单桩）。页间导航 `Link`/`router.push`。
- **FR-3 NativeWind v4**：`babel.config.js`（`["babel-preset-expo", { jsxImportSource: "nativewind" }]` + `nativewind/babel`）、`metro.config.js`（`withNativeWind(config, { input: "./global.css" })`）、`tailwind.config.js`（content 覆盖 `./src/**/*.{js,jsx,ts,tsx}`、`nativewind/preset`、theme.extend ← tokens）、`global.css`（@tailwind 三指令）、`nativewind-env.d.ts`（triple-slash reference）。四页全部消费 `src/theme/tokens.ts`（与 ui-wallet §2.3 逐值一致），零硬编码色值。暗色主题（D5）。
- **FR-4 Reanimated/Worklets 钉版**：4.7.1 / 0.13.0 精确钉版；解析结果断言；**本条目零动画代码**（motion 纪律归 T4.x）。
- **FR-5 FlashList v2**：2.3.3 精确钉版；仅 home 静态占位列表（真实列表随 T2.2/T4.x 数据而来）。
- **FR-6 R4**：仅安装 tech-stack 批准行（expo-router ~57 + 官方 SDK-57 peer 集 + 四行 UI 钉版 + tailwindcss 3.4.17（D3 修宪）+ babel-preset-expo（SDK 绑定单元））；`git diff` 证明零额外漂移；Expo Go 出局（原生依赖），dev build 验证（Invariant 8）。
- **FR-7 R1/R5/R7**：`src/` 零 vault-core 引用、零密钥语义标识符；web 产物 grep 无 `signDigest|importMnemonic|vault-core`；无假数据；无敏感日志。
- **FR-8 Handoff 裁定入档**（§Handoff re-read 表逐字记录）。

## Out of scope（本条目不做）

- 任何钱包业务逻辑、状态库（zustand/Query 归 T2.2）、viem/polyfill（T2.2）、链上数据、WalletConnect。
- 助记词输入页与桥接调用（T4.1；R1 下 index 仅占位按钮）。
- 动画/动效（Reanimated 仅接入不写 motion）。
- eslint/jest/prettier 引入（T1.1 H6）。
- iOS 构建相关的一切（T1.5-H7/H8 已 replan）。
- ui-wallet 路由表的其余页面（tabs/unlock/confirm 等属后续条目）。

## 验收标准（AC ↔ 门禁 ↔ 用例）

| AC | 内容 | 绑定用例 | 验证方式（B.4 双侧） |
|---|---|---|---|
| AC-1 | `tsc --noEmit` exit 0（strict 已开） | B.6-1 | 命令 + 退出码 |
| AC-2 | `expo export --platform web` exit 0；产物 JS grep 无 `signDigest|importMnemonic|vault-core` | TC-UI-001（web grep 子集） | 命令 + 退出码 + 产物路径 + grep 观察结果 |
| AC-3 | Android 路由 smoke：四路由 deep-link 启动 + 截图（自动侧）；owner 点走（人工侧） | TC-UI-001 子集 / **MAN-014** | `adb` 命令 + 截图路径 + owner 走查记录 |
| AC-4 | Web 路由 smoke：导出产物本地 serve + curl 四路由 200 + 标记文案（自动侧）；owner 浏览器走查（人工侧） | TC-UI-009 子集 / **MAN-015** | curl 输出表 + owner 走查记录 |
| AC-5 | 依赖漂移 = 仅批准行 | R4 门禁 | `git diff apps/mobile/package.json pnpm-lock.yaml` |
| AC-6 | R1 grep 干净 + 占位文案 "Not configured" 就位 | R1/R5 门禁 | grep 观察结果 + 页面文案检查 |
| AC-7 | tokens.ts 与 ui-wallet §2.3 一致（快照） | TC-UI-007 子集 | 文件逐值比对 |

## 测试编号登记

- **MAN-014**：Android 路由点走（onboarding → home → receive → send），`specs/pending-manual-test-cases.md` 登记，依赖 Pixel 6a。
- **MAN-015**：Web 浏览器路由走查（四路由 + console 无报错），桌面 Chrome/Safari。
- TC-UI-001/007/009 子集以本条目门禁承载；完整 TC-UI-* 由后续 UI 条目闭合。
