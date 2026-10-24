import ExpoModulesCore
import Foundation
import SecurityIos
import VaultCoreCore

/// Expo bridge for the four frozen methods (spec: vault-core §2.3).
///
/// Deliberately thin: all logic lives in `VaultCoreCore`, which carries the **iOS 15.1** floor so the
/// XCTest bundle can run on an iOS 15.8 device (T1.5-r2). This pod keeps the **iOS 16.4** floor that
/// ExpoModulesCore requires. The only thing that happens here is input copying/zeroization and the
/// mapping of the core's coded errors onto the frozen `E_VAULT_*` exception the JS layer sees.
///
/// T3.2 (2026-10-24): the bridge injects the security gate into the core. Order per call is
/// gate -> key work, and a denial is re-labelled E_VAULT_SECURITY_GATE (same code as Android).
public class VaultCoreModule: Module {
  private let service = VaultCoreService(signingGate: VaultCoreModule.securityGate)

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

  /// T3.2 fail-closed gate. The security pod owns the decision; this bridge only translates a denial
  /// into the frozen vault-core code, so JS sees E_VAULT_SECURITY_GATE exactly as on Android.
  private static func securityGate() throws {
    do {
      try SecurityGate.assertSigningAllowed()
    } catch is SecurityGateDenied {
      throw VaultCodedException(code: VaultErrorCode.securityGate, reason: "Security gate denied the operation")
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
