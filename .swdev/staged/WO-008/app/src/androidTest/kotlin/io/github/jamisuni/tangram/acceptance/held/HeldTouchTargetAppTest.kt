package io.github.jamisuni.tangram.acceptance.held

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import io.github.jamisuni.tangram.acceptance.layout.UsesDisplayRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/**
 * HELD-OUT (Test & Verify only): the touch-target matrix of design WO-006 section 4 (DA-99). Own kit copies (package `...acceptance.held`);
 * the marker is the visible one by FQN. A "player control" is a merged-tree node with a click or long-click action or an interactive role
 * (tag or nearest tagged ancestor; an unclassified one fails the walk), plus the flip badge and the seven tray cells; `dev-*` is skipped.
 * Screens: New, In progress, Solved, the grid scrolled to every puzzle, and (WO-007, C5, now a visible test) the settings screen and its
 * reset confirmation, scrolled control by control; the gear is a player control of every play screen. Windows: the reference windows of REQ-006, 035 and 036, the 360 dp
 * phone and the smallest tablet (the AVD's own size is [HeldTouchTargetNativeWindowAppTest]). English everywhere, Finnish on the 360 dp
 * phone and the smallest tablet, font scale 2.0 on the 360 dp phone only (a size in dp does not shrink with the font).
 */
@UsesDisplayRule
@RunWith(Parameterized::class)
class HeldTouchTargetAppTest(private val spec: DisplaySpec) {
    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun specs(): List<DisplaySpec> = listOf(
            DisplaySpec.PHONE_390x844, DisplaySpec.PHONE_360x780, DisplaySpec.TABLET_1280x800, DisplaySpec.TABLET_800x1280, DisplaySpec.TABLET_600x960,
        )
    }

    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(DisplayRule(spec)).around(TestConfigRule()).around(ResetStoreRule()).around(compose)

    // REQ-037.A1 - "No player control in any screen has a touch area below 48 dp."
    // Every control of every screen state of this window, in each language and font scale of the matrix: both axes at least 48 dp
    // (previous and next 52 dp, the flip badge 60 dp, REQ-037 Rules); the walk is not blind (it must find the controls that exist).
    @Test
    fun req037_A1_noPlayerControlHasATouchAreaBelowItsMinimum() {
        val problems = HeldTouchWalk.run(compose, spec.name, languages(spec), fontScale(spec))
        assertTrue("player controls under their minimum on $spec:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    private fun languages(s: DisplaySpec): List<String> =
        if (s == DisplaySpec.PHONE_360x780 || s == DisplaySpec.TABLET_600x960) listOf("en-US", "fi-FI") else listOf("en-US")

    private fun fontScale(s: DisplaySpec): List<Float?> = if (s == DisplaySpec.PHONE_360x780) listOf(null, 2.0f) else listOf(null)
}

/** The AVD's own window: the same walk on the native display, English only (the matrix's sixth window). Runs in the fallback's native call. */
class HeldTouchTargetNativeWindowAppTest {
    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(TestConfigRule()).around(ResetStoreRule()).around(compose)

    @Test
    fun noPlayerControlOnTheNativeWindowIsBelowItsMinimum() {
        val problems = HeldTouchWalk.run(compose, "NATIVE", listOf("en-US"), listOf(null))
        assertTrue("player controls under their minimum on the native window:\n" + problems.joinToString("\n"), problems.isEmpty())
    }
}

private object HeldTouchWalk {
    fun run(compose: androidx.compose.ui.test.junit4.ComposeTestRule, window: String, languages: List<String>, scales: List<Float?>): List<String> {
        val ids = Seed.puzzles.map { it.id.value }
        val problems = ArrayList<String>()
        for (lang in languages) for (scale in scales) for (screen in WalkScreen.entries) {
            Seed.screen(screen)
            AppLaunch.launch(lang, scale).use {
                Seed.reach(compose, screen)
                val controls = PlayerControlWalk.controlsFor(compose, screen, ids)
                val label = "$window $lang font=${scale ?: 1.0f}"
                problems += PlayerControlWalk.problems(screen, controls, ids).map { "$label: $it" }
                // the flip badge is a player control of the play screens while the parallelogram is in the tray
                if ((screen == WalkScreen.NEW || screen == WalkScreen.IN_PROGRESS) && controls.none { it.tag == "flip-badge" }) {
                    problems += "$label: walk blind on $screen: no 'flip-badge' control was found"
                }
            }
        }
        problems += timerPill(compose, window, languages, scales, ids)
        return problems
    }

    /**
     * WO-008 (C6, design "Carried parts"): REQ-037's 48 dp concerns the SETTING SWITCH, which the settings walks above now learn (`settings-timer`
     * is a must-find tag of both settings screens, and every `settings-*` control is held to 48 dp). The on-board timer is not a control: with the
     * timer on, the pill is composed on a New and an In-progress puzzle, is NOT a player control of the walk and carries no click or long-click
     * action (design 7: "a plain text node (no click action, no pointer input)"), so it can never be a touch target under 48 dp.
     */
    private fun timerPill(
        compose: androidx.compose.ui.test.junit4.ComposeTestRule,
        window: String,
        languages: List<String>,
        scales: List<Float?>,
        ids: List<String>,
    ): List<String> {
        val problems = ArrayList<String>()
        for (lang in languages) for (scale in scales) for (screen in listOf(WalkScreen.NEW, WalkScreen.IN_PROGRESS)) {
            Seed.screen(screen)
            Seed.settings(timerShown = true)
            AppLaunch.launch(lang, scale).use {
                Seed.reach(compose, screen)
                val label = "$window $lang font=${scale ?: 1.0f} timer on"
                if (compose.onAllNodesWithTag("puzzle-timer").fetchSemanticsNodes().isEmpty()) {
                    problems += "$label: walk blind on $screen: no 'puzzle-timer' with the timer switched on"
                } else {
                    val config = compose.onNodeWithTag("puzzle-timer").fetchSemanticsNode().config
                    if (config.contains(SemanticsActions.OnClick) || config.contains(SemanticsActions.OnLongClick)) problems += "$label: the pill on $screen is not inert (it has a click action)"
                    val controls = PlayerControlWalk.controlsFor(compose, screen, ids)
                    if (controls.any { it.tag == "puzzle-timer" }) problems += "$label: the walk classified the pill as a player control on $screen"
                    problems += PlayerControlWalk.problems(screen, controls, ids).map { "$label: $it" }
                }
            }
        }
        return problems
    }
}
