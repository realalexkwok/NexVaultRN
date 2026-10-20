import 'react-native-get-random-values';
import 'fastestsmallesttextencoderdecoder';

// Web3 polyfill contract — spec: web3-adapter §2.1. The import order above is a hard contract
// and this module is the FIRST import of the app entry (src/app/_layout.tsx).
// 1. crypto.getRandomValues (native CSPRNG) — R8: Chrome remote debugging degrades this to
//    Math.random; real-money flows must never run under remote debug.
// 2. TextEncoder/TextDecoder defensive polyfill (RN >= 0.74 already ships a native impl;
//    viem's compatibility page recommends the polyfill regardless).
// On web both are no-ops (the platform provides crypto + TextEncoder).
