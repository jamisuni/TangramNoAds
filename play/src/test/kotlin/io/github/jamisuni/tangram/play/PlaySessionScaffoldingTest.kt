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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// SCAFFOLDING (disposable, TASK-015a): not an acceptance test.
class PlaySessionScaffoldingTest {
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

    private val rows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val mini: Puzzle = PuzzleLibrary.packaged().puzzles.first()

    private fun setup(resolver: DropResolver = DropResolver(mini)): PlaySession {
        val s = PlaySession(mini, { false }, resolver)
        s.layout = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, rows, mini)
        return s
    }

    private fun solutionPose(piece: PieceId): PlacedPiece =
        PieceGeometry.poseOf(piece, mini.solution.first { it.piece == piece }.polygon)!!

    /** Finger (dp) at which a piece held at [pose] floats exactly on [pose]. */
    private fun fingerFor(s: PlaySession, pose: PlacedPiece): Vec2 {
        val lay = s.layout!!
        val c = PieceGeometry.centroidOffset(pose.piece.shape, pose.turn, pose.mirrored)
        val centre = lay.toDp(Vec2(pose.at.x.toDouble() + c.x, pose.at.y.toDouble() + c.y))
        return Vec2(centre.x, centre.y + s.drag!!.lift)
    }

    private fun dropOn(s: PlaySession, pose: PlacedPiece, t0: Long): DropOutcome? {
        val lay = s.layout!!
        s.beginDrag(pose.piece, lay.cell(pose.piece).centre, t0)
        s.setDragTurn(pose.turn)
        s.dragTo(fingerFor(s, pose))
        s.onFrame(t0 + 500)
        return s.release(t0 + 500)
    }

    @Test fun startsAllInTrayNew() {
        val s = setup()
        assertEquals(PuzzleState.NEW, s.state)
        assertEquals(mini.solution.map { it.piece }.toSet(), s.pieces.map { it.piece }.toSet())
        for (p in s.pieces) {
            assertEquals(Where.Tray, p.where)
            assertEquals(TrayRules.restingTurn(p.piece.shape), p.turn)
        }
        assertTrue(s.placed.isEmpty())
    }

    @Test fun trayDragStartsInProgressAndPieceIsDragged() {
        val s = setup()
        val p = s.pieces.first().piece
        s.beginDrag(p, s.layout!!.cell(p).centre, 0)
        assertEquals(PuzzleState.IN_PROGRESS, s.state)
        assertTrue(s.isDragging)
        assertEquals(Where.Dragged, s.pieces.first { it.piece == p }.where)
        assertEquals(1, s.pieces.count { it.where == Where.Dragged })
    }

    @Test fun floatsOnFingerAndGrowsToBoardScale() {
        val s = setup()
        val p = s.pieces.first().piece
        val lay = s.layout!!
        s.beginDrag(p, lay.cell(p).centre, 1000)
        s.dragTo(Vec2(100.0, 300.0))
        s.onFrame(1000)
        assertEquals(lay.trayScale, s.drag!!.frame!!.scale, 1e-9)
        s.onFrame(1060)
        val mid = s.drag!!.frame!!.scale
        assertTrue(mid > lay.trayScale && mid < lay.dpPerUnit)
        s.onFrame(1120)
        val f = s.drag!!.frame!!
        assertEquals(lay.dpPerUnit, f.scale, 0.0)
        assertEquals(Vec2(100.0, 300.0 - s.drag!!.lift), f.centre)
    }

    @Test fun releaseUsesLastDisplayedFrameNotLiveFinger() {
        val s = setup()
        val p = s.pieces.first().piece
        val target = solutionPose(p)
        s.beginDrag(p, s.layout!!.cell(p).centre, 0)
        s.setDragTurn(target.turn)
        s.dragTo(fingerFor(s, target))
        s.onFrame(500)
        val shown = s.drag!!.frame!!.preview
        assertNotNull(shown)
        s.dragTo(Vec2(5.0, 5.0)) // moved after the last frame
        val out = s.release(510)
        assertTrue(out is DropOutcome.Locked)
        assertEquals(shown, (out as DropOutcome.Locked).placed)
        assertEquals(Where.Board(shown!!.at), s.pieces.first { it.piece == p }.where)
    }

    @Test fun solvingEveryPieceGivesSolved() {
        val s = setup()
        var t = 0L
        for (p in s.pieces.map { it.piece }) {
            assertEquals(PuzzleState.IN_PROGRESS.takeIf { t > 0 } ?: PuzzleState.NEW, s.state)
            assertTrue(dropOn(s, solutionPose(p), t) is DropOutcome.Locked)
            t += 1000
        }
        assertEquals(PuzzleState.SOLVED, s.state)
        assertEquals(t - 500, s.solved!!.t0)
        assertEquals(mini.solution.size, s.placed.size)
    }

    @Test fun missOverBoardPulsesOnceAndTrayDropDoesNot() {
        val s = setup()
        val p = s.pieces.first().piece
        val lay = s.layout!!
        s.beginDrag(p, lay.cell(p).centre, 0)
        // far from any lock but over the board
        s.dragTo(Vec2(lay.boardRect.right - 2, lay.boardRect.top + 2 + s.drag!!.lift))
        s.onFrame(500)
        val out = s.release(500)
        assertTrue(out is DropOutcome.Home)
        assertNotNull(s.pulse)
        assertEquals(Where.Tray, s.pieces.first { it.piece == p }.where)

        val s2 = setup()
        s2.beginDrag(p, s2.layout!!.cell(p).centre, 0)
        s2.dragTo(Vec2(200.0, s2.layout!!.areaHeight + 300.0)) // dropped on the tray
        s2.onFrame(500)
        assertTrue(s2.release(500) is DropOutcome.Home)
        assertNull(s2.pulse)
    }

    @Test fun interruptFromTrayRestoresWholePoseWithoutResolver() {
        val r = Counting(mini)
        val s = setup(r)
        val p = s.pieces.first().piece
        val before = s.pieces.first { it.piece == p }
        s.beginDrag(p, s.layout!!.cell(p).centre, 0)
        s.setDragTurn(before.turn.let { io.github.jamisuni.tangram.kernel.model.Turn((it.steps + 3) % 8) })
        s.dragTo(fingerFor(s, solutionPose(p)))
        s.onFrame(500)
        val previewsAtInterrupt = r.previews
        s.interruptDrag()
        assertEquals(previewsAtInterrupt, r.previews)
        assertEquals(0, r.releases)
        assertEquals(before, s.pieces.first { it.piece == p })
        assertNull(s.pulse)
        assertTrue(s.glides.isEmpty())
        assertEquals(PuzzleState.IN_PROGRESS, s.state)
        s.interruptDrag() // idempotent
        assertNull(s.release(600))
    }

    @Test fun interruptFromBoardRestoresPoseTurnAndMirror() {
        val r = Counting(mini)
        val s = setup(r)
        val p = s.pieces.first().piece
        assertTrue(dropOn(s, solutionPose(p), 0) is DropOutcome.Locked)
        val onBoard = s.pieces.first { it.piece == p }
        s.beginDrag(p, Vec2(200.0, 300.0), 2000)
        s.setDragTurn(io.github.jamisuni.tangram.kernel.model.Turn((onBoard.turn.steps + 2) % 8))
        s.dragTo(Vec2(10.0, 600.0))
        s.onFrame(2500)
        val releases = r.releases
        s.interruptDrag()
        assertEquals(releases, r.releases)
        assertEquals(onBoard, s.pieces.first { it.piece == p })
        assertNull(s.pulse)
    }

    @Test fun everyPieceIsExactlyOneLocationAlways() {
        val s = setup()
        val p = s.pieces.first().piece
        s.beginDrag(p, s.layout!!.cell(p).centre, 0)
        assertEquals(1, s.pieces.count { it.where == Where.Dragged })
        assertEquals(p, s.drag!!.piece)
        s.interruptDrag()
        assertEquals(0, s.pieces.count { it.where == Where.Dragged })
    }

    @Test fun dragMotionEndpoints() {
        val pick = Vec2(0.0, 0.0)
        val f = Vec2(100.0, 200.0)
        assertEquals(pick, DragMotion.centre(f, pick, 30.0, 0))
        assertEquals(Vec2(100.0, 170.0), DragMotion.centre(f, pick, 30.0, 120))
        assertEquals(Vec2(100.0, 170.0), DragMotion.centre(f, pick, 30.0, 5000))
        assertEquals(10.0, DragMotion.scale(10.0, 25.0, 0), 0.0)
        assertEquals(25.0, DragMotion.scale(10.0, 25.0, 120), 0.0)
    }
}
