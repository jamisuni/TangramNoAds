package io.github.jamisuni.tangram.kernel.lock

import io.github.jamisuni.tangram.kernel.geometry.ConvexClip
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.READING_ORDER
import io.github.jamisuni.tangram.kernel.geometry.Silhouette
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.geometry.minus
import io.github.jamisuni.tangram.kernel.geometry.plus
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.Turn

// TYPE-004: where a dropped piece locks (design WO-001 §4 and §6; architecture O-01: the one search for drop,
// preview and later tap-turn / mirror). Everything that decides a lock is exact: a candidate position is
// `at = anchor - offset` (exact), identified by its exact `at`. Doubles appear only in the distance to the
// finger-driven float pose, in the clip areas of the validity test and in the score.

/**
 * The winning spot of a search: where the piece's local origin (vertex 0) locks, exactly, and how it was
 * chosen. [distance] is |at - origin| in puzzle units; [cornersOnAnchors] counts the piece's corners that
 * land exactly on an anchor.
 */
data class Lock(val at: ExactPoint, val distance: Double, val cornersOnAnchors: Int) {
    /** TYPE-004: `|t| - 0.04 x (corners on anchors)`; the lowest score wins. */
    val score: Double get() = distance - LockSearch.SNUG_BONUS * cornersOnAnchors
}

/** How a candidate position misfits, in areas (units²): the part of the piece outside the silhouette and its worst overlap. */
internal data class Fit(val insideDeficit: Double, val maxOverlap: Double)

/** A candidate position (`at`, exact) and its distance to the float pose. */
internal class Candidate(val at: ExactPoint, val distance: Double)

object LockSearch {
    const val BASE_DISTANCE = 0.65
    const val MIN_DP = 30.0
    const val SNUG_BONUS = 0.04
    const val WINDOW = 0.16

    /** Area tolerance of "inside" and "overlaps" (DA-3: applied to clip area, as the prototype; the margin test guards it). */
    const val TOLERANCE = 1e-6

    /** Two scores or distances closer than this are a tie (DA-1). */
    const val TIE_EPS = 1e-9

    /**
     * R in puzzle units: `max(0.65, 30 / dpPerUnit)`, never below 30 dp on screen. A non-positive or NaN
     * scale gives 0.65 (no crash path from layout).
     */
    fun lockDistance(dpPerUnit: Double): Double {
        if (!(dpPerUnit > 0.0)) return BASE_DISTANCE
        return maxOf(BASE_DISTANCE, MIN_DP / dpPerUnit)
    }

    /**
     * The TYPE-004 search. [origin] is the float counterpart of `at` (where the piece's local origin lies
     * now, in puzzle units, design §4). Only [turn] and [mirrored] are tried: a piece one step off, or the
     * other mirror image, never locks. The anchors are the silhouette's outline corners and the corners of
     * every placed piece except [piece] itself (F31: filtered by id here, so drop, preview and later
     * tap-turn / mirror share the rule). Returns `null` when no valid spot lies within [lockDistance]
     * (G-10: also for NaN or huge input; nothing here throws for touch input).
     */
    fun find(
        silhouette: Silhouette,
        placed: List<PlacedPiece>,
        piece: PieceId,
        turn: Turn,
        mirrored: Boolean,
        origin: Vec2,
        lockDistance: Double,
    ): Lock? {
        val others = placed.filter { it.piece != piece }
        val offsets = PieceGeometry.offsets(piece.shape, turn, mirrored)
        val anchors = HashSet<ExactPoint>()
        anchors.addAll(silhouette.outlineCorners)
        for (other in others) anchors.addAll(other.corners)

        var best: Lock? = null
        var nearest = Double.NaN // the smallest |t| among valid candidates, once there is one (DA-2)
        for (candidate in candidates(anchors, offsets, origin, lockDistance)) {
            // The window (TYPE-004) only stops the scan: a candidate beyond it can never win (design §6).
            if (!nearest.isNaN() && candidate.distance > nearest + WINDOW) break
            val fit = fitAt(silhouette, others, piece, turn, mirrored, candidate.at)
            if (!(fit.insideDeficit <= TOLERANCE && fit.maxOverlap <= TOLERANCE)) continue
            if (nearest.isNaN()) nearest = candidate.distance
            val snug = offsets.count { (it + candidate.at) in anchors }
            val lock = Lock(candidate.at, candidate.distance, snug)
            val current = best
            if (current == null || beats(lock, current)) best = lock
        }
        return best
    }

    /**
     * TYPE-004 validity of one placement, the lock search's own rule (O-01; DA-50): [piece] at ([turn],
     * [mirrored], [at]) is inside the [silhouette] and overlaps none of [others] (an entry with the piece's own
     * id is ignored), within [TOLERANCE]. Unlike [find] it does not need an anchor to touch. Fail-closed
     * (G-10): non-finite input or any exception gives `false`, never a throw.
     */
    fun isValidPlacement(
        silhouette: Silhouette,
        others: List<PlacedPiece>,
        piece: PieceId,
        turn: Turn,
        mirrored: Boolean,
        at: ExactPoint,
    ): Boolean = try {
        val fit = fitAt(silhouette, others, piece, turn, mirrored, at)
        fit.insideDeficit <= TOLERANCE && fit.maxOverlap <= TOLERANCE
    } catch (e: Exception) {
        false
    }

    /**
     * DA-1 (TYPE-004 has no tie-break): [candidate] beats [best] when its score is lower by more than
     * [TIE_EPS]; else, scores within [TIE_EPS], when its distance is lower by more than [TIE_EPS]; else,
     * distances within [TIE_EPS], when its `at` is earlier in reading order (smaller y, then smaller x).
     */
    private fun beats(candidate: Lock, best: Lock): Boolean {
        if (candidate.score < best.score - TIE_EPS) return true
        if (candidate.score > best.score + TIE_EPS) return false
        if (candidate.distance < best.distance - TIE_EPS) return true
        if (candidate.distance > best.distance + TIE_EPS) return false
        return READING_ORDER.compare(candidate.at, best.at) < 0
    }
}

/**
 * The candidates of TYPE-004: `at = anchor - offset` for every offset and every anchor, kept when
 * `|at - origin| <= lockDistance` (plain double compare). A candidate is identified by its exact `at`: the
 * same `at` reached by several corner/anchor pairs is one candidate. Ordered by distance ascending, then by
 * [READING_ORDER] of `at`, so the scan never depends on the order of the inputs. NaN anywhere keeps nothing.
 */
internal fun LockSearch.candidates(
    anchors: Iterable<ExactPoint>,
    offsets: List<ExactPoint>,
    origin: Vec2,
    lockDistance: Double,
): List<Candidate> {
    val seen = HashSet<ExactPoint>()
    val kept = ArrayList<Candidate>()
    for (offset in offsets) {
        for (anchor in anchors) {
            val at = anchor - offset
            if (!seen.add(at)) continue
            val distance = Math.hypot(at.x.toDouble() - origin.x, at.y.toDouble() - origin.y)
            if (distance <= lockDistance) kept.add(Candidate(at, distance))
        }
    }
    kept.sortWith { a, b ->
        val byDistance = a.distance.compareTo(b.distance)
        if (byDistance != 0) byDistance else READING_ORDER.compare(a.at, b.at)
    }
    return kept
}

/**
 * How well [piece] at ([turn], [mirrored], [at]) fits (TYPE-004 validity, design §6): `insideDeficit = area(P) -
 * sum_j area(P ∩ S_j)` over the silhouette polygons (disjoint, V4, and P and every S_j convex, so the sum is
 * the area of P within the silhouette); `maxOverlap` = the largest `area(P ∩ Q)` over [others] (0 with none).
 * [others] are the placed pieces to test against; an entry with the moving piece's own id is ignored.
 * A spot is valid when both values are at most [LockSearch.TOLERANCE]. Internal: the margin test reads it.
 */
internal fun LockSearch.fitAt(
    silhouette: Silhouette,
    others: List<PlacedPiece>,
    piece: PieceId,
    turn: Turn,
    mirrored: Boolean,
    at: ExactPoint,
): Fit {
    val subject = PieceGeometry.corners(piece, turn, mirrored, at).map(::toVec2)
    return misfit(
        subject,
        silhouette.polygons.map { polygon -> polygon.map(::toVec2) },
        others.filter { it.piece != piece }.map { other -> other.corners.map(::toVec2) },
        PieceGeometry.area(piece.shape).toDouble(),
    )
}

/**
 * The pure core of [fitAt] over float polygons (design WO-003 6.1, DA-25). [area] is the subject's own area.
 * Fail-closed: when any coordinate of any input polygon or [area] is not finite, or a result is not finite,
 * the answer is `Fit(+inf, +inf)`, invalid under both tests (ConvexClip alone would turn NaN into "no overlap").
 */
internal fun LockSearch.misfit(
    subject: List<Vec2>,
    silhouette: List<List<Vec2>>,
    others: List<List<Vec2>>,
    area: Double,
): Fit {
    val invalid = Fit(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
    if (!area.isFinite() || !finite(subject) || silhouette.any { !finite(it) } || others.any { !finite(it) }) return invalid
    var inside = 0.0
    for (polygon in silhouette) inside += ConvexClip.intersectionArea(subject, polygon)
    var maxOverlap = 0.0
    for (other in others) maxOverlap = maxOf(maxOverlap, ConvexClip.intersectionArea(subject, other))
    val insideDeficit = area - inside
    if (!insideDeficit.isFinite() || !maxOverlap.isFinite()) return invalid
    return Fit(insideDeficit, maxOverlap)
}

private fun finite(polygon: List<Vec2>): Boolean = polygon.all { it.x.isFinite() && it.y.isFinite() }

private fun toVec2(p: ExactPoint): Vec2 = Vec2(p.x.toDouble(), p.y.toDouble())
