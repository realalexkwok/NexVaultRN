import { create } from 'zustand';

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
  setSelectedChain: (chainId) => set({ selectedChainId: chainId }),
  setWatchedAddress: (address) => set({ watchedAddresses: address ? [address] : [] }),
}));
