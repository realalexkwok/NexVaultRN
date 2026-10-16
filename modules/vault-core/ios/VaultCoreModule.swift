import ExpoModulesCore
import Foundation

/// vault-core iOS module: the four frozen methods (spec: vault-core §2.3). The TypeScript layer is
/// untouched — same method names, `Data` in/out, opaque string handle.
public class VaultCoreModule: Module {
  private let store = VaultStore()
  private lazy var wordlist: [String] = Bip39.loadWordlist()

  public func definition() -> ModuleDefinition {
    Name("VaultCore")

    AsyncFunction("importMnemonic") { (bytes: Data) throws -> String in
      var input = [UInt8](bytes)
      defer { wipe(&input) }
      guard let decoded = String(bytes: input, encoding: .utf8) else {
        throw vaultException(VaultErrorCode.invalidMnemonic, "Invalid mnemonic")
      }
      let normalized = decoded
        .decomposedStringWithCompatibilityMapping
        .trimmingCharacters(in: .whitespacesAndNewlines)
      guard Bip39.validate(normalized: normalized, wordlist: self.wordlist) else {
        throw vaultException(VaultErrorCode.invalidMnemonic, "Invalid mnemonic")
      }
      guard let seed = Bip39.mnemonicToSeed(normalizedMnemonic: normalized, passphrase: "") else {
        throw vaultException(VaultErrorCode.pbkdf2Unavailable, "PBKDF2-HMAC-SHA512 is unavailable on this device")
      }
      var seedBuffer = seed
      defer { wipe(&seedBuffer) }
      return self.store.put(seed: seedBuffer)
    }

    AsyncFunction("deriveAddress") { (handle: String, path: String) throws -> String in
      guard var seed = self.store.copyOf(handle: handle) else {
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

    AsyncFunction("signDigest") { (handle: String, path: String, digest: Data) throws -> Data in
      var digestBytes = [UInt8](digest)
      defer { wipe(&digestBytes) }
      try Ecdsa.requireDigest32(digestBytes)
      guard var seed = self.store.copyOf(handle: handle) else {
        throw vaultException(VaultErrorCode.handleNotFound, "Handle not found")
      }
      defer { wipe(&seed) }
      let indices = try Bip32.parseEthereumPath(path)
      let signature = try Ecdsa.signDigest(seed: seed, indices: indices, digest32: digestBytes)
      return Data(signature)
    }

    AsyncFunction("destroy") { (handle: String) in
      _ = self.store.evict(handle: handle)
    }
  }
}
