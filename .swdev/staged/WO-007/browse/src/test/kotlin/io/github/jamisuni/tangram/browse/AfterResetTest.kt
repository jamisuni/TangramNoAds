package io.github.jamisuni.tangram.browse

import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * decision DA-120 (design WO-007 section 5 and seam row B-1): `BrowseController.afterReset()` is `showAt(indexState)`. After a confirmed
 * reset the CURRENT puzzle stays shown, now New: a fresh session of the same puzzle restored from the (erased) store, no `lastShown`
 * write, no index change, the grid not involved; and it NEVER persists, so the erased board cannot come back (`persist()` captures
 * the host's session, which `afterReset` has already replaced). No acceptance token: this is the engine-level half of REQ-034's
 * "every puzzle shows as New" and of REQ-032's "return to the same puzzle", read by the AI decision DA-120 / F25; the on-screen proof is the
 * held test.
 *
 * Adapters: `BrowseKit.Rig`, `FakeHost`, `FakeProgressStore` (the WO-004 test kit). The fake store's `resetAllProgress()` clears the
 * puzzles and the play time and keeps `lastShown`, as the `IProgressStore` KDoc says; the test calls it itself, as the settings
 * controller does just before `onReset()`.
 */
class AfterResetTest {

    private val puzzles = BrowseKit.puzzles
    private val sevenPiece = BrowseKit.indexOfFirstSevenPiecePuzzle()

    private fun threePieces() = BrowseKit.puzzles[sevenPiece].let { p ->
        p.solution.map { it.piece }.take(3).associateWith { BrowseKit.solutionSave(p, it) }
    }

    /** A rig showing puzzle [index] with a live in-progress board, a SOLVED puzzle 0 with a best time, everything stored as the real app would. */
    private fun playingRig(index: Int = sevenPiece): BrowseKit.Rig {
        assertTrue("fixture: the shown puzzle must not be the first one, or 'stay' and 'first' cannot be told apart", index > 0)
        val rig = BrowseKit.Rig {
            seed(BrowseKit.id(0), BrowseKit.progress(PuzzleState.SOLVED, BrowseKit.fullSolution(puzzles[0]), 47, 41))
        }
        rig.controller.open(index)
        rig.host.play(threePieces())
        rig.controller.persist() // onChanged keeps the store current while playing
        assertEquals(PuzzleState.IN_PROGRESS, rig.store.progress(puzzles[index].id).state)
        return rig
    }

    // decision DA-120: the same puzzle stays shown, now New, with every piece back in the tray (a fresh session restored from the store).
    @Test
    fun decisionDA120_afterResetTheCurrentPuzzleStaysShownAndIsNew() {
        val rig = playingRig()
        val shownBefore = rig.host.shown.size
        rig.store.resetAllProgress()
        rig.controller.afterReset()

        assertTrue("a new session was shown", rig.host.shown.size > shownBefore)
        assertEquals("the same puzzle", puzzles[sevenPiece].id, rig.host.lastShown.puzzle.id)
        assertEquals("restored from the erased store: New, nothing placed, no times", PuzzleProgress.NEW, rig.host.lastShown.progress)
        assertEquals("the index did not move (not the first puzzle)", sevenPiece, rig.controller.index)
        assertEquals(PuzzleState.NEW, rig.controller.shownState)
        assertEquals(PuzzleState.NEW, rig.host.state)
        assertTrue(rig.host.pieces.isEmpty())
    }

    // decision DA-120 + REQ-034 (the reset reaches the whole collection): another puzzle that was solved reads New too.
    @Test
    fun decisionDA120_everyOtherPuzzleReadsNewAfterTheReset() {
        val rig = playingRig()
        assertEquals("fixture: puzzle 0 starts solved", PuzzleState.SOLVED, rig.controller.stateOf(0))
        rig.store.resetAllProgress()
        rig.controller.afterReset()
        for (i in puzzles.indices) assertEquals("puzzle $i", PuzzleState.NEW, rig.controller.stateOf(i))
    }

    // decision DA-120: a SOLVED puzzle on screen comes back New with its best time gone (the best time is erased by a reset, REQ-034).
    @Test
    fun decisionDA120_aSolvedShownPuzzleComesBackNewWithoutItsBestTime() {
        val id = BrowseKit.id(2)
        val rig = BrowseKit.Rig {
            seedLastShown(id)
            seed(id, BrowseKit.progress(PuzzleState.SOLVED, BrowseKit.fullSolution(puzzles[2]), 52, 44))
        }
        assertEquals("fixture: shown solved", PuzzleState.SOLVED, rig.host.lastShown.progress.state)
        assertEquals(2, rig.controller.index)
        rig.store.resetAllProgress()
        rig.controller.afterReset()
        assertEquals(2, rig.controller.index)
        assertEquals(PuzzleProgress.NEW, rig.host.lastShown.progress)
        assertEquals(puzzles[2].id, rig.host.lastShown.puzzle.id)
    }

    // decision DA-120: no `lastShown` write, no write of any kind, and the live session is not captured (never calls persist()).
    @Test
    fun decisionDA120_afterResetWritesNothingAndNeverCapturesTheLiveBoard() {
        val rig = playingRig()
        val lastShownBefore = rig.store.lastShownPuzzle()
        rig.store.resetAllProgress()
        rig.store.clearWriteLog()
        val capturesBefore = rig.host.captures
        rig.controller.afterReset()

        assertFalse("afterReset wrote to the store: progress=${rig.store.progressWrites} lastShown=${rig.store.lastShownWrites}", rig.store.anyWrites)
        assertEquals("the last shown puzzle is unchanged", lastShownBefore, rig.store.lastShownPuzzle())
        assertEquals("the live board was captured: that is persist()", capturesBefore, rig.host.captures)
    }

    // decision DA-120 (section 5 invariant, review N3b): after afterReset, `persist()` as onPause calls it saves the NEW session, so every
    // puzzle in the store is still New.
    @Test
    fun decisionDA120_aPersistAfterTheResetCannotBringTheErasedBoardBack() {
        val rig = playingRig()
        rig.store.resetAllProgress()
        rig.controller.afterReset()
        rig.controller.persist()
        for (i in puzzles.indices) {
            assertEquals("puzzle $i read through the controller", PuzzleState.NEW, rig.controller.stateOf(i))
            assertEquals("puzzle $i read from the store", PuzzleProgress.NEW, rig.store.progress(puzzles[i].id))
        }
        assertEquals("what persist() wrote for the shown puzzle is New", PuzzleState.NEW, rig.store.progress(puzzles[sevenPiece].id).state)
    }

    // decision DA-120: the reset also leaves navigation working from where the player is: next goes to the following puzzle.
    @Test
    fun decisionDA120_afterTheResetNavigationContinuesFromTheSamePuzzle() {
        val rig = playingRig()
        rig.store.resetAllProgress()
        rig.controller.afterReset()
        rig.controller.next()
        assertEquals(sevenPiece + 1, rig.controller.index)
        rig.controller.previous()
        assertEquals(sevenPiece, rig.controller.index)
        assertEquals(PuzzleProgress.NEW, rig.host.lastShown.progress)
    }
}
