package expo.modules.vaultcore

import java.util.UUID

/**
 * Native-only handle store: maps an opaque handle to the BIP-39 seed bytes (R1 — the seed never
 * crosses the JS↔native boundary).
 *
 * Concurrency contract (review MINOR-1) — every accessor is serialized on one monitor:
 * - [put] generates a random UUID handle and takes ownership of the given seed array;
 * - [copyOf] returns an **independent copy**, so a concurrent destroy can neither tear nor zero
 *   the buffer an in-flight derive/sign is reading;
 * - [evict] removes the entry and wipes the stored array **inside the same critical section**,
 *   making destroy atomic with respect to in-flight operations and idempotent (a missing handle
 *   is a no-op returning false — spec: vault-core §2.4 幂等语义);
 * - [size] reports the live entry count (used by tests).
 */
internal class HandleStore {
  private val lock = Any()
  private val entries = HashMap<String, ByteArray>()

  internal fun put(seed: ByteArray): String = synchronized(lock) {
    val handle = UUID.randomUUID().toString()
    entries[handle] = seed
    handle
  }

  /** Independent copy of the stored seed, or null when the handle is unknown/destroyed. */
  internal fun copyOf(handle: String): ByteArray? = synchronized(lock) {
    entries[handle]?.copyOf()
  }

  /** Removes and wipes the stored seed; returns false when the handle was already gone. */
  internal fun evict(handle: String): Boolean = synchronized(lock) {
    val seed = entries.remove(handle)
    if (seed == null) {
      false
    } else {
      seed.fill(0)
      true
    }
  }

  internal fun size(): Int = synchronized(lock) {
    entries.size
  }
}
