package io.github.jamisuni.tangram.acceptance.layout

import android.app.Activity
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import io.github.jamisuni.tangram.acceptance.ResetStoreRule
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

// decision DA-98: SCAFFOLDING PROBE (no requirement token), spot-check E1: its own class, OUTSIDE the G-DISPLAY gate. It asserts that no
// tray cell lies under the bottom system area ("gesture bar" read as DA-98 says: the larger of the stable navigation-bar inset and, on
// API 29+, the system-gesture and mandatory-gesture insets). API 29+ is run in gesture navigation; below 29 only the inset RULE is
// tested and the message says so. Red until the contingency (safeGestures padding, task 045) if the API 37 inset exceeds bar + 10 dp.
@UsesDisplayRule
@RunWith(Parameterized::class)
class GestureInsetProbeScaffoldingTest(private val spec: DisplaySpec) {
    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun specs(): List<DisplaySpec> = listOf(DisplaySpec.PHONE_390x844)
    }

    private val compose = createEmptyComposeRule()
    private val nav = NavigationMode()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(DisplayRule(spec)).around(TestConfigRule()).around(ResetStoreRule()).around(compose)

    @After
    fun restoreNavigationMode() = nav.restore()

    @Test
    fun decisionDA98_noTrayCellLiesUnderTheBottomSystemArea() {
        val gesture = nav.ensureGesture()
        val wording = if (gesture) "gesture navigation" else "rule-level: tray above the system bar; REQ-035 A2 not applicable on this API"
        Seed.screen(WalkScreen.NEW)
        AppLaunch.launch().use { scenario ->
            compose.waitForIdle()
            var activity: Activity? = null
            scenario.onActivity { activity = it }
            val a = activity ?: error("no activity")
            DeviceShell.waitUntil("the window insets to arrive", 5_000) { runCatching { GestureInsets.read(a).exclusion > 0 }.getOrDefault(false) }
            val exclusionPx = GestureInsets.bottomExclusionPx(a)
            val windowHeight = a.window.decorView.height.toFloat()
            val tray = compose.readTray()
            for ((piece, r) in tray.cells) {
                val bottom = tray.areaTopInWindow + r.bottom
                assertTrue(
                    "$wording: $piece cell bottom $bottom is under the exclusion (window $windowHeight - $exclusionPx = ${windowHeight - exclusionPx})",
                    bottom <= windowHeight - exclusionPx + 0.5f,
                )
            }
        }
    }
}
