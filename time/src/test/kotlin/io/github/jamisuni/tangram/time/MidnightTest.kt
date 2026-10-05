package io.github.jamisuni.tangram.time

import io.github.jamisuni.tangram.contracts.progress.PlayTime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

// decision DA-139: DECISION TEST (WO-008 T8c v-t), no requirement token. Midnight and clock rules (design "Value shapes", Midnight):
// "the interval that ends after the date changed belongs entirely to the new day; a stored `day` that differs from `source.today()`, earlier
// or later, restarts today at 0 (F28); total is never reduced". REQ-029 rule: today restarts at local midnight. Seam row: `todaySeconds`
// is day-aware (0 when the held day differs from `source.today()`).
class MidnightTest {
    private val d1 = LocalDate.of(2026, 10, 5)

    @Test fun todayRestartsAtZeroWhenTheLocalDateChangesAndTotalKeepsItsValue() {
        val r = GateRig()
        r.start(); r.tap()
        r.tickFor(10)
        assertEquals(10L, r.keeper.todaySeconds)
        r.source.date = d1.plusDays(1) // midnight passes
        assertEquals("today reads 0 at once on the new date", 0L, r.keeper.todaySeconds)
        assertEquals("total is never reduced", 10L, r.keeper.totalSeconds)
        r.tickFor(1)
        assertEquals(1L, r.keeper.todaySeconds)
        assertEquals(11L, r.keeper.totalSeconds)
        r.keeper.flush()
        assertEquals(PlayTime(d1.plusDays(1), 1, 11), r.store.playTime())
    }

    @Test fun anIntervalEndingAfterTheDateChangedBelongsEntirelyToTheNewDay() {
        val r = GateRig()
        r.start(); r.tap()
        r.tickFor(5) // 5 s on day 1
        r.pass(2_000) // 2 s pass, uncredited; the date changes inside them
        r.source.date = d1.plusDays(1)
        r.keeper.accrue()
        assertEquals("the whole 2 s belong to the new day", 2L, r.keeper.todaySeconds)
        assertEquals(7L, r.keeper.totalSeconds)
    }

    @Test fun aStoredDayLaterThanTodayAlsoRestartsToday() {
        // the clock was set back: the stored day is in the future of the device's date (F28)
        val source = GateManualTimeSource(date = d1)
        val store = GateRecordingProgressStore()
        store.seedPlayTime(PlayTime(d1.plusDays(5), 100, 500))
        val keeper = PlayTimeKeeper(source, store)
        assertEquals("today restarts", 0L, keeper.todaySeconds)
        assertEquals("total is kept", 500L, keeper.totalSeconds)
        keeper.setVisible(true)
        keeper.touch(true); keeper.touch(false)
        source.advance(1_000); keeper.accrue()
        assertEquals(1L, keeper.todaySeconds)
        assertEquals(501L, keeper.totalSeconds)
        keeper.flush()
        assertEquals(PlayTime(d1, 1, 501), store.playTime())
    }

    @Test fun aStoredEarlierDayRestartsTodayAndKeepsTotal() {
        val source = GateManualTimeSource(date = d1)
        val store = GateRecordingProgressStore()
        store.seedPlayTime(PlayTime(d1.minusDays(1), 100, 500))
        val keeper = PlayTimeKeeper(source, store)
        assertEquals(0L, keeper.todaySeconds)
        assertEquals(500L, keeper.totalSeconds)
        keeper.setVisible(true)
        keeper.touch(true); keeper.touch(false)
        source.advance(3_000); keeper.accrue()
        keeper.flush()
        assertEquals(PlayTime(d1, 3, 503), store.playTime())
    }

    @Test fun aStoredDayEqualToTodayContinuesToday() {
        val source = GateManualTimeSource(date = d1)
        val store = GateRecordingProgressStore()
        store.seedPlayTime(PlayTime(d1, 100, 500))
        val keeper = PlayTimeKeeper(source, store)
        assertEquals(100L, keeper.todaySeconds)
        assertEquals(500L, keeper.totalSeconds)
        keeper.setVisible(true)
        keeper.touch(true); keeper.touch(false)
        source.advance(2_000); keeper.accrue()
        assertEquals(102L, keeper.todaySeconds)
        assertEquals(502L, keeper.totalSeconds)
    }
}
