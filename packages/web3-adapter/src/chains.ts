import { defineChain, type Chain } from 'viem';

import type { ChainConfig } from './chains.base';

export * from './chains.base';

/** Internal viem mapping — deliberately not part of the public barrel (native only). */
export function toViemChain(config: ChainConfig): Chain {
  return defineChain({
    id: config.chainId,
    name: config.name,
    nativeCurrency: { ...config.nativeCurrency, name: config.name },
    rpcUrls: { default: { http: config.rpcUrls } },
  });
}
