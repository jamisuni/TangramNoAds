package io.github.jamisuni.tangram.content.acceptance

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleKind
import java.util.Random
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Req007LibraryTest {

    // REQ-007.A1 -- "Every shipped puzzle meets REQ-038, REQ-039 and REQ-040."
    @Test
    fun everyShippedPuzzleMeetsReq038Req039AndReq040() {
        val lib = PuzzleLibrary.packaged()
        val golden = AcceptanceGolden.puzzles
        val stems = Fx.shippedFiles().map { it.name.removeSuffix(".json") }

        // "every shipped puzzle": no file of Tangrams/ is silently left out, and nothing else
        // (the schema, README, previews) is listed as a puzzle.
        assertEquals("a puzzle per file, no extras", stems.sorted(), lib.puzzles.map { it.id.value }.sorted())
        assertEquals("no duplicate ids", stems.size, lib.puzzles.size)

        for (p in lib.puzzles) {
            val id = p.id.value
            // REQ-038: passes the validator (decision F15: the G1-baseline validator is the reference; golden verdict).
            val g = checkNotNull(golden[id]) { "$id is not in the golden: ${AcceptanceGolden.REFRESH_HINT}" }
            assertEquals(emptyList<String>(), AcceptanceGolden.verdictProblems(g))
            // REQ-039: a picture exists for every puzzle, with its shapes kept (shape count counted from the raw file).
            val rawShapes = Fx.raw(id).getValue("art").jsonObject.getValue("shapes").jsonArray.size
            assertEquals("$id: picture shapes kept", rawShapes, p.picture.shapes.size)
            // REQ-040: Rules: "Rating: an integer from 1 to 5"; cross-check against the Python reading.
            assertTrue("$id rating ${p.rating}", p.rating in 1..5)
            assertEquals("$id rating vs golden difficulty", g.difficulty, p.rating)
        }
    }

    // REQ-007.A2 -- "The first puzzles in the list are the mini puzzles (REQ-045), then the warm-ups (REQ-041)."
    @Test
    fun listStartsWithMiniPuzzlesThenWarmups() {
        assertStartsMiniThenWarmups(PuzzleLibrary.packaged().puzzles.map { it.kind })
    }

    // REQ-007.A2 + IPuzzleLibrary.puzzles ("in list order: by kind"): the order must not depend on the order the files are read.
    @Test
    fun listOrderDoesNotDependOnInputOrder() {
        val files = Fx.shippedFiles()
        val reference = PuzzleLibrary(files).puzzles.map { it.id.value }
        val orders = listOf(files.reversed()) + (1..6).map { files.shuffled(Random(it.toLong())) }
        for (shuffled in orders) {
            val lib = PuzzleLibrary(shuffled)
            assertEquals(reference, lib.puzzles.map { it.id.value })
            assertStartsMiniThenWarmups(lib.puzzles.map { it.kind })
        }
    }

    private fun assertStartsMiniThenWarmups(kinds: List<PuzzleKind>) {
        assertTrue("at least one mini first (REQ-045)", kinds.first() == PuzzleKind.MINI)
        val firstNonMini = kinds.indexOfFirst { it != PuzzleKind.MINI }
        assertTrue(firstNonMini > 0)
        val afterMinis = kinds.drop(firstNonMini)
        assertTrue("warm-ups follow the minis (REQ-041)", afterMinis.first() == PuzzleKind.WARMUP)
        assertTrue("at least four warm-ups (REQ-041)", kinds.count { it == PuzzleKind.WARMUP } >= 4)
        // REQ-040: kind (mini, then warm-up, then full): no earlier kind after a later one.
        assertEquals(kinds.sortedBy { it.ordinal }, kinds)
    }
}
