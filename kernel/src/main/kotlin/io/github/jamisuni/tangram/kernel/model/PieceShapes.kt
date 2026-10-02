package io.github.jamisuni.tangram.kernel.model

// TYPE-001: the local shape table of the five piece shapes (Spec/03-puzzle-format.md §3, same
// vertex order as `PIECE_TYPES` in tools/tangram_geom.py). Notify-tier contract-delta of WO-001:
// saved board positions depend on this table and on its vertex-0 convention (see below).

private fun corner(x: Long, y: Long): ExactPoint = ExactPoint(Q2.of(x), Q2.of(y))

private val LARGE_TRIANGLE_CORNERS: List<ExactPoint> = listOf(corner(0, 0), corner(4, 0), corner(2, 2))
private val MEDIUM_TRIANGLE_CORNERS: List<ExactPoint> = listOf(corner(0, 0), corner(2, 0), corner(0, 2))
private val SMALL_TRIANGLE_CORNERS: List<ExactPoint> = listOf(corner(0, 0), corner(2, 0), corner(1, 1))
private val SQUARE_CORNERS: List<ExactPoint> = listOf(corner(0, 0), corner(1, -1), corner(2, 0), corner(1, 1))
private val PARALLELOGRAM_CORNERS: List<ExactPoint> = listOf(corner(0, 0), corner(2, 0), corner(3, -1), corner(1, -1))

/**
 * The shape's vertices in its local frame at turn 0, unmirrored, in puzzle units (x right, y down).
 * Vertex 0 is (0, 0) for every shape, so `PieceSave.OnBoard.at` is exactly where vertex 0 lies:
 * world vertex = R(turn · 45°) · F(mirror) · local vertex + at (F mirrors x, before the turn).
 */
val PieceShape.localCorners: List<ExactPoint>
    get() = when (this) {
        PieceShape.LARGE_TRIANGLE -> LARGE_TRIANGLE_CORNERS
        PieceShape.MEDIUM_TRIANGLE -> MEDIUM_TRIANGLE_CORNERS
        PieceShape.SMALL_TRIANGLE -> SMALL_TRIANGLE_CORNERS
        PieceShape.SQUARE -> SQUARE_CORNERS
        PieceShape.PARALLELOGRAM -> PARALLELOGRAM_CORNERS
    }
