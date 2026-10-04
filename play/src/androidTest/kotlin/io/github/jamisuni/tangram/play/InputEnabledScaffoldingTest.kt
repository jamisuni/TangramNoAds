package io.github.jamisuni.tangram.play

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PieceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

// SCAFFOLDING (TASK-054, disposable; decision DA-118): inputEnabled = false cancels a press silently and ignores downs.
class InputEnabledScaffoldingTest {

    @get:Rule
    val rule = createComposeRule()

    private val rows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val puzzle = PuzzleLibrary.packaged().puzzles.first { it.id.value == "shapes-mini-1" }
    private var enabled by mutableStateOf(true)

    private fun cellPoint(piece: PieceId): Offset {
        val c = PlayLayout.compute(360.0, 640.0, 780.0, LayoutClass.PHONE, rows, puzzle).cell(piece)
        val d = rule.density.density
        return Offset(((c.left + 8.0) * d).toFloat(), ((c.bottom - 8.0) * d).toFloat())
    }

    private fun up(dpY: Float) = Offset(0f, dpY * rule.density.density)

    @Test
    fun disablingMidDragCancelsSilentlyAndReEnablingWorks() {
        val events = ArrayList<PlayEvent>()
        val session = PlaySession(puzzle)
        session.onEvent = { events += it }
        rule.mainClock.autoAdvance = false
        rule.setContent {
            Box(Modifier.size(360.dp, 640.dp)) {
                PlayArea(session, LayoutClass.PHONE, rows, 780.dp, Modifier.fillMaxSize(), inputEnabled = enabled)
            }
        }
        rule.mainClock.advanceTimeBy(100)
        val before = rule.runOnUiThread { session.pieces.first { it.piece == PieceId.ST1 }.where }
        val p = cellPoint(PieceId.ST1)

        // press and start a drag, then disable
        rule.onNodeWithTag("play-area").performTouchInput { down(p) }
        rule.mainClock.advanceTimeBy(16)
        rule.onNodeWithTag("play-area").performTouchInput { moveTo(p + up(-60f)) }
        rule.mainClock.advanceTimeBy(200)
        assertNotNull("drag started", rule.runOnUiThread { session.drag })
        events.clear()
        rule.runOnUiThread { enabled = false }
        rule.mainClock.advanceTimeBy(100)
        assertNull("no drag", rule.runOnUiThread { session.drag })
        assertEquals("no event", emptyList<PlayEvent>(), events)
        assertEquals("piece back", before, rule.runOnUiThread { session.pieces.first { it.piece == PieceId.ST1 }.where })
        rule.onNodeWithTag("play-area").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(100)

        // new downs are ignored while disabled
        rule.onNodeWithTag("play-area").performTouchInput { down(p) }
        rule.mainClock.advanceTimeBy(16)
        rule.onNodeWithTag("play-area").performTouchInput { moveTo(p + up(-60f)) }
        rule.mainClock.advanceTimeBy(200)
        assertNull("ignored while disabled", rule.runOnUiThread { session.drag })
        assertEquals(emptyList<PlayEvent>(), events)
        rule.onNodeWithTag("play-area").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(100)

        // re-enabled: a drag works
        rule.runOnUiThread { enabled = true }
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithTag("play-area").performTouchInput { down(p) }
        rule.mainClock.advanceTimeBy(16)
        rule.onNodeWithTag("play-area").performTouchInput { moveTo(p + up(-60f)) }
        rule.mainClock.advanceTimeBy(200)
        assertNotNull("drag works again", rule.runOnUiThread { session.drag })
        assertEquals(listOf(PlayEvent.PICK_UP), events)
    }
}
