import { Text } from 'react-native';

import { NexVaultCard } from '@/components/NexVaultCard';
import { Screen } from '@/components/Screen';

// DApp browser — original `DApp` tab (MainTab.kt @3c7487f). R5 placeholder: no fake data;
// the real screen lands with its own roadmap item (T4.x).
export default function DappScreen() {
  return (
    <Screen title="DApp">
      <NexVaultCard className="items-center">
        <Text className="text-body-medium font-inter-regular text-text-medium">DApp browser</Text>
        <Text className="mt-sm text-headline-small font-inter-semibold text-text-high">Not configured</Text>
        <Text className="mt-sm text-center text-body-small font-inter-regular text-text-medium">
          Data and actions are not wired yet.
        </Text>
      </NexVaultCard>
    </Screen>
  );
}
