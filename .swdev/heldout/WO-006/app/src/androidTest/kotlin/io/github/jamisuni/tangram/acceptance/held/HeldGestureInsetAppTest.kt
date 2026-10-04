package io.github.jamisuni.tangram.acceptance.held

import android.app.Activity
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import io.github.jamisuni.tangram.acceptance.layout.UsesDisplayRule
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/**
 * HELD-OUT (Test & Verify only): the gesture bar of the phone layout, on the real window (design WO-006 section 2, DA-98). Own kit copies
 * (package `...acceptance.held`); the marker is the visible one by FQN. "Gesture bar" is read as the bottom system area: the larger of the
 * stable navigation-bar inset and, on API 29+, the system-gesture and mandatory-gesture insets. API 37 is the evidence; below API 29
 * there is no gesture navigation and only the inset rule against the 3-button bar can be tested, which the failure message says.
 */
@UsesDisplayRule
@RunWith(Parameterized::class)
class HeldGestureInsetAppTest(private val spec: DisplaySpec) {
    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun specs(): List<DisplaySpec> = listOf(DisplaySpec.PHONE_390x844, DisplaySpec.PHONE_360x780)
    }

    private val compose = createEmptyComposeRule()
    private val nav = NavigationMode()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(DisplayRule(spec)).around(TestConfigRule()).around(ResetStoreRule()).around(compose)

    @After
    fun restoreNavigationMode() = nav.restore()

    // REQ-035.A2 - "With gesture navigation, no tray cell lies under the gesture bar."
    // Gesture navigation is forced first (API 29+, loud when it is not reached), the full tray is shown, and every tray cell's bottom, in
    // window coordinates, is at or above the window height minus the bottom exclusion. The exclusion is never 0 (loud).
    @Test
    fun req035_A2_noTrayCellLiesUnderTheGestureBar() {
        val gesture = nav.ensureGesture()
        val wording = if (gesture) "gesture navigation" else "rule-level: tray above the system bar; A2 not applicable on this API"
        Seed.screen(WalkScreen.NEW)
        AppLaunch.launch("en-US").use { scenario ->
            compose.waitForIdle()
            var activity: Activity? = null
            scenario.onActivity { activity = it }
            val a = activity ?: error("no activity")
            DeviceShell.waitUntil("the window insets to arrive", 5_000) { runCatching { GestureInsets.read(a).exclusion > 0 }.getOrDefault(false) }
            val inset = GestureInsets.read(a)
            val exclusionPx = GestureInsets.bottomExclusionPx(a)
            val windowHeight = a.window.decorView.height.toFloat()
            val tray = compose.readTray()
            assertTrue("seven tray cells expected, got ${tray.cells.size}", tray.cells.size == 7)
            for ((piece, r) in tray.cells) {
                val bottom = tray.areaTopInWindow + r.bottom
                assertTrue(
                    "$wording ($spec, insets $inset): $piece cell bottom $bottom is under the exclusion (window $windowHeight - $exclusionPx = ${windowHeight - exclusionPx})",
                    bottom <= windowHeight - exclusionPx + 0.5f,
                )
            }
        }
    }
}
