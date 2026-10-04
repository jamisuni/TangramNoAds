package io.github.jamisuni.tangram.acceptance.layout

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performScrollToNode
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.play.BoardTransform

// Scaffolding for the WO-006 touch-target walks (decision DA-99, design WO-006 section 4); no requirement token. The REQ-037 sizes it
// applies are the REQ's own: every player control >= 48 dp in both directions; the previous and next buttons 52 dp; the flip badge's
// touch area 60 dp; a tray piece's touch area is its whole cell.
//
// A "player control" is a node of the MERGED semantics tree with a click or long-click action or an interactive role, classified by
// its own test tag, else by the nearest tagged ancestor (the grid cell's click layer is untagged under the tagged cell); an
// interactive node with no tagged ancestor FAILS the walk. Plus the seven tray cells (the BoardTransform cell rects). Not controls: the
// board, board pieces, text, rating dots, size marks. Skipped: any node whose tag or nearest tagged ancestor's tag starts with `dev-`
// (testing aids are exempt, REQ-037 Rules); the Restart pill while hidden has cleared semantics and is not in the tree at all.

/** [slackDp]: how far under a minimum a measurement may read and still count (layout rounds a node to whole px; tray cells are exact). */
internal class Control(val tag: String, val widthDp: Double, val heightDp: Double, val slackDp: Double = 0.0) {
    fun meets(minDp: Double): Boolean = widthDp + slackDp >= minDp && heightDp + slackDp >= minDp

    override fun toString() = "$tag %.2f x %.2f dp".format(widthDp, heightDp)
}

internal object PlayerControlWalk {
    private val INTERACTIVE_ROLES = listOf(Role.Button, Role.Checkbox, Role.Switch, Role.RadioButton, Role.Tab)

    /** REQ-037 Rules: prev and next 52 dp, the flip badge 60 dp, every other player control 48 dp. */
    fun minimumDp(tag: String): Double = when (tag) {
        "prev-button", "next-button" -> 52.0
        "flip-badge" -> 60.0
        else -> 48.0
    }

    private fun isInteractive(n: SemanticsNode): Boolean {
        val c = n.config
        if (c.contains(SemanticsActions.OnClick) || c.contains(SemanticsActions.OnLongClick)) return true
        val role = c.getOrNull(SemanticsProperties.Role) ?: return false
        return role in INTERACTIVE_ROLES
    }

    /** The node's own test tag, else the nearest tagged ancestor's; null when there is none. */
    private fun classify(n: SemanticsNode): String? {
        var cur: SemanticsNode? = n
        while (cur != null) {
            cur.config.getOrNull(SemanticsProperties.TestTag)?.let { return it }
            cur = cur.parent
        }
        return null
    }

    private fun walk(n: SemanticsNode, out: MutableList<Control>) {
        if (isInteractive(n)) {
            val tag = classify(n) ?: error("an interactive node has no tagged ancestor: the walk cannot classify it (${n.config})")
            if (!tag.startsWith("dev-")) {
                // the LAYOUT box (node size), not touchBoundsInRoot: Compose widens pointer-input touch bounds to 48 dp, which would
                // make the 48 dp rule unable to fail (CR-5 S1; DA-99: node size in px / density)
                val b = n.boundsInRoot
                val d = n.layoutInfo.density.density.toDouble()
                out += Control(tag, b.width / d, b.height / d, slackDp = 1.0 / d)
            }
        }
        for (c in n.children) walk(c, out)
    }

    /** The player controls of the current screen: interactive merged nodes, plus the seven tray cells when a tray is shown. */
    fun controls(rule: ComposeTestRule): List<Control> {
        val out = ArrayList<Control>()
        for (root in rule.onAllNodes(isRoot()).fetchSemanticsNodes()) walk(root, out)
        for (board in rule.onAllNodesWithTag("board").fetchSemanticsNodes()) {
            val t = board.config.getOrNull(BoardTransform) ?: error("the board node carries no BoardTransform")
            val d = board.layoutInfo.density.density.toDouble()
            for ((piece, r) in t.trayCellsPx) out += Control("tray-cell-${piece.name}", r.width / d, r.height / d, slackDp = 0.01)
        }
        return out
    }

    /**
     * The controls of the open grid: it is lazy (one cell per library puzzle), so it is scrolled to every cell in [ids] and the
     * controls are collected at each stop; the last reading of a tag wins.
     */
    fun gridControls(rule: ComposeTestRule, ids: List<String>): List<Control> {
        val byTag = LinkedHashMap<String, Control>()
        for (id in ids) {
            val tag = "grid-cell-$id"
            if (rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()) {
                rule.onNode(hasScrollAction()).performScrollToNode(hasTestTag(tag))
                rule.waitForIdle()
            }
            for (c in controls(rule)) byTag[c.tag] = c
        }
        return byTag.values.toList()
    }

    /** The tags a screen must show, or the walk is blind (design section 4); [ids] are the library puzzle ids for the grid. */
    fun mustFind(screen: WalkScreen, ids: List<String> = emptyList()): Set<String> {
        val trayCells = PieceId.entries.map { "tray-cell-${it.name}" }
        val top = setOf("prev-button", "next-button", "puzzle-counter")
        return when (screen) {
            // the Cat keeps its parallelogram in the tray on both screens, so the 60 dp flip badge must be there (CR-5 N13)
            WalkScreen.NEW -> top + trayCells + "flip-badge"
            WalkScreen.IN_PROGRESS -> top + trayCells + "flip-badge" + "restart-button"
            WalkScreen.SOLVED -> top + setOf("retry-button", "solved-next-button")
            WalkScreen.GRID -> setOf("grid-close") + ids.map { "grid-cell-$it" }
        }
    }

    /** The problems of one screen: a missing must-find tag ("walk blind"), and every control under its REQ-037 minimum. Empty = ok. */
    fun problems(screen: WalkScreen, controls: List<Control>, ids: List<String> = emptyList()): List<String> {
        val out = ArrayList<String>()
        val seen = controls.map { it.tag }.toSet()
        for (t in mustFind(screen, ids)) if (t !in seen) out += "walk blind on $screen: no control '$t' was found"
        for (c in controls) {
            val min = minimumDp(c.tag)
            if (!c.meets(min)) out += "$screen: $c is under its minimum of $min dp"
        }
        return out
    }
}
