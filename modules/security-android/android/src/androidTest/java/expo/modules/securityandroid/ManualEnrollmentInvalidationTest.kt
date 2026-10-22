package expo.modules.securityandroid

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import expo.modules.kotlin.exception.CodedException
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MAN-020 device demo in two explicit phases, so the invalidation assertion cannot false-pass on a
 * missing key:
 *
 * 1. phase1PrepareKeyForEnrollmentWalk — creates the wrap key and asserts it exists.
 * 2. The owner enrolls one additional fingerprint in system settings.
 * 3. phase2AddingABiometricInvalidatesTheWrapKey — asserts the key still existed before use, then
 *    expects E_SEC_KEY_INVALIDATED (setInvalidatedByBiometricEnrollment) and the documented
 *    recovery (delete + ensure).
 *
 * Manual only: gated by `-e t3_1_manual 1` (connectedDebugAndroidTest skips them).
 */
@RunWith(AndroidJUnit4::class)
class ManualEnrollmentInvalidationTest {
  private val context: Context get() = ApplicationProvider.getApplicationContext()

  @Test
  fun phase1PrepareKeyForEnrollmentWalk() {
    assumeManualWalkEnabled()
    val manager = WrapKeyManager(context)
    manager.deleteKey()
    val info = manager.ensureKey()
    assertTrue(
      "expected a hardware-backed provenance level, got " + info.securityLevel,
      info.securityLevel == SecurityLevel.STRONGBOX || info.securityLevel == SecurityLevel.TRUSTED_ENVIRONMENT
    )
    assertNotNull("phase 1 must leave an existing wrap key", manager.existingSecurityLevel())
    Log.i("T3.1-DEMO", "PHASE 1 OK level=" + info.securityLevel + " — now enroll a NEW fingerprint in Settings, then run phase2")
  }

  @Test
  fun phase2AddingABiometricInvalidatesTheWrapKey() {
    assumeManualWalkEnabled()
    val manager = WrapKeyManager(context)
    val before = manager.existingSecurityLevel()
    assertNotNull("phase 1 must have created the wrap key before enrollment", before)

    val error = runCatching { manager.encryptCipher() }.exceptionOrNull()
    assertTrue("expected E_SEC_KEY_INVALIDATED, got " + error, error is KeyInvalidatedException)
    Log.i("T3.1-DEMO", "ENROLLMENT INVALIDATION OK code=" + (error as CodedException).code)

    manager.deleteKey()
    val recovered = manager.ensureKey()
    assertTrue(
      "recovery must return a hardware-backed provenance level, got " + recovered.securityLevel,
      recovered.securityLevel == SecurityLevel.STRONGBOX || recovered.securityLevel == SecurityLevel.TRUSTED_ENVIRONMENT
    )
  }

  private fun assumeManualWalkEnabled() {
    Assume.assumeTrue(
      "manual walk only: pass -e t3_1_manual 1",
      InstrumentationRegistry.getArguments().getString("t3_1_manual") == "1"
    )
  }
}
