package io.github.jamisuni.tangram.play.acceptance.held

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.play.PlaySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * HELD-OUT, on a device with injected touches (performTouchInput, never performClick): the adapter cases of the design
 * ("Adapter on device"), REQ-015 A1/A2, REQ-017 A1, REQ-020 A2, REQ-021 A1/A2 and REQ-051 A1 on screen, and decision F5.
 */
class GestureDeviceHeldTest {

    @get:Rule
    val rule = createComposeRule()

    private val d get() = rule.density.density
    private fun off(p: Vec2) = Offset((p.x * d).toFloat(), (p.y * d).toFloat())
    private fun area() = rule.onNodeWithTag("play-area")
    private fun advance(ms: Long) = rule.mainClock.advanceTimeBy(ms)

    private class Rig(val session: PlaySession, val layout: io.github.jamisuni.tangram.play.PlayLayout, val driver: TouchDriver, val p: io.github.jamisuni.tangram.contracts.puzzle.Puzzle)

    private fun rig(id: String, reduced: Boolean = true): Rig {
        val p = puzzle(id)
        val session = PlaySession(p, { reduced })
        rule.showPlay(session)
        val l = layoutFor(p)
        return Rig(session, l, TouchDriver(rule, session, l, p), p)
    }

    private fun turnNow(s: PlaySession): Int? = rule.runOnUiThread { s.drag?.frame?.pose?.turn?.steps }

    // REQ-017.A1 (device) - "A 90 degree twist turns the piece exactly two steps": pointer 1 is rotated 90 degrees about pointer 0
    // by real multi-touch injection while pointer 0 drags the piece.
    @Test
    fun req017_A1_onDeviceANinetyDegreeTwistOfPointerOneAboutPointerZeroTurnsThePieceTwoSteps() {
        val r = rig("shapes-mini-1")
        r.driver.startDrag(pressPoint(r.layout.cellR(PieceId.ST1)))
        r.driver.steerOrigin(Vec2(1.0, 1.5))
        advance(100)
        val t0 = turnNow(r.session)!!
        val f = r.driver.finger
        fun at(a: Double) = Vec2(f.x + 100.0 * cos(a * PI / 180), f.y + 100.0 * sin(a * PI / 180))
        area().performTouchInput { down(1, off(at(0.0))) }
        advance(16)
        for (a in 1..10) {
            area().performTouchInput { moveTo(1, off(at(a * 9.0))) }
            advance(16)
        }
        advance(32)
        assertEquals(Math.floorMod(t0 + 2, 8), turnNow(r.session))
        area().performTouchInput { up(1) }
        advance(32)
        assertEquals("lifting the second finger keeps the turn", Math.floorMod(t0 + 2, 8), turnNow(r.session))
        assertNotNull("and the drag continues", rule.runOnUiThread { r.session.drag })
        area().performTouchInput { up(0) }
        advance(300)
    }

    // REQ-015.A1 (device) - "A 3 s still press on a piece acts as a tap" (here with 8 dp of movement, still under 12 dp).
    @Test
    fun req015_A1_onDeviceAThreeSecondPressWithEightDpOfMovementIsATap() {
        val r = rig("shapes-mini-1")
        val cell = pressPoint(r.layout.cellR(PieceId.ST1))
        val t0 = r.session.ps(PieceId.ST1).turn
        area().performTouchInput { down(off(cell)) }
        advance(100)
        area().performTouchInput { moveTo(off(Vec2(cell.x + 8.0, cell.y))) }
        advance(3000)
        area().performTouchInput { up() }
        advance(100)
        assertEquals("tapped: one step clockwise", (t0.steps + 1) % 8, r.session.ps(PieceId.ST1).turn.steps)
        assertNull("not a drag", rule.runOnUiThread { r.session.drag })
        assertEquals("Tray", r.session.where(PieceId.ST1))
    }

    // REQ-015.A2 (device) - "A touch 10 dp outside a small triangle's edge picks it up."
    @Test
    fun req015_A2_onDeviceATouchTenDpOutsideASmallTriangleOnTheBoardPicksItUp() {
        val r = rig("shapes-mini-1")
        r.driver.place(targetsOf(r.p).first { it.piece == PieceId.ST1 })
        val st = r.session.placed.single { it.piece == PieceId.ST1 }
        val poly = polyOf(st).map { r.layout.toDp(it) }
        val c = centroidOf(poly)
        var picked = 0
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            val len = hypot(b.x - a.x, b.y - a.y)
            var n = Vec2(-(b.y - a.y) / len, (b.x - a.x) / len)
            val m = Vec2((a.x + b.x) / 2, (a.y + b.y) / 2)
            if ((m.x - c.x) * n.x + (m.y - c.y) * n.y < 0) n = Vec2(-n.x, -n.y)
            val p10 = Vec2(m.x + 10.0 * n.x, m.y + 10.0 * n.y)
            if (p10.y > r.layout.boardR().b) continue
            area().performTouchInput { down(off(p10)) }
            advance(16)
            area().performTouchInput { moveTo(off(Vec2(p10.x, p10.y - 40.0))) }
            advance(150)
            assertEquals("edge $i: the triangle was picked up", PieceId.ST1, rule.runOnUiThread { r.session.drag?.piece })
            area().performTouchInput { cancel() }
            advance(300)
            assertEquals("a cancelled pick-up puts it back (F5)", "Board", r.session.where(PieceId.ST1))
            picked++
        }
        assertTrue("fixture: touched on at least two edges", picked >= 2)
    }

    // decision F5 / DA-5 (device): "A drag that ends without a release returns the piece silently to where it was picked up."
    @Test
    fun decision_F5_onDeviceASystemCancelRestoresThePickUpPoseFromTrayAndBoard() {
        // (1) tray piece held over a spot where it would lock, then the system cancels the gesture
        run {
            val r = rig("shapes-mini-1")
            val piece = PieceId.ST1
            val tg = targetsOf(r.p).first { it.piece == piece }
            val start = r.session.ps(piece)
            r.driver.startDrag(pressPoint(r.layout.cellR(piece)))
            r.driver.steerOrigin(v(tg.at))
            assertNotNull("fixture: it would lock here", rule.runOnUiThread { r.session.drag?.frame?.preview })
            area().performTouchInput { cancel() }
            advance(500)
            assertEquals("Tray", r.session.where(piece))
            assertEquals(start.turn, r.session.ps(piece).turn)
            assertTrue("no lock", r.session.placed.isEmpty())
            assertNull("no pulse", r.session.pulse)
            assertEquals("started stays started (TYPE-006)", PuzzleState.IN_PROGRESS, r.session.state)
        }
        // (2) board piece picked up, carried away, cancelled: it is back, exactly
        run {
            val r = rig("shapes-mini-1")
            r.driver.place(targetsOf(r.p).first { it.piece == PieceId.ST1 })
            val before = r.session.placed.single()
            r.driver.startDrag(r.layout.toDp(centroidOf(polyOf(before))))
            r.driver.moveTo(Vec2(r.driver.finger.x + 30.0, r.driver.finger.y - 80.0))
            advance(200)
            area().performTouchInput { cancel() }
            advance(500)
            assertEquals(before, r.session.placed.single())
            assertNull(r.session.pulse)
        }
        // (3) cancel while only pressed is not a tap
        run {
            val r = rig("shapes-mini-1")
            val cell = pressPoint(r.layout.cellR(PieceId.ST1))
            val t0 = r.session.ps(PieceId.ST1).turn
            area().performTouchInput { down(off(cell)) }
            advance(200)
            area().performTouchInput { cancel() }
            advance(200)
            assertEquals(t0, r.session.ps(PieceId.ST1).turn)
        }
    }

    private fun texts(): List<String> {
        val out = ArrayList<SemanticsNode>()
        fun walk(n: SemanticsNode) { out.add(n); n.children.forEach { walk(it) } }
        walk(rule.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        return out.flatMap { n ->
            (n.config.getOrNull(SemanticsProperties.Text)?.map { it.text } ?: emptyList()) +
                (n.config.getOrNull(SemanticsProperties.ContentDescription) ?: emptyList()) +
                (n.config.getOrNull(SemanticsProperties.Error)?.let { listOf(it) } ?: emptyList())
        }.sorted()
    }

    // REQ-020.A2 - "No error message is shown for a missed drop."
    // The set of texts on screen is identical before, during the 180 ms return and after a missed drop (a mini puzzle also pulses).
    @Test
    fun req020_A2_aMissedDropShowsNoMessage() {
        for (id in listOf("shapes-mini-1", "shapes-warmup-1")) {
            val r = rig(id)
            val before = texts()
            assertTrue("fixture: the screen has some text to compare (badge or marks)", before.isNotEmpty() || id == "shapes-mini-1")
            val piece = r.session.pieces.first { r.session.where(it.piece) == "Tray" }.piece
            r.driver.missDrop(piece)
            assertEquals("$id: texts during the return", before, texts())
            advance(100)
            assertEquals("$id: texts mid-return", before, texts())
            advance(800)
            assertEquals("$id: texts after", before, texts())
            assertEquals("the piece is home", "Tray", r.session.where(piece))
        }
    }

    private fun worldDp(r: Rig): List<Vec2> {
        val pose = rule.runOnUiThread { r.session.drag?.frame?.pose }!!
        return PieceGeometry.offsets(pose.piece.shape, pose.turn, pose.mirrored)
            .map { r.layout.toDp(Vec2(pose.origin.x + it.x.toDouble(), pose.origin.y + it.y.toDouble())) }
    }

    // REQ-021.A1 (device) - "Releasing the piece always locks it where the preview was drawn."
    // The small triangle is held 0.3 units to the left of its place: a white dashed outline (REQ-021 ASSUMPTION: white, dashed,
    // 2.5 dp) must show along the edge it shares with the square - an inner edge, so white is visible against the silhouette -
    // and releasing locks the piece exactly there.
    @Test
    fun req021_A1_onDeviceAWhiteDashedOutlineShowsWhereThePieceWillLockAndReleaseLocksThere() {
        val r = rig("shapes-mini-1")
        val piece = PieceId.ST1
        val tg = targetsOf(r.p).first { it.piece == piece }
        val before = r.driver.let { rule.shot() }
        r.driver.startDrag(pressPoint(r.layout.cellR(piece)))
        r.driver.steerOrigin(Vec2(tg.at.x.toDouble() - 0.3, tg.at.y.toDouble()))
        advance(100)
        val preview = rule.runOnUiThread { r.session.drag?.frame?.preview }
        assertNotNull("fixture: a landing spot is in reach", preview)
        assertEquals("the preview is the piece's own place", tg.at, preview!!.at)

        val dragged = worldDp(r)
        val a = r.layout.toDp(Vec2(1.0, 1.0))
        val b = r.layout.toDp(Vec2(2.0, 2.0)) // the edge shared by ST1 and SQ, in the preview's polygon
        val shot = rule.shot()
        val len = hypot(b.x - a.x, b.y - a.y)
        var n = 0
        var white = 0
        var s = 0.0
        while (s <= len) {
            val pt = Vec2(a.x + (b.x - a.x) * s / len, a.y + (b.y - a.y) * s / len)
            val covered = inPoly(dragged, pt) || dragged.indices.any { i -> distToSegment(pt, dragged[i], dragged[(i + 1) % dragged.size]) < 5.0 }
            if (!covered) {
                n++
                if (diff(shot.px(pt), rgb(0xFFFFFF)) <= 40) white++
            }
            s += 1.0 / shot.density
        }
        assertTrue("fixture: enough of the preview edge is uncovered ($n samples)", n >= 30)
        val fraction = white.toDouble() / n
        assertTrue("a dashed white line: some, not all, of the edge is white (fraction $fraction)", fraction in 0.2..0.85)
        assertTrue("the same spot is not white without a preview", diff(before.px(Vec2((a.x + b.x) / 2, (a.y + b.y) / 2)), rgb(0xFFFFFF)) > 40)

        r.driver.up()
        advance(400)
        assertEquals("released: locked exactly where the preview was", tg.at, r.session.placed.single().at)
    }

    // REQ-021.A2 (device) - "With no valid position in reach, no preview is drawn and the drop goes home (REQ-020)."
    @Test
    fun req021_A2_onDeviceNoPreviewIsDrawnWhereNothingLocksAndTheDropGoesHome() {
        val r = rig("shapes-mini-1")
        val piece = PieceId.ST1
        val ps = r.session.ps(piece)
        val clean = rule.shot()
        val origin = missOrigin(r.p, r.layout.dpPerUnit, piece, ps.turn, ps.mirrored, r.session.placed)
        r.driver.startDrag(pressPoint(r.layout.cellR(piece)))
        r.driver.steerOrigin(origin)
        advance(100)
        assertNull("no preview", rule.runOnUiThread { r.session.drag?.frame?.preview })
        val dragged = worldDp(r)
        val silColour = clean.px(r.layout.toDp(centroidOf(solutionPolys(r.p)[0])))
        val shot = rule.shot()
        var tested = 0
        for (poly in solutionPolys(r.p)) {
            val c = centroidOf(poly)
            for (k in 0..20) {
                val pt = Vec2(c.x + (poly[k % poly.size].x - c.x) * k / 25.0, c.y + (poly[k % poly.size].y - c.y) * k / 25.0)
                val dp = r.layout.toDp(pt)
                val covered = dragged.indices.any { i -> distToSegment(dp, dragged[i], dragged[(i + 1) % dragged.size]) < 6.0 } || inPoly(dragged, dp)
                if (covered) continue
                tested++
                assertTrue("a white outline is drawn at $pt although nothing would lock", diff(shot.px(dp), silColour) <= 6)
            }
        }
        assertTrue("fixture: enough silhouette pixels were looked at ($tested)", tested >= 20)
        r.driver.up()
        advance(500)
        assertEquals("the drop goes home", "Tray", r.session.where(piece))
        assertTrue(r.session.placed.isEmpty())
    }

    // REQ-051.A1 (device) - "In a mini puzzle, a drop in the middle of the silhouette sends the piece home and the corners pulse once."
    // Marker: a small circle in the accent colour #3D8BFD (REQ-051 ASSUMPTION, DA-20) on every outline corner, for about 600 ms.
    @Test
    fun req051_A1_onDeviceTheCornersShowAnAccentMarkerForAbout600msAfterAMissInAMiniPuzzle() {
        val r = rig("shapes-mini-1", reduced = true)
        val piece = PieceId.SQ
        val none = rule.shot()
        fun accentNear(shot: Shot, c: Vec2): Boolean {
            val cd = r.layout.toDp(c)
            var dy = -6.0
            while (dy <= 6.0) {
                var dx = -6.0
                while (dx <= 6.0) {
                    if (diff(shot.px(Vec2(cd.x + dx, cd.y + dy)), rgb(0x3D8BFD)) <= 24) return true
                    dx += 1.5
                }
                dy += 1.5
            }
            return false
        }
        val corners = silhouetteOf(r.p).outlineCorners.map { v(it) }
        assertTrue("fixture: no marker before the miss", corners.none { accentNear(none, it) })
        val centre = r.driver.missDrop(piece)
        assertTrue("fixture: over the board", r.layout.overBoard(centre))
        advance(250)
        assertEquals("the piece is home", "Tray", r.session.where(piece))
        val during = rule.shot()
        for (c in corners) assertTrue("an accent marker sits on the corner $c", accentNear(during, c))
        advance(1200)
        val after = rule.shot()
        for (c in corners) assertFalse("the marker at $c is gone after the pulse", accentNear(after, c))
    }
}
