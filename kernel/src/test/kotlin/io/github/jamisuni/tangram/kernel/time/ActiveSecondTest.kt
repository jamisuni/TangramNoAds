package io.github.jamisuni.tangram.kernel.time

import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// decision DA-134 (the active second) and DA-135 (which clock counts what): DECISION TEST (WO-008 T8c v-k), no requirement token.
// Design 2.1 and "Value shapes": `activeMs` returns, of the interval (prevMs, nowMs], the milliseconds that were visible and within
// 60 s of a touch; `touching` with `visible` credits the whole interval; `lastTouchMs == null` credits 0; a touch exactly 60 000 ms before
// `nowMs` credits through `nowMs` and not a millisecond more; an interval wholly beyond the window credits 0; `visible == false` credits 0.
// Every interval below starts at or after the last touch: each keeper input closes the open interval under the OLD flags before it
// records a new touch (2.1), so a touch inside an interval is not a case the keeper produces and the design does not state its value.
class ActiveSecondTest {
    private val t0 = 5_000_000L // a touch at an arbitrary monotonic reading

    @Test fun theIdleLimitIsSixtySeconds() {
        // TYPE-005's locked ASSUMPTION, one constant (2.1)
        assertEquals(60_000L, ActiveSecond.IDLE_LIMIT_MS)
    }

    @Test fun aFingerHeldDownCreditsTheWholeIntervalWhateverTheLastTouch() {
        // DA-134: "a finger held down counts as engaged" (touching), even with no or a stale last touch, even over 5 minutes
        assertEquals(300_000L, ActiveSecond.activeMs(t0, t0 + 300_000, visible = true, lastTouchMs = t0, touching = true))
        assertEquals(300_000L, ActiveSecond.activeMs(t0, t0 + 300_000, visible = true, lastTouchMs = null, touching = true))
        assertEquals(1_000L, ActiveSecond.activeMs(t0, t0 + 1_000, visible = true, lastTouchMs = t0 - 10_000_000, touching = true))
        // ... but never while the app is not visible
        assertEquals(0L, ActiveSecond.activeMs(t0, t0 + 300_000, visible = false, lastTouchMs = t0, touching = true))
    }

    @Test fun anInvisibleAppCreditsNothing() {
        assertEquals(0L, ActiveSecond.activeMs(t0, t0 + 10_000, visible = false, lastTouchMs = t0, touching = false))
    }

    @Test fun neverTouchedCreditsNothing() {
        assertEquals(0L, ActiveSecond.activeMs(t0, t0 + 10_000, visible = true, lastTouchMs = null, touching = false))
    }

    @Test fun anEmptyOrBackwardsIntervalCreditsNothing() {
        assertEquals(0L, ActiveSecond.activeMs(t0, t0, visible = true, lastTouchMs = t0, touching = true))
        assertEquals(0L, ActiveSecond.activeMs(t0, t0 - 5, visible = true, lastTouchMs = t0, touching = true))
        assertEquals(0L, ActiveSecond.activeMs(t0, t0 - 5, visible = true, lastTouchMs = t0 - 1_000, touching = false))
    }

    @Test fun anIntervalInsideTheWindowIsCreditedWhole() {
        assertEquals(10_000L, ActiveSecond.activeMs(t0, t0 + 10_000, visible = true, lastTouchMs = t0, touching = false))
        assertEquals(1L, ActiveSecond.activeMs(t0 + 59_998, t0 + 59_999, visible = true, lastTouchMs = t0, touching = false))
    }

    @Test fun fiveMinutesUntouchedCreditExactlySixtySeconds() {
        // REQ-029's "at most 60 s": the window is inclusive of its last millisecond and not a millisecond more (value shapes)
        assertEquals(60_000L, ActiveSecond.activeMs(t0, t0 + 300_000, visible = true, lastTouchMs = t0, touching = false))
    }

    @Test fun aTouchExactlySixtySecondsBeforeNowCreditsThroughNowAndNotAMillisecondMore() {
        assertEquals(1_000L, ActiveSecond.activeMs(t0 + 59_000, t0 + 60_000, visible = true, lastTouchMs = t0, touching = false))
        // an interval that straddles the end of the window: only the part inside it counts
        assertEquals(1_000L, ActiveSecond.activeMs(t0 + 59_000, t0 + 61_000, visible = true, lastTouchMs = t0, touching = false))
        assertEquals(1L, ActiveSecond.activeMs(t0 + 59_999, t0 + 70_000, visible = true, lastTouchMs = t0, touching = false))
    }

    @Test fun anIntervalWhollyBeyondTheWindowCreditsNothing() {
        assertEquals(0L, ActiveSecond.activeMs(t0 + 60_000, t0 + 65_000, visible = true, lastTouchMs = t0, touching = false))
        assertEquals(0L, ActiveSecond.activeMs(t0 + 90_000, t0 + 95_000, visible = true, lastTouchMs = t0, touching = false))
    }

    @Test fun oneBigJumpCreditsWhatManySmallStepsCredit() {
        // 2.1 "interval accrual": a late tick neither loses nor invents time
        var sum = 0L
        for (i in 0 until 300) sum += ActiveSecond.activeMs(t0 + i * 1_000L, t0 + (i + 1) * 1_000L, visible = true, lastTouchMs = t0, touching = false)
        assertEquals(60_000L, sum)
        assertEquals(sum, ActiveSecond.activeMs(t0, t0 + 300_000, visible = true, lastTouchMs = t0, touching = false))
    }

    // DA-135 / design 2.2: puzzleCounts(state, settingsOpen, gridOpen) = state == IN_PROGRESS && !settingsOpen && !gridOpen.
    // TYPE-006 "time is counted only In progress"; a New puzzle starts no time; settings pauses puzzle time (REQ-032 rule); the grid pauses (F28).
    @Test fun thePuzzleClockRunsOnlyForAnInProgressPuzzleWithNothingOnTop() {
        for (state in PuzzleState.values()) for (settings in listOf(false, true)) for (grid in listOf(false, true)) {
            val expected = state == PuzzleState.IN_PROGRESS && !settings && !grid
            assertEquals("state=$state settingsOpen=$settings gridOpen=$grid", expected, ActiveSecond.puzzleCounts(state, settings, grid))
        }
        assertTrue(ActiveSecond.puzzleCounts(PuzzleState.IN_PROGRESS, settingsOpen = false, gridOpen = false))
        assertFalse(ActiveSecond.puzzleCounts(PuzzleState.NEW, settingsOpen = false, gridOpen = false))
        assertFalse(ActiveSecond.puzzleCounts(PuzzleState.SOLVED, settingsOpen = false, gridOpen = false))
    }
}
