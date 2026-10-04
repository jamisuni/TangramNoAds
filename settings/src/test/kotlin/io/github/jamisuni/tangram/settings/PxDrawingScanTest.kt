package io.github.jamisuni.tangram.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * decision DA-92 (AGENTS.md "Two device channels": "API 26 renders differently: a path drawn under a canvas scale blurs there, so
 * every path is drawn in px; never reintroduce a scaled-canvas path draw"; design WO-007 section 2: the gear glyph "is drawn in a
 * Canvas with drawLine and drawCircle in px, no scale, no path"; acceptance table: "the DA-92 scan (no `scale` / `withTransform`
 * path draw in `settings/src/main`)"). A source scan, no token. Comments are skipped; matching is on identifier tokens, so `scaleX`
 * or `AudioScale` are not hits.
 */
class PxDrawingScanTest {

    // `withTransform` and `drawPath` as identifier tokens; `scale` only as a CALL (`scale(2f) { ... }` is DrawScope.scale; a local
    // variable named scale, used to turn dp into px, is exactly what the px rule asks for and is not a hit).
    private val forbidden: List<Pair<String, Regex>> = listOf(
        "withTransform" to RepoFiles.tokenRegex("withTransform"),
        "drawPath" to RepoFiles.tokenRegex("drawPath"),
        "scale(" to Regex("""(?<![A-Za-z0-9_])scale\s*\("""),
    )

    private fun codeFiles(): List<String> = RepoFiles.filesUnder("settings/src/main").filter { it.endsWith(".kt") }

    internal fun hitsIn(src: String): List<String> {
        val code = RepoFiles.stripComments(src)
        val out = ArrayList<String>()
        for ((name, re) in forbidden) for (m in re.findAll(code)) out += "$name at offset ${m.range.first}"
        return out
    }

    // decision DA-92: no scaled-canvas draw anywhere in the module's main sources.
    @Test
    fun decisionDA92_noScaledCanvasOrPathDrawInTheSettingsModule() {
        val files = codeFiles()
        assertTrue("no Kotlin file under settings/src/main: the scan would be blind", files.isNotEmpty())
        val hits = ArrayList<String>()
        for (rel in files) for (h in hitsIn(RepoFiles.file(rel).readText())) hits += "$rel: $h"
        assertTrue("scaled or path draws in settings/src/main (DA-92): $hits", hits.isEmpty())
    }

    // decision DA-92: the scan must see a drawing at all (the gear is drawn in a Canvas, design section 2), else it proved nothing.
    @Test
    fun decisionDA92_theScanSeesTheGearsCanvas() {
        val joined = codeFiles().joinToString("\n") { RepoFiles.stripComments(RepoFiles.file(it).readText()) }
        assertTrue("no `Canvas` in settings/src/main: the scan is blind to the gear", RepoFiles.tokenRegex("Canvas").containsMatchIn(joined))
    }

    // decision DA-92 (control): the scanner flags what it must flag and nothing else.
    @Test
    fun decisionDA92_theScannerCanFail() {
        assertFalse(hitsIn("Canvas { scale(2f) { drawLine() } }").isEmpty())
        assertFalse(hitsIn("Canvas { withTransform({ translate(1f, 1f) }) { drawCircle() } }").isEmpty())
        assertFalse(hitsIn("Canvas { drawPath(path, color) }").isEmpty())
        assertTrue("a comment is not code", hitsIn("// scale(2f) and withTransform and drawPath\nCanvas { drawLine(); drawCircle() }").isEmpty())
        assertTrue("scaleX is another identifier", hitsIn("Modifier.graphicsLayer { scaleX = 1f }").isEmpty())
        assertTrue("a variable named scale is a px conversion, not a scaled canvas", hitsIn("val scale = density; drawCircle(c, 4f * scale, p)").isEmpty())
        assertTrue("a clean glyph", hitsIn("Canvas(m) { drawCircle(c, r, centre); drawLine(a, b, c) }").isEmpty())
    }
}
