// packages/web3-adapter — read-only subset (T2.2). Web entry (R7: observer-only build).
//
// Identical surface to index.ts for T2.2; §2.5's web-only extras (getTransactionStatus,
// buildUnsignedTxQrPayload) are T4.x. Re-exported from the leaf modules (not from './index')
// so Metro's platform resolution cannot self-import this file.
export { SUPPORTED_CHAINS, getChainConfig, type ChainConfig } from './chains';
export {
  formatNativeBalance,
  getLatestBlockNumber,
  getNativeBalance,
  isValidAddress,
  normalizeAddress,
} from './balance';
export { W3AError, isW3AError, type W3AErrorCode } from './errors';
