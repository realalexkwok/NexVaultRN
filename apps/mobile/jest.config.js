// Jest config (SDK 57 binding: jest-expo 57.0.6 <-> jest 29). T2.2 adds adapter unit tests for
// the unknown-chain guard, display formatting, and address normalization (review MAJOR-1).
module.exports = {
  preset: 'jest-expo',
  testMatch: ['<rootDir>/src/**/*.test.ts'],
};
