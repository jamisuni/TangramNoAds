package io.github.jamisuni.tangram.kernel.geometry

import io.github.jamisuni.tangram.kernel.model.ExactPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * SCAFFOLDING (TASK-004, WO-001 design §5, DA-7): the mask predicate, the exact outline corners against the
 * golden on every puzzle file (compared as sets), the pin test for shapes-warmup-4 (2,2) and the design's
 * spot values. No acceptance IDs.
 */
class SilhouetteTest {

    private val golden get() = Golden.data

    private fun pt(x: Long, y: Long): ExactPoint = pointOf(x, y)

    private fun polygon(vararg xy: Long): List<ExactPoint> = (xy.indices step 2).map { pt(xy[it], xy[it + 1]) }

    private fun mask(vararg wedges: Int): Int = wedges.fold(0) { acc, k -> acc or (1 shl k) }

    private fun solutionPolygons(id: String): List<List<ExactPoint>> =
        golden.puzzles.getValue(id).solution.map { it.polygon }

    // ---- isOutlineCornerMask -------------------------------------------------------------------------

    @Test
    fun aSingleContiguousRunOfFourWedgesIsAStraightSideNotACorner() {
        for (start in 0 until 8) {
            val run = mask(start, (start + 1) % 8, (start + 2) % 8, (start + 3) % 8)
            assertFalse("run of 4 from wedge $start", isOutlineCornerMask(run))
        }
        // The wrapping case named in the design: 6, 7, 0, 1.
        assertFalse(isOutlineCornerMask(mask(6, 7, 0, 1)))
    }

    @Test
    fun theFullCircleIsInsideNotACorner() {
        assertFalse(isOutlineCornerMask(0xFF))
        assertFalse(isOutlineCornerMask(mask(0, 1, 2, 3, 4, 5, 6, 7)))
    }

    @Test
    fun aHalfCircleMadeOfTwoSeparateRunsIsACorner() {
        // 3 + 1: three adjacent wedges and one elsewhere (the shapes-warmup-4 shape of (2,2)).
        assertTrue(isOutlineCornerMask(mask(0, 1, 2, 5)))
        assertTrue(isOutlineCornerMask(mask(0, 1, 2, 4)))
        // 2 + 2 and 1 + 3, separated, in every rotation.
        for (k in 0 until 8) {
            val twoPlusTwo = mask(k, (k + 1) % 8, (k + 4) % 8, (k + 5) % 8)
            assertTrue("2+2 from $k", isOutlineCornerMask(twoPlusTwo))
            val onePlusThree = mask(k, (k + 2) % 8, (k + 3) % 8, (k + 4) % 8)
            assertTrue("1+3 from $k", isOutlineCornerMask(onePlusThree))
        }
    }

    @Test
    fun runsOfOneTwoThreeFiveSixAndSevenWedgesAreCorners() {
        for (count in listOf(1, 2, 3, 5, 6, 7)) {
            for (start in 0 until 8) {
                val run = (0 until count).fold(0) { acc, j -> acc or (1 shl ((start + j) % 8)) }
                assertTrue("run of $count from wedge $start", isOutlineCornerMask(run))
            }
        }
    }

    @Test
    fun fourContiguousWedgesPlusOneMoreOrOneLessAreCorners() {
        assertTrue(isOutlineCornerMask(mask(0, 1, 2, 3, 5)))
        assertTrue(isOutlineCornerMask(mask(0, 1, 2)))
    }

    @Test
    fun theMaskIsReducedToEightBitsAndEmptyIsACorner() {
        // Bits above the eighth are ignored: 0x10F is the run 0..3, 0x100 is empty.
        assertFalse(isOutlineCornerMask(0x100 or mask(0, 1, 2, 3)))
        assertTrue(isOutlineCornerMask(0x100))
        assertTrue(isOutlineCornerMask(0))
    }

    // ---- the pin: shapes-warmup-4 (2,2) ---------------------------------------------------------------

    /** shapes-warmup-4's solution, written out literally (not through the golden reader). */
    private val warmup4 = listOf(
        polygon(4, 6, 0, 6, 2, 4),                  // LT1
        polygon(2, 2, 6, 2, 4, 4),                  // LT2
        polygon(2, 2, 2, 0, 4, 2),                  // MT
        polygon(0, 4, 1, 3, 2, 4, 1, 5),            // SQ
        polygon(4, 6, 4, 4, 3, 3, 3, 5),            // PG
        polygon(6, 4, 4, 4, 5, 3),                  // ST1
        polygon(2, 2, 2, 4, 1, 3),                  // ST2
    )

    @Test
    fun warmup4PinTwoTwoIsAnOutlineCorner() {
        // MT covers wedges 6,7 and LT2 wedge 0 (one run 6,7,0), ST2 covers wedge 2 elsewhere: 180 degrees in two runs.
        assertTrue(pt(2, 2) in Silhouette(warmup4).outlineCorners)
    }

    @Test
    fun warmup4PinAlsoHoldsOnTheGoldenPolygonsAndTheGoldenListsIt() {
        assertTrue(pt(2, 2) in Silhouette(solutionPolygons("shapes-warmup-4")).outlineCorners)
        assertTrue(pt(2, 2) in golden.puzzles.getValue("shapes-warmup-4").outlineCorners)
    }

    // ---- corners equal the golden on every puzzle file ------------------------------------------------

    @Test
    fun cornersEqualTheGoldenOnEveryPuzzleFile() {
        assertEquals("the golden holds one entry per Tangrams file", Golden.puzzleFileBytes().keys, golden.puzzles.keys)
        assertEquals(13, golden.puzzles.size)
        for ((id, puzzle) in golden.puzzles) {
            val corners = Silhouette(puzzle.solution.map { it.polygon }).outlineCorners
            assertEquals("$id: no corner listed twice", corners.size, corners.toSet().size)
            if (corners.toSet() != puzzle.outlineCorners.toSet()) {
                fail(
                    "$id: kernel corners differ from the golden. only in kernel: " +
                        "${corners.toSet() - puzzle.outlineCorners.toSet()}; only in golden: " +
                        "${puzzle.outlineCorners.toSet() - corners.toSet()}",
                )
            }
        }
    }

    @Test
    fun cornersDoNotDependOnPolygonOrderOrOrientation() {
        for ((id, puzzle) in golden.puzzles) {
            val polygons = puzzle.solution.map { it.polygon }
            val expected = Silhouette(polygons).outlineCorners.toSet()
            val shuffled = polygons.reversed().mapIndexed { i, poly ->
                if (i % 2 == 0) poly.reversed() else poly.drop(1) + poly.first()
            }
            assertEquals("$id: reordered, reversed or rotated polygons", expected, Silhouette(shuffled).outlineCorners.toSet())
        }
    }

    // ---- the design's spot values ----------------------------------------------------------------------

    @Test
    fun spotValueMini1() {
        assertEquals(
            setOf(pt(2, 0), pt(0, 2), pt(4, 2)),
            Silhouette(solutionPolygons("shapes-mini-1")).outlineCorners.toSet(),
        )
    }

    @Test
    fun spotValueMini2() {
        assertEquals(
            setOf(pt(0, 0), pt(2, 0), pt(0, 2), pt(2, 2)),
            Silhouette(solutionPolygons("shapes-mini-2")).outlineCorners.toSet(),
        )
    }

    @Test
    fun spotValueHouseNeedsRootTwo() {
        // (4, 4-sqrt2), (4+sqrt2, 4-sqrt2), (4+sqrt2, 4): exact numbers a + b*sqrt2.
        val expected = setOf(
            pt(0, 2), pt(2, 0), pt(0, 4), pt(3, 1), pt(3, -1), pt(4, 0),
            ExactPoint(q2(4), q2(4, -1)),
            ExactPoint(q2(4, 1), q2(4, -1)),
            ExactPoint(q2(4, 1), q2(4)),
        )
        assertEquals(expected, Silhouette(solutionPolygons("things-house")).outlineCorners.toSet())
    }

    // ---- synthetic cases: a vertex inside another piece's edge, contiguity across polygons -----------

    // Three pieces below the line y = 0 around p = (2,0): wedges 3 (MT), 2 (left ST) and 0,1 (right MT).
    private val mtLeft = polygon(0, 0, 2, 0, 0, 2)       // covers wedge 3 at (2,0)
    private val stMiddle = polygon(2, 0, 2, 2, 0, 2)     // covers wedge 2 at (2,0)
    private val mtRight = polygon(2, 0, 4, 0, 2, 2)      // covers wedges 0, 1 at (2,0)
    private val lidAbove = polygon(0, 0, 4, 0, 2, -2)    // p = (2,0) lies strictly inside its edge: wedges 4..7

    @Test
    fun threePiecesFormingOneStraightRunAreNotACorner() {
        val corners = Silhouette(listOf(mtLeft, stMiddle, mtRight)).outlineCorners
        assertFalse("wedges 0..3 as one run", pt(2, 0) in corners)
    }

    @Test
    fun aPieceWithAGapWedgeLeavesACorner() {
        val corners = Silhouette(listOf(mtLeft, mtRight)).outlineCorners
        assertTrue("wedges 0,1,3: a notch", pt(2, 0) in corners)
    }

    @Test
    fun aVertexInsideAnotherPiecesEdgeContributesAHalfPlane() {
        // Straight run below + the lid's edge above (inside an edge): all 8 wedges, the point is inside.
        val inside = Silhouette(listOf(mtLeft, stMiddle, mtRight, lidAbove)).outlineCorners
        assertFalse("full circle through an inside-edge half plane", pt(2, 0) in inside)
        // Lid (4 wedges) + one small triangle below it: 5 wedges, not one run of 4.
        val partly = Silhouette(listOf(stMiddle, lidAbove)).outlineCorners
        assertTrue("half plane plus one wedge", pt(2, 0) in partly)
    }

    @Test
    fun aNonFortyFiveDegreeEdgeIsAContentError() {
        val bad = polygon(0, 0, 2, 0, 2, 1)
        try {
            Silhouette(listOf(bad))
            fail("an edge of slope 1/2 must fail a require")
        } catch (expected: IllegalArgumentException) {
            // content error, as designed
        }
    }

    @Test
    fun anEdgeOfSlopeOneInRootTwoCoordinatesIsAccepted() {
        // |dx| == |dy| with sqrt2 parts: (0,0) -> (sqrt2, sqrt2) is a 45-degree edge, exact equality decides it.
        val r2 = ExactPoint(q2(0, 1), q2(0, 1))
        val polygon = listOf(pt(0, 0), r2, ExactPoint(q2(0, 1), q2(0)))
        val corners = Silhouette(listOf(polygon)).outlineCorners
        assertEquals(3, corners.size)
    }
}
