const fs = require('fs');
const path = require('path');
const { withDangerousMod } = require('expo/config-plugins');

/**
 * T3.2 — expected hashes for the native self-checks (signature cert + JS bundle).
 *
 * Why prebuild-time instead of a Gradle task: the T3.2 implementation tried the Gradle route first
 * (a generated resource directory + task wiring) and Gradle 9 rejected every variant of it with
 * "uses this output without declaring an explicit or implicit dependency" across the resource task
 * graph (mapSourceSetPaths / generateResources / extractDeepLinks ...). Per plan Task 4 / Task 8 the
 * documented fallback D3(b) applies: the expected values are backfilled from a built artifact in a
 * two-stage local build, which is also the local form of the roadmap item "bundle hash CI two-stage
 * backfill pipeline". No task graph is touched, so the build is deterministic.
 *
 * Source of truth: apps/mobile/security-hashes.json (tracked). Refresh it with
 *   node scripts/update-security-hashes.mjs <path-to-built-apk>
 * then re-run `expo prebuild --clean` and rebuild. Values are validated as lowercase 64-hex;
 * anything else (missing file, placeholder) degrades to NOT_EVALUATED, which the native checks
 * report as "not evaluated" — never as "passed" (R6 honesty).
 *
 * CNG: writes into the generated android project at prebuild time; android/ itself stays untracked.
 */
const NOT_EVALUATED = 'NOT_EVALUATED';
const VALUES_DIR = ['app', 'src', 'main', 'res', 'values'];
const FILE = 'nexvault_security_hashes.xml';

function normalizeHash(value) {
  if (typeof value !== 'string') return null;
  const normalized = value.trim().toLowerCase().replace(/:/g, '');
  return /^[0-9a-f]{64}$/.test(normalized) ? normalized : null;
}

function findHashesFile(projectRoot) {
  let dir = projectRoot;
  for (let depth = 0; depth < 8; depth += 1) {
    const candidate = path.join(dir, 'security-hashes.json');
    if (fs.existsSync(candidate)) return candidate;
    const parent = path.dirname(dir);
    if (parent === dir) break;
    dir = parent;
  }
  return null;
}

function readHashes(projectRoot) {
  const file = findHashesFile(projectRoot);
  if (!file) return { cert: NOT_EVALUATED, bundle: NOT_EVALUATED, source: null };
  try {
    const parsed = JSON.parse(fs.readFileSync(file, 'utf8'));
    return {
      cert: normalizeHash(parsed.certSha256) ?? NOT_EVALUATED,
      bundle: normalizeHash(parsed.bundleSha256) ?? NOT_EVALUATED,
      source: file,
    };
  } catch (error) {
    return { cert: NOT_EVALUATED, bundle: NOT_EVALUATED, source: file };
  }
}

function renderResource(hashes) {
  return (
    '<?xml version="1.0" encoding="utf-8"?>\n' +
    '<resources>\n' +
    '  <string name="nexvault_expected_cert_sha256" translatable="false">' + hashes.cert + '</string>\n' +
    '  <string name="nexvault_expected_bundle_sha256" translatable="false">' + hashes.bundle + '</string>\n' +
    '</resources>\n'
  );
}

module.exports = function withSecurityHashes(config) {
  return withDangerousMod(config, [
    'android',
    async (cfg) => {
      const dir = path.join(cfg.modRequest.platformProjectRoot, ...VALUES_DIR);
      fs.mkdirSync(dir, { recursive: true });
      const hashes = readHashes(cfg.modRequest.projectRoot);
      fs.writeFileSync(path.join(dir, FILE), renderResource(hashes));
      return cfg;
    },
  ]);
};

module.exports.readHashes = readHashes;
module.exports.renderResource = renderResource;
module.exports.normalizeHash = normalizeHash;
