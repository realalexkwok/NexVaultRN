package expo.modules.securityandroid

import android.content.Context

/**
 * T3.2 risk levels (spec: security-android section 2.2). The full L0-L5 matrix is driven by
 * attestation verdicts and lands with T5.x; this task produces only the subset it can prove:
 * L0 (no hit) or L4 (a hard detection hit), and the gate denies at L3 and above.
 *
 * Public on purpose: vault-core (a separate Gradle module) imports the gate, and the device test
 * suite seeds [SecurityState]. None of this is an Expo JS surface -- the only JS face is
 * SecurityAndroidModule (AGENTS.md R1).
 */
enum class RiskLevel { L0, L1, L2, L3, L4, L5 }

/**
 * One detection pass. Null booleans mean "not evaluated" (missing build-time constant / no embedded
 * bundle in a debug build) -- never "passed" (R6 honesty). Vector id lists carry no plaintext
 * details (R5).
 */
data class DetectionReport(
  val signatureMatch: Boolean?,
  val bundleHashMatch: Boolean?,
  val rootSignals: List<String>,
  val fridaSignals: List<String>,
  val debuggerAttached: Boolean,
  val evaluatedAt: Long
)

internal enum class DetectionScope { FAST, FULL }

internal object DetectionPure {
  /** Section 3 action matrix: column C (signing) is denied from L3 upward. */
  fun isDenied(level: RiskLevel): Boolean = level >= RiskLevel.L3

  /**
   * T3.2 subset mapping: any hard hit is treated as the L4-equivalent "do not sign" state; a clean
   * pass is L0. Attestation-driven L1/L2/L3/L5 semantics need the T5.x verdict pipeline.
   */
  fun evaluate(report: DetectionReport): RiskLevel {
    val hardHit = report.signatureMatch == false ||
      report.bundleHashMatch == false ||
      report.rootSignals.isNotEmpty() ||
      report.fridaSignals.isNotEmpty() ||
      report.debuggerAttached
    return if (hardHit) RiskLevel.L4 else RiskLevel.L0
  }
}

/**
 * Process-wide detection cache. In-memory only and never persisted (R5's on-disk whitelist is
 * addresses and SecureStore/Keychain ciphertext). [debugApplyForTest] is the documented test seam
 * (skill invariant 6): it is a no-op unless the app is debuggable and is never reachable from JS.
 */
object SecurityState {
  @Volatile private var report: DetectionReport? = null
  @Volatile private var level: RiskLevel = RiskLevel.L0

  fun update(report: DetectionReport) {
    this.report = report
    this.level = DetectionPure.evaluate(report)
  }

  fun cachedLevel(): RiskLevel = level

  fun cachedReport(): DetectionReport? = report

  fun hasReport(): Boolean = report != null

  fun debugApplyForTest(report: DetectionReport) {
    if (!BuildConfig.DEBUG) return
    update(report)
  }

  fun resetForTest() {
    if (!BuildConfig.DEBUG) return
    report = null
    level = RiskLevel.L0
  }
}

internal object DetectionRunner {
  fun run(scope: DetectionScope, context: Context): DetectionReport {
    val signature = SignatureSelfCheck.evaluate(context)
    val bundle = BundleSelfCheck.evaluate(context)
    val root = if (scope == DetectionScope.FULL) RootDetector.evaluate(context) else emptyList()
    val frida = if (scope == DetectionScope.FULL) FridaDetector.evaluate() else emptyList()
    val debugger = if (scope == DetectionScope.FULL) AntiDebugDetector.evaluate() else false
    val report = DetectionReport(
      signatureMatch = signature,
      bundleHashMatch = bundle,
      rootSignals = root,
      fridaSignals = frida,
      debuggerAttached = debugger,
      evaluatedAt = System.currentTimeMillis()
    )
    SecurityState.update(report)
    return report
  }
}

/**
 * fail-closed gate (AGENTS.md R1/R6: the only [P]-eligible on-device mechanism once device-verified).
 *
 * Called by vault-core before any key material is touched. If no detection pass has happened yet it
 * runs the cheap (fast) pass inline, so a missing report can never fail open.
 */
object SecurityGate {
  fun assertSigningAllowed(context: Context) {
    if (!SecurityState.hasReport()) {
      DetectionRunner.run(DetectionScope.FAST, context)
    }
    if (DetectionPure.isDenied(SecurityState.cachedLevel())) {
      throw GateDeniedException()
    }
  }
}
