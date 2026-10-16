import Foundation

/// Native-only handle store — the T1.4 MINOR-1 contract on iOS:
/// - every accessor is serialized on one queue;
/// - `copyOf` hands out freshly allocated storage (no CoW aliasing with the stored seed);
/// - `evict` removes **and** wipes inside the same critical region, and is idempotent (a missing
///   handle is a no-op returning false — spec: vault-core §2.4).
final class VaultStore {
  private let queue = DispatchQueue(label: "io.github.realalexkwok.nexvault.vaultstore")
  private var entries: [String: [UInt8]] = [:]

  /// Stores an independent copy of the seed and returns a fresh `UUID().uuidString` handle.
  func put(seed: [UInt8]) -> String {
    queue.sync {
      let handle = UUID().uuidString
      entries[handle] = [UInt8](seed)
      return handle
    }
  }

  /// Independent copy of the stored seed, or nil when the handle is unknown/destroyed.
  func copyOf(handle: String) -> [UInt8]? {
    queue.sync {
      guard let seed = entries[handle] else { return nil }
      return [UInt8](seed)
    }
  }

  /// Removes and wipes the stored seed; false when the handle was already gone.
  @discardableResult
  func evict(handle: String) -> Bool {
    queue.sync {
      guard let seed = entries.removeValue(forKey: handle) else { return false }
      var stored = seed
      wipe(&stored)
      return true
    }
  }

  var count: Int {
    queue.sync { entries.count }
  }
}
