# T1.4 — vault-core Android signDigest/destroy (plan)

> Feature spec (plan). Branch: `feature/T1.4-vault-core-android-sign-digest`.
> Parent: `plans/3-day-implementation-plan.md` Day 1 T1.4（关键路径，四方法闭环 G1）.
> Skill: `nexvault-t1-4-vault-core-android-sign-digest` (feature-dir `SKILL.md`, sha256
> `b0dbe67a1427adbf1a6a0add79dbbd049d3b045b484729e964666cbb7ea1f9b2`).
> Session status: planning written 2026-10-16 (this file); implementation executes in a later
> session after `GATE-MODEL-SWITCH` (owner switches the model to V4.1-Flash).

## Hard gates (read before any work)

- **GATE-MODEL-SWITCH** — the planning session commits the spec files and **stops**; the
  implementation session runs only after the owner switches the model to **V4.1-Flash** (D10).
- **No new dependency** — `bcprov-jdk18on:1.86` is already R4-approved (T1.3). No
  `build.gradle`, `specs/tech-stack.md` or `doc/dependencies.md` edit is expected; if any
  becomes necessary, **stop and ask** (R4 = 修宪).
- **GATE-REFERENCE-ORIGINAL-REPO (owner rule Q8)** — non-RN/TS/Expo questions resolve from the
  original repo `/home/superguo/Projects/NexVault` (reference-only; e.g.
  `data/src/main/java/com/nexvault/wallet/data/wallet/SigningKeyProvider.kt` — web3j-based,
  never copied). Record the consulted file in `validation.md`; if still unresolved, stop and
  ask the owner.
- **H9 first** — the zeroization patch is applied before any test run (Task 5 precedes the
  suites in Task 6), so both sides sit on the patched revision.
- No iOS/web implementation; `signDigest`/`destroy` only; the four-method API is frozen
  (R1) — no signature changes to T1.2/T1.3 methods.
- No `E_VAULT_SECURITY_GATE` wiring, no mlock, no at-rest wrap (security-android items).

## Task 0 — Preflight and baseline recording

Commands (record command + exit code + observed output in `validation.md` Task 0):

```bash
cd /home/superguo/Projects/NexVaultRN
git status --short                    # baseline on the feature branch
git branch --show-current             # must be feature/T1.4-vault-core-android-sign-digest
cd apps/mobile
npx expo-modules-autolinking search --platform android --json | grep vault-core   # must resolve
# Jar inspection (T1.3 lesson — verify, don't assume):
JAR=$(find ~/.gradle -name 'bcprov-jdk18on-1.86.jar' | head -1)
unzip -l "$JAR" | grep -cE 'ECDSASigner|DSAKCalculator'   # crypto/signers present
unzip -l "$JAR" | grep -E 'ECPoint|ECAlgorithms|HMac|KeccakDigest' | head
sha256sum "$JAR"                                          # vs Maven Central .sha256
# RFC 6979 ground truth:
curl -fsSL --max-time 30 https://www.rfc-editor.org/rfc/rfc6979.txt -o /tmp/rfc6979.txt
sha256sum /tmp/rfc6979.txt
# note in validation.md: A.2.5 official vectors are NIST P-256, NOT secp256k1 — never a gate
```

Cross-check tooling (throwaway only — zero repo drift):

```bash
# reuse the validated dir if present, else recreate:
[ -d /tmp/t14-toolcheck ] || { mkdir -p /tmp/t14-toolcheck && cd /tmp/t14-toolcheck &&   npm init -y && npm i @noble/curves@2 @noble/hashes@2 viem@2.56.9; }
node -e "console.log(require('/tmp/t14-toolcheck/node_modules/@noble/curves/package.json').version, require('/tmp/t14-toolcheck/node_modules/viem/package.json').version)"
```

GATE-REFERENCE-ORIGINAL-REPO: read `SigningKeyProvider.kt` (web3j `Credentials` —
reference-only, never copied).

Acceptance: branch name matches B.3 regex; autolinking resolves `vault-core`; jar greps show
the ECDSA/HMac classes with the sha256 recorded; RFC 6979 fetched with sha256 recorded and the
A.2.5 trap noted; noble/viem versions recorded; `GATE-MODEL-SWITCH` state recorded (owner
model switch happened).

## Task 1 — Spec check (no amendment expected)

Record that `specs/vault-core.md` already covers this feature: §2.1 TS signatures
(`signDigest` 65B `r‖s‖recid` 取值 0/1; `destroy` idempotent), §2.2/§2.3 Kotlin/iOS
`AsyncFunction` blocks, §2.4 `E_VAULT_INVALID_DIGEST_LENGTH` row, TC-VC-004/005.
Acceptance: a table in `validation.md` citing the exact sections; **no spec amendment**. If a
gap surfaces, stop and ask — never silently amend.

## Task 2 — Pin the TS interface (no dependency)

1. `modules/vault-core/src/VaultCoreModule.ts`:

```typescript
import { NativeModule, requireNativeModule } from 'expo';

declare class VaultCoreModule extends NativeModule<{}> {
  importMnemonic(bytes: Uint8Array): Promise<string>;
  deriveAddress(handle: string, path: string): Promise<string>;
  signDigest(handle: string, path: string, digest: Uint8Array): Promise<Uint8Array>; // 65B
  destroy(handle: string): Promise<void>;
}

export default requireNativeModule<VaultCoreModule>('VaultCore');
```

2. `modules/vault-core/index.ts`: add the `signDigest` and `destroy` wrappers with the
   T1.2/T1.3 fail-closed guard pattern (`error.code` machine-readable). `signDigest`:
   guard → `digest.byteLength !== 32` fast-fail (`error.code =
   'E_VAULT_INVALID_DIGEST_LENGTH'`) → `new Uint8Array(digest)` → native call → exact-65
   `Uint8Array` copy → `copy.buffer`. **No `fill(0)` on the digest** (public tx hash).
   `destroy`: guard → native call, `Promise<void>`. Keep `importMnemonic`/`deriveAddress`
   untouched.
3. Extend the throwaway `/tmp` Node harness (T1.2 review-fix NIT-4 pattern): `STUB_MODE=missing`
   → fixed-code throw for both ops; `STUB_MODE=present` → passes handle/path/digest through;
   digest-31 → `E_VAULT_INVALID_DIGEST_LENGTH`. Record all outputs.

Rules: no `VaultHandle = number`; do not touch `VaultCoreModule.web.ts`; no other method.
Record `git diff` and a `grep -n 'signDigest\|destroy'` result in `validation.md`.

## Task 3 — `Ecdsa.kt` (pure Kotlin core)

Create `modules/vault-core/android/src/main/java/expo/modules/vaultcore/Ecdsa.kt`
(framework-light `internal` functions; no `android.*` imports):

```kotlin
internal class Rfc6979Drbg(x: ByteArray, digest32: ByteArray)
internal fun signDigestWithKey(privKey32: ByteArray, digest32: ByteArray): ByteArray // 65B
internal fun signDigest(seed: ByteArray, pathIndices: IntArray, digest32: ByteArray): ByteArray
internal fun requireDigest32(digest: ByteArray)
```

Algorithm checklist (must):
- **Rfc6979Drbg** — RFC 6979 §3.2 over HMAC-SHA256: `h1 = int2octets(bits2int(digest32) mod n)`
  (32-byte BE); `K = 0x00×32`, `V = 0x01×32`;
  `K = HMAC(K, V‖0x00‖x‖h1); V = HMAC(K, V); K = HMAC(K, V‖0x01‖x‖h1); V = HMAC(K, V)`;
  `nextK()`: h-loop (`V = HMAC(K,V); T = V; k = bits2int(T) mod n; 1 ≤ k < n`);
  `retry()`: h3 continuation (`K = HMAC(K, V‖0x00); V = HMAC(K, V)`) for caller-reported
  r/s==0; attempts capped at 64 → `CryptoInternalException`; `wipe()` zeroes K, V, h1 and
  the x copy. The constructor copies `x` and never retains the caller's array.
- **signDigestWithKey** — `d = BigInteger(1, privKey32)`, `z = BigInteger(1, digest32)`,
  `n = CustomNamedCurves.getByName("secp256k1").n`; loop: `R = G.multiply(k)` (BC point op);
  `r = R.affineX mod n`; retry if `r == 0` **or `R.affineX ≥ n`** (keeps recid ∈ {0,1}
  recoverable) or `s = k.modInverse(n) · (z + r·d) mod n == 0`; **low-s**:
  `s > n/2 → s = n − s`; `recid = if (R.affineY.testBit(0)) 1 else 0` **flipped with the low-s negation (`recid xor 1`)**; output
  `r(32BE) ‖ s(32BE) ‖ recid`; `finally { drbg.wipe(); privKeyCopy.fill(0) }`.
- **signDigest** — derive the child node via `Bip32.masterNode`/`deriveChild`; normalize
  `Node.key.toByteArray()` to 32-byte BE (strip the 33-byte leading sign byte); wipe key
  bytes and every chain code in `finally`; delegate to `signDigestWithKey`.
- **requireDigest32** — size != 32 → `InvalidDigestLengthException`; called by the module
  **before** the store lookup (fail-fast) and again inside `signDigest`.
- BC lightweight API only; never register `BouncyCastleProvider`; no self-written point math;
  no `Log.*`; error text never interpolates key material.
- BigInteger caveats: `d`, `s`, `k`, `R` immutable → **[C]** in the zeroization review.

Record the file diff and the zeroization checklist in `validation.md` Task 3.

## Task 4 — Handle-store extraction + Expo module wiring

1. New `modules/vault-core/android/src/main/java/expo/modules/vaultcore/HandleStore.kt`:
   `internal class HandleStore` wrapping `ConcurrentHashMap<String, ByteArray>` with
   `put(seed): String` (UUID), `get(handle): ByteArray?`, `evict(handle): ByteArray?`
   (= `remove`), `size()` — direct JVM/device testability without the JS bridge. KDoc notes
   the evict-then-wipe race (in-flight ops hold their own `copyOf()`).
2. `modules/vault-core/android/src/main/java/expo/modules/vaultcore/VaultCoreModule.kt`:

```kotlin
private val handleStore = HandleStore()   // replaces the inline ConcurrentHashMap

AsyncFunction("signDigest") Coroutine { handle: String, path: String, digest: ByteArray ->
  withContext(Dispatchers.Default) {
    Ecdsa.requireDigest32(digest)                              // fail fast, before the store
    val seed = handleStore.get(handle) ?: throw HandleNotFoundException()
    val indices = Bip32.parseEthereumPath(path)                // E_VAULT_INVALID_PATH
    val seedCopy = seed.copyOf()
    try { Ecdsa.signDigest(seedCopy, indices, digest) } finally { seedCopy.fill(0) }
  }
}

AsyncFunction("destroy") Coroutine { handle: String ->
  withContext(Dispatchers.Default) {
    handleStore.evict(handle)?.fill(0)   // evict first, then wipe — idempotent, never throws
    Unit
  }
}
```

3. `modules/vault-core/android/src/main/java/expo/modules/vaultcore/VaultExceptions.kt`: add
   `InvalidDigestLengthException` (`E_VAULT_INVALID_DIGEST_LENGTH`, fixed template, no
   fragments) — `CodedException` like T1.2/T1.3.

Rules: `importMnemonic`/`deriveAddress` untouched; unknown handle must not distinguish
"empty store" from "bad handle"; no `Log.*`.

## Task 5 — H9 zeroization patch (apply BEFORE any test run)

1. `Bip32.kt` `deriveChild`: `data.fill(0)` moves into `finally` —
   `val i = try { hmacSha512(parent.chainCode, data) } finally { data.fill(0) }`.
2. `Bip32.kt` `toChecksumAddress`: wipe the `body.toByteArray(US_ASCII)` temp
   (`try/finally` around the keccak call; the derived Strings stay as the documented
   public-data caveat).
3. Zeroization-only edits — no derivation/API change. Record the exact diff; **both** the JVM
   gate and the device suite run on this patched revision (2026-10-16 round-2 lesson).

## Task 6 — Tests (vectors and cross-checks first)

### 6.1 JVM `EcdsaTest.kt` + `HandleStoreTest.kt` (mandatory automated gate)

Create under `modules/vault-core/android/src/test/java/expo/modules/vaultcore/` (layout
mirrors `src/main`; `// TC-VC-XXX` marker above each `@Test`):

| Test | Marker | Assertion |
| --- | --- | --- |
| `drbgDeterminismAndRange` | TC-VC-019 | same (x, digest) twice → identical k bytes; k ∈ [1, n−1]; different digest → different k |
| `probeVectorKAT` | TC-VC-020 | d/digest as pinned in requirements.md → r/s/recid exactly (sha256("sample") digest = `AF2BDBE1…ADD1BF`) |
| `apiDeterminismAndFormat` | TC-VC-021 | canonical seed @ `m/44'/60'/0'/0/0` + digest keccak256("nexvault-test-vector") twice → byte-identical; length 65; recid ∈ {0,1}; s ≤ n/2 |
| `e2ePinnedSignature` | TC-VC-022 | same signature equals the r‖s‖recid values pinned by the /tmp noble/viem script (Task 7 — transcribe only after the script ran) |
| `rejectsInvalidDigestLength` | TC-VC-023 | digest 0/31/33 bytes → `E_VAULT_INVALID_DIGEST_LENGTH`; bad path → `E_VAULT_INVALID_PATH` (via `Bip32`) |
| `destroyEvictsWipesIdempotent` | TC-VC-024 | `HandleStore`: put → evict wipes in place and returns true; `copyOf` → null; second evict → false (no throw); 1000× put/evict loop → size 0, process alive |
| `copyOfIsIndependentOfTheStoredBuffer` | TC-VC-024 | `copyOf` hands out a distinct array; wiping the copy leaves the stored seed readable (review MINOR-1) |
| `pinnedVectorsWithAndWithoutLowSNormalization` | TC-VC-027 | 6 pinned vectors (3 normalizing / 3 not): exact 65-byte equality + recovery ⇒ signing key; vector -2's `s` starts with `0x00` (BLOCKER-1 gate) |
| `recidRecoversTheSigningKeyForManyDigests` | TC-VC-028 | 24 digests: BC-based recovery from (r,s,recid) equals the signer's public key; pinned table must cover both branches |

### 6.2 Instrumentation `VaultCoreModuleTest.kt` (device, TC-VC-025)

- `moduleDefinitionExposesAllFourFunctions` — importMnemonic + deriveAddress + signDigest +
  destroy registered (**no map-size pin** — T1.2 lesson).
- `signAndDestroyOnDevice` — canonical seed/path/digest via `Bip39`/`Bip32`/`Ecdsa`
  (direct calls, honestly labeled): 65 bytes, recid ∈ {0,1}, r‖s equals the pinned values;
  `HandleStore` evict + wipe + idempotency on-device.
- `thousandRoundLoop` — 1000× import→derive→sign→destroy; **post-GC settled PSS**
  (`Debug.MemoryInfo.totalPss`, 3-sample average) after a 100-round warm-up, tolerance ±5%;
  no crash. (Superseded wording: this task originally specified `/proc/self/status` VmRSS; the
  2026-10-16 review round records the protocol change — see the trailing note and validation.md
  §Review round. The ±5% tolerance is unchanged.)
- The existing 4 tests stay green.
- **Honest scope label (FR-9):** this exercises the same code the AsyncFunction runs, **not**
  through the JS runtime — the bridge smoke is T1.3-H8, owner-adjudicated replan → T4.1. Never
  claim bridge coverage here.

### 6.3 Gradle runs

```bash
cd apps/mobile
npx expo prebuild --platform android --no-install   # CNG; no hand edits
cd android
./gradlew projects                                   # verify the :vault-core project path
./gradlew :vault-core:testDebugUnitTest --rerun      # mandatory JVM gate (26 existing + new)
ANDROID_SERIAL=<serial> ./gradlew :vault-core:connectedDebugAndroidTest   # device suite
```

Pin `ANDROID_SERIAL` (T1.2 duplicate-transport lesson). If no device is attached: record it,
the device gate reopens as Handoff T1.4-H, and the JVM gate remains the mandatory automated
evidence (Q3).

## Task 7 — E2E cross-check (throwaway /tmp, zero repo drift)

- Write `/tmp/t14-xcheck/t14-e2e.mjs`: noble@2 derives the canonical privkey
  (`abandon×23 about` @ `m/44'/60'/0'/0/0`), signs keccak256("nexvault-test-vector")
  (`prehash:false` → 64-byte compact, low-s default); asserts the Kotlin `r‖s` (input hex
  from the JVM run) byte-equals noble's compact; then **await** viem 2.56.9
  `recoverPublicKey({hash, signature: {r, s, yParity: recid}})` → address ==
  `0x9858EfFD232B4033E47d90003D41EC34EcaEda94`.
- Record script + full output in `validation.md`; transcribe the pinned full 65-byte signature
  into the JVM test only after this run (never transcribe blind).
- **Review round (MAJOR-1) amendment:** the script must derive the recovery id **independently**
  (trial yParity 0/1 against the address derived from the private key) and compare the **full
  65-byte** signature for **every** pinned vector — never parse a recid back out of `EcdsaTest.kt`;
  that is what made the original check blind to the BLOCKER-1 rule error.

## Task 8 — Verification, MAN-011, evidence, Handoff

```bash
cd /home/superguo/Projects/NexVaultRN
git diff -- package.json apps/mobile/package.json pnpm-lock.yaml   # zero npm drift
grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile --include='*.ts' --include='*.tsx' --exclude-dir=node_modules | grep -v 'packages/vault-core' && exit 1
grep -rnE 'Log.|println' modules/vault-core/android/src           # R5 native
cd apps/mobile && npx --no-install tsc --noEmit                    # exit 0
```

- Zeroization review (recorded item by item in `validation.md` Task 8): DRBG K/V/h1/x, k
  buffers, privkey copies, seed copies, chain codes, digest scratch, destroy path — each
  `fill(0)` with file:line; `[C]` rows for BigInteger intermediates and BC `HMac` pad
  state; the H9 patch rows move to PASS.
- Copy device evidence into the feature `evidence/` dir (XML/pb/log + device-info.md —
  T1.3 NIT-3 pattern).
- Register **MAN-011** in `specs/pending-manual-test-cases.md` (TC-VC-005 manual side:
  dumpsys meminfo / PSS-RSS walk on Pixel 6a; 走查记录格式 per B.4); walk it if the device is
  attached; iPhone TC-VC-005 side → T1.5 Handoff.
- Write the final T1.4 Handoff list (T1.4-H1..H5 from `requirements.md`). T1.4 is never
  marked `[x]` unless the JVM gate output, the E2E cross-check and the zeroization review are
  all recorded with command + output and both sides sit on the same committed revision (B.4-3).

## Task 9 — Board + git closure (B.3) + push

- AGENTS.md Part A: update the 自动化验证 cell (T1.4 JVM/device counts) and prepend the
  最近关闭记录 row (date + evidence absolute paths; merge hash + branch deletion; Handoff
  dispositions).
- Commits on the branch grouped as: spec trio (this planning session) · implementation ·
  tests · evidence/docs/board.
- `git merge --no-ff` into `main` using `-F /tmp/t14-merge-msg.txt` (the T1.3 lesson:
  `-F -` fails); delete the feature branch; board-line commit on `main`.
- **Push `origin/main`** (Q6) — `git push origin main`; record output.

## File manifest (T1.4)

| File | Action |
| --- | --- |
| `specs/features/2026-10-16-T1.4-vault-core-android-sign-digest/{requirements,plan,validation}.md` + `SKILL.md` | create (planning session) |
| `modules/vault-core/src/VaultCoreModule.ts`, `modules/vault-core/index.ts` | modify (Task 2) |
| `modules/vault-core/android/.../Ecdsa.kt`, `HandleStore.kt` | create (Tasks 3/4) |
| `modules/vault-core/android/.../VaultCoreModule.kt`, `VaultExceptions.kt` | modify (Task 4) |
| `modules/vault-core/android/.../Bip32.kt` | modify (Task 5 — H9 zeroization-only) |
| `modules/vault-core/android/src/test/.../EcdsaTest.kt`, `HandleStoreTest.kt` | create (Task 6) |
| `modules/vault-core/android/src/androidTest/.../VaultCoreModuleTest.kt` | modify (Task 6) |
| `specs/pending-manual-test-cases.md` (MAN-011), `AGENTS.md` Part A | modify (Tasks 8/9) |
| `specs/vault-core.md`, `specs/tech-stack.md`, `doc/dependencies.md`, `build.gradle`, `VaultCoreModule.web.ts` | **unchanged** (Task 1 evidence) |

## Edge cases & failure modes

- digest ≠ 32 → `E_VAULT_INVALID_DIGEST_LENGTH` before any store access (module + core).
- k out of range / r==0 / s==0 / x(R)≥n → RFC 6979 §3.2 h.3 continuation (reject `bits2int(T) ≥ n`, never reduce mod n — review NIT-2); 64-attempt cap → `E_VAULT_CRYPTO_INTERNAL`.
- `BigInteger.toByteArray()` sign byte → normalize to 32-byte BE.
- destroy during in-flight derive/sign → stored seed evicted+wiped; in-flight op finishes on
  its own copy; later calls throw `E_VAULT_HANDLE_NOT_FOUND`.
- viem `recoverPublicKey` is async (await); RFC6979 A.2.5 is P-256 (never used).
- no device → Handoff T1.4-H, JVM gate mandatory; duplicate adb transports → `ANDROID_SERIAL`
  pinned; VmRSS flakiness → 3-sample post-GC average, raw numbers recorded.
- noble low-s default matches our normalization (proven by the toolcheck probe).
- Never weaken a test to fix a failure (B.6-5); never claim bridge coverage (H8 → T4.1).

## Risks

| Risk | Trigger | Response |
| --- | --- | --- |
| No device at implementation | `adb devices` empty | Device gate → Handoff T1.4-H; JVM gate stays mandatory (Q3) |
| DRBG byte-order bug | probe vector or noble equality fails | Fix the DRBG; never touch the test vectors (B.6-5) |
| Prebuild drift | `expo prebuild` updates scripts | Record diff; no dependency change is acceptable |
| Expo ByteArray return shape surprise | native return ≠ 65-byte Uint8Array | Exact-65 copy in the TS wrapper (skill-mandated) |
| ±5% VmRSS tolerance noise | GC timing | 3-sample post-GC average protocol; record raw numbers |
| Toolcheck versions drifted | /tmp dir removed or versions bumped | Recreate with the pinned `@noble/curves@2 @noble/hashes@2 viem@2.56.9`; record the versions actually used |
