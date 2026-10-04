package io.github.jamisuni.tangram.acceptance.held

import android.app.UiAutomation
import androidx.test.platform.app.InstrumentationRegistry

// Scaffolding helper (decision DA-96 / DA-98): freezes the display rotation through UiAutomation and puts the SAVED PRIOR
// `accelerometer_rotation` and `user_rotation` back, never UNFREEZE (which would switch auto-rotate on whatever it was) and never an
// assumed value: an unset value is deleted again (device_reset.py accepts null as the 1 / 0 baselines). Fails loudly.
internal class Rotation {
    private var savedAccelerometer: String? = null
    private var savedUser: String? = null

    private fun saveOnce() {
        if (savedAccelerometer == null) {
            val acc = DeviceShell.getSetting("system", "accelerometer_rotation")
            val user = DeviceShell.getSetting("system", "user_rotation")
            savedUser = user // the pair is stored whole: savedAccelerometer is the "saved" flag, set last
            savedAccelerometer = acc
        }
    }

    private fun freeze(mode: Int, what: String) {
        saveOnce()
        if (!InstrumentationRegistry.getInstrumentation().uiAutomation.setRotation(mode)) error("UiAutomation.setRotation($what) was refused")
    }

    fun freeze0() = freeze(UiAutomation.ROTATION_FREEZE_0, "FREEZE_0")

    /** Freezes at 90 degrees and waits until the real window shows the swapped orientation (loud on timeout). */
    fun freeze90() {
        val before = RealDisplay.sizePx()
        freeze(UiAutomation.ROTATION_FREEZE_90, "FREEZE_90")
        DeviceShell.waitUntil("the real window to turn (was ${before.first}x${before.second})", 10_000) {
            val now = RealDisplay.sizePx()
            (now.first > now.second) != (before.first > before.second)
        }
    }

    /** Puts the saved prior values back and verifies the read-back. No-op when nothing was frozen. */
    fun restore() {
        val acc = savedAccelerometer ?: return
        val user = savedUser ?: error("saved user_rotation missing")
        DeviceShell.restoreSetting("system", "user_rotation", user, "0")
        DeviceShell.restoreSetting("system", "accelerometer_rotation", acc, "1")
        val accNow = DeviceShell.getSetting("system", "accelerometer_rotation")
        val userNow = DeviceShell.getSetting("system", "user_rotation")
        val accOk = accNow == acc || (acc == "null" && accNow == "1")
        val userOk = userNow == user || (user == "null" && userNow == "0")
        if (accOk && userOk) {
            savedAccelerometer = null
            savedUser = null
        }
        if (!accOk || !userOk) error("rotation settings not restored: accelerometer_rotation $acc -> $accNow, user_rotation $user -> $userNow")
    }
}
