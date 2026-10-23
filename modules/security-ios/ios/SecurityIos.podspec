Pod::Spec.new do |s|
  s.name           = 'SecurityIos'
  s.version        = '1.0.0'
  s.summary        = 'NexVault T3.1 iOS KEK/wrap submodule (Keychain biometric ACL + CryptoKit AES-GCM).'
  s.description    = 'Keychain KEK storage behind biometric access control and the wrap-blob codec shared with Android. wrap/unwrap stay native-internal in T3.1; T3.2 wires them to vault-core.'
  s.author         = 'NexVault'
  s.homepage       = 'https://github.com/realalexkwok/NexVaultRN'
  s.platforms      = {
    :ios => '16.4'
  }
  s.source         = { :git => 'https://github.com/realalexkwok/NexVaultRN.git', :tag => s.version.to_s }
  s.static_framework = true

  s.dependency 'ExpoModulesCore'

  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    # The XCTest suites use @testable import SecurityIos.
    'ENABLE_TESTABILITY' => 'YES'
  }

  s.source_files = '*.swift'
  s.swift_version = '5.9'

  s.test_spec 'Tests' do |test_spec|
    test_spec.source_files = 'Tests/**/*.swift'
  end
end
