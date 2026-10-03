package io.github.jamisuni.tangram.browse

import io.github.jamisuni.tangram.contracts.progress.GameSettings
import io.github.jamisuni.tangram.contracts.progress.IProgressStore
import io.github.jamisuni.tangram.contracts.progress.PlayTime
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId

/**
 * ACCEPTANCE-TEST ADAPTER (TASK-T4): an in-memory `IProgressStore` that records every write, written from the
 * `IProgressStore` KDoc only. It does not skip equal writes (the real store does), so tests see exactly what the
 * controller asked for. Fails loudly: there is nothing optional here.
 */
class FakeProgressStore : IProgressStore {
    private val map = HashMap<PuzzleId, PuzzleProgress>()
    private var lastShown: PuzzleId? = null
    private var time: PlayTime = PlayTime.NONE
    private var prefs: GameSettings = GameSettings()

    /** Every `saveProgress` call, in order. */
    val progressWrites = mutableListOf<Pair<PuzzleId, PuzzleProgress>>()

    /** Every `saveLastShownPuzzle` call, in order. */
    val lastShownWrites = mutableListOf<PuzzleId>()

    /** Number of calls to any other `save*` / reset. */
    var otherWrites = 0
        private set

    val anyWrites: Boolean get() = progressWrites.isNotEmpty() || lastShownWrites.isNotEmpty() || otherWrites > 0

    /** Seeds without recording a write (a previous run's data). */
    fun seed(id: PuzzleId, progress: PuzzleProgress): FakeProgressStore = apply { map[id] = progress }

    fun seedLastShown(id: PuzzleId?): FakeProgressStore = apply { lastShown = id }

    fun clearWriteLog() {
        progressWrites.clear()
        lastShownWrites.clear()
        otherWrites = 0
    }

    /** The whole stored map, for before/after comparisons. */
    fun snapshot(): Map<PuzzleId, PuzzleProgress> = HashMap(map)

    override fun progress(puzzle: PuzzleId): PuzzleProgress = map[puzzle] ?: PuzzleProgress.NEW

    override fun saveProgress(puzzle: PuzzleId, progress: PuzzleProgress) {
        progressWrites += puzzle to progress
        map[puzzle] = progress
    }

    override fun playTime(): PlayTime = time

    override fun savePlayTime(playTime: PlayTime) {
        otherWrites++
        time = playTime
    }

    override fun settings(): GameSettings = prefs

    override fun saveSettings(settings: GameSettings) {
        otherWrites++
        prefs = settings
    }

    override fun lastShownPuzzle(): PuzzleId? = lastShown

    override fun saveLastShownPuzzle(puzzle: PuzzleId) {
        lastShownWrites += puzzle
        lastShown = puzzle
    }

    override fun resetAllProgress() {
        otherWrites++
        map.clear()
        time = PlayTime.NONE
    }
}
