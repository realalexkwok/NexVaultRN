import XCTest
@testable import VaultCoreCore

/// TC-VC-020/021/022/023/027/028 — oracle-transcribed from the committed Android EcdsaTest.kt.
/// These are the cross-platform gates: the six pinned signatures must be byte-identical to the
/// Android outputs, including the low-s normalizing cases whose recid is the flipped value
/// (T1.4 BLOCKER-1).
final class EcdsaTests: XCTestCase {
  // TC-VC-020
  func testProbeVectorKAT() throws {
    let digest = TestSupport.sha256(Array("sample".utf8))
    let signature = try Ecdsa.sign(privateKey: Hex.bytes(Vectors.probePrivateKey), digest32: digest)
    XCTAssertEqual(signature.count, 65)
    XCTAssertEqual(Hex.string(Array(signature[0..<32])), Vectors.probeR)
    XCTAssertEqual(Hex.string(Array(signature[32..<64])), Vectors.probeS)
    XCTAssertEqual(signature[64], 0)
  }

  // TC-VC-021 / TC-VC-022 / TC-VC-027
  func testPinnedSignaturesDeterminismAndLowS() throws {
    let seed = try TestSupport.canonicalSeed()
    let indices = try Bip32.parseEthereumPath(Vectors.canonicalPath)
    var sawNormalized = false
    var sawPlain = false
    for vector in Vectors.pinnedSignatures {
      let digest = Bip32.keccak256(Array(vector.message.utf8))
      let signature = try Ecdsa.signDigest(seed: seed, indices: indices, digest32: digest)
      XCTAssertEqual(signature.count, 65, vector.message)
      XCTAssertEqual(Hex.string(signature), vector.signature, "pinned " + vector.message)
      let s = Array(signature[32..<64])
      XCTAssertLessThanOrEqual(Bip32.compare(s, TestSupport.halfOrder), 0, "low-s " + vector.message)
      XCTAssertLessThanOrEqual(signature[64], 1, "recid " + vector.message)
      if vector.normalized { sawNormalized = true } else { sawPlain = true }
    }
    XCTAssertTrue(sawNormalized, "pinned set must include a normalizing case")
    XCTAssertTrue(sawPlain, "pinned set must include a non-normalizing case")

    let digest = Bip32.keccak256(Array(Vectors.digestMessage.utf8))
    let first = try Ecdsa.signDigest(seed: seed, indices: indices, digest32: digest)
    let second = try Ecdsa.signDigest(seed: seed, indices: indices, digest32: digest)
    XCTAssertEqual(Hex.string(first), Hex.string(second), "RFC6979 determinism")
    XCTAssertEqual(Hex.string(first), Vectors.canonicalSignature, "canonical signature")
  }

  // TC-VC-028
  func testRecidRecoversTheSigningKeyForManyDigests() throws {
    let seed = try TestSupport.canonicalSeed()
    let indices = try Bip32.parseEthereumPath(Vectors.canonicalPath)
    let privateKey = try Bip32.derivePrivateKey(seed: seed, indices: indices)
    let expected = try Bip32.uncompressedPublicKey(privateKey: privateKey)
    for index in 0..<24 {
      let digest = Bip32.keccak256(Array(("nexvault-recovery-" + String(index)).utf8))
      let signature = try Ecdsa.signDigest(seed: seed, indices: indices, digest32: digest)
      let recovered = try Ecdsa.recoverPublicKey(digest32: digest, signature: signature)
      XCTAssertEqual(Hex.string(recovered), Hex.string(expected), "digest #" + String(index))
    }
  }

  // TC-VC-023
  func testDigestLengthNegatives() {
    for size in [0, 31, 33] {
      XCTAssertThrowsError(try Ecdsa.sign(privateKey: Hex.bytes(Vectors.probePrivateKey), digest32: [UInt8](repeating: 0, count: size))) { error in
        XCTAssertEqual((error as? VaultCodedException)?.code, VaultErrorCode.invalidDigestLength)
      }
    }
  }

  // TC-VC-023
  func testErrorCodeTableMatchesAndroid() {
    XCTAssertEqual(VaultErrorCode.invalidMnemonic, "E_VAULT_INVALID_MNEMONIC")
    XCTAssertEqual(VaultErrorCode.pbkdf2Unavailable, "E_VAULT_PBKDF2_UNAVAILABLE")
    XCTAssertEqual(VaultErrorCode.invalidPath, "E_VAULT_INVALID_PATH")
    XCTAssertEqual(VaultErrorCode.invalidDigestLength, "E_VAULT_INVALID_DIGEST_LENGTH")
    XCTAssertEqual(VaultErrorCode.handleNotFound, "E_VAULT_HANDLE_NOT_FOUND")
    XCTAssertEqual(VaultErrorCode.cryptoInternal, "E_VAULT_CRYPTO_INTERNAL")
  }
}
