package expo.modules.vaultcore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Best-effort instrumentation smoke for T1.3 on a real device (H3 closure). The mandatory
 * automated gate remains the JVM [Bip39Test]/[Bip32Test] suites; this class proves that the module
 * ships, that both async functions are registered, and that the BIP-32/keccak path reproduces the
 * canonical address on ART using the real `res/raw` wordlist.
 *
 * Limitation (recorded in validation.md): the JS→native bridge invocation itself is still not
 * exercised here — that needs a JS runtime (dev-build screen or jest), carried as Handoff T1.3-H8.
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

  // TC-VC-012
  @Test
  fun moduleDefinitionExposesImportMnemonicAndDeriveAddress() {
    val definition = VaultCoreModule().definition()
    assertEquals("VaultCore", definition.name)
    assertTrue(definition.asyncFunctions.containsKey("importMnemonic"))
    assertTrue(definition.asyncFunctions.containsKey("deriveAddress"))
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
      assertEquals("0x9858EfFD232B4033E47d90003D41EC34EcaEda94", address)
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

  private companion object {
    const val CANONICAL_MNEMONIC =
      "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
    const val CANONICAL_PATH = "m/44'/60'/0'/0/0"
  }
}
