package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PieceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// decision DA-140: DECISION TEST (WO-008 T8c v-p), no requirement token. The timer pill's place on the board, design 7 (rev 1 S3, rev 2 E3/E4)
// and "Value shapes". Frozen seam:
//   internal fun PlayLayout.Companion.timerRect(layout: PlayLayout, obstacles: List<RectDp>, widthDp: Double, heightDp: Double): RectDp
// Mandated by the design text:
//  - plain place: the board rect's top-right, inset 8 dp, width capped at the area width minus 16 dp (7);
//  - "if that rect comes within 4 dp of an obstacle it steps below that obstacle (top = obstacle bottom + 4, right-aligned), repeating for
//    the next obstacle" (7); value shapes: "with no obstacle in the way is the top-right rect inset 8 dp; with an obstacle within 4 dp it sits
//    4 dp below the obstacle, right-aligned";
//  - the obstacles are the REAL rects of the Restart control (`computeWithCorner`'s placement) and the DEV pill (`placeSecondary`'s result);
//    release has no DEV pill, so both configurations are run (E4);
//  - the sweep asserts the pill never overlaps either obstacle, stays inside the area and above the tray, and that the no-legal-rect fallback
//    count is 0 (7(a)); it RECORDS, not asserts, how many pills touch the outline;
//  - the step decision is made from a TEMPLATE width (the widest text, "88 h 59 min"), and the live text is drawn right-aligned inside that
//    placement (7, E3(b)): a live pill narrower than the template, right-aligned inside the placed rect, must also clear every obstacle.
// Width bounds (7(b)): the design bounds start as Restart at most 280 dp wide and a pill at most 170 x 44 dp; the Test Author raises them if
// the device-measured widths (MEASURE-8) exceed them. The measured widths replace the numbers in BOUNDS below at that step.
// Fixture conventions are the existing DEV-pill sweep's (PlaceSecondarySweepTest): the nine areas, Restart 48 dp high, DEV 56 x 40 dp.
class TimerPlacementSweepTest {

    private val puzzles = PuzzleLibrary.packaged().puzzles
    private val phoneRows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val tabletRows = listOf(PieceId.entries.toList())

    private class Area(val name: String, val w: Double, val h: Double, val screenH: Double, val cls: LayoutClass, val rows: List<List<PieceId>>)

    private val areas = listOf(
        Area("phone 360x600", 360.0, 600.0, 780.0, LayoutClass.PHONE, phoneRows),
        Area("phone 360x620", 360.0, 620.0, 780.0, LayoutClass.PHONE, phoneRows),
        Area("phone 360x640", 360.0, 640.0, 780.0, LayoutClass.PHONE, phoneRows),
        Area("phone 360x780", 360.0, 780.0, 780.0, LayoutClass.PHONE, phoneRows),
        Area("phone 390x700", 390.0, 700.0, 780.0, LayoutClass.PHONE, phoneRows),
        Area("phone 390x844", 390.0, 844.0, 844.0, LayoutClass.PHONE, phoneRows),
        Area("tablet 800x1100", 800.0, 1100.0, 1280.0, LayoutClass.TABLET, tabletRows),
        Area("tablet 800x1180", 800.0, 1180.0, 1280.0, LayoutClass.TABLET, tabletRows),
        Area("tablet 1280x700", 1280.0, 700.0, 800.0, LayoutClass.TABLET, tabletRows),
    )

    /** The design bounds (7(b)); raise them from the measured widths (MEASURE-8), never silently. */
    private object BOUNDS {
        const val RESTART_MAX_WIDTH = 280.0
        const val PILL_MAX_WIDTH = 170.0
        const val PILL_MAX_HEIGHT = 44.0
    }

    // Restart: fi and en at font scale 1.0 and 2.0, up to the bound. Pill: live "59:59" and "10 h 59 min" and the template "88 h 59 min", at font
    // scale 1.0 (28 dp high) and 2.0 (44 dp high, the bound); the template is the widest, at most the bound.
    private val restartWidths = listOf(82.0, 118.0, 170.0, 230.0, BOUNDS.RESTART_MAX_WIDTH)
    private val restartHeight = 48.0
    private val devW = 56.0
    private val devH = 40.0

    private class Pill(val name: String, val templateW: Double, val liveWs: List<Double>, val h: Double)

    private val pills = listOf(
        Pill("font 1.0", 96.0, listOf(56.0, 88.0, 96.0), 28.0),
        Pill("font 2.0", BOUNDS.PILL_MAX_WIDTH, listOf(110.0, 150.0, BOUNDS.PILL_MAX_WIDTH), BOUNDS.PILL_MAX_HEIGHT),
    )

    private fun overlaps(a: RectDp, b: RectDp) = !(a.right <= b.left || b.right <= a.left || a.bottom <= b.top || b.bottom <= a.top)

    private fun within(a: RectDp, margin: Double, b: RectDp) =
        !(a.right + margin <= b.left || b.right + margin <= a.left || a.bottom + margin <= b.top || b.bottom + margin <= a.top)

    private fun plain(layout: PlayLayout, w: Double, h: Double): RectDp {
        val width = minOf(w, layout.areaWidth - 16.0)
        val right = layout.boardRect.right - 8.0
        val top = layout.boardRect.top + 8.0
        return RectDp(right - width, top, right, top + h)
    }

    private fun touchesOutline(r: RectDp, layout: PlayLayout): Boolean {
        val rect = listOf(Vec2(r.left, r.top), Vec2(r.right, r.top), Vec2(r.right, r.bottom), Vec2(r.left, r.bottom))
        for (poly in layout.silhouetteDp) {
            val axes = mutableListOf(Vec2(1.0, 0.0), Vec2(0.0, 1.0))
            for (i in poly.indices) {
                val a = poly[i]
                val b = poly[(i + 1) % poly.size]
                axes += Vec2(-(b.y - a.y), b.x - a.x)
            }
            var separated = false
            for (ax in axes) {
                fun range(pts: List<Vec2>) = pts.map { it.x * ax.x + it.y * ax.y }.let { it.min() to it.max() }
                val (a0, a1) = range(rect)
                val (b0, b1) = range(poly)
                if (a1 < b0 - 1e-9 || b1 < a0 - 1e-9) { separated = true; break }
            }
            if (!separated) return true
        }
        return false
    }

    // decision DA-140: the sweep. Every packaged puzzle x nine areas x five Restart widths x two pill sizes x (with the DEV pill, without it).
    @Test fun thePillIsAlwaysLegalNeverMovesWithItsTextAndTheFallbackIsNeverNeeded() {
        assertTrue("fixture: the sweep needs the 13 packaged puzzles (found ${puzzles.size})", puzzles.size >= 13)
        var cases = 0
        var fallbacks = 0
        var touchingOutline = 0
        var stepped = 0
        val problems = ArrayList<String>()
        for (puzzle in puzzles) for (a in areas) for (rw in restartWidths) {
            val (layout, restart) = PlayLayout.computeWithCorner(a.w, a.h, a.screenH, a.cls, a.rows, puzzle, rw, restartHeight)
            val dev = PlayLayout.placeSecondary(layout, restart, devW, devH)
            for (withDev in listOf(true, false)) {
                // the Restart rect is an obstacle even while Restart is hidden (E3a): the obstacle list never changes within an attempt
                val obstacles = buildList { add(restart.rect); if (withDev && dev != null) add(dev) }
                for (pill in pills) {
                    cases++
                    val tag = "${puzzle.id.value} | ${a.name} | restart ${rw} | ${pill.name} | ${if (withDev) "debug (DEV pill)" else "release (no secondary)"}"
                    val placed = PlayLayout.timerRect(layout, obstacles, pill.templateW, pill.h)
                    val again = PlayLayout.timerRect(layout, obstacles, pill.templateW, pill.h)
                    if (placed != again) problems += "$tag: not deterministic: $placed vs $again"

                    val legal = placed.left >= -1e-6 && placed.top >= -1e-6 && placed.right <= layout.areaWidth + 1e-6 && placed.bottom <= layout.areaHeight + 1e-6 &&
                        placed.bottom <= layout.trayTop + 1e-6 && obstacles.none { overlaps(placed, it) }
                    if (!legal) { fallbacks++; problems += "$tag: no legal place (fallback) $placed, obstacles $obstacles, trayTop ${layout.trayTop}" }

                    // right-aligned at the board's top-right whenever the plain corner is free; otherwise only the top changed (stepped below)
                    val p = plain(layout, pill.templateW, pill.h)
                    val free = obstacles.none { within(p, 4.0, it) }
                    if (free) {
                        if (placed != p) problems += "$tag: the top-right corner is free but the pill is at $placed, not $p"
                    } else {
                        stepped++
                        if (legal && Math.abs(placed.right - p.right) > 1e-6) problems += "$tag: a stepped pill must stay right-aligned: $placed vs $p"
                        if (legal && placed.top < p.top - 1e-6) problems += "$tag: a stepped pill never moves above the corner: $placed"
                    }

                    // the live text, right-aligned inside the template placement, never leaves it and so never reaches an obstacle (E3b)
                    for (lw in pill.liveWs) {
                        val live = RectDp(placed.right - minOf(lw, placed.width), placed.top, placed.right, placed.bottom)
                        if (live.left < placed.left - 1e-6 || live.right > placed.right + 1e-6) problems += "$tag: live width $lw leaves the placement"
                        if (legal && obstacles.any { overlaps(live, it) }) problems += "$tag: live width $lw overlaps an obstacle"
                    }
                    if (touchesOutline(placed, layout)) touchingOutline++
                }
            }
        }
        println("TimerPlacementSweepTest: cases=$cases fallbacks=$fallbacks stepped=$stepped pillsTouchingOutline(recorded, not asserted)=$touchingOutline")
        assertEquals("no-legal-rect fallbacks (a nonzero count is a design finding for the Design Author, never a loosened test): ${problems.take(5)}", 0, fallbacks)
        assertTrue("sweep problems (${problems.size}), first: ${problems.take(5)}", problems.isEmpty())
        assertTrue("fixture: the sweep must have exercised the stepping rule at least once", stepped > 0)
    }

    // decision DA-140, value shapes: "timerRect with no obstacle in the way is the top-right rect inset 8 dp"
    @Test fun withNoObstacleThePillIsTheTopRightRectInsetEightDp() {
        val puzzle = puzzles.first()
        val a = areas[5]
        val (layout, _) = PlayLayout.computeWithCorner(a.w, a.h, a.screenH, a.cls, a.rows, puzzle, 82.0, restartHeight)
        val r = PlayLayout.timerRect(layout, emptyList(), 96.0, 28.0)
        assertEquals(layout.boardRect.right - 8.0, r.right, 1e-9)
        assertEquals(layout.boardRect.top + 8.0, r.top, 1e-9)
        assertEquals(96.0, r.width, 1e-9)
        assertEquals(28.0, r.height, 1e-9)
    }

    // decision DA-140, value shapes: "with an obstacle within 4 dp it sits 4 dp below the obstacle, right-aligned"
    @Test fun withAnObstacleInTheWayThePillSitsFourDpBelowItRightAligned() {
        val puzzle = puzzles.first()
        val a = areas[5]
        val (layout, _) = PlayLayout.computeWithCorner(a.w, a.h, a.screenH, a.cls, a.rows, puzzle, 82.0, restartHeight)
        val corner = plain(layout, 96.0, 28.0)
        val obstacle = RectDp(corner.left - 20.0, corner.top, corner.right, corner.top + 40.0) // covers the corner exactly
        val r = PlayLayout.timerRect(layout, listOf(obstacle), 96.0, 28.0)
        assertEquals("4 dp below the obstacle", obstacle.bottom + 4.0, r.top, 1e-9)
        assertEquals("right-aligned as before", corner.right, r.right, 1e-9)
        assertEquals(96.0, r.width, 1e-9)
        // an obstacle 5 dp away (nothing within 4 dp) does not move the pill
        val far = RectDp(corner.left, corner.top - 45.0, corner.right, corner.top - 5.0)
        assertEquals(corner, PlayLayout.timerRect(layout, listOf(far), 96.0, 28.0))
    }

    // decision DA-140: "repeating for the next obstacle": stepping below the first obstacle lands in the second, so it steps below that too
    @Test fun twoObstaclesInAColumnStepTheSecondTimeToo() {
        val puzzle = puzzles.first()
        val a = areas[5]
        val (layout, _) = PlayLayout.computeWithCorner(a.w, a.h, a.screenH, a.cls, a.rows, puzzle, 82.0, restartHeight)
        val corner = plain(layout, 96.0, 28.0)
        val first = RectDp(corner.left, corner.top, corner.right, corner.top + 40.0)
        val second = RectDp(corner.left, first.bottom + 4.0, corner.right, first.bottom + 4.0 + 40.0)
        val r = PlayLayout.timerRect(layout, listOf(first, second), 96.0, 28.0)
        assertEquals(second.bottom + 4.0, r.top, 1e-9)
        assertEquals(corner.right, r.right, 1e-9)
    }

    // decision DA-140 (7): the width is capped at the area width minus 16 dp, so a pill can never be wider than the area allows
    @Test fun aPillWiderThanTheAreaIsCappedAtTheAreaWidthMinusSixteen() {
        val puzzle = puzzles.first()
        val a = areas[0]
        val (layout, _) = PlayLayout.computeWithCorner(a.w, a.h, a.screenH, a.cls, a.rows, puzzle, 82.0, restartHeight)
        val r = PlayLayout.timerRect(layout, emptyList(), 1000.0, 28.0)
        assertEquals(layout.areaWidth - 16.0, r.width, 1e-9)
    }
}
