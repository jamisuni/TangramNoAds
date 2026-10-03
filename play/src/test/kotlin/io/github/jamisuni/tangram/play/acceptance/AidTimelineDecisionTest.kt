package io.github.jamisuni.tangram.play.acceptance

import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// ACCEPTANCE TEST (TASK-T5, independent author): decision DA-83, the aid solve runs the normal REQ-023 timeline. Written from
// design WO-005 section 2b step 4 and the frozen seam: "`true` = ... a pending solve that the next `onFrame(nowMs)` turns into
// `solved = SolvedAt(nowMs, reducedMotion())` (so the REQ-023 timeline runs from that frame)"; "`internal val solvePending`
// (true from the call until that frame)". The fade itself (600 ms delay, 800 ms) is the unchanged `SolvedTimeline`; its device
// half is in `AidTimelineDeviceTest`. No acceptance tokens: this pins an AI decision.
class AidTimelineDecisionTest {

    private val p = SEVEN_PIECE.first()

    // decision DA-83: the solve is pending right after the call and an IDLE frame (no drag at all) resolves it with that frame's
    // clock. This is the early-return trap: a frame handler that returns early when nothing is dragged would never resolve it.
    @Test
    fun decisionDA83_anIdleFrameResolvesThePendingSolveWithItsOwnClock() {
        val s = aidSession(p, AidEvents())
        assertTrue(s.solveByAid(aidPoses(p)))
        assertTrue(s.solvePending)
        assertNull(s.solved)
        assertEquals(PuzzleState.SOLVED, s.state)
        s.onFrame(1000)
        assertFalse("still pending after a frame", s.solvePending)
        assertNotNull(s.solved)
        assertEquals(1000L, s.solved!!.t0.toLong())
    }

    // decision DA-83: the start time is the NEXT frame's clock, never a stale one: an idle board whose last frame was long ago
    // still starts the timeline at the frame after the call.
    @Test
    fun decisionDA83_theStartIsTheNextFramesClockNotAStaleOne() {
        val s = aidSession(p, AidEvents())
        s.onFrame(10) // an old frame before the aid is used
        assertTrue(s.solveByAid(aidPoses(p)))
        s.onFrame(90_000)
        assertEquals(90_000L, s.solved!!.t0.toLong())
    }

    // decision DA-83: the timeline starts once; later frames do not restart it.
    @Test
    fun decisionDA83_laterFramesDoNotRestartTheTimeline() {
        val s = aidSession(p, AidEvents())
        assertTrue(s.solveByAid(aidPoses(p)))
        s.onFrame(1000)
        s.onFrame(1016)
        s.onFrame(5000)
        assertEquals(1000L, s.solved!!.t0.toLong())
        assertFalse(s.solvePending)
    }

    // decision DA-83: reduced motion is respected exactly as for a player solve: the session's own flag is recorded on the solve.
    @Test
    fun decisionDA83_reducedMotionIsTheSessionsOwn() {
        for (reduced in listOf(false, true)) {
            val s = aidSession(p, AidEvents()) { reduced }
            assertTrue(s.solveByAid(aidPoses(p)))
            s.onFrame(1000)
            assertEquals("reduced=$reduced", reduced, s.solved!!.reducedMotion)
        }
    }

    // decision DA-83: while the solve is pending the board already shows every piece at its place (the pieces are what the REQ-023
    // timeline pops and then fades away), and no solve time exists yet.
    @Test
    fun decisionDA83_thePiecesStandAtTheirPlacesWhilePending() {
        val s = aidSession(p, AidEvents())
        assertTrue(s.solveByAid(aidPoses(p)))
        for (piece in p.solution.map { it.piece }) {
            assertEquals("$piece", "Board", s.where(piece))
        }
        assertEquals(poseKeys(aidPoses(p)), poseKeys(s.placed))
        assertNull(s.solved)
    }
}
