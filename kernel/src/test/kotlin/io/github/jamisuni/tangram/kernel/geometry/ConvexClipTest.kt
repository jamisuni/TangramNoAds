package io.github.jamisuni.tangram.kernel.geometry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SCAFFOLDING (TASK-005a, WO-001): disposable unit tests of ConvexClip.kt (design WO-001 §6, the clip
 * contract). They are not acceptance tests and carry no acceptance IDs. Expected areas are hand-derived;
 * the rectangle sweep uses an interval-overlap oracle that is independent of the clipper.
 */
class ConvexClipTest {

    private val tol = 1e-12
    private val s2 = Math.sqrt(2.0)

    private fun v(x: Double, y: Double) = Vec2(x, y)

    private fun rect(x1: Double, y1: Double, x2: Double, y2: Double) =
        listOf(v(x1, y1), v(x2, y1), v(x2, y2), v(x1, y2))

    /** Both orientations of [p]: as given and reversed. */
    private fun bothWays(p: List<Vec2>) = listOf(p, p.reversed())

    /** `area(a, b)` is finite and equal to [expected] within [eps], for every cw/ccw combination and both argument orders. */
    private fun assertArea(expected: Double, a: List<Vec2>, b: List<Vec2>, eps: Double = tol) {
        for (pa in bothWays(a)) for (pb in bothWays(b)) {
            val ab = ConvexClip.intersectionArea(pa, pb)
            val ba = ConvexClip.intersectionArea(pb, pa)
            assertTrue("not finite: $ab", ab.isFinite())
            assertTrue("not finite: $ba", ba.isFinite())
            assertEquals("a∩b of $pa and $pb", expected, ab, eps)
            assertEquals("b∩a of $pb and $pa", expected, ba, eps)
        }
    }

    // ---- degenerate contact: the cases every lock decision runs through ----

    @Test
    fun flushSharedFullEdgeIsZero() {
        // Two unit squares, the right edge of one is the left edge of the other.
        assertArea(0.0, rect(0.0, 0.0, 1.0, 1.0), rect(1.0, 0.0, 2.0, 1.0))
        // Above / below, too.
        assertArea(0.0, rect(0.0, 0.0, 1.0, 1.0), rect(0.0, 1.0, 1.0, 2.0))
    }

    @Test
    fun flushSharedPartialEdgeIsZero() {
        assertArea(0.0, rect(0.0, 0.0, 1.0, 1.0), rect(1.0, 0.5, 2.0, 1.5))
        // A small square on a long edge (collinear edges of different length, touching from outside).
        assertArea(0.0, rect(0.0, 0.0, 4.0, 2.0), rect(1.0, 2.0, 2.0, 3.0))
    }

    @Test
    fun touchingAtASingleVertexIsZero() {
        assertArea(0.0, rect(0.0, 0.0, 1.0, 1.0), rect(1.0, 1.0, 2.0, 2.0))
        // A triangle apex on a square's corner and on the middle of its edge.
        val tri = listOf(v(0.0, 0.0), v(2.0, 0.0), v(1.0, -1.0))
        assertArea(0.0, rect(0.0, 0.0, 2.0, 2.0), tri) // shares the whole edge y = 0 from outside
        assertArea(0.0, rect(0.0, 0.0, 2.0, 2.0), listOf(v(2.0, 2.0), v(3.0, 2.0), v(3.0, 3.0)))
        assertArea(0.0, rect(0.0, 0.0, 2.0, 2.0), listOf(v(1.0, 2.0), v(0.0, 3.0), v(2.0, 3.0)))
    }

    @Test
    fun identicalPolygonsGiveTheFullArea() {
        assertArea(1.0, rect(0.0, 0.0, 1.0, 1.0), rect(0.0, 0.0, 1.0, 1.0))
        val tri = listOf(v(0.0, 0.0), v(2.0, 0.0), v(0.0, 2.0))
        assertArea(2.0, tri, tri)
        val roof = listOf(v(-s2, 0.0), v(s2, 0.0), v(0.0, s2))
        assertArea(2.0, roof, roof)
    }

    @Test
    fun oneInsideTheOtherGivesTheSmallerArea() {
        val big = rect(0.0, 0.0, 4.0, 4.0)
        val small = rect(1.0, 1.0, 2.0, 3.0)
        assertArea(2.0, big, small)
        // Inside with edges on the boundary (collinear edges, containment).
        assertArea(4.0, big, rect(0.0, 0.0, 2.0, 2.0))
        // A triangle inside a square, one corner on the square's corner.
        assertArea(2.0, rect(0.0, 0.0, 2.0, 2.0), listOf(v(0.0, 0.0), v(2.0, 0.0), v(0.0, 2.0)))
    }

    @Test
    fun collinearOverlappingEdgesGiveTheRealOverlap() {
        // Share a stretch of the bottom line y = 0 and overlap in [1,2] x [0,1].
        assertArea(1.0, rect(0.0, 0.0, 2.0, 2.0), rect(1.0, 0.0, 3.0, 1.0))
        // Same line, same side, one edge contained in the other.
        assertArea(1.5, rect(0.0, 0.0, 3.0, 1.0), rect(1.0, 0.0, 2.5, 5.0))
        // Edge-to-edge along a 45 degree line with different lengths.
        val big = listOf(v(0.0, 0.0), v(4.0, 0.0), v(0.0, 4.0))
        val small = listOf(v(1.0, 3.0), v(3.0, 1.0), v(0.0, 0.0)) // its edge (3,1)-(1,3) lies on big's hypotenuse x + y = 4
        // Hand-derived: x + y <= 4 holds for all of small, so it is inside big; its area is |3*3 - 1*1| / 2 = 4.
        assertArea(4.0, big, small)
    }

    @Test
    fun partialOverlapsHaveTheirHandDerivedAreas() {
        // Square [0.5,1.5]^2 against the triangle x, y >= 0, x + y <= 2: a right triangle of legs 1.
        val tri = listOf(v(0.0, 0.0), v(2.0, 0.0), v(0.0, 2.0))
        assertArea(0.5, tri, rect(0.5, 0.5, 1.5, 1.5))
        // Two unit squares offset by (0.5, 0.5): overlap 0.25.
        assertArea(0.25, rect(0.0, 0.0, 1.0, 1.0), rect(0.5, 0.5, 1.5, 1.5))
        // Diamond (45 degrees) against a square: the diamond |x| + |y| <= 1 cut by x >= 0 gives half, 1.
        val diamond = listOf(v(1.0, 0.0), v(0.0, 1.0), v(-1.0, 0.0), v(0.0, -1.0))
        assertArea(1.0, diamond, rect(0.0, -2.0, 2.0, 2.0))
    }

    // ---- < 3 vertices and zero area ----

    @Test
    fun fewerThanThreeVerticesIsZero() {
        val sq = rect(0.0, 0.0, 1.0, 1.0)
        val none = emptyList<Vec2>()
        val one = listOf(v(0.5, 0.5))
        val two = listOf(v(0.0, 0.0), v(1.0, 1.0))
        for (degenerate in listOf(none, one, two)) {
            assertEquals(0.0, ConvexClip.intersectionArea(degenerate, sq), 0.0)
            assertEquals(0.0, ConvexClip.intersectionArea(sq, degenerate), 0.0)
            assertEquals(0.0, ConvexClip.intersectionArea(degenerate, degenerate), 0.0)
        }
    }

    @Test
    fun zeroAreaPolygonsGiveZero() {
        val sq = rect(0.0, 0.0, 2.0, 2.0)
        // Three collinear points, a segment inside the square.
        val line = listOf(v(0.5, 0.5), v(1.0, 1.0), v(1.5, 1.5))
        assertArea(0.0, sq, line)
        // Three equal points.
        assertArea(0.0, sq, listOf(v(1.0, 1.0), v(1.0, 1.0), v(1.0, 1.0)))
        // A zero-width rectangle lying along the square's edge, and one lying across it.
        assertArea(0.0, sq, rect(0.0, 0.0, 2.0, 0.0))
        assertArea(0.0, sq, rect(1.0, -1.0, 1.0, 3.0))
        // Two degenerate polygons.
        assertArea(0.0, line, line)
    }

    // ---- both orientations, symmetry ----

    @Test
    fun orientationOfEitherPolygonDoesNotChangeTheArea() {
        // assertArea already runs the four cw/ccw combinations; pin an explicit cw vs ccw pair as well.
        val ccwSquare = listOf(v(0.0, 0.0), v(2.0, 0.0), v(2.0, 2.0), v(0.0, 2.0))
        val cwSquare = ccwSquare.reversed()
        val tri = listOf(v(1.0, 1.0), v(3.0, 1.0), v(1.0, 3.0))
        val expected = ConvexClip.intersectionArea(ccwSquare, tri)
        assertEquals(1.0, expected, tol) // [1,2]^2 lies wholly inside the triangle (x + y <= 4)
        assertEquals(expected, ConvexClip.intersectionArea(cwSquare, tri), tol)
        assertEquals(expected, ConvexClip.intersectionArea(ccwSquare, tri.reversed()), tol)
        assertEquals(expected, ConvexClip.intersectionArea(cwSquare, tri.reversed()), tol)
    }

    // ---- 45 degree edges and sqrt(2) coordinates (the house case) ----

    @Test
    fun roofFlushOnItsSquareIsZero() {
        // Roof: base on y = 0 from -sqrt2 to sqrt2, apex (0, sqrt2): 45 degree edges, area 2.
        val roof = listOf(v(-s2, 0.0), v(s2, 0.0), v(0.0, s2))
        // Square below the roof, side 2*sqrt2, sharing the roof's base edge.
        val square = rect(-s2, -2 * s2, s2, 0.0)
        assertArea(0.0, roof, square)
        assertArea(2.0, roof, roof)
        assertArea(8.0, square, square)
    }

    @Test
    fun roofSunkIntoItsSquareGivesTheTrapezoid() {
        val square = rect(-s2, -2 * s2, s2, 0.0)
        // Roof moved down by sqrt2 / 2: the part below y = 0 is a trapezoid, parallel sides 2*sqrt2 and sqrt2, height sqrt2/2.
        val roof = listOf(v(-s2, -s2 / 2), v(s2, -s2 / 2), v(0.0, s2 / 2))
        assertArea(1.5, roof, square)
    }

    @Test
    fun rotatedTrianglesSharingAnEdgeWithRoundingNoiseGiveZero() {
        // The two halves of a square cut along its diagonal, both turned by 45 degrees, but with the
        // rotation computed two ways so the shared (now irrational) edge differs by an ulp or so.
        val p0 = v(0.0, 0.0)
        val p1 = v(2.0, 0.0)
        val p2 = v(0.0, 2.0)
        val p3 = v(2.0, 2.0)
        val half = Math.sqrt(0.5)
        fun rot(p: Vec2, c: Double, s: Double) = v(p.x * c - p.y * s, p.x * s + p.y * c)
        val a = listOf(p0, p1, p2).map { rot(it, Math.cos(Math.PI / 4), Math.sin(Math.PI / 4)) }
        val b = listOf(p1, p3, p2).map { rot(it, half, half) }
        assertArea(0.0, a, b)
        // Same triangle, noisy copy of itself: full area 2.
        val aNoisy = listOf(p0, p1, p2).map { rot(it, half, half) }
        assertArea(2.0, a, aNoisy, 1e-9)
    }

    @Test
    fun noiseSizedPenetrationAndGapStayNoiseSized() {
        // Two unit squares at flush distance plus/minus noise: the area stays at noise level, never NaN.
        for (noise in doubleArrayOf(0.0, 1e-16, -1e-16, 1e-14, -1e-14, 1e-12, -1e-12, 1e-10, -1e-10)) {
            val a = rect(0.0, 0.0, 1.0, 1.0)
            val b = rect(1.0 - noise, 0.0, 2.0 - noise, 1.0)
            for (pa in bothWays(a)) for (pb in bothWays(b)) {
                val area = ConvexClip.intersectionArea(pa, pb)
                assertTrue("not finite at noise $noise", area.isFinite())
                assertTrue("area $area too large at noise $noise", area <= Math.abs(noise) + 1e-15)
                assertTrue("area $area negative at noise $noise", area >= 0.0)
            }
        }
    }

    @Test
    fun gapWithinTheSideToleranceDoesNotExtrapolate() {
        // The clamp of t: with a 1e-12 gap the crossing would lie beyond the subject's edge; the clamp keeps
        // the clipped polygon degenerate (area 0), where an unclamped crossing would add a 1e-12 sliver.
        val a = rect(0.0, 0.0, 1.0, 1.0)
        val b = rect(1.0 + 1e-12, 0.0, 2.0, 1.0)
        assertEquals(0.0, ConvexClip.intersectionArea(a, b), 1e-15)
        assertEquals(0.0, ConvexClip.intersectionArea(b, a), 1e-15)
    }

    // ---- the textbook-port hazard ----

    @Test
    fun collinearEdgesWouldDivideByZeroInATextbookPortButHereAreFinite() {
        // The textbook line-line intersection divides by cross(dirP, dirS), which is exactly 0 for collinear edges.
        val dirP = v(1.0, 0.0)
        val dirS = v(2.0, 0.0)
        val denom = dirP.x * dirS.y - dirP.y * dirS.x
        assertEquals(0.0, denom, 0.0)
        val textbook = (1.0 * dirS.y - 0.0 * dirS.x) / denom // t numerator 0 / denom 0
        assertTrue("sanity: the textbook form really is NaN here", textbook.isNaN())

        // The same inputs, fed to ConvexClip: collinear edges everywhere (flush, shared partial edge, identical,
        // contained with shared edges), every result finite.
        val cases = listOf(
            rect(0.0, 0.0, 1.0, 1.0) to rect(1.0, 0.0, 2.0, 1.0),
            rect(0.0, 0.0, 1.0, 1.0) to rect(0.0, 0.0, 1.0, 1.0),
            rect(0.0, 0.0, 2.0, 2.0) to rect(0.0, 0.0, 1.0, 1.0),
            rect(0.0, 0.0, 2.0, 1.0) to rect(1.0, 0.0, 3.0, 1.0),
            rect(0.0, 0.0, 1.0, 1.0) to rect(1.0, 1.0, 2.0, 2.0),
        )
        for ((a, b) in cases) for (pa in bothWays(a)) for (pb in bothWays(b)) {
            val area = ConvexClip.intersectionArea(pa, pb)
            assertFalse("NaN for $pa and $pb", area.isNaN())
            assertTrue("not finite for $pa and $pb", area.isFinite())
            assertTrue("negative area $area", area >= 0.0)
        }
    }

    @Test
    fun nonFiniteInputNeverLeaksNaNOrInfinity() {
        val sq = rect(0.0, 0.0, 1.0, 1.0)
        val bad = listOf(v(Double.NaN, 0.0), v(1.0, 0.0), v(0.0, 1.0))
        val inf = listOf(v(Double.POSITIVE_INFINITY, 0.0), v(1.0, 0.0), v(0.0, 1.0))
        for (poly in listOf(bad, inf)) {
            assertTrue(ConvexClip.intersectionArea(sq, poly).isFinite())
            assertTrue(ConvexClip.intersectionArea(poly, sq).isFinite())
        }
    }

    // ---- sweep: every pair of integer rectangles on a 0..3 grid (flush, corner, collinear, nested, disjoint) ----

    @Test
    fun everyIntegerRectanglePairMatchesTheIntervalOverlapOracle() {
        val rects = ArrayList<DoubleArray>()
        for (x1 in 0..2) for (x2 in x1 + 1..3) for (y1 in 0..2) for (y2 in y1 + 1..3) {
            rects.add(doubleArrayOf(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble()))
        }
        assertEquals(36, rects.size)
        fun overlap(lo1: Double, hi1: Double, lo2: Double, hi2: Double) = Math.max(0.0, Math.min(hi1, hi2) - Math.max(lo1, lo2))
        for (r in rects) for (q in rects) {
            val expected = overlap(r[0], r[2], q[0], q[2]) * overlap(r[1], r[3], q[1], q[3])
            assertArea(expected, rect(r[0], r[1], r[2], r[3]), rect(q[0], q[1], q[2], q[3]))
        }
    }
}
