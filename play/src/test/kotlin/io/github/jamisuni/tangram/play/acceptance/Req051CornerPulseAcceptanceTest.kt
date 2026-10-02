package io.github.jamisuni.tangram.play.acceptance

import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.play.DropOutcome
import io.github.jamisuni.tangram.play.DropResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * WO-001 acceptance tests of REQ-051 "The corners pulse once in a mini puzzle": A1 and A2, at engine level.
 * (The 600 ms animation, reduced motion and the drawing are WO-003.) A missed drop's go-home outcome carries the
 * pulse (frozen design sec. 7: `Home.pulse: CornerPulse?`, `CornerPulse.corners` = the outline corners).
 *
 * "Middle" drops use half-integer releases of the turn-0 pieces: every corner then lies at least 0.707 from every
 * integer-grid anchor of these puzzles (R = 0.65), so the drop has no valid position and goes home.
 * decisions.md DA-4: the pulse is for mini puzzles only, only when the missed drop was over the board, once per
 * missed drop.
 */
class Req051CornerPulseAcceptanceTest {

    private fun missedDrop(puzzle: Puzzle, x: Double, y: Double): DropOutcome.Home {
        val pose = drag(PieceId.ST1, 0, false, x, y)
        return assertHome(DropResolver(puzzle).release(pose, emptyList(), DP_PER_UNIT_R_0_65), pose, puzzle.id.value)
    }

    private fun cornersOf(home: DropOutcome.Home): Set<ExactPoint> {
        val pulse = home.pulse
        assertNotNull("a missed drop over the board in a mini puzzle must carry a corner pulse", pulse)
        return pulse!!.corners.toSet()
    }

    // REQ-051.A1 — "In a mini puzzle, a drop in the middle of the silhouette sends the piece home and the corners pulse once."
    @Test
    fun a1_inAMiniPuzzleAMissedDropGoesHomeAndTheOutlineCornersPulse_square() {
        // shapes-mini-2: the 2 x 2 square. Outline corners: the square's four corners ((1,1) lies inside it).
        // REQ-051 Rules: "shows only the outline corners, never a piece position".
        val home = missedDrop(Fixtures.MINI_2, 0.5, 0.5)
        assertEquals(Fixtures.MINI_2_OUTLINE_CORNERS, cornersOf(home))
    }

    // REQ-051.A1
    @Test
    fun a1_inAMiniPuzzleAMissedDropGoesHomeAndTheOutlineCornersPulse_triangle() {
        // shapes-mini-1: the pyramid triangle (2,0)(0,2)(4,2). Its outline corners are the three triangle corners;
        // the solution vertices (2,2), (1,1), (3,1) lie on straight sides and are NOT outline corners.
        val home = missedDrop(Fixtures.MINI_1, 1.5, 0.5)
        assertEquals(Fixtures.MINI_1_OUTLINE_CORNERS, cornersOf(home))
    }

    // REQ-051.A1 / Rules — the pulse shows only outline corners, never a position of a piece.
    @Test
    fun a1_thePulseNeverShowsCornersOfPiecesAlreadyOnTheBoard() {
        // shapes-mini-2 with the medium triangle and small triangle 1 on the board: (1,1) is now an anchor for
        // locking, but it is a piece corner, not an outline corner, so it must not be pulsed.
        // Small triangle 2, turn 1 (one step off the hole (2,2)(0,2)(1,1)): cannot lock anywhere.
        val resolver = DropResolver(Fixtures.MINI_2)
        val pose = drag(PieceId.ST2, 1, false, 1.0, 1.0)
        val home = assertHome(
            resolver.release(pose, listOf(Mini2Placed.MT, Mini2Placed.ST1), DP_PER_UNIT_R_0_65), pose, "missed drop",
        )
        assertEquals(Fixtures.MINI_2_OUTLINE_CORNERS, cornersOf(home))
    }

    // REQ-051.A1 — "once": every missed drop pulses, and each go-home carries exactly one pulse.
    @Test
    fun a1_everyMissedDropPulsesOnceAgain() {
        // DA-4: "once per missed drop". The engine keeps nothing between drops, so a second missed drop pulses
        // again - there is no "already shown" flag - and a locked drop in between changes nothing about that.
        val resolver = DropResolver(Fixtures.MINI_2)
        val miss = drag(PieceId.ST1, 0, false, 0.5, 0.5)
        val first = assertHome(resolver.release(miss, emptyList(), DP_PER_UNIT_R_0_65), miss, "first miss")
        val second = assertHome(resolver.release(miss, emptyList(), DP_PER_UNIT_R_0_65), miss, "second miss")
        assertEquals(Fixtures.MINI_2_OUTLINE_CORNERS, cornersOf(first))
        assertEquals(Fixtures.MINI_2_OUTLINE_CORNERS, cornersOf(second))

        val hit = drag(PieceId.MT, 0, false, 0.2, 0.1)
        assertLockedExactly(resolver.release(hit, emptyList(), DP_PER_UNIT_R_0_65), hit, pt(0, 0), "a hit in between")
        val third = assertHome(resolver.release(miss, listOf(Mini2Placed.MT), DP_PER_UNIT_R_0_65), miss, "third miss")
        assertEquals(Fixtures.MINI_2_OUTLINE_CORNERS, cornersOf(third))
    }

    // REQ-051.A1 / DA-4 — only a missed drop over the board pulses; a drop on the tray or outside goes home silently.
    @Test
    fun a1_aDropOnTheTrayOrOutsideTheBoardGoesHomeWithoutAPulse() {
        // DA-4: "only when the missed drop was over the board (a drop on the tray or outside the board goes home
        // without a pulse)". Same release as the first test, but off the board.
        val resolver = DropResolver(Fixtures.MINI_2)
        val pose = drag(PieceId.ST1, 0, false, 0.5, 0.5, overBoard = false)
        val home = assertHome(resolver.release(pose, emptyList(), DP_PER_UNIT_R_0_65), pose, "off the board")
        assertNull(home.pulse)
    }

    // REQ-051.A2 — "In a warm-up, the same drop sends the piece home and nothing else is shown."
    @Test
    fun a2_inAWarmUpTheSameDropGoesHomeWithoutAPulse() {
        // The very drop of REQ-051.A1 in a mini puzzle (small triangle 1, turn 0, released at (0.5, 0.5)) pulses there ...
        val inMini = missedDrop(Fixtures.MINI_2, 0.5, 0.5)
        assertEquals(Fixtures.MINI_2_OUTLINE_CORNERS, cornersOf(inMini))
        // ... and in a warm-up it goes home with nothing shown. REQ-051 Rules: "It happens only in mini puzzles;
        // full-set and warm-up puzzles never show anchors."
        val inWarmUp = missedDrop(Fixtures.WARMUP_1, 0.5, 0.5)
        assertNull("a warm-up never shows the corners", inWarmUp.pulse)
    }

    // REQ-051.A2 / Rules — full-set puzzles never show anchors either.
    @Test
    fun a2_inAFullSetPuzzleAMissedDropGoesHomeWithoutAPulse() {
        val home = missedDrop(Fixtures.SQUARE, 1.5, 1.5)
        assertNull("a full-set puzzle never shows the corners", home.pulse)
        // On every drop of a sweep, in warm-up and full-set puzzles: never a pulse.
        for (puzzle in listOf(Fixtures.WARMUP_1, Fixtures.SQUARE)) {
            val resolver = DropResolver(puzzle)
            for (i in 0..5) {
                for (j in 0..5) {
                    val pose = drag(PieceId.ST1, 0, false, 0.5 + i, 0.5 + j)
                    val outcome = resolver.release(pose, emptyList(), DP_PER_UNIT_R_0_65)
                    assertHome(outcome, pose, "${puzzle.id.value} at (${0.5 + i}, ${0.5 + j})")
                    assertNull("${puzzle.id.value}: no pulse outside a mini puzzle", (outcome as DropOutcome.Home).pulse)
                }
            }
        }
    }
}
