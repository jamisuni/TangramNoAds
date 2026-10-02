package io.github.jamisuni.tangram.kernel.state

import io.github.jamisuni.tangram.kernel.model.PuzzleState

/** TYPE-006 transitions (architecture O-07). Restart and Retry arrive with WO-004, their first caller. */
object PuzzleStates {
    /** New to In progress when a drag starts on a tray piece; any other state is unchanged. */
    fun onTrayDragStarted(state: PuzzleState): PuzzleState =
        if (state == PuzzleState.NEW) PuzzleState.IN_PROGRESS else state

    /** In progress to Solved when the last piece locked ([allLocked]); any other state is unchanged. */
    fun onPieceLocked(state: PuzzleState, allLocked: Boolean): PuzzleState =
        if (state == PuzzleState.IN_PROGRESS && allLocked) PuzzleState.SOLVED else state
}
