package io.github.jamisuni.tangram.acceptance.layout

import androidx.test.core.app.ActivityScenario
import io.github.jamisuni.tangram.LocaleOverrideActivity
import io.github.jamisuni.tangram.TestConfig
import io.github.jamisuni.tangram.time.TimeSource
import org.junit.rules.ExternalResource

// Scaffolding (decision DA-102): the one way the WO-006 device tests start the app. The debug-only LocaleOverrideActivity runs the
// REAL MainActivity code under the per-launch locale list and font scale of TestConfig (null = no override, identical behaviour).
// Nothing global is touched. TestConfigRule resets the fields (language, font scale, clock) to null before and after every test.
internal object AppLaunch {
    // WO-008 (design 5.5, DA-142): [timeSource] is the manual clock the run counts time by; null = the real clock, identical behaviour.
    fun launch(languageTags: String? = null, fontScale: Float? = null, timeSource: TimeSource? = null): ActivityScenario<LocaleOverrideActivity> {
        TestConfig.languageTags = languageTags
        TestConfig.fontScale = fontScale
        TestConfig.timeSource = timeSource
        return ActivityScenario.launch(LocaleOverrideActivity::class.java)
    }
}

internal class TestConfigRule : ExternalResource() {
    private fun reset() {
        TestConfig.languageTags = null
        TestConfig.fontScale = null
        TestConfig.timeSource = null // WO-008 (S2 iii): a clock leaked by one test would freeze time for the rest of the run
    }

    override fun before() = reset()

    override fun after() = reset()
}
