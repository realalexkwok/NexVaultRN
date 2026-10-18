import CommonCrypto
import Foundation
@_implementationOnly import P256K

/// One BIP-32 node: private scalar + chain code + serialization metadata (Android parity).
struct Bip32Node {
  var privateKey: [UInt8]
  var chainCode: [UInt8]
  let depth: UInt8
  let parentFingerprint: [UInt8]
  let childNumber: UInt32

  /// Wipes both secret buffers **in place** (MINOR-3). A `var copy = node.privateKey` + `wipe(&copy)`
  /// would be a CoW alias and zero only the copy — the trap that BLOCKER-1 fixed in `VaultStore`.
  mutating func destroy() {
    wipe(&privateKey)
    wipe(&chainCode)
    #if DEBUG
    Bip32Node.onDestroyed?(privateKey, chainCode)
    #endif
  }

  #if DEBUG
  /// Test seam (NIT-9): observes the buffers after they were wiped. Never set in production.
  static var onDestroyed: (([UInt8], [UInt8]) -> Void)?
  #endif
}

/// BIP-32 (secp256k1) + BIP-44 EVM path parsing + pre-NIST Keccak-256 + EIP-55 checksum.
///
/// The CKD glue (HMAC-SHA512, ser32, IL + k_par mod n, HASH160 fingerprint) is written here;
/// **all curve arithmetic stays in swift-secp256k1** (`P256K`) per spec: vault-core §2.2 — this
/// file never performs EC point math. Every scratch buffer is wiped in a `defer`.
enum Bip32 {
  static let hardenedOffset: UInt32 = 0x80000000
  /// secp256k1 group order n (big-endian).
  static let order: [UInt8] = [
    0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFE,
    0xBA, 0xAE, 0xDC, 0xE6, 0xAF, 0x48, 0xA0, 0x3B, 0xBF, 0xD2, 0x5E, 0x8C, 0xD0, 0x36, 0x41, 0x41,
  ]
  private static let masterSecret: [UInt8] = Array("Bitcoin seed".utf8)

  // MARK: - Path

  /// BIP-44 EVM whitelist `m/44'/60'/<account>'/0/<index>` (`h` spelling allowed, trimmed),
  /// identical to the Android parser; anything else throws `E_VAULT_INVALID_PATH`.
  static func parseEthereumPath(_ path: String) throws -> [UInt32] {
    let trimmed = path.trimmingCharacters(in: .whitespacesAndNewlines)
    let segments = trimmed.split(separator: "/", omittingEmptySubsequences: false).map(String.init)
    guard segments.count == 6, segments[0] == "m" else { throw invalidPath() }
    guard isHardenedSegment(segments[1], "44"), isHardenedSegment(segments[2], "60") else { throw invalidPath() }
    // The account segment must be hardened (Android parity): without this, `m/44'/60'/0/0/0` parsed
    // successfully and silently derived `m/44'/60'/0'/0/0` (MAJOR-1).
    guard segments[3].hasSuffix("'") || segments[3].hasSuffix("h") else { throw invalidPath() }
    let account = try hardenedIndex(segments[3])
    guard segments[4] == "0" else { throw invalidPath() }
    let index = try plainIndex(segments[5])
    return [44 | hardenedOffset, 60 | hardenedOffset, account | hardenedOffset, 0, index]
  }

  private static func isHardenedSegment(_ segment: String, _ expected: String) -> Bool {
    segment == expected + "'" || segment == expected + "h"
  }

  private static func hardenedIndex(_ segment: String) throws -> UInt32 {
    let digits = segment.hasSuffix("'") || segment.hasSuffix("h") ? String(segment.dropLast()) : segment
    guard let value = UInt32(digits), value < hardenedOffset else { throw invalidPath() }
    return value
  }

  private static func plainIndex(_ segment: String) throws -> UInt32 {
    guard let value = UInt32(segment), value < hardenedOffset else { throw invalidPath() }
    return value
  }

  // MARK: - Derivation

  /// BIP-32 master node from a BIP-39 seed (HMAC-SHA512 with the key "Bitcoin seed").
  static func masterNode(seed: [UInt8]) throws -> Bip32Node {
    var output = hmacSHA512(key: masterSecret, data: seed)
    defer { wipe(&output) }
    var left = Array(output[0..<32])
    defer { wipe(&left) }
    var right = Array(output[32..<64])
    guard isInRange(left) else {
      wipe(&right)
      throw cryptoInternal()
    }
    return Bip32Node(privateKey: left, chainCode: right, depth: 0, parentFingerprint: [0, 0, 0, 0], childNumber: 0)
  }

  /// BIP-32 CKDpriv for one child index (hardened when bit 31 is set).
  static func deriveChild(parent: Bip32Node, childNumber: UInt32) throws -> Bip32Node {
    let hardened = childNumber >= hardenedOffset
    var data = [UInt8]()
    if hardened {
      data.append(0)
      data.append(contentsOf: parent.privateKey)
      data.append(contentsOf: ser32(childNumber))
    } else {
      var publicKey = try compressedPublicKey(privateKey: parent.privateKey)
      data.append(contentsOf: publicKey)
      data.append(contentsOf: ser32(childNumber))
      wipe(&publicKey)
    }

    var output = hmacSHA512(key: parent.chainCode, data: data)
    wipe(&data)
    defer { wipe(&output) }
    var left = Array(output[0..<32])
    defer { wipe(&left) }
    var right = Array(output[32..<64])
    guard compare(left, order) < 0 else {
      wipe(&right)
      throw cryptoInternal()
    }
    guard var childKey = addScalarsModOrder(left, parent.privateKey), !isZero(childKey) else {
      wipe(&right)
      throw cryptoInternal()
    }
    // `fingerprint` can throw; the buffers created above must not survive that path (MINOR-3).
    let parentFingerprintDigest: [UInt8]
    do {
      parentFingerprintDigest = try fingerprint(privateKey: parent.privateKey)
    } catch {
      wipe(&childKey)
      wipe(&right)
      throw error
    }
    return Bip32Node(
      privateKey: childKey,
      chainCode: right,
      depth: parent.depth &+ 1,
      parentFingerprint: parentFingerprintDigest,
      childNumber: childNumber
    )
  }

  /// Derives the child private key at [indices] and returns it (caller wipes).
  static func derivePrivateKey(seed: [UInt8], indices: [UInt32]) throws -> [UInt8] {
    var current = try masterNode(seed: seed)
    do {
      for index in indices {
        let next = try deriveChild(parent: current, childNumber: index)
        current.destroy()          // wipe this node's key + chain code in place, then move on
        current = next
      }
      // Success path: hand the key over and wipe the node's chain code in place. Copying the key
      // into a local and calling destroy() would be the COW trap again (the wipe would hit a copy).
      let key = current.privateKey
      wipe(&current.chainCode)
      return key
    } catch {
      current.destroy()            // throw paths must not leave the node's secrets behind
      throw error
    }
  }

  // MARK: - Scalar arithmetic (no EC point math; spec §2.2)

  /// (a + b) mod n for 32-byte big-endian scalars; nil on malformed input.
  static func addScalarsModOrder(_ a: [UInt8], _ b: [UInt8]) -> [UInt8]? {
    guard a.count == 32, b.count == 32 else { return nil }
    var sum = [UInt8](repeating: 0, count: 33)
    var carry: UInt16 = 0
    for index in stride(from: 31, through: 0, by: -1) {
      let value = UInt16(a[index]) + UInt16(b[index]) + carry
      sum[index + 1] = UInt8(value & 0xFF)
      carry = value >> 8
    }
    sum[0] = UInt8(carry)
    if sum[0] != 0 || compare(Array(sum[1..<33]), order) >= 0 {
      var borrow: Int16 = 0
      for index in stride(from: 32, through: 1, by: -1) {
        let value = Int16(sum[index]) - Int16(order[index - 1]) - borrow
        if value < 0 {
          sum[index] = UInt8(value + 256)
          borrow = 1
        } else {
          sum[index] = UInt8(value)
          borrow = 0
        }
      }
      sum[0] = UInt8(max(0, Int16(sum[0]) - borrow))
    }
    let result = Array(sum[1..<33])
    wipe(&sum)
    return result
  }

  static func isZero(_ scalar: [UInt8]) -> Bool {
    for byte in scalar where byte != 0 { return false }
    return true
  }

  static func isInRange(_ scalar: [UInt8]) -> Bool {
    !isZero(scalar) && compare(scalar, order) < 0
  }

  /// Big-endian lexicographic compare: -1 (a<b), 0 (equal), 1 (a>b).
  static func compare(_ a: [UInt8], _ b: [UInt8]) -> Int {
    let count = min(a.count, b.count)
    for index in 0..<count {
      if a[index] != b[index] { return a[index] < b[index] ? -1 : 1 }
    }
    if a.count == b.count { return 0 }
    return a.count < b.count ? -1 : 1
  }

  static func ser32(_ value: UInt32) -> [UInt8] {
    [UInt8((value >> 24) & 0xFF), UInt8((value >> 16) & 0xFF), UInt8((value >> 8) & 0xFF), UInt8(value & 0xFF)]
  }

  static func hmacSHA512(key: [UInt8], data: [UInt8]) -> [UInt8] {
    var output = [UInt8](repeating: 0, count: Int(CC_SHA512_DIGEST_LENGTH))
    key.withUnsafeBufferPointer { keyBuffer in
      data.withUnsafeBufferPointer { dataBuffer in
        CCHmac(
          CCHmacAlgorithm(kCCHmacAlgSHA512),
          keyBuffer.baseAddress,
          keyBuffer.count,
          dataBuffer.baseAddress,
          dataBuffer.count,
          &output
        )
      }
    }
    return output
  }

  // MARK: - Public keys (swift-secp256k1 only)

  static func compressedPublicKey(privateKey: [UInt8]) throws -> [UInt8] {
    try publicKey(privateKey: privateKey, format: .compressed)
  }

  static func uncompressedPublicKey(privateKey: [UInt8]) throws -> [UInt8] {
    try publicKey(privateKey: privateKey, format: .uncompressed)
  }

  private static func publicKey(privateKey: [UInt8], format: P256K.Format) throws -> [UInt8] {
    do {
      let key = try P256K.Recovery.PrivateKey(dataRepresentation: privateKey, format: format)
      return [UInt8](key.publicKey.dataRepresentation)
    } catch {
      throw cryptoInternal()
    }
  }

  /// First four bytes of HASH160(compressed public key) — BIP-32 parent fingerprint.
  static func fingerprint(privateKey: [UInt8]) throws -> [UInt8] {
    var publicKey = try compressedPublicKey(privateKey: privateKey)
    defer { wipe(&publicKey) }
    var sha = [UInt8](repeating: 0, count: Int(CC_SHA256_DIGEST_LENGTH))
    defer { wipe(&sha) }
    CC_SHA256(publicKey, CC_LONG(publicKey.count), &sha)
    var ripemd = Ripemd160.hash(sha)
    defer { wipe(&ripemd) }
    return Array(ripemd[0..<4])
  }

  // MARK: - Address (pre-NIST Keccak-256 + EIP-55)

  static func keccak256(_ bytes: [UInt8]) -> [UInt8] { Keccak.keccak256(bytes) }

  /// keccak256(x || y)[12..32) of an uncompressed (65-byte, 0x04-prefixed) public key.
  static func addressBytes(uncompressedPublicKey: [UInt8]) throws -> [UInt8] {
    guard uncompressedPublicKey.count == 65, uncompressedPublicKey[0] == 0x04 else { throw cryptoInternal() }
    var xy = Array(uncompressedPublicKey[1..<65])
    defer { wipe(&xy) }
    var digest = Keccak.keccak256(xy)
    defer { wipe(&digest) }
    return Array(digest[12..<32])
  }

  /// EIP-55 mixed-case checksum of a 20-byte address.
  static func checksumAddress(addressBytes: [UInt8]) throws -> String {
    guard addressBytes.count == 20 else { throw cryptoInternal() }
    let digits = Array("0123456789abcdef")
    var lower = ""
    lower.reserveCapacity(40)
    for byte in addressBytes {
      lower.append(digits[Int(byte >> 4)])
      lower.append(digits[Int(byte & 0x0F)])
    }
    var hash = Keccak.keccak256(Array(lower.utf8))
    defer { wipe(&hash) }
    let characters = Array(lower)
    var checksummed = "0x"
    checksummed.reserveCapacity(42)
    for index in 0..<characters.count {
      let character = characters[index]
      let shift: UInt8 = index % 2 == 0 ? 4 : 0
      let nibble = (hash[index / 2] >> shift) & 0x0F
      if character >= "a" && character <= "f" && nibble >= 8 {
        checksummed.append(Character(String(character).uppercased()))
      } else {
        checksummed.append(character)
      }
    }
    return checksummed
  }

  // MARK: - Errors

  static func invalidPath() -> VaultCodedException {
    vaultException(VaultErrorCode.invalidPath, "Invalid derivation path")
  }

  static func cryptoInternal() -> VaultCodedException {
    vaultException(VaultErrorCode.cryptoInternal, "Cryptographic operation failed")
  }
}
