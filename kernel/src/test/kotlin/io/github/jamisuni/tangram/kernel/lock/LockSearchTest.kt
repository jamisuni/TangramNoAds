package io.github.jamisuni.tangram.kernel.lock

import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.READING_ORDER
import io.github.jamisuni.tangram.kernel.geometry.Silhouette
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.geometry.minus
import io.github.jamisuni.tangram.kernel.geometry.plus
import io.github.jamisuni.tangram.kernel.geometry.pointOf
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SCAFFOLDING (TASK-005b, WO-001 design §6, decisions DA-1, DA-2, DA-3): unit tests of `LockSearch`: R, the
 * score, the DA-1 tie-break worked example, dedupe by `at`, own corners excluded (F31), turn/mirror strictness,
 * validity, the window as a stop only (against a brute-force reference), and no exception for NaN/huge input.
 * No acceptance IDs.
 */
class LockSearchTest {

    private fun pt(x: Long, y: Long): ExactPoint = pointOf(x, y)

    private fun puzzle(id: String) = goldenPuzzles.first { it.id == id }

    // ---- lockDistance ----------------------------------------------------------------------------------

    @Test
    fun lockDistanceIsTheLargerOfPointSixFiveUnitsAndThirtyDp() {
        assertEquals(0.65, LockSearch.lockDistance(100.0), 0.0) // 30 / 100 = 0.3 < 0.65
        assertEquals(0.65, LockSearch.lockDistance(30.0 / 0.65), 1e-12) // the crossover
        assertEquals(0.75, LockSearch.lockDistance(40.0), 1e-15) // 30 / 40
        assertEquals(1.0, LockSearch.lockDistance(30.0), 0.0)
        assertEquals(3.0, LockSearch.lockDistance(10.0), 0.0)
        assertEquals(0.65, LockSearch.lockDistance(1000.0), 0.0)
    }

    @Test
    fun lockDistanceOfNonPositiveOrNaNInputIsPointSixFive() {
        for (bad in listOf(0.0, -0.0, -1.0, -100.0, Double.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertEquals("dpPerUnit = $bad", 0.65, LockSearch.lockDistance(bad), 0.0)
        }
    }

    @Test
    fun theConstantsAreTheTypeFourParameters() {
        assertEquals(0.65, LockSearch.BASE_DISTANCE, 0.0)
        assertEquals(30.0, LockSearch.MIN_DP, 0.0)
        assertEquals(0.04, LockSearch.SNUG_BONUS, 0.0)
        assertEquals(0.16, LockSearch.WINDOW, 0.0)
        assertEquals(1e-6, LockSearch.TOLERANCE, 0.0)
        assertEquals(1e-9, LockSearch.TIE_EPS, 0.0)
    }

    @Test
    fun scoreIsDistanceMinusFourHundredthsPerSnugCorner() {
        assertEquals(0.5 - 0.04 * 3, Lock(pt(2, 2), 0.5, 3).score, 1e-15)
        assertEquals(0.5, Lock(pt(0, 0), 0.5, 0).score, 0.0)
    }

    // ---- the DA-1 tie-break worked example (shapes-mini-1) ---------------------------------------------

    private val mini1 get() = puzzle("shapes-mini-1")

    /** shapes-mini-1 with its square placed; the two small-triangle holes are at (2,2) and (4,2) at turn 4. */
    private fun mini1Square(): List<PlacedPiece> = listOf(mini1.solutionPiece(PieceId.SQ).placed())

    @Test
    fun tieBreakWorkedExampleLeftHoleWinsByReadingOrder() {
        val lock = LockSearch.find(
            mini1.silhouette(), mini1Square(), PieceId.ST1, Turn(4), false, Vec2(3.0, 2.0), 1.2,
        )
        assertNotNull(lock)
        assertEquals(pt(2, 2), lock!!.at) // (2,2) before (4,2): same y, smaller x
        assertEquals(1.0, lock.distance, 1e-12)
        assertEquals(3, lock.cornersOnAnchors)
        assertEquals(1.0 - 0.04 * 3, lock.score, 1e-12)
    }

    @Test
    fun theNearerHoleWinsWhenTheDistancesDiffer() {
        val silhouette = mini1.silhouette()
        val placed = mini1Square()
        // 3.1: the right hole (4,2) is at 0.9, the left (2,2) at 1.1; both n = 3, so the nearer wins.
        val right = LockSearch.find(silhouette, placed, PieceId.ST1, Turn(4), false, Vec2(3.1, 2.0), 1.2)
        assertEquals(pt(4, 2), right!!.at)
        val left = LockSearch.find(silhouette, placed, PieceId.ST1, Turn(4), false, Vec2(2.9, 2.0), 1.2)
        assertEquals(pt(2, 2), left!!.at)
    }

    @Test
    fun onlyHolesWithinRAreCandidates() {
        val silhouette = mini1.silhouette()
        // R = 0.65 around (3.4, 2): only the right hole (d = 0.6); the left one (d = 1.4) is out of reach.
        assertEquals(pt(4, 2), LockSearch.find(silhouette, mini1Square(), PieceId.ST1, Turn(4), false, Vec2(3.4, 2.0), 0.65)!!.at)
        // Around (3, 2) both holes are at 1.0: nothing within 0.65.
        assertNull(LockSearch.find(silhouette, mini1Square(), PieceId.ST1, Turn(4), false, Vec2(3.0, 2.0), 0.65))
    }

    @Test
    fun theResultDoesNotDependOnTheOrderOfThePlacedList() {
        for (puzzle in goldenPuzzles) {
            val silhouette = puzzle.silhouette()
            for (n in 1 until puzzle.buildOrder.size) {
                val placed = puzzle.buildOrder.take(n).map { puzzle.solutionPiece(it).placed() }
                val next = puzzle.solutionPiece(puzzle.buildOrder[n])
                val exact = next.pose.at.vec()
                for (dx in listOf(-0.4, 0.0, 0.3)) {
                    val origin = Vec2(exact.x + dx, exact.y + 0.2)
                    val forward = LockSearch.find(silhouette, placed, next.piece, next.pose.turn, next.pose.mirrored, origin, 1.2)
                    val reversed = LockSearch.find(silhouette, placed.reversed(), next.piece, next.pose.turn, next.pose.mirrored, origin, 1.2)
                    assertEquals("${puzzle.id} after $n pieces", forward, reversed)
                }
            }
        }
    }

    // ---- dedupe by `at` ---------------------------------------------------------------------------------

    @Test
    fun theSameAtFromSeveralCornerAnchorPairsIsOneCandidate() {
        val silhouette = mini1.silhouette()
        val placed = mini1Square()
        val offsets = PieceGeometry.offsets(PieceId.ST1.shape, Turn(4), false)
        val anchors = HashSet<ExactPoint>().apply {
            addAll(silhouette.outlineCorners)
            for (p in placed) addAll(p.corners)
        }
        val pairs = offsets.sumOf { o -> anchors.count { a -> a - o == pt(2, 2) } }
        assertEquals("(2,2) is reached from three corner/anchor pairs", 3, pairs)

        val candidates = LockSearch.candidates(anchors, offsets, Vec2(3.0, 2.0), 1.2)
        assertEquals("no `at` twice", candidates.size, candidates.map { it.at }.toSet().size)
        assertEquals(1, candidates.count { it.at == pt(2, 2) })
        assertEquals(1, candidates.count { it.at == pt(4, 2) })
        // The same anchors listed twice change nothing.
        val twice = LockSearch.candidates(anchors.toList() + anchors.toList(), offsets, Vec2(3.0, 2.0), 1.2)
        assertEquals(candidates.map { it.at }, twice.map { it.at })
    }

    @Test
    fun candidatesAreOrderedByDistanceThenReadingOrderAndStayWithinR() {
        val silhouette = mini1.silhouette()
        val placed = mini1Square()
        val offsets = PieceGeometry.offsets(PieceId.ST1.shape, Turn(4), false)
        val anchors = silhouette.outlineCorners + placed.flatMap { it.corners }
        val candidates = LockSearch.candidates(anchors, offsets, Vec2(3.0, 2.0), 1.5)
        assertTrue(candidates.size >= 2)
        for (c in candidates) assertTrue(c.distance <= 1.5)
        for (i in 1 until candidates.size) {
            val a = candidates[i - 1]
            val b = candidates[i]
            assertTrue(a.distance <= b.distance)
            if (a.distance == b.distance) assertTrue(READING_ORDER.compare(a.at, b.at) < 0)
        }
        // The exact tie of the worked example: both holes at distance 1, the left one first.
        val tied = candidates.filter { it.distance == 1.0 }.map { it.at }
        assertTrue(tied.indexOf(pt(2, 2)) in 0 until tied.indexOf(pt(4, 2)))
    }

    // ---- own corners excluded (F31) --------------------------------------------------------------------

    private val square4 get() = puzzle("shapes-square")

    @Test
    fun theMovingPiecesOwnCornersAreNoAnchors() {
        // ST1 sits at (1,1) turn 0 (corners (1,1) (3,1) (2,2)), inside the 4x4 square. Picked up again, it has
        // no anchor within reach but its own corners: the search must NOT lock it back onto itself.
        val own = PlacedPiece(PieceId.ST1, Turn(0), false, pt(1, 1))
        val silhouette = square4.silhouette()
        assertNull(LockSearch.find(silhouette, listOf(own), PieceId.ST1, Turn(0), false, Vec2(1.2, 1.1), 0.65))
        // Another piece, same board and pose: ST1's corners ARE anchors for it (and its area an obstacle).
        // ST2 at turn 4 at (3,1): corners (3,1) (1,1) (2,0); (3,1) and (1,1) are ST1's corners, flush along y = 1.
        val lock = LockSearch.find(silhouette, listOf(own), PieceId.ST2, Turn(4), false, Vec2(3.1, 1.1), 0.65)
        assertNotNull(lock)
        assertEquals(pt(3, 1), lock!!.at)
        assertEquals(2, lock.cornersOnAnchors)
    }

    @Test
    fun aPlacedPieceIsAnObstacleToAnotherPieceButNotToItself() {
        val st1 = PlacedPiece(PieceId.ST1, Turn(0), false, pt(1, 1))
        val silhouette = square4.silhouette()
        // ST2 dropped onto ST1's spot does not lock there: overlap (its only candidate in reach is (1,1)).
        assertNull(LockSearch.find(silhouette, listOf(st1), PieceId.ST2, Turn(0), false, Vec2(1.1, 1.0), 0.3))
        // ST1 itself dropped onto its own old spot: the placed list holds it, but it is not an obstacle to
        // itself. With the other pieces' corners as anchors it locks back (here: against ST2 at (3,1)).
        val st2 = PlacedPiece(PieceId.ST2, Turn(4), false, pt(3, 1))
        val relock = LockSearch.find(silhouette, listOf(st1, st2), PieceId.ST1, Turn(0), false, Vec2(1.1, 1.0), 0.3)
        assertEquals(pt(1, 1), relock!!.at)
    }

    // ---- turn / mirror strictness (TYPE-004) -----------------------------------------------------------

    @Test
    fun aTriangleOneStepOffInTurnNeverLocks() {
        // shapes-mini-2: MT and ST1 placed, ST2's hole at turn 4 (corners (2,2) (0,2) (1,1)).
        val puzzle = puzzle("shapes-mini-2")
        val board = listOf(PieceId.MT, PieceId.ST1).map { puzzle.solutionPiece(it).placed() }
        val silhouette = puzzle.silhouette()
        val origin = Vec2(2.0, 2.0)
        assertEquals(pt(2, 2), LockSearch.find(silhouette, board, PieceId.ST2, Turn(4), false, origin, 0.65)!!.at)
        assertNull(LockSearch.find(silhouette, board, PieceId.ST2, Turn(3), false, origin, 1.2))
        assertNull(LockSearch.find(silhouette, board, PieceId.ST2, Turn(5), false, origin, 1.2))
        // The search tries the given mirror only. The mirror image of the triangle at turn 4 happens to cover the
        // same hole, but only at (0,2): out of reach at R = 1.2, found only when R reaches it (a symmetric shape).
        assertNull(LockSearch.find(silhouette, board, PieceId.ST2, Turn(4), true, origin, 1.2))
        assertEquals(pt(0, 2), LockSearch.find(silhouette, board, PieceId.ST2, Turn(4), true, origin, 2.5)!!.at)
    }

    @Test
    fun theParallelogramOtherMirrorNeverLocksIntoItsHole() {
        val puzzle = square4
        val truth = puzzle.solution.first { it.piece == PieceId.PG }
        val board = puzzle.solution.filter { it.piece != PieceId.PG }.map { it.placed() }
        val silhouette = puzzle.silhouette()
        for (steps in 0..7) {
            val lock = LockSearch.find(
                silhouette, board, PieceId.PG, Turn(steps), !truth.pose.mirrored, truth.pose.at.vec(), 1.2,
            )
            assertNull("PG other mirror, turn $steps", lock)
        }
        assertNotNull(
            LockSearch.find(silhouette, board, PieceId.PG, truth.pose.turn, truth.pose.mirrored, truth.pose.at.vec(), 0.65),
        )
    }

    // ---- validity: inside the silhouette, no overlap ----------------------------------------------------

    @Test
    fun aPieceThatWouldStickOutOfTheSilhouetteDoesNotLock() {
        val silhouette = square4.silhouette()
        // ST1 at turn 0 at the corner (0,0) lies inside the 4x4 square: locks.
        assertEquals(pt(0, 0), LockSearch.find(silhouette, emptyList(), PieceId.ST1, Turn(0), false, Vec2(0.1, 0.1), 0.65)!!.at)
        // At turn 4 the same corner would put it at (0,0) (0,0) (-2,0) (-1,-1): outside, the only candidate in reach.
        assertNull(LockSearch.find(silhouette, emptyList(), PieceId.ST1, Turn(4), false, Vec2(0.1, 0.1), 0.65))
    }

    @Test
    fun noAnchorWithinReachGivesNull() {
        // No anchor within reach, at kernel level: a piece in the middle of an empty 4x4.
        assertNull(LockSearch.find(square4.silhouette(), emptyList(), PieceId.ST1, Turn(0), false, Vec2(1.8, 1.9), 0.65))
    }

    // ---- the window is a stop only, against a brute-force reference ---------------------------------------

    /**
     * An independent reference of TYPE-004 without the window and without sorting: every (offset, anchor) pair
     * within R, validated, then the lowest score, ties by distance, then reading order.
     */
    private fun reference(
        silhouette: Silhouette,
        placed: List<PlacedPiece>,
        piece: PieceId,
        turn: Turn,
        mirrored: Boolean,
        origin: Vec2,
        r: Double,
    ): Lock? {
        val valid = validLocks(silhouette, placed, piece, turn, mirrored, origin, r)
        if (valid.isEmpty()) return null
        val minScore = valid.minOf { it.score }
        val tiedScore = valid.filter { it.score <= minScore + 1e-9 }
        val minDistance = tiedScore.minOf { it.distance }
        return tiedScore.filter { it.distance <= minDistance + 1e-9 }.minWithOrNull { x, y -> READING_ORDER.compare(x.at, y.at) }
    }

    /** Every valid spot within R, one per exact `at`, each with its distance and snug count. No window, no stop. */
    private fun validLocks(
        silhouette: Silhouette, placed: List<PlacedPiece>, piece: PieceId, turn: Turn, mirrored: Boolean, origin: Vec2, r: Double,
    ): List<Lock> {
        val others = placed.filter { it.piece != piece }
        val offsets = PieceGeometry.offsets(piece.shape, turn, mirrored)
        val anchors = (silhouette.outlineCorners + others.flatMap { it.corners }).toSet()
        val valid = LinkedHashMap<ExactPoint, Lock>()
        for (o in offsets) {
            for (a in anchors) {
                val at = a - o
                if (at in valid) continue
                val d = Math.hypot(at.x.toDouble() - origin.x, at.y.toDouble() - origin.y)
                if (d > r) continue
                val fit = LockSearch.fitAt(silhouette, others, piece, turn, mirrored, at)
                if (!(fit.insideDeficit <= 1e-6 && fit.maxOverlap <= 1e-6)) continue
                valid[at] = Lock(at, d, offsets.count { (it + at) in anchors })
            }
        }
        return valid.values.toList()
    }

    @Test
    fun theWindowOnlyStopsTheScanItNeverChangesTheWinner() {
        var compared = 0
        var found = 0
        var winnerNotNearest = 0
        var validBeyondWindow = 0
        // Coarse grid for every turn and mirror; fine grid (0.05) at the true pose only, where several valid
        // spots with different snug counts lie within 0.12 of each other (the score can pick a farther one).
        val coarse = listOf(-0.5, -0.25, 0.0, 0.25, 0.5)
        val fine = (-12..12).map { it * 0.05 }
        for (puzzle in goldenPuzzles) {
            val silhouette = puzzle.silhouette()
            val placed = ArrayList<PlacedPiece>()
            for (id in puzzle.buildOrder) {
                val truth = puzzle.solutionPiece(id)
                val exact = truth.pose.at.vec()
                for (mirrored in listOf(false, true)) {
                    for (steps in 0..7) {
                        val isTruePose = mirrored == truth.pose.mirrored && steps == truth.pose.turn.steps
                        val grid = if (isTruePose) fine else coarse
                        for (dx in grid) for (dy in grid) for (r in listOf(0.65, 1.2)) {
                            val origin = Vec2(exact.x + dx, exact.y + dy)
                            val turn = Turn(steps)
                            val got = LockSearch.find(silhouette, placed, id, turn, mirrored, origin, r)
                            val valid = validLocks(silhouette, placed, id, turn, mirrored, origin, r)
                            val want = reference(silhouette, placed, id, turn, mirrored, origin, r)
                            compared++
                            assertEquals("${puzzle.id} $id turn=$steps mirrored=$mirrored origin=$origin R=$r", want, got)
                            if (got != null) {
                                found++
                                // Is the winner the nearest valid candidate? Is there a valid one beyond the window?
                                val nearest = valid.minOf { it.distance }
                                if (got.distance > nearest + 1e-9) winnerNotNearest++
                                if (valid.any { it.distance > nearest + LockSearch.WINDOW }) validBeyondWindow++
                            }
                        }
                    }
                }
                placed += truth.placed()
            }
        }
        assertTrue("compared $compared cases", compared > 50_000)
        assertTrue("some cases lock ($found)", found > 1_000)
        // Non-vacuous: the sweep holds cases where the score picks a farther candidate, and cases where a valid
        // candidate lies beyond the 0.16 window (which the stop condition skips without changing the winner).
        assertTrue("winner is not the nearest valid candidate in $winnerNotNearest cases", winnerNotNearest > 0)
        assertTrue("valid candidate beyond the window in $validBeyondWindow cases", validBeyondWindow > 0)
        println("window sweep: compared=$compared locked=$found winnerNotNearest=$winnerNotNearest validBeyondWindow=$validBeyondWindow")
    }

    // ---- no exception for NaN or huge input (G-10) -------------------------------------------------------

    @Test
    fun nanOrHugeOriginGivesNullAndNeverThrows() {
        val puzzle = square4
        val silhouette = puzzle.silhouette()
        val board = puzzle.buildOrder.take(3).map { puzzle.solutionPiece(it).placed() }
        val bad = listOf(
            Vec2(Double.NaN, Double.NaN), Vec2(Double.NaN, 0.0), Vec2(0.0, Double.NaN),
            Vec2(1e300, 1e300), Vec2(-1e300, 1e300), Vec2(Double.MAX_VALUE, Double.MAX_VALUE),
            Vec2(Double.POSITIVE_INFINITY, 0.0), Vec2(0.0, Double.NEGATIVE_INFINITY),
            Vec2(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY), Vec2(1e18, -1e18), Vec2(1e9, 1e9),
        )
        for (origin in bad) {
            for (piece in PieceId.values()) {
                for (steps in listOf(0, 3, 7)) {
                    assertNull("$origin $piece", LockSearch.find(silhouette, board, piece, Turn(steps), steps == 3, origin, 0.65))
                    assertNull("$origin $piece empty board", LockSearch.find(silhouette, emptyList(), piece, Turn(steps), false, origin, 0.65))
                }
            }
        }
    }

    @Test
    fun aNanOrNonPositiveLockDistanceFindsNothingAndASafeOneFindsTheSpot() {
        val puzzle = square4
        val silhouette = puzzle.silhouette()
        val truth = puzzle.solution.first { it.piece == PieceId.LT1 }
        val exact = truth.pose.at.vec()
        assertNull(LockSearch.find(silhouette, emptyList(), PieceId.LT1, truth.pose.turn, truth.pose.mirrored, exact, Double.NaN))
        assertNull(LockSearch.find(silhouette, emptyList(), PieceId.LT1, truth.pose.turn, truth.pose.mirrored, exact, -1.0))
        // A distance read from a bad layout scale is the safe 0.65, which does find the true spot.
        val r = LockSearch.lockDistance(Double.NaN)
        assertEquals(truth.pose.at, LockSearch.find(silhouette, emptyList(), PieceId.LT1, truth.pose.turn, truth.pose.mirrored, exact, r)!!.at)
    }
}
