# NexVault T3.2 — security detection + fail-closed gate (combined skill)

## Provenance

No single published skill covers T3.2 (six defender-side detection signals → risk score → the
`E_VAULT_SECURITY_GATE` fail-closed coupling with vault-core, under R6's [C]-vs-[P] labeling
rules), so this skill combines the platform-API skills with the project's own security specs and
the T3.1 module conventions:

| Source | What it contributes to T3.2 | Gap / override |
|---|---|---|
| [claude-dev-suite `android-native`](https://github.com/claude-dev-suite/claude-dev-suite/tree/main/skills/mobile/android-native) + [`ios-native`](https://github.com/claude-dev-suite/claude-dev-suite/tree/main/skills/mobile/ios-native) skills | Platform API discipline: PackageManager, /proc/self/status, sysctl, dlsym, lifecycle — the T3.1-proven source family | No detection-domain content (no root/frida quick-ref exists anywhere) |
| `specs/security-android.md` §2.2/§2.4 + `specs/security-ios.md` §2.3 + `specs/vault-core.md` §2.4 | **Governing contracts**: `DetectionReport`/`RiskLevel L0–L5`, `runDetection('fast'\|'full')`, `getCachedRiskLevel` (<1ms hot path), `assertSigningAllowed` → `E_SEC_GATE_DENIED`; `SignatureSelfCheck`/`BundleSelfCheck`/`JailbreakDetector` signatures; `E_VAULT_SECURITY_GATE` code | Authoritative for every interface |
| T3.1 skill + validation (module conventions, H-T3.1-1/2/7) | T3.2 **extends** the existing `modules/security-android` + `modules/security-ios` — no new scaffolding; inherited handoffs | Project-specific |
| [skydoves `android-testing-skills`](https://github.com/skydoves/android-testing-skills) | JVM parser tests (injected fixtures) + device instrumentation | No security content |
| NexVault `AGENTS.md` | **R6: every detection signal = [C]; the ONLY [P]-eligible item is the fail-closed coupling** (检测信号与密钥派生 fail-closed 耦合 — the R6 exception, device-verified); R1/R5/R8; B.4 clean-vs-Magisk demo recordings | Project-specific |

**Rejected:** incogbyte android-reverse-engineering, DragonJAR iOS-Pentesting, brucesongs
mobile-app-instrumentation (attacker-side), mobile-security-mcp (MCP server), skills.rest
mobile-application-security (thin analysis), art-of-technology android-build-sign (signing, not
verification).

## Scope (owner 2026-10-24)

- **Demo/integration subset** (plan row, honest about the boundary): ① native signature check —
  PackageManager `GET_SIGNING_CERTIFICATES` SHA-256 constant comparison (~30 lines, **[C]**;
  the spec §2.4 `.so`-embedded no-PackageManager variant stays roadmap) ② bundle hash —
  `assets/index.android.bundle` SHA-256 self-check ③ Root 1–2 signals (su paths / Magisk
  package names / props) ④ jailbreak basics (path family / dyld / sysctl) ⑤ anti-debug
  (TracerPid; iOS `PT_DENY_ATTACH` via `dlsym`) ⑥ Frida 1–2 signals (port 27042 / maps
  strings / process names) → risk score → L0–L5 → `assertSigningAllowed` →
  `E_VAULT_SECURITY_GATE` in vault-core `importMnemonic`/`signDigest` + JS call wrappers.
- **Full multi-signal cross-coverage chain → roadmap** (4.5–6 人日, report §4.2 — do not absorb).
- **Inherited (owner-confirmed):** H-T3.1-1 iOS JS face + TC-SI-009 → implement; H-T3.1-2 Android
  JS face + gate coupling → implement; H-T3.1-7 wrap-key schema tag / unconditional rebuild →
  implement; H-T3.1-3/4 → T5.x; H-T3.1-5 device-pending; H-T3.1-6 → roadmap stage 3;
  H-T3.1-8 owner-fingerprint later.

## Invariants (MUST)

1. **R6 labeling, no inflating**: every detection signal is **[C]** (cost-raiser on rooted
   devices); the **fail-closed coupling** is the only [P]-eligible mechanism and only after
   real-device verification; the signature check MVP is [C]. Record all labels in the feature
   R6 table.
2. **R1 gate placement**: the gate runs **before any key material is released** in
   `importMnemonic`/`signDigest`; on `E_VAULT_SECURITY_GATE` the native path must not touch
   seed/privkey and must still run its zeroization.
3. **R5**: detection reports carry **vector IDs only** (spec §2.2: 不落明文细节); error messages
   use fixed templates; no `Log.*`/`print` with signal details.
4. **Hot path budget** (ui-wallet §2.5): `getCachedRiskLevel` <1ms cached read;
   `runDetection('fast')` after first frame; `full` only in background — never in the cold
   start path.
5. **B.4 two-sided**: clean device → L0 pass-through (sign works); Magisk device → risk above
   threshold → `signDigest`/`importMnemonic` rejected with `E_VAULT_SECURITY_GATE` (demo
   recording); simulator auxiliary only (B.4-5).
6. **B.6**: tsc both forms + the modules gate (`typecheck:modules`); JVM tests use injected
   fixtures, never weakened assertions; production seams are test-only and documented.

## Preflight (Step 0)

```bash
cd /home/superguo/Projects/NexVaultRN
git status && git log --oneline -3
git switch -c feature/T3.2-security-detection-gate
ls modules/security-android modules/security-ios   # T3.1 artifacts must exist (no new scaffolding)
```

- Re-read `security-android.md` §2.2/§2.4, `security-ios.md` §2.3, `vault-core.md` §2.4; the
  T3.1 handoff table (H-T3.1-1..8) is adjudicated at the planning round with D-numbers.
- **GATE-R4-IOSSECURITYSUITE (planning round)**: spec §2.3 names IOSSecuritySuite for jailbreak
  detection — it is **not** in tech-stack. Owner picks: (a) self-written basics only (paths /
  dyld / sysctl — zero new deps, recommended for the demo subset), or (b) IOSSecuritySuite with a
  full R4 amendment. No install before approval.
- Fetch official docs (record URLs + dates): `GET_SIGNING_CERTIFICATES` / APK signature,
  `/proc/self/status` TracerPid, Frida detection patterns, iOS `ptrace`/`sysctl` references.
- Device inventory: clean Pixel 6a + the Root/Magisk device for the demo (dev-environment list).

## Step 1 — Detection core (`modules/security-android`)

- `SignatureSelfCheck.kt` (MVP): PackageManager `GET_SIGNING_CERTIFICATES` → SHA-256 → compare
  against the embedded constant; rotation note in the KDoc; **[C]** label; `.so`-embedded
  variant → roadmap comment (spec §2.4).
- `BundleSelfCheck.kt`: read `assets/index.android.bundle`, SHA-256 vs expected (record how the
  expected hash is supplied — constant/config, planning decision).
- `RootDetector.kt` / `FridaDetector.kt` / `AntiDebugDetector.kt`: 1–2 signals each (MVP
  subset per plan); all parsers are pure functions over injected strings (JVM-testable).
- `DetectionAggregator.kt`: signals → `RiskLevel L0–L5` thresholds (spec mapping);
  `getCachedRiskLevel` cached read; `runDetection(scope)`; `assertSigningAllowed()` throwing
  `E_SEC_GATE_DENIED`.
- `SecurityStateCache`: in-memory only, updated by fast/full runs; never persisted (R5).
- No `Log.*` of signal details; vector IDs only.

## Step 2 — vault-core coupling + JS faces

- `VaultCoreModule.kt`: in `importMnemonic`/`signDigest`, call the security-android gate
  **before** key operations; on denial throw `E_VAULT_SECURITY_GATE` (fixed template, §2.4
  table); zeroization `finally` still runs.
- JS wrapper (`modules/vault-core/index.ts`): surface the new code machine-readably (T1.2
  platform-guard pattern); document the call order: **gate → biometric → crypto**.
- **H-T3.1-2**: Android JS face `wrapKey`/`unwrapKey` (ArrayBuffer) per spec §2.1 with the
  R1-exception `fill(0)` discipline, wired behind the same gate.
- **H-T3.1-7**: wrap-key schema — bump the blob version / add a key-schema tag, or do an
  unconditional `deleteWrapKey`+`ensureWrapKey` rebuild at first run; **never** attempt to
  "detect" legacy key types (T3.1 D6 note).

## Step 3 — iOS counterpart (authored here, Mac-verified — T1.5 pattern)

- `JailbreakDetector` per spec §2.3: path family, dyld suspicious libs, `sysctl`; per
  GATE-R4-IOSSECURITYSUITE decision, self-written basics or the approved lib.
- `denyDebuggerAttach()` via `dlsym(RTLD_DEFAULT, "ptrace")` (never direct linkage — App Store
  review risk, spec §2.3); `isDebuggerAttached()` via `sysctl(KERN_PROC)` `P_TRACED`.
- **H-T3.1-1**: iOS JS face wrap/unwrap + TC-SI-009 full KEK lifecycle tests.
- MAC-HANDOFF doc with build/test commands (this host has no Xcode).

## Step 4 — App-side wrappers (`apps/mobile`)

- ui-wallet §2.1/§2.5 mount point: security scan starts **after first frame**
  (`InteractionManager.runAfterInteractions`); call-site wrappers for
  `importMnemonic`/`signDigest` surface `E_VAULT_SECURITY_GATE` and drive the L0–L5
  indicator in the UI (security level visualization is a later UI item — T3.2 shows the gate
  state honestly).

## Step 5 — Tests

- **JVM**: root/frida/anti-debug parsers against injected fixtures (positive + negative);
  aggregation threshold mapping L0–L5; bundle-hash mismatch on a temp bundle; signature-check
  unit where the API is mockable, device-asserted otherwise (record the boundary).
- **Instrumentation (device)**: self-signature check passes on the dev-signed build; TracerPid
  parse on device; L0 pass-through on the clean device; gate-deny path exercised through a
  **test-only seam** (documented, never weakening production).
- **Manual (MAN, recorded)**: clean Pixel 6a → L0, sign works; Magisk device → risk above
  threshold → `signDigest`/`importMnemonic` rejected, demo recording per the plan row.
- **iOS XCTest**: jailbreak collector with injected fixtures (no real jailbreak needed);
  authored here, run on the Mac.

## Step 6 — Gates, labeling, handoff

```bash
cd /home/superguo/Projects/NexVaultRN
pnpm --filter mobile exec tsc --noEmit && pnpm -r --if-present run typecheck && pnpm --filter mobile run typecheck:modules
cd apps/mobile/android && ./gradlew :security-android:testDebugUnitTest
cd /home/superguo/Projects/NexVaultRN
grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile packages --include='*.ts' --include='*.tsx' --exclude-dir=node_modules   # R1 (clean)
grep -rnE 'Log\.|println|print\(|NSLog' modules/security-android modules/security-ios | grep -v Test    # R5 (clean)
git diff -- apps/mobile/package.json pnpm-lock.yaml modules/security-android/android/build.gradle   # zero unapproved deps
```

- **R6 table**: signature check [C] · bundle hash [C] · root/jailbreak/anti-debug/Frida [C] ·
  **fail-closed coupling [P]** (after device verification) — nothing else claimed as [P].
- Handoff list: full multi-signal chain → roadmap; `.so` signature variant → roadmap;
  attestation → T5.x; iOS real-device → pending; H-T3.1-8 fingerprint → owner; UI security-level
  indicator → T4.x polish. Close with the Part A board line + git per B.3.

## Appendix — parity table (sources → sections)

| Source | Where it lands |
|---|---|
| cds android-native / ios-native | Steps 1/3 platform API mechanics |
| security-android/ios + vault-core specs | Invariants 1–3, Steps 1–2 contracts |
| T3.1 skill + validation | Preflight, H-T3.1-1/2/7 in Steps 2–3 |
| skydoves testing | Step 5 + Step 6 gates |
| NexVault AGENTS.md | Invariants 1/2/4/5/6, B.4 demo recordings |
