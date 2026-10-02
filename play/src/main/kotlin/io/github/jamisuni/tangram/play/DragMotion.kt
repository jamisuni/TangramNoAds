package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.kernel.geometry.Vec2

// WO-003 design section 4 (REQ-014, DA-17): the 120 ms growth and move from the pick-up pose to the floating pose.
// Pure; time is frame-clock ms since the drag began.
internal object DragMotion {
    /** Ease-out cubic; exactly 0 at 0 and exactly 1 from 1 on. */
    fun ease(t: Double): Double {
        val c = t.coerceIn(0.0, 1.0)
        val u = 1.0 - c
        return 1.0 - u * u * u
    }

    private fun progress(elapsedMs: Long): Double = ease(elapsedMs.toDouble() / PlayTiming.GROW_MS)

    /** The piece centre in dp: eases from [pickUp] to `finger + (0, -lift)`; after 120 ms exactly the latter. */
    fun centre(finger: Vec2, pickUp: Vec2, lift: Double, elapsedMs: Long): Vec2 {
        val tx = finger.x
        val ty = finger.y - lift
        val t = progress(elapsedMs)
        return if (t >= 1.0) Vec2(tx, ty) else Vec2(pickUp.x + (tx - pickUp.x) * t, pickUp.y + (ty - pickUp.y) * t)
    }

    /** The drawn scale: from [startScale] (tray scale or board scale) to [boardScale], exactly it after 120 ms. */
    fun scale(startScale: Double, boardScale: Double, elapsedMs: Long): Double {
        val t = progress(elapsedMs)
        return if (t >= 1.0) boardScale else startScale + (boardScale - startScale) * t
    }
}
