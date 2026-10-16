package expo.modules.vaultcore

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * JVM gate for the T1.4 handle-store destroy semantics (H1).
 *
 * The instrumentation suite runs the same class on-device; the JS→native bridge invocation
 * itself stays Handoff T1.3-H8 (owner-adjudicated replan → T4.1) and is never claimed here.
 */
class HandleStoreTest {
  // TC-VC-024
  @Test
  fun destroyEvictsWipesAndIsIdempotent() {
    val store = HandleStore()
    val seed = ByteArray(64) { index -> (index + 1).toByte() }
    val handle = store.put(seed)
    assertEquals(1, store.size())

    val evicted = store.evict(handle)
    assertNotNull("destroy must hand the stored seed back for wiping", evicted)
    // The module's destroy implementation is evict(handle)?.fill(0).
    evicted!!.fill(0)
    assertArrayEquals(ByteArray(64), evicted)

    assertNull("the handle must be gone after destroy", store.get(handle))
    assertNull("a second destroy must be a no-op, not an error", store.evict(handle))
    assertEquals(0, store.size())
  }

  // TC-VC-024
  @Test
  fun thousandPutEvictCyclesLeaveStoreEmpty() {
    val store = HandleStore()
    repeat(1000) {
      val seed = ByteArray(64) { index -> (index + 7).toByte() }
      val handle = store.put(seed)
      store.evict(handle)?.fill(0)
    }
    assertEquals("1000 import/destroy cycles must leave no entries", 0, store.size())
  }
}
