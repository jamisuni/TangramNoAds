package io.github.jamisuni.tangram.time

import io.github.jamisuni.tangram.promise.RepoScan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// decision DA-147: DECISION / SCAFFOLDING TEST (WO-008 CR-7 N2, DA-150), no requirement token. Design 7 (rev 2, E3): the pill is placed from a TEMPLATE
// width, "the width of the widest text the pill can show in the current language and font scale (`88 h 59 min`, measured once in the shared
// SubcomposeLayout)", so the pill never moves within an attempt. `play` measures that template (PlayArea.kt) and `time` draws the pill
// (PuzzleTimer.kt); they are different modules (G-06) and nothing but this scan keeps their numbers in step: a pill whose real text is larger than the
// template's text, or padded wider, could outgrow its placement and overlap Restart or the DEV pill.
// Reads through `repo.root` (loud when missing; `app`'s unit-test task declares */src/main/** as inputs, so a change re-runs it). Comments are skipped.
class TimerTemplateScanTest {
    private val root: File = RepoScan.repoRoot()

    /** What PlayArea.kt says about its template. */
    internal class Template(val text: String, val sp: Double, val padDp: Double, val tnum: Boolean, val styleUsesTemplateSp: Boolean)

    /** What PuzzleTimer.kt says about the pill. */
    internal class Pill(val fontSp: Double, val featureSettings: String, val horizontalPaddingDp: Double)

    private fun strip(src: String): String = src.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ").replace(Regex("//[^\\n]*"), " ")

    private fun number(src: String, pattern: String, what: String): Double =
        Regex(pattern).find(src)?.groupValues?.get(1)?.toDouble() ?: error("$what: not found by /$pattern/ (the scan would be blind)")

    internal fun parseTemplate(source: String): Template {
        val src = strip(source)
        val text = Regex("const\\s+val\\s+TIMER_TEMPLATE_TEXT\\s*=\\s*\"([^\"]*)\"").find(src)?.groupValues?.get(1) ?: error("TIMER_TEMPLATE_TEXT not found in PlayArea.kt")
        val sp = number(src, "const\\s+val\\s+TIMER_TEMPLATE_SP\\s*=\\s*(\\d+(?:\\.\\d+)?)", "TIMER_TEMPLATE_SP")
        val pad = number(src, "const\\s+val\\s+TIMER_TEMPLATE_PAD_DP\\s*=\\s*(\\d+(?:\\.\\d+)?)", "TIMER_TEMPLATE_PAD_DP")
        val call = Regex("BasicText\\(\\s*TIMER_TEMPLATE_TEXT\\s*,[^\\n]*").find(src)?.value ?: error("the template measurement `BasicText(TIMER_TEMPLATE_TEXT, ...)` is not in PlayArea.kt")
        return Template(text, sp, pad, Regex("fontFeatureSettings\\s*=\\s*\"tnum\"").containsMatchIn(call), Regex("fontSize\\s*=\\s*TIMER_TEMPLATE_SP\\.sp").containsMatchIn(call))
    }

    internal fun parsePill(source: String): Pill {
        val src = strip(source)
        val sp = number(src, "fontSize\\s*=\\s*(\\d+(?:\\.\\d+)?)\\.sp", "the pill's fontSize")
        val feature = Regex("fontFeatureSettings\\s*=\\s*\"([^\"]*)\"").find(src)?.groupValues?.get(1) ?: error("the pill sets no fontFeatureSettings")
        val pad = number(src, "padding\\(\\s*horizontal\\s*=\\s*(\\d+(?:\\.\\d+)?)\\.dp", "the pill's horizontal padding")
        return Pill(sp, feature, pad)
    }

    /** The widest value the pill can show: the hours format at its widest digits (88 h 59 min, design 7). */
    private fun widestFormat(hoursMinutesFormat: String): String = hoursMinutesFormat.replace("%1\$d", "88").replace("%2\$d", "59")

    /** The disagreements between the template and the pill; empty = they match. */
    internal fun problems(t: Template, p: Pill, hoursMinutesFormat: String): List<String> {
        val out = ArrayList<String>()
        if (!t.styleUsesTemplateSp) out += "the template is not measured at fontSize = TIMER_TEMPLATE_SP.sp"
        if (p.fontSp != t.sp) out += "the pill's font size is ${p.fontSp} sp, the template measures ${t.sp} sp"
        if (!t.tnum) out += "the template text style has no tnum feature"
        if (p.featureSettings != "tnum") out += "the pill's fontFeatureSettings is \"${p.featureSettings}\", not tnum"
        if (2 * p.horizontalPaddingDp != t.padDp) out += "twice the pill's horizontal padding is ${2 * p.horizontalPaddingDp} dp, the template reserves ${t.padDp} dp"
        val widest = widestFormat(hoursMinutesFormat)
        if (t.text != widest) out += "the template text is \"${t.text}\", the widest value the pill can show is \"$widest\""
        if (t.text.length <= "59:59".length) out += "the template text \"${t.text}\" is not wider than an m:ss value"
        return out
    }

    private fun read(rel: String): String {
        val f = File(root, rel)
        if (!f.isFile) error("$rel does not exist under $root: the scan would be blind")
        return f.readText()
    }

    private fun hoursMinutesFormat(): String {
        val xml = read("time/src/main/res/values/strings.xml")
        val raw = Regex("<string name=\"time_hours_minutes\">([^<]*)</string>").find(xml)?.groupValues?.get(1) ?: error("time_hours_minutes not found in time/src/main/res/values/strings.xml")
        return raw.replace("\\'", "'")
    }

    // decision DA-147: the real files agree.
    @Test
    fun thePillAndItsPlacementTemplateAgreeOnSizeFeatureAndPadding() {
        val template = parseTemplate(read("play/src/main/kotlin/io/github/jamisuni/tangram/play/PlayArea.kt"))
        val pill = parsePill(read("time/src/main/kotlin/io/github/jamisuni/tangram/time/PuzzleTimer.kt"))
        assertEquals("the design's numbers (a font size of 14 sp and 20 dp of padding in total)", listOf(14.0, 20.0), listOf(template.sp, template.padDp))
        val problems = problems(template, pill, hoursMinutesFormat())
        assertTrue("the pill and the template of its placement disagree (DA-147): $problems", problems.isEmpty())
    }

    // decision DA-147 (positive control): the checker rejects each kind of mismatch, and accepts the matching pair.
    @Test
    fun theCheckerCanFail() {
        val format = "%1\$d h %2\$d min"
        val goodT = Template("88 h 59 min", 14.0, 20.0, tnum = true, styleUsesTemplateSp = true)
        val goodP = Pill(14.0, "tnum", 10.0)
        assertTrue(problems(goodT, goodP, format).isEmpty())
        assertTrue("a different font size", problems(goodT, Pill(15.0, "tnum", 10.0), format).isNotEmpty())
        assertTrue("another feature", problems(goodT, Pill(14.0, "liga", 10.0), format).isNotEmpty())
        assertTrue("a wider padding", problems(goodT, Pill(14.0, "tnum", 12.0), format).isNotEmpty())
        assertTrue("a narrower padding", problems(goodT, Pill(14.0, "tnum", 8.0), format).isNotEmpty())
        assertTrue("a template without tnum", problems(Template("88 h 59 min", 14.0, 20.0, tnum = false, styleUsesTemplateSp = true), goodP, format).isNotEmpty())
        assertTrue("a template not measured at its own size", problems(Template("88 h 59 min", 14.0, 20.0, tnum = true, styleUsesTemplateSp = false), goodP, format).isNotEmpty())
        assertTrue("a template text that is not the widest", problems(Template("8 h 9 min", 14.0, 20.0, tnum = true, styleUsesTemplateSp = true), goodP, format).isNotEmpty())
        assertTrue("an m:ss template", problems(Template("59:59", 14.0, 20.0, tnum = true, styleUsesTemplateSp = true), goodP, format).isNotEmpty())
        // the parsers read what the sources say, and fail loudly on what they cannot find
        val pill = parsePill("BasicText(text, style = TextStyle(fontSize = 15.sp, fontFeatureSettings = \"tnum\")) // 14.sp\nModifier.padding(horizontal = 9.dp, vertical = 4.dp)")
        assertEquals(15.0, pill.fontSp, 0.0)
        assertEquals(9.0, pill.horizontalPaddingDp, 0.0)
        var failed = false
        try { parsePill("nothing here") } catch (e: IllegalStateException) { failed = true }
        assertTrue("a missing value is a loud error, never a pass", failed)
    }
}
