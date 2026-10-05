package io.github.jamisuni.tangram.settings

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.contracts.progress.GameSettings
import io.github.jamisuni.tangram.contracts.progress.IProgressStore
import io.github.jamisuni.tangram.contracts.progress.PlayTime
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Module-level cover of REQ-029 A2 under the code home `settings` (design WO-008 Acceptance IDs table): the `SettingsOverlay` on its own with a
 * `PlayTimeReadout` fake behind a real `SettingsController` (`settings` never depends on `time`, G-06). Seam row: `interface PlayTimeReadout { val
 * todaySeconds: Long; val totalSeconds: Long; companion object { val NONE } }`; rows `settings-play-today` ("Active today" / "Aktiivinen
 * tänään") and `settings-play-total` ("Active in total" / "Aktiivinen yhteensä") under the heading "Play time (kept on this device only)" /
 * "Peliaika (vain tällä laitteella)", values through `DurationFormat.parts`, "one format everywhere": m:ss below one hour, "h min" from one hour
 * (REQ-029 rule). The locale is set on the composition's context (the pattern of `SettingsScreenTest`).
 */
class SettingsPlayTimeTest {
    @get:Rule
    val compose = createComposeRule()

    private class Store : IProgressStore {
        override fun progress(puzzle: PuzzleId): PuzzleProgress = PuzzleProgress.NEW
        override fun saveProgress(puzzle: PuzzleId, progress: PuzzleProgress) = Unit
        override fun playTime(): PlayTime = PlayTime.NONE
        override fun savePlayTime(playTime: PlayTime) = Unit
        override fun settings(): GameSettings = GameSettings()
        override fun saveSettings(settings: GameSettings) = Unit
        override fun lastShownPuzzle(): PuzzleId? = null
        override fun saveLastShownPuzzle(puzzle: PuzzleId) = Unit
        override fun resetAllProgress() = Unit
    }

    /** A readout whose values the test moves; snapshot-backed like the app's (design 6: "the values are snapshot-backed"). */
    private class FakeReadout(today: Long, total: Long) : PlayTimeReadout {
        override var todaySeconds: Long by mutableLongStateOf(today)
        override var totalSeconds: Long by mutableLongStateOf(total)
    }

    private fun show(readout: PlayTimeReadout, languageTag: String = "en-US") {
        val controller = SettingsController(Store(), readout = readout, onReset = {})
        controller.open()
        compose.setContent {
            val base = LocalContext.current
            val cfg = Configuration(base.resources.configuration).apply { setLocales(LocaleList.forLanguageTags(languageTag)) }
            val ctx = base.createConfigurationContext(cfg)
            CompositionLocalProvider(LocalContext provides ctx, LocalConfiguration provides cfg, LocalResources provides ctx.resources) {
                SettingsOverlay(controller)
            }
        }
        compose.waitForIdle()
    }

    private fun textOf(tag: String): String =
        compose.onNodeWithTag(tag).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
            ?: error("the node '$tag' carries no text")

    private fun allTexts(): List<String> {
        val out = ArrayList<String>()
        fun walk(n: SemanticsNode) {
            n.config.getOrNull(SemanticsProperties.Text)?.forEach { out += it.text }
            for (c in n.children) walk(c)
        }
        for (r in compose.onAllNodes(isRoot()).fetchSemanticsNodes()) walk(r)
        return out
    }

    // REQ-029.A2 - "The settings screen shows today's and all-time play time."
    // Below one hour both rows read m:ss from the readout's values: 125 s is 2:05 and 3599 s is 59:59.
    @Test
    fun todaysAndAllTimePlayTimeAreShownAsMinutesAndSecondsBelowAnHour() {
        show(FakeReadout(today = 125, total = 3_599))
        assertTrue("today's row: ${textOf("settings-play-today")}", "2:05" in textOf("settings-play-today"))
        assertTrue("the all-time row: ${textOf("settings-play-total")}", "59:59" in textOf("settings-play-total"))
    }

    // REQ-029.A2 - from one hour up, hours and minutes: 3600 s is 1 h 0 min and 7325 s is 2 h 2 min; and the rows follow the readout.
    @Test
    fun fromOneHourUpTheyAreShownAsHoursAndMinutesAndFollowTheReadout() {
        val readout = FakeReadout(today = 3_600, total = 7_325)
        show(readout)
        assertTrue("today's row: ${textOf("settings-play-today")}", "1 h 0 min" in textOf("settings-play-today"))
        assertTrue("the all-time row: ${textOf("settings-play-total")}", "2 h 2 min" in textOf("settings-play-total"))
        compose.runOnIdle { readout.todaySeconds = 59; readout.totalSeconds = 61 }
        compose.waitForIdle()
        assertTrue("today's row follows the readout: ${textOf("settings-play-today")}", "0:59" in textOf("settings-play-today"))
        assertTrue("the all-time row follows the readout: ${textOf("settings-play-total")}", "1:01" in textOf("settings-play-total"))
    }

    // REQ-029.A2 - on a fresh install both read 0:00 (design 6, REQ-005's total readable at any time), with no best list yet
    @Test
    fun aFreshInstallShowsZeroForBothAndNoBestTimeList() {
        show(PlayTimeReadout.NONE)
        assertTrue("today's row: ${textOf("settings-play-today")}", "0:00" in textOf("settings-play-today"))
        assertTrue("the all-time row: ${textOf("settings-play-total")}", "0:00" in textOf("settings-play-total"))
    }

    // decision DA-141: the labels are the prototype's strings (F14), in English and in Finnish, under the heading; no best-time list while no
    // puzzle has a best time (REQ-030 rule: the best time is in the settings list; design 6: "none shown when there is none").
    @Test
    fun theSectionHasItsEnglishLabelsAndNoBestListWhenThereIsNoBest() {
        show(FakeReadout(0, 0))
        val texts = allTexts()
        for (label in listOf("Play time (kept on this device only)", "Active today", "Active in total")) assertTrue("no \"$label\" on the screen: $texts", texts.any { label in it })
        compose.onAllNodesWithTag("settings-best-times").assertCountEquals(0)
    }

    @Test
    fun theSectionHasItsFinnishLabels() {
        show(FakeReadout(0, 0), languageTag = "fi-FI")
        val texts = allTexts()
        for (label in listOf("Peliaika (vain tällä laitteella)", "Aktiivinen tänään", "Aktiivinen yhteensä")) assertTrue("no \"$label\" on the screen: $texts", texts.any { label in it })
    }

    // decision DA-141: REQ-032's order (DA-124): timer, sound, play time, reset, top to bottom
    @Test
    fun theRowsAreInRequirementOrder() {
        show(FakeReadout(0, 0))
        fun top(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.top
        val order = listOf("settings-timer", "settings-sound", "settings-play-today", "settings-play-total", "settings-reset")
        val tops = order.map { top(it) }
        assertTrue("the order $order has tops $tops", tops.zipWithNext().all { (a, b) -> a < b })
    }
}
