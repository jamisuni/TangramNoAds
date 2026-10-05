package io.github.jamisuni.tangram.time

import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.time.GateRig.Companion.A
import io.github.jamisuni.tangram.time.GateRig.Companion.inProgress
import org.junit.Assert.assertEquals
import org.junit.Test

// WO-008 T8c (v-t): the covering test of REQ-005 A1 under the code home `time` (design, Acceptance IDs table).
// REQ-005 A1: "After solving a puzzle, its solve time is available." Design 3: the solve time is fixed at the solve event and is available
// as `puzzleSeconds` of the Solved record, written by the same persist that writes the state (DA-136); `solved(false)` then `mergeInto`
// yields a Solved record whose `puzzleSeconds` is the counted solve time and `bestSeconds` equals it on a first solve.
class SolveTimeTest {

    // REQ-005.A1
    @Test fun afterASolveTheSolveTimeIsAvailableAsTheSolvedRecordsSeconds() {
        val r = GateRig()
        r.store.seedProgress(A, inProgress(0, null))
        r.start()
        r.show(A)
        r.keeper.setPuzzleRunning(true)
        r.tap()
        r.tickFor(30) // 30 active seconds of play
        r.keeper.solved(false)
        val solved = r.keeper.mergeInto(A, r.store.progress(A).copy(state = PuzzleState.SOLVED))
        assertEquals(PuzzleState.SOLVED, solved.state)
        assertEquals("the solve time is the counted 30 s", 30L, solved.puzzleSeconds)
        assertEquals("a first solve is its own best", 30L, solved.bestSeconds)
        r.store.saveProgress(A, solved)

        // it is AVAILABLE afterwards, from the store, to a keeper that never saw the play (a relaunch)
        r.keeper.setPuzzleRunning(false)
        val relaunched = PlayTimeKeeper(r.source, r.store)
        relaunched.puzzleShown(A, r.store.progress(A))
        assertEquals("the stored solve time is read back", 30L, relaunched.puzzleSeconds)
        assertEquals(30L, r.store.progress(A).puzzleSeconds)
    }

    // REQ-005.A1
    @Test fun theSolveTimeIsFixedAtTheSolveNotAtTheLeave() {
        val r = GateRig()
        r.store.seedProgress(A, inProgress(0, null))
        r.start()
        r.show(A)
        r.keeper.setPuzzleRunning(true)
        r.tap()
        r.tickFor(30)
        r.keeper.solved(false)
        r.tickFor(20) // looking at the solved picture for 20 more active seconds
        val merged = r.keeper.mergeInto(A, r.store.progress(A).copy(state = PuzzleState.SOLVED))
        assertEquals("the 20 s after the solve are not solve time", 30L, merged.puzzleSeconds)
        assertEquals(30L, merged.bestSeconds)
    }

    // REQ-005.A1
    @Test fun onlyPuzzleTimeCountsNotTheTimeTheClockWasOff() {
        val r = GateRig()
        r.store.seedProgress(A, inProgress(0, null))
        r.start()
        r.show(A)
        r.keeper.setPuzzleRunning(true)
        r.tap()
        r.tickFor(10)
        r.keeper.setPuzzleRunning(false) // the settings screen opens
        r.tap()
        r.tickFor(40)
        r.keeper.setPuzzleRunning(true) // and closes
        r.tap()
        r.tickFor(5)
        r.keeper.solved(false)
        val merged = r.keeper.mergeInto(A, r.store.progress(A).copy(state = PuzzleState.SOLVED))
        assertEquals("10 + 5 puzzle seconds; the 40 s with settings open are not in it", 15L, merged.puzzleSeconds)
        assertEquals(15L, merged.bestSeconds)
    }

    // REQ-005.A1
    @Test fun aSlowerSolveAfterRetryHasItsOwnSolveTimeAvailable() {
        val r = GateRig()
        r.store.seedProgress(A, inProgress(0, null))
        r.start()
        r.show(A)
        r.keeper.setPuzzleRunning(true)
        r.tap()
        r.tickFor(30)
        r.keeper.solved(false)
        r.store.saveProgress(A, r.keeper.mergeInto(A, r.store.progress(A).copy(state = PuzzleState.SOLVED)))
        r.keeper.setPuzzleRunning(false)

        // Retry: the record is restarted (best kept), the puzzle is shown again, the player plays slower
        r.store.saveProgress(A, r.store.progress(A).restarted())
        r.show(A)
        r.keeper.setPuzzleRunning(true)
        r.tap()
        r.tickFor(50)
        r.keeper.solved(false)
        val second = r.keeper.mergeInto(A, r.store.progress(A).copy(state = PuzzleState.SOLVED))
        assertEquals("the new solve's time is available as the record's seconds", 50L, second.puzzleSeconds)
        assertEquals("the faster best is kept", 30L, second.bestSeconds)
    }
}
