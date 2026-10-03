package io.github.jamisuni.tangram.acceptance.held

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import io.github.jamisuni.tangram.MainActivity
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import java.util.Locale

/**
 * HELD-OUT (Test & Verify only): REQ-026 A1, REQ-050 A1 and A3 on the real app. Self-contained (own rule and touch kit).
 * No scenario contradicts a logged decision: a solved puzzle takes no touches (DA-35) and stores no positions (DA-48/65), so the
 * Solved state is SEEDED, with stale piece positions as old data might hold them, and only the bar's buttons and the top bar are touched.
 */
class HeldSolvedFindAppTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(ResetStoreRule()).around(compose)

    private val puzzles = PuzzleLibrary.packaged().puzzles
    private val n = puzzles.size
    private val seven = puzzles.indexOfFirst { it.solution.size == 7 }
    private fun titleOf(p: Puzzle) = p.title.inLanguage(Locale.getDefault().language)
    private val solvedEntry = PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 47, 41)

    private fun assertShows(index: Int) {
        compose.onNodeWithTag("puzzle-title").assertTextEquals(titleOf(puzzles[index]))
        compose.onNodeWithTag("puzzle-counter").assertTextEquals("${index + 1} / $n")
    }

    private fun save(puzzle: Puzzle, piece: PieceId): PieceSave.OnBoard {
        val sp = puzzle.solution.first { it.piece == piece }
        val pose = PieceGeometry.poseOf(sp.piece, sp.polygon) ?: error("no pose for $piece")
        return PieceSave.OnBoard(pose.at, pose.turn, pose.mirrored)
    }

    /** Every colour the picture paints, as 0xRRGGBB. */
    private fun pictureColours(p: Puzzle): List<Int> = buildList {
        add(p.picture.base.value)
        for (s in p.picture.shapes) {
            s.style.fill?.let { add(it.value) }
            s.style.stroke?.let { add(it.value) }
        }
    }

    private fun far(a: Int, b: Int): Int = maxOf(
        kotlin.math.abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)),
        kotlin.math.abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)),
        kotlin.math.abs((a and 0xFF) - (b and 0xFF)),
    )

    /** A puzzle whose picture paints no colour close to any piece colour, so "no piece colour on the board" is decisive. */
    private fun puzzleWithPieceFreePicture(): Int {
        val pieceColours = PieceId.entries.map { PieceColours.rgb(it) and 0xFFFFFF }
        return puzzles.indices.firstOrNull { i ->
            pictureColours(puzzles[i]).all { c -> pieceColours.all { far(c, it) > 30 } }
        } ?: error("fixture: every picture paints a colour close to a piece colour")
    }

    // REQ-026.A1 - "A solved puzzle never shows where the pieces were."
    // A puzzle is stored Solved together with stale piece positions (old data). On screen: the picture (its base colour) fills the
    // board, no piece colour is anywhere on the board, and it stays so after leaving and returning.
    @Test
    fun req026_A1_aSolvedPuzzleShowsNoPiecePositions() {
        val i = puzzleWithPieceFreePicture()
        val p = puzzles[i]
        val stale = p.solution.associate { it.piece to save(p, it.piece) }
        AppStore.open().apply {
            saveProgress(p.id, PuzzleProgress(PuzzleState.SOLVED, stale, 47, 41))
            saveLastShownPuzzle(p.id)
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(400)
            val rig = TouchRig(compose)
            compose.onNodeWithTag("solved-bar").assertExists()
            assertNoPositions(rig, p)

            compose.touch("next-button")
            compose.touch("prev-button")
            compose.mainClock.advanceTimeBy(600)
            assertShows(i)
            compose.onNodeWithTag("solved-bar").assertExists()
            assertNoPositions(rig, p)
        }
    }

    private fun assertNoPositions(rig: TouchRig, p: Puzzle) {
        val shot = rig.shot()
        val board = rig.board()
        assertEquals("no piece colour on the board of ${p.id.value}", emptyList<PieceId>(), rig.anyPieceColourOnBoard(shot, board))
        var baseCount = 0
        val base = PieceColours.rgbOf(p.picture.base.value)
        for (y in board.y0 until board.y1 step 2) for (x in board.x0 until board.x1 step 2) if (colourDiff(shot.getPixel(x, y), base) <= 10) baseCount++
        assertTrue("fixture: the picture's base colour is on the board ($baseCount)", baseCount >= 100)
    }

    private fun seedAllSolvedExcept(vararg unsolved: Int, shown: Int) {
        AppStore.open().apply {
            for ((i, p) in puzzles.withIndex()) if (i !in unsolved) saveProgress(p.id, solvedEntry)
            saveLastShownPuzzle(puzzles[shown].id)
        }
    }

    // REQ-050.A1 - "With puzzles 3 and 7 unsolved and puzzle 3 open, a long press on › opens puzzle 7."
    // On the real app with a seeded store: down, advance the test clock past 500 ms, up.
    @Test
    fun req050_A1_aLongPressOnNextOpensTheNextUnsolvedPuzzle() {
        seedAllSolvedExcept(2, 6, shown = 2)
        ActivityScenario.launch(MainActivity::class.java).use {
            assertShows(2)
            compose.hold("next-button", 650)
            assertShows(6)
        }
    }

    // REQ-050.A1 - the same press held for less than 500 ms is a normal next.
    @Test
    fun req050_A1_aShortPressOnNextIsANormalNext() {
        seedAllSolvedExcept(2, 6, shown = 2)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.hold("next-button", 120)
            assertShows(3)
        }
    }

    // REQ-050.A3 - "Choosing a cell opens that puzzle without changing any other puzzle's state."
    // Real store file: before the launch it holds Solved, New-with-a-turned-tray-piece, In-progress-with-three-pieces and Solved
    // entries. The player opens the grid and chooses puzzle 2 (index 1). After closing the app, the file holds exactly the same
    // progress for every puzzle, and `lastShown` is the chosen puzzle.
    @Test
    fun req050_A3_choosingACellLeavesEveryPuzzlesStoredStateAsItWas() {
        check(seven > 1 && seven + 1 < n) { "fixture: the library layout changed" }
        val seven7 = puzzles[seven]
        val three = seven7.solution.map { it.piece }.take(3).associateWith { save(seven7, it) }
        val own = puzzles[1].solution.first().piece
        val ids = listOf(0, 1, seven, seven + 1)
        AppStore.open().apply {
            saveProgress(puzzles[0].id, solvedEntry)
            saveProgress(puzzles[1].id, PuzzleProgress(PuzzleState.NEW, mapOf(own to PieceSave.InTray(Turn(3), true)), 0, null))
            saveProgress(seven7.id, PuzzleProgress(PuzzleState.IN_PROGRESS, three, 12, 9))
            saveProgress(puzzles[seven + 1].id, solvedEntry)
            saveLastShownPuzzle(seven7.id)
        }
        val before = ids.associate { puzzles[it].id to AppStore.open().progress(puzzles[it].id) }

        ActivityScenario.launch(MainActivity::class.java).use {
            assertShows(seven)
            compose.touch("puzzle-counter")
            compose.touch("grid-cell-${puzzles[1].id.value}")
            assertShows(1)
        }

        val reopened = AppStore.open()
        for ((id, progress) in before) assertEquals("stored progress of ${id.value} changed", progress, reopened.progress(id))
        assertEquals(puzzles[1].id, reopened.lastShownPuzzle())
    }
}
