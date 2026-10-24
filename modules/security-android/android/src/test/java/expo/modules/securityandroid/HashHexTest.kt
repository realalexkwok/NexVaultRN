package expo.modules.securityandroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** TC-T32-005: hashing + expected-value semantics shared by the signature/bundle self-checks. */
class HashHexTest {
  @Test
  fun sha256MatchesTheKnownVector() {
    assertEquals(
      "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
      HashHex.sha256Hex("abc".toByteArray(Charsets.UTF_8))
    )
  }

  @Test
  fun hexNormalizationStripsSeparatorsAndCase() {
    assertEquals("abcdef", HashHex.normalizeHex(" AB:CD:EF "))
    assertEquals("00ff", HashHex.normalizeHex("00ff"))
  }

  @Test
  fun missingOrPlaceholderExpectedIsNotEvaluated() {
    assertNull(HashHex.expectedOrNull(null))
    assertNull(HashHex.expectedOrNull(""))
    assertNull(HashHex.expectedOrNull("NOT_EVALUATED"))
    assertNull(HashHex.expectedOrNull("abc"))
    assertNull(HashHex.expectedOrNull("z".repeat(64)))
    assertEquals("a".repeat(64), HashHex.expectedOrNull("A".repeat(64)))
  }

  @Test
  fun matchSemanticsFailClosedOnUnreadableActual() {
    val expected = "a".repeat(64)
    assertNull(HashHex.matches(null, expected))
    assertEquals(true, HashHex.matches(expected, "A".repeat(64)))
    assertEquals(false, HashHex.matches(expected, "b".repeat(64)))
    assertEquals(false, HashHex.matches(expected, null))
  }
}
