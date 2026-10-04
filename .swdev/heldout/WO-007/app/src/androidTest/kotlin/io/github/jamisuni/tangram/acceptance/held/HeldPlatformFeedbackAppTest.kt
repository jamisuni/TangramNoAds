package io.github.jamisuni.tangram.acceptance.held

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.AndroidComposeUiFlags
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import io.github.jamisuni.tangram.FeedbackProbe
import io.github.jamisuni.tangram.PlatformFeedbackLever
import io.github.jamisuni.tangram.contracts.progress.GameSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * HELD-OUT (Test & Verify only): REQ-033 A1, the PLATFORM defaults (design WO-007 sections 3.6 item 4 and 3.7, DA-125). Own kit copies.
 *
 * Compose asks the platform for a click sound on every tap and for a haptic on a long press; the app's one root lever
 * (`PlatformFeedbackLever`) answers those requests itself and counts them. With sound OFF and again with sound ON ("suppressed always"):
 * a tap on each control raises `interceptedClicks` by one (the positive control: the node DID ask for a click and the lever, not the
 * platform, answered), a long press on the next button raises `interceptedHaptics`, and none of it reaches `FeedbackProbe` (our own
 * cues). `AndroidComposeUiFlags.isInteractionSoundEffectsEnabled` reads false (the keyboard-focus sounds). Measured: the lever's
 * interception counters and the flag. Not measured: the platform's own `playSoundEffect`, which has no external observer (design 3.6).
 * Controls tapped: previous, next, the counter, the gear, Done, the sound switch, Reset, Keep, Restart, Retry, Next, a grid cell, grid Done.
 */
@OptIn(ExperimentalComposeUiApi::class)
class HeldPlatformFeedbackAppTest {
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(TestConfigRule()).around(ResetStoreRule()).around(compose)

    private val puzzles = Seed.puzzles

    private fun settle(ms: Long = 300) = compose.mainClock.advanceTimeBy(ms)

    /** One tap on [tag]: the lever's click counter must rise by exactly one (the node asked, the lever answered). */
    private fun tapCounted(tag: String, label: String) {
        val before = PlatformFeedbackLever.interceptedClicks
        compose.touch(tag)
        compose.waitForIdle()
        assertEquals("$label ($tag): the lever did not intercept exactly one click request", before + 1, PlatformFeedbackLever.interceptedClicks)
    }

    private fun scrollThenTapCounted(tag: String, label: String) {
        PlayerControlWalk.scrollIntoView(compose, tag) // only inside the scroll container: Done is in the fixed header
        tapCounted(tag, label)
    }

    private fun assertNoOwnCue(phase: String) {
        assertEquals("$phase: our own sound output was asked: ${FeedbackProbe.soundCounts}", 0, FeedbackProbe.soundCounts.values.sum())
        assertEquals("$phase: our own haptic output was asked", 0, FeedbackProbe.ticks)
    }

    private fun seed(soundOn: Boolean, screen: WalkScreen) {
        Seed.screen(screen)
        val store = AppStore.open()
        store.saveSettings(GameSettings(timerShown = false, soundOn = soundOn))
    }

    private fun phase(soundOn: Boolean) {
        val tag = if (soundOn) "sound ON" else "sound OFF"
        FeedbackProbe.reset()

        // the process flag of the focus sounds
        assertFalse("$tag: the focus-sound flag is still on", AndroidComposeUiFlags.isInteractionSoundEffectsEnabled)

        // 1. a solved puzzle: Next (the following puzzle), back to it, Retry
        seed(soundOn, WalkScreen.SOLVED)
        AppLaunch.launch("en-US").use {
            compose.waitForIdle()
            tapCounted("solved-next-button", "$tag Next")
            tapCounted("prev-button", "$tag previous")
            tapCounted("retry-button", "$tag Retry")
        }

        // 2. an in-progress puzzle: Restart
        seed(soundOn, WalkScreen.IN_PROGRESS)
        AppLaunch.launch("en-US").use {
            compose.waitForIdle()
            tapCounted("restart-button", "$tag Restart")
        }

        // 3. a new puzzle: browse, the grid, the settings screen with its controls, the long press
        seed(soundOn, WalkScreen.NEW)
        AppLaunch.launch("en-US").use {
            compose.waitForIdle()
            tapCounted("next-button", "$tag next")
            tapCounted("prev-button", "$tag previous")
            tapCounted("puzzle-counter", "$tag counter (opens the grid)")
            tapCounted("grid-cell-${puzzles[1].id.value}", "$tag grid cell")
            tapCounted("puzzle-counter", "$tag counter again")
            tapCounted("grid-close", "$tag grid Done")

            tapCounted("settings-button", "$tag gear")
            compose.onNodeWithTag("settings-overlay").assertExists()
            if (soundOn) compose.onNodeWithTag("settings-sound").assertIsOn() else compose.onNodeWithTag("settings-sound").assertIsOff()
            scrollThenTapCounted("settings-sound", "$tag sound switch (off or on)")
            scrollThenTapCounted("settings-sound", "$tag sound switch (back)")
            if (soundOn) compose.onNodeWithTag("settings-sound").assertIsOn() else compose.onNodeWithTag("settings-sound").assertIsOff()
            scrollThenTapCounted("settings-reset", "$tag Reset")
            scrollThenTapCounted("settings-reset-cancel", "$tag Keep")
            scrollThenTapCounted("settings-close", "$tag Done")
            compose.onNodeWithTag("settings-overlay").assertDoesNotExist()

            // the long press on the next button: the lever answers the haptic
            val hapticsBefore = PlatformFeedbackLever.interceptedHaptics
            compose.hold("next-button", 700)
            compose.waitForIdle()
            assertTrue("$tag: the long press on next raised no haptic request (before $hapticsBefore, after ${PlatformFeedbackLever.interceptedHaptics})", PlatformFeedbackLever.interceptedHaptics > hapticsBefore)
        }

        // none of this reached our own outputs
        assertNoOwnCue(tag)
    }

    // REQ-033.A1 - "With sound off, no action makes a sound or a haptic tick."
    // The platform's click and long-press haptic are intercepted by the lever with sound OFF and with sound ON, and our own cues are not asked.
    @Test
    fun req033_A1_withSoundOffNoControlAsksThePlatformForAClickOrAHaptic() = phase(soundOn = false)

    // REQ-033.A1 - the same with sound ON: the platform defaults are suppressed always (DA-125), not only when the switch is off.
    @Test
    fun req033_A1_theSamePlatformDefaultsAreSuppressedWithSoundOnToo() = phase(soundOn = true)
}
