package io.github.jamisuni.tangram.acceptance.held

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * A VISIBLE test since WO-007 (it was held out in WO-006; the in-tree `acceptance/held` kit copies are no longer withheld): a device in Swedish (design WO-006 section 5, DA-103). Own kit copies (package `...acceptance.held`).
 * The locale comes from the debug seam (`TestConfig`), never a system setting. The device's own display is used. The settings screen, its
 * reset confirmation, the free note and the privacy text are in the walk since WO-007 (C2).
 */
class HeldLanguageSwedishAppTest {
    private companion object {
        const val EN_FREE_NOTE = "Enjoy, it's absolutely free. No ads, no purchases, no network. Your play time stays on this device."
        const val EN_PRIVACY = "Privacy: this game collects no data. It has no network access, no accounts and no analytics. " +
            "Your puzzle progress and play times are stored only on this device and are deleted when the game is uninstalled."
    }

    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(ResetStoreRule()).around(TestConfigRule()).around(compose)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val fi = ScreenWalk.bundle(context, "fi-FI")
    private val en = ScreenWalk.bundle(context, "en-US")
    private val puzzles = Seed.puzzles
    private fun titles(lang: String): Set<String> = puzzles.map { it.title.inLanguage(lang) }.toSet()

    // REQ-047.A2 - "With the device in Swedish, everything is in English."
    // Every screen state of the Cat puzzle under sv-SE: every visible text and content description is an English value, an English puzzle
    // title or a neutral token, none is a Finnish value whose English value differs; the top bar and every grid cell carry the English
    // title; the English buttons are on the screens that have them.
    @Test
    fun req047_A2_everyScreenStateIsInEnglishWithTheDeviceInSwedish() {
        val cat = Seed.fullPuzzle()
        val ids = puzzles.map { it.id.value }
        val browse = en.getValue("browse")
        val settings = en.getValue("settings")
        val problems = ArrayList<String>()
        for (screen in WalkScreen.entries) {
            Seed.screen(screen, cat)
            AppLaunch.launch("sv-SE").use {
                Seed.reach(compose, screen)
                val texts = ArrayList(ScreenWalk.texts(compose))

                // the settings sheet covers the top bar (its semantics are cleared while the sheet is open)
                if (!screen.isSettings) assertEquals("$screen: the Cat is titled in English in the top bar", cat.title.inLanguage("en"), ScreenWalk.textOf(compose, "puzzle-title"))
                val required = when (screen) {
                    WalkScreen.NEW -> listOf(browse.getValue("state_new"))
                    WalkScreen.IN_PROGRESS -> listOf(browse.getValue("restart"), browse.getValue("state_in_progress"))
                    WalkScreen.SOLVED -> listOf(browse.getValue("retry"), browse.getValue("next"), browse.getValue("state_solved"))
                    WalkScreen.GRID -> listOf(browse.getValue("all_puzzles"), browse.getValue("done"))
                    // REQ-009 and REQ-049 Statement/Rules, verbatim: the two English texts in full, and the English labels
                    WalkScreen.SETTINGS -> listOf(
                        settings.getValue("settings_title"), settings.getValue("settings_done"), settings.getValue("settings_sound"),
                        settings.getValue("settings_reset"), EN_FREE_NOTE, EN_PRIVACY,
                    )
                    WalkScreen.SETTINGS_RESET_CONFIRM -> listOf(
                        settings.getValue("settings_reset_question"), settings.getValue("settings_reset_confirm"), settings.getValue("settings_reset_cancel"),
                    )
                }
                for (r in required) if (r !in texts) problems += "$screen: the English text \"$r\" is not on the screen (walk blind)"

                if (screen == WalkScreen.GRID) {
                    for (c in ScreenWalk.gridDescriptions(compose, ids)) {
                        texts += "${c.number}. ${c.title} · ${c.state}"
                        val expected = puzzles[c.number - 1].title.inLanguage("en")
                        if (c.title != expected) problems += "grid cell ${c.number}: title \"${c.title}\", wanted the English \"$expected\""
                    }
                }
                problems += ScreenWalk.problems(texts, "en", fi, en) { lang -> titles(lang) }.map { "$screen: $it" }
            }
        }
        assertTrue("not everything is English under sv-SE:\n" + problems.joinToString("\n"), problems.isEmpty())
    }
}
