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
| R1 grep | `grep -rnEi 'mnemonic|privateKey|seedPhrase|xprv' src` | **1（零命中）**（首轮 1 命中为注释措辞，已改；重跑干净捕获 exit 1） | 密钥语义标识符干净 |
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
| 人工侧（owner 点走，MAN-014） | `[~]` 待走查——Metro 8082 保持常驻供走查（expo dev server job 未杀） |

## Task 6 — Web 路由 smoke（2026-10-18）

| 步骤 | 命令 | 观察结果 |
|---|---|---|
| 静态服务 | `python3 -m http.server 8931 -d /tmp/t21-web`（后台 job） | 常驻（供 owner 浏览器走查备选） |
| curl 四路由 | `curl -s -o /dev/null -w '%{http_code}'` `/index.html /home.html /receive.html /send.html /+not-found.html` | 全部 **HTTP 200**；四页各含 `Not configured` 标记 ≥1（+not-found 含其专属文案） |
| 导出日志 | `evidence/web-export.log` | `Static routes (6)`：/home /send / (index) /receive /_sitemap /+not-found |

### Task 6 状态

| 项 | 状态 |
|---|---|
| 自动侧（导出 + curl 四路由 200 + 标记） | `[x]` verified 2026-10-18 |
| 人工侧（owner 浏览器走查，MAN-015） | `[~]` 待走查（`npx expo start --web` 或 127.0.0.1:8931） |
## T2.1 总览（截至 2026-10-18 实现完成）

| 项 | 状态 |
|---|---|
| Task 0 规划 + R4/追认修订 | `[x]` verified |
| Task 1 安装 + 钉版 | `[x]` verified |
| Task 2 路由接线 + 四页空壳 | `[x]` verified（typed routes 生成物 + tsc + 真机渲染） |
| Task 3 NativeWind v4 + 令牌 + FlashList | `[x]` verified（tokens 逐值 = ui-wallet §2.3；tailwind 映射单一来源） |
| Task 4 自动门禁（export/tsc/R7/R1/doctor） | `[x]` verified |
| Task 5 Android smoke 自动侧 | `[x]` verified（真机五路由渲染 + 0 crash）；人工侧 MAN-014 `[~]` |
| Task 6 Web smoke 自动侧 | `[x]` verified（curl 四路由 200 + 标记）；人工侧 MAN-015 `[~]` |
| Task 7 收尾（Handoff + 公告板 + git） | `[~]` 待 owner：MAN-014/015 走查结论 + 闭项指令（本轮按要求停在实现完成处） |

## AC 对照（收尾核对，2026-10-19 复跑版）

| AC | 状态 | 证据 |
|---|---|---|
| AC-1 tsc exit 0 | `[x]` | 2026-10-19 复跑：`npx --no-install tsc --noEmit` → exit 0（含 typed routes `.expo/types/router.d.ts`） |
| AC-2 web export + R7 grep | `[x]` | 复跑：`expo export -p web` exit 0（6 静态路由）；`grep -rE 'signDigest|importMnemonic|vault-core' /tmp/t21-web/_expo/static/js/` exit 1（零命中） |
| AC-3 Android 路由 smoke | 自动侧 `[x]` / 人工侧 `[~]` | 2026-10-18：五路由 deep-link + UI dump + 截图（`evidence/ui-*.xml`）；2026-10-19 复验：TLS 通道重连、`adb reverse tcp:8082` + dev-server deep link 重新拉起，onboarding 页 UI dump 正常（红屏仅因传输切换后未重连，已恢复） |
| AC-4 Web 路由 smoke | 自动侧 `[x]` / 人工侧 `[~]` | 2026-10-18：curl `/index.html /home.html /receive.html /send.html` → 全部 200 + 标记（服务器 job 常驻 8931） |
| AC-5 依赖漂移 | `[x]` | `git diff apps/mobile/package.json`：仅批准行（expo-router + peer 集、nativewind/tailwindcss/css-interop、reanimated/worklets、flash-list、babel-preset-expo、expo 补丁对齐 + `expo.install.exclude`） |
| AC-6 R1 + Not configured | `[x]` | 2026-10-19 复跑 `grep -rnEi 'mnemonic|privateKey|seedPhrase|xprv' src` → exit 1（零命中）；四页 UI dump 均含 "Not configured" |
| AC-7 tokens = ui-wallet §2.3 | `[x]` | `src/theme/tokens.ts` 逐值比对（11 色 / 3 半径 / 2 间距 / 1 字体）；tailwind.config.js 全部从 tokens 映射 |

## Handoff 清单（下一 roadmap 条目规划轮逐条裁定，B.7-1/B.7-2）

| # | 事项 | 状态 | 建议处置 |
|---|---|---|---|
| T2.1-H1 | 人工走查：Android 路由点走（MAN-014） | `[ ]` open | owner 走查后回填机型/OS/结论，闭 AC-3 人工侧 |
| T2.1-H2 | 人工走查：Web 浏览器路由走查（MAN-015） | `[ ]` open | owner 走查后回填，闭 AC-4 人工侧 |
| T2.1-H3 | 真实数据接入（FlashList 真实列表、Zustand store、TanStack Query Provider 挂载） | `[ ]` open | replan → T2.2 |
| T2.1-H4 | 自定入口 polyfill 槽（`index.ts`：`react-native-get-random-values` 首行 + TextEncoder polyfill，ui-wallet §2.1 契约） | `[ ]` open | replan → T2.2（当前入口 = `expo-router/entry`，无 polyfill） |
| T2.1-H5 | 首页/历史/设置 tabs 导航（NativeTabs）与其余路由表页（unlock/receive-confirm/address-book 等） | `[ ]` open | replan → 后续 UI 条目（T4.x 流程条目） |
| T2.1-H6 | iOS 路由 smoke（本条目仅 Android/Web，符合任务界定；iOS 需 Mac/Xcode） | `[ ]` open | 随 T1.5-H1 真机门一并处理（T4.1） |
| T2.1-H7 | eslint/jest 引入（B.6-2/B.6-3 现为零覆盖；T1.1 scaffold 债 H6） | `[ ]` open | 维持 → T1.1 closure 吸收 |
| T2.1-H8 | `expo-env.d.ts` 被 SDK 57 CLI 自动加入 `.gitignore`（不入库）→ `*.css` 模块声明改由 `nativewind-env.d.ts` 自持 | 已规避（`[x]` 记录） | 备注：CI 裸检出下 tsc 已实证通过（2026-10-19 复跑） |
| T2.1-H9 | expo-router 解析为 57.0.28（tech-stack 括注 57.0.26，同为 ~57 线）；expo 基座 57.0.28→57.0.30 补丁对齐 | 已记录（`[x]` 备注） | 不需修宪（~57 范围内）；下次 R4 修订时同步括注 |

## 收尾材料（草案，待 owner 闭项指令后执行）

**Part A 关闭行（草案）**：
> - 2026-10-19：**T2.1 关闭（owner 裁定）**——Expo Router 57 Stack 四页壳（onboarding/home/receive/send + not-found，typed routes、暗色令牌、FlashList v2 占位）；NativeWind 4.2.7 + tailwindcss 3.4.17 + css-interop 0.2.7（R4 修宪 D3/D6）+ Reanimated 4.7.1/worklets 0.13.0/FlashList 2.3.3 精确钉版；门禁 tsc exit 0、web export exit 0（6 静态路由）、R7/R1 grep 零命中、expo-doctor 21/21；Android 真机（Pixel 6a / Android 17）五路由 deep-link 渲染 0 crash（UI dump + 截图）、Web curl 四路由 200；MAN-014/015 走查通过（owner）。证据：`/home/superguo/Projects/NexVaultRN/specs/features/2026-10-18-T2.1-mobile-ui-shell/validation.md`

**Part A 工程阶段 cell 更新（草案）**：`… 下一步 **T2.2（viem + polyfill + Zustand/Query 挂载 + 余额只读）**`

**git（草案）**：`git merge --no-ff -F /tmp/msg.txt` → main；删除 `feature/T2.1-mobile-ui-shell`；push 按 owner 指令（仅 feature/main 范围以 owner 确认为准）。






