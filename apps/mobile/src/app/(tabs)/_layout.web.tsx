import { usePathname } from 'expo-router';
import { TabList, TabSlot, TabTrigger, Tabs } from 'expo-router/ui';
import { Text, View } from 'react-native';

import { tokens } from '@/theme/tokens';

// Web fallback: there is no system tab bar on web (docs.expo.dev/router/advanced/native-tabs.md
// "Custom web layout", fetched 2026-10-19) → headless tabs from expo-router/ui. Same five tabs;
// glyphs stand in for icons because no icon package is approved in tech-stack (deviation recorded).
// Active state mirrors NavigationBarItem: primary icon/label + primaryContainer indicator pill.
// Bar height = M3 NavigationBar default 80dp (MainScreen.kt renders the default; the 64dp
// NexVaultDimens.bottomNavHeight constant is unused in the original — validation §7.10 n2).
const TABS = [
  { name: 'home', href: '/home', glyph: '⌂', label: 'Home' },
  { name: 'history', href: '/history', glyph: '◷', label: 'History' },
  { name: 'dapp', href: '/dapp', glyph: '◎', label: 'DApp' },
  { name: 'nft', href: '/nft', glyph: '▣', label: 'NFT' },
  { name: 'settings', href: '/settings', glyph: '⚙', label: 'Settings' },
] as const;

export default function TabsLayoutWeb() {
  const pathname = usePathname();
  return (
    <Tabs>
      <TabSlot />
      <TabList
        style={{
          flexDirection: 'row',
          height: tokens.size.navBar,
          borderTopWidth: tokens.size.borderWidth,
          borderTopColor: tokens.color.outlineVariant,
          backgroundColor: tokens.color.surface,
        }}
      >
        {TABS.map((tab) => {
          const active = pathname === tab.href;
          const tint = active ? tokens.color.primary : tokens.color.onSurfaceVariant;
          return (
            <TabTrigger
              key={tab.name}
              name={tab.name}
              href={tab.href}
              accessibilityState={{ selected: active }}
              // RN Web forwards aria-* / role to the rendered DOM anchor (TabTrigger passes unknown props
              // through; static SSR omits accessibilityState, so the ARIA attributes are set explicitly).
              {...({ role: 'tab', 'aria-selected': active } as Record<string, unknown>)}
              style={{ flex: 1, alignItems: 'center', justifyContent: 'center', gap: tokens.spacing.xxs }}
            >
              <View
                style={{
                  paddingHorizontal: tokens.spacing.md,
                  paddingVertical: tokens.spacing.xs,
                  borderRadius: tokens.radius.lg,
                  backgroundColor: active ? tokens.color.primaryContainer : 'transparent',
                }}
              >
                <Text
                  style={{
                    fontSize: tokens.type.titleLarge.size,
                    lineHeight: tokens.type.titleLarge.lineHeight,
                    color: tint,
                  }}
                >
                  {tab.glyph}
                </Text>
              </View>
              <Text
                style={{
                  fontFamily: 'inter-medium',
                  fontSize: tokens.type.labelSmall.size,
                  lineHeight: tokens.type.labelSmall.lineHeight,
                  color: tint,
                }}
              >
                {tab.label}
              </Text>
            </TabTrigger>
          );
        })}
      </TabList>
    </Tabs>
  );
}
