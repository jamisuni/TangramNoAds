package io.github.jamisuni.tangram.content

import io.github.jamisuni.tangram.contracts.puzzle.Picture
import io.github.jamisuni.tangram.contracts.puzzle.PicturePoint
import io.github.jamisuni.tangram.contracts.puzzle.PictureShape
import io.github.jamisuni.tangram.contracts.puzzle.PictureStyle
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleCategory
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleKind
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleTitle
import io.github.jamisuni.tangram.contracts.puzzle.Rgb
import io.github.jamisuni.tangram.contracts.puzzle.SolutionPiece
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.Silhouette
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.Turn
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** A puzzle file as read: [name] includes `.json`. */
class PuzzleFile(val name: String, val text: String)

internal sealed interface ParseResult {
    data class Parsed(val puzzle: Puzzle) : ParseResult
    data class Rejected(val fileName: String, val reason: String) : ParseResult
}

/** `tangram-puzzle/1` to contract types. Never throws: the one try below is the G-10 boundary for stored data. */
internal object PuzzleParser {

    private val ID_PATTERN = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
    private val COLOR_PATTERN = Regex("^#[0-9A-Fa-f]{6}$")
    private val INT_PATTERN = Regex("^-?[0-9]+$")

    fun parse(file: PuzzleFile): ParseResult = try {
        ParseResult.Parsed(parseOrThrow(file))
    } catch (e: RuntimeException) {
        ParseResult.Rejected(file.name, e.message ?: e.javaClass.simpleName)
    }

    private fun parseOrThrow(file: PuzzleFile): Puzzle {
        val root = Json.parseToJsonElement(file.text) as? JsonObject ?: bad("top level is not an object")
        require(str(root, "format") == "tangram-puzzle/1") { "unknown format" }
        val id = str(root, "id")
        require(ID_PATTERN.matches(id)) { "id is not kebab-case: $id" }
        require(file.name == "$id.json") { "id $id does not match file name ${file.name}" }

        val titleObj = obj(root, "title")
        val en = str(titleObj, "en")
        val fi = str(titleObj, "fi")
        require(en.isNotBlank() && fi.isNotBlank()) { "blank title" }

        val categoryName = str(root, "category")
        val category = PuzzleCategory.entries.firstOrNull { it.name.lowercase(Locale.ROOT) == categoryName }
            ?: bad("unknown category $categoryName")

        val rating = intToken(need(root, "difficulty"), "difficulty")
        require(rating in 1..5) { "difficulty out of range" }

        val kindElement = root["kind"]
        val kind = if (kindElement == null) {
            PuzzleKind.FULL
        } else {
            val name = primitiveString(kindElement, "kind")
            PuzzleKind.entries.firstOrNull { it.name.lowercase(Locale.ROOT) == name } ?: bad("unknown kind $name")
        }

        val solution = (need(root, "solution") as? JsonArray ?: bad("solution is not an array")).map(::solutionPiece)
        require(solution.map { it.piece }.toSet().size == solution.size) { "a piece is used twice" }
        if (kind == PuzzleKind.MINI) {
            require(solution.size in 1..6) { "a mini puzzle uses 1..6 pieces" }
        } else {
            require(solution.size == 7) { "this kind uses all seven pieces" }
        }
        // A bad outline (or arithmetic overflow in it) is a rejection here, not a crash in a consumer.
        Silhouette(solution.map { it.polygon })

        val picture = picture(obj(root, "art"))

        val flag = (root["provenance"] as? JsonObject)?.get("reviewedByHuman")
        val reviewed = flag is JsonPrimitive && flag !is JsonNull && !flag.isString && flag.content == "true"

        return Puzzle(PuzzleId(id), PuzzleTitle(en, fi), category, rating, kind, solution, picture, reviewed)
    }

    private fun solutionPiece(e: JsonElement): SolutionPiece {
        val o = e as? JsonObject ?: bad("solution entry is not an object")
        val pieceName = str(o, "piece")
        val piece = PieceId.entries.firstOrNull { it.name == pieceName } ?: bad("unknown piece $pieceName")
        val hasPolygon = "polygon" in o
        val hasPose = "rot" in o || "flip" in o || "at" in o
        require(hasPolygon != hasPose) { "$pieceName: give either polygon or rot/flip/at" }
        if (hasPolygon) {
            val arr = o.getValue("polygon") as? JsonArray ?: bad("polygon is not an array")
            val polygon = arr.map(ExactNumbers::point)
            val pose = PieceGeometry.poseOf(piece, polygon) ?: bad("$pieceName polygon is not that piece")
            val corners = PieceGeometry.corners(piece, pose.turn, pose.mirrored, pose.at)
            require(cyclicEqual(polygon, corners)) { "$pieceName polygon vertices are not in order" }
            return SolutionPiece(piece, polygon)
        }
        val rot = o["rot"]?.let { intToken(it, "rot") } ?: 0
        require(rot in 0..7) { "rot out of range" }
        val flipElement = o["flip"]
        val flip = if (flipElement == null) {
            false
        } else {
            require(
                flipElement is JsonPrimitive && flipElement !is JsonNull && !flipElement.isString &&
                    (flipElement.content == "true" || flipElement.content == "false"),
            ) { "flip is not a boolean" }
            flipElement.content == "true"
        }
        val at = ExactNumbers.point(need(o, "at"))
        return SolutionPiece(piece, PieceGeometry.corners(piece, Turn(rot), flip, at))
    }

    /** Equal as a cyclic sequence, in either direction. */
    private fun cyclicEqual(a: List<ExactPoint>, b: List<ExactPoint>): Boolean {
        if (a.size != b.size) return false
        val n = a.size
        for (start in 0 until n) {
            if ((0 until n).all { a[(start + it) % n] == b[it] }) return true
            if ((0 until n).all { a[((start - it) % n + n) % n] == b[it] }) return true
        }
        return false
    }

    private fun picture(art: JsonObject): Picture {
        val base = color(str(art, "base"))
        val shapesElement = art["shapes"]
        val shapes = when (shapesElement) {
            null -> JsonArray(emptyList())
            is JsonArray -> shapesElement
            else -> bad("shapes is not an array")
        }
        return Picture(base, shapes.map(::shape))
    }

    private fun shape(e: JsonElement): PictureShape {
        val o = e as? JsonObject ?: bad("shape is not an object")
        val style = PictureStyle(
            fill = o["fill"]?.let { color(primitiveString(it, "fill")) },
            stroke = o["stroke"]?.let { color(primitiveString(it, "stroke")) },
            strokeWidth = o["width"]?.let { num(it).also { w -> require(w > 0) { "width must be positive" } } },
            opacity = o["opacity"]?.let { num(it).also { v -> require(v in 0.0..1.0) { "opacity out of range" } } },
        )
        return when (val type = str(o, "type")) {
            "polygon" -> {
                val pts = (need(o, "points") as? JsonArray ?: bad("points is not an array")).map(::picPoint)
                require(pts.size >= 3) { "a polygon needs 3 points" }
                PictureShape.Polygon(pts, style)
            }
            "rect" -> PictureShape.Rect(
                num(need(o, "x")), num(need(o, "y")), num(need(o, "w")), num(need(o, "h")),
                o["rx"]?.let { num(it) }, style,
            )
            "circle" -> PictureShape.Circle(picPoint(need(o, "c")), num(need(o, "r")), style)
            "ellipse" -> PictureShape.Ellipse(picPoint(need(o, "c")), num(need(o, "rx")), num(need(o, "ry")), style)
            "line" -> {
                require(style.stroke != null) { "a line needs a stroke" }
                PictureShape.Line(picPoint(need(o, "from")), picPoint(need(o, "to")), style)
            }
            // d is carried verbatim, not validated (WO-003 owns drawing it safely)
            "path" -> PictureShape.Path(str(o, "d"), style)
            else -> bad("unknown art type $type")
        }
    }

    private fun picPoint(e: JsonElement): PicturePoint {
        require(e is JsonArray && e.size == 2) { "a picture point has two numbers" }
        return PicturePoint(num(e[0]), num(e[1]))
    }

    /** A picture number: plain Double (contract), never a string, always finite. */
    private fun num(e: JsonElement): Double {
        require(e is JsonPrimitive && e !is JsonNull && !e.isString) { "not a number: $e" }
        val d = e.content.toDouble()
        require(d.isFinite()) { "number is not finite: ${e.content}" }
        return d
    }

    private fun color(text: String): Rgb {
        require(COLOR_PATTERN.matches(text)) { "bad colour $text" }
        return Rgb(text.substring(1).toInt(16))
    }

    private fun intToken(e: JsonElement, name: String): Int {
        require(e is JsonPrimitive && e !is JsonNull && !e.isString && INT_PATTERN.matches(e.content)) { "$name is not an integer" }
        return e.content.toIntOrNull() ?: bad("$name out of range")
    }

    private fun need(o: JsonObject, key: String): JsonElement = o[key] ?: bad("missing $key")

    private fun obj(o: JsonObject, key: String): JsonObject = need(o, key) as? JsonObject ?: bad("$key is not an object")

    private fun str(o: JsonObject, key: String): String = primitiveString(need(o, key), key)

    private fun primitiveString(e: JsonElement, name: String): String {
        require(e is JsonPrimitive && e !is JsonNull && e.isString) { "$name is not a string" }
        return e.content
    }

    private fun bad(message: String): Nothing = throw IllegalArgumentException(message)
}
