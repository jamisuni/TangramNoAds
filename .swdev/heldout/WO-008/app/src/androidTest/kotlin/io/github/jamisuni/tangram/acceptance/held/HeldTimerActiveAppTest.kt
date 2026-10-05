package io.github.jamisuni.tangram.acceptance.held

import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * HELD-OUT (Test & Verify only): REQ-031 A2 on the real app (design WO-008 Acceptance IDs table). Own use of the held kit.
 *
 * A fresh install (the timer is off, REQ-031 A1). The timer is switched ON through the real settings screen. One piece is placed by real touch
 * (so the puzzle is In progress and its clock runs). Time moves only by `advanceActive`. The pill must show the active seconds: it follows the
 * touched clock, and it does not move once the player has left the phone alone for more than a minute.
 */
class HeldTimerActiveAppTest {
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(TestConfigRule()).around(ResetStoreRule()).around(compose)

    private val puzzle = Seed.puzzles.first()

    private fun pill() = ScreenWalk.textOf(compose, "puzzle-timer")

    private fun touchPlayText() = compose.onNodeWithTag("puzzle-state").performTouchInput { click() }

    // REQ-031.A2 - "With the setting on, the timer is shown and counts active seconds only."
    @Test
    fun withTheSettingOnTheTimerIsShownAndCountsActiveSecondsOnly() {
        Seed.screen(WalkScreen.NEW, puzzle)
        val clock = ManualTimeSource()
        AppLaunch.launch("en-US", timeSource = clock).use { scenario ->
            compose.waitForIdle()
            compose.onAllNodesWithTag("puzzle-timer").assertCountIsZero()

            // switched on through the real settings screen
            compose.touch("settings-button")
            compose.onNodeWithTag("settings-overlay").assertExists()
            compose.touch("settings-timer")
            compose.onNodeWithTag("settings-timer").assertIsOn()
            compose.touch("settings-close")
            compose.waitForIdle()
            assertEquals("the timer is shown once switched on", "0:00", pill())

            // one piece by real touch: the puzzle is In progress and its clock runs
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(300)
            TouchRig(compose).placePieces(puzzle, 1)
            compose.mainClock.advanceTimeBy(600)
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()

            scenario.advanceActive(compose, clock, 5_000)
            assertEquals("the timer follows the touched clock", "0:05", pill())

            touchPlayText()
            scenario.advanceActive(compose, clock, 10_000)
            assertEquals("and keeps following it", "0:15", pill())

            // a touch, then exactly the minute's window, then two minutes with nobody touching: the value stops moving after the minute
            touchPlayText()
            scenario.advanceActive(compose, clock, 60_000)
            val atWindowEnd = pill()
            assertEquals("a minute after the last touch the time still counted", "1:15", atWindowEnd)
            scenario.advanceActive(compose, clock, 120_000)
            assertEquals("untouched for a minute or more: the timer does not move", atWindowEnd, pill())
            scenario.advanceActive(compose, clock, 300_000)
            assertEquals("still not after five more minutes", atWindowEnd, pill())

            // the next touch starts it again
            touchPlayText()
            scenario.advanceActive(compose, clock, 3_000)
            assertEquals("the next touch counts again", "1:18", pill())
        }
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.assertCountIsZero() {
    assertEquals("a timer is shown while the setting is off", 0, fetchSemanticsNodes().size)
}
