package io.github.jamisuni.tangram.content.acceptance

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.Picture
import io.github.jamisuni.tangram.contracts.puzzle.PicturePoint
import io.github.jamisuni.tangram.contracts.puzzle.PictureShape
import io.github.jamisuni.tangram.contracts.puzzle.PictureStyle
import io.github.jamisuni.tangram.contracts.puzzle.Rgb
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// HELD-OUT (REQ-039.A1, library level). Goes only to Test & Verify.
// "No picture paints outside its puzzle's outline" is enforced by clipping at draw time (Picture doc: "everything is clipped
// to the silhouette"), an on-screen assertion carried to WO-003 (WO-002 Scope, decision DA-12). At library level the
// mandated part is that the library hands over the picture exactly as the file states it (it is what gets clipped) and
// the exact silhouette it is clipped to. Field mapping follows Tangrams/puzzle.schema.json and the Picture* contract docs.
class Req039A1PictureTest {

    private fun rgb(text: String): Rgb = Rgb(text.removePrefix("#").toInt(16))
    private fun num(e: JsonElement): Double = e.jsonPrimitive.double
    private fun pt(e: JsonElement): PicturePoint = PicturePoint(num(e.jsonArray[0]), num(e.jsonArray[1]))
    private fun optRgb(o: JsonObject, k: String): Rgb? = o[k]?.let { rgb(it.jsonPrimitive.content) }
    private fun optNum(o: JsonObject, k: String): Double? = o[k]?.let(::num)

    private fun style(o: JsonObject) = PictureStyle(optRgb(o, "fill"), optRgb(o, "stroke"), optNum(o, "width"), optNum(o, "opacity"))

    private fun expectedShape(o: JsonObject): PictureShape {
        val s = style(o)
        return when (val type = o.getValue("type").jsonPrimitive.content) {
            "polygon" -> PictureShape.Polygon(o.getValue("points").jsonArray.map(::pt), s)
            "rect" -> PictureShape.Rect(num(o.getValue("x")), num(o.getValue("y")), num(o.getValue("w")), num(o.getValue("h")), optNum(o, "rx"), s)
            "circle" -> PictureShape.Circle(pt(o.getValue("c")), num(o.getValue("r")), s)
            "ellipse" -> PictureShape.Ellipse(pt(o.getValue("c")), num(o.getValue("rx")), num(o.getValue("ry")), s)
            "line" -> PictureShape.Line(pt(o.getValue("from")), pt(o.getValue("to")), s)
            "path" -> PictureShape.Path(o.getValue("d").jsonPrimitive.content, s)
            else -> error("unknown art type $type in a shipped file")
        }
    }

    // REQ-039.A1 (library level, decision DA-12) -- every puzzle's picture is the file's picture: base colour, every shape in
    // order with its numbers and paint. Nothing dropped, reordered or rounded.
    @Test
    fun everyPictureIsCarriedExactlyAsTheFileStatesIt() { // REQ-039.A1
        for (p in PuzzleLibrary.packaged().puzzles) {
            val art = Fx.raw(p.id.value).getValue("art").jsonObject
            val want = Picture(rgb(art.getValue("base").jsonPrimitive.content), art.getValue("shapes").jsonArray.map { expectedShape(it.jsonObject) })
            assertEquals("${p.id.value} base", want.base, p.picture.base)
            assertEquals("${p.id.value} shape count", want.shapes.size, p.picture.shapes.size)
            want.shapes.forEachIndexed { i, w -> assertEquals("${p.id.value} shape $i", w, p.picture.shapes[i]) }
        }
    }

    // REQ-039.A1: the clip region is the puzzle's exact outline -- the solution polygons handed over must be the golden's, so
    // "inside the outline" is decided on exact geometry. (Library level; pixels are WO-003.)
    @Test
    fun theOutlineThePictureIsClippedToIsTheExactSolution() { // REQ-039.A1
        for (p in PuzzleLibrary.packaged().puzzles) {
            val g = AcceptanceGolden.puzzles.getValue(p.id.value)
            for (s in p.solution) assertEquals("${p.id.value} ${s.piece}", g.polygons.getValue(s.piece).toSet(), s.polygon.toSet())
        }
    }

    // REQ-039 Rules: "flat shapes, 3-8 colours and no text" -- a path's `d` is carried verbatim (never altered) and unset paint is
    // null, not a default colour (PictureStyle doc: "`null` means not set in the file").
    @Test
    fun unsetPaintStaysNullAndPathDataStaysVerbatim() { // REQ-039.A1
        val cat = PuzzleLibrary.packaged().puzzle(io.github.jamisuni.tangram.contracts.puzzle.PuzzleId("animals-cat"))!!
        val paths = cat.picture.shapes.filterIsInstance<PictureShape.Path>()
        assertTrue(paths.isNotEmpty())
        assertEquals("M3.3,3.35 Q3.7,3.55 4,3.35", paths.first().data)
        assertNull("a path in the file with stroke and no fill has no fill", paths.first().style.fill)
        assertEquals(Rgb(0xC8651F), paths.first().style.stroke)
        assertEquals(0.16, paths.first().style.strokeWidth!!, 0.0)
        assertNull(paths.first().style.opacity)
        // exact decimal tokens: 2.15 is the double of "2.15", not a rounded or float-narrowed value
        val firstPoly = cat.picture.shapes.filterIsInstance<PictureShape.Polygon>().first()
        assertEquals(PicturePoint(2.15, 1.55), firstPoly.points.first())
    }
}
