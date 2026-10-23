import { NativeModule, requireNativeModule } from 'expo';

/**
 * T3.1 minimal JS face (plan requirements FR-4; spec: security-android section 2.1 trimmed).
 * wrap/unwrap stay native-internal; the ArrayBuffer face lands in T3.2 together with the
 * vault-core coupling. No key-semantic identifiers may appear here (AGENTS.md R1).
 */
export type SecurityLevel = 'STRONGBOX' | 'TRUSTED_ENVIRONMENT' | 'SOFTWARE_DEBUG';

export interface WrapKeyInfo {
  alias: string;
  securityLevel: SecurityLevel;
  strongBoxBacked: boolean;
}

export interface WrapCapabilities {
  hasStrongBox: boolean;
  isDeviceSecure: boolean;
  securityLevel: SecurityLevel | null;
}

declare class SecurityAndroidModule extends NativeModule<{}> {
  getCapabilities(): Promise<WrapCapabilities>;
  ensureWrapKey(): Promise<WrapKeyInfo>;
  deleteWrapKey(): Promise<void>;
}

export default requireNativeModule<SecurityAndroidModule>('SecurityAndroid');
