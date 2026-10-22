// T2.2 adapter unit tests (review MAJOR-1): the unknown-chain guard is the only wrong-chain guard
// for T4.x work, and the display/address helpers are acceptance criteria (AC-2 / AC-5, AC-12).
// Run with: pnpm --filter mobile exec jest
import { describe, expect, it } from '@jest/globals';
import {
  SUPPORTED_CHAINS,
  formatNativeBalance,
  getChainConfig,
  isW3AError,
  isValidAddress,
  normalizeAddress,
} from 'web3-adapter';

const CANONICAL = '0x9858EfFD232B4033E47d90003D41EC34EcaEda94';
const VITALIK = '0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045';
const VITALIK_WEI = 5773251986299759339n;

describe('getChainConfig (TC-W3A-010)', () => {
  it('returns the registered mainnet and sepolia configs', () => {
    expect(getChainConfig(1).name).toBe('Ethereum');
    expect(getChainConfig(11155111).name).toBe('Sepolia');
  });

  it('throws E_W3A_UNKNOWN_CHAIN for an unregistered chainId (no RPC involved)', () => {
    let caught: unknown;
    try {
      getChainConfig(999999);
    } catch (error) {
      caught = error;
    }
    expect(isW3AError(caught)).toBe(true);
    if (isW3AError(caught)) {
      expect(caught.code).toBe('E_W3A_UNKNOWN_CHAIN');
      expect(caught.message).toContain('999999');
    }
  });

  it('registry invariants: every chain has >= 2 distinct endpoints from >= 2 operators', () => {
    expect(SUPPORTED_CHAINS.length).toBeGreaterThanOrEqual(2);
    for (const chain of SUPPORTED_CHAINS) {
      expect(chain.rpcUrls.length).toBeGreaterThanOrEqual(2);
      // Distinct hostnames, and at least two distinct operator domains (review-fix loop 2 /
      // review round 5 MINOR-5).
      const hostnames = chain.rpcUrls.map((url) => new URL(url).hostname);
      expect(new Set(hostnames).size).toBe(chain.rpcUrls.length);
      const operatorDomains = new Set(hostnames.map((host) => host.split('.').slice(-2).join('.')));
      expect(operatorDomains.size).toBeGreaterThanOrEqual(2);
      expect(chain.nativeCurrency.decimals).toBe(18);
      expect(chain.explorerTxUrl('0xdead')).toContain('0xdead');
    }
  });
});

describe('formatNativeBalance (TC-W3A-013)', () => {
  it('renders an exact zero as 0', () => {
    expect(formatNativeBalance(0n)).toBe('0');
  });

  it('renders nonzero wei below display precision as <0.0001, never a false zero (NIT-9)', () => {
    expect(formatNativeBalance(1n)).toBe('<0.0001');
    expect(formatNativeBalance(9999n)).toBe('<0.0001');
    expect(formatNativeBalance(10n ** 14n - 1n)).toBe('<0.0001');
  });

  it('renders exactly 0.0001 at the threshold', () => {
    expect(formatNativeBalance(10n ** 14n)).toBe('0.0001');
  });

  it('trims trailing zeros and keeps at most 4 fraction digits', () => {
    expect(formatNativeBalance(5n * 10n ** 18n)).toBe('5');
    expect(formatNativeBalance(VITALIK_WEI)).toBe('5.7732');
    expect(formatNativeBalance(1_234_567n, 6)).toBe('1.2345');
  });
});

describe('address helpers (TC-W3A-009)', () => {
  it('normalizes lowercase input to EIP-55 checksum', () => {
    expect(normalizeAddress(CANONICAL.toLowerCase())).toBe(CANONICAL);
    expect(normalizeAddress(VITALIK.toLowerCase())).toBe(VITALIK);
  });

  it('accepts already-checksummed and all-lowercase addresses', () => {
    expect(normalizeAddress(CANONICAL)).toBe(CANONICAL);
    expect(isValidAddress(VITALIK)).toBe(true);
    expect(isValidAddress(VITALIK.toLowerCase())).toBe(true);
  });

  it('rejects malformed input', () => {
    expect(normalizeAddress('0x123')).toBeNull();
    expect(normalizeAddress('not-an-address')).toBeNull();
    expect(isValidAddress('0x')).toBe(false);
  });
});
