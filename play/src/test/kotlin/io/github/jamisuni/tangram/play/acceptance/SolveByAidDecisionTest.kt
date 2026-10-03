package io.github.jamisuni.tangram.play.acceptance

import io.github.jamisuni.tangram.kernel.lock.LockSearch
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// ACCEPTANCE TEST (TASK-T5, independent author): decision DA-73, `PlaySession.solveByAid(poses): Boolean` and the `onSolved`
// event. Written from design WO-005 section 2b/2c and the frozen seam rows only ("false = nothing changed, no event, no
// onChanged"; "true = pieces on the board at the poses, state == SOLVED, a pending solve, onSolved(true) once, then onChanged()
// once"; "onSolved(false) fires exactly once per drop that solves"). No acceptance tokens: these pin an AI decision (the REQ's own
// criterion is held out).
class SolveByAidDecisionTest {

    // decision DA-73: valid poses solve every packaged puzzle: true, SOLVED, the pieces are at the poses, and the events are
    // onSolved(true) once and then onChanged once (design 2b step 5).
    @Test
    fun decisionDA73_validPosesSolveEveryPackagedPuzzle() {
        for (p in PUZZLES) {
            val ev = AidEvents()
            val s = aidSession(p, ev)
            val poses = aidPoses(p)
            assertEquals("${p.id.value}: a fresh puzzle is New", PuzzleState.NEW, s.state)
            assertTrue("${p.id.value}: the stored solution was refused", s.solveByAid(poses))
            assertEquals("${p.id.value}: events", listOf("solved:true", "changed"), ev.log)
            assertEquals("${p.id.value}: state", PuzzleState.SOLVED, s.state)
            assertEquals("${p.id.value}: the pieces are at the poses", poseKeys(poses), poseKeys(s.placed))
            assertEquals("${p.id.value}: every piece is on the board", p.solution.size, s.placed.size)
            assertTrue("${p.id.value}: the solve is pending until the next frame", s.solvePending)
            assertNull("${p.id.value}: no solve time before the next frame", s.solved)
        }
    }

    // decision DA-73: a refused call changes nothing - no state, no piece, no event, no onChanged - for each way the set can be wrong.
    @Test
    fun decisionDA73_aRefusedCallChangesNothing() {
        val p = SEVEN_PIECE.first()
        val poses = aidPoses(p)
        val cases = linkedMapOf(
            "an empty list" to emptyList(),
            "a missing piece" to poses.drop(1),
            "one piece twice and one missing" to poses.dropLast(1) + poses.first(),
            "an extra copy of a piece" to poses + poses.first(),
            "one piece posed where another puzzle's solution puts it" to withOnePieceMoved(p),
            "another puzzle's complete solution" to aidPoses(otherSilhouette(p)),
        )
        for ((label, bad) in cases) {
            val ev = AidEvents()
            val s = aidSession(p, ev)
            val before = aidSnapshot(s)
            assertFalse("$label was accepted", s.solveByAid(bad))
            assertEquals("$label changed the session", before, aidSnapshot(s))
            assertEquals("$label produced events", emptyList<String>(), ev.log)
        }
    }

    // decision DA-73: the same on a puzzle in progress - refused sets leave the player's pieces where they are.
    @Test
    fun decisionDA73_aRefusedCallLeavesAPlayersPiecesAlone() {
        val p = SEVEN_PIECE.first()
        val ev = AidEvents()
        val d = AidDriver(p, ev)
        val targets = aidPoses(p)
        d.place(buildOrder(p, targets, d.layout.dpPerUnit).first())
        assertEquals("fixture: one piece placed by the player", 1, d.session.placed.size)
        assertEquals("fixture: In progress", PuzzleState.IN_PROGRESS, d.session.state)
        val before = aidSnapshot(d.session)
        val log = ev.log.toList()
        assertFalse(d.session.solveByAid(targets.drop(1)))
        assertFalse(d.session.solveByAid(withOnePieceMoved(p)))
        assertEquals(before, aidSnapshot(d.session))
        assertEquals("a refused call fired an event", log, ev.log)
    }

    // decision DA-73: once SOLVED, a second call is refused and fires nothing (the state is not SOLVED is a condition).
    @Test
    fun decisionDA73_aSecondCallOnASolvedPuzzleIsRefused() {
        val p = SEVEN_PIECE.first()
        val ev = AidEvents()
        val s = aidSession(p, ev)
        assertTrue(s.solveByAid(aidPoses(p)))
        s.onFrame(1000)
        val before = aidSnapshot(s)
        val log = ev.log.toList()
        assertFalse(s.solveByAid(aidPoses(p)))
        assertEquals(before, aidSnapshot(s))
        assertEquals(log, ev.log)
        assertEquals(1, ev.count("solved:true"))
    }

    // decision DA-73: a puzzle the player already solved is not "solved by the aid": refused, no solved:true event.
    @Test
    fun decisionDA73_aPlayerSolvedPuzzleRefusesTheAidAndTheDropEventIsFalse() {
        val p = PUZZLES.first { it.solution.size == 3 }
        val ev = AidEvents()
        val d = AidDriver(p, ev)
        d.placeAll(aidPoses(p))
        assertEquals("fixture: the player solved ${p.id.value}", PuzzleState.SOLVED, d.session.state)
        assertEquals("a drop that solves fires onSolved(false) exactly once", 1, ev.count("solved:false"))
        assertEquals(0, ev.count("solved:true"))
        val before = aidSnapshot(d.session)
        assertFalse(d.session.solveByAid(aidPoses(p)))
        assertEquals(before, aidSnapshot(d.session))
        assertEquals(0, ev.count("solved:true"))
        assertEquals(1, ev.count("solved:false"))
    }

    // decision DA-73: "indistinguishable from a player placing those pieces: any exact cover solves (REQ-022)". Two pieces of the
    // same shape swapped between their places are a different exact cover and are accepted.
    @Test
    fun decisionDA73_anyExactCoverSolves() {
        var tried = 0
        for (p in SEVEN_PIECE) {
            val poses = aidPoses(p)
            for ((a, b) in listOf(PieceId.LT1 to PieceId.LT2, PieceId.ST1 to PieceId.ST2)) {
                val sa = p.solution.first { it.piece == a }
                val sb = p.solution.first { it.piece == b }
                val aOnB = io.github.jamisuni.tangram.kernel.geometry.PieceGeometry.poseOf(a, sb.polygon) ?: continue
                val bOnA = io.github.jamisuni.tangram.kernel.geometry.PieceGeometry.poseOf(b, sa.polygon) ?: continue
                val swapped = poses.map { when (it.piece) { a -> aOnB; b -> bOnA; else -> it } }
                val ev = AidEvents()
                val s = aidSession(p, ev)
                assertTrue("${p.id.value}: swapping $a and $b is an exact cover but was refused", s.solveByAid(swapped))
                assertEquals(PuzzleState.SOLVED, s.state)
                assertEquals(poseKeys(swapped), poseKeys(s.placed))
                tried++
            }
        }
        assertTrue("fixture: no swappable pair was found", tried > 0)
    }

    // decision DA-73 (design 2b step 1-2): the aid interrupts a drag first and replaces wherever the player had put the pieces.
    @Test
    fun decisionDA73_theAidInterruptsADragAndReplacesThePlayersPieces() {
        val p = SEVEN_PIECE.first()
        val ev = AidEvents()
        val d = AidDriver(p, ev)
        val targets = aidPoses(p)
        d.place(buildOrder(p, targets, d.layout.dpPerUnit).first())
        val another = targets.first { t -> d.session.placed.none { it.piece == t.piece } }
        d.dragFromTray(another.piece)
        assertTrue("fixture: a drag is running", d.session.isDragging)
        assertTrue(d.session.solveByAid(targets))
        assertFalse("the drag was not interrupted", d.session.isDragging)
        assertNull(d.session.drag)
        assertEquals(PuzzleState.SOLVED, d.session.state)
        assertEquals(poseKeys(targets), poseKeys(d.session.placed))
    }

    // decision DA-73: the player's piece that sits at another valid place is moved to its stored pose (the aid ignores where the
    // player put it): LT1 placed on LT2's place by the player, then the aid.
    @Test
    fun decisionDA73_aPieceThePlayerPlacedElsewhereEndsAtItsStoredPose() {
        for (p in SEVEN_PIECE) {
            val sil = silhouetteOf(p)
            val poses = aidPoses(p)
            val lt2 = p.solution.first { it.piece == PieceId.LT2 }
            val lt1OnLt2 = io.github.jamisuni.tangram.kernel.geometry.PieceGeometry.poseOf(PieceId.LT1, lt2.polygon) ?: continue
            val ev = AidEvents()
            val d = AidDriver(p, ev)
            val r = LockSearch.lockDistance(d.layout.dpPerUnit)
            if (LockSearch.find(sil, emptyList(), lt1OnLt2.piece, lt1OnLt2.turn, lt1OnLt2.mirrored, v(lt1OnLt2.at), r)?.at != lt1OnLt2.at) continue
            d.place(lt1OnLt2)
            if (d.session.placed.none { it.piece == PieceId.LT1 && it.at == lt1OnLt2.at }) continue
            assertTrue(d.session.solveByAid(poses))
            assertEquals(poseKeys(poses), poseKeys(d.session.placed))
            return
        }
        error("fixture: no puzzle lets LT1 lock on LT2's place first")
    }
}
