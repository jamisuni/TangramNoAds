package io.github.jamisuni.tangram.acceptance

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.model.PieceId

// ACCEPTANCE-TEST ADAPTERS (TASK-T5) for the visible `app` device tests of WO-005. Built on the WO-004 kit of this package
// (`TouchRig`, `touch`, `AppStore`, `ResetStoreRule`, `PieceColours`, `colourDiff`). Seams used: the tags `dev-button`,
// `dev-dialog`, `dev-passcode`, `dev-ok`, `dev-show-solution`, `dev-solution-overlay`, `dev-solution-<PieceId.name>` (design
// WO-005 "Test seams"). The passcode is REQ-046's. Every miss is a loud error.

internal const val AID_PASSCODE = "0417"

/** Opens the DEV dialog, types the passcode, confirms: the tools view (`dev-show-solution`) must be there afterwards. */
internal fun ComposeTestRule.unlockDevAid() {
    touch("dev-button")
    onNodeWithTag("dev-dialog").assertExists()
    onNodeWithTag("dev-passcode").performTextInput(AID_PASSCODE)
    touch("dev-ok")
    waitForIdle()
    onNodeWithTag("dev-show-solution").assertExists()
}

/** Tags of the per-piece overlay nodes (`dev-solution-<PieceId.name>`), not the overlay root itself. */
internal fun ComposeTestRule.overlayNodes(): List<SemanticsNode> = onAllNodes(
    SemanticsMatcher("a dev-solution piece node") { n ->
        val t = n.config.getOrNull(SemanticsProperties.TestTag)
        t != null && t.startsWith("dev-solution-") && t != "dev-solution-overlay"
    },
    useUnmergedTree = true,
).fetchSemanticsNodes()

internal fun SemanticsNode.tag(): String = config.getOrNull(SemanticsProperties.TestTag) ?: error("node without a test tag")

internal fun ComposeTestRule.overlayTags(): List<String> = overlayNodes().map { it.tag() }.sorted()

internal fun expectedOverlayTags(p: Puzzle): List<String> = p.solution.map { "dev-solution-${it.piece.name}" }.sorted()

/** A string resource of the running app by key (the devtools strings are merged into the debug app), loud when absent. */
internal fun aidString(name: String): String {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    val id = ctx.resources.getIdentifier(name, "string", ctx.packageName)
    if (id == 0) error("no string resource $name in ${ctx.packageName}")
    return ctx.getString(id)
}

internal fun ComposeTestRule.boundsOf(tag: String) = onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

/** A saved board piece at its solution pose. */
internal fun solutionSave(p: Puzzle, piece: PieceId): PieceSave.OnBoard {
    val sp = p.solution.first { it.piece == piece }
    val pose = PieceGeometry.poseOf(sp.piece, sp.polygon) ?: error("no pose for $piece")
    return PieceSave.OnBoard(pose.at, pose.turn, pose.mirrored)
}

/** Points of the convex polygon (units) at least [margin] units from every edge, on a [step] grid. */
internal fun interiorPoints(poly: List<Pair<Double, Double>>, margin: Double, step: Double = 0.05): List<Pair<Double, Double>> {
    fun cross(a: Pair<Double, Double>, b: Pair<Double, Double>, p: Pair<Double, Double>) =
        (b.first - a.first) * (p.second - a.second) - (b.second - a.second) * (p.first - a.first)
    fun distToEdge(a: Pair<Double, Double>, b: Pair<Double, Double>, p: Pair<Double, Double>): Double {
        val dx = b.first - a.first
        val dy = b.second - a.second
        val l2 = dx * dx + dy * dy
        val t = (((p.first - a.first) * dx + (p.second - a.second) * dy) / l2).coerceIn(0.0, 1.0)
        return Math.hypot(p.first - (a.first + t * dx), p.second - (a.second + t * dy))
    }
    val out = ArrayList<Pair<Double, Double>>()
    var y = poly.minOf { it.second }
    while (y <= poly.maxOf { it.second }) {
        var x = poly.minOf { it.first }
        while (x <= poly.maxOf { it.first }) {
            val p = x to y
            val signs = poly.indices.map { cross(poly[it], poly[(it + 1) % poly.size], p) }
            val inside = signs.all { it >= 0 } || signs.all { it <= 0 }
            if (inside && poly.indices.all { distToEdge(poly[it], poly[(it + 1) % poly.size], p) >= margin }) out += p
            x += step
        }
        y += step
    }
    return out
}

/** The tray miniature's pixel centroid of [piece] in [shot], below [boardBottom] (as the WO-004 rig finds it). */
internal fun trayCentreOf(shot: Bitmap, boardBottom: Int, piece: PieceId): Offset {
    val want = PieceColours.rgb(piece)
    var sx = 0.0
    var sy = 0.0
    var n = 0
    for (y in boardBottom until shot.height step 2) for (x in 0 until shot.width step 2) {
        if (colourDiff(shot.getPixel(x, y), want) <= 10) {
            sx += x
            sy += y
            n++
        }
    }
    if (n <= 20) error("the $piece miniature is not in the tray ($n matching pixels)")
    return Offset((sx / n).toFloat(), (sy / n).toFloat())
}

/** Rect (l,t,r,b) against a convex polygon by separating axes; touching counts as overlap. */
internal fun rectHitsPolygon(l: Double, t: Double, r: Double, b: Double, poly: List<Pair<Double, Double>>): Boolean {
    val rect = listOf(l to t, r to t, r to b, l to b)
    val axes = mutableListOf(1.0 to 0.0, 0.0 to 1.0)
    for (i in poly.indices) {
        val a = poly[i]
        val c = poly[(i + 1) % poly.size]
        axes += (-(c.second - a.second)) to (c.first - a.first)
    }
    for ((ax, ay) in axes) {
        fun range(pts: List<Pair<Double, Double>>) = pts.map { it.first * ax + it.second * ay }.let { it.min() to it.max() }
        val (a0, a1) = range(rect)
        val (b0, b1) = range(poly)
        if (a1 < b0 - 1e-6 || b1 < a0 - 1e-6) return false
    }
    return true
}
