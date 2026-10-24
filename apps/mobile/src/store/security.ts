import { create } from 'zustand';

import {
  runDetection,
  type DetectionReport,
  type RiskLevel,
} from '../../../../modules/security-android';

/**
 * T3.2 security scan state (spec: security-android section 2.2; ui-wallet section 2.5).
 *
 * The scan is fired after the first frame (root layout) so the cold-start path never waits for file
 * I/O or a loopback probe. Web has no detection path and is reported as `unsupported` -- never as a
 * fake L0 (R5/R7 honesty).
 *
 * The report is kept in memory only (vector ids only; nothing is persisted — R5).
 */
export type SecurityScanStatus = 'idle' | 'running' | 'ready' | 'unsupported' | 'error';

interface SecurityScanState {
  status: SecurityScanStatus;
  riskLevel: RiskLevel | null;
  report: DetectionReport | null;
  scan: (scope?: 'fast' | 'full') => Promise<void>;
}

export const useSecurityStore = create<SecurityScanState>()((set) => ({
  status: 'idle',
  riskLevel: null,
  report: null,
  scan: async (scope = 'fast') => {
    set({ status: 'running' });
    try {
      const report = await runDetection(scope);
      set({ status: 'ready', riskLevel: report.riskLevel, report });
    } catch (error) {
      const code = (error as { code?: string } | null)?.code;
      set({
        status: code === 'E_SEC_UNSUPPORTED_OPERATION' ? 'unsupported' : 'error',
        riskLevel: null,
        report: null,
      });
    }
  },
}));
