package io.github.jamisuni.tangram.play.acceptance

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.play.PlayArea
import io.github.jamisuni.tangram.play.PlayLayout
import io.github.jamisuni.tangram.play.PlaySession
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import kotlin.math.abs
import kotlin.math.hypot

// DEVICE SCAFFOLDING for WO-003 (no acceptance tokens). The play area is shown at a fixed 360 x 640 dp inside a Box, so
// PlayLayout.compute(360, 640, 780, PHONE, rows, puzzle) of the JVM seams describes exactly what is on screen (PlayArea
// computes from its own size; screenHeight 780 is passed). The test clock is manual (autoAdvance = false): a drag
// keeps a per-frame loop alive, so waiting for idleness would never return; every step advances the clock itself.

internal const val AREA_W = 360.0
internal const val AREA_H = 640.0
internal const val SCREEN_H = 780.0

internal fun ComposeContentTestRule.showPlay(session: PlaySession) {
    mainClock.autoAdvance = false
    setContent {
        Box(Modifier.size(AREA_W.dp, AREA_H.dp)) {
            PlayArea(session, LayoutClass.PHONE, PHONE_ROWS, SCREEN_H.dp, Modifier.fillMaxSize())
        }
    }
    mainClock.advanceTimeBy(100)
}

/** A screenshot of the play area and the dp -> pixel mapping (play-area origin = dp origin of PlayLayout). */
internal class Shot(val bmp: Bitmap, val density: Float) {
    fun px(p: Vec2): Int =
        bmp.getPixel((p.x * density).toInt().coerceIn(0, bmp.width - 1), (p.y * density).toInt().coerceIn(0, bmp.height - 1))
}

internal fun ComposeContentTestRule.shot(): Shot =
    Shot(onNodeWithTag("play-area").captureToImage().asAndroidBitmap(), density.density)

internal fun chan(c: Int, shift: Int) = (c shr shift) and 0xFF

/** Largest per-channel difference of two ARGB pixels. */
internal fun diff(a: Int, b: Int): Int = maxOf(abs(chan(a, 16) - chan(b, 16)), abs(chan(a, 8) - chan(b, 8)), abs(chan(a, 0) - chan(b, 0)))

internal fun rgb(hex: Int): Int = 0xFF000000.toInt() or hex

internal fun distToSegment(p: Vec2, a: Vec2, b: Vec2): Double {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val l2 = dx * dx + dy * dy
    val t = if (l2 == 0.0) 0.0 else (((p.x - a.x) * dx + (p.y - a.y) * dy) / l2).coerceIn(0.0, 1.0)
    return hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy))
}

internal fun distToOutlineUnits(p: Puzzle, pt: Vec2): Double =
    solutionPolys(p).minOf { poly -> poly.indices.minOf { i -> distToSegment(pt, poly[i], poly[(i + 1) % poly.size]) } }

/** Mid-edge sample points (units) of the solution polygons, with the unit normal and whether both sides lie in the silhouette. */
internal data class EdgeSample(val at: Vec2, val normal: Vec2, val interior: Boolean)

/**
 * DA-40: true when [m] is an outline corner of the silhouette, i.e. some direction (16 of them) at radius [eps] leaves the
 * silhouette. At such a point a piece tip meets the outline (DA-7 sector rule), so the pixel is partly background however the
 * silhouette is drawn. A point inside a straight shared edge has every direction inside.
 */
internal fun isOutlineCorner(p: Puzzle, m: Vec2, eps: Double = 0.05): Boolean =
    (0 until 16).any { k ->
        val a = 2 * Math.PI * k / 16
        !inSilhouette(p, Vec2(m.x + eps * Math.cos(a), m.y + eps * Math.sin(a)))
    }

internal fun edgeSamples(p: Puzzle, eps: Double = 0.05): List<EdgeSample> {
    val out = ArrayList<EdgeSample>()
    for (poly in solutionPolys(p)) {
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            val len = hypot(b.x - a.x, b.y - a.y)
            val n = Vec2(-(b.y - a.y) / len, (b.x - a.x) / len)
            val at = { f: Double -> Vec2(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f) }
            val used = HashSet<Double>()
            for (f0 in listOf(0.25, 0.5, 0.75)) {
                var f = f0
                var m = at(f)
                var s1 = inSilhouette(p, Vec2(m.x + n.x * eps, m.y + n.y * eps))
                var s2 = inSilhouette(p, Vec2(m.x - n.x * eps, m.y - n.y * eps))
                if (s1 && s2 && isOutlineCorner(p, m)) {
                    // DA-40: a shared-edge sample on an outline corner moves along its edge (never dropped)
                    f = listOf(0.25, 0.75, 0.4, 0.6, 0.1, 0.9).firstOrNull { g -> g !in used && g != f0 && !isOutlineCorner(p, at(g)) } ?: continue
                    m = at(f)
                    s1 = inSilhouette(p, Vec2(m.x + n.x * eps, m.y + n.y * eps))
                    s2 = inSilhouette(p, Vec2(m.x - n.x * eps, m.y - n.y * eps))
                }
                if (!used.add(f)) continue
                if (s1 || s2) out.add(EdgeSample(m, n, s1 && s2))
            }
        }
    }
    return out
}

/** The real touch path: injected with performTouchInput on the play area (never performClick). Coordinates in dp of the play area. */
internal class TouchDriver(
    private val rule: ComposeContentTestRule,
    val session: PlaySession,
    val layout: PlayLayout,
    val puzzle: Puzzle,
) {
    private val d = rule.density.density
    var finger = Vec2(0.0, 0.0)

    private fun off(p: Vec2) = Offset((p.x * d).toFloat(), (p.y * d).toFloat())
    private fun area() = rule.onNodeWithTag("play-area")
    fun advance(ms: Long) = rule.mainClock.advanceTimeBy(ms)

    fun down(p: Vec2) { area().performTouchInput { down(off(p)) }; advance(16) }
    fun moveTo(p: Vec2) { area().performTouchInput { moveTo(off(p)) }; advance(16) }
    fun up() { area().performTouchInput { up() }; advance(32) }
    fun tap(p: Vec2) { down(p); advance(30); up() }

    fun origin(): Vec2? = rule.runOnUiThread { session.drag?.frame?.pose?.origin }

    fun startDrag(from: Vec2) {
        down(from)
        finger = Vec2(from.x, from.y - 20.0)
        moveTo(finger)
    }

    fun steerOrigin(target: Vec2) {
        repeat(3) {
            advance(150)
            val o = origin() ?: error("seam: steerOrigin called with no dragged piece")
            val a = layout.toDp(o)
            val b = layout.toDp(target)
            finger = Vec2(finger.x + b.x - a.x, finger.y + b.y - a.y)
            moveTo(finger)
        }
        advance(150)
    }

    fun place(target: PlacedPiece) {
        val cell = layout.cellR(target.piece)
        if (target.mirrored != session.ps(target.piece).mirrored) tap(layout.badgeR(session).centre)
        val taps = Math.floorMod(target.turn.steps - session.ps(target.piece).turn.steps, 8)
        repeat(taps) { tap(pressPoint(cell)) }
        startDrag(pressPoint(cell))
        steerOrigin(v(target.at))
        up()
    }

    fun placeAll(targets: List<PlacedPiece>) {
        for (tg in buildOrder(puzzle, targets, layout.dpPerUnit, session.placed)) {
            place(tg)
            check(session.placed.any { it.piece == tg.piece && it.at == tg.at }) { "${tg.piece} did not lock in ${puzzle.id.value}" }
        }
    }

    /** Drag to a kernel-oracle miss spot over the board and release; returns the float centre (dp) at release. */
    fun missDrop(piece: PieceId): Vec2 {
        val ps = session.ps(piece)
        val origin = missOrigin(puzzle, layout.dpPerUnit, piece, ps.turn, ps.mirrored, session.placed)
        val off = centroidOffset(piece, ps.turn, ps.mirrored)
        startDrag(pressPoint(layout.cellR(piece)))
        steerOrigin(origin)
        val centre = layout.toDp(Vec2(origin.x + off.x, origin.y + off.y))
        up()
        return centre
    }
}
