package io.github.jamisuni.tangram.kernel.geometry

import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.Q2

// The silhouette of a puzzle and its exact outline corners (design WO-001 §5, decisions DA-7; REQ-019
// "Anchor points are only the silhouette's outline corners ...", REQ-051 pulse corners).
//
// The rule (DA-7): a solution vertex p is an outline corner unless the directions covered by pieces around p
// form exactly one contiguous run of 4 wedges (a straight side) or all 8 wedges (inside the shape). A
// wedge k (k = 0..7) is the 45-degree sector between direction k and direction k+1; direction k has the
// angle k * 45 degrees clockwise on screen from +x (y points down). The old total-angle rule ("180 degrees
// covered = straight") is wrong where 180 degrees are covered in separate runs (shapes-warmup-4 at (2,2)).
//
// Everything here is exact (Q2 signs and equality); no double decides a corner. The Python reference
// tools/tangram_geom.py (float based, sanity checked) must agree on every puzzle file (the golden tests).

/**
 * The silhouette of a puzzle: its stored solution polygons, in any order and orientation. Every polygon
 * edge must be a multiple of 45 degrees (TYPE-001/003; validator V3 enforces it for shipped files);
 * anything else is a content error and fails a `require` when the silhouette is built.
 */
class Silhouette(val polygons: List<List<ExactPoint>>) {

    /**
     * The outline corners: distinct solution vertices where the outline turns, in first-appearance order
     * (as the Python reference); compare as a set.
     */
    val outlineCorners: List<ExactPoint> = computeOutlineCorners(polygons)
}

/**
 * DA-7. [mask] has 8 bits, bit k set = wedge k is covered by a piece. Not an outline corner exactly when
 * the covered wedges are all 8 or one contiguous (circular) run of 4; every other mask is a corner.
 */
internal fun isOutlineCornerMask(mask: Int): Boolean {
    val covered = mask and FULL_MASK
    if (covered == FULL_MASK) return false
    return (0 until WEDGES).none { covered == wedgeRun(it, 4) }
}

private const val WEDGES = 8
private const val FULL_MASK = 0xFF

/** Bit mask of [count] consecutive wedges starting at wedge [start] (mod 8). */
private fun wedgeRun(start: Int, count: Int): Int {
    var mask = 0
    for (j in 0 until count) mask = mask or (1 shl ((start + j) % WEDGES))
    return mask
}

private fun computeOutlineCorners(polygons: List<List<ExactPoint>>): List<ExactPoint> {
    for (polygon in polygons) require(polygon.size >= 3) { "a solution polygon needs at least 3 vertices: $polygon" }
    val seen = HashSet<ExactPoint>()
    val corners = ArrayList<ExactPoint>()
    for (polygon in polygons) {
        for (p in polygon) {
            if (!seen.add(p)) continue
            var mask = 0
            for (q in polygons) mask = mask or wedgeMaskAt(p, q)
            if (isOutlineCornerMask(mask)) corners.add(p)
        }
    }
    return corners
}

/**
 * The direction of the vector ([dx], [dy]) as an octant 0..7 (angle octant * 45 degrees clockwise on
 * screen from +x, y down), from exact signs. Axis aligned, or |dx| == |dy|; anything else (or the zero
 * vector) fails a `require`: a content error.
 */
private fun octant(dx: Q2, dy: Q2): Int {
    val sx = dx.signum()
    val sy = dy.signum()
    require(sx != 0 || sy != 0) { "zero-length polygon edge" }
    if (sx != 0 && sy != 0) {
        require(dx == dy || dx == -dy) { "polygon edge ($dx, $dy) is not a multiple of 45 degrees" }
    }
    return when {
        sx > 0 && sy == 0 -> 0
        sx > 0 && sy > 0 -> 1
        sx == 0 && sy > 0 -> 2
        sx < 0 && sy > 0 -> 3
        sx < 0 && sy == 0 -> 4
        sx < 0 && sy < 0 -> 5
        sx == 0 && sy < 0 -> 6
        else -> 7
    }
}

/** The wedges (bit mask) that the convex polygon [q] covers around the exact point [p]; 0 if none. */
private fun wedgeMaskAt(p: ExactPoint, q: List<ExactPoint>): Int {
    val n = q.size
    val vertex = q.indexOf(p)
    if (vertex >= 0) {
        // p is vertex i of q: the interior sector is the short arc between the two edge directions.
        val previous = q[(vertex + n - 1) % n]
        val next = q[(vertex + 1) % n]
        val ou = octant(previous.x - p.x, previous.y - p.y)
        val ow = octant(next.x - p.x, next.y - p.y)
        val d = Math.floorMod(ow - ou, WEDGES)
        return if (d <= 4) wedgeRun(ou, d) else wedgeRun(ow, Math.floorMod(ou - ow, WEDGES))
    }
    for (i in 0 until n) {
        val a = q[i]
        val b = q[(i + 1) % n]
        val ex = b.x - a.x
        val ey = b.y - a.y
        val px = p.x - a.x
        val py = p.y - a.y
        // p strictly inside the edge a-b: collinear (cross == 0) and 0 < dot < |edge|^2, all exact.
        if ((ex * py - ey * px).signum() != 0) continue
        val dot = ex * px + ey * py
        val lengthSquared = ex * ex + ey * ey
        if (dot.signum() <= 0 || (lengthSquared - dot).signum() <= 0) continue
        // The half-plane on q's side: the sign of the cross product against an off-line vertex of q.
        val o = octant(ex, ey)
        var side = 0
        for (v in q) {
            side = (ex * (v.y - a.y) - ey * (v.x - a.x)).signum()
            if (side != 0) break
        }
        require(side != 0) { "degenerate polygon (all vertices on one line): $q" }
        return wedgeRun(if (side > 0) o else o + 4, 4)
    }
    return 0
}
