package io.github.jamisuni.tangram.acceptance.layout

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.IntSize
import io.github.jamisuni.tangram.acceptance.AppStore
import io.github.jamisuni.tangram.acceptance.solutionSave
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.play.BoardTransform

// Scaffolding for the WO-006 device tests (decision DA-99 / DA-107); no requirement token. Seeds a screen state through the app's
// real store (design "Seeding recipe": write after the rule wiped and before the activity launches), and reads the tray.

/**
 * The screen states the walks visit (design WO-006 section 4: New, In progress with Restart, Solved, the grid; design WO-007 section 7: the
 * settings screen and its reset confirmation, reached by real touch from a New puzzle).
 */
internal enum class WalkScreen { NEW, IN_PROGRESS, SOLVED, GRID, SETTINGS, SETTINGS_RESET_CONFIRM }

/** The two settings states: the base screen is cleared from the semantics tree while they are open (design WO-007 section 2). */
internal val WalkScreen.isSettings: Boolean get() = this == WalkScreen.SETTINGS || this == WalkScreen.SETTINGS_RESET_CONFIRM

internal object Seed {
    val puzzles: List<Puzzle> get() = PuzzleLibrary.packaged().puzzles

    /** A full-set puzzle (seven pieces: the fresh-install puzzle is a 3-piece mini): the Cat of REQ-047 A1. */
    fun fullPuzzle(): Puzzle {
        val cat = puzzles.firstOrNull { it.id.value == "animals-cat" } ?: error("the library has no animals-cat puzzle")
        if (cat.solution.size != 7) error("fixture: animals-cat is not a full-set puzzle (${cat.solution.size} pieces)")
        return cat
    }

    /** Wipes the store and writes the state for [screen] of [puzzle]; the puzzle becomes the last shown one. */
    fun screen(screen: WalkScreen, puzzle: Puzzle = fullPuzzle()) {
        AppStore.wipe()
        val store = AppStore.open()
        when (screen) {
            WalkScreen.NEW, WalkScreen.GRID, WalkScreen.SETTINGS, WalkScreen.SETTINGS_RESET_CONFIRM -> Unit
            WalkScreen.IN_PROGRESS -> {
                val three = puzzle.solution.map { it.piece }.take(3)
                store.saveProgress(puzzle.id, PuzzleProgress(PuzzleState.IN_PROGRESS, three.associateWith { solutionSave(puzzle, it) }, 12, null))
            }
            WalkScreen.SOLVED -> store.saveProgress(puzzle.id, PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 47, 41))
        }
        store.saveLastShownPuzzle(puzzle.id)
    }

    private fun tap(rule: ComposeTestRule, tag: String) {
        rule.onNodeWithTag(tag).performTouchInput { click() }
        rule.waitForIdle()
    }

    /**
     * After the activity is up: reaches [screen] by REAL touch (WO-006: the grid by the counter; WO-007: settings by the gear, the reset
     * confirmation by the reset button after scrolling to it). The other screens need nothing. Loud when the target did not appear, so a
     * walk can never read the wrong screen.
     */
    fun reach(rule: ComposeTestRule, screen: WalkScreen) {
        rule.waitForIdle()
        when (screen) {
            WalkScreen.GRID -> tap(rule, "puzzle-counter")
            WalkScreen.SETTINGS -> {
                tap(rule, "settings-button")
                rule.onNodeWithTag("settings-overlay").assertExists()
            }
            WalkScreen.SETTINGS_RESET_CONFIRM -> {
                tap(rule, "settings-button")
                rule.onNodeWithTag("settings-overlay").assertExists()
                PlayerControlWalk.scrollIntoView(rule, "settings-reset")
                tap(rule, "settings-reset")
                rule.onNodeWithTag("settings-reset-confirm").assertExists()
            }
            else -> Unit
        }
        rule.waitForIdle()
    }
}

/** The tray as the product reports it: cell rects in px relative to the `play-area` origin (the public BoardTransform semantics). */
internal class TrayRead(val areaSize: IntSize, val cells: Map<PieceId, Rect>, val areaLeftInWindow: Float, val areaTopInWindow: Float)

internal fun ComposeTestRule.readTray(): TrayRead {
    val area: SemanticsNode = onNodeWithTag("play-area").fetchSemanticsNode()
    val board = onNodeWithTag("board").fetchSemanticsNode()
    val t = board.config.getOrNull(BoardTransform) ?: error("the board node carries no BoardTransform")
    if (t.trayCellsPx.isEmpty()) error("the tray is empty: the puzzle is solved or has no pieces")
    return TrayRead(area.size, t.trayCellsPx, area.boundsInWindow.left, area.boundsInWindow.top)
}

/** True when the node declares a scroll action (design section 2: no node on the play screen may). */
internal fun SemanticsNode.declaresScroll(): Boolean =
    config.contains(SemanticsActions.ScrollBy)
