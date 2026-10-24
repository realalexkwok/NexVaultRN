import Foundation

/// Fixed vault-core error codes (spec: vault-core §2.4). Identical to the Android module so the JS
/// layer branches on `error.code` cross-platform. Messages never carry key fragments (R5).
///
/// Public because the Expo bridge lives in a separate pod (`VaultCore`) and maps these codes onto
/// the frozen `E_VAULT_*` exception it throws through the bridge.
public enum VaultErrorCode {
  public static let invalidMnemonic = "E_VAULT_INVALID_MNEMONIC"
  public static let pbkdf2Unavailable = "E_VAULT_PBKDF2_UNAVAILABLE"
  public static let invalidPath = "E_VAULT_INVALID_PATH"
  public static let invalidDigestLength = "E_VAULT_INVALID_DIGEST_LENGTH"
  public static let handleNotFound = "E_VAULT_HANDLE_NOT_FOUND"
  public static let cryptoInternal = "E_VAULT_CRYPTO_INTERNAL"
  /// T3.2: environment detection refused the operation (spec: vault-core section 2.4; same code as
  /// the Android SecurityGateException). Fixed template; never carries detection detail (R5).
  public static let securityGate = "E_VAULT_SECURITY_GATE"
}

/// The core's coded error. It deliberately does **not** reference ExpoModulesCore: this pod targets
/// iOS 15.1 so its XCTest bundle can run on an iOS 15.8 device (T1.5-r2), while the Expo bridge pod
/// keeps the iOS 16.4 floor that ExpoModulesCore requires. The bridge converts this value into
/// Expo's `Exception(name:description:code:)` so JS still sees the frozen codes.
public struct VaultCodedException: Error, Equatable {
  public let code: String
  public let reason: String

  public init(code: String, reason: String) {
    self.code = code
    self.reason = reason
  }
}

func vaultException(_ code: String, _ reason: String) -> VaultCodedException {
  VaultCodedException(code: code, reason: reason)
}
