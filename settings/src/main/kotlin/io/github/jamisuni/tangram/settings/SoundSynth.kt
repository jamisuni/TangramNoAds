package io.github.jamisuni.tangram.settings

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Pure synthesis of the five cues, mono 16-bit (decision DA-114, design WO-007 3.3).
 * The values are tunable constants (the owner's ear judges), not requirements.
 * Each cue: 3 ms attack, exponential decay, a short linear fade-out so the last sample is 0,
 * and a peak of at most [Cue.peak] (at most 0.35 of full scale).
 */
internal object SoundSynth {
    private const val ATTACK_MS = 3.0
    private const val FADE_OUT_MS = 4.0

    private enum class Wave { SINE, TRIANGLE }

    /** One voice: starts [startMs] into the cue, lasts [lengthMs], glides [fromHz] to [toHz]. */
    private class Voice(
        val wave: Wave,
        val fromHz: Double,
        val toHz: Double,
        val startMs: Double,
        val lengthMs: Double,
        val gain: Double = 1.0,
    )

    private class Cue(val totalMs: Int, val peak: Double, val voices: List<Voice>)

    private val cues: Map<FeedbackEvent, Cue> = mapOf(
        FeedbackEvent.PICK_UP to Cue(70, 0.18, listOf(Voice(Wave.SINE, 520.0, 700.0, 0.0, 70.0))),
        FeedbackEvent.TURN to Cue(40, 0.12, listOf(Voice(Wave.SINE, 760.0, 760.0, 0.0, 40.0))),
        FeedbackEvent.LOCK to Cue(
            90, 0.22,
            listOf(
                Voice(Wave.TRIANGLE, 1320.0, 1320.0, 0.0, 90.0),
                Voice(Wave.SINE, 1760.0, 1760.0, 30.0, 60.0),
            ),
        ),
        FeedbackEvent.RETURN to Cue(180, 0.16, listOf(Voice(Wave.SINE, 300.0, 180.0, 0.0, 180.0))),
        FeedbackEvent.SOLVE to Cue(
            670, 0.25,
            listOf(523.0, 659.0, 784.0, 1047.0).mapIndexed { i, hz ->
                Voice(Wave.TRIANGLE, hz, hz, 100.0 + 110.0 * i, 240.0)
            },
        ),
    )

    fun durationMs(event: FeedbackEvent): Int = cues.getValue(event).totalMs

    fun pcm(event: FeedbackEvent, sampleRate: Int): ShortArray {
        require(sampleRate > 0) { "sampleRate must be positive" }
        val cue = cues.getValue(event)
        val n = (sampleRate.toLong() * cue.totalMs / 1000).toInt()
        val mix = DoubleArray(n)
        for (v in cue.voices) {
            val first = (v.startMs * sampleRate / 1000.0).roundToInt()
            val len = (v.lengthMs * sampleRate / 1000.0).roundToInt()
            var phase = 0.0
            for (k in 0 until len) {
                val i = first + k
                if (i >= n) break
                val t = k.toDouble() / sampleRate
                val frac = k.toDouble() / len
                val hz = v.fromHz + (v.toHz - v.fromHz) * frac
                phase += 2.0 * PI * hz / sampleRate
                val raw = when (v.wave) {
                    Wave.SINE -> sin(phase)
                    Wave.TRIANGLE -> 2.0 / PI * asin(sin(phase))
                }
                val attack = min(1.0, t * 1000.0 / ATTACK_MS)
                val decay = exp(-5.0 * frac)
                mix[i] += raw * attack * decay * v.gain
            }
        }
        // Short linear fade-out so the last sample is exactly 0.
        val fade = (FADE_OUT_MS * sampleRate / 1000.0).roundToInt().coerceIn(1, n)
        for (j in 0 until fade) mix[n - 1 - j] *= j.toDouble() / fade
        var maxAbs = 0.0
        for (x in mix) maxAbs = maxOf(maxAbs, abs(x))
        val scale = if (maxAbs > 0.0) cue.peak * Short.MAX_VALUE / maxAbs else 0.0
        return ShortArray(n) { (mix[it] * scale).roundToInt().toShort() }
    }
}
