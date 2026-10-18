// NativeWind v4.2.7 documented Metro shape: withNativeWind compiles the Tailwind input CSS
// (global.css) for native + web (web.bundler: "metro" in app.json).
const { getDefaultConfig } = require('expo/metro-config');
const { withNativeWind } = require('nativewind/metro');

const config = getDefaultConfig(__dirname);

module.exports = withNativeWind(config, { input: './global.css' });
