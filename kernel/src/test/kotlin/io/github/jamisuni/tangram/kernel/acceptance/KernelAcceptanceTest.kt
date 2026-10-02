package io.github.jamisuni.tangram.kernel.acceptance

import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Silhouette
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.geometry.pointOf
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.layout.LayoutRules
import io.github.jamisuni.tangram.kernel.lock.LockSearch
import io.github.jamisuni.tangram.kernel.lock.SolvedCheck
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Turn
import io.github.jamisuni.tangram.kernel.state.PuzzleStates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Kernel additions of WO-003 (design section 8): REQ-022 A1 truth table, TYPE-006/007 rules, WO-001 F3 fail-closed. */
class KernelAcceptanceTest {

    private fun at(piece: PieceId, x: Long, y: Long) = PlacedPiece(piece, Turn(0), false, pointOf(x, y))

    // REQ-022.A1 - "An arrangement different from the stored solution that fills the silhouette counts as solved."
    // REQ-022 statement/rules: solved when the last of the puzzle's pieces locks; "No comparison with the stored solution".
    // Kernel truth table: the decision depends on which pieces are locked, never on where (locks are validated elsewhere).
    @Test
    fun req022_A1_solvedMeansEveryPieceOfThePuzzleIsLockedWhereverItLies() {
        val all = PieceId.entries
        val somewhere = all.mapIndexed { i, p -> at(p, i.toLong() * 3, 7) } // nowhere near any stored solution
        assertTrue("all seven locked, in an arbitrary arrangement", SolvedCheck.isSolved(all, somewhere))
        assertFalse("six of seven", SolvedCheck.isSolved(all, somewhere.drop(1)))
        assertFalse("none", SolvedCheck.isSolved(all, emptyList()))
        for (missing in all) {
            assertFalse("missing $missing", SolvedCheck.isSolved(all, somewhere.filter { it.piece != missing }))
        }
        // a mini puzzle (REQ-045): its own three pieces are enough
        val mini = listOf(PieceId.SQ, PieceId.ST1, PieceId.ST2)
        assertTrue(SolvedCheck.isSolved(mini, somewhere.filter { it.piece in mini }))
        assertFalse(SolvedCheck.isSolved(mini, somewhere.filter { it.piece in mini }.drop(1)))
    }

    // TYPE-007 (decisions F3): "Phone layout when the window is less than 600 dp wide; Tablet layout when it is 600 dp or wider"
    // and "< 600 dp Phone (portrait-locked)" for the device's smallest width.
    @Test
    fun type007_boundaryAt600dp() {
        fun cls(w: Number) = invoke("classFor", w)
        assertEquals(LayoutClass.PHONE, cls(599))
        assertEquals(LayoutClass.PHONE, cls(599.9))
        assertEquals(LayoutClass.TABLET, cls(600))
        assertEquals(LayoutClass.TABLET, cls(840))
        assertEquals(LayoutClass.PHONE, cls(360))
        assertEquals(true, invoke("lockPortrait", 599))
        assertEquals(false, invoke("lockPortrait", 600))
    }

    private fun invoke(name: String, n: Number): Any? {
        val m = LayoutRules.javaClass.methods.first { it.name == name && it.parameterCount == 1 }
        val arg: Any = when (m.parameterTypes[0]) {
            java.lang.Integer.TYPE -> n.toInt()
            java.lang.Long.TYPE -> n.toLong()
            java.lang.Float.TYPE -> n.toFloat()
            else -> n.toDouble()
        }
        return m.invoke(LayoutRules, arg)
    }

    // TYPE-006 - "New -> In progress when a piece first leaves the tray (a drag starts on a tray piece)"; "In progress ->
    // Solved when the last piece locks (REQ-022)"; nothing else moves in this work order.
    @Test
    fun type006_transitionsOfThisWorkOrder() {
        assertEquals(PuzzleState.IN_PROGRESS, PuzzleStates.onTrayDragStarted(PuzzleState.NEW))
        assertEquals(PuzzleState.IN_PROGRESS, PuzzleStates.onTrayDragStarted(PuzzleState.IN_PROGRESS))
        assertEquals(PuzzleState.SOLVED, PuzzleStates.onTrayDragStarted(PuzzleState.SOLVED))
        assertEquals(PuzzleState.IN_PROGRESS, PuzzleStates.onPieceLocked(PuzzleState.IN_PROGRESS, false))
        assertEquals(PuzzleState.SOLVED, PuzzleStates.onPieceLocked(PuzzleState.IN_PROGRESS, true))
        assertEquals(PuzzleState.SOLVED, PuzzleStates.onPieceLocked(PuzzleState.SOLVED, false))
        assertEquals(PuzzleState.SOLVED, PuzzleStates.onPieceLocked(PuzzleState.SOLVED, true))
    }

    // decision WO-001 F3 (code review) / DA-25 - ConvexClip fail-closed: non-finite input must never produce a lock
    // ("returns Fit(+inf, +inf) (invalid under both tests) when any coordinate ... is non-finite"). Reachable through the
    // public search only via its float inputs; the exact geometry is Long-based and cannot carry NaN.
    @Test
    fun decision_WO001_F3_nonFiniteInputNeverLocks() {
        val square = listOf(
            listOf(pointOf(0, 0), pointOf(4, 0), pointOf(2, 2)),
            listOf(pointOf(0, 0), pointOf(2, 2), pointOf(0, 4)),
            listOf(pointOf(4, 0), pointOf(4, 2), pointOf(3, 1)),
        )
        val sil = Silhouette(square)
        val ok = LockSearch.find(sil, emptyList(), PieceId.LT1, Turn(0), false, Vec2(0.0, 0.0), 0.65)
        assertTrue("control: the same call with finite input does lock", ok != null)
        val nan = Double.NaN
        val inf = Double.POSITIVE_INFINITY
        assertNull(LockSearch.find(sil, emptyList(), PieceId.LT1, Turn(0), false, Vec2(nan, 0.0), 0.65))
        assertNull(LockSearch.find(sil, emptyList(), PieceId.LT1, Turn(0), false, Vec2(0.0, nan), 0.65))
        assertNull(LockSearch.find(sil, emptyList(), PieceId.LT1, Turn(0), false, Vec2(inf, 0.0), 0.65))
        assertNull(LockSearch.find(sil, emptyList(), PieceId.LT1, Turn(0), false, Vec2(0.0, 0.0), nan))
        assertNull(LockSearch.find(sil, emptyList(), PieceId.LT1, Turn(0), false, Vec2(-inf, inf), 0.65))
    }
}
