package io.github.jamisuni.tangram.kernel.lock

import io.github.jamisuni.tangram.kernel.geometry.Golden
import io.github.jamisuni.tangram.kernel.geometry.GoldenPiece
import io.github.jamisuni.tangram.kernel.geometry.GoldenPuzzle
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Silhouette
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId

// SCAFFOLDING (TASK-005b, WO-001 design §8): shared helpers of the lock tests. A test helper, not a product
// file, and carrying no acceptance IDs.

/** The golden's puzzles in a stable order (13 files). */
internal val goldenPuzzles: List<GoldenPuzzle> get() = Golden.data.puzzles.values.sortedBy { it.id }

/** The silhouette of the puzzle, from its stored solution polygons. */
internal fun GoldenPuzzle.silhouette(): Silhouette = Silhouette(solution.map { it.polygon })

/** The solution piece [id], as stored. */
internal fun GoldenPuzzle.solutionPiece(id: PieceId): GoldenPiece =
    solution.first { it.piece == id }

/** The golden pose of a solution piece as a placed piece. */
internal fun GoldenPiece.placed(): PlacedPiece = PlacedPiece(piece, pose.turn, pose.mirrored, pose.at)

/** The float counterpart of an exact point. */
internal fun ExactPoint.vec(): Vec2 = Vec2(x.toDouble(), y.toDouble())
