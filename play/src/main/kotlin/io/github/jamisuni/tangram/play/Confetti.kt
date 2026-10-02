package io.github.jamisuni.tangram.play

// WO-003 design section 2 (DA-19): 110 confetti rectangles for 2.4 s from the board centre. Each particle is a
// closed-form ballistic function of time from a fixed-seed generator, so the same time always gives the same picture.

/** Offset in dp from the board centre, rotation in radians, size in dp, colour as 0xRRGGBB, alpha 0..1. */
internal data class ConfettiParticle(
    val dx: Double, val dy: Double, val rotation: Double, val width: Double, val height: Double,
    val colour: Int, val alpha: Double,
)

internal object Confetti {
    private const val SEED = 20241002L
    private const val GRAVITY = 900.0 // dp / s^2
    private val colours = intArrayOf(0x3D8BFD, 0xE63946, 0xFFC857, 0x2EC4B6, 0xFF9F1C, 0x9B5DE5, 0x4CAF50)

    private class Spec(
        val vx: Double, val vy: Double, val spin: Double, val phase: Double,
        val w: Double, val h: Double, val colour: Int,
    )

    private val specs: List<Spec> = run {
        val r = java.util.Random(SEED)
        List(PlayTiming.CONFETTI_COUNT) {
            val angle = Math.PI * (0.15 + 0.7 * r.nextDouble()) // upward fan
            val speed = 350.0 + 450.0 * r.nextDouble()
            Spec(
                vx = Math.cos(angle) * speed * (if (r.nextBoolean()) 1 else -1),
                vy = -Math.sin(angle) * speed,
                spin = (r.nextDouble() - 0.5) * 16.0,
                phase = r.nextDouble() * Math.PI * 2,
                w = 6.0 + 6.0 * r.nextDouble(),
                h = 3.0 + 4.0 * r.nextDouble(),
                colour = colours[r.nextInt(colours.size)],
            )
        }
    }

    /** Particles at [ms] since the last lock; empty outside 0 until 2400 ms. */
    fun at(ms: Long): List<ConfettiParticle> {
        if (ms < 0L || ms >= PlayTiming.CONFETTI_MS) return emptyList()
        val t = ms / 1000.0
        // Fade over the last 400 ms.
        val fade = ((PlayTiming.CONFETTI_MS - ms) / 400.0).coerceIn(0.0, 1.0)
        return specs.map {
            ConfettiParticle(
                dx = it.vx * t,
                dy = it.vy * t + 0.5 * GRAVITY * t * t,
                rotation = it.phase + it.spin * t,
                width = it.w, height = it.h, colour = it.colour, alpha = fade,
            )
        }
    }
}
