package io.github.jamisuni.tangram.acceptance.layout

import android.util.Log
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import io.github.jamisuni.tangram.acceptance.ResetStoreRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import kotlin.math.abs

/** One window of the measured matrix: [language] and [fontScale] (null = 1.0) on [spec]. */
class TimerCase(val spec: DisplaySpec, val language: String, val fontScale: Float?) {
    override fun toString() = "${spec.name}_${language}_font${fontScale ?: 1.0f}"
}

/**
 * decision DA-140 (design WO-008 section 7 (b) and the Test Author's task T8a): the timer pill measured on the real device, in the matrix of the
 * design: Finnish at font scale 2.0 on the 360 dp phone (the worst case: the widest Restart, the widest text), English at 1.0 on the 390 dp
 * phone, and the tablet. Seeded through the real store: the Cat In progress, three pieces, the timer on, and a puzzle time of **10 h 59 min**
 * (the widest text of the matrix except the template). The debug build shows the DEV pill, so the real rects of `restart-button`, the DEV pill
 * (`dev-button`) and `puzzle-timer` are read from the semantics tree. The measured widths are LOGGED under the logcat tag
 * [TAG] for the workorder (MEASURE-8 raises the JVM sweep's width bounds if they exceed them).
 *
 * Mandated by the design (7): the pill never overlaps the Restart control or the DEV pill, stays inside the play area, is right-aligned at the
 * board rect's top-right edge inset 8 dp whatever its text, and sits at the corner (top = board top + 8 dp) unless an obstacle stepped it below
 * (top = obstacle bottom + 4 dp). "The pill does not move within an attempt" (rev 2, E3): crossing one hour changes the text and not the place.
 */
@UsesDisplayRule
@RunWith(Parameterized::class)
class TimerLayoutMeasuredAppTest(private val case: TimerCase) {
    companion object {
        const val TAG = "TimerLayoutMeasured"

        @JvmStatic
        @Parameters(name = "{0}")
        fun cases(): List<TimerCase> = listOf(
            TimerCase(DisplaySpec.PHONE_360x780, "fi-FI", 2.0f),
            TimerCase(DisplaySpec.PHONE_390x844, "en-US", null),
            TimerCase(DisplaySpec.TABLET_1280x800, "en-US", null),
        )
    }

    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(DisplayRule(case.spec)).around(TestConfigRule()).around(ResetStoreRule()).around(compose)

    // The density of the LAUNCHED app under this case's display override (DisplaySpec sets 400 or 240 dpi), read from the composition itself:
    // never `targetContext.resources.displayMetrics`, which is the emulator's native density (DA-40: the fixture was wrong, not the product).
    private val density: Float get() = compose.onNodeWithTag("play-area").fetchSemanticsNode().layoutInfo.density.density
    private val cat = Seed.fullPuzzle()

    private fun seed(seconds: Long) {
        Seed.screen(WalkScreen.IN_PROGRESS, cat)
        Seed.settings(timerShown = true)
        Seed.inProgress(cat, seconds = seconds, best = null, pieces = 3)
    }

    private fun rectOf(tag: String): Rect = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    private fun dp(px: Float) = px / density

    private fun overlaps(a: Rect, b: Rect) = a.overlaps(b)

    /** The obstacles on this screen: Restart always, the DEV pill in this (debug) build. */
    private fun obstacles(): Map<String, Rect> {
        val out = LinkedHashMap<String, Rect>()
        out["restart-button"] = rectOf("restart-button")
        if (compose.onAllNodesWithTag("dev-button").fetchSemanticsNodes().isNotEmpty()) out["dev-button"] = rectOf("dev-button")
        return out
    }

    private fun engage() {
        compose.onNodeWithTag("puzzle-title").performTouchInput { click() }
        compose.waitForIdle()
    }

    // decision DA-140: with an hours value the pill clears Restart and the DEV pill, stays in the area, keeps the board's top-right edge, and
    // steps below an obstacle only by the design's rule. The measured widths go to the log.
    @Test
    fun thePillClearsRestartAndTheDevPillAndKeepsTheBoardCorner() {
        seed(10 * 3_600L + 59 * 60)
        AppLaunch.launch(case.language, case.fontScale).use {
            compose.waitForIdle()
            assertEquals("fixture: the hours value is on screen", true, ScreenWalk.textOf(compose, "puzzle-timer").let { "10" in it && "59" in it })
            val pill = rectOf("puzzle-timer")
            val area = rectOf("play-area")
            val board = rectOf("board")
            val obs = obstacles()
            Log.i(
                TAG,
                "case=$case pill=${dp(pill.width)}x${dp(pill.height)}dp restart=${dp(obs.getValue("restart-button").width)}x${dp(obs.getValue("restart-button").height)}dp " +
                    "dev=${obs["dev-button"]?.let { "${dp(it.width)}x${dp(it.height)}dp" } ?: "none"} area=${dp(area.width)}x${dp(area.height)}dp",
            )

            assertTrue("the pill $pill leaves the play area $area", pill.left >= area.left - 1 && pill.right <= area.right + 1 && pill.top >= area.top - 1 && pill.bottom <= area.bottom + 1)
            for ((name, r) in obs) assertTrue("the pill $pill overlaps $name $r", !overlaps(pill, r))

            val inset = 8 * density
            assertTrue("the pill's right edge ${pill.right} is not the board's right edge ${board.right} less 8 dp", abs(pill.right - (board.right - inset)) <= 1.5f)
            val atCorner = abs(pill.top - (board.top + inset)) <= 1.5f
            if (!atCorner) {
                val below = obs.filter { (_, r) -> abs(pill.top - (r.bottom + 4 * density)) <= 1.5f }
                assertTrue("the pill $pill is neither at the corner (top ${board.top + inset}) nor 4 dp below an obstacle $obs", below.isNotEmpty())
            }
        }
    }

    // decision DA-140 (rev 2, E3): "the pill does not move within an attempt". The text crosses from 59:59 to 1 h 0 min (a longer text) and
    // the pill's top and right edge are the same: the step decision comes from a template width, not from the live text.
    @Test
    fun thePillDoesNotMoveWhenItsTextCrossesOneHour() {
        seed(3_599)
        val clock = ManualTimeSource()
        AppLaunch.launch(case.language, case.fontScale, timeSource = clock).use { scenario ->
            compose.waitForIdle()
            engage()
            val before = rectOf("puzzle-timer")
            assertEquals("fixture: 59:59 on screen", "59:59", ScreenWalk.textOf(compose, "puzzle-timer"))
            scenario.advanceActive(compose, clock, 2_000)
            val after = rectOf("puzzle-timer")
            assertTrue("fixture: the text changed to hours: ${ScreenWalk.textOf(compose, "puzzle-timer")}", "h" in ScreenWalk.textOf(compose, "puzzle-timer"))
            assertTrue("the pill moved vertically across one hour: $before -> $after", abs(before.top - after.top) <= 1.0f)
            assertTrue("the pill's right edge moved across one hour: $before -> $after", abs(before.right - after.right) <= 1.0f)
        }
    }
}
