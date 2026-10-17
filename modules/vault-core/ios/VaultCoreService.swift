import Foundation

/// The four frozen operations (spec: vault-core §2.3) with **no Expo dependency**.
///
/// This is the whole surface the Expo bridge (`VaultCore` pod) and the XCTest bundle use; the crypto
/// types stay `internal` to this module. Each instance owns its own handle store, so one service is
/// one vault. Handles are opaque `String`s; seeds never leave native memory (R1).
public final class VaultCoreService {
  private let store = VaultStore()
  private let wordlist: [String] = Bip39.loadWordlist()

  public init() {}

  /// Validates + normalises the mnemonic, runs PBKDF2, stores the seed and returns a fresh handle.
  public func importMnemonic(bytes: [UInt8]) throws -> String {
    var input = bytes
    defer { wipe(&input) }
    guard let decoded = String(bytes: input, encoding: .utf8) else {
      throw vaultException(VaultErrorCode.invalidMnemonic, "Invalid mnemonic")
    }
    let normalized = decoded
      .decomposedStringWithCompatibilityMapping
      .trimmingCharacters(in: .whitespacesAndNewlines)
    guard Bip39.validate(normalized: normalized, wordlist: wordlist) else {
      throw vaultException(VaultErrorCode.invalidMnemonic, "Invalid mnemonic")
    }
    guard var seed = Bip39.mnemonicToSeed(normalizedMnemonic: normalized, passphrase: "") else {
      throw vaultException(VaultErrorCode.pbkdf2Unavailable, "PBKDF2-HMAC-SHA512 is unavailable")
    }
    defer { wipe(&seed) }
    return store.put(seed: seed)
  }

  /// EIP-55 address for `path` under the handle's seed.
  public func deriveAddress(handle: String, path: String) throws -> String {
    guard var seed = store.copyOf(handle: handle) else {
      throw vaultException(VaultErrorCode.handleNotFound, "Handle not found")
    }
    defer { wipe(&seed) }
    let indices = try Bip32.parseEthereumPath(path)
    var privateKey = try Bip32.derivePrivateKey(seed: seed, indices: indices)
    defer { wipe(&privateKey) }
    var publicKey = try Bip32.uncompressedPublicKey(privateKey: privateKey)
    defer { wipe(&publicKey) }
    let addressBytes = try Bip32.addressBytes(uncompressedPublicKey: publicKey)
    return try Bip32.checksumAddress(addressBytes: addressBytes)
  }

  /// 65-byte `r ‖ s ‖ recid` signature over a 32-byte digest (digest length checked before store access).
  public func signDigest(handle: String, path: String, digest: [UInt8]) throws -> [UInt8] {
    try Ecdsa.requireDigest32(digest)
    guard var seed = store.copyOf(handle: handle) else {
      throw vaultException(VaultErrorCode.handleNotFound, "Handle not found")
    }
    defer { wipe(&seed) }
    let indices = try Bip32.parseEthereumPath(path)
    return try Ecdsa.signDigest(seed: seed, indices: indices, digest32: digest)
  }

  /// Removes **and wipes** the stored seed; destroying an unknown handle is a no-op (§2.4).
  public func destroy(handle: String) {
    _ = store.evict(handle: handle)
  }

  /// Live handle count — diagnostics/tests only.
  public var handleCount: Int { store.count }
}
