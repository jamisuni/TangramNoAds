package io.github.jamisuni.tangram.acceptance

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.MainActivity
import io.github.jamisuni.tangram.browse.R
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import java.util.Locale

/**
 * Cross-slice browsing on the real app (design WO-004 "Acceptance IDs": the `app/src/androidTest` column). Real touches, the
 * real library, the real store file. Each test starts from a wiped store (`ResetStoreRule`, DA-62).
 * The held-out slice (relaunch, restart pill, no-positions, displaced piece, long press, store writes) is NOT here.
 */
class BrowsingAppTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(ResetStoreRule()).around(compose)

    private val puzzles = PuzzleLibrary.packaged().puzzles
    private val n = puzzles.size
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun titleOf(p: Puzzle) = p.title.inLanguage(Locale.getDefault().language)

    private fun assertShows(index: Int) {
        compose.onNodeWithTag("puzzle-title").assertTextEquals(titleOf(puzzles[index]))
        compose.onNodeWithTag("puzzle-counter").assertTextEquals("${index + 1} / $n")
    }

    // REQ-003.A1 - "From any puzzle, every other puzzle can be reached without solving anything."
    // On the real app: the grid has a cell per puzzle and each cell opens its puzzle; nothing was solved.
    @Test
    fun req003_A1_theGridHasACellPerPuzzleAndEachOpensItsPuzzle() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.touch("puzzle-counter")
            for (p in puzzles) compose.onNodeWithTag("grid-cell-${p.id.value}").assertExists()
            compose.touch("grid-close")
            for ((i, p) in puzzles.withIndex().reversed()) {
                compose.touch("puzzle-counter")
                compose.touch("grid-cell-${p.id.value}")
                compose.onNodeWithTag("all-puzzles").assertDoesNotExist()
                assertShows(i)
            }
        }
    }

    // REQ-003.A2 - "Leaving and returning to a puzzle never loses its state."
    // Real touch: 3 pieces placed, ›, ‹: the same three pieces are on the board in their places and the other pieces are in the tray.
    @Test
    fun req003_A2_threePlacedPiecesSurviveLeavingAndReturning() {
        val index = puzzles.indexOfFirst { it.solution.size == 7 }
        assertTrue("fixture: a 7-piece puzzle exists", index >= 0)
        val puzzle = puzzles[index]
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(300)
            repeat(index) { compose.touch("next-button") }
            val rig = TouchRig(compose)
            val placed = rig.placePieces(puzzle, 3)

            compose.touch("next-button")
            compose.touch("prev-button")
            compose.mainClock.advanceTimeBy(600)

            val shot = rig.shot()
            val board = rig.board()
            val m = rig.mapper(shot, board, puzzle)
            for (piece in puzzle.solution.map { it.piece }) {
                if (piece in placed) {
                    assertTrue("$piece is on the board in its place", rig.pieceIsOnBoardAtSolution(shot, m, puzzle, piece))
                    assertFalse("$piece is not in the tray any more", rig.inTray(shot, board, piece))
                } else {
                    assertTrue("$piece is still in the tray", rig.inTray(shot, board, piece))
                }
            }
        }
    }

    // REQ-024.A2 - "The top bar always shows the current puzzle's name and number."
    // On the real top bar after each › and each ‹.
    @Test
    fun req024_A2_theRealTopBarShowsNameAndNumberAfterEveryMove() {
        ActivityScenario.launch(MainActivity::class.java).use {
            assertShows(0)
            for (i in 1 until n) {
                compose.touch("next-button")
                assertShows(i)
            }
            for (i in n - 2 downTo 0) {
                compose.touch("prev-button")
                assertShows(i)
            }
        }
    }

    // REQ-024.A3 - "› on the last puzzle shows the first."  On the real list.
    @Test
    fun req024_A3_theRealListWraps() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.touch("prev-button")
            assertShows(n - 1)
            compose.touch("next-button")
            assertShows(0)
        }
    }

    // REQ-026.A2 - "Retry leads to an empty silhouette with all pieces in the tray."
    // Seed: the first puzzle is Solved (with a best time). On screen: the solved bar and the picture; one touch on Retry and the
    // bar is gone, the picture is gone (the silhouette colour is not the picture base), no piece colour is on the board and
    // every piece of the puzzle is back in the tray.
    @Test
    fun req026_A2_retryShowsAnEmptySilhouetteWithAllPiecesInTheTray() {
        val puzzle = puzzles.first()
        AppStore.open().apply {
            saveProgress(puzzle.id, PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 47, 41))
            saveLastShownPuzzle(puzzle.id)
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(400)
            val rig = TouchRig(compose)
            compose.onNodeWithTag("solved-bar").assertExists()

            val solvedShot = rig.shot()
            val board = rig.board()
            val m = rig.mapper(solvedShot, board, puzzle)
            val base = PieceColours.rgbOf(puzzle.picture.base.value)
            val probe = rig.probeInside(solvedShot, m, puzzle, base) // inset in the base colour, on no picture edge (DA-70)
            assertTrue("fixture: the solved board shows the picture base at the probe", colourDiff(solvedShot.getPixel(probe.x.toInt(), probe.y.toInt()), base) <= 10)

            compose.touch("retry-button")
            compose.mainClock.advanceTimeBy(600)

            compose.onNodeWithTag("solved-bar").assertDoesNotExist()
            val shot = rig.shot()
            assertTrue("the picture is gone from the board", colourDiff(shot.getPixel(probe.x.toInt(), probe.y.toInt()), base) > 20)
            assertEquals("no piece colour on the board", emptyList<PieceId>(), rig.anyPieceColourOnBoard(shot, rig.board()))
            for (sp in puzzle.solution) assertTrue("${sp.piece} is in the tray", rig.inTray(shot, rig.board(), sp.piece))
            compose.onNodeWithTag("puzzle-state").assertTextEquals(context.getString(R.string.state_new))
            compose.onNodeWithTag("restart-button").assertDoesNotExist()
        }
    }

    // REQ-050.A2 - "Pressing the counter shows one cell per puzzle, solved ones in colour."
    // Real thumbnails: with the first puzzle New its cell has no pixel of the picture's base colour (it is flat); after the same
    // puzzle is seeded Solved, its cell has many (the picture). Another, New puzzle's cell stays flat.
    @Test
    fun req050_A2_aSolvedPuzzlesCellShowsItsPictureAndANewOneIsFlat() {
        val solved = puzzles.first()
        val other = puzzles[3]
        fun baseCount(id: String, p: Puzzle): Int {
            val bmp: Bitmap = compose.onNodeWithTag("grid-cell-$id").captureToImage().asAndroidBitmap()
            val base = PieceColours.rgbOf(p.picture.base.value)
            var count = 0
            for (y in 0 until bmp.height step 2) for (x in 0 until bmp.width step 2) if (colourDiff(bmp.getPixel(x, y), base) <= 10) count++
            return count
        }

        var newCount = -1
        var otherCount = -1
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.touch("puzzle-counter")
            compose.onNodeWithTag("grid-cell-${solved.id.value}").assertExists()
            newCount = baseCount(solved.id.value, solved)
            otherCount = baseCount(other.id.value, other)
        }
        AppStore.open().apply {
            saveProgress(solved.id, PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 47, 41))
            saveLastShownPuzzle(puzzles[1].id)
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.touch("puzzle-counter")
            val solvedCount = baseCount(solved.id.value, solved)
            assertTrue("fixture: a New cell is flat (base-colour pixels: $newCount)", newCount <= 3)
            assertTrue("another New cell is flat too ($otherCount)", otherCount <= 3)
            assertTrue("the solved cell shows the picture (base-colour pixels: $solvedCount)", solvedCount >= 100)
        }
    }

    // REQ-050 rule 3 (prose, not an acceptance claim): "closing it (or the back button) returns to the same puzzle state".
    // The back button closes the grid and the app stays open.
    @Test
    fun decisionDA55_backClosesTheGridInsteadOfLeavingTheApp() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.touch("next-button")
            compose.touch("puzzle-counter")
            compose.onNodeWithTag("all-puzzles").assertExists()
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            compose.waitForIdle()
            compose.onNodeWithTag("all-puzzles").assertDoesNotExist()
            assertTrue("the activity is still there", scenario.state.isAtLeast(Lifecycle.State.STARTED))
            assertShows(1)
        }
    }
}
