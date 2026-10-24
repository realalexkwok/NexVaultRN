package expo.modules.securityandroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** TC-T32-004: L0/L4 mapping, deny threshold, cache semantics and the test-only seam. */
class DetectionAggregatorTest {
  private fun report(
    signatureMatch: Boolean? = null,
    bundleHashMatch: Boolean? = null,
    rootSignals: List<String> = emptyList(),
    fridaSignals: List<String> = emptyList(),
    debuggerAttached: Boolean = false
  ) = DetectionReport(signatureMatch, bundleHashMatch, rootSignals, fridaSignals, debuggerAttached, 0L)

  @Test
  fun cleanReportIsL0AndAllowed() {
    val level = DetectionPure.evaluate(report(signatureMatch = true, bundleHashMatch = true))
    assertEquals(RiskLevel.L0, level)
    assertFalse(DetectionPure.isDenied(level))
  }

  @Test
  fun notEvaluatedIsNeverAHit() {
    assertEquals(RiskLevel.L0, DetectionPure.evaluate(report()))
  }

  @Test
  fun eachHardSignalDenies() {
    val hits = listOf(
      report(signatureMatch = false),
      report(bundleHashMatch = false),
      report(rootSignals = listOf(RootPure.ID_SU_BIN)),
      report(fridaSignals = listOf(FridaPure.ID_MAPS)),
      report(debuggerAttached = true)
    )
    hits.forEach { hit ->
      val level = DetectionPure.evaluate(hit)
      assertEquals(RiskLevel.L4, level)
      assertTrue(DetectionPure.isDenied(level))
    }
  }

  @Test
  fun stateCachesTheLatestReportAndTheSeamRequiresDebug() {
    SecurityState.resetForTest()
    assertFalse(SecurityState.hasReport())
    SecurityState.debugApplyForTest(report(rootSignals = listOf(RootPure.ID_SU_BIN)))
    assertTrue(SecurityState.hasReport())
    assertEquals(RiskLevel.L4, SecurityState.cachedLevel())
    assertEquals(listOf(RootPure.ID_SU_BIN), SecurityState.cachedReport()?.rootSignals)
    SecurityState.resetForTest()
    assertEquals(RiskLevel.L0, SecurityState.cachedLevel())
    assertNull(SecurityState.cachedReport())
  }
}
