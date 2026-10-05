package io.github.jamisuni.tangram.acceptance.held

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * HELD-OUT (Test & Verify only): REQ-029 A1 on the real app (design WO-008 Acceptance IDs table). Own use of the held kit (`acceptance.held`).
 *
 * A real touch opens the real settings screen; a manual clock then runs five minutes with nobody touching the phone, moved only by
 * `advanceActive` (on the main thread, never a sleep). The screen's all-time play time may grow by at most one minute. Seeded through the real
 * store: nothing (a fresh install), so the total starts at 0:00 and every second on the screen is one the keeper credited.
 *
 * The fixture can tell the cases apart: a keeper that ignores the idle window shows 5:00, one that never counts shows 0:00. So the test first
 * proves the clock counts (10 touched seconds read 0:10), then the five untouched minutes (the total is exactly the 60 s window: at most 60 and
 * not less than the window, so a keeper that stopped counting early, or never counted, fails too), and that the next touch counts again.
 */
class HeldIdleCutoffAppTest {
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(TestConfigRule()).around(ResetStoreRule()).around(compose)

    private fun totalSeconds(): Long {
        val text = ScreenWalk.textOf(compose, "settings-play-total")
        val m = Regex("(\\d+):(\\d{2})").findAll(text).lastOrNull() ?: error("no m:ss value in the all-time row: \"$text\"")
        return m.groupValues[1].toLong() * 60 + m.groupValues[2].toLong()
    }

    // REQ-029.A1 - "Leaving the phone untouched for 5 minutes adds at most 60 s."
    @Test
    fun leavingThePhoneUntouchedForFiveMinutesAddsAtMostSixtySeconds() {
        Seed.screen(WalkScreen.NEW)
        val clock = ManualTimeSource()
        AppLaunch.launch("en-US", timeSource = clock).use { scenario ->
            compose.waitForIdle()
            compose.touch("settings-button") // a real touch: the keeper is engaged from here
            compose.onNodeWithTag("settings-overlay").assertExists()
            assertEquals("fresh install: nothing played yet", 0L, totalSeconds())

            scenario.advanceActive(compose, clock, 10_000)
            assertEquals("fixture: 10 touched seconds are counted (the clock is wired and the keeper counts)", 10L, totalSeconds())

            val before = totalSeconds()
            scenario.advanceActive(compose, clock, 300_000) // five minutes, nobody touches the phone
            val added = totalSeconds() - before
            assertTrue("5 untouched minutes added $added s (REQ-029 A1: at most 60 s)", added <= 60)
            assertEquals("the 60 s window after the last touch is still counted: 10 s were already in, 50 s of it remain", 50L, added)
            assertEquals("all-time total read back", 60L, totalSeconds())

            // "until next press": one more touch and the count goes on
            compose.onNodeWithTag("settings-free-note").performScrollTo()
            compose.onNodeWithTag("settings-free-note").performTouchInput { click() } // a touch on plain text: no control, but a real contact
            scenario.advanceActive(compose, clock, 5_000)
            assertEquals("a touch after the pause starts the count again", 65L, totalSeconds())
        }
    }

    // REQ-029.A1 - the same, for a screen that is not the settings screen: the idle cut-off is the keeper's, not the screen's. The total is read
    // after the settings screen is opened at the end (a touch), so the reading itself is the only touch since the five minutes.
    @Test
    fun theCutOffHoldsOnAPlayScreenToo() {
        Seed.screen(WalkScreen.NEW)
        val clock = ManualTimeSource()
        AppLaunch.launch("en-US", timeSource = clock).use { scenario ->
            compose.waitForIdle()
            compose.touch("puzzle-state") // a harmless real touch on the play screen
            scenario.advanceActive(compose, clock, 300_000)
            compose.touch("settings-button") // the touch that reads the value: it accounts the interval first, under the old flags
            val total = totalSeconds()
            assertTrue("5 untouched minutes on a play screen added $total s (at most 60)", total <= 60)
            assertEquals("and exactly the window", 60L, total)
        }
    }
}
