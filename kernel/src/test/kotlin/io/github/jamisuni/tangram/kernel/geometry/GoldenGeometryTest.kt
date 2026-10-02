package io.github.jamisuni.tangram.kernel.geometry

import io.github.jamisuni.tangram.kernel.model.PieceShape
import io.github.jamisuni.tangram.kernel.model.Rational
import io.github.jamisuni.tangram.kernel.model.Turn
import io.github.jamisuni.tangram.kernel.model.localCorners
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * SCAFFOLDING / guardrail tests (TASK-003b, WO-001 design §8, G-03): the Kotlin geometry against
 * tools/golden/geometry.json, which the Python reference (tools/tangram_geom.py) wrote. No acceptance
 * IDs. Outline corners are TASK-004's, the round trip is TASK-005b's.
 */
class GoldenGeometryTest {

    private val golden get() = Golden.data

    // ---- freshness ---------------------------------------------------------------------------------

    @Test
    fun goldenFormatIsKnown() {
        assertEquals(Golden.FORMAT, golden.format)
    }

    @Test
    fun goldenIsFreshAgainstTheTangramsFolder() {
        val problems = Golden.freshnessProblems(Golden.puzzleFileBytes(), golden)
        if (problems.isNotEmpty()) fail("${Golden.REFRESH_HINT}: ${problems.joinToString("; ")}")
    }

    @Test
    fun freshnessFailsWhenOnePuzzleByteChanges() {
        val files = Golden.puzzleFileBytes().toMutableMap()
        val id = files.keys.first()
        val original = files.getValue(id)
        // A different file: the last byte flipped (a content change, not a line-ending change).
        files[id] = original.copyOf().also { it[it.size - 1] = (it[it.size - 1].toInt() xor 0x01).toByte() }
        val problems = Golden.freshnessProblems(files, golden)
        assertEquals(listOf("Tangrams/$id.json changed (sha256 differs from the golden)"), problems)
    }

    @Test
    fun freshnessFailsOnAnExtraAndOnAMissingPuzzleFile() {
        val files = Golden.puzzleFileBytes().toMutableMap()
        val dropped = files.keys.first()
        files.remove(dropped)
        files["not-in-golden"] = "{}".toByteArray()
        val problems = Golden.freshnessProblems(files, golden)
        assertTrue(problems.toString(), "Tangrams/not-in-golden.json is not in the golden" in problems)
        assertTrue(problems.toString(), "the golden lists $dropped but Tangrams/$dropped.json is missing" in problems)
        assertEquals(2, problems.size)
    }

    @Test
    fun hashIgnoresLineEndingStyle() {
        for ((id, bytes) in Golden.puzzleFileBytes()) {
            val lf = Golden.normalizeCrlf(bytes)
            val crlf = String(lf, Charsets.UTF_8).replace("\n", "\r\n").toByteArray(Charsets.UTF_8)
            assertEquals("$id: CRLF copy hashes like the LF copy", Golden.sha256(lf), Golden.sha256(crlf))
        }
    }

    // ---- shapes and transforms ---------------------------------------------------------------------

    @Test
    fun shapesMatchLocalCornersAndArea() {
        assertEquals(PieceShape.values().toSet(), golden.shapes.keys)
        for (shape in PieceShape.values()) {
            val row = golden.shapes.getValue(shape)
            assertEquals("$shape local corners", row.local, shape.localCorners)
            assertEquals("$shape area", row.area, PieceGeometry.area(shape))
            assertEquals("$shape area is the exact shoelace of its corners", row.area, Golden.shoelaceArea(shape.localCorners))
        }
    }

    @Test
    fun offsetsMatchAllEightyTransformRows() {
        assertEquals(80, golden.transforms.size)
        // Rows are looked up by (shape, turn, mirrored), so the file's row order does not matter; each row's
        // corners are compared as an ordered list (the vertex order is part of the convention).
        val byKey = golden.transforms.associateBy { Triple(it.shape, it.turn, it.mirrored) }
        assertEquals("every (shape, turn, mirrored) appears once", 80, byKey.size)
        var checked = 0
        for (shape in PieceShape.values()) {
            for (mirrored in listOf(false, true)) {
                for (steps in 0..7) {
                    val row = byKey[Triple(shape, steps, mirrored)]
                    assertNotNull("$shape turn $steps mirrored=$mirrored is missing from the golden", row)
                    assertEquals(
                        "$shape turn $steps mirrored=$mirrored",
                        row!!.corners,
                        PieceGeometry.offsets(shape, Turn(steps), mirrored),
                    )
                    checked++
                }
            }
        }
        assertEquals(80, checked)
    }

    // ---- per puzzle file ---------------------------------------------------------------------------

    @Test
    fun goldenCoversEveryPuzzleFile() {
        // The reader's own sanity: the build order names exactly the solution's pieces.
        assertFalse(golden.puzzles.isEmpty())
        for (puzzle in golden.puzzles.values) {
            val pieces = puzzle.solution.map { it.piece }
            assertEquals("${puzzle.id}: solution pieces are distinct", pieces.size, pieces.toSet().size)
            assertEquals("${puzzle.id}: buildOrder is a permutation of the solution", pieces.toSet(), puzzle.buildOrder.toSet())
            assertEquals("${puzzle.id}: buildOrder has no repeats", puzzle.buildOrder.size, puzzle.buildOrder.toSet().size)
        }
    }

    @Test
    fun poseOfMatchesTheGoldenPoseOnEveryPuzzleFile() {
        val failures = mutableListOf<String>()
        for (puzzle in golden.puzzles.values) {
            for (entry in puzzle.solution) {
                val name = "${puzzle.id} ${entry.piece}"
                val placed = PieceGeometry.poseOf(entry.piece, entry.polygon)
                if (placed == null) {
                    failures += "$name: poseOf returned null"
                    continue
                }
                if (placed.piece != entry.piece) failures += "$name: poseOf answered piece ${placed.piece}"
                if (placed.turn != entry.pose.turn) failures += "$name: turn ${placed.turn.steps}, golden ${entry.pose.turn.steps}"
                if (placed.mirrored != entry.pose.mirrored) failures += "$name: mirrored ${placed.mirrored}, golden ${entry.pose.mirrored}"
                if (placed.at != entry.pose.at) failures += "$name: at ${placed.at}, golden ${entry.pose.at}"
                if (placed.corners.toSet() != entry.polygon.toSet()) {
                    failures += "$name: corners ${placed.corners} differ from the polygon ${entry.polygon} as a set"
                }
                if (placed.corners.size != entry.polygon.size) failures += "$name: corner count ${placed.corners.size}, polygon ${entry.polygon.size}"
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun exactAreasMatchTheGoldenOnEveryPuzzleFile() {
        val failures = mutableListOf<String>()
        for (puzzle in golden.puzzles.values) {
            var shoelaceTotal = Rational.ZERO
            var pieceTotal = Rational.ZERO
            for (entry in puzzle.solution) {
                val shoelace = Golden.shoelaceArea(entry.polygon)
                val area = PieceGeometry.area(entry.piece.shape)
                if (shoelace != area) failures += "${puzzle.id} ${entry.piece}: shoelace $shoelace, PieceGeometry.area $area"
                shoelaceTotal += shoelace
                pieceTotal += area
            }
            if (shoelaceTotal != puzzle.area) failures += "${puzzle.id}: sum of shoelace areas $shoelaceTotal, golden area ${puzzle.area}"
            if (pieceTotal != puzzle.area) failures += "${puzzle.id}: sum of PieceGeometry.area $pieceTotal, golden area ${puzzle.area}"
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }
}
