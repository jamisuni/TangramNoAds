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
 * The v1 reader rules of design WO-004 section 2 ("Reader rules" table), decisions DA-46, DA-63, DA-64, DA-68.
 *
 * guardrail G-09 (an unreadable or newer document is moved aside, never overwritten)
 * guardrail G-10 (stored data never crashes; an unreadable save is a New puzzle)
 * decision DA-46, DA-63, DA-64, DA-68
 *
 * No acceptance token: the format is a guardrail/decision, not an acceptance criterion (design "Acceptance IDs").
 * Every document is built here in a temp dir, so no unfrozen fixture file name is assumed.
 * The aside names (`progress-bad-<nowMs>.json`, `progress-v<version>-<nowMs>.json`, `progress-partial-<nowMs>.json`)
 * are the design's text.
 */
class StoreReaderDamageTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val now = 1234L

    // ---------------------------------------------------------------- document building

    private val okPlayTime = """{"day": "2026-10-03", "todaySeconds": 340, "totalSeconds": 5120}"""
    private val okSettings = """{"timerShown": true, "soundOn": false}"""

    private fun tray(turn: Int = 0, mirrored: Boolean = false) = """{"where":"tray","turn":$turn,"mirrored":$mirrored}"""

    private fun board(turn: Int = 0, mirrored: Boolean = false, at: String = AT_OK) =
        """{"where":"board","turn":$turn,"mirrored":$mirrored,"at":$at}"""

    private fun entry(
        state: String = "in_progress",
        pieces: String = """{"MT": ${board()}, "ST1": ${tray(7)}}""",
        puzzleSeconds: String = "99",
        bestSeconds: String = "null",
    ) = """{"state":"$state","pieces":$pieces,"puzzleSeconds":$puzzleSeconds,"bestSeconds":$bestSeconds}"""

    private val okSibling = """{"state":"solved","pieces":{},"puzzleSeconds":5,"bestSeconds":5}"""

    private fun doc(
        puzzles: String = "{}",
        lastShown: String = "null",
        playTime: String = okPlayTime,
        settings: String = okSettings,
        version: String = "1",
        extra: String = "",
    ) = """{"version":$version,"lastShown":$lastShown,"puzzles":$puzzles,"playTime":$playTime,"settings":$settings$extra}"""

    private fun docWithEntry(entryJson: String) = doc(puzzles = """{"bad":$entryJson,"ok":$okSibling}""", lastShown = "\"ok\"")

    private fun dirWith(text: String): File {
        val dir = tmp.newFolder()
        File(dir, JsonProgressStore.FILE_NAME).writeText(text, Charsets.UTF_8)
        return dir
    }

    private fun open(dir: File, clock: () -> Long = { now }) = JsonProgressStore(dir, clock)

    private fun asides(dir: File, prefix: String) = dir.listFiles()!!.filter { it.name.startsWith(prefix) }.sortedBy { it.name }

    private fun assertFresh(store: JsonProgressStore) {
        assertNull(store.lastShownPuzzle())
        assertEquals(PlayTime.NONE, store.playTime())
        assertEquals(GameSettings(), store.settings())
        assertEquals(PuzzleProgress.NEW, store.progress(PuzzleId("ok")))
        assertEquals(PuzzleProgress.NEW, store.progress(PuzzleId("bad")))
    }

    // ---------------------------------------------------------------- no file, and a valid document

    @Test
    fun noFileIsAFreshInstallAndCreatesNothing() {
        val dir = tmp.newFolder()
        val store = open(dir)
        assertFresh(store)
        assertNull(store.movedAside)
        assertNull(store.partialCopy)
        assertFalse(store.asideFailed)
        assertEquals(emptyList<String>(), dir.list()!!.toList())
    }

    @Test
    fun unknownKeysAreIgnoredAndAreNoDamage() {
        // design section 2 "Writer rules": "Unknown keys are ignored on read".
        val withExtras = doc(
            puzzles = """{"ok":{"state":"solved","pieces":{},"puzzleSeconds":5,"bestSeconds":5,"future":1}}""",
            extra = ""","future":{"x":1}""",
        )
        val dir = dirWith(withExtras)
        val store = open(dir)
        assertEquals(PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 5, 5), store.progress(PuzzleId("ok")))
        store.saveSettings(GameSettings(timerShown = false, soundOn = false))
        assertNull(store.movedAside)
        assertNull(store.partialCopy)
        assertEquals(emptyList<File>(), asides(dir, "progress-partial"))
    }

    @Test
    fun inBoundRationalExtremesAreReadAsWritten() {
        // decision DA-63: |n| <= 65536 and d in {1,2,4,...,64}, normalized, are valid.
        val at = """{"x":{"a":"65535/64","b":"-65536"},"y":{"a":"1/64","b":"-1/64"}}"""
        val store = open(dirWith(docWithEntry(entry(pieces = """{"MT": ${board(at = at)}}"""))))
        val read = store.progress(PuzzleId("bad")).pieces[PieceId.MT] as? PieceSave.OnBoard
            ?: error("an in-bound entry must load, not read as New")
        assertEquals(
            ExactPoint(Q2(Rational.of(65535, 64), Rational.of(-65536)), Q2(Rational.of(1, 64), Rational.of(-1, 64))),
            read.at,
        )
        assertNull(store.partialCopy)
    }

    // ---------------------------------------------------------------- whole document bad: moved aside

    @Test
    fun anUnreadableDocumentIsMovedAsideWholeAndTheGameStartsFresh() {
        // design section 2 reader table row 2: empty, not JSON, not an object, no integer `version`, version < 1.
        val bad = listOf(
            "",
            "   \n",
            "not json at all",
            "[]",
            "42",
            "{}",
            """{"puzzles":{}}""",
            """{"version":null,"lastShown":"ok"}""",
            """{"version":0,"lastShown":"ok","puzzles":{"ok":$okSibling}}""",
            """{"version":-3,"lastShown":"ok","puzzles":{"ok":$okSibling}}""",
            """{"version":1.5,"lastShown":"ok","puzzles":{"ok":$okSibling}}""",
            """{"version":1,"lastShown":"ok","puzzles":{"ok":$okSibling}""", // truncated
        )
        for (text in bad) {
            val dir = dirWith(text)
            val store = open(dir)
            assertFresh(store)
            val aside = store.movedAside ?: error("not moved aside: <$text>")
            assertEquals("progress-bad-$now.json", aside.name)
            assertEquals("the aside copy is the original bytes: <$text>", text, aside.readText(Charsets.UTF_8))
            assertNull("a whole-document move leaves no partial copy: <$text>", store.partialCopy)
            assertFalse(store.asideFailed)
        }
    }

    @Test
    fun aNewerVersionIsMovedAsideUnderItsVersionNameAndNeverOverwritten() {
        // design section 2 reader table row 3; guardrail G-09 "never overwritten".
        for (v in listOf(2, 7, 99)) {
            val original = doc(version = "$v", puzzles = """{"ok":$okSibling}""", lastShown = "\"ok\"")
            val dir = dirWith(original)
            val store = open(dir)
            assertFresh(store)
            val aside = store.movedAside ?: error("version $v was not moved aside")
            assertEquals("progress-v$v-$now.json", aside.name)
            assertEquals(original, aside.readText(Charsets.UTF_8))

            // the game now plays and saves: the newer document stays untouched
            store.saveSettings(GameSettings(timerShown = true, soundOn = true))
            assertEquals(original, aside.readText(Charsets.UTF_8))
            val reopened = open(dir)
            assertEquals(GameSettings(timerShown = true, soundOn = true), reopened.settings())
            assertEquals(PuzzleProgress.NEW, reopened.progress(PuzzleId("ok")))
        }
    }

    // ---------------------------------------------------------------- a bad section: that section alone is fresh

    @Test
    fun aBadSettingsSectionReadsAsDefaultsAndTheOthersLoad() {
        // design section 2 reader table (rev 2, E4): the section alone reads as its fresh-install value.
        for (settings in listOf("5", "null", "[]", """{"timerShown":"yes","soundOn":false}""", """{"timerShown":true,"soundOn":1}""")) {
            val store = open(dirWith(doc(puzzles = """{"ok":$okSibling}""", lastShown = "\"ok\"", settings = settings)))
            assertEquals("settings <$settings>", GameSettings(), store.settings())
            assertEquals(PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 5, 5), store.progress(PuzzleId("ok")))
            assertEquals(PuzzleId("ok"), store.lastShownPuzzle())
            assertEquals(PlayTime(LocalDate.of(2026, 10, 3), 340, 5120), store.playTime())
            assertNull("a bad section is not a whole-document move", store.movedAside)
        }
    }

    @Test
    fun aBadPlayTimeSectionReadsAsNoneAndTheOthersLoad() {
        val bad = listOf(
            "7",
            """{"day":"2026-13-45","todaySeconds":1,"totalSeconds":2}""",
            """{"day":"yesterday","todaySeconds":1,"totalSeconds":2}""",
            """{"day":"2026-10-03","todaySeconds":-1,"totalSeconds":2}""",
        )
        for (pt in bad) {
            val store = open(dirWith(doc(puzzles = """{"ok":$okSibling}""", playTime = pt)))
            assertEquals("playTime <$pt>", PlayTime.NONE, store.playTime())
            assertEquals(GameSettings(timerShown = true, soundOn = false), store.settings())
            assertEquals(PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 5, 5), store.progress(PuzzleId("ok")))
            assertNull(store.movedAside)
        }
    }

    @Test
    fun aBadPuzzlesSectionMakesEveryPuzzleNewAndTheOthersLoad() {
        for (puzzles in listOf("[]", "5", "null", "\"x\"")) {
            val store = open(dirWith(doc(puzzles = puzzles, lastShown = "\"ok\"")))
            assertEquals(PuzzleProgress.NEW, store.progress(PuzzleId("ok")))
            assertEquals(PuzzleId("ok"), store.lastShownPuzzle())
            assertEquals(GameSettings(timerShown = true, soundOn = false), store.settings())
            assertNull(store.movedAside)
        }
    }

    @Test
    fun aBadLastShownSectionReadsAsNullAndTheOthersLoad() {
        for (ls in listOf("5", "true", "[]", "{}")) {
            val store = open(dirWith(doc(puzzles = """{"ok":$okSibling}""", lastShown = ls)))
            assertNull("lastShown <$ls>", store.lastShownPuzzle())
            assertEquals(PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 5, 5), store.progress(PuzzleId("ok")))
            assertNull(store.movedAside)
        }
    }

    @Test
    fun aMissingSectionReadsAsFreshAndTheOthersLoad() {
        val noSettings = """{"version":1,"lastShown":"ok","puzzles":{"ok":$okSibling},"playTime":$okPlayTime}"""
        val store = open(dirWith(noSettings))
        assertEquals(GameSettings(), store.settings())
        assertEquals(PuzzleId("ok"), store.lastShownPuzzle())
        assertEquals(PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 5, 5), store.progress(PuzzleId("ok")))
    }

    @Test
    fun aBadSectionCopiesTheOriginalOnceBeforeTheFirstWriteOfTheRun() {
        // design section 2 reader table: "copied once to progress-partial-<nowMs>.json before the first write of the run".
        val original = doc(puzzles = """{"ok":$okSibling}""", settings = "5")
        val dir = dirWith(original)
        var clock = 5000L
        val store = open(dir) { clock++ }
        store.saveLastShownPuzzle(PuzzleId("ok")) // the first write of the run
        val copy = store.partialCopy ?: error("no partial copy after the first write over a damaged section")
        assertTrue(copy.name.startsWith("progress-partial-"))
        assertTrue(copy.name.endsWith(".json"))
        assertEquals(original, copy.readText(Charsets.UTF_8))
        store.saveLastShownPuzzle(PuzzleId("other"))
        store.saveSettings(GameSettings(timerShown = true, soundOn = true))
        assertEquals("copied once, not once per write", 1, asides(dir, "progress-partial-").size)
        assertEquals(original, asides(dir, "progress-partial-").single().readText(Charsets.UTF_8))
    }

    // ---------------------------------------------------------------- a bad entry: that puzzle is New

    private fun assertEntryReadsAsNew(label: String, entryJson: String) {
        val store = open(dirWith(docWithEntry(entryJson)))
        // distinguishing fixture: the entry also carries puzzleSeconds 99, which a salvaging reader would show
        assertEquals("entry <$label> must read as New", PuzzleProgress.NEW, store.progress(PuzzleId("bad")))
        assertEquals("the sibling loads: <$label>", PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 5, 5), store.progress(PuzzleId("ok")))
        assertEquals(PuzzleId("ok"), store.lastShownPuzzle())
        assertEquals(GameSettings(timerShown = true, soundOn = false), store.settings())
        assertNull("a bad entry is not a whole-document move: <$label>", store.movedAside)
    }

    private fun atOf(xa: String, xb: String = "0", ya: String = "1", yb: String = "0") =
        """{"x":{"a":"$xa","b":"$xb"},"y":{"a":"$ya","b":"$yb"}}"""

    private fun entryWithBoardMt(at: String) = entry(pieces = """{"MT": ${board(at = at)}}""")

    @Test
    fun anEntryWithABadStateOrPieceReadsAsNew() {
        // design section 2 reader table: bad state string, unknown piece id, bad where/turn/at.
        assertEntryReadsAsNew("state", entry(state = "paused"))
        assertEntryReadsAsNew("state upper", entry(state = "SOLVED"))
        assertEntryReadsAsNew("unknown piece", entry(pieces = """{"XX": ${tray()}}"""))
        assertEntryReadsAsNew("where", entry(pieces = """{"MT": {"where":"shelf","turn":0,"mirrored":false}}"""))
        assertEntryReadsAsNew("turn 8", entry(pieces = """{"MT": ${tray(turn = 8)}}"""))
        assertEntryReadsAsNew("turn -1", entry(pieces = """{"MT": ${tray(turn = -1)}}"""))
        assertEntryReadsAsNew("mirrored not boolean", entry(pieces = """{"MT": {"where":"tray","turn":0,"mirrored":"yes"}}"""))
        assertEntryReadsAsNew("board without at", entry(pieces = """{"MT": {"where":"board","turn":0,"mirrored":false}}"""))
        assertEntryReadsAsNew("at not an object", entryWithBoardMt("5"))
        assertEntryReadsAsNew("pieces not an object", entry(pieces = "[]"))
        assertEntryReadsAsNew("negative puzzleSeconds", entry(puzzleSeconds = "-1"))
        assertEntryReadsAsNew("negative bestSeconds", entry(bestSeconds = "-5"))
    }

    @Test
    fun aPartlyValidEntryIsNeverPartiallyApplied() {
        // design section 2: "the document is never partially applied to a puzzle".
        assertEntryReadsAsNew("one good piece one bad", entry(pieces = """{"MT": ${board()}, "ST1": ${tray(turn = 9)}}"""))
    }

    @Test
    fun anEntryWithABadRationalReadsAsNew() {
        // design section 2 encodings: normalized "n" or "n/d" matched by -?\d+(/\d+)?; decision DA-63 bound; DA-68 (5).
        val badRationals = listOf(
            "2/4" to "not normalized",
            "3/1" to "n/1 is not normalized (DA-68)",
            "1/0" to "denominator 0",
            "1/63" to "d = 63 (DA-63)",
            "1/3" to "d = 3, not dyadic",
            "1/128" to "d = 128, above the bound",
            "65537" to "|n| above the bound",
            "-65537" to "|n| above the bound, negative",
            "4611686018427387904" to "2^62 (F2)",
            "99999999999999999999" to "beyond Long",
            "+1" to "plus sign",
            "1.5" to "decimal",
            "" to "empty",
            "abc" to "not a number",
            "1/-2" to "negative denominator",
            " 1" to "space",
        )
        for ((r, why) in badRationals) {
            assertEntryReadsAsNew("x.a = <$r> ($why)", entryWithBoardMt(atOf(xa = r)))
            assertEntryReadsAsNew("y.b = <$r> ($why)", entryWithBoardMt(atOf(xa = "1", yb = r)))
        }
    }

    @Test
    fun aRationalWithAMissingPartIsEntryDamage() {
        // design section 2: Q2 is {"a":R,"b":R} and `at` is {"x":Q2,"y":Q2}; a missing part cannot be read.
        assertEntryReadsAsNew("missing b", entryWithBoardMt("""{"x":{"a":"2"},"y":{"a":"1","b":"0"}}"""))
        assertEntryReadsAsNew("missing y", entryWithBoardMt("""{"x":{"a":"2","b":"0"}}"""))
    }

    @Test
    fun aDamagedEntryCopiesTheOriginalOnceBeforeTheFirstWrite() {
        // design section 2 (rev 1): a damaged or wrongly rejected entry is never lost without a copy.
        val original = docWithEntry(entry(state = "paused"))
        val dir = dirWith(original)
        var clock = 9000L
        val store = open(dir) { clock++ }
        store.saveLastShownPuzzle(PuzzleId("bad")) // first write
        val copy = store.partialCopy ?: error("no partial copy after the first write over a damaged entry")
        assertEquals(original, copy.readText(Charsets.UTF_8))
        store.saveLastShownPuzzle(PuzzleId("ok"))
        assertEquals(1, asides(dir, "progress-partial-").size)
        // a different later load of the rewritten file is clean: the bad entry is gone, the sibling stays
        val reopened = open(dir)
        assertEquals(PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 5, 5), reopened.progress(PuzzleId("ok")))
        assertNull(reopened.partialCopy)
    }

    @Test
    fun aValidDocumentNeverMakesAPartialCopy() {
        val dir = dirWith(doc(puzzles = """{"ok":$okSibling}"""))
        val store = open(dir)
        store.saveSettings(GameSettings(timerShown = false, soundOn = true))
        assertNull(store.partialCopy)
        assertNull(store.movedAside)
        assertEquals(emptyList<File>(), asides(dir, "progress-partial-"))
        assertEquals(emptyList<File>(), asides(dir, "progress-bad-"))
    }

    // ---------------------------------------------------------------- aside failures and pruning

    @Test
    fun whenMovingAsideAndCopyingBothFailTheStoreOverwritesAndSaysSo() {
        // design section 2 reader table (rev 1 F4, rev 2 E3), decision DA-64: asideFailed is set and later saves
        // overwrite ("stored before it returns" wins). The aside name is occupied by a non-empty directory, so
        // neither a move nor a copy to that name can succeed.
        val dir = dirWith("garbage that is not JSON")
        val blocker = File(dir, "progress-bad-$now.json")
        assertTrue(blocker.mkdir())
        File(blocker, "keep.txt").writeText("x")

        val store = open(dir)
        assertTrue("asideFailed must be set when move and copy both fail", store.asideFailed)
        assertNull(store.movedAside)
        assertFresh(store)

        store.saveSettings(GameSettings(timerShown = true, soundOn = false))
        val reopened = open(dir) // what a new process sees
        assertEquals(GameSettings(timerShown = true, soundOn = false), reopened.settings())
        assertNull("the overwritten file is a valid v1 document now", reopened.movedAside)
    }

    @Test
    fun onlyTheNewestThreeAsideFilesAreKept() {
        // design section 2 (rev 1, N9): after any move-aside the store keeps only the newest 3 files matching
        // progress-bad-*, progress-v*, progress-partial-*. All files here are one family whose name stamp and
        // modification time agree, so "newest" is unambiguous.
        val dir = tmp.newFolder()
        for (stamp in listOf(1000L, 2000L, 3000L, 4000L, 5000L)) {
            val f = File(dir, "progress-bad-$stamp.json")
            f.writeText("old $stamp")
            assertTrue(f.setLastModified(stamp))
        }
        File(dir, "notes.txt").writeText("unrelated")
        File(dir, JsonProgressStore.FILE_NAME).writeText("garbage")

        val store = open(dir) { 9000L }
        assertEquals("progress-bad-9000.json", store.movedAside?.name)
        assertEquals(
            listOf("progress-bad-4000.json", "progress-bad-5000.json", "progress-bad-9000.json"),
            asides(dir, "progress-bad-").map { it.name },
        )
        assertTrue("an unrelated file is never pruned", File(dir, "notes.txt").isFile)
    }

    @Test
    fun theStoreNeverThrowsOnAnyDamageAndSyncIsSafeAfterwards() {
        // guardrail G-10: stored data never crashes; design section 2: sync() never throws.
        for (text in listOf("", "{", "\u0000\u0001", "[1,2,", doc(version = "3"), docWithEntry("null"))) {
            val dir = dirWith(text)
            val store = open(dir)
            store.progress(PuzzleId("bad"))
            store.sync()
        }
        assertNotNull(open(tmp.newFolder()))
    }
}

private const val AT_OK = """{"x":{"a":"2","b":"0"},"y":{"a":"1","b":"0"}}"""
