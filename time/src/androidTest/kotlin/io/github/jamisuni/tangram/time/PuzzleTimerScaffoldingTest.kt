package io.github.jamisuni.tangram.time

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

// SCAFFOLDING (disposable, TASK-064 implementer's own check; not an acceptance test).
class PuzzleTimerScaffoldingTest {
    @get:Rule val rule = createComposeRule()

    @Test fun hiddenRendersNothing() {
        rule.setContent { PuzzleTimer(shown = false, seconds = 65) }
        rule.onNodeWithTag("puzzle-timer").assertDoesNotExist()
    }

    @Test fun minutesSeconds() {
        rule.setContent { PuzzleTimer(shown = true, seconds = 65) }
        rule.onNodeWithTag("puzzle-timer", useUnmergedTree = true).assertExists()
        rule.onNodeWithTag("puzzle-timer").assertTextEquals("1:05")
    }

    @Test fun hoursMinutes() {
        rule.setContent { PuzzleTimer(shown = true, seconds = 3600) }
        rule.onNodeWithTag("puzzle-timer").assertTextEquals("1 h 0 min")
    }

    @Test fun inert() {
        rule.setContent { PuzzleTimer(shown = true, seconds = 0) }
        val config = rule.onNodeWithTag("puzzle-timer").fetchSemanticsNode().config
        assertFalse(config.contains(SemanticsProperties.Role))
        assertFalse(config.contains(androidx.compose.ui.semantics.SemanticsActions.OnClick))
    }
}
