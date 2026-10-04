package io.github.jamisuni.tangram.acceptance.held

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.acceptance.layout.UsesDisplayRule
import io.github.jamisuni.tangram.kernel.model.PieceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import kotlin.math.abs

/**
 * HELD-OUT (Test & Verify only): rotating a tablet mid-puzzle (design WO-006 section 3). Own kit copies (package `...acceptance.held`);
 * the marker is the visible one by FQN. On the 1920 x 1200 display (landscape) a 90 degree turn gives 800 x 1280 dp.
 */
@UsesDisplayRule
@RunWith(Parameterized::class)
class HeldRotationAppTest(private val spec: DisplaySpec) {
    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun specs(): List<DisplaySpec> = listOf(DisplaySpec.TABLET_1280x800)
    }

    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(DisplayRule(spec)).around(TestConfigRule()).around(ResetStoreRule()).around(compose)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val en = ScreenWalk.bundle(context, "en-US").getValue("browse")

    // REQ-036.A2 - "Rotating mid-puzzle keeps every placed piece."
    // Real touch places three pieces on their solution places of a full-set puzzle; the display is then really rotated by 90 degrees. The
    // window is 800 x 1280 dp, the puzzle is still in progress, each placed piece has its colour at its solution centroid mapped through
    // the NEW board transform (a centroid, never a corner), and every other piece is still in the tray. The layout did change.
    @Test
    fun req036_A2_rotatingMidPuzzleKeepsEveryPlacedPiece() {
        val puzzle = Seed.fullPuzzle()
        Seed.screen(WalkScreen.NEW, puzzle)
        val rot = Rotation()
        AppLaunch.launch("en-US").use {
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(300)
            val rig = TouchRig(compose)
            val placed = rig.placePieces(puzzle, 3)
            compose.mainClock.advanceTimeBy(600)

            val shotBefore = rig.shot()
            val boardBefore = rig.board()
            val mBefore = rig.mapper(shotBefore, boardBefore, puzzle)
            for (p in placed) assertTrue("fixture: $p is on the board before the turn", rig.pieceIsOnBoardAtSolution(shotBefore, mBefore, puzzle, p))
            val areaBefore = compose.onNodeWithTag("play-area").fetchSemanticsNode().size

            try {
                rot.freeze90()
                DeviceShell.waitUntil("the activity to be recreated and laid out in portrait", 20_000) {
                    compose.mainClock.advanceTimeBy(200)
                    val nodes = compose.onAllNodesWithTag("play-area").fetchSemanticsNodes()
                    nodes.isNotEmpty() && nodes[0].size.height > nodes[0].size.width
                }
                compose.mainClock.advanceTimeBy(1000)

                val (w, h) = RealDisplay.sizePx()
                val density = spec.densityDpi / 160.0
                assertTrue("window ${w / density} x ${h / density} dp, wanted 800 x 1280", abs(w / density - 800) <= 0.5 && abs(h / density - 1280) <= 0.5)
                compose.onNodeWithTag("puzzle-state").assertTextEquals(en.getValue("state_in_progress"))
                val areaAfter = compose.onNodeWithTag("play-area").fetchSemanticsNode().size
                assertNotEquals("the screen was not laid out again", areaBefore, areaAfter)

                val rig2 = TouchRig(compose)
                val shot = rig2.shot()
                val board = rig2.board()
                val m = rig2.mapper(shot, board, puzzle)
                for (piece in puzzle.solution.map { it.piece }) {
                    if (piece in placed) {
                        assertTrue("$piece is no longer on the board at its place after the turn", rig2.pieceIsOnBoardAtSolution(shot, m, puzzle, piece))
                    } else {
                        assertTrue("$piece is no longer in the tray after the turn", rig2.inTray(shot, board, piece))
                    }
                }
                val unplaced = PieceId.entries.filter { it in puzzle.solution.map { s -> s.piece } && it !in placed }
                assertEquals("fixture: four pieces remain in the tray", 4, unplaced.size)
            } finally {
                rot.restore()
            }
        }
    }
}
