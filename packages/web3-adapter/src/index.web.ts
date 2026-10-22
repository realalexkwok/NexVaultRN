// packages/web3-adapter — web entry (R7: observer-only build). Same ./public-api surface as the
// native entry, so platform barrels cannot drift (review MINOR-6). §2.5's web-only extras
// (getTransactionStatus, buildUnsignedTxQrPayload) are T4.x.
export * from './public-api';
