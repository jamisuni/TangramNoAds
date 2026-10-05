package io.github.jamisuni.tangram.time

import org.junit.Assert.assertEquals
import org.junit.Test

// WO-008 T8c (v-t): the covering test of REQ-029 A1 under the code home `time` (design, Acceptance IDs table).
// REQ-029 A1: "Leaving the phone untouched for 5 minutes adds at most 60 s." Design 2.1 and "Value shapes": after `touch`, 300 s of source
// time and `accrue()`, today and total rose by exactly 60; the same in 300 one-second steps; `visible == false` adds 0.
// "A held finger keeps counting" is NOT here: it is `ActiveSecondTest` (decision DA-134, N7a).
class IdleCutoffTest {

    // REQ-029.A1
    @Test fun oneFiveMinuteJumpAddsExactlySixtySeconds() {
        val r = GateRig()
        r.start()
        r.tap()
        r.pass(300_000)
        r.keeper.accrue()
        assertEquals(60L, r.keeper.todaySeconds)
        assertEquals(60L, r.keeper.totalSeconds)
    }

    // REQ-029.A1
    @Test fun threeHundredOneSecondStepsAddExactlySixtySeconds() {
        val r = GateRig()
        r.start()
        r.tap()
        r.tickFor(300)
        assertEquals(60L, r.keeper.todaySeconds)
        assertEquals(60L, r.keeper.totalSeconds)
    }

    // REQ-029.A1
    @Test fun anInvisibleAppAddsNothingWhateverTheTouches() {
        val r = GateRig()
        r.start()
        r.keeper.setVisible(false) // screen off or in the background
        r.tap()
        r.pass(300_000)
        r.keeper.accrue()
        assertEquals(0L, r.keeper.todaySeconds)
        assertEquals(0L, r.keeper.totalSeconds)
    }

    // REQ-029.A1
    @Test fun aNeverTouchedAppAddsNothing() {
        val r = GateRig()
        r.start()
        r.pass(300_000)
        r.keeper.accrue()
        assertEquals(0L, r.keeper.totalSeconds)
    }

    // REQ-029.A1: the next touch after the pause starts the count again (the owner's "until next press")
    @Test fun theNextTouchAfterTheIdlePauseCountsAgain() {
        val r = GateRig()
        r.start()
        r.tap()
        r.pass(300_000)
        r.keeper.accrue() // 60 s so far
        r.tap()
        r.tickFor(5)
        assertEquals(65L, r.keeper.totalSeconds)
        assertEquals(65L, r.keeper.todaySeconds)
    }

    // REQ-029.A1: a touch every 30 s keeps the count going for the whole time (no cut-off while the player plays)
    @Test fun aPlayerWhoKeepsTouchingIsNeverCutOff() {
        val r = GateRig()
        r.start()
        repeat(10) { r.tap(); r.tickFor(30) } // 300 s with a touch every 30 s
        assertEquals(300L, r.keeper.totalSeconds)
    }
}
