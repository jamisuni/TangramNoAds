package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertTrue
import org.junit.Test

// SCAFFOLDING (disposable, TASK-018a): every mutating intent and onFrame bumps the snapshot version the drawing reads.
class SessionObservationScaffoldingTest {
    private val rows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val puzzle: Puzzle = PuzzleLibrary.packaged().puzzles.first()
    private val piece: PieceId = puzzle.solution.first().piece

    private fun session(): PlaySession = PlaySession(puzzle, { false }).also {
        it.layout = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, rows, puzzle)
    }

    private fun PlaySession.bumps(what: String, op: () -> Unit) {
        val before = version
        op()
        assertTrue("$what must bump the version ($before -> $version)", version > before)
    }

    private fun solutionFinger(s: PlaySession): Vec2 {
        val lay = s.layout!!
        val pose = PieceGeometry.poseOf(piece, puzzle.solution.first().polygon)!!
        val c = PieceGeometry.centroidOffset(piece.shape, pose.turn, pose.mirrored)
        val centre = lay.toDp(Vec2(pose.at.x.toDouble() + c.x, pose.at.y.toDouble() + c.y))
        return Vec2(centre.x, centre.y + s.drag!!.lift)
    }

    @Test
    fun trayIntentsBump() {
        val s = session()
        s.bumps("tapTray") { s.tapTray(piece) }
        s.bumps("flipTray") { s.flipTray(piece) }
    }

    @Test
    fun dragIntentsAndFramesBump() {
        val s = session()
        val cell = s.layout!!.cell(piece).centre
        s.bumps("beginDrag") { s.beginDrag(piece, cell, 1000L) }
        s.bumps("dragTo") { s.dragTo(Vec2(cell.x + 5.0, cell.y - 40.0)) }
        s.bumps("setDragTurn") { s.setDragTurn(Turn(2)) }
        s.bumps("onFrame while dragging") { s.onFrame(1016L) }
        s.bumps("interruptDrag") { s.interruptDrag() }
    }

    @Test
    fun releaseGlideExpiryAndBoardIntentsBump() {
        val s = session()
        val cell = s.layout!!.cell(piece).centre
        val pose = PieceGeometry.poseOf(piece, puzzle.solution.first().polygon)!!
        s.beginDrag(piece, cell, 1000L)
        s.dragTo(solutionFinger(s))
        s.onFrame(1200L)
        s.bumps("release") { s.release(1200L) }
        s.bumps("onFrame expiring the glide") { s.onFrame(1400L) }
        // a mini puzzle may be solved by this one piece; the board intents are no-ops after the solve
        if (s.state != io.github.jamisuni.tangram.kernel.model.PuzzleState.SOLVED && s.placed.any { it.piece == piece }) {
            s.bumps("tapBoard") { s.tapBoard(piece, 2000L) }
            s.bumps("flipBoard") { s.flipBoard(piece, 2100L) }
        }
        assertTrue(pose.piece == piece)
    }
}
