package io.github.jamisuni.tangram.devtools

import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.model.PieceId

/**
 * One solution piece for display (design WO-005, frozen seam D-2): its polygon in puzzle units
 * (the stored exact polygon converted to doubles), its fixed colour, and the label position.
 *
 * [label] is the polygon centroid. Every tangram piece is a triangle, a square or a parallelogram,
 * and for those the vertex mean equals the area centroid, so the vertex mean is used.
 */
data class SolutionShape(
    val piece: PieceId,
    val colour: Int,
    val polygon: List<Vec2>,
    val label: Vec2,
)
