package io.github.jamisuni.tangram.browse

// SCAFFOLDING (implementer's disposable test helpers, TASK-025). Not acceptance tests; the independent
// test author owns FakeProgressStore / FakeHost.

import io.github.jamisuni.tangram.contracts.progress.GameSettings
import io.github.jamisuni.tangram.contracts.progress.IProgressStore
import io.github.jamisuni.tangram.contracts.progress.PlayTime
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.IPuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import io.github.jamisuni.tangram.kernel.model.PuzzleState

class ScaffoldStore : IProgressStore {
    val map = LinkedHashMap<PuzzleId, PuzzleProgress>()
    var lastShown: PuzzleId? = null

    /** Every write, in order: "progress:<id>" or "last:<id>". */
    val writes = ArrayList<String>()

    override fun progress(puzzle: PuzzleId): PuzzleProgress = map[puzzle] ?: PuzzleProgress.NEW
    override fun saveProgress(puzzle: PuzzleId, progress: PuzzleProgress) {
        writes += "progress:${puzzle.value}"
        map[puzzle] = progress
    }

    override fun playTime(): PlayTime = PlayTime.NONE
    override fun savePlayTime(playTime: PlayTime) = error("not used by browse")
    override fun settings(): GameSettings = GameSettings()
    override fun saveSettings(settings: GameSettings) = error("not used by browse")
    override fun lastShownPuzzle(): PuzzleId? = lastShown
    override fun saveLastShownPuzzle(puzzle: PuzzleId) {
        writes += "last:${puzzle.value}"
        lastShown = puzzle
    }

    override fun resetAllProgress() = error("not used by browse")
}

class ScaffoldHost : PuzzleHost {
    override var state: PuzzleState = PuzzleState.NEW
    override var isDragging: Boolean = false

    /** What the next capture reports on top of the base; null before the first show. */
    var captureAs: ((PuzzleProgress) -> PuzzleProgress)? = null
    val shown = ArrayList<Pair<Puzzle, PuzzleProgress>>()
    var captures = 0

    override fun show(puzzle: Puzzle, progress: PuzzleProgress) {
        shown += puzzle to progress
        state = progress.state
    }

    override fun capture(base: PuzzleProgress): PuzzleProgress? {
        captures++
        if (shown.isEmpty()) return null
        return (captureAs ?: { b -> b.copy(state = state) })(base)
    }
}

class ScaffoldLibrary(override val puzzles: List<Puzzle>) : IPuzzleLibrary {
    override fun puzzle(id: PuzzleId): Puzzle? = puzzles.firstOrNull { it.id == id }
}
