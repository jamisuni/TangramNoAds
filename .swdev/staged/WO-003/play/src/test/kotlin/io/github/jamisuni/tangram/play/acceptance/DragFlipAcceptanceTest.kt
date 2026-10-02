package io.github.jamisuni.tangram.play.acceptance

import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.geometry.pointOf
import io.github.jamisuni.tangram.kernel.lock.LockSearch
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** REQ-014 A2, REQ-018 A1, REQ-051 A2 at engine level. */
class DragFlipAcceptanceTest {

    // REQ-014.A2 - "The piece is never drawn larger or smaller than its board size once on the board."
    // REQ-014 rules: "A tray piece grows from miniature to board size within 120 ms of the drag starting. The piece's size
    // never changes on the board."
    @Test
    fun req014_A2_aPieceIsNeverDrawnLargerThanItsBoardSizeAndIsExactlyBoardSizeOnTheBoard() {
        val p = puzzle("shapes-mini-1")
        val d = Driver(p)
        val l = d.layout
        val piece = PieceId.ST1
        assertEquals("a tray piece is drawn at the tray scale", l.trayScale, drawnScale(d.session, piece, l), 1e-6)
        assertTrue("fixture: miniature and board size differ", l.dpPerUnit > l.trayScale + 1.0)

        d.startDrag(pressPoint(l.cellR(piece)))
        var elapsed = 16L + 16L // the frames of startDrag after the drag began are at most this old
        repeat(40) {
            d.frame(8)
            elapsed += 8
            val s = d.session.drag?.frame?.scale
            assertNotNull("a dragged piece has a frame with a scale", s)
            assertTrue("never larger than board size (elapsed ~$elapsed ms): $s > ${l.dpPerUnit}", s!! <= l.dpPerUnit + 1e-6)
            assertTrue("never smaller than the miniature", s >= l.trayScale - 1e-6)
            if (elapsed >= 150) assertEquals("board size after the 120 ms growth", l.dpPerUnit, s, 1e-6)
        }
        val target = targetsOf(p).first { it.piece == piece }
        d.steerOrigin(v(target.at))
        d.up0(d.finger)
        d.frame()
        d.frame(400)
        assertEquals("locked on the board", "Board", d.session.where(piece))
        assertEquals("on the board: exactly board size", l.dpPerUnit, drawnScale(d.session, piece, l), 1e-6)
    }

    // (turn, at) pose candidates of the parallelogram whose mirror image about its own centre still locks next to it,
    // found by the reference geometry; the test picks the first the kernel oracle confirms, so it needs no luck.
    private data class Cand(val puzzleId: String, val turn: Int, val x: Long, val y: Long)

    private val roomy = listOf(
        Cand("shapes-warmup-2", 1, 2, 2), Cand("shapes-warmup-4", 1, 2, 2), Cand("animals-cat", 1, 2, 4),
        Cand("shapes-warmup-3", 3, 3, 2), Cand("shapes-rectangle", 2, 0, 0),
    )

    /** The kernel's answer for the flipped parallelogram: REQ-016/018 "turned about its centre ... locks within the lock distance". */
    private fun flipOracle(d: Driver, before: PlacedPiece): io.github.jamisuni.tangram.kernel.lock.Lock? {
        val others = d.session.placed.filter { it.piece != before.piece }
        val c0 = centroidOf(polyOf(before))
        val off = centroidOffset(before.piece, before.turn, !before.mirrored)
        val origin = Vec2(c0.x - off.x, c0.y - off.y)
        return LockSearch.find(
            silhouetteOf(d.puzzle), others, before.piece, before.turn, !before.mirrored, origin,
            LockSearch.lockDistance(d.layout.dpPerUnit),
        )
    }

    // REQ-018.A1 - "The parallelogram can be mirrored in the tray and on the board."
    @Test
    fun req018_A1_theParallelogramCanBeMirroredInTheTrayAndOnTheBoard() {
        // tray: the badge toggles the mirror, nothing else changes
        val tray = Driver(puzzle("shapes-square"))
        val turn0 = tray.session.ps(PieceId.PG).turn
        tray.tap(tray.layout.badgeR(tray.session).centre)
        assertTrue("mirrored after one badge press", tray.session.ps(PieceId.PG).mirrored)
        tray.tap(tray.layout.badgeR(tray.session).centre)
        assertFalse("and back after the second", tray.session.ps(PieceId.PG).mirrored)
        assertEquals(turn0, tray.session.ps(PieceId.PG).turn)
        assertEquals("Tray", tray.session.where(PieceId.PG))
        assertEquals("a tray flip does not start the puzzle (TYPE-006)", PuzzleState.NEW, tray.session.state)

        // board: kept when the mirrored piece still locks (REQ-018 rules, "as REQ-016")
        var done = false
        for (c in roomy) {
            val p = puzzle(c.puzzleId)
            val d = Driver(p)
            val target = PlacedPiece(PieceId.PG, Turn(c.turn), false, pointOf(c.x, c.y))
            val sil = silhouetteOf(p)
            val drop = LockSearch.find(sil, emptyList(), PieceId.PG, target.turn, false, v(target.at), LockSearch.lockDistance(d.layout.dpPerUnit))
            if (drop?.at != target.at) continue
            if (flipOracle(d, target) == null) continue
            d.place(target)
            val before = d.session.placed.single { it.piece == PieceId.PG }
            val expected = flipOracle(d, before) ?: continue
            d.tap(d.layout.badgeR(d.session).centre)
            val after = d.session.placed.single { it.piece == PieceId.PG }
            assertTrue("${c.puzzleId}: the mirror is kept because it still locks", after.mirrored)
            assertEquals("${c.puzzleId}: the turn is unchanged", before.turn, after.turn)
            assertEquals("${c.puzzleId}: locked at the kernel's spot", expected.at, after.at)
            assertEquals("Board", d.session.where(PieceId.PG))
            done = true
            break
        }
        assertTrue("fixture: one candidate pose must have room for the mirror image (reference geometry says five do)", done)

        // board, no room: mirrors back with a short shake (REQ-018 rules), position and turn unchanged
        val sq = puzzle("shapes-square")
        val d = Driver(sq)
        val pg = targetsOf(sq).first { it.piece == PieceId.PG }
        d.place(pg)
        val before = d.session.placed.single()
        assertNull("fixture: the kernel finds no lock for the mirrored piece here", flipOracle(d, before))
        d.tap(d.layout.badgeR(d.session).centre)
        val after = d.session.placed.single()
        assertEquals(before, after)
        assertFalse(d.session.ps(PieceId.PG).mirrored)
        assertTrue("a refused mirror shakes the piece once (REQ-018 rules)", shakeActive(d.session))
    }

    // REQ-051.A2 - "In a warm-up, the same drop sends the piece home and nothing else is shown."
    // (the drop is a miss over the board; DA-4 / REQ-051 rule "full-set and warm-up puzzles never show anchors")
    @Test
    fun req051_A2_inAWarmupAMissedDropGoesHomeWithoutAnyPulse() {
        for (id in listOf("shapes-warmup-1", "shapes-warmup-3")) {
            val d = Driver(puzzle(id))
            val piece = PieceId.ST1
            val centre = d.missDrop(piece)
            assertTrue("fixture: the drop was over the board", d.layout.overBoard(centre))
            assertEquals("$id: goes home", "Tray", d.session.where(piece))
            assertTrue("$id: nothing is on the board", d.session.placed.isEmpty())
            assertNull("$id: no corner pulse in a warm-up", d.session.pulse)
            assertFalse("$id: no shake", shakeActive(d.session))
            d.frame(300)
            assertNull("$id: still no pulse", d.session.pulse)
        }
    }
}
