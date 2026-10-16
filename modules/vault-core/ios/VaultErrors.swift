import ExpoModulesCore

/// Fixed vault-core error codes (spec: vault-core §2.4). Identical to the Android module so the
/// JS layer branches on `error.code` cross-platform. Messages never carry key fragments (R5).
enum VaultErrorCode {
  static let invalidMnemonic = "E_VAULT_INVALID_MNEMONIC"
  static let pbkdf2Unavailable = "E_VAULT_PBKDF2_UNAVAILABLE"
  static let invalidPath = "E_VAULT_INVALID_PATH"
  static let invalidDigestLength = "E_VAULT_INVALID_DIGEST_LENGTH"
  static let handleNotFound = "E_VAULT_HANDLE_NOT_FOUND"
  static let cryptoInternal = "E_VAULT_CRYPTO_INTERNAL"
}

/// Thrown through the Expo bridge with an explicit `code`, mirroring Android's `CodedException`.
final class VaultCodedException: Exception {
  init(code: String, reason: String) {
    super.init(name: code, description: reason, code: code)
  }
}

func vaultException(_ code: String, _ reason: String) -> VaultCodedException {
  VaultCodedException(code: code, reason: reason)
}
