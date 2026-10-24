import { NativeModule, requireNativeModule } from 'expo';

/**
 * T3.2 JS face (spec: security-android sections 2.1 / 2.2). Detection reports carry vector ids
 * only (R5); booleans stay non-null and each self-check carries an explicit evaluated flag so
 * "not evaluated" (debug build without an embedded bundle) is never read as "passed" (R6).
 * No key-semantic identifiers may appear here (AGENTS.md R1) and plaintext buffers are only ever
 * ArrayBuffer/Uint8Array transports (never strings).
 */
export type SecurityLevel = 'STRONGBOX' | 'TRUSTED_ENVIRONMENT' | 'SOFTWARE_DEBUG';

export type RiskLevel = 'L0' | 'L1' | 'L2' | 'L3' | 'L4' | 'L5';

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

/**
 * Detection pass result (T3.2 subset; full L0-L5 attestation matrix lands with T5.x).
 *
 * A check that could not be evaluated reports `false` **and** `evaluated: false` -- never a
 * false "match" (R6). `scope: 'fast'` means root/Frida/anti-debug were not checked, so their
 * empty/false values are "not checked", not "clean".
 */
export interface DetectionReport {
  signatureMatch: boolean;
  signatureEvaluated: boolean;
  bundleHashMatch: boolean;
  bundleEvaluated: boolean;
  rootSignals: string[];
  fridaSignals: string[];
  debuggerAttached: boolean;
  scope: 'fast' | 'full';
  evaluatedAt: number;
  riskLevel: RiskLevel;
}

declare class SecurityAndroidModule extends NativeModule<{}> {
  getCapabilities(): Promise<WrapCapabilities>;
  ensureWrapKey(): Promise<WrapKeyInfo>;
  deleteWrapKey(): Promise<void>;
  runDetection(scope: 'fast' | 'full'): Promise<DetectionReport>;
  getCachedRiskLevel(): RiskLevel;
  assertSigningAllowed(): Promise<void>;
  wrapKey(plaintext: Uint8Array, aad: Uint8Array): Promise<Uint8Array>;
  unwrapKey(blob: Uint8Array, aad: Uint8Array): Promise<Uint8Array>;
}

export default requireNativeModule<SecurityAndroidModule>('SecurityAndroid');
