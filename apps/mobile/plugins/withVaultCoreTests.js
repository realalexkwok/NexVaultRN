const fs = require('fs');
const path = require('path');
// Import from `expo/config-plugins`, not `@expo/config-plugins`: under pnpm's isolated layout this
// app has no node_modules edge to the latter, so the Xcode script phase that generates the prebuilt
// Constants manifest (expo-constants -> @expo/config library path, i.e. without the Expo CLI's own
// resolution hooks) aborted with INVALID_PLUGIN_IMPORT / "Cannot find module
// '@expo/config-plugins'". `expo` is a declared dependency of this app and re-exports the identical
// API. Evidence: specs/features/2026-10-16-T1.5-vault-core-ios-port/validation.md (plugin-import fix).
const { withPodfile } = require('expo/config-plugins');

/**
 * T1.5 (vault-core iOS port) — integrate the `modules/vault-core` iOS pods and their test spec.
 *
 * Since T1.5-r2 the module is two pods: `VaultCoreCore` (crypto core, iOS 15.0, no Expo) and
 * `VaultCore` (the Expo bridge, iOS 16.4, depends on the core). Expo autolinking only discovers the
 * app-facing pod, and CocoaPods only creates a pod's test-spec target when the Podfile asks for it
 * with `:testspecs` — which autolinking never passes for local modules. So without this plugin the
 * generated Pods project has `VaultCore` but no `VaultCoreCore-Unit-Tests` target/scheme, and the
 * T1.5 runbook's `xcodebuild test -scheme VaultCoreCore-Unit-Tests` cannot run.
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
const VAULT_CORE_PODSPEC_DIR = path.join('modules', 'vault-core', 'ios');

/**
 * Locate the podspec directory by walking up from the app root.
 *
 * `config.modRequest.projectRoot` is the app directory when prebuild runs from `apps/mobile` and the
 * workspace root when it runs from the repo root; the module itself always lives at
 * `<repo>/modules/vault-core/ios`. Resolving it by probing keeps the injected `:path` identical to
 * the one Expo autolinking generates (two different paths for one pod make `pod install` fail).
 */
function resolveVaultCoreDir(projectRoot) {
  let dir = projectRoot;
  for (let depth = 0; depth < 8; depth += 1) {
    const candidate = path.join(dir, VAULT_CORE_PODSPEC_DIR);
    if (fs.existsSync(path.join(candidate, 'VaultCoreCore.podspec'))) return candidate;
    const parent = path.dirname(dir);
    if (parent === dir) break;
    dir = parent;
  }
  throw new Error(
    'withVaultCoreTests: cannot locate modules/vault-core/ios (VaultCoreCore.podspec) above ' + projectRoot
  );
}
const DECLARATION_COMMENT = [
  '# T1.5-r2: crypto core (iOS 15.0, owns the test spec) + Expo bridge (iOS 16.4). The test scheme',
  '# `VaultCoreCore-Unit-Tests` must exist for `xcodebuild test`.',
  '# Injected by apps/mobile/plugins/withVaultCoreTests.js.',
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
  let contents = podfileContents;

  if (!/^[ \t]*pod[ \t]+'VaultCoreCore'/m.test(contents)) {
    const anchor = /^([ \t]*)use_expo_modules!\s*$/m;
    if (!anchor.test(contents)) {
      throw new Error(
        'withVaultCoreTests: no `use_expo_modules!` line found in the generated Podfile; the ' +
          'vault-core pods and their test spec (VaultCoreCore-Unit-Tests) cannot be integrated.'
      );
    }
    const relativePath = path.relative(podfileDir, resolveVaultCoreDir(projectRoot));
    contents = contents.replace(anchor, (line, indent) => {
      const injected = [
        ...DECLARATION_COMMENT.map((text) => indent + text),
        `${indent}pod 'VaultCoreCore', :path => '${relativePath}', :testspecs => ['Tests']`,
        `${indent}pod 'VaultCore', :path => '${relativePath}'`,
      ].join('\n');
      return `${line}\n${injected}`;
    });
  }

  // No post_install hook here: CocoaPods rejects a second `post_install` in one Podfile. The core
  // pod stays at iOS 15.0 because `modules/vault-core/expo-module.config.json` pins
  // `apple.podspecPath` to VaultCore.podspec, so Expo autolinking never treats VaultCoreCore as an
  // Expo module and its "raise every autolinked pod to ExpoModulesCore's floor" step skips it.
  return contents;
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
