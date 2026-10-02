package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.contracts.puzzle.Picture
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleCategory
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleKind
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleTitle
import io.github.jamisuni.tangram.contracts.puzzle.Rgb
import io.github.jamisuni.tangram.contracts.puzzle.SolutionPiece
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.layout.TrayRules
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PieceShape
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// SCAFFOLDING (disposable, TASK-013): not an acceptance test.
class PlayLayoutScaffoldingTest {
    private val phoneRows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val tabletRows = listOf(PieceId.entries.toList())

    private fun pt(x: Long, y: Long) = ExactPoint(Q2.of(x), Q2.of(y))

    // Layout only reads the polygons' bounds, so a spread-out arrangement is enough.
    private fun puzzle(pieces: List<PieceId>): Puzzle {
        val sol = pieces.mapIndexed { i, p ->
            SolutionPiece(p, PieceGeometry.corners(p, Turn(0), false, pt((i % 4) * 2L, (i / 4) * 2L)))
        }
        return Puzzle(
            PuzzleId("t"), PuzzleTitle("t", "t"), PuzzleCategory.SHAPES, 1,
            if (pieces.size == 7) PuzzleKind.FULL else PuzzleKind.MINI, sol, Picture(Rgb(0), emptyList()), false,
        )
    }

    private val full = puzzle(PieceId.entries.toList())
    private val mini = puzzle(listOf(PieceId.SQ, PieceId.ST1, PieceId.ST2))

    @Test fun phone360CellsAtLeast56() {
        val l = PlayLayout.compute(360.0, 700.0, 780.0, LayoutClass.PHONE, phoneRows, full)
        for (p in PieceId.entries) {
            val c = l.cell(p)
            assertTrue("$p ${c.width}x${c.height}", c.width >= 56 && c.height >= 56)
        }
    }

    @Test fun phone390Scale() {
        val l = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, phoneRows, full)
        assertEquals(26.8, l.trayScale, 0.1)
    }

    @Test fun scaleAndCellWidthSameInMiniAndFull() {
        val a = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, phoneRows, full)
        val b = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, phoneRows, mini)
        assertEquals(a.trayScale, b.trayScale, 0.0)
        assertEquals(a.cell(PieceId.ST1).width, b.cell(PieceId.ST1).width, 1e-9)
        assertFalse(b.hasCell(PieceId.LT1))
        assertEquals(1, b.trayRows.size)
    }

    @Test fun fullSetCellsEqualAcrossPuzzles() {
        val a = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, phoneRows, full)
        val b = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, phoneRows, puzzle(PieceId.entries.reversed()))
        for (p in PieceId.entries) assertEquals(a.cell(p), b.cell(p))
    }

    @Test fun rowHeightWithinLimitAndTablet() {
        val l = PlayLayout.compute(800.0, 1100.0, 1280.0, LayoutClass.TABLET, tabletRows, full)
        val cap = maxOf(84.0, 0.16 * 1280.0)
        for (p in PieceId.entries) assertTrue(l.cell(p).height <= cap + 1e-9)
        assertTrue(l.cell(PieceId.ST2).right <= 800.0)
    }

    @Test fun turnDiametersMatchReqTable() {
        assertEquals(4.22, TrayRules.turnDiameter(PieceShape.LARGE_TRIANGLE), 0.005)
        assertEquals(3.16, TrayRules.turnDiameter(PieceShape.PARALLELOGRAM), 0.005)
        assertEquals(2.98, TrayRules.turnDiameter(PieceShape.MEDIUM_TRIANGLE), 0.005)
        assertEquals(2.11, TrayRules.turnDiameter(PieceShape.SMALL_TRIANGLE), 0.005)
        assertEquals(2.00, TrayRules.turnDiameter(PieceShape.SQUARE), 0.005)
    }

    @Test fun cellWidthFormula() {
        val l = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, phoneRows, full)
        assertEquals(TrayRules.turnDiameter(PieceShape.SQUARE) * l.trayScale + 12, l.cell(PieceId.SQ).width, 1e-9)
    }

    @Test fun mappingRoundTripAndSilhouetteDp() {
        val l = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, phoneRows, full)
        val u = Vec2(1.5, 2.5)
        val back = l.toUnits(l.toDp(u))
        assertEquals(u.x, back.x, 1e-9)
        assertEquals(u.y, back.y, 1e-9)
        val first = full.solution[0].polygon[1]
        val dp = l.toDp(Vec2(first.x.toDouble(), first.y.toDouble()))
        assertEquals(dp.x, l.silhouetteDp[0][1].x, 1e-9)
        assertEquals(dp.y, l.silhouetteDp[0][1].y, 1e-9)
        for (poly in l.silhouetteDp) for (p in poly) assertTrue(l.boardRect.contains(p))
    }

    @Test fun overBoardTable() {
        val l = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, phoneRows, full)
        assertTrue(l.overBoard(l.boardRect.centre))
        assertTrue(l.overBoard(Vec2(-19.0, 100.0)))
        assertFalse(l.overBoard(Vec2(-21.0, 100.0)))
        assertTrue(l.overBoard(Vec2(100.0, -19.0)))
        assertFalse(l.overBoard(Vec2(100.0, -21.0)))
        assertTrue(l.overBoard(Vec2(100.0, l.trayTop - 5)))
        assertFalse(l.overBoard(Vec2(100.0, l.trayTop - 3)))
        assertFalse(l.overBoard(Vec2(100.0, l.trayTop + 30)))
    }

    @Test fun trayBadgeInsidePgColumn() {
        for (w in listOf(360.0 to 780.0, 390.0 to 844.0)) {
            val l = PlayLayout.compute(w.first, w.second - 80, w.second, LayoutClass.PHONE, phoneRows, full)
            val pg = l.cell(PieceId.PG)
            val b = assertNotNull(l.trayBadgeRect()).let { l.trayBadgeRect()!! }
            assertEquals(60.0, b.width, 1e-9)
            assertEquals(60.0, b.height, 1e-9)
            assertTrue(b.left >= pg.left - 1e-9 && b.right <= pg.right + l.trayGap + 1e-9)
            assertEquals(pg.top, b.top, 1e-9)
        }
        assertNull(PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, phoneRows, mini).trayBadgeRect())
    }

    @Test fun boardBadgeClampedInside() {
        val l = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, phoneRows, full)
        val b = l.boardBadgeRect(PlacedPiece(PieceId.PG, Turn(0), false, pt(7, 0)))
        assertTrue(b.left >= 0 && b.right <= 390.0 && b.top >= 0 && b.bottom <= 760.0)
    }

    @Test fun windowMetrics() {
        assertEquals(LayoutClass.TABLET, WindowMetrics.layoutClass(1200, 2.0f))
        assertEquals(LayoutClass.PHONE, WindowMetrics.layoutClass(1199, 2.0f))
    }
}
