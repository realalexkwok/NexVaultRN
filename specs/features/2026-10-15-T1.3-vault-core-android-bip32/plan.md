# T1.3 — vault-core Android BIP32/BIP44 deriveAddress (plan)

> Feature spec (plan). Branch: `feature/T1.3-vault-core-android-bip32`.
> Parent: `plans/3-day-implementation-plan.md` Day 1 T1.3.
> Skill: `nexvault-t1-3-vault-core-android-bip32` (feature-dir `SKILL.md`, sha256
> `1c9aec107f717a765e17a3c1da06fa3d62da0e20500c45b78842a2ca30e07ad7`).
> Session status: planning written 2026-10-15 (this file); implementation executes in a later
> session after `GATE-MODEL-SWITCH` and `GATE-R4-BC`.

## Hard gates (read before any work)

- **GATE-MODEL-SWITCH** — the implementation session MUST stop here and let the owner switch the
  model to **V4.1-Flash** before writing code. This planning session writes no code.
- **GATE-R4-BC** — `org.bouncycastle:bcprov-jdk18on:1.86` is NOT in `specs/tech-stack.md`.
  Tasks 1–2 may proceed (no dependency added), but **Task 3's amendment edits and Tasks 4–6 are
  blocked until the owner approves the R4 package and the approval is recorded in
  `validation.md` Task 3**.
- **GATE-REFERENCE-ORIGINAL-REPO (owner rule Q8, carried from T1.2)** — any question these
  specs/skill leave open whose topic is NOT React Native / TypeScript / JavaScript / Expo is
  resolved from the original NexVault repo at **`/home/superguo/Projects/NexVault`**
  (`AGENTS.md`, `specs/tech-stack.md`, `gradle/libs.versions.toml`, existing source/tests).
  RN/TS/JS/Expo questions use this repo's `AGENTS.md` + `apps/mobile/AGENTS.md` + the T1.3 skill
  + SDK 57 docs. This repo's R1–R8/R4 and this feature spec win on conflict; record the consulted
  original-repo file in `validation.md`; if still unresolved, stop and ask the owner.
- No `signDigest` / `destroy`; Android only; no iOS/web implementation.
- No dependency outside the FR-9 package, and only after owner approval.
- Known reference anchors (read during planning, re-verified in Task 0):
  - `/home/superguo/Projects/NexVault/core/core-security/src/main/java/com/nexvault/wallet/core/security/wallet/HDKeyManager.kt`
    (web3j-based; reference-only, do not copy the library);
  - `/home/superguo/Projects/NexVault/core/core-security/src/test/java/com/nexvault/wallet/core/security/wallet/HDKeyManagerTest.kt`
    TC-SEC-005 → `0x9858EfFD232B4033E47d90003D41EC34EcaEda94`.

## Task 0 — Preflight and baseline recording

Commands (record command + exit code + observed output in `validation.md` Task 0):

```bash
cd /home/superguo/Projects/NexVaultRN
git status --short                    # baseline; only the untracked planning files at first
git branch --show-current             # must be feature/T1.3-vault-core-android-bip32
git switch -c feature/T1.3-vault-core-android-bip32   # only if missing; baseline = main
cd apps/mobile
npx expo-modules-autolinking search --platform android --json | grep vault-core   # must resolve
# BIP-32 ground truth (T1.2 P0.4 pattern)
curl -fsSL --max-time 30 https://raw.githubusercontent.com/bitcoin/bips/master/bip-0032.mediawiki -o /tmp/bip32.mediawiki
sha256sum /tmp/bip32.mediawiki
# independent re-verification of vector 1 master xprv before transcription:
python3 - <<'PY'
# HMAC-SHA512("Bitcoin seed", seed) -> IL,IR; version 0x0488ADE4, depth 0, fingerprint 0,
# child 0, chainCode=IR, key=0x00||IL -> SHA256d -> first 4 bytes appended -> Base58Check encode
PY
```

Acceptance:
- Branch name matches the B.3 regex `feature/T1.3-vault-core-android-bip32`.
- Autolinking resolves `modules/vault-core` (planning-round observed result: it resolved).
- `bip-0032.mediawiki` downloaded, sha256 recorded, and vector 1's master xprv re-verified
  independently before any vector string is transcribed into `Bip32Test.kt`.
- `GATE-MODEL-SWITCH` state recorded (owner model switch happened / explicitly waived).
- No `android/` hand-editing (CNG): generated dirs are only produced by
  `npx expo prebuild --platform android`.

## Task 1 — H2 spec amendment (`specs/vault-core.md`, no dependency)

Owner-dispositioned (D2) and Q1-confirmed edits, each recorded as a diff in `validation.md`:

1. §2.1: `export type VaultHandle = number & …` → `export type VaultHandle = string & { readonly __brand: 'VaultHandle' }` and note the T1.2 frozen UUID semantics (opaque, non-encoding).
2. §2.2 Kotlin block: `AsyncFunction("importMnemonic") { mnemonic: ArrayBuffer -> Int }` → `String` (matching the committed T1.2 module); `deriveAddress`/`signDigest`/`destroy` `handle: Int` → `handle: String`.
3. §2.3 iOS block: same `Int → String` signature update for doc consistency (implementation stays T1.5).
4. §2.4 error table: delete the `E_VAULT_CHECKSUM_MISMATCH` row; fold its meaning into the `E_VAULT_INVALID_MNEMONIC` row ("word not in list / illegal word count / checksum mismatch").
5. TC-VC-002: the `Then` clause expects only `E_VAULT_INVALID_MNEMONIC` for both negatives.

Acceptance: `git diff -- specs/vault-core.md` shows exactly these five edits; nothing else in
`specs/` is touched.

## Task 2 — Pin the TS interface (no dependency)

1. `modules/vault-core/src/VaultCoreModule.ts`:

```typescript
import { NativeModule, requireNativeModule } from 'expo';

declare class VaultCoreModule extends NativeModule<{}> {
  importMnemonic(bytes: Uint8Array): Promise<string>;
  deriveAddress(handle: string, path: string): Promise<string>;
}

export default requireNativeModule<VaultCoreModule>('VaultCore');
```

2. `modules/vault-core/index.ts`: add `VaultHandle` branded type + the `deriveAddress`
   wrapper with the T1.2 fail-closed platform guard (`error.code = 'E_VAULT_UNSUPPORTED_PLATFORM'`,
   machine-readable). Keep `importMnemonic` untouched.

3. Extend the throwaway `/tmp` Node harness (T1.2 review-fix NIT-4 pattern) with a
   `deriveAddress` stub: `STUB_MODE=missing` → fixed-code throw; `STUB_MODE=present` →
   returns the stub value and the handle/path are passed through. Record both outputs.

Rules: no `VaultHandle = number`; do not touch `VaultCoreModule.web.ts`; no other method.
Record `git diff` and a `grep -n 'deriveAddress'` result in `validation.md`.

## Task 3 — GATE-R4-BC: BouncyCastle amendment approval (BLOCKING)

- Re-verify the FR-9 evidence (Maven Central `bcprov-jdk18on:1.86` metadata: publish/lastUpdated
  date, min-release-age ≥ 7 days, CVE check against BC security notes + Snyk) and record it.
- Present the exact FR-9 package to the owner and record the approval in `validation.md` Task 3
  (date, owner, exact coordinate) — the T1.2 FR-10 precedent.
- Only after approval: amend `specs/tech-stack.md` (new row under §2) and
  `doc/dependencies.md` (new section: version, purpose, publish date, CVE disposition, trust
  basis), then add `implementation("org.bouncycastle:bcprov-jdk18on:1.86")` to
  `modules/vault-core/android/build.gradle`.
- If approval is not given: stop. Tasks 4–6 stay blocked and the blocker is recorded.

## Task 4 — Pure Kotlin BIP32/address core

Create `modules/vault-core/android/src/main/java/expo/modules/vaultcore/Bip32.kt`
(framework-light `internal` functions; no `android.*` imports):

```kotlin
internal fun parseEthereumPath(path: String): IntArray
internal fun deriveAddressBytes(seed: ByteArray, pathIndices: IntArray): ByteArray // 20 bytes
internal fun toChecksumAddress(addressBytes: ByteArray): String
internal fun keccak256(bytes: ByteArray): ByteArray
```

Algorithm checklist (must):
- **parseEthereumPath** — regex whitelist `^m/44'/60'/(\d+)'/0/(\d+)$` (accept `h` and
  single-quote hardened spellings, nothing else); `account, index ∈ [0, 0x80000000)`; return
  `[44|H, 60|H, account|H, 0, index]`; violations → `E_VAULT_INVALID_PATH` (fixed template;
  path-syntax-class echo only).
- **Implementation correction (applied, recorded in validation.md Task 4):** BouncyCastle 1.86
  ships **no** HD-key API (`HDKeyDerivation`/`HDKey` are absent from the artifact; there is no
  `Base58` helper either). The BIP-32 CKD *glue* is therefore implemented in `Bip32.kt` on BC
  primitives (`HMac(SHA512Digest)` + `CustomNamedCurves.getByName("secp256k1")`); **all curve
  arithmetic stays in BC**, so `specs/vault-core.md` §2.2's "point math must not be
  self-implemented" rule holds. Base58Check lives only in the JVM test (formatting).
- **deriveAddressBytes** — `masterNode(seed)` on a **copy** of the stored seed (the store copy
  survives for T1.4); `deriveChild(parent, index)` loop over the indices (hardened ⇒ serialized
  parent key, else compressed parent pubkey); pubkey via
  `CustomNamedCurves.getByName("secp256k1").g.multiply(key).getEncoded(false)` (65B, `0x04‖x‖y`);
  keccak over `x‖y`; return the last 20 bytes. Wipe the seed copy, every chain code, the `x‖y`
  buffer and keccak scratch in `finally`.
- **toChecksumAddress** — lowercase hex → `KeccakDigest(256)` over the ASCII hex string →
  ERC-55 case mixing → `0x` prefix.
- **keccak256** — `KeccakDigest(256)` wrapper (pre-NIST Keccak, never `SHA3Digest`).
- BC lightweight API only; never register `BouncyCastleProvider`; no self-written point math;
  no `Log.*`; error text never interpolates key material.

Record the file diff and the zeroization checklist in `validation.md` Task 4.

## Task 5 — Expo module wiring

1. `modules/vault-core/android/src/main/java/expo/modules/vaultcore/VaultCoreModule.kt`:

```kotlin
AsyncFunction("deriveAddress") Coroutine { handle: String, path: String ->
  withContext(Dispatchers.Default) {
    val seed = handleStore[handle] ?: throw HandleNotFoundException()
    val indices = Bip32.parseEthereumPath(path)
    val seedCopy = seed.copyOf()
    try {
      Bip32.toChecksumAddress(Bip32.deriveAddressBytes(seedCopy, indices))
    } finally {
      seedCopy.fill(0)
    }
  }
}
```

2. `modules/vault-core/android/src/main/java/expo/modules/vaultcore/VaultExceptions.kt`: add
   `InvalidPathException` (`E_VAULT_INVALID_PATH`), `HandleNotFoundException`
   (`E_VAULT_HANDLE_NOT_FOUND`), `CryptoInternalException` (`E_VAULT_CRYPTO_INTERNAL`) —
   fixed templates, no fragments, `CodedException` like T1.2.

Rules: `importMnemonic` untouched; no `destroy`; unknown handle must not distinguish "empty
store" from "bad handle" in any message; no `Log.*`.

## Task 6 — Tests (official vectors first)

### 6.1 JVM `Bip32Test.kt` (mandatory automated gate)

Create `modules/vault-core/android/src/test/java/expo/modules/vaultcore/Bip32Test.kt` (layout
mirrors `src/main/java`; `// TC-VC-XXX` marker above each `@Test`):

| Test | Marker | Assertion |
| --- | --- | --- |
| `bip32Vector1` | `// TC-VC-013` | master + every V1 chain xprv/xpub match (test-side 78-byte serializer + Base58Check) |
| `bip32Vector2` | `// TC-VC-013` | same for V2 (incl. `2147483647'` hardened chains) |
| `bip32Vector3` | `// TC-VC-013` | same for V3 |
| `bip32Vector4` | `// TC-VC-013` | public-only derivation: every V4 chain xpub matches |
| — (V5 excluded) | — | BIP-32 vector 5 = 16 malformed **extended keys** (serialization validity, not derivation); T1.3 has no production parser → Handoff T1.3-H7 |
| `canonicalAddress` | `// TC-VC-003` | `Bip39.mnemonicToSeed(abandon×23 about, "")` → `deriveAddressBytes` @ `m/44'/60'/0'/0/0` → `0x9858EfFD232B4033E47d90003D41EC34EcaEda94` |
| `secondAddressCrossCheck` | `// TC-VC-003` | `m/44'/60'/0'/0/1` equals the `@scure/bip32` /tmp cross-check value (recorded in `validation.md`) |
| `keccakSanity` | `// TC-VC-015` | `keccak256("") == c5d246…5d85a470` |
| `erc55Vectors` | `// TC-VC-015` | the four ERC-55 vectors (lowercase in → mixed-case out) |
| `rejectsInvalidPaths` | `// TC-VC-016` | `m/`, `m/49'/60'/0'/0/0`, non-numeric, hardened change/index, `account ≥ 2^31` → `E_VAULT_INVALID_PATH` |
| `rejectsUnknownHandle` | `// TC-VC-016` | `deriveAddress("no-such-handle", validPath)` → `E_VAULT_HANDLE_NOT_FOUND` |

Vector transcription order: only after Task 0's sha256 + independent verification.

### 6.2 Instrumentation `VaultCoreModuleTest.kt` (H3 closure, device)

- Update `moduleDefinitionExposesImportMnemonic` → also assert `deriveAddress` exists
  (**no map-size pin** — Expo's builder adds `startObserving`/`stopObserving`; T1.2 lesson).
- `// TC-VC-017`: `importMnemonic` with the canonical 12-word bytes through the module path →
  non-empty handle → `deriveAddress(handle, "m/44'/60'/0'/0/0")` ==
  `0x9858EfFD232B4033E47d90003D41EC34EcaEda94`; invalid path → typed code.

### 6.3 Gradle runs

```bash
cd apps/mobile
npx expo prebuild --platform android --no-install   # CNG; no hand edits
cd android
./gradlew projects                                   # verify the :vault-core project path
./gradlew :vault-core:testDebugUnitTest --rerun      # mandatory vector gate
ANDROID_SERIAL=<serial> ./gradlew :vault-core:connectedDebugAndroidTest   # device smoke (H3)
```

Pin `ANDROID_SERIAL` (T1.2 duplicate-transport lesson). If no device is attached: record it,
H3 reopens as Handoff T1.3-H3, and the JVM gate remains the mandatory automated evidence (Q5).

## Task 7 — Verification and handoff recording

```bash
cd /home/superguo/Projects/NexVaultRN
git diff -- package.json apps/mobile/package.json pnpm-lock.yaml   # zero npm drift (BC is Gradle-only)
grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile --include='*.ts' --include='*.tsx' --exclude-dir=node_modules | grep -v 'packages/vault-core' && exit 1
grep -rnE 'Log.|println' modules/vault-core/android/src
cd apps/mobile && npx --no-install tsc --noEmit
```

Zeroization review (recorded item by item in `validation.md` Task 7): seed copy, `x‖y` pubkey
buffer, keccak buffers, path buffers; the BC `BigInteger` intermediates listed as the **[C]**
caveat. Write the final Handoff list (T1.3-H1..H6 from `requirements.md`). T1.3 is never marked
`[x]` unless the JVM vector gate output and the zeroization review note are both recorded with
command + output (B.4-3).

## File manifest (T1.3 implementation)

| File | Action |
| --- | --- |
| `specs/vault-core.md` | amend (H2, Task 1 — five edits) |
| `modules/vault-core/src/VaultCoreModule.ts` | modify (add `deriveAddress` declaration) |
| `modules/vault-core/index.ts` | modify (add `VaultHandle` + guarded wrapper) |
| `modules/vault-core/android/src/main/java/expo/modules/vaultcore/Bip32.kt` | create |
| `modules/vault-core/android/src/main/java/expo/modules/vaultcore/VaultCoreModule.kt` | modify (add `deriveAddress` AsyncFunction) |
| `modules/vault-core/android/src/main/java/expo/modules/vaultcore/VaultExceptions.kt` | modify (add three exceptions) |
| `modules/vault-core/android/build.gradle` | modify (BC coordinate — after GATE-R4-BC) |
| `specs/tech-stack.md`, `doc/dependencies.md` | amend (after GATE-R4-BC) |
| `modules/vault-core/android/src/test/java/expo/modules/vaultcore/Bip32Test.kt` | create (after GATE-R4-BC) |
| `modules/vault-core/android/src/androidTest/java/expo/modules/vaultcore/VaultCoreModuleTest.kt` | modify (H3) |
| `specs/features/2026-10-15-T1.3-vault-core-android-bip32/validation.md` | fill every task |

## Risks

| Risk | Trigger | Response |
| --- | --- | --- |
| R4 approval not granted | Owner blocks the BC package | Halt at GATE-R4-BC; Tasks 4–6 stay blocked; record the blocker (never fake evidence) |
| No device at implementation | `adb devices` empty | H3 → Handoff T1.3-H3; JVM gate stays mandatory (Q5) |
| `@scure/bip32` unavailable in `/tmp` | npm install fails there | Use a stdlib-Python secp256k1/hmac cross-check instead; record which one ran |
| BIP-32 transcription error | vector string typo | Task 0 independent verification catches it before any test asserts (never fix a failing test by weakening production code — B.6-5) |
| Duplicate adb transports | two entries for one device | Pin `ANDROID_SERIAL` (T1.2 evidence) |
| Prebuild drift | `expo prebuild` updates scripts | Record diff; no dependency change is acceptable without the R4 gate |
