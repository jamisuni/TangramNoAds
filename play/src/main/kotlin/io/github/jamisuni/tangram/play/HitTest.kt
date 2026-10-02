package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import kotlin.math.hypot

// WO-003 design section 4 (REQ-015, F13, DA-29): what a touch lands on. Pure; all lengths are dp.

/** What a touch hit: a piece, or the flip badge. */
internal sealed interface Hit {
    data class Piece(val id: PieceId) : Hit

    data object Badge : Hit
}

internal object HitTest {
    /** REQ-015 A2: the nearest-edge reach in dp. */
    const val EDGE_REACH_DP = 12.0

    /**
     * (1) the flip badge; (2) a board piece containing [p]; (3) else the board piece with the nearest edge within
     * 12 dp; (4) a tray piece whose cell contains [p]. Dragged pieces and a solved puzzle hit nothing.
     */
    fun pick(p: Vec2, layout: PlayLayout, session: PlaySession): Hit? {
        if (session.state == PuzzleState.SOLVED) return null
        if (layout.badgeRect(session)?.contains(p) == true) return Hit.Badge

        val polys = session.placed.map { pl ->
            pl.piece to pl.corners.map { layout.toDp(Vec2(it.x.toDouble(), it.y.toDouble())) }
        }
        pickBoard(polys, p)?.let { return Hit.Piece(it) }

        return session.pieces
            .lastOrNull { it.where == Where.Tray && layout.hasCell(it.piece) && layout.cell(it.piece).contains(p) }
            ?.let { Hit.Piece(it.piece) }
    }

    /** DA-36: inside a polygon beats a nearer edge; the top-most (later in draw order) wins an overlap and an edge tie. */
    internal fun pickBoard(polys: List<Pair<PieceId, List<Vec2>>>, p: Vec2): PieceId? {
        polys.lastOrNull { (_, poly) -> contains(poly, p) }?.let { return it.first }
        var best: PieceId? = null
        var bestDist = Double.MAX_VALUE
        for ((piece, poly) in polys) {
            val d = edgeDistance(poly, p)
            if (d <= EDGE_REACH_DP && d <= bestDist) {
                best = piece
                bestDist = d
            }
        }
        return best
    }

    private fun contains(poly: List<Vec2>, p: Vec2): Boolean {
        var inside = false
        var j = poly.size - 1
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[j]
            if ((a.y > p.y) != (b.y > p.y) && p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x) inside = !inside
            j = i
        }
        return inside
    }

    private fun edgeDistance(poly: List<Vec2>, p: Vec2): Double {
        var best = Double.MAX_VALUE
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            val dx = b.x - a.x
            val dy = b.y - a.y
            val len2 = dx * dx + dy * dy
            val t = if (len2 == 0.0) 0.0 else (((p.x - a.x) * dx + (p.y - a.y) * dy) / len2).coerceIn(0.0, 1.0)
            best = minOf(best, hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy)))
        }
        return best
    }
}
