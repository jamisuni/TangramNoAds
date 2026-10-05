package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PieceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// SCAFFOLDING (TASK-065, disposable): timerRect basics and the solvedListener call sites. No acceptance claim.
class TimerRectScaffoldingTest {
    private val puzzle = PuzzleLibrary.packaged().puzzles.first()
    private val rows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val layout = PlayLayout.compute(360.0, 700.0, 780.0, LayoutClass.PHONE, rows, puzzle)

    @Test
    fun noObstacleIsTopRightInset8() {
        val r = PlayLayout.timerRect(layout, emptyList(), 90.0, 28.0)
        val b = layout.boardRect
        assertEquals(b.right - 8.0, r.right, 1e-9)
        assertEquals(b.top + 8.0, r.top, 1e-9)
        assertEquals(90.0, r.width, 1e-9)
        assertEquals(28.0, r.height, 1e-9)
    }

    @Test
    fun obstacleWithinFourStepsBelowIt() {
        val b = layout.boardRect
        val o = RectDp(b.right - 8.0 - 60.0, b.top + 4.0, b.right - 8.0, b.top + 4.0 + 40.0)
        val r = PlayLayout.timerRect(layout, listOf(o), 90.0, 28.0)
        assertEquals(o.bottom + 4.0, r.top, 1e-9)
        assertEquals(b.right - 8.0, r.right, 1e-9)
    }

    @Test
    fun farObstacleDoesNotMoveIt() {
        val b = layout.boardRect
        val o = RectDp(b.left, b.top + 8.0 + 28.0 + 4.0, b.left + 50.0, b.top + 90.0)
        val r = PlayLayout.timerRect(layout, listOf(o), 90.0, 28.0)
        assertEquals(b.top + 8.0, r.top, 1e-9)
    }

    @Test
    fun templateWidthDecidesTheStepNotTheLiveText() {
        val b = layout.boardRect
        // an obstacle that only a wide (template) pill reaches
        val o = RectDp(b.right - 8.0 - 200.0, b.top, b.right - 8.0 - 120.0, b.top + 40.0)
        val narrow = PlayLayout.timerRect(layout, listOf(o), 70.0, 28.0)
        val wide = PlayLayout.timerRect(layout, listOf(o), 150.0, 28.0)
        assertEquals(b.top + 8.0, narrow.top, 1e-9)
        assertEquals(o.bottom + 4.0, wide.top, 1e-9)
    }

    @Test
    fun solvedListenerAfterOnSolvedBeforeSettledAndNotOnRestore() {
        val log = mutableListOf<String>()
        val s = PlaySession(puzzle, { false }, onChanged = { log += "changed" }, onSolved = { log += "onSolved($it)" })
        s.layout = layout
        s.solvedListener = { log += "listener($it)" }
        s.restore(PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 0, null))
        assertTrue(log.none { it.startsWith("listener") || it.startsWith("onSolved") })
        val s2 = PlaySession(puzzle, { false }, onChanged = { log += "changed" }, onSolved = { log += "onSolved($it)" })
        s2.layout = layout
        s2.solvedListener = { log += "listener($it)" }
        log.clear()
        val poses = puzzle.solution.map { PieceGeometry.poseOf(it.piece, it.polygon)!! }
        assertTrue(s2.solveByAid(poses))
        assertEquals(listOf("onSolved(true)", "listener(true)", "changed"), log)
    }
}
