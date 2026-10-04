package io.github.jamisuni.tangram

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.AndroidComposeUiFlags
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoundEffect
import androidx.compose.ui.platform.SoundEffect
import java.util.concurrent.atomic.AtomicInteger

/**
 * The platform feedback lever (decisions DA-116, DA-125; design WO-007 section 3.7). The toolkit asks the system for a click
 * sound on every tap and for a haptic on a long press; this app wants neither, sound on or off (REQ-033 A1). A root provider
 * answers both requests with silent objects that only count. This is the ONLY file that may name `LocalSoundEffect`,
 * `LocalHapticFeedback` and `AndroidComposeUiFlags` (the call-path scan).
 *
 * Precondition: no Dialog or Popup window in release (each re-provides both locals; the REQ-008 A2 walk enforces it).
 * Guarded on the whole APK by V-08's feedback-caller check (`.swdev/verifiers/v08_promise_apk.py`, FEEDBACK_ALLOW): any new
 * caller of the platform sound or haptic APIs fails that check.
 */
object PlatformFeedbackLever {
    private val clicks = AtomicInteger()
    private val haptics = AtomicInteger()

    /** Click requests a node made and the lever answered (the positive control for tests). */
    internal val interceptedClicks: Int get() = clicks.get()

    /** Haptic requests a node made and the lever answered. */
    internal val interceptedHaptics: Int get() = haptics.get()

    /** Silences the keyboard and D-pad focus sounds. Call first in `onCreate`, before `setContent`. */
    @OptIn(ExperimentalComposeUiApi::class)
    fun installProcessFlags() {
        AndroidComposeUiFlags.isInteractionSoundEffectsEnabled = false
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun Provide(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalSoundEffect provides SilentSoundEffect,
            LocalHapticFeedback provides SilentHaptic,
            content = content,
        )
    }

    private object SilentSoundEffect : SoundEffect {
        override fun playClickSound() {
            if (clicks.get() < Int.MAX_VALUE) clicks.incrementAndGet()
        }
    }

    private object SilentHaptic : HapticFeedback {
        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            if (haptics.get() < Int.MAX_VALUE) haptics.incrementAndGet()
        }
    }
}
