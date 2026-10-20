import { useState } from 'react';
import { Text, TextInput, View } from 'react-native';

import { tokens } from '@/theme/tokens';

// NexVaultTextField.kt @3c7487f: outlined field, height 56, radius 12 (cornerRadiusMedium);
// focused border = primary, unfocused = cardBorder, error = error; label/placeholder textMedium;
// text textHigh; disabled text/border/label textDisabled; error caption bodySmall (start 16 / top 4).
export function NexVaultTextField({
  label,
  placeholder,
  value,
  onChangeText,
  error,
  prefix,
  editable = true,
  keyboardType = 'default',
  labelBackdrop = 'bg-bg',
  className = '',
  autoCapitalize = 'sentences',
  onBlur,
  onSubmitEditing,
}: {
  label?: string;
  placeholder?: string;
  value: string;
  onChangeText?: (value: string) => void;
  error?: string;
  prefix?: string;
  editable?: boolean;
  keyboardType?: 'default' | 'numeric' | 'email-address';
  labelBackdrop?: string;
  className?: string;
  autoCapitalize?: 'none' | 'sentences' | 'words' | 'characters';
  /** Commit hook (T2.2 watch-address field); fires after the focus state is updated. */
  onBlur?: () => void;
  onSubmitEditing?: () => void;
}) {
  const [focused, setFocused] = useState(false);
  const floated = focused || value.length > 0;
  const borderClass = error
    ? 'border-error'
    : focused
      ? 'border-primary'
      : editable
        ? 'border-card-border'
        : 'border-text-disabled';
  return (
    <View className={`w-full ${className}`}>
      <View className={`h-input w-full flex-row items-center rounded-input border px-md ${borderClass}`}>
        {label && floated ? (
          <Text
            className={`absolute left-md -top-2 px-xs text-label-small font-inter-regular ${labelBackdrop} ${
              focused ? 'text-primary' : editable ? 'text-text-medium' : 'text-text-disabled'
            }`}
          >
            {label}
          </Text>
        ) : null}
        {prefix ? <Text className="mr-xs text-body-large font-inter-regular text-text-medium">{prefix}</Text> : null}
        <TextInput
          className={`flex-1 text-body-large font-inter-regular ${editable ? 'text-text-high' : 'text-text-disabled'}`}
          value={value}
          onChangeText={onChangeText}
          editable={editable}
          placeholder={floated ? placeholder : (label ?? placeholder)}
          placeholderTextColor={tokens.color.textMedium}
          keyboardType={keyboardType}
          autoCapitalize={autoCapitalize}
          onFocus={() => setFocused(true)}
          onBlur={() => {
            setFocused(false);
            onBlur?.();
          }}
          onSubmitEditing={onSubmitEditing}
        />
      </View>
      {error ? <Text className="mt-xs pl-md text-body-small font-inter-regular text-error">{error}</Text> : null}
    </View>
  );
}
