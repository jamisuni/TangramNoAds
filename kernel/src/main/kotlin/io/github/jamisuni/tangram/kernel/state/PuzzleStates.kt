package io.github.jamisuni.tangram.kernel.state

import io.github.jamisuni.tangram.kernel.model.PuzzleState

/** TYPE-006 transitions (architecture O-07). Restart and Retry (DA-59). */
object PuzzleStates {
    /** New to In progress when a drag starts on a tray piece; any other state is unchanged. */
    fun onTrayDragStarted(state: PuzzleState): PuzzleState =
        if (state == PuzzleState.NEW) PuzzleState.IN_PROGRESS else state

    /** In progress to Solved when the last piece locked ([allLocked]); any other state is unchanged. */
    fun onPieceLocked(state: PuzzleState, allLocked: Boolean): PuzzleState =
        if (state == PuzzleState.IN_PROGRESS && allLocked) PuzzleState.SOLVED else state

    /** Restart (TYPE-006): In progress to New; any other state is unchanged (a no-op on New). */
    fun onRestart(state: PuzzleState): PuzzleState =
        if (state == PuzzleState.IN_PROGRESS) PuzzleState.NEW else state

    /** Retry (TYPE-006): Solved to New (the best time is kept elsewhere); any other state is unchanged. */
    fun onRetry(state: PuzzleState): PuzzleState =
        if (state == PuzzleState.SOLVED) PuzzleState.NEW else state
}
