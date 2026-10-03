package io.github.jamisuni.tangram.browse

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.IPuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState

/** ACCEPTANCE-TEST SCAFFOLDING (TASK-T4): the real packaged puzzles (test scope may use `content`, G-06) and helpers. */
object BrowseKit {

    val library: IPuzzleLibrary by lazy { PuzzleLibrary.packaged() }
    val puzzles: List<Puzzle> get() = library.puzzles

    /** The solution pose of [piece] as a saved board piece: the exact turn, mirror and `at` for the stored polygon. */
    fun solutionSave(puzzle: Puzzle, piece: PieceId): PieceSave.OnBoard {
        val sp = puzzle.solution.firstOrNull { it.piece == piece } ?: error("${puzzle.id.value} has no piece $piece")
        val pose = PieceGeometry.poseOf(sp.piece, sp.polygon) ?: error("no pose for $piece of ${puzzle.id.value}")
        return PieceSave.OnBoard(pose.at, pose.turn, pose.mirrored)
    }

    /** All pieces of the puzzle in their solution poses. */
    fun fullSolution(puzzle: Puzzle): Map<PieceId, PieceSave> = puzzle.solution.associate { it.piece to solutionSave(puzzle, it.piece) }

    /** Index of the first puzzle that uses all seven pieces and is not the last in the list (a 7-piece puzzle). */
    fun indexOfFirstSevenPiecePuzzle(): Int = puzzles.indexOfFirst { it.solution.size == 7 }.also { check(it >= 0) { "no 7-piece puzzle in the library" } }

    fun progress(state: PuzzleState, pieces: Map<PieceId, PieceSave> = emptyMap(), puzzleSeconds: Long = 0, best: Long? = null) =
        PuzzleProgress(state, pieces, puzzleSeconds, best)

    fun id(i: Int): PuzzleId = puzzles[i].id

    /** A started controller over a fresh fake store and host. */
    class Rig(seed: FakeProgressStore.() -> Unit = {}) {
        val store = FakeProgressStore().apply(seed)
        val host = FakeHost()
        val controller = BrowseController(library, store, host)
        init {
            controller.start()
        }
    }
}
