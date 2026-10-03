package io.github.jamisuni.tangram.browse

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.GameSettings
import io.github.jamisuni.tangram.contracts.progress.IProgressStore
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PlayTime
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.IPuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState

// ACCEPTANCE-TEST ADAPTERS for `browse/src/androidTest` (TASK-T4), written from IProgressStore's KDoc and the PuzzleHost
// signature of design WO-004 section 5 only. The androidTest source set cannot see `browse/src/test`, so they are repeated here.
// The host keeps `state` and `isDragging` in snapshot state, as the PuzzleHost contract says ("observable").

class DeviceStore : IProgressStore {
    private val map = HashMap<PuzzleId, PuzzleProgress>()
    private var lastShown: PuzzleId? = null
    private var time: PlayTime = PlayTime.NONE
    private var prefs: GameSettings = GameSettings()

    fun seed(id: PuzzleId, progress: PuzzleProgress): DeviceStore = apply { map[id] = progress }
    fun seedLastShown(id: PuzzleId?): DeviceStore = apply { lastShown = id }

    override fun progress(puzzle: PuzzleId): PuzzleProgress = map[puzzle] ?: PuzzleProgress.NEW
    override fun saveProgress(puzzle: PuzzleId, progress: PuzzleProgress) { map[puzzle] = progress }
    override fun playTime(): PlayTime = time
    override fun savePlayTime(playTime: PlayTime) { time = playTime }
    override fun settings(): GameSettings = prefs
    override fun saveSettings(settings: GameSettings) { prefs = settings }
    override fun lastShownPuzzle(): PuzzleId? = lastShown
    override fun saveLastShownPuzzle(puzzle: PuzzleId) { lastShown = puzzle }
    override fun resetAllProgress() { map.clear(); time = PlayTime.NONE }
}

class DeviceHost : PuzzleHost {
    override var state: PuzzleState by mutableStateOf(PuzzleState.NEW)
    override var isDragging: Boolean by mutableStateOf(false)
    var pieces: Map<PieceId, PieceSave> = emptyMap()
    var shownCount = 0
        private set
    private var shownAny = false

    override fun show(puzzle: Puzzle, progress: PuzzleProgress) {
        shownAny = true
        shownCount++
        state = progress.state
        pieces = progress.pieces
    }

    override fun capture(base: PuzzleProgress): PuzzleProgress? = if (!shownAny) null else base.copy(state = state, pieces = pieces)
}

object DeviceKit {
    val library: IPuzzleLibrary by lazy { PuzzleLibrary.packaged() }
    val puzzles: List<Puzzle> get() = library.puzzles

    /** A started controller; [seed] runs before `start()`. */
    fun controller(host: DeviceHost = DeviceHost(), seed: DeviceStore.() -> Unit = {}): Pair<BrowseController, DeviceStore> {
        val store = DeviceStore().apply(seed)
        val c = BrowseController(library, store, host)
        c.start()
        return c to store
    }

    fun solved(): PuzzleProgress = PuzzleProgress(PuzzleState.SOLVED, emptyMap(), 47, 41)
    fun inProgress(): PuzzleProgress = PuzzleProgress(PuzzleState.IN_PROGRESS, emptyMap(), 10, null)
}
