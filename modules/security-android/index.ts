import SecurityAndroidModule, {
  type DetectionReport,
  type RiskLevel,
  type SecurityLevel,
  type WrapCapabilities,
  type WrapKeyInfo,
} from './src/SecurityAndroidModule';

/**
 * Public JS face for the security-android module (T3.1 wrap-key + T3.2 detection/gate).
 *
 * On web (and any platform where the native module is unavailable) the stub exposes no functions;
 * this guard turns that into the fixed E_SEC_UNSUPPORTED_OPERATION code instead of a raw
 * TypeError -- the same fail-closed pattern as modules/vault-core/index.ts (AGENTS.md R7).
 *
 * T3.2 additions: detection report, cached risk level (sync hot path), the fail-closed gate and the
 * ArrayBuffer wrap/unwrap surface (Handoff H-T3.1-2). wrap/unwrap wipe the JS input view after the
 * call (R1); vault-core's signing path still unwraps natively, so plaintext never persists in JS.
 */
const UNSUPPORTED_CODE = 'E_SEC_UNSUPPORTED_OPERATION';

function unsupportedOperationError(operation: string): Error & { code: string } {
  const error = new Error(
    UNSUPPORTED_CODE + ': security-android ' + operation + ' is unavailable on this platform'
  ) as Error & { code: string };
  error.code = UNSUPPORTED_CODE;
  return error;
}

export type { DetectionReport, RiskLevel, SecurityLevel, WrapCapabilities, WrapKeyInfo };

export async function getCapabilities(): Promise<WrapCapabilities> {
  if (typeof SecurityAndroidModule.getCapabilities !== 'function') {
    throw unsupportedOperationError('getCapabilities');
  }
  return SecurityAndroidModule.getCapabilities();
}

export async function ensureWrapKey(): Promise<WrapKeyInfo> {
  if (typeof SecurityAndroidModule.ensureWrapKey !== 'function') {
    throw unsupportedOperationError('ensureWrapKey');
  }
  return SecurityAndroidModule.ensureWrapKey();
}

export async function deleteWrapKey(): Promise<void> {
  if (typeof SecurityAndroidModule.deleteWrapKey !== 'function') {
    throw unsupportedOperationError('deleteWrapKey');
  }
  return SecurityAndroidModule.deleteWrapKey();
}

/** Runs a detection pass ('fast' = signature + bundle, 'full' = + root/frida/anti-debug). */
export async function runDetection(scope: 'fast' | 'full'): Promise<DetectionReport> {
  if (typeof SecurityAndroidModule.runDetection !== 'function') {
    throw unsupportedOperationError('runDetection');
  }
  return SecurityAndroidModule.runDetection(scope);
}

/** Cached risk level (synchronous, <1ms contract); callers on web must handle the fixed error. */
export function getCachedRiskLevel(): RiskLevel {
  if (typeof SecurityAndroidModule.getCachedRiskLevel !== 'function') {
    throw unsupportedOperationError('getCachedRiskLevel');
  }
  return SecurityAndroidModule.getCachedRiskLevel();
}

/** fail-closed gate mirror for UI callers; vault-core enforces the same gate natively. */
export async function assertSigningAllowed(): Promise<void> {
  if (typeof SecurityAndroidModule.assertSigningAllowed !== 'function') {
    throw unsupportedOperationError('assertSigningAllowed');
  }
  return SecurityAndroidModule.assertSigningAllowed();
}

/**
 * Wraps plaintext under the biometric-bound Keystore key (spec section 2.1). The JS input view is
 * wiped after the call on every path (R1); the returned blob is ciphertext only.
 */
export async function wrapKey(plaintext: ArrayBuffer, aad: ArrayBuffer): Promise<ArrayBuffer> {
  if (typeof SecurityAndroidModule.wrapKey !== 'function') {
    throw unsupportedOperationError('wrapKey');
  }
  const view = new Uint8Array(plaintext);
  try {
    const result = await SecurityAndroidModule.wrapKey(view, new Uint8Array(aad));
    const copy = new Uint8Array(result.byteLength);
    copy.set(result);
    return copy.buffer;
  } finally {
    view.fill(0);
  }
}

/** Unwraps a T3.1 blob; the caller owns the plaintext buffer and must wipe it (R1). */
export async function unwrapKey(blob: ArrayBuffer, aad: ArrayBuffer): Promise<ArrayBuffer> {
  if (typeof SecurityAndroidModule.unwrapKey !== 'function') {
    throw unsupportedOperationError('unwrapKey');
  }
  const result = await SecurityAndroidModule.unwrapKey(new Uint8Array(blob), new Uint8Array(aad));
  const copy = new Uint8Array(result.byteLength);
  copy.set(result);
  return copy.buffer;
}
