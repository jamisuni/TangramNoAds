package io.github.jamisuni.tangram.play.acceptance

import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Rational
import io.github.jamisuni.tangram.kernel.model.Turn
import io.github.jamisuni.tangram.play.DropOutcome
import io.github.jamisuni.tangram.play.DropResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HELD-OUT acceptance test of REQ-019.A2 (Test & Verify only):
 * "A locked piece never overlaps another piece and never sticks out of the silhouette."
 *
 * The check is an oracle written in the test ([HeldOutOracle]) from the published pose convention and the stored
 * polygons - it shares nothing with the kernel's clipping.
 */
class Req019A2NeverOverlapsAcceptanceTest {

    private class Scenario(
        val name: String,
        val puzzle: Puzzle,
        val board: List<PlacedPiece>,
        val dpPerUnit: Double,
        val extent: Double,
        val step: Double,
        val minLocks: Int,
    )

    // Poses of three warm-up-1 pieces (literal): large triangle 1 (0,0)(4,0)(2,2); square (1,5)(0,4)(1,3)(2,4);
    // parallelogram (2,4)(2,2)(3,1)(3,3), mirrored, turn 2, vertex 0 at (2,4).
    private val warmUpBoard = listOf(
        placed(PieceId.LT1, 0, false, 0, 0),
        placed(PieceId.SQ, 0, false, 0, 4),
        placed(PieceId.PG, 2, true, 2, 4),
    )

    private val scenarios = listOf(
        Scenario("square, empty, 100 dp", Fixtures.SQUARE, emptyList(), DP_PER_UNIT_R_0_65, 4.0, 0.373, 500),
        Scenario("square, large triangle 1 + square placed, 100 dp", Fixtures.SQUARE, listOf(SquarePlaced.LT1, SquarePlaced.SQ), DP_PER_UNIT_R_0_65, 4.0, 0.373, 300),
        Scenario("square, four pieces placed, 20 dp", Fixtures.SQUARE, listOf(SquarePlaced.LT1, SquarePlaced.MT, SquarePlaced.ST1, SquarePlaced.PG), DP_PER_UNIT_R_1_5, 4.0, 0.373, 400),
        Scenario("mini-2, small triangle 1 placed, 100 dp", Fixtures.MINI_2, listOf(Mini2Placed.ST1), DP_PER_UNIT_R_0_65, 2.0, 0.19, 100),
        Scenario("warm-up-1, three pieces placed, 100 dp", Fixtures.WARMUP_1, warmUpBoard, DP_PER_UNIT_R_0_65, 6.0, 0.547, 80),
    )

    // REQ-019.A2 — the oracle itself must be able to say "no" (otherwise the sweeps below prove nothing).
    @Test
    fun a2_oracleSelfCheck() {
        val sq = Fixtures.SQUARE
        // The stored placements are fine.
        assertNull(HeldOutOracle.violation(sq, listOf(SquarePlaced.LT1), SquarePlaced.SQ))
        assertNull(HeldOutOracle.violation(sq, emptyList(), SquarePlaced.PG))
        // A large triangle with vertex 0 at (1,0) sticks out of the 4 x 4 square.
        assertNotNull(HeldOutOracle.violation(sq, emptyList(), placed(PieceId.LT1, 0, false, 1, 0)))
        // A medium triangle at (0,0) overlaps large triangle 1.
        assertNotNull(HeldOutOracle.violation(sq, listOf(SquarePlaced.LT1), placed(PieceId.MT, 0, false, 0, 0)))
        // The square (diamond) at the corner sticks out.
        assertNotNull(HeldOutOracle.violation(sq, emptyList(), placed(PieceId.SQ, 0, false, 0, 0)))
        // A sliver is still an overlap: small triangle 1 moved 0.01 to the left of its place (4,0) overlaps the
        // large triangle along their shared edge (area about 0.005, far above the 1e-6 tolerance).
        val shifted = PlacedPiece(
            PieceId.ST1, Turn(2), false, ExactPoint(Q2(Rational.of(399, 100), Rational.ZERO), Q2.of(0)),
        )
        assertNotNull(HeldOutOracle.violation(sq, listOf(SquarePlaced.LT1), shifted))
        // ... and the unshifted one is fine.
        assertNull(HeldOutOracle.violation(sq, listOf(SquarePlaced.LT1), SquarePlaced.ST1))
        // The stored polygons add up to the whole 4 x 4 square.
        assertEquals(16.0, sq.solution.sumOf { HeldOutOracle.area(HeldOutOracle.polygon(it.polygon)) }, 1e-9)
    }

    // REQ-019.A2 — sweeps over all pieces, turns and mirrors, many releases: every lock is legal.
    @Test
    fun a2_everyLockInASweepIsInsideTheSilhouetteAndOverlapsNothing() {
        for (sc in scenarios) {
            val resolver = DropResolver(sc.puzzle)
            val onBoard = sc.board.map { it.piece }.toSet()
            val tray = sc.puzzle.solution.map { it.piece }.filter { it !in onBoard }
            val n = (sc.extent / sc.step).toInt() + 5
            var locks = 0
            var drops = 0
            for (piece in tray) {
                val mirrors = if (piece == PieceId.PG) listOf(false, true) else listOf(false)
                for (turn in 0..7) {
                    for (mirrored in mirrors) {
                        for (i in 0 until n) {
                            for (j in 0 until n) {
                                val pose = drag(piece, turn, mirrored, -0.7 + sc.step * i, -0.7 + sc.step * j)
                                drops++
                                val outcome = resolver.release(pose, sc.board, sc.dpPerUnit)
                                if (outcome is DropOutcome.Locked) {
                                    locks++
                                    val placed = outcome.placed
                                    val where = "${sc.name}: $piece turn $turn mirrored $mirrored released at " +
                                        "(${pose.origin.x}, ${pose.origin.y})"
                                    // REQ-019 Rules: the lock never changes the turn and mirror.
                                    assertEquals("$where: piece", piece, placed.piece)
                                    assertEquals("$where: turn", pose.turn, placed.turn)
                                    assertEquals("$where: mirror", pose.mirrored, placed.mirrored)
                                    val problem = HeldOutOracle.violation(sc.puzzle, sc.board, placed)
                                    assertNull("$where: $problem", problem)
                                }
                            }
                        }
                    }
                }
            }
            // Non-vacuity: a search that never locks would pass the loop above.
            assertTrue("${sc.name}: only $locks locks in $drops drops", locks >= sc.minLocks)
        }
    }

    // REQ-019.A2 — a spot that would stick out is never taken: the diamond has no valid spot in the empty square.
    @Test
    fun a2_aPieceThatWouldStickOutNeverLocksAnywhere() {
        val resolver = DropResolver(Fixtures.SQUARE)
        // Square (diamond), turn 0: its corners lie on the four extremes of a 2 x 2 box; with one corner on an outline
        // corner of the 4 x 4 square the others always stick out (checked for each of its 4 corners and 4 outline corners).
        for (dp in listOf(DP_PER_UNIT_R_0_65, DP_PER_UNIT_R_1_5)) {
            for (i in 0..16) {
                for (j in 0..16) {
                    val pose = drag(PieceId.SQ, 0, false, -0.3 + 0.29 * i, -0.3 + 0.29 * j)
                    val outcome = resolver.release(pose, emptyList(), dp)
                    assertHome(outcome, pose, "diamond at ($i, $j), dp $dp")
                }
            }
        }
    }

    // REQ-019.A2 — an anchor next to a placed piece whose spot would overlap it is not taken.
    @Test
    fun a2_aPieceThatWouldOverlapANeighbourNeverLocksThere() {
        val resolver = DropResolver(Fixtures.SQUARE)
        // Large triangle 1 on the board. A medium triangle at turn 0 dropped on the top-left corner would have its right
        // angle on the outline corner (0,0) - but that spot is covered by large triangle 1.
        val pose = drag(PieceId.MT, 0, false, 0.15, 0.1)
        assertHome(resolver.release(pose, listOf(SquarePlaced.LT1), DP_PER_UNIT_R_0_65), pose, "on top of a neighbour")
        // Control: with the neighbour gone the very same release locks.
        assertLockedExactly(resolver.release(pose, emptyList(), DP_PER_UNIT_R_0_65), pose, pt(0, 0), "no neighbour")
    }

    // REQ-019.A2 + DA-2 — the nearest anchor spot is invalid, a farther one is valid: take the valid one, never the
    // invalid nearest and never give up. (R = 1.5 so both are in reach; the 0.16 window is measured from the nearest
    // VALID position, decisions.md DA-2.)
    @Test
    fun a2_anInvalidNearestSpotIsSkippedAndTheValidFartherSpotIsTaken() {
        val resolver = DropResolver(Fixtures.SQUARE)

        // Empty square, medium triangle, turn 0. Nearest anchor spot: vertex 0 at (0,4) (0.73 away) - the triangle
        // would stick out below the square. Valid spot: vertex 0 at (0,2) (1.27 away): corners (0,2)(2,2)(0,4), the
        // outline corner (0,4) on its corner (0,4).
        val belowLeft = drag(PieceId.MT, 0, false, 0.03, 3.27)
        assertLockedExactly(resolver.release(belowLeft, emptyList(), DP_PER_UNIT_R_1_5), belowLeft, pt(0, 2), "sticks out below")

        // Empty square, medium triangle, turn 0 near the top-right corner: nearest (4,0) sticks out to the right;
        // valid (2,0): corners (2,0)(4,0)(2,2).
        val rightTop = drag(PieceId.MT, 0, false, 3.23, 0.07)
        assertLockedExactly(resolver.release(rightTop, emptyList(), DP_PER_UNIT_R_1_5), rightTop, pt(2, 0), "sticks out right")

        // Large triangle 1 on the board; medium triangle, turn 0, near the top-left corner: nearest (0,0) overlaps the
        // neighbour; valid (0,2): corners (0,2)(2,2)(0,4), touching the neighbour's apex (2,2) only.
        val overlapping = drag(PieceId.MT, 0, false, -0.37, 0.87)
        assertLockedExactly(
            resolver.release(overlapping, listOf(SquarePlaced.LT1), DP_PER_UNIT_R_1_5), overlapping, pt(0, 2), "overlaps a neighbour",
        )

        // Large triangle 1 and the square on the board; large triangle 2, turn 6, near the left: nearest (1,3) is
        // inside the square's area; valid (0,4) is the triangle's own place (0,4)(0,0)(2,2).
        val inTheWay = drag(PieceId.LT2, 6, false, 0.03, 2.87)
        assertLockedExactly(
            resolver.release(inTheWay, listOf(SquarePlaced.LT1, SquarePlaced.SQ), DP_PER_UNIT_R_1_5), inTheWay, pt(0, 4),
            "the nearest spot is taken by a neighbour",
        )
    }
}
