# NexVault T2.2 — web3 read-only adapter (viem + polyfills + Zustand/Query + balance) — combined skill

## Provenance

No single published skill covers T2.2 (viem 2.56 read-only balance query inside a pure-TS
workspace adapter, mounted through TanStack Query + Zustand, under NexVault's R1/R4/R5/R7/R8
red lines), so this skill combines the best viem and data-fetching skills with the project's
own adapter contract:

| Source | What it contributes to T2.2 | Gap / override |
|---|---|---|
| [0xSardius `viem` skill](https://github.com/0xSardius/onchain-typescript-skills/blob/main/skills/viem/SKILL.md) | viem 2.x low-level patterns: `createPublicClient` + `http`, `getBalance`, `as const` ABI typing, BigInt discipline, simulate-then-write, common-mistakes table | **Overrides: read-only only** — no `privateKeyToAccount`/`createWalletClient`/signing (R1; T4.x territory); chains come from the adapter registry, not inline; no `console.log` of anything sensitive (R5 — public values only) |
| [expo-data-fetching](https://github.com/expo/skills/blob/main/plugins/expo/skills/expo-data-fetching/SKILL.md) (official) | React Query v5 discipline: four states per screen (loading/error/empty/content), `isLoading` vs `isFetching`, skeleton-first with cached content on refetch, hydration gating | Matches ui-wallet §2.5 PERF_BUDGET (homeSkeletonFirst) — apply verbatim |
| `specs/web3-adapter.md` §1/§2.1/§2.2 (+ read-only sections) | The project's own contract: `polyfills.ts` first-line import order, `ChainConfig`/SUPPORTED_CHAINS (≥2 independent RPCs, failover + cross-check), `E_W3A_UNKNOWN_CHAIN`, viem-first APIs | Governing spec for every interface in this item |
| Zustand 5 + polyfill docs (react-native-get-random-values, fastestsmallesttextencoderdecoder) | Minimal store shape; polyfill semantics + R8 remote-debug caveat (Math.random degradation) | Reference — fetch current docs at implementation |
| NexVault `AGENTS.md` + T2.1/T2.1a conventions | R4 pins (all five rows already exist — verify, zero amendments), R5 "Not configured"/no fake data, R7 web-read-only grep, B.6-1 two-form tsc gate (now covering `packages/web3-adapter` too), B.4 evidence | Project-specific |

**Rejected:** 0xSardius `wagmi` skill (wagmi not in tech-stack — raw viem + TanStack Query is the
approved shape), uniswap swap-integration (trading scope), davila7 web3-specialist (agent
template, not a skill), popey native-data-fetching (NativeScript), @dzhechkov skills-web3
(generic), Expo Router data loaders (web-only route loaders — the spec mandates TanStack Query).

## Scope (owner 2026-10-20)

- **Create `packages/web3-adapter`** (pure-TS workspace package, zero native deps — B.1-3):
  `src/chains.ts` (mainnet + sepolia, ≥2 independent public RPC endpoints each, demo-scope)
  + `src/balance.ts` (read-only: `createPublicClient` → `getBalance` → formatted units)
  + the §2.1 polyfill contract. Transaction/signing APIs stay **stubs** until T4.x.
- **apps/mobile**: `polyfills.ts` as the first import (before expo-router); Zustand 5 store
  (chain selection + watched addresses — **no key semantics**, R1); TanStack Query
  QueryClientProvider mount; home renders a real balance for an input address.
- **NO**: private-key handling, wallet clients, transaction construction/broadcast (T4.x),
  wagmi, WalletConnect, fake balances (R5).

## Invariants (MUST)

1. **R1**: no key-material identifiers anywhere in the new code; the store holds only public
   data (chain id, addresses, cached balances); `privateKeyToAccount`/signing APIs must not
   even be imported in T2.2.
2. **R4**: all five dependencies already have approved rows — `viem` 2.56.9 (exact),
   `react-native-get-random-values` 2.0.0, `fastestsmallesttextencoderdecoder` 1.0.20,
   `@tanstack/react-query` 5.104.2, `zustand` 5.0.15. **Verify the rows at preflight and
   expect zero amendments**; any drift → stop and ask.
3. **R5**: no fabricated data — an unresolved RPC/chain shows **"Not configured"** with a retry,
   never a made-up balance; logs may contain addresses/balances (public) but never mnemonic/
   key material (grep gate).
4. **R7**: the adapter's read-only API is web-safe by construction (no key paths); the web
   export grep stays clean; Web = read-only observer (mission).
5. **R8**: polyfill order is a hard contract (random-values first line); the release/remote-debug
   caveat is documented, not worked around.
6. **B.6-1**: `packages/web3-adapter` becomes the second TS project — give it its own strict
   tsconfig + `typecheck` script; gates run both forms per the 2026-10-19 ruling
   (`pnpm --filter mobile exec tsc --noEmit` and `pnpm -r --if-present run typecheck`).
7. **B.4**: balance-walk evidence = returned balance **and block number**, recorded for Web and
   Android separately, plus the query command/response transcript.

## Preflight (Step 0)

```bash
cd /home/superguo/Projects/NexVaultRN
git status && git log --oneline -3          # main baseline; T2.1a merged
git switch -c feature/T2.2-web3-readonly
cat pnpm-workspace.yaml                     # packages glob must include packages/* (else amend as scaffold change)
grep -n 'viem\|random-values\|fastest\|tanstack\|zustand' specs/tech-stack.md   # the five rows
```

- Read `specs/web3-adapter.md` end to end (balance/read sections beyond §2.3 included) and
  `specs/ui-wallet.md` §2.5; adjudicate the T2.1a Handoff list per B.7-2 (record D-numbers).
- Fetch current docs for viem 2.x (`viem.sh/docs`) and the two polyfills; record URLs + access
  dates. Confirm `apps/mobile/src/app/_layout.tsx` is the polyfill import site (§2.1 route
  table: root `_layout` first line `import '../polyfills'`).

## Step 1 — packages/web3-adapter (pure TS)

- Workspace wiring: add the package to `pnpm-workspace.yaml` globs + `package.json`
  (private, `typecheck` script, exact `viem@2.56.9` dependency), strict tsconfig.
- `src/chains.ts` per §2.2: `ChainConfig`/`SUPPORTED_CHAINS` (mainnet 1, sepolia 11155111;
  ≥2 independent public endpoints each), `getChainConfig` throwing `E_W3A_UNKNOWN_CHAIN`.
- `src/balance.ts`: `getNativeBalance(chainId, address)` → `createPublicClient` +
  `getBalance` (returns bigint wei) + formatted-unit helper (`formatUnits`); failover over
  the configured `rpcUrls`; errors typed, no key fragments.
- Barrel `index.ts` exporting the read-only subset only; signing APIs left as documented stubs
  with `// T4.x` pointers (do not fake-implement them).

## Step 2 — Polyfills (hard contract)

- `apps/mobile/polyfills.ts`: first line `import 'react-native-get-random-values'`, then
  `import 'fastestsmallesttextencoderdecoder'` — exactly the §2.1 order; root
  `_layout.tsx` imports it **before anything else** (expo-router entry).
- Document the R8 caveat in a comment (remote debug degrades the CSPRNG to Math.random).

## Step 3 — Zustand + TanStack Query mount

- `src/store/`: minimal Zustand store — selected chain, watched address list; **no key
  semantics ever** (tech-stack row). Selectors only, no deep state.
- Root `_layout.tsx`: `QueryClientProvider` with a v5 `QueryClient` (retry + staleTime
  tuned for public-RPC calls); home skeleton-first per ui-wallet §2.5 — balance merges
  asynchronously into the already-rendered skeleton.
- Balance query hook per expo-data-fetching: four states (skeleton → error+retry → "Not
  configured"/zero → content), `isLoading` vs `isFetching` distinction, cached data survives
  failed refetches.

## Step 4 — Home wiring

- Input address → store → `useQuery` on `getNativeBalance` → formatted balance + symbol +
  chain name; chain switch re-queries. viem discipline: bigint end-to-end (format only at the
  render edge), `formatUnits` for display, no number coercion.

## Step 5 — Gates (B.4, two-sided)

```bash
cd /home/superguo/Projects/NexVaultRN
pnpm --filter mobile exec tsc --noEmit && pnpm -r --if-present run typecheck
cd apps/mobile && npx expo export --platform web -o /tmp/t22-web        # exit 0
grep -rE 'signDigest|importMnemonic|vault-core' /tmp/t22-web/_expo/static/js/ && exit 1   # R7
grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile packages --include='*.ts' --include='*.tsx' --exclude-dir=node_modules && exit 1   # R1 (expect clean)
git diff -- apps/mobile/package.json packages pnpm-lock.yaml pnpm-workspace.yaml   # only the 5 rows + new package
```

- **Balance walk** (owner anchors): Web + Android each query
  `0x9858EfFD232B4033E47d90003D41EC34EcaEda94` and `0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045`
  (vitalik.eth) on mainnet — record the returned balance **and block number** in validation.md
  (curl transcript for Web; device log/screenshot for Android). Register the walk as a MAN case.
- If Metro/web export hits a viem resolver issue: the plan's risk-e fallback
  (`resolver.resolveRequest` / `extraNodeModules` mapping) is the documented escape hatch —
  try the clean install first, record which path was taken.

## Step 6 — Verification and handoff

Write into `specs/features/2026-10-20-T2.2-web3-readonly/validation.md`: every command + exit
code + observed output; the balance-walk table (address → balance → block → platform);
dependency-drift proof. Handoff list: transaction/signing workflow → T4.x; malicious-RPC
cross-check + EIP-155 assertions → T4.x; WalletConnect → v2; TLS pinning → T3.3; chain
failover live-fire → later. Close with the Part A board line + git per B.3.

## Appendix — parity table (sources → sections)

| Source | Where it lands |
|---|---|
| 0xSardius viem skill | Steps 1/4 viem patterns + mistakes table; read-only overrides in Scope/Invariant 1 |
| expo-data-fetching | Step 3 four-states/hydration/skeleton discipline |
| web3-adapter spec | Steps 1–2 interface contracts |
| Zustand/polyfill docs | Steps 2–3 |
| NexVault AGENTS.md + T2.1/T2.1a conventions | Invariants 1–7, gates |
