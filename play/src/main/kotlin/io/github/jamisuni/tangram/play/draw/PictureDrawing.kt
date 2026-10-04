package io.github.jamisuni.tangram.play.draw

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
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

/**
 * The silhouette as ONE [Path] with no interior edge (REQ-011), in dp; it is also the picture clip. The union is
 * computed in UNITS on a 2^-20 grid (shared vertices then coincide exactly and pieces that touch along a half-unit edge
 * cancel exactly) and the result is scaled into dp; per-polygon fills left anti-aliased hairlines on a device. If the
 * union fails the polygons are added to one path (G-10: draw something). Main thread only.
 */
internal fun silhouettePath(layout: PlayLayout): Path {
    val polys = layout.silhouetteDp
    if (polys === cachedPolys) cachedPath?.let { return it }
    val result = buildSilhouettePath(layout)
    cachedPolys = polys
    cachedPath = result
    return result
}

private var pxSource: Path? = null
private var pxDensity = 0f
private var pxPath: Path? = null

/**
 * [silhouettePath] already scaled to px (a copy, cached on the dp path and the density). Filling a path under a canvas
 * scale transform is blurred about 2.5 px each side by the API 26 hardware renderer; a path in px is crisp there, and the
 * same pixels as the scaled draw elsewhere (REQ-011 A2). Main thread only.
 */
internal fun silhouettePathPx(layout: PlayLayout, density: Float): Path {
    val src = silhouettePath(layout)
    pxPath?.let { if (src === pxSource && density == pxDensity) return it }
    val m = Matrix().apply { scale(density, density) }
    val out = Path().apply { addPath(src); transform(m) }
    pxSource = src
    pxDensity = density
    pxPath = out
    return out
}

/** The uncached union of [silhouettePath]; a grid thumbnail keeps its own copy so it never evicts the play area's slot. */
internal fun buildSilhouettePath(layout: PlayLayout): Path {
    val polys = layout.silhouetteDp
    val grid = 1048576.0
    fun snap(v: Double) = Math.rint(v * grid) / grid
    var union: Path? = null
    var ok = true
    for (poly in polys) {
        val units = poly.map { layout.toUnits(it).let { u -> Vec2(snap(u.x), snap(u.y)) } }
        val p = polygonPath(units)
        val cur = union
        if (cur == null) {
            union = p
        } else {
            val out = Path()
            if (out.op(cur, p, PathOperation.Union)) union = out else { ok = false; break }
        }
    }
    val result: Path
    if (ok && union != null) {
        val o = layout.toDp(Vec2(0.0, 0.0))
        val sc = layout.dpPerUnit.toFloat()
        union.transform(
            Matrix(
                floatArrayOf(
                    sc, 0f, 0f, 0f,
                    0f, sc, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    o.x.toFloat(), o.y.toFloat(), 0f, 1f,
                ),
            ),
        )
        result = union
    } else {
        result = Path().also { all -> polys.forEach { all.addPath(polygonPath(it)) } }
    }
    return result
}

internal fun polygonPath(points: List<Vec2>): Path = Path().apply {
    points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x.toFloat(), p.y.toFloat()) else lineTo(p.x.toFloat(), p.y.toFloat()) }
    close()
}

/** [polygonPath] with every point times [density]: the px path every draw uses (decision DA-92, see PlayDrawing.kt). */
internal fun polygonPathPx(points: List<Vec2>, density: Float): Path = Path().apply {
    points.forEachIndexed { i, p ->
        val x = (p.x * density).toFloat()
        val y = (p.y * density).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

internal fun rgbColor(c: Rgb): Color = Color(0xFF000000.toInt() or c.value)

/**
 * The solved picture: base colour, then every shape in order, clipped to the silhouette (REQ-023 A1/A2, REQ-039 A1).
 * `layout` is dp and every value is turned into px (times `density`) as it is used; nothing is drawn under a canvas
 * scale (decision DA-92). [clip] is a px path (default [silhouettePathPx]). A shape whose `d` does not parse is
 * skipped (DA-21).
 */
internal fun DrawScope.drawPicture(picture: Picture, layout: PlayLayout, clip: Path = silhouettePathPx(layout, density)) {
    val d = density
    clipPath(clip) {
        drawRect(
            rgbColor(picture.base),
            Offset(layout.boardRect.left.toFloat() * d, layout.boardRect.top.toFloat() * d),
            Size(layout.areaWidth.toFloat() * d, layout.areaHeight.toFloat() * d),
        )
        for (shape in picture.shapes) drawShape(shape, layout)
    }
}

/** Releases both picture caches (N2). Main thread only; the next draw rebuilds what it needs. */
internal fun clearPictureCaches() {
    cachedPolys = null
    cachedPath = null
    pxSource = null
    pxPath = null
    pictureKey = null
    pictureImage = null
}

private var pictureKey: Array<Any>? = null
private var pictureImage: ImageBitmap? = null

/**
 * The picture as the same pixels [drawPicture] gives on its own (REQ-023 A2: what the screen shows is the picture
 * rendered alone). It is rendered once by the software canvas into a bitmap of the play area (a hardware canvas
 * anti-aliased the picture's own inner edges differently) and drawn with [alpha]. One slot, rebuilt when the picture,
 * layout, density or size changes. Main thread only.
 */
internal fun DrawScope.drawPictureImage(picture: Picture, layout: PlayLayout, alpha: Float) {
    val w = size.width.toInt()
    val h = size.height.toInt()
    if (w <= 0 || h <= 0) return
    val key = pictureKey
    var img = pictureImage
    if (img == null || key == null || key[0] !== picture || key[1] !== layout || key[2] != density || key[3] != w || key[4] != h) {
        val fresh = ImageBitmap(w, h)
        CanvasDrawScope().draw(Density(density), LayoutDirection.Ltr, Canvas(fresh), Size(w.toFloat(), h.toFloat())) {
            drawPicture(picture, layout)
        }
        pictureKey = arrayOf(picture, layout, density, w, h)
        pictureImage = fresh
        img = fresh
    }
    drawImage(img, alpha = alpha)
}

private fun DrawScope.drawShape(shape: PictureShape, layout: PlayLayout) {
    // px, not dp: a path under a canvas scale is blurred on API 26 (decision DA-92)
    val d = density
    fun pt(p: PicturePoint): Vec2 = layout.toDp(Vec2(p.x, p.y)).let { Vec2(it.x * d, it.y * d) }
    val u = layout.dpPerUnit * d
    when (shape) {
        is PictureShape.Polygon -> paintShape(
            Path().apply {
                shape.points.forEachIndexed { i, p ->
                    val q = pt(p)
                    if (i == 0) moveTo(q.x.toFloat(), q.y.toFloat()) else lineTo(q.x.toFloat(), q.y.toFloat())
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
        val w = (style.strokeWidth ?: VisualTokens.DEFAULT_STROKE_UNITS) * layout.dpPerUnit * density
        drawPath(path, rgbColor(stroke), alpha, Stroke(w.toFloat(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
