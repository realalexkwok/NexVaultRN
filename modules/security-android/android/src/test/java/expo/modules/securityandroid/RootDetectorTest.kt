package expo.modules.securityandroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** TC-T32-001: root parsers over injected fixtures (positive + negative), no device needed. */
class RootDetectorTest {
  @Test
  fun suBinaryIsDetectedOnlyWhenPresent() {
    assertTrue(RootPure.suPresent(listOf("/system/bin/su", "/system/bin/sh")))
    assertFalse(RootPure.suPresent(listOf("/system/bin/sh", "/system/bin/rm")))
  }

  @Test
  fun magiskPackageIsDetectedOnlyWhenInstalled() {
    assertTrue(RootPure.magiskInstalled(listOf("com.topjohnwu.magisk", "com.android.chrome")))
    assertFalse(RootPure.magiskInstalled(listOf("com.android.chrome", "com.android.settings")))
  }

  @Test
  fun rootPropsAreDetected() {
    assertTrue(RootPure.propsIndicateRoot(listOf("ro.secure=0\nro.debuggable=1\n")))
    assertTrue(RootPure.propsIndicateRoot(listOf("ro.build.tags=test-keys\n")))
    assertFalse(RootPure.propsIndicateRoot(listOf("ro.secure=1\nro.debuggable=0\nro.build.tags=release-keys\n")))
  }

  @Test
  fun vectorIdsAggregateEachSignalOnce() {
    assertEquals(emptyList<String>(), RootPure.vectorIds(emptyList(), emptyList(), listOf("ro.secure=1")))
    assertEquals(
      listOf(RootPure.ID_SU_BIN),
      RootPure.vectorIds(listOf("/sbin/su"), emptyList(), listOf("ro.secure=1"))
    )
    assertEquals(
      listOf(RootPure.ID_MAGISK_PKG),
      RootPure.vectorIds(emptyList(), listOf("com.topjohnwu.magisk"), listOf("ro.secure=1"))
    )
    assertEquals(
      listOf(RootPure.ID_PROPS),
      RootPure.vectorIds(emptyList(), emptyList(), listOf("ro.debuggable=1"))
    )
    assertEquals(
      listOf(RootPure.ID_SU_BIN, RootPure.ID_MAGISK_PKG, RootPure.ID_PROPS),
      RootPure.vectorIds(listOf("/sbin/su"), listOf("com.topjohnwu.magisk"), listOf("ro.debuggable=1"))
    )
  }
}
