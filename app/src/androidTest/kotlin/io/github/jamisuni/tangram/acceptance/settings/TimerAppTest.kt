package io.github.jamisuni.tangram.acceptance.settings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import io.github.jamisuni.tangram.acceptance.AppStore
import io.github.jamisuni.tangram.acceptance.ResetStoreRule
import io.github.jamisuni.tangram.acceptance.layout.AppLaunch
import io.github.jamisuni.tangram.acceptance.layout.Seed
import io.github.jamisuni.tangram.acceptance.layout.TestConfigRule
import io.github.jamisuni.tangram.acceptance.layout.WalkScreen
import io.github.jamisuni.tangram.acceptance.touch
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * App-level cover of REQ-031 A1 (design WO-008 Acceptance IDs table, TASK-T8a): the real app on a fresh install shows no timer on any play
 * screen. Seeded through the real store (design 8 "Seeding recipe"): the store is wiped and no setting is written, so the timer switch is at its
 * default. English and Finnish are the same here (the pill is digits). The positive control (decision DA-140) proves the check can see a pill.
 */
class TimerAppTest {
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(ResetStoreRule()).around(TestConfigRule()).around(compose)

    // REQ-031.A1 - "On a fresh install, no timer is shown."
    // Every play screen state on a fresh install (New, In progress, Solved, and the grid over a play screen): no `puzzle-timer` node exists; and the
    // switch on the settings screen reads Off.
    @Test
    fun aFreshInstallShowsNoTimerOnAnyPlayScreen() {
        for (screen in listOf(WalkScreen.NEW, WalkScreen.IN_PROGRESS, WalkScreen.SOLVED, WalkScreen.GRID)) {
            Seed.screen(screen) // wipes the store: nothing is stored for the timer, so the default (off) is in force
            AppLaunch.launch("en-US").use {
                Seed.reach(compose, screen)
                compose.onAllNodesWithTag("puzzle-timer").assertCountEquals(0)
            }
        }
        Seed.screen(WalkScreen.SETTINGS)
        AppLaunch.launch("en-US").use {
            Seed.reach(compose, WalkScreen.SETTINGS)
            compose.onNodeWithTag("settings-timer").assertIsOff()
            compose.onAllNodesWithTag("puzzle-timer").assertCountEquals(0)
        }
        assertFalse("nothing was ever stored for the timer", AppStore.open().settings().timerShown)
    }

    // decision DA-140 (positive control for the check above): the same screen DOES show a pill once the real settings switch is turned on, and not
    // again once it is turned off, so "no timer" above is not a blind spot. The switch is stored (REQ-031 Rules: off or on).
    @Test
    fun thePillAppearsWithTheSwitchOnAndGoesWithItOff() {
        Seed.screen(WalkScreen.NEW)
        AppLaunch.launch("en-US").use {
            compose.waitForIdle()
            compose.onAllNodesWithTag("puzzle-timer").assertCountEquals(0)
            compose.touch("settings-button")
            compose.onNodeWithTag("settings-overlay").assertExists()
            compose.touch("settings-timer")
            assertTrue("the on switch is stored", AppStore.open().settings().timerShown)
            compose.touch("settings-close")
            compose.waitForIdle()
            compose.onNodeWithTag("puzzle-timer").assertExists()

            compose.touch("settings-button")
            compose.touch("settings-timer")
            assertFalse("the off switch is stored", AppStore.open().settings().timerShown)
            compose.touch("settings-close")
            compose.waitForIdle()
            compose.onAllNodesWithTag("puzzle-timer").assertCountEquals(0)
        }
    }
}
