package io.github.jamisuni.tangram.time

import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.time.ActiveSecond
import io.github.jamisuni.tangram.time.GateRig.Companion.A
import io.github.jamisuni.tangram.time.GateRig.Companion.inProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

// WO-008 T8c (v-t): the covering test of REQ-030 A3 under the code home `time` (design, Acceptance IDs table).
// REQ-030 A3: "Turning a tray piece on a New puzzle starts no time." Design 2.2: the puzzle clock's gate is `puzzleCounts(state, settingsOpen,
// gridOpen)`, false for New; the time starts exactly when the first drag moves New to In progress; a captured New state forces 0 in the
// merge. (Engine level: a tray tap or turn leaves a puzzle New, `PuzzleStates`, existing `play` tests.)
class NewPuzzleStartsNoTimeTest {

    // REQ-030.A3
    @Test fun theGateIsFalseForANewPuzzleForEveryOverlayCombination() {
        for (settings in listOf(false, true)) for (grid in listOf(false, true)) {
            assertFalse("settings=$settings grid=$grid", ActiveSecond.puzzleCounts(PuzzleState.NEW, settings, grid))
        }
    }

    // REQ-030.A3: tapping and turning tray pieces is only touching: the New puzzle's seconds stay 0 over any touched time
    @Test fun touchedTimeOnANewPuzzleLeavesItsSecondsAtZeroWhileTodayAndTotalRun() {
        val r = GateRig()
        r.start()
        r.show(A) // New: no record
        r.keeper.setPuzzleRunning(false) // the gate says false for New
        repeat(4) { r.tap(); r.tickFor(30) } // two minutes of tapping and turning tray pieces
        assertEquals(0L, r.keeper.puzzleSeconds)
        assertEquals("play time did run", 120L, r.keeper.totalSeconds)
        val merged = r.keeper.mergeInto(A, PuzzleProgress.NEW)
        assertEquals(PuzzleProgress.NEW, merged)
    }

    // REQ-030.A3: then the first drag starts it, and only then
    @Test fun theFirstDragStartsTheTimeAndOnlyThen() {
        val r = GateRig()
        r.start()
        r.show(A)
        r.keeper.setPuzzleRunning(false)
        r.tap()
        r.tickFor(30)
        assertEquals(0L, r.keeper.puzzleSeconds)
        r.keeper.setPuzzleRunning(true) // a drag started on a tray piece: New -> In progress
        r.tap()
        r.tickFor(5)
        assertEquals(5L, r.keeper.puzzleSeconds)
    }

    // REQ-030.A3: a captured New state is stored with 0 seconds even if the gate lagged a frame behind (value of the merge for New)
    @Test fun aCapturedNewStateIsMergedWithZeroSeconds() {
        val r = GateRig()
        r.store.seedProgress(A, inProgress(0, 41))
        r.start()
        r.show(A)
        r.keeper.setPuzzleRunning(true) // the gate still says true for a second, then the puzzle is sent back to New by Restart
        r.tap()
        r.tickFor(3)
        val merged = r.keeper.mergeInto(A, PuzzleProgress.NEW.copy(bestSeconds = 41))
        assertEquals(PuzzleState.NEW, merged.state)
        assertEquals("a New record never carries seconds", 0L, merged.puzzleSeconds)
        assertEquals(41L, merged.bestSeconds)
    }
}
