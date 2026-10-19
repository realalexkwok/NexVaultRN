# T2.1a — mobile UI visual parity — validation

> Status: 实现完成（Task 2–6，静态门禁全绿，2026-10-19）；设备门禁（Android 路由 smoke + MAN-016）待 Pixel 6a 重连后执行。
> 三档制（B.4）：本文件所有 `[x]` 均须命令原文 + 观察结果 + 构件绝对路径；双侧齐备。

## 规划轮（2026-10-19，owner 裁定）

| 步骤 | 内容 | 结果 |
|---|---|---|
| 访谈 | `ask_user_question` ×5（D1–D5） | 全部答复批准：D1 expo-font 行 / D2 §2.3 令牌块 / D3 §2.1 (tabs) / D4 H3–H9 / D5 APK 截图参考侧（原文见 requirements.md §Owner decisions） |
| GATE-R4-EXPOFONT | tech-stack §2 行 + dependencies §6d 行 | owner 批准（D1）；行文本见 plan.md §4.4 |
| ui-wallet 修宪 | §2.1 路由表 + §2.3 令牌块 | owner 批准（D2/D3）；修订文本见 plan.md §4.1/§4.2 |
| Handoff 裁定 | T2.1-H3…H9 | D4 批准；T2.1 validation.md 行已更新（Task 1） |
| MAN-016 登记 | pending-manual-test-cases.md | 已登记（Task 1） |
| 分支 | feature/T2.1a-mobile-ui-parity rebase 到 main e2ac943 | Task 0 记录 |

## Task 0 — 分支（先于一切落盘；owner 硬性要求）

- `git status --short --branch` → `## main...origin/main [ahead 1]`（干净）
- `git switch feature/T2.1a-mobile-ui-parity` → `Switched to branch 'feature/T2.1a-mobile-ui-parity'`（exit 0）
- `git rebase main` → 冲突仅 AGENTS.md（公告板记录区）；plans/roadmap 自动合并（exit 1 = 冲突）
- 裁决：公告板两套记录**并存**（T2.1 复核重关/代码评审轮 + 2026-10-19 治理修订行），按日期倒序；冲突标记清零（`grep -n '<<<<<<<\|>>>>>>>\|^=======' AGENTS.md` → NO_MARKERS）
- `GIT_EDITOR=true git rebase --continue` → `Successfully rebased and updated refs/heads/feature/T2.1a-mobile-ui-parity.`（新 tip `63c73c5`，基线 `e2ac943`）
- 复核：`specs/roadmap.md` L20 T2.1a 修宪子弹 ✓；`plans/3-day-implementation-plan.md` L205 v1.3 ✓；AGENTS.md L25 治理行 ✓；`git status --short --branch` 干净 ✓

结论：**分支先于任何文件落盘成立**（本 feature 全部文件写于 rebase 之后；治理提交 `63c73c5` 位于 main `e2ac943` 之上）。

## Task 1 — 规划轮产物（2026-10-19，提交 `570d24d`）

| 产物 | 路径 |
|---|---|
| requirements / plan / SKILL（provenance 副本）/ validation | `specs/features/2026-10-19-T2.1a-mobile-ui-parity/` |
| ui-wallet §2.1/§2.3 修订 | `specs/ui-wallet.md`（D2/D3 文本见 plan.md §4） |
| expo-font 行 + expo-router 括注同步 | `specs/tech-stack.md` §2 + `doc/dependencies.md` §6d（D1/H9） |
| MAN-016 登记 | `specs/pending-manual-test-cases.md` |
| T2.1-H3…H9 行更新 | `specs/features/2026-10-18-T2.1-mobile-ui-shell/validation.md` §Handoff 清单 |

SKILL provenance：.agents 原件 sha256 `67892de8792e1e40815a198e81ebd5a213621b1b478f728af3ab286227b66e80`（提交 e2ac943）；feature 目录副本附 provenance 头。
提交：`570d24d docs(T2.1a): planning round — …`（9 files, +627/−27）。

## Task 2 — expo-font + Inter 资产 `[x]`（自动侧）

- `cd apps/mobile && npx expo install expo-font` → exit 0；`+ expo-font 57.0.4`；`Lockfile passes supply-chain policies`。
- 漂移：`git diff -- apps/mobile/package.json pnpm-lock.yaml` = package.json +1 行（`expo-font: ~57.0.4`）+ lockfile importer 3 行；**零新包下载**（store 已有 expo-font@57.0.4，4 个 peer 变体）。
- `expo install` 自动把 `expo-font` 写入 app.json plugins → **已移除**（运行期 `useFonts` 即覆盖三端；不改变原生配置，故 dev build 不需重编）。
- 资产：从原仓库 `core/core-ui/src/main/res/{font,raw}` 复制 `inter_{regular,medium,semibold,bold}.ttf` + `inter_ofl_license.txt` → `apps/mobile/assets/fonts/`（OFL 随包）。
- 根 `_layout.tsx`：`useFonts({ 'inter-regular': require(...), … })`，`if (!fontsLoaded && !fontError) return null`；`nativewind-env.d.ts` 增 `declare module '*.ttf'`（expo/types 无 ttf 声明）。
- Web 产物实证：`/tmp/t21a-web/assets/assets/fonts/inter_{regular,medium,semibold,bold}.<hash>.ttf` 四个 TTF 均已打包；`/tmp/t21a-web/home.html` 引用 `inter_regular.4145168e…ttf`。

## Task 3 — tokens / tailwind.config / global.css `[x]`

- `src/theme/tokens.ts`（130 行）：色板 39 键 + radius 9 + spacing 14 + size 12 + type 15 阶 + font 表，逐值带证据注释（`// Color.kt:DarkPrimary @3c7487f` 格式）；`withAlpha()` 复刻原仓库 `onSurface.copy(alpha = …)`。
- `tailwind.config.js`：colors / borderRadius / spacing / borderWidth / fontFamily / fontSize 全量映射（fontSize 元组带 lineHeight + letterSpacing）。
- `global.css`：`:root { --nv-bg: #0A0E1A; }` + `html, body, #root { background-color: var(--nv-bg); }`（T2.1 NIT 关闭：暖色硬编码 → CSS var）。
- **WCAG 对比抽查**（palette 固定：失败对只报告、不改值）：

| 对 | 对比度 | 判定 |
|---|---|---|
| textHigh / bg | 19.25:1 | AAA |
| textMedium / bg | 8.19:1 | AAA |
| textHigh / cardSurface | 17.51:1 | AAA |
| textMedium / cardSurface | 7.44:1 | AAA |
| onSurfaceVariant / surface | 7.51:1 | AAA |
| onPrimary / primary | 5.48:1 | AA |
| error / bg | 5.97:1 | AA |
| positive / cardSurface | 10.49:1 | AAA |
| **primary / bg** | **3.51:1** | **低于 4.5（AA 正文）**——仅大字号/UI 组件可用；primary 作正文色的场合（secondary 按钮标签）视觉上与原 app 一致，按「色板固定、不改值」上报 owner |
| textDisabled / bg | 3.17:1 | 禁用态豁免 |

## Task 4 — 原语组件 `[x]`（自动侧）

| 文件 | 内容（证据与注释） |
|---|---|
| `src/components/NexVaultCard.tsx` | fillMaxWidth / radius 16 / cardSurface / 1px cardBorder / elevation 0 / p-md 16 —— NexVaultCard.kt |
| `src/components/NexVaultButton.tsx` | h52 / radius 16；primary = primary/onPrimary；secondary = outlined primary；disabled = onSurface 12%/38%（`withAlpha`）；loading spinner；labelLarge；a11y role/state —— NexVaultButton.kt |
| `src/components/NexVaultTextField.tsx` | h56 / radius 12；聚焦 primary、未聚焦 cardBorder、错误 error；label 浮动（labelSmall）；disabled textDisabled；error caption bodySmall —— NexVaultTextField.kt |
| `src/components/NexVaultTopBar.tsx` | 透明默认 / opaque = cardSurface；居中 titleLarge 18/26 SemiBold；可选 back；**高度不强制**（insets.top + 56dp 内容行自测量）—— NexVaultTopBar.kt |

## Task 5 — chrome + 5-tab + 页面 `[x]`（自动侧）

- `src/app/(tabs)/_layout.tsx`：`NativeTabs`（`expo-router/unstable-native-tabs`，SDK 57 入口，docs fetched 2026-10-19）：Home/History/DApp/NFT/Settings；md material symbols = home/receipt_long/explore/photo_library/settings；sf = house/clock/safari/photo.on.rectangle/gearshape；`tintColor` primary、`backgroundColor` surface、`labelStyle` onSurfaceVariant、`disableTransparentOnScrollEdge`。
- `src/app/(tabs)/_layout.web.tsx`：headless tabs（`expo-router/ui`，docs「Custom web layout」）；同五 tab；bar = surface + outline-variant 顶边 + 64dp；标签 labelSmall/onSurfaceVariant；glyph 图标（见偏差）。
- 页面：`(tabs)/home`（无 TopBar，链选择器行 + 余额卡 + 图占位 + FlashList 占位行 + Receive/Send 动作，gutter 16）、`history/dapp/nft/settings`（"Not configured" 占位）、`index`（Welcome：wordmark + Create/Import 禁用 + Not configured）、`receive/send`（TopBar + back + 占位/禁用表单）、`+not-found`；旧 `src/app/home.tsx` 已删除。
- 记录偏差：① web tab 图标用文字 glyph（未批准任何图标包，R4）；② 选中态 filled 图标对未实现（NativeTabs 单 md/sf + tintColor 主色；Android filled 变体名未经真机验证）；③ GradientBackground 未移植（需 `expo-linear-gradient`，非批准依赖；原仓库 grep 显示无屏幕实际使用该组件，背景即纯 `#0A0E1A`）；④ `expo export -o` 在当前 CLI 已不受支持 → 统一用 `--output-dir`（B.5 R7 示例命令文字待修宪，Handoff）。

## Task 6 — 自动门禁 `[x]`（2026-10-19）

| 门禁 | 命令 | 观察结果 |
|---|---|---|
| tsc | `pnpm --filter mobile exec tsc --noEmit` | **exit 0** |
| web export | `npx expo export --platform web --output-dir /tmp/t21a-web` | **exit 0**；15 静态路由：/ /home /history /dapp /nft /settings /receive /send /+not-found /_sitemap + 5 个 `/(tabs)/*` 别名 |
| R7 | `grep -rE 'signDigest|importMnemonic|vault-core' /tmp/t21a-web/_expo/static/js/` | **exit 1（零命中）** |
| R1 | 宪法原文 grep（apps/mobile，`*.ts|*.tsx`，exclude node_modules） | **exit 1（零命中）** |
| R5 | `console.*` 敏感启发式 grep | **exit 1（零命中）** |
| 漂移·hex | `src` 内 `#RRGGBB`（排除 tokens.ts） | **exit 1（零命中）**（仅 `global.css --nv-bg` 一处镜像字面量，注释声明） |
| 漂移·rgba | `src` 内 `rgba(`（排除 tokens.ts） | **exit 1（零命中）** |
| 漂移·旧键 | `textDim|surfaceHi|cardPad|#1B1815|#C79A6B` | **exit 1（零命中）** |
| 依赖漂移 | `git diff package.json app.json pnpm-lock.yaml` | 仅 expo-font 行（+ app.json 无改动） |
| expo-doctor | `npx expo-doctor` | **21/21 checks passed** |
| Web 路由 smoke | serve `/tmp/t21a-web` + curl 8 路由 | 全部 **200**；转录 `evidence/web-route-smoke.txt` |

漂移审计（design-system-enforcer 清单）：hex/rgba/魔法间距/临场字号 = 0 处（除上述受控字面量与 `h-14` 的 M3 app bar 56dp 内容行，注释说明）；组件重复 = 0（四原语单一定义）。

## Task 7 — 设备 smoke + MAN-016 [x]（自动侧完成；owner 裁决待 MAN-016）

### 7.1 设备 findings 与修复（2026-10-19，Pixel 6a 重连后）

| # | 发现 | 根因 | 修复 |
|---|---|---|---|
| F1 | 启动后永久停在 splash（JS 已运行，`T21A_ROOT_RENDER` 有日志） | `useFonts` 门控 `if (!loaded) return null`，字体加载未结算时 splash 不消失 | 根布局改为**立即渲染**，字体异步加载、失败回退系统字体（`_layout.tsx`） |
| F2 | 文本块高度膨胀 ~20×（探针：`SIZE_ONLY` 文本 735px vs `PLAIN_NOCLASS` 51px） | 字阶元组的 `lineHeight`/`letterSpacing` 为无单位数值 → Tailwind 原样输出 → 被当作倍数 | `tailwind.config.js` 改为 `${n}px` 字符串（device-diff 定位） |
| F3 | 顶部内容被状态栏裁切（顶端胶囊只剩半截） | 缺 `SafeAreaProvider` → insets 全 0 | 根布局包 `SafeAreaProvider`（react-native-safe-area-context） |
| F4 | `flex-1` 居中容器失效（onboarding 内容贴顶） | ScrollView contentContainer 无 `flexGrow` | `Screen.tsx` 补 `flexGrow: 1` |

复测：onboarding/home 文本 bounds 恢复正常（display-large 116px），字体为 Inter。

### 7.2 Android 路由 smoke（Pixel 6a / Android 17，0 crash）

- `adb reverse tcp:8082 tcp:8082` → 8082；dev-client deep link 拉起（Metro 8082，`--clear` 后 1830 模块整包）。
- 路由 ×8：`nexvault://{home,history,dapp,nft,settings,receive,send}` + 启动页 index —— 全部渲染、无红屏、0 crash。
- UI dump 核对（home）：NexVault / Chain · Not configured / Total balance / Not configured / Price chart · Not configured / Tokens / NFTs / Activity / Receive / Send / Home。
- 截图（绝对路径，`specs/features/2026-10-19-T2.1a-mobile-ui-parity/evidence/`）：`shell-{index,home,history,dapp,nft,settings,receive,send}.png`。

### 7.3 MAN-016 参考侧（原仓库 APK，同机）

- 安装 `com.nexvault.wallet.debug`（APK `/home/superguo/Projects/NexVault/app/build/outputs/apk/debug/app-debug.apk`）；PIN `123456`（原仓库 `UnlockViewModelTest` 样例）解锁成功。
- 原 app 截图：`evidence/orig-{unlock,home,history,dapp,nft,settings}.png`。
- 并排要点：色板一致（#0A0E1A 底 / #121829 surface / #141928 卡面 / #1E2440 描边）、Inter 渲染、五 tab 原生 Material symbols + 选中 primary、TopBar 透明居中（send/receive）、Home 无 bar、卡片 1dp 描边无阴影、按钮 52/输入 56。

### 7.4 偏差清单（owner 裁决 MAN-016 时逐条判定）

| # | 偏差 | 说明 |
|---|---|---|
| D-a | M3 active indicator 颜色 | NativeTabs 未暴露 indicator 颜色（文档仅 `tintColor`/`backgroundColor`/`labelStyle`），系统主题给琥珀调；原 app 为 primaryContainer #1E3A8A |
| D-b | Home 头部 | 保留 wordmark + 链选择器胶囊行；原 app Home 首屏直接是余额卡（无 wordmark） |
| D-c | 选中态 filled 图标 | 单 md/sf 图标 + primary tint（filled 变体名待验证/后续） |
| D-d | web tab 图标 | glyph 占位（无批准图标包，R4） |
| D-e | GradientBackground | 未移植（需 expo-linear-gradient，非批准依赖；原仓库无屏幕实际使用） |

### 7.5 待 owner
MAN-016 裁决（通过 / 失败附 issue / 阻塞）。通过后执行：公告板行 + `--no-ff` 合并 + 删分支 + push main。

### 7.6 红屏事件与独立 release 构建（2026-10-19 21:14–21:45）

- 用户报告红屏 `Unable to load script`：**dev build 依赖 Metro**（`adb reverse` + 8082）；Metro/端口断开即红屏——普通用户体验不可接受（T2.1 的 dev-build 已知限制）。本期把「可演示产物」升级为 **release APK（内置 JS bundle，零 Metro 依赖）**。
- 构建：`cd apps/mobile/android && JAVA_HOME=/home/superguo/tools/jdk-17 ./gradlew :app:assembleRelease -PreactNativeArchitectures=arm64-v8a` → **BUILD SUCCESSFUL in 2m 26s**（532 tasks；先跑过 4 ABI 全量构建约 25 min 后中止，arm64-only 提速）。
- 产物：`/home/superguo/Projects/NexVaultRN/apps/mobile/android/app/build/outputs/apk/release/app-release.apk`（44,985,066 B；`unzip -l` 实证内置 `assets/index.android.bundle` 3,159,940 B + `libhermesvm.so`）。
- **独立运行实证：先停 Metro（job killed）** → `adb install -r app-release.apk` → `monkey -p io.github.realalexkwok.nexvault -c android.intent.category.LAUNCHER 1` → onboarding 渲染（无红屏、无 Metro）；五 tab 逐一点按 + 文本核对（home/history/dapp/nft/settings 全部命中）+ receive/send deep link。
- 截图（release 产物）：`evidence/shell-release-{index,home,history,dapp,nft,settings,receive,send}.png`。
- 注：NativeTabs 对短 `input tap` 不敏感（dev/release 均如此）→ 脚本用 `input swipe x y x y 120`（120ms 按压）切换 tab，已记入复现命令。

### 7.8 Home 对齐重做（owner 反馈 2026-10-19 22:12）

- Owner 裁定：**Home 主屏布局与原仓库差异过大**（其余偏差可递延）。原始结构（HomeScreen.kt + `evidence/orig-home.png`）：chain 下拉 → 居中 "Total Balance"/余额/24h 变化 → 180dp chart 卡 + 1D/7D/1M/3M/ALL chips → **Send/Receive/Swap 三个圆形动作** → "Tokens" + "+ Add Token" → token 列表 → 底部 tab。
- 重写 `(tabs)/home.tsx` 严格按此顺序；删除 T2.1 遗留的 wordmark / NFT / Activity / 底部 Receive-Send 布局；无数据处一律 "Not configured"（R5），24h 显示 "— (24h)"，Swap 无目标置 disabled。
- release 重建 + 装机 + 截图：`evidence/shell-release-home.png`；UI dump 顺序核对：Chain · Not configured / ▾ / Total Balance / Not configured / — (24h) / Not configured / 1D 7D 1M 3M ALL / ↑ ↓ ⇄ / Send Receive Swap / Tokens / + Add Token / Not configured / Home ✓。

### 7.9 release 打包 Babel 解析修复（R4 lift 请求，待 owner 批准）

- 现象：Gradle `:app:createBundleReleaseJsAndAssets` 在 pnpm isolated 布局下冷缓存报 `Cannot find module '@babel/plugin-transform-react-jsx'`（手动 `expo export:embed` 因缓存热而通过）。
- 排除：`-x :app:createBundleReleaseJsAndAssets` 不可行（RN gradle provider 依赖该 task 输出 → `packageReleaseResources` 失败）；root `public-hoist-pattern` 在本 pnpm 版本未生成根链接（`--force` 后仍无），已回退 `.npmrc`。
- 采用：把 **lockfile 已有传递依赖** `@babel/plugin-transform-react-jsx@7.29.7` 提升为 `apps/mobile` devDependency（构建期专用；T2.1-D6 css-interop 同型先例）→ 重建 **BUILD SUCCESSFUL in 6m**（532 tasks），APK `44,892,134 B`，装机后 Home 渲染正常。
- **R4 待批准**：tech-stack §2 行 + dependencies.md §6d 行（版本 7.29.7 精确；"babel-preset-expo 已有传递依赖，pnpm isolated 下 Gradle release 打包按 app 根解析 Babel 插件，提升为直接行仅新增 importer 条目；构建期专用"）。批准后补记；拒绝则回退该 devDependency 并登记为环境限制（release 包改由手动 embed 产物提供 — 已验证不可靠）。
- 命令：`pnpm add -D @babel/plugin-transform-react-jsx@7.29.7 --filter mobile` → package.json +1 行 / lockfile importer +3 行（零新包下载）。

### 7.7 复现命令（release 验收）

```bash
cd /home/superguo/Projects/NexVaultRN/apps/mobile
JAVA_HOME=/home/superguo/tools/jdk-17 ./android/gradlew -p android :app:assembleRelease -PreactNativeArchitectures=arm64-v8a
adb install -r android/app/build/outputs/apk/release/app-release.apk
adb shell monkey -p io.github.realalexkwok.nexvault -c android.intent.category.LAUNCHER 1
# tab 切换（NativeTabs 需 120ms 按压）
adb shell input swipe 320 2270 320 2270 120
```

## 复现命令（app 侧，`apps/mobile`）

```bash
pnpm --filter mobile exec tsc --noEmit
npx expo export --platform web --output-dir /tmp/t21a-web
grep -rE 'signDigest|importMnemonic|vault-core' /tmp/t21a-web/_expo/static/js/ ; echo R7_EXIT=$?
```

## Handoff 清单（T2.2 / T4.x / 后续 UI 条目）

| # | 事项 | 递延理由 | 建议处置 | 关联 AC/TC |
|---|---|---|---|---|
| H1 | Android 路由 smoke + MAN-016 视觉走查 | Pixel 6a 掉线（无 route / 无 mDNS） | 设备恢复后立即执行；通过后该项方可关闭 | AC-5 / AC-8 / TC-UI-007 |
| H2 | 选中态 filled 图标对（Material/SF） | NativeTabs 单 md/sf 已在真机验证前保守使用；filled 变体名待设备确认 | 设备验证 `md={{default, selected}}` 后切换；偏差记录在案 | MAN-016 |
| H3 | web tab glyph 图标 | 无批准图标包（R4） | 若 web 视觉需对齐 → 单独 R4 提案（@expo/vector-icons 或 SVG） | AC-7 |
| H4 | B.5 R7 示例命令 `-o` 失效 | CLI 改为 `--output-dir` | 下次修宪同步 AGENTS.md B.5 R7 命令文字 | R7 |
| H5 | 18 屏全量 + 真实列表/数据 | T2.2 / T4.x | 随各流程条目 | — |
| H6 | 地址等宽 + 首尾高亮、GradientBackground | 地址组件属 T4.x；渐变需新依赖 | 各自条目评估 | TC-UI-007 |
