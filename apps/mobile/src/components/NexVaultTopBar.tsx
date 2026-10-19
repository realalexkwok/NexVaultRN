import { Pressable, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { tokens } from '@/theme/tokens';

// NexVaultTopBar.kt @3c7487f: transparent by default (opaqueBackground = cardSurface), centered
// title = titleLarge 18/26 SemiBold, textHigh, single line; optional back arrow; the height is
// deliberately not forced — M3 under edge-to-edge measures status inset + content row.
export function NexVaultTopBar({
  title,
  showBack = false,
  onBack,
  opaque = false,
  className = '',
}: {
  title: string;
  showBack?: boolean;
  onBack?: () => void;
  opaque?: boolean;
  className?: string;
}) {
  const insets = useSafeAreaInsets();
  return (
    <View
      className={`w-full ${opaque ? 'bg-card-surface' : 'bg-transparent'} ${className}`}
      style={{ paddingTop: insets.top }}
    >
      {/* M3 CenterAlignedTopAppBar content row = 64dp (TopAppBarDefaults); the Kotlin bar itself
          measures status inset + this row (NexVaultTopBar.kt deliberately does not force height). */}
      <View className="w-full flex-row items-center justify-center px-md" style={{ height: tokens.spacing.s64 }}>
        {showBack ? (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Back"
            onPress={onBack}
            hitSlop={tokens.spacing.sm}
            className="absolute left-md items-center justify-center"
            style={{ width: tokens.size.tokenIcon, height: tokens.size.tokenIcon }}
          >
            <Text className="text-headline-small font-inter-regular text-text-high">{'\u2190'}</Text>
          </Pressable>
        ) : null}
        <Text numberOfLines={1} className="text-title-large font-inter-semibold text-text-high">
          {title}
        </Text>
      </View>
    </View>
  );
}
