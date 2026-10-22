import { createPublicClient, fallback, http } from 'viem';

import { getChainConfig, toViemChain } from './chains';
import type { RpcOptions } from './types';

export { formatNativeBalance } from './format';
export { isValidAddress, normalizeAddress } from './address';

function publicClient(chainId: number, options: RpcOptions) {
  const config = getChainConfig(chainId);
  return createPublicClient({
    chain: toViemChain(config),
    // §2.2 failover: fixed array order (rank: false). The second endpoint doubles as the future
    // cross-verification source (§2.4 readVerified — T4.x, not part of this read-only subset).
    transport: fallback(
      config.rpcUrls.map((url) => http(url, { timeout: options.timeoutMs })),
      { rank: false },
    ),
  });
}

/** Native-coin balance in wei (bigint end-to-end; format only at the render edge). */
export async function getNativeBalance(address: `0x${string}`, chainId: number, options: RpcOptions = {}): Promise<bigint> {
  return publicClient(chainId, options).getBalance({ address });
}

/** Latest block number — doubles as the balance-walk evidence anchor (spec §2.5 / validation §6.2). */
export async function getLatestBlockNumber(chainId: number, options: RpcOptions = {}): Promise<bigint> {
  return publicClient(chainId, options).getBlockNumber();
}
