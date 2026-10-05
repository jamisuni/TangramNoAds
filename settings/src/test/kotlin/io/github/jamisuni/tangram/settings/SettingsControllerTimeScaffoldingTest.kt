package io.github.jamisuni.tangram.settings

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.GameSettings
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// SCAFFOLDING (disposable, TASK-066): decision DA-124 and design WO-008 section 6. Not an acceptance test.
class SettingsControllerTimeScaffoldingTest {
    private val puzzles = PuzzleLibrary.packaged().puzzles

    @Test
    fun timerShownWriteKeepsSoundOn() {
        val store = RecordingStore().seedSettings(GameSettings(timerShown = false, soundOn = false))
        val c = SettingsController(store, onReset = {})
        assertFalse(c.timerShown)
        c.setTimerShown(true)
        assertTrue(c.timerShown)
        assertEquals(GameSettings(timerShown = true, soundOn = false), store.settings())
        c.setTimerShown(false)
        assertEquals(GameSettings(timerShown = false, soundOn = false), store.settings())
    }

    @Test
    fun revisionBumpsOnOpenAndConfirmedReset() {
        val c = SettingsController(RecordingStore(), onReset = {})
        val r0 = c.revision
        c.open()
        assertEquals(r0 + 1, c.revision)
        c.requestReset()
        c.confirmReset()
        assertEquals(r0 + 2, c.revision)
    }

    @Test
    fun readoutDefaultsToZeroAndPassesThrough() {
        assertEquals(0L, SettingsController(RecordingStore(), onReset = {}).readout.todaySeconds)
        val fake = object : PlayTimeReadout {
            override val todaySeconds = 7L
            override val totalSeconds = 99L
        }
        val c = SettingsController(RecordingStore(), readout = fake, onReset = {})
        assertEquals(7L, c.readout.todaySeconds)
        assertEquals(99L, c.readout.totalSeconds)
    }

    @Test
    fun bestListIsLibraryOrderOnlyWithABestAndEmptiesOnReset() {
        val store = RecordingStore()
        val solved = { s: Long -> PuzzleProgress(PuzzleState.SOLVED, emptyMap(), s, s) }
        store.seed(puzzles[2].id, solved(30)).seed(puzzles[0].id, solved(61))
        val c = SettingsController(store, puzzles = { puzzles }, onReset = {})
        assertEquals(listOf(puzzles[0] to 61L, puzzles[2] to 30L), c.bestTimes())
        c.requestReset()
        c.confirmReset()
        assertTrue(c.bestTimes().isEmpty())
    }
}
