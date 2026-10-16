# T1.5 — vault-core iOS four-method port (requirements)

> Feature spec (requirements). Planning round **2026-10-16**. Branch:
> feature/T1.5-vault-core-ios-port (created from main f9e9630 before this file was written).
> Parent roadmap item: plans/3-day-implementation-plan.md Day 2 T1.5（关键路径：iOS Swift 薄封装移植）.
> Skill: nexvault-t1-5-vault-core-ios-port（committed at .agents/skills/nexvault-t1-5-vault-core-ios-port/SKILL.md,
> commit f9e9630; also copied in this feature directory as SKILL.md; both copies sha256
> 2b0b26dc9b8540fbe35664d8e512f44863a7c5b4fa10e2b109f4ef4e00968bb8）.
> Sibling files: plan.md, validation.md.
> Session status: planning round **2026-10-16** wrote this record. Implementation starts only
> after GATE-MODEL-SWITCH (owner switches the model).
> Process guarantees (T1.4 lessons, owner Q6): **no closure/merge/board-row/push without the
> owner's explicit call**; no production-code edit before GATE-MODEL-SWITCH.

## Goal

Port the four frozen vault-core methods to iOS in modules/vault-core/ios (Swift, same module
name VaultCore). **Behavioral oracle = the verified Android implementation** (T1.2 16/16 + 2/2,
T1.3 26/26 + 4/4, T1.4 37/37 + 6/6 after the BLOCKER-1 recid fix):

- **importMnemonic** — BIP-39 validation (bundled wordlist, byte-identical to the Android
  res/raw/bip39_english.txt) + seed via CommonCrypto CCKeyDerivationPBKDF
  (kCCPBKDF2/HmacSHA512, 2048 rounds — owner gate-3 unification a05c862; no fallback).
- **deriveAddress** — BIP-32/44 CKD (self-written glue on CommonCrypto HMAC-SHA512) +
  **swift-secp256k1 0.23.2** curve math (no self-written EC arithmetic, spec §2.2) +
  **self-written Keccak-256** + EIP-55 → canonical 0x9858EfFD…Eda94.
- **signDigest** — RFC6979 deterministic ECDSA, 65 bytes r(32)‖s(32)‖recid(1), low-s,
  **recid flips when low-s negates s** (the T1.4 BLOCKER-1 rule; TC-VC-027/028 are
  cross-platform gates and byte-equality with the Android signatures is mandatory).
- **destroy** — serialized handle store; evict = remove + wipe inside one critical region;
  idempotent; post-destroy calls throw E_VAULT_HANDLE_NOT_FOUND.

**TS layer frozen**: zero TypeScript changes; tsc --noEmit stays green as a gate.

## Owner decisions (2026-10-16, ask-user round — all confirmed)

| # | Item | Decision |
| --- | --- | --- |
| D1 | GATE-R4-SWIFTSECP | **Approved: 21-DOT-DEV swift-secp256k1 exact 0.23.2 via CocoaPods** (s.dependency 'swift-secp256k1', '0.23.2'); consequence accepted: pod requires **iOS ≥ 18.0** → app.json ios.deploymentTarget: '18.0' (CNG). Vendored xcframework (31.5 MB zip) and SPM-via-app-target (no Expo support) rejected as routes |
| D2 | GATE-KECCAK | **Self-written Keccak-f[1600]** (Keccak.swift, ~60 lines), gated by keccak256('') and keccak256('abc'); no third-party package |
| D3 | iOS environment | **Mac + iPhone available at implementation time** (owner-provided access); if unreachable → Handoff T1.5-H1 and T1.5 cannot reach [x] (risk b) |
| D4 | Module contract | Serial DispatchQueue store, UUID().uuidString handles, evict+wipe atomic; wordlist as iOS bundle resource with sha256 2f5eed53…b24dbda asserted; Data in / handle/address/65-byte-signature out; §2.4 error codes identical to Android; no print/NSLog/os_log; [UInt8] secrets + memset_s/resetBytes(in:), Data CoW = [C]; TS frozen |
| D5 | Oracle | **Transcribe all vectors from the committed Kotlin tests** (never invent): BIP-39 V1–V8, BIP-32 V1–V4 (17 chains), canonical + second address, keccak/ERC-55, probe KAT, TC-VC-027 six vectors, TC-VC-028 recovery class, destroy/1000, negatives, determinism |
| D6 | Process | Spec trio + SKILL.md copy committed on the branch → **STOP (GATE-MODEL-SWITCH)**; implementation after the owner switches the model; **close only on the owner's explicit call; push only on the owner's explicit call** |

## Ask-user round (C.1-2, all confirmed 2026-10-16)

| Q | Question | Decision |
| --- | --- | --- |
| Q1 | R4 approval: swift-secp256k1 exact 0.23.2 via CocoaPods + iOS 18.0 minimum? | **Approve pod route + iOS 18.0 minimum** |
| Q2 | Keccak: self-written Keccak-f[1600]? | **Confirmed — self-written Keccak-f[1600]** |
| Q3 | iOS verification environment (Linux host, no Xcode/eas.json/iPhone attached)? | **Mac + iPhone available at implementation time** (fallback → Handoff T1.5-H1) |
| Q4 | Module contract (serial store, UUID handles, bundle wordlist, §2.4 codes, no logs, CoW [C], TS frozen)? | **Confirmed** |
| Q5 | Oracle transcription scope (V1–V8, BIP-32 17 chains, TC-VC-027/028, destroy/1000, negatives)? | **Confirmed — transcribe from the Kotlin oracle** |
| Q6 | Model switch + closure/push discipline? | **Confirmed** — spec trio committed → stop → model switch; closure/push only on explicit owner calls |

## Reference precedence for unclarified questions (owner rule, carried from T1.2 Q8)

1. **RN/TS/JS/Expo topics**: this repo's AGENTS.md, apps/mobile/AGENTS.md, the T1.5 skill,
   and the Expo SDK 57 versioned docs (docs.expo.dev/versions/v57.0.0 — never memory).
2. **All other topics** (Swift, Xcode/XCTest, BIP-39/32/44, RFC6979, CommonCrypto,
   swift-secp256k1): the Android oracle (committed Kotlin tests + validation trails) is
   authoritative for behavior; API usage resolves from the consulted Apple/21-DOT-DEV docs
   recorded in validation.md.
3. **Conflict resolution**: this repo's AGENTS.md R1–R8/R4, the T1.5 skill, and this feature
   spec win.
4. **Still unresolved?** Stop and ask the owner before writing code.

## Handoff re-read (AGENTS.md B.7 — every pre-existing candidate gets an explicit verdict)

| Source | Deferred entry | Verdict for T1.5 |
| --- | --- | --- |
| T1.3 validation.md H2 | iOS port of the four methods | **Implement** — this round |
| T1.4 validation.md T1.4-H3 | TC-VC-005 iPhone side (memory hygiene) | **Implement** — device 1000× loop + manual walk → **MAN-012** |
| T1.4 validation.md T1.4-H1 | JS→native bridge smoke | **Replan → T4.1** (record only; iOS device tests exercise the same code the AsyncFunction runs, honestly labeled) |
| T1.4 validation.md T1.4-H2 | jest carriers | **Replan → T1.1 closure / T4.1** |
| AGENTS.md B.7-3 | iOS mlock semantics | **[C]** + one-line real-device probe → **MAN-013** (record errno/result; no functional dependency) |
| AGENTS.md B.7-3 others | word-grid keyboard / C++ core / Secure Enclave wrap | unchanged roadmap items; Secure Enclave/Keychain at-rest = security-ios scope (note only) |

## Functional requirements

- **FR-1 — R4 gate and amendments (GATE-R4-SWIFTSECP).** The owner-approved evidence (MIT; tag
  published 2026-05-23 ≥7-day age; repo pushed 2026-10-16, 160 stars; release xcframework zip
  sha256 0084b98982506a85dfbdf4568236c7c95cc77f10c2b8cdc0e567647844c00f77; pod platform iOS
  ≥18.0; default build traits include the libsecp256k1 recovery module;
  Blockstream→21-DOT-DEV rename recorded per R4-5) is written into specs/tech-stack.md +
  doc/dependencies.md §6c and the approval is recorded in validation.md **before** any
  podspec/app.json change.
- **FR-2 — Deployment target.** apps/mobile/app.json gains 'ios': { 'deploymentTarget':
  '18.0', … } (CNG only; never hand-edit ios/). R3 note: deployment-target change =
  new-binary class (nothing published yet).
- **FR-3 — Keccak.swift.** Self-written Keccak-f[1600] sponge (rate 1088, output 32 bytes —
  pre-NIST Keccak-256, never SHA3); gates: keccak256('') =
  c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470 and keccak256('abc') =
  4e03657aea45a94fc7d47ba826c8d667c0d1e6e33a64a036ec44f58fa12d6c45.
- **FR-4 — Bip39.swift.** Wordlist loaded from the module bundle (byte-identical copy of the
  Android res/raw/bip39_english.txt, sha256 2f5eed53a4727b4bf8880d8f3f199efc90e58503646d9ff
  8eff3a2ed3b24dbda asserted in tests); NFKD normalization (String intermediate = [C]);
  12/15/18/21/24 words; checksum via CC_SHA256; seed via CCKeyDerivationPBKDF over a
  [UInt8]-backed password with kCCPBKDF2/kCCPRFHmacAlgSHA512/2048 rounds; every secret
  buffer wiped with memset_s/defer; no String password reuse.
- **FR-5 — Bip32.swift.** Self-written CKD glue (CCHmac SHA-512 with key 'Bitcoin seed' /
  chain codes; ser32/ser256; HASH160 fingerprint via CC_SHA256 + self-written RIPEMD-160,
  gated by the BIP-32 vectors; path whitelist identical to Android m/44'/60'/<a>'/0/<i> with
  the same error semantics). All curve arithmetic via swift-secp256k1 only (spec §2.2).
- **FR-6 — Ecdsa.swift.** RFC6979 HMAC-SHA256 DRBG self-written (mirrors Ecdsa.kt —
  byte-wipe controlled, §3.2 h.3 reject semantics, cap 64) wired through libsecp256k1's
  recoverable-sign nonce callback if the P256K API exposes one; otherwise the built-in
  noncefp == NULL RFC6979 fallback is used and the loss of k-buffer wipe control is recorded
  as **[C]**. Low-s normalization + **recid flip** (BLOCKER-1 rule) verified against
  TC-VC-027; 65-byte output; byte-equality with the Android signatures is mandatory (B.6-5 —
  divergence is investigated, never papered over).
- **FR-7 — VaultStore.swift.** One serial DispatchQueue; put (UUID().uuidString),
  copyOf (fresh [UInt8] copy — no CoW aliasing), evict (remove + memset_s inside the
  same critical region, idempotent), count — the T1.4 MINOR-1 contract.
- **FR-8 — Module wiring.** VaultCoreModule.swift: four AsyncFunctions per specs/vault-core.md
  §2.3; Data inputs copied to [UInt8] and wiped; typed errors with the §2.4 whitelist codes
  identical to Android (E_VAULT_INVALID_MNEMONIC, E_VAULT_PBKDF2_UNAVAILABLE,
  E_VAULT_INVALID_PATH, E_VAULT_INVALID_DIGEST_LENGTH, E_VAULT_HANDLE_NOT_FOUND,
  E_VAULT_CRYPTO_INTERNAL, E_VAULT_UNSUPPORTED_PLATFORM); no print/NSLog/os_log (R5);
  fixed templates, no input fragments.
- **FR-9 — Podspec.** VaultCore.podspec: s.dependency 'swift-secp256k1', '0.23.2',
  s.platforms = { :ios => '18.0', :tvos => '18.0' }, wordlist under a resource bundle,
  honest summary/description/author metadata (no fake entries).
- **FR-10 — XCTest oracle suites.** Transcribed from the committed Kotlin tests with
  // TC-VC-XXX traceability: BIP-39 V1–V8 (incl. V7/V8 entropy/checksum decode), BIP-32
  V1–V4 all 17 chains xprv/xpub (test-side 78-byte + Base58Check serializer), canonical +
  second address, keccak/ERC-55, probe KAT, TC-VC-027 six pinned vectors (normalizing and
  not, incl. the 0x00-leading s), TC-VC-028-style recovery consistency, destroy wipe/
  idempotency/copy-independence + 1000-cycle loop, digest/path negatives, determinism.
- **FR-11 — Device gate + manual cases.** iPhone XCTest with one pinned UDID (authoritative,
  B.4-5); **MAN-012** (iOS memory walk: 1000× import→derive→sign→destroy with a PSS-style
  curve, no monotonic growth) and **MAN-013** (mlock one-line probe: mlock/munlock a test
  page, record errno/result, [C] either way) registered in
  specs/pending-manual-test-cases.md.
- **FR-12 — Frozen TS + static discipline + GATE-MODEL-SWITCH.** Zero TypeScript changes;
  zero npm drift; tsc --noEmit exit 0; grep -rnE 'print(|NSLog|os_log' modules/vault-core/ios
  exit 1; GATE-MODEL-SWITCH recorded before any production-code edit.

## Vector anchors (transcribed from the Kotlin oracle — never invented)

- Wordlist sha256 2f5eed53a4727b4bf8880d8f3f199efc90e58503646d9ff8eff3a2ed3b24dbda.
- Canonical anchor: abandon×23 about @ m/44'/60'/0'/0/0 → 0x9858EfFD232B4033E47d90003D41EC34EcaEda94;
  second address m/44'/60'/0'/0/1 → 0x6Fac4D18c912343BF86fa7049364Dd4E424Ab9C0.
- keccak256('') / keccak256('abc') as in FR-3; four ERC-55 vectors from Bip32Test.kt.
- Probe KAT: d C9AFA9D8…F6721, sha256('sample') → r 432310e3…a6c8, s 530128b6…ab69, recid 0.
- TC-VC-027 six vectors: nexvault-test-vector(-2…-6) — full 65-byte signatures incl. the
  three normalizing cases (-2 s starts 0x00, -4, -6) where recid is the flipped value.
- Determinism: two signs byte-identical; low-s invariant; 24-digest recovery consistency
  (TC-VC-028 class) with the recovery formula assembled from lib point ops in the test.

## Out of scope

- Any TypeScript/JS change (TS layer frozen); Web implementation; Android changes.
- At-rest key wrap / Keychain / Secure Enclave (security-ios); biometric gating.
- JS→native bridge smoke (T1.4-H1 class → T4.1); jest carriers (→ T1.1/T4.1).
- BIP-39 passphrase, multi-chain, native word-grid keyboard, C++ core, mlock as a protection
  (probe only, [C]).
- Any dependency beyond the owner-approved swift-secp256k1 0.23.2 (FR-1).

## Acceptance criteria (each AC binds a test/gate and evidence location)

| AC | Content | Test/gate |
| --- | --- | --- |
| AC-1 | R4 amendments (specs/tech-stack.md + doc/dependencies.md §6c) and the approval record precede any podspec/app.json change | GATE-R4-SWIFTSECP evidence (Task 1) |
| AC-2 | XCTest oracle suites green: BIP-39 V1–V8 + BIP-32 V1–V4 17 chains + keccak/ERC-55 + probe KAT | Task 4 suites on the Mac |
| AC-3 | TC-VC-027 six signatures byte-equal to the Android oracle (recid-flip covered) + TC-VC-028-style recovery consistency + determinism | Task 4 + device (Task 5) |
| AC-4 | keccak256('')/keccak256('abc') gates pass; ERC-55 vectors pass | KeccakAndEip55Tests |
| AC-5 | destroy wipes in place, idempotent, copy independence, 1000-cycle loop leaves the store empty | VaultStoreTests + device |
| AC-6 | §2.4 error codes identical to Android; no print/NSLog/os_log (grep exit 1); fixed templates | Task 6 greps + negative tests |
| AC-7 | Zeroization review lists every memset_s/defer site with file:line; [C] rows: Data CoW, String intermediates, lib internal buffers (if built-in nonce); mlock labeled [C] + probe result | Task 6 review + MAN-013 |
| AC-8 | iPhone XCTest green with one pinned UDID; simulator auxiliary only | Task 5 device gate |
| AC-9 | MAN-012 (iOS memory walk) and MAN-013 (mlock probe) registered; conclusions only 通过/失败/阻塞 | Task 5 + specs/pending-manual-test-cases.md |
| AC-10 | Zero TS diff; zero npm drift; tsc --noEmit exit 0; R1/R5-style greps clean | Task 6 static checks |
| AC-11 | validation.md contains command + exit code + observed output for every task; no [x] without evidence | B.4/B.6 audit |
| AC-12 | GATE-MODEL-SWITCH recorded before any code; **closure and push only on the owner's explicit calls** | FR-12 / plan Task 7 |

## Security-level labelling (R6 — no level inflation)

| Mechanism | Label | Reason |
| --- | --- | --- |
| Private key never crosses the JS↔native boundary; only the public address/signature returned | **[P]** (within T1.5 scope) | API shape + store + greps |
| RFC6979 determinism; nonce never externally supplied | **[P]** (within T1.5 scope) | DRBG internal (or lib noncefp==NULL); k-reuse class structurally excluded |
| [UInt8] + memset_s/resetBytes(in:) zeroization | **[C]** | Swift CoW Data, String intermediates, lib internal buffers are GC/CoW-retained copies |
| Data copy-on-write zeroization | **[C]** | Minimized copies; never [P] |
| iOS mlock | **[C]** | No documented guarantee in the iOS sandbox (C6); one-line probe scheduled; no functional dependency |
| JS-side platform guard / TS frozen layer | **[S]** | Contract; the real R7 boundary is the web stub + CI grep |

## Handoff (owned by T1.5, adjudicated by the next planning round)

| # | Item | Reason | Suggested disposal | AC/TC |
| --- | --- | --- | --- | --- |
| T1.5-H1 | Mac/iPhone unreachable at implementation | Environment (D3); only if it happens | Replan the device/XCTest gates; T1.5 stays [~] (risk b) | AC-2/AC-8 |
| T1.5-H2 | JS→native bridge invocation (H8 class) | Needs an RN host on iOS too | Replan → T4.1 dev-build screen smoke | AC-8 |
| T1.5-H3 | At-rest key wrap / Keychain / Secure Enclave | security-ios scope | Replan → security-ios feature | — |
| T1.5-H4 | mlock probe result + [C] labeling | C6 unresolved item | Recorded this round via MAN-013; roadmap follow-up if semantics warrant | AC-7/AC-9 |
