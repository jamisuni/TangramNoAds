package io.github.jamisuni.tangram.browse

import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.IPuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleKind
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `BrowseController` navigation (design WO-004 section 5 and "Acceptance IDs and where the covering test lives"),
 * with the fake store and host. Real puzzles through test-scope `content`.
 */
class NavigationTest {

    private val puzzles = BrowseKit.puzzles
    private val n = puzzles.size

    private fun rigAt(i: Int, seed: FakeProgressStore.() -> Unit = {}) = BrowseKit.Rig { seedLastShown(BrowseKit.id(i)); seed() }

    // REQ-003.A1 - "From any puzzle, every other puzzle can be reached without solving anything."
    // (REQ-003 rule: "No puzzle is locked behind another puzzle.") Every ordered pair (i, j), all puzzles New.
    @Test
    fun req003_A1_everyPuzzleReachesEveryOtherWithAllPuzzlesNew() {
        assertTrue("fixture: the library has several puzzles", n >= 5)
        for (i in 0 until n) {
            for (j in 0 until n) {
                val rig = rigAt(i)
                assertEquals("fixture: started at $i", i, rig.controller.index)
                rig.controller.open(j)
                assertEquals("open($j) from $i", j, rig.controller.index)
                assertEquals(puzzles[j].id, rig.host.lastShown.puzzle.id)
                assertEquals(PuzzleState.NEW, rig.controller.stateOf(j))
            }
        }
    }

    // REQ-003.A1 - the same, by the next button alone: it walks the whole list in order, nothing solved.
    @Test
    fun req003_A1_nextWalksTheWholeListWithoutSolvingAnything() {
        val rig = BrowseKit.Rig()
        val seen = mutableListOf(rig.controller.index)
        repeat(n - 1) {
            rig.controller.next()
            seen += rig.controller.index
        }
        assertEquals((0 until n).toList(), seen)
        assertTrue(rig.host.shown.all { it.progress.state == PuzzleState.NEW })
    }

    // REQ-024.A1 - "› works on an unsolved puzzle."  (REQ-024 statement: "whether or not the current one is solved.")
    // From New and from In progress, and (statement) from Solved: the next puzzle is shown.
    @Test
    fun req024_A1_nextWorksFromNewInProgressAndSolved() {
        for (state in PuzzleState.entries) {
            val rig = rigAt(1)
            rig.host.state = state
            rig.controller.next()
            assertEquals("next() from $state", 2, rig.controller.index)
            assertEquals(puzzles[2].id, rig.host.lastShown.puzzle.id)
        }
    }

    // REQ-024.A3 - "› on the last puzzle shows the first."  (rule: "‹ on the first puzzle shows the last".)
    @Test
    fun req024_A3_theListWrapsInBothDirections() {
        val atLast = rigAt(n - 1)
        atLast.controller.next()
        assertEquals(0, atLast.controller.index)
        assertEquals(puzzles.first().id, atLast.host.lastShown.puzzle.id)

        val atFirst = rigAt(0)
        atFirst.controller.previous()
        assertEquals(n - 1, atFirst.controller.index)
        assertEquals(puzzles.last().id, atFirst.host.lastShown.puzzle.id)

        // a full lap in either direction returns to the start
        val lap = rigAt(2)
        repeat(n) { lap.controller.next() }
        assertEquals(2, lap.controller.index)
        repeat(n) { lap.controller.previous() }
        assertEquals(2, lap.controller.index)
    }

    // REQ-045.A1 - "A fresh install opens on a mini puzzle whose tray holds three pieces."
    // Design acceptance table row `start()` (i): an empty store gives index 0 and show(first, NEW). The tray holds the
    // puzzle's own pieces (REQ-045 statement), so the shown puzzle must be a mini of three pieces with rating 1.
    @Test
    fun req045_A1_aFreshInstallStartsOnTheFirstPuzzleWhichIsATriplePieceMini() {
        val store = FakeProgressStore()
        val host = FakeHost()
        val controller = BrowseController(BrowseKit.library, store, host)
        controller.start()
        assertEquals(0, controller.index)
        assertEquals(1, host.shown.size)
        assertEquals(puzzles.first(), host.shown.single().puzzle)
        assertEquals(PuzzleProgress.NEW, host.shown.single().progress)
        assertEquals(PuzzleKind.MINI, puzzles.first().kind)
        assertEquals(3, puzzles.first().solution.size)
        assertEquals(1, puzzles.first().rating)
    }

    // decision F4 (start rows iii and iv) and the lastShown relaunch rule.
    @Test
    fun decisionF4_startOpensTheLastShownPuzzleIgnoresAnUnknownOneAndWritesNothing() {
        val known = rigAt(4)
        assertEquals(4, known.controller.index)
        assertEquals(puzzles[4].id, known.host.lastShown.puzzle.id)

        val unknown = BrowseKit.Rig { seedLastShown(PuzzleId("a-puzzle-an-update-removed")) }
        assertEquals("(iii) an id the library lost is ignored", 0, unknown.controller.index)

        assertFalse("(iv) start() writes nothing", known.store.anyWrites)
        assertFalse("(iv) start() writes nothing", unknown.store.anyWrites)
        assertEquals("start() shows exactly once", 1, known.host.shown.size)
    }

    // decision F5 / design section 5 "Every controller entry a control can reach is guarded".
    @Test
    fun decisionF5_everyEntryIsANoOpWhileAPieceIsDragged() {
        val rig = rigAt(3)
        val one = BrowseKit.fullSolution(puzzles[3]).entries.first()
        rig.host.play(mapOf(one.key to one.value))
        rig.host.isDragging = true
        rig.store.clearWriteLog()
        val shownBefore = rig.host.shown.size

        rig.controller.next()
        rig.controller.previous()
        rig.controller.nextUnsolved()
        rig.controller.open(0)
        rig.controller.restart()
        rig.controller.openGrid()

        assertEquals(3, rig.controller.index)
        assertFalse(rig.controller.gridOpen)
        assertEquals(shownBefore, rig.host.shown.size)
        assertFalse("nothing is written while dragging", rig.store.anyWrites)

        rig.host.isDragging = false
        rig.controller.openGrid()
        assertTrue(rig.controller.gridOpen)
        rig.controller.closeGrid()
        assertFalse(rig.controller.gridOpen)
        rig.controller.next()
        assertEquals(4, rig.controller.index)
    }

    // design section 5 `open(i)`: no-op out of range or for the shown puzzle; nothing is written then.
    @Test
    fun decisionF5_openIgnoresOutOfRangeAndTheShownIndex() {
        val rig = rigAt(2)
        rig.store.clearWriteLog()
        val shownBefore = rig.host.shown.size
        rig.controller.open(-1)
        rig.controller.open(n)
        rig.controller.open(2)
        assertEquals(2, rig.controller.index)
        assertEquals(shownBefore, rig.host.shown.size)
        assertFalse(rig.store.anyWrites)
    }

    // decision DA-57: an empty library is a packaging error that fails with a clear check, not an opaque exception.
    @Test
    fun decisionDA57_anEmptyLibraryFailsWithACheck() {
        val empty = object : IPuzzleLibrary {
            override val puzzles: List<Puzzle> = emptyList()
            override fun puzzle(id: PuzzleId): Puzzle? = null
        }
        assertThrows(IllegalStateException::class.java) {
            BrowseController(empty, FakeProgressStore(), FakeHost()).start()
        }
    }

    // design section 5 member table: `current` follows `index`; `shownState` reads the host; `stateOf` reads the store for the rest.
    @Test
    fun decisionDA51_currentFollowsTheIndexAndShownStateFollowsTheHost() {
        val rig = rigAt(5)
        assertEquals(puzzles[5], rig.controller.current)
        assertEquals(puzzles, rig.controller.puzzles)
        rig.host.state = PuzzleState.IN_PROGRESS
        assertEquals(PuzzleState.IN_PROGRESS, rig.controller.shownState)
        assertEquals("stateOf(shown) reads the host", PuzzleState.IN_PROGRESS, rig.controller.stateOf(5))
        assertEquals("stateOf(other) reads the store", PuzzleState.NEW, rig.controller.stateOf(6))
    }
}
