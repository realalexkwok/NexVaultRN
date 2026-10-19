import { ReactNode } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { tokens } from '@/theme/tokens';

// Shared shell scaffold: dark warm theme, tokens-driven, zero logic.
// `scroll` (default true) wraps content in a ScrollView so large font scales / small
// screens never clip (NIT-4); screens with their own virtualized list (home) pass false.
const contentStyle = {
  paddingHorizontal: tokens.spacing.gutter,
  paddingBottom: tokens.spacing.cardPad,
};

export function Screen({
  title,
  children,
  scroll = true,
}: {
  title: string;
  children: ReactNode;
  scroll?: boolean;
}) {
  const body = (
    <>
      <Text className="text-text text-2xl font-bold mt-cardPad mb-cardPad">{title}</Text>
      {children}
    </>
  );
  return (
    <SafeAreaView className="flex-1 bg-bg">
      {scroll ? (
        <ScrollView className="flex-1" contentContainerStyle={contentStyle} keyboardShouldPersistTaps="handled">
          {body}
        </ScrollView>
      ) : (
        <View className="flex-1" style={contentStyle}>
          {body}
        </View>
      )}
    </SafeAreaView>
  );
}
