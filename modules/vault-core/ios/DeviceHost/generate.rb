#!/usr/bin/env ruby
# frozen_string_literal: true

# T1.5-r2 device harness: a standalone Xcode project (NO CocoaPods) with
#   VaultCoreCore        - framework target: the crypto core + Resources + the P256K SwiftPM product
#   DeviceHost           - tiny app (no Expo) that hosts the unit tests
#   VaultCoreCoreTests   - the existing Tests/*.swift, hosted by DeviceHost
#
# CocoaPods' pod test specs are logic-test bundles and Xcode refuses to run them on a physical
# device ("Logic Testing Unavailable") no matter how TEST_HOST / the scheme is configured. A normal
# app-hosted unit-test bundle is the supported shape, hence this project.
#
# Regenerate with: ruby modules/vault-core/ios/DeviceHost/generate.rb

require 'xcodeproj'
require 'fileutils'

HERE = __dir__
IOS = File.expand_path('..', HERE)
PROJECT_PATH = File.join(HERE, 'VaultCoreDeviceHost.xcodeproj')
DEPLOYMENT = '15.1'
CORE_SOURCES = %w[Bip32.swift Bip39.swift Ecdsa.swift Keccak.swift Ripemd160.swift
                  VaultCoreService.swift VaultErrors.swift VaultStore.swift Zeroize.swift].freeze

FileUtils.rm_rf(PROJECT_PATH)
project = Xcodeproj::Project.new(PROJECT_PATH)
project.root_object.attributes['LastUpgradeCheck'] = '2650'

def configure(target, extra = {})
  target.build_configurations.each do |config|
    config.build_settings['IPHONEOS_DEPLOYMENT_TARGET'] = DEPLOYMENT
    config.build_settings['SWIFT_VERSION'] = '5.0'
    config.build_settings['CODE_SIGN_STYLE'] = 'Automatic'
    # Never hard-code the signing team: it comes from the environment (DEVELOPMENT_TEAM=...) and is
    # intentionally absent from the repository. Leave it unset so Xcode prompts when opened.
    team = ENV['DEVELOPMENT_TEAM'].to_s
    config.build_settings['DEVELOPMENT_TEAM'] = team unless team.empty?
    config.build_settings['GENERATE_INFOPLIST_FILE'] = 'YES'
    config.build_settings['TARGETED_DEVICE_FAMILY'] = '1,2'
    extra.each { |k, v| config.build_settings[k] = v }
  end
end

ios_group = project.main_group.new_group('vault-core-ios', IOS)

# --- framework: the crypto core ---------------------------------------------------------------
framework = project.new_target(:framework, 'VaultCoreCore', :ios, DEPLOYMENT)
configure(framework, 'ENABLE_TESTABILITY' => 'YES', 'DEFINES_MODULE' => 'YES',
                     'PRODUCT_BUNDLE_IDENTIFIER' => 'io.github.realalexkwok.nexvault.core')
CORE_SOURCES.each { |name| framework.source_build_phase.add_file_reference(ios_group.new_file(name)) }
resources = ios_group.new_group('Resources', 'Resources')
wordlist = resources.new_file('bip39_english.txt')
framework.resources_build_phase.add_file_reference(wordlist)

package = project.new(Xcodeproj::Project::Object::XCRemoteSwiftPackageReference)
package.repositoryURL = 'https://github.com/21-DOT-DEV/swift-secp256k1'
package.requirement = { 'kind' => 'exactVersion', 'version' => '0.23.2' }
project.root_object.package_references << package
product = project.new(Xcodeproj::Project::Object::XCSwiftPackageProductDependency)
product.package = package
product.product_name = 'P256K'
framework.package_product_dependencies << product

# --- host app ---------------------------------------------------------------------------------
app = project.new_target(:application, 'DeviceHost', :ios, DEPLOYMENT)
configure(app, 'PRODUCT_BUNDLE_IDENTIFIER' => 'io.github.realalexkwok.nexvault.devicehost',
               'INFOPLIST_KEY_UILaunchScreen_Generation' => 'YES')
app.source_build_phase.add_file_reference(ios_group.new_group('DeviceHost', 'DeviceHost').new_file('AppDelegate.swift'))

app.frameworks_build_phase.add_file_reference(framework.product_reference)
# MAJOR-3a: a dynamic framework must travel with the app, otherwise dyld cannot find it on a device.
embed = app.new_copy_files_build_phase('Embed Frameworks')
embed.dst_subfolder_spec = '10'
embedded = embed.add_file_reference(framework.product_reference)
embedded.settings = { 'ATTRIBUTES' => ['CodeSignOnCopy', 'RemoveHeadersOnCopy'] }
# --- unit-test bundle hosted by the app -------------------------------------------------------
tests = project.new_target(:unit_test_bundle, 'VaultCoreCoreTests', :ios, DEPLOYMENT)
configure(tests, 'PRODUCT_BUNDLE_IDENTIFIER' => 'io.github.realalexkwok.nexvault.coretests')
tests.add_dependency(app)
tests.add_dependency(framework)
tests.frameworks_build_phase.add_file_reference(framework.product_reference)
tests.build_configurations.each do |config|
  config.build_settings['TEST_HOST'] = '$(BUILT_PRODUCTS_DIR)/DeviceHost.app/DeviceHost'
  config.build_settings['BUNDLE_LOADER'] = '$(TEST_HOST)'
end
test_group = ios_group.new_group('Tests', 'Tests')
%w[Bip32Tests.swift Bip32WipeTests.swift Bip39EntropyTests.swift Bip39Tests.swift EcdsaTests.swift KeccakTests.swift MlockProbeTests.swift
   MemoryWalkTests.swift ReviewRegressionTests.swift TestSupport.swift VaultCoreServiceTests.swift VaultStoreTests.swift Vectors.swift].each do |name|
  tests.source_build_phase.add_file_reference(test_group.new_file(name))
end
tests.resources_build_phase.add_file_reference(wordlist)

scheme = Xcodeproj::XCScheme.new
scheme.add_build_target(app)
scheme.add_build_target(tests)
scheme.set_launch_target(app)
scheme.add_test_target(tests)
scheme.save_as(PROJECT_PATH, 'VaultCoreCoreTests', true)

# Preserve the SPM pin: Xcode writes Package.resolved into the project, but regenerate deletes the
# whole project directory. Keep a committed copy and restore it after generation.
resolved = File.join(HERE, 'Package.resolved')
if File.exist?(resolved)
  swiftpm = File.join(PROJECT_PATH, 'project.xcworkspace', 'xcshareddata', 'swiftpm')
  FileUtils.mkdir_p(swiftpm)
  FileUtils.cp(resolved, File.join(swiftpm, 'Package.resolved'))
end

# Xcodeproj seeds a Foundation.framework reference under a versioned SDK path
# (e.g. iPhoneOS18.0.sdk) that does not exist on this Xcode; re-point it at the SDK-relative form.
project.files.each do |file|
  next unless file.path.to_s.include?('Developer/SDKs/')
  file.path = file.path.sub(%r{Platforms/iPhoneOS\.platform/Developer/SDKs/iPhoneOS[0-9.]+\.sdk/}, '')
  file.source_tree = 'SDKROOT'
end

project.build_configurations.each { |c| c.build_settings['SDKROOT'] = 'iphoneos' }
project.save
puts "generated #{PROJECT_PATH}"
puts project.targets.map { |t| "  target: #{t.name} (#{t.product_type})" }
