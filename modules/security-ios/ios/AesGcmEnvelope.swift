import CryptoKit
import Foundation

/**
 * Wrap-blob codec shared with Android (spec: security-ios section 2.2):
 * version(1B) = 1, IV(12B), ciphertext, GCM tag(16B). AAD binds address + userId (public only).
 *
 * Pure CryptoKit AES-GCM, so the structure and decrypt-interop can be exercised by XCTest on the
 * Mac; the biometric KEK read (KeychainKek) is the device-only half.
 */
enum AesGcmEnvelope {
  static let version: UInt8 = 1
  static let ivSize = 12
  static let tagSize = 16

  struct Parsed {
    let iv: Data
    let ciphertext: Data
    let tag: Data
  }

  static func parse(_ blob: Data) throws -> Parsed {
    guard blob.count >= 1 + ivSize + tagSize else { throw SecurityIosError.blobCorrupt }
    let bytes = [UInt8](blob)
    guard bytes[0] == version else { throw SecurityIosError.blobCorrupt }
    let iv = Data(bytes[1..<(1 + ivSize)])
    let body = Data(bytes[(1 + ivSize)..<bytes.count])
    guard body.count >= tagSize else { throw SecurityIosError.blobCorrupt }
    let ciphertext = body.prefix(body.count - tagSize)
    let tag = body.suffix(tagSize)
    return Parsed(iv: iv, ciphertext: Data(ciphertext), tag: Data(tag))
  }

  /// Seals [plaintext] with a random 12-byte IV (or an injected one for tests).
  static func seal(key: SymmetricKey, plaintext: Data, aad: Data, iv: Data? = nil) throws -> Data {
    let ivBytes = try iv ?? RandomBytes.data(ivSize)
    let nonce = try AES.GCM.Nonce(data: ivBytes)
    let sealed = try AES.GCM.seal(plaintext, using: key, nonce: nonce, authenticating: aad)
    var blob = Data([version])
    blob.append(ivBytes)
    blob.append(sealed.ciphertext)
    blob.append(sealed.tag)
    return blob
  }

  /// Opens the envelope; any tag or AAD mismatch fails closed with E_SEC_IOS_BLOB_CORRUPT.
  static func open(key: SymmetricKey, parsed: Parsed, aad: Data) throws -> Data {
    do {
      let nonce = try AES.GCM.Nonce(data: parsed.iv)
      let box = try AES.GCM.SealedBox(nonce: nonce, ciphertext: parsed.ciphertext, tag: parsed.tag)
      return try AES.GCM.open(box, using: key, authenticating: aad)
    } catch let error as SecurityIosError {
      throw error
    } catch {
      throw SecurityIosError.blobCorrupt
    }
  }
}
