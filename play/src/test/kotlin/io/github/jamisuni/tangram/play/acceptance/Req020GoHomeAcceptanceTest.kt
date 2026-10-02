package io.github.jamisuni.tangram.play.acceptance

import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.play.DropOutcome
import io.github.jamisuni.tangram.play.DropResolver
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WO-001 acceptance tests of REQ-020 "Missed drop returns to the tray": A1.
 * (A2 "no error message is shown" is a screen matter; carried to WO-003 by the design. The engine's part of it is
 * that a go-home outcome carries a piece and its turn and mirror - nothing else to show.)
 */
class Req020GoHomeAcceptanceTest {

    private val square = DropResolver(Fixtures.SQUARE)

    // REQ-020.A1 — "After any drop, the piece is either locked on the board or back in its tray cell."
    @Test
    fun a1_everyDropEndsLockedOrHomeAndNeverInBetween() {
        // A sweep of drops of every piece not on the board - all turns, both mirrors of the parallelogram, on and off
        // the board, near and far from anchors. REQ-020 Rules: "There is no loose (unlocked) piece on the board at
        // any time" - so every outcome is a lock (a piece on the board with the turn and mirror it was dropped with:
        // REQ-019 Rules) or a go-home (the same piece, with the turn and mirror it had: decisions.md P1 F2).
        val board = listOf(SquarePlaced.LT1, SquarePlaced.LT2)
        val onBoard = board.map { it.piece }.toSet()
        var locks = 0
        var homes = 0
        for (piece in PieceId.values().filter { it !in onBoard }) {
            val mirrors = if (piece == PieceId.PG) listOf(false, true) else listOf(false)
            for (turn in 0..7) {
                for (mirrored in mirrors) {
                    for (i in 0..6) {
                        for (j in 0..6) {
                            for (overBoard in listOf(true, false)) {
                                val pose = drag(piece, turn, mirrored, -0.3 + 0.78 * i, -0.3 + 0.78 * j, overBoard)
                                when (val outcome = square.release(pose, board, DP_PER_UNIT_R_0_65)) {
                                    is DropOutcome.Locked -> {
                                        locks++
                                        assertTrue("a drop off the board locked: $pose -> $outcome", overBoard)
                                        assertTrue("$pose -> $outcome", outcome.placed.piece == piece)
                                        assertTrue("turn changed: $pose -> $outcome", outcome.placed.turn == pose.turn)
                                        assertTrue("mirror changed: $pose -> $outcome", outcome.placed.mirrored == mirrored)
                                    }
                                    is DropOutcome.Home -> {
                                        homes++
                                        assertHome(outcome, pose)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // Non-vacuity: the sweep contains both kinds of ending.
        assertTrue("the sweep produced only $locks locks", locks >= 20)
        assertTrue("the sweep produced only $homes go-homes", homes >= 20)
    }

    // REQ-020.A1 / Statement — "...or is dropped outside the board or on the tray, THEN the game SHALL return it to its tray cell."
    @Test
    fun a1_aDropOutsideTheBoardOrOnTheTrayGoesHomeEvenWithAnAnchorInReach() {
        // The same release over the board locks (control) and off the board (on the tray or outside it) goes home.
        // Large triangle 1, turn 0, 0.36 from its lock position (0,0).
        val overBoard = drag(PieceId.LT1, 0, false, 0.3, 0.2, overBoard = true)
        assertLockedExactly(square.release(overBoard, emptyList(), DP_PER_UNIT_R_0_65), overBoard, pt(0, 0), "over the board")

        val offBoard = drag(PieceId.LT1, 0, false, 0.3, 0.2, overBoard = false)
        assertHome(square.release(offBoard, emptyList(), DP_PER_UNIT_R_0_65), offBoard, "same release, off the board")

        // And with a neighbour's corner as the only anchor in reach.
        val tied = drag(PieceId.ST2, 4, false, 3.3, 3.2, overBoard = false)
        assertHome(square.release(tied, listOf(SquarePlaced.SQ), DP_PER_UNIT_R_0_65), tied, "off the board next to a piece")
    }

    // REQ-020.A1 / Statement — "IF a dropped piece has no valid position by REQ-019 ... THEN the game SHALL return it to its tray cell."
    @Test
    fun a1_aMissedDropOnTheBoardGoesHomeKeepingItsTurnAndMirror() {
        // Parallelogram, mirrored, turn 3 (an odd turn), released in the middle of the empty square: nothing to tie
        // to, so it goes home with the turn and mirror it had (decisions.md P1 F2: "A piece that goes back to the
        // tray keeps the turn and mirror it had").
        val pose = drag(PieceId.PG, 3, true, 2.0, 2.0)
        val home = assertHome(square.release(pose, emptyList(), DP_PER_UNIT_R_0_65), pose, "missed drop")
        assertTrue(home.turn.steps == 3 && home.mirrored)

        // The same for every turn of the parallelogram and both mirrors: nothing is reset by going home. Released at
        // the centre of the empty square, every position that would put a corner of the parallelogram on an outline
        // corner is at least 0.82 away, in each of the 16 poses (computed for this fixture): out of reach, so none locks.
        for (turn in 0..7) {
            for (mirrored in listOf(false, true)) {
                val p = drag(PieceId.PG, turn, mirrored, 2.0, 2.0)
                assertHome(square.release(p, emptyList(), DP_PER_UNIT_R_0_65), p, "turn $turn mirrored $mirrored")
            }
        }
    }

    // REQ-020.A1 — repeated drops of the same piece: the engine keeps nothing between drops, so a missed drop
    // leaves nothing loose and the next drop is judged on its own.
    @Test
    fun a1_aMissedDropLeavesNothingBehindForTheNextDrop() {
        val miss = drag(PieceId.LT1, 0, false, 1.0, 1.0)
        assertHome(square.release(miss, emptyList(), DP_PER_UNIT_R_0_65), miss, "first drop: missed")
        val hit = drag(PieceId.LT1, 0, false, 0.3, 0.2)
        assertLockedExactly(square.release(hit, emptyList(), DP_PER_UNIT_R_0_65), hit, pt(0, 0), "next drop")
        // And once more the miss: same answer as the first time.
        assertHome(square.release(miss, emptyList(), DP_PER_UNIT_R_0_65), miss, "third drop: missed again")
    }
}
