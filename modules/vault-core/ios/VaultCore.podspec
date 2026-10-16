Pod::Spec.new do |s|
  s.name           = 'VaultCore'
  s.version        = '1.0.0'
  s.summary        = 'NexVault vault-core local module (iOS): BIP-39/32/44, RFC6979 ECDSA, opaque handles.'
  s.description    = 'Imports a BIP-39 mnemonic into a native handle store, derives EIP-55 EVM addresses, signs 32-byte digests with RFC6979 recoverable ECDSA, and destroys handles with full zeroization.'
  s.author         = 'NexVault'
  s.homepage       = 'https://github.com/realalexkwok/NexVaultRN'
  s.platforms      = {
    :ios => '18.0',
    :tvos => '18.0'
  }
  s.source         = { :git => 'https://github.com/realalexkwok/NexVaultRN.git', :tag => s.version.to_s }
  s.static_framework = true

  s.dependency 'ExpoModulesCore'

  # T1.5 R4 amendment (owner 2026-10-16): secp256k1 curve math + RFC6979 recoverable ECDSA.
  # Evidence and the pre-1.0 / iOS-18 caveats live in doc/dependencies.md section 6c.
  #
  # T1.5 Mac session, 2026-10-17 — the R4-approved *prebuilt* CocoaPods artifact
  # (swift-secp256k1 0.23.2, P256K.xcframework, zip sha256 0084b989...) is not consumable on
  # this toolchain (Xcode 26.5 / Swift 6.3.2). Its shipped .swiftinterface
  #   (a) starts with `import libsecp256k1` while the pod vendors no such Clang module or
  #       header (upstream zip contains only P256K.xcframework), and
  #   (b) names UInt256's `Words` as `P256K._UInt256Words`, which the compiler resolves to
  #       the enum `P256K` rather than the module of the same name;
  # so `import P256K` cannot load the module at all (66 diagnostics, reproduced with a cold
  # module cache). 0.23.2 is the newest trunk/GitHub release, so no fixed artifact exists.
  # The *same* pinned release compiles from source (git tag 0.23.2 = commit e70a10e;
  # `swift build` -> "Build complete!"), which is upstream's documented primary route
  # ("Source (SwiftPM) builds are unaffected"). The dependency is therefore declared as an
  # exact-version SwiftPM package through React Native's helper; the vendored xcframework is
  # gone. Version pin, upstream source, license and vectors are unchanged.
  # Evidence: specs/features/2026-10-16-T1.5-vault-core-ios-port/validation.md (Task 3, Mac session).
  unless respond_to?(:spm_dependency, true)
    raise 'VaultCore.podspec: react-native/scripts/react_native_pods.rb must be required by ' \
          'the Podfile — it provides spm_dependency() for the P256K SwiftPM package ' \
          '(see validation.md Task 3, Mac session).'
  end
  spm_dependency(
    s,
    url: 'https://github.com/21-DOT-DEV/swift-secp256k1',
    requirement: { kind: 'exactVersion', version: '0.23.2' },
    products: ['P256K']
  )

  # Swift/Objective-C compatibility
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    # The XCTest suites use @testable import VaultCore.
    'ENABLE_TESTABILITY' => 'YES',
    # React Native's SPM helper builds the package product into this directory and adds it
    # to the pod target itself; declaring it here as well keeps the pod and its test spec
    # resolved from the same place (the test target transitively loads P256K's module).
    'SWIFT_INCLUDE_PATHS' => '$(inherited) ${SYMROOT}/${CONFIGURATION}${EFFECTIVE_PLATFORM_NAME}/',
  }

  s.source_files = '**/*.{h,m,mm,swift,hpp,cpp}'
  s.exclude_files = 'Tests/**/*'
  s.resources = 'Resources/**'

  s.test_spec 'Tests' do |t|
    t.source_files = 'Tests/**/*.swift'
    t.resources = 'Resources/**'
    t.pod_target_xcconfig = {
      'SWIFT_INCLUDE_PATHS' => '$(inherited) ${SYMROOT}/${CONFIGURATION}${EFFECTIVE_PLATFORM_NAME}/',
    }
  end
end
