package io.github.jamisuni.tangram.play.acceptance

import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PieceShape
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Visible oracle helper (WO-001; held-out at WO-001 time, now an ordinary suite file). An oracle for "inside the silhouette" and "overlaps no other piece",
 * written in plain doubles from the published pose convention (design sec. 3: world = R(turn * 45 deg) * F(mirror) *
 * local + at; F mirrors x before rotating; y down; positive turn clockwise on screen) and the stored polygons -
 * deliberately NOT sharing any code with `kernel`'s exact clipping.
 *
 * REQ-019 A2 reads: a locked piece never overlaps another piece and never sticks out of the silhouette.
 * TYPE-004: "inside" and "overlaps" are decided with a tolerance of at most 1e-6 units.
 */
internal object HeldOutOracle {

    const val TOLERANCE = 1e-6

    private val LOCAL: Map<PieceShape, List<Pair<Double, Double>>> = mapOf(
        PieceShape.LARGE_TRIANGLE to listOf(0.0 to 0.0, 4.0 to 0.0, 2.0 to 2.0),
        PieceShape.MEDIUM_TRIANGLE to listOf(0.0 to 0.0, 2.0 to 0.0, 0.0 to 2.0),
        PieceShape.SMALL_TRIANGLE to listOf(0.0 to 0.0, 2.0 to 0.0, 1.0 to 1.0),
        PieceShape.SQUARE to listOf(0.0 to 0.0, 1.0 to -1.0, 2.0 to 0.0, 1.0 to 1.0),
        PieceShape.PARALLELOGRAM to listOf(0.0 to 0.0, 2.0 to 0.0, 3.0 to -1.0, 1.0 to -1.0),
    )

    /** Corners of a piece in a pose, in plain doubles. */
    fun corners(piece: PieceId, turn: Int, mirrored: Boolean, at: ExactPoint): List<Pair<Double, Double>> {
        val angle = Math.toRadians(45.0 * turn)
        val c = cos(angle)
        val s = sin(angle)
        val ax = at.x.toDouble()
        val ay = at.y.toDouble()
        return LOCAL.getValue(piece.shape).map { (x0, y0) ->
            val x = if (mirrored) -x0 else x0
            (x * c - y0 * s + ax) to (x * s + y0 * c + ay)
        }
    }

    fun corners(p: PlacedPiece): List<Pair<Double, Double>> = corners(p.piece, p.turn.steps, p.mirrored, p.at)

    fun polygon(points: List<ExactPoint>): List<Pair<Double, Double>> = points.map { it.x.toDouble() to it.y.toDouble() }

    fun area(poly: List<Pair<Double, Double>>): Double = abs(signedArea(poly))

    private fun signedArea(poly: List<Pair<Double, Double>>): Double {
        var sum = 0.0
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            sum += a.first * b.second - b.first * a.second
        }
        return sum / 2
    }

    /** Intersection area of two convex polygons (Sutherland-Hodgman). */
    fun overlapArea(subject: List<Pair<Double, Double>>, clipperIn: List<Pair<Double, Double>>): Double {
        val clipper = if (signedArea(clipperIn) < 0) clipperIn.reversed() else clipperIn
        var out: List<Pair<Double, Double>> = subject
        for (i in clipper.indices) {
            val a = clipper[i]
            val b = clipper[(i + 1) % clipper.size]
            val input = out
            val next = ArrayList<Pair<Double, Double>>()
            if (input.isEmpty()) return 0.0
            fun inside(p: Pair<Double, Double>) =
                (b.first - a.first) * (p.second - a.second) - (b.second - a.second) * (p.first - a.first) >= -1e-12
            fun cross(p: Pair<Double, Double>, q: Pair<Double, Double>): Pair<Double, Double> {
                val den = (p.first - q.first) * (a.second - b.second) - (p.second - q.second) * (a.first - b.first)
                val t = ((p.first - a.first) * (a.second - b.second) - (p.second - a.second) * (a.first - b.first)) / den
                return (p.first + t * (q.first - p.first)) to (p.second + t * (q.second - p.second))
            }
            var prev = input.last()
            for (cur in input) {
                if (inside(cur)) {
                    if (!inside(prev)) next.add(cross(prev, cur))
                    next.add(cur)
                } else if (inside(prev)) {
                    next.add(cross(prev, cur))
                }
                prev = cur
            }
            out = next
        }
        return if (out.size >= 3) area(out) else 0.0
    }

    /**
     * What is wrong with putting [locked] on the board of [puzzle] next to [others], or null when nothing is.
     * The silhouette is the union of the stored polygons (convex, with disjoint interiors), so the part of the piece
     * inside it is the sum of its overlaps with them.
     */
    fun violation(puzzle: Puzzle, others: List<PlacedPiece>, locked: PlacedPiece): String? {
        val poly = corners(locked)
        val inside = puzzle.solution.sumOf { overlapArea(poly, polygon(it.polygon)) }
        if (inside < area(poly) - TOLERANCE) {
            return "sticks out of the silhouette by ${area(poly) - inside} units^2: $locked"
        }
        for (other in others) {
            val common = overlapArea(poly, corners(other))
            if (common > TOLERANCE) return "overlaps ${other.piece} by $common units^2: $locked"
        }
        return null
    }
}
