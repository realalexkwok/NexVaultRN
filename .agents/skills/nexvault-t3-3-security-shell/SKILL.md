# NexVault T3.3 — security shell (FLAG_SECURE + TLS pinning + clipboard policy) — combined skill

## Provenance

No single published skill covers T3.3 (screen-capture hardening, dual-SPKI TLS pinning on the RPC
path, and the clipboard policy, under R4/R5/R6/R7 and the demo-scope labeling rules), so this
skill combines versioned docs for the three approved mechanisms with the research pinning anchor
and the T3.x app-layer conventions:

| Source | What it contributes to T3.3 | Gap / override |
|---|---|---|
| expo-screen-capture v57 docs + [Play FLAG_SECURE guidance](https://support.google.com/googleplay/android-developer/answer/14638385) | `preventScreenCaptureAsync` semantics, app-switcher blur; **iOS #48985 black-screen bug → same-key idempotent re-invocation** (plan row); FLAG_SECURE is a hint, not a boundary — honesty note | Reference — fetch at implementation |
| `@bam.tech/react-native-app-security` 1.0.0 docs + research §5.2/dim11 pinning decision | The approved dual-SPKI pinning lib; pin-set sourcing, rotation caveat, Web-no-pinning (R7) | Reference + research anchor (doc/ is read-only — cite, don't edit) |
| expo-clipboard v57 docs | Address copy with `EXTRA_IS_SENSITIVE` (`setAttributes`); private-key paths never copy | Reference + **GATE-R4-EXPO-CLIPBOARD** (not in tech-stack) |
| [claude-dev-suite `android-native`](https://github.com/claude-dev-suite/claude-dev-suite/tree/main/skills/mobile/android-native) skill | Platform window-flag / system-service discipline (FLAG_SECURE), T3.1/T3.2-proven family | No pinning/clipboard content |
| T3.2 conventions + T2.2 web3 client | App-layer wiring patterns; the pinning demo rides the balance-query path (observable channel); MAN registration discipline | Project-specific |
| NexVault `AGENTS.md` + plans §4 labeling table | **R6 labels: FLAG_SECURE/clipboard = [C], TLS pinning = [P] demo-scope** (plans §4); R2/R5/R7; B.4 two-sided evidence | Project-specific |

**Rejected:** yaklang `mobile-ssl-pinning-bypass` (attacker-side), skills.rest/tmhsdigital
mobile-security (thin/generic), hoangnguyen0403 react-native-security (unverified).

## Scope (owner 2026-10-24)

- **① Screen capture**: wire `expo-screen-capture` FLAG_SECURE **now** on the onboarding
  placeholder + a dedicated demo screen (black-screenshot evidence recorded in T3.3); the shared
  hook is what T4.1's real import page inherits. Same-key idempotent re-invocation to avoid the
  iOS #48985 black-screen bug; app-switcher blur enabled.
- **② TLS pinning**: `@bam.tech/react-native-app-security` 1.0.0 dual-SPKI pinning on the RPC
  client path (T2.2 balance query as the observable channel); pins sourced per research §5.2
  (rotation caveat in comments); **wrong-pin evidence = a deliberately mismatched endpoint is
  blocked with log-observable failure, correct pins pass**. Web honestly records **no pinning**
  (R7: Web cannot pin at app layer — architecture, not a gap).
- **③ Clipboard**: private-key paths never copy; address copy via `expo-clipboard` with
  `EXTRA_IS_SENSITIVE` — **GATE-R4-EXPO-CLIPBOARD** at the planning round (not in tech-stack;
  no install before owner approval).
- **NOT**: pin-rotation runbook (roadmap), keyboard hardening, Web pinning, UI security-indicator
  polish (T4.x).

## Invariants (MUST)

1. **R6 labels, no inflating** (plans §4 table): screen capture **[C]**, clipboard **[C]**, TLS
   pinning **[P] demo-scope** (with the rotation caveat stated wherever [P] is claimed) — record
   in the feature R6 table.
2. **R5**: no fake "working" states — a pin failure is shown/recorded honestly, never papered
   over; no sensitive data in logs.
3. **R7**: the web export stays key-path-free (grep gate); the web pinning note is honest
   documentation, not a workaround.
4. **R4**: only the two approved rows (screen-capture ~57.0.4, @bam.tech 1.0.0) plus the
   expo-clipboard amendment after owner approval — zero other drift.
5. **B.4**: acceptance evidence = black screenshot on the wired screen (real device), wrong-pin
   RPC block (log-observable), correct-pin pass — all recorded with commands + device info; MAN
   cases registered.

## Preflight (Step 0)

```bash
cd /home/superguo/Projects/NexVaultRN
git status && git log --oneline -3
git switch -c feature/T3.3-security-shell
grep -n 'screen-capture\|bam.tech\|clipboard' specs/tech-stack.md   # two rows exist; clipboard absent
```

- Re-read research §5.2 pinning + plans §4 labeling table; adjudicate the T3.2 handoff list
  (H-T3.2-1..8) per B.7-2 with D-numbers.
- Fetch (record URLs + dates): expo-screen-capture v57 docs (incl. the #48985 workaround shape),
  @bam.tech README (pinning API + pin-set format), expo-clipboard v57 docs (`setAttributes`).
- `GATE-R4-EXPO-CLIPBOARD` proposal prepared for the planning round.

## Step 1 — Screen capture (wire now)

- Shared hook (`src/hooks/useSecureScreen.ts` or equivalent): `preventScreenCaptureAsync`
  with **same-key idempotent re-invocation** (the iOS #48985 black-screen workaround — verify the
  exact shape against the fetched docs); enable app-switcher blur.
- Apply on the onboarding placeholder and a dedicated demo screen; the real import page calls the
  same hook in T4.1 (documented pointer).
- Evidence: `adb exec-out screencap` on the wired screen → black image; screenshot recorded in
  the feature evidence dir (NIT-3 pattern).

## Step 2 — TLS pinning (RPC path)

- Pin the web3 RPC client per @bam.tech docs (pin-set from research §5.2 anchors; rotation caveat
  comment at the call site).
- Evidence: correct pins → balance query succeeds; **deliberately mismatched endpoint → request
  blocked, log-observable** (record the exact log line); both sides recorded. Web: no-pinning note
  (R7) recorded honestly.

## Step 3 — Clipboard policy (after GATE-R4-EXPO-CLIPBOARD)

- Rule: **private-key paths never copy** (code audit + documented rule); address copy uses
  `setAttributes` `EXTRA_IS_SENSITIVE` per the fetched docs; audit output recorded.

## Step 4 — Gates (B.4, two-sided)

```bash
cd /home/superguo/Projects/NexVaultRN
pnpm --filter mobile exec tsc --noEmit && pnpm -r --if-present run typecheck && pnpm --filter mobile run typecheck:modules
cd apps/mobile && npx expo export --platform web -o /tmp/t33-web      # exit 0
grep -rE 'signDigest|importMnemonic|vault-core' /tmp/t33-web/_expo/static/js/ && exit 1   # R7
grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile packages --include='*.ts' --include='*.tsx' --exclude-dir=node_modules   # R1 (clean)
git diff -- apps/mobile/package.json pnpm-lock.yaml                  # only approved rows (+ clipboard amendment record)
```

- Device demos (MAN-registered): black screenshot on the wired screen; wrong-pin RPC block +
  correct-pin pass; clipboard audit walk.

## Step 5 — Verification and handoff

Write into `specs/features/2026-10-24-T3.3-security-shell/validation.md`: every command + exit
code + observed result; the R6 labels table; the two device demos; the clipboard audit. Handoff
list: real import-page FLAG_SECURE → T4.1; pin-rotation runbook → roadmap; Web no-pinning →
documented architecture note; iOS verification → device availability. Close with the Part A board
line + git **only on explicit owner instruction (B.3-5)** — otherwise end with a status report and
the closure request.

## Appendix — parity table (sources → sections)

| Source | Where it lands |
|---|---|
| expo-screen-capture docs + Play guidance | Step 1 + Invariant 1 honesty note |
| @bam.tech docs + research §5.2 | Step 2 pin sourcing |
| expo-clipboard docs | Step 3 + GATE-R4-EXPO-CLIPBOARD |
| cds android-native | Step 1 window-flag discipline |
| T3.2 + T2.2 conventions | Steps 1–2 wiring + evidence paths |
| NexVault AGENTS.md + plans §4 | Invariants 1–5, B.3-5 closure discipline |
