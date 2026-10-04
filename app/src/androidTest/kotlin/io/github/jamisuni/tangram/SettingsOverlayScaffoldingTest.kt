package io.github.jamisuni.tangram

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import io.github.jamisuni.tangram.acceptance.AppStore
import io.github.jamisuni.tangram.acceptance.ResetStoreRule
import io.github.jamisuni.tangram.acceptance.touch
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

// SCAFFOLDING (decision DA-118, no REQ token): the settings overlay in TangramApp, design WO-007 section 2: Back closes it (and cancels a
// pending reset question: closing clears it), it survives a recreate with its state, and with a hardware keyboard focus cannot move into
// the covered play screen (spec-check E4: the test FOCUSES the previous button FIRST, then opens the overlay, then sends Tab and Enter, and
// asserts the puzzle did not change; it is never loosened). It lives in `app` because the overlay, Back and focus handling are TangramApp's
// (the `settings` module cannot see them); the orchestrator's brief named settings/src/androidTest, which cannot host it.
// KNOWN LIMIT (CR-6 N1, no product change in WO-007): a hardware keyboard cannot Tab INTO the overlay itself (focus is cleared when it opens
// and the base group refuses entry); the test therefore asserts only that the covered play screen is not reachable, never that the overlay's
// own controls are keyboard-reachable.
@OptIn(ExperimentalTestApi::class)
class SettingsOverlayScaffoldingTest {
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(ResetStoreRule()).around(compose)

    private val puzzles = PuzzleLibrary.packaged().puzzles
    private val n = puzzles.size

    /**
     * Focuses the previous button the way a keyboard user would. In TOUCH mode a clickable is not focusable at all (Compose's canFocus default
     * follows the input mode), so a bare `requestFocus()` focuses nothing (device evidence, MOVE-DEV7: the counter stayed "2 / 13" after Enter).
     * A key event switches the input mode to Keyboard; then the request works, and focus is ASSERTED, so the caller can trust it.
     */
    private fun keyboardFocusPrevious() {
        sendKey(android.view.KeyEvent.KEYCODE_SHIFT_LEFT) // a real key event: touch mode ends, input mode -> Keyboard
        compose.onNodeWithTag("prev-button").requestFocus()
        compose.waitForIdle()
        val seen = ArrayList<String>()
        repeat(12) {
            if (focusedTags().contains("prev-button")) return
            seen += focusedTags().toString()
            sendKey(android.view.KeyEvent.KEYCODE_TAB)
        }
        error("the previous button never took keyboard focus; focused tags while tabbing: $seen")
    }

    /** A REAL key press through the window (instrumentation), not the compose test's injected one: the path a hardware keyboard takes. */
    private fun sendKey(code: Int) {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        compose.waitForIdle()
    }

    private fun focusedTags(): List<String> =
        compose.onAllNodes(androidx.compose.ui.test.isFocused()).fetchSemanticsNodes().mapNotNull { n ->
            var cur: androidx.compose.ui.semantics.SemanticsNode? = n
            var tag: String? = null
            while (cur != null && tag == null) {
                tag = cur.config.getOrNull(SemanticsProperties.TestTag)
                cur = cur.parent
            }
            tag
        }

    private fun openOverlay() {
        compose.touch("settings-button")
        compose.onNodeWithTag("settings-overlay").assertExists()
    }

    @Test
    fun backClosesTheOverlayAndTheActivityStaysAlive() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            openOverlay()
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            compose.waitForIdle()
            compose.onNodeWithTag("settings-overlay").assertDoesNotExist()
            assertTrue("Back finished the activity instead of closing the overlay", scenario.state.isAtLeast(Lifecycle.State.RESUMED))
            compose.onNodeWithTag("settings-button").assertExists()
        }
    }

    // design section 2 / section 5: "Closing clears a pending reset confirmation (Back during a confirmation is a cancel)": nothing is erased,
    // and the screen reopens without the question.
    @Test
    fun backDuringTheResetQuestionCancelsItAndErasesNothing() {
        val first = puzzles.first()
        AppStore.open().apply {
            saveProgress(first.id, PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 47, 41))
            saveLastShownPuzzle(first.id)
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            openOverlay()
            compose.onNodeWithTag("settings-reset").performScrollTo()
            compose.touch("settings-reset")
            compose.onNodeWithTag("settings-reset-confirm").assertExists()
            scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            compose.waitForIdle()
            compose.onNodeWithTag("settings-overlay").assertDoesNotExist()
            assertEquals("Back erased the progress", PuzzleState.SOLVED, AppStore.open().progress(first.id).state)
            openOverlay()
            compose.onNodeWithTag("settings-reset-confirm").assertDoesNotExist()
            compose.onNodeWithTag("settings-reset").assertExists()
        }
    }

    // design section 2: the controller lives in the ViewModel, so the overlay and its question survive recreate().
    @Test
    fun theOverlayAndAPendingQuestionSurviveARecreate() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            openOverlay()
            compose.onNodeWithTag("settings-reset").performScrollTo()
            compose.touch("settings-reset")
            compose.onNodeWithTag("settings-reset-confirm").assertExists()
            scenario.recreate()
            compose.waitForIdle()
            compose.onNodeWithTag("settings-overlay").assertExists()
            compose.onNodeWithTag("settings-reset-confirm").assertExists()
            compose.onNodeWithTag("settings-reset-cancel").assertExists()
        }
    }

    // POSITIVE CONTROL for the case below (CR-6 N1): with the overlay CLOSED, focus the previous button and send Enter: the counter CHANGES. That
    // proves the key path reaches ‹ in this harness, so "unchanged with the overlay open" means the overlay blocked it, not that keys go nowhere.
    @Test
    fun withTheOverlayClosedAFocusedPreviousButtonIsActivatedByEnter() {
        AppStore.open().saveLastShownPuzzle(puzzles[1].id)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForIdle()
            compose.onNodeWithTag("puzzle-counter").assertTextEquals("2 / $n")
            keyboardFocusPrevious()
            sendKey(android.view.KeyEvent.KEYCODE_ENTER)
            compose.onNodeWithTag("puzzle-counter").assertTextEquals("1 / $n")
        }
    }

    // spec-check E4: a hardware keyboard must not reach the covered play screen. ‹ is focused FIRST (a base control may already hold focus),
    // the overlay is opened, then Tab and Enter are sent: the puzzle did not change. The window starts on the SECOND puzzle so that a
    // pressed ‹ would be visible as a change of the counter.
    @Test
    fun aFocusedPreviousButtonCannotBeActivatedFromTheKeyboardWhileTheOverlayIsOpen() {
        AppStore.open().saveLastShownPuzzle(puzzles[1].id)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.waitForIdle()
            compose.onNodeWithTag("puzzle-counter").assertTextEquals("2 / $n")
            keyboardFocusPrevious()
            openOverlay()
            repeat(2) {
                sendKey(android.view.KeyEvent.KEYCODE_TAB)
                sendKey(android.view.KeyEvent.KEYCODE_ENTER)
            }
            if (compose.onAllNodesCount("settings-overlay") > 0) compose.touch("settings-close")
            compose.waitForIdle()
            compose.onNodeWithTag("puzzle-counter").assertTextEquals("2 / $n")
            assertFalse("the overlay is still covering the puzzle", compose.onAllNodesCount("settings-overlay") > 0)
        }
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesCount(tag: String): Int =
        onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().size
}
