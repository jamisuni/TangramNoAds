package io.github.jamisuni.tangram.time

import java.time.LocalDate

// GATE HELPER (WO-008 T8c part ii, gate set of GATE-063). Test scaffolding, no requirement token.
// A `TimeSource` the test moves by hand, so no gate test ever waits (design 2.4: "tests never wait"). Monotonic by construction:
// `advance` only moves forward. The date is set explicitly and never follows `nowMs` (the gate tests are not about midnight).
class GateManualTimeSource(startMs: Long = 1_000_000L, var date: LocalDate = LocalDate.of(2026, 10, 5)) : TimeSource {
    private var now: Long = startMs

    override fun nowMs(): Long = now

    override fun today(): LocalDate = date

    fun advance(ms: Long) {
        require(ms >= 0) { "a monotonic clock never runs backwards: advance($ms)" }
        now += ms
    }
}
