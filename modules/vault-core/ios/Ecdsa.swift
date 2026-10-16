import Foundation
import P256K

/// RFC6979 deterministic ECDSA over secp256k1 (T1.5).
///
/// The nonce generator is libsecp256k1's built-in RFC6979 (`secp256k1_ecdsa_sign_recoverable` with
/// `noncefp == NULL`, owner ruling 2026-10-16 Q2): the k bytes live in library-internal memory
/// that cannot be `memset_s`-wiped, so the zeroization review records them as a **[C]** caveat.
/// Low-s normalization **and the recid flip** required by the T1.4 BLOCKER-1 fix are performed
/// inside the library; the XCTest oracle (TC-VC-027/028) asserts byte-equality with Android.
enum Ecdsa {
  static let digestLength = 32
  static let signatureLength = 65

  /// Fails fast (before any store access) when the digest is not exactly 32 bytes.
  static func requireDigest32(_ digest: [UInt8]) throws {
    guard digest.count == digestLength else {
      throw vaultException(VaultErrorCode.invalidDigestLength, "Invalid digest length")
    }
  }

  /// 65 bytes `r(32) ‖ s(32) ‖ recid(1)` for a 32-byte private key and a 32-byte digest.
  static func sign(privateKey: [UInt8], digest32: [UInt8]) throws -> [UInt8] {
    try requireDigest32(digest32)
    guard privateKey.count == digestLength else { throw Bip32.cryptoInternal() }
    let key: P256K.Recovery.PrivateKey
    do {
      key = try P256K.Recovery.PrivateKey(dataRepresentation: privateKey, format: .compressed)
    } catch {
      throw Bip32.cryptoInternal()
    }
    let signature = key.signature(for: P256K.HashDigest(digest32))
    let compact = signature.compactRepresentation
    let recoveryId = compact.recoveryId
    guard recoveryId >= 0, recoveryId <= 3 else { throw Bip32.cryptoInternal() }
    var output = [UInt8](compact.signature)
    guard output.count == 64 else { throw Bip32.cryptoInternal() }
    output.append(UInt8(recoveryId))
    return output
  }

  /// Derives the child key at [indices] from the caller-owned [seed] and signs [digest32].
  static func signDigest(seed: [UInt8], indices: [UInt32], digest32: [UInt8]) throws -> [UInt8] {
    try requireDigest32(digest32)
    var privateKey = try Bip32.derivePrivateKey(seed: seed, indices: indices)
    defer { wipe(&privateKey) }
    return try sign(privateKey: privateKey, digest32: digest32)
  }

  /// Recovery used by the tests (and available for future diagnostics): public key from a
  /// 65-byte signature. Uncompressed by default so it can be compared with the signer's key.
  static func recoverPublicKey(digest32: [UInt8], signature: [UInt8], compressed: Bool = false) throws -> [UInt8] {
    try requireDigest32(digest32)
    guard signature.count == signatureLength else { throw Bip32.cryptoInternal() }
    var compact = Array(signature[0..<64])
    defer { wipe(&compact) }
    let recoveryId = Int32(signature[64])
    let ecdsaSignature: P256K.Recovery.ECDSASignature
    do {
      ecdsaSignature = try P256K.Recovery.ECDSASignature(compactRepresentation: compact, recoveryId: recoveryId)
    } catch {
      throw Bip32.cryptoInternal()
    }
    let publicKey = P256K.Recovery.PublicKey(
      P256K.HashDigest(digest32),
      signature: ecdsaSignature,
      format: compressed ? .compressed : .uncompressed
    )
    return [UInt8](publicKey.dataRepresentation)
  }
}
