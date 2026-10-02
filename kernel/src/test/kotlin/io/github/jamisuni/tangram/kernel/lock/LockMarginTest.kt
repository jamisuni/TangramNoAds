package io.github.jamisuni.tangram.kernel.lock

import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Silhouette
import io.github.jamisuni.tangram.kernel.geometry.minus
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PieceShape
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SCAFFOLDING (TASK-005b, WO-001 design §8, decisions DA-3, design review F2 and its spot-check Note): the
 * margin test. TYPE-004's 1e-6 tolerance is applied to clip AREA (units²); that is equivalent to a length
 * tolerance only while no reachable placement has a nonzero offending area anywhere near it. So over every
 * golden puzzle no `Fit` value (`insideDeficit`, `maxOverlap`) evaluated on the sweep below may lie in
 * (1e-9, 1e-4): flush contact is exactly 0 (or float noise, far below 1e-9) and every real misfit is far above
 * 1e-4. A future puzzle that approaches the tolerance fails here, loudly, instead of locking wrongly.
 *
 * The sweep, per puzzle: every `at = anchor - offset` (all offsets, all anchors of that board, NO distance
 * limit; one candidate per exact `at`) of every piece not on the board, every turn and mirror, against
 *  (i)   the empty board,
 *  (ii)  every build-order prefix at the golden poses,
 *  (iii) every board of exactly one piece: each solution piece at its golden pose, AND (the review's Note) each
 *        piece at every valid NON-solution position found on the empty board,
 *  (iv)  (extra) every board of the other six pieces: the last hole.
 * Same-shape pieces give identical values, so a moving piece stands for its shape (the first PieceId of that
 * shape that is not on the board). No acceptance IDs.
 */
class LockMarginTest {

    private companion object {
        /** At or below: flush contact / float noise. */
        const val NOISE = 1e-9

        /** At or above: a real misfit. */
        const val MARGIN = 1e-4
    }

    private class Stats {
        var boards = 0
        var evaluated = 0L
        var smallestReal = Double.POSITIVE_INFINITY
        var smallestRealWhere = ""
        var largestNoise = 0.0
        var largestNoiseWhere = ""
        val violations = ArrayList<String>()

        fun record(value: Double, where: String) {
            evaluated++
            if (value > NOISE) {
                if (value < smallestReal) {
                    smallestReal = value
                    smallestRealWhere = where
                }
            } else if (Math.abs(value) > largestNoise) {
                largestNoise = Math.abs(value)
                largestNoiseWhere = where
            }
            // NaN fails both comparisons and is a violation too.
            val ok = (value <= NOISE && value >= -NOISE) || value >= MARGIN
            if (!ok && violations.size < 20) violations += "$where: $value"
        }
    }

    /** One moving piece per shape that is not on the board: same-shape pieces give identical fits. */
    private fun movingPieces(onBoard: Set<PieceId>): List<PieceId> =
        PieceShape.values().mapNotNull { shape -> PieceId.values().firstOrNull { it.shape == shape && it !in onBoard } }

    /**
     * Sweeps one board. Returns the valid placements it found for the moving pieces (used on the empty board
     * to build the widened one-piece boards of (iii)).
     */
    private fun sweep(label: String, silhouette: Silhouette, board: List<PlacedPiece>, stats: Stats): List<PlacedPiece> {
        stats.boards++
        val anchors = LinkedHashSet<ExactPoint>().apply {
            addAll(silhouette.outlineCorners)
            for (p in board) addAll(p.corners)
        }
        val valid = ArrayList<PlacedPiece>()
        for (piece in movingPieces(board.map { it.piece }.toSet())) {
            for (mirrored in listOf(false, true)) {
                for (steps in 0..7) {
                    val turn = Turn(steps)
                    val seen = HashSet<ExactPoint>()
                    for (offset in PieceGeometry.offsets(piece.shape, turn, mirrored)) {
                        for (anchor in anchors) {
                            val at = anchor - offset
                            if (!seen.add(at)) continue
                            val fit = LockSearch.fitAt(silhouette, board, piece, turn, mirrored, at)
                            val where = "$label $piece turn=$steps mirrored=$mirrored at=$at"
                            stats.record(fit.insideDeficit, "$where insideDeficit")
                            stats.record(fit.maxOverlap, "$where maxOverlap")
                            if (fit.insideDeficit <= LockSearch.TOLERANCE && fit.maxOverlap <= LockSearch.TOLERANCE) {
                                valid += PlacedPiece(piece, turn, mirrored, at)
                            }
                        }
                    }
                }
            }
        }
        return valid
    }

    @Test
    fun noFitValueLiesBetweenTheNoiseFloorAndTheMargin() {
        val stats = Stats()
        for (puzzle in goldenPuzzles) {
            val silhouette = puzzle.silhouette()
            val solution = puzzle.buildOrder.map { puzzle.solutionPiece(it).placed() }

            // (i) the empty board; its valid placements are the one-piece boards of (iii), widened.
            val emptyBoardValid = sweep("${puzzle.id} empty", silhouette, emptyList(), stats)
            // (ii) every build-order prefix at the golden poses (length 1..6; 0 is (i), 7 has no moving piece).
            for (n in 1 until solution.size) sweep("${puzzle.id} prefix$n", silhouette, solution.take(n), stats)
            // (iii) each solution piece alone at its golden pose, and every valid empty-board placement alone.
            val oneBoards = LinkedHashSet<PlacedPiece>()
            for (piece in solution) oneBoards += piece
            oneBoards += emptyBoardValid
            for (piece in oneBoards) sweep("${puzzle.id} alone[$piece]", silhouette, listOf(piece), stats)
            // (iv) the last hole: every piece missing from the full solution.
            for (missing in solution) {
                sweep("${puzzle.id} hole${missing.piece}", silhouette, solution.filter { it.piece != missing.piece }, stats)
            }
        }
        println(
            "margin test: puzzles=${goldenPuzzles.size} boards=${stats.boards} fitsEvaluated=${stats.evaluated} " +
                "smallestNonzeroArea=${stats.smallestReal} (${stats.smallestRealWhere}) " +
                "largestNoise=${stats.largestNoise} (${stats.largestNoiseWhere})",
        )
        assertEquals("fit values outside (<= 1e-9 | >= 1e-4): ${stats.violations}", 0, stats.violations.size)
        assertTrue("the sweep ran (${stats.evaluated} fits)", stats.evaluated > 100_000)
        // The design review measured 1.263e-3 on the first 13 files; a margin below that figure means the
        // sweep or the geometry changed and DA-3 must be looked at again.
        assertTrue("smallest nonzero area ${stats.smallestReal}", stats.smallestReal >= MARGIN)
    }
}
