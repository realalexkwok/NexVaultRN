# T2.2 — web3 read-only adapter（viem + polyfills + Zustand/Query + 余额只读）— requirements

> Status: 规划轮完成（2026-10-20，plan 经 exit_plan_mode 批准：`approved: true`）；实施由本会话执行 | Constitution: AGENTS.md C.1/B.2/B.4 + 本 feature SKILL.md（`.agents/skills/nexvault-t2-2-web3-readonly/SKILL.md`，提交 `9118080`，144 行，已通读）
> 上位文件：`specs/roadmap.md` 阶段 1（T2.2：viem + polyfill + Zustand/Query 挂载 + 链上余额只读）；`plans/3-day-implementation-plan.md` Day1 晚间 T2.2 行；`specs/web3-adapter.md`（§2.1/§2.2/§2.5 本期修订）；`specs/ui-wallet.md`（§2.1 home 行 + §5 TC-UI-010 本期修订）
> 条目范围（skill Scope）：**只读子集**——`packages/web3-adapter` 建包（chains + balance + polyfill 契约），apps/mobile 挂 polyfill / Zustand / TanStack Query / Home 真实余额。交易/签名 API 保持**文档级 stub 指针**（T4.x），禁止假实现。

## Goal

以 viem 2.56.9 为唯一 EVM 交互库，交付纯 TS 只读适配层与 Home 真实余额：① `packages/web3-adapter`（零原生依赖，B.1-3）= 链注册表（mainnet 1 / sepolia 11155111，各 ≥2 独立公共 RPC）+ 原生币余额查询（bigint wei）+ 显示格式化；② apps/mobile = polyfill 入口契约（get-random-values → textencoder，App 根 `_layout` 第一行）+ Zustand 观测钱包 store（无密钥语义，R1）+ TanStack Query Provider 挂载 + Home「watch 地址 → 链上余额」四态渲染。**Web 端只读、无假数据（R5）。R4：原五行核准零修订；2026-10-22 经 owner 批准「Add Jest tests now」新增 jest 工具链**（jest 29.7.0 / jest-expo 57.0.6 / @jest/globals 29.7.0，apps/mobile dev；tech-stack + doc/dependencies.md 已同步）。

## Owner decisions（规划轮 Q/A 2026-10-20，ask_user_question 两次超时后随 plan 批准生效）

> 过程记录：Round A 于 14:19 / 14:29 两次 ask_user_question（120s / 600s 预算）均超时，提问通道不可用；按 plan-mode 纪律把**推荐答案**折入 plan 并提交 `exit_plan_mode`（`approved: true`，2026-10-20）。**无 override**——下表即为批准口径。

| # | 问题 | 裁定（随 plan 批准） |
|---|---|---|
| Q1 | watch 地址输入位置（原 Home 无输入） | **A**：链 pill 下方紧凑输入行（`NexVaultTextField`）；平衡/图表/动作/Tokens 区块保持原位（偏差 **D-i**） |
| Q2 | 地址就绪后哪些区块渲染真实数据 | **A**：Total Balance = 格式化 ETH + caption「链名 · Block #n」；Tokens 列表 = 单行 ETH；图表 + 区间 chips 保持 "Not configured"（行情 T4.x） |
| Q3 | 链 pill 交互 | **A**：点按 mainnet ↔ sepolia 切换并重查询（偏差 **D-j**）；不做下拉组件 |
| Q4 | watched-address store 形态 | **A**：单输入框 UI + list-capable store + 会话内有效（无持久化——AsyncStorage/SecureStore 均未入 R4） |
| Q5 | T2.1a Handoff H1–H6 裁定（B.7-2） | H1 **关闭**（已完成，仅记录）；H4 **implement 本期**（AGENTS.md B.5 R7 命令文本 `-o` → `--output-dir`）；H2/H3/H5/H6 **replan → T4.x**（H3/H6 各自带 R4 提案） |

## Handoff re-read（B.7-2；上一条目 T2.1a 的 H1–H6 逐条裁定）

| Handoff | T2.1a 状态 | 本规划轮裁定 | 落点 |
|---|---|---|---|
| T2.1a-H1 Android 路由 smoke + MAN-016 走查 | 已完成（2026-10-20，owner 通过） | 无动作（记录） | — |
| T2.1a-H2 选中态 filled 图标对 | open | **replan** | T4.x（UI 打磨条目） |
| T2.1a-H3 web tab glyph 图标 | open（需新依赖） | **replan** | T4.x（自带 R4 提案） |
| T2.1a-H4 AGENTS.md B.5 R7 示例 `-o` 失效 | open | **implement（本期）** | 本条目 Task 5（AGENTS.md 文本同步）|
| T2.1a-H5 18 屏全量 + 真实列表/数据 | open | **replan** | T4.x（Home 余额子项本期已落地） |
| T2.1a-H6 地址等宽 + 首尾高亮、GradientBackground | open（渐变需新依赖） | **replan** | T4.x（自带 R4 提案） |

## R4 预检（五个依赖行核验，2026-10-20；零修订结论）

| 包 | 钉版 | 安装位置（tech-stack.md §2） | 行证据 | 结论 |
|---|---|---|---|---|
| viem | 2.56.9 | packages/web3-adapter + apps/mobile | tech-stack §2；dependencies §43 | ✓ 零修订（dependencies 行位置 cell 误写 `packages/vault-core`——doc/ 只读，记观察 O-1） |
| react-native-get-random-values | 2.0.0 | apps/mobile | §44 | ✓ |
| fastestsmallesttextencoderdecoder | 1.0.20 | apps/mobile | §45 | ✓ |
| @tanstack/react-query | 5.104.2 | apps/mobile | §25 | ✓ |
| zustand | 5.0.15 | apps/mobile | §26 | ✓ |

预检观察（不改档、入 validation）：**O-1** `doc/dependencies.md` viem 行安装位置写 `packages/vault-core + apps/mobile`，与权威 `specs/tech-stack.md`（`packages/web3-adapter + apps/mobile`）不一致；doc/ 只读（B.1-2），以 tech-stack 为准。**O-2** tech-stack typescript 行仍写 `~5.9.x / root（dev）`，实际为 `apps/mobile` devDependency `~6.0.3`（SDK 57 模板）——本期用**委派脚本**（adapter `typecheck` 调 `pnpm --filter mobile exec tsc`）绕开新 importer 条目，不触发 R4 修订。

## 功能需求（FR）

- **FR-1 polyfill 契约**：`apps/mobile/src/polyfills.ts` 第一行 `react-native-get-random-values`、第二行 `fastestsmallesttextencoderdecoder`（web3-adapter §2.1 硬性顺序）；根 `src/app/_layout.tsx` **第一条 import** `import '../polyfills'`（先于任何 app 模块）；文件内注释记录 R8 远程调试退化 Math.random 的告警。契约位置与 `specs/web3-adapter.md` §2.1（2026-10-22 修订）一致，无遗留冲突。
- **FR-2 adapter 包**：`packages/web3-adapter`（纯 TS，零原生依赖）：`package.json`（name `web3-adapter`、private、**`main = src/index`（无扩展名，Metro 平台解析；review M2/NIT-1 更正）**、`types = src/index.ts`、dependency `viem@2.56.9` 精确、`typecheck` 委派脚本）、`src/public-api.ts`（**单一导出面**，index.ts/index.web.ts 均 `export *`，review MINOR-6）、`tsconfig.json`（strict/noEmit/ES2022/bundler/skipLibCheck）、`src/{errors,chains.base,chains,chains.web,balance,balance.web,format,address,address.web,public-api,index,index.web}.ts`；**原生**用 viem（chains.ts/balance.ts/address.ts），**Web** 走纯 fetch JSON-RPC（balance.web.ts）+ 纯注册表（chains.web.ts）+ 无 keccak 的地址校验（address.web.ts），web chunk 不引 viem（R7 拆分，2026-10-22）；**无任何** `privateKeyToAccount`/`createWalletClient`/签名导入（skill Invariant 1）。
- **FR-3 只读 API（spec §2.2/§2.5 本期口径）**：`ChainConfig`（chainId/name/rpcUrls≥2/explorerTxUrl/nativeCurrency）、`SUPPORTED_CHAINS`（mainnet 1、sepolia 11155111）、`getChainConfig` 抛 `E_W3A_UNKNOWN_CHAIN`（`W3AError.code` 机器可读）；`getNativeBalance(address, chainId): Promise<bigint>`（viem `createPublicClient` + `fallback(http…, {rank:false})` 顺序 failover + `getBalance`）；`getLatestBlockNumber(chainId): Promise<bigint>`；`formatNativeBalance(wei, decimals=18)`（`formatUnits` → ≤4 位小数、去尾零）；`isValidAddress`/`normalizeAddress`（原生 viem `isAddress`/`getAddress`；Web 正则 + lowercase，无 keccak）。bigint 端到端，仅渲染边缘格式化（skill Step 4）。
- **FR-4 Zustand store**：`src/store/observer.ts`——`selectedChainId`（默认 1）、`watchedAddresses: string[]`、`setSelectedChain`（忽略未注册 chainId）、`setWatchedAddress`（归一化后写入，非法忽略）；**无持久化**、无密钥语义（R1）。
- **FR-5 Query 挂载**：根 `_layout.tsx` 模块级 `QueryClient`（**仅** `staleTime 30s` 与 `structuralSharing: false`——后者为防御性：跳过默认 `replaceEqualDeep` 深比较；v5 缓存不 JSON 序列化、primitive 用 `===` 比较，bigint 本安全，review N1/NIT-1 更正；**`retry 2` / `refetchInterval 30s` 挂在 home 的 balance `useQuery` 上**，review round 4 MINOR-4）；`QueryClientProvider` 包住 SafeAreaProvider 与既有 keyed Stack；首帧不 await 任何 RPC（ui-wallet §2.5 `homeSkeletonFirst`）。
- **FR-6 Home 接入**：链 pill 可点按切换（Q3/D-j）；pill 下 watch 地址输入（Q1/D-i）；`useQuery(['native-balance', chainId, address])`，queryFn 并行取余额 + 区块号；四态：无地址 "Not configured"（R5）→ loading 骨架（shimmer tokens）→ 缓存内容在失败刷新时保留 → error "RPC unavailable" + Retry；Total Balance = `formatNativeBalance` + symbol，caption = `链名 · Block #n`；Tokens = 单行 ETH；非法地址 → inline error 且不发查询。
- **FR-7 规格修订**：`specs/web3-adapter.md`（§2.2 交付注记、§2.5 只读子集口径 + T4.x 分界、附录 AC-9..AC-12 / TC-W3A-009..013）；`specs/ui-wallet.md`（§2.1 home 行注记 + §5 TC-UI-010；**a11y：§4 AC-10 + §5 TC-UI-011**，MAN-018 / H10）；`specs/pending-manual-test-cases.md` MAN-017；`AGENTS.md` B.5 R7 命令文本（H4）+ Part A 关闭行（Task 8）。
- **FR-8 证据纪律（B.4）**：每 Task 命令原文 + 退出码/观察结果；balance walk 表格（address → balance → block → platform）；curl 转录 + 设备 UI dump/截图持久化在 `evidence/`（绝对路径）；`tsc` 双形态 + export + R1/R5/R7 grep 全量记录。

## Out of scope（本条目不做）

签名/交易构造与广播、`readVerified` 主备交叉验证、EIP-155 断言（均 T4.x）；WalletConnect（v2）；TLS pinning（T3.3）；行情/图表数据（T4.x）；地址持久化（未来 R4 提案）；18 屏全量（T4.x）；H2/H3/H5/H6。（**adapter jest 单测已纳入本期**：review MAJOR-1 裁定后接线，`apps/mobile/src/web3-adapter.test.ts`，见 AC-2/AC-5/AC-9。）

## 验收标准（AC ↔ 用例 ↔ 门禁）

| AC | 内容 | 绑定用例/门禁 | 验证方式（B.4） |
|---|---|---|---|
| AC-1 | polyfill 顺序：`_layout.tsx` 第一行 `import '../polyfills'`；polyfills.ts 内 random-values 在 textencoder 前 | TC-W3A-001 | grep（入口文件断言）+ 退出码 |
| AC-2 | adapter 只读 API 契约 + `E_W3A_UNKNOWN_CHAIN` 机器可读 | TC-W3A-009/010 | **jest（3 suites / 22 tests：adapter 10 + web 8 + store 4）** + tsc 双形态 + grep |
| AC-3 | RPC 顺序 failover 机制在代码路径中（`fallback(..., {rank:false})`）；live-fire 演练递延 | TC-W3A-011 | grep + Handoff 记录 |
| AC-4 | Web 只读：导出产物无 vault-core 绑定与任何密钥材料/签名路径（`signDigest`/`importMnemonic`/`privateKeyToAccount`/`mnemonicToAccount`/`hdKey`/`viem`/`@noble/curves`/`@noble`/`@scure`/`secp256k1`/`createWalletClient`/`signMessage`/`signTransaction`/`mnemonicToSeedSync` = 0；2026-10-22 web split 后实测全 0） | TC-W3A-012 / R7 | export + grep 退出码（全符号矩阵见 validation §Task 5） |
| AC-5 | 余额正确性：UI 格式化值 == curl `eth_getBalance` wei 的 `formatUnits` 截断值；非零但低于显示精度 → `<0.0001`（review NIT-9） | TC-W3A-013 | **jest 格式化用例** + curl 转录 + 交叉计算表 |
| AC-6 | Home 四态 + 首帧零 RPC 阻塞（骨架先行） | TC-UI-010 + TC-W3A-008（部分） | 截图/UI dump/正文（四态含 RPC error：`evidence/web-walk-rpc-error.{png,txt}`，CDP 阻断 RPC 实测）+ 代码路径 |
| AC-7 | balance walk：Web + Android 两锚点均返回余额与区块号 | MAN-017 | 自动侧：RPC curl 转录 + `expo serve` 路由 200（Web）、设备 UI dump/截图（Android）；手工侧：**MAN-017 通过（2026-10-21，真实 Chrome/155 via CDP）**，证据见 validation §Task 7 与 `evidence/web-walk-*` |
| AC-8 | R1/R5 干净；无地址时显示 "Not configured"（无假数据） | R1/R5 门禁 | grep 退出码 + 走查证据 |
| AC-9 | `pnpm --filter mobile exec tsc --noEmit` 与 `pnpm -r --if-present run typecheck` 双形态 exit 0；`pnpm --filter mobile exec jest` exit 0（**3 suites / 22 tests**）；`expo export --platform web --output-dir` exit 0 | B.6-1 / B.6-3 / R7 | 命令 + 退出码 |
| AC-10 | 错误提示无障碍播报：输入错误出现时 TalkBack/VoiceOver **恰好播报一次**（不重复、不静默；实现为 `NexVaultTextField` 内单通道 announce） | TC-UI-011 | 组件实现（`NexVaultTextField`）；MAN-018 待真机（Android 可执行；iOS 侧 H10）。**本 AC 不纳入 T2.2 关闭门禁**（a11y 增强项，H10 跟踪；关闭以 AC-1..9 为准） |

## 记录偏差

- **D-i**：Home 新增 watch 地址输入行（Q1-A；原仓库 Home 无输入，读演示必需）。
- **D-j**：链 pill 由静态 "Not configured" 改为 mainnet↔sepolia 点按切换（Q3-A；原仓库为下拉菜单，本期不建下拉组件）。
