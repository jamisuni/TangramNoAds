package io.github.jamisuni.tangram.play

import androidx.compose.ui.test.junit4.createComposeRule
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.play.acceptance.TouchDriver
import io.github.jamisuni.tangram.play.acceptance.cellR
import io.github.jamisuni.tangram.play.acceptance.centroidOf
import io.github.jamisuni.tangram.play.acceptance.diff
import io.github.jamisuni.tangram.play.acceptance.layoutFor
import io.github.jamisuni.tangram.play.acceptance.puzzle
import androidx.compose.ui.graphics.toArgb
import io.github.jamisuni.tangram.play.acceptance.shot
import io.github.jamisuni.tangram.play.acceptance.showPlay
import io.github.jamisuni.tangram.play.acceptance.targetsOf
import io.github.jamisuni.tangram.play.acceptance.v
import io.github.jamisuni.tangram.play.draw.VisualTokens
import io.github.jamisuni.tangram.play.draw.pieceDp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.hypot

// DEVICE SCAFFOLDING (disposable), no acceptance tokens. decision DA-92 regression guard: on API 26 the hardware
// renderer blurred a path drawn under a canvas scale, so a piece's white edge bled about 2.5 px into its
// surroundings. With one piece on the board and the others in the tray, a pixel 2 px outside the outer edge of a
// piece (its 2 dp edge stroke included) must be exactly what is there without the piece.
class CrispEdgesScaffoldingTest {

    @get:Rule
    val rule = createComposeRule()

    /** Samples just outside every edge of [poly] (dp): the edge midpoint pushed out by half the stroke plus 2 px (measured: the blur ramp reaches 2 px past the edge, a pixel 3 px out stays clean even when blurred). */
    private fun outside(poly: List<Vec2>, density: Float): List<Vec2> {
        val c = centroidOf(poly)
        val push = VisualTokens.PIECE_EDGE_DP / 2.0 + 2.0 / density
        return poly.indices.map { i ->
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            val mx = (a.x + b.x) / 2
            val my = (a.y + b.y) / 2
            var nx = -(b.y - a.y)
            var ny = b.x - a.x
            val len = hypot(nx, ny)
            nx /= len; ny /= len
            if ((mx - c.x) * nx + (my - c.y) * ny < 0) { nx = -nx; ny = -ny }
            Vec2(mx + nx * push, my + ny * push)
        }
    }

    // decision DA-92
    @Test
    fun aPieceEdgeDoesNotBleedOnApi26() {
        val p = puzzle("shapes-square")
        val session = PlaySession(p, { true })
        rule.showPlay(session)
        val layout = layoutFor(p)
        val d = rule.density.density
        val before = rule.shot()

        val target = targetsOf(p).first()
        TouchDriver(rule, session, layout, p).place(target)
        assertTrue("the piece locked", session.placed.any { it.piece == target.piece })
        rule.mainClock.advanceTimeBy(PlayTiming.GLIDE_MS + 120) // past the lock glide, so the piece rests
        val after = rule.shot()

        // the placed piece: outside its edge the board is unchanged by its arrival
        val poly = p.solution.first { it.piece == target.piece }.polygon.map { layout.toDp(v(it)) }
        var checked = 0
        for (pt in outside(poly, d)) {
            val x = (pt.x * d).toInt()
            val y = (pt.y * d).toInt()
            if (x < 0 || y < 0 || x >= after.bmp.width || y >= after.bmp.height) continue
            assertTrue(
                "a pixel 2 px outside the placed ${target.piece} edge at $pt is tinted by it (diff ${diff(after.px(pt), before.px(pt))})",
                diff(after.px(pt), before.px(pt)) <= 6,
            )
            checked++
        }
        assertTrue("placed piece samples ($checked)", checked >= 3)

        // a piece still in the tray: outside its edge is the bare tray cell
        var trayChecked = 0
        for (s in session.pieces.filter { it.where == Where.Tray && it.piece != PieceId.PG }) {
            val cell = layout.cellR(s.piece)
            val tray = poly(s, layout)
            for (pt in outside(tray, d)) {
                if (pt.x < cell.l + 2 || pt.x > cell.r - 2 || pt.y < cell.t + 2 || pt.y > cell.b - 2) continue
                if (pt.x < cell.l + VisualTokens.MARK_INSET_DP + VisualTokens.MARK_CHIP_DP + 3 &&
                    pt.y < cell.t + VisualTokens.MARK_INSET_DP + VisualTokens.MARK_CHIP_DP + 3
                ) continue // the size-mark chip sits there
                assertTrue(
                    "a pixel 2 px outside the tray ${s.piece} edge at $pt is not the tray cell (diff ${diff(after.px(pt), trayCell)})",
                    diff(after.px(pt), trayCell) <= 6,
                )
                trayChecked++
            }
        }
        assertTrue("tray piece samples ($trayChecked)", trayChecked >= 3)
    }

    private val trayCell = VisualTokens.TRAY_CELL.toArgb()

    private fun poly(s: PieceState, layout: PlayLayout): List<Vec2> =
        pieceDp(s.piece, s.turn, s.mirrored, layout.cell(s.piece).centre, PieceDrawing.scale(s, layout, null))
}
