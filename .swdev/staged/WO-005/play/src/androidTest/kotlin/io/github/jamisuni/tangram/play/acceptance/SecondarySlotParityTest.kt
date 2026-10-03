package io.github.jamisuni.tangram.play.acceptance

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.play.BoardTransform
import io.github.jamisuni.tangram.play.PlayArea
import io.github.jamisuni.tangram.play.PlaySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// ACCEPTANCE TEST (TASK-T5, independent author), device: decision DA-75 board parity. Written from design WO-005 section 2a/3b and
// the frozen `PlayArea(..., cornerControl, boardOverlay, secondaryCornerControl)` seam: "`secondaryCornerControl` is laid out after
// the board and the primary slot and never changes them", "A device test composes PlayArea with and without
// `secondaryCornerControl` and asserts the `BoardTransform` semantics (scale, origin) are equal for several puzzles".
// The play area is a fixed 360 x 640 dp (the WO-003 device convention); the primary slot is an 82 x 48 dp box (a Restart-sized
// stand-in), the secondary a 56 x 40 dp box, both tagged here. No acceptance tokens: this pins an AI decision.
class SecondarySlotParityTest {

    @get:Rule
    val compose = createComposeRule()

    private val rows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )

    private val library = PuzzleLibrary.packaged().puzzles
    private fun byId(id: String): Puzzle = library.firstOrNull { it.id.value == id } ?: error("fixture: no packaged puzzle $id")

    // The four puzzles on which the design sweep found the corner contested, plus the first 7-piece puzzle and the first puzzle.
    private val puzzles = listOf(
        library.first(),
        library.first { it.solution.size == 7 },
        byId("animals-cat"),
        byId("shapes-warmup-1"),
        byId("shapes-warmup-2"),
        byId("shapes-square"),
    ).distinctBy { it.id.value }

    private val current = mutableStateOf(puzzles.first())
    private val withSecondary = mutableStateOf(false)

    private fun show() {
        compose.setContent {
            val puzzle = current.value
            val session = remember(puzzle) { PlaySession(puzzle) }
            Box(Modifier.size(360.dp, 640.dp)) {
                PlayArea(
                    session, LayoutClass.PHONE, rows, 780.dp, Modifier.fillMaxSize(),
                    cornerControl = { Box(Modifier.size(82.dp, 48.dp).testTag("aid-primary")) },
                    secondaryCornerControl = if (withSecondary.value) {
                        { Box(Modifier.size(56.dp, 40.dp).testTag("aid-secondary")) }
                    } else {
                        null
                    },
                )
            }
        }
        compose.waitForIdle()
    }

    private fun transform() =
        compose.onNodeWithTag("board").fetchSemanticsNode().config.getOrNull(BoardTransform) ?: error("the board node carries no BoardTransform")

    private fun bounds(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun set(p: Puzzle, secondary: Boolean) {
        compose.runOnUiThread {
            current.value = p
            withSecondary.value = secondary
        }
        compose.waitForIdle()
    }

    private class Poly(val pts: List<Pair<Double, Double>>)

    /** Rect (l,t,r,b) against a convex polygon, separating axes, touching counts as overlap. */
    private fun overlaps(l: Double, t: Double, r: Double, b: Double, poly: Poly): Boolean {
        val rect = listOf(l to t, r to t, r to b, l to b)
        val axes = mutableListOf(1.0 to 0.0, 0.0 to 1.0)
        for (i in poly.pts.indices) {
            val a = poly.pts[i]
            val c = poly.pts[(i + 1) % poly.pts.size]
            axes += (-(c.second - a.second)) to (c.first - a.first)
        }
        for ((ax, ay) in axes) {
            fun range(pts: List<Pair<Double, Double>>) = pts.map { it.first * ax + it.second * ay }.let { it.min() to it.max() }
            val (a0, a1) = range(rect)
            val (b0, b1) = range(poly.pts)
            if (a1 < b0 - 1e-6 || b1 < a0 - 1e-6) return false
        }
        return true
    }

    // decision DA-75: adding the secondary slot changes neither the board (scale and origin) nor where the primary control sits,
    // for several puzzles, and the pill itself is placed inside the area, 4 dp clear of the primary control and of the silhouette.
    @Test
    fun decisionDA75_theSecondarySlotChangesNeitherTheBoardNorTheRestartPlace() {
        show()
        val density = compose.density.density
        for (p in puzzles) {
            set(p, secondary = false)
            val t0 = transform()
            val primary0 = bounds("aid-primary")
            set(p, secondary = true)
            val t1 = transform()
            val primary1 = bounds("aid-primary")
            val tag = p.id.value
            assertEquals("$tag: board scale", t0.scalePx, t1.scalePx, 0.01f)
            assertEquals("$tag: board origin x", t0.originXPx, t1.originXPx, 0.01f)
            assertEquals("$tag: board origin y", t0.originYPx, t1.originYPx, 0.01f)
            assertEquals("$tag: the primary control moved (left)", primary0.left, primary1.left, 0.5f)
            assertEquals("$tag: the primary control moved (top)", primary0.top, primary1.top, 0.5f)

            val sec = bounds("aid-secondary")
            val area = bounds("play-area")
            assertEquals("$tag: the pill is 56 dp wide", 56.0, (sec.right - sec.left) / density.toDouble(), 0.5)
            assertEquals("$tag: the pill is 40 dp high", 40.0, (sec.bottom - sec.top) / density.toDouble(), 0.5)
            assertTrue("$tag: inside the area", sec.left >= area.left - 1 && sec.top >= area.top - 1 && sec.right <= area.right + 1 && sec.bottom <= area.bottom + 1)
            val gap = 4.0 * density - 1.5 // 4 dp clearance, less rounding slack
            val apart = sec.left - gap > primary1.right || primary1.left - gap > sec.right || sec.top - gap > primary1.bottom || primary1.top - gap > sec.bottom
            assertTrue("$tag: the pill is not 4 dp clear of the primary control: $sec vs $primary1", apart)
            for (sp in p.solution) {
                val poly = Poly(sp.polygon.map { pt ->
                    (area.left + t1.originXPx + pt.x.toDouble() * t1.scalePx) to (area.top + t1.originYPx + pt.y.toDouble() * t1.scalePx)
                })
                assertTrue(
                    "$tag: the pill is not 4 dp clear of the silhouette (${sp.piece})",
                    !overlaps(sec.left - gap, sec.top - gap, sec.right + gap, sec.bottom + gap, poly),
                )
            }
        }
    }
}
