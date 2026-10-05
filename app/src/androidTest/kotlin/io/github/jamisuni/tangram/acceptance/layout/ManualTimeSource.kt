package io.github.jamisuni.tangram.acceptance.layout

import io.github.jamisuni.tangram.time.TimeSource
import java.time.LocalDate

// Scaffolding for the WO-008 device tests (design 2.4, 5.5, 8; decision DA-142); no requirement token. The manual clock a test installs
// through `AppLaunch.launch(timeSource = ...)` (G-04: the manual source lives in androidTest, never in the product), so nothing ever sleeps.
// The clock is written ONLY on the main thread, inside `advanceActive` (design 8, E6), where the keeper's ticker also reads it: no race, no
// @Volatile. A frozen clock (never advanced) credits no second at all, which keeps a test's exact play-time assertions exact.
internal class ManualTimeSource(startMs: Long = 1_000_000L, date: LocalDate = LocalDate.of(2026, 10, 5)) : TimeSource {
    private var now: Long = startMs
    private var day: LocalDate = date

    override fun nowMs(): Long = now

    override fun today(): LocalDate = day

    /** Main thread only (through `advanceActive`). */
    fun advance(ms: Long) {
        require(ms >= 0) { "a monotonic clock never runs backwards: advance($ms)" }
        now += ms
    }

    /** Main thread only (through `advanceActive`). */
    fun setDate(date: LocalDate) {
        day = date
    }
}
