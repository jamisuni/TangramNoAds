package io.github.jamisuni.tangram.browse

import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Silhouette
import io.github.jamisuni.tangram.kernel.layout.TrayRules
import io.github.jamisuni.tangram.kernel.lock.LockSearch
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState

/**
 * Makes saved progress safe to show (decision DA-50; G-10: stored data never crashes a launch).
 * The result is not written back; the next save does that.
 */
internal object ProgressRestore {

    fun sanitize(puzzle: Puzzle, saved: PuzzleProgress): PuzzleProgress {
        val own = puzzle.solution.map { it.piece }.toSet()
        val known = saved.pieces.filterKeys { it in own }
        return when (saved.state) {
            // a Solved puzzle never shows where pieces were
            PuzzleState.SOLVED -> saved.copy(pieces = emptyMap())
            // a New entry cannot have board pieces (impossible from the writer); they rest in the tray
            PuzzleState.NEW -> saved.copy(pieces = known.mapValues { (_, s) -> asTray(s) })
            PuzzleState.IN_PROGRESS -> saved.copy(pieces = sanitizeBoard(puzzle, known))
        }
    }

    private fun asTray(s: PieceSave): PieceSave = when (s) {
        is PieceSave.OnBoard -> PieceSave.InTray(s.turn, s.mirrored)
        is PieceSave.InTray -> s
    }

    private fun sanitizeBoard(puzzle: Puzzle, known: Map<PieceId, PieceSave>): Map<PieceId, PieceSave> {
        val silhouette = Silhouette(puzzle.solution.map { it.polygon })
        val accepted = ArrayList<PlacedPiece>()
        val result = LinkedHashMap<PieceId, PieceSave>()
        for (piece in TrayRules.order) {
            val s = known[piece] ?: continue
            if (s !is PieceSave.OnBoard) {
                result[piece] = s
                continue
            }
            // geometry may throw on out-of-range stored values: fail closed, the piece goes to the tray
            val placed = runCatching {
                val ok = LockSearch.isValidPlacement(silhouette, accepted, piece, s.turn, s.mirrored, s.at)
                if (ok) PlacedPiece(piece, s.turn, s.mirrored, s.at) else null
            }.getOrNull()
            if (placed != null) {
                accepted += placed
                result[piece] = s
            } else {
                result[piece] = PieceSave.InTray(s.turn, s.mirrored)
            }
        }
        return result
    }
}
