package io.github.jamisuni.tangram.play.draw

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import io.github.jamisuni.tangram.contracts.puzzle.Picture
import io.github.jamisuni.tangram.contracts.puzzle.PicturePoint
import io.github.jamisuni.tangram.contracts.puzzle.PictureShape
import io.github.jamisuni.tangram.contracts.puzzle.PictureStyle
import io.github.jamisuni.tangram.contracts.puzzle.Rgb
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.play.PathData
import io.github.jamisuni.tangram.play.PathSeg
import io.github.jamisuni.tangram.play.PlayLayout

// WO-003 design section 2 "Solved picture" (REQ-023, REQ-039, DA-21). The silhouette fill and the picture clip are
// built from the SAME polygon list (`layout.silhouetteDp`) by the same function.

private var cachedPolys: List<List<Vec2>>? = null
private var cachedPath: Path? = null

/** The polygons (dp) as one [Path]: a union (no seam, REQ-011); per-polygon fill if the union fails (G-10). Main thread only. */
internal fun silhouettePath(polys: List<List<Vec2>>): Path {
    if (polys === cachedPolys) cachedPath?.let { return it }
    var union: Path? = null
    var ok = true
    for (poly in polys) {
        val p = polygonPath(poly)
        val cur = union
        if (cur == null) {
            union = p
        } else {
            val out = Path()
            if (out.op(cur, p, PathOperation.Union)) union = out else { ok = false; break }
        }
    }
    val result = if (ok && union != null) union else Path().also { all -> polys.forEach { all.addPath(polygonPath(it)) } }
    cachedPolys = polys
    cachedPath = result
    return result
}

internal fun polygonPath(points: List<Vec2>): Path = Path().apply {
    points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x.toFloat(), p.y.toFloat()) else lineTo(p.x.toFloat(), p.y.toFloat()) }
    close()
}

internal fun rgbColor(c: Rgb): Color = Color(0xFF000000.toInt() or c.value)

/**
 * The solved picture: base colour, then every shape in order, clipped to the silhouette (REQ-023 A1/A2, REQ-039 A1).
 * `layout` is dp; the dp to px step is [inDp]. A shape whose `d` does not parse is skipped (DA-21).
 */
internal fun DrawScope.drawPicture(picture: Picture, layout: PlayLayout) {
    val clip = silhouettePath(layout.silhouetteDp)
    inDp {
        clipPath(clip) {
            drawRect(
                rgbColor(picture.base),
                Offset(layout.boardRect.left.toFloat(), layout.boardRect.top.toFloat()),
                Size(layout.areaWidth.toFloat(), layout.areaHeight.toFloat()),
            )
            for (shape in picture.shapes) drawShape(shape, layout)
        }
    }
}

private fun DrawScope.drawShape(shape: PictureShape, layout: PlayLayout) {
    fun pt(p: PicturePoint): Vec2 = layout.toDp(Vec2(p.x, p.y))
    val u = layout.dpPerUnit
    when (shape) {
        is PictureShape.Polygon -> paintShape(
            Path().apply {
                shape.points.forEachIndexed { i, p ->
                    val d = pt(p)
                    if (i == 0) moveTo(d.x.toFloat(), d.y.toFloat()) else lineTo(d.x.toFloat(), d.y.toFloat())
                }
                close()
            },
            shape.style, layout, true,
        )
        is PictureShape.Rect -> {
            val o = pt(PicturePoint(shape.x, shape.y))
            val r = ((shape.cornerRadius ?: 0.0) * u).toFloat()
            paintShape(
                Path().apply {
                    addRoundRect(
                        RoundRect(
                            o.x.toFloat(), o.y.toFloat(), (o.x + shape.width * u).toFloat(), (o.y + shape.height * u).toFloat(),
                            CornerRadius(r, r),
                        ),
                    )
                },
                shape.style, layout, true,
            )
        }
        is PictureShape.Circle -> {
            val c = pt(shape.center)
            val r = shape.radius * u
            paintShape(
                Path().apply { addOval(Rect((c.x - r).toFloat(), (c.y - r).toFloat(), (c.x + r).toFloat(), (c.y + r).toFloat())) },
                shape.style, layout, true,
            )
        }
        is PictureShape.Ellipse -> {
            val c = pt(shape.center)
            val rx = shape.radiusX * u
            val ry = shape.radiusY * u
            paintShape(
                Path().apply { addOval(Rect((c.x - rx).toFloat(), (c.y - ry).toFloat(), (c.x + rx).toFloat(), (c.y + ry).toFloat())) },
                shape.style, layout, true,
            )
        }
        is PictureShape.Line -> {
            val a = pt(shape.from)
            val b = pt(shape.to)
            paintShape(
                Path().apply { moveTo(a.x.toFloat(), a.y.toFloat()); lineTo(b.x.toFloat(), b.y.toFloat()) },
                shape.style, layout, false,
            )
        }
        is PictureShape.Path -> {
            val segs = PathData.parse(shape.data) ?: return // DA-21: skip this one shape, draw the rest
            fun m(x: Double, y: Double) = pt(PicturePoint(x, y))
            val path = Path()
            for (s in segs) when (s) {
                is PathSeg.MoveTo -> m(s.x, s.y).let { path.moveTo(it.x.toFloat(), it.y.toFloat()) }
                is PathSeg.LineTo -> m(s.x, s.y).let { path.lineTo(it.x.toFloat(), it.y.toFloat()) }
                is PathSeg.QuadTo -> {
                    val a = m(s.x1, s.y1)
                    val b = m(s.x, s.y)
                    path.quadraticTo(a.x.toFloat(), a.y.toFloat(), b.x.toFloat(), b.y.toFloat())
                }
                is PathSeg.CubicTo -> {
                    val a = m(s.x1, s.y1)
                    val b = m(s.x2, s.y2)
                    val c = m(s.x, s.y)
                    path.cubicTo(a.x.toFloat(), a.y.toFloat(), b.x.toFloat(), b.y.toFloat(), c.x.toFloat(), c.y.toFloat())
                }
                PathSeg.Close -> path.close()
            }
            paintShape(path, shape.style, layout, true)
        }
    }
}

private fun DrawScope.paintShape(path: Path, style: PictureStyle, layout: PlayLayout, canFill: Boolean) {
    val alpha = (style.opacity ?: 1.0).coerceIn(0.0, 1.0).toFloat()
    val fill = style.fill
    if (canFill && fill != null) drawPath(path, rgbColor(fill), alpha)
    val stroke = style.stroke
    if (stroke != null) {
        val w = (style.strokeWidth ?: VisualTokens.DEFAULT_STROKE_UNITS) * layout.dpPerUnit
        drawPath(path, rgbColor(stroke), alpha, Stroke(w.toFloat(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
