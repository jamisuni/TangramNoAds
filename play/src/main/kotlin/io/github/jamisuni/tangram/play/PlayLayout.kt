package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.layout.LayoutRules
import io.github.jamisuni.tangram.kernel.layout.TrayRules
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState

// WO-003 design sections 2-3: pure geometry of the play area (REQ-011, REQ-012, REQ-013, REQ-018, REQ-035/036).
// All lengths are dp as Double; no Compose or Android type.

/** A rectangle in dp; [right] and [bottom] are the far edges. */
internal data class RectDp(val left: Double, val top: Double, val right: Double, val bottom: Double) {
    val width: Double get() = right - left
    val height: Double get() = bottom - top
    val centre: Vec2 get() = Vec2((left + right) / 2, (top + bottom) / 2)

    fun contains(p: Vec2): Boolean = p.x >= left && p.x <= right && p.y >= top && p.y <= bottom
}

/** F1 / DA-71: where the corner control sits; [STRIP] = no corner was free, a strip above the board was reserved. */
internal enum class ControlCorner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, STRIP }

internal data class ControlPlacement(val corner: ControlCorner, val rect: RectDp)

/** DA-30: layout inputs come from window metrics including the system bars (never `Configuration.screenHeightDp`). */
internal object WindowMetrics {
    fun toDp(px: Int, density: Float): Double = px.toDouble() / density.toDouble()

    fun layoutClass(windowWidthPx: Int, density: Float): LayoutClass =
        LayoutRules.classFor(toDp(windowWidthPx, density))
}

internal class PlayLayout private constructor(
    val layoutClass: LayoutClass,
    val areaWidth: Double,
    val areaHeight: Double,
    /** Board scale; the value passed to `LockSearch.lockDistance` (O-01, TYPE-004). */
    val dpPerUnit: Double,
    private val originX: Double,
    private val originY: Double,
    val boardRect: RectDp,
    /** Top of the first tray row; the board ends above it. */
    val trayTop: Double,
    val trayScale: Double,
    val trayGap: Double,
    /** The rows of the puzzle (pieces the puzzle lacks and empty rows dropped). */
    val trayRows: List<List<PieceId>>,
    private val cells: Map<PieceId, RectDp>,
    /** The one polygon list used for the silhouette fill and the picture clip. */
    val silhouetteDp: List<List<Vec2>>,
) {
    fun toDp(u: Vec2): Vec2 = Vec2(originX + u.x * dpPerUnit, originY + u.y * dpPerUnit)

    fun toUnits(dp: Vec2): Vec2 = Vec2((dp.x - originX) / dpPerUnit, (dp.y - originY) / dpPerUnit)

    /** The tray cell of a piece of the puzzle (never depends on the piece's turn, REQ-013 A2). */
    fun cell(piece: PieceId): RectDp = cells[piece] ?: error("$piece is not a piece of this puzzle")

    fun hasCell(piece: PieceId): Boolean = piece in cells

    /**
     * F8 / DA-15: the dragged piece's floating centre lies in the board rect grown 20 dp left, right and top,
     * ending 4 dp above the tray top.
     */
    fun overBoard(centreDp: Vec2): Boolean =
        centreDp.x >= boardRect.left - OVER_MARGIN && centreDp.x <= boardRect.right + OVER_MARGIN &&
            centreDp.y >= boardRect.top - OVER_MARGIN && centreDp.y <= trayTop - OVER_BOTTOM_GAP

    /**
     * DA-29: the flip badge while PG is in the tray: a 60 dp square whose right edge is the PG cell's right
     * edge plus the adjacent gap, top at the cell top. Null when the puzzle has no PG.
     */
    fun trayBadgeRect(): RectDp? {
        val pg = cells[PieceId.PG] ?: return null
        val right = pg.right + trayGap
        return RectDp(right - BADGE_DP, pg.top, right, pg.top + BADGE_DP)
    }

    /**
     * The flip badge now (DA-29): PG in the tray gives [trayBadgeRect], on the board [boardBadgeRect];
     * null while PG is dragged, when the puzzle is solved, or has no PG.
     */
    fun badgeRect(session: PlaySession): RectDp? {
        if (session.state == PuzzleState.SOLVED) return null
        val pg = session.pieces.firstOrNull { it.piece == PieceId.PG } ?: return null
        return when (pg.where) {
            Where.Tray -> trayBadgeRect()
            is Where.Board -> session.placed.firstOrNull { it.piece == PieceId.PG }?.let { boardBadgeRect(it) }
            Where.Dragged -> null
        }
    }

    /**
     * The badge while PG lies on the board: left edge 22 dp right of its bounding box, top 4 dp above the
     * box top, clamped inside the play area.
     */
    fun boardBadgeRect(pg: PlacedPiece): RectDp {
        val pts = pg.corners.map { toDp(Vec2(it.x.toDouble(), it.y.toDouble())) }
        val left = pts.maxOf { it.x } + BOARD_BADGE_RIGHT
        val top = pts.minOf { it.y } - BOARD_BADGE_ABOVE
        val l = left.coerceIn(0.0, maxOf(0.0, areaWidth - BADGE_DP))
        val t = top.coerceIn(0.0, maxOf(0.0, areaHeight - BADGE_DP))
        return RectDp(l, t, l + BADGE_DP, t + BADGE_DP)
    }

    companion object {
        const val BADGE_DP = 60.0
        const val CELL_PADDING = 12.0
        const val MIN_ROW_HEIGHT = 84.0
        const val ROW_HEIGHT_FRACTION = 0.16
        private const val OVER_MARGIN = 20.0
        private const val OVER_BOTTOM_GAP = 4.0
        private const val BOARD_BADGE_RIGHT = 22.0
        private const val BOARD_BADGE_ABOVE = 4.0
        private const val BOARD_W_FRACTION = 0.82
        private const val BOARD_H_FRACTION = 0.80
        private const val THUMB_FRACTION = 0.84

        /** REQ-013 turn diameter in units, from the kernel (one definition). */
        private fun diameter(piece: PieceId): Double = TrayRules.turnDiameter(piece.shape)

        private fun sideMargin(c: LayoutClass) = if (c == LayoutClass.PHONE) 8.0 else 16.0
        private fun gap(c: LayoutClass) = if (c == LayoutClass.PHONE) 8.0 else 14.0
        private fun bottomMargin(c: LayoutClass) = if (c == LayoutClass.PHONE) 10.0 else 14.0
        private fun boardGap(c: LayoutClass) = if (c == LayoutClass.PHONE) 8.0 else 12.0

        /** REQ-013: the common scale for the FULL set [fullRows]; the same for every puzzle. */
        fun trayScaleFor(
            areaWidthDp: Double,
            screenHeightDp: Double,
            layoutClass: LayoutClass,
            fullRows: List<List<PieceId>>,
        ): Double {
            val trayWidth = areaWidthDp - 2 * sideMargin(layoutClass)
            val g = gap(layoutClass)
            val rowCap = maxOf(MIN_ROW_HEIGHT, ROW_HEIGHT_FRACTION * screenHeightDp)
            var scale = Double.MAX_VALUE
            for (row in fullRows) {
                if (row.isEmpty()) continue
                val sum = row.sumOf { diameter(it) }
                val byWidth = (trayWidth - g * (row.size + 1) - CELL_PADDING * row.size) / sum
                val byHeight = (rowCap - CELL_PADDING) / row.maxOf { diameter(it) }
                scale = minOf(scale, byWidth, byHeight)
            }
            return if (scale == Double.MAX_VALUE) 1.0 else maxOf(scale, 0.0)
        }

        /** Code review F1 / DA-71: the corner control's touch box, dp (REQ-037 minimum), and its gap from the edges. */
        const val CORNER_CONTROL_DP = 48.0
        const val CORNER_INSET_DP = 4.0

        /** Clearance kept between the control and the silhouette, so a touch beside a piece never reaches the control. */
        const val CORNER_CLEARANCE_DP = 4.0

        /**
         * F1 / DA-71: lays out the play area AND places a [controlWidthDp] x [controlHeightDp] control (its MEASURED size) (the corner slot) so it never
         * sits over the silhouette. The control takes the first corner of the board rect (top-left, top-right,
         * bottom-left, bottom-right) whose rect, grown by [CORNER_CLEARANCE_DP], touches no silhouette polygon. If no
         * corner is free, a strip of the control's measured height is reserved above the board (the board shrinks by it) and
         * the control sits at the strip's left. Pure: no Compose, no Android.
         */
        fun computeWithCorner(
            areaWidthDp: Double,
            areaHeightDp: Double,
            screenHeightDp: Double,
            layoutClass: LayoutClass,
            trayRows: List<List<PieceId>>,
            puzzle: Puzzle,
            controlWidthDp: Double = CORNER_CONTROL_DP,
            controlHeightDp: Double = CORNER_CONTROL_DP,
        ): Pair<PlayLayout, ControlPlacement> {
            val base = compute(areaWidthDp, areaHeightDp, screenHeightDp, layoutClass, trayRows, puzzle)
            val b = base.boardRect
            val m = CORNER_INSET_DP
            val cw = minOf(controlWidthDp, maxOf(0.0, areaWidthDp - 2 * m)) // never wider than the area minus both insets
            val ch = controlHeightDp
            val candidates = listOf(
                ControlPlacement(ControlCorner.TOP_LEFT, RectDp(b.left + m, b.top + m, b.left + m + cw, b.top + m + ch)),
                ControlPlacement(ControlCorner.TOP_RIGHT, RectDp(b.right - m - cw, b.top + m, b.right - m, b.top + m + ch)),
                ControlPlacement(ControlCorner.BOTTOM_LEFT, RectDp(b.left + m, b.bottom - m - ch, b.left + m + cw, b.bottom - m)),
                ControlPlacement(ControlCorner.BOTTOM_RIGHT, RectDp(b.right - m - cw, b.bottom - m - ch, b.right - m, b.bottom - m)),
            )
            for (c in candidates) {
                val r = c.rect
                val fitsBoard = r.left >= b.left && r.right <= b.right && r.top >= b.top && r.bottom <= b.bottom
                if (fitsBoard && !touchesSilhouette(r, base.silhouetteDp, CORNER_CLEARANCE_DP)) return base to c
            }
            val shrunk = compute(areaWidthDp, areaHeightDp, screenHeightDp, layoutClass, trayRows, puzzle, topReserve = ch)
            return shrunk to ControlPlacement(ControlCorner.STRIP, RectDp(m, 0.0, m + cw, ch))
        }

        /** True if [r] grown by [margin] overlaps or touches any polygon (vertex in, rect corner in, or edges cross). */
        internal fun touchesSilhouette(r: RectDp, polys: List<List<Vec2>>, margin: Double): Boolean {
            val g = RectDp(r.left - margin, r.top - margin, r.right + margin, r.bottom + margin)
            val corners = listOf(Vec2(g.left, g.top), Vec2(g.right, g.top), Vec2(g.right, g.bottom), Vec2(g.left, g.bottom))
            for (poly in polys) {
                if (poly.any { g.contains(it) }) return true
                if (corners.any { pointInPolygon(it, poly) }) return true
                for (i in poly.indices) {
                    val a = poly[i]
                    val c = poly[(i + 1) % poly.size]
                    for (k in corners.indices) if (segmentsIntersect(a, c, corners[k], corners[(k + 1) % 4])) return true
                }
            }
            return false
        }

        private fun pointInPolygon(p: Vec2, poly: List<Vec2>): Boolean {
            var inside = false
            var j = poly.size - 1
            for (i in poly.indices) {
                val a = poly[i]
                val c = poly[j]
                if ((a.y > p.y) != (c.y > p.y) && p.x < (c.x - a.x) * (p.y - a.y) / (c.y - a.y) + a.x) inside = !inside
                j = i
            }
            return inside
        }

        private fun segmentsIntersect(a: Vec2, b: Vec2, c: Vec2, d: Vec2): Boolean {
            fun cross(o: Vec2, p: Vec2, q: Vec2) = (p.x - o.x) * (q.y - o.y) - (p.y - o.y) * (q.x - o.x)
            val d1 = cross(c, d, a)
            val d2 = cross(c, d, b)
            val d3 = cross(a, b, c)
            val d4 = cross(a, b, d)
            return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) && ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))
        }

        fun compute(
            areaWidthDp: Double,
            areaHeightDp: Double,
            screenHeightDp: Double,
            layoutClass: LayoutClass,
            trayRows: List<List<PieceId>>,
            puzzle: Puzzle,
            /** F1: height reserved above the board (the corner control's strip); 0 for the plain layout. */
            topReserve: Double = 0.0,
        ): PlayLayout {
            val g = gap(layoutClass)
            val ts = trayScaleFor(areaWidthDp, screenHeightDp, layoutClass, trayRows)
            val present = puzzle.solution.map { it.piece }.toSet()
            val rows = trayRows.map { r -> r.filter { it in present } }.filter { it.isNotEmpty() }

            // Rows stacked up from the bottom margin; each row centred horizontally.
            val rowHeights = rows.map { r -> r.maxOf { diameter(it) } * ts + CELL_PADDING }
            val totalH = rowHeights.sum() + g * maxOf(0, rows.size - 1)
            val trayTop = areaHeightDp - bottomMargin(layoutClass) - totalH
            val cells = HashMap<PieceId, RectDp>()
            var y = trayTop
            rows.forEachIndexed { i, row ->
                val widths = row.map { diameter(it) * ts + CELL_PADDING }
                val rowW = widths.sum() + g * (row.size - 1)
                var x = (areaWidthDp - rowW) / 2
                row.forEachIndexed { j, p ->
                    cells[p] = RectDp(x, y, x + widths[j], y + rowHeights[i])
                    x += widths[j] + g
                }
                y += rowHeights[i] + g
            }

            // Board: from the top of the area to the tray top, minus the board gap.
            val boardRect = RectDp(0.0, topReserve, areaWidthDp, maxOf(topReserve, trayTop - boardGap(layoutClass)))
            val fit = fitSilhouette(boardRect, puzzle, BOARD_W_FRACTION, BOARD_H_FRACTION)
            return PlayLayout(
                layoutClass, areaWidthDp, areaHeightDp, fit.scale, fit.originX, fit.originY, boardRect, trayTop, ts, g, rows,
                cells, fit.silhouetteDp,
            )
        }

        /**
         * WO-004 section 6 (REQ-050 A2, O-09): a layout with NO tray for a grid thumbnail: the silhouette fitted into
         * a [widthDp] x [heightDp] box with padding. `silhouetteDp`, `toDp` and `boardRect` are the ones the play
         * area uses, so `silhouettePath` and `drawPicture` are reused as they are.
         */
        fun forThumbnail(widthDp: Double, heightDp: Double, puzzle: Puzzle): PlayLayout {
            val w = maxOf(widthDp, 0.0)
            val h = maxOf(heightDp, 0.0)
            val boardRect = RectDp(0.0, 0.0, w, h)
            val fit = fitSilhouette(boardRect, puzzle, THUMB_FRACTION, THUMB_FRACTION)
            return PlayLayout(
                LayoutClass.PHONE, w, h, fit.scale, fit.originX, fit.originY, boardRect, h, 1.0, 0.0, emptyList(),
                emptyMap(), fit.silhouetteDp,
            )
        }

        private class Fit(val scale: Double, val originX: Double, val originY: Double, val silhouetteDp: List<List<Vec2>>)

        /** The silhouette centred in [boardRect], filling at most the given fractions of its width and height. */
        private fun fitSilhouette(boardRect: RectDp, puzzle: Puzzle, wFraction: Double, hFraction: Double): Fit {
            val units = puzzle.solution.flatMap { s -> s.polygon.map { Vec2(it.x.toDouble(), it.y.toDouble()) } }
            val minX = units.minOf { it.x }
            val maxX = units.maxOf { it.x }
            val minY = units.minOf { it.y }
            val maxY = units.maxOf { it.y }
            val silW = maxOf(maxX - minX, 1e-9)
            val silH = maxOf(maxY - minY, 1e-9)
            val scale = minOf(boardRect.width * wFraction / silW, boardRect.height * hFraction / silH)
            val c = boardRect.centre
            val ox = c.x - (minX + maxX) / 2 * scale
            val oy = c.y - (minY + maxY) / 2 * scale
            val silDp = puzzle.solution.map { s ->
                s.polygon.map { Vec2(ox + it.x.toDouble() * scale, oy + it.y.toDouble() * scale) }
            }
            return Fit(scale, ox, oy, silDp)
        }
    }
}
