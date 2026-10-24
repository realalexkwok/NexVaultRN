import { Text, View } from 'react-native';

import { NexVaultCard } from '@/components/NexVaultCard';
import { Screen } from '@/components/Screen';
import { useSecurityStore } from '@/store/security';

/**
 * Security settings — original `Settings` tab (MainTab.kt @3c7487f) plus the T3.2 readout.
 *
 * Honest readout only (R5/R6): the risk level and the self-check evaluated/match flags. Detection
 * vector ids stay out of the UI — a visible "root detected" hint would tell an attacker exactly
 * what fired (spec TC-SA-006: adversarial silence); the gate, not the screen, is the enforcement.
 */
export default function SettingsScreen() {
  const status = useSecurityStore((state) => state.status);
  const riskLevel = useSecurityStore((state) => state.riskLevel);
  const report = useSecurityStore((state) => state.report);

  const headline =
    status === 'ready' && riskLevel
      ? riskLevel
      : status === 'unsupported'
        ? 'Not supported on web'
        : status === 'error'
          ? 'Unavailable'
          : 'Scanning…';

  return (
    <Screen title="Settings">
      <NexVaultCard className="items-center">
        <Text className="text-body-medium font-inter-regular text-text-medium">Security level</Text>
        <Text className="mt-sm text-headline-small font-inter-semibold text-text-high">{headline}</Text>
        <Text className="mt-sm text-center text-body-small font-inter-regular text-text-medium">
          {status === 'ready' && report
            ? 'Signature check: ' +
              (report.signatureEvaluated ? (report.signatureMatch ? 'match' : 'mismatch') : 'not evaluated') +
              ' · Bundle check: ' +
              (report.bundleEvaluated ? (report.bundleHashMatch ? 'match' : 'mismatch') : 'not evaluated')
            : status === 'unsupported'
              ? 'Detection runs on Android and iOS builds only.'
              : 'Detection signals are collected after the first frame.'}
        </Text>
        <Text className="mt-sm text-center text-body-small font-inter-regular text-text-medium">
          Detection is [C] (cost-raiser). The fail-closed signing gate is [P] once device-verified.
        </Text>
      </NexVaultCard>
    </Screen>
  );
}
