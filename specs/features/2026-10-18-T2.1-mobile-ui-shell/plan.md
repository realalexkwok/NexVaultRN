# T2.1 — mobile UI shell — plan

> Status: approved (owner 2026-10-18, plan-mode round) | 依据：requirements.md + SKILL.md（sha256 `133e4069…`）

## Task 0 — 规划产物 + R4/追认修订

- 分支 `feature/T2.1-mobile-ui-shell` 自 `main` `407d5ae`；基线记录。
- 落盘三件套 + SKILL.md（`.agents/` 原件不动，feature 目录为溯源副本 + sha256）。
- 执行 R4 修订（D1/D3 已批）：
  - `specs/tech-stack.md` §2：新增 `tailwindcss | 3.4.17（精确钉版）| apps/mobile（devDependency）` 行 + nativewind 行 companion 注记；swift-secp256k1 行"待 owner 追认"→"2026-10-18 owner 追认（T2.1 规划轮 D1）"。
  - `doc/dependencies.md`：nativewind 行备注追加 companion 钉版注记 + 新增 tailwindcss 行（3.4.17，2024-12-17 发布，冷静期天然满足；latest=4.3.3 属 Tailwind v4 线，随 NativeWind v5 RC 一并排除）；§6c 顶部修订段追认标记摘除。
- 文档纪律：记录已核对的参考 URL 与访问日期（expo.dev/router/installation.md，modified 2026-10-16；nativewind.dev v4 installation（llms.txt/mdx）；npm registry：nativewind 4.2.7 peers（tailwindcss >3.3.0）、expo-router 57.0.26 peers（expo-linking ^57.0.12 / expo-constants ^57.0.21 / @expo/log-box ^57.0.4 / @expo/metro-runtime ^57.0.16 / screens ^4.26.0 / safe-area-context >=5.4.0，@expo/metro-runtime 非 optional 且 expo 57.0.28 不带 → Task 1 条件安装）；版本存在性：reanimated 4.7.1 / worklets 0.13.0 / flash-list 2.3.3 / tailwindcss 3.4.17 均存在于 registry）。

## Task 1 — 安装（R4 精确钉版）

    cd apps/mobile
    pnpm exec expo install expo-router react-native-safe-area-context react-native-screens expo-linking expo-constants expo-status-bar
    pnpm exec expo install nativewind react-native-reanimated react-native-worklets @shopify/flash-list
    pnpm exec expo install --dev tailwindcss babel-preset-expo
    # 精确钉：nativewind 4.2.7 / reanimated 4.7.1 / worklets 0.13.0 / flash-list 2.3.3 / tailwindcss 3.4.17 / expo-router 57.0.26
    # 条件：@expo/metro-runtime（peer 检查或 expo-doctor 报缺时经 expo install 补，SDK 对齐）

证据：`git diff -- apps/mobile/package.json pnpm-lock.yaml`（仅批准行）、`pnpm list --depth 0`、`npx expo-doctor`。expo install 若解析出更高 ~57 patch，记录两值并保持 tech-stack 钉版可兼容者（偏差记 validation）。

## Task 2 — 路由接线 + 四页空壳

- `package.json` main → `expo-router/entry`；删除 `App.tsx`、`index.ts`；`app.json`（plugins 加 `expo-router`、`experiments.typedRoutes: true`、`userInterfaceStyle: "dark"`）；`tsconfig.json`（`@/*` → `./src/*`、include `.expo/types/**/*.ts` 与 `expo-env.d.ts`）。
- 新建 `src/app/_layout.tsx`（Stack、import `../../global.css`、expo-status-bar light）、`index.tsx`、`home.tsx`、`receive.tsx`、`send.tsx`、`+not-found.tsx`、`src/components/Screen.tsx`。占位一律 "Not configured"；onboarding 零助记词 UI。
- typed routes 类型由首次 start/export 生成 → **先跑 Task 4 的 web export 再跑 tsc**。

## Task 3 — NativeWind v4 + 令牌 + FlashList

- 五配置文件（babel/metro/tailwind/global.css/nativewind-env.d.ts）按 v4.2.7 文档形态（已核对的官方页），非 v5 RC、非记忆。
- `src/theme/tokens.ts` 逐值等于 ui-wallet §2.3；tailwind theme.extend 从 tokens 映射；四页零硬编码色值。
- home 静态 FlashList 占位行；reanimated/worklets 解析版本断言；零动画代码。

## Task 4 — 自动门禁（B.4 自动侧）

    cd apps/mobile
    npx expo export --platform web -o /tmp/t21-web            # exit 0；先于 tsc（typed-route 类型生成）
    grep -rE 'signDigest|importMnemonic|vault-core' /tmp/t21-web/_expo/static/js/ && exit 1   # R7：期望零命中
    npx --no-install tsc --noEmit                             # exit 0（B.6-1）
    grep -rnEi 'mnemonic|privateKey|seedPhrase|xprv' src 2>/dev/null || true    # R1：期望干净
    git diff --stat && git diff -- apps/mobile/package.json pnpm-lock.yaml     # 漂移证明

命令 + 退出码 + 观察结果逐条记 validation；export 日志落 `specs/features/2026-10-18-T2.1-mobile-ui-shell/evidence/`。

## Task 5 — Android 路由 smoke（Pixel 6a）

- `adb connect 192.168.1.5:45179`（规划时已离线）→ `npx expo run:android --device`（debug dev build；Reanimated/Worklets 原生 → Expo Go 出局）。
- 自动侧：`adb shell am start -a android.intent.action.VIEW -d 'nexvault://…'` 依次 `/`、`home`、`receive`、`send` + `adb exec-out screencap -p` 每路由 → evidence/；build 日志尾 `BUILD SUCCESSFUL`。
- 人工侧（owner，D4）：点走四路由，记录机型/OS/结论 → **MAN-014**。
- 设备接不上：如实 `[~]` + Handoff 重试，禁止假 `[x]`。

## Task 6 — Web 路由 smoke

- `python3 -m http.server 8931 -d /tmp/t21-web`（后台）→ curl `-s -o /dev/null -w '%{http_code} %{url_effective}'` 四路由 + body grep 标记文案 → 证据表。
- 人工侧（owner）：`npx expo start --web` 浏览器走查、console 无报错 → **MAN-015**。

## Task 7 — validation + Handoff + 收尾（owner-gated）

- validation.md 每任务一节（命令 + 退出码 + 观察结果）；漂移证明；文档 URL；MAN-014/015。
- Handoff 清单：T1.3-H8/T1.5-H2 → T4.1；T1.5-H1/H7/H8 落点；T1.1 H6；真实 FlashList 数据/zustand/Query → T2.2；T2.2 polyfill 自定入口（index.ts 重建）记入。
- **owner 决定闭项后**：Part A 公告板关闭行（日期 + 证据路径）→ commit → `git merge --no-ff -F /tmp/msg.txt` 进 main → 删 feature 分支 → push 仅凭 owner 明确指令（T1.4 教训：不提前闭项）。
- 实现完成后**停在此处**等 owner 换模型/审阅（本轮要求）。

## 风险与回退

- expo-router patch 漂移：记录 + 钉回，偏差入档。
- @expo/metro-runtime peer 缺：expo install 补（SDK 对齐）。
- FlashList web 导出失败：home 列表改 `.web.tsx` FlatList 回退并记录偏差（令牌/文案不变）。
- NativeWind × pnpm isolated：content glob 相对 apps/mobile；编译器找不到 tailwindcss 时先取 monorepo 指南（nativewind.dev/docs/guides/using-with-monorepos）再改。
- typed-route 类型未生成：export/start 先行 + tsconfig include 覆盖 `.expo/types`。
- Pixel 6a 离线 / owner 走查未及：如实 `[~]` + Handoff。
