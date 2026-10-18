import XCTest
@testable import VaultCoreCore

/// Regression tests from the 2026-10-18 code review (BLOCKER-1, NIT-2, NIT-3).
final class ReviewRegressionTests: XCTestCase {
  /// BLOCKER-1: `evict` used to `guard let seed` + `var stored = seed` — a CoW alias, so the wipe only
  /// zeroed a copy. The seam observes the buffer that actually held the seed, after the wipe.
  func testEvictZeroizesTheStoredBufferInPlace() {
    let store = VaultStore()
    let seed: [UInt8] = (1...64).map(UInt8.init)
    let handle = store.put(seed: seed)

    var observed: [UInt8]?
    store.onEvictWiped = { observed = $0 }
    XCTAssertTrue(store.evict(handle: handle))

    XCTAssertEqual(observed?.count, 64)
    XCTAssertEqual(observed, [UInt8](repeating: 0, count: 64),
                   "the evicted seed buffer must be zeroed, not merely dropped (BLOCKER-1)")
    XCTAssertNil(store.copyOf(handle: handle))
    XCTAssertFalse(store.evict(handle: handle), "destroy stays idempotent")
    XCTAssertEqual(store.count, 0)
  }

  /// NIT-2: the alias test only compared counts, which is how MAJOR-1 slipped through.
  func testPathAliasesProduceTheExactIndices() throws {
    let canonical = try Bip32.parseEthereumPath("m/44'/60'/0'/0/0")
    for alias in ["m/44h/60h/0h/0/0", "m/44'/60h/0'/0/0"] {
      XCTAssertEqual(try Bip32.parseEthereumPath(alias), canonical, alias)
    }
    XCTAssertEqual(canonical, [44 | Bip32.hardenedOffset, 60 | Bip32.hardenedOffset, 0 | Bip32.hardenedOffset, 0, 0])
    for bad in ["m/44'/60'/0/0/0", "m/44'/60'/0'/0'/0", "m/44'/60/0'/0/0"] {
      XCTAssertThrowsError(try Bip32.parseEthereumPath(bad), bad)
    }
  }
}
