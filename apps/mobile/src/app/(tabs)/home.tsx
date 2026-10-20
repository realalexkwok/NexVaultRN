import { router } from 'expo-router';
import { Pressable, Text, View } from 'react-native';

import { Screen } from '@/components/Screen';
import { tokens } from '@/theme/tokens';

// Home — original HomeScreen.kt @3c7487f layout:
//   ChainSelectorHeader (dropdown) → PortfolioBalanceSection (label / value / 24h change, centred)
//   → PortfolioChartSection (180dp card + 1D/7D/1M/3M/ALL chips) → QuickActionsRow (Send/Receive/Swap)
//   → Tokens header ("Tokens" left, "+ Add Token" right) → token list / empty state.
// There is no data layer yet (T2.2/T4.x), so every value is an honest "Not configured" placeholder
// (R5 — no fake balances). The list is a static empty state, no virtualized list needed.
const INTERVALS = ['1D', '7D', '1M', '3M', 'ALL'] as const;

const QUICK_ACTIONS = [
  { label: 'Send', glyph: '\u2191', onPress: () => router.push('/send') },
  { label: 'Receive', glyph: '\u2193', onPress: () => router.push('/receive') },
  { label: 'Swap', glyph: '\u21c4', onPress: undefined },
] as const;

export default function HomeScreen() {
  return (
    <Screen>
      {/* ChainSelectorHeader — original renders ChainSelectorDropdown only when a chain is set */}
      <View className="mb-sm flex-row items-center gap-xs self-start rounded-chip border border-outline-variant bg-surface px-md py-xs">
        <Text className="text-body-medium font-inter-medium text-text-high">Chain · Not configured</Text>
        <Text className="text-body-small font-inter-regular text-text-medium">{'\u25be'}</Text>
      </View>

      {/* PortfolioBalanceSection (centred) */}
      <View className="items-center py-sm">
        <Text className="text-body-medium font-inter-regular text-text-medium">Total Balance</Text>
        <Text className="mt-xs text-headline-large font-inter-bold text-text-high">Not configured</Text>
        <Text className="mt-xs text-body-medium font-inter-regular text-text-medium">{'\u2014'} (24h)</Text>
      </View>

      {/* PortfolioChartSection — flat 180dp card + interval chips (FilterChip in the original) */}
      <View className="h-chart items-center justify-center rounded-card border border-card-border bg-card-surface">
        <Text className="text-body-small font-inter-regular text-text-medium">Not configured</Text>
      </View>
      <View className="mt-sm flex-row justify-evenly">
        {INTERVALS.map((label) => {
          // FilterChip(selected = days == selectedDays); default range is 7D (HomeUiState)
          const selected = label === '7D';
          return (
            <View
              key={label}
              className={`rounded-chip border px-md py-s6 ${selected ? 'border-secondary-container bg-secondary-container' : 'border-outline-variant'}`}
            >
              <Text
                className={`text-label-large font-inter-medium ${selected ? 'text-on-secondary-container' : 'text-text-medium'}`}
              >
                {label}
              </Text>
            </View>
          );
        })}
      </View>

      {/* QuickActionsRow — FilledTonalIconButton equivalents (secondaryContainer circle + label) */}
      <View className="mt-lg flex-row justify-evenly">
        {QUICK_ACTIONS.map((action) => (
          <View key={action.label} className="items-center gap-xs">
            <Pressable
              accessibilityRole="button"
              accessibilityState={{ disabled: action.onPress === undefined }}
              disabled={action.onPress === undefined}
              onPress={action.onPress}
              hitSlop={tokens.spacing.sm}
              className="h-action w-action items-center justify-center rounded-full bg-secondary-container"
            >
              <Text className="text-headline-small font-inter-semibold text-on-secondary-container">{action.glyph}</Text>
            </Pressable>
            <Text className="text-label-small font-inter-medium text-on-surface-variant">{action.label}</Text>
          </View>
        ))}
      </View>

      {/* Tokens header — titleMedium bold left, TextButton primary right */}
      <View className="mt-lg flex-row items-center justify-between py-sm">
        <Text className="text-title-medium font-inter-bold text-text-high">Tokens</Text>
        <Text className="text-label-large font-inter-medium text-primary">+ Add Token</Text>
      </View>

      {/* Token list — honest empty state until T2.2/T4.x wires data */}
      <View className="items-center py-xxl">
        <Text className="text-body-large font-inter-regular text-text-medium">Not configured</Text>
      </View>
    </Screen>
  );
}
