package io.github.jamisuni.tangram.play

import androidx.compose.runtime.mutableIntStateOf
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.layout.TrayRules
import io.github.jamisuni.tangram.kernel.lock.SolvedCheck
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Turn
import io.github.jamisuni.tangram.kernel.state.PuzzleStates

// WO-003 design sections 4-5 (REQ-012, 014, 020, 021, 022; F5/DA-5): the state of one puzzle.
// Plain Kotlin, no Compose type, no clock of its own: time is the frame-clock ms handed in (design E5).

/** REQ-012: a piece is exactly one of these. */
internal sealed interface Where {
    data object Tray : Where

    data class Board(val at: ExactPoint) : Where

    data object Dragged : Where
}

internal data class PieceState(val piece: PieceId, val turn: Turn, val mirrored: Boolean, val where: Where)

/** The pose a drag started from; F5/DA-5 restores all of it. */
internal data class PickUp(
    val where: Where,
    val turn: Turn,
    val mirrored: Boolean,
    /** Piece centre in dp at pick-up. */
    val centre: Vec2,
    /** Drawn scale at pick-up (tray scale or board scale). */
    val scale: Double,
)

/** What one frame froze: the pose, the preview computed from that pose, and what is drawn (WO-001 calling rule 1). */
internal data class DragFrame(
    val pose: DragPose,
    val preview: PlacedPiece?,
    /** Piece centre in dp as drawn this frame. */
    val centre: Vec2,
    /** Drawn scale this frame. */
    val scale: Double,
)

internal class DragState(
    val piece: PieceId,
    val pickUp: PickUp,
    /** Lift in dp, fixed at drag start (DA-17). */
    val lift: Double,
    val startMs: Long,
    var finger: Vec2,
    var turn: Turn,
    val mirrored: Boolean,
    var frame: DragFrame? = null,
)

/** A pulse started by a miss; alive 600 ms (static when [reduced]); expired by [PlaySession.onFrame]. */
internal data class Pulse(val corners: CornerPulse, val startMs: Long, val reduced: Boolean = false)

/** The solve moment (REQ-023); the timeline functions read it. */
internal data class SolvedAt(val t0: Long, val reducedMotion: Boolean)

/** A cosmetic 180 ms glide from the drop pose to the logical location (REQ-019/020). */
internal data class Glide(val piece: PieceId, val fromCentre: Vec2, val fromScale: Double, val startMs: Long)

/** A 400 ms shake of a board piece that could not turn or mirror (REQ-016 A2, REQ-018). */
internal data class Shake(val piece: PieceId, val startMs: Long)

/** Touched only from the main thread (no locking). */
class PlaySession(
    val puzzle: Puzzle,
    private val reducedMotion: () -> Boolean = { false },
    private val resolver: DropResolver = DropResolver(puzzle),
) {
    /** Version counter (snapshot state) bumped on every mutation for Compose observability. CR-1 N1. */
    private val versionState = androidx.compose.runtime.mutableIntStateOf(0)

    private fun invalidate() {
        versionState.intValue++
    }

    /**
     * The observation handle of the drawing (CR-1 N1): reading it inside a Compose draw or derived state makes that
     * scope redraw after any mutation. Every mutating intent and [onFrame] bumps it, so the drawing reads it ONCE per
     * pass before reading `pieces`, `placed`, `state`, `drag` (also its in-place fields and `frame`), `pulse`, `solved`,
     * `shake` and `glides`.
     */
    internal val version: Int get() = versionState.intValue

    private var pieceList: List<PieceState> =
        puzzle.solution.map { it.piece }.distinct().sortedBy { TrayRules.order.indexOf(it) }.map {
            PieceState(it, TrayRules.restingTurn(it.shape), false, Where.Tray)
        }

    internal val pieces: List<PieceState>
        get() {
            versionState.intValue // Read version to observe state
            return pieceList
        }

    internal val placed: List<PlacedPiece>
        get() = pieceList.mapNotNull { p ->
            (p.where as? Where.Board)?.let { PlacedPiece(p.piece, p.turn, p.mirrored, it.at) }
        }

    var state: PuzzleState = PuzzleState.NEW
        private set

    internal var drag: DragState? = null
        private set

    internal var pulse: Pulse? = null
        private set

    internal var solved: SolvedAt? = null
        private set

    /** Glides of pieces that just locked or went home; expired by [onFrame] after 180 ms. */
    internal var glides: List<Glide> = emptyList()
        private set

    /** The shake after a refused board turn or mirror; expired by [onFrame] after 400 ms. */
    internal var shake: Shake? = null
        private set

    /** Set by the play area from the measured size; drags need it. */
    internal var layout: PlayLayout? = null

    val isDragging: Boolean get() = drag != null

    private fun update(piece: PieceId, f: (PieceState) -> PieceState) {
        pieceList = pieceList.map { if (it.piece == piece) f(it) else it }
        invalidate()
    }

    private fun centroid(piece: PieceId, turn: Turn, mirrored: Boolean) =
        PieceGeometry.centroidOffset(piece.shape, turn, mirrored)

    /** Starts a drag at [finger] (dp). No-op when solved, already dragging, no layout, or the piece is unknown. */
    internal fun beginDrag(piece: PieceId, finger: Vec2, nowMs: Long) {
        if (drag != null || state == PuzzleState.SOLVED) return
        val lay = checkNotNull(layout) { "PlaySession.layout must be set before beginDrag (GestureMachine.down sets it)" }
        val ps = pieceList.firstOrNull { it.piece == piece } ?: return
        if (ps.where == Where.Dragged) return
        val dpu = lay.dpPerUnit
        val c = centroid(piece, ps.turn, ps.mirrored)
        val pickCentre: Vec2
        val pickScale: Double
        when (val w = ps.where) {
            is Where.Board -> {
                pickCentre = lay.toDp(Vec2(w.at.x.toDouble() + c.x, w.at.y.toDouble() + c.y))
                pickScale = dpu
            }
            else -> {
                if (!lay.hasCell(piece)) return
                pickCentre = lay.cell(piece).centre
                pickScale = lay.trayScale
            }
        }
        if (ps.where == Where.Tray) {
            state = PuzzleStates.onTrayDragStarted(state)
            invalidate()
        }
        val offs = PieceGeometry.offsets(piece.shape, ps.turn, ps.mirrored)
        val lowest = offs.maxOf { it.y.toDouble() } - c.y
        val lift = lowest * dpu + if (lay.layoutClass == LayoutClass.PHONE) LIFT_PHONE_DP else LIFT_TABLET_DP
        drag = DragState(
            piece, PickUp(ps.where, ps.turn, ps.mirrored, pickCentre, pickScale), lift, nowMs, finger, ps.turn, ps.mirrored,
        )
        invalidate()
        update(piece) { it.copy(where = Where.Dragged) }
    }

    /** The finger moved; the pose is frozen only by [onFrame]. */
    internal fun dragTo(finger: Vec2) {
        drag?.finger = finger
        invalidate()
    }

    /** A twist changed the turn; applied about the fixed centre at the next frame. */
    internal fun setDragTurn(turn: Turn) {
        drag?.turn = turn
        invalidate()
    }

    /** Once per frame: freezes the pose and the preview computed from it. */
    internal fun onFrame(nowMs: Long) {
        expire(nowMs)
        val d = drag ?: return
        d.frame = frameAt(d, nowMs) ?: return
        invalidate()
    }

    /** Drops animation facts whose lifetime is over (frame-clock ms). */
    private fun expire(nowMs: Long) {
        val glidesBefore = glides
        glides = glides.filter { nowMs - it.startMs < PlayTiming.GLIDE_MS }
        val shakeBefore = shake
        shake?.let { if (nowMs - it.startMs >= PlayTiming.SHAKE_MS) shake = null }
        val pulseBefore = pulse
        pulse?.let { if (nowMs - it.startMs >= PlayTiming.PULSE_MS) pulse = null }
        if (glides != glidesBefore || shake != shakeBefore || pulse != pulseBefore) {
            invalidate()
        }
    }

    /** REQ-016 A1: a tray piece turns one step (TYPE-003); no effect on the state. No-op unless it is in the tray. */
    internal fun tapTray(piece: PieceId) {
        if (state == PuzzleState.SOLVED) return
        update(piece) { if (it.where == Where.Tray) it.copy(turn = Turn((it.turn.steps + 1) % 8)) else it }
    }

    /** REQ-018 A1: a tray piece mirrors. No-op unless it is in the tray. */
    internal fun flipTray(piece: PieceId) {
        if (state == PuzzleState.SOLVED) return
        update(piece) { if (it.where == Where.Tray) it.copy(mirrored = !it.mirrored) else it }
    }

    /** REQ-016: a board piece turns one step about its centre, kept only if it still locks; else unchanged and shakes. */
    internal fun tapBoard(piece: PieceId, nowMs: Long) {
        val cur = boardPiece(piece) ?: return
        refit(cur, Turn((cur.turn.steps + 1) % 8), cur.mirrored, nowMs)
    }

    /** REQ-018: a board piece mirrors, kept only if it still locks; else unchanged and shakes. */
    internal fun flipBoard(piece: PieceId, nowMs: Long) {
        val cur = boardPiece(piece) ?: return
        refit(cur, cur.turn, !cur.mirrored, nowMs)
    }

    private fun boardPiece(piece: PieceId): PlacedPiece? =
        if (state == PuzzleState.SOLVED || drag != null) null else placed.firstOrNull { it.piece == piece }

    private fun refit(cur: PlacedPiece, turn: Turn, mirrored: Boolean, nowMs: Long) {
        val lay = checkNotNull(layout) { "PlaySession.layout must be set before a board turn or mirror" }
        expire(nowMs)
        val next = resolver.refit(cur, turn, mirrored, placed, lay.dpPerUnit)
        if (next == null) {
            shake = Shake(cur.piece, nowMs)
            invalidate()
        } else {
            update(cur.piece) { it.copy(turn = next.turn, mirrored = next.mirrored, where = Where.Board(next.at)) }
        }
    }

    private fun frameAt(d: DragState, nowMs: Long): DragFrame? {
        val lay = layout ?: return null
        val elapsed = (nowMs - d.startMs).coerceAtLeast(0L)
        val centre = DragMotion.centre(d.finger, d.pickUp.centre, d.lift, elapsed)
        val scale = DragMotion.scale(d.pickUp.scale, lay.dpPerUnit, elapsed)
        val u = lay.toUnits(centre)
        val c = centroid(d.piece, d.turn, d.mirrored)
        val pose = DragPose(d.piece, d.turn, d.mirrored, Vec2(u.x - c.x, u.y - c.y), lay.overBoard(centre))
        val preview = resolver.preview(pose, placed, lay.dpPerUnit)
        return DragFrame(pose, preview, centre, scale)
    }

    /**
     * Drops the dragged piece with the last DISPLAYED pose (WO-001 calling rule 1), never the live finger.
     * Before the first frame the pose of the drag start is used. Null when no drag.
     */
    internal fun release(nowMs: Long): DropOutcome? {
        val d = drag ?: return null
        val lay = layout
        val frame = if (lay == null) null else d.frame ?: frameAt(d, d.startMs)
        if (lay == null || frame == null) {
            interruptDrag() // never leave a piece stuck Dragged
            return null
        }
        val outcome = resolver.release(frame.pose, placed, lay.dpPerUnit)
        drag = null
        invalidate()
        expire(nowMs)
        glides = glides.filter { it.piece != d.piece } + Glide(d.piece, frame.centre, frame.scale, nowMs)
        invalidate()
        when (outcome) {
            is DropOutcome.Locked -> {
                val p = outcome.placed
                update(d.piece) { it.copy(turn = p.turn, mirrored = p.mirrored, where = Where.Board(p.at)) }
                val all = SolvedCheck.isSolved(pieceList.map { it.piece }, placed)
                state = PuzzleStates.onPieceLocked(state, all)
                invalidate()
                if (state == PuzzleState.SOLVED) {
                    solved = SolvedAt(nowMs, reducedMotion())
                    invalidate()
                }
            }
            is DropOutcome.Home -> {
                update(d.piece) { it.copy(turn = outcome.turn, mirrored = outcome.mirrored, where = Where.Tray) }
                outcome.pulse?.let {
                    pulse = Pulse(it, nowMs, reducedMotion())
                    invalidate()
                }
            }
        }
        return outcome
    }

    /**
     * F5/DA-5: restore the whole pick-up pose silently. Never reaches the resolver, starts no pulse, glide or
     * sound, does not undo NEW to IN_PROGRESS. Idempotent.
     */
    fun interruptDrag() {
        val d = drag ?: return
        drag = null
        invalidate()
        val pu = d.pickUp
        update(d.piece) { it.copy(turn = pu.turn, mirrored = pu.mirrored, where = pu.where) }
    }

    private companion object {
        const val LIFT_PHONE_DP = 30.0
        const val LIFT_TABLET_DP = 40.0
    }
}
