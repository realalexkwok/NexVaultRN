#!/usr/bin/env node
/**
 * T3.2 — refresh apps/mobile/security-hashes.json from a built APK (two-stage backfill, D3(b)).
 *
 * Stage 1: build the APK (release for the demo). Stage 2: run this script on it, re-run
 * `expo prebuild --clean`, rebuild. The values are consumed by
 * apps/mobile/plugins/withSecurityHashes.js, which writes the Android string resources read by
 * modules/security-android (SignatureSelfCheck / BundleSelfCheck).
 *
 * The CI pipeline that performs this automatically for every artifact is the roadmap item
 * "bundle 哈希 CI 两段构建回填流水线" (roadmap stage 2, item 1) — this script is its local form.
 *
 * Usage: node scripts/update-security-hashes.mjs <path-to-apk>
 * Requires: unzip + Android build-tools apksigner (or a JDK keytool fallback).
 */
import { execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { existsSync, mkdtempSync, readdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const outFile = join(repoRoot, 'apps', 'mobile', 'security-hashes.json');
const apk = process.argv[2];

if (!apk) {
  console.error('usage: node scripts/update-security-hashes.mjs <path-to-apk>');
  process.exit(1);
}

const apkPath = resolve(apk);

function sha256Hex(bytes) {
  return createHash('sha256').update(bytes).digest('hex');
}

/** Newest apksigner in $ANDROID_HOME/build-tools (apksigner reads v2/v3 blocks; keytool needs v1). */
function findApksigner() {
  const sdk =
    process.env.ANDROID_HOME ||
    process.env.ANDROID_SDK_ROOT ||
    join(process.env.HOME ?? '', 'Android', 'Sdk');
  try {
    const builds = readdirSync(join(sdk, 'build-tools')).sort().reverse();
    for (const build of builds) {
      const candidate = join(sdk, 'build-tools', build, 'apksigner');
      if (existsSync(candidate)) return candidate;
    }
  } catch {
    // fall through to keytool
  }
  return null;
}

function certSha256FromApk() {
  const apksigner = findApksigner();
  if (apksigner) {
    const output = execFileSync(apksigner, ['verify', '--print-certs', apkPath], {
      encoding: 'utf8',
    });
    const match = output.match(/certificate SHA-256 digest:\s*([0-9a-fA-F]{64})/);
    if (match) return match[1].toLowerCase();
  }
  const keytoolOutput = execFileSync('keytool', ['-printcert', '-jarfile', apkPath], {
    encoding: 'utf8',
  });
  const match = keytoolOutput.match(/SHA256:\s*([0-9A-F:]+)/);
  if (!match) throw new Error('could not parse the signing certificate SHA-256');
  return match[1].replace(/:/g, '').toLowerCase();
}

// 1) Signing certificate SHA-256 of the artifact.
const certSha256 = certSha256FromApk();

// 2) JS bundle SHA-256 from assets/index.android.bundle inside the APK (release embeds it).
const workDir = mkdtempSync(join(tmpdir(), 'nexvault-hashes-'));
let bundleSha256 = 'NOT_EVALUATED';
try {
  execFileSync('unzip', ['-o', '-q', apkPath, 'assets/index.android.bundle', '-d', workDir]);
  bundleSha256 = sha256Hex(readFileSync(join(workDir, 'assets', 'index.android.bundle')));
} catch {
  console.warn('warn: APK has no embedded assets/index.android.bundle (debug/Metro build?)');
} finally {
  rmSync(workDir, { recursive: true, force: true });
}

const payload = {
  // Backfilled by scripts/update-security-hashes.mjs; do not hand-edit (R5/R6 honesty).
  generatedFrom: apkPath.replace(repoRoot + '/', ''),
  certSha256,
  bundleSha256,
  updatedAt: new Date().toISOString(),
};
writeFileSync(outFile, JSON.stringify(payload, null, 2) + '\n');
console.log(JSON.stringify(payload, null, 2));
