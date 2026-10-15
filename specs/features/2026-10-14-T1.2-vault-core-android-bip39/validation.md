# T1.2 — vault-core Android BIP39 (validation)

> Feature spec (validation record). Branch: `feature/T1.2-vault-core-android-bip39`.
> Status: **`[x]` verified 2026-10-15** — automated JVM vector gate (11/11), device
> instrumentation smoke (2/2 on Pixel 6a / Android 17) and the zeroization review are all
> green. No task is marked `[x]` without command + observed output below.
> Status scale (AGENTS.md B.4): `[x]` verified (both sides with evidence) · `[~]` implemented
> but unverified · `[ ]` not started.

## Planning-round evidence (2026-10-14, read-only / record-only actions)

### P0.1 — Constitution inputs

| Command | Exit | Observed result |
| --- | --- | --- |
| `git status --short` | 0 | `?? .agents/`, `?? specs/features/` (skill install artifacts predating this feature) |
| `git branch --show-current` | 0 | `main` before branching |
| read `AGENTS.md`, `specs/mission.md`, `specs/roadmap.md`, `specs/pending-manual-test-cases.md`, `specs/tech-stack.md`, `specs/vault-core.md`, `plans/3-day-implementation-plan.md`, `apps/mobile/AGENTS.md` | n/a | C.1 ask-user round Q1–Q7 answered by owner; decisions recorded in `requirements.md`; owner follow-up Q8 added the original-repo reference rule |

### P0.2 — Feature branch (owner decision Q6)

| Command | Exit | Observed result |
| --- | --- | --- |
| `git switch -c feature/T1.2-vault-core-android-bip39` | 0 | `Switched to a new branch 'feature/T1.2-vault-core-android-bip39'` |
| `git branch --show-current` | 0 | `feature/T1.2-vault-core-android-bip39` |

### P0.3 — Autolinking preflight (skill Step 0)

| Command | Exit | Observed result |
| --- | --- | --- |
| `cd apps/mobile && npx expo-modules-autolinking search --platform android --json` | 0 | `vault-core` resolved at `/home/superguo/Projects/NexVaultRN/modules/vault-core`; android module `expo.modules.vaultcore.VaultCoreModule`; `"duplicates":[]` |

### P0.4 — Official vector source verification

| Command | Exit | Observed result |
| --- | --- | --- |
| `curl -fsSL https://raw.githubusercontent.com/trezor/python-mnemonic/master/vectors.json -o /tmp/trezor_vectors.json` | 0 | downloaded |
| Python extraction of `english[0..3]` | 0 | 24 English groups; schema `[entropy, mnemonic, seed, xprv]`; first 4 seeds match BIP-0039 passphrase `"TREZOR"` by independent `hashlib.pbkdf2_hmac('sha512', NFKD(mnemonic), 'mnemonicTREZOR', 2048)` comparison |

### P0.5 — Wordlist source verification

| Command | Exit | Observed result |
| --- | --- | --- |
| `curl -fsSL https://raw.githubusercontent.com/bitcoin/bips/master/bip-0039/english.txt -o /tmp/bip39_english.txt` | 0 | downloaded |
| `wc -l /tmp/bip39_english.txt` | 0 | `2048` |
| `sha256sum /tmp/bip39_english.txt` | 0 | `2f5eed53a4727b4bf8880d8f3f199efc90e58503646d9ff8eff3a2ed3b24dbda` |
| BOM/byte check | 0 | `ASCII text`, no BOM, 13116 bytes, 2048 unique lines |

### P0.6 — Original-repo reference rule (owner Q4 + Q8)

Owner follow-up: any unclarified question NOT about React Native / TypeScript / JavaScript /
Expo may be resolved from `/home/superguo/Projects/NexVault`. Recorded in `requirements.md`
and `plan.md` (`GATE-REFERENCE-ORIGINAL-REPO`).

| Source read | Observed result |
| --- | --- |
| `/home/superguo/Projects/NexVault/AGENTS.md` | Spec-driven three-file workflow; JUnit 4 as the actual testing standard; `<module>/src/test/java/...` mirrors the main package; evidence discipline |
| `/home/superguo/Projects/NexVault/gradle/libs.versions.toml` + `specs/tech-stack.md` | JUnit `4.13.2`; `androidx.test.ext:junit` `1.3.0`; runner/rules/core 1.7.0 family |
| installed `expo-modules-core@57.0.22` `android/build.gradle` | Expo SDK 57's own tests use `junit:junit:4.13.2` and androidx.test 1.7.0 family — consistent with FR-10 |

## Implementation evidence

### Task 0 — Preflight, gates, reference log

Status: `[x]`

| Command | Exit | Observed result |
| --- | --- | --- |
| `git branch --show-current` | 0 | `feature/T1.2-vault-core-android-bip39` |
| `git status --short` | 0 | before implementation: `?? .agents/`, `?? specs/features/` |
| `cd apps/mobile && npx expo-modules-autolinking search --platform android --json` | 0 | `vault-core` resolved (`duplicates: []`) |
| `ls /home/superguo/Projects/NexVault/specs/tech-stack.md` | 0 | file present (`-rw-rw-r--`, 25332 bytes) — Q8 reference repo reachable |

- Owner answered Q7 `GATE-MODEL-SWITCH`: model switched to V4.1-Flash; implementation may begin.
- Owner approved the FR-10 R4 package exactly as listed (date 2026-10-14) in the follow-up
  ask-user round.
- Original-repo files consulted during implementation: `/home/superguo/Projects/NexVault/gradle/libs.versions.toml`
  and `specs/tech-stack.md` (R4 test-tooling versions).

### Task 1 — TS interface

Status: `[x]`

```bash
git diff -- modules/vault-core/index.ts modules/vault-core/src/VaultCoreModule.ts
grep -n 'importMnemonic' modules/vault-core/src/VaultCoreModule.ts modules/vault-core/index.ts
```

Observed:
- `src/VaultCoreModule.ts` now declares `importMnemonic(bytes: Uint8Array): Promise<string>`.
- `index.ts` replaces the placeholder with the ArrayBuffer wrapper; `view.fill(0)` runs in
  `finally` after the native call settles (R1).
- `grep` output:
  - `src/VaultCoreModule.ts:4:  importMnemonic(bytes: Uint8Array): Promise<string>;`
  - `index.ts:11:export async function importMnemonic(mnemonicBytes: ArrayBuffer): Promise<string> {`
  - `index.ts:14:    return await VaultCoreModule.importMnemonic(view);`

### Task 2 — Wordlist embedding

Status: `[x]`

```bash
wc -l modules/vault-core/android/src/main/res/raw/bip39_english.txt
sha256sum modules/vault-core/android/src/main/res/raw/bip39_english.txt \
          modules/vault-core/android/src/test/resources/bip39_english.txt
cmp -s modules/vault-core/android/src/main/res/raw/bip39_english.txt \
        modules/vault-core/android/src/test/resources/bip39_english.txt && echo IDENTICAL
```

Observed:
- `2048 modules/vault-core/android/src/main/res/raw/bip39_english.txt`
- both files `2f5eed53a4727b4bf8880d8f3f199efc90e58503646d9ff8eff3a2ed3b24dbda`
- no BOM (`head -c 3 | xxd` → `6162 61  |  aba`)
- `IDENTICAL`

### Task 3 — Bip39.kt core

Status: `[x]`

- Created `modules/vault-core/android/src/main/java/expo/modules/vaultcore/Bip39.kt`.
- Implemented `validateMnemonic`, `mnemonicToSeed`, `mnemonicToSeedUsingFallback`,
  `loadWordlist`.
- `grep -rnE 'Log\.|println'` on `src/main`/`src/test`/`src/androidTest` → `NO_LOG_CALLS`.
- JVM vector suite passed (Task 5.3), including the fallback branch.

Zeroization source references:
- `Bip39.kt:88-89` — `entropyBytes.fill(0)` + SHA-256 `digest.fill(0)` in `finally`.
- `Bip39.kt:142-143` — password `CharArray.fill('\u0000')` + salt `ByteArray.fill(0)`.
- `Bip39.kt:161-165` — encoded secret copy zeroed; `PBEKeySpec.clearPassword()`.
- `Bip39.kt:209-211` — fallback `blockInput`, `u`, `t` wiped per block.
- `Bip39.kt:215` — fallback `passwordBytes.fill(0)`.
- `Bip39.kt:228-230` — UTF-8 encoder backing buffer zeroed.

### Task 4 — Module wiring

Status: `[x]`

- `VaultCoreModule.kt:22-42` registers only `AsyncFunction("importMnemonic") Coroutine`;
  `bytes.fill(0)` runs in `finally` immediately after UTF-8 decode; seed goes only into the
  native `handleStore`; returned handle is `UUID.randomUUID().toString()`.
- `VaultExceptions.kt` defines only `E_VAULT_INVALID_MNEMONIC` and
  `E_VAULT_PBKDF2_UNAVAILABLE` with fixed messages.
- No log calls in module sources (`grep` evidence above).
- Kotlin compilation succeeded in Task 5 (`:vault-core:compileDebugKotlin` and Gradle build
  green).

### Task 5 — R4 gate, tests, mandatory vector gate

Status: `[x]` (R4 amendment + JVM automated gate + device instrumentation smoke)

Owner approval record: **2026-10-14 — owner approved the exact FR-10 package**:

- `junit:junit:4.13.2` (`testImplementation`)
- `androidx.test.ext:junit:1.3.0` (`androidTestImplementation`)
- `androidx.test:runner:1.7.0` (`androidTestImplementation`)
- `androidx.test:rules:1.7.0` (`androidTestImplementation`)
- `androidx.test:core:1.7.0` (`androidTestImplementation`)

Amendment evidence:

```bash
git diff --stat -- specs/tech-stack.md doc/dependencies.md modules/vault-core/android/build.gradle
```

Observed: `3 files changed, 30 insertions(+)`:
- `specs/tech-stack.md`: new approved row `junit / androidx.test（modules/vault-core 测试）`.
- `doc/dependencies.md`: new `§6a` evidence table + `[^23^]` source; header records the
  2026-10-14 R4 amendment.
- `modules/vault-core/android/build.gradle`: `testInstrumentationRunner` + the five test
  coordinates above, with a comment pointing at `doc/dependencies.md §6a`.

#### 5.3 — JVM vector gate (mandatory automated gate)

```bash
cd apps/mobile
npx expo prebuild --platform android --no-install
cd android
./gradlew projects
./gradlew :vault-core:testDebugUnitTest
```

Observed:
- prebuild exit 0: `✔ Created native directory`, `✔ Finished prebuild`; package.json scripts
  were updated by Expo (`expo start --android` → `expo run:android`), no dependency change.
- `./gradlew projects`: exit 0, `BUILD SUCCESSFUL in 6m 33s`; project list contains
  `\--- Project ':vault-core'`.
- `./gradlew :vault-core:testDebugUnitTest`: first run `BUILD SUCCESSFUL in 4m 18s`;
  final rerun `gradle_exit=0`, `BUILD SUCCESSFUL in 10s`.
- Test XML: `modules/vault-core/android/build/test-results/testDebugUnitTest/TEST-expo.modules.vaultcore.Bip39Test.xml`
  → `tests="11" skipped="0" failures="0" errors="0"`.

Vector result table (all PASS):

| Vector | Expected seed match | validate | seed hex | Result |
| --- | --- | --- | --- | --- |
| V1 `abandon…about` | `c55257c360c07c72029aebc1b53c05ed0362ada38ead3e3e9efa3708e53495531f09a6987599d18264c1e1c92f2cf141630c7a3c4ab7c81b2f001698e7463b04` | PASS | PASS | `[x]` |
| V2 `legal winner…yellow` | `2e8905819b8723fe2c1d161860e5ee1830318dbf49a83bd451cfb8440c28bd6fa457fe1296106559a3c80937a1c1069be3a3a5bd381ee6260e8d9739fce1f607` | PASS | PASS | `[x]` |
| V3 `letter advice…above` | `d71de856f81a8acc65e6fc851a38d4d7ec216fd0796d0a6827a3ad6ed5511a30fa280f12eb2e47ed2ac03b5c462a0358d18d69fe4f985ec81778c1b370b652a8` | PASS | PASS | `[x]` |
| V4 `zoo…wrong` | `ac27495480225222079d7be181583751e86f571027b0497b5b5d11218e0a8a13332572917f0f8e5a589620c6f15b11c61dee327651a14c34e18231052e48c069` | PASS | PASS | `[x]` |

Additional tests passed: `fallbackLoopMatchesVectorV1`,
`emptyPassphraseDerivesDistinctVectorSeed`, `emptyWordlistFailsClosed`,
`wordlistHasExactly2048Words` (11 tests total; report above).

Negative result table:

| Negative | Mutation | Expected | Result |
| --- | --- | --- | --- |
| N1 | V1 word → `zzzz` | reject | PASS (`rejectsUnknownWord`) |
| N2 | V1 `about` → `abandon` (checksum mismatch, all words valid) | reject | PASS (`rejectsChecksumMismatch`) |
| N3 | 11-word and 13-word inputs | reject | PASS (`rejectsWrongWordCount`) |

Wordlist integrity: fixture == `res/raw` (`cmp` IDENTICAL); 2048 lines; sha256 matches.

#### 5.4 — Instrumentation smoke (device: Pixel 6a / Android 17)

Status: `[x]` — clean device rerun **2026-10-15**: `Starting 2 tests … Finished 2 tests`,
`BUILD SUCCESSFUL in 1m 1s`, JUnit XML `tests="2" failures="0" errors="0"`.

Device under test (recorded for the B.4-2 real-device side): **Pixel 6a** (`bluejay`),
**Android 17** (API 37), build `google/bluejay/bluejay:17/CP2A.260705.006/15641320:user/release-keys`,
adb serial `10.42.0.139:34139` over wireless debugging.

| Command | Exit | Observed result |
| --- | --- | --- |
| `adb devices -l` | 0 | the *same* Pixel 6a visible through two adb transports: `10.42.0.139:34139` (transport_id:1) and `adb-26111jEGR13989-MJzh3R._adb-tls-connect._tcp` (transport_id:2) |
| `./gradlew :vault-core:connectedDebugAndroidTest` (2026-10-15, unpinned) | 1 | `Starting 2 tests on Pixel 6a - 17-adb-26111jEGR13989-MJzh3R._adb-tls-connect._tcp`; `Finished 2 tests` (XML `tests="2" failures="0" errors="0"`); the same device then appeared as `10.42.0.139:34139` and the duplicate install failed with `INSTALL_FAILED_DUPLICATE_PACKAGE: Duplicate package expo.modules.vaultcore.test in pending install requests` → `BUILD FAILED in 2m 55s` |
| harness root cause | n/a | One physical device was exposed through two adb transports (mDNS TLS auto-connect + direct `adb connect`), so AGP/UTP installed the test APK twice in parallel. **Neither production nor test code is at fault**; no source change was made for this |
| `ANDROID_SERIAL=10.42.0.139:34139 ./gradlew :vault-core:connectedDebugAndroidTest` (2026-10-15, single device) | 0 | `Starting 2 tests on Pixel 6a - 17`; `Finished 2 tests on Pixel 6a - 17`; `BUILD SUCCESSFUL in 1m 1s`; 110 actionable tasks: 9 executed, 101 up-to-date |
| JUnit XML artifact | 0 | `modules/vault-core/android/build/outputs/androidTest-results/connected/debug/TEST-Pixel 6a - 17-_vault-core-.xml` → `tests="2" skipped="0" failures="0" errors="0"`; `moduleDefinitionExposesImportMnemonic` 0.029s, `rawWordlistHasExactly2048Words` 0.002s |

History — first attempt 2026-10-14 (kept for the record): that run executed 2 tests; the
wordlist test passed and `moduleDefinitionExposesOnlyImportMnemonic` failed with
`expected:<1> but was:<3>`. The assertion was wrong, not production: Expo's
`ObjectDefinitionBuilder.buildObject()` adds framework-internal `startObserving`/`stopObserving`
async functions, so the async map correctly contains 3 entries. The test was renamed to
`moduleDefinitionExposesImportMnemonic` and now asserts only `containsKey("importMnemonic")`
(comment in the test records why the map is not size-pinned); after the fix
`./gradlew :vault-core:compileDebugAndroidTestKotlin` was `BUILD SUCCESSFUL in 35s`. The device
then disconnected (`adb connect 10.42.0.139:33053` → `Connection refused`) before a clean rerun.

Instrumentation source coverage: `VaultCoreModuleTest.kt` asserts the raw `res/raw` wordlist
= 2048 words and that `VaultCoreModule` definitions contain `importMnemonic`. The JS-bridge
invocation of `importMnemonic` (handle + invalid-input assertion) stays deferred to T1.3 per
`plan.md` §5.4. The 2026-10-14 correction was test-only and did not weaken production code
(AGENTS.md B.6-5).

### Task 6 — Static checks, zeroization review, Handoff

Status: `[x]`

#### Close-out rerun (2026-10-15)

| Command | Exit | Observed result |
| --- | --- | --- |
| `./gradlew :vault-core:testDebugUnitTest --rerun` | 0 | `BUILD SUCCESSFUL in 5s`; 1 executed / 56 up-to-date; XML `modules/vault-core/android/build/test-results/testDebugUnitTest/TEST-expo.modules.vaultcore.Bip39Test.xml` → `tests="11" skipped="0" failures="0" errors="0"` (`timestamp="2026-10-15T08:07:06.064Z"`) |
| R1 grep (`grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile … \| grep -v packages/vault-core`) | 1 | zero matches (clean; exit 1 = nothing found) |
| R5 grep (`console\.(log\|warn\|error\|info)\(…(mnemonic\|privKey\|secret\|seed\|sig)\)`) | 1 | zero matches (clean) |
| Kotlin log grep (`Log\.\|println` in `src/main`/`src/test`/`src/androidTest`) | 1 | zero matches (clean) |
| `git diff --stat -- package.json apps/mobile/package.json pnpm-lock.yaml` | 0 | only `apps/mobile/package.json` (+2/−2, Expo prebuild script update); root `package.json` and `pnpm-lock.yaml` untouched — zero new npm dependencies |
| `cd apps/mobile && npx --no-install tsc --noEmit` | 0 | no output (clean) |

#### Dependency-drift proof (R4)

```bash
git diff -- package.json apps/mobile/package.json pnpm-lock.yaml
```

Observed: exit 0; the only `apps/mobile/package.json` delta is Expo's prebuild script
update (`android`/`ios` → `expo run:*`). Root `package.json` and `pnpm-lock.yaml` are
untouched; **no npm dependency was added or upgraded by T1.2**. The only new dependencies
are the owner-approved FR-10 JUnit/AndroidX test coordinates in `modules/vault-core`.

#### R1/R5 greps

| Command | Exit | Observed result |
| --- | --- | --- |
| `grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile --include='*.ts' --include='*.tsx' --exclude-dir=node_modules` | 1 | zero matches (clean) |
| `grep -rnE 'console\.(log|warn|error|info)\(.*(mnemonic|privKey|secret|seed|sig)\)' apps/mobile packages --include='*.ts' --include='*.tsx'` | 1 | zero matches (clean) |
| `grep -rnE 'Log\.|println' modules/vault-core/android/src/main modules/vault-core/android/src/test modules/vault-core/android/src/androidTest` | 1 | `NO_LOG_CALLS` |

#### TypeScript / Expo checks

| Command | Exit | Observed result |
| --- | --- | --- |
| `cd apps/mobile && npx --no-install tsc --noEmit` | 0 | no output (clean) |
| ad-hoc strict typecheck of `modules/vault-core/index.ts` + `src/VaultCoreModule.ts` (temporary `/tmp/t12-tsconfig.json`, `baseUrl` + `paths: { "expo": ["./node_modules/expo"] }`) | 0 | clean; validates the R1 wrapper/declaration against the installed Expo types |
| `cd apps/mobile && npx --yes expo-doctor` | 1 | `20/21 checks passed. 1 checks failed.` — `expo expected ~57.0.29 found 57.0.28` (pre-existing patch drift; no T1.2 dependency change) |

`expo-doctor` failure disposition: **accepted only as Handoff H7**, not fixed by T1.2 because
R4 forbids an unapproved Expo upgrade. `specs/tech-stack.md` pins Expo `≥57.0.21`, so the
installed `57.0.28` is constitutional; the doctor check tracks the newest patch.

#### Zeroization review note (mandatory)

Reviewed every sensitive path with file:line evidence:

| Path | Evidence | Verdict |
| --- | --- | --- |
| Native input copy after UTF-8 decode | `VaultCoreModule.kt:24-29` `bytes.fill(0)` in `finally` | PASS |
| Password chars / salt | `Bip39.kt:131-144` `fill('\u0000')` / `fill(0)` in `finally` | PASS |
| JDK PBKDF2 encoded secret | `Bip39.kt:160-165` copy then `encoded.fill(0)`; `spec.clearPassword()` | PASS |
| Validation entropy + checksum digest | `Bip39.kt:76-90` `entropyBytes.fill(0)` + `digest?.fill(0)` in `finally` | PASS |
| Fallback PBKDF2 scratch | `Bip39.kt:193-217` `blockInput`/`u`/`t` wiped per block; `passwordBytes.fill(0)` in `finally` | PASS |
| UTF-8 encoder buffer | `Bip39.kt:224-231` backing array wiped when present | PASS |
| Module log surface | no `Log.*`; fixed error templates only | PASS |
| Seed store | native-only `ConcurrentHashMap`; never returned/logged | PASS (T1.2 scope) |

Residual **[C]** caveats (not raised to [P]): Java `String` normalized mnemonic and
`BigInteger` entropy/packed intermediates are immutable and not reliably zeroizable; HMAC
`Mac`/`SecretKeySpec` internals are only released by GC; `handleStore` seed persists for
process lifetime until T1.3/T1.4 adds `destroy` (H1).

## Handoff list (final for this round)

| # | Item | Status 2026-10-15 | Disposal |
| --- | --- | --- | --- |
| H1 | No `destroy`; handle store lives per process | `[ ]` open | Implement in T1.3/T1.4 (`destroy(handle)` + eviction) |
| H2 | `specs/vault-core.md` conflicts (Int vs UUID handle; checksum error split) | `[ ]` open | Replan to a spec-amendment round |
| H3 | Instrumentation smoke | **`[x]` closed 2026-10-15** — clean rerun on Pixel 6a / Android 17: `Starting 2 tests … Finished 2 tests`, `BUILD SUCCESSFUL in 1m 1s`, XML `tests="2" failures="0" errors="0"` (pinned with `ANDROID_SERIAL`; the unpinned failure was the duplicate-transport harness artifact in §5.4) | Keep `VaultCoreModuleTest.kt`; JS-bridge `importMnemonic` invocation stays deferred to T1.3 per plan §5.4 |
| H4 | PBKDF2 fallback coverage | `[x]` closed | `fallbackLoopMatchesVectorV1` passes |
| H5 | R4 test-dependency amendment approval | `[x]` closed | Owner approved exact package; specs/doc/build.gradle amended and evidence above |
| H6 | T1.1 scaffold debt (no eslint/jest; `packages/vault-core` absent) | `[ ]` open | Replan to T1.1 closure |
| H7 | `expo-doctor` expo `57.0.28` vs expected `~57.0.29` | `[ ]` open | T1.1 closure or owner-approved R4 patch bump; T1.2 must not upgrade |

## Status board

| Task | Status |
| --- | --- |
| Planning round | recorded — evidence P0.1–P0.6 |
| Task 0 preflight / gates / reference log | `[x]` verified |
| Task 1 TS interface | `[x]` verified |
| Task 2 wordlist | `[x]` verified |
| Task 3 Bip39.kt core | `[x]` verified by tests + zeroization review |
| Task 4 module wiring | `[x]` verified by compile + review |
| Task 5 R4 amendment + JVM vector gate | `[x]` verified |
| Task 5 instrumentation smoke | `[x]` verified — Pixel 6a / Android 17, 2/2 tests, XML `tests="2" failures="0" errors="0"` (H3 closed) |
| Task 6 static checks + zeroization + Handoff | `[x]` verified — 2026-10-15 close-out rerun: JVM 11/11, R1/R5/log greps clean, `tsc --noEmit` exit 0, zero new npm deps; `expo-doctor` patch drift carried as H7 |
| T1.2 overall | **`[x]` verified 2026-10-15** — automated JVM vector gate + real-device instrumentation smoke + zeroization review all green |

Real-device side (B.4-2): the instrumentation rerun on the physical Pixel 6a is the recorded
device evidence for this feature. The human-eyes R5 walkthrough for mnemonic/seed material
("no plaintext on disk, no log leak") is the global `MAN-009` case in
`specs/pending-manual-test-cases.md`; it depends on the end-to-end import→sign flow and
therefore remains **pending** (T1.2 does not close it and does not claim it).
