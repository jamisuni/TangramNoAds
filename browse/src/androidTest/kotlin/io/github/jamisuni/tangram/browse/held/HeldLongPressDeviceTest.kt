package io.github.jamisuni.tangram.browse.held

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import io.github.jamisuni.tangram.browse.BrowseController
import io.github.jamisuni.tangram.browse.BrowseTopBar
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * HELD-OUT (Test & Verify only): REQ-050 A1 on the real top bar. Touch injection only; the hold runs on the TEST clock
 * (design section 5: "the device test advances the test clock by at least 500 ms").
 */
class HeldLongPressDeviceTest {

    @get:Rule
    val rule = createComposeRule()

    private val puzzles = DeviceKit.puzzles
    private val n = puzzles.size
    private fun titleOf(p: Puzzle) = p.title.inLanguage(Locale.getDefault().language)

    private fun show(c: BrowseController) {
        rule.setContent { Box(Modifier.fillMaxSize()) { Column(Modifier.fillMaxSize()) { BrowseTopBar(c, LayoutClass.PHONE) } } }
    }

    /** Puzzles [unsolved] (0-based) are New, all others Solved; [shown] is open. */
    private fun controller(shown: Int, vararg unsolved: Int): BrowseController =
        DeviceKit.controller {
            for ((i, p) in puzzles.withIndex()) if (i !in unsolved) seed(p.id, DeviceKit.solved())
            seedLastShown(puzzles[shown].id)
        }.first

    private fun assertShows(index: Int) {
        rule.onNodeWithTag("puzzle-title").assertTextEquals(titleOf(puzzles[index]))
        rule.onNodeWithTag("puzzle-counter").assertTextEquals("${index + 1} / $n")
    }

    private fun hold(holdMs: Long) {
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("next-button").performTouchInput { down(center) }
        rule.mainClock.advanceTimeBy(holdMs)
        rule.onNodeWithTag("next-button").performTouchInput { up() }
        rule.mainClock.advanceTimeBy(300)
        rule.mainClock.autoAdvance = true
    }

    // REQ-050.A1 - "With puzzles 3 and 7 unsolved and puzzle 3 open, a long press on › opens puzzle 7."
    // Down, advance the test clock past 500 ms, up. Puzzle 3 is index 2 and puzzle 7 is index 6.
    @Test
    fun req050_A1_aLongPressOnNextWithPuzzles3And7UnsolvedOpensPuzzle7() {
        show(controller(2, 2, 6))
        assertShows(2)
        hold(650)
        assertShows(6)
    }

    // REQ-050.A1 - a shorter press is a normal next: it lands on puzzle 4, not on the unsolved puzzle 7.
    @Test
    fun req050_A1_aShortPressIsANormalNextNotAJumpToTheUnsolvedPuzzle() {
        show(controller(2, 2, 6))
        hold(120)
        assertShows(3)
    }

    // REQ-050.A1 - at least 500 ms: a hold just under it does not jump.
    @Test
    fun req050_A1_aHoldUnderTheThresholdDoesNotJump() {
        show(controller(2, 2, 6))
        hold(400)
        assertShows(3)
    }

    // REQ-050.A1 - the jump wraps: from puzzle 7 the next unsolved is puzzle 3.
    @Test
    fun req050_A1_theLongPressWrapsToTheEarlierUnsolvedPuzzle() {
        show(controller(6, 2, 6))
        hold(650)
        assertShows(2)
    }
}
