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

/** T1.4: signDigest input digest is not exactly 32 bytes. */
internal class InvalidDigestLengthException : CodedException(
  "E_VAULT_INVALID_DIGEST_LENGTH",
  "Invalid digest length",
  null
)

/** T1.3: derivation path outside the BIP-44 EVM whitelist (syntax class echo only). */
internal class InvalidPathException : CodedException(
  "E_VAULT_INVALID_PATH",
  "Invalid derivation path",
  null
)

/** T1.3: handle missing from the native store (never distinguishes empty store from bad handle). */
internal class HandleNotFoundException : CodedException(
  "E_VAULT_HANDLE_NOT_FOUND",
  "Handle not found",
  null
)

/** T1.3: underlying BIP-32/keccak primitive failed (never carries key fragments). */
internal class CryptoInternalException : CodedException(
  "E_VAULT_CRYPTO_INTERNAL",
  "Cryptographic operation failed",
  null
)

/**
 * T3.2: the security detection gate denied importMnemonic/signDigest (spec: vault-core section
 * 2.4). Fixed template -- the security module's signal details never cross this boundary (R5).
 */
internal class SecurityGateException : CodedException(
  "E_VAULT_SECURITY_GATE",
  "Security gate denied signing operation",
  null
)
