package io.github.jamisuni.tangram.settings

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.contracts.progress.GameSettings
import io.github.jamisuni.tangram.contracts.progress.IProgressStore
import io.github.jamisuni.tangram.contracts.progress.PlayTime
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Module-level cover of REQ-032 A2, REQ-009 A1 and REQ-049 A1 (design WO-007 acceptance table): the `SettingsOverlay` composable on its own, in
 * English (the locale is set on the composition's context, never a system setting), with a recording store behind a real `SettingsController`.
 * The app-level twins are `app/.../acceptance/settings/SettingsScreenAppTest` (English and Finnish, through the real app). Plus decision tests
 * for the controls' behaviour (DA-117, DA-120) with no token.
 *
 * The tags are the seam table's: `settings-overlay`, `settings-close`, `settings-sound` (a `Role.Switch` toggleable), `settings-reset`,
 * `settings-reset-question`, `settings-reset-confirm`, `settings-reset-cancel`, `settings-how-to`, `settings-free-note`, `settings-privacy`,
 * `settings-scroll`.
 */
class SettingsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val targetContext: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    // REQ-009 Statement and REQ-049 Rules, verbatim
    private val enFreeNote = "Enjoy, it's absolutely free. No ads, no purchases, no network. Your play time stays on this device."
    private val enPrivacy = "Privacy: this game collects no data. It has no network access, no accounts and no analytics. " +
        "Your puzzle progress and play times are stored only on this device and are deleted when the game is uninstalled."

    private class Store : IProgressStore {
        var resets = 0
        var writes = 0
        private var prefs = GameSettings()
        override fun progress(puzzle: PuzzleId): PuzzleProgress = PuzzleProgress.NEW
        override fun saveProgress(puzzle: PuzzleId, progress: PuzzleProgress) { writes++ }
        override fun playTime(): PlayTime = PlayTime.NONE
        override fun savePlayTime(playTime: PlayTime) { writes++ }
        override fun settings(): GameSettings = prefs
        override fun saveSettings(settings: GameSettings) { writes++; prefs = settings }
        override fun lastShownPuzzle(): PuzzleId? = null
        override fun saveLastShownPuzzle(puzzle: PuzzleId) { writes++ }
        override fun resetAllProgress() { resets++ }
    }

    private val store = Store()
    private val controller = SettingsController(store, { true }) { }

    private fun show(languageTag: String = "en-US", content: @Composable () -> Unit = { SettingsOverlay(controller) }) {
        controller.open()
        compose.setContent {
            val base = LocalContext.current
            val cfg = Configuration(base.resources.configuration).apply { setLocales(LocaleList.forLanguageTags(languageTag)) }
            val ctx = base.createConfigurationContext(cfg)
            CompositionLocalProvider(LocalContext provides ctx, LocalConfiguration provides cfg, LocalResources provides ctx.resources) { content() }
        }
        compose.waitForIdle()
    }

    private fun tap(tag: String) {
        // scroll only when the node sits inside the scroll container: the Done button is in the fixed header (MOVE-DEV7 evidence)
        var cur: SemanticsNode? = compose.onNodeWithTag(tag).fetchSemanticsNode().parent
        while (cur != null) {
            if (cur.config.contains(SemanticsActions.ScrollBy)) {
                compose.onNodeWithTag(tag).performScrollTo()
                break
            }
            cur = cur.parent
        }
        compose.onNodeWithTag(tag).performTouchInput { click() }
        compose.waitForIdle()
    }

    private val interactiveRoles = listOf(Role.Button, Role.Checkbox, Role.Switch, Role.RadioButton, Role.Tab)

    /** The tags of the interactive nodes (click or long-click action or an interactive role) of the merged tree, by own or nearest tagged ancestor. */
    private fun interactiveTags(): Set<String> {
        val out = LinkedHashSet<String>()
        fun tagOf(n: SemanticsNode): String? {
            var cur: SemanticsNode? = n
            while (cur != null) {
                cur.config.getOrNull(SemanticsProperties.TestTag)?.let { return it }
                cur = cur.parent
            }
            return null
        }
        fun walk(n: SemanticsNode) {
            val c = n.config
            val role = c.getOrNull(SemanticsProperties.Role)
            if (c.contains(SemanticsActions.OnClick) || c.contains(SemanticsActions.OnLongClick) || (role != null && role in interactiveRoles)) {
                out += tagOf(n) ?: error("an interactive node has no tagged ancestor: ${n.config}")
            }
            for (k in n.children) walk(k)
        }
        for (r in compose.onAllNodes(isRoot()).fetchSemanticsNodes()) walk(r)
        return out
    }

    private fun textOf(tag: String): String =
        compose.onNodeWithTag(tag).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
            ?: error("the node '$tag' carries no text")

    /** True when a text layout of the node with [tag] is cut (maxLines, an ellipsis, a line wider than its box, a height cut). */
    private fun truncated(tag: String): Boolean {
        var any = false
        compose.onNodeWithTag(tag).performSemanticsActionGetLayout { results ->
            any = results.any { r ->
                r.multiParagraph.didExceedMaxLines ||
                    (0 until r.lineCount).any { i -> r.isLineEllipsized(i) || r.getLineRight(i) - r.getLineLeft(i) > r.layoutInput.constraints.maxWidth + 0.5f } ||
                    r.size.height < r.multiParagraph.height
            }
        }
        return any
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.performSemanticsActionGetLayout(check: (List<androidx.compose.ui.text.TextLayoutResult>) -> Unit) {
        performSemanticsAction(SemanticsActions.GetTextLayoutResult) { read ->
            val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            if (!read(results) || results.isEmpty()) error("no text layout")
            check(results)
        }
    }

    // ---- REQ-032.A2 -------------------------------------------------------------------------------------------------------------

    private val difficultyWhole = listOf("easy", "medium", "hard", "level", "levels")
    private val difficultyPrefix = listOf("difficult", "helppo", "vaikea", "keskitaso", "taso")
    private val allowedWords: Map<String, String> = emptyMap()

    private fun difficultyHits(text: String): List<String> {
        val out = ArrayList<String>()
        for (w in difficultyWhole) if (Regex("(?<!\\p{L})" + Regex.escape(w) + "(?!\\p{L})", RegexOption.IGNORE_CASE).containsMatchIn(text)) out += "$w in \"$text\""
        for (w in difficultyPrefix) for (m in Regex("(?<!\\p{L})" + Regex.escape(w), RegexOption.IGNORE_CASE).findAll(text)) {
            val whole = Regex("\\p{L}+").find(text, m.range.first)?.value ?: m.value
            if (whole.lowercase() !in allowedWords) out += "$w* ($whole) in \"$text\""
        }
        return out
    }

    // REQ-032.A2 - "No difficulty control is present."
    // The interactive nodes are exactly the tagged set (close, timer, sound, reset; then also Erase and Keep): no slider, chip, radio or other
    // control; the must-find controls are present (the check is not blind).
    @Test
    fun req032_A2_theInteractiveNodesAreExactlyTheTaggedSetAndNoDifficultyControlExists() {
        show()
        val base = setOf("settings-close", "settings-timer", "settings-sound", "settings-reset") // WO-008: + the timer switch
        assertEquals("the settings screen's interactive nodes", base, interactiveTags())
        tap("settings-reset")
        val confirming = interactiveTags()
        for (t in listOf("settings-close", "settings-timer", "settings-sound", "settings-reset-confirm", "settings-reset-cancel")) assertTrue("no '$t' while confirming: $confirming", t in confirming)
        val extra = confirming - (base + setOf("settings-reset-confirm", "settings-reset-cancel"))
        assertTrue("a control outside the design's set while confirming: $extra", extra.isEmpty())
    }

    // REQ-032.A2 - and no string of the module matches a difficulty word, in English or Finnish (word-marked, design section 2).
    @Test
    fun req032_A2_noStringOfTheModuleMatchesADifficultyWord() {
        val cls = Class.forName("io.github.jamisuni.tangram.settings.R\$string")
        val hits = ArrayList<String>()
        var seen = 0
        for (lang in listOf("en-US", "fi-FI")) {
            val cfg = Configuration(targetContext.resources.configuration).apply { setLocales(LocaleList.forLanguageTags(lang)) }
            val res = targetContext.createConfigurationContext(cfg).resources
            for (f in cls.declaredFields) {
                if (f.type != Int::class.javaPrimitiveType) continue
                val text = res.getString(f.getInt(null))
                seen++
                for (h in difficultyHits(text)) hits += "$lang ${f.name}: $h"
            }
        }
        assertTrue("fixture: the module has strings (the check would be blind)", seen > 10)
        assertTrue("difficulty words in the settings module:\n" + hits.joinToString("\n"), hits.isEmpty())
    }

    // ---- REQ-009.A1 / REQ-049.A1 ----------------------------------------------------------------------------------------------

    // REQ-009.A1 - "The settings screen shows the note in full."
    // The note's node holds the REQ literal, is displayed after scrolling to it, and is not cut.
    @Test
    fun req009_A1_theFreeNoteIsShownInFull() {
        show()
        compose.onNodeWithTag("settings-free-note").performScrollTo().assertIsDisplayed()
        assertEquals(enFreeNote, textOf("settings-free-note"))
        assertFalse("the free note is cut (ellipsis or line limit)", truncated("settings-free-note"))
    }

    // REQ-049.A1 - "The settings screen shows the privacy text in full, below the free note."
    // The privacy node holds the REQ literal, is displayed after scrolling, is not cut, carries no click action (plain text, REQ-049 rule),
    // and its top is at or below the note's bottom.
    @Test
    fun req049_A1_thePrivacyTextIsShownInFullBelowTheNoteAsPlainText() {
        show()
        compose.onNodeWithTag("settings-free-note").performScrollTo()
        compose.onNodeWithTag("settings-privacy").performScrollTo().assertIsDisplayed()
        assertEquals(enPrivacy, textOf("settings-privacy"))
        assertFalse("the privacy text is cut (ellipsis or line limit)", truncated("settings-privacy"))
        var n: SemanticsNode? = compose.onNodeWithTag("settings-privacy").fetchSemanticsNode()
        val privacy = n!!
        while (n != null) {
            assertFalse("the privacy text is a link or a button: ${n.config}", n.config.contains(SemanticsActions.OnClick) || n.config.contains(SemanticsActions.OnLongClick))
            n = n.parent
        }
        val note = compose.onNodeWithTag("settings-free-note").fetchSemanticsNode()
        assertTrue(
            "the privacy text (top ${privacy.boundsInRoot.top}) is not below the free note (bottom ${note.boundsInRoot.bottom})",
            privacy.boundsInRoot.top >= note.boundsInRoot.bottom - 0.5f,
        )
    }

    // ---- decision tests: the controls drive the controller (no token) -----------------------------------------------------------

    // decision DA-117 (REQ-033 persists in settings; section 4): the sound switch is a Role.Switch toggleable and a tap stores the new value.
    @Test
    fun decisionDA117_theSoundSwitchIsASwitchAndATapStoresTheValue() {
        show()
        val node = compose.onNodeWithTag("settings-sound").fetchSemanticsNode()
        assertEquals(Role.Switch, node.config.getOrNull(SemanticsProperties.Role))
        assertTrue("on by default", controller.soundOn)
        tap("settings-sound")
        assertFalse(controller.soundOn)
        assertFalse("the switch is stored", store.settings().soundOn)
        tap("settings-sound")
        assertTrue(controller.soundOn)
        assertTrue(store.settings().soundOn)
    }

    // decision DA-118 (section 2: "Done" closes): the close control closes and writes nothing.
    @Test
    fun decisionDA118_doneClosesWithoutWriting() {
        show()
        tap("settings-close")
        assertFalse(controller.isOpen)
        assertEquals(0, store.writes)
        assertEquals(0, store.resets)
    }

    // decision DA-120 (section 5): Reset asks first; Keep erases nothing; Erase calls the store's reset once.
    @Test
    fun decisionDA120_resetAsksFirstKeepErasesNothingEraseResetsOnce() {
        show()
        compose.onNodeWithTag("settings-reset-confirm").assertDoesNotExist()
        tap("settings-reset")
        compose.onNodeWithTag("settings-reset-question").assertExists()
        compose.onNodeWithTag("settings-reset-confirm").assertExists()
        assertEquals("asking erased something", 0, store.resets)
        tap("settings-reset-cancel")
        compose.onNodeWithTag("settings-reset-confirm").assertDoesNotExist()
        assertEquals("Keep erased something", 0, store.resets)
        assertEquals(0, store.writes)
        tap("settings-reset")
        tap("settings-reset-confirm")
        assertEquals(1, store.resets)
    }
}
