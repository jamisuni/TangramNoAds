// ─────────────────────────────────────────────────────────────────
// GOVERNED INTERFACE — SWDev Contract (see architecture.md registry)
// Tier: NOTIFY. May change inside feature work; every change is a logged
// contract-delta reported at WO close. Shape changes of the stored data are
// hard-stop 2 (migration + migration test, architecture.md G-09).
// Serves: REQ-003, REQ-010, REQ-012, REQ-025, REQ-026, REQ-029, REQ-030, REQ-031, REQ-033, REQ-034, REQ-046
// ─────────────────────────────────────────────────────────────────
package io.github.jamisuni.tangram.contracts.progress

import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId

/**
 * The saved game: everything the game remembers between runs, kept only on
 * this device — no network, no cloud backup, no device transfer (REQ-010,
 * decisions.md F7).
 *
 * Reads return the current state. Every save is stored before it returns, so
 * closing, killing or updating the app afterwards loses nothing that was saved
 * (REQ-025; decisions.md F4). Missing or unreadable stored data never throws:
 * it reads as a fresh install for the part that cannot be read
 * (architecture.md G-10).
 *
 * Callers follow architecture.md O-08: every save is a read-modify-write of
 * the current stored value inside one event (`progress(id).copy(…)`), no
 * caller keeps a stored record across events, and after [resetAllProgress]
 * the app reloads every slice from the store.
 */
interface IProgressStore {

    /**
     * One puzzle's progress (REQ-003, REQ-025, TYPE-006). A puzzle never
     * saved, or whose stored entry cannot be read, is [PuzzleProgress.NEW].
     */
    fun progress(puzzle: PuzzleId): PuzzleProgress

    /** Replaces one puzzle's saved progress: pieces, state, times (REQ-025, REQ-030). */
    fun saveProgress(puzzle: PuzzleId, progress: PuzzleProgress)

    /** Today's and all-time active play time (REQ-029); [PlayTime.NONE] on a fresh install. */
    fun playTime(): PlayTime

    /** Replaces the play time (REQ-029). */
    fun savePlayTime(playTime: PlayTime)

    /** The player's settings; the defaults of [GameSettings] on a fresh install (REQ-031, REQ-033). */
    fun settings(): GameSettings

    /** Replaces the settings (REQ-031, REQ-033). */
    fun saveSettings(settings: GameSettings)

    /**
     * The puzzle shown when the game was last used, or `null` on a fresh
     * install (decisions.md F4: a relaunch opens it; REQ-045.A1: a fresh
     * install opens the first puzzle).
     */
    fun lastShownPuzzle(): PuzzleId?

    /** Remembers the puzzle now shown (decisions.md F4). */
    fun saveLastShownPuzzle(puzzle: PuzzleId)

    /**
     * REQ-034, called only after the player's confirmation: every puzzle
     * becomes [PuzzleProgress.NEW] (all pieces back in the tray, puzzle and
     * best times erased) and the play time becomes [PlayTime.NONE]. The
     * settings are kept.
     */
    fun resetAllProgress()
}
