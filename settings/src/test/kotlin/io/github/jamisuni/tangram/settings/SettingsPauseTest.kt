package io.github.jamisuni.tangram.settings

import androidx.compose.runtime.snapshots.Snapshot
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.time.ActiveSecond
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// decision DA-138: DECISION TEST (WO-008 T8c v-s), no requirement token. REQ-032's rule "puzzle time is paused while the settings screen is
// open", realised (design 5.3, DA-138) as one derivation: `ActiveSecond.puzzleCounts(host.state, settings.isOpen, controller.gridOpen)` read by a
// `snapshotFlow`. This module owns the `settings.isOpen` input: it must be true from `open()` until `close()`, stay true while a reset is being
// asked, cancelled or confirmed (DA-120: the overlay stays open after a confirmed reset), stay false when `canOpen` refuses (a drag is in
// progress), and be Compose snapshot state so the flow sees every change.
class SettingsPauseTest {

    private fun puzzleClockRuns(c: SettingsController) = ActiveSecond.puzzleCounts(PuzzleState.IN_PROGRESS, settingsOpen = c.isOpen, gridOpen = false)

    @Test fun theSettingsPauseThePuzzleClockFromOpenUntilClose() {
        val c = SettingsController(RecordingStore(), onReset = {})
        assertTrue("closed: an In-progress puzzle counts", puzzleClockRuns(c))
        c.open()
        assertFalse("open: paused", puzzleClockRuns(c))
        c.close()
        assertTrue("closed again: it resumes", puzzleClockRuns(c))
    }

    @Test fun theClockStaysPausedWhileAResetIsAskedCancelledAndConfirmed() {
        val c = SettingsController(RecordingStore(), onReset = {})
        c.open()
        c.requestReset()
        assertFalse("the confirmation is shown: still paused", puzzleClockRuns(c))
        c.cancelReset()
        assertFalse("cancelled: the screen is still open", puzzleClockRuns(c))
        c.requestReset()
        c.confirmReset()
        assertTrue("DA-120: the overlay stays open after a confirmed reset", c.isOpen)
        assertFalse("so the puzzle clock stays paused", puzzleClockRuns(c))
        c.close()
        assertTrue(puzzleClockRuns(c))
    }

    @Test fun aRefusedOpenDoesNotPauseThePuzzle() {
        // canOpen is false while a piece is being dragged: the settings do not open, so nothing pauses
        val c = SettingsController(RecordingStore(), canOpen = { false }, onReset = {})
        c.open()
        assertFalse(c.isOpen)
        assertTrue(puzzleClockRuns(c))
    }

    @Test fun isOpenIsSnapshotStateSoTheRunningFlowSeesEveryChange() {
        val c = SettingsController(RecordingStore(), onReset = {})
        val read = HashSet<Any>()
        Snapshot.observe(readObserver = { read += it }) { c.isOpen }
        assertTrue("reading `isOpen` must read a Compose state object, or `snapshotFlow` never re-emits", read.isNotEmpty())
        c.open()
        val readAfterOpen = HashSet<Any>()
        Snapshot.observe(readObserver = { readAfterOpen += it }) { assertEquals(true, c.isOpen) }
        assertTrue(readAfterOpen.isNotEmpty())
    }
}
