package io.github.jamisuni.tangram.play

import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.layout.TrayRules
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.play.draw.VisualTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// SCAFFOLDING (TASK-026, disposable): the SOLVED drawing rule (DA-52), the solvedBar slot and PuzzleThumbnail on a
// device. Not acceptance tests.
class PlayAreaSolvedScaffoldingTest {

    @get:Rule
    val rule = createComposeRule()

    private val rows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val puzzle = PuzzleLibrary.packaged().puzzles.first { it.id.value == "shapes-mini-1" }

    private fun show(session: PlaySession, withBar: Boolean) {
        rule.setContent {
            Box(Modifier.size(360.dp, 640.dp)) {
                PlayArea(
                    session, LayoutClass.PHONE, rows, 780.dp, Modifier.fillMaxSize(),
                    solvedBar = if (withBar) {
                        { Box(Modifier.fillMaxSize().semantics { testTag = "test-solved-bar" }) }
                    } else null,
                )
            }
        }
        rule.waitForIdle()
    }

    private fun restoredSolved() = PlaySession(puzzle, reducedMotion = { true }).also {
        it.restore(PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 0, null))
    }

    @Test
    fun restoredSolvedSessionShowsNoTrayAndTheSolvedBar() {
        val session = restoredSolved()
        show(session, withBar = true)
        for (piece in PieceId.entries) rule.onNodeWithTag("size-mark-${piece.name}").assertDoesNotExist()
        rule.onNodeWithTag("flip-badge").assertDoesNotExist()
        val t = rule.onNodeWithTag("board").fetchSemanticsNode().config[BoardTransform]
        assertTrue("no tray cell rects while solved", t.trayCellsPx.isEmpty())
        rule.onNodeWithTag("test-solved-bar").assertIsDisplayed()

        // the bar fills the tray region: its top is the layout's trayTop
        val layout = PlayLayout.compute(360.0, 640.0, 780.0, LayoutClass.PHONE, rows, puzzle)
        val d = rule.density.density
        val barTop = rule.onNodeWithTag("test-solved-bar").fetchSemanticsNode().boundsInRoot.top
        assertEquals(layout.trayTop * d, barTop.toDouble(), 1.5)

        // and nothing of the tray is drawn there: the old SQ cell centre is neither the cell fill nor the piece colour
        val c = layout.cell(PieceId.SQ).centre
        val px = rule.onNodeWithTag("play-area").captureToImage().asAndroidBitmap()
            .getPixel((c.x * d).toInt(), (c.y * d).toInt())
        assertNotEquals(VisualTokens.TRAY_CELL.toArgb(), px)
        assertNotEquals(0xFF000000.toInt() or TrayRules.colour(PieceId.SQ), px)
    }

    @Test
    fun unsolvedSessionShowsTrayAndNoSolvedBar() {
        val session = PlaySession(puzzle, reducedMotion = { true })
        show(session, withBar = true)
        rule.onNodeWithTag("test-solved-bar").assertDoesNotExist()
        rule.onNodeWithTag("size-mark-ST1").assertExists()
        val t = rule.onNodeWithTag("board").fetchSemanticsNode().config[BoardTransform]
        assertTrue(t.trayCellsPx.keys.contains(PieceId.ST1))
    }

    @Test
    fun wideCornerControlIsPlacedAtItsMeasuredSizeInsideTheBoardClearOfTheSilhouette() {
        val all = PuzzleLibrary.packaged().puzzles
        var current by androidx.compose.runtime.mutableStateOf(PlaySession(all.first(), reducedMotion = { true }))
        rule.setContent {
            Box(Modifier.size(360.dp, 640.dp)) {
                PlayArea(
                    current, LayoutClass.PHONE, rows, 780.dp, Modifier.fillMaxSize(),
                    cornerControl = { Box(Modifier.size(160.dp, 48.dp).semantics { testTag = "test-corner" }) },
                )
            }
        }
        for (p in all) {
            rule.runOnUiThread { current = PlaySession(p, reducedMotion = { true }) }
            rule.waitForIdle()
            rule.onNodeWithTag("test-corner").assertIsDisplayed()
            val d = rule.density.density
            val b = rule.onNodeWithTag("test-corner").fetchSemanticsNode().boundsInRoot
            // not clipped: exactly its own size
            assertEquals(p.id.value, 160.0 * d, (b.right - b.left).toDouble(), 1.5)
            assertEquals(p.id.value, 48.0 * d, (b.bottom - b.top).toDouble(), 1.5)
            val (layout, placement) = PlayLayout.computeWithCorner(360.0, 640.0, 780.0, LayoutClass.PHONE, rows, p, 160.0, 48.0)
            assertEquals(p.id.value, placement.rect.left * d, b.left.toDouble(), 1.5)
            assertEquals(p.id.value, placement.rect.top * d, b.top.toDouble(), 1.5)
            // fully inside the board (or the strip), never over the silhouette
            val r = RectDp(b.left / d.toDouble(), b.top / d.toDouble(), b.right / d.toDouble(), b.bottom / d.toDouble())
            assertTrue(p.id.value, r.left >= -0.5 && r.right <= 360.5)
            if (placement.corner != ControlCorner.STRIP) {
                assertTrue(p.id.value, r.top >= layout.boardRect.top - 0.5 && r.bottom <= layout.boardRect.bottom + 0.5)
            }
            assertTrue("${p.id.value}: over the silhouette", !PlayLayout.touchesSilhouette(r, layout.silhouetteDp, 0.0))
        }
    }

    @Test
    fun thumbnailUnderUnboundedConstraintsDrawsNothingAndDoesNotCrash() {
        rule.setContent {
            val scroll = androidx.compose.foundation.rememberScrollState()
            androidx.compose.foundation.layout.Row(Modifier.horizontalScroll(scroll)) {
                PuzzleThumbnail(puzzle, true)
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun solvedWithoutASlotStillDrawsNoTray() {
        show(restoredSolved(), withBar = false)
        rule.onNodeWithTag("size-mark-ST1").assertDoesNotExist()
    }

    @Test
    fun thumbnailDrawsFlatSilhouetteWhenNotSolvedAndPictureWhenSolved() {
        rule.setContent {
            androidx.compose.foundation.layout.Row {
                Box(Modifier.size(100.dp).semantics { testTag = "thumb-flat" }) { PuzzleThumbnail(puzzle, false) }
                Box(Modifier.size(100.dp).semantics { testTag = "thumb-pic" }) { PuzzleThumbnail(puzzle, true) }
            }
        }
        rule.waitForIdle()
        fun colours(tag: String): Set<Int> {
            val b = rule.onNodeWithTag(tag).captureToImage().asAndroidBitmap()
            val s = HashSet<Int>()
            for (y in 0 until b.height step 2) for (x in 0 until b.width step 2) s += b.getPixel(x, y)
            return s
        }
        val flat = colours("thumb-flat")
        val sil = VisualTokens.SILHOUETTE.toArgb()
        assertTrue("flat thumbnail paints the silhouette colour; got ${flat.map { Integer.toHexString(it) }.take(8)}", sil in flat)
        val pic = colours("thumb-pic")
        assertTrue("picture thumbnail has pixels other than the silhouette colour", pic.any { it != sil && it !in flat })
    }

    // The early-return trap (plan review F5), on a device: an aid solve with no touch must still run the REQ-023 timeline.
    @Test
    fun scaffoldAidSolveWithNoTouchRunsTheFadeTimeline() {
        rule.mainClock.autoAdvance = false
        val session = PlaySession(puzzle, reducedMotion = { true })
        show(session, withBar = false)
        val poses = puzzle.solution.map {
            io.github.jamisuni.tangram.kernel.geometry.PieceGeometry.poseOf(it.piece, it.polygon)!!
        }
        rule.runOnUiThread { assertTrue(session.solveByAid(poses)) }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(100)
        val early = rule.onNodeWithTag("play-area").captureToImage().asAndroidBitmap()
        rule.mainClock.advanceTimeBy(3000)
        rule.waitForIdle()
        assertEquals(PuzzleState.SOLVED, session.state)
        assertTrue("the solve timeline must have started", session.solved != null && !session.solvePending)
        val late = rule.onNodeWithTag("play-area").captureToImage().asAndroidBitmap()
        var diff = 0
        for (y in 0 until late.height step 3) for (x in 0 until late.width step 3) {
            if (early.getPixel(x, y) != late.getPixel(x, y)) diff++
        }
        assertTrue("the solved picture must have faded in (differing samples: $diff)", diff > 50)
    }
}
