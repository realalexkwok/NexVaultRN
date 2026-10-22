package expo.modules.securityandroid

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import expo.modules.kotlin.exception.CodedException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MAN-019 device demo: the biometric prompt appears; THREE WRONG fingerprints fail closed with
 * E_SEC_BIOMETRIC_CANCELLED (no plaintext, input wiped); a correct fingerprint then round-trips; a
 * wrong AAD fails with E_SEC_BLOB_CORRUPT; and a cancelled WRAP also wipes its plaintext (M2).
 *
 * The red banner in TestBiometricActivity states the required action before every prompt, so the
 * walker does not need to read chat while touching the sensor.
 *
 * Manual only: gated by `-e t3_1_manual 1` (connectedDebugAndroidTest skips the test).
 */
@RunWith(AndroidJUnit4::class)
class ManualBiometricGateTest {
  private val context: Context get() = ApplicationProvider.getApplicationContext()

  @Test
  fun promptThreeWrongFingerprintsFailClosedThenCorrectSucceeds() = runBlocking {
    Assume.assumeTrue(
      "manual walk only: pass -e t3_1_manual 1",
      InstrumentationRegistry.getArguments().getString("t3_1_manual") == "1"
    )
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

      banner(activity, "1/5 WRAP: press the REGISTERED finger ONCE")
      Log.i(TAG, "STEP 1/5 WRAP: enter a CORRECT fingerprint")
      val blob = manager.wrap(activity, plaintext, aad)
      assertEquals(1.toByte(), blob[0])
      assertArrayEquals("wrap must wipe its plaintext input", ByteArray(32), plaintext)

      banner(activity, "2/5 UNWRAP: press an UNREGISTERED finger 3 TIMES (sensor rejects each)")
      Log.i(TAG, "STEP 2/5 UNWRAP: enter THREE WRONG fingerprints (the gate cancels itself)")
      val failClosed = runCatching { manager.unwrap(activity, blob, aad) }.exceptionOrNull()
      assertTrue("expected a typed fail-closed error, got " + failClosed, failClosed is BiometricCancelledException)
      assertEquals(SecurityErrorCodes.BIOMETRIC_CANCELLED, (failClosed as CodedException).code)
      Log.i(TAG, "FAIL-CLOSED OK code=" + failClosed.code)

      banner(activity, "3/5 UNWRAP: press the REGISTERED finger ONCE")
      Log.i(TAG, "STEP 3/5 UNWRAP: enter a CORRECT fingerprint")
      val recovered = manager.unwrap(activity, blob, aad)
      assertArrayEquals(expected, recovered)
      recovered.fill(0)

      banner(activity, "4/5 WRONG AAD: press the REGISTERED finger ONCE (an error is expected)")
      Log.i(TAG, "STEP 4/5 UNWRAP WITH WRONG AAD: enter a CORRECT fingerprint; expect E_SEC_BLOB_CORRUPT")
      val wrongAad = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94|another-user".toByteArray()
      val corrupt = runCatching { manager.unwrap(activity, blob, wrongAad) }.exceptionOrNull()
      assertTrue("expected E_SEC_BLOB_CORRUPT, got " + corrupt, corrupt is BlobCorruptException)
      wrongAad.fill(0)

      banner(activity, "5/5 WRAP CANCELLED: press an UNREGISTERED finger 3 TIMES")
      Log.i(TAG, "STEP 5/5 WRAP CANCELLED: enter THREE WRONG fingerprints; expect fail-closed + wiped plaintext")
      val cancelledPlaintext = ByteArray(32) { index -> (index + 2).toByte() }
      val cancelled = runCatching { manager.wrap(activity, cancelledPlaintext, aad) }.exceptionOrNull()
      assertTrue("expected a typed fail-closed error, got " + cancelled, cancelled is BiometricCancelledException)
      assertArrayEquals("a cancelled wrap must still wipe its plaintext", ByteArray(32), cancelledPlaintext)
      Log.i(TAG, "CANCELLED-WRAP WIPE OK")

      expected.fill(0)
    }
  }

  private fun banner(activity: FragmentActivity, text: String) {
    activity.runOnUiThread { (activity as TestBiometricActivity).showInstruction(text) }
    Thread.sleep(400)
  }

  companion object {
    private const val TAG = "T3.1-DEMO"
  }
}
