package io.github.jamisuni.tangram.play.acceptance

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.play.BoardTransform
import io.github.jamisuni.tangram.play.PlayArea
import io.github.jamisuni.tangram.play.PlaySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

// ACCEPTANCE TEST (TASK-T5, independent author), device: decision DA-83. After `solveByAid` on an idle board (no touch at all) the
// REQ-023 timeline runs from the next frame: the pieces stand at their places first and the picture is NOT yet there 300 ms
// later (it fades in from 600 ms after the last piece locks), and it is fully there after the fade and the confetti. A stale or
// immediate start would show the picture at once; a frame loop that never ran for an idle board would never show it.
// Probe: a point inside the picture's base colour found on the settled picture, away from picture-internal edges and outline
// corners (DA-40, DA-70); the transform is the public `BoardTransform` of the `board` node, never inferred from pixels.
// No acceptance tokens: this pins an AI decision.
class AidTimelineDeviceTest {

    @get:Rule
    val compose = createComposeRule()

    private val rows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val puzzle: Puzzle = PuzzleLibrary.packaged().puzzles.first { it.solution.size == 7 }

    private fun poses(): List<PlacedPiece> =
        puzzle.solution.map { PieceGeometry.poseOf(it.piece, it.polygon) ?: error("fixture: no pose for ${it.piece}") }

    private fun shot(): Bitmap = compose.onNodeWithTag("play-area").captureToImage().asAndroidBitmap()

    private fun far(a: Int, b: Int): Int = maxOf(abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)), abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)), abs((a and 0xFF) - (b and 0xFF)))

    private fun probe(shot: Bitmap, base: Int): Offset {
        val t = compose.onNodeWithTag("board").fetchSemanticsNode().config.getOrNull(BoardTransform) ?: error("the board node carries no BoardTransform")
        val pts = puzzle.solution.flatMap { sp -> sp.polygon.map { it.x.toDouble() to it.y.toDouble() } }
        val candidates = puzzle.solution.map { sp -> sp.polygon.map { it.x.toDouble() }.average() to sp.polygon.map { it.y.toDouble() }.average() } +
            generateSequence(pts.minOf { it.first }) { it + 0.25 }.takeWhile { it <= pts.maxOf { p -> p.first } }.flatMap { x ->
                generateSequence(pts.minOf { it.second }) { it + 0.25 }.takeWhile { it <= pts.maxOf { p -> p.second } }.map { y -> x to y }
            }.toList()
        val r = 0.15 * t.scalePx
        for ((ux, uy) in candidates) {
            val cx = (t.originXPx + ux * t.scalePx).toFloat()
            val cy = (t.originYPx + uy * t.scalePx).toFloat()
            var uniform = true
            loop@ for (dy in -2..2) for (dx in -2..2) {
                val x = (cx + dx * r / 2).toInt()
                val y = (cy + dy * r / 2).toInt()
                if (x !in 0 until shot.width || y !in 0 until shot.height || far(shot.getPixel(x, y), base) > 10) {
                    uniform = false
                    break@loop
                }
            }
            if (uniform) return Offset(cx, cy)
        }
        error("fixture: no point of the settled picture is uniformly its base colour")
    }

    // decision DA-83: aid solve on an idle board, no touch: pieces first, picture only after the REQ-023 delay, full at the end.
    @Test
    fun decisionDA83_theAidSolveRunsTheNormalTimelineOnAnIdleBoard() {
        val session = PlaySession(puzzle)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            Box(Modifier.size(360.dp, 640.dp)) { PlayArea(session, LayoutClass.PHONE, rows, 780.dp, Modifier.fillMaxSize()) }
        }
        compose.mainClock.advanceTimeBy(500) // an idle board: no drag, no animation, the frame loop is asleep

        assertTrue("the aid solve was refused", compose.runOnUiThread { session.solveByAid(poses()) })
        assertEquals(PuzzleState.SOLVED, compose.runOnUiThread { session.state })
        compose.mainClock.advanceTimeBy(16) // the next frame starts the timeline
        assertTrue("the pending solve was not resolved by a frame on an idle board", compose.runOnUiThread { session.solved != null })

        compose.mainClock.advanceTimeBy(300)
        val early = shot()

        compose.mainClock.advanceTimeBy(3200) // 600 ms delay + 800 ms fade + 2.4 s confetti, with room
        val late = shot()
        val base = 0xFF000000.toInt() or puzzle.picture.base.value
        val at = probe(late, base)
        val x = at.x.toInt()
        val y = at.y.toInt()
        assertTrue("fixture: the settled picture shows its base colour at the probe", far(late.getPixel(x, y), base) <= 10)
        assertTrue(
            "300 ms after the solve the picture must not be there yet (a stale or immediate start?): pixel ${Integer.toHexString(early.getPixel(x, y))} vs base ${Integer.toHexString(base)}",
            far(early.getPixel(x, y), base) > 20,
        )
    }
}
