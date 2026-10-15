# NexVault T1.3 — vault-core Android BIP32/BIP44 deriveAddress (combined skill)

## Provenance

No single published skill covered all of T1.3, so this skill combines the best parts of the
following verified sources (same combine pattern as the T1.2 skill):

| Source | What it contributes to T1.3 | Gap it leaves |
|---|---|---|
| Expo official [`expo-module`](https://github.com/expo/skills/blob/main/plugins/expo/skills/expo-module/SKILL.md) skill | Expo Modules API DSL, `AsyncFunction`, Kotlin↔JS type map, autolinking rules — the `deriveAddress` wiring | No crypto/domain knowledge, no test discipline |
| [`android-testing-skills`](https://github.com/skydoves/android-testing-skills) (JVM + instrumentation sets) | JUnit4 JVM test organization, AndroidJUnit4 device workflow, ADB/Gradle test commands, evidence discipline | No BIP32 or BouncyCastle content |
| [claude-dev-suite `bitcoin-bip32`](https://github.com/claude-dev-suite/claude-dev-suite/blob/main/skills/bitcoin/cryptography/bip32/SKILL.md) skill | Exact BIP32 formulas (HMAC-SHA512 "Bitcoin seed" master derivation, hardened vs unhardened CKD, ser256/serP/ser32), the 78-byte xprv/xpub serialization layout with version bytes, parent fingerprint, and the parent-pubkey-leak warning | Bitcoin-flavored domain reference; no Expo/BouncyCastle/EVM-address workflow |
| [j4flmao `blockchain-cryptography`](https://github.com/j4flmao/agent-skills/blob/main/skills/blockchain/blockchain-cryptography/SKILL.md) | Curve-selection discipline (secp256k1 for EVM), Keccak-256 vs SHA3-256 distinction, known-answer-test practice (RFC6979 preview for T1.4) | Language-agnostic reference (C++/Rust/Go/Python); not Kotlin/Android operational |
| [BIP-0032](https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki) + [BIP-0044](https://github.com/bitcoin/bips/blob/master/bip-0044.mediawiki) + [ERC-55](https://github.com/ethereum/ercs/blob/master/ERCS/erc-55.md) official specs/vectors | Algorithm ground truth: official BIP-32 test vectors 1–5, path-level semantics (purpose/coin_type/account/change/address_index), EIP-55 checksum algorithm + test vectors | Reference material, not an agent workflow |
| [BouncyCastle javadocs](https://javadoc.io/doc/org.bouncycastle/bcprov-jdk18on/latest/org/bouncycastle/crypto/generators/HDKeyDerivation.html) (`HDKeyDerivation`/`HDPath`/`CustomNamedCurves`/`KeccakDigest`/`Base58`) | Verified API surface of the R4-approved crypto provider | Reference material |
| NexVault `AGENTS.md` R1/R4/R5/B.6 + T1.2 skill/validation + `specs/vault-core.md` §1–§6 | Security red lines, native handle-store reuse, zeroization-review pattern, R4 amendment workflow | Project-specific only |

Rejected as primary sources: Trust Wallet `wallet-core` skill (its library is banned by
`specs/vault-core.md` §6 — String-held mnemonics + CVE-2025-66692), `affaan-m/ECC
nodejs-keccak256` (Node.js platform), ApeWorX skills (Python), and the original NexVault
repo's web3j `Bip32ECKeyPair` approach (reference-only per GATE-REFERENCE-ORIGINAL-REPO; the
RN spec mandates packaged BouncyCastle — web3j would be a second R4 dependency for no gain).

## Scope

- Implement **only** `deriveAddress(handle, path) → Promise<string>` (EIP-55 checksummed EVM
  address) on Android: BIP-32 (secp256k1) + BIP-44 path parsing + keccak256(pub)[12:] + EIP-55.
- **Do not** implement `signDigest` / `destroy` — those are T1.4 (H1 → replan T1.4).
- **Do not** add the BouncyCastle Gradle dependency before the Step 2 R4 amendment is
  owner-approved.
- **Do not** write iOS/web implementations in this item (web stub keeps failing closed with
  `E_VAULT_UNSUPPORTED_PLATFORM`).
- Owner decisions 2026-10-15: propose `bcprov-jdk18on` **1.86**; H2 checksum error split →
  **amend spec to T1.2's single code** (merge `E_VAULT_CHECKSUM_MISMATCH` into
  `E_VAULT_INVALID_MNEMONIC`); H6/H7 → **replan to T1.1 closure** (not T1.3 scope; T1.3 must
  not upgrade expo).

## Invariants (MUST)

1. **R1 boundary**: the seed stays in the T1.2 native `handleStore`; `deriveAddress`
   consumes it and returns only the public address string. No key material crosses the
   JS↔native boundary in either direction.
2. **Keccak-256, not SHA3-256**: use BC `KeccakDigest(256)` (pre-standard Keccak). NIST
   `SHA3Digest` produces different digests. Gate with the sanity vector
   `keccak256("") = c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470`.
3. **BC lightweight API only**: use `org.bouncycastle.crypto.*` classes directly. **Never**
   register `BouncyCastleProvider` as a JCE provider — that clashes with the ancient
   BouncyCastle remnants in the Android platform.
4. **R4**: `bcprov-jdk18on` pinned exact `1.86` (Maven Central lastUpdated 2026-09-11 →
   min-release-age ≥ 7 days satisfied); `specs/tech-stack.md` + `doc/dependencies.md`
   amendment + owner approval **before** touching `build.gradle` (T1.2 junit precedent).
5. **R5**: no `Log.*`/`console.*` with key material; error messages use the §2.4 whitelist
   codes with fixed templates; messages must not echo input fragments (path syntax class echo
   is allowed, key fragments are not).
6. **B.6**: every step records its command and observed output in
   `specs/features/2026-10-15-T1.3-vault-core-android-bip32/validation.md`; no `[x]`
   without command + observed result. Never weaken a failing test (B.6-5).
7. **Zeroization**: every transient `ByteArray` this feature creates (seed copies, parent
   serialization buffers, keccak buffers) is `fill(0)`-ed in `finally`. BC `BigInteger`
   private-key intermediates are immutable and not reliably zeroizable — record this as a
   documented **[C]** caveat (same class as the T1.2 String/BigInteger caveat), never as [P].
8. **Path whitelist**: accept only `m/44'/60'/<account>'/0/<index>` with
   `account, index ∈ [0, 2^31)`; the hardened bit is added only where the apostrophe says.

## Preflight (Step 0)

```bash
cd /home/superguo/Projects/NexVaultRN
git status                     # record baseline
git switch -c feature/T1.3-vault-core-android-bip32   # short-lived branch per AGENTS B.3
cd apps/mobile
npx expo-modules-autolinking search --platform android --json | grep vault-core   # must resolve
```

Record all outputs. Before writing code:

- Re-read the T1.2 validation Handoff list (H1 destroy→T1.4, H2 spec conflicts, H3 JS-bridge
  invocation deferred to T1.3, H6/H7) — every item gets a disposition this round.
- GATE-REFERENCE-ORIGINAL-REPO: read
  `/home/superguo/Projects/NexVault/core/core-security/src/main/java/com/nexvault/wallet/core/security/wallet/HDKeyManager.kt`
  and `HDKeyManagerTest.kt` — canonical anchor `TC-SEC-005`:
  `abandon×23 about` @ `m/44'/60'/0'/0/0` → **`0x9858EfFD232B4033E47d90003D41EC34EcaEda94`**.
  Their web3j implementation is reference-only; do not copy the library.
- Download `bip-0032.mediawiki`, record `sha256sum`, and independently verify vector 1's
  master xprv (HMAC-SHA512("Bitcoin seed", seed) → 0x00||IL||IR → Base58Check; stdlib Python
  is enough) before transcribing vectors into tests — the T1.2 P0.4 pattern.

## Step 1 — Pin the TS interface

TS declaration (`modules/vault-core/src/VaultCoreModule.ts`):

```typescript
import { NativeModule, requireNativeModule } from 'expo';

declare class VaultCoreModule extends NativeModule<{}> {
  importMnemonic(bytes: Uint8Array): Promise<string>;
  deriveAddress(handle: string, path: string): Promise<string>;
}

export default requireNativeModule<VaultCoreModule>('VaultCore');
```

Public barrel (`modules/vault-core/index.ts`) — extend the T1.2 R1 wrapper with the same
fail-closed platform guard (T1.2 review-fix NIT-4 pattern; `error.code` machine-readable):

```typescript
export type VaultHandle = string & { readonly __brand: 'VaultHandle' };

export async function deriveAddress(handle: VaultHandle, path: string): Promise<string> {
  if (typeof VaultCoreModule.deriveAddress !== 'function') {
    const err = new Error('vault-core deriveAddress is unavailable on this platform');
    err.name = 'E_VAULT_UNSUPPORTED_PLATFORM';
    (err as Error & { code?: string }).code = 'E_VAULT_UNSUPPORTED_PLATFORM';
    throw err;
  }
  return VaultCoreModule.deriveAddress(handle, path);
}
```

H2 amendment (owner-approved direction, record the diff in this item's spec round):
`specs/vault-core.md` §2.1 `VaultHandle` becomes a **string-brand UUID** (matching the
committed T1.2 implementation, not `number`); §2.2 `AsyncFunction` signatures take
`handle: String`; §2.4 merges `E_VAULT_CHECKSUM_MISMATCH` into
`E_VAULT_INVALID_MNEMONIC`.

## Step 2 — R4 gate: BouncyCastle amendment (BLOCKING, before any install)

Amend `specs/tech-stack.md` (new row) and `doc/dependencies.md` (version, purpose, publish
date, CVE disposition, trust basis) with:

- `org.bouncycastle:bcprov-jdk18on:1.86`, `implementation`, `modules/vault-core/android`
- purpose: BIP-32/44 HD derivation (secp256k1), Keccak-256, Base58Check test helpers — the
  `specs/vault-core.md` §2.2/§6 mandated "packaged new BouncyCastle" for the Android MVP
- evidence: Maven Central artifact, lastUpdated 2026-09-11 (≥7-day min-release-age), no
  known unpatched CVE in the 1.8x line at amendment time (re-verify against the BC security
  notes and Snyk before submitting), trust basis = BouncyCastle ecosystem incl. bitcoinj
- do **not** add web3j / bitcoinj / @noble / @scure to production — only BC

Get owner approval (recorded in `validation.md` like T1.2 FR-10). Only then:

```gradle
// modules/vault-core/android/build.gradle
implementation("org.bouncycastle:bcprov-jdk18on:1.86")
```

## Step 3 — BIP32/BIP44 + address core

Create `modules/vault-core/android/src/main/java/expo/modules/vaultcore/Bip32.kt` with pure
`internal` functions so JVM tests can call them (T1.2 Bip39.kt pattern):

- `internal fun parseEthereumPath(path: String): IntArray`
  - regex whitelist `^m/44'/60'/(\d+)'/0/(\d+)$` (accept `h`/single-quote hardened
    spellings but nothing else),
  - `account, index < 0x80000000`, non-negative,
  - return `intArrayOf(44 or 0x80000000, 60 or 0x80000000, account or 0x80000000, 0, index)`,
  - violations throw the typed `E_VAULT_INVALID_PATH` (message echoes only the path syntax
    class, never key material).
- `internal fun deriveAddressBytes(seed: ByteArray, pathIndices: IntArray): ByteArray` (20B)
  - `val master = HDKeyDerivation.createMasterPrivateKey(seed)` — pass a **copy** of the
    stored seed and `fill(0)` the copy in `finally` (the store copy survives for T1.4);
  - loop `HDKeyDerivation.deriveChildKey(parent, index)` over `pathIndices`;
  - public key: `CustomNamedCurves.getByName("secp256k1").g.multiply(privKey.getD())` →
    `getEncoded(false)` (65B uncompressed `0x04||x||y`); take the trailing 64 bytes;
  - address bytes: `KeccakDigest(256)` over `x||y`, take the last 20 bytes;
  - wipe the 64-byte `x||y` buffer and keccak buffers in `finally`;
  - the private `BigInteger` intermediates are immutable → **[C] caveat**, documented in
    validation.md (Invariant 7).
- `internal fun toChecksumAddress(addressBytes: ByteArray): String`
  - lowercase hex → `KeccakDigest(256)` over the hex **string** (ASCII) →
    per ERC-55, uppercase letter nibbles where the hash nibble ≥ 8 → prefix `0x`;
  - ERC-55 self-test vectors: `0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed`,
    `0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359`,
    `0xdbF03B407c01E7cD3CBea99509d93f8DDDC8C6FB`,
    `0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb`.
- `internal fun keccak256(bytes: ByteArray): ByteArray` — `KeccakDigest(256)` wrapper.

Rules: no `String` mnemonic/seed representations; no `Log.*`; every scratch buffer
`fill(0)` in `finally`; do not self-implement point math (spec §2.2 — BC only).

## Step 4 — Module wiring

In `VaultCoreModule.kt` (reuse the T1.2 `handleStore`, typed exceptions,
`withContext(Dispatchers.Default)`):

```kotlin
AsyncFunction("deriveAddress") Coroutine { handle: String, path: String ->
  withContext(Dispatchers.Default) {
    val seed = handleStore[handle] ?: throw HandleNotFoundException()
    val indices = Bip32.parseEthereumPath(path)          // E_VAULT_INVALID_PATH
    val seedCopy = seed.copyOf()
    try {
      Bip32.toChecksumAddress(Bip32.deriveAddressBytes(seedCopy, indices))
    } finally {
      seedCopy.fill(0)
    }
  }
}
```

- Typed errors (fixed templates, no fragments): `E_VAULT_INVALID_PATH`,
  `E_VAULT_HANDLE_NOT_FOUND`, `E_VAULT_CRYPTO_INTERNAL`; `destroy` semantics stay T1.4.
- Unknown handle must **not** distinguish "empty store" from "bad handle" in any log.
- No `Log.*`/print in `src/main`.

## Step 5 — Tests (official vectors first)

JVM `modules/vault-core/android/src/test/java/expo/modules/vaultcore/Bip32Test.kt`:

1. **BIP-32 official vectors 1–5** (transcribe every chain's xprv/xpub from
   `bip-0032.mediawiki`, verified in Step 0): implement a test-side serializer — 78-byte
   layout `version(4) | depth(1) | parentFingerprint(4) | childNumber(4, BE, high bit =
   hardened) | chainCode(32) | key(33: 0x00||priv for xprv, compressed pub for xpub)`, then
   Base58Check = `Base58.encode(data || SHA256(SHA256(data)).take(4))` (BC `Base58` +
   `SHA256Digest`); version bytes `xprv = 0x0488ADE4`, `xpub = 0x0488B21E`; parent
   fingerprint = first 4 bytes of `RIPEMD160(SHA256(pub))`. Assert master + every listed
   chain for all five seeds. `// TC-VC-XXX` comments trace every vector.
2. **Canonical address** (`TC-VC-003`): `Bip39.mnemonicToSeed("abandon×23 about")` →
   `deriveAddressBytes` @ `m/44'/60'/0'/0/0` → checksummed address ==
   `0x9858EfFD232B4033E47d90003D41EC34EcaEda94` (original-repo `TC-SEC-005` record).
3. **Second address cross-check**: `m/44'/60'/0'/0/1` value cross-checked against an
   independent implementation (`@scure/bip32` in a throwaway `/tmp` Node script, or stdlib
   Python hmac+ecdsa — **no repo dependency change**); record the value in validation.md.
4. **Keccak sanity**: `keccak256("") == c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470`
   (guards against a SHA3Digest mix-up, Invariant 2).
5. **EIP-55**: the four ERC-55 vectors above, all-lowercase inputs produce the mixed-case
   outputs.
6. **Negatives**: `m/`, `m/49'/60'/0'/0/0`, non-numeric, hardened change/index
   (`m/44'/60'/0'/1'/0`), `account ≥ 2^31` → typed `E_VAULT_INVALID_PATH`; unknown handle
   → `E_VAULT_HANDLE_NOT_FOUND`.

Instrumentation `VaultCoreModuleTest.kt` (H3 closure — the JS-bridge invocation T1.2
deferred per plan §5.4):

- update `moduleDefinitionExposesImportMnemonic` → also assert `deriveAddress` exists
  (**no map-size pin** — Expo's builder adds `startObserving`/`stopObserving`; T1.2 lesson);
- end-to-end on device: call `importMnemonic` with the canonical 12-word bytes through the
  module path → non-empty handle → `deriveAddress(handle, "m/44'/60'/0'/0/0")` ==
  `0x9858EfFD232B4033E47d90003D41EC34EcaEda94`;
- invalid path → typed error with machine-readable code.

Gradle (exact project path after prebuild — verify with `./gradlew projects` first):

```bash
cd apps/mobile
npx expo prebuild --platform android --no-install
cd android
./gradlew projects
./gradlew :vault-core:testDebugUnitTest --rerun                 # BIP32 vectors (JVM)
ANDROID_SERIAL=<serial> ./gradlew :vault-core:connectedDebugAndroidTest   # device smoke
```

Pin `ANDROID_SERIAL` — T1.2's duplicate-transport install failure lesson (Pixel 6a exposed
through two adb transports). Acceptance: all vector assertions green, `BUILD SUCCESSFUL`.

## Step 6 — Verification and handoff

```bash
cd /home/superguo/Projects/NexVaultRN
git diff -- package.json apps/mobile/package.json pnpm-lock.yaml   # prove zero npm drift
grep -rnEi 'mnemonic|privateKey|secretKey|seedPhrase|xprv' apps/mobile   --include='*.ts' --include='*.tsx' --exclude-dir=node_modules    # R1 CI-style check
grep -rnE 'Log.|println' modules/vault-core/android/src           # R5 native log check
cd apps/mobile
npx --no-install tsc --noEmit
```

Write into `specs/features/2026-10-15-T1.3-vault-core-android-bip32/validation.md`:

- every command, exit code, observed result;
- BIP-32 vector pass/fail table (vector id → chains → xprv/xpub match);
- zeroization code-review note (every sensitive buffer + `fill(0)` location; the immutable
  `BigInteger` intermediates listed as **[C]** caveat);
- R4 amendment approval record (owner, date, exact coordinate);
- Handoff list: H1 `destroy` → replan T1.4; H2 spec amendments executed (record diff); H3
  closed by the instrumentation canonical-address test; H6/H7 → replan T1.1 closure (T1.3 did
  not upgrade expo); BC version pinned for the T1.4 signing work.

Do not mark T1.3 `[x]` until both the automated vector gate and the zeroization review are
recorded with command + output.
