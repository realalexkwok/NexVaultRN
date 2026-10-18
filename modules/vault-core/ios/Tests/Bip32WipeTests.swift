import XCTest
@testable import VaultCoreCore

/// NIT-9: proves the MINOR-3 fix at buffer level — every node the derivation destroys must have its
/// key and chain code already zeroed at the moment of destruction.
final class Bip32WipeTests: XCTestCase {
  func testDerivationDestroysNodesWithZeroedBuffers() throws {
    var observed: [([UInt8], [UInt8])] = []
    Bip32Node.onDestroyed = { key, chainCode in observed.append((key, chainCode)) }
    defer { Bip32Node.onDestroyed = nil }

    let seed = try TestSupport.canonicalSeed()
    let indices = try Bip32.parseEthereumPath(Vectors.canonicalPath)
    var key = try Bip32.derivePrivateKey(seed: seed, indices: indices)
    defer { wipe(&key) }

    XCTAssertEqual(observed.count, indices.count, "one destroyed node per derivation step")
    for (index, pair) in observed.enumerated() {
      XCTAssertEqual(pair.0, [UInt8](repeating: 0, count: 32), "node \(index) key must be wiped")
      XCTAssertEqual(pair.1, [UInt8](repeating: 0, count: 32), "node \(index) chain code must be wiped")
    }
  }

  func testFinalChainCodeIsWipedOnTheSuccessPath() throws {
    var observed: [[UInt8]] = []
    Bip32Node.onFinalChainCodeWiped = { observed.append($0) }
    defer { Bip32Node.onFinalChainCodeWiped = nil }

    let seed = try TestSupport.canonicalSeed()
    let indices = try Bip32.parseEthereumPath(Vectors.canonicalPath)
    var key = try Bip32.derivePrivateKey(seed: seed, indices: indices)
    defer { wipe(&key) }

    XCTAssertEqual(observed.count, 1, "the success path must wipe the final node's chain code")
    XCTAssertEqual(observed.first, [UInt8](repeating: 0, count: 32))
  }
}
