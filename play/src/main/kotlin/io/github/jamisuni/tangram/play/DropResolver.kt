package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleKind
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Silhouette
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.lock.Lock
import io.github.jamisuni.tangram.kernel.lock.LockSearch
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.Turn

// WO-001 design section 7: drop resolution, landing preview and corner pulse (REQ-019, REQ-020, REQ-021, REQ-051).
// Stateless: the board calls it (WO-003 calling rules: last displayed preview on release; an interrupted drag never calls it).

/** A dragged piece: pose, float origin (design section 4) and whether the finger is over the board. */
data class DragPose(
    val piece: PieceId,
    val turn: Turn,
    val mirrored: Boolean,
    val origin: Vec2,
    val overBoard: Boolean,
)

/** What a release can produce; nothing is ever loose (REQ-020). */
sealed interface DropOutcome {
    data class Locked(val placed: PlacedPiece) : DropOutcome

    data class Home(
        val piece: PieceId,
        val turn: Turn,
        val mirrored: Boolean,
        val pulse: CornerPulse?,
    ) : DropOutcome
}

/** The outline corners of the silhouette, nothing else (REQ-051). */
data class CornerPulse(val corners: List<ExactPoint>)

class DropResolver(private val puzzle: Puzzle) {
    private val silhouette = Silhouette(puzzle.solution.map { it.polygon })

    /** REQ-021: where a release would lock now; null = draw nothing. */
    fun preview(pose: DragPose, placed: List<PlacedPiece>, dpPerUnit: Double): PlacedPiece? =
        search(pose, placed, dpPerUnit)?.let { placedAt(pose, it) }

    /** REQ-019 / REQ-020 / REQ-051. */
    fun release(pose: DragPose, placed: List<PlacedPiece>, dpPerUnit: Double): DropOutcome {
        val lock = search(pose, placed, dpPerUnit)
        if (lock != null) return DropOutcome.Locked(placedAt(pose, lock))
        val pulse = if (puzzle.kind == PuzzleKind.MINI && pose.overBoard) CornerPulse(silhouette.outlineCorners) else null
        return DropOutcome.Home(pose.piece, pose.turn, pose.mirrored, pulse)
    }

    // The one search behind preview and release: they can never disagree (REQ-021).
    private fun search(pose: DragPose, placed: List<PlacedPiece>, dpPerUnit: Double): Lock? =
        if (!pose.overBoard) {
            null
        } else {
            LockSearch.find(
                silhouette, placed, pose.piece, pose.turn, pose.mirrored, pose.origin,
                LockSearch.lockDistance(dpPerUnit),
            )
        }

    private fun placedAt(pose: DragPose, lock: Lock) = PlacedPiece(pose.piece, pose.turn, pose.mirrored, lock.at)
}
