package io.github.jamisuni.tangram

import android.animation.ValueAnimator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.jamisuni.tangram.browse.BrowseController
import io.github.jamisuni.tangram.content.PuzzleLibrary
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

    init {
        host.onChanged = controller::persist
        controller.start()
    }

    class Factory(private val filesDir: File) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T = AppViewModel(filesDir) as T
    }
}
