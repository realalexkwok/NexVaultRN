import { ActivityIndicator, Pressable, Text } from 'react-native';

import { tokens, withAlpha } from '@/theme/tokens';

// NexVaultButton.kt @3c7487f: fillMaxWidth, height 52, radius 16; primary = primary/onPrimary,
// secondary = outlined primary; disabled = onSurface.copy(alpha 0.12 / 0.38); label = labelLarge;
// loading = 20dp spinner; content padding horizontal 24 / vertical 16.
export function NexVaultButton({
  label,
  onPress,
  variant = 'primary',
  disabled = false,
  loading = false,
  className = '',
}: {
  label: string;
  onPress?: () => void;
  variant?: 'primary' | 'secondary';
  disabled?: boolean;
  loading?: boolean;
  className?: string;
}) {
  const isPrimary = variant === 'primary';
  const inactive = disabled || loading;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ disabled: inactive, busy: loading }}
      disabled={inactive}
      onPress={onPress}
      className={`w-full h-btn rounded-button flex-row items-center justify-center px-lg ${
        !inactive && isPrimary ? 'bg-primary' : ''
      } ${!inactive && !isPrimary ? 'border border-primary' : ''} ${inactive && !isPrimary ? 'border' : ''} ${className}`}
      style={{
        ...(inactive && isPrimary ? { backgroundColor: withAlpha(tokens.color.onSurface, 0.12) } : {}),
        ...(inactive && !isPrimary ? { borderColor: withAlpha(tokens.color.onSurface, 0.12) } : {}),
      }}
    >
      {loading ? (
        <ActivityIndicator size="small" color={isPrimary ? tokens.color.onPrimary : tokens.color.primary} />
      ) : (
        <Text
          className={`text-label-large font-inter-medium ${!inactive && isPrimary ? 'text-on-primary' : ''} ${
            !inactive && !isPrimary ? 'text-primary' : ''
          }`}
          style={inactive ? { color: withAlpha(tokens.color.onSurface, 0.38) } : undefined}
        >
          {label}
        </Text>
      )}
    </Pressable>
  );
}
