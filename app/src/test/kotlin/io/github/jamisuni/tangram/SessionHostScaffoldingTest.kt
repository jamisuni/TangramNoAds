package io.github.jamisuni.tangram

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.play.PlaySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

// SCAFFOLDING (disposable): code review WO-004 F3/F4, CR-2 F9 (G-10). No REQ token: this pins a guardrail, not a criterion.
class SessionHostScaffoldingTest {
    private val puzzle = PuzzleLibrary.packaged().puzzles.first()
    private val real: (io.github.jamisuni.tangram.contracts.puzzle.Puzzle, () -> Unit) -> PlaySession =
        { p, changed -> PlaySession(puzzle = p, onChanged = changed) }

    @Test
    fun restoreFailureShowsANewSessionOfThatPuzzle() {
        val host = SessionHost(real) { _, _ -> error("damaged") }
        host.show(puzzle, PuzzleProgress.NEW)
        assertNotNull(host.session)
        assertEquals(puzzle, host.session!!.puzzle)
        assertEquals(PuzzleState.NEW, host.state)
    }

    @Test
    fun buildFailureShowsNothingWithoutThrowingAndCaptureIsNull() {
        val host = SessionHost(real) { _, _ -> }
        host.show(puzzle, PuzzleProgress.NEW)
        assertNotNull(host.session)
        val failing = SessionHost({ _, _ -> error("cannot build") }) { _, _ -> }
        failing.show(puzzle, PuzzleProgress.NEW)
        assertNull(failing.session)
        assertNull(failing.capture(PuzzleProgress.NEW))
        assertEquals(PuzzleState.NEW, failing.state)
    }

    @Test
    fun aLaterFailedBuildDropsThePreviousPuzzlesSession() {
        var fail = false
        val host = SessionHost({ p, c -> if (fail) error("x") else real(p, c) }) { s, pr -> s.restore(pr) }
        host.show(puzzle, PuzzleProgress.NEW)
        fail = true
        host.show(puzzle, PuzzleProgress.NEW)
        assertNull(host.session)
        assertNull(host.capture(PuzzleProgress.NEW))
    }
}
