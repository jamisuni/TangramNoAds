package io.github.jamisuni.tangram.play

import androidx.compose.runtime.mutableIntStateOf
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.layout.TrayRules
import io.github.jamisuni.tangram.kernel.geometry.Silhouette
import io.github.jamisuni.tangram.kernel.lock.LockSearch
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
    /** decision DA-49: fired after each settled event (see [settled]); never mid-drag, per frame, by [toProgress] or [restore]. */
    private val onChanged: () -> Unit = {},
    /** decision DA-73: fired once per solve, `false` for a drop, `true` for [solveByAid]; never by [restore]. */
    private val onSolved: (byAid: Boolean) -> Unit = {},
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

    // Own snapshot state (code review F2): only a REAL transition notifies, so a reader of `state` / `isDragging`
    // does not recompose on every drag frame. The version counter stays for the drawing.
    private val stateField = androidx.compose.runtime.mutableStateOf(PuzzleState.NEW)
    private val draggingField = androidx.compose.runtime.mutableStateOf(false)

    /** Observable, and changes only when the state really changes (WO-004 section 4, review F2). */
    var state: PuzzleState
        get() = stateField.value
        private set(value) {
            if (stateField.value != value) stateField.value = value
        }

    internal var drag: DragState? = null
        private set(value) {
            field = value
            val now = value != null
            if (draggingField.value != now) draggingField.value = now
        }

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

    /** Observable, and changes only when a drag starts or ends (review F2). */
    val isDragging: Boolean
        get() = draggingField.value

    /** The frame-clock ms of the latest [onFrame]; a rebuilt composition seeds its clock from it (CR-F1). */
    internal var lastFrameMs: Long = 0L
        private set

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
        lastFrameMs = nowMs
        expire(nowMs)
        // decision DA-83: resolve a pending aid solve BEFORE the no-drag early return below (an idle board has no drag)
        if (solvePending) {
            solved = SolvedAt(nowMs, reducedMotion())
            solvePending = false
            invalidate()
        }
        val d = drag ?: return
        d.frame = frameAt(d, nowMs) ?: return
        invalidate()
    }

    /**
     * decision DA-73: true from an accepted [solveByAid] until the next [onFrame], which starts the REQ-023 timeline
     * from that frame's clock; the frame loop of the play area treats it as alive.
     */
    internal var solvePending: Boolean = false
        private set

    /**
     * decision DA-73: the neutral "place these poses and finish" hook. False, changing nothing, unless the puzzle is not
     * solved, [poses] name exactly the puzzle's pieces once each and every pose is a valid
     * placement in list order (the lock engine's own rule). On true the pieces sit at the poses, the state is Solved
     * (TYPE-006 path), the solve is pending for the next frame, then onSolved(true) and onChanged fire.
     */
    fun solveByAid(poses: List<PlacedPiece>): Boolean {
        if (state == PuzzleState.SOLVED) return false
        val ids = pieceList.map { it.piece }
        if (poses.size != ids.size || poses.map { it.piece }.toSet() != ids.toSet()) return false
        val silhouette = Silhouette(puzzle.solution.map { it.polygon })
        val accepted = ArrayList<PlacedPiece>()
        for (p in poses) {
            if (!LockSearch.isValidPlacement(silhouette, accepted, p.piece, p.turn, p.mirrored, p.at)) return false
            accepted += p
        }
        if (!SolvedCheck.isSolved(ids, poses)) return false
        interruptDrag() // F5/DA-5: a running drag is restored silently first; only an accepted solve gets here
        val byPiece = poses.associateBy { it.piece }
        pieceList = pieceList.map { ps ->
            val p = byPiece.getValue(ps.piece)
            ps.copy(turn = p.turn, mirrored = p.mirrored, where = Where.Board(p.at))
        }
        glides = emptyList()
        pulse = null
        shake = null
        state = PuzzleStates.onPieceLocked(PuzzleStates.onTrayDragStarted(state), true)
        solvePending = true
        invalidate()
        onSolved(true)
        settled()
        return true
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
        if (pieceList.none { it.piece == piece && it.where == Where.Tray }) return
        update(piece) { it.copy(turn = Turn((it.turn.steps + 1) % 8)) }
        settled()
    }

    /** REQ-018 A1: a tray piece mirrors. No-op unless it is in the tray. */
    internal fun flipTray(piece: PieceId) {
        if (state == PuzzleState.SOLVED) return
        if (pieceList.none { it.piece == piece && it.where == Where.Tray }) return
        update(piece) { it.copy(mirrored = !it.mirrored) }
        settled()
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
            settled()
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
                    onSolved(false)
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
        settled()
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

    /** decision DA-49: a settled event finished (drop locked or returned, tray or successful board turn or mirror). */
    private fun settled() = onChanged()

    /**
     * WO-004 section 4: the session as progress on top of [base]. Interrupts a drag first (F5/DA-5), reports EVERY piece
     * in any state (the storage policy is browse's, DA-48), never calls onChanged.
     */
    fun toProgress(base: PuzzleProgress): PuzzleProgress {
        interruptDrag()
        val saved = pieceList.associate { p ->
            p.piece to when (val w = p.where) {
                is Where.Board -> PieceSave.OnBoard(w.at, p.turn, p.mirrored)
                else -> PieceSave.InTray(p.turn, p.mirrored)
            }
        }
        return base.copy(state = state, pieces = saved)
    }

    /**
     * WO-004 section 4: apply [saved] to a fresh session (already sanitized by browse). Calls no onChanged.
     * A complete IN_PROGRESS save becomes a Solved settle (DA-66).
     */
    fun restore(saved: PuzzleProgress) {
        check(drag == null) { "restore needs a session with no drag" }
        fun resting(p: PieceState) = p.copy(turn = TrayRules.restingTurn(p.piece.shape), mirrored = false, where = Where.Tray)
        when (saved.state) {
            PuzzleState.SOLVED -> settle()
            PuzzleState.NEW, PuzzleState.IN_PROGRESS -> {
                val board = saved.state == PuzzleState.IN_PROGRESS
                pieceList = pieceList.map { p ->
                    when (val s = saved.pieces[p.piece]) {
                        null -> resting(p)
                        is PieceSave.OnBoard ->
                            if (board) p.copy(turn = s.turn, mirrored = s.mirrored, where = Where.Board(s.at))
                            else p.copy(turn = s.turn, mirrored = s.mirrored, where = Where.Tray)
                        is PieceSave.InTray -> p.copy(turn = s.turn, mirrored = s.mirrored, where = Where.Tray)
                    }
                }
                if (board && SolvedCheck.isSolved(pieceList.map { it.piece }, placed)) {
                    settle() // decision DA-66
                } else {
                    state = saved.state
                }
            }
        }
        invalidate()
    }

    /**
     * Solved settle: nothing placed, every piece resting, and t0 far enough in the past that [SolvedTimeline] is
     * settled from the first frame (picture alpha 1, pieces hidden, no pop, no confetti); no best time here.
     */
    private fun settle() {
        pieceList = pieceList.map {
            it.copy(turn = TrayRules.restingTurn(it.piece.shape), mirrored = false, where = Where.Tray)
        }
        state = PuzzleState.SOLVED
        solved = SolvedAt(-SETTLED_AGO_MS, reducedMotion())
    }

    private companion object {
        const val SETTLED_AGO_MS = 10_000L
        const val LIFT_PHONE_DP = 30.0
        const val LIFT_TABLET_DP = 40.0
    }
}
