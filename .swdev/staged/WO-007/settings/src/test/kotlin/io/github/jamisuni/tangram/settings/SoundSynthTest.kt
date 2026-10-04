package io.github.jamisuni.tangram.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * decision DA-114 (design WO-007 section 3.3 and the value shapes): the synthesized cues obey the locked limits, whatever the owner's ear
 * later does to the tones. Basis, each from a locked line or the design's pinned shape (never from a particular frequency, which the
 * design calls a tunable starting value):
 *   - REQ-033 rule "Each effect lasts at most 1 s", and F26 reads REQ-023's chime ("a 4-note chime ... plays with it") the same way;
 *   - REQ-033 rule "a return to the tray uses a soft low tone" and REQ-020 rule ("plays a soft sound"): the design pins the RETURN cue's
 *     fundamental at most 300 Hz and the lowest of the five;
 *   - the design's value shapes: each cue's peak at most 0.35 of full scale, the last sample 0, SOLVE begins with 100 ms of silence
 *     (so that LOCK, which is played first by a completing drop, ends before SOLVE sounds), checked at 44 100 and 48 000 Hz.
 * No acceptance token: the REQ lines above are Rules, not acceptance criteria.
 */
class SoundSynthTest {

    private val rates = listOf(44_100, 48_000)
    private val events = FeedbackEvent.values().toList()

    private fun peak(pcm: ShortArray): Double = pcm.maxOf { abs(it.toInt()) } / 32767.0

    /** Zero-crossing estimate of the fundamental over the audible part of the cue (above 5 % of its own peak). */
    private fun estimatedHz(pcm: ShortArray, rate: Int): Double {
        val top = pcm.maxOf { abs(it.toInt()) }
        val threshold = top * 0.05
        val first = pcm.indexOfFirst { abs(it.toInt()) >= threshold }
        val last = pcm.indexOfLast { abs(it.toInt()) >= threshold }
        check(first in 0..last) { "the cue has no audible part" }
        var crossings = 0
        var previous = 0
        for (i in first..last) {
            val s = pcm[i].toInt()
            val sign = if (s > 0) 1 else if (s < 0) -1 else 0
            if (sign != 0) {
                if (previous != 0 && sign != previous) crossings++
                previous = sign
            }
        }
        return crossings / 2.0 / ((last - first + 1) / rate.toDouble())
    }

    // REQ-033 rule "Each effect lasts at most 1 s": the declared length and the real sample count, every cue, both rates.
    @Test
    fun decisionDA114_everyCueLastsAtMostOneSecond() {
        for (rate in rates) for (e in events) {
            val pcm = SoundSynth.pcm(e, rate)
            assertTrue("$e @$rate: empty cue", pcm.isNotEmpty())
            val ms = SoundSynth.durationMs(e)
            assertTrue("$e: durationMs=$ms must be in 1..1000", ms in 1..1000)
            assertTrue("$e @$rate: ${pcm.size} samples last more than 1 s", pcm.size <= rate)
            // the declared length is the sample count, to a couple of milliseconds
            val realMs = pcm.size * 1000.0 / rate
            assertTrue("$e @$rate: durationMs=$ms but the samples last $realMs ms", abs(realMs - ms) <= 2.0)
        }
    }

    // design value shape: "each cue's peak at most 0.35 of full scale"; and no cue is silent (a silent cue would pass the cap and the
    // 1 s limit trivially, so the positive control is that something is audible).
    @Test
    fun decisionDA114_everyCuePeaksAtMostAtThirtyFivePercentAndIsAudible() {
        for (rate in rates) for (e in events) {
            val p = peak(SoundSynth.pcm(e, rate))
            assertTrue("$e @$rate: peak $p exceeds 0.35 of full scale", p <= 0.35 + 1.0 / 32767)
            assertTrue("$e @$rate: the cue is silent", p > 0.0)
        }
    }

    // design section 3.3: "an exponential decay to zero (the last sample is 0)": no click at the end of a cue.
    @Test
    fun decisionDA114_everyCueEndsOnASampleOfZero() {
        for (rate in rates) for (e in events) {
            val pcm = SoundSynth.pcm(e, rate)
            assertEquals("$e @$rate: last sample", 0, pcm.last().toInt())
        }
    }

    // REQ-033 rule "No sound signals failure: a return to the tray uses a soft low tone" + REQ-020 rule: the design pins the RETURN
    // cue's fundamental at most 300 Hz and the lowest of the five. Measured by zero crossings with a 5 % tolerance (a constant 300 Hz
    // tone measures within a few Hz of 300).
    @Test
    fun decisionDA114_theReturnCueIsALowToneAndTheLowestOfTheFive() {
        for (rate in rates) {
            val hz = events.associateWith { estimatedHz(SoundSynth.pcm(it, rate), rate) }
            val back = hz.getValue(FeedbackEvent.RETURN)
            assertTrue("RETURN @$rate: fundamental ~$back Hz is above 300", back <= 300.0 * 1.05)
            for ((e, f) in hz) {
                if (e == FeedbackEvent.RETURN) continue
                assertTrue("RETURN (~$back Hz) must be lower than $e (~$f Hz) @$rate", back < f)
            }
        }
    }

    // design section 3.3 / value shape: "SOLVE begins with 100 ms of silence", so the LOCK click, played first by a completing drop,
    // has ended before the chime sounds (the two never overlap and the mix never exceeds one cue's peak).
    @Test
    fun decisionDA114_solveBeginsWithOneHundredMillisecondsOfSilenceAndLockEndsInsideIt() {
        for (rate in rates) {
            val solve = SoundSynth.pcm(FeedbackEvent.SOLVE, rate)
            val lead = rate / 10 // 100 ms
            for (i in 0 until lead - 1) assertEquals("SOLVE @$rate: sample $i must be silent", 0, solve[i].toInt())
            val firstSound = solve.indexOfFirst { it.toInt() != 0 }
            assertTrue("SOLVE @$rate: the chime starts at sample $firstSound, before the 100 ms lead-in", firstSound >= lead - 1)
            assertTrue("SOLVE @$rate: no sound at all", firstSound >= 0)
        }
        assertTrue(
            "LOCK lasts ${SoundSynth.durationMs(FeedbackEvent.LOCK)} ms, longer than SOLVE's 100 ms lead-in: they would overlap",
            SoundSynth.durationMs(FeedbackEvent.LOCK) <= 100,
        )
    }

    // REQ-023 rule "a 4-note chime plays with it": the chime is the longest cue (four notes cannot fit in a click), still within 1 s.
    @Test
    fun decisionDA114_theSolveChimeIsTheLongestCue() {
        val solve = SoundSynth.durationMs(FeedbackEvent.SOLVE)
        for (e in events) if (e != FeedbackEvent.SOLVE) {
            assertTrue("SOLVE ($solve ms) must be longer than $e (${SoundSynth.durationMs(e)} ms)", solve > SoundSynth.durationMs(e))
        }
        assertTrue(solve <= 1000)
    }

    // REQ-033 Statement (a sound for each of five actions): the five cues are five different sounds, and the cue depends on the
    // sample rate only by its sampling (the same duration at both rates).
    @Test
    fun decisionDA114_theFiveCuesDiffer() {
        for (rate in rates) {
            val all = events.map { SoundSynth.pcm(it, rate).toList() }
            assertEquals("two events share one sound @$rate", all.size, all.toSet().size)
        }
        for (e in events) {
            val a = SoundSynth.pcm(e, 44_100).size * 1000.0 / 44_100
            val b = SoundSynth.pcm(e, 48_000).size * 1000.0 / 48_000
            assertTrue("$e: ${a} ms at 44.1 kHz but $b ms at 48 kHz", abs(a - b) <= 2.0)
        }
    }
}
