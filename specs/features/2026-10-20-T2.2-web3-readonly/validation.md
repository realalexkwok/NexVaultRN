# T2.2 — web3 read-only adapter — validation

> 证据纪律（B.4）：每 Task = 命令原文 + 退出码/观察结果 + 构件绝对路径；仓库根 = `/home/superguo/Projects/NexVaultRN`。
> 实施日期：2026-10-20（14:39–15:0x +08:00）。分支 `feature/T2.2-web3-readonly`（基线 main `9118080`）。

## Task 0 — 预检、分支与 feature 目录

```bash
$ git status --short --branch
## main...origin/main [ahead 1]
$ git log --oneline -2
9118080 chore: install agent skill (T2.2 web3 read-only adapter — viem + polyfills + Zustand/Query)
e00b7ee docs(T2.1a): record closure merge 827cf6b, push range 98e0ba0..827cf6b, H1 done
$ git switch -c feature/T2.2-web3-readonly
Switched to a new branch 'feature/T2.2-web3-readonly'   # exit 0
$ mkdir -p specs/features/2026-10-20-T2.2-web3-readonly/evidence
```

- 预检（规划轮）：R4 五行核验 → 零修订（requirements §R4 预检）；`pnpm-workspace.yaml` 已含 `packages/*`；polyfill 注入点 `src/app/_layout.tsx` 确认；MAN 下一号 = MAN-017。
- 落盘：`requirements.md` / `plan.md` / 本文件。

## Task 1 — 依赖安装 + workspace 接线

新建：`packages/web3-adapter/package.json`（name `web3-adapter`、private、**`main = src/index`（无扩展名，Metro 平台解析；review M2 修复）**、`types = src/index.ts`、dep `viem 2.56.9`、`typecheck` 委派脚本）、`packages/web3-adapter/tsconfig.json`（strict/noEmit/ES2022/bundler/skipLibCheck/types []）。修改：`apps/mobile/package.json`（+6 依赖、+1 `expo.install.exclude`）。

```bash
$ pnpm install
Scope: all 2 workspace projects
✓ Lockfile passes supply-chain policies (628 entries in 3.3s)
Packages: +19
Done in 8.5s                  # exit 0
```

版本探针（`node -p require(...).version`，apps/mobile）：`viem 2.56.9` / `@tanstack/react-query 5.104.2` / `zustand 5.0.15` / `react-native-get-random-values 2.0.0` / `fastestsmallesttextencoderdecoder 1.0.20`。
adapter 链接：`packages/web3-adapter/node_modules/viem -> ../../../node_modules/.pnpm/viem@2.56.9_typescript@6.0.3_zod@3.25.76/node_modules/viem`；lockfile importer `packages/web3-adapter: viem specifier 2.56.9 → 2.56.9(typescript@6.0.3)(zod@3.25.76)`。

新增解析包（全部为已核准行的传递依赖；无删除、无升级；review N6 补全）：`abitype@1.2.3`、`eventemitter3@5.0.1`、`fast-base64-decode@1.0.0`、`isows@1.0.7`、`ox@0.14.45`、`ws@8.21.0`、**`@noble/curves@1.9.1`、`@noble/hashes@1.8.0`、`@noble/ciphers@1.3.0`、`@scure/base@1.2.6`、`@scure/bip32@1.7.0`、`@scure/bip39@1.6.0`、`@adraffy/ens-normalize@1.11.1`、`@tanstack/query-core@5.104.2`**。

- **Peer 警告（预存在）**：`expo-modules-core@57.0.23` 期望 worklets `^0.7.4…^0.10.0`，实装 `0.13.0`（CVE-2026-97724 下限，tech-stack 已钉）——T2.1a 即存在，非本条目引入。
- **expo-doctor 回归与处置**：首次接入后 doctor exit 1（`react-native-get-random-values` 期望 `~1.11.0`、实装 `2.0.0` = R4 钉版，`2.0 起仅新架构/RN ≥0.81`，本工程 RN 0.86.3）；按文档化机制加入 `expo.install.exclude`（scaffold 配置，非依赖变更）→ 复跑 **21/21 通过**。

## Task 2 — polyfill 契约 + Query Provider

`apps/mobile/src/polyfills.ts`（新建；**两行 import 在文件最前**，注释在后）：
```ts
import 'react-native-get-random-values';
import 'fastestsmallesttextencoderdecoder';
```
`apps/mobile/src/app/_layout.tsx`：第 1 行 `import '../polyfills';`（grep 实测）；模块级 `QueryClient`：`staleTime 30_000` / `retry 2` / `refetchInterval 30_000` / `refetchIntervalInBackground false` / **`structuralSharing false`**（**防御性**：跳过默认 `replaceEqualDeep` 深比较；v5 缓存不 JSON 序列化、primitive 用 `===` 比较并原样复制，bigint 本安全——review N1 更正，早前「bigint 会被 JSON 序列化破坏」的表述错误）；`<QueryClientProvider client={queryClient}>` 包住 `<SafeAreaProvider>`（keyed Stack 重挂载不重挂 Provider）。

位置口径（O-5）：spec §2.1 写 `apps/mobile/polyfills.ts`，ui-wallet §2.1 要求根 `_layout` 第一行 `import '../polyfills'`（相对 = `src/polyfills.ts`）——取后者的位置（`src/polyfills.ts`），契约顺序不变。

## Task 3 — adapter 源码 + specs/web3-adapter.md 修订

新建：`packages/web3-adapter/src/errors.ts`（`W3AError` + `E_W3A_UNKNOWN_CHAIN`，T4.x 码仅注释）、`chains.ts`（`ChainConfig` / `SUPPORTED_CHAINS` / `getChainConfig` / 内部 `toViemChain`）、`balance.ts`（`getNativeBalance` / `getLatestBlockNumber` / `formatNativeBalance` / `isValidAddress` / `normalizeAddress`）、`index.ts`、`index.web.ts`（同为只读 barrel，无 `toViemChain`）。

```bash
$ pnpm --filter mobile exec tsc --noEmit -p ../../packages/web3-adapter/tsconfig.json
(no output)                   # exit 0
$ grep -rnE 'privateKeyToAccount|createWalletClient|signDigest|importMnemonic|broadcastSigned|signPrepared' packages/web3-adapter/src | grep -vE '^[^:]+:[0-9]+: *//'
CODE_SYMBOL_EXIT=1            # 代码行零命中（仅注释里的 T4.x 指针行命中，已排除）
```

规格修订（`specs/web3-adapter.md`）：§2.2 追加 T2.2 交付注记（端点/错误码/failover）；§2.5 追加交付口径（`getNativeBalance(address, chainId)` 由 `getBalance` 更名，参数顺序不变；`getTransactionStatus`/`buildUnsignedTxQrPayload` 标 T4.x）；文末附录 `T2.2 交付（只读子集，2026-10-20）`：AC-9..AC-12 + TC-W3A-009..013（gherkin）。

## Task 4 — store + Home + ui-wallet + MAN-017

新建 `apps/mobile/src/store/observer.ts`（Zustand v5：`selectedChainId` 默认 1、`watchedAddresses`、`setSelectedChain`、`setWatchedAddress`；无持久化、无密钥语义）。修改 `apps/mobile/src/components/NexVaultTextField.tsx`（新增可选 `autoCapitalize` / `onBlur` / `onSubmitEditing` 透传，视觉零变化）、`apps/mobile/src/app/(tabs)/home.tsx`（链 pill → 点按切换；watch 地址输入；`useQuery(['native-balance', chainId, address])` + `Promise.all(getNativeBalance, getLatestBlockNumber)`；四态；Total Balance + caption；Tokens 单行 ETH）。

```bash
$ pnpm --filter mobile exec tsc --noEmit
FORM1_EXIT=0
$ grep -rnE 'AsyncStorage|SecureStore|expo-secure-store' apps/mobile/src/store apps/mobile/src/app/'(tabs)'/home.tsx
# 仅 observer.ts 注释行命中（说明文字），无 import/调用
```

规格修订：`specs/ui-wallet.md` §2.1 home 行注记 + §5 TC-UI-010（四态 gherkin）；`specs/pending-manual-test-cases.md` 登记 **MAN-017**（余额只读走查）。

## Task 5 — 静态门禁（终态复跑）

| # | 命令 | 观察结果 | 判定 |
|---|---|---|---|
| 1 | `pnpm --filter mobile exec tsc --noEmit` | FORM1_EXIT=0 | ✓ |
| 2 | `pnpm -r --if-present run typecheck` | FORM2_EXIT=0（adapter 委派脚本执行 `pnpm --filter mobile exec tsc --noEmit -p ../../packages/web3-adapter/tsconfig.json`；mobile 无 typecheck script 由形态 1 覆盖） | ✓ |
| 3 | `npx expo export --platform web --output-dir /tmp/t22-web` | EXPORT_EXIT=0；**15 静态路由**；终态 entry `entry-fd1422cf11f7ad4bf46cc42d28d8e2b4.js` = **1,972,693 B**（review 修复前为 `entry-a8f1f27a…` = 1,971,473 B） | ✓ |
| 4 | `grep -rE 'signDigest|importMnemonic|vault-core' /tmp/t22-web/_expo/static/js/` | R7_EXIT=1（零命中） | ✓ |
| 5 | `grep -rE 'signPrepared|signAndBroadcast|privateKeyToAccount|mnemonicToAccount' /tmp/t22-web/_expo/static/js/` | SIGN_EXIT=1（零命中） | ✓ |
| 6 | `grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile packages --include='*.ts' --include='*.tsx' --exclude-dir=node_modules \| grep -v 'packages/vault-core'` | R1_EXIT=1（零命中） | ✓ |
| 7 | R5 console grep（宪法原文） | R5_EXIT=1（零命中） | ✓ |
| 8 | `npx expo-doctor`（apps/mobile） | **21/21 checks passed** | ✓ |
| 9 | `git diff --stat`（期望：仅五行 + workspace link + exclude 行） | `apps/mobile/package.json +8/-1`；`pnpm-lock.yaml +221`；新增 `packages/web3-adapter/**` | ✓ |
| 10 | 产物端点断言 | `grep -c eth.drpc.org` = 1；`grep -c eth.llamarpc.com` = 0 | ✓ |

- **H4 implement（T2.1a Handoff）**：`AGENTS.md` B.5 R7 示例命令 `-o dist-web` → `--output-dir dist-web`（行 179），证据用新文本复跑（行 4）。
- **O-4（review M4 更正，实测矩阵）**：web bundle 的 viem root-barrel surface——`createWalletClient` **3**、`signMessage` **10**、`signTransaction` **10**、`signTypedData` **7**；**密钥材料符号 0**（`privateKeyToAccount` / `mnemonicToAccount` / `hdKey` / `signDigest` / `importMnemonic` / `vault-core`）。viem wallet-action 模块随根导入进入（Metro 不做 tree-shaking），但无账户对象即无法签名；R7 宪法 grep 干净。AC-11 措辞已收窄为「无 vault-core / 密钥材料导出」；T4.x 若需更严（AC-6 子路径收敛），在 T4.1 规划轮处理。

### 5.1 Review round 1 修复后复跑（2026-10-20 19:0x）

| # | 项 | 结果 |
|---|---|---|
| 1 | `pnpm --filter mobile exec tsc --noEmit` | 0 |
| 2 | `pnpm -r --if-present run typecheck` | 0 |
| 3 | `npx expo export --platform web --output-dir /tmp/t22-web` | 0；15 路由；`entry-fd1422cf…js` = 1,972,693 B |
| 4 | R7 / R1 / R5 grep | 全部 exit 1（零命中） |
| 5 | `expo-doctor` | 21/21 |
| 6 | **M2 平台 barrel 实证** | `npx expo export --platform web --dev` → dev bundle `entry-387a433f27736775868d30a7f5717939.js` 模块路径：`packages/web3-adapter/src/index.web.ts` ×1、`index.ts` ×0（`chains.ts`/`balance.ts`/`errors.ts` 各 ×1）——证据 `evidence/m2-web-barrel-proof.txt`；SDK 57 `expo export` 无 `--dump-sourcemap`，故以 dev bundle 的模块路径代替 source map |
| 7 | **M4 符号矩阵** | 见 O-4（`createWalletClient` 3 / `signMessage` 10 / `signTransaction` 10 / `signTypedData` 7 / 密钥材料符号 0） |

## Task 6 — balance walk（自动化侧）

### 6.1 端点探针与登记表修订（O-3）

`evidence/rpc-endpoint-probe.txt`：`https://eth.llamarpc.com` → **HTTP 525**（不可用）；`https://rpc.sepolia.org` → 空响应；`https://cloudflare-eth.com` → `-32046 Cannot fulfill request`。可用且 CORS 开放：`https://eth.drpc.org`（CORS 回显 Origin）、`https://sepolia.drpc.org`、`https://ethereum-rpc.publicnode.com`、`https://ethereum-sepolia-rpc.publicnode.com`（CORS `*`）。
→ 登记表按「≥2 独立运营商 + CORS 可用」调整为：mainnet = `eth.drpc.org` + `ethereum-rpc.publicnode.com`；sepolia = `sepolia.drpc.org` + `ethereum-sepolia-rpc.publicnode.com`（O-3，公开端点、无 API key、非 R4 变更）。

### 6.2 RPC 转录（Web 侧证据，`evidence/`）

| 文件 | 内容（2026-10-20 19:14:50 +08，与设备复走同轮次） |
|---|---|
| `curl-mainnet-drpc.json` | `0x9858…Eda94` → `0x0`；`0xd8dA…6045` → `0x501eb617f2372aeb`；blockNumber → `0x1904a56` = **26,233,430** |
| `curl-mainnet-publicnode.json` | 同上两地址 → `0x0` / `0x501eb617f2372aeb`；blockNumber → `0x1904a56` = **26,233,430** |
| `curl-sepolia-blocknumber.json` | 两个 Sepolia 端点均 → `0xb66af3` = **11,954,931**（19:15:23 +08） |
| `curl-mainnet-llamarpc.json` | HTTP 525（探针留档，证明替换理由） |

交叉计算（viem `formatUnits`，node @ apps/mobile）：`0x501eb617f2372aeb` = `5773251986299759339` wei → `5.773251986299759339` → display **`5.7732`**；`0x0` → **`0`**。

### 6.3 Web 静态导出路由 smoke（`evidence/web-serve-smoke.txt`）

`npx expo serve --port 8080`（dist = /tmp/t22-web 副本）：`/` 200、`/home` 200、`/history` 200、`/settings` 200、`/receive` 200、`/send` 200；bundle `entry-a8f1…js` 200 / 1,971,473 B。

### 6.4 Android 真机 walk（release APK，零 Metro）

- 构件（review 修复后重构建，19:08）：`/home/superguo/Projects/NexVaultRN/apps/mobile/android/app/build/outputs/apk/release/app-release.apk` = **46,756,998 B**（内置 bundle **4,129,056 B**；arm64；`BUILD SUCCESSFUL in 1m 37s`；`JAVA_HOME=/home/superguo/tools/jdk-17`，见 spec dev-environment §1）。
- 设备：Pixel 6a / Android 17（`CP2A.260705.006`，`bluejay`，user/release-keys）；本轮 `adb devices` 串口 **`10.42.0.139:35343`**（会话内曾为 `:39871`，重连后变更）；`adb install -r` → Success；输入方法 = Gboard **Alphabet** 子类型（中文拼音子类型会把 adb 注入的十六进制串组合成汉字，见 §6.5）。

| # | 操作 | 观察（UI dump，2026-10-20 19:14–19:16 +08 复走） | 证据 |
|---|---|---|---|
| 1 | 冷启动 deep-link `nexvault://home`（无地址） | `Chain · Ethereum` / `Watch address` / `Total Balance` / **`Not configured`** / `— (24h)` / Tokens `Not configured` | `shell-home-empty.png` |
| 2 | 输入 `0xd8dA…6045` | **`5.7732 ETH`**；caption **`Ethereum · Block #26233430`**；Tokens 行 `Ethereum / ETH / 5.7732` | `shell-home-balance-vitalik.png` |
| 3 | 输入 `0x9858…Eda94` | **`0 ETH`**；`Ethereum · Block #26233433` | `shell-home-balance-anchor1.png` |
| 4 | 点按链 pill → Sepolia | pill `Chain · Sepolia`；**`0 ETH`**；**`Sepolia · Block #11954933`** | `shell-home-balance-sepolia.png` |
| 5 | 输入 `0x123` 并失焦（N3/N4 回归） | 输入框 **`Invalid address`**；Total Balance **`Not configured`**；Tokens **`Not configured`**（错误输入不再冒充已配置） | `shell-home-invalid.png` |

一致性（review N2 复算）：设备显示值 == curl wei 交叉计算值（5.7732）；主网 transcripts `0x1904a56` = **26,233,430** @19:14:50 → 设备 anchor2 **#26233430**（同块）→ anchor1 **#26233433**（+3 块 ≈ 36 s，~12 s/块 ✓）；Sepolia transcripts `0xb66af3` = **11,954,931** @19:15:23 → 设备 **#11954933**（+2 块 ≈ 24 s ✓）。

### 6.5 设备输入方法注记（复走期间）

复走首次尝试 `adb shell input text` 产生汉字乱码（Gboard 简体拼音子类型把十六进制串组合为中文，如 `0x`→`0在`）；切换 Gboard 子类型为 **Alphabet** 后同一命令逐字正确（`FIELD=0x9858EfFD232B4033E47d90003D41EC34EcaEda94` 实测）。属设备侧输入法状态，非应用缺陷；§6.4 证据均来自 Alphabet 子类型会话。

## Task 7 — MAN-017 走查（review round 1 M1 更正）

- **状态：☐ 待走查**（MAN-017 未通过）。`plan.md` Task 7 明示「owner 走查后回填；若 owner 未即时走查：MAN-017 保持 ☐ 待走查」——此前记录的「☑ 通过（agent 走查）」**作废**。
- **已完成的自动侧**：RPC curl 转录（drpc/publicnode）+ `expo serve` 六路由 200。
- **已完成的 Android 真机侧（agent）**：review 修复后重构建 APK 的**五态**截图 + UI dump（§6.4，2026-10-20 19:14–19:16 +08 复走，含 N3/N4 错误态回归）。
- **未完成的手工侧**：**Web 桌面浏览器**输入地址并截图/UI dump（MAN-017 与 reviewer M1 要求）——待 owner 走查后回填并通过。
- 同步更正：`requirements.md` AC-7 的验证方式列（自动侧/手工侧分列，消除与 MAN-017 的口径矛盾）。

## Task 8 — 收尾

## Closure（2026-10-20）

- **关闭依据**：owner 指令「Plan + implement T2.2 … close with Part A board line + git per B.3」（2026-10-20）；plan 经 `exit_plan_mode` `approved: true`；Q/A 推荐答案随批准生效（无 override）。
- **关闭门禁复跑（2026-10-20 15:0x 首轮 + 19:0x review 修复后复跑）**：`pnpm --filter mobile exec tsc --noEmit` exit 0；`pnpm -r --if-present run typecheck` exit 0；`npx expo export --platform web --output-dir /tmp/t22-web` exit 0（**15 静态路由**；终态 entry `entry-fd1422cf11f7ad4bf46cc42d28d8e2b4.js` = **1,972,693 B**）；R1 / R5 / R7 grep exit 1（零命中）；`expo-doctor` **21/21**。
- **balance walk（AC-7）**：review 修复后重构建 release APK `apps/mobile/android/app/build/outputs/apk/release/app-release.apk` = **46,756,998 B**（内置 bundle 4,129,056 B；arm64；`BUILD SUCCESSFUL in 1m 37s`；零 Metro）——复走 19:14–19:16 +08：anchor2 **`5.7732 ETH`** / **`Ethereum · Block #26233430`**；anchor1 **`0 ETH`** / `#26233433`；Sepolia **`Block #11954933`**；错误输入态 **`Invalid address` + `Not configured`**；Web 侧同轮次 curl 转录（drpc/publicnode `0x1904a56` = 26,233,430）+ `expo serve` 六路由 200。**MAN-017 ☐ 待 owner 浏览器走查**（Android 真机侧 + Web 自动侧已完成；review round 1 M1 更正——原「通过」记录作废）。
- **规格修订**：`specs/web3-adapter.md`（§2.2/§2.5 注记 + 附录 AC-9..AC-12 / TC-W3A-009..013）；`specs/ui-wallet.md`（§2.1 home 行 + §5 TC-UI-010）；`specs/pending-manual-test-cases.md`（MAN-017）；`AGENTS.md`（B.5 R7 `--output-dir`，T2.1a-H4；Part A 阶段 cell + 关闭行）。
- **合并/推送**：feature commit **`d3f773f`**；`git merge --no-ff feature/T2.2-web3-readonly` → **`82addd0`**（parents **`9118080` + `d3f773f`**——review N7 更正，`git show -s --format=%P 82addd0` 实测；合并时 main 位于 skill 提交）；分支已删除（was `d3f773f`）；`git push origin main` → **`e00b7ee..82addd0 main -> main`**；`origin/main` == 本地 HEAD（本记录提交另行追加）。


## Review round 1（2026-10-20，独立评审 → 修复）

| # | 评审发现 | 处置 | 证据 |
|---|---|---|---|
| **M1** | MAN-017 以 agent 走查记为 ☑ 通过，未含 plan 要求的 owner 浏览器走查；Web 半侧仅 curl/serve | **记录作废**：MAN-017 回退 ☐ 待走查（Android 真机 + Web 自动侧保留为部分证据）；AC-7 拆「自动侧 / 手工侧」两列；AGENTS 关闭行与 §Task 7 同步更正；新增 Handoff **H9** | `specs/pending-manual-test-cases.md` MAN-017；§Task 7；H9 |
| **M2** | `main: src/index.ts`（带扩展名）使 Metro 永不解析 `index.web.ts` | `main` 改**无扩展名** `src/index`（`types` 仍 `src/index.ts`）；dev bundle 模块路径实证 web 图只含 `index.web.ts`、无 `index.ts` | `evidence/m2-web-barrel-proof.txt`；§5.1 #6 |
| **M3** | §2.2 注记仍写已死端点（llamarpc/sepolia.org） | 注记改为**实装** drpc 端点并引 O-3（替换依据 + 探针转录） | `specs/web3-adapter.md` §2.2 注记 |
| **M4** | AC-11「零签名符号」措辞过宽、O-4 计数不全 | AC-11 收窄为「无 vault-core / 密钥材料导出」；O-4 记录实测矩阵（`createWalletClient` 3 / `signMessage` 10 / `signTransaction` 10 / `signTypedData` 7 / 密钥材料符号 0） | §5.1 #7；O-4 |
| **N1** | `structuralSharing` 理由错误（v5 用 `replaceEqualDeep`，缓存不 JSON 序列化） | `_layout.tsx` 注释 + §Task 2 文案更正；配置保留为防御性（跳过深比较） | 代码 + §Task 2 |
| **N2** | 区块号引用与存储证据不符 | 以**同轮次**新 transcripts + 设备复走重写一致性段（主网 `0x1904a56` = 26,233,430 ↔ 设备 26,233,430/433；Sepolia `0xb66af3` = 11,954,931 ↔ 设备 11,954,933） | §6.2 / §6.4 |
| **N3** | 地址提交语义：非法输入后旧地址仍被展示（评审原文经 paste 损坏，按「输入/展示一致性」语义处置并在此说明） | 空输入清空 watch；非法输入清空 + inline error；合法输入即时提交；展示的余额不可能对应与输入框不同的地址 | `home.tsx`；§6.4 #5；O-7 |
| **N4** | loading / RPC 错误时 Tokens 恒显示 "Not configured" | Tokens **四态**：无地址 = `Not configured`（R5 唯一出口）/ loading = 骨架行 / error = `—` / data = ETH 行 | `home.tsx`；§6.4 #5；O-7 |
| **N5** | `formatNativeBalance` 未传链 decimals（两链均 18，暂无可见缺陷） | 两处调用传入 `chain.nativeCurrency.decimals` | `home.tsx:147,244`；O-9 |
| **N6** | 新包清单不全 | 补齐 `@noble/curves|hashes|ciphers`、`@scure/*`、`@adraffy/ens-normalize`、`@tanstack/query-core` | §Task 1 |
| **N7** | merge parents 引用错误 | 更正为 **`9118080` + `d3f773f`**（`git show -s --format=%P 82addd0` 实测） | §Closure |

修复后复跑：tsc 双形态 0 / export 0（15 路由，`entry-fd1422cf…` 1,972,693 B）/ R1·R5·R7 exit 1 / doctor 21/21 / Android 真机复走五态通过（§6.4）。修复提交：`<pending>`。
## 记录偏差与观察

| # | 事项 | 处置 |
|---|---|---|
| D-i | Home 新增 watch 地址输入行（原仓库 Home 无输入） | owner Q1-A 批准；`home.tsx` 注释标注 |
| D-j | 链 pill 由静态占位改为 mainnet↔sepolia 点按切换（原仓库为下拉菜单） | owner Q3-A 批准 |
| O-1 | `doc/dependencies.md` viem 行安装位置 cell 写 `packages/vault-core`，与权威 `specs/tech-stack.md` 不一致 | doc/ 只读（B.1-2）；以 tech-stack 为准，记观察 |
| O-2 | tech-stack typescript 行 stale（`~5.9.x / root` vs 实际 `apps/mobile ~6.0.3`） | 用委派 typecheck 脚本绕开新 importer 条目，不触发 R4 修订 |
| O-3 | RPC 端点替换（llamarpc 525 / sepolia.org 空响应 → drpc；cloudflare-eth 不可用） | 探针证据留档；满足「≥2 独立运营商 + CORS」契约 |
| O-4 | viem root-barrel wallet surface 入 web bundle（`createWalletClient` 3 / `signMessage` 10 / `signTransaction` 10 / `signTypedData` 7；**密钥材料符号 0**） | 接受（无账户对象不可签名）；AC-11 措辞已收窄；T4.x AC-6 再评估子路径收敛 |
| O-5 | polyfills 位置（spec 文本 vs `import '../polyfills'` 相对路径） | 取 `src/polyfills.ts`，契约顺序不变 |
| O-6 | `structuralSharing` 理由表述错误（v5 用 `replaceEqualDeep`，缓存不经 JSON；bigint 本安全） | 注释 + validation + requirements 已更正（review N1）；配置保留为防御性 |
| O-7 | 地址提交语义与 Tokens 状态：空输入曾报 "Invalid address" 且保留旧地址；loading/error 时 Tokens 恒显示 "Not configured" | 已修（N3/N4）：空输入清空；非法输入清空 + 报错；输入即校验提交；Tokens 四态（无地址=Not configured / loading=骨架 / error=— / data=行） |
| O-8 | 包 `main` 带扩展名（`src/index.ts`）使 web 永不解析 `index.web.ts`（M2） | 已修：`main: src/index`；`types: src/index.ts`；dev bundle 模块路径实证（§5.1） |
| O-9 | `formatNativeBalance` 未传链 decimals（两链均 18，暂无可见缺陷） | 已修（N5）：两处调用传入 `chain.nativeCurrency.decimals` |

## Handoff 清单（T2.2 → T3.x / T4.x）

| # | 事项 | 递延理由 | 建议处置 | 关联 AC/TC |
|---|---|---|---|---|
| H1 | 交易/签名工作流（`prepareTransaction`/`signPrepared`/`broadcastSigned`） | T2.2 只读范围 | implement @ T4.1 | TC-W3A-002..004 |
| H2 | `readVerified` 主备交叉验证 + EIP-155 断言（`E_W3A_RPC_DIVERGENCE` 等） | 同上 | T4.1 | TC-W3A-005 |
| H3 | 链 failover live-fire 演练（可编程故障注入） | 需故障注入载具 | T4.x | TC-W3A-011 |
| H4 | watch 地址持久化（SecureStore/AsyncStorage） | 新依赖需 R4 提案 | 未来条目 | Q4 / AC-6 |
| H5 | 行情/图表数据（Total Balance 24h、chart、区间切换） | 非 T2.2 范围 | T4.x | Q2 |
| H6 | adapter jest 单测接线（纯函数格式化/未知链） | B.6-3 当前目标 = vault-core；本期以门禁 + 走查取材 | 未来 | B.6-3 |
| H7 | WalletConnect(Reown) 桥接 | v2 计划 | v2 | spec §7 |
| H8 | T2.1a H2/H3/H5/H6（filled 图标 / web glyph / 18 屏 / 地址等宽+渐变） | B.7-2 replan | T4.x（H3/H6 各自 R4 提案） | 见 requirements §Handoff |
| H9 | **MAN-017 的 Web 浏览器走查**（owner 人工侧） | 本期仅完成自动侧（curl/serve）与 Android 真机侧；plan 明示 owner 未走查时保持 ☐ | owner 在桌面浏览器走查两锚点 + 链切换后回填 ☑ | MAN-017 |