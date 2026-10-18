import XCTest
@testable import VaultCoreCore

/// TC-VC-024 (plus the T1.4 MINOR-1 copy-independence contract and the TC-VC-005 automated side).
final class VaultStoreTests: XCTestCase {
  // TC-VC-024
  func testDestroyWipesAndIsIdempotent() {
    let store = VaultStore()
    let seed = (0..<64).map { UInt8($0 + 1) }
    let handle = store.put(seed: seed)
    XCTAssertEqual(store.count, 1)
    XCTAssertTrue(store.evict(handle: handle))
    XCTAssertEqual(store.count, 0)
    XCTAssertNil(store.copyOf(handle: handle))
    XCTAssertFalse(store.evict(handle: handle), "second destroy is a no-op")
  }

  // TC-VC-024 (MINOR-1)
  func testCopyOfIsIndependentOfStoredSeed() {
    let store = VaultStore()
    let seed = (0..<64).map { UInt8($0 + 9) }
    let handle = store.put(seed: seed)
    var copy = store.copyOf(handle: handle) ?? []
    XCTAssertEqual(copy.count, 64)
    copy[0] = 0xFF
    XCTAssertEqual(store.copyOf(handle: handle)?.first, 9, "mutating the copy must not touch the store")
    XCTAssertTrue(store.evict(handle: handle))
  }

  // TC-VC-024 / TC-VC-005
  func testThousandPutEvictCyclesLeaveStoreEmpty() {
    let store = VaultStore()
    for _ in 0..<1000 {
      let seed = [UInt8](repeating: 7, count: 64)
      let handle = store.put(seed: seed)
      XCTAssertTrue(store.evict(handle: handle))
    }
    XCTAssertEqual(store.count, 0)
  }
}
