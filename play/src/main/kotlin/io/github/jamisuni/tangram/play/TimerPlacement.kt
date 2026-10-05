package io.github.jamisuni.tangram.play

// WO-008 design section 7 (REQ-031 A2 placement; rev 1 S3, rev 2 E3/E4). Pure, no Compose. It only READS the layout and
// the obstacle rects, so it can never change the board, Restart or DEV (computeWithCorner / placeSecondary stay as they are).

internal const val TIMER_INSET_DP = 8.0
internal const val TIMER_CLEARANCE_DP = 4.0

/**
 * The rect the puzzle-time pill is PLACED in: [widthDp] x [heightDp] at the board's top-right corner inset 8 dp. When the
 * rect would come within 4 dp of an [obstacles] rect it steps to 4 dp below that obstacle (right-aligned), repeating for
 * the next one. [obstacles] are the real rects: Restart (even while hidden, E3) and the DEV pill when there is one (none
 * in release, E4).
 *
 * E3: [widthDp] is the TEMPLATE width (the widest text, "88 h 59 min", for the language and font scale), never the live
 * text, so the step never changes within an attempt; the caller draws the live pill right-aligned inside the returned rect.
 * If no legal rect (inside the area, above the tray) exists, the plain top-right rect is returned.
 */
internal fun PlayLayout.Companion.timerRect(
    layout: PlayLayout,
    obstacles: List<RectDp>,
    widthDp: Double,
    heightDp: Double,
): RectDp {
    val b = layout.boardRect
    val w = minOf(widthDp, maxOf(0.0, layout.areaWidth - 2 * TIMER_INSET_DP))
    val right = b.right - TIMER_INSET_DP
    val plain = RectDp(right - w, b.top + TIMER_INSET_DP, right, b.top + TIMER_INSET_DP + heightDp)
    val e = 1e-9
    var top = plain.top
    var changed = true
    var rounds = 0
    while (changed && rounds <= obstacles.size + 1) {
        changed = false
        rounds++
        val r = RectDp(plain.left, top, plain.right, top + heightDp)
        for (o in obstacles) {
            val near = r.left < o.right + TIMER_CLEARANCE_DP - e && r.right > o.left - TIMER_CLEARANCE_DP + e &&
                r.top < o.bottom + TIMER_CLEARANCE_DP - e && r.bottom > o.top - TIMER_CLEARANCE_DP + e
            if (near) {
                top = o.bottom + TIMER_CLEARANCE_DP
                changed = true
                break
            }
        }
    }
    val result = RectDp(plain.left, top, plain.right, top + heightDp)
    val legal = !changed && result.bottom <= layout.trayTop + e && result.bottom <= layout.areaHeight + e && result.left >= -e
    return if (legal) result else plain
}
