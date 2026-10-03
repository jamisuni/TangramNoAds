package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PieceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// ACCEPTANCE TEST (TASK-T5, independent author): decision DA-75, the DEV pill's place on the board. Written from design WO-005
// section 3b and the frozen seam
//   internal fun PlayLayout.Companion.placeSecondary(layout: PlayLayout, primary: ControlPlacement?, widthDp: Double, heightDp: Double): RectDp?
// Legal (section 3b): inside the area, 4 dp clear of the silhouette and of the primary rect, above `layout.trayTop`.
// Order (section 3b): corners of the board rect inset 4 dp - bottom-left, bottom-right, top-right, top-left - then beside Restart
// in its strip (only when primary.corner == STRIP: left = primary.right + 8, top = primary.top), else null; the sweep says null
// never happens (13 puzzles x 9 areas x 4 Restart widths). All geometry here is an independent oracle, never the product's.
// Fixture assumptions (recorded in the hand-back): Restart is 48 dp high (as the WO-004 corner sweep); the screen height of an
// area is max(h, 780) on phones and 1280 / 800 on tablets (the existing sweep's convention).
// No acceptance tokens: this pins an AI decision.
class PlaceSecondarySweepTest {

    private val puzzles = PuzzleLibrary.packaged().puzzles
    private val phoneRows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val tabletRows = listOf(PieceId.entries.toList())

    private class Area(val w: Double, val h: Double, val screenH: Double, val cls: LayoutClass, val rows: List<List<PieceId>>)

    private val areas = listOf(
        Area(360.0, 600.0, 780.0, LayoutClass.PHONE, phoneRows),
        Area(360.0, 620.0, 780.0, LayoutClass.PHONE, phoneRows),
        Area(360.0, 640.0, 780.0, LayoutClass.PHONE, phoneRows),
        Area(360.0, 780.0, 780.0, LayoutClass.PHONE, phoneRows),
        Area(390.0, 700.0, 780.0, LayoutClass.PHONE, phoneRows),
        Area(390.0, 844.0, 844.0, LayoutClass.PHONE, phoneRows),
        Area(800.0, 1100.0, 1280.0, LayoutClass.TABLET, tabletRows),
        Area(800.0, 1180.0, 1280.0, LayoutClass.TABLET, tabletRows),
        Area(1280.0, 700.0, 800.0, LayoutClass.TABLET, tabletRows),
    )
    private val restartWidths = listOf(82.0, 118.0, 170.0, 230.0)
    private val restartHeight = 48.0
    private val devW = 56.0
    private val devH = 40.0

    // ---- independent geometry ------------------------------------------------------------------------------------

    private data class Box(val l: Double, val t: Double, val r: Double, val b: Double) {
        fun grow(d: Double) = Box(l - d, t - d, r + d, b + d)
    }

    private fun boxOf(r: RectDp) = Box(r.left, r.top, r.right, r.bottom)

    /** Separating axes (rect axes + polygon edge normals); touching counts as overlap. */
    private fun overlaps(r: Box, poly: List<Vec2>): Boolean {
        val rect = listOf(Vec2(r.l, r.t), Vec2(r.r, r.t), Vec2(r.r, r.b), Vec2(r.l, r.b))
        val axes = mutableListOf(Vec2(1.0, 0.0), Vec2(0.0, 1.0))
        for (i in poly.indices) {
            val a = poly[i]
            val b = poly[(i + 1) % poly.size]
            axes += Vec2(-(b.y - a.y), b.x - a.x)
        }
        for (ax in axes) {
            fun range(pts: List<Vec2>) = pts.map { it.x * ax.x + it.y * ax.y }.let { it.min() to it.max() }
            val (a0, a1) = range(rect)
            val (b0, b1) = range(poly)
            if (a1 < b0 - 1e-9 || b1 < a0 - 1e-9) return false
        }
        return true
    }

    private fun boxesTouch(a: Box, b: Box) = !(a.r < b.l || b.r < a.l || a.b < b.t || b.b < a.t)

    /**
     * Legal with [clearance] dp to the silhouette and to the primary rect. Probing with 3.99 asks "could it be legal, ties allowed",
     * with 4.01 "is it surely legal": the tie at exactly 4 dp is the product's call, so neither direction asserts on a tie.
     */
    private fun legal(r: Box, layout: PlayLayout, primary: ControlPlacement?, a: Area, clearance: Double): Boolean {
        val eps = 1e-6
        if (r.l < -eps || r.t < -eps || r.r > a.w + eps || r.b > a.h + eps) return false
        if (r.b > layout.trayTop + eps) return false
        if (layout.silhouetteDp.any { overlaps(r.grow(clearance), it) }) return false
        if (primary != null && boxesTouch(r.grow(clearance), boxOf(primary.rect))) return false
        return true
    }

    private fun corners(layout: PlayLayout): List<Pair<String, Box>> {
        val b = layout.boardRect
        return listOf(
            "BOTTOM_LEFT" to Box(b.left + 4.0, b.bottom - 4.0 - devH, b.left + 4.0 + devW, b.bottom - 4.0),
            "BOTTOM_RIGHT" to Box(b.right - 4.0 - devW, b.bottom - 4.0 - devH, b.right - 4.0, b.bottom - 4.0),
            "TOP_RIGHT" to Box(b.right - 4.0 - devW, b.top + 4.0, b.right - 4.0, b.top + 4.0 + devH),
            "TOP_LEFT" to Box(b.left + 4.0, b.top + 4.0, b.left + 4.0 + devW, b.top + 4.0 + devH),
        )
    }

    private fun same(a: Box, b: Box) =
        Math.abs(a.l - b.l) < 1e-6 && Math.abs(a.t - b.t) < 1e-6 && Math.abs(a.r - b.r) < 1e-6 && Math.abs(a.b - b.b) < 1e-6

    /** Runs one case and asserts everything; returns the name of the chosen place. */
    private fun verifyCase(a: Area, layout: PlayLayout, primary: ControlPlacement?, tag: String, allowNull: Boolean = false): String {
        val got = PlayLayout.placeSecondary(layout, primary, devW, devH)
        if (got == null && allowNull) {
            // null = no legal place: then no board corner may be surely legal
            for ((name, c) in corners(layout)) {
                assertTrue("$tag: null although $name is surely legal", !legal(c, layout, primary, a, 4.01))
            }
            return "NULL"
        }
        assertNotNull("$tag: no place for the DEV pill (design: never null for the packaged puzzles)", got)
        got!!
        assertEquals("$tag: width", devW, got.width, 1e-9)
        assertEquals("$tag: height", devH, got.height, 1e-9)
        val box = boxOf(got)
        assertTrue("$tag: not even loosely legal (inside area, 4 dp clear of silhouette and Restart, above trayTop): $got", legal(box, layout, primary, a, 3.99))

        val cands = corners(layout)
        val index = cands.indexOfFirst { same(it.second, box) }
        if (index >= 0) {
            for (earlier in 0 until index) {
                assertTrue(
                    "$tag: chose ${cands[index].first} although ${cands[earlier].first} is surely legal",
                    !legal(cands[earlier].second, layout, primary, a, 4.01),
                )
            }
            return cands[index].first
        }
        // not a corner: it must be the strip place beside Restart, and then no corner was surely legal
        assertNotNull("$tag: the result $got is neither a board corner nor beside Restart (no primary)", primary)
        assertEquals("$tag: a non-corner place is only allowed when Restart is in its strip", ControlCorner.STRIP, primary!!.corner)
        assertEquals("$tag: beside Restart: left", primary.rect.right + 8.0, got.left, 1e-6)
        assertEquals("$tag: beside Restart: top", primary.rect.top, got.top, 1e-6)
        assertTrue("$tag: beside Restart must fit the width less a 4 dp margin", got.right <= a.w - 4.0 + 1e-6)
        for ((name, c) in cands) {
            assertTrue("$tag: went beside Restart although $name is surely legal", !legal(c, layout, primary, a, 4.01))
        }
        return "BESIDE_RESTART"
    }

    // decision DA-75: for every packaged puzzle, area and Restart width the pill gets a place of exactly 56 x 40 dp that is legal
    // (inside the area, 4 dp clear of the silhouette and of Restart, above the solved bar) and is the first legal one in the
    // order bottom-left, bottom-right, top-right, top-left, beside Restart. Never null.
    @Test
    fun decisionDA75_theSweepNeverFailsAndFollowsTheOrder() {
        assertTrue("fixture: the sweep needs the 13 packaged puzzles (found ${puzzles.size})", puzzles.size >= 13)
        val report = StringBuilder()
        var cases = 0
        for (a in areas) for (rw in restartWidths) {
            val tally = java.util.TreeMap<String, MutableList<String>>()
            for (p in puzzles) {
                val (layout, restart) = PlayLayout.computeWithCorner(a.w, a.h, a.screenH, a.cls, a.rows, p, rw, restartHeight)
                val before = layout.boardRect.let { listOf(it.left, it.top, it.right, it.bottom) } + layout.dpPerUnit
                val chosen = verifyCase(a, layout, restart, "${p.id.value} ${a.w.toInt()}x${a.h.toInt()} restart ${rw.toInt()}")
                val after = layout.boardRect.let { listOf(it.left, it.top, it.right, it.bottom) } + layout.dpPerUnit
                assertEquals("placeSecondary changed the layout it was given", before, after)
                tally.getOrPut(chosen) { mutableListOf() }.add(p.id.value)
                cases++
            }
            report.append("area ${a.w.toInt()}x${a.h.toInt()} restart ${rw.toInt()}: ")
            report.append(tally.entries.joinToString("; ") { (k, v) -> if (v.size == puzzles.size) "$k x${v.size}" else "$k $v" })
            report.append(System.lineSeparator())
        }
        assertEquals(areas.size * restartWidths.size * puzzles.size, cases)
        println("PLACESECONDARY SWEEP ($cases cases)" + System.lineSeparator() + report)
    }

    // decision DA-75: without a primary slot (no Restart control at all) the design promises no place for every puzzle: null is
    // allowed (and only where no corner is free); every non-null result obeys the same rules, with bottom-left first.
    @Test
    fun decisionDA75_withoutAPrimarySlotTheSameRulesHold() {
        for (a in areas) for (p in puzzles) {
            val layout = PlayLayout.compute(a.w, a.h, a.screenH, a.cls, a.rows, p)
            val chosen = verifyCase(a, layout, null, "${p.id.value} ${a.w.toInt()}x${a.h.toInt()} no primary", allowNull = true)
            assertTrue("no primary: only board corners are possible, got $chosen", chosen != "BESIDE_RESTART")
        }
    }
}
