package io.github.jamisuni.tangram.play.acceptance.held

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Silhouette
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.lock.LockSearch
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.Turn
import io.github.jamisuni.tangram.play.DropResolver
import io.github.jamisuni.tangram.play.GestureMachine
import io.github.jamisuni.tangram.play.PlayLayout
import io.github.jamisuni.tangram.play.PlaySession

// ACCEPTANCE-TEST SCAFFOLDING for WO-003 (no acceptance tokens here). Written from the REQs and the frozen seam
// list of designs/WO-003-design.md "Test seams" only. Every access to a member whose shape the design does not
// freeze (RectDp fields, the `where` subtype, the shake value, the layout hook) goes through ONE helper below,
// so a mismatch with the real API is fixed here once. Signatures ASSUMED (logged in the handoff):
//   PlayLayout.compute(areaW: Double, areaH: Double, screenH: Double, LayoutClass, rows, puzzle)
//   RectDp has left/top/right/bottom (or x/y/width/height); toDp/toUnits take and return Vec2
//   timeMs / nowMs are Long; GestureMachine(session, layoutProvider: () -> PlayLayout); pointer ids are literals.

/** The phone tray rows of REQ-035/036 as the design lists them (full set; `play` drops missing pieces). */
internal val PHONE_ROWS: List<List<PieceId>> = listOf(
    listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
    listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
)

internal val PUZZLES: List<Puzzle> by lazy { PuzzleLibrary.packaged().puzzles }

internal fun puzzle(id: String): Puzzle = PUZZLES.first { it.id.value == id }

internal fun layoutFor(p: Puzzle, w: Double = 360.0, h: Double = 640.0, screenH: Double = 780.0): PlayLayout =
    PlayLayout.compute(w, h, screenH, LayoutClass.PHONE, PHONE_ROWS, p)

// ---- rectangles (RectDp) ---------------------------------------------------------------------------------

internal data class R(val l: Double, val t: Double, val r: Double, val b: Double) {
    val w: Double get() = r - l
    val h: Double get() = b - t
    val cx: Double get() = (l + r) / 2
    val cy: Double get() = (t + b) / 2
    val centre: Vec2 get() = Vec2(cx, cy)
    fun contains(p: Vec2): Boolean = p.x >= l && p.x <= r && p.y >= t && p.y <= b
}

internal fun rectOf(o: Any): R {
    fun num(vararg names: String): Double? {
        for (n in names) {
            val getter = "get" + n.replaceFirstChar { it.uppercase() }
            val m = o.javaClass.methods.firstOrNull { it.name == getter && it.parameterCount == 0 }
            val v = m?.invoke(o)
            if (v is Number) return v.toDouble()
        }
        val s = o.toString()
        for (n in names) {
            Regex("""\b$n=(-?[0-9]+(?:\.[0-9]+)?(?:E-?[0-9]+)?)""").find(s)?.let { return it.groupValues[1].toDouble() }
        }
        return null
    }
    val l = num("left", "x", "l")!!
    val t = num("top", "y", "t")!!
    val r = num("right") ?: (l + num("width", "w")!!)
    val b = num("bottom") ?: (t + num("height", "h")!!)
    return R(l, t, r, b)
}

internal fun PlayLayout.cellR(p: PieceId): R = rectOf(cell(p))
internal fun PlayLayout.boardR(): R = rectOf(boardRect)
internal fun PlayLayout.badgeR(s: PlaySession): R = rectOf(badgeRect(s))

/** A tray press point that cannot be covered by the flip badge (it sits at the cell's top right): the cell's bottom left. */
internal fun pressPoint(c: R): Vec2 = Vec2(c.l + 8.0, c.b - 8.0)

// ---- geometry helpers ------------------------------------------------------------------------------------

internal fun v(p: ExactPoint): Vec2 = Vec2(p.x.toDouble(), p.y.toDouble())

internal fun polyOf(p: PlacedPiece): List<Vec2> = p.corners.map(::v)

internal fun centroidOf(poly: List<Vec2>): Vec2 = Vec2(poly.sumOf { it.x } / poly.size, poly.sumOf { it.y } / poly.size)

/** Centroid of a piece pose relative to its local origin (vertex 0), in units. */
internal fun centroidOffset(piece: PieceId, turn: Turn, mirrored: Boolean): Vec2 =
    centroidOf(PieceGeometry.offsets(piece.shape, turn, mirrored).map(::v))

/** Point in a convex polygon (closed), any orientation. */
internal fun inPoly(poly: List<Vec2>, p: Vec2, eps: Double = 1e-9): Boolean {
    var pos = false
    var neg = false
    for (i in poly.indices) {
        val a = poly[i]
        val b = poly[(i + 1) % poly.size]
        val c = (b.x - a.x) * (p.y - a.y) - (b.y - a.y) * (p.x - a.x)
        if (c > eps) pos = true
        if (c < -eps) neg = true
    }
    return !(pos && neg)
}

internal fun solutionPolys(p: Puzzle): List<List<Vec2>> = p.solution.map { s -> s.polygon.map(::v) }

internal fun inSilhouette(p: Puzzle, pt: Vec2): Boolean = solutionPolys(p).any { inPoly(it, pt) }

internal fun silhouetteOf(p: Puzzle): Silhouette = Silhouette(p.solution.map { it.polygon })

internal fun targetsOf(p: Puzzle): List<PlacedPiece> =
    p.solution.map { PieceGeometry.poseOf(it.piece, it.polygon) ?: error("no pose for ${it.piece} in ${p.id.value}") }

/** A build order in which every piece, dropped exactly on its target, wins the kernel lock search at that target. */
internal fun buildOrder(
    p: Puzzle,
    targets: List<PlacedPiece>,
    dpPerUnit: Double,
    already: List<PlacedPiece> = emptyList(),
): List<PlacedPiece> {
    val sil = silhouetteOf(p)
    val r = LockSearch.lockDistance(dpPerUnit)
    val placed = already.toMutableList()
    val rest = targets.filter { t -> already.none { it.piece == t.piece } }.toMutableList()
    val out = ArrayList<PlacedPiece>()
    while (rest.isNotEmpty()) {
        val next = rest.firstOrNull { t ->
            LockSearch.find(sil, placed, t.piece, t.turn, t.mirrored, v(t.at), r)?.at == t.at
        } ?: error("no build order for ${p.id.value}: stuck with ${rest.map { it.piece }}")
        rest.remove(next)
        placed.add(next)
        out.add(next)
    }
    return out
}

/** Float origin (units) at which a piece of this pose, dropped over the board, has NO valid lock (kernel oracle). */
internal fun missOrigin(
    p: Puzzle,
    dpPerUnit: Double,
    piece: PieceId,
    turn: Turn,
    mirrored: Boolean,
    placed: List<PlacedPiece>,
): Vec2 {
    val pts = solutionPolys(p).flatten()
    val minX = pts.minOf { it.x }
    val maxX = pts.maxOf { it.x }
    val minY = pts.minOf { it.y }
    val maxY = pts.maxOf { it.y }
    val centre = Vec2((minX + maxX) / 2, (minY + maxY) / 2)
    val off = centroidOffset(piece, turn, mirrored)
    val sil = silhouetteOf(p)
    val r = LockSearch.lockDistance(dpPerUnit)
    val cands = ArrayList<Vec2>()
    var y = minY
    while (y <= maxY) {
        var x = minX
        while (x <= maxX) {
            cands.add(Vec2(x, y))
            x += 0.25
        }
        y += 0.25
    }
    cands.sortBy { Math.hypot(it.x - centre.x, it.y - centre.y) }
    for (c in cands) {
        val origin = Vec2(c.x - off.x, c.y - off.y)
        if (LockSearch.find(sil, placed, piece, turn, mirrored, origin, r) == null) return origin
    }
    error("no miss spot found for $piece in ${p.id.value}")
}

// ---- session reading -------------------------------------------------------------------------------------

internal fun PlaySession.ps(p: PieceId) = pieces.first { it.piece == p }

/** "Tray", "Board" or "Dragged": the simple name of the `where` value (REQ-012 A2 names exactly these three). */
internal fun PlaySession.where(p: PieceId): String = ps(p).where.javaClass.simpleName

internal fun attach(session: PlaySession, layout: PlayLayout) {
    // PlayArea hands the live layout to the session (design seam list: `.layout`); a JVM test does it here.
    session.javaClass.methods.firstOrNull { it.name == "setLayout" && it.parameterCount == 1 }?.invoke(session, layout)
}

/** True when the session reports a running shake (design seam `.shake`; the value type is not frozen). */
internal fun shakeActive(session: PlaySession): Boolean {
    val s: Any? = session.shake
    return when (s) {
        null -> false
        is Boolean -> s
        is Collection<*> -> s.isNotEmpty()
        is Map<*, *> -> s.isNotEmpty()
        is Number -> s.toDouble() != 0.0
        else -> true
    }
}

/** Calls a member with number arguments by name, converting each number to the parameter type. */
internal fun invokeNum(target: Any, name: String, vararg args: Number): Any? {
    val m = target.javaClass.methods.first { it.name == name && it.parameterCount == args.size }
    val conv = m.parameterTypes.mapIndexed { i, t ->
        when (t) {
            java.lang.Integer.TYPE, Int::class.javaObjectType -> args[i].toInt()
            java.lang.Long.TYPE, Long::class.javaObjectType -> args[i].toLong()
            java.lang.Float.TYPE, Float::class.javaObjectType -> args[i].toFloat()
            else -> args[i].toDouble()
        }
    }
    return m.invoke(target, *conv.toTypedArray())
}


// ---- seams whose package the design does not freeze (found by simple name; fix the list here if needed) -----------

private val SEAM_PACKAGES = listOf(
    "io.github.jamisuni.tangram.play.", "io.github.jamisuni.tangram.play.draw.", "io.github.jamisuni.tangram.play.gesture.",
    "io.github.jamisuni.tangram.play.timeline.", "io.github.jamisuni.tangram.play.layout.",
)

private fun convert(t: Class<*>, a: Any?): Any? = if (a is Number) when (t) {
    java.lang.Integer.TYPE -> a.toInt()
    java.lang.Long.TYPE -> a.toLong()
    java.lang.Float.TYPE -> a.toFloat()
    java.lang.Double.TYPE -> a.toDouble()
    else -> a
} else a

/**
 * Calls `name` on the first of the named classes (Kotlin `object`, or top-level functions of a `...Kt` facade) that has it,
 * searching the packages above; numbers are converted to the parameter type. `internal` members are found by their
 * mangled prefix.
 */
internal fun callSeam(classNames: List<String>, name: String, vararg args: Any?): Any? {
    for (cn in classNames) {
        for (pk in SEAM_PACKAGES) {
            val c = try { Class.forName(pk + cn) } catch (_: ClassNotFoundException) { continue }
            val m = c.methods.firstOrNull { (it.name == name || it.name.startsWith("$name$")) && it.parameterCount == args.size } ?: continue
            val target = try { c.getField("INSTANCE").get(null) } catch (_: NoSuchFieldException) { null }
            val conv = m.parameterTypes.mapIndexed { i, t -> convert(t, args[i]) }
            return m.invoke(target, *conv.toTypedArray())
        }
    }
    error("seam $name not found in $classNames")
}

internal fun callAny(target: Any, name: String, vararg args: Any?): Any? {
    val m = target.javaClass.methods.first { (it.name == name || it.name.startsWith("$name$")) && it.parameterCount == args.size }
    return m.invoke(target, *m.parameterTypes.mapIndexed { i, t -> convert(t, args[i]) }.toTypedArray())
}

/** PieceDrawing.scale(pieceState, layout): the drawn scale in dp per unit (design seam list). */
internal fun drawnScale(session: PlaySession, piece: PieceId, layout: PlayLayout): Double =
    (callSeam(listOf("PieceDrawing"), "scale", session.ps(piece), layout) as Number).toDouble()

/** HitTest.pick(p, layout, session) rendered as text: "null", "Piece(id=ST1)" or "Badge" (Hit.Piece(id) | Hit.Badge). */
internal fun pickText(p: Vec2, layout: PlayLayout, session: PlaySession): String =
    (callSeam(listOf("HitTest"), "pick", p, layout, session)?.toString() ?: "null")

internal fun pictureAlpha(ms: Int): Double = (callSeam(listOf("SolvedTimeline"), "pictureAlpha", ms) as Number).toDouble()

internal fun pulseRadius(ms: Int, reduced: Boolean): Double =
    (callSeam(listOf("PlayTiming", "PlayTimingKt", "PulseKt", "TimelineKt", "SolvedTimeline"), "pulseRadius", ms, reduced) as Number).toDouble()

internal fun parsePath(d: String): Any? = callSeam(listOf("PathData"), "parse", d)

// ---- the scripted finger ---------------------------------------------------------------------------------

/**
 * A scripted touch trace through the real [GestureMachine]. Time is the frame clock: every `frame()` advances it and calls
 * `session.onFrame`, as the adapter does once per displayed frame (design section 4, "Frame sync").
 */
internal class Driver(
    val puzzle: Puzzle,
    val reduced: Boolean = false,
    val layout: PlayLayout = layoutFor(puzzle),
    val resolver: DropResolver = DropResolver(puzzle),
) {
    val session: PlaySession = PlaySession(puzzle, { reduced }, resolver).also { attach(it, layout) }
    val machine = GestureMachine(session) { layout }
    var t: Long = 1000L
    var finger: Vec2 = Vec2(0.0, 0.0)

    fun frame(ms: Long = 16L) {
        t += ms
        session.onFrame(t)
    }

    fun down0(p: Vec2) = machine.down(0, p, t)
    fun move0(p: Vec2) = machine.move(0, p, t)
    fun up0(p: Vec2) = machine.up(0, p, t)
    fun down1(p: Vec2) = machine.down(1, p, t)
    fun move1(p: Vec2) = machine.move(1, p, t)
    fun up1(p: Vec2) = machine.up(1, p, t)
    fun down2(p: Vec2) = machine.down(2, p, t)
    fun move2(p: Vec2) = machine.move(2, p, t)
    fun up2(p: Vec2) = machine.up(2, p, t)

    fun tap(p: Vec2, holdMs: Long = 16L) {
        down0(p)
        frame(holdMs)
        up0(p)
        frame()
    }

    /** Press on [from], cross the 12 dp threshold upwards: the machine has begun a drag. */
    fun startDrag(from: Vec2) {
        down0(from)
        frame()
        finger = Vec2(from.x, from.y - 20.0)
        move0(finger)
        frame()
    }

    /** Steer the finger until the dragged piece's float origin (units) equals [target]; the offset is constant after 120 ms. */
    fun steerOrigin(target: Vec2) {
        repeat(3) {
            frame(150)
            val o = session.drag?.frame?.pose?.origin ?: return@repeat
            val a = layout.toDp(o)
            val b = layout.toDp(target)
            finger = Vec2(finger.x + b.x - a.x, finger.y + b.y - a.y)
            move0(finger)
        }
        frame(150)
    }

    /** Turn (taps) and mirror (badge) in the tray, then drag to the target and release. */
    fun place(target: PlacedPiece) {
        val cell = layout.cellR(target.piece)
        if (target.mirrored != session.ps(target.piece).mirrored) tap(layout.badgeR(session).centre)
        val taps = Math.floorMod(target.turn.steps - session.ps(target.piece).turn.steps, 8)
        repeat(taps) { tap(pressPoint(cell)) }
        startDrag(pressPoint(cell))
        steerOrigin(v(target.at))
        up0(finger)
        frame()
    }

    /**
     * Drag [piece] (at its current tray turn/mirror) to a spot over the board where the kernel oracle says nothing locks, release.
     * Returns the float centre (dp) the piece had at release, so a test can state it was over the board.
     */
    fun missDrop(piece: PieceId): Vec2 {
        val ps = session.ps(piece)
        val origin = missOrigin(puzzle, layout.dpPerUnit, piece, ps.turn, ps.mirrored, session.placed)
        val off = centroidOffset(piece, ps.turn, ps.mirrored)
        startDrag(pressPoint(layout.cellR(piece)))
        steerOrigin(origin)
        val centreDp = layout.toDp(Vec2(origin.x + off.x, origin.y + off.y))
        up0(finger)
        frame()
        return centreDp
    }

    fun placeAll(targets: List<PlacedPiece>) {
        for (tg in buildOrder(puzzle, targets, layout.dpPerUnit, session.placed)) {
            place(tg)
            check(session.placed.any { it.piece == tg.piece && it.at == tg.at }) {
                "${tg.piece} did not lock at its target in ${puzzle.id.value}"
            }
        }
    }
}
