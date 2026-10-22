import { useQuery } from '@tanstack/react-query';
import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, Text, View } from 'react-native';
import {
  formatNativeBalance,
  getChainConfig,
  getLatestBlockNumber,
  getNativeBalance,
  normalizeAddress,
  SUPPORTED_CHAINS,
} from 'web3-adapter';

import { NexVaultTextField } from '@/components/NexVaultTextField';
import { Screen } from '@/components/Screen';
import { useObserverStore } from '@/store/observer';
import { tokens } from '@/theme/tokens';

// Home — original HomeScreen.kt @3c7487f layout:
//   ChainSelectorHeader (dropdown) → PortfolioBalanceSection (label / value / 24h change, centred)
//   → PortfolioChartSection (180dp card + 1D/7D/1M/3M/ALL chips) → QuickActionsRow (Send/Receive/Swap)
//   → Tokens header ("Tokens" left, "+ Add Token" right) → token list.
// T2.2 adds the read-only data path (spec web3-adapter §2.2/§2.5): chain pill taps toggle
// mainnet <-> sepolia (deviation D-j, owner Q3-A) and a watch-address field commits a public
// address into the Zustand observer store (D-i, owner Q1-A). The balance query is async and
// skeleton-first (ui-wallet §2.5 homeSkeletonFirst) — the first frame never awaits an RPC.
// No address => the honest "Not configured" placeholder (R5, no fake balances). Chart/interval
// chips stay placeholders: price history is T4.x.
const INTERVALS = ['1D', '7D', '1M', '3M', 'ALL'] as const;

const QUICK_ACTIONS = [
  { label: 'Send', glyph: '\u2191', onPress: () => router.push('/send') },
  { label: 'Receive', glyph: '\u2193', onPress: () => router.push('/receive') },
  { label: 'Swap', glyph: '\u21c4', onPress: undefined },
] as const;

export default function HomeScreen() {
  const selectedChainId = useObserverStore((state) => state.selectedChainId);
  const setSelectedChain = useObserverStore((state) => state.setSelectedChain);
  const watchedAddress = useObserverStore((state) => state.watchedAddresses[0] ?? null);
  const setWatchedAddress = useObserverStore((state) => state.setWatchedAddress);

  const [input, setInput] = useState(watchedAddress ?? '');
  const [inputError, setInputError] = useState<string | undefined>();
  const chain = getChainConfig(selectedChainId);

  const balanceQuery = useQuery({
    // Public data only (address + numeric chainId); safe to poll (R5). Retry/polling are scoped
    // to this query instead of global defaults (review MINOR-4); structuralSharing:false is a
    // defensive client setting — v5 never JSON-serializes the cache (O-6/N1).
    queryKey: ['native-balance', selectedChainId, watchedAddress],
    enabled: watchedAddress !== null,
    retry: 2,
    refetchInterval: 30_000,
    refetchIntervalInBackground: false,
    queryFn: async () => {
      const [wei, blockNumber] = await Promise.all([
        getNativeBalance(watchedAddress as `0x${string}`, selectedChainId),
        getLatestBlockNumber(selectedChainId),
      ]);
      return { wei, blockNumber };
    },
  });

  const toggleChain = () => {
    const index = SUPPORTED_CHAINS.findIndex((candidate) => candidate.chainId === selectedChainId);
    const next = SUPPORTED_CHAINS[(index + 1) % SUPPORTED_CHAINS.length];
    // Query key carries chainId — the switch re-queries and never displays cross-chain data.
    setSelectedChain(next.chainId);
  };

  const commitAddress = () => {
    const trimmed = input.trim();
    if (trimmed.length === 0) {
      // Empty field = stop watching; the balance section returns to "Not configured" (R5).
      setInputError(undefined);
      setWatchedAddress(null);
      return;
    }
    const normalized = normalizeAddress(trimmed);
    if (!normalized) {
      // Invalid input clears the committed watch and shows the inline error (R5: no fake data).
      // Guarantee is commit-time only: while the user edits an invalid fragment the previous
      // balance stays visible until blur/submit clears it (O-7 / review NIT-2).
      setInputError('Invalid address');
      setWatchedAddress(null);
      return;
    }
    setInputError(undefined);
    setWatchedAddress(normalized);
  };

  const showBalance = watchedAddress !== null;
  const data = balanceQuery.data;

  return (
    <Screen>
      {/* ChainSelectorHeader — original renders ChainSelectorDropdown when a chain is set. */}
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={`Chain ${chain.name}. Activate to switch chain`}
        onPress={toggleChain}
        className="mb-sm flex-row items-center gap-xs self-start rounded-chip border border-outline-variant bg-surface px-md py-xs"
      >
        <Text className="text-body-medium font-inter-medium text-text-high">Chain · {chain.name}</Text>
        <Text className="text-body-small font-inter-regular text-text-medium">{'\u25be'}</Text>
      </Pressable>

      {/* Watch-address field (T2.2 D-i; Q1-A) — the original app derives the address from the
          wallet, the read-only observer takes it as input. Commit on blur/submit. */}
      <View className="mb-sm">
        <NexVaultTextField
          label="Watch address"
          placeholder="0x…"
          value={input}
          onChangeText={(value) => {
            setInput(value);
            if (inputError) setInputError(undefined);
            // Commit as soon as the text parses (N3); while editing, an invalid fragment leaves
            // the previous committed address in place until blur/submit (O-7 / NIT-2). Empty text
            // clears the watch.
            const trimmed = value.trim();
            if (trimmed.length === 0) {
              setWatchedAddress(null);
            } else {
              const normalized = normalizeAddress(trimmed);
              if (normalized) setWatchedAddress(normalized);
            }
          }}
          onBlur={commitAddress}
          onSubmitEditing={commitAddress}
          autoCapitalize="none"
          autoCorrect={false}
          spellCheck={false}
          accessibilityLabel="Watch address"
          accessibilityHint="Enter a public EVM address to watch on the selected chain"
          error={inputError}
        />
      </View>

      {/* PortfolioBalanceSection (centred) — four states: not-configured / skeleton / error / data. */}
      <View className="items-center py-sm">
        <Text className="text-body-medium font-inter-regular text-text-medium">Total Balance</Text>
        {!showBalance ? (
          <>
            <Text className="mt-xs text-headline-large font-inter-bold text-text-high">Not configured</Text>
            <Text className="mt-xs text-body-medium font-inter-regular text-text-medium">{'\u2014'} (24h)</Text>
          </>
        ) : balanceQuery.isLoading ? (
          <>
            <View className="mt-xs h-9 w-40 rounded-sm bg-shimmer" />
            <View className="mt-xs h-4 w-32 rounded-sm bg-shimmer" />
          </>
        ) : data ? (
          <>
            <Text className="mt-xs text-headline-large font-inter-bold text-text-high">
              {formatNativeBalance(data.wei, chain.nativeCurrency.decimals)} {chain.nativeCurrency.symbol}
            </Text>
            <Text className="mt-xs text-body-medium font-inter-regular text-text-medium">
              {chain.name} · Block #{data.blockNumber.toString()}
            </Text>
          </>
        ) : (
          <>
            <Text className="mt-xs text-headline-large font-inter-bold text-text-high">RPC unavailable</Text>
            <Pressable
              accessibilityRole="button"
              onPress={() => balanceQuery.refetch()}
              className="mt-sm rounded-chip border border-outline-variant px-md py-xs"
            >
              <Text className="text-label-large font-inter-medium text-primary">Retry</Text>
            </Pressable>
          </>
        )}
      </View>

      {/* PortfolioChartSection — flat 180dp card + interval chips (FilterChip in the original).
          Price history is T4.x; the card stays an honest placeholder. */}
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

      {/* Token list — four states (review N4): no address = "Not configured" (R5, the only place
          that string may appear), loading = skeleton row, error = em dash, data = one native-coin
          row (Q2-A). */}
      {!showBalance ? (
        <View className="items-center py-xxl">
          <Text className="text-body-large font-inter-regular text-text-medium">Not configured</Text>
        </View>
      ) : balanceQuery.isLoading ? (
        <View className="flex-row items-center gap-sm rounded-card border border-card-border bg-card-surface px-md py-sm">
          <View className="h-token w-token rounded-full bg-shimmer" />
          <View className="flex-1">
            <View className="h-4 w-24 rounded-sm bg-shimmer" />
            <View className="mt-xs h-3 w-10 rounded-sm bg-shimmer" />
          </View>
          <View className="h-4 w-16 rounded-sm bg-shimmer" />
        </View>
      ) : data ? (
        <View className="flex-row items-center gap-sm rounded-card border border-card-border bg-card-surface px-md py-sm">
          <View className="h-token w-token items-center justify-center rounded-full bg-secondary-container">
            <Text className="text-title-medium font-inter-bold text-on-secondary-container">{'\u039e'}</Text>
          </View>
          <View className="flex-1">
            <Text className="text-title-small font-inter-semibold text-text-high">{chain.name}</Text>
            <Text className="text-body-small font-inter-regular text-text-medium">
              {chain.nativeCurrency.symbol}
            </Text>
          </View>
          <Text className="text-title-small font-inter-semibold text-text-high">
            {formatNativeBalance(data.wei, chain.nativeCurrency.decimals)}
          </Text>
        </View>
      ) : (
        <View className="items-center py-xxl">
          <Text className="text-body-large font-inter-regular text-text-medium">{'\u2014'}</Text>
        </View>
      )}
    </Screen>
  );
}
