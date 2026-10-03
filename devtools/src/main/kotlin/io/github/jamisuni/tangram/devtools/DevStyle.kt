package io.github.jamisuni.tangram.devtools

import androidx.compose.ui.graphics.Color

/** Colours of the DEV aid (the Spec/02 palette; `devtools` may not use other modules' tokens, G-06). */
internal object DevStyle {
    val PAPER = Color(0xFFFBF7EE)
    val INK = Color(0xFF2B2D42)
    val MUTED = Color(0xFF6B6F85)
    val ACCENT = Color(0xFF3D8BFD)
    val CELL = Color(0xFFEFE7D6)
    val NOTICE = Color(0xFFB3261E)

    /** Design 3 rule 1: the pill's touch box is exactly this size, in every language and font scale. */
    const val PILL_WIDTH_DP = 56
    const val PILL_HEIGHT_DP = 40
}
