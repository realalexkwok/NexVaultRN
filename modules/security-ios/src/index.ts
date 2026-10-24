import { NativeModule, requireNativeModule } from 'expo';
import * as SecureStore from 'expo-secure-store';

/**
 * Public JS face for modules/security-ios (spec: security-ios section 2.1).
 *
 * T3.1 shipped ensureKek/deleteKek over expo-secure-store; T3.2 adds the wrap/unwrap ArrayBuffer
 * face plus detection and the fail-closed gate. Call order is fixed: gate -> Keychain biometric
 * read -> crypto. On web (and any platform without the native module) every native call throws the
 * fixed E_SEC_UNSUPPORTED_OPERATION code instead of a raw TypeError, mirroring
 * modules/security-android/index.ts (AGENTS.md R7). Status [~] until the Mac runbook passes.
 *
 * R1 note: k1 plaintext never crosses this bridge in the wallet pipeline — vault-core performs
 * unwrap -> sign -> wipe inside one native call. wrapKey/unwrapKey exist for the spec section 2.1
 * surface and for callers that must wrap behind the gate; the caller owns zeroizing any plaintext
 * buffer it receives.
 */

const KEK_KEY = 'nexvault.kek.v1';

const KEK_OPTIONS: SecureStore.SecureStoreOptions = {
  requireAuthentication: true,
  keychainAccessible: SecureStore.WHEN_UNLOCKED_THIS_DEVICE_ONLY,
};

const UNSUPPORTED_CODE = 'E_SEC_UNSUPPORTED_OPERATION';

export type RiskLevel = 'L0' | 'L1' | 'L2' | 'L3' | 'L4' | 'L5';
export type DetectionScope = 'fast' | 'full';

/** Wire shape shared with Android (spec: security-android section 2.2). */
export interface DetectionReport {
  signatureMatch: boolean;
  signatureEvaluated: boolean;
  bundleHashMatch: boolean;
  bundleEvaluated: boolean;
  rootSignals: string[];
  fridaSignals: string[];
  debuggerAttached: boolean;
  evaluatedAt: number;
  riskLevel: RiskLevel;
  /** iOS-only: stable jailbreak vector IDs (JB_PATH_* / JB_DYLD_* / JB_BOOTTIME). */
  jailbreakSignals: string[];
}

export interface KeychainWrapInfo {
  backend: 'KEYCHAIN_BIOMETRIC_ACL';
  aclEnforcedBySecureEnclave: true;
}

declare class SecurityIosNativeModule extends NativeModule<{}> {
  wrapKey(plaintext: Uint8Array, aad: Uint8Array): Promise<Uint8Array>;
  unwrapKey(blob: Uint8Array, aad: Uint8Array): Promise<Uint8Array>;
  runDetection(scope: DetectionScope): Promise<DetectionReport>;
  getCachedRiskLevel(): RiskLevel;
  assertSigningAllowed(): Promise<void>;
}

let cachedNative: SecurityIosNativeModule | null | undefined;

function unsupportedOperationError(operation: string): Error & { code: string } {
  const error = new Error(
    UNSUPPORTED_CODE + ': security-ios ' + operation + ' is unavailable on this platform'
  ) as Error & { code: string };
  error.code = UNSUPPORTED_CODE;
  return error;
}

function requireSecurityModule(operation: string): SecurityIosNativeModule {
  if (cachedNative === undefined) {
    try {
      cachedNative = requireNativeModule<SecurityIosNativeModule>('SecurityIos');
    } catch {
      cachedNative = null;
    }
  }
  if (cachedNative === null) {
    throw unsupportedOperationError(operation);
  }
  return cachedNative;
}

function copyToArrayBuffer(bytes: Uint8Array): ArrayBuffer {
  const copy = new Uint8Array(bytes.byteLength);
  copy.set(bytes);
  return copy.buffer;
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

/**
 * Wraps the provided plaintext behind the biometric KEK. The input view is wiped once the native
 * call settles (R1 transport discipline, same shape as vault-core importMnemonic).
 */
export async function wrapKey(plaintextKey: ArrayBuffer, aad: ArrayBuffer): Promise<ArrayBuffer> {
  const view = new Uint8Array(plaintextKey);
  try {
    const native = requireSecurityModule('wrapKey');
    const sealed = await native.wrapKey(view, new Uint8Array(aad));
    return copyToArrayBuffer(sealed);
  } finally {
    view.fill(0);
  }
}

/**
 * Opens a wrap blob. The returned plaintext is owned by the caller and MUST be zeroed after use;
 * the wallet pipeline never routes k1 through this call (vault-core unwraps natively).
 */
export async function unwrapKey(blob: ArrayBuffer, aad: ArrayBuffer): Promise<ArrayBuffer> {
  const native = requireSecurityModule('unwrapKey');
  const plaintext = await native.unwrapKey(new Uint8Array(blob), new Uint8Array(aad));
  return copyToArrayBuffer(plaintext);
}

/** Runs the detection signals and refreshes the in-memory cache (never persisted; R5). */
export async function runDetection(scope: DetectionScope): Promise<DetectionReport> {
  const native = requireSecurityModule('runDetection');
  return native.runDetection(scope);
}

/** Hot-path cached read (<1ms); L0 before the first scan is a placeholder, not a verdict. */
export function getCachedRiskLevel(): RiskLevel {
  const native = requireSecurityModule('getCachedRiskLevel');
  return native.getCachedRiskLevel();
}

/** Fail-closed gate. Throws E_SEC_GATE_DENIED when the cached/detected risk is L3 or above. */
export async function assertSigningAllowed(): Promise<void> {
  const native = requireSecurityModule('assertSigningAllowed');
  await native.assertSigningAllowed();
}
