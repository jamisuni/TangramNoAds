package io.github.jamisuni.tangram.play

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.BasicText
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.play.draw.PlayCanvas
import io.github.jamisuni.tangram.play.draw.clearPictureCaches
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
 * Required by design section 1 and 4 (REQ-014 A1, REQ-015 A1, REQ-017 A1, REQ-018 A1; DA-38 queued events on one frame clock).
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
    solvedBar: (@Composable BoxScope.() -> Unit)? = null,
    /** F1 / DA-71: content placed at its measured size in a board corner clear of the silhouette (see [PlayLayout.computeWithCorner]). */
    cornerControl: (@Composable BoxScope.() -> Unit)? = null,
    /** decision DA-74: composed over the play area (above the canvas, below the corner slots) only while NOT solved; takes no touches. */
    boardOverlay: (@Composable BoxScope.(BoardSpace) -> Unit)? = null,
    /** decision DA-75: placed by [PlayLayout.placeSecondary] AFTER the board and the primary slot; never feeds the layout. */
    secondaryCornerControl: (@Composable BoxScope.() -> Unit)? = null,
    /** decision DA-118: false cancels the gesture machine silently (no event) and ignores new downs. */
    inputEnabled: Boolean = true,
    /** WO-008 (REQ-031 A2): the puzzle-time pill; composed only while NOT solved, placed by [PlayLayout.timerRect]; takes no touches. */
    timer: (@Composable BoxScope.() -> Unit)? = null,
) {
    BoxWithConstraints(modifier.semantics { testTag = "play-area" }) {
        val w = maxWidth.value.toDouble()
        val h = maxHeight.value.toDouble()
        val screenH = screenHeight.value.toDouble()
        // F1 / DA-71: with a corner slot the layout also places the control clear of the silhouette (or reserves a
        // strip). Decided by the slot's PRESENCE, never by what it shows, so the board never shifts when it appears.
        val hasCorner = cornerControl != null
        // The control's MEASURED size (loose constraints) places it; the last non-empty size is kept, so hiding the
        // content (it composes nothing) neither moves the board nor flickers the strip decision. Until the first
        // measurement a typical pill size is assumed (one relayout at most, when the real size differs).
        var measured by remember { mutableStateOf<DpSize?>(null) }
        val ctl = measured ?: DpSize(DEFAULT_CORNER_W_DP.dp, PlayLayout.CORNER_CONTROL_DP.dp)
        val placed = remember(session, w, h, screenH, layoutClass, trayRows, hasCorner, ctl) {
            if (hasCorner) {
                PlayLayout.computeWithCorner(
                    w, h, screenH, layoutClass, trayRows, session.puzzle, ctl.width.value.toDouble(), ctl.height.value.toDouble(),
                )
            } else {
                PlayLayout.compute(w, h, screenH, layoutClass, trayRows, session.puzzle) to null
            }
        }
        val layout = placed.first
        val controlRect = placed.second?.rect
        val currentLayout = rememberUpdatedState(layout)
        // DA-52: derived, so only a change of the answer (not every drag frame) recomposes the readers below.
        val isSolved by remember(session) { derivedStateOf { session.state == PuzzleState.SOLVED } }
        val machine = remember(session) { GestureMachine(session) { currentLayout.value } }

        // Frame clock ms. Written only by the frame loop below; read by PlayCanvas.
        var nowMs by remember(session) { mutableLongStateOf(session.lastFrameMs) }
        // Pointer events are queued by the adapter and dispatched INSIDE the frame loop with that frame's nowMs, so
        // every time the machine sees is frame-clock time (never stale, never uptime). `pending` wakes the loop.
        val queue = remember(session) { ArrayDeque<PointerEv>() }
        var pending by remember(session) { mutableStateOf(false) }
        fun enqueue(e: PointerEv) {
            queue.addLast(e)
            pending = true
        }

        // Attach the layout in a composition side effect (never in draw). A size change while a gesture is live
        // cancels the machine (CR-1 F2 residual), which also interrupts a drag silently; older queued events are stale.
        val attached = remember(session) { arrayOfNulls<PlayLayout>(1) }
        SideEffect {
            val previous = attached[0]
            if (previous != null && previous !== layout) {
                queue.clear()
                machine.cancel()
                session.interruptDrag()
            }
            attached[0] = layout
            session.layout = layout
        }
        // decision DA-118: disabling cancels a live gesture through the same silent path; downs are ignored below.
        val inputOn = rememberUpdatedState(inputEnabled)
        SideEffect {
            if (!inputEnabled) {
                queue.clear()
                machine.cancel()
                session.interruptDrag()
            }
        }
        // Leaving the composition: the loop dies with its queue, so cancel directly (needs no time).
        DisposableEffect(machine) {
            onDispose {
                queue.clear()
                machine.cancel()
                clearPictureCaches() // N2: the caches are process-wide; do not keep ~16 MB after the screen is left
            }
        }

        // One frame loop: runs while events are pending or an animation is alive; an idle board schedules no frames.
        // Every frame: nowMs, queued events in order, then session.onFrame(nowMs) (one time base, E5).
        LaunchedEffect(session, machine) {
            snapshotFlow { pending || session.animating(nowMs) }.collectLatest { active ->
                if (!active) return@collectLatest
                while (true) {
                    androidx.compose.runtime.withFrameNanos { nanos ->
                        val ms = nanos / 1_000_000L
                        nowMs = ms
                        while (queue.isNotEmpty()) {
                            when (val e = queue.removeFirst()) {
                                is PointerEv.Down -> machine.down(e.id, e.p, ms)
                                is PointerEv.Move -> machine.move(e.id, e.p, ms)
                                is PointerEv.Up -> machine.up(e.id, e.p, ms)
                                PointerEv.Cancel -> machine.cancel()
                            }
                        }
                        pending = false
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
                    ids[first.id] = next++
                    if (inputOn.value) enqueue(PointerEv.Down(ids.getValue(first.id), dp(first.position, d)))
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        for (c in event.changes) {
                            val id = ids.getOrPut(c.id) { next++ }
                            val p = dp(c.position, d)
                            when {
                                c.changedToDown() -> if (inputOn.value) enqueue(PointerEv.Down(id, p))
                                c.changedToUp() -> {
                                    enqueue(PointerEv.Up(id, p))
                                    ids.remove(c.id)
                                }
                                // Consumed up: the system cancel arrives like this (design section 4), not as a coroutine cancel.
                                c.previousPressed && !c.pressed -> {
                                    enqueue(PointerEv.Cancel)
                                    ids.remove(c.id)
                                }
                                c.pressed && c.position != c.previousPosition -> enqueue(PointerEv.Move(id, p))
                            }
                        }
                    } while (event.changes.any { it.pressed })
                } finally {
                    // CR-1 N2: a leaked id must never freeze the next gesture. Queued (in order) so it cannot be
                    // overtaken; if the composable leaves instead, the DisposableEffect above cancels directly.
                    enqueue(PointerEv.Cancel)
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
                // DA-52: a solved puzzle has no tray, so no cell rects
                trayCellsPx = (if (isSolved) emptyList() else layout.trayRows.flatten()).associateWith { piece ->
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
        val overlay = boardOverlay
        if (overlay != null && !isSolved) {
            val space = remember(layout) {
                BoardSpace(layout.dpPerUnit, layout.toDp(Vec2(0.0, 0.0)).x, layout.toDp(Vec2(0.0, 0.0)).y)
            }
            // A plain Box with no pointer input: hit testing falls through to the gesture box below (decision DA-74).
            Box(Modifier.fillMaxSize()) { overlay(space) }
        }
        // DA-52 (REQ-026 rule 2): the tray region belongs to the solved bar. A sibling ABOVE the gesture box, so its
        // controls take their own touches and the drag adapter never sees them.
        val corner = cornerControl
        if (corner != null && controlRect != null) {
            // Measure first (loose: up to the area width and 96 dp high), report the size, then give the content
            // exactly that size at the computed rect. No fixed box: the touch floor is the content's own job.
            SubcomposeLayout(Modifier.fillMaxSize()) { c ->
                val p = subcompose("corner-control") { Box { corner() } }
                    .firstOrNull()
                    ?.measure(Constraints(maxWidth = maxOf(0, c.maxWidth - 2 * PlayLayout.CORNER_INSET_DP.dp.roundToPx()), maxHeight = CORNER_MAX_H_DP.dp.roundToPx()))
                if (p != null && p.width > 0 && p.height > 0) {
                    val s = DpSize(p.width.toDp(), p.height.toDp())
                    if (measured != s) measured = s
                }
                layout(c.maxWidth, c.maxHeight) {
                    p?.place(controlRect.left.dp.roundToPx(), controlRect.top.dp.roundToPx())
                }
            }
        }
        // decision DA-75: composed after the primary slot; measured, then placed from the finished layout. Its size is
        // never written to state, so nothing here can relayout the board.
        val secondary = secondaryCornerControl
        // WO-008 (rev 1 S3, rev 2 E3): the timer is composed only while not solved. The secondary control and the timer
        // share ONE SubcomposeLayout: the secondary is placed first (placeSecondary), then the timer by timerRect from the
        // real obstacle rects. Neither size is written to state, so nothing here can relayout the board.
        val timerSlot = if (isSolved) null else timer
        if (secondary != null || timerSlot != null) {
            SubcomposeLayout(Modifier.fillMaxSize()) { c ->
                val p = if (secondary == null) null else subcompose("secondary-corner-control") { Box { secondary() } }
                    .firstOrNull()
                    ?.measure(Constraints(maxWidth = c.maxWidth, maxHeight = CORNER_MAX_H_DP.dp.roundToPx()))
                val t = if (timerSlot == null) null else subcompose("timer") { Box { timerSlot() } }
                    .firstOrNull()
                    ?.measure(Constraints(maxWidth = maxOf(0, c.maxWidth - 2 * TIMER_INSET_DP.dp.roundToPx()), maxHeight = CORNER_MAX_H_DP.dp.roundToPx()))
                // E3: the widest text the pill can show, measured once per language and font scale (density), never the live text.
                val tpl = if (t == null || t.width <= 0) null else subcompose("timer-template") {
                    BasicText(TIMER_TEMPLATE_TEXT, style = TextStyle(fontSize = TIMER_TEMPLATE_SP.sp, fontFeatureSettings = "tnum"))
                }.firstOrNull()?.measure(Constraints())
                layout(c.maxWidth, c.maxHeight) {
                    var secondaryRect: RectDp? = null
                    if (p != null && p.width > 0 && p.height > 0) {
                        val r = PlayLayout.placeSecondary(layout, placed.second, p.width.toDp().value.toDouble(), p.height.toDp().value.toDouble())
                        if (r != null) p.place(r.left.dp.roundToPx(), r.top.dp.roundToPx())
                        secondaryRect = r
                    }
                    if (t != null && t.width > 0 && t.height > 0) {
                        val live = t.width.toDp().value.toDouble()
                        val template = maxOf(live, (tpl?.width?.toDp()?.value?.toDouble() ?: 0.0) + TIMER_TEMPLATE_PAD_DP)
                        val obstacles = listOfNotNull(controlRect, secondaryRect)
                        val r = PlayLayout.timerRect(layout, obstacles, template, t.height.toDp().value.toDouble())
                        t.place((r.right - live).dp.roundToPx(), r.top.dp.roundToPx())
                    }
                }
            }
        }
        val bar = solvedBar
        if (isSolved && bar != null) {
            val top = layout.trayTop.coerceIn(0.0, h)
            Box(
                Modifier
                    .offset(0.dp, top.dp)
                    .size(maxWidth, (h - top).dp),
            ) { bar() }
        }
    }
}

private const val DEFAULT_CORNER_W_DP = 120.0
private const val CORNER_MAX_H_DP = 96

// E3 template: the widest pill text. The same literal in fi and en (the two format strings are identical); the font scale
// comes from the density the template is measured in. The padding allowance stands for the pill's own horizontal padding
// (the live width, when larger, always wins, so the pill can never overlap).
private const val TIMER_TEMPLATE_TEXT = "88 h 59 min"
private const val TIMER_TEMPLATE_SP = 14.0
private const val TIMER_TEMPLATE_PAD_DP = 20.0

private fun dp(p: androidx.compose.ui.geometry.Offset, density: Float) = Vec2((p.x / density).toDouble(), (p.y / density).toDouble())

/** True while something needs frames: a drag, a pending aid solve, a glide, the shake, the pulse, or the solve animation (until 2.5 s). */
private fun PlaySession.animating(nowMs: Long): Boolean {
    version // observe every mutation
    if (solvePending || drag != null || glides.isNotEmpty() || shake != null || pulse != null) return true
    val s = solved ?: return false
    return nowMs - s.t0 < PlayTiming.CONFETTI_MS + 100L
}

private sealed interface PointerEv {
    data class Down(val id: Int, val p: Vec2) : PointerEv
    data class Move(val id: Int, val p: Vec2) : PointerEv
    data class Up(val id: Int, val p: Vec2) : PointerEv
    data object Cancel : PointerEv
}
