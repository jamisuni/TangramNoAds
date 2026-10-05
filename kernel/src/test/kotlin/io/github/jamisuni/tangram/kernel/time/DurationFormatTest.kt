package io.github.jamisuni.tangram.kernel.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// decision DA-135: DECISION TEST (WO-008 T8c v-k), no requirement token. One time format everywhere (F28, accepted at G1; design 6):
// m:ss below one hour, "h min" from one hour (REQ-029 rule). Seam row: `DurationFormat.parts(totalSeconds)`: below one hour
// `withHours = false`; from 3600 s `withHours = true`, seconds floored away; a negative input reads as 0.
// Value shapes: `parts(3599)` is 59:59, `parts(3600)` is 1 h 0 min, `parts(0)` is 0:00.
// The `seconds` field is asserted only below one hour: from one hour it is "floored away" and its value is not stated.
class DurationFormatTest {
    private fun below(total: Long, minutes: Long, seconds: Long) {
        val p = DurationFormat.parts(total)
        assertFalse("$total s is below one hour", p.withHours)
        assertEquals("$total hours", 0L, p.hours)
        assertEquals("$total minutes", minutes, p.minutes)
        assertEquals("$total seconds", seconds, p.seconds)
    }

    private fun from(total: Long, hours: Long, minutes: Long) {
        val p = DurationFormat.parts(total)
        assertTrue("$total s is one hour or more", p.withHours)
        assertEquals("$total hours", hours, p.hours)
        assertEquals("$total minutes", minutes, p.minutes)
    }

    @Test fun belowOneHourIsMinutesAndSeconds() {
        below(0, 0, 0)
        below(1, 0, 1)
        below(59, 0, 59)
        below(60, 1, 0)
        below(125, 2, 5)
        below(3599, 59, 59)
    }

    @Test fun fromOneHourIsHoursAndMinutesWithSecondsFlooredAway() {
        from(3600, 1, 0)
        from(3659, 1, 0) // 59 s floored away, not rounded up to 1 min
        from(3660, 1, 1)
        from(7199, 1, 59)
        from(7200, 2, 0)
        from(10 * 3600L + 59 * 60 + 59, 10, 59)
        from(88 * 3600L + 59 * 60 + 59, 88, 59) // the widest value the pill template allows
    }

    @Test fun aNegativeInputReadsAsZero() {
        below(-1, 0, 0)
        below(-100_000, 0, 0)
    }
}
