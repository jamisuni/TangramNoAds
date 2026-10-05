package io.github.jamisuni.tangram

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.store.JsonProgressStore
import io.github.jamisuni.tangram.time.PlayTimeKeeper
import io.github.jamisuni.tangram.time.TimeSource
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

// SCAFFOLDING (disposable): WO-008 TASK-068 wiring of SessionHost.time. No acceptance token on purpose.
class SessionHostTimeWiringTest {
    @get:Rule val folder = TemporaryFolder()

    private class Manual : TimeSource {
        var now = 1_000L
        override fun nowMs() = now
        override fun today(): LocalDate = LocalDate.of(2026, 10, 5)
    }

    private val puzzle = PuzzleLibrary.packaged().puzzles.first()
    private val inProgress = PuzzleProgress(PuzzleState.IN_PROGRESS, emptyMap(), 12, null)

    private fun rig(): Triple<SessionHost, PlayTimeKeeper, Manual> {
        val clock = Manual()
        val keeper = PlayTimeKeeper(clock, JsonProgressStore(folder.newFolder()))
        val host = SessionHost(reducedMotion = { false })
        host.time = keeper
        return Triple(host, keeper, clock)
    }

    @Test
    fun showAdoptsTheStoredSeconds() {
        val (host, keeper, _) = rig()
        host.show(puzzle, inProgress)
        assertEquals(12L, keeper.puzzleSeconds)
    }

    @Test
    fun captureMergesTheKeepersSeconds() {
        val (host, keeper, clock) = rig()
        host.show(puzzle, inProgress)
        keeper.setVisible(true)
        keeper.touch(false)
        keeper.setPuzzleRunning(true)
        clock.now += 5_000
        keeper.accrue()
        assertEquals(17L, host.capture(inProgress)!!.puzzleSeconds)
    }

    @Test
    fun theSolvedListenerReachesTheKeeper() {
        val (host, keeper, _) = rig()
        host.show(puzzle, inProgress)
        host.session!!.solvedListener(false)
        // an own solve leaves a pending result, which the merge turns into a best (12 s adopted, none counted)
        assertEquals(12L, keeper.mergeInto(puzzle.id, inProgress).bestSeconds)
    }

    @Test
    fun anAidSolveSetsNoBest() {
        val (host, keeper, _) = rig()
        host.show(puzzle, inProgress)
        host.session!!.solvedListener(true)
        assertEquals(null, keeper.mergeInto(puzzle.id, inProgress).bestSeconds)
    }

    @Test
    fun withoutAKeeperNothingChanges() {
        val host = SessionHost(reducedMotion = { false })
        host.show(puzzle, inProgress)
        assertEquals(12L, host.capture(inProgress)!!.puzzleSeconds)
    }
}
