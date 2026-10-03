package io.github.jamisuni.tangram.store

import io.github.jamisuni.tangram.contracts.progress.GameSettings
import io.github.jamisuni.tangram.contracts.progress.IProgressStore
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PlayTime
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Rational
import io.github.jamisuni.tangram.kernel.model.Turn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.LocalDate

/**
 * The first [IProgressStore] (WO-004 design section 2, ADR-005, DA-46/47/63/64/65): one compact UTF-8 JSON
 * document `progress.json` in [dir] (app-private, G-01), read once by the constructor, so the object is the
 * in-memory state. Every save updates memory first, then writes temp file + atomic rename.
 *
 * Threading: main thread only, synchronous, no lock (the document is a few KB).
 * A save is skipped when the value equals the held one (DA-65), so a first run creates no file until a save
 * that differs from the fresh state (DA-68). A disk-level write failure never throws (G-10): memory stays
 * current and the next save retries the whole document (DA-47).
 *
 * Reading never throws (G-10). Damage handling follows the design section 2 reader table (G-09): a bad
 * document or a newer version is moved aside whole; a damaged section reads fresh for that section only and a
 * damaged entry reads New, with the original copied once to `progress-partial-*`.
 */
class JsonProgressStore internal constructor(
    private val dir: File,
    private val nowMs: () -> Long,
    private val io: StoreIo,
) : IProgressStore {

    constructor(dir: File, nowMs: () -> Long = System::currentTimeMillis) : this(dir, nowMs, RealStoreIo)

    /** Where the unreadable or newer document went (moved or copied); null when not applicable. */
    internal var movedAside: File? = null
        private set

    /** The one-per-run copy of a document with a damaged section or entry; null when not applicable. */
    internal var partialCopy: File? = null
        private set

    /** Both the move-aside and the copy failed: later saves overwrite (DA-64, see [asideOrCopy]). */
    internal var asideFailed: Boolean = false
        private set

    /** Count of disk-level write failures (the memory stayed current; the next save retries). */
    internal var failedWrites: Int = 0
        private set

    private val file = File(dir, FILE_NAME)

    private var lastShown: String? = null
    private val puzzles = LinkedHashMap<String, PuzzleProgress>()
    private var playTime: PlayTime = PlayTime.NONE
    private var settings: GameSettings = GameSettings()

    /** True when memory holds something the disk does not (a failed write), so skip-equal must not skip. */
    private var dirty = false

    init {
        runCatching { dir.mkdirs() }
        runCatching { load() }.onFailure { // G-10: a read never throws; anything unforeseen reads fresh
            lastShown = null
            puzzles.clear()
            playTime = PlayTime.NONE
            settings = GameSettings()
        }
    }

    // ------------------------------------------------------------------ IProgressStore

    override fun progress(puzzle: PuzzleId): PuzzleProgress = puzzles[puzzle.value] ?: PuzzleProgress.NEW

    override fun saveProgress(puzzle: PuzzleId, progress: PuzzleProgress) {
        if (!dirty && progress(puzzle) == progress) return
        puzzles[puzzle.value] = progress
        write()
    }

    override fun playTime(): PlayTime = playTime

    override fun savePlayTime(playTime: PlayTime) {
        if (!dirty && this.playTime == playTime) return
        this.playTime = playTime
        write()
    }

    override fun settings(): GameSettings = settings

    override fun saveSettings(settings: GameSettings) {
        if (!dirty && this.settings == settings) return
        this.settings = settings
        write()
    }

    override fun lastShownPuzzle(): PuzzleId? = lastShown?.let(::PuzzleId)

    override fun saveLastShownPuzzle(puzzle: PuzzleId) {
        if (!dirty && lastShown == puzzle.value) return
        lastShown = puzzle.value
        write()
    }

    override fun resetAllProgress() {
        if (!dirty && puzzles.isEmpty() && playTime == PlayTime.NONE) return
        puzzles.clear()
        playTime = PlayTime.NONE
        write() // settings and lastShown are kept (KDoc; design section 2 Semantics)
    }

    /**
     * Forces the document and then its directory to disk (DA-47, O2), so a power cut after this call keeps the
     * last save. Called from `MainActivity.onStop`. Never throws; a file system that cannot force is ignored.
     */
    fun sync() {
        if (dirty) write() // F4: onStop is the last chance to retry a failed write
        runCatching { io.force(file) }
        runCatching { io.force(dir) }
    }

    // ------------------------------------------------------------------ writer

    private fun write() {
        val bytes = runCatching { encode().toString().toByteArray(Charsets.UTF_8) }.getOrNull() ?: run {
            failedWrites++
            dirty = true
            return
        }
        try {
            io.writeAtomic(file, bytes)
            dirty = false
        } catch (e: Throwable) {
            failedWrites++
            dirty = true
            log("write failed: $e")
        }
    }

    private fun encode(): JsonObject {
        val puzzleObj = LinkedHashMap<String, JsonElement>()
        for ((id, p) in puzzles) puzzleObj[id] = encodeEntry(p)
        return JsonObject(
            linkedMapOf(
                "version" to JsonPrimitive(VERSION),
                "lastShown" to (lastShown?.let { JsonPrimitive(it) } ?: JsonNull),
                "puzzles" to JsonObject(puzzleObj),
                "playTime" to JsonObject(
                    linkedMapOf(
                        "day" to (playTime.day?.let { JsonPrimitive(it.toString()) } ?: JsonNull),
                        "todaySeconds" to JsonPrimitive(playTime.todaySeconds),
                        "totalSeconds" to JsonPrimitive(playTime.totalSeconds),
                    ),
                ),
                "settings" to JsonObject(
                    linkedMapOf(
                        "timerShown" to JsonPrimitive(settings.timerShown),
                        "soundOn" to JsonPrimitive(settings.soundOn),
                    ),
                ),
            ),
        )
    }

    private fun encodeEntry(p: PuzzleProgress): JsonObject {
        val pieces = LinkedHashMap<String, JsonElement>()
        for ((id, save) in p.pieces) pieces[pieceName(id)] = encodePiece(save)
        return JsonObject(
            linkedMapOf(
                "state" to JsonPrimitive(stateName(p.state)),
                "pieces" to JsonObject(pieces),
                "puzzleSeconds" to JsonPrimitive(p.puzzleSeconds),
                "bestSeconds" to (p.bestSeconds?.let { JsonPrimitive(it) } ?: JsonNull),
            ),
        )
    }

    private fun encodePiece(s: PieceSave): JsonObject {
        val m = LinkedHashMap<String, JsonElement>()
        m["where"] = JsonPrimitive(if (s is PieceSave.OnBoard) "board" else "tray")
        m["turn"] = JsonPrimitive(s.turn.steps)
        m["mirrored"] = JsonPrimitive(s.mirrored)
        if (s is PieceSave.OnBoard) {
            m["at"] = JsonObject(linkedMapOf("x" to encodeQ2(s.at.x), "y" to encodeQ2(s.at.y)))
        }
        return JsonObject(m)
    }

    private fun encodeQ2(q: Q2): JsonObject =
        JsonObject(linkedMapOf("a" to JsonPrimitive(formatRational(q.a)), "b" to JsonPrimitive(formatRational(q.b))))

    // F2 / G-09: serial names are independent of Kotlin identifiers and of Rational.toString().
    private fun pieceName(id: PieceId): String = when (id) {
        PieceId.LT1 -> "LT1"
        PieceId.LT2 -> "LT2"
        PieceId.MT -> "MT"
        PieceId.SQ -> "SQ"
        PieceId.PG -> "PG"
        PieceId.ST1 -> "ST1"
        PieceId.ST2 -> "ST2"
    }

    private fun pieceIdOf(name: String): PieceId? = PieceId.values().firstOrNull { pieceName(it) == name }

    private fun formatRational(r: Rational): String =
        if (r.denominator == 1L) "${r.numerator}" else "${r.numerator}/${r.denominator}"

    private fun stateName(s: PuzzleState): String = when (s) {
        PuzzleState.NEW -> "new"
        PuzzleState.IN_PROGRESS -> "in_progress"
        PuzzleState.SOLVED -> "solved"
    }

    // ------------------------------------------------------------------ reader

    /** Thrown inside the reader for any damage; always caught at section or entry level. */
    private class Damaged(message: String) : Exception(message)

    private fun load() {
        if (!file.exists()) return // fresh install
        val root: JsonObject
        val version: Long
        try {
            val text = String(io.readBytes(file), Charsets.UTF_8)
            root = Json.parseToJsonElement(text) as? JsonObject ?: throw Damaged("not an object")
            version = (root["version"] as? JsonPrimitive)?.takeIf { !it.isString }?.longOrNull
                ?: throw Damaged("no integer version")
        } catch (e: Throwable) {
            asideOrCopy("progress-bad-${nowMs()}.json", move = true)
            return
        }
        if (version < 1) {
            asideOrCopy("progress-bad-${nowMs()}.json", move = true)
            return
        }
        if (version > VERSION) {
            asideOrCopy("progress-v$version-${nowMs()}.json", move = true)
            return
        }

        var partial = false

        lastShown = when (val e = root["lastShown"]) {
            JsonNull -> null
            is JsonPrimitive -> if (e.isString) e.content else { partial = true; null }
            else -> { partial = true; null } // missing or wrong type: that section reads fresh
        }

        val pz = root["puzzles"] as? JsonObject
        if (pz == null) partial = true
        else for ((id, entry) in pz) {
            try {
                puzzles[id] = decodeEntry(entry)
            } catch (e: Throwable) {
                partial = true // a damaged entry reads New (it is simply not held)
            }
        }

        try {
            playTime = decodePlayTime(root["playTime"])
        } catch (e: Throwable) {
            partial = true
            playTime = PlayTime.NONE
        }
        try {
            settings = decodeSettings(root["settings"])
        } catch (e: Throwable) {
            partial = true
            settings = GameSettings()
        }

        if (partial) asideOrCopy("progress-partial-${nowMs()}.json", move = false)
    }

    /**
     * Gets the unreadable, newer or partly damaged document out of harm's way (G-09: never lose it silently).
     * [move] true: try a move, then a copy. [move] false: copy only (the document stays as the live file).
     *
     * GUARDRAIL CONFLICT, DA-64 (G-09 "never overwritten" vs the `IProgressStore` promise "stored before it
     * returns"; governance row 13 ai+inform): when the move and the copy both fail we set [asideFailed] and
     * later saves OVERWRITE the document, because a store that silently stops saving would lose the player's
     * new work. Logged here; the orchestrator surfaces it at checkpoint 4.
     */
    private fun asideOrCopy(name: String, move: Boolean) {
        val target = File(dir, name)
        var done = false
        if (move) {
            done = runCatching { io.move(file, target) }.isSuccess
        }
        if (!done) {
            done = runCatching { io.copy(file, target) }.isSuccess
        }
        if (done) {
            if (move) movedAside = target else partialCopy = target
            prune()
        } else {
            asideFailed = true
            log("could not move aside or copy $FILE_NAME to $name; later saves overwrite it (DA-64)")
        }
    }

    /**
     * Keeps only the newest 3 of `progress-bad-*` and `progress-partial-*` (N9). `progress-v<N>-*` files are
     * never pruned: they are the only copy of newer-version data (CR-2 F3).
     */
    private fun prune() {
        runCatching {
            val files = dir.listFiles { f ->
                f.isFile && (f.name.startsWith("progress-bad-") || f.name.startsWith("progress-partial-"))
            } ?: return
            val ordered = files.sortedWith(
                compareByDescending<File> { stamp(it) }.thenByDescending { it.name },
            )
            ordered.drop(KEEP_ASIDE).forEach { runCatching { it.delete() } }
        }
    }

    private fun stamp(f: File): Long =
        STAMP.find(f.name)?.groupValues?.get(1)?.toLongOrNull() ?: f.lastModified()

    private fun decodeEntry(e: JsonElement): PuzzleProgress {
        val o = e as? JsonObject ?: throw Damaged("entry")
        val state = when ((o["state"] as? JsonPrimitive)?.takeIf { it.isString }?.content) {
            "new" -> PuzzleState.NEW
            "in_progress" -> PuzzleState.IN_PROGRESS
            "solved" -> PuzzleState.SOLVED
            else -> throw Damaged("state")
        }
        val pieceObj = o["pieces"] as? JsonObject ?: throw Damaged("pieces")
        val pieces = LinkedHashMap<PieceId, PieceSave>()
        for ((k, v) in pieceObj) {
            val id = pieceIdOf(k) ?: throw Damaged("piece id")
            pieces[id] = decodePiece(v)
        }
        val seconds = nonNegative(o["puzzleSeconds"])
        val best = if (o["bestSeconds"] == JsonNull) null else nonNegative(o["bestSeconds"])
        return PuzzleProgress(state, pieces, seconds, best)
    }

    private fun decodePiece(e: JsonElement): PieceSave {
        val o = e as? JsonObject ?: throw Damaged("piece")
        val steps = (o["turn"] as? JsonPrimitive)?.takeIf { !it.isString }?.longOrNull ?: throw Damaged("turn")
        if (steps !in 0..7) throw Damaged("turn range")
        val turn = Turn(steps.toInt())
        val mirrored = (o["mirrored"] as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull
            ?: throw Damaged("mirrored")
        return when ((o["where"] as? JsonPrimitive)?.takeIf { it.isString }?.content) {
            "tray" -> PieceSave.InTray(turn, mirrored)
            "board" -> {
                val at = o["at"] as? JsonObject ?: throw Damaged("at")
                PieceSave.OnBoard(ExactPoint(decodeQ2(at["x"]), decodeQ2(at["y"])), turn, mirrored)
            }
            else -> throw Damaged("where")
        }
    }

    private fun decodeQ2(e: JsonElement?): Q2 {
        val o = e as? JsonObject ?: throw Damaged("q2")
        return Q2(decodeRational(o["a"]), decodeRational(o["b"]))
    }

    /**
     * DA-63 / DA-68: a normalized `"n"` or `"n/d"` with |n| <= 65536 and d in {1,2,4,...,64}. `"n/1"`, `"2/4"`,
     * `"-0"`, leading zeros and anything else not equal to the canonical form is damage.
     */
    private fun decodeRational(e: JsonElement?): Rational {
        val s = (e as? JsonPrimitive)?.takeIf { it.isString }?.content ?: throw Damaged("rational")
        if (!RATIONAL.matches(s)) throw Damaged("rational syntax")
        val parts = s.split('/')
        val n = parts[0].toLongOrNull() ?: throw Damaged("rational overflow")
        val d = if (parts.size == 2) parts[1].toLongOrNull() ?: throw Damaged("rational overflow") else 1L
        if (n < -MAX_NUMERATOR || n > MAX_NUMERATOR) throw Damaged("rational bound")
        if (d !in ALLOWED_DENOMINATORS) throw Damaged("rational denominator")
        val r = Rational.of(n, d)
        if (formatRational(r) != s) throw Damaged("rational not normalized")
        return r
    }

    private fun nonNegative(e: JsonElement?): Long {
        val v = (e as? JsonPrimitive)?.takeIf { !it.isString }?.longOrNull ?: throw Damaged("seconds")
        if (v < 0) throw Damaged("negative seconds")
        return v
    }

    private fun decodePlayTime(e: JsonElement?): PlayTime {
        val o = e as? JsonObject ?: throw Damaged("playTime")
        val day = when (val d = o["day"]) {
            JsonNull -> null
            is JsonPrimitive -> if (d.isString) LocalDate.parse(d.content) else throw Damaged("day")
            else -> throw Damaged("day")
        }
        return PlayTime(day, nonNegative(o["todaySeconds"]), nonNegative(o["totalSeconds"]))
    }

    private fun decodeSettings(e: JsonElement?): GameSettings {
        val o = e as? JsonObject ?: throw Damaged("settings")
        fun flag(k: String) = (o[k] as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull ?: throw Damaged(k)
        return GameSettings(timerShown = flag("timerShown"), soundOn = flag("soundOn"))
    }

    private fun log(message: String) {
        System.err.println("JsonProgressStore: $message")
    }

    companion object {
        const val FILE_NAME: String = "progress.json"
        const val VERSION: Int = 1

        private const val KEEP_ASIDE = 3
        private const val MAX_NUMERATOR = 65536L
        private val ALLOWED_DENOMINATORS = setOf(1L, 2L, 4L, 8L, 16L, 32L, 64L)
        private val RATIONAL = Regex("-?\\d+(/\\d+)?")
        private val STAMP = Regex("-(\\d+)\\.json$")
    }
}

/** The file operations of the store, one seam so tests can make them fail (scaffolding seam, DA-64). */
internal interface StoreIo {
    fun readBytes(f: File): ByteArray
    fun move(src: File, dst: File)
    fun copy(src: File, dst: File)
    fun writeAtomic(target: File, bytes: ByteArray)
    fun force(f: File)
}

internal object RealStoreIo : StoreIo {
    override fun readBytes(f: File): ByteArray = f.readBytes()

    override fun move(src: File, dst: File) {
        Files.move(src.toPath(), dst.toPath()) // no REPLACE_EXISTING: never clobber an earlier aside file
    }

    override fun copy(src: File, dst: File) {
        Files.copy(src.toPath(), dst.toPath())
    }

    override fun writeAtomic(target: File, bytes: ByteArray) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        try {
            tmp.writeBytes(bytes)
            // No non-atomic fallback (CR-2 F1): a refused atomic move is a counted write failure.
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } catch (e: Throwable) {
            runCatching { tmp.delete() }
            throw e
        }
    }

    override fun force(f: File) {
        if (!f.exists()) return
        val opts = if (f.isDirectory) arrayOf(StandardOpenOption.READ) else arrayOf(StandardOpenOption.WRITE)
        FileChannel.open(f.toPath(), *opts).use { it.force(true) }
    }
}
