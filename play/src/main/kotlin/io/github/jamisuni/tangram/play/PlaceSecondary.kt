package io.github.jamisuni.tangram.play

// decision DA-75 (WO-005 design section 3b): where the second corner control (the DEV pill) goes. Pure, no Compose.
// It only READS the layout and the primary placement, so it can never change the board.

private const val SECONDARY_INSET_DP = 4.0
private const val SECONDARY_CLEARANCE_DP = 4.0
private const val BESIDE_STRIP_GAP_DP = 8.0

/**
 * The [widthDp] x [heightDp] rect for the secondary control, or null (not shown). Order: board-rect corners inset 4 dp
 * (bottom-left, bottom-right, top-right, top-left), then beside the primary control when it sits in a strip, else null.
 * A rect is legal when inside the area, above `trayTop`, 4 dp clear of the silhouette and of the [primary] rect.
 */
internal fun PlayLayout.Companion.placeSecondary(
    layout: PlayLayout,
    primary: ControlPlacement?,
    widthDp: Double,
    heightDp: Double,
): RectDp? {
    val b = layout.boardRect
    val m = SECONDARY_INSET_DP
    val corners = listOf(
        RectDp(b.left + m, b.bottom - m - heightDp, b.left + m + widthDp, b.bottom - m),
        RectDp(b.right - m - widthDp, b.bottom - m - heightDp, b.right - m, b.bottom - m),
        RectDp(b.right - m - widthDp, b.top + m, b.right - m, b.top + m + heightDp),
        RectDp(b.left + m, b.top + m, b.left + m + widthDp, b.top + m + heightDp),
    )
    for (r in corners) if (legal(layout, primary, r)) return r
    if (primary != null && primary.corner == ControlCorner.STRIP) {
        val left = primary.rect.right + BESIDE_STRIP_GAP_DP
        val r = RectDp(left, primary.rect.top, left + widthDp, primary.rect.top + heightDp)
        if (r.right <= layout.areaWidth - m && legal(layout, primary, r)) return r
    }
    return null
}

private fun legal(layout: PlayLayout, primary: ControlPlacement?, r: RectDp): Boolean {
    val e = 1e-9
    if (r.left < -e || r.top < -e || r.right > layout.areaWidth + e || r.bottom > layout.areaHeight + e) return false
    if (r.bottom > layout.trayTop + e) return false
    if (PlayLayout.touchesSilhouette(r, layout.silhouetteDp, SECONDARY_CLEARANCE_DP)) return false
    if (primary != null) {
        val p = primary.rect
        val c = SECONDARY_CLEARANCE_DP
        val apart = r.left >= p.right + c - e || r.right <= p.left - c + e || r.top >= p.bottom + c - e || r.bottom <= p.top - c + e
        if (!apart) return false
    }
    return true
}
