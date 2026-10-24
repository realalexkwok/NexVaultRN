package expo.modules.securityandroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** TC-SA-013 (JVM half): the fixed E_SEC_* code set matches security-android section 2.5. */
class SecurityErrorCodesTest {
  @Test
  fun codesAreTheSpecSet() {
    assertEquals(
      setOf(
        "E_SEC_NO_LOCKSCREEN",
        "E_SEC_KEY_INVALIDATED",
        "E_SEC_BIOMETRIC_CANCELLED",
        "E_SEC_BIOMETRIC_LOCKOUT",
        "E_SEC_BLOB_CORRUPT",
        "E_SEC_SOFTWARE_KEYSTORE",
        "E_SEC_UNSUPPORTED_OPERATION",
        "E_SEC_GATE_DENIED",
        "E_SEC_SIG_MISMATCH",
        "E_SEC_BUNDLE_MISMATCH"
      ),
      SecurityErrorCodes.ALL
    )
    assertTrue(SecurityErrorCodes.ALL.all { it.startsWith("E_SEC_") })
  }
}
