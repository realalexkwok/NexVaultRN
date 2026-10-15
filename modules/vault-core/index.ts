import VaultCoreModule from './src/VaultCoreModule';

/**
 * Imports a BIP-39 English mnemonic. The caller passes UTF-8 bytes only; no
 * mnemonic string exists on the JS side. The returned value is an opaque,
 * non-encoding handle for use by later vault-core operations.
 *
 * R1: the zero-copy Uint8Array view is wiped as soon as the native call
 * settles, covering the caller's ArrayBuffer contents in `finally`.
 */
export async function importMnemonic(mnemonicBytes: ArrayBuffer): Promise<string> {
  const view = new Uint8Array(mnemonicBytes);
  try {
    return await VaultCoreModule.importMnemonic(view);
  } finally {
    view.fill(0);
  }
}
