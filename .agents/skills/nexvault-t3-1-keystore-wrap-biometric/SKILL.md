# NexVault T3.1 — Keystore/Keychain wrap + biometric gate (combined skill)

## Provenance

No single published skill covers T3.1 (Android Keystore AES-256-GCM wrap + per-use
BiometricPrompt gate + the iOS expo-secure-store equivalent, under the security spec contracts
and R1/R4/R5/R6 red lines), so this skill combines the strongest hardware-key reference with the
Expo module workflow and the project's own specs:

| Source | What it contributes to T3.1 | Gap / override |
|---|---|---|
| [claude-dev-suite `keystore-biometric` quick-ref](https://github.com/claude-dev-suite/claude-dev-suite/blob/main/skills/mobile/android-native/quick-ref/keystore-biometric.md) | **Best find**: TEE/StrongBox/Software hardware layers + `KeyInfo` security-level detection, full `KeyGenParameterSpec` flag reference (GCM, per-use auth window=0, enrollment invalidation, StrongBox→TEE fallback), BiometricPrompt mechanics | Deep reference — the governing flags come from `specs/security-android.md` §2.4, not from memory |
| [Expo `expo-module` skill](https://github.com/expo/skills/blob/main/plugins/expo/skills/expo-module/SKILL.md) (official) | Building `modules/security-android` + `modules/security-ios` as new Expo local modules (DSL, AsyncFunction, autolinking) — T1.2/T1.5-proven | No security domain |
| expo-secure-store v57 docs + Android Keystore/BiometricPrompt official docs | iOS Keychain wrap (`requireAuthentication`, `WHEN_UNLOCKED_THIS_DEVICE_ONLY`, 2048B cap); platform API signatures; CryptoKit `AES.GCM` (system framework — note: CryptoKit has no PBKDF2 but **does** have AES-GCM, distinct from the T1.5 KDF ruling) | Reference — fetch at implementation, record URLs |
| `specs/security-android.md` §2.1/§2.4 + `specs/security-ios.md` §2.1/§2.2 | **Governing contracts**: blob `version(1B)‖IV(12B)‖ciphertext‖tag`, AAD = 账户地址‖userId, the KeyGenParameterSpec flag set, `E_SEC_*` errors, Keychain OPTS | Authoritative for every interface |
| [skydoves `android-testing-skills`](https://github.com/skydoves/android-testing-skills) | JVM + instrumentation evidence discipline (T1.x-proven) | No keystore content |
| NexVault `AGENTS.md` | R1 boundary, R4 (conditional androidx.biometric gate), R5 logging, R6 [P]/[C]/[S] labels, B.4 two-sided evidence | Project-specific |

**Rejected:** skills.rest `android-security` (thin JS page), skillavatars android-security-encryption,
lobehub mobile-security, vanman2024 mobile-auth-patterns, jeffreytse grimoire (wrong path),
wallet-core skill (banned library).

## Scope (owner 2026-10-22)

- **Wrap-key submodule only.** Android: `getCapabilities` / `ensureWrapKey` / `wrapKey` /
  `unwrapKey` / `deleteWrapKey`. iOS: `ensureKek` / `wrapKey` / `unwrapKey` /
  `deleteKek` via expo-secure-store.
- **R1 coupling decision (owner)**: wrap/unwrap are **native-internal** APIs in T3.1; the JS
  surface is minimal (capabilities / ensure / delete). The spec §2.1 draft's ArrayBuffer JS face
  lands when T3.2 couples vault-core — the deviation is recorded in requirements.md, no R1
  exception needed now.
- **NOT** in T3.1: `runDetection`, attestation, `E_VAULT_SECURITY_GATE` fail-closed coupling
  (T3.2/T5.x). The T3.1 demo gate = the CryptoObject-required biometric prompt itself; the
  validation record states this boundary honestly.

## Invariants (MUST)

1. **R1**: k1 plaintext never crosses JS↔native in this item; unwrap plaintext lives only inside
   the native signing window and is zeroized in `finally` before return. AAD carries only
   public material (address + userId).
2. **R4**: Android uses platform APIs (`KeyGenParameterSpec`, `BiometricPrompt`, `Cipher`)
   — **preflight determines the real minSdk**: ≥ 28 → framework BiometricPrompt, zero new deps;
   < 28 → `GATE-R4-ANDROIDX-BIOMETRIC` (propose exact androidx.biometric pin at the planning
   round, owner approval **before** install). iOS: expo-secure-store row already approved
   (`~57`); CryptoKit AES.GCM is a system framework — no amendment.
3. **Hardware-first, never silent** (spec §2.4 + reference): `setIsStrongBoxBacked(true)` with
   a **TEE retry** on `StrongBoxUnavailableException`; SOFTWARE level only on debug/emulator
   and always surfaced via `WrapKeyInfo.securityLevel` — never silently degraded.
4. **Flag set** (spec §2.4, verify against the fetched reference): AES-256-GCM,
   `setUserAuthenticationRequired(true)`, `setUserAuthenticationParameters(0,
   AUTH_BIOMETRIC_STRONG or AUTH_DEVICE_CREDENTIAL)` (window=0, per-use),
   `setInvalidatedByBiometricEnrollment(true)`, `setUnlockedDeviceRequired(true)` **only
   API ≥ 35** (API 28–34 documented bug — default off).
5. **R5**: no `Log.*`/`print` with key material, AAD, or blob bytes; fixed `E_SEC_*` error
   templates without fragments.
6. **R6 labels**: hardware-backed wrap = **[P]** only after real-device verification;
   SOFTWARE_DEBUG path = clearly labeled, never upgraded; biometric prompt = authentication, the
   wrap key is the protection — label per spec §3, no inflating.
7. **Zeroization**: unwrap plaintext, KEK copies, IV/AAD buffers wiped (`Arrays.fill(0)` Kotlin;
   `memset_s`/`resetBytes` Swift); CoW `Data` caveat recorded **[C]**.
8. **B.4**: two-sided evidence for `[x]`; device acceptance = lock screen → biometric prompt on
   unwrap; **3 wrong fingerprints → fail-closed (session cleared)** recorded with device/OS;
   simulator is auxiliary only (B.4-5).

## Preflight (Step 0)

```bash
cd /home/superguo/Projects/NexVaultRN
git status && git log --oneline -3
git switch -c feature/T3.1-keystore-wrap-biometric
grep -rn 'minSdk' apps/mobile/android/build.gradle apps/mobile/android/gradle.properties 2>/dev/null   # the R4 branch point
```

- Read both security specs end to end; adjudicate the T2.2 handoff list (incl. H10) per B.7-2 at
  the planning round; record D-numbers.
- Fetch (record URLs + access dates): the full keystore-biometric quick-ref, expo-secure-store
  v57 docs, developer.android.com Keystore/BiometricPrompt pages.
- Autolinking preflight for the new modules (T1.2 Step 0 pattern).
- **iOS environment**: this Linux host has no Xcode — the iOS half follows the T1.5
  `MAC-HANDOFF.md` pattern (author here, compile/verify on the owner's Mac).

## Step 1 — modules/security-android (new Expo local module)

- Scaffold via `create-expo-module` (local) + autolinking check.
- `WrapKeyManager.kt`: `ensureKey` (StrongBox→TEE fallback, `KeyInfo.securityLevel`
  verification, never silent SOFTWARE), `wrap`/`unwrap` bound to a
  `BiometricPrompt.CryptoObject` (Cipher init ENCRYPT/DECRYPT with the wrapped key), 
  `deleteWrapKey`; every plaintext buffer `Arrays.fill(0)` in `finally`.
- BiometricPrompt plumbing: `currentActivity` must be a `FragmentActivity` (Expo module
  `currentActivity`); handle cancellation/negative-button and device-credential fallback per
  the flag set; no prompt state leaks.
- Blob pack/unpack: `version(1B) ‖ IV(12B) ‖ ciphertext ‖ GCM tag`; AAD = address‖userId
  (UTF-8), mismatch must fail decryption (GCM property, asserted in tests).
- JS surface (minimal per R1 decision): `getCapabilities`, `ensureWrapKey(opts)`,
  `deleteWrapKey`; `wrapKey`/`unwrapKey` native-internal with `// T3.2 wiring` pointers.
- Typed errors (`E_SEC_*`, fixed templates); no logs (R5).

## Step 2 — modules/security-ios

- `expo-secure-store` per spec §2.2 OPTS (`requireAuthentication: true`,
  `WHEN_UNLOCKED_THIS_DEVICE_ONLY`, synchronizable off); random 32B KEK (single value, well
  under the 2048B cap); `ensureKek`/`deleteKek`.
- `wrapKey`/`unwrapKey`: AES-256-GCM via **CryptoKit `AES.GCM`** (system framework) —
  verify the API shape against fetched docs; identical blob format as Android; AAD binding same.
- Native-internal wrap/unwrap, minimal JS surface, same `E_SEC_*` codes.
- Swift files compile-checked only on this host; the Mac handoff doc carries the build/test
  commands (T1.5 pattern).

## Step 3 — Tests

- **JVM** (blob logic unit-testable): pack/unpack roundtrip, version byte, wrong-AAD
  decrypt-fails (via a software AES-GCM key in test scope), error paths. Keystore/BiometricPrompt
  coverage is device-only — record the boundary honestly.
- **Instrumentation (device)**: `ensureWrapKey` → `KeyInfo.securityLevel` ∈
  {STRONGBOX, TRUSTED_ENVIRONMENT}; wrap→unwrap roundtrip byte-equality; wrong AAD fails;
  `deleteWrapKey` → subsequent unwrap fails with the typed error.
- **iOS XCTest**: KEK Keychain roundtrip (simulator); blob-format parity against the same fixed
  test vectors used on Android (structure + decrypt-interop asserted, IV randomness asserted).
- **Cross-platform blob parity**: one shared test vector set — same plaintext+AAD → same layout
  on both platforms (ciphertext differs, IV is random; assert structure and mutual decrypt where
  feasible with a test-only software key).

## Step 4 — Gates + demo path (B.4, two-sided)

```bash
cd /home/superguo/Projects/NexVaultRN
pnpm --filter mobile exec tsc --noEmit && pnpm -r --if-present run typecheck
cd apps/mobile/android && ./gradlew :security-android:testDebugUnitTest   # JVM gate (project name per ./gradlew projects)
cd /home/superguo/Projects/NexVaultRN
grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile packages modules/security-android/index.ts --include='*.ts' --exclude-dir=node_modules   # R1 (clean = exit 1)
grep -rnE 'Log\.|println|print\(|NSLog' modules/security-android modules/security-ios | grep -v Test    # R5 (clean)
git diff -- apps/mobile/package.json pnpm-lock.yaml modules/security-android/android/build.gradle   # zero unapproved deps (or the approved androidx.biometric row)
```

- **Device demo (manual side, MAN-registered)**: lock the Pixel 6a → invoke unwrap → biometric
  prompt appears; 3 wrong fingerprints → fail-closed with session cleared; correct fingerprint →
  unwrap succeeds. Record device/OS/outcome.
- Honest boundary note in validation.md: `signDigest` itself is not gated in T3.1 — the
  `E_VAULT_SECURITY_GATE` fail-closed coupling is T3.2's deliverable.

## Step 5 — Verification and handoff

Write into `specs/features/2026-10-22-T3.1-keystore-wrap-biometric/validation.md`: every
command + exit code + observed result; the device demo record; [P]/[C]/[S] labels per spec §3.
Handoff list: JS face + gate coupling → T3.2; attestation submodule → T5.x; iOS device run +
MAN → iPhone availability; SE P-256 wrap v2 → roadmap; SOFTWARE_DEBUG policy → security-testing.
Close with the Part A board line + git per B.3.

## Appendix — parity table (sources → sections)

| Source | Where it lands |
|---|---|
| claude-dev-suite keystore-biometric | Invariants 3–4 flag set + Step 1 mechanics |
| expo-module skill | Steps 1–2 module scaffolding/wiring |
| expo-secure-store / platform / CryptoKit docs | Step 2 + Invariant 2 |
| security-android/ios specs | Invariants 3/4/6, Steps 1–2 contracts, error codes |
| skydoves testing | Step 3 + Step 4 gates |
| NexVault AGENTS.md | Invariants 1/2/5/6/8, Preflight, B.4 evidence |
