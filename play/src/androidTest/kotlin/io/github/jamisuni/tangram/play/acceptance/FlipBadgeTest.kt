package io.github.jamisuni.tangram.play.acceptance

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.play.PlaySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** REQ-018 A2 for every shipped puzzle: the badge is there when the puzzle has the parallelogram, nowhere else. */
@RunWith(Parameterized::class)
class FlipBadgeTest(private val id: String) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun ids(): List<String> = PUZZLES.map { it.id.value }
    }

    @get:Rule
    val rule = createComposeRule()

    // REQ-018.A2 - "The badge is visible in every puzzle that contains the parallelogram, and only next to the parallelogram."
    // (REQ-018 rules: "The badge's touch area is at least 60 dp across")
    @Test
    fun req018_A2_theBadgeIsShownInEveryPuzzleWithTheParallelogramAndOnlyThere() {
        val p = puzzle(id)
        val hasPg = p.solution.any { it.piece == PieceId.PG }
        rule.showPlay(PlaySession(p, { true }))
        val badges = rule.onAllNodesWithTag("flip-badge").fetchSemanticsNodes()
        assertEquals("${p.id.value}: number of flip badges", if (hasPg) 1 else 0, badges.size)
        if (hasPg) {
            rule.onNodeWithTag("flip-badge").assertWidthIsAtLeast(60.dp).assertHeightIsAtLeast(60.dp)
            val d = rule.density.density
            val b = badges.single().boundsInRoot
            val badge = R(b.left / d.toDouble(), b.top / d.toDouble(), b.right / d.toDouble(), b.bottom / d.toDouble())
            val cell = layoutFor(p).cellR(PieceId.PG)
            val near = R(cell.l - 20.0, cell.t - 20.0, cell.r + 20.0, cell.b + 20.0)
            assertTrue(
                "${p.id.value}: the badge $badge must be next to the parallelogram's cell $cell",
                badge.l < near.r && badge.r > near.l && badge.t < near.b && badge.b > near.t,
            )
        }
    }
}
