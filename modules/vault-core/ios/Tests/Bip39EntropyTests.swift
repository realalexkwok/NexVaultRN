import XCTest
@testable import VaultCoreCore

/// NIT-1: the BIP-39 vectors were only checked through the derived seed. This test rebuilds each
/// mnemonic from its entropy hex with the bundled wordlist (independent of Bip39.derive), so a wrong
/// entropy/checksum mapping cannot hide behind a matching seed.
final class Bip39EntropyTests: XCTestCase {
  private func mnemonic(fromEntropy hex: String, wordlist: [String]) -> String {
    let entropy = Hex.bytes(hex)
    let checksumBits = entropy.count * 8 / 32
    let digest = TestSupport.sha256(entropy)
    var bits = entropy.map { String($0, radix: 2).padLeft(8) }.joined()
    bits += digest.map { String($0, radix: 2).padLeft(8) }.joined().prefix(checksumBits).description
    var words: [String] = []
    var index = bits.startIndex
    while index < bits.endIndex {
      let next = bits.index(index, offsetBy: 11)
      guard let value = Int(bits[index..<next], radix: 2) else { return "" }
      words.append(wordlist[value])
      index = next
    }
    return words.joined(separator: " ")
  }

  func testEveryVectorRebuildsFromItsEntropy() {
    let wordlist = Bip39.loadWordlist()
    XCTAssertEqual(wordlist.count, 2048)
    XCTAssertEqual(Vectors.bip39.count, 8)
    for vector in Vectors.bip39 {
      XCTAssertEqual(mnemonic(fromEntropy: vector.entropyHex, wordlist: wordlist), vector.mnemonic, vector.id)
      // NIT-10: not a tautological round-trip - entropy length must imply the BIP-39 word count.
      XCTAssertTrue([16, 20, 24, 28, 32].contains(Hex.bytes(vector.entropyHex).count), vector.id)
    }
  }
}

private extension String {
  func padLeft(_ width: Int) -> String {
    String(repeating: "0", count: max(0, width - count)) + self
  }
}
