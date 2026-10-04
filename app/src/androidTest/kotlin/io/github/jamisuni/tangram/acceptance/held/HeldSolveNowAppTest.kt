package io.github.jamisuni.tangram.acceptance.held

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ActivityScenario
import io.github.jamisuni.tangram.MainActivity
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * HELD-OUT (Test & Verify only): REQ-046.A3 entirely, on the real app and its real store file. Self-contained.
 * Scenarios respect the logged decisions: a solved puzzle takes no touches (DA-35) and stores no piece positions (DA-48, DA-65),
 * so the aid is used on a New or In-progress puzzle and only the bar's buttons are touched afterwards; the aid solve runs the
 * normal REQ-023 timeline (DA-83); the stored document is read back through `JsonProgressStore` (what a restarted app reads),
 * never by editing the frozen fixtures.
 *
 * The fixtures can tell the cases apart: an ordinary solve of a puzzle that has run 77 s would store 77 as a best time (or lower
 * a best of 600 to 77), so "best time stays empty" and "stays 600" cannot hold by accident.
 */
class HeldSolveNowAppTest {

    private val compose = createEmptyComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(HeldResetStoreRule()).around(compose)

    private val puzzles = PuzzleLibrary.packaged().puzzles

    private fun seed(index: Int, progress: PuzzleProgress) {
        HeldStore.open().apply {
            saveProgress(puzzles[index].id, progress)
            saveLastShownPuzzle(puzzles[index].id)
        }
    }

    private fun threePieces(index: Int) = puzzles[index].solution.map { it.piece }.take(3).associateWith { heldSave(puzzles[index], it) }

    /** Unlocks (clock running), then pauses the clock and uses "Solve this puzzle now"; returns after the fade and the confetti. */
    private fun solveNowAndWait(): Pair<android.graphics.Bitmap, android.graphics.Bitmap> {
        compose.mainClock.autoAdvance = true // an earlier launch of the same test may have left the clock paused
        compose.waitForIdle()
        compose.heldUnlock()
        compose.mainClock.autoAdvance = false
        compose.mainClock.advanceTimeBy(100)
        val rig = HeldBoardRig(compose)
        compose.heldTouch("dev-solve-now") // the solve happens on the release of this touch; 200 ms of test time follow it
        val early = rig.shot()
        compose.mainClock.advanceTimeBy(3500) // 600 ms delay + 800 ms fade + 2.4 s confetti, with room
        return early to rig.shot()
    }

    // REQ-046.A3 - "'Solve this puzzle now' shows the solved picture and leaves the best time empty."  (the picture half)
    // New puzzle 1 (nothing stored). After the aid: shortly after the solve the pieces stand at their places and the picture is NOT
    // there yet (REQ-023: it fades in from 600 ms after the last lock); after the fade the picture's base colour fills the board,
    // the solved bar with Retry is shown, and the DEV pill is hidden (REQ-046 rule 5).
    @Test
    fun req046_A3_solveNowShowsTheSolvedPictureThroughTheNormalTimeline() {
        val puzzle = puzzles.first()
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
            val flat = HeldBoardRig(compose).baseCount(HeldBoardRig(compose).shot(), puzzle)
            // Noise band: isolated anti-aliased edge pixels between piece colours fall inside the colour tolerance (Test & Verify saw 1-4,
            // and 72 at 1030 ms mid-fade). A picture that is really there fills the board (about 10000-22000 samples), so "no picture"
            // is an absolute bound of 20, three orders of magnitude below it.
            assertTrue("fixture: a New puzzle's silhouette shows no picture ($flat)", flat <= 20)

            val (early, late) = solveNowAndWait()
            val rig = HeldBoardRig(compose)

            // Early means before the REQ-023 delay (600 ms). Noise-robust: the early count must be at most 2 % of the settled count
            // (about 435 of 21790 seen), so edge noise (1-4) passes and a picture already shown (thousands) fails.
            val earlyBase = rig.baseCount(early, puzzle)
            val lateBase = rig.baseCount(late, puzzle)
            assertTrue("shortly after the solve the picture is already there (no timeline?): $earlyBase of $lateBase", earlyBase * 50 <= lateBase)
            assertTrue("shortly after the solve the pieces stand at their places", rig.piecesOnBoard(early).isNotEmpty())

            val base = rig.baseCount(late, puzzle)
            assertTrue("after the fade the picture's base colour must fill the board ($base)", base >= 100)
            compose.onNodeWithTag("solved-bar").assertExists()
            compose.onNodeWithTag("retry-button").assertExists()
            compose.onNodeWithTag("dev-button").assertDoesNotExist()
            assertEquals("no solution shape on a solved puzzle", emptyList<String>(), compose.heldOverlayTags())
        }
        // a restarted app finds the puzzle solved
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.mainClock.autoAdvance = false
            compose.mainClock.advanceTimeBy(500)
            compose.onNodeWithTag("solved-bar").assertExists()
            assertTrue("the picture is shown after a relaunch", HeldBoardRig(compose).baseCount(HeldBoardRig(compose).shot(), puzzle) >= 100)
        }
    }

    // REQ-046.A3 - (the stored half, best time empty) A puzzle never solved by the player, 77 s into play with three pieces down:
    // after the aid the stored entry is Solved and its best time is still empty (an ordinary solve would have stored 77).
    @Test
    fun req046_A3_aPuzzleSolvedByTheAidIsStoredSolvedWithNoBestTime() {
        val index = puzzles.indexOfFirst { it.solution.size == 7 }
        check(index >= 0) { "fixture: a 7-piece puzzle" }
        seed(index, PuzzleProgress(PuzzleState.IN_PROGRESS, threePieces(index), 77, null))
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
            compose.onNodeWithTag("restart-button").assertExists() // fixture: the seeded puzzle is in progress
            solveNowAndWait()
            compose.onNodeWithTag("solved-bar").assertExists()
            val stored = HeldStore.open().progress(puzzles[index].id)
            assertEquals(PuzzleState.SOLVED, stored.state)
            assertNull("the aid set a best time", stored.bestSeconds)
        }
        val afterClose = HeldStore.open().progress(puzzles[index].id)
        assertEquals(PuzzleState.SOLVED, afterClose.state)
        assertNull("the aid set a best time", afterClose.bestSeconds)
    }

    // REQ-046.A3 - (the stored half, an earlier best is kept) 600 s earlier best, now 77 s into a replay with pieces down: the aid
    // neither sets a new best (77 would be one) nor loses the old one. Also the state right after a Retry (New, no pieces, best 41).
    @Test
    fun req046_A3_anEarlierBestTimeIsKept() {
        val index = puzzles.indexOfFirst { it.solution.size == 7 }
        check(index >= 0) { "fixture: a 7-piece puzzle" }
        val cases = listOf(
            PuzzleProgress(PuzzleState.IN_PROGRESS, threePieces(index), 77, 600) to 600L,
            PuzzleProgress(PuzzleState.NEW, emptyMap(), 0, 41) to 41L,
        )
        for ((seeded, best) in cases) {
            HeldStore.wipe()
            seed(index, seeded)
            ActivityScenario.launch(MainActivity::class.java).use {
                solveNowAndWait()
                compose.onNodeWithTag("solved-bar").assertExists()
                val stored = HeldStore.open().progress(puzzles[index].id)
                assertEquals("seeded ${seeded.state}", PuzzleState.SOLVED, stored.state)
                assertEquals("seeded ${seeded.state}: the earlier best time", best, stored.bestSeconds)
            }
            assertEquals("seeded ${seeded.state}: after the app closed", best, HeldStore.open().progress(puzzles[index].id).bestSeconds)
        }
    }

    // REQ-046.A3 - the aid touches only the puzzle it was used on: every other puzzle's stored progress is as it was (the stored
    // document is otherwise untouched).
    @Test
    fun req046_A3_onlyTheSolvedPuzzlesEntryChanges() {
        val others = mapOf(
            1 to PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 47, 41),
            3 to PuzzleProgress(PuzzleState.NEW, emptyMap(), 0, 9),
        )
        HeldStore.open().apply { for ((i, p) in others) saveProgress(puzzles[i].id, p) }
        val before = others.keys.associateWith { HeldStore.open().progress(puzzles[it].id) }
        seed(0, PuzzleProgress(PuzzleState.NEW, emptyMap(), 0, null))
        ActivityScenario.launch(MainActivity::class.java).use {
            solveNowAndWait()
            compose.onNodeWithTag("solved-bar").assertExists()
        }
        val store = HeldStore.open()
        assertEquals(PuzzleState.SOLVED, store.progress(puzzles[0].id).state)
        assertNull(store.progress(puzzles[0].id).bestSeconds)
        for ((i, p) in before) assertEquals("puzzle ${i + 1} changed", p, store.progress(puzzles[i].id))
    }
}
