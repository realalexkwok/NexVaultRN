import { Text, View } from 'react-native';

import { NexVaultButton } from '@/components/NexVaultButton';
import { Screen } from '@/components/Screen';

// Welcome shell (original WelcomeScreen.kt @3c7487f: wordmark + create/import actions).
// GradientBackground is not ported — it would need expo-linear-gradient, which is not an
// approved dependency; the flat bg token stands in (deviation recorded in validation.md).
export default function OnboardingScreen() {
  return (
    <Screen>
      <View className="flex-1 items-center justify-center">
        <Text className="text-display-large font-inter-bold text-text-high">NexVault</Text>
        <Text className="mt-sm text-center text-body-large font-inter-regular text-text-medium">
          Self-custodial EVM wallet prototype
        </Text>
        <View className="mt-xxl w-full max-w-md gap-md">
          <NexVaultButton label="Create wallet" disabled />
          <NexVaultButton label="Import wallet" variant="secondary" disabled />
          <Text className="text-center text-body-small font-inter-regular text-text-medium">Not configured</Text>
        </View>
      </View>
    </Screen>
  );
}
