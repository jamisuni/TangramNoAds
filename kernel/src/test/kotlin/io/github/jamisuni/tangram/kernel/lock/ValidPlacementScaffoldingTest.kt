package io.github.jamisuni.tangram.kernel.lock

import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.geometry.pointOf
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Rational
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SCAFFOLDING (TASK-022, disposable): `LockSearch.isValidPlacement`, decision DA-50 (validity without an
 * anchor), fail-closed per guardrail G-10. No acceptance IDs.
 */
class ValidPlacementScaffoldingTest {

    private val puzzle = goldenPuzzles.first { it.id == "shapes-mini-1" }
    private val silhouette = puzzle.silhouette()
    private val solved = puzzle.solution.map { it.placed() }

    @Test
    fun everySolutionPieceIsValidAgainstTheOthers() { // decision DA-50
        for (p in solved) {
            val others = solved.filter { it.piece != p.piece }
            assertTrue("$p", LockSearch.isValidPlacement(silhouette, others, p.piece, p.turn, p.mirrored, p.at))
        }
    }

    @Test
    fun ownEntryInOthersIsIgnored() { // decision DA-50
        val p = solved.first()
        assertTrue(LockSearch.isValidPlacement(silhouette, solved, p.piece, p.turn, p.mirrored, p.at))
    }

    @Test
    fun validWithoutTouchingAnyAnchor() { // decision DA-50: only the first piece placed, so a spot touching nothing current
        val p = solved.first()
        assertTrue(LockSearch.isValidPlacement(silhouette, emptyList(), p.piece, p.turn, p.mirrored, p.at))
        // the lock search from far away finds nothing near, but validity does not care about anchors
        assertTrue(LockSearch.find(silhouette, emptyList(), p.piece, p.turn, p.mirrored, Vec2(500.0, 500.0), 0.65) == null)
    }

    @Test
    fun overlapAndOutsideAreInvalid() { // decision DA-50
        val a = solved[0]
        val b = solved[1]
        // b's piece placed exactly where a lies: overlaps a
        assertFalse(LockSearch.isValidPlacement(silhouette, listOf(a), b.piece, a.turn, a.mirrored, a.at))
        // far outside the silhouette
        assertFalse(LockSearch.isValidPlacement(silhouette, emptyList(), a.piece, a.turn, a.mirrored, pointOf(100, 100)))
    }

    @Test
    fun failClosedAtTheBoundExtremes() { // guardrail G-10, decision DA-63
        val p = solved.first()
        val big = Rational.of(65536)
        val tiny = Rational.of(1, 64)
        val spots = listOf(
            ExactPoint(Q2(big, big), Q2(Rational.of(-65536), big)),
            ExactPoint(Q2(Rational.of(-65536), tiny), Q2(tiny, Rational.of(-65536))),
            ExactPoint(Q2(tiny, tiny), Q2(tiny, tiny)),
            ExactPoint(Q2(Rational.of(65535, 64), Rational.of(-65535, 64)), Q2(Rational.of(1, 2), Rational.of(1, 32))),
        )
        for (at in spots) {
            // must not throw; extreme spots are simply invalid
            assertFalse(LockSearch.isValidPlacement(silhouette, solved - p, p.piece, Turn(1), true, at))
        }
        for (step in 0..64) {
            val at = ExactPoint(Q2(Rational.of(step.toLong(), 64), Rational.ZERO), Q2(Rational.of(65536 - step.toLong(), 64), Rational.ZERO))
            LockSearch.isValidPlacement(silhouette, solved - p, p.piece, Turn(step % 8), step % 2 == 0, at)
            LockSearch.find(silhouette, solved - p, p.piece, Turn(step % 8), false, Vec2(step / 64.0, 1024.0), 0.65)
        }
    }

    @Test
    fun overflowingPlacementIsFalseNotAThrow() { // guardrail G-10
        val p = solved.first()
        val max = Rational.of(Long.MAX_VALUE)
        val at = ExactPoint(Q2(max, Rational.ZERO), Q2(max, Rational.ZERO))
        // The raw geometry must overflow here (a corner adds a positive offset to Long.MAX_VALUE) ...
        try {
            PieceGeometry.corners(p.piece, Turn(0), false, at)
            org.junit.Assert.fail("expected ArithmeticException from the raw geometry")
        } catch (expected: ArithmeticException) {
            // proven: the exact corner computation throws
        }
        // ... and isValidPlacement turns that throw into false; it fails if the catch is removed.
        assertFalse(LockSearch.isValidPlacement(silhouette, emptyList(), p.piece, Turn(0), false, at))
        // the coprime 63 / 61 values at the magnitude limit (DA-63 proof) overflow in the clip stage or are invalid
        val c = ExactPoint(Q2(Rational.of(65536, 63), Rational.of(-65535, 61)), Q2(Rational.of(-65535, 61), Rational.of(65536, 63)))
        assertFalse(LockSearch.isValidPlacement(silhouette, solved - p, p.piece, Turn(1), true, c))
    }
}
