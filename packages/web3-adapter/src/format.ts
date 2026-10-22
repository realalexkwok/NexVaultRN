// Platform-neutral display math (no viem): both the native and the web balance modules re-export
// this, so the web chunk never needs viem just to format a balance (R7 split).
function formatUnits(value: bigint, decimals: number): string {
  const negative = value < 0n;
  const absolute = negative ? -value : value;
  const base = 10n ** BigInt(decimals);
  const whole = absolute / base;
  const fraction = absolute % base;
  const sign = negative ? '-' : '';
  if (fraction === 0n) return sign + whole.toString();
  const fractionDigits = fraction.toString().padStart(decimals, '0').replace(/0+$/, '');
  return `${sign}${whole}.${fractionDigits}`;
}

/**
 * Display helper: wei -> decimal string with at most 4 fraction digits, trailing zeros stripped.
 * Precision is lost only here, at the render edge (spec §2.5; AC-12 / TC-W3A-013). A nonzero
 * balance below the display precision renders `<0.0001`, never a false zero (review NIT-9).
 */
export function formatNativeBalance(wei: bigint, decimals = 18): string {
  const [whole, fraction] = formatUnits(wei, decimals).split('.');
  if (!fraction) return whole;
  const trimmed = fraction.slice(0, 4).replace(/0+$/, '');
  if (trimmed.length > 0) return `${whole}.${trimmed}`;
  // e.g. 5.0000 -> "5"; 1 wei -> "<0.0001" (not "0"); exact zero still renders "0".
  return whole === '0' && wei > 0n ? '<0.0001' : whole;
}
