package expo.modules.securityandroid

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import expo.modules.kotlin.exception.CodedException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MAN-019 device demo (owner 2026-10-22 gate): the biometric prompt appears; THREE WRONG
 * fingerprints fail closed with E_SEC_BIOMETRIC_CANCELLED (no plaintext, input wiped); a correct
 * fingerprint then round-trips; a wrong AAD fails with E_SEC_BLOB_CORRUPT.
 *
 * Run per method with the owner at the device:
 * adb shell am instrument -w -e class expo.modules.securityandroid.ManualBiometricGateTest#promptThreeWrongFingerprintsFailClosedThenCorrectSucceeds expo.modules.securityandroid.test/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4::class)
class ManualBiometricGateTest {
  private val context: Context get() = ApplicationProvider.getApplicationContext()

  @Test
  fun promptThreeWrongFingerprintsFailClosedThenCorrectSucceeds() = runBlocking {
    ActivityScenario.launch(TestBiometricActivity::class.java).use { scenario ->
      var host: FragmentActivity? = null
      scenario.onActivity { activity -> host = activity }
      val activity = requireNotNull(host)
      val manager = WrapKeyManager(context)
      manager.deleteKey()
      manager.ensureKey()

      val plaintext = ByteArray(32) { index -> (index + 1).toByte() }
      val expected = plaintext.copyOf()
      val aad = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94|demo-user".toByteArray()

      Log.i(TAG, "STEP 1/4 WRAP: enter a CORRECT fingerprint")
      val blob = manager.wrap(activity, plaintext, aad)
      assertEquals(1.toByte(), blob[0])
      assertArrayEquals("wrap must wipe its plaintext input", ByteArray(32), plaintext)

      Log.i(TAG, "STEP 2/4 UNWRAP: enter THREE WRONG fingerprints (the gate cancels itself)")
      val failClosed = runCatching { manager.unwrap(activity, blob, aad) }.exceptionOrNull()
      assertTrue("expected a typed fail-closed error, got " + failClosed, failClosed is BiometricCancelledException)
      assertEquals(SecurityErrorCodes.BIOMETRIC_CANCELLED, (failClosed as CodedException).code)
      Log.i(TAG, "FAIL-CLOSED OK code=" + failClosed.code)

      Log.i(TAG, "STEP 3/4 UNWRAP: enter a CORRECT fingerprint")
      val recovered = manager.unwrap(activity, blob, aad)
      assertArrayEquals(expected, recovered)
      recovered.fill(0)

      Log.i(TAG, "STEP 4/4 UNWRAP WITH WRONG AAD: enter a CORRECT fingerprint; expect E_SEC_BLOB_CORRUPT")
      val wrongAad = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94|another-user".toByteArray()
      val corrupt = runCatching { manager.unwrap(activity, blob, wrongAad) }.exceptionOrNull()
      assertTrue("expected E_SEC_BLOB_CORRUPT, got " + corrupt, corrupt is BlobCorruptException)
      wrongAad.fill(0)
      expected.fill(0)
    }
  }

  companion object {
    private const val TAG = "T3.1-DEMO"
  }
}
