package io.github.jamisuni.tangram.acceptance.layout

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import io.github.jamisuni.tangram.acceptance.ResetStoreRule
import io.github.jamisuni.tangram.kernel.layout.TrayRules
import io.github.jamisuni.tangram.kernel.model.PieceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/**
 * Phone portrait layout on the real app at the REQ-035 reference window (design WO-006 section 2). The display comes from [DisplayRule];
 * the full-set puzzle is seeded through `lastShown` (the fresh-install puzzle is a 3-piece mini).
 */
@UsesDisplayRule
@RunWith(Parameterized::class)
class PhoneLayoutAppTest(private val spec: DisplaySpec) {
    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun specs(): List<DisplaySpec> = listOf(DisplaySpec.PHONE_390x844)
    }

    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(DisplayRule(spec)).around(TestConfigRule()).around(ResetStoreRule()).around(compose)

    private fun scrollNodes(n: SemanticsNode, out: MutableList<SemanticsNode>) {
        if (n.declaresScroll()) out += n
        for (c in n.children) scrollNodes(c, out)
    }

    // REQ-035.A1 - "On a 390 x 844 dp phone, all seven pieces are visible in the tray without scrolling."
    // On the real window: exactly seven tray cells in two rows (3 + 4: large and medium triangles above square, parallelogram and
    // small triangles, REQ-035 Rules, in TYPE-001 order), every cell inside the play-area bounds, the area below the top bar and
    // inside the window, and no node on the play screen declares a scroll action.
    @Test
    fun req035_A1_sevenTrayCellsInTwoRowsInsideTheAreaWithoutScrolling() {
        Seed.screen(WalkScreen.NEW)
        AppLaunch.launch().use {
            compose.waitForIdle()
            val tray = compose.readTray()

            // (a) seven cells, two rows, 3 + 4
            assertEquals("seven tray cells", 7, tray.cells.size)
            val sorted = tray.cells.entries.sortedBy { it.value.top }
            val rows = ArrayList<MutableList<Map.Entry<PieceId, androidx.compose.ui.geometry.Rect>>>()
            for (e in sorted) {
                val last = rows.lastOrNull()
                if (last != null && e.value.top - last.first().value.top <= 0.5f) last += e else rows += mutableListOf(e)
            }
            assertEquals("two tray rows, got tops ${rows.map { it.first().value.top }}", 2, rows.size)
            assertEquals(setOf(PieceId.LT1, PieceId.LT2, PieceId.MT), rows[0].map { it.key }.toSet())
            assertEquals(setOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2), rows[1].map { it.key }.toSet())
            for (row in rows) {
                val inOrder = row.sortedBy { it.value.left }.map { it.key }
                assertEquals("left-to-right order of a row is the TYPE-001 order", TrayRules.order.filter { it in inOrder }, inOrder)
            }

            // (b) every cell inside the play area, the area below the top bar and inside the window
            val w = tray.areaSize.width.toFloat()
            val h = tray.areaSize.height.toFloat()
            for ((piece, r) in tray.cells) {
                assertTrue("$piece cell $r lies outside the play area ${w}x$h", r.left >= 0f && r.top >= 0f && r.right <= w && r.bottom <= h)
            }
            val area = compose.onNodeWithTag("play-area").fetchSemanticsNode().boundsInRoot
            val bar = compose.onNodeWithTag("top-bar").fetchSemanticsNode().boundsInRoot
            val window = compose.onRoot().fetchSemanticsNode().boundsInRoot
            assertTrue("the play area $area starts above the bottom of the top bar $bar", area.top >= bar.bottom - 0.5f)
            assertTrue("the play area $area is not inside the window $window", area.left >= window.left && area.right <= window.right && area.bottom <= window.bottom)

            // (c) nothing on the play screen scrolls
            val scrolling = ArrayList<SemanticsNode>()
            scrollNodes(compose.onRoot(useUnmergedTree = true).fetchSemanticsNode(), scrolling)
            assertTrue("nodes declaring a scroll action: ${scrolling.map { it.config }}", scrolling.isEmpty())
        }
    }
}
