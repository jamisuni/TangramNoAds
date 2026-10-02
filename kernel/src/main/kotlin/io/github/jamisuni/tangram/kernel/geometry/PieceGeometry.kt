package io.github.jamisuni.tangram.kernel.geometry

import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PieceShape
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Rational
import io.github.jamisuni.tangram.kernel.model.Turn
import io.github.jamisuni.tangram.kernel.model.localCorners

// TYPE-001 shapes and the TYPE-003 transform on exact numbers (design WO-001 §3, ADR-003).
// Convention, identical to `PieceSave.OnBoard`, Spec/03-puzzle-format.md §3 and tools/tangram_geom.py:
//   world = R(turn · 45°) · F(mirror) · local + at
// F mirrors x BEFORE the turn; y points down; a positive turn is clockwise on screen:
//   x' = x·c − y·s,  y' = x·s + y·c  with c = cos(turn·45°), s = sin(turn·45°) as exact Q2.

private val HALF_SQRT2: Q2 = Q2(Rational.ZERO, Rational.of(1, 2))
private val ZERO_Q2: Q2 = Q2.ZERO
private val ONE_Q2: Q2 = q2(1)

/** `COS` of tangram_geom.py: cos(k · 45°), k = 0..7. */
private val COS: List<Q2> = listOf(ONE_Q2, HALF_SQRT2, ZERO_Q2, -HALF_SQRT2, -ONE_Q2, -HALF_SQRT2, ZERO_Q2, HALF_SQRT2)

/** `SIN` of tangram_geom.py: sin(k · 45°), k = 0..7. */
private val SIN: List<Q2> = listOf(ZERO_Q2, HALF_SQRT2, ONE_Q2, HALF_SQRT2, ZERO_Q2, -HALF_SQRT2, -ONE_Q2, -HALF_SQRT2)

object PieceGeometry {

    /** The piece's area in units²: LT 4, MT 2, ST 1, SQ 2, PG 2 (the seven pieces sum to 16). */
    fun area(shape: PieceShape): Rational = when (shape) {
        PieceShape.LARGE_TRIANGLE -> Rational.of(4)
        PieceShape.MEDIUM_TRIANGLE -> Rational.of(2)
        PieceShape.SMALL_TRIANGLE -> Rational.of(1)
        PieceShape.SQUARE -> Rational.of(2)
        PieceShape.PARALLELOGRAM -> Rational.of(2)
    }

    /** R(turn)·F(mirror)·local for every local corner, in the shape's vertex order; `offsets[0]` is (0, 0). */
    fun offsets(shape: PieceShape, turn: Turn, mirrored: Boolean): List<ExactPoint> {
        val c = COS[turn.steps]
        val s = SIN[turn.steps]
        return shape.localCorners.map { local ->
            val x = if (mirrored) -local.x else local.x
            val y = local.y
            ExactPoint(x * c - y * s, x * s + y * c)
        }
    }

    /**
     * TYPE-003: the piece's centre (the average of its corners) relative to its local origin, as floats in
     * units. A turn keeps the centre fixed, so the origin moves as the centre offset changes.
     */
    fun centroidOffset(shape: PieceShape, turn: Turn, mirrored: Boolean): Vec2 {
        val offsets = offsets(shape, turn, mirrored)
        return Vec2(offsets.sumOf { it.x.toDouble() } / offsets.size, offsets.sumOf { it.y.toDouble() } / offsets.size)
    }

    /** Where the local origin lies (float, units) when [placed] takes ([turn], [mirrored]) with its centre kept fixed. */
    fun originKeepingCentre(placed: PlacedPiece, turn: Turn, mirrored: Boolean): Vec2 {
        val now = centroidOffset(placed.piece.shape, placed.turn, placed.mirrored)
        val next = centroidOffset(placed.piece.shape, turn, mirrored)
        return Vec2(placed.at.x.toDouble() + now.x - next.x, placed.at.y.toDouble() + now.y - next.y)
    }

    /** The world corners of [piece] with its local origin (vertex 0) at [at]: [offsets] + [at]. */
    fun corners(piece: PieceId, turn: Turn, mirrored: Boolean, at: ExactPoint): List<ExactPoint> =
        offsets(piece.shape, turn, mirrored).map { it + at }

    /**
     * O-01: the pose that yields [polygon] for [piece], or `null` when no pose does (G-10).
     * Port of `placement_from_polygon`: unmirrored before mirrored, turn ascending, each polygon
     * vertex in order as `at`; the first [PlacedPiece] whose corners equal [polygon] as a set of
     * exact points (and in size) wins. Symmetric pieces fit several poses; this order picks one
     * deterministically (decisions DA-6).
     */
    fun poseOf(piece: PieceId, polygon: List<ExactPoint>): PlacedPiece? {
        val shape = piece.shape
        if (polygon.size != shape.localCorners.size) return null
        val target = polygon.toSet()
        for (mirrored in listOf(false, true)) {
            for (steps in 0..7) {
                val turn = Turn(steps)
                val offs = offsets(shape, turn, mirrored)
                for (at in polygon) {
                    // offs[0] is (0, 0), so the corner at `at` is vertex 0 itself.
                    val moved = offs.map { it + at }
                    if (moved.toSet() == target) return PlacedPiece(piece, turn, mirrored, at)
                }
            }
        }
        return null
    }
}

/**
 * A piece locked on the board: `PieceSave.OnBoard` plus its id (the kernel cannot use the contract
 * type, `contracts` depends on `kernel`). [at] is where the piece's local origin (vertex 0) lies.
 */
data class PlacedPiece(val piece: PieceId, val turn: Turn, val mirrored: Boolean, val at: ExactPoint) {
    /** The exact world corners, in the shape's vertex order. */
    val corners: List<ExactPoint> = PieceGeometry.corners(piece, turn, mirrored, at)
}

/** A float point in puzzle units: the finger-driven pose, never an exact value (design WO-001 §4). */
data class Vec2(val x: Double, val y: Double)
