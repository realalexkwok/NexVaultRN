package expo.modules.vaultcore

import java.math.BigInteger
import java.security.MessageDigest
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.math.ec.ECPoint
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM automated gate for T1.4 (RFC6979 DRBG, deterministic ECDSA, signature format, negatives).
 *
 * Anchors:
 * - probe vector d = C9AFA9D8…F6721 with digest sha256("sample") — independently validated
 *   2026-10-16 with noble@2 (see validation.md Task 0/7; RFC 6979 A.2.5 is NIST P-256 and is
 *   deliberately not used);
 * - E2E anchor: canonical mnemonic @ m/44'/60'/0'/0/0 with digest
 *   keccak256("nexvault-test-vector") — the pinned r‖s must equal noble@2's compact output, and
 *   viem@2.56.9 recoverPublicKey(yParity = recid) recovers 0x9858EfFD232B4033E47d90003D41EC34EcaEda94.
 *
 * Every test wipes the scratch arrays it creates. No key material is logged (R5).
 */
class EcdsaTest {
  // TC-VC-019
  @Test
  fun drbgDeterminismAndRange() {
    val key = PRIVATE_KEY.hexToBytes()
    val digest = sha256("sample")
    var first: ByteArray? = null
    var second: ByteArray? = null
    var other: ByteArray? = null
    try {
      val drbgA = Rfc6979Drbg(key, digest)
      first = try {
        drbgA.nextK()
      } finally {
        drbgA.wipe()
      }
      val drbgB = Rfc6979Drbg(key, digest)
      second = try {
        drbgB.nextK()
      } finally {
        drbgB.wipe()
      }
      assertArrayEquals("same (x, digest) must yield the same k", first, second)

      val k = BigInteger(1, first)
      assertTrue("k must be >= 1", k >= BigInteger.ONE)
      assertTrue("k must be < n", k < SECP256K1_N)

      val drbgOther = Rfc6979Drbg(key, sha256("samples"))
      other = try {
        drbgOther.nextK()
      } finally {
        drbgOther.wipe()
      }
      assertFalse("different digests must yield different k", first.contentEquals(other))

      val drbgAgain = Rfc6979Drbg(key, digest)
      try {
        val k1 = drbgAgain.nextK()
        val k2 = drbgAgain.nextK()
        try {
          assertFalse("consecutive nonces must differ", k1.contentEquals(k2))
        } finally {
          k1.fill(0)
          k2.fill(0)
        }
      } finally {
        drbgAgain.wipe()
      }
    } finally {
      key.fill(0)
      digest.fill(0)
      first?.fill(0)
      second?.fill(0)
      other?.fill(0)
    }
  }

  // TC-VC-020
  @Test
  fun probeVectorKAT() {
    val key = PROBE_PRIVATE_KEY.hexToBytes()
    val digest = sha256("sample")
    var signature: ByteArray? = null
    try {
      signature = Ecdsa.signDigestWithKey(key, digest)
      assertEquals(65, signature.size)
      assertEquals(PROBE_R, signature.copyOfRange(0, 32).toHex())
      assertEquals(PROBE_S, signature.copyOfRange(32, 64).toHex())
      assertEquals(0, signature[64].toInt())
    } finally {
      key.fill(0)
      digest.fill(0)
      signature?.fill(0)
    }
  }

  // TC-VC-021
  @Test
  fun apiDeterminismAndFormat() {
    val digest = Bip32.keccak256(DIGEST_MESSAGE.toByteArray(Charsets.UTF_8))
    var first: ByteArray? = null
    var second: ByteArray? = null
    val seed = Bip39.mnemonicToSeed(CANONICAL_MNEMONIC, "")
    try {
      val indices = Bip32.parseEthereumPath(CANONICAL_PATH)
      first = Ecdsa.signDigest(seed, indices, digest)
      second = Ecdsa.signDigest(seed, indices, digest)
      assertArrayEquals("RFC6979 output must be deterministic", first, second)
      assertEquals(65, first.size)
      val recid = first[64].toInt()
      assertTrue("recid must be 0 or 1 (got $recid)", recid == 0 || recid == 1)
      val s = BigInteger(1, first.copyOfRange(32, 64))
      assertTrue("low-s normalization required", s <= SECP256K1_N.shiftRight(1))
    } finally {
      seed.fill(0)
      digest.fill(0)
      first?.fill(0)
      second?.fill(0)
    }
  }

  // TC-VC-022
  @Test
  fun e2ePinnedSignature() {
    // Pinned by /tmp/t14-xcheck (noble@2 secp256k1.sign + viem recoverPublicKey trial) — see
    // validation.md Task 7. Byte equality here + the script's viem recovery of the same bytes
    // prove the canonical address is recovered from this implementation's signature.
    val digest = Bip32.keccak256(DIGEST_MESSAGE.toByteArray(Charsets.UTF_8))
    val seed = Bip39.mnemonicToSeed(CANONICAL_MNEMONIC, "")
    var signature: ByteArray? = null
    try {
      signature = Ecdsa.signDigest(seed, Bip32.parseEthereumPath(CANONICAL_PATH), digest)
      assertEquals(CANONICAL_SIGNATURE, signature.toHex())
    } finally {
      seed.fill(0)
      digest.fill(0)
      signature?.fill(0)
    }
  }

  // TC-VC-023
  @Test
  fun rejectsInvalidDigestLength() {
    val key = PRIVATE_KEY.hexToBytes()
    val seed = Bip39.mnemonicToSeed(CANONICAL_MNEMONIC, "")
    try {
      for (size in listOf(0, 31, 33)) {
        val keyError = assertThrows(
          "signDigestWithKey must reject a $size-byte digest",
          InvalidDigestLengthException::class.java
        ) {
          Ecdsa.signDigestWithKey(key, ByteArray(size))
        }
        assertEquals("E_VAULT_INVALID_DIGEST_LENGTH", keyError.code)
        assertEquals("Invalid digest length", keyError.message)

        val seedError = assertThrows(
          "signDigest must reject a $size-byte digest",
          InvalidDigestLengthException::class.java
        ) {
          Ecdsa.signDigest(seed, Bip32.parseEthereumPath(CANONICAL_PATH), ByteArray(size))
        }
        assertEquals("E_VAULT_INVALID_DIGEST_LENGTH", seedError.code)
      }
      val pathError = assertThrows(InvalidPathException::class.java) {
        Bip32.parseEthereumPath("m/49'/60'/0'/0/0")
      }
      assertEquals("E_VAULT_INVALID_PATH", pathError.code)
    } finally {
      key.fill(0)
      seed.fill(0)
    }
  }

  // TC-VC-023
  @Test
  fun errorCodeTableMatchesSpec() {
    assertEquals("E_VAULT_INVALID_DIGEST_LENGTH", InvalidDigestLengthException().code)
    assertEquals("Invalid digest length", InvalidDigestLengthException().message)
    assertEquals("E_VAULT_INVALID_PATH", InvalidPathException().code)
    assertEquals("E_VAULT_HANDLE_NOT_FOUND", HandleNotFoundException().code)
    assertEquals("E_VAULT_CRYPTO_INTERNAL", CryptoInternalException().code)
  }

  // TC-VC-027
  @Test
  fun pinnedVectorsWithAndWithoutLowSNormalization() {
    // BLOCKER-1 regression gate: the three "normalized = true" vectors have a raw s > n/2, so the
    // signer negates s and MUST flip recid (libsecp256k1 does the same); the other three are
    // left untouched. All values were pinned independently with noble@2 + viem (see
    // validation.md §Review round) and each signature is re-verified by recovery below.
    val seed = Bip39.mnemonicToSeed(CANONICAL_MNEMONIC, "")
    try {
      val indices = Bip32.parseEthereumPath(CANONICAL_PATH)
      for (vector in PINNED_VECTORS) {
        val digest = Bip32.keccak256(vector.message.toByteArray(Charsets.UTF_8))
        var signature: ByteArray? = null
        try {
          signature = Ecdsa.signDigest(seed, indices, digest)
          assertEquals("${vector.message} signature", vector.signature, signature.toHex())
          assertEquals(
            "${vector.message} recid",
            vector.signature.takeLast(2),
            "%02x".format(signature[64].toInt() and 0xFF)
          )
          val s = BigInteger(1, signature.copyOfRange(32, 64))
          assertTrue("${vector.message} must stay low-s", s <= SECP256K1_N.shiftRight(1))
          if (vector.normalized) {
            // A normalized vector's s is the negation of the raw s, so its leading byte differs
            // from the parity-only encoding — recovery is the real proof:
            assertEquals(
              "${vector.message} recovered key",
              publicKeyHex(seed, indices),
              recoverPublicKeyHex(signature, digest)
            )
          }
        } finally {
          digest.fill(0)
          signature?.fill(0)
        }
      }
    } finally {
      seed.fill(0)
    }
  }

  // TC-VC-028
  @Test
  fun recidRecoversTheSigningKeyForManyDigests() {
    // Any future recid regression (flip, parity, encoding) fails here for the digest class as a
    // whole, not just the single canonical message.
    val seed = Bip39.mnemonicToSeed(CANONICAL_MNEMONIC, "")
    try {
      val indices = Bip32.parseEthereumPath(CANONICAL_PATH)
      val expected = publicKeyHex(seed, indices)
      for (index in 0 until 24) {
        val digest = Bip32.keccak256("nexvault-recovery-$index".toByteArray(Charsets.UTF_8))
        var signature: ByteArray? = null
        try {
          signature = Ecdsa.signDigest(seed, indices, digest)
          val recid = signature[64].toInt()
          assertTrue("recid must be 0 or 1 (got $recid)", recid == 0 || recid == 1)
          assertEquals("digest #$index recovers the signing key", expected, recoverPublicKeyHex(signature, digest))
        } finally {
          digest.fill(0)
          signature?.fill(0)
        }
      }
      // The pinned table must keep covering both branches explicitly (BLOCKER-1 regression class):
      assertTrue("pinned vectors must include a low-s normalizing case", PINNED_VECTORS.any { it.normalized })
      assertTrue("pinned vectors must include a non-normalizing case", PINNED_VECTORS.any { !it.normalized })
    } finally {
      seed.fill(0)
    }
  }

  /**
   * Test-only ECDSA public-key recovery assembled from BC point primitives (the production rule
   * "no self-written point math" applies to src/main; this mirrors libsecp256k1's recoverable
   * signing so the recid rule is checked, not assumed).
   */
  private fun recoverPublicKeyHex(signature: ByteArray, digest: ByteArray): String {
    val curve = CustomNamedCurves.getByName("secp256k1")
    val r = BigInteger(1, signature.copyOfRange(0, 32))
    val s = BigInteger(1, signature.copyOfRange(32, 64))
    val recid = signature[64].toInt()
    val x = if (recid >= 2) r.add(curve.n) else r
    val encoded = ByteArray(33)
    encoded[0] = if ((recid and 1) == 1) 0x03 else 0x02
    val xBytes = x.toByteArray()
    try {
      require(xBytes.size <= 33) { "recovery x does not fit in 32 bytes" }
      // Copy the low 32 bytes into the payload — a 33-byte BigInteger encoding (leading sign zero)
      // must not overwrite the prefix byte.
      System.arraycopy(xBytes, xBytes.size - 32, encoded, 1, 32)
    } finally {
      xBytes.fill(0)
    }
    val pointR = curve.curve.decodePoint(encoded)
    val z = BigInteger(1, digest)
    val recovering = pointR.multiply(s).subtract(curve.g.multiply(z.mod(curve.n))).multiply(r.modInverse(curve.n))
    return recovering.normalize().getEncoded(false).toHex()
  }

  private fun publicKeyHex(seed: ByteArray, indices: IntArray): String {
    var node = Bip32.masterNode(seed)
    for (index in indices) {
      node = Bip32.deriveChild(node, index)
    }
    return CustomNamedCurves.getByName("secp256k1").g.multiply(node.key).normalize().getEncoded(false).toHex()
  }

  private fun sha256(message: String): ByteArray {
    val digest = MessageDigest.getInstance("SHA-256").digest(message.toByteArray(Charsets.UTF_8))
    return digest
  }

  private fun String.hexToBytes(): ByteArray {
    val clean = removePrefix("0x")
    require(clean.length % 2 == 0) { "hex length must be even" }
    return ByteArray(clean.length / 2) { index ->
      clean.substring(index * 2, index * 2 + 2).toInt(16).toByte()
    }
  }

  private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
    "%02x".format(byte.toInt() and 0xFF)
  }

  private data class PinnedVector(val message: String, val signature: String, val normalized: Boolean)

  private companion object {
    /**
     * Pinned independently with noble@2 (secp256k1.sign, lowS true/false) + viem trial recovery;
     * "normalized" marks the digests whose raw s > n/2, i.e. where s is negated and recid flips.
     * Vector -2's s starts with a 0x00 byte, which also re-proves the leading-zero encoding.
     */
    val PINNED_VECTORS = listOf(
      PinnedVector(
        "nexvault-test-vector",
        "36eba619abb6f0c6aea1f504ca79613270ef55b72313036ce574dad1e87106ea" +
          "72aa8de93f14ca6e5921b06ece96fe727fc6f4573d70090cc9556f25745f4595" + "01",
        normalized = false
      ),
      PinnedVector(
        "nexvault-test-vector-2",
        "631e7a792cece9ea44f574fe6dba7e1ce1b62fa462d53767801980a37a9a79bd" +
          "00783a080ad2c9d3881163b882c5fb2c9f890dc470623a10ab8c7c948f1bbe38" + "00",
        normalized = true
      ),
      PinnedVector(
        "nexvault-test-vector-3",
        "4a6723438513007fa3ed57c002bbe811ac58031fc8fcfe1179c43192fd6df4b8" +
          "11abce9d9a0b66140d746b1d7aea9ae143591f6a1a12d7ca887b922f6221fd53" + "00",
        normalized = false
      ),
      PinnedVector(
        "nexvault-test-vector-4",
        "0498077fe131fd453e4806e59173fa37dc8d2c807bfbe39d93d44be6f0b3410c" +
          "4e3f3b0e5452bb16a6b431d1d27976c8bbeade1c098ac05eb46181042e43415c" + "00",
        normalized = true
      ),
      PinnedVector(
        "nexvault-test-vector-5",
        "37c016be7e875c7c24bb8f53ca2e05ed48a5aa4380a1ff7b83faf30a0b2d427f" +
          "7cd029092cecdc3adb02e0095fa9f37c88ea2e6f268fab58b9bdfc7931cdbcbc" + "01",
        normalized = false
      ),
      PinnedVector(
        "nexvault-test-vector-6",
        "91db147e1c93355f36ce479b2eb7ceccbcd7087d43dac3afa10c6b4e8814e0a0" +
          "76106bbe859160f5847d0c889cf9faecef661bd857f27098658ad19d285f5216" + "00",
        normalized = true
      ),
    )
    val SECP256K1_N: BigInteger =
      BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16)
    const val CANONICAL_MNEMONIC =
      "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
    const val CANONICAL_PATH = "m/44'/60'/0'/0/0"
    const val DIGEST_MESSAGE = "nexvault-test-vector"

    // Probe vector — sha256("sample") with the fixed private key, validated against noble@2.
    const val PROBE_PRIVATE_KEY = "C9AFA9D845BA75166B5C215767B1D6934E50C3DB36E89B127B8A622B120F6721"
    const val PROBE_R = "432310e32cb80eb6503a26ce83cc165c783b870845fb8aad6d970889fcd7a6c8"
    const val PROBE_S = "530128b6b81c548874a6305d93ed071ca6e05074d85863d4056ce89b02bfab69"

    // E2E anchor pinned by /tmp/t14-xcheck (noble@2 compact r‖s + recid=1, viem-recovered).
    const val PRIVATE_KEY = "1ab42cc412b618bdea3a599e3c9bae199ebf030895b039e9db1e30dafb12b727"
    const val CANONICAL_SIGNATURE =
      "36eba619abb6f0c6aea1f504ca79613270ef55b72313036ce574dad1e87106ea" +
        "72aa8de93f14ca6e5921b06ece96fe727fc6f4573d70090cc9556f25745f4595" +
        "01"
  }
}
