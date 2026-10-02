package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.Turn
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.roundToLong

// WO-003 design section 4 (REQ-014..018; DA-16, DA-17, DA-18, DA-31; F5/DA-5): pointer events to session intents.
// Pure Kotlin. Positions are dp; timeMs is frame-clock time supplied by the adapter, never pointer uptime.
// Adapter contract: up() is for an UNCONSUMED up only; a consumed up (system cancel) is cancel().

internal sealed interface GestureState {
    data object Idle : GestureState

    /** Fingers tracked only to be dropped when they lift; no effect on the session. */
    data class Ignored(val ids: Set<Int>) : GestureState

    data class Pressed(val piece: PieceId, val id: Int, val start: Vec2) : GestureState

    data class Dragging(val id: Int, val twistId: Int?) : GestureState
}

internal class GestureMachine(
    private val session: PlaySession,
    private val layoutProvider: () -> PlayLayout?,
) {
    private class Press(val piece: PieceId, val id: Int, val start: Vec2)

    private class Twist(val id: Int, var pos: Vec2, var angle: Double, val turnAtStart: Int) {
        /** Cumulative, continuously unwrapped angle in radians. */
        var cumulative = 0.0
    }

    private class Drag(val id: Int, var pos: Vec2) {
        var twist: Twist? = null
    }

    private var press: Press? = null
    private var drag: Drag? = null

    /** Fingers that are down but ignored (third finger, second finger while pressed, badge finger, survivors). */
    private val ignored = LinkedHashSet<Int>()

    val state: GestureState
        get() {
            press?.let { return GestureState.Pressed(it.piece, it.id, it.start) }
            drag?.let { return GestureState.Dragging(it.id, it.twist?.id) }
            return if (ignored.isEmpty()) GestureState.Idle else GestureState.Ignored(ignored.toSet())
        }

    private fun isIdle() = press == null && drag == null && ignored.isEmpty()

    private fun tracked(id: Int) = press?.id == id || drag?.id == id || drag?.twist?.id == id || id in ignored

    /** F3d: the session dropped the drag without us (onPause, size change, onCreate): the fingers become Ignored. */
    private fun syncExternalInterrupt() {
        val d = drag ?: return
        if (session.isDragging) return
        ignored += d.id
        d.twist?.let { ignored += it.id }
        drag = null
    }

    fun down(id: Int, p: Vec2, timeMs: Long) {
        syncExternalInterrupt()
        if (tracked(id)) return
        if (!isIdle()) {
            val d = drag
            if (d != null && d.twist == null) {
                d.twist = Twist(id, p, atan2(p.y - d.pos.y, p.x - d.pos.x), session.drag?.turn?.steps ?: 0)
            } else {
                ignored += id // second finger while pressed, third finger, survivor of an interrupt
            }
            return
        }
        val layout = layoutProvider() ?: return
        session.layout = layout // the one place the session learns its layout: hit test and drag agree
        when (val hit = HitTest.pick(p, layout, session)) {
            null -> Unit
            Hit.Badge -> {
                flipBadge(timeMs)
                ignored += id
            }
            is Hit.Piece -> press = Press(hit.id, id, p)
        }
    }

    fun move(id: Int, p: Vec2, timeMs: Long) {
        syncExternalInterrupt()
        val pr = press
        if (pr != null && pr.id == id) {
            if (hypot(p.x - pr.start.x, p.y - pr.start.y) >= DRAG_THRESHOLD_DP) startDrag(pr, p, timeMs)
            return
        }
        val d = drag ?: return
        val tw = d.twist
        if (id == d.id) {
            d.pos = p
            session.dragTo(p)
            if (tw != null) updateTwist(d, tw)
        } else if (tw != null && id == tw.id) {
            tw.pos = p
            updateTwist(d, tw)
        }
    }

    /** An UNCONSUMED up. */
    fun up(id: Int, p: Vec2, timeMs: Long) {
        syncExternalInterrupt()
        if (ignored.remove(id)) return
        val pr = press
        if (pr != null && pr.id == id) {
            press = null
            // Up 12 dp or more from the down point with no move between: not a tap, and no frame was ever shown
            // for a release, so nothing happens.
            if (hypot(p.x - pr.start.x, p.y - pr.start.y) < DRAG_THRESHOLD_DP) tap(pr.piece, timeMs)
            return
        }
        val d = drag ?: return
        val tw = d.twist
        if (id == d.id) {
            // Lifting the drag finger ends the drag whatever the second finger does.
            drag = null
            if (tw != null) ignored += tw.id
            session.release(timeMs)
        } else if (tw != null && id == tw.id) {
            d.twist = null // the turn stays; a new twist may start later
        }
    }

    /** A system cancel (a consumed up) or a detach: a drag is interrupted silently; a press is not a tap. */
    fun cancel() {
        if (drag != null && session.isDragging) session.interruptDrag()
        press = null
        drag = null
        ignored.clear()
    }

    private fun startDrag(pr: Press, p: Vec2, timeMs: Long) {
        press = null
        session.beginDrag(pr.piece, p, timeMs)
        if (session.isDragging) drag = Drag(pr.id, p) else ignored += pr.id
    }

    private fun tap(piece: PieceId, timeMs: Long) {
        when (session.pieces.firstOrNull { it.piece == piece }?.where) {
            Where.Tray -> session.tapTray(piece)
            is Where.Board -> session.tapBoard(piece, timeMs)
            else -> Unit
        }
    }

    private fun flipBadge(timeMs: Long) {
        when (session.pieces.firstOrNull { it.piece == PieceId.PG }?.where) {
            Where.Tray -> session.flipTray(PieceId.PG)
            is Where.Board -> session.flipBoard(PieceId.PG, timeMs)
            else -> Unit
        }
    }

    /** DA-18 / DA-31: shortest signed change counts only while the fingers are 24 dp apart; the reference always follows. */
    private fun updateTwist(d: Drag, tw: Twist) {
        val a = atan2(tw.pos.y - d.pos.y, tw.pos.x - d.pos.x)
        var delta = a - tw.angle
        while (delta > PI) delta -= 2 * PI
        while (delta <= -PI) delta += 2 * PI
        tw.angle = a
        if (hypot(tw.pos.x - d.pos.x, tw.pos.y - d.pos.y) >= TWIST_MIN_SEPARATION_DP) tw.cumulative += delta
        session.setDragTurn(Turn(Math.floorMod(tw.turnAtStart + steps(Math.toDegrees(tw.cumulative)), 8)))
    }

    internal companion object {
        const val DRAG_THRESHOLD_DP = 12.0
        const val TWIST_MIN_SEPARATION_DP = 24.0

        /** DA-18: sign(a) * ceil((|a| - 22.5)/45) for |a| > 22.5 degrees, else 0 (rounded to 1e-9 degrees against float noise). */
        fun steps(degrees: Double): Int {
            val a = (degrees * 1e9).roundToLong() / 1e9
            if (abs(a) <= 22.5) return 0
            val n = ceil((abs(a) - 22.5) / 45.0).toInt()
            return if (a < 0) -n else n
        }
    }
}
