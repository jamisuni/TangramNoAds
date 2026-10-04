package io.github.jamisuni.tangram.play.draw

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.TrayRules
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PieceShape
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Turn
import io.github.jamisuni.tangram.play.DragMotion
import io.github.jamisuni.tangram.play.PieceDrawing
import io.github.jamisuni.tangram.play.PieceState
import io.github.jamisuni.tangram.play.PlayLayout
import io.github.jamisuni.tangram.play.PlaySession
import io.github.jamisuni.tangram.play.PlayTiming
import io.github.jamisuni.tangram.play.RectDp
import io.github.jamisuni.tangram.play.SolvedTimeline
import io.github.jamisuni.tangram.play.Where
import io.github.jamisuni.tangram.play.pulseRadius
import io.github.jamisuni.tangram.play.shakeOffset

// WO-003 TASK-018a: the drawing of the play area, a pure function of (session, layout, nowMs) with no side effects.
// nowMs is the frame clock handed in by the caller (design E5); there is no clock in here.
// decision DA-92, the ONE rule: everything PlayLayout hands out is dp and is turned into px at the point of use
// (value times `density`, G-03) and drawn in px; no draw here runs under a canvas scale transform. Reason: the API 26
// hardware renderer blurs a path (fill, stroke, dashes) drawn under a scale by about 2.5 px each side, while rects,
// round rects and circles stay crisp (measured). Paths are therefore built from px points (polygonPathPx), strokes,
// dashes and radii are multiplied by density, and the picture and the silhouette do the same (PictureDrawing.kt).

/** REQ-043: the three marks; the square and the parallelogram have none. */
internal enum class SizeMark { LARGE, MEDIUM, SMALL }

internal fun sizeMarkOf(piece: PieceId): SizeMark? = when (piece.shape) {
    PieceShape.LARGE_TRIANGLE -> SizeMark.LARGE
    PieceShape.MEDIUM_TRIANGLE -> SizeMark.MEDIUM
    PieceShape.SMALL_TRIANGLE -> SizeMark.SMALL
    else -> null
}

/** REQ-043: the marks on screen now: a triangle that is in the tray (not on the board, not in the hand). Reads the session. */
internal fun visibleSizeMarks(session: PlaySession): List<Pair<PieceId, SizeMark>> =
    if (session.state == PuzzleState.SOLVED) emptyList() // DA-52: no tray, so no marks, once solved
    else session.pieces.filter { it.where == Where.Tray }.mapNotNull { p -> sizeMarkOf(p.piece)?.let { p.piece to it } }

/** The chip of a mark inside its cell (dp): top-left corner (Spec/02 section 3 step 4b). */
internal fun sizeMarkRect(cell: RectDp): RectDp {
    val l = cell.left + VisualTokens.MARK_INSET_DP
    val t = cell.top + VisualTokens.MARK_INSET_DP
    return RectDp(l, t, l + VisualTokens.MARK_CHIP_DP, t + VisualTokens.MARK_CHIP_DP)
}

/** The mark letters, resolved from string resources by the composable (REQ-047). */
internal class SizeMarkTexts(val large: String, val medium: String, val small: String) {
    fun of(mark: SizeMark): String = when (mark) {
        SizeMark.LARGE -> large
        SizeMark.MEDIUM -> medium
        SizeMark.SMALL -> small
    }
}

/** The polygon of a piece (dp): offsets minus the centroid, times [scale], around [centre]; no canvas rotate or mirror. */
internal fun pieceDp(piece: PieceId, turn: Turn, mirrored: Boolean, centre: Vec2, scale: Double): List<Vec2> {
    val c = PieceGeometry.centroidOffset(piece.shape, turn, mirrored)
    return PieceGeometry.offsets(piece.shape, turn, mirrored).map {
        Vec2(centre.x + (it.x.toDouble() - c.x) * scale, centre.y + (it.y.toDouble() - c.y) * scale)
    }
}

private fun pieceColour(piece: PieceId): Color = Color(TrayRules.colour(piece))

/** Where a piece is drawn when it is not being dragged: the tray cell centre or the board centroid (dp), and the drawn scale. */
private fun restingCentre(s: PieceState, layout: PlayLayout): Vec2? = when (val w = s.where) {
    Where.Tray -> if (layout.hasCell(s.piece)) layout.cell(s.piece).centre else null
    is Where.Board -> {
        val c = PieceGeometry.centroidOffset(s.piece.shape, s.turn, s.mirrored)
        layout.toDp(Vec2(w.at.x.toDouble() + c.x, w.at.y.toDouble() + c.y))
    }
    Where.Dragged -> null
}

private fun DrawScope.fillPiece(points: List<Vec2>, colour: Color) {
    val path = polygonPathPx(points, density)
    drawPath(path, colour)
    drawPath(
        path, VisualTokens.PIECE_EDGE,
        style = Stroke(VisualTokens.PIECE_EDGE_DP * density, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

/** Everything of the play area in the design's layer order. `texts` and `measurer` draw the size marks. */
internal fun DrawScope.drawPlay(
    session: PlaySession,
    layout: PlayLayout,
    nowMs: Long,
    texts: SizeMarkTexts,
    measurer: TextMeasurer,
) {
    session.version // the one observation read of this pass (CR-1 N1)
    val trayShown = session.state != PuzzleState.SOLVED // DA-52: SOLVED draws no tray, marks, badge or dashed outlines
    val solved = session.solved
    val sinceSolve = if (solved != null) nowMs - solved.t0 else Long.MIN_VALUE
    val piecesHidden = solved != null && SolvedTimeline.piecesHidden(sinceSolve)
    val pieces = session.pieces
    val drag = session.drag
    val glides = session.glides
    val gliding = glides.map { it.piece }.toSet()
    val pop = if (solved != null) SolvedTimeline.popScale(sinceSolve, solved.reducedMotion) else 1.0

    val d = density
    // board background (white, rounded) then the silhouette: ONE union path, one flat colour (REQ-011)
    val b = layout.boardRect
    drawRoundRect(
        VisualTokens.BOARD,
        Offset(b.left.toFloat() * d, b.top.toFloat() * d),
        Size(b.width.toFloat() * d, b.height.toFloat() * d),
        CornerRadius(VisualTokens.BOARD_RADIUS_DP * d, VisualTokens.BOARD_RADIUS_DP * d),
    )
    drawPath(silhouettePathPx(layout, d), VisualTokens.SILHOUETTE)

    // tray: a cell holds its piece, its size mark (REQ-043), the flip badge for PG, or, once the piece left, its dashed outline (REQ-012, DA-37)
    for (s in pieces) {
        if (!trayShown || !layout.hasCell(s.piece)) continue
        val cell = layout.cell(s.piece)
        drawRoundRect(
            VisualTokens.TRAY_CELL,
            Offset(cell.left.toFloat() * d, cell.top.toFloat() * d),
            Size(cell.width.toFloat() * d, cell.height.toFloat() * d),
            CornerRadius(VisualTokens.CELL_RADIUS_DP * d, VisualTokens.CELL_RADIUS_DP * d),
        )
    }
    for (s in pieces) {
        if (!trayShown || s.where == Where.Tray || !layout.hasCell(s.piece)) continue
        // DA-37: a piece that left the tray leaves its dashed outline (resting turn, unmirrored, at trayScale)
        val pts = pieceDp(s.piece, TrayRules.restingTurn(s.piece.shape), false, layout.cell(s.piece).centre, layout.trayScale)
        drawPath(
            polygonPathPx(pts, d), VisualTokens.CELL_OUTLINE,
            style = Stroke(
                VisualTokens.CELL_OUTLINE_DP * d,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(VisualTokens.CELL_OUTLINE_DASH_DP * d, VisualTokens.CELL_OUTLINE_GAP_DP * d)),
            ),
        )
    }
    for (s in pieces) {
        if (!trayShown || s.where != Where.Tray || s.piece in gliding) continue
        val centre = restingCentre(s, layout) ?: continue
        fillPiece(pieceDp(s.piece, s.turn, s.mirrored, centre, PieceDrawing.scale(s, layout, drag)), pieceColour(s.piece))
    }

    // placed pieces (2 dp white edge), pop about their centres, shake of the one that refused a turn
    if (!piecesHidden) {
        val shake = session.shake
        for (s in pieces) {
            if (s.where !is Where.Board || s.piece in gliding) continue
            val centre = restingCentre(s, layout) ?: continue
            val dx = if (shake != null && shake.piece == s.piece) shakeOffset(nowMs - shake.startMs) else 0.0
            val at = Vec2(centre.x + dx, centre.y)
            fillPiece(pieceDp(s.piece, s.turn, s.mirrored, at, PieceDrawing.scale(s, layout, drag) * pop), pieceColour(s.piece))
        }
    }

    // landing preview (DA-20): dashed white outline, no fill
    drag?.frame?.preview?.let { pv ->
        val pts = pv.corners.map { layout.toDp(Vec2(it.x.toDouble(), it.y.toDouble())) }
        drawPath(
            polygonPathPx(pts, d), VisualTokens.PREVIEW,
            style = Stroke(
                VisualTokens.PREVIEW_DP * d,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(VisualTokens.PREVIEW_DASH_DP * d, VisualTokens.PREVIEW_GAP_DP * d)),
            ),
        )
    }

    // corner pulse (DA-20, REQ-051): radius from the seam, static under reduced motion
    session.pulse?.let { pulse ->
        val r = pulseRadius(nowMs - pulse.startMs, pulse.reduced).toFloat()
        if (r > 0f) {
            for (c in pulse.corners.corners) {
                val p = layout.toDp(Vec2(c.x.toDouble(), c.y.toDouble()))
                val centre = Offset(p.x.toFloat() * d, p.y.toFloat() * d)
                drawCircle(VisualTokens.ACCENT, r * d, centre)
                drawCircle(VisualTokens.PULSE_RING, r * d, centre, style = Stroke(VisualTokens.PULSE_RING_DP * d))
            }
        }
    }

    // gliding pieces (180 ms from the drop pose to the logical place), then the dragged piece
    if (!piecesHidden) {
        for (g in glides) {
            val s = pieces.firstOrNull { it.piece == g.piece } ?: continue
            val to = restingCentre(s, layout) ?: continue
            val t = DragMotion.ease((nowMs - g.startMs).toDouble() / PlayTiming.GLIDE_MS)
            val toScale = PieceDrawing.scale(s, layout, null)
            val centre = Vec2(g.fromCentre.x + (to.x - g.fromCentre.x) * t, g.fromCentre.y + (to.y - g.fromCentre.y) * t)
            val sc = g.fromScale + (toScale - g.fromScale) * t
            fillPiece(pieceDp(s.piece, s.turn, s.mirrored, centre, sc * pop), pieceColour(s.piece)) // N6: same pop as the board loop, no step at 180 ms
        }
    }
    if (drag != null) {
        val s = pieces.firstOrNull { it.piece == drag.piece }
        if (s != null) {
            val centre = drag.frame?.centre ?: drag.pickUp.centre
            val turn = drag.frame?.pose?.turn ?: drag.turn
            fillPiece(pieceDp(s.piece, turn, drag.mirrored, centre, PieceDrawing.scale(s, layout, drag)), pieceColour(s.piece))
        }
    }

    // the solved picture fades in over the pieces (group alpha), then the confetti (REQ-023); drawPictureImage is px too
    if (solved != null) {
        val a = SolvedTimeline.pictureAlpha(sinceSolve).toFloat()
        if (a > 0f) drawPictureImage(session.puzzle.picture, layout, a)

        val centre = layout.boardRect.centre
        for (p in SolvedTimeline.confetti(sinceSolve, solved.reducedMotion)) {
            val col = Color(0xFF000000.toInt() or p.colour).copy(alpha = p.alpha.toFloat())
            translate((centre.x + p.dx).toFloat() * d, (centre.y + p.dy).toFloat() * d) {
                rotate(Math.toDegrees(p.rotation).toFloat(), Offset.Zero) {
                    drawRect(col, Offset((-p.width / 2).toFloat() * d, (-p.height / 2).toFloat() * d), Size(p.width.toFloat() * d, p.height.toFloat() * d))
                }
            }
        }
    }

    // the flip badge (DA-29/35): null = none; the 38 dp disc sits at the top right of its 60 dp square
    layout.badgeRect(session)?.let { drawBadge(it) }

    // size marks (REQ-043): chips tinted with the piece colour
    for ((piece, _) in visibleSizeMarks(session)) {
        if (!layout.hasCell(piece)) continue
        val r = sizeMarkRect(layout.cell(piece))
        drawRoundRect(
            pieceColour(piece),
            Offset(r.left.toFloat() * d, r.top.toFloat() * d),
            Size(r.width.toFloat() * d, r.height.toFloat() * d),
            CornerRadius(5f * d, 5f * d),
        )
    }

    // size mark letters: text is measured in px, so positions are dp times density here (measured in px, so converted by hand like every other value here)
    val style = TextStyle(color = VisualTokens.MARK_TEXT, fontSize = VisualTokens.MARK_TEXT_SP.sp)
    for ((piece, mark) in visibleSizeMarks(session)) {
        if (!layout.hasCell(piece)) continue
        val r = sizeMarkRect(layout.cell(piece))
        val laid = measurer.measure(texts.of(mark), style)
        val cx = (r.centre.x * density).toFloat()
        val cy = (r.centre.y * density).toFloat()
        drawText(laid, topLeft = Offset(cx - laid.size.width / 2f, cy - laid.size.height / 2f))
    }
}

private fun DrawScope.drawBadge(rect: RectDp) {
    val d = density
    val r = VisualTokens.BADGE_DISC_DP / 2f * d
    val centre = Offset((rect.right * d).toFloat() - r, (rect.top * d).toFloat() + r)
    drawCircle(VisualTokens.BADGE_FILL, r, centre)
    drawCircle(VisualTokens.BADGE_EDGE, r, centre, style = Stroke(VisualTokens.BADGE_EDGE_DP * d))
    // two opposite arrows (mirror glyph), drawn as strokes so no font glyph is needed
    val w = 7f * d
    val dy = 4f * d
    val h = 3f * d
    val st = Stroke(2f * d, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val up = Path().apply {
        moveTo(centre.x - w, centre.y - dy); lineTo(centre.x + w, centre.y - dy)
        moveTo(centre.x + w - h, centre.y - dy - h); lineTo(centre.x + w, centre.y - dy); lineTo(centre.x + w - h, centre.y - dy + h)
    }
    val down = Path().apply {
        moveTo(centre.x + w, centre.y + dy); lineTo(centre.x - w, centre.y + dy)
        moveTo(centre.x - w + h, centre.y + dy - h); lineTo(centre.x - w, centre.y + dy); lineTo(centre.x - w + h, centre.y + dy + h)
    }
    drawPath(up, VisualTokens.BADGE_GLYPH, style = st)
    drawPath(down, VisualTokens.BADGE_GLYPH, style = st)
}
