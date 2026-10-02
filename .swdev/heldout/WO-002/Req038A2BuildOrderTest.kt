package io.github.jamisuni.tangram.content.acceptance

import io.github.jamisuni.tangram.content.PuzzleFile
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleKind
import io.github.jamisuni.tangram.kernel.geometry.plus
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Rational
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// HELD-OUT (REQ-038.A2). Goes only to Test & Verify.
class Req038A2BuildOrderTest {

    // REQ-038.A2 -- "Each puzzle's printed build order places all of its pieces."
    // Decision F15: tested as "every stored solution locks piece by piece in build order" (kernel lock search over the parsed polygons).
    @Test
    fun everyPuzzleLocksPieceByPieceInItsBuildOrder() { // REQ-038.A2
        val lib = PuzzleLibrary.packaged()
        assertTrue(lib.puzzles.isNotEmpty())
        for (p in lib.puzzles) {
            val g = checkNotNull(AcceptanceGolden.puzzles[p.id.value]) { "${p.id.value} missing from golden" }
            val placed = lockInOrder(p, g.buildOrder)
            // "places all of its pieces": the placed set is the puzzle's piece set, each piece once
            assertEquals(p.solution.map { it.piece }.toSet(), placed.map { it.piece }.toSet())
            assertEquals("each piece once", p.solution.size, placed.size)
            // and each lands exactly on its stored polygon
            for (pl in placed) {
                assertEquals(
                    "${p.id.value} ${pl.piece}",
                    p.solution.first { it.piece == pl.piece }.polygon.toSet(),
                    pl.corners.toSet(),
                )
            }
        }
    }

    // REQ-038 Statement: "use every piece of TYPE-001 exactly once, except mini puzzles" (Rules: mini 1 to 6 distinct pieces).
    @Test
    fun everyNonMiniUsesAllSevenPiecesOnceAndMiniUsesFewer() { // guardrail G-03 (exact parsing), not REQ-038 acceptance 2
        for (p in PuzzleLibrary.packaged().puzzles) {
            val pieces = p.solution.map { it.piece }
            assertEquals("${p.id.value}: no piece twice", pieces.size, pieces.toSet().size)
            if (p.kind == PuzzleKind.MINI) assertTrue(pieces.size in 1..6) else assertEquals(7, pieces.size)
        }
    }

    // Puzzle.kt SolutionPiece: "its polygon in exact puzzle units (ADR-003)"; G-03 (golden): parsed polygons equal the reference's, as vertex sets.
    // Covers the exact number forms of the real files: integers, halves (sailboat), and [a, b] = a + b*sqrt(2) with negative b (things-house).
    @Test
    fun parsedPolygonsEqualTheGoldenPolygonsExactly() { // guardrail G-03 (exact parsing), not REQ-038 acceptance 2
        for (p in PuzzleLibrary.packaged().puzzles) {
            val g = AcceptanceGolden.puzzles.getValue(p.id.value)
            assertEquals(g.polygons.keys, p.solution.map { it.piece }.toSet())
            for (s in p.solution) {
                val want = g.polygons.getValue(s.piece)
                assertEquals("${p.id.value} ${s.piece}", want.toSet(), s.polygon.toSet())
                assertEquals("${p.id.value} ${s.piece} vertex count", want.size, s.polygon.size)
            }
        }
    }

    // Puzzle.kt SolutionPiece: "A file entry given as rot + flip + at arrives here already turned into its polygon."
    // Every real puzzle is re-written in that form from the golden poses (decimal tokens, exact) and must parse to the same polygons.
    @Test
    fun rotFlipAtFormGivesTheSamePolygons() { // guardrail G-03 (exact parsing), not REQ-038 acceptance 2
        val real = Fx.shippedFiles()
        for (file in real) {
            val stem = file.name.removeSuffix(".json")
            val g = AcceptanceGolden.puzzles.getValue(stem)
            val entries = g.poses.entries.joinToString(",\n") { (piece, pose) ->
                "    {\"piece\": \"${piece.name}\", \"rot\": ${pose.turn}, \"flip\": ${pose.mirrored}, " +
                    "\"at\": [${coord(pose.at.x)}, ${coord(pose.at.y)}]}"
            }
            val rewritten = PuzzleFile(file.name, replaceSolution(file.text, entries))
            val lib = PuzzleLibrary(real.filter { it.name != file.name } + rewritten)
            val p = lib.puzzle(PuzzleId(stem))
            assertNotNull("$stem in rot/flip/at form was rejected: ${lib.rejected}", p)
            for ((piece, want) in g.polygons) {
                val got = p!!.solution.first { it.piece == piece }.polygon
                assertEquals("$stem $piece", want.toSet(), got.toSet())
            }
        }
    }

    // G-03 (WO-002 Scope: "no number goes through a double"): the decimal token 0.1 is exactly 1/10, not the nearest double.
    // The whole mini-1 solution is moved by (0.1, 0.3); a valid tangram stays valid, and every coordinate must be the exact rational.
    @Test
    fun decimalCoordinatesAreExactRationals() { // guardrail G-03 (exact parsing), not REQ-038 acceptance 2
        val mini = Fx.shipped("shapes-mini-1")
        val start = mini.text.indexOf("\"solution\"")
        val end = mini.text.indexOf("\"art\"")
        val moved = Regex("\\[(\\d+), (\\d+)]").replace(mini.text.substring(start, end)) { m ->
            "[${m.groupValues[1]}.1, ${m.groupValues[2]}.3]"
        }
        val text = mini.text.substring(0, start) + moved + mini.text.substring(end)
        val real = Fx.shippedFiles()
        val lib = PuzzleLibrary(real.filter { it.name != mini.name } + PuzzleFile(mini.name, text))
        val p = lib.puzzle(PuzzleId("shapes-mini-1"))
        assertNotNull("a translated mini was rejected: ${lib.rejected}", p)
        val golden = AcceptanceGolden.puzzles.getValue("shapes-mini-1")
        val dx = Q2(Rational.of(1, 10), Rational.ZERO)
        val dy = Q2(Rational.of(3, 10), Rational.ZERO)
        for (s in p!!.solution) {
            val want = golden.polygons.getValue(s.piece).map { ExactPoint(it.x + dx, it.y + dy) }
            assertEquals(s.piece.name, want.toSet(), s.polygon.toSet())
        }
    }

    private fun coord(q: Q2): String =
        if (q.b == Rational.ZERO) AcceptanceGolden.decimal(q.a) else "[${AcceptanceGolden.decimal(q.a)}, ${AcceptanceGolden.decimal(q.b)}]"

    /** Replaces the contents of the top-level `"solution": [ ... ]` array. */
    private fun replaceSolution(text: String, entries: String): String {
        val key = text.indexOf("\"solution\"")
        val open = text.indexOf('[', key)
        var depth = 0
        var close = -1
        for (i in open until text.length) {
            if (text[i] == '[') depth++
            if (text[i] == ']') {
                depth--
                if (depth == 0) { close = i; break }
            }
        }
        check(close > 0)
        return text.substring(0, open + 1) + "\n" + entries + "\n  " + text.substring(close)
    }
}
