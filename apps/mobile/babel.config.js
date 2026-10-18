// NativeWind v4.2.7 documented Babel shape (nativewind.dev/docs/getting-started/installation, 2026-10-18).
// babel-preset-expo is the SDK 57 binding unit (57.0.15); jsxImportSource enables the className prop.
module.exports = function (api) {
  api.cache(true);
  return {
    presets: [
      ['babel-preset-expo', { jsxImportSource: 'nativewind' }],
      'nativewind/babel',
    ],
  };
};
