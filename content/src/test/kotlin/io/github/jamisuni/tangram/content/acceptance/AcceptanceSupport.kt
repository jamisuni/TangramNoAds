package io.github.jamisuni.tangram.content.acceptance

import io.github.jamisuni.tangram.content.PuzzleFile
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Silhouette
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.lock.LockSearch
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Rational
import java.io.ByteArrayOutputStream
import java.io.File
import java.math.BigDecimal
import java.security.MessageDigest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

// SCAFFOLDING for the WO-002 acceptance tests; carries no acceptance ID.
// Written from the REQs, the frozen contract and the design's "Test seams" section only.
// Assumption (logged): content/build.gradle.kts sets the system property `tangram.root` for tests,
// exactly as kernel/build.gradle.kts does (design sections 1 and 6).

/** Fixtures built from the real files in Tangrams/, read from the source folder (not the packaged copy). */
object Fx {
    val root: File by lazy {
        File(checkNotNull(System.getProperty("tangram.root")) { "system property tangram.root is not set for :content:test" })
    }
    val tangramsDir: File get() = File(root, "Tangrams")
    val goldenFile: File get() = File(root, "tools/golden/geometry.json")

    private fun sourceFiles(): List<File> {
        val files = tangramsDir.listFiles { f -> f.isFile && f.name.endsWith(".json") && !f.name.endsWith(".schema.json") }
        return checkNotNull(files) { "cannot list ${tangramsDir.path}" }.sortedBy { it.name }
    }

    /** Every puzzle file of Tangrams/ (the schema excluded), by name including `.json`. */
    fun shippedFiles(): List<PuzzleFile> = sourceFiles().map { PuzzleFile(it.name, it.readText(Charsets.UTF_8)) }

    fun shippedBytes(): Map<String, ByteArray> = sourceFiles().associate { it.name.removeSuffix(".json") to it.readBytes() }

    fun shipped(stem: String): PuzzleFile = shippedFiles().first { it.name == "$stem.json" }

    /** The raw JSON tree of a shipped file. */
    fun raw(stem: String): JsonObject = Json.parseToJsonElement(shipped(stem).text).jsonObject

    private fun String.replaceFirstRegex(pattern: String, with: String): String {
        val re = Regex(pattern)
        check(re.containsMatchIn(this)) { "fixture pattern $pattern not found" }
        return re.replaceFirst(this, Regex.escapeReplacement(with))
    }

    /**
     * A copy of [base] under a new id (the file name follows the id), optionally with another `difficulty`
     * or `kind`, or with the `kind` field removed. The geometry and picture are the base file's.
     */
    fun variant(
        base: PuzzleFile,
        id: String,
        difficulty: Int? = null,
        kind: String? = null,
        dropKind: Boolean = false,
    ): PuzzleFile {
        var t = base.text.replaceFirstRegex("\"id\"\\s*:\\s*\"[^\"]*\"", "\"id\": \"$id\"")
        if (difficulty != null) t = t.replaceFirstRegex("\"difficulty\"\\s*:\\s*\\d+", "\"difficulty\": $difficulty")
        if (kind != null) t = t.replaceFirstRegex("\"kind\"\\s*:\\s*\"[^\"]*\"", "\"kind\": \"$kind\"")
        if (dropKind) t = t.replaceFirstRegex("\"kind\"\\s*:\\s*\"[^\"]*\"\\s*,", "")
        return PuzzleFile("$id.json", t)
    }

    /** [base] under a new id with `"reviewedByHuman": false` replaced by [literal] (raw JSON text), or the flag removed when null. */
    fun withReviewed(base: PuzzleFile, id: String, literal: String?): PuzzleFile {
        val t0 = variant(base, id).text
        val t = if (literal == null) {
            t0.replaceFirstRegex(",\\s*\"reviewedByHuman\"\\s*:\\s*false", "")
        } else {
            t0.replaceFirstRegex("\"reviewedByHuman\"\\s*:\\s*false", "\"reviewedByHuman\": $literal")
        }
        return PuzzleFile("$id.json", t)
    }
}

class GVerdict(val pass: Boolean, val errors: List<String>, val exposure: Map<String, Double>)

class GPose(val turn: Int, val mirrored: Boolean, val at: ExactPoint)

class GPuzzle(
    val id: String,
    val sha256: String,
    val kind: String,
    val difficulty: Int?,
    val verdict: GVerdict?,
    val buildOrder: List<PieceId>,
    val polygons: Map<PieceId, List<ExactPoint>>,
    val poses: Map<PieceId, GPose>,
)

/** tools/golden/geometry.json, read the way kernel's Golden.kt reads it (R "n"/"n/d", Q [R,R], P [Q,Q]). */
object AcceptanceGolden {
    const val REFRESH_HINT = "re-run tools/export_geometry_golden.py"

    val puzzles: Map<String, GPuzzle> by lazy {
        check(Fx.goldenFile.isFile) { "missing ${Fx.goldenFile.path}: $REFRESH_HINT" }
        val top = Json.parseToJsonElement(Fx.goldenFile.readText(Charsets.UTF_8)).jsonObject
        top.getValue("puzzles").jsonObject.entries.associate { (id, v) -> id to puzzle(id, v.jsonObject) }
    }

    fun rational(text: String): Rational {
        val slash = text.indexOf('/')
        return if (slash < 0) Rational.of(text.toLong()) else Rational.of(text.substring(0, slash).toLong(), text.substring(slash + 1).toLong())
    }

    private fun quad(e: JsonElement): Q2 {
        val p = e.jsonArray
        return Q2(rational(p[0].jsonPrimitive.content), rational(p[1].jsonPrimitive.content))
    }

    private fun point(e: JsonElement): ExactPoint {
        val p = e.jsonArray
        return ExactPoint(quad(p[0]), quad(p[1]))
    }

    private fun puzzle(id: String, o: JsonObject): GPuzzle {
        val verdict = o["validator"]?.jsonObject?.let { v ->
            GVerdict(
                pass = v.getValue("pass").jsonPrimitive.boolean,
                errors = v.getValue("errors").jsonArray.map { it.jsonPrimitive.content },
                exposure = v["exposure"]?.jsonObject?.entries?.associate { (k, x) -> k to x.jsonPrimitive.double } ?: emptyMap(),
            )
        }
        val sol = o.getValue("solution").jsonArray.map { it.jsonObject }
        return GPuzzle(
            id = id,
            sha256 = o.getValue("sha256").jsonPrimitive.content,
            kind = o.getValue("kind").jsonPrimitive.content,
            difficulty = o["difficulty"]?.jsonPrimitive?.int,
            verdict = verdict,
            buildOrder = o.getValue("buildOrder").jsonArray.map { PieceId.valueOf(it.jsonPrimitive.content) },
            polygons = sol.associate { PieceId.valueOf(it.getValue("piece").jsonPrimitive.content) to it.getValue("polygon").jsonArray.map(::point) },
            poses = sol.associate {
                val p = it.getValue("pose").jsonObject
                PieceId.valueOf(it.getValue("piece").jsonPrimitive.content) to
                    GPose(p.getValue("turn").jsonPrimitive.int, p.getValue("mirrored").jsonPrimitive.boolean, point(p.getValue("at")))
            },
        )
    }

    fun sha256(bytes: ByteArray): String {
        val out = ByteArrayOutputStream(bytes.size)
        var i = 0
        while (i < bytes.size) {
            val crlf = bytes[i] == 0x0D.toByte() && i + 1 < bytes.size && bytes[i + 1] == 0x0A.toByte()
            if (!crlf) out.write(bytes[i].toInt())
            i++
        }
        return MessageDigest.getInstance("SHA-256").digest(out.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    /** Hash and file-set differences between Tangrams/ and the golden; empty means fresh. */
    fun freshnessProblems(files: Map<String, ByteArray>, golden: Map<String, GPuzzle>): List<String> {
        val p = mutableListOf<String>()
        for (id in files.keys.sorted()) {
            val g = golden[id]
            if (g == null) {
                p += "Tangrams/$id.json is not in the golden"
            } else if (g.sha256 != sha256(files.getValue(id))) {
                p += "Tangrams/$id.json changed since the golden: $REFRESH_HINT"
            }
        }
        for (id in golden.keys.sorted()) if (id !in files) p += "the golden lists $id but Tangrams/$id.json is missing"
        return p
    }

    /** What is wrong with a golden entry's validator verdict (G1-baseline validator, decision F15); empty means it passes. */
    fun verdictProblems(g: GPuzzle): List<String> {
        val v = g.verdict ?: return listOf("${g.id}: the golden has no validator verdict: $REFRESH_HINT")
        val p = mutableListOf<String>()
        if (!v.pass) p += "${g.id}: validator did not pass"
        v.errors.forEach { p += "${g.id}: validator error $it" }
        return p
    }

    /** A decimal token for an exact rational, never through a Double. */
    fun decimal(r: Rational): String = BigDecimal(r.numerator).divide(BigDecimal(r.denominator)).toPlainString()
}

/**
 * Locks [order] piece by piece on the silhouette of [puzzle]'s stored solution through the kernel
 * (PieceGeometry.poseOf, LockSearch.find), the engine reading of "the solution builds" (decision F15).
 * Each lock must land exactly on the stored position. Returns the placed pieces.
 */
fun lockInOrder(puzzle: Puzzle, order: List<PieceId>): List<PlacedPiece> {
    val silhouette = Silhouette(puzzle.solution.map { it.polygon })
    val placed = mutableListOf<PlacedPiece>()
    for (piece in order) {
        val entry = puzzle.solution.firstOrNull { it.piece == piece }
        assertNotNull("${puzzle.id.value}: build order names $piece but the solution has no such piece", entry)
        val pose = PieceGeometry.poseOf(piece, entry!!.polygon)
        assertNotNull("${puzzle.id.value}: $piece polygon is no pose of that piece", pose)
        val origin = Vec2(pose!!.at.x.toDouble(), pose.at.y.toDouble())
        val lock = LockSearch.find(silhouette, placed, piece, pose.turn, pose.mirrored, origin, LockSearch.BASE_DISTANCE)
        assertNotNull("${puzzle.id.value}: $piece does not lock at its stored place (build order $order)", lock)
        assertEquals("${puzzle.id.value}: $piece locks elsewhere than its stored place", pose.at, lock!!.at)
        placed += pose
    }
    return placed
}
