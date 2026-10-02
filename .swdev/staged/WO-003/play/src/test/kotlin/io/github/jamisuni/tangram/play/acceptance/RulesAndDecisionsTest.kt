package io.github.jamisuni.tangram.play.acceptance

import io.github.jamisuni.tangram.contracts.puzzle.PictureShape
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.model.PieceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Decision tests (F8, DA-21) and timeline rules of REQ-023 / REQ-051 that carry no acceptance ID of their own. */
class RulesAndDecisionsTest {

    // decision F8 / DA-15 - "overBoard = the dragged piece's floating centre inside the board rect grown 20 dp left, right
    // and top and ending 4 dp above the tray top". WO-001 code review F8: overBoard must exist and be true to that.
    @Test
    fun decision_F8_overBoardTable() {
        for (id in listOf("shapes-mini-1", "shapes-square")) {
            val p = puzzle(id)
            val l = layoutFor(p)
            val b = l.boardR()
            val trayTop = p.solution.map { it.piece }.minOf { l.cellR(it).t }
            val midY = (b.t + minOf(b.b, trayTop)) / 2
            assertTrue("$id: board centre", l.overBoard(Vec2(b.cx, midY)))
            assertTrue("$id: 15 dp left of the board is inside the 20 dp margin", l.overBoard(Vec2(b.l - 15.0, midY)))
            assertFalse("$id: 25 dp left of the board is outside", l.overBoard(Vec2(b.l - 25.0, midY)))
            assertTrue("$id: 15 dp right of the board", l.overBoard(Vec2(b.r + 15.0, midY)))
            assertFalse("$id: 25 dp right of the board", l.overBoard(Vec2(b.r + 25.0, midY)))
            assertTrue("$id: 15 dp above the board top", l.overBoard(Vec2(b.cx, b.t - 15.0)))
            assertFalse("$id: 25 dp above the board top", l.overBoard(Vec2(b.cx, b.t - 25.0)))
            assertTrue("$id: well above the tray", l.overBoard(Vec2(b.cx, trayTop - 40.0)))
            assertFalse("$id: 1 dp above the tray top is below 'ending 4 dp above the tray top'", l.overBoard(Vec2(b.cx, trayTop - 1.0)))
            for (piece in p.solution.map { it.piece }) {
                assertFalse("$id: centre on the $piece tray cell", l.overBoard(l.cellR(piece).centre))
            }
        }
    }

    // REQ-023 rule (no acceptance ID): "The picture fades in over 800 ms, starting 600 ms after the last piece locks."
    @Test
    fun req023_rule_pictureFadesInOver800msStarting600msAfterTheLastLock() {
        fun a(ms: Int) = pictureAlpha(ms)
        assertEquals(0.0, a(0), 1e-9)
        assertEquals(0.0, a(599), 1e-9)
        assertEquals("not yet visible when the delay has just passed", 0.0, a(600), 0.02)
        assertEquals(1.0, a(1400), 1e-9)
        assertEquals(1.0, a(5000), 1e-9)
        assertTrue("half way is strictly between", a(1000) > 0.05 && a(1000) < 0.95)
        var last = 0.0
        for (ms in 0..1500 step 25) {
            assertTrue("monotonic at $ms", a(ms) >= last - 1e-9)
            last = a(ms)
        }
    }

    // REQ-051 rule (no acceptance ID): "With reduced motion enabled, the corners are shown for 600 ms without animation."
    @Test
    fun req051_rule_reducedMotionShowsTheCornersStaticFor600ms() {
        // pulseRadius(ms, reduced) is a frozen draw seam ("PlayTiming-based pulseRadius(ms, reduced)").
        val first = pulseRadius(0, true)
        assertTrue("shown at all", first > 0.0)
        for (ms in listOf(100, 300, 599)) assertEquals("static at $ms ms with reduced motion", first, pulseRadius(ms, true), 1e-9)
        assertTrue("animated without reduced motion", pulseRadius(0, false) != pulseRadius(250, false))
    }

    // decision DA-21 - "Path data: exact M/L/Q/C/Z absolute grammar; a bad d skips that shape at run time; every packaged
    // d must parse (build test)". REQ-039 (a picture for every puzzle): no puzzle may ship a path that cannot be drawn.
    @Test
    fun decision_DA21_everyPackagedPathParsesAndTheGrammarIsExact() {
        val parse = { d: String -> parsePath(d) }
        var count = 0
        for (p in PUZZLES) {
            for (s in p.picture.shapes.filterIsInstance<PictureShape.Path>()) {
                count++
                assertNotNull("${p.id.value}: path '${s.data}' must parse", parse(s.data))
            }
        }
        assertTrue("fixture: the packaged puzzles contain path shapes", count > 0)
        assertNotNull("letters glued to numbers are valid", parse("M3.3,3.35 Q3.7,3.55 4,3.35"))
        assertNotNull("closed path", parse("M0,0 L1,0 L1,1 Z"))
        assertNotNull("cubic", parse("M0,0 C1,1 2,1 3,0"))
        for (bad in listOf("", "L1,1", "m0,0", "M0,0 l1,1", "M0,0 L1", "M0,0 L1,1 2,2", "M1e3,0", "M+1,0", "M0,0 Q1,1", "MNaN,0", "M0,0 X1,1")) {
            assertNull("'$bad' must be rejected (null), not crash", parse(bad))
        }
    }

    // decision F2 prerequisite (cell colours): the tray colours are TYPE-001's - pixel-level proof is on device
    // (TrayDeviceTest); here only that the session knows seven distinct pieces in TYPE-001 order for a full set.
    @Test
    fun fullSetSessionHasTheSevenPiecesOfType001() {
        assertEquals(PieceId.entries, Driver(puzzle("shapes-square")).session.pieces.map { it.piece })
        assertEquals(listOf(PieceId.SQ, PieceId.ST1, PieceId.ST2), Driver(puzzle("shapes-mini-1")).session.pieces.map { it.piece })
    }
}
