package io.github.jamisuni.tangram

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.progress.PuzzleProgress
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.model.PuzzleState
import io.github.jamisuni.tangram.kernel.model.Turn
import io.github.jamisuni.tangram.play.PlayEvent
import io.github.jamisuni.tangram.play.PlaySession
import io.github.jamisuni.tangram.promise.RepoScan
import io.github.jamisuni.tangram.settings.FeedbackEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * decision DA-115 / DA-123 (design WO-007 section 3.1, seam rows A-1): the wiring between `play` and `settings`, which stay independent (G-06).
 * No acceptance token (scaffolding for a mechanism).
 *   - `PlayEvent` (play) and `FeedbackEvent` (settings) are the same five values; `app` maps one to the other with `internal fun
 *     PlayEvent.toFeedback(): FeedbackEvent`, "an exhaustive `when` (no `else`)": so a new `PlayEvent` is a compile error, never a silent default.
 *   - `SessionHost` gains `var onEvent: (PlayEvent) -> Unit = {}` (the `onChanged` pattern) and `build` does `session.onEvent = { onEvent(it) }`,
 *     right after `newSession` returns; nothing fires during construction (restore is silent). The internal constructor
 *     `SessionHost(newSession, restoreInto)` does not change (F4), which is why this test builds the host exactly as SessionHostScaffoldingTest does.
 * Assumed places (the design names the signatures, not the packages): `PlayEvent` in `io.github.jamisuni.tangram.play`, `toFeedback()` a top-level
 * function in `io.github.jamisuni.tangram` (a miss is a compile error that the orchestrator routes).
 */
class FeedbackMappingTest {

    // ---- the mapping --------------------------------------------------------------------------------------------------------------

    @Test
    fun decisionDA115_theTwoEnumsAreTheSameFiveValues() {
        assertEquals(
            listOf("PICK_UP", "TURN", "LOCK", "RETURN", "SOLVE"),
            PlayEvent.values().map { it.name },
        )
        assertEquals(PlayEvent.values().map { it.name }, FeedbackEvent.values().map { it.name })
    }

    @Test
    fun decisionDA115_eachPlayEventMapsToItsOwnNamedFeedbackEvent() {
        for (e in PlayEvent.values()) assertEquals("$e", FeedbackEvent.valueOf(e.name), e.toFeedback())
    }

    @Test
    fun decisionDA115_theMappingIsOneToOne() {
        val images = PlayEvent.values().map { it.toFeedback() }
        assertEquals("two play events map to one feedback event", PlayEvent.values().size, images.toSet().size)
        assertEquals("a feedback event has no source", FeedbackEvent.values().toSet(), images.toSet())
    }

    // design: "an exhaustive `when` (no `else`)": an `else` branch would let a sixth event fall into some cue silently.
    @Test
    fun decisionDA115_theMappingIsAnExhaustiveWhenWithNoElse() {
        val root = RepoScan.repoRoot()
        val sources = File(root, "app/src/main").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("no Kotlin source under app/src/main: the check is blind", sources.isNotEmpty())
        val holders = sources.filter { Regex("fun\\s+PlayEvent\\s*\\.\\s*toFeedback\\s*\\(").containsMatchIn(stripLineAndBlockComments(it.readText())) }
        assertEquals("expected exactly one declaration of PlayEvent.toFeedback in app/src/main: $holders", 1, holders.size)
        val body = functionBlock(stripLineAndBlockComments(holders.single().readText()), "toFeedback")
        assertTrue("the mapping holds no `when`: $body", Regex("\\bwhen\\b").containsMatchIn(body))
        assertFalse("the mapping has an `else` branch: $body", Regex("\\belse\\b").containsMatchIn(body))
    }

    @Test
    fun decisionDA115_theBodyReaderCanFail() {
        val ok = "internal fun PlayEvent.toFeedback(): FeedbackEvent = when (this) {\n PlayEvent.TURN -> FeedbackEvent.TURN\n PlayEvent.LOCK -> FeedbackEvent.LOCK\n}\n"
        val bad = "internal fun PlayEvent.toFeedback(): FeedbackEvent = when (this) {\n PlayEvent.TURN -> FeedbackEvent.TURN\n else -> FeedbackEvent.LOCK\n}\n"
        assertFalse(Regex("\\belse\\b").containsMatchIn(functionBlock(ok, "toFeedback")))
        assertTrue(Regex("\\belse\\b").containsMatchIn(functionBlock(bad, "toFeedback")))
    }

    private fun stripLineAndBlockComments(s: String): String =
        s.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ").replace(Regex("//[^\\n]*"), " ")

    /** The text from `fun ... name(` to the closing brace that balances the first `{` after it; error() when there is none. */
    private fun functionBlock(src: String, name: String): String {
        val decl = Regex("fun\\s+(?:\\w+\\s*\\.\\s*)?$name\\s*\\(").find(src) ?: error("no function $name")
        val open = src.indexOf('{', decl.range.last)
        if (open < 0) error("function $name has no block or when")
        var depth = 0
        for (i in open until src.length) {
            if (src[i] == '{') depth++
            if (src[i] == '}') {
                depth--
                if (depth == 0) return src.substring(decl.range.first, i + 1)
            }
        }
        error("unbalanced braces in $name")
    }

    // ---- SessionHost.onEvent -------------------------------------------------------------------------------------------------------

    private val puzzle: Puzzle = PuzzleLibrary.packaged().puzzles.first()
    private val real: (Puzzle, () -> Unit) -> PlaySession = { p, changed -> PlaySession(puzzle = p, onChanged = changed) }

    private fun host() = SessionHost(real) { s, pr -> s.restore(pr) }

    // DA-123: a built session reports its events to the host's `onEvent`.
    @Test
    fun decisionDA123_aShownSessionsEventsReachTheHost() {
        val host = host()
        val got = ArrayList<PlayEvent>()
        host.onEvent = { got += it }
        host.show(puzzle, PuzzleProgress.NEW)
        val session = host.session
        assertNotNull(session)
        session!!.onEvent(PlayEvent.LOCK)
        session.onEvent(PlayEvent.SOLVE)
        assertEquals(listOf(PlayEvent.LOCK, PlayEvent.SOLVE), got)
    }

    // DA-123 ("the `onChanged` pattern"; `build` does `session.onEvent = { onEvent(it) }`): the host's property is read when the event
    // comes, so a listener set after the session was built still hears it, and a replaced listener replaces the old one.
    @Test
    fun decisionDA123_theHostsListenerCanBeSetOrReplacedAfterTheSessionIsBuilt() {
        val host = host()
        host.show(puzzle, PuzzleProgress.NEW)
        val first = ArrayList<PlayEvent>()
        val second = ArrayList<PlayEvent>()
        host.onEvent = { first += it }
        host.session!!.onEvent(PlayEvent.TURN)
        host.onEvent = { second += it }
        host.session!!.onEvent(PlayEvent.PICK_UP)
        assertEquals(listOf(PlayEvent.TURN), first)
        assertEquals(listOf(PlayEvent.PICK_UP), second)
    }

    // DA-123: every session the host builds is wired, not only the first (a new puzzle, a restart).
    @Test
    fun decisionDA123_aSecondShownSessionIsWiredToo() {
        val host = host()
        val got = ArrayList<PlayEvent>()
        host.onEvent = { got += it }
        host.show(puzzle, PuzzleProgress.NEW)
        val firstSession = host.session
        host.show(puzzle, PuzzleProgress.NEW)
        assertTrue("fixture: a new session was built", host.session !== firstSession)
        host.session!!.onEvent(PlayEvent.RETURN)
        assertEquals(listOf(PlayEvent.RETURN), got)
    }

    // DA-123 / DA-115: building and restoring a session fires nothing (a saved board coming back is silent; "nothing fires during
    // construction"), and the default listener is a no-op.
    @Test
    fun decisionDA123_showingAPuzzleEmitsNothingAndTheDefaultListenerIsHarmless() {
        val host = host()
        val got = ArrayList<PlayEvent>()
        host.onEvent = { got += it }
        val saved = PuzzleProgress(
            PuzzleState.IN_PROGRESS,
            mapOf(puzzle.solution.first().piece to PieceSave.InTray(Turn(1), false)),
            12,
            null,
        )
        host.show(puzzle, saved)
        assertEquals("a restored board must be silent", emptyList<PlayEvent>(), got)

        val quiet = host()
        quiet.show(puzzle, PuzzleProgress.NEW)
        quiet.session!!.onEvent(PlayEvent.LOCK) // no listener set: must not throw
    }
}
