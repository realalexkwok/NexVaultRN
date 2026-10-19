import { TabList, TabSlot, TabTrigger, Tabs } from 'expo-router/ui';
import { Text } from 'react-native';

import { tokens } from '@/theme/tokens';

// Web fallback: there is no system tab bar on web (docs.expo.dev/router/advanced/native-tabs.md
// "Custom web layout", fetched 2026-10-19) → headless tabs from expo-router/ui. Same five tabs;
// glyphs stand in for icons because no icon package is approved in tech-stack (deviation recorded
// in validation.md; the Android MAN-016 walk uses the native Material symbols).
const TABS = [
  { name: 'home', href: '/home', glyph: '⌂', label: 'Home' },
  { name: 'history', href: '/history', glyph: '◷', label: 'History' },
  { name: 'dapp', href: '/dapp', glyph: '◎', label: 'DApp' },
  { name: 'nft', href: '/nft', glyph: '▣', label: 'NFT' },
  { name: 'settings', href: '/settings', glyph: '⚙', label: 'Settings' },
] as const;

export default function TabsLayoutWeb() {
  return (
    <Tabs>
      <TabSlot />
      <TabList
        style={{
          flexDirection: 'row',
          height: tokens.size.bottomNavHeight,
          borderTopWidth: tokens.size.borderWidth,
          borderTopColor: tokens.color.outlineVariant,
          backgroundColor: tokens.color.surface,
        }}
      >
        {TABS.map((tab) => (
          <TabTrigger
            key={tab.name}
            name={tab.name}
            href={tab.href}
            style={{ flex: 1, alignItems: 'center', justifyContent: 'center', gap: tokens.spacing.xxs }}
          >
            <Text
              style={{
                fontSize: tokens.type.titleLarge.size,
                lineHeight: tokens.type.titleLarge.lineHeight,
                color: tokens.color.onSurfaceVariant,
              }}
            >
              {tab.glyph}
            </Text>
            <Text
              style={{
                fontFamily: 'inter-medium',
                fontSize: tokens.type.labelSmall.size,
                lineHeight: tokens.type.labelSmall.lineHeight,
                color: tokens.color.onSurfaceVariant,
              }}
            >
              {tab.label}
            </Text>
          </TabTrigger>
        ))}
      </TabList>
    </Tabs>
  );
}
