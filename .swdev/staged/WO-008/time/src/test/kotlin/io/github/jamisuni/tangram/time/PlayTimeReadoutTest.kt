package io.github.jamisuni.tangram.time

import io.github.jamisuni.tangram.kernel.time.DurationFormat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

// WO-008 (TASK-069 trace rule 3): the covering test of REQ-029 A2 under the code home `time`. REQ-029 A2: "The settings screen shows today's and
// all-time play time." `time`'s part of it is that the keeper supplies today's and the all-time play time as the values the settings screen shows
// (`todaySeconds`, `totalSeconds`; the screen reads them through the `PlayTimeReadout` the app implements over the keeper, design 6), correct and
// readable at any time, and that they format as the screen shows them: m:ss below one hour and "h min" from one hour (REQ-029 rule, `DurationFormat`).
// The screen's own rows are covered in `settings` (SettingsPlayTimeTest) and in `app` (PlayTimeSettingsAppTest). Every assertion below is a nonzero
// or a distinguishing value: a keeper whose today never restarts at midnight, or whose total is not read back from the store, fails.
class PlayTimeReadoutTest {

    /** The value as the settings screen shows it (the one format everywhere, design 6). */
    private fun shown(seconds: Long): String {
        val p = DurationFormat.parts(seconds)
        return if (p.withHours) "${p.hours} h ${p.minutes} min" else "${p.minutes}:${"%02d".format(p.seconds)}"
    }

    private fun GateRig.readout() = shown(keeper.todaySeconds) to shown(keeper.totalSeconds)

    /** Touched play for [seconds]: a touch every 30 s keeps the 60 s window open. */
    private fun GateRig.play(seconds: Int) {
        var left = seconds
        while (left > 0) {
            tap()
            val step = minOf(30, left)
            tickFor(step)
            left -= step
        }
    }

    // REQ-029.A2
    @Test fun aFreshKeeperSuppliesZeroAndZero() {
        val r = GateRig()
        assertEquals("0:00" to "0:00", r.readout())
        r.start()
        assertEquals("starting the app credits nothing: " + r.readout(), "0:00" to "0:00", r.readout())
    }

    // REQ-029.A2
    @Test fun afterTouchedPlayBothValuesAreTheActiveSecondsAndFormatInTheOneFormat() {
        val r = GateRig()
        r.start()
        r.play(59)
        assertEquals("0:59" to "0:59", r.readout())
        r.play(3_540) // 3599 s in all
        assertEquals("59:59 is the last m:ss", "59:59" to "59:59", r.readout())
        r.play(1)
        assertEquals("3600 s is the first h min", "1 h 0 min" to "1 h 0 min", r.readout())
        r.play(125)
        assertEquals("1 h 2 min at 3725 s", "1 h 2 min" to "1 h 2 min", r.readout())
    }

    // REQ-029.A2: today and total are separate values: the keeper supplies each its own
    @Test fun todayAndTotalAreEachTheirOwnValueWhenTheStoredHistoryIsLonger() {
        val source = GateManualTimeSource()
        val store = GateRecordingProgressStore()
        store.seedPlayTime(io.github.jamisuni.tangram.contracts.progress.PlayTime(source.today(), 125, 7_325))
        val keeper = PlayTimeKeeper(source, store)
        assertEquals("2:05" to "2 h 2 min", shown(keeper.todaySeconds) to shown(keeper.totalSeconds))
        keeper.setVisible(true)
        keeper.touch(true); keeper.touch(false)
        source.advance(4_000); keeper.accrue()
        assertEquals("both grew by the 4 active seconds", "2:09" to "2 h 2 min", shown(keeper.todaySeconds) to shown(keeper.totalSeconds))
        assertEquals(129L, keeper.todaySeconds)
        assertEquals(7_329L, keeper.totalSeconds)
    }

    // REQ-029.A2: after an idle stretch the supplied values stop at what the 60 s window allowed (they do not keep growing)
    @Test fun afterAnIdleStretchTheValuesDoNotChangeAndAreStillReadable() {
        val r = GateRig()
        r.start()
        r.tap()
        r.tickFor(20)
        r.pass(300_000) // five untouched minutes
        r.keeper.accrue()
        assertEquals("20 s played and the rest of the 60 s window", "1:00" to "1:00", r.readout())
        r.pass(300_000)
        r.keeper.accrue()
        assertEquals("another five idle minutes change nothing", "1:00" to "1:00", r.readout())
    }

    // REQ-029.A2 + REQ-029 rule "Today's time restarts at local midnight"
    @Test fun afterALocalMidnightChangeTodayRestartsAtZeroAndTotalIsKept() {
        val r = GateRig()
        r.start()
        r.play(10)
        assertEquals("0:10" to "0:10", r.readout())
        r.source.date = LocalDate.of(2026, 10, 6)
        assertEquals("at midnight today is 0 and the total is kept", "0:00" to "0:10", r.readout())
        r.play(1)
        assertEquals("the next second belongs to the new day", "0:01" to "0:11", r.readout())
    }

    // REQ-029.A2: after a reload from the store both are read back (the same day), and on a later day today restarts and total is read back
    @Test fun afterAReloadFromTheStoreBothValuesAreReadBack() {
        val r = GateRig()
        r.start()
        r.play(25)
        r.keeper.setVisible(false) // a stop point: written
        val sameDay = PlayTimeKeeper(r.source, r.store)
        assertEquals("a relaunch the same day", "0:25" to "0:25", shown(sameDay.todaySeconds) to shown(sameDay.totalSeconds))
        r.source.date = LocalDate.of(2026, 10, 7)
        val nextDay = PlayTimeKeeper(r.source, r.store)
        assertEquals("a relaunch the next day: today restarts, the total is read back", "0:00" to "0:25", shown(nextDay.todaySeconds) to shown(nextDay.totalSeconds))
    }

    // REQ-029.A2: after a reset both read zero, and the next play counts from zero
    @Test fun afterAResetBothValuesAreZeroAndCountFromZeroAgain() {
        val r = GateRig()
        r.start()
        r.play(15)
        r.keeper.flush()
        assertEquals("fixture: something to erase", "0:15" to "0:15", r.readout())
        r.store.resetAllProgress()
        r.keeper.afterReset()
        assertEquals("0:00" to "0:00", r.readout())
        r.play(3)
        assertEquals("only the post-reset seconds", "0:03" to "0:03", r.readout())
    }
}
