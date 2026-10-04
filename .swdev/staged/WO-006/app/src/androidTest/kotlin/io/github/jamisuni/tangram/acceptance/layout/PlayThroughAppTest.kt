package io.github.jamisuni.tangram.acceptance.layout

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.acceptance.AppStore
import io.github.jamisuni.tangram.acceptance.PieceColours
import io.github.jamisuni.tangram.acceptance.ResetStoreRule
import io.github.jamisuni.tangram.acceptance.TouchRig
import io.github.jamisuni.tangram.acceptance.colourDiff
import io.github.jamisuni.tangram.acceptance.touch
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.kernel.model.PieceId
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import kotlin.math.abs

/**
 * "Fully playable" (design WO-006 section 7, DA-107): the player's flow on 390 x 844 portrait, 1280 x 800 and 800 x 1280: open, solve the
 * first puzzle by real touch, Retry, Restart, Next, browse and the grid; on a tablet case one parallelogram flip as well (the badge's
 * place depends on the layout, DA-29). Solvability of all 13 puzzles stays with REQ-002 and REQ-046.
 */
@UsesDisplayRule
@RunWith(Parameterized::class)
class PlayThroughAppTest(private val spec: DisplaySpec) {
    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun specs(): List<DisplaySpec> = listOf(DisplaySpec.PHONE_390x844, DisplaySpec.TABLET_1280x800, DisplaySpec.TABLET_800x1280)
    }

    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(DisplayRule(spec)).around(TestConfigRule()).around(ResetStoreRule()).around(compose)

    private val puzzles = PuzzleLibrary.packaged().puzzles
    private val n = puzzles.size
    private val en = ScreenWalk.bundle(InstrumentationRegistry.getInstrumentation().targetContext, "en-US").getValue("browse")

    private fun settle(ms: Int = 600) = compose.mainClock.advanceTimeBy(ms.toLong())

    private fun assertState(key: String) = compose.onNodeWithTag("puzzle-state").assertTextEquals(en.getValue(key))

    private fun assertCounter(index: Int) = compose.onNodeWithTag("puzzle-counter").assertTextEquals("${index + 1} / $n")

    /** The sign of (top-right + bottom-left) minus (top-left + bottom-right) of the parallelogram's pixels in its cell: mirrored = opposite sign. */
    private fun pgSkew(shot: Bitmap, cell: Rect): Int {
        val cx = (cell.left + cell.right) / 2
        val cy = (cell.top + cell.bottom) / 2
        val want = PieceColours.rgb(PieceId.PG)
        var diag = 0
        var anti = 0
        for (y in cell.top.toInt().coerceAtLeast(0) until cell.bottom.toInt().coerceAtMost(shot.height) step 2) {
            for (x in cell.left.toInt().coerceAtLeast(0) until cell.right.toInt().coerceAtMost(shot.width) step 2) {
                if (colourDiff(shot.getPixel(x, y), want) <= 10) if ((x >= cx) == (y < cy)) diag++ else anti++
            }
        }
        return diag - anti
    }

    // REQ-006.A1 - "The game is fully playable on a phone in portrait and on a tablet."
    // The player's flow on the real window: the first puzzle is solved by real touch, Retry returns it empty, Restart empties a started
    // one, solving again and Next opens the second puzzle, then browse > and <, and the grid opens any puzzle. On a tablet, one flip
    // of the parallelogram mirrors its miniature.
    @Test
    fun req006_A1_theFirstPuzzleIsSolvedAndTheRestOfTheGameIsUsableOnThisWindow() {
        val first = puzzles.first()
        AppLaunch.launch("en-US").use {
            compose.mainClock.autoAdvance = false
            settle(300)
            assertCounter(0)

            TouchRig(compose).solveByTouch(first)
            settle(3000)
            compose.onNodeWithTag("solved-bar").assertExists()
            assertState("state_solved")

            compose.touch("retry-button") // Retry: back to an empty silhouette
            settle()
            compose.onNodeWithTag("solved-bar").assertDoesNotExist()
            assertState("state_new")

            TouchRig(compose).placePieces(first, 1)
            compose.onNodeWithTag("restart-button").assertExists()
            compose.touch("restart-button") // Restart: a started puzzle is empty again
            settle()
            assertState("state_new")
            compose.onNodeWithTag("restart-button").assertDoesNotExist()

            TouchRig(compose).solveByTouch(first)
            settle(3000)
            compose.onNodeWithTag("solved-bar").assertExists()
            compose.touch("solved-next-button") // Next: the second puzzle
            settle()
            assertCounter(1)

            compose.touch("next-button")
            settle()
            assertCounter(2)
            compose.touch("prev-button")
            settle()
            assertCounter(1)

            compose.touch("puzzle-counter") // the grid opens any puzzle
            compose.onNodeWithTag("all-puzzles").assertExists()
            compose.touch("grid-cell-${puzzles.last().id.value}")
            settle()
            compose.onNodeWithTag("all-puzzles").assertDoesNotExist()
            assertCounter(n - 1)
        }

        if (spec.widthDp >= 600) {
            // one parallelogram flip on a tablet, on a full-set puzzle
            val seven = puzzles.first { it.solution.size == 7 }
            AppStore.open().saveLastShownPuzzle(seven.id)
            AppLaunch.launch("en-US").use {
                compose.mainClock.autoAdvance = false
                settle(300)
                val rig = TouchRig(compose)
                val cell = compose.readTray().cells.getValue(PieceId.PG)
                val before = pgSkew(rig.shot(), cell)
                compose.touch("flip-badge")
                settle()
                val after = pgSkew(rig.shot(), cell)
                assertTrue("the flip did not mirror the parallelogram's miniature (skew $before -> $after)", abs(before) > 20 && before.toLong() * after < 0)
            }
        }
    }
}
