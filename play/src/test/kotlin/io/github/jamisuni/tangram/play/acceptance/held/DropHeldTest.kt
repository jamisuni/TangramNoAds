package io.github.jamisuni.tangram.play.acceptance.held

import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.lock.LockSearch
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Turn
import io.github.jamisuni.tangram.play.DragPose
import io.github.jamisuni.tangram.play.DropResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

/** A resolver that counts calls: F5 says an interrupted drag never reaches the lock search (design seam: counting subclass). */
internal class CountingResolver(p: Puzzle) : DropResolver(p) {
    var previews = 0
    var releases = 0

    override fun preview(pose: DragPose, placed: List<PlacedPiece>, dpPerUnit: Double): PlacedPiece? {
        previews++
        return super.preview(pose, placed, dpPerUnit)
    }

    override fun release(pose: DragPose, placed: List<PlacedPiece>, dpPerUnit: Double) = super.release(pose, placed, dpPerUnit).also { releases++ }
}

/** HELD-OUT: REQ-012 A2, REQ-018 A3, REQ-021 A1/A2, REQ-051 A1 and the decision tests F5/DA-5, F2, DA-4 at engine level. */
class DropHeldTest {

    private fun pose(d: Driver): DragPose = d.session.drag?.frame?.pose ?: error("no drag frame")

    private fun assertInvariant(d: Driver, what: String) {
        val s = d.session
        val dragged = s.pieces.filter { s.where(it.piece) == "Dragged" }.map { it.piece }
        for (ps in s.pieces) {
            assertTrue("$what: ${ps.piece} is Tray, Board or Dragged, not ${s.where(ps.piece)}", s.where(ps.piece) in setOf("Tray", "Board", "Dragged"))
        }
        assertEquals("$what: Dragged exactly for the piece being dragged", listOfNotNull(s.drag?.piece), dragged)
        assertEquals("$what: placed = the pieces on the board", s.pieces.filter { s.where(it.piece) == "Board" }.map { it.piece }.toSet(), s.placed.map { it.piece }.toSet())
    }

    // REQ-012.A2 - "Every piece is always either in the tray, on the board, or being dragged."
    // A seeded random script of taps, drags, drops, twists, cancels and interruptions; the invariant is checked after every
    // step and in the middle of every drag. (REQ-020 rule: "There is no loose (unlocked) piece on the board at any time.")
    @Test
    fun req012_A2_everyPieceIsAlwaysInTheTrayOnTheBoardOrBeingDragged() {
        for (id in listOf("shapes-mini-1", "shapes-square")) {
            val rnd = Random(20261002L)
            val d = Driver(puzzle(id), reduced = true)
            val pieces = d.session.pieces.map { it.piece }
            val targets = targetsOf(d.puzzle)
            repeat(70) { step ->
                val piece = pieces[rnd.nextInt(pieces.size)]
                val inTray = d.session.where(piece) == "Tray"
                val from = if (inTray) pressPoint(d.layout.cellR(piece)) else d.layout.toDp(centroidOf(polyOf(d.session.placed.first { it.piece == piece })))
                val b = d.layout.boardR()
                val to = Vec2(b.l + rnd.nextDouble() * b.w, b.t + rnd.nextDouble() * b.h)
                when (rnd.nextInt(6)) {
                    0 -> d.tap(from)
                    1 -> { // a drag to a random spot, released
                        d.startDrag(from); d.frame(200); d.finger = to; d.move0(to); d.frame(); assertInvariant(d, "mid-drag $step"); d.up0(to); d.frame()
                    }
                    2 -> { // a drag to its true place, released
                        val tg = targets.first { it.piece == piece }
                        d.startDrag(from); d.steerOrigin(v(tg.at)); assertInvariant(d, "mid-drag $step"); d.up0(d.finger); d.frame()
                    }
                    3 -> { d.startDrag(from); d.frame(100); assertInvariant(d, "pre-cancel $step"); d.machine.cancel(); d.frame() }
                    4 -> { d.startDrag(from); d.frame(100); d.down1(Vec2(d.finger.x + 90.0, d.finger.y)); d.move1(Vec2(d.finger.x, d.finger.y + 90.0)); d.frame(); assertInvariant(d, "twist $step"); d.up1(Vec2(d.finger.x, d.finger.y + 90.0)); d.up0(d.finger); d.frame() }
                    else -> { d.startDrag(from); d.frame(100); d.session.interruptDrag(); d.frame() }
                }
                assertInvariant(d, "after step $step of $id")
                assertNull("nothing is dragged when no finger is down (step $step)", d.session.drag)
                // finger bookkeeping of the scripted trace: lift anything still down so the next step starts clean
                d.machine.cancel()
            }
        }
    }

    // (turn, x, y) of the parallelogram in shapes-square, where its place is at the edge of the silhouette
    // REQ-018.A3 - "A parallelogram dropped in its mirror image of a fitting spot does not lock there."
    @Test
    fun req018_A3_aParallelogramDroppedInTheMirrorImageOfAFittingSpotDoesNotLock() {
        val p = puzzle("shapes-square")
        val pg = targetsOf(p).first { it.piece == PieceId.PG }

        // control: dropped on the fitting spot as it is, it locks
        val control = Driver(p)
        control.place(pg)
        assertEquals("control: the fitting spot locks", pg.at, control.session.placed.single().at)

        // the mirror image of that spot: the mirrored piece about the same centre
        val d = Driver(p)
        val c = centroidOf(polyOf(pg))
        val off = centroidOffset(PieceId.PG, pg.turn, true)
        val origin = Vec2(c.x - off.x, c.y - off.y)
        assertNull(
            "fixture: the kernel finds no lock for the mirror image of this spot",
            LockSearch.find(silhouetteOf(p), emptyList(), PieceId.PG, pg.turn, true, origin, LockSearch.lockDistance(d.layout.dpPerUnit)),
        )
        d.tap(d.layout.badgeR(d.session).centre) // mirror in the tray (REQ-018)
        assertTrue(d.session.ps(PieceId.PG).mirrored)
        val taps = Math.floorMod(pg.turn.steps - d.session.ps(PieceId.PG).turn.steps, 8)
        val cell = d.layout.cellR(PieceId.PG)
        repeat(taps) { d.tap(pressPoint(cell)) }
        d.startDrag(pressPoint(cell))
        d.steerOrigin(origin)
        d.up0(d.finger)
        d.frame()
        assertTrue("nothing locked", d.session.placed.isEmpty())
        assertEquals("the piece went home", "Tray", d.session.where(PieceId.PG))
        assertTrue("and keeps its mirror (decisions F2)", d.session.ps(PieceId.PG).mirrored)
    }

    // REQ-021.A1 - "Releasing the piece always locks it where the preview was drawn."
    // Seeded random drag traces over several puzzles and board states. After the last displayed frame the preview is read
    // (design: frame.preview); a finger move that arrives after that frame must not change the release.
    @Test
    fun req021_A1_releasingAlwaysLocksWhereThePreviewWasDrawn() {
        val rnd = Random(77L)
        var locked = 0
        var home = 0
        for (id in listOf("shapes-mini-1", "shapes-square", "shapes-warmup-2", "animals-cat")) {
            val p = puzzle(id)
            repeat(40) { trace ->
                val d = Driver(p, reduced = true)
                val targets = buildOrder(p, targetsOf(p), d.layout.dpPerUnit)
                // a random prefix of the build order is already on the board
                val prefix = rnd.nextInt(targets.size)
                for (t in targets.take(prefix)) d.place(t)
                val remaining = d.session.pieces.map { it.piece }.filter { d.session.where(it) == "Tray" }
                val piece = remaining[rnd.nextInt(remaining.size)]
                val b = d.layout.boardR()
                d.startDrag(pressPoint(d.layout.cellR(piece)))
                val tg = targets.first { it.piece == piece }
                repeat(2 + rnd.nextInt(5)) {
                    // positions anywhere on the screen, a third of them within reach of the piece's true place
                    val f = if (rnd.nextInt(3) == 0) {
                        val a = d.layout.toDp(v(tg.at)); Vec2(a.x + rnd.nextDouble() * 60 - 30, a.y + 60.0 + rnd.nextDouble() * 80)
                    } else Vec2(b.l + rnd.nextDouble() * b.w, b.t + rnd.nextDouble() * (b.h + 200))
                    d.finger = f
                    d.move0(f)
                    d.frame(8 + rnd.nextInt(30).toLong())
                }
                // optionally turn the piece by a twist so that turns other than the resting one are exercised
                d.frame(150)
                val shown: PlacedPiece? = d.session.drag?.frame?.preview
                val shownPose = pose(d)
                if (rnd.nextBoolean()) { // a move after the last displayed frame
                    val late = Vec2(d.finger.x + 55.0, d.finger.y - 35.0)
                    d.move0(late)
                    d.up0(late)
                } else {
                    d.up0(d.finger)
                }
                d.frame()
                val now = d.session.placed.firstOrNull { it.piece == piece }
                if (shown != null) {
                    locked++
                    assertNotNull("$id/$trace: a preview was shown, so the drop must lock", now)
                    assertEquals("$id/$trace: locked position = previewed position", shown.at, now!!.at)
                    assertEquals(shown.turn, now.turn)
                    assertEquals(shown.mirrored, now.mirrored)
                    assertEquals(shownPose.turn, now.turn)
                } else {
                    home++
                    assertNull("$id/$trace: no preview was shown, so the drop must not lock", now)
                    assertEquals("Tray", d.session.where(piece))
                }
            }
        }
        assertTrue("fixture: traces produced locks ($locked) and misses ($home)", locked >= 5 && home >= 5)
    }

    // REQ-021 rule (no acceptance ID): "Updated at most once per displayed frame."
    @Test
    fun req021_rule_thePreviewIsSearchedAtMostOncePerFrame() {
        val p = puzzle("shapes-mini-1")
        val counting = CountingResolver(p)
        val d = Driver(p, resolver = counting)
        d.startDrag(pressPoint(d.layout.cellR(PieceId.ST1)))
        d.frame(200)
        val before = counting.previews
        for (i in 1..10) d.move0(Vec2(120.0 + i, 300.0))
        d.frame(16)
        assertTrue("ten moves inside one frame cost at most one preview search, cost ${counting.previews - before}", counting.previews - before <= 1)
        d.machine.cancel()
    }

    // REQ-021.A2 - "With no valid position in reach, no preview is drawn and the drop goes home (REQ-020)."
    @Test
    fun req021_A2_withNoValidPositionInReachNoPreviewIsDrawnAndTheDropGoesHome() {
        val p = puzzle("shapes-mini-1")
        val piece = PieceId.ST1
        // (a) over the board, but nowhere a lock exists for this pose (kernel oracle)
        run {
            val d = Driver(p)
            val ps = d.session.ps(piece)
            val origin = missOrigin(p, d.layout.dpPerUnit, piece, ps.turn, ps.mirrored, d.session.placed)
            d.startDrag(pressPoint(d.layout.cellR(piece)))
            d.steerOrigin(origin)
            assertNull("no preview where no position is valid", d.session.drag?.frame?.preview)
            d.up0(d.finger); d.frame()
            assertEquals("the drop goes home", "Tray", d.session.where(piece))
            assertTrue(d.session.placed.isEmpty())
        }
        // (b) at the true place but one 45 degree step off (TYPE-004: a piece one step off does not lock)
        run {
            val d = Driver(p)
            val tg = targetsOf(p).first { it.piece == piece }
            d.tap(pressPoint(d.layout.cellR(piece))) // one step off the resting turn, which is the target turn
            assertFalse(d.session.ps(piece).turn == tg.turn)
            assertNull(
                "fixture: the kernel finds no lock for the piece one step off",
                LockSearch.find(silhouetteOf(p), emptyList(), piece, d.session.ps(piece).turn, false, v(tg.at), LockSearch.lockDistance(d.layout.dpPerUnit)),
            )
            d.startDrag(pressPoint(d.layout.cellR(piece)))
            d.steerOrigin(v(tg.at))
            assertNull("a piece one step off shows no preview", d.session.drag?.frame?.preview)
            d.up0(d.finger); d.frame()
            assertEquals("Tray", d.session.where(piece))
        }
        // (c) at the true place and turn, but over the tray (the finger deep in the tray): not over the board, no search
        run {
            val d = Driver(p)
            d.startDrag(pressPoint(d.layout.cellR(piece)))
            d.frame(200)
            val low = Vec2(d.layout.cellR(PieceId.SQ).cx, AREA_H - 2.0)
            d.finger = low
            d.move0(low)
            d.frame(200)
            val pz = pose(d)
            assertFalse("fixture: the piece is not over the board", pz.overBoard)
            assertNull(d.session.drag?.frame?.preview)
            d.up0(low); d.frame()
            assertEquals("Tray", d.session.where(piece))
        }
    }

    // REQ-051.A1 - "In a mini puzzle, a drop in the middle of the silhouette sends the piece home and the corners pulse once."
    // REQ-051 rules: "shows only the outline corners, never a piece position".
    @Test
    fun req051_A1_inAMiniPuzzleAMissedDropOverTheBoardPulsesTheOutlineCorners() {
        for (id in listOf("shapes-mini-1", "shapes-mini-2")) {
            val p = puzzle(id)
            val d = Driver(p)
            val piece = d.session.pieces.first().piece
            assertNull("no pulse before the miss", d.session.pulse)
            val centre = d.missDrop(piece)
            assertTrue("fixture: the drop was over the board", d.layout.overBoard(centre))
            assertEquals("$id: the piece went home", "Tray", d.session.where(piece))
            val pulse = d.session.pulse
            assertNotNull("$id: a pulse starts", pulse)
            assertEquals(
                "$id: the pulse carries exactly the silhouette's outline corners",
                silhouetteOf(p).outlineCorners.toSet(), pulseCorners(pulse),
            )
        }
    }

    // decision DA-4 (WO-001): "mini puzzles only, only when the missed drop was over the board (a drop on the tray or outside
    // the board goes home without a pulse)".
    @Test
    fun decision_DA4_aMiniDropOnTheTrayGoesHomeWithoutAPulse() {
        val p = puzzle("shapes-mini-1")
        val d = Driver(p)
        val piece = PieceId.ST1
        d.startDrag(pressPoint(d.layout.cellR(piece)))
        d.frame(200)
        val low = Vec2(d.layout.cellR(PieceId.SQ).cx, AREA_H - 2.0)
        d.finger = low
        d.move0(low)
        d.frame(200)
        assertFalse("fixture: not over the board", pose(d).overBoard)
        d.up0(low); d.frame()
        assertEquals("Tray", d.session.where(piece))
        assertNull("no pulse for a drop on the tray", d.session.pulse)
    }

    // decision F2 - "A piece that goes back to the tray keeps the turn and mirror it had".
    @Test
    fun decision_F2_aPieceSentHomeKeepsItsTurnAndMirror() {
        val d = Driver(puzzle("shapes-warmup-1"))
        d.tap(d.layout.badgeR(d.session).centre) // PG mirrored
        repeat(3) { d.tap(pressPoint(d.layout.cellR(PieceId.PG))) }
        val before = d.session.ps(PieceId.PG)
        assertTrue(before.mirrored)
        d.missDrop(PieceId.PG)
        assertEquals("Tray", d.session.where(PieceId.PG))
        assertEquals(before.turn, d.session.ps(PieceId.PG).turn)
        assertTrue(d.session.ps(PieceId.PG).mirrored)
        // and a tray piece dropped on the tray cell itself
        val st = d.session.ps(PieceId.ST2)
        d.tap(pressPoint(d.layout.cellR(PieceId.ST2)))
        val turned = d.session.ps(PieceId.ST2).turn
        assertEquals(Turn((st.turn.steps + 1) % 8), turned)
        d.startDrag(pressPoint(d.layout.cellR(PieceId.ST2)))
        d.frame(200)
        d.up0(d.finger); d.frame()
        assertEquals(turned, d.session.ps(PieceId.ST2).turn)
    }

    // decision F5 / DA-5 - "A drag that ends without a release (system cancel, app hidden, window change) returns the piece
    // silently to where it was picked up" and "an interrupted drag never reaches the lock search; the board restores the whole
    // pick-up pose (position, turn, mirror)"; "A cancel while Pressed is not a tap".
    @Test
    fun decision_F5_anInterruptedDragRestoresTheWholePickUpPoseSilently() {
        for (how in listOf("cancel", "interruptDrag")) {
            // -- from the tray, over a spot where the piece WOULD lock, with a twist applied
            run {
                val p = puzzle("shapes-mini-1")
                val counting = CountingResolver(p)
                val d = Driver(p, resolver = counting)
                val piece = PieceId.ST1
                val tg = targetsOf(p).first { it.piece == piece }
                val start = d.session.ps(piece)
                d.startDrag(pressPoint(d.layout.cellR(piece)))
                d.steerOrigin(v(tg.at))
                assertNotNull("fixture: a preview is showing (it would lock)", d.session.drag?.frame?.preview)
                d.down1(Vec2(d.finger.x + 100.0, d.finger.y)); d.frame()
                for (a in 1..10) { d.move1(Vec2(d.finger.x + 100.0 * Math.cos(a * 9.0 * Math.PI / 180), d.finger.y + 100.0 * Math.sin(a * 9.0 * Math.PI / 180))); d.frame() }
                assertTrue("fixture: the twist changed the dragged turn", pose(d).turn != start.turn)
                val previews = counting.previews
                val releases = counting.releases
                if (how == "cancel") d.machine.cancel() else d.session.interruptDrag()
                d.frame()
                assertEquals("$how: back in its cell", "Tray", d.session.where(piece))
                assertEquals("$how: pick-up turn restored, twist discarded", start.turn, d.session.ps(piece).turn)
                assertEquals(start.mirrored, d.session.ps(piece).mirrored)
                assertTrue("$how: nothing locked", d.session.placed.isEmpty())
                assertNull("$how: no pulse", d.session.pulse)
                assertFalse(d.session.isDragging)
                assertNull(d.session.drag)
                assertEquals("$how: no lock search after the interruption (previews)", previews, counting.previews)
                assertEquals("$how: no release", releases, counting.releases)
                assertEquals("the drag did start, so the puzzle stays started (TYPE-006)", PuzzleState.IN_PROGRESS, d.session.state)
                // the surviving fingers change nothing and raise nothing
                d.move0(Vec2(d.finger.x + 5.0, d.finger.y)); d.frame()
                d.up1(Vec2(d.finger.x, d.finger.y)); d.up0(d.finger); d.frame()
                assertTrue(d.session.placed.isEmpty())
                assertEquals(start.turn, d.session.ps(piece).turn)
                assertEquals("Tray", d.session.where(piece))
                d.session.interruptDrag() // idempotent
            }
            // -- from the board: the exact position, turn and mirror come back
            run {
                val p = puzzle("shapes-square")
                val counting = CountingResolver(p)
                val d = Driver(p, resolver = counting)
                val targets = targetsOf(p)
                d.placeAll(targets.filter { it.piece == PieceId.PG || it.piece == PieceId.SQ })
                val before = d.session.placed.toList()
                val pg = before.first { it.piece == PieceId.PG }
                d.startDrag(d.layout.toDp(centroidOf(polyOf(pg))))
                d.frame(200)
                val away = Vec2(d.finger.x + 40.0, d.finger.y - 90.0)
                d.finger = away; d.move0(away); d.frame(200)
                d.down1(Vec2(away.x + 100.0, away.y)); d.frame()
                for (a in 1..10) { d.move1(Vec2(away.x + 100.0 * Math.cos(a * 9.0 * Math.PI / 180), away.y + 100.0 * Math.sin(a * 9.0 * Math.PI / 180))); d.frame() }
                val releases = counting.releases
                val previews = counting.previews
                if (how == "cancel") d.machine.cancel() else d.session.interruptDrag()
                d.frame()
                assertEquals("$how: the board is exactly as before (position, turn, mirror)", before.toSet(), d.session.placed.toSet())
                assertEquals("Board", d.session.where(PieceId.PG))
                assertEquals(pg.turn, d.session.ps(PieceId.PG).turn)
                assertEquals(pg.mirrored, d.session.ps(PieceId.PG).mirrored)
                assertEquals(releases, counting.releases)
                assertEquals(previews, counting.previews)
                assertNull(d.session.pulse)
                assertFalse(d.session.isDragging)
            }
            // -- a mirrored tray piece comes back mirrored
            run {
                val d = Driver(puzzle("shapes-square"))
                d.tap(d.layout.badgeR(d.session).centre)
                val start = d.session.ps(PieceId.PG)
                assertTrue(start.mirrored)
                d.startDrag(pressPoint(d.layout.cellR(PieceId.PG)))
                d.frame(300)
                if (how == "cancel") d.machine.cancel() else d.session.interruptDrag()
                d.frame()
                assertEquals(start.mirrored, d.session.ps(PieceId.PG).mirrored)
                assertEquals(start.turn, d.session.ps(PieceId.PG).turn)
            }
        }
        // cancel while only Pressed is not a tap
        run {
            val d = Driver(puzzle("shapes-mini-1"))
            val at = pressPoint(d.layout.cellR(PieceId.ST1))
            val t0 = d.session.ps(PieceId.ST1).turn
            d.down0(at); d.frame(100)
            d.machine.cancel()
            d.up0(at); d.frame()
            assertEquals("a cancelled press does not turn the piece", t0, d.session.ps(PieceId.ST1).turn)
        }
        // G-10: intents without a drag are no-ops, release returns null
        run {
            val d = Driver(puzzle("shapes-mini-1"))
            assertNull(d.session.release(d.t))
            d.session.interruptDrag()
            d.session.dragTo(Vec2(10.0, 10.0))
            d.session.setDragTurn(Turn(3))
            assertTrue(d.session.pieces.all { d.session.where(it.piece) == "Tray" })
            assertEquals(PuzzleState.NEW, d.session.state)
        }
        // an external interrupt (app hidden) while a finger is down: its next events are ignored
        run {
            val d = Driver(puzzle("shapes-mini-1"))
            d.startDrag(pressPoint(d.layout.cellR(PieceId.ST1)))
            d.frame(200)
            d.session.interruptDrag()
            d.move0(Vec2(150.0, 250.0)); d.frame(); d.up0(Vec2(150.0, 250.0)); d.frame()
            assertEquals("Tray", d.session.where(PieceId.ST1))
            assertTrue(d.session.placed.isEmpty())
            // a third finger is ignored
            val e = Driver(puzzle("shapes-mini-1"))
            e.startDrag(pressPoint(e.layout.cellR(PieceId.ST1)))
            e.frame(200)
            e.down1(Vec2(e.finger.x + 100.0, e.finger.y)); e.frame()
            val turn = pose(e).turn
            e.down2(Vec2(e.finger.x - 100.0, e.finger.y)); e.move2(Vec2(e.finger.x - 60.0, e.finger.y + 80.0)); e.frame()
            assertEquals("a third finger changes nothing", turn, pose(e).turn)
            e.up2(Vec2(0.0, 0.0))
            e.machine.cancel()
        }
    }
}
