package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Scaffolding tests for [PieceDrawing.scale].
 * These are not acceptance tests; they validate the pure logic of the scale function.
 */
class PieceDrawingScaffoldingTest {

    private val puzzles by lazy { PuzzleLibrary.packaged().puzzles }
    private fun puzzle(id: String) = puzzles.first { it.id.value == id }
    private val phoneRows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )

    @Test
    fun trayPieceIsDrawnAtTrayScale() {
        val puzzle = puzzle("shapes-mini-1")
        val layout = PlayLayout.compute(360.0, 780.0, 780.0, LayoutClass.PHONE, phoneRows, puzzle)
        val state = PieceState(PieceId.ST1, Turn(0), false, Where.Tray)

        val scale = PieceDrawing.scale(state, layout)

        assertEquals("tray piece drawn at tray scale", layout.trayScale, scale, 1e-6)
    }

    @Test
    fun boardPieceIsDrawnAtBoardScale() {
        val puzzle = puzzle("shapes-mini-1")
        val layout = PlayLayout.compute(360.0, 780.0, 780.0, LayoutClass.PHONE, phoneRows, puzzle)
        val boardAt = ExactPoint(Q2.of(0), Q2.of(0))
        val state = PieceState(PieceId.ST1, Turn(0), false, Where.Board(boardAt))

        val scale = PieceDrawing.scale(state, layout)

        assertEquals("board piece drawn at board scale (dpPerUnit)", layout.dpPerUnit, scale, 1e-6)
    }

    @Test
    fun draggedPieceThrowsWithClearMessage() {
        val puzzle = puzzle("shapes-mini-1")
        val layout = PlayLayout.compute(360.0, 780.0, 780.0, LayoutClass.PHONE, phoneRows, puzzle)
        val state = PieceState(PieceId.ST1, Turn(0), false, Where.Dragged)

        val ex = try {
            PieceDrawing.scale(state, layout)
            error("Should have thrown")
        } catch (e: IllegalArgumentException) {
            e
        }

        assertTrue("error message must mention dragged", ex.message?.contains("dragged") == true)
        assertTrue("error message must mention DragFrame.scale", ex.message?.contains("DragFrame.scale") == true)
    }

    @Test
    fun totalVariantCoversAllThreeWhereCasesWithoutThrowing() {
        val puzzle = puzzle("shapes-mini-1")
        val layout = PlayLayout.compute(360.0, 780.0, 780.0, LayoutClass.PHONE, phoneRows, puzzle)
        val at = ExactPoint(Q2.of(0), Q2.of(0))
        assertEquals(layout.trayScale, PieceDrawing.scale(PieceState(PieceId.ST1, Turn(0), false, Where.Tray), layout, null), 0.0)
        assertEquals(layout.dpPerUnit, PieceDrawing.scale(PieceState(PieceId.ST1, Turn(0), false, Where.Board(at)), layout, null), 0.0)
        val dragged = PieceState(PieceId.ST1, Turn(0), false, Where.Dragged)
        // no drag known: falls back, no throw
        assertEquals(layout.dpPerUnit, PieceDrawing.scale(dragged, layout, null), 0.0)
        // a live drag: pick-up scale before the first frame, frame scale after
        val s = PlaySession(puzzle)
        s.layout = layout
        s.beginDrag(PieceId.ST1, layout.cell(PieceId.ST1).centre, 0)
        assertEquals(layout.trayScale, PieceDrawing.scale(dragged, layout, s.drag), 1e-9)
        s.onFrame(60)
        assertEquals(s.drag!!.frame!!.scale, PieceDrawing.scale(dragged, layout, s.drag), 0.0)
    }
}
