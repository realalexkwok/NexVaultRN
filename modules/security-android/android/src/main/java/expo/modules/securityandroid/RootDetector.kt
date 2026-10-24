package expo.modules.securityandroid

import android.content.Context
import java.io.File

/**
 * T3.2 MVP root detection [C] (spec: security-android T-SA-5; demo subset = 1-2 signals per plan).
 *
 * Signals: su binaries on the classic paths, known Magisk/superuser package ids, and root props
 * (ro.debuggable=1 / ro.secure=0 / service.adb.root=1 / ro.build.tags=test-keys). Every parser is a
 * pure function over injected strings (JVM gate, TC-T32-001); [RootDetector] only supplies the
 * device inputs. Vector ids are stable and carry no plaintext details (R5).
 *
 * [C] honest boundary: a Magisk + Shamiko/SUSFS stack can hide all three signals; the full
 * mounts/mountinfo + PID-namespace cross-coverage chain is the roadmap stage-2 item (4.5-6 person
 * days), not this demo subset.
 */
internal object RootPure {
  const val ID_SU_BIN = "ROOT_SU_BIN"
  const val ID_MAGISK_PKG = "ROOT_MAGISK_PKG"
  const val ID_PROPS = "ROOT_PROPS"

  val SU_CANDIDATES = listOf(
    "/system/bin/su",
    "/system/xbin/su",
    "/sbin/su",
    "/su/bin/su",
    "/vendor/bin/su",
    "/data/local/tmp/su",
    "/data/local/xbin/su",
    "/data/adb/magisk"
  )

  val MAGISK_PACKAGES = listOf(
    "com.topjohnwu.magisk",
    "io.github.vvb2060.magisk",
    "com.kingroot.kinguser",
    "eu.chainfire.supersu"
  )

  private val ROOT_PROPS = listOf("ro.debuggable=1", "ro.secure=0", "service.adb.root=1")
  private const val TEST_KEYS_TAG = "ro.build.tags=test-keys"

  fun suPresent(existingPaths: Collection<String>, candidates: List<String> = SU_CANDIDATES): Boolean =
    candidates.any { existingPaths.contains(it) }

  fun magiskInstalled(installedPackages: Collection<String>, candidates: List<String> = MAGISK_PACKAGES): Boolean =
    candidates.any { installedPackages.contains(it) }

  /** Scans build.prop-style key=value text for root markers (test-keys counts as a signal). */
  fun propsIndicateRoot(propTexts: List<String>): Boolean = propTexts.any { text ->
    ROOT_PROPS.any { marker -> text.contains(marker) } || text.contains(TEST_KEYS_TAG)
  }

  fun vectorIds(
    existingPaths: Collection<String>,
    installedPackages: Collection<String>,
    propTexts: List<String>
  ): List<String> {
    val ids = mutableListOf<String>()
    if (suPresent(existingPaths)) ids += ID_SU_BIN
    if (magiskInstalled(installedPackages)) ids += ID_MAGISK_PKG
    if (propsIndicateRoot(propTexts)) ids += ID_PROPS
    return ids
  }
}

internal object RootDetector {
  private val PROP_FILES = listOf("/system/build.prop", "/vendor/build.prop", "/default.prop")

  fun evaluate(context: Context): List<String> {
    val existing = RootPure.SU_CANDIDATES.filter { File(it).exists() }
    val installed = RootPure.MAGISK_PACKAGES.filter { installed(context, it) }
    val props = PROP_FILES.mapNotNull { path ->
      runCatching { File(path).readText() }.getOrNull()
    }
    return RootPure.vectorIds(existing, installed, props)
  }

  private fun installed(context: Context, packageName: String): Boolean = try {
    context.packageManager.getPackageInfo(packageName, 0)
    true
  } catch (e: Exception) {
    false
  }
}
