package expo.modules.securityandroid

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * T3.2 MVP app-signature self-check [C] (spec: security-android sections 2.2 / 2.4; plan row).
 *
 * Computes the SHA-256 of the first signing certificate through PackageManager
 * (GET_SIGNING_CERTIFICATES on API 28+, legacy GET_SIGNATURES on 24-27) and compares it with the
 * constant injected at build time by apps/mobile/plugins/withSecurityHashes.js.
 *
 * [C] cost-raiser only: on a rooted device the PackageManager read can be hooked and the expected
 * constant is reachable by the same attacker. The spec section 2.4 self-parsing .so variant (no
 * PackageManager, v2/v3 signature block) stays a roadmap item. Certificate rotation is not handled
 * here: Play App Signing lineage / multi-signer needs the T5.x work and an updated constant.
 *
 * Returns null when the build did not inject a real constant (unit-test builds) -- "not evaluated"
 * must never be reported as "passed" (R6 honesty).
 */
internal object SignatureSelfCheck {
  const val RESOURCE_NAME = "nexvault_expected_cert_sha256"

  fun evaluate(context: Context): Boolean? {
    val expected = context.resources.getIdentifier(RESOURCE_NAME, "string", context.packageName)
      .takeIf { it != 0 }
      ?.let { context.getString(it) }
    if (HashHex.expectedOrNull(expected) == null) return null
    return HashHex.matches(expected, currentCertificateSha256(context))
  }

  @Suppress("DEPRECATION")
  private fun currentCertificateSha256(context: Context): String? = try {
    val flags = if (Build.VERSION.SDK_INT >= 28) {
      PackageManager.GET_SIGNING_CERTIFICATES
    } else {
      PackageManager.GET_SIGNATURES
    }
    val info = context.packageManager.getPackageInfo(context.packageName, flags)
    val first = if (Build.VERSION.SDK_INT >= 28) {
      info.signingInfo?.apkContentsSigners?.firstOrNull()
    } else {
      info.signatures?.firstOrNull()
    }
    first?.toByteArray()?.let(HashHex::sha256Hex)
  } catch (e: PackageManager.NameNotFoundException) {
    null
  }
}
