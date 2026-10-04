package io.github.jamisuni.tangram.acceptance.held

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * HELD-OUT (Test & Verify only): a device in Swedish (design WO-006 section 5, DA-103). Own kit copies (package `...acceptance.held`).
 * The locale comes from the debug seam (`TestConfig`), never a system setting. The device's own display is used. The settings screen, the
 * free note and the privacy text are carried to WO-007 (C2).
 */
class HeldLanguageSwedishAppTest {
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
        val problems = ArrayList<String>()
        for (screen in WalkScreen.entries) {
            Seed.screen(screen, cat)
            AppLaunch.launch("sv-SE").use {
                if (screen == WalkScreen.GRID) compose.touch("puzzle-counter")
                compose.waitForIdle()
                val texts = ArrayList(ScreenWalk.texts(compose))

                assertEquals("$screen: the Cat is titled in English in the top bar", cat.title.inLanguage("en"), ScreenWalk.textOf(compose, "puzzle-title"))
                val required = when (screen) {
                    WalkScreen.NEW -> listOf(browse.getValue("state_new"))
                    WalkScreen.IN_PROGRESS -> listOf(browse.getValue("restart"), browse.getValue("state_in_progress"))
                    WalkScreen.SOLVED -> listOf(browse.getValue("retry"), browse.getValue("next"), browse.getValue("state_solved"))
                    WalkScreen.GRID -> listOf(browse.getValue("all_puzzles"), browse.getValue("done"))
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
