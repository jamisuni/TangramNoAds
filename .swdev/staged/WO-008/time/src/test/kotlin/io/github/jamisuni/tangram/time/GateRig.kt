package io.github.jamisuni.tangram.time

import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

// GATE HELPER (WO-008 T8c part ii, gate set of GATE-063). Test scaffolding, no requirement token.
// One keeper wired to a manual clock and a recording store, plus the few moves the gate tests repeat. The moves are named after what the
// app does, so a test reads as the sequence it simulates (design 5.1, 5.3):
//   tap()       a finger goes down and up: the root pointer observer calls touch(true) then touch(false)
//   pass(ms)    time passes with NO keeper call: the open interval stays uncredited, so a flush is "due" at the next input
//   tickFor(s)  the 1 Hz ticker: one second, accrue(), repeated
// The gate tests simulate the keeper's callers; the real wiring (BrowseController, SettingsController, SessionHost) is pinned in
// `app`'s TimeStoreRoundTripTest (rev 2, E1), not here.
class GateRig {
    val source = GateManualTimeSource()
    val store = GateRecordingProgressStore()
    val keeper = PlayTimeKeeper(source, store)

    fun start() {
        keeper.setVisible(true)
    }

    fun tap() {
        keeper.touch(true)
        keeper.touch(false)
    }

    fun pass(ms: Long) = source.advance(ms)

    fun tickFor(seconds: Int) {
        repeat(seconds) {
            source.advance(1_000)
            keeper.accrue()
        }
    }

    /** Shows [id] the way `SessionHost.show` does: adopt what the store holds. */
    fun show(id: PuzzleId) = keeper.puzzleShown(id, store.progress(id))

    companion object {
        val A = PuzzleId("gate-puzzle-a")
        val B = PuzzleId("gate-puzzle-b")

        /** An In-progress record with one piece still in the tray (the kind of record the board saves). */
        fun inProgress(seconds: Long, best: Long?) = PuzzleProgress(
            state = PuzzleState.IN_PROGRESS,
            pieces = mapOf(PieceId.SQ to PieceSave.InTray(Turn(0), false)),
            puzzleSeconds = seconds,
            bestSeconds = best,
        )

        fun solved(seconds: Long, best: Long?) = inProgress(seconds, best).copy(state = PuzzleState.SOLVED)
    }

    /** No write call after [mark] (every write, effective or not); [what] names the step in the failure message. */
    fun assertNoWrites(what: String, mark: Int) {
        val writes = store.writesSince(mark)
        assertTrue("$what must make no store write at all (design 5.1), but made: $writes", writes.isEmpty())
    }

    fun assertNoEffectiveWrites(what: String, mark: Int) {
        val writes = store.effectiveWritesSince(mark)
        assertTrue("$what must change nothing in the store, but changed it with: $writes", writes.isEmpty())
    }

    fun assertSecondsOf(label: String, id: PuzzleId, seconds: Long) {
        assertEquals("$label: stored puzzleSeconds of ${id.value}", seconds, store.progress(id).puzzleSeconds)
    }
}
