// Platform-neutral request options shared by the native (viem transport) and web (fetch) paths.
// Keeping one type keeps the public arity identical on both platforms (review轮 3 MINOR-1).
export interface RpcOptions {
  /** Per-request ceiling in ms. Native: viem http(timeout); Web: AbortSignal.timeout. */
  timeoutMs?: number;
}
