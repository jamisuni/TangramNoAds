package io.github.jamisuni.tangram.play.acceptance

import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.play.DropResolver
import io.github.jamisuni.tangram.play.GestureMachine
import io.github.jamisuni.tangram.play.PlayLayout
import io.github.jamisuni.tangram.play.PlaySession

// ACCEPTANCE-TEST ADAPTERS (TASK-T5) for the aid-solve decision tests of WO-005 (DA-73, DA-83). Built on the WO-003 `Seams.kt`
// helpers of this package (PUZZLES, layoutFor, attach, cellR, badgeR, pressPoint, v, buildOrder, ps). Every miss is a loud error.
// The PlaySession constructor is the frozen seam: (puzzle, reducedMotion, resolver, onChanged, onSolved) with onSolved last.

/** Records the session's callbacks in order: "changed" and "solved:true|false". */
internal class AidEvents {
    val log = ArrayList<String>()
    fun changed() { log += "changed" }
    fun solved(byAid: Boolean) { log += "solved:$byAid" }
    fun count(entry: String) = log.count { it == entry }
}

internal fun aidSession(p: Puzzle, ev: AidEvents, reduced: () -> Boolean = { false }): PlaySession =
    PlaySession(p, reduced, DropResolver(p), { ev.changed() }, { byAid -> ev.solved(byAid) })

/** The stored solution as poses, one per solution entry in solution order (what the aid is meant to hand over). */
internal fun aidPoses(p: Puzzle): List<PlacedPiece> =
    p.solution.map { PieceGeometry.poseOf(it.piece, it.polygon) ?: error("fixture: no pose for ${it.piece} in ${p.id.value}") }

/** A piece's pose as an exact point set, to compare poses across puzzles. */
internal fun cornerSet(p: PlacedPiece): Set<String> = p.corners.map { "${it.x}/${it.y}" }.toSet()

internal fun poseKeys(poses: List<PlacedPiece>): Set<String> = poses.map { "${it.piece}@${it.at}/${it.turn.steps}/${it.mirrored}" }.toSet()

/** Everything observable about a session that "changes nothing" must keep. */
internal fun aidSnapshot(s: PlaySession): String =
    "state=${s.state}; solved=${s.solved}; pending=${s.solvePending}; dragging=${s.isDragging}; " +
        s.pieces.joinToString { "${it.piece}:${it.where}:${it.turn.steps}:${it.mirrored}" } +
        "; placed=" + poseKeys(s.placed).sorted()

internal val SEVEN_PIECE: List<Puzzle> get() = PUZZLES.filter { it.solution.size == 7 }

private fun box(poses: List<PlacedPiece>): List<Double> {
    val pts = poses.flatMap { it.corners }.map { it.x.toDouble() to it.y.toDouble() }
    return listOf(pts.minOf { it.first }, pts.minOf { it.second }, pts.maxOf { it.first }, pts.maxOf { it.second })
}

/** A puzzle whose silhouette surely differs from [p]'s (different bounding box): its full pose set cannot cover [p]. */
internal fun otherSilhouette(p: Puzzle): Puzzle {
    val mine = box(aidPoses(p))
    return SEVEN_PIECE.firstOrNull { q -> q.id != p.id && box(aidPoses(q)).zip(mine).any { (a, b) -> Math.abs(a - b) > 0.01 } }
        ?: error("fixture: no 7-piece puzzle with a different silhouette than ${p.id.value}")
}

/**
 * [p]'s poses with ONE piece taken from another puzzle's stored solution at a different place. With the six other pieces where
 * they belong, the only free room is that piece's own polygon, so a pose elsewhere overlaps or leaves the silhouette: the set
 * cannot be placed whatever the engine's order of checking.
 */
internal fun withOnePieceMoved(p: Puzzle): List<PlacedPiece> {
    val mine = aidPoses(p)
    for (q in SEVEN_PIECE) {
        if (q.id == p.id) continue
        val theirs = aidPoses(q)
        for (i in mine.indices) {
            val other = theirs.firstOrNull { it.piece == mine[i].piece } ?: continue
            if (cornerSet(other) != cornerSet(mine[i])) return mine.toMutableList().also { it[i] = other }
        }
    }
    error("fixture: no piece of ${p.id.value} has a different pose in another puzzle")
}

/** The scripted finger of the WO-003 `Driver`, over a session that reports its callbacks to [events]. */
internal class AidDriver(
    val puzzle: Puzzle,
    val events: AidEvents,
    reduced: () -> Boolean = { false },
    val layout: PlayLayout = layoutFor(puzzle),
) {
    val session: PlaySession = aidSession(puzzle, events, reduced).also { attach(it, layout) }
    private val machine = GestureMachine(session) { layout }
    private var t: Long = 1000L
    private var finger: Vec2 = Vec2(0.0, 0.0)

    fun frame(ms: Long = 16L) {
        t += ms
        session.onFrame(t)
    }

    private fun tap(p: Vec2) {
        machine.down(0, p, t)
        frame(16)
        machine.up(0, p, t)
        frame()
    }

    fun startDrag(from: Vec2) {
        machine.down(0, from, t)
        frame()
        finger = Vec2(from.x, from.y - 20.0)
        machine.move(0, finger, t)
        frame()
    }

    private fun steerOrigin(target: Vec2) {
        repeat(3) {
            frame(150)
            val o = session.drag?.frame?.pose?.origin ?: error("seam: steerOrigin called with no dragged piece")
            val a = layout.toDp(o)
            val b = layout.toDp(target)
            finger = Vec2(finger.x + b.x - a.x, finger.y + b.y - a.y)
            machine.move(0, finger, t)
        }
        frame(150)
    }

    /** Press the tray miniature of [piece] and drag it a little: the session is dragging. */
    fun dragFromTray(piece: PieceId) = startDrag(pressPoint(layout.cellR(piece)))

    /** Turn (taps) and mirror (badge) in the tray, then drag to the target and release. */
    fun place(target: PlacedPiece) {
        val cell = layout.cellR(target.piece)
        if (target.mirrored != session.ps(target.piece).mirrored) tap(layout.badgeR(session).centre)
        val taps = Math.floorMod(target.turn.steps - session.ps(target.piece).turn.steps, 8)
        repeat(taps) { tap(pressPoint(cell)) }
        startDrag(pressPoint(cell))
        steerOrigin(v(target.at))
        machine.up(0, finger, t)
        frame()
    }

    fun placeAll(targets: List<PlacedPiece>) {
        for (tg in buildOrder(puzzle, targets, layout.dpPerUnit, session.placed)) {
            place(tg)
            check(session.placed.any { it.piece == tg.piece && it.at == tg.at }) { "${tg.piece} did not lock at its target in ${puzzle.id.value}" }
        }
    }
}
