package io.github.jamisuni.tangram.devtools.held

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.devtools.DevCornerButton
import io.github.jamisuni.tangram.devtools.DevSolution
import io.github.jamisuni.tangram.devtools.DevToolsState
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * HELD-OUT (Test & Verify only): the dialog of the DEV pill in the `devtools` module, REQ-046.A1 (wrong code refused, 0417
 * unlocks) and the dialog half of REQ-046.A3 ("Solve this puzzle now" hands the stored solution to the game's solve hook).
 * Written from REQ-046 and the frozen seams: `DevCornerButton(state, puzzle, solveNow, blocked, modifier)`, the tags `dev-button`,
 * `dev-dialog`, `dev-passcode`, `dev-ok`, `dev-notice`, `dev-show-solution`, `dev-solve-now`, `dev-done`, the notice strings
 * `devtools_wrong_passcode` / `devtools_solve_failed` ("its test reads the resource, not a literal"). Self-contained.
 */
class HeldDevDialogDeviceTest {

    @get:Rule
    val compose = createComposeRule()

    private val puzzle = PuzzleLibrary.packaged().puzzles.first { it.solution.size == 7 }
    private val state = DevToolsState()
    private val calls = ArrayList<List<PlacedPiece>>()
    private var solveResult = true

    private fun show() {
        compose.setContent {
            Box(Modifier.size(200.dp, 120.dp)) {
                DevCornerButton(state, puzzle, { poses -> calls.add(poses); solveResult }, { false })
            }
        }
        compose.waitForIdle()
    }

    private fun string(name: String): String {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val id = ctx.resources.getIdentifier(name, "string", ctx.packageName)
        if (id == 0) error("no string resource $name in ${ctx.packageName}")
        return ctx.getString(id)
    }

    private fun open() {
        compose.onNodeWithTag("dev-button").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("dev-dialog").assertExists()
    }

    private fun enter(code: String) {
        compose.onNodeWithTag("dev-passcode").performTextInput(code)
        compose.onNodeWithTag("dev-ok").performClick()
        compose.waitForIdle()
    }

    private fun assertNoTools() {
        compose.onNodeWithTag("dev-show-solution").assertDoesNotExist()
        compose.onNodeWithTag("dev-solve-now").assertDoesNotExist()
    }

    // REQ-046.A1 - "Entering 1234 is refused; entering 0417 unlocks the aid."
    // The pill opens the dialog; 1234 shows the wrong-passcode text (the resource, and the REQ's English text) and no tool exists;
    // the state stays locked; 0417 offers both tools and the refusal is gone.
    @Test
    fun req046_A1_theDialogRefuses1234AndUnlocksWith0417() {
        show()
        compose.onNodeWithTag("dev-dialog").assertDoesNotExist()
        open()
        assertNoTools()

        enter("1234")
        compose.onNodeWithTag("dev-notice").assertTextEquals(string("devtools_wrong_passcode"))
        if (Locale.getDefault().language == "en") compose.onNodeWithTag("dev-notice").assertTextEquals("Wrong passcode.")
        assertNoTools()
        assertFalse(state.unlocked)
        assertFalse(state.overlayOn)

        enter("0417")
        compose.onNodeWithTag("dev-show-solution").assertExists()
        compose.onNodeWithTag("dev-solve-now").assertExists()
        compose.onNodeWithTag("dev-notice").assertDoesNotExist()
        assertTrue(state.unlocked)
        assertTrue("nothing was solved by merely unlocking", calls.isEmpty())
    }

    // REQ-046.A3 - "'Solve this puzzle now' shows the solved picture and leaves the best time empty."  (dialog half)
    // Once unlocked, "Solve this puzzle now" hands the game the puzzle's whole stored solution, exactly once, and the dialog closes.
    // The dialog itself touches no store or time: its only effect is the hand-over.
    @Test
    fun req046_A3_solveNowHandsOverTheStoredSolutionOnceAndClosesTheDialog() {
        show()
        open()
        enter("0417")
        compose.onNodeWithTag("dev-solve-now").performClick()
        compose.waitForIdle()
        assertEquals("solveNow calls", 1, calls.size)
        fun keys(l: List<PlacedPiece>) = l.map { "${it.piece}@${it.at}/${it.turn.steps}/${it.mirrored}" }
        assertEquals(keys(DevSolution.poses(puzzle) ?: error("fixture: no poses")), keys(calls.single()))
        assertEquals("one pose per piece of the puzzle", puzzle.solution.map { it.piece }, calls.single().map { it.piece })
        compose.onNodeWithTag("dev-dialog").assertDoesNotExist()
    }

    // REQ-046.A3 - when the game cannot place the stored solution (the hook says no) nothing is claimed solved: the dialog stays open
    // and says so (design DA-80, the notice `devtools_solve_failed`); a later try after the game accepts works once.
    @Test
    fun req046_A3_aRefusedSolveKeepsTheDialogOpenWithTheFailureNotice() {
        solveResult = false
        show()
        open()
        enter("0417")
        compose.onNodeWithTag("dev-solve-now").performClick()
        compose.waitForIdle()
        assertEquals(1, calls.size)
        compose.onNodeWithTag("dev-dialog").assertExists()
        compose.onNodeWithTag("dev-notice").assertTextEquals(string("devtools_solve_failed"))

        solveResult = true
        compose.onNodeWithTag("dev-solve-now").performClick()
        compose.waitForIdle()
        assertEquals(2, calls.size)
        compose.onNodeWithTag("dev-dialog").assertDoesNotExist()
    }
}
