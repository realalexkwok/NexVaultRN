package expo.modules.securityandroid

import javax.crypto.AEADBadTagException
import javax.crypto.Cipher

/**
 * Pure blob codec for the T3.1 wrap-key envelope (specs/security-android.md section 2.1 / 2.4).
 *
 * Blob layout: version(1B) = 1, IV(12B), ciphertext, GCM tag(16B).
 * AAD = account address + "|" + userId (UTF-8); public material only.
 *
 * The caller initialises the [Cipher] so the device path can bind it to a
 * BiometricPrompt.CryptoObject (init -> authenticate -> doFinal), while the JVM tests inject a
 * software AES key. No Android or Keystore types appear here, so the whole codec is
 * unit-testable on the JVM; the Keystore/prompt paths are device-only by construction.
 */
internal object AesGcmEnvelope {
  const val VERSION: Byte = 1
  const val IV_SIZE = 12
  const val TAG_SIZE = 16
  const val TAG_BITS = 128

  internal class Parsed(val iv: ByteArray, val ciphertext: ByteArray)

  /** Validates the version byte and length, then splits IV and ciphertext (including the tag). */
  fun parse(blob: ByteArray): Parsed {
    if (blob.size < 1 + IV_SIZE + TAG_SIZE) throw BlobCorruptException()
    if (blob[0] != VERSION) throw BlobCorruptException()
    val iv = blob.copyOfRange(1, 1 + IV_SIZE)
    val ciphertext = blob.copyOfRange(1 + IV_SIZE, blob.size)
    return Parsed(iv, ciphertext)
  }

  /**
   * Seals [plaintext] with the caller-initialised ENCRYPT cipher and packs the envelope.
   * The plaintext buffer is wiped in `finally` (AC-7: wrap consumes its input).
   */
  fun seal(cipher: Cipher, plaintext: ByteArray, aad: ByteArray): ByteArray {
    try {
      cipher.updateAAD(aad)
      val ciphertext = cipher.doFinal(plaintext)
      val iv = cipher.iv
      if (iv == null || iv.size != IV_SIZE) throw BlobCorruptException()
      val blob = ByteArray(1 + IV_SIZE + ciphertext.size)
      blob[0] = VERSION
      System.arraycopy(iv, 0, blob, 1, IV_SIZE)
      System.arraycopy(ciphertext, 0, blob, 1 + IV_SIZE, ciphertext.size)
      return blob
    } finally {
      plaintext.fill(0)
    }
  }

  /** Opens the envelope with the caller-initialised DECRYPT cipher; any tag mismatch fails closed. */
  fun open(cipher: Cipher, parsed: Parsed, aad: ByteArray): ByteArray {
    cipher.updateAAD(aad)
    return try {
      cipher.doFinal(parsed.ciphertext)
    } catch (e: AEADBadTagException) {
      throw BlobCorruptException()
    }
  }
}
