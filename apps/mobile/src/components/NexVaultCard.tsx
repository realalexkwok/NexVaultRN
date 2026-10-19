import { ReactNode } from 'react';
import { View } from 'react-native';

// NexVaultCard.kt @3c7487f: fillMaxWidth, radius 16 (cornerRadiusLarge), cardSurface,
// 1px cardBorder outline, elevation 0 (flat — no shadows), inner padding 16 (spacingMd).
export function NexVaultCard({ children, className = '' }: { children: ReactNode; className?: string }) {
  return (
    <View className={`w-full rounded-card border border-card-border bg-card-surface p-md ${className}`}>
      {children}
    </View>
  );
}
