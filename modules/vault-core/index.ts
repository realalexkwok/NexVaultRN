import VaultCoreModule from './src/VaultCoreModule';

/** Fixed error code for the fail-closed platform guard (FR-8). */
const UNSUPPORTED_PLATFORM_CODE = 'E_VAULT_UNSUPPORTED_PLATFORM';

/** Fixed error code for a digest whose length is not exactly 32 bytes (spec: vault-core §2.4). */
const INVALID_DIGEST_LENGTH_CODE = 'E_VAULT_INVALID_DIGEST_LENGTH';

/** Fixed error code for a native return value that violates a frozen contract (spec: vault-core §2.4). */
const CRYPTO_INTERNAL_CODE = 'E_VAULT_CRYPTO_INTERNAL';

/**
 * Opaque handle returned by {@link importMnemonic}. String-branded: it is an ID only and
 * carries no key semantics (spec: vault-core §2.1, amended 2026-10-15 for T1.3/H2).
 */
export type VaultHandle = string & { readonly __brand: 'VaultHandle' };

/**
 * Builds the guard error with a machine-readable `code` property, so callers can branch on
 * `error.code` instead of parsing the message text (native errors already surface through
 * Expo's `CodedException.code`).
 */
function unsupportedPlatformError(operation: string): Error & { code: string } {
  const error = new Error(
    `${UNSUPPORTED_PLATFORM_CODE}: vault-core ${operation} is unavailable on this platform`
  ) as Error & { code: string };
  error.code = UNSUPPORTED_PLATFORM_CODE;
  return error;
}

/**
 * Imports a BIP-39 English mnemonic. The caller passes UTF-8 bytes only; no
 * mnemonic string exists on the JS side. The returned value is an opaque,
 * non-encoding handle for use by later vault-core operations.
 *
 * R1: the zero-copy Uint8Array view is wiped as soon as the native call
 * settles, covering the caller's ArrayBuffer contents in `finally`.
 *
 * R7: on web the module resolves to the read-only stub (`VaultCoreModule.web.ts`),
 * which exposes no key path. The guard below converts that into a fixed
 * fail-closed error code instead of a raw "not a function" TypeError.
 */
export async function importMnemonic(mnemonicBytes: ArrayBuffer): Promise<string> {
  const view = new Uint8Array(mnemonicBytes);
  try {
    if (typeof VaultCoreModule.importMnemonic !== 'function') {
      throw unsupportedPlatformError('importMnemonic');
    }
    return await VaultCoreModule.importMnemonic(view);
  } finally {
    view.fill(0);
  }
}

/**
 * Derives the EIP-55 checksummed EVM address for `handle` at `path` (for example
 * `m/44'/60'/0'/0/0`). The seed stays in the native handle store; only the public address
 * string crosses the boundary (R1).
 *
 * R7: same fail-closed platform guard as {@link importMnemonic} — on web the stub exposes no
 * `deriveAddress`, so the call throws `E_VAULT_UNSUPPORTED_PLATFORM` instead of a raw
 * "not a function" TypeError.
 */
export async function deriveAddress(handle: VaultHandle, path: string): Promise<string> {
  if (typeof VaultCoreModule.deriveAddress !== 'function') {
    throw unsupportedPlatformError('deriveAddress');
  }
  return await VaultCoreModule.deriveAddress(handle, path);
}

/**
 * Signs a 32-byte digest with the secp256k1 key derived at `path` and returns the 65-byte
 * signature `r(32) ‖ s(32) ‖ recid(1)` (RFC6979 deterministic ECDSA; recid ∈ {0,1}).
 *
 * The digest is a public transaction hash, not key material, so it is not wiped (R1); the
 * private key never leaves native. The returned buffer owns exactly 65 bytes — the native
 * buffer may be pooled, so it is never handed out directly.
 *
 * R7: same fail-closed platform guard as {@link importMnemonic}.
 */
export async function signDigest(
  handle: VaultHandle,
  path: string,
  digest: ArrayBuffer
): Promise<ArrayBuffer> {
  if (typeof VaultCoreModule.signDigest !== 'function') {
    throw unsupportedPlatformError('signDigest');
  }
  if (digest.byteLength !== 32) {
    const error = new Error(
      `${INVALID_DIGEST_LENGTH_CODE}: digest must be exactly 32 bytes`
    ) as Error & { code: string };
    error.code = INVALID_DIGEST_LENGTH_CODE;
    throw error;
  }
  const signature = await VaultCoreModule.signDigest(handle, path, new Uint8Array(digest));
  if (signature.byteLength !== 65) {
    const error = new Error(
      `${CRYPTO_INTERNAL_CODE}: native signature length contract violated`
    ) as Error & { code: string };
    error.code = CRYPTO_INTERNAL_CODE;
    throw error;
  }
  const copy = new Uint8Array(65);
  copy.set(signature);
  return copy.buffer;
}

/**
 * Destroys the handle: the native store evicts the entry and wipes the stored seed. Idempotent —
 * a second call (or an unknown handle) resolves without throwing (spec: vault-core §2.4); after
 * destroy, every operation with the old handle rejects with `E_VAULT_HANDLE_NOT_FOUND`.
 *
 * R7: same fail-closed platform guard as {@link importMnemonic}.
 */
export async function destroy(handle: VaultHandle): Promise<void> {
  if (typeof VaultCoreModule.destroy !== 'function') {
    throw unsupportedPlatformError('destroy');
  }
  await VaultCoreModule.destroy(handle);
}
