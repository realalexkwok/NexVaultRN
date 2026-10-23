package expo.modules.securityandroid

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM gate for the fail-closed counter (TC-SA-014, non-prompt half). */
class PromptFailureGateTest {
  @Test
  fun thirdConsecutiveFailureFailsClosed() {
    val gate = PromptFailureGate(3)
    assertFalse(gate.onFailure())
    assertFalse(gate.onFailure())
    assertTrue("the third failure must fail closed", gate.onFailure())
    assertTrue("the gate stays closed afterwards", gate.onFailure())
  }

  @Test
  fun aFreshGateStartsOpen() {
    assertFalse(PromptFailureGate(3).onFailure())
  }
}
