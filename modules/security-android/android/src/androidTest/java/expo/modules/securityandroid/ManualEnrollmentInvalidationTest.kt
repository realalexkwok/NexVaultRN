package expo.modules.securityandroid

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import expo.modules.kotlin.exception.CodedException
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MAN-020 device demo: after a new fingerprint is enrolled in system settings, the wrap key
 * created with setInvalidatedByBiometricEnrollment(true) is permanently invalidated; the next use
 * fails closed with E_SEC_KEY_INVALIDATED, and the documented recovery (delete + ensure) restores
 * a hardware-backed key.
 *
 * Precondition for the run: the owner has ALREADY enrolled an additional fingerprint.
 */
@RunWith(AndroidJUnit4::class)
class ManualEnrollmentInvalidationTest {
  @Test
  fun addingABiometricInvalidatesTheWrapKey() {
    val manager = WrapKeyManager(ApplicationProvider.getApplicationContext<Context>())
    val error = runCatching { manager.encryptCipher() }.exceptionOrNull()
    assertTrue("expected E_SEC_KEY_INVALIDATED, got " + error, error is KeyInvalidatedException)
    Log.i("T3.1-DEMO", "ENROLLMENT INVALIDATION OK code=" + (error as CodedException).code)

    manager.deleteKey()
    val recovered = manager.ensureKey()
    assertTrue(
      "recovery must return a hardware-backed key, got " + recovered.securityLevel,
      recovered.securityLevel == SecurityLevel.STRONGBOX || recovered.securityLevel == SecurityLevel.TRUSTED_ENVIRONMENT
    )
  }
}
