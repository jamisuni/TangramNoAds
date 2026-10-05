package io.github.jamisuni.tangram.time

import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.time.GateRig.Companion.A
import io.github.jamisuni.tangram.time.GateRig.Companion.B
import io.github.jamisuni.tangram.time.GateRig.Companion.inProgress
import org.junit.Assert.assertEquals
import org.junit.Test

// WO-008 T8c (v-t): the covering test of REQ-030 A1 under the code home `time` (design, Acceptance IDs table).
// REQ-030 A1: "Browsing away for 2 minutes adds nothing to the puzzle's time." Rules: "The time pauses while another puzzle or the settings
// screen is shown, and resumes on return." Design 2.2: with `setPuzzleRunning(false)` (settings or the grid on top) the puzzle's seconds do
// not move while today and total do; browsing to another puzzle shows that puzzle's own seconds, adopted from the store.
class BrowseAwayTest {

    private fun touchedFor(r: GateRig, seconds: Int) { // touched time: a tap every 30 s keeps the 60 s window open
        var left = seconds
        while (left > 0) {
            r.tap()
            val step = minOf(30, left)
            r.tickFor(step)
            left -= step
        }
    }

    // REQ-030.A1
    @Test fun twoMinutesWithTheSettingsOrTheGridOnTopAddNothingToThePuzzle() {
        val r = GateRig()
        r.store.seedProgress(A, inProgress(0, null))
        r.start()
        r.show(A)
        r.keeper.setPuzzleRunning(true)
        touchedFor(r, 20)
        assertEquals(20L, r.keeper.puzzleSeconds)
        val totalBefore = r.keeper.totalSeconds
        r.keeper.setPuzzleRunning(false) // settings opens (or the grid)
        touchedFor(r, 120)
        assertEquals("the puzzle's time did not move", 20L, r.keeper.puzzleSeconds)
        assertEquals("today and total did", totalBefore + 120, r.keeper.totalSeconds)
        assertEquals(totalBefore + 120, r.keeper.todaySeconds)
        r.keeper.setPuzzleRunning(true) // and closes: it resumes on return
        touchedFor(r, 5)
        assertEquals(25L, r.keeper.puzzleSeconds)
    }

    // REQ-030.A1
    @Test fun twoMinutesOnAnotherPuzzleLeaveThisPuzzlesTimeAsItWas() {
        val r = GateRig()
        r.store.seedProgress(A, inProgress(10, null))
        r.store.seedProgress(B, inProgress(7, 90))
        r.start()
        r.show(A)
        r.keeper.setPuzzleRunning(true)
        touchedFor(r, 20)
        // browse away: capture A (merge, persist), show B
        r.store.saveProgress(A, r.keeper.mergeInto(A, r.store.progress(A)))
        assertEquals(30L, r.store.progress(A).puzzleSeconds)
        r.show(B)
        touchedFor(r, 120) // two minutes on B: they are B's own time, never A's
        r.store.saveProgress(B, r.keeper.mergeInto(B, r.store.progress(B)))
        assertEquals("B counted its own 120 s", 127L, r.store.progress(B).puzzleSeconds)
        // back to A: A's time is exactly what it was when it was left
        r.show(A)
        assertEquals("A's time did not move while it was away", 30L, r.keeper.puzzleSeconds)
        assertEquals(30L, r.store.progress(A).puzzleSeconds)
        touchedFor(r, 5)
        r.store.saveProgress(A, r.keeper.mergeInto(A, r.store.progress(A)))
        assertEquals("and it resumes on return", 35L, r.store.progress(A).puzzleSeconds)
    }

    // REQ-030.A1: browsing to a New puzzle and back costs the first puzzle nothing and the New one nothing
    @Test fun twoMinutesOnANewPuzzleCostBothPuzzlesNothing() {
        val r = GateRig()
        r.store.seedProgress(A, inProgress(30, null))
        r.start()
        r.show(A)
        r.keeper.setPuzzleRunning(true)
        r.tap()
        r.store.saveProgress(A, r.keeper.mergeInto(A, r.store.progress(A)))
        r.show(B) // New: the gate turns the puzzle clock off
        r.keeper.setPuzzleRunning(false)
        touchedFor(r, 120)
        assertEquals(0L, r.keeper.puzzleSeconds)
        r.store.saveProgress(B, r.keeper.mergeInto(B, PuzzleProgress.NEW))
        assertEquals(PuzzleProgress.NEW, r.store.progress(B))
        r.show(A)
        assertEquals(30L, r.keeper.puzzleSeconds)
    }
}
