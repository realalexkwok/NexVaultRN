package expo.modules.vaultcore

import android.content.Context
import java.math.BigInteger
import java.nio.CharBuffer
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Framework-light BIP-39 English validation and PBKDF2-HMAC-SHA512 seed derivation.
 *
 * Security notes:
 * - [validateMnemonic] accepts an already NFKD-normalized mnemonic (the module owns
 *   normalization); all sensitive scratch byte arrays are wiped before return.
 * - [mnemonicToSeed] prefers the JDK PBKDF2 provider and falls back to a self-written
 *   PBKDF2 loop when `PBKDF2WithHmacSHA512` is unavailable. No third-party code.
 * - Java `String`/`BigInteger` intermediates are not zeroizable (JVM semantics); the
 *   feature spec labels that compensation as [C], never [P].
 * - This file must not gain any log call (AGENTS.md R5).
 */
internal object Bip39 {
  private const val EXPECTED_WORD_COUNT = 2048
  private val ALLOWED_WORD_COUNTS = setOf(12, 15, 18, 21, 24)
  private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA512"
  private const val HMAC_ALGORITHM = "HmacSHA512"
  private const val PBKDF2_ITERATIONS = 2048
  private const val KEY_LENGTH_BITS = 512
  private const val KEY_LENGTH_BYTES = KEY_LENGTH_BITS / 8
  private val ASCII_WHITESPACE = Regex("[\\s]+")

  /**
   * Validates a normalized BIP-39 English mnemonic against [wordlist].
   * Fails closed if the wordlist is not the expected 2048 entries.
   */
  internal fun validateMnemonic(normalized: String, wordlist: List<String>): Boolean {
    if (wordlist.size != EXPECTED_WORD_COUNT) {
      return false
    }

    val words = normalized.trim().split(ASCII_WHITESPACE).filter { it.isNotEmpty() }
    if (words.size !in ALLOWED_WORD_COUNTS) {
      return false
    }

    val totalBits = words.size * 11
    val entropyBits = totalBits * 32 / 33
    val checksumBits = totalBits - entropyBits
    val entropyBytesLength = entropyBits / 8

    var packed = BigInteger.ZERO
    for (word in words) {
      val index = wordlist.binarySearch(word)
      if (index < 0) {
        return false
      }
      packed = packed.shiftLeft(11).or(BigInteger.valueOf(index.toLong()))
    }

    val checksumMask = BigInteger.ONE.shiftLeft(checksumBits).subtract(BigInteger.ONE)
    val checksum = packed.and(checksumMask).toInt()
    val entropy = packed.shiftRight(checksumBits)

    val entropyBytes = ByteArray(entropyBytesLength)
    for (i in 0 until entropyBytesLength) {
      val shift = 8 * (entropyBytesLength - 1 - i)
      entropyBytes[i] = entropy
        .shiftRight(shift)
        .and(BigInteger.valueOf(0xFF))
        .toByte()
    }

    var digest: ByteArray? = null
    return try {
      digest = MessageDigest.getInstance("SHA-256").digest(entropyBytes)
      var expectedChecksum = 0
      for (i in 0 until checksumBits) {
        val bit = (digest[i / 8].toInt() ushr (7 - (i % 8))) and 1
        expectedChecksum = (expectedChecksum shl 1) or bit
      }
      checksum == expectedChecksum
    } catch (_: NoSuchAlgorithmException) {
      false
    } finally {
      entropyBytes.fill(0)
      digest?.fill(0)
    }
  }

  /**
   * Derives the 64-byte BIP-39 seed with the default empty passphrase.
   */
  internal fun mnemonicToSeed(normalizedMnemonic: String, passphrase: String = ""): ByteArray =
    deriveSeed(normalizedMnemonic, passphrase, forceFallback = false)

  /**
   * Same output as [mnemonicToSeed], but always uses the self-written PBKDF2 loop.
   * Exposed `internal` so the JVM suite can cover the fallback branch on API >= 26.
   */
  internal fun mnemonicToSeedUsingFallback(
    normalizedMnemonic: String,
    passphrase: String = ""
  ): ByteArray = deriveSeed(normalizedMnemonic, passphrase, forceFallback = true)

  /**
   * Loads `res/raw/bip39_english.txt`. Returns an empty list (fail-closed for all
   * validation) if the resource is missing or does not contain exactly 2048 words.
   * No log call is emitted here.
   */
  internal fun loadWordlist(context: Context): List<String> {
    val words = try {
      context.resources.openRawResource(R.raw.bip39_english).use { stream ->
        stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
          lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
        }
      }
    } catch (_: Exception) {
      emptyList()
    }
    return if (words.size == EXPECTED_WORD_COUNT) words else emptyList()
  }

  private fun deriveSeed(
    normalizedMnemonic: String,
    passphrase: String,
    forceFallback: Boolean
  ): ByteArray {
    val passwordChars = normalizedMnemonic.toCharArray()
    val saltBytes = ("mnemonic" + passphrase).toByteArray(Charsets.UTF_8)
    return try {
      if (!forceFallback) {
        val derived = deriveWithJdkProvider(passwordChars, saltBytes)
        if (derived != null) {
          return derived
        }
      }
      deriveWithFallback(passwordChars, saltBytes)
    } finally {
      passwordChars.fill('\u0000')
      saltBytes.fill(0)
    }
  }

  /**
   * Returns null when `PBKDF2WithHmacSHA512` is unavailable, so the caller can fall
   * back to [deriveWithFallback].
   */
  private fun deriveWithJdkProvider(passwordChars: CharArray, saltBytes: ByteArray): ByteArray? {
    val factory = try {
      SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
    } catch (_: NoSuchAlgorithmException) {
      return null
    }

    val spec = PBEKeySpec(passwordChars, saltBytes, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
    return try {
      val encoded = factory.generateSecret(spec).encoded ?: throw Pbkdf2UnavailableException()
      val copy = encoded.copyOf()
      encoded.fill(0)
      copy
    } finally {
      spec.clearPassword()
    }
  }

  /** Self-written PBKDF2-HMAC-SHA512; no third-party code. */
  private fun deriveWithFallback(passwordChars: CharArray, saltBytes: ByteArray): ByteArray {
    val mac = try {
      Mac.getInstance(HMAC_ALGORITHM)
    } catch (_: NoSuchAlgorithmException) {
      throw Pbkdf2UnavailableException()
    }

    val passwordBytes = encodeUtf8(passwordChars)
    val output = ByteArray(KEY_LENGTH_BYTES)
    try {
      mac.init(SecretKeySpec(passwordBytes, HMAC_ALGORITHM))
      val macLength = mac.macLength
      val blockCount = (output.size + macLength - 1) / macLength
      var outputOffset = 0

      for (blockIndex in 1..blockCount) {
        val blockInput = ByteArray(saltBytes.size + 4)
        System.arraycopy(saltBytes, 0, blockInput, 0, saltBytes.size)
        blockInput[saltBytes.size] = ((blockIndex ushr 24) and 0xFF).toByte()
        blockInput[saltBytes.size + 1] = ((blockIndex ushr 16) and 0xFF).toByte()
        blockInput[saltBytes.size + 2] = ((blockIndex ushr 8) and 0xFF).toByte()
        blockInput[saltBytes.size + 3] = (blockIndex and 0xFF).toByte()

        var u = mac.doFinal(blockInput)
        val t = u.copyOf()
        try {
          repeat(PBKDF2_ITERATIONS - 1) {
            val next = mac.doFinal(u)
            u.fill(0)
            u = next
            for (j in t.indices) {
              t[j] = (t[j].toInt() xor u[j].toInt()).toByte()
            }
          }

          val copyLength = minOf(macLength, output.size - outputOffset)
          System.arraycopy(t, 0, output, outputOffset, copyLength)
          outputOffset += copyLength
        } finally {
          blockInput.fill(0)
          u.fill(0)
          t.fill(0)
        }
      }
    } finally {
      passwordBytes.fill(0)
    }
    return output
  }

  /**
   * Encodes a CharArray as UTF-8 without creating an unbounded-lived String copy.
   * The encoder's backing buffer is wiped after the exact-size copy is made.
   */
  private fun encodeUtf8(chars: CharArray): ByteArray {
    val encoded = Charsets.UTF_8.newEncoder().encode(CharBuffer.wrap(chars))
    val bytes = ByteArray(encoded.remaining())
    encoded.get(bytes)
    if (encoded.hasArray()) {
      encoded.array().fill(0)
    }
    return bytes
  }
}
