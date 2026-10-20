# T2.1a — mobile UI visual parity — plan

> Status: 规划轮完成（2026-10-19）；owner 已批准（plan-mode 评审 "Approved"）；执行在 **T1/T2 边界暂停**（owner 指令：实施前切换模型）。
> 本文件是实施蓝图：决策完备，实施会话按 T0–T7 逐项执行，不得再发明设计决策。

## 1. Goal & success criteria

将原 Kotlin 工程设计系统（`/home/superguo/Projects/NexVault` @ `3c7487f`，**实现代码为准，doc/03 作废**）转录进 T2.1 的 NativeWind v4 壳：暗色 M3 色板、Inter 排版、四原语（NexVaultCard/Button/TextField/TopBar）、透明居中 TopBar（Home 无 bar）、5-tab 底部导航（Home/History/DApp/NFT/Settings；后三 "Not configured" 占位）、四页 + not-found 重皮。**零发明**：每个值带源码证据注释。

完成判据：§9 全部门禁通过、MAN-016 owner 裁决记录、修宪文件全部在 main、公告板行、分支删除、main push（B.3）。新依赖仅 expo-font（D1）。T2.1 保持关闭；T2.2 不变。

## 2. Owner decisions（规划轮 Q/A 2026-10-19，全部批准；原文见 requirements.md）

| # | 裁定 |
|---|---|
| D1 | GATE-R4-EXPOFONT **批准**：expo-font ~57.0.4（SDK 绑定单元；lockfile 已含 ^57.0.4；直接行仅新增 importer 条目）→ tech-stack §2 + dependencies §6d |
| D2 | ui-wallet §2.3 令牌块整体替换（§4.2 文本）**批准** |
| D3 | ui-wallet §2.1 增 (tabs) 组（§4.1 文本）**批准**；其余路由表页 T4.x |
| D4 | T2.1-H3…H9 裁定（§4.3 表）**批准** |
| D5 | MAN-016 参考侧 = 原仓库 app-debug.apk 截图（同机 Pixel 6a）；agent 备双侧截图 + owner 裁决 **批准** |

## 3. Authority & skill

- Skill：`.agents/skills/nexvault-t2-1a-mobile-ui-parity/SKILL.md`（161 行；sha256 `67892de8792e1e40815a198e81ebd5a213621b1b478f728af3ab286227b66e80`；提交 e2ac943）；feature 目录 SKILL.md 为 provenance 副本。
- 证据源（逐值 file:line 注释）：`core/core-ui/.../theme/{Color,NexVaultColors,NexVaultDimens,Type,Shape,NexVaultTheme}.kt`、`.../components/{NexVaultCard,NexVaultButton,NexVaultTextField,NexVaultTopBar,GradientBackground}.kt`、`app/.../navigation/{MainTab,MainNavigation}.kt`、`app/.../ui/main/MainScreen.kt`、`feature/feature-home/.../HomeScreen.kt`。
- 版本文档纪律（apps/mobile/AGENTS.md）：实施时 fetch 并记录（URL + 访问日期）NativeWind v4 theming、expo-router tabs（NativeTabs）、expo-font v57 文档；禁止凭记忆。

## 4. 规划轮修订（精确文本；实施会话照抄）

### 4.1 ui-wallet §2.1 路由表修订（D3）

(tabs) 组替换为：

```
  (tabs)/
    _layout.tsx            # NativeTabs（2026-10-19 T2.1a 实装：Home/History/DApp/NFT/Settings；
                           # 容器 surface、选中 primary、未选中 onSurfaceVariant、indicator primaryContainer、
                           # label labelSmall、高 64 —— 原仓库 MainScreen.kt/MainTab.kt 证据）
    home.tsx               # 资产总览（T2.1a 视觉对齐：无 TopBar、链选择器行领起；数据接入 T2.2/T4.x）
    history.tsx            # 交易历史 FlashList v2（T2.1a："Not configured" 占位，R5）
    dapp.tsx               # DApp 浏览器（T2.1a："Not configured" 占位；WalletConnect v2 后置）
    nft.tsx                # NFT 画廊（T2.1a："Not configured" 占位）
    settings.tsx           # 安全设置（T2.1a："Not configured" 占位；L0–L5 指示 T4.x）
```

并在路由表代码块后追加修订注记：`(tabs)` 组在 T2.1a 实装五 tab（DApp/NFT/Settings 先占位）；其余路由表页（unlock / send/confirm / address-book / token-detail / tx-detail / swap 等）维持后续条目（T4.x）；Home 无 TopBar（原仓库 HomeScreen.kt：链选择器领起）。

### 4.2 ui-wallet §2.3 令牌块修订（D2）——整块替换（暖色块作废）

```ts
// theme/tokens.ts —— tailwind.config 的 theme.extend 映射源
// 2026-10-19 T2.1a：全量对齐原仓库设计系统（/home/superguo/Projects/NexVault @ 3c7487f，实现代码为准）。
// 每个值注释 = 证据（文件:符号 @3c7487f）；转录错误按 T1.3 向量转录发现同级处理。
export const tokens = {
  color: {
    // Color.kt（dark scheme，生产为纯暗色）
    bg:                '#0A0E1A',  // Color.kt:DarkBackground
    surface:           '#121829',  // Color.kt:DarkSurface
    surfaceVariant:    '#1A1F35',  // Color.kt:DarkSurfaceVariant
    surfaceVariant2:   '#232842',  // Color.kt:DarkSurfaceVariant2
    primary:           '#2D5AF0',  // Color.kt:DarkPrimary
    onPrimary:         '#FFFFFF',  // Color.kt:DarkOnPrimary
    primaryContainer:  '#1E3A8A',  // Color.kt:DarkPrimaryContainer
    onPrimaryContainer:'#D8E2FF',  // Color.kt:DarkOnPrimaryContainer
    secondary:         '#00D4AA',  // Color.kt:DarkSecondary
    onSecondary:       '#003730',  // Color.kt:DarkOnSecondary
    secondaryContainer:'#005045',  // Color.kt:DarkSecondaryContainer
    onSecondaryContainer:'#70F7DC',// Color.kt:DarkOnSecondaryContainer
    tertiary:          '#7B61FF',  // Color.kt:DarkTertiary
    onTertiary:        '#2E0080',  // Color.kt:DarkOnTertiary
    tertiaryContainer: '#4A00B8',  // Color.kt:DarkTertiaryContainer
    onTertiaryContainer:'#E8DDFF', // Color.kt:DarkOnTertiaryContainer
    error:             '#FF4C6E',  // Color.kt:DarkError
    onError:           '#FFFFFF',  // Color.kt:DarkOnError
    errorContainer:    '#93000A',  // Color.kt:DarkErrorContainer
    onErrorContainer:  '#FFDAD6',  // Color.kt:DarkOnErrorContainer
    onBackground:      '#FFFFFF',  // Color.kt:DarkOnBackground
    onSurface:         '#FFFFFF',  // Color.kt:DarkOnSurface
    onSurfaceVariant:  '#A0A8C8',  // Color.kt:DarkOnSurfaceVariant
    onSurfaceDisabled: '#5A6180',  // Color.kt:DarkOnSurfaceDisabled
    outline:           '#404759',  // Color.kt:DarkOutline
    outlineVariant:    '#252B3F',  // Color.kt:DarkOutlineVariant
    // NexVaultColors.kt（dark；语义/加密专用色）
    positive:          '#00E676',  // NexVaultColors.kt:positive
    negative:          '#FF5252',  // NexVaultColors.kt:negative
    warning:           '#FFB74D',  // NexVaultColors.kt:warning
    info:              '#2196F3',  // NexVaultColors.kt:info
    cardSurface:       '#141928',  // NexVaultColors.kt:cardSurface
    cardBorder:        '#1E2440',  // NexVaultColors.kt:cardBorder
    shimmer:           '#1E2440',  // NexVaultColors.kt:shimmer
    shimmerHighlight:  '#2A3148',  // NexVaultColors.kt:shimmerHighlight
    textHigh:          '#FFFFFF',  // NexVaultColors.kt:textHigh
    textMedium:        '#A0A8C8',  // NexVaultColors.kt:textMedium
    textDisabled:      '#5A6180',  // NexVaultColors.kt:textDisabled
    gradientStart:     '#0A0E1A',  // NexVaultColors.kt:gradientStart
    gradientEnd:       '#121829',  // NexVaultColors.kt:gradientEnd
  },
  radius: {
    // NexVaultDimens.kt:cornerRadius*；组件级用法（NexVaultCard/Button/TextField.kt）
    xs: 4, sm: 8, md: 12, lg: 16, xl: 24,
    card: 16, button: 16, input: 12, chip: 8,
  },
  spacing: {
    // NexVaultDimens.kt:spacing*（+ 扩展步 spacing6/10/12/18/64）
    xxs: 2, xs: 4, sm: 8, md: 16, lg: 24, xl: 32, xxl: 48,
    s6: 6, s10: 10, s12: 12, s18: 18, s64: 64,
    gutterLg: 24,   // onboarding/receive/swap 屏 gutter（原仓库盘点）
    gutterMd: 16,   // home/token/history/send/nft 屏 gutter（原仓库盘点）
  },
  size: {
    buttonHeight: 52,      // NexVaultDimens.kt:buttonHeight
    inputHeight: 56,       // NexVaultDimens.kt:inputHeight
    bottomNavHeight: 64,   // NexVaultDimens.kt:bottomNavHeight
    icon: 24, iconSmall: 20, tokenIcon: 40, actionButton: 56,
    chartHeight: 180,      // NexVaultDimens.kt:chartHeight
    borderWidth: 1, borderWidthEmphasis: 1.5, borderWidthStrong: 2,  // NexVaultDimens.kt
  },
  type: {
    // Type.kt:NexVaultTypography（size/lineHeight/weight/letterSpacing；family = Inter）
    displayLarge:   { size: 36, lineHeight: 44, weight: '700', letterSpacing: -0.25 },
    displayMedium:  { size: 32, lineHeight: 40, weight: '700', letterSpacing: 0 },
    displaySmall:   { size: 28, lineHeight: 36, weight: '700', letterSpacing: 0 },
    headlineLarge:  { size: 28, lineHeight: 36, weight: '700', letterSpacing: 0 },
    headlineMedium: { size: 24, lineHeight: 32, weight: '600', letterSpacing: 0 },
    headlineSmall:  { size: 20, lineHeight: 28, weight: '600', letterSpacing: 0 },
    titleLarge:     { size: 18, lineHeight: 26, weight: '600', letterSpacing: 0 },
    titleMedium:    { size: 16, lineHeight: 24, weight: '600', letterSpacing: 0.1 },
    titleSmall:     { size: 14, lineHeight: 20, weight: '600', letterSpacing: 0.1 },
    bodyLarge:      { size: 16, lineHeight: 24, weight: '400', letterSpacing: 0.5 },
    bodyMedium:     { size: 14, lineHeight: 20, weight: '400', letterSpacing: 0.25 },
    bodySmall:      { size: 12, lineHeight: 16, weight: '400', letterSpacing: 0.4 },
    labelLarge:     { size: 14, lineHeight: 20, weight: '500', letterSpacing: 0.1 },
    labelMedium:    { size: 12, lineHeight: 16, weight: '500', letterSpacing: 0.5 },
    labelSmall:     { size: 10, lineHeight: 14, weight: '500', letterSpacing: 0.5 },
  },
  font: {
    family: 'Inter',  // Type.kt:NexVaultFontFamily（4 权重 TTF，OFL，自原仓库 res/font 复制）
    weights: { regular: '400', medium: '500', semibold: '600', bold: '700' },
  },
} as const;
```

修订注记追加：暖色块（`#1B1815`/`#C79A6B` 等）作废；每个值在 tokens.ts 以证据注释溯源；地址等宽 + 首尾高亮纪律随地址组件（T4.x）落地，原 `addressMono` 键不再保留（原仓库无 mono 字体）。

### 4.3 Handoff 裁定（D4）

| Handoff | 裁定 | 落点 |
|---|---|---|
| T2.1-H3 真实数据/Zustand/Query | replan（维持） | T2.2 |
| T2.1-H4 polyfill 自定入口 | replan（维持） | T2.2 |
| T2.1-H5 tabs 导航 + 其余路由页 | **implement（5-tab，本期）+ replan（其余）** | 本期 / T4.x |
| T2.1-H6 iOS 路由 smoke | 保持 | T1.5-H1 / T4.1 |
| T2.1-H7 eslint/jest | 保持 | T1.1 closure |
| T2.1-H8 expo-env.d.ts（已规避） | 无动作（复核确认） | — |
| T2.1-H9 expo-router 括注 57.0.26 | **本期关闭**（R4 修订同步 → 57.0.28） | tech-stack.md |

### 4.4 R4 行（D1）

- tech-stack.md §2 新行：`| expo-font | ~57.0.4 | apps/mobile | SDK 57 绑定单元（expo 基座依赖 ^57.0.4 已在 lockfile）；提升为直接行仅新增 importer 条目、依赖图不变；Inter 4 权重 TTF（OFL，原仓库 core-ui res/font 复制 + res/raw/inter_ofl_license.txt）；2026-10-19 T2.1a 规划轮 R4 修宪（owner 批准，D1） |`
- doc/dependencies.md §6d 追加同内容行（含 TTF 来源 + OFL 随包）。
- expo-router 行括注 `57.0.26` → `57.0.28`（关闭 H9）。

## 5. 实施任务（T2 起由下一会话执行）

### T0 分支（已完成，见 validation.md Task 0）
### T1 规划轮产物（本提交）
### T2 依赖 + 字体
- `cd apps/mobile && npx expo install expo-font`（→ ~57.0.4）；`git diff -- package.json pnpm-lock.yaml` = 仅 expo-font 行。
- 复制 `inter_{regular,medium,semibold,bold}.ttf` + `inter_ofl_license.txt`（记录源路径）→ `apps/mobile/assets/fonts/`；`useFonts` 挂载（键 inter-regular/medium/semibold/bold）；web 端 FontFace 验证（失败 → 记录 + Handoff）。
### T3 令牌与配置
- `src/theme/tokens.ts` = §4.2 块；`tailwind.config.js` theme.extend 全量映射新键（colors/radii/spacing/fontFamily/fontSize/lineHeight/letterSpacing）；`global.css`：`:root { --nv-bg: #0A0E1A; }` + `html, body, #root { background-color: var(--nv-bg); }`（关闭 T2.1 NIT：硬编码 `#1B1815` → CSS var）；WCAG 对比全对计算，失败对**报告 owner、不改值**。
### T4 原语（`src/components/`，零硬编码，零阴影，a11y 属性）
- NexVaultCard：fillMaxWidth / radius 16 / cardSurface / 1px cardBorder / elevation 0 / 内边距 16。
- NexVaultButton：fillMaxWidth / h52 / radius 16；primary = primary/onPrimary，disabled = onSurface 12%/38%；secondary = outlined，border/content primary（disabled border onSurface 12%）；isLoading spinner 20dp stroke 2；label = labelLarge；contentPadding h24 v16。
- NexVaultTextField：h56 / radius 12；聚焦边框 primary、未聚焦 cardBorder、错误 error；label textMedium→primary（聚焦）；文本 textHigh；禁用 textDisabled；placeholder/prefix textMedium；error 文案 bodySmall + error 色 + start 16/top 4。
- NexVaultTopBar：透明默认（opaque 变体 = cardSurface）；居中标题 titleLarge 18/26 SemiBold、textHigh、单行 ellipsis；可选 back（tint textHigh）；高度不强制（status inset + 内容行自测量）。
### T5 chrome + 导航 + 页面
- `src/app/(tabs)/_layout.tsx` NativeTabs（按 fetch 的 v57 文档）：五 tab（MainTab.kt filled/outlined 图标对）；容器 surface / 选中 primary / 未选中 onSurfaceVariant / indicator primaryContainer / label labelSmall / 高 64。**回退**：NativeTabs 破坏 web 静态导出 → styled JS Tabs（同令牌），记录不静默。
- `home.tsx` → `(tabs)/home.tsx`：无 TopBar、链选择器行（仅视觉）、占位卡片/FlashList 段、gutter 16；history/dapp/nft/settings = "Not configured" 占位；index/receive/send/+not-found 重皮（onboarding 保持禁用 Import/Create，无助记词 UI）；GradientBackground 按原仓库使用点镜像；StatusBar light、背景 = bg。
### T6 自动门禁（命令 + 观察结果 → validation.md）
tsc exit 0 · export exit 0（≈9 静态路由）· R7 grep 零 · R1 grep 零 · R5 grep 零 · 漂移 diff = 仅 expo-font · expo-doctor · drift audit（zero-hardcode，file:line 全清零或登记）。
### T7 smoke + MAN-016 + 关闭
- Android：Pixel 6a dev build + Metro 8082 + `adb reverse tcp:8082 tcp:8082`；deep-link tabs ×5 + receive + send + not-found；UI dump + 截图；0 crash。
- Web：serve /tmp/t21a-web，curl 全路由 200（转录持久化 evidence/）。
- MAN-016（D5）：agent 备双侧截图（原 APK vs RN shell，同机）→ owner 裁决 → validation + MAN 台账记录。
- Handoff 清单（T2.2/T4.x）→ 公告板行（附 validation 路径）→ `git switch main && git merge --no-ff feature/T2.1a-mobile-ui-parity` → 删分支 → `git push origin main`（一并发布 e2ac943 skill 提交）。

## 6. 路由图

新增 (tabs) 组（URL 新增 /history /dapp /nft /settings；/home /receive /send 不变；index = onboarding）。无 vault-core 引用；无数据层；公开 API = 4 原语 + tokens + 字体资产。应用身份/包名不动。

## 7. AC ↔ 门禁（细则见 requirements.md §验收标准）

AC-1 tsc · AC-2 export+R7 · AC-3 R1/R5 · AC-4 漂移 · AC-5 Android smoke · AC-6 Web smoke · AC-7 令牌快照+zero-hardcode（TC-UI-007）· AC-8 MAN-016（TC-UI-007 人工侧）· AC-9 修订+H3–H9 落盘。

## 8. 边界与失败模式

公告板 rebase 冲突（已解决，并存）；NativeTabs web 导出失败 → JS Tabs 回退（记录）；原 APK 走查受阻 → code+inventory 转录（记录路径）；Metro 8082 / 设备重配对；typed routes 再生成；expo-font web 加载（export 门禁验证）；R5 无假数据；`*.log` force-add；偏离本计划须再经 owner 批准。

## 9. 非目标

无数据/polyfill（T2.2）、无 Zustand/Query、无 18 屏（T4.x）、无动画、无 eslint/jest、无其他依赖、无 iOS 构建、不重开 T2.1、不用 doc/03。

## 10. Git 关闭序列（B.3，owner 触发）

提交惯例 `feat(T2.1a): …` / `docs(T2.1a): …`；关闭 = 公告板行提交 → main `--no-ff` 合并（治理提交 63c73c5 随并入 main）→ 删分支 → push main。命令 + 观察结果入 validation.md。

## 11. 模型切换点

**T1 完成后停止**（本提交即 T1 全部产物）；下一模型从 T2 开始，逐项执行本蓝图，先读 AGENTS.md Part A/B + 本 feature 三件套 + SKILL.md + fetch 版本文档，再写 app 代码。
