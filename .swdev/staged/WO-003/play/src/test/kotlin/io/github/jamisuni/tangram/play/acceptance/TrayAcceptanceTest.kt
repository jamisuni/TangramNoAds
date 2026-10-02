package io.github.jamisuni.tangram.play.acceptance

import io.github.jamisuni.tangram.contracts.puzzle.PuzzleKind
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PieceShape
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** REQ-012 A1, REQ-013 A1-A3 (+ its cell rule), REQ-016 A1: the tray at engine level. */
class TrayAcceptanceTest {

    /**
     * TYPE-001 ASSUMPTION: "the resting turn in the tray is long side down and pointing up for every triangle, upright for
     * the square, and leaning right for the parallelogram" - tested as the shape of the drawn polygon, not as a number.
     */
    private fun restingShapeOk(piece: PieceId, turn: Turn, mirrored: Boolean): Boolean {
        if (mirrored) return false
        val o = PieceGeometry.offsets(piece.shape, turn, false).map(::v)
        val maxY = o.maxOf { it.y }
        val minY = o.minOf { it.y }
        val bottom = o.filter { abs(it.y - maxY) < 1e-9 }
        val top = o.filter { abs(it.y - minY) < 1e-9 }
        return when (piece.shape) {
            PieceShape.LARGE_TRIANGLE, PieceShape.MEDIUM_TRIANGLE, PieceShape.SMALL_TRIANGLE ->
                bottom.size == 2 && top.size == 1
            PieceShape.SQUARE -> bottom.size == 2 && top.size == 2
            PieceShape.PARALLELOGRAM ->
                bottom.size == 2 && top.size == 2 && top.minOf { it.x } > bottom.minOf { it.x } + 1e-9
        }
    }

    private val fullSetPuzzles get() = PUZZLES.filter { it.kind != PuzzleKind.MINI }

    // REQ-012.A1 - "Two different full-set puzzles show identical trays at their start."
    @Test
    fun req012_A1_twoFullSetPuzzlesShowIdenticalTrays() {
        val a = puzzle("shapes-square")
        val b = puzzle("animals-cat")
        val da = Driver(a)
        val db = Driver(b)
        assertEquals(PieceId.entries, da.session.pieces.map { it.piece }) // TYPE-001 tray order
        assertEquals(da.session.pieces.map { it.piece }, db.session.pieces.map { it.piece })
        for (p in PieceId.entries) {
            val sa = da.session.ps(p)
            val sb = db.session.ps(p)
            assertEquals("turn of $p", sa.turn, sb.turn)
            assertEquals("mirror of $p", sa.mirrored, sb.mirrored)
            assertEquals("$p in the tray", "Tray", da.session.where(p))
            assertEquals("$p in the tray", "Tray", db.session.where(p))
            assertTrue("$p rests as TYPE-001 says", restingShapeOk(p, sa.turn, sa.mirrored))
            assertEquals("cell of $p", da.layout.cellR(p), db.layout.cellR(p))
        }
        // and with every other full-set puzzle: the same start
        for (q in fullSetPuzzles) {
            val dq = Driver(q)
            for (p in PieceId.entries) {
                assertEquals("${q.id.value} $p", da.session.ps(p).turn, dq.session.ps(p).turn)
                assertEquals("${q.id.value} $p cell", da.layout.cellR(p), dq.layout.cellR(p))
            }
        }
    }

    // REQ-013.A1 - "On a 360 x 780 dp phone the smallest tray cell is at least 56 dp in both directions."
    @Test
    fun req013_A1_smallestTrayCellIsAtLeast56dpOnA360x780Phone() {
        for (areaH in listOf(640.0, 700.0, 740.0)) { // the play area is the screen minus bars and the title bar
            for (p in listOf(puzzle("shapes-mini-1"), puzzle("shapes-square"), puzzle("shapes-warmup-1"))) {
                val layout = layoutFor(p, 360.0, areaH, 780.0)
                for (piece in p.solution.map { it.piece }) {
                    val c = layout.cellR(piece)
                    assertTrue("${p.id.value} $piece cell width ${c.w} (area height $areaH)", c.w >= 56.0 - 1e-6)
                    assertTrue("${p.id.value} $piece cell height ${c.h} (area height $areaH)", c.h >= 56.0 - 1e-6)
                }
            }
        }
    }

    // REQ-013 rule (no acceptance ID): "Cell width = the piece's turn diameter x scale + 12 dp padding; turn diameters in
    // units: large triangle 4.22, parallelogram 3.16, medium triangle 2.98, small triangle 2.11, square 2.00."
    @Test
    fun req013_rule_cellWidthIsTurnDiameterTimesScalePlus12() {
        val diameters = mapOf(
            PieceShape.LARGE_TRIANGLE to 4.22, PieceShape.PARALLELOGRAM to 3.16, PieceShape.MEDIUM_TRIANGLE to 2.98,
            PieceShape.SMALL_TRIANGLE to 2.11, PieceShape.SQUARE to 2.00,
        )
        val p = puzzle("shapes-square")
        val layout = layoutFor(p)
        for (piece in PieceId.entries) {
            val expected = diameters.getValue(piece.shape) * layout.trayScale + 12.0
            // the table is rounded to 0.005 units, so allow 0.005 x scale
            assertEquals("cell width of $piece", expected, layout.cellR(piece).w, 0.005 * layout.trayScale + 1e-6)
        }
    }

    // REQ-013.A2 - "Turning a piece in the tray never changes any cell size."
    @Test
    fun req013_A2_turningAPieceInTheTrayNeverChangesAnyCellSize() {
        val p = puzzle("shapes-square")
        val d = Driver(p)
        val before = PieceId.entries.associateWith { d.layout.cellR(it) }
        for (piece in PieceId.entries) {
            repeat(8) {
                d.tap(pressPoint(d.layout.cellR(piece)))
                for (q in PieceId.entries) {
                    assertEquals("cell of $q after turning $piece", before.getValue(q), d.layout.cellR(q))
                    assertEquals("cell of $q after turning $piece (fresh layout)", before.getValue(q), layoutFor(p).cellR(q))
                }
            }
        }
    }

    // REQ-013.A3 - "The same piece has the same miniature size in a mini puzzle and in a full-set puzzle."
    @Test
    fun req013_A3_samePieceSameMiniatureSizeInMiniAndFullPuzzle() {
        val mini = layoutFor(puzzle("shapes-mini-1"))
        for (full in fullSetPuzzles) {
            val l = layoutFor(full)
            assertEquals("trayScale ${full.id.value}", mini.trayScale, l.trayScale, 1e-9)
            for (piece in listOf(PieceId.SQ, PieceId.ST1, PieceId.ST2)) { // the pieces of shapes-mini-1
                val a = mini.cellR(piece)
                val b = l.cellR(piece)
                assertEquals("width of $piece vs ${full.id.value}", a.w, b.w, 1e-9)
                assertEquals("height of $piece vs ${full.id.value}", a.h, b.h, 1e-9)
            }
        }
    }

    // REQ-016.A1 - "Eight taps return a tray piece to its starting turn."  (and each tap is one clockwise 45 degree step:
    // REQ-016 statement, TYPE-003 clockwise; REQ-015: a touch that moves less than 12 dp is a tap)
    @Test
    fun req016_A1_eightTapsReturnATrayPieceToItsStartingTurn() {
        for (id in listOf("shapes-square", "shapes-mini-1")) {
            val d = Driver(puzzle(id))
            for (piece in d.session.pieces.map { it.piece }) {
                val start = d.session.ps(piece).turn
                for (k in 1..8) {
                    d.tap(pressPoint(d.layout.cellR(piece)))
                    assertEquals("$piece after $k taps", Turn((start.steps + k) % 8), d.session.ps(piece).turn)
                    assertEquals("$piece stays in the tray", "Tray", d.session.where(piece))
                    assertEquals("$piece stays unmirrored", false, d.session.ps(piece).mirrored)
                }
                assertEquals(start, d.session.ps(piece).turn)
            }
            // TYPE-006: "Turning a piece in the tray ... does not start" the puzzle.
            assertEquals(PuzzleState.NEW, d.session.state)
        }
    }
}
