package io.github.jamisuni.tangram.browse

// SCAFFOLDING (TASK-027, disposable): code review N1 (touch areas inside a short region) and N5 (Button role).
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ShortWindowScaffoldingTest {
    @get:Rule
    val rule = createComposeRule()

    // guardrail: REQ-037 touch areas of at least 48 dp even when the solved-bar region is only 61 dp high (360 x 560 window)
    @Test
    fun solvedBarButtonsKeep48DpInAShortRegion() {
        rule.setContent {
            Box(Modifier.width(360.dp).height(61.dp)) {
                SolvedBar(bestSeconds = 75L, onRetry = {}, onNext = {})
            }
        }
        for (tag in listOf("retry-button", "solved-next-button")) {
            rule.onNodeWithTag(tag).assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        }
    }

    // decision N5: the clickable controls announce as buttons
    @Test
    fun controlsHaveButtonRole() {
        rule.setContent {
            Box(Modifier.width(360.dp).height(120.dp)) {
                SolvedBar(bestSeconds = null, onRetry = {}, onNext = {})
            }
        }
        for (tag in listOf("retry-button", "solved-next-button")) {
            val role = rule.onNodeWithTag(tag).fetchSemanticsNode().config[SemanticsProperties.Role]
            assertEquals(Role.Button, role)
        }
    }
}
