import { Link } from 'expo-router';
import { Pressable, Text, View } from 'react-native';

import { Screen } from '@/components/Screen';

// Receive shell: address is derived on-device from vault-core (T4.1), never from route params
// (spec: ui-wallet §2.1). Until wired: honest "Not configured" (R5).
export default function ReceiveScreen() {
  return (
    <Screen title="Receive">
      <View className="bg-surface rounded-card border border-border p-cardPad items-center">
        <Text className="text-textDim">Receive address</Text>
        <Text className="text-text text-xl font-bold mt-cardPad mb-cardPad">Not configured</Text>
        <Text className="text-textDim text-center">
          On-device address derivation is not wired yet.
        </Text>
      </View>
      <Link href="/home" asChild>
        <Pressable className="bg-surface rounded-button border border-border py-3 items-center mt-cardPad">
          <Text className="text-primary font-semibold">Back to Home</Text>
        </Pressable>
      </Link>
    </Screen>
  );
}
