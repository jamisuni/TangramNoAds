package io.github.jamisuni.tangram.kernel.state

import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.PuzzleState.IN_PROGRESS
import io.github.jamisuni.tangram.kernel.model.PuzzleState.NEW
import io.github.jamisuni.tangram.kernel.model.PuzzleState.SOLVED
import org.junit.Assert.assertEquals
import org.junit.Test

/** SCAFFOLDING (TASK-022, disposable): decision DA-59, TYPE-006 restart and retry. No acceptance IDs. */
class RestartRetryScaffoldingTest {
    @Test
    fun restartTable() { // decision DA-59
        assertEquals(NEW, PuzzleStates.onRestart(NEW))
        assertEquals(NEW, PuzzleStates.onRestart(IN_PROGRESS))
        assertEquals(SOLVED, PuzzleStates.onRestart(SOLVED))
    }

    @Test
    fun retryTable() { // decision DA-59
        assertEquals(NEW, PuzzleStates.onRetry(NEW))
        assertEquals(IN_PROGRESS, PuzzleStates.onRetry(IN_PROGRESS))
        assertEquals(NEW, PuzzleStates.onRetry(SOLVED))
    }

    @Test
    fun everyStateStaysAState() { // decision DA-59
        for (s in PuzzleState.values()) {
            PuzzleStates.onRestart(s)
            PuzzleStates.onRetry(s)
        }
    }
}
