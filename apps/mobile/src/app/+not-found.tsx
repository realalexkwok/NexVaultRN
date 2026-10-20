import { router } from 'expo-router';
import { Text } from 'react-native';

import { NexVaultButton } from '@/components/NexVaultButton';
import { Screen } from '@/components/Screen';

export default function NotFoundScreen() {
  return (
    <Screen title="Not found">
      <Text className="mb-md text-body-medium font-inter-regular text-text-medium">This route does not exist.</Text>
      <NexVaultButton label="Go to Home" onPress={() => router.replace('/home')} />
    </Screen>
  );
}
