package io.github.jamisuni.tangram.acceptance.held

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import io.github.jamisuni.tangram.MainActivity
import io.github.jamisuni.tangram.content.PuzzleLibrary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import java.util.Locale

/**
 * HELD-OUT (Test & Verify only): REQ-046.A1 entirely, on the real app. Self-contained (own rule, own helpers). Touch and text input
 * only; the store is wiped first. Written from REQ-046 and the frozen tags of design WO-005 "Test seams".
 */
class HeldPasscodeAppTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(HeldResetStoreRule()).around(compose)

    private val puzzles = PuzzleLibrary.packaged().puzzles

    private fun assertNothingRevealed(label: String) {
        compose.onNodeWithTag("dev-show-solution").assertDoesNotExist()
        compose.onNodeWithTag("dev-solve-now").assertDoesNotExist()
        assertEquals("$label: a solution shape is drawn", emptyList<String>(), compose.heldOverlayTags())
    }

    private fun assertWrongPasscodeShown(label: String) {
        compose.onNodeWithTag("dev-notice").assertTextEquals(heldString("devtools_wrong_passcode"))
        // REQ-046 rule 1 gives the English text verbatim; the app follows the device language, so check it where it applies.
        if (Locale.getDefault().language == "en") compose.onNodeWithTag("dev-notice").assertTextEquals("Wrong passcode.")
        assertNothingRevealed(label)
    }

    // REQ-046.A1 - "Entering 1234 is refused; entering 0417 unlocks the aid."  (first half)
    // Tap the DEV button, type 1234, OK: "Wrong passcode." is shown and nothing is revealed - no tools, no solution shape. Other
    // wrong tries (0000, an empty field) are refused the same way, and the dialog can be cancelled with the aid still locked.
    @Test
    fun req046_A1_aWrongPasscodeIsRefusedAndRevealsNothing() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForIdle()
            assertNothingRevealed("before the dialog")
            compose.heldOpenDialog()
            compose.onNodeWithTag("dev-passcode").assertExists()
            assertNothingRevealed("a freshly opened dialog")

            compose.heldEnter("1234")
            assertWrongPasscodeShown("1234")

            compose.heldEnter("0000")
            assertWrongPasscodeShown("0000")

            compose.heldTouch("dev-ok") // an empty field
            compose.waitForIdle()
            assertWrongPasscodeShown("an empty field")

            compose.heldTouch("dev-cancel")
            compose.waitForIdle()
            compose.onNodeWithTag("dev-dialog").assertDoesNotExist()
            assertNothingRevealed("after cancelling")

            compose.heldOpenDialog() // still locked: asked again
            compose.onNodeWithTag("dev-passcode").assertExists()
            assertNothingRevealed("a reopened dialog")
        }
    }

    // REQ-046.A1 - (second half) entering 0417 unlocks the aid: the tools are offered, and no refusal is shown.
    @Test
    fun req046_A1_0417UnlocksTheAid() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForIdle()
            compose.heldUnlock()
            compose.onNodeWithTag("dev-notice").assertDoesNotExist()
        }
    }

    // REQ-046.A1 - a refusal does not spoil the next, right try: 1234 is refused, then 0417 unlocks.
    @Test
    fun req046_A1_theRightPasscodeAfterAWrongOneUnlocks() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForIdle()
            compose.heldOpenDialog()
            compose.heldEnter("1234")
            assertWrongPasscodeShown("1234")
            compose.heldEnter(HELD_PASSCODE)
            compose.onNodeWithTag("dev-show-solution").assertExists()
            compose.onNodeWithTag("dev-solve-now").assertExists()
            compose.onNodeWithTag("dev-notice").assertDoesNotExist()
        }
    }

    // REQ-046.A1 + rule 2 (REQ-046: "After the right passcode the aid stays unlocked until the page or app is restarted.")
    // Unlocked: it is not asked again after closing the dialog, moving to other puzzles and back, opening the grid, or a
    // configuration change (the activity is recreated). A restart of the app - the activity is finished and started again, a new
    // process-level lifetime for the aid - asks for the passcode again.
    @Test
    fun req046_A1_theAidStaysUnlockedAcrossBrowsingUntilTheAppIsRestarted() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitForIdle()
            compose.heldUnlock()
            compose.heldTouch("dev-done")
            compose.waitForIdle()
            compose.onNodeWithTag("dev-dialog").assertDoesNotExist()

            compose.heldTouch("next-button")
            compose.heldTouch("next-button")
            compose.heldTouch("prev-button")
            compose.heldTouch("puzzle-counter")
            compose.heldTouch("grid-cell-${puzzles[3].id.value}")
            compose.waitForIdle()

            compose.heldOpenDialog()
            compose.onNodeWithTag("dev-passcode").assertDoesNotExist()
            compose.onNodeWithTag("dev-show-solution").assertExists()
            compose.onNodeWithTag("dev-solve-now").assertExists()
            compose.heldTouch("dev-done")
            compose.waitForIdle()

            scenario.recreate()
            compose.waitForIdle()
            compose.heldOpenDialog()
            compose.onNodeWithTag("dev-passcode").assertDoesNotExist()
            compose.onNodeWithTag("dev-show-solution").assertExists()
            compose.heldTouch("dev-done")
        }
        // the app is restarted
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForIdle()
            compose.heldOpenDialog()
            compose.onNodeWithTag("dev-passcode").assertExists()
            assertNothingRevealed("after the app was restarted")
        }
    }
}
