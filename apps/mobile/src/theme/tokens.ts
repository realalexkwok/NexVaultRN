// src/theme/tokens.ts — design tokens, single source of truth (spec: ui-wallet §2.3).
// Consumed by: tailwind.config.js (theme.extend) and by screens via className/theme keys.
// Zero hardcoded color values in route/component files (TC-UI-007 discipline).
export const tokens = {
  color: {
    bg: '#1B1815',
    surface: '#282219',
    surfaceHi: '#342C21',
    primary: '#C79A6B',
    primaryFg: '#1B1815',
    text: '#EDE6DC',
    textDim: '#A79C8E',
    success: '#7FA37A',
    warning: '#C9A25B',
    danger: '#B4635A',
    border: '#3D352A',
  },
  radius: { card: 12, button: 10, chip: 999 },
  spacing: { gutter: 16, cardPad: 16 },
  type: { addressMono: 'monospace' },
} as const;
