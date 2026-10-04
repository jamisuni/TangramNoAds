package io.github.jamisuni.tangram.acceptance.layout

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import io.github.jamisuni.tangram.acceptance.ResetStoreRule
import io.github.jamisuni.tangram.kernel.layout.TrayRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/**
 * Tablet tray on the real app at the two REQ-036 reference windows (design WO-006 section 3), a full-set puzzle seeded through `lastShown`.
 */
@UsesDisplayRule
@RunWith(Parameterized::class)
class TabletLayoutAppTest(private val spec: DisplaySpec) {
    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun specs(): List<DisplaySpec> = listOf(DisplaySpec.TABLET_1280x800, DisplaySpec.TABLET_800x1280)
    }

    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(DisplayRule(spec)).around(TestConfigRule()).around(ResetStoreRule()).around(compose)

    // REQ-036.A1 - "On 1280 x 800 dp and 800 x 1280 dp tablets, the tray is one row."
    // Seven cells, one row (all tops equal within 0.5 px, lefts strictly increasing in the TYPE-001 order of REQ-012), all inside the area.
    @Test
    fun req036_A1_theTrayIsOneRowOfSevenCellsInsideTheArea() {
        Seed.screen(WalkScreen.NEW)
        AppLaunch.launch().use {
            compose.waitForIdle()
            val tray = compose.readTray()
            assertEquals("seven tray cells", 7, tray.cells.size)
            val tops = tray.cells.values.map { it.top }
            assertTrue("one row: tops $tops differ by more than 0.5 px", tops.max() - tops.min() <= 0.5f)
            val lefts = TrayRules.order.map { tray.cells.getValue(it).left }
            for (i in 1 until lefts.size) assertTrue("lefts $lefts are not strictly increasing in the tray order", lefts[i] > lefts[i - 1])
            val w = tray.areaSize.width.toFloat()
            val h = tray.areaSize.height.toFloat()
            for ((piece, r) in tray.cells) {
                assertTrue("$piece cell $r lies outside the play area ${w}x$h", r.left >= 0f && r.top >= 0f && r.right <= w && r.bottom <= h)
            }
        }
    }
}
