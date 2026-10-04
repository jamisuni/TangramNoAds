@file:Suppress("UNUSED_PARAMETER")

package io.github.jamisuni.tangram

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.play.BoardSpace
import io.github.jamisuni.tangram.settings.HapticOut
import io.github.jamisuni.tangram.settings.SoundOut

/** Release build: nothing is shown and nothing is held (DA-72). Same public signatures as the debug twin. */
class DebugAids {
    /** Release: the real out, unchanged (DA-89). */
    fun sound(real: SoundOut): SoundOut = real

    fun haptic(real: HapticOut): HapticOut = real

    @Composable
    fun CornerButton(
        puzzle: Puzzle,
        solveNow: (List<PlacedPiece>) -> Boolean,
        blocked: () -> Boolean,
        modifier: Modifier = Modifier,
    ) {
    }

    @Composable
    fun BoardOverlay(puzzle: Puzzle, space: BoardSpace) {
    }
}
