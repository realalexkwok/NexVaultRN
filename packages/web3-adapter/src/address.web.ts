// Web address helpers: validation + canonicalization without viem, so the web chunk stays free of
// the signing/derivation graph (R7 split). EIP-55 mixed-case checksums are a display nicety; the
// observer path only needs a valid 20-byte hex address, and JSON-RPC accepts lowercase.
const ADDRESS_PATTERN = /^0x[0-9a-fA-F]{40}$/;

export function isValidAddress(value: string): value is `0x${string}` {
  return ADDRESS_PATTERN.test(value);
}

export function normalizeAddress(value: string): `0x${string}` | null {
  return isValidAddress(value) ? (value.toLowerCase() as `0x${string}`) : null;
}
