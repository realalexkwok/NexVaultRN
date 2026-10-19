import { useFonts } from 'expo-font';
import { Stack } from 'expo-router';
import Head from 'expo-router/head';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import '../../global.css';
import { tokens } from '@/theme/tokens';

// Root Stack (T2.1 shell + T2.1a visual parity): onboarding (index) / (tabs) / receive / send
// / +not-found. Typed routes are on (app.json experiments.typedRoutes). No vault-core, no wallet
// logic. Inter loads at runtime via expo-font `useFonts` (Android/iOS/web — the expo-font config
// plugin is deliberately not used; see validation.md). The shell renders immediately: gating the
// tree on font load leaves the splash up forever if the asset never settles, so text falls back
// to the system face until Inter resolves (device check 2026-10-19).
export default function RootLayout() {
  const [fontsLoaded] = useFonts({
    'inter-regular': require('../../assets/fonts/inter_regular.ttf'),
    'inter-medium': require('../../assets/fonts/inter_medium.ttf'),
    'inter-semibold': require('../../assets/fonts/inter_semibold.ttf'),
    'inter-bold': require('../../assets/fonts/inter_bold.ttf'),
  });
  return (
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
  );
}
