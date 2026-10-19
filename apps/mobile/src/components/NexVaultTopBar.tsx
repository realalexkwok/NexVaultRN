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
      <View className="h-14 w-full flex-row items-center justify-center px-md">
        {showBack ? (
          <Pressable
            accessibilityRole="button"
            accessibilityLabel="Back"
            onPress={onBack}
            hitSlop={tokens.spacing.sm}
            className="absolute left-md h-10 w-10 items-center justify-center"
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
