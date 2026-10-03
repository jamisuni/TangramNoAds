package io.github.jamisuni.tangram.devtools

// SCAFFOLDING (TASK-033): disposable device checks of the DEV pill, dialog and overlay. Not acceptance tests.
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import io.github.jamisuni.tangram.contracts.puzzle.Picture
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleCategory
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleKind
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleTitle
import io.github.jamisuni.tangram.contracts.puzzle.Rgb
import io.github.jamisuni.tangram.contracts.puzzle.SolutionPiece
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.Q2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DevUiScaffoldingTest {
    @get:Rule
    val rule = createComposeRule()

    private fun p(x: Long, y: Long) = ExactPoint(Q2.of(x), Q2.of(y))

    private val puzzle = Puzzle(
        id = PuzzleId("scaffold"),
        title = PuzzleTitle("Scaffold", "Teline"),
        category = PuzzleCategory.SHAPES,
        rating = 1,
        kind = PuzzleKind.MINI,
        solution = listOf(SolutionPiece(PieceId.SQ, listOf(p(0, 0), p(1, 0), p(1, 1), p(0, 1)))),
        picture = Picture(Rgb(0), emptyList()),
        reviewedByHuman = false,
    )

    @Test
    fun pillIsFixedSizeAndBlockedClickIsANoOp() {
        val state = DevToolsState()
        var blocked = true
        rule.setContent { DevCornerButton(state, puzzle, { true }, { blocked }, Modifier) }
        rule.onNodeWithTag("dev-button").assertWidthIsEqualTo(56.dp).assertHeightIsEqualTo(40.dp)
        rule.onNodeWithTag("dev-button").performClick()
        assertFalse(state.dialogOpen)
        blocked = false
        rule.onNodeWithTag("dev-button").performClick()
        assertTrue(state.dialogOpen)
    }

    @Test
    fun wrongCodeRefusedRightCodeUnlocks() {
        val state = DevToolsState()
        rule.setContent { DevCornerButton(state, puzzle, { true }, { false }) }
        rule.onNodeWithTag("dev-button").performClick()
        rule.onNodeWithTag("dev-passcode").performTextInput("1234")
        rule.onNodeWithTag("dev-ok").performClick()
        rule.onNodeWithTag("dev-notice").assertExists()
        rule.onNodeWithTag("dev-show-solution").assertDoesNotExist()
        rule.onNodeWithTag("dev-passcode").performTextInput("0417")
        rule.onNodeWithTag("dev-ok").performClick()
        rule.onNodeWithTag("dev-show-solution").assertExists()
        rule.onNodeWithTag("dev-solve-now").assertExists()
    }

    @Test
    fun solveNowFailureShowsNotice() {
        val state = DevToolsState()
        state.submit("0417")
        rule.setContent { DevCornerButton(state, puzzle, { false }, { false }) }
        rule.onNodeWithTag("dev-button").performClick()
        rule.onNodeWithTag("dev-solve-now").performClick()
        rule.onNodeWithTag("dev-notice").assertExists()
        assertEquals(DevNotice.SOLVE_FAILED, state.notice)
    }

    @Test
    fun overlayHasOneNodePerPieceOnlyWhileOn() {
        val state = DevToolsState()
        rule.setContent { DevSolutionOverlay(state, puzzle, { Vec2(it.x * 100, it.y * 100) }) }
        rule.onNodeWithTag("dev-solution-overlay").assertDoesNotExist()
        state.toggleOverlay()
        rule.waitForIdle()
        rule.onNodeWithTag("dev-solution-SQ").assertExists()
    }
}
