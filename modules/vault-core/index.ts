import VaultCoreModule from './src/VaultCoreModule';

/** Fixed error code for the fail-closed platform guard (FR-8). */
const UNSUPPORTED_PLATFORM_CODE = 'E_VAULT_UNSUPPORTED_PLATFORM';

/**
 * Builds the guard error with a machine-readable `code` property, so callers can branch on
 * `error.code` instead of parsing the message text (native errors already surface through
 * Expo's `CodedException.code`).
 */
function unsupportedPlatformError(): Error & { code: string } {
  const error = new Error(
    `${UNSUPPORTED_PLATFORM_CODE}: vault-core importMnemonic is unavailable on this platform`
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
      throw unsupportedPlatformError();
    }
    return await VaultCoreModule.importMnemonic(view);
  } finally {
    view.fill(0);
  }
}
