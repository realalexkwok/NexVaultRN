import { createPublicClient, fallback, formatUnits, getAddress, http, isAddress } from 'viem';

import { getChainConfig, toViemChain } from './chains';

function publicClient(chainId: number) {
  const config = getChainConfig(chainId);
  return createPublicClient({
    chain: toViemChain(config),
    // §2.2 failover: fixed array order (rank: false). The second endpoint doubles as the future
    // cross-verification source (§2.4 readVerified — T4.x, not part of this read-only subset).
    transport: fallback(
      config.rpcUrls.map((url) => http(url)),
      { rank: false },
    ),
  });
}

/** Native-coin balance in wei (bigint end-to-end; format only at the render edge). */
export async function getNativeBalance(address: `0x${string}`, chainId: number): Promise<bigint> {
  return publicClient(chainId).getBalance({ address });
}

/** Latest block number — doubles as the balance-walk evidence anchor (skill Step 5). */
export async function getLatestBlockNumber(chainId: number): Promise<bigint> {
  return publicClient(chainId).getBlockNumber();
}

/**
 * Display helper: wei -> decimal string with at most 4 fraction digits, trailing zeros stripped.
 * Precision is lost only here, at the render edge (skill Step 4).
 */
export function formatNativeBalance(wei: bigint, decimals = 18): string {
  const [whole, fraction] = formatUnits(wei, decimals).split('.');
  if (!fraction) return whole;
  const trimmed = fraction.slice(0, 4).replace(/0+$/, '');
  return trimmed.length > 0 ? `${whole}.${trimmed}` : whole;
}

/** viem isAddress — checksum insensitivity is handled by normalizeAddress. */
export function isValidAddress(value: string): value is `0x${string}` {
  return isAddress(value);
}

/** Returns the EIP-55 checksummed address, or null when the input is not an address. */
export function normalizeAddress(value: string): `0x${string}` | null {
  return isAddress(value) ? getAddress(value) : null;
}
