package io.github.jamisuni.tangram.acceptance.held

import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import io.github.jamisuni.tangram.contracts.progress.GameSettings
import io.github.jamisuni.tangram.contracts.progress.PlayTime
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import java.time.LocalDate

// decision DA-138: HELD DECISION TEST (WO-008 T8b, E5), no requirement token. It asserts REQ-034's Statement ("erase all puzzle, best and play
// times"), not the meaning of A1 ("every puzzle shows as New", which HeldResetAppTest covers), so it carries no REQ token.
//
// The discriminating guards for a flush that is due AT the moment of Erase are OwnershipOrderTest (1) and (8) in `time` (a device test cannot hold
// a flush due at that very moment: the ticker flushes within a second of an advance). This class is the end-to-end regression check: with a flush
// already written and time running on both sides of the Erase, the play time and every best read as erased from a FRESH store and from a relaunched
// app, and only the seconds played AFTER the reset ever accumulate (never the pre-reset ones, which a stale write would bring back).
class HeldResetTimeAppTest {
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(TestConfigRule()).around(ResetStoreRule()).around(compose)

    private val day = LocalDate.of(2026, 10, 5)
    private val puzzles = Seed.puzzles
    private val playing = puzzles[0]
    private val solvedOne = puzzles[1]

    private fun totalText() = ScreenWalk.textOf(compose, "settings-play-total")

    private fun touchSettingsText() {
        compose.onNodeWithTag("settings-free-note").performScrollTo()
        compose.onNodeWithTag("settings-free-note").performTouchInput { click() }
    }

    @Test
    fun afterAResetWithTimeRunningOnBothSidesOnlyPostResetSecondsAccumulate() {
        Seed.screen(WalkScreen.NEW, playing)
        Seed.inProgress(playing, seconds = 10, best = 80, pieces = 2)
        Seed.solved(solvedOne, seconds = 52, best = 44)
        Seed.playTime(day, 125, 4_000)
        Seed.settings(timerShown = true, soundOn = false)
        val clock = ManualTimeSource(date = day)

        AppLaunch.launch("en-US", timeSource = clock).use { scenario ->
            compose.waitForIdle()
            compose.touch("puzzle-state") // engages the keeper
            scenario.advanceActive(compose, clock, 15_000) // past the 10 s flush mark, with a touch: seconds are written
            val before = AppStore.open().playTime()
            assertEquals("fixture: 15 active seconds are on the seeded play time and were written (today)", 140L, before.todaySeconds)
            assertEquals("fixture: ... and in total", 4_015L, before.totalSeconds)

            compose.touch("settings-button")
            compose.onNodeWithTag("settings-overlay").assertExists()
            compose.onNodeWithTag("settings-reset").performScrollTo()
            compose.touch("settings-reset")
            compose.onNodeWithTag("settings-reset-confirm").performScrollTo()
            compose.touch("settings-reset-confirm") // Erase
            compose.waitForIdle()
            assertEquals("right after Erase the total reads zero", "0:00", Regex("(\\d+:\\d{2})").findAll(totalText()).last().value)
            assertEquals("the best-time list is empty at once", 0, compose.onAllNodesWithTag("settings-best-times").fetchSemanticsNodes().size)

            // time keeps running AFTER the reset: 7 touched seconds with the settings screen still open
            touchSettingsText()
            scenario.advanceActive(compose, clock, 7_000)
            assertEquals("only the 7 post-reset seconds are on the screen", "0:07", Regex("(\\d+:\\d{2})").findAll(totalText()).last().value)
            compose.touch("settings-close")
        } // the activity stops: the stop point writes the play time

        val fresh = AppStore.open()
        assertEquals("the stored play time holds only the post-reset seconds", PlayTime(day, 7, 7), fresh.playTime())
        for (p in puzzles) assertEquals("${p.id.value} is erased in the stored file (puzzle and best time)", PuzzleProgress.NEW, fresh.progress(p.id))
        assertEquals("the settings are kept by a reset", GameSettings(timerShown = true, soundOn = false), fresh.settings())

        // a relaunch reads the same, and nothing from before comes back while it runs
        val clock2 = ManualTimeSource(date = day)
        AppLaunch.launch("en-US", timeSource = clock2).use { scenario ->
            compose.waitForIdle()
            compose.touch("settings-button")
            assertEquals("after the relaunch the total is the post-reset 7 s", "0:07", Regex("(\\d+:\\d{2})").findAll(totalText()).last().value)
            assertEquals("no best time is listed", 0, compose.onAllNodesWithTag("settings-best-times").fetchSemanticsNodes().size)
            scenario.advanceActive(compose, clock2, 3_000)
            assertEquals("and only new seconds are added", "0:10", Regex("(\\d+:\\d{2})").findAll(totalText()).last().value)
            compose.touch("settings-close")
        }
        val after = AppStore.open().playTime()
        assertEquals("the relaunch stored the 7 + 3 post-reset seconds and nothing of the erased history", PlayTime(day, 10, 10), after)
    }
}
