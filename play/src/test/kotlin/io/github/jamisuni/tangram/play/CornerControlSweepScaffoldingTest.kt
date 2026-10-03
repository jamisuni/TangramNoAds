package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PieceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// SCAFFOLDING (disposable, code review F1 / DA-71): the corner control never sits over the silhouette.
class CornerControlSweepScaffoldingTest {
    private val puzzles = PuzzleLibrary.packaged().puzzles
    private val phoneRows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val tabletRows = listOf(PieceId.entries.toList())

    private class Size(val w: Double, val h: Double, val screenH: Double, val cls: LayoutClass, val rows: List<List<PieceId>>)

    private val sizes = listOf(
        Size(360.0, 640.0, 780.0, LayoutClass.PHONE, phoneRows),
        Size(360.0, 780.0, 780.0, LayoutClass.PHONE, phoneRows),
        Size(390.0, 844.0, 844.0, LayoutClass.PHONE, phoneRows),
        Size(800.0, 1100.0, 1280.0, LayoutClass.TABLET, tabletRows),
    )

    /** Independent of the product code: separating axes (rect axes + polygon edge normals); touching counts as overlap. */
    private fun overlaps(r: RectDp, poly: List<Vec2>): Boolean {
        val rect = listOf(Vec2(r.left, r.top), Vec2(r.right, r.top), Vec2(r.right, r.bottom), Vec2(r.left, r.bottom))
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
            if (a1 < b0 - 1e-6 || b1 < a0 - 1e-6) return false
        }
        return true
    }

    private val controls = listOf(48.0 to 48.0, 120.0 to 48.0, 200.0 to 48.0)

    @Test
    fun theControlNeverIntersectsTheSilhouetteAndKeepsTheClearance() {
        val report = StringBuilder()
        for ((cw, ch) in controls) for (sz in sizes) {
            val tally = java.util.TreeMap<String, MutableList<String>>()
            for (p in puzzles) {
                val (layout, placement) = PlayLayout.computeWithCorner(sz.w, sz.h, sz.screenH, sz.cls, sz.rows, p, cw, ch)
                val r = placement.rect
                assertEquals(cw, r.width, 1e-9)
                assertEquals(ch, r.height, 1e-9)
                val tag = "${p.id.value} ${cw.toInt()}x${ch.toInt()} on ${sz.w.toInt()}x${sz.h.toInt()}"
                for (poly in layout.silhouetteDp) assertTrue("$tag: control overlaps the silhouette", !overlaps(r, poly))
                val grown = RectDp(r.left - 4.0, r.top - 4.0, r.right + 4.0, r.bottom + 4.0)
                for (poly in layout.silhouetteDp) assertTrue("$tag: clearance", !overlaps(grown, poly))
                assertTrue("$tag: inside the area", r.left >= 0.0 && r.top >= 0.0 && r.right <= sz.w + 1e-9 && r.bottom <= sz.h + 1e-9)
                assertTrue(layout.boardRect.bottom <= layout.trayTop + 1e-9)
                if (placement.corner == ControlCorner.STRIP) {
                    assertTrue("$tag: strip is above the board", r.bottom <= layout.boardRect.top + 1e-9)
                } else {
                    assertTrue("$tag: corner is inside the board", r.top >= layout.boardRect.top && r.bottom <= layout.boardRect.bottom)
                }
                tally.getOrPut(placement.corner.name) { mutableListOf() }.add(p.id.value)
            }
            report.append("control ${cw.toInt()}x${ch.toInt()} on ${sz.w.toInt()}x${sz.h.toInt()}: ")
            report.append(tally.entries.joinToString("; ") { (k, v) -> if (k == "TOP_LEFT") "TOP_LEFT x${v.size}" else "$k ${v}" })
            report.append(System.lineSeparator())
        }
        println("SWEEP " + System.lineSeparator() + report)
    }

    @Test
    fun aFreeTopLeftIsAlwaysChosenAndEachLaterCornerWasBlockedBeforeIt() {
        for ((cw, ch) in controls) for (sz in sizes) for (p in puzzles) {
            val (_, placement) = PlayLayout.computeWithCorner(sz.w, sz.h, sz.screenH, sz.cls, sz.rows, p, cw, ch)
            if (placement.corner == ControlCorner.TOP_LEFT) continue
            val plain = PlayLayout.compute(sz.w, sz.h, sz.screenH, sz.cls, sz.rows, p)
            val b = plain.boardRect
            val tl = RectDp(b.left + 4.0, b.top + 4.0, b.left + 4.0 + cw, b.top + 4.0 + ch)
            assertTrue(
                "${p.id.value} ${sz.w}x${sz.h} ${cw}x$ch: top-left was free but ${placement.corner} was chosen",
                tl.right > b.right || PlayLayout.touchesSilhouette(tl, plain.silhouetteDp, 4.0),
            )
        }
    }

    @Test
    fun aControlAsWideAsTheAreaIsClampedInsideIt() {
        for (sz in sizes) for (p in puzzles) {
            val (layout, placement) = PlayLayout.computeWithCorner(sz.w, sz.h, sz.screenH, sz.cls, sz.rows, p, sz.w, 48.0)
            val r = placement.rect
            assertTrue("${p.id.value} ${sz.w}: left", r.left >= 0.0)
            assertTrue("${p.id.value} ${sz.w}: right ${r.right}", r.right <= sz.w - 4.0 + 1e-9)
            assertEquals(sz.w - 8.0, r.width, 1e-9)
            for (poly in layout.silhouetteDp) assertTrue("${p.id.value}: overlap", !overlaps(r, poly))
        }
    }

    @Test
    fun withoutAControlTheLayoutIsTheOldOne() {
        val p = puzzles.first()
        val a = PlayLayout.compute(360.0, 640.0, 780.0, LayoutClass.PHONE, phoneRows, p)
        assertEquals(0.0, a.boardRect.top, 0.0)
    }
}
