package io.github.jamisuni.tangram.browse

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * Device tests of the top bar, the grid and the solved bar (design WO-004 sections 5 and 8, test seams: the tags are the
 * frozen list). Touch injection only (`performTouchInput`, never `performClick`); the long press runs on the test clock.
 * The Finnish/English state words are read from the string resources by their frozen keys.
 */
class BrowseDeviceTest {

    @get:Rule
    val rule = createComposeRule()

    private val puzzles = DeviceKit.puzzles
    private val n = puzzles.size
    private val strings = InstrumentationRegistry.getInstrumentation().targetContext

    private fun titleOf(p: Puzzle): String = p.title.inLanguage(Locale.getDefault().language)

    // setContent may run once per test (the activity refuses a second call): the controller is swapped through state.
    private var contentSet = false
    private var shownController by mutableStateOf<BrowseController?>(null)
    private var shownThumbs: MutableMap<PuzzleId, Boolean> = HashMap()

    private fun show(c: BrowseController, thumbs: MutableMap<PuzzleId, Boolean> = HashMap()) {
        shownThumbs = thumbs
        if (contentSet) {
            rule.runOnIdle { shownController = c }
            rule.waitForIdle()
            return
        }
        contentSet = true
        shownController = c
        rule.setContent {
            val current = shownController ?: return@setContent
            key(current) {
                Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) { BrowseTopBar(current, LayoutClass.PHONE) }
                    if (current.gridOpen) {
                        AllPuzzlesOverlay(
                            current,
                            thumbnail = { p, solved, m ->
                                SideEffect { shownThumbs[p.id] = solved }
                                Box(m)
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    private fun press(tag: String) = rule.onNodeWithTag(tag).performTouchInput { click() }

    private fun assertShows(index: Int) {
        rule.onNodeWithTag("puzzle-title").assertTextEquals(titleOf(puzzles[index]))
        rule.onNodeWithTag("puzzle-counter").assertTextEquals("${index + 1} / $n")
    }

    /** Down, hold for [holdMs] of test-clock time, up. */
    private fun hold(tag: String, holdMs: Long) {
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag(tag).performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(holdMs)
        rule.onNodeWithTag(tag).performTouchInput { up() }
        rule.mainClock.advanceTimeBy(300)
        rule.mainClock.autoAdvance = true
    }

    // ------------------------------------------------------------------------------------------ REQ-024

    // REQ-024.A1 - "› works on an unsolved puzzle."  A real touch on the next button shows the next puzzle.
    @Test
    fun req024_A1_aTouchOnNextShowsTheNextPuzzle() {
        val (c, _) = DeviceKit.controller()
        show(c)
        assertShows(0)
        press("next-button")
        assertShows(1)
        press("next-button")
        assertShows(2)
    }

    // REQ-024.A2 - "The top bar always shows the current puzzle's name and number."
    // The title in the device language and the counter `n / total`, for the first, a middle and the last puzzle.
    @Test
    fun req024_A2_theTopBarShowsTheNameAndNumberOfTheFirstAMiddleAndTheLastPuzzle() {
        for (index in listOf(0, n / 2, n - 1)) {
            val (c, _) = DeviceKit.controller { seedLastShown(puzzles[index].id) }
            show(c)
            assertShows(index)
        }
    }

    // REQ-024.A2 - and it follows every move of the buttons, both ways.
    @Test
    fun req024_A2_theTopBarFollowsNextAndPrevious() {
        val (c, _) = DeviceKit.controller { seedLastShown(puzzles[3].id) }
        show(c)
        assertShows(3)
        press("next-button")
        assertShows(4)
        press("prev-button")
        press("prev-button")
        assertShows(2)
    }

    // REQ-024.A3 - "› on the last puzzle shows the first."  (and ‹ on the first shows the last)
    @Test
    fun req024_A3_nextOnTheLastShowsTheFirstAndPreviousOnTheFirstShowsTheLast() {
        val (c, _) = DeviceKit.controller()
        show(c)
        press("prev-button")
        assertShows(n - 1)
        press("next-button")
        assertShows(0)
    }

    // ------------------------------------------------------------------------------------------ REQ-050 grid

    // REQ-050.A2 - "Pressing the counter shows one cell per puzzle, solved ones in colour."
    // One `grid-cell-<id>` per puzzle in library order (REQ-050 rule 2); the thumbnail slot gets `solved = true` only for the
    // solved puzzle (the picture, ASSUMPTION of REQ-050); `grid-dot-<id>` only for the In progress puzzle (rule 2).
    @Test
    fun req050_A2_theCounterOpensAGridWithOneCellPerPuzzleSolvedOnesInColour() {
        val (c, _) = DeviceKit.controller {
            seed(puzzles[1].id, DeviceKit.solved())
            seed(puzzles[2].id, DeviceKit.inProgress())
        }
        val thumbs = HashMap<PuzzleId, Boolean>()
        show(c, thumbs)
        rule.onNodeWithTag("all-puzzles").assertDoesNotExist()
        press("puzzle-counter")
        rule.onNodeWithTag("all-puzzles").assertExists()

        for (p in puzzles) rule.onNodeWithTag("grid-cell-${p.id.value}").assertExists()

        // library order: cells sorted by (top, left) are the puzzles in list order
        val positions = puzzles.map { p ->
            val b = rule.onNodeWithTag("grid-cell-${p.id.value}").fetchSemanticsNode().boundsInRoot
            p.id to (b.top to b.left)
        }
        assertEquals(puzzles.map { it.id }, positions.sortedWith(compareBy({ it.second.first }, { it.second.second })).map { it.first })

        // thumbnails: solved only for the solved puzzle
        rule.waitForIdle()
        for ((i, p) in puzzles.withIndex()) {
            assertEquals("thumbnail solved flag of ${p.id.value}", i == 1, thumbs[p.id])
        }

        // dots: only the In progress puzzle
        for ((i, p) in puzzles.withIndex()) {
            val dot = rule.onNodeWithTag("grid-dot-${p.id.value}")
            if (i == 2) dot.assertExists() else dot.assertDoesNotExist()
        }
    }

    // REQ-050 rule 4 (prose, not an acceptance claim): each grid cell's touch area is at least 64 dp square.
    @Test
    fun decisionDA55_everyGridCellIsAtLeast64DpSquare() {
        val (c, _) = DeviceKit.controller()
        show(c)
        press("puzzle-counter")
        for (p in puzzles) {
            rule.onNodeWithTag("grid-cell-${p.id.value}").assertWidthIsAtLeast(64.dp).assertHeightIsAtLeast(64.dp)
        }
    }

    // decision DA-55: choosing a cell opens that puzzle and closes the grid (the A3 claim itself is held out).
    @Test
    fun decisionDA55_aCellOpensItsPuzzleAndClosesTheGrid() {
        val (c, _) = DeviceKit.controller()
        show(c)
        press("puzzle-counter")
        press("grid-cell-${puzzles[5].id.value}")
        rule.onNodeWithTag("all-puzzles").assertDoesNotExist()
        assertShows(5)
    }

    // REQ-050 rule 3 (prose): closing the grid returns to the same puzzle.
    @Test
    fun decisionDA55_theCloseButtonReturnsToTheSamePuzzle() {
        val (c, _) = DeviceKit.controller { seedLastShown(puzzles[3].id) }
        show(c)
        press("puzzle-counter")
        rule.onNodeWithTag("all-puzzles").assertExists()
        press("grid-close")
        rule.onNodeWithTag("all-puzzles").assertDoesNotExist()
        assertShows(3)
    }

    // ------------------------------------------------------------------------------------------ long press (DA-54)

    // decision DA-54: with no other unsolved puzzle the long press behaves like a normal press.
    @Test
    fun decisionDA54_whenEveryPuzzleIsSolvedALongPressShowsTheNextPuzzle() {
        val (c, _) = DeviceKit.controller {
            for (p in puzzles) seed(p.id, DeviceKit.solved())
            seedLastShown(puzzles[3].id)
        }
        show(c)
        assertShows(3)
        hold("next-button", 650)
        assertShows(4)
    }

    /** Every puzzle Solved except index 3, puzzle 0 shown: the next unsolved puzzle is NOT index + 1, so a long press is distinguishable from a tap. */
    private fun controllerWithOnlyPuzzle4Unsolved(): BrowseController = DeviceKit.controller {
        for ((i, p) in puzzles.withIndex()) if (i != 3) seed(p.id, DeviceKit.solved())
        seedLastShown(puzzles[0].id)
    }.first

    // decision DA-54: a hold fires once (and is recognised): from puzzle 1 it lands on the unsolved puzzle 4 (index 3), not on
    // puzzle 2, and the release then adds no press (a second jump would fall back to a normal next and land on puzzle 5).
    @Test
    fun decisionDA54_aLongPressJumpsToTheNextUnsolvedPuzzleOnceAndTheReleaseDoesNotAddAPress() {
        show(controllerWithOnlyPuzzle4Unsolved())
        assertShows(0)
        hold("next-button", 900)
        assertShows(3)
    }

    // decision DA-54: in the same scenario a tap is a normal next and lands on puzzle 2 (index 1), not on the unsolved puzzle.
    @Test
    fun decisionDA54_aTapIsANormalNextEvenWhenAnUnsolvedPuzzleIsFurtherOn() {
        show(controllerWithOnlyPuzzle4Unsolved())
        press("next-button")
        assertShows(1)
    }

    // ------------------------------------------------------------------------------------------ state text and touch areas

    // decision DA-53: the state text shows all three states, New included (strings by their frozen resource keys).
    @Test
    fun decisionDA53_theStateTextShowsNewInProgressAndSolved() {
        val host = DeviceHost()
        val (c, _) = DeviceKit.controller(host)
        show(c)
        val expected = mapOf(
            PuzzleState.NEW to R.string.state_new,
            PuzzleState.IN_PROGRESS to R.string.state_in_progress,
            PuzzleState.SOLVED to R.string.state_solved,
        )
        for ((state, res) in expected) {
            rule.runOnIdle { host.state = state }
            rule.onNodeWithTag("puzzle-state").assertTextEquals(strings.getString(res))
        }
    }

    // decision DA-53: every player control has a touch area of at least 48 dp (top bar).
    @Test
    fun decisionDA53_topBarControlsAreAtLeast48Dp() {
        val (c, _) = DeviceKit.controller()
        show(c)
        for (tag in listOf("prev-button", "next-button", "puzzle-counter")) {
            rule.onNodeWithTag(tag).assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        }
    }

    // guardrail DA-53 (design section 5 `RestartButton`): the touch area is at least 48 x 48 dp, even if drawn 44 dp high.
    @Test
    fun decisionDA53_theRestartButtonIsAtLeast48DpSquareAndCallsBack() {
        var calls = 0
        rule.setContent { RestartButton(onClick = { calls++ }) }
        rule.onNodeWithTag("restart-button").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("restart-button").performTouchInput { click() }
        rule.runOnIdle { assertEquals(1, calls) }
    }

    // ------------------------------------------------------------------------------------------ solved bar

    // design section 5 `SolvedBar`: Retry and Next call back; each touch area is at least 48 dp high (DA-53).
    @Test
    fun decisionDA53_solvedBarButtonsCallBackAndAreAtLeast48DpHigh() {
        var retries = 0
        var nexts = 0
        rule.setContent { SolvedBar(bestSeconds = null, onRetry = { retries++ }, onNext = { nexts++ }) }
        rule.onNodeWithTag("solved-bar").assertExists()
        rule.onNodeWithTag("retry-button").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("solved-next-button").assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("retry-button").performTouchInput { click() }
        rule.onNodeWithTag("solved-next-button").performTouchInput { click() }
        rule.runOnIdle {
            assertEquals(1, retries)
            assertEquals(1, nexts)
        }
    }

    // design section 5 `SolvedBar`: `best-time` is the value only: the dash when there is none.
    @Test
    fun decisionDA52_theBestTimeNodeHoldsTheDashWhenThereIsNone() {
        rule.setContent { SolvedBar(bestSeconds = null, onRetry = {}, onNext = {}) }
        rule.onNodeWithTag("best-time").assertTextEquals(strings.getString(R.string.best_time_none))
    }

    // design section 5 `SolvedBar`: otherwise m:ss (hours are WO-008's).
    @Test
    fun decisionDA52_theBestTimeIsShownAsMinutesAndSeconds() {
        rule.setContent { SolvedBar(bestSeconds = 75L, onRetry = {}, onNext = {}) }
        rule.onNodeWithTag("best-time").assertTextEquals(strings.getString(R.string.time_minutes_seconds, 1, 15))
        rule.onNodeWithTag("best-time").assertTextEquals("1:15")
    }
}
