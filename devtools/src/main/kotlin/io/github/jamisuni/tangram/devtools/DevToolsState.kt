package io.github.jamisuni.tangram.devtools

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The DEV aid state: passcode lock, solution visibility, and messages.
 * Owned by DebugAids in AppViewModel (decision DA-76); survives rotation and browsing,
 * lives until the app is restarted. Never persisted.
 */
class DevToolsState {
    var unlocked: Boolean by mutableStateOf(false)
        private set

    var overlayOn: Boolean by mutableStateOf(false)
        private set

    var dialogOpen: Boolean by mutableStateOf(false)
        private set

    var notice: DevNotice by mutableStateOf(DevNotice.NONE)
        private set

    fun open() {
        dialogOpen = true
        notice = DevNotice.NONE
    }

    fun close() {
        dialogOpen = false
    }

    /**
     * Submit the entered passcode. Returns true if the code was correct and unlocked.
     * A wrong code sets WRONG_PASSCODE and changes nothing else.
     */
    fun submit(code: String): Boolean {
        return if (DevPasscode.accepts(code)) {
            unlocked = true
            notice = DevNotice.NONE
            true
        } else {
            notice = DevNotice.WRONG_PASSCODE
            false
        }
    }

    fun toggleOverlay() {
        overlayOn = !overlayOn
    }

    fun solveFailed() {
        notice = DevNotice.SOLVE_FAILED
    }
}
