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

**2026-10-22 — review round 4 的 R4 修订（owner 批准「Add Jest tests now」）**：
```bash
$ pnpm --filter mobile add -D jest@^29.7.0 jest-expo@57.0.6     # +252 packages
$ pnpm --filter mobile add -D @jest/globals@^29.7.0            # +5 packages
$ pnpm install                                                 # exit 0; 'Lockfile is up to date'; supply-chain policy pass
$ pnpm audit                                                   # 2 high + 4 moderate; 1 NEW dev-only (sprintf-js via jest) + 5 pre-existing
```
- 实测版本：jest **29.7.0** / jest-expo **57.0.6** / @jest/globals **29.7.0**（package.json 已钉精确版本）；安装位置 **apps/mobile devDependencies**。
- lockfile `packages`：**647 → 862（+215）**（复现命令：`git show 6ca3885:pnpm-lock.yaml | sed -n "/^packages:/,/^[a-z]/p" | grep -c '^  [^ ]'`；`pnpm audit --json` → `totalDependencies: 862`）；`@jest/globals` 在 `6ca3885` 为 **0** 处，现 **5** 处。（此前记录的 1,032→1,384 无复现命令，**已撤回**——review round 6 MINOR-2。）
- `pnpm audit`：2 high（node-forge、braces）+ 4 moderate（uuid、decode-uri-component、**sprintf-js**、**postcss-selector-parser**）。**更正（review round 6 MAJOR-1）**：`sprintf-js@1.0.3` 在 `6ca3885` 不存在，由 jest 引入（`@jest/transform → babel-plugin-istanbul → @istanbuljs/load-nyc-config → js-yaml → argparse → sprintf-js`，dev-only，无补丁版本）；braces 既有（tailwind/chokidar），jest 另增 `@jest/core → micromatch → braces` 路径；其余为既有 toolchain。**处置：接受为 dev-only 残余风险**（不进入 web/native 产物；jest 仅开发/CI 运行），依据 owner 2026-10-22「Add Jest tests now」批准；显式 advisory 确认已于同日 ask（10 分钟窗口）未获回复，记录备查，revert 路径保留（评审选项 c）。
- 记录更正（review round 2 MAJOR-1）：此前「零新包 / 零新解析」**不成立**；逐行记录见 `specs/tech-stack.md` jest 行与 `doc/dependencies.md`（jest / @jest/globals 两行）。

## Task 2 — polyfill 契约 + Query Provider

`apps/mobile/src/polyfills.ts`（新建；**两行 import 在文件最前**，注释在后）：
```ts
import 'react-native-get-random-values';
import 'fastestsmallesttextencoderdecoder';
```
`apps/mobile/src/app/_layout.tsx`：第 1 行 `import '../polyfills';`（grep 实测）；模块级 `QueryClient`：**仅** `staleTime 30_000` + **`structuralSharing false`**（`retry 2` / `refetchInterval 30_000` / `refetchIntervalInBackground false` 已移至 home 的 balance `useQuery`，review round 4 MINOR-4）（**防御性**：跳过默认 `replaceEqualDeep` 深比较；v5 缓存不 JSON 序列化、primitive 用 `===` 比较并原样复制，bigint 本安全——review N1 更正，早前「bigint 会被 JSON 序列化破坏」的表述错误）；`<QueryClientProvider client={queryClient}>` 包住 `<SafeAreaProvider>`（keyed Stack 重挂载不重挂 Provider）。

位置口径（O-5）：spec §2.1 已修订为 `apps/mobile/src/polyfills.ts` + 「根 `_layout.tsx` 第一条 import」；ui-wallet §2.1 的 `import '../polyfills'` 与之一致。

**AC-1 / AC-3 门禁命令（review 轮 1 MAJOR-2 补齐，B.4-2）**：
```bash
$ head -1 apps/mobile/src/app/_layout.tsx
import '../polyfills';                 # exit 0
$ head -2 apps/mobile/src/polyfills.ts
import 'react-native-get-random-values';
import 'fastestsmallesttextencoderdecoder';   # exit 0（顺序断言）
$ grep -n '{ rank: false }' packages/web3-adapter/src/balance.ts
16:      { rank: false },               # exit 0（AC-3 代码路径）
```
AC-3 的 live-fire 故障注入仍为 **H3**（递延），本行只证明代码路径存在。

## Task 3 — adapter 源码 + specs/web3-adapter.md 修订

新建：`packages/web3-adapter/src/errors.ts`（`W3AError` + `E_W3A_UNKNOWN_CHAIN`，T4.x 码仅注释）、`chains.ts`（`ChainConfig` / `SUPPORTED_CHAINS` / `getChainConfig` / 内部 `toViemChain`）、`balance.ts`（`getNativeBalance` / `getLatestBlockNumber` / `formatNativeBalance` / `isValidAddress` / `normalizeAddress`）、`public-api.ts`（**单一导出面**，两 barrel `export *`，review round 4 MINOR-6+）、`index.ts`、`index.web.ts`（同为只读 barrel，无 `toViemChain`）。

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
| 2 | `pnpm -r --if-present run typecheck` | FORM2_EXIT=0（adapter 委派脚本执行 `pnpm --filter mobile exec tsc --noEmit -p ../../packages/web3-adapter/tsconfig.json`；mobile 无 typecheck script 由形态 1 覆盖）。**审核轮 2 BLOCKER-1 更正**：该行此前为红（`fetch` 缺 DOM lib），已修 `packages/web3-adapter/tsconfig.json`（lib 加 DOM）后复跑 0 | ✓ |
| 2b | `pnpm --filter mobile exec jest --ci` | **3 suites / 25 tests passed**（adapter 10 + web 11 + store 4；审核轮 2 增 stall/timeout、checksum 分歧、web barrel 表面 3 例） | ✓ |
| 2c | `head -1 apps/mobile/src/app/_layout.tsx` / `head -2 apps/mobile/src/polyfills.ts` / `grep -n '{ rank: false }' packages/web3-adapter/src/balance.ts` | 三条命令 exit 0；输出见 §Task 2 命令块（AC-1 顺序 + AC-3 代码路径） | ✓ |
| 3 | `npx expo export --platform web --output-dir /tmp/t22-web-canon4 --clear` | EXPORT_EXIT=0；**15 静态路由**；**canonical（2026-10-22 审核轮 3）**：`entry-828c830cd0e4acaa015173fc3a8a3741.js` = **1,331,901 B**（此前 canon3/`52f35f` 为审核轮 2 历史；较拆分前 1,973,511 B **−641,610 B**）。每轮记录 **hash + bytes**，不作跨构建体积相等断言 | ✓ |
| 4 | `grep -rE 'signDigest|importMnemonic|vault-core' /tmp/t22-web-canon4/_expo/static/js/` | R7_EXIT=1（零命中） | ✓ |
| 5 | `grep -rE 'signPrepared|signAndBroadcast|privateKeyToAccount|mnemonicToAccount' /tmp/t22-web-canon4/_expo/static/js/` | SIGN_EXIT=1（零命中） | ✓ |
| 5b | 全图矩阵（canon4 `entry-828c830c…js`，审核轮 3） | `@noble/curves` 0 · `@noble` 0 · `@scure` 0 · `secp256k1` 0 · `createWalletClient` 0 · `signMessage` 0 · `signTransaction` 0 · `HDKey` 0 · `mnemonicToSeedSync` 0 · `viem` 0（R7 web split 达成，AC-6/TC-W3A-006） | ✓ |
| 6 | `grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile packages --include='*.ts' --include='*.tsx' --exclude-dir=node_modules \| grep -v 'packages/vault-core'` | R1_EXIT=1（零命中） | ✓ |
| 7 | R5 console grep（宪法原文） | R5_EXIT=1（零命中） | ✓ |
| 8 | `npx expo-doctor`（apps/mobile） | **21/21 checks passed** | ✓ |
| 9 | `git diff --stat`（期望：仅五行 + workspace link + exclude 行） | `apps/mobile/package.json +8/-1`；`pnpm-lock.yaml +221`；新增 `packages/web3-adapter/**` | ✓ |
| 10 | 产物端点断言 | `grep -c eth.drpc.org` = 1；`grep -c eth.llamarpc.com` = 0 | ✓ |

- **H4 implement（T2.1a Handoff）**：`AGENTS.md` B.5 R7 示例命令 `-o dist-web` → `--output-dir dist-web`（行 179），证据用新文本复跑（行 4）。
- **O-4（2026-10-22 复核重写）**：拆分前的 web bundle 曾含 viem root-barrel surface（`createWalletClient` 3、`signMessage` 10、`signTransaction` 10、`signTypedData` 7）与 `@noble/curves`/`@scure` 图——**review 轮 1 MAJOR-1 判为 R7/AC-6 违约**；owner 裁定走 **web split**（方案 a）。现 web 读路径 = `balance.web.ts`（fetch JSON-RPC）+ `chains.web.ts`（纯注册表）+ `address.web.ts`（正则/lowercase），**全矩阵 0**（行 5b），bundle 减 641,809 B；原生仍用 viem（`chains.ts`/`balance.ts`/`address.ts`）。AC-4 措辞已扩为「无 viem 图/签名路径」；T4.x 若需更严（AC-6 子路径收敛），在 T4.1 规划轮处理。

### 5.1 Review round 1 修复后复跑（2026-10-20 19:0x）

| # | 项 | 结果 |
|---|---|---|
| 1 | `pnpm --filter mobile exec tsc --noEmit` | 0 |
| 2 | `pnpm -r --if-present run typecheck` | 0 |
| 2b | `pnpm --filter mobile exec jest` | **2 suites / 13 tests pass**（round 4 = adapter 10；round 5 = 13（+store 3）；round 6 不变） |
| 3 | `npx expo export --platform web --output-dir /tmp/t22-web` | 0；15 路由；**该轮（review round 1 修复后）entry = 1,972,693 B**（历史值；后续轮以 Task 5 当轮 hash+bytes 为准） |
| 4 | R7 / R1 / R5 grep | 全部 exit 1（零命中） |
| 5 | `expo-doctor` | 21/21 |
| 6 | **M2 平台 barrel 实证** | ① **生产 source map**（`npx expo export --platform web --output-dir /tmp/t22-web-map --source-maps external`，exit 0）：`entry-160b032e…js.map` 含 `packages/web3-adapter/src/index.web.ts` ×1、**不含 `index.ts`**（`chains.ts`/`balance.ts`/`errors.ts` 各 ×1）；② dev bundle `entry-387a433f…js` 模块路径同结论。证据 `evidence/m2-web-barrel-proof.txt`（review NIT-1 更正：`expo export` **确有** `-s, --source-maps external`，早前「无 source-map 旗标」的表述错误） |
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

`npx expo serve --port 8080`（dist = **审核轮 3 canonical 导出** `/tmp/t22-web-canon4`，2026-10-22T07:12:34Z = 15:12 +08 复跑）：`/` 200、`/home` 200、`/history` 200、`/settings` 200、`/receive` 200、`/send` 200；bundle `entry-828c830c…js` 200 / **1,331,901 B**（证据 `evidence/web-serve-smoke.txt`；此前的 fd1422/160b/18dbd8d0/456faa1f/52f35f21 为历史记录）。

### 6.4 Android 真机 walk（release APK，零 Metro）

- 构件（review 修复后重构建，2026-10-20 19:08）：`/home/superguo/Projects/NexVaultRN/apps/mobile/android/app/build/outputs/apk/release/app-release.apk` = **46,756,998 B**（内置 bundle **4,129,056 B**；arm64；`BUILD SUCCESSFUL in 1m 37s`；`JAVA_HOME=/home/superguo/tools/jdk-17`，见 spec dev-environment §1）。**构件版本注记（review round 9 MINOR-2）**：该 APK 的源码修订 = **`3943f28`**（review round 1 修复后、round 4 之前）；此后 `a239aeb`/`56b7f48`/`5eefaf0`/`94c661f`/`a8db6eb` 均改过 app 代码（含 a11y），故该 APK **不能**用于 AC-10/MAN-018 验证，MAN-017 余额走查亦为 round-4 前代码的既有证据；如需当前代码的真机证据须重新构建。
- 设备：Pixel 6a / Android 17（`CP2A.260705.006`，`bluejay`，user/release-keys）；**2026-10-21 20:00 +08 键盘收起复走**（`adb` 串口 `10.42.0.139:42779`；10-20 首轮为 `:35343`/`:39871`）；输入方法 = Gboard **Alphabet** 子类型（中文拼音子类型会把 adb 注入的十六进制串组合成汉字，见 §6.5）。

| # | 操作 | 观察（UI dump，2026-10-21 20:00–20:02 +08，键盘收起） | 证据 |
|---|---|---|---|
| 1 | 冷启动 deep-link `nexvault://home`（无地址） | `Chain · Ethereum` / `Watch address` / `Total Balance` / **`Not configured`** / `— (24h)` / Tokens `Not configured` | `shell-home-empty.png` + `dump-home-empty.xml` |
| 2 | 输入 `0xd8dA…6045`，`KEYCODE_BACK` 收起键盘 | **`5.7732 ETH`**；caption **`Ethereum · Block #26240838`**；Tokens 行 `Ξ / Ethereum / ETH / 5.7732`（PNG 可见） | `shell-home-balance-vitalik.png` + `dump-home-balance-vitalik.xml` |
| 3 | 输入 `0x9858…Eda94`，收起键盘 | **`0 ETH`**；`Ethereum · Block #26240840`；Tokens 行 `Ξ / Ethereum / ETH / 0` | `shell-home-balance-anchor1.png` + `dump-home-balance-anchor1.xml` |
| 4 | 点按链 pill → Sepolia（字段为 `0x9858…Eda94`） | pill `Chain · Sepolia`；**`0 ETH`**；**`Sepolia · Block #11962283`**；Tokens 行 `Ξ / Sepolia / ETH / 0` | `shell-home-balance-sepolia.png` + `dump-home-balance-sepolia.xml` |
| 5 | 输入 `0x123` + `ENTER`（N3/N4 回归） | 输入框 **`Invalid address`**；Total Balance **`Not configured`**；Tokens **`Not configured`**（错误输入不冒充已配置） | `shell-home-invalid.png` + `dump-home-invalid.xml` |

持久化 UI dump（review round 2 MINOR-1）：**五份** `evidence/dump-home-{empty,balance-vitalik,balance-anchor1,balance-sepolia,invalid}.xml`；本轮截图**键盘已收起**，Tokens 行在 PNG 与 dump 中**均可见**（10-20 首轮截图被软键盘遮挡的问题已消除）。loading 骨架为瞬时态、em-dash 错误态需断网注入（会切断 adb-over-TCP），两者仍以代码路径 + invalid 态 dump 佐证。

一致性（review N2 复算）：设备显示值 == curl wei 交叉计算值（5.7732）；主网 walk2 transcripts `0x1904a56` = **26,240,727** @19:38:59 → 设备 anchor2 **#26240838**（+111 块 / ~22 min ≈ 5.05 块/分钟 ✓）→ anchor1 **#26240840**（+2 块）；Sepolia 设备 **#11962283** 相对 10-20 19:15 的 `0xb66af3` = 11,954,931 累计 +7,352 块 / ~24.8 h ≈ 4.9 块/分钟 ✓。（10-20 首轮复走数值 26,233,430/433、11,954,933 保留在 §Closure 历史记录中。）

### 6.4b 2026-10-22 当前代码重建复走（审核轮 2 MINOR-6）

- 构件：`apps/mobile/android/app/build/outputs/apk/release/app-release.apk` = **46,778,748 B**（arm64-only；sha256 前缀 `e6160366…`；`BUILD SUCCESSFUL in 1m 27s`，2026-10-22 14:54 +08；`JAVA_HOME=/home/superguo/tools/jdk-17`）；源码 = **`87e6d8e`**（构建时为其工作树，提交含相同 app 代码：R7 web split + 审核轮 1/2 修复），零 Metro。
- 设备：Pixel 6a / Android 17（adb `10.42.0.139:42409`）；输入法 = Gboard **English** 子类型（本轮中途一度复现中文拼音组合乱码，切回 English 后逐字正确）。

| # | 操作 | 观察（UI dump，2026-10-22 14:55–15:00 +08，键盘收起） | 证据 |
|---|---|---|---|
| 1 | 冷启动 → Shell preview（无地址） | `Chain · Ethereum` / `Total Balance` / **`Not configured`**（无假数据） | `shell-home-empty.png` + `dump-home-empty.xml` |
| 2 | 输入 `0x9858…Eda94` + ENTER | **`0 ETH`**；`Ethereum · Block #26246506` | `shell-home-balance-anchor1.png` + `dump-home-balance-anchor1.xml` |
| 3 | 输入 `0xd8dA…6045` + ENTER | **`5.7732 ETH`**；`Ethereum · Block #26246508`；Tokens 行 `ETH / 5.7732` | `shell-home-balance-anchor2.png` + `dump-home-balance-anchor2.xml` |
| 4 | 点按链 pill → Sepolia（字段仍 `0xd8dA…`） | `Chain · Sepolia`；**`61.595 ETH`**；`Sepolia · Block #11967905` | `shell-home-balance-sepolia.png` + `dump-home-balance-sepolia.xml` |
| 5 | 清空字段 + ENTER | **`Not configured`**（三处），无残留余额 | `shell-home-cleared.png` + `dump-home-cleared.xml` |
| 6 | 输入 `0x123` + ENTER | 输入框 **`Invalid address`**；Total Balance / Tokens **`Not configured`** | `shell-home-invalid.png` + `dump-home-invalid.xml` |

上一版 §6.4 表（2026-10-21，源码 `3943f28`）保留为历史证据；本表为审核 HEAD 的当前代码复走。
### 6.5 设备输入方法注记（复走期间）

复走首次尝试 `adb shell input text` 产生汉字乱码（Gboard 简体拼音子类型把十六进制串组合为中文，如 `0x`→`0在`）；切换 Gboard 子类型为 **Alphabet** 后同一命令逐字正确（`FIELD=0x9858EfFD232B4033E47d90003D41EC34EcaEda94` 实测）。属设备侧输入法状态，非应用缺陷；§6.4 证据均来自 Alphabet 子类型会话。

## Task 7 — MAN-017 走查（review round 1 M1 更正）

- **状态（历史）：☐ 待走查**——当时（review round 1 M1）MAN-017 未完成，此前「☑ 通过（agent 走查）」作废。**最终状态：☑ 通过（2026-10-21，见下方『走查完成』）**；本条保留时间顺序。
- **已完成的自动侧**：RPC curl 转录（drpc/publicnode）+ `expo serve` 六路由 200。
- **已完成的 Android 真机侧（agent）**：review 修复后重构建 APK 的**五态**截图 + **五份持久化 UI dump**（`evidence/dump-home-{empty,balance-vitalik,balance-anchor1,balance-sepolia,invalid}.xml`；§6.4，2026-10-20 19:14–19:16 +08 复走 + 2026-10-21 20:00 键盘收起复走，含 N3/N4 错误态回归与 Tokens 行文本——review MINOR-1 / round 3 NIT-3 计数对齐）。
- **手工侧（已完成 2026-10-21）**：**Web 桌面浏览器**走查两锚点 + 链切换 + 空态（真实 Chrome/155 macOS via CDP；身份与截图证据见下）。
- 同步更正：`requirements.md` AC-7 的验证方式列——**2026-10-22 最终同步为「MAN-017 通过」**（review round 4 MINOR-2 指出此前只写了分列、未回填通过状态）。
- **owner 走查邀请与 deferral（2026-10-21 19:38–19:49 +08）**：按已批准 continuation plan 起 `npx expo serve --port 8080`（dist = post-fix 导出副本；`/`、`/home`、`/history`、`/settings`、`/receive`、`/send` 全 200；bundle `entry-fd1422cf…js` 1,972,693 B），并采集**同轮次**交叉核对 transcripts：`evidence/curl-mainnet-walk2-{drpc,publicnode}.json`（2026-10-21T11:38:59Z；block `0x19066d7` = **26,240,727**；anchor1 `0x0`；anchor2 `0x501eb617f2372aeb` = `5,773,251,986,299,759,339` wei → 显示 **5.7732**）。已向 owner 发起 10 分钟走查确认窗口，**未收到结论** → 按 plan 记录 **deferral**：MAN-017 保持 ☐、H9 保持 open；服务可由 `cd apps/mobile && npx expo serve --port 8080` 复起，owner 完成后回填。**未以任何形式把走查记为通过。**
- **走查完成（2026-10-21 19:59 +08 首跑；20:26 +08 复跑并持久化浏览器身份；owner 指示 agent 代为执行）**：真实桌面浏览器 **Chrome/155.0.8059.85（macOS；UA 含 `Macintosh; Intel Mac OS X 10_15_7`）** 经 **CDP :9222** + 8080 端口转发驱动（真实渲染，非模拟器）。**身份证据**：`evidence/web-walk-browser.txt`——`Browser.getVersion`（product/UA/protocolVersion 1.3）、`navigator.userAgent/platform/language`、viewport **1280×900** @dpr 1、timezone Asia/Hong_Kong、CDP target id（review round 3 MINOR-1）。五步全 PASS：

| 步骤 | 观察（body text） | 证据 |
|---|---|---|
| 空态（无地址） | `Not configured` + `— (24h)`，无 ETH | `web-walk-empty.png` |
| 锚点1 `0x9858…Eda94` | **0 ETH** · `Ethereum · Block #26240962` | `web-walk-anchor1.png` |
| 锚点2 `0xd8dA…6045` | **5.7732 ETH** · `Ethereum · Block #26240962`（curl 交叉值 5.7732 ✓） | `web-walk-anchor2.png` |
| 链 pill → Sepolia（字段内仍为 `0xd8dA…`） | `Chain · Sepolia` · **61.095 ETH** · `Sepolia · Block #11962401` | `web-walk-sepolia.png` |
| 清空字段 | `Not configured`（无假数据） | `web-walk-cleared.png` |

  **2026-10-22 14:36 +08（R7 web split 后）复跑**：五步全 PASS——anchor1/anchor2 `Ethereum · Block #26246406`（0 ETH / **5.7732 ETH**）、Sepolia `61.595 ETH` @ `#11967802`（字段内仍为 `0xd8dA…`）、清空 `Not configured`；身份仍为 Chrome/155.0.8059.85，view 1280×900。**附加：AC-6 RPC error 态**（同轮 CDP `Network.setBlockedURLs` 拦截 `*drpc.org*`/`*publicnode.com*`）→ UI 显示 **`RPC unavailable` + `Retry`**、Tokens `—`，证据 `evidence/web-walk-rpc-error.{png,txt}`（2026-10-22T06:33:30Z）。

  转录：`evidence/web-walk-transcript.txt`（头含 browser/UA/viewport；五步全文 + 时间戳）。上表数值为 **20:26 复跑**：`#26,240,962` / Sepolia `#11,962,401`，相对 19:38 基线 26,240,727 为 +235 块 / ~48 分钟 ≈ 4.9 块/分钟 ✓。**MAN-017 判定：通过**；**H9 关闭**。（注：Sepolia 步骤余额属于当时仍在输入框内的 `0xd8dA…6045` → `61.095 ETH`，如实记录。）

## Task 8 — 收尾

## Review round 3（2026-10-21，独立评审 → 修复）

| # | 评审发现 | 处置 | 证据 |
|---|---|---|---|
| **MINOR-1** | MAN-017 的 owner 委托与浏览器身份仅口头断言，未持久化（transcript 头只写 “real Chrome via CDP”） | 复跑走查并**持久化身份**：`evidence/web-walk-browser.txt`（`Browser.getVersion` product/UA/protocol、`navigator.userAgent/platform/language`、viewport 1280×900 @dpr 1、tz、target id）；transcript 头内嵌 browser/UA/viewport；五步复跑全 PASS（§Task 7 表已更新） | `evidence/web-walk-browser.txt`；`web-walk-transcript.txt`；§Task 7 |
| **NIT-1** | M2 证明沿用了「`expo export` 无 source-map 旗标」的错误理由 | 更正：`expo export` 有 `-s, --source-maps external`；**已产出生产 source map**（`/tmp/t22-web-map`，exit 0）并验证 `index.web.ts` 在、`index.ts` 不在；`m2-web-barrel-proof.txt` 重写 | `evidence/m2-web-barrel-proof.txt`；§5.1 #6 |
| **NIT-2** | O-7 的「展示余额不可能对应不同地址」过于绝对 | 措辞更正：提交时刻语义正确；**编辑过程中**非法片段存在时，余额可短时显示上一个已提交地址直至 blur/submit | O-7 |
| **NIT-3** | UI dump 计数不一致（四处写四份，另一处与 MAN 行写五份） | 统一为**五份**（`dump-home-{empty,balance-vitalik,balance-anchor1,balance-sepolia,invalid}.xml`） | §Task 7；§6.4 |
| **NIT-4** | 分支/记录不一致：validation 写「本地 ref 未推送」，且分支内嵌记录仍称 MAN-017 通过/T2.2 已关闭 | 分支已推送 origin（`d3f773f`）+ 注释 tag `T2.2-pre-review`；记录标注分支为 **pre-review snapshot**，以 `main` 为准 | §Status correction |
| **NIT-5** | entry 文件名 hash 不可复现（`entry-fd1422…` vs clean rebuild `entry-160b…`，同体积 1,972,693 B） | 记录改为**以体积为准**并注明 filename hash 是构建快照（差异限于 Expo Router NativeTabs CSS 类图键序）；§5.1/§6.3/§Task 5 同步 | §5.1 #3/#6；§6.3；§Task 5 #3 |

修复后复跑：走查五步 PASS（20:26 +08）、生产 source map 导出 exit 0、按体积核对 entry 1,972,693 B。修复提交：**`e51691e`**（main）。
## Review round 4（2026-10-22，独立评审 subagent（review-fix-loop 轮 1）→ 修复）

| # | 评审发现 | 处置 | 证据 |
|---|---|---|---|
| **MAJOR-1** | adapter 无可执行测试（仅 typecheck），AC-2/AC-5 却绑定 TC | **owner 裁定「Add Jest tests now」**：安装已核准的 jest 29.7 + jest-expo 57.0.6（+ 工具链成员 `@jest/globals`；lockfile **+215** packages（647→862）——review-fix 循环轮 2 更正了「零新解析」的错误表述，轮 3 更正了 delta 数字），新增 `apps/mobile/src/web3-adapter.test.ts`（未知链/格式化/地址/注册表 10/10），`test` script + AC-2/AC-5/AC-9 与 H6 同步 | `apps/mobile/jest.config.js`；`apps/mobile/src/web3-adapter.test.ts`；`pnpm --filter mobile exec jest` 10/10 |
| **MINOR-2** | AC-7/§Task 7 仍写 ☐ 待走查，与 MAN-017 ☑ 矛盾 | AC-7 最终同步为「MAN-017 通过（真实 Chrome via CDP）」；§Task 7 历史条目改为「状态（历史）」+「手工侧（已完成）」 | requirements AC-7；§Task 7 |
| **MINOR-3** | plan.md 仍沿用 `main: src/index.ts`、bigint 理由、死端点 | plan 顶部新增 **Post-review divergences** 块（逐项指向 FR-2/FR-5、spec §2.2、validation M2/O-3/O-6），并就地修正 4 处 | plan.md |
| **MINOR-4** | `retry`/`refetchInterval` 是全局默认，所有查询都轮询 | 移至 balance `useQuery`；全局默认仅保留 `staleTime` + `structuralSharing` | `_layout.tsx`；`home.tsx` |
| **MINOR-5** | `setSelectedChain` 接受任意 number，未知链会让 Home 渲染抛错 | store 校验 `SUPPORTED_CHAINS`，忽略未注册 id | `store/observer.ts` |
| **MINOR-6** | index.ts / index.web.ts 双 barrel 复制导出，可漂移 | 抽出 `src/public-api.ts`，两 barrel `export *`，平台不再可漂移 | `packages/web3-adapter/src/{public-api,index,index.web}.ts` |
| **NIT-7** | home.tsx 注释与已更正结论矛盾；多处注释引用 agent skill 而非 spec | 注释改为评审后语义（structuralSharing 防御性、提交时刻保证），引用 `specs/web3-adapter.md` | `home.tsx`；`balance.ts`/`chains.ts`/`errors.ts`/`index.ts` |
| **NIT-8** | 地址输入无 a11yLabel、错误无 live region、autoCorrect/spellCheck/maxLength 缺失 | TextField 增 `accessibilityLabel/Hint`、`autoCorrect/spellCheck/maxLength` 透传；错误 `accessibilityRole=alert` + `accessibilityLiveRegion=polite`；home 传入 42 上限等 | `NexVaultTextField.tsx`；`home.tsx` |
| **NIT-9** | `formatNativeBalance` 把 1–9999 wei 显示为 `0` | **owner 裁定**：非零低于精度 → `<0.0001`；AC-12/TC-W3A-013 更新；jest 覆盖 1/9999/阈值边界 | `balance.ts`；spec AC-12/TC-W3A-013；test |
| **NIT-10** | AGENTS「手工验证」cell 仍写 MAN-001…011 | 更新为 MAN-001…017，列 MAN-011/014/015/016/017 通过日期与证据 | AGENTS.md |

复跑：tsc 双形态 0、jest 10/10、export 0（15 路由，entry 1,973,116 B）、R1/R5/R7 exit 1、web smoke 六路由 200。修复提交：**`a239aeb`**（main）。
## Review round 5（2026-10-22，review-fix 循环轮 2 → 修复）

| # | 评审发现 | 处置 | 证据 |
|---|---|---|---|
| **MAJOR-1** | R4 记录不实：`@jest/globals` 无 dependencies 行、`doc/dependencies.md` 未同步、lockfile delta 不可复现、validation 无 install/policy/audit，且「零新包/零新解析」表述错误 | ① `doc/dependencies.md` 新增 @jest/globals 行并更新 jest 行（版本/位置 apps/mobile/audit/审批）；② tech-stack 拆分为 typescript / jest 工具链 / eslint（未安装）三行；③ validation Task 1 记录 install/policy/audit 实测；④ requirements §R4 改为「原五行零修订 + 2026-10-22 owner 批准 jest 工具链」；⑤「零新解析」表述已在 §Review round 4 更正；⑥ **review round 6 再更正**：lockfile `packages` 647→862（+215），此前 1,032→1,384 无复现命令；sprintf-js@1.0.3 为 **jest 新引入**（dev-only，无补丁），非「既有」 | `specs/tech-stack.md`；`doc/dependencies.md`；validation §Task 1 / §Review round 4/6 |
| **MINOR-1** | FR-2/FR-5、validation Task 2/3、spec §2.5、plan Task 3 仍是旧形态（全局 retry/refetch、双 barrel） | FR-2 增 `public-api.ts`；FR-5 改为仅 staleTime+structuralSharing（retry/refetch 在 home query）；Task 2/3 与 spec §2.5 同步单 barrel；plan Task 3 增 public-api + tests | requirements；validation §Task 2/3；`specs/web3-adapter.md` §2.5；plan Task 3 |
| **MINOR-2** | jest testMatch 只含 `*.test.ts`，未来 tsx/__tests__ 会被静默跳过 | 扩为 `**/*.test.{ts,tsx}` + `**/__tests__/**/*.{ts,tsx}` | `apps/mobile/jest.config.js` |
| **MINOR-3** | AGENTS 仍写完成条件 = review round 3，且缺 round 4 记录 | 改为「独立评审 review-fix 循环零发现轮」；补 round 4 修复 `a239aeb` 记录 | AGENTS Part A 两处 |
| **MINOR-4** | `setSelectedChain` 守卫无测试 | 新增 `apps/mobile/src/store/observer.test.ts`（未知 id 忽略 / 所有注册 id 生效 / 单地址与清空） | 该测试；jest 13/13 |
| **MINOR-5** | 注册表测试标题称「independent」但只断言数量 | 断言 hostname 唯一 + 至少两个 operator domain | `web3-adapter.test.ts` |
| **MINOR-6** | `maxLength={42}` 会在 trim 前截断带前导空格粘贴的合法地址 | 移除 maxLength（保留 trim 后校验），TextField 仍支持该 prop | `home.tsx` |
| **NIT-1** | `accessibilityLiveRegion` 仅 Android，iOS 可能不播报 | error 变化时 `AccessibilityInfo.announceForAccessibility(error)` | `home.tsx` |
| **NIT-2** | AGENTS 手工 cell 中 MAN-011 重复且「1 条」自相矛盾 | 重写为单行状态：MAN-011/014/015/016/017 通过 + MAN-009 未完成 | AGENTS Part A 手工验证 |
| **NIT-3** | 「仍prescribe」拼写、Task 2/3 陈旧 | 改为「仍沿用」；Task 2/3 已同步（见 MINOR-1） | validation |

复跑：tsc 双形态 0、jest **13/13**、export 0（15 路由，entry 1,973,195 B）、R1/R5/R7 exit 1。修复提交：**`56b7f48`**（main）。
## Review round 6（2026-10-22，review-fix 循环轮 3 → 修复）

| # | 评审发现 | 处置 | 证据 |
|---|---|---|---|
| **MAJOR-1** | R4 audit 记录失实：sprintf-js 由 jest 新引入（dev-only，无补丁）、braces 经 jest 可达、advisory 名写作 PostCSS | 重新记录实测：`sprintf-js@1.0.3` 路径（`pnpm why sprintf-js`）、braces 路径、`postcss-selector-parser` 正名；**处置 = 接受 dev-only 残余风险**（依据 owner 2026-10-22 批准 jest 工具链；显式确认 ask 超时未答，标记备查；revert 路径保留） | doc/dependencies.md jest 行；validation §Task 1 |
| **MINOR-2** | lockfile delta 1,032→1,384 不可复现 | 改为 **647 → 862（+215）** + 复现命令 + `pnpm audit --json` totalDependencies=862；旧数字撤回 | §Task 1 |
| **MINOR-3** | iOS announce 未按平台门控，Android 可能重复播报 | 改为 `Platform.OS === 'ios'` 才 announce；Android 走既有 live region | `home.tsx` |
| **MINOR-4** | 注释把 round-5 findings 标为「review round 2」 | 统一为「review-fix loop 2 / review round 5」；validation 的轮次引用同步 | `home.tsx`、`jest.config.js`、两测试文件、validation |
| **MINOR-5** | AC-9 与 Task-5 门禁仍写 10/10 | 更新为 **2 suites / 13 tests**（AC-9、Task 5、§5.1） | requirements AC-9；validation |
| **MINOR-6** | closure 条件仍写「review round 3 通过」 | 两处改为「独立评审 review-fix 循环零发现轮」并标注 2026-10-21 原文已过时 | validation §Status correction / §Closure |
| **MINOR-7** | `@jest/globals` 文档写「精确」但 manifest 是 `^29.7.0` | 两者都钉 **29.7.0**（jest 同）；pnpm install up to date；文档与 manifest 一致 | package.json；doc/dependencies.md |
| **MINOR-8** | tech-stack 行把 eslint-config-expo 的位置一并改错；该包未安装 | tech-stack 拆三行：typescript / jest 工具链 / **eslint-config-expo（未安装，B.6-2 待接线）**；doc/dependencies.md 同步 | specs/tech-stack.md；doc/dependencies.md |

复跑：tsc 双形态 0、jest **13/13**、`pnpm install` up to date、export 0（15 路由，entry **1,973,171 B**）、R1/R5/R7 exit 1。修复提交：**`5eefaf0`**（main）。
## Review round 7（2026-10-22，review-fix 循环轮 4 → 修复）

| # | 评审发现 | 处置 | 证据 |
|---|---|---|---|
| **MINOR-1** | iOS-only announce 可能让 Android 在 live region 未触发时**零播报**（live region 在条件挂载节点上） | 改为 **iOS+Android 单一显式播报**并**移除** error Text 的 live region（消除双播报/零播报两个路径）；设备级验证登记为 **MAN-018（☐ 待走查）** | `home.tsx`；`NexVaultTextField.tsx`；`pending-manual-test-cases.md` |
| **MINOR-2** | 3 个 store 测试被误记为 round 6 新增（实为 round 5 `56b7f48`；round 6 仅改注释） | 门禁表改为「round 4 adapter 10；round 5 +store 3 = 13；round 6 不变」 | validation §Task 5 / §5.1 |
| **MINOR-3** | `pnpm audit` 命令注释仍写「pre-existing toolchain」，与 round 6 更正矛盾 | 改为「1 NEW dev-only（sprintf-js via jest）+ 5 pre-existing」 | validation §Task 1 |
| **NIT-4** | 注释「Pressable event handlers keep this effect inert on web」不实 | 改写为「显式播报在 iOS/Android 生效；web 为 no-op」 | `home.tsx` |

复跑：tsc 双形态 0、jest **13/13**、export 0（15 路由，entry **1,973,162 B**）、R1/R5/R7 exit 1。修复提交：**`94c661f`**（main）。
## Review round 8（2026-10-22，review-fix 循环轮 5 → 修复）

| # | 评审发现 | 处置 | 证据 |
|---|---|---|---|
| **MAJOR-1** | MAN-018 无 spec TC（关联写 review finding + 无关 AC-8） | 新增 `specs/ui-wallet.md` **AC-10 + TC-UI-011**（错误播报恰好一次）；MAN-018 关联改为 TC-UI-011；T2.2 requirements 增 **AC-10**（组件实现 + MAN-018/H10） | `specs/ui-wallet.md` §4/§5；`pending-manual-test-cases.md`；requirements |
| **MINOR-2** | MAN-018 未入 Handoff，且 iPhone 依赖未披露 | 新增 **H10**（Android Pixel 6a 可立即执行；iOS 待合格 iPhone，MAN-013 记录）；MAN-018 状态注明 | validation Handoff；registry |
| **MINOR-3** | announce 只在 Home，共享组件契约回归 | 播报移入 **`NexVaultTextField`**（`error` 变化时）；Home 删除 effect 与 AccessibilityInfo import | `NexVaultTextField.tsx`；`home.tsx` |
| **MINOR-4** | 证据不一致：交叉引用错、§5.1 历史表被回填 round-7 数值、体积稳定断言不可复现 | 交叉引用改为 round 6 MINOR-2/round 8；§5.1 恢复该轮 1,972,693 B；撤回「体积相同」断言，改记 **hash+bytes**（clean rebuild 1,973,512 B） | validation §Task 5 / §5.1 |
| **NIT-5** | MAN-018 插在 MAN-017 前、表格被空行切断 | MAN-018 移到 MAN-017 之后；清除该段空行，表格连续 | registry |

复跑：tsc 双形态 0、jest **13/13**、export 0（15 路由；entry **1,973,161 B** / `entry-9ea97df9a1a269a8c5239ee55d900b4b.js`）、R1/R5/R7 exit 1。修复提交：**`a8db6eb`**（main）。
## Review round 9（2026-10-22，review-fix 循环轮 6 → 修复）

| # | 评审发现 | 处置 | 证据 |
|---|---|---|---|
| **MAJOR-1** | AC-10 唯一测试 MAN-018 仍待走查，零发现轮会带着未验证 AC 关闭 | 明确 **AC-10 不纳入 T2.2 关闭门禁**（a11y 增强项，H10 跟踪；关闭以 AC-1..9 为准）；H10 注明需含 `a8db6eb` 的当前代码 APK（旧 `3943f28` APK 不含 announce） | requirements AC-10；validation H10 |
| **MINOR-2** | Task 6.4「代码自构建后未变」不实 | 记录 APK 源码修订 **`3943f28`**、证据范围（MAN-017 走查为 round 4 前代码）；a11y 验证需重建 | validation §Task 6.4 |
| **MINOR-3** | 交叉引用错（round 6 MINOR-2 实为 lockfile delta） | 改为 **round 7 MINOR-2**（归属）+ **round 6 MINOR-5**（计数）+ round 8 MINOR-4 | §Task 5 |
| **MINOR-4** | FR-7/AC-10 未含 a11y 交付 | FR-7 增 ui-wallet **AC-10/TC-UI-011/MAN-018**；AC-10 改为 outcome 口径（恰好一次） | requirements |
| **MINOR-5** | round-8 export 数值不可复现 | 以 **canonical clean rebuild**（HEAD `22243b9`，`--clear`）为准：`entry-8182b0df…js` = **1,973,511 B**；旧 1,973,16x 标注为增量构建快照 | §Task 5 |
| **NIT-6** | 「alert 不被朗读」表述不精确 | 改为「不自动播报（Android roleDescription / iOS UIAccessibilityTraitNone）」 | `NexVaultTextField.tsx` |
| **NIT-7** | AC-10 「dev-only」混淆构建类型与测试范围 | 改为「MAN-018 待真机（Android 可执行；iOS H10）」 | requirements |
| **NIT-8** | H10 行无行尾换行 | 已补 | validation |

复跑：tsc 双形态 0、jest **13/13**、export 0（15 路由；canonical 见 §Task 5）、R1/R5/R7 exit 1。修复提交：**`6a4b6e7`**（main）。
## Review round 10（2026-10-22，review-fix 循环轮 7 / 持久 reviewer 轮 1 → 修复）

| # | 评审发现 | 处置 | 证据 |
|---|---|---|---|
| **MAJOR-1** | web bundle 仍含 viem 签名/派生图（`@noble/curves`、`@scure`、wallet actions）→ 违反 AC-6/R7 | **owner 裁定方案 a（web split）**：新增 `chains.base.ts` / `chains.web.ts` / `balance.web.ts`（fetch JSON-RPC + 顺序 failover）/ `format.ts` / `address.web.ts`；原生仍 viem。web 全矩阵 **0**、bundle **−641,809 B**；新增 8 例 web 测试 | §Task 5 行 3/5b；`apps/mobile/src/web3-adapter-web.test.ts` |
| **MAJOR-2** | AC-1/AC-3 门禁缺命令原文与退出码 | §Task 2 增命令块（`head -1 _layout`、`head -2 polyfills`、`grep '{ rank: false }'`），§Task 5 增行 2c | §Task 2 / §Task 5 |
| **MINOR-3** | R4 记录称 `@noble`/`@scure` 「仅 devDependency」、viem 位置错 | tech-stack 行 + doc/dependencies viem/@noble 行更正：viem 传递生产副本（curves 1.9.1 等，native）；web chunk 经 split 后为 0 | `specs/tech-stack.md`；`doc/dependencies.md` |
| **MINOR-4** | FR-7/plan 仍写 AC-9..AC-11 | 改为 **AC-9..AC-12** | requirements FR-7；plan Task 3 |
| **MINOR-5** | spec §2.1 仍写 `apps/mobile/polyfills.ts` + entry 前 import | §2.1 改为 `apps/mobile/src/polyfills.ts` + 根 `_layout` 第一条 import；AC-1 行同步 | `specs/web3-adapter.md` §2.1/AC-1 |
| **MINOR-6** | AC-6 RPC-error 态无 UI 证据 | CDP `Network.setBlockedURLs`（`*drpc.org*`/`*publicnode.com*`）实测：`RPC unavailable` + `Retry`、Tokens `—`；证据 `evidence/web-walk-rpc-error.{png,txt}` | §Task 7；AC-6 |
| **MINOR-7** | AGENTS 轮次标注混乱 + 阶段证据缺 T2.2 | 统一为「review 轮 N = 循环轮 N−3」；阶段证据补 T2.2 validation 路径 | AGENTS Part A |
| **NIT-8** | Home 提交逻辑两处重复 | 抽出 `commitAddress(value, showError)`，typing 与 blur/submit 共用 | `home.tsx` |
| **NIT-9** | store 无地址不变量 | `setWatchedAddress` 归一化后写入、非法忽略；store 测试 +1（共 4） | `observer.ts`；`observer.test.ts` |
| **NIT-10** | chain pill / Retry 触控目标 < 48dp | 两处加 `hitSlop={tokens.spacing.sm}` | `home.tsx` |

复跑：tsc 双形态 0（**更正：该提交的形态 2 实为红——adapter 缺 DOM lib，见 §Review round 11 BLOCKER-1**）、jest **3 suites / 22 tests**、export 0（**15 路由**；`entry-456faa1f…js` **1,331,702 B**）、全图矩阵 0、web smoke 六路由 200、五态 + RPC-error 态 CDP 走查 PASS、R1/R5/R7 exit 1。修复提交：**`6563ff6`**（main）。
## Review round 11（2026-10-22，review-fix 循环轮 8 / 持久 reviewer 轮 2 → 修复）

| # | 评审发现 | 处置 | 证据 |
|---|---|---|---|
| **BLOCKER-1** | `pnpm -r --if-present run typecheck` 红：adapter tsconfig 无 DOM lib，`fetch` 未声明；而 validation 记为 0 | `packages/web3-adapter/tsconfig.json` lib 加 **DOM**；双形态复跑均 0；§Task 5 行 2 更正并记录历史失败 | §Task 5 行 2 |
| **MINOR-2** | web fetch 无超时，stalled RPC 会让查询永远 loading | `AbortSignal.timeout(10s)`（可覆盖 `timeoutMs`）+ 新增 stall 测试 | `balance.web.ts`；web test |
| **MINOR-3** | web 无 EIP-55（混合大小写 checksum 错误也接受），与原生/文档不一致 | 在 spec §2.5 明确平台分歧（原生 viem 含 checksum；web 正则+lowercase）、原生 docstring 注明；新增 checksum-invalid 用例 | `specs/web3-adapter.md` §2.5；`address.ts`；web test |
| **MINOR-4** | FR-1 仍写旧路径与「冲突」 | 重写为「`src/app/_layout.tsx` 第一条 import」+ 无遗留冲突 | requirements FR-1 |
| **MINOR-5** | R4 记录仍称 `@noble 2.4.x` 直装 `packages/vault-core`（该路径不存在） | 删除该虚构 dev 依赖/位置；保留 viem 传递生产版本（curves 1.9.1 等），注明 2.4.x 仅 `/tmp` 一次性核验 | tech-stack；doc/dependencies |
| **MINOR-6** | Android 证据仍是 `3943f28` 旧构建 | **当前代码重建 release APK**（46,778,748 B）并复走五态 + invalid 态，记录于 §6.4b | §6.4b；`evidence/shell-home-*.png` / `dump-home-*.xml` |
| **NIT-7** | web 测试绕过 `index.web` 映射 | 新增静态 `import * as webBarrel from 'web3-adapter/src/index.web'` 表面断言 | web test |

复跑：tsc 双形态 0、jest **3 suites / 25 tests**、export 0（15 路由；`entry-52f35f21…js` **1,331,901 B**）、全矩阵 0、web smoke 六路由 200、Android 五态+invalid 复走通过、R1/R5/R7 exit 1。修复提交：**`87e6d8e`**（main）。
## Review round 12（2026-10-22，review-fix 循环轮 9 / 持久 reviewer 轮 3 → 修复）

| # | 评审发现 | 处置 | 证据 |
|---|---|---|---|
| **MINOR-1** | web 独有第三参导致公开签名平台不一致，`RpcOptions` 未导出/未文档 | 新增平台中立 `types.ts`（`RpcOptions.timeoutMs`），**两平台同签名**（原生映射 `http(timeout)`，Web 映射 `AbortSignal.timeout`），`RpcOptions` 由 barrel 导出；spec §2.5 + FR-3 记录 | `types.ts`；`balance.ts`；`public-api.ts`；spec/FR-3 |
| **MINOR-2** | 门禁行 3/4/5 指向 canon2 而 canonical 实为 canon3，新 hash 无法从记录命令复现 | 重导 **canon4**（`entry-828c830c…js`，1,331,901 B）；行 3/4/5/5b 全部改为 canon4 并标注矩阵来源；§6.3 smoke 同步 | §Task 5；§6.3；`evidence/web-serve-smoke.txt` |
| **MINOR-3** | requirements AC-2/AC-9 仍写 22 tests | 改为 **3 suites / 25 tests**（adapter 10 + web 11 + store 4） | requirements AC-2/AC-9 |
| **MINOR-4** | H10/MAN-017/AGENTS/§6.4b 仍指向旧 APK/旧走查；§6.4b 未记源码修订 | H10 改为「当前代码 APK 已在设备（46,778,748 B，源码 `87e6d8e`）可跑 MAN-018」；MAN-017/AGENTS 补 2026-10-22 复走；§6.4b 记源码 `87e6d8e` + 构建时间/hash | H10；MAN-017；AGENTS；§6.4b |
| **NIT-5** | web barrel 测试只是表面 smoke，不证明平台映射 | 测试内注明「surface smoke；映射门禁 = export matrix」，validation 同步 | web test；§Task 5 |

复跑：tsc 双形态 0、jest **3 suites / 25 tests**、export 0（15 路由；`entry-828c830c…js` **1,331,901 B**）、全矩阵 0、web smoke 六路由 200、R1/R5/R7 exit 1。修复提交：**`ff80939`**（main）。
## Status correction（2026-10-21，owner 裁定 Option B）

- **发现**：T2.2 于 2026-10-20 在评审验收前即按任务行的「close with Part A board line + git per B.3」执行关闭（board 行 + `--no-ff` 合并 `82addd0` + push + 删分支）；同一时点 MAN-017 以 **agent 自走查**记为「通过」，reviewer M1 判为无效并回退（后经真实 Chrome CDP 走查补正并通过）。
- **裁定**：**T2.2 改回「复核中」**——代码与全部评审修复保留在 `main`（不重写历史）；**完成条件 = 独立评审 review-fix 循环的零发现轮**（2026-10-21 原文写「review round 3 通过」，已随 review-fix 循环启动而过时——2026-10-22 更正）；通过后恢复「已关闭」board 行。
- **保留不变**：合并 `82addd0`；round 1–2 修复 `3943f28` / `f198711`；MAN-017 真实通过 `c90bf08`（真实 Chrome 155/macOS via CDP 五态）与键盘收起复走 `0099af9`（Tokens 行 PNG+dump 均可见）。
- **治理教训（本条记录）**：关闭必须晚于评审验收；手工用例的「通过」只能来自真机/真实浏览器走查（B.4-2），不得由 agent 自证替代。
- **分支现状（2026-10-22 owner 指示更新）**：owner 裁定「**修复应在该分支**、**代码评审不得改分支**」——`feature/T2.2-web3-readonly` 已 **fast-forward 到 `main` 尖端**并推送 origin（`git diff feature/T2.2-web3-readonly..main` 为空）；原 pre-review 快照 `d3f773f` 保留在注释 tag **`T2.2-pre-review`**。历史：2026-10-21 曾按 Option A 冻结在 `d3f773f`（当时记录的该分支内嵌 validation/AGENTS 为评审前版本，以 main 为准）。**评审只读纪律**：reviewer 仅读取该分支与提交范围，不得 commit / push / switch / 改写任何 branch ref；发现以文字回报，由主 agent 修复。

## Closure（2026-10-20）

> **2026-10-21 状态更正**：本节降级为「关闭候选记录」——T2.2 现处「复核中」，直至**独立评审 review-fix 循环零发现轮**（原文「review round 3」已过时，2026-10-22 更正；见上节与 AGENTS）。


- **关闭依据**：owner 指令「Plan + implement T2.2 … close with Part A board line + git per B.3」（2026-10-20）；plan 经 `exit_plan_mode` `approved: true`；Q/A 推荐答案随批准生效（无 override）。
- **关闭门禁复跑（2026-10-20 15:0x 首轮 + 19:0x review 修复后复跑）**：`pnpm --filter mobile exec tsc --noEmit` exit 0；`pnpm -r --if-present run typecheck` exit 0；`npx expo export --platform web --output-dir /tmp/t22-web` exit 0（**15 静态路由**；终态 entry `entry-fd1422cf11f7ad4bf46cc42d28d8e2b4.js` = **1,972,693 B**）；R1 / R5 / R7 grep exit 1（零命中）；`expo-doctor` **21/21**。
- **balance walk（AC-7）**：review 修复后重构建 release APK `apps/mobile/android/app/build/outputs/apk/release/app-release.apk` = **46,756,998 B**（内置 bundle 4,129,056 B；arm64；`BUILD SUCCESSFUL in 1m 37s`；零 Metro）——复走 19:14–19:16 +08：anchor2 **`5.7732 ETH`** / **`Ethereum · Block #26233430`**；anchor1 **`0 ETH`** / `#26233433`；Sepolia **`Block #11954933`**；错误输入态 **`Invalid address` + `Not configured`**；Web 侧同轮次 curl 转录（drpc/publicnode `0x1904a56` = 26,233,430）+ `expo serve` 六路由 200。**MAN-017 通过（2026-10-21，真实 Chrome 155/macOS 经 CDP 走查五态；H9 关闭）**。
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

修复后复跑：tsc 双形态 0 / export 0（15 路由，`entry-fd1422cf…` 1,972,693 B）/ R1·R5·R7 exit 1 / doctor 21/21 / Android 真机复走五态通过（§6.4）。修复提交：**`3943f28`**（main）。
## Review round 2（2026-10-21，独立评审 → 修复）

| # | 评审发现 | 处置 | 证据 |
|---|---|---|---|
| **MINOR-1** | post-fix Tokens 行的 dump 未持久化（截图被软键盘遮挡）；MAN 行写「四截图 + UI dump」与实际（五 PNG、无 dump）不符 | 四份 UI dump 持久化到 `evidence/dump-home-*.xml`（含 Tokens 行文本与 balance/block）；截图软键盘遮挡说明入 §6.4；MAN-017 行措辞更正为「五张截图 + 四份 UI dump」；loading 骨架 / em-dash 错误态标注为代码路径 + invalid dump 佐证 | `evidence/dump-home-*.xml`；§6.4；MAN-017 |
| **NIT-1** | requirements 未随 O-6 同步（FR-2 main/types；FR-5 bigint 理由；AC-4 过宽措辞） | 三处更正：FR-2 `main = src/index` + `types = src/index.ts`；FR-5 改为防御性理由（replaceEqualDeep，不 JSON 序列化）；AC-4 收窄为「无 vault-core / 密钥材料导出」 | `requirements.md:49/52/68` |
| **NIT-2** | 「MAN-017 未通过」可被读成失败 | 改为「MAN-017 未完成」 | §Task 7 |
| **NIT-3** | Web route-smoke 证据引用修复前 bundle（`entry-a8f1…` / 1,971,473 B / 06:52Z） | 以 post-3943f28 导出复跑（`entry-fd1422…` / **1,972,693 B** / 六路由 200 / 2026-10-21 14:18 +08），`evidence/web-serve-smoke.txt` 覆盖 | §6.3；`evidence/web-serve-smoke.txt` |

修复提交：**`f198711`**（main）。
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
| O-7 | 地址提交语义与 Tokens 状态：空输入曾报 "Invalid address" 且保留旧地址；loading/error 时 Tokens 恒显示 "Not configured" | 已修（N3/N4）：空输入清空；非法输入清空 + 报错；输入即校验提交；Tokens 四态（无地址=Not configured / loading=骨架 / error=— / data=行）。**措辞更正（review round 3 NIT-2）**：提交时刻保证「展示余额 = 已提交地址」；**编辑过程中**字段持有非法片段时，余额仍可能短时显示上一个已提交地址，直到 blur/submit 清空——非「任何时候都不可能对应不同地址」 |
| O-8 | 包 `main` 带扩展名（`src/index.ts`）使 web 永不解析 `index.web.ts`（M2） | 已修：`main: src/index`；`types: src/index.ts`；dev bundle 模块路径实证（§5.1） |
| O-9 | `formatNativeBalance` 未传链 decimals（两链均 18，暂无可见缺陷） | 已修（N5）：两处调用传入 `chain.nativeCurrency.decimals` |
| O-10 | 显示截断把非零小额渲染成 `0`（1–9999 wei） | 2026-10-22 owner 裁定 + review NIT-9：非零且低于精度显示 `<0.0001`；AC-12/TC-W3A-013 与 jest 用例同步 |

## Handoff 清单（T2.2 → T3.x / T4.x）

| # | 事项 | 递延理由 | 建议处置 | 关联 AC/TC |
|---|---|---|---|---|
| H1 | 交易/签名工作流（`prepareTransaction`/`signPrepared`/`broadcastSigned`） | T2.2 只读范围 | implement @ T4.1 | TC-W3A-002..004 |
| H2 | `readVerified` 主备交叉验证 + EIP-155 断言（`E_W3A_RPC_DIVERGENCE` 等） | 同上 | T4.1 | TC-W3A-005 |
| H3 | 链 failover live-fire 演练（可编程故障注入） | 需故障注入载具 | T4.x | TC-W3A-011 |
| H4 | watch 地址持久化（SecureStore/AsyncStorage） | 新依赖需 R4 提案 | 未来条目 | Q4 / AC-6 |
| H5 | 行情/图表数据（Total Balance 24h、chart、区间切换） | 非 T2.2 范围 | T4.x | Q2 |
| H6 | ~~adapter jest 单测接线~~ **已完成（2026-10-22，review round 4 MAJOR-1）** | — | **关闭**：jest-expo 57.0.6 + jest 29.7，`apps/mobile/src/web3-adapter.test.ts` 10/10（未知链/格式化/地址帮助函数/注册表） | B.6-3 / TC-W3A-009/010/013 |
| H7 | WalletConnect(Reown) 桥接 | v2 计划 | v2 | spec §7 |
| H8 | T2.1a H2/H3/H5/H6（filled 图标 / web glyph / 18 屏 / 地址等宽+渐变） | B.7-2 replan | T4.x（H3/H6 各自 R4 提案） | 见 requirements §Handoff |
| H9 | ~~MAN-017 的 Web 浏览器走查~~ **已完成（2026-10-21）** | — | **关闭**：真实 Chrome 155/macOS 经 CDP 走查五态通过（§Task 7） | MAN-017 |
| H10 | **MAN-018 无障碍播报的设备验证**（TalkBack/VoiceOver 恰好一次） | 需真机 + 读屏软件人工确认；iOS 侧无合格 iPhone（MAN-013 记录）。**Android 条件已满足**：当前代码 release APK 已在设备上（**46,778,748 B**，源码 `87e6d8e`，§6.4b），可直接跑 MAN-018（旧 `3943f28` 构建不含 announce，已弃用） | Android：Pixel 6a 上完成 MAN-018；iOS：待 iPhone 到位后补（T4.x 前的前置）。**AC-10 不计入 T2.2 关闭门禁** | TC-UI-011 / MAN-018 / AC-10 |
