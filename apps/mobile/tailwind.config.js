/** @type {import('tailwindcss').Config} */
// NativeWind v4 stable (4.2.7) — Tailwind v3 line. Theme values come from src/theme/tokens.ts
// (spec: ui-wallet §2.3). content globs cover every file that can carry a className.
const { tokens } = require('./src/theme/tokens');

module.exports = {
  content: ['./src/**/*.{js,jsx,ts,tsx}'],
  presets: [require('nativewind/preset')],
  theme: {
    extend: {
      colors: {
        bg: tokens.color.bg,
        surface: tokens.color.surface,
        surfaceHi: tokens.color.surfaceHi,
        primary: tokens.color.primary,
        primaryFg: tokens.color.primaryFg,
        text: tokens.color.text,
        textDim: tokens.color.textDim,
        success: tokens.color.success,
        warning: tokens.color.warning,
        danger: tokens.color.danger,
        border: tokens.color.border,
      },
      borderRadius: {
        card: tokens.radius.card,
        button: tokens.radius.button,
        chip: tokens.radius.chip,
      },
      spacing: {
        gutter: tokens.spacing.gutter,
        cardPad: tokens.spacing.cardPad,
      },
      fontFamily: {
        addressMono: tokens.type.addressMono,
      },
    },
  },
  plugins: [],
};
