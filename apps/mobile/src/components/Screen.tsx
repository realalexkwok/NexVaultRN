import { router } from 'expo-router';
import { ReactNode } from 'react';
import { ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { NexVaultTopBar } from '@/components/NexVaultTopBar';
import { tokens } from '@/theme/tokens';

// Shared shell scaffold: dark M3 theme (ui-wallet §2.3 / original repo @3c7487f), tokens-driven.
// `title` renders the transparent centered TopBar (NexVaultTopBar.kt); omit it for screens that
// lead with their own header (home tab — chain selector). The TopBar owns the status-bar inset,
// so the SafeAreaView drops the top edge when a bar is present. `scroll` (default true) keeps
// large font scales / small screens from clipping; screens with a virtualized list pass false.
export function Screen({
  title,
  children,
  scroll = true,
  showBack = false,
  opaqueBar = false,
  gutter = 'md',
}: {
  title?: string;
  children: ReactNode;
  scroll?: boolean;
  showBack?: boolean;
  opaqueBar?: boolean;
  /** Original screens use spacingLg (24) on onboarding/receive and spacingMd (16) elsewhere. */
  gutter?: 'md' | 'lg';
}) {
  const contentStyle = {
    flexGrow: 1,
    paddingHorizontal: gutter === 'lg' ? tokens.spacing.gutterLg : tokens.spacing.gutterMd,
    paddingBottom: tokens.spacing.lg,
  };
  return (
    <SafeAreaView className="flex-1 bg-bg" edges={title ? ['left', 'right'] : ['top', 'left', 'right']}>
      {title ? (
        <NexVaultTopBar title={title} showBack={showBack} onBack={() => router.back()} opaque={opaqueBar} />
      ) : null}
      {scroll ? (
        <ScrollView className="flex-1" contentContainerStyle={contentStyle} keyboardShouldPersistTaps="handled">
          {children}
        </ScrollView>
      ) : (
        <View className="flex-1" style={contentStyle}>
          {children}
        </View>
      )}
    </SafeAreaView>
  );
}
