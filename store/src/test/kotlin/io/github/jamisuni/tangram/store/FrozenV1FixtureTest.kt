package io.github.jamisuni.tangram.store

import io.github.jamisuni.tangram.contracts.progress.GameSettings
import io.github.jamisuni.tangram.contracts.progress.IProgressStore
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PlayTime
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.layout.TrayRules
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PieceShape
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Rational
import io.github.jamisuni.tangram.kernel.model.Turn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate

/**
 * The frozen v1 save fixtures: the oracle of the saved-state format.
 *
 * guardrail G-09 (a frozen v1 document must always load identically; every shape change is a migration)
 * decision DA-46 (the v1 document of design WO-004 section 2)
 *
 * Authored from design WO-004 section 2 and the contract types only (TASK-T4a, DA-67), never from the
 * store implementation. These tests carry no acceptance token on purpose: they pin a guardrail and a
 * decision, not an acceptance criterion.
 *
 * Assumptions about the seam (design "Test seams"): `JsonProgressStore(dir)`, `FILE_NAME = "progress.json"`,
 * `VERSION = 1`, and the `internal` members `movedAside`, `partialCopy`, `asideFailed`, called directly
 * (same module, AGENTS.md seams lesson).
 */
class FrozenV1FixtureTest {

    @get:Rule
    val tmp = TemporaryFolder()

    // ---------------------------------------------------------------- fixtures

    private fun fixtureBytes(name: String): ByteArray {
        val stream = FrozenV1FixtureTest::class.java.getResourceAsStream("/fixtures/$name")
            ?: error("frozen fixture /fixtures/$name is missing from the test resources")
        return stream.use { it.readBytes() }
    }

    /** CRLF normalized to LF (design section 2, guard 3). */
    private fun normalized(bytes: ByteArray): ByteArray =
        String(bytes, Charsets.UTF_8).replace("\r\n", "\n").toByteArray(Charsets.UTF_8)

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun tree(text: String): JsonElement = Json.parseToJsonElement(text)

    /** A store over a fresh temp directory holding [name] as `progress.json`. */
    private fun storeOver(name: String): Pair<JsonProgressStore, File> {
        val dir = tmp.newFolder()
        val file = File(dir, JsonProgressStore.FILE_NAME)
        file.writeBytes(fixtureBytes(name))
        return JsonProgressStore(dir) to dir
    }

    // ---------------------------------------------------------------- 1. SHA-256 pins

    @Test
    fun v1FixtureIsByteForByteTheFrozenOne() {
        // G-09 / design section 2 guard 3: editing a fixture to make a test pass must fail.
        val actual = sha256(normalized(fixtureBytes("progress-v1.json")))
        assertEquals("progress-v1.json changed; the frozen v1 document is a hard-stop. Actual SHA-256: $actual", V1_SHA256, actual)
    }

    @Test
    fun v1FreshFixtureIsByteForByteTheFrozenOne() {
        val actual = sha256(normalized(fixtureBytes("progress-v1-fresh.json")))
        assertEquals("progress-v1-fresh.json changed; the frozen v1 document is a hard-stop. Actual SHA-256: $actual", V1_FRESH_SHA256, actual)
    }

    @Test
    fun formatConstantsAreTheFrozenOnes() {
        // design section 2 "Class": FILE_NAME = "progress.json", VERSION = 1.
        assertEquals("progress.json", JsonProgressStore.FILE_NAME)
        assertEquals(1, JsonProgressStore.VERSION)
    }

    // ---------------------------------------------------------------- 2. reader

    private fun assertReadsAsFrozenV1(store: IProgressStore) {
        assertEquals(PuzzleId("shapes-mini-2"), store.lastShownPuzzle())

        assertEquals(
            PuzzleProgress(PuzzleState.SOLVED, emptyMap(), puzzleSeconds = 47, bestSeconds = 41),
            store.progress(PuzzleId("shapes-mini-1")),
        )
        assertEquals(
            PuzzleProgress(
                PuzzleState.IN_PROGRESS,
                mapOf(
                    PieceId.MT to PieceSave.OnBoard(MT_AT, Turn(0), false),
                    PieceId.ST1 to PieceSave.InTray(Turn(7), false),
                    PieceId.ST2 to PieceSave.InTray(Turn(4), true),
                ),
                puzzleSeconds = 12,
                bestSeconds = null,
            ),
            store.progress(PuzzleId("shapes-mini-2")),
        )
        assertEquals(
            PuzzleProgress(
                PuzzleState.IN_PROGRESS,
                mapOf(
                    PieceId.PG to PieceSave.OnBoard(PG_AT, Turn(7), true),
                    PieceId.LT1 to PieceSave.InTray(Turn(2), false),
                ),
                puzzleSeconds = 95,
                bestSeconds = 95,
            ),
            store.progress(PuzzleId("animals-cat")),
        )
        assertEquals(
            PuzzleProgress(
                PuzzleState.NEW,
                mapOf(PieceId.SQ to PieceSave.InTray(Turn(3), false)),
                puzzleSeconds = 0,
                bestSeconds = null,
            ),
            store.progress(PuzzleId("things-arrow")),
        )
        // design section 2: a puzzle never saved reads as New.
        assertEquals(PuzzleProgress.NEW, store.progress(PuzzleId("never-saved")))

        assertEquals(PlayTime(LocalDate.of(2026, 10, 3), todaySeconds = 340, totalSeconds = 5120), store.playTime())
        assertEquals(GameSettings(timerShown = true, soundOn = false), store.settings())
    }

    @Test
    fun readerLoadsTheV1FixtureToTheExpectedValues() {
        val (store, dir) = storeOver("progress-v1.json")
        assertReadsAsFrozenV1(store)
        assertUntouchedByReading(store, dir, "progress-v1.json")
    }

    @Test
    fun readerLoadsTheFreshFixtureToAFreshInstall() {
        val (store, dir) = storeOver("progress-v1-fresh.json")
        // design section 2: nulls and the default settings, as a fresh install.
        assertNull(store.lastShownPuzzle())
        assertEquals(PlayTime.NONE, store.playTime())
        assertEquals(GameSettings(), store.settings())
        assertEquals(GameSettings(timerShown = false, soundOn = true), store.settings())
        assertEquals(PuzzleProgress.NEW, store.progress(PuzzleId("shapes-mini-1")))
        assertUntouchedByReading(store, dir, "progress-v1-fresh.json")
    }

    /** A valid v1 document is no damage: nothing moved aside, copied or rewritten (design section 2 reader table). */
    private fun assertUntouchedByReading(store: JsonProgressStore, dir: File, name: String) {
        assertNull("a valid v1 document must not be moved aside", store.movedAside)
        assertNull("a valid v1 document must not need a partial copy", store.partialCopy)
        assertFalse(store.asideFailed)
        assertEquals(listOf(JsonProgressStore.FILE_NAME), dir.list()!!.sorted())
        assertEquals(sha256(normalized(fixtureBytes(name))), sha256(normalized(File(dir, JsonProgressStore.FILE_NAME).readBytes())))
    }

    // ---------------------------------------------------------------- 3. writer

    @Test
    fun writerReproducesTheV1TreeFromTheReadValues() {
        val (reader, _) = storeOver("progress-v1.json")

        // Save everything read into an EMPTY directory: every value differs from the fresh one, so none of
        // the writes is skipped as "equal" (design section 2 "Write").
        val outDir = tmp.newFolder()
        val writer = JsonProgressStore(outDir)
        for (id in listOf("shapes-mini-1", "shapes-mini-2", "animals-cat", "things-arrow")) {
            writer.saveProgress(PuzzleId(id), reader.progress(PuzzleId(id)))
        }
        writer.saveLastShownPuzzle(reader.lastShownPuzzle() ?: error("fixture has a lastShown"))
        writer.savePlayTime(reader.playTime())
        writer.saveSettings(reader.settings())

        val written = File(outDir, JsonProgressStore.FILE_NAME)
        assertEquals("the writer must have produced ${JsonProgressStore.FILE_NAME}", true, written.isFile)
        val writtenTree = tree(written.readText(Charsets.UTF_8))
        val frozenTree = tree(String(fixtureBytes("progress-v1.json"), Charsets.UTF_8))
        assertEquals(frozenTree, writtenTree)

        // design section 2 "Writer rules": every section always written, keys in the order above.
        assertEquals(
            listOf("version", "lastShown", "puzzles", "playTime", "settings"),
            (writtenTree as JsonObject).keys.toList(),
        )
    }

    @Test
    fun writerReproducesTheFreshTreeWithExplicitNulls() {
        val (reader, _) = storeOver("progress-v1-fresh.json")

        // A fresh model equals what an empty directory holds, so a plain save would be skipped. Force one
        // write by saving a different value first, then the read (default) value back.
        val outDir = tmp.newFolder()
        val writer = JsonProgressStore(outDir)
        writer.saveSettings(GameSettings(timerShown = true, soundOn = false))
        writer.saveSettings(reader.settings())
        writer.savePlayTime(reader.playTime())

        val written = File(outDir, JsonProgressStore.FILE_NAME)
        assertEquals("the writer must have produced ${JsonProgressStore.FILE_NAME}", true, written.isFile)
        val writtenTree = tree(written.readText(Charsets.UTF_8))
        val frozenTree = tree(String(fixtureBytes("progress-v1-fresh.json"), Charsets.UTF_8))
        // The JSON trees differ if a null is omitted instead of written explicitly (design section 2: nulls explicit).
        assertEquals(frozenTree, writtenTree)
        assertEquals(
            listOf("version", "lastShown", "puzzles", "playTime", "settings"),
            (writtenTree as JsonObject).keys.toList(),
        )
    }

    // ---------------------------------------------------------------- 4. world corners (the meaning of `at`)

    /**
     * Design section 2 / `SavedGame.OnBoard`: world vertex = R(turn * 45 deg) * F(mirror) * local vertex + at,
     * F mirrors x, y down, a positive turn clockwise on screen; local shapes of the kernel table:
     * MT (0,0) (2,0) (0,2); PG (0,0) (2,0) (3,-1) (1,-1).
     *
     * MT, turn 0, unmirrored, at (2, -1/2 + 1/2 s2): no rotation, no mirror, so corners = local + at:
     *   (2, -1/2 + 1/2 s2), (4, -1/2 + 1/2 s2), (2, 3/2 + 1/2 s2).
     *
     * PG, turn 7 (cos = 1/2 s2, sin = -1/2 s2), mirrored, at (3 - s2, 1):
     *   mirrored local: (0,0) (-2,0) (-3,-1) (-1,-1); x' = 1/2 s2 (x + y), y' = 1/2 s2 (y - x):
     *   (0,0) (-s2, s2) (-2 s2, s2) (-s2, 0); plus at:
     *   (3 - s2, 1), (3 - 2 s2, 1 + s2), (3 - 3 s2, 1 + s2), (3 - 2 s2, 1).
     */
    private val expectedMtCorners = listOf(
        point(q(2), q(-1, 2, 1, 2)),
        point(q(4), q(-1, 2, 1, 2)),
        point(q(2), q(3, 2, 1, 2)),
    )
    private val expectedPgCorners = listOf(
        point(q(3, 1, -1, 1), q(1)),
        point(q(3, 1, -2, 1), q(1, 1, 1, 1)),
        point(q(3, 1, -3, 1), q(1, 1, 1, 1)),
        point(q(3, 1, -2, 1), q(1)),
    )

    @Test
    fun worldCornersOfTheFixtureBoardPiecesFreezeTheMeaningOfAt() {
        val (store, _) = storeOver("progress-v1.json")

        val mt = store.progress(PuzzleId("shapes-mini-2")).pieces[PieceId.MT] as? PieceSave.OnBoard
            ?: error("shapes-mini-2 MT must be on the board in the fixture")
        assertEquals(
            expectedMtCorners,
            PieceGeometry.corners(PieceId.MT, mt.turn, mt.mirrored, mt.at),
        )

        val pg = store.progress(PuzzleId("animals-cat")).pieces[PieceId.PG] as? PieceSave.OnBoard
            ?: error("animals-cat PG must be on the board in the fixture")
        assertEquals(
            expectedPgCorners,
            PieceGeometry.corners(PieceId.PG, pg.turn, pg.mirrored, pg.at),
        )
    }

    @Test
    fun theFixtureHoldsExactlyTheTwoBoardPiecesTheCornerCheckCovers() {
        // Guards the guard: if a board piece is ever added to the frozen fixture, the corner list above must grow.
        val (store, _) = storeOver("progress-v1.json")
        val boardPieces = listOf("shapes-mini-1", "shapes-mini-2", "animals-cat", "things-arrow")
            .flatMap { id -> store.progress(PuzzleId(id)).pieces.filterValues { it is PieceSave.OnBoard }.keys.map { id to it } }
        assertEquals(listOf("shapes-mini-2" to PieceId.MT, "animals-cat" to PieceId.PG), boardPieces)
    }

    // ---------------------------------------------------------------- 5. absent pieces and the tray turn frame (CR-2 F7)

    /**
     * guardrail G-09 / decision DA-46: a piece absent from an entry means "in the tray at its TYPE-001 resting turn, unmirrored"
     * (`SavedGame` KDoc: "A piece of the puzzle that is not in the map is in the tray in its TYPE-001 resting turn, unmirrored").
     * The reader must not invent entries for absent pieces, and the resting turns are the TYPE-001 literals.
     */
    @Test
    fun absentPiecesStayAbsentAndRestAtTheTypeOneRestingTurns() {
        val (store, _) = storeOver("progress-v1.json")
        // fixture entries that omit pieces: shapes-mini-2 holds MT, ST1, ST2 only; things-arrow holds SQ only; shapes-mini-1 none.
        assertEquals(setOf(PieceId.MT, PieceId.ST1, PieceId.ST2), store.progress(PuzzleId("shapes-mini-2")).pieces.keys)
        assertEquals(setOf(PieceId.SQ), store.progress(PuzzleId("things-arrow")).pieces.keys)
        assertEquals(emptyMap<PieceId, PieceSave>(), store.progress(PuzzleId("shapes-mini-1")).pieces)
        assertNull(store.progress(PuzzleId("animals-cat")).pieces[PieceId.ST2])
        assertNull(store.progress(PuzzleId("things-arrow")).pieces[PieceId.LT1])

        // TYPE-001 resting turns (req_types TYPE-001): large triangle 4, medium triangle 1, small triangle 4, square 1, parallelogram 0.
        assertEquals(Turn(4), TrayRules.restingTurn(PieceShape.LARGE_TRIANGLE))
        assertEquals(Turn(1), TrayRules.restingTurn(PieceShape.MEDIUM_TRIANGLE))
        assertEquals(Turn(4), TrayRules.restingTurn(PieceShape.SMALL_TRIANGLE))
        assertEquals(Turn(1), TrayRules.restingTurn(PieceShape.SQUARE))
        assertEquals(Turn(0), TrayRules.restingTurn(PieceShape.PARALLELOGRAM))
    }

    /**
     * guardrail G-09 / decision DA-46: a tray piece's stored `turn` is in the same frame as a board piece's: the TYPE-003 turn in
     * 45 degree steps, clockwise on screen, with the mirror applied first (`SavedGame.OnBoard` KDoc:
     * world = R(turn * 45 deg) * F(mirror) * local). Derived by hand from the local shapes SQ (0,0) (1,-1) (2,0) (1,1) and
     * ST (0,0) (2,0) (1,1):
     *  - SQ at turn 3 (cos = -1/2 s2, sin = 1/2 s2), unmirrored: (0,0) (0, s2) (-s2, s2) (-s2, 0);
     *  - ST2 at turn 4 (cos = -1, sin = 0), mirrored (x -> -x first): (0,0) (2,0) (1,-1).
     */
    @Test
    fun aTrayPiecesStoredTurnIsInTheSameFrameAsABoardPiecesTurn() {
        val (store, _) = storeOver("progress-v1.json")
        val sq = store.progress(PuzzleId("things-arrow")).pieces[PieceId.SQ] as? PieceSave.InTray ?: error("things-arrow SQ is in the tray")
        assertEquals(3, sq.turn.steps)
        assertEquals(
            listOf(point(q(0), q(0)), point(q(0), q(0, 1, 1, 1)), point(q(0, 1, -1, 1), q(0, 1, 1, 1)), point(q(0, 1, -1, 1), q(0))),
            PieceGeometry.offsets(PieceId.SQ.shape, sq.turn, sq.mirrored),
        )
        val st2 = store.progress(PuzzleId("shapes-mini-2")).pieces[PieceId.ST2] as? PieceSave.InTray ?: error("shapes-mini-2 ST2 is in the tray")
        assertEquals(4, st2.turn.steps)
        assertEquals(true, st2.mirrored)
        assertEquals(
            listOf(point(q(0), q(0)), point(q(2), q(0)), point(q(1), q(-1))),
            PieceGeometry.offsets(PieceId.ST2.shape, st2.turn, st2.mirrored),
        )
    }

    // ---------------------------------------------------------------- literals

    private companion object {
        // Computed on the fixture files after CRLF to LF (Python hashlib, TASK-T4a).
        const val V1_SHA256 = "ab1c9d1e53ad861ed42bb3ac18447b6bf16eb9e5595a20a71b6b6d46a982b63c"
        const val V1_FRESH_SHA256 = "2be7af50d2ae54bbf0bc4682032c02dc6c62f42fea5ed6211d5589093e89e109"

        fun q(a: Long): Q2 = Q2(Rational.of(a), Rational.ZERO)
        fun q(an: Long, ad: Long, bn: Long, bd: Long): Q2 = Q2(Rational.of(an, ad), Rational.of(bn, bd))
        fun point(x: Q2, y: Q2) = ExactPoint(x, y)

        // design section 2 document: "at":{"x":{"a":"2","b":"0"},"y":{"a":"-1/2","b":"1/2"}}
        val MT_AT = ExactPoint(q(2), q(-1, 2, 1, 2))

        // design section 2 document: "at":{"x":{"a":"3","b":"-1"},"y":{"a":"1","b":"0"}}
        val PG_AT = ExactPoint(q(3, 1, -1, 1), q(1))
    }
}
