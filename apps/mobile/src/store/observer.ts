import { create } from 'zustand';

import { SUPPORTED_CHAINS, normalizeAddress } from 'web3-adapter';

// Observed-wallet store (T2.2). Public data only: a chain id and watched addresses (R1 — no key
// material ever enters the store; nothing here is sensitive). Session-only by design: no
// persistence dependency is R4-approved (AsyncStorage/SecureStore would each need an amendment —
// recorded as a T2.2 Handoff). The UI shows one field (owner Q4-A); the list shape keeps the
// chain switch and a future multi-watch list from needing a schema change.
export interface ObserverState {
  selectedChainId: number;
  watchedAddresses: string[];
  setSelectedChain: (chainId: number) => void;
  /** null clears the watched list (input emptied). */
  setWatchedAddress: (address: string | null) => void;
}

export const useObserverStore = create<ObserverState>()((set) => ({
  selectedChainId: 1, // Ethereum mainnet — the balance-walk anchor chain
  watchedAddresses: [],
  setSelectedChain: (chainId) => {
    // Ignore unregistered ids: Home renders getChainConfig(selectedChainId), which throws for an
    // unknown chain, so the store is the invariant boundary (review MINOR-5).
    if (!SUPPORTED_CHAINS.some((candidate) => candidate.chainId === chainId)) return;
    set({ selectedChainId: chainId });
  },
  setWatchedAddress: (address) => {
    // Same boundary as the chain guard: only canonical addresses enter the store, so a future
    // writer (address book, deep link, test) cannot hand an invalid string to the RPC queryFn
    // (review round 1 NIT-9). Empty/null clears; an invalid non-empty value is ignored.
    if (!address) {
      set({ watchedAddresses: [] });
      return;
    }
    const normalized = normalizeAddress(address);
    if (normalized) set({ watchedAddresses: [normalized] });
  },
}));
