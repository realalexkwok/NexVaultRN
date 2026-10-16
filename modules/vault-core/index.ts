import VaultCoreModule from './src/VaultCoreModule';

/** Fixed error code for the fail-closed platform guard (FR-8). */
const UNSUPPORTED_PLATFORM_CODE = 'E_VAULT_UNSUPPORTED_PLATFORM';

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
