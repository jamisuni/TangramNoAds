package io.github.jamisuni.tangram.acceptance

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import io.github.jamisuni.tangram.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/** REQ-002 A2 on the app's screen. */
class NoModeScreenTest {

    private val rule = createAndroidComposeRule<MainActivity>()

    // DA-62 (WO-004): the app persists now, so every launch starts from a wiped store (setup only; no assertion changed).
    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(ResetStoreRule()).around(rule)

    private fun walk(n: SemanticsNode, out: MutableList<SemanticsNode>) {
        out.add(n)
        n.children.forEach { walk(it, out) }
    }

    // REQ-002.A2 - "No screen offers a toddler or picture-matching mode."
    // (REQ-002 rules: "there are no modes for younger children".) Every text and description in the whole composition,
    // unmerged, is checked against mode vocabulary in both shipped languages; the play surface and board must be there
    // so the test is looking at the real screen.
    @Test
    fun req002_A2_noScreenOffersAToddlerOrPictureMatchingMode() {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(200)
        rule.onNodeWithTag("play-area").assertExists()
        rule.onNodeWithTag("board").assertExists()
        val nodes = ArrayList<SemanticsNode>()
        walk(rule.onRoot(useUnmergedTree = true).fetchSemanticsNode(), nodes)
        val words = Regex(
            "toddler|baby|babies|picture.?match|matching|memory game|kids? mode|easy mode|child mode|taapero|vauva|kuvapari|muistipeli|lapsitila",
            RegexOption.IGNORE_CASE,
        )
        for (n in nodes) {
            val texts = (n.config.getOrNull(SemanticsProperties.Text)?.map { it.text } ?: emptyList()) +
                (n.config.getOrNull(SemanticsProperties.ContentDescription) ?: emptyList())
            for (t in texts) assertTrue("a mode-like text on screen: '$t'", !words.containsMatchIn(t))
        }
    }
}
