package io.github.jamisuni.tangram.acceptance.settings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.acceptance.ResetStoreRule
import io.github.jamisuni.tangram.acceptance.layout.AppLaunch
import io.github.jamisuni.tangram.acceptance.layout.ManualTimeSource
import io.github.jamisuni.tangram.acceptance.layout.ScreenWalk
import io.github.jamisuni.tangram.acceptance.layout.Seed
import io.github.jamisuni.tangram.acceptance.layout.TestConfigRule
import io.github.jamisuni.tangram.acceptance.layout.WalkScreen
import io.github.jamisuni.tangram.acceptance.layout.advanceActive
import io.github.jamisuni.tangram.acceptance.touch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import java.time.LocalDate

/**
 * App-level cover of REQ-005 A2, REQ-029 A2 (and the midnight and best-list rules as decision tests), design WO-008 Acceptance IDs table and
 * TASK-T8a: the real wiring (keeper, store, settings screen) under a manual clock. Seeded through the real store; every time the screen reads
 * moves only through `advanceActive`, never a sleep (design 8). English and Finnish. The settings rows are read by tag: the row text holds its label
 * and value ("Active today" / "Aktiivinen tänään" and the value `m:ss` or `h min`), so the value is checked with `contains`.
 */
class PlayTimeSettingsAppTest {
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(ResetStoreRule()).around(TestConfigRule()).around(compose)

    private val day = LocalDate.of(2026, 10, 5)
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val puzzles = Seed.puzzles
    private val languages = listOf("en-US", "fi-FI")

    private fun openSettings() {
        compose.touch("settings-button")
        compose.onNodeWithTag("settings-overlay").assertExists()
    }

    private fun row(tag: String): String = ScreenWalk.textOf(compose, tag)

    private fun assertRow(tag: String, value: String, what: String) =
        assertTrue("$what: \"$value\" not in the row ${row(tag)}", value in row(tag))

    // REQ-005.A2 - "The total active play time is available at any time."
    // From a fresh install: the total reads 0:00 on the settings screen; after 7 active seconds it reads 0:07 at once (no wait for a save); and
    // after the activity is closed and launched again (a new ViewModel re-reads the store) it still reads 0:07. English and Finnish.
    @Test
    fun theTotalIsAvailableFromAFreshInstallThroughPlayAndAfterARelaunch() {
        for (lang in languages) {
            Seed.screen(WalkScreen.NEW)
            val clock = ManualTimeSource()
            AppLaunch.launch(lang, timeSource = clock).use { scenario ->
                compose.waitForIdle()
                openSettings()
                assertRow("settings-play-total", "0:00", "$lang fresh install")
                scenario.advanceActive(compose, clock, 7_000)
                assertRow("settings-play-total", "0:07", "$lang after 7 active seconds")
            }
            AppLaunch.launch(lang, timeSource = clock).use {
                compose.waitForIdle()
                openSettings()
                assertRow("settings-play-total", "0:07", "$lang after a relaunch")
            }
        }
    }

    // REQ-029.A2 - "The settings screen shows today's and all-time play time."
    // Seeded play time of today: 3599 s today and 7325 s in total; the screen shows 59:59 and 2 h 2 min (m:ss below one hour, h min from one
    // hour up), and after one more active second today reads 1 h 0 min. English and Finnish.
    @Test
    fun todaysAndAllTimePlayTimeAreShownInTheOneFormat() {
        for (lang in languages) {
            Seed.screen(WalkScreen.NEW)
            Seed.playTime(day, 3_599, 7_325)
            val clock = ManualTimeSource(date = day)
            AppLaunch.launch(lang, timeSource = clock).use { scenario ->
                compose.waitForIdle()
                openSettings()
                assertRow("settings-play-today", "59:59", "$lang today")
                assertRow("settings-play-total", "2 h 2 min", "$lang total")
                scenario.advanceActive(compose, clock, 1_000)
                assertRow("settings-play-today", "1 h 0 min", "$lang today after one second")
                assertRow("settings-play-total", "2 h 2 min", "$lang total after one second")
            }
        }
    }

    // decision DA-139: today restarts at local midnight and the total is kept (REQ-029 rule; F28). The interval that ends after the date changed
    // belongs entirely to the new day.
    @Test
    fun todayRestartsAtMidnightAndTheTotalIsKept() {
        Seed.screen(WalkScreen.NEW)
        Seed.playTime(day, 100, 500)
        val clock = ManualTimeSource(date = day)
        AppLaunch.launch("en-US", timeSource = clock).use { scenario ->
            compose.waitForIdle()
            openSettings()
            assertRow("settings-play-today", "1:40", "before midnight, today")
            assertRow("settings-play-total", "8:20", "before midnight, total")
            scenario.advanceActive(compose, clock, 1_000, newDate = day.plusDays(1))
            assertRow("settings-play-today", "0:01", "after midnight, today restarted")
            assertRow("settings-play-total", "8:21", "after midnight, total kept and counting")
        }
    }

    // decision DA-141 (design 6; REQ-030 rule "the best time is shown ... in the settings list"): one row per puzzle that HAS a best time, in
    // library order, labelled "Best · <title>" / "Paras · <title>"; none for a puzzle never solved or solved only by the aid (best empty); the
    // times in the one format; and a confirmed reset empties the list while the screen stays open (DA-120).
    @Test
    fun theBestTimeListHasOneRowPerBestInLibraryOrderAndEmptiesOnReset() {
        for ((lang, prefix) in listOf("en-US" to "Best · ", "fi-FI" to "Paras · ")) {
            val under = puzzles[1]
            val over = puzzles[2]
            val byAidOnly = puzzles[3]
            Seed.screen(WalkScreen.NEW)
            Seed.solved(over, 5_000, 3_900) // 1 h 5 min, listed AFTER `under` because the library order rules, not the time
            Seed.solved(under, 90, 3_599) // 59:59
            Seed.solved(byAidOnly, 50, null) // solved by the aid: no best time, no row
            AppLaunch.launch(lang, timeSource = ManualTimeSource()).use {
                compose.waitForIdle()
                openSettings()
                compose.onNodeWithTag("settings-best-times").assertExists()
                compose.onNodeWithTag("settings-best-${under.id.value}").assertExists()
                compose.onNodeWithTag("settings-best-${over.id.value}").assertExists()
                compose.onAllNodesWithTag("settings-best-${puzzles[0].id.value}").assertCountEquals(0)
                compose.onAllNodesWithTag("settings-best-${byAidOnly.id.value}").assertCountEquals(0)
                assertTrue("the label of ${under.id.value}: ${row("settings-best-${under.id.value}")}", "$prefix${under.title.inLanguage(lang.take(2))}" in row("settings-best-${under.id.value}"))
                assertRow("settings-best-${under.id.value}", "59:59", "$lang under an hour")
                assertRow("settings-best-${over.id.value}", "1 h 5 min", "$lang over an hour")
                val underTop = compose.onNodeWithTag("settings-best-${under.id.value}").fetchSemanticsNode().boundsInRoot.top
                val overTop = compose.onNodeWithTag("settings-best-${over.id.value}").fetchSemanticsNode().boundsInRoot.top
                assertTrue("library order: ${under.id.value} (index 1) above ${over.id.value} (index 2): $underTop vs $overTop", underTop < overTop)

                // Reset, then Erase: the list empties at once, the screen stays open, today and total read zero
                compose.onNodeWithTag("settings-reset").performScrollTo()
                compose.touch("settings-reset")
                compose.onNodeWithTag("settings-reset-confirm").performScrollTo()
                compose.touch("settings-reset-confirm")
                compose.waitForIdle()
                compose.onNodeWithTag("settings-overlay").assertExists()
                compose.onAllNodesWithTag("settings-best-times").assertCountEquals(0)
                assertEquals("today reads zero after a reset", true, "0:00" in row("settings-play-today"))
                assertEquals("total reads zero after a reset", true, "0:00" in row("settings-play-total"))
            }
        }
    }
}
