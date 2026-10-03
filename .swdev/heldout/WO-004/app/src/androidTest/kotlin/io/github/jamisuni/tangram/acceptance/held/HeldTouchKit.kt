package io.github.jamisuni.tangram.acceptance.held

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.layout.TrayRules
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.play.BoardTransform
import kotlin.math.abs

// ACCEPTANCE-TEST SCAFFOLDING for `app` device tests (TASK-T4). It needs no seam beyond the `play-area` and `board` tags and
// the TYPE-001 colours (req_types TYPE-001: LT1 #E8505B, LT2 #3D8BFD, MT #F9A826, SQ #FFD23F, PG #FF7AB8, ST1 #2DBE7E,
// ST2 #9B5DE5), exactly as the WO-003 end-to-end test does. All input is injected touch. Every miss is a loud `error`.

internal object PieceColours {
    private val hex = mapOf(
        PieceId.SQ to 0xFFD23F, PieceId.ST1 to 0x2DBE7E, PieceId.ST2 to 0x9B5DE5, PieceId.LT1 to 0xE8505B,
        PieceId.LT2 to 0x3D8BFD, PieceId.MT to 0xF9A826, PieceId.PG to 0xFF7AB8,
    )

    fun rgb(piece: PieceId): Int = 0xFF000000.toInt() or hex.getValue(piece)

    fun rgbOf(value: Int): Int = 0xFF000000.toInt() or value
}

internal fun colourDiff(a: Int, b: Int): Int = maxOf(
    abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)),
    abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)),
    abs((a and 0xFF) - (b and 0xFF)),
)

/** The board rectangle in the coordinates of the `play-area` node (and of its screenshot). */
internal class BoardRect(val x0: Int, val x1: Int, val y0: Int, val y1: Int)

/**
 * Unit coordinates to pixels of the `play-area` screenshot, taken from the public `BoardTransform` semantics key of the `board`
 * node (design WO-003 "Test seams", DA-70): never inferred from pixel colours, which would swallow overlays such as the Restart pill.
 */
internal class UnitMapper(val scalePx: Float, private val originX: Float, private val originY: Float) {
    fun px(ux: Double, uy: Double) = Offset((originX + ux * scalePx).toFloat(), (originY + uy * scalePx).toFloat())
}

internal class TouchRig(private val rule: ComposeTestRule) {

    val density: Float = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics.density

    fun shot(): Bitmap = rule.onNodeWithTag("play-area").captureToImage().asAndroidBitmap()

    fun board(): BoardRect {
        val area = rule.onNodeWithTag("play-area").fetchSemanticsNode().boundsInRoot
        val b = rule.onNodeWithTag("board").fetchSemanticsNode().boundsInRoot
        return BoardRect(
            (b.left - area.left).toInt(), (b.right - area.left).toInt(),
            (b.top - area.top).toInt(), (b.bottom - area.top).toInt(),
        )
    }

    /** Pixels of [piece]'s TYPE-001 colour (step 2) inside the rectangle. */
    fun count(shot: Bitmap, piece: PieceId, x0: Int, x1: Int, y0: Int, y1: Int): Int {
        val want = PieceColours.rgb(piece)
        var n = 0
        for (y in y0.coerceAtLeast(0) until y1.coerceAtMost(shot.height) step 2) {
            for (x in x0.coerceAtLeast(0) until x1.coerceAtMost(shot.width) step 2) {
                if (colourDiff(shot.getPixel(x, y), want) <= 10) n++
            }
        }
        return n
    }

    /** The tray is everything below the board. */
    fun trayCount(shot: Bitmap, board: BoardRect, piece: PieceId): Int = count(shot, piece, 0, shot.width, board.y1, shot.height)

    fun inTray(shot: Bitmap, board: BoardRect, piece: PieceId): Boolean = trayCount(shot, board, piece) > 20

    /** Pixels of the piece's colour on the board region: a placed piece is large, so presence means many. */
    fun boardCount(shot: Bitmap, board: BoardRect, piece: PieceId): Int = count(shot, piece, board.x0, board.x1, board.y0, board.y1)

    fun anyPieceColourOnBoard(shot: Bitmap, board: BoardRect): List<PieceId> =
        PieceId.entries.filter { boardCount(shot, board, it) > 3 }

    private fun trayCentre(shot: Bitmap, board: BoardRect, piece: PieceId): Offset {
        val want = PieceColours.rgb(piece)
        var sx = 0.0
        var sy = 0.0
        var n = 0
        for (y in board.y1 until shot.height step 2) {
            for (x in 0 until shot.width step 2) {
                if (colourDiff(shot.getPixel(x, y), want) <= 10) {
                    sx += x
                    sy += y
                    n++
                }
            }
        }
        if (n <= 20) error("the $piece miniature is not in the tray ($n matching pixels)")
        return Offset((sx / n).toFloat(), (sy / n).toFloat())
    }

    /** [shot], [board] and [puzzle] are kept for the call sites; the transform comes from the `board` node's semantics. */
    @Suppress("UNUSED_PARAMETER")
    fun mapper(shot: Bitmap, board: BoardRect, puzzle: Puzzle): UnitMapper {
        val t = rule.onNodeWithTag("board").fetchSemanticsNode().config.getOrNull(BoardTransform)
            ?: error("the board node carries no BoardTransform")
        return UnitMapper(t.scalePx, t.originXPx, t.originYPx)
    }

    /**
     * A probe point inside the picture's base colour: the first candidate (solution piece centroids, then a 0.25-unit grid over the
     * silhouette) whose surrounding patch of radius 0.15 unit is uniformly [colour] (tolerance 10) in [shot]. So the probe lies on no
     * picture-internal shape edge and on no silhouette corner (DA-70).
     */
    fun probeInside(shot: Bitmap, m: UnitMapper, puzzle: Puzzle, colour: Int): Offset {
        val pts = puzzle.solution.flatMap { sp -> sp.polygon.map { it.x.toDouble() to it.y.toDouble() } }
        val candidates = puzzle.solution.map { sp -> sp.polygon.map { it.x.toDouble() }.average() to sp.polygon.map { it.y.toDouble() }.average() } +
            generateSequence(pts.minOf { it.first }) { it + 0.25 }.takeWhile { it <= pts.maxOf { p -> p.first } }.flatMap { x ->
                generateSequence(pts.minOf { it.second }) { it + 0.25 }.takeWhile { it <= pts.maxOf { p -> p.second } }.map { y -> x to y }
            }.toList()
        val r = 0.15 * m.scalePx
        for ((ux, uy) in candidates) {
            val c = m.px(ux, uy)
            var uniform = true
            loop@ for (dy in -2..2) for (dx in -2..2) {
                val x = (c.x + dx * r / 2).toInt()
                val y = (c.y + dy * r / 2).toInt()
                if (x !in 0 until shot.width || y !in 0 until shot.height || colourDiff(shot.getPixel(x, y), colour) > 10) {
                    uniform = false
                    break@loop
                }
            }
            if (uniform) return c
        }
        error("fixture: no point of the board is uniformly the base colour")
    }

    fun pose(puzzle: Puzzle, piece: PieceId): PlacedPiece {
        val sp = puzzle.solution.first { it.piece == piece }
        return PieceGeometry.poseOf(sp.piece, sp.polygon) ?: error("no pose for $piece of ${puzzle.id.value}")
    }

    /** The solution centroid of [piece] in pixels: inside the piece, so robust against outline anti-aliasing. */
    fun centroidPx(m: UnitMapper, puzzle: Puzzle, piece: PieceId): Offset {
        val poly = puzzle.solution.first { it.piece == piece }.polygon
        return m.px(poly.map { it.x.toDouble() }.average(), poly.map { it.y.toDouble() }.average())
    }

    /** True when the pixel at the solution centroid of [piece] has the piece's colour: it lies on the board in its solution place. */
    fun pieceIsOnBoardAtSolution(shot: Bitmap, m: UnitMapper, puzzle: Puzzle, piece: PieceId): Boolean {
        val c = centroidPx(m, puzzle, piece)
        return colourDiff(shot.getPixel(c.x.toInt(), c.y.toInt()), PieceColours.rgb(piece)) <= 12
    }

    // ---------------------------------------------------------------- touch (needs `rule.mainClock.autoAdvance = false`)

    private fun tap(at: Offset) {
        rule.onNodeWithTag("play-area").performTouchInput { down(at) }
        rule.mainClock.advanceTimeBy(30)
        rule.onNodeWithTag("play-area").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(50)
    }

    private fun drag(start: Offset, target: Offset) {
        val d = density
        rule.onNodeWithTag("play-area").performTouchInput { down(start) }
        rule.mainClock.advanceTimeBy(16)
        rule.onNodeWithTag("play-area").performTouchInput { moveTo(Offset(start.x, start.y - 40f * d)) }
        rule.mainClock.advanceTimeBy(150)
        rule.onNodeWithTag("play-area").performTouchInput { moveTo(target) }
        rule.mainClock.advanceTimeBy(250)
        rule.onNodeWithTag("play-area").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(1200)
    }

    private val turnedInTray = HashSet<PieceId>()

    /**
     * Turns [piece] by taps in the tray (once) and drags it to its solution place, as the WO-003 end-to-end test does (the lowest
     * point of a dragged piece floats 30 dp above the finger, REQ-014 ASSUMPTION). Returns whether it locked.
     */
    fun tryPlace(puzzle: Puzzle, piece: PieceId): Boolean {
        val before = shot()
        val board = board()
        val m = mapper(before, board, puzzle)
        val start = trayCentre(before, board, piece)
        val t = pose(puzzle, piece)
        if (piece !in turnedInTray) {
            val taps = Math.floorMod(t.turn.steps - TrayRules.restingTurn(piece.shape).steps, 8)
            repeat(taps) { tap(start) }
            turnedInTray += piece
        }
        val cx = t.corners.map { it.x.toDouble() }.average()
        val lowest = t.corners.maxOf { it.y.toDouble() }
        val target = Offset(m.px(cx, 0.0).x, m.px(0.0, lowest).y + 30f * density)
        drag(start, target)
        return pieceIsOnBoardAtSolution(shot(), m, puzzle, piece)
    }

    /**
     * Places [wanted] pieces by real touch, trying the pieces that need no flip in solution order and retrying the ones that
     * did not lock (a piece needs an anchor to lock against, so the order is found by trying). Returns the placed pieces.
     */
    fun placePieces(puzzle: Puzzle, wanted: Int): List<PieceId> {
        val placed = mutableListOf<PieceId>()
        val candidates = puzzle.solution.map { it.piece }.filter { !pose(puzzle, it).mirrored }
        repeat(3) {
            for (piece in candidates) {
                if (placed.size == wanted) return placed
                if (piece !in placed && tryPlace(puzzle, piece)) placed += piece
            }
        }
        check(placed.size == wanted) { "fixture: could place only $placed of $wanted pieces of ${puzzle.id.value} by touch" }
        return placed
    }
}

// ---------------------------------------------------------------------------------------------- tag-level touches

/** One touch on the node with [tag]: down, a little clock time, up. Works with the clock paused or running. */
internal fun ComposeTestRule.touch(tag: String) {
    if (mainClock.autoAdvance) {
        onNodeWithTag(tag).performTouchInput { click() }
    } else {
        onNodeWithTag(tag).performTouchInput { down(center) }
        mainClock.advanceTimeBy(30)
        onNodeWithTag(tag).performTouchInput { up() }
        mainClock.advanceTimeBy(200)
    }
}

/** Down on the node with [tag], [holdMs] of TEST-clock time, up (the long press of DA-54 runs on the test clock). */
internal fun ComposeTestRule.hold(tag: String, holdMs: Long) {
    val wasAuto = mainClock.autoAdvance
    mainClock.autoAdvance = false
    onNodeWithTag(tag).performTouchInput { down(center) }
    mainClock.advanceTimeBy(holdMs)
    onNodeWithTag(tag).performTouchInput { up() }
    mainClock.advanceTimeBy(300)
    mainClock.autoAdvance = wasAuto
}
