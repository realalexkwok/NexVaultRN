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

  # T1.5-r2 split (2026-10-17): the crypto core lives in its own pod with an iOS 15.0 floor so the
  # XCTest bundle can run on an iOS 15.8 device. This bridge must keep ExpoModulesCore's iOS 16.4
  # floor, and the app target stays at its own deployment target (currently iOS 18.0). The pod name
  # is unchanged, so Expo autolinking and the frozen four-method surface are untouched.
  s.dependency 'ExpoModulesCore'
  s.dependency 'VaultCoreCore'

  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
  }

  s.source_files = 'VaultCoreModule.swift'
end
