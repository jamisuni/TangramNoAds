package io.github.jamisuni.tangram.time

import java.time.LocalDate

/** The clock the playing-time keeper reads (WO-008 design 2.4): a monotonic millisecond count and the local date. */
interface TimeSource {
    /** Monotonic milliseconds; only differences mean anything. */
    fun nowMs(): Long

    /** The local calendar date (REQ-029: today restarts at local midnight). */
    fun today(): LocalDate
}

/** The real clock: `System.nanoTime` (monotonic, immune to wall-clock changes) and the local date. */
object SystemTimeSource : TimeSource {
    override fun nowMs(): Long = System.nanoTime() / 1_000_000

    override fun today(): LocalDate = LocalDate.now()
}
