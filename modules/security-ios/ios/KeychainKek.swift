import CryptoKit
import Foundation
import LocalAuthentication
import Security

/**
 * Reads the KEK that the T3.1 JS face (src/index.ts) creates through expo-secure-store with
 * requireAuthentication + WHEN_UNLOCKED_THIS_DEVICE_ONLY.
 *
 * Keychain coordinates mirror expo-secure-store's defaults: generic password, service = the app
 * bundle identifier, account = the key name. The stored value is the hex encoding of the 32-byte
 * KEK (SecureStore stores strings). Verification on the Mac/device is MS-3 of MAC-HANDOFF.md; if
 * expo-secure-store's service differs (keychainService option), align the constants there.
 *
 * Zeroization caveat: Swift Data/SymmetricKey are copy-on-write values; the intermediate hex and
 * byte arrays are wiped best-effort and the limitation is recorded [C] (AGENTS.md R6).
 */
enum KeychainKek {
  static let account = "nexvault.kek.v1"
  static let kekSize = 32

  static var service: String {
    Bundle.main.bundleIdentifier ?? "io.github.realalexkwok.nexvault"
  }

  /// Loads and decodes the KEK; a missing/invalidated item fails closed with E_SEC_IOS_KEK_MISSING.
  static func loadKek() throws -> SymmetricKey {
    var query: [String: Any] = [
      kSecClass as String: kSecClassGenericPassword,
      kSecAttrService as String: service,
      kSecAttrAccount as String: account,
      kSecReturnData as String: true,
      kSecMatchLimit as String: kSecMatchLimitOne,
      kSecUseOperationPrompt as String: "Unlock the wallet key"
    ]
    if #available(iOS 13.0, *) {
      query[kSecUseAuthenticationContext as String] = LAContext()
    }

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
