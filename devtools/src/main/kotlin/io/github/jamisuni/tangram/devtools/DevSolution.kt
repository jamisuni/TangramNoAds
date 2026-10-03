package io.github.jamisuni.tangram.devtools

import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.TrayRules

/**
 * The stored solution of a puzzle: one shape per solution piece, and the placed pieces.
 * Never reads puzzle.solution to choose poses; that stays in devtools.
 * decision DA-6: poseOf picks deterministically.
 */
object DevSolution {

    /**
     * The solution overlay: one shape per piece in the puzzle's solution.
     * Colour is TrayRules.colour; polygon is the stored polygon as doubles; label is the vertex mean.
     */
    fun shapes(puzzle: Puzzle): List<SolutionShape> = puzzle.solution.map { solution ->
        val polygon = solution.polygon.map { Vec2(it.x.toDouble(), it.y.toDouble()) }
        SolutionShape(
            piece = solution.piece,
            colour = TrayRules.colour(solution.piece),
            polygon = polygon,
            label = Vec2(polygon.sumOf { it.x } / polygon.size, polygon.sumOf { it.y } / polygon.size),
        )
    }

    /**
     * The placed pieces for a solve-now: one per solution entry, derived via poseOf.
     * Returns null if any piece has no pose (G-10), never placed unless solveByAid validates it.
     */
    fun poses(puzzle: Puzzle): List<PlacedPiece>? {
        val result = mutableListOf<PlacedPiece>()
        for (solution in puzzle.solution) {
            val pose = PieceGeometry.poseOf(solution.piece, solution.polygon) ?: return null
            result.add(pose)
        }
        return result
    }
}
