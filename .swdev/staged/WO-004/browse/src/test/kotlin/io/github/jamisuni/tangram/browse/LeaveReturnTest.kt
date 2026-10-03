package io.github.jamisuni.tangram.browse

import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.kernel.layout.TrayRules
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Leaving and returning, Retry, and the canonical stored form (design WO-004 sections 3 and 5). */
class LeaveReturnTest {

    private val puzzles = BrowseKit.puzzles
    private val sevenPiece = BrowseKit.indexOfFirstSevenPiecePuzzle()

    private fun threePieces(): Map<PieceId, PieceSave> {
        val p = puzzles[sevenPiece]
        val ids = p.solution.map { it.piece }.take(3)
        return ids.associateWith { BrowseKit.solutionSave(p, it) }
    }

    // REQ-003.A2 - "Leaving and returning to a puzzle never loses its state."
    // Design table: the fake host captures 3 pieces, leaving and returning passes them back (through the store).
    @Test
    fun req003_A2_leavingAndReturningKeepsThePlacedPieces() {
        val rig = BrowseKit.Rig()
        rig.controller.open(sevenPiece)
        val three = threePieces()
        rig.host.play(three)

        rig.controller.next() // away
        assertEquals(sevenPiece + 1, rig.controller.index)
        rig.controller.previous() // and back

        val back = rig.host.lastShown
        assertEquals(puzzles[sevenPiece].id, back.puzzle.id)
        assertEquals(PuzzleState.IN_PROGRESS, back.progress.state)
        assertEquals(three, back.progress.pieces)
    }

    // REQ-003.A2 - the same by jumping far away and back, with another puzzle also holding state (Solved).
    @Test
    fun req003_A2_eachPuzzleKeepsItsOwnStateAcrossAJumpAndBack() {
        val rig = BrowseKit.Rig()
        rig.controller.open(sevenPiece)
        val three = threePieces()
        rig.host.play(three)
        rig.controller.open(0)
        rig.host.state = PuzzleState.SOLVED
        rig.controller.open(sevenPiece)
        assertEquals(three, rig.host.lastShown.progress.pieces)
        rig.controller.open(0)
        assertEquals(PuzzleState.SOLVED, rig.host.lastShown.progress.state)
    }

    // design section 3 "When" table (DA-49): moving away saves the leaving puzzle, then lastShown.
    @Test
    fun decisionDA49_movingAwaySavesTheLeavingPuzzleBeforeShowingTheNext() {
        val rig = BrowseKit.Rig()
        rig.controller.open(sevenPiece)
        rig.host.play(threePieces())
        rig.store.clearWriteLog()
        rig.controller.next()
        val saved = rig.store.progress(puzzles[sevenPiece].id)
        assertEquals(PuzzleState.IN_PROGRESS, saved.state)
        assertEquals(threePieces(), saved.pieces)
        assertEquals(puzzles[sevenPiece + 1].id, rig.store.lastShownPuzzle())
    }

    // REQ-026.A2 - "Retry leads to an empty silhouette with all pieces in the tray."
    // Design table: restart() from SOLVED gives New, empty pieces, best time kept (REQ-026 rule 1: "Retry keeps the best time").
    @Test
    fun req026_A2_retryReturnsAnEmptyNewPuzzleAndKeepsTheBestTime() {
        val id = BrowseKit.id(0)
        val rig = BrowseKit.Rig {
            seedLastShown(id)
            seed(id, BrowseKit.progress(PuzzleState.SOLVED, emptyMap(), puzzleSeconds = 47, best = 41))
        }
        assertEquals(PuzzleState.SOLVED, rig.host.state)
        val shownBefore = rig.host.shown.size

        rig.controller.restart()

        val stored = rig.store.progress(id)
        assertEquals(PuzzleState.NEW, stored.state)
        assertEquals(emptyMap<PieceId, PieceSave>(), stored.pieces)
        assertEquals(0L, stored.puzzleSeconds)
        assertEquals(41L, stored.bestSeconds)
        assertEquals(shownBefore + 1, rig.host.shown.size)
        assertEquals(PuzzleState.NEW, rig.host.lastShown.progress.state)
        assertEquals(emptyMap<PieceId, PieceSave>(), rig.host.lastShown.progress.pieces)
        assertEquals("Retry stays on the same puzzle", 0, rig.controller.index)
    }

    // decision DA-48: persist() stores a Solved puzzle without its pieces (the capture still reports them).
    @Test
    fun decisionDA48_persistStoresASolvedPuzzleWithoutItsPieces() {
        val rig = BrowseKit.Rig()
        rig.host.state = PuzzleState.SOLVED
        rig.host.pieces = BrowseKit.fullSolution(puzzles[0])
        rig.controller.persist()
        val stored = rig.store.progress(BrowseKit.id(0))
        assertEquals(PuzzleState.SOLVED, stored.state)
        assertEquals(emptyMap<PieceId, PieceSave>(), stored.pieces)
    }

    // decision DA-48/DA-65: resting unmirrored tray entries are dropped, turned or mirrored ones are kept.
    @Test
    fun decisionDA65_persistDropsRestingUnmirroredTrayEntriesAndKeepsTurnedOnes() {
        val rig = BrowseKit.Rig()
        rig.controller.open(sevenPiece) // uses all seven pieces, so none of the entries below is foreign to the puzzle
        val rest = TrayRules.restingTurn(PieceId.LT1.shape)
        val turned = PieceSave.InTray(Turn(3), false)
        val mirrored = PieceSave.InTray(TrayRules.restingTurn(PieceId.SQ.shape), true)
        rig.host.pieces = mapOf(
            PieceId.LT1 to PieceSave.InTray(rest, false),
            PieceId.LT2 to turned,
            PieceId.SQ to mirrored,
        )
        rig.controller.persist()
        val stored = rig.store.progress(BrowseKit.id(sevenPiece))
        assertEquals(mapOf(PieceId.LT2 to turned, PieceId.SQ to mirrored), stored.pieces)
        assertEquals("a New puzzle keeps its tray turns but stays New", PuzzleState.NEW, stored.state)
    }

    @Test
    fun decisionDA65_anUntouchedNewPuzzleStoresAsEqualToNew() {
        val rig = BrowseKit.Rig()
        rig.host.pieces = puzzles[0].solution.associate { it.piece to PieceSave.InTray(TrayRules.restingTurn(it.piece.shape), false) }
        rig.controller.persist()
        assertEquals(PuzzleProgress.NEW, rig.store.progress(BrowseKit.id(0)))
    }

    // design section 3 (O-08): every write is a read-modify-write, so times pass through untouched.
    @Test
    fun decisionO08_persistKeepsTheStoredTimes() {
        val id = BrowseKit.id(0)
        val rig = BrowseKit.Rig {
            seedLastShown(id)
            seed(id, BrowseKit.progress(PuzzleState.NEW, puzzleSeconds = 33, best = 20))
        }
        rig.host.play(mapOf(PieceId.ST1 to PieceSave.InTray(Turn(2), true)))
        rig.controller.persist()
        val stored = rig.store.progress(id)
        assertEquals(33L, stored.puzzleSeconds)
        assertEquals(20L, stored.bestSeconds)
    }

    // design section 5 `restart()` and DA-59: on a New puzzle it is a no-op.
    @Test
    fun decisionDA59_restartOnANewPuzzleIsANoOp() {
        val rig = BrowseKit.Rig()
        rig.store.clearWriteLog()
        val shown = rig.host.shown.size
        rig.controller.restart()
        assertEquals(shown, rig.host.shown.size)
        assertFalse(rig.store.anyWrites)
        assertEquals(0, rig.controller.index)
    }
}
