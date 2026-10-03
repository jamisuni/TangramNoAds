package io.github.jamisuni.tangram.play

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.play.draw.VisualTokens
import io.github.jamisuni.tangram.play.draw.buildSilhouettePath
import io.github.jamisuni.tangram.play.draw.drawPicture
import io.github.jamisuni.tangram.play.draw.inDp

/**
 * WO-004 section 6 (REQ-050 A2, O-09: play lends its drawing, `browse` never duplicates it). One grid cell's art: the
 * flat silhouette ([VisualTokens.SILHOUETTE]) when [solved] is false, the solved picture clipped to the same silhouette
 * when true, fitted into the box with padding by [PlayLayout.forThumbnail].
 *
 * Cost: the layout and the silhouette union are computed once per (puzzle, box size) in `remember`, and the picture is
 * the vector [drawPicture] into the cell's own display list (no bitmap, no frame loop, no global cache slot touched), so
 * a lazy grid of 13+ cells pays one small path union per cell when it scrolls into view and nothing per frame.
 */
@Composable
fun PuzzleThumbnail(puzzle: Puzzle, solved: Boolean, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val w = maxWidth.value.toDouble()
        val h = maxHeight.value.toDouble()
        // N4: an unbounded or empty box has nothing to fit into; draw nothing instead of infinite or NaN geometry.
        if (!w.isFinite() || !h.isFinite() || w <= 0.0 || h <= 0.0) return@BoxWithConstraints
        val layout = remember(puzzle, w, h) { PlayLayout.forThumbnail(w, h, puzzle) }
        val clip = remember(layout) { buildSilhouettePath(layout) }
        Canvas(Modifier.fillMaxSize()) { // fills the box the caller gives; with no size modifier, all the room offered
            if (solved) {
                drawPicture(puzzle.picture, layout, clip)
            } else {
                inDp { drawPath(clip, VisualTokens.SILHOUETTE) }
            }
        }
    }
}
