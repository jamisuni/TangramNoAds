package io.github.jamisuni.tangram.settings

import androidx.compose.ui.graphics.Color

/** Prototype palette (tools/prototype_template.html) for the settings screen. */
internal object SettingsStyle {
    val PAPER = Color(0xFFFBF7EE)
    val INK = Color(0xFF2B2D42)
    val MUTED = Color(0xFF7E8299)
    val ACCENT = Color(0xFF3D8BFD)
    val CELL = Color(0xFFEFE7D6)
    val DANGER = Color(0xFFE8505B)
    val FREE_BG = Color(0xFFEAF7F0)
    val FREE_INK = Color(0xFF1F6B47)

    /** REQ-037: every player control has a touch area of at least this many dp. */
    const val MIN_TOUCH_DP = 48
}
