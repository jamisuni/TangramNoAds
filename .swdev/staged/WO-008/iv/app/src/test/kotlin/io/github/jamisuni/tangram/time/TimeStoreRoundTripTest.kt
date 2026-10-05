package io.github.jamisuni.tangram.time

import io.github.jamisuni.tangram.SessionHost
import io.github.jamisuni.tangram.browse.BrowseController
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.GameSettings
import io.github.jamisuni.tangram.contracts.progress.IProgressStore
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PlayTime
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.play.PlaySession
import io.github.jamisuni.tangram.settings.SettingsController
import io.github.jamisuni.tangram.store.JsonProgressStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

// decision DA-137: DECISION TEST (WO-008 T8c part iv, lands with TASK-068), no requirement token. Design 4.2, "What proves the fit":
// the keeper over a REAL `JsonProgressStore` on a temp folder - solve, flush, reopen a NEW store and keeper, read today, total,
// `puzzleSeconds` and `bestSeconds` back - the same through a Restart, a Retry and a reset. Rev 2 E1: the reset leg makes a flush due before
// Erase and wires `onReset` exactly as `AppViewModel` does (`{ time.afterReset(); controller.afterReset() }`, design 3 and seam row `app`),
// with the REAL `BrowseController` and `SettingsController` over a real store wrapped by a call recorder.
// Written against the design's seam rows: `SessionHost.time: PlayTimeKeeper?` (a settable property; `show` calls `puzzleShown` at its end,
// `capture` calls `mergeInto`), delivered by TASK-068; it compiles only after 068. Nothing sleeps: the clock is a manual `TimeSource`.
class TimeStoreRoundTripTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val library = PuzzleLibrary.packaged()
    private val puzzles = library.puzzles
    private val day = LocalDate.of(2026, 10, 5)

    /** A manual monotonic clock; only this test moves it. */
    private class RoundTripClock : TimeSource {
        private var now = 5_000_000L
        override fun nowMs(): Long = now
        override fun today(): LocalDate = LocalDate.of(2026, 10, 5)
        fun advance(ms: Long) { now += ms }
    }

    /** The real store wrapped by a recorder of every WRITE call, in order (a delegate: reads are not recorded). */
    private class CallRecorder(private val inner: IProgressStore) : IProgressStore by inner {
        val calls = ArrayList<String>()
        override fun saveProgress(puzzle: PuzzleId, progress: PuzzleProgress) { calls += "saveProgress:${puzzle.value}"; inner.saveProgress(puzzle, progress) }
        override fun savePlayTime(playTime: PlayTime) { calls += "savePlayTime"; inner.savePlayTime(playTime) }
        override fun saveSettings(settings: GameSettings) { calls += "saveSettings"; inner.saveSettings(settings) }
        override fun saveLastShownPuzzle(puzzle: PuzzleId) { calls += "saveLastShown:${puzzle.value}"; inner.saveLastShownPuzzle(puzzle) }
        override fun resetAllProgress() { calls += "resetAllProgress"; inner.resetAllProgress() }
    }

    /** The app's composition for time, as `AppViewModel` builds it after TASK-068 (only the pieces this test needs). */
    private class Chain(val dir: File, library: io.github.jamisuni.tangram.contracts.puzzle.IPuzzleLibrary, val clock: RoundTripClock) {
        val real = JsonProgressStore(dir)
        val store = CallRecorder(real)
        val keeper = PlayTimeKeeper(clock, store)
        val host = SessionHost({ p, changed -> PlaySession(puzzle = p, onChanged = changed) }) { s, pr -> s.restore(pr) }
        val controller = BrowseController(library, store, host)
        val settings = SettingsController(
            store = store,
            canOpen = { host.session?.isDragging != true },
            onReset = { keeper.afterReset(); controller.afterReset() }, // exactly as AppViewModel (design 3, rev 1)
        )

        init {
            host.time = keeper
            host.onChanged = controller::persist
        }

        fun tap() { keeper.touch(true); keeper.touch(false) }

        fun tickFor(seconds: Int) = repeat(seconds) { clock.advance(1_000); keeper.accrue() }
    }

    private fun onBoard(p: Puzzle, seconds: Long, best: Long?): PuzzleProgress {
        val pieces: Map<PieceId, PieceSave> = p.solution.take((p.solution.size - 1).coerceIn(1, 3)).associate {
            val pose = PieceGeometry.poseOf(it.piece, it.polygon) ?: error("no pose for ${it.piece}")
            it.piece to PieceSave.OnBoard(pose.at, pose.turn, pose.mirrored)
        }
        return PuzzleProgress(PuzzleState.IN_PROGRESS, pieces, seconds, best)
    }

    private fun seed(dir: File, vararg entries: Pair<Puzzle, PuzzleProgress>, shown: Puzzle = entries.first().first) {
        JsonProgressStore(dir).apply {
            for ((p, pr) in entries) saveProgress(p.id, pr)
            saveLastShownPuzzle(shown.id)
        }
    }

    private fun chain(dir: File): Chain = Chain(dir, library, RoundTripClock()).also {
        it.controller.start()
        it.keeper.setVisible(true)
    }

    // ---------------------------------------------------------------------------------------------- solve, flush, reopen
    @Test fun aSolveAndAFlushAreReadBackByANewStoreAndANewKeeper() {
        val dir = tmp.newFolder()
        val a = puzzles[0]
        seed(dir, a to onBoard(a, 10, null))
        val c = chain(dir)
        c.keeper.setPuzzleRunning(true)
        c.tap()
        c.tickFor(30)
        c.keeper.solved(false)
        c.controller.persist() // the settled event after the completing drop: capture -> mergeInto -> one saveProgress
        c.keeper.setPuzzleRunning(false)
        c.keeper.setVisible(false) // onStop: a stop point, flush before sync()
        c.real.sync()

        val reopened = JsonProgressStore(dir) // a new process reads the document the first one wrote
        val keeper2 = PlayTimeKeeper(RoundTripClock(), reopened)
        assertEquals("today", 30L, keeper2.todaySeconds)
        assertEquals("total", 30L, keeper2.totalSeconds)
        assertEquals(PlayTime(day, 30, 30), reopened.playTime())
        // the capture is the restored In-progress board (the real solve path needs `play`'s internal layout, so the solve is signalled to the
        // keeper directly); the seconds and the best are what the merge computed: 10 adopted + 30 counted, and an own solve is its own best
        val back = reopened.progress(a.id)
        assertEquals("the puzzle's seconds", 40L, back.puzzleSeconds)
        assertEquals("the best of an own solve", 40L, back.bestSeconds)
    }

    // ---------------------------------------------------------------------------------------------------------- Restart
    @Test fun aRestartStoresZeroSecondsKeepsTheBestAndTheOldAttemptIsNeverWrittenBack() {
        val dir = tmp.newFolder()
        val a = puzzles[0]
        seed(dir, a to onBoard(a, 25, 41))
        val c = chain(dir)
        c.keeper.setPuzzleRunning(true)
        c.tap()
        c.tickFor(12) // a flush has fallen due and been written: the record holds the running seconds
        c.controller.restart()
        assertEquals("the new attempt starts at 0", 0L, c.keeper.puzzleSeconds)
        c.keeper.setPuzzleRunning(false) // New: the gate turns the puzzle clock off, a stop point
        c.tickFor(15) // time passes on the New puzzle: today and total run, the puzzle does not
        c.keeper.setVisible(false)
        c.real.sync()

        val reopened = JsonProgressStore(dir)
        val back = reopened.progress(a.id)
        assertEquals(PuzzleState.NEW, back.state)
        assertEquals("the restarted record holds no seconds of the old attempt", 0L, back.puzzleSeconds)
        assertEquals("the best time is kept by a Restart", 41L, back.bestSeconds)
        assertTrue("play time kept running through the restart", reopened.playTime().totalSeconds >= 27)
        val keeper2 = PlayTimeKeeper(RoundTripClock(), reopened)
        keeper2.puzzleShown(a.id, back)
        assertEquals(0L, keeper2.puzzleSeconds)
    }

    // ------------------------------------------------------------------------------------------------------------ Retry
    @Test fun aRetryOfASolvedPuzzleStoresZeroSecondsAndKeepsTheBest() {
        val dir = tmp.newFolder()
        val a = puzzles[0]
        seed(dir, a to PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 47, 41))
        val c = chain(dir)
        assertEquals(PuzzleState.SOLVED, c.host.state)
        c.tap()
        c.tickFor(5)
        c.controller.restart() // Retry
        c.keeper.setVisible(false)
        c.real.sync()
        val back = JsonProgressStore(dir).progress(a.id)
        assertEquals(PuzzleState.NEW, back.state)
        assertEquals(0L, back.puzzleSeconds)
        assertEquals("the best time survives the Retry", 41L, back.bestSeconds)
    }

    // ------------------------------------------------------------------------------------------------- reset with a flush due
    // Rev 2 E1: a flush is due when Erase is confirmed; between `resetAllProgress()` and the end of `onReset` the keeper and the
    // controller write NOTHING; the store reads as erased afterwards, and only post-reset seconds are ever stored again.
    // Played twice: Erase by tap (the touch accounts first) and Erase with no touch (keyboard or an accessibility click), when the
    // flush that fell due is still due at the reset.
    @Test fun aResetWithAFlushDueWritesNothingBetweenTheEraseAndTheEndOfOnResetAndStoresOnlyPostResetSeconds() {
        for (tapOnErase in listOf(true, false)) {
            val tag = if (tapOnErase) "Erase by tap" else "Erase without a touch"
            val dir = tmp.newFolder()
            val a = puzzles[0]
            val b = puzzles[1]
            seed(dir, a to onBoard(a, 10, null), b to PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 47, 41), shown = a)
            JsonProgressStore(dir).saveSettings(GameSettings(timerShown = true, soundOn = false)) // settings are kept by a reset (DA-120)
            val c = chain(dir)
            c.keeper.setPuzzleRunning(true)
            c.tap()
            c.clock.advance(12_000); c.keeper.accrue(); c.keeper.flush() // the store holds a play time now: something to erase
            assertEquals("[$tag] fixture: a play time is stored", 12L, c.real.playTime().totalSeconds)
            c.clock.advance(15_000) // a flush is due now, with no input since
            c.settings.open()
            c.settings.requestReset()
            if (tapOnErase) c.tap()

            c.settings.confirmReset()
            val resetAt = c.store.calls.indexOf("resetAllProgress")
            assertTrue("[$tag] the reset reached the store: ${c.store.calls}", resetAt >= 0)
            val between = c.store.calls.drop(resetAt + 1)
            assertTrue("[$tag] writes between resetAllProgress() and the end of onReset: $between", between.isEmpty())
            assertEquals("[$tag] play time is erased", PlayTime.NONE, JsonProgressStore(dir).playTime())
            assertEquals("[$tag] the shown puzzle is New again", PuzzleProgress.NEW, JsonProgressStore(dir).progress(a.id))
            assertEquals("[$tag] the solved puzzle's best is erased", PuzzleProgress.NEW, JsonProgressStore(dir).progress(b.id))
            assertEquals("[$tag] the settings are kept", GameSettings(timerShown = true, soundOn = false), JsonProgressStore(dir).settings())
            assertEquals("[$tag] today", 0L, c.keeper.todaySeconds)
            assertEquals("[$tag] total", 0L, c.keeper.totalSeconds)

            // life goes on: 12 more touched seconds, then the app stops; only those are stored
            c.keeper.setPuzzleRunning(false) // New: the puzzle clock is off
            c.tap()
            c.tickFor(12)
            c.keeper.setVisible(false)
            c.real.sync()
            val after = JsonProgressStore(dir)
            assertEquals("[$tag] only the post-reset seconds are stored", PlayTime(day, 12, 12), after.playTime())
            assertEquals("[$tag] the reopened keeper reads them", 12L, PlayTimeKeeper(RoundTripClock(), after).totalSeconds)
        }
    }
}
