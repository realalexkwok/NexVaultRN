# T1.5 — vault-core iOS four-method port (plan)

> Feature spec (plan). Branch: feature/T1.5-vault-core-ios-port.
> Parent: plans/3-day-implementation-plan.md Day 2 T1.5（关键路径：iOS Swift 薄封装移植）.
> Skill: feature-dir SKILL.md, sha256 2b0b26dc9b8540fbe35664d8e512f44863a7c5b4fa10e2b109f4ef4e00968bb8.
> Session status: planning written 2026-10-16 (this file); implementation executes after
> GATE-MODEL-SWITCH (owner switches the model).
> Process guarantees (owner Q6): no closure/merge/board-row/push without the owner's explicit
> call; no production-code edit before GATE-MODEL-SWITCH.

## Hard gates (read before any work)

- **GATE-MODEL-SWITCH** — the planning session commits the spec files and **stops**; the
  implementation session runs only after the owner switches the model.
- **GATE-R4-SWIFTSECP (owner-approved 2026-10-16, D1)** — swift-secp256k1 **exact 0.23.2** via
  CocoaPods. The amendments (specs/tech-stack.md + doc/dependencies.md §6c) and the approval
  record land **before** any podspec/app.json change.
- **GATE-KECCAK (owner-confirmed 2026-10-16, D2)** — self-written Keccak-f[1600]; no
  third-party keccak package.
- **No TS change** — the TS layer is frozen (T1.2/T1.4 wrappers); tsc --noEmit stays green.
- **Oracle rule (D5)** — every vector value is transcribed from the committed Kotlin tests
  (Bip39Test/Bip32Test/EcdsaTest/HandleStoreTest.kt) and the T1.2–T1.4 validation trails;
  never invent hex values.

## Task 0 — Preflight, R4 evidence, API inspection

- git status/branch; Mac reachability probe (ssh/local xcodebuild -version, xcrun simctl
  list, xcrun xctrace list devices → pin one iPhone UDID). If the Mac is unreachable: stop
  and record Handoff T1.5-H1 (D3) — T1.5 cannot reach [x] without it.
- Re-verify and record the R4 evidence already gathered this planning round: CocoaPods trunk
  podspec for swift-secp256k1 0.23.2 (MIT; vendored P256K.xcframework; zip sha256
  0084b98982506a85dfbdf4568236c7c95cc77f10c2b8cdc0e567647844c00f77; platforms ios 18.0;
  module_name P256K; swift_versions 6.0); GitHub release 2026-05-23T07:46:28Z (≥7-day age);
  repo pushed 2026-10-16, 160 stars; Blockstream→21-DOT-DEV rename fact (R4-5).
- P256K API inspection: recoverable-sign / point-mul surface; whether a custom RFC6979 nonce
  callback can be passed (else the built-in noncefp == NULL RFC6979 fallback per FR-6, [C]);
  consult docs.21.dev/data/documentation/p256k/llms.txt and record what was read.
- Oracle re-read: transcribe the vector suites from the Kotlin tests with TC-VC markers.
- Autolinking: npx expo-modules-autolinking search --platform apple --json | grep vault-core.

## Task 1 — R4 amendments + deployment target (after the approval record)
- specs/tech-stack.md: swift-secp256k1 row (0.23.2, MIT, purpose, iOS 18.0 constraint).
- doc/dependencies.md §6c: version, purpose, publish date, CVE disposition, trust basis,
  rename fact, zip sha256, pod platform constraint.
- apps/mobile/app.json: 'ios': { 'deploymentTarget': '18.0', … } (CNG only; never hand-edit
  ios/; R3 note recorded).

## Task 2 — Swift cores (modules/vault-core/ios/)
- **Keccak.swift** — self-written Keccak-f[1600], rate 1088, 32-byte output; gates
  keccak256('') and keccak256('abc').
- **Bip39.swift** — wordlist from the module bundle (sha256 2f5eed53…b24dbda asserted); NFKD
  normalize (String = [C]); 12/15/18/21/24 + checksum via CC_SHA256; seed via
  CCKeyDerivationPBKDF (kCCPBKDF2/kCCPRFHmacAlgSHA512/2048) over a [UInt8]-backed password;
  memset_s/defer on every secret buffer; no String password reuse.
- **Bip32.swift** — self-written CKD glue (CCHmac SHA-512; ser32/ser256; HASH160 = CC_SHA256
  + self-written RIPEMD-160, gated by the BIP-32 vectors; path whitelist identical to
  Android). All curve arithmetic via swift-secp256k1 only (spec §2.2).
- **Ecdsa.swift** — RFC6979 HMAC-SHA256 DRBG self-written (mirrors Ecdsa.kt: h.3 reject
  semantics, cap 64, byte-wipe controlled), wired through libsecp256k1's recoverable-sign
  nonce callback if P256K exposes one; otherwise built-in noncefp == NULL RFC6979 ([C]
  recorded). Low-s + **recid flip** (BLOCKER-1) verified against TC-VC-027; 65-byte output;
  byte-equality with Android mandatory (B.6-5).
- **VaultStore.swift** — one serial DispatchQueue; put/copyOf (fresh [UInt8] copy)/evict
  (remove + memset_s in the same critical region)/count — the T1.4 MINOR-1 contract.

## Task 3 — Module wiring
- VaultCoreModule.swift: four AsyncFunctions per specs/vault-core.md §2.3; Data → [UInt8]
  copies wiped; typed CodedExceptions with the §2.4 codes; no print/NSLog/os_log.
- VaultCore.podspec: s.dependency 'swift-secp256k1', '0.23.2'; s.platforms ios/tvos 18.0;
  wordlist resource bundle; honest metadata.
- Resources/bip39_english.txt — byte-identical copy of the Android res/raw file (sha256
  asserted in tests).

## Task 4 — XCTest suites (oracle-transcribed, // TC-VC-XXX)

| Suite | Content |
| --- | --- |
| Bip39Tests.swift | V1–V8 incl. entropy/checksum decode assertions |
| Bip32Tests.swift | BIP-32 vectors 1–4, all 17 chains xprv/xpub (test-side 78-byte + Base58Check serializer); canonical + second address |
| KeccakAndEip55Tests.swift | keccak256('')/('abc') + the four ERC-55 vectors |
| EcdsaTests.swift | probe KAT; TC-VC-027 six pinned signatures (byte-equal to Android incl. recid-flip cases); TC-VC-028-style 24-digest recovery consistency; determinism; digest/path negatives |
| VaultStoreTests.swift | destroy wipe/idempotency/copy-independence + 1000-cycle loop |

Run: xcodebuild test on the simulator (auxiliary) and the physical iPhone (authoritative,
B.4-5) with one pinned UDID.

## Task 5 — Manual side (B.4)
- **MAN-012**: iOS memory walk — device 1000× import→derive→sign→destroy; Instruments/
  Profiler PSS-style curve, no monotonic growth (±5% band carried by the automated assertion
  as on Android).
- **MAN-013**: mlock one-line probe — mlock/munlock a test page on the device; record
  errno/result; label [C] either way; no functional dependency (C6).
- Register both in specs/pending-manual-test-cases.md; conclusions only 通过/失败/阻塞.

## Task 6 — Verification, evidence, handoffs
- Zero TS diff; pnpm-lock.yaml untouched; tsc --noEmit exit 0; grep -rnE 'print(|NSLog|os_log'
  modules/vault-core/ios → exit 1.
- Zeroization review: every memset_s/defer site with file:line; [C] rows: Data CoW, String
  intermediates, lib internal buffers (if built-in nonce), mlock.
- Evidence into the feature evidence/ (xcresult/XML + device record; NIT-3 pattern).
- Handoff list: T1.5-H1 (only if the Mac was unreachable), bridge smoke → T4.1, at-rest →
  security-ios, mlock probe result, C++ core/passphrase → roadmap.
- **Do not mark T1.5 [x].** Status [~] + 'awaiting owner close call and independent review'.

## Task 7 — Board + git (owner-gated)
- On the owner's explicit close: board row + git merge --no-ff + branch deletion; push only
  on the owner's explicit go-ahead (D6).

## File manifest

| File | Action |
| --- | --- |
| specs/features/2026-10-16-T1.5-vault-core-ios-port/{requirements,plan,validation}.md + SKILL.md | create (Phase A) |
| modules/vault-core/ios/{Keccak,Bip39,Bip32,Ecdsa,VaultStore}.swift | create (Task 2) |
| modules/vault-core/ios/VaultCoreModule.swift, VaultCore.podspec | modify (Task 3) |
| modules/vault-core/ios/Resources/bip39_english.txt | create (byte-identical copy) |
| modules/vault-core/ios/Tests/*Tests.swift | create (Task 4) |
| apps/mobile/app.json | modify (ios.deploymentTarget 18.0) |
| specs/tech-stack.md, doc/dependencies.md §6c | amend (Task 1, R4) |
| specs/pending-manual-test-cases.md (MAN-012/013), AGENTS.md Part A | modify (Tasks 5/7) |
| All modules/vault-core TS files | **unchanged** (TS frozen) |

## Edge cases & failure modes

- Mac/iPhone unreachable → Handoff T1.5-H1; T1.5 stays [~] (D3, risk b).
- P256K API surprises (renamed symbols, no nonce callback) → built-in RFC6979 fallback
  recorded as [C]; oracle vectors still gate correctness.
- Oracle divergence (iOS signature ≠ Android bytes) → root-cause; never weaken tests.
- RIPEMD-160 absent from CommonCrypto → self-written, gated by the BIP-32 fingerprints.
- CCKeyDerivationPBKDF wants a Data password → build from [UInt8], wipe after; no String
  password reuse.
- Duplicate iPhone transports → pin one UDID (T1.2 ANDROID_SERIAL lesson).
- iOS 18.0 minimum → recorded; no OTA implications yet (nothing published).
- CoW Data zeroization and String intermediates → [C], never [P] (R6).

## Acceptance criteria mapping (requirements.md AC-1..12)

R4 amendment + approval record (AC-1) · XCTest oracle gates (AC-2) · recid-flip oracle
TC-VC-027/028 (AC-3) · keccak gates (AC-4) · destroy/1000 + idempotency (AC-5) · §2.4
error-code parity + no log calls (AC-6) · zeroization review + [C] labels (AC-7) · iPhone
device gate, pinned UDID (AC-8) · MAN-012/013 walks (AC-9) · zero drift/tsc/greps (AC-10) ·
validation discipline (AC-11) · GATE-MODEL-SWITCH + owner-gated closure/push (AC-12).

## Risks

| Risk | Trigger | Response |
| --- | --- | --- |
| b: no Mac/iPhone at implementation | xcodebuild unreachable | Handoff T1.5-H1; T1.5 cannot reach [x] (owner D3) |
| P256K API mismatch | compile errors on new names | Task 0 inspection + docs; record consulted sources; never guess signatures |
| Oracle divergence | XCTest vector failure | root-cause; B.6-5 (never weaken tests) |
| Pod install network/checksum | CocoaPods fetch fails | podspec pins the release; sha256 recorded; retry/record |
| iOS 18.0 constraint | pod platform mismatch | deployment target already planned in app.json (D1) |
| Device transport duplication | two UDID entries | pin one UDID per run |
