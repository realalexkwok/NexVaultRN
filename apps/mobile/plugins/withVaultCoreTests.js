const path = require('path');
// Import from `expo/config-plugins`, not `@expo/config-plugins`: under pnpm's isolated layout this
// app has no node_modules edge to the latter, so the Xcode script phase that generates the prebuilt
// Constants manifest (expo-constants -> @expo/config library path, i.e. without the Expo CLI's own
// resolution hooks) aborted with INVALID_PLUGIN_IMPORT / "Cannot find module
// '@expo/config-plugins'". `expo` is a declared dependency of this app and re-exports the identical
// API. Evidence: specs/features/2026-10-16-T1.5-vault-core-ios-port/validation.md (plugin-import fix).
const { withPodfile } = require('expo/config-plugins');

/**
 * T1.5 (vault-core iOS port) — integrate the `modules/vault-core` iOS pod's test spec.
 *
 * CocoaPods only creates a pod's test-spec target when the Podfile asks for it, and Expo
 * autolinking does not pass `:testspecs` for local modules. Without this plugin the generated
 * Pods project contains `VaultCore` but no `VaultCore-Unit-Tests` target/scheme, so the T1.5
 * validation runbook's `xcodebuild test -scheme VaultCore-Unit-Tests` cannot run.
 *
 * The declaration is appended **after** `use_expo_modules!` on purpose:
 *  - `use_expo_modules!(includeTests: true)` is too broad — it also activates `Expo/Tests`, which
 *    depends on the unpublished `ExpoModulesTestCore` pod and aborts `pod install`.
 *  - declaring the same pod a second time is accepted by CocoaPods (it merges the attributes),
 *    so autolinking keeps registering VaultCore exactly as before (same `:path`, same modular
 *    headers for its transitive dependencies) and only the test spec is added.
 *
 * CNG: the generated `ios/` directory is never hand-edited — this plugin is the source of truth
 * and `expo prebuild` re-applies it. Idempotent: it is a no-op when the declaration is present.
 */
const VAULT_CORE_PODSPEC_DIR = 'modules/vault-core/ios';
const DECLARATION_COMMENT = [
  '# T1.5: integrate the modules/vault-core iOS test spec so the VaultCore-Unit-Tests',
  '# scheme exists for `xcodebuild test`. Injected by apps/mobile/plugins/withVaultCoreTests.js.',
];

/**
 * Pure transform (exported for the T1.5 validation evidence: it is applied to the pristine
 * `expo prebuild` Podfile template and diffed against the generated Podfile).
 *
 * @param {string} podfileContents generated Podfile
 * @param {{podfileDir: string, projectRoot: string}} paths Podfile directory + app root
 * @returns {string} Podfile with the vault-core test-spec declaration
 */
function addVaultCoreTestSpec(podfileContents, { podfileDir, projectRoot }) {
  if (/^[ \t]*pod[ \t]+'VaultCore'/m.test(podfileContents)) {
    return podfileContents;
  }
  const anchor = /^([ \t]*)use_expo_modules!\s*$/m;
  if (!anchor.test(podfileContents)) {
    throw new Error(
      'withVaultCoreTests: no `use_expo_modules!` line found in the generated Podfile; the ' +
        'VaultCore test spec (VaultCore-Unit-Tests) cannot be integrated.'
    );
  }
  const relativePath = path.relative(podfileDir, path.join(projectRoot, VAULT_CORE_PODSPEC_DIR));
  return podfileContents.replace(anchor, (line, indent) => {
    const injected = [
      ...DECLARATION_COMMENT.map((text) => indent + text),
      `${indent}pod 'VaultCore', :path => '${relativePath}', :testspecs => ['Tests']`,
    ].join('\n');
    return `${line}\n${injected}`;
  });
}

const withVaultCoreTests = (config) =>
  withPodfile(config, (cfg) => {
    cfg.modResults.contents = addVaultCoreTestSpec(cfg.modResults.contents, {
      podfileDir: cfg.modRequest.platformProjectRoot,
      projectRoot: cfg.modRequest.projectRoot,
    });
    return cfg;
  });

module.exports = withVaultCoreTests;
module.exports.addVaultCoreTestSpec = addVaultCoreTestSpec;
