package io.github.jamisuni.tangram.play.acceptance

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.play.PlaySession
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * REQ-002 A2 at play level (code-home covering test; the app carries the same token on its own screen).
 * The play area is the whole play surface, so "no screen offers a toddler or picture-matching mode" means here: the
 * composition `PlayArea` shows offers no mode choice of any kind.
 */
class NoModeAcceptanceTest {

    @get:Rule
    val rule = createComposeRule()

    private val modeWords = Regex(
        "toddler|baby|babies|picture.?match|matching|memory game|kids? mode|easy mode|child mode|\\bmode\\b|" +
            "taapero|vauva|kuvapari|muistipeli|lapsitila|\\btila\\b",
        RegexOption.IGNORE_CASE,
    )

    private fun walk(n: SemanticsNode, chain: List<SemanticsNode>, out: MutableList<List<SemanticsNode>>) {
        val here = chain + n
        out.add(here)
        n.children.forEach { walk(it, here, out) }
    }

    private fun tagOf(n: SemanticsNode): String? = n.config.getOrNull(SemanticsProperties.TestTag)

    /** Every string resource of the play module, in English and in Finnish (both shipped languages). */
    private fun allPlayStrings(): List<String> {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val fields = Class.forName("io.github.jamisuni.tangram.play.R\$string").fields
        check(fields.isNotEmpty()) { "seam: play R.string has no entries" }
        return listOf("en", "fi").flatMap { lang ->
            val cfg = android.content.res.Configuration(ctx.resources.configuration).also { it.setLocale(Locale(lang)) }
            val res = ctx.createConfigurationContext(cfg).resources
            fields.filter { it.type == Int::class.javaPrimitiveType }.map { res.getString(it.getInt(null)) }
        }
    }

    // REQ-002.A2 - "No screen offers a toddler or picture-matching mode."
    // (REQ-002 rules: "there are no modes for younger children"; REQ-013 rules: a tray cell holds nothing but its piece,
    // its size mark and the flip badge, "the former button strip is gone".) For a full-set puzzle (the one with the most
    // controls): nothing in the composition is clickable except the flip badge, the only text is the size marks (and the badge), and no text,
    // description or string resource of the play module, in English or Finnish, offers a mode.
    @Test
    fun req002_A2_thePlayAreaOffersNoToddlerOrPictureMatchingMode() {
        val id = "shapes-square"
        rule.showPlay(PlaySession(puzzle(id), { true }))
        rule.onNodeWithTag("play-area").assertExists()
        rule.onNodeWithTag("board").assertExists()
        val all = ArrayList<List<SemanticsNode>>()
        walk(rule.onRoot(useUnmergedTree = true).fetchSemanticsNode(), emptyList(), all)
        assertTrue("the composition has nodes", all.size > 2)
        for (chain in all) {
            val n = chain.last()
            val inBadge = chain.any { tagOf(it) == "flip-badge" }
            val inMark = chain.any { tagOf(it)?.startsWith("size-mark-") == true }
            if (n.config.contains(SemanticsActions.OnClick) && !inBadge) {
                assertFalse("$id: a clickable node other than the flip badge: tag ${tagOf(n)}", true)
            }
            val texts = n.config.getOrNull(SemanticsProperties.Text)?.map { it.text } ?: emptyList()
            for (t in texts) {
                assertTrue("$id: text '$t' outside the size marks and the flip badge", inMark || inBadge)
                assertFalse("$id: a mode-like text on screen: '$t'", modeWords.containsMatchIn(t))
            }
            for (d in n.config.getOrNull(SemanticsProperties.ContentDescription) ?: emptyList()) {
                assertFalse("$id: a mode-like description on screen: '$d'", modeWords.containsMatchIn(d))
            }
        }
        for (s in allPlayStrings()) assertFalse("a mode-like string in the play module: '$s'", modeWords.containsMatchIn(s))
    }
}
