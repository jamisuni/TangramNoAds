package io.github.jamisuni.tangram.kernel.geometry

import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PieceShape
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Rational
import io.github.jamisuni.tangram.kernel.model.Turn
import io.github.jamisuni.tangram.kernel.model.localCorners
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SCAFFOLDING (TASK-002, WO-001): disposable unit tests of PieceShapes.kt and PieceGeometry.kt.
 * They are not acceptance tests and carry no acceptance IDs. Expected values are hand-derived from
 * Spec/03-puzzle-format.md §3 and cross-checked against tools/tangram_geom.py (not read from the golden;
 * the golden comparison is TASK-003b's).
 */
class PieceGeometryTest {

    private val allTurns = (0..7).map { Turn(it) }

    /** A point from a + b√2 coordinates: p(xa, xb, ya, yb). */
    private fun p(xa: Long, xb: Long, ya: Long, yb: Long) = ExactPoint(q2(xa, xb), q2(ya, yb))

    /** A point with whole-number coordinates. */
    private fun w(x: Long, y: Long) = pointOf(x, y)

    private fun abs(r: Rational): Rational = if (r.signum() < 0) -r else r

    /** Twice the signed shoelace sum; positive = clockwise on screen (y down). Exact, over Q2. */
    private fun shoelace2(points: List<ExactPoint>): Q2 {
        var sum = Q2.ZERO
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % points.size]
            sum += a.x * b.y - b.x * a.y
        }
        return sum
    }

    // ---- local shapes (TYPE-001, Spec/03 §3) ----

    @Test
    fun localCornersAreTheSpecTable() {
        assertEquals(listOf(w(0, 0), w(4, 0), w(2, 2)), PieceShape.LARGE_TRIANGLE.localCorners)
        assertEquals(listOf(w(0, 0), w(2, 0), w(0, 2)), PieceShape.MEDIUM_TRIANGLE.localCorners)
        assertEquals(listOf(w(0, 0), w(2, 0), w(1, 1)), PieceShape.SMALL_TRIANGLE.localCorners)
        assertEquals(listOf(w(0, 0), w(1, -1), w(2, 0), w(1, 1)), PieceShape.SQUARE.localCorners)
        assertEquals(listOf(w(0, 0), w(2, 0), w(3, -1), w(1, -1)), PieceShape.PARALLELOGRAM.localCorners)
    }

    @Test
    fun vertexZeroIsTheLocalOriginOfEveryShape() {
        for (shape in PieceShape.entries) {
            assertEquals(w(0, 0), shape.localCorners[0])
        }
    }

    // ---- area ----

    @Test
    fun areasAreTheSpecValuesAndTheSevenPiecesSumTo16() {
        assertEquals(Rational.of(4), PieceGeometry.area(PieceShape.LARGE_TRIANGLE))
        assertEquals(Rational.of(2), PieceGeometry.area(PieceShape.MEDIUM_TRIANGLE))
        assertEquals(Rational.of(1), PieceGeometry.area(PieceShape.SMALL_TRIANGLE))
        assertEquals(Rational.of(2), PieceGeometry.area(PieceShape.SQUARE))
        assertEquals(Rational.of(2), PieceGeometry.area(PieceShape.PARALLELOGRAM))
        var total = Rational.ZERO
        for (id in PieceId.entries) total += PieceGeometry.area(id.shape)
        assertEquals(Rational.of(16), total)
    }

    @Test
    fun areaEqualsTheExactShoelaceOfTheLocalCorners() {
        for (shape in PieceShape.entries) {
            val twice = shoelace2(shape.localCorners)
            assertEquals(Rational.ZERO, twice.b)
            assertEquals(PieceGeometry.area(shape) * Rational.of(2), abs(twice.a))
        }
    }

    // ---- offsets: the convention ----

    @Test
    fun turnZeroUnmirroredIsTheLocalShape() {
        for (shape in PieceShape.entries) {
            assertEquals(shape.localCorners, PieceGeometry.offsets(shape, Turn(0), false))
        }
    }

    @Test
    fun firstOffsetIsTheOriginForEveryShapeTurnAndMirror() {
        for (shape in PieceShape.entries) for (turn in allTurns) for (mirrored in listOf(false, true)) {
            assertEquals(w(0, 0), PieceGeometry.offsets(shape, turn, mirrored)[0])
        }
    }

    @Test
    fun positiveTurnIsClockwiseOnScreenWithYDown() {
        // One step = 45° clockwise: +x turns towards +y (down). ST (2,0),(1,1):
        val st = PieceShape.SMALL_TRIANGLE
        assertEquals(listOf(w(0, 0), p(0, 1, 0, 1), p(0, 0, 0, 1)), PieceGeometry.offsets(st, Turn(1), false))
        assertEquals(listOf(w(0, 0), w(0, 2), w(-1, 1)), PieceGeometry.offsets(st, Turn(2), false))
        assertEquals(listOf(w(0, 0), p(0, -1, 0, 1), p(0, -1, 0, 0)), PieceGeometry.offsets(st, Turn(3), false))
        assertEquals(listOf(w(0, 0), w(-2, 0), w(-1, -1)), PieceGeometry.offsets(st, Turn(4), false))
    }

    @Test
    fun turnFiveOfTheLargeTriangleNeedsRootTwoAtBothCoordinates() {
        // (4,0) -> (-2√2, -2√2); (2,2) -> (0, -2√2)
        assertEquals(
            listOf(w(0, 0), p(0, -2, 0, -2), p(0, 0, 0, -2)),
            PieceGeometry.offsets(PieceShape.LARGE_TRIANGLE, Turn(5), false),
        )
    }

    @Test
    fun turnFourNegatesEveryOffsetOfTurnZero() {
        for (shape in PieceShape.entries) {
            val t0 = PieceGeometry.offsets(shape, Turn(0), false)
            val t4 = PieceGeometry.offsets(shape, Turn(4), false)
            assertEquals(t0.map { ExactPoint(-it.x, -it.y) }, t4)
        }
    }

    @Test
    fun mirrorFlipsXOfTheLocalShapeAtTurnZero() {
        assertEquals(
            listOf(w(0, 0), w(-4, 0), w(-2, 2)),
            PieceGeometry.offsets(PieceShape.LARGE_TRIANGLE, Turn(0), true),
        )
        assertEquals(
            listOf(w(0, 0), w(-2, 0), w(-3, -1), w(-1, -1)),
            PieceGeometry.offsets(PieceShape.PARALLELOGRAM, Turn(0), true),
        )
    }

    @Test
    fun mirrorIsAppliedBeforeTheTurn() {
        // PG mirrored first: (0,0),(-2,0),(-3,-1),(-1,-1); then turn 2 (x,y) -> (-y, x).
        val mirroredThenTurned = PieceGeometry.offsets(PieceShape.PARALLELOGRAM, Turn(2), true)
        assertEquals(listOf(w(0, 0), w(0, -2), w(1, -3), w(1, -1)), mirroredThenTurned)
        // Turned first and then mirrored would give (-1, 3) for the third vertex instead of (1, -3).
        val turnedOnly = PieceGeometry.offsets(PieceShape.PARALLELOGRAM, Turn(2), false)
        assertEquals(listOf(w(0, 0), w(0, 2), w(1, 3), w(1, 1)), turnedOnly)
        val turnedThenMirrored = turnedOnly.map { ExactPoint(-it.x, it.y) }
        assertTrue(mirroredThenTurned != turnedThenMirrored)
    }

    @Test
    fun mirroredMediumTriangleAtTurnSevenUsesRootTwoOffsets() {
        // mirror: (0,0),(-2,0),(0,2); turn 7 (c = ½√2, s = -½√2): (-2,0) -> (-√2, √2); (0,2) -> (√2, √2)
        assertEquals(
            listOf(w(0, 0), p(0, -1, 0, 1), p(0, 1, 0, 1)),
            PieceGeometry.offsets(PieceShape.MEDIUM_TRIANGLE, Turn(7), true),
        )
    }

    @Test
    fun localOrientationIsClockwiseOnScreenForFourShapesAndCounterClockwiseForThePg() {
        // y down: a positive shoelace sum is clockwise on screen. The table's vertex order gives
        // LT, MT, ST, SQ clockwise and PG (0,0)(2,0)(3,-1)(1,-1) counter-clockwise.
        for (shape in PieceShape.entries) {
            val expected = if (shape == PieceShape.PARALLELOGRAM) -1 else 1
            assertEquals(expected, shoelace2(shape.localCorners).signum())
        }
    }

    @Test
    fun turnsKeepOrientationAndMirrorReversesIt() {
        for (shape in PieceShape.entries) {
            val local = shoelace2(shape.localCorners).signum()
            for (turn in allTurns) {
                assertEquals(local, shoelace2(PieceGeometry.offsets(shape, turn, false)).signum())
                assertEquals(-local, shoelace2(PieceGeometry.offsets(shape, turn, true)).signum())
            }
        }
    }

    @Test
    fun rotationsAndMirrorsKeepTheArea() {
        for (shape in PieceShape.entries) for (turn in allTurns) for (mirrored in listOf(false, true)) {
            val twice = shoelace2(PieceGeometry.offsets(shape, turn, mirrored))
            assertEquals(Rational.ZERO, twice.b)
            assertEquals(PieceGeometry.area(shape) * Rational.of(2), abs(twice.a))
        }
    }

    @Test
    fun samePieceShapeGivesSameOffsetsForBothIdsOfTheShape() {
        assertEquals(
            PieceGeometry.corners(PieceId.LT1, Turn(3), true, w(1, 1)),
            PieceGeometry.corners(PieceId.LT2, Turn(3), true, w(1, 1)),
        )
        assertEquals(
            PieceGeometry.corners(PieceId.ST1, Turn(6), false, w(0, 5)),
            PieceGeometry.corners(PieceId.ST2, Turn(6), false, w(0, 5)),
        )
    }

    // ---- corners ----

    @Test
    fun cornersAreOffsetsPlusAt() {
        // shapes-mini-2: MT turn 0 at (0,0) = (0,0)(2,0)(0,2); ST1 turn 2 at (2,0) = (2,0)(2,2)(1,1)
        assertEquals(listOf(w(0, 0), w(2, 0), w(0, 2)), PieceGeometry.corners(PieceId.MT, Turn(0), false, w(0, 0)))
        assertEquals(listOf(w(2, 0), w(2, 2), w(1, 1)), PieceGeometry.corners(PieceId.ST1, Turn(2), false, w(2, 0)))
        // ST2 turn 4 at (2,2) = (2,2)(0,2)(1,1)
        assertEquals(listOf(w(2, 2), w(0, 2), w(1, 1)), PieceGeometry.corners(PieceId.ST2, Turn(4), false, w(2, 2)))
    }

    @Test
    fun cornersWithAnIrrationalAtStayExact() {
        // at = (4 + √2, 4 − √2); SQ turn 1: offsets (0,0),(√2,0),(√2,√2),(0,√2)
        val at = p(4, 1, 4, -1)
        assertEquals(
            listOf(p(4, 1, 4, -1), p(4, 2, 4, -1), p(4, 2, 4, 0), p(4, 1, 4, 0)),
            PieceGeometry.corners(PieceId.SQ, Turn(1), false, at),
        )
    }

    @Test
    fun placedPieceCornersAreThePieceGeometryCorners() {
        val placed = PlacedPiece(PieceId.PG, Turn(5), true, p(1, 1, 2, 0))
        assertEquals(PieceGeometry.corners(PieceId.PG, Turn(5), true, p(1, 1, 2, 0)), placed.corners)
        assertEquals(p(1, 1, 2, 0), placed.corners[0])
        assertEquals(PieceId.PG, placed.piece)
        assertEquals(Turn(5), placed.turn)
        assertTrue(placed.mirrored)
    }

    // ---- poseOf ----

    @Test
    fun poseOfRoundTripsEveryPoseOfEveryPiece() {
        val ats = listOf(w(0, 0), w(3, 1), p(4, 1, 4, -1), p(-2, 0, 5, 2))
        for (id in PieceId.entries) for (turn in allTurns) for (mirrored in listOf(false, true)) for (at in ats) {
            val polygon = PieceGeometry.corners(id, turn, mirrored, at)
            val pose = PieceGeometry.poseOf(id, polygon)
            assertNotNull("$id turn=${turn.steps} mirrored=$mirrored at=$at", pose)
            assertEquals(id, pose!!.piece)
            assertEquals(polygon.toSet(), pose.corners.toSet())
            assertEquals(polygon.size, pose.corners.size)
        }
    }

    @Test
    fun poseOfRoundTripsTheExactPoseOfAPieceWithNoSymmetry() {
        // PG is chiral and has only a half-turn symmetry, so turns 0..3 are unique for it.
        for (steps in 0..3) for (mirrored in listOf(false, true)) {
            val pose = PlacedPiece(PieceId.PG, Turn(steps), mirrored, p(1, 1, 2, -1))
            assertEquals(pose, PieceGeometry.poseOf(PieceId.PG, pose.corners))
        }
    }

    @Test
    fun poseOfFindsTheMirroredPoseOfAChiralPiece() {
        val polygon = PieceGeometry.corners(PieceId.PG, Turn(1), true, w(2, 3))
        val pose = PieceGeometry.poseOf(PieceId.PG, polygon)
        assertNotNull(pose)
        assertTrue(pose!!.mirrored)
    }

    @Test
    fun poseOfIgnoresTheVertexOrderOfTheInput() {
        val polygon = PieceGeometry.corners(PieceId.MT, Turn(3), false, w(5, 5))
        val pose = PieceGeometry.poseOf(PieceId.MT, polygon)
        assertEquals(pose!!.corners.toSet(), PieceGeometry.poseOf(PieceId.MT, polygon.reversed())!!.corners.toSet())
        assertEquals(pose.corners.toSet(), PieceGeometry.poseOf(PieceId.MT, polygon.drop(1) + polygon.take(1))!!.corners.toSet())
    }

    @Test
    fun poseOfOnASymmetricPiecePicksUnmirroredFirstTurnAscendingThenFirstPolygonVertex() {
        // DA-6. SQ mirrored at turn 3 at (5,5) is the axis-aligned square (5,5)(5+√2,5)(5+√2,5−√2)(5,5−√2).
        // An axis-aligned square is an odd-turn SQ: unmirrored, turn 1 is the first that fits, with vertex 0
        // at the top-left corner (5, 5−√2) - the fourth polygon vertex, the first one that works as `at`.
        val polygon = PieceGeometry.corners(PieceId.SQ, Turn(3), true, w(5, 5))
        assertEquals(listOf(w(5, 5), p(5, 1, 5, 0), p(5, 1, 5, -1), p(5, 0, 5, -1)), polygon)
        val pose = PieceGeometry.poseOf(PieceId.SQ, polygon)
        assertEquals(PlacedPiece(PieceId.SQ, Turn(1), false, p(5, 0, 5, -1)), pose)
    }

    @Test
    fun poseOfPrefersTheUnmirroredPoseForAMirrorSymmetricShape() {
        // The large triangle is its own mirror image: the mirrored pose polygon is also an unmirrored one.
        val polygon = PieceGeometry.corners(PieceId.LT1, Turn(0), true, w(4, 0))
        assertEquals(listOf(w(4, 0), w(0, 0), w(2, 2)), polygon)
        val pose = PieceGeometry.poseOf(PieceId.LT1, polygon)
        assertEquals(false, pose!!.mirrored)
        assertEquals(polygon.toSet(), pose.corners.toSet())
    }

    @Test
    fun poseOfReturnsNullForAPolygonOfAnotherShape() {
        // The large triangle's corners are not a medium triangle in any pose.
        val lt = PieceGeometry.corners(PieceId.LT1, Turn(0), false, w(0, 0))
        assertNull(PieceGeometry.poseOf(PieceId.MT, lt))
        // A small triangle is not a medium triangle either (both are three-corner shapes).
        val st = PieceGeometry.corners(PieceId.ST1, Turn(0), false, w(0, 0))
        assertNull(PieceGeometry.poseOf(PieceId.MT, st))
        // The parallelogram is not the square.
        val pg = PieceGeometry.corners(PieceId.PG, Turn(0), false, w(0, 0))
        assertNull(PieceGeometry.poseOf(PieceId.SQ, pg))
    }

    @Test
    fun poseOfReturnsNullForTheWrongNumberOfCornersOrAShiftedCorner() {
        val mt = PieceGeometry.corners(PieceId.MT, Turn(0), false, w(0, 0))
        assertNull(PieceGeometry.poseOf(PieceId.MT, mt + w(5, 5)))
        assertNull(PieceGeometry.poseOf(PieceId.MT, mt.take(2)))
        assertNull(PieceGeometry.poseOf(PieceId.MT, emptyList()))
        // one corner moved by an exact √2 hair: still three points, not a medium triangle
        assertNull(PieceGeometry.poseOf(PieceId.MT, listOf(mt[0], mt[1], p(0, 1, 2, 0))))
        // a repeated corner is not the polygon, even with the right count
        assertNull(PieceGeometry.poseOf(PieceId.MT, listOf(mt[0], mt[1], mt[1])))
    }

    @Test
    fun poseOfReturnsNullForAScaledCopyOfTheShape() {
        val scaled = PieceShape.MEDIUM_TRIANGLE.localCorners.map { ExactPoint(it.x * q2(2), it.y * q2(2)) }
        assertNull(PieceGeometry.poseOf(PieceId.MT, scaled))
    }
}
