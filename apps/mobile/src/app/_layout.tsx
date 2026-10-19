import '../../global.css';

import { Stack } from 'expo-router';
import Head from 'expo-router/head';
import { StatusBar } from 'expo-status-bar';

import { tokens } from '@/theme/tokens';

// Root Stack (T2.1 shell): onboarding (index) / home / receive / send / +not-found.
// Typed routes are on (app.json experiments.typedRoutes). No vault-core, no wallet logic.
export default function RootLayout() {
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
