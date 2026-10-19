# NexVault T2.1a — mobile UI visual parity with the original repo (combined skill)

## Provenance

No single published skill covers T2.1a (transcribe the original Kotlin app's design system into
the NativeWind v4 shell and prove visual parity — without inventing anything), so this skill
combines the official Expo design-system skill, two community design skills, versioned docs and
the original-repo reference discipline:

| Source | What it contributes to T2.1a | Gap / override |
|---|---|---|
| [expo-design-system](https://github.com/expo/skills/blob/main/plugins/expo/skills/expo-design-system/SKILL.md) (official) | Token table discipline, "extend NativeWind in its own idiom", design-system drift audit (hardcoded colors/spacing/fonts) | No reference-source concept — T2.1a's tokens are **fixed by the original repo**, not designed |
| [brunoviews `react-native-design`](https://github.com/brunoviews/agent-skills/blob/main/react-native-design/SKILL.md) | Design procedure + reference files (color-system, styling-stacks, platform-constraints: safe areas, edge-to-edge), bundled WCAG contrast script (`scripts/contrast.mjs`) | **Override: palette invention is forbidden** — the skill's "generate a palette" direction does not apply; only its audit/token mechanics |
| [apexscaleai `design-system-enforcer`](https://github.com/apexscaleai/codex-claude-skills/blob/main/.codex/skills/design-system-enforcer/SKILL.md) | "Don't drift" diff-gate: flag hex/`rgba` literals, magic spacing, ad-hoc font sizes, duplicated components — maps 1:1 onto TC-UI-007 (zero hardcoded values) | Web+mobile generic; no NativeWind specifics |
| expo-router `tabs` reference (official skill repo) | `NativeTabs` 5-tab bottom navigation, tab bar styling | Reference — fetch at implementation |
| NativeWind v4 theming docs + expo-font docs (v57) | Token→tailwind.config wiring; Inter via `expo-font` with bundled TTFs | Reference — fetch at implementation, never from memory |
| Original repo `/home/superguo/Projects/NexVault` @ `3c7487f` + `/tmp/nexvault-original-ui-inventory.md` | **The authoritative visual DNA** (dark-M3 cobalt palette, Inter + M3 type scale, shapes/spacing ladder, NexVaultCard/Button/TextField/TopBar, chrome, 5-tab nav) | Its `doc/03` (purple `#6C5CE7` + Space Grotesk) is **explicitly rejected** — code wins |
| NexVault `AGENTS.md` + T2.1 skill conventions | R4 (Inter via expo-font = SDK binding unit → planning-round gate), R5/R7/R1, B.4 visual-walk MAN, `src/app/` + `npx expo install` + v57 doc discipline | Project-specific |

**Rejected:** apex `modern-ui-ux` ("make it nicer" — parity is not modernization), argus
Storybook visual regression / driftx / pixel-print (pixel-diff across different renderers is
fragile; the owner defined the acceptance as a MAN visual walk), generic `react-native-expert`
tome.

## Scope (owner 2026-10-19, roadmap v1.3 scope A)

- **Visual layer only**: replace the T2.1 warm tokens (`#1B1815`/`#C79A6B`) with the original
  dark-M3 palette; build `NexVaultCard` / `NexVaultButton` / `NexVaultTextField` /
  `NexVaultTopBar` primitives; transparent centered TopBar chrome (Home has no bar — the chain
  selector leads it); **5-tab NativeTabs bottom nav (Home/History/DApp/NFT/Settings**, last three
  as "Not configured" placeholders); reskin the four existing pages + `+not-found`.
- **NO**: data/polyfill work (T2.2 unchanged), full 18-screen build-out (T4.x), animation,
  fake data (R5).
- Planning round amends `specs/ui-wallet.md` §2.1 (route table → `(tabs)` group) and §2.3
  (token block → original palette) and adjudicates T2.1-H3…H9. **T2.1 stays closed.**

## Invariants (MUST)

1. **Reference authority**: the original repo's **implementation code** @ `3c7487f` and the
   inventory are the only visual sources; `doc/03` is rejected; **every** hex/spacing/type
   value in the port carries an inventory/code evidence reference in a comment — zero invented
   values (transcription errors are treated like T1.3's vector-transcription findings).
2. **R4**: Inter arrives via `expo-font` (SDK binding unit) — `GATE-R4-EXPOFONT`: propose the
   tech-stack.md/dependencies.md rows at the planning round, owner approval **before** install.
   TTFs are copied from the original repo's bundled assets **with their OFL license file**; no
   other new dependency. NativeWind stays v4 (v5 RC forbidden).
3. **R5/R7/R1**: placeholder tabs/screens show **"Not configured"**; no vault-core imports; the
   R1/R5/R7 greps stay clean (gates below).
4. **B.6**: `tsc --noEmit` exit 0; **TC-UI-007 discipline**: zero hardcoded values outside
   `src/theme/tokens.ts` (+ tailwind.config theme.extend mapping) — enforced by the
   design-system-enforcer checklist with file:line output.
5. **B.4**: the visual-parity walk is a registered MAN case; **reference side = original APK
   screencaps on the Pixel 6a** (owner decision; fallback = code+inventory transcription if the
   original app cannot be built/installed — record which path was used).
6. **[P]/[C]/[S]**: this item introduces no security mechanism — no labels needed beyond noting
   that absence.

## Preflight (Step 0)

```bash
cd /home/superguo/Projects/NexVaultRN
git status && git log --oneline -3
# the governance branch feature/T2.1a-mobile-ui-parity (c27c96d) already exists on top of 26ad458;
# the planning round decides whether to continue it or rebranch from main (record the decision)
cp /tmp/nexvault-original-ui-inventory.md /tmp/t21a-inventory-backup.md   # /tmp is volatile — persist a copy early
```

- Read the original repo (reference only): `app/src/main/res/values/colors.xml`,
  `themes.xml`, `ui/**` Compose screens (esp. `ui/main/MainScreen.kt`), navigation setup,
  and the Inter TTF asset paths + license file. Record every value source (file:line).
- Attempt the reference side of the walk: look for an existing APK under the original repo's
  build outputs, else try a debug build; install on the Pixel 6a and screencap the four+tab
  screens (`adb exec-out screencap -p`). Record the result — failure falls back to the
  code+inventory path, never to "looks close enough".
- Fetch versioned docs (record URLs + access date): NativeWind v4 theming (dark mode/tokens),
  expo-router `tabs.md` reference (NativeTabs), expo-font v57 (`docs.expo.dev/versions/v57.0.0/`).

## Step 1 — Planning-round amendments (before code)

- Amend `specs/ui-wallet.md` §2.3: replace the warm token block with the original palette
  (bg `#0A0E1A` · surface `#121829` · surfaceVariant `#1A1F35` · cardSurface `#141928` ·
  cardBorder `#1E2440` · primary `#2D5AF0` · primaryContainer `#1E3A8A` ·
  onPrimaryContainer `#D8E2FF` · secondary `#00D4AA` · tertiary `#7B61FF` · error
  `#FF4C6E` · positive `#00E676` · negative `#FF5252` · info `#2196F3` · text
  `#FFFFFF`/`#A0A8C8`/`#5A6180` · outline `#404759` · outlineVariant `#252B3F` ·
  shimmer `#1E2440`/`#2A3148`; Inter + M3 type scale; shapes 4/8/12/16/24; spacing
  2/4/8/16/24/32/48; gutters 24 (onboarding/receive) / 16 (home/…); button 52dp; input 56dp).
- Amend §2.1 with the `(tabs)` group (Home/History/DApp/NFT/Settings) per the original nav.
- `GATE-R4-EXPOFONT` proposal + owner approval; adjudicate T2.1-H3…H9 (B.7-2). Record all
  decisions with D-numbers.

## Step 2 — Font assets (Inter)

- Copy the Inter TTFs from the original repo (record source paths) into
  `apps/mobile/assets/fonts/` **together with the OFL license file**; wire via expo-font per
  the fetched v57 docs (`npx expo install expo-font` only after GATE-R4-EXPOFONT approval).

## Step 3 — Tokens (transcribe, don't design)

- Rewrite `src/theme/tokens.ts` to the palette above — each value commented with its inventory
  evidence reference; update `tailwind.config.js` `theme.extend` mapping; remove all T2.1 warm
  values.
- Run the brunoviews contrast script for every text/surface pair (record outputs; the palette is
  fixed, so any failing pair is reported to the owner, not silently "fixed").

## Step 4 — Primitives (`src/components/`)

- `NexVaultCard.tsx` — elevation 0, 1px outline (`#1E2440`), radius 16, inner padding 16.
- `NexVaultButton.tsx` — height 52, radius 16, variants on the M3 color roles.
- `NexVaultTextField.tsx` — height 56, radius 12, label/labelSmall typography.
- `NexVaultTopBar.tsx` — transparent, center-aligned 18sp SemiBold title; Home renders **no**
  bar (chain-selector-led).
- No elevation/shadows (original cards are flat 1dp-outline) — the brunoviews platform-constraints
  reference is the anti-shadow checklist.

## Step 5 — Chrome + navigation

- `src/app/(tabs)/_layout.tsx` — `NativeTabs` per the expo-router tabs reference; five tabs
  (Home/History/DApp/NFT/Settings); last three = "Not configured" placeholders.
- Keep the root Stack `_layout.tsx`; reskin onboarding/receive/send/not-found with tokens +
  primitives; typed routes stay on.

## Step 6 — Gates (B.4, two-sided)

```bash
cd apps/mobile
npx --no-install tsc --noEmit
npx expo export --platform web -o /tmp/t21a-web                    # exit 0
grep -rE 'signDigest|importMnemonic|vault-core' /tmp/t21a-web/_expo/static/js/ && exit 1   # R7
cd /home/superguo/Projects/NexVaultRN
grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile --include='*.ts' --include='*.tsx' --exclude-dir=node_modules   # R1 (clean = exit 1)
git diff -- apps/mobile/package.json pnpm-lock.yaml               # only the approved expo-font row
```

- Android route smoke on the Pixel 6a (tabs + four pages, no crash) and Web smoke (browser,
  console clean) — recorded with device/OS.
- **MAN visual walk**: side-by-side screencaps (original APK vs RN shell) for the pages in
  scope; owner/agent walk record per B.4-4; drift audit per the design-system-enforcer checklist
  (hex literals, magic spacing, duplicate components) with file:line findings — all resolved or
  handoff-registered.

## Step 7 — Verification and handoff

Write into `specs/features/2026-10-19-T2.1a-mobile-ui-parity/validation.md`: every command +
observed output; token evidence table (reference value → source file:line → tokens.ts key);
walk verdicts; the drift-audit report; GATE-R4-EXPOFONT approval record. Handoff candidates:
full 18-screen build-out → T4.x; real data in lists → T2.2; motion discipline → T4.x polish;
remaining T2.1-H items per the planning-round adjudication. Close with the Part A board line +
git per B.3 (merge `--no-ff`, branch deleted).

## Appendix — parity table (sources → sections)

| Source | Where it lands |
|---|---|
| expo-design-system | Step 1/3 token discipline, Step 6 drift audit |
| brunoviews react-native-design | Step 3 contrast script, Step 4 platform constraints (flat cards, safe areas), Preflight reference files |
| apexscaleai design-system-enforcer | Step 6 don't-drift gate + TC-UI-007 |
| expo-router tabs reference | Step 5 NativeTabs |
| NativeWind v4 / expo-font docs | Steps 2–3 (fetched at implementation) |
| Original repo @`3c7487f` + inventory | Invariant 1, Preflight, Steps 1–5 evidence |
| NexVault AGENTS.md + T2.1 skill | Invariants 2–6, gates, `src/app` conventions |
