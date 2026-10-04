package io.github.jamisuni.tangram.acceptance.layout

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import io.github.jamisuni.tangram.acceptance.ResetStoreRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

// decision DA-99 / DA-100: DECISION TEST (no requirement token), spot-check E6a: the PlayerControlWalk on the SMALLEST tablet (600 x 960 dp,
// en), a visible signal for L-1 and L-3, outside HarnessScaffoldingTest and outside the G-DISPLAY gate: a product breach must not block a
// harness gate. RED until L-1 while the 47.78 dp breach of the square's tray cell stands (the sweep in `play` measured it); it must be
// green after L-1, or with no code at all if L-1 is withdrawn. The full touch-target matrix is held-out.
@UsesDisplayRule
@RunWith(Parameterized::class)
class TabletSmallestWalkAppTest(private val spec: DisplaySpec) {
    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun specs(): List<DisplaySpec> = listOf(DisplaySpec.TABLET_600x960)
    }

    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(DisplayRule(spec)).around(TestConfigRule()).around(ResetStoreRule()).around(compose)

    @Test
    fun decisionDA99_everyPlayerControlMeetsItsMinimumOnTheSmallestTablet() {
        val ids = Seed.puzzles.map { it.id.value }
        val problems = ArrayList<String>()
        for (screen in WalkScreen.entries) {
            Seed.screen(screen)
            AppLaunch.launch("en-US").use {
                Seed.reach(compose, screen)
                val controls = PlayerControlWalk.controlsFor(compose, screen, ids)
                problems += PlayerControlWalk.problems(screen, controls, ids)
            }
        }
        assertTrue("player controls under their minimum on the smallest tablet:\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    // decision DA-100: the 600 dp-wide tablet is a Tablet class window (TYPE-007), so the tray is one row there too.
    @Test
    fun decisionDA100_theTrayIsOneRowOfSevenCellsAtTheSmallestTablet() {
        Seed.screen(WalkScreen.NEW)
        AppLaunch.launch("en-US").use {
            compose.waitForIdle()
            val cells = compose.readTray().cells
            assertEquals(7, cells.size)
            val tops = cells.values.map { it.top }
            assertTrue("one row: tops $tops", tops.max() - tops.min() <= 0.5f)
        }
    }
}
