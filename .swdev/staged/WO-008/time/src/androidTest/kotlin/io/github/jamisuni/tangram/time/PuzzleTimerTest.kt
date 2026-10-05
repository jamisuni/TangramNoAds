package io.github.jamisuni.tangram.time

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import io.github.jamisuni.tangram.contracts.progress.GameSettings
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

/**
 * Module-level cover of REQ-031 A1 and A2 under the code home `time` (design WO-008 Acceptance IDs table): the `PuzzleTimer` composable on its own.
 * Seam row: `@Composable fun PuzzleTimer(shown: Boolean, seconds: Long, modifier: Modifier = Modifier)`: renders nothing when `!shown`; else a pill,
 * tag `puzzle-timer`, text from `DurationFormat.parts` and the two strings, no click action, no pointer input. The locale is set on the
 * composition's context, never a system setting (the pattern of `SettingsScreenTest`).
 */
class PuzzleTimerTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(languageTag: String = "en-US", content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent {
            val base = LocalContext.current
            val cfg = Configuration(base.resources.configuration).apply { setLocales(LocaleList.forLanguageTags(languageTag)) }
            val ctx = base.createConfigurationContext(cfg)
            CompositionLocalProvider(LocalContext provides ctx, LocalConfiguration provides cfg, LocalResources provides ctx.resources) { content() }
        }
        compose.waitForIdle()
    }

    // REQ-031.A1 - "On a fresh install, no timer is shown."
    // Module reading: the setting's default is off (the fresh-install value of `GameSettings`), and with `shown = false` the composable renders no
    // `puzzle-timer` node, whatever seconds it is given.
    @Test
    fun aFreshInstallShowsNoTimer() {
        assertFalse("the fresh-install setting is off", GameSettings().timerShown)
        show {
            PuzzleTimer(shown = GameSettings().timerShown, seconds = 5)
            PuzzleTimer(shown = false, seconds = 3_599)
        }
        compose.onAllNodesWithTag("puzzle-timer").assertCountEquals(0)
    }

    // REQ-031.A2 - "With the setting on, the timer is shown and counts active seconds only."
    // Module reading: with `shown = true` the pill is there and shows the seconds it is given (what the keeper counts; "active seconds only"
    // is proved by the keeper's own tests, design table), and it takes no touch: it has no click action.
    @Test
    fun withTheSettingOnThePillShowsTheGivenSecondsAndIsInert() {
        var seconds by mutableLongStateOf(5)
        show { PuzzleTimer(shown = true, seconds = seconds) }
        compose.onNodeWithTag("puzzle-timer").assertExists().assertTextEquals("0:05").assertHasNoClickAction()
        compose.runOnIdle { seconds = 17 }
        compose.waitForIdle()
        compose.onNodeWithTag("puzzle-timer").assertTextEquals("0:17") // it follows the value it is given
    }

    // decision DA-135: one time format everywhere (REQ-029 rule): m:ss below one hour, "h min" from one hour; the same in Finnish.
    @Test
    fun thePillUsesTheOneTimeFormatInEnglish() = checkFormat("en-US")

    @Test
    fun thePillUsesTheOneTimeFormatInFinnish() = checkFormat("fi-FI")

    private fun checkFormat(tag: String) {
        var seconds by mutableLongStateOf(0)
        show(tag) { PuzzleTimer(shown = true, seconds = seconds) }
        for ((value, text) in listOf(0L to "0:00", 59L to "0:59", 60L to "1:00", 3_599L to "59:59", 3_600L to "1 h 0 min", 3_660L to "1 h 1 min", 39_540L to "10 h 59 min")) {
            compose.runOnIdle { seconds = value }
            compose.waitForIdle()
            compose.onNodeWithTag("puzzle-timer").assertTextEquals(text)
        }
    }
}
