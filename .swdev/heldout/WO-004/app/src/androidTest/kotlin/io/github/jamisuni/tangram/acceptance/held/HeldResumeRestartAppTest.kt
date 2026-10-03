package io.github.jamisuni.tangram.acceptance.held

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.MainActivity
import io.github.jamisuni.tangram.browse.R
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Rational
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import java.util.Locale

/**
 * HELD-OUT (Test & Verify only): REQ-025 A1 (relaunch round trip), A2 (Restart pill) and A3 (displaced piece) on the real app.
 * Self-contained: own rule, own touch kit (package `...acceptance.held`). Touch injection only; the store is wiped first.
 */
class HeldResumeRestartAppTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(ResetStoreRule()).around(compose)

    private val puzzles = PuzzleLibrary.packaged().puzzles
    private val index = puzzles.indexOfFirst { it.solution.size == 7 }
    private val puzzle: Puzzle get() = puzzles[index]
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun titleOf(p: Puzzle) = p.title.inLanguage(Locale.getDefault().language)

    private fun save(piece: PieceId): PieceSave.OnBoard {
        val sp = puzzle.solution.first { it.piece == piece }
        val pose = PieceGeometry.poseOf(sp.piece, sp.polygon) ?: error("no pose for $piece")
        return PieceSave.OnBoard(pose.at, pose.turn, pose.mirrored)
    }

    private fun startClockPaused() {
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(400)
    }

    private fun assertBoardAndTray(rig: TouchRig, onBoard: Collection<PieceId>) {
        val shot = rig.shot()
        val board = rig.board()
        val m = rig.mapper(shot, board, puzzle)
        for (piece in puzzle.solution.map { it.piece }) {
            if (piece in onBoard) {
                assertTrue("$piece is on the board in its place", rig.pieceIsOnBoardAtSolution(shot, m, puzzle, piece))
                assertTrue("$piece is not in the tray", !rig.inTray(shot, board, piece))
            } else {
                assertTrue("$piece is in the tray", rig.inTray(shot, board, piece))
            }
        }
    }

    // REQ-025.A1 - "Leave a puzzle with three pieces placed, come back: the same three pieces are in the same places."
    // Real touch: three pieces locked; the activity is closed; a fresh launch (a new ViewModel reading the file) opens the same
    // puzzle with the same three pieces in the same places and the other pieces in the tray.
    @Test
    fun req025_A1_threePlacedPiecesAreInTheSamePlacesAfterClosingAndRelaunching() {
        assertTrue("fixture: a 7-piece puzzle", index >= 0)
        var placed: List<PieceId> = emptyList()
        ActivityScenario.launch(MainActivity::class.java).use {
            startClockPaused()
            repeat(index) { compose.touch("next-button") }
            compose.onNodeWithTag("puzzle-title").assertTextEquals(titleOf(puzzle))
            placed = TouchRig(compose).placePieces(puzzle, 3)
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            startClockPaused()
            compose.onNodeWithTag("puzzle-title").assertTextEquals(titleOf(puzzle))
            assertBoardAndTray(TouchRig(compose), placed)
        }
    }

    // REQ-025.A2 - "After Restart, all the puzzle's pieces are in the tray and the puzzle time is zero."
    // Seeded: three pieces on the board, 77 s on the clock, best 40 s. One touch on the Restart pill: every piece is back in the
    // tray, no piece colour is on the board, the pill is gone (the puzzle is New), and a relaunch shows the same empty puzzle.
    // The stored times follow the design (puzzle time 0, best kept).
    @Test
    fun req025_A2_restartReturnsEveryPieceToTheTrayAndZeroesTheTime() {
        val three = puzzle.solution.map { it.piece }.take(3)
        AppStore.open().apply {
            saveProgress(puzzle.id, PuzzleProgress(PuzzleState.IN_PROGRESS, three.associateWith { save(it) }, 77, 40))
            saveLastShownPuzzle(puzzle.id)
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            startClockPaused()
            val rig = TouchRig(compose)
            assertBoardAndTray(rig, three)
            compose.onNodeWithTag("restart-button").assertExists()
            compose.onNodeWithTag("puzzle-state").assertTextEquals(context.getString(R.string.state_in_progress))

            compose.touch("restart-button")
            compose.mainClock.advanceTimeBy(600)

            assertBoardAndTray(rig, emptyList())
            assertEquals(emptyList<PieceId>(), rig.anyPieceColourOnBoard(rig.shot(), rig.board()))
            compose.onNodeWithTag("restart-button").assertDoesNotExist()
            compose.onNodeWithTag("puzzle-state").assertTextEquals(context.getString(R.string.state_new))
        }
        val stored = AppStore.open().progress(puzzle.id)
        assertEquals(PuzzleState.NEW, stored.state)
        assertEquals(emptyMap<PieceId, PieceSave>(), stored.pieces)
        assertEquals(0L, stored.puzzleSeconds)
        assertEquals(40L, stored.bestSeconds)
        ActivityScenario.launch(MainActivity::class.java).use {
            startClockPaused()
            assertBoardAndTray(TouchRig(compose), emptyList())
        }
    }

    // REQ-025.A3 - "A saved piece that overlaps the current silhouette's edge is in the tray after loading; the others stay."
    // Seeded before launch: the full solution, but ST2 (the last in tray order, the only free hole) moved by one unit. On screen
    // ST2 is in the tray and the six other pieces are on the board in their places.
    @Test
    fun req025_A3_aStoredPieceThatNoLongerFitsIsInTheTrayAndTheOthersStay() {
        val ids = puzzle.solution.map { it.piece }
        assertTrue("fixture: ST2 is in the puzzle", PieceId.ST2 in ids)
        val pieces = ids.associateWith<PieceId, PieceSave> { save(it) }.toMutableMap()
        val good = save(PieceId.ST2)
        val y = good.at.y.a
        // one unit down: the free hole is exactly ST2's own shape, so a translated copy cannot fit
        pieces[PieceId.ST2] = PieceSave.OnBoard(
            ExactPoint(good.at.x, Q2(Rational.of(y.numerator + y.denominator, y.denominator), good.at.y.b)),
            good.turn,
            good.mirrored,
        )
        AppStore.open().apply {
            saveProgress(puzzle.id, PuzzleProgress(PuzzleState.IN_PROGRESS, pieces, 12, null))
            saveLastShownPuzzle(puzzle.id)
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            startClockPaused()
            compose.onNodeWithTag("puzzle-title").assertTextEquals(titleOf(puzzle))
            assertBoardAndTray(TouchRig(compose), ids.filter { it != PieceId.ST2 })
        }
    }
}
