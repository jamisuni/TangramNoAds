package io.github.jamisuni.tangram.settings

import io.github.jamisuni.tangram.contracts.progress.GameSettings
import io.github.jamisuni.tangram.contracts.progress.IProgressStore
import io.github.jamisuni.tangram.contracts.progress.PlayTime
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// SCAFFOLDING (disposable, TASK-053): decision DA-120. Not an acceptance test.
class SettingsControllerScaffoldingTest {
    private class FakeStore : IProgressStore {
        var settings = GameSettings(timerShown = true, soundOn = true)
        var resets = 0
        var saves = 0
        override fun progress(puzzle: PuzzleId): PuzzleProgress = error("unused")
        override fun saveProgress(puzzle: PuzzleId, progress: PuzzleProgress) = error("unused")
        override fun playTime(): PlayTime = error("unused")
        override fun savePlayTime(playTime: PlayTime) = error("unused")
        override fun settings(): GameSettings = settings
        override fun saveSettings(settings: GameSettings) { saves++; this.settings = settings }
        override fun lastShownPuzzle(): PuzzleId? = error("unused")
        override fun saveLastShownPuzzle(puzzle: PuzzleId) = error("unused")
        override fun resetAllProgress() { resets++ }
    }

    @Test
    fun resetTwoStepAndIdempotent() {
        val store = FakeStore()
        var calls = 0
        val c = SettingsController(store, onReset = { calls++ })
        c.confirmReset()
        assertEquals(0, store.resets)
        c.requestReset()
        c.cancelReset()
        c.confirmReset()
        assertEquals(0, store.resets)
        c.requestReset()
        c.confirmReset()
        c.confirmReset()
        assertEquals(1, store.resets)
        assertEquals(1, calls)
        assertFalse(c.confirmingReset)
    }

    @Test
    fun openCloseGateAndSound() {
        val store = FakeStore()
        var allowed = false
        val c = SettingsController(store, canOpen = { allowed }, onReset = {})
        c.open()
        assertFalse(c.isOpen)
        allowed = true
        c.open()
        c.requestReset()
        c.close()
        assertFalse(c.isOpen)
        assertFalse(c.confirmingReset)
        assertEquals(0, store.saves)
        c.setSound(false)
        assertFalse(c.soundOn)
        assertTrue(store.settings.timerShown)
        assertFalse(store.settings.soundOn)
    }
}
