package io.github.jamisuni.tangram.settings

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Module-level cover of REQ-033 A1 (design WO-007 section 3.6 item 1, acceptance table): the one gate `Feedback.on` makes no call
 * on either output while sound is off, and exactly the stated calls while it is on (the positive control). The device half
 * (the probe, the lever) is held out; this is the JVM half.
 *
 * Basis, all from the locked text and the frozen seam: REQ-033 Statement ("SHALL play no sound WHILE sound is switched off"),
 * REQ-033 rule ("A lock also gives a short haptic tick ... the tick follows the sound switch"), the design's value shape
 * ("Feedback.on with soundOn() == false makes no call on either out; with it true, exactly one sound.play(event), and exactly one
 * haptic.tick() iff event == LOCK") and section 3.2 (the switch is read through `soundOn()` for every event, each out call in
 * runCatching, G-10).
 */
class FeedbackGateTest {

    private val allEvents = FeedbackEvent.values().toList()

    // REQ-033.A1 - "With sound off, no action makes a sound or a haptic tick."
    // Every one of the five actions, each through its own gate: sound off gives 0 sounds and 0 ticks (LOCK included: its tick follows
    // the switch, REQ-033 rule).
    @Test
    fun req033_A1_withSoundOffNoEventMakesASoundOrATick() {
        assertEquals("fixture: the five actions of REQ-033 are five events", 5, allEvents.size)
        for (e in allEvents) {
            val sound = RecordingSound()
            val haptic = RecordingHaptic()
            Feedback(soundOn = { false }, sound = sound, haptic = haptic).on(e)
            assertEquals("$e: a sound request reached the output with sound off", emptyList<FeedbackEvent>(), sound.played)
            assertEquals("$e: a haptic tick reached the output with sound off", 0, haptic.ticks)
        }
    }

    // REQ-033.A1 - positive control for the test above: the same gate with sound on does reach the outputs, so the recorders can
    // see a request and "0" above means "gated", not "blind". Exactly one sound per event, and one tick only for the lock.
    @Test
    fun req033_A1_withSoundOnEachEventMakesExactlyOneSoundAndOnlyALockTicks() {
        for (e in allEvents) {
            val sound = RecordingSound()
            val haptic = RecordingHaptic()
            Feedback(soundOn = { true }, sound = sound, haptic = haptic).on(e)
            assertEquals("$e: exactly one sound, for that event", listOf(e), sound.played)
            assertEquals("$e: a tick exactly for the lock", if (e == FeedbackEvent.LOCK) 1 else 0, haptic.ticks)
        }
    }

    // REQ-033.A1 - the switch is read for every event, not once at construction: flipped between two events, the NEXT one obeys at
    // once (a lazy gate that captured the value when it was built would keep sounding, or stay silent, after the player's tap).
    @Test
    fun req033_A1_aSwitchFlippedBetweenEventsChangesTheNextEventAtOnce() {
        var on = true
        val sound = RecordingSound()
        val haptic = RecordingHaptic()
        val gate = Feedback(soundOn = { on }, sound = sound, haptic = haptic)

        gate.on(FeedbackEvent.LOCK)
        assertEquals(listOf(FeedbackEvent.LOCK), sound.played)
        assertEquals(1, haptic.ticks)

        on = false
        gate.on(FeedbackEvent.LOCK)
        gate.on(FeedbackEvent.SOLVE)
        gate.on(FeedbackEvent.TURN)
        assertEquals("nothing more after the switch went off", listOf(FeedbackEvent.LOCK), sound.played)
        assertEquals("not even the lock tick", 1, haptic.ticks)

        on = true
        gate.on(FeedbackEvent.RETURN)
        assertEquals("sound resumes with the next event after the switch went on", listOf(FeedbackEvent.LOCK, FeedbackEvent.RETURN), sound.played)
        gate.on(FeedbackEvent.LOCK)
        assertEquals("the tick resumes with the lock", 2, haptic.ticks)
    }

    // REQ-033.A1 - a long mixed run of every event with sound off stays at zero (a counter that leaks on the Nth call, or an event
    // that is only gated the first time, would show here).
    @Test
    fun req033_A1_aLongRunOfEveryEventWithSoundOffStaysSilent() {
        val sound = RecordingSound()
        val haptic = RecordingHaptic()
        val gate = Feedback(soundOn = { false }, sound = sound, haptic = haptic)
        repeat(20) { for (e in allEvents) gate.on(e) }
        assertEquals(emptyList<FeedbackEvent>(), sound.played)
        assertEquals(0, haptic.ticks)
    }

    // guardrail G-10 (design section 3.2: "Each out call is in runCatching"; seam: `Feedback.on` never throws): an output that fails
    // never reaches the caller, and a failing sound does not stop the lock tick (each call is wrapped on its own).
    @Test
    fun guardrailG10_aFailingOutNeverThrowsAndDoesNotStopTheOtherOut() {
        val failingSound = ThrowingSound()
        val haptic = RecordingHaptic()
        val gate = Feedback(soundOn = { true }, sound = failingSound, haptic = haptic)
        for (e in allEvents) gate.on(e) // must not throw
        assertEquals(5, failingSound.attempts)
        assertEquals("the lock tick is still requested when the sound out fails", 1, haptic.ticks)

        val sound = RecordingSound()
        val failingHaptic = ThrowingHaptic()
        val gate2 = Feedback(soundOn = { true }, sound = sound, haptic = failingHaptic)
        for (e in allEvents) gate2.on(e) // must not throw
        assertEquals(allEvents, sound.played)
        assertEquals(1, failingHaptic.attempts)
    }

    // guardrail G-10 + REQ-033: with sound off, a failing out is not even asked (the gate stops first).
    @Test
    fun guardrailG10_withSoundOffAFailingOutIsNeverCalled() {
        val s = ThrowingSound()
        val h = ThrowingHaptic()
        val gate = Feedback(soundOn = { false }, sound = s, haptic = h)
        for (e in allEvents) gate.on(e)
        assertEquals(0, s.attempts)
        assertEquals(0, h.attempts)
    }
}
