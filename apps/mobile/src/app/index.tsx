import { Link } from 'expo-router';
import { Pressable, Text, View } from 'react-native';

import { NexVaultButton } from '@/components/NexVaultButton';
import { Screen } from '@/components/Screen';
import { tokens } from '@/theme/tokens';

// Welcome — transcription of WelcomeScreen.kt @3c7487f: centred column with a 120dp shield/lock
// mark, displaySmall wordmark, bodyLarge tagline ("Your keys. Your crypto."), and the two
// bottom-pinned buttons (16dp apart, 48dp bottom spacer). GradientBackground is not ported
// (expo-linear-gradient is not an approved dependency — deviation recorded in validation.md).
// The "Shell preview" link is a demo affordance so the APK is reachable without adb deep links;
// it disappears when the real onboarding flow lands (T4.1, R1 — no mnemonic UI here).
function VaultMark() {
  // WelcomeScreen.kt VaultIcon: 120dp shield (Canvas path, primary) with a 48dp lock (onPrimary).
  return (
    <View className="items-center justify-center" style={{ width: 120, height: 120 }}>
      <View
        style={{
          position: 'absolute',
          top: 4,
          width: 96,
          height: 70,
          borderTopLeftRadius: 48,
          borderTopRightRadius: 48,
          backgroundColor: tokens.color.primary,
        }}
      />
      <View
        style={{
          position: 'absolute',
          top: 74,
          width: 0,
          height: 0,
          borderLeftWidth: 48,
          borderRightWidth: 48,
          borderTopWidth: 42,
          borderLeftColor: 'transparent',
          borderRightColor: 'transparent',
          borderTopColor: tokens.color.primary,
        }}
      />
      <View
        style={{
          position: 'absolute',
          top: 38,
          width: 28,
          height: 20,
          borderTopLeftRadius: 14,
          borderTopRightRadius: 14,
          borderWidth: 4,
          borderColor: tokens.color.onPrimary,
        }}
      />
      <View
        style={{
          position: 'absolute',
          top: 54,
          width: 44,
          height: 30,
          borderRadius: 6,
          backgroundColor: tokens.color.onPrimary,
        }}
      />
    </View>
  );
}

export default function OnboardingScreen() {
  return (
    <Screen gutter="lg">
      <View className="flex-1 items-center">
        <View className="flex-1" />
        <VaultMark />
        <Text className="mt-lg text-display-small font-inter-bold text-text-high">NexVault</Text>
        <Text className="mt-sm text-center text-body-large font-inter-regular text-text-medium">
          Your keys. Your crypto.
        </Text>
        <View className="flex-1" />
        <View className="w-full gap-md">
          <NexVaultButton label="Create New Wallet" disabled />
          <NexVaultButton label="Import Wallet" variant="secondary" disabled />
        </View>
        <View style={{ height: tokens.spacing.xxl }} />
        <Link href="/home" asChild>
          <Pressable accessibilityRole="link">
            <Text className="text-label-medium font-inter-medium text-text-medium">
              Shell preview {'\u2192'}
            </Text>
          </Pressable>
        </Link>
      </View>
    </Screen>
  );
}
