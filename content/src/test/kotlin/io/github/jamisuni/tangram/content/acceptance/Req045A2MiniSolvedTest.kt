package io.github.jamisuni.tangram.content.acceptance

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// HELD-OUT (REQ-045.A2). Goes only to Test & Verify.
// Engine level (design Test seams; WO-002 Scope): the kernel has no "solved" check yet (REQ-022 is WO-003), so
// "placing those three pieces" is read as: the mini's pieces lock one after the other through the kernel at their
// stored places and together fill the stored silhouette. The on-screen solved picture is WO-003.
class Req045A2MiniSolvedTest {

    // REQ-045.A2 -- "Placing those three pieces shows the solved picture."
    @Test
    fun theThreePiecesLockAndFillTheSilhouette() { // REQ-045.A2
        val minis = PuzzleLibrary.packaged().puzzles.filter { it.kind == PuzzleKind.MINI }
        assertTrue("at least one mini (Statement)", minis.isNotEmpty())
        for (m in minis) {
            val g = AcceptanceGolden.puzzles.getValue(m.id.value)
            assertEquals("golden agrees it is a mini", "mini", g.kind)
            val placed = lockInOrder(m, g.buildOrder)
            // "those three pieces" = the tray's pieces (Puzzle.solution doc; Statement "only that puzzle's pieces")
            assertEquals(m.solution.map { it.piece }.toSet(), placed.map { it.piece }.toSet())
            assertEquals(m.solution.size, placed.size)
            // together they cover exactly the stored solution: each lies on its stored polygon
            for (pl in placed) assertEquals(m.solution.first { it.piece == pl.piece }.polygon.toSet(), pl.corners.toSet())
        }
    }

    // REQ-045.A2 / REQ-045 Rules: "a solved picture" -- a mini carries its picture like any puzzle (REQ-039): base colour and
    // every file shape kept (counted from the raw file).
    @Test
    fun theMiniCarriesItsSolvedPicture() { // REQ-045.A2
        val minis = PuzzleLibrary.packaged().puzzles.filter { it.kind == PuzzleKind.MINI }
        assertTrue(minis.isNotEmpty())
        for (m in minis) {
            val raw = Fx.raw(m.id.value)
            val shapes = (raw.getValue("art") as kotlinx.serialization.json.JsonObject).getValue("shapes") as kotlinx.serialization.json.JsonArray
            assertTrue("${m.id.value} has a picture with shapes", shapes.size > 0)
            assertEquals(shapes.size, m.picture.shapes.size)
        }
    }

    // REQ-045 Statement: "exactly three pieces" and "the tray SHALL show only that puzzle's pieces": the FIRST mini has three
    // distinct pieces, so a mini is not listed with all seven in the tray.
    @Test
    fun firstMiniTrayHoldsOnlyItsThreePieces() { // REQ-045.A2
        val first = PuzzleLibrary.packaged().puzzles.first()
        assertEquals(PuzzleKind.MINI, first.kind)
        val tray = first.solution.map { it.piece }.toSet()
        assertEquals(3, tray.size)
        val g = AcceptanceGolden.puzzles.getValue(first.id.value)
        assertEquals("the build order places exactly the tray pieces", tray, g.buildOrder.toSet())
    }
}
