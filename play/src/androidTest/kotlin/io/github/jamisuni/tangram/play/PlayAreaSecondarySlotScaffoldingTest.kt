package io.github.jamisuni.tangram.play

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// SCAFFOLDING (TASK-031a, disposable): decisions DA-74 / DA-75 on a device: board parity with a secondary slot,
// and the overlay slot (not composed when solved, takes no touches).
class PlayAreaSecondarySlotScaffoldingTest {

    @get:Rule
    val rule = createComposeRule()

    private val rows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val all = PuzzleLibrary.packaged().puzzles

    @Test
    fun boardTransformIsIdenticalWithAndWithoutTheSecondarySlot() {
        val ids = listOf("shapes-mini-1", "animals-cat", "shapes-warmup-2", "shapes-square")
        val picks = ids.mapNotNull { id -> all.firstOrNull { it.id.value == id } }.ifEmpty { all.take(4) }
        var session by mutableStateOf(PlaySession(picks.first(), reducedMotion = { true }))
        var withSecondary by mutableStateOf(false)
        rule.setContent {
            Box(Modifier.size(360.dp, 640.dp)) {
                PlayArea(
                    session, LayoutClass.PHONE, rows, 780.dp, Modifier.fillMaxSize(),
                    cornerControl = { Box(Modifier.size(118.dp, 48.dp).semantics { testTag = "test-corner" }) },
                    secondaryCornerControl = if (withSecondary) {
                        { Box(Modifier.size(56.dp, 40.dp).semantics { testTag = "test-secondary" }) }
                    } else null,
                )
            }
        }
        for (p in picks) {
            rule.runOnUiThread { session = PlaySession(p, reducedMotion = { true }); withSecondary = false }
            rule.waitForIdle()
            val without = rule.onNodeWithTag("board").fetchSemanticsNode().config[BoardTransform]
            rule.runOnUiThread { withSecondary = true }
            rule.waitForIdle()
            val with = rule.onNodeWithTag("board").fetchSemanticsNode().config[BoardTransform]
            assertEquals(p.id.value, without, with)
            val b = rule.onNodeWithTag("test-secondary").fetchSemanticsNode().boundsInRoot
            val (layout, primary) = PlayLayout.computeWithCorner(360.0, 640.0, 780.0, LayoutClass.PHONE, rows, p, 118.0, 48.0)
            val expected = PlayLayout.placeSecondary(layout, primary, 56.0, 40.0)
            val d = rule.density.density
            if (expected != null) {
                rule.onNodeWithTag("test-secondary").assertIsDisplayed()
                assertEquals(p.id.value, expected.left * d, b.left.toDouble(), 1.5)
                assertEquals(p.id.value, expected.top * d, b.top.toDouble(), 1.5)
            }
        }
    }

    @Test
    fun overlayIsNotComposedWhenSolvedAndTakesNoTouches() {
        val p = all.first { it.id.value == "shapes-mini-1" }
        val session = PlaySession(p, reducedMotion = { true })
        var composed = 0
        var space: BoardSpace? = null
        rule.setContent {
            Box(Modifier.size(360.dp, 640.dp)) {
                PlayArea(
                    session, LayoutClass.PHONE, rows, 780.dp, Modifier.fillMaxSize(),
                    boardOverlay = { s ->
                        composed++
                        space = s
                        Box(Modifier.fillMaxSize().semantics { testTag = "test-overlay" })
                    },
                )
            }
        }
        rule.waitForIdle()
        rule.mainClock.autoAdvance = false // manual frame clock, as in DeviceSeams
        rule.onNodeWithTag("test-overlay").assertIsDisplayed()
        val layout = PlayLayout.compute(360.0, 640.0, 780.0, LayoutClass.PHONE, rows, p)
        val s = space ?: error("overlay was not given a BoardSpace")
        val u = Vec2(1.0, 2.0)
        assertEquals(layout.toDp(u).x, s.toDp(u).x, 1e-9)
        assertEquals(layout.toDp(u).y, s.toDp(u).y, 1e-9)
        // a drag started under the full-area overlay still reaches the gesture machine
        val d = rule.density.density
        val cell = layout.cell(PieceId.SQ).centre
        rule.onNodeWithTag("play-area").performTouchInput { down(Offset((cell.x * d).toFloat(), (cell.y * d).toFloat())) }
        rule.mainClock.advanceTimeBy(16)
        rule.onNodeWithTag("play-area").performTouchInput { moveTo(Offset((cell.x * d).toFloat(), ((cell.y - 20) * d).toFloat())) }
        rule.mainClock.advanceTimeBy(48)
        assertTrue("drag started through the overlay", rule.runOnUiThread { session.isDragging })
        rule.onNodeWithTag("play-area").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(500)

        rule.runOnUiThread { session.restore(PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 0, null)) }
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithTag("test-overlay").assertDoesNotExist()
    }
}
