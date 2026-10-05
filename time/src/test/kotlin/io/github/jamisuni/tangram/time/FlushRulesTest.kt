package io.github.jamisuni.tangram.time

import io.github.jamisuni.tangram.contracts.progress.PlayTime
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.time.GateRig.Companion.A
import io.github.jamisuni.tangram.time.GateRig.Companion.inProgress
import io.github.jamisuni.tangram.time.GateRig.Companion.solved
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// decision DA-137: DECISION / GATE TEST (WO-008 T8c part ii, gate set of GATE-063), no requirement token.
//
// The CLOSED LIST OF WRITERS of design 4.3 (2) and the seam row `PlayTimeKeeper` ("Writing entries"):
//   flush(): `store.savePlayTime(PlayTime(day, today, total))`, then, for the shown puzzle, a read-modify-write of `puzzleSeconds` only if
//            the stored record is In progress and the value differs; never creates a record, never writes `state`, `pieces` or a best;
//            never throws, a store failure is swallowed (G-10).
//   accrue(): flushes when at least FLUSH_EVERY_MS = 10_000 of source time have passed since the last flush while counting, and when
//            `counting` just became false.
//   setVisible(false) and setPuzzleRunning(false): account, then flush (the stop points).
// plus the rev 2 E2 rule: `account()` sets or rolls `day` ONLY when it credits at least one whole active second, so `NONE` stays `NONE`
// and a stop-point flush after a reset writes nothing (the store skips an equal save).
//
// Moves that are not exact in the seam text are avoided: every flush() is preceded by an accrue() so the counters are current whether or
// not `flush()` accounts by itself.
class FlushRulesTest {

    // ---------------------------------------------------------------------------------------------------------- what a flush writes
    // Seam row: "store.savePlayTime(PlayTime(day, today, total)), then ... a read-modify-write of puzzleSeconds only if the stored record
    // is In progress and the value differs; ... never writes state, pieces or a best". Design 4.3 (2): "two whole-document writes".
    @Test fun aFlushWritesPlayTimeThenOnlyThePuzzleSecondsOfTheStoredInProgressRecord() {
        val r = GateRig()
        val stored = inProgress(3, 41)
        r.store.seedProgress(A, stored)
        r.start()
        r.show(A)
        r.keeper.setPuzzleRunning(true)
        r.tap()
        r.pass(7_000)
        r.keeper.accrue() // 7 s counted, below the 10 s mark
        val mark = r.store.mark()
        r.keeper.flush()
        val writes = r.store.writesSince(mark)
        assertEquals("a flush is two writes, play time first: $writes", listOf(GateStoreOp.SAVE_PLAY_TIME, GateStoreOp.SAVE_PROGRESS), writes.map { it.op })
        assertEquals(PlayTime(r.source.today(), 7, 7), writes[0].playTime)
        assertEquals("the record keeps state, pieces and best; only the seconds move (3 adopted + 7)", stored.copy(puzzleSeconds = 10), writes[1].progress)
        assertEquals(A, writes[1].puzzle)
    }

    // Seam row: "never creates a record, never writes state, pieces or a best"; "only if the stored record is In progress and the value
    // differs". A New puzzle (no record), a Solved record, and an In-progress record whose seconds already equal the counter.
    @Test fun aFlushNeverCreatesARecordNorTouchesOneThatIsNotInProgressOrAlreadyEqual() {
        // New: the first drag counts puzzle seconds while the stored state is still new (DA-49)
        run {
            val r = GateRig()
            r.start(); r.show(A); r.keeper.setPuzzleRunning(true); r.keeper.touch(true)
            r.tickFor(4)
            val mark = r.store.mark()
            r.keeper.flush()
            assertTrue("a New puzzle's flush made a record write: ${r.store.writesSince(mark)}", r.store.writesSince(mark).none { it.op == GateStoreOp.SAVE_PROGRESS })
            assertEquals(PuzzleProgress.NEW, r.store.progress(A))
        }
        // Solved: a record of another state is never rewritten
        run {
            val r = GateRig()
            r.store.seedProgress(A, solved(47, 41))
            r.start(); r.show(A); r.keeper.setPuzzleRunning(true); r.tap()
            r.tickFor(4)
            val mark = r.store.mark()
            r.keeper.flush()
            assertTrue("a Solved record was rewritten: ${r.store.writesSince(mark)}", r.store.writesSince(mark).none { it.op == GateStoreOp.SAVE_PROGRESS })
            assertEquals(solved(47, 41), r.store.progress(A))
        }
        // In progress with the same seconds: the puzzle clock is off, time passes, today and total rise, the record has nothing to learn
        run {
            val r = GateRig()
            r.store.seedProgress(A, inProgress(10, null))
            r.start(); r.show(A); r.keeper.setPuzzleRunning(false); r.tap()
            r.tickFor(4)
            assertEquals("fixture: the puzzle clock is off", 10L, r.keeper.puzzleSeconds)
            val mark = r.store.mark()
            r.keeper.flush()
            assertTrue("an unchanged record was written: ${r.store.writesSince(mark)}", r.store.writesSince(mark).none { it.op == GateStoreOp.SAVE_PROGRESS })
            assertEquals("today and total did rise", 4L, r.store.playTime().todaySeconds)
        }
    }

    // Design 4.3 (2): "for the shown puzzle ... when a puzzle is shown". With nothing shown, a flush writes the play time only.
    @Test fun aFlushWithNoPuzzleShownWritesOnlyThePlayTime() {
        val r = GateRig()
        r.start(); r.tap()
        r.tickFor(4)
        val mark = r.store.mark()
        r.keeper.flush()
        assertEquals(listOf(GateStoreOp.SAVE_PLAY_TIME), r.store.writesSince(mark).map { it.op })
        assertEquals(PlayTime(r.source.today(), 4, 4), r.store.playTime())
    }

    // ------------------------------------------------------------------------------------------------ when accrue() flushes
    // Seam row: "accrue() ... flushes when at least FLUSH_EVERY_MS = 10_000 of source time have passed since the last flush while
    // counting". The boundary is exact: 9_999 ms later no write, 10_000 ms later a write.
    @Test fun accrueFlushesWhenTenSecondsOfSourceTimeHavePassedSinceTheLastFlushNotBefore() {
        val r = GateRig()
        r.start(); r.tap()
        r.pass(2_000); r.keeper.accrue(); r.keeper.flush() // the last flush
        val mark = r.store.mark()
        r.pass(9_999); r.keeper.accrue()
        assertTrue("flushed 1 ms early: ${r.store.writesSince(mark)}", r.store.writesSince(mark).isEmpty())
        r.pass(1); r.keeper.accrue()
        val writes = r.store.writesSince(mark)
        assertTrue("no flush at exactly 10 s since the last one", writes.any { it.op == GateStoreOp.SAVE_PLAY_TIME })
        assertEquals("what it wrote is the counters at that moment", 12L, writes.first { it.op == GateStoreOp.SAVE_PLAY_TIME }.playTime?.totalSeconds)
    }

    // Seam row: "and when `counting` just became false" (design 4.3 (2): "accrue() when counting becomes false (idle expiry)"), which is a
    // flush even when the last one was only 6 s ago; and "none while idle" (4.3, Disk churn). Control: the same moment with the window kept
    // open by another touch writes nothing, so the flush is caused by the idle expiry and not by the clock.
    @Test fun accrueFlushesWhenCountingJustBecameFalseAndThenNothingWhileIdle() {
        // expiry: touched at 0, flush at 55 s, the 60 s window ends, the next accrue at 61 s notices
        run {
            val r = GateRig()
            r.start(); r.tap()
            r.pass(55_000); r.keeper.accrue() // due (55 s): the last flush is now
            val mark = r.store.mark()
            r.pass(6_000); r.keeper.accrue() // 61 s: counting became false, only 6 s since the last flush
            val written = r.store.writesSince(mark).filter { it.op == GateStoreOp.SAVE_PLAY_TIME }
            assertTrue("counting became false and nothing was flushed (idle expiry)", written.isNotEmpty())
            assertEquals("an untouched stretch credits exactly the 60 s window (REQ-029 A1: at most 60 s)", PlayTime(r.source.today(), 60, 60), written.last().playTime)
            // idle: five minutes of one-second accruals write nothing
            val idle = r.store.mark()
            r.tickFor(300)
            assertTrue("a write while idle: ${r.store.writesSince(idle).take(3)}", r.store.writesSince(idle).isEmpty())
        }
        // control: touched again at 55 s, so at 61 s the keeper is still counting and 6 s since the last flush is not due
        run {
            val r = GateRig()
            r.start(); r.tap()
            r.pass(55_000); r.keeper.accrue()
            r.tap()
            val mark = r.store.mark()
            r.pass(6_000); r.keeper.accrue()
            assertTrue("flushed early while still counting: ${r.store.writesSince(mark)}", r.store.writesSince(mark).isEmpty())
        }
    }

    // ------------------------------------------------------------------------------------------------------------- stop points
    // Seam row: "setVisible(false) ... accounts then flushes", "setPuzzleRunning(false) ... accounts then flushes" (4.3 (2): the closed
    // list). They flush whatever the clock says: 3 s after the last flush is not due, yet the stop point writes the 3 s.
    @Test fun theStopPointsAccountThenFlushWhateverTheClockSays() {
        for (stop in listOf<Pair<String, (GateRig) -> Unit>>(
            "setVisible(false)" to { r -> r.keeper.setVisible(false) },
            "setPuzzleRunning(false)" to { r -> r.keeper.setPuzzleRunning(false) },
        )) {
            val r = GateRig()
            r.store.seedProgress(A, inProgress(0, null))
            r.start(); r.show(A); r.keeper.setPuzzleRunning(true); r.tap()
            r.pass(3_000) // 3 uncredited seconds, far from the 10 s mark
            val mark = r.store.mark()
            stop.second(r)
            val writes = r.store.writesSince(mark)
            val playTime = writes.firstOrNull { it.op == GateStoreOp.SAVE_PLAY_TIME }?.playTime
            assertEquals("${stop.first} accounts the open 3 s and then flushes them", PlayTime(r.source.today(), 3, 3), playTime)
        }
    }

    // ------------------------------------------------------------------------------------------------------------- store failure
    // Seam row: "Never throws; a store failure is swallowed (G-10)". Every writing entry, with a store whose every write throws; then a
    // healthy store gets the then-current whole values (a flush writes whole counters, so nothing is lost by the failed attempts).
    @Test fun aStoreFailureIsSwallowedByEveryWritingEntryAndTheNextFlushWritesTheCurrentValues() {
        val r = GateRig()
        r.store.seedProgress(A, inProgress(0, null))
        r.start(); r.show(A); r.keeper.setPuzzleRunning(true); r.tap()
        r.store.failWrites = true
        r.tickFor(12) // a due flush inside accrue()
        r.keeper.flush()
        r.keeper.setPuzzleRunning(false)
        r.keeper.setVisible(false)
        r.store.failWrites = false
        r.keeper.setVisible(true)
        r.tap()
        r.tickFor(1)
        r.keeper.flush()
        assertEquals("the later flush writes the counters as they are now: 12 + 1 s", 13L, r.store.playTime().totalSeconds)
    }

    // ------------------------------------------------------------------------------------------------- E2 (rev 2): NONE stays NONE
    // Rev 2 E2 (design 5.1 and value shapes): "`account()` sets or rolls `day` only when it credits at least one whole active second (a zero
    // credit leaves `day` as it is, so `NONE` stays `NONE`)"; "After `afterReset()`, any number of `account()` calls with zero credit leave
    // `PlayTime` as `NONE` (`day == null`), and `flush()` then writes nothing (the store skips an equal save)". READING of "writes nothing":
    // the store's contents do not change (no EFFECTIVE write); the keeper may still call `savePlayTime` with a value equal to NONE, which the
    // store skips. Zero-credit accounts used: no time passes; the app is not visible while time passes; a fresh keeper never touched.
    @Test fun afterAResetAndAnyZeroCreditAccountFlushWritesNothingAndPlayTimeIsStillNone() {
        // (a) the HeldResetAppTest shape: play, reset, then the stop points and a flush with no credited second
        run {
            val r = GateRig()
            r.store.seedProgress(A, inProgress(10, null))
            r.start(); r.show(A); r.keeper.setPuzzleRunning(true); r.tap()
            r.tickFor(30); r.keeper.flush()
            assertEquals("fixture: something stored to erase", 30L, r.store.playTime().totalSeconds)
            r.tap() // the Erase tap
            r.store.resetAllProgress()
            r.keeper.afterReset()
            r.show(A)
            val mark = r.store.mark()
            r.keeper.setPuzzleRunning(false) // New: the gate turns the puzzle clock off, a stop point
            r.keeper.touch(true); r.keeper.touch(false) // a tap at the same instant: still no credited second
            r.keeper.setVisible(false) // the app goes to the background: a stop point, flush
            r.pass(20_000) // time passes while invisible: zero credit
            r.keeper.accrue()
            r.keeper.flush()
            r.assertNoEffectiveWrites("a stop-point flush or flush() after a reset with zero credit", mark)
            assertEquals(PlayTime.NONE, r.store.playTime())
            assertEquals(0L, r.keeper.todaySeconds)
            assertEquals(0L, r.keeper.totalSeconds)
        }
        // (b) a keeper that was never touched: seconds pass, nothing is credited, a flush stores NONE (an equal save)
        run {
            val r = GateRig()
            r.start()
            r.pass(30_000)
            r.keeper.accrue()
            val mark = r.store.mark()
            r.keeper.flush()
            r.keeper.setVisible(false)
            r.assertNoEffectiveWrites("flush of a keeper that credited nothing", mark)
            assertEquals(PlayTime.NONE, r.store.playTime())
        }
        // (c) the control that makes (a) and (b) mean something: one credited second and the day is set, to the source's date
        run {
            val r = GateRig()
            r.start(); r.tap()
            r.tickFor(1)
            r.keeper.flush()
            assertEquals(PlayTime(r.source.today(), 1, 1), r.store.playTime())
        }
    }
}
