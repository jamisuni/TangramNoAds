package io.github.jamisuni.tangram.kernel.time

/** A duration split for display: m:ss below an hour ([withHours] false), h and min from an hour (seconds dropped). */
data class DurationParts(val hours: Long, val minutes: Long, val seconds: Long, val withHours: Boolean)

/** REQ-029 format rule: the one split of a duration, used by browse, settings and time (each holds its two strings). */
object DurationFormat {
    /** Below 3600 s: minutes and seconds. From 3600 s: hours and minutes, seconds floored away. Negative reads as 0. */
    fun parts(totalSeconds: Long): DurationParts {
        val total = maxOf(0L, totalSeconds)
        return if (total < 3600L) {
            DurationParts(hours = 0L, minutes = total / 60L, seconds = total % 60L, withHours = false)
        } else {
            DurationParts(hours = total / 3600L, minutes = (total % 3600L) / 60L, seconds = 0L, withHours = true)
        }
    }
}
