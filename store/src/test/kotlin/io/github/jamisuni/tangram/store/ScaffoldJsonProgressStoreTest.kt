package io.github.jamisuni.tangram.store

import io.github.jamisuni.tangram.contracts.progress.GameSettings
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PlayTime
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

/**
 * SCAFFOLDING (disposable, TASK-023): reader-table rows, skip-equal, newest-3, asideFailed, sync, bound edges.
 * Pins decisions DA-46, DA-47, DA-63, DA-64, DA-65, DA-68; no acceptance tokens on purpose.
 */
class ScaffoldJsonProgressStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun resource(name: String): String =
        ScaffoldJsonProgressStoreTest::class.java.getResourceAsStream("/fixtures/$name")!!.use { String(it.readBytes()) }

    private fun dirWith(text: String): File {
        val dir = tmp.newFolder()
        File(dir, JsonProgressStore.FILE_NAME).writeText(text)
        return dir
    }

    private fun names(dir: File) = dir.list()!!.sorted()

    private val good1 = PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 47, 41)

    // ---- reader table rows ----

    @Test
    fun noFileIsFreshInstallAndCreatesNoFile() { // decision DA-68
        val dir = tmp.newFolder()
        val s = JsonProgressStore(dir)
        assertNull(s.lastShownPuzzle())
        assertEquals(PuzzleProgress.NEW, s.progress(PuzzleId("x")))
        assertEquals(PlayTime.NONE, s.playTime())
        assertEquals(GameSettings(), s.settings())
        s.saveProgress(PuzzleId("x"), PuzzleProgress.NEW)
        s.saveSettings(GameSettings())
        s.savePlayTime(PlayTime.NONE)
        assertEquals(emptyList<String>(), names(dir))
        s.saveLastShownPuzzle(PuzzleId("x"))
        assertEquals(listOf("progress.json"), names(dir))
    }

    @Test
    fun badDocumentsAreMovedAsideWholeAndReadFresh() { // decision DA-64
        val bad = listOf(
            "", "not json {", "[1,2]", "{}", "{\"version\": \"1\"}", "{\"version\": 1.5}", "{\"version\": 0}",
            "{\"version\": -3, \"settings\": {\"timerShown\": true, \"soundOn\": false}}",
        )
        for (text in bad) {
            val dir = dirWith(text)
            val s = JsonProgressStore(dir) { 123L }
            assertEquals("for [$text]", File(dir, "progress-bad-123.json"), s.movedAside)
            assertEquals(text, s.movedAside!!.readText())
            assertFalse(File(dir, "progress.json").exists())
            assertNull(s.partialCopy)
            assertEquals(GameSettings(), s.settings())
            assertFalse(s.asideFailed)
        }
    }

    @Test
    fun unreadableFileIsMovedAsideAndReadsFresh() { // decision DA-64: an I/O error on read
        val dir = dirWith("{}")
        val io = ScaffoldIo().apply { failRead = true }
        val s = JsonProgressStore(dir, { 5L }, io)
        assertNotNull(s.movedAside)
        assertEquals(PlayTime.NONE, s.playTime())
    }

    @Test
    fun newerVersionIsMovedAsideAsVersionN() { // decision DA-64
        val dir = dirWith(resource("version-2.json"))
        val s = JsonProgressStore(dir) { 77L }
        assertEquals(File(dir, "progress-v2-77.json"), s.movedAside)
        assertEquals(PuzzleProgress.NEW, s.progress(PuzzleId("good-1")))
        assertNull(s.lastShownPuzzle())
        assertEquals(listOf("progress-v2-77.json"), names(dir))
    }

    @Test
    fun damagedSectionReadsFreshForThatSectionOnlyWithPartialCopy() { // decision DA-64
        val dir = dirWith(resource("damaged-section.json"))
        val s = JsonProgressStore(dir) { 9L }
        assertEquals(GameSettings(), s.settings())
        assertEquals(good1, s.progress(PuzzleId("good-1")))
        assertEquals(PuzzleId("good-1"), s.lastShownPuzzle())
        assertEquals(PlayTime(LocalDate.of(2026, 10, 3), 340, 5120), s.playTime())
        assertEquals(File(dir, "progress-partial-9.json"), s.partialCopy)
        assertNull(s.movedAside)
        assertEquals(resource("damaged-section.json"), s.partialCopy!!.readText())
        assertTrue(File(dir, "progress.json").exists())
    }

    @Test
    fun otherSectionDamagesEachReadFreshForThatSectionOnly() { // decision DA-64
        fun doc(last: String, pz: String, pt: String, st: String) =
            """{"version":1,"lastShown":$last,"puzzles":$pz,"playTime":$pt,"settings":$st}"""
        val okPt = """{"day":null,"todaySeconds":3,"totalSeconds":4}"""
        val okSt = """{"timerShown":true,"soundOn":false}"""
        val okPz = """{"a":{"state":"solved","pieces":{},"puzzleSeconds":1,"bestSeconds":null}}"""
        val docs = listOf(
            doc("7", okPz, okPt, okSt), doc("null", "[]", okPt, okSt),
            doc("null", okPz, """{"day":"nope","todaySeconds":3,"totalSeconds":4}""", okSt),
            doc("null", okPz, """{"day":null,"todaySeconds":-1,"totalSeconds":4}""", okSt),
            doc("null", okPz, okPt, """{"timerShown":"true","soundOn":false}"""),
        )
        for (d in docs) {
            val dir = dirWith(d)
            val s = JsonProgressStore(dir) { 1L }
            assertNotNull("for $d", s.partialCopy)
            assertNull(s.movedAside)
        }
        // and the undamaged sections still load
        val s = JsonProgressStore(dirWith(docs[4]))
        assertEquals(3L, s.playTime().todaySeconds)
        assertEquals(PuzzleState.SOLVED, s.progress(PuzzleId("a")).state)
    }

    @Test
    fun damagedEntryReadsNewOthersLoadWithPartialCopy() { // decision DA-64
        val dir = dirWith(resource("damaged-entry.json"))
        val s = JsonProgressStore(dir) { 4L }
        assertEquals(PuzzleProgress.NEW, s.progress(PuzzleId("bad-1")))
        assertEquals(good1, s.progress(PuzzleId("good-1")))
        assertEquals(GameSettings(timerShown = true, soundOn = false), s.settings())
        assertNotNull(s.partialCopy)
        assertNull(s.movedAside)
    }

    @Test
    fun hugeRationalEntryReadsNew() { // decision DA-63 (2^62 would overflow the exact arithmetic)
        val s = JsonProgressStore(dirWith(resource("huge-rational.json")))
        assertEquals(PuzzleProgress.NEW, s.progress(PuzzleId("bad-1")))
        assertEquals(good1, s.progress(PuzzleId("good-1")))
        assertNotNull(s.partialCopy)
    }

    @Test
    fun denominator63EntryReadsNew() { // decision DA-63
        val s = JsonProgressStore(dirWith(resource("denominator-63.json")))
        assertEquals(PuzzleProgress.NEW, s.progress(PuzzleId("bad-1")))
        assertEquals(good1, s.progress(PuzzleId("good-1")))
    }

    private fun readsAt(a: String): PuzzleProgress {
        val text = """{"version":1,"lastShown":null,"puzzles":{"p":{"state":"in_progress","pieces":{"MT":
            {"where":"board","turn":0,"mirrored":false,"at":{"x":{"a":"$a","b":"0"},"y":{"a":"0","b":"0"}}}},
            "puzzleSeconds":1,"bestSeconds":null}},"playTime":{"day":null,"todaySeconds":0,"totalSeconds":0},
            "settings":{"timerShown":false,"soundOn":true}}"""
        return JsonProgressStore(dirWith(text)).progress(PuzzleId("p"))
    }

    @Test
    fun boundEdges() { // decision DA-63 and DA-68
        val valid = listOf("65536", "-65536", "0", "1/64", "-65535/64", "1/2", "3/32", "65535/64")
        for (r in valid) assertEquals("valid [$r]", PuzzleState.IN_PROGRESS, readsAt(r).state)
        val invalid = listOf(
            "65537", "-65537", "1/128", "1/3", "1/63", "2/4", "0/2", "1/1", "5/1", "-0", "007", "1/0", "",
            "1/", "/2", " 1", "+1", "1.5", "1e2", "99999999999999999999", "65537/64", "1/-2",
        )
        for (r in invalid) assertEquals("invalid [$r]", PuzzleProgress.NEW, readsAt(r))
    }

    @Test
    fun unknownKeysAndLastShownPassThrough() { // decision DA-68 (3): unknown lastShown id is not validated
        val text = """{"version":1,"lastShown":"gone-puzzle","extra":1,"puzzles":{},
            "playTime":{"day":null,"todaySeconds":0,"totalSeconds":0},"settings":{"timerShown":false,"soundOn":true}}"""
        val dir = dirWith(text)
        val s = JsonProgressStore(dir)
        assertEquals(PuzzleId("gone-puzzle"), s.lastShownPuzzle())
        assertNull(s.partialCopy)
        assertEquals(listOf("progress.json"), names(dir))
    }

    @Test
    fun badPieceFieldsMakeTheEntryNew() { // decision DA-46
        fun entry(piece: String) = """{"version":1,"lastShown":null,"puzzles":{"p":{"state":"in_progress","pieces":{$piece},
            "puzzleSeconds":1,"bestSeconds":null}},"playTime":{"day":null,"todaySeconds":0,"totalSeconds":0},
            "settings":{"timerShown":false,"soundOn":true}}"""
        val bad = listOf(
            """"XX":{"where":"tray","turn":0,"mirrored":false}""",
            """"MT":{"where":"floor","turn":0,"mirrored":false}""",
            """"MT":{"where":"tray","turn":8,"mirrored":false}""",
            """"MT":{"where":"tray","turn":-1,"mirrored":false}""",
            """"MT":{"where":"tray","turn":0,"mirrored":"no"}""",
            """"MT":{"where":"board","turn":0,"mirrored":false}""",
        )
        for (b in bad) assertEquals("for $b", PuzzleProgress.NEW, JsonProgressStore(dirWith(entry(b))).progress(PuzzleId("p")))
        val ok = JsonProgressStore(dirWith(entry(""""MT":{"where":"tray","turn":7,"mirrored":true}""")))
        assertEquals(
            mapOf(PieceId.MT to PieceSave.InTray(Turn(7), true)),
            ok.progress(PuzzleId("p")).pieces,
        )
    }

    @Test
    fun negativeSecondsMakeTheEntryNew() { // decision DA-46
        val text = """{"version":1,"lastShown":null,"puzzles":{"p":{"state":"new","pieces":{},"puzzleSeconds":-1,"bestSeconds":null}},
            "playTime":{"day":null,"todaySeconds":0,"totalSeconds":0},"settings":{"timerShown":false,"soundOn":true}}"""
        assertEquals(PuzzleProgress.NEW, JsonProgressStore(dirWith(text)).progress(PuzzleId("p")))
    }

    // ---- newest 3 ----

    @Test
    fun onlyTheNewest3AsideFilesSurvive() { // decision DA-64 (N9)
        val dir = tmp.newFolder()
        for (t in listOf(10L, 20L, 30L, 40L)) {
            File(dir, "progress.json").writeText("garbage")
            JsonProgressStore(dir) { t }
        }
        assertEquals(listOf("progress-bad-20.json", "progress-bad-30.json", "progress-bad-40.json"), names(dir))
        // CR-2 F3: a progress-v<N> file is never pruned, and does not count towards the 3
        File(dir, "progress.json").writeText(resource("version-2.json"))
        JsonProgressStore(dir) { 50L }
        assertEquals(
            listOf("progress-bad-20.json", "progress-bad-30.json", "progress-bad-40.json", "progress-v2-50.json"),
            names(dir),
        )
        File(dir, "progress.json").writeText(resource("damaged-entry.json"))
        JsonProgressStore(dir) { 60L }
        assertEquals(
            listOf("progress-bad-30.json", "progress-bad-40.json", "progress-partial-60.json", "progress-v2-50.json", "progress.json"),
            names(dir),
        )
        for (t in listOf(70L, 80L, 90L)) { // many more garbage launches never evict the v2 file
            File(dir, "progress.json").writeText("garbage")
            JsonProgressStore(dir) { t }
        }
        assertTrue(File(dir, "progress-v2-50.json").exists())
        assertEquals(3, dir.list()!!.count { it.startsWith("progress-bad-") || it.startsWith("progress-partial-") })
    }

    // ---- asideFailed ----

    @Test
    fun moveFailsCopyWorksKeepsTheDocument() { // decision DA-64
        val dir = dirWith("garbage")
        val io = ScaffoldIo().apply { failMove = true }
        val s = JsonProgressStore(dir, { 1L }, io)
        assertEquals(File(dir, "progress-bad-1.json"), s.movedAside)
        assertEquals("garbage", s.movedAside!!.readText())
        assertFalse(s.asideFailed)
    }

    @Test
    fun moveAndCopyBothFailSetsAsideFailedAndLaterSavesOverwrite() { // decision DA-64 (G-09 conflict, ai+inform)
        val dir = dirWith("garbage")
        val io = ScaffoldIo().apply { failMove = true; failCopy = true }
        val s = JsonProgressStore(dir, { 1L }, io)
        assertTrue(s.asideFailed)
        assertNull(s.movedAside)
        assertEquals("garbage", File(dir, "progress.json").readText())
        s.saveSettings(GameSettings(timerShown = true))
        assertTrue(File(dir, "progress.json").readText().startsWith("{\"version\":1"))
        assertEquals(GameSettings(timerShown = true), JsonProgressStore(dir).settings())
    }

    @Test
    fun partialCopyFailureSetsAsideFailed() { // decision DA-64
        val dir = dirWith(resource("damaged-entry.json"))
        val io = ScaffoldIo().apply { failCopy = true }
        val s = JsonProgressStore(dir, { 1L }, io)
        assertTrue(s.asideFailed)
        assertNull(s.partialCopy)
        assertEquals(good1, s.progress(PuzzleId("good-1")))
    }

    // ---- writer ----

    @Test
    fun skipEqualWritesNothingForAnEqualValue() { // decision DA-65
        val dir = tmp.newFolder()
        val io = ScaffoldIo()
        val s = JsonProgressStore(dir, { 1L }, io)
        s.saveProgress(PuzzleId("a"), good1)
        assertEquals(1, io.writes)
        s.saveProgress(PuzzleId("a"), good1)
        s.saveLastShownPuzzle(PuzzleId("a"))
        assertEquals(2, io.writes)
        s.saveLastShownPuzzle(PuzzleId("a"))
        s.saveSettings(GameSettings(timerShown = true))
        s.saveSettings(GameSettings(timerShown = true))
        s.savePlayTime(PlayTime(LocalDate.of(2026, 1, 2), 1, 2))
        s.savePlayTime(PlayTime(LocalDate.of(2026, 1, 2), 1, 2))
        assertEquals(4, io.writes)
        s.resetAllProgress()
        assertEquals(5, io.writes)
        s.resetAllProgress()
        assertEquals(5, io.writes)
        // reset: puzzles New, play time NONE, settings and lastShown kept
        assertEquals(PuzzleProgress.NEW, s.progress(PuzzleId("a")))
        assertEquals(PlayTime.NONE, s.playTime())
        assertEquals(GameSettings(timerShown = true), s.settings())
        assertEquals(PuzzleId("a"), s.lastShownPuzzle())
    }

    @Test
    fun writeFailureNeverThrowsMemoryStaysCurrentAndNextSaveRetries() { // decision DA-47
        val dir = tmp.newFolder()
        val io = ScaffoldIo().apply { failWrite = true }
        val s = JsonProgressStore(dir, { 1L }, io)
        s.saveProgress(PuzzleId("a"), good1)
        assertEquals(1, s.failedWrites)
        assertEquals(good1, s.progress(PuzzleId("a")))
        assertFalse(File(dir, "progress.json").exists())
        io.failWrite = false
        s.saveProgress(PuzzleId("a"), good1) // equal value, but the disk is behind: must retry
        assertEquals(good1, JsonProgressStore(dir).progress(PuzzleId("a")))
        assertEquals(1, s.failedWrites)
    }

    @Test
    fun writerRoundTripsEveryPieceKindAndLeavesNoTempFile() { // decision DA-46
        val dir = tmp.newFolder()
        val s = JsonProgressStore(dir)
        val p = PuzzleProgress(
            PuzzleState.IN_PROGRESS,
            mapOf(
                PieceId.MT to PieceSave.OnBoard(
                    io.github.jamisuni.tangram.kernel.model.ExactPoint(
                        io.github.jamisuni.tangram.kernel.model.Q2(
                            io.github.jamisuni.tangram.kernel.model.Rational.of(-3, 2),
                            io.github.jamisuni.tangram.kernel.model.Rational.of(65536, 1),
                        ),
                        io.github.jamisuni.tangram.kernel.model.Q2.ZERO,
                    ),
                    Turn(5), true,
                ),
                PieceId.SQ to PieceSave.InTray(Turn(3), true),
            ),
            12, null,
        )
        s.saveProgress(PuzzleId("p"), p)
        s.saveLastShownPuzzle(PuzzleId("p"))
        assertEquals(listOf("progress.json"), names(dir))
        val r = JsonProgressStore(dir)
        assertEquals(p, r.progress(PuzzleId("p")))
        assertNull(r.partialCopy)
        assertFalse(File(dir, "progress.json").readText().contains("\n"))
    }

    @Test
    fun refusedAtomicMoveIsACountedWriteFailureWithNoFallback() { // decision DA-47 (CR-2 F1)
        val dir = tmp.newFolder()
        val io = ScaffoldIo()
        val s = JsonProgressStore(dir, { 1L }, io)
        s.saveSettings(GameSettings(timerShown = true))
        io.refuseAtomic = true
        s.saveSettings(GameSettings(timerShown = false))
        assertEquals(1, s.failedWrites)
        assertFalse(GameSettings(timerShown = true) != JsonProgressStore(dir).settings()) // old file intact
        io.refuseAtomic = false
        s.sync() // F4: dirty, so sync retries the write
        assertEquals(GameSettings(timerShown = false), JsonProgressStore(dir).settings())
    }

    @Test
    fun serialNamesAreTheFrozenLiterals() { // guardrail G-09 (CR-2 F2)
        val dir = tmp.newFolder()
        val s = JsonProgressStore(dir)
        val pieces = PieceId.values().associateWith { PieceSave.InTray(Turn(1), false) as PieceSave }
        s.saveProgress(PuzzleId("p"), PuzzleProgress(PuzzleState.IN_PROGRESS, pieces, 1, null))
        val text = File(dir, "progress.json").readText()
        for (n in listOf("LT1", "LT2", "MT", "SQ", "PG", "ST1", "ST2")) assertTrue(text.contains("\"$n\":"))
        assertTrue(text.contains("\"in_progress\""))
    }

    @Test
    fun readOnlySeamsAreReadable() { // CR-2 F10: private setters, readable in the module
        val s = JsonProgressStore(tmp.newFolder())
        assertNull(s.movedAside); assertNull(s.partialCopy); assertFalse(s.asideFailed); assertEquals(0, s.failedWrites)
    }

    // ---- sync ----

    @Test
    fun syncForcesTheFileAndTheDirectoryAndNeverThrows() { // decision DA-47 (O2)
        val dir = tmp.newFolder()
        val io = ScaffoldIo()
        val s = JsonProgressStore(dir, { 1L }, io)
        s.saveSettings(GameSettings(timerShown = true))
        s.sync()
        assertEquals(listOf(File(dir, "progress.json"), dir), io.forced)
        io.failForce = true
        s.sync() // swallowed
        JsonProgressStore(dir).sync() // the real file system
        JsonProgressStore(tmp.newFolder()).sync() // no file yet
    }
}

/** SCAFFOLDING: real file operations with switchable failures and counters. */
internal class ScaffoldIo : StoreIo {
    var failRead = false
    var failMove = false
    var failCopy = false
    var failWrite = false
    var failForce = false
    var refuseAtomic = false
    var writes = 0
    val forced = mutableListOf<File>()

    override fun readBytes(f: File): ByteArray {
        if (failRead) throw java.io.IOException("scaffold read failure")
        return RealStoreIo.readBytes(f)
    }

    override fun move(src: File, dst: File) {
        if (failMove) throw java.io.IOException("scaffold move failure")
        RealStoreIo.move(src, dst)
    }

    override fun copy(src: File, dst: File) {
        if (failCopy) throw java.io.IOException("scaffold copy failure")
        RealStoreIo.copy(src, dst)
    }

    override fun writeAtomic(target: File, bytes: ByteArray) {
        if (failWrite) throw java.io.IOException("scaffold write failure")
        if (refuseAtomic) throw java.nio.file.AtomicMoveNotSupportedException("a", "b", "scaffold refuses")
        writes++
        RealStoreIo.writeAtomic(target, bytes)
    }

    override fun force(f: File) {
        forced += f
        if (failForce) throw java.io.IOException("scaffold force failure")
        RealStoreIo.force(f)
    }
}
