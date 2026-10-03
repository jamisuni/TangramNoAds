package io.github.jamisuni.tangram.browse

import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.model.PuzzleState

/**
 * AI-owned seam (WO-004 design section 5): `browse` may not depend on `play` (G-06), so the session that
 * shows a puzzle is reached through this interface, implemented by `app` over `play`.
 */
interface PuzzleHost {
    /** The shown session's state; observable (snapshot reads). */
    val state: PuzzleState

    /** True while a piece is dragged; observable. */
    val isDragging: Boolean

    /** Builds a fresh session restored from [progress] and makes it the shown one. */
    fun show(puzzle: Puzzle, progress: PuzzleProgress)

    /** The shown session as progress on top of [base] (interrupts a drag first); null before the first [show]. */
    fun capture(base: PuzzleProgress): PuzzleProgress?
}
