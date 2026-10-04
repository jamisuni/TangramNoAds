package io.github.jamisuni.tangram.settings

import android.view.HapticFeedbackConstants
import android.view.View
import java.lang.ref.WeakReference

/**
 * The haptic tick without a permission (decision DA-114, design WO-007 3.4): `View.performHapticFeedback`,
 * no Vibrator. Holds only a weak reference to the view (the ViewModel outlives the Activity).
 * It follows the user's system touch-feedback setting; no override flag is passed.
 */
class ViewHapticOut : HapticOut {
    @Volatile private var view: WeakReference<View>? = null

    fun attach(view: View) {
        this.view = WeakReference(view)
    }

    /** Clears only if the held view is [view], so a late onDestroy of an old activity cannot detach the new one. */
    fun detach(view: View) {
        synchronized(this) {
            if (this.view?.get() === view) this.view = null
        }
    }

    /** Unconditional detach, kept for compatibility. */
    fun detach() {
        view = null
    }

    override fun tick() {
        runCatching { view?.get()?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    }
}
