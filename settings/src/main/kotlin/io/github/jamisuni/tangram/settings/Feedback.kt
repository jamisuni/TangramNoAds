package io.github.jamisuni.tangram.settings

/**
 * The one gate between game events and the sound and haptic outputs (DA-116).
 * Sound off: no call on either out. Sound on: one cue, and a tick only for LOCK.
 * Never throws (G-10): a failing out is swallowed.
 */
class Feedback(
    private val soundOn: () -> Boolean,
    private val sound: SoundOut,
    private val haptic: HapticOut,
) {
    fun on(event: FeedbackEvent) {
        val enabled = try {
            soundOn()
        } catch (_: Throwable) {
            return
        }
        if (!enabled) return
        try {
            sound.play(event)
        } catch (_: Throwable) {
        }
        if (event == FeedbackEvent.LOCK) {
            try {
                haptic.tick()
            } catch (_: Throwable) {
            }
        }
    }
}
