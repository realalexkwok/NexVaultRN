import { Text } from 'react-native';

import { NexVaultCard } from '@/components/NexVaultCard';
import { Screen } from '@/components/Screen';

// Receive shell: the address is derived on-device from vault-core (T4.1), never from route
// params (spec: ui-wallet §2.1). Until wired: honest "Not configured" (R5).
export default function ReceiveScreen() {
  return (
    <Screen title="Receive" showBack>
      <NexVaultCard className="items-center">
        <Text className="text-body-medium font-inter-regular text-text-medium">Receive address</Text>
        <Text className="mt-sm text-headline-small font-inter-semibold text-text-high">Not configured</Text>
        <Text className="mt-sm text-center text-body-small font-inter-regular text-text-medium">
          On-device address derivation is not wired yet.
        </Text>
      </NexVaultCard>
    </Screen>
  );
}
