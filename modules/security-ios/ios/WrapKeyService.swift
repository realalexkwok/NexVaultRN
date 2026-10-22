import CryptoKit
import Foundation

/**
 * Native-internal wrap/unwrap (T3.1: no JS face yet). Each call reads the biometric-gated KEK
 * from the Keychain, which is what triggers Face ID / Touch ID on the device; the plaintext never
 * crosses the JS bridge.
 */
enum WrapKeyService {
  static func wrap(plaintext: Data, aad: Data) throws -> Data {
    let key = try KeychainKek.loadKek()
    return try AesGcmEnvelope.seal(key: key, plaintext: plaintext, aad: aad)
  }

  static func unwrap(blob: Data, aad: Data) throws -> Data {
    let parsed = try AesGcmEnvelope.parse(blob)
    let key = try KeychainKek.loadKek()
    return try AesGcmEnvelope.open(key: key, parsed: parsed, aad: aad)
  }
}
