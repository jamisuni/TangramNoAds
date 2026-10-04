package io.github.jamisuni.tangram.acceptance.held

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.LocaleOverrideActivity
import io.github.jamisuni.tangram.contracts.progress.GameSettings
import io.github.jamisuni.tangram.contracts.progress.PlayTime
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import java.time.LocalDate

/**
 * HELD-OUT (Test & Verify only): REQ-034 A1 and A2 on the real app, end to end (design WO-007 acceptance table). Own kit copies.
 *
 * Seeded through the real store: a solved puzzle with best and puzzle times, the Cat in progress with three pieces on the board (the live
 * board), play time, and SOUND OFF with the timer setting on. A1: Reset then Erase through the real settings screen: the SAME puzzle now
 * reads New (top bar, every piece back in the tray), EVERY grid cell reads New, the store re-read through a FRESH `JsonProgressStore`
 * is fresh (puzzles, play time), sound is still off (settings kept, REQ-034 rule); then the activity is CLOSED and launched again (a new
 * ViewModel re-reads the store): still New, sound still off. A2: Reset then Keep, and Reset then Back, change nothing.
 */
class HeldResetAppTest {
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(TestConfigRule()).around(ResetStoreRule()).around(compose)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val browse = ScreenWalk.bundle(context, "en-US").getValue("browse")
    private val puzzles = Seed.puzzles
    private val cat = Seed.fullPuzzle()
    private val solvedPuzzle = puzzles.first { it.id != cat.id }
    private val kept = GameSettings(timerShown = true, soundOn = false)
    private val playTime = PlayTime(LocalDate.of(2026, 10, 4), 125, 4_000)

    private fun seed() {
        Seed.screen(WalkScreen.IN_PROGRESS, cat) // wipes; the Cat in progress with three pieces; the Cat is the last shown puzzle
        val store = AppStore.open()
        store.saveProgress(solvedPuzzle.id, PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 52, 44))
        store.savePlayTime(playTime)
        store.saveSettings(kept)
        store.saveLastShownPuzzle(cat.id)
    }

    private fun openReset() {
        compose.touch("settings-button")
        compose.onNodeWithTag("settings-overlay").assertExists()
        compose.onNodeWithTag("settings-sound").assertIsOff() // fixture: sound starts off
        compose.onNodeWithTag("settings-reset").performScrollTo()
        compose.touch("settings-reset")
        compose.onNodeWithTag("settings-reset-confirm").assertExists()
    }

    private fun counterText() = ScreenWalk.textOf(compose, "puzzle-counter")

    private fun gridStates(): Map<String, String> {
        compose.touch("puzzle-counter")
        val cells = ScreenWalk.gridDescriptions(compose, puzzles.map { it.id.value })
        val out = cells.associate { it.id to it.state }
        compose.touch("grid-close")
        compose.waitForIdle()
        return out
    }

    private fun placedPiecesOnBoard(): List<io.github.jamisuni.tangram.kernel.model.PieceId> {
        val rig = TouchRig(compose)
        val shot = rig.shot()
        val m = rig.mapper(shot, rig.board(), cat)
        return cat.solution.map { it.piece }.filter { rig.pieceIsOnBoardAtSolution(shot, m, cat, it) }
    }

    private fun allInTray(): Boolean {
        val rig = TouchRig(compose)
        val shot = rig.shot()
        val board = rig.board()
        return cat.solution.map { it.piece }.all { rig.inTray(shot, board, it) }
    }

    private fun assertStoreIsFreshAndSettingsKept(what: String) {
        val fresh = AppStore.open() // a store instance that has read the file anew
        for (p in puzzles) assertEquals("$what: ${p.id.value} in the stored file", PuzzleProgress.NEW, fresh.progress(p.id))
        assertEquals("$what: stored play time", PlayTime.NONE, fresh.playTime())
        assertEquals("$what: the settings are kept", kept, fresh.settings())
        assertEquals("$what: the last shown puzzle is kept", cat.id, fresh.lastShownPuzzle())
    }

    // REQ-034.A1 - "After a confirmed reset, every puzzle shows as New."
    @Test
    fun req034_A1_afterAConfirmedResetEveryPuzzleShowsAsNewEvenAfterARelaunch() {
        seed()
        var scenario: ActivityScenario<LocaleOverrideActivity> = AppLaunch.launch("en-US")
        try {
            compose.waitForIdle()
            // the live board: three pieces on the board, in progress
            compose.onNodeWithTag("puzzle-state").assertTextEquals(browse.getValue("state_in_progress"))
            val counterBefore = counterText()
            assertEquals("fixture: three pieces are on the live board", 3, placedPiecesOnBoard().size)
            val gridBefore = gridStates()
            assertEquals("fixture: the solved puzzle shows Solved", browse.getValue("state_solved"), gridBefore.getValue(solvedPuzzle.id.value))
            assertEquals("fixture: the Cat shows In progress", browse.getValue("state_in_progress"), gridBefore.getValue(cat.id.value))

            openReset()
            compose.onNodeWithTag("settings-reset-confirm").performScrollTo()
            compose.touch("settings-reset-confirm") // Erase
            compose.waitForIdle()
            compose.touch("settings-close")
            compose.waitForIdle()
            compose.onNodeWithTag("settings-overlay").assertDoesNotExist()

            // the same puzzle, now New, every piece in the tray
            assertEquals("the same puzzle stays shown", counterBefore, counterText())
            compose.onNodeWithTag("puzzle-state").assertTextEquals(browse.getValue("state_new"))
            assertTrue("a piece is still on the board after the reset", allInTray())
            // every grid cell New
            val gridAfter = gridStates()
            assertEquals("every puzzle of the library has a cell", puzzles.size, gridAfter.size)
            for ((id, state) in gridAfter) assertEquals("grid cell $id", browse.getValue("state_new"), state)
            // the store, re-read fresh, is fresh; the sound setting survived
            assertStoreIsFreshAndSettingsKept("after Erase")

            // close and relaunch the activity: a new ViewModel re-reads the store
            scenario.close()
            scenario = AppLaunch.launch("en-US")
            compose.waitForIdle()
            assertEquals("after the relaunch: the same puzzle", counterBefore, counterText())
            compose.onNodeWithTag("puzzle-state").assertTextEquals(browse.getValue("state_new"))
            assertTrue("after the relaunch: a piece is on the board", allInTray())
            for ((id, state) in gridStates()) assertEquals("after the relaunch: grid cell $id", browse.getValue("state_new"), state)
            compose.touch("settings-button")
            compose.onNodeWithTag("settings-sound").assertIsOff() // sound still off after a relaunch
            compose.touch("settings-close")
            assertStoreIsFreshAndSettingsKept("after the relaunch")
        } finally {
            scenario.close()
        }
    }

    private class Before(val counter: String, val grid: Map<String, String>, val placed: List<io.github.jamisuni.tangram.kernel.model.PieceId>)

    private fun readBefore() = Before(counterText(), gridStates(), placedPiecesOnBoard())

    private fun assertNothingChanged(what: String, before: Before, stored: Map<String, PuzzleProgress>) {
        compose.onNodeWithTag("puzzle-state").assertTextEquals(browse.getValue("state_in_progress"))
        assertEquals("$what: the counter", before.counter, counterText())
        assertEquals("$what: the pieces on the board", before.placed, placedPiecesOnBoard())
        assertEquals("$what: the grid", before.grid, gridStates())
        val fresh = AppStore.open()
        for ((id, p) in stored) {
            val now = fresh.progress(io.github.jamisuni.tangram.contracts.puzzle.PuzzleId(id))
            // state, pieces and best time: the running seconds of a live puzzle may legitimately be re-saved
            assertEquals("$what: the stored state of $id", p.state, now.state)
            assertEquals("$what: the stored pieces of $id", p.pieces, now.pieces)
            assertEquals("$what: the stored best time of $id", p.bestSeconds, now.bestSeconds)
        }
        assertEquals("$what: stored play time", playTime, fresh.playTime())
        assertEquals("$what: the settings", kept, fresh.settings())
    }

    // REQ-034.A2 - "Cancelling the confirmation changes nothing."
    // Reset then Keep, and Reset then Android Back: the board, the grid, the stored file, the play time and the settings are as they were,
    // and the question is gone when the screen is opened again.
    @Test
    fun req034_A2_keepAndBackChangeNothing() {
        seed()
        val storedBefore = (puzzles.map { it.id.value }).associateWith { AppStore.open().progress(io.github.jamisuni.tangram.contracts.puzzle.PuzzleId(it)) }
        assertTrue("fixture: something is stored", storedBefore.values.any { it != PuzzleProgress.NEW })
        AppLaunch.launch("en-US").use { scenario ->
            compose.waitForIdle()
            val before = readBefore()
            assertEquals("fixture: three pieces on the live board", 3, before.placed.size)

            // Keep
            openReset()
            compose.touch("settings-reset-cancel")
            compose.onNodeWithTag("settings-reset-confirm").assertDoesNotExist()
            compose.touch("settings-close")
            compose.waitForIdle()
            assertNothingChanged("after Keep", before, storedBefore)

            // Back during the question
            openReset()
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            compose.waitForIdle()
            compose.onNodeWithTag("settings-overlay").assertDoesNotExist()
            assertNothingChanged("after Back", before, storedBefore)

            // the cancelled question is gone
            compose.touch("settings-button")
            compose.onNodeWithTag("settings-reset-confirm").assertDoesNotExist()
            compose.touch("settings-close")
        }
    }
}
