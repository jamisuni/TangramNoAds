package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.content.PuzzleLibrary
import org.junit.Assert.assertEquals
import org.junit.Test

// SCAFFOLDING (disposable, TASK-018b): the session keeps the last frame time so a rebuilt composition seeds its clock.
class LastFrameScaffoldingTest {
    @Test fun onFrameRecordsTheFrameClock() {
        val s = PlaySession(PuzzleLibrary.packaged().puzzles.first())
        assertEquals(0L, s.lastFrameMs)
        s.onFrame(12_345)
        assertEquals(12_345L, s.lastFrameMs)
    }
}
