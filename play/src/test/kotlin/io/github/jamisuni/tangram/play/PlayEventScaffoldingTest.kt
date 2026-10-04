package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// SCAFFOLDING (disposable, TASK-054; decision DA-115): not an acceptance test. Each emission point and the silent cases.
class PlayEventScaffoldingTest {
    private val rows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val mini: Puzzle = PuzzleLibrary.packaged().puzzles.first()

    /** One ordered log: events as their name, the session's onChanged as "settled". */
    private val log = ArrayList<String>()

    private fun setup(): PlaySession {
        val s = PlaySession(mini, { false }, onChanged = { log += "settled" })
        s.layout = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, rows, mini)
        s.onEvent = { log += it.name }
        return s
    }

    private fun pose(p: PieceId): PlacedPiece = PieceGeometry.poseOf(p, mini.solution.first { it.piece == p }.polygon)!!

    private fun fingerFor(s: PlaySession, pose: PlacedPiece): Vec2 {
        val c = PieceGeometry.centroidOffset(pose.piece.shape, pose.turn, pose.mirrored)
        val centre = s.layout!!.toDp(Vec2(pose.at.x.toDouble() + c.x, pose.at.y.toDouble() + c.y))
        return Vec2(centre.x, centre.y + s.drag!!.lift)
    }

    private fun dropOn(s: PlaySession, p: PlacedPiece, t0: Long) {
        s.beginDrag(p.piece, s.layout!!.cell(p.piece).centre, t0)
        s.setDragTurn(p.turn)
        s.dragTo(fingerFor(s, p))
        s.onFrame(t0 + 500)
        s.release(t0 + 500)
    }

    @Test fun pickUpThenLockThenSettled() {
        val s = setup()
        val p = s.pieces.first().piece
        s.beginDrag(p, s.layout!!.cell(p).centre, 0)
        assertEquals(listOf("PICK_UP"), log)
        s.setDragTurn(pose(p).turn) // may emit TURN when the solution turn differs from the resting turn
        s.dragTo(fingerFor(s, pose(p)))
        s.onFrame(500)
        s.release(500)
        assertEquals(listOf("PICK_UP", "LOCK", "settled"), log.filter { it != "TURN" })
    }

    @Test fun twistStepEmitsTurnOnlyWhenTheTurnChanges() {
        val s = setup()
        val p = s.pieces.first().piece
        s.beginDrag(p, s.layout!!.cell(p).centre, 0)
        val t = s.drag!!.turn
        s.setDragTurn(t) // same turn: silent
        s.setDragTurn(Turn((t.steps + 1) % 8))
        s.setDragTurn(Turn((t.steps + 1) % 8)) // same again: silent
        assertEquals(listOf("PICK_UP", "TURN"), log)
        s.setDragTurn(Turn((t.steps + 2) % 8))
        assertEquals(listOf("PICK_UP", "TURN", "TURN"), log)
    }

    @Test fun trayTapAndTrayMirrorEmitTurnBeforeSettled() {
        val s = setup()
        val p = s.pieces.first().piece
        s.tapTray(p)
        s.flipTray(p)
        assertEquals(listOf("TURN", "settled", "TURN", "settled"), log)
    }

    @Test fun boardTurnAndMirrorEmitOnlyWhenAccepted() {
        val s = setup()
        val p = s.pieces.first().piece
        dropOn(s, pose(p), 0)
        log.clear()
        val before = s.pieces.first { it.piece == p }
        s.tapBoard(p, 2000)
        if (s.pieces.first { it.piece == p }.turn != before.turn) {
            assertEquals(listOf("TURN", "settled"), log)
        } else {
            assertTrue(s.shake != null)
            assertEquals(emptyList<String>(), log)
        }
        log.clear()
        val m = s.pieces.first { it.piece == p }
        s.flipBoard(p, 3000)
        if (s.pieces.first { it.piece == p }.mirrored != m.mirrored) {
            assertEquals(listOf("TURN", "settled"), log)
        } else {
            assertEquals(emptyList<String>(), log)
        }
    }

    @Test fun missEmitsReturnAndTrayDropEmitsReturn() {
        val s = setup()
        val p = s.pieces.first().piece
        val lay = s.layout!!
        s.beginDrag(p, lay.cell(p).centre, 0)
        s.dragTo(Vec2(lay.boardRect.right - 2, lay.boardRect.top + 2 + s.drag!!.lift))
        s.onFrame(500)
        s.release(500)
        assertEquals(listOf("PICK_UP", "RETURN", "settled"), log)
        log.clear()
        s.beginDrag(p, lay.cell(p).centre, 1000)
        s.dragTo(Vec2(200.0, lay.areaHeight + 300.0))
        s.onFrame(1500)
        s.release(1500)
        assertEquals(listOf("PICK_UP", "RETURN", "settled"), log)
    }

    @Test fun completingLockEmitsLockThenSolveBeforeSettled() {
        val s = setup()
        var t = 0L
        for (p in s.pieces.map { it.piece }) {
            log.clear()
            dropOn(s, pose(p), t)
            t += 1000
        }
        assertEquals(PuzzleState.SOLVED, s.state)
        assertEquals(listOf("LOCK", "SOLVE", "settled"), log.filter { it != "PICK_UP" && it != "TURN" })
    }

    @Test fun solveByAidEmitsSolveAloneBeforeSettled() {
        val s = setup()
        assertTrue(s.solveByAid(mini.solution.map { PieceGeometry.poseOf(it.piece, it.polygon)!! }))
        assertEquals(listOf("SOLVE", "settled"), log)
    }

    @Test fun interruptAndRestoreAndToProgressAreSilent() {
        val s = setup()
        val p = s.pieces.first().piece
        s.beginDrag(p, s.layout!!.cell(p).centre, 0)
        log.clear()
        s.interruptDrag()
        s.toProgress(PuzzleProgress(PuzzleState.NEW, emptyMap(), 0, null))
        val fresh = setup()
        fresh.restore(PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 0, null))
        assertEquals(emptyList<String>(), log)
    }
}
