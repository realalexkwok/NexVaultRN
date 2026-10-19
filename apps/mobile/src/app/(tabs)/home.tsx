import { FlashList } from '@shopify/flash-list';
import { router } from 'expo-router';
import { Text, View } from 'react-native';

import { NexVaultButton } from '@/components/NexVaultButton';
import { NexVaultCard } from '@/components/NexVaultCard';
import { Screen } from '@/components/Screen';

// Home (original HomeScreen.kt @3c7487f): no top bar — the chain-selector row leads; portfolio
// card, chart area, token list, quick actions. Data stays R5 "Not configured" until T2.2/T4.x.
const PLACEHOLDER_ROWS = [
  { id: 'tokens', label: 'Tokens' },
  { id: 'nfts', label: 'NFTs' },
  { id: 'activity', label: 'Activity' },
] as const;

export default function HomeScreen() {
  return (
    <Screen scroll={false}>
      <View className="h-14 flex-row items-center justify-between">
        <Text className="text-title-large font-inter-semibold text-text-high">NexVault</Text>
        <View className="flex-row items-center rounded-chip border border-outline-variant bg-surface px-md py-xs">
          <Text className="text-label-medium font-inter-medium text-text-medium">Chain · Not configured</Text>
        </View>
      </View>
      <View className="flex-1">
        <FlashList
          data={PLACEHOLDER_ROWS}
          keyExtractor={(row) => row.id}
          ListHeaderComponent={
            <View className="gap-md pb-md">
              <NexVaultCard>
                <Text className="text-body-medium font-inter-regular text-text-medium">Total balance</Text>
                <Text className="mt-sm text-display-small font-inter-bold text-text-high">Not configured</Text>
              </NexVaultCard>
              <View className="h-chart items-center justify-center rounded-card border border-card-border bg-card-surface">
                <Text className="text-body-small font-inter-regular text-text-medium">Price chart · Not configured</Text>
              </View>
            </View>
          }
          renderItem={({ item }) => (
            <NexVaultCard className="mb-sm flex-row items-center justify-between">
              <Text className="text-body-large font-inter-medium text-text-high">{item.label}</Text>
              <Text className="text-body-medium font-inter-regular text-text-medium">Not configured</Text>
            </NexVaultCard>
          )}
          showsVerticalScrollIndicator={false}
        />
      </View>
      <View className="flex-row gap-md pt-md">
        <View className="flex-1">
          <NexVaultButton label="Receive" variant="secondary" onPress={() => router.push('/receive')} />
        </View>
        <View className="flex-1">
          <NexVaultButton label="Send" onPress={() => router.push('/send')} />
        </View>
      </View>
    </Screen>
  );
}
