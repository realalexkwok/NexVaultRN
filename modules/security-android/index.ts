import SecurityAndroidModule, {
  type SecurityLevel,
  type WrapCapabilities,
  type WrapKeyInfo,
} from './src/SecurityAndroidModule';

/**
 * Public JS face for the T3.1 wrap-key submodule (requirements FR-4).
 *
 * On web (and any platform where the native module is unavailable) the stub exposes no functions;
 * this guard turns that into the fixed E_SEC_UNSUPPORTED_OPERATION code instead of a raw
 * TypeError — the same fail-closed pattern as modules/vault-core/index.ts (AGENTS.md R7).
 * wrap/unwrap stay native-internal and reach this surface in T3.2.
 */
const UNSUPPORTED_CODE = 'E_SEC_UNSUPPORTED_OPERATION';

function unsupportedOperationError(operation: string): Error & { code: string } {
  const error = new Error(
    UNSUPPORTED_CODE + ': security-android ' + operation + ' is unavailable on this platform'
  ) as Error & { code: string };
  error.code = UNSUPPORTED_CODE;
  return error;
}

export type { SecurityLevel, WrapCapabilities, WrapKeyInfo };

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
