package io.github.jamisuni.tangram.browse

import androidx.compose.ui.graphics.Color

/** The paper colour for the app's root background: one source, shared with the overlay (CR WO-004 N7). */
val BROWSE_PAPER: Color get() = BrowseStyle.PAPER

/** Colours of the browse UI (the Spec/02 palette; `browse` may not use `play`'s tokens, G-06). */
internal object BrowseStyle {
    val PAPER = Color(0xFFFBF7EE)
    val INK = Color(0xFF2B2D42)
    val ACCENT = Color(0xFF3D8BFD)
    val CELL = Color(0xFFEFE7D6)
    val DOT = Color(0xFFF2A33A)
    val GREEN = Color(0xFF3FA66B)

    /** REQ-037: every player control has a touch area of at least this many dp. */
    const val MIN_TOUCH_DP = 48
}
