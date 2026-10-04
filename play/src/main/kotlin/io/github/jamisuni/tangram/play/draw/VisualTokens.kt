package io.github.jamisuni.tangram.play.draw

import androidx.compose.ui.graphics.Color

// WO-003 TASK-018a: the visual tokens. Colours and sizes the REQs leave open follow Spec/02-ui-layout.md section 4/5
// (DA-24) and the pulse / preview looks follow DA-20. All lengths are dp; the drawing converts to px
// (value times density at the point of use; rule in the PlayDrawing.kt header, decision DA-92).
internal object VisualTokens {
    /** Spec/02 section 5: board white. */
    val BOARD = Color(0xFFFFFFFF)
    const val BOARD_RADIUS_DP = 12f

    /** Spec/02 section 5: silhouette #4A4E69, flat. */
    val SILHOUETTE = Color(0xFF4A4E69)

    /** Spec/02 section 5: tray #EFE7D6 (the cell fill). */
    val TRAY_CELL = Color(0xFFEFE7D6)
    const val CELL_RADIUS_DP = 10f

    /** Spec/02 section 5: pieces have a 2 dp white edge. */
    val PIECE_EDGE = Color.White
    const val PIECE_EDGE_DP = 2f

    /** DA-37: the dashed outline left in the tray cell of a piece that is not in the tray; prototype tools/prototype_template.html:370,477 (DI-1), Spec/02 is silent. */
    val CELL_OUTLINE = Color(0xFFD5CDBB)
    const val CELL_OUTLINE_DP = 2f
    const val CELL_OUTLINE_DASH_DP = 5f
    const val CELL_OUTLINE_GAP_DP = 4f

    /** DA-20: the landing preview, white 2.5 dp, dash 7 / 5 dp, no fill. */
    val PREVIEW = Color.White
    const val PREVIEW_DP = 2.5f
    const val PREVIEW_DASH_DP = 7f
    const val PREVIEW_GAP_DP = 5f

    /** DA-20: the corner marker, accent #3D8BFD (Spec/02 section 5), 2.5 dp white ring; radius from `pulseRadius`. */
    val ACCENT = Color(0xFF3D8BFD)
    val PULSE_RING = Color.White
    const val PULSE_RING_DP = 2.5f

    /** Spec/02 section 4: the flip badge is a 38 dp visible disc inside its 60 dp touch square, top right. */
    const val BADGE_DISC_DP = 38f
    val BADGE_FILL = Color.White
    val BADGE_EDGE = SILHOUETTE
    val BADGE_GLYPH = SILHOUETTE
    const val BADGE_EDGE_DP = 2f

    /** Spec/02 section 3 step 4b: the size mark chip in the cell's top-left corner, tinted with the piece colour. */
    const val MARK_CHIP_DP = 20f
    const val MARK_INSET_DP = 4f
    val MARK_TEXT = Color.White
    const val MARK_TEXT_SP = 12f

    /** Picture strokes without a width (design section 2 default, Spec/03). */
    const val DEFAULT_STROKE_UNITS = 0.05
}
