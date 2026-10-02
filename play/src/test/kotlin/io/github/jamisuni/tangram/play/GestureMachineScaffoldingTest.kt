package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PieceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// SCAFFOLDING (disposable, TASK-017): not an acceptance test.
class GestureMachineScaffoldingTest {
    private val rows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val mini: Puzzle = PuzzleLibrary.packaged().puzzles.first()

    private class Counting(p: Puzzle) : DropResolver(p) {
        var previews = 0
        var releases = 0

        override fun preview(pose: DragPose, placed: List<PlacedPiece>, dpPerUnit: Double): PlacedPiece? {
            previews++
            return super.preview(pose, placed, dpPerUnit)
        }

        override fun release(pose: DragPose, placed: List<PlacedPiece>, dpPerUnit: Double): DropOutcome {
            releases++
            return super.release(pose, placed, dpPerUnit)
        }
    }

    private fun session(p: Puzzle = mini, resolver: DropResolver = DropResolver(p)): PlaySession {
        val s = PlaySession(p, { false }, resolver)
        s.layout = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, rows, p)
        return s
    }

    private fun machine(s: PlaySession) = GestureMachine(s) { s.layout }

    private fun turnOf(s: PlaySession, p: PieceId) = s.pieces.first { it.piece == p }.turn.steps

    private fun solutionPose(p: Puzzle, piece: PieceId) =
        PieceGeometry.poseOf(piece, p.solution.first { it.piece == piece }.polygon)!!

    private fun dropOn(s: PlaySession, pose: PlacedPiece) {
        val lay = s.layout!!
        val c = PieceGeometry.centroidOffset(pose.piece.shape, pose.turn, pose.mirrored)
        s.beginDrag(pose.piece, lay.cell(pose.piece).centre, 0)
        s.setDragTurn(pose.turn)
        val centre = lay.toDp(Vec2(pose.at.x.toDouble() + c.x, pose.at.y.toDouble() + c.y))
        s.dragTo(Vec2(centre.x, centre.y + s.drag!!.lift))
        s.onFrame(500)
        s.release(500)
    }

    /** Starts a tray drag of [piece] with finger 1 and returns the machine; finger ends at (200, 300). */
    private fun startTrayDrag(s: PlaySession, m: GestureMachine, piece: PieceId): Vec2 {
        val c = s.layout!!.cell(piece).centre
        m.down(1, c, 0)
        m.move(1, Vec2(c.x + 12.0, c.y), 10)
        assertTrue(s.isDragging)
        val f = Vec2(200.0, 300.0)
        m.move(1, f, 20)
        return f
    }

    private fun twistTo(m: GestureMachine, f: Vec2, deg: Double, id: Int = 2, t: Long = 40) {
        val r = Math.toRadians(deg)
        m.move(id, Vec2(f.x + 100 * cos(r), f.y + 100 * sin(r)), t)
    }

    private val piece = PieceId.ST1

    @Test fun stillPressFor3sIsATap() {
        val s = session()
        val m = machine(s)
        val c = s.layout!!.cell(piece).centre
        val before = turnOf(s, piece)
        m.down(1, c, 0)
        m.up(1, c, 3000)
        assertEquals((before + 1) % 8, turnOf(s, piece))
        assertTrue(!s.isDragging)
        assertEquals(GestureState.Idle, m.state)
    }

    @Test fun elevenPointNineIsATapTwelveIsADrag() {
        val s = session()
        val m = machine(s)
        val c = s.layout!!.cell(piece).centre
        m.down(1, c, 0)
        m.move(1, Vec2(c.x + 11.9, c.y), 10)
        assertTrue(m.state is GestureState.Pressed)
        assertTrue(!s.isDragging)
        m.up(1, Vec2(c.x + 11.9, c.y), 20)
        assertEquals((4 + 1) % 8, turnOf(s, piece))

        m.down(2, c, 100)
        m.move(2, Vec2(c.x + 12.0, c.y), 110)
        assertTrue(s.isDragging)
        assertTrue(m.state is GestureState.Dragging)
    }

    @Test fun dragIsStickyOnceStarted() {
        val s = session()
        val m = machine(s)
        val c = s.layout!!.cell(piece).centre
        m.down(1, c, 0)
        m.move(1, Vec2(c.x + 15.0, c.y), 10)
        m.move(1, c, 20)
        assertTrue(s.isDragging)
    }

    @Test fun cancelMidDragRestoresWholePose() {
        val counting = Counting(mini)
        val s = session(resolver = counting)
        val m = machine(s)
        val f = startTrayDrag(s, m, piece)
        m.down(2, Vec2(f.x + 100, f.y), 30)
        twistTo(m, f, 90.0)
        s.onFrame(100)
        val previews = counting.previews
        m.cancel()
        assertTrue(!s.isDragging)
        assertEquals(PieceId.ST1, s.pieces.first { it.piece == piece }.piece)
        assertEquals(Where.Tray, s.pieces.first { it.piece == piece }.where)
        assertEquals(4, turnOf(s, piece))
        assertEquals(0, counting.releases)
        assertEquals(previews, counting.previews)
        assertNull(s.pulse)
        assertTrue(s.placed.isEmpty())
        assertEquals(GestureState.Idle, m.state)
    }

    @Test fun cancelWhilePressedIsNotATap() {
        val s = session()
        val m = machine(s)
        val c = s.layout!!.cell(piece).centre
        m.down(1, c, 0)
        m.cancel()
        m.up(1, c, 50) // late event of the cancelled finger: no-op
        assertEquals(4, turnOf(s, piece))
        assertEquals(GestureState.Idle, m.state)
    }

    private fun twistedTurn(deg: Double): Int {
        val s = session()
        val m = machine(s)
        val f = startTrayDrag(s, m, piece)
        m.down(2, Vec2(f.x + 100, f.y), 30)
        twistTo(m, f, deg)
        return s.drag!!.turn.steps
    }

    @Test fun twistSteps() {
        assertEquals(4, twistedTurn(20.0))
        assertEquals(4, twistedTurn(22.5))
        assertEquals(5, twistedTurn(22.6))
        assertEquals(6, twistedTurn(90.0))
        assertEquals(3, twistedTurn(-22.6))
        assertEquals(2, twistedTurn(-90.0))
        assertEquals(0, GestureMachine.steps(-22.5))
        assertEquals(1, GestureMachine.steps(67.5))
        assertEquals(2, GestureMachine.steps(67.6))
    }

    @Test fun continuousTwist270IsSixSteps() {
        val s = session()
        val m = machine(s)
        val f = startTrayDrag(s, m, piece)
        m.down(2, Vec2(f.x + 100, f.y), 30)
        var a = 0.0
        while (a < 270.0) {
            a += 10.0
            twistTo(m, f, a)
        }
        assertEquals((4 + 6) % 8, s.drag!!.turn.steps)
    }

    @Test fun secondFingerLiftKeepsTurnAndNewTwistMayStart() {
        val s = session()
        val m = machine(s)
        val f = startTrayDrag(s, m, piece)
        m.down(2, Vec2(f.x + 100, f.y), 30)
        twistTo(m, f, 90.0)
        m.up(2, Vec2(f.x, f.y + 100), 50)
        assertTrue(s.isDragging)
        assertEquals(6, s.drag!!.turn.steps)
        m.down(3, Vec2(f.x + 100, f.y), 60)
        twistTo(m, f, 90.0, id = 3, t = 70)
        assertEquals(0, s.drag!!.turn.steps) // 6 + 2
    }

    @Test fun separationUnder24dpDoesNotCount() {
        val s = session()
        val m = machine(s)
        val f = startTrayDrag(s, m, piece)
        m.down(2, Vec2(f.x + 20, f.y), 30)
        m.move(2, Vec2(f.x, f.y + 20), 40) // 90 degrees at 20 dp: not counted
        assertEquals(4, s.drag!!.turn.steps)
    }

    @Test fun thirdFingerAndSecondFingerWhilePressedAreIgnored() {
        val s = session()
        val m = machine(s)
        val c = s.layout!!.cell(piece).centre
        m.down(1, c, 0)
        m.down(2, Vec2(c.x + 40, c.y), 5)
        assertTrue(m.state is GestureState.Pressed)
        m.up(2, Vec2(c.x + 40, c.y), 6)
        m.up(1, c, 7)
        assertEquals(5, turnOf(s, piece))

        val f = startTrayDrag(s, m, PieceId.ST2)
        m.down(2, Vec2(f.x + 100, f.y), 30)
        m.down(3, Vec2(f.x, f.y + 100), 31)
        twistTo(m, f, 90.0)
        assertEquals(6, s.drag!!.turn.steps)
        m.move(3, Vec2(f.x - 100, f.y), 50) // third finger never promoted
        assertEquals(6, s.drag!!.turn.steps)
    }

    @Test fun dragFingerLiftedFirstReleasesAndSurvivorIsIgnored() {
        val s = session()
        val m = machine(s)
        val target = solutionPose(mini, PieceId.ST1)
        val lay = s.layout!!
        val c = lay.cell(PieceId.ST1).centre
        m.down(1, c, 0)
        m.move(1, Vec2(c.x + 12.0, c.y), 10)
        m.down(2, Vec2(c.x + 100, c.y), 20)
        val cen = PieceGeometry.centroidOffset(PieceId.ST1.shape, target.turn, target.mirrored)
        val dp = lay.toDp(Vec2(target.at.x.toDouble() + cen.x, target.at.y.toDouble() + cen.y))
        val lift = s.drag!!.lift
        val finger = Vec2(dp.x, dp.y + lift)
        m.move(1, finger, 400)
        // turn the piece to the solution turn with the twist finger
        s.setDragTurn(target.turn)
        s.onFrame(500)
        m.up(1, finger, 510)
        assertTrue(!s.isDragging)
        assertEquals(Where.Board::class, s.pieces.first { it.piece == piece }.where::class)
        assertEquals(GestureState.Ignored(setOf(2)), m.state)
        m.move(2, Vec2(0.0, 0.0), 520) // no effect
        m.up(2, Vec2(0.0, 0.0), 530)
        assertEquals(GestureState.Idle, m.state)
    }

    @Test fun eventsAfterOutsideInterruptAreNoOps() {
        val s = session()
        val m = machine(s)
        val f = startTrayDrag(s, m, piece)
        s.interruptDrag()
        m.move(1, Vec2(f.x + 5, f.y), 50)
        assertTrue(m.state is GestureState.Ignored)
        m.up(1, f, 60)
        assertEquals(GestureState.Idle, m.state)
        assertEquals(4, turnOf(s, piece))
    }

    @Test fun otherTouchWhileDraggingDoesNotStartAnything() {
        val s = session()
        val m = machine(s)
        startTrayDrag(s, m, PieceId.ST1)
        // a twist finger on another tray cell does not pick that piece
        m.down(2, s.layout!!.cell(PieceId.ST2).centre, 30)
        assertEquals(Where.Tray, s.pieces.first { it.piece == PieceId.ST2 }.where)
    }

    private fun placedMiniSt1(s: PlaySession): List<Vec2> {
        dropOn(s, solutionPose(mini, PieceId.ST1))
        val pl = s.placed.first { it.piece == PieceId.ST1 }
        return pl.corners.map { s.layout!!.toDp(Vec2(it.x.toDouble(), it.y.toDouble())) }
    }

    private fun outside(poly: List<Vec2>, i: Int, dist: Double): Vec2 {
        val a = poly[i]
        val b = poly[(i + 1) % poly.size]
        val mid = Vec2((a.x + b.x) / 2, (a.y + b.y) / 2)
        val cx = poly.sumOf { it.x } / poly.size
        val cy = poly.sumOf { it.y } / poly.size
        var nx = -(b.y - a.y)
        var ny = b.x - a.x
        val len = hypot(nx, ny)
        nx /= len
        ny /= len
        if ((mid.x - cx) * nx + (mid.y - cy) * ny < 0) {
            nx = -nx
            ny = -ny
        }
        return Vec2(mid.x + nx * dist, mid.y + ny * dist)
    }

    @Test fun touchTenDpOutsideSmallTrianglePicksItThirteenDoesNot() {
        val s = session()
        val poly = placedMiniSt1(s)
        assertEquals(Where.Board::class, s.pieces.first { it.piece == PieceId.ST1 }.where::class)
        for (i in poly.indices) {
            assertEquals(Hit.Piece(PieceId.ST1), HitTest.pick(outside(poly, i, 10.0), s.layout!!, s))
            assertNull(HitTest.pick(outside(poly, i, 13.0), s.layout!!, s))
        }
    }

    @Test fun touchInsideShapeBeatsNearerEdgeOfAnother() {
        val s = session()
        dropOn(s, solutionPose(mini, PieceId.ST1))
        val other = mini.solution.map { it.piece }.first { it != PieceId.ST1 && s.pieces.any { p -> p.piece == it } }
        dropOn(s, solutionPose(mini, other))
        val lay = s.layout!!
        val polyOf = { pc: PieceId ->
            s.placed.first { it.piece == pc }.corners.map { lay.toDp(Vec2(it.x.toDouble(), it.y.toDouble())) }
        }
        val a = polyOf(PieceId.ST1)
        val cen = Vec2(a.sumOf { it.x } / a.size, a.sumOf { it.y } / a.size)
        assertEquals(Hit.Piece(PieceId.ST1), HitTest.pick(cen, lay, s))
    }

    @Test fun badgeWinsOverlapWithPgCell() {
        val full = PuzzleLibrary.packaged().puzzles.first { p -> p.solution.any { it.piece == PieceId.PG } }
        val s = session(full)
        val lay = s.layout!!
        val badge = lay.trayBadgeRect()
        assertNotNull(badge)
        val cell = lay.cell(PieceId.PG)
        val x = maxOf(badge!!.left, cell.left) + 1.0
        val y = badge.top + 1.0
        assertTrue(cell.contains(Vec2(x, y)) && badge.contains(Vec2(x, y)))
        assertEquals(Hit.Badge, HitTest.pick(Vec2(x, y), lay, s))
        // a touch on the badge flips PG in the tray through the machine, and no drag starts
        val m = machine(s)
        m.down(1, Vec2(x, y), 0)
        assertTrue(s.pieces.first { it.piece == PieceId.PG }.mirrored)
        assertTrue(m.state is GestureState.Ignored)
        m.up(1, Vec2(x, y), 10)
        assertEquals(GestureState.Idle, m.state)
    }

    @Test fun machineSetsSessionLayoutFromProviderOnDown() {
        val s = PlaySession(mini)
        val lay = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, rows, mini)
        val m = GestureMachine(s) { lay }
        assertNull(s.layout)
        val c = lay.cell(piece).centre
        m.down(1, c, 0)
        assertTrue(s.layout === lay)
        m.move(1, Vec2(c.x + 12.0, c.y), 10)
        assertTrue(s.isDragging)
    }

    @Test fun beginDragWithoutLayoutIsALoudProgrammerError() {
        val s = PlaySession(mini)
        val threw = try {
            s.beginDrag(piece, Vec2(0.0, 0.0), 0)
            false
        } catch (e: IllegalStateException) {
            true
        }
        assertTrue(threw)
    }

    @Test fun releaseWithoutLayoutRestoresPickUpAndReturnsNull() {
        val s = session()
        val m = machine(s)
        startTrayDrag(s, m, piece)
        s.layout = null
        assertNull(s.release(100))
        assertTrue(!s.isDragging)
        val ps = s.pieces.first { it.piece == piece }
        assertEquals(Where.Tray, ps.where)
        assertEquals(4, ps.turn.steps)
    }

    @Test fun upFarFromDownWithoutMoveIsNotATapNorADrag() {
        val s = session()
        val m = machine(s)
        val c = s.layout!!.cell(piece).centre
        m.down(1, c, 0)
        m.up(1, Vec2(c.x + 30.0, c.y), 50)
        assertEquals(4, turnOf(s, piece))
        assertTrue(!s.isDragging)
        assertEquals(Where.Tray, s.pieces.first { it.piece == piece }.where)
        assertEquals(GestureState.Idle, m.state)
    }

    private fun square(x: Double, y: Double, w: Double) =
        listOf(Vec2(x, y), Vec2(x + w, y), Vec2(x + w, y + w), Vec2(x, y + w))

    @Test fun overlappingPolygonsTopMostWinsAndEdgeTieGoesToTopMost() {
        // overlap: both contain (15, 15); the later one (ST2) is on top
        val a = PieceId.ST1 to square(0.0, 0.0, 20.0)
        val b = PieceId.ST2 to square(10.0, 10.0, 20.0)
        assertEquals(PieceId.ST2, HitTest.pickBoard(listOf(a, b), Vec2(15.0, 15.0)))
        assertEquals(PieceId.ST1, HitTest.pickBoard(listOf(b, a), Vec2(15.0, 15.0)))
        // inside beats a nearer edge: (2, 10) is inside a, 2 dp from a's edge; b's edge is 8 away
        assertEquals(PieceId.ST1, HitTest.pickBoard(listOf(a, b), Vec2(2.0, 10.0)))
        // edge tie: (5, 30) is 10 dp from c's bottom edge and 10 dp from d's top edge; the later one wins
        val c = PieceId.ST1 to square(0.0, 0.0, 20.0)
        val d = PieceId.ST2 to square(0.0, 40.0, 20.0)
        assertEquals(PieceId.ST2, HitTest.pickBoard(listOf(c, d), Vec2(5.0, 30.0)))
        assertEquals(PieceId.ST1, HitTest.pickBoard(listOf(d, c), Vec2(5.0, 30.0)))
    }
}
