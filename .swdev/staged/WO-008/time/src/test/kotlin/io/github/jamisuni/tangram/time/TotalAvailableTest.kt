package io.github.jamisuni.tangram.time

import io.github.jamisuni.tangram.contracts.progress.PlayTime
import io.github.jamisuni.tangram.time.GateRig.Companion.A
import io.github.jamisuni.tangram.time.GateRig.Companion.inProgress
import org.junit.Assert.assertEquals
import org.junit.Test

// WO-008 T8c (v-t): the covering test of REQ-005 A2 under the code home `time` (design, Acceptance IDs table):
// "total readable on a fresh store (0), after play, after idle, after afterReset() (0), after reload from a store".
// REQ-005 A2: "The total active play time is available at any time." The readout is exact at any time (design 6), not stale by up to
// one flush: the keeper's `totalSeconds` is read after `accrue()` with no flush in between.
class TotalAvailableTest {

    // REQ-005.A2
    @Test fun theTotalIsReadableOnAFreshStoreAsZero() {
        val r = GateRig()
        assertEquals(0L, r.keeper.totalSeconds)
        r.start()
        assertEquals(0L, r.keeper.totalSeconds)
    }

    // REQ-005.A2
    @Test fun theTotalIsReadableAtAnyTimeDuringPlayWithoutWaitingForAFlush() {
        val r = GateRig()
        r.store.seedProgress(A, inProgress(0, null))
        r.start()
        r.show(A)
        r.keeper.setPuzzleRunning(true)
        r.tap()
        for (second in 1..9) { // below the 10 s flush mark: nothing has been written, the reading is still exact
            r.tickFor(1)
            assertEquals("after $second s", second.toLong(), r.keeper.totalSeconds)
        }
    }

    // REQ-005.A2
    @Test fun theTotalIsReadableAfterIdleAndHasGrownByAtMostTheWindow() {
        val r = GateRig()
        r.start()
        r.tap()
        r.tickFor(20)
        r.pass(300_000) // five untouched minutes
        r.keeper.accrue()
        assertEquals("the 60 s window after the last touch is all that idle time adds, 20 s of it already played", 60L, r.keeper.totalSeconds)
    }

    // REQ-005.A2
    @Test fun theTotalIsReadableAfterAReset() {
        val r = GateRig()
        r.start()
        r.tap()
        r.tickFor(15)
        r.keeper.flush()
        assertEquals(15L, r.keeper.totalSeconds)
        r.store.resetAllProgress()
        r.keeper.afterReset()
        assertEquals("after a reset the total reads zero", 0L, r.keeper.totalSeconds)
        assertEquals(PlayTime.NONE, r.store.playTime())
    }

    // REQ-005.A2
    @Test fun theTotalIsReadableAfterAReloadFromTheStore() {
        val r = GateRig()
        r.start()
        r.tap()
        r.tickFor(25)
        r.keeper.setVisible(false) // a stop point: everything is written
        val relaunched = PlayTimeKeeper(r.source, r.store) // a new process over the same stored document
        assertEquals("the total survives a relaunch", 25L, relaunched.totalSeconds)
        assertEquals(r.source.today(), r.store.playTime().day)
    }
}
