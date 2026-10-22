// packages/web3-adapter — public read-only surface (T2.2), shared by both platform entries so the
// native and web barrels can never drift (review MINOR-6). T4.x signing/verification APIs
// (spec §2.3/§2.4) are deliberately absent — never stub-implement them here (R1/R7).
export { SUPPORTED_CHAINS, getChainConfig, type ChainConfig } from './chains';
export {
  formatNativeBalance,
  getLatestBlockNumber,
  getNativeBalance,
  isValidAddress,
  normalizeAddress,
} from './balance';
export { W3AError, isW3AError, type W3AErrorCode } from './errors';
export type { RpcOptions } from './types';
