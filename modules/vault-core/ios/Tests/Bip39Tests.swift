import XCTest
@testable import VaultCoreCore

/// TC-VC-001/002/010 — oracle-transcribed from the committed Android Bip39Test.kt.
final class Bip39Tests: XCTestCase {
  // TC-VC-001
  func testOfficialVectorsValidateAndDeriveTrezorSeeds() throws {
    let wordlist = TestSupport.wordlist()
    XCTAssertEqual(wordlist.count, 2048)
    for vector in Vectors.bip39 {
      XCTAssertTrue(Bip39.validate(normalized: vector.mnemonic, wordlist: wordlist), "validate " + vector.id)
      let seed = try XCTUnwrap(Bip39.mnemonicToSeed(normalizedMnemonic: vector.mnemonic, passphrase: "TREZOR"), "seed " + vector.id)
      XCTAssertEqual(Hex.string(seed), vector.seedHex, "seed " + vector.id)
    }
  }

  // TC-VC-001
  func testEmptyPassphraseSeedDiffersFromTrezorSeed() throws {
    let expected = "5eb00bbddcf069084889a8ab9155568165f5c453ccb85e70811aaed6f6da5fc19a5ac40b389cd370d086206dec8aa6c43daea6690f20ad3d8d48b2d2ce9e38e4"
    let seed = try XCTUnwrap(Bip39.mnemonicToSeed(normalizedMnemonic: Vectors.canonicalMnemonic, passphrase: ""))
    XCTAssertEqual(Hex.string(seed), expected)
    XCTAssertNotEqual(Hex.string(seed), Vectors.bip39[0].seedHex)
  }

  // TC-VC-010
  func testBundledWordlistShapeAndHash() {
    let wordlist = TestSupport.wordlist()
    XCTAssertEqual(wordlist.count, 2048)
    XCTAssertEqual(wordlist.first, "abandon")
    XCTAssertEqual(wordlist.last, "zoo")
    XCTAssertEqual(TestSupport.wordlistSha256Hex(), Vectors.wordlistSha256)
  }

  // TC-VC-002
  func testNegatives() {
    let wordlist = TestSupport.wordlist()
    XCTAssertFalse(Bip39.validate(normalized: Vectors.canonicalMnemonic.replacingOccurrences(of: "about", with: "zzzz"), wordlist: wordlist))
    XCTAssertFalse(Bip39.validate(normalized: Vectors.canonicalMnemonic.replacingOccurrences(of: "about", with: "abandon"), wordlist: wordlist))
    let elevenWords = Vectors.canonicalMnemonic.split(separator: " ").dropLast().joined(separator: " ")
    let thirteenWords = Vectors.canonicalMnemonic + " abandon"
    XCTAssertFalse(Bip39.validate(normalized: elevenWords, wordlist: wordlist))
    XCTAssertFalse(Bip39.validate(normalized: thirteenWords, wordlist: wordlist))
    XCTAssertFalse(Bip39.validate(normalized: Vectors.canonicalMnemonic, wordlist: []))
  }
}
