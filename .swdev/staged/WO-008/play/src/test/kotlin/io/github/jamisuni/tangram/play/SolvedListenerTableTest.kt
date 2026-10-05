package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// decision DA-138: DECISION TEST (WO-008 T8c v-p), no requirement token. Design 5.2 and the seam row for `play`:
// `PlaySession.solvedListener: (byAid: Boolean) -> Unit` is a public settable property, default `{}`, "called at the same two sites as the
// constructor's `onSolved` (immediately after it, before `settled()`)", `false` for the completing drop, `true` for the aid solve,
// "never by `restore`". `settled()` is the event that fires the constructor's `onChanged` (DA-49), so the order in a solve is
// onSolved, then solvedListener, then onChanged. The table below is one row per way a session can end up solved or not.
class SolvedListenerTableTest {
    private val rows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val puzzle: Puzzle = PuzzleLibrary.packaged().puzzles.first()
    private val poses: List<PlacedPiece> = puzzle.solution.map { PieceGeometry.poseOf(it.piece, it.polygon)!! }

    private class Rig(val session: PlaySession, val log: MutableList<String>)

    /** A session whose three observers all write to one ordered log; [listener] false leaves `solvedListener` at its default. */
    private fun rig(listener: Boolean = true): Rig {
        val log = ArrayList<String>()
        val s = PlaySession(puzzle, { false }, onChanged = { log += "changed" }, onSolved = { log += "onSolved($it)" })
        if (listener) s.solvedListener = { log += "listener($it)" }
        s.layout = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, rows, puzzle)
        return Rig(s, log)
    }

    /** Drops pieces by touch at their solution poses; [count] pieces, the last of them completing the puzzle when count = all. */
    private fun dropPieces(s: PlaySession, from: Int, count: Int, startT: Long): Long {
        var t = startT
        val order = s.pieces.map { it.piece }
        for (p in order.drop(from).take(count)) {
            val pose = poses.first { it.piece == p }
            val lay = s.layout!!
            s.beginDrag(p, lay.cell(p).centre, t)
            s.setDragTurn(pose.turn)
            val c = PieceGeometry.centroidOffset(p.shape, pose.turn, pose.mirrored)
            val centre = lay.toDp(Vec2(pose.at.x.toDouble() + c.x, pose.at.y.toDouble() + c.y))
            s.dragTo(Vec2(centre.x, centre.y + s.drag!!.lift))
            s.onFrame(t + 500)
            s.release(t + 500)
            t += 1000
        }
        return t
    }

    private fun solveByDrops(s: PlaySession): Long {
        val t = dropPieces(s, 0, s.pieces.size, 0L)
        s.onFrame(t + 100)
        return t
    }

    private fun solvedEntries(log: List<String>) = log.filter { it.startsWith("onSolved") || it.startsWith("listener") }

    @Test fun aCompletingDropCallsTheListenerOnceWithByAidFalseRightAfterOnSolvedAndBeforeSettled() {
        val r = rig()
        solveByDrops(r.session)
        assertEquals(PuzzleState.SOLVED, r.session.state)
        assertEquals(listOf("onSolved(false)", "listener(false)"), solvedEntries(r.log))
        val iSolved = r.log.indexOf("onSolved(false)")
        val iListener = r.log.indexOf("listener(false)")
        assertEquals("immediately after onSolved", iSolved + 1, iListener)
        assertTrue("before the settled event of the completing drop: ${r.log.takeLast(4)}", r.log.drop(iListener + 1).firstOrNull() == "changed")
    }

    @Test fun anAidSolveCallsTheListenerOnceWithByAidTrueRightAfterOnSolvedAndBeforeSettled() {
        val r = rig()
        assertTrue(r.session.solveByAid(poses))
        r.session.onFrame(1000L)
        assertEquals(PuzzleState.SOLVED, r.session.state)
        assertEquals(listOf("onSolved(true)", "listener(true)"), solvedEntries(r.log))
        val iListener = r.log.indexOf("listener(true)")
        assertEquals("immediately after onSolved", r.log.indexOf("onSolved(true)") + 1, iListener)
        assertTrue("before the settled event: ${r.log}", r.log.drop(iListener + 1).firstOrNull() == "changed")
    }

    @Test fun aDropThatDoesNotCompleteThePuzzleCallsNothing() {
        val r = rig()
        dropPieces(r.session, 0, r.session.pieces.size - 1, 0L)
        assertEquals(PuzzleState.IN_PROGRESS, r.session.state)
        assertTrue("a listener call before the puzzle is solved: ${r.log}", solvedEntries(r.log).isEmpty())
    }

    @Test fun furtherFramesAndASecondAidSolveAddNoCalls() {
        val r = rig()
        val t = solveByDrops(r.session)
        r.session.onFrame(t + 200)
        r.session.onFrame(t + 300)
        r.session.solveByAid(poses) // a Solved session: refused
        r.session.onFrame(t + 400)
        assertEquals("exactly one call per solve", listOf("onSolved(false)", "listener(false)"), solvedEntries(r.log))
    }

    @Test fun restoringASolvedRecordNeverCallsTheListener() {
        val r = rig()
        r.session.restore(PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 0, null))
        r.session.onFrame(100L)
        assertEquals(PuzzleState.SOLVED, r.session.state)
        assertTrue("restore fired a solve callback: ${r.log}", solvedEntries(r.log).isEmpty())
    }

    @Test fun theDefaultListenerIsANoOpAndTheConstructorCallbackStillFires() {
        val r = rig(listener = false)
        solveByDrops(r.session)
        assertEquals(PuzzleState.SOLVED, r.session.state)
        assertEquals(listOf("onSolved(false)"), solvedEntries(r.log))
    }

    @Test fun aListenerSetAfterConstructionCanBeReplacedAndTheLatestOneIsCalled() {
        val r = rig()
        val other = ArrayList<Boolean>()
        r.session.solvedListener = { other += it }
        assertTrue(r.session.solveByAid(poses))
        r.session.onFrame(1000L)
        assertEquals(listOf(true), other)
        assertTrue("the replaced listener was still called: ${r.log}", r.log.none { it.startsWith("listener") })
    }
}
