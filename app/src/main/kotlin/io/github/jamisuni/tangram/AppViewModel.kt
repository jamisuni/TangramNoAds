package io.github.jamisuni.tangram

import android.animation.ValueAnimator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.jamisuni.tangram.browse.BrowseController
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.settings.AudioTrackSoundOut
import io.github.jamisuni.tangram.settings.Feedback
import io.github.jamisuni.tangram.settings.SettingsController
import io.github.jamisuni.tangram.settings.ViewHapticOut
import io.github.jamisuni.tangram.store.JsonProgressStore
import java.io.File

/**
 * The composition root's state (WO-004 design section 7): library, store, session host and browse controller.
 * It survives rotation (DA-26 ended, decisions F3), so the session keeps every piece.
 */
class AppViewModel(filesDir: File) : ViewModel() {
    val library: PuzzleLibrary = PuzzleLibrary.packaged()

    /** Concrete type so `MainActivity.onStop` can call `sync()` (DA-47). */
    val store: JsonProgressStore = JsonProgressStore(filesDir)

    val host = SessionHost(reducedMotion = { !ValueAnimator.areAnimatorsEnabled() })

    val controller = BrowseController(library, store, host)

    /** The testing aid's state holder (REQ-046, DA-76): a ViewModel field, so it survives rotation and browsing. */
    val aids = DebugAids()

    /** The settings overlay's state (REQ-032..034): a field here so it survives rotation. */
    val settings = SettingsController(
        store = store,
        canOpen = { host.session?.isDragging != true },
        onReset = { controller.afterReset() },
    )

    private val audio = AudioTrackSoundOut()

    /** The window view for the haptic tick; `MainActivity` attaches and detaches it. */
    val haptics = ViewHapticOut()

    /** The one gate to sound and haptics (DA-116); the outs are wrapped by the debug probe in debug builds only. */
    val feedback = Feedback(
        soundOn = { settings.soundOn },
        sound = aids.sound(audio),
        haptic = aids.haptic(haptics),
    )

    init {
        host.onChanged = controller::persist
        // G-10 (CR-6 N9): nothing may throw into the play loop, so the mapping is guarded too
        host.onEvent = { event -> runCatching { feedback.on(event.toFeedback()) } }
        audio.prepare()
        controller.start()
    }

    override fun onCleared() {
        audio.release()
        super.onCleared()
    }

    class Factory(private val filesDir: File) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T = AppViewModel(filesDir) as T
    }
}
