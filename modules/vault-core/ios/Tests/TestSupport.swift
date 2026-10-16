import CommonCrypto
import Foundation
@testable import VaultCore

enum Hex {
  static func bytes(_ hex: String) -> [UInt8] {
    var output = [UInt8]()
    var index = hex.startIndex
    while index < hex.endIndex {
      let next = hex.index(index, offsetBy: 2, limitedBy: hex.endIndex) ?? hex.endIndex
      output.append(UInt8(String(hex[index..<next]), radix: 16) ?? 0)
      index = next
    }
    return output
  }

  static func string(_ bytes: [UInt8]) -> String {
    let digits = Array("0123456789abcdef")
    var output = ""
    output.reserveCapacity(bytes.count * 2)
    for byte in bytes {
      output.append(digits[Int(byte >> 4)])
      output.append(digits[Int(byte & 0x0F)])
    }
    return output
  }
}

/// Test-only helpers: CommonCrypto hashing, Base58Check and the 78-byte extended-key layout
/// (test-side formatting only, mirroring the Kotlin oracle serializer).
enum TestSupport {
  static func sha256(_ bytes: [UInt8]) -> [UInt8] {
    var digest = [UInt8](repeating: 0, count: Int(CC_SHA256_DIGEST_LENGTH))
    CC_SHA256(bytes, CC_LONG(bytes.count), &digest)
    return digest
  }

  static func wordlist() -> [String] {
    Bip39.loadWordlist()
  }

  static func wordlistSha256Hex() -> String {
    let bundle = Bundle(for: VaultCoreModule.self)
    guard let url = bundle.url(forResource: "bip39_english", withExtension: "txt"),
          let data = try? Data(contentsOf: url) else { return "MISSING" }
    return Hex.string(sha256([UInt8](data)))
  }

  /// n >> 1 — the low-s boundary.
  static let halfOrder: [UInt8] = {
    var value = Bip32.order
    var carry: UInt8 = 0
    for index in 0..<value.count {
      let nextCarry = value[index] & 1
      value[index] = (value[index] >> 1) | (carry << 7)
      carry = nextCarry
    }
    return value
  }()

  static func base58Check(_ payload: [UInt8]) -> String {
    let first = sha256(payload)
    let second = sha256(first)
    var checksummed = payload
    checksummed.append(contentsOf: second[0..<4])
    return base58Encode(checksummed)
  }

  private static func base58Encode(_ input: [UInt8]) -> String {
    let alphabet = Array("123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz")
    var digits = [Int]()
    var working = input
    while !working.isEmpty {
      var remainder = 0
      var quotient = [UInt8]()
      quotient.reserveCapacity(working.count)
      for byte in working {
        let value = remainder * 256 + Int(byte)
        let digit = value / 58
        remainder = value % 58
        if !quotient.isEmpty || digit != 0 {
          quotient.append(UInt8(digit))
        }
      }
      digits.append(remainder)
      working = quotient
    }
    var output = ""
    for byte in input {
      if byte != 0 { break }
      output.append(alphabet[0])
    }
    for digit in digits.reversed() {
      output.append(alphabet[digit])
    }
    return output
  }

  /// 78-byte extended key payload (xprv/xpub prefixes) + Base58Check.
  static func serializeExtendedKey(_ node: Bip32Node, isPrivate: Bool) throws -> String {
    var payload = [UInt8](repeating: 0, count: 78)
    if isPrivate {
      payload[0] = 0x04
      payload[1] = 0x88
      payload[2] = 0xAD
      payload[3] = 0xE4
    } else {
      payload[0] = 0x04
      payload[1] = 0x88
      payload[2] = 0xB2
      payload[3] = 0x1E
    }
    payload[4] = node.depth
    for offset in 0..<4 {
      payload[5 + offset] = node.parentFingerprint[offset]
    }
    let child = Bip32.ser32(node.childNumber)
    for offset in 0..<4 {
      payload[9 + offset] = child[offset]
    }
    for offset in 0..<32 {
      payload[13 + offset] = node.chainCode[offset]
    }
    if isPrivate {
      payload[45] = 0
      for offset in 0..<32 {
        payload[46 + offset] = node.privateKey[offset]
      }
    } else {
      let publicKey = try Bip32.compressedPublicKey(privateKey: node.privateKey)
      for offset in 0..<33 {
        payload[45 + offset] = publicKey[offset]
      }
    }
    return base58Check(payload)
  }

  /// Seed for the canonical mnemonic with the empty passphrase (Android oracle parity).
  static func canonicalSeed() throws -> [UInt8] {
    guard let seed = Bip39.mnemonicToSeed(normalizedMnemonic: Vectors.canonicalMnemonic, passphrase: "") else {
      throw NSError(domain: "TestSupport", code: 1)    
    }
    return seed
  }
}
