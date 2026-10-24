package expo.modules.securityandroid

import android.content.Context
import androidx.fragment.app.FragmentActivity
import expo.modules.kotlin.functions.Coroutine
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * T3.2 JS face (spec: security-android sections 2.1 / 2.2): the T3.1 wrap-key functions plus the
 * detection report, the cached risk level, the fail-closed gate and the ArrayBuffer wrap/unwrap
 * surface (Handoff H-T3.1-2). wrap/unwrap run behind the gate; vault-core's signing path still uses
 * the native-internal unwrap, so plaintext never persists on the JS side (spec section 6, R1).
 *
 * The activity for BiometricPrompt is resolved lazily from the host app (Expo's current activity is
 * a FragmentActivity); a null activity is a fixed E_SEC_UNSUPPORTED_OPERATION, never a crash.
 */
class SecurityAndroidModule : Module() {
  private val manager: WrapKeyManager by lazy { WrapKeyManager(context()) }

  override fun definition() = ModuleDefinition {
    Name("SecurityAndroid")

    AsyncFunction("getCapabilities") Coroutine { ->
      withContext(Dispatchers.Default) {
        mapOf(
          "hasStrongBox" to manager.hasStrongBox(),
          "isDeviceSecure" to manager.isDeviceSecure(),
          "securityLevel" to manager.existingSecurityLevel()?.name
        )
      }
    }

    AsyncFunction("ensureWrapKey") Coroutine { ->
      withContext(Dispatchers.Default) {
        val info = manager.ensureCurrentSchemaKey()
        mapOf(
          "alias" to info.alias,
          "securityLevel" to info.securityLevel.name,
          "strongBoxBacked" to info.strongBoxBacked
        )
      }
    }

    AsyncFunction("deleteWrapKey") Coroutine { ->
      withContext(Dispatchers.Default) {
        manager.deleteKey()
        Unit
      }
    }

    AsyncFunction("runDetection") Coroutine { scope: String ->
      withContext(Dispatchers.Default) {
        val parsed = when (scope) {
          "fast" -> DetectionScope.FAST
          "full" -> DetectionScope.FULL
          else -> throw SecurityUnsupportedException()
        }
        reportToMap(DetectionRunner.run(parsed, context()))
      }
    }

    // Cached read for the hot path: synchronous by contract (spec section 2.2, <1ms).
    Function("getCachedRiskLevel") { -> SecurityState.cachedLevel().name }

    AsyncFunction("assertSigningAllowed") Coroutine { ->
      withContext(Dispatchers.Default) {
        SecurityGate.assertSigningAllowed(context())
        Unit
      }
    }

    // Handoff H-T3.1-2: the ArrayBuffer wrap/unwrap face. The gate runs first, before any cipher or
    // prompt work; the manager wipes the plaintext buffer on every path (AC-7/R1).
    AsyncFunction("wrapKey") Coroutine { plaintext: ByteArray, aad: ByteArray ->
      withContext(Dispatchers.Default) {
        SecurityGate.assertSigningAllowed(context())
        manager.wrap(requireActivity(), plaintext, aad)
      }
    }

    AsyncFunction("unwrapKey") Coroutine { blob: ByteArray, aad: ByteArray ->
      withContext(Dispatchers.Default) {
        SecurityGate.assertSigningAllowed(context())
        manager.unwrap(requireActivity(), blob, aad)
      }
    }
  }

  /**
   * Wire shape: booleans stay non-null and each self-check carries an explicit "evaluated" flag so
   * "not evaluated" (debug build without an embedded bundle) can never be read as "passed" (R6).
   */
  private fun reportToMap(report: DetectionReport): Map<String, Any> = mapOf(
    "signatureMatch" to (report.signatureMatch ?: true),
    "signatureEvaluated" to (report.signatureMatch != null),
    "bundleHashMatch" to (report.bundleHashMatch ?: true),
    "bundleEvaluated" to (report.bundleHashMatch != null),
    "rootSignals" to report.rootSignals,
    "fridaSignals" to report.fridaSignals,
    "debuggerAttached" to report.debuggerAttached,
    "evaluatedAt" to report.evaluatedAt,
    "riskLevel" to SecurityState.cachedLevel().name
  )

  private fun context(): Context =
    requireNotNull(appContext.reactContext) { "React context is unavailable" }

  private fun requireActivity(): FragmentActivity =
    appContext.currentActivity as? FragmentActivity ?: throw SecurityUnsupportedException()
}
