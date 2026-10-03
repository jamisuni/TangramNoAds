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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// SCAFFOLDING (disposable, TASK-031b): not an acceptance test.
class SolveByAidScaffoldingTest {
    private val rows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val puzzle: Puzzle = PuzzleLibrary.packaged().puzzles.first()
    private val poses: List<PlacedPiece> =
        puzzle.solution.map { PieceGeometry.poseOf(it.piece, it.polygon)!! }

    private class Log {
        val solved = ArrayList<Boolean>()
        var changed = 0
    }

    private fun session(log: Log = Log()): PlaySession =
        PlaySession(puzzle, { false }, onChanged = { log.changed++ }, onSolved = { log.solved += it }).also {
            it.layout = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, rows, puzzle)
        }

    // The early-return trap (plan review F5): no drag was ever started, ONE idle frame must resolve the pending solve.
    @Test fun scaffoldTrapIdleFrameResolvesPendingSolve() {
        val log = Log()
        val s = session(log)
        assertTrue(s.solveByAid(poses))
        assertTrue(s.solvePending)
        assertNull(s.solved)
        assertNull(s.drag)
        s.onFrame(1000L)
        assertFalse(s.solvePending)
        assertEquals(PuzzleState.SOLVED, s.state)
        assertEquals(1000L, s.solved!!.t0)
        assertEquals(listOf(true), log.solved)
        assertEquals(1, log.changed)
    }

    @Test fun placedEqualsPosesAndGlidesCleared() {
        val s = session()
        assertTrue(s.solveByAid(poses))
        assertEquals(poses.toSet(), s.placed.toSet())
        assertTrue(s.glides.isEmpty())
        assertNull(s.pulse)
        assertNull(s.shake)
    }

    @Test fun invalidPosesReturnFalseAndChangeNothing() {
        val log = Log()
        val s = session(log)
        val v = s.version
        val bad = poses.toMutableList().also { it[0] = it[0].copy(at = it[1].at) }
        assertFalse(s.solveByAid(bad))
        assertFalse(s.solveByAid(poses.drop(1)))
        assertFalse(s.solveByAid(poses + poses.first()))
        assertFalse(s.solveByAid(poses.map { it.copy(turn = Turn((it.turn.steps + 1) % 8)) }))
        assertEquals(v, s.version)
        assertEquals(PuzzleState.NEW, s.state)
        assertTrue(s.placed.isEmpty())
        assertFalse(s.solvePending)
        assertEquals(0, log.changed)
        assertTrue(log.solved.isEmpty())
    }

    @Test fun draggingIsInterruptedThenSolvedAndSolvedReturnsFalse() {
        val log = Log()
        val s = session(log)
        val cell = s.layout!!.cell(poses[0].piece).centre
        s.beginDrag(poses[0].piece, cell, 0L)
        assertTrue(s.isDragging)
        assertTrue(s.solveByAid(poses))
        assertFalse(s.isDragging)
        assertNull(s.drag)
        assertEquals(poses.toSet(), s.placed.toSet())
        s.onFrame(10L)
        val before = s.version
        assertFalse(s.solveByAid(poses))
        assertEquals(before, s.version)
        assertEquals(listOf(true), log.solved)
        assertEquals(1, log.changed)
    }

    @Test fun dropSolveFiresOnSolvedFalseExactlyOnce() {
        val log = Log()
        val s = session(log)
        var t = 0L
        for (p in s.pieces.map { it.piece }) {
            val pose = poses.first { it.piece == p }
            val lay = s.layout!!
            s.beginDrag(p, lay.cell(p).centre, t)
            s.setDragTurn(pose.turn)
            val c = PieceGeometry.centroidOffset(p.shape, pose.turn, pose.mirrored)
            val centre = lay.toDp(Vec2(pose.at.x.toDouble() + c.x, pose.at.y.toDouble() + c.y))
            s.dragTo(Vec2(centre.x, centre.y + s.drag!!.lift))
            s.onFrame(t + 500)
            s.release(t + 500)
            t += 1000
        }
        assertEquals(PuzzleState.SOLVED, s.state)
        s.onFrame(t + 100)
        s.onFrame(t + 200)
        assertEquals(listOf(false), log.solved)
        assertFalse(s.solvePending)
        assertNotNull(s.solved)
    }

    @Test fun restoreNeverFiresOnSolved() {
        val log = Log()
        val s = session(log)
        s.restore(PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 0, null))
        s.onFrame(100L)
        assertEquals(PuzzleState.SOLVED, s.state)
        assertTrue(log.solved.isEmpty())
        assertEquals(0, log.changed)
    }
}
