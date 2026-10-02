package io.github.jamisuni.tangram.play.acceptance

import androidx.compose.ui.test.junit4.createComposeRule
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.play.PlaySession
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** REQ-011 A1/A2 on the real canvas, once per shipped puzzle (design "Test seams": pixel tests, `captureToImage`). */
@RunWith(Parameterized::class)
class BoardPixelsTest(private val id: String) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun ids(): List<String> = PUZZLES.map { it.id.value }
    }

    @get:Rule
    val rule = createComposeRule()

    private val p get() = puzzle(id)

    private fun silhouetteColourAndBackground(shot: Shot): Pair<Int, Int> {
        val l = layoutFor(p)
        val polys = solutionPolys(p)
        val big = polys.maxByOrNull { poly -> poly.indices.sumOf { i -> poly[i].x * poly[(i + 1) % poly.size].y - poly[(i + 1) % poly.size].x * poly[i].y }.let { abs(it) } }!!
        val inside = shot.px(l.toDp(centroidOf(big)))
        val all = polys.flatten()
        val outside = shot.px(l.toDp(Vec2(all.minOf { it.x } - 0.15, all.minOf { it.y } - 0.15)))
        return inside to outside
    }

    private fun abs(d: Double) = if (d < 0) -d else d

    // REQ-011.A1 - "No inner line or slot outline is visible on an unsolved puzzle."
    // Pixels on the midpoints of every edge shared by two solution pieces equal the silhouette colour (REQ-011 rule:
    // "One colour for the whole silhouette"), and that colour differs from the background (distinguishing fixture).
    @Test
    fun req011_A1_noInnerLineOrSlotOutlineIsVisible() {
        val session = PlaySession(p, { true })
        rule.showPlay(session)
        val shot = rule.shot()
        val l = layoutFor(p)
        val (silColour, bg) = silhouetteColourAndBackground(shot)
        assertTrue("fixture: silhouette colour must differ from the background", diff(silColour, bg) > 40)
        val interior = edgeSamples(p).filter { it.interior }
        assertTrue("fixture: ${p.id.value} has inner edges to test", interior.isNotEmpty())
        for (s in interior) {
            val c = shot.px(l.toDp(s.at))
            assertTrue("${p.id.value}: inner edge at ${s.at} is visible (diff ${diff(c, silColour)})", diff(c, silColour) <= 4)
        }
        // the slots are not outlined either: no pixel along the inside of any piece boundary differs from the silhouette
        for (poly in solutionPolys(p)) {
            val c = centroidOf(poly)
            assertTrue("piece interior at $c", diff(shot.px(l.toDp(c)), silColour) <= 4)
        }
    }

    // REQ-011.A2 - "The silhouette outline matches the puzzle's solution outline exactly."
    // Just inside every outline edge the pixel is the silhouette colour, just outside it is the board background.
    @Test
    fun req011_A2_theSilhouetteOutlineMatchesTheSolutionOutline() {
        val session = PlaySession(p, { true })
        rule.showPlay(session)
        val shot = rule.shot()
        val l = layoutFor(p)
        val (silColour, bg) = silhouetteColourAndBackground(shot)
        val delta = 3.0 / (l.dpPerUnit * shot.density) // 3 px in units
        var checked = 0
        for (s in edgeSamples(p).filter { !it.interior }) {
            val a = Vec2(s.at.x + s.normal.x * delta, s.at.y + s.normal.y * delta)
            val b = Vec2(s.at.x - s.normal.x * delta, s.at.y - s.normal.y * delta)
            val aIn = inSilhouette(p, a)
            val inner = if (aIn) a else b
            val outer = if (aIn) b else a
            if (inSilhouette(p, outer) || !inSilhouette(p, inner)) continue
            assertTrue("${p.id.value}: 3 px inside the outline at ${s.at} is not silhouette colour", diff(shot.px(l.toDp(inner)), silColour) <= 6)
            assertTrue("${p.id.value}: 3 px outside the outline at ${s.at} is not background", diff(shot.px(l.toDp(outer)), bg) <= 6)
            checked++
        }
        assertTrue("fixture: outline samples were checked ($checked)", checked >= 4)
    }
}
