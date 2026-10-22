import CryptoKit
import XCTest
@testable import SecurityIos

/**
 * TC-SI-009 / TC-SI-010 (Mac-executed): blob structure parity with the Android vector set and the
 * codec's fail-closed paths. The Keychain KEK read is device-gated and covered by MS-4 in
 * MAC-HANDOFF.md, not here.
 */
final class SecurityIosTests: XCTestCase {
  private let aad = Data("0x9858EfFD232B4033E47d90003D41EC34EcaEda94|demo-user".utf8)

  private func plaintext(_ count: Int = 32) -> Data {
    Data((0..<count).map { UInt8(($0 + 1) % 256) })
  }

  func testBlobLayoutMatchesAndroid() throws {
    let key = SymmetricKey(size: .bits256)
    let iv = Data((0..<12).map { UInt8($0 + 1) })
    let blob = try AesGcmEnvelope.seal(key: key, plaintext: plaintext(), aad: aad, iv: iv)

    XCTAssertEqual(blob.count, 1 + 12 + 32 + 16)
    XCTAssertEqual(blob.first, AesGcmEnvelope.version)

    let parsed = try AesGcmEnvelope.parse(blob)
    XCTAssertEqual(parsed.iv, iv)
    XCTAssertEqual(parsed.ciphertext.count, 32 + 16)
  }

  func testRoundtripReturnsTheOriginalPlaintext() throws {
    let key = SymmetricKey(size: .bits256)
    let input = plaintext(48)
    let blob = try AesGcmEnvelope.seal(key: key, plaintext: input, aad: aad)
    let parsed = try AesGcmEnvelope.parse(blob)
    XCTAssertEqual(try AesGcmEnvelope.open(key: key, parsed: parsed, aad: aad), input)
  }

  func testWrongAadFailsClosed() throws {
    let key = SymmetricKey(size: .bits256)
    let blob = try AesGcmEnvelope.seal(key: key, plaintext: plaintext(), aad: aad)
    let parsed = try AesGcmEnvelope.parse(blob)
    let wrongAad = Data("0x9858EfFD232B4033E47d90003D41EC34EcaEda94|another-user".utf8)
    XCTAssertThrowsError(try AesGcmEnvelope.open(key: key, parsed: parsed, aad: wrongAad)) { error in
      XCTAssertEqual(error as? SecurityIosError, .blobCorrupt)
    }
  }

  func testTamperedTagAndVersionFailClosed() throws {
    let key = SymmetricKey(size: .bits256)
    var blob = try AesGcmEnvelope.seal(key: key, plaintext: plaintext(), aad: aad)
    blob[blob.count - 1] ^= 0x80
    let parsed = try AesGcmEnvelope.parse(blob)
    XCTAssertThrowsError(try AesGcmEnvelope.open(key: key, parsed: parsed, aad: aad))

    var wrongVersion = try AesGcmEnvelope.seal(key: key, plaintext: plaintext(), aad: aad)
    wrongVersion[0] = 2
    XCTAssertThrowsError(try AesGcmEnvelope.parse(wrongVersion))
  }

  func testTruncatedBlobFailsClosed() throws {
    let key = SymmetricKey(size: .bits256)
    let blob = try AesGcmEnvelope.seal(key: key, plaintext: plaintext(), aad: aad)
    XCTAssertThrowsError(try AesGcmEnvelope.parse(blob.prefix(1 + 12 + 15)))
    XCTAssertThrowsError(try AesGcmEnvelope.parse(Data()))
  }
}
