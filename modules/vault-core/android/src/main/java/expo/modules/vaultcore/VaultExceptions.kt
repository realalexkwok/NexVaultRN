package expo.modules.vaultcore

import expo.modules.kotlin.exception.CodedException

/**
 * Fixed error codes exposed to JS. Messages never contain mnemonic bytes, word
 * indices, seed bytes, or handle material (AGENTS.md R5).
 */
internal class InvalidMnemonicException : CodedException(
  "E_VAULT_INVALID_MNEMONIC",
  "Invalid mnemonic",
  null
)

internal class Pbkdf2UnavailableException : CodedException(
  "E_VAULT_PBKDF2_UNAVAILABLE",
  "PBKDF2-HMAC-SHA512 is unavailable on this device",
  null
)
