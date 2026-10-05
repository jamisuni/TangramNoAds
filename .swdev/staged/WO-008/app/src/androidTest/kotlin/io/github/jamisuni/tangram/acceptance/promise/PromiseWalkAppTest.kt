package io.github.jamisuni.tangram.acceptance.promise

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

private class Seen(val lang: String, val screen: WalkScreen, val texts: List<String>, val roots: Int)

/**
 * The promise walk (design WO-006 section 6 item 4, DA-106): for every screen state that exists (New, In progress, Solved, the grid; a phone
 * and a tablet; English and Finnish) it collects every visible text and content description (testing aids, `dev-*`, skipped) and checks
 * them against the forbidden-word lists of [PromiseWords] (a copy of the JVM list, asserted equal by a JVM test). The texts whose keys are
 * in PROMISE_TEXT_KEYS (the REQ-009 free note and the REQ-049 privacy text, WO-007) are exempt by key, nothing else is. The walk fails
 * "blind" when it does not see its canaries. Since WO-007 the walk includes the settings screen and its reset confirmation (C3: the free note,
 * the privacy text and the confirmation's words are scanned, the two texts exempt by key). Since WO-008 (C4) it also visits the timer on a play screen
 * and the settings screen with the timer on and two best times (one over an hour), with the same checks and tokens.
 */
@UsesDisplayRule
@RunWith(Parameterized::class)
class PromiseWalkAppTest(private val spec: DisplaySpec) {
    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun specs(): List<DisplaySpec> = listOf(DisplaySpec.PHONE_390x844, DisplaySpec.TABLET_1280x800)

        /** One walk per spec per process: the three tests read the same observations. */
        private val cache = HashMap<DisplaySpec, List<Seen>>()
    }

    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(DisplayRule(spec)).around(TestConfigRule()).around(ResetStoreRule()).around(compose)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val puzzles = Seed.puzzles

    /** The texts of the keys in PROMISE_TEXT_KEYS in [tag]'s language, to be skipped. */
    private fun exemptTexts(tag: String): Set<String> {
        val b = ScreenWalk.bundle(context, tag)
        return b.values.flatMap { kv -> kv.filterKeys { it in PromiseWords.PROMISE_TEXT_KEYS }.values }.toSet()
    }

    private fun walk(): List<Seen> = cache.getOrPut(spec) {
        val out = ArrayList<Seen>()
        val ids = puzzles.map { it.id.value }
        for (lang in listOf("en-US", "fi-FI")) {
            val bundle = ScreenWalk.bundle(context, lang)
            val browse = bundle.getValue("browse")
            val settings = bundle.getValue("settings")
            for (screen in WalkScreen.entries) {
                Seed.screen(screen)
                AppLaunch.launch(lang).use {
                    Seed.reach(compose, screen)
                    val texts = ArrayList(ScreenWalk.texts(compose))
                    // canaries: the walk must see the texts it exists to read, or it is blind (the settings screen: its title, and the free
                    // note and the privacy text, which are the exempt ones; the confirmation: its question and its two buttons)
                    val canaries = when (screen) {
                        WalkScreen.NEW -> listOf(browse.getValue("state_new"))
                        WalkScreen.IN_PROGRESS -> listOf(browse.getValue("restart"))
                        WalkScreen.SOLVED -> listOf(browse.getValue("retry"))
                        WalkScreen.GRID -> listOf(browse.getValue("all_puzzles"))
                        WalkScreen.SETTINGS -> listOf(settings.getValue("settings_title"), settings.getValue("settings_free_note"), settings.getValue("settings_privacy"))
                        WalkScreen.SETTINGS_RESET_CONFIRM -> listOf(
                            settings.getValue("settings_reset_question"), settings.getValue("settings_reset_confirm"), settings.getValue("settings_reset_cancel"),
                        )
                    }
                    for (canary in canaries) {
                        if (canary !in texts) error("walk blind on $screen ($lang, $spec): the canary text \"$canary\" is not on the screen: $texts")
                    }
                    if (screen == WalkScreen.GRID) {
                        for (c in ScreenWalk.gridDescriptions(compose, ids)) texts += "${c.number}. ${c.title} · ${c.state}"
                        // every library puzzle's cell can be selected: REQ-001 rule 3 (no content is locked)
                        for (id in ids) {
                            compose.onNode(hasClickAction() and hasAnyAncestor(hasTestTag("grid-cell-$id"))).assertExists()
                        }
                    }
                    out += Seen(lang, screen, texts, ScreenWalk.rootCount(compose))
                }
            }
            out += timerScreens(lang)
        }
        out
    }

    /**
     * WO-008 (C4, design "Carried parts"): the same walk over the two screens the timer adds, with the SAME checks and the same tokens. (a) A play
     * screen with the timer on (the pill is composed over the board); (b) the settings screen seeded with the timer on, one best time under one hour
     * and one over (the `h min` format), so the play-time section and the best-time list are on screen. They count as screen states of the walk:
     * both are hit-checked, root-checked and canary-checked like every other, and the walk is blind without its canaries.
     */
    private fun timerScreens(lang: String): List<Seen> {
        val out = ArrayList<Seen>()
        val ids = puzzles
        // (a) a play screen with the timer on
        Seed.screen(WalkScreen.IN_PROGRESS)
        Seed.settings(timerShown = true)
        AppLaunch.launch(lang).use {
            Seed.reach(compose, WalkScreen.IN_PROGRESS)
            if (compose.onAllNodesWithTag("puzzle-timer").fetchSemanticsNodes().isEmpty()) error("walk blind on the timer screen ($lang, $spec): no 'puzzle-timer' with the timer switched on")
            out += Seen(lang, WalkScreen.IN_PROGRESS, ArrayList(ScreenWalk.texts(compose)), ScreenWalk.rootCount(compose))
        }
        // (b) the settings screen: timer on, a best of 59:59 and a best of 1 h 5 min
        val underHour = ids[1]
        val overHour = ids[2]
        Seed.screen(WalkScreen.SETTINGS)
        Seed.settings(timerShown = true)
        Seed.solved(underHour, 4000, 59 * 60 + 59L)
        Seed.solved(overHour, 5000, 3600 + 5 * 60L)
        AppLaunch.launch(lang).use {
            Seed.reach(compose, WalkScreen.SETTINGS)
            val texts = ArrayList(ScreenWalk.texts(compose))
            for (tag in listOf("settings-timer", "settings-play-today", "settings-play-total", "settings-best-times", "settings-best-${underHour.id.value}", "settings-best-${overHour.id.value}")) {
                if (compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()) error("walk blind on the timer settings ($lang, $spec): no node '$tag'")
            }
            for (value in listOf("59:59", "1 h 5 min")) {
                if (texts.none { value in it }) error("walk blind on the timer settings ($lang, $spec): the best time \"$value\" is not on the screen: $texts")
            }
            out += Seen(lang, WalkScreen.SETTINGS, texts, ScreenWalk.rootCount(compose))
        }
        return out
    }

    private fun hitsIn(seen: List<Seen>, only: (String) -> Boolean = { true }): List<String> {
        val out = ArrayList<String>()
        for (s in seen) {
            val exempt = exemptTexts(s.lang)
            for (t in s.texts) {
                if (t in exempt) continue
                for (h in PromiseWords.hits(t)) if (only(h.substringBefore(" ("))) out += "${s.lang} ${s.screen} \"$t\": $h"
            }
        }
        return out
    }

    // REQ-001.A1 - "A full play-through of every screen shows no ad, no price and no request for money."
    // Every screen state, both languages: no word of the forbidden lists and no currency sign or amount in any visible text or
    // description; and no content locked (REQ-001 rule 3): every library puzzle's grid cell has a click action and no text says "locked".
    @Test
    fun req001_A1_noScreenShowsAnAdAPriceOrARequestForMoney() {
        val seen = walk()
        val hits = hitsIn(seen)
        assertTrue("promise words on screen:\n" + hits.joinToString("\n"), hits.isEmpty())
        val locked = Regex("(?i)(?<!\\p{L})(locked|lukittu)")
        val lockedTexts = seen.flatMap { s -> s.texts.filter { locked.containsMatchIn(it) }.map { "${s.lang} ${s.screen} \"$it\"" } }
        assertTrue("texts that say content is locked: $lockedTexts", lockedTexts.isEmpty())
    }

    // REQ-008.A1 - "No ad, price, purchase, tip or donation prompt appears anywhere."
    // The same walk against the whole list (ad, price, purchase, tip and donation words in English and Finnish): nothing on any screen.
    @Test
    fun req008_A1_noAdPricePurchaseTipOrDonationPromptAppears() {
        val seen = walk()
        assertTrue("fixture: the walk saw all ${2 * (WalkScreen.entries.size + 2)} screens (WO-008 adds the two timer screens per language)", seen.size == 2 * (WalkScreen.entries.size + 2))
        val hits = hitsIn(seen)
        assertTrue("promise words on screen:\n" + hits.joinToString("\n"), hits.isEmpty())
    }

    // REQ-008.A2 - "No rating prompt appears."
    // No dialog or popup (an extra compose root) on any screen, and none of the rating words of the list on any screen. The rating words
    // are taken from the shared list, so the check is blind if they are ever removed from it.
    @Test
    fun req008_A2_noRatingPromptAppears() {
        val rating = setOf("rate", "stars", "review", "arvostel", "arvio")
        val known = (PromiseWords.EN + PromiseWords.FI).map { it.text }.toSet()
        assertTrue("rating words missing from the shared list: ${rating - known}", known.containsAll(rating))
        val seen = walk()
        for (s in seen) assertEquals("${s.lang} ${s.screen}: a dialog or popup root is on the screen", 1, s.roots)
        val hits = hitsIn(seen) { it in rating }
        assertTrue("rating words on screen:\n" + hits.joinToString("\n"), hits.isEmpty())
    }
}
