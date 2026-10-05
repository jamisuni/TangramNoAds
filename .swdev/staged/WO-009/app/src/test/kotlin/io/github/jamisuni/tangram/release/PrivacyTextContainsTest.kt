package io.github.jamisuni.tangram.release

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// WO-009 T9c (independent author), in tree, scaffolding: the decision test of the hosted privacy text. NO acceptance token.
// decision DA-160 (design section 6 "privacy-policy.md" and the seam row; REQ-048 Rules: "both say that the game collects nothing"):
// release/privacy-policy.md has a `## English` block and a `## Suomi` block, and each block CONTAINS the in-app privacy sentence of its
// language, copied from the strings of the settings module (`settings_privacy` in values/ and values-fi/), never retyped. The hosted
// text may carry more than the sentence. White space is not compared (a markdown file wraps lines), and a leading `>` of a quote is ignored.
// Inputs: release/privacy-policy.md (declared test input `releaseDocs` of :app) and settings/src/main/res/values*/strings.xml (an input of
// the app's scan already). A missing file, block or string: error(), never a skip.
class PrivacyTextContainsTest {

    private fun root(): File {
        val p = System.getProperty("repo.root")
        if (p.isNullOrBlank()) error("system property repo.root is not set for :app:test (app/build.gradle.kts passes it)")
        val f = File(p)
        if (!File(f, "settings.gradle.kts").isFile) error("repo.root=$p is not the repository root")
        return f
    }

    private fun squash(s: String): String =
        s.replace("\r\n", "\n").lines().joinToString(" ") { it.trim().removePrefix(">").trim() }.replace(Regex("\\s+"), " ").trim()

    /** The in-app sentence in [qualifier] ("values" or "values-fi"), with the XML entities and aapt's escapes resolved. */
    private fun inAppSentence(qualifier: String): String {
        val f = File(root(), "settings/src/main/res/$qualifier/strings.xml")
        if (!f.isFile) error("${f.path} does not exist")
        val m = Regex("<string\\s+name=\"settings_privacy\"[^>]*>(.*?)</string>", RegexOption.DOT_MATCHES_ALL).find(f.readText())
            ?: error("$qualifier/strings.xml has no settings_privacy string")
        val raw = m.groupValues[1]
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'")
        val sb = StringBuilder()
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c == '\\' && i + 1 < raw.length) {
                when (val d = raw[i + 1]) {
                    'n' -> sb.append(' ')
                    else -> sb.append(d)
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        val s = squash(sb.toString())
        if (s.length < 40) error("the in-app privacy sentence in $qualifier is implausibly short: '$s'")
        return s
    }

    private fun block(md: String, heading: String): String {
        val lines = md.replace("\r\n", "\n").lines()
        val start = lines.indexOfFirst { it.trim() == "## $heading" }
        if (start < 0) error("release/privacy-policy.md has no `## $heading` block")
        val end = (start + 1 until lines.size).firstOrNull { lines[it].startsWith("## ") } ?: lines.size
        return squash(lines.subList(start + 1, end).joinToString("\n"))
    }

    private fun policy(): String {
        val f = File(root(), "release/privacy-policy.md")
        if (!f.isFile) error("release/privacy-policy.md does not exist (TASK-094 delivers it)")
        return f.readText()
    }

    // decision DA-160: the English block contains the English in-app sentence.
    @Test
    fun theEnglishBlockContainsTheInAppSentence() {
        val sentence = inAppSentence("values")
        assertTrue("the `## English` block does not contain the in-app sentence: '$sentence'", block(policy(), "English").contains(sentence))
    }

    // decision DA-160: the Finnish block contains the Finnish in-app sentence.
    @Test
    fun theFinnishBlockContainsTheInAppSentence() {
        val sentence = inAppSentence("values-fi")
        assertTrue("the `## Suomi` block does not contain the in-app sentence: '$sentence'", block(policy(), "Suomi").contains(sentence))
    }

    // decision DA-160 (fixture: the two sentences differ, so each block's check above can tell its language from the other).
    @Test
    fun theTwoInAppSentencesAreDifferentTexts() {
        assertFalse(inAppSentence("values") == inAppSentence("values-fi"))
    }
}
