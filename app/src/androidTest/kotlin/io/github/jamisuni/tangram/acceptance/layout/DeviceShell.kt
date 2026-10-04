package io.github.jamisuni.tangram.acceptance.layout

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry

// Scaffolding for the WO-006 device harness (decision DA-96 / DA-108); no requirement token. Shell access through UiAutomation, with
// the output DRAINED to end of stream: executeShellCommand returns before the command is done otherwise (design section 1, step 3).
// Every miss is a loud error.

internal object DeviceShell {
    fun run(command: String): String {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        return ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes().toString(Charsets.UTF_8) }.trim()
    }

    /** `settings get <namespace> <key>`; the text "null" when the value is unset. */
    fun getSetting(namespace: String, key: String): String = run("settings get $namespace $key")

    fun putSetting(namespace: String, key: String, value: String) {
        run("settings put $namespace $key $value")
    }

    fun deleteSetting(namespace: String, key: String) {
        run("settings delete $namespace $key")
    }

    /**
     * Puts a setting back to a SAVED PRIOR value (never to an assumed one): an unset prior ("null") is deleted again, and when the
     * platform keeps a value after the delete [fallbackWhenStillSet] is written (the value device_reset.py accepts as equal to unset).
     */
    fun restoreSetting(namespace: String, key: String, prior: String, fallbackWhenStillSet: String?) {
        if (prior == "null") {
            deleteSetting(namespace, key)
            val now = getSetting(namespace, key)
            if (now != "null" && fallbackWhenStillSet != null) putSetting(namespace, key, fallbackWhenStillSet)
        } else {
            putSetting(namespace, key, prior)
        }
    }

    /**
     * Presses HOME, waits until a launcher is the resumed activity, then waits [quietMs] more. After a display change the launcher of the
     * API 26 image relaunches or crashes for a few seconds; an app activity finished meanwhile is not destroyed until it has settled, and
     * `ActivityScenario.close()` then times out after 45 s ("never becomes DESTROYED", device evidence, WO-006 API 26 gate).
     */
    fun settleHome(quietMs: Long = 2_500) {
        run("input keyevent KEYCODE_HOME")
        waitUntil("a launcher to be the resumed activity", 10_000) {
            run("dumpsys activity activities").lines().any { it.contains("mResumedActivity") && it.contains("Launcher") }
        }
        Thread.sleep(quietMs)
    }

    /** Polls [condition] every 250 ms up to [timeoutMs]; a timeout is `error("timed out waiting for <what>")`. */
    fun waitUntil(what: String, timeoutMs: Long, condition: () -> Boolean) {
        val end = System.currentTimeMillis() + timeoutMs
        while (true) {
            if (condition()) return
            if (System.currentTimeMillis() > end) error("timed out after $timeoutMs ms waiting for $what")
            Thread.sleep(250)
        }
    }
}
