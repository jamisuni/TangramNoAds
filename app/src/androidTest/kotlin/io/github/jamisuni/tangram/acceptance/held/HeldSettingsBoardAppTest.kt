package io.github.jamisuni.tangram.acceptance.held

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.LocaleOverrideActivity
import io.github.jamisuni.tangram.acceptance.layout.UsesDisplayRule
import io.github.jamisuni.tangram.kernel.model.PieceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import kotlin.math.abs

/**
 * HELD-OUT (Test & Verify only): REQ-032 A1 on the real app, end to end (design WO-007 acceptance table). Own kit copies (package
 * `...acceptance.held`); the marker is the visible one by FQN.
 *
 * Real touch places three pieces of a full-set puzzle on their solution places. The settings overlay is then opened and closed in every way
 * the player can: by Done, by Android Back, by recreating the activity (and, on the 1280 x 800 tablet, by a REAL 90 degree rotation) with the
 * overlay open, and with a SECOND FINGER pressed on a tray piece while the gear is tapped (review N4: a finger already down on a piece must
 * not drag under the sheet). After each, the board is unchanged: `puzzle-state` and the counter read as before, each placed piece has its
 * colour at its solution centroid (never a corner, DA-40/DA-70), and every other piece is in the tray. A confirmed reset is EXCLUDED (F25):
 * nothing here erases.
 */
@UsesDisplayRule
@RunWith(Parameterized::class)
class HeldSettingsBoardAppTest(private val spec: DisplaySpec) {
    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun specs(): List<DisplaySpec> = listOf(DisplaySpec.PHONE_390x844, DisplaySpec.TABLET_1280x800)
    }

    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(DisplayRule(spec)).around(TestConfigRule()).around(ResetStoreRule()).around(compose)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val en = ScreenWalk.bundle(context, "en-US").getValue("browse")

    private class Baseline(val placed: List<PieceId>, val counter: String)

    private fun settle(ms: Long = 600) = compose.mainClock.advanceTimeBy(ms)

    /** The board as the player sees it equals [b]: state text, counter, each placed piece at its solution centroid, every other piece in the tray. */
    private fun assertBoardUnchanged(what: String, b: Baseline, puzzle: io.github.jamisuni.tangram.contracts.puzzle.Puzzle) {
        compose.onNodeWithTag("settings-overlay").assertDoesNotExist()
        compose.onNodeWithTag("puzzle-state").assertTextEquals(en.getValue("state_in_progress"))
        assertEquals("$what: the counter", b.counter, ScreenWalk.textOf(compose, "puzzle-counter"))
        val rig = TouchRig(compose)
        val shot = rig.shot()
        val board = rig.board()
        val m = rig.mapper(shot, board, puzzle)
        for (piece in puzzle.solution.map { it.piece }) {
            if (piece in b.placed) {
                assertTrue("$what: $piece is no longer on the board at its place", rig.pieceIsOnBoardAtSolution(shot, m, puzzle, piece))
            } else {
                assertTrue("$what: $piece left the tray", rig.inTray(shot, board, piece))
            }
        }
    }

    private fun openByGear() {
        compose.touch("settings-button")
        compose.onNodeWithTag("settings-overlay").assertExists()
    }

    private fun closeByDone() {
        compose.touch("settings-close")
        compose.onNodeWithTag("settings-overlay").assertDoesNotExist()
    }

    // REQ-032.A1 - "Opening and closing the settings changes nothing on the board."
    // (a reset is excluded, F25) Every way of opening and closing, on a board with three placed pieces.
    @Test
    fun req032_A1_openingAndClosingTheSettingsChangesNothingOnTheBoard() {
        val puzzle = Seed.fullPuzzle()
        Seed.screen(WalkScreen.NEW, puzzle)
        val rot = Rotation()
        val rotate = spec == DisplaySpec.TABLET_1280x800
        val scenario: ActivityScenario<LocaleOverrideActivity> = AppLaunch.launch("en-US")
        try {
            compose.mainClock.autoAdvance = false
            settle(300)
            val rig = TouchRig(compose)
            val placed = rig.placePieces(puzzle, 3)
            settle()
            val baseline = Baseline(placed, ScreenWalk.textOf(compose, "puzzle-counter"))
            for (p in placed) assertTrue("fixture: $p is on the board before", rig.pieceIsOnBoardAtSolution(rig.shot(), rig.mapper(rig.shot(), rig.board(), puzzle), puzzle, p))
            assertBoardUnchanged("before anything", baseline, puzzle)

            // 1. open by the gear, close by Done
            openByGear()
            closeByDone()
            settle()
            assertBoardUnchanged("after Done", baseline, puzzle)

            // 2. open, close by Android Back
            openByGear()
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            settle()
            compose.waitForIdle()
            compose.onNodeWithTag("settings-overlay").assertDoesNotExist()
            assertBoardUnchanged("after Back", baseline, puzzle)

            // 3. open, recreate the activity with the overlay open, then Done
            openByGear()
            scenario.recreate()
            settle(1000)
            compose.onNodeWithTag("settings-overlay").assertExists()
            closeByDone()
            settle()
            assertBoardUnchanged("after recreate with the overlay open", baseline, puzzle)

            // 4. a second finger: a finger is already down on a tray piece when the gear is tapped (N4)
            val tray = compose.readTray()
            val victim = puzzle.solution.map { it.piece }.first { it !in placed }
            val cell = tray.cells.getValue(victim)
            val press = Offset(cell.left + 8f * rig.density, cell.bottom - 8f * rig.density) // the cell's bottom left: never under the flip badge
            compose.onNodeWithTag("play-area").performTouchInput { down(0, press) }
            settle(30)
            compose.onNodeWithTag("settings-button").performTouchInput { down(1, center) }
            settle(30)
            compose.onNodeWithTag("settings-button").performTouchInput { up(1) }
            settle(300)
            compose.onNodeWithTag("settings-overlay").assertExists()
            // the first finger now moves a long way and lets go, under the sheet: nothing may be dragged, locked or returned
            compose.onRoot().performTouchInput { moveTo(0, Offset(press.x, press.y - 200f * rig.density)) }
            settle(150)
            compose.onRoot().performTouchInput { moveTo(0, center) }
            settle(250)
            compose.onRoot().performTouchInput { up(0) }
            settle(600)
            closeByDone()
            settle()
            assertBoardUnchanged("after a second finger on a piece while the gear was tapped", baseline, puzzle)

            // 5. a real rotation with the overlay open (tablet), then Done
            if (rotate) {
                openByGear()
                try {
                    rot.freeze90()
                    DeviceShell.waitUntil("the overlay to be laid out in portrait", 20_000) {
                        compose.mainClock.advanceTimeBy(200)
                        val nodes = compose.onAllNodesWithTag("settings-overlay").fetchSemanticsNodes()
                        nodes.isNotEmpty() && nodes[0].size.height > nodes[0].size.width
                    }
                    settle(1000)
                    val (w, h) = RealDisplay.sizePx()
                    val density = spec.densityDpi / 160.0
                    assertTrue("window ${w / density} x ${h / density} dp, wanted 800 x 1280", abs(w / density - 800) <= 0.5 && abs(h / density - 1280) <= 0.5)
                    compose.onNodeWithTag("settings-overlay").assertExists()
                    closeByDone()
                    settle()
                    assertBoardUnchanged("after a rotation with the overlay open", baseline, puzzle)
                } finally {
                    rot.restore()
                }
            }
        } finally {
            scenario.close()
        }
    }
}
