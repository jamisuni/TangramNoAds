package io.github.jamisuni.tangram.time

import io.github.jamisuni.tangram.contracts.progress.PlayTime
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.time.GateRig.Companion.A
import io.github.jamisuni.tangram.time.GateRig.Companion.B
import io.github.jamisuni.tangram.time.GateRig.Companion.inProgress
import io.github.jamisuni.tangram.time.GateRig.Companion.solved
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// decision DA-138: DECISION / GATE TEST (WO-008 T8c part ii, the done-check gate of TASK-063), no requirement token.
//
// What it guards: design 5.1 ("Account, then write: one owner, a closed list of writers") and review F1, the Blocker that found an
// erased play time coming back after a confirmed reset. The keeper separates ACCOUNTING (in memory; used by every input; never touches the
// store) from WRITING (a closed list). The design's own consequence, "stated and tested": `puzzleShown`, `touch`, `solved`, `mergeInto` and
// `afterReset` CANNOT write, "so no ordering of them against open(), restart() or the reset can write a stale value" (5.1), and the best
// is computed from the stored base, never cached (3, 4.4; seam row PlayTimeKeeper).
//
// The nine cases are KEEPER-LEVEL SIMULATIONS (design, scaffolding list, rev 2 E1): the test plays the part of BrowseController,
// SettingsController and SessionHost over a recording fake store (GateRecordingProgressStore); `time`'s test scope has only `content`
// (G-06) and no build file edit is allowed. The real wiring, with a flush due before Erase, is pinned in `app`'s TimeStoreRoundTripTest.
// Every case makes a flush DUE at the moment under test: time passes (GateRig.pass) with no keeper call, so the next input that
// accounts would, in a keeper that lets an input flush, write.
//
// Reading of "a flush is due": at least 10 s of source time since the last flush while counting (seam row, FLUSH_EVERY_MS = 10_000).
class OwnershipOrderTest {

    // ---------------------------------------------------------------------------------------------------------------- (1)
    // Design scaffolding list (1): "a reset with a flush due: no savePlayTime / saveProgress between resetAllProgress() and the end of
    // onReset, play time NONE afterwards, and after the next flush only post-reset seconds". Both call orders are played: the design's
    // `onReset = { time.afterReset(); controller.afterReset() }` (afterReset first, then the show that reaches puzzleShown) and the
    // reverse, because 5.1 says no ordering of the inputs may write a stale value (rev 0's order is the one review F1 found unsafe).
    @Test fun case1_aResetWithAFlushDueWritesNothingAndNothingErasedComesBack() {
        for ((afterResetFirst, tapOnErase) in listOf(true to true, false to true, true to false, false to false)) {
            // tapOnErase = false: Erase activated without a pointer (keyboard or an accessibility click), so no touch input accounts just
            // before the reset and the flush that fell due is still due when the reset runs
            val order = (if (afterResetFirst) "afterReset then puzzleShown (the design's onReset)" else "puzzleShown then afterReset") +
                (if (tapOnErase) ", Erase by tap" else ", Erase without a touch")
            val r = GateRig()
            r.store.seedProgress(A, inProgress(10, null))
            r.start()
            r.show(A)
            r.keeper.setPuzzleRunning(true)
            r.tap()
            r.pass(12_000)
            r.keeper.accrue()
            r.keeper.flush() // the store now holds play time (12 s): there is something to erase
            assertEquals("fixture: a play time is stored before the reset", 12L, r.store.playTime().totalSeconds)
            r.pass(15_000) // a flush is due now
            if (tapOnErase) r.tap() // the tap on Erase: accounts, writes nothing

            r.store.resetAllProgress()
            val afterReset = r.store.mark() // the window under test: from the end of resetAllProgress() to the end of onReset
            if (afterResetFirst) { r.keeper.afterReset(); r.show(A) } else { r.show(A); r.keeper.afterReset() }
            r.assertNoWrites("[$order] between resetAllProgress() and the end of onReset", afterReset)

            assertEquals("[$order] play time is NONE afterwards", PlayTime.NONE, r.store.playTime())
            assertEquals("[$order] the erased record stays erased", PuzzleProgress.NEW, r.store.progress(A))
            assertEquals("[$order] today", 0L, r.keeper.todaySeconds)
            assertEquals("[$order] total", 0L, r.keeper.totalSeconds)
            assertEquals("[$order] puzzle seconds", 0L, r.keeper.puzzleSeconds)

            // the player goes on after the reset: after the NEXT flush the store holds only the post-reset seconds
            r.keeper.setPuzzleRunning(false)
            val next = r.store.mark()
            r.tickFor(12)
            r.keeper.flush()
            assertEquals(
                "[$order] after the next flush only the 12 post-reset seconds are stored",
                PlayTime(r.source.today(), 12, 12), r.store.playTime(),
            )
            val stale = r.store.writesSince(next).mapNotNull { it.playTime }.filter { it.todaySeconds > 12 || it.totalSeconds > 12 }
            assertTrue("[$order] a pre-reset value was written back: $stale", stale.isEmpty())
        }
    }

    // ---------------------------------------------------------------------------------------------------------------- (2)
    // Scaffolding list (2): "restart() with a flush due: the old attempt is never written". BrowseController.restart() writes
    // `progress.restarted()` (seconds 0, best kept) and then shows the puzzle; `puzzleShown` adopts the stored 0 (5.1 table row 3).
    @Test fun case2_aRestartWithAFlushDueNeverWritesTheOldAttempt() {
        val r = GateRig()
        r.store.seedProgress(A, inProgress(30, 41))
        r.start()
        r.show(A)
        r.keeper.setPuzzleRunning(true)
        r.tap()
        r.pass(15_000) // 15 more seconds of the old attempt, uncredited, and a flush is due

        r.store.saveProgress(A, r.store.progress(A).restarted()) // restart(): the restarted record first ...
        val restarted = r.store.progress(A)
        val afterRestartWrite = r.store.mark()
        r.show(A) // ... then show -> puzzleShown
        r.assertNoWrites("puzzleShown after a restart", afterRestartWrite)
        assertEquals("the new attempt adopts the stored 0", 0L, r.keeper.puzzleSeconds)
        assertEquals("the restarted record is as written: New, 0 s, best kept", PuzzleState.NEW, restarted.state)
        assertEquals(restarted, r.store.progress(A))
        assertEquals("the best survives the restart", 41L, r.store.progress(A).bestSeconds)

        // the next attempt: a New puzzle has no puzzle clock; the first drag starts it; the drop is merged
        r.keeper.setPuzzleRunning(false)
        r.keeper.setPuzzleRunning(true)
        r.tickFor(12)
        val keeperSaves = r.store.callsSince(afterRestartWrite).filter { it.op == GateStoreOp.SAVE_PROGRESS }
        assertTrue("the keeper wrote a record of a New puzzle (old attempt?): $keeperSaves", keeperSaves.isEmpty())
        val merged = r.keeper.mergeInto(A, restarted.copy(state = PuzzleState.IN_PROGRESS, pieces = inProgress(0, null).pieces))
        assertEquals("the new attempt counts from 0: 12 s, not the old attempt's 30 + 15 + 12", 12L, merged.puzzleSeconds)
        assertEquals(41L, merged.bestSeconds)
    }

    // ---------------------------------------------------------------------------------------------------------------- (3)
    // Scaffolding list (3): "open() (a leave) with a flush due: the leaving seconds are written exactly once, by the merge, and the new
    // puzzle adopted". The leave is capture -> mergeInto -> one saveProgress by the browse side, then show -> puzzleShown (5.1 table row 1).
    @Test fun case3_openingAnotherPuzzleWithAFlushDueWritesTheLeavingSecondsOnlyThroughTheMerge() {
        val r = GateRig()
        r.store.seedProgress(A, inProgress(10, null))
        r.store.seedProgress(B, inProgress(5, 70))
        r.start()
        r.show(A)
        r.keeper.setPuzzleRunning(true)
        r.tap()
        r.pass(17_000) // 17 leaving seconds, uncredited, flush due

        val beforeMerge = r.store.mark()
        val merged = r.keeper.mergeInto(A, r.store.progress(A)) // capture
        r.assertNoWrites("mergeInto", beforeMerge)
        assertEquals("the merge accounts first: a leave loses no second", 27L, merged.puzzleSeconds)
        r.store.saveProgress(A, merged) // persist: the ONE write of the leaving seconds
        val beforeShow = r.store.mark()
        r.show(B)
        r.assertNoWrites("puzzleShown of the new puzzle", beforeShow)
        assertEquals("the new puzzle is adopted", 5L, r.keeper.puzzleSeconds)
        assertEquals(27L, r.store.progress(A).puzzleSeconds)

        // life goes on on B: the keeper's own flushes may write B and play time, never A
        r.tickFor(12)
        r.keeper.flush()
        val savesOfA = r.store.calls.filter { it.op == GateStoreOp.SAVE_PROGRESS && it.puzzle == A }
        assertEquals("the leaving seconds are written exactly once, by the merge: $savesOfA", 1, savesOfA.size)
        assertEquals("A keeps its leaving seconds", 27L, r.store.progress(A).puzzleSeconds)
        val b = r.store.progress(B)
        assertEquals("B stays In progress", PuzzleState.IN_PROGRESS, b.state)
        assertEquals("B's best is untouched", 70L, b.bestSeconds)
        assertEquals("B's seconds are its own (5 adopted + 12 counted)", 17L, b.puzzleSeconds)
    }

    // ---------------------------------------------------------------------------------------------------------------- (4)
    // Scaffolding list (4): "an aid solve whose counted seconds are below the stored best: best unchanged". REQ-030 rule 4 and
    // design 3: "`solved(true)` freezes the seconds counted so far and records no pending result, so the merge leaves the stored best as it
    // is"; value shapes: "`solved(true)` leaves `bestSeconds == base.bestSeconds` even when the counted seconds are below it".
    @Test fun case4_anAidSolveBelowTheStoredBestLeavesTheBestAlone() {
        for (storedBest in listOf<Long?>(50L, null)) {
            val r = GateRig()
            r.store.seedProgress(A, inProgress(0, storedBest))
            r.start()
            r.show(A)
            r.keeper.setPuzzleRunning(true)
            r.tap()
            r.tickFor(20) // 20 counted seconds: below a stored best of 50
            r.keeper.solved(true) // by the DEV aid
            val merged = r.keeper.mergeInto(A, r.store.progress(A).copy(state = PuzzleState.SOLVED))
            assertEquals("[stored best $storedBest] the best is the stored one", storedBest, merged.bestSeconds)
            assertEquals("[stored best $storedBest] the seconds counted so far are frozen", 20L, merged.puzzleSeconds)
            assertEquals(PuzzleState.SOLVED, merged.state)
            r.store.saveProgress(A, merged)

            // `solved` stops the puzzle clock (seam row), and nothing later rewrites the Solved record
            r.tickFor(12)
            r.keeper.flush()
            assertEquals("[stored best $storedBest] the puzzle clock stopped at the solve", 20L, r.keeper.puzzleSeconds)
            assertEquals("[stored best $storedBest] the solved record is as persisted", merged, r.store.progress(A))
        }
    }

    // ---------------------------------------------------------------------------------------------------------------- (5)
    // Scaffolding list (5): "a first drag of a New puzzle lasting more than 10 s: no write until the drop". Design 4.3 (2): the flush
    // "never creates a record, changes `state` or `pieces`"; "during the first drag of a New puzzle the stored state is `new` until the
    // drop (DA-49)". READING: the flush's own `savePlayTime` is allowed (the flush list writes it first); what must not happen is any
    // `saveProgress`, that is, no record is created or changed. The drag's seconds are kept for the drop's merge.
    @Test fun case5_aFirstDragOfANewPuzzleOverTenSecondsWritesNoRecordUntilTheDrop() {
        val r = GateRig()
        r.start()
        r.show(A) // a New puzzle: the store holds no record
        r.keeper.touch(true) // the finger goes down on a tray piece and stays down
        r.keeper.setPuzzleRunning(true) // the state moved New -> In progress at drag start (REQ-030 A3), the stored state did not
        val drag = r.store.mark()
        r.tickFor(15) // a drag lasting more than 10 s: the flush falls due inside it
        val recordWrites = r.store.callsSince(drag).filter { it.op == GateStoreOp.SAVE_PROGRESS }
        assertTrue("a record was written during the first drag (stored state is new until the drop, DA-49): $recordWrites", recordWrites.isEmpty())
        assertEquals("the stored record is still New", PuzzleProgress.NEW, r.store.progress(A))
        assertEquals("the drag's seconds are counted", 15L, r.keeper.puzzleSeconds)

        // the drop: the board captures an In-progress record; the merge carries the drag's seconds into it
        r.keeper.touch(false)
        val merged = r.keeper.mergeInto(A, inProgress(0, null))
        assertEquals(PuzzleState.IN_PROGRESS, merged.state)
        assertEquals("the drag's seconds ride the drop's write", 15L, merged.puzzleSeconds)
        r.store.saveProgress(A, merged)

        // from the drop on, the record exists and the flush keeps its seconds fresh
        r.tickFor(10)
        r.keeper.flush()
        val after = r.store.progress(A)
        assertEquals(25L, after.puzzleSeconds)
        assertEquals("the flush writes the seconds only", merged.copy(puzzleSeconds = 25), after)
    }

    // ---------------------------------------------------------------------------------------------------------------- (6a)
    // Scaffolding list (6a): "an own solve on A, then a leave to B and a settled event on B: B's stored best is untouched" (rev 2 E1).
    // "The pending result is cleared by `puzzleShown` and `afterReset`" (3); `mergeInto` "returns `base` unchanged for another id".
    // A flush is due at the leave, as in a player who studies the solved picture before moving on.
    @Test fun case6a_anOwnSolveThenALeaveToBAndASettledEventOnBLeavesBsBestUntouched() {
        val r = GateRig()
        r.store.seedProgress(A, inProgress(0, null))
        r.store.seedProgress(B, inProgress(5, null)) // B has no best: a leaked pending 30 would show
        r.start()
        r.show(A)
        r.keeper.setPuzzleRunning(true)
        r.tap()
        r.tickFor(30)
        r.keeper.solved(false) // the player's own solve, 30 s
        val ownMerge = r.keeper.mergeInto(A, r.store.progress(A).copy(state = PuzzleState.SOLVED))
        assertEquals("fixture: the own solve is merged as the best of A", 30L, ownMerge.bestSeconds)
        assertEquals(30L, ownMerge.puzzleSeconds)
        r.store.saveProgress(A, ownMerge)
        r.keeper.setPuzzleRunning(false) // the gate follows the Solved state
        assertEquals("a stop point never rewrites a Solved record", ownMerge, r.store.progress(A))

        r.pass(17_000) // looking at the solved picture; a flush is due at the leave
        val beforeMerge = r.store.mark()
        val leaveMerge = r.keeper.mergeInto(A, r.store.progress(A)) // capture on leaving
        r.assertNoWrites("mergeInto on the leave", beforeMerge)
        r.store.saveProgress(A, leaveMerge)
        val beforeShow = r.store.mark()
        r.show(B)
        r.assertNoWrites("puzzleShown of B", beforeShow)
        assertEquals("A keeps its best through the leave", 30L, r.store.progress(A).bestSeconds)

        // a settled event on B (a lock, a return, a turn): capture, merge, one saveProgress
        val settled = r.store.progress(B).copy(pieces = inProgress(0, null).pieces)
        val mergedB = r.keeper.mergeInto(B, settled)
        r.store.saveProgress(B, mergedB)
        assertNull("B's best is untouched by A's solve", r.store.progress(B).bestSeconds)
        assertNull(mergedB.bestSeconds)

        // the merge applies only to the keeper's current puzzle (B now): another id's record comes back as it went in
        val stale = r.store.progress(A).copy(puzzleSeconds = 99)
        assertEquals("another id's merge returns its input unchanged", stale, r.keeper.mergeInto(A, stale))
    }

    // ---------------------------------------------------------------------------------------------------------------- (6b)
    // Scaffolding list (6b): "an own solve on the shown puzzle, then a reset and a settled event: that puzzle's best stays `null`"
    // (rev 2 E1). `afterReset()` "zeroes ... the pending result" (seam row); together with 6a "they fail if the pending result is not
    // cleared by `puzzleShown` and `afterReset`". Three routes through the reset: afterReset alone, puzzleShown then afterReset, and the
    // design's afterReset then puzzleShown.
    @Test fun case6b_anOwnSolveThenAResetAndASettledEventLeavesTheBestNull() {
        for (route in listOf("afterReset only", "puzzleShown then afterReset", "afterReset then puzzleShown")) {
            val r = GateRig()
            r.store.seedProgress(A, inProgress(0, null))
            r.start()
            r.show(A)
            r.keeper.setPuzzleRunning(true)
            r.tap()
            r.tickFor(30)
            r.keeper.solved(false) // an own solve, 30 s, pending: the persist has not happened yet
            r.pass(17_000) // a flush is due
            r.tap() // the Erase tap

            r.store.resetAllProgress()
            val window = r.store.mark()
            when (route) {
                "afterReset only" -> r.keeper.afterReset()
                "puzzleShown then afterReset" -> { r.show(A); r.keeper.afterReset() }
                else -> { r.keeper.afterReset(); r.show(A) }
            }
            r.assertNoWrites("[$route] the reset's keeper part", window)

            // a settled event: the captured state is New (nothing placed yet) or In progress (a piece locked after the reset)
            for (captured in listOf(PuzzleProgress.NEW, inProgress(0, null))) {
                val merged = r.keeper.mergeInto(A, captured)
                assertNull("[$route] a reset puzzle's best stays null (pending solve not cleared?)", merged.bestSeconds)
                assertEquals("[$route] the puzzle's seconds were zeroed by the reset", 0L, merged.puzzleSeconds)
            }
            assertEquals("[$route] nothing was written back", PuzzleProgress.NEW, r.store.progress(A))
        }
    }

    // ---------------------------------------------------------------------------------------------------------------- (7)
    // Scaffolding list (7): "a best changed in the store behind the keeper is respected by the next merge". Value shapes: "a best changed
    // in the store between adoption and the merge is respected (nothing cached)"; 3: "the keeper holds and caches no best". The change is
    // seeded behind the keeper (no keeper call, no recorded call), as another writer would.
    @Test fun case7_aBestChangedInTheStoreBehindTheKeeperIsRespectedByTheNextMerge() {
        // (a) an own solve of 30 s while the store's best was lowered to 20 behind the keeper: min(20, 30) = 20 (a cached 50 would give 30)
        run {
            val r = GateRig()
            r.store.seedProgress(A, inProgress(0, 50))
            r.start()
            r.show(A) // adopts a best of 50
            r.keeper.setPuzzleRunning(true)
            r.tap()
            r.tickFor(30)
            r.keeper.solved(false)
            r.store.seedProgress(A, r.store.progress(A).copy(bestSeconds = 20)) // behind the keeper
            val merged = r.keeper.mergeInto(A, r.store.progress(A).copy(state = PuzzleState.SOLVED))
            assertEquals("(a) the best is min(stored 20, own 30), computed from the stored base", 20L, merged.bestSeconds)
        }
        // (b) no solve at all: a settled event after the best changed behind the keeper must not write the old best back
        run {
            val r = GateRig()
            r.store.seedProgress(A, inProgress(0, 50))
            r.start()
            r.show(A)
            r.keeper.setPuzzleRunning(true)
            r.tap()
            r.tickFor(5)
            r.store.seedProgress(A, r.store.progress(A).copy(bestSeconds = 20)) // behind the keeper
            val merged = r.keeper.mergeInto(A, r.store.progress(A))
            assertEquals("(b) the stored best 20 comes back untouched, not the adopted 50", 20L, merged.bestSeconds)
        }
        // (c) an own solve slower than a best set behind the keeper: the stored base wins
        run {
            val r = GateRig()
            r.store.seedProgress(A, inProgress(0, null))
            r.start()
            r.show(A)
            r.keeper.setPuzzleRunning(true)
            r.tap()
            r.tickFor(30)
            r.keeper.solved(false)
            r.store.seedProgress(A, solved(0, 10)) // somebody stored a best of 10
            val merged = r.keeper.mergeInto(A, r.store.progress(A))
            assertEquals("(c) min(stored 10, own 30)", 10L, merged.bestSeconds)
        }
    }

    // ---------------------------------------------------------------------------------------------------------------- (8)
    // Scaffolding list (8): "`puzzleShown`, `touch`, `solved`, `mergeInto` and `afterReset` make no store call when a flush is due"
    // (value shapes: "make no store call at all, whatever is due"; `afterReset` "writes nothing"). READING: "no store call" is read as
    // no WRITE call (a save or a reset): `afterReset` is specified to re-read `store.playTime()`, so reads cannot be forbidden.
    // The seam also lists `setVisible(true)` and `setPuzzleRunning(true)` under "Accounting only, never writes": they are checked too.
    @Test fun case8_theFiveAccountingInputsMakeNoStoreWriteWhenAFlushIsDue() {
        val inputs: List<Pair<String, (GateRig) -> Unit>> = listOf(
            "puzzleShown" to { r -> r.show(B) },
            "puzzleShown (same puzzle)" to { r -> r.show(A) },
            "touch(true)" to { r -> r.keeper.touch(true) },
            "touch(false)" to { r -> r.keeper.touch(false) },
            "solved(false)" to { r -> r.keeper.solved(false) },
            "solved(true)" to { r -> r.keeper.solved(true) },
            "mergeInto (the shown puzzle)" to { r -> r.keeper.mergeInto(A, r.store.progress(A)) },
            "mergeInto (another puzzle)" to { r -> r.keeper.mergeInto(B, r.store.progress(B)) },
            "afterReset" to { r -> r.keeper.afterReset() },
            "setVisible(true)" to { r -> r.keeper.setVisible(true) },
            "setPuzzleRunning(true)" to { r -> r.keeper.setPuzzleRunning(true) },
        )
        for ((name, input) in inputs) {
            val r = GateRig()
            r.store.seedProgress(A, inProgress(10, 41))
            r.store.seedProgress(B, inProgress(5, null))
            r.start()
            r.show(A)
            r.keeper.setPuzzleRunning(true)
            r.tap()
            r.pass(15_000) // 15 uncredited seconds: a flush is due at the next accounting
            val mark = r.store.mark()
            input(r)
            r.assertNoWrites("input $name with a flush due", mark)
        }
    }
}
