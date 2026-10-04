package io.github.jamisuni.tangram.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * decision DA-121 (design WO-007 section 6, "Verbatim is enforced, not hoped for"): the English free note and privacy text in
 * `values/strings.xml` equal the locked REQ texts character for character, the Finnish values equal the F14 texts accepted at G1. No
 * acceptance token: the on-screen A1 criteria are covered on the device; this pins the resources.
 *
 * Pinned rules, each from the design:
 *   1. Extraction: REQ-009's text is the single double-quoted span in the `## Statement` section; REQ-049's is the span after
 *      `in full: ` in the `## Rules` section. Exactly one match each, else error().
 *   2. Unescape: the test reads values/strings.xml without aapt, so it unescapes exactly as aapt does (\' \" \\ \n \t \uXXXX, and
 *      the XML entities, which the XML parser decodes), and asserts no run of two white-space characters and no leading or
 *      trailing white space in the resource text (aapt collapses them).
 *   3. The English resource equals the extracted text; the Finnish values are pinned here as literals (the prototype 0.6 `fi` lines the
 *      design quotes, with the `<b>` tags dropped).
 * Inputs: `Requirements/reqs/REQ-009.md` and `REQ-049.md`, declared as the unit-test input `lockedReqTexts` (read-only access), and the
 * module's two strings.xml files; the root comes from `repo.root`.
 */
class LockedTextsTest {

    private val reqNote = "Requirements/reqs/REQ-009.md"
    private val reqPrivacy = "Requirements/reqs/REQ-049.md"
    private val stringsEn = "settings/src/main/res/values/strings.xml"
    private val stringsFi = "settings/src/main/res/values-fi/strings.xml"

    // The design's section 6 states the English verbatim; the extraction below must reproduce it from the REQ files, or the rule is wrong.
    private val englishNote = "Enjoy, it's absolutely free. No ads, no purchases, no network. Your play time stays on this device."
    private val englishPrivacy = "Privacy: this game collects no data. It has no network access, no accounts and no analytics. " +
        "Your puzzle progress and play times are stored only on this device and are deleted when the game is uninstalled."

    // F14 (accepted at G1): prototype 0.6's Finnish strings, verbatim, as the design quotes them.
    private val finnishNote = "Nauti, se on ihan ilmaista. Ei mainoksia, ei ostoksia, ei verkkoa. Peliaikasi pysyy tällä laitteella."
    private val finnishPrivacy = "Tietosuoja: tämä peli ei kerää mitään tietoja. Sillä ei ole verkkoyhteyttä, tilejä eikä analytiikkaa. " +
        "Ratkaisusi ja peliaikasi tallennetaan vain tälle laitteelle, ja ne poistuvat kun peli poistetaan."

    // ---- extraction and unescape (pure, so the controls below can fail them) --------------------------------------------------

    internal fun section(md: String, heading: String): String {
        val lines = md.replace("\r\n", "\n").lines()
        val start = lines.indexOfFirst { it.trim() == "## $heading" }
        if (start < 0) error("no `## $heading` section")
        val end = (start + 1 until lines.size).firstOrNull { lines[it].startsWith("## ") } ?: lines.size
        return lines.subList(start + 1, end).joinToString("\n")
    }

    /** REQ-009: the single double-quoted span of the Statement. Exactly one, else error(). */
    internal fun extractNote(md: String): String {
        val spans = Regex("\"([^\"]*)\"").findAll(section(md, "Statement")).map { it.groupValues[1] }.toList()
        if (spans.size != 1) error("expected exactly one double-quoted span in the Statement, found ${spans.size}: $spans")
        return spans.single()
    }

    /** REQ-049: the span after `in full: ` in the Rules. Exactly one, else error(). */
    internal fun extractPrivacy(md: String): String {
        val spans = Regex("in full: \"([^\"]*)\"").findAll(section(md, "Rules")).map { it.groupValues[1] }.toList()
        if (spans.size != 1) error("expected exactly one `in full: \"...\"` span in the Rules, found ${spans.size}: $spans")
        return spans.single()
    }

    /** aapt's backslash escapes, left to right in one pass (so `\\'` is a backslash and then a quote, never a quote). */
    internal fun aaptUnescape(raw: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c != '\\' || i + 1 >= raw.length) {
                sb.append(c)
                i++
                continue
            }
            when (val d = raw[i + 1]) {
                'n' -> { sb.append('\n'); i += 2 }
                't' -> { sb.append('\t'); i += 2 }
                'u' -> {
                    val hex = raw.substring(i + 2, minOf(raw.length, i + 6))
                    if (hex.length != 4 || !hex.all { it in "0123456789abcdefABCDEF" }) error("bad \\u escape in `$raw`")
                    sb.append(hex.toInt(16).toChar())
                    i += 6
                }
                else -> { sb.append(d); i += 2 } // \' \" \\ \@ \? and, as aapt does, any other escaped character as itself
            }
        }
        return sb.toString()
    }

    private class Res(val raw: String, val hasChildElements: Boolean)

    private fun readStrings(file: File): Map<String, Res> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val out = LinkedHashMap<String, Res>()
        val kids = doc.documentElement.childNodes
        for (i in 0 until kids.length) {
            val n = kids.item(i) as? Element ?: continue
            if (n.tagName != "string") continue
            var child = false
            for (j in 0 until n.childNodes.length) if (n.childNodes.item(j) is Element) child = true
            val name = n.getAttribute("name")
            if (name in out) error("${file.name}: key $name is declared twice")
            out[name] = Res(n.textContent, child)
        }
        return out
    }

    private fun resourceText(rel: String, key: String): String {
        val res = readStrings(RepoFiles.file(rel))[key] ?: error("$rel has no string `$key`")
        assertFalse("$rel $key: split into runs or markup (design: one plain string each)", res.hasChildElements)
        val raw = res.raw
        assertFalse("$rel $key: leading or trailing white space (aapt trims it)", raw != raw.trim())
        assertFalse("$rel $key: a run of two white-space characters (aapt collapses it)", Regex("\\s{2,}").containsMatchIn(raw))
        return aaptUnescape(raw)
    }

    // ---- the pins -------------------------------------------------------------------------------------------------------------

    // decision DA-121 (rule 1): the extraction reproduces the design's verbatim English from the REQ files, so a changed REQ or a wrong
    // rule shows here before it reaches the string comparison.
    @Test
    fun decisionDA121_theExtractionFindsTheLockedTexts() {
        assertEquals(englishNote, extractNote(RepoFiles.file(reqNote).readText()))
        assertEquals(englishPrivacy, extractPrivacy(RepoFiles.file(reqPrivacy).readText()))
    }

    // decision DA-121 (rule 3): the English resources equal the REQ texts character for character (straight apostrophe included).
    @Test
    fun decisionDA121_theEnglishResourcesEqualTheLockedRequirementTexts() {
        assertEquals(extractNote(RepoFiles.file(reqNote).readText()), resourceText(stringsEn, "settings_free_note"))
        assertEquals(extractPrivacy(RepoFiles.file(reqPrivacy).readText()), resourceText(stringsEn, "settings_privacy"))
    }

    // decision DA-121 (rule 3, F14): the Finnish values are the prototype's, verbatim.
    @Test
    fun decisionDA121_theFinnishResourcesAreTheAcceptedPrototypeTexts() {
        assertEquals(finnishNote, resourceText(stringsFi, "settings_free_note"))
        assertEquals(finnishPrivacy, resourceText(stringsFi, "settings_privacy"))
    }

    // ---- controls: the rules must be able to fail -----------------------------------------------------------------------------

    @Test
    fun decisionDA121_theExtractionRulesCanFail() {
        val one = "## Statement\nWHEN x, the game SHALL show: \"A b.\"\n\n## Rationale\nsaid \"later\"\n"
        assertEquals("A b.", extractNote(one))
        assertTrue("two spans is an error, not a pick", fails { extractNote("## Statement\n\"a\" and \"b\"\n## Rules\n") })
        assertTrue("no span is an error", fails { extractNote("## Statement\nno quotes here\n## Rules\n") })
        assertTrue("no section is an error", fails { extractNote("## Rules\n\"a\"\n") })
        val rules = "## Rules\n- The text says, in full: \"P q.\" *(note)*\n- Plain text.\n## Acceptance\n"
        assertEquals("P q.", extractPrivacy(rules))
        assertTrue("two in-full spans is an error", fails { extractPrivacy("## Rules\nin full: \"a\"\nin full: \"b\"\n## Acceptance\n") })
        assertTrue("no in-full span is an error", fails { extractPrivacy("## Rules\n\"a\"\n## Acceptance\n") })
        assertEquals("a span outside the section is not counted", "P q.", extractPrivacy("## Statement\nin full: \"x\"\n$rules"))
    }

    @Test
    fun decisionDA121_theUnescapeIsAaptExact() {
        assertEquals("it's", aaptUnescape("it\\'s"))
        assertEquals("say \"x\"", aaptUnescape("say \\\"x\\\""))
        assertEquals("a\\b", aaptUnescape("a\\\\b"))
        assertEquals("a\nb\tc", aaptUnescape("a\\nb\\tc"))
        assertEquals("ä", aaptUnescape("\\u00e4"))
        assertEquals("an escaped backslash then an escaped quote: a backslash and a quote", "\\'", aaptUnescape("\\\\\\'"))
        assertEquals("one pass: an escaped backslash is not re-read", "\\n", aaptUnescape("\\\\n"))
        assertEquals("plain text is untouched", "No ads.", aaptUnescape("No ads."))
    }

    private fun fails(block: () -> Unit): Boolean = try {
        block()
        false
    } catch (e: IllegalStateException) {
        true
    }
}
