package io.github.jamisuni.tangram.play

import io.github.jamisuni.tangram.content.PuzzleLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// SCAFFOLDING (TASK-026, disposable): PlayLayout.forThumbnail maths. Not an acceptance test.
class PlayLayoutThumbnailScaffoldingTest {
    private val puzzles = PuzzleLibrary.packaged().puzzles

    @Test
    fun thumbnailHasNoTrayAndFitsInsideItsBoxWithPadding() {
        for (p in puzzles) {
            val l = PlayLayout.forThumbnail(100.0, 80.0, p)
            assertTrue("${p.id.value}: no tray rows", l.trayRows.isEmpty())
            assertEquals("${p.id.value}: board rect is the whole box", RectDp(0.0, 0.0, 100.0, 80.0), l.boardRect)
            assertEquals(80.0, l.trayTop, 0.0)
            var minX = Double.MAX_VALUE; var maxX = -Double.MAX_VALUE
            var minY = Double.MAX_VALUE; var maxY = -Double.MAX_VALUE
            for (poly in l.silhouetteDp) for (v in poly) {
                minX = minOf(minX, v.x); maxX = maxOf(maxX, v.x); minY = minOf(minY, v.y); maxY = maxOf(maxY, v.y)
            }
            assertTrue("${p.id.value}: padded left/right", minX >= 100.0 * 0.07 - 1e-6 && maxX <= 100.0 * 0.93 + 1e-6)
            assertTrue("${p.id.value}: padded top/bottom", minY >= 80.0 * 0.07 - 1e-6 && maxY <= 80.0 * 0.93 + 1e-6)
            // centred, and the limiting axis uses 84 percent of the box
            assertEquals(100.0 / 2, (minX + maxX) / 2, 1e-6)
            assertEquals(80.0 / 2, (minY + maxY) / 2, 1e-6)
            val fill = maxOf((maxX - minX) / 100.0, (maxY - minY) / 80.0)
            assertEquals("${p.id.value}: limiting axis fill", 0.84, fill, 1e-6)
        }
    }

    @Test
    fun thumbnailScaleFollowsTheBoxAndKeepsTheUnitMapping() {
        val p = puzzles.first()
        val small = PlayLayout.forThumbnail(60.0, 60.0, p)
        val big = PlayLayout.forThumbnail(120.0, 120.0, p)
        assertEquals(2.0, big.dpPerUnit / small.dpPerUnit, 1e-9)
        // toDp and toUnits invert each other, as for the play layout
        val u = io.github.jamisuni.tangram.kernel.geometry.Vec2(1.5, 2.0)
        val back = big.toUnits(big.toDp(u))
        assertEquals(u.x, back.x, 1e-9)
        assertEquals(u.y, back.y, 1e-9)
    }

    @Test
    fun degenerateBoxDoesNotThrow() {
        val l = PlayLayout.forThumbnail(0.0, 0.0, puzzles.first())
        assertEquals(0.0, l.dpPerUnit, 0.0)
    }
}
