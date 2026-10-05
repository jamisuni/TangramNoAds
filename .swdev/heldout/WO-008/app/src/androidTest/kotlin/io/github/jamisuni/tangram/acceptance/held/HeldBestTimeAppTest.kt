package io.github.jamisuni.tangram.acceptance.held

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * HELD-OUT (Test & Verify only): REQ-005 A1 and REQ-030 A2 on the real app, and the carried REQ-046 A3 (the aid sets no best time) on real best
 * times (design WO-008 Acceptance IDs table, carried parts, N7c and N7d). Own use of the held kit; the real store file is re-read through a
 * FRESH `JsonProgressStore` (what a relaunched app reads).
 *
 * A player's solve is made by real touch in the SolveByTouch pattern: all pieces but the last are placed (the Compose clock paused), the clock
 * is given back and N active seconds pass through `advanceActive` (never a sleep), the clock is paused again and the LAST piece is dropped and
 * not judged by colour (the solved picture replaces the piece colours at the moment of the solve, so a colour check of that drop would read a
 * correct solve as a miss). The solve time is therefore exactly the N seconds: placing and dropping happen on the frozen manual clock.
 */
class HeldBestTimeAppTest {
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(TestConfigRule()).around(ResetStoreRule()).around(compose)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val en = ScreenWalk.bundle(context, "en-US").getValue("browse")
    private val puzzles = Seed.puzzles
    private val puzzle: Puzzle = puzzles.first()

    private fun bestText() = ScreenWalk.textOf(compose, "best-time")

    /**
     * One player's solve of the shown puzzle (New): [seconds] active seconds pass between the second-to-last drop and the last one. Leaves the
     * solved bar showing, the Compose clock running.
     */
    private fun solveInSeconds(scenario: androidx.test.core.app.ActivityScenario<io.github.jamisuni.tangram.LocaleOverrideActivity>, clock: ManualTimeSource, seconds: Long) {
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(300)
        val rig = TouchRig(compose)
        val placed = rig.placePieces(puzzle, puzzle.solution.size - 1)
        compose.mainClock.advanceTimeBy(600)
        compose.mainClock.autoAdvance = true // the running gate follows a state change by one frame: give it the clock before time passes
        compose.waitForIdle()
        scenario.advanceActive(compose, clock, seconds * 1_000)
        compose.mainClock.autoAdvance = false
        val last = puzzle.solution.map { it.piece }.first { it !in placed }
        rig.tryPlace(puzzle, last) // deliberately not judged by colour (the SolveByTouch pattern)
        compose.mainClock.advanceTimeBy(3_500) // the REQ-023 timeline
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithTag("solved-bar").assertExists()
        assertEquals(en.getValue("state_solved"), ScreenWalk.textOf(compose, "puzzle-state"))
    }

    private fun mmss(seconds: Long) = "${seconds / 60}:${"%02d".format(seconds % 60)}"

    private fun stored() = AppStore.open().progress(puzzle.id)

    // REQ-005.A1 - "After solving a puzzle, its solve time is available."
    // The first solve takes 30 active seconds: the solved bar shows 0:30; the stored Solved record holds the attempt's time (30) and the best (30)
    // in a fresh store reader; and after a relaunch the solved bar still shows 0:30.
    @Test
    fun afterSolvingAPuzzleItsSolveTimeIsAvailableOnScreenAndInTheStore() {
        Seed.screen(WalkScreen.NEW, puzzle)
        val clock = ManualTimeSource()
        AppLaunch.launch("en-US", timeSource = clock).use { scenario ->
            compose.waitForIdle()
            solveInSeconds(scenario, clock, 30)
            assertEquals("the solved bar shows the solve time", mmss(30), bestText())
        }
        val record = stored()
        assertEquals(PuzzleState.SOLVED, record.state)
        assertEquals("the stored solve time", 30L, record.puzzleSeconds)
        assertEquals("a first solve is its own best", 30L, record.bestSeconds)
        AppLaunch.launch("en-US", timeSource = ManualTimeSource()).use {
            compose.waitForIdle()
            assertEquals("after a relaunch the solve time is still shown", mmss(30), bestText())
        }
    }

    // REQ-030.A2 - "Solving again slower keeps the earlier, faster best time."
    // Solve (30 s), Retry, solve slower (50 s): the bar still shows 0:30 and the store's best is 30. Retry, solve faster (20 s): 0:20. Retry,
    // solve in exactly 20 s again: it stays 0:20.
    @Test
    fun solvingAgainSlowerKeepsTheFasterBestAndFasterUpdatesIt() {
        Seed.screen(WalkScreen.NEW, puzzle)
        val clock = ManualTimeSource()
        AppLaunch.launch("en-US", timeSource = clock).use { scenario ->
            compose.waitForIdle()
            solveInSeconds(scenario, clock, 30)
            assertEquals(mmss(30), bestText())

            compose.touch("retry-button")
            compose.waitForIdle()
            solveInSeconds(scenario, clock, 50)
            assertEquals("a slower solve keeps the earlier, faster best on the bar", mmss(30), bestText())
            val slower = stored()
            assertEquals("the stored best is still the faster one", 30L, slower.bestSeconds)
            // REQ-005.A1 (N7d): the Solved record's seconds are the NEW attempt's time, the best stays the first
            assertEquals("the Solved record holds the new attempt's time", 50L, slower.puzzleSeconds)

            compose.touch("retry-button")
            compose.waitForIdle()
            solveInSeconds(scenario, clock, 20)
            assertEquals("a faster solve updates the best", mmss(20), bestText())
            assertEquals(20L, stored().bestSeconds)
            assertEquals(20L, stored().puzzleSeconds)

            compose.touch("retry-button")
            compose.waitForIdle()
            solveInSeconds(scenario, clock, 20)
            assertEquals("an equal solve keeps the best", mmss(20), bestText())
            assertEquals(20L, stored().bestSeconds)
        }
        assertEquals("the best survives the app closing", 20L, stored().bestSeconds)
    }

    private fun useTheAid() {
        compose.mainClock.autoAdvance = true // an earlier step may have left the clock paused
        compose.waitForIdle()
        compose.heldUnlock()
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(100)
        compose.heldTouch("dev-solve-now") // the solve happens on the release of this touch
        compose.mainClock.advanceTimeBy(3_500) // 600 ms delay + 800 ms fade + 2.4 s confetti, with room
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithTag("solved-bar").assertExists()
    }

    // REQ-046.A3 - "'Solve this puzzle now' shows the solved picture and leaves the best time empty." (carried, N7c: on REAL best times)
    // Puzzle 1 is seeded In progress, 10 s counted, with an EARLIER BEST OF 600 s that is larger than the 30 seconds counted by the time the aid is
    // used (so a best computed as min(best, counted) would show 30). After the aid its best is still 600 (10:00) on the bar and in the store.
    // Puzzle 2 was never solved (best empty): after the aid its bar shows the dash, its stored best is null, and the settings best-time list has a row
    // for puzzle 1 and none for puzzle 2.
    @Test
    fun theAidSetsNoBestTimeOnRealBestTimes() {
        val x = puzzles[0]
        val y = puzzles[1]
        Seed.screen(WalkScreen.NEW, x)
        Seed.inProgress(x, seconds = 10, best = 600, pieces = 2)
        val clock = ManualTimeSource()
        AppLaunch.launch("en-US", timeSource = clock).use { scenario ->
            compose.waitForIdle()
            compose.touch("puzzle-state") // engages the keeper
            scenario.advanceActive(compose, clock, 20_000)
            useTheAid()
            assertEquals("the earlier best is still shown after the aid solve", "10:00", bestText())
        }
        val afterX = AppStore.open().progress(x.id)
        assertEquals(PuzzleState.SOLVED, afterX.state)
        assertEquals("the aid did not replace an earlier best by the seconds counted so far", 600L, afterX.bestSeconds)

        Seed.lastShown(y) // puzzle 2: never solved, nothing stored
        AppLaunch.launch("en-US", timeSource = ManualTimeSource()).use {
            compose.waitForIdle()
            useTheAid()
            assertEquals("a never-solved puzzle has no best after the aid: the dash", en.getValue("best_time_none"), bestText())
            compose.touch("settings-button")
            compose.onNodeWithTag("settings-overlay").assertExists()
            compose.onNodeWithTag("settings-best-times").assertExists()
            compose.onNodeWithTag("settings-best-${x.id.value}").assertExists()
            assertTrue("puzzle 1's row shows its best: ${ScreenWalk.textOf(compose, "settings-best-${x.id.value}")}", "10:00" in ScreenWalk.textOf(compose, "settings-best-${x.id.value}"))
            compose.onAllNodesWithTag("settings-best-${y.id.value}").let { assertEquals("no best-time row for a puzzle solved only by the aid", 0, it.fetchSemanticsNodes().size) }
        }
        val afterY = AppStore.open().progress(y.id)
        assertEquals(PuzzleState.SOLVED, afterY.state)
        assertNull("the aid set a best time on a never-solved puzzle", afterY.bestSeconds)
    }
}
