package io.github.jamisuni.tangram.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.jamisuni.tangram.contracts.progress.IProgressStore

/**
 * The settings screen's state (REQ-032, REQ-033, REQ-034; design WO-007 sections 2 and 5). Main thread only.
 * Open, close, request and cancel write nothing; [confirmReset] is the only path that touches progress, once.
 */
class SettingsController(
    private val store: IProgressStore,
    private val canOpen: () -> Boolean = { true },
    private val onReset: () -> Unit,
) {
    var isOpen: Boolean by mutableStateOf(false)
        private set
    var soundOn: Boolean by mutableStateOf(store.settings().soundOn)
        private set
    var confirmingReset: Boolean by mutableStateOf(false)
        private set

    fun open() {
        if (!canOpen()) return
        confirmingReset = false
        isOpen = true
    }

    fun close() {
        confirmingReset = false
        isOpen = false
    }

    fun setSound(on: Boolean) {
        store.saveSettings(store.settings().copy(soundOn = on))
        soundOn = on
    }

    fun requestReset() {
        confirmingReset = true
    }

    fun cancelReset() {
        confirmingReset = false
    }

    fun confirmReset() {
        if (!confirmingReset) return
        confirmingReset = false
        store.resetAllProgress()
        onReset()
    }
}
