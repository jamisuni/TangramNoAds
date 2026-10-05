package io.github.jamisuni.tangram.time

import io.github.jamisuni.tangram.contracts.progress.GameSettings
import io.github.jamisuni.tangram.contracts.progress.IProgressStore
import io.github.jamisuni.tangram.contracts.progress.PlayTime
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId

// GATE HELPER (WO-008 T8c part ii, gate set of GATE-063). Test scaffolding, no requirement token.
// An in-memory `IProgressStore` that RECORDS EVERY CALL, in order, so a test can ask "which writes happened between these two points".
// It behaves like the real `JsonProgressStore` where the gate depends on it: a save of a value equal to the held one changes nothing
// ("an equal save is skipped by the store", design 4.3 and the rev 2 E2 line); `resetAllProgress()` empties the puzzles and the play time
// and keeps settings and lastShown (the `IProgressStore` KDoc). A call is recorded whether or not it changed anything: `effective` says
// whether the stored value changed.

enum class GateStoreOp(val isWrite: Boolean) {
    READ_PROGRESS(false),
    SAVE_PROGRESS(true),
    READ_PLAY_TIME(false),
    SAVE_PLAY_TIME(true),
    READ_SETTINGS(false),
    SAVE_SETTINGS(true),
    READ_LAST_SHOWN(false),
    SAVE_LAST_SHOWN(true),
    RESET_ALL(true),
}

data class GateStoreCall(
    val op: GateStoreOp,
    val puzzle: PuzzleId? = null,
    val progress: PuzzleProgress? = null,
    val playTime: PlayTime? = null,
    /** For a write: the stored value changed. Always false for a read. */
    val effective: Boolean = false,
)

class GateRecordingProgressStore : IProgressStore {
    private val puzzles = LinkedHashMap<String, PuzzleProgress>()
    private var playTime: PlayTime = PlayTime.NONE
    private var settings: GameSettings = GameSettings()
    private var lastShown: PuzzleId? = null

    private val log = ArrayList<GateStoreCall>()

    /** Every call so far, oldest first (a copy). */
    val calls: List<GateStoreCall> get() = log.toList()

    /** When true, every write call is recorded and then throws, like a failing disk would (G-10 says the keeper swallows it). */
    var failWrites: Boolean = false

    /** A position in the call log; ask for the writes since it with [writesSince]. */
    fun mark(): Int = log.size

    fun callsSince(mark: Int): List<GateStoreCall> = log.drop(mark)

    /** The write calls (saves and the reset) after [mark], effective or not. */
    fun writesSince(mark: Int): List<GateStoreCall> = log.drop(mark).filter { it.op.isWrite }

    /** The write calls after [mark] that changed the stored value. */
    fun effectiveWritesSince(mark: Int): List<GateStoreCall> = writesSince(mark).filter { it.effective }

    /** Puts a record in the store without recording a call: the fixture, or "somebody else changed it behind the keeper". */
    fun seedProgress(puzzle: PuzzleId, progress: PuzzleProgress) {
        puzzles[puzzle.value] = progress
    }

    fun seedPlayTime(value: PlayTime) {
        playTime = value
    }

    private fun failIfAsked(what: String) {
        if (failWrites) throw IllegalStateException("GateRecordingProgressStore: simulated disk failure in $what")
    }

    override fun progress(puzzle: PuzzleId): PuzzleProgress {
        log += GateStoreCall(GateStoreOp.READ_PROGRESS, puzzle = puzzle)
        return puzzles[puzzle.value] ?: PuzzleProgress.NEW
    }

    override fun saveProgress(puzzle: PuzzleId, progress: PuzzleProgress) {
        val held = puzzles[puzzle.value] ?: PuzzleProgress.NEW
        val effective = held != progress
        log += GateStoreCall(GateStoreOp.SAVE_PROGRESS, puzzle = puzzle, progress = progress, effective = effective)
        failIfAsked("saveProgress")
        if (effective) puzzles[puzzle.value] = progress
    }

    override fun playTime(): PlayTime {
        log += GateStoreCall(GateStoreOp.READ_PLAY_TIME)
        return playTime
    }

    override fun savePlayTime(playTime: PlayTime) {
        val effective = this.playTime != playTime
        log += GateStoreCall(GateStoreOp.SAVE_PLAY_TIME, playTime = playTime, effective = effective)
        failIfAsked("savePlayTime")
        if (effective) this.playTime = playTime
    }

    override fun settings(): GameSettings {
        log += GateStoreCall(GateStoreOp.READ_SETTINGS)
        return settings
    }

    override fun saveSettings(settings: GameSettings) {
        val effective = this.settings != settings
        log += GateStoreCall(GateStoreOp.SAVE_SETTINGS, effective = effective)
        failIfAsked("saveSettings")
        if (effective) this.settings = settings
    }

    override fun lastShownPuzzle(): PuzzleId? {
        log += GateStoreCall(GateStoreOp.READ_LAST_SHOWN)
        return lastShown
    }

    override fun saveLastShownPuzzle(puzzle: PuzzleId) {
        val effective = lastShown != puzzle
        log += GateStoreCall(GateStoreOp.SAVE_LAST_SHOWN, puzzle = puzzle, effective = effective)
        failIfAsked("saveLastShownPuzzle")
        if (effective) lastShown = puzzle
    }

    override fun resetAllProgress() {
        val effective = puzzles.isNotEmpty() || playTime != PlayTime.NONE
        log += GateStoreCall(GateStoreOp.RESET_ALL, effective = effective)
        failIfAsked("resetAllProgress")
        puzzles.clear()
        playTime = PlayTime.NONE
        // settings and lastShown are kept (IProgressStore KDoc, DA-120)
    }
}
