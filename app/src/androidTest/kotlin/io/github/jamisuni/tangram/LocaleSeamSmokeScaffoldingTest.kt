package io.github.jamisuni.tangram

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import io.github.jamisuni.tangram.acceptance.ResetStoreRule
import io.github.jamisuni.tangram.acceptance.touch
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * SCAFFOLDING (disposable): smoke of the debug locale / font-scale seam. decision DA-102
 */
class LocaleSeamSmokeScaffoldingTest {
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(ResetStoreRule()).around(compose)

    @After
    fun resetConfig() {
        TestConfig.languageTags = null
        TestConfig.fontScale = null
    }

    private fun openCatAndAssertTitle(expected: String) {
        ActivityScenario.launch(LocaleOverrideActivity::class.java).use {
            compose.touch("puzzle-counter")
            compose.touch("grid-cell-animals-cat")
            compose.onNodeWithTag("puzzle-title").assertTextEquals(expected)
        }
    }

    @Test
    fun finnishShowsKissa() {
        TestConfig.languageTags = "fi-FI"
        openCatAndAssertTitle("Kissa")
    }

    @Test
    fun englishShowsCat() {
        TestConfig.languageTags = "en-US"
        openCatAndAssertTitle("Cat")
    }

    // puzzle-state is 13 sp: Android 14+ scales larger sizes non-linearly, so the 20 sp title grew only 62 -> 72 px at 1.3.
    private fun titleHeight(scale: Float): Float {
        TestConfig.fontScale = scale
        try {
            ActivityScenario.launch(LocaleOverrideActivity::class.java).use {
                val b = compose.onNodeWithTag("puzzle-state").fetchSemanticsNode().boundsInRoot
                return b.height
            }
        } finally {
            TestConfig.fontScale = null
        }
    }

    @Test
    fun fontScaleGrowsTextHeight() {
        val base = titleHeight(1.0f)
        val big = titleHeight(1.3f)
        assertTrue("height at 1.3 = $big, at 1.0 = $base", big >= 1.2f * base)
    }
}
