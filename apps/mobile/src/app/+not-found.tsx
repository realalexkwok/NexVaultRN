import { Link } from 'expo-router';
import { Pressable, Text } from 'react-native';

import { Screen } from '@/components/Screen';

export default function NotFoundScreen() {
  return (
    <Screen title="Not found">
      <Text className="text-textDim mb-cardPad">This route does not exist.</Text>
      <Link href="/home" asChild>
        <Pressable className="bg-surface rounded-button border border-border py-3 items-center">
          <Text className="text-primary font-semibold">Go to Home</Text>
        </Pressable>
      </Link>
    </Screen>
  );
}
