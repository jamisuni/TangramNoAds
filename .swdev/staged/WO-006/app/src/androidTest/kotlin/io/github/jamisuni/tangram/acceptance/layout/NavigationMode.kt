package io.github.jamisuni.tangram.acceptance.layout

import android.os.Build

// Scaffolding helper (decision DA-98): forces gesture navigation on API 29+ and puts the SAVED PRIOR mode back. The switch is
// asynchronous (about 6 s after `cmd overlay enable-exclusive` on API 37), so both directions poll up to 15 s and fail loudly on
// timeout. Values of Settings.Secure "navigation_mode": 0 three-button, 1 two-button, 2 gestural. Below API 29 no gesture navigation
// exists: ensureGesture() returns false and touches nothing (the callers then report the rule-level wording of DA-98).
internal class NavigationMode {
    private var prior: String? = null

    private fun category(mode: Int) = when (mode) {
        0 -> "com.android.internal.systemui.navbar.threebutton"
        1 -> "com.android.internal.systemui.navbar.twobutton"
        2 -> "com.android.internal.systemui.navbar.gestural"
        else -> error("unknown navigation mode $mode")
    }

    private fun current(): String = DeviceShell.getSetting("secure", "navigation_mode")

    /** Forces [mode] and waits for it (used by the scaffolding test to start from a state that is not the baseline). */
    fun switchTo(mode: Int) {
        DeviceShell.run("cmd overlay enable-exclusive --category ${category(mode)}")
        DeviceShell.waitUntil("navigation_mode to become $mode", 15_000) { current() == mode.toString() }
    }

    /** True when gesture navigation (mode 2) is active afterwards; false below API 29 (nothing changed). */
    fun ensureGesture(): Boolean {
        if (Build.VERSION.SDK_INT < 29) return false
        val now = current()
        if (now !in listOf("0", "1", "2")) error("navigation_mode reads '$now' on API ${Build.VERSION.SDK_INT}: cannot save a prior mode to restore")
        if (prior == null) prior = now
        if (now != "2") switchTo(2)
        return true
    }

    /** Puts the saved prior mode back (no-op when it was already gesture mode or nothing was changed). */
    fun restore() {
        val was = prior ?: return
        if (current() != was) switchTo(was.toInt())
        prior = null // forgotten only once the switch is confirmed: a timed-out restore can be retried
    }
}
