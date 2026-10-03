package io.github.jamisuni.tangram.held

import io.github.jamisuni.tangram.SessionHost
import io.github.jamisuni.tangram.browse.BrowseController
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.devtools.DevSolution
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.play.PlaySession
import io.github.jamisuni.tangram.store.JsonProgressStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * HELD-OUT (Test & Verify only): REQ-046.A3 through the whole JVM chain, for ALL packaged puzzles: the stored solution that
 * `DevSolution.poses` hands over is accepted by the real `PlaySession.solveByAid`, and the real `BrowseController` writes through
 * the real `JsonProgressStore` an entry that is Solved with the best time untouched (empty for a never-solved puzzle, an earlier
 * one kept). Self-contained; the stored document is read back through a NEW `JsonProgressStore` on the same folder, what a
 * restarted app reads. The frozen fixtures are never touched.
 *
 * The host is the app's real `SessionHost`; the aid solve is followed by one frame and `BrowseController.persist()` (the existing
 * save path that `onChanged` drives in the app); the device test covers the wiring through the real screen.
 */
class HeldSolveNowStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val library = PuzzleLibrary.packaged()
    private val puzzles = library.puzzles

    private class Chain(val dir: File, library: io.github.jamisuni.tangram.contracts.puzzle.IPuzzleLibrary) {
        val byAid = ArrayList<Boolean>()
        val store = JsonProgressStore(dir)
        val host = SessionHost({ p, changed -> PlaySession(puzzle = p, onChanged = changed, onSolved = { byAid.add(it) }) }) { s, pr -> s.restore(pr) }
        val controller = BrowseController(library, store, host)
    }

    private fun inProgress(p: Puzzle, seconds: Long, best: Long?): PuzzleProgress {
        val pieces: Map<PieceId, PieceSave> = p.solution.take((p.solution.size - 1).coerceIn(1, 3)).associate {
            val pose = io.github.jamisuni.tangram.kernel.geometry.PieceGeometry.poseOf(it.piece, it.polygon) ?: error("no pose for ${it.piece}")
            it.piece to PieceSave.OnBoard(pose.at, pose.turn, pose.mirrored)
        }
        return PuzzleProgress(PuzzleState.IN_PROGRESS, pieces, seconds, best)
    }

    /** Seeds [p] with [seed] (null = fresh), opens it, uses the aid on it, persists; returns the chain after the aid solve. */
    private fun useTheAid(index: Int, seed: PuzzleProgress?, others: Map<Int, PuzzleProgress> = emptyMap()): Chain {
        val p = puzzles[index]
        val dir = tmp.newFolder()
        JsonProgressStore(dir).apply {
            if (seed != null) saveProgress(p.id, seed)
            for ((i, pr) in others) saveProgress(puzzles[i].id, pr)
            saveLastShownPuzzle(p.id)
        }
        val chain = Chain(dir, library)
        chain.controller.start()
        assertEquals("${p.id.value}: the seeded puzzle is the one shown", index, chain.controller.index)
        val session = chain.host.session ?: error("${p.id.value}: the host built no session")
        val poses = DevSolution.poses(p) ?: error("${p.id.value}: the aid has no poses")
        assertTrue("${p.id.value}: the engine refused the stored solution", session.solveByAid(poses))
        session.onFrame(1000)
        chain.controller.persist()
        assertEquals("${p.id.value}: onSolved(byAid = true) exactly once", listOf(true), chain.byAid)
        return chain
    }

    // REQ-046.A3 - "'Solve this puzzle now' shows the solved picture and leaves the best time empty."
    // Every packaged puzzle, never solved before (nothing stored): the stored entry is Solved and its best time is empty.
    @Test
    fun req046_A3_everyPuzzleSolvedByTheAidIsStoredSolvedWithNoBestTime() {
        assertTrue("fixture: the library is empty", puzzles.isNotEmpty())
        for ((i, p) in puzzles.withIndex()) {
            val chain = useTheAid(i, seed = null)
            val back = JsonProgressStore(chain.dir).progress(p.id)
            assertEquals("${p.id.value}: state", PuzzleState.SOLVED, back.state)
            assertNull("${p.id.value}: the aid set a best time", back.bestSeconds)
        }
    }

    // REQ-046.A3 - a puzzle 77 s into play (pieces down, never solved): an ordinary solve would store 77; the aid stores no best time.
    @Test
    fun req046_A3_aPuzzleInProgressSolvedByTheAidStillHasNoBestTime() {
        for ((i, p) in puzzles.withIndex()) {
            val chain = useTheAid(i, seed = inProgress(p, 77, null))
            val back = JsonProgressStore(chain.dir).progress(p.id)
            assertEquals("${p.id.value}: state", PuzzleState.SOLVED, back.state)
            assertNull("${p.id.value}: the aid set a best time", back.bestSeconds)
        }
    }

    // REQ-046.A3 - an earlier best time is kept: 600 s earlier, 77 s into a replay (an ordinary solve would lower it to 77), and
    // 41 s kept through a Retry (New, no pieces).
    @Test
    fun req046_A3_anEarlierBestTimeIsKeptForEveryPuzzle() {
        for ((i, p) in puzzles.withIndex()) {
            val replay = useTheAid(i, seed = inProgress(p, 77, 600))
            assertEquals("${p.id.value}: replay", 600L, JsonProgressStore(replay.dir).progress(p.id).bestSeconds)
            assertEquals(PuzzleState.SOLVED, JsonProgressStore(replay.dir).progress(p.id).state)

            val retried = useTheAid(i, seed = PuzzleProgress(PuzzleState.NEW, emptyMap(), 0, 41))
            assertEquals("${p.id.value}: after Retry", 41L, JsonProgressStore(retried.dir).progress(p.id).bestSeconds)
            assertEquals(PuzzleState.SOLVED, JsonProgressStore(retried.dir).progress(p.id).state)
        }
    }

    // REQ-046.A3 - the aid changes the solved puzzle's entry and nothing else: other puzzles' stored progress is as it was.
    @Test
    fun req046_A3_otherPuzzlesStoredProgressIsUntouched() {
        val others = mapOf(
            1 to PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 47, 41),
            2 to inProgress(puzzles[2], 12, 9),
            4 to PuzzleProgress(PuzzleState.NEW, emptyMap(), 0, 5),
        )
        val chain = useTheAid(0, seed = null, others = others)
        val back = JsonProgressStore(chain.dir)
        for ((i, pr) in others) {
            val now = back.progress(puzzles[i].id)
            assertEquals("puzzle ${i + 1}: state", pr.state, now.state)
            assertEquals("puzzle ${i + 1}: best", pr.bestSeconds, now.bestSeconds)
            assertEquals("puzzle ${i + 1}: seconds", pr.puzzleSeconds, now.puzzleSeconds)
        }
    }
}
