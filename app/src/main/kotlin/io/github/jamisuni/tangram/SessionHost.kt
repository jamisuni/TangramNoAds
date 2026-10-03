package io.github.jamisuni.tangram

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.jamisuni.tangram.browse.PuzzleHost
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.play.PlaySession

/**
 * The `app` implementation of [PuzzleHost] over `play` (WO-004 design section 7, DA-51): every [show] builds a NEW
 * [PlaySession] (restore requires a session with no drag). It never throws (CR-2 F9, G-10), and it never leaves a
 * session of another puzzle shown (code review F4):
 * - restore fails: a fresh New session of this puzzle is shown (an unreadable save is a New puzzle) and it is logged;
 * - building fails too: [session] is null, [capture] is null and the controller's `persist` writes nothing.
 */
class SessionHost internal constructor(
    private val newSession: (Puzzle, () -> Unit) -> PlaySession,
    private val restoreInto: (PlaySession, PuzzleProgress) -> Unit,
) : PuzzleHost {

    constructor(reducedMotion: () -> Boolean) : this(
        newSession = { puzzle, changed -> PlaySession(puzzle = puzzle, reducedMotion = reducedMotion, onChanged = changed) },
        restoreInto = { session, progress -> session.restore(progress) },
    )

    /** The shown session (snapshot state); null before the first [show] and after a failed build. */
    var session: PlaySession? by mutableStateOf(null)
        private set

    /** Fired after each settled session event (DA-49); the view model points it at `controller.persist()`. */
    var onChanged: () -> Unit = {}

    override val state: PuzzleState get() = session?.state ?: PuzzleState.NEW

    override val isDragging: Boolean get() = session?.isDragging ?: false

    override fun show(puzzle: Puzzle, progress: PuzzleProgress) {
        val restored = runCatching { build(puzzle).also { restoreInto(it, progress) } }
            .onFailure { warn("restore failed for ${puzzle.id.value}; showing it as New", it) }
            .getOrNull()
        session = restored ?: runCatching { build(puzzle) }
            .onFailure { warn("session build failed for ${puzzle.id.value}; nothing is shown", it) }
            .getOrNull()
    }

    override fun capture(base: PuzzleProgress): PuzzleProgress? = session?.toProgress(base)

    /** A new session whose `onChanged` goes to the host's current callback. */
    private fun build(puzzle: Puzzle): PlaySession = newSession(puzzle) { onChanged() }

    private fun warn(message: String, t: Throwable) {
        runCatching { Log.w("SessionHost", message, t) } // a plain JVM test has no android.util.Log
    }
}
