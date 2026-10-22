// Jest config (SDK 57 binding: jest-expo 57.0.6 <-> jest 29.7). T2.2 adds adapter unit tests for
// the unknown-chain guard, display formatting, and address normalization (review MAJOR-1).
// Match .test.ts/.test.tsx and __tests__ so future component/hook tests are not silently skipped
// (review round 2 MINOR-2).
module.exports = {
  preset: 'jest-expo',
  testMatch: ['<rootDir>/src/**/*.test.{ts,tsx}', '<rootDir>/src/**/__tests__/**/*.{ts,tsx}'],
};
