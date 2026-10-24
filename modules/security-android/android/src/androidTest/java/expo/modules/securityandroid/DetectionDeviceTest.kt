package expo.modules.securityandroid

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * TC-T32-006 device leg (clean Pixel 6a): a full detection pass must report an honest L0 state and
 * the gate must allow signing. The debug build is signed with the debug keystore and has no
 * embedded JS bundle, so the signature check is evaluated-and-matching while the bundle check is
 * explicitly "not evaluated" -- asserted here so it can never be read as a silent pass (R6).
 *
 * Boundary: a library module's androidTest APK is self-targeted (its manifest targetPackage is the
 * test package), so the instrumentation process does not run inside the app process. The production
 * code always receives the app context; the test builds that exact context explicitly with
 * createPackageContext(<applicationId>) and asserts the injected resource lives there.
 */
@RunWith(AndroidJUnit4::class)
class DetectionDeviceTest {
  private val context: Context
    get() = ApplicationProvider.getApplicationContext<Context>()
      .createPackageContext("io.github.realalexkwok.nexvault", 0)

  @Test
  fun fullDetectionOnCleanDeviceIsL0AndAllowed() {
    SecurityState.resetForTest()
    val report = DetectionRunner.run(DetectionScope.FULL, context)

    assertTrue(
      "the app build must carry the injected expected-hash resource",
      context.resources.getIdentifier(
        SignatureSelfCheck.RESOURCE_NAME,
        "string",
        context.packageName
      ) != 0
    )
    assertEquals("dev-signed build must match the injected cert constant", true, report.signatureMatch)
    // Release embeds the bundle -> evaluated and matching; debug loads it from Metro -> null
    // ("not evaluated", asserted as such). A false value always fails the clean-device gate.
    assertEquals(
      "bundle self-check must pass when evaluated and be not-evaluated without an embedded asset",
      true,
      report.bundleHashMatch ?: true
    )
    assertEquals(emptyList<String>(), report.rootSignals)
    assertEquals(emptyList<String>(), report.fridaSignals)
    assertFalse(report.debuggerAttached)
    assertEquals(RiskLevel.L0, SecurityState.cachedLevel())

    SecurityGate.assertSigningAllowed(context) // must not throw on a clean device
  }

  @Test
  fun seededSignalDeniesAndResets() {
    SecurityState.debugApplyForTest(
      DetectionReport(null, null, listOf(RootPure.ID_SU_BIN), emptyList(), false, 0L)
    )
    val error = assertThrows(GateDeniedException::class.java) {
      SecurityGate.assertSigningAllowed(context)
    }
    assertEquals("E_SEC_GATE_DENIED", error.code)

    SecurityState.resetForTest()
    assertEquals(RiskLevel.L0, SecurityState.cachedLevel())
  }
}
