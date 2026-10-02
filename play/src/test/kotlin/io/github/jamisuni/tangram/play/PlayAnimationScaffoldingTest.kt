package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.layout.TrayRules
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// SCAFFOLDING (disposable, TASK-015b): not an acceptance test.
class PlayAnimationScaffoldingTest {
    private val rows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val mini: Puzzle = PuzzleLibrary.packaged().puzzles.first()
    private val full: Puzzle = PuzzleLibrary.packaged().puzzles.first { p -> p.solution.any { it.piece == PieceId.PG } }

    private fun setup(p: Puzzle, reduced: Boolean = false): PlaySession {
        val s = PlaySession(p, { reduced })
        s.layout = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, rows, p)
        return s
    }

    private fun pose(p: Puzzle, piece: PieceId): PlacedPiece =
        PieceGeometry.poseOf(piece, p.solution.first { it.piece == piece }.polygon)!!

    private fun drop(s: PlaySession, p: Puzzle, pc: PlacedPiece, t0: Long): DropOutcome? {
        val lay = s.layout!!
        s.beginDrag(pc.piece, lay.cell(pc.piece).centre, t0)
        s.setDragTurn(pc.turn)
        val c = PieceGeometry.centroidOffset(pc.piece.shape, pc.turn, pc.mirrored)
        val centre = lay.toDp(Vec2(pc.at.x.toDouble() + c.x, pc.at.y.toDouble() + c.y))
        s.dragTo(Vec2(centre.x, centre.y + s.drag!!.lift))
        s.onFrame(t0 + 500)
        return s.release(t0 + 500)
    }

    @Test fun trayTapEightTimesReturnsAndFlipToggles() {
        val s = setup(full)
        val start = s.pieces.first { it.piece == PieceId.LT1 }.turn
        repeat(8) { s.tapTray(PieceId.LT1) }
        assertEquals(start, s.pieces.first { it.piece == PieceId.LT1 }.turn)
        s.tapTray(PieceId.LT1)
        assertEquals(Turn((start.steps + 1) % 8), s.pieces.first { it.piece == PieceId.LT1 }.turn)
        s.flipTray(PieceId.PG)
        assertTrue(s.pieces.first { it.piece == PieceId.PG }.mirrored)
        assertEquals(PuzzleState.NEW, s.state)
    }

    @Test fun boardTapThatCannotLockShakesAndKeepsPose() {
        val s = setup(mini)
        val sq = pose(mini, PieceId.SQ)
        assertTrue(drop(s, mini, sq, 0) is DropOutcome.Locked)
        s.tapBoard(PieceId.SQ, 1000)
        assertEquals(sq, s.placed.first { it.piece == PieceId.SQ })
        assertEquals(Shake(PieceId.SQ, 1000), s.shake)
        s.onFrame(1399)
        assertNotNull(s.shake)
        s.onFrame(1400)
        assertNull(s.shake)
    }

    @Test fun boardFlipThatStillLocksIsKept() {
        val s = setup(mini)
        val sq = pose(mini, PieceId.SQ)
        drop(s, mini, sq, 0)
        s.flipBoard(PieceId.SQ, 1000)
        assertNull(s.shake)
        assertTrue(s.placed.first { it.piece == PieceId.SQ }.mirrored)
    }

    @Test fun refitIsOverrideableAndUsedByTheSession() {
        var calls = 0
        val r = object : DropResolver(mini) {
            override fun refit(
                current: PlacedPiece, turn: Turn, mirrored: Boolean, placed: List<PlacedPiece>, dpPerUnit: Double,
            ): PlacedPiece? {
                calls++
                return super.refit(current, turn, mirrored, placed, dpPerUnit)
            }
        }
        val s = PlaySession(mini, { false }, r)
        s.layout = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, rows, mini)
        drop(s, mini, pose(mini, PieceId.SQ), 0)
        s.tapBoard(PieceId.SQ, 10)
        assertEquals(1, calls)
    }

    @Test fun releaseStartsGlideThatExpiresAt180() {
        val s = setup(mini)
        drop(s, mini, pose(mini, PieceId.SQ), 0)
        assertEquals(1, s.glides.size)
        assertEquals(500L, s.glides[0].startMs)
        s.onFrame(679)
        assertEquals(1, s.glides.size)
        s.onFrame(680)
        assertTrue(s.glides.isEmpty())
    }

    @Test fun missPulseLivesSixHundredAndRecordsReduced() {
        for (reduced in listOf(false, true)) {
            val s = setup(mini, reduced)
            val lay = s.layout!!
            val piece = PieceId.SQ
            s.beginDrag(piece, lay.cell(piece).centre, 0)
            val mid = Vec2(lay.boardRect.right - 2, lay.boardRect.top + 2 + s.drag!!.lift) // far from any lock, over the board
            s.dragTo(mid)
            s.onFrame(300)
            val out = s.release(300)
            assertTrue(out is DropOutcome.Home)
            assertNotNull((out as DropOutcome.Home).pulse)
            assertEquals(reduced, s.pulse!!.reduced)
            s.onFrame(899)
            assertNotNull(s.pulse)
            s.onFrame(900)
            assertNull(s.pulse)
        }
    }

    @Test fun pulseRadiusAndShakeOffset() {
        assertEquals(5.4, pulseRadius(0, false), 1e-9)
        assertEquals(11.7, pulseRadius(300, false), 1e-9)
        assertEquals(9.0, pulseRadius(600 - 1, true), 1e-9)
        assertEquals(9.0, pulseRadius(0, true), 1e-9)
        assertEquals(0.0, pulseRadius(600, false), 0.0)
        assertEquals(0.0, shakeOffset(0), 1e-9)
        assertEquals(0.0, shakeOffset(400), 0.0)
        var peak = 0.0
        for (t in 0L until 400L) peak = maxOf(peak, Math.abs(shakeOffset(t)))
        assertTrue(peak <= 6.0 && peak > 5.9)
    }

    @Test fun badgeRectByWhere() {
        val s = setup(full)
        val lay = s.layout!!
        assertEquals(lay.trayBadgeRect(), lay.badgeRect(s))
        val pg = pose(full, PieceId.PG)
        s.beginDrag(PieceId.PG, lay.cell(PieceId.PG).centre, 0)
        assertNull(lay.badgeRect(s))
        s.interruptDrag()
        assertEquals(lay.trayBadgeRect(), lay.badgeRect(s))
        assertNull(setup(mini).let { it.layout!!.badgeRect(it) })

        // On the board: some packaged puzzle must let PG lock on its own (precondition asserted, not branched on).
        val sess = PuzzleLibrary.packaged().puzzles.filter { p -> p.solution.any { it.piece == PieceId.PG } }
            .map { p -> setup(p).also { drop(it, p, pose(p, PieceId.PG), 100) } }
            .firstOrNull { it.pieces.first { x -> x.piece == PieceId.PG }.where is Where.Board }
        assertNotNull("no packaged puzzle lets PG lock first", sess)
        val l2 = sess!!.layout!!
        assertEquals(l2.boardBadgeRect(sess.placed.first { it.piece == PieceId.PG }), l2.badgeRect(sess))
    }

    @Test fun pieceNeverShrinksOnPickUp() {
        for ((w, h, sh) in listOf(Triple(360.0, 700.0, 780.0), Triple(390.0, 760.0, 844.0))) {
            for (p in PuzzleLibrary.packaged().puzzles) {
                val l = PlayLayout.compute(w, h, sh, LayoutClass.PHONE, rows, p)
                assertTrue("${p.id.value} at $w: ${l.dpPerUnit} < ${l.trayScale}", l.dpPerUnit >= l.trayScale)
            }
        }
    }

    @Test fun trayKeepsTurnAfterHome() {
        val s = setup(full)
        s.tapTray(PieceId.LT1)
        val t = s.pieces.first { it.piece == PieceId.LT1 }.turn
        assertTrue(t != TrayRules.restingTurn(PieceId.LT1.shape))
    }
}
