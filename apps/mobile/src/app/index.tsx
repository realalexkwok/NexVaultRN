import { Link } from 'expo-router';
import { Pressable, Text, View } from 'react-native';

import { Screen } from '@/components/Screen';

// Onboarding shell: identity text + disabled placeholders. NO key-material UI or logic (R1) —
// the real import/create flow is T4.1. "Not configured" labels per R5.
const shellLinks = [
  { href: '/home', label: 'Home' },
  { href: '/receive', label: 'Receive' },
  { href: '/send', label: 'Send' },
] as const;

export default function OnboardingScreen() {
  return (
    <Screen title="NexVault">
      <Text className="text-textDim mb-cardPad">
        Self-custodial EVM wallet prototype — Android/iOS full, Web read-only.
      </Text>
      <View className="bg-surface rounded-card border border-border p-cardPad gap-cardPad">
        <Pressable
          disabled
          accessibilityRole="button"
          accessibilityState={{ disabled: true }}
          className="bg-surfaceHi rounded-button py-3 items-center opacity-50"
        >
          <Text className="text-textDim font-semibold">Import wallet</Text>
        </Pressable>
        <Pressable
          disabled
          accessibilityRole="button"
          accessibilityState={{ disabled: true }}
          className="bg-surfaceHi rounded-button py-3 items-center opacity-50"
        >
          <Text className="text-textDim font-semibold">Create wallet</Text>
        </Pressable>
        <Text className="text-textDim text-center">Not configured</Text>
      </View>
      <View className="mt-cardPad">
        <Text className="text-textDim mb-cardPad">Shell preview</Text>
        {shellLinks.map((l) => (
          <Link key={l.href} href={l.href} asChild>
            <Pressable className="bg-surface rounded-card border border-border p-cardPad mb-cardPad">
              <Text className="text-primary font-semibold">{l.label}</Text>
            </Pressable>
          </Link>
        ))}
      </View>
    </Screen>
  );
}
