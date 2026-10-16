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
  s.dependency 'swift-secp256k1', '0.23.2'

  # Swift/Objective-C compatibility
  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES',
    # The XCTest suites use @testable import VaultCore.
    'ENABLE_TESTABILITY' => 'YES',
  }

  s.source_files = '**/*.{h,m,mm,swift,hpp,cpp}'
  s.exclude_files = 'Tests/**/*'
  s.resources = 'Resources/**'

  s.test_spec 'Tests' do |t|
    t.source_files = 'Tests/**/*.swift'
    t.resources = 'Resources/**'
  end
end
