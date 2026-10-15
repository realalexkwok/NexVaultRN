package expo.modules.vaultcore

import expo.modules.kotlin.functions.Coroutine
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import java.text.Normalizer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VaultCoreModule : Module() {
  private val handleStore = ConcurrentHashMap<String, ByteArray>()

  private val wordlist: List<String> by lazy {
    Bip39.loadWordlist(requireNotNull(appContext.reactContext))
  }

  override fun definition() = ModuleDefinition {
    Name("VaultCore")

    AsyncFunction("importMnemonic") Coroutine { bytes: ByteArray ->
      withContext(Dispatchers.Default) {
        val normalized = try {
          Normalizer.normalize(String(bytes, Charsets.UTF_8), Normalizer.Form.NFKD)
        } finally {
          // R1/zeroize: wipe the converted native copy immediately after decoding.
          bytes.fill(0)
        }
        val mnemonic = normalized.trim()

        if (!Bip39.validateMnemonic(mnemonic, wordlist)) {
          throw InvalidMnemonicException()
        }

        // T1.3/T1.4 consume this seed; T1.2 never returns or logs it.
        val seed = Bip39.mnemonicToSeed(mnemonic, "")
        val handle = UUID.randomUUID().toString()
        handleStore[handle] = seed
        handle
      }
    }
  }
}
