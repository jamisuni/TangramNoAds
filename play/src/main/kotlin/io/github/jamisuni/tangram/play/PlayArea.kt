package io.github.jamisuni.tangram.play

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.play.draw.PlayCanvas
import kotlinx.coroutines.flow.collectLatest

/**
 * Test seam (design "Test seams"): the units-to-px board transform and the tray cell rects in px, relative to the
 * origin of the `play-area` node. A piece-unit point `u` is at `(originXPx + u.x * scalePx, originYPx + u.y * scalePx)`.
 */
data class BoardTransformData(
    val scalePx: Float,
    val originXPx: Float,
    val originYPx: Float,
    val trayCellsPx: Map<PieceId, Rect>,
)

/** Public semantics key carried by the `board` node (bounds = the board rect). */
val BoardTransform = SemanticsPropertyKey<BoardTransformData>("BoardTransform")

var SemanticsPropertyReceiver.boardTransform: BoardTransformData by BoardTransform

/**
 * TASK-018b: the one composable `app` calls. Computes the layout from its own size, owns the frame loop (the ONLY time
 * base: frame-clock ms), adapts pointer events to [GestureMachine] and nests [PlayCanvas].
 * Reduced motion comes from the session; nothing here reads system settings.
 */
@Composable
fun PlayArea(
    session: PlaySession,
    layoutClass: LayoutClass,
    trayRows: List<List<PieceId>>,
    screenHeight: Dp,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.semantics { testTag = "play-area" }) {
        val w = maxWidth.value.toDouble()
        val h = maxHeight.value.toDouble()
        val screenH = screenHeight.value.toDouble()
        val layout = remember(session, w, h, screenH, layoutClass, trayRows) {
            PlayLayout.compute(w, h, screenH, layoutClass, trayRows, session.puzzle)
        }
        val currentLayout = rememberUpdatedState(layout)
        val machine = remember(session) { GestureMachine(session) { currentLayout.value } }

        // Frame clock ms. Written only by the frame loop below; read by the adapter and PlayCanvas.
        var nowMs by remember(session) { mutableLongStateOf(0L) }
        var touching by remember(session) { mutableStateOf(false) }

        // Attach the layout in a composition side effect (never in draw). A size change while a gesture is live
        // cancels the machine (CR-1 F2 residual), which also interrupts a drag silently.
        val attached = remember(session) { arrayOfNulls<PlayLayout>(1) }
        SideEffect {
            val previous = attached[0]
            if (previous != null && previous !== layout) {
                machine.cancel()
                session.interruptDrag()
            }
            attached[0] = layout
            session.layout = layout
        }

        // One frame loop for the whole play area: runs while a finger is down or an animation is alive, so an idle
        // board schedules no frames. Every frame: nowMs, then session.onFrame(nowMs) (one time base, E5).
        LaunchedEffect(session) {
            snapshotFlow { touching || session.animating(nowMs) }.collectLatest { active ->
                if (!active) return@collectLatest
                while (true) {
                    androidx.compose.runtime.withFrameNanos { nanos ->
                        val ms = nanos / 1_000_000L
                        nowMs = ms
                        session.onFrame(ms)
                    }
                }
            }
        }

        val density = LocalDensity.current.density
        val adapter = Modifier.pointerInput(session, machine) {
            val d = this.density
            val ids = HashMap<PointerId, Int>()
            var next = 0
            awaitEachGesture {
                try {
                    ids.clear()
                    val first = awaitFirstDown(requireUnconsumed = false)
                    touching = true // wakes the frame loop; idle boards schedule no frames
                    ids[first.id] = next++
                    machine.down(ids.getValue(first.id), dp(first.position, d), nowMs)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        for (c in event.changes) {
                            val id = ids.getOrPut(c.id) { next++ }
                            val p = dp(c.position, d)
                            when {
                                c.changedToDown() -> machine.down(id, p, nowMs)
                                c.changedToUp() -> {
                                    machine.up(id, p, nowMs)
                                    ids.remove(c.id)
                                }
                                // Consumed up: the system cancel arrives like this (design section 4), not as a coroutine cancel.
                                c.previousPressed && !c.pressed -> {
                                    machine.cancel()
                                    ids.remove(c.id)
                                }
                                c.pressed && c.position != c.previousPosition -> machine.move(id, p, nowMs)
                            }
                        }
                    } while (event.changes.any { it.pressed })
                } finally {
                    // CR-1 N2: a leaked id must never freeze the next gesture.
                    machine.cancel()
                    touching = false
                }
            }
        }

        Box(Modifier.fillMaxSize().then(adapter)) {
            PlayCanvas(session, layout, nowMs, Modifier.fillMaxSize())
            val b = layout.boardRect
            val transform = BoardTransformData(
                scalePx = (layout.dpPerUnit * density).toFloat(),
                originXPx = (layout.toDp(Vec2(0.0, 0.0)).x * density).toFloat(),
                originYPx = (layout.toDp(Vec2(0.0, 0.0)).y * density).toFloat(),
                trayCellsPx = layout.trayRows.flatten().associateWith { piece ->
                    val c = layout.cell(piece)
                    Rect(
                        (c.left * density).toFloat(), (c.top * density).toFloat(),
                        (c.right * density).toFloat(), (c.bottom * density).toFloat(),
                    )
                },
            )
            Box(
                Modifier
                    .offset(b.left.dp, b.top.dp)
                    .size(b.width.dp, b.height.dp)
                    .semantics {
                        testTag = "board"
                        boardTransform = transform
                    },
            )
        }
    }
}

private fun dp(p: androidx.compose.ui.geometry.Offset, density: Float) = Vec2((p.x / density).toDouble(), (p.y / density).toDouble())

/** True while something needs frames: a drag, a glide, the shake, the pulse, or the solve animation (until 2.5 s). */
private fun PlaySession.animating(nowMs: Long): Boolean {
    version // observe every mutation
    if (drag != null || glides.isNotEmpty() || shake != null || pulse != null) return true
    val s = solved ?: return false
    return nowMs - s.t0 < PlayTiming.CONFETTI_MS + 100L
}
