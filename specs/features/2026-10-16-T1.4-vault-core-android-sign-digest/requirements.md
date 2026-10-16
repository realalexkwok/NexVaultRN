# T1.4 — vault-core Android signDigest/destroy (requirements)

> Feature spec (requirements). Planning round **2026-10-16**. Branch:
> `feature/T1.4-vault-core-android-sign-digest` (created from `main` `85dbf83` before this
> file was written).
> Parent roadmap item: `plans/3-day-implementation-plan.md` Day 1 T1.4（关键路径）— closes the
> four-method native loop (G1).
> Skill: `nexvault-t1-4-vault-core-android-sign-digest`（committed at
> `.agents/skills/nexvault-t1-4-vault-core-android-sign-digest/SKILL.md`, commit `85dbf83`;
> also copied in this feature directory as `SKILL.md`; both copies sha256
> `b0dbe67a1427adbf1a6a0add79dbbd049d3b045b484729e964666cbb7ea1f9b2`）.
> Sibling files: `plan.md`, `validation.md`.
> Session status: planning round **2026-10-16** wrote this record. Implementation starts only
> after `GATE-MODEL-SWITCH` (owner switches the model to V4.1-Flash).

## Goal

Implement **only** `signDigest(handle, path, digest32)` and `destroy(handle)` in
`modules/vault-core` (Android/Kotlin), closing the four-method native loop:

- **`signDigest`** — RFC6979 deterministic ECDSA over secp256k1. Input: 32-byte digest
  (keccak256 of the serialized transaction, produced by web3-adapter — this module never
  pre-hashes). Output: **65 bytes** `r(32) ‖ s(32) ‖ recid(1)` with
  **recid = y-parity of R ∈ {0,1}** (spec: vault-core §2.1 取值 0/1) and **low-s normalization**
  (`s > n/2 → s = n − s`, BIP146/EIP-2 style). Deterministic: same (handle, path, digest) →
  byte-identical output; the nonce is never externally supplied.
- **`destroy`** — idempotent eviction of the handle-store entry followed by `fill(0)` of the
  stored seed. Second `destroy` does not throw; after destroy every operation on the old
  handle throws `E_VAULT_HANDLE_NOT_FOUND` (spec: vault-core §2.4 幂等语义).
- **k-generation is self-written** (RFC 6979 §3.2 HMAC-SHA256 DRBG); **all curve arithmetic
  stays in BouncyCastle 1.86** (`CustomNamedCurves` secp256k1, `ECPoint.multiply`) —
  owner decision D9, consistent with spec: vault-core §2.2（点运算禁止自实现）.
- automated gates: the independently validated **probe vector KAT**, determinism/format tests,
  the **E2E recovery cross-check** (noble@2 byte-equality + viem@2.56.9
  `recoverPublicKey` → canonical address `0x9858EfFD232B4033E47d90003D41EC34EcaEda94`),
  destroy semantics + 1000-round loop, and the real-device instrumentation suite.
- H9 closure: the T1.3-H9 zeroization patch is applied **early** on this branch and both the
  JVM gate and the device suite run on the patched revision.

iOS stays T1.5; web keeps the fail-closed stub (R7); the JS→native bridge smoke stays T4.1
(H8, record only).

## Owner decisions (2026-10-16, given in the task instruction)

| # | Item | Disposition |
| --- | --- | --- |
| D1 | T1.3 Handoff H1 (`destroy` absent) | **Implement** this round |
| D2 | T1.3 Handoff H2 (iOS port) | **Replan → T1.5** |
| D3 | T1.3 Handoff H5 (JS wrapper harness) | **Replan → T1.1 closure**; T1.4 reuses the throwaway-harness pattern for wrapper evidence |
| D4 | T1.3 Handoff H6 (BC 1.86 pin) | **Implement** — reuse `bcprov-jdk18on:1.86`; **no new dependency**, no `build.gradle`/tech-stack/dependencies edits |
| D5 | T1.3 Handoff H7 (BIP-32 vector 5) | **Replan → future extended-key parser feature** |
| D6 | T1.3 Handoff H8 (JS→native bridge smoke) | **Replan → T4.1, record only** — device tests exercise the same code the AsyncFunction runs, honestly labeled, not through the JS runtime |
| D7 | T1.3 Handoff H9 (zeroization residuals) | **Implement with a device re-run** on the patched revision |
| D8 | Cross-check tooling | Throwaway `/tmp` script with **noble@2 + viem@2.56.9**; zero repo drift |
| D9 | RFC6979 k-gen | **Self-written HMAC-DRBG + BC point ops** (from the committed T1.4 SKILL.md) |
| D10 | Model switch | **GATE-MODEL-SWITCH = V4.1-Flash** — stop after the spec files are committed; implementation runs only after the owner switches the model |

## Ask-user round (C.1-2, all confirmed 2026-10-16)

| Q | Question | Decision |
| --- | --- | --- |
| Q1 | Signing semantics: self-written RFC6979 HMAC-DRBG + BC secp256k1 point ops; low-s normalization; recid = y-parity of R ∈ {0,1}; h3 retry on r==0 / s==0 / x(R)≥n, capped ~64 → `E_VAULT_CRYPTO_INTERNAL` | **Confirmed as stated** |
| Q2 | KAT anchors: (a) probe vector d=`C9AFA9D8…` + digest sha256("sample") → r/s/recid as pinned below; (b) E2E anchor — canonical seed @ `m/44'/60'/0'/0/0` + digest keccak256("nexvault-test-vector") → Kotlin r‖s == noble compact output, viem `recoverPublicKey(yParity=recid)` → `0x9858EfFD…Eda94`. RFC6979 A.2.5 (P-256) deliberately unused | **Confirmed as stated** |
| Q3 | Device scope: 4-function registration + on-device sign/recid (direct native calls, NOT through the JS runtime — H8 → T4.1) + destroy eviction/idempotency + 1000-round loop with VmRSS ±5%; no device → Handoff T1.4-H (JVM gate stays mandatory); MAN-011 registered; iPhone TC-VC-005 side → T1.5 | **Confirmed as stated** |
| Q4 | H9 patch applied as an explicit early task; both suites run on the patched revision | **Confirmed as stated** |
| Q5 | Planning session writes the spec trio + commits, then stops for the model switch to V4.1-Flash | **Confirmed** |
| Q6 | At closure: merge --no-ff → delete branch → board line → **push origin/main** | **Push at closure** |

## Reference precedence for unclarified questions (owner rule, carried from T1.2 Q8)

1. **RN/TS/JS/Expo topics**: this repo's `AGENTS.md`, `apps/mobile/AGENTS.md`, the T1.4 skill,
   and the Expo SDK 57 versioned docs are authoritative.
2. **All other topics** (Kotlin, Gradle, JUnit/AndroidX, RFC6979/ECDSA details, BouncyCastle
   usage): resolve by referring to the original NexVault repo at
   `/home/superguo/Projects/NexVault` — its `AGENTS.md`, `specs/tech-stack.md`,
   `gradle/libs.versions.toml`, and existing source/tests (`SigningKeyProvider.kt` is
   **reference-only**: web3j-based, never copied). When used, the implementation record must
   name the exact original-repo file consulted in `validation.md`.
3. **Conflict resolution**: this repo's `AGENTS.md` R1–R8/R4, the T1.4 skill, and this feature
   spec win.
4. **Still unresolved?** Stop and ask the owner before writing code — never invent an answer.

## Handoff re-read (AGENTS.md B.7 — every pre-existing candidate gets an explicit verdict)

| Source | Deferred entry | Verdict for T1.4 |
| --- | --- | --- |
| T1.3 `validation.md` H1 | `destroy` absent; handle store per process | **Implement → Task 4/6** (owner D1) |
| T1.3 `validation.md` H2 | iOS `deriveAddress` port | **Replan → T1.5** (owner D2) |
| T1.3 `validation.md` H3 | device instrumentation fallback | Closed in T1.3; T1.4 re-runs the device suite on its own revision (Q3) |
| T1.3 `validation.md` H4 | BIP-32 vector transcription provenance | Closed in T1.3 |
| T1.3 `validation.md` H5 | JS wrapper evidence via throwaway harness | **Replan → T1.1 closure** (owner D3); T1.4 reuses the harness pattern for its own wrapper evidence |
| T1.3 `validation.md` H6 | BC 1.86 pin carried to signing work | **Implement** (owner D4) — same coordinate, zero new dependency |
| T1.3 `validation.md` H7 | BIP-32 vector 5 (16 malformed extended keys) | **Replan → future extended-key parser feature** (owner D5) |
| T1.3 `validation.md` H8 | JS→native bridge invocation (= T1.2-H3) | **Replan → T4.1, record only** (owner D6; owner adjudication 2026-10-16) |
| T1.3 `validation.md` H9 | `data.fill(0)` outside `finally`; BC `HMac` pad state; EIP-55 ASCII temp | **Implement** (owner D7) — `data` fix + EIP-55 temp wipe in Task 5; BC `HMac` pad state stays a documented **[C]** caveat |
| AGENTS.md B.7-3 | iOS mlock semantics / word-grid keyboard / C++ core / Secure Enclave wrap | Unchanged — roadmap stage-2 items |
| `specs/vault-core.md` §7 | BIP-39 passphrase, multi-coin, Schnorr, StrongBox role | Unchanged — roadmap items |

## Functional requirements

- **FR-1 — TS interface and R1 boundary.** `modules/vault-core/src/VaultCoreModule.ts` adds
  `signDigest(handle: string, path: string, digest: Uint8Array): Promise<Uint8Array>` (65B)
  and `destroy(handle: string): Promise<void>` declarations next to the T1.2/T1.3 ones.
  `modules/vault-core/index.ts` exports `signDigest` with the fail-closed platform guard
  (`typeof VaultCoreModule.signDigest !== 'function'` → throw with
  `error.code = 'E_VAULT_UNSUPPORTED_PLATFORM'`) and a JS fast-fail
  `digest.byteLength !== 32` → throw with `error.code = 'E_VAULT_INVALID_DIGEST_LENGTH'`;
  passes `new Uint8Array(digest)`; **does not `fill(0)` the digest** (public tx hash, not key
  material — skill invariant 1); copies the native result into an exact-65 `Uint8Array` and
  returns `copy.buffer` (the native buffer may be pooled). `destroy` wrapper = guard only.
  Both wrapper branches (module present / absent) + the digest-31 fast-fail are executed via
  the throwaway `/tmp` Node harness (T1.2 review-fix NIT-4 pattern; H5 → T1.1). No key
  material crosses the boundary in either direction; the signature is public information (R1).
  `VaultCoreModule.web.ts` is not touched (R7 boundary).
- **FR-2 — `Ecdsa.kt` RFC6979 HMAC-DRBG.** New
  `modules/vault-core/android/src/main/java/expo/modules/vaultcore/Ecdsa.kt` (pure internal
  functions, framework-light, JVM-testable — the T1.3 `Bip32.kt` pattern).
  `internal class Rfc6979Drbg(x: ByteArray, digest32: ByteArray)` implements RFC 6979 §3.2
  over HMAC-SHA256: `h1 = int2octets(bits2int(digest32) mod n)` (32-byte BE; for the 256-bit
  order, bits2int is the plain BE integer); `K = 0x00×32`, `V = 0x01×32`;
  `K = HMAC(K, V‖0x00‖x‖h1); V = HMAC(K, V); K = HMAC(K, V‖0x01‖x‖h1); V = HMAC(K, V)`;
  `nextK()` runs the h-loop (`V = HMAC(K, V); T = V; k = bits2int(T) mod n; 1 ≤ k < n`);
  `retry()` applies the h3 continuation (`K = HMAC(K, V‖0x00); V = HMAC(K, V)`) for the
  caller-reported r/s==0 retry; total attempts capped at 64 → `CryptoInternalException`;
  `wipe()` zeroes K, V, h1 and the x copy. The constructor copies `x` (never retains the
  caller's array) and stores it for `wipe()`.
- **FR-3 — `signDigestWithKey`.** `internal fun signDigestWithKey(privKey32: ByteArray,
  digest32: ByteArray): ByteArray` (65B): `d = BigInteger(1, privKey32)`,
  `z = BigInteger(1, digest32)`, `n = CustomNamedCurves.getByName("secp256k1").n`; loop:
  `k` from the DRBG; `R = G.multiply(k)` (BC point op only — no self-written curve math);
  `r = R.affineX mod n`; retry (h3) if `r == 0` **or `R.affineX ≥ n`** (keeps
  recid ∈ {0,1} recoverable) or `s = k.modInverse(n) · (z + r·d) mod n == 0`; then **low-s**:
  `s > n/2 → s = n − s`; `recid = if (R.affineY.testBit(0)) 1 else 0`; output 32-byte BE r
  ‖ 32-byte BE s ‖ recid; `finally { drbg.wipe(); privKeyCopy.fill(0) }` — the caller-owned
  array is copied first and never passed straight into `BigInteger`.
- **FR-4 — `signDigest(seed, pathIndices, digest32)`.** Derives the child node via the
  existing `Bip32.masterNode`/`Bip32.deriveChild`, normalizes `Node.key.toByteArray()` to
  32-byte BE (strip the 33-byte leading sign byte), delegates to `signDigestWithKey`, and
  wipes the key bytes and every chain code in `finally`. `internal fun requireDigest32`
  throws `InvalidDigestLengthException` — called by the module **before** the store lookup
  (fail-fast, no key material involved) and again inside `signDigest` so the check is
  JVM/device-tested.
- **FR-5 — module wiring.** `VaultCoreModule.kt` adds
  `AsyncFunction("signDigest") Coroutine { handle: String, path: String, digest: ByteArray }`
  — `withContext(Dispatchers.Default)`; `requireDigest32(digest)` → store lookup →
  `Bip32.parseEthereumPath(path)` → `seed.copyOf()` →
  `Ecdsa.signDigest(seedCopy, indices, digest)` → `finally { seedCopy.fill(0) }`; returns
  the 65-byte array. `AsyncFunction("destroy") Coroutine { handle: String }` —
  `handleStore.evict(handle)?.fill(0)`; returns `Unit`, never throws (idempotent).
  `importMnemonic`/`deriveAddress` behavior unchanged. No `Log.*` (R5).
- **FR-6 — `HandleStore.kt` extraction.** The `ConcurrentHashMap<String, ByteArray>`
  moves into `internal class HandleStore` with `put(seed): String` (UUID),
  `get(handle): ByteArray?`, `evict(handle): ByteArray?` (= `remove`), `size()` — so
  destroy semantics are directly testable on JVM + device without the JS bridge (same honesty
  class as T1.3's direct `Bip39`/`Bip32` calls). Evict-then-wipe means an in-flight
  derive/sign finishes on its own `copyOf()` — documented in the KDoc + validation.
- **FR-7 — `InvalidDigestLengthException`.** `VaultExceptions.kt` adds
  `InvalidDigestLengthException` (`E_VAULT_INVALID_DIGEST_LENGTH`, fixed template, no input
  fragments) — the code already exists in spec: vault-core §2.4; no spec amendment needed.
- **FR-8 — H9 zeroization patch (apply BEFORE any test run).** `Bip32.kt` `deriveChild`:
  `data.fill(0)` moves into `finally` —
  `val i = try { hmacSha512(parent.chainCode, data) } finally { data.fill(0) }` (the hardened
  branch holds `ser256(parent.key)` if HMAC throws). `Bip32.kt` `toChecksumAddress`: wipe
  the `body.toByteArray(US_ASCII)` temp (`try/finally` around the keccak call; the derived
  Strings stay as the documented public-data caveat). Zeroization-only edits — no
  derivation/API change; **both** suites re-run on the patched revision (2026-10-16 round-2
  lesson).
- **FR-9 — tests.** JVM `EcdsaTest.kt` + `HandleStoreTest.kt` (`// TC-VC-019`..`024`
  markers per test, table in plan.md Task 6): DRBG determinism/range; probe vector KAT;
  determinism + format via the API path; E2E pinned signature; digest/path negatives; destroy
  eviction/wipe/idempotency + 1000× put/evict. Instrumentation `VaultCoreModuleTest.kt`
  (`// TC-VC-025`): all four AsyncFunctions registered (**no map-size pin** — T1.2 lesson);
  on-device canonical sign + recid + pinned r‖s; `HandleStore` evict/wipe/idempotency
  on-device; 1000-round import→derive→sign→destroy loop with `/proc/self/status` VmRSS
  (3-sample post-GC average, ±5% tolerance); the existing 4 tests stay green. **Honest scope
  label:** these exercise the same code the AsyncFunction runs, not through the JS runtime —
  the bridge smoke is H8, owner-adjudicated replan → T4.1 (never claimed here).
- **FR-10 — E2E cross-check.** Throwaway `/tmp/t14-xcheck/t14-e2e.mjs` (noble@2 +
  viem@2.56.9; zero repo drift): noble derives the canonical privkey
  (`abandon×23 about` @ `m/44'/60'/0'/0/0`), signs keccak256("nexvault-test-vector")
  (`prehash:false` → 64-byte compact, low-s default); asserts the Kotlin `r‖s` byte-equals
  noble's compact output; then **await** viem `recoverPublicKey({hash, signature: {r, s,
  yParity: recid}})` → address == `0x9858EfFD232B4033E47d90003D41EC34EcaEda94`. Script +
  full output recorded in `validation.md`; the pinned r/s/recid are transcribed into
  TC-VC-022 only after this run.
- **FR-11 — zero-dependency and static discipline.** No `build.gradle` change (BC 1.86 reuse,
  D4); zero npm drift; R1/R5/Log greps clean; `tsc --noEmit` exit 0; BC lightweight API only;
  never register a JCE provider; no self-written point math.
- **FR-12 — GATE-MODEL-SWITCH.** Recorded before any production-code edit (this planning
  session, AC-12); the implementation session starts at plan.md Task 0 only after the owner
  switches the model to V4.1-Flash.

## Vector anchors (locked before transcription)

- **Probe vector KAT (independently validated 2026-10-16 in `/tmp/t14-toolcheck` against
  noble@2 + viem@2.56.9; NOT RFC 6979 A.2.5 — that appendix is NIST P-256, not secp256k1):**
  - d = `C9AFA9D845BA75166B5C215767B1D6934E50C3DB36E89B127B8A622B120F6721`
  - digest = sha256("sample") =
    `AF2BDBE1AA9B6EC1E2ADE1D694F41FC71A831D0268E9891562113D8A62ADD1BF`
  - expect r = `432310e32cb80eb6503a26ce83cc165c783b870845fb8aad6d970889fcd7a6c8`
  - expect s = `530128b6b81c548874a6305d93ed071ca6e05074d85863d4056ce89b02bfab69`
  - expect recid = 0
- **E2E anchor:** canonical mnemonic `abandon×23 about` (empty passphrase) @
  `m/44'/60'/0'/0/0` with digest = keccak256("nexvault-test-vector") → Kotlin `r‖s` ==
  noble@2 compact output; viem `recoverPublicKey(yParity=recid)` → canonical address
  `0x9858EfFD232B4033E47d90003D41EC34EcaEda94` (TC-VC-003 anchor).
- RFC 6979 A.2.5 official vectors are **P-256** — never used as a secp256k1 gate (skill
  Provenance trap).

## Out of scope

- iOS implementation (T1.5; including the iPhone side of TC-VC-005).
- Web implementation (stub keeps failing closed with `E_VAULT_UNSUPPORTED_PLATFORM`; R7).
- The JS→native bridge smoke (T1.3-H8 = T1.2-H3) — owner-adjudicated **replan → T4.1**
  (dev-build screen smoke: onboarding import → `deriveAddress` through the AsyncFunction/JS
  bridge); T1.4 only records the carrier.
- jest/dev-build AC-3 carrier (T1.1 closure debt, H5).
- BIP-32 vector 5 (extended-key serialization validity) — no production parser (H7).
- mlock telemetry (TC-VC-009), `E_VAULT_SECURITY_GATE` wiring, at-rest key wrap,
  StrongBox/Enclave, BIP-39 passphrase — security-android / roadmap items.
- Any production dependency beyond the owner-approved `bcprov-jdk18on:1.86` (FR-11).

## Acceptance criteria (each AC binds a test/gate and evidence location)

| AC | Content | Test/gate |
| --- | --- | --- |
| AC-1 | TS interface: 4 native declarations; guarded `signDigest`/`destroy` wrappers with machine-readable `error.code`; exact-65 ArrayBuffer return; digest fast-fail; harness executes both branches | FR-1 + /tmp harness evidence |
| AC-2 | Probe vector KAT reproduced: d/digest → r/s/recid as pinned | TC-VC-020 (JVM) |
| AC-3 | Determinism + format: same (seed, path, digest) twice → byte-identical; length 65; recid ∈ {0,1}; low-s | TC-VC-021 (JVM) + TC-VC-025 (device) |
| AC-4 | E2E recovery: Kotlin r‖s == noble@2 compact output; viem `recoverPublicKey(yParity=recid)` → `0x9858EfFD232B4033E47d90003D41EC34EcaEda94` | TC-VC-022 + /tmp script (Task 7) |
| AC-5 | destroy: idempotent; post-destroy ops throw `E_VAULT_HANDLE_NOT_FOUND`; 1000× loop no crash, store empty | TC-VC-024 (JVM) + TC-VC-025 (device) |
| AC-6 | Typed errors `E_VAULT_INVALID_DIGEST_LENGTH` / `E_VAULT_INVALID_PATH` / `E_VAULT_HANDLE_NOT_FOUND` / `E_VAULT_CRYPTO_INTERNAL`; fixed templates, no fragments | TC-VC-023 (JVM) + TC-VC-025 (device) |
| AC-7 | H9 patch applied early; both suites on the patched revision; zeroization review lists every scratch buffer with file:line; `[C]` caveats for BigInteger + BC HMac pad state | TC-VC-026 review + Task 5 diff |
| AC-8 | Zero npm drift; `build.gradle` unchanged (BC 1.86 reuse); R1/R5/Log greps clean; `tsc --noEmit` exit 0 | Task 8 static checks |
| AC-9 | Device instrumentation green on Pixel 6a / Android 17 (`ANDROID_SERIAL` pinned): `BUILD SUCCESSFUL`, XML failures=0, 4-function registration + on-device sign + 1000-loop; honest scope label (H8 → T4.1); evidence copied to feature `evidence/`. No device → Handoff T1.4-H, JVM gate stays mandatory | TC-VC-025 |
| AC-10 | MAN-011 registered in `specs/pending-manual-test-cases.md` (TC-VC-005 manual side); iPhone TC-VC-005 side → T1.5 Handoff | Task 8 |
| AC-11 | `validation.md` contains command + exit code + observed output for every task; no `[x]` without evidence | B.4/B.6 audit |
| AC-12 | `GATE-MODEL-SWITCH` recorded before any code write; board line + git closure per B.3 + push `origin/main` at closure | FR-12 / plan Task 9 |

## Security-level labelling (R6 — no level inflation)

| Mechanism | Label | Reason |
| --- | --- | --- |
| Private key never crosses the JS↔native boundary; only the 65-byte signature is returned | **[P]** (within T1.4 scope) | Enforced by API shape + store + CI greps |
| RFC6979 determinism — the nonce is never externally supplied | **[P]** (within T1.4 scope) | K-gen is internal; k-reuse class is structurally excluded |
| Kotlin `ByteArray` zeroization (DRBG K/V/h1/x, privkey copies, seed copies, chain codes) | **[C]** | ART compacting GC may retain copies |
| BigInteger intermediates (d, s, k, R) — immutable, GC-released only | **[C]** | Documented in the zeroization review; never [P] |
| BC `HMac` pad state (XOR-derived copy of the chain code / DRBG key) | **[C]** | BC exposes no wipe API (T1.3-H9 caveat class) |
| `destroy` eviction + `fill(0)` | **[C]** | Same GC copy-caveat class |
| JS-side platform guard + digest fast-fail | **[S]** | Contract, not protection — the real R7 boundary is the key-less web stub + R7 CI grep |

## Handoff (owned by T1.4, adjudicated by the next planning round)

| # | Item | Reason | Suggested disposal | AC/TC |
| --- | --- | --- | --- | --- |
| T1.4-H1 | JS→native bridge invocation (= T1.3-H8 = T1.2-H3): signDigest/destroy not exercised through the JS runtime | Needs an RN host (expo-modules-core 57.0.22 `AppContext.kt:50-54`) | Replan → **T4.1** dev-build screen smoke (onboarding import → `deriveAddress` through the bridge); T4.1's planning round re-adjudicates per B.7-2 | AC-9 |
| T1.4-H2 | jest/dev-build carriers of spec AC-3 (TC-VC-004) | Repo has no JS test runner (T1.1 closure debt, H5) | Replan → **T1.1 closure** / **T4.1** | AC-3 |
| T1.4-H3 | TC-VC-005 iPhone side (memory hygiene on iOS) | iOS implementation is T1.5 | Replan → **T1.5** | AC-10 |
| T1.4-H4 | Device gate fallback | Only if no Pixel 6a is available at implementation time (Q3) | Rerun `:vault-core:connectedDebugAndroidTest` when a device is attached; JVM gate stays the mandatory automated evidence | AC-9 |
| T1.4-H5 | Cross-check tooling versions | Throwaway /tmp only | Record the exact noble/viem versions used in `validation.md` Task 7 | AC-4 |
