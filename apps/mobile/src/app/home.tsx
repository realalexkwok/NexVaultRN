import { FlashList } from '@shopify/flash-list';
import { Link } from 'expo-router';
import { Pressable, Text, View } from 'react-native';

import { Screen } from '@/components/Screen';

// Home shell: balance card honestly "Not configured" (R5 — no fake data),
// FlashList v2 with static placeholder rows. Real data arrives with T2.2/T4.x.
const PLACEHOLDER_ROWS = [
  { id: 'tokens', label: 'Tokens' },
  { id: 'nfts', label: 'NFTs' },
  { id: 'activity', label: 'Activity' },
] as const;

export default function HomeScreen() {
  return (
    <Screen title="Home">
      <View className="bg-surface rounded-card border border-border p-cardPad mb-cardPad">
        <Text className="text-textDim">Balance</Text>
        <Text className="text-text text-xl font-bold mt-cardPad">Not configured</Text>
      </View>
      <View className="flex-1">
        <FlashList
          data={PLACEHOLDER_ROWS}
          keyExtractor={(row) => row.id}
          renderItem={({ item }) => (
            <View className="bg-surface rounded-card border border-border p-cardPad mb-cardPad flex-row justify-between">
              <Text className="text-text">{item.label}</Text>
              <Text className="text-textDim">Not configured</Text>
            </View>
          )}
          showsVerticalScrollIndicator={false}
        />
      </View>
      <View className="flex-row gap-cardPad mt-cardPad">
        <Link href="/receive" asChild>
          <Pressable className="flex-1 bg-surface rounded-button border border-border py-3 items-center">
            <Text className="text-primary font-semibold">Receive</Text>
          </Pressable>
        </Link>
        <Link href="/send" asChild>
          <Pressable className="flex-1 bg-surface rounded-button border border-border py-3 items-center">
            <Text className="text-primary font-semibold">Send</Text>
          </Pressable>
        </Link>
      </View>
    </Screen>
  );
}
