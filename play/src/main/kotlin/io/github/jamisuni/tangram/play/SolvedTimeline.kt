package io.github.jamisuni.tangram.play

// WO-003 design section 2 "Solved picture" / "Pop and confetti" (DA-19): the solved sequence as pure functions of
// the milliseconds since the last lock (frame clock). Reduced motion gates the pop and the confetti only.
internal object SolvedTimeline {
    /** Picture alpha: 0 until 600 ms, linear to 1 at 1400 ms; the same under reduced motion (the picture still appears). */
    fun pictureAlpha(ms: Long): Double {
        val t = ms - PlayTiming.PICTURE_DELAY_MS
        return when {
            t <= 0L -> 0.0
            t >= PlayTiming.PICTURE_FADE_MS -> 1.0
            else -> t.toDouble() / PlayTiming.PICTURE_FADE_MS
        }
    }

    /** True from the moment the picture fully covers the pieces; pieces stop being drawn then (design section 2). */
    fun piecesHidden(ms: Long): Boolean = pictureAlpha(ms) >= 1.0

    /** Pieces scale 1 -> 1.06 -> 1 about their centres over 300 ms; 1 when reduced motion or outside the window. */
    fun popScale(ms: Long, reducedMotion: Boolean): Double {
        if (reducedMotion || ms < 0L || ms >= PlayTiming.POP_MS) return 1.0
        val phase = ms.toDouble() / PlayTiming.POP_MS
        return 1.0 + (PlayTiming.POP_SCALE - 1.0) * Math.sin(Math.PI * phase)
    }

    /** Confetti runs for 2.4 s after the last lock; never under reduced motion. */
    fun confettiActive(ms: Long, reducedMotion: Boolean): Boolean =
        !reducedMotion && ms >= 0L && ms < PlayTiming.CONFETTI_MS

    /** Confetti particles to draw at [ms] since the last lock; empty under reduced motion or outside the window. */
    fun confetti(ms: Long, reducedMotion: Boolean): List<ConfettiParticle> =
        if (confettiActive(ms, reducedMotion)) Confetti.at(ms) else emptyList()
}
