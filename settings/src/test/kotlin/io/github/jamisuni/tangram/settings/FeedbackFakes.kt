package io.github.jamisuni.tangram.settings

/**
 * ACCEPTANCE-TEST ADAPTERS (WO-007 T7c), written from the seam table only (`SoundOut.play(event)`, `HapticOut.tick()`): recording
 * outs for the JVM gate test. They record, they never filter, so the test sees every request that reaches the output boundary.
 */
internal class RecordingSound : SoundOut {
    val played = ArrayList<FeedbackEvent>()
    override fun play(event: FeedbackEvent) {
        played += event
    }
}

internal class RecordingHaptic : HapticOut {
    var ticks = 0
        private set
    override fun tick() {
        ticks++
    }
}

/** An out that throws on every request (G-10: an output failure never reaches the caller). */
internal class ThrowingSound : SoundOut {
    var attempts = 0
        private set
    override fun play(event: FeedbackEvent) {
        attempts++
        error("sound out failed")
    }
}

internal class ThrowingHaptic : HapticOut {
    var attempts = 0
        private set
    override fun tick() {
        attempts++
        error("haptic out failed")
    }
}
