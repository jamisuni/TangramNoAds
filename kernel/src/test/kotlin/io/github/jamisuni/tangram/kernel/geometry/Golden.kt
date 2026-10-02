package io.github.jamisuni.tangram.kernel.geometry

import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PieceShape
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Rational
import io.github.jamisuni.tangram.kernel.model.Turn
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// SCAFFOLDING (TASK-003b, WO-001 design §8): the Kotlin side of the G-03 golden route. A test helper,
// not a product file, and carrying no acceptance IDs. It reads tools/golden/geometry.json (written by
// tools/export_geometry_golden.py from the Python reference) into plain Kotlin values; the tests of
// TASK-003b, TASK-004 (outline corners) and TASK-005b (build order, round trip) share it.
//
// Exact numbers in the file are strings: R = "n" or "n/d"; Q = [R, R] is a + b·√2; P = [Q, Q] is a point.
// They are parsed straight into Rational / Q2 / ExactPoint, never through Double.

/** The five shapes of the golden's `shapes` table, in the file's own key order. */
class GoldenShape(val shape: PieceShape, val local: List<ExactPoint>, val area: Rational)

/** One of the 80 `transforms` rows: the corners of [shape] at ([turn], [mirrored]) with `at` = (0, 0). */
class GoldenTransform(val shape: PieceShape, val turn: Int, val mirrored: Boolean, val corners: List<ExactPoint>)

/** The pose `placement_from_polygon` answered for a solution polygon. */
class GoldenPose(val turn: Turn, val mirrored: Boolean, val at: ExactPoint)

/** One solution piece of a puzzle file: its polygon (as stored) and the reference pose. */
class GoldenPiece(val piece: PieceId, val polygon: List<ExactPoint>, val pose: GoldenPose)

/**
 * One puzzle file's reference answers. [outlineCorners] is in the reference's first-appearance order
 * (compare as a set); [buildOrder] is `build_order`'s order; [area] is the exact sum of polygon areas.
 */
class GoldenPuzzle(
    val id: String,
    val sha256: String,
    val kind: String,
    val solution: List<GoldenPiece>,
    val outlineCorners: List<ExactPoint>,
    val area: Rational,
    val buildOrder: List<PieceId>,
)

class GoldenData(
    val format: String,
    val shapes: Map<PieceShape, GoldenShape>,
    val transforms: List<GoldenTransform>,
    val puzzles: Map<String, GoldenPuzzle>,
)

object Golden {

    const val FORMAT = "tangram-golden/1"

    /** The failure message the freshness test starts with. */
    const val REFRESH_HINT = "re-run tools/export_geometry_golden.py"

    /** The repository root, from the `tangram.root` system property that `kernel/build.gradle.kts` sets for tests. */
    val root: File by lazy {
        val path = System.getProperty("tangram.root")
        checkNotNull(path) { "system property tangram.root is not set (kernel/build.gradle.kts sets it for :kernel:test)" }
        File(path)
    }

    val goldenFile: File get() = File(root, "tools/golden/geometry.json")

    val tangramsDir: File get() = File(root, "Tangrams")

    /** The golden, parsed once. */
    val data: GoldenData by lazy {
        check(goldenFile.isFile) { "missing ${goldenFile.path}: $REFRESH_HINT" }
        parse(goldenFile.readText(Charsets.UTF_8))
    }

    // ---- exact numbers from strings ------------------------------------------------------------

    /** `"n"` or `"n/d"` (n may be negative) to an exact [Rational]. */
    fun rational(text: String): Rational {
        val slash = text.indexOf('/')
        return if (slash < 0) {
            Rational.of(text.toLong())
        } else {
            Rational.of(text.substring(0, slash).toLong(), text.substring(slash + 1).toLong())
        }
    }

    /** `[R, R]` to a + b·√2. */
    fun quad(element: JsonElement): Q2 {
        val pair = element.jsonArray
        check(pair.size == 2) { "a Q is [a, b], got $element" }
        return Q2(rational(pair[0].jsonPrimitive.content), rational(pair[1].jsonPrimitive.content))
    }

    /** `[Q, Q]` to an exact point. */
    fun point(element: JsonElement): ExactPoint {
        val pair = element.jsonArray
        check(pair.size == 2) { "a P is [Q, Q], got $element" }
        return ExactPoint(quad(pair[0]), quad(pair[1]))
    }

    fun points(element: JsonElement): List<ExactPoint> = element.jsonArray.map { point(it) }

    // ---- ids -------------------------------------------------------------------------------------

    /** The golden's shape keys: LT, MT, ST, SQ, PG. */
    fun shapeOf(key: String): PieceShape = when (key) {
        "LT" -> PieceShape.LARGE_TRIANGLE
        "MT" -> PieceShape.MEDIUM_TRIANGLE
        "ST" -> PieceShape.SMALL_TRIANGLE
        "SQ" -> PieceShape.SQUARE
        "PG" -> PieceShape.PARALLELOGRAM
        else -> error("unknown shape key $key in the golden")
    }

    /** The puzzle files' piece ids (LT1, LT2, MT, SQ, PG, ST1, ST2) are the names of [PieceId]. */
    fun pieceOf(id: String): PieceId = PieceId.valueOf(id)

    // ---- the file --------------------------------------------------------------------------------

    fun parse(text: String): GoldenData {
        val top = Json.parseToJsonElement(text).jsonObject
        val format = top.getValue("format").jsonPrimitive.content
        check(format == FORMAT) { "unknown golden format $format (expected $FORMAT)" }

        val shapes = top.getValue("shapes").jsonObject.entries.associate { (key, value) ->
            val shape = shapeOf(key)
            val obj = value.jsonObject
            shape to GoldenShape(shape, points(obj.getValue("local")), rational(obj.getValue("area").jsonPrimitive.content))
        }

        val transforms = top.getValue("transforms").jsonArray.map { row ->
            val obj = row.jsonObject
            GoldenTransform(
                shape = shapeOf(obj.getValue("shape").jsonPrimitive.content),
                turn = obj.getValue("turn").jsonPrimitive.int,
                mirrored = obj.getValue("mirrored").jsonPrimitive.boolean,
                corners = points(obj.getValue("corners")),
            )
        }

        val puzzles = top.getValue("puzzles").jsonObject.entries.associate { (id, value) ->
            val obj = value.jsonObject
            id to GoldenPuzzle(
                id = id,
                sha256 = obj.getValue("sha256").jsonPrimitive.content,
                kind = obj.getValue("kind").jsonPrimitive.content,
                solution = obj.getValue("solution").jsonArray.map { entry ->
                    val e = entry.jsonObject
                    val pose = e.getValue("pose").jsonObject
                    GoldenPiece(
                        piece = pieceOf(e.getValue("piece").jsonPrimitive.content),
                        polygon = points(e.getValue("polygon")),
                        pose = GoldenPose(
                            turn = Turn(pose.getValue("turn").jsonPrimitive.int),
                            mirrored = pose.getValue("mirrored").jsonPrimitive.boolean,
                            at = point(pose.getValue("at")),
                        ),
                    )
                },
                outlineCorners = points(obj.getValue("outlineCorners")),
                area = rational(obj.getValue("area").jsonPrimitive.content),
                buildOrder = obj.getValue("buildOrder").jsonArray.map { pieceOf(it.jsonPrimitive.content) },
            )
        }
        return GoldenData(format, shapes, transforms, puzzles)
    }

    // ---- freshness (design §8) -------------------------------------------------------------------

    /** The puzzle files as the exporter sees them: every .json in the Tangrams folder except the .schema.json one, keyed by file stem. */
    fun puzzleFileBytes(): Map<String, ByteArray> {
        val files = tangramsDir.listFiles { f -> f.isFile && f.name.endsWith(".json") && !f.name.endsWith(".schema.json") }
        checkNotNull(files) { "cannot list ${tangramsDir.path}" }
        return files.sortedBy { it.name }.associate { it.name.removeSuffix(".json") to it.readBytes() }
    }

    /** `bytes.replace(b"\r\n", b"\n")`, as the exporter does before hashing. A lone CR stays. */
    fun normalizeCrlf(bytes: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(bytes.size)
        var i = 0
        while (i < bytes.size) {
            val isCrLf = bytes[i] == CR && i + 1 < bytes.size && bytes[i + 1] == LF
            if (!isCrLf) out.write(bytes[i].toInt())
            i++
        }
        return out.toByteArray()
    }

    /** Lower-case hex SHA-256 of the CRLF-normalized bytes. */
    fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(normalizeCrlf(bytes)).joinToString("") { "%02x".format(it) }

    /**
     * What differs between the puzzle files ([files], keyed by stem) and the golden: a file the golden lacks,
     * a golden puzzle without a file, a hash that does not match. Empty means the golden is fresh.
     */
    fun freshnessProblems(files: Map<String, ByteArray>, golden: GoldenData): List<String> {
        val problems = mutableListOf<String>()
        for (id in files.keys.sorted()) {
            val entry = golden.puzzles[id]
            if (entry == null) {
                problems += "Tangrams/$id.json is not in the golden"
            } else if (entry.sha256 != sha256(files.getValue(id))) {
                problems += "Tangrams/$id.json changed (sha256 differs from the golden)"
            }
        }
        for (id in golden.puzzles.keys.sorted()) {
            if (id !in files) problems += "the golden lists $id but Tangrams/$id.json is missing"
        }
        return problems
    }

    // ---- exact area ------------------------------------------------------------------------------

    /** The exact shoelace area of a polygon in either orientation. The √2 part must cancel (it is a rational area). */
    fun shoelaceArea(polygon: List<ExactPoint>): Rational {
        var twice = Q2.ZERO
        for (i in polygon.indices) {
            val p = polygon[i]
            val q = polygon[(i + 1) % polygon.size]
            twice += p.x * q.y - q.x * p.y
        }
        check(twice.b == Rational.ZERO) { "area of $polygon has a √2 part: $twice" }
        val half = twice.a * Rational.of(1, 2)
        return if (half.signum() < 0) -half else half
    }

    private const val CR: Byte = 0x0D
    private const val LF: Byte = 0x0A
}
