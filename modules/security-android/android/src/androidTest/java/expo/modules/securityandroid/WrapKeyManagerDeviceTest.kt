package expo.modules.securityandroid

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device-only suite (TC-SA-013 device half + TC-SA-014): Keystore key lifecycle, hardware level
 * and typed errors that need no biometric prompt. The interactive prompt legs live in
 * ManualBiometricGateTest / ManualEnrollmentInvalidationTest and are driven per method with the
 * owner present (T1.4 precedent: am instrument -e class ...#method).
 */
@RunWith(AndroidJUnit4::class)
class WrapKeyManagerDeviceTest {
  private val context: Context get() = ApplicationProvider.getApplicationContext()
  private val manager: WrapKeyManager get() = WrapKeyManager(context)

  // TC-SA-014
  @Test
  fun keyLifecycleIsHardwareBackedAndIdempotent() {
    val wrap = manager
    wrap.deleteKey()
    wrap.deleteKey()

    val info = wrap.ensureKey()
    assertEquals(WrapKeyManager.ALIAS, info.alias)
    assertTrue(
      "expected a hardware-backed level, got " + info.securityLevel,
      info.securityLevel == SecurityLevel.STRONGBOX || info.securityLevel == SecurityLevel.TRUSTED_ENVIRONMENT
    )
    assertEquals(info.securityLevel == SecurityLevel.STRONGBOX, info.strongBoxBacked)
    assertEquals(info, wrap.ensureKey())
    assertNotNull(wrap.existingSecurityLevel())
  }

  // TC-SA-014
  @Test
  fun capabilitiesReportDeviceState() {
    val wrap = manager
    wrap.ensureKey()
    assertTrue("the Pixel 6a must report a secure lock screen", wrap.isDeviceSecure())
    assertNotNull(wrap.existingSecurityLevel())
  }

  // TC-SA-014
  @Test
  fun deletedKeyFailsClosedWithTypedError() {
    val wrap = manager
    wrap.ensureKey()
    val dummyBlob = ByteArray(1 + 12 + 32 + 16).also { it[0] = AesGcmEnvelope.VERSION }

    wrap.deleteKey()
    assertThrows(KeyInvalidatedException::class.java) { wrap.decryptCipher(dummyBlob) }
    assertThrows(KeyInvalidatedException::class.java) { wrap.encryptCipher() }

    val regenerated = wrap.ensureKey()
    assertTrue(
      "recovery must return a hardware-backed key, got " + regenerated.securityLevel,
      regenerated.securityLevel == SecurityLevel.STRONGBOX || regenerated.securityLevel == SecurityLevel.TRUSTED_ENVIRONMENT
    )
  }

  // TC-SA-013 (device half)
  @Test
  fun errorTemplatesCarryNoMaterial() {
    val cases = listOf(
      BlobCorruptException(),
      KeyInvalidatedException(),
      BiometricCancelledException(),
      BiometricLockoutException(),
      NoLockscreenException(),
      SoftwareKeystoreException(),
      SecurityUnsupportedException()
    )
    val fragments = listOf("nexvault.wrap.v1", "0x9858EfFD", "demo-user")
    cases.forEach { error ->
      assertTrue("missing E_SEC_ code: " + error.code, error.code.startsWith("E_SEC_"))
      val message = error.message ?: ""
      assertTrue("message too long: " + message, message.length < 120)
      fragments.forEach { fragment ->
        assertTrue("message leaks a fragment: " + message, !message.contains(fragment))
      }
    }
  }
}
