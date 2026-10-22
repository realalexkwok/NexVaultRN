import { getAddress, isAddress } from 'viem';

/** viem isAddress — checksum insensitivity is handled by normalizeAddress. */
export function isValidAddress(value: string): value is `0x${string}` {
  return isAddress(value);
}

/** Native: returns the EIP-55 checksummed address, or null when viem rejects the input.
 *  Web (address.web.ts) cannot run keccak, so it only regex-validates and lowercases — see
 *  specs/web3-adapter.md §2.5 for the documented platform split. */
export function normalizeAddress(value: string): `0x${string}` | null {
  return isAddress(value) ? getAddress(value) : null;
}
