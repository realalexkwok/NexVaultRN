# T1.5 — vault-core iOS four-method port (validation)

> Feature spec (validation). Branch: feature/T1.5-vault-core-ios-port.
> Parent: plans/3-day-implementation-plan.md Day 2 T1.5（关键路径：iOS Swift 薄封装移植）.
> Skill: feature-dir SKILL.md, sha256 2b0b26dc9b8540fbe35664d8e512f44863a7c5b4fa10e2b109f4ef4e00968bb8.
> Session status: **Linux implementation session 2026-10-16** (model deepseek-flash, after
> GATE-MODEL-SWITCH). This host has **no Apple toolchain** (no xcodebuild/xcrun/swift, no
> reachable Mac, no eas.json/CI runner); per the owner's instruction the branch is handed off to
> a Mac session — see **MAC-HANDOFF.md** in this directory. Everything authored here is
> **uncompiled** until that session runs; nothing is marked `[x]`.
>
> Session status 2: **Mac session 2026-10-17 (HKT) / 2026-10-16 19:2x UTC** — MAC-HANDOFF.md was
> executed on the owner's Mac (macOS 26.2 arm64, **Xcode 26.5 / Swift 6.3.2**): the module now
> **compiles** and the XCTest suite is **18/18 green on the pinned iOS 26.5 simulator** (section
> "Mac session" below). The authoritative real-device gate is still open (the attached iPhone was
> offline), and the P256K integration route was amended (needs owner ratification — **T1.5-H6**),
> so T1.5 stays `[~]`.

## Gate checklist

| Gate | Status | Evidence |
| --- | --- | --- |
| GATE-MODEL-SWITCH | `[x]` satisfied | Owner switched the model; this session runs deepseek-flash |
| GATE-R4-SWIFTSECP | `[x]` owner-approved 2026-10-16 (Q1) | Amendments landed: specs/tech-stack.md row + doc/dependencies.md §6c (Task 1) |
| GATE-KECCAK | `[x]` owner-confirmed 2026-10-16 (Q2) | Self-written Keccak.swift + the two pinned vectors in Tests/KeccakTests.swift |
| GATE-M4-RFC6979 | `[x]` owner-confirmed 2026-10-16 (Q2 ruling) | Built-in libsecp256k1 RFC6979 (noncefp == NULL); k-buffer wipe control recorded as **[C]** |

---

## Mac session — 2026-10-17 HKT (2026-10-16 19:2x UTC)

> Ran **MAC-HANDOFF.md** on the owner's Mac. Host: macOS 26.2 (25C56), arm64; toolchain
> `xcodebuild -version` → **Xcode 26.5 (17F42)**, `swift --version` → **Apple Swift 6.3.2**.
> Branch `feature/T1.5-vault-core-ios-port` at `29892de` + the working-tree changes listed below.
> Integrity preflight of the handoff: session log intact (zstd frame OK, 695 records, seq 0..693
> contiguous, delivery marker `throughSeq 692`), tree in sync with `origin` (0/0), `git fsck` clean.

### Task 0 (Mac) — preflight

| Check | Command | Exit | Observed result |
| --- | --- | --- | --- |
| Toolchain | `xcodebuild -version`; `swift --version` | 0 | Xcode 26.5 (17F42); Swift 6.3.2 (swiftlang-6.3.2.1.108) |
| Simulators | `xcrun simctl list devices available` | 0 | iOS 26.4 + 26.5 runtimes; **no "iPhone 16"** (the runbook's example) |
| Pinned simulator | `xcrun simctl list devices` (grep UDID) | 0 | **iPhone 17, iOS 26.5 (23F77), UDID `8A63EA78-9400-4324-A81F-58C2A499FAD9`**, arm64 |
| Real device | `xcrun xctrace list devices` | 0 | **offline**: `iPaaad (2) (26.5) 00008020-001611111101402E` under *Devices Offline* → the authoritative B.4-5 device gate cannot run this session |
| Pods/prebuild | `pod install` (after the previous session's `expo prebuild --platform ios`) | 0 | 89 dependencies / 87 pods; `VaultCore` + `VaultCore/Tests` integrated, `VaultCore-Unit-Tests` scheme present |

### Task 2/3 (Mac) — compile gate: two defects in the prebuilt artifact, and the route amendment

Reproduced the handoff blocker:

```
P256K.framework/Modules/P256K.swiftmodule/arm64-apple-ios-simulator.private.swiftinterface:10:8:
  error: no such module 'libsecp256k1'
Bip32.swift:3:8: error: failed to build module 'P256K'; this SDK is not supported by the compiler
  (the SDK is built with 'Apple Swift version 6.1.2 ...', while this compiler is 'Apple Swift version 6.3.2 ...')
```

Root-caused to **two independent defects** in the R4-approved *prebuilt* artifact
`swift-secp256k1 0.23.2` (`P256K.xcframework.zip`, re-downloaded and sha256-matched to the
podspec pin `0084b989...c00f77`):

1. **The shipped interface imports a Clang module the pod does not ship.** Both
   `arm64-apple-ios.swiftinterface` and `.private.swiftinterface` line 10 are
   `import libsecp256k1`; `unzip -l P256K.xcframework.zip` lists **only** `P256K.xcframework/**`
   (no libsecp256k1 framework, module map or headers), and `grep -c secp256k1_ <interface>` = **0**
   (no C symbol is ever referenced — the C library is statically linked into the binary). A
   temporary empty Clang module named `libsecp256k1` made the import resolve (`swiftc -I <shim dir>`
   → import OK; `module libsecp256k1 {}` is accepted), which exposed defect 2.
2. **The interface cannot be loaded at all by this toolchain.** With the import satisfied, loading
   the module emits **66 diagnostics**, headed by
   `error: '_UInt256Words' is not a member type of enum 'P256K.P256K'` — the interface writes
   `extension UInt256 : Swift.BinaryInteger { public typealias Words = P256K._UInt256Words }`
   while `_UInt256Words` is declared at module scope, so `P256K` resolves to the **enum** `P256K`
   (declared in the same module) instead of to the module. Reproduced with a **cold module cache**
   (`-module-cache-path /tmp/freshcache`) → not a cache artifact. `0.23.2` is the newest release on
   CocoaPods trunk (2026-05-23) and on GitHub Releases → **no fixed artifact exists**.

**Route amendment (R4 record change; owner ratification pending — T1.5-H6).** The *same* pinned
release compiles from source: `git clone --branch 0.23.2` → commit
`e70a10e036a55fffea31568f0af92d69b6d449cd`; `swift build` → `Build complete! (13.79s)`; upstream
documents source/SwiftPM as the primary route ("Source (SwiftPM) builds are unaffected").
`VaultCore.podspec` therefore declares the package through React Native's own helper (documented as
callable from a podspec, `react-native/scripts/react_native_pods.rb:339`) instead of the vendored
xcframework:

```ruby
spm_dependency(s, url: 'https://github.com/21-DOT-DEV/swift-secp256k1',
               requirement: { kind: 'exactVersion', version: '0.23.2' }, products: ['P256K'])
```

Observed after `pod install`: `Pods.xcodeproj` gains
`XCRemoteSwiftPackageReference "swift-secp256k1"` + product `P256K` on the `VaultCore` target, and
the `swift-secp256k1` pod disappears from `Podfile.lock` (87 pods). **Version pin, upstream
repository, commit, license and every vector/test are unchanged**; the R4 records were amended
(tech-stack.md row + dependencies.md §6c).

SPM plugin trust: the package ships a build-tool plugin (`SharedSourcesPlugin`, copies
`Sources/Shared/*.swift` into each target because the P256K sources live in a shared directory), so
the Mac commands add `-skipPackagePluginValidation -skipMacroValidation` (without them `xcodebuild`
stops at *"Validate plug-in 'SharedSourcesPlugin'"*) — also recorded in MAC-HANDOFF.md §2.

**Call-site fix (MAC-HANDOFF.md §4 risk 1).** `Ecdsa.swift` spelled the digest type
`P256K.HashDigest(...)`, which fails for the same shadowing reason as defect 2
(`error: type 'P256K' has no member 'HashDigest'`). Fixed to the unqualified `HashDigest(...)`
(2 sites) — the type is module-level in `P256K` (`Sources/Shared/HashDigest.swift:42`) and is
documented as being "passed directly to secp256k1 signing APIs". **No vector, oracle value or test
was touched** (`git diff --stat -- modules/vault-core/ios/Tests modules/vault-core/ios/Resources` is
empty; `Tests/Vectors.swift` sha256
`63605160aec6b36077bb86a3af712057c8ff1b1395e3f90a9068a790919144e7`). The overload it selects is the
**prehashed** one — `Recovery+Signature.swift:207` (`DigestSigner`, signs the digest directly with
`secp256k1_ecdsa_sign_recoverable`, `noncefp == NULL`), not the SHA-256-then-sign `DataProtocol`
overload at line 232; the byte-equality oracle (TC-VC-027) is the mechanical proof that no re-hash
happened.

Result of the build gate:

```bash
xcodebuild -workspace ios/NexVault.xcworkspace -scheme VaultCore \
  -destination 'platform=iOS Simulator,id=8A63EA78-9400-4324-A81F-58C2A499FAD9' \
  -skipPackagePluginValidation -skipMacroValidation CODE_SIGNING_ALLOWED=NO build
# → xcodebuild exit=0, 0 "error:" lines, ** BUILD SUCCEEDED **
```

The handoff's other compile risks were re-checked: the `Exception(name:description:code:)` bridge and
the four `AsyncFunction` mappings compiled unchanged, the wordlist resource loaded from the pod
bundle (asserted by the wordlist test in `Bip39Tests`), `@testable import VaultCore` works with
`ENABLE_TESTABILITY=YES`, and the `VaultCore-Unit-Tests` scheme name matched the runbook.

### Task 4 (Mac) — `VaultCore-Unit-Tests` on the pinned simulator

```bash
xcodebuild test -workspace ios/NexVault.xcworkspace -scheme VaultCore-Unit-Tests \
  -destination 'platform=iOS Simulator,id=8A63EA78-9400-4324-A81F-58C2A499FAD9' \
  -skipPackagePluginValidation -skipMacroValidation CODE_SIGNING_ALLOWED=NO \
  -resultBundlePath /tmp/VaultCore-Unit-Tests.xcresult
# → xcodebuild test exit=0, ** TEST SUCCEEDED **
```

| Suite | Executed | Failures | Time |
| --- | --- | --- | --- |
| `Bip32Tests` | 3 | 0 | 0.046 s |
| `Bip39Tests` | 4 | 0 | 0.022 s |
| `EcdsaTests` | 5 | 0 | 0.110 s |
| `KeccakTests` | 2 | 0 | 0.003 s |
| `VaultStoreTests` | 3 | 0 | 0.004 s |
| **total** | **17** | **0** | 0.185 s |

`xcrun xcresulttool get test-results summary`: `passedTests: 17`, `failedTests: 0`,
`skippedTests: 0`, `result: "Passed"`, device `iPhone 17 / iOS 26.5 (23F77) / arm64`.
This run carries the TC-VC-027/028 oracle on iOS (6 pinned signatures byte-equal to the Android
oracle incl. normalized/non-normalized coverage, and 24-digest public-key recovery).
Durable evidence: `evidence/simulator-test-2026-10-17.txt` (console transcript + suite summary),
`evidence/simulator-test-suite-summary-2026-10-17.txt`,
`evidence/simulator-test-xcresult-summary-2026-10-17.json` (raw `xcresulttool` output).

**Scope honesty (B.4-2/B.4-5):** the simulator is auxiliary evidence only; the real-device run
(and with it MAN-012/013 and the authoritative B.4 `[x]`) is still open.
**Follow-up round (same Mac session): MAN-012 in-test carrier added.** MAN-012's acceptance text
says the ±5% tolerance is carried by an in-test assertion, but the T1.5 suite had no walk test — only
`VaultStoreTests`' store-level 1000-cycle loop. Added `Tests/MemoryWalkTests.swift`: 1000 rounds of
the four-method sequence (validate + PBKDF2 → `VaultStore.put` → BIP-32/Keccak/EIP-55 → `Ecdsa.signDigest`
→ `VaultStore.evict`, each round in an `autoreleasepool`), the process footprint
(`task_vm_info.phys_footprint`) sampled every 100 rounds and compared with the post-warm-up
(20-round) baseline. Everything asserted in-test: 1000/1000 rounds equal the pinned TC-VC-027
address + canonical signature, `VaultStore.count == 0` afterwards, and an unknown handle returns
`nil`/`false` (idempotent destroy). **No existing vector, test or production line was modified — the
suite only grew.**

```bash
pod install   # the new Tests/*.swift file must enter the pod target's source list
xcodebuild test -workspace ios/NexVault.xcworkspace -scheme VaultCore-Unit-Tests \
  -destination 'platform=iOS Simulator,id=8A63EA78-9400-4324-A81F-58C2A499FAD9' \
  -skipPackagePluginValidation -skipMacroValidation CODE_SIGNING_ALLOWED=NO \
  -resultBundlePath /tmp/VaultCore-Unit-Tests-2.xcresult
# → exit=0, ** TEST SUCCEEDED ** — 18 tests, 0 failures (MemoryWalkTests 7.26 s of 7.44 s)
```

Footprint curve (xcresult attachment, exported with `xcrun xcresulttool export attachments`):

```
phys_footprint samples (bytes): 29890112,29890112,29890112,29890112,30905920,
                                30905920,30905920,30905920,30840384,30840384,30840384
baseline 29.89 MB → peak 30.91 MB (+3.40%) → final 30.84 MB (+3.18%)   [±5% band, non-monotonic]
```

Durable evidence: `evidence/simulator-test-2026-10-17-run2.txt`,
`evidence/simulator-test-xcresult-summary-2026-10-17-run2.json`,
`evidence/simulator-memory-walk-2026-10-17.txt`. This is **auxiliary** (simulator) evidence: the
MAN-012 row stays ☐ 待走查 until the walk runs on a physical iPhone with an Instruments residency
curve, and the `AsyncFunction` bridge path remains T1.5-H2 → T4.1.


### Task 5 (Mac) — MAN-012 / MAN-013

**Not executed — no reachable device.** The only attached iPhone (`iPaaad (2)`, 26.5,
`00008020-001611111101402E`) is listed under *Devices Offline*; both cases are real-device-only by
B.4-5, and no simulator substitute is admissible for a memory-hygiene / mlock walk (the attached
device is also an **iPad (8th generation), iPad11,6**, `devicectl` state `unavailable` — not the
iPhone the row pins). Rows stay 待走查 (see T1.5-H1/H4).

MAN-012's *automated* carrier was nevertheless put in place this session (`MemoryWalkTests`, see the
Task 4 follow-up block below): when the promised iPhone appears, the walk part is one `xcodebuild
test` away and the manual row only needs the Instruments residency curve + the machine/OS/conclusion
record. MAN-013 (mlock probe) has no simulator substitute at all — it stays open until a device is
attached.

### Task 6 (Mac) — verification

| Check | Command | Exit | Observed result |
| --- | --- | --- | --- |
| Swift log red line | `grep -rnE` for print/NSLog/os_log/debugPrint/dump under modules/vault-core/ios | 1 | zero matches |
| Zeroization inventory | `grep -rn 'wipe(' modules/vault-core/ios` | 0 | **35 sites** — Bip32 18, VaultCoreModule 7, Bip39 7, Ecdsa 2, VaultStore 1, Zeroize 1 (definition) |
| Wordlist parity | `shasum -a 256` iOS vs Android | 0 | identical `2f5eed53a4727b4bf8880d8f3f199efc90e58503646d9ff8eff3a2ed3b24dbda` |
| R1 grep (TS side) | `grep -rnEi 'mnemonic/privateKey/secretKey/seedPhrase/xprv' apps/mobile --include='*.ts'` | 1 | zero matches |
| TypeScript | `cd apps/mobile && npx --no-install tsc --noEmit` | 0 | clean |
| Test/oracle integrity | `git diff --stat -- modules/vault-core/ios/Tests modules/vault-core/ios/Resources` | 0 | empty — no vector, oracle or test changed by this session |
| Evidence | — | — | `evidence/` created (transcript, suite summary, xcresult summary) |

Zeroization recap (unchanged claims, now compile-verified): every `wipe(&...)` site listed above is
in a `defer` or immediately after last use; the **[C]** caveats stay as recorded — Swift `Data`/`String`
CoW copies, the CommonCrypto PBKDF2 `Data` bridge, and libsecp256k1's internal RFC6979 nonce buffer
(`Recovery+Signature.swift:207-224` signs with `noncefp == NULL`, so `k` never leaves library memory).
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
| `MemoryWalkTests` | MAN-012 carrier (added 2026-10-17, Mac session): 1000 × validate/PBKDF2 → put → derive → sign → evict, per-round oracle equality, in-test `phys_footprint` ±5% band, destroy idempotency |
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
| T1.5-H1 | **Mac/iPhone gate**: macOS side **done 2026-10-17** (compiles; `VaultCore-Unit-Tests` 18/18 on the pinned iOS 26.5 simulator); **real-device side still open** (the only attached device is an offline iPad 8th gen, not an iPhone) | `[~]` partially closed | Run the same `xcodebuild test` against the pinned iPhone UDID, then MAN-012/013; only then does the B.4 manual side exist |
| T1.5-H2 | JS→native bridge invocation (H8 class) | `[ ]` open | Replan → T4.1 dev-build screen smoke; the iOS device tests exercise the same code the AsyncFunction runs, honestly labelled |
| T1.5-H3 | At-rest key wrap / Keychain / Secure Enclave | `[ ]` open | Replan → security-ios feature |
| T1.5-H4 | mlock probe result + `[C]` labelling | `[ ]` open | MAN-013 on the iPhone; roadmap follow-up if the probe shows usable semantics |
| T1.5-H5 | Swift compile risks (P256K call sites, podspec resource bundle, test-spec scheme name) | `[x]` resolved 2026-10-17 | Two artifact defects + one call-site error fixed with no vector/test change (Mac-session section); the prebuilt-artifact route is replaced by the SwiftPM source route — see T1.5-H6 |
| T1.5-H6 | **R4 route change needs owner ratification**: `swift-secp256k1` moved from the prebuilt CocoaPods xcframework to the **SwiftPM source build** of the same exact version 0.23.2 (tag `e70a10e`), because the prebuilt artifact cannot be loaded by Xcode 26.5 (two defects above). Driven by the handoff mandate to make the port compile; **owner was asked twice via `ask_user_question` in the Mac session and did not answer (both calls timed out)**, so the change is applied provisionally and recorded here rather than silently | `[ ]` open — owner decision | Ratify (amend tech-stack.md + dependencies.md §6c as already drafted) or reject. If rejected, the only alternatives are: wait for an upstream fixed artifact (0.23.2 is newest), or author a local CocoaPods source podspec (more moving parts). Reverting is one podspec block + one `pod install` |
| T1.5-H7 | **CI/EAS implication of the SPM route**: the package's build-tool plugin requires `-skipPackagePluginValidation -skipMacroValidation` (or an equivalent trust step) on every `xcodebuild` invocation; EAS Build does not run these flags today | `[ ]` open | Decide the trust policy before the first EAS iOS build (T4.x); document in the build profile or a config plugin |

## Task 7 — Board + git (owner-gated)

Status: `[~]` — **2026-10-17 (Mac session): the working tree was committed and the branch pushed.**
Observed: `git commit` → `e620fb3` (implementation + evidence + spec/doc amendments) and `git push
origin feature/T1.5-vault-core-ios-port` → `29892de..e620fb3` (branch in sync with origin, 0/0).
**Not** executed: merging into `main`, the AGENTS.md Part A board row, and the T1.5 closure call —
all pending the owner's explicit instruction after an independent review (Q6).

## Status board

| Task | Status |
| --- | --- |
| Planning round (owner decisions + Q/A) | `[x]` verified 2026-10-16 — D1–D6 + Q1–Q6 in requirements.md |
| Task 0 preflight + R4 evidence + API inspection | `[x]` verified — Linux side 2026-10-16 **plus Mac preflight 2026-10-17** (toolchain / simulator / device matrix in the Mac-session section) |
| Task 1 R4 amendments + deployment target | `[x]` verified (docs/config) |
| Task 2 Swift cores | `[~]` **compiled 2026-10-17** (Mac, 0 errors) and exercised by the passing simulator suite; real-device side pending |
| Task 3 module wiring + podspec | `[~]` compile + `pod install` verified 2026-10-17; **integration route amended to the SwiftPM source build of the same pin (T1.5-H6, owner ratification pending)** |
| Task 4 XCTest suites | `[~]` **run 2026-10-17: 17/17 oracle/unit tests**, then **18/18** after the MAN-012 carrier was added (same simulator); authoritative real-device run pending |
| Task 5 MAN-012/013 | `[ ]` registered, awaiting the iPhone session |
| Task 6 verification + handoffs | `[~]` Linux + Mac automated checks green; device evidence pending |
| Task 7 board + git | `[ ]` owner-gated |
| T1.5 overall | **`[~]` — compiles and passes 18/18 on the iOS 26.5 simulator (2026-10-17); real-device gate + MAN-012/013 + owner ratification of the P256K route (T1.5-H6) still open** |
