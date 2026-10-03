package io.github.jamisuni.tangram.browse

// SCAFFOLDING (TASK-027, disposable): the Restart pill's touch area. Not an acceptance test.
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test

class RestartButtonScaffoldingTest {
    @get:Rule
    val rule = createComposeRule()

    // guardrail: REQ-037 touch area of at least 48 x 48 dp, although the pill is drawn 44 dp high (DA-53)
    @Test
    fun restartTouchAreaIsAtLeast48DpSquare() {
        rule.setContent { RestartButton(onClick = {}) }
        rule.onNodeWithTag("restart-button").assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }
}
