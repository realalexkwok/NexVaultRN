# T1.5 — Mac handoff runbook (Linux session -> Mac session)

> **Why this file exists.** The T1.5 implementation was written on a Linux host with no Xcode
> (`xcodebuild`/`xcrun`/`swift` absent, no reachable Mac, no `eas.json`). The owner instructed:
> "Handoff to Mac machine, leaving necessary message, committing and pushing the branch." So the
> Swift sources, podspec, wiring and XCTest suites below are **written but never compiled**.
> Everything that requires Apple tooling stays open until this runbook is executed on a Mac.

## 1. State of the branch

| Item | Value |
| --- | --- |
| Branch | `feature/T1.5-vault-core-ios-port` (pushed to origin) |
| Base | `main` `f9e9630` |
| Spec trio | `specs/features/2026-10-16-T1.5-vault-core-ios-port/{requirements,plan,validation}.md` |
| Status | T1.5 `[~]` — implementation + wiring + tests authored; **compile/simulator/device gates pending** (Handoff T1.5-H1) |

New/changed files:

```
modules/vault-core/ios/VaultErrors.swift          (new) fixed §2.4 codes + VaultCodedException
modules/vault-core/ios/Zeroize.swift              (new) memset_s wipe helper
modules/vault-core/ios/Keccak.swift               (new) self-written Keccak-f[1600] (GATE-KECCAK)
modules/vault-core/ios/Ripemd160.swift            (new) self-written RIPEMD-160 (BIP-32 fingerprints)
modules/vault-core/ios/Bip39.swift                (new) wordlist + CommonCrypto PBKDF2-HMAC-SHA512
modules/vault-core/ios/Bip32.swift                (new) CKD glue + scalar add mod n + EIP-55
modules/vault-core/ios/Ecdsa.swift                (new) RFC6979 recoverable ECDSA via swift-secp256k1
modules/vault-core/ios/VaultStore.swift           (new) serialized handle store (T1.4 MINOR-1 contract)
modules/vault-core/ios/VaultCoreModule.swift      (rewritten) four AsyncFunctions
modules/vault-core/ios/VaultCore.podspec          (rewritten) swift-secp256k1 0.23.2 + iOS 18 + test spec
modules/vault-core/ios/Resources/bip39_english.txt(new) sha256 2f5eed53...b24dbda (Android-identical)
modules/vault-core/ios/Tests/Vectors.swift        (new, generated) oracle vectors
modules/vault-core/ios/Tests/TestSupport.swift    (new) hex/Base58Check/78-byte serializer
modules/vault-core/ios/Tests/{Bip39Tests,Bip32Tests,KeccakTests,EcdsaTests,VaultStoreTests}.swift (new)
apps/mobile/app.json                              ios.deploymentTarget = 18.0
specs/tech-stack.md, doc/dependencies.md §6c      T1.5 R4 amendment (approved)
```

## 2. Commands to run on the Mac (in order)

```bash
git fetch origin && git switch feature/T1.5-vault-core-ios-port && git pull
cd apps/mobile
npx expo prebuild --platform ios --no-install    # CNG regenerates ios/ from app.json
npx pod-install                                  # resolves ExpoModulesCore + swift-secp256k1 0.23.2
# verify the pod resolved and its platform matches:
grep -n "swift-secp256k1" ios/Podfile.lock

# simulator run (auxiliary evidence only):
xcodebuild test -workspace ios/NexVault.xcworkspace -scheme VaultCore-Unit-Tests \
  -destination 'platform=iOS Simulator,name=iPhone 16' CODE_SIGNING_ALLOWED=NO

# real-device run (authoritative, B.4-5) — pin ONE device id:
xcrun xctrace list devices            # copy the iPhone UDID
xcodebuild test -workspace ios/NexVault.xcworkspace -scheme VaultCore-Unit-Tests \
  -destination 'platform=iOS,id=<UDID>'
```

If the scheme name differs, list it with `xcodebuild -workspace ios/NexVault.xcworkspace -list`.

## 3. What to record in `validation.md`

- Task 0: `xcodebuild -version`, `xcrun simctl list devices available`, the pinned iPhone UDID.
- Task 4: full `xcodebuild test` output, test counts per suite, `xcresult` path, and byte-equality
  results for the six TC-VC-027 pinned signatures (they are the cross-platform oracle).
- Task 5: **MAN-012** iOS memory walk (1000x import/derive/sign/destroy + PSS-style curve) and
  **MAN-013** mlock one-line probe (`mlock`/`munlock` a page, record errno) — register both rows in
  `specs/pending-manual-test-cases.md` with 机型 + OS + 结论.
- Task 6: `grep -rnE 'print(|NSLog|os_log' modules/vault-core/ios` (expect exit 1), zero TS diff,
  `npx tsc --noEmit`, and the zeroization review (every `wipe(&...)` site + the `[C]` caveats:
  Swift `Data`/`String` copies, libsecp256k1's internal nonce buffer).

## 4. Known compile-risk list (things only a compiler can confirm)

1. `P256K` API surface: `Recovery.PrivateKey(dataRepresentation:format:)`,
   `signature(for: P256K.HashDigest)` (prehashed overload) and
   `Recovery.PublicKey(_:signature:format:)` are taken from the published docs
   (docs.21.dev, P256K module of swift-secp256k1 0.23.2). If a throwing/non-throwing mismatch or a
   renamed label shows up, fix the call site — **never** change a vector or weaken a test.
2. `ECDSASignature(compactRepresentation:recoveryId:)` is wrapped in `try/catch`; if it is
   non-throwing Swift will emit a warning (not an error).
3. `Exception(name:description:code:)` comes from expo-modules-core 57.0.22
   (`ios/Core/Exceptions/Exception.swift:39`); `VaultCodedException` subclasses it so JS sees the
   frozen `E_VAULT_*` codes.
4. `AsyncFunction` mapping: `Data` in, `String`/`Data` out (the frozen TS wrappers expect
   `Uint8Array`/`ArrayBuffer`).
5. Wordlist loading uses `Bundle(for: VaultCoreModule.self)` with `s.resources = 'Resources/**'`;
   if the resource lands in a nested bundle instead, switch to `s.resource_bundles` and load that
   bundle (record the change).
6. `@testable import VaultCore` requires `ENABLE_TESTABILITY` (already set in the podspec).
7. iOS deployment target must be >= 18.0 (`app.json` set accordingly) because the pod's platform
   floor is iOS 18.0.

## 5. Do not do on the Mac

- Do not merge to `main`, write the board row, or push `main` — T1.5 is **not** closed; the owner
  closes it explicitly after an independent review (owner Q6).
- Do not edit `Vectors.swift` by hand; regenerate it with the transcription script so the oracle
  stays mechanical (`Vectors.swift` records the Kotlin file sha256s).
## 6. Mac session addendum — 2026-10-17 (executed)

The runbook above was executed on the owner's Mac (macOS 26.2 arm64, Xcode 26.5 / Swift 6.3.2).
Three deviations from the text above, all recorded in `validation.md` §"Mac session":

1. **P256K integration route changed** — from the prebuilt CocoaPods xcframework to the SwiftPM
   source build of the same exact pin 0.23.2: the prebuilt artifact cannot be loaded by this
   toolchain (its `.swiftinterface` imports a Clang module the pod does not ship, and its
   `UInt256.Words` conformance is written through the module name that the enum `P256K` shadows).
   **Owner ratification pending (T1.5-H6).**
2. **`-skipPackagePluginValidation -skipMacroValidation` are required** on every `xcodebuild`
   command: the package ships a build-tool plugin (`SharedSourcesPlugin`) that Xcode must be told to
   trust, otherwise the build stops at "Validate plug-in 'SharedSourcesPlugin'".
3. **Simulator**: no "iPhone 16" runtime exists here; the pinned auxiliary destination is
   **iPhone 17, iOS 26.5 (23F77), UDID `8A63EA78-9400-4324-A81F-58C2A499FAD9`**.

Commands as actually run (cwd `apps/mobile`):

```bash
pod install

xcodebuild -workspace ios/NexVault.xcworkspace -scheme VaultCore \
  -destination 'platform=iOS Simulator,id=8A63EA78-9400-4324-A81F-58C2A499FAD9' \
  -skipPackagePluginValidation -skipMacroValidation CODE_SIGNING_ALLOWED=NO build
# ** BUILD SUCCEEDED ** (0 errors)

xcodebuild test -workspace ios/NexVault.xcworkspace -scheme VaultCore-Unit-Tests \
  -destination 'platform=iOS Simulator,id=8A63EA78-9400-4324-A81F-58C2A499FAD9' \
  -skipPackagePluginValidation -skipMacroValidation CODE_SIGNING_ALLOWED=NO \
  -resultBundlePath /tmp/VaultCore-Unit-Tests.xcresult
# ** TEST SUCCEEDED ** — 17/17, 0 failures (Bip32 3, Bip39 4, Ecdsa 5, Keccak 2, VaultStore 3)
```

Still open from §2: the **real-device** run (`platform=iOS,id=<UDID>`) — the only attached iPhone
(`iPaaad (2)`, 26.5) was offline, so MAN-012/013 stay open too. The `[C]` caveats and the "do not do"
list in §5 remain in force.
