package io.github.jamisuni.tangram.acceptance.promise

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.acceptance.ResetStoreRule
import io.github.jamisuni.tangram.acceptance.TouchRig
import io.github.jamisuni.tangram.acceptance.layout.AirplaneMode
import io.github.jamisuni.tangram.acceptance.layout.AppLaunch
import io.github.jamisuni.tangram.acceptance.layout.ScreenWalk
import io.github.jamisuni.tangram.acceptance.layout.Seed
import io.github.jamisuni.tangram.acceptance.layout.TestConfigRule
import io.github.jamisuni.tangram.acceptance.layout.solveByTouch
import io.github.jamisuni.tangram.acceptance.touch
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * Airplane mode on the real app (design WO-006 section 6 item 5, DA-105). The test turns airplane mode on itself and asserts the flag, plays
 * every feature that exists now, and restores the SAVED PRIOR state in `after`. The claim itself rests on the absence of the INTERNET
 * permission (the installed-permissions test and V-08): with it Android refuses every socket, so airplane mode cannot change what the app
 * does; this run shows the device behaviour. On API 26 airplane mode is "flag set and data off" (Wi-Fi cannot be cut on that image; logged).
 * Settings, the timer and play time are carried to WO-007 and WO-008 (C7).
 */
class AirplaneModeAppTest {
    private val compose = createEmptyComposeRule()
    private val airplane = AirplaneMode()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(ResetStoreRule()).around(TestConfigRule()).around(compose)

    @After
    fun restoreAirplane() = airplane.restore()

    private val puzzles = Seed.puzzles
    private val n = puzzles.size
    private val en = ScreenWalk.bundle(InstrumentationRegistry.getInstrumentation().targetContext, "en-US").getValue("browse")

    // REQ-010.A1 - "With the device in airplane mode, every feature works."
    // Airplane mode on (flag asserted), then the player's flow: a piece is dragged to its solution place, > and < keep it, the grid opens
    // another puzzle, Restart empties the started puzzle, a relaunch (recreate) brings the progress back, and the first puzzle is solved.
    @Test
    fun req010_A1_everyFeatureThatExistsWorksInAirplaneMode() {
        airplane.on()
        assertTrue("fixture: airplane mode is on", airplane.isOn())
        val first = puzzles.first()
        AppLaunch.launch("en-US").use { scenario ->
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(300)

            // a piece by real touch, kept across > and <
            val rig = TouchRig(compose)
            val placed = rig.placePieces(first, 1).single()
            compose.touch("next-button")
            compose.touch("prev-button")
            compose.mainClock.advanceTimeBy(600)
            val shot = rig.shot()
            val m = rig.mapper(shot, rig.board(), first)
            assertTrue("$placed is still on the board in its place", rig.pieceIsOnBoardAtSolution(shot, m, first, placed))

            // the grid opens another puzzle, and back
            compose.touch("puzzle-counter")
            compose.touch("grid-cell-${puzzles[1].id.value}")
            compose.mainClock.advanceTimeBy(600)
            compose.onNodeWithTag("puzzle-counter").assertTextEquals("2 / $n")
            compose.touch("prev-button")
            compose.mainClock.advanceTimeBy(600)
            compose.onNodeWithTag("puzzle-counter").assertTextEquals("1 / $n")

            // a relaunch (recreate) brings the progress back
            scenario.recreate()
            compose.mainClock.advanceTimeBy(600)
            val rig2 = TouchRig(compose)
            val shot2 = rig2.shot()
            val m2 = rig2.mapper(shot2, rig2.board(), first)
            assertTrue("$placed came back after the relaunch", rig2.pieceIsOnBoardAtSolution(shot2, m2, first, placed))

            // Restart empties the started puzzle
            compose.touch("restart-button")
            compose.mainClock.advanceTimeBy(600)
            compose.onNodeWithTag("puzzle-state").assertTextEquals(en.getValue("state_new"))

            // the first puzzle is solved
            TouchRig(compose).solveByTouch(first)
            compose.mainClock.advanceTimeBy(3000)
            compose.onNodeWithTag("solved-bar").assertExists()
            compose.onNodeWithTag("puzzle-state").assertTextEquals(en.getValue("state_solved"))
        }
    }
}
