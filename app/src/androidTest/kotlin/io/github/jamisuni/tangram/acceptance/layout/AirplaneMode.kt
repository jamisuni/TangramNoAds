package io.github.jamisuni.tangram.acceptance.layout

import android.os.Build
import android.util.Log

// Scaffolding helper (decision DA-105): the test turns airplane mode on itself and restores the SAVED PRIOR state.
// API 30+: `cmd connectivity airplane-mode enable|disable`. API 26-29: no such command (the system broadcast needs root), so the
// settings flag is set and mobile data is turned off with `svc data disable`, then both are put back. On the API 26 image used here
// `svc wifi` is killed (rc 137): Wi-Fi cannot be cut there, so it is not attempted; that is LOGGED (tag WO006Airplane), not an
// error: DA-105 rests the claim on the absence of the INTERNET permission, the run shows device behaviour.
internal class AirplaneMode {
    private var priorFlag: String? = null
    private var priorData: String? = null

    /** What the last [on] could not do (empty when everything the API level allows was done). */
    var limits: String = ""
        private set

    fun isOn(): Boolean = DeviceShell.getSetting("global", "airplane_mode_on") == "1"

    fun on() {
        if (priorFlag != null) error("AirplaneMode.on() called twice without restore()")
        priorFlag = DeviceShell.getSetting("global", "airplane_mode_on")
        if (Build.VERSION.SDK_INT >= 30) {
            DeviceShell.run("cmd connectivity airplane-mode enable")
            limits = ""
        } else {
            priorData = DeviceShell.getSetting("global", "mobile_data")
            DeviceShell.putSetting("global", "airplane_mode_on", "1")
            DeviceShell.run("svc data disable")
            limits = "API ${Build.VERSION.SDK_INT}: flag set and mobile data off; Wi-Fi could not be cut (svc wifi is killed on this image, rc 137)"
        }
        DeviceShell.waitUntil("airplane_mode_on to read 1", 15_000) { isOn() }
        if (limits.isNotEmpty()) Log.w("WO006Airplane", limits)
    }

    /**
     * Puts the saved prior flag (and, below API 30, the mobile-data state) back and verifies the read-back. Every step runs even if
     * the one before it throws, and the priors are forgotten only after everything is confirmed, so a failed restore can be retried
     * (CR-5 N12).
     */
    fun restore() {
        val flag = priorFlag ?: return
        val data = priorData
        var failure: Throwable? = null
        fun attempt(step: () -> Unit) {
            try { step() } catch (t: Throwable) { if (failure == null) failure = t }
        }
        if (Build.VERSION.SDK_INT >= 30) {
            attempt {
                if (flag != "1") DeviceShell.run("cmd connectivity airplane-mode disable")
                val want = if (flag == "1") "1" else "0"
                DeviceShell.waitUntil("airplane_mode_on to read $want", 15_000) { DeviceShell.getSetting("global", "airplane_mode_on") == want }
            }
        } else {
            attempt { DeviceShell.restoreSetting("global", "airplane_mode_on", flag, "0") }
            attempt { if (data != "0") DeviceShell.run("svc data enable") }
            attempt {
                val now = DeviceShell.getSetting("global", "airplane_mode_on")
                if (!(now == flag || (flag == "null" && now == "0"))) error("airplane_mode_on not restored: $flag -> $now")
            }
            attempt { if (data != "0" && DeviceShell.getSetting("global", "mobile_data") == "0") error("mobile data not restored (was '$data')") }
        }
        failure?.let { throw it }
        priorFlag = null
        priorData = null
    }
}
