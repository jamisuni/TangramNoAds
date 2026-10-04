package io.github.jamisuni.tangram.acceptance.language

import android.util.Log
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.acceptance.ResetStoreRule
import io.github.jamisuni.tangram.acceptance.layout.AppLaunch
import io.github.jamisuni.tangram.acceptance.layout.ScreenWalk
import io.github.jamisuni.tangram.acceptance.layout.Seed
import io.github.jamisuni.tangram.acceptance.layout.TestConfigRule
import io.github.jamisuni.tangram.acceptance.layout.WalkScreen
import io.github.jamisuni.tangram.TestConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

// decision DA-101 (and decision F6): DECISION TEST, no requirement token, run on today's code BEFORE anything is changed (design WO-006
// section 5). Runs on the device's own display. For three language LISTS it launches the real app on an In-progress Cat puzzle and
// asserts ONE language across the screen: the language of the top-bar title equals the language of the Restart label. It does not say
// which language each list must give; it RECORDS it (log tag WO006ListDecision, one line per list) for the orchestrator:
//   sv-SE,fi-FI  the APK ships the libraries' values-sv, so the platform resolves sv and the whole UI is English (title too);
//   fi-FI,sv-SE  the control: Finnish;
//   se-NO,fi-FI  Northern Sami is packaged by no library, so it is the only first entry Android resolves PAST to fi: every string
//                Finnish; the title is Finnish only if the platform moves the resolved locale to locales[0]. THIS list decides L-2:
//                a mismatch here (Finnish buttons over an English title) means the ui_language contingency (task 046) is needed.
class LanguageListDecisionAppTest {
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(ResetStoreRule()).around(TestConfigRule()).around(compose)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val browseEn = ScreenWalk.bundle(context, "en-US").getValue("browse")
    private val browseFi = ScreenWalk.bundle(context, "fi-FI").getValue("browse")
    private val cat = Seed.fullPuzzle()

    private fun languageOfTitle(title: String): String = when (title) {
        cat.title.inLanguage("en") -> "en"
        cat.title.inLanguage("fi") -> "fi"
        else -> error("the top-bar title '$title' is neither the English nor the Finnish title of the Cat")
    }

    private fun languageOfRestart(label: String): String = when (label) {
        browseEn.getValue("restart") -> "en"
        browseFi.getValue("restart") -> "fi"
        else -> error("the Restart label '$label' is neither the English nor the Finnish one")
    }

    private fun record(list: String) {
        Seed.screen(WalkScreen.IN_PROGRESS, cat)
        AppLaunch.launch(list).use {
            compose.waitForIdle()
            val title = ScreenWalk.textOf(compose, "puzzle-title")
            val restart = ScreenWalk.textOf(compose, "restart-button")
            val t = languageOfTitle(title)
            val r = languageOfRestart(restart)
            val line = "list=$list title=$t($title) restart=$r($restart)"
            Log.i("WO006ListDecision", line)
            println("WO006ListDecision $line")
            assertEquals("two languages on one screen for the list $list: $line", t, r)
        }
    }

    @Test fun decisionDA101_swedishThenFinnishGivesOneLanguage() = record("sv-SE,fi-FI")

    @Test fun decisionDA101_finnishThenSwedishControlGivesOneLanguage() = record("fi-FI,sv-SE")

    @Test fun decisionDA101_northernSamiThenFinnishGivesOneLanguage() = record("se-NO,fi-FI")

    // decision F6: a change of language takes effect when the screen is recreated and changes no puzzle state.
    @Test
    fun decisionF6_recreatingWithAnotherLanguageChangesTheTextAndKeepsThePuzzleState() {
        Seed.screen(WalkScreen.IN_PROGRESS, cat)
        AppLaunch.launch("fi-FI").use { scenario ->
            compose.waitForIdle()
            assertEquals("fi", languageOfRestart(ScreenWalk.textOf(compose, "restart-button")))
            val counter = ScreenWalk.textOf(compose, "puzzle-counter")

            TestConfig.languageTags = "en-US"
            scenario.recreate()
            compose.waitForIdle()

            assertEquals("en", languageOfRestart(ScreenWalk.textOf(compose, "restart-button")))
            assertEquals("en", languageOfTitle(ScreenWalk.textOf(compose, "puzzle-title")))
            assertEquals("the puzzle counter changed", counter, ScreenWalk.textOf(compose, "puzzle-counter"))
            assertTrue("the puzzle is no longer in progress", ScreenWalk.texts(compose).contains(browseEn.getValue("state_in_progress")))
            compose.onNodeWithTag("solved-bar").assertDoesNotExist()
        }
    }
}
