package expo.modules.securityandroid

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * JVM gate for the pure envelope codec (T3.1 Task 3; TC-SA-010 / TC-SA-011 / TC-SA-012).
 *
 * Honest boundary: Android Keystore and BiometricPrompt cannot run on the JVM; those paths are
 * device-only (instrumentation). This suite pins the blob layout, integrity failure, AAD binding,
 * IV uniqueness and the wrap-input zeroization contract with a software AES key.
 */
class AesGcmEnvelopeTest {
  private val key = SecretKeySpec(ByteArray(32) { index -> (index + 1).toByte() }, "AES")
  private val aad = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94|demo-user".toByteArray()

  private fun encryptCipher(iv: ByteArray? = null): Cipher =
    Cipher.getInstance("AES/GCM/NoPadding").apply {
      if (iv == null) {
        init(Cipher.ENCRYPT_MODE, key)
      } else {
        init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(AesGcmEnvelope.TAG_BITS, iv))
      }
    }

  private fun decryptCipher(iv: ByteArray): Cipher =
    Cipher.getInstance("AES/GCM/NoPadding").apply {
      init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(AesGcmEnvelope.TAG_BITS, iv))
    }

  // TC-SA-010
  @Test
  fun blobLayoutIsVersionIvCiphertextTag() {
    val iv = ByteArray(12) { index -> (index + 3).toByte() }
    val plaintext = ByteArray(32) { index -> (index + 5).toByte() }
    val blob = AesGcmEnvelope.seal(encryptCipher(iv), plaintext, aad)

    assertEquals(1 + 12 + 32 + 16, blob.size)
    assertEquals(1.toByte(), blob[0])

    val parsed = AesGcmEnvelope.parse(blob)
    assertArrayEquals(iv, parsed.iv)
    assertEquals(32 + 16, parsed.ciphertext.size)
  }

  // TC-SA-011
  @Test
  fun roundtripReturnsTheOriginalPlaintext() {
    val plaintext = ByteArray(48) { index -> (index * 3).toByte() }
    val expected = plaintext.copyOf()
    val blob = AesGcmEnvelope.seal(encryptCipher(), plaintext, aad)

    val parsed = AesGcmEnvelope.parse(blob)
    val opened = AesGcmEnvelope.open(decryptCipher(parsed.iv), parsed, aad)
    assertArrayEquals(expected, opened)
  }

  // TC-SA-011
  @Test
  fun wrongAadFailsClosed() {
    val blob = AesGcmEnvelope.seal(encryptCipher(), ByteArray(32) { 9 }, aad)
    val parsed = AesGcmEnvelope.parse(blob)
    val wrongAad = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94|another-user".toByteArray()

    assertThrows(BlobCorruptException::class.java) {
      AesGcmEnvelope.open(decryptCipher(parsed.iv), parsed, wrongAad)
    }
  }

  // TC-SA-011
  @Test
  fun tamperedCiphertextOrTagFailsClosed() {
    val blob = AesGcmEnvelope.seal(encryptCipher(), ByteArray(32) { 4 }, aad)

    val tamperedCiphertext = blob.copyOf().also {
      it[1 + 12 + 3] = (it[1 + 12 + 3].toInt() xor 0x01).toByte()
    }
    val parsedCiphertext = AesGcmEnvelope.parse(tamperedCiphertext)
    assertThrows(BlobCorruptException::class.java) {
      AesGcmEnvelope.open(decryptCipher(parsedCiphertext.iv), parsedCiphertext, aad)
    }

    val tamperedTag = blob.copyOf().also {
      it[it.size - 1] = (it[it.size - 1].toInt() xor 0x80).toByte()
    }
    val parsedTag = AesGcmEnvelope.parse(tamperedTag)
    assertThrows(BlobCorruptException::class.java) {
      AesGcmEnvelope.open(decryptCipher(parsedTag.iv), parsedTag, aad)
    }
  }

  // TC-SA-010
  @Test
  fun versionMismatchAndTruncatedBlobFailClosed() {
    val blob = AesGcmEnvelope.seal(encryptCipher(), ByteArray(16) { 1 }, aad)

    val wrongVersion = blob.copyOf().also { it[0] = 2 }
    assertThrows(BlobCorruptException::class.java) { AesGcmEnvelope.parse(wrongVersion) }
    assertThrows(BlobCorruptException::class.java) { AesGcmEnvelope.parse(blob.copyOf(1 + 12 + 15)) }
    assertThrows(BlobCorruptException::class.java) { AesGcmEnvelope.parse(ByteArray(0)) }
  }

  // TC-SA-011
  @Test
  fun ivIsUniqueAcrossSeals() {
    val first = AesGcmEnvelope.parse(AesGcmEnvelope.seal(encryptCipher(), ByteArray(16) { 2 }, aad))
    val second = AesGcmEnvelope.parse(AesGcmEnvelope.seal(encryptCipher(), ByteArray(16) { 2 }, aad))
    assertFalse(first.iv.contentEquals(second.iv))
  }

  // TC-SA-011
  @Test
  fun sealProducesDifferentCiphertextForTheSamePlaintext() {
    val ivOne = ByteArray(12) { 1 }
    val ivTwo = ByteArray(12) { 2 }
    val first = AesGcmEnvelope.parse(AesGcmEnvelope.seal(encryptCipher(ivOne), ByteArray(16) { 8 }, aad))
    val second = AesGcmEnvelope.parse(AesGcmEnvelope.seal(encryptCipher(ivTwo), ByteArray(16) { 8 }, aad))
    assertFalse(first.ciphertext.contentEquals(second.ciphertext))
  }

  // TC-SA-012
  @Test
  fun sealWipesItsPlaintextInput() {
    val plaintext = ByteArray(32) { index -> (index + 11).toByte() }
    AesGcmEnvelope.seal(encryptCipher(), plaintext, aad)
    assertArrayEquals(ByteArray(32), plaintext)
    assertTrue(plaintext.all { it == 0.toByte() })
  }
}
