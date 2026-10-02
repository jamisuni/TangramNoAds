package io.github.jamisuni.tangram.acceptance

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import io.github.jamisuni.tangram.MainActivity
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.layout.TrayRules
import io.github.jamisuni.tangram.kernel.model.PieceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

/**
 * Cross-slice end to end on the real app (design "Test seams": `app/src/androidTest` carries the token). The app opens on the
 * first puzzle of the library (the rating-1 mini puzzle, REQ-045); it is solved with injected touches only, and the board is
 * read from a screenshot - the test needs no seam beyond the `play-area` and `board` tags and the TYPE-001 colours.
 */
class FirstPuzzleSolvedTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun diff(a: Int, b: Int): Int = maxOf(
        abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)),
        abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)),
        abs((a and 0xFF) - (b and 0xFF)),
    )

    private fun shot(): Bitmap = rule.onNodeWithTag("play-area").captureToImage().asAndroidBitmap()

    private fun rgb(hex: Int) = 0xFF000000.toInt() or hex

    private val colours = mapOf(
        PieceId.SQ to 0xFFD23F, PieceId.ST1 to 0x2DBE7E, PieceId.ST2 to 0x9B5DE5, PieceId.LT1 to 0xE8505B,
        PieceId.LT2 to 0x3D8BFD, PieceId.MT to 0xF9A826, PieceId.PG to 0xFF7AB8,
    )

    // REQ-045.A2 - "Placing those three pieces shows the solved picture."  Also REQ-002.A1 on screen: the first puzzle is
    // completed with pieces of TYPE-001 by drag, turn (tap) and drop only.
    @Test
    fun req045_A2_req002_A1_firstPuzzleIsSolvedByRealTouchAndShowsThePicture() {
        rule.mainClock.autoAdvance = false
        rule.mainClock.advanceTimeBy(200)
        val puzzle = PuzzleLibrary.packaged().puzzles.first()
        val d = rule.density.density
        val area = rule.onNodeWithTag("play-area").fetchSemanticsNode().boundsInRoot
        val boardNode = rule.onNodeWithTag("board").fetchSemanticsNode().boundsInRoot
        val bx0 = (boardNode.left - area.left).toInt()
        val bx1 = (boardNode.right - area.left).toInt()
        val by0 = (boardNode.top - area.top).toInt()
        val by1 = (boardNode.bottom - area.top).toInt()

        // 1. read the empty board: background and the silhouette's pixel bounding box
        val first = shot()
        val bg = first.getPixel((bx0 + bx1) / 2, by0 + 6)
        var minX = Int.MAX_VALUE
        var maxX = -1
        var minY = Int.MAX_VALUE
        var maxY = -1
        for (y in by0 + 6 until by1 - 6 step 2) {
            for (x in bx0 + 6 until bx1 - 6 step 2) {
                if (diff(first.getPixel(x, y), bg) > 40) {
                    minX = minOf(minX, x)
                    maxX = maxOf(maxX, x)
                    minY = minOf(minY, y)
                    maxY = maxOf(maxY, y)
                }
            }
        }
        assertTrue("fixture: a silhouette is visible on the empty board", maxX > minX && maxY > minY)
        val targets = puzzle.solution.map { PieceGeometry.poseOf(it.piece, it.polygon)!! }
        val pts = targets.flatMap { t -> t.corners.map { it.x.toDouble() to it.y.toDouble() } }
        val ux0 = pts.minOf { it.first }
        val ux1 = pts.maxOf { it.first }
        val uy0 = pts.minOf { it.second }
        val scale = (maxX - minX + 2) / (ux1 - ux0) // px per unit (step-2 sampling: +-1 px)
        fun px(ux: Double, uy: Double) = Offset((minX + (ux - ux0) * scale).toFloat(), (minY + (uy - uy0) * scale).toFloat())

        // 2. find the tray miniatures by their TYPE-001 colours below the board
        val trayPieces = puzzle.solution.map { it.piece }
        assertEquals("fixture: a fresh install opens on a puzzle with three pieces (REQ-045 A1)", 3, trayPieces.size)
        val centres = HashMap<PieceId, Offset>()
        for (piece in trayPieces) {
            var sx = 0.0
            var sy = 0.0
            var n = 0
            for (y in by1 until first.height step 2) {
                for (x in 0 until first.width step 2) {
                    if (diff(first.getPixel(x, y), rgb(colours.getValue(piece))) <= 10) {
                        sx += x
                        sy += y
                        n++
                    }
                }
            }
            assertTrue("fixture: the $piece miniature is in the tray", n > 20)
            centres[piece] = Offset((sx / n).toFloat(), (sy / n).toFloat())
        }

        // 3. place the pieces: the triangles first, the square last; the unit point (1.9, 1.0) lies inside the square only
        val order = trayPieces.sortedBy { if (it == PieceId.SQ) 1 else 0 }
        val probe = px(1.9, 1.0)
        for ((i, piece) in order.withIndex()) {
            val t: PlacedPiece = targets.first { it.piece == piece }
            val start = centres.getValue(piece)
            val taps = Math.floorMod(t.turn.steps - TrayRules.restingTurn(piece.shape).steps, 8)
            repeat(taps) {
                rule.onNodeWithTag("play-area").performTouchInput { down(start) }
                rule.mainClock.advanceTimeBy(30)
                rule.onNodeWithTag("play-area").performTouchInput { up() }
                rule.mainClock.advanceTimeBy(50)
            }
            // REQ-014 ASSUMPTION: the lowest point of a dragged piece floats 30 dp above the finger (phone)
            val cx = t.corners.map { it.x.toDouble() }.average()
            val lowest = t.corners.maxOf { it.y.toDouble() }
            val target = Offset(px(cx, 0.0).x, px(0.0, lowest).y + 30f * d)
            rule.onNodeWithTag("play-area").performTouchInput { down(start) }
            rule.mainClock.advanceTimeBy(16)
            rule.onNodeWithTag("play-area").performTouchInput { moveTo(Offset(start.x, start.y - 40f * d)) }
            rule.mainClock.advanceTimeBy(150)
            rule.onNodeWithTag("play-area").performTouchInput { moveTo(target) }
            rule.mainClock.advanceTimeBy(250)
            rule.onNodeWithTag("play-area").performTouchInput { up() }
            rule.mainClock.advanceTimeBy(400)
            if (i < order.size - 1) {
                val mid = shot().getPixel(probe.x.toInt(), probe.y.toInt())
                assertTrue("not solved yet after ${i + 1} of ${order.size} pieces", diff(mid, rgb(puzzle.picture.base.value)) > 20)
            }
        }

        // 4. the solved picture is shown (REQ-023 rules: it fades in 600-1400 ms after the last lock; wait past the confetti)
        rule.mainClock.advanceTimeBy(3000)
        val done = shot().getPixel(probe.x.toInt(), probe.y.toInt())
        assertTrue(
            "the solved picture's base colour shows at the probe point: got ${Integer.toHexString(done)}",
            diff(done, rgb(puzzle.picture.base.value)) <= 10,
        )
    }
}
