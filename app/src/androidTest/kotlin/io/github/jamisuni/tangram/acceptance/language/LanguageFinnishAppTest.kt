package io.github.jamisuni.tangram.acceptance.language

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
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
 * The device in Finnish, on the real app (design WO-006 section 5, DA-103): the language walk over every screen state that exists, on the
 * 360 dp phone (REQ-013's size, F9) and the smallest tablet. The locale comes from the debug seam (`TestConfig`), never a system setting.
 * The settings screen and its reset confirmation are part of the walk since WO-007 (C1).
 */
@UsesDisplayRule
@RunWith(Parameterized::class)
class LanguageFinnishAppTest(private val spec: DisplaySpec) {
    companion object {
        // F14 (accepted at G1): prototype 0.6's Finnish REQ-009 and REQ-049 texts, as design WO-007 section 6 quotes them
        private const val FI_FREE_NOTE = "Nauti, se on ihan ilmaista. Ei mainoksia, ei ostoksia, ei verkkoa. Peliaikasi pysyy tällä laitteella."
        private const val FI_PRIVACY = "Tietosuoja: tämä peli ei kerää mitään tietoja. Sillä ei ole verkkoyhteyttä, tilejä eikä analytiikkaa. " +
            "Ratkaisusi ja peliaikasi tallennetaan vain tälle laitteelle, ja ne poistuvat kun peli poistetaan."

        @JvmStatic
        @Parameters(name = "{0}")
        fun specs(): List<DisplaySpec> = listOf(DisplaySpec.PHONE_360x780, DisplaySpec.TABLET_600x960)
    }

    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(DisplayRule(spec)).around(TestConfigRule()).around(ResetStoreRule()).around(compose)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val fi = ScreenWalk.bundle(context, "fi-FI")
    private val en = ScreenWalk.bundle(context, "en-US")
    private val puzzles = Seed.puzzles
    private fun titles(lang: String): Set<String> = puzzles.map { it.title.inLanguage(lang) }.toSet()

    // REQ-047.A1 - "With the device in Finnish, the top bar, the settings screen and the buttons show Finnish; the Cat puzzle is titled 'Kissa'."
    // (the settings screen is in the walk since WO-007, C1: WalkScreen.SETTINGS and SETTINGS_RESET_CONFIRM) Every screen state of the Cat puzzle: every visible text and content description is
    // a Finnish value, a Finnish puzzle title or a neutral token, none is an English value whose Finnish value differs; composite strings
    // are checked recursively; the top bar says "Kissa"; the buttons are the Finnish ones of the resources.
    @Test
    fun req047_A1_everyScreenStateOfTheCatPuzzleIsInFinnish() {
        val cat = Seed.fullPuzzle()
        val ids = puzzles.map { it.id.value }
        val problems = ArrayList<String>()
        for (screen in WalkScreen.entries) {
            Seed.screen(screen, cat)
            AppLaunch.launch("fi-FI").use {
                Seed.reach(compose, screen)
                val texts = ArrayList(ScreenWalk.texts(compose))

                // the settings sheet covers the top bar (its semantics are cleared while the sheet is open): the title is read on the others
                if (!screen.isSettings) assertEquals("$screen: the Cat puzzle is titled Kissa in the top bar", "Kissa", ScreenWalk.textOf(compose, "puzzle-title"))
                val browse = fi.getValue("browse")
                val settings = fi.getValue("settings")
                val required = when (screen) {
                    WalkScreen.NEW -> listOf(browse.getValue("state_new"))
                    WalkScreen.IN_PROGRESS -> listOf(browse.getValue("restart"), browse.getValue("state_in_progress"))
                    WalkScreen.SOLVED -> listOf(browse.getValue("retry"), browse.getValue("next"), browse.getValue("state_solved"))
                    WalkScreen.GRID -> listOf(browse.getValue("all_puzzles"), browse.getValue("done"))
                    // the settings canaries: the title, the Finnish labels of design section 6, the two Finnish texts in full (C1)
                    WalkScreen.SETTINGS -> listOf(
                        settings.getValue("settings_title"), settings.getValue("settings_done"), settings.getValue("settings_sound"),
                        settings.getValue("settings_reset"), settings.getValue("settings_free_note"), settings.getValue("settings_privacy"),
                        "Äänet", "Nollaa kaikki edistyminen", FI_FREE_NOTE, FI_PRIVACY,
                    )
                    WalkScreen.SETTINGS_RESET_CONFIRM -> listOf(
                        settings.getValue("settings_reset_question"), settings.getValue("settings_reset_confirm"),
                        settings.getValue("settings_reset_cancel"), "Poistetaanko varmasti kaikki ratkaisut ja ajat?", "Poista", "Säilytä",
                    )
                }
                for (r in required) if (r !in texts) problems += "$screen: the Finnish text \"$r\" is not on the screen (walk blind or not translated)"

                if (screen == WalkScreen.GRID) {
                    for (c in ScreenWalk.gridDescriptions(compose, ids)) {
                        texts += "${c.number}. ${c.title} · ${c.state}"
                        val expected = puzzles[c.number - 1].title.inLanguage("fi")
                        if (c.title != expected) problems += "grid cell ${c.number}: title \"${c.title}\", wanted the Finnish \"$expected\""
                        if (c.id == cat.id.value) assertEquals("the Cat's grid cell is titled Kissa", "Kissa", c.title)
                    }
                }
                problems += ScreenWalk.problems(texts, "fi", fi, en) { lang -> titles(lang) }.map { "$screen: $it" }
            }
        }
        assertTrue("not everything is Finnish:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    // REQ-047.A1 - the settings screen itself, on its own (C1; design WO-007 section 7): in Finnish it carries the Finnish labels
    // "Äänet" and "Nollaa kaikki edistyminen", the two Finnish texts IN FULL (F14, the prototype's), the play screens' gear is described
    // "Asetukset", and the free note and the privacy text are the Finnish values, not the English ones.
    @Test
    fun req047_A1_theSettingsScreenIsInFinnish() {
        val settings = fi.getValue("settings")
        val enSettings = en.getValue("settings")
        Seed.screen(WalkScreen.NEW)
        AppLaunch.launch("fi-FI").use {
            compose.waitForIdle()
            assertEquals("the gear is described in Finnish", "Asetukset", settings.getValue("settings_open_description"))
            assertTrue("the gear's Finnish description is not on the play screen: ${ScreenWalk.texts(compose)}", "Asetukset" in ScreenWalk.texts(compose))
            Seed.reach(compose, WalkScreen.SETTINGS)
            val texts = ScreenWalk.texts(compose)
            for (r in listOf("Äänet", "Nollaa kaikki edistyminen", FI_FREE_NOTE, FI_PRIVACY)) assertTrue("the Finnish text \"$r\" is not on the settings screen: $texts", r in texts)
            assertEquals(FI_FREE_NOTE, ScreenWalk.textOf(compose, "settings-free-note"))
            assertEquals(FI_PRIVACY, ScreenWalk.textOf(compose, "settings-privacy"))
            for (key in listOf("settings_free_note", "settings_privacy", "settings_sound", "settings_reset")) {
                assertTrue("$key has no Finnish value of its own", settings.getValue(key) != enSettings.getValue(key))
                assertTrue("the English $key is on the Finnish settings screen", enSettings.getValue(key) !in texts)
            }
        }
    }

    /** True when any text layout of the node with [tag] overflows its width or height (clipped or ellipsised). */
    private fun overflows(tag: String): Boolean {
        var any = false
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { read ->
            val results = mutableListOf<TextLayoutResult>()
            if (!read(results) || results.isEmpty()) error("no text layout for '$tag'")
            // NOT hasVisualOverflow / didOverflowWidth: for wrap-content text the layout's paragraph width is the available width, so
            // size.width < paragraph width is true for EVERY text, "9 / 13" in English included (device evidence, MOVE-DEV6). A real clip is
            // text cut by maxLines, an ellipsised line, a line wider than the available width, or a height cut.
            any = results.any { r ->
                r.multiParagraph.didExceedMaxLines ||
                    (0 until r.lineCount).any { i -> r.isLineEllipsized(i) || r.getLineRight(i) - r.getLineLeft(i) > r.layoutInput.constraints.maxWidth + 0.5f } ||
                    r.size.height < r.multiParagraph.height
            }
        }
        return any
    }

    // decision DA-103: the Finnish labelled controls and state texts report no overflow at font scale 1.0 (design section 5: 360 dp is the
    // size that matters; the smallest tablet is wider, so asserting there cannot fail where 360 passes). The top bar's fixed height is
    // included: its title and state text must lie inside it. There is no no-clip claim at font scale 2.0 (no requirement states one).
    @Test
    fun decisionDA103_finnishControlTextDoesNotClipAtFontScale1() {
        val clipped = ArrayList<String>()
        val perScreen = mapOf(
            WalkScreen.IN_PROGRESS to listOf("restart-button", "puzzle-state", "puzzle-counter"),
            WalkScreen.SOLVED to listOf("retry-button", "solved-next-button", "best-time", "puzzle-state", "puzzle-counter"),
        )
        for ((screen, tags) in perScreen) {
            Seed.screen(screen)
            AppLaunch.launch("fi-FI").use {
                compose.waitForIdle()
                for (t in tags) if (overflows(t)) clipped += "$screen: the text of '$t' overflows"
                val bar = compose.onNodeWithTag("top-bar").fetchSemanticsNode().boundsInRoot
                for (t in listOf("puzzle-title", "puzzle-state")) {
                    val b = compose.onNodeWithTag(t).fetchSemanticsNode().boundsInRoot
                    if (b.top < bar.top - 0.5f || b.bottom > bar.bottom + 0.5f) clipped += "$screen: '$t' $b lies outside the top bar $bar"
                }
            }
        }
        assertTrue("clipped Finnish control text:\n" + clipped.joinToString("\n"), clipped.isEmpty())
    }
}
