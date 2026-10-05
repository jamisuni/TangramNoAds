package io.github.jamisuni.tangram.acceptance.promise

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.FeedbackProbe
import io.github.jamisuni.tangram.acceptance.AppStore
import io.github.jamisuni.tangram.acceptance.ResetStoreRule
import io.github.jamisuni.tangram.acceptance.TouchRig
import io.github.jamisuni.tangram.acceptance.layout.AirplaneMode
import io.github.jamisuni.tangram.acceptance.layout.AppLaunch
import io.github.jamisuni.tangram.acceptance.layout.ManualTimeSource
import io.github.jamisuni.tangram.acceptance.layout.WalkScreen
import io.github.jamisuni.tangram.acceptance.layout.advanceActive
import io.github.jamisuni.tangram.acceptance.layout.ScreenWalk
import io.github.jamisuni.tangram.acceptance.layout.Seed
import io.github.jamisuni.tangram.acceptance.layout.TestConfigRule
import io.github.jamisuni.tangram.acceptance.layout.solveByTouch
import io.github.jamisuni.tangram.acceptance.touch
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.settings.FeedbackEvent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * Airplane mode on the real app (design WO-006 section 6 item 5, DA-105). The test turns airplane mode on itself and asserts the flag, plays
 * every feature that exists now, and restores the SAVED PRIOR state in `after`. The claim itself rests on the absence of the INTERNET
 * permission (the installed-permissions test and V-08): with it Android refuses every socket, so airplane mode cannot change what the app
 * does; this run shows the device behaviour. On API 26 airplane mode is "flag set and data off" (Wi-Fi cannot be cut on that image; logged).
 * Since WO-007 (C7) the settings are in it: sound off and on, a lock with its tick request, and the reset with its confirmation; the timer
 * and play time are in it since WO-008 (C7): the timer on, a piece placed, the clock advanced, today and total shown, a solve, the best shown.
 */
class AirplaneModeAppTest {
    private val compose = createEmptyComposeRule()
    private val airplane = AirplaneMode()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(ResetStoreRule()).around(TestConfigRule()).around(compose)

    @After
    fun restoreAirplane() = airplane.restore()

    private val puzzles = Seed.puzzles
    private val n = puzzles.size
    private val en = ScreenWalk.bundle(InstrumentationRegistry.getInstrumentation().targetContext, "en-US").getValue("browse")

    // REQ-010.A1 - "With the device in airplane mode, every feature works."
    // Airplane mode on (flag asserted), then the player's flow: a piece is dragged to its solution place, > and < keep it, the grid opens
    // another puzzle, Restart empties the started puzzle, a relaunch (recreate) brings the progress back, and the first puzzle is solved.
    @Test
    fun req010_A1_everyFeatureThatExistsWorksInAirplaneMode() {
        airplane.on()
        assertTrue("fixture: airplane mode is on", airplane.isOn())
        val first = puzzles.first()
        AppLaunch.launch("en-US").use { scenario ->
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(300)

            // a piece by real touch, kept across > and <
            val rig = TouchRig(compose)
            val placed = rig.placePieces(first, 1).single()
            compose.touch("next-button")
            compose.touch("prev-button")
            compose.mainClock.advanceTimeBy(600)
            val shot = rig.shot()
            val m = rig.mapper(shot, rig.board(), first)
            assertTrue("$placed is still on the board in its place", rig.pieceIsOnBoardAtSolution(shot, m, first, placed))

            // the grid opens another puzzle, and back
            compose.touch("puzzle-counter")
            compose.touch("grid-cell-${puzzles[1].id.value}")
            compose.mainClock.advanceTimeBy(600)
            compose.onNodeWithTag("puzzle-counter").assertTextEquals("2 / $n")
            compose.touch("prev-button")
            compose.mainClock.advanceTimeBy(600)
            compose.onNodeWithTag("puzzle-counter").assertTextEquals("1 / $n")

            // a relaunch (recreate) brings the progress back
            scenario.recreate()
            compose.mainClock.advanceTimeBy(600)
            val rig2 = TouchRig(compose)
            val shot2 = rig2.shot()
            val m2 = rig2.mapper(shot2, rig2.board(), first)
            assertTrue("$placed came back after the relaunch", rig2.pieceIsOnBoardAtSolution(shot2, m2, first, placed))

            // Restart empties the started puzzle
            compose.touch("restart-button")
            compose.mainClock.advanceTimeBy(600)
            compose.onNodeWithTag("puzzle-state").assertTextEquals(en.getValue("state_new"))

            // the first puzzle is solved
            TouchRig(compose).solveByTouch(first)
            compose.mainClock.advanceTimeBy(3000)
            compose.onNodeWithTag("solved-bar").assertExists()
            compose.onNodeWithTag("puzzle-state").assertTextEquals(en.getValue("state_solved"))
        }
    }

    // REQ-010.A1 - "With the device in airplane mode, every feature works." (C7, design WO-007 section 7: sound, haptic and reset)
    // Airplane mode on (flag asserted); the sound switch is on by default and a lock by real touch asks for its sound and its haptic tick
    // (the debug probe counts the requests at the output boundary, a positive control); the switch is turned off by the real settings
    // screen (state asserted, stored) and a lock then makes no request; it is turned on again; Reset then Keep leaves the progress,
    // Reset then Erase makes the puzzles New and keeps the sound setting. No crash on the way.
    @Test
    fun req010_A1_soundTheLockTickAndResetWorkInAirplaneMode() {
        airplane.on()
        assertTrue("fixture: airplane mode is on", airplane.isOn())
        val first = puzzles.first()
        val second = puzzles[1]
        AppLaunch.launch("en-US").use {
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(300)
            FeedbackProbe.reset()

            TouchRig(compose).placePieces(first, 1)
            compose.mainClock.advanceTimeBy(600)
            assertTrue("sound on: the lock asked for its sound (${FeedbackProbe.soundCounts})", (FeedbackProbe.soundCounts[FeedbackEvent.LOCK] ?: 0) >= 1)
            assertTrue("sound on: the lock asked for its haptic tick", FeedbackProbe.ticks >= 1)

            // sound off through the real settings screen
            compose.touch("settings-button")
            compose.onNodeWithTag("settings-overlay").assertExists()
            compose.onNodeWithTag("settings-sound").assertIsOn()
            compose.touch("settings-sound")
            compose.onNodeWithTag("settings-sound").assertIsOff()
            assertFalse("the off switch is stored", AppStore.open().settings().soundOn)
            compose.touch("settings-close")
            compose.onNodeWithTag("settings-overlay").assertDoesNotExist()

            // a lock with sound off: it really locks (placePieces checks) and makes no request
            compose.touch("next-button")
            compose.mainClock.advanceTimeBy(600)
            FeedbackProbe.reset()
            TouchRig(compose).placePieces(second, 1)
            compose.mainClock.advanceTimeBy(600)
            assertEquals("sound off: a sound request reached the output: ${FeedbackProbe.soundCounts}", 0, FeedbackProbe.soundCounts.values.sum())
            assertEquals("sound off: a haptic tick was requested", 0, FeedbackProbe.ticks)

            compose.mainClock.autoAdvance = true
            compose.touch("settings-button")
            compose.touch("settings-sound")
            compose.onNodeWithTag("settings-sound").assertIsOn()
            assertTrue("the on switch is stored", AppStore.open().settings().soundOn)

            // Reset, then Keep: nothing changes
            compose.onNodeWithTag("settings-reset").performScrollTo()
            compose.touch("settings-reset")
            compose.onNodeWithTag("settings-reset-confirm").assertExists()
            compose.touch("settings-reset-cancel")
            compose.onNodeWithTag("settings-reset-confirm").assertDoesNotExist()
            assertEquals("Keep must change nothing", PuzzleState.IN_PROGRESS, AppStore.open().progress(second.id).state)

            // Reset, then Erase: every puzzle New, the settings kept
            compose.onNodeWithTag("settings-reset").performScrollTo()
            compose.touch("settings-reset")
            compose.onNodeWithTag("settings-reset-confirm").performScrollTo()
            compose.touch("settings-reset-confirm")
            compose.waitForIdle()
            val store = AppStore.open()
            assertEquals(PuzzleState.NEW, store.progress(first.id).state)
            assertEquals(PuzzleState.NEW, store.progress(second.id).state)
            assertTrue("the sound setting is kept by a reset", store.settings().soundOn)
            compose.onNodeWithTag("settings-sound").assertIsOn()
            compose.touch("settings-close")
            compose.onNodeWithTag("settings-overlay").assertDoesNotExist()
        }
    }
    // REQ-010.A1 - "With the device in airplane mode, every feature works." (C7, design WO-008 "Carried parts": play time and best times)
    // Airplane mode on (flag asserted), the timer switched on in the store, a manual clock installed. Pieces are placed by real touch (the clock
    // is frozen, so touching credits nothing), the Compose clock is switched back on so the running gate has emitted, and 7 ACTIVE seconds pass
    // through `advanceActive` (never a sleep): the on-board pill reads 0:07 and the settings screen shows today's and the all-time play time. The
    // last piece is dropped by touch: the solved bar shows the best time, which is that first solve's time, 0:07. No crash on the way.
    @Test
    fun req010_A1_theTimerPlayTimeAndTheBestTimeWorkInAirplaneMode() {
        airplane.on()
        assertTrue("fixture: airplane mode is on", airplane.isOn())
        val first = puzzles.first()
        Seed.screen(WalkScreen.NEW, first)
        Seed.settings(timerShown = true)
        val clock = ManualTimeSource()
        AppLaunch.launch("en-US", timeSource = clock).use { scenario ->
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(300)
            val rig = TouchRig(compose)
            val placed = rig.placePieces(first, first.solution.size - 1)
            compose.mainClock.advanceTimeBy(600)
            compose.mainClock.autoAdvance = true // a paused Compose clock never emits the running gate: switch it on before any time is asserted
            compose.waitForIdle()

            scenario.advanceActive(compose, clock, 7_000)
            assertEquals("the on-board timer shows the 7 active seconds", "0:07", ScreenWalk.textOf(compose, "puzzle-timer"))

            compose.touch("settings-button")
            compose.onNodeWithTag("settings-overlay").assertExists()
            assertTrue("today's play time is shown: ${ScreenWalk.textOf(compose, "settings-play-today")}", "0:07" in ScreenWalk.textOf(compose, "settings-play-today"))
            assertTrue("the all-time play time is shown: ${ScreenWalk.textOf(compose, "settings-play-total")}", "0:07" in ScreenWalk.textOf(compose, "settings-play-total"))
            compose.touch("settings-close")
            compose.onNodeWithTag("settings-overlay").assertDoesNotExist()

            // the last piece by touch: the picture replaces the piece colours, so its drop is not judged (SolveByTouch.kt header)
            compose.mainClock.autoAdvance = false
            val last = first.solution.map { it.piece }.first { it !in placed }
            rig.tryPlace(first, last)
            compose.mainClock.advanceTimeBy(3000)
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
            compose.onNodeWithTag("solved-bar").assertExists()
            assertEquals("the best time is the first solve's 7 s", "0:07", ScreenWalk.textOf(compose, "best-time"))
            assertEquals(7L, AppStore.open().progress(first.id).bestSeconds)
        }
    }
}
