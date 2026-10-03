package io.github.jamisuni.tangram.acceptance

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import io.github.jamisuni.tangram.MainActivity
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

// ACCEPTANCE TEST (TASK-T5, independent author), visible slice: decision tests of WO-005 on the real app. No acceptance tokens:
// these pin AI decisions (DA-75 the pill's place, DA-87 the drag guard). Real touch only; the transform is the public
// `BoardTransform` through `TouchRig.mapper`. Held-out A1/A3 scenarios are not here.
class DevAidDecisionsAppTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(ResetStoreRule()).around(compose)

    private val puzzles = PuzzleLibrary.packaged().puzzles

    // decision DA-75 (design "Scaffolding tests", app): at the emulator's own size, with Restart shown, the DEV pill and Restart do not
    // intersect, the pill lies inside the play area and it intersects no silhouette polygon of the shown puzzle. (The sweep over
    // sizes is the `play` JVM test; this one is the real screen.)
    @Test
    fun decisionDA75_theDevPillIsClearOfRestartAndTheSilhouetteOnTheRealScreen() {
        val index = puzzles.indexOfFirst { it.solution.size == 7 }
        check(index >= 0) { "fixture: a 7-piece puzzle" }
        val puzzle = puzzles[index]
        val three = puzzle.solution.map { it.piece }.take(3)
        AppStore.open().apply {
            saveProgress(puzzle.id, PuzzleProgress(PuzzleState.IN_PROGRESS, three.associateWith { solutionSave(puzzle, it) }, 12, null))
            saveLastShownPuzzle(puzzle.id)
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForIdle()
            val rig = TouchRig(compose)
            compose.onNodeWithTag("restart-button").assertExists()
            compose.onNodeWithTag("dev-button").assertExists()
            val dev = compose.boundsOf("dev-button")
            val restart = compose.boundsOf("restart-button")
            val area = compose.boundsOf("play-area")
            assertTrue("the DEV pill is not inside the play area: $dev in $area", dev.left >= area.left - 1 && dev.top >= area.top - 1 && dev.right <= area.right + 1 && dev.bottom <= area.bottom + 1)
            val apart = dev.right <= restart.left || restart.right <= dev.left || dev.bottom <= restart.top || restart.bottom <= dev.top
            assertTrue("the DEV pill $dev intersects Restart $restart", apart)

            val m = rig.mapper(rig.shot(), rig.board(), puzzle)
            for (sp in puzzle.solution) {
                val poly = sp.polygon.map { pt ->
                    val o = m.px(pt.x.toDouble(), pt.y.toDouble())
                    (area.left + o.x).toDouble() to (area.top + o.y).toDouble()
                }
                assertFalse(
                    "the DEV pill $dev intersects the silhouette (${sp.piece})",
                    rectHitsPolygon(dev.left.toDouble(), dev.top.toDouble(), dev.right.toDouble(), dev.bottom.toDouble(), poly),
                )
            }
        }
    }

    // decision DA-87 (decisions F5): a second finger on the DEV pill while a piece is being dragged opens nothing - and the same
    // pill opens its dialog by an ordinary tap once the drag is over, so the pill is not simply dead.
    @Test
    fun decisionDA87_aSecondFingerOnTheDevPillDuringADragOpensNothing() {
        val puzzle = puzzles.first()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(400)
            val rig = TouchRig(compose)
            val shot = rig.shot()
            val board = rig.board()
            val piece = puzzle.solution.first().piece
            val start = trayCentreOf(shot, board.y1, piece)
            val area = compose.boundsOf("play-area")
            val dev = compose.boundsOf("dev-button")
            val devAt = Offset(dev.center.x - area.left, dev.center.y - area.top)
            val d = rig.density

            compose.onNodeWithTag("play-area").performTouchInput { down(0, start) }
            compose.mainClock.advanceTimeBy(16)
            compose.onNodeWithTag("play-area").performTouchInput { moveTo(Offset(start.x, start.y - 40f * d)) }
            compose.mainClock.advanceTimeBy(150) // past the 12 dp threshold: the drag has begun

            compose.onNodeWithTag("play-area").performTouchInput { down(1, devAt) }
            compose.mainClock.advanceTimeBy(30)
            compose.onNodeWithTag("play-area").performTouchInput { up(1) }
            compose.mainClock.advanceTimeBy(300)
            compose.onNodeWithTag("dev-dialog").assertDoesNotExist()

            compose.onNodeWithTag("play-area").performTouchInput { up(0) }
            compose.mainClock.advanceTimeBy(1200)
            compose.onNodeWithTag("dev-dialog").assertDoesNotExist()

            compose.touch("dev-button")
            compose.onNodeWithTag("dev-dialog").assertExists()
        }
    }
}
