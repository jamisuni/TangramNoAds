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
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.layout.LayoutRules
import io.github.jamisuni.tangram.kernel.layout.TrayRules
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PieceShape
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// decision DA-100: SCAFFOLDING / DECISION TEST (WO-006 T6c), not an acceptance test, carries no requirement token.
//
// What it decides: whether the tablet tray gap must change (L-1) and how the short-window conflict looks in numbers.
// Mandated assertions, each traced to a sentence of the spec:
//   * every tray cell is at least 48 dp in both axes  -- REQ-037 "controls at least 48 dp" (a tray piece's touch area is its whole cell,
//     design section 4 / DA-99);
//   * a phone-class window shows the tray in 2 rows and a tablet-class window in 1 row -- REQ-035 A1 / REQ-036 A1 row counts
//     (design section 2 / 3; the rows themselves are the REQ-035 / REQ-036 orders);
//   * every cell lies inside the play area (design "Value shapes": 0 <= left, right <= area width, 0 <= top, bottom <= area height).
// The sweep goes through the product's PlayLayout.compute (which calls PlayLayout.trayScaleFor and TrayRules.turnDiameter); nothing
// is re-derived here. The class a width selects comes from LayoutRules.classFor (TYPE-007).
// Window assumptions (design section 4 "TraySizeSweepTest"): widths 360..1400 in 10 dp steps plus the 599..603 boundary,
// heights 560, 640, 780, 844, 960, 1200 (the sweep stays clear of the 549.3 dp conflict boundary). The play area is the window
// minus a 66 dp top bar and 72 dp of system bars (24 status + 48 navigation); cell SIZES do not depend on the area height,
// so only the inside-the-area check uses this.
class TraySizeSweepTest {
    private val phoneRows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val tabletRows = listOf(PieceId.entries.toList())

    private fun pt(x: Long, y: Long) = ExactPoint(Q2.of(x), Q2.of(y))

    // Layout reads only the solution polygons' bounds; a spread-out arrangement of all seven pieces is enough (a full-set puzzle).
    private val full: Puzzle = run {
        val sol = PieceId.entries.mapIndexed { i, p ->
            SolutionPiece(p, PieceGeometry.corners(p, Turn(0), false, pt((i % 4) * 2L, (i / 4) * 2L)))
        }
        Puzzle(PuzzleId("t"), PuzzleTitle("t", "t"), PuzzleCategory.SHAPES, 1, PuzzleKind.FULL, sol, Picture(Rgb(0), emptyList()), false)
    }

    private fun layoutFor(widthDp: Double, heightDp: Double): PlayLayout {
        val cls = LayoutRules.classFor(widthDp)
        val rows = if (cls == LayoutClass.PHONE) phoneRows else tabletRows
        val areaHeight = heightDp - TOP_BAR_DP - SYSTEM_BARS_DP
        return PlayLayout.compute(widthDp, areaHeight, heightDp, cls, rows, full)
    }

    private fun smallestCellSide(l: PlayLayout): Pair<Double, PieceId> {
        var best = Double.MAX_VALUE
        var who = PieceId.SQ
        for (p in PieceId.entries) {
            val c = l.cell(p)
            val m = minOf(c.width, c.height)
            if (m < best) { best = m; who = p }
        }
        return best to who
    }

    private val widths: List<Int> = (360..1400 step 10).toList().plus(listOf(599, 600, 601, 602, 603)).distinct().sorted()
    private val heights = listOf(560, 640, 780, 844, 960, 1200)

    @Test fun everyTrayCellIsAtLeast48DpBothAxesInEveryWindowFrom560() {
        val breaches = ArrayList<String>()
        var worst = Double.MAX_VALUE
        var worstAt = ""
        for (w in widths) for (h in heights) {
            val l = layoutFor(w.toDouble(), h.toDouble())
            for (p in PieceId.entries) {
                val c = l.cell(p)
                if (c.width < MIN_DP - EPS || c.height < MIN_DP - EPS) {
                    breaches += "%dx%d %s %.2f x %.2f".format(w, h, p, c.width, c.height)
                }
            }
            val (m, who) = smallestCellSide(l)
            if (m < worst) { worst = m; worstAt = "${w}x$h $who" }
        }
        println("SWEEP smallest cell over all ${widths.size * heights.size} windows: %.3f dp at %s".format(worst, worstAt))
        println("SWEEP breaches (${breaches.size}): " + breaches.take(12).joinToString("; ") + if (breaches.size > 12) "; ..." else "")
        assertTrue("tray cells under $MIN_DP dp (window WxH, piece, cell WxH): $breaches", breaches.isEmpty())
    }

    @Test fun rowCountAndInsideTheAreaInEveryWindowFrom560() {
        for (w in widths) for (h in heights) {
            val l = layoutFor(w.toDouble(), h.toDouble())
            val cls = LayoutRules.classFor(w.toDouble())
            val expectedRows = if (cls == LayoutClass.PHONE) 2 else 1
            assertEquals("${w}x$h rows", expectedRows, l.trayRows.size)
            assertEquals("${w}x$h cells", 7, l.trayRows.sumOf { it.size })
            val areaH = h - TOP_BAR_DP - SYSTEM_BARS_DP
            for (p in PieceId.entries) {
                val c = l.cell(p)
                assertTrue("${w}x$h $p $c outside the area ${w.toDouble()} x $areaH",
                    c.left >= 0.0 && c.right <= w.toDouble() + EPS && c.top >= 0.0 && c.bottom <= areaH + EPS)
            }
        }
    }

    // The measurement the orchestrator asked for: the square's cell on a 600 dp-wide tablet. Printed, and asserted by the sweep above.
    @Test fun printSquareCellOnTheSmallestTablets() {
        for (w in listOf(599, 600, 601, 602, 603, 640, 800)) for (h in listOf(560, 960, 1280)) {
            val l = layoutFor(w.toDouble(), h.toDouble())
            val sq = l.cell(PieceId.SQ)
            println("SQUARE ${w}x$h class=${LayoutRules.classFor(w.toDouble())} cell %.3f x %.3f dp (scale %.4f)".format(sq.width, sq.height, l.trayScale))
        }
        // Sanity that the printed value is the product's formula, not a guess: the square's cell width is its turn diameter * scale + 12.
        val l = layoutFor(600.0, 960.0)
        assertEquals(TrayRules.turnDiameter(PieceShape.SQUARE) * l.trayScale + PlayLayout.CELL_PADDING, l.cell(PieceId.SQ).width, 1e-9)
    }

    // NON-ASSERTING (DA-100 (ii), the interim zone): prints the smallest cell for window heights 500..549 so the REQ-013 / REQ-037
    // conflict is on record with numbers. Nothing here can fail.
    @Test fun printInterimZoneHeights500To549() {
        for (w in listOf(360, 390, 599, 600, 800, 1280)) {
            val sb = StringBuilder("INTERIM width $w: ")
            for (h in 500..549 step 10) {
                val (m, who) = smallestCellSide(layoutFor(w.toDouble(), h.toDouble()))
                sb.append("h%d=%.2f(%s) ".format(h, m, who))
            }
            // also the last height of the band
            val (m, who) = smallestCellSide(layoutFor(w.toDouble(), 549.0))
            sb.append("h549=%.2f(%s)".format(m, who))
            println(sb)
        }
    }

    private companion object {
        const val MIN_DP = 48.0
        const val EPS = 1e-9
        const val TOP_BAR_DP = 66.0
        const val SYSTEM_BARS_DP = 72.0
    }
}
