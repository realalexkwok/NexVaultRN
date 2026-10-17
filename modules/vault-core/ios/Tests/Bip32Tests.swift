import XCTest
@testable import VaultCoreCore

/// TC-VC-003/013/016 — oracle-transcribed from the committed Android Bip32Test.kt.
final class Bip32Tests: XCTestCase {
  // TC-VC-013
  func testOfficialVectorsAllChains() throws {
    for vector in Vectors.bip32 {
      var node = try Bip32.masterNode(seed: Hex.bytes(vector.seedHex))
      XCTAssertEqual(vector.chains[0].path, "m", vector.id + " first chain")
      try assertChain(vector: vector, chain: vector.chains[0], node: node)
      var index = 0
      while index + 1 < vector.chains.count {
        let parentPath = vector.chains[index].path
        let childPath = vector.chains[index + 1].path
        XCTAssertTrue(childPath.hasPrefix(parentPath + "/"), vector.id + " chain order")
        let segment = String(childPath.dropFirst(parentPath.count + 1))
        let hardened = segment.hasSuffix("'")
        let digits = segment.replacingOccurrences(of: "'", with: "")
        let value = UInt32(digits) ?? 0
        let childNumber = hardened ? (value + Bip32.hardenedOffset) : value
        node = try Bip32.deriveChild(parent: node, childNumber: childNumber)
        try assertChain(vector: vector, chain: vector.chains[index + 1], node: node)
        index += 1
      }
    }
  }

  private func assertChain(vector: Vectors.OfficialVector, chain: Vectors.Chain, node: Bip32Node) throws {
    let xprv = try TestSupport.serializeExtendedKey(node, isPrivate: true)
    let xpub = try TestSupport.serializeExtendedKey(node, isPrivate: false)
    XCTAssertEqual(xprv, chain.xprv, vector.id + " " + chain.path + " xprv")
    XCTAssertEqual(xpub, chain.xpub, vector.id + " " + chain.path + " xpub")
  }

  // TC-VC-003
  func testCanonicalAndSecondAddress() throws {
    let seed = try TestSupport.canonicalSeed()
    XCTAssertEqual(try address(seed: seed, path: Vectors.canonicalPath), Vectors.canonicalAddress)
    XCTAssertEqual(try address(seed: seed, path: "m/44'/60'/0'/0/1"), Vectors.secondAddress)
  }

  private func address(seed: [UInt8], path: String) throws -> String {
    let indices = try Bip32.parseEthereumPath(path)
    let privateKey = try Bip32.derivePrivateKey(seed: seed, indices: indices)
    let publicKey = try Bip32.uncompressedPublicKey(privateKey: privateKey)
    let bytes = try Bip32.addressBytes(uncompressedPublicKey: publicKey)
    return try Bip32.checksumAddress(addressBytes: bytes)
  }

  // TC-VC-016
  func testPathNegativesAndAliases() throws {
    let invalid = [
      "m/", "m/44'/60'/0'/0", "m/49'/60'/0'/0/0", "m/44'/60'/0'/0/0/5",
      "m/44'/60'/0'/1'/0", "m/44'/60'/0'/0/x", "m/44'/60'/2147483648'/0/0",
      "m/44'/60'/0'/0/2147483648", "m/44/60'/0'/0/0", "M/44'/60'/0'/0/0", "",
    ]
    for path in invalid {
      XCTAssertThrowsError(try Bip32.parseEthereumPath(path), "must reject: " + path) { error in
        XCTAssertEqual((error as? VaultCodedException)?.code, VaultErrorCode.invalidPath)
      }
    }
    XCTAssertEqual(try Bip32.parseEthereumPath("m/44h/60h/0h/0/0").count, 5)
    XCTAssertEqual(try Bip32.parseEthereumPath("  m/44'/60'/0'/0/0  ").count, 5)
  }
}
