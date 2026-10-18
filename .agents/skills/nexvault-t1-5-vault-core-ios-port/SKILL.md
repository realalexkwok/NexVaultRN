# NexVault T1.5 — vault-core iOS four-method port (merged, platform-swapped skill)

## Synthesis method (provenance of this skill)

Merge-and-swap, **not** written from scratch: the three committed Android skills
(T1.2 `0229587644bf78ffc57cc28c04a84e2ea1c2b74d2965c8c81c8fb0ca1e3c728d`,
T1.3 `1c9aec107f717a765e17a3c1da06fa3d62da0e20500c45b78842a2ca30e07ad7`,
T1.4 `b0dbe67a1427adbf1a6a0add79dbbd049d3b045b484729e964666cbb7ea1f9b2`) were merged
section by section over the T1.2 skeleton (Provenance / Scope / Invariants / Preflight / Steps /
Verification) and the platform layer was swapped per the table below. Platform-agnostic content
is inherited verbatim; the behavioral oracle is the **verified Android implementation** plus the
T1.2/T1.3/T1.4 validation evidence trails.

### Swap table (Android → iOS)

| Android (T1.2–T1.4 skills) | iOS (this skill) |
|---|---|
| Kotlin (`expo.modules.vaultcore`) | Swift (`expo.modules.vaultcore`, same module name) |
| Gradle project + `:vault-core:testDebugUnitTest` / `connectedDebugAndroidTest` | Xcode project/podspec + `xcodebuild test` (simulator auxiliary, real iPhone authoritative) |
| JUnit4 + AndroidJUnit4 | XCTest |
| `javax.crypto` PBKDF2-HMAC-SHA512 (+ self-written fallback) | **CommonCrypto `CCKeyDerivationPBKDF`** (kCCPBKDF2, kCCPRFHmacAlgSHA512, 2048 rounds — gate-3 decision 2026-10-16, owner-approved; no fallback needed, system framework) |
| BouncyCastle `bcprov-jdk18on:1.86` (point ops, hashes) | **swift-secp256k1** (21-DOT-DEV, libsecp256k1 bindings — pin proposal 0.23.2, see GATE-R4-SWIFTSECP) + **CommonCrypto** (SHA-256/HMAC) |
| BC `KeccakDigest(256)` | **self-written Keccak-f[1600] (`Keccak.swift`)** — CommonCrypto has no Keccak; see GATE-KECCAK |
| `ByteArray.fill(0)` zeroization | `[UInt8]` + `memset_s` / `Data.resetBytes(in:)`; Data copy-on-write caveat → **[C]** (Invariant 7) |
| `res/raw/bip39_english.txt` (sha256 `2f5eed53…`) | same file as a module bundle resource, **identical sha256 `2f5eed53a4727b4bf8880d8f3f199efc90e58503646d9ff8eff3a2ed3b24dbda`** asserted in tests |
| Pixel 6a / Android 17 instrumentation | iPhone real-device XCTest (simulator only auxiliary for security ACs, B.4-5); pin one UDID per run (T1.2 `ANDROID_SERIAL` lesson) |
| `UUID.randomUUID().toString()` handle | `UUID().uuidString` handle (same opaque-string semantics, H2-closed) |
| Android mlock (`mmap`+mlock, T-VC-3) | **iOS mlock = no documented guarantee → [C]** + scheduled one-line real-device probe (C6); no functional dependency on it |

### Sources allowed

| Source | Contribution |
|---|---|
| Expo official [`expo-module` skill](https://github.com/expo/skills/blob/main/plugins/expo/skills/expo-module/SKILL.md) (Swift DSL sections) | `ModuleDefinition`, `AsyncFunction` async/throws, JS `ArrayBuffer` ↔ Swift `Data` type map, autolinking |
| [BIP-0039](https://github.com/bitcoin/bips/blob/master/bip-0039.mediawiki) + [Trezor vectors.json](https://github.com/trezor/python-mnemonic/blob/master/vectors.json) | same V1–V8 oracle vectors as T1.2 |
| T1.3/T1.4 validation trails (BIP-32/44, ERC-55, RFC6979, TC-VC-027/028) | oracle values already cross-verified on Android — re-transcribe from the Kotlin tests, do not invent |
| [21-DOT-DEV swift-secp256k1](https://github.com/21-DOT-DEV/swift-secp256k1) (ex-Blockstream) | secp256k1 point/sign ops; MIT; active (pushed 2026-10-16); latest tag 0.23.2 (2026-05-23, ≥7-day age) |
| Apple CommonCrypto / XCTest documentation | CCKeyDerivationPBKDF / CCHmac / CC_SHA256 signatures; XCTest + `xcodebuild test` workflow |
| NexVault `AGENTS.md` R1/R4/R5/R6/B.6/B.7 | red lines, evidence discipline, [P]/[C]/[S] labeling |

**Rejected:** same list as T1.2 (wallet-core skill/library, `@ronickg/react-native-bip39`, `react-native-quick-bip39`) **plus** CryptoSwift (gate-3: superseded by CommonCrypto), CryptoKit (has **no PBKDF2** — HKDF only, cannot serve the KDF path), RFC 6979 A.2.5 vectors as secp256k1 gates (P-256 trap, T1.4 lesson), and copying web3j from the original repo (reference-only).

## Scope

- Port **all four methods** to iOS per `specs/vault-core.md` §2.3 (post-H2, post-T1.4-recid
  amendment): `importMnemonic` / `deriveAddress` / `signDigest` / `destroy`.
- **Behavioral oracle** = the verified Android implementation: T1.2 JVM 16/16 + device 2/2,
  T1.3 26/26 + 4/4, T1.4 37/37 + 6/6 + `ALL VECTORS PASS` (incl. the BLOCKER-1 recid-flip
  semantics: low-s 归一化取负 s 时 recid 必须 `xor 1` — TC-VC-027/028 are cross-platform gates).
- **Not** in scope: Web, C++ core (libsecp256k1 CMake), Secure Enclave/Keychain at-rest (that is
  security-ios), passphrase, multi-chain, native word-grid keyboard.
- TS layer is **frozen** (T1.2/T1.4 wrappers): T1.5 changes zero TypeScript; `tsc --noEmit`
  stays green as a gate.
- Hard-gate status (all satisfied 2026-10-16): T1.4 owner re-closed (`f11947c`); H2 closed;
  KDF = CommonCrypto unified (plans v1.2 `a05c862` + `vault-core.md` revision note);
  swift-secp256k1 pin proposal researched (below) → R4 approval at the T1.5 planning round.

## Invariants (MUST)

1. **R1 boundary**: JS↔native passes only `ArrayBuffer` + opaque handle; Swift receives the
   mnemonic as `Data` and must zeroize its copy immediately after decoding (the Expo bridge
   copy is wiped by the T1.2 JS wrapper `finally fill(0)` — the native-side copy is this
   feature's responsibility).
2. **Handle semantics = Android oracle**: `UUID().uuidString`; native-only seed store;
   post-destroy calls throw `E_VAULT_HANDLE_NOT_FOUND`; `destroy` is idempotent (no throw on
   second call).
3. **R5 log red line**: no `print`/`NSLog`/`os_log` with key material; error messages use
   the §2.4 whitelist codes with fixed templates (same codes as Android — cross-platform parity:
   `E_VAULT_INVALID_MNEMONIC`, `E_VAULT_PBKDF2_UNAVAILABLE`, `E_VAULT_INVALID_PATH`,
   `E_VAULT_INVALID_DIGEST_LENGTH`, `E_VAULT_HANDLE_NOT_FOUND`,
   `E_VAULT_CRYPTO_INTERNAL`, `E_VAULT_UNSUPPORTED_PLATFORM`).
4. **RFC6979 determinism + recid oracle**: byte-identical outputs for identical
   (handle, path, digest); low-s normalization; **recid flips on normalization** (T1.4
   BLOCKER-1 — the TC-VC-027 six-vector set covers both branches and is mandatory).
5. **Keccak-256, not SHA3-256** (EIP-55 + address derivation): `keccak256("") =
   c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470` and
   `keccak256("abc") = 4e03657aea45a94fc7d47ba826c8d667c0d1e6e33a64a036ec44f58fa12d6c45` as gates.
6. **Zeroization**: every secret `[UInt8]` buffer wiped via `memset_s`/`resetBytes(in:)` in
   `defer`; **Data copy-on-write caveat is [C]** — minimize copies, prefer `[UInt8]` for all
   secret storage, never claim [P] for CoW-backed zeroization.
7. **Handle-store serialization** (T1.4 HandleStore lesson): all access behind one serial queue;
   `evict` = remove + wipe inside the same critical region; copies handed out are freshly
   allocated (no CoW aliasing of the stored seed).
8. **B.6 evidence discipline**: every `[x]` needs command + observed output in
   `specs/features/2026-10-16-T1.5-vault-core-ios-port/validation.md`; never weaken a failing
   test.
9. **[P]/[C]/[S] labeling**: iOS mlock = **[C]** (no documented guarantee) with a scheduled
   one-line real-device probe (C6); compensating controls = short residency + explicit
   zeroization + Keychain at-rest (security-ios scope, note only).

## Preflight (Step 0)

```bash
cd /home/superguo/Projects/NexVaultRN
git status && git log --oneline -3          # baseline: main at a05c862, T1.4 re-closed f11947c
git switch -c feature/T1.5-vault-core-ios-port
xcodebuild -version && xcrun simctl list devices available | head -20   # iOS env gate (risk b)
```

- **GATE-R4-SWIFTSECP (blocking, planning round)**: propose owner approval of
  `21-DOT-DEV/swift-secp256k1` **exact tag 0.23.2** (SPM; MIT; published 2026-05-23 ≥7-day age;
  active repo pushed 2026-10-16; record the Blockstream→21-DOT-DEV rename fact per R4-5 package
  diligence). Amendment = new rows in `specs/tech-stack.md` + `doc/dependencies.md` (§6c,
  CVE/OSV + trust evidence). **No integration before owner approval** (T1.3 BC 1.86 precedent).
  The planning round must also resolve the **integration route** with evidence (Expo iOS modules
  are CocoaPods-first: podspec dependency, vendored source, or SPM via the app target — present
  options in the ask-user round).
- **GATE-KECCAK (planning-round owner confirmation)**: CommonCrypto has no Keccak-256; the skill
  default is a self-written Keccak-f[1600] (`Keccak.swift`, ~60 lines, gated by the two vectors
  above) — the T1.3 "self-written glue + library for curve ops" pattern. A third-party keccak
  package would need its own R4 admission; do not silently pick one.
- **Oracle transcription**: re-read the T1.2/T1.3/T1.4 validation trails; transcribe vectors from
  the committed Kotlin tests (`Bip39Test.kt`, `Bip32Test.kt`, `EcdsaTest.kt`) into the Swift
  tests with `// TC-VC-XXX` traceability — never invent hex values.
- **API-surface verification (T1.3 jar-inspection lesson, iOS edition)**: after GATE-R4-SWIFTSECP
  approval, inspect the pinned swift-secp256k1 sources for the exposed API (sign with recoverable
  recid semantics? point multiply? scalar ops?) and record what exists before choosing the
  signing construction in Step 2.
- Wordlist resource: copy the exact `bip39_english.txt` bytes (sha256 `2f5eed53…`) into the
  module bundle; tests assert the bundled resource's sha256 equals the fixture.
- GATE-MODEL-SWITCH per planning round; T1.4 re-closure already recorded (`f11947c`).

## Step 1 — TS interface: frozen (verify only)

No TypeScript edits. Re-run the gates to prove the frozen layer stays green:

```bash
cd apps/mobile && npx --no-install tsc --noEmit
grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile --include='*.ts' --include='*.tsx' --exclude-dir=node_modules   # R1 (exit 1 = clean)
```

## Step 2 — Swift core (new files under `modules/vault-core/ios/`)

- `Bip39.swift` — NFKD via `String.precomposedStringWithCompatibilityMapping`; 12/15/18/21/24
  words; binary-search the 2048-word list; checksum recompute with `CC_SHA256`;
  `mnemonicToSeed` via `CCKeyDerivationPBKDF(kCCPBKDF2, …, kCCPRFHmacAlgSHA512, 2048, 64)`;
  no fallback needed (system framework); non-`kCCSuccess` → `E_VAULT_PBKDF2_UNAVAILABLE`.
- `Bip32.swift` — **self-written CKD glue** (T1.3 pattern): HMAC-SHA512 via
  `CCHmac(kCCHmacAlgSHA512, …)`, hardened-bit `0x80000000`, path whitelist
  `^m/44'/60'/(\d+)'/0/(\d+)$` (account/index < 2^31); point ops only via swift-secp256k1;
  address = keccak256(uncompressed-pub[1...64])[12...31] + EIP-55 (ERC-55 algorithm + 4 vectors).
- `Keccak.swift` — self-written Keccak-f[1600] per GATE-KECCAK; gated by the two vectors above.
- `Ecdsa.swift` — RFC6979 DRBG self-written (`CCHmac(kCCHmacAlgSHA256, …)`, §3.2 h.3
  reject-retry, no `mod n` shortcut — T1.4 NIT-2) + swift-secp256k1 for `kG`/scalar context
  per the verified API surface; **low-s normalization flips recid** (BLOCKER-1 semantics);
  output 65B `r‖s‖recid`.
- `VaultStore.swift` — serial-queue-protected `[String: [UInt8]]`; `evict` = remove + wipe
  in one critical region; fresh copies on read (no CoW aliasing).
- `VaultError.swift` — typed errors carrying the §2.4 codes, fixed templates, no fragments.

Zeroization discipline: `defer { memset_s(&buf, count, 0, count) }` for every secret buffer
(seed copies, DRBG K/V/h1, derived privkey, digest scratch). Document as **[C]**: CoW `Data`
copies, Foundation `String` intermediates (NFKD normalization), and any lib-internal buffers.

## Step 3 — Module wiring (`VaultCoreModule.swift`)

Per `specs/vault-core.md` §2.3 (post-H2 — handle is `String`):

```swift
AsyncFunction("importMnemonic") { (mnemonic: Data) async throws -> String }
AsyncFunction("deriveAddress") { (handle: String, path: String) async throws -> String }
AsyncFunction("signDigest") { (handle: String, path: String, digest: Data) async throws -> Data }
AsyncFunction("destroy") { (handle: String) async throws -> Void }
```

- digest length ≠ 32 → `E_VAULT_INVALID_DIGEST_LENGTH` **before** touching the store.
- `destroy`: idempotent, never throws for an absent handle.
- No `print`/`NSLog`/`os_log` (R5). Errors map to the same codes as Android.

## Step 4 — Tests (XCTest, oracle vectors first)

`xcodebuild -list` first (the `./gradlew projects` lesson), then:

```bash
xcodebuild test -project ios/VaultCore.xcodeproj -scheme VaultCore   -destination 'platform=iOS Simulator,name=iPhone 16'        # auxiliary gate (fast loop)
xcodebuild test -destination 'platform=iOS,id=<iPhone-UDID>'   # authoritative device gate (pin ONE udid)
```

XCTest suites (`Bip39Tests.swift` / `Bip32Tests.swift` / `EcdsaTests.swift` /
`VaultStoreTests.swift`), mirroring the Android gate, oracle-transcribed with TC comments:

1. BIP-39 V1–V8 (T1.2 oracle, incl. V7/V8 derived + entropy/checksum decode assertions).
2. BIP-32 official vectors 1–4, all 17 chains xprv/xpub via a test-side 78-byte + Base58Check
   serializer (BC `Base58` was absent — Swift tests must self-write the same serializer);
   canonical address `0x9858EfFD232B4033E47d90003D41EC34EcaEda94` + second address
   `0x6Fac4D18…` (T1.3 oracle values).
3. keccak256("")/("abc") + the 4 ERC-55 vectors.
4. Probe KAT (T1.4 oracle): d=`C9AFA9D8…F6721`, digest=sha256("sample") → r=`432310e3…6a27`,
   s=`530128b6…ab69`, recid=0.
5. **TC-VC-027** six fixed vectors (3 normalized / 3 non-normalized, incl. an `s` with leading
   `0x00`) and **TC-VC-028** 24-digest recovery consistency — transcribe from
   `EcdsaTest.kt`; these are the recid-oracle gates.
6. destroy semantics + 1000× import/destroy loop (store empty, no crash); digest/path/handle
   negatives; determinism (two signs byte-identical).
7. Wordlist resource sha256 == `2f5eed53…`.

Manual side (B.4): register MAN cases — TC-VC-005 iOS memory walk (Instruments/Profiler PSS-style
curve on a real iPhone) and the **mlock one-line probe** (C6): `mlock`/`munlock` a test page
on the device, record errno/result, no functional dependency; simulator results are auxiliary
only (B.4-5). Bridge invocation (JS↔native through the RN runtime) stays out of T1.5 —
same H8 class as Android, owner-adjudicated replan → T4.1; record honestly in the device notes.

## Step 5 — Verification and handoff

```bash
cd /home/superguo/Projects/NexVaultRN
git diff -- package.json apps/mobile/package.json pnpm-lock.yaml    # zero npm drift (SPM/podspec changes recorded separately)
cd apps/mobile && npx --no-install tsc --noEmit
grep -rnE 'print(|NSLog|os_log' modules/vault-core/ios              # R5 native log check (exit 1 = clean)
```

Write into `specs/features/2026-10-16-T1.5-vault-core-ios-port/validation.md`: every command,
exit code, observed result; vector pass/fail tables; zeroization review (each `memset_s`/
`defer` location + the [C] caveats: CoW Data, String intermediates, lib-internal buffers);
GATE-R4-SWIFTSECP approval record; evidence/ dir with device xcresult/XML artifacts (NIT-3
pattern). Handoff list candidates: mlock probe result (C6), Secure Enclave/Keychain at-rest →
security-ios, bridge smoke → T4.1 (H8 class, re-adjudicate there), C++ core → roadmap,
passphrase/multi-chain → roadmap. Do not mark T1.5 `[x]` until the XCTest oracle gates, the
device run, the zeroization review and the manual MAN walk are recorded with command + output.

## Appendix — Parity table (merged Android skeleton → iOS clause)

| Android-skill section | iOS clause | Status |
|---|---|---|
| Provenance / rejected sources | Same + CryptoSwift/CryptoKit/A.2.5-trap rejections | swapped |
| Scope (single-method per skill) | All four methods in one skill; oracle = Android | merged |
| Invariant R1 ArrayBuffer boundary | Same, `Data` in, handle out; native-side wipe | inherited |
| Invariant opaque handle | `UUID().uuidString`, store native-only | swapped |
| Invariant R4 dependency gate | GATE-R4-SWIFTSECP (0.23.2 proposal) + GATE-KECCAK | swapped |
| Invariant R5 logging | Same, `print`/`NSLog`/`os_log` banned | inherited |
| Invariant zeroization | `[UInt8]`+`memset_s`/`defer`; CoW Data [C] | swapped |
| Preflight autolinking grep | `xcodebuild -list` + autolinking check | swapped |
| Preflight vector download + sha256 | Same sources; transcription from Kotlin tests | inherited |
| Jar inspection (T1.3 lesson) | swift-secp256k1 vendored-source API inspection | swapped |
| Step 1 TS interface | Frozen — verify-only | N/A (no TS work) |
| Step 2 BIP-39 core | `Bip39.swift`, CommonCrypto KDF, no fallback | swapped |
| Step 2 BIP-32 core | `Bip32.swift`, self-written CKD + lib point ops | swapped |
| Step 2 keccak/EIP-55 | `Keccak.swift` self-written (CommonCrypto has none) | swapped + GATE |
| Step 2 RFC6979 signing | `Ecdsa.swift`, recid-flip oracle (TC-VC-027/028) | swapped |
| Step 2 handle store | `VaultStore.swift` serial queue, evict=remove+wipe | swapped |
| Step 3 module wiring | 4×`AsyncFunction` per §2.3 | swapped |
| Step 4 JVM vectors | XCTest same vector sets (oracle-transcribed) | swapped |
| Step 4 device instrumentation | iPhone XCTest, one pinned UDID; simulator auxiliary | swapped |
| Step 4 MAN walks | MAN: iOS memory walk + mlock probe (C6) | swapped |
| Step 5 greps/tsc/evidence | Same + native log grep; evidence/ dir | inherited |
| Android mlock [P]/[C] | iOS mlock **no guarantee → [C]** + probe scheduled | iOS-specific |
| H8 bridge invocation | Same H8 class → T4.1 (record honestly) | inherited |
| H9 zeroization patch | N/A — fresh Swift code (no inherited patch) | N/A + reason |
