package io.github.jamisuni.tangram.devtools

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.TrayRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// SCAFFOLDING (decision DA-78 era D-2): DevSolution over the real packaged puzzles.
class DevSolutionScaffoldingTest {

    private val puzzles: List<Puzzle> = PuzzleLibrary.packaged().puzzles

    @Test
    fun thirteenPuzzlesArePackaged() {
        assertEquals(13, puzzles.size)
    }

    @Test
    fun shapesMatchTheStoredSolution() {
        for (puzzle in puzzles) {
            val shapes = DevSolution.shapes(puzzle)
            assertEquals(puzzle.id.toString(), puzzle.solution.size, shapes.size)
            for ((shape, stored) in shapes.zip(puzzle.solution)) {
                assertEquals(stored.piece, shape.piece)
                assertEquals(TrayRules.colour(stored.piece), shape.colour)
                assertEquals(stored.polygon.size, shape.polygon.size)
                for ((v, p) in shape.polygon.zip(stored.polygon)) {
                    assertEquals(p.x.toDouble(), v.x, 1e-9)
                    assertEquals(p.y.toDouble(), v.y, 1e-9)
                }
                assertTrue("${puzzle.id} ${stored.piece} label inside", inside(shape.label, shape.polygon))
            }
        }
    }

    @Test
    fun posesEqualTheStoredPolygons() {
        for (puzzle in puzzles) {
            val poses = DevSolution.poses(puzzle)
            assertNotNull(puzzle.id.toString(), poses)
            assertEquals(puzzle.solution.size, poses!!.size)
            for ((pose, stored) in poses.zip(puzzle.solution)) {
                assertEquals(stored.piece, pose.piece)
                assertEquals(stored.polygon.toSet(), pose.corners.toSet())
            }
        }
    }

    /** Ray casting; the labels are interior points, so edge cases do not arise. */
    private fun inside(p: Vec2, poly: List<Vec2>): Boolean {
        var c = false
        var j = poly.size - 1
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[j]
            if ((a.y > p.y) != (b.y > p.y) && p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x) c = !c
            j = i
        }
        return c
    }
}
