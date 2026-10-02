package io.github.jamisuni.tangram.play

// WO-003 design section 5 and DA-19: the timing constants of the animations, in milliseconds on the frame clock
// (design E5; never wall clock). Distances are dp.
internal object PlayTiming {
    const val GLIDE_MS = 180L
    const val GROW_MS = 120L
    const val PULSE_MS = 600L
    const val PICTURE_DELAY_MS = 600L
    const val PICTURE_FADE_MS = 800L
    const val CONFETTI_MS = 2400L
    const val POP_MS = 300L
    const val SHAKE_MS = 400L
    const val SHAKE_AMPLITUDE_DP = 6.0
    const val POP_SCALE = 1.06
    const val CONFETTI_COUNT = 110
    const val PULSE_RADIUS_DP = 9.0
    private const val SHAKE_CYCLES = 3.0

    /** The corner marker radius in dp at [ms] after the miss: x0.6 -> x1.3 -> x1.0; static under [reduced]; 0 outside 0..600 ms. */
    fun pulseRadius(ms: Long, reduced: Boolean): Double = when {
        ms < 0L || ms >= PULSE_MS -> 0.0
        reduced -> PULSE_RADIUS_DP
        else -> {
            val half = PULSE_MS / 2.0
            val f = if (ms <= half) 0.6 + 0.7 * (ms / half) else 1.3 - 0.3 * ((ms - half) / half)
            PULSE_RADIUS_DP * f
        }
    }

    /** The horizontal shake offset in dp at [ms]: three swings, peak +-6 dp, 0 at the start and from 400 ms on. */
    fun shakeOffset(ms: Long): Double =
        if (ms < 0L || ms >= SHAKE_MS) 0.0 else SHAKE_AMPLITUDE_DP * Math.sin(2.0 * Math.PI * SHAKE_CYCLES * ms / SHAKE_MS)
}

internal fun pulseRadius(ms: Long, reduced: Boolean): Double = PlayTiming.pulseRadius(ms, reduced)

internal fun shakeOffset(ms: Long): Double = PlayTiming.shakeOffset(ms)
