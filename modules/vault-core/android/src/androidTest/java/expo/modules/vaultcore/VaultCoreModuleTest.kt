package expo.modules.vaultcore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import android.os.Debug

/**
 * Best-effort instrumentation suite on a real device. The mandatory automated gate remains the
 * JVM suites ([Bip39Test]/[Bip32Test]/[EcdsaTest]/[HandleStoreTest]); this class proves that the
 * module ships, that all four async functions are registered, that the BIP-39/BIP-32/RFC6979 path
 * reproduces the canonical address and the pinned signature on ART with the real `res/raw`
 * wordlist, and that the handle store's destroy semantics wipe and stay idempotent.
 *
 * Limitation (recorded in validation.md): the JS→native bridge invocation itself is still not
 * exercised here — that needs a JS runtime (dev-build screen or jest), carried as Handoff
 * T1.3-H8 / T1.4-H1 (owner-adjudicated replan → T4.1). This suite calls the same code the
 * AsyncFunction runs, and never claims bridge coverage.
 */
@RunWith(AndroidJUnit4::class)
class VaultCoreModuleTest {
  // TC-VC-012
  @Test
  fun rawWordlistHasExactly2048Words() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val words = context.resources.openRawResource(R.raw.bip39_english).use { stream ->
      stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
        lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
      }
    }
    assertEquals(2048, words.size)
    assertTrue(words.contains("abandon"))
    assertTrue(words.contains("zoo"))
  }

  // TC-VC-012 / TC-VC-025
  @Test
  fun moduleDefinitionExposesAllFourFunctions() {
    val definition = VaultCoreModule().definition()
    assertEquals("VaultCore", definition.name)
    assertTrue(definition.asyncFunctions.containsKey("importMnemonic"))
    assertTrue(definition.asyncFunctions.containsKey("deriveAddress"))
    assertTrue(definition.asyncFunctions.containsKey("signDigest"))
    assertTrue(definition.asyncFunctions.containsKey("destroy"))
    // Expo's ModuleDefinitionBuilder also registers framework-internal
    // startObserving/stopObserving async functions, so the map is intentionally not size-pinned.
  }

  // TC-VC-017
  @Test
  fun canonicalAddressOnDevice() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val words = Bip39.loadWordlist(context)
    assertEquals(2048, words.size)
    assertTrue(Bip39.validateMnemonic(CANONICAL_MNEMONIC, words))

    val seed = Bip39.mnemonicToSeed(CANONICAL_MNEMONIC, "")
    try {
      val indices = Bip32.parseEthereumPath(CANONICAL_PATH)
      val address = Bip32.toChecksumAddress(Bip32.deriveAddressBytes(seed, indices))
      assertEquals(CANONICAL_ADDRESS, address)
    } finally {
      seed.fill(0)
    }
  }

  // TC-VC-017
  @Test
  fun invalidPathExposesTypedCode() {
    val error = try {
      Bip32.parseEthereumPath("m/49'/60'/0'/0/0")
      null
    } catch (e: InvalidPathException) {
      e
    }
    assertTrue("expected InvalidPathException", error != null)
    assertEquals("E_VAULT_INVALID_PATH", error!!.code)
  }

  // TC-VC-025
  @Test
  fun signAndDestroyOnDevice() {
    val seed = Bip39.mnemonicToSeed(CANONICAL_MNEMONIC, "")
    val digest = Bip32.keccak256(DIGEST_MESSAGE.toByteArray(Charsets.UTF_8))
    val signature = Ecdsa.signDigest(seed, Bip32.parseEthereumPath(CANONICAL_PATH), digest)
    try {
      assertEquals(65, signature.size)
      val recid = signature[64].toInt()
      assertTrue("recid must be 0 or 1 (got $recid)", recid == 0 || recid == 1)
      assertEquals("on-device signature must equal the noble@2 cross-check", CANONICAL_SIGNATURE, signature.toHex())
    } finally {
      seed.fill(0)
      digest.fill(0)
      signature.fill(0)
    }

    // destroy semantics, exercised on the exact store object the AsyncFunction uses.
    val store = HandleStore()
    val stored = ByteArray(64) { index -> (index + 3).toByte() }
    val handle = store.put(stored)
    assertEquals(1, store.size())
    val evicted = store.evict(handle)
    assertNotNull("destroy must hand the stored seed back for wiping", evicted)
    evicted!!.fill(0)
    assertArrayEquals(ByteArray(64), evicted)
    assertNull("the handle must be gone after destroy", store.get(handle))
    assertNull("a second destroy must be a no-op, not an error", store.evict(handle))
    assertEquals(0, store.size())
  }

  // TC-VC-025 (TC-VC-005 automated side)
  @Test
  fun thousandRoundLoop() {
    // Warm-up first: JIT, class loading and allocator pools otherwise masquerade as growth.
    // The 2026-10-16 first run measured VmRSS without a warm-up and read 5.83% on a re-run —
    // protocol corrected to the spec metric (PSS) with warm-up + GC settling; the ±5%
    // tolerance itself is unchanged (see validation.md Task 6.2).
    repeat(WARM_UP_ROUNDS) { roundTrip() }
    val before = settledPssKb()
    repeat(MEASURED_ROUNDS) { roundTrip() }
    val after = settledPssKb()
    val growthPercent = if (before > 0) (after - before).toDouble() / before.toDouble() * 100.0 else 0.0
    assertTrue(
      "$MEASURED_ROUNDS import/derive/sign/destroy cycles: PSS before=${before}kB after=${after}kB growth=${growthPercent}%",
      growthPercent <= 5.0
    )
  }

  /** One full import → derive → sign → destroy cycle against the real handle store. */
  private fun roundTrip() {
    val seed = Bip39.mnemonicToSeed(CANONICAL_MNEMONIC, "")
    val store = HandleStore()
    val handle = store.put(seed)
    val stored = store.get(handle)
    assertNotNull(stored)
    val seedCopy = stored!!.copyOf()
    try {
      val indices = Bip32.parseEthereumPath(CANONICAL_PATH)
      assertEquals(CANONICAL_ADDRESS, Bip32.toChecksumAddress(Bip32.deriveAddressBytes(seedCopy, indices)))
      val digest = Bip32.keccak256(DIGEST_MESSAGE.toByteArray(Charsets.UTF_8))
      try {
        val signature = Ecdsa.signDigest(seedCopy, indices, digest)
        assertEquals(65, signature.size)
        signature.fill(0)
      } finally {
        digest.fill(0)
      }
    } finally {
      seedCopy.fill(0)
    }
    store.evict(handle)?.fill(0)
  }

  /**
   * Post-GC settled PSS of this process (the metric spec TC-VC-005 names first), averaged over
   * three samples so a single GC pause cannot skew the before/after comparison.
   */
  private fun settledPssKb(): Long {
    var total = 0L
    repeat(3) {
      System.gc()
      Thread.sleep(250)
      val info = Debug.MemoryInfo()
      Debug.getMemoryInfo(info)
      total += info.totalPss.toLong()
    }
    return total / 3
  }

  private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
    "%02x".format(byte.toInt() and 0xFF)
  }

  private companion object {
    const val CANONICAL_MNEMONIC =
      "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
    const val CANONICAL_PATH = "m/44'/60'/0'/0/0"
    const val WARM_UP_ROUNDS = 100
    const val MEASURED_ROUNDS = 1000
    const val CANONICAL_ADDRESS = "0x9858EfFD232B4033E47d90003D41EC34EcaEda94"
    const val DIGEST_MESSAGE = "nexvault-test-vector"
    // Pinned by /tmp/t14-xcheck (noble@2 compact r‖s + recid=1, viem-recovered canonical address);
    // identical to the JVM EcdsaTest TC-VC-022 literal.
    const val CANONICAL_SIGNATURE =
      "36eba619abb6f0c6aea1f504ca79613270ef55b72313036ce574dad1e87106ea" +
        "72aa8de93f14ca6e5921b06ece96fe727fc6f4573d70090cc9556f25745f4595" +
        "01"
  }
}
