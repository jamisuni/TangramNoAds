package io.github.jamisuni.tangram.devtools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.github.jamisuni.tangram.content.PuzzleLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// ACCEPTANCE TEST (TASK-T5, independent author): decision tests for the DEV pill's drag guard (DA-87, decisions F5, O-09).
// Frozen seam: `DevCornerButton(state, puzzle, solveNow, blocked, modifier)`; "a click while `blocked()` is true is a no-op: no
// dialog, no state change"; tags `dev-button`, `dev-dialog`. No acceptance tokens: these pin AI decisions.
class DevButtonBlockedTest {

    @get:Rule
    val compose = createComposeRule()

    private val puzzle = PuzzleLibrary.packaged().puzzles.first()
    private var blocked = true
    private val state = DevToolsState()

    private fun show() {
        compose.setContent {
            Box(Modifier.size(200.dp, 120.dp)) {
                DevCornerButton(state, puzzle, { _ -> error("solveNow must not be called by this test") }, { blocked })
            }
        }
        compose.waitForIdle()
    }

    // decision DA-87: while blocked() is true a click on the pill opens no dialog and changes no state.
    @Test
    fun decisionDA87_aBlockedClickIsANoOp() {
        show()
        repeat(3) { compose.onNodeWithTag("dev-button").performClick() }
        compose.waitForIdle()
        compose.onNodeWithTag("dev-dialog").assertDoesNotExist()
        assertFalse("dialogOpen", state.dialogOpen)
        assertFalse("unlocked", state.unlocked)
        assertFalse("overlayOn", state.overlayOn)
        assertEquals(DevNotice.NONE, state.notice)
    }

    // decision DA-87: the guard is read at click time. The same pill opens its dialog once blocked() turns false, is blocked
    // again when it turns true while the dialog is closed, and a blocked click never leaves anything pending that opens later.
    @Test
    fun decisionDA87_theGuardIsReadAtEveryClick() {
        show()
        compose.onNodeWithTag("dev-button").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("dev-dialog").assertDoesNotExist()

        blocked = false
        compose.onNodeWithTag("dev-button").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("dev-dialog").assertExists()
        assertTrue("dialogOpen after an unblocked click", state.dialogOpen)

        compose.runOnUiThread { state.close() }
        compose.waitForIdle()
        compose.onNodeWithTag("dev-dialog").assertDoesNotExist()

        blocked = true
        compose.onNodeWithTag("dev-button").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("dev-dialog").assertDoesNotExist()
        assertFalse(state.dialogOpen)
    }
}
