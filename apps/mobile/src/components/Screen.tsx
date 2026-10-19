import { ReactNode } from 'react';
import { Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { tokens } from '@/theme/tokens';

// Shared shell scaffold: dark warm theme, tokens-driven, zero logic.
export function Screen({ title, children }: { title: string; children: ReactNode }) {
  return (
    <SafeAreaView className="flex-1 bg-bg">
      <View className="flex-1" style={{ paddingHorizontal: tokens.spacing.gutter }}>
        <Text className="text-text text-2xl font-bold mt-cardPad mb-cardPad">{title}</Text>
        {children}
      </View>
    </SafeAreaView>
  );
}
