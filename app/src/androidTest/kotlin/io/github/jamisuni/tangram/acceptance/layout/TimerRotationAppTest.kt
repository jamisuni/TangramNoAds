package io.github.jamisuni.tangram.acceptance.layout

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.lifecycle.Lifecycle
import io.github.jamisuni.tangram.acceptance.ResetStoreRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * C9 (design WO-008 "Carried parts": puzzle time across rotation): the on-board timer when the activity is stopped and started again (a rotation
 * or `recreate()` stops and starts the activity, the keeper lives in the ViewModel and so survives). Seeded through the real store: the Cat In
 * progress with 12 puzzle seconds, the timer switch on. A manual clock, moved only by `advanceActive` (and, across the stop-to-start gap, by one
 * `onActivity` on the main thread: E6). The pill text is the format `m:ss` (0:12).
 *
 * REQ-031 A2 is carried here for what A2 means ("the timer is shown and counts active seconds only"): the timer is shown after the stop and
 * start, the clock advanced across the gap credits nothing, and it counts again afterwards. "The value is kept" alone is `// decision F3`.
 */
class TimerRotationAppTest {
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(ResetStoreRule()).around(TestConfigRule()).around(compose)

    private val cat = Seed.fullPuzzle()

    private fun seed() {
        Seed.screen(WalkScreen.IN_PROGRESS, cat)
        Seed.settings(timerShown = true)
        Seed.inProgress(cat, seconds = 12, best = null, pieces = 3)
    }

    /** A harmless touch: it engages the keeper (a pointer contact anywhere is a touch) without moving a piece or opening a screen. */
    private fun engage() {
        compose.onNodeWithTag("puzzle-title").performTouchInput { click() }
        compose.waitForIdle()
    }

    private fun pill() = ScreenWalk.textOf(compose, "puzzle-timer")

    // REQ-031.A2 - "With the setting on, the timer is shown and counts active seconds only."
    // The timer is shown on launch (0:12); 5 active seconds make it 0:17; the activity is stopped, the clock runs on 30 s while it is stopped
    // (nobody is looking: nothing is credited), and it is started again: the timer is shown and reads 0:17; 3 more active seconds make it 0:20.
    @Test
    fun theTimerIsShownAfterStopAndStartAndTheGapCreditsNothing() {
        seed()
        val clock = ManualTimeSource()
        AppLaunch.launch("en-US", timeSource = clock).use { scenario ->
            compose.waitForIdle()
            assertEquals("the timer is shown with the seeded puzzle time", "0:12", pill())
            engage()
            scenario.advanceActive(compose, clock, 5_000)
            assertEquals("0:17", pill())

            scenario.moveToState(Lifecycle.State.CREATED) // onStop: the app is no longer visible
            scenario.onActivity { clock.advance(30_000) } // 30 s pass while it is stopped (main thread, E6)
            scenario.moveToState(Lifecycle.State.RESUMED) // onStart: visible again
            compose.waitForIdle()
            assertEquals("the timer is shown after the stop and start, the 30 s credited nothing", "0:17", pill())

            scenario.advanceActive(compose, clock, 3_000) // the last touch was 38 s ago: inside the 60 s window, so these count
            assertEquals("it counts again afterwards", "0:20", pill())
        }
    }

    // decision F3: the puzzle's seconds are kept by a recreate() (rotation): the ViewModel and its keeper survive, so the value on the new
    // activity is the value on the old one, and the timer is still shown.
    @Test
    fun theValueIsKeptByARecreate() {
        seed()
        val clock = ManualTimeSource()
        AppLaunch.launch("en-US", timeSource = clock).use { scenario ->
            compose.waitForIdle()
            engage()
            scenario.advanceActive(compose, clock, 5_000)
            assertEquals("0:17", pill())
            scenario.recreate()
            compose.waitForIdle()
            assertEquals("the value is kept by the recreate", "0:17", pill())
        }
    }
}
