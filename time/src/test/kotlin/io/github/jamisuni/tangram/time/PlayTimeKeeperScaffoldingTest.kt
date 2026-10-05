package io.github.jamisuni.tangram.time

import io.github.jamisuni.tangram.contracts.progress.PlayTime
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

// SCAFFOLDING (disposable, TASK-063 implementer): no requirement token.
class PlayTimeKeeperScaffoldingTest {
    @Test fun oneJumpAndManyStepsCreditTheSame() {
        val a = GateRig(); a.start(); a.tap(); a.pass(300_000); a.keeper.accrue()
        val b = GateRig(); b.start(); b.tap(); b.tickFor(300)
        assertEquals(60L, a.keeper.totalSeconds)
        assertEquals(60L, b.keeper.totalSeconds)
        assertEquals(60L, a.keeper.todaySeconds)
        assertFalse(a.keeper.counting)
    }

    @Test fun midnightBelongsToTheNewDayAndTotalIsKept() {
        val r = GateRig(); r.start(); r.tap(); r.tickFor(5)
        r.source.date = LocalDate.of(2026, 10, 6)
        assertEquals(0L, r.keeper.todaySeconds)
        r.tickFor(3)
        assertEquals(3L, r.keeper.todaySeconds)
        assertEquals(8L, r.keeper.totalSeconds)
    }

    @Test fun countingChangesAreReported() {
        val seen = ArrayList<Boolean>()
        val source = GateManualTimeSource()
        val k = PlayTimeKeeper(source, GateRecordingProgressStore()) { seen += it }
        k.setVisible(true); k.touch(true); k.touch(false)
        source.advance(61_000); k.accrue()
        assertEquals(listOf(true, false), seen)
        assertEquals(60L, k.totalSeconds)
    }

    @Test fun aNewKeeperStartsFromTheStoredPlayTimeWithoutWriting() {
        val source = GateManualTimeSource()
        val store = GateRecordingProgressStore()
        store.seedPlayTime(PlayTime(source.today(), 40, 900))
        val k = PlayTimeKeeper(source, store)
        assertEquals(40L, k.todaySeconds)
        assertEquals(900L, k.totalSeconds)
        assertEquals(emptyList<GateStoreCall>(), store.writesSince(0))
        source.date = source.date.plusDays(1)
        assertEquals(0L, k.todaySeconds)
        assertEquals(900L, k.totalSeconds)
        source.date = source.date.minusDays(2)
        assertEquals(0L, k.todaySeconds)
    }
}
