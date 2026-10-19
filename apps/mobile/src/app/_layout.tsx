import { useFonts } from 'expo-font';
import { Stack } from 'expo-router';
import Head from 'expo-router/head';
import { StatusBar } from 'expo-status-bar';

import '../../global.css';
import { tokens } from '@/theme/tokens';

// Root Stack (T2.1 shell + T2.1a visual parity): onboarding (index) / (tabs) / receive / send
// / +not-found. Typed routes are on (app.json experiments.typedRoutes). No vault-core, no wallet
// logic. Inter loads at runtime via expo-font `useFonts` (Android/iOS/web — the expo-font config
// plugin is deliberately not used; see validation.md).
export default function RootLayout() {
  const [fontsLoaded, fontError] = useFonts({
    'inter-regular': require('../../assets/fonts/inter_regular.ttf'),
    'inter-medium': require('../../assets/fonts/inter_medium.ttf'),
    'inter-semibold': require('../../assets/fonts/inter_semibold.ttf'),
    'inter-bold': require('../../assets/fonts/inter_bold.ttf'),
  });
  if (!fontsLoaded && !fontError) return null;
  return (
    <>
      <Head>
        <title>NexVault</title>
        <meta name="theme-color" content={tokens.color.bg} />
      </Head>
      <StatusBar style="light" />
      <Stack
        screenOptions={{
          headerShown: false,
          contentStyle: { backgroundColor: tokens.color.bg },
        }}
      />
    </>
  );
}
