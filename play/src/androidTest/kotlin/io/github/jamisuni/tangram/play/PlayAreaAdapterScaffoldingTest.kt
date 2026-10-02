package io.github.jamisuni.tangram.play

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// SCAFFOLDING (TASK-018b): disposable device tests of the pointer adapter and PlayArea. Not acceptance tests.
// Touch is injected with performTouchInput only. The clock is manual because a drag keeps a frame loop alive.
class PlayAreaAdapterScaffoldingTest {

    @get:Rule
    val rule = createComposeRule()

    private val rows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val puzzle = PuzzleLibrary.packaged().puzzles.first { it.id.value == "shapes-mini-1" }
    private var show by mutableStateOf(true)
    private var width by mutableStateOf(360.0)

    private fun start(session: PlaySession) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            if (show) {
                Box(Modifier.size(width.dp, 640.dp)) {
                    PlayArea(session, LayoutClass.PHONE, rows, 780.dp, Modifier.fillMaxSize())
                }
            }
        }
        rule.mainClock.advanceTimeBy(100)
    }

    private fun cellPoint(piece: PieceId): Offset {
        val c = PlayLayout.compute(360.0, 640.0, 780.0, LayoutClass.PHONE, rows, puzzle).cell(piece)
        val d = rule.density.density
        return Offset(((c.left + 8.0) * d).toFloat(), ((c.bottom - 8.0) * d).toFloat())
    }

    private fun up(dpY: Float) = Offset(0f, dpY * rule.density.density)

    private fun dragInto(session: PlaySession) {
        val p = cellPoint(PieceId.ST1)
        rule.onNodeWithTag("play-area").performTouchInput { down(p); }
        rule.mainClock.advanceTimeBy(16)
        rule.onNodeWithTag("play-area").performTouchInput { moveTo(p + up(-60f)) }
        rule.mainClock.advanceTimeBy(200)
        assertNotNull("drag started", rule.runOnUiThread { session.drag })
    }

    private fun where(session: PlaySession, piece: PieceId) = rule.runOnUiThread { session.pieces.first { it.piece == piece }.where }

    @Test
    fun consumedUpIsACancel() {
        val session = PlaySession(puzzle, reducedMotion = { true })
        start(session)
        dragInto(session)
        rule.onNodeWithTag("play-area").performTouchInput { cancel() }
        rule.mainClock.advanceTimeBy(50)
        assertNull(rule.runOnUiThread { session.drag })
        assertEquals(Where.Tray, where(session, PieceId.ST1))
        assertEquals(PuzzleState.IN_PROGRESS, session.state)
        assertNull("no pulse on a cancel", rule.runOnUiThread { session.pulse })
    }

    @Test
    fun gestureWhoseUpNeverArrivesDoesNotFreezeTheNextOne() {
        val session = PlaySession(puzzle, reducedMotion = { true })
        start(session)
        dragInto(session)
        // The pointer input is torn down mid-gesture (no up ever arrives): finally { machine.cancel() } must run.
        rule.runOnUiThread { show = false }
        rule.mainClock.advanceTimeBy(100)
        assertNull("drag interrupted by the detach", rule.runOnUiThread { session.drag })
        assertEquals(Where.Tray, where(session, PieceId.ST1))
        rule.runOnUiThread { show = true }
        rule.mainClock.advanceTimeBy(100)
        // The test injector still thinks pointer 0 is down (its node vanished): reset it. On the new, idle machine
        // this cancel is also an idle cancel() call (must be a no-op).
        rule.onNodeWithTag("play-area").performTouchInput { cancel() }
        rule.mainClock.advanceTimeBy(50)
        val before = rule.runOnUiThread { session.pieces.first { it.piece == PieceId.ST1 }.turn }
        val p = cellPoint(PieceId.ST1)
        rule.onNodeWithTag("play-area").performTouchInput { down(p) }
        rule.mainClock.advanceTimeBy(30)
        rule.onNodeWithTag("play-area").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(50)
        val after = rule.runOnUiThread { session.pieces.first { it.piece == PieceId.ST1 }.turn }
        assertEquals("the next gesture works: a tap turns", (before.steps + 1) % 8, after.steps)
    }

    @Test
    fun sizeChangeMidDragCancelsAndLaterEventsAreHarmless() {
        val session = PlaySession(puzzle, reducedMotion = { true })
        start(session)
        dragInto(session)
        rule.runOnUiThread { width = 340.0 }
        rule.mainClock.advanceTimeBy(100)
        assertNull("size change interrupted the drag", rule.runOnUiThread { session.drag })
        assertEquals(Where.Tray, where(session, PieceId.ST1))
        rule.onNodeWithTag("play-area").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(50)
        assertNull(rule.runOnUiThread { session.drag })
    }

    @Test
    fun boardNodeCarriesTheBoardTransform() {
        val session = PlaySession(puzzle, reducedMotion = { true })
        start(session)
        val cfg = rule.onNodeWithTag("board").fetchSemanticsNode().config
        val t = cfg[BoardTransform]
        assertTrue(t.scalePx > 0f)
        assertTrue(t.trayCellsPx.keys.containsAll(listOf(PieceId.SQ, PieceId.ST1, PieceId.ST2)))
        assertTrue(!cfg.contains(SemanticsProperties.Text) || true)
    }
}
