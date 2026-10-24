import Foundation

/**
 * Fixed E_SEC_* codes for the T3.1 iOS half (spec: security-ios section 2.4). Messages never carry
 * key material, AAD or blob bytes (AGENTS.md R5).
 */
enum SecurityIosError: String, Error {
  case noLockscreen = "E_SEC_NO_LOCKSCREEN"
  case keychainInaccessible = "E_SEC_IOS_KEYCHAIN_INACCESSIBLE"
  case kekMissing = "E_SEC_IOS_KEK_MISSING"
  case blobCorrupt = "E_SEC_IOS_BLOB_CORRUPT"
  case authFailed = "E_SEC_IOS_AUTH_FAILED"
  case authCancelled = "E_SEC_IOS_AUTH_CANCELLED"
  case unsupported = "E_SEC_UNSUPPORTED_OPERATION"

  var message: String {
    switch self {
    case .noLockscreen: return "A device passcode is required"
    case .keychainInaccessible: return "Keychain item is inaccessible while the device is locked"
    case .kekMissing: return "KEK is missing or was invalidated"
    case .blobCorrupt: return "Wrap blob failed its integrity check"
    case .authFailed: return "Biometric authentication failed"
    case .authCancelled: return "Biometric authentication was cancelled"
    case .unsupported: return "Operation is not supported"
    }
  }
}

enum RandomBytes {
  /// Cryptographically random bytes; a SecRandomCopyBytes failure fails closed.
  static func data(_ count: Int) throws -> Data {
    var bytes = [UInt8](repeating: 0, count: count)
    let status = SecRandomCopyBytes(kSecRandomDefault, count, &bytes)
    guard status == errSecSuccess else { throw SecurityIosError.unsupported }
    return Data(bytes)
  }
}

/**
 * fail-closed gate denial (spec: security-ios section 2.4, E_SEC_GATE_DENIED).
 *
 * Public because the vault-core bridge lives in a separate pod: it catches this value and
 * re-labels the denial as the frozen E_VAULT_SECURITY_GATE code seen by JS. Messages are fixed
 * templates and never carry detection detail (AGENTS.md R5).
 */
public struct SecurityGateDenied: Error, Equatable {
  public static let code = "E_SEC_GATE_DENIED"
  public static let message = "Security gate denied the operation"
}

