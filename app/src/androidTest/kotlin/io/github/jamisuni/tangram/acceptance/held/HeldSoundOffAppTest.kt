package io.github.jamisuni.tangram.acceptance.held

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.FeedbackProbe
import io.github.jamisuni.tangram.settings.FeedbackEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * HELD-OUT (Test & Verify only): REQ-033 A1, our cues at the output boundary, end to end through the real app (design WO-007 section 3.6
 * item 4). Own kit copies (package `...acceptance.held`).
 *
 * `FeedbackProbe` (debug-only) counts per kind what reaches `SoundOut.play` and `HapticOut.tick` and passes it on. With sound ON, real touch
 * plays every kind of action (pick up, turn, lock, return, solve) and EACH kind is counted: the positive control, so the probe cannot be blind
 * and a zero below means "gated". The sound is then switched OFF through the real settings screen (the switch state and the stored
 * `soundOn == false` are asserted), the probe is reset and every action is repeated: after a settle both counts are 0. Switched on again,
 * requests resume.
 */
class HeldSoundOffAppTest {
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(TestConfigRule()).around(ResetStoreRule()).around(compose)

    private val puzzle = Seed.puzzles.first()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val en = ScreenWalk.bundle(context, "en-US").getValue("browse")

    private fun settle(ms: Long = 600) = compose.mainClock.advanceTimeBy(ms)

    private fun total(): Int = FeedbackProbe.soundCounts.values.sum()
    private fun count(e: FeedbackEvent): Int = FeedbackProbe.soundCounts[e] ?: 0

    private fun setSound(on: Boolean) {
        compose.touch("settings-button")
        compose.onNodeWithTag("settings-overlay").assertExists()
        val node = compose.onNodeWithTag("settings-sound")
        val currentlyOn = runCatching { node.assertIsOn() }.isSuccess
        if (currentlyOn != on) compose.touch("settings-sound")
        if (on) compose.onNodeWithTag("settings-sound").assertIsOn() else compose.onNodeWithTag("settings-sound").assertIsOff()
        assertEquals("the switch is stored", on, AppStore.open().settings().soundOn)
        compose.touch("settings-close")
        compose.onNodeWithTag("settings-overlay").assertDoesNotExist()
        settle()
    }

    private fun tap(at: Offset) {
        compose.onNodeWithTag("play-area").performTouchInput { down(at) }
        settle(30)
        compose.onNodeWithTag("play-area").performTouchInput { up() }
        settle(50)
    }

    /** Eight taps on one tray piece: eight turn requests, and the piece ends where it began (so a later placement is not disturbed). */
    private fun turnInTray(piece: io.github.jamisuni.tangram.kernel.model.PieceId) {
        val c = compose.readTray().cells.getValue(piece)
        repeat(8) { tap(Offset(c.left + 8f * density, c.bottom - 8f * density)) }
    }

    private val density: Float get() = context.resources.displayMetrics.density

    /** A drag of a tray piece onto the next tray cell and a release there: the piece goes back to its own cell (REQ-020). Returns the dragged piece. */
    private fun dragWithinTray(): io.github.jamisuni.tangram.kernel.model.PieceId {
        val tray = compose.readTray()
        val ids = puzzle.solution.map { it.piece }.filter { it in tray.cells }
        val a = ids[0]
        val b = ids[1]
        val from = Offset(tray.cells.getValue(a).left + 8f * density, tray.cells.getValue(a).bottom - 8f * density)
        val to = tray.cells.getValue(b).center
        compose.onNodeWithTag("play-area").performTouchInput { down(from) }
        settle(16)
        compose.onNodeWithTag("play-area").performTouchInput { moveTo(Offset(from.x + 40f * density, from.y)) }
        settle(150)
        compose.onNodeWithTag("play-area").performTouchInput { moveTo(to) }
        settle(250)
        compose.onNodeWithTag("play-area").performTouchInput { up() }
        settle(1200)
        return a
    }

    /** Every kind of action once: turn, pick up, return, lock (three locks), solve. Returns when the puzzle reads solved. */
    private fun playEveryKindOfAction() {
        val first = compose.readTray().cells.keys.first { it in puzzle.solution.map { s -> s.piece } }
        turnInTray(first)
        val dragged = dragWithinTray()
        val shot = TouchRig(compose)
        assertTrue("fixture: $dragged went back to the tray after the drop", shot.inTray(shot.shot(), shot.board(), dragged))
        // CORRECTED CHECK (T&V7, fixture step only; no assertion changed): the LAST locked piece solves the puzzle and the solved picture replaces
        // the piece colours, so tryPlace judges it false (SolveByTouch.kt header, WO-006 MOVE-DEV6). All but the last go through placePieces; the
        // last is dropped and not judged; the solved state is asserted below as before.
        val rig = TouchRig(compose)
        val placed = rig.placePieces(puzzle, puzzle.solution.size - 1)
        rig.tryPlace(puzzle, puzzle.solution.map { it.piece }.first { it !in placed })
        settle(3000)
        compose.onNodeWithTag("puzzle-state").assertTextEquals(en.getValue("state_solved"))
    }

    // REQ-033.A1 - "With sound off, no action makes a sound or a haptic tick."
    // ON: every kind is counted (positive control). OFF (through the real UI): the same actions, zero requests of any kind and no tick.
    // ON again: requests resume.
    @Test
    fun req033_A1_withSoundOffNoActionReachesTheSoundOrHapticOutput() {
        Seed.screen(WalkScreen.NEW, puzzle)
        AppLaunch.launch("en-US").use {
            compose.mainClock.autoAdvance = false
            settle(300)

            // ON (the default): the probe sees every kind, so it is not blind
            FeedbackProbe.reset()
            playEveryKindOfAction()
            for (e in FeedbackEvent.values()) assertTrue("sound on: the probe saw no $e request (${FeedbackProbe.soundCounts}); it would be blind", count(e) >= 1)
            assertTrue("sound on: no haptic tick for the locks", FeedbackProbe.ticks >= 1)

            // back to an empty puzzle, then sound OFF through the real settings screen
            compose.touch("retry-button")
            settle()
            compose.onNodeWithTag("puzzle-state").assertTextEquals(en.getValue("state_new"))
            setSound(false)

            FeedbackProbe.reset()
            playEveryKindOfAction() // the actions really happened (it asserts the tray drop and the solved state)
            settle(1500)
            assertEquals("sound off: requests reached the sound output: ${FeedbackProbe.soundCounts}", 0, total())
            for (e in FeedbackEvent.values()) assertEquals("sound off: $e", 0, count(e))
            assertEquals("sound off: a haptic tick was requested", 0, FeedbackProbe.ticks)

            // ON again: requests resume
            compose.touch("retry-button")
            settle()
            setSound(true)
            FeedbackProbe.reset()
            val c = compose.readTray().cells.entries.first()
            tap(Offset(c.value.left + 8f * density, c.value.bottom - 8f * density))
            assertTrue("sound on again: requests did not resume (${FeedbackProbe.soundCounts})", count(FeedbackEvent.TURN) >= 1)
            assertFalse("the probe counted nothing at all", total() == 0)
        }
    }
}
