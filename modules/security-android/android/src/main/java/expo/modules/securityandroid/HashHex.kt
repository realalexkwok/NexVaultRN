package expo.modules.securityandroid

import java.security.MessageDigest

/**
 * T3.2 pure hashing helpers shared by the signature and bundle self-checks (spec:
 * security-android section 2.2). No Android types appear here, so the JVM suite owns the
 * sha256/normalization/match semantics (TC-T32-005); the device readers only supply inputs.
 */
internal object HashHex {
  const val NOT_EVALUATED = "NOT_EVALUATED"
  private val HEX = "0123456789abcdef".toCharArray()

  fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)

  fun sha256Hex(bytes: ByteArray): String = toHex(sha256(bytes))

  fun toHex(bytes: ByteArray): String {
    val out = CharArray(bytes.size * 2)
    for (i in bytes.indices) {
      val value = bytes[i].toInt() and 0xFF
      out[i * 2] = HEX[value ushr 4]
      out[i * 2 + 1] = HEX[value and 0x0F]
    }
    return String(out)
  }

  /** Lowercase, strips ':' separators and whitespace; blank stays blank. */
  fun normalizeHex(value: String): String =
    value.trim().replace(":", "").replace(" ", "").lowercase()

  /**
   * Normalized expected hash, or null when the build did not inject a real value (missing
   * resource / the NOT_EVALUATED placeholder the config plugin writes for debug builds that do
   * not embed the JS bundle). A null expected value means "not evaluated", never "passed".
   */
  fun expectedOrNull(rawExpected: String?): String? {
    val normalized = rawExpected?.let(::normalizeHex) ?: return null
    if (normalized.length != 64) return null
    if (normalized == NOT_EVALUATED.lowercase()) return null
    if (!normalized.all { it in '0'..'9' || it in 'a'..'f' }) return null
    return normalized
  }

  /** null = not evaluated; true/false = comparison outcome (unreadable actual fails closed). */
  fun matches(rawExpected: String?, actualSha256Hex: String?): Boolean? {
    val expected = expectedOrNull(rawExpected) ?: return null
    val actual = actualSha256Hex?.let(::normalizeHex) ?: return false
    return expected == actual
  }
}
