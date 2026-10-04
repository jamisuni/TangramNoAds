package io.github.jamisuni.tangram.acceptance.layout

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode

// Scaffolding for the WO-006 language and promise walks (decision DA-103 / DA-106, design WO-006 sections 5 and 6); no requirement
// token. Collects every visible text and content description of the merged semantics tree (testing aids, `dev-*`, skipped) and
// compares them with the module resources of each language. Composite strings (counters, descriptions) are checked RECURSIVELY.

internal class GridCell(val id: String, val number: Int, val title: String, val state: String)

internal object ScreenWalk {
    /** Module name -> its generated R class (`R$string` holds the keys). The devtools texts are not walked. */
    private val MODULE_R = listOf(
        "browse" to "io.github.jamisuni.tangram.browse.R",
        "play" to "io.github.jamisuni.tangram.play.R",
        "app" to "io.github.jamisuni.tangram.R",
        "settings" to "io.github.jamisuni.tangram.settings.R", // WO-007 (design section 7: the bundle reads `settings` beside the others)
    )

    // ---- what is on the screen ------------------------------------------------------------------------------------------------

    /** Every text and content description of the merged tree of every compose root, `dev-*` skipped, distinct, in tree order. */
    fun texts(rule: ComposeTestRule): List<String> {
        val out = ArrayList<String>()
        for (root in rule.onAllNodes(isRoot()).fetchSemanticsNodes()) collect(root, out)
        return out.distinct()
    }

    private fun collect(n: SemanticsNode, out: MutableList<String>) {
        val tag = n.config.getOrNull(SemanticsProperties.TestTag)
        if (tag != null && tag.startsWith("dev-")) return
        n.config.getOrNull(SemanticsProperties.Text)?.forEach { out += it.text }
        n.config.getOrNull(SemanticsProperties.ContentDescription)?.forEach { out += it }
        for (c in n.children) collect(c, out)
    }

    /** How many compose roots the screen has (a dialog or popup adds one). */
    fun rootCount(rule: ComposeTestRule): Int = rule.onAllNodes(isRoot()).fetchSemanticsNodes().size

    /** The text of the node with [tag] (its texts joined), loud when it has none. */
    fun textOf(rule: ComposeTestRule, tag: String): String {
        val list = rule.onNodeWithTag(tag).fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)
            ?: error("the node '$tag' carries no text")
        return list.joinToString("") { it.text }
    }

    /**
     * The grid cell descriptions `%1$d. %2$s · %3$s` of every puzzle in [ids], split into their captures. The grid is lazy (one cell per
     * puzzle, composed on demand), so it is scrolled to every cell first. The click layer under the tagged cell carries the description.
     */
    fun gridDescriptions(rule: ComposeTestRule, ids: List<String>): List<GridCell> {
        val re = Regex("^(\\d+)\\. (.+) · (.+)$")
        val out = ArrayList<GridCell>()
        for (id in ids) {
            val tag = "grid-cell-$id"
            if (rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()) {
                rule.onNode(hasScrollAction()).performScrollToNode(hasTestTag(tag))
                rule.waitForIdle()
            }
            val click = rule.onNode(hasClickAction() and hasAnyAncestor(hasTestTag(tag))).fetchSemanticsNode()
            val d = click.config.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
                ?: error("the click layer of $tag has no content description")
            val m = re.matchEntire(d) ?: error("grid cell description '$d' does not look like '%1\$d. %2\$s · %3\$s'")
            out += GridCell(id, m.groupValues[1].toInt(), m.groupValues[2], m.groupValues[3])
        }
        return out
    }

    // ---- what the resources say -----------------------------------------------------------------------------------------------

    /** module -> key -> value of every string key of `browse`, `play`, `app` and `settings`, resolved under [languageTag]. Raw templates, not formatted. */
    fun bundle(context: Context, languageTag: String): Map<String, Map<String, String>> {
        val cfg = Configuration(context.resources.configuration)
        cfg.setLocales(LocaleList.forLanguageTags(languageTag))
        val ctx = context.createConfigurationContext(cfg)
        val out = LinkedHashMap<String, Map<String, String>>()
        for ((module, rClass) in MODULE_R) {
            val cls = Class.forName("$rClass\$string")
            val values = LinkedHashMap<String, String>()
            for (f in cls.declaredFields) {
                if (f.type != Int::class.javaPrimitiveType) continue
                val id = ctx.resources.getIdentifier(f.name, "string", ctx.packageName)
                if (id == 0) error("string resource '${f.name}' of module $module not found in package ${ctx.packageName}")
                values[f.name] = ctx.getString(id)
            }
            if (values.isEmpty()) error("module $module has no string keys: the language walk would be blind")
            out[module] = values
        }
        return out
    }

    // ---- the comparison -------------------------------------------------------------------------------------------------------

    private val FORMAT_SPEC = Regex("%(?:\\d+\\$)?[-#+ 0,(]*\\d*(?:\\.\\d+)?([a-zA-Z])")

    /** Language-neutral tokens (design section 5): digits and the counter, dashes, punctuation, the arrow glyphs. */
    private val NEUTRAL = Regex("^[\\d\\s/:.,–\\-‹›]*$")

    /** The captures when [text] is an instance of [template] (a format string), [emptyList] for an exact match, null otherwise. */
    private fun capturesOf(text: String, template: String): List<Pair<Char, String>>? {
        val specs = FORMAT_SPEC.findAll(template).toList()
        if (specs.isEmpty()) return if (text == template) emptyList() else null
        val sb = StringBuilder("^")
        var last = 0
        val kinds = ArrayList<Char>()
        for (m in specs) {
            sb.append(Regex.escape(template.substring(last, m.range.first)))
            val k = m.groupValues[1][0]
            kinds += k
            sb.append(if (k == 'd') "(\\d+)" else "(.+?)")
            last = m.range.last + 1
        }
        sb.append(Regex.escape(template.substring(last))).append("$")
        val m = Regex(sb.toString()).matchEntire(text) ?: return null
        return kinds.mapIndexed { i, k -> k to m.groupValues[i + 1] }
    }

    private class Entry(val module: String, val key: String, val value: String)

    private fun flatten(b: Map<String, Map<String, String>>) = b.flatMap { (m, kv) -> kv.map { Entry(m, it.key, it.value) } }

    /**
     * The problems in [texts] for a screen in [lang] ("fi" or "en"), empty when the screen is entirely in that language (design
     * section 5). A text is explained when it is the own-language value of some key, a puzzle title in [lang], a neutral token, or an
     * instance of an own-language template whose every `%s` capture is itself explained (recursion, N3). It is a problem when it is
     * not explained, or when it equals a value of the OTHER language whose own-language value differs (a leaked string).
     * [fi] and [en] are the bundles; [titles] maps a language to the puzzle titles of the library.
     */
    fun problems(
        texts: List<String>,
        lang: String,
        fi: Map<String, Map<String, String>>,
        en: Map<String, Map<String, String>>,
        titles: (String) -> Set<String>,
    ): List<String> {
        val own = flatten(if (lang == "fi") fi else en)
        val other = flatten(if (lang == "fi") en else fi)
        val ownTitles = titles(lang)
        val otherTitles = titles(if (lang == "fi") "en" else "fi")

        fun explained(s: String, depth: Int): Boolean {
            if (NEUTRAL.matches(s) || s in ownTitles) return true
            if (depth > 4) return false
            for (e in own) {
                val caps = capturesOf(s, e.value) ?: continue
                if (caps.all { (k, v) -> k == 'd' || explained(v, depth + 1) }) return true
            }
            return false
        }

        fun leaked(s: String): String? {
            if (s in otherTitles && s !in ownTitles) return "a puzzle title of the other language"
            for (o in other) {
                if (capturesOf(s, o.value) == null) continue
                val mine = own.firstOrNull { it.module == o.module && it.key == o.key }?.value ?: continue
                if (mine != o.value && capturesOf(s, mine) == null) return "the other language's value of ${o.module}:${o.key}"
            }
            return null
        }

        val out = ArrayList<String>()
        for (s in texts) {
            leaked(s)?.let { out += "\"$s\" is $it" }
            if (!explained(s, 0)) out += "\"$s\" is not a $lang string, a $lang title or a neutral token"
        }
        return out
    }
}
