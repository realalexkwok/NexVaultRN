# T1.3 — vault-core Android BIP32/BIP44 deriveAddress (validation)

> Feature spec (validation record). Branch: `feature/T1.3-vault-core-android-bip32`.
> Status: **`[x]` verified 2026-10-15** — JVM vector gate 26/26, real-device instrumentation
> 4/4 on Pixel 6a / Android 17 (`BUILD SUCCESSFUL in 4m 22s`), zeroization review recorded.
> T1.3-H3（device smoke）由该次真机运行关闭；桥接事项（T1.2-H3，本文件记为 T1.3-H8）与其余
> 编号 Handoff 仍开放。2026-10-16 代码评审轮修订了规格并复跑 JVM 门禁——见文末 §Code review round。
> No task is marked `[x]` without command + observed output below.
> Status scale (AGENTS.md B.4): `[x]` verified (both sides with evidence) · `[~]` implemented
> but unverified · `[ ]` not started.

## Planning-round evidence (2026-10-15, read-only / record-only actions)

### P0.1 — Constitution inputs

| Item | Observed result |
| --- | --- |
| Read `AGENTS.md` (Part A board, B.1–B.7, C.1/C.2) | T1.2 closed and merged to `main` (`6b9fec4` + board record `24a35e6`); roadmap order makes T1.3 the next item |
| Read `specs/roadmap.md`, `plans/3-day-implementation-plan.md` T1.3 row | T1.3 = Android BIP32 (secp256k1, packaged BouncyCastle) + BIP44 → `deriveAddress`; acceptance: BIP32 official vectors green + known-mnemonic address matches the original repo record |
| Read `specs/tech-stack.md` | No BouncyCastle row → R4 amendment is a hard prerequisite (GATE-R4-BC) |
| Read `specs/vault-core.md` | §2.1/§2.2/§2.3 declared `VaultHandle = number` and `handle: Int`; §2.4 split `E_VAULT_CHECKSUM_MISMATCH` (both H2 conflicts) |
| Read T1.2 three files + `specs/pending-manual-test-cases.md` | T1.2 Handoff H1/H2/H3/H6/H7 adjudicated in `requirements.md` (owner D1–D4) |

### P0.2 — Original-repo anchor (GATE-REFERENCE-ORIGINAL-REPO)

| Source read | Observed result |
| --- | --- |
| `…/NexVault/core/core-security/src/test/java/…/HDKeyManagerTest.kt` | line 16 `// TC-SEC-005 …`; line 24 `assertEquals("0x9858EfFD232B4033E47d90003D41EC34EcaEda94", address)` |
| `…/HDKeyManager.kt` | web3j `Bip32ECKeyPair` — reference-only (this repo uses packaged BouncyCastle) |
| Second-address scan | no recorded value in the original repo → pinned independently (see Task 0) |

### P0.3 — Skill identity

| Command | Exit | Observed result |
| --- | --- | --- |
| `sha256sum .agents/skills/nexvault-t1-3-…/SKILL.md specs/features/2026-10-15-T1.3-…/SKILL.md` | 0 | both `1c9aec107f717a765e17a3c1da06fa3d62da0e20500c45b78842a2ca30e07ad7` |

### P0.4 — Ask-user rounds (C.1)

- 2026-10-15 planning: Q1–Q6 all **confirmed** (H2 scope · canonical anchor · R4 proposal wording ·
  `@scure/bip32` /tmp cross-check · Pixel 6a + `ANDROID_SERIAL` · GATE-MODEL-SWITCH) →
  transcribed in `requirements.md`.
- 2026-10-15 implementation: GATE-R4-BC approval asked twice via `ask_user_question` (first call
  timed out at 600 s, re-asked after owner request) → **owner selected "Approve as proposed"**.

### P0.5 — Feature branch and autolinking

| Command | Exit | Observed result |
| --- | --- | --- |
| `git switch -c feature/T1.3-vault-core-android-bip32` (from `main` `2dbe7a5`) | 0 | branch created |
| `cd apps/mobile && npx expo-modules-autolinking search --platform android --json` | 0 | `vault-core` resolves at `modules/vault-core` |

### P0.6 — Planning-round commits

| Command | Exit | Observed result |
| --- | --- | --- |
| `git commit -m "docs(T1.3): planning-round specs — deriveAddress (BIP32/BIP44)"` | 0 | `10fbb64`, 3 files, +632 |
| `git commit -m "docs(T1.3): record planning commit evidence (P0.6)"` | 0 | `335b29f` |

## Implementation evidence (2026-10-15)

### Task 0 — Preflight, vector provenance, spec correction

Status: `[x]`

```bash
git status --short --branch
cd apps/mobile && npx expo-modules-autolinking search --platform android --json | grep vault-core
curl -fsSL https://raw.githubusercontent.com/bitcoin/bips/master/bip-0032.mediawiki -o /tmp/bip32.mediawiki
sha256sum /tmp/bip32.mediawiki
python3 /tmp/bip32_verify.py
```

Observed:
- `GATE-MODEL-SWITCH` **satisfied**: owner switched the model and resumed implementation
  (this session runs the V4.1-Flash model).
- `bip-0032.mediawiki` sha256 `e5e00a8289db2f681052cf24a745320afc225e66b25d1e489a7c884d2fc7f11f`
  (315 lines; vectors at 218–309).
- Independent Python implementation (own HMAC-SHA512/Base58Check/secp256k1 point math/Keccak-256)
  reproduced **every** listed chain of V1–V4: `V1 6 PASS · V2 6 PASS · V3 2 PASS · V4 3 PASS`.
- `keccak256("")` and the four ERC-55 vectors OK; canonical anchor `0x9858EfFD…Eda94`
  **independently MATCH**; second address `0x6Fac4D18c912343BF86fa7049364Dd4E424Ab9C0`;
  BIP-39 seed for the canonical mnemonic `5eb00bbd…9e38e4` (matches the T1.2 locked value).
- **Spec correction (recorded):** the planning draft over-listed V3/V4 chains and mis-described V5.
  The fetched revision has V3 = 2 chains, V4 = 3 chains, and V5 = **16 malformed extended keys**
  (serialization validity, not derivation). `requirements.md` + `plan.md` corrected; V5 is
  explicitly outside the gate (Handoff T1.3-H7) instead of a fake gate.

### Task 1 — H2 spec amendment (`specs/vault-core.md`)

Status: `[x]`

```bash
git diff --stat -- specs/vault-core.md
```

Observed: `1 file changed, 17 insertions(+), 13 deletions(-)` — six edits: header amendment note;
§2.1 `VaultHandle` → branded string; §2.2 Kotlin handles → `String`; §2.3 iOS handles → `String`
(doc-only, implementation stays T1.5); §2.4 `E_VAULT_CHECKSUM_MISMATCH` merged into
`E_VAULT_INVALID_MNEMONIC`; TC-VC-002 expects only `E_VAULT_INVALID_MNEMONIC`.

### Task 2 — TS interface and wrapper

Status: `[x]`

```bash
cd apps/mobile && npx --no-install tsc --noEmit                              # exit 0
…/node_modules/.bin/tsc -p /tmp/t13-typecheck                                # module files vs real Expo types, exit 0
…/node_modules/.bin/tsc -p /tmp/t13-guard && STUB_MODE=missing node /tmp/t13-guard/run.js
STUB_MODE=present node /tmp/t13-guard/run.js
```

Observed (throwaway harness, not committed):
- `missing`: `importMnemonic=throw:E_VAULT_UNSUPPORTED_PLATFORM | …`, `bufferAfter=[0,0,0,0,0]`,
  `deriveAddress=throw:E_VAULT_UNSUPPORTED_PLATFORM | … deriveAddress …`
- `present`: `importMnemonic=handle:handle-abc`, `bufferAfter=[0,0,0,0,0]`,
  `deriveAddress=0x9858EfFD232B4033E47d90003D41EC34EcaEda94`,
  `stubSaw={"handle":"handle-abc","path":"m/44'/60'/0'/0/0"}`

### Task 3 — GATE-R4-BC (owner approved)

Status: `[x]`

| Evidence | Observed result |
| --- | --- |
| Maven Central `maven-metadata.xml` | `<version>1.86</version>`, `<lastUpdated>20260911045244</lastUpdated>` (2026-09-11 → **34 days ≥ 7-day min-release-age**) |
| HEAD `bcprov-jdk18on-1.86.jar` | `HTTP/2 200`, `content-length: 7224011`, `last-modified: Fri, 11 Sep 2026 02:42:07 GMT` |
| Hash cross-check | published `.sha256` `2af190b300cbb0b35e248ccf5f4a06b6072030aeb3da7a98ec73abe5b4cb371f` == local download |
| OSV `query` @ 1.86 | `vulns affecting 1.86: 0`; package total 12 advisories, all fixed ≤ 1.85 |
| **Owner approval** | `ask_user_question` → **"Approve as proposed"** (2026-10-15; first call timed out at 600 s, retried on owner request) |

Amendments applied after approval:

```bash
git diff --stat -- specs/tech-stack.md doc/dependencies.md modules/vault-core/android/build.gradle
```

- `specs/tech-stack.md` — new row `org.bouncycastle:bcprov-jdk18on | 1.86（精确钉版） | modules/vault-core/android（implementation）`.
- `doc/dependencies.md` — new **§6b** (version, purpose, install location, activity, CVE disposition,
  audit notes) + footnote `[^24^]`.
- `modules/vault-core/android/build.gradle` — `implementation 'org.bouncycastle:bcprov-jdk18on:1.86'`.

### Task 4 — `Bip32.kt` core

Status: `[x]`

```bash
unzip -l /tmp/bcprov-1.86.jar | grep -i hdkey        # → no HDKeyDerivation / HDKey classes
cd apps/mobile/android && ./gradlew :vault-core:compileDebugKotlin   # BUILD SUCCESSFUL
```

**Documented deviation from the skill/plan (recorded, not hidden):** the skill and the first draft of
`requirements.md` assumed BouncyCastle's `HDKeyDerivation`/`HDKey` API. The **1.86 artifact ships
no HD-key classes** (verified by listing the jar; there is also no `Base58` helper). The BIP-32 CKD
*glue* is therefore implemented in `Bip32.kt` (HMAC-SHA512 via `HMac(SHA512Digest)`, ser32/ser256
concatenation, `IL + k_parent mod n`, HASH160 fingerprinting) while **all curve arithmetic stays in
BC** (`CustomNamedCurves.getByName("secp256k1")`), which preserves `specs/vault-core.md` §2.2's
"point math must not be self-implemented" rule. Base58Check exists only in the JVM test
(test-side formatting). `requirements.md` FR-3 and `plan.md` Task 4 were corrected accordingly.

API surface: `parseEthereumPath`, `masterNode`, `deriveChild`, `deriveAddressBytes`,
`toChecksumAddress`, `keccak256`, `compressedPublicKey`, `uncompressedPublicKey`, `Node`.
Lightweight API only; no JCE provider registration (grep-verified).

### Task 5 — Module wiring and typed errors

Status: `[x]`

- `VaultExceptions.kt` adds `InvalidPathException` (`E_VAULT_INVALID_PATH`),
  `HandleNotFoundException` (`E_VAULT_HANDLE_NOT_FOUND`), `CryptoInternalException`
  (`E_VAULT_CRYPTO_INTERNAL`) — fixed messages, no fragments.
- `VaultCoreModule.kt` registers `AsyncFunction("deriveAddress") Coroutine` on
  `Dispatchers.Default`: handle lookup (`HandleNotFoundException` when absent) → path parse →
  `seed.copyOf()` → derive → `seedCopy.fill(0)` in `finally`; returns the checksummed address.
  `importMnemonic` untouched.
- `./gradlew :vault-core:compileDebugKotlin` → `BUILD SUCCESSFUL in 37s` (exit 0).

### Task 6 — Tests and gates

Status: `[x]` — JVM gate 26/26 and device smoke 4/4 both green

```bash
./gradlew :vault-core:testDebugUnitTest --rerun
./gradlew :vault-core:compileDebugAndroidTestKotlin
adb devices -l                                     # two transports for one phone → pin ANDROID_SERIAL
ANDROID_SERIAL=10.42.0.139:41771 ./gradlew :vault-core:connectedDebugAndroidTest
```

Observed:
- **JVM gate green:** `BUILD SUCCESSFUL`; XMLs
  `/home/superguo/Projects/NexVaultRN/modules/vault-core/android/build/test-results/testDebugUnitTest/TEST-expo.modules.vaultcore.Bip32Test.xml`
  → `tests="10" skipped="0" failures="0" errors="0"` and
  `TEST-expo.modules.vaultcore.Bip39Test.xml` → `tests="16" skipped="0" failures="0" errors="0"`
  (**26 JVM tests total**).
- First run of the new suite **failed 3/10**: `parseEthereumPath` rejected valid EVM paths because
  the range check compared against `HARDENED.toLong()` (= `-2147483648L`). **Production bug fixed**
  (compare against `0x80000000L`), not the test — the vector/negative cases were correct
  (B.6-5 respected: the assertion stayed, the defect was removed).
- BIP-32 vector assertions: `bip32Vector1..4` PASS (all chains, xprv + xpub, via the test-side
  78-byte serializer + Base58Check); V5 excluded (T1.3-H7).
- `canonicalAddress` = `0x9858EfFD232B4033E47d90003D41EC34EcaEda94`;
  `secondAddressCrossCheck` = `0x6Fac4D18c912343BF86fa7049364Dd4E424Ab9C0`; `keccakSanity` and
  `erc55Vectors` PASS; `rejectsInvalidPaths` (10 shapes) + `acceptsHardenedSpellings` PASS.
- `compileDebugAndroidTestKotlin` → `BUILD SUCCESSFUL` (so the extended instrumentation suite
  compiles against `internal` members).
- **Device smoke (H3 closed 2026-10-15):** Pixel 6a (`bluejay`, serial `26111jEGR13989`),
  Android 17 / API 37, build `google/bluejay/bluejay:17/CP2A.260705.006/15641320:user/release-keys`;
  wireless adb, with `ANDROID_SERIAL=10.42.0.139:41771` pinned because the same phone again
  appeared through two transports (T1.2 lesson applied). Command
  `ANDROID_SERIAL=… ./gradlew :vault-core:connectedDebugAndroidTest` →
  `Starting 4 tests on Pixel 6a - 17`, `Finished 4 tests on Pixel 6a - 17`,
  `BUILD SUCCESSFUL in 4m 22s` (`gradle_exit=0`). XML
  `/home/superguo/Projects/NexVaultRN/modules/vault-core/android/build/outputs/androidTest-results/connected/debug/TEST-Pixel 6a - 17-_vault-core-.xml`
  → `tests="4" skipped="0" failures="0" errors="0"` (`timestamp="2026-10-15T18:51:49"`); cases:
  `rawWordlistHasExactly2048Words`, `moduleDefinitionExposesImportMnemonicAndDeriveAddress`,
  `canonicalAddressOnDevice`, `invalidPathExposesTypedCode`. APK:
  `/home/superguo/Projects/NexVaultRN/modules/vault-core/android/build/outputs/apk/androidTest/debug/vault-core-debug-androidTest.apk`.

### Task 7 — Verification, zeroization review, Handoff

Status: `[x]`

| Check | Command | Exit | Observed result |
| --- | --- | --- | --- |
| npm/Gradle drift | `git diff --stat -- package.json apps/mobile/package.json pnpm-lock.yaml` | 0 | empty — **zero npm change** (BC is a Gradle-only dependency) |
| R1 grep | `grep -rnEi 'mnemonic\|privateKey\|secretKey\|seedPhrase\|xprv' apps/mobile --include='*.ts' …` | 1 | zero matches |
| R5 grep | `grep -rnE 'console\.(log\|warn\|error\|info)\(…\)'` | 1 | zero matches |
| Kotlin log | code-level `Log.[vdiew]`/`println(`/`System.out` scan | 1 | zero real calls (only a KDoc sentence mentions `Log.*`) |
| JCE provider | `grep -rn 'BouncyCastleProvider\|Security.addProvider'` src/main | 0 | **代码评审轮 2026-10-16（NIT-1）更正：原记录 exit `1` 与命令不符**——该命令命中 `Bip32.kt:21` 的 KDoc 句子（本次复核复现为 exit 0）；实质不变：从未注册 provider，仅注释命中 |
| TypeScript | `cd apps/mobile && npx --no-install tsc --noEmit` | 0 | clean |

Zeroization review (file:line, all `fill(0)` in `finally`):

| Path | Evidence | Verdict |
| --- | --- | --- |
| Module seed copy | `VaultCoreModule.kt:49,53` `seed.copyOf()` → `seedCopy.fill(0)` | PASS |
| `importMnemonic` input bytes | `VaultCoreModule.kt:28` (T1.2) | PASS |
| Master-node IL/IR + HMAC output | `Bip32.kt:79,82` | PASS |
| CKD hardened/public serialization buffer + compressed pubkey | `Bip32.kt:103,108` | PASS |
| CKD IL/IR on both failure and success paths | `Bip32.kt:115,120,125,128` | PASS |
| Path chain codes (every derived node) | `Bip32.kt:164` (loop) | PASS |
| Address pubkey buffer `x‖y` + keccak digest | `Bip32.kt:157,161,162` | PASS |
| EIP-55 keccak hash buffer | `Bip32.kt:201` | PASS |
| Fingerprint HASH160 buffers | `Bip32.kt:237-239` | PASS |
| `writeSer256` BigInteger copy | `Bip32.kt:261` | PASS |
| BC `BigInteger` private-key intermediates (immutable, GC-released) | `Node.key`, `offset`, `childKey` | **[C] caveat** — same class as the T1.2 String/BigInteger caveat; never [P] |
| BC `HMac` interior (`inputPad`/`outputBuf`/`opadState`; fields verified with `javap` on 1.86) — XOR-derived copy of the chain code / master HMAC key | not wiped, GC-released only (BC exposes no wipe API) | **[C] caveat — added by the 2026-10-16 review round (MINOR-3); the original table listed only `BigInteger`** |
| CKD serialization buffer `data` (`Bip32.kt:89-108`) | wiped on the success path (`:108`), but the `fill(0)` sits **outside** `finally`: if `hmacSha512` throws, the hardened-branch buffer (`ser256(parent.key)`) survives | **[C] residual — Handoff T1.3-H9** (patch: `val i = try { hmacSha512(parent.chainCode, data) } finally { data.fill(0) }`) |
| EIP-55 `body.toByteArray(US_ASCII)` temp (`Bip32.kt:183`, public address hex) | unwiped temp; the derived `String`s are not wipeable | **[C] (public data only, no key material)** |

## Gate checklist

| Gate | Status | Evidence |
| --- | --- | --- |
| GATE-MODEL-SWITCH | `[x]` satisfied | Owner switched the model; implementation session runs V4.1-Flash |
| GATE-R4-BC | `[x]` satisfied | Owner approved `bcprov-jdk18on:1.86`; amendments applied (Task 3) |

## Handoff list (adjudicated by the next planning round)

| # | Item | Status 2026-10-15 | Disposal |
| --- | --- | --- | --- |
| T1.3-H1 | `destroy` absent; handle store per process | `[ ]` open | Implement in T1.4 |
| T1.3-H2 | iOS `deriveAddress` port | `[ ]` open | Replan to T1.5 |
| T1.3-H3 | Device smoke | **`[x]` closed 2026-10-15** — Pixel 6a / Android 17, 4/4 tests, `BUILD SUCCESSFUL in 4m 22s` (Task 6) | Rerun with `ANDROID_SERIAL` pinned whenever the module surface changes |
| T1.3-H4 | BIP-32 vector transcription provenance | `[x]` closed | Vectors verified before transcription (Task 0) and asserted green in Task 6 |
| T1.3-H5 | JS wrapper evidence via throwaway harness | `[ ]` open | T1.1 closure should absorb it as committed jest tests |
| T1.3-H6 | BC 1.86 pin carried to T1.4 signing work | `[ ]` open | Replan to T1.4 |
| T1.3-H7 | BIP-32 vector 5 (16 invalid extended keys) not gated | `[ ]` open | No production extended-key parser; absorb with a future xpub/parser feature |
| T1.3-H8 | **JS→native bridge invocation still not exercised** (the instrumentation suite calls the same code the `AsyncFunction` runs, but not through the JS runtime) — **= the T1.2-H3 item; owner D3 "Implement" is not achieved** (review round 2026-10-16, MAJOR-1) | `[ ]` open | **Feasibility verdict (round-2 action, evidence below):** the "AppContext-backed AsyncFunction call" route is not available inside the library-module instrumentation APK — `AppContext` requires a `WeakReference<ReactApplicationContext>` + `ModulesProvider` + legacy `ModuleRegistry` (expo-modules-core@57.0.22 `AppContext.kt:50-54`), i.e. a full RN host. Practical route = a dev-build app screen smoke (T4.1/UI) or jest once the app imports the module. **Owner adjudication 2026-10-16: REPLAN → T4.1** — the dev-build screen smoke (onboarding 导入助记词 → `deriveAddress` through the AsyncFunction/JS bridge) is the acceptance carrier; the T4.1 planning round must re-adjudicate this row (B.7-2) |
| T1.3-H9 | Zeroization residuals found by the 2026-10-16 review: `data.fill(0)` outside `finally` (`Bip32.kt` `deriveChild`, hardened branch holds `ser256(parent.key)`); BC `HMac` pad state; EIP-55 ASCII temp | `[ ]` open | Zeroization-only edits, no derivation/API change. **Round-2 action:** the `data` patch was applied and the JVM gate re-ran green (26/26), but the device re-run could not be performed (no device attached), so the patch was **reverted** to keep the T1.3 device evidence on the exact committed revision — apply in T1.4 together with its device suite re-run | AC-5 |

## Status board

| Task | Status |
| --- | --- |
| Planning round (P0.1–P0.6) | `[x]` verified |
| Task 0 preflight + vector verification + spec correction | `[x]` verified |
| Task 1 H2 `specs/vault-core.md` amendment | `[x]` verified |
| Task 2 TS interface + wrapper harness | `[x]` verified |
| Task 3 GATE-R4-BC approval + amendments | `[x]` verified |
| Task 4 `Bip32.kt` core | `[x]` verified (compiles; vectors green) |
| Task 5 module wiring + typed errors | `[x]` verified |
| Task 6 JVM vector gate | `[x]` verified (26/26) |
| Task 6 device smoke (H3) | `[x]` verified — Pixel 6a / Android 17, 4/4 tests |
| Task 7 verification + zeroization review | `[x]` verified |
| T1.3 overall | **`[x]` verified 2026-10-15** — JVM vector gate 26/26 + real-device instrumentation 4/4 + zeroization review |
| Code review round (2026-10-16) | **specs amended + JVM gate re-run 26/26**; H9 patch applied→reverted (no device to re-verify), H8 = replan candidate; no production code change — findings/dispositions + round-2 actions in §Code review round | `specs/features/2026-10-15-T1.3-vault-core-android-bip32/validation.md` §Code review round |

## Code review round (2026-10-16, independent reviewer)

Independent re-verification performed by the reviewer (not read from this file):

| Check | Method / command | Observed result |
| --- | --- | --- |
| BIP-32 vector transcription | reviewer's own Python Keccak-f[1600] + secp256k1 + CKDpriv + Base58Check, run against the `OfficialChain(...)` strings parsed out of `Bip32Test.kt` | all **17** chains (V1 6 · V2 6 · V3 2 · V4 3) reproduce xprv **and** xpub; `keccak256("")`/`("abc")` and the 4 ERC-55 vectors pass; BIP-39 seed `5eb00bbd…9e38e4` and both addresses match |
| BIP-32 source revision | `curl … bip-0032.mediawiki` + `sha256sum` | `e5e00a8289db2f681052cf24a745320afc225e66b25d1e489a7c884d2fc7f11f`, 315 lines — matches the Task-0 record; V3 = 2 chains, V4 = 3 chains, V5 = exactly **16** malformed extended keys |
| JVM gate | `cd apps/mobile/android && ./gradlew :vault-core:testDebugUnitTest --rerun` | `BUILD SUCCESSFUL in 32s`, exit 0; `Bip32Test tests="10"` + `Bip39Test tests="16"`, 0 failures/errors = **26/26** |
| Device gate | XML + `…/connected/debug/Pixel 6a - 17/test-result.textproto` | `scheduled_test_case_count: 4`, `test_status: PASSED`, device `10.42.0.139:41771` (pinned serial), XML `tests="4" … errors="0"`, `timestamp="2026-10-15T18:51:49"`; run mtime 02:48–02:52 (+08) precedes commit `848d62f` (02:52:30) |
| BC 1.86 | `sha256sum` of the Gradle-cached jar vs Maven Central `.sha256`; `maven-metadata.xml`; OSV API | jar `2af190b3…b371f` both sides; `<lastUpdated>20260911045244`; **OSV version query for 1.86 → `{}` (0 vulns)**, package total 12 (matches §6b) |
| HDKey / Base58 absence | `unzip -l bcprov-jdk18on-1.86.jar` | 0 `hdkey` entries, 0 `base58` entries — the deviation note is factually correct |
| Dependency graph | `./gradlew :vault-core:dependencies --configuration debugRuntimeClasspath` | exactly one artifact: `org.bouncycastle:bcprov-jdk18on:1.86` |
| Greps / TS | R1 grep · R5 grep · Kotlin `Log./println/System.out` · `tsc --noEmit` | exit 1 · exit 1 · exit 1 · `tsc_exit=0`; JCE two-pattern grep = exit 0 (KDoc only — NIT-1) |
| Device logcat | grep over the preserved `logcat-*` artifacts | no key material (only test names containing "Mnemonic") |

Findings and dispositions:

| # | Severity | Finding | Disposition in this round |
| --- | --- | --- | --- |
| MAJOR-1 | 🔴 MAJOR | FR-8/AC-9 "end-to-end via the bridge" is not implemented — the device test calls `Bip39`/`Bip32` directly; T1.2-H3 (owner D3 "Implement") was re-deferred as T1.3-H8 without amending Goal/FR-8/AC-9 | **Specs amended**: Goal bullet, D3 row, Handoff re-read row, AC-9, `plan.md` Task 6.2, and an explicit T1.3-H8 row added to `requirements.md`. The code gap stays open pending owner adjudication (implement or replan) |
| MINOR-1 | 🟡 MINOR | Several claims still contradicted the corrections: V5 gated ("vectors 1–5") in Goal/FR-7 heading/AC-3; BC credited with `Base58`/Base58Check in FR-7 + `tech-stack.md:41` + `dependencies.md §6b`; `plan.md` called V4 "public-only" | **Specs amended** in all six locations; the underlying facts were re-verified by the reviewer (jar + fetched BIP-32) |
| MINOR-2 | 🟡 MINOR | `specs/vault-core.md` §2.4 omitted the committed `E_VAULT_PBKDF2_UNAVAILABLE` (`VaultExceptions.kt:15`; documented in T1.2 FR/AC-7) | **Spec amended**: §2.4 row + revision note (no signature/behaviour change) |
| MINOR-3 | 🟡 MINOR | Zeroization inventory incomplete: `data.fill(0)` outside `finally` (`Bip32.kt:108`); BC `HMac` interior holds an XOR copy of the chain code; EIP-55 ASCII temp unwiped | **Zeroization table corrected above + Handoff T1.3-H9 registered.** No production code touched this round: a zeroization-only edit would need a fresh device run to keep B.4-2 two-sided evidence intact — fold into T1.4 |
| MINOR-4 | 🟡 MINOR | Second-address cross-check tool not named in Task 0 although AC-2/Q4 said `@scure/bip32` | **Specs amended** (FR-7 item 3, AC-2 — recorded tool: stdlib Python); the value itself was independently confirmed by the reviewer |
| NIT-1 | 🔵 NIT | Task-7 JCE row recorded exit `1`; the recorded command actually yields `0` | **Corrected in the Task-7 table above** |
| NIT-2 | 🔵 NIT | Path parser accepts undocumented aliases (trim / leading zeros / mixed `'`+`h`) while AC-1 said "exactly" | **Documented** in FR-2 + AC-1 |
| NIT-3 | 🔵 NIT | The device XML path quoted here is the same file T1.2's validation quotes; it now holds `tests="4"`, so T1.2's `tests="2"` evidence is no longer reproducible at its quoted path | Accepted (build-dir artifact, not committed). Future features should copy device evidence into a per-feature location |
| NIT-4 | 🔵 NIT | Android 17 / API 37 and the `bluejay` build fingerprint are quoted without an artifact (`device-info.pb` carries id/model/manufacturer only; adb not attached at review time) | Accepted — identical device record to T1.2; next device session should capture `adb shell getprop ro.build.fingerprint` into the evidence dir |
| NIT-5 | 🔵 NIT | "First run failed 3/10" is narrative-only (the failing XML was overwritten; the pre-fix code was never committed) | **Accepted as truthful.** `HARDENED.toInt().toLong() == -2147483648L`, so the old check rejected every non-negative account (a real production defect), and exactly the 3 tests that pass a valid path through `parseEthereumPath` would fail — matching 3/10. No test was weakened: both overflow negatives and both acceptance assertions remain |
| NIT-6 | 🔵 NIT | Both `SKILL.md` copies still teach `HDKeyDerivation` / "vectors 1–5" / BC `Base58` (the harness auto-loads them for future sessions) | **Superseded-pointer added** to `requirements.md`; the two copies stay sha256-frozen as provenance evidence |

No production source was modified in this round, so the device evidence revision is unchanged. The JVM XMLs quoted in Task 6 were regenerated by the reviewer's rerun (`2026-10-16T04:12Z`); the pre-rerun copies are preserved at `/tmp/t13_evidence_backup/`.

### Round-2 actions (main agent, 2026-10-16)

| Action | Command / evidence | Result |
| --- | --- | --- |
| H9 production patch | applied `val i = try { hmacSha512(parent.chainCode, data) } finally { data.fill(0) }`; `./gradlew :vault-core:testDebugUnitTest --rerun` | JVM green (26/26, `BUILD SUCCESSFUL in 13s`) — then **reverted** because `adb devices -l` was empty (two attempts, minutes apart), and a production edit without a matching device run would invalidate the T1.3 device evidence. `git checkout -- …/Bip32.kt` restored the committed revision; `Bip32Test 10/10` + `Bip39Test 16/16` re-confirmed on the reverted revision (`BUILD SUCCESSFUL in 7s`, XML mtime `2026-10-16 12:22`) |
| H8 feasibility | `javap` unavailable → read the shipped sources instead: `node_modules/.pnpm/expo-modules-core@57.0.22_…/android/src/main/java/expo/modules/kotlin/AppContext.kt:50-54` | `class AppContext(modulesProvider: ModulesProvider, val legacyModuleRegistry: ModuleRegistry, reactContextHolder: WeakReference<ReactApplicationContext>)` — a real `ReactApplicationContext` is mandatory, so an AsyncFunction bridge test needs an RN host ⇒ H8 is a **replan** candidate (dev-build screen / jest), recorded above |
| Evidence durability (NIT-3/NIT-4) | `evidence/` in this feature dir: `device-TEST-Pixel6a-2026-10-15.xml`, `device-test-result-2026-10-15.pb`, `device-run-2026-10-15.log`, `jvm-TEST-Bip32Test.xml`, `jvm-TEST-Bip39Test.xml`, `device-info.md` (serial/model/Android 17/API 37/fingerprint `google/bluejay/bluejay:17/CP2A.260705.006/15641320:user/release-keys`) | Device evidence now lives outside the gitignored build dirs; the fingerprint NIT-4 asked for is captured with its provenance |
