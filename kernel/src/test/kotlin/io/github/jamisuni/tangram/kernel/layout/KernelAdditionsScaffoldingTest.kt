package io.github.jamisuni.tangram.kernel.layout

import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.geometry.pointOf
import io.github.jamisuni.tangram.kernel.lock.Fit
import io.github.jamisuni.tangram.kernel.lock.LockSearch
import io.github.jamisuni.tangram.kernel.lock.SolvedCheck
import io.github.jamisuni.tangram.kernel.lock.misfit
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PieceShape
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Turn
import io.github.jamisuni.tangram.kernel.state.PuzzleStates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// SCAFFOLDING: disposable implementer checks for the WO-003 kernel additions (not acceptance tests).
class KernelAdditionsScaffoldingTest {

    @Test fun layoutBoundaryAt599And600() {
        assertEquals(LayoutClass.PHONE, LayoutRules.classFor(599))
        assertEquals(LayoutClass.TABLET, LayoutRules.classFor(600))
        assertEquals(LayoutClass.PHONE, LayoutRules.classFor(599.9))
        assertTrue(LayoutRules.lockPortrait(599))
        assertFalse(LayoutRules.lockPortrait(600))
    }

    @Test fun puzzleStateTransitions() {
        assertEquals(PuzzleState.IN_PROGRESS, PuzzleStates.onTrayDragStarted(PuzzleState.NEW))
        assertEquals(PuzzleState.IN_PROGRESS, PuzzleStates.onTrayDragStarted(PuzzleState.IN_PROGRESS))
        assertEquals(PuzzleState.SOLVED, PuzzleStates.onTrayDragStarted(PuzzleState.SOLVED))
        assertEquals(PuzzleState.SOLVED, PuzzleStates.onPieceLocked(PuzzleState.IN_PROGRESS, true))
        assertEquals(PuzzleState.IN_PROGRESS, PuzzleStates.onPieceLocked(PuzzleState.IN_PROGRESS, false))
        assertEquals(PuzzleState.NEW, PuzzleStates.onPieceLocked(PuzzleState.NEW, true))
        assertEquals(PuzzleState.SOLVED, PuzzleStates.onPieceLocked(PuzzleState.SOLVED, false))
    }

    @Test fun solvedCheckTruthTable() {
        val all = PieceId.entries.toList()
        val placed = all.map { PlacedPiece(it, Turn(0), false, pointOf(0, 0)) }
        assertTrue(SolvedCheck.isSolved(all, placed))
        assertFalse(SolvedCheck.isSolved(all, placed.drop(1)))
        assertTrue(SolvedCheck.isSolved(listOf(PieceId.SQ), placed.filter { it.piece == PieceId.SQ }))
        assertFalse(SolvedCheck.isSolved(emptyList(), placed))
        assertFalse(SolvedCheck.isSolved(all, emptyList()))
    }

    @Test fun trayRulesFacts() {
        assertEquals(listOf(4, 4, 1, 1, 0, 4, 4), PieceId.entries.map { TrayRules.restingTurn(it.shape).steps })
        assertEquals(0xFFE8505B.toInt(), TrayRules.colour(PieceId.LT1))
        assertEquals(0xFF9B5DE5.toInt(), TrayRules.colour(PieceId.ST2))
        val table = mapOf(
            PieceShape.LARGE_TRIANGLE to 4.22, PieceShape.PARALLELOGRAM to 3.16, PieceShape.MEDIUM_TRIANGLE to 2.98,
            PieceShape.SMALL_TRIANGLE to 2.11, PieceShape.SQUARE to 2.00,
        )
        for ((shape, d) in table) assertEquals(d, TrayRules.turnDiameter(shape), 0.005)
    }

    @Test fun turnKeepsCentre() {
        for (piece in PieceId.entries) for (mirrored in listOf(false, true)) {
            val start = PlacedPiece(piece, Turn(0), mirrored, pointOf(3, 2))
            val c0 = PieceGeometry.centroidOffset(piece.shape, Turn(0), mirrored)
            for (steps in 0..7) {
                val o = PieceGeometry.originKeepingCentre(start, Turn(steps), mirrored)
                val c = PieceGeometry.centroidOffset(piece.shape, Turn(steps), mirrored)
                assertEquals(3.0 + c0.x, o.x + c.x, 1e-9)
                assertEquals(2.0 + c0.y, o.y + c.y, 1e-9)
            }
        }
        // identity: same turn gives the same origin
        val p = PlacedPiece(PieceId.PG, Turn(3), true, pointOf(1, 1))
        val o = PieceGeometry.originKeepingCentre(p, Turn(3), true)
        assertEquals(1.0, o.x, 1e-9)
        assertEquals(1.0, o.y, 1e-9)
    }

    private val square = listOf(Vec2(0.0, 0.0), Vec2(2.0, 0.0), Vec2(2.0, 2.0), Vec2(0.0, 2.0))

    @Test fun misfitFiniteMatchesFlush() {
        val fit = LockSearch.misfit(square, listOf(square), listOf(square.map { Vec2(it.x + 2.0, it.y) }), 4.0)
        assertEquals(0.0, fit.insideDeficit, 1e-9)
        assertEquals(0.0, fit.maxOverlap, 1e-9)
        val overlap = LockSearch.misfit(square, listOf(square), listOf(square), 4.0)
        assertEquals(4.0, overlap.maxOverlap, 1e-9)
    }

    private fun assertInvalid(fit: Fit) {
        assertTrue(!(fit.insideDeficit <= LockSearch.TOLERANCE && fit.maxOverlap <= LockSearch.TOLERANCE))
    }

    @Test fun misfitNonFiniteIsInvalid() {
        val bad = listOf(Vec2(Double.NaN, 0.0), Vec2(2.0, 0.0), Vec2(2.0, 2.0), Vec2(0.0, 2.0))
        val inf = listOf(Vec2(Double.POSITIVE_INFINITY, 0.0), Vec2(2.0, 0.0), Vec2(2.0, 2.0), Vec2(0.0, 2.0))
        assertInvalid(LockSearch.misfit(bad, listOf(square), emptyList(), 4.0))
        assertInvalid(LockSearch.misfit(inf, listOf(square), emptyList(), 4.0))
        assertInvalid(LockSearch.misfit(square, listOf(bad), emptyList(), 4.0))
        assertInvalid(LockSearch.misfit(square, listOf(square), listOf(bad), 4.0))
        assertInvalid(LockSearch.misfit(square, listOf(square), listOf(inf), 4.0))
        assertInvalid(LockSearch.misfit(square, listOf(square), emptyList(), Double.NaN))
    }
}
