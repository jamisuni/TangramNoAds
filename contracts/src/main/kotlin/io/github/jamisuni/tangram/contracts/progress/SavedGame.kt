// ─────────────────────────────────────────────────────────────────
// GOVERNED INTERFACE — SWDev Contract (see architecture.md registry)
// Tier: NOTIFY. Contract types of IProgressStore.
// Serves: REQ-003, REQ-010, REQ-012, REQ-025, REQ-026, REQ-029, REQ-030, REQ-031, REQ-033, REQ-034, REQ-046
// ─────────────────────────────────────────────────────────────────
package io.github.jamisuni.tangram.contracts.progress

import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Turn
import java.time.LocalDate

/**
 * One puzzle's saved progress.
 *
 * @property state New, In progress or Solved (TYPE-006).
 * @property pieces per piece: in the tray or on the board, with position, turn
 *   and mirror (REQ-025). A piece of the puzzle that is not in the map is in
 *   the tray in its TYPE-001 resting turn, unmirrored (decisions.md F2).
 * @property puzzleSeconds active seconds of the current attempt (REQ-030); 0 when New.
 * @property bestSeconds the fastest solve so far, or `null` when there is none
 *   (never solved, or solved only by the DEV aid, REQ-046). Kept by Restart and
 *   Retry; erased only by a reset (REQ-030, REQ-034).
 */
data class PuzzleProgress(
    val state: PuzzleState,
    val pieces: Map<PieceId, PieceSave>,
    val puzzleSeconds: Long,
    val bestSeconds: Long?,
) {
    /**
     * Restart (REQ-025) and Retry (REQ-026, TYPE-006): back to New with every
     * piece in the tray and the running time at 0, **keeping the best time**
     * (REQ-030: "Restart and Retry reset the running time, not the best time").
     */
    fun restarted(): PuzzleProgress = copy(state = PuzzleState.NEW, pieces = emptyMap(), puzzleSeconds = 0)

    companion object {
        val NEW = PuzzleProgress(PuzzleState.NEW, emptyMap(), 0, null)
    }
}

/** Where one piece is, with the turn (TYPE-003) and mirror the player gave it (REQ-025). */
sealed interface PieceSave {
    val turn: Turn
    val mirrored: Boolean

    /** In its own tray cell (REQ-012), keeping its turn and mirror (decisions.md F2). */
    data class InTray(override val turn: Turn, override val mirrored: Boolean) : PieceSave

    /**
     * Locked on the board (REQ-019). [at] is where the piece's local origin
     * lies, exactly (ADR-003): world vertex = R(turn · 45°) · F(mirror) · local
     * vertex + [at], F mirroring x, with the local shapes of the kernel's
     * piece-shape table (`kernel/…/kernel/model`, pinned to
     * Spec/03-puzzle-format.md §3 and `tools/tangram_geom.py` by a golden test)
     * — the same convention as a puzzle file's `rot`/`flip`/`at`.
     */
    data class OnBoard(
        val at: ExactPoint,
        override val turn: Turn,
        override val mirrored: Boolean,
    ) : PieceSave
}

/**
 * Active play time in seconds (TYPE-005, REQ-029).
 *
 * @property day the local date [todaySeconds] belongs to, or `null` before
 *   the first active second; on a later local date today's time starts at 0
 *   (REQ-029: today's time restarts at local midnight).
 */
data class PlayTime(
    val day: LocalDate?,
    val todaySeconds: Long,
    val totalSeconds: Long,
) {
    companion object {
        val NONE = PlayTime(null, 0, 0)
    }
}

/**
 * The player's settings.
 *
 * @property timerShown the running timer on the board (REQ-031); off by default.
 * @property soundOn sound effects and the haptic tick (REQ-033); on by default.
 */
data class GameSettings(
    val timerShown: Boolean = false,
    val soundOn: Boolean = true,
)
