import XCTest
@testable import VaultCoreCore

/// MAJOR-2 (code review 2026-10-18): the public facade was untested — the memory walk reimplemented
/// its calls. These tests go through `VaultCoreService` exactly as the Expo bridge does.
final class VaultCoreServiceTests: XCTestCase {
  private let mnemonic = Array(Vectors.canonicalMnemonic.utf8)

  private func code(of body: () throws -> Void) -> String? {
    do { try body(); return nil } catch let error as VaultCodedException { return error.code } catch { return "UNCODED" }
  }

  func testFourMethodsRoundTripMatchesTheOracle() throws {
    let service = VaultCoreService()
    XCTAssertEqual(service.handleCount, 0)

    let handle = try service.importMnemonic(bytes: mnemonic)
    XCTAssertFalse(handle.isEmpty)
    XCTAssertEqual(service.handleCount, 1)

    let address = try service.deriveAddress(handle: handle, path: Vectors.canonicalPath)
    XCTAssertEqual(address, Vectors.canonicalAddress)

    let digest = Bip32.keccak256(Array(Vectors.digestMessage.utf8))
    let signature = try service.signDigest(handle: handle, path: Vectors.canonicalPath, digest: digest)
    XCTAssertEqual(Hex.string(signature), Vectors.canonicalSignature)
    XCTAssertEqual(signature.count, 65)
    XCTAssertLessThanOrEqual(signature[64], 1, "recid must stay in {0,1}")

    // two handles are independent vaults; destroying one leaves the other usable
    let second = try service.importMnemonic(bytes: mnemonic)
    service.destroy(handle: handle)
    XCTAssertEqual(service.handleCount, 1)
    XCTAssertEqual(try service.deriveAddress(handle: second, path: Vectors.canonicalPath), Vectors.canonicalAddress)

    // post-destroy: every accessor fails closed, destroy itself is idempotent
    XCTAssertEqual(code { _ = try service.deriveAddress(handle: handle, path: Vectors.canonicalPath) },
                   VaultErrorCode.handleNotFound)
    XCTAssertEqual(code { _ = try service.signDigest(handle: handle, path: Vectors.canonicalPath, digest: digest) },
                   VaultErrorCode.handleNotFound)
    service.destroy(handle: handle)
    service.destroy(handle: "not-a-handle")
    XCTAssertEqual(service.handleCount, 1)
  }

  func testErrorCodes() throws {
    let service = VaultCoreService()
    XCTAssertEqual(code { _ = try service.importMnemonic(bytes: Array("abandon abandon".utf8)) },
                   VaultErrorCode.invalidMnemonic)
    XCTAssertEqual(code { _ = try service.deriveAddress(handle: "nope", path: Vectors.canonicalPath) },
                   VaultErrorCode.handleNotFound)

    let handle = try service.importMnemonic(bytes: mnemonic)
    defer { service.destroy(handle: handle) }

    // MAJOR-1 regression: a non-hardened account segment must be rejected, not silently rewritten.
    XCTAssertEqual(code { _ = try service.deriveAddress(handle: handle, path: "m/44'/60'/0/0/0") },
                   VaultErrorCode.invalidPath)

    XCTAssertEqual(code { _ = try service.signDigest(handle: handle, path: Vectors.canonicalPath,
                                                     digest: [UInt8](repeating: 1, count: 31)) },
                   VaultErrorCode.invalidDigestLength)
    XCTAssertEqual(code { _ = try service.signDigest(handle: "nope", path: Vectors.canonicalPath,
                                                     digest: [UInt8](repeating: 1, count: 32)) },
                   VaultErrorCode.handleNotFound)
  }

  func testPathAliasesResolveToTheSameAddress() throws {
    let service = VaultCoreService()
    let handle = try service.importMnemonic(bytes: mnemonic)
    defer { service.destroy(handle: handle) }
    for alias in ["m/44h/60h/0h/0/0", "m/44'/60'/0h/0/0"] {
      XCTAssertEqual(try service.deriveAddress(handle: handle, path: alias), Vectors.canonicalAddress, alias)
    }
    XCTAssertEqual(try Bip32.parseEthereumPath("m/44h/60h/0h/0/0"), [44 | Bip32.hardenedOffset, 60 | Bip32.hardenedOffset,
                                                                    0 | Bip32.hardenedOffset, 0, 0])
  }
}
