package expo.modules.securityandroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** TC-T32-003: TracerPid parsing (0 / non-zero / malformed / missing). */
class AntiDebugDetectorTest {
  @Test
  fun parsesTracerPidValues() {
    assertEquals(0, AntiDebugPure.parseTracerPid("Name:\tapp\nTracerPid:\t0\nUid:\t10000\n"))
    assertEquals(1234, AntiDebugPure.parseTracerPid("TracerPid:\t1234\n"))
    assertEquals(-1, AntiDebugPure.parseTracerPid("TracerPid:\tabc\n"))
    assertEquals(-1, AntiDebugPure.parseTracerPid("Name:\tapp\n"))
  }

  @Test
  fun onlyPositiveTracerPidIsTraced() {
    assertFalse(AntiDebugPure.isTraced("TracerPid:\t0\n"))
    assertTrue(AntiDebugPure.isTraced("TracerPid:\t12\n"))
    assertFalse(AntiDebugPure.isTraced("TracerPid:\t-1\n"))
  }
}
