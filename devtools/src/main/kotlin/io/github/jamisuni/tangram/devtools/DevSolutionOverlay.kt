package io.github.jamisuni.tangram.devtools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.Vec2

/** A polygon's bounding box in overlay dp; the frame of its semantics-only node (DA-44 pattern). */
internal data class BoundsDp(val left: Double, val top: Double, val width: Double, val height: Double)

internal fun boundsOf(points: List<Vec2>): BoundsDp {
    val minX = points.minOf { it.x }
    val maxX = points.maxOf { it.x }
    val minY = points.minOf { it.y }
    val maxY = points.maxOf { it.y }
    return BoundsDp(minX, minY, maxX - minX, maxY - minY)
}

/**
 * REQ-046 rule 3 (DA-82): each piece's stored solution polygon in its colour (alpha .55) with a white 2 dp dashed
 * outline (6/4 dp) and its id at the centroid (14 sp bold white). Composes nothing while the overlay is off.
 * Takes no touches: one `Canvas` without pointer input, plus one semantics-only node per piece
 * (`dev-solution-<PieceId.name>`, bounds = the polygon's bounding box in overlay coordinates).
 * [toDp] maps puzzle units to overlay dp (the board's transform).
 */
@Composable
fun DevSolutionOverlay(
    state: DevToolsState,
    puzzle: Puzzle,
    toDp: (Vec2) -> Vec2,
    modifier: Modifier = Modifier,
) {
    if (!state.overlayOn) return
    val shapes = remember(puzzle) { DevSolution.shapes(puzzle) }
    val measurer = rememberTextMeasurer()
    Box(modifier = modifier.fillMaxSize().testTag("dev-solution-overlay")) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val d = density
            val dash = PathEffect.dashPathEffect(floatArrayOf(6f * d, 4f * d))
            for (shape in shapes) {
                val pts = shape.polygon.map { toDp(it) }
                val path = Path().apply {
                    pts.forEachIndexed { i, p ->
                        val x = (p.x * d).toFloat()
                        val y = (p.y * d).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(path, Color(shape.colour).copy(alpha = 0.55f), style = Fill)
                drawPath(path, Color.White, style = Stroke(width = 2f * d, pathEffect = dash))
                val c = toDp(shape.label)
                val layout = measurer.measure(
                    text = shape.piece.name,
                    style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                )
                drawText(
                    layout,
                    topLeft = Offset(
                        (c.x * d).toFloat() - layout.size.width / 2f,
                        (c.y * d).toFloat() - layout.size.height / 2f,
                    ),
                )
            }
        }
        for (shape in shapes) {
            val b = boundsOf(shape.polygon.map { toDp(it) })
            Box(
                modifier = Modifier
                    .offset(b.left.dp, b.top.dp)
                    .size(b.width.dp, b.height.dp)
                    .testTag("dev-solution-${shape.piece.name}"),
            )
        }
    }
}
