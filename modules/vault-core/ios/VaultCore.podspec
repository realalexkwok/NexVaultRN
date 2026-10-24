Pod::Spec.new do |s|
  s.name           = 'VaultCore'
  s.version        = '1.0.0'
  s.summary        = 'NexVault vault-core local module (iOS): Expo bridge for the four frozen methods.'
  s.description    = 'Expo Modules bridge that exposes VaultCoreCore (importMnemonic, deriveAddress, signDigest, destroy) to JS with the frozen E_VAULT_* error codes.'
  s.author         = 'NexVault'
  s.homepage       = 'https://github.com/realalexkwok/NexVaultRN'
  s.platforms      = {
    :ios => '16.4'
  }
  s.source         = { :git => 'https://github.com/realalexkwok/NexVaultRN.git', :tag => s.version.to_s }
  s.static_framework = true

  # T1.5-r2 split (2026-10-17): the crypto core lives in its own pod with an iOS 15.1 floor so the
  # XCTest bundle can run on an iOS 15.8 device. This bridge must keep ExpoModulesCore's iOS 16.4
  # floor, and the app target stays at its own deployment target (currently iOS 18.0). The pod name
  # is unchanged, so Expo autolinking and the frozen four-method surface are untouched.
  s.dependency 'ExpoModulesCore'
  s.dependency 'VaultCoreCore'
  # T3.2 (2026-10-24): fail-closed gate coupling. This bridge imports SecurityIos and injects
  # SecurityGate.assertSigningAllowed() into VaultCoreCore. Both pods sit at the iOS 16.4
  # ExpoModulesCore floor, so the VaultCoreCore / VaultCore platform split is unaffected.
  s.dependency 'SecurityIos'

  # T1.5-r2: the P256K product is deliberately attached to VaultCoreCore ONLY. Declaring it here as
  # well makes both static pods embed the package's objects and the app link fails with duplicate
  # P256K symbols. Instead the core hides the import from its consumers with
  # `@_implementationOnly` (Ecdsa.swift / Bip32.swift), so this bridge compiles without P256K.

  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    # This target compiles Swift that imports VaultCoreCore -> P256K, so the SwiftPM product's
    # module directory must be on its search path as well.
    'SWIFT_INCLUDE_PATHS' => '$(inherited) ${SYMROOT}/${CONFIGURATION}${EFFECTIVE_PLATFORM_NAME}/',
  }

  # T1.5-r2: the app target compiles Expo's generated ExpoModulesProvider.swift, which imports this
  # bridge pod -> which imports VaultCoreCore -> which imports P256K. The SwiftPM product lives in
  # SYMROOT, so the consumer target needs that search path too, otherwise the app build fails with
  # "missing required module 'libsecp256k1'" (the C module of the same package).
  s.user_target_xcconfig = {
    'SWIFT_INCLUDE_PATHS' => '$(inherited) ${SYMROOT}/${CONFIGURATION}${EFFECTIVE_PLATFORM_NAME}/',
  }

  s.source_files = 'VaultCoreModule.swift'
end
