package io.github.jamisuni.tangram.acceptance.held

import androidx.test.core.app.ActivityScenario
import io.github.jamisuni.tangram.LocaleOverrideActivity
import io.github.jamisuni.tangram.TestConfig
import org.junit.rules.ExternalResource

// Scaffolding (decision DA-102): the one way the WO-006 device tests start the app. The debug-only LocaleOverrideActivity runs the
// REAL MainActivity code under the per-launch locale list and font scale of TestConfig (null = no override, identical behaviour).
// Nothing global is touched. TestConfigRule resets both fields to null before and after every test.
internal object AppLaunch {
    fun launch(languageTags: String? = null, fontScale: Float? = null): ActivityScenario<LocaleOverrideActivity> {
        TestConfig.languageTags = languageTags
        TestConfig.fontScale = fontScale
        return ActivityScenario.launch(LocaleOverrideActivity::class.java)
    }
}

internal class TestConfigRule : ExternalResource() {
    private fun reset() {
        TestConfig.languageTags = null
        TestConfig.fontScale = null
    }

    override fun before() = reset()

    override fun after() = reset()
}
