package expo.modules.securityandroid

import expo.modules.kotlin.exception.CodedException

/**
 * Fixed E_SEC_* codes (spec: security-android section 2.5). The pure constants object stays
 * JVM-testable; the exception classes wrap it for the Expo bridge. Messages never carry key
 * material, AAD or blob bytes (AGENTS.md R5).
 */
internal object SecurityErrorCodes {
  const val NO_LOCKSCREEN = "E_SEC_NO_LOCKSCREEN"
  const val KEY_INVALIDATED = "E_SEC_KEY_INVALIDATED"
  const val BIOMETRIC_CANCELLED = "E_SEC_BIOMETRIC_CANCELLED"
  const val BIOMETRIC_LOCKOUT = "E_SEC_BIOMETRIC_LOCKOUT"
  const val BLOB_CORRUPT = "E_SEC_BLOB_CORRUPT"
  const val SOFTWARE_KEYSTORE = "E_SEC_SOFTWARE_KEYSTORE"
  const val UNSUPPORTED_OPERATION = "E_SEC_UNSUPPORTED_OPERATION"

  // T3.2 (spec section 2.5): the gate denial is raised by SecurityGate; the two mismatch codes are
  // the report-level contract for the JS face (the report carries the flags, the gate throws
  // E_SEC_GATE_DENIED). Kept in the fixed set so the spec table stays machine-checked.
  const val GATE_DENIED = "E_SEC_GATE_DENIED"
  const val SIG_MISMATCH = "E_SEC_SIG_MISMATCH"
  const val BUNDLE_MISMATCH = "E_SEC_BUNDLE_MISMATCH"

  val ALL = setOf(
    NO_LOCKSCREEN,
    KEY_INVALIDATED,
    BIOMETRIC_CANCELLED,
    BIOMETRIC_LOCKOUT,
    BLOB_CORRUPT,
    SOFTWARE_KEYSTORE,
    UNSUPPORTED_OPERATION,
    GATE_DENIED,
    SIG_MISMATCH,
    BUNDLE_MISMATCH
  )
}

internal class NoLockscreenException : CodedException(
  SecurityErrorCodes.NO_LOCKSCREEN,
  "A secure lock screen is required",
  null
)

internal class KeyInvalidatedException : CodedException(
  SecurityErrorCodes.KEY_INVALIDATED,
  "Wrap key is missing or was invalidated",
  null
)

internal class BiometricCancelledException : CodedException(
  SecurityErrorCodes.BIOMETRIC_CANCELLED,
  "Biometric authentication was cancelled",
  null
)

internal class BiometricLockoutException : CodedException(
  SecurityErrorCodes.BIOMETRIC_LOCKOUT,
  "Biometric authentication is locked out",
  null
)

internal class BlobCorruptException : CodedException(
  SecurityErrorCodes.BLOB_CORRUPT,
  "Wrap blob failed its integrity check",
  null
)

internal class SoftwareKeystoreException : CodedException(
  SecurityErrorCodes.SOFTWARE_KEYSTORE,
  "Wrap key is not hardware-backed",
  null
)

internal class SecurityUnsupportedException : CodedException(
  SecurityErrorCodes.UNSUPPORTED_OPERATION,
  "Operation is not supported",
  null
)

/**
 * T3.2 fail-closed gate denial (spec section 2.5). Public because the gate itself is imported by
 * vault-core (and its device suite asserts this exception type); it is never thrown with signal
 * details -- vector ids only reach the report (R5).
 */
class GateDeniedException : CodedException(
  SecurityErrorCodes.GATE_DENIED,
  "Security gate denied the operation",
  null
)
