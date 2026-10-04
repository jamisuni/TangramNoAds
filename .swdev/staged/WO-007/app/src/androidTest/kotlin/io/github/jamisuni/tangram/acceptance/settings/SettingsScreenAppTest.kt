package io.github.jamisuni.tangram.acceptance.settings

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import io.github.jamisuni.tangram.acceptance.ResetStoreRule
import io.github.jamisuni.tangram.acceptance.layout.AppLaunch
import io.github.jamisuni.tangram.acceptance.layout.DisplayRule
import io.github.jamisuni.tangram.acceptance.layout.DisplaySpec
import io.github.jamisuni.tangram.acceptance.layout.PlayerControlWalk
import io.github.jamisuni.tangram.acceptance.layout.ScreenWalk
import io.github.jamisuni.tangram.acceptance.layout.Seed
import io.github.jamisuni.tangram.acceptance.layout.TestConfigRule
import io.github.jamisuni.tangram.acceptance.layout.UsesDisplayRule
import io.github.jamisuni.tangram.acceptance.layout.WalkScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/**
 * The settings screen on the real app, English and Finnish (design WO-007 acceptance table and section 6): REQ-032 A2 (no difficulty
 * control), REQ-009 A1 (the free note in full, only in the settings), REQ-049 A1 (the privacy text in full, plain text, below the note).
 * The module-level twins are `settings/src/androidTest/.../SettingsScreenTest`. The locale comes from the debug seam (`TestConfig`).
 * Everything is reached by real touch (`Seed.reach`: the gear, the reset button); the sheet scrolls, so each text is scrolled to first.
 */
@UsesDisplayRule
@RunWith(Parameterized::class)
class SettingsScreenAppTest(private val spec: DisplaySpec) {
    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun specs(): List<DisplaySpec> = listOf(DisplaySpec.PHONE_390x844, DisplaySpec.TABLET_1280x800)

        // REQ-009 Statement and REQ-049 Rules, verbatim
        private const val EN_FREE_NOTE = "Enjoy, it's absolutely free. No ads, no purchases, no network. Your play time stays on this device."
        private const val EN_PRIVACY = "Privacy: this game collects no data. It has no network access, no accounts and no analytics. " +
            "Your puzzle progress and play times are stored only on this device and are deleted when the game is uninstalled."

        // F14 (accepted at G1): prototype 0.6's Finnish texts, as design WO-007 section 6 quotes them
        private const val FI_FREE_NOTE = "Nauti, se on ihan ilmaista. Ei mainoksia, ei ostoksia, ei verkkoa. Peliaikasi pysyy tällä laitteella."
        private const val FI_PRIVACY = "Tietosuoja: tämä peli ei kerää mitään tietoja. Sillä ei ole verkkoyhteyttä, tilejä eikä analytiikkaa. " +
            "Ratkaisusi ja peliaikasi tallennetaan vain tälle laitteelle, ja ne poistuvat kun peli poistetaan."

        private val NOTE = mapOf("en-US" to EN_FREE_NOTE, "fi-FI" to FI_FREE_NOTE)
        private val PRIVACY = mapOf("en-US" to EN_PRIVACY, "fi-FI" to FI_PRIVACY)

        /** Design section 2 (rev 1, N6): word-marked like PromiseWords. WHOLE words and PREFIXES; ALLOWED_WORDS (empty) takes a legitimate hit by exact word. */
        private val DIFFICULTY_WHOLE = listOf("easy", "medium", "hard", "level", "levels")
        private val DIFFICULTY_PREFIX = listOf("difficult", "helppo", "vaikea", "keskitaso", "taso")
        private val ALLOWED_WORDS: Map<String, String> = emptyMap()

        fun difficultyHits(text: String): List<String> {
            val out = ArrayList<String>()
            for (w in DIFFICULTY_WHOLE) for (m in Regex("(?<!\\p{L})" + Regex.escape(w) + "(?!\\p{L})", RegexOption.IGNORE_CASE).findAll(text)) out += "$w in \"$text\""
            for (w in DIFFICULTY_PREFIX) for (m in Regex("(?<!\\p{L})" + Regex.escape(w), RegexOption.IGNORE_CASE).findAll(text)) {
                val whole = Regex("\\p{L}+").find(text, m.range.first)?.value ?: m.value
                if (whole.lowercase() !in ALLOWED_WORDS) out += "$w* ($whole) in \"$text\""
            }
            return out
        }
    }

    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(DisplayRule(spec)).around(TestConfigRule()).around(ResetStoreRule()).around(compose)

    private val languages = listOf("en-US", "fi-FI")

    /** True when a text layout of the node with [tag] is cut (maxLines, an ellipsis, a line wider than its box, a height cut). */
    private fun truncated(tag: String): Boolean {
        var any = false
        compose.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { read ->
            val results = mutableListOf<TextLayoutResult>()
            if (!read(results) || results.isEmpty()) error("no text layout for '$tag'")
            any = results.any { r ->
                r.multiParagraph.didExceedMaxLines ||
                    (0 until r.lineCount).any { i -> r.isLineEllipsized(i) || r.getLineRight(i) - r.getLineLeft(i) > r.layoutInput.constraints.maxWidth + 0.5f } ||
                    r.size.height < r.multiParagraph.height
            }
        }
        return any
    }

    private fun hasClickInChain(n: SemanticsNode): Boolean {
        var cur: SemanticsNode? = n
        while (cur != null) {
            if (cur.config.contains(SemanticsActions.OnClick) || cur.config.contains(SemanticsActions.OnLongClick)) return true
            cur = cur.parent
        }
        return false
    }

    // REQ-032.A2 - "No difficulty control is present."
    // Both settings screens, both languages: the interactive nodes are exactly the tagged set of the design (close, sound, reset; and while
    // confirming also Erase and Keep), so there is no slider, chip or radio of any kind; and no visible text matches a difficulty word
    // (word-marked, design section 2). The must-find controls are present, so the walk is not blind.
    @Test
    fun req032_A2_noDifficultyControlIsPresentOnEitherSettingsScreen() {
        val allowed = mapOf(
            WalkScreen.SETTINGS to setOf("settings-close", "settings-sound", "settings-reset"),
            WalkScreen.SETTINGS_RESET_CONFIRM to setOf("settings-close", "settings-sound", "settings-reset", "settings-reset-confirm", "settings-reset-cancel"),
        )
        val problems = ArrayList<String>()
        for (lang in languages) for ((screen, tags) in allowed) {
            Seed.screen(screen)
            AppLaunch.launch(lang).use {
                Seed.reach(compose, screen)
                val found = PlayerControlWalk.settingsControls(compose, screen).map { it.tag }.toSet()
                for (t in PlayerControlWalk.mustFind(screen)) if (t !in found) problems += "$lang $screen: walk blind, no control '$t'"
                for (t in found - tags) problems += "$lang $screen: an interactive node '$t' is not in the design's set $tags"
                for (t in ScreenWalk.texts(compose)) for (h in difficultyHits(t)) problems += "$lang $screen: difficulty word $h"
            }
        }
        assertTrue("a difficulty control or word on the settings screens:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    // REQ-032.A2 - control for the word matcher (it can fail, and it does not hit the app's own words).
    @Test
    fun req032_A2_theDifficultyWordMatcherCanFail() {
        for (s in listOf("Easy", "Medium puzzles", "Hard mode", "Level 2", "Levels", "Difficulty", "Choose difficulty", "Helppo", "Vaikeaa", "Keskitaso", "Taso 3")) {
            assertTrue("must hit: \"$s\"", difficultyHits(s).isNotEmpty())
        }
        for (s in listOf("Sound", "Reset all progress", "Keep", "Erase", "Äänet", "Nollaa kaikki edistyminen", "Valmis", "Done", "Placeholder", "hardware", "Medially")) {
            assertTrue("must NOT hit: \"$s\" -> ${difficultyHits(s)}", difficultyHits(s).isEmpty())
        }
    }

    // REQ-009.A1 - "The settings screen shows the note in full."
    // English and Finnish: the note's node holds exactly the REQ text (English) or the accepted Finnish text, is displayed after scrolling
    // to it, and is not cut (no ellipsis, no line limit).
    @Test
    fun req009_A1_theSettingsScreenShowsTheFreeNoteInFull() {
        for (lang in languages) {
            Seed.screen(WalkScreen.SETTINGS)
            AppLaunch.launch(lang).use {
                Seed.reach(compose, WalkScreen.SETTINGS)
                compose.onNodeWithTag("settings-free-note").performScrollTo().assertIsDisplayed()
                assertEquals("$lang: the free note", NOTE.getValue(lang), ScreenWalk.textOf(compose, "settings-free-note"))
                assertFalse("$lang: the free note is cut (ellipsis or line limit)", truncated("settings-free-note"))
            }
        }
    }

    // REQ-009 rule "The note appears in the settings screen only, never over a puzzle": on every play screen and the grid, in both
    // languages, neither the English nor the Finnish note text is anywhere (REQ-009.A1's reading: it is shown in settings and nowhere else).
    @Test
    fun req009_A1_theFreeNoteIsNotOnAnyPlayScreenOrTheGrid() {
        for (lang in languages) for (screen in listOf(WalkScreen.NEW, WalkScreen.IN_PROGRESS, WalkScreen.SOLVED, WalkScreen.GRID)) {
            Seed.screen(screen)
            AppLaunch.launch(lang).use {
                Seed.reach(compose, screen)
                val texts = ScreenWalk.texts(compose)
                assertTrue("fixture: the walk read the $screen screen at all", texts.isNotEmpty())
                for (note in NOTE.values) assertFalse("$lang $screen: the free note is on a play screen: $texts", texts.any { it.contains(note) || it.contains("absolutely free") || it.contains("ihan ilmaista") })
            }
        }
    }

    // REQ-049.A1 - "The settings screen shows the privacy text in full, below the free note."
    // English and Finnish: the node holds exactly the REQ text (or the accepted Finnish text), is displayed after scrolling, is not cut,
    // is plain text (no click action on it or any ancestor, REQ-049 rule), and its top lies at or below the note's bottom.
    @Test
    fun req049_A1_theSettingsScreenShowsThePrivacyTextInFullBelowTheNote() {
        for (lang in languages) {
            Seed.screen(WalkScreen.SETTINGS)
            AppLaunch.launch(lang).use {
                Seed.reach(compose, WalkScreen.SETTINGS)
                compose.onNodeWithTag("settings-free-note").performScrollTo()
                compose.onNodeWithTag("settings-privacy").performScrollTo().assertIsDisplayed()
                assertEquals("$lang: the privacy text", PRIVACY.getValue(lang), ScreenWalk.textOf(compose, "settings-privacy"))
                assertFalse("$lang: the privacy text is cut (ellipsis or line limit)", truncated("settings-privacy"))
                val privacy = compose.onNodeWithTag("settings-privacy").fetchSemanticsNode()
                assertFalse("$lang: the privacy text is a link or a button (it must be plain text)", hasClickInChain(privacy))
                val note = compose.onNodeWithTag("settings-free-note").fetchSemanticsNode()
                assertTrue(
                    "$lang: the privacy text (top ${privacy.boundsInRoot.top}) is not below the free note (bottom ${note.boundsInRoot.bottom})",
                    privacy.boundsInRoot.top >= note.boundsInRoot.bottom - 0.5f,
                )
            }
        }
    }
}
