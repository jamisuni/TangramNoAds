package io.github.jamisuni.tangram.play

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// SCAFFOLDING (TASK-018b): disposable device tests of the pointer adapter and PlayArea. Not acceptance tests.
// Touch is injected with performTouchInput only. The clock is manual because a drag keeps a frame loop alive.
class PlayAreaAdapterScaffoldingTest {

    @get:Rule
    val rule = createComposeRule()

    private val rows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val puzzle = PuzzleLibrary.packaged().puzzles.first { it.id.value == "shapes-mini-1" }
    private var show by mutableStateOf(true)
    private var width by mutableStateOf(360.0)

    private fun start(session: PlaySession, advance: Boolean = true) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            if (show) {
                Box(Modifier.size(width.dp, 640.dp)) {
                    PlayArea(session, LayoutClass.PHONE, rows, 780.dp, Modifier.fillMaxSize())
                }
            }
        }
        if (advance) rule.mainClock.advanceTimeBy(100)
    }

    private fun cellPoint(piece: PieceId): Offset {
        val c = PlayLayout.compute(360.0, 640.0, 780.0, LayoutClass.PHONE, rows, puzzle).cell(piece)
        val d = rule.density.density
        return Offset(((c.left + 8.0) * d).toFloat(), ((c.bottom - 8.0) * d).toFloat())
    }

    private fun up(dpY: Float) = Offset(0f, dpY * rule.density.density)

    private fun dragInto(session: PlaySession) {
        val p = cellPoint(PieceId.ST1)
        rule.onNodeWithTag("play-area").performTouchInput { down(p); }
        rule.mainClock.advanceTimeBy(16)
        rule.onNodeWithTag("play-area").performTouchInput { moveTo(p + up(-60f)) }
        rule.mainClock.advanceTimeBy(200)
        assertNotNull("drag started", rule.runOnUiThread { session.drag })
    }

    private fun where(session: PlaySession, piece: PieceId) = rule.runOnUiThread { session.pieces.first { it.piece == piece }.where }

    @Test
    fun consumedUpIsACancel() {
        val session = PlaySession(puzzle, reducedMotion = { true })
        start(session)
        dragInto(session)
        rule.onNodeWithTag("play-area").performTouchInput { cancel() }
        rule.mainClock.advanceTimeBy(50)
        assertNull(rule.runOnUiThread { session.drag })
        assertEquals(Where.Tray, where(session, PieceId.ST1))
        assertEquals(PuzzleState.IN_PROGRESS, session.state)
        assertNull("no pulse on a cancel", rule.runOnUiThread { session.pulse })
    }

    private fun tapCell(piece: PieceId) {
        val p = cellPoint(piece)
        rule.onNodeWithTag("play-area").performTouchInput { down(p) }
        rule.mainClock.advanceTimeBy(30)
        rule.onNodeWithTag("play-area").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(50)
    }

    private fun turnOf(session: PlaySession, piece: PieceId) =
        rule.runOnUiThread { session.pieces.first { it.piece == piece }.turn.steps }

    // CR-1 N2, same composition: a gesture ends without an unconsumed up (cancelled mid-press), and the next tap in
    // the same PlayArea (same GestureMachine) must still work. A leaked Pressed state would turn it into an ignored touch.
    @Test
    fun cancelMidPressDoesNotFreezeTheNextGestureInTheSameComposition() {
        val session = PlaySession(puzzle, reducedMotion = { true })
        start(session)
        val before = turnOf(session, PieceId.ST1)
        val p = cellPoint(PieceId.ST1)
        rule.onNodeWithTag("play-area").performTouchInput { down(p) }
        rule.mainClock.advanceTimeBy(30)
        rule.onNodeWithTag("play-area").performTouchInput { cancel() }
        rule.mainClock.advanceTimeBy(50)
        assertEquals("a cancel is not a tap", before, turnOf(session, PieceId.ST1))
        tapCell(PieceId.ST1)
        assertEquals("the next gesture works: a tap turns", (before + 1) % 8, turnOf(session, PieceId.ST1))
    }

    @Test
    fun cancelMidDragDoesNotFreezeTheNextGestureInTheSameComposition() {
        val session = PlaySession(puzzle, reducedMotion = { true })
        start(session)
        val before = turnOf(session, PieceId.ST1)
        dragInto(session)
        rule.onNodeWithTag("play-area").performTouchInput { cancel() }
        rule.mainClock.advanceTimeBy(50)
        assertNull(rule.runOnUiThread { session.drag })
        tapCell(PieceId.ST1)
        assertEquals((before + 1) % 8, turnOf(session, PieceId.ST1))
    }

    // The DisposableEffect half: leaving the composition cancels a live drag (the frame loop and queue die with it).
    @Test
    fun leavingTheCompositionCancelsTheDrag() {
        val session = PlaySession(puzzle, reducedMotion = { true })
        start(session)
        dragInto(session)
        rule.runOnUiThread { show = false }
        rule.mainClock.advanceTimeBy(100)
        assertNull("drag interrupted by the detach", rule.runOnUiThread { session.drag })
        assertEquals(Where.Tray, where(session, PieceId.ST1))
    }

    // CR F1: a rebuilt composition for a surviving session must not restart the animation clock at 0.
    @Test
    fun recreatingTheCompositionOfASolvedSessionKeepsThePicture() {
        val session = PlaySession(puzzle, reducedMotion = { true })
        start(session)
        for (sol in puzzle.solution) placePiece(session, sol.piece)
        rule.mainClock.advanceTimeBy(3_000)
        assertEquals(PuzzleState.SOLVED, session.state)
        val c = rule.runOnUiThread { session.layout!!.toDp(centreUnits(PieceId.SQ)) }
        val d = rule.density.density
        fun sample() = rule.onNodeWithTag("play-area").captureToImage().asAndroidBitmap()
            .getPixel((c.x * d).toInt(), (c.y * d).toInt())
        val settled = sample()
        rule.runOnUiThread { show = false }
        rule.mainClock.advanceTimeBy(50)
        rule.runOnUiThread { show = true }
        // one frame: a solved session is not animating, so the frame loop sleeps and the seeded clock is all there is
        rule.mainClock.advanceTimeBy(16)
        assertEquals("first drawn frame after recreation still shows the picture", settled, sample())
        assertTrue(rule.runOnUiThread { session.lastFrameMs } > 0L)
    }

    @Test
    fun sizeChangeMidDragCancelsAndLaterEventsAreHarmless() {
        val session = PlaySession(puzzle, reducedMotion = { true })
        start(session)
        dragInto(session)
        rule.runOnUiThread { width = 340.0 }
        rule.mainClock.advanceTimeBy(100)
        assertNull("size change interrupted the drag", rule.runOnUiThread { session.drag })
        assertEquals(Where.Tray, where(session, PieceId.ST1))
        rule.onNodeWithTag("play-area").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(50)
        assertNull(rule.runOnUiThread { session.drag })
    }

    @Test
    fun boardNodeCarriesTheBoardTransform() {
        val session = PlaySession(puzzle, reducedMotion = { true })
        start(session)
        val cfg = rule.onNodeWithTag("board").fetchSemanticsNode().config
        val t = cfg[BoardTransform]
        assertTrue(t.scalePx > 0f)
        assertTrue(t.trayCellsPx.keys.containsAll(listOf(PieceId.SQ, PieceId.ST1, PieceId.ST2)))
    }

    /** Locks SQ at its solution pose through the session API (UI thread); returns the px point of its centre. */
    private fun centreUnits(piece: PieceId): io.github.jamisuni.tangram.kernel.geometry.Vec2 {
        val sq = io.github.jamisuni.tangram.kernel.geometry.PieceGeometry.poseOf(
            piece, puzzle.solution.first { it.piece == piece }.polygon,
        )!!
        val c = io.github.jamisuni.tangram.kernel.geometry.PieceGeometry.centroidOffset(piece.shape, sq.turn, sq.mirrored)
        return io.github.jamisuni.tangram.kernel.geometry.Vec2(sq.at.x.toDouble() + c.x, sq.at.y.toDouble() + c.y)
    }

    private fun placePiece(session: PlaySession, piece: PieceId) = rule.runOnUiThread {
        val lay = session.layout!!
        val sq = io.github.jamisuni.tangram.kernel.geometry.PieceGeometry.poseOf(
            piece, puzzle.solution.first { it.piece == piece }.polygon,
        )!!
        session.beginDrag(piece, lay.cell(piece).centre, 500)
        session.setDragTurn(sq.turn)
        val c = io.github.jamisuni.tangram.kernel.geometry.PieceGeometry.centroidOffset(piece.shape, sq.turn, sq.mirrored)
        val centre = lay.toDp(io.github.jamisuni.tangram.kernel.geometry.Vec2(sq.at.x.toDouble() + c.x, sq.at.y.toDouble() + c.y))
        session.dragTo(io.github.jamisuni.tangram.kernel.geometry.Vec2(centre.x, centre.y + session.drag!!.lift))
        session.onFrame(1000)
        assertTrue(session.release(1000) is DropOutcome.Locked)
    }

    private fun placeSq(session: PlaySession): Offset = rule.runOnUiThread {
        val lay = session.layout!!
        val sq = io.github.jamisuni.tangram.kernel.geometry.PieceGeometry.poseOf(
            PieceId.SQ, puzzle.solution.first { it.piece == PieceId.SQ }.polygon,
        )!!
        session.beginDrag(PieceId.SQ, lay.cell(PieceId.SQ).centre, 0)
        session.setDragTurn(sq.turn)
        val c = io.github.jamisuni.tangram.kernel.geometry.PieceGeometry.centroidOffset(PieceId.SQ.shape, sq.turn, sq.mirrored)
        val centre = lay.toDp(io.github.jamisuni.tangram.kernel.geometry.Vec2(sq.at.x.toDouble() + c.x, sq.at.y.toDouble() + c.y))
        session.dragTo(io.github.jamisuni.tangram.kernel.geometry.Vec2(centre.x, centre.y + session.drag!!.lift))
        session.onFrame(500)
        assertTrue(session.release(500) is DropOutcome.Locked)
        val d = rule.density.density
        Offset((centre.x * d).toFloat(), (centre.y * d).toFloat())
    }

    @Test
    fun boardTapAfterLongIdleShakesWithRealFrameTime() {
        val session = PlaySession(puzzle, reducedMotion = { true })
        start(session)
        val at = placeSq(session)
        rule.mainClock.advanceTimeBy(10_000) // long idle: the loop sleeps, nothing pending
        // down and up arrive with no clock advance in between
        rule.onNodeWithTag("play-area").performTouchInput { down(at); up() }
        rule.mainClock.advanceTimeBy(16)
        val shake = rule.runOnUiThread { session.shake }
        assertNotNull("the refused turn shakes", shake)
        assertTrue("stamped with a real frame time, not stale: ${shake!!.startMs}", shake.startMs >= 9_000)
        rule.mainClock.advanceTimeBy(200)
        assertNotNull("alive ~400 ms", rule.runOnUiThread { session.shake })
        rule.mainClock.advanceTimeBy(300)
        assertNull(rule.runOnUiThread { session.shake })
    }

    @Test
    fun tapBeforeTheFirstFrameGetsARealFrameTime() {
        val session = PlaySession(puzzle, reducedMotion = { true })
        start(session, advance = false)
        val at = placeSq(session)
        rule.onNodeWithTag("play-area").performTouchInput { down(at); up() }
        rule.mainClock.advanceTimeBy(16)
        val shake = rule.runOnUiThread { session.shake }
        assertNotNull("the refused turn shakes", shake)
        assertTrue("frame time, not 0: ${shake!!.startMs}", shake.startMs > 0)
    }
}
