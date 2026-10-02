package io.github.jamisuni.tangram.play.acceptance

import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.minus
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Q2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** REQ-002 A1 and REQ-022 A1 at engine level, through the real gesture machine (design "Test seams"). */
class SolvingAcceptanceTest {

    // REQ-002.A1 - "Every puzzle can be completed with pieces of TYPE-001 by drag, turn and drop only."
    // Every one of the shipped puzzles is solved with scripted touches only: tap (turn), badge (mirror), drag, drop.
    @Test
    fun req002_A1_everyPuzzleIsCompletedByDragTurnAndDrop() {
        assertTrue("the library ships the 13 puzzles of Tangrams/", PUZZLES.size >= 13)
        val failures = ArrayList<String>()
        for (p in PUZZLES) {
            try {
                val d = Driver(p)
                assertEquals(PuzzleState.NEW, d.session.state) // TYPE-006: nothing has left the tray
                val order = buildOrder(p, targetsOf(p), d.layout.dpPerUnit)
                for ((i, tg) in order.withIndex()) {
                    d.place(tg)
                    check(d.session.placed.any { it.piece == tg.piece && it.at == tg.at }) { "${tg.piece} not locked" }
                    if (i < order.size - 1) {
                        // REQ-022 rule: solved only when the LAST piece locks.
                        check(d.session.state == PuzzleState.IN_PROGRESS) { "state ${d.session.state} after piece ${i + 1}" }
                    }
                }
                check(d.session.state == PuzzleState.SOLVED) { "state ${d.session.state} after the last piece" }
            } catch (e: Throwable) {
                failures.add("${p.id.value}: $e")
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    // REQ-022.A1 - "An arrangement different from the stored solution that fills the silhouette counts as solved."
    // shapes-square (4x4 square) mirrored left-right is a different arrangement of the same silhouette; the
    // parallelogram has to be flipped to fit it, so "different from the stored solution" is real, not a relabel.
    @Test
    fun req022_A1_anArrangementOtherThanTheStoredSolutionSolvesThePuzzle() {
        val p = puzzle("shapes-square")
        val reflected = p.solution.map { s ->
            s.piece to s.polygon.map { pt -> ExactPoint(Q2.of(4) - pt.x, pt.y) }
        }
        val targets = reflected.map { (piece, poly) -> PieceGeometry.poseOf(piece, poly)!! }
        // The fixture must differ from the stored solution (distinguishing fixture): at least 2 pieces sit elsewhere.
        val moved = reflected.count { (piece, poly) ->
            poly.toSet() != p.solution.first { it.piece == piece }.polygon.toSet()
        }
        assertTrue("the reflected arrangement must differ from the stored one for several pieces, differs for $moved", moved >= 2)
        assertTrue("the parallelogram is mirrored in the other arrangement", targets.first { it.piece.name == "PG" }.mirrored)

        val d = Driver(p)
        d.placeAll(targets)
        assertEquals(PuzzleState.SOLVED, d.session.state)
        assertNotNull(d.session.solved)
        // No comparison with the stored solution was made: the placed arrangement is not the stored one.
        val placedPolys = d.session.placed.associate { it.piece to it.corners.toSet() }
        val differs = p.solution.any { placedPolys[it.piece] != it.polygon.toSet() }
        assertTrue(differs)
        assertNotEquals(0, d.session.placed.size)
    }
}
