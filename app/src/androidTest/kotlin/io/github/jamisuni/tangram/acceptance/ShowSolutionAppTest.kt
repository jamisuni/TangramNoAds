package io.github.jamisuni.tangram.acceptance

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import io.github.jamisuni.tangram.MainActivity
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import kotlin.math.abs

// ACCEPTANCE TEST (TASK-T5, independent author), visible slice: REQ-046.A2 on the real app, plus the prose rules around it.
// Real touches, the real library, the real store (wiped by `ResetStoreRule`, DA-62). The held-out slice (A1, A3) is not here.
// Mapping from puzzle units to pixels is the public `BoardTransform` of the `board` node (through the WO-004 `TouchRig.mapper`),
// never inferred from pixels. The solution overlay is found by its tags, never by colour; the pixel check compares the SAME points
// of the empty silhouette before and after switching the overlay on, so no silhouette colour is assumed (DA-40: points are at
// least 0.12 unit from every polygon edge, so no outline corner or dashed edge is sampled).
class ShowSolutionAppTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(ResetStoreRule()).around(compose)

    private val puzzles = PuzzleLibrary.packaged().puzzles

    private fun dist(a: Int, b: Int): Int =
        abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)) + abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)) + abs((a and 0xFF) - (b and 0xFF))

    private fun open(p: Puzzle) {
        compose.touch("puzzle-counter")
        compose.touch("grid-cell-${p.id.value}")
        compose.onNodeWithTag("all-puzzles").assertDoesNotExist()
    }

    private fun showTheSolution() {
        compose.unlockDevAid()
        compose.touch("dev-show-solution")
        compose.waitForIdle()
    }

    // REQ-046.A2 - "'Show the solution' draws one shape per piece of the puzzle on the silhouette."
    // On the real app: before unlocking there is no shape node; after unlocking and "Show the solution" there is exactly one
    // `dev-solution-<piece>` node per piece of the shown puzzle, each node's bounds are its stored polygon's bounding box mapped
    // through the board's transform, and inside every piece of the silhouette the pixels changed towards that piece's colour.
    @Test
    fun req046_A2_showTheSolutionDrawsOneShapePerPieceOnTheSilhouette() {
        val puzzle = puzzles.first()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForIdle()
            val rig = TouchRig(compose)
            val before = rig.shot()
            assertEquals("no overlay shape before the aid is used", emptyList<String>(), compose.overlayTags())

            showTheSolution()

            assertEquals(expectedOverlayTags(puzzle), compose.overlayTags())

            val after = rig.shot()
            val m = rig.mapper(after, rig.board(), puzzle)
            val area = compose.boundsOf("play-area")
            val byTag = compose.overlayNodes().associateBy { it.tag() }
            for (entry in puzzle.solution) {
                val poly = entry.polygon.map { it.x.toDouble() to it.y.toDouble() }
                val node = byTag["dev-solution-${entry.piece.name}"] ?: error("no node for ${entry.piece}")
                val b = node.boundsInRoot
                val tol = 2.5
                val lo = m.px(poly.minOf { it.first }, poly.minOf { it.second })
                val hi = m.px(poly.maxOf { it.first }, poly.maxOf { it.second })
                assertEquals("${entry.piece} left", lo.x.toDouble(), (b.left - area.left).toDouble(), tol)
                assertEquals("${entry.piece} top", lo.y.toDouble(), (b.top - area.top).toDouble(), tol)
                assertEquals("${entry.piece} right", hi.x.toDouble(), (b.right - area.left).toDouble(), tol)
                assertEquals("${entry.piece} bottom", hi.y.toDouble(), (b.bottom - area.top).toDouble(), tol)

                val points = interiorPoints(poly, margin = 0.12)
                assertTrue("fixture: ${entry.piece} has no sample point away from its edges", points.size >= 3)
                val own = PieceColours.rgb(entry.piece)
                var changed = 0
                var distBefore = 0.0
                var distAfter = 0.0
                for ((ux, uy) in points) {
                    val px = m.px(ux, uy)
                    val c0 = before.getPixel(px.x.toInt(), px.y.toInt())
                    val c1 = after.getPixel(px.x.toInt(), px.y.toInt())
                    if (colourDiff(c0, c1) > 12) changed++
                    distBefore += dist(c0, own)
                    distAfter += dist(c1, own)
                }
                distBefore /= points.size
                distAfter /= points.size
                assertTrue("${entry.piece}: only $changed of ${points.size} points inside its solution polygon changed", changed * 10 >= points.size * 6)
                assertTrue("fixture: the empty silhouette is too close to the colour of ${entry.piece} ($distBefore)", distBefore > 40)
                assertTrue("${entry.piece}: the shape is not drawn in its colour (mean distance $distBefore -> $distAfter)", distAfter <= distBefore * 0.8)
            }
        }
    }

    // REQ-046 rule 3 (prose, not an acceptance claim): the overlay "stays on while the player browses puzzles until it is switched
    // off". On the real app: a puzzle with another piece count shows that puzzle's own shapes; switching it off removes them.
    @Test
    fun theOverlayStaysOnWhileBrowsingUntilSwitchedOff() {
        val first = puzzles.first()
        val other = puzzles.firstOrNull { it.solution.size != first.solution.size } ?: error("fixture: every puzzle has ${first.solution.size} pieces")
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForIdle()
            showTheSolution()
            assertEquals(expectedOverlayTags(first), compose.overlayTags())

            open(other)
            compose.waitForIdle()
            assertEquals("the other puzzle shows its own shapes", expectedOverlayTags(other), compose.overlayTags())

            compose.touch("next-button")
            compose.touch("prev-button")
            compose.waitForIdle()
            assertEquals("still on after moving away and back", expectedOverlayTags(other), compose.overlayTags())

            compose.touch("dev-button")
            compose.onNodeWithTag("dev-show-solution").assertTextEquals(aidString("devtools_hide_solution"))
            compose.touch("dev-show-solution")
            compose.waitForIdle()
            assertEquals("switched off", emptyList<String>(), compose.overlayTags())
        }
    }

    // decision DA-82: the overlay is never drawn on a SOLVED puzzle (the picture replaces the pieces) and the DEV pill is hidden
    // there (REQ-046 rule 5, "hidden on a solved puzzle"); after Retry the puzzle is New again and both come back.
    @Test
    fun decisionDA82_aSolvedPuzzleHasNeitherOverlayNorPillAndRetryBringsThemBack() {
        val solved = puzzles[2]
        AppStore.open().saveProgress(solved.id, PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 47, 41))
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForIdle()
            showTheSolution()
            assertEquals(expectedOverlayTags(puzzles.first()), compose.overlayTags())

            open(solved)
            compose.waitForIdle()
            compose.onNodeWithTag("solved-bar").assertExists()
            assertEquals("no shapes on a solved puzzle", emptyList<String>(), compose.overlayTags())
            compose.onNodeWithTag("dev-button").assertDoesNotExist()

            compose.touch("retry-button")
            compose.waitForIdle()
            compose.onNodeWithTag("solved-bar").assertDoesNotExist()
            assertEquals("the overlay returns after Retry", expectedOverlayTags(solved), compose.overlayTags())
            compose.onNodeWithTag("dev-button").assertExists()
        }
    }
}
