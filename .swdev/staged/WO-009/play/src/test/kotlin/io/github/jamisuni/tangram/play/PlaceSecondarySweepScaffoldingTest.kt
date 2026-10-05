package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PieceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// SCAFFOLDING (TASK-031a, disposable): decision DA-75, the secondary corner control sweep.
class PlaceSecondarySweepScaffoldingTest {
    private val puzzles = PuzzleLibrary.packaged().puzzles
    private val phoneRows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val tabletRows = listOf(PieceId.entries.toList())

    private class Size(val w: Double, val h: Double, val screenH: Double, val cls: LayoutClass, val rows: List<List<PieceId>>)

    private val sizes = listOf(
        Size(360.0, 600.0, 780.0, LayoutClass.PHONE, phoneRows),
        Size(360.0, 620.0, 780.0, LayoutClass.PHONE, phoneRows),
        Size(360.0, 640.0, 780.0, LayoutClass.PHONE, phoneRows),
        Size(360.0, 780.0, 780.0, LayoutClass.PHONE, phoneRows),
        Size(390.0, 700.0, 844.0, LayoutClass.PHONE, phoneRows),
        Size(390.0, 844.0, 844.0, LayoutClass.PHONE, phoneRows),
        Size(800.0, 1100.0, 1280.0, LayoutClass.TABLET, tabletRows),
        Size(800.0, 1180.0, 1280.0, LayoutClass.TABLET, tabletRows),
        Size(1280.0, 700.0, 800.0, LayoutClass.TABLET, tabletRows),
    )
    private val restartWidths = listOf(82.0, 118.0, 170.0, 230.0)

    /** Independent of the product code: separating axes; touching counts as overlap. */
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

    @Test
    fun theSecondaryIsNeverNullAndClearOfSilhouettePrimaryTrayAndEdges() {
        val tally = java.util.TreeMap<String, Int>()
        var cases = 0
        for (rw in restartWidths) for (sz in sizes) for (p in puzzles) {
            val (layout, primary) = PlayLayout.computeWithCorner(sz.w, sz.h, sz.screenH, sz.cls, sz.rows, p, rw, 48.0)
            val tag = "${p.id.value} restart $rw on ${sz.w}x${sz.h}"
            val r = PlayLayout.placeSecondary(layout, primary, 56.0, 40.0)
            assertNotNull("$tag: null", r)
            r!!
            assertEquals(56.0, r.width, 1e-9)
            assertEquals(40.0, r.height, 1e-9)
            for (poly in layout.silhouetteDp) assertTrue("$tag: silhouette", !overlaps(r, poly))
            val grown = RectDp(r.left - 4.0, r.top - 4.0, r.right + 4.0, r.bottom + 4.0)
            for (poly in layout.silhouetteDp) assertTrue("$tag: silhouette clearance", !overlaps(grown, poly))
            val pr = primary.rect
            assertTrue(
                "$tag: primary clearance",
                r.left >= pr.right + 4.0 - 1e-6 || r.right <= pr.left - 4.0 + 1e-6 ||
                    r.top >= pr.bottom + 4.0 - 1e-6 || r.bottom <= pr.top - 4.0 + 1e-6,
            )
            assertTrue("$tag: above trayTop", r.bottom <= layout.trayTop + 1e-6)
            assertTrue("$tag: inside area", r.left >= 0.0 && r.top >= 0.0 && r.right <= sz.w + 1e-6 && r.bottom <= sz.h + 1e-6)
            val b = layout.boardRect
            val kind = when {
                r.left == b.left + 4.0 && r.bottom == b.bottom - 4.0 -> "BOTTOM_LEFT"
                r.right == b.right - 4.0 && r.bottom == b.bottom - 4.0 -> "BOTTOM_RIGHT"
                r.right == b.right - 4.0 && r.top == b.top + 4.0 -> "TOP_RIGHT"
                r.left == b.left + 4.0 && r.top == b.top + 4.0 -> "TOP_LEFT"
                else -> "BESIDE_STRIP"
            }
            tally.merge(kind, 1, Int::plus)
            cases++
        }
        // WO-009 T9d (DA-163): derived from the library, never pinned to a count: every packaged puzzle was swept at every size and width.
        val packaged = checkNotNull(PuzzleLibrary::class.java.classLoader?.getResourceAsStream("tangrams/index.txt")) {
            "tangrams/index.txt is missing from the classpath"
        }.bufferedReader().use { r -> r.readLines().count { it.isNotBlank() } }
        assertEquals("the library holds one puzzle per packaged file", packaged, puzzles.size)
        assertEquals(puzzles.size * sizes.size * restartWidths.size, cases)
        println("SECONDARY_SWEEP cases=$cases distribution=$tally")
    }

    @Test
    fun anImpossibleSizeIsNull() {
        val sz = sizes[0]
        val layout = PlayLayout.compute(sz.w, sz.h, sz.screenH, sz.cls, sz.rows, puzzles.first())
        assertEquals(null, PlayLayout.placeSecondary(layout, null, 400.0, 400.0))
    }
}
