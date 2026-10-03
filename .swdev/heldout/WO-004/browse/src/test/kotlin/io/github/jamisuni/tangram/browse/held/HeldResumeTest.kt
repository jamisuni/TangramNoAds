package io.github.jamisuni.tangram.browse.held

import io.github.jamisuni.tangram.browse.BrowseController
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** HELD-OUT (Test & Verify only): REQ-025 A1 and A2 in `browse`, with the held-out adapters of this package. */
class HeldResumeTest {

    private val puzzles = BrowseKit.puzzles
    private val seven = puzzles.indexOfFirst { it.solution.size == 7 }

    /** Three pieces in their solution poses plus a turned and mirrored piece still in the tray. */
    private fun savedState(): Map<PieceId, PieceSave> {
        val p = puzzles[seven]
        val ids = p.solution.map { it.piece }
        val board = ids.take(3).associateWith { BrowseKit.solutionSave(p, it) }
        val turnedTray = ids[3] to PieceSave.InTray(Turn(3), true)
        return board + turnedTray
    }

    // REQ-025.A1 - "Leave a puzzle with three pieces placed, come back: the same three pieces are in the same places."
    // Closing the app: a second controller on the same store (what a new process sees) shows exactly the saved pieces, the exact
    // positions, turns and mirrors included, and the turned tray piece keeps its turn.
    @Test
    fun req025_A1_threePlacedPiecesComeBackInTheSamePlacesAfterARelaunch() {
        assertTrue("fixture: a 7-piece puzzle", seven >= 0)
        val first = BrowseKit.Rig()
        first.controller.open(seven)
        val saved = savedState()
        first.host.play(saved)
        first.controller.persist() // what leaving or onPause does

        val hostAfter = FakeHost()
        val second = BrowseController(BrowseKit.library, first.store, hostAfter)
        second.start()

        assertEquals("a relaunch opens the last shown puzzle", seven, second.index)
        val back = hostAfter.lastShown
        assertEquals(puzzles[seven].id, back.puzzle.id)
        assertEquals(PuzzleState.IN_PROGRESS, back.progress.state)
        assertEquals(saved, back.progress.pieces)
    }

    // REQ-025.A1 - and by leaving to another puzzle that is also in progress and coming back to each.
    @Test
    fun req025_A1_twoPuzzlesInProgressEachComeBackWithTheirOwnPieces() {
        val other = puzzles.indices.first { it != seven && puzzles[it].solution.size == 7 }
        val rig = BrowseKit.Rig()
        rig.controller.open(seven)
        val a = BrowseKit.fullSolution(puzzles[seven]).entries.take(3).associate { it.key to it.value }
        rig.host.play(a)
        rig.controller.open(other)
        val b = BrowseKit.fullSolution(puzzles[other]).entries.drop(2).take(2).associate { it.key to it.value }
        rig.host.play(b)

        rig.controller.open(seven)
        assertEquals(a, rig.host.lastShown.progress.pieces)
        rig.controller.open(other)
        assertEquals(b, rig.host.lastShown.progress.pieces)
    }

    // REQ-025.A2 - "After Restart, all the puzzle's pieces are in the tray and the puzzle time is zero."
    // Design table: restart() leaves the store entry New, pieces empty, puzzleSeconds 0, best time kept (REQ-030 via the contract
    // KDoc of PuzzleProgress.restarted). The puzzle is shown again from that entry, on the same index.
    @Test
    fun req025_A2_restartPutsEveryPieceBackInTheTrayAndZeroesTheTimeKeepingTheBest() {
        val id = BrowseKit.id(seven)
        val three = BrowseKit.fullSolution(puzzles[seven]).entries.take(3).associate { it.key to it.value }
        val rig = BrowseKit.Rig {
            seed(id, BrowseKit.progress(PuzzleState.IN_PROGRESS, three, puzzleSeconds = 77, best = 40))
            seedLastShown(id)
        }
        assertEquals(PuzzleState.IN_PROGRESS, rig.host.state)
        assertEquals(three, rig.host.lastShown.progress.pieces)
        val shownBefore = rig.host.shown.size

        rig.controller.restart()

        val stored = rig.store.progress(id)
        assertEquals(PuzzleState.NEW, stored.state)
        assertEquals(emptyMap<PieceId, PieceSave>(), stored.pieces)
        assertEquals(0L, stored.puzzleSeconds)
        assertEquals(40L, stored.bestSeconds)
        assertEquals(seven, rig.controller.index)
        assertEquals(shownBefore + 1, rig.host.shown.size)
        assertEquals(PuzzleState.NEW, rig.host.lastShown.progress.state)
        assertEquals(emptyMap<PieceId, PieceSave>(), rig.host.lastShown.progress.pieces)
        assertEquals(id, rig.store.lastShownPuzzle())
    }

    // REQ-025.A2 - Restart discards the pieces of the live session: it must not first save them and then reset the entry.
    @Test
    fun req025_A2_restartDiscardsPiecesThatWereNeverSaved() {
        val rig = BrowseKit.Rig()
        rig.controller.open(seven)
        rig.host.play(BrowseKit.fullSolution(puzzles[seven]).entries.take(2).associate { it.key to it.value })
        rig.controller.restart()
        assertEquals(PuzzleState.NEW, rig.store.progress(BrowseKit.id(seven)).state)
        assertEquals(emptyMap<PieceId, PieceSave>(), rig.store.progress(BrowseKit.id(seven)).pieces)
        assertEquals(emptyMap<PieceId, PieceSave>(), rig.host.lastShown.progress.pieces)
        // and a relaunch sees the empty puzzle too
        val hostAfter = FakeHost()
        BrowseController(BrowseKit.library, rig.store, hostAfter).also { it.start() }
        assertEquals(PuzzleState.NEW, hostAfter.lastShown.progress.state)
        assertEquals(emptyMap<PieceId, PieceSave>(), hostAfter.lastShown.progress.pieces)
    }
}
