import '../polyfills';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useFonts } from 'expo-font';
import { Stack } from 'expo-router';
import Head from 'expo-router/head';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import '../../global.css';
import { tokens } from '@/theme/tokens';

// TanStack Query client (T2.2 — spec ui-wallet §2.5 homeSkeletonFirst): mounted at module scope so
// the keyed Stack remount below does not recycle it. `structuralSharing: false` is defensive: the
// balance result is a one-shot { wei, blockNumber } record, so skipping the default
// replaceEqualDeep pass avoids deep-compare work (v5 never JSON-serializes the cache and compares
// primitives with ===, so bigint is safe either way — corrects the earlier rationale, review N1).
// Global defaults stay minimal on purpose: retry/refetchInterval belong to the query that wants
// them (the balance query sets both in home.tsx), so unrelated future queries do not inherit an
// RPC polling policy (review MINOR-4). First frame never awaits an RPC: the balance query is
// mounted by the home screen after render.
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      structuralSharing: false,
    },
  },
});

// Root Stack (T2.1 shell + T2.1a visual parity): onboarding (index) / (tabs) / receive / send
// / +not-found. Typed routes are on (app.json experiments.typedRoutes). No vault-core, no wallet
// logic. Inter ships two ways: embedded at build time by the expo-font config plugin (app.json —
// Android xml families inter-regular…, iOS TTFs, so the first frame measures with Inter) and
// loaded at runtime via `useFonts` for web. The remount below re-lays-out text once fonts settle
// (review M1/R2 — first paint must not be measured against the fallback face).
export default function RootLayout() {
  const [fontsLoaded] = useFonts({
    'inter-regular': require('../../assets/fonts/inter_regular.ttf'),
    'inter-medium': require('../../assets/fonts/inter_medium.ttf'),
    'inter-semibold': require('../../assets/fonts/inter_semibold.ttf'),
    'inter-bold': require('../../assets/fonts/inter_bold.ttf'),
  });
  return (
    <QueryClientProvider client={queryClient}>
      <SafeAreaProvider>
        <Head>
          <title>NexVault</title>
          <meta name="theme-color" content={tokens.color.bg} />
        </Head>
        <StatusBar style="light" />
        {/* Keyed remount once Inter resolves: the first frame is measured with the fallback face and
            RN does not re-measure text after a font swap → clipped last words (review M1, 2026-10-19). */}
        <Stack
          key={fontsLoaded ? 'fonts-ready' : 'fonts-pending'}
          screenOptions={{
            headerShown: false,
            contentStyle: { backgroundColor: tokens.color.bg },
          }}
        />
      </SafeAreaProvider>
    </QueryClientProvider>
  );
}
