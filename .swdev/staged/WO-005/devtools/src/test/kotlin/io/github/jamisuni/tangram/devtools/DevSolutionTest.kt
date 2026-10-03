package io.github.jamisuni.tangram.devtools

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.kernel.model.PieceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// ACCEPTANCE TEST (TASK-T5, independent author): the data half of the solution overlay, from REQ-046 and the frozen seam
// `object DevSolution { fun shapes(puzzle: Puzzle): List<SolutionShape> }` (design WO-005 "Test seams", "Value shapes").
// Real packaged puzzles through test-scope `content`.
class DevSolutionTest {

    private val puzzles = PuzzleLibrary.packaged().puzzles

    // req_types TYPE-001 piece colours (the one colour table; REQ-046 rule 3 draws a piece "in its colour").
    private val type001 = mapOf(
        PieceId.SQ to 0xFFD23F, PieceId.ST1 to 0x2DBE7E, PieceId.ST2 to 0x9B5DE5, PieceId.LT1 to 0xE8505B,
        PieceId.LT2 to 0x3D8BFD, PieceId.MT to 0xF9A826, PieceId.PG to 0xFF7AB8,
    )

    // REQ-046.A2 - "'Show the solution' draws one shape per piece of the puzzle on the silhouette."
    // Data level, for every packaged puzzle: one shape per stored solution piece (each piece once, in solution order), and each
    // shape's polygon is the piece's stored solution polygon, so the shapes lie exactly on the silhouette.
    @Test
    fun req046_A2_everyPuzzleHasOneShapePerPieceAtItsStoredPolygon() {
        assertTrue("fixture: the packaged library is empty", puzzles.isNotEmpty())
        for (p in puzzles) {
            val shapes = DevSolution.shapes(p)
            val tag = p.id.value
            assertEquals("$tag: one shape per piece", p.solution.size, shapes.size)
            assertEquals("$tag: every piece once", shapes.size, shapes.map { it.piece }.toSet().size)
            assertEquals("$tag: the pieces of the puzzle, in solution order", p.solution.map { it.piece }, shapes.map { it.piece })
            for ((entry, shape) in p.solution.zip(shapes)) {
                assertEquals("$tag ${entry.piece}: vertex count", entry.polygon.size, shape.polygon.size)
                for ((stored, drawn) in entry.polygon.zip(shape.polygon)) {
                    assertEquals("$tag ${entry.piece}: x", stored.x.toDouble(), drawn.x, 1e-9)
                    assertEquals("$tag ${entry.piece}: y", stored.y.toDouble(), drawn.y, 1e-9)
                }
            }
        }
    }

    // REQ-046 rule 3 (prose, not an acceptance claim): each piece's solution position is drawn "in its colour" - the TYPE-001 colour.
    @Test
    fun theShapesCarryTheirPiecesTypeColours() {
        for (p in puzzles) for (shape in DevSolution.shapes(p)) {
            assertEquals(
                "${p.id.value} ${shape.piece}",
                type001.getValue(shape.piece),
                shape.colour and 0xFFFFFF,
            )
        }
    }

    // Design WO-005 "Value shapes": `label` is the vertex average of the polygon (where the piece id is drawn).
    @Test
    fun theLabelIsTheVertexAverageOfThePolygon() {
        for (p in puzzles) for (shape in DevSolution.shapes(p)) {
            assertEquals("${p.id.value} ${shape.piece} x", shape.polygon.sumOf { it.x } / shape.polygon.size, shape.label.x, 1e-9)
            assertEquals("${p.id.value} ${shape.piece} y", shape.polygon.sumOf { it.y } / shape.polygon.size, shape.label.y, 1e-9)
        }
    }
}
