package io.github.jamisuni.tangram.time

import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.time.GateRig.Companion.A
import io.github.jamisuni.tangram.time.GateRig.Companion.inProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// WO-008 T8c (v-t): the covering test of REQ-030 A2 under the code home `time` (design, Acceptance IDs table).
// REQ-030 A2: "Solving again slower keeps the earlier, faster best time." Design 3: `bestSeconds = min(base.bestSeconds ?: MAX, pending own
// solve)`; equal keeps; `0` is a legal value and differs from `null`; Retry restarts the record and keeps the best.
class BestTimeTest {

    /** One attempt: Retry (when [retry]), play [seconds] active seconds, solve by the player, merge and persist. Returns the merged record. */
    private fun attempt(r: GateRig, seconds: Int, retry: Boolean) = run {
        if (retry) {
            r.store.saveProgress(A, r.store.progress(A).restarted()) // Retry: New, 0 s, best kept
            r.show(A)
        }
        r.keeper.setPuzzleRunning(true) // the first drag moved New -> In progress
        r.tap()
        r.tickFor(seconds)
        r.keeper.solved(false)
        val merged = r.keeper.mergeInto(A, r.store.progress(A).copy(state = PuzzleState.SOLVED, pieces = inProgress(0, null).pieces))
        r.store.saveProgress(A, merged)
        r.keeper.setPuzzleRunning(false)
        merged
    }

    private fun fresh(): GateRig = GateRig().also {
        it.store.seedProgress(A, inProgress(0, null))
        it.start()
        it.show(A)
    }

    // REQ-030.A2
    @Test fun solvingAgainSlowerKeepsTheEarlierFasterBest() {
        val r = fresh()
        assertEquals(30L, attempt(r, 30, retry = false).bestSeconds)
        val slower = attempt(r, 50, retry = true)
        assertEquals("the faster best is kept", 30L, slower.bestSeconds)
        assertEquals("the stored best is still 30", 30L, r.store.progress(A).bestSeconds)
        assertEquals("the record holds the new attempt's time", 50L, slower.puzzleSeconds)
    }

    // REQ-030.A2: and a faster solve does set a new best
    @Test fun aFasterSolveSetsANewBest() {
        val r = fresh()
        attempt(r, 30, retry = false)
        attempt(r, 50, retry = true)
        val faster = attempt(r, 20, retry = true)
        assertEquals(20L, faster.bestSeconds)
        assertEquals(20L, r.store.progress(A).bestSeconds)
    }

    // REQ-030.A2: an equal time keeps the best (same value either way, never lost)
    @Test fun anEqualSolveKeepsTheBest() {
        val r = fresh()
        attempt(r, 30, retry = false)
        assertEquals(30L, attempt(r, 30, retry = true).bestSeconds)
    }

    // REQ-030.A2: Retry resets the running time and not the best (REQ-030 Rules)
    @Test fun retryKeepsTheBestAndStartsTheNextAttemptFromZero() {
        val r = fresh()
        attempt(r, 30, retry = false)
        r.store.saveProgress(A, r.store.progress(A).restarted())
        r.show(A)
        assertEquals("the running time restarts", 0L, r.keeper.puzzleSeconds)
        assertEquals("the best survives the Retry", 30L, r.store.progress(A).bestSeconds)
    }

    // REQ-030.A2: a zero-second solve is a best of 0, which is a value and not "none" (value shapes)
    @Test fun aZeroSecondBestIsAValueAndALaterSlowerSolveKeepsIt() {
        val r = fresh()
        val instant = attempt(r, 0, retry = false)
        assertEquals(0L, instant.bestSeconds)
        assertEquals("0 s is not null", 0L, r.store.progress(A).bestSeconds)
        assertEquals(0L, attempt(r, 30, retry = true).bestSeconds)
    }

    // REQ-030.A2 (the other side of "best"): a puzzle never solved by the player has no best
    @Test fun aPuzzleNeverSolvedHasNoBest() {
        val r = fresh()
        r.keeper.setPuzzleRunning(true)
        r.tap()
        r.tickFor(30)
        val merged = r.keeper.mergeInto(A, r.store.progress(A))
        assertNull(merged.bestSeconds)
    }
}
