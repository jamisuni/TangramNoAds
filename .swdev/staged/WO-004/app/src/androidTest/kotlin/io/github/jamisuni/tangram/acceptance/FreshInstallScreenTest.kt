package io.github.jamisuni.tangram.acceptance

import android.content.Intent
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.MainActivity
import io.github.jamisuni.tangram.browse.R
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleKind
import io.github.jamisuni.tangram.kernel.model.PieceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import java.util.Locale

/**
 * The fresh-install screen of the real app (design WO-004 section 7 and the acceptance table: REQ-040/041/045 A1 "on screen",
 * carried from WO-002). The store is wiped first (`ResetStoreRule`, DA-62). Touch injection only.
 */
class FreshInstallScreenTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(ResetStoreRule()).around(compose)

    private val puzzles = PuzzleLibrary.packaged().puzzles
    private val n = puzzles.size
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun titleOf(p: Puzzle) = p.title.inLanguage(Locale.getDefault().language)

    private fun next() = compose.onNodeWithTag("next-button").performTouchInput { click() }

    // REQ-040.A1 - "The first puzzle shown on a fresh install has rating 1."
    // The top bar shows the first library puzzle and its rating dots say rating 1 (`rating-dots`, content description
    // `rating_description`, design test seams).
    @Test
    fun req040_A1_theFirstPuzzleOnAFreshInstallHasRatingOne() {
        assertEquals("fixture: the library's first puzzle is rated 1", 1, puzzles.first().rating)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("puzzle-title").assertTextEquals(titleOf(puzzles.first()))
            compose.onNodeWithTag("puzzle-counter").assertTextEquals("1 / $n")
            compose.onNodeWithTag("rating-dots").assertContentDescriptionEquals(context.getString(R.string.rating_description, 1))
        }
    }

    // REQ-045.A1 - "A fresh install opens on a mini puzzle whose tray holds three pieces."
    // On screen: the puzzle is a mini of three pieces, and exactly those three miniatures are in the tray (TYPE-001 colours below
    // the board); the other four colours are absent from the tray.
    @Test
    fun req045_A1_aFreshInstallOpensOnAMiniWithThreePiecesInTheTray() {
        val first = puzzles.first()
        assertEquals(PuzzleKind.MINI, first.kind)
        assertEquals(3, first.solution.size)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(300)
            val rig = TouchRig(compose)
            val shot = rig.shot()
            val board = rig.board()
            val own = first.solution.map { it.piece }.toSet()
            for (piece in PieceId.entries) {
                assertEquals("tray holds $piece: ${piece in own}", piece in own, rig.inTray(shot, board, piece))
            }
        }
    }

    // REQ-041.A1 - "On a fresh install, the first four puzzles after the mini puzzles are warm-ups."
    // Walking › from the first puzzle shows the library's titles in the library's order, and the library's kinds in that order
    // are the minis, then four warm-ups. Nothing is solved on the way (REQ-003 rule).
    @Test
    fun req041_A1_pressingNextWalksMinisThenFourWarmUpsInLibraryOrder() {
        val minis = puzzles.takeWhile { it.kind == PuzzleKind.MINI }.size
        assertTrue("fixture: at least one mini", minis >= 1)
        for (i in minis until minis + 4) assertEquals("kind of puzzle ${i + 1}", PuzzleKind.WARMUP, puzzles[i].kind)

        ActivityScenario.launch(MainActivity::class.java).use {
            for (i in 0 until n) {
                compose.onNodeWithTag("puzzle-title").assertTextEquals(titleOf(puzzles[i]))
                if (i < n - 1) next()
            }
        }
    }

    // decision DA-56: the debug `puzzle` extra is retired; the exported launcher reads no extra in any build type.
    @Test
    fun decisionDA56_thePuzzleLaunchExtraIsIgnored() {
        val intent = Intent(context, MainActivity::class.java).putExtra("puzzle", puzzles[5].id.value)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<MainActivity>(intent).use {
            compose.onNodeWithTag("puzzle-title").assertTextEquals(titleOf(puzzles.first()))
        }
    }
}
