package io.github.jamisuni.tangram.devtools.held

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.devtools.DevSolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HELD-OUT (Test & Verify only): REQ-046.A3, the pose half in `devtools`, from the frozen seam
 * `DevSolution.poses(puzzle): List<PlacedPiece>?` ("exactly one `PlacedPiece` per `puzzle.solution` entry, in solution order, and
 * each pose's `corners` equal that entry's polygon as an exact point set; null only if `poseOf` returns null; never throws").
 * That the engine accepts these poses and solves is checked on the whole chain in the `app` held test. Self-contained.
 */
class HeldDevSolvePosesTest {

    private val puzzles = PuzzleLibrary.packaged().puzzles

    // REQ-046.A3 - "'Solve this puzzle now' shows the solved picture and leaves the best time empty."
    // What "solve now" hands to the game must be the whole stored solution: for every packaged puzzle, one pose per stored piece,
    // in solution order, each pose covering exactly the stored polygon.
    @Test
    fun req046_A3_everyPuzzleHasOnePosePerStoredPieceCoveringItsPolygon() {
        assertTrue("fixture: the library is empty", puzzles.isNotEmpty())
        for (p in puzzles) {
            val poses = DevSolution.poses(p)
            assertNotNull("${p.id.value}: no poses", poses)
            poses!!
            assertEquals("${p.id.value}: one pose per piece", p.solution.size, poses.size)
            for ((entry, pose) in p.solution.zip(poses)) {
                assertEquals("${p.id.value}: piece order", entry.piece, pose.piece)
                assertEquals(
                    "${p.id.value} ${entry.piece}: the pose covers the stored polygon exactly",
                    entry.polygon.toSet(),
                    pose.corners.toSet(),
                )
                assertEquals("${p.id.value} ${entry.piece}: corner count", entry.polygon.size, pose.corners.size)
            }
        }
    }
}
