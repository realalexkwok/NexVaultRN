# NexVault T2.1 — mobile UI shell (Expo Router + NativeWind v4 + Reanimated + FlashList) — combined skill

## Provenance

No single published skill covers T2.1 (four-page shell over Expo Router with NativeWind v4,
Reanimated 4.7 and FlashList v2, under NexVault's R4/R5/R7 red lines), so this skill combines
the official Expo skill family with versioned reference docs and the repo's own constitutions:

| Source | What it contributes to T2.1 | Gap it leaves |
|---|---|---|
| [expo-router](https://github.com/expo/skills/blob/main/plugins/expo/skills/expo-router/SKILL.md) (official, v1.0.1) | File-based routes, `_layout.tsx` Stack, `Link`/`router`, page titles, groups; **SDK 56+ rule: never import `@react-navigation/*` directly** (use `expo-router/react-navigation`); kebab-case route files; tsconfig path aliases | Says routes live in `app/` — **repo override: `src/app/`** (apps/mobile/AGENTS.md) |
| [expo-project-structure](https://github.com/expo/skills/blob/main/plugins/expo/skills/expo-project-structure/SKILL.md) (official) | `src/app` routes-only + `components/` `screens/` `theme/` layout — matches the repo convention exactly | Structural guidance, no styling/navigation rules |
| [expo-design-system](https://github.com/expo/skills/blob/main/plugins/expo/skills/expo-design-system/SKILL.md) (official) | Design tokens (color/spacing/typography) + the "extend a styling library (NativeWind) in its own idiom" pattern — the NativeWind v4 theme setup | No NativeWind v4 install/config specifics |
| [expo-animation](https://github.com/expo/skills/blob/main/plugins/expo/skills/expo-animation/SKILL.md) (official; = Emil Kowalski's `animate-expo`, co-authored) | Reanimated 4 discipline: worklets two-runtime model, motion stays on the UI runtime | T2.1 shell needs no real motion — reference only |
| NativeWind **v4** docs (nativewind.dev, v4 not v5 RC) + FlashList v2 docs (shopify.github.io/flash-list) | Version-correct install/config shape (`withNativeWind` metro config, tailwind.config, global.css) and list usage | Reference material — must be fetched at implementation time, not recalled |
| `apps/mobile/AGENTS.md` + NexVault `AGENTS.md` | `src/app/` convention, `npx expo install`, CNG (never hand-edit `android/`), SDK 57 versioned-docs discipline, R4 pinned versions, R5 "Not configured", R7 web-only, B.4/B.6 gates | Project-specific |

**Rejected:** oimiragieo `nativewind-and-tailwind-css-compatibility` (pins nativewind **2.0.11** +
tailwind 3.3.2 — stale, contradicts the repo's v4 pin), Global-Software-Consulting
project-scaffolding ("always latest versions" philosophy conflicts with R4 pins), MolcajeteAI
`flashlist-patterns` (thin index page), Ultron nativewind (unverifiable JS shell),
`@expo/ui` skill (package not in `specs/tech-stack.md` — adopting it would be a new R4
dependency), generic `react-native-expert` tome.

## Scope

- **Four shell pages, Stack layout** (owner 2026-10-18): `/` = onboarding shell (index route),
  `home` / `receive` / `send` stack pages. No real wallet logic, no real data — R5 placeholders.
- Install **only** the R4-approved rows: `expo-router` ~57 (57.0.26), `nativewind` **exact
  4.2.7**, `react-native-reanimated` **4.7.1**, `react-native-worklets` **0.13.0**,
  `@shopify/flash-list` **2.3.3**, plus expo-router's documented SDK-57 peer set
  (safe-area-context / screens / linking / constants / status-bar — verify against the fetched
  install doc). **No other dependency changes.**
- **Do NOT**: add eslint/jest (T1.1 scaffold debt H6 — stays there); import vault-core from any
  screen (R7); build mnemonic input logic (that is T4.1); add animations beyond nothing.
- T1.5 handoffs (H1 device gate · H6 SPM-route ratification · H7 EAS trust flags · H8 release
  plumbing) and T1.3-H8 (bridge smoke → T4.1) are **adjudicated at the T2.1 planning round**
  (B.7-2) and recorded in `requirements.md` — this skill only fixes where they go.

## Invariants (MUST)

1. **R4**: `npx expo install` resolves SDK-compatible versions, then package.json is pinned to
   the exact tech-stack versions above; `git diff package.json pnpm-lock.yaml` shows only those
   rows. Never install a version outside the approved set; NativeWind stays **v4** (v5 RC pins a
   CVE-carrying worklets — excluded by tech-stack).
2. **Routes**: `src/app/` only (apps/mobile/AGENTS.md overrides the expo-router skill's
   `app/`); every file there is a route; `_layout.tsx` defines the Stack; no components/utils
   co-located in `src/app/`. Kebab-case filenames. `"main": "expo-router/entry"` per the
   manual-install doc. Never import `@react-navigation/*` (B.6-2 `no-restricted-imports`).
3. **R5**: every screen that would need config/RPC shows **"Not configured"** — no fake data to
   make the shell "look working"; no `console.*` logging of sensitive identifiers.
4. **R7**: the UI shell must not reference vault-core at all; the web-export grep must stay
   clean (gate in Step 4).
5. **R1**: the onboarding page is a **shell** — placeholder text/button only; no mnemonic
   bytes, no ArrayBuffer plumbing (T4.1's job). R1 grep must stay clean.
6. **B.4 evidence**: every gate command + observed output in
   `specs/features/2026-10-18-T2.1-mobile-ui-shell/validation.md`; both sides required for
   `[x]` (web export + route smoke on Android device/browser).
7. **SDK 57 doc discipline** (apps/mobile/AGENTS.md): before writing any Expo/RN API code, read
   the matching v57 docs (`docs.expo.dev/versions/v57.0.0/…`, `llms.txt` index) and the
   NativeWind v4 install page — never answer from training data; record fetched URLs + access
   date in validation.md.
8. **CNG**: `apps/mobile/android/` exists (T1.2 prebuild) — never hand-edit it; native config
   via app.json/config plugins only. Reanimated/worklets are native deps: after install, Expo Go
   is out — verify via `npx expo run:android` / development build and record it.
9. **B.6-1**: `tsc --noEmit` exit 0 (strict is already on in apps/mobile/tsconfig.json).

## Preflight (Step 0)

```bash
cd /home/superguo/Projects/NexVaultRN
git status && git log --oneline -3          # baseline; main at 12746df (T1.5 merged, device gate open)
git switch -c feature/T2.1-mobile-ui-shell
cd apps/mobile
cat AGENTS.md                              # the local-overrides layer is authoritative here
```

- Re-read `specs/tech-stack.md` rows 20/23/24/25/26 (the four pins + worklets binding) and
  record them in the planning round; adjudicate T1.5-H1/H6/H7/H8 + T1.3-H8 (dispositions above).
- Fetch and record (URL + access date): Expo Router manual installation
  (`docs.expo.dev/router/installation/`), NativeWind v4 installation
  (`nativewind.dev/docs/getting-started/installation`), FlashList v2 usage, and
  `docs.expo.dev/versions/v57.0.0/` for any API touched. Confirm the current scaffold state
  (blank template: `App.tsx` entry, no `src/`, no metro/babel/tailwind configs).

## Step 1 — Install (R4-exact pins)

```bash
cd apps/mobile
npx expo install expo-router react-native-safe-area-context react-native-screens   expo-linking expo-constants expo-status-bar        # expo-router peer set — verify vs the fetched doc first
npx expo install nativewind react-native-reanimated react-native-worklets @shopify/flash-list
```

Then pin exact in package.json: nativewind **4.2.7**, react-native-reanimated **4.7.1**,
react-native-worklets **0.13.0**, @shopify/flash-list **2.3.3** (lockfile recorded). Record
`git diff -- package.json pnpm-lock.yaml` — only these rows, nothing else.

## Step 2 — Router wiring (`src/app/`)

- `package.json` `"main"` → `"expo-router/entry"` (per the manual-install doc).
- `src/app/_layout.tsx` — root `Stack` from `expo-router` (or `expo-router/stack` per the
  skill), NativeWind `global.css` import, page titles via `Stack.Title`/options.
- `src/app/index.tsx` — onboarding shell (`/`): app identity text + disabled "Import" /
  "Create" placeholders ("Not configured" where applicable; no mnemonic logic).
- `src/app/home.tsx` / `src/app/receive.tsx` / `src/app/send.tsx` — shell screens; navigate
  with `Link`/`router.push` from `expo-router`; home may render a static FlashList with
  placeholder rows labelled "Not configured" (no fake balances).
- Non-route code outside `src/app/`: `src/components/`, `src/theme/` (expo-project-structure
  layout); tsconfig `@/*` path alias per the expo-router skill (record the tsconfig diff).

## Step 3 — NativeWind v4 + tokens + motion discipline

- `metro.config.js` via `withNativeWind`; `tailwind.config.js` with `content:
  ["./src/**/*.{ts,tsx}"]`; `global.css`; `nativewind-env.d.ts` — **verify the exact v4 file
  shape against the fetched NativeWind v4 docs**, not v5 RC and not memory.
- Design tokens per expo-design-system idiom: a minimal `src/theme/tokens.ts` (color/spacing/
  typography); all four pages consume tokens, zero hardcoded values.
- Reanimated 4.7 + worklets 0.13.0: assert the resolved versions match the pins; babel is the
  `babel-preset-expo` default (no babel.config.js needed unless the fetched docs say so — record
  which way the docs go). **No animation code in this item** — the skill's motion discipline is a
  reference for T4.x only.
- FlashList v2: only the static placeholder list on home; real lists arrive with T4.x data.

## Step 4 — Gates (B.4, two-sided)

```bash
cd apps/mobile
npx --no-install tsc --noEmit                                    # exit 0 (B.6-1)
npx expo export --platform web -o /tmp/t21-web                   # exit 0 — plan T2.1 acceptance
grep -rE 'signDigest|importMnemonic|vault-core' /tmp/t21-web/_expo/static/js/ && exit 1   # R7 (expect NO match)
grep -rnEi 'mnemonic|privateKey|seedPhrase' apps/mobile/src apps/mobile/App.tsx 2>/dev/null || true   # R1 (expect clean)
```

- **Android smoke** (manual side): `npx expo run:android` / development build on the Pixel 6a —
  walk onboarding → home → receive → send, no crash; record device, OS, route walk result.
- **Web smoke**: `npx expo start --web` — same route walk in a browser; console error-free.
- Record `git diff --stat` (only approved deps + new `src/` + configs) and
  `git diff -- package.json apps/mobile/package.json pnpm-lock.yaml` dependency-drift proof.

## Step 5 — Verification and handoff

Write into `specs/features/2026-10-18-T2.1-mobile-ui-shell/validation.md`: every command, exit
code, observed result; the two-side route-smoke records; the dependency-drift proof; the fetched
doc URLs with access dates. Handoff list: eslint/jest (H6) → T1.1 closure; bridge smoke
(T1.3-H8 / T1.5-H2) → T4.1; real FlashList/Zustand/TanStack usage → T2.2; T1.5-H1/H6/H7/H8
adjudication records. Close with the Part A board line + git per B.3 (merge `--no-ff`, branch
deleted).

## Appendix — parity table (sources → sections)

| Source | Where it lands in this skill |
|---|---|
| expo-router | Step 2 routing, Invariants 2/7, kebab-case + alias rules |
| expo-project-structure | Scope (src layout), Step 2 non-route placement |
| expo-design-system | Step 3 tokens, "NativeWind in its own idiom" |
| expo-animation (Emil Kowalski) | Invariant 7 context + Step 3 "no motion in T2.1" note |
| NativeWind v4 docs | Step 3 install/config verification mandate |
| FlashList v2 docs | Step 3 placeholder-list note |
| apps/mobile/AGENTS.md | Invariants 2/7/8 (src/app, expo install, CNG, v57 docs) |
| NexVault AGENTS.md | Invariants 1/3/4/5/6/9 (R4/R5/R7/R1/B.4/B.6) |
