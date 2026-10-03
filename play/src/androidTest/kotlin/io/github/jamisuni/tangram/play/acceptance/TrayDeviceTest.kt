package io.github.jamisuni.tangram.play.acceptance

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.pointOf
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.Turn
import io.github.jamisuni.tangram.play.PlaySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** REQ-043 A1/A2, REQ-012 A1 (colours), REQ-013 A2 (no reflow) and REQ-018 A1 on a device, with injected touches. */
class TrayDeviceTest {

    @get:Rule
    val rule = createComposeRule()

    private fun markNodes(): List<SemanticsNode> =
        rule.onAllNodes(SemanticsMatcher("size mark") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("size-mark-") == true })
            .fetchSemanticsNodes()

    private fun tag(n: SemanticsNode) = n.config.getOrNull(SemanticsProperties.TestTag)!!
    private fun text(n: SemanticsNode) = n.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text } ?: ""

    private fun boundsDp(n: SemanticsNode): R {
        val d = rule.density.density
        val b = n.boundsInRoot
        return R(b.left / d.toDouble(), b.top / d.toDouble(), b.right / d.toDouble(), b.bottom / d.toDouble())
    }

    // REQ-043.A1 - "A full-set tray shows the marks L, L, M, S, S on the five triangles, and none on the square or the parallelogram."
    // (REQ-043 rule: "The mark sits in the top-left corner of the cell")
    @Test
    fun req043_A1_aFullSetTrayShowsLLMSSOnTheTrianglesAndNothingOnSquareOrParallelogram() {
        val p = puzzle("shapes-square")
        rule.showPlay(PlaySession(p, { true }))
        val l = layoutFor(p)
        val marks = markNodes().sortedBy { PieceId.valueOf(tag(it).removePrefix("size-mark-")).ordinal }
        assertEquals(
            listOf(PieceId.LT1, PieceId.LT2, PieceId.MT, PieceId.ST1, PieceId.ST2).map { it.name },
            marks.map { tag(it).removePrefix("size-mark-") },
        )
        assertEquals(listOf("L", "L", "M", "S", "S"), marks.map { text(it).trim() })
        rule.onNodeWithTag("size-mark-SQ").assertDoesNotExist()
        rule.onNodeWithTag("size-mark-PG").assertDoesNotExist()
        for (n in marks) {
            val piece = PieceId.valueOf(tag(n).removePrefix("size-mark-"))
            val cell = l.cellR(piece)
            val b = boundsDp(n)
            assertTrue("mark of $piece sits in the left half of its cell", b.l >= cell.l - 1.0 && b.l <= cell.l + cell.w / 2)
            assertTrue("mark of $piece sits in the upper half of its cell", b.t >= cell.t - 1.0 && b.t <= cell.t + cell.h / 2)
        }
    }

    // REQ-043.A2 - "A placed triangle's cell shows only its dashed outline, without a mark."
    // (REQ-043 rule: the mark "is hidden while the piece is on the board"; REQ-012 rule: "A piece on the board leaves a dashed
    // outline in its tray cell" - so the cell centre is no longer the piece colour.)
    @Test
    fun req043_A2_aPlacedTrianglesCellShowsNoMarkAndNoPiece() {
        val p = puzzle("shapes-square")
        val session = PlaySession(p, { true })
        rule.showPlay(session)
        val l = layoutFor(p)
        val before = rule.shot().px(l.cellR(PieceId.ST1).centre)
        assertTrue("fixture: before placing, the cell shows the piece colour (TYPE-001 ST1 #2DBE7E)", diff(before, rgb(0x2DBE7E)) <= 12)
        val driver = TouchDriver(rule, session, l, p)
        driver.place(targetsOf(p).first { it.piece == PieceId.ST1 })
        assertEquals("Board", session.where(PieceId.ST1))
        rule.onNodeWithTag("size-mark-ST1").assertDoesNotExist()
        assertEquals("the other four marks stay", 4, markNodes().size)
        val after = rule.shot().px(l.cellR(PieceId.ST1).centre)
        assertTrue("the placed triangle's cell no longer shows the piece (dashed outline only)", diff(after, rgb(0x2DBE7E)) > 40)
    }

    // REQ-012.A1 (colours half) - TYPE-001: "fixed colours are LT1 #E8505B, LT2 #3D8BFD, MT #F9A826, SQ #FFD23F, PG #FF7AB8,
    // ST1 #2DBE7E, ST2 #9B5DE5"; two different full-set puzzles show the identical tray.
    @Test
    fun req012_A1_trayMiniaturesWearTheType001Colours() {
        val colours = mapOf(
            PieceId.LT1 to 0xE8505B, PieceId.LT2 to 0x3D8BFD, PieceId.MT to 0xF9A826, PieceId.SQ to 0xFFD23F,
            PieceId.PG to 0xFF7AB8, PieceId.ST1 to 0x2DBE7E, PieceId.ST2 to 0x9B5DE5,
        )
        val p = puzzle("animals-cat")
        rule.showPlay(PlaySession(p, { true }))
        val l = layoutFor(p)
        val shot = rule.shot()
        for ((piece, hex) in colours) {
            val px = shot.px(l.cellR(piece).centre)
            assertTrue("$piece miniature colour ${Integer.toHexString(px)} vs ${Integer.toHexString(hex)}", diff(px, rgb(hex)) <= 12)
        }
    }

    // REQ-013.A2 (on screen) - "Turning a piece in the tray never changes any cell size."
    // The size marks sit in the cells' top-left corners and the badge at the parallelogram's cell: if a cell reflowed when its
    // piece is turned by touch, these nodes would move.
    @Test
    fun req013_A2_turningByTouchNeverMovesOrResizesTheCells() {
        val p = puzzle("shapes-square")
        val session = PlaySession(p, { true })
        rule.showPlay(session)
        val l = layoutFor(p)
        val driver = TouchDriver(rule, session, l, p)
        val before = markNodes().associate { tag(it) to boundsDp(it) }
        val badgeBefore = boundsDp(rule.onNodeWithTag("flip-badge").fetchSemanticsNode())
        for (piece in listOf(PieceId.LT1, PieceId.MT, PieceId.PG, PieceId.ST2)) {
            repeat(8) {
                driver.tap(pressPoint(l.cellR(piece)))
                assertEquals(before, markNodes().associate { tag(it) to boundsDp(it) })
                assertEquals(badgeBefore, boundsDp(rule.onNodeWithTag("flip-badge").fetchSemanticsNode()))
            }
        }
    }

    // REQ-018.A1 (on screen) - "The parallelogram can be mirrored in the tray and on the board." The badge is pressed by touch.
    @Test
    fun req018_A1_theBadgeMirrorsTheParallelogramInTheTrayAndOnTheBoard() {
        // shapes-warmup-2: the parallelogram turned one step and locked with its first corner on (2,2) has room for its mirror
        // image next to it (reference geometry, and the kernel oracle in DragFlipAcceptanceTest confirms it before this runs).
        val p = puzzle("shapes-warmup-2")
        val session = PlaySession(p, { true })
        rule.showPlay(session)
        val l = layoutFor(p)
        val driver = TouchDriver(rule, session, l, p)
        driver.tap(l.badgeR(session).centre)
        assertTrue("mirrored in the tray", session.ps(PieceId.PG).mirrored)
        driver.tap(l.badgeR(session).centre)
        assertFalse("and back", session.ps(PieceId.PG).mirrored)
        driver.place(PlacedPiece(PieceId.PG, Turn(1), false, pointOf(2, 2)))
        assertEquals("fixture: the piece is on the board", "Board", session.where(PieceId.PG))
        driver.tap(l.badgeR(session).centre)
        assertTrue("mirrored on the board (it still locks)", session.ps(PieceId.PG).mirrored)
        assertEquals("Board", session.where(PieceId.PG))
    }
}
