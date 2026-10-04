package io.github.jamisuni.tangram.settings

import io.github.jamisuni.tangram.contracts.progress.GameSettings
import io.github.jamisuni.tangram.contracts.progress.IProgressStore
import io.github.jamisuni.tangram.contracts.progress.PlayTime
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import java.time.LocalDate

/**
 * ACCEPTANCE-TEST ADAPTER (WO-007 T7c): an in-memory `IProgressStore` written from the `IProgressStore` KDoc only (the settings
 * module does not depend on `store` or `browse`, so it cannot borrow `browse`'s fake). It records every WRITE call in [calls], in
 * order, in a list the test may share with other recorders (the reset callback), and its `resetAllProgress()` really does what the
 * KDoc says (REQ-034: every puzzle New, play time none, settings and lastShown kept), so a test can read the outcome back.
 * Reads are not recorded. It never skips an equal write (the real store does), so a test sees exactly what the controller asked for.
 */
internal class RecordingStore(val calls: MutableList<String> = ArrayList()) : IProgressStore {
    private val map = LinkedHashMap<PuzzleId, PuzzleProgress>()
    private var time: PlayTime = PlayTime.NONE
    private var prefs: GameSettings = GameSettings()
    private var lastShown: PuzzleId? = null

    /** Writes only (the entries of [calls] that are not the test's own markers), by call name. */
    fun writes(): List<String> = calls.filter { it.startsWith("save") || it == "resetAllProgress" }

    fun resetCalls(): Int = calls.count { it == "resetAllProgress" }

    fun seed(id: PuzzleId, progress: PuzzleProgress): RecordingStore = apply { map[id] = progress }
    fun seedPlayTime(playTime: PlayTime): RecordingStore = apply { time = playTime }
    fun seedSettings(settings: GameSettings): RecordingStore = apply { prefs = settings }
    fun seedLastShown(id: PuzzleId?): RecordingStore = apply { lastShown = id }

    /** Everything stored, for before / after comparisons. */
    data class Snapshot(
        val puzzles: Map<PuzzleId, PuzzleProgress>,
        val playTime: PlayTime,
        val settings: GameSettings,
        val lastShown: PuzzleId?,
    )

    fun snapshot(): Snapshot = Snapshot(LinkedHashMap(map), time, prefs, lastShown)

    override fun progress(puzzle: PuzzleId): PuzzleProgress = map[puzzle] ?: PuzzleProgress.NEW

    override fun saveProgress(puzzle: PuzzleId, progress: PuzzleProgress) {
        calls += "saveProgress:${puzzle.value}"
        map[puzzle] = progress
    }

    override fun playTime(): PlayTime = time

    override fun savePlayTime(playTime: PlayTime) {
        calls += "savePlayTime"
        time = playTime
    }

    override fun settings(): GameSettings = prefs

    override fun saveSettings(settings: GameSettings) {
        calls += "saveSettings"
        prefs = settings
    }

    override fun lastShownPuzzle(): PuzzleId? = lastShown

    override fun saveLastShownPuzzle(puzzle: PuzzleId) {
        calls += "saveLastShown:${puzzle.value}"
        lastShown = puzzle
    }

    override fun resetAllProgress() {
        calls += "resetAllProgress"
        map.clear()
        time = PlayTime.NONE
    }

    companion object {
        val PLAY_TIME: PlayTime = PlayTime(LocalDate.of(2026, 10, 4), 125, 4_000)
    }
}
