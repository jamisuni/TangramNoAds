package io.github.jamisuni.tangram.content.acceptance

import io.github.jamisuni.tangram.content.PuzzleFile
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

// Tests of AI design decisions and frozen-contract sentences. They carry NO acceptance token on purpose.
class ContractAndDecisionTest {

    private val real get() = Fx.shippedFiles()

    // contract IPuzzleLibrary.puzzle: "The puzzle with this id, or null when no shipped puzzle has it"
    @Test
    fun lookupByIdReturnsThePuzzleOrNull() {
        val lib = PuzzleLibrary.packaged()
        for (p in lib.puzzles) assertEquals(p, lib.puzzle(p.id))
        assertNull(lib.puzzle(PuzzleId("no-such-puzzle")))
        assertNull("the schema file is not a puzzle", lib.puzzle(PuzzleId("puzzle.schema")))
        assertNull(lib.puzzle(PuzzleId("puzzle")))
    }

    // contract Puzzle.kt: kind "(default `full`)"
    @Test
    fun missingKindMeansFull() {
        // cat is `full`; a warm-up file without `kind` must not stay a warm-up, a full file must be FULL.
        val noKindFull = Fx.variant(Fx.shipped("animals-cat"), "animals-cat", dropKind = true)
        val noKindWarmup = Fx.variant(Fx.shipped("shapes-warmup-1"), "shapes-warmup-1", dropKind = true)
        val lib = PuzzleLibrary(real.filter { it.name != "animals-cat.json" && it.name != "shapes-warmup-1.json" } + noKindFull + noKindWarmup)
        assertEquals(PuzzleKind.FULL, lib.puzzle(PuzzleId("animals-cat"))?.kind)
        assertEquals(PuzzleKind.FULL, lib.puzzle(PuzzleId("shapes-warmup-1"))?.kind)
    }

    // contract Puzzle.reviewedByHuman: "a missing flag counts as false"; decision F16 (REQ-039 acceptance 2 = true only for a reviewed puzzle)
    @Test
    fun reviewedFlagMissingOrNotABooleanTrueIsFalse() {
        val base = Fx.shipped("animals-cat")
        val missing = Fx.withReviewed(base, "animals-cat", null)
        val stringTrue = Fx.withReviewed(base, "animals-cat", "\"true\"")
        val others = real.filter { it.name != "animals-cat.json" }
        for (f in listOf(missing, stringTrue)) {
            val p = PuzzleLibrary(others + f).puzzle(PuzzleId("animals-cat"))
            // either not listed or false; it must never read as reviewed
            assertTrue(p == null || !p.reviewedByHuman)
        }
        assertNotNull("a missing flag is readable, not a rejection", PuzzleLibrary(others + missing).puzzle(PuzzleId("animals-cat")))
        assertFalse(PuzzleLibrary(others + missing).puzzle(PuzzleId("animals-cat"))!!.reviewedByHuman)
    }

    // decision F16 / contract: the flag is the file's `provenance.reviewedByHuman`; `true` reads as true, `false` as false
    @Test
    fun reviewedFlagIsReadFromTheFile() {
        val base = Fx.shipped("animals-cat")
        val others = real.filter { it.name != "animals-cat.json" }
        val yes = PuzzleLibrary(others + Fx.withReviewed(base, "animals-cat", "true")).puzzle(PuzzleId("animals-cat"))
        assertEquals(true, yes?.reviewedByHuman)
        val no = PuzzleLibrary(others + base).puzzle(PuzzleId("animals-cat"))
        assertEquals(false, no?.reviewedByHuman)
    }

    // contract IPuzzleLibrary.puzzles + architecture G-10: "a file that still cannot be read at run time is left out of the list rather than stopping the game"
    @Test
    fun unreadableFileIsLeftOutAndTheRestKept() {
        val junk = listOf(
            PuzzleFile("broken-json.json", "{ this is not json"),
            PuzzleFile("empty-file.json", ""),
            PuzzleFile("wrong-format.json", "{\"format\": \"tangram-puzzle/9\", \"id\": \"wrong-format\"}"),
        )
        val lib = PuzzleLibrary(real + junk)
        assertEquals(real.map { it.name.removeSuffix(".json") }.sorted(), lib.puzzles.map { it.id.value }.sorted())
        assertEquals(3, lib.rejected.size)
    }

    // contract PuzzleId: "the file's id, kebab-case, equal to its file name"
    @Test
    fun idDifferingFromFileNameIsNotListed() {
        val cat = Fx.shipped("animals-cat")
        val wrongName = PuzzleFile("animals-dog.json", cat.text) // id inside still animals-cat
        val lib = PuzzleLibrary(real.filter { it.name != "animals-cat.json" } + wrongName)
        assertNull(lib.puzzle(PuzzleId("animals-cat")))
        assertNull(lib.puzzle(PuzzleId("animals-dog")))
    }

    // decision DA-10: "never empty" is a build-time guarantee; an empty or all-invalid package fails loudly
    @Test
    fun emptyOrAllInvalidLibraryFailsLoudly() {
        for (files in listOf<List<PuzzleFile>>(emptyList(), listOf(PuzzleFile("broken.json", "nope")))) {
            try {
                PuzzleLibrary(files)
                fail("a library with no readable puzzle must not be constructed (decision DA-10)")
            } catch (expected: IllegalStateException) {
                // decision DA-10: IllegalStateException
            }
        }
    }

    // decision DA-8 / contract SolutionPiece: nothing the library lists may be a non-tangram; here a file with
    // a duplicated piece (so not "every piece exactly once", REQ-038 statement) is not listed.
    @Test
    fun duplicatePieceFileIsNotListed() {
        val mini = Fx.shipped("shapes-mini-1")
        // turn the ST2 entry into a second ST1 entry: 3 entries, 2 distinct pieces
        val text = mini.text.replace("\"piece\": \"ST2\"", "\"piece\": \"ST1\"")
        val lib = PuzzleLibrary(real.filter { it.name != "shapes-mini-1.json" } + PuzzleFile("shapes-mini-1.json", text))
        assertNull(lib.puzzle(PuzzleId("shapes-mini-1")))
    }
}
