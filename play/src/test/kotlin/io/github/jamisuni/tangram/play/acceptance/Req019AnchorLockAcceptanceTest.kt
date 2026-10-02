package io.github.jamisuni.tangram.play.acceptance

import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.play.DropOutcome
import io.github.jamisuni.tangram.play.DropResolver
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WO-001 acceptance tests of REQ-019 "Lock to corners and neighbours": A1 and A3.
 *
 * Reading of the acceptance wording: req_review_01.md F23 (A1 = "a position is chosen by TYPE-004 when released
 * within the lock distance"; A3 = "no anchor within the lock distance"). Tie-break: decisions.md DA-1.
 * Lock distance: TYPE-004 (R = 0.65 units, never below 30 dp): [DP_PER_UNIT_R_0_65] gives R = 0.65,
 * [DP_PER_UNIT_R_1_5] gives R = 1.5. No pose below sits within 0.05 units of an exact R.
 */
class Req019AnchorLockAcceptanceTest {

    private val square = DropResolver(Fixtures.SQUARE)

    // ---------------------------------------------------------------------------------------------
    // A1 — "A piece released within the lock distance of a valid position locks there exactly."
    // ---------------------------------------------------------------------------------------------

    // REQ-019.A1
    @Test
    fun a1_releasedWithinLockDistanceOfAnOutlineCornerLocksExactlyOnIt() {
        // Large triangle, turn 0: corners (0,0)(4,0)(2,2) when its vertex 0 is at (0,0). On the empty 4x4 square
        // the only valid position within 0.65 of these releases is vertex 0 at (0,0): corners on the outline
        // corners (0,0) and (4,0), everything inside the square (REQ-019 Statement).
        // Distances to (0,0): 0.36, 0.43, 0.50, 0.57 - all inside R = 0.65, none near it.
        val releases = listOf(0.3 to 0.2, -0.25 to 0.35, 0.0 to -0.5, 0.4 to 0.4)
        for ((x, y) in releases) {
            val pose = drag(PieceId.LT1, 0, false, x, y)
            val outcome = square.release(pose, emptyList(), DP_PER_UNIT_R_0_65)
            assertLockedExactly(outcome, pose, pt(0, 0), "release at ($x, $y)")
        }
    }

    // REQ-019.A1
    @Test
    fun a1_everyOutlineCornerOfTheSilhouetteIsAnAnchor() {
        // Medium triangle (right-angle corner = vertex 0), turned so that its right angle fits each corner of the
        // 4x4 square; a lazy search that only uses some outline corners misses one of these.
        //   turn 0 at (0,0): (0,0)(2,0)(0,2)    turn 2 at (4,0): (4,0)(4,2)(2,0)
        //   turn 4 at (4,4): (4,4)(2,4)(4,2)    turn 6 at (0,4): (0,4)(0,2)(2,4)
        val cases = listOf(
            Triple(0, pt(0, 0), 0.2 to 0.1),
            Triple(2, pt(4, 0), 3.8 to 0.15),
            Triple(4, pt(4, 4), 4.1 to 3.85),
            Triple(6, pt(0, 4), 0.15 to 3.8),
        )
        for ((turn, expectedAt, origin) in cases) {
            val pose = drag(PieceId.MT, turn, false, origin.first, origin.second)
            val outcome = square.release(pose, emptyList(), DP_PER_UNIT_R_0_65)
            assertLockedExactly(outcome, pose, expectedAt, "medium triangle turn $turn")
        }
    }

    // REQ-019.A1
    @Test
    fun a1_lockedPositionIsExactEvenWhenItHasASqrt2Part() {
        // TYPE-004: "the locked position is the anchor's exact position minus the piece corner's exact offset, so a
        // locked piece sits exactly on its anchor". Small triangle at turn 1 (45 degrees): the corner offsets are
        // (0,0), (sqrt2, sqrt2), (0, sqrt2). Its corner with offset (sqrt2, sqrt2) fits the outline corner (4,4):
        // vertex 0 at (4 - sqrt2, 4 - sqrt2), corners (4-sqrt2, 4-sqrt2)(4,4)(4-sqrt2, 4), all inside the square.
        // The released position (2.7, 2.7) is 0.16 from it; no other valid position is within 0.65.
        val pose = drag(PieceId.ST1, 1, false, 2.7, 2.7)
        val outcome = square.release(pose, emptyList(), DP_PER_UNIT_R_0_65)
        assertLockedExactly(outcome, pose, ExactPoint(q2(4, -1), q2(4, -1)))
    }

    // REQ-019.A1
    @Test
    fun a1_cornersOfPiecesAlreadyOnTheBoardAreAnchorsToo() {
        // REQ-019 Statement: "...a corner of the silhouette outline or of a piece already on the board".
        // (a) Large triangle 1 on the board; large triangle 2, turn 6, vertex 0 at (0,4): corners (0,4)(0,0)(2,2),
        //     all three on anchors (outline corners (0,4)(0,0) and the neighbour's corner (2,2)).
        val lt2 = drag(PieceId.LT2, 6, false, 0.25, 4.3)
        assertLockedExactly(
            square.release(lt2, listOf(SquarePlaced.LT1), DP_PER_UNIT_R_0_65),
            lt2,
            pt(0, 4),
            "next to the large triangle",
        )

        // (b) Only the square on the board (diamond (2,2)(3,1)(4,2)(3,3)). Small triangle 2, turn 4, vertex 0 at
        //     (3,3): corners (3,3)(1,3)(2,2); its only anchors are the square's corners (3,3) and (2,2) - it
        //     touches no outline corner, so only a neighbour's corner can hold it.
        val st2 = drag(PieceId.ST2, 4, false, 3.3, 3.2)
        assertLockedExactly(
            square.release(st2, listOf(SquarePlaced.SQ), DP_PER_UNIT_R_0_65),
            st2,
            pt(3, 3),
            "tied only to the square's corners",
        )
    }

    // REQ-019.A1
    @Test
    fun a1_lockDistanceFollowsTheThirtyDpFloor() {
        // TYPE-004: "lock distance R = 0.65 units, and never below 30 dp on screen".
        // The same release one unit from the lock position: out of reach at 100 dp/unit (R = 0.65 units) and in
        // reach at 20 dp/unit (R = 30 dp = 1.5 units).
        val far = drag(PieceId.LT1, 0, false, 1.0, 0.0)
        assertHome(square.release(far, emptyList(), DP_PER_UNIT_R_0_65), far, "one unit away, R = 0.65")
        assertLockedExactly(
            square.release(far, emptyList(), DP_PER_UNIT_R_1_5), far, pt(0, 0), "one unit away, R = 1.5",
        )

        // A release 0.8 units away is still out of reach at R = 0.65 (a lock beyond the lock distance is wrong),
        // while 0.5 units away is in reach at both.
        val point8 = drag(PieceId.LT1, 0, false, 0.8, 0.0)
        assertHome(square.release(point8, emptyList(), DP_PER_UNIT_R_0_65), point8, "0.8 away, R = 0.65")
        val near = drag(PieceId.LT1, 0, false, 0.5, 0.0)
        assertLockedExactly(square.release(near, emptyList(), DP_PER_UNIT_R_0_65), near, pt(0, 0), "0.5 away")
        assertLockedExactly(square.release(near, emptyList(), DP_PER_UNIT_R_1_5), near, pt(0, 0), "0.5 away, R = 1.5")
    }

    // REQ-019.A1
    @Test
    fun a1_lockKeepsTheMirrorOfAParallelogram() {
        // The parallelogram is the one piece whose mirror image differs (TYPE-003). Mirrored, turn 0: corners
        // (4,4)(2,4)(1,3)(3,3) when vertex 0 is at (4,4) - it fits the bottom-right corner; unmirrored it would not.
        val mirrored = drag(PieceId.PG, 0, true, 3.8, 3.8)
        assertLockedExactly(
            square.release(mirrored, emptyList(), DP_PER_UNIT_R_0_65), mirrored, pt(4, 4), "mirrored parallelogram",
        )
        // Unmirrored, turn 0: corners (0,4)(2,4)(3,3)(1,3) when vertex 0 is at (0,4) - the bottom-left corner.
        val plain = drag(PieceId.PG, 0, false, 0.2, 3.8)
        assertLockedExactly(
            square.release(plain, emptyList(), DP_PER_UNIT_R_0_65), plain, pt(0, 4), "unmirrored parallelogram",
        )
    }

    // REQ-019.A1 (F23: with two valid positions in reach, TYPE-004 picks the one with the lowest score)
    @Test
    fun a1_ofTwoValidPositionsTheLowerScoreWins() {
        // TYPE-004: score = |t| - 0.04 x (corners that land exactly on anchors); the lowest score wins.
        // Square on the board; small triangle, turn 4. Two valid positions for it:
        //   X: vertex 0 at (3,3): corners (3,3)(1,3)(2,2)  - (3,3) and (2,2) are the square's corners
        //   Y: vertex 0 at (4,4): corners (4,4)(2,4)(3,3)  - (4,4) outline corner, (3,3) the square's corner
        // R = 1.5 so that both are in reach. Equal anchored corners (2 each), so the nearer one wins - on both sides.
        val onX = drag(PieceId.ST2, 4, false, 3.4, 3.4) // 0.57 from X, 0.85 from Y
        assertLockedExactly(square.release(onX, listOf(SquarePlaced.SQ), DP_PER_UNIT_R_1_5), onX, pt(3, 3), "nearer to X")
        val onY = drag(PieceId.ST2, 4, false, 3.6, 3.6) // 0.85 from X, 0.57 from Y
        assertLockedExactly(square.release(onY, listOf(SquarePlaced.SQ), DP_PER_UNIT_R_1_5), onY, pt(4, 4), "nearer to Y")
    }

    // REQ-019.A1
    @Test
    fun a1_anchoredCornersCountButOnlyFortyThousandthsEach() {
        // TYPE-004: score = |t| - 0.04 x (anchored corners). Large triangle 1 and small triangle 1 on the board.
        // The square (diamond), turn 0, has two valid positions:
        //   X: vertex 0 at (1,3): corners (1,3)(2,2)(3,3)(2,4)   - 1 anchored corner, (2,2) the large triangle's apex
        //   Y: vertex 0 at (2,2): corners (2,2)(3,1)(4,2)(3,3)   - 3 anchored corners: (2,2)(3,1)(4,2)
        // R = 1.5 so both are in reach.
        val board = listOf(SquarePlaced.LT1, SquarePlaced.ST1)

        // (1.49, 2.51): |t| = 0.69 to X, 0.72 to Y. Scores: X 0.69 - 0.04 = 0.65, Y 0.72 - 0.12 = 0.60: the FARTHER,
        // better-anchored Y wins - a plain "nearest corner" search gets this wrong.
        val towardsY = drag(PieceId.SQ, 0, false, 1.49, 2.51)
        assertLockedExactly(square.release(towardsY, board, DP_PER_UNIT_R_1_5), towardsY, pt(2, 2), "bonus decides")

        // (1.45, 2.55): |t| = 0.64 to X, 0.78 to Y. Scores: X 0.64 - 0.04 = 0.60, Y 0.78 - 0.12 = 0.66: the nearer X
        // wins although Y has more anchored corners - a bonus bigger than 0.04 per corner gets this wrong.
        val towardsX = drag(PieceId.SQ, 0, false, 1.45, 2.55)
        assertLockedExactly(square.release(towardsX, board, DP_PER_UNIT_R_1_5), towardsX, pt(1, 3), "distance decides")
    }

    // REQ-019.A1 (decisions.md DA-1: equal score -> smaller |t| -> reading order of the position, y then x)
    @Test
    fun a1_equalScoreAndDistanceAreBrokenByReadingOrderOfThePosition() {
        // Each release is exactly 1.0 (or 0.707) from two valid positions with the same number of anchored corners,
        // so score and |t| are equal; DA-1 picks the position that comes first in reading order (y, then x).
        // R = 1.5 so both positions are in reach. Empty 4x4 square unless noted.

        // Small triangle, turn 0: vertex 0 at (0,0) [corner (0,0)] or at (2,0) [corner (4,0)]: same y, smaller x wins.
        val top = drag(PieceId.ST1, 0, false, 1.0, 0.0)
        assertLockedExactly(square.release(top, emptyList(), DP_PER_UNIT_R_1_5), top, pt(0, 0), "same y, x decides")

        // Small triangle, turn 6: vertex 0 at (0,2) [corner (0,0)] or at (0,4) [corner (0,4)]: same x, smaller y wins.
        val left = drag(PieceId.ST1, 6, false, 0.0, 3.0)
        assertLockedExactly(square.release(left, emptyList(), DP_PER_UNIT_R_1_5), left, pt(0, 2), "same x, y decides")

        // Small triangle, turn 4: vertex 0 at (2,4) [corner (0,4)] or at (4,4) [corner (4,4)]: smaller x wins.
        val bottom = drag(PieceId.ST1, 4, false, 3.0, 4.0)
        assertLockedExactly(square.release(bottom, emptyList(), DP_PER_UNIT_R_1_5), bottom, pt(2, 4), "bottom edge")

        // Large triangle 1 on the board; small triangle, turn 2: vertex 0 at (4,0) or at (3,1), both with one
        // anchored corner, both 0.71 from the release at (3.5, 0.5). The smaller y wins although its x is larger:
        // the order is y first.
        val diagonal = drag(PieceId.ST1, 2, false, 3.5, 0.5)
        assertLockedExactly(
            square.release(diagonal, listOf(SquarePlaced.LT1), DP_PER_UNIT_R_1_5), diagonal, pt(4, 0), "y before x",
        )
    }

    // ---------------------------------------------------------------------------------------------
    // A3 — "A piece that touches no outline corner and has no neighbour to tie to never locks."
    //      (F23 reading: no anchor within the lock distance.)
    // ---------------------------------------------------------------------------------------------

    // REQ-019.A3
    @Test
    fun a3_aPieceInTheMiddleOfTheEmptySilhouetteNeverLocks() {
        // The large triangle dropped well inside the empty square: its corners (1,1)(5,1)(3,3) are all more than
        // 1.4 from every outline corner, and the position would otherwise be "valid" - a lazy search that lets a
        // piece lock where it lies, or that treats the moving piece's own corners as anchors, locks it here.
        val middle = drag(PieceId.LT1, 0, false, 1.0, 1.0)
        assertHome(square.release(middle, emptyList(), DP_PER_UNIT_R_0_65), middle, "middle of the empty square")

        // A small triangle that lies completely inside the square, far from every corner.
        val inside = drag(PieceId.ST1, 0, false, 1.0, 1.5)
        assertHome(square.release(inside, emptyList(), DP_PER_UNIT_R_0_65), inside, "small triangle in the middle")
    }

    // REQ-019.A3
    @Test
    fun a3_noPieceLocksWhenNoAnchorIsWithinReachOnAnyDropAcrossTheBoard() {
        // Origins at half-integers: with the turns 0, 2, 4, 6 every corner of every piece lies at a half-integer
        // offset from the integer grid on which all outline corners and all placed corners lie, so each corner is at
        // least 0.707 (> R = 0.65) from every anchor: no anchor is within the lock distance, whatever the piece.
        val halfSteps = listOf(-0.5, 0.5, 1.5, 2.5, 3.5, 4.5)
        val boards = listOf(emptyList<PlacedPiece>(), listOf(SquarePlaced.LT1), listOf(SquarePlaced.LT1, SquarePlaced.SQ))
        for (board in boards) {
            val onBoard = board.map { it.piece }.toSet()
            for (piece in PieceId.values().filter { it !in onBoard }) {
                val mirrors = if (piece == PieceId.PG) listOf(false, true) else listOf(false)
                for (turn in listOf(0, 2, 4, 6)) {
                    for (mirrored in mirrors) {
                        for (x in halfSteps) {
                            for (y in halfSteps) {
                                val pose = drag(piece, turn, mirrored, x, y)
                                val outcome = square.release(pose, board, DP_PER_UNIT_R_0_65)
                                assertHome(outcome, pose, "$piece turn $turn mirrored $mirrored at ($x, $y), ${board.size} placed")
                            }
                        }
                    }
                }
            }
        }
    }

    // REQ-019.A3
    @Test
    fun a3_aPieceWithNothingToTieToNextToANeighbourStillNeverLocks() {
        // Large triangle 1 on the board. A small triangle in the open area below it has the neighbour's apex (2,2)
        // 1.1 away from its nearest corner and no outline corner in reach: nothing to tie to, so it goes home.
        val pose = drag(PieceId.ST1, 0, false, 1.0, 2.5)
        assertHome(square.release(pose, listOf(SquarePlaced.LT1), DP_PER_UNIT_R_0_65), pose, "open area next to a piece")
    }

    // REQ-019.A3
    @Test
    fun a3_everyLockTouchesAnOutlineCornerOrANeighbourCorner() {
        // Sweep: a grid of releases of the five pieces not yet on the board, in all 8 turns (and both mirrors of the
        // parallelogram), over a board holding the large triangle 1 and the square. Whenever a drop locks, at
        // least one corner of the locked piece lies exactly on an outline corner or on a corner of a placed piece
        // (REQ-019 Statement and Rules: "Anchor points are only ...").
        val board = listOf(SquarePlaced.LT1, SquarePlaced.SQ)
        val anchors: Set<ExactPoint> = Fixtures.SQUARE_OUTLINE_CORNERS +
            Fixtures.SQUARE.solution.filter { it.piece == PieceId.LT1 || it.piece == PieceId.SQ }.flatMap { it.polygon }
        val onBoard = board.map { it.piece }.toSet()
        var locks = 0
        for (piece in PieceId.values().filter { it !in onBoard }) {
            val mirrors = if (piece == PieceId.PG) listOf(false, true) else listOf(false)
            for (turn in 0..7) {
                for (mirrored in mirrors) {
                    for (i in 0..8) {
                        for (j in 0..8) {
                            val pose = drag(piece, turn, mirrored, -0.4 + 0.57 * i, -0.4 + 0.57 * j)
                            val outcome = square.release(pose, board, DP_PER_UNIT_R_0_65)
                            if (outcome is DropOutcome.Locked) {
                                locks++
                                assertTrue(
                                    "$piece turn $turn mirrored $mirrored released at (${pose.origin.x}, ${pose.origin.y}) " +
                                        "locked with no corner on an anchor: ${outcome.placed}",
                                    outcome.placed.corners.any { it in anchors },
                                )
                            }
                        }
                    }
                }
            }
        }
        // Non-vacuity: the sweep does contain drops that lock (otherwise it proves nothing about locks).
        assertTrue("the sweep produced only $locks locks", locks >= 20)
    }

    // REQ-019.A3 (TYPE-004: "The turn must match: a piece one 45 degree step off does not lock.")
    @Test
    fun a3_aPieceOneTurnStepOffDoesNotLock() {
        // Medium triangle at the top-left corner: turn 0 has its right angle at (0,0) and fits there exactly. One
        // step off (turn 1 or turn 7) its corners poke out of the square at that corner, and no other
        // position within 0.65 is valid, so the very same release goes home.
        val onTurn = drag(PieceId.MT, 0, false, 0.2, 0.1)
        assertLockedExactly(square.release(onTurn, emptyList(), DP_PER_UNIT_R_0_65), onTurn, pt(0, 0), "matching turn")
        for (turn in listOf(1, 7)) {
            val off = drag(PieceId.MT, turn, false, 0.2, 0.1)
            assertHome(square.release(off, emptyList(), DP_PER_UNIT_R_0_65), off, "turn $turn, one step off")
        }

        // The same in a hole: shapes-mini-2 with the medium triangle and a small triangle on the board; the free
        // hole is small triangle 2's triangle (2,2)(0,2)(1,1). Turn 4 fits it; turn 3 and turn 5 do not.
        val mini = DropResolver(Fixtures.MINI_2)
        val board = listOf(Mini2Placed.MT, Mini2Placed.ST1)
        val fits = drag(PieceId.ST2, 4, false, 2.2, 1.9)
        assertLockedExactly(mini.release(fits, board, DP_PER_UNIT_R_0_65), fits, pt(2, 2), "small triangle in the hole")
        for (turn in listOf(3, 5)) {
            val off = drag(PieceId.ST2, turn, false, 2.2, 1.9)
            assertHome(mini.release(off, board, DP_PER_UNIT_R_0_65), off, "hole, turn $turn, one step off")
        }
    }

    // REQ-019.A3 (TYPE-004: "The mirror must match: the search never tries the other mirror image")
    @Test
    fun a3_theOtherMirrorImageIsNeverTried() {
        // Unmirrored parallelogram, turn 0, vertex 0 at (0,4): fits the bottom-left corner. The mirrored
        // parallelogram at the same release has no valid position (it would stick out to the left); if the search
        // also tried the other mirror image it would lock here with the wrong mirror.
        val mirrored = drag(PieceId.PG, 0, true, 0.2, 3.8)
        assertHome(square.release(mirrored, emptyList(), DP_PER_UNIT_R_0_65), mirrored, "mirrored at the left corner")
        // And the other way round at the bottom-right corner.
        val plain = drag(PieceId.PG, 0, false, 3.8, 3.8)
        assertHome(square.release(plain, emptyList(), DP_PER_UNIT_R_0_65), plain, "unmirrored at the right corner")

        // Same in a hole. Six pieces in the shapes-square solution reflected left-right (x -> 4 - x) leave the hole
        // (4,4)(3,3)(1,3)(2,4), which only the MIRRORED parallelogram fills.
        val board = listOf(
            placed(PieceId.LT1, 0, false, 0, 0), // (0,0)(4,0)(2,2)
            placed(PieceId.LT2, 2, false, 4, 0), // (4,0)(4,4)(2,2)
            placed(PieceId.MT, 6, false, 0, 4), // (0,4)(0,2)(2,4)
            placed(PieceId.ST1, 6, false, 0, 2), // (0,2)(0,0)(1,1)
            placed(PieceId.SQ, 0, false, 0, 2), // (0,2)(1,1)(2,2)(1,3)
            placed(PieceId.ST2, 4, false, 3, 3), // (3,3)(1,3)(2,2)
        )
        val fills = drag(PieceId.PG, 0, true, 4.1, 3.9)
        assertLockedExactly(square.release(fills, board, DP_PER_UNIT_R_0_65), fills, pt(4, 4), "mirrored fills the hole")
        // The unmirrored parallelogram, in any of the 8 turns, released around the hole: never locks.
        for (turn in 0..7) {
            for (x in listOf(0.9, 1.9, 2.9, 3.9)) {
                for (y in listOf(2.9, 3.9)) {
                    val wrong = drag(PieceId.PG, turn, false, x, y)
                    assertHome(square.release(wrong, board, DP_PER_UNIT_R_0_65), wrong, "unmirrored, turn $turn at ($x, $y)")
                }
            }
        }
    }
}
