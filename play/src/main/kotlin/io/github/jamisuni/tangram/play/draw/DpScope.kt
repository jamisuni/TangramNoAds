package io.github.jamisuni.tangram.play.draw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale

/**
 * The ONE place where dp becomes px: everything `PlayLayout` hands out is dp, so a drawing block runs inside a
 * `scale(density)` transform and uses those dp values (strokes, dashes and radii included) as they are.
 * Text is the exception (measured in px) and is drawn outside this block, with dp values times `density`.
 */
internal inline fun DrawScope.inDp(block: DrawScope.() -> Unit) {
    scale(density, density, Offset.Zero, block)
}
