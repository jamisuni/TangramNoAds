package io.github.jamisuni.tangram.acceptance.held

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry

// Scaffolding helper (decision DA-98): the bottom system area, read from the real window insets. "Gesture bar" is read as the larger
// of the stable navigation-bar inset and, on API 29+, the system-gesture and mandatory-gesture insets (design WO-006 section 2).
internal class BottomInsets(val stable: Int, val systemGesture: Int, val mandatoryGesture: Int) {
    /** The exclusion: the larger of the three (the two gesture values are 0 below API 29). */
    val exclusion: Int get() = maxOf(stable, systemGesture, mandatoryGesture)

    override fun toString() = "stable=$stable systemGesture=$systemGesture mandatoryGesture=$mandatoryGesture exclusion=$exclusion"
}

internal object GestureInsets {
    private const val TAG = "WO006Insets"

    /** The three bottom insets of [activity]'s window, logged; asserts nothing. Loud when the window has no insets yet. */
    @Suppress("DEPRECATION")
    fun read(activity: Activity): BottomInsets {
        var out: BottomInsets? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val insets = activity.window.decorView.rootWindowInsets
            if (insets != null) {
                val stable = insets.stableInsetBottom
                val system = if (Build.VERSION.SDK_INT >= 29) insets.systemGestureInsets.bottom else 0
                val mandatory = if (Build.VERSION.SDK_INT >= 29) insets.mandatorySystemGestureInsets.bottom else 0
                out = BottomInsets(stable, system, mandatory)
            }
        }
        val r = out ?: error("the activity window has no root window insets")
        Log.i(TAG, "bottom insets on API ${Build.VERSION.SDK_INT}: $r")
        return r
    }

    /** The exclusion in px; `error()` when it is 0 (a phone display always has a bottom system area). */
    fun bottomExclusionPx(activity: Activity): Int {
        val e = read(activity).exclusion
        if (e == 0) error("the bottom exclusion is 0: no navigation bar or gesture area was found (is the window edge to edge on a phone display?)")
        return e
    }
}
