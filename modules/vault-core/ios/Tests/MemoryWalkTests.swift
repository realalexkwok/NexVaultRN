import Darwin
import XCTest
@testable import VaultCore

/// MAN-012 carrier (TC-VC-005, iOS side) — the iOS twin of the Android MAN-011 walk.
///
/// Runs 1000 rounds of the exact sequence the four `VaultCoreModule` AsyncFunctions run:
/// mnemonic normalisation + validation + PBKDF2 (`Bip39`), `VaultStore.put`, BIP-32 / Keccak-256 /
/// EIP-55 derivation (`Bip32`), RFC6979 recoverable signing (`Ecdsa`) and `VaultStore.evict`.
/// Every round's address and signature are compared against the pinned TC-VC-027 oracle, and the
/// process footprint (`task_vm_info.phys_footprint`, the iOS analogue of the Android walk's PSS
/// sample) is sampled every 100 rounds and compared with the post-warm-up baseline in a ±5% band.
///
/// The `AsyncFunction` closures themselves need the Expo host (T1.5-H2 -> T4.1), so this walk calls
/// the same cores the closure bodies call, in the same order. It is the memory-hygiene carrier for
/// the MAN-012 row, not a JS-bridge test.
final class MemoryWalkTests: XCTestCase {
  private static let rounds = 1000
  private static let warmupRounds = 20
  private static let sampleEvery = 100
  private static let tolerance = 0.05

  /// Physical footprint of this process; 0 when the kernel call fails.
  private func footprintBytes() -> UInt64 {
    var info = task_vm_info_data_t()
    var count = mach_msg_type_number_t(
      MemoryLayout<task_vm_info_data_t>.size / MemoryLayout<natural_t>.size
    )
    let status = withUnsafeMutablePointer(to: &info) { pointer -> kern_return_t in
      pointer.withMemoryRebound(to: integer_t.self, capacity: Int(count)) { rebound in
        task_info(mach_task_self_, task_flavor_t(TASK_VM_INFO), rebound, &count)
      }
    }
    return status == KERN_SUCCESS ? info.phys_footprint : 0
  }

  /// One round, written the way the module's AsyncFunctions are written: seed in, handle out,
  /// derive, sign, destroy (the `defer` makes destroy unconditional).
  private func round(store: VaultStore, wordlist: [String]) throws -> (address: String, signature: [UInt8]) {
    let normalized = Vectors.canonicalMnemonic
      .decomposedStringWithCompatibilityMapping
      .trimmingCharacters(in: .whitespacesAndNewlines)
    guard Bip39.validate(normalized: normalized, wordlist: wordlist) else {
      throw vaultException(VaultErrorCode.invalidMnemonic, "Invalid mnemonic")
    }
    guard var seed = Bip39.mnemonicToSeed(normalizedMnemonic: normalized, passphrase: "") else {
      throw vaultException(VaultErrorCode.pbkdf2Unavailable, "PBKDF2-HMAC-SHA512 is unavailable")
    }
    defer { wipe(&seed) }

    let handle = store.put(seed: seed)
    defer { store.evict(handle: handle) }

    guard var roundSeed = store.copyOf(handle: handle) else {
      throw vaultException(VaultErrorCode.handleNotFound, "Handle not found")
    }
    defer { wipe(&roundSeed) }

    let indices = try Bip32.parseEthereumPath(Vectors.canonicalPath)
    var privateKey = try Bip32.derivePrivateKey(seed: roundSeed, indices: indices)
    defer { wipe(&privateKey) }
    var publicKey = try Bip32.uncompressedPublicKey(privateKey: privateKey)
    defer { wipe(&publicKey) }
    let addressBytes = try Bip32.addressBytes(uncompressedPublicKey: publicKey)
    let address = try Bip32.checksumAddress(addressBytes: addressBytes)

    let digest = Bip32.keccak256(Array(Vectors.digestMessage.utf8))
    let signature = try Ecdsa.signDigest(seed: roundSeed, indices: indices, digest32: digest)
    return (address, signature)
  }

  // TC-VC-005 (iOS side) / MAN-012
  func testThousandRoundsKeepFootprintFlatAndDestroyEveryHandle() throws {
    let wordlist = Bip39.loadWordlist()
    XCTAssertEqual(wordlist.count, 2048)

    let store = VaultStore()
    // Warm-up: first-touch allocations (wordlist, CommonCrypto, the secp256k1 context, allocator
    // pools) must not be attributed to the walk itself.
    for _ in 0..<Self.warmupRounds {
      _ = try round(store: store, wordlist: wordlist)
    }
    XCTAssertEqual(store.count, 0, "every warm-up handle must be destroyed")

    let baseline = footprintBytes()
    XCTAssertGreaterThan(baseline, 0, "phys_footprint is unavailable on this platform")

    var samples = [UInt64]()
    var mismatches = 0
    var firstMismatch = ""
    for index in 0..<Self.rounds {
      try autoreleasepool {
        let (address, signature) = try round(store: store, wordlist: wordlist)
        if address != Vectors.canonicalAddress || Hex.string(signature) != Vectors.canonicalSignature {
          if mismatches == 0 {
            firstMismatch = "round \(index): address \(address), signature \(Hex.string(signature))"
          }
          mismatches += 1
        }
      }
      if index % Self.sampleEvery == 0 {
        samples.append(footprintBytes())
      }
    }
    samples.append(footprintBytes())

    let curve = samples.map(String.init).joined(separator: ",")
    let attachment = XCTAttachment(string: "phys_footprint samples (bytes): " + curve)
    attachment.name = "MAN-012 footprint curve"
    attachment.lifetime = .keepAlways
    add(attachment)

    XCTAssertEqual(mismatches, 0, "walk output diverged from the pinned oracle at " + firstMismatch)
    XCTAssertEqual(store.count, 0, "no handle may survive the walk")

    let peak = samples.max() ?? 0
    let allowed = UInt64(Double(baseline) * (1 + Self.tolerance))
    XCTAssertLessThanOrEqual(
      peak, allowed,
      "footprint exceeded the ±5% band: baseline \(baseline) B, peak \(peak) B, samples \(curve)"
    )

    // A destroyed handle stays unknown and destroy stays idempotent.
    let unknown = UUID().uuidString
    XCTAssertNil(store.copyOf(handle: unknown))
    XCTAssertFalse(store.evict(handle: unknown))
  }
}
