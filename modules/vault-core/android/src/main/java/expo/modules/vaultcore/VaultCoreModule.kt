package expo.modules.vaultcore

import expo.modules.kotlin.functions.Coroutine
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import java.text.Normalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VaultCoreModule : Module() {
  private val handleStore = HandleStore()

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
        try {
          handleStore.put(seed)
        } catch (error: Throwable) {
          // The store owns the seed only once put() succeeded; never leak it on failure.
          seed.fill(0)
          throw error
        }
      }
    }

    AsyncFunction("deriveAddress") Coroutine { handle: String, path: String ->
      withContext(Dispatchers.Default) {
        // The seed stays native: only the public address string is returned (R1).
        val seed = handleStore.get(handle) ?: throw HandleNotFoundException()
        val indices = Bip32.parseEthereumPath(path)
        val seedCopy = seed.copyOf()
        try {
          Bip32.toChecksumAddress(Bip32.deriveAddressBytes(seedCopy, indices))
        } finally {
          seedCopy.fill(0)
        }
      }
    }

    AsyncFunction("signDigest") Coroutine { handle: String, path: String, digest: ByteArray ->
      withContext(Dispatchers.Default) {
        // Fail fast before touching the store; the digest is a public tx hash (R1).
        Ecdsa.requireDigest32(digest)
        val seed = handleStore.get(handle) ?: throw HandleNotFoundException()
        val indices = Bip32.parseEthereumPath(path)
        val seedCopy = seed.copyOf()
        try {
          Ecdsa.signDigest(seedCopy, indices, digest)
        } finally {
          seedCopy.fill(0)
        }
      }
    }

    AsyncFunction("destroy") Coroutine { handle: String ->
      withContext(Dispatchers.Default) {
        // Evict first, then wipe: idempotent (a missing handle is a no-op, never an error).
        handleStore.evict(handle)?.fill(0)
        Unit
      }
    }
  }
}
