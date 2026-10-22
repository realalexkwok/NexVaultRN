# T2.2 — web3 read-only adapter — plan

> Status: 规划轮批准（2026-10-20，`exit_plan_mode approved: true`）；按 Task 0–8 顺序执行，每 Task 完成即在 validation.md 落命令与观察结果 | Skill: `.agents/skills/nexvault-t2-2-web3-readonly/SKILL.md`（提交 `9118080`）
> 实施纪律：R1/R5/R7/R4（技能 Invariant 1–3）；B.4 双侧证据；B.6-1 tsc 双形态；每次落盘前已有规划轮 Q/A（Q1–Q5，随 plan 批准）。

> **Post-review divergences（2026-10-22，以实现与 validation 为准）**：本计划的早期草案文字已被评审轮修正，实际以以下条目为准——
> ① `package.json main` = **无扩展名 `src/index`**（M2；写成 `src/index.ts` 会让 Metro 绕过 `index.web.ts`）；
> ② 查询默认值仅 `staleTime` + `structuralSharing`，`retry/refetchInterval` 在 balance `useQuery` 上（MINOR-4）；`structuralSharing` 是防御性设置（v5 不 JSON 序列化缓存，N1/O-6）；
> ③ 端点 = **drpc + publicnode**（O-3；llamarpc 已死、sepolia.org 无响应）；
> ④ barrel = `src/public-api.ts` 单一导出面（MINOR-6）；⑤ 新增 jest 单测（MAJOR-1）；⑥ 显示 <0.0001 阈值（NIT-9）。
> 细节指针：requirements FR-2/FR-5/AC-2/AC-5/AC-9；`specs/web3-adapter.md` §2.2/§2.5/附录 AC-12；validation M2/O-3/O-6/O-10 与 §Review round 4。

## Task 0 — 预检、分支与 feature 目录（validation §Task 0）

- 预检（规划轮已做）：R4 五行核验（requirements §R4 预检，零修订）；`pnpm-workspace.yaml` 含 `packages/*`；polyfill 注入点 = `src/app/_layout.tsx`；MAN 下一号 = MAN-017；git main @ `9118080`。
- `git switch -c feature/T2.2-web3-readonly`（B.3-2，基线 = main `9118080`）；`mkdir -p specs/features/2026-10-20-T2.2-web3-readonly/evidence`。
- 落盘：requirements.md（含 Q/A 与 Handoff 裁定）、plan.md（本文件）、validation.md（渐进）。

## Task 1 — 依赖安装 + workspace 接线（R4 零修订；validation §Task 1）

新建 `packages/web3-adapter/package.json`：
```json
{
  "name": "web3-adapter",
  "version": "0.1.0",
  "private": true,
  "main": "src/index",  // M2: extensionless -> Metro applies index.web.ts on web
  "types": "src/index.ts",
  "scripts": {
    "typecheck": "pnpm --filter mobile exec tsc --noEmit -p ../../packages/web3-adapter/tsconfig.json"
  },
  "dependencies": { "viem": "2.56.9" }
}
```
（`typecheck` 委派 mobile 的 tsc 二进制：避免新 importer 条目、零 R4 修订；O-2 见 requirements。）
新建 `packages/web3-adapter/tsconfig.json`：`strict/noEmit/target ES2022/lib [ES2022]/module ESNext/moduleResolution bundler/skipLibCheck/types []/include ["src/**/*.ts"]`。
`apps/mobile/package.json` dependencies 增（精确钉版）：`@tanstack/react-query 5.104.2`、`fastestsmallesttextencoderdecoder 1.0.20`、`react-native-get-random-values 2.0.0`、`viem 2.56.9`、`web3-adapter workspace:*`、`zustand 5.0.15`。
命令：`pnpm install`（`.npmrc` `ignore-scripts=true` 已强制）；lockfile 更新为新增 importer 条目 + 5 个包解析。
验证：lockfile importer 精确；`git diff --stat` 只含预期文件；`node -e require.resolve` 探针（app 侧 `viem` / `web3-adapter` 可解析）。

## Task 2 — polyfill 契约 + Query Provider（FR-1/FR-5；validation §Task 2）

新建 `apps/mobile/src/polyfills.ts`：
```ts
// spec: web3-adapter §2.1 — order is a hard contract (getRandomValues first).
import 'react-native-get-random-values';
import 'fastestsmallesttextencoderdecoder';
// R8: Chrome remote debugging degrades getRandomValues to Math.random — real-money flows
// must never run under remote debug. Web: both polyfills are no-ops (native crypto/TextEncoder exist).
```
`src/app/_layout.tsx`：**第一行** `import '../polyfills';`；模块级 `const queryClient = new QueryClient({ defaultOptions: { queries: { staleTime: 30_000, structuralSharing: false } } })`（`structuralSharing:false` 为防御性、非 JSON-serialization 问题，N1/O-6；`retry/refetchInterval` 移至 balance query，MINOR-4）；`<QueryClientProvider client={queryClient}>` 包住 `<SafeAreaProvider>`（keyed Stack 重挂载不重挂 Provider）。
验证：`head -1` grep 断言；`grep -n 'structuralSharing'`；tsc（Task 5 统一）。

## Task 3 — adapter 源码 + specs/web3-adapter.md 修订（FR-2/FR-3/FR-7；validation §Task 3）

- `src/errors.ts`：`W3AErrorCode = 'E_W3A_UNKNOWN_CHAIN'`（T2.2 子集；T4.x 码注释列出不实现）；`class W3AError extends Error { readonly code }`；`isW3AError`。
- `src/chains.base.ts`（纯注册表 + `getChainConfig`）/ `src/chains.ts`（native：`export *` + `toViemChain` via viem `defineChain`）/ `src/chains.web.ts`（web：仅 `export * from './chains.base'`，无 viem）；注册表 mainnet(1) [`eth.drpc.org`, `ethereum-rpc.publicnode.com`] / sepolia(11155111) [`sepolia.drpc.org`, `ethereum-sepolia-rpc.publicnode.com`]（O-3 实装端点）；`getChainConfig` 抛 `E_W3A_UNKNOWN_CHAIN`。
- `src/balance.ts`（native）：`getNativeBalance(address, chainId): Promise<bigint>`（`createPublicClient` + `fallback(rpcUrls.map(u => http(u)), { rank: false })` + `getBalance`）；`getLatestBlockNumber(chainId): Promise<bigint>`；`src/balance.web.ts`（web）：同 API，用 `fetch` JSON-RPC + 顺序 failover，不引 viem（R7 split，2026-10-22）；`src/format.ts` 共享 `formatNativeBalance(wei, decimals=18)`（纯 BigInt → 截 4 位小数去尾零）；`src/address.ts`（native viem `isAddress`/`getAddress`）/ `src/address.web.ts`（正则 + lowercase）；/`normalizeAddress`（viem `isAddress`/`getAddress`）。
- `src/public-api.ts`：单一只读导出面（errors/chains/balance 全部再导出；**不含** `toViemChain`）；`src/index.ts` / `src/index.web.ts` 均 `export * from './public-api'`（review round 4 MINOR-6；签名/交易/验证 API = 文档注释指针（T4.x），零假实现）；`apps/mobile/src/web3-adapter.test.ts` + `jest.config.js`（review round 4 MAJOR-1）。
- `specs/web3-adapter.md`：§2.2 追加 T2.2 交付注记；§2.5 只读子集块改为本期口径（`getNativeBalance(address, chainId)` 更名 + `getLatestBlockNumber`/`formatNativeBalance`；`getTransactionStatus`/`buildUnsignedTxQrPayload` 标 T4.x）；附录 `T2.2 交付（2026-10-20；2026-10-22 AC-12/web split 更新）` = AC-9..AC-12 + TC-W3A-009..013（gherkin）。
验证：`pnpm -r --if-present run typecheck`（adapter 委派脚本）exit 0；`grep -rE 'privateKeyToAccount|createWalletClient|signDigest|importMnemonic' packages/web3-adapter/src` 零命中。

## Task 4 — store + Home + ui-wallet 修订 + MAN-017（FR-4/FR-6；validation §Task 4）

- `src/store/observer.ts`：Zustand v5 store（`selectedChainId: number` 默认 1；`watchedAddresses: string[]`；`setSelectedChain`；`setWatchedAddress(address|null)` 单值→数组）；注释声明 R1（无密钥语义）与无持久化。
- `src/app/(tabs)/home.tsx`：链 pill → `Pressable`（label `Chain · ${chain.name}`，点按切换 1↔11155111，`accessibilityRole="button"`）；pill 下 `NexVaultTextField`（label "Watch address"，placeholder `0x…`，`autoCapitalize="none"`——若原语不支持则扩展 prop）；地址输入经 `normalizeAddress` 校验（非法 → inline error，不发查询）；`useQuery({ queryKey: ['native-balance', chainId, address], enabled: !!address, queryFn: () => Promise.all([getNativeBalance(address, chainId), getLatestBlockNumber(chainId)]) → { wei, block } })`；四态渲染（无地址 → 原 "Not configured" 占位；isLoading → 骨架（`bg-shimmer` 条）；error → "RPC unavailable" + Retry 按钮调 `refetch()`；data → Total Balance `formatNativeBalance(wei) + ' ' + symbol` + caption `${chain.name} · Block #${block.toString()}`；Tokens 单行 ETH）。图表/区间 chips 不动。
- `specs/ui-wallet.md`：§2.1 home 行注记（T2.2 数据接入口径）；§5 追加 TC-UI-010（四态 gherkin）。
- `specs/pending-manual-test-cases.md`：MAN-017 行（Web + Android 双锚点余额走查；预期含 block 号；状态 ☐ 待走查，Task 6/7 后回填）。
验证：tsc；`grep -n 'useQuery' home.tsx`；store 无持久化 import（grep AsyncStorage）。

## Task 5 — 静态门禁 + AGENTS.md H4（FR-7；validation §Task 5）

1. `pnpm --filter mobile exec tsc --noEmit` → 0
2. `pnpm -r --if-present run typecheck` → 0
3. `npx expo export --platform web --output-dir /tmp/t22-web` → 0
4. `grep -rE 'signDigest|importMnemonic|vault-core' /tmp/t22-web/_expo/static/js/` → 1；`grep -rE 'signPrepared|signAndBroadcast|privateKeyToAccount' /tmp/t22-web` → 1
5. `grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile packages --include='*.ts' --include='*.tsx' --exclude-dir=node_modules` → 1
6. R5 console grep（宪法原文）→ 1
7. `npx expo-doctor` → 21/21
8. 漂移：`git diff apps/mobile/package.json packages pnpm-lock.yaml pnpm-workspace.yaml` = 仅五行 + 新包 + workspace link
9. **H4**：`AGENTS.md` B.5 R7 示例 `-o dist-web` → `--output-dir dist-web`（文本同步，证据用新文本复跑 4）。

## Task 6 — balance walk 自动化侧（FR-8/AC-7；validation §Task 6）

锚点：`0x9858EfFD232B4033E47d90003D41EC34EcaEda94`、`0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045`（mainnet）。
- curl 转录：对 mainnet 双 RPC 分别 `eth_getBalance` + `eth_blockNumber` → `evidence/curl-mainnet-{drpc,publicnode}.json`；交叉计算表（wei → formatUnits → UI 截断值；非零低于精度显示 `<0.0001`）。
- Web：`python3 -m http.server` 服 `/tmp/t22-web` → curl `/` 与 `/home` 200；转录 `evidence/web-serve-smoke.txt`。
- Android：`./gradlew :app:assembleRelease -PreactNativeArchitectures=arm64-v8a`（T2.1a 同管线）→ `adb install -r` → 停 Metro → 启动 → 输入锚点地址 → UI dump + 截图（余额 + block caption）→ `evidence/shell-home-balance-*.png/.txt`；记录 `adb devices`、`getprop ro.build.version.release`。

## Task 7 — MAN-017 owner 走查（validation §Task 7）

- 交付走查包（Web + Android 双侧自动证据；Android release APK 路径）；owner 走查后回填 MAN-017（B.4-4）。若 owner 未即时走查：MAN-017 保持 ☐ 待走查，validation 记录自动侧完成、人工侧待 owner；**关闭行注明**。

## Task 8 — 收尾（validation §Closure）

- validation.md 补 §Handoff 清单（T2.2 → 下一条目）、D-i/D-j、O-1/O-2、全部门禁结果。
- `AGENTS.md` Part A：阶段 cell 改「T2.2 已关闭（2026-10-20）；下一步 T3.x」+ 顶部关闭行（证据绝对路径）。
- `git add -A && git commit`（记录 commit hash）→ `git merge --no-ff feature/T2.2-web3-readonly` → 删分支 → `git push origin main`（记录 range）→ `git log/status` 复核。
