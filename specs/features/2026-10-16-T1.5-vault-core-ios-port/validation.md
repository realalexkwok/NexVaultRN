# T1.5 — vault-core iOS four-method port (validation)

> Feature spec (validation). Branch: feature/T1.5-vault-core-ios-port.
> Parent: plans/3-day-implementation-plan.md Day 2 T1.5（关键路径：iOS Swift 薄封装移植）.
> Skill: feature-dir SKILL.md, sha256 2b0b26dc9b8540fbe35664d8e512f44863a7c5b4fa10e2b109f4ef4e00968bb8.
> Session status: **Linux implementation session 2026-10-16** (model deepseek-flash, after
> GATE-MODEL-SWITCH). This host has **no Apple toolchain** (no xcodebuild/xcrun/swift, no
> reachable Mac, no eas.json/CI runner); per the owner's instruction the branch is handed off to
> a Mac session — see **MAC-HANDOFF.md** in this directory. Everything authored here is
> **uncompiled** until that session runs; nothing is marked `[x]`.

## Gate checklist

| Gate | Status | Evidence |
| --- | --- | --- |
| GATE-MODEL-SWITCH | `[x]` satisfied | Owner switched the model; this session runs deepseek-flash |
| GATE-R4-SWIFTSECP | `[x]` owner-approved 2026-10-16 (Q1) | Amendments landed: specs/tech-stack.md row + doc/dependencies.md §6c (Task 1) |
| GATE-KECCAK | `[x]` owner-confirmed 2026-10-16 (Q2) | Self-written Keccak.swift + the two pinned vectors in Tests/KeccakTests.swift |
| GATE-M4-RFC6979 | `[x]` owner-confirmed 2026-10-16 (Q2 ruling) | Built-in libsecp256k1 RFC6979 (noncefp == NULL); k-buffer wipe control recorded as **[C]** |

## Task 0 — Preflight, R4 evidence, API inspection

Status: `[~]` — everything executable on Linux was done; the Mac probe failed (below).

| Check | Command | Exit | Observed result |
| --- | --- | --- | --- |
| Baseline | `git branch --show-current`, `git log --oneline -2` | 0 | `feature/T1.5-vault-core-ios-port`, HEAD `b649962` (planning specs) on `f9e9630` |
| Apple toolchain | `which xcodebuild xcrun swift` | 1 | none on this Linux host |
| Mac reachability | `~/.ssh/config`, `known_hosts`, `which tailscale`, `ip -br addr` | — | no Mac host configured, no Tailscale; LAN is 192.168.1.0/24 (Pixel only) |
| Owner ruling | `ask_user_question` (2026-10-16) | — | **"Handoff to Mac machine, leaving necessary message, committing and pushing the branch"** — handoff path authorised |
| Oracle provenance | `sha256sum` of the Kotlin tests | 0 | Bip39Test.kt `18e56933e8b0f2d4db1aba88f811f3ad4ef20a6a05ba8a3b847143fa76fc4241`; Bip32Test.kt `9ec8ffab6aff2d67c65bad8e335a4e84c3d051807baedeb6df93fb9535861c09`; EcdsaTest.kt `6b0b5e9a3e9243e1301f6b53a5f2e4ceb95269b0a896df305f671d9c8212f1e3`; HandleStoreTest.kt `f453015eeeef084813c8e5a7aca48b451d8af6dde0aa50edd1d8636c085d9154`; VaultCoreModuleTest.kt `de5e7dbeda4f9c6bc172ec4f5d3cb7e06ed183f7484bdc03b6bf34a942a036ed` |
| Wordlist | `sha256sum` (Android res/raw vs iOS Resources) | 0 | **identical**: `2f5eed53a4727b4bf8880d8f3f199efc90e58503646d9ff8eff3a2ed3b24dbda` (13116 bytes, 2048 lines) |
| Autolinking (apple) | `npx expo-modules-autolinking search --platform apple --json` | 0 | `vault-core` resolves with `apple.modules = [VaultCoreModule]` |
| R4 evidence (re-verified) | CocoaPods trunk + GitHub API | 0 | pod `swift-secp256k1` **0.23.2** on trunk; podspec: MIT, `platforms.ios = 18.0`, `vendored_frameworks = P256K.xcframework`, source zip sha256 `0084b98982506a85dfbdf4568236c7c95cc77f10c2b8cdc0e567647844c00f77`; release published `2026-05-23T07:46:28Z`; repo pushed `2026-10-16`, 160 stars, MIT, not archived |
| P256K API inspection | docs.21.dev (`p256k` llms.txt + symbol pages) | 200 | `P256K.Context.rawRepresentation`; `Recovery.PrivateKey(dataRepresentation:format:)`; prehashed overload `signature(for: some Digest)`; `P256K.HashDigest([UInt8])`; `ECDSASignature.compactRepresentation` → `ECDSACompactSignature{signature, recoveryId}`; `Recovery.PublicKey(digest, signature:format:)`; `P256K.Format.{compressed,uncompressed}` — recorded in MAC-HANDOFF.md §4 |
| Oracle transcription | `node /tmp/gen_swift_vectors.mjs` (mechanical) | 0 | `{bip39: 8, official: [V1:6, V2:6, V3:2, V4:3] = 17 chains, pinned: 6, erc55: 4}`, second address + canonical signature + probe constants extracted; emitted `Tests/Vectors.swift` |

**Mac gate: FAILED (environment)** — recorded as Handoff **T1.5-H1**; the owner authorised the
handoff path instead of degrading the vector scope.

## Task 1 — R4 amendments + deployment target

Status: `[x]` (documentation/config only; no Apple tooling needed)

| Artefact | Observed result |
| --- | --- |
| `specs/tech-stack.md` | new row for `swift-secp256k1` (21-DOT-DEV) 0.23.2 — pin, pod route, MIT, 2026-05-23, active repo, zip sha256, iOS 18.0 floor, pre-1.0 maturity note, rename fact, install target = iOS podspec dependency |
| `doc/dependencies.md` §6c | full R4 record (purpose, install location, maintenance, CVE/supply-chain check with the pinned zip sha256, audit notes incl. the built-in-RFC6979 `[C]` caveat and the iOS 18.0 constraint) |
| `apps/mobile/app.json` | `ios.deploymentTarget: "18.0"` (CNG only — no `ios/` directory is hand-edited) |
| Prebuild interaction | R3 note: deployment-target change is a new-binary class; nothing is published yet |

## Task 2 — Swift cores

Status: `[~]` — authored, **not compiled** (T1.5-H1)

| File | Bytes | Content |
| --- | --- | --- |
| `ios/VaultErrors.swift` | 972 | frozen §2.4 codes + `VaultCodedException` (explicit `code` through the Expo bridge) |
| `ios/Zeroize.swift` | 514 | `wipe(_:)` via `memset_s` |
| `ios/Keccak.swift` | 3457 | self-written Keccak-f[1600] sponge (rate 1088, 0x01..0x80 padding) |
| `ios/Ripemd160.swift` | 4180 | self-written RIPEMD-160 (BIP-32 fingerprints + test serializer) |
| `ios/Bip39.swift` | 4337 | bundle wordlist, bit-accurate checksum, `CCKeyDerivationPBKDF` (2048, SHA512), wipes |
| `ios/Bip32.swift` | 10325 | path whitelist, CKD glue (`CCHmac`), scalar add mod n (33-byte carry + single subtract), HASH160 fingerprint, Keccak/EIP-55 |
| `ios/Ecdsa.swift` | 3230 | recoverable RFC6979 signing via `P256K` (prehashed `HashDigest`), 65-byte output, recovery helper |
| `ios/VaultStore.swift` | 1425 | serial queue, `put`/`copyOf` (independent storage)/`evict` (remove+wipe, idempotent)/`count` |

Zeroization inventory (all sites `wipe(&...)` in `defer` or immediately after use): `Bip39.swift`
password/salt/entropy/bits/digest/derived seed; `Bip32.swift` HMAC outputs, Il/Ir, serialization
buffer, pubkeys, fingerprints, xy, keccak digests, scalar sum; `Ecdsa.swift` derived private key +
compact copy; `VaultCoreModule.swift` input bytes, seed buffers, digest copy, private key, public
key; `VaultStore.swift` evicted seed.
**`[C]` caveats (never [P]):** Swift `Data`/`String` intermediates (CoW/heap, unwipable), the
PBKDF2 `Data` bridge inside CommonCrypto, and libsecp256k1's internal nonce buffer (built-in
RFC6979 per owner Q2).

## Task 3 — Module wiring + podspec

Status: `[~]` — authored, **not compiled** (T1.5-H1)

- `VaultCoreModule.swift`: four `AsyncFunction`s (`Data` in / handle+address+signature out), digest
  length checked **before** the store lookup, seed copies wiped, `destroy` = `store.evict`.
- `VaultCore.podspec`: `s.dependency 'swift-secp256k1', '0.23.2'`, `platforms ios/tvos 18.0`,
  `s.resources = 'Resources/**'` (wordlist), `s.exclude_files = 'Tests/**/*'`,
  `ENABLE_TESTABILITY = YES`, `test_spec 'Tests'`.
- `Resources/bip39_english.txt`: sha256 identical to the Android file (Task 0 table).

## Task 4 — XCTest suites

Status: `[~]` — authored, **not run** (T1.5-H1)

| Suite | Covers |
| --- | --- |
| `Bip39Tests` | V1–V8 validate + TREZOR seeds, empty-passphrase seed `5eb00bbd…9e38e4`, wordlist shape/sha256, negatives (unknown word, checksum flip, 11/13 words, empty wordlist) |
| `Bip32Tests` | 17 official chains xprv/xpub via the test-side 78-byte+Base58Check serializer, canonical `0x9858EfFD…` + second `0x6Fac4D18…` addresses, 11 path negatives + aliases |
| `KeccakTests` | keccak256("")/keccak256("abc"), four ERC-55 vectors |
| `EcdsaTests` | probe KAT, six TC-VC-027 pinned signatures (byte-equal, normalized coverage asserted), determinism + canonical signature, 24-digest recovery (TC-VC-028), digest-length negatives, §2.4 code table |
| `VaultStoreTests` | destroy wipe/idempotency, copy independence (MINOR-1), 1000-cycle loop |
| `Vectors.swift` | mechanically transcribed oracle data (generator + Kotlin sha256s in the file header) |

## Task 5 — Manual side (MAN-012 / MAN-013)

Status: `[ ]` — registered, awaiting the iPhone session

`specs/pending-manual-test-cases.md` now carries **MAN-012** (iOS memory walk, TC-VC-005) and
**MAN-013** (mlock one-line probe, C6), both marked 待走查 with the T1.5-H1 dependency.

## Task 6 — Verification, evidence, handoffs

Status: `[~]` — Linux-side checks green; compile/device evidence pending

| Check | Command | Exit | Observed result |
| --- | --- | --- | --- |
| TS frozen / npm drift | `git status --short -- package.json apps/mobile/package.json pnpm-lock.yaml` | 0 | empty — **zero TS and zero npm change** |
| TypeScript | `cd apps/mobile && npx --no-install tsc --noEmit` | 0 | clean |
| Swift log red line | `grep -rnE '(^|[^A-Za-z_])print\(|NSLog|os_log|debugPrint|dump\(' modules/vault-core/ios` | 1 | zero matches (an earlier looser pattern matched `fingerprint(` — false positives only; the word-boundary pattern is the recorded command) |
| R1 grep (TS side) | `grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile --include='*.ts' …` | 1 | zero matches (unchanged from T1.4) |
| Wordlist parity | `sha256sum` both copies | 0 | identical (Task 0) |

Evidence directory: none yet — xcresult/device artefacts belong to the Mac session (NIT-3 pattern).

### Handoff list (adjudicated by the next planning round)

| # | Item | Status 2026-10-16 | Disposal |
| --- | --- | --- | --- |
| T1.5-H1 | **Mac/iPhone gate not executed on this host** (no Apple toolchain; owner authorised handoff) | `[ ]` open | Run **MAC-HANDOFF.md** on a Mac: `xcodebuild test` (simulator auxiliary + pinned iPhone authoritative), then MAN-012/013; record results here |
| T1.5-H2 | JS→native bridge invocation (H8 class) | `[ ]` open | Replan → T4.1 dev-build screen smoke; the iOS device tests exercise the same code the AsyncFunction runs, honestly labelled |
| T1.5-H3 | At-rest key wrap / Keychain / Secure Enclave | `[ ]` open | Replan → security-ios feature |
| T1.5-H4 | mlock probe result + `[C]` labelling | `[ ]` open | MAN-013 on the iPhone; roadmap follow-up if the probe shows usable semantics |
| T1.5-H5 | Swift compile risks (P256K call sites, podspec resource bundle, test-spec scheme name) | `[ ]` open | MAC-HANDOFF.md §4 lists each with its fix direction; fix on the Mac **without weakening any vector or test** (B.6-5) |

## Task 7 — Board + git (owner-gated)

Status: `[ ]` — not executed. The owner authorised **commit + push of this branch**; merging into
`main`, the board row and the T1.5 closure call remain pending the owner's explicit instruction
after an independent review (Q6).

## Status board

| Task | Status |
| --- | --- |
| Planning round (owner decisions + Q/A) | `[x]` verified 2026-10-16 — D1–D6 + Q1–Q6 in requirements.md |
| Task 0 preflight + R4 evidence + API inspection | `[~]` Linux-side complete; Mac probe failed → T1.5-H1 |
| Task 1 R4 amendments + deployment target | `[x]` verified (docs/config) |
| Task 2 Swift cores | `[~]` authored, uncompiled |
| Task 3 module wiring + podspec | `[~]` authored, uncompiled |
| Task 4 XCTest suites | `[~]` authored, not run |
| Task 5 MAN-012/013 | `[ ]` registered, awaiting the iPhone session |
| Task 6 verification + handoffs | `[~]` Linux-side checks green; device evidence pending |
| Task 7 board + git | `[ ]` owner-gated |
| T1.5 overall | **`[~]` — implementation authored on Linux; Mac/iPhone verification pending (T1.5-H1)** |
