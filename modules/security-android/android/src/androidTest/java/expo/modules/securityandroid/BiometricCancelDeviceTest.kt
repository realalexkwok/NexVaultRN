package expo.modules.securityandroid

import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import expo.modules.kotlin.exception.CodedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device fail-closed leg with no human input: the test injects BACK into the focused biometric
 * dialog ~2.5 s after the prompt appears, so wrap() must resume with E_SEC_BIOMETRIC_CANCELLED and
 * the cancelled wrap must still wipe its plaintext (M2/AC-7).
 *
 * adb shell am instrument -w -e class expo.modules.securityandroid.BiometricCancelDeviceTest expo.modules.securityandroid.test/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4::class)
class BiometricCancelDeviceTest {
  @Test
  fun backKeyCancellationFailsClosedAndWipesPlaintext() = runBlocking {
    ActivityScenario.launch(TestBiometricActivity::class.java).use { scenario ->
      var host: FragmentActivity? = null
      scenario.onActivity { activity -> host = activity }
      val activity = requireNotNull(host)
      val manager = WrapKeyManager(ApplicationProvider.getApplicationContext())
      manager.deleteKey()
      manager.ensureKey()

      val plaintext = ByteArray(32) { 5 }
      val aad = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94|demo-user".toByteArray()

      Log.i(TAG, "CANCEL TEST: prompt up; injecting BACK in 2.5 s")
      val attempt = async(Dispatchers.IO) { runCatching { manager.wrap(activity, plaintext, aad) } }
      delay(2500)
      InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
      val error = attempt.await().exceptionOrNull()
      assertTrue("expected a typed fail-closed error, got " + error, error is BiometricCancelledException)
      assertArrayEquals("a cancelled wrap must still wipe its plaintext", ByteArray(32), plaintext)
      Log.i(TAG, "CANCEL FAIL-CLOSED OK code=" + (error as CodedException).code)
      Unit
    }
  }

  companion object {
    private const val TAG = "T3.1-DEMO"
  }
}
