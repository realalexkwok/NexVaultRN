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
// it disappears when the real onboarding flow lands (T4.1, R1 — no key-material UI here).
function VaultMark() {
  // WelcomeScreen.kt VaultIcon: 120dp shield + 48dp lock, drawn there with Canvas. The shield path
  // is the hexagon (0.5w,0) → (w,0.25h) → (0.85w,0.75h) → (0.5w,h) → (0.15w,0.75h) → (0,0.25h);
  // here it is composed from plain Views (triangles + rect) — no drawing dependency is approved.
  return (
    <View className="items-center justify-center" style={{ width: 120, height: 120 }}>
      {/* top facet: apex (60,0) → shoulders (0/120, 30) */}
      <View
        style={{
          position: 'absolute',
          top: 0,
          width: 0,
          height: 0,
          borderLeftWidth: 60,
          borderRightWidth: 60,
          borderBottomWidth: 30,
          borderLeftColor: 'transparent',
          borderRightColor: 'transparent',
          borderBottomColor: tokens.color.primary,
        }}
      />
      {/* joint strip: the top facet ends at y=30 and band 0 starts there; RN frame rounding at
          2.625 px/dp leaves a 1px background line. A 1dp strip at 29.5 bridges it; width 118
          follows the facet (path ≈118 at 29.5) so the shoulders are not widened (round-6 N1). */}
      <View style={{ position: 'absolute', top: 29.5, left: 1, width: 118, height: 1, backgroundColor: tokens.color.primary }} />
      {/* body: the path tapers 120 → 84 (0.70w) between 25%h and 75%h; approximated with twenty
          4dp bands (1dp overlap so band-to-band rounding cannot leave seams). The silhouette is a
          20-step staircase (~0.9dp = 2.4px per side per band at 2.625x) — recorded, not "smooth". */}
      {Array.from({ length: 20 }, (_, i) => ({
        top: 30 + i * 3,
        width: Math.round(120 - (i + 0.5) * 1.8),
      })).map((band) => (
        <View
          key={band.top}
          style={{
            position: 'absolute',
            top: band.top,
            left: (120 - band.width) / 2,
            width: band.width,
            height: 4,
            backgroundColor: tokens.color.primary,
          }}
        />
      ))}
      {/* bottom facet: shoulders (18/102, 90) → apex (60,120), base 84 = 0.70w */}
      <View
        style={{
          position: 'absolute',
          top: 90,
          width: 0,
          height: 0,
          borderLeftWidth: 42,
          borderRightWidth: 42,
          borderTopWidth: 30,
          borderLeftColor: 'transparent',
          borderRightColor: 'transparent',
          borderTopColor: tokens.color.primary,
        }}
      />
      {/* lock (48dp in the original, centred): shackle + body in onPrimary */}
      <View
        style={{
          position: 'absolute',
          top: 40,
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
          top: 56,
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
