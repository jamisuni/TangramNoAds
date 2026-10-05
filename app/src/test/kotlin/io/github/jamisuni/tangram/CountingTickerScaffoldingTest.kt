package io.github.jamisuni.tangram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// SCAFFOLDING (disposable): WO-008 TASK-068, the ticker over fake post / cancel. No acceptance token on purpose.
class CountingTickerScaffoldingTest {
    private val queue = mutableListOf<Runnable>()
    private var seconds = 0
    private val ticker = CountingTicker(
        post = { r, _ -> queue.add(r) },
        cancel = { r -> queue.remove(r) },
        onSecond = { seconds++ },
    )

    @Test
    fun nothingIsPendingUntilCounting() {
        assertFalse(ticker.pending)
        assertTrue(queue.isEmpty())
    }

    @Test
    fun oneMessageWhileCountingAndItRepostsItself() {
        ticker.set(true)
        ticker.set(true)
        assertEquals(1, queue.size)
        queue.removeAt(0).run()
        assertEquals(1, seconds)
        assertEquals(1, queue.size)
        assertTrue(ticker.pending)
    }

    @Test
    fun stoppingCancelsTheMessage() {
        ticker.set(true)
        ticker.set(false)
        assertTrue(queue.isEmpty())
        assertFalse(ticker.pending)
    }

    @Test
    fun aThrowingOnSecondLeavesTheTickerReposting() {
        val own = mutableListOf<Runnable>()
        val t = CountingTicker({ r, _ -> own.add(r) }, { r -> own.remove(r) }, { throw IllegalStateException("boom") })
        t.set(true)
        own.removeAt(0).run()
        assertEquals(1, own.size)
        assertTrue(t.pending)
    }

    @Test
    fun countingEndingInsideASecondLeavesNothingPending() {
        val own = mutableListOf<Runnable>()
        lateinit var t: CountingTicker
        t = CountingTicker({ r, _ -> own.add(r) }, { r -> own.remove(r) }, { t.set(false) })
        t.set(true)
        own.removeAt(0).run()
        assertTrue(own.isEmpty())
        assertFalse(t.pending)
    }
}
