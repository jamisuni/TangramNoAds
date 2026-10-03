package io.github.jamisuni.tangram.browse

// SCAFFOLDING (implementer's disposable tests, TASK-025). No acceptance tokens on purpose.

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.layout.TrayRules
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Rational
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowseControllerScaffoldingTest {

    private val real: List<Puzzle> = PuzzleLibrary.packaged().puzzles

    /** Five puzzles with distinct ids over the first real puzzle's content. */
    private val five: List<Puzzle> = (1..5).map { real[0].copy(id = PuzzleId("p$it")) }

    private fun controller(
        puzzles: List<Puzzle> = five,
        store: ScaffoldStore = ScaffoldStore(),
        host: ScaffoldHost = ScaffoldHost(),
    ) = Triple(BrowseController(ScaffoldLibrary(puzzles), store, host), store, host)

    private fun solvedEntry() = PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 0, 7)

    // --- start() -----------------------------------------------------------------------------

    @Test fun startOnEmptyStoreShowsFirstPuzzleAndWritesNothing() {
        val (c, store, host) = controller(real)
        c.start()
        assertEquals(0, c.index)
        assertEquals(real[0], host.shown.single().first)
        assertEquals(PuzzleProgress.NEW, host.shown.single().second)
        assertTrue(store.writes.isEmpty())
    }

    @Test fun startOpensKnownLastShownAndSanitizesDisplacedPiece() {
        val p = real.first { it.solution.size >= 2 }
        val store = ScaffoldStore()
        store.lastShown = p.id
        val far = ExactPoint(Q2.of(500), Q2.of(500))
        store.map[p.id] = PuzzleProgress(
            PuzzleState.IN_PROGRESS,
            mapOf(p.solution[0].piece to PieceSave.OnBoard(far, Turn(3), true)),
            5, null,
        )
        val (c, _, host) = controller(real, store)
        c.start()
        assertEquals(real.indexOf(p), c.index)
        assertEquals(PieceSave.InTray(Turn(3), true), host.shown.single().second.pieces[p.solution[0].piece])
        assertTrue(store.writes.isEmpty())
        // the stored data is not rewritten
        assertTrue(store.map[p.id]!!.pieces.values.single() is PieceSave.OnBoard)
    }

    @Test fun startIgnoresUnknownLastShown() {
        val store = ScaffoldStore().apply { lastShown = PuzzleId("gone") }
        val (c, _, host) = controller(five, store)
        c.start()
        assertEquals(0, c.index)
        assertEquals(1, host.shown.size)
        assertTrue(store.writes.isEmpty())
    }

    // --- DA-57 -------------------------------------------------------------------------------

    @Test fun emptyLibraryFailsWithClearMessage() {
        val e = assertThrows(IllegalStateException::class.java) { controller(emptyList()) }
        assertTrue(e.message!!.contains("library is empty"))
    }

    // --- wrap and navigation -----------------------------------------------------------------

    @Test fun nextAndPreviousWrap() {
        val (c, store, host) = controller()
        c.start()
        c.previous()
        assertEquals(4, c.index)
        c.next()
        assertEquals(0, c.index)
        assertEquals("last:p5", store.writes.first { it.startsWith("last:") })
        assertEquals(3, host.shown.size)
    }

    @Test fun openWritesOnlyLeavingPuzzleAndLastShown() {
        val (c, store, _) = controller()
        c.start()
        c.open(3)
        assertEquals(listOf("progress:p1", "last:p4"), store.writes)
    }

    @Test fun openIgnoresSameAndOutOfRange() {
        val (c, store, host) = controller()
        c.start()
        c.open(0)
        c.open(-1)
        c.open(5)
        assertEquals(1, host.shown.size)
        assertTrue(store.writes.isEmpty())
    }

    // --- drag guard --------------------------------------------------------------------------

    @Test fun everyEntryIsNoOpWhileDragging() {
        val (c, store, host) = controller()
        c.start()
        host.state = PuzzleState.IN_PROGRESS
        host.isDragging = true
        c.next(); c.previous(); c.open(2); c.nextUnsolved(); c.restart(); c.openGrid()
        assertEquals(0, c.index)
        assertFalse(c.gridOpen)
        assertEquals(1, host.shown.size)
        assertTrue(store.writes.isEmpty())
        host.isDragging = false
        c.openGrid()
        assertTrue(c.gridOpen)
        c.closeGrid()
        assertFalse(c.gridOpen)
    }

    // --- next unsolved -----------------------------------------------------------------------

    @Test fun nextUnsolvedSkipsSolvedAndWraps() {
        val store = ScaffoldStore()
        // 1-based puzzles 3 and 7 unsolved, shown puzzle 3
        val seven = (1..7).map { real[0].copy(id = PuzzleId("q$it")) }
        seven.forEachIndexed { i, p -> if (i != 2 && i != 6) store.map[p.id] = solvedEntry() }
        store.lastShown = seven[2].id
        val (c, _, _) = controller(seven, store)
        c.start()
        assertEquals(2, c.index)
        c.nextUnsolved()
        assertEquals(6, c.index)
        c.nextUnsolved() // wraps back to 3
        assertEquals(2, c.index)
    }

    @Test fun nextUnsolvedWithOnlyCurrentUnsolvedActsAsNext() {
        val store = ScaffoldStore()
        five.forEachIndexed { i, p -> if (i != 0) store.map[p.id] = solvedEntry() }
        val (c, _, _) = controller(five, store)
        c.start()
        c.nextUnsolved()
        assertEquals(1, c.index) // decision DA-54
    }

    @Test fun nextUnsolvedAllSolvedActsAsNext() {
        val store = ScaffoldStore()
        five.forEach { store.map[it.id] = solvedEntry() }
        val (c, _, _) = controller(five, store)
        c.start()
        c.nextUnsolved()
        assertEquals(1, c.index)
    }

    // --- restart / retry ---------------------------------------------------------------------

    @Test fun restartFromInProgressKeepsBestTimeAndShowsEmptyNew() {
        val store = ScaffoldStore()
        store.map[five[0].id] = PuzzleProgress(
            PuzzleState.IN_PROGRESS,
            mapOf(PieceId.MT to PieceSave.InTray(Turn(2), false)), 33, 20,
        )
        val (c, _, host) = controller(five, store)
        c.start()
        c.restart()
        val saved = store.map[five[0].id]!!
        assertEquals(PuzzleState.NEW, saved.state)
        assertTrue(saved.pieces.isEmpty())
        assertEquals(0L, saved.puzzleSeconds)
        assertEquals(20L, saved.bestSeconds)
        assertEquals(2, host.shown.size)
        assertEquals(PuzzleState.NEW, host.shown.last().second.state)
    }

    @Test fun retryFromSolvedGivesNew() {
        val store = ScaffoldStore()
        store.map[five[0].id] = solvedEntry()
        val (c, _, host) = controller(five, store)
        c.start()
        assertEquals(PuzzleState.SOLVED, c.shownState)
        c.restart()
        assertEquals(PuzzleState.NEW, store.map[five[0].id]!!.state)
        assertEquals(7L, store.map[five[0].id]!!.bestSeconds)
        assertEquals(PuzzleState.NEW, host.state)
    }

    @Test fun restartOnNewIsNoOp() {
        val (c, store, host) = controller()
        c.start()
        c.restart()
        assertTrue(store.writes.isEmpty())
        assertEquals(1, host.shown.size)
    }

    // --- persist canonical form --------------------------------------------------------------

    @Test fun persistSolvedCaptureStoresNoPieces() {
        val (c, store, host) = controller()
        c.start()
        host.state = PuzzleState.SOLVED
        host.captureAs = { b ->
            b.copy(
                state = PuzzleState.SOLVED,
                pieces = mapOf(PieceId.MT to PieceSave.OnBoard(ExactPoint(Q2.of(0), Q2.of(0)), Turn(1), false)),
            )
        }
        c.persist()
        assertTrue(store.map[five[0].id]!!.pieces.isEmpty())
        assertEquals(PuzzleState.SOLVED, store.map[five[0].id]!!.state)
    }

    @Test fun persistDropsRestingUnmirroredTrayEntriesOnly() {
        val (c, store, host) = controller()
        c.start()
        host.captureAs = { b ->
            b.copy(
                state = PuzzleState.NEW,
                pieces = mapOf(
                    PieceId.LT1 to PieceSave.InTray(TrayRules.restingTurn(PieceId.LT1.shape), false),
                    PieceId.LT2 to PieceSave.InTray(Turn(0), false),
                    PieceId.MT to PieceSave.InTray(TrayRules.restingTurn(PieceId.MT.shape), true),
                    PieceId.SQ to PieceSave.OnBoard(ExactPoint(Q2.of(1), Q2.of(1)), TrayRules.restingTurn(PieceId.SQ.shape), false),
                ),
            )
        }
        c.persist()
        assertEquals(setOf(PieceId.LT2, PieceId.MT, PieceId.SQ), store.map[five[0].id]!!.pieces.keys)
    }

    @Test fun persistBeforeShowWritesNothing() {
        val (c, store, _) = controller()
        c.persist()
        assertTrue(store.writes.isEmpty())
    }

    @Test fun persistPassesTimesThroughFromBase() {
        val store = ScaffoldStore()
        store.map[five[0].id] = PuzzleProgress(PuzzleState.IN_PROGRESS, emptyMap(), 12, 9)
        val (c, _, _) = controller(five, store)
        c.start()
        c.persist()
        assertEquals(12L, store.map[five[0].id]!!.puzzleSeconds)
        assertEquals(9L, store.map[five[0].id]!!.bestSeconds)
    }

    // --- stateOf -----------------------------------------------------------------------------

    @Test fun stateOfShownUsesHostOthersUseStore() {
        val store = ScaffoldStore()
        store.map[five[2].id] = solvedEntry()
        val (c, _, host) = controller(five, store)
        c.start()
        host.state = PuzzleState.IN_PROGRESS
        assertEquals(PuzzleState.IN_PROGRESS, c.stateOf(0))
        assertEquals(PuzzleState.SOLVED, c.stateOf(2))
        assertEquals(PuzzleState.NEW, c.stateOf(1))
    }

    // --- sanitize ----------------------------------------------------------------------------

    private fun placedSave(p: Puzzle, piece: PieceId): PieceSave.OnBoard {
        val pose = PieceGeometry.poseOf(piece, p.solution.first { it.piece == piece }.polygon)!!
        return PieceSave.OnBoard(pose.at, pose.turn, pose.mirrored)
    }

    @Test fun sanitizeKeepsValidSolutionPlacementsAndTraysDisplacedOne() {
        val p = real.first { it.solution.size >= 3 }
        val (a, b, d) = p.solution.map { it.piece }.sortedBy { TrayRules.order.indexOf(it) }.take(3)
        val displaced = PieceSave.OnBoard(ExactPoint(Q2.of(400), Q2.of(0)), Turn(5), true)
        val saved = PuzzleProgress(
            PuzzleState.IN_PROGRESS,
            mapOf(a to placedSave(p, a), b to displaced, d to placedSave(p, d)), 3, 2,
        )
        val out = ProgressRestore.sanitize(p, saved)
        assertEquals(placedSave(p, a), out.pieces[a])
        assertEquals(PieceSave.InTray(Turn(5), true), out.pieces[b])
        assertEquals(placedSave(p, d), out.pieces[d])
        assertEquals(3L, out.puzzleSeconds)
        assertEquals(2L, out.bestSeconds)
    }

    @Test fun sanitizeSendsOverlappingLaterPieceToTray() {
        val p = real.first { it.solution.size >= 2 }
        val (a, b) = p.solution.map { it.piece }.sortedBy { TrayRules.order.indexOf(it) }.take(2)
        // b is saved exactly where a is: only the earlier piece in tray order is kept
        val onA = placedSave(p, a)
        val saved = PuzzleProgress(
            PuzzleState.IN_PROGRESS,
            mapOf(a to onA, b to PieceSave.OnBoard(onA.at, onA.turn, onA.mirrored)), 0, null,
        )
        val out = ProgressRestore.sanitize(p, saved)
        assertEquals(onA, out.pieces[a])
        assertTrue(out.pieces[b] is PieceSave.InTray)
    }

    @Test fun hostileCoordinatesEndInTrayWithoutThrowing() {
        // The kernel's isValidPlacement fails closed first, so sanitize's runCatching is a second net this test cannot isolate.
        // decision DA-50: a value that makes the exact arithmetic overflow must not crash
        val p = real.first { it.solution.isNotEmpty() }
        val piece = p.solution[0].piece
        // Coprime denominators at the reader's magnitude limit (65536/63 vs -65535/61) overflow Q2.signum
        // inside the lock geometry (design DA-63), so the geometry really throws for this value.
        val at = ExactPoint(
            Q2(Rational.of(65536, 63), Rational.of(-65535, 61)),
            Q2(Rational.of(-65535, 61), Rational.of(65536, 63)),
        )
        val saved = PuzzleProgress(PuzzleState.IN_PROGRESS, mapOf(piece to PieceSave.OnBoard(at, Turn(2), false)), 0, null)
        val out = ProgressRestore.sanitize(p, saved)
        assertEquals(PieceSave.InTray(Turn(2), false), out.pieces[piece])
    }

    @Test fun sanitizeSolvedEntryHasNoPieces() {
        val p = real[0]
        val piece = p.solution[0].piece
        val saved = PuzzleProgress(PuzzleState.SOLVED, mapOf(piece to placedSave(p, piece)), 0, 4)
        val out = ProgressRestore.sanitize(p, saved)
        assertTrue(out.pieces.isEmpty())
        assertEquals(PuzzleState.SOLVED, out.state)
        assertEquals(4L, out.bestSeconds)
    }

    @Test fun sanitizeNewEntryTraysBoardPiecesAndDropsForeignPieces() {
        val p = real.first { it.solution.size < 7 }
        val foreign = PieceId.entries.first { id -> p.solution.none { it.piece == id } }
        val own = p.solution[0].piece
        val saved = PuzzleProgress(
            PuzzleState.NEW,
            mapOf(own to placedSave(p, own), foreign to PieceSave.InTray(Turn(2), false)), 0, null,
        )
        val out = ProgressRestore.sanitize(p, saved)
        assertTrue(out.pieces[own] is PieceSave.InTray)
        assertNull(out.pieces[foreign])
    }

    // --- grid --------------------------------------------------------------------------------

    @Test fun gridStateOpensAndCloses() {
        val (c, _, _) = controller()
        c.start()
        assertFalse(c.gridOpen)
        c.openGrid()
        assertTrue(c.gridOpen)
        c.closeGrid()
        assertFalse(c.gridOpen)
    }
}
