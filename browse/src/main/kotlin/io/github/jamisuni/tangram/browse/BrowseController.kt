package io.github.jamisuni.tangram.browse

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.jamisuni.tangram.contracts.progress.IProgressStore
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.IPuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.layout.TrayRules
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.state.PuzzleStates

/**
 * Browsing logic (REQ-003, 024, 025, 026, 050), plain Kotlin on the main thread. Every value is
 * undefined before [start]. [index] is 0-based; the on-screen counter is 1-based.
 *
 * Every entry a control can reach is a no-op while [PuzzleHost.isDragging] (decisions F5).
 */
class BrowseController(
    library: IPuzzleLibrary,
    private val store: IProgressStore,
    private val host: PuzzleHost,
) {
    val puzzles: List<Puzzle> = library.puzzles

    init {
        // decision DA-57: an empty library is a packaging error, not a state to run in
        check(puzzles.isNotEmpty()) { "BrowseController needs at least one puzzle: the library is empty (packaging error)" }
    }

    private var indexState by mutableIntStateOf(0)
    private var gridOpenState by mutableStateOf(false)

    val index: Int get() = indexState
    val current: Puzzle get() = puzzles[indexState]
    val shownState: PuzzleState get() = host.state
    val gridOpen: Boolean get() = gridOpenState

    /** The shown puzzle's best time, read from the store (not snapshot state; WO-008 makes it observable). */
    val shownBestSeconds: Long? get() = store.progress(current.id).bestSeconds

    /** The one route to [PuzzleHost.show]: start, open and restart all sanitize what they restore (DA-51). */
    private fun showAt(i: Int) {
        indexState = i
        val p = puzzles[i]
        host.show(p, ProgressRestore.sanitize(p, store.progress(p.id)))
    }

    /** Shows the last shown puzzle, else the first (the rating-1 mini). Writes nothing. */
    fun start() {
        // decision F4: an id the library lost is ignored
        val last = store.lastShownPuzzle()
        val i = if (last == null) -1 else puzzles.indexOfFirst { it.id == last }
        showAt(if (i >= 0) i else 0)
    }

    fun previous() = open((indexState - 1 + puzzles.size) % puzzles.size)

    fun next() = open((indexState + 1) % puzzles.size)

    /** Long-press next: the first later puzzle (wrapping, current excluded) that is not solved, else a normal [next]. */
    fun nextUnsolved() {
        if (host.isDragging) return
        for (step in 1 until puzzles.size) {
            val j = (indexState + step) % puzzles.size
            if (stateOf(j) != PuzzleState.SOLVED) {
                open(j)
                return
            }
        }
        // decision DA-54: no other puzzle is unsolved
        next()
    }

    /** Saves the leaving puzzle and the last-shown id, then shows puzzle [index]. */
    fun open(index: Int) {
        if (host.isDragging || index !in puzzles.indices || index == indexState) return
        persist()
        store.saveLastShownPuzzle(puzzles[index].id)
        showAt(index)
    }

    fun openGrid() {
        if (host.isDragging) return
        gridOpenState = true
    }

    fun closeGrid() {
        gridOpenState = false
    }

    /** Restart (In progress) and Retry (Solved) are one operation (decision DA-59). */
    fun restart() {
        if (host.isDragging) return
        val state = host.state
        val after = if (state == PuzzleState.SOLVED) PuzzleStates.onRetry(state) else PuzzleStates.onRestart(state)
        if (after == state) return
        val id = current.id
        store.saveProgress(id, store.progress(id).restarted())
        showAt(indexState)
    }

    /**
     * After a confirmed reset (decision DA-120): the current puzzle stays shown, now New. It is exactly
     * `showAt(index)`: a fresh session restored from the (already erased) store. It never calls [persist],
     * never writes the store and never writes `lastShown`. [persist] captures `host.session`, which this call has
     * already replaced, so a later [persist] can only save the New board. Idempotent.
     */
    fun afterReset() = showAt(indexState)

    /** Saves the shown puzzle in canonical form (decisions DA-48, DA-65). */
    fun persist() {
        val id = current.id
        val captured = host.capture(store.progress(id)) ?: return
        store.saveProgress(id, canonical(captured))
    }

    fun stateOf(index: Int): PuzzleState =
        if (index == indexState) host.state else store.progress(puzzles[index].id).state

    private fun canonical(p: PuzzleProgress): PuzzleProgress {
        if (p.state == PuzzleState.SOLVED) return p.copy(pieces = emptyMap())
        val kept = p.pieces.filterNot { (piece, s) ->
            s is PieceSave.InTray && !s.mirrored && s.turn == TrayRules.restingTurn(piece.shape)
        }
        return p.copy(pieces = kept)
    }
}
