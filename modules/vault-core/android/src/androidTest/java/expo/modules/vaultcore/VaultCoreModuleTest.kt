package expo.modules.vaultcore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Best-effort instrumentation smoke for T1.2. The mandatory automated gate is the
 * JVM [Bip39Test] vector suite (spec Q5); this class adds real-device raw-resource
 * and module-definition coverage when a device/emulator is available.
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
  fun moduleDefinitionExposesImportMnemonic() {
    val definition = VaultCoreModule().definition()
    assertEquals("VaultCore", definition.name)
    assertTrue(definition.asyncFunctions.containsKey("importMnemonic"))
    // Expo's ModuleDefinitionBuilder also registers framework-internal
    // startObserving/stopObserving async functions, so the map is intentionally not
    // asserted to contain only our function.
  }
}
