package io.github.jamisuni.tangram.settings

import io.github.jamisuni.tangram.contracts.progress.GameSettings
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Turn
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Module-level cover of REQ-032 A1 (design WO-007 acceptance table: "open and close make no store write and no callback; a pending
 * confirmation is cleared"), plus the controller's decision-level behaviour (DA-117, DA-118) with no token.
 *
 * A1 reads "opening and closing the settings changes nothing on the board", excluding a confirmed reset (design section 2, F25).
 * At module level "changes nothing" has one reading: the controller touches no store and calls no callback (architecture
 * section 5 item 4); the board itself is proven on the device by the held test. Basis for every assertion: the seam table's
 * `SettingsController` row and the design's value shapes ("open(), close(), requestReset(), cancelReset() make no store write and
 * (except the confirmed path) no callback").
 */
class SettingsControllerTest {

    private class Rig(seed: RecordingStore.() -> Unit = {}, canOpen: () -> Boolean = { true }) {
        val calls = ArrayList<String>()
        val store = RecordingStore(calls).apply(seed)
        var resets = 0
        val controller = SettingsController(store, canOpen) {
            resets++
            calls += "onReset"
        }
    }

    private fun solved() = PuzzleProgress(PuzzleState.SOLVED, mapOf(PieceId.SQ to PieceSave.InTray(Turn(0), false)), 41, 41)
    private fun inProgress() = PuzzleProgress(PuzzleState.IN_PROGRESS, mapOf(PieceId.SQ to PieceSave.InTray(Turn(2), true)), 95, null)

    private fun seeded(): RecordingStore.() -> Unit = {
        seed(PuzzleId("a"), solved())
        seed(PuzzleId("b"), inProgress())
        seedPlayTime(RecordingStore.PLAY_TIME)
        seedSettings(GameSettings(timerShown = true, soundOn = false))
        seedLastShown(PuzzleId("b"))
    }

    // REQ-032.A1 - "Opening and closing the settings changes nothing on the board."
    // Module reading: open() then close() make no store write, call no callback, and leave every stored value as it was.
    @Test
    fun req032_A1_openingAndClosingWritesNothingAndCallsNothing() {
        val r = Rig(seeded())
        val before = r.store.snapshot()
        assertFalse("fixture: closed at the start", r.controller.isOpen)

        r.controller.open()
        assertTrue(r.controller.isOpen)
        r.controller.close()
        assertFalse(r.controller.isOpen)

        assertEquals("a store write or a callback happened", emptyList<String>(), r.calls)
        assertEquals("the stored data changed", before, r.store.snapshot())
        assertEquals("the reset callback ran", 0, r.resets)
    }

    // REQ-032.A1 - the same many times, in every order the player can do it (open twice, close without open, reopen): still no write.
    @Test
    fun req032_A1_repeatedOpeningAndClosingStillWritesNothing() {
        val r = Rig(seeded())
        val before = r.store.snapshot()
        repeat(5) {
            r.controller.open()
            r.controller.close()
        }
        r.controller.close() // a stray close while closed
        r.controller.open()
        r.controller.open()
        r.controller.close()
        assertEquals(emptyList<String>(), r.calls)
        assertEquals(before, r.store.snapshot())
        assertFalse(r.controller.isOpen)
    }

    // REQ-032.A1 - the whole screen without erasing: open, ask for a reset, change mind (Keep), close: nothing was written and the
    // board-side callback never ran (only a confirmed reset may call it).
    @Test
    fun req032_A1_requestAndCancelResetInsideTheScreenWritesNothingAndCallsNothing() {
        val r = Rig(seeded())
        val before = r.store.snapshot()
        r.controller.open()
        r.controller.requestReset()
        assertTrue(r.controller.confirmingReset)
        r.controller.cancelReset()
        assertFalse(r.controller.confirmingReset)
        r.controller.close()
        assertEquals(emptyList<String>(), r.calls)
        assertEquals(before, r.store.snapshot())
    }

    // REQ-032.A1 - "a pending confirmation is cleared" (design table): closing the screen with the question showing clears it, so the
    // screen opens clean next time and a later Erase tap cannot find a stale confirmation.
    @Test
    fun req032_A1_closingClearsAPendingConfirmationAndWritesNothing() {
        val r = Rig(seeded())
        val before = r.store.snapshot()
        r.controller.open()
        r.controller.requestReset()
        assertTrue("fixture: the question is showing", r.controller.confirmingReset)
        r.controller.close()
        assertFalse(r.controller.confirmingReset)
        r.controller.open()
        assertFalse("the screen reopens without the question", r.controller.confirmingReset)
        assertEquals(emptyList<String>(), r.calls)
        assertEquals(before, r.store.snapshot())
    }

    // decision DA-118 (seam: `open()` "no-op if !canOpen()"; F5: refused while a piece is dragged): a refused open changes nothing at
    // all, and it is decided at the moment of the call (a drag that ended lets the next open through).
    @Test
    fun decisionDA118_openIsRefusedWhileCanOpenIsFalseAndAllowedAgainAfterwards() {
        var dragging = true
        val r = Rig(seeded(), canOpen = { !dragging })
        val before = r.store.snapshot()
        r.controller.open()
        assertFalse("opened during a drag", r.controller.isOpen)
        assertEquals(emptyList<String>(), r.calls)
        assertEquals(before, r.store.snapshot())
        dragging = false
        r.controller.open()
        assertTrue(r.controller.isOpen)
    }

    // decision DA-118 (seam row: `open()` "clears confirmingReset"): an open always starts without the question.
    @Test
    fun decisionDA118_openClearsConfirmingReset() {
        val r = Rig(seeded())
        r.controller.open()
        r.controller.requestReset()
        assertTrue(r.controller.confirmingReset)
        r.controller.open()
        assertFalse(r.controller.confirmingReset)
    }

    // decision DA-117 (REQ-033 ASSUMPTION "sound is on by default"; seam: `soundOn` is read from `store.settings().soundOn`): a fresh
    // store reads sound on, and the controller reports what the store holds, so a stored "off" survives a relaunch.
    @Test
    fun decisionDA117_soundStartsFromTheStore() {
        assertTrue("on by default", Rig().controller.soundOn)
        assertFalse("a stored off is read", Rig({ seedSettings(GameSettings(soundOn = false)) }).controller.soundOn)
    }

    // decision DA-117 (section 4: `store.saveSettings(store.settings().copy(soundOn = on))`; value shape: "setSound leaves timerShown and
    // every other GameSettings field as read"): the switch is stored, the other field survives, exactly one save per tap.
    @Test
    fun decisionDA117_setSoundStoresTheSwitchAndKeepsTheTimerSetting() {
        val r = Rig({ seedSettings(GameSettings(timerShown = true, soundOn = true)) })
        r.controller.setSound(false)
        assertFalse(r.controller.soundOn)
        assertEquals(GameSettings(timerShown = true, soundOn = false), r.store.settings())
        assertEquals(listOf("saveSettings"), r.store.writes())
        r.controller.setSound(true)
        assertTrue(r.controller.soundOn)
        assertEquals(GameSettings(timerShown = true, soundOn = true), r.store.settings())
        assertEquals(listOf("saveSettings", "saveSettings"), r.store.writes())
    }

    // decision DA-117 (O-08: read-modify-write in one event, no caller keeps a stored record across events): the copy is taken from the
    // store at the moment of the tap. A timer setting changed in the store after the controller was built (WO-008's row) must survive.
    @Test
    fun decisionDA117_setSoundReadsTheStoreAtTheMomentOfTheTapNotAtConstruction() {
        val r = Rig({ seedSettings(GameSettings(timerShown = false, soundOn = true)) })
        r.store.saveSettings(GameSettings(timerShown = true, soundOn = true)) // another writer, after the controller exists
        r.controller.setSound(false)
        assertEquals(
            "a stale copy overwrote the timer setting",
            GameSettings(timerShown = true, soundOn = false),
            r.store.settings(),
        )
    }

    // decision DA-117: switching sound touches no puzzle, no play time, no last-shown (only the settings record is written).
    @Test
    fun decisionDA117_setSoundWritesNothingButTheSettings() {
        val r = Rig(seeded())
        val before = r.store.snapshot()
        r.controller.setSound(true)
        val after = r.store.snapshot()
        assertEquals(before.puzzles, after.puzzles)
        assertEquals(before.playTime, after.playTime)
        assertEquals(before.lastShown, after.lastShown)
        assertEquals(GameSettings(timerShown = true, soundOn = true), after.settings)
        assertEquals(0, r.resets)
    }
}
