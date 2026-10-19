// src/theme/tokens.ts — design tokens, single source of truth (spec: ui-wallet §2.3).
// 2026-10-19 T2.1a: full transcription of the original Kotlin app's design system
// (/home/superguo/Projects/NexVault @ 3c7487f — implementation code is authoritative;
// doc/03 is rejected). Every value carries its evidence reference; transcription errors
// are handled like T1.3's vector-transcription findings (zero invention).
// Consumed by: tailwind.config.js (theme.extend), screens/components via className, and
// the few RN props that cannot take classes (placeholderTextColor, ActivityIndicator).
export const tokens = {
  color: {
    // Color.kt（dark scheme，生产为纯暗色）
    bg: '#0A0E1A', // Color.kt:DarkBackground
    surface: '#121829', // Color.kt:DarkSurface
    surfaceVariant: '#1A1F35', // Color.kt:DarkSurfaceVariant
    surfaceVariant2: '#232842', // Color.kt:DarkSurfaceVariant2
    primary: '#2D5AF0', // Color.kt:DarkPrimary
    onPrimary: '#FFFFFF', // Color.kt:DarkOnPrimary
    primaryContainer: '#1E3A8A', // Color.kt:DarkPrimaryContainer
    onPrimaryContainer: '#D8E2FF', // Color.kt:DarkOnPrimaryContainer
    secondary: '#00D4AA', // Color.kt:DarkSecondary
    onSecondary: '#003730', // Color.kt:DarkOnSecondary
    secondaryContainer: '#005045', // Color.kt:DarkSecondaryContainer
    onSecondaryContainer: '#70F7DC', // Color.kt:DarkOnSecondaryContainer
    tertiary: '#7B61FF', // Color.kt:DarkTertiary
    onTertiary: '#2E0080', // Color.kt:DarkOnTertiary
    tertiaryContainer: '#4A00B8', // Color.kt:DarkTertiaryContainer
    onTertiaryContainer: '#E8DDFF', // Color.kt:DarkOnTertiaryContainer
    error: '#FF4C6E', // Color.kt:DarkError
    onError: '#FFFFFF', // Color.kt:DarkOnError
    errorContainer: '#93000A', // Color.kt:DarkErrorContainer
    onErrorContainer: '#FFDAD6', // Color.kt:DarkOnErrorContainer
    onBackground: '#FFFFFF', // Color.kt:DarkOnBackground
    onSurface: '#FFFFFF', // Color.kt:DarkOnSurface
    onSurfaceVariant: '#A0A8C8', // Color.kt:DarkOnSurfaceVariant
    onSurfaceDisabled: '#5A6180', // Color.kt:DarkOnSurfaceDisabled
    outline: '#404759', // Color.kt:DarkOutline
    outlineVariant: '#252B3F', // Color.kt:DarkOutlineVariant
    // NexVaultColors.kt（dark；语义/加密专用色）
    positive: '#00E676', // NexVaultColors.kt:positive
    negative: '#FF5252', // NexVaultColors.kt:negative
    warning: '#FFB74D', // NexVaultColors.kt:warning
    info: '#2196F3', // NexVaultColors.kt:info
    cardSurface: '#141928', // NexVaultColors.kt:cardSurface
    cardBorder: '#1E2440', // NexVaultColors.kt:cardBorder
    shimmer: '#1E2440', // NexVaultColors.kt:shimmer
    shimmerHighlight: '#2A3148', // NexVaultColors.kt:shimmerHighlight
    textHigh: '#FFFFFF', // NexVaultColors.kt:textHigh
    textMedium: '#A0A8C8', // NexVaultColors.kt:textMedium
    textDisabled: '#5A6180', // NexVaultColors.kt:textDisabled
    gradientStart: '#0A0E1A', // NexVaultColors.kt:gradientStart
    gradientEnd: '#121829', // NexVaultColors.kt:gradientEnd
  },
  radius: {
    // NexVaultDimens.kt:cornerRadius*；组件级用法（NexVaultCard/Button/TextField.kt）
    xs: 4,
    sm: 8,
    md: 12,
    lg: 16,
    xl: 24,
    card: 16,
    button: 16,
    input: 12,
    chip: 8,
  },
  spacing: {
    // NexVaultDimens.kt:spacing*（+ 扩展步 spacing6/10/12/18/64）
    xxs: 2,
    xs: 4,
    sm: 8,
    md: 16,
    lg: 24,
    xl: 32,
    xxl: 48,
    s6: 6,
    s10: 10,
    s12: 12,
    s18: 18,
    s64: 64,
    gutterLg: 24, // onboarding/receive/swap 屏 gutter（原仓库盘点）
    gutterMd: 16, // home/token/history/send/nft 屏 gutter（原仓库盘点）
  },
  size: {
    buttonHeight: 52, // NexVaultDimens.kt:buttonHeight
    inputHeight: 56, // NexVaultDimens.kt:inputHeight
    bottomNavHeight: 64, // NexVaultDimens.kt:bottomNavHeight
    icon: 24,
    iconSmall: 20,
    tokenIcon: 40,
    actionButton: 56,
    chartHeight: 180, // NexVaultDimens.kt:chartHeight
    borderWidth: 1,
    borderWidthEmphasis: 1.5,
    borderWidthStrong: 2,
  },
  type: {
    // Type.kt:NexVaultTypography（size/lineHeight/weight/letterSpacing；family = Inter）
    displayLarge: { size: 36, lineHeight: 44, weight: '700', letterSpacing: -0.25 },
    displayMedium: { size: 32, lineHeight: 40, weight: '700', letterSpacing: 0 },
    displaySmall: { size: 28, lineHeight: 36, weight: '700', letterSpacing: 0 },
    headlineLarge: { size: 28, lineHeight: 36, weight: '700', letterSpacing: 0 },
    headlineMedium: { size: 24, lineHeight: 32, weight: '600', letterSpacing: 0 },
    headlineSmall: { size: 20, lineHeight: 28, weight: '600', letterSpacing: 0 },
    titleLarge: { size: 18, lineHeight: 26, weight: '600', letterSpacing: 0 },
    titleMedium: { size: 16, lineHeight: 24, weight: '600', letterSpacing: 0.1 },
    titleSmall: { size: 14, lineHeight: 20, weight: '600', letterSpacing: 0.1 },
    bodyLarge: { size: 16, lineHeight: 24, weight: '400', letterSpacing: 0.5 },
    bodyMedium: { size: 14, lineHeight: 20, weight: '400', letterSpacing: 0.25 },
    bodySmall: { size: 12, lineHeight: 16, weight: '400', letterSpacing: 0.4 },
    labelLarge: { size: 14, lineHeight: 20, weight: '500', letterSpacing: 0.1 },
    labelMedium: { size: 12, lineHeight: 16, weight: '500', letterSpacing: 0.5 },
    labelSmall: { size: 10, lineHeight: 14, weight: '500', letterSpacing: 0.5 },
  },
  font: {
    family: 'Inter', // Type.kt:NexVaultFontFamily（4 权重 TTF，OFL，自原仓库 res/font 复制）
    weights: { regular: '400', medium: '500', semibold: '600', bold: '700' },
  },
} as const;

/**
 * The original app derives disabled colours as `onSurface.copy(alpha = …)`
 * (NexVaultButton.kt / NexVaultTextField.kt). This is the RN equivalent — the hex source
 * is always a token, so no literal colours enter component files.
 */
export function withAlpha(hex: string, alpha: number): string {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  const a = Math.round(Math.min(1, Math.max(0, alpha)) * 1000) / 1000;
  return `rgba(${r}, ${g}, ${b}, ${a})`;
}
