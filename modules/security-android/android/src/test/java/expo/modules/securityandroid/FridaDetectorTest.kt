package expo.modules.securityandroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** TC-T32-002: Frida parsers over injected maps text and port sets. */
class FridaDetectorTest {
  @Test
  fun mapsNeedlesAreMatched() {
    assertTrue(FridaPure.mapsIndicateFrida("7f00-7f01 r-xp 00000000 fd:00 /data/local/tmp/frida-agent-64.so"))
    assertTrue(FridaPure.mapsIndicateFrida("7f00-7f01 r-xp 00000000 fd:00 /data/app/libfrida.so"))
    assertFalse(FridaPure.mapsIndicateFrida("7f00-7f01 r-xp 00000000 fd:00 /system/lib64/libc.so"))
  }

  @Test
  fun vectorIdsCombineMapsAndPorts() {
    assertEquals(emptyList<String>(), FridaPure.vectorIds(null, emptyList()))
    assertEquals(emptyList<String>(), FridaPure.vectorIds("/system/lib64/libc.so", emptyList()))
    assertEquals(
      listOf(FridaPure.ID_MAPS),
      FridaPure.vectorIds("/data/local/tmp/frida-gadget.so", emptyList())
    )
    assertEquals(
      listOf(FridaPure.ID_PORT_PREFIX + 27042),
      FridaPure.vectorIds("/system/lib64/libc.so", listOf(27042))
    )
    assertEquals(
      listOf(FridaPure.ID_MAPS, FridaPure.ID_PORT_PREFIX + 27042, FridaPure.ID_PORT_PREFIX + 27043),
      FridaPure.vectorIds("/data/local/tmp/frida-agent.so", listOf(27043, 27042))
    )
  }
}
