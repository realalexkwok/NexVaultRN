import { Link } from 'expo-router';
import { Pressable, Text, View } from 'react-native';

import { Screen } from '@/components/Screen';

// Send shell: disabled form stub. Transaction construction / native confirm / signDigest
// are T4.x (spec: ui-wallet §2.2). Honest "Not configured" (R5), no fake flow.
export default function SendScreen() {
  return (
    <Screen title="Send">
      <View className="bg-surface rounded-card border border-border p-cardPad gap-cardPad">
        <View>
          <Text className="text-textDim mb-cardPad">To address</Text>
          <View className="bg-surfaceHi rounded-button p-3">
            <Text className="text-textDim">Not configured</Text>
          </View>
        </View>
        <View>
          <Text className="text-textDim mb-cardPad">Amount</Text>
          <View className="bg-surfaceHi rounded-button p-3">
            <Text className="text-textDim">Not configured</Text>
          </View>
        </View>
        <Pressable
          disabled
          accessibilityRole="button"
          accessibilityState={{ disabled: true }}
          className="bg-surfaceHi rounded-button py-3 items-center opacity-50"
        >
          <Text className="text-textDim font-semibold">Continue</Text>
        </Pressable>
      </View>
      <Link href="/home" asChild>
        <Pressable className="bg-surface rounded-button border border-border py-3 items-center mt-cardPad">
          <Text className="text-primary font-semibold">Back to Home</Text>
        </Pressable>
      </Link>
    </Screen>
  );
}
