package expo.modules.vaultcore

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM gate for the T1.4 handle-store semantics (H1 + review MINOR-1).
 *
 * The instrumentation suite runs the same class on-device; the JS→native bridge invocation
 * itself stays Handoff T1.4-H1 (owner-adjudicated replan → T4.1) and is never claimed here.
 */
class HandleStoreTest {
  // TC-VC-024
  @Test
  fun destroyEvictsWipesAndIsIdempotent() {
    val store = HandleStore()
    val seed = ByteArray(64) { index -> (index + 1).toByte() }
    val handle = store.put(seed)
    assertEquals(1, store.size())

    assertTrue("first destroy must evict", store.evict(handle))
    // put() takes ownership of the caller's array, so an in-place wipe is observable here.
    assertArrayEquals("destroy must wipe the stored seed", ByteArray(64), seed)

    assertNull("the handle must be gone after destroy", store.copyOf(handle))
    assertFalse("a second destroy must be a no-op, not an error", store.evict(handle))
    assertEquals(0, store.size())
  }

  // TC-VC-024
  @Test
  fun copyOfIsIndependentOfTheStoredBuffer() {
    val store = HandleStore()
    val seed = ByteArray(64) { index -> (index + 9).toByte() }
    val handle = store.put(seed)

    val copy = store.copyOf(handle)
    assertNotSame("copyOf must not hand out the stored array", seed, copy)
    copy!!.fill(0)
    // Wiping the copy must leave the store intact for a later read.
    val second = store.copyOf(handle)
    assertEquals(seed[0].toInt(), second!![0].toInt())
    second.fill(0)

    assertTrue(store.evict(handle))
    assertEquals(0, store.size())
  }

  // TC-VC-024
  @Test
  fun thousandPutEvictCyclesLeaveStoreEmpty() {
    val store = HandleStore()
    repeat(1000) {
      val seed = ByteArray(64) { index -> (index + 7).toByte() }
      val handle = store.put(seed)
      assertTrue(store.evict(handle))
      assertArrayEquals(ByteArray(64), seed)
    }
    assertEquals("1000 import/destroy cycles must leave no entries", 0, store.size())
  }
}
