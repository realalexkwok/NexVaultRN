import ExpoModulesCore
import Foundation
import VaultCoreCore

/// Expo bridge for the four frozen methods (spec: vault-core §2.3).
///
/// Deliberately thin: all logic lives in `VaultCoreCore`, which carries the **iOS 15.0** floor so the
/// XCTest bundle can run on an iOS 15.8 device (T1.5-r2). This pod keeps the **iOS 16.4** floor that
/// ExpoModulesCore requires. The only thing that happens here is input copying/zeroization and the
/// mapping of the core's coded errors onto the frozen `E_VAULT_*` exception the JS layer sees.
public class VaultCoreModule: Module {
  private let service = VaultCoreService()

  public func definition() -> ModuleDefinition {
    Name("VaultCore")

    AsyncFunction("importMnemonic") { (bytes: Data) throws -> String in
      var input = [UInt8](bytes)
      defer { wipe(&input) }
      return try self.bridged { try self.service.importMnemonic(bytes: input) }
    }

    AsyncFunction("deriveAddress") { (handle: String, path: String) throws -> String in
      try self.bridged { try self.service.deriveAddress(handle: handle, path: path) }
    }

    AsyncFunction("signDigest") { (handle: String, path: String, digest: Data) throws -> Data in
      var digestBytes = [UInt8](digest)
      defer { wipe(&digestBytes) }
      return try self.bridged {
        try Data(self.service.signDigest(handle: handle, path: path, digest: digestBytes))
      }
    }

    AsyncFunction("destroy") { (handle: String) in
      self.service.destroy(handle: handle)
    }
  }

  /// Maps the core's coded error onto Expo's exception so `error.code` stays identical to Android.
  private func bridged<T>(_ body: () throws -> T) throws -> T {
    do {
      return try body()
    } catch let error as VaultCodedException {
      throw Exception(name: error.code, description: error.reason, code: error.code)
    }
  }
}
