---
name: nexvault-t1-2-vault-core-android-bip39
description: Combined agent skill for NexVault plan T1.2 — implement importMnemonic in modules/vault-core as an Expo local module (Android/Kotlin) using self-written BIP39 validation and PBKDF2-HMAC-SHA512, opaque native handles, input zeroization, and official BIP39 vector tests. Use for T1.2, importMnemonic, BIP39, PBKDF2, mnemonic import, or vault-core Android work. Not for deriveAddress/signDigest/destroy (T1.3/T1.4).
version: 0.1.0
---

# NexVault T1.2 — vault-core Android BIP39 (combined skill)

## Provenance

No single published skill covered all of T1.2, so this skill combines the best parts of the
following verified sources:

| Source | What it contributes to T1.2 | Gap it leaves |
|---|---|---|
| Expo official [`expo-module`](https://github.com/expo/skills/blob/main/plugins/expo/skills/expo-module/SKILL.md) skill | Expo Modules API DSL, Kotlin module shape, AsyncFunction, type map (`kotlin.ByteArray` ↔ JS `Uint8Array`), autolinking, scaffolding rules | No crypto/domain knowledge, no test discipline |
| [`android-testing-skills`](https://github.com/skydoves/android-testing-skills) (JVM + instrumentation sets) | JVM unit-test organization, AndroidJUnit4 instrumentation workflow, ADB/Gradle test commands, verification discipline | No Expo module or BIP39 content |
| [BIP-0039 spec](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki) + [Trezor official vectors](https://github.com/trezor/python-mnemonic/blob/master/vectors.json) | Algorithm/validation rules and authoritative test vectors | Reference material, not an agent workflow |
| NexVault `AGENTS.md` R1/R4/R5/B.6 | Security red lines, dependency gate, strict verification | Project-specific only |

Rejected as primary sources: `awesome-android-agent-skills` and `j4flmao/agent-skills/android`
(general app architecture, no native Expo module/crypto), `android-additional-tests` (UI
navigation tests), `ruvnet/agent-spec-mobile-react-native` (agent template, not operational
instructions), and Trust Wallet `wallet-core` skill (its library is banned by
`specs/tech-stack.md` R4).

## Scope

- Implement **only** `importMnemonic(mnemonicBytes: ArrayBuffer) → handle`.
- **Do not** implement `deriveAddress` / `signDigest` / `destroy` — those are T1.3/T1.4.
- **Do not** add any npm/Gradle/Maven dependency before the R4 amendment is approved.
- **Do not** write iOS/web implementation for T1.2.

## Invariants (MUST)

1. **R1 boundary**: JS/TS holds mnemonic bytes only as `ArrayBuffer`; after the native call
   settles, the JS side MUST zero the input buffer with `fill(0)`. No mnemonic `string` may
   cross the JS↔native boundary in either direction.
2. **Opaque handle**: native returns a random, non-encoding handle (e.g. UUID string); the
   derived seed stays in a native-side store and is never returned to JS.
3. **R4**: no new dependency (npm or Gradle). Test dependencies for JUnit/AndroidX are NOT
   yet in `specs/tech-stack.md` — the T1.2 feature spec must amend
   `specs/tech-stack.md` + `doc/dependencies.md` and get owner approval before they are added.
4. **R5**: no `Log.*`/`console.*` call may include mnemonic bytes, word indices, seed bytes,
   or handle material that could reconstruct them. Errors must not echo input fragments.
5. **B.6**: every step records the command and its observed output in
   `specs/features/2026-10-14-T1.2-vault-core-android-bip39/validation.md`; no “done” without evidence.
6. Zeroize: every intermediate `ByteArray`/`CharArray` (normalized mnemonic, salt, PBKDF2
   buffers, entropy, checksum) MUST be `fill(0)` before leaving scope; use `PBEKeySpec.clearPassword()`.

## Preflight (Step 0)

```bash
cd /home/superguo/Projects/NexVaultRN
git status                     # clean baseline
git switch -c feature/T1.2-vault-core-android-bip39   # short-lived branch per AGENTS B.3-1/B.3-2
cd apps/mobile
npx expo-modules-autolinking search --platform android --json | grep vault-core   # must resolve
```

Record all outputs. If autolinking does not show `modules/vault-core`, stop and fix
`expo.autolinking.searchPaths` in `apps/mobile/package.json` before writing code.

## Step 1 — Pin the interface

TS declaration (`modules/vault-core/src/VaultCoreModule.ts`):

```typescript
import { NativeModule, requireNativeModule } from 'expo';

declare class VaultCoreModule extends NativeModule<{}> {
  importMnemonic(bytes: Uint8Array): Promise<string>;
}

export default requireNativeModule<VaultCoreModule>('VaultCore');
```

Public barrel (`modules/vault-core/index.ts`) keeps the R1 ArrayBuffer-only boundary:

```typescript
import VaultCoreModule from './src/VaultCoreModule';

export async function importMnemonic(mnemonicBytes: ArrayBuffer): Promise<string> {
  const view = new Uint8Array(mnemonicBytes);   // zero-copy view for the Expo JS↔native map
  try {
    return await VaultCoreModule.importMnemonic(view);
  } finally {
    view.fill(0);                                // R1: wipe after native has consumed it
  }
}
```

Expo SDK 57 type map (from the official expo-module skill): JS `Uint8Array` ↔
`kotlin.ByteArray` — this is why the wrapper passes a view over the caller's `ArrayBuffer`.

## Step 2 — Pure Kotlin BIP39/PBKDF2 core

Create `modules/vault-core/android/src/main/java/expo/modules/vaultcore/Bip39.kt` with pure,
framework-light `internal` functions so JVM tests can call them:

- `internal fun validateMnemonic(normalized: String, wordlist: List<String>): Boolean`
  - NFKD normalize (`java.text.Normalizer`), split on ASCII whitespace,
  - accept 12/15/18/21/24 words only,
  - binary-search each word in the 2048-word list,
  - reconstruct the entropy+checksum bit string with `BigInteger`/`BitSet` (264 bits MAX —
    a `Long` is NOT wide enough),
  - recompute `entropyLen = totalBits * 32 / 33`, take the first `entropyLen / 32` bits of
    `SHA-256(entropy)` and compare with the checksum bits.
- `internal fun mnemonicToSeed(normalizedMnemonic: String, passphrase: String = ""): ByteArray`
  - password = NFKD-normalized mnemonic,
  - salt = UTF-8 `"mnemonic" + passphrase`,
  - PBKDF2-HMAC-SHA512, 2048 iterations, 512-bit output,
  - prefer `SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")`,
  - on `NoSuchAlgorithmException`, fall back to a self-written PBKDF2 loop using
    `Mac.getInstance("HmacSHA512")` and `Integer.toBeBytes` blocks — no third-party code,
  - clear the `PBEKeySpec` password and every intermediate buffer.
- `internal fun loadWordlist(context: Context): List<String>`
  - load `res/raw/bip39_english.txt`, expect exactly 2048 lines; fail closed otherwise.

Embed the official English wordlist as
`modules/vault-core/android/src/main/res/raw/bip39_english.txt` (download from the
[bip-0039 English wordlist](https://raw.githubusercontent.com/bitcoin/bips/master/bip-0039/english.txt),
2048 lines, no BOM). This is data, not a dependency.

## Step 3 — Expo module wiring

In `VaultCoreModule.kt`:

```kotlin
AsyncFunction("importMnemonic") Coroutine { bytes: ByteArray ->
  withContext(Dispatchers.Default) {
    val normalized = Normalizer.normalize(String(bytes, Charsets.UTF_8), Normalizer.Form.NFKD)
    val mnemonic = normalized.trim()
    // zero `bytes` AFTER decoding; never reuse for logging
    bytes.fill(0)
    if (!Bip39.validateMnemonic(mnemonic, wordlist)) throw InvalidMnemonicException()
    val seed = Bip39.mnemonicToSeed(mnemonic, "")   // T1.3/T1.4 will consume this seed
    val handle = UUID.randomUUID().toString()
    handleStore[handle] = seed
    handle
  }
}
```

Rules:
- Store seeds in a native `ConcurrentHashMap<String, ByteArray>` owned by the module.
- Throw typed errors (`E_VAULT_INVALID_MNEMONIC`, `E_VAULT_PBKDF2_UNAVAILABLE`) with messages
  that contain no input fragments (R5).
- No `Log.*` with mnemonic/seed bytes.
- The input `ByteArray` received by the coroutine is the converted copy from JS; wipe it too.

## Step 4 — Tests (official vectors first)

Unit vectors — `modules/vault-core/android/src/test/java/expo/modules/vaultcore/Bip39Test.kt`:

- Use the first 4 English groups of
  [Trezor `vectors.json`](https://github.com/trezor/python-mnemonic/blob/master/vectors.json).
- For each: validate `mnemonic`, derive seed with PBKDF2-HMAC-SHA512, assert seed hex equals
  the vector's `seed`; assert a corrupted word / checksum fails closed.
- Test `res/raw/bip39_english.txt` contains exactly 2048 words.

Instrumentation smoke — `modules/vault-core/android/src/androidTest/java/expo/modules/vaultcore/VaultCoreModuleTest.kt`:

- `AndroidJUnit4` test calls `importMnemonic` through the module path the host app exposes;
  asserts non-empty handle and invalid input rejection. If the local-module Gradle test setup
  cannot invoke the JS bridge, the plan explicitly accepts local JVM vector tests as the
  automated gate — record that choice and the reason in `validation.md`.

Gradle (exact project path after prebuild — verify with `./gradlew projects` first):

```bash
cd apps/mobile
npx expo prebuild --platform android
./gradlew projects                       # find the vault-core project path
./gradlew :<vault-core-project>:testDebugUnitTest          # BIP39 vectors (JVM)
./gradlew :<vault-core-project>:connectedDebugAndroidTest  # instrumentation smoke (device)
```

Acceptance: all vector assertions green and the Gradle run prints `BUILD SUCCESSFUL`.

## Step 5 — Verification and handoff

```bash
cd /home/superguo/Projects/NexVaultRN
git diff -- package.json apps/mobile/package.json pnpm-lock.yaml   # prove zero new deps
grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile \
  --include='*.ts' --include='*.tsx' --exclude-dir=node_modules    # R1 CI-style check
cd apps/mobile
npx expo-doctor
npx tsc --noEmit
```

Write into `specs/features/2026-10-14-T1.2-vault-core-android-bip39/validation.md`:

- each command, exit code, observed result,
- vector-test pass/fail table (vector id → seed hex match),
- zeroization code review note (every sensitive buffer has `fill(0)`),
- Handoff items (e.g. device used for instrumentation, PBKDF2 fallback path not exercised on
  API ≥ 26 devices, test-dependency R4 amendment status).

Do not mark T1.2 `[x]` until both the automated vector gate and the zeroization review are
recorded with command + output.
