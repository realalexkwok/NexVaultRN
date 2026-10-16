package expo.modules.vaultcore

import java.math.BigInteger
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.params.KeyParameter

/**
 * RFC 6979 deterministic ECDSA over secp256k1 (T1.4).
 *
 * Design (owner decision 2026-10-16):
 * - the nonce DRBG (RFC 6979 §3.2, HMAC-SHA256) is written here so every k byte lives in a
 *   `ByteArray` under explicit `fill(0)` control (skill invariant 8);
 * - **all curve arithmetic stays in BouncyCastle** (`CustomNamedCurves` secp256k1 +
 *   `ECPoint.multiply`) — spec: vault-core §2.2 forbids self-written point math;
 * - output is 65 bytes `r(32) ‖ s(32) ‖ recid(1)` with **low-s normalization**
 *   (`s > n/2 → s = n − s`, BIP146/EIP-2 style) and **recid = y-parity of R ∈ {0,1}**
 *   (spec: vault-core §2.1); when `x(R) ≥ n` the nonce is retried instead of widening recid
 *   to 2/3, so the frozen {0,1} contract holds;
 * - the nonce is never supplied externally — k reuse would leak the private key (skill
 *   invariant 2);
 * - `java.math.BigInteger` intermediates (d, z, k, r, s, R coordinates) are immutable and not
 *   zeroizable, and BC's `HMac` keeps unwipeable pad state — both recorded as **[C]** caveats,
 *   never [P] (AGENTS.md R6);
 * - no `Log.*` calls; error messages never interpolate key material (R5).
 */
private const val ECDSA_FIELD_LENGTH = 32
private const val ECDSA_SIGNATURE_LENGTH = 65
private const val ECDSA_MAX_ATTEMPTS = 64
private val ECDSA_ZERO = byteArrayOf(0)
private val ECDSA_ONE = byteArrayOf(1)
private val secp256k1 by lazy { CustomNamedCurves.getByName("secp256k1") }

/** Big-endian fixed-length (32-byte) encoding; the scratch `toByteArray()` copy is wiped. */
private fun toFixedLength(value: BigInteger): ByteArray {
  val bytes = value.toByteArray()
  try {
    if (bytes.size > ECDSA_FIELD_LENGTH + 1) {
      throw CryptoInternalException()
    }
    val out = ByteArray(ECDSA_FIELD_LENGTH)
    when {
      bytes.size == ECDSA_FIELD_LENGTH + 1 && bytes[0].toInt() == 0 ->
        System.arraycopy(bytes, 1, out, 0, ECDSA_FIELD_LENGTH)
      bytes.size <= ECDSA_FIELD_LENGTH ->
        System.arraycopy(bytes, 0, out, ECDSA_FIELD_LENGTH - bytes.size, bytes.size)
      else -> throw CryptoInternalException()
    }
    return out
  } finally {
    bytes.fill(0)
  }
}

private fun concat(vararg chunks: ByteArray): ByteArray {
  var size = 0
  for (chunk in chunks) {
    size += chunk.size
  }
  val out = ByteArray(size)
  var offset = 0
  for (chunk in chunks) {
    System.arraycopy(chunk, 0, out, offset, chunk.size)
    offset += chunk.size
  }
  return out
}

/**
 * RFC 6979 §3.2 HMAC-SHA256 DRBG over the private key `x` and the message digest.
 * The constructor copies both inputs; [wipe] zeroes K, V, the x copy and h1.
 */
internal class Rfc6979Drbg(x: ByteArray, digest32: ByteArray) {
  private val mac = HMac(SHA256Digest())
  private val macLength = mac.macSize
  private val key = ByteArray(macLength)
  private val value = ByteArray(macLength)
  private val xBytes = x.copyOf()
  private val h1 = toFixedLength(BigInteger(1, digest32).mod(secp256k1.n))

  init {
    if (xBytes.size != ECDSA_FIELD_LENGTH || h1.size != ECDSA_FIELD_LENGTH) {
      wipe()
      throw CryptoInternalException()
    }
    // b/c: V = 0x01*; K = 0x00*
    value.fill(0x01)
    // d/e: K = HMAC_K(V || 0x00 || int2octets(x) || bits2octets(h1)); V = HMAC_K(V)
    updateKey(concat(value, ECDSA_ZERO, xBytes, h1))
    updateValue()
    // f/g: K = HMAC_K(V || 0x01 || int2octets(x) || bits2octets(h1)); V = HMAC_K(V)
    updateKey(concat(value, ECDSA_ONE, xBytes, h1))
    updateValue()
  }

  /**
   * Next candidate nonce (32-byte BE) per the h-loop; k ∈ [1, n−1] is guaranteed by the
   * `mod n` + zero check, with the §3.2 h.3 continuation applied internally.
   */
  internal fun nextK(): ByteArray {
    while (true) {
      val candidate = hmac(key, value)
      System.arraycopy(candidate, 0, value, 0, macLength)
      val k = BigInteger(1, candidate).mod(secp256k1.n)
      candidate.fill(0)
      if (k.signum() > 0) {
        return toFixedLength(k)
      }
      retry()
    }
  }

  /** RFC 6979 §3.2 h.3 continuation after the caller rejects a nonce (r == 0 / s == 0 / x ≥ n). */
  internal fun retry() {
    updateKey(concat(value, ECDSA_ZERO))
    updateValue()
  }

  /** Zeroes all DRBG state; safe to call repeatedly. */
  internal fun wipe() {
    key.fill(0)
    value.fill(0)
    xBytes.fill(0)
    h1.fill(0)
  }

  private fun updateKey(message: ByteArray) {
    try {
      val nextKey = hmac(key, message)
      System.arraycopy(nextKey, 0, key, 0, macLength)
      nextKey.fill(0)
    } finally {
      message.fill(0)
    }
  }

  private fun updateValue() {
    val nextValue = hmac(key, value)
    System.arraycopy(nextValue, 0, value, 0, macLength)
    nextValue.fill(0)
  }

  private fun hmac(keyMaterial: ByteArray, data: ByteArray): ByteArray {
    mac.init(KeyParameter(keyMaterial))
    mac.update(data, 0, data.size)
    val out = ByteArray(macLength)
    mac.doFinal(out, 0)
    return out
  }
}

internal object Ecdsa {
  private val EMPTY_BYTES = ByteArray(0)

  /** Fails fast when the digest is not exactly 32 bytes (`E_VAULT_INVALID_DIGEST_LENGTH`). */
  internal fun requireDigest32(digest: ByteArray) {
    if (digest.size != ECDSA_FIELD_LENGTH) {
      throw InvalidDigestLengthException()
    }
  }

  /**
   * Derives the child key at [pathIndices] from the caller-owned [seed] and signs [digest32].
   * Every derived private-key byte and chain code is wiped in `finally`.
   */
  internal fun signDigest(seed: ByteArray, pathIndices: IntArray, digest32: ByteArray): ByteArray {
    requireDigest32(digest32)
    val chainCodes = ArrayList<ByteArray>(pathIndices.size + 1)
    var privateKey32: ByteArray = EMPTY_BYTES
    try {
      var node = Bip32.masterNode(seed)
      chainCodes.add(node.chainCode)
      for (index in pathIndices) {
        node = Bip32.deriveChild(node, index)
        chainCodes.add(node.chainCode)
      }
      privateKey32 = toFixedLength(node.key)
      return signDigestWithKey(privateKey32, digest32)
    } finally {
      privateKey32.fill(0)
      for (chainCode in chainCodes) {
        chainCode.fill(0)
      }
    }
  }

  /**
   * Deterministic ECDSA over secp256k1 for a 32-byte [privateKey32] and [digest32].
   * Returns 65 bytes `r(32) ‖ s(32) ‖ recid(1)`; retries the DRBG continuation at most
   * [ECDSA_MAX_ATTEMPTS] times, then fails with `E_VAULT_CRYPTO_INTERNAL`.
   */
  internal fun signDigestWithKey(privateKey32: ByteArray, digest32: ByteArray): ByteArray {
    requireDigest32(digest32)
    if (privateKey32.size != ECDSA_FIELD_LENGTH) {
      throw CryptoInternalException()
    }

    val privateKeyCopy = privateKey32.copyOf()
    val digestCopy = digest32.copyOf()
    var drbg: Rfc6979Drbg? = null
    try {
      val d = BigInteger(1, privateKeyCopy)
      if (d.signum() == 0 || d >= secp256k1.n) {
        throw CryptoInternalException()
      }
      val z = BigInteger(1, digestCopy)
      val halfOrder = secp256k1.n.shiftRight(1)
      drbg = Rfc6979Drbg(privateKeyCopy, digestCopy)

      var attempt = 0
      while (attempt < ECDSA_MAX_ATTEMPTS) {
        attempt += 1
        val kBytes = drbg.nextK()
        try {
          val k = BigInteger(1, kBytes)
          // BC returns a projective point; affine coordinate access requires normalize().
          val point = secp256k1.g.multiply(k).normalize()
          val x = point.affineXCoord.toBigInteger()
          // x(R) >= n would need recovery-id bit 2; retry so recid stays in {0,1}.
          if (x >= secp256k1.n) {
            drbg.retry()
            continue
          }
          val r = x.mod(secp256k1.n)
          if (r.signum() == 0) {
            drbg.retry()
            continue
          }
          var s = k.modInverse(secp256k1.n).multiply(z.add(r.multiply(d))).mod(secp256k1.n)
          if (s.signum() == 0) {
            drbg.retry()
            continue
          }
          // low-s normalization (BIP146/EIP-2 style) — R and therefore recid are unaffected.
          if (s > halfOrder) {
            s = secp256k1.n.subtract(s)
          }
          val recid = if (point.affineYCoord.toBigInteger().testBit(0)) 1 else 0
          val rBytes = toFixedLength(r)
          val sBytes = toFixedLength(s)
          try {
            val signature = ByteArray(ECDSA_SIGNATURE_LENGTH)
            System.arraycopy(rBytes, 0, signature, 0, ECDSA_FIELD_LENGTH)
            System.arraycopy(sBytes, 0, signature, ECDSA_FIELD_LENGTH, ECDSA_FIELD_LENGTH)
            signature[ECDSA_SIGNATURE_LENGTH - 1] = recid.toByte()
            return signature
          } finally {
            rBytes.fill(0)
            sBytes.fill(0)
          }
        } finally {
          kBytes.fill(0)
        }
      }
      throw CryptoInternalException()
    } finally {
      privateKeyCopy.fill(0)
      digestCopy.fill(0)
      drbg?.wipe()
    }
  }
}
