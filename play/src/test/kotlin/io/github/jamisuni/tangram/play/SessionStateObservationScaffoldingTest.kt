package io.github.jamisuni.tangram.play

import androidx.compose.runtime.snapshots.Snapshot
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.layout.LayoutClass
import io.github.jamisuni.tangram.kernel.model.PieceId
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// SCAFFOLDING (disposable, code review F2): a reader of PlaySession.state / isDragging is notified only by a real
// transition, never by drag moves or frames (the version counter is for the drawing).
class SessionStateObservationScaffoldingTest {
    private val rows = listOf(
        listOf(PieceId.LT1, PieceId.LT2, PieceId.MT),
        listOf(PieceId.SQ, PieceId.PG, PieceId.ST1, PieceId.ST2),
    )
    private val puzzle = PuzzleLibrary.packaged().puzzles.first()
    private val piece = puzzle.solution.first().piece

    /** The snapshot state objects that reading [read] touches. */
    private fun readSet(read: () -> Unit): Set<Any> {
        val seen = HashSet<Any>()
        Snapshot.takeSnapshot(readObserver = { o -> seen.add(o) }).let { snap -> try { snap.enter(read) } finally { snap.dispose() } }
        return seen
    }

    /** The state objects written (and applied) while [op] runs. */
    private fun changedBy(op: () -> Unit): Set<Any> {
        Snapshot.sendApplyNotifications()
        val changed = HashSet<Any>()
        val handle = Snapshot.registerApplyObserver { set, _ -> changed.addAll(set) }
        try {
            op()
            Snapshot.sendApplyNotifications()
        } finally {
            handle.dispose()
        }
        return changed
    }

    @Test
    fun stateAndIsDraggingDoNotChangeOnDragFramesButDoOnRealTransitions() {
        val s = PlaySession(puzzle, { false })
        s.layout = PlayLayout.compute(390.0, 760.0, 844.0, LayoutClass.PHONE, rows, puzzle)
        val stateReads = readSet { s.state }
        val dragReads = readSet { s.isDragging }
        val versionReads = readSet { s.version }
        assertTrue(stateReads.isNotEmpty() && dragReads.isNotEmpty())
        assertTrue("state does not read the version counter", stateReads.intersect(versionReads).isEmpty())
        assertTrue("isDragging does not read the version counter", dragReads.intersect(versionReads).isEmpty())

        val cell = s.layout!!.cell(piece).centre
        val start = changedBy { s.beginDrag(piece, cell, 1000L) }
        assertTrue("a drag start flips isDragging", start.intersect(dragReads).isNotEmpty())
        assertTrue("and NEW to IN_PROGRESS", start.intersect(stateReads).isNotEmpty())

        val frames = changedBy {
            s.dragTo(Vec2(cell.x + 5.0, cell.y - 40.0))
            s.onFrame(1016L)
            s.dragTo(Vec2(cell.x + 9.0, cell.y - 60.0))
            s.onFrame(1032L)
        }
        assertTrue("the version counter did change (drawing still redraws)", frames.intersect(versionReads).isNotEmpty())
        assertTrue("state untouched by drag frames", frames.intersect(stateReads).isEmpty())
        assertTrue("isDragging untouched by drag frames", frames.intersect(dragReads).isEmpty())

        val end = changedBy { s.interruptDrag() }
        assertTrue("the drag end flips isDragging", end.intersect(dragReads).isNotEmpty())
        assertFalse(s.isDragging)
    }
}
