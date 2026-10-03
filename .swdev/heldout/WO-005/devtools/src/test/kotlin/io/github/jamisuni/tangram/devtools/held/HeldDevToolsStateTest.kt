package io.github.jamisuni.tangram.devtools.held

import io.github.jamisuni.tangram.devtools.DevNotice
import io.github.jamisuni.tangram.devtools.DevToolsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HELD-OUT (Test & Verify only): REQ-046.A1 at state level, from REQ-046 and the frozen seam
 * `DevToolsState` ("`submit(code)` is true iff `code == "0417"`; a wrong code sets `notice = WRONG_PASSCODE` and nothing else;
 * a right code sets `unlocked = true`, `notice = NONE`"). Self-contained.
 */
class HeldDevToolsStateTest {

    private fun assertLockedAndRevealingNothing(s: DevToolsState, label: String) {
        assertFalse("$label: unlocked", s.unlocked)
        assertFalse("$label: overlayOn", s.overlayOn)
    }

    // REQ-046.A1 - "Entering 1234 is refused; entering 0417 unlocks the aid."
    // 1234 is refused with the wrong-passcode notice and nothing else changes; 0417 unlocks and clears the notice.
    @Test
    fun req046_A1_1234IsRefusedAnd0417Unlocks() {
        val s = DevToolsState()
        assertLockedAndRevealingNothing(s, "a new state")
        assertEquals(DevNotice.NONE, s.notice)

        assertFalse(s.submit("1234"))
        assertEquals(DevNotice.WRONG_PASSCODE, s.notice)
        assertLockedAndRevealingNothing(s, "after 1234")

        assertTrue(s.submit("0417"))
        assertTrue("unlocked after 0417", s.unlocked)
        assertEquals(DevNotice.NONE, s.notice)
        assertFalse("unlocking alone shows no solution", s.overlayOn)
    }

    // REQ-046.A1 - every other input is refused too, exactly 0417 is the passcode (no prefix, no suffix, no padding, no trim).
    @Test
    fun req046_A1_onlyExactly0417Unlocks() {
        for (wrong in listOf("", "0", "041", "417", "04170", "10417", "0417 ", " 0417", "0418", "4170", "0000", "1234", "O417", "0417\n")) {
            val s = DevToolsState()
            assertFalse("'$wrong' was accepted", s.submit(wrong))
            assertEquals("'$wrong'", DevNotice.WRONG_PASSCODE, s.notice)
            assertLockedAndRevealingNothing(s, "'$wrong'")
        }
    }

    // REQ-046.A1 - a refusal does not spoil the next right try, and a second wrong try keeps the aid locked.
    @Test
    fun req046_A1_aWrongTryBeforeTheRightOneChangesNothing() {
        val s = DevToolsState()
        assertFalse(s.submit("1234"))
        assertFalse(s.submit("1235"))
        assertLockedAndRevealingNothing(s, "after two wrong tries")
        assertTrue(s.submit("0417"))
        assertTrue(s.unlocked)
        assertEquals(DevNotice.NONE, s.notice)
    }

    // REQ-046.A1 + rule 2 ("until the page or app is restarted"): the unlock belongs to one state object. A new state - what a
    // restarted app builds - is locked again, whatever another state did; no global flag survives.
    @Test
    fun req046_A1_anotherStateStartsLockedSoTheUnlockIsNotGlobal() {
        val first = DevToolsState()
        assertTrue(first.submit("0417"))
        first.toggleOverlay()
        val second = DevToolsState()
        assertLockedAndRevealingNothing(second, "a second state after the first unlocked")
        assertEquals(DevNotice.NONE, second.notice)
        assertTrue("the first state is unaffected", first.unlocked)
    }
}
