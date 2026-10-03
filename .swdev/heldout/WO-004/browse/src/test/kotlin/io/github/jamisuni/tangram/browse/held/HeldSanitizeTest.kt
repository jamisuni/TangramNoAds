package io.github.jamisuni.tangram.browse.held

import io.github.jamisuni.tangram.browse.ProgressRestore
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Rational
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** HELD-OUT (Test & Verify only): REQ-025 A3 and the `start()` row (ii), against the real puzzles. */
class HeldSanitizeTest {

    private val puzzles = BrowseKit.puzzles
    private val seven = puzzles.indexOfFirst { it.solution.size == 7 }
    private val puzzle get() = puzzles[seven]

    private fun shifted(save: PieceSave.OnBoard, dx: Long = 1, dy: Long = 0) = PieceSave.OnBoard(
        ExactPoint(
            Q2(save.at.x.a + Rational.of(dx), save.at.x.b),
            Q2(save.at.y.a + Rational.of(dy), save.at.y.b),
        ),
        save.turn,
        save.mirrored,
    )

    private operator fun Rational.plus(o: Rational): Rational = Rational.of(
        numerator * o.denominator + o.numerator * denominator,
        denominator * o.denominator,
    )

    private fun sanitize(p: PuzzleProgress): PuzzleProgress = ProgressRestore.sanitize(puzzle, p)

    private fun inProgress(pieces: Map<PieceId, PieceSave>) = PuzzleProgress(PuzzleState.IN_PROGRESS, pieces, 12, 9)

    // REQ-025.A3 - "A saved piece that overlaps the current silhouette's edge is in the tray after loading; the others stay."
    // The full solution is saved, but the LAST piece in tray order (ST2) is moved by one unit. It is the only free hole, so
    // a translated copy of it cannot fit: it leaves the silhouette or overlaps an accepted piece. It goes to the tray with its
    // saved turn and mirror; the six others stay exactly as saved.
    @Test
    fun req025_A3_aDisplacedPieceGoesToTheTrayAndTheOthersStay() {
        assertTrue("fixture: a 7-piece puzzle", seven >= 0)
        val full = BrowseKit.fullSolution(puzzle).mapValues { it.value as PieceSave.OnBoard }
        val displaced = shifted(full.getValue(PieceId.ST2))
        val result = sanitize(inProgress(full + (PieceId.ST2 to displaced)))

        assertEquals(PuzzleState.IN_PROGRESS, result.state)
        for ((piece, save) in full) {
            if (piece == PieceId.ST2) continue
            assertEquals("$piece stays where it was saved", save, result.pieces[piece])
        }
        assertEquals(PieceSave.InTray(displaced.turn, displaced.mirrored), result.pieces[PieceId.ST2])
    }

    // REQ-025.A3 - a piece saved far outside the silhouette goes to the tray, the others stay.
    @Test
    fun req025_A3_aPieceFarOutsideTheSilhouetteGoesToTheTrayKeepingItsTurnAndMirror() {
        val full = BrowseKit.fullSolution(puzzle).mapValues { it.value as PieceSave.OnBoard }
        val far = PieceSave.OnBoard(shifted(full.getValue(PieceId.LT1), dx = 1000).at, Turn(5), true)
        val result = sanitize(inProgress(full + (PieceId.LT1 to far)))
        assertEquals(PieceSave.InTray(Turn(5), true), result.pieces[PieceId.LT1])
        for ((piece, save) in full) if (piece != PieceId.LT1) assertEquals("$piece", save, result.pieces[piece])
    }

    // REQ-025.A3 - a piece that overlaps an already accepted piece is not a valid lock either (TYPE-004 validity):
    // LT2 saved on top of LT1's place goes to the tray, LT1 (earlier in tray order) stays.
    @Test
    fun req025_A3_aPieceOverlappingAnAcceptedPieceGoesToTheTray() {
        val lt1 = BrowseKit.solutionSave(puzzle, PieceId.LT1)
        val onTopOfLt1 = PieceSave.OnBoard(lt1.at, lt1.turn, lt1.mirrored)
        val result = sanitize(inProgress(mapOf(PieceId.LT1 to lt1, PieceId.LT2 to onTopOfLt1)))
        assertEquals(lt1, result.pieces[PieceId.LT1])
        assertEquals(PieceSave.InTray(lt1.turn, lt1.mirrored), result.pieces[PieceId.LT2])
    }

    // REQ-025.A3 - valid saves are untouched, tray entries too.
    @Test
    fun req025_A3_validPiecesAndTrayEntriesAreUnchanged() {
        val three = BrowseKit.fullSolution(puzzle).entries.take(3).associate { it.key to it.value }
        val tray = PieceId.ST2 to PieceSave.InTray(Turn(6), true)
        val saved = inProgress(three + tray)
        val result = sanitize(saved)
        assertEquals(three + tray, result.pieces)
    }

    // decision F2 / design section 5 (fail-closed): a position beyond the reader's bound, handed straight to sanitize, goes to the tray
    // without an exception.
    @Test
    fun decisionF2_aGeometryOverflowSendsThePieceToTheTrayWithoutAnException() {
        val full = BrowseKit.fullSolution(puzzle).mapValues { it.value as PieceSave.OnBoard }
        val huge = Rational.of(1L shl 62)
        val bad = PieceSave.OnBoard(ExactPoint(Q2(huge, Rational.of(1L shl 61)), Q2(Rational.of(-(1L shl 62)), huge)), Turn(2), false)
        val result = sanitize(inProgress(full + (PieceId.ST2 to bad)))
        assertEquals(PieceSave.InTray(Turn(2), false), result.pieces[PieceId.ST2])
        assertEquals(full.getValue(PieceId.LT1), result.pieces[PieceId.LT1])
    }

    // design section 5 DA-50: pieces the puzzle lacks are dropped.
    @Test
    fun decisionDA50_piecesThePuzzleLacksAreDropped() {
        val mini = puzzles.first { it.solution.size < 7 }
        val missing = PieceId.entries.first { id -> mini.solution.none { it.piece == id } }
        val own = mini.solution.first().piece
        val saved = inProgress(
            mapOf(
                own to BrowseKit.solutionSave(mini, own),
                missing to PieceSave.InTray(Turn(1), false),
            ),
        )
        val result = ProgressRestore.sanitize(mini, saved)
        assertFalse(result.pieces.containsKey(missing))
        assertEquals(BrowseKit.solutionSave(mini, own), result.pieces[own])
    }

    // decision F3 (design section 5): a stored NEW entry's board pieces (impossible from the writer) become tray pieces.
    @Test
    fun decisionF3_aNewEntryHoldsNoBoardPieces() {
        val lt1 = BrowseKit.solutionSave(puzzle, PieceId.LT1)
        val result = sanitize(PuzzleProgress(PuzzleState.NEW, mapOf(PieceId.LT1 to lt1), 0, null))
        assertEquals(PieceSave.InTray(lt1.turn, lt1.mirrored), result.pieces[PieceId.LT1])
    }

    // REQ-025.A3 - through `start()` (design acceptance table row `start()` (ii)): the stored puzzle is the last shown one with a
    // displaced piece, so a relaunch shows that puzzle with the piece in the tray and the others where they were.
    // decision DA-50 (rider): the sanitized progress is not written back by `start()`.
    @Test
    fun req025_A3_aRelaunchSanitizesTheLastShownPuzzle() {
        val full = BrowseKit.fullSolution(puzzle).mapValues { it.value as PieceSave.OnBoard }
        val displaced = shifted(full.getValue(PieceId.ST2))
        val stored = inProgress(full + (PieceId.ST2 to displaced))
        val rig = BrowseKit.Rig {
            seed(BrowseKit.id(seven), stored)
            seedLastShown(BrowseKit.id(seven))
        }
        assertEquals(seven, rig.controller.index)
        val shown = rig.host.lastShown.progress
        assertEquals(PieceSave.InTray(displaced.turn, displaced.mirrored), shown.pieces[PieceId.ST2])
        for ((piece, save) in full) if (piece != PieceId.ST2) assertEquals("$piece", save, shown.pieces[piece])
        assertFalse("start() writes nothing", rig.store.anyWrites)
        assertEquals("the stored data is not rewritten", stored, rig.store.progress(BrowseKit.id(seven)))
    }
}
