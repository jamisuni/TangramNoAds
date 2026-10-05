package io.github.jamisuni.tangram.content.release

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.content.acceptance.Fx
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleCategory
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// WO-009 T9c (independent author), always on, in tree: REQ-042 A2 and A3 on the REAL packaged library (the files of Tangrams/ as the
// game ships them), never on a fixture. Written from REQ-042 and the frozen design only.
// The two token-carrying tests are the only ones that carry a token. The `control_*` tests are fixture controls with no token: they
// show that the predicate the real tests use can tell a library that meets the criterion from one that does not (a check that cannot
// fail proves nothing). Inputs: the packaged library and, for the controls, the shipped files through the acceptance fixture `Fx`.
class ReleaseLibraryTest {

    /** The themes a library covers: its distinct categories (REQ-042 Rules: "at least four of the categories in Spec/03"). */
    private fun themes(puzzles: List<Puzzle>): Set<PuzzleCategory> = puzzles.map { it.category }.toSet()

    private fun hasSquare(lib: PuzzleLibrary): Boolean = lib.puzzle(PuzzleId("shapes-square")) != null

    // REQ-042.A2 - "At least four categories are present."
    // Counted over the puzzles the library really holds (a file the parser rejects is not in the release), mini and warm-up puzzles
    // included (REQ-042 Rules: they count toward the 20).
    @Test
    fun req042_A2_atLeastFourCategoriesArePresentInTheRealLibrary() {
        val lib = PuzzleLibrary.packaged()
        assertTrue("fixture: the real library is not empty", lib.puzzles.isNotEmpty())
        val themes = themes(lib.puzzles)
        assertTrue("the real library covers ${themes.size} categories ($themes), REQ-042 needs at least four", themes.size >= 4)
    }

    // REQ-042.A3 - "shapes-square is among them."
    @Test
    fun req042_A3_shapesSquareIsAmongTheRealLibrarysPuzzles() {
        val lib = PuzzleLibrary.packaged()
        assertTrue("fixture: the real library is not empty", lib.puzzles.isNotEmpty())
        assertNotNull("shapes-square is not in the real library: ${lib.puzzles.map { it.id.value }}", lib.puzzle(PuzzleId("shapes-square")))
        assertTrue("shapes-square is listed in puzzles", lib.puzzles.any { it.id.value == "shapes-square" })
    }

    // ---- fixture controls (no token): the predicates above can fail --------------------------------------------------------------

    @Test
    fun control_aLibraryOfThreeCategoriesDoesNotCountAsFour() {
        val files = Fx.shippedFiles()
        val full = PuzzleLibrary(files)
        assertTrue("fixture: the shipped files cover at least four categories", themes(full.puzzles).size >= 4)
        // keep only the files of three of its categories
        val keep = themes(full.puzzles).sortedBy { it.name }.take(3).toSet()
        val three = PuzzleLibrary(files.filter { f -> full.puzzles.first { it.id.value == f.name.removeSuffix(".json") }.category in keep })
        assertEquals(3, themes(three.puzzles).size)
        assertFalse("three categories are not four", themes(three.puzzles).size >= 4)
    }

    @Test
    fun control_aLibraryWithoutTheSquareFileLacksIt() {
        val files = Fx.shippedFiles()
        assertTrue("fixture: the shipped files include shapes-square", hasSquare(PuzzleLibrary(files)))
        assertFalse(hasSquare(PuzzleLibrary(files.filter { it.name != "shapes-square.json" })))
    }

    @Test
    fun control_aSquareFileThatTheParserRejectsIsNotInTheLibrary() {
        // A file named shapes-square that cannot be read is left out of the release, so it must not satisfy the criterion.
        val files = Fx.shippedFiles().filter { it.name != "shapes-square.json" } +
            io.github.jamisuni.tangram.content.PuzzleFile("shapes-square.json", "{ not json")
        assertNull(PuzzleLibrary(files).puzzle(PuzzleId("shapes-square")))
    }
}
