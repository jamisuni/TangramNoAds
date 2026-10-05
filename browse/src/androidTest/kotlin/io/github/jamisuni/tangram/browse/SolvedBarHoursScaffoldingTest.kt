package io.github.jamisuni.tangram.browse

// SCAFFOLDING (TASK-067, disposable): the SolvedBar best-time text around the one-hour switch. Not an acceptance test.
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Locale
import org.junit.Rule
import org.junit.Test

class SolvedBarHoursScaffoldingTest {
    @get:Rule
    val rule = createComposeRule()

    private fun check(seconds: Long, expected: String, language: String) {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val config = Configuration(base.resources.configuration).apply { setLocale(Locale(language)) }
        val context = base.createConfigurationContext(config)
        rule.setContent {
            CompositionLocalProvider(LocalContext provides context, LocalConfiguration provides config) {
                SolvedBar(bestSeconds = seconds, onRetry = {}, onNext = {})
            }
        }
        rule.onNodeWithTag("best-time").assertTextEquals(expected)
    }

    @Test fun en59m59s() = check(3599L, "59:59", "en")
    @Test fun en1h() = check(3600L, "1 h 0 min", "en")
    @Test fun en1h2m() = check(3725L, "1 h 2 min", "en")
    @Test fun fi59m59s() = check(3599L, "59:59", "fi")
    @Test fun fi1h() = check(3600L, "1 h 0 min", "fi")
    @Test fun fi1h2m() = check(3725L, "1 h 2 min", "fi")
}
