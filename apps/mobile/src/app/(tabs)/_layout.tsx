import { NativeTabs } from 'expo-router/unstable-native-tabs';

import { tokens } from '@/theme/tokens';

// 5-tab bottom nav — original MainTab.kt/MainScreen.kt @3c7487f: Home/History/DApp/NFT/Settings;
// M3 NavigationBar colours: container surface, selected primary, unselected onSurfaceVariant,
// label labelSmall. Native tabs per docs.expo.dev/router/advanced/native-tabs.md (fetched 2026-10-19);
// SDK 57 entry point is `expo-router/unstable-native-tabs`.
export default function TabsLayout() {
  return (
    <NativeTabs
      tintColor={tokens.color.primary}
      labelStyle={{ color: tokens.color.onSurfaceVariant }}
      backgroundColor={tokens.color.surface}
      disableTransparentOnScrollEdge
    >
      <NativeTabs.Trigger name="home">
        <NativeTabs.Trigger.Icon sf="house" md="home" />
        <NativeTabs.Trigger.Label>Home</NativeTabs.Trigger.Label>
      </NativeTabs.Trigger>
      <NativeTabs.Trigger name="history">
        <NativeTabs.Trigger.Icon sf="clock" md="receipt_long" />
        <NativeTabs.Trigger.Label>History</NativeTabs.Trigger.Label>
      </NativeTabs.Trigger>
      <NativeTabs.Trigger name="dapp">
        <NativeTabs.Trigger.Icon sf="safari" md="explore" />
        <NativeTabs.Trigger.Label>DApp</NativeTabs.Trigger.Label>
      </NativeTabs.Trigger>
      <NativeTabs.Trigger name="nft">
        <NativeTabs.Trigger.Icon sf="photo.on.rectangle" md="photo_library" />
        <NativeTabs.Trigger.Label>NFT</NativeTabs.Trigger.Label>
      </NativeTabs.Trigger>
      <NativeTabs.Trigger name="settings">
        <NativeTabs.Trigger.Icon sf="gearshape" md="settings" />
        <NativeTabs.Trigger.Label>Settings</NativeTabs.Trigger.Label>
      </NativeTabs.Trigger>
    </NativeTabs>
  );
}
