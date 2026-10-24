package expo.modules.securityandroid

import android.content.Context
import java.io.FileNotFoundException
import java.io.IOException
import java.security.MessageDigest

/**
 * T3.2 JS-bundle self-check [C] (spec: security-android sections 2.2 / 2.4; plan row).
 *
 * Hashes assets/index.android.bundle and compares it with the constant injected at build time by
 * apps/mobile/plugins/withSecurityHashes.js.
 *
 * Debug builds load the bundle from Metro and do not embed the asset; that case is reported as
 * "not evaluated" (null) so a dev build is never denied by a missing file, while the release demo
 * build (the one that ships the bundle) gets a real check. The full two-stage CI pipeline that
 * rebuilds the bundle hash for every artifact stays a roadmap item.
 */
internal object BundleSelfCheck {
  const val RESOURCE_NAME = "nexvault_expected_bundle_sha256"
  const val ASSET_PATH = "index.android.bundle"
  private const val BUFFER_SIZE = 64 * 1024

  fun evaluate(context: Context): Boolean? {
    val expected = context.resources.getIdentifier(RESOURCE_NAME, "string", context.packageName)
      .takeIf { it != 0 }
      ?.let { context.getString(it) }
    if (HashHex.expectedOrNull(expected) == null) return null
    val actual = try {
      hashAsset(context)
    } catch (e: FileNotFoundException) {
      return null
    } catch (e: IOException) {
      return null
    }
    return HashHex.matches(expected, actual)
  }

  private fun hashAsset(context: Context): String {
    val digest = MessageDigest.getInstance("SHA-256")
    context.assets.open(ASSET_PATH).use { stream ->
      val buffer = ByteArray(BUFFER_SIZE)
      while (true) {
        val read = stream.read(buffer)
        if (read < 0) break
        digest.update(buffer, 0, read)
      }
    }
    return HashHex.toHex(digest.digest())
  }
}
