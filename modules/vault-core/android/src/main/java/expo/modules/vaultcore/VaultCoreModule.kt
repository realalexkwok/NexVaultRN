package expo.modules.vaultcore

import android.content.Context
import expo.modules.kotlin.functions.Coroutine
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.securityandroid.GateDeniedException
import expo.modules.securityandroid.SecurityGate
import java.text.Normalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * vault-core Expo module (spec: vault-core sections 2.1-2.4).
 *
 * T3.2 adds the fail-closed security gate: [importMnemonicGuarded] and [signDigestGuarded] call the
 * security-android gate before any key material exists and translate a denial into the frozen
 * E_VAULT_SECURITY_GATE code. The guarded methods are the exact code the AsyncFunction lambdas run,
 * so the device suite can exercise the production coupling without a JS runtime (T1.2 precedent).
 */
class VaultCoreModule : Module() {
  private val handleStore = HandleStore()
  private var cachedWordlist: List<String>? = null

  override fun definition() = ModuleDefinition {
    Name("VaultCore")

    AsyncFunction("importMnemonic") Coroutine { bytes: ByteArray ->
      withContext(Dispatchers.Default) {
        importMnemonicGuarded(context(), bytes)
      }
    }

    AsyncFunction("deriveAddress") Coroutine { handle: String, path: String ->
      withContext(Dispatchers.Default) {
        // The seed stays native: only the public address string is returned (R1). The store hands
        // back an independent copy, so a concurrent destroy cannot zero it mid-derivation.
        val seedCopy = handleStore.copyOf(handle) ?: throw HandleNotFoundException()
        val indices = Bip32.parseEthereumPath(path)
        try {
          Bip32.toChecksumAddress(Bip32.deriveAddressBytes(seedCopy, indices))
        } finally {
          seedCopy.fill(0)
        }
      }
    }

    AsyncFunction("signDigest") Coroutine { handle: String, path: String, digest: ByteArray ->
      withContext(Dispatchers.Default) {
        signDigestGuarded(context(), handle, path, digest)
      }
    }

    AsyncFunction("destroy") Coroutine { handle: String ->
      withContext(Dispatchers.Default) {
        // The store evicts and wipes in one critical section: idempotent and race-free against an
        // in-flight sign/derive. destroy is deliberately not gated: the recovery path stays open.
        handleStore.evict(handle)
        Unit
      }
    }
  }

  /**
   * Production import path: gate first, then decode/validate/derive. The caller's bytes are wiped
   * on every path -- including a gate denial, which happens before the decode (R1).
   */
  internal fun importMnemonicGuarded(context: Context, bytes: ByteArray): String {
    try {
      assertSecurityGate(context)
      val normalized = Normalizer.normalize(String(bytes, Charsets.UTF_8), Normalizer.Form.NFKD)
      val mnemonic = normalized.trim()
      if (!Bip39.validateMnemonic(mnemonic, wordlist(context))) {
        throw InvalidMnemonicException()
      }
      val seed = Bip39.mnemonicToSeed(mnemonic, "")
      try {
        return handleStore.put(seed)
      } catch (error: Throwable) {
        // The store owns the seed only once put() succeeded; never leak it on failure.
        seed.fill(0)
        throw error
      }
    } finally {
      bytes.fill(0)
    }
  }

  /**
   * Production signing path: gate first (fail-closed before the store is read), then the frozen
   * digest length and derivation-path checks, then the RFC6979 signature. The digest is a public
   * transaction hash and is not wiped (R1); the seed copy is wiped in finally.
   */
  internal fun signDigestGuarded(
    context: Context,
    handle: String,
    path: String,
    digest: ByteArray
  ): ByteArray {
    assertSecurityGate(context)
    Ecdsa.requireDigest32(digest)
    val seedCopy = handleStore.copyOf(handle) ?: throw HandleNotFoundException()
    val indices = Bip32.parseEthereumPath(path)
    try {
      return Ecdsa.signDigest(seedCopy, indices, digest)
    } finally {
      seedCopy.fill(0)
    }
  }

  /**
   * Fail-closed gate (AGENTS.md R1/R6). A denial is translated into the frozen vault-core code so
   * callers never branch on the security module's internal E_SEC_* namespace.
   */
  private fun assertSecurityGate(context: Context) {
    try {
      SecurityGate.assertSigningAllowed(context)
    } catch (e: GateDeniedException) {
      throw SecurityGateException()
    }
  }

  private fun wordlist(context: Context): List<String> =
    cachedWordlist ?: Bip39.loadWordlist(context).also { cachedWordlist = it }

  private fun context(): Context =
    requireNotNull(appContext.reactContext) { "React context is unavailable" }
}
