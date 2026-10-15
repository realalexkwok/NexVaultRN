# T1.3 — vault-core Android BIP32/BIP44 deriveAddress (validation)

> Feature spec (validation record). Branch: `feature/T1.3-vault-core-android-bip32`.
> Status: **`[x]` verified 2026-10-15** — JVM vector gate 26/26, real-device instrumentation
> 4/4 on Pixel 6a / Android 17 (`BUILD SUCCESSFUL in 4m 22s`), zeroization review recorded.
> H3 is closed by the device run; the remaining open Handoffs are non-T1.3 items.
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
| JCE provider | `grep -rn 'BouncyCastleProvider\|Security.addProvider'` src/main | 1 | only the KDoc note; **never registered** |
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
| T1.3-H8 | **JS→native bridge invocation still not exercised** (the instrumentation suite calls the same code the `AsyncFunction` runs, but not through the JS runtime) | `[ ]` open | Cover with a dev-build screen smoke (T4.1/UI items) or jest once the app wires the module |

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
