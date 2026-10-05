package io.github.jamisuni.tangram.time

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.jamisuni.tangram.contracts.progress.IProgressStore
import io.github.jamisuni.tangram.contracts.progress.PlayTime
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.time.ActiveSecond
import java.time.LocalDate

/**
 * Counts the active playing time (TYPE-005, REQ-005, REQ-029, REQ-030; WO-008 design 2, 3, 4.3, 5.1).
 *
 * Two kinds of entry, kept apart so no input can write a stale value (review F1):
 *  - **Accounting** (in memory only): [touch], [setVisible], [setPuzzleRunning], [puzzleShown], [solved], [mergeInto], [afterReset].
 *    They all close the open interval through the private `account()`, which has no path to the store.
 *  - **Writing** (the closed list): [accrue] (when due), [flush], and the stop points `setVisible(false)` and `setPuzzleRunning(false)`.
 *
 * The keeper caches no best time: [mergeInto] computes it from the stored base. It never throws (G-10).
 * The constructor reads `store.playTime()` (a read, never a write), so today and total survive a restart.
 */
class PlayTimeKeeper(
    private val source: TimeSource,
    private val store: IProgressStore,
    private val onCountingChanged: (Boolean) -> Unit = {},
) {
    // ---- the reads (snapshot state)
    private var puzzleSecondsState by mutableLongStateOf(0L)
    private var todayState by mutableLongStateOf(0L)
    private var totalState by mutableLongStateOf(0L)
    private var dayState by mutableStateOf<LocalDate?>(null)
    private var countingState by mutableStateOf(false)

    /** Active seconds of the shown puzzle's current attempt. */
    val puzzleSeconds: Long get() = puzzleSecondsState

    /** Active seconds today; 0 when the held day is not the source's date (earlier or later). */
    val todaySeconds: Long get() = if (dayState != null && dayState == source.today()) todayState else 0L

    /** Active seconds in total. */
    val totalSeconds: Long get() = totalState

    /** True while the clock is running: visible and within the idle window of a touch (or a finger is down). */
    val counting: Boolean get() = countingState

    // ---- the memory (not snapshot state)
    private var visible = false
    private var touching = false
    private var lastTouchMs: Long? = null
    private var lastMs: Long = source.nowMs()
    private var lastFlushMs: Long = lastMs
    private var dayCarryMs = 0L
    private var puzzleCarryMs = 0L
    private var puzzleRunning = false
    private var shownId: PuzzleId? = null
    private var pendingOwnSolve: Long? = null
    private var flushOnIdle = false
    private var reportedCounting = false

    init {
        readPlayTime()
    }

    // ---------------------------------------------------------------------------------------------------- accounting inputs

    fun touch(pressed: Boolean) {
        account()
        touching = pressed
        lastTouchMs = lastMs
        refreshCounting()
    }

    fun setVisible(visible: Boolean) {
        account()
        this.visible = visible
        if (!visible) touching = false
        refreshCounting()
        if (!visible) flush()
    }

    fun setPuzzleRunning(running: Boolean) {
        account()
        puzzleRunning = running
        if (!running) flush()
    }

    fun puzzleShown(id: PuzzleId, progress: PuzzleProgress) {
        account()
        shownId = id
        puzzleSecondsState = progress.puzzleSeconds
        puzzleCarryMs = 0L
        pendingOwnSolve = null
    }

    fun solved(byAid: Boolean) {
        account()
        puzzleRunning = false
        puzzleCarryMs = 0L
        pendingOwnSolve = if (byAid) null else puzzleSecondsState
    }

    fun mergeInto(id: PuzzleId, base: PuzzleProgress): PuzzleProgress {
        account()
        if (id != shownId) return base
        val pending = pendingOwnSolve
        return base.copy(
            puzzleSeconds = if (base.state == PuzzleState.NEW) 0L else puzzleSecondsState,
            bestSeconds = if (pending != null) minOf(base.bestSeconds ?: Long.MAX_VALUE, pending) else base.bestSeconds,
        )
    }

    /** The store has been erased: discard the open interval, zero everything, re-read the store. Writes nothing. */
    fun afterReset() {
        val now = source.nowMs()
        if (now > lastMs) lastMs = now
        dayCarryMs = 0L
        puzzleCarryMs = 0L
        puzzleSecondsState = 0L
        pendingOwnSolve = null
        todayState = 0L
        totalState = 0L
        dayState = null
        readPlayTime()
        lastFlushMs = lastMs
        flushOnIdle = false
        refreshCounting()
    }

    // ---------------------------------------------------------------------------------------------------- writing entries

    /** The one-second entry: accounts, then flushes when due (10 s since the last flush while counting, or counting just ended). */
    fun accrue() {
        account()
        val due = countingState && lastMs - lastFlushMs >= FLUSH_EVERY_MS
        if (due || flushOnIdle) flush()
    }

    /** Writes the playing time, then the shown puzzle's seconds into its stored In-progress record. Never creates a record. */
    fun flush() {
        account()
        lastFlushMs = lastMs
        flushOnIdle = false
        try {
            store.savePlayTime(PlayTime(dayState, todayState, totalState))
        } catch (_: Exception) {
            // G-10: a store failure is swallowed; the next flush writes the then-current whole values
        }
        val id = shownId ?: return
        try {
            val stored = store.progress(id)
            if (stored.state == PuzzleState.IN_PROGRESS && stored.puzzleSeconds != puzzleSecondsState) {
                store.saveProgress(id, stored.copy(puzzleSeconds = puzzleSecondsState))
            }
        } catch (_: Exception) {
            // G-10
        }
    }

    // ---------------------------------------------------------------------------------------------------- private

    /** Closes the open interval in memory. No path to the store. */
    private fun account() {
        val now = source.nowMs()
        val prev = lastMs
        if (now > prev) {
            val active = ActiveSecond.activeMs(prev, now, visible, lastTouchMs, touching)
            lastMs = now
            if (active > 0L) credit(active)
        }
        refreshCounting()
    }

    private fun credit(activeMs: Long) {
        dayCarryMs += activeMs
        val seconds = dayCarryMs / 1000
        dayCarryMs %= 1000
        if (seconds > 0L) {
            val today = source.today()
            if (dayState != today) {
                dayState = today
                todayState = 0L
            }
            todayState += seconds
            totalState += seconds
        }
        if (puzzleRunning && shownId != null) {
            puzzleCarryMs += activeMs
            val puzzleSecs = puzzleCarryMs / 1000
            puzzleCarryMs %= 1000
            if (puzzleSecs > 0L) puzzleSecondsState += puzzleSecs
        }
    }

    private fun refreshCounting() {
        val last = lastTouchMs
        val now = visible && (touching || (last != null && lastMs - last < ActiveSecond.IDLE_LIMIT_MS))
        if (reportedCounting && !now) flushOnIdle = true
        if (now != reportedCounting) {
            reportedCounting = now
            countingState = now
            try {
                onCountingChanged(now)
            } catch (_: Exception) {
                // a listener must not break the keeper
            }
        }
    }

    private fun readPlayTime() {
        val stored = try {
            store.playTime()
        } catch (_: Exception) {
            PlayTime.NONE
        }
        dayState = stored.day
        todayState = stored.todaySeconds
        totalState = stored.totalSeconds
    }

    companion object {
        /** Source time between two flushes while counting. */
        const val FLUSH_EVERY_MS = 10_000L
    }
}
