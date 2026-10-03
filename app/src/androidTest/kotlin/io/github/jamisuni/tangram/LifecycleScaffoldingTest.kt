package io.github.jamisuni.tangram

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import io.github.jamisuni.tangram.acceptance.AppStore
import io.github.jamisuni.tangram.acceptance.ResetStoreRule
import io.github.jamisuni.tangram.acceptance.TouchRig
import io.github.jamisuni.tangram.acceptance.touch
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

// SCAFFOLDING (disposable): code review WO-004 F3, the lifecycle wiring of MainActivity. Pins DA-49 / decisions F4/F5 and
// ViewModel survival (DA-26 ended); it carries no REQ token.
class LifecycleScaffoldingTest {
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(ResetStoreRule()).around(compose)

    private val puzzle = PuzzleLibrary.packaged().puzzles.first()

    @Test
    fun pauseSavesTheTouchPlacedPiece() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(300)
            val placed = TouchRig(compose).placePieces(puzzle, 1)
            scenario.moveToState(Lifecycle.State.CREATED) // onPause then onStop
            val saved = AppStore.open().progress(puzzle.id)
            assertEquals(PuzzleState.IN_PROGRESS, saved.state)
            assertTrue("the placed piece is stored on the board", saved.pieces[placed.single()] is PieceSave.OnBoard)
        }
    }

    @Test
    fun recreateKeepsThePuzzleThePlacedPieceAndTheOpenGrid() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(300)
            val rig = TouchRig(compose)
            val placed = rig.placePieces(puzzle, 1).single()
            compose.touch("puzzle-counter") // open the grid
            compose.onNodeWithTag("all-puzzles").assertExists()

            scenario.recreate()
            compose.mainClock.advanceTimeBy(600)

            compose.onNodeWithTag("all-puzzles").assertExists()
            compose.touch("grid-close")
            compose.mainClock.advanceTimeBy(300)
            compose.onNodeWithTag("puzzle-title").assertTextEquals(puzzle.title.inLanguage(java.util.Locale.getDefault().language))
            val shot = rig.shot()
            assertTrue("the placed piece is still on the board", rig.boardCount(shot, rig.board(), placed) > 3)
        }
    }

    // O1 (code-review spot-check): the hidden Restart pill must take no touch. In Solved a tap reaching it would call
    // restart() = Retry and reset the puzzle. The slot rectangle is read from the pill while In progress.
    @Test
    fun aTapOnTheHiddenRestartSlotInSolvedLeavesThePuzzleSolved() {
        var centre: Offset? = null
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(300)
            TouchRig(compose).placePieces(puzzle, 1) // In progress: the pill is placed in its slot
            val area = compose.onNodeWithTag("play-area").fetchSemanticsNode().boundsInRoot
            val pill = compose.onNodeWithTag("restart-button").fetchSemanticsNode().boundsInRoot
            centre = Offset(pill.center.x - area.left, pill.center.y - area.top)
        }
        AppStore.open().saveProgress(puzzle.id, PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 47, 41))
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(400)
            compose.onNodeWithTag("solved-bar").assertExists()
            val at = centre ?: error("no slot rectangle")
            compose.onNodeWithTag("play-area").performTouchInput { down(at) }
            compose.mainClock.advanceTimeBy(60)
            compose.onNodeWithTag("play-area").performTouchInput { up() }
            compose.mainClock.advanceTimeBy(600)

            compose.onNodeWithTag("solved-bar").assertExists()
            compose.onNodeWithTag("puzzle-state").assertTextEquals(
                InstrumentationRegistry.getInstrumentation().targetContext.getString(io.github.jamisuni.tangram.browse.R.string.state_solved),
            )
            assertEquals(PuzzleState.SOLVED, AppStore.open().progress(puzzle.id).state)
        }
    }
}
