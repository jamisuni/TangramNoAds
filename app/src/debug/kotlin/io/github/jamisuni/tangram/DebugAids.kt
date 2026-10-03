package io.github.jamisuni.tangram

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.devtools.DevCornerButton
import io.github.jamisuni.tangram.devtools.DevSolutionOverlay
import io.github.jamisuni.tangram.devtools.DevToolsState
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.play.BoardSpace

/**
 * Debug build: the real testing aid (REQ-046, DA-72). One [DevToolsState] lives as long as the owning ViewModel (DA-76),
 * so the unlock and the overlay flag survive rotation and browsing and end with the process. Never persisted.
 */
class DebugAids {
    private val state = DevToolsState()

    @Composable
    fun CornerButton(
        puzzle: Puzzle,
        solveNow: (List<PlacedPiece>) -> Boolean,
        blocked: () -> Boolean,
        modifier: Modifier = Modifier,
    ) {
        DevCornerButton(state, puzzle, solveNow, blocked, modifier)
    }

    @Composable
    fun BoardOverlay(puzzle: Puzzle, space: BoardSpace) {
        DevSolutionOverlay(state, puzzle, toDp = space::toDp)
    }
}
