# NexVault T1.4 — vault-core Android signDigest/destroy (combined skill)

## Provenance

No single published skill covered all of T1.4, so this skill combines the best parts of the
following verified sources (same combine pattern as the T1.2/T1.3 skills):

| Source | What it contributes to T1.4 | Gap it leaves |
|---|---|---|
| Expo official [`expo-module`](https://github.com/expo/skills/blob/main/plugins/expo/skills/expo-module/SKILL.md) skill | Module DSL, `AsyncFunction` wiring incl. `ByteArray`↔`ArrayBuffer` return, autolinking | No signing domain, no test discipline |
| [`android-testing-skills`](https://github.com/skydoves/android-testing-skills) (JVM + instrumentation sets) | JUnit4 organization, AndroidJUnit4 device workflow, Gradle/ADB evidence discipline | No ECDSA/RFC6979 content |
| [claude-dev-suite `bitcoin-ecdsa`](https://github.com/claude-dev-suite/claude-dev-suite/blob/main/skills/bitcoin/cryptography/ecdsa/SKILL.md) skill | **Best crypto find**: sign/verify formulas, RFC6979 k discipline, low-s normalization (BIP146), recovery-id layout, the k-reuse private-key-leak warning, r/s==0 retry rules | Bitcoin-flavored; no Kotlin/BouncyCastle/Expo workflow |
| [RFC 6979](https://www.rfc-editor.org/rfc/rfc6979.txt) §2.3.2–2.5 / §3.2 | HMAC-SHA256 DRBG construction (int2octets, bits2octets, the K/V loop, the h3 retry continuation) | **Trap (verified 2026-10-16): Appendix A.2.5 official vectors are NIST P-256, NOT secp256k1** — for secp256k1, x=C9AFA9D8… + sha256("sample") gives r=432310e3… (≠ the RFC's EFD48B2A…). Never gate a secp256k1 signer on A.2.5 directly |
| [j4flmao `blockchain-cryptography`](https://github.com/j4flmao/agent-skills/blob/main/skills/blockchain/blockchain-cryptography/SKILL.md) | Nonce-reuse / signature-malleability / known-answer-test checklist discipline | Shallow on concrete implementation rules |
| BouncyCastle `bcprov-jdk18on:1.86` jar (inspected locally 2026-10-16) | Present: `crypto/signers` (129 entries incl. `ECDSASigner`, 3×`DSAKCalculator`), `ECPoint`/`ECAlgorithms`/`CustomNamedCurves`, `HMac`, `SHA256Digest`, `KeccakDigest`. Absent: `HDKeyDerivation`/`HDKey`/`Base58` (T1.3 lesson — verify the jar, never trust memory) | Reference material |
| `@noble/curves@2` + `viem@2.56.9` (validated in `/tmp/t14-toolcheck`, 2026-10-16) | Cross-check tooling semantics, evidence-backed: noble v2 `sign(digest, key, {prehash:false})` → deterministic 64-byte compact r‖s (low-s default); viem 2.56.9 `recoverPublicKey({hash, signature:{r,s,yParity}})` is **async**, hex-shaped, accepts yParity 0/1 (probe: recid=0 recovered the true address) | Tooling, not an agent skill |
| NexVault `AGENTS.md` R1/R4/R5/B.6 + T1.2/T1.3 validation records | Handle-store reuse, zeroization-review pattern, H9 patch (rolled back pending a device re-run), evidence/ durability pattern (NIT-3), error whitelist §2.4 | Project-specific |

Rejected as primary sources: Trust Wallet `wallet-core` skill (library banned — `specs/vault-core.md` §6, CVE-2025-66692), a5c-ai crypto references (index of links only), Garl-Protocol / MCP-ecdsa (MCP servers, not skills), RFC 6979 A.2.5 vectors as a direct gate (P-256 trap above), and the original NexVault repo's web3j `Credentials`/`Sign.signMessage` path (`SigningKeyProvider.kt` — reference-only per GATE-REFERENCE-ORIGINAL-REPO; RN spec mandates packaged BouncyCastle).

## Scope

- Implement **only** `signDigest(handle, path, digest32) → 65B r‖s‖recid` (RFC6979 deterministic
  ECDSA, recid ∈ {0,1}, low-s) and `destroy(handle)` (idempotent eviction + full zeroization) —
  this closes the four-method native loop (G1).
- **Do not** add iOS/web implementations (web stub keeps failing closed with
  `E_VAULT_UNSUPPORTED_PLATFORM`).
- **No new dependency**: BouncyCastle `bcprov-jdk18on:1.86` is already R4-approved (T1.3);
  reuse it. `@noble/curves`/viem live only in throwaway `/tmp` scripts (zero repo drift).
- Owner decisions 2026-10-16: **self-written RFC6979 K-gen + BC point ops**; handoff bundle
  confirmed (H1 implement · H2→T1.5 · H5→T1.1 closure · H6 implement · H7→future parser feature ·
  H8→T4.1 record-only · H9 implement with device re-run); `/tmp` cross-check tooling approved;
  GATE-MODEL-SWITCH = keep V4.1-Flash.

## Invariants (MUST)

1. **R1 boundary**: the private key never crosses JS↔native in either direction; `signDigest`
   returns only the signature bytes; `destroy` wipes native-side materials. The JS side does
   **not** `fill(0)` the digest input (it is a public tx hash, not key material) but keeps the
   ArrayBuffer-only wrapper shape.
2. **RFC6979 determinism**: same (handle, path, digest) → byte-identical output (TC-VC-004).
   Never accept an external nonce (k reuse leaks the private key).
3. **recid ∈ {0,1} identifies the recovery point** (review BLOCKER-1 corrected the original
   rule here — it wrongly claimed recid is unaffected by normalization): start from the y-parity
   of R, then normalize to low-s (`s > n/2 → s = n − s`) **and flip recid whenever s is negated**
   — negating s negates the recovery point, exactly as libsecp256k1's recoverable signing does.
   Verified by recovery, not by parity alone: the public key recovered from `(r, s, recid)` must
   equal the signing key for every digest, and the pinned vector set must cover both the
   normalizing and the non-normalizing branch.
4. **Retry rules** (bitcoin-ecdsa skill): k must satisfy 1 ≤ k < n; retry the DRBG h3
   continuation if r == 0 or s == 0; cap retries and fail with `E_VAULT_CRYPTO_INTERNAL`.
5. **BC lightweight API only** (`org.bouncycastle.crypto.*`); **never** register a JCE provider.
6. **R5**: no `Log.*`/`console.*` with key material; error messages use the §2.4 whitelist
   fixed templates (`E_VAULT_INVALID_PATH`, `E_VAULT_INVALID_DIGEST_LENGTH`,
   `E_VAULT_HANDLE_NOT_FOUND`, `E_VAULT_CRYPTO_INTERNAL`, …); no input fragments.
7. **B.6**: every step records its command and observed output in
   `specs/features/2026-10-16-T1.4-vault-core-android-sign-digest/validation.md`; no `[x]`
   without command + observed result; never weaken a failing test.
8. **Zeroization**: byte-level control over k (ByteArray DRBG state, `fill(0)` in `finally`),
   the derived private-key bytes, seed copies, digest scratch. BigInteger intermediates (d, s, R,
   k-as-BigInteger) are immutable → documented **[C]** caveat, never [P]. **Re-apply the T1.3-H9
   patch** (`data.fill(0)` in `finally` in `Bip32.kt` hardened branch; EIP-55 ASCII temp) in
   this feature and re-run the device suite on the patched revision.
9. **destroy idempotent**: second `destroy` does not throw; after destroy, any call with the
   old handle throws `E_VAULT_HANDLE_NOT_FOUND`.

## Preflight (Step 0)

```bash
cd /home/superguo/Projects/NexVaultRN
git status                       # record baseline
git switch -c feature/T1.4-vault-core-android-sign-digest   # B.3 branch discipline
cd apps/mobile
npx expo-modules-autolinking search --platform android --json | grep vault-core
```

Then, with command + output recorded:

- Re-read the T1.3 validation Handoff list and §Code review round; adjudicate H1–H9 per the
  owner bundle above and transcribe into `requirements.md` (owner D-numbers).
- **Jar inspection (T1.3 lesson — verify, don't assume)**: locate the Gradle-cached
  `bcprov-jdk18on-1.86.jar`, run `unzip -l` greps for `ECDSASigner`/`DSAKCalculator`/`ECPoint`/`ECAlgorithms`/`HMac` and `sha256sum` vs Maven Central; record counts.
- Download RFC 6979, record `sha256sum`; note the A.2.5 P-256 trap (Provenance).
- GATE-REFERENCE-ORIGINAL-REPO: read `/home/superguo/Projects/NexVault/data/src/main/java/com/nexvault/wallet/data/wallet/SigningKeyProvider.kt` (web3j `Credentials` — reference-only).
- Set up the throwaway cross-check tooling (shape validated 2026-10-16 in `/tmp/t14-toolcheck`):

```bash
mkdir -p /tmp/t14-xcheck && cd /tmp/t14-xcheck && npm init -y
npm i @noble/curves@2 @noble/hashes@2 viem@2.56.9     # throwaway only — zero repo drift
```

## Step 1 — Pin the TS interface

`modules/vault-core/src/VaultCoreModule.ts`:

```typescript
declare class VaultCoreModule extends NativeModule<{}> {
  importMnemonic(bytes: Uint8Array): Promise<string>;
  deriveAddress(handle: string, path: string): Promise<string>;
  signDigest(handle: string, path: string, digest: Uint8Array): Promise<Uint8Array>; // 65B
  destroy(handle: string): Promise<void>;
}
```

`modules/vault-core/index.ts` (extend the T1.2/T1.3 fail-closed guard pattern — `error.code`
machine-readable):

```typescript
export async function signDigest(handle: VaultHandle, path: string, digest: ArrayBuffer): Promise<ArrayBuffer> {
  if (typeof VaultCoreModule.signDigest !== 'function') { /* throw E_VAULT_UNSUPPORTED_PLATFORM */ }
  if (digest.byteLength !== 32) { /* throw E_VAULT_INVALID_DIGEST_LENGTH (machine-readable code) */ }
  const out = await VaultCoreModule.signDigest(handle, path, new Uint8Array(digest)); // digest is public — no fill(0)
  const copy = new Uint8Array(65); copy.set(out);          // exact-65 contract (out.buffer may be pooled)
  return copy.buffer;
}

export async function destroy(handle: VaultHandle): Promise<void> {
  if (typeof VaultCoreModule.destroy !== 'function') { /* throw E_VAULT_UNSUPPORTED_PLATFORM */ }
  await VaultCoreModule.destroy(handle);
}
```

## Step 2 — RFC6979 K-gen + ECDSA core

Create `modules/vault-core/android/src/main/java/expo/modules/vaultcore/Ecdsa.kt` with pure
`internal` functions (JVM-testable, T1.3 Bip32.kt pattern). Use BC only for hashing (HMAC,
SHA256Digest) and point ops (`ECPoint.multiply`); write the DRBG glue yourself (owner decision
2026-10-16).

- `internal class Rfc6979Drbg(x: ByteArray, digest32: ByteArray)` — RFC 6979 §3.2:
  - `h1 = bits2octets(digest32)` = `int2octets(bits2int(digest32) mod n)` (32-byte BE);
  - K = 0x00×32, V = 0x01×32; K = HMAC(K, V‖0x00‖x‖h1); V = HMAC(K, V);
    K = HMAC(K, V‖0x01‖x‖h1); V = HMAC(K, V);
  - `nextK(): ByteArray` — T loop over `V = HMAC(K, V)`, `k = bits2int(T) mod n`;
    return the first `1 ≤ k < n`; on invalid k (or when the caller reports r/s == 0) run the h3
    continuation `K = HMAC(K, V‖0x00); V = HMAC(K, V)` and repeat; cap total attempts (~64) →
    `E_VAULT_CRYPTO_INTERNAL`.
  - expose `wipe()` to `fill(0)` K, V, h1, x copies — call in `finally`.
- `internal fun signDigestWithKey(privKey: ByteArray, digest32: ByteArray): ByteArray` (65B):
  - `n = CustomNamedCurves.getByName("secp256k1").n`; `d = BigInteger(1, privKey)`;
  - drbg loop: `R = G.multiply(k)`; `r = R.x mod n`; if `r == 0` → nextK; 
    `s = k.modInverse(n) * (z + r * d) mod n` with `z = BigInteger(1, digest32)`; if `s == 0`
    → nextK;
  - low-s: `recid = if (R.y.testBit(0)) 1 else 0`; if `s > n/2` then `s = n − s` **and
    `recid = recid xor 1`** (the flip is mandatory — the recovery point is negated);
  - output = 32-byte BE r ‖ 32-byte BE s ‖ recid byte;
  - `finally { drbg.wipe(); privKeyCopy.fill(0) }` — never pass the stored seed/privKey array
    directly into BigInteger without a copy.
- `internal fun signDigest(seed: ByteArray, pathIndices: IntArray, digest32: ByteArray): ByteArray`
  — reuse the T1.3 `Bip32` internals to derive the child private key bytes, `fill(0)` them in
  `finally`, then call `signDigestWithKey`.
- BigInteger caveat: d, s, k-as-BigInteger, R are immutable — record as **[C]** in validation.md.

## Step 3 — Module wiring (signDigest + destroy)

In `VaultCoreModule.kt` (reuse the T1.2 `handleStore`, typed exceptions,
`withContext(Dispatchers.Default)`):

```kotlin
AsyncFunction("signDigest") Coroutine { handle: String, path: String, digest: ByteArray ->
  withContext(Dispatchers.Default) {
    if (digest.size != 32) throw InvalidDigestLengthException()
    val seed = handleStore[handle] ?: throw HandleNotFoundException()
    val indices = Bip32.parseEthereumPath(path)                 // E_VAULT_INVALID_PATH
    val seedCopy = seed.copyOf()
    try { Ecdsa.signDigest(seedCopy, indices, digest) } finally { seedCopy.fill(0) }
  }
}
AsyncFunction("destroy") Coroutine { handle: String ->
  withContext(Dispatchers.Default) {
    handleStore.remove(handle)?.fill(0)   // evict first, then wipe — idempotent, never throws
    Unit
  }
}
```

- digest length is checked **before** touching the store (fail fast, no key material involved).
- No `Log.*`; fixed error templates only (R5).

## Step 4 — Tests (vectors and cross-checks first)

JVM `modules/vault-core/android/src/test/java/expo/modules/vaultcore/EcdsaTest.kt`:

1. **DRBG determinism + range**: same (x, digest) twice → identical k bytes; k ∈ [1, n−1];
   different digests → different k. `// TC-VC-XXX` traceability comments.
2. **Probe vector (validated 2026-10-16, /tmp/t14-toolcheck — NOT RFC A.2.5)**: d =
   `C9AFA9D845BA75166B5C215767B1D6934E50C3DB36E89B127B8A622B120F6721`,
   digest = sha256("sample") → expect
   r = `432310e32cb80eb6503a26ce83cc165c783b870845fb8aad6d970889fcd7a6c8`,
   s = `530128b6b81c548874a6305d93ed071ca6e05074d85863d4056ce89b02bfab69`,
   recid = 0. (Independent tools: noble `sign(digest, key, {prehash:false})` compact bytes +
   viem `yParity` trial recovered `0xe471a8d4E306eEFA74ba35DF7500a4512AeB542C`.)
3. **Determinism via the API path**: `signDigest` over the canonical seed
   (`abandon×23 about`) at `m/44'/60'/0'/0/0` with `keccak256("nexvault-test-vector")`
   twice → byte-identical; length 65; recid ∈ {0,1}; s ≤ n/2.
4. **E2E recovery cross-check (TC-VC-004)**: write the canonical signature hex to
   `/tmp/t14-xcheck/sig.txt`; the throwaway script asserts (a) first 64 bytes == noble's
   compact output for the derived privkey + digest, (b) viem
   `recoverPublicKey({hash, signature:{r,s,yParity: recid}})` → address ==
   `0x9858EfFD232B4033E47d90003D41EC34EcaEda94` (recid trial over 0/1). Record script + output
   in validation.md. viem 2.56.9's `recoverPublicKey` is **async** — await it.
5. **Negatives**: digest 0/31/33 bytes → `E_VAULT_INVALID_DIGEST_LENGTH`; bad path →
   `E_VAULT_INVALID_PATH`; unknown handle → `E_VAULT_HANDLE_NOT_FOUND`.
6. **destroy (H1)**: import → destroy → deriveAddress/signDigest throw
   `E_VAULT_HANDLE_NOT_FOUND`; destroy twice does not throw; 1000× import/destroy loop leaves
   `handleStore` empty and the process alive (JVM side of TC-VC-005).

Instrumentation `VaultCoreModuleTest.kt` (device):

- `moduleDefinitionExposes…` now asserts all four functions (**no map-size pin** — Expo's
  builder adds `startObserving`/`stopObserving`; T1.2 lesson);
- on-device sign + recid + destroy eviction + the 1000-loop smoke;
- record honestly: this exercises the same code the `AsyncFunction` runs, **not through the JS
  runtime** — the bridge smoke is T1.3-H8, owner-adjudicated replan → T4.1 (do not claim it here).

Manual side (B.4): TC-VC-005 PSS/RSS curve — `adb shell dumpsys meminfo` samples + profiler
screenshots on Pixel 6a / Android 17, registered as a MAN case in
`specs/pending-manual-test-cases.md`.

Gradle (exact project path after prebuild — verify with `./gradlew projects` first):

```bash
cd apps/mobile
npx expo prebuild --platform android --no-install
cd android
./gradlew :vault-core:testDebugUnitTest --rerun
ANDROID_SERIAL=<serial> ./gradlew :vault-core:connectedDebugAndroidTest
```

Pin `ANDROID_SERIAL` (T1.2 duplicate-transport lesson).

## Step 5 — Verification, H9 re-application, handoff

- Apply the T1.3-H9 zeroization patch (`data.fill(0)` in `finally`, EIP-55 ASCII temp) as an
  explicit early task, then run BOTH the JVM gate and the device suite on the patched revision so
  the two-sided evidence matches the committed revision (the 2026-10-16 round-2 lesson).
- Close-out reruns and greps:

```bash
cd /home/superguo/Projects/NexVaultRN
git diff -- package.json apps/mobile/package.json pnpm-lock.yaml   # prove zero npm drift
grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile --include='*.ts' --include='*.tsx' --exclude-dir=node_modules
grep -rnE 'Log.|println' modules/vault-core/android/src           # R5 native
cd apps/mobile && npx --no-install tsc --noEmit
```

- Zeroization review table (sign path: DRBG K/V/h1/x/k buffers, seed/privKey copies, digest
  scratch — each `fill(0)` location; destroy path: stored seed wiped; immutable BigInteger
  intermediates as **[C]**; BC `HMac` pad state as **[C]** — the T1.3-H9 caveat).
- Copy device evidence into the feature `evidence/` dir (T1.3 NIT-3 pattern).
- Handoff list: H8 carried forward (bridge smoke → T4.1 re-adjudication), noble/viem version
  pins used for the cross-check, TC-VC-005 manual-side status, remaining [C] caveats.
- Do not mark T1.4 `[x]` until the vector/determinism gates, the E2E recovery cross-check and
  the zeroization review are all recorded with command + output, and both sides (JVM + device +
  manual MAN) are on the same committed revision.
