package io.github.jamisuni.tangram.time

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * decision DA-92 (AGENTS.md "Two device channels": "API 26 renders differently: a path drawn under a canvas scale blurs there, so
 * every path is drawn in px; never reintroduce a scaled-canvas path draw"; design WO-008 section 7: the pill is drawn "with `BasicText`
 * and a rounded `Box` (no canvas path, DA-92)"; design 8 inventory: "DA-92 scan: a `time`-local copy reading `time/src/main` through
 * `repo.root`"). A source scan, no token. Comments are skipped; matching is on identifier tokens, so `scaleX` is not a hit.
 */
class PxDrawingScanTest {

    // `withTransform` and `drawPath` as identifier tokens; `scale` only as a CALL (`scale(2f) { ... }` is DrawScope.scale; a local
    // variable named scale, used to turn dp into px, is exactly what the px rule asks for and is not a hit).
    private val forbidden: List<Pair<String, Regex>> = listOf(
        "withTransform" to RepoFiles.tokenRegex("withTransform"),
        "drawPath" to RepoFiles.tokenRegex("drawPath"),
        "scale(" to Regex("""(?<![A-Za-z0-9_])scale\s*\("""),
    )

    private fun codeFiles(): List<String> = RepoFiles.filesUnder("time/src/main").filter { it.endsWith(".kt") }

    internal fun hitsIn(src: String): List<String> {
        val code = RepoFiles.stripComments(src)
        val out = ArrayList<String>()
        for ((name, re) in forbidden) for (m in re.findAll(code)) out += "$name at offset ${m.range.first}"
        return out
    }

    // decision DA-92: no scaled-canvas draw anywhere in the module's main sources.
    @Test
    fun decisionDA92_noScaledCanvasOrPathDrawInTheTimeModule() {
        val files = codeFiles()
        assertTrue("no Kotlin file under time/src/main: the scan would be blind", files.isNotEmpty())
        val hits = ArrayList<String>()
        for (rel in files) for (h in hitsIn(RepoFiles.file(rel).readText())) hits += "$rel: $h"
        assertTrue("scaled or path draws in time/src/main (DA-92): $hits", hits.isEmpty())
    }

    // decision DA-92: the scan must see the module's code at all. At LAND-A the pill composable does not exist yet (TASK-064 follows), so the
    // canary is the keeper file, which TASK-063 delivers: a scan that finds no `PlayTimeKeeper.kt` under time/src/main read the wrong folder.
    @Test
    fun decisionDA92_theScanSeesTheKeeperSource() {
        val keeper = codeFiles().filter { it.endsWith("/PlayTimeKeeper.kt") }
        assertTrue("no PlayTimeKeeper.kt under time/src/main: the scan is blind", keeper.size == 1)
        assertTrue("PlayTimeKeeper.kt is empty", RepoFiles.stripComments(RepoFiles.file(keeper.single()).readText()).contains("class PlayTimeKeeper"))
    }

    // decision DA-92 (WO-008 design 7: the pill is drawn "with `BasicText` and a rounded `Box` (no canvas path, DA-92)"): the timer pill's source must be
    // seen by the scan (a `PuzzleTimer.kt` under time/src/main that draws with `BasicText`), else the scan could be blind to the one composable that draws.
    @Test
    fun decisionDA92_theScanSeesThePillAndItDrawsWithBasicText() {
        val pill = codeFiles().filter { it.endsWith("/PuzzleTimer.kt") }
        assertTrue("expected exactly one PuzzleTimer.kt under time/src/main, found $pill: the scan is blind to the pill", pill.size == 1)
        val code = RepoFiles.stripComments(RepoFiles.file(pill.single()).readText())
        assertTrue("PuzzleTimer.kt does not call BasicText (design 7)", RepoFiles.tokenRegex("BasicText").containsMatchIn(code))
        assertTrue("PuzzleTimer.kt names Canvas: the pill is not drawn on a canvas (design 7)", !RepoFiles.tokenRegex("Canvas").containsMatchIn(code))
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
