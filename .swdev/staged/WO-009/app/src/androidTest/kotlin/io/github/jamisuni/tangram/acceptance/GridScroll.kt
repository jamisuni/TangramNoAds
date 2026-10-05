package io.github.jamisuni.tangram.acceptance

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performScrollToNode

// WO-009 T9d (DA-163), scaffolding, no requirement token. The all-puzzles grid is a LAZY grid: only the cells near the window are composed,
// and the window starts at the row of the puzzle that is shown. With 13 puzzles every cell was composed on every phone; with 25 a cell
// can sit outside the window, so a test that looks for or touches a cell scrolls to it first. The pattern is the one `ScreenWalk` already
// uses (`hasScrollAction()`: the grid is the only scroll container while the overlay is open). A cell that is already composed is left
// where it is. With a paused test clock the scroll needs a frame to apply, so the clock is moved a little; with a running clock the
// test waits for idle. A cell that is not in the grid at all still fails loudly (`performScrollToNode` finds no such node).

/** Scrolls the open all-puzzles grid so that the cell with [tag] (`grid-cell-<id>`) is composed; a no-op when it already is. */
internal fun ComposeTestRule.scrollGridTo(tag: String) {
    if (onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()) return
    onNode(hasScrollAction()).performScrollToNode(hasTestTag(tag))
    if (mainClock.autoAdvance) waitForIdle() else mainClock.advanceTimeBy(300)
}
