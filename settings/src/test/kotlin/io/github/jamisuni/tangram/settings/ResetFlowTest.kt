package io.github.jamisuni.tangram.settings

import io.github.jamisuni.tangram.contracts.progress.GameSettings
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PlayTime
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Module-level cover of REQ-034 A1 and A2 (design WO-007 acceptance table, "strengthened, N7a"): a recording fake store SEEDED with
 * solved and in-progress entries for several puzzles, whose `resetAllProgress()` really clears (written from the `IProgressStore`
 * KDoc: every puzzle New, play time none, settings kept). After Erase every seeded id reads New, the settings are kept (sound
 * OFF stays off, REQ-034 rule "Settings (timer, sound) are kept"), and the reset callback ran once, AFTER the store call (section
 * 5: "`confirmReset()`: ... `store.resetAllProgress()`; `onReset()`", value shape "calls resetAllProgress() once, then onReset()
 * once"). This is the engine-level reading of "every puzzle shows as New" (architecture section 5 item 4); the grid and the top bar
 * are proven on the device by the held test.
 */
class ResetFlowTest {

    private val ids = listOf("a", "b", "c", "d").map { PuzzleId(it) }

    private val solved = PuzzleProgress(PuzzleState.SOLVED, mapOf(PieceId.SQ to PieceSave.InTray(Turn(0), false)), 41, 41)
    private val inProgress = PuzzleProgress(PuzzleState.IN_PROGRESS, mapOf(PieceId.SQ to PieceSave.InTray(Turn(2), true)), 95, 80)

    private val keptSettings = GameSettings(timerShown = true, soundOn = false)

    private class Rig(val store: RecordingStore, val calls: MutableList<String>) {
        var resets = 0
        val controller = SettingsController(store, { true }) {
            resets++
            calls += "onReset"
        }
    }

    private fun rig(): Rig {
        val calls = ArrayList<String>()
        val store = RecordingStore(calls)
            .seed(ids[0], solved)
            .seed(ids[1], inProgress)
            .seed(ids[2], solved.copy(bestSeconds = 33))
            .seed(ids[3], inProgress.copy(puzzleSeconds = 7))
            .seedPlayTime(RecordingStore.PLAY_TIME)
            .seedSettings(keptSettings)
            .seedLastShown(ids[1])
        return Rig(store, calls)
    }

    private fun assertSeededFixtureIsNotNew(r: Rig) {
        // the fixture must distinguish the states it claims to test: nothing seeded may already read New
        for (id in ids) assertNotEquals("fixture: $id must start non-New", PuzzleProgress.NEW, r.store.progress(id))
        assertNotEquals(PlayTime.NONE, r.store.playTime())
        assertFalse("fixture: sound starts OFF so that 'kept' differs from the default (on)", r.store.settings().soundOn)
    }

    // REQ-034.A1 - "After a confirmed reset, every puzzle shows as New."
    // Every seeded puzzle (solved with best times, in progress with pieces and times) reads exactly the New progress; the play time is
    // erased too (REQ-034 Statement: "erase all puzzle, best and play times").
    @Test
    fun req034_A1_afterAConfirmedResetEveryPuzzleReadsNew() {
        val r = rig()
        assertSeededFixtureIsNotNew(r)
        r.controller.open()
        r.controller.requestReset()
        r.controller.confirmReset()

        for (id in ids) assertEquals("$id must read New", PuzzleProgress.NEW, r.store.progress(id))
        assertEquals("play time erased", PlayTime.NONE, r.store.playTime())
        assertEquals("the store's reset was called exactly once", 1, r.store.resetCalls())
    }

    // REQ-034.A1 + rule "Settings (timer, sound) are kept": sound was OFF and the timer ON before; both are exactly so after, in the
    // store and in the controller's own state (with sound off the player must not hear a sound after a reset).
    @Test
    fun req034_A1_theResetKeepsTheSettingsSoundOffStaysOff() {
        val r = rig()
        assertSeededFixtureIsNotNew(r)
        r.controller.open()
        assertFalse("fixture: the controller reads sound off", r.controller.soundOn)
        r.controller.requestReset()
        r.controller.confirmReset()
        assertEquals(keptSettings, r.store.settings())
        assertFalse("sound is still off", r.controller.soundOn)
        assertEquals("the last shown puzzle is kept (store KDoc: only puzzles and play time are reset)", ids[1], r.store.lastShownPuzzle())
    }

    // REQ-034.A1 - the board-side callback: called once, and after the store call (it reloads what the reset erased; before the erase
    // it would reload the old state).
    @Test
    fun req034_A1_theReloadCallbackRunsOnceAfterTheStoreIsErased() {
        val r = rig()
        r.controller.open()
        r.controller.requestReset()
        r.controller.confirmReset()
        assertEquals(1, r.resets)
        assertEquals(listOf("resetAllProgress", "onReset"), r.calls)
    }

    // REQ-034.A1 - the controller leaves the confirmation state clean after the erase, and the screen stays usable.
    @Test
    fun req034_A1_afterTheEraseTheQuestionIsGone() {
        val r = rig()
        r.controller.open()
        r.controller.requestReset()
        r.controller.confirmReset()
        assertFalse(r.controller.confirmingReset)
    }

    // decision DA-120 (section 5: "`if (!confirmingReset) return` (a double tap erases once)"): a second Erase tap does nothing more.
    @Test
    fun decisionDA120_aDoubleTapOnEraseErasesOnce() {
        val r = rig()
        r.controller.open()
        r.controller.requestReset()
        r.controller.confirmReset()
        r.controller.confirmReset()
        assertEquals(1, r.store.resetCalls())
        assertEquals(1, r.resets)
    }

    // decision DA-120 / REQ-034 rule "The reset needs a second, explicit confirmation": Erase without the question showing is a no-op.
    @Test
    fun decisionDA120_eraseWithoutAskingFirstDoesNothing() {
        val r = rig()
        val before = r.store.snapshot()
        r.controller.open()
        r.controller.confirmReset()
        assertEquals(emptyList<String>(), r.calls)
        assertEquals(before, r.store.snapshot())
    }

    // decision DA-120: a new reset needs a new question; the confirmation is not left armed by the first one.
    @Test
    fun decisionDA120_aSecondResetNeedsItsOwnQuestion() {
        val r = rig()
        r.controller.open()
        r.controller.requestReset()
        r.controller.confirmReset()
        r.store.seed(ids[0], solved) // the player plays on and solves again
        r.controller.confirmReset() // no question: nothing
        assertEquals(solved, r.store.progress(ids[0]))
        assertEquals(1, r.store.resetCalls())
        r.controller.requestReset()
        r.controller.confirmReset()
        assertEquals(PuzzleProgress.NEW, r.store.progress(ids[0]))
        assertEquals(2, r.store.resetCalls())
        assertEquals(2, r.resets)
    }

    // REQ-034.A2 - "Cancelling the confirmation changes nothing."
    // Keep: question shown, Keep tapped: not one store write, no callback, every stored value equal to the seed; the question is gone.
    @Test
    fun req034_A2_keepChangesNothing() {
        val r = rig()
        assertSeededFixtureIsNotNew(r)
        val before = r.store.snapshot()
        r.controller.open()
        r.controller.requestReset()
        assertEquals("asking writes nothing", emptyList<String>(), r.calls)
        r.controller.cancelReset()

        assertFalse(r.controller.confirmingReset)
        assertEquals("a store call or a callback happened", emptyList<String>(), r.calls)
        assertEquals("the stored data changed", before, r.store.snapshot())
        assertEquals(0, r.resets)
        assertTrue("the screen is still open after Keep", r.controller.isOpen)
        assertFalse("sound setting unchanged", r.controller.soundOn)
    }

    // REQ-034.A2 - Back (the same entry point as closing the screen, REQ-032 rule) cancels too: zero calls, and the cancelled
    // question cannot be answered later: reopening and tapping Erase does nothing (a lazy cancel that left the flag armed would erase).
    @Test
    fun req034_A2_closingTheScreenWithTheQuestionShowingCancelsAndStaysCancelled() {
        val r = rig()
        val before = r.store.snapshot()
        r.controller.open()
        r.controller.requestReset()
        r.controller.close()

        assertFalse(r.controller.isOpen)
        assertFalse(r.controller.confirmingReset)
        assertEquals(emptyList<String>(), r.calls)
        assertEquals(before, r.store.snapshot())

        r.controller.open()
        r.controller.confirmReset() // a stale Erase
        assertEquals("a stale confirmation erased the progress", emptyList<String>(), r.calls)
        assertEquals(before, r.store.snapshot())
        assertEquals(0, r.resets)
    }

    // REQ-034.A2 - Keep, then an Erase tap with no new question: nothing; and after Keep a fresh question works as usual
    // (cancelling does not break the later, real reset).
    @Test
    fun req034_A2_afterKeepEraseNeedsAFreshQuestion() {
        val r = rig()
        val before = r.store.snapshot()
        r.controller.open()
        r.controller.requestReset()
        r.controller.cancelReset()
        r.controller.confirmReset()
        assertEquals(emptyList<String>(), r.calls)
        assertEquals(before, r.store.snapshot())

        r.controller.requestReset()
        r.controller.confirmReset()
        assertEquals(1, r.store.resetCalls())
        for (id in ids) assertEquals(PuzzleProgress.NEW, r.store.progress(id))
    }

    // REQ-034.A2 - asking, cancelling and asking again, many times, still changes nothing until Erase.
    @Test
    fun req034_A2_askingAndKeepingRepeatedlyChangesNothing() {
        val r = rig()
        val before = r.store.snapshot()
        r.controller.open()
        repeat(4) {
            r.controller.requestReset()
            r.controller.cancelReset()
        }
        assertEquals(emptyList<String>(), r.calls)
        assertEquals(before, r.store.snapshot())
    }
}
