// Placeholder barrel export for the vault-core local Expo module.
// Native methods (importMnemonic / deriveAddress / signDigest / destroy) are
// intentionally NOT implemented in this bootstrap — implementation starts at
// plans task T1.2 (per roadmap T1.2, do not implement here).
export { default as VaultCoreModule } from './src/VaultCoreModule';
export type {} from './src/VaultCore.types';
