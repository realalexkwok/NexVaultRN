import { getAddress, isAddress } from 'viem';

/** viem isAddress — checksum insensitivity is handled by normalizeAddress. */
export function isValidAddress(value: string): value is `0x${string}` {
  return isAddress(value);
}

/** Returns the EIP-55 checksummed address, or null when the input is not an address. */
export function normalizeAddress(value: string): `0x${string}` | null {
  return isAddress(value) ? getAddress(value) : null;
}
