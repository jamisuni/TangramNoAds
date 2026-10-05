package io.github.jamisuni.tangram

import android.os.Handler
import android.os.Looper

/**
 * A once-a-second callback that exists only while counting (WO-008 design 5.4, DA-137): no message is pending when
 * [set] was last called with false, so an idle app wakes nothing. [post] and [cancel] are seams: production passes a
 * main-looper Handler ([main]); a JVM test passes two fakes.
 */
class CountingTicker(
    private val post: (Runnable, Long) -> Unit,
    private val cancel: (Runnable) -> Unit,
    private val onSecond: () -> Unit,
) {
    private var counting = false
    private var posted = false

    private val again = Runnable {
        posted = false
        if (!counting) return@Runnable
        runCatching { onSecond() } // N6: a throwing callback must not end the repost loop
        schedule()
    }

    /** True while a message is waiting to run. */
    internal val pending: Boolean get() = posted

    fun set(counting: Boolean) {
        this.counting = counting
        if (counting) {
            schedule()
        } else if (posted) {
            cancel(again)
            posted = false
        }
    }

    private fun schedule() {
        if (counting && !posted) {
            posted = true
            post(again, PERIOD_MS)
        }
    }

    companion object {
        const val PERIOD_MS = 1000L

        /** On the main looper. */
        fun main(onSecond: () -> Unit): CountingTicker {
            val handler = Handler(Looper.getMainLooper())
            return CountingTicker(
                post = { r, delay -> handler.postDelayed(r, delay) },
                cancel = { r -> handler.removeCallbacks(r) },
                onSecond = onSecond,
            )
        }
    }
}
