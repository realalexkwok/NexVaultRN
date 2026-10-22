package expo.modules.securityandroid

/**
 * Pure consecutive-failure counter for the per-use biometric gate (TC-SA-014).
 *
 * The Nth consecutive failure asks the caller to cancel the prompt, which resumes the operation
 * with E_SEC_BIOMETRIC_CANCELLED (fail-closed). Kept free of Android types so the JVM gate covers
 * the counting contract; the prompt plumbing itself is device-only.
 */
internal class PromptFailureGate(private val maxAttempts: Int = 3) {
  private var failures = 0

  /** Registers a failed attempt; true when the gate must fail closed (cancel the prompt). */
  fun onFailure(): Boolean {
    failures += 1
    return failures >= maxAttempts
  }
}
