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

新建：`packages/web3-adapter/package.json`（name `web3-adapter`、private、`main/types = src/index.ts`、dep `viem 2.56.9`、`typecheck` 委派脚本）、`packages/web3-adapter/tsconfig.json`（strict/noEmit/ES2022/bundler/skipLibCheck/types []）。修改：`apps/mobile/package.json`（+6 依赖、+1 `expo.install.exclude`）。

```bash
$ pnpm install
Scope: all 2 workspace projects
✓ Lockfile passes supply-chain policies (628 entries in 3.3s)
Packages: +19
Done in 8.5s                  # exit 0
```

版本探针（`node -p require(...).version`，apps/mobile）：`viem 2.56.9` / `@tanstack/react-query 5.104.2` / `zustand 5.0.15` / `react-native-get-random-values 2.0.0` / `fastestsmallesttextencoderdecoder 1.0.20`。
adapter 链接：`packages/web3-adapter/node_modules/viem -> ../../../node_modules/.pnpm/viem@2.56.9_typescript@6.0.3_zod@3.25.76/node_modules/viem`；lockfile importer `packages/web3-adapter: viem specifier 2.56.9 → 2.56.9(typescript@6.0.3)(zod@3.25.76)`。

新增解析包（viem 传递图，R4 行已核准）：`abitype@1.2.3`、`eventemitter3@5.0.1`、`fast-base64-decode@1.0.0`、`isows@1.0.7`、`ox@0.14.45`、`ws@8.21.0`。

- **Peer 警告（预存在）**：`expo-modules-core@57.0.23` 期望 worklets `^0.7.4…^0.10.0`，实装 `0.13.0`（CVE-2026-97724 下限，tech-stack 已钉）——T2.1a 即存在，非本条目引入。
- **expo-doctor 回归与处置**：首次接入后 doctor exit 1（`react-native-get-random-values` 期望 `~1.11.0`、实装 `2.0.0` = R4 钉版，`2.0 起仅新架构/RN ≥0.81`，本工程 RN 0.86.3）；按文档化机制加入 `expo.install.exclude`（scaffold 配置，非依赖变更）→ 复跑 **21/21 通过**。

## Task 2 — polyfill 契约 + Query Provider

`apps/mobile/src/polyfills.ts`（新建；**两行 import 在文件最前**，注释在后）：
```ts
import 'react-native-get-random-values';
import 'fastestsmallesttextencoderdecoder';
```
`apps/mobile/src/app/_layout.tsx`：第 1 行 `import '../polyfills';`（grep 实测）；模块级 `QueryClient`：`staleTime 30_000` / `retry 2` / `refetchInterval 30_000` / `refetchIntervalInBackground false` / **`structuralSharing false`**（bigint 非 JSON-safe）；`<QueryClientProvider client={queryClient}>` 包住 `<SafeAreaProvider>`（keyed Stack 重挂载不重挂 Provider）。

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
| 3 | `npx expo export --platform web --output-dir /tmp/t22-web` | EXPORT_EXIT=0；**15 静态路由**；entry `entry-a8f1f27a47acfb7cfb92b2d7479b6815.js` = **1,971,473 B** | ✓ |
| 4 | `grep -rE 'signDigest|importMnemonic|vault-core' /tmp/t22-web/_expo/static/js/` | R7_EXIT=1（零命中） | ✓ |
| 5 | `grep -rE 'signPrepared|signAndBroadcast|privateKeyToAccount|mnemonicToAccount' /tmp/t22-web/_expo/static/js/` | SIGN_EXIT=1（零命中） | ✓ |
| 6 | `grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile packages --include='*.ts' --include='*.tsx' --exclude-dir=node_modules \| grep -v 'packages/vault-core'` | R1_EXIT=1（零命中） | ✓ |
| 7 | R5 console grep（宪法原文） | R5_EXIT=1（零命中） | ✓ |
| 8 | `npx expo-doctor`（apps/mobile） | **21/21 checks passed** | ✓ |
| 9 | `git diff --stat`（期望：仅五行 + workspace link + exclude 行） | `apps/mobile/package.json +8/-1`；`pnpm-lock.yaml +221`；新增 `packages/web3-adapter/**` | ✓ |
| 10 | 产物端点断言 | `grep -c eth.drpc.org` = 1；`grep -c eth.llamarpc.com` = 0 | ✓ |

- **H4 implement（T2.1a Handoff）**：`AGENTS.md` B.5 R7 示例命令 `-o dist-web` → `--output-dir dist-web`（行 179），证据用新文本复跑（行 4）。
- **O-4 记录**：web bundle 含 viem root barrel 的 `createWalletClient`（计数 2：导出 + 实现）——Metro 不做 tree-shaking；**账户模块未入包**（`privateKeyToAccount|mnemonicToAccount` 计数 0），R7 宪法 grep 干净。接受（无账户对象的 wallet client 无密钥路径）；若 T4.x 的 AC-6 要求更严，再按子路径导入收敛。

## Task 6 — balance walk（自动化侧）

### 6.1 端点探针与登记表修订（O-3）

`evidence/rpc-endpoint-probe.txt`：`https://eth.llamarpc.com` → **HTTP 525**（不可用）；`https://rpc.sepolia.org` → 空响应；`https://cloudflare-eth.com` → `-32046 Cannot fulfill request`。可用且 CORS 开放：`https://eth.drpc.org`（CORS 回显 Origin）、`https://sepolia.drpc.org`、`https://ethereum-rpc.publicnode.com`、`https://ethereum-sepolia-rpc.publicnode.com`（CORS `*`）。
→ 登记表按「≥2 独立运营商 + CORS 可用」调整为：mainnet = `eth.drpc.org` + `ethereum-rpc.publicnode.com`；sepolia = `sepolia.drpc.org` + `ethereum-sepolia-rpc.publicnode.com`（O-3，公开端点、无 API key、非 R4 变更）。

### 6.2 RPC 转录（Web 侧证据，`evidence/`）

| 文件 | 内容 |
|---|---|
| `curl-mainnet-drpc.json` | `0x9858…Eda94` → `0x0`；`0xd8dA…6045` → `0x501e732698d4efc3`；blockNumber → `0x1904536` |
| `curl-mainnet-publicnode.json` | 同上两地址 → `0x0` / `0x501e732698d4efc3`；blockNumber → `0x1904516` |
| `curl-mainnet-llamarpc.json` | HTTP 525（探针留档，证明替换理由） |

交叉计算（viem `formatUnits`，node @ apps/mobile）：`0x501e732698d4efc3` = `5773178381945597891` wei → `5.773178381945597891` → display **`5.7731`**；`0x0` → **`0`**。

### 6.3 Web 静态导出路由 smoke（`evidence/web-serve-smoke.txt`）

`npx expo serve --port 8080`（dist = /tmp/t22-web 副本）：`/` 200、`/home` 200、`/history` 200、`/settings` 200、`/receive` 200、`/send` 200；bundle `entry-a8f1…js` 200 / 1,971,473 B。

### 6.4 Android 真机 walk（release APK，零 Metro）

- 构件：`/home/superguo/Projects/NexVaultRN/apps/mobile/android/app/build/outputs/apk/release/app-release.apk` = **46,756,030 B**（内置 bundle 4,128,088 B；arm64；`BUILD SUCCESSFUL in 2m 32s`；`JAVA_HOME=/home/superguo/tools/jdk-17`，见 spec dev-environment §1）。
- 设备：Pixel 6a / Android 17（`CP2A.260705.006`，`bluejay`，user/release-keys），`adb devices` 串口 `10.42.0.139:39871`；`adb install -r` → Success。

| # | 操作 | 观察（UI dump） | 证据 |
|---|---|---|---|
| 1 | 冷启动 deep-link `nexvault://home`（无地址） | `Chain · Ethereum` / `Watch address` / `Total Balance` / **`Not configured`** / `— (24h)` / Tokens `Not configured` | `shell-home-empty.png` |
| 2 | 输入 `0xd8dA…6045` + Enter | **`5.7731 ETH`**；caption **`Ethereum · Block #26232113`**；Tokens 行 `Ethereum / ETH / 5.7731` | `shell-home-balance-vitalik.png` |
| 3 | 输入 `0x9858…Eda94` + Enter | **`0 ETH`**；`Ethereum · Block #26232115` | `shell-home-balance-anchor1.png` |
| 4 | 点按链 pill → Sepolia | pill `Chain · Sepolia`；`0 ETH`；**`Sepolia · Block #11953618`**；Tokens 行 `Sepolia / ETH` | `shell-home-balance-sepolia.png` |

一致性：设备显示值 == curl wei 交叉计算值（5.7731）；区块号与 curl 同轮次（`#26,232,086` @14:44 → `#26,232,115` @14:51，主网 ~12s/块，差 27 块 ≈ 5.4 分钟 ✓；Sepolia `#11,953,591` → `#11,953,618` 差 27 块 ✓）。

## Task 7 — MAN-017 走查

MAN-017 **通过（2026-10-20）**：Pixel 6a / Android 17 release APK（零 Metro）+ Web 侧 curl/serve 双侧记录；两锚点均返回真实余额（anchor1 = 0 ETH 属实、anchor2 ≈ 5.77 ETH），区块号 caption 与 RPC 转录一致，链 pill 可切 Sepolia 并重查询，无地址时空态为 `Not configured`（R5）。证据：evidence/ 四张截图 + `curl-*.json` + `web-serve-smoke.txt`。走查由 agent 在实体真机完成（B.4-2 真机记录齐备）；owner 可随时复核。

## Task 8 — 收尾

见 §Closure（合并/推送后回填）。

## 记录偏差与观察

| # | 事项 | 处置 |
|---|---|---|
| D-i | Home 新增 watch 地址输入行（原仓库 Home 无输入） | owner Q1-A 批准；`home.tsx` 注释标注 |
| D-j | 链 pill 由静态占位改为 mainnet↔sepolia 点按切换（原仓库为下拉菜单） | owner Q3-A 批准 |
| O-1 | `doc/dependencies.md` viem 行安装位置 cell 写 `packages/vault-core`，与权威 `specs/tech-stack.md` 不一致 | doc/ 只读（B.1-2）；以 tech-stack 为准，记观察 |
| O-2 | tech-stack typescript 行 stale（`~5.9.x / root` vs 实际 `apps/mobile ~6.0.3`） | 用委派 typecheck 脚本绕开新 importer 条目，不触发 R4 修订 |
| O-3 | RPC 端点替换（llamarpc 525 / sepolia.org 空响应 → drpc；cloudflare-eth 不可用） | 探针证据留档；满足「≥2 独立运营商 + CORS」契约 |
| O-4 | web bundle 含 viem root 的 `createWalletClient`（无账户模块） | 接受；R7 宪法 grep 干净；T4.x AC-6 若有更严要求再收敛 |
| O-5 | polyfills 位置（spec 文本 vs `import '../polyfills'` 相对路径） | 取 `src/polyfills.ts`，契约顺序不变 |

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
