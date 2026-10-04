package io.github.jamisuni.tangram

import io.github.jamisuni.tangram.settings.FeedbackEvent
import io.github.jamisuni.tangram.settings.HapticOut
import io.github.jamisuni.tangram.settings.SoundOut
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Debug only (decision DA-116): counts what reaches the sound and haptic outs, per kind, so a device test can see that a
 * muted game sends nothing. No log; every call is passed on to the real out. Absent from release (DebugAids twin).
 */
object FeedbackProbe {
    private val sounds = ConcurrentHashMap<FeedbackEvent, AtomicInteger>()
    private val tickCount = AtomicInteger()

    val soundCounts: Map<FeedbackEvent, Int>
        get() = FeedbackEvent.values().associateWith { sounds[it]?.get() ?: 0 }

    val ticks: Int get() = tickCount.get()

    fun reset() {
        sounds.clear()
        tickCount.set(0)
    }

    fun wrapSound(real: SoundOut): SoundOut = object : SoundOut {
        override fun play(event: FeedbackEvent) {
            sounds.getOrPut(event) { AtomicInteger() }.incrementAndGet()
            real.play(event)
        }
    }

    fun wrapHaptic(real: HapticOut): HapticOut = object : HapticOut {
        override fun tick() {
            tickCount.incrementAndGet()
            real.tick()
        }
    }
}
