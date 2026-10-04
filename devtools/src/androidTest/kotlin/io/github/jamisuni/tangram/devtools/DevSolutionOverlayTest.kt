package io.github.jamisuni.tangram.devtools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// ACCEPTANCE TEST (TASK-T5, independent author), device half of the overlay in the `devtools` module. Written from REQ-046 and
// the frozen seams `DevSolutionOverlay(state, puzzle, toDp, modifier)`, `DevToolsState` and the tags `dev-solution-overlay`,
// `dev-solution-<PieceId.name>` (one per piece, bounds = the polygon's bounding box in overlay coordinates; none while the
// overlay is off). The coordinate mapping is a literal scale and offset, so expected bounds are computed independently.
class DevSolutionOverlayTest {

    @get:Rule
    val compose = createComposeRule()

    private val puzzle = PuzzleLibrary.packaged().puzzles.first { it.solution.size == 7 }
    private val scale = 30.0
    private val offset = 12.0
    private fun toDp(p: Vec2) = Vec2(offset + p.x * scale, offset + p.y * scale)

    private fun ComposeTestRule.pieceNodes(): List<SemanticsNode> = onAllNodes(
        SemanticsMatcher("a dev-solution piece node") { n ->
            val t = n.config.getOrNull(SemanticsProperties.TestTag)
            t != null && t.startsWith("dev-solution-") && t != "dev-solution-overlay"
        },
        useUnmergedTree = true,
    ).fetchSemanticsNodes()

    private fun tagOf(n: SemanticsNode): String = n.config.getOrNull(SemanticsProperties.TestTag) ?: error("node without a test tag")

    private fun show(state: DevToolsState) {
        compose.setContent {
            Box(Modifier.size(360.dp, 640.dp)) {
                DevSolutionOverlay(state, puzzle, { toDp(it) }, Modifier.fillMaxSize())
            }
        }
        compose.waitForIdle()
    }

    private fun unlockedState() = DevToolsState().also { check(it.submit("0417")) { "fixture: the passcode did not unlock" } }

    // REQ-046.A2 - "'Show the solution' draws one shape per piece of the puzzle on the silhouette."
    // With the overlay on there is exactly one `dev-solution-<piece>` node per piece of the puzzle (no more, no fewer, none of a
    // piece the puzzle does not have); with it switched off there is none; switched on again they return.
    @Test
    fun req046_A2_theOverlayHasExactlyOneShapeNodePerPieceAndNoneWhenOff() {
        val state = unlockedState()
        state.toggleOverlay()
        assertTrue("fixture: the overlay flag is on", state.overlayOn)
        show(state)
        val want = puzzle.solution.map { "dev-solution-${it.piece.name}" }.sorted()
        assertEquals(want, compose.pieceNodes().map { tagOf(it) }.sorted())

        compose.runOnUiThread { state.toggleOverlay() }
        compose.waitForIdle()
        assertEquals("no shape nodes while the overlay is off", emptyList<String>(), compose.pieceNodes().map { tagOf(it) })

        compose.runOnUiThread { state.toggleOverlay() }
        compose.waitForIdle()
        assertEquals(want, compose.pieceNodes().map { tagOf(it) }.sorted())
    }

    // REQ-046.A2 - the shape of a piece lies where the stored solution puts it: its node's bounds are the bounding box of the
    // piece's stored polygon, mapped through the overlay's own coordinate mapping, in overlay coordinates (design seam).
    @Test
    fun req046_A2_eachShapeLiesAtItsStoredSolutionPosition() {
        val state = unlockedState()
        state.toggleOverlay()
        show(state)
        val overlay = compose.onNodeWithTag("dev-solution-overlay", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val density = compose.density.density
        val nodes = compose.pieceNodes().associateBy { tagOf(it) }
        for (entry in puzzle.solution) {
            val node = nodes["dev-solution-${entry.piece.name}"] ?: error("no node for ${entry.piece}")
            val pts = entry.polygon.map { toDp(Vec2(it.x.toDouble(), it.y.toDouble())) }
            val b = node.boundsInRoot
            val tol = 2.0
            assertEquals("${entry.piece} left", pts.minOf { it.x } * density, (b.left - overlay.left).toDouble(), tol)
            assertEquals("${entry.piece} top", pts.minOf { it.y } * density, (b.top - overlay.top).toDouble(), tol)
            assertEquals("${entry.piece} right", pts.maxOf { it.x } * density, (b.right - overlay.left).toDouble(), tol)
            assertEquals("${entry.piece} bottom", pts.maxOf { it.y } * density, (b.bottom - overlay.top).toDouble(), tol)
        }
    }

    // REQ-046 rule 3 (prose, not an acceptance claim): a puzzle with fewer pieces gets fewer shapes - the count follows the
    // puzzle, not a fixed seven.
    @Test
    fun theCountFollowsThePuzzleNotAFixedSeven() {
        val small = PuzzleLibrary.packaged().puzzles.firstOrNull { it.solution.size < 7 }
            ?: error("fixture: the library has no puzzle with fewer than seven pieces")
        val state = unlockedState()
        state.toggleOverlay()
        compose.setContent {
            Box(Modifier.size(360.dp, 640.dp)) {
                DevSolutionOverlay(state, small, { toDp(it) }, Modifier.fillMaxSize())
            }
        }
        compose.waitForIdle()
        assertEquals(small.solution.map { "dev-solution-${it.piece.name}" }.sorted(), compose.pieceNodes().map { tagOf(it) }.sorted())
    }
}
