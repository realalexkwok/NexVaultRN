import ExpoModulesCore
import Foundation

/**
 * SecurityIos module (author-only on this Linux host; compiled and tested on the Mac per
 * MAC-HANDOFF.md). Status [~] until MS-1..MS-6 pass. No signal detail is ever logged or put in an
 * error message (AGENTS.md R5).
 *
 * T3.2 takes over the JS face of spec: security-ios section 2.1. Call order is fixed:
 * gate -> Keychain biometric read -> crypto, and wrap/unwrap delegate to WrapKeyService.
 */
public class SecurityIosModule: Module {
  public func definition() -> ModuleDefinition {
    Name("SecurityIos")

    AsyncFunction("wrapKey") { (plaintext: Data, aad: Data) throws -> Data in
      try self.bridged {
        try SecurityGate.assertSigningAllowed()
        return try WrapKeyService.wrap(plaintext: plaintext, aad: aad)
      }
    }

    AsyncFunction("unwrapKey") { (blob: Data, aad: Data) throws -> Data in
      try self.bridged {
        try SecurityGate.assertSigningAllowed()
        return try WrapKeyService.unwrap(blob: blob, aad: aad)
      }
    }

    AsyncFunction("runDetection") { (scope: String) throws -> DetectionReportRecord in
      try self.bridged {
        guard scope == "fast" || scope == "full" else {
          throw SecurityIosError.unsupported
        }
        let report = scope == "full" ? DetectionRun.full() : DetectionRun.fast()
        SecurityState.apply(report)
        return Self.record(for: report)
      }
    }

    Function("getCachedRiskLevel") { () -> String in
      SecurityState.cachedRiskLevel().rawValue
    }

    AsyncFunction("assertSigningAllowed") { () throws -> Void in
      try self.bridged { try SecurityGate.assertSigningAllowed() }
    }
  }

  /// Maps the pod's fixed-code errors onto Expo exceptions so JS branches on error.code exactly as
  /// it does for security-android.
  private func bridged<T>(_ body: () throws -> T) throws -> T {
    do {
      return try body()
    } catch {
      throw Self.bridgeError(error)
    }
  }

  private static func bridgeError(_ error: Error) -> Error {
    if let denied = error as? SecurityGateDenied {
      return Exception(name: SecurityGateDenied.code, description: SecurityGateDenied.message, code: SecurityGateDenied.code)
    }
    if let iosError = error as? SecurityIosError {
      return Exception(name: iosError.rawValue, description: iosError.message, code: iosError.rawValue)
    }
    return error
  }

  /// Wire shape shared with Android (spec: security-android section 2.2). iOS T3.2 has no APK
  /// signature or JS-bundle self-check, so those fields are reported as NOT EVALUATED instead of
  /// being omitted; jailbreak vectors travel in jailbreakSignals and rootSignals stays empty.
  private static func record(for report: DetectionReport) -> DetectionReportRecord {
    var record = DetectionReportRecord()
    record.signatureMatch = false
    record.signatureEvaluated = false
    record.bundleHashMatch = false
    record.bundleEvaluated = false
    record.rootSignals = []
    record.fridaSignals = report.fridaSignals
    record.debuggerAttached = report.debuggerAttached
    record.evaluatedAt = report.evaluatedAt.timeIntervalSince1970 * 1000.0
    record.riskLevel = DetectionPure.evaluate(report).rawValue
    record.jailbreakSignals = report.jailbreakSignals
    return record
  }
}

/// JS-facing detection report. Field names match the Android record so the App wrapper can share
/// one TypeScript type; see SecurityIosModule.record(for:) for the not-evaluated rationale.
struct DetectionReportRecord: Record {
  @Field var signatureMatch: Bool = false
  @Field var signatureEvaluated: Bool = false
  @Field var bundleHashMatch: Bool = false
  @Field var bundleEvaluated: Bool = false
  @Field var rootSignals: [String] = []
  @Field var fridaSignals: [String] = []
  @Field var debuggerAttached: Bool = false
  @Field var evaluatedAt: Double = 0
  @Field var riskLevel: String = "L0"
  @Field var jailbreakSignals: [String] = []
}
