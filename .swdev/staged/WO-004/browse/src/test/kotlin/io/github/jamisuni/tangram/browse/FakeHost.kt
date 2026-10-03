package io.github.jamisuni.tangram.browse

import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState

/**
 * ACCEPTANCE-TEST ADAPTER (TASK-T4): a scripted `PuzzleHost`, written from the interface in design WO-004 section 5
 * only. `show` records the call and makes the shown session "restored" from the given progress; the test then plays
 * the session by setting [state], [pieces] and [isDragging]. `capture` reports exactly that, on top of `base`
 * (like `PlaySession.toProgress`), and is null before the first `show`.
 */
class FakeHost : PuzzleHost {

    data class Shown(val puzzle: Puzzle, val progress: PuzzleProgress)

    /** Every `show` call, in order. */
    val shown = mutableListOf<Shown>()

    /** Number of `capture` calls. */
    var captures = 0
        private set

    override var state: PuzzleState = PuzzleState.NEW
    override var isDragging: Boolean = false

    /** What the shown session would report for its pieces; reset to the restored pieces by every `show`. */
    var pieces: Map<PieceId, PieceSave> = emptyMap()

    val lastShown: Shown get() = shown.last()

    override fun show(puzzle: Puzzle, progress: PuzzleProgress) {
        shown += Shown(puzzle, progress)
        state = progress.state
        pieces = progress.pieces
    }

    override fun capture(base: PuzzleProgress): PuzzleProgress? {
        captures++
        if (shown.isEmpty()) return null
        return base.copy(state = state, pieces = pieces)
    }

    /** Test script: the player placed [saves] and the puzzle is now in progress. */
    fun play(saves: Map<PieceId, PieceSave>) {
        state = PuzzleState.IN_PROGRESS
        pieces = saves
    }
}
