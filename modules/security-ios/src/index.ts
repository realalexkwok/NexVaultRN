import * as SecureStore from 'expo-secure-store';

/**
 * T3.1 iOS minimal JS face (spec: security-ios section 2.2). The KEK entry is created and removed
 * through expo-secure-store with requireAuthentication + WHEN_UNLOCKED_THIS_DEVICE_ONLY; the
 * Swift half (KeychainKek + AesGcmEnvelope) reads the same Keychain item for native-internal
 * wrap/unwrap. wrap/unwrap reach JS in T3.2 together with vault-core.
 *
 * AGENTS.md R1 note: the KEK is not k1 — k1 plaintext never crosses the JS bridge; only the
 * wrapping key's creation touches JS, exactly as the spec's section 2.2 calling surface shows.
 */
const KEK_KEY = 'nexvault.kek.v1';

const KEK_OPTIONS: SecureStore.SecureStoreOptions = {
  requireAuthentication: true,
  keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY,
};

export interface KeychainWrapInfo {
  backend: 'KEYCHAIN_BIOMETRIC_ACL';
  aclEnforcedBySecureEnclave: true;
}

/** 32 random bytes as lowercase hex; SecureStore stores strings, Swift decodes the same encoding. */
function randomKekHex(): string {
  const bytes = new Uint8Array(32);
  globalThis.crypto.getRandomValues(bytes);
  let hex = '';
  for (const byte of bytes) {
    hex += byte.toString(16).padStart(2, '0');
  }
  bytes.fill(0);
  return hex;
}

export async function ensureKek(): Promise<KeychainWrapInfo> {
  const existing = await SecureStore.getItemAsync(KEK_KEY, KEK_OPTIONS);
  if (existing === null) {
    await SecureStore.setItemAsync(KEK_KEY, randomKekHex(), KEK_OPTIONS);
  }
  return { backend: 'KEYCHAIN_BIOMETRIC_ACL', aclEnforcedBySecureEnclave: true };
}

export async function deleteKek(): Promise<void> {
  await SecureStore.deleteItemAsync(KEK_KEY, KEK_OPTIONS);
}
