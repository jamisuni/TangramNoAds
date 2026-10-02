package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleKind
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
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

open class DropResolver(private val puzzle: Puzzle) {
    private val silhouette = Silhouette(puzzle.solution.map { it.polygon })

    /** REQ-021: where a release would lock now; null = draw nothing. */
    open fun preview(pose: DragPose, placed: List<PlacedPiece>, dpPerUnit: Double): PlacedPiece? =
        search(pose, placed, dpPerUnit)?.let { placedAt(pose, it) }

    /** REQ-019 / REQ-020 / REQ-051. */
    open fun release(pose: DragPose, placed: List<PlacedPiece>, dpPerUnit: Double): DropOutcome {
        val lock = search(pose, placed, dpPerUnit)
        if (lock != null) return DropOutcome.Locked(placedAt(pose, lock))
        val pulse = if (puzzle.kind == PuzzleKind.MINI && pose.overBoard) CornerPulse(silhouette.outlineCorners) else null
        return DropOutcome.Home(pose.piece, pose.turn, pose.mirrored, pulse)
    }

    /**
     * REQ-016 / REQ-018 (TYPE-003 "a turn keeps the centre"): the board piece [current] takes ([turn], [mirrored])
     * about its centre and is kept only if the same lock search finds a spot; null = restore and shake.
     * The piece is filtered by its own id inside the search (F31), so it never blocks itself.
     */
    open fun refit(
        current: PlacedPiece,
        turn: Turn,
        mirrored: Boolean,
        placed: List<PlacedPiece>,
        dpPerUnit: Double,
    ): PlacedPiece? {
        val origin = PieceGeometry.originKeepingCentre(current, turn, mirrored)
        val lock = LockSearch.find(
            silhouette, placed, current.piece, turn, mirrored, origin, LockSearch.lockDistance(dpPerUnit),
        ) ?: return null
        return PlacedPiece(current.piece, turn, mirrored, lock.at)
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
