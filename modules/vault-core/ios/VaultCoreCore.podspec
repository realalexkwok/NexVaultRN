Pod::Spec.new do |s|
  s.name           = 'VaultCoreCore'
  s.version        = '1.0.0'
  s.summary        = 'NexVault vault-core crypto core (iOS): BIP-39/32/44, RFC6979 ECDSA, opaque handles.'
  s.description    = 'Expo-free crypto core (T1.5-r2): targets iOS 15.0 so its XCTest bundle can run on an iOS 15.8 device. The Expo bridge lives in the separate VaultCore pod (iOS 16.4, ExpoModulesCore).'
  s.author         = 'NexVault'
  s.homepage       = 'https://github.com/realalexkwok/NexVaultRN'
  # iOS 15.1, not 15.0: React Native's Podfile declares `platform :ios, 15.1`, and CocoaPods
  # raises every pod target to that floor. 15.1 is still below the iPad mini 4's iOS 15.8.8.
  s.platforms      = {
    :ios => '15.1'
  }
  s.source         = { :git => 'https://github.com/realalexkwok/NexVaultRN.git', :tag => s.version.to_s }
  s.static_framework = true

  # T1.5 R4 amendment (owner 2026-10-16, route amended 2026-10-17): secp256k1 curve math + RFC6979
  # recoverable ECDSA from the approved pin 0.23.2, built **from source** through React Native's
  # spm_dependency helper. The prebuilt xcframework cannot be loaded by Xcode 26.5 (missing
  # libsecp256k1 module + a UID256/module-name name-lookup defect in its shipped interface), and the
  # *source* manifest declares no platform floor, so it compiles at this pod's iOS 15.0 floor —
  # verified: xcodebuild -scheme P256K -destination 'generic/platform=iOS' IPHONEOS_DEPLOYMENT_TARGET=15.0
  # -> BUILD SUCCEEDED. Evidence: validation.md (Mac session + T1.5-r2).
  unless respond_to?(:spm_dependency, true)
    raise 'VaultCoreCore.podspec: react-native/scripts/react_native_pods.rb must be required by ' \
          'the Podfile — it provides spm_dependency() for the P256K SwiftPM package.'
  end
  spm_dependency(
    s,
    url: 'https://github.com/21-DOT-DEV/swift-secp256k1',
    requirement: { kind: 'exactVersion', version: '0.23.2' },
    products: ['P256K']
  )

  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    # The XCTest suites use @testable import VaultCoreCore.
    'ENABLE_TESTABILITY' => 'YES',
    # React Native's SPM helper builds the product into this directory; the pod target and its test
    # target must both find P256K.swiftmodule there.
    'SWIFT_INCLUDE_PATHS' => '$(inherited) ${SYMROOT}/${CONFIGURATION}${EFFECTIVE_PLATFORM_NAME}/',
  }

  s.source_files = [
    'Bip32.swift', 'Bip39.swift', 'Ecdsa.swift', 'Keccak.swift', 'Ripemd160.swift',
    'VaultCoreService.swift', 'VaultErrors.swift', 'VaultStore.swift', 'Zeroize.swift',
  ]
  s.resources = 'Resources/**'

  s.test_spec 'Tests' do |t|
    t.source_files = 'Tests/**/*.swift'
    t.resources = 'Resources/**'
    # Xcode refuses *hostless* (logic) test bundles on physical iOS devices. CocoaPods scaffolds a
    # tiny host app when this is set, which is what lets the T1.5-r2 device gate run on an iOS 15.8
    # iPad (the real app itself cannot go below Expo's 16.4 floor).
    t.requires_app_host = true
    t.pod_target_xcconfig = {
      'SWIFT_INCLUDE_PATHS' => '$(inherited) ${SYMROOT}/${CONFIGURATION}${EFFECTIVE_PLATFORM_NAME}/',
    }
  end
end
