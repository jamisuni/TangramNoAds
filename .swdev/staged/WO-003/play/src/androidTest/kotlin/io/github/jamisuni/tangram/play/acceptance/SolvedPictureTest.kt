package io.github.jamisuni.tangram.play.acceptance

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Size
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.play.PlaySession
import io.github.jamisuni.tangram.play.draw.drawPicture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * REQ-023 A1/A2 and REQ-039 A1 on the real canvas, once per shipped puzzle: the puzzle is solved with injected touches, the
 * test clock runs past the confetti (2400 ms, REQ-023 rules) and reduced motion is on, then pixels are read.
 */
@RunWith(Parameterized::class)
class SolvedPictureTest(private val id: String) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun ids(): List<String> = PUZZLES.map { it.id.value }
    }

    @get:Rule
    val rule = createComposeRule()

    private val p get() = puzzle(id)

    private class Solved(val shot: Shot, val ref: Bitmap, val bg: Int, val layout: io.github.jamisuni.tangram.play.PlayLayout)

    private fun solveAndShoot(): Solved {
        val session = PlaySession(p, { true }) // reduced motion: no pop, no confetti; the picture still appears (REQ-023 rules)
        rule.showPlay(session)
        val layout = layoutFor(p)
        val driver = TouchDriver(rule, session, layout, p)
        driver.placeAll(targetsOf(p))
        assertEquals("the puzzle is solved by its last lock", PuzzleState.SOLVED, session.state)
        driver.advance(3000) // > 600 delay + 800 fade, and > 2400 confetti
        val shot = rule.shot()
        val d = shot.density
        val img = ImageBitmap((AREA_W * d).toInt(), (AREA_H * d).toInt())
        CanvasDrawScope().draw(Density(d), LayoutDirection.Ltr, Canvas(img), Size(img.width.toFloat(), img.height.toFloat())) {
            drawRect(Color.White)
            drawPicture(p.picture, layout)
        }
        val all = solutionPolys(p).flatten()
        val bg = shot.px(layout.toDp(Vec2(all.minOf { it.x } - 0.15, all.minOf { it.y } - 0.15)))
        return Solved(shot, img.asAndroidBitmap(), bg, layout)
    }

    private fun Solved.refPx(pt: Vec2): Int {
        val d = shot.density
        return ref.getPixel((layout.toDp(pt).x * d).toInt().coerceIn(0, ref.width - 1), (layout.toDp(pt).y * d).toInt().coerceIn(0, ref.height - 1))
    }

    // REQ-023.A1 - "The picture's outline is identical to the silhouette's."
    // Outside the outline nothing is painted (background); inside, the picture's own pixels (the reference render of the
    // picture alone) show right up to the outline.
    @Test
    fun req023_A1_theSolvedPicturesOutlineIsTheSilhouettesOutline() {
        val s = solveAndShoot()
        val delta = 3.0 / (s.layout.dpPerUnit * s.shot.density)
        var outside = 0
        var inside = 0
        for (e in edgeSamples(p).filter { !it.interior }) {
            val a = Vec2(e.at.x + e.normal.x * delta, e.at.y + e.normal.y * delta)
            val b = Vec2(e.at.x - e.normal.x * delta, e.at.y - e.normal.y * delta)
            val aIn = inSilhouette(p, a)
            val inner = if (aIn) a else b
            val outer = if (aIn) b else a
            if (inSilhouette(p, outer) || !inSilhouette(p, inner)) continue
            assertTrue("${p.id.value}: picture paints outside the outline at ${e.at}", diff(s.shot.px(s.layout.toDp(outer)), s.bg) <= 6)
            outside++
            val expected = s.refPx(inner)
            if (diff(expected, s.bg) > 20) { // only where the picture is distinguishable from the background
                assertTrue("${p.id.value}: picture is missing 3 px inside the outline at ${e.at}", diff(s.shot.px(s.layout.toDp(inner)), expected) <= 14)
                inside++
            }
        }
        assertTrue("fixture: outline samples checked ($outside)", outside >= 4)
        assertTrue("fixture: picture distinguishable from the background at $inside of $outside samples", inside >= outside / 2)
    }

    // REQ-023.A2 - "No piece boundary is visible in the picture."
    // On every edge between two former pieces the pixel equals the picture rendered alone (a piece edge would be white/coloured).
    @Test
    fun req023_A2_noPieceBoundaryIsVisibleInThePicture() {
        val s = solveAndShoot()
        var distinct = 0
        val interior = edgeSamples(p).filter { it.interior }
        assertTrue("fixture: inner edges exist", interior.isNotEmpty())
        for (e in interior) {
            val expected = s.refPx(e.at)
            val actual = s.shot.px(s.layout.toDp(e.at))
            assertTrue("${p.id.value}: a piece boundary shows at ${e.at} (diff ${diff(actual, expected)})", diff(actual, expected) <= 14)
            if (diff(expected, rgb(0xFFFFFF)) > 25) distinct++
        }
        assertTrue("fixture: the picture is not white along the boundaries ($distinct of ${interior.size})", distinct >= interior.size / 2)
    }

    // REQ-039.A1 - "No picture paints outside its puzzle's outline."
    // A grid over the whole board: every pixel clear of the outline (and of the board's own rounded edge) is background.
    @Test
    fun req039_A1_noPicturePaintsOutsideItsOutline() {
        val s = solveAndShoot()
        val b = s.layout.boardR()
        val margin = 4.0 / s.layout.dpPerUnit // 4 dp in units
        var y = b.t + 8.0
        var tested = 0
        while (y < b.b - 8.0) {
            var x = b.l + 8.0
            while (x < b.r - 8.0) {
                val u = s.layout.toUnits(Vec2(x, y))
                if (!inSilhouette(p, u) && distToOutlineUnits(p, u) > margin) {
                    assertTrue("${p.id.value}: paint outside the outline at dp ($x, $y)", diff(s.shot.px(Vec2(x, y)), s.bg) <= 6)
                    tested++
                }
                x += 4.0
            }
            y += 4.0
        }
        assertTrue("fixture: enough background samples ($tested)", tested > 200)
    }
}
