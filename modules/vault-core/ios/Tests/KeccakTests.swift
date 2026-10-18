import XCTest
@testable import VaultCoreCore

/// TC-VC-015 — keccak-256 (pre-NIST) and EIP-55 gates, oracle-transcribed from Bip32Test.kt.
final class KeccakTests: XCTestCase {
  // TC-VC-015
  func testKeccak256Vectors() {
    XCTAssertEqual(Hex.string(Bip32.keccak256([])), Vectors.keccakEmpty)
    XCTAssertEqual(Hex.string(Bip32.keccak256(Array("abc".utf8))), Vectors.keccakAbc)
  }

  // TC-VC-015
  func testErc55Vectors() throws {
    for vector in Vectors.erc55 {
      let addressBytes = Hex.bytes(String(vector.lower.dropFirst(2)))
      XCTAssertEqual(try Bip32.checksumAddress(addressBytes: addressBytes), vector.checksummed)
    }
  }
}
