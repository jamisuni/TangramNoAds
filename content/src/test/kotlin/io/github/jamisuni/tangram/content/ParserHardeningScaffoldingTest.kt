package io.github.jamisuni.tangram.content

import io.github.jamisuni.tangram.content.acceptance.Fx
import io.github.jamisuni.tangram.contracts.puzzle.PictureShape
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Rational
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

// SCAFFOLDING (disposable): parser-hardening tests of the implementer. Not acceptance tests.
class ParserHardeningScaffoldingTest {

    private fun num(text: String) = ExactNumbers.rational(Json.parseToJsonElement(text))
    private fun coord(text: String) = ExactNumbers.coordinate(Json.parseToJsonElement(text))

    private val mini = Fx.shipped("shapes-mini-1")

    private fun mutated(from: String, to: String): ParseResult {
        assertTrue("fixture text not found: $from", mini.text.contains(from))
        return PuzzleParser.parse(PuzzleFile(mini.name, mini.text.replace(from, to)))
    }

    private fun assertRejected(r: ParseResult) = assertTrue("expected Rejected, got $r", r is ParseResult.Rejected)

    @Test
    fun decimalTokensAreExact() {
        assertEquals(Rational.of(3, 2), num("1.5"))
        assertEquals(Rational.of(67, 100), num("0.67"))
        assertEquals(Rational.of(-5, 2), num("-2.5"))
        assertEquals(Rational.of(2), num("2"))
        assertEquals(Rational.of(1, 10), num("1e-1"))
        assertEquals(Rational.of(200), num("2E+2"))
        assertEquals(Q2(Rational.of(4), Rational.of(-1)), coord("[4, -1]"))
    }

    @Test
    fun badNumberTokensFail() {
        for (t in listOf("\"1\"", "null", "NaN", "Infinity", "+1", "0x10", "true", "1234567890123456789012", "1e99999999", "0.1234567890123456789")) {
            try {
                num(t)
                fail("accepted $t")
            } catch (expected: RuntimeException) {
                // rejected as designed
            }
        }
    }

    @Test
    fun hostileNumbersInAFileAreRejectedNotThrown() {
        assertRejected(mutated("\"difficulty\": 1", "\"difficulty\": 2.0"))
        assertRejected(mutated("\"difficulty\": 1", "\"difficulty\": \"1\""))
        assertRejected(mutated("\"difficulty\": 1", "\"difficulty\": 6"))
        assertRejected(mutated("[[2, 2], [3, 1]", "[[2, 1234567890123456789012], [3, 1]"))
        assertRejected(mutated("\"opacity\": 0.45", "\"opacity\": 1.5"))
        assertRejected(mutated("\"opacity\": 0.45", "\"opacity\": NaN"))
        assertRejected(mutated("\"width\": 0.06, \"opacity\": 0.7},\n      {\"type\": \"line\", \"from\": [0, 1.33]", "\"width\": 0, \"opacity\": 0.7},\n      {\"type\": \"line\", \"from\": [0, 1.33]"))
    }

    @Test
    fun structureProblemsAreRejected() {
        assertRejected(mutated("\"fi\": \"Mini: Pyramidi\"", "\"fi\": \"  \""))
        assertRejected(mutated("#E9C46A", "#E9C46"))
        assertRejected(mutated("\"type\": \"rect\"", "\"type\": \"star\""))
        assertRejected(mutated("\"piece\": \"ST2\"", "\"piece\": \"XX\""))
        assertRejected(mutated("\"kind\": \"mini\"", "\"kind\": \"huge\""))
        assertRejected(mutated("\"kind\": \"mini\"", "\"kind\": \"full\""))
        assertRejected(mutated("{\"piece\": \"ST1\", \"polygon\": [[0, 2], [2, 2], [1, 1]]}", "{\"piece\": \"ST1\", \"polygon\": [[0, 2], [2, 2], [1, 1]], \"rot\": 0, \"at\": [0, 0]}"))
        assertRejected(mutated("\"id\": \"shapes-mini-1\"", "\"id\": \"shapes-mini-9\""))
        assertRejected(PuzzleParser.parse(PuzzleFile(mini.name, "{ nope")))
    }

    @Test
    fun polygonOfWrongShapeOrOrderIsRejected() {
        // not a small triangle
        assertRejected(mutated("[[0, 2], [2, 2], [1, 1]]", "[[0, 2], [3, 2], [1, 1]]"))
        // the square's vertices in a bowtie order: right vertex set, wrong cyclic order
        assertRejected(mutated("[[2, 2], [3, 1], [2, 0], [1, 1]]", "[[2, 2], [2, 0], [3, 1], [1, 1]]"))
    }

    @Test
    fun reviewedFlagIsLiteralTrueOnly() {
        fun flag(literal: String): Boolean {
            val r = mutated("\"reviewedByHuman\": false", "\"reviewedByHuman\": $literal") as ParseResult.Parsed
            return r.puzzle.reviewedByHuman
        }
        assertEquals(true, flag("true"))
        assertEquals(false, flag("\"true\""))
        assertEquals(false, flag("1"))
        assertEquals(false, flag("null"))
    }

    @Test
    fun rotFlipAtFormGivesAPolygon() {
        val text = mini.text.replace(
            "{\"piece\": \"ST1\", \"polygon\": [[0, 2], [2, 2], [1, 1]]}",
            "{\"piece\": \"ST1\", \"rot\": 0, \"at\": [0, 2]}",
        )
        val r = PuzzleParser.parse(PuzzleFile(mini.name, text))
        assertTrue("got $r", r is ParseResult.Parsed)
        val st1 = (r as ParseResult.Parsed).puzzle.solution.first { it.piece == PieceId.ST1 }
        assertEquals(3, st1.polygon.size)
        assertEquals(Q2.of(0), st1.polygon[0].x)
        assertEquals(Q2.of(2), st1.polygon[0].y)
    }

    @Test
    fun pictureShapesAreCarried() {
        val p = (PuzzleParser.parse(mini) as ParseResult.Parsed).puzzle
        assertEquals(4, p.picture.shapes.size)
        assertTrue(p.picture.shapes[0] is PictureShape.Polygon)
        assertTrue(p.picture.shapes[3] is PictureShape.Rect)
    }

    @Test
    fun packagingDefectsFailLoudly() {
        val empty = object : ClassLoader(null) {}
        try {
            PackagedPuzzles.files(empty)
            fail("a missing index must throw")
        } catch (expected: IllegalStateException) {
            // fail loudly (DA-10)
        }
        val badIndex = object : ClassLoader(null) {
            override fun getResourceAsStream(name: String) =
                if (name == "tangrams/index.txt") "missing-one.json\n".byteInputStream() else null
        }
        try {
            PackagedPuzzles.files(badIndex)
            fail("a listed but missing file must throw")
        } catch (expected: IllegalStateException) {
            // fail loudly (DA-10)
        }
    }

    @Test
    fun packagedLibraryReadsEveryFile() {
        val lib = PuzzleLibrary.packaged()
        assertTrue(lib.rejected.isEmpty())
        assertEquals(Fx.shippedFiles().size, lib.puzzles.size)
    }
}
