// W3A error taxonomy — spec: web3-adapter §2.6. T2.2 ships the read-only subset only: the
// transaction-path codes (CHAIN_MISMATCH / TXHASH_MISMATCH / RPC_DIVERGENCE / BROADCAST_REJECTED /
// REVERTED / VAULT / READ_ONLY_PLATFORM) arrive with T4.x and are intentionally absent — never
// stub-implement them (spec §2.3/§2.4; R1/R7).
export type W3AErrorCode = 'E_W3A_UNKNOWN_CHAIN';

export class W3AError extends Error {
  readonly code: W3AErrorCode;

  constructor(code: W3AErrorCode, message?: string) {
    super(message ?? code);
    this.name = 'W3AError';
    this.code = code;
  }
}

export function isW3AError(value: unknown): value is W3AError {
  return value instanceof W3AError;
}
