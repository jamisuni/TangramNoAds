package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.layout.TrayRules
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// SCAFFOLDING (disposable, TASK-024): onChanged events, toProgress/restore round trip. Not an acceptance test.
class SessionPersistenceScaffoldingTest {
    private val rows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val mini: Puzzle = PuzzleLibrary.packaged().puzzles.first()
    private var calls = 0

    private fun fresh(): PlaySession {
        val s = PlaySession(mini, { false }, onChanged = { calls++ })
        s.layout = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, rows, mini)
        return s
    }

    private fun pose(piece: PieceId): PlacedPiece =
        PieceGeometry.poseOf(piece, mini.solution.first { it.piece == piece }.polygon)!!

    private fun drag(s: PlaySession, p: PlacedPiece, t0: Long) {
        val lay = s.layout!!
        s.beginDrag(p.piece, lay.cell(p.piece).centre, t0)
        s.setDragTurn(p.turn)
        val c = PieceGeometry.centroidOffset(p.piece.shape, p.turn, p.mirrored)
        val centre = lay.toDp(Vec2(p.at.x.toDouble() + c.x, p.at.y.toDouble() + c.y))
        s.dragTo(Vec2(centre.x, centre.y + s.drag!!.lift))
        s.onFrame(t0 + 500)
    }

    private fun lock(s: PlaySession, piece: PieceId, t0: Long = 0) {
        drag(s, pose(piece), t0)
        s.release(t0 + 500)
    }

    private fun solveAll(s: PlaySession) {
        s.pieces.map { it.piece }.forEachIndexed { i, p -> lock(s, p, i * 1000L) }
    }

    @Test fun lockFiresOnce_notMidDrag() {
        val s = fresh()
        val p = s.pieces.first().piece
        drag(s, pose(p), 0)
        s.dragTo(Vec2(10.0, 10.0))
        s.onFrame(900)
        assertEquals(0, calls) // decision DA-49
        s.release(1000)
        assertEquals(1, calls)
    }

    @Test fun returnHomeFires() {
        val s = fresh()
        val p = s.pieces.first().piece
        s.beginDrag(p, s.layout!!.cell(p).centre, 0)
        s.onFrame(100)
        s.release(200) // dropped where it started: goes home
        assertEquals(1, calls)
        assertTrue(s.pieces.first { it.piece == p }.where == Where.Tray)
    }

    @Test fun trayTurnAndMirrorFire_noopsDoNot() {
        val s = fresh()
        val p = s.pieces.first().piece
        s.tapTray(p)
        s.flipTray(p)
        assertEquals(2, calls)
        lock(s, p)
        val after = calls
        s.tapTray(p) // on the board now: no-op
        s.flipTray(p)
        assertEquals(after, calls)
    }

    @Test fun boardTurnFiresOnlyWhenKept() {
        val s = fresh()
        val p = s.pieces.first().piece
        lock(s, p)
        val before = calls
        val turnBefore = s.pieces.first { it.piece == p }.turn
        s.tapBoard(p, 5000)
        val turned = s.pieces.first { it.piece == p }.turn != turnBefore
        assertEquals(if (turned) before + 1 else before, calls)
    }

    @Test fun beginDragInterruptToProgressRestoreNeverFire() {
        val s = fresh()
        val p = s.pieces.first().piece
        s.beginDrag(p, s.layout!!.cell(p).centre, 0)
        assertEquals(0, calls)
        val prog = s.toProgress(PuzzleProgress.NEW)
        assertFalse(s.isDragging) // F5: interrupted first
        assertEquals(PuzzleState.IN_PROGRESS, prog.state)
        assertEquals(0, calls)
        PlaySession(mini, onChanged = { calls++ }).restore(prog)
        assertEquals(0, calls)
    }

    @Test fun stateAndIsDraggingAreObservable() {
        val s = fresh()
        val v0 = s.version
        val p = s.pieces.first().piece
        s.beginDrag(p, s.layout!!.cell(p).centre, 0)
        assertTrue(s.version != v0)
        assertTrue(s.isDragging)
        assertEquals(PuzzleState.IN_PROGRESS, s.state)
    }

    @Test fun roundTripKeepsExactAtTurnMirror() {
        val s = fresh()
        val ids = s.pieces.map { it.piece }
        lock(s, ids[0])
        s.tapTray(ids[1])
        s.flipTray(ids[1])
        val base = PuzzleProgress(PuzzleState.NEW, emptyMap(), 12, 7)
        val prog = s.toProgress(base)
        assertEquals(12L, prog.puzzleSeconds)
        assertEquals(7L, prog.bestSeconds)
        assertEquals(ids.toSet(), prog.pieces.keys) // every piece, any state
        val onBoard = prog.pieces[ids[0]] as PieceSave.OnBoard
        val t = prog.pieces[ids[1]] as PieceSave.InTray
        assertTrue(t.mirrored)
        val r = PlaySession(mini)
        r.restore(prog)
        assertEquals(prog.state, r.state)
        assertEquals(prog, r.toProgress(base))
        val back = r.pieces.first { it.piece == ids[0] }
        assertEquals(Where.Board(onBoard.at), back.where)
        assertEquals(onBoard.turn, back.turn)
        assertEquals(
            Turn((TrayRules.restingTurn(ids[1].shape).steps + 1) % 8),
            r.pieces.first { it.piece == ids[1] }.turn,
        )
        assertNull(r.solved)
    }

    @Test fun newRestoreAppliesTrayTurnsOnly() {
        val s = fresh()
        val p = s.pieces.first().piece
        s.tapTray(p)
        val prog = s.toProgress(PuzzleProgress.NEW)
        assertEquals(PuzzleState.NEW, prog.state)
        val r = PlaySession(mini)
        r.restore(prog)
        assertEquals(PuzzleState.NEW, r.state)
        assertEquals(s.pieces.first { it.piece == p }.turn, r.pieces.first { it.piece == p }.turn)
    }

    @Test fun completeInProgressRestoreIsSolvedSettle() { // decision DA-66
        val s = fresh()
        solveAll(s)
        assertEquals(PuzzleState.SOLVED, s.state)
        val complete = s.toProgress(PuzzleProgress.NEW).copy(state = PuzzleState.IN_PROGRESS)
        val r = PlaySession(mini, onChanged = { calls++ })
        val before = calls
        r.restore(complete)
        assertSettled(r)
        assertEquals(before, calls)
    }

    @Test fun solvedRestoreIsSettled() {
        val s = fresh()
        solveAll(s)
        val r = PlaySession(mini)
        r.restore(s.toProgress(PuzzleProgress.NEW)) // state SOLVED with pieces: nothing may be placed
        assertSettled(r)
    }

    private fun assertSettled(r: PlaySession) {
        assertEquals(PuzzleState.SOLVED, r.state)
        assertTrue(r.placed.isEmpty())
        assertTrue(r.pieces.all { it.where == Where.Tray })
        val ms = 0L - r.solved!!.t0
        assertTrue(SolvedTimeline.piecesHidden(ms))
        assertEquals(1.0, SolvedTimeline.pictureAlpha(ms), 0.0)
        assertEquals(1.0, SolvedTimeline.popScale(ms, false), 0.0)
        assertFalse(SolvedTimeline.confettiActive(ms, false))
    }

    @Test fun partialInProgressRestoreStaysInProgress() {
        val s = fresh()
        lock(s, s.pieces.first().piece)
        val r = PlaySession(mini)
        r.restore(s.toProgress(PuzzleProgress.NEW))
        assertEquals(PuzzleState.IN_PROGRESS, r.state)
        assertNull(r.solved)
    }
}
