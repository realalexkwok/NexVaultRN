# T1.2 — vault-core Android BIP39 (plan)

> Feature spec (plan). Branch: `feature/T1.2-vault-core-android-bip39`.
> Parent: `plans/3-day-implementation-plan.md` Day 1 T1.2.
> Skill: `nexvault-t1-2-vault-core-android-bip39`.
> Session status: planning written 2026-10-14; implementation executed 2026-10-14 and closed
> out **2026-10-15** with all commands/results recorded in `validation.md` (overall T1.2
> `[x]` — JVM vector gate 11/11 + real-device instrumentation 2/2 on Pixel 6a / Android 17).

## Hard gates (read before any work)

- **GATE-MODEL-SWITCH** — the implementation session MUST stop here and let the owner switch
  the model to **V4.1-Flash** before writing code. This planning session writes no code.
- **GATE-R4-TEST-DEPS** — JUnit/AndroidX test coordinates are NOT in `specs/tech-stack.md`.
  Tasks 1–4 may proceed because they add no dependency, but **Task 5's dependency edits and
  the vector-test run are blocked until the owner approves the FR-10 amendment package and the
  approval is recorded in `validation.md` Task 5**.
- **GATE-REFERENCE-ORIGINAL-REPO (owner rule Q8)** — any question these specs/skill leave open
  whose topic is NOT React Native / TypeScript / JavaScript / Expo is resolved from the original
  NexVault repo at **`/home/superguo/Projects/NexVault`** (`AGENTS.md`, `specs/tech-stack.md`,
  `specs/dev-environment.md`, `gradle/libs.versions.toml`, existing source/tests). RN/TS/JS/Expo
  questions use this repo's `AGENTS.md` + `apps/mobile/AGENTS.md` + the T1.2 skill + SDK 57 docs.
  This repo's R1–R8/R4 and this feature spec win on conflict; record the consulted original-repo
  file in `validation.md` whenever this rule is used; if still unresolved, stop and ask the owner.
- No `deriveAddress` / `signDigest` / `destroy`; Android only; no iOS/web implementation.
- No dependency outside the FR-10 package, and only after owner approval.
- **Status 2026-10-14:** both gates passed; owner approved FR-10; discovered Gradle project
  path is `:vault-core`. See `validation.md` for commands and observed results.

## Task 0 — Preflight and baseline recording

Commands (record command + exit code + observed output in `validation.md` Task 0):

```bash
cd /home/superguo/Projects/NexVaultRN
git status --short                    # planning note: .agents/ and specs/features/ untracked from skill install
git branch --show-current             # must be feature/T1.2-vault-core-android-bip39 (created in planning round)
git switch -c feature/T1.2-vault-core-android-bip39   # only if the branch is missing; baseline = main
cd apps/mobile
npx expo-modules-autolinking search --platform android --json | grep -o '"vault-core":{[^}]*}' | head -c 400
ls /home/superguo/Projects/NexVault/specs/tech-stack.md   # Q8 reference repo must be reachable
```

Acceptance:
- Branch name matches the B.3 regex `feature/T1.2-vault-core-android-bip39`.
- Autolinking resolves `modules/vault-core` (planning-round observed result: it resolved,
  version `""`, no duplicates). If it ever stops resolving, fix
  `apps/mobile/package.json` → `expo.autolinking.searchPaths` before code.
- `/home/superguo/Projects/NexVault/specs/tech-stack.md` exists (Q8 reference repo reachable);
  record in `validation.md` every original-repo file consulted during the implementation.
- No `android/` hand-editing (CNG): generated dirs are only produced by
  `npx expo prebuild --platform android`.

## Task 1 — Pin the TS interface (no dependency)

Files:

1. `modules/vault-core/src/VaultCoreModule.ts`

```typescript
import { NativeModule, requireNativeModule } from 'expo';

declare class VaultCoreModule extends NativeModule<{}> {
  importMnemonic(bytes: Uint8Array): Promise<string>;
}

export default requireNativeModule<VaultCoreModule>('VaultCore');
```

2. `modules/vault-core/index.ts` (replaces placeholder barrel; keeps the R1
   ArrayBuffer-only boundary)

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

Rules:
- Handle type is `string` (UUID) — owner decision Q2; do not introduce `VaultHandle = number`.
- Do not touch `VaultCoreModule.web.ts` (web stub stays; R7) and do not implement any other
  method.
- Record `git diff -- modules/vault-core/index.ts modules/vault-core/src/VaultCoreModule.ts`
  and a `grep -n 'importMnemonic'` result in `validation.md`.

## Task 2 — Embed the official English wordlist (data, not dependency)

1. Download to the exact path:

```bash
curl -fsSL --max-time 30 https://raw.githubusercontent.com/bitcoin/bips/master/bip-0039/english.txt \
  -o modules/vault-core/android/src/main/res/raw/bip39_english.txt
```

2. Verify and record:

```bash
wc -l modules/vault-core/android/src/main/res/raw/bip39_english.txt     # 2048
sha256sum modules/vault-core/android/src/main/res/raw/bip39_english.txt # 2f5eed53a4727b4bf8880d8f3f199efc90e58503646d9ff8eff3a2ed3b24dbda
head -c 3 modules/vault-core/android/src/main/res/raw/bip39_english.txt | xxd   # no BOM
```

3. Create the JVM test fixture byte-identical to the production resource (no dependency):

```bash
mkdir -p modules/vault-core/android/src/test/resources
cp modules/vault-core/android/src/main/res/raw/bip39_english.txt \
   modules/vault-core/android/src/test/resources/bip39_english.txt
cmp -s modules/vault-core/android/src/main/res/raw/bip39_english.txt \
        modules/vault-core/android/src/test/resources/bip39_english.txt && echo IDENTICAL
```

Acceptance: 2048 lines, expected SHA-256, `cmp` exit 0. Record all outputs in
`validation.md` Task 2. (The test fixture exists so plain-JVM `Bip39Test` needs no
Robolectric/Context; the `cmp` gate proves it is the same bytes as `res/raw`.)

## Task 3 — Pure Kotlin BIP39/PBKDF2 core

Create `modules/vault-core/android/src/main/java/expo/modules/vaultcore/Bip39.kt`
(framework-light `internal` functions; no `android.*` import except the small
`loadWordlist(context)` helper).

Signatures:

```kotlin
internal fun validateMnemonic(normalized: String, wordlist: List<String>): Boolean
internal fun mnemonicToSeed(normalizedMnemonic: String, passphrase: String = ""): ByteArray
internal fun loadWordlist(context: Context): List<String>
```

Algorithm checklist (must):

- **validateMnemonic** — NFKD normalize (`java.text.Normalizer`); split on ASCII whitespace;
  accept only 12/15/18/21/24 words; binary-search (`Collections.binarySearch` or hand-rolled)
  each word in the 2048-word list; `totalBits = wordCount * 11`; `entropyBits = totalBits * 32 / 33`;
  `checksumBits = totalBits - entropyBits`; reconstruct the bit string with
  `BigInteger`/`BitSet` (264 bits max — never `Long`); `entropyLen = entropyBits / 8`;
  compare the first `checksumBits` bits of `SHA-256(entropy)` with the trailing checksum bits.
  Wipe `entropy` and checksum buffers (`fill(0)`) before return.
- **mnemonicToSeed** — password = NFKD mnemonic as `CharArray` for `PBEKeySpec`; salt =
  UTF-8 `"mnemonic" + passphrase`; 2048 iterations; 512-bit output.
  - Preferred: `SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")`,
    `PBEKeySpec(password, saltBytes, 2048, 512)`, then `secret.encoded` copy.
  - Fallback on `NoSuchAlgorithmException`: self-written loop over
    `Mac.getInstance("HmacSHA512")`, 4-byte big-endian block counter, standard PBKDF2
    U1/U2 XOR construction — no third-party code.
  - `PBEKeySpec.clearPassword()`; wipe `saltBytes`, password chars, all PBKDF2 scratch
    buffers, and the `SecretKey`/encoded copies.
- **loadWordlist** — read `R.raw.bip39_english`; expect exactly 2048 non-blank lines;
  throw/return-fail-closed otherwise (surfaced as `E_VAULT_INVALID_MNEMONIC` or an internal
  fail-closed error at module level — no word fragments in the message).
- No `Log.*` anywhere in this file. Error/exception text must never interpolate input words,
  indices, entropy, checksum, seed, or salt.

Record the file's diff and the zeroization checklist in `validation.md` Task 3.

## Task 4 — Expo module wiring

Files:

1. `modules/vault-core/android/src/main/java/expo/modules/vaultcore/VaultCoreModule.kt`
   (replace the empty scaffold definition):

```kotlin
AsyncFunction("importMnemonic") Coroutine { bytes: ByteArray ->
  withContext(Dispatchers.Default) {
    val normalized = Normalizer.normalize(String(bytes, Charsets.UTF_8), Normalizer.Form.NFKD)
    val mnemonic = normalized.trim()
    bytes.fill(0)                        // wipe the converted copy AFTER decoding; never reuse for logging
    if (!Bip39.validateMnemonic(mnemonic, wordlist)) throw InvalidMnemonicException()
    val seed = Bip39.mnemonicToSeed(mnemonic, "")   // T1.3/T1.4 consume this seed
    val handle = UUID.randomUUID().toString()
    handleStore[handle] = seed
    handle
  }
}
```

Module rules:
- `private val handleStore = ConcurrentHashMap<String, ByteArray>()`.
- `private val wordlist: List<String> by lazy { Bip39.loadWordlist(requireNotNull(appContext.reactContext)) }`
  (deferred until after module creation).
- Throwing helpers use `CodedException(code, message, cause)` from
  `expo.modules.kotlin.exception.CodedException` (SDK 57 API verified during planning):
  - `InvalidMnemonicException`: code `E_VAULT_INVALID_MNEMONIC`, fixed message
    `"Invalid mnemonic"`.
  - `Pbkdf2UnavailableException`: code `E_VAULT_PBKDF2_UNAVAILABLE`, fixed message
    `"PBKDF2-HMAC-SHA512 is unavailable on this device"`.
- Never log `bytes`, `mnemonic`, `seed`, or `handle`.
- Do not add `deriveAddress`/`signDigest`/`destroy`.

2. Optional small file `modules/vault-core/android/src/main/java/expo/modules/vaultcore/VaultExceptions.kt`
   for the two exception classes (or put them in the module file — plan keeps them separate
   for reviewability).

Acceptance: `git diff` shows only the intended files; no dependency lines in
`modules/vault-core/android/build.gradle` yet.

## Task 5 — GATE-R4-TEST-DEPS: amendment approval, then tests

### 5.1 Owner approval (blocking)

- Present the FR-10 amendment package to the owner and record the approval in
  `validation.md` Task 5 (date, owner, exact scope).
- Only after approval: amend `specs/tech-stack.md` (add a JUnit 4.13.2 + AndroidX test
  1.3.0/1.7.0 row under §2/测试工具) and `doc/dependencies.md` (versions, purpose, CVE
  disposition, trust basis — original NexVault repo reference + Expo SDK 57 own-test-usage
  reference). This is a constitution amendment, not a research edit.
- If approval is not given, stop: Tasks 5.2+ and Task 6's vector gate stay blocked, and the
  blocker is recorded in `validation.md`.

### 5.2 Add test dependencies

`modules/vault-core/android/build.gradle`:

```kotlin
dependencies {
  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.3.0")
  androidTestImplementation("androidx.test:runner:1.7.0")
  androidTestImplementation("androidx.test:rules:1.7.0")
  androidTestImplementation("androidx.test:core:1.7.0")
}
```

Also add `testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"` to
`android.defaultConfig` if the prebuild host app/module doesn't already supply one.

### 5.3 JVM vector tests (mandatory automated gate)

Create
`modules/vault-core/android/src/test/java/expo/modules/vaultcore/Bip39Test.kt`
(layout mirrors `src/main/java`, per the original NexVault repo's non-RN convention).

Test methods (each `// TC-VC-XXX` marker directly above `@Test`):

| Test | Marker | Assertion |
| --- | --- | --- |
| `vectorV1_abandonAbout` | `// TC-VC-001` | validate true; `mnemonicToSeed(mnemonic, "TREZOR")` hex == V1 seed |
| `vectorV2_legalWinner` | `// TC-VC-001` | validate true; seed hex == V2 seed |
| `vectorV3_letterAdvice` | `// TC-VC-001` | validate true; seed hex == V3 seed |
| `vectorV4_zooWrong` | `// TC-VC-001` | validate true; seed hex == V4 seed |
| `rejectsUnknownWord` | `// TC-VC-002` | replace one word with `zzzz` → validate false |
| `rejectsChecksumMismatch` | `// TC-VC-002` | V1 with `about` → `abandon` (valid words, bad checksum) → validate false |
| `rejectsWrongWordCount` | `// TC-VC-002` | 11/13 words → validate false |
| `wordlistHasExactly2048Words` | `// TC-VC-010` | load `src/test/resources/bip39_english.txt` → exactly 2048 words |
| `emptyWordlistFailsClosed` | `// TC-VC-010` | an empty wordlist cannot validate even a correct mnemonic |
| `fallbackLoopMatchesVectorV1` | `// TC-VC-001` | `mnemonicToSeedUsingFallback(mnemonic, "TREZOR")` hex == V1 seed |
| `emptyPassphraseDerivesDistinctSeed` | `// TC-VC-001` | V1 with passphrase `""` is a different 64-byte hex than V1 TREZOR seed (locks the production empty-passphrase behaviour without exposing it) |

Vector data is embedded in the test file (mnemonic, passphrase `"TREZOR"`, expected seed hex).
The production wordlist is not loaded through Android `Context` in JVM tests; the byte-identical
fixture + Task 2 `cmp` gate covers the data integrity.

### 5.4 Instrumentation smoke (best-effort, secondary)

Create
`modules/vault-core/android/src/androidTest/java/expo/modules/vaultcore/VaultCoreModuleTest.kt`:

- `@RunWith(AndroidJUnit4::class)`; `// TC-VC-012`.
- Wordlist via `ApplicationProvider.getApplicationContext<Context>()` → 2048 words
  (exercises the real `res/raw`).
- `VaultCoreModule().definition()` contains `importMnemonic` (the async map also carries
  Expo's framework-internal `startObserving`/`stopObserving`, so it is not size-pinned).
- The JS-bridge `importMnemonic` invocation / invalid-input assertion is deferred to T1.3
  (Handoff H3). A 2026-10-14 Pixel 6a run executed both tests; the earlier size assertion was
  corrected as a test-side bug (see `validation.md` §5.4). **Closed 2026-10-15:** clean rerun on
  Pixel 6a / Android 17 → `Starting 2 tests … Finished 2 tests`, `BUILD SUCCESSFUL in 1m 1s`,
  XML `tests="2" failures="0" errors="0"` (device pinned via `ANDROID_SERIAL` because the same
  phone was visible through two adb transports).
- If a future device run cannot invoke the JS bridge/module path, do not force it: record the
  reason in `validation.md` and the accepted automated gate is the JVM vector suite (Q5).

## Task 6 — Verification and handoff recording

Generate native project (CNG; no hand edits) and run gates:

```bash
cd apps/mobile
npx expo prebuild --platform android --no-install   # generates android/ from app.json; do not edit by hand
cd android
./gradlew projects                                  # discovered :vault-core
./gradlew :vault-core:testDebugUnitTest             # mandatory vector gate — BUILD SUCCESSFUL 2026-10-14
./gradlew :vault-core:connectedDebugAndroidTest     # best-effort; Pixel 6a clean rerun 2026-10-15 → BUILD SUCCESSFUL, 2/2 tests (pin ANDROID_SERIAL if adb lists the device twice)
./gradlew :vault-core:compileDebugAndroidTestKotlin # compile-only fallback — BUILD SUCCESSFUL 2026-10-14
```

Then the security/quality checks:

```bash
cd /home/superguo/Projects/NexVaultRN
git diff -- package.json apps/mobile/package.json pnpm-lock.yaml   # prove zero new deps
grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile \
  --include='*.ts' --include='*.tsx' --exclude-dir=node_modules | grep -v 'packages/vault-core' && exit 1
grep -rnE 'console\.(log|warn|error|info)\(.*(mnemonic|privKey|secret|seed|sig)\)' \
  apps/mobile packages --include='*.ts' --include='*.tsx' && exit 1
cd apps/mobile
npx expo-doctor
npx tsc --noEmit
```

Zeroization review (must be recorded in `validation.md` Task 6, item by item):
input `ByteArray` wipe, NFKD byte buffer, `PBEKeySpec` password + `clearPassword()`, salt
bytes, fallback-loop scratch buffers, entropy/checksum buffers, seed store policy (no
persistence, process-lifetime only in T1.2). State the [C] caveats from requirements.

Write `validation.md` Task 6 evidence and the final Handoff list. T1.2 is never marked
`[x]` unless the automated vector gate output and the zeroization review note are both
recorded with command + output (AGENTS.md B.4-3).

## File manifest (T1.2 implementation)

| File | Action |
| --- | --- |
| `modules/vault-core/src/VaultCoreModule.ts` | modify (add method declaration) |
| `modules/vault-core/index.ts` | replace placeholder barrel |
| `modules/vault-core/android/src/main/java/expo/modules/vaultcore/Bip39.kt` | create |
| `modules/vault-core/android/src/main/java/expo/modules/vaultcore/VaultExceptions.kt` | create |
| `modules/vault-core/android/src/main/java/expo/modules/vaultcore/VaultCoreModule.kt` | replace scaffold |
| `modules/vault-core/android/src/main/res/raw/bip39_english.txt` | create (2048 lines, no BOM) |
| `modules/vault-core/android/src/test/resources/bip39_english.txt` | create (byte-identical fixture) |
| `modules/vault-core/android/src/test/java/expo/modules/vaultcore/Bip39Test.kt` | create (after GATE-R4) |
| `modules/vault-core/android/src/androidTest/java/expo/modules/vaultcore/VaultCoreModuleTest.kt` | create (after GATE-R4, best-effort) |
| `modules/vault-core/android/build.gradle` | modify (test deps, after GATE-R4) |
| `specs/tech-stack.md`, `doc/dependencies.md` | amend (after GATE-R4, exact FR-10 rows) |
| `specs/features/2026-10-14-T1.2-vault-core-android-bip39/validation.md` | fill every task |

## Risks

| Risk | Trigger | Response |
| --- | --- | --- |
| R4 approval not granted | Owner blocks FR-10 package | Halt at GATE-R4; record blocker; vector gate stays red-blocked (never fake evidence) |
| Gradle project path differs after prebuild | `./gradlew projects` output | Use the discovered path verbatim in validation.md |
| Plain-JVM test cannot read `src/test/resources` layout | test failure on fixture | Use absolute-classpath resource loader; record fix |
| Instrumentation bridge unavailable | module path can't be invoked from androidTest | Accept JVM vector gate as automated gate (Q5), record reason, add Handoff H3 |
| Prebuild creates untracked `android/` | `git status` noise | Do not hand-edit; record; never commit generated native dirs unless the repo policy changes |
| R1 grep false positives in future T1.3 code | CI grep hits | Keep T1.2 surface minimal; verify grep scope before flagging |
