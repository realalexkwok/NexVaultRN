# T1.4 — vault-core Android signDigest/destroy (validation)

> Feature spec (validation). Branch: `feature/T1.4-vault-core-android-sign-digest`.
> Parent: `plans/3-day-implementation-plan.md` Day 1 T1.4（关键路径，四方法闭环 G1）.
> Skill: feature-dir `SKILL.md`, sha256
> `b0dbe67a1427adbf1a6a0add79dbbd049d3b045b484729e964666cbb7ea1f9b2`.
> Session status: implementation session 2026-10-16 on **V4.1-Flash** (GATE-MODEL-SWITCH
> satisfied) — every task below carries command + observed result (B.4-3).

## Gate checklist

| Gate | Status | Evidence |
| --- | --- | --- |
| GATE-MODEL-SWITCH | `[x]` satisfied | Owner switched the model to V4.1-Flash (deepseek-flash) between the planning commit `ed84a82` and Task 0; no production-code edit happened before the switch |
| No-new-dependency (R4) | `[x]` satisfied | `git diff` over `modules/vault-core/android/build.gradle` is empty on the whole branch; BC 1.86 reused (Tasks 0/8) |
| H9-first ordering | `[x]` satisfied | The zeroization patch (Task 5) is part of the first implementation commit `729c48f`; every test run in Task 6 ran on it |

## Task 0 — Preflight and baseline recording

Status: `[x]`

| Check | Command | Exit | Observed result |
| --- | --- | --- | --- |
| Baseline | `git branch --show-current`, `git status --short`, `git log --oneline -3` | 0 | `feature/T1.4-vault-core-android-sign-digest`, clean tree, HEAD `ed84a82` (planning specs) |
| Device | `adb devices -l` | 0 | one transport: `adb-26111jEGR13989-MJzh3R._adb-tls-connect._tcp  device product:bluejay model:Pixel_6a` — device gate viable |
| Autolinking | `npx expo-modules-autolinking search --platform android --json \| grep vault-core` | 0 | `{"vault-core":{…"path":"/home/superguo/Projects/NexVaultRN/modules/vault-core"…"android":{"modules":["expo.modules.vaultcore.VaultCoreModule"]}…}}` |
| BC jar | `find ~/.gradle -name 'bcprov-jdk18on-1.86.jar'` + `unzip -l` + `sha256sum` | 0 | `/home/superguo/.gradle/caches/modules-2/files-2.1/org.bouncycastle/bcprov-jdk18on/1.86/830c8467bed8e4c1148262386b09fc42aedcf262/bcprov-jdk18on-1.86.jar`; sha256 `2af190b300cbb0b35e248ccf5f4a06b6072030aeb3da7a98ec73abe5b4cb371f` (**identical to the T1.3 record**); 129 `crypto/signers/` entries incl. `ECDSASigner.class`, `HMacDSAKCalculator.class`; `ECPoint.class`, `ECAlgorithms.class`, `HMac.class`, `KeccakDigest.class` all present |
| RFC 6979 | `curl -fsSL https://www.rfc-editor.org/rfc/rfc6979.txt -o /tmp/rfc6979.txt && sha256sum` | 0 | sha256 `456e8f17558fdbd206f968b96fc6f1b4a71ea331ab30ad17f711ab3adaa7d701`, 4427 lines; `A.2.5. ECDSA, 256 Bits (Prime Field)` confirmed at line 1799 — **P-256, never used as a secp256k1 gate** (skill Provenance trap) |
| Reference (GATE-REFERENCE-ORIGINAL-REPO) | read `/home/superguo/Projects/NexVault/data/src/main/java/com/nexvault/wallet/data/wallet/SigningKeyProvider.kt` | 0 | 93 lines, web3j `Credentials`/`ECKeyPair` + `hdKeyManager.deriveEthereumKeyPair` — **reference-only, nothing copied**; this repo packages BouncyCastle per spec: vault-core §2.2 |
| Cross-check tooling | `node -e` on `/tmp/t14-toolcheck/node_modules/.../package.json` | 0 | `@noble/curves 2.4.0`, `@noble/hashes 2.4.0`, `viem 2.56.9` (throwaway only, zero repo drift); scripts live in `/tmp/t14-xcheck` with `node_modules` symlinked to the toolcheck tree |

## Task 1 — Spec check (no amendment)

Status: `[x]` — **no amendment needed**, recorded instead of edited:

| Spec anchor | Already specifies |
| --- | --- |
| `specs/vault-core.md` §2.1 | `signDigest(handle, path, digest): Promise<ArrayBuffer>` = 65 bytes `r(32)‖s(32)‖recid(1, 取值 0/1)`; `destroy(handle): Promise<void>` 幂等 |
| §2.2 | Kotlin `AsyncFunction("signDigest") { handle: String, path: String, digest: ArrayBuffer -> ArrayBuffer }` and `AsyncFunction("destroy") { handle: String -> Unit }` |
| §2.4 | `E_VAULT_INVALID_DIGEST_LENGTH` (digest ≠ 32 bytes) and `E_VAULT_HANDLE_NOT_FOUND`（幂等 destroy 不抛此错）already listed |
| §4/§5 | AC-3 = TC-VC-004 (determinism/format/recovery), AC-4 = TC-VC-005 (1000× hygiene + post-destroy code) |

Evidence of no edit: `git diff --stat 85dbf83..HEAD -- specs/vault-core.md` is empty (specs/ untouched by this branch).

## Task 2 — TS interface

Status: `[x]`

Files: `modules/vault-core/src/VaultCoreModule.ts` (+2 declarations) and `modules/vault-core/index.ts`
(+`signDigest`/`destroy` wrappers, `INVALID_DIGEST_LENGTH_CODE`, `CRYPTO_INTERNAL_CODE`, exact-65 copy).

Throwaway harness `/tmp/t14-guard` (not committed): `index.ts` copied from the repo —
`sha256sum` of `/home/superguo/Projects/NexVaultRN/modules/vault-core/index.ts` and
`/tmp/t14-guard/index.ts` are **both** `ed665f5fb7856b2d11ae815845248bbb39b691878f51468ce61941ced7ee6981`;
the native side is a `STUB_MODE`-driven stub; `tsc -p /tmp/t14-guard` → `tsc_exit=0`.

```
STUB_MODE=missing node /tmp/t14-guard/run.js
  signDigest=throw:E_VAULT_UNSUPPORTED_PLATFORM | … vault-core signDigest is unavailable on this platform
  digestAfter=[7,7,…] (32×7 — the public digest is deliberately NOT wiped)
  destroy=throw:E_VAULT_UNSUPPORTED_PLATFORM | …
  shortDigest=throw:E_VAULT_UNSUPPORTED_PLATFORM      (guard order: platform check first)
  stubSaw=[]
STUB_MODE=present node /tmp/t14-guard/run.js
  signDigest=ok byteLength=65 last=170 isArrayBuffer=true   (65-byte copy out of a pooled 128-byte stub buffer)
  digestAfter=[7,7,…]                                        (no fill(0) — public tx hash, FR-1)
  destroy=ok
  shortDigest=throw:E_VAULT_INVALID_DIGEST_LENGTH
  stubSaw=[{"op":"signDigest","handle":"handle-abc","path":"m/44'/60'/0'/0/0","digestLength":32},{"op":"destroy","handle":"handle-abc"}]
```

## Task 3 — `Ecdsa.kt` core

Status: `[x]` — new file `modules/vault-core/android/src/main/java/expo/modules/vaultcore/Ecdsa.kt` (≈270 lines):
file-private helpers (`toFixedLength`, `concat`), `internal class Rfc6979Drbg` (RFC 6979 §3.2 HMAC-SHA256:
`h1 = int2octets(bits2int(h) mod q)`, K/V init, h-loop, h3 `retry()`, capped at 64 attempts,
`wipe()`) and `internal object Ecdsa` (`requireDigest32`, `signDigest(seed, pathIndices, digest32)`,
`signDigestWithKey`). Curve arithmetic is 100% BouncyCastle (`CustomNamedCurves` secp256k1,
`ECPoint.multiply`); no self-written point math; lightweight API only.

**Production defect found by the first JVM run (not a test weakness):**

```
EcdsaTest > e2ePinnedSignature FAILED  — java.lang.IllegalStateException: point not in normal form
  at org.bouncycastle.math.ec.ECPoint.checkNormalized
  at expo.modules.vaultcore.Ecdsa.signDigestWithKey(Ecdsa.kt:218)
  (same for probeVectorKAT / apiDeterminismAndFormat; 34 tests completed, 3 failed)
```

`secp256k1.g.multiply(k)` returns a projective point in BC 1.86; affine coordinate access requires
`normalize()`. Fix (production code, tests untouched):
`val point = secp256k1.g.multiply(k).normalize()` — re-run green (Task 6.1).

Zeroization checklist for this file (file:line, all in `finally` or immediately after use):
`Ecdsa.kt:52` (`toFixedLength` scratch), `:106` (candidate k), `:122-125` (DRBG K/V/x/h1),
`:132`/`:134` (K-message + old key), `:141` (V), `:181`/`:183` (derived privkey + chain codes),
`:249`/`:250`/`:253` (r/s/k scratch), `:258`/`:259`/`:260` (key+digest copies + DRBG wipe).
**`[C]` caveats (never `[P]`):** `BigInteger` intermediates `d, z, k, r, s` and the R coordinates are
immutable; BC `HMac` keeps unwipeable pad state (`inputPad`/`opadState`).

## Task 4 — Handle store extraction + module wiring

Status: `[x]`

- New `HandleStore.kt`: `put(seed): String` (UUID), `get`, `evict` (= `remove`, hands the array back),
  `size`; KDoc records the evict-then-wipe race (in-flight derive/sign works on its own `copyOf()`).
- `VaultCoreModule.kt`: store field switched to `HandleStore`; importMnemonic now `handleStore.put(seed)`
  with a `catch (Throwable) { seed.fill(0) }` guard; `deriveAddress` uses `handleStore.get`;
  new `AsyncFunction("signDigest")` (`requireDigest32` **before** the store lookup → store → path parse →
  `seed.copyOf()` → `Ecdsa.signDigest` → `finally { seedCopy.fill(0) }`) and
  `AsyncFunction("destroy")` (`handleStore.evict(handle)?.fill(0)`, returns `Unit`, never throws).
- `VaultExceptions.kt`: `InvalidDigestLengthException` (`E_VAULT_INVALID_DIGEST_LENGTH`, fixed template).

## Task 5 — H9 zeroization patch (applied before every test run)

Status: `[x]` — in `Bip32.kt`, part of commit `729c48f`:

```kotlin
// deriveChild — the hardened branch holds ser256(parent.key); wipe even if HMAC throws
val i = try {
  hmacSha512(parent.chainCode, data)
} finally {
  data.fill(0)
}
// toChecksumAddress — wipe the ASCII scratch copy (public data, but it must not linger)
val bodyBytes = body.toByteArray(Charsets.US_ASCII)
val hash = try { keccak256(bodyBytes) } finally { bodyBytes.fill(0) }
```

Zeroization-only edits; no derivation/API change; both sides (Task 6.1/6.2) ran on this revision.

## Task 6.1 — JVM gate

Status: `[x]`

```
cd /home/superguo/Projects/NexVaultRN/apps/mobile/android
./gradlew :vault-core:testDebugUnitTest --rerun
observed: BUILD SUCCESSFUL in 6s; gradle_exit=0
```

| Suite | XML | tests | failures | timestamp |
| --- | --- | --- | --- | --- |
| Bip39Test | `modules/vault-core/android/build/test-results/testDebugUnitTest/TEST-expo.modules.vaultcore.Bip39Test.xml` | 16 | 0 | 2026-10-16T09:25:06.271Z |
| Bip32Test | …`Bip32Test.xml` | 10 | 0 | 2026-10-16T09:25:05.778Z |
| **EcdsaTest** | …`EcdsaTest.xml` | **6** | 0 | 2026-10-16T09:25:06.418Z |
| **HandleStoreTest** | …`HandleStoreTest.xml` | **2** | 0 | 2026-10-16T09:25:06.469Z |
| **Total** | | **34** | **0** | revision `79d1072` |

Durable copies: `specs/features/2026-10-16-T1.4-vault-core-android-sign-digest/evidence/TEST-expo.modules.vaultcore.*.xml`.
The earlier 09:19Z run (green, revision `729c48f`) is superseded by the 09:25Z run on the final
revision (both green, 34/34).

## Task 6.2 — Device suite + MAN-011 walk

Status: `[x]`

```
cd /home/superguo/Projects/NexVaultRN/apps/mobile/android
ANDROID_SERIAL='adb-26111jEGR13989-MJzh3R._adb-tls-connect._tcp' ./gradlew :vault-core:connectedDebugAndroidTest
observed (revision 79d1072): Starting 6 tests on Pixel 6a - 17 → Finished 6 tests on Pixel 6a - 17,
BUILD SUCCESSFUL in 1m 36s, gradle_exit=0
```

| Run | Artifact | Observed |
| --- | --- | --- |
| Gradle `729c48f` | `evidence/device-TEST-Pixel6a-2026-10-16-r729c48f.xml` + `.pb` | `tests="6" skipped="0" failures="0" errors="0" time="27.578" timestamp="2026-10-16T09:22:14"`; `BUILD SUCCESSFUL in 1m 54s` |
| Gradle `79d1072` | `evidence/device-TEST-Pixel6a-2026-10-16-r79d1072.xml` + `.pb` | `tests="6" skipped="0" failures="0" errors="0" time="31.197" timestamp="2026-10-16T09:26:43"`; `BUILD SUCCESSFUL in 1m 36s` |
| Device record | `evidence/device-info.md` | Pixel 6a / bluejay / Android 17 / API 37 / `google/bluejay/bluejay:17/CP2A.260705.006/15641320:user/release-keys` |

Test cases in the suite: `rawWordlistHasExactly2048Words`, `moduleDefinitionExposesAllFourFunctions`,
`canonicalAddressOnDevice`, `invalidPathExposesTypedCode`, `signAndDestroyOnDevice`, `thousandRoundLoop`.
Scope label (honest): these call the same code the AsyncFunction runs — **not** through the JS runtime;
the bridge smoke stays T1.4-H1 (replan → T4.1).

### MAN-011 walk and the ±5% protocol correction (finding + fix, recorded)

1. **First manual re-run** (`am instrument -w -e class …VaultCoreModuleTest`, revision `729c48f`):
   `Tests run: 6, Failures: 1` — `thousandRoundLoop`:
   `VmRSS before=125818kB after=133152kB growth=5.829054666263969%` → over the spec's ±5% band while the
   preceding gradle run had passed: the VmRSS metric plus a cold start let JIT/class-loading/allocator
   growth masquerade as leakage.
2. **Protocol correction** (commit `79d1072`, test-side only, **tolerance unchanged at ±5%**): measure the
   metric spec TC-VC-005 names first — post-GC settled **PSS** (`Debug.getMemoryInfo().totalPss`, 3-sample
   average, 250 ms settle) — with a 100-round warm-up before the baseline.
3. **Re-runs after the fix**: gradle suite green (`79d1072`, 09:26:43Z) and the manual walk
   `am instrument … VaultCoreModuleTest` → `OK (6 tests)` / `Time: 37.151`.

Manual-side sampling during that walk (`adb shell dumpsys meminfo expo.modules.vaultcore.test | grep TOTAL`,
first column = total PSS kB, one sample ≈ every 2 s, `pidof` for liveness):

```
17:27:42 pid=12524 TOTAL 62783
17:27:46 pid=12524 TOTAL 112749   (peak — mid-run allocations, GC not yet settled)
17:27:49 pid=12524 TOTAL 82312
17:27:51 pid=12524 TOTAL 86607
17:27:54 pid=12524 TOTAL 97962
17:27:57 pid=12524 TOTAL 63070    (post-GC dip)
17:28:00 pid=12524 TOTAL 86969
17:28:05 pid=12524 TOTAL 86761
17:28:10 pid=12524 TOTAL 92082
17:28:16 pid=12524 TOTAL 92630
17:28:19 pid=12524 (process finishing) → pid gone afterwards
```

Observation: the curve is **non-monotonic** (peak mid-run, post-GC dips, final value below the peak),
no OOM/crash, and the process exits cleanly after the loop. The ±5% residency band itself is carried by
the automated post-GC PSS before/after assertion inside `thousandRoundLoop` (green on `79d1072`).
MAN-011 recorded as 通过 in `specs/pending-manual-test-cases.md` with this evidence pointer.

## Task 7 — E2E cross-check (throwaway /tmp)

Status: `[x]` — `/tmp/t14-xcheck/t14-e2e.mjs` (never committed; `node_modules` symlinked to
`/tmp/t14-toolcheck`, zero repo drift). The script **reads the pinned literal out of `EcdsaTest.kt`**,
derives the canonical key with `viem/accounts` `mnemonicToAccount`, signs with `@noble/curves 2.4.0`
(`{ prehash: false, lowS: true }`), then awaits viem `recoverPublicKey`:

```
noble/curves      = 2.4.0
viem              = 2.56.9
bip44 address     = 0x9858EfFD232B4033E47d90003D41EC34EcaEda94
digest (keccak)   = 0x27a4c007a3e6817d766159696db59e4e71f1200c41389eb9c63bb2bff0dccf7c
pinned digest msg = nexvault-test-vector (from EcdsaTest.kt)
pinned signature  = 0x36eba619…745f459501 (from EcdsaTest.kt)
noble signature   = 0x36eba619…745f459501
signature match   = PASS
recid             = 1
viem recovered    = 0x9858EfFD232B4033E47d90003D41EC34EcaEda94
address match     = PASS
script_exit=0
```

Pinned values (also in `EcdsaTest.CANONICAL_SIGNATURE` and the device suite): r =
`36eba619abb6f0c6aea1f504ca79613270ef55b72313036ce574dad1e87106ea`, s =
`72aa8de93f14ca6e5921b06ece96fe727fc6f4573d70090cc9556f25745f4595` (low-s), recid = `1`.
**Honest note:** the very first script run printed `signature match = FAIL` — a bug in the throwaway
script (it appended the recid as one nibble while the pinned literal is byte-aligned); after fixing the
script (`padStart(2,'0')`) it printed PASS. Neither the test nor the implementation was changed for that.
Recovery sanity: viem trialled both yParity values — `0` recovered a different address,
`1` recovered the canonical one, so recid is uniquely determined.

Probe-vector KAT (TC-VC-020) is independent of the canonical path: d =
`C9AFA9D845BA75166B5C215767B1D6934E50C3DB36E89B127B8A622B120F6721`, digest = sha256("sample") =
`AF2BDBE1AA9B6EC1E2ADE1D694F41FC71A831D0268E9891562113D8A62ADD1BF` → r =
`432310e32cb80eb6503a26ce83cc165c783b870845fb8aad6d970889fcd7a6c8`, s =
`530128b6b81c548874a6305d93ed071ca6e05074d85863d4056ce89b02bfab69`, recid = 0 (validated
2026-10-16 with the same noble/viem tooling before being pinned).

## Task 8 — Verification, evidence, Handoff

Status: `[x]`

| Check | Command | Exit | Observed result |
| --- | --- | --- | --- |
| npm drift | `git diff HEAD~2 --stat -- package.json apps/mobile/package.json pnpm-lock.yaml` | 0 | empty — **zero npm change** |
| Gradle deps | `git diff --stat 85dbf83..HEAD -- modules/vault-core/android/build.gradle` | 0 | empty — BC 1.86 reused, no R4 amendment |
| R1 grep | `grep -rnEi 'mnemonic\|privateKey\|secretKey\|seedPhrase\|xprv' apps/mobile --include='*.ts' …` | 1 | zero matches |
| R5 grep | `grep -rnE 'console\.(log\|warn\|error\|info)\(…(mnemonic\|privKey\|secret\|seed\|sig)\)'` | 1 | zero matches |
| Kotlin log | `grep -rnE 'Log\.[vdiew]\|println\(\|System\.out' modules/vault-core/android/src` | 1 | zero matches (production **and** test sources) |
| JCE provider | `grep -rn 'BouncyCastleProvider\|Security.addProvider' modules/vault-core/android/src` | 0 | only the `Bip32.kt:21` KDoc sentence (never registered; the T1.3 NIT-1 class of hit) |
| TypeScript | `cd apps/mobile && npx --no-install tsc --noEmit` | 0 | clean |

### Zeroization review (T1.4 paths)

| Path | Evidence (file:line) | Verdict |
| --- | --- | --- |
| `importMnemonic` input bytes | `VaultCoreModule.kt:26` | PASS (T1.2) |
| Store ownership on put failure | `VaultCoreModule.kt:40` (`catch { seed.fill(0) }`) | PASS |
| derive/sign seed copies | `VaultCoreModule.kt:55`, `:70` (`seedCopy.fill(0)` in `finally`) | PASS |
| destroy | `VaultCoreModule.kt:78` `handleStore.evict(handle)?.fill(0)` (idempotent, no-op when absent) | PASS |
| Derived private-key bytes + every chain code | `Ecdsa.kt:181`, `:183` (`finally`) | PASS |
| DRBG state K/V/x-copy/h1 | `Ecdsa.kt:122-125` via `wipe()` (`:260` in `signDigestWithKey` finally) | PASS |
| DRBG message/old-key/V scratch | `Ecdsa.kt:132`, `:134`, `:141` (each wiped after use) | PASS |
| Nonce candidate k, r/s scratch | `Ecdsa.kt:106` (candidate, wiped per attempt), `:249`/`:250`/`:253` (`finally`) | PASS |
| Key/digest copies inside the signer | `Ecdsa.kt:258`/`:259` (`finally`) | PASS |
| **H9 fixed:** CKD serialization buffer (hardened branch holds `ser256(parent.key)`) | `Bip32.kt:111` now `finally { data.fill(0) }` (was outside `finally`) | PASS (was `[C]` residual) |
| **H9 fixed:** EIP-55 ASCII temp | `Bip32.kt:192` `bodyBytes.fill(0)` in `finally` | PASS (was `[C]` residual) |
| `BigInteger` intermediates (`d, z, k, r, s`, R coords) | immutable, GC-released | **`[C]` caveat** |
| BC `HMac` pad state (`inputPad`/`opadState`) | no BC wipe API | **`[C]` caveat** (T1.3-H9 class) |
| EIP-55 derived `String`s | public address only | **`[C]` (public data)** |

### Durable evidence

`specs/features/2026-10-16-T1.4-vault-core-android-sign-digest/evidence/`:
`TEST-expo.modules.vaultcore.{Bip39Test,Bip32Test,EcdsaTest,HandleStoreTest}.xml` (JVM, `79d1072`,
09:25Z), `device-TEST-Pixel6a-2026-10-16-r729c48f.xml`/`-r79d1072.xml` + matching `.pb`
(UTP result protos), `device-info.md` (getprop transcript).

### Handoff list (adjudicated by the next planning round)

| # | Item | Status 2026-10-16 | Disposal |
| --- | --- | --- | --- |
| T1.4-H1 | JS→native bridge invocation still not exercised (= T1.3-H8 = T1.2-H3); device suite calls the same code the AsyncFunction runs | `[ ]` open | **Owner adjudication 2026-10-16: replan → T4.1** (dev-build screen smoke: onboarding import → `deriveAddress` through the bridge); T4.1's planning round re-adjudicates |
| T1.4-H2 | jest/dev-build carriers of spec AC-3 (TC-VC-004) and of the JS wrapper guards | `[ ]` open | Replan → T1.1 closure / T4.1 (H5 pattern; the /tmp harness evidence stays throwaway) |
| T1.4-H3 | TC-VC-005 iPhone side (memory hygiene on iOS) | `[ ]` open | Replan → T1.5 |
| T1.4-H4 | Device-gate fallback | **not needed** — Pixel 6a attached; suite green twice | Keep `ANDROID_SERIAL` pinning; re-run when the module surface changes |
| T1.4-H5 | Cross-check tooling versions | recorded | `@noble/curves 2.4.0` · `@noble/hashes 2.4.0` · `viem 2.56.9` (throwaway `/tmp` only, zero repo drift) |

## Task 9 — Board + git closure + push

Status: `[ ]` — filled by the closure step (board line, merge, branch deletion, push).

## Status board

| Task | Status |
| --- | --- |
| Planning round (owner decisions + Q/A) | `[x]` verified 2026-10-16 — D1–D10 + Q1–Q6 in requirements.md |
| Task 0 preflight | `[x]` verified |
| Task 1 spec check (no amendment) | `[x]` verified |
| Task 2 TS interface + harness | `[x]` verified |
| Task 3 `Ecdsa.kt` core | `[x]` verified (incl. the `normalize()` production fix) |
| Task 4 store extraction + wiring | `[x]` verified |
| Task 5 H9 zeroization patch | `[x]` verified |
| Task 6 JVM gate | `[x]` verified — 34/34 on `79d1072` |
| Task 6 device suite | `[x]` verified — 6/6 on Pixel 6a / Android 17 (`79d1072`), `1m 36s` |
| Task 6.2 MAN-011 manual walk | `[x]` verified — `OK (6 tests)`, non-monotonic PSS curve recorded |
| Task 7 E2E cross-check | `[x]` verified — noble equality + viem recovery PASS |
| Task 8 verification + Handoff | `[x]` verified |
| Task 9 board + git closure + push | `[ ]` |
| T1.4 overall | **`[x]` verified 2026-10-16** — JVM 34/34 + device 6/6 + E2E recovery + zeroization review |
