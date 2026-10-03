package io.github.jamisuni.tangram.play.acceptance.held

import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.lock.LockSearch
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Turn
import io.github.jamisuni.tangram.play.DragPose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** HELD-OUT: REQ-014 A1, REQ-015 A1/A2, REQ-016 A2, REQ-017 A1/A2 at engine level, through the real gesture machine. */
class GestureHeldTest {

    private fun pose(d: Driver): DragPose = d.session.drag?.frame?.pose ?: error("no drag frame")

    private fun worldDp(d: Driver, pose: DragPose): List<Vec2> =
        PieceGeometry.offsets(pose.piece.shape, pose.turn, pose.mirrored)
            .map { d.layout.toDp(Vec2(pose.origin.x + it.x.toDouble(), pose.origin.y + it.y.toDouble())) }

    // REQ-014.A1 - "A dragged piece follows the finger without delay."
    // REQ-014 rules/ASSUMPTION: after the 120 ms growth the piece floats with its lowest point 30 dp (phone) above the finger;
    // DA-17: centred horizontally on the finger. Every frame after 120 ms must show exactly that, for the LATEST finger
    // position, also when two moves arrive within one frame (design seam row: "two moves in one frame show the later").
    @Test
    fun req014_A1_aDraggedPieceFollowsTheFingerWithoutDelay() {
        for (id in listOf("shapes-mini-1", "shapes-square")) {
            val d = Driver(puzzle(id))
            for (piece in listOf(d.session.pieces.first().piece, d.session.pieces.last().piece)) {
                d.startDrag(pressPoint(d.layout.cellR(piece)))
                d.frame(200) // past the 120 ms growth
                val route = listOf(Vec2(150.0, 300.0), Vec2(200.0, 250.0), Vec2(120.0, 330.0), Vec2(90.0, 200.0), Vec2(260.0, 280.0))
                for ((i, f) in route.withIndex()) {
                    if (i % 2 == 1) d.move0(Vec2(f.x - 40.0, f.y + 25.0)) // an earlier move in the same frame ...
                    d.move0(f) // ... superseded by this one
                    d.frame(16)
                    val w = worldDp(d, pose(d))
                    assertEquals("$id $piece: lowest point 30 dp above the finger", f.y - 30.0, w.maxOf { it.y }, 0.75)
                    assertEquals("$id $piece: centred on the finger", f.x, centroidOf(w).x, 0.75)
                }
                d.machine.cancel()
                d.frame()
            }
        }
        // the same from a board piece (DA-17: board pieces lift like tray pieces)
        val p = puzzle("shapes-mini-1")
        val d = Driver(p)
        d.placeAll(targetsOf(p).filter { it.piece != PieceId.ST2 }) // DA-45: leave one piece in the tray, a solved puzzle takes no touches (DA-35)
        val sq = d.session.placed.first { it.piece == PieceId.SQ }
        d.startDrag(d.layout.toDp(centroidOf(polyOf(sq))))
        d.frame(250)
        val f = Vec2(180.0, 300.0)
        d.move0(f)
        d.frame(16)
        val w = worldDp(d, pose(d))
        assertEquals(f.y - 30.0, w.maxOf { it.y }, 0.75)
        assertEquals(f.x, centroidOf(w).x, 0.75)
    }

    // REQ-015.A1 - "A 3 s still press on a piece acts as a tap."
    // REQ-015 statement: "moved less than 12 dp ... a tap regardless of its duration; otherwise ... a drag". DA-16: once the
    // 12 dp are reached it is a drag for good, even if the finger comes back.
    @Test
    fun req015_A1_aLongPressUnder12dpIsATapAndTwelveOrMoreIsADrag() {
        val p = puzzle("shapes-mini-1")
        fun fresh() = Driver(p)
        val piece = PieceId.ST1

        run { // 3 s completely still
            val d = fresh()
            val at = pressPoint(d.layout.cellR(piece))
            val t0 = d.session.ps(piece).turn
            d.down0(at); d.frame(3000); d.up0(at); d.frame()
            assertEquals("a 3 s still press turns the piece one step", Turn((t0.steps + 1) % 8), d.session.ps(piece).turn)
            assertNull("no drag happened", d.session.drag)
            assertEquals("Tray", d.session.where(piece))
            assertEquals("tapping does not start the puzzle (TYPE-006)", PuzzleState.NEW, d.session.state)
        }
        run { // 3 s with 8 dp of wandering
            val d = fresh()
            val at = pressPoint(d.layout.cellR(piece))
            val t0 = d.session.ps(piece).turn
            val moved = Vec2(at.x + 8.0, at.y)
            d.down0(at); d.frame(1000); d.move0(moved); d.frame(2000); d.up0(moved); d.frame()
            assertEquals(Turn((t0.steps + 1) % 8), d.session.ps(piece).turn)
            assertNull(d.session.drag)
        }
        run { // 11.9 dp: still a tap
            val d = fresh()
            val at = pressPoint(d.layout.cellR(piece))
            val t0 = d.session.ps(piece).turn
            val moved = Vec2(at.x + 11.9, at.y)
            d.down0(at); d.frame(); d.move0(moved); d.frame(); d.up0(moved); d.frame()
            assertEquals(Turn((t0.steps + 1) % 8), d.session.ps(piece).turn)
        }
        run { // 12.1 dp: a drag, whose release on the tray sends the piece back unturned
            val d = fresh()
            val at = pressPoint(d.layout.cellR(piece))
            val t0 = d.session.ps(piece).turn
            val moved = Vec2(at.x + 12.1, at.y)
            d.down0(at); d.frame(); d.move0(moved); d.frame()
            assertNotNull("a 12.1 dp move starts a drag", d.session.drag)
            assertEquals(PuzzleState.IN_PROGRESS, d.session.state) // TYPE-006: a drag started on a tray piece
            d.up0(moved); d.frame()
            assertNull(d.session.drag)
            assertEquals("not turned: it was a drag", t0, d.session.ps(piece).turn)
        }
        run { // DA-16: sticky
            val d = fresh()
            val at = pressPoint(d.layout.cellR(piece))
            val t0 = d.session.ps(piece).turn
            d.down0(at); d.frame(); d.move0(Vec2(at.x + 30.0, at.y)); d.frame()
            d.move0(at); d.frame()
            assertNotNull("coming back does not undo the drag", d.session.drag)
            d.up0(at); d.frame()
            assertEquals("and it is not a tap on release", t0, d.session.ps(piece).turn)
        }
    }

    // REQ-015.A2 - "A touch 10 dp outside a small triangle's edge picks it up."
    // REQ-015 rules: board piece touch area = its shape grown by 12 dp; F13: inside a shape beats a nearer edge.
    @Test
    fun req015_A2_aTouchTenDpOutsideASmallTrianglePicksItUp() {
        val p = puzzle("shapes-mini-1")
        val d = Driver(p)
        d.place(targetsOf(p).first { it.piece == PieceId.ST1 })
        val st = d.session.placed.single { it.piece == PieceId.ST1 }
        val poly = polyOf(st).map { d.layout.toDp(it) }
        val c = centroidOf(poly)
        var checked = 0
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            val len = hypot(b.x - a.x, b.y - a.y)
            var n = Vec2(-(b.y - a.y) / len, (b.x - a.x) / len)
            val m = Vec2((a.x + b.x) / 2, (a.y + b.y) / 2)
            if ((m.x - c.x) * n.x + (m.y - c.y) * n.y < 0) n = Vec2(-n.x, -n.y) // outward
            val p10 = Vec2(m.x + 10.0 * n.x, m.y + 10.0 * n.y)
            val p13 = Vec2(m.x + 13.0 * n.x, m.y + 13.0 * n.y)
            val busy = PieceId.entries.any { it in d.session.pieces.map { s -> s.piece } && d.layout.cellR(it).contains(p13) }
            assertTrue("10 dp outside edge $i picks the small triangle: ${pickText(p10, d.layout, d.session)}", pickText(p10, d.layout, d.session).contains("ST1"))
            if (!busy) {
                assertEquals("13 dp outside edge $i is beyond the 12 dp margin", "null", pickText(p13, d.layout, d.session))
                checked++
            }
        }
        assertTrue("fixture: at least one edge has a free 13 dp point", checked >= 1)

        // F13: a touch INSIDE a shape picks that shape although another shape's edge is nearer; the flip badge wins over a piece.
        d.place(targetsOf(p).first { it.piece == PieceId.SQ })
        val sq = d.session.placed.single { it.piece == PieceId.SQ }
        val sqC = centroidOf(polyOf(sq))
        val stC = centroidOf(polyOf(d.session.placed.single { it.piece == PieceId.ST1 }))
        // the shared edge of ST1 and SQ is (1,1)-(2,2); its midpoint in units is (1.5, 1.5), the normal towards SQ is (1,-1)/sqrt2
        val k = 2.0 / d.layout.dpPerUnit / Math.sqrt(2.0)
        val towardsSq = d.layout.toDp(Vec2(1.5 + k, 1.5 - k))
        val towardsSt = d.layout.toDp(Vec2(1.5 - k, 1.5 + k))
        assertTrue("inside the square, 2 dp from the triangle's edge: the square", pickText(towardsSq, d.layout, d.session).contains("SQ"))
        assertTrue("inside the triangle, 2 dp from the square's edge: the triangle", pickText(towardsSt, d.layout, d.session).contains("ST1"))
        assertTrue(sqC != stC)
        val full = Driver(puzzle("shapes-square"))
        assertTrue("the flip badge wins", pickText(full.layout.badgeR(full.session).centre, full.layout, full.session).contains("Badge"))
    }

    // REQ-016.A2 - "A board piece that has no room to turn keeps its position and turn."
    // REQ-016 rules: "IF a turned board piece cannot lock near its place, THEN the game SHALL restore its previous turn and
    // shake it once." The kernel search (REQ-019, the same lock distance) is the oracle for "has room".
    @Test
    fun req016_A2_aBoardPieceWithNoRoomToTurnKeepsItsPositionAndTurn() {
        val p = puzzle("shapes-square")
        val d = Driver(p)
        d.placeAll(targetsOf(p).filter { it.piece != PieceId.PG }) // six pieces, the parallelogram's hole stays open
        assertEquals(PuzzleState.IN_PROGRESS, d.session.state)
        val sil = silhouetteOf(p)
        val r = LockSearch.lockDistance(d.layout.dpPerUnit)
        var noRoom = 0
        for (piece in d.session.placed.map { it.piece }) {
            d.frame(1000) // let any earlier shake end
            val before = d.session.placed.single { it.piece == piece }
            val newTurn = Turn((before.turn.steps + 1) % 8)
            val others = d.session.placed.filter { it.piece != piece }
            val c0 = centroidOf(polyOf(before))
            val off = centroidOffset(piece, newTurn, before.mirrored)
            val expected = LockSearch.find(sil, others, piece, newTurn, before.mirrored, Vec2(c0.x - off.x, c0.y - off.y), r)
            d.tap(d.layout.toDp(c0))
            val after = d.session.placed.single { it.piece == piece }
            if (expected == null) {
                noRoom++
                assertEquals("$piece keeps its place and turn", before, after)
                assertEquals(before.turn, d.session.ps(piece).turn)
                assertTrue("$piece shakes once", shakeActive(d.session))
            } else {
                assertEquals("$piece turns because it still locks", newTurn, after.turn)
                assertEquals("$piece locks where the kernel says", expected.at, after.at)
            }
            assertEquals(PuzzleState.IN_PROGRESS, d.session.state)
        }
        assertTrue("fixture: at least the large triangles have no room ($noRoom pieces)", noRoom >= 2)
    }

    private fun twistFixture(): Pair<Driver, Int> {
        val d = Driver(puzzle("shapes-mini-1"))
        d.startDrag(pressPoint(d.layout.cellR(PieceId.ST1)))
        d.steerOrigin(Vec2(1.0, 1.5))
        d.frame(100)
        return d to pose(d).turn.steps
    }

    /** Second finger down 100 dp right of the drag finger, then rotated clockwise by [deg] in [stepDeg] increments. */
    private fun twist(d: Driver, deg: Double, stepDeg: Double = 9.0, from: Double = 0.0, down: Boolean = true) {
        val f = d.finger
        fun at(a: Double) = Vec2(f.x + 100.0 * cos(a * PI / 180.0), f.y + 100.0 * sin(a * PI / 180.0))
        if (down) {
            d.down1(at(from)); d.frame()
        }
        var a = from
        val dir = if (deg >= 0) 1.0 else -1.0
        val end = from + deg
        while ((end - a) * dir > 1e-9) {
            a += dir * minOf(stepDeg, Math.abs(end - a))
            d.move1(at(a)); d.frame()
        }
        d.frame()
    }

    // REQ-017.A1 - "A 90 degree twist turns the piece exactly two steps."
    // REQ-017 rules: boundaries at +-22.5, +-67.5 ...; DA-18: cumulative, continuous (a 270 degree twist is six steps); a positive
    // (clockwise on a y-down screen) twist is a positive Turn (TYPE-003).
    @Test
    fun req017_A1_aNinetyDegreeTwistTurnsThePieceExactlyTwoSteps() {
        run {
            val (d, t0) = twistFixture()
            twist(d, 90.0)
            assertEquals(Math.floorMod(t0 + 2, 8), pose(d).turn.steps)
        }
        run { // anticlockwise
            val (d, t0) = twistFixture()
            twist(d, -90.0)
            assertEquals(Math.floorMod(t0 - 2, 8), pose(d).turn.steps)
        }
        run { // step boundary at 67.5: 67 is one step, 68 is two
            val (d, t0) = twistFixture()
            twist(d, 67.0, 4.0)
            assertEquals(Math.floorMod(t0 + 1, 8), pose(d).turn.steps)
            twist(d, 1.0, 1.0, from = 67.0, down = false)
            assertEquals(Math.floorMod(t0 + 2, 8), pose(d).turn.steps)
        }
        run { // continuous: 270 degrees = 6 steps
            val (d, t0) = twistFixture()
            twist(d, 270.0, 9.0)
            assertEquals(Math.floorMod(t0 + 6, 8), pose(d).turn.steps)
        }
        run { // the piece's position is not disturbed: a twist turns it about its centre (TYPE-003), the drag goes on
            val (d, _) = twistFixture()
            val before = centroidOf(worldDp(d, pose(d)))
            twist(d, 90.0)
            val after = centroidOf(worldDp(d, pose(d)))
            assertEquals(before.x, after.x, 0.75)
            assertEquals(before.y, after.y, 0.75)
        }
        run { // lifting the second finger keeps the turn; the drag continues; lifting the drag finger ends it
            val (d, t0) = twistFixture()
            twist(d, 90.0)
            d.up1(Vec2(d.finger.x, d.finger.y + 100.0)); d.frame()
            assertEquals(Math.floorMod(t0 + 2, 8), pose(d).turn.steps)
            assertNotNull("the drag continues", d.session.drag)
            d.move0(Vec2(d.finger.x + 20.0, d.finger.y)); d.frame()
            assertNotNull(d.session.drag)
            d.up0(d.finger); d.frame()
            assertNull("lifting the drag finger releases", d.session.drag)
        }
        run { // a new twist after the first twist finger lifted works, and counts on from the turn reached
            val (d, t0) = twistFixture()
            twist(d, 90.0)
            d.up1(Vec2(d.finger.x, d.finger.y + 100.0)); d.frame()
            twist(d, 90.0)
            assertEquals(Math.floorMod(t0 + 4, 8), pose(d).turn.steps)
        }
        run { // the drag finger lifted first: release at once, the second finger left on screen changes nothing
            val (d, _) = twistFixture()
            twist(d, 45.0)
            d.up0(d.finger); d.frame()
            assertNull(d.session.drag)
            d.move1(Vec2(d.finger.x + 80.0, d.finger.y + 80.0)); d.frame()
            assertNull(d.session.drag)
            assertTrue(d.session.pieces.none { it.where.javaClass.simpleName == "Dragged" })
        }
    }

    // REQ-017.A2 - "A 20 degree twist does not turn the piece."
    // (REQ-017 ASSUMPTION: the twist works only while a piece is being dragged - so a second finger while only pressed does nothing.)
    @Test
    fun req017_A2_aTwentyDegreeTwistDoesNotTurnThePiece() {
        run {
            val (d, t0) = twistFixture()
            twist(d, 20.0, 5.0)
            assertEquals(t0, pose(d).turn.steps)
            twist(d, -20.0, 5.0, from = 20.0, down = false) // back through zero: still no step
            assertEquals(t0, pose(d).turn.steps)
            twist(d, -22.0, 5.0, from = 0.0, down = false) // 22 degrees the other way: still none
            assertEquals(t0, pose(d).turn.steps)
        }
        run { // 23 degrees passes the boundary
            val (d, t0) = twistFixture()
            twist(d, 23.0, 5.0)
            assertEquals(Math.floorMod(t0 + 1, 8), pose(d).turn.steps)
        }
        run { // a second finger while the first is only pressed (drag not begun) is ignored, and does not turn anything later
            val d = Driver(puzzle("shapes-mini-1"))
            val cell = pressPoint(d.layout.cellR(PieceId.ST1))
            val t0 = d.session.ps(PieceId.ST1).turn
            d.down0(cell); d.frame()
            d.down1(Vec2(cell.x + 100.0, cell.y)); d.frame()
            for (a in 1..10) { d.move1(Vec2(cell.x + 100.0 * cos(a * 9.0 * PI / 180), cell.y + 100.0 * sin(a * 9.0 * PI / 180))); d.frame() }
            d.move0(Vec2(cell.x, cell.y - 30.0)); d.frame() // now the drag begins
            assertEquals("no twist was applied from the ignored finger", t0, pose(d).turn)
            d.machine.cancel()
        }
    }

    @Suppress("unused")
    private fun unusedImports(p: PlacedPiece) = p
}
