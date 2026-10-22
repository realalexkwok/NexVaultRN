import { getChainConfig } from './chains';
import type { RpcOptions } from './types';

export { formatNativeBalance } from './format';
export { isValidAddress, normalizeAddress } from './address.web';

// Web read path: a minimal JSON-RPC client over fetch. Deliberately no viem — importing the viem
// root drags @noble/curves, @scure/bip32/bip39 and the wallet actions into the web chunk, which
// module AC-6 / AGENTS R7 forbid. Order-based failover mirrors the native transport (§2.2).
interface JsonRpcResponse {
  result?: string;
  error?: { code: number; message: string };
}

const DEFAULT_TIMEOUT_MS = 10_000;

async function rpcCall(url: string, method: string, params: unknown[], options: RpcOptions): Promise<string> {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({ jsonrpc: '2.0', id: 1, method, params }),
    // A stalled endpoint must fail (and fail over) instead of leaving the query in isLoading
    // forever — React Query retry cannot abort an in-flight fetch (review轮 2 MINOR-2).
    signal: AbortSignal.timeout(options.timeoutMs ?? DEFAULT_TIMEOUT_MS),
  });
  if (!response.ok) throw new Error(`RPC HTTP ${response.status} from ${url}`);
  const payload = (await response.json()) as JsonRpcResponse;
  if (payload.error) throw new Error(`RPC ${method} error ${payload.error.code}: ${payload.error.message}`);
  if (typeof payload.result !== 'string') throw new Error(`RPC ${method} returned no result`);
  return payload.result;
}

async function callWithFailover(chainId: number, method: string, params: unknown[], options: RpcOptions): Promise<bigint> {
  const { rpcUrls } = getChainConfig(chainId);
  let lastError: unknown;
  for (const url of rpcUrls) {
    try {
      return BigInt(await rpcCall(url, method, params, options));
    } catch (error) {
      lastError = error;
    }
  }
  throw lastError instanceof Error ? lastError : new Error('RPC unavailable');
}

export async function getNativeBalance(address: `0x${string}`, chainId: number, options: RpcOptions = {}): Promise<bigint> {
  return callWithFailover(chainId, 'eth_getBalance', [address, 'latest'], options);
}

export async function getLatestBlockNumber(chainId: number, options: RpcOptions = {}): Promise<bigint> {
  return callWithFailover(chainId, 'eth_blockNumber', [], options);
}
