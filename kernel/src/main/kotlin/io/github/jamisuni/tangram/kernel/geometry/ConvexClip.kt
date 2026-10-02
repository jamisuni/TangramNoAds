package io.github.jamisuni.tangram.kernel.geometry

// TYPE-004 validity primitive (design WO-001 §6, "The clip contract"; internal, ADR-002).
// Port of the prototype's `polyArea` / `ccw` / `clip` / `interArea` (tools/prototype_template.html 219-234).
//
// Every lock is decided at exact flush contact, where a clip edge is collinear with an edge of the
// subject polygon. So the contract is pinned, and where the prototype and the design differ the design wins:
//  - both polygons are oriented so that the shoelace sum is >= 0; the subject is clipped against each
//    edge A->B of the clipper in turn (Sutherland-Hodgman);
//  - side(P) = (B - A) x (P - A); a vertex is inside INCLUSIVELY: side(P) >= -CLIP_SIDE_EPS;
//  - for a subject edge P->Q: emit P when it is inside, and emit the crossing P + t (Q - P),
//    t = sp / (sp - sq), only when EXACTLY ONE of P, Q is inside. On such a strict sign change
//    sp - sq is nonzero (one value is >= -eps, the other < -eps), so there is no 0/0 anywhere;
//    t is clamped to [0, 1] (the one deviation from the prototype, which does not clamp);
//  - no line-line intersection (cross of the two direction vectors) anywhere: it divides by zero exactly
//    on collinear edges and silently yields NaN or infinity in Kotlin doubles;
//  - area = |shoelace| / 2 of the result, 0 when the result has fewer than 3 vertices; always finite.
// One addition to the contract, for degenerate input only (a clipper whose edges are all shorter than
// the side tolerance constrains nothing and would return the whole subject): the result never exceeds
// the area of either input, so a zero-area polygon gives 0. For a real overlap it changes nothing.

internal object ConvexClip {

    /** A vertex is inside a clip edge when `side >= -CLIP_SIDE_EPS` (inclusive: flush contact stays inside). */
    const val CLIP_SIDE_EPS: Double = 1e-9

    /**
     * The area of the intersection of two convex polygons, in the polygons' own units.
     * Either orientation (clockwise or counter-clockwise) of either polygon is accepted.
     * Returns 0.0 for an input of fewer than 3 vertices, for flush or single-point contact and for a
     * zero-area input; never NaN or infinite.
     */
    fun intersectionArea(a: List<Vec2>, b: List<Vec2>): Double {
        if (a.size < 3 || b.size < 3) return 0.0
        val subject = oriented(a)
        val clipper = oriented(b)

        var out: List<Vec2> = subject
        var i = 0
        while (i < clipper.size && out.isNotEmpty()) {
            val ea = clipper[i]
            val eb = clipper[(i + 1) % clipper.size]
            val input = out
            val next = ArrayList<Vec2>(input.size + 2)
            for (j in input.indices) {
                val p = input[j]
                val q = input[(j + 1) % input.size]
                val sp = side(ea, eb, p)
                val sq = side(ea, eb, q)
                val pInside = sp >= -CLIP_SIDE_EPS
                val qInside = sq >= -CLIP_SIDE_EPS
                if (pInside) next.add(p)
                if (pInside != qInside) {
                    // Strict sign change: sp - sq != 0, so this is never 0/0.
                    val t = (sp / (sp - sq)).coerceIn(0.0, 1.0)
                    next.add(Vec2(p.x + t * (q.x - p.x), p.y + t * (q.y - p.y)))
                }
            }
            out = next
            i++
        }
        if (out.size < 3) return 0.0
        val area = minOf(Math.abs(shoelace(out)), Math.abs(shoelace(subject)), Math.abs(shoelace(clipper))) / 2.0
        // Non-finite coordinates in the input (never from a placed piece) must not leak out as NaN.
        return if (area.isFinite()) area else 0.0
    }

    /** `(B - A) x (P - A)`: >= 0 on the inner side of the edge A->B for a polygon with shoelace sum >= 0. */
    private fun side(a: Vec2, b: Vec2, p: Vec2): Double =
        (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)

    /** Twice the signed area (the shoelace sum). */
    private fun shoelace(polygon: List<Vec2>): Double {
        var sum = 0.0
        for (i in polygon.indices) {
            val p = polygon[i]
            val q = polygon[(i + 1) % polygon.size]
            sum += p.x * q.y - q.x * p.y
        }
        return sum
    }

    /** The polygon with its shoelace sum >= 0 (reversed when negative), as the prototype's `ccw`. */
    private fun oriented(polygon: List<Vec2>): List<Vec2> =
        if (shoelace(polygon) < 0.0) polygon.reversed() else polygon
}
