# T1.3 — vault-core Android BIP32/BIP44 deriveAddress (requirements)

> Feature spec (requirements). Planning round **2026-10-15**. Branch:
> `feature/T1.3-vault-core-android-bip32` (created from `main` `2dbe7a5` before this file was
> written).
> Parent roadmap item: `plans/3-day-implementation-plan.md` Day 1 T1.3（关键路径）.
> Skill: `nexvault-t1-3-vault-core-android-bip32`（also copied in this feature directory as
> `SKILL.md`; both copies sha256 `1c9aec107f717a765e17a3c1da06fa3d62da0e20500c45b78842a2ca30e07ad7`）.
> **代码评审轮 2026-10-16：** SKILL.md 内的 `HDKeyDerivation`/`HDKey`、「向量 1–5」与 BC `Base58` 假设已在实现期被证伪并修正——上述三点以本文件与 `plan.md` 为准（证据：`validation.md` §Code review round）。
> Sibling files: `plan.md`, `validation.md`.
> Session status: planning round **2026-10-15** wrote this record. Implementation starts only
> after `GATE-MODEL-SWITCH` (owner switches the model to V4.1-Flash) and `GATE-R4-BC`
> (owner approves the BouncyCastle R4 package).

## Goal

Implement **only** `deriveAddress(handle: string, path: string) → Promise<string>` in
`modules/vault-core` (Android/Kotlin), returning the **EIP-55 checksummed EVM address**:

- BIP-32 (secp256k1) hierarchical derivation from the 64-byte seed that T1.2's
  `importMnemonic` stored in the native `handleStore` — the seed never leaves native (R1);
- BIP-44 EVM path whitelist `m/44'/60'/<account>'/0/<index>` with `account, index ∈ [0, 2^31)`;
- address bytes = `keccak256(x‖y)[12..32)` of the uncompressed secp256k1 public key
  (`0x04‖x‖y`, 64 bytes after the prefix), keccak = **pre-NIST Keccak-256**, never NIST SHA3-256;
- EIP-55 mixed-case checksum output;
- automated gates: official **BIP-32 test vectors 1–4** (JVM; V5 = 16 malformed extended keys is deliberately outside the gate — Handoff T1.3-H7), the original-repo canonical
  anchor **`0x9858EfFD232B4033E47d90003D41EC34EcaEda94`** (TC-VC-003, JVM + real device), an
  independently cross-checked second address, keccak-256 sanity and EIP-55 vectors;
- H3 closure: device instrumentation on the physical Pixel 6a asserts the module surface and
  reproduces the canonical address with the packaged wordlist.
  **代码评审轮 2026-10-16（MAJOR-1）：** 实装的 device test 直接调用 `Bip39`/`Bip32`，**未**调用
  `importMnemonic`/`deriveAddress` AsyncFunction，故上方「bridge path」口径**未达成**；
  AsyncFunction/JS 桥接调用改记为 T1.3-H8（= T1.2-H3 事项），待 owner 裁定 implement 或 replan。

`signDigest` / `destroy` stay T1.4; iOS stays T1.5; web keeps the fail-closed stub (R7).

## Owner decisions (2026-10-15, C.1 ask-user round)

Owner dispositions carried into this round (given 2026-10-15):

| # | Item | Disposition |
| --- | --- | --- |
| D1 | T1.2 Handoff H1 (`destroy` absent) | **T1.4** — `destroy` remains out of T1.3 scope |
| D2 | T1.2 Handoff H2 (`specs/vault-core.md` conflicts) | **Amend the spec this round** (Task 1 scope, Q1) |
| D3 | T1.2 Handoff H3 (JS-bridge invocation deferred) | **Implement** — the T1.3 device test is the canonical-address bridge path. **代码评审轮 2026-10-16：未达成**（device test 未调用模块函数）；改记 T1.3-H8，待 owner 裁定 |
| D4 | T1.2 Handoff H6/H7 (T1.1 scaffold debt; `expo-doctor` patch drift) | **T1.1 closure** — out of T1.3 scope; T1.3 must not upgrade expo |
| D5 | R4 dependency for secp256k1 | **Propose `org.bouncycastle:bcprov-jdk18on:1.86`**; a separate owner approval (GATE-R4-BC) precedes any `build.gradle` change |
| D6 | Model switch | **GATE-MODEL-SWITCH** — stop after the spec files are committed; implementation runs only after the owner switches the model to V4.1-Flash |

Ask-user round answers (all confirmed by the owner, 2026-10-15):

| Q | Question | Decision |
| --- | --- | --- |
| Q1 | H2 amendment scope: `specs/vault-core.md` §2.1 `VaultHandle` → branded string UUID; §2.2 Kotlin + §2.3 iOS signatures `handle: Int` → `String` (iOS doc-only, implementation stays T1.5); §2.4 delete `E_VAULT_CHECKSUM_MISMATCH`, fold into `E_VAULT_INVALID_MNEMONIC`; TC-VC-002 expects only `E_VAULT_INVALID_MNEMONIC` | **Confirmed — amend as stated** |
| Q2 | Canonical anchor: `abandon×23 about` @ `m/44'/60'/0'/0/0` → `0x9858EfFD232B4033E47d90003D41EC34EcaEda94` (EIP-55 mixed-case; source: original repo `HDKeyManagerTest.kt` TC-SEC-005) | **Confirmed** |
| Q3 | R4 proposal wording: `org.bouncycastle:bcprov-jdk18on:1.86`, `implementation`, `modules/vault-core/android`; lightweight API (`org.bouncycastle.crypto.*`) only; never register a JCE provider; approval recorded in `validation.md` before touching `specs/tech-stack.md` + `doc/dependencies.md` + `build.gradle` | **Confirmed** |
| Q4 | Second-address cross-check: `m/44'/60'/0'/0/1` value pinned via `@scure/bip32` in a throwaway `/tmp` script (devDependency-only comparison; zero repo dependency change) | **Confirmed** |
| Q5 | H3 device plan: Pixel 6a (Android 17) wireless adb, `ANDROID_SERIAL` pinned to one transport (T1.2 duplicate-transport lesson); if no device at implementation time, H3 reopens as a Handoff and the JVM gate remains the mandatory automated gate | **Confirmed** |
| Q6 | `GATE-MODEL-SWITCH`: spec files + commit first, then stop for the V4.1-Flash switch before any code | **Confirmed** |

## Reference precedence for unclarified questions (owner rule, carried from T1.2 Q8)

1. **RN/TS/JS/Expo topics** (Expo Modules API, autolinking, Kotlin↔JS typing, SDK 57 behaviour):
   this repo's `AGENTS.md`, `apps/mobile/AGENTS.md`, the T1.3 skill, and the Expo SDK 57
   versioned docs are authoritative.
2. **All other topics** (Kotlin, Gradle, JUnit/AndroidX testing, BIP-32/44 details, BouncyCastle
   usage, test layout): resolve by referring to the original NexVault repo at
   **`/home/superguo/Projects/NexVault`** — its `AGENTS.md`, `specs/tech-stack.md`,
   `gradle/libs.versions.toml`, and existing source/tests. When used, the implementation record
   must name the exact original-repo file consulted in `validation.md`.
3. **Conflict resolution**: this repo's `AGENTS.md` R1–R8/R4, the T1.3 skill, and this feature
   spec win. (Example: the original repo's web3j `Bip32ECKeyPair` approach is **reference-only**;
   this repo packages BouncyCastle per `specs/vault-core.md` §2.2/§6 — web3j would be a second
   R4 dependency for no gain.)
4. **Still unresolved?** Stop and ask the owner before writing code — never invent an answer.

## Handoff re-read (AGENTS.md B.7 — every pre-existing candidate gets an explicit verdict)

| Source | Deferred entry | Verdict for T1.3 |
| --- | --- | --- |
| T1.2 `validation.md` H1 | No `destroy`; handle store lives per process | **Replan → T1.4** (owner D1). T1.3 reads the store but does not add destruction |
| T1.2 `validation.md` H2 | `specs/vault-core.md` conflicts (Int vs UUID handle; checksum error split) | **Implement → Task 1** (owner D2; Q1 scope) |
| T1.2 `validation.md` H3 | JS-bridge `importMnemonic` invocation deferred to T1.3 | **Implement → Task 6** (owner D3; Q2/Q5) — **代码评审轮 2026-10-16：未达成**，实装 device test 直调密码核心；改记 T1.3-H8（补 AsyncFunction/JS smoke，或 owner 批准 replan） |
| T1.2 `validation.md` H6 | T1.1 scaffold debt (no eslint/jest; `packages/vault-core` absent) | **Replan → T1.1 closure** (owner D4). T1.3 reuses the T1.2 throwaway-harness pattern for JS-wrapper evidence |
| T1.2 `validation.md` H7 | `expo-doctor` expo `57.0.28` vs expected `~57.0.29` | **Replan → T1.1 closure** (owner D4). T1.3 must not upgrade expo |
| AGENTS.md B.7-3 | iOS mlock semantics / native word-grid keyboard / vault-core C++-ification / Secure Enclave wrap | Unchanged — all remain roadmap stage-2 items; not touched by T1.3 |
| `specs/vault-core.md` §7 | BIP-39 passphrase (25th word), multi-account management | Not in T1.3 (EVM path whitelist is single-purpose/account 0..n via path argument only) |

## Functional requirements

- **FR-1 — TS interface and R1 boundary.** `modules/vault-core/src/VaultCoreModule.ts` declares
  `deriveAddress(handle: string, path: string): Promise<string>` next to the T1.2
  `importMnemonic` declaration; `modules/vault-core/index.ts` exports
  `VaultHandle = string & { readonly __brand: 'VaultHandle' }` and a `deriveAddress` wrapper
  with the T1.2 fail-closed platform guard (`typeof VaultCoreModule.deriveAddress !== 'function'`
  → throw with `error.code = 'E_VAULT_UNSUPPORTED_PLATFORM'`). Both wrapper branches (module
  present / module absent) are executed via the throwaway `/tmp` Node harness (T1.2 review-fix
  NIT-4 pattern; the repo still has no JS test runner — H6 → T1.1). No key material crosses the
  boundary in either direction; the address string is public information (R1).
- **FR-2 — Path whitelist.** `parseEthereumPath(path: String): IntArray` accepts only
  `m/44'/60'/<account>'/0/<index>` (single-quote or `h` hardened spellings, nothing else;
  non-negative `account, index < 0x80000000`) and returns
  `[44|H, 60|H, account|H, 0, index]`. Any violation throws `E_VAULT_INVALID_PATH` with a fixed
  template that may echo the path **syntax class** but never key material. **已接受语法类（2026-10-16 评审
  NIT-2 记录）：** 首尾空白被 trim；`<account>`/`<index>` 为 `\d+`（接受前导零，故
  `m/44'/60'/007'/0/0` 与 `…/7'/…` 同索引）；`'` 与 `h` 硬化标记可混用。三者均为良性别名
  （派生索引相同），不是白名单逃逸；AC-1 已同步。
- **FR-3 — BIP32 core (BouncyCastle lightweight API + self-written CKD glue).** New
  `modules/vault-core/android/src/main/java/expo/modules/vaultcore/Bip32.kt` with `internal`
  functions: `parseEthereumPath`, `masterNode(seed)`, `deriveChild(node, index)`,
  `deriveAddressBytes(seed: ByteArray, indices: IntArray): ByteArray` (20 bytes:
  CKDpriv chain → uncompressed pubkey via
  `CustomNamedCurves.getByName("secp256k1").g.multiply(key)` → `getEncoded(false)` 65B, drop the
  `0x04` prefix → `KeccakDigest(256)` over `x‖y` → last 20 bytes),
  `toChecksumAddress(addressBytes: ByteArray): String`, `keccak256(bytes: ByteArray): ByteArray`.
  **Implementation-time correction (Task 4, recorded):** BouncyCastle 1.86 ships **no** HD-key API —
  `HDKeyDerivation`/`HDKey` do not exist in the artifact (nor a `Base58` helper), so the BIP-32 CKD
  *glue* (HMAC-SHA512 via `HMac(SHA512Digest)`, ser32/ser256 concatenation, `IL + k_parent mod n`,
  HASH160 fingerprinting) is written in `Bip32.kt` while **all curve arithmetic remains in BC**,
  preserving `specs/vault-core.md` §2.2's "point math must not be self-implemented" rule. Only
  `org.bouncycastle.crypto.*` classes are used; `BouncyCastleProvider` is **never** registered as a
  JCE provider (Android platform BC remnants clash).
- **FR-4 — Keccak-256 and EIP-55.** Keccak is the **pre-NIST** variant (`KeccakDigest(256)`),
  gated by `keccak256("") == c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470`.
  `toChecksumAddress` implements ERC-55 over the lowercase hex string and is gated by the four
  official ERC-55 vectors: `0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed`,
  `0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359`,
  `0xdbF03B407c01E7cD3CBea99509d93f8DDDC8C6FB`,
  `0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb`.
- **FR-5 — Module wiring and typed errors.** `VaultCoreModule.kt` registers
  `AsyncFunction("deriveAddress") Coroutine { handle: String, path: String -> ... }` on
  `Dispatchers.Default`; looks up the seed in the T1.2 `handleStore`
  (`E_VAULT_HANDLE_NOT_FOUND` when absent — the message must not distinguish "empty store" from
  "bad handle" beyond the code); copies the seed, wipes the copy with `fill(0)` in `finally`;
  returns `Bip32.toChecksumAddress(Bip32.deriveAddressBytes(...))`. `VaultExceptions.kt` adds
  `E_VAULT_INVALID_PATH`, `E_VAULT_HANDLE_NOT_FOUND`, `E_VAULT_CRYPTO_INTERNAL` (fixed
  templates, no fragments). No `Log.*`/print in `src/main`.
- **FR-6 — Zeroization and security labelling.** Every scratch `ByteArray` this feature creates
  (seed copy, pubkey `x‖y` buffer, keccak buffers) is `fill(0)`-ed in `finally`. BouncyCastle
  `BigInteger` private-key intermediates are immutable and not reliably zeroizable — recorded as
  a **[C]** caveat (same class as the T1.2 String/BigInteger caveat), never [P]. The seed-native
  boundary and the no-key-material-logging rule are **[P]** within T1.3 scope.
- **FR-7 — JVM vector gate (mandatory automated gate).** New
  `modules/vault-core/android/src/test/java/expo/modules/vaultcore/Bip32Test.kt`:
  1. **BIP-32 official vectors 1–4**（V5 在门禁之外，见下）— seeds and chain lists as pinned in the
     "Vectors" section
     below; the test implements a test-side 78-byte serializer (version|depth|parentFingerprint|
     childNumber|chainCode|key + **测试侧自写** Base58Check（BC 1.86 无 `Base58` 类，仅复用其
     `SHA256Digest`——代码评审轮 2026-10-16，MINOR-1）; `xprv=0x0488ADE4`,
     `xpub=0x0488B21E`; parent fingerprint = first 4 bytes of `RIPEMD160(SHA256(pub))`) and
     asserts master + every listed chain for vectors 1–4. **Vector 5 is deliberately outside the
     T1.3 gate**: the fetched revision lists 16 invalid/malformed *extended keys* (version mismatch,
     bad key prefixes, zero depth with non-zero parent fingerprint/index, unknown version, private
     key ∉ [1, n-1], invalid public key, bad checksum); recognising them needs an extended-key
     parser, which T1.3 production code does not have, so asserting them would exercise only test
     scaffolding — recorded as Handoff T1.3-H7 instead of a fake gate. The exact xprv/xpub strings
     are transcribed into the test **only after** the Step-0 verification (download
     `bip-0032.mediawiki`, record sha256, independently recompute **all** chains with a separate
     Python implementation — the T1.2 P0.4 pattern, strengthened); the transcription itself is
     recorded in `validation.md`.
  2. **Canonical address (TC-VC-003)** — `Bip39.mnemonicToSeed("abandon ×23 about", "")` →
     `deriveAddressBytes` @ `m/44'/60'/0'/0/0` → `0x9858EfFD232B4033E47d90003D41EC34EcaEda94`
     (original-repo TC-SEC-005 anchor).
  3. **Second-address cross-check** — `m/44'/60'/0'/0/1` value pinned by the Task-0 independent
     cross-check recorded in `validation.md`（记录的工具为 stdlib-Python 实现，符合 plan Task 0 的
     回退规则；Q4 的 `@scure/bip32` 未被记录为实际运行——代码评审轮 2026-10-16，MINOR-4；
     zero repo dependency change），then embedded in the test.
  4. **Keccak sanity** (FR-4 vector) and **EIP-55** (four vectors above).
  5. **Negatives (TC-VC-016)** — `m/`, `m/49'/60'/0'/0/0`, non-numeric segments, hardened
     change/index (`m/44'/60'/0'/1'/0`), `account ≥ 2^31` → `E_VAULT_INVALID_PATH`; unknown
     handle → `E_VAULT_HANDLE_NOT_FOUND`; every error message contains no input fragments.
- **FR-8 — Instrumentation (H3 closure, TC-VC-017).** Update
  `modules/vault-core/android/src/androidTest/java/expo/modules/vaultcore/VaultCoreModuleTest.kt`:
  assert the module definition exposes `deriveAddress` (**no map-size pin** — T1.2 lesson);
  end-to-end device test: `importMnemonic` with the canonical 12-word bytes through the module
  path → non-empty handle → `deriveAddress(handle, "m/44'/60'/0'/0/0")` == the canonical anchor;
  invalid path → typed error with machine-readable code. Run pinned to one transport:
  `ANDROID_SERIAL=<serial> ./gradlew :vault-core:connectedDebugAndroidTest` (T1.2
  duplicate-transport lesson).
- **FR-9 — R4 dependency amendment (GATE-R4-BC, owner approval required).**
  `org.bouncycastle:bcprov-jdk18on:1.86`, `implementation`, `modules/vault-core/android`.
  Purpose: BIP-32/44 HD derivation (secp256k1), pre-NIST Keccak-256, Base58Check test helpers —
  the "packaged new BouncyCastle" mandated by `specs/vault-core.md` §2.2/§6. Evidence to
  re-verify at implementation before asking: Maven Central artifact metadata (skill states
  lastUpdated 2026-09-11 → min-release-age ≥ 7 days satisfied), no known unpatched CVE in the
  1.8x line, trust basis = BouncyCastle ecosystem incl. bitcoinj. Only after the owner approval is
  recorded in `validation.md` may `specs/tech-stack.md` (new row) + `doc/dependencies.md`
  (new section) be amended and the coordinate added to `modules/vault-core/android/build.gradle`.
  web3j / bitcoinj / @noble / @scure must **not** enter production — BC only.
- **FR-10 — H2 spec amendment.** Execute and record the `specs/vault-core.md` diff per Q1:
  §2.1 `VaultHandle` → branded string UUID; §2.2 Kotlin and §2.3 iOS `AsyncFunction` handles →
  `String`; §2.4 delete `E_VAULT_CHECKSUM_MISMATCH` and fold into `E_VAULT_INVALID_MNEMONIC`;
  TC-VC-002 wording updated to expect only `E_VAULT_INVALID_MNEMONIC`. The diff is evidence in
  `validation.md` Task 1 (this is the constitution amendment the T1.2 H2 disposal ordered).
- **FR-11 — Verification evidence.** Every step records command + exit code + observed output in
  `validation.md` (B.4/B.6). Mandatory final checks: JVM vector gate green
  (`BUILD SUCCESSFUL`), zero-new-deps diff (`package.json`, `apps/mobile/package.json`,
  `pnpm-lock.yaml`), R1 CI-style grep, R5 log grep, `tsc --noEmit`, zeroization code-review
  note with file:line evidence.
- **FR-12 — Implementation-session stop contract.** `plan.md` Task 0 begins with
  `GATE-MODEL-SWITCH`: the implementation session must stop and let the owner switch the model
  to V4.1-Flash before any code is written (same as T1.2 FR-12).

## Vectors and anchors (automated gates)

### BIP-32 official test vectors (transcribed into `Bip32Test.kt` at implementation Task 6)

Source: `bip-0032.mediawiki` (fetched at implementation Step 0; sha256 recorded; vector 1's
master xprv independently recomputed with stdlib Python before transcription).

| Vector | Seed (hex) | Chains asserted (xprv + xpub per chain, master included) |
| --- | --- | --- |
| V1 | `000102030405060708090a0b0c0d0e0f` | `m`, `m/0'`, `m/0'/1`, `m/0'/1/2'`, `m/0'/1/2'/2`, `m/0'/1/2'/2/1000000000` |
| V2 | `fffcf9f6f3f0edeae7e4e1dedbd8d5d2cfccc9c6c3c0bdbab7b4b1aeaba8a5a29f9c999693908d8a8784817e7b7875726f6c696663605d5a5754514e4b484542` | `m`, `m/0`, `m/0/2147483647'`, `m/0/2147483647'/1`, `m/0/2147483647'/1/2147483646'`, `m/0/2147483647'/1/2147483646'/2` |
| V3 | `4b381541583be4423346c643850da4b320e46a87ae3d2a4e6da11eba819cd4acba45d239319ac14f863b8d5ab5a0d0c64d2e8a1e7d1457df2e5a3c51c73235be` | `m`, `m/0'` (2 chains in the fetched revision) |
| V4 | `3ddd5602285899a946114506157c7997e5444528f3003f6134712147db19b678` | `m`, `m/0'`, `m/0'/1'` (leading-zero-retention vector; 3 chains in the fetched revision) |
| V5 | — (no seed; 16 malformed extended keys) | **outside the T1.3 gate** — extended-key validity needs a parser T1.3 does not have; see Handoff T1.3-H7 |

Chain lists corrected in implementation Task 0 to match the **fetched** `bip-0032.mediawiki`
(`sha256 e5e00a8289db2f681052cf24a745320afc225e66b25d1e489a7c884d2fc7f11f`, lines 218–309) after an
independent Python implementation reproduced **every** listed chain (xprv and xpub) for V1–V4; the
planning draft had over-listed chains for V3/V4 from memory and mis-described V5. The correction is
recorded in `validation.md` Task 0.

### BIP-44 canonical anchor (TC-VC-003, JVM + device)

| Mnemonic (12 words) | Path | Expected address (EIP-55) |
| --- | --- | --- |
| `abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about` | `m/44'/60'/0'/0/0` | `0x9858EfFD232B4033E47d90003D41EC34EcaEda94` |

Provenance: original repo `/home/superguo/Projects/NexVault/core/core-security/src/test/java/com/nexvault/wallet/core/security/wallet/HDKeyManagerTest.kt` lines 16–24 (`// TC-SEC-005: the canonical BIP-39 vector derives the known BIP-44 address.` → `assertEquals("0x9858EfFD232B4033E47d90003D41EC34EcaEda94", address)`). The BIP-39 seed for
`abandon×23 about` with empty passphrase is
`5eb00bbddcf069084889a8ab9155568165f5c453ccb85e70811aaed6f6da5fc19a5ac40b389cd370d086206dec8aa6c43daea6690f20ad3d8d48b2d2ce9e38e4`
(locked by T1.2's `emptyPassphraseDerivesDistinctVectorSeed`).

### Keccak-256 sanity + EIP-55 vectors

- `keccak256("") = c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470`
- ERC-55 (lowercase input → mixed-case output): `0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed`,
  `0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359`, `0xdbF03B407c01E7cD3CBea99509d93f8DDDC8C6FB`,
  `0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb`.

## Out of scope

- `signDigest` / `destroy` (T1.4); RFC6979, `E_VAULT_INVALID_DIGEST_LENGTH` and memory-hygiene
  stress stay there.
- iOS implementation (T1.5; the H2 amendment keeps the iOS signatures consistent, doc-only).
- Web implementation (stub keeps failing closed with `E_VAULT_UNSUPPORTED_PLATFORM`; R7).
- BIP-39 passphrase (25th word), multi-coin paths (`60'` coin-type only), at-rest key wrap,
  StrongBox/Enclave, mlock, the security-gate integration (`E_VAULT_SECURITY_GATE` wiring is a
  security-android item).
- Any production dependency beyond the owner-approved R4 package (FR-9).

## Acceptance criteria (each AC binds a test/gate and evidence location)

| AC | Content | Test/gate |
| --- | --- | --- |
| AC-1 | Path whitelist accepts exactly `m/44'/60'/<account>'/0/<index>` (`h` spelling allowed; plus the documented benign aliases of FR-2 — 首尾空白 trim、前导零、`'`/`h` 混用); all other shapes rejected | TC-VC-016 (JVM negatives) |
| AC-2 | Canonical anchor reproduced: `abandon×23 about` @ `m/44'/60'/0'/0/0` → `0x9858EfFD232B4033E47d90003D41EC34EcaEda94`; second address matches the Task-0 independent cross-check value（记录工具：stdlib Python——代码评审轮 2026-10-16，MINOR-4） | TC-VC-003 (JVM) + TC-VC-017 (device) |
| AC-3 | BIP-32 official vectors 1–4: master + every listed chain xprv/xpub match（共 17 链）。Vector 5（16 条畸形扩展密钥）**不在门禁内**——无生产解析器；Handoff T1.3-H7（代码评审轮 2026-10-16 修订，MINOR-1） | TC-VC-013 (JVM, `BUILD SUCCESSFUL`) |
| AC-4 | `keccak256("")` sanity vector and the four ERC-55 vectors pass | TC-VC-015 (JVM) |
| AC-5 | R1/R5 greps clean; zeroization review lists every scratch buffer with file:line; `[C]` caveat for BC `BigInteger` intermediates recorded | TC-VC-018 review + CI-style greps |
| AC-6 | Typed errors `E_VAULT_INVALID_PATH` / `E_VAULT_HANDLE_NOT_FOUND` / `E_VAULT_CRYPTO_INTERNAL`; messages contain no input fragments | TC-VC-016 (JVM) + TC-VC-017 (device) |
| AC-7 | `specs/vault-core.md` H2 amendment applied and its diff recorded | Task 1 evidence |
| AC-8 | R4: BouncyCastle package approved by the owner and the approval recorded before any `build.gradle` change; `specs/tech-stack.md` + `doc/dependencies.md` amended | GATE-R4-BC evidence (Task 3) |
| AC-9 | Device instrumentation green on the physical Pixel 6a (Android 17): `BUILD SUCCESSFUL`, `tests="4" failures="0"`, canonical anchor reproduced on-device with the packaged wordlist. **代码评审轮 2026-10-16：** 「via the bridge」未达成——AsyncFunction/JS 调用属 T1.3-H8，不在 AC-9 内 | TC-VC-017 (`connectedDebugAndroidTest` with `ANDROID_SERIAL` pinned) |
| AC-10 | Zero new npm deps (`package.json` / `apps/mobile/package.json` / `pnpm-lock.yaml` diff); `tsc --noEmit` exit 0 | Task 7 static checks |
| AC-11 | `validation.md` contains command + exit code + observed output for every task; no `[x]` without evidence | B.4/B.6 audit |
| AC-12 | `GATE-MODEL-SWITCH` recorded before any code write (this planning session) | FR-12 / plan Task 0 |

## Security-level labelling (R6 — no level inflation)

| Mechanism | Label | Reason |
| --- | --- | --- |
| Seed never crosses the JS↔native boundary; only the public address string is returned | **[P]** (within T1.3 scope) | Enforced by API shape + T1.2 store + CI greps |
| No key-material logging / fixed error templates | **[P]** (within T1.3 scope) | No `Log.*`; fixed templates; grep gate |
| Kotlin `ByteArray` zeroization of scratch buffers | **[C]** | ART compacting GC may retain copies |
| BouncyCastle `BigInteger` private-key intermediates (immutable, released only by GC) | **[C]** | Documented in the zeroization review; never [P] |
| JS-side platform guard `E_VAULT_UNSUPPORTED_PLATFORM` | **[S]** | Contract, not protection — the real R7 boundary is the key-less web stub + R7 CI grep |

## Handoff (owned by T1.3, adjudicated by the next planning round)

| # | Item | Reason | Suggested disposal | AC/TC |
| --- | --- | --- | --- | --- |
| T1.3-H1 | `destroy` still absent; handle store entries live per process | T1.4 scope (owner D1) | Implement in T1.4 | — |
| T1.3-H2 | iOS `deriveAddress` port pending | T1.5 | Replan to T1.5 | AC-2 |
| T1.3-H3 | Device instrumentation fallback | Only if no Pixel 6a is available at implementation time (owner Q5) | Rerun `:vault-core:connectedDebugAndroidTest` when a device is attached; JVM gate stays the mandatory automated evidence | AC-9 |
| T1.3-H4 | BIP-32 xprv/xpub strings transcribed at implementation | Transcription happens only after Step-0 sha256 + independent verification (FR-7) | Keep the transcription evidence in `validation.md` | AC-3 |
| T1.3-H5 | JS wrapper evidence still uses the throwaway `/tmp` harness | Repo has no JS test runner (T1.2 H6) | T1.1 closure should absorb the harness as committed jest tests | AC-10 |
| T1.3-H6 | BC version pin (1.86) must carry to T1.4 signing work | T1.4 uses the same provider | Replan to T1.4 | AC-8 |
| T1.3-H7 | BIP-32 vector 5 (16 invalid extended keys) not gated | No production extended-key parser in T1.3; the vector tests serialization validity | Absorb when an xpub-export/parser feature exists (roadmap stage 2) | AC-3 |
| T1.3-H8 | Module/JS-bridge invocation not exercised by the device test (= the T1.2-H3 item; owner D3 "Implement" not achieved) | 代码评审轮 2026-10-16：instrumentation test 直调 `Bip39`/`Bip32`，未调用 AsyncFunction/JS；且 `AppContext` 需真实 `ReactApplicationContext`（expo-modules-core 57.0.22 `AppContext.kt:50-54`），库模块测试 APK 内无法桥接 | **Owner 裁定 2026-10-16：replan → T4.1**（dev-build 页面 smoke：onboarding 导入 → `deriveAddress` 走 JS 桥接）；由 T4.1 规划轮再裁定 | AC-9 / TC-VC-017 |
