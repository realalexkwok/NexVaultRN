// Web read path (R7 split): the web balance module must work without viem, using fetch JSON-RPC
// with order-based failover. These tests exercise the module directly (jest runs the native
// barrel for the regular adapter tests, so the web file is imported explicitly).
import { afterEach, describe, expect, it, jest } from '@jest/globals';

import {
  formatNativeBalance,
  getLatestBlockNumber,
  getNativeBalance,
  isValidAddress,
  normalizeAddress,
} from 'web3-adapter/src/balance.web';

const originalFetch = globalThis.fetch;

function mockFetch(handler: (url: string, init?: RequestInit) => Promise<unknown>) {
  const calls: Array<{ url: string; body?: string }> = [];
  const fetchMock = jest.fn(async (input: unknown, init?: RequestInit) => {
    const url = String(input);
    calls.push({ url, body: init?.body as string | undefined });
    const payload = await handler(url, init);
    return { ok: true, json: async () => payload } as Response;
  });
  globalThis.fetch = fetchMock as unknown as typeof fetch;
  return { fetchMock, calls };
}

afterEach(() => {
  globalThis.fetch = originalFetch;
  jest.restoreAllMocks();
});

describe('web JSON-RPC balance path', () => {
  it('reads a balance as bigint from eth_getBalance', async () => {
    const { calls } = mockFetch(async () => ({ result: '0x501eb617f2372aeb' }));
    const wei = await getNativeBalance('0xd8da6bf26964af9d7eed9e03e53415d37aa96045', 1);
    expect(wei).toBe(5773251986299759339n);
    expect(calls[0].url).toBe('https://eth.drpc.org');
    expect(JSON.parse(calls[0].body as string)).toMatchObject({ method: 'eth_getBalance', params: ['0xd8da6bf26964af9d7eed9e03e53415d37aa96045', 'latest'] });
  });

  it('reads the latest block number', async () => {
    mockFetch(async () => ({ result: '0x19066d7' }));
    await expect(getLatestBlockNumber(1)).resolves.toBe(26240727n);
  });

  it('fails over to the second endpoint when the first fails', async () => {
    const { calls } = mockFetch(async (url) => {
      if (url.includes('drpc.org')) throw new Error('network down');
      return { result: '0x10' };
    });
    await expect(getNativeBalance('0x9858effd232b4033e47d90003d41ec34ecaeda94', 1)).resolves.toBe(16n);
    expect(calls.map((c) => c.url)).toEqual(['https://eth.drpc.org', 'https://ethereum-rpc.publicnode.com']);
  });

  it('surfaces the last error when every endpoint fails', async () => {
    mockFetch(async () => { throw new Error('network down'); });
    await expect(getLatestBlockNumber(1)).rejects.toThrow('network down');
  });

  it('rejects JSON-RPC errors instead of returning a bogus balance', async () => {
    mockFetch(async () => ({ error: { code: -32000, message: 'boom' } }));
    await expect(getLatestBlockNumber(11155111)).rejects.toThrow('boom');
  });
});

describe('web address helpers (no keccak in the web chunk)', () => {
  it('validates 20-byte hex addresses and rejects malformed input', () => {
    expect(isValidAddress('0x9858EfFD232B4033E47d90003D41EC34EcaEda94')).toBe(true);
    expect(isValidAddress('0x9858effd232b4033e47d90003d41ec34ecaeda94')).toBe(true);
    expect(isValidAddress('0x123')).toBe(false);
    expect(isValidAddress('not-an-address')).toBe(false);
  });

  it('canonicalizes to lowercase (JSON-RPC form)', () => {
    expect(normalizeAddress('0x9858EfFD232B4033E47d90003D41EC34EcaEda94')).toBe('0x9858effd232b4033e47d90003d41ec34ecaeda94');
    expect(normalizeAddress('0x123')).toBeNull();
  });

  it('shares the display formatting with native', () => {
    expect(formatNativeBalance(1n)).toBe('<0.0001');
    expect(formatNativeBalance(5773251986299759339n)).toBe('5.7732');
  });
});
