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

## Task 7 — 设备 smoke + MAN-016 + 关闭 [ ] 待设备

- **阻塞**：Pixel 6a 于 2026-10-19 18:39 从 adb 消失 —— `ping 10.42.0.139` 100% 丢包、`adb connect` → `No route to host`、`adb mdns services` 空。需 owner 恢复 Wi-Fi/无线调试（或 USB）后执行：
  - `adb reverse tcp:8082 tcp:8082` + dev-client deep link（Metro 8082 已在跑）
  - 五 tab + receive/send/+not-found deep-link 截图 + UI dump（0 crash；fonts 加载实证）
  - MAN-016：原仓库 `app-debug.apk` 安装 + onboarding 截图 vs RN shell 同机并排 → owner 裁决
- 静态侧已全部就绪；设备恢复后按序补齐 AC-5 + AC-8，再走公告板 + merge。

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
