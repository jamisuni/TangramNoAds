package io.github.jamisuni.tangram.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// SCAFFOLDING (disposable, TASK-051): decision DA-114. Not an acceptance test.
class SoundSynthScaffoldingTest {
    @Test
    fun cuesAreBoundedAndEndAtZero() {
        for (rate in listOf(44_100, 48_000)) for (e in FeedbackEvent.entries) {
            val p = SoundSynth.pcm(e, rate)
            assertTrue(SoundSynth.durationMs(e) <= 1000)
            assertEquals(rate.toLong() * SoundSynth.durationMs(e) / 1000, p.size.toLong())
            assertEquals(0, p.last().toInt())
            val peak = p.maxOf { kotlin.math.abs(it.toInt()) } / 32767.0
            assertTrue("$e $peak", peak <= 0.35 && peak > 0.05)
        }
    }

    @Test
    fun solveStartsWithSilence() {
        val p = SoundSynth.pcm(FeedbackEvent.SOLVE, 48_000)
        for (i in 0 until 4800) assertEquals(0, p[i].toInt())
    }

    @Test
    fun gateBehaviour() {
        val sounds = mutableListOf<FeedbackEvent>()
        var ticks = 0
        var on = false
        val f = Feedback({ on }, object : SoundOut { override fun play(event: FeedbackEvent) { sounds += event; error("x") } },
            object : HapticOut { override fun tick() { ticks++ } })
        FeedbackEvent.entries.forEach { f.on(it) }
        assertTrue(sounds.isEmpty() && ticks == 0)
        on = true
        FeedbackEvent.entries.forEach { f.on(it) }
        assertEquals(5, sounds.size)
        assertEquals(1, ticks)
    }
}
