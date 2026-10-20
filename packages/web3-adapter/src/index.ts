// packages/web3-adapter — read-only subset (T2.2). Native entry.
//
// Deliberately absent until T4.x (spec §2.3/§2.4, skill Invariant 1): prepareTransaction,
// signPrepared, broadcastSigned, signAndBroadcast, waitAndVerifyReceipt, readVerified.
// Never stub-implement them here — a fake signing surface would violate R1/R7.
export { SUPPORTED_CHAINS, getChainConfig, type ChainConfig } from './chains';
export {
  formatNativeBalance,
  getLatestBlockNumber,
  getNativeBalance,
  isValidAddress,
  normalizeAddress,
} from './balance';
export { W3AError, isW3AError, type W3AErrorCode } from './errors';
