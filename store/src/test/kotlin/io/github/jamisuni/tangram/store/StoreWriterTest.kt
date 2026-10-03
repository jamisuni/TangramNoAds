package io.github.jamisuni.tangram.store

import io.github.jamisuni.tangram.contracts.progress.GameSettings
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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

/**
 * The v1 writer and the IProgressStore semantics of design WO-004 section 2 ("Write", "Writer rules", "Semantics").
 *
 * guardrail G-09 (versioned document, explicit serial names)
 * guardrail G-10 (a disk failure never throws)
 * decision DA-46 (the document), DA-47 (atomic write, skip-equal, no throw), DA-65 (skip-equal), DA-68 (no file until a
 * save differs from the fresh state)
 *
 * No acceptance token. "Kill" at JVM level = a second `JsonProgressStore` on the same directory (design section 10).
 */
class StoreWriterTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun file(dir: File) = File(dir, JsonProgressStore.FILE_NAME)

    private fun treeOf(dir: File): JsonObject = Json.parseToJsonElement(file(dir).readText(Charsets.UTF_8)).jsonObject

    private fun q(an: Long, ad: Long = 1, bn: Long = 0, bd: Long = 1) = Q2(Rational.of(an, ad), Rational.of(bn, bd))

    private val inProgress = PuzzleProgress(
        PuzzleState.IN_PROGRESS,
        mapOf(
            PieceId.LT1 to PieceSave.OnBoard(ExactPoint(q(-3, 4, 5, 8), q(7, 2, -1, 64)), Turn(5), true),
            PieceId.LT2 to PieceSave.InTray(Turn(2), false),
            PieceId.MT to PieceSave.OnBoard(ExactPoint(q(2), q(-1, 2, 1, 2)), Turn(0), false),
            PieceId.SQ to PieceSave.InTray(Turn(7), true),
            PieceId.PG to PieceSave.OnBoard(ExactPoint(q(3, 1, -1), q(1)), Turn(7), true),
            PieceId.ST1 to PieceSave.InTray(Turn(0), false),
            PieceId.ST2 to PieceSave.InTray(Turn(4), true),
        ),
        puzzleSeconds = 321,
        bestSeconds = 150,
    )

    // ---------------------------------------------------------------- no file until something differs

    @Test
    fun aFreshInstallWritesNothingUntilASaveDiffersFromTheFreshState() {
        // decision DA-68 (2), DA-47 skip-equal.
        val dir = tmp.newFolder()
        val store = JsonProgressStore(dir)
        store.saveProgress(PuzzleId("a"), PuzzleProgress.NEW)
        store.savePlayTime(PlayTime.NONE)
        store.saveSettings(GameSettings())
        assertFalse("saving fresh values must not create the file", file(dir).exists())

        store.saveSettings(GameSettings(timerShown = true, soundOn = true))
        assertTrue(file(dir).isFile)
        assertEquals(1, treeOf(dir)["version"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun anEqualSaveIsSkippedForEveryKindOfSave() {
        // design section 2 "Write": saveProgress, saveLastShownPuzzle, savePlayTime and saveSettings skip the write
        // when the value equals the stored one. Observed by deleting the file between the saves.
        val dir = tmp.newFolder()
        val store = JsonProgressStore(dir)
        val day = PlayTime(LocalDate.of(2026, 10, 3), 10, 20)
        val settings = GameSettings(timerShown = true, soundOn = false)

        store.saveProgress(PuzzleId("a"), inProgress)
        store.saveLastShownPuzzle(PuzzleId("a"))
        store.savePlayTime(day)
        store.saveSettings(settings)
        assertTrue(file(dir).delete())

        store.saveProgress(PuzzleId("a"), inProgress)
        assertFalse("equal saveProgress rewrote the file", file(dir).exists())
        store.saveLastShownPuzzle(PuzzleId("a"))
        assertFalse("equal saveLastShownPuzzle rewrote the file", file(dir).exists())
        store.savePlayTime(day)
        assertFalse("equal savePlayTime rewrote the file", file(dir).exists())
        store.saveSettings(settings)
        assertFalse("equal saveSettings rewrote the file", file(dir).exists())

        // and a real change does write again, with the whole current model
        store.saveSettings(GameSettings(timerShown = false, soundOn = false))
        assertTrue(file(dir).isFile)
        val reopened = JsonProgressStore(dir)
        assertEquals(inProgress, reopened.progress(PuzzleId("a")))
        assertEquals(PuzzleId("a"), reopened.lastShownPuzzle())
        assertEquals(day, reopened.playTime())
    }

    // ---------------------------------------------------------------- semantics: what is saved is what is read

    @Test
    fun aSaveIsReadBackEqualAndSurvivesAKill() {
        // design section 2 "Semantics": "saveProgress stores the value as given and a later progress(id) returns an
        // equal value"; kill = a new store on the same directory.
        val dir = tmp.newFolder()
        val store = JsonProgressStore(dir)
        val solved = PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 47, 41)
        val fresh = PuzzleProgress(PuzzleState.NEW, mapOf(PieceId.SQ to PieceSave.InTray(Turn(3), false)), 0, null)
        store.saveProgress(PuzzleId("p-in"), inProgress)
        store.saveProgress(PuzzleId("p-solved"), solved)
        store.saveProgress(PuzzleId("p-new"), fresh)
        assertEquals(inProgress, store.progress(PuzzleId("p-in")))

        val afterKill = JsonProgressStore(dir)
        assertEquals(inProgress, afterKill.progress(PuzzleId("p-in")))
        assertEquals(solved, afterKill.progress(PuzzleId("p-solved")))
        assertEquals(fresh, afterKill.progress(PuzzleId("p-new")))
        assertEquals(PuzzleProgress.NEW, afterKill.progress(PuzzleId("p-none")))
    }

    @Test
    fun lastShownPlayTimeAndSettingsSurviveAKill() {
        val dir = tmp.newFolder()
        val store = JsonProgressStore(dir)
        store.saveLastShownPuzzle(PuzzleId("shapes-mini-2"))
        store.savePlayTime(PlayTime(LocalDate.of(2026, 12, 31), 59, 3600))
        store.saveSettings(GameSettings(timerShown = true, soundOn = false))
        val afterKill = JsonProgressStore(dir)
        assertEquals(PuzzleId("shapes-mini-2"), afterKill.lastShownPuzzle())
        assertEquals(PlayTime(LocalDate.of(2026, 12, 31), 59, 3600), afterKill.playTime())
        assertEquals(GameSettings(timerShown = true, soundOn = false), afterKill.settings())
    }

    @Test
    fun savingOnePuzzleKeepsTheOthers() {
        val dir = tmp.newFolder()
        val store = JsonProgressStore(dir)
        val solved = PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 5, 5)
        store.saveProgress(PuzzleId("a"), solved)
        store.saveProgress(PuzzleId("b"), inProgress)
        store.saveProgress(PuzzleId("b"), inProgress.copy(puzzleSeconds = 400))
        val afterKill = JsonProgressStore(dir)
        assertEquals(solved, afterKill.progress(PuzzleId("a")))
        assertEquals(inProgress.copy(puzzleSeconds = 400), afterKill.progress(PuzzleId("b")))
    }

    // ---------------------------------------------------------------- the document the writer produces

    @Test
    fun theWrittenDocumentUsesTheFrozenSerialNamesAndEncodings() {
        // design section 2 field table; guardrail G-09 (explicit serial names, never enum.name).
        val dir = tmp.newFolder()
        val store = JsonProgressStore(dir)
        store.saveProgress(PuzzleId("p-in"), inProgress)
        store.saveProgress(PuzzleId("p-solved"), PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 47, 41))
        store.saveProgress(PuzzleId("p-new"), PuzzleProgress(PuzzleState.NEW, mapOf(PieceId.SQ to PieceSave.InTray(Turn(3), false)), 0, null))
        val root = treeOf(dir)
        val puzzles = root["puzzles"]!!.jsonObject

        assertEquals("in_progress", puzzles["p-in"]!!.jsonObject["state"]!!.jsonPrimitive.content)
        assertEquals("solved", puzzles["p-solved"]!!.jsonObject["state"]!!.jsonPrimitive.content)
        assertEquals("new", puzzles["p-new"]!!.jsonObject["state"]!!.jsonPrimitive.content)

        val pieces = puzzles["p-in"]!!.jsonObject["pieces"]!!.jsonObject
        assertEquals(setOf("LT1", "LT2", "MT", "SQ", "PG", "ST1", "ST2"), pieces.keys)

        // board piece: where/turn/mirrored/at, rationals as strings, normalized, integers without "/1"
        val mt = pieces["MT"]!!.jsonObject
        assertEquals(setOf("where", "turn", "mirrored", "at"), mt.keys)
        assertEquals("board", mt["where"]!!.jsonPrimitive.content)
        val at = mt["at"]!!.jsonObject
        assertEquals("2", at["x"]!!.jsonObject["a"]!!.jsonPrimitive.content)
        assertEquals("0", at["x"]!!.jsonObject["b"]!!.jsonPrimitive.content)
        assertEquals("-1/2", at["y"]!!.jsonObject["a"]!!.jsonPrimitive.content)
        assertEquals("1/2", at["y"]!!.jsonObject["b"]!!.jsonPrimitive.content)
        assertTrue("a rational is a JSON string", at["x"]!!.jsonObject["a"]!!.jsonPrimitive.isString)
        val lt1 = pieces["LT1"]!!.jsonObject["at"]!!.jsonObject
        assertEquals("-3/4", lt1["x"]!!.jsonObject["a"]!!.jsonPrimitive.content)
        assertEquals("5/8", lt1["x"]!!.jsonObject["b"]!!.jsonPrimitive.content)
        assertEquals("-1/64", lt1["y"]!!.jsonObject["b"]!!.jsonPrimitive.content)

        // tray piece: no `at`
        val st2 = pieces["ST2"]!!.jsonObject
        assertEquals(setOf("where", "turn", "mirrored"), st2.keys)
        assertEquals("tray", st2["where"]!!.jsonPrimitive.content)
        assertEquals("4", st2["turn"]!!.jsonPrimitive.content)
        assertEquals("true", st2["mirrored"]!!.jsonPrimitive.content)
    }

    @Test
    fun nullsAreWrittenExplicitlyAndEverySectionIsAlwaysWritten() {
        // design section 2 "Writer rules": every section always written, nulls explicit (lastShown, day, bestSeconds).
        val dir = tmp.newFolder()
        val store = JsonProgressStore(dir)
        store.saveProgress(PuzzleId("p"), PuzzleProgress(PuzzleState.IN_PROGRESS, mapOf(PieceId.SQ to PieceSave.InTray(Turn(1), false)), 12, null))
        val root = treeOf(dir)
        assertEquals(listOf("version", "lastShown", "puzzles", "playTime", "settings"), root.keys.toList())
        assertEquals(JsonNull, root["lastShown"])
        val playTime = root["playTime"]!!.jsonObject
        assertTrue(playTime.containsKey("day"))
        assertEquals(JsonNull, playTime["day"])
        assertEquals("0", playTime["todaySeconds"]!!.jsonPrimitive.content)
        assertEquals("0", playTime["totalSeconds"]!!.jsonPrimitive.content)
        val entry = root["puzzles"]!!.jsonObject["p"]!!.jsonObject
        assertTrue("bestSeconds is always written", entry.containsKey("bestSeconds"))
        assertEquals(JsonNull, entry["bestSeconds"])
        assertEquals("12", entry["puzzleSeconds"]!!.jsonPrimitive.content)
        val settings = root["settings"]!!.jsonObject
        assertEquals("false", settings["timerShown"]!!.jsonPrimitive.content)
        assertEquals("true", settings["soundOn"]!!.jsonPrimitive.content)
    }

    @Test
    fun theSameModelGivesTheSameJsonTree() {
        // design section 2 "Writer rules": "so the same model gives the same JSON tree".
        val a = tmp.newFolder()
        val b = tmp.newFolder()
        for (dir in listOf(a, b)) {
            val s = JsonProgressStore(dir)
            s.saveProgress(PuzzleId("x"), inProgress)
            s.saveLastShownPuzzle(PuzzleId("x"))
            s.savePlayTime(PlayTime(LocalDate.of(2026, 10, 3), 1, 2))
            s.saveSettings(GameSettings(timerShown = true, soundOn = true))
        }
        assertEquals(treeOf(a), treeOf(b))
    }

    @Test
    fun entriesOfPuzzlesTheLibraryNoLongerHasAreKept() {
        // design section 2 field table: "entries of ids the library no longer has are kept and ignored by the game (F4)".
        val dir = tmp.newFolder()
        file(dir).writeText(
            """{"version":1,"lastShown":null,"puzzles":{"gone-puzzle":{"state":"solved","pieces":{},"puzzleSeconds":9,"bestSeconds":9}},""" +
                """"playTime":{"day":null,"todaySeconds":0,"totalSeconds":0},"settings":{"timerShown":false,"soundOn":true}}""",
            Charsets.UTF_8,
        )
        val store = JsonProgressStore(dir)
        store.saveProgress(PuzzleId("other"), inProgress)
        val puzzles = treeOf(dir)["puzzles"]!!.jsonObject
        assertTrue("the entry of a removed puzzle was dropped by a save", puzzles.containsKey("gone-puzzle"))
        assertEquals("solved", puzzles["gone-puzzle"]!!.jsonObject["state"]!!.jsonPrimitive.content)
        assertEquals(PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 9, 9), JsonProgressStore(dir).progress(PuzzleId("gone-puzzle")))
    }

    @Test
    fun theWriteLeavesNoTemporaryFileBehind() {
        // design section 2 "Write": temp file then atomic move.
        val dir = tmp.newFolder()
        val store = JsonProgressStore(dir)
        store.saveProgress(PuzzleId("a"), inProgress)
        store.saveSettings(GameSettings(timerShown = true, soundOn = true))
        assertEquals(listOf(JsonProgressStore.FILE_NAME), dir.list()!!.toList())
    }

    // ---------------------------------------------------------------- reset

    @Test
    fun resetAllProgressDoesExactlyWhatTheKdocListsAndLeavesLastShown() {
        // IProgressStore.resetAllProgress KDoc: every puzzle New, best times erased, play time NONE, settings kept;
        // design section 2 "Semantics": it does not touch lastShown.
        val dir = tmp.newFolder()
        val store = JsonProgressStore(dir)
        store.saveProgress(PuzzleId("a"), PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 47, 41))
        store.saveProgress(PuzzleId("b"), inProgress)
        store.saveLastShownPuzzle(PuzzleId("b"))
        store.savePlayTime(PlayTime(LocalDate.of(2026, 10, 3), 10, 20))
        store.saveSettings(GameSettings(timerShown = true, soundOn = false))

        store.resetAllProgress()
        for (s in listOf(store, JsonProgressStore(dir))) {
            assertEquals(PuzzleProgress.NEW, s.progress(PuzzleId("a")))
            assertEquals("the best time is erased too", null, s.progress(PuzzleId("a")).bestSeconds)
            assertEquals(PuzzleProgress.NEW, s.progress(PuzzleId("b")))
            assertEquals(PlayTime.NONE, s.playTime())
            assertEquals(GameSettings(timerShown = true, soundOn = false), s.settings())
            assertEquals(PuzzleId("b"), s.lastShownPuzzle())
        }
    }

    // ---------------------------------------------------------------- sync and write failures

    @Test
    fun syncNeverThrowsAndChangesNothing() {
        // design section 2 "Write": sync() is public, returns Unit and never throws.
        val empty = tmp.newFolder()
        val none = JsonProgressStore(empty)
        none.sync()
        assertFalse("sync must not create a file", file(empty).exists())

        val dir = tmp.newFolder()
        val store = JsonProgressStore(dir)
        store.saveProgress(PuzzleId("a"), inProgress)
        val before = file(dir).readText(Charsets.UTF_8)
        store.sync()
        store.sync()
        assertEquals(before, file(dir).readText(Charsets.UTF_8))
        assertEquals(inProgress, JsonProgressStore(dir).progress(PuzzleId("a")))
    }

    @Test
    fun aDiskWriteFailureNeverThrowsKeepsMemoryAndTheNextSaveRetriesTheWholeDocument() {
        // design section 2 "Write": "A disk-level write failure never throws (G-10): memory stays current,
        // failedWrites counts it, and the next save retries the whole document." The temp file name
        // `progress.json.tmp` is occupied by a directory, so writing the temp file fails on every platform.
        val dir = tmp.newFolder()
        val store = JsonProgressStore(dir)
        store.saveSettings(GameSettings(timerShown = true, soundOn = true)) // a good first write
        assertEquals(0, store.failedWrites)

        val blocker = File(dir, "${JsonProgressStore.FILE_NAME}.tmp")
        assertTrue(blocker.mkdir())
        File(blocker, "keep.txt").writeText("x")

        store.saveProgress(PuzzleId("a"), inProgress) // must not throw
        assertEquals(1, store.failedWrites)
        assertEquals("memory stays current", inProgress, store.progress(PuzzleId("a")))
        assertEquals("the failed write is not on disk", PuzzleProgress.NEW, JsonProgressStore(dir).progress(PuzzleId("a")))

        assertTrue(File(blocker, "keep.txt").delete())
        assertTrue(blocker.delete())
        store.saveLastShownPuzzle(PuzzleId("a")) // the next save retries everything
        val afterKill = JsonProgressStore(dir)
        assertEquals("the earlier failed value is in the retried document", inProgress, afterKill.progress(PuzzleId("a")))
        assertEquals(PuzzleId("a"), afterKill.lastShownPuzzle())
        assertEquals(GameSettings(timerShown = true, soundOn = true), afterKill.settings())
    }
}
