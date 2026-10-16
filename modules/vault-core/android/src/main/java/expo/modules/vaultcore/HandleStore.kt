package expo.modules.vaultcore

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Native-only handle store: maps an opaque handle to the BIP-39 seed bytes (R1 — the seed
 * never crosses the JS↔native boundary).
 *
 * - [put] generates a random UUID handle and takes ownership of the given seed array.
 * - [get] returns the stored array; callers copy before mutating.
 * - [evict] removes the entry and hands the former array back so the caller can wipe it.
 *   `destroy` is therefore evict-then-wipe and **idempotent**: a missing handle returns null
 *   and never throws (spec: vault-core §2.4).
 *
 * An in-flight derive/sign holds its own `copyOf()`, so eviction during a call is safe: the
 * in-flight operation completes on its copy while later calls fail with the handle-not-found
 * code.
 */
internal class HandleStore {
  private val entries = ConcurrentHashMap<String, ByteArray>()

  internal fun put(seed: ByteArray): String {
    val handle = UUID.randomUUID().toString()
    entries[handle] = seed
    return handle
  }

  internal fun get(handle: String): ByteArray? = entries[handle]

  internal fun evict(handle: String): ByteArray? = entries.remove(handle)

  internal fun size(): Int = entries.size
}
