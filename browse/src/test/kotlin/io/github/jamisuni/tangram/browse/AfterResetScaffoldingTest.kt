package io.github.jamisuni.tangram.browse

// SCAFFOLDING (implementer's disposable tests, TASK-055). decision DA-120. No acceptance tokens on purpose.

import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AfterResetScaffoldingTest {

    private fun rig(): Triple<BrowseController, ScaffoldStore, ScaffoldHost> {
        val store = ScaffoldStore()
        val host = ScaffoldHost()
        val c = BrowseController(ScaffoldLibrary(BrowseKit.puzzles), store, host)
        c.start()
        c.next()
        return Triple(c, store, host)
    }

    @Test fun afterResetKeepsIndexAndShowsNew() {
        val (c, store, host) = rig()
        store.map[c.current.id] = PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 5, 5)
        store.map.clear() // what the store's reset did
        val index = c.index
        c.afterReset()
        assertEquals(index, c.index)
        assertEquals(PuzzleState.NEW, c.shownState)
        assertEquals(c.current, host.shown.last().first)
        c.afterReset() // idempotent
        assertEquals(index, c.index)
        assertEquals(PuzzleState.NEW, c.shownState)
    }

    @Test fun afterResetWritesNothingAndKeepsLastShown() {
        val (c, store, _) = rig()
        val lastBefore = store.lastShown
        val writesBefore = store.writes.toList()
        c.afterReset()
        c.afterReset()
        assertEquals(writesBefore, store.writes)
        assertEquals(lastBefore, store.lastShown)
        assertTrue(store.writes.none { it.startsWith("progress:") && it !in writesBefore })
    }
}
