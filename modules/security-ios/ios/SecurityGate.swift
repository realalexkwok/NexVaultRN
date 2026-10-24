import Foundation

/// Risk levels shared with Android (spec: security-android section 3 / security-ios section 3).
public enum RiskLevel: String, CaseIterable {
  case l0 = "L0"
  case l1 = "L1"
  case l2 = "L2"
  case l3 = "L3"
  case l4 = "L4"
  case l5 = "L5"
}

/// iOS detection report (T3.2). The Android-only fields (APK signature / JS bundle self-check) are
/// intentionally absent: the module reports them as not evaluated on the shared JS wire shape.
public struct DetectionReport: Equatable {
  public let jailbreakSignals: [String]
  public let fridaSignals: [String]
  public let debuggerAttached: Bool
  public let evaluatedAt: Date

  public init(
    jailbreakSignals: [String],
    fridaSignals: [String],
    debuggerAttached: Bool,
    evaluatedAt: Date
  ) {
    self.jailbreakSignals = jailbreakSignals
    self.fridaSignals = fridaSignals
    self.debuggerAttached = debuggerAttached
    self.evaluatedAt = evaluatedAt
  }
}

/// Pure mapping for the T3.2 subset: any hard signal -> L4, otherwise L0. The attestation-backed
/// levels (L1/L2/L3/L5) are T5.x; L0 here is "no client-side signal fired", never a verified
/// clean-device verdict (AGENTS.md R6).
public enum DetectionPure {
  public static func evaluate(_ report: DetectionReport) -> RiskLevel {
    if report.debuggerAttached || !report.jailbreakSignals.isEmpty || !report.fridaSignals.isEmpty {
      return .l4
    }
    return .l0
  }

  /// Fail-closed threshold: L3+ denies signing/import (spec matrix column C).
  public static func isDenied(_ level: RiskLevel) -> Bool {
    switch level {
    case .l0, .l1, .l2: return false
    case .l3, .l4, .l5: return true
    }
  }
}

/// In-memory detection cache (never persisted; AGENTS.md R5). Thread-safe: runDetection runs
/// off-main while the gate reads on the calling thread.
public enum SecurityState {
  private static let lock = NSLock()
  private static var cached: DetectionReport?

  public static func apply(_ report: DetectionReport) {
    lock.lock()
    defer { lock.unlock() }
    cached = report
  }

  public static func cachedReport() -> DetectionReport? {
    lock.lock()
    defer { lock.unlock() }
    return cached
  }

  public static func cachedRiskLevel() -> RiskLevel {
    cachedReport().map { DetectionPure.evaluate($0) } ?? .l0
  }

  #if DEBUG
  /// Test-only seam, compiled out of Release builds (fixtures seed/reset the cache).
  public static func resetForTest() {
    lock.lock()
    defer { lock.unlock() }
    cached = nil
  }

  public static func debugApplyForTest(_ report: DetectionReport) {
    apply(report)
  }
  #endif
}

/// Device-facing detection runs shared by the module and the gate's self-sufficient fast pass.
enum DetectionRun {
  static func fast() -> DetectionReport {
    DetectionReport(
      jailbreakSignals: JailbreakDetector.pathFamilySignals(),
      fridaSignals: JailbreakDetector.dyldSignals(),
      debuggerAttached: JailbreakDetector.isDebuggerAttached(),
      evaluatedAt: Date()
    )
  }

  static func full() -> DetectionReport {
    let fastReport = fast()
    var signals = fastReport.jailbreakSignals
    if JailbreakDetector.isBootTimeAnomalous(), !signals.contains("JB_BOOTTIME") {
      signals.append("JB_BOOTTIME")
    }
    return DetectionReport(
      jailbreakSignals: signals,
      fridaSignals: fastReport.fridaSignals,
      debuggerAttached: fastReport.debuggerAttached,
      evaluatedAt: fastReport.evaluatedAt
    )
  }
}

/// Fail-closed gate (AGENTS.md R6: this coupling is the only [P]-eligible control; every signal
/// feeding it is [C]). Throws only SecurityGateDenied; any other error must be read as denial too.
public enum SecurityGate {
  /// Refreshes a fast pass when no report exists yet, so the gate is never fail-open by omission.
  public static func assertSigningAllowed() throws {
    let report = SecurityState.cachedReport() ?? DetectionRun.fast()
    SecurityState.apply(report)
    if DetectionPure.isDenied(DetectionPure.evaluate(report)) {
      throw SecurityGateDenied()
    }
  }
}
