package io.github.jamisuni.tangram.kernel.layout

import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PieceShape
import io.github.jamisuni.tangram.kernel.model.Turn

/** TYPE-001 / REQ-012 / REQ-013 / REQ-043 table facts: one definition for tray, board pieces and thumbnails. */
object TrayRules {
    /** Tray order, left to right and top to bottom. */
    val order: List<PieceId> = PieceId.entries.toList()

    /** Resting turn in the tray (unmirrored): triangles long side down, square upright, parallelogram leaning right. */
    fun restingTurn(shape: PieceShape): Turn = when (shape) {
        PieceShape.LARGE_TRIANGLE -> Turn(4)
        PieceShape.MEDIUM_TRIANGLE -> Turn(1)
        PieceShape.SMALL_TRIANGLE -> Turn(4)
        PieceShape.SQUARE -> Turn(1)
        PieceShape.PARALLELOGRAM -> Turn(0)
    }

    /** Fixed colour as opaque ARGB (TYPE-001). */
    fun colour(piece: PieceId): Int = when (piece) {
        PieceId.LT1 -> 0xFFE8505B
        PieceId.LT2 -> 0xFF3D8BFD
        PieceId.MT -> 0xFFF9A826
        PieceId.SQ -> 0xFFFFD23F
        PieceId.PG -> 0xFFFF7AB8
        PieceId.ST1 -> 0xFF2DBE7E
        PieceId.ST2 -> 0xFF9B5DE5
    }.toInt()

    /**
     * REQ-013 turn diameter in units: twice the largest centre-to-corner distance over all turns and mirrors
     * (LT 4.22, PG 3.16, MT 2.98, ST 2.11, SQ 2.00). A turn keeps the centre, so this is the cell content size.
     */
    fun turnDiameter(shape: PieceShape): Double {
        var max = 0.0
        for (mirrored in listOf(false, true)) {
            for (steps in 0..7) {
                val c = PieceGeometry.centroidOffset(shape, Turn(steps), mirrored)
                for (o in PieceGeometry.offsets(shape, Turn(steps), mirrored)) {
                    max = maxOf(max, Math.hypot(o.x.toDouble() - c.x, o.y.toDouble() - c.y))
                }
            }
        }
        return 2 * max
    }
}
