package io.github.jamisuni.tangram.acceptance.layout

import android.app.UiAutomation
import android.content.res.Resources
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.acceptance.ResetStoreRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import kotlin.math.abs

// decision DA-96 / DA-98 / DA-99 / DA-102: SCAFFOLDING, the G-DISPLAY gate (design WO-006 section 1; no requirement token). One
// set-verify-restore case per helper. It changes displays ITSELF through DisplayRule(spec, ignorePreset = true), so it carries no
// @UsesDisplayRule marker and no @Parameters: in the fallback (one Gradle call per spec) it is not selected by a spec regex and runs in
// the sixth, native-display call. The gate passes only when every case is green on both channels. The control walk is NOT here
// (it is TabletSmallestWalkAppTest, E6a) and the asserting gesture-inset probe is NOT here (GestureInsetProbeScaffoldingTest, E1).
class HarnessScaffoldingTest {
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(TestConfigRule()).around(ResetStoreRule()).around(compose)

    private fun nativeUnordered(): Pair<Int, Int> = RealDisplay.sizePx().let { minOf(it.first, it.second) to maxOf(it.first, it.second) }

    // gate (1) wm accepts the specs, (3) smallestScreenWidthDp follows the override, (4) the restore is observed
    @Test
    fun everyDisplaySpecIsAppliedAndTheNativeDisplayIsObservedAgain() {
        val native = nativeUnordered()
        for (spec in DisplaySpec.entries) {
            val rule = DisplayRule(spec, ignorePreset = true)
            rule.engage()
            try {
                val (w, h) = RealDisplay.sizePx()
                assertTrue("$spec: real window ${w}x$h", abs(w - spec.widthPx) <= 2 && abs(h - spec.heightPx) <= 2)
                val c = Resources.getSystem().configuration
                assertTrue("$spec: smallestScreenWidthDp ${c.smallestScreenWidthDp}", abs(c.smallestScreenWidthDp - spec.smallestWidthDp) <= 1)
                assertEquals("$spec: densityDpi", spec.densityDpi, c.densityDpi)
            } finally {
                rule.release()
            }
            assertNull("size override left behind after $spec", RealDisplay.override("size"))
            assertNull("density override left behind after $spec", RealDisplay.override("density"))
            assertEquals("native display not observed after $spec", native, nativeUnordered())
        }
    }

    // gate (2): a mid-process density change leaves the instrumentation (and a launched app) alive
    @Test
    fun aDensityChangeMidProcessLeavesTheInstrumentationAndTheAppAlive() {
        val rule = DisplayRule(DisplaySpec.TABLET_1280x800, ignorePreset = true)
        rule.engage()
        try {
            assertEquals("alive", DeviceShell.run("echo alive"))
            Seed.screen(WalkScreen.NEW)
            AppLaunch.launch().use {
                compose.waitForIdle()
                compose.onNodeWithTag("play-area").assertExists()
            }
        } finally {
            rule.release()
        }
    }

    @Test
    fun navigationModeReachesGestureFromAnotherModeAndThePriorModeComesBack() {
        val nav = NavigationMode()
        if (Build.VERSION.SDK_INT < 29) {
            assertFalse("below API 29 there is no gesture mode", nav.ensureGesture())
            nav.restore()
            return
        }
        val baseline = DeviceShell.getSetting("secure", "navigation_mode")
        assertTrue("navigation_mode reads '$baseline'", baseline in listOf("0", "1", "2"))
        // start from a mode that is NOT gestural, so reaching 2 and coming back are two distinguishable states
        try {
            nav.switchTo(0) // inside the try: a timed-out switch must still reach the finally that puts the baseline back (CR-5 S4)
            assertTrue(nav.ensureGesture())
            assertEquals("2", DeviceShell.getSetting("secure", "navigation_mode"))
            nav.restore()
            assertEquals("the prior mode did not come back", "0", DeviceShell.getSetting("secure", "navigation_mode"))
        } finally {
            nav.restore()
            nav.switchTo(baseline.toInt())
        }
        assertEquals(baseline, DeviceShell.getSetting("secure", "navigation_mode"))
    }

    // A phone (smallestScreenWidthDp < 600) is portrait-locked by decision F3, so the window can only turn on a tablet display: the
    // 1920 x 1200 spec is applied first (landscape by nature), then a 90 degree turn must give 1200 x 1920.
    @Test
    fun rotationFreeze90SwapsTheTabletWindowAndTheSavedSettingsComeBack() {
        val rule = DisplayRule(DisplaySpec.TABLET_1280x800, ignorePreset = true)
        rule.engage()
        try {
            // The app is in the foreground while the display turns: on API 26 the launcher does not rotate (device evidence: the display
            // stays 1920 x 1200 with the launcher on top and turns to 1200 x 1920 with the app on top).
            Seed.screen(WalkScreen.NEW)
            AppLaunch.launch().use {
                compose.waitForIdle()
                val acc = DeviceShell.getSetting("system", "accelerometer_rotation")
                val user = DeviceShell.getSetting("system", "user_rotation")
                val before = RealDisplay.sizePx()
                val rot = Rotation()
                rot.freeze90()
                try {
                    val now = RealDisplay.sizePx()
                    assertNotEquals("the window did not turn", before.first > before.second, now.first > now.second)
                    assertEquals("a 90 degree turn swaps width and height", before.first to before.second, now.second to now.first)
                } finally {
                    rot.restore()
                }
                val accNow = DeviceShell.getSetting("system", "accelerometer_rotation")
                val userNow = DeviceShell.getSetting("system", "user_rotation")
                assertTrue("accelerometer_rotation $acc -> $accNow", accNow == acc || (acc == "null" && accNow == "1"))
                assertTrue("user_rotation $user -> $userNow", userNow == user || (user == "null" && userNow == "0"))
            }
        } finally {
            rule.release()
        }
    }

    // decision F3: on the native PHONE display the app window does not turn, even with rotation frozen at 90 degrees.
    @Test
    fun decisionF3_aPhoneWindowStaysPortraitWhenRotationIsFrozenAt90() {
        val smallest = Resources.getSystem().configuration.smallestScreenWidthDp
        assertTrue("fixture: the native display is a phone (smallestScreenWidthDp $smallest)", smallest < 600)
        Seed.screen(WalkScreen.NEW)
        val rot = Rotation()
        rot.freeze0() // saves the prior rotation settings, then freezes at 0
        try {
            AppLaunch.launch().use {
                compose.waitForIdle()
                val start = compose.onRoot().fetchSemanticsNode().boundsInRoot
                assertTrue("fixture: the app starts in portrait ($start)", start.height > start.width)
                assertTrue("setRotation was refused", InstrumentationRegistry.getInstrumentation().uiAutomation.setRotation(UiAutomation.ROTATION_FREEZE_90))
                Thread.sleep(3_000)
                compose.waitForIdle()
                val after = compose.onRoot().fetchSemanticsNode().boundsInRoot
                assertTrue("the phone window turned to landscape ($after)", after.height > after.width)
            }
        } finally {
            rot.restore()
        }
        DeviceShell.waitUntil("the phone display to be portrait again", 10_000) { RealDisplay.sizePx().let { it.first < it.second } }
    }

    // puzzle-state is 13 sp: Android 14+ scales larger sizes non-linearly (a 20 sp title grew only x1.16 at 1.3), so a small-sp node is used.
    private fun stateHeight(scale: Float): Float {
        Seed.screen(WalkScreen.NEW)
        AppLaunch.launch(fontScale = scale).use {
            compose.waitForIdle()
            return compose.onNodeWithTag("puzzle-state").fetchSemanticsNode().boundsInRoot.height
        }
    }

    @Test
    fun theFontScaleOfTheDebugActivityChangesATextHeightAndTouchesNoGlobalSetting() {
        val before = DeviceShell.getSetting("system", "font_scale")
        val base = stateHeight(1.0f)
        val big = stateHeight(1.3f)
        assertTrue("height at 1.3 = $big, at 1.0 = $base", big >= 1.2f * base)
        assertEquals("the global font_scale setting changed", before, DeviceShell.getSetting("system", "font_scale"))
    }

    @Test
    fun airplaneModeIsSetAndTheSavedPriorStateIsRestored() {
        val prior = DeviceShell.getSetting("global", "airplane_mode_on")
        val data = DeviceShell.getSetting("global", "mobile_data")
        val a = AirplaneMode()
        a.on()
        try {
            assertTrue("airplane_mode_on did not read 1", a.isOn())
            Log.i("WO006Airplane", "limits: ${a.limits.ifEmpty { "none" }}")
        } finally {
            a.restore()
        }
        val now = DeviceShell.getSetting("global", "airplane_mode_on")
        assertTrue("airplane_mode_on $prior -> $now", now == prior || (prior == "null" && now == "0"))
        if (Build.VERSION.SDK_INT < 30) assertEquals("mobile_data changed", data, DeviceShell.getSetting("global", "mobile_data"))
    }

    // E1: this case only LOGS the three bottom inserts (WO006Insets); the asserting probe is GestureInsetProbeScaffoldingTest.
    @Test
    fun gestureInsetsAreReadAndLogged() {
        val rule = DisplayRule(DisplaySpec.PHONE_390x844, ignorePreset = true)
        rule.engage()
        try {
            Seed.screen(WalkScreen.NEW)
            AppLaunch.launch().use { scenario ->
                compose.waitForIdle()
                var activity: android.app.Activity? = null
                scenario.onActivity { activity = it }
                val a = activity ?: error("no activity")
                DeviceShell.waitUntil("the window insets to arrive", 5_000) { runCatching { GestureInsets.read(a) }.isSuccess }
                Log.i("WO006Insets", "HarnessScaffoldingTest read: ${GestureInsets.read(a)}")
            }
        } finally {
            rule.release()
        }
    }

    // decision DA-99 (CR-5 S1): NEGATIVE CONTROL for the touch-target walk. A tagged clickable of 30 x 30 dp, set up here, must be reported
    // under its 48 dp minimum, and a 48 x 48 dp one must not: the measure can fail, and it measures the layout box (Compose widens the
    // touch bounds of pointer-input nodes to 48 dp, so a walk reading those would pass the small one).
    @Test
    fun theControlWalkReportsAThirtyDpClickableAsTooSmall() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    Column {
                        Box(Modifier.size(30.dp).testTag("neg-30").clickable { })
                        Box(Modifier.size(48.dp).testTag("neg-48").clickable { })
                    }
                }
            }
            compose.waitForIdle()
            val controls = PlayerControlWalk.controls(compose)
            val small = controls.singleOrNull { it.tag == "neg-30" } ?: error("walk blind: the 30 dp control was not found in $controls")
            val ok = controls.singleOrNull { it.tag == "neg-48" } ?: error("walk blind: the 48 dp control was not found in $controls")
            assertFalse("a 30 dp clickable was measured as $small and accepted", small.meets(PlayerControlWalk.minimumDp("neg-30")))
            assertTrue("a 48 dp clickable was measured as $ok and rejected", ok.meets(PlayerControlWalk.minimumDp("neg-48")))
            assertTrue(PlayerControlWalk.problems(WalkScreen.NEW, controls).any { it.contains("neg-30") })
        }
    }

    // decision DA-102: the locale wrapper shows the Cat in Finnish ("Kissa", REQ-047's own text) in the real top bar
    @Test
    fun theLocaleWrapperShowsKissaInFinnish() {
        Seed.screen(WalkScreen.NEW)
        AppLaunch.launch("fi-FI").use {
            compose.waitForIdle()
            compose.onNodeWithTag("puzzle-title").assertTextEquals("Kissa")
        }
    }
}
