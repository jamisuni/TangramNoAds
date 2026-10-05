package io.github.jamisuni.tangram.settings

import io.github.jamisuni.tangram.contracts.progress.GameSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// decision DA-141: DECISION TEST (WO-008 T8c v-s), no requirement token. Design 6 and the seam row for `settings`:
// `SettingsController(store, canOpen = { true }, readout = PlayTimeReadout.NONE, puzzles = { emptyList() }, onReset)` with
// `val timerShown: Boolean`, `fun setTimerShown(on: Boolean)`, `val revision: Int`. Design 6: `timerShown` starts as `store.settings().timerShown`;
// `setTimerShown(on)` = `store.saveSettings(store.settings().copy(timerShown = on))` then the state ("O-08: `soundOn` survives, tested both
// ways"); `revision` is "bumped by `open()` and `confirmReset()`". The new parameters sit before `onReset`, so `onReset` is passed by name here.
class SettingsTimerTest {

    private fun controller(store: RecordingStore, onReset: () -> Unit = {}) = SettingsController(store, onReset = onReset)

    @Test fun theTimerStartsAsTheStoredSettingAndOffOnAFreshInstall() {
        assertFalse("the default is off", controller(RecordingStore()).timerShown)
        assertTrue(controller(RecordingStore().seedSettings(GameSettings(timerShown = true, soundOn = true))).timerShown)
    }

    @Test fun settingTheTimerWritesItThroughTheStoreAndUpdatesTheState() {
        val store = RecordingStore()
        val c = controller(store)
        c.setTimerShown(true)
        assertTrue(c.timerShown)
        assertTrue(store.settings().timerShown)
        assertEquals("one settings write", listOf("saveSettings"), store.writes())
        c.setTimerShown(false)
        assertFalse(c.timerShown)
        assertFalse(store.settings().timerShown)
    }

    @Test fun switchingTheTimerKeepsTheSoundSettingBothWays() {
        val store = RecordingStore().seedSettings(GameSettings(timerShown = false, soundOn = false))
        val c = controller(store)
        c.setTimerShown(true)
        assertEquals("the sound setting survives the timer switch", GameSettings(timerShown = true, soundOn = false), store.settings())
        c.setSound(true)
        assertEquals("the timer setting survives the sound switch", GameSettings(timerShown = true, soundOn = true), store.settings())
    }

    @Test fun theTimerSwitchIsAReadModifyWriteOfTheStoredSettingsNotOfACopy() {
        val store = RecordingStore().seedSettings(GameSettings(timerShown = false, soundOn = false))
        val c = controller(store)
        store.seedSettings(GameSettings(timerShown = false, soundOn = true)) // changed behind the controller (O-08)
        c.setTimerShown(true)
        assertEquals(GameSettings(timerShown = true, soundOn = true), store.settings())
    }

    @Test fun theRevisionIsBumpedByOpenAndByAConfirmedResetAndNotByTheSwitch() {
        val store = RecordingStore()
        val c = controller(store)
        val r0 = c.revision
        c.setTimerShown(true)
        assertEquals("the timer switch does not bump it", r0, c.revision)
        c.open()
        val r1 = c.revision
        assertTrue("open() bumps it", r1 > r0)
        c.requestReset()
        c.cancelReset()
        assertEquals("asking and cancelling a reset do not", r1, c.revision)
        c.requestReset()
        c.confirmReset()
        assertTrue("a confirmed reset bumps it", c.revision > r1)
    }

    @Test fun aConfirmedResetKeepsTheTimerSettingAndCallsOnResetOnce() {
        // DA-120: a reset erases puzzles, bests and play time and keeps the settings
        val store = RecordingStore().seedSettings(GameSettings(timerShown = true, soundOn = false))
        var resets = 0
        val c = controller(store) { resets++ }
        c.open()
        c.requestReset()
        c.confirmReset()
        assertEquals(1, resets)
        assertTrue("the timer stays on after a reset", c.timerShown)
        assertEquals(GameSettings(timerShown = true, soundOn = false), store.settings())
    }
}
