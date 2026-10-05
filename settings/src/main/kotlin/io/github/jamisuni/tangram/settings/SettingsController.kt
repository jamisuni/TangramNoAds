package io.github.jamisuni.tangram.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.jamisuni.tangram.contracts.progress.IProgressStore
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle

/** The play-time numbers the settings screen shows (REQ-005, REQ-030); `app` implements it over the keeper. */
interface PlayTimeReadout {
    val todaySeconds: Long
    val totalSeconds: Long

    companion object {
        val NONE: PlayTimeReadout = object : PlayTimeReadout {
            override val todaySeconds: Long = 0L
            override val totalSeconds: Long = 0L
        }
    }
}

/**
 * The settings screen's state (REQ-032, REQ-033, REQ-034; design WO-007 sections 2 and 5). Main thread only.
 * Open, close, request and cancel write nothing; [confirmReset] is the only path that touches progress, once.
 */
class SettingsController(
    private val store: IProgressStore,
    private val canOpen: () -> Boolean = { true },
    val readout: PlayTimeReadout = PlayTimeReadout.NONE,
    private val puzzles: () -> List<Puzzle> = { emptyList() },
    private val onReset: () -> Unit,
) {
    var isOpen: Boolean by mutableStateOf(false)
        private set
    var soundOn: Boolean by mutableStateOf(store.settings().soundOn)
        private set
    private var timerShownState: Boolean by mutableStateOf(store.settings().timerShown)
    val timerShown: Boolean get() = timerShownState
    var confirmingReset: Boolean by mutableStateOf(false)
        private set

    /** Bumped by [open] and [confirmReset]: the best-time list is recomputed when it changes (design WO-008 section 6). */
    var revision: Int by mutableIntStateOf(0)
        private set

    fun open() {
        if (!canOpen()) return
        confirmingReset = false
        revision++
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

    fun setTimerShown(on: Boolean) {
        store.saveSettings(store.settings().copy(timerShown = on))
        timerShownState = on
    }

    /** One entry per puzzle that has a best time, in library order. */
    internal fun bestTimes(): List<Pair<Puzzle, Long>> =
        puzzles().mapNotNull { p -> store.progress(p.id).bestSeconds?.let { p to it } }

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
        revision++
        onReset()
    }
}
