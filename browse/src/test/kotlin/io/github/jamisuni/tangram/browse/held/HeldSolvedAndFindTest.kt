package io.github.jamisuni.tangram.browse.held

import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** HELD-OUT (Test & Verify only): REQ-026 A1, REQ-050 A1 and A3 in `browse`. */
class HeldSolvedAndFindTest {

    private val puzzles = BrowseKit.puzzles
    private val n = puzzles.size
    private val seven = puzzles.indexOfFirst { it.solution.size == 7 }
    private val solvedEntry = BrowseKit.progress(PuzzleState.SOLVED, emptyMap(), puzzleSeconds = 47, best = 41)

    // ------------------------------------------------------------------------------------------ REQ-026.A1

    // REQ-026.A1 - "A solved puzzle never shows where the pieces were."
    // Design table (F6, real `browse` code): a host that reports a SOLVED session WITH all its pieces gives a store entry with no pieces.
    @Test
    fun req026_A1_aSolvedCaptureIsStoredWithoutAnyPiecePosition() {
        val rig = BrowseKit.Rig()
        rig.controller.open(seven)
        rig.host.state = PuzzleState.SOLVED
        rig.host.pieces = BrowseKit.fullSolution(puzzles[seven])
        rig.controller.next() // leaving saves it
        val stored = rig.store.progress(BrowseKit.id(seven))
        assertEquals(PuzzleState.SOLVED, stored.state)
        assertEquals(emptyMap<PieceId, PieceSave>(), stored.pieces)
        // also through persist() directly, and for tray entries
        rig.controller.previous()
        rig.host.state = PuzzleState.SOLVED
        rig.host.pieces = BrowseKit.fullSolution(puzzles[seven]) + (PieceId.LT1 to PieceSave.InTray(Turn(3), true))
        rig.controller.persist()
        assertEquals(emptyMap<PieceId, PieceSave>(), rig.store.progress(BrowseKit.id(seven)).pieces)
    }

    // REQ-026.A1 - and a stored SOLVED entry that still has pieces (older data) reaches the host with an empty map, on start and on open.
    @Test
    fun req026_A1_aStoredSolvedEntryWithPiecesReachesTheHostWithoutThem() {
        val id = BrowseKit.id(seven)
        val withPieces = PuzzleProgress(PuzzleState.SOLVED, BrowseKit.fullSolution(puzzles[seven]), 47, 41)
        val rig = BrowseKit.Rig { seed(id, withPieces); seedLastShown(id) }
        assertEquals(PuzzleState.SOLVED, rig.host.lastShown.progress.state)
        assertEquals(emptyMap<PieceId, PieceSave>(), rig.host.lastShown.progress.pieces)

        rig.controller.open(0)
        rig.controller.open(seven)
        assertEquals(PuzzleState.SOLVED, rig.host.lastShown.progress.state)
        assertEquals(emptyMap<PieceId, PieceSave>(), rig.host.lastShown.progress.pieces)
        // the best time is not lost on the way
        assertEquals(41L, rig.store.progress(id).bestSeconds)
    }

    // ------------------------------------------------------------------------------------------ REQ-050.A1

    private fun rigWithUnsolved(shown: Int, vararg unsolved: Int, unsolvedState: PuzzleState = PuzzleState.NEW): BrowseKit.Rig =
        BrowseKit.Rig {
            for ((i, p) in puzzles.withIndex()) {
                if (i !in unsolved) seed(p.id, solvedEntry)
                else if (unsolvedState != PuzzleState.NEW) seed(p.id, BrowseKit.progress(unsolvedState))
            }
            seedLastShown(BrowseKit.id(shown))
        }

    // REQ-050.A1 - "With puzzles 3 and 7 unsolved and puzzle 3 open, a long press on › opens puzzle 7."
    // Puzzles are numbered from 1: indices 2 and 6.
    @Test
    fun req050_A1_withPuzzles3And7UnsolvedAndPuzzle3OpenNextUnsolvedOpensPuzzle7() {
        val rig = rigWithUnsolved(2, 2, 6)
        assertEquals(2, rig.controller.index)
        assertEquals(PuzzleState.NEW, rig.host.state)
        rig.controller.nextUnsolved()
        assertEquals(6, rig.controller.index)
        assertEquals(puzzles[6].id, rig.host.lastShown.puzzle.id)
    }

    // REQ-050.A1 - the next unsolved AFTER the current one, not the first unsolved of the list: 1, 3 and 7 unsolved, 3 open gives 7.
    @Test
    fun req050_A1_itSearchesAfterTheCurrentPuzzleNotFromTheStart() {
        val rig = rigWithUnsolved(2, 0, 2, 6)
        rig.controller.nextUnsolved()
        assertEquals(6, rig.controller.index)
    }

    // REQ-050.A1 - it wraps past the end, never stays on the current puzzle while another is unsolved.
    @Test
    fun req050_A1_itWrapsAndExcludesTheCurrentPuzzle() {
        val rig = rigWithUnsolved(6, 2, 6)
        rig.controller.nextUnsolved()
        assertEquals(2, rig.controller.index)
    }

    // REQ-050.A1 - a puzzle in progress counts as unsolved, and so does a Retried one (New).
    @Test
    fun req050_A1_inProgressPuzzlesCountAsUnsolved() {
        val rig = rigWithUnsolved(2, 2, 4, 6, unsolvedState = PuzzleState.IN_PROGRESS)
        rig.host.state = PuzzleState.NEW
        rig.controller.nextUnsolved()
        assertEquals(4, rig.controller.index)
    }

    // REQ-050 rule 1 (prose, not an acceptance claim; DA-54): when every puzzle is solved the long press acts like a normal press.
    // The shown puzzle's state comes from the host (the session), the others from the store.
    @Test
    fun decisionDA54_whenEveryPuzzleIsSolvedItIsANormalNext() {
        val rig = rigWithUnsolved(2)
        rig.host.state = PuzzleState.SOLVED
        rig.controller.nextUnsolved()
        assertEquals(3, rig.controller.index)
        // and from the last puzzle it wraps like next()
        val atLast = rigWithUnsolved(n - 1)
        atLast.host.state = PuzzleState.SOLVED
        atLast.controller.nextUnsolved()
        assertEquals(0, atLast.controller.index)
    }

    // decision DA-54: with the current puzzle the only unsolved one it falls back to a normal next.
    @Test
    fun decisionDA54_aLoneUnsolvedCurrentPuzzleFallsBackToNext() {
        val rig = rigWithUnsolved(5, 5)
        rig.controller.nextUnsolved()
        assertEquals(6, rig.controller.index)
    }

    // ------------------------------------------------------------------------------------------ REQ-050.A3

    // REQ-050.A3 - "Choosing a cell opens that puzzle without changing any other puzzle's state."
    // Design table: `open(j)` writes only the leaving puzzle and `lastShown`; every other puzzle's stored progress is equal
    // before and after - including a stale-looking IN_PROGRESS entry (a displaced piece) that sanitizing would change, a Solved
    // entry, and a New entry with a turned tray piece.
    @Test
    fun req050_A3_openingACellWritesOnlyTheLeavingPuzzleAndTheLastShownId() {
        val leaving = 1
        check(seven > leaving + 0 && seven + 1 < n) { "fixture: the library layout changed" }
        val full = BrowseKit.fullSolution(puzzles[seven])
        // an IN_PROGRESS entry with a piece far outside the silhouette: sanitizing would move it to the tray, so a write-back shows
        val farOff = PieceSave.OnBoard(ExactPoint(Q2.of(1000), Q2.of(1000)), Turn(0), false)
        val stale = full + (PieceId.ST2 to farOff)
        val ownPiece = puzzles[leaving].solution.first().piece
        val rig = BrowseKit.Rig {
            seed(BrowseKit.id(0), BrowseKit.progress(PuzzleState.SOLVED, emptyMap(), 47, 41))
            seed(BrowseKit.id(leaving), BrowseKit.progress(PuzzleState.NEW, mapOf(ownPiece to PieceSave.InTray(Turn(3), true))))
            seed(BrowseKit.id(seven), BrowseKit.progress(PuzzleState.IN_PROGRESS, stale, 12, 9))
            seed(BrowseKit.id(seven + 1), BrowseKit.progress(PuzzleState.SOLVED, emptyMap(), 5, 5))
            seedLastShown(BrowseKit.id(leaving))
        }
        val before = rig.store.snapshot()
        rig.store.clearWriteLog()

        rig.controller.open(seven)

        assertEquals(seven, rig.controller.index)
        assertEquals("only the leaving puzzle may be written", true, rig.store.progressWrites.all { it.first == BrowseKit.id(leaving) })
        assertEquals(listOf(BrowseKit.id(seven)), rig.store.lastShownWrites)
        val after = rig.store.snapshot()
        assertEquals(before.keys, after.keys)
        for ((id, progress) in before) assertEquals("progress of ${id.value} changed", progress, after[id])
    }

    // REQ-050.A3 - the same for a run of choices (grid cells in any order): untouched puzzles stay equal.
    @Test
    fun req050_A3_aRunOfChoicesLeavesEveryUntouchedPuzzleAsItWas() {
        val rig = BrowseKit.Rig {
            seed(BrowseKit.id(2), BrowseKit.progress(PuzzleState.SOLVED, emptyMap(), 30, 30))
            seed(BrowseKit.id(4), BrowseKit.progress(PuzzleState.NEW, mapOf(PieceId.LT1 to PieceSave.InTray(Turn(1), true))))
        }
        val untouched = listOf(2, 4)
        val before = untouched.associateWith { rig.store.progress(BrowseKit.id(it)) }
        for (j in listOf(6, 0, 7, 1, 8, 3)) rig.controller.open(j)
        for ((i, p) in before) {
            // 2 and 4 were never shown in this run except through the list above; compare the stored values
            if (i in listOf(6, 0, 7, 1, 8, 3)) continue
            assertEquals("puzzle ${i + 1} changed", p, rig.store.progress(BrowseKit.id(i)))
        }
        assertFalse(rig.store.progressWrites.any { it.first == BrowseKit.id(2) || it.first == BrowseKit.id(4) })
    }
}
