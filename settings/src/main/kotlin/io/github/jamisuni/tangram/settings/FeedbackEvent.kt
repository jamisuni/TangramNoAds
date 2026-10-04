package io.github.jamisuni.tangram.settings

/** The five game events that can sound (design WO-007 3.1). `app` maps `play`'s own enum onto this one. */
enum class FeedbackEvent { PICK_UP, TURN, LOCK, RETURN, SOLVE }

/** Plays one cue. Implemented by the Android output (TASK-052); a fake in tests. */
interface SoundOut {
    fun play(event: FeedbackEvent)
}

/** One short haptic tick, without any permission. */
interface HapticOut {
    fun tick()
}
