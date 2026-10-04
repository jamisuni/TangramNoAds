package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * decision DA-115 (design WO-007 section 3.1 and the acceptance table): WHICH action emits WHICH `PlayEvent`, including the silent ones, and
 * that every event is emitted BEFORE `onChanged` (review N3: `settled()` runs onChanged, persist and an atomic file write on the main
 * thread, so a sound must not wait behind disk I/O). No acceptance token: the sounds are REQ-033's Statement and REQ-020 / REQ-023
 * rules; the table is the AI decision DA-115.
 *
 * The table, from the design:
 *   PICK_UP  when `beginDrag` actually starts a drag.
 *   TURN     a tray tap; a SUCCESSFUL board tap-turn; a twist step that changes the drag's turn (`setDragTurn` runs on every twist move, so
 *            only when the turn differs); a mirror, tray or board, successful (the mirror uses the turn cue).
 *   LOCK     a drop that locks (`DropOutcome.Locked`).   RETURN  a drop that goes home (a miss, or a drop on the tray: `DropOutcome.Home`).
 *   SOLVE    after the LOCK that completes the puzzle, and ALONE for `solveByAid`.
 *   Silent   a refused board turn or mirror (it only shakes: REQ-033 "No sound signals failure"), `interruptDrag`, `restore`, `toProgress`.
 *
 * Seams: `enum class PlayEvent { PICK_UP, TURN, LOCK, RETURN, SOLVE }` and the settable property `PlaySession.onEvent: (PlayEvent) -> Unit`
 * (seam row P-1). The session API used is the existing one (see PlaySessionScaffoldingTest, PlayAnimationScaffoldingTest).
 */
class PlayEventTableTest {

    private val rows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val library = PuzzleLibrary.packaged().puzzles
    private val mini: Puzzle = library.first()
    private val withParallelogram: Puzzle = library.first { p -> p.solution.any { it.piece == PieceId.PG } }
    private val square: Puzzle = library.first { it.id.value == "shapes-square" }

    /** A session with a layout, a recording `onChanged` and `onEvent`; [log] holds "event:X" and "changed" entries in call order. */
    private inner class Rig(val puzzle: Puzzle) {
        val log = ArrayList<String>()
        val session: PlaySession = PlaySession(
            puzzle,
            { false },
            DropResolver(puzzle),
            { log.add("changed") },
            { byAid -> log.add("solved:$byAid") },
        ).also {
            it.layout = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, rows, puzzle)
            it.onEvent = { e -> log.add("event:$e") }
        }

        fun events(): List<PlayEvent> = log.filter { it.startsWith("event:") }.map { PlayEvent.valueOf(it.removePrefix("event:")) }

        fun clear() = log.clear()

        /** Every event of the last action precedes the first `onChanged` of that action (N3). */
        fun assertEventsBeforeChanged(what: String) {
            val changed = log.indexOf("changed")
            val lastEvent = log.indexOfLast { it.startsWith("event:") }
            assertTrue("$what: no event was emitted: $log", lastEvent >= 0)
            if (changed >= 0) assertTrue("$what: an event came after onChanged (disk I/O would delay the sound): $log", lastEvent < changed)
        }

        fun pose(piece: PieceId): PlacedPiece =
            PieceGeometry.poseOf(piece, puzzle.solution.first { it.piece == piece }.polygon)!!

        /** Press [pc]'s piece in the tray and carry it exactly onto [pc], up to (not including) the release. */
        fun carryTo(pc: PlacedPiece, t0: Long) {
            val lay = session.layout!!
            session.beginDrag(pc.piece, lay.cell(pc.piece).centre, t0)
            session.setDragTurn(pc.turn)
            val c = PieceGeometry.centroidOffset(pc.piece.shape, pc.turn, pc.mirrored)
            val centre = lay.toDp(Vec2(pc.at.x.toDouble() + c.x, pc.at.y.toDouble() + c.y))
            session.dragTo(Vec2(centre.x, centre.y + session.drag!!.lift))
            session.onFrame(t0 + 500)
        }

        fun lockAt(pc: PlacedPiece, t0: Long): DropOutcome? {
            carryTo(pc, t0)
            return session.release(t0 + 500)
        }
    }

    // ---- TURN ----------------------------------------------------------------------------------------------------------------

    // DA-115: a tray tap is TURN, once per tap, nothing else; the event precedes onChanged.
    @Test
    fun decisionDA115_aTrayTapEmitsTurnEachTime() {
        val r = Rig(mini)
        val piece = r.session.pieces.first().piece
        repeat(3) {
            r.clear()
            r.session.tapTray(piece)
            assertEquals(listOf(PlayEvent.TURN), r.events())
            r.assertEventsBeforeChanged("tray tap")
        }
        assertEquals("a tray tap does not start the puzzle", PuzzleState.NEW, r.session.state)
    }

    // DA-115: a mirror in the tray uses the turn cue (REQ-018: the parallelogram is mirrored with the badge).
    @Test
    fun decisionDA115_aTrayMirrorEmitsTurn() {
        val r = Rig(withParallelogram)
        r.session.flipTray(PieceId.PG)
        assertTrue("fixture: the mirror happened", r.session.pieces.first { it.piece == PieceId.PG }.mirrored)
        assertEquals(listOf(PlayEvent.TURN), r.events())
        r.assertEventsBeforeChanged("tray mirror")
    }

    // DA-115: a twist step emits TURN only when it changes the drag's turn (`setDragTurn` runs on every twist move).
    @Test
    fun decisionDA115_aTwistStepEmitsTurnOnlyWhenTheTurnChanges() {
        val r = Rig(mini)
        val piece = r.session.pieces.first().piece
        val start = r.session.pieces.first { it.piece == piece }.turn
        r.session.beginDrag(piece, r.session.layout!!.cell(piece).centre, 0)
        val next = Turn((start.steps + 1) % 8)

        r.clear()
        r.session.setDragTurn(next)
        assertEquals("a step that changes the turn", listOf(PlayEvent.TURN), r.events())
        r.assertEventsBeforeChanged("twist step")

        r.clear()
        r.session.setDragTurn(next)
        r.session.setDragTurn(next)
        assertEquals("the same turn again (every twist move calls it): silent", emptyList<PlayEvent>(), r.events())

        r.clear()
        r.session.setDragTurn(start)
        assertEquals("a step back is a change too", listOf(PlayEvent.TURN), r.events())
    }

    // ---- PICK_UP ---------------------------------------------------------------------------------------------------------------

    // DA-115: PICK_UP when beginDrag actually starts a drag (from the tray), and nothing else with it.
    @Test
    fun decisionDA115_startingADragFromTheTrayEmitsPickUpBeforeOnChanged() {
        val r = Rig(mini)
        val piece = r.session.pieces.first().piece
        r.session.beginDrag(piece, r.session.layout!!.cell(piece).centre, 0)
        assertTrue("fixture: the drag started", r.session.isDragging)
        assertEquals(listOf(PlayEvent.PICK_UP), r.events())
        r.assertEventsBeforeChanged("pick up")
    }

    // DA-115: lifting a locked piece off the board is also a pick-up.
    @Test
    fun decisionDA115_liftingAPieceOffTheBoardEmitsPickUp() {
        val r = Rig(mini)
        val sq = r.pose(PieceId.SQ)
        assertTrue("fixture: the square locked", r.lockAt(sq, 0) is DropOutcome.Locked)
        r.clear()
        r.session.beginDrag(PieceId.SQ, Vec2(200.0, 300.0), 2000)
        assertTrue("fixture: the drag started", r.session.isDragging)
        assertEquals(listOf(PlayEvent.PICK_UP), r.events())
    }

    // ---- LOCK / RETURN / SOLVE -------------------------------------------------------------------------------------------------

    // DA-115: a drop that locks is LOCK alone (the pick-up and turn came earlier), before onChanged.
    @Test
    fun decisionDA115_aLockingDropEmitsLock() {
        val r = Rig(mini)
        val pc = r.pose(r.session.pieces.first().piece)
        r.carryTo(pc, 0)
        r.clear()
        val out = r.session.release(500)
        assertTrue("fixture: it locked", out is DropOutcome.Locked)
        assertEquals(listOf(PlayEvent.LOCK), r.events())
        r.assertEventsBeforeChanged("lock")
        assertTrue("one piece does not solve this puzzle", r.session.state != PuzzleState.SOLVED)
    }

    // DA-115 + REQ-020: a drop over the board with no valid position goes home: RETURN, not LOCK, no failure event of any other kind.
    @Test
    fun decisionDA115_aMissedDropOverTheBoardEmitsReturn() {
        val r = Rig(mini)
        val piece = r.session.pieces.first().piece
        val lay = r.session.layout!!
        r.session.beginDrag(piece, lay.cell(piece).centre, 0)
        r.session.dragTo(Vec2(lay.boardRect.right - 2, lay.boardRect.top + 2 + r.session.drag!!.lift))
        r.session.onFrame(500)
        r.clear()
        val out = r.session.release(500)
        assertTrue("fixture: the drop was a miss that went home", out is DropOutcome.Home)
        assertEquals(listOf(PlayEvent.RETURN), r.events())
        r.assertEventsBeforeChanged("missed drop")
    }

    // DA-115 + REQ-020 ("dropped outside the board or on the tray"): a drop on the tray is RETURN too.
    @Test
    fun decisionDA115_aDropOnTheTrayEmitsReturn() {
        val r = Rig(mini)
        val piece = r.session.pieces.first().piece
        val lay = r.session.layout!!
        r.session.beginDrag(piece, lay.cell(piece).centre, 0)
        r.session.dragTo(Vec2(200.0, lay.areaHeight + 300.0))
        r.session.onFrame(500)
        r.clear()
        val out = r.session.release(500)
        assertTrue("fixture: a drop on the tray goes home", out is DropOutcome.Home)
        assertEquals(listOf(PlayEvent.RETURN), r.events())
    }

    // DA-115: the drop that completes the puzzle is LOCK then SOLVE, in that order and once, both before onChanged; the earlier drops
    // are LOCK alone; no later frame or glide expiry emits anything.
    @Test
    fun decisionDA115_theCompletingDropEmitsLockThenSolveOnce() {
        val r = Rig(mini)
        val pieces = r.session.pieces.map { it.piece }
        var t = 0L
        val all = ArrayList<PlayEvent>()
        for ((i, piece) in pieces.withIndex()) {
            r.carryTo(r.pose(piece), t)
            r.clear()
            val out = r.session.release(t + 500)
            assertTrue("fixture: $piece locked", out is DropOutcome.Locked)
            if (i < pieces.size - 1) {
                assertEquals("piece ${i + 1} of ${pieces.size}", listOf(PlayEvent.LOCK), r.events())
            } else {
                assertEquals("the completing drop", listOf(PlayEvent.LOCK, PlayEvent.SOLVE), r.events())
                r.assertEventsBeforeChanged("completing drop")
            }
            all += r.events()
            t += 1000
        }
        assertEquals(PuzzleState.SOLVED, r.session.state)
        assertEquals("exactly one SOLVE for the whole puzzle", 1, all.count { it == PlayEvent.SOLVE })
        r.clear()
        r.session.onFrame(t + 10_000)
        r.session.onFrame(t + 20_000)
        assertEquals("frames and glide expiry are silent", emptyList<PlayEvent>(), r.events())
    }

    // DA-115: the DEV aid solve sounds SOLVE alone (no LOCK, no PICK_UP), once, before onChanged.
    @Test
    fun decisionDA115_solveByAidEmitsSolveAlone() {
        val r = Rig(mini)
        val poses = mini.solution.map { PieceGeometry.poseOf(it.piece, it.polygon)!! }
        assertTrue("fixture: the aid accepted the stored solution", r.session.solveByAid(poses))
        assertEquals(PuzzleState.SOLVED, r.session.state)
        assertEquals(listOf(PlayEvent.SOLVE), r.events())
        r.assertEventsBeforeChanged("solve by aid")
    }

    // ---- the silent cases ------------------------------------------------------------------------------------------------------

    // DA-115 + REQ-033 rule "No sound signals failure": a board tap-turn that cannot lock only shakes: no event at all. (The square
    // turned by 45 degrees has no place in the first puzzle: the same fixture PlayAnimationScaffoldingTest uses.)
    @Test
    fun decisionDA115_aRefusedBoardTurnIsSilent() {
        val r = Rig(mini)
        val sq = r.pose(PieceId.SQ)
        assertTrue("fixture: the square locked", r.lockAt(sq, 0) is DropOutcome.Locked)
        r.clear()
        r.session.tapBoard(PieceId.SQ, 1000)
        assertNotNull("fixture: the turn was refused (the piece shakes)", r.session.shake)
        assertEquals("the pose is unchanged", sq, r.session.placed.first { it.piece == PieceId.SQ })
        assertEquals(emptyList<PlayEvent>(), r.events())
    }

    // DA-115: a board mirror that still locks is kept and uses the turn cue (the square's mirror image is itself).
    @Test
    fun decisionDA115_aSuccessfulBoardMirrorEmitsTurn() {
        val r = Rig(mini)
        assertTrue("fixture: the square locked", r.lockAt(r.pose(PieceId.SQ), 0) is DropOutcome.Locked)
        r.clear()
        r.session.flipBoard(PieceId.SQ, 1000)
        assertNull("fixture: the mirror was accepted", r.session.shake)
        assertTrue("fixture: the piece is mirrored now", r.session.placed.first { it.piece == PieceId.SQ }.mirrored)
        assertEquals(listOf(PlayEvent.TURN), r.events())
        r.assertEventsBeforeChanged("board mirror")
    }

    // DA-115 + REQ-033 rule: a board mirror with no room only shakes: silent. (shapes-square: the mirrored parallelogram has no lock, the
    // fixture DragFlipAcceptanceTest uses.)
    @Test
    fun decisionDA115_aRefusedBoardMirrorIsSilent() {
        val r = Rig(square)
        val pg = r.pose(PieceId.PG)
        assertTrue("fixture: the parallelogram locked", r.lockAt(pg, 0) is DropOutcome.Locked)
        r.clear()
        r.session.flipBoard(PieceId.PG, 1000)
        assertNotNull("fixture: the mirror was refused (the piece shakes)", r.session.shake)
        assertTrue("fixture: the piece is not mirrored", !r.session.placed.first { it.piece == PieceId.PG }.mirrored)
        assertEquals(emptyList<PlayEvent>(), r.events())
    }

    // DA-115: over every piece of every packaged puzzle, alone on the board at its solution pose, a board tap-turn emits TURN exactly
    // when it changed the pose and nothing when it was refused (it shakes). The library must offer at least one successful turn, else
    // the success half of the rule is untested and this fails loudly (a fixture that cannot distinguish the cases proves nothing).
    @Test
    fun decisionDA115_aBoardTapTurnEmitsTurnExactlyWhenItSucceeds() {
        var succeeded = 0
        var refused = 0
        for (p in library) for (sp in p.solution) {
            val r = Rig(p)
            val pose = r.pose(sp.piece)
            if (r.lockAt(pose, 0) !is DropOutcome.Locked) continue
            if (r.session.state == PuzzleState.SOLVED) continue // a one-piece puzzle is solved by the drop: no board turn to test
            val before = r.session.placed.first { it.piece == sp.piece }
            r.clear()
            r.session.tapBoard(sp.piece, 1000)
            val after = r.session.placed.first { it.piece == sp.piece }
            val changed = after != before
            val shaken = r.session.shake != null
            assertTrue("${p.id.value} ${sp.piece}: changed and shaken at once", !(changed && shaken))
            when {
                changed -> {
                    succeeded++
                    assertEquals("${p.id.value} ${sp.piece}: a successful board turn", listOf(PlayEvent.TURN), r.events())
                    r.assertEventsBeforeChanged("board turn ${p.id.value} ${sp.piece}")
                }
                shaken -> {
                    refused++
                    assertEquals("${p.id.value} ${sp.piece}: a refused board turn", emptyList<PlayEvent>(), r.events())
                }
                else -> assertEquals("${p.id.value} ${sp.piece}: a no-op", emptyList<PlayEvent>(), r.events())
            }
        }
        assertTrue("fixture: no packaged puzzle offers a successful board tap-turn (refused cases seen: $refused)", succeeded > 0)
        assertTrue("fixture: no packaged puzzle offers a refused board tap-turn (successful cases seen: $succeeded)", refused > 0)
    }

    // DA-115: interruptDrag (a rotation, a lifecycle stop, the overlay's input gate) is silent: no RETURN, no sound for a drag that
    // ends without a drop; the piece goes back silently (F5).
    @Test
    fun decisionDA115_interruptDragIsSilent() {
        val r = Rig(mini)
        val piece = r.session.pieces.first().piece
        r.session.beginDrag(piece, r.session.layout!!.cell(piece).centre, 0)
        assertEquals(listOf(PlayEvent.PICK_UP), r.events())
        r.clear()
        r.session.interruptDrag()
        assertTrue("fixture: the drag ended", !r.session.isDragging)
        assertEquals(emptyList<PlayEvent>(), r.events())
        r.session.interruptDrag()
        assertNull("nothing left to release", r.session.release(600))
        assertEquals(emptyList<PlayEvent>(), r.events())
    }

    // DA-115: restore (a saved board coming back) and toProgress (saving) are silent.
    @Test
    fun decisionDA115_restoreAndToProgressAreSilent() {
        val source = Rig(mini)
        val sq = source.pose(PieceId.SQ)
        assertTrue(source.lockAt(sq, 0) is DropOutcome.Locked)
        val saved = source.session.toProgress(PuzzleProgress.NEW)
        assertTrue("fixture: the save holds a piece on the board", saved.pieces.values.any { it is PieceSave.OnBoard })

        source.clear()
        source.session.toProgress(PuzzleProgress.NEW)
        assertEquals("toProgress is silent", emptyList<PlayEvent>(), source.events())

        val r = Rig(mini)
        r.session.restore(saved)
        assertTrue("fixture: the piece is back on the board", r.session.placed.any { it.piece == PieceId.SQ })
        assertEquals("restore is silent", emptyList<PlayEvent>(), r.events())
        r.session.restore(PuzzleProgress.NEW)
        assertEquals(emptyList<PlayEvent>(), r.events())
    }

    // DA-115 / seam: `onEvent` has a default, so a session nobody listens to works (the property is assigned after construction).
    @Test
    fun decisionDA115_aSessionWithNoListenerStillWorks() {
        val s = PlaySession(mini, { false })
        s.layout = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, rows, mini)
        val piece = s.pieces.first().piece
        s.tapTray(piece)
        s.beginDrag(piece, s.layout!!.cell(piece).centre, 0)
        s.interruptDrag()
    }
}
