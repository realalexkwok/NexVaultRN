// Store invariant tests (review-fix loop 2 / review round 5 MINOR-4): setSelectedChain is the only guard against
// Home rendering getChainConfig() with an unregistered id (which would throw during render).
import { beforeEach, describe, expect, it } from '@jest/globals';
import { SUPPORTED_CHAINS } from 'web3-adapter';

import { useObserverStore } from './observer';

describe('observer store', () => {
  beforeEach(() => {
    useObserverStore.setState({ selectedChainId: 1, watchedAddresses: [] });
  });

  it('ignores unregistered chain ids', () => {
    useObserverStore.getState().setSelectedChain(999999);
    expect(useObserverStore.getState().selectedChainId).toBe(1);
  });

  it('accepts every registered chain id', () => {
    for (const chain of SUPPORTED_CHAINS) {
      useObserverStore.getState().setSelectedChain(chain.chainId);
      expect(useObserverStore.getState().selectedChainId).toBe(chain.chainId);
    }
  });

  it('keeps a single watched address and clears on null', () => {
    const address = '0x9858EfFD232B4033E47d90003D41EC34EcaEda94';
    useObserverStore.getState().setWatchedAddress(address);
    expect(useObserverStore.getState().watchedAddresses).toEqual([address]);
    useObserverStore.getState().setWatchedAddress(null);
    expect(useObserverStore.getState().watchedAddresses).toEqual([]);
  });
});
