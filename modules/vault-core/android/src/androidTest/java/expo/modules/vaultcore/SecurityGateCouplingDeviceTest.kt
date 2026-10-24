package expo.modules.vaultcore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import expo.modules.securityandroid.DetectionReport
import expo.modules.securityandroid.GateDeniedException
import expo.modules.securityandroid.SecurityGate
import expo.modules.securityandroid.SecurityState
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TC-T32-007 device leg -- the [P]-eligible fail-closed coupling (owner plan Task 5).
 *
 * Seeds the documented debug-only security state with a root vector, then calls the exact methods
 * the AsyncFunction lambdas run (importMnemonicGuarded / signDigestGuarded). A denial must surface
 * as the frozen E_VAULT_SECURITY_GATE code, must still wipe the caller's mnemonic bytes (R1), and
 * must disappear once the state is clean again (L0 pass-through).
 */
@RunWith(AndroidJUnit4::class)
class SecurityGateCouplingDeviceTest {
  // A library module's androidTest APK is self-targeted: its resource table (and therefore the
  // R.raw wordlist id compiled into this library) belongs to the test APK, so mnemonic paths must
  // use the test context. The production app context is resolved separately and is used for the
  // gate/detection assertions -- there the injected signature resource is real and evaluated.
  private val testContext: Context get() = ApplicationProvider.getApplicationContext()
  private val appContext: Context
    get() = testContext.createPackageContext("io.github.realalexkwok.nexvault", 0)
  private val words =
    "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
  private val path = "m/44'/60'/0'/0/0"

  @Test
  fun gateDeniesImportAndSignOnDeviceAndWipesInput() {
    val module = VaultCoreModule()
    SecurityState.resetForTest()

    // L0 pass-through first: the real app context passes the evaluated signature check, and a
    // clean state imports and signs (same code the bridge runs).
    SecurityGate.assertSigningAllowed(appContext)
    val cleanBytes = words.toByteArray(Charsets.UTF_8)
    val cleanSize = cleanBytes.size
    val handle = module.importMnemonicGuarded(testContext, cleanBytes)
    assertTrue(handle.isNotEmpty())
    assertArrayEquals(ByteArray(cleanSize), cleanBytes)

    val digest = ByteArray(32) { index -> index.toByte() }
    assertEquals(65, module.signDigestGuarded(testContext, handle, path, digest).size)

    // Seed the L4-equivalent state: every gated entry point must refuse.
    SecurityState.debugApplyForTest(
      DetectionReport(null, null, listOf("ROOT_SU_BIN"), emptyList(), false, 0L)
    )

    val deniedBytes = words.toByteArray(Charsets.UTF_8)
    val deniedSize = deniedBytes.size
    val gateDenied = assertThrows(GateDeniedException::class.java) {
      SecurityGate.assertSigningAllowed(appContext)
    }
    assertEquals("E_SEC_GATE_DENIED", gateDenied.code)

    val importDenied = assertThrows(SecurityGateException::class.java) {
      module.importMnemonicGuarded(testContext, deniedBytes)
    }
    assertEquals("E_VAULT_SECURITY_GATE", importDenied.code)
    assertArrayEquals("R1: mnemonic bytes wiped even on a gate denial", ByteArray(deniedSize), deniedBytes)

    val signDenied = assertThrows(SecurityGateException::class.java) {
      module.signDigestGuarded(testContext, handle, path, digest)
    }
    assertEquals("E_VAULT_SECURITY_GATE", signDenied.code)

    // Recovery: the clean state restores pass-through (no sticky lockout).
    SecurityState.resetForTest()
    assertEquals(65, module.signDigestGuarded(testContext, handle, path, digest).size)
  }
}
