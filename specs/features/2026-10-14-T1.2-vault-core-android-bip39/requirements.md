# T1.2 — vault-core Android BIP39 (requirements)

> Feature spec (requirements). Planning round **2026-10-14**. Branch:
> `feature/T1.2-vault-core-android-bip39` (created from `main` before this file was written).
> Parent roadmap item: `plans/3-day-implementation-plan.md` Day 1 T1.2（关键路径）.
> Skill: `nexvault-t1-2-vault-core-android-bip39`（also copied in this feature directory as `SKILL.md`）。
> Sibling files: `plan.md`, `validation.md`.
> Session status: planning round **2026-10-14** wrote this record; a later session implemented
> T1.2 with the owner-approved V4.1-Flash switch, and the round closed **2026-10-15**. Evidence
> is in `validation.md`; overall T1.2 is **`[x]` verified** (JVM vector gate 11/11, real-device
> instrumentation 2/2 on Pixel 6a / Android 17, zeroization review). Remaining Handoffs are
> non-T1.2 items (H1/H2/H6/H7).

## Goal

Implement **only** `importMnemonic(mnemonicBytes: ArrayBuffer) → handle` in
`modules/vault-core` as an Android/Kotlin Expo local module:

- self-written BIP-39 English validation (12/15/18/21/24 words, checksum check);
- self-written PBKDF2-HMAC-SHA512 seed derivation (2048 iterations, 512-bit output);
- official BIP-39 English wordlist embedded as `res/raw` data;
- opaque, random, non-encoding handle (UUID string); the derived seed stays in a
  native-side store and is **never** returned to JS;
- R1 ArrayBuffer-only boundary with JS-side `fill(0)` after the native call settles;
- first-4 official BIP39 vectors as the automated gate.

## Owner decisions (2026-10-14, C.1 ask-user round)

| Q | Question | Decision |
| --- | --- | --- |
| Q1 | Scope | **Skill scope is authoritative**: only `importMnemonic(ArrayBuffer)→handle` on Android/Kotlin. No `deriveAddress`/`signDigest`/`destroy`, no iOS/web implementation, zero new npm/Gradle/Maven dependencies before the R4 amendment is approved. |
| Q2 | Handle type conflict | **UUID `string` per the skill.** T1.2 pins `handle: string` (UUID, random, non-encoding). This owner decision supersedes the conflicting draft `specs/vault-core.md` §2.1/§2.2 (branded `number`/`Int` handle); reconciliation is tracked in Handoff H2, not done inside T1.2 implementation. |
| Q3 | Vector gate | **Embed the first-4 official BIP-0039 vectors (TREZOR passphrase).** The automated gate calls the pure Kotlin `mnemonicToSeed(normalized, "TREZOR")` directly. Module-level `importMnemonic` tests assert only handle/invalid-input behaviour because the seed never leaves native. |
| Q4 | R4 test dependencies | **Application of the Q8 reference rule to non-React-Native/non-Expo test tooling**: versions and conventions come from `/home/superguo/Projects/NexVault` (JUnit 4.13.2 as the standard, AndroidX test 1.3.0/1.7.0 family, test-layout mirrors source package). This produces the R4 amendment package in FR-10; **owner approval is still required before any JUnit/AndroidX test dependency is added to this repo.** |
| Q5 | Test layers | **JVM vector tests (`testDebugUnitTest`) are the mandatory automated gate.** Instrumentation smoke (`connectedDebugAndroidTest`) is best-effort/secondary and becomes a Handoff item if no device/bridge path is available. |
| Q6 | Git worktree | **Create the feature branch before writing spec files.** `feature/T1.2-vault-core-android-bip39` is created; no commit is required in the planning session. |
| Q7 | Model-switch stop | **Confirmed.** This session plans and writes the three spec files only — no code, no prebuild. Implementation starts in a later session only after the owner switches the model to V4.1-Flash (`GATE-MODEL-SWITCH`). |
| Q8 | Unclarified-question reference rule (owner follow-up before implementation) | **Any question this spec or the skill leaves open whose topic is NOT React Native / TypeScript / JavaScript / Expo (or another RN/Expo ecosystem concern) is resolved by referring to the original NexVault repo at `/home/superguo/Projects/NexVault`.** React Native / TypeScript / JavaScript / Expo-specific questions remain governed by this repo's `AGENTS.md` + `apps/mobile/AGENTS.md` + the T1.2 skill + Expo SDK 57 versioned docs. This repo's red lines (R1–R8, R4) always win on conflict. |

## Reference precedence for unclarified questions (owner rule, 2026-10-14)

1. **RN/TS/JS/Expo topics** (Expo Modules API, autolinking, Metro, TS strict, JS↔native
   typing, SDK 57 behaviour, etc.): this repo's `AGENTS.md`, `apps/mobile/AGENTS.md`, the T1.2
   skill, and the Expo SDK 57 versioned docs are authoritative.
2. **All other topics** (Kotlin, Gradle, JUnit/AndroidX testing, native BIP39/PBKDF2 details,
   test layout, quality conventions, toolchain choices, etc.): resolve by referring to the
   original NexVault repo at **`/home/superguo/Projects/NexVault`** — its `AGENTS.md`,
   `specs/tech-stack.md`, `specs/dev-environment.md`, `gradle/libs.versions.toml`, and existing
   source/tests. When this rule is used, the implementation record must name the exact
   original-repo file consulted in `validation.md`.
3. **Conflict resolution**: if the original-repo reference conflicts with this repo's
   `AGENTS.md` R1–R8/R4, the T1.2 skill, or an explicit owner decision in this feature spec,
   this repo wins (for example: the original repo's “agents must not commit” rule is superseded
   by this repo's B.3-3; the original repo's kethereum BIP39 dependency is not adopted because
   T1.2 requires self-written BIP39/PBKDF2 and R4 forbids unapproved dependencies).
4. **Still unresolved?** Stop and ask the owner before writing code — never invent an answer.

## Handoff re-read (AGENTS.md B.7 — every pre-existing candidate gets an explicit verdict)

| Source | Deferred entry | Verdict for T1.2 |
| --- | --- | --- |
| AGENTS.md B.7-3 | iOS mlock 无语义保证（known limitation） | **Replan → T1.5** (iOS importMnemonic port). Not exercised by Android-only T1.2. |
| AGENTS.md B.7-3 | native UI 自定义词格键盘完整版 | **Replan → roadmap stage 2 item 9 / UI wallet feature.** T1.2 keeps the skill's ArrayBuffer byte path; the full keyboard is out of scope. |
| AGENTS.md B.7-3 | vault-core C++ 化（libsecp256k1 经 CMake） | **Replan → roadmap stage 2 item 2.** T1.2 must not preempt it. |
| AGENTS.md B.7-3 | Secure Enclave P-256 包裹（v2） | **Replan → roadmap stage 2 item 8.** Not in T1.2. |
| `specs/vault-core.md` §7 | BIP-39 passphrase (25th word) | **Replan → roadmap stage 2 item 9.** T1.2 `importMnemonic` derives with empty passphrase; passphrase only appears inside test vectors (Q3). |

## Functional requirements

- **FR-1 — Interface and R1 boundary.** `modules/vault-core/src/VaultCoreModule.ts` declares
  `importMnemonic(bytes: Uint8Array): Promise<string>`; `modules/vault-core/index.ts` exports
  `importMnemonic(mnemonicBytes: ArrayBuffer): Promise<string>` and zero-copies the caller buffer
  into a `Uint8Array` view, then runs `view.fill(0)` in `finally` after the native call settles.
  No mnemonic `string` crosses the JS↔native boundary in either direction.
- **FR-2 — Opaque handle.** Native returns `UUID.randomUUID().toString()`; the handle encodes
  nothing. The derived 64-byte seed lives in a module-owned `ConcurrentHashMap<String, ByteArray>`
  and is never returned to JS, logged, or serialized. T1.2 has no `destroy`, so entries persist
  for the process lifetime until T1.3/T1.4 adds destruction — recorded as an accepted interim
  limitation in Handoff H1.
- **FR-3 — BIP-39 validation.** `Bip39.kt` exposes
  `internal fun validateMnemonic(normalized: String, wordlist: List<String>): Boolean`:
  NFKD normalize, split on ASCII whitespace, accept exactly 12/15/18/21/24 words, binary-search
  each word in the 2048-entry list, reconstruct the `totalBits = words*11` bit string with
  `BigInteger`/`BitSet` (264 bits max — never a `Long`), recompute
  `entropyLen = totalBits * 32 / 33`, and compare the first `entropyLen/32` bits of
  `SHA-256(entropy)` with the checksum bits. Fail closed on any mismatch.
- **FR-4 — Seed derivation.** `internal fun mnemonicToSeed(normalizedMnemonic: String,
  passphrase: String = ""): ByteArray`: password = NFKD mnemonic, salt = UTF-8
  `"mnemonic" + passphrase`, PBKDF2-HMAC-SHA512, 2048 iterations, 512-bit output. Prefer
  `SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")`; on `NoSuchAlgorithmException`, fall back
  to a self-written PBKDF2 loop over `Mac.getInstance("HmacSHA512")` with a 4-byte big-endian
  block counter. `PBEKeySpec.clearPassword()` and every intermediate `ByteArray`/`CharArray`
  must be wiped (`fill(0)`). `importMnemonic` itself calls `mnemonicToSeed(normalized, "")`.
- **FR-5 — Wordlist data.** `modules/vault-core/android/src/main/res/raw/bip39_english.txt`
  from the official BIP-0039 English wordlist: exactly 2048 lines, no BOM, data not dependency.
  `internal fun loadWordlist(context: Context): List<String>` fails closed unless exactly
  2048 words are loaded. Planning-time reference bytes: 13116 bytes,
  SHA-256 `2f5eed53a4727b4bf8880d8f3f199efc90e58503646d9ff8eff3a2ed3b24dbda`.
- **FR-6 — Module wiring.** `VaultCoreModule.kt` registers
  `AsyncFunction("importMnemonic") Coroutine { bytes: ByteArray -> ... }`, runs the work on
  `Dispatchers.Default`, NFKD-normalizes, `trim()`s, wipes the received `bytes` after UTF-8
  decode, validates against the loaded wordlist, derives and stores the seed, and returns the
  UUID handle. The wordlist is loaded lazily from `appContext.reactContext` after module
  creation.
- **FR-7 — Zeroization invariant.** Every sensitive intermediate (`ByteArray`/`CharArray`:
  input copy, normalized bytes, salt, PBKDF2 buffers, entropy, checksum) is `fill(0)` before
  leaving scope; `PBEKeySpec.clearPassword()` is called. JVM `String` intermediates are
  unavoidable for BIP-39 word processing and are not zeroizable — this is labelled **[C]**,
  never [P] (R6); compensating controls are short lifetime, no logging, no persistence.
- **FR-8 — Typed errors and R5 log red line.** Only two error codes are exposed:
  `E_VAULT_INVALID_MNEMONIC` (word not in list / illegal word count / checksum mismatch — all
  fail closed) and `E_VAULT_PBKDF2_UNAVAILABLE` (both provider and self-written fallback
  unavailable). Error messages are fixed templates with no input fragments; no `Log.*` call
  contains mnemonic bytes, word indices, seed bytes, or handle material. This supersedes the
  draft `E_VAULT_CHECKSUM_MISMATCH` split in `specs/vault-core.md` §2.4 for T1.2 (Handoff H2).
- **FR-9 — Tests and vector gate.** `modules/vault-core/android/src/test/java/expo/modules/vaultcore/Bip39Test.kt`
  (JVM, JUnit 4, test-layout mirroring the source package — per the original NexVault repo's
  non-RN test convention) is the mandatory automated gate. It must:
  1. assert all first-4 official vectors below: `validateMnemonic(...)` true and
     `mnemonicToSeed(normalized, "TREZOR")` hex equals the vector seed;
  2. assert corrupted-word and checksum-mismatch negatives fail closed;
  3. assert the wordlist fixture contains exactly 2048 words (and validation gate proves the
     fixture byte-identical to `res/raw/bip39_english.txt`).
  Instrumentation smoke `VaultCoreModuleTest.kt` (best-effort) asserts non-empty handle and
  invalid-input rejection. Each mapped test method carries a `// TC-VC-XXX` comment above its
  `@Test` (AGENTS.md B.2-3).
- **FR-10 — R4 dependency amendment package (owner approval required).** No dependency is added
  before owner approval. The exact proposal, with versions referenced from
  `/home/superguo/Projects/NexVault/gradle/libs.versions.toml` and Expo SDK 57's own test
  usage, is:

  | Coordinate | Configuration | Version | Purpose |
  | --- | --- | --- | --- |
  | `junit:junit` | `testImplementation` | 4.13.2 | JVM BIP39 vector gate (original repo standard) |
  | `androidx.test.ext:junit` | `androidTestImplementation` | 1.3.0 | AndroidJUnit4 instrumentation smoke |
  | `androidx.test:runner` | `androidTestImplementation` | 1.7.0 | Instrumentation runner |
  | `androidx.test:rules` | `androidTestImplementation` | 1.7.0 | Test rules, if needed |
  | `androidx.test:core` | `androidTestImplementation` | 1.7.0 | `ApplicationProvider`/instrumentation context, if needed |

  Owner approval must be recorded in `validation.md`; only then may
  `specs/tech-stack.md` and `doc/dependencies.md` be amended with these rows (doc amendment is
  a constitution amendment action, not a research correction) and the coordinates added to
  `modules/vault-core/android/build.gradle`.
- **FR-11 — Verification evidence.** Every implementation step records the command, exit code,
  and observed output in `validation.md` (AGENTS.md B.4/B.6). Mandatory final checks: vector
  gate green (`BUILD SUCCESSFUL`), zeroization code-review note, zero-new-deps diff
  (`package.json`, `apps/mobile/package.json`, `pnpm-lock.yaml`), R1 CI-style grep, R5 log
  grep, `npx expo-doctor`, `npx tsc --noEmit`. No `[x]` or “done” without command + output.
- **FR-12 — Implementation-session stop contract.** The planning session ends after writing
  the three spec files. `plan.md` Task 0 begins with `GATE-MODEL-SWITCH`: the implementation
  session must stop and let the owner switch the model to V4.1-Flash before any code is written.

## Official BIP39 vectors — first 4 (automated gate)

Source: BIP-0039 specification test vectors as mirrored by Trezor
`vectors.json` (fetched 2026-10-14; current schema `[entropy, mnemonic, seed, xprv]`; the seed
field matches the BIP-0039 rows with passphrase **`TREZOR`**). These four rows are embedded
verbatim in test code and in `validation.md`.

| # | Entropy (hex) | Mnemonic (space-separated) | Seed expected with passphrase `"TREZOR"` (hex) |
| --- | --- | --- | --- |
| V1 | `00000000000000000000000000000000` | `abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about` | `c55257c360c07c72029aebc1b53c05ed0362ada38ead3e3e9efa3708e53495531f09a6987599d18264c1e1c92f2cf141630c7a3c4ab7c81b2f001698e7463b04` |
| V2 | `7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f` | `legal winner thank year wave sausage worth useful legal winner thank yellow` | `2e8905819b8723fe2c1d161860e5ee1830318dbf49a83bd451cfb8440c28bd6fa457fe1296106559a3c80937a1c1069be3a3a5bd381ee6260e8d9739fce1f607` |
| V3 | `80808080808080808080808080808080` | `letter advice cage absurd amount doctor acoustic avoid letter advice cage above` | `d71de856f81a8acc65e6fc851a38d4d7ec216fd0796d0a6827a3ad6ed5511a30fa280f12eb2e47ed2ac03b5c462a0358d18d69fe4f985ec81778c1b370b652a8` |
| V4 | `ffffffffffffffffffffffffffffffff` | `zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong` | `ac27495480225222079d7be181583751e86f571027b0497b5b5d11218e0a8a13332572917f0f8e5a589620c6f15b11c61dee327651a14c34e18231052e48c069` |

Negative gate (mandatory, all must fail closed):

| # | Input mutation on V1 | Expected |
| --- | --- | --- |
| N1 | Replace a word with a non-wordlist token (e.g. `zzzz`) | `E_VAULT_INVALID_MNEMONIC`; message contains no input fragment |
| N2 | Replace last word `about` with wordlist word `abandon` (valid word, checksum now wrong) | `E_VAULT_INVALID_MNEMONIC`; message contains no input fragment |
| N3 | Wrong word count (e.g. 11 or 13 words) | `E_VAULT_INVALID_MNEMONIC` |

## Out of scope

- `deriveAddress` / `signDigest` / `destroy` (T1.3/T1.4).
- iOS and web implementation; web keeps its existing stub module (R7).
- BIP-39 passphrase input in the production path (empty passphrase only).
- BIP-32/44, secp256k1, BouncyCastle, libsecp256k1, Keystore/SecureStore wrap.
- Any npm/Gradle/Maven dependency not approved by FR-10; `@scure/*`/`@noble/*` are not used by
  this feature (they remain test-only TS helpers elsewhere).
- Seed export, seed persistence, logging, or any path that returns seed bytes to JS.

## Acceptance criteria (each AC binds a test/gate and evidence location)

| AC | Content | Test/gate |
| --- | --- | --- |
| AC-1 | Public TS API is ArrayBuffer-in / opaque string handle-out; wrapper `finally` wipes the caller buffer | TC-VC-006 static grep + code review |
| AC-2 | 12/15/18/21/24-word validation, binary search, `BigInteger` bit reconstruction (264-bit max), SHA-256 checksum | TC-VC-001, TC-VC-002 (JVM) |
| AC-3 | PBKDF2-HMAC-SHA512 2048 iterations matches V1–V4 with passphrase `TREZOR`; fallback loop present and unit-reachable | TC-VC-001 (JVM), TC-VC-011 review |
| AC-4 | `res/raw/bip39_english.txt` = 2048 lines, no BOM, SHA-256 `2f5eed…b24dbda`; `loadWordlist` fails closed otherwise | TC-VC-010 (JVM fixture + shell `cmp`/`sha256sum`) |
| AC-5 | `importMnemonic` returns a UUID-shaped, non-encoding handle; seed stays in native `ConcurrentHashMap`; never returned | TC-VC-012 instrumentation (best-effort) + code review |
| AC-6 | Input `ByteArray` wiped after decode; all sensitive byte/char buffers `fill(0)`; `PBEKeySpec.clearPassword()` | TC-VC-011 zeroization review |
| AC-7 | Only `E_VAULT_INVALID_MNEMONIC` / `E_VAULT_PBKDF2_UNAVAILABLE`; fixed error templates; no sensitive log calls | TC-VC-007 review + R5 grep |
| AC-8 | `git diff` on `package.json`, `apps/mobile/package.json`, `pnpm-lock.yaml` proves zero new deps; test deps appear only after owner approval recorded in `validation.md` | TC-VC-013 diff gate + R4 approval record |
| AC-9 | `./gradlew :<vault-core-project>:testDebugUnitTest` prints `BUILD SUCCESSFUL` with V1–V4 + N1–N3 + wordlist all green | TC-VC-001/002/010 mandatory automated gate |
| AC-10 | `npx tsc --noEmit` exits 0 and R1/R5 greps are clean. `npx expo-doctor` is recorded; 21/21 is expected, but a single pre-existing Expo patch-drift failure (57.0.28 vs doctor's expected ~57.0.29) is accepted only as named Handoff H7 — T1.2 itself changes no dependency | TC-VC-006/007 static checks + validation Task 6 |
| AC-11 | `validation.md` contains command + exit code + observed output for every task; no `[x]` without evidence | B.4/B.6 audit |
| AC-12 | Implementation-session precondition `GATE-MODEL-SWITCH` recorded before any code write | FR-12 / plan Task 0 |

## Security-level labelling (R6 — no level inflation)

| Mechanism | Label | Reason |
| --- | --- | --- |
| R1 ArrayBuffer-only boundary + no mnemonic string across JS↔native | **[P]** (within T1.2 scope) | Enforced by API shape and CI grep |
| Seed never leaves native / opaque UUID handle | **[P]** (within T1.2 scope) | Store is native-only; no return path |
| Kotlin `ByteArray`/`CharArray` zeroization | **[C]** | ART compacting GC may retain copies; JVM `String` intermediates are not zeroizable |
| No-seed/no-mnemonic logging | **[P]** (within T1.2 scope) | No log call + fixed error templates + grep gate |
| Handle-store lifetime without `destroy` (T1.2 interim) | **[C]** | Entries live until process death until T1.3/T1.4 implements destruction; no persistence, no export |

## Handoff (owned by T1.2, adjudicated by the next planning round)

| # | Item | Reason | Suggested disposal | AC/TC |
| --- | --- | --- | --- | --- |
| H1 | No `destroy` in T1.2: handle store grows per import for process lifetime | T1.3/T1.4 out of scope | Implement in T1.3/T1.4 (`destroy(handle)` + eviction) | AC-5 |
| H2 | `specs/vault-core.md` conflicts (handle type, checksum error code, UUID vs Int) | Draft spec predates Q2/skill | Replan to a spec-amendment round; T1.2 spec is authoritative for importMnemonic until then | AC-1, AC-7 |
| H3 | Instrumentation smoke | **Closed 2026-10-15**: clean rerun on Pixel 6a / Android 17 → 2/2 tests, `BUILD SUCCESSFUL in 1m 1s`, XML `tests="2" failures="0" errors="0"`. The 2026-10-14 test-side assertion had already been fixed; the later unpinned failure was a duplicate adb-transport harness artifact, not code (see `validation.md` §5.4) | Keep `VaultCoreModuleTest.kt` for device runs; JS-bridge `importMnemonic` invocation stays deferred to T1.3 | AC-5, AC-9 |
| H4 | PBKDF2 fallback loop coverage | **Closed 2026-10-14**: `fallbackLoopMatchesVectorV1` calls `mnemonicToSeedUsingFallback` and matches V1 | Keep regression test | AC-3 |
| H5 | R4 test-dependency amendment | **Closed 2026-10-14**: owner approved the exact FR-10 package; amendment applied | Keep approval record in validation.md | AC-8, AC-9 |
| H6 | T1.1 scaffold debt visible in tree (no eslint/jest installed; `packages/vault-core` absent) | Out of T1.2 scope; current app still typechecks | Replan to T1.1 closure | AC-10 |
| H7 | `npx expo-doctor`: 20/21, expo `57.0.28` vs expected `~57.0.29` | Pre-existing T1.1 dependency drift; R4 forbids T1.2 from upgrading a dependency | Replan to T1.1 closure or an owner-approved R4 patch bump | AC-10 |
