package io.github.jamisuni.tangram.devtools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// SCAFFOLDING (decision DA-76): DevToolsState transitions.
class DevToolsStateTest {

    @Test
    fun wrongPasscodeIsRefused() {
        val state = DevToolsState()
        assertFalse(state.submit("1234"))
        assertEquals(DevNotice.WRONG_PASSCODE, state.notice)
        assertFalse(state.unlocked)
        assertFalse(state.overlayOn)
    }

    @Test
    fun rightPasscodeUnlocks() {
        val state = DevToolsState()
        assertTrue(state.submit("0417"))
        assertTrue(state.unlocked)
        assertEquals(DevNotice.NONE, state.notice)
    }

    @Test
    fun unlockSurvivesCloseAndOpen() {
        val state = DevToolsState()
        state.submit("0417")
        state.open()
        state.close()
        state.open()
        assertTrue(state.unlocked)
    }

    @Test
    fun openClearsNotice() {
        val state = DevToolsState()
        state.submit("1234")
        state.close()
        state.open()
        assertEquals(DevNotice.NONE, state.notice)
        assertTrue(state.dialogOpen)
    }

    @Test
    fun toggleOverlayFlips() {
        val state = DevToolsState()
        state.toggleOverlay()
        assertTrue(state.overlayOn)
        state.toggleOverlay()
        assertFalse(state.overlayOn)
    }

    @Test
    fun solveFailedSetsNotice() {
        val state = DevToolsState()
        state.solveFailed()
        assertEquals(DevNotice.SOLVE_FAILED, state.notice)
    }
}
