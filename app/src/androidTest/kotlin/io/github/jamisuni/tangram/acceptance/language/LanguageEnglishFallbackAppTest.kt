package io.github.jamisuni.tangram.acceptance.language

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.acceptance.ResetStoreRule
import io.github.jamisuni.tangram.acceptance.layout.AppLaunch
import io.github.jamisuni.tangram.acceptance.layout.DisplayRule
import io.github.jamisuni.tangram.acceptance.layout.DisplaySpec
import io.github.jamisuni.tangram.acceptance.layout.ScreenWalk
import io.github.jamisuni.tangram.acceptance.layout.Seed
import io.github.jamisuni.tangram.acceptance.layout.TestConfigRule
import io.github.jamisuni.tangram.acceptance.layout.UsesDisplayRule
import io.github.jamisuni.tangram.acceptance.layout.WalkScreen
import io.github.jamisuni.tangram.acceptance.layout.isSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/**
 * The English fallback on the real app (WO-006 C2, created here by WO-007; design WO-007 section 7): with the device in Swedish (`sv-SE`,
 * a language the game does not ship) every screen state, the settings screen and its reset confirmation included, is English: the
 * free note and the privacy text are the REQ-009 and REQ-049 English texts in full. The locale comes from the debug seam (`TestConfig`),
 * never a system setting. The in-tree `HeldLanguageSwedishAppTest` walks the same states on the device's own display.
 */
@UsesDisplayRule
@RunWith(Parameterized::class)
class LanguageEnglishFallbackAppTest(private val spec: DisplaySpec) {
    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun specs(): List<DisplaySpec> = listOf(DisplaySpec.PHONE_390x844, DisplaySpec.TABLET_1280x800)

        // REQ-009 Statement and REQ-049 Rules, verbatim
        private const val EN_FREE_NOTE = "Enjoy, it's absolutely free. No ads, no purchases, no network. Your play time stays on this device."
        private const val EN_PRIVACY = "Privacy: this game collects no data. It has no network access, no accounts and no analytics. " +
            "Your puzzle progress and play times are stored only on this device and are deleted when the game is uninstalled."
    }

    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(DisplayRule(spec)).around(TestConfigRule()).around(ResetStoreRule()).around(compose)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val fi = ScreenWalk.bundle(context, "fi-FI")
    private val en = ScreenWalk.bundle(context, "en-US")
    private val puzzles = Seed.puzzles
    private fun titles(lang: String): Set<String> = puzzles.map { it.title.inLanguage(lang) }.toSet()

    // REQ-047.A2 - "With the device in Swedish, everything is in English."
    // Every screen state of the Cat puzzle under sv-SE, the settings screen and its reset confirmation among them: every visible text and
    // content description is an English value, an English puzzle title or a neutral token, none is a Finnish value whose English value
    // differs; the top bar and every grid cell carry the English title; the English buttons and the two English texts in full are on
    // the screens that have them.
    @Test
    fun req047_A2_everyScreenStateIncludingSettingsIsInEnglishWithTheDeviceInSwedish() {
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
                if (!screen.isSettings) {
                    assertEquals("$screen: the Cat is titled in English in the top bar", cat.title.inLanguage("en"), ScreenWalk.textOf(compose, "puzzle-title"))
                }
                val required = when (screen) {
                    WalkScreen.NEW -> listOf(browse.getValue("state_new"), settings.getValue("settings_open_description"))
                    WalkScreen.IN_PROGRESS -> listOf(browse.getValue("restart"), browse.getValue("state_in_progress"))
                    WalkScreen.SOLVED -> listOf(browse.getValue("retry"), browse.getValue("next"), browse.getValue("state_solved"))
                    WalkScreen.GRID -> listOf(browse.getValue("all_puzzles"), browse.getValue("done"))
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
        assertTrue("not everything is English under sv-SE on $spec:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    // REQ-047.A2 - the two REQ texts themselves, read from their own nodes: under sv-SE the free note and the privacy text are the English
    // values of the resources (never the Finnish ones) and the REQ literals.
    @Test
    fun req047_A2_theFreeNoteAndThePrivacyTextAreTheEnglishTextsUnderSwedish() {
        Seed.screen(WalkScreen.SETTINGS)
        AppLaunch.launch("sv-SE").use {
            Seed.reach(compose, WalkScreen.SETTINGS)
            assertEquals(EN_FREE_NOTE, ScreenWalk.textOf(compose, "settings-free-note"))
            assertEquals(EN_PRIVACY, ScreenWalk.textOf(compose, "settings-privacy"))
            assertEquals(EN_FREE_NOTE, en.getValue("settings").getValue("settings_free_note"))
            assertTrue("the Finnish note is on the screen", fi.getValue("settings").getValue("settings_free_note") !in ScreenWalk.texts(compose))
        }
    }
}
