import CryptoKit
import Foundation
import Security

/**
 * Reads the KEK that the T3.1 JS face (src/index.ts) creates through expo-secure-store 57.0.4
 * with requireAuthentication + WHEN_UNLOCKED_THIS_DEVICE_ONLY.
 *
 * Coordinates are taken from the installed dependency, not from memory:
 * SecureStoreModule.swift `query(with:options:requireAuthentication:)` stores
 * requireAuthentication items under service = (keychainService ?? "app") + ":auth" and
 * kSecAttrAccount / kSecAttrGeneric = Data(key.utf8). The T3.1 JS face passes no
 * keychainService, so the item lives under "app:auth" / Data("nexvault.kek.v1".utf8).
 *
 * The item carries a .biometryCurrentSet ACL over WHEN_UNLOCKED_THIS_DEVICE_ONLY, so this read
 * triggers Face ID / Touch ID. TC-SI-011 (SecurityIosTests) asserts the coordinate match with a
 * test item written in the same shape; MAC-HANDOFF MS-3 runs it on the Mac.
 *
 * Zeroization caveat: Swift Data/SymmetricKey are copy-on-write values; the intermediate hex and
 * byte arrays are wiped best-effort and the limitation is recorded [C] (AGENTS.md R6).
 */
enum KeychainKek {
  static let account = "nexvault.kek.v1"
  static let kekSize = 32

  /// (keychainService ?? "app") + ":auth" — expo-secure-store 57.0.4, requireAuthentication=true.
  static let service = "app:auth"

  static func loadKek(
    service: String = KeychainKek.service,
    account: String = KeychainKek.account
  ) throws -> SymmetricKey {
    let encodedKey = Data(account.utf8)
    var query: [String: Any] = [
      kSecClass as String: kSecClassGenericPassword,
      kSecAttrService as String: service,
      kSecAttrGeneric as String: encodedKey,
      kSecAttrAccount as String: encodedKey,
      kSecReturnData as String: true,
      kSecMatchLimit as String: kSecMatchLimitOne,
      // Deprecated since iOS 14 but still honoured; LAContext.localizedReason is read-only, so the
      // operation prompt remains the supported way to label a Keychain ACL read.
      kSecUseOperationPrompt as String: "Unlock the wallet key"
    ]

    var item: CFTypeRef?
    let status = SecItemCopyMatching(query as CFDictionary, &item)
    guard status == errSecSuccess, let raw = item as? Data else {
      switch status {
      case errSecInteractionNotAllowed: throw SecurityIosError.keychainInaccessible
      case errSecUserCanceled: throw SecurityIosError.authCancelled
      case errSecAuthFailed: throw SecurityIosError.authFailed
      default: throw SecurityIosError.kekMissing
      }
    }

    guard let text = String(data: raw, encoding: .utf8), text.count == kekSize * 2 else {
      throw SecurityIosError.kekMissing
    }
    var bytes = [UInt8]()
    bytes.reserveCapacity(kekSize)
    var index = text.startIndex
    while index < text.endIndex {
      let next = text.index(index, offsetBy: 2)
      guard let byte = UInt8(text[index..<next], radix: 16) else {
        throw SecurityIosError.kekMissing
      }
      bytes.append(byte)
      index = next
    }
    defer {
      for i in bytes.indices {
        bytes[i] = 0
      }
    }
    return SymmetricKey(data: Data(bytes))
  }
}
