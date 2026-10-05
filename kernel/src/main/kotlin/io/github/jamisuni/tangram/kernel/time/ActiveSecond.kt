package io.github.jamisuni.tangram.kernel.time

import io.github.jamisuni.tangram.kernel.model.PuzzleState

/** TYPE-005 / REQ-029 / REQ-030: the active second (the 60 s idle rule) and which screens let the puzzle count. */
object ActiveSecond {
    /** TYPE-005's locked idle limit: counting stops this long after the last touch. */
    const val IDLE_LIMIT_MS = 60_000L

    /**
     * Milliseconds of the interval ([prevMs], [nowMs]] that were visible and within [IDLE_LIMIT_MS] of a touch.
     * The window is inclusive: a touch exactly 60 s before [nowMs] credits through [nowMs]. Never negative.
     */
    fun activeMs(prevMs: Long, nowMs: Long, visible: Boolean, lastTouchMs: Long?, touching: Boolean): Long {
        if (!visible || nowMs <= prevMs) return 0L
        if (touching) return nowMs - prevMs
        if (lastTouchMs == null) return 0L
        val from = maxOf(prevMs, lastTouchMs)
        val to = minOf(nowMs, lastTouchMs + IDLE_LIMIT_MS)
        return maxOf(0L, to - from)
    }

    /** REQ-030 A3: puzzle time counts only while In progress with neither settings nor the grid open. */
    fun puzzleCounts(state: PuzzleState, settingsOpen: Boolean, gridOpen: Boolean): Boolean =
        state == PuzzleState.IN_PROGRESS && !settingsOpen && !gridOpen
}
