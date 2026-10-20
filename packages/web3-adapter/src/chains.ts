import { defineChain, type Chain } from 'viem';

import { W3AError } from './errors';

// spec: web3-adapter §2.2 — the registry is the single source of chain truth. Two independent
// public operators per chain, no API keys (R5: nothing to inject, nothing to leak). Demo scope is
// mainnet + sepolia; the balance-walk anchors live on mainnet (skill Step 5).
export interface ChainConfig {
  chainId: number;
  name: string;
  /** >= 2 independent public RPC endpoints; failover follows array order. */
  rpcUrls: string[];
  explorerTxUrl: (hash: string) => string;
  nativeCurrency: { symbol: string; decimals: number };
}

export const SUPPORTED_CHAINS: readonly ChainConfig[] = [
  {
    chainId: 1,
    name: 'Ethereum',
    rpcUrls: ['https://eth.drpc.org', 'https://ethereum-rpc.publicnode.com'],
    explorerTxUrl: (hash) => `https://etherscan.io/tx/${hash}`,
    nativeCurrency: { symbol: 'ETH', decimals: 18 },
  },
  {
    chainId: 11155111,
    name: 'Sepolia',
    rpcUrls: ['https://sepolia.drpc.org', 'https://ethereum-sepolia-rpc.publicnode.com'],
    explorerTxUrl: (hash) => `https://sepolia.etherscan.io/tx/${hash}`,
    nativeCurrency: { symbol: 'ETH', decimals: 18 },
  },
];

export function getChainConfig(chainId: number): ChainConfig {
  const config = SUPPORTED_CHAINS.find((candidate) => candidate.chainId === chainId);
  if (!config) {
    // §2.6: unregistered chainIds fail before any RPC or signature work (EIP-155 domain guard).
    throw new W3AError('E_W3A_UNKNOWN_CHAIN', `Unregistered chainId ${chainId}`);
  }
  return config;
}

/** Internal viem mapping — deliberately not part of the public barrel. */
export function toViemChain(config: ChainConfig): Chain {
  return defineChain({
    id: config.chainId,
    name: config.name,
    nativeCurrency: { ...config.nativeCurrency, name: config.name },
    rpcUrls: { default: { http: config.rpcUrls } },
  });
}
