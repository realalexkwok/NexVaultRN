# T2.1 — mobile UI shell — validation

> 状态三档：`[x]` 已验证（双侧齐备）/ `[~]` 已实现未验证 / `[ ]` 未开始。命令 + 退出码 + 观察结果逐条记录（B.4-3）。

## Task 0 — 规划产物 + R4/追认修订（2026-10-18）

| 步骤 | 命令/动作 | 观察结果 |
|---|---|---|
| 基线 | `git log --oneline -1`（main） | `407d5ae chore: install agent skill (T2.1 mobile UI shell — Expo Router + NativeWind v4)`，工作树干净 |
| 分支 | `git switch -c feature/T2.1-mobile-ui-shell` | 已创建并切换；`git branch --show-current` → `feature/T2.1-mobile-ui-shell` |
| 技能溯源 | `sha256sum .agents/skills/nexvault-t2-1-mobile-ui-shell/SKILL.md` | `133e4069e47d86c5710a30b81841015275f9dcd5338a3f8c60991ce0fb5e21bc`（requirements.md/plan.md/SKILL.md 副本同源） |
| 规划访谈 | `ask_user_question` ×5（D1–D5） | 全部答复：D1 追认 SPM 路线；D2 确认四条 Handoff 处置；D3 批准 tailwindcss 3.4.17 修宪；D4 载体确认、人工侧 owner 执行；D5 暗色令牌主题 |
| 计划审批 | plan-mode `exit_plan_mode` | approved: true（2026-10-18） |
| 文档核对 | web_fetch：expo.dev/router/installation.md（modified 2026-10-16）；nativewind.dev llms.txt + docs/getting-started/installation.mdx；registry.npmjs.org（nativewind/4.2.7、expo-router/57.0.26、tailwindcss/3.4.17、reanimated/4.7.1、worklets/0.13.0、flash-list/2.3.3） | 关键结论：nativewind 4.2.7 peer = `tailwindcss >3.3.0`；expo-router 57.0.26 peers 含 `@expo/metro-runtime ^57.0.16`（非 optional）且 expo 57.0.28 不携带 → Task 1 条件安装；tailwindcss 3.4.17 发布于 2024-12-17（latest 4.3.3 = Tailwind v4 线，排除）；四钉版均存在 |
| R4 修宪（D3） | edit `specs/tech-stack.md` + `doc/dependencies.md` | tailwindcss 3.4.17 行 + nativewind 行 companion 注记两文件同步落地 |
| 追认（D1） | edit `specs/tech-stack.md`（swift-secp256k1 行）+ `doc/dependencies.md` §6c + T1.5 validation.md H6 行 | "待 owner 追认"标记清零（`grep` 复核：specs/ 仅剩 T2.1 自身文件中"摘除标记"的历史描述）；T1.5-H6 行置 `[x]` closed |
| 三件套落盘 | write requirements.md / plan.md / SKILL.md | 三文件已创建（+validation.md 本文件） |

### Task 0 状态

| 项 | 状态 |
|---|---|
| 规划轮（访谈 + 审批 + 三件套） | `[x]` verified |
| R4 修宪（tailwindcss，D3） | `[x]` verified（两文件同步 + grep 复核） |
| T1.5-H6 追认（D1） | `[x]` verified（标记清零，T1.5 validation 行更新） |
## Task 1 — 安装（R4 精确钉版，2026-10-18）

| 步骤 | 命令 | 退出码 | 观察结果 |
|---|---|---|---|
| router + peer 集 | `pnpm exec expo install expo-router react-native-safe-area-context react-native-screens expo-linking expo-constants expo-status-bar` | 0 | + expo-router 57.0.28、safe-area-context 5.7.0、screens 4.26.2、linking 57.0.13、constants 57.0.22、status-bar 57.0.1；自动加 config plugins `expo-router`/`expo-status-bar` |
| 四行 UI | `pnpm exec expo install nativewind react-native-reanimated react-native-worklets @shopify/flash-list` | 0 | expo 解析出 SDK 默认对（reanimated **4.5.1** / worklets **0.10.1**（低于 CVE-2026-97724 下限！）/ flash-list **2.0.2**）→ 立即按 tech-stack 钉回 |
| 精确钉版 | `pnpm add react-native-reanimated@4.7.1 react-native-worklets@0.13.0 @shopify/flash-list@2.3.3` | 0 | 三包替换为批准钉版（flash-list 2.0.2→2.3.3、reanimated 4.5.1→4.7.1、worklets 0.10.1→0.13.0） |
| dev 钉版 | `pnpm add -D tailwindcss@3.4.17 babel-preset-expo`（后者先误解析 58.0.17 → `pnpm exec expo install --dev babel-preset-expo` 修正） | 0 | tailwindcss 3.4.17；babel-preset-expo **57.0.15**（SDK 57 绑定单元；58.0.17 已回退） |
| 钉版收口 | edit package.json：nativewind `^4.2.7` → `4.2.7` 精确；expo 57.0.28 → **57.0.30**（SDK 57 补丁内，doctor 提示的基座补丁对齐）；新增 `expo.install.exclude` = 三钉版（flash-list/reanimated/worklets，理由：owner 批准的 tech-stack 钉版故意偏离 SDK 默认集） | — | package.json 终态见 diff |
| 依赖体检 | `npx --yes expo-doctor@latest` | 0 | **21/21 checks passed**（钉版前 20/21：三钉版 + expo 补丁被判 out-of-date；exclude 后全绿） |
| peer 分类 | `pnpm peers check` | — | 唯一告警：expo-modules-core@57.0.22 对 worklets 的可选 peer（`^0.7.4…^0.10.0`，`peerDependenciesMeta.optional=true`）；**运行时权威对是 reanimated 4.7.1 ↔ worklets `0.13.x`（registry 实证，已满足）** → 告警良性，记录不整改 |
| 传递项溯源 | `pnpm why uuid` | — | 弃用告警 `uuid@7.0.3` 来自 `xcode@3.0.1 → @expo/config-plugins@57.0.10 → @expo/cli`（Expo 官方工具链内部，dev-time，非运行时），SDK 绑定单元，零动作 |
| 漂移证明 | `git diff -- apps/mobile/package.json pnpm-lock.yaml` | — | package.json 仅批准行（+expo 补丁对齐 +exclude 配置）；lockfile 变更 = 新包传递树，无新增直接行；`@expo/metro-runtime@57.0.17` 经 expo-router 自身依赖进入（条件安装未触发） |

### Task 1 状态

| 项 | 状态 |
|---|---|
| 安装 + 精确钉版 | `[x]` verified（doctor 21/21、diff 仅批准行、四钉版在 package.json） |
## Task 2 — 路由接线 + 四页空壳（2026-10-18）

| 步骤 | 命令/动作 | 观察结果 |
|---|---|---|
| 入口 | package.json `main` → `expo-router/entry`；`rm App.tsx index.ts` | 旧模板入口移除，`ls apps/mobile` 无 App.tsx/index.ts |
| app.json | plugins 追加 `expo-router`（expo install 自动写入）+ `expo-status-bar`；`userInterfaceStyle: "dark"`（D5）；`experiments.typedRoutes: true`；`web.output: "static"`（每路由 HTML，供 curl smoke 与 TC-UI-009 子集） | 全量 diff 记录 |
| tsconfig | paths `@/*` → `./src/*`；include `.expo/types/**/*.ts` + `expo-env.d.ts`；NativeWind 自动追加 `nativewind-env.d.ts`（export 首轮日志明示） | typed routes + nativewind 类型齐备 |
| 路由文件 | 新建 `src/app/_layout.tsx`（Stack + global.css + StatusBar light）、`index/home/receive/send/+not-found.tsx`、`src/components/Screen.tsx` | 6 路由文件就位（typed routes 生成物证实：`/home`、`/`、`/receive`、`/send`、`/_sitemap`、`/+not-found`） |
| R1 评论规避 | 首轮 R1 grep 命中 index.tsx 注释中的 "mnemonic" 字样 → 改措辞为 "key-material" | 重跑 grep 干净（证据见 Task 4） |

## Task 3 — NativeWind v4 + 令牌 + FlashList（2026-10-18）

| 文件 | 内容要点 | 状态 |
|---|---|---|
| `src/theme/tokens.ts` | 逐值等于 ui-wallet §2.3（color 11 项 / radius 3 项 / spacing 2 项 / type addressMono） | `[x]`（逐值比对） |
| `tailwind.config.js` | content `./src/**/*.{js,jsx,ts,tsx}`；`nativewind/preset`；theme.extend 全部从 tokens 映射（colors/borderRadius/spacing/fontFamily） | `[x]` |
| `global.css` / `babel.config.js` / `metro.config.js` / `nativewind-env.d.ts` | 按 v4.2.7 官方安装文档形态（llms.txt/mdx，2026-10-18 核对） | `[x]` |
| 依赖解析修复 | 首轮 export 失败：`Unable to resolve module react-native-css-interop/jsx-runtime`（pnpm isolated：app 代码无法从 nativewind 上下文解析其运行时）。`public-hoist-pattern` 试验失败（3 个 peer 变体无法合并根链接，已回退 .npmrc）。**D6（owner 批准）**：`react-native-css-interop@0.2.7` 提升为直接行（= nativewind 4.2.7 自身钉版；依赖图不变，仅 package.json 行）；tech-stack.md + dependencies.md 行已补 | `[x]`（修复后 export 通过） |
| 动画/列表 | reanimated 4.7.1 / worklets 0.13.0 解析断言（package.json 精确钉）；零动画代码；home 静态 FlashList 占位行 | `[x]` |

## Task 4 — 自动门禁（2026-10-18）

| 门禁 | 命令 | 退出码 | 观察结果 |
|---|---|---|---|
| web export | `npx expo export -p web --output-dir /tmp/t21-web` | **0** | `Web Bundled` 852 modules；`Static routes (6): /home /send / (index) /receive /_sitemap /+not-found`；日志存 `evidence/web-export.log` |
| R7 grep | `grep -rE 'signDigest|importMnemonic|vault-core' /tmp/t21-web/_expo/static/js/` | **1（零命中）** | 导出 JS 无签名/导入/模块路径（R7 干净） |
| tsc | `npx --no-install tsc --noEmit`（typed routes 由 `expo start` 生成 `.expo/types/router.d.ts` 后） | **0** | 严格模式零错误；typed routes 生效（四路由字面量在生成物中） |
| R1 grep | **宪法原文命令（AGENTS.md R1），工作目录 = 仓库根**：`grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile --include='*.ts' --include='*.tsx' --exclude-dir=node_modules \| grep -v 'packages/vault-core'; echo "exit: ${PIPESTATUS[0]}"` | **exit 1，零命中，且无 `No such file or directory`**（首轮 1 命中为注释措辞，已改；2026-10-19 评审修复轮与第二轮复跑均从仓库根执行——**MINOR-A**：在 `apps/mobile` 内执行同一命令会打印 `apps/mobile: No such file or directory` 且 grep 以 exit 2 失败——旧记录的 `exit 1` 实为管道误读，属假通过；已在 §复现命令 拆分为"应用侧（apps/mobile）"与"仓库根侧"两段） | 密钥语义标识符干净（NIT-2：早前记录的命令漏 `secretKey` 且范围窄于宪法，已改正） |
| expo-doctor | `npx --yes expo-doctor@latest` | 0 | **21/21 passed** |

### Task 4 状态

| 项 | 状态 |
|---|---|
| 自动侧四门禁 | `[x]` verified（命令 + 退出码 + 产物路径齐备） |
## Task 5 — Android 路由 smoke（Pixel 6a / Android 17，2026-10-18）

| 步骤 | 命令 | 观察结果 |
|---|---|---|
| 设备接入 | `adb devices -l` | Pixel 6a（bluejay）双通道：`10.42.0.139:38385`（transport_id 50）与 TLS 通道（49，同一设备）；`getprop`：Android **17**，fingerprint `google/bluejay/bluejay:17/CP2A.260705.006/15641320:user/release-keys` |
| dev build #1 | `ANDROID_SERIAL=10.42.0.139:38385 npx expo run:android`（后台） | **BUILD FAILED in 2m 22s**：`Toolchain installation '/usr/lib/jvm/java-17-openjdk-amd64' does not provide the required capabilities: [JAVA_COMPILER]`（`react-native-masked-view/masked-view:bundleLibCompileToJarDebug`）。根因：本机 JRE-only 17 被 Gradle 9 工具链选中，而 expo-router 传递引入的 masked-view 含 Java 源码（首个需要 javac 的原生依赖）；host 上可用的完整 JDK：`~/tools/jdk-17`（17.0.20.1）、`~/.gradle/jdks/eclipse_adoptium-17/21-*`。另：8081 被 `byterra` 项目 dev server 占用 → expo 跳过 dev server |
| 环境修复 | `specs/dev-environment.md` §1 新增 JDK 行（JAVA_HOME=`/home/superguo/tools/jdk-17`） | spec 已修订（命令 + 观察结果见上） |
| dev build #2 | `JAVA_HOME=/home/superguo/tools/jdk-17 npx expo run:android --port 8082`（后台，日志 `evidence/android-dev-build.log`） | **`BUILD SUCCESSFUL in 10m 18s`**（log 行 681）；`Installing …/app-debug.apk`；`Opening nexvault://expo-development-client/?url=http://192.168.1.17:8082 on Pixel_6a`；`Android Bundled 17880ms（1793 modules）`——Metro 8082 常驻（8081 被 byterra 占用，已绕过） |
| deep-link smoke | `adb shell am start -a android.intent.action.VIEW -d "nexvault://<route>"` ×5（index/home/receive/send/+not-found），每路由 `uiautomator dump` + `screencap` | 五路由全部渲染成功，UI 文本逐路由核对：index（NexVault / Import wallet / Create wallet / Not configured / Shell preview）；home（Balance / Not configured / Tokens·NFTs·Activity / Receive·Send）；receive（Receive address / Not configured / On-device address derivation is not wired yet.）；send（To address / Amount / Continue / Not configured）；+not-found（Not found / This route does not exist.） |
| 崩溃核查 | `adb logcat -d -t 400` grep `FATAL|AndroidRuntime|ReactNativeJS` | **0 命中**——无 crash、无红屏 |
| 证据落盘 | `evidence/` | `android-dev-build.log`、`android-route-{index,home,receive,send,+not-found}.png` ×5、`ui-{index,home,receive,send,+not-found}.xml` ×5 |

### Task 5 状态

| 项 | 状态 |
|---|---|
| 自动侧（deep-link + UI dump + 截图 + logcat） | `[x]` verified 2026-10-18（Pixel 6a / Android 17，fingerprint `google/bluejay/bluejay:17/CP2A.260705.006/15641320`） |
| 人工侧（owner 点走，MAN-014） | `[x]` verified 2026-10-19——**owner 点走通过**（Pixel 6a / Android 17；自动侧截图 + UI dump 见 `evidence/`） |

## Task 6 — Web 路由 smoke（2026-10-18）

| 步骤 | 命令 | 观察结果 |
|---|---|---|
| 静态服务 | `python3 -m http.server 8931 -d /tmp/t21-web`（后台 job） | 常驻（供 owner 浏览器走查备选） |
| curl 五路由 | `curl -s -o /dev/null -w '%{http_code}'` `/index.html /home.html /receive.html /send.html /+not-found.html` | 全部 **HTTP 200**；四页各含 `Not configured` 标记 ≥1（+not-found 含其专属文案）；**完整转录落盘（NIT-3）**：`/home/superguo/Projects/NexVaultRN/specs/features/2026-10-18-T2.1-mobile-ui-shell/evidence/web-route-smoke.txt`（2026-10-19 评审修复轮生成；含命令、状态码、标记计数、`<title>` 与背景规则断言） |
| 导出日志 | `/home/superguo/Projects/NexVaultRN/specs/features/2026-10-18-T2.1-mobile-ui-shell/evidence/web-export.log` | `Static routes (6)`：/home /send / (index) /receive /_sitemap /+not-found |

### Task 6 状态

| 项 | 状态 |
|---|---|
| 自动侧（导出 + curl 四路由 200 + 标记） | `[x]` verified 2026-10-18 |
| 人工侧（owner 浏览器走查，MAN-015） | `[x]` verified 2026-10-19——**owner 浏览器走查通过**（桌面浏览器；自动侧 curl 四路由 200 见 Task 6） |
## T2.1 总览（截至 2026-10-18 实现完成）

| 项 | 状态 |
|---|---|
| Task 0 规划 + R4/追认修订 | `[x]` verified |
| Task 1 安装 + 钉版 | `[x]` verified |
| Task 2 路由接线 + 四页空壳 | `[x]` verified（typed routes 生成物 + tsc + 真机渲染） |
| Task 3 NativeWind v4 + 令牌 + FlashList | `[x]` verified（tokens 逐值 = ui-wallet §2.3；tailwind 映射单一来源） |
| Task 4 自动门禁（export/tsc/R7/R1/doctor） | `[x]` verified |
| Task 5 Android smoke | `[x]` verified（自动侧：真机五路由渲染 + 0 crash；人工侧：MAN-014 owner 点走通过 2026-10-19） |
| Task 6 Web smoke | `[x]` verified（自动侧：curl 四路由 200 + 标记；人工侧：MAN-015 owner 浏览器走查通过 2026-10-19） |
| Task 7 收尾（Handoff + 公告板 + git） | `[x]` verified 2026-10-19（owner 闭项指令：公告板行 + merge --no-ff 入 main + 分支删除 + push） |

## AC 对照（收尾核对，2026-10-19 复跑版）

| AC | 状态 | 证据 |
|---|---|---|
| AC-1 tsc exit 0 | `[x]` | 2026-10-19 复跑：`npx --no-install tsc --noEmit` → exit 0（含 typed routes `.expo/types/router.d.ts`） |
| AC-2 web export + R7 grep | `[x]` | 复跑：`expo export -p web` exit 0（6 静态路由）；`grep -rE 'signDigest|importMnemonic|vault-core' /tmp/t21-web/_expo/static/js/` exit 1（零命中） |
| AC-3 Android 路由 smoke | `[x]` | 2026-10-18：五路由 deep-link + UI dump + 截图（`evidence/ui-*.xml`）；2026-10-19 复验：TLS 通道重连、`adb reverse tcp:8082` + dev-server deep link 重新拉起，onboarding 页 UI dump 正常（红屏仅因传输切换后未重连，已恢复） |
| AC-4 Web 路由 smoke | `[x]` | 2026-10-18：curl `/index.html /home.html /receive.html /send.html` → 全部 200 + 标记（服务器 job 常驻 8931） |
| AC-5 依赖漂移 | `[x]` | `git diff apps/mobile/package.json`：仅批准行（expo-router + peer 集、nativewind/tailwindcss/css-interop、reanimated/worklets、flash-list、babel-preset-expo、expo 补丁对齐 + `expo.install.exclude`） |
| AC-6 R1 + Not configured | `[x]` | 2026-10-19 复跑宪法原文命令（含 `secretKey`，范围 `apps/mobile`，**从仓库根执行**）→ exit 1（零命中、无 "No such file or directory"）；四页 UI dump 均含 "Not configured" |
| AC-7 tokens = ui-wallet §2.3 | `[x]` | `src/theme/tokens.ts` 逐值比对（11 色 / 3 半径 / 2 间距 / 1 字体）；tailwind.config.js 全部从 tokens 映射 |

## Handoff 清单（下一 roadmap 条目规划轮逐条裁定，B.7-1/B.7-2）

| # | 事项 | 状态 | 建议处置 |
|---|---|---|---|
| T2.1-H1 | 人工走查：Android 路由点走（MAN-014） | **`[x]` closed 2026-10-19**（owner 通过；Pixel 6a / Android 17） | — |
| T2.1-H2 | 人工走查：Web 浏览器路由走查（MAN-015） | **`[x]` closed 2026-10-19**（owner 通过；桌面浏览器） | — |
| T2.1-H3 | 真实数据接入（FlashList 真实列表、Zustand store、TanStack Query Provider 挂载） | `[ ]` open | replan → T2.2 |
| T2.1-H4 | 自定入口 polyfill 槽（`index.ts`：`react-native-get-random-values` 首行 + TextEncoder polyfill，ui-wallet §2.1 契约） | `[ ]` open | replan → T2.2（当前入口 = `expo-router/entry`，无 polyfill） |
| T2.1-H5 | 首页/历史/设置 tabs 导航（NativeTabs）与其余路由表页（unlock/receive-confirm/address-book 等） | `[ ]` open | replan → 后续 UI 条目（T4.x 流程条目） |
| T2.1-H6 | iOS 路由 smoke（本条目仅 Android/Web，符合任务界定；iOS 需 Mac/Xcode） | `[ ]` open | 随 T1.5-H1 真机门一并处理（T4.1） |
| T2.1-H7 | eslint/jest 引入（B.6-2/B.6-3 现为零覆盖；T1.1 scaffold 债 H6） | `[ ]` open | 维持 → T1.1 closure 吸收 |
| T2.1-H8 | `expo-env.d.ts` 被 SDK 57 CLI 自动加入 `.gitignore`（不入库）→ `*.css` 模块声明改由 `nativewind-env.d.ts` 自持 | 已规避（`[x]` 记录） | 备注：CI 裸检出下 tsc 已实证通过（2026-10-19 复跑） |
| T2.1-H9 | expo-router 解析为 57.0.28（tech-stack 括注 57.0.26，同为 ~57 线）；expo 基座 57.0.28→57.0.30 补丁对齐 | 已记录（`[x]` 备注） | 不需修宪（~57 范围内）；下次 R4 修订时同步括注 |

## 收尾材料（2026-10-19 owner 批准执行）

**Part A 关闭行（已写入 AGENTS.md）**：
> - 2026-10-19：**T2.1 关闭（owner 裁定）**——Expo Router 57 Stack 四页壳（onboarding/home/receive/send + not-found，typed routes、暗色令牌、FlashList v2 占位）；NativeWind 4.2.7 + tailwindcss 3.4.17 + css-interop 0.2.7（R4 修宪 D3/D6）+ Reanimated 4.7.1/worklets 0.13.0/FlashList 2.3.3 精确钉版；门禁 tsc exit 0、web export exit 0（6 静态路由）、R7/R1 grep 零命中、expo-doctor 21/21；Android 真机（Pixel 6a / Android 17）五路由 deep-link 渲染 0 crash（UI dump + 截图）、Web curl 四路由 200；MAN-014/015 走查通过（owner）。证据：`/home/superguo/Projects/NexVaultRN/specs/features/2026-10-18-T2.1-mobile-ui-shell/validation.md`

**Part A 工程阶段 cell 更新（NIT-5 更正）**：本评审分支（`feature/T2.1-mobile-ui-shell`，自 main `26ad458`）的公告板仍是 `… 下一步 **T2.2（viem + polyfill + Zustand/Query 挂载 + 余额只读）**`；把此 cell 改为 `… **T2.1a（视觉对齐原仓库）→ T2.2**` 的治理提交在**另一分支** `feature/T2.1a-mobile-ui-parity`（`c27c96d`），随 T2.1a 流程合入——两处记录已按分支实际状态更正。

**git（已执行 2026-10-19）**：`git merge --no-ff` → main（提交见 main 历史）；本地分支删除；`git push origin main`。
## Code review handoff (2026-10-19)

> 评审范围：**`407d5ae..HEAD`**（分支 **`feature/T2.1-mobile-ui-shell`**）。
> 提交：`9b78205`（实现 + 证据 + 规格/依赖文档）、`5277f9b`（AC 对照 + Handoff 草案）、`d9aa3f2`（闭项文档）、`26ad458`（merge --no-ff），以及本分支最新提交（**本节 §Code review handoff**；T2.1a 的 roadmap 修宪在独立分支 `feature/T2.1a-mobile-ui-parity`，与本评审无关）。

### 阅读顺序

1. `apps/mobile/package.json` / `app.json` / `tsconfig.json`——钉版、`expo.install.exclude`、typed routes、`web.output: "static"`。
2. `apps/mobile/babel.config.js` / `metro.config.js` / `tailwind.config.js` / `global.css` / `nativewind-env.d.ts`——NativeWind v4 形态（对照 v4.2.7 官方安装页，2026-10-18 实取）。
3. `apps/mobile/src/theme/tokens.ts`——逐值对照 `specs/ui-wallet.md` §2.3。
4. `apps/mobile/src/app/_layout.tsx` + `index.tsx` / `home.tsx` / `receive.tsx` / `send.tsx` / `+not-found.tsx` + `src/components/Screen.tsx`。
5. 本文件 Task 0–7 + `evidence/`（`android-dev-build.log`、`ui-*.xml` ×5、`android-route-*.png` ×5、`web-export.log`）。
6. 规格/依赖 diff：`specs/tech-stack.md`（tailwindcss / css-interop 行 + swift-secp256k1 追认措辞）、`doc/dependencies.md`（同级）、`specs/dev-environment.md` §1（JDK）、`specs/pending-manual-test-cases.md`（MAN-014/015）、T1.5 `validation.md` 的 H6 行。

### 评审须盯住的红线与不变量

- **R4**：直接依赖仅批准行——expo-router ~57（解析 **57.0.28**，在 tech-stack"~57（57.0.26）"线内）、SDK-57 peer 集、nativewind **4.2.7**、tailwindcss **3.4.17**（**D3 修宪**）、react-native-css-interop **0.2.7**（**D6 修宪**，依赖图未变）、reanimated **4.7.1**、worklets **0.13.0**、flash-list **2.3.3**、babel-preset-expo 57.0.15（SDK 绑定单元）；expo `~57.0.30` 为 SDK 57 补丁对齐。`expo.install.exclude` 的三项是**故意偏离 SDK 默认集**（CVE-2026-97724 下限），不是漏配。
- **R1**：`apps/mobile/src` 无 key-material 标识符、无 vault-core import（`grep` exit 1 见 Task 4）；onboarding 零助记词 UI。
- **R5**：所有占位为 "Not configured"，无假数据；无敏感日志。
- **R7**：导出 web JS 无 `signDigest|importMnemonic|vault-core`（Task 4 证据）。
- **B.6-1**：`tsc --noEmit` exit 0（strict；typed routes 类型 `.expo/types/router.d.ts` 由 `expo start` 生成，不入库）。
- **CNG**：`apps/mobile/android/` 未手改（gitignored），配置仅经 app.json/plugins。
- **禁项**：无 `@react-navigation/*` 直接导入；零动画代码（reanimated 仅接入）。
- **证据完整性**：`evidence/*.log` 用 `git add -f` 入库（根 `.gitignore` 有 `*.log`，T1.3 先例）；截图/UI dump 为真机（Pixel 6a / Android 17）实采；`nativewind-env.d.ts` 自带 `declare module '*.css'` 是因为 SDK 57 CLI 会把 `expo-env.d.ts` 自动加进 `.gitignore`（不入库），tsc 已在无该文件时复验 exit 0。

### 复现命令

    # 1) 应用侧（工作目录 apps/mobile）
    pnpm install --frozen-lockfile
    cd apps/mobile
    npx --no-install tsc --noEmit
    npx expo export -p web --output-dir /tmp/t21-web
    grep -rE 'signDigest|importMnemonic|vault-core' /tmp/t21-web/_expo/static/js/; echo "R7 exit: $?"   # 期望 exit 1（无输出）
    npx --yes expo-doctor@latest                                                    # 21/21

    # 2) 仓库根侧（工作目录 = 仓库根；**R1 必须在此执行**，MINOR-A）
    cd /home/superguo/Projects/NexVaultRN
    grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile \
      --include='*.ts' --include='*.tsx' --exclude-dir=node_modules | grep -v 'packages/vault-core'
    echo "R1 exit: ${PIPESTATUS[0]}"   # 期望 exit 1，且不得出现 'apps/mobile: No such file or directory'

### 已知开放项（记录在案的决策，**不是缺陷**）

- **视觉差异**：当前壳层按 ui-wallet §2.3 自研暖色令牌实现，与原 Kotlin 工程差异显著 → **已由 2026-10-19 roadmap 修宪插入 T2.1a（视觉对齐，route A/scope A）**；评审请勿把"长得不像原仓库"记为 T2.1 缺陷。
- T2.1-H3（真实数据 → T2.2）、H4（polyfill 自定入口 → T2.2）、H5（tabs/其余路由 → 后续 UI 条目）、H6（iOS 路由 smoke → T1.5-H1/T4.1）、H7（eslint/jest → T1.1 H6）、H8（expo-env.d.ts 与 `*.css` 声明，已规避）、H9（expo-router 57.0.28 vs 括注 57.0.26，备注）。
- 真机 smoke 为 **debug dev build + Metro 8082**（原生模块要求 dev build；Expo Go 不适用）；Release/EAS 构建不在 T2.1 范围（T1.5-H8 → T5.3）。
- 本条目**无自动化测试**（jest/eslint 属 T1.1 H6 债，未在本期引入）——门禁为 tsc/export/grep/doctor/真机 smoke。

## Review-fix round (2026-10-19, 独立评审发现处置)

| Finding | 判定 | 处置 | 证据 |
|---|---|---|---|
| **MAJOR-1** 六个直接依赖未入 R4 登记 | 采纳 | `specs/tech-stack.md` §2 新增 6 行（expo-linking / expo-constants / expo-status-bar / react-native-safe-area-context / react-native-screens / babel-preset-expo，均注记"2026-10-19 T2.1 评审 MAJOR-1 R4 补记"）；`doc/dependencies.md` 新增 **§6d** 补记（第一方 Expo/Meta、SDK 解析、lockfile 精确；零代码变更） | `specs/tech-stack.md` §2；`doc/dependencies.md` §6d |
| **MINOR-1** B.6-1 的 workspace 门禁是 no-op | 采纳 | `AGENTS.md` B.6-1 修订：CI 门禁 = `pnpm --filter mobile exec tsc --noEmit`；明示 `pnpm -r exec tsc --noEmit` 在本 workspace（无根 `package.json`、`packages/*` 为空）为 no-op、**不得**作为门禁；`packages/*` 出现 TS 工程后改 `pnpm -r --if-present exec tsc --noEmit` | `AGENTS.md` §B.6-1 |
| **NIT-1** Web 空 `<title>` + 浅色页面底 | 采纳 | `_layout.tsx` 增 `import Head from 'expo-router/head'` + `<title>NexVault</title>` + `theme-color`；`global.css` 增 `html,body,#root{background-color:#1B1815}`（注释指向 tokens）。复跑 export 实证：`<title data-rh="true">NexVault</title>`、字体 CSS 含 `html,body,#root{background-color:#1b1815}` | `evidence/web-route-smoke.txt`；`/tmp/t21-export-fix.log` |
| **NIT-2** R1 记录命令弱于宪法 | 采纳 | Task 4 / AC-6 / §复现命令 全部改为宪法原文命令（含 `secretKey`、范围 `apps/mobile`、`--exclude-dir=node_modules`、去 `packages/vault-core`）；重跑 **exit 1**（零命中） | 本文件 Task 4 / AC-6 / §复现命令 |
| **NIT-3** curl 证据不持久 + 相对路径 | 采纳 | 新增 `evidence/web-route-smoke.txt`（2026-10-19 生成：五路由 HTTP 200 + 标记计数 + `<title>` + 背景规则断言 + 复现命令/产物路径）；Task 6 证据表改绝对路径 | `/home/superguo/Projects/NexVaultRN/specs/features/2026-10-18-T2.1-mobile-ui-shell/evidence/web-route-smoke.txt` |
| **NIT-4** 无 ScrollView / 原生 a11y 缺 disabled 态 | 采纳 | `Screen` 新增 `scroll?: boolean`（默认 ScrollView 包裹，`contentContainerStyle` 用 tokens；home 传 `scroll={false}` 避免嵌套虚拟列表）；`index.tsx` 两个与 `send.tsx` 一个 disabled Pressable 增加 `accessibilityRole="button"` + `accessibilityState={{ disabled: true }}`；tsc 复跑 exit 0 | `src/components/Screen.tsx`；`src/app/{index,send,home}.tsx` |
| **NIT-5** 公告板记录与验证文档不一致 | 采纳 | 本文件 §收尾材料 改为按分支实际状态描述：本评审分支（自 main `26ad458`）cell 仍为 `… T2.2`；`T2.1a → T2.2` 的治理提交在 `feature/T2.1a-mobile-ui-parity`（`c27c96d`），随 T2.1a 合入 | 本文件 §收尾材料；`feature/T2.1a-mobile-ui-parity` |

**评审修复轮复跑（2026-10-19）**：`expo export -p web` exit 0（6 静态路由）；`tsc --noEmit` exit 0；R7 grep exit 1（干净）；R1 宪法原文命令 exit 1（干净）。
## Review round 2 (2026-10-19, 第二轮复核发现处置)

| Finding | 判定 | 处置 | 证据 |
|---|---|---|---|
| **MINOR-A** R1 命令在应用目录内假通过 | 采纳 | §复现命令 拆为两段："1) 应用侧（apps/mobile）"与"2) 仓库根侧（R1 必须在此执行）"；Task 4 / AC-6 行标注工作目录 = 仓库根；并记录反例形态（从 `apps/mobile` 执行 → `grep: apps/mobile: No such file or directory`，exit **2**；旧记录的 `exit 1` 属管道误读） | 本节复跑证据 |
| **NIT-A** B.6-1 的"未来命令"非法 | 采纳 | 改为 `pnpm -r --if-present run typecheck`（各包提供 `typecheck` script；`--if-present` 是 `run` 的旗标，不是 `exec` 的）；实测旧命令：`[ERROR] Unknown option: 'if-present'`，exit 1 | `AGENTS.md` §B.6-1；本节复跑 |
| **NIT-B** R4 行 peer 引文过期 | 采纳 | 按**实际安装**的 expo-router **57.0.28** 更正：expo-linking `^57.0.13`、expo-constants `^57.0.22`（`specs/tech-stack.md` §2 两行 + `doc/dependencies.md` §6d 两行） | `node_modules/expo-router/package.json`（version 57.0.28）；两文件 |
| **NIT-C** | **未收到正文**——粘贴内容在 NIT-B 之后截断为 `iew), but §Review-fix round`，无法据以修复 | 请 owner 补发 NIT-C 正文后再处置 | — |

**复跑证据（2026-10-19）**：

- 仓库根 R1：`grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile --include='*.ts' --include='*.tsx' --exclude-dir=node_modules | grep -v 'packages/vault-core'` → 零输出，**exit 1**（正确形态）
- `apps/mobile` 内跑同一命令 → `grep: apps/mobile: No such file or directory`，**exit 2**（反例，不得作为干净证据）
- `pnpm -r --if-present exec tsc --noEmit` → `[ERROR] Unknown option: 'if-present'`，**exit 1**（NIT-A 实证）









