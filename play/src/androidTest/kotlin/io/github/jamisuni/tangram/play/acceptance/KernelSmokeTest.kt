package io.github.jamisuni.tangram.play.acceptance

import io.github.jamisuni.tangram.kernel.lock.SolvedCheck
import io.github.jamisuni.tangram.play.DragPose
import io.github.jamisuni.tangram.play.DropOutcome
import io.github.jamisuni.tangram.play.DropResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * decision DA-22 / design section 6.2 "kernel smoke": on the device (API 26 when present) every packaged puzzle can be built
 * up by DropResolver.release, one piece at a time, and the kernel says solved - proving BigDecimal, Math.hypot and the Java 17
 * bytecode of the JVM modules work on the oldest supported Android.
 */
class KernelSmokeTest {

    @Test
    fun decision_DA22_everyPackagedPuzzleSolvesThroughTheKernelOnTheDevice() {
        assertTrue(PUZZLES.size >= 13)
        for (p in PUZZLES) {
            val resolver = DropResolver(p)
            val placed = ArrayList<io.github.jamisuni.tangram.kernel.geometry.PlacedPiece>()
            for (t in buildOrder(p, targetsOf(p), 60.0)) {
                val out = resolver.release(DragPose(t.piece, t.turn, t.mirrored, v(t.at), true), placed, 60.0)
                assertTrue("${p.id.value} ${t.piece} locks", out is DropOutcome.Locked)
                assertEquals(t.at, (out as DropOutcome.Locked).placed.at)
                placed.add(out.placed)
            }
            assertTrue("${p.id.value} solved", SolvedCheck.isSolved(p.solution.map { it.piece }, placed))
        }
    }
}
