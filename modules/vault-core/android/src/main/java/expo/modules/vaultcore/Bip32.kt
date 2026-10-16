package expo.modules.vaultcore

import java.math.BigInteger
import org.bouncycastle.crypto.digests.KeccakDigest
import org.bouncycastle.crypto.digests.RIPEMD160Digest
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.params.KeyParameter

/**
 * BIP-32 (secp256k1) + BIP-44 EVM path parsing + Keccak-256 + EIP-55 checksum.
 *
 * Implementation notes:
 * - **BouncyCastle 1.86 ships no HD-key API** (neither `HDKeyDerivation` nor `HDKey` exist in the
 *   1.86 jar — verified by listing the artifact). The BIP-32 CKD *glue* (HMAC-SHA512,
 *   ser32/ser256 concatenation, IL + k_parent mod n, fingerprinting) is therefore written here,
 *   while **all curve arithmetic is delegated to BC** (`CustomNamedCurves.getByName("secp256k1")`),
 *   which satisfies spec: vault-core §2.2 ("点运算禁止自实现").
 * - Lightweight API only (`org.bouncycastle.crypto.*`); `BouncyCastleProvider` is never registered.
 * - Every scratch byte-array (HMAC output, chain codes, pubkey/serialization buffers) is wiped in
 *   a `finally` block. `java.math.BigInteger` intermediates are immutable and not zeroizable —
 *   recorded as a **[C]** caveat in the T1.3 validation zeroization review.
 * - No `Log.*` calls; the typed error messages never interpolate key material (R5).
 */
internal object Bip32 {
  private const val HARDENED = 0x80000000.toInt()
  private const val ADDRESS_BYTES = 20
  private const val MASTER_SECRET = "Bitcoin seed"
  private val EMPTY_BYTES = ByteArray(0)
  private val HEX_DIGITS = "0123456789abcdef".toCharArray()

  /** BIP-44 EVM path whitelist: `m/44'/60'/<account>'/0/<index>` (`h` hardened spelling allowed). */
  private val PATH_PATTERN = Regex("^m/44['h]/60['h]/(\\d+)['h]/0/(\\d+)$")

  private val curve by lazy { CustomNamedCurves.getByName("secp256k1") }

  /** One BIP-32 node: private key + chain code + serialization metadata. */
  internal class Node(
    val key: BigInteger,
    val chainCode: ByteArray,
    val depth: Int,
    val parentFingerprint: Int,
    val childNumber: Int,
  )

  /** Parses the EVM BIP-44 path into hardened child numbers; throws [InvalidPathException]. */
  internal fun parseEthereumPath(path: String): IntArray {
    val match = PATH_PATTERN.matchEntire(path.trim()) ?: throw InvalidPathException()
    val account = match.groupValues[1].toLongOrNull() ?: throw InvalidPathException()
    val index = match.groupValues[2].toLongOrNull() ?: throw InvalidPathException()
    // HARDENED is Int.MIN_VALUE as an Int; compare against the unsigned 2^31 bound instead.
    if (account >= 0x80000000L || index >= 0x80000000L) {
      throw InvalidPathException()
    }
    return intArrayOf(
      44 or HARDENED,
      60 or HARDENED,
      account.toInt() or HARDENED,
      0,
      index.toInt(),
    )
  }

  /** BIP-32 master node from a BIP-39 seed (HMAC-SHA512 with key "Bitcoin seed"). */
  internal fun masterNode(seed: ByteArray): Node {
    val i = hmacSha512(MASTER_SECRET.toByteArray(Charsets.US_ASCII), seed)
    try {
      val il = i.copyOfRange(0, 32)
      val ir = i.copyOfRange(32, 64)
      try {
        val key = BigInteger(1, il)
        if (key.signum() == 0 || key >= curve.n) {
          throw CryptoInternalException()
        }
        return Node(key, ir, 0, 0, 0)
      } finally {
        il.fill(0)
      }
    } finally {
      i.fill(0)
    }
  }

  /** BIP-32 CKDpriv for one child index (hardened when bit 31 is set). */
  internal fun deriveChild(parent: Node, childNumber: Int): Node {
    val hardened = (childNumber and HARDENED) != 0
    val data = if (hardened) {
      ByteArray(1 + 32 + 4).also { out ->
        out[0] = 0
        writeSer256(parent.key, out, 1)
        writeSer32(childNumber, out, 33)
      }
    } else {
      val publicKey = compressedPublicKey(parent)
      try {
        ByteArray(33 + 4).also { out ->
          System.arraycopy(publicKey, 0, out, 0, 33)
          writeSer32(childNumber, out, 33)
        }
      } finally {
        publicKey.fill(0)
      }
    }

    val i = hmacSha512(parent.chainCode, data)
    data.fill(0)
    try {
      val il = i.copyOfRange(0, 32)
      val ir = i.copyOfRange(32, 64)
      try {
        val offset = BigInteger(1, il)
        if (offset >= curve.n) {
          ir.fill(0)
          throw CryptoInternalException()
        }
        val childKey = offset.add(parent.key).mod(curve.n)
        if (childKey.signum() == 0) {
          ir.fill(0)
          throw CryptoInternalException()
        }
        return Node(childKey, ir, parent.depth + 1, fingerprint(parent), childNumber)
      } finally {
        il.fill(0)
      }
    } finally {
      i.fill(0)
    }
  }

  /**
   * Derives the 20-byte EVM address: keccak256(uncompressed pubkey x||y)[12..32).
   * [seed] is a caller-owned copy; this function never retains it.
   */
  internal fun deriveAddressBytes(seed: ByteArray, pathIndices: IntArray): ByteArray {
    val chainCodes = ArrayList<ByteArray>(pathIndices.size + 1)
    var publicKey: ByteArray = EMPTY_BYTES
    var digest: ByteArray = EMPTY_BYTES
    try {
      var node = masterNode(seed)
      chainCodes.add(node.chainCode)
      for (index in pathIndices) {
        node = deriveChild(node, index)
        chainCodes.add(node.chainCode)
      }

      publicKey = uncompressedPublicKey(node)
      if (publicKey.size != 65) {
        throw CryptoInternalException()
      }
      val xy = ByteArray(64)
      try {
        System.arraycopy(publicKey, 1, xy, 0, 64)
        digest = keccak256(xy)
      } finally {
        xy.fill(0)
      }
      return digest.copyOfRange(digest.size - ADDRESS_BYTES, digest.size)
    } finally {
      publicKey.fill(0)
      digest.fill(0)
      for (chainCode in chainCodes) {
        chainCode.fill(0)
      }
    }
  }

  /** ERC-55 mixed-case checksum of a 20-byte address. */
  internal fun toChecksumAddress(addressBytes: ByteArray): String {
    if (addressBytes.size != ADDRESS_BYTES) {
      throw CryptoInternalException()
    }
    val lower = buildString(2 + ADDRESS_BYTES * 2) {
      append("0x")
      for (byte in addressBytes) {
        val value = byte.toInt() and 0xFF
        append(HEX_DIGITS[value ushr 4])
        append(HEX_DIGITS[value and 0x0F])
      }
    }
    val body = lower.substring(2)
    val hash = keccak256(body.toByteArray(Charsets.US_ASCII))
    try {
      val hashHex = buildString(hash.size * 2) {
        for (byte in hash) {
          val value = byte.toInt() and 0xFF
          append(HEX_DIGITS[value ushr 4])
          append(HEX_DIGITS[value and 0x0F])
        }
      }
      val checksummed = StringBuilder(lower.length)
      checksummed.append("0x")
      for (index in body.indices) {
        val character = body[index]
        val nibble = Character.digit(hashHex[index], 16)
        checksummed.append(if (character in 'a'..'f' && nibble >= 8) character.uppercaseChar() else character)
      }
      return checksummed.toString()
    } finally {
      hash.fill(0)
    }
  }

  /** Pre-NIST Keccak-256 (`KeccakDigest`, never NIST `SHA3Digest`). */
  internal fun keccak256(bytes: ByteArray): ByteArray {
    val digest = KeccakDigest(256)
    val out = ByteArray(digest.digestSize)
    digest.update(bytes, 0, bytes.size)
    digest.doFinal(out, 0)
    return out
  }

  /** 33-byte compressed public key (used for fingerprints and BIP-32 public serialization). */
  internal fun compressedPublicKey(node: Node): ByteArray = curve.g.multiply(node.key).getEncoded(true)

  /** 65-byte uncompressed public key (0x04 || x || y). */
  internal fun uncompressedPublicKey(node: Node): ByteArray = curve.g.multiply(node.key).getEncoded(false)

  /** First four bytes of HASH160(compressed pubkey), big-endian. */
  private fun fingerprint(node: Node): Int {
    val publicKey = compressedPublicKey(node)
    val sha = ByteArray(32)
    val ripemd = ByteArray(20)
    try {
      val shaDigest = SHA256Digest()
      shaDigest.update(publicKey, 0, publicKey.size)
      shaDigest.doFinal(sha, 0)
      val ripemdDigest = RIPEMD160Digest()
      ripemdDigest.update(sha, 0, sha.size)
      ripemdDigest.doFinal(ripemd, 0)
      return ((ripemd[0].toInt() and 0xFF) shl 24) or
        ((ripemd[1].toInt() and 0xFF) shl 16) or
        ((ripemd[2].toInt() and 0xFF) shl 8) or
        (ripemd[3].toInt() and 0xFF)
    } finally {
      publicKey.fill(0)
      sha.fill(0)
      ripemd.fill(0)
    }
  }

  private fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray {
    val mac = HMac(SHA512Digest())
    mac.init(KeyParameter(key))
    mac.update(data, 0, data.size)
    val out = ByteArray(mac.macSize)
    mac.doFinal(out, 0)
    return out
  }

  private fun writeSer256(value: BigInteger, out: ByteArray, offset: Int) {
    val bytes = value.toByteArray()
    try {
      when {
        bytes.size == 33 && bytes[0].toInt() == 0 -> System.arraycopy(bytes, 1, out, offset, 32)
        bytes.size <= 32 -> System.arraycopy(bytes, 0, out, offset + (32 - bytes.size), bytes.size)
        else -> throw CryptoInternalException()
      }
    } finally {
      bytes.fill(0)
    }
  }

  private fun writeSer32(value: Int, out: ByteArray, offset: Int) {
    out[offset] = (value ushr 24).toByte()
    out[offset + 1] = (value ushr 16).toByte()
    out[offset + 2] = (value ushr 8).toByte()
    out[offset + 3] = value.toByte()
  }
}
