package io.github.jamisuni.tangram.acceptance.held

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.play.BoardTransform
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * HELD-OUT (Test & Verify only): REQ-030 A1 and A3 on the real app (design WO-008 Acceptance IDs table). Own use of the held kit.
 *
 * Seeded through the real store: a fresh install with the timer switched ON (so the puzzle's time is on screen), the first (3-piece) puzzle.
 * A piece is placed by real touch; time is moved only by `advanceActive` (never a sleep). "Touched" time is made of real touches on plain text
 * (a contact, no control) every 30 s, so the idle window never ends the count for a reason other than the pause under test.
 */
class HeldPuzzleTimeAppTest {
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(TestConfigRule()).around(ResetStoreRule()).around(compose)

    private val puzzle = Seed.puzzles.first()
    private val en = ScreenWalk.bundle(InstrumentationRegistry.getInstrumentation().targetContext, "en-US").getValue("browse")

    private fun seed() {
        Seed.screen(WalkScreen.NEW, puzzle)
        Seed.settings(timerShown = true)
    }

    private fun pill() = ScreenWalk.textOf(compose, "puzzle-timer")

    /** Places [n] pieces by real touch (the Compose clock is paused for that), then gives the clock back and lets the running gate emit. */
    private fun place(n: Int) {
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(300)
        TouchRig(compose).placePieces(puzzle, n)
        compose.mainClock.advanceTimeBy(600)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
    }

    /** `ms` of active time on the shown screen: a real touch on [touchOn] every 30 s (down and cancel on a scroll node is a contact, not a click). */
    private fun touchedFor(scenario: androidx.test.core.app.ActivityScenario<io.github.jamisuni.tangram.LocaleOverrideActivity>, clock: ManualTimeSource, ms: Long, touch: () -> Unit) {
        var left = ms
        while (left > 0) {
            touch()
            val step = minOf(30_000L, left)
            scenario.advanceActive(compose, clock, step)
            left -= step
        }
    }

    private fun touchPlayText() = compose.onNodeWithTag("puzzle-state").performTouchInput { click() }

    private fun touchSettingsText() {
        compose.onNodeWithTag("settings-free-note").performScrollTo()
        compose.onNodeWithTag("settings-free-note").performTouchInput { click() }
    }

    private fun touchGridList() = compose.onNode(hasScrollAction()).performTouchInput { down(center); cancel() }

    // REQ-030.A1 - "Browsing away for 2 minutes adds nothing to the puzzle's time."
    // One piece is down and 5 active seconds are counted (the timer reads 0:05). Then the player is away for 2 minutes of TOUCHED time, three
    // ways in turn: the settings screen, another puzzle (> and <), the all-puzzles grid. Each time the timer, once back, still reads 0:05; and
    // the play-time total shows the 2 minutes did pass (so "nothing added" is not "the clock stopped").
    @Test
    fun browsingAwayForTwoMinutesAddsNothingToThePuzzlesTime() {
        seed()
        val clock = ManualTimeSource()
        AppLaunch.launch("en-US", timeSource = clock).use { scenario ->
            compose.waitForIdle()
            place(1)
            scenario.advanceActive(compose, clock, 5_000)
            assertEquals("fixture: 5 active seconds on the puzzle", "0:05", pill())

            // (1) the settings screen
            compose.touch("settings-button")
            compose.onNodeWithTag("settings-overlay").assertExists()
            val totalBefore = totalInSettings()
            touchedFor(scenario, clock, 120_000, ::touchSettingsText)
            assertEquals("the 2 minutes did pass for play time", totalBefore + 120, totalInSettings())
            compose.touch("settings-close")
            compose.waitForIdle()
            assertEquals("after the settings screen: the puzzle's time did not move", "0:05", pill())

            // (2) another puzzle, and back
            compose.touch("next-button")
            compose.waitForIdle()
            touchedFor(scenario, clock, 120_000, ::touchPlayText)
            compose.touch("prev-button")
            compose.waitForIdle()
            assertEquals("after another puzzle: the puzzle's time did not move", "0:05", pill())

            // (3) the grid
            compose.touch("puzzle-counter")
            compose.waitForIdle()
            touchedFor(scenario, clock, 120_000, ::touchGridList)
            compose.touch("grid-close")
            compose.waitForIdle()
            assertEquals("after the grid: the puzzle's time did not move", "0:05", pill())

            // and it resumes on return
            touchPlayText()
            scenario.advanceActive(compose, clock, 4_000)
            assertEquals("it counts again on return", "0:09", pill())
        }
    }

    private fun totalInSettings(): Long {
        val text = ScreenWalk.textOf(compose, "settings-play-total")
        val m = Regex("(\\d+):(\\d{2})").findAll(text).lastOrNull() ?: error("no m:ss in the all-time row: \"$text\"")
        return m.groupValues[1].toLong() * 60 + m.groupValues[2].toLong()
    }

    /** One tap (down, 30 ms, up) on the tray cell of [piece] with the Compose clock paused: the tap that turns a tray piece. */
    private fun tapTrayPiece(piece: PieceId) {
        val t = compose.onNodeWithTag("board").fetchSemanticsNode().config.getOrNull(BoardTransform) ?: error("the board node carries no BoardTransform")
        val cell = t.trayCellsPx[piece] ?: error("no tray cell for $piece")
        val at: Offset = cell.center
        compose.onNodeWithTag("play-area").performTouchInput { down(at) }
        compose.mainClock.advanceTimeBy(30)
        compose.onNodeWithTag("play-area").performTouchInput { up() }
        compose.mainClock.advanceTimeBy(50)
    }

    // REQ-030.A3 - "Turning a tray piece on a New puzzle starts no time."
    // A New puzzle with the timer on: tray pieces are tapped (turned) three times; half a minute of active time passes; the puzzle still reads New
    // and the timer reads 0:00. Then ONE piece is dragged out: the puzzle is In progress and, after 5 active seconds, the timer reads 0:05.
    @Test
    fun turningATrayPieceOnANewPuzzleStartsNoTimeAndTheFirstDragDoes() {
        seed()
        val clock = ManualTimeSource()
        AppLaunch.launch("en-US", timeSource = clock).use { scenario ->
            compose.waitForIdle()
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(300)
            val piece = puzzle.solution.first().piece
            repeat(3) { tapTrayPiece(piece) }
            compose.mainClock.advanceTimeBy(600)
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
            compose.onNodeWithTag("puzzle-state").assertTextEquals(en.getValue("state_new"))
            scenario.advanceActive(compose, clock, 30_000)
            assertEquals("a turned tray piece started no time", "0:00", pill())
            compose.onNodeWithTag("puzzle-state").assertTextEquals(en.getValue("state_new"))

            place(1) // the first drag
            compose.onNodeWithTag("puzzle-state").assertTextEquals(en.getValue("state_in_progress"))
            scenario.advanceActive(compose, clock, 5_000)
            assertEquals("the first drag started the time", "0:05", pill())
        }
    }
}
