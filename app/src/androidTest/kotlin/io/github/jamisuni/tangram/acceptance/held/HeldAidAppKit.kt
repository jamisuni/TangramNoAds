package io.github.jamisuni.tangram.acceptance.held

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.contracts.progress.PieceSave
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.store.JsonProgressStore
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File
import kotlin.math.abs

// HELD-OUT adapters (TASK-T5) for the WO-005 A1 / A3 device tests: own rule, own store access, own touch and board helpers, so
// this slice shares nothing with the visible `app` kit. Seams: the tags `dev-button`, `dev-dialog`, `dev-passcode`, `dev-ok`,
// `dev-cancel`, `dev-notice`, `dev-show-solution`, `dev-solve-now`, `dev-done`, `dev-solution-<PieceId.name>`,
// `dev-solution-overlay` (design WO-005 "Test seams"), the app's `play-area`, `board`, `solved-bar`, `retry-button`, `restart-button`
// tags and `BoardTransform` (WO-003/004). Every miss is a loud error.

internal const val HELD_PASSCODE = "0417"

/** Wipes the app's saved progress before and after each test (the app persists now, DA-62). Use as the OUTER rule. */
internal class HeldResetStoreRule : TestWatcher() {
    override fun starting(description: Description) = HeldStore.wipe()
    override fun finished(description: Description) = HeldStore.wipe()
}

internal object HeldStore {
    private val filesDir: File get() = InstrumentationRegistry.getInstrumentation().targetContext.filesDir

    fun wipe() {
        filesDir.listFiles { f -> f.name.startsWith("progress") }?.forEach { it.deleteRecursively() }
    }

    /** A fresh reader over the app's real store file: what a restarted app would read. */
    fun open(): JsonProgressStore = JsonProgressStore(filesDir)
}

/** One touch on the node with [tag]; works with the test clock running or paused. */
internal fun ComposeTestRule.heldTouch(tag: String) {
    if (mainClock.autoAdvance) {
        onNodeWithTag(tag).performTouchInput { click() }
    } else {
        onNodeWithTag(tag).performTouchInput { down(center) }
        mainClock.advanceTimeBy(30)
        onNodeWithTag(tag).performTouchInput { up() }
        mainClock.advanceTimeBy(200)
    }
}

/** Needs the clock running: types [code] into the passcode field and confirms with OK. */
internal fun ComposeTestRule.heldEnter(code: String) {
    onNodeWithTag("dev-passcode").performTextInput(code)
    heldTouch("dev-ok")
    waitForIdle()
}

/** Opens the DEV dialog by a touch on the pill; the dialog must be there. */
internal fun ComposeTestRule.heldOpenDialog() {
    heldTouch("dev-button")
    waitForIdle()
    onNodeWithTag("dev-dialog").assertExists()
}

/** Opens the dialog and enters the passcode; the tools must be there afterwards. */
internal fun ComposeTestRule.heldUnlock() {
    heldOpenDialog()
    heldEnter(HELD_PASSCODE)
    onNodeWithTag("dev-show-solution").assertExists()
    onNodeWithTag("dev-solve-now").assertExists()
}

internal fun ComposeTestRule.heldOverlayTags(): List<String> = onAllNodes(
    SemanticsMatcher("a dev-solution piece node") { n: SemanticsNode ->
        val t = n.config.getOrNull(SemanticsProperties.TestTag)
        t != null && t.startsWith("dev-solution-") && t != "dev-solution-overlay"
    },
    useUnmergedTree = true,
).fetchSemanticsNodes().map { it.config.getOrNull(SemanticsProperties.TestTag) ?: error("untagged node") }.sorted()

/** A string resource of the running app by key; loud when absent. */
internal fun heldString(name: String): String {
    val ctx = InstrumentationRegistry.getInstrumentation().targetContext
    val id = ctx.resources.getIdentifier(name, "string", ctx.packageName)
    if (id == 0) error("no string resource $name in ${ctx.packageName}")
    return ctx.getString(id)
}

internal fun heldSave(p: Puzzle, piece: PieceId): PieceSave.OnBoard {
    val sp = p.solution.first { it.piece == piece }
    val pose = PieceGeometry.poseOf(sp.piece, sp.polygon) ?: error("no pose for $piece")
    return PieceSave.OnBoard(pose.at, pose.turn, pose.mirrored)
}

private val HELD_HEX = mapOf(
    PieceId.SQ to 0xFFD23F, PieceId.ST1 to 0x2DBE7E, PieceId.ST2 to 0x9B5DE5, PieceId.LT1 to 0xE8505B,
    PieceId.LT2 to 0x3D8BFD, PieceId.MT to 0xF9A826, PieceId.PG to 0xFF7AB8,
)

internal fun heldFar(a: Int, b: Int): Int = maxOf(
    abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)),
    abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)),
    abs((a and 0xFF) - (b and 0xFF)),
)

/** Screenshots of the play area and counts of the colours on its board region (the board rect from the `board` node). */
internal class HeldBoardRig(private val rule: ComposeTestRule) {

    fun shot(): Bitmap = rule.onNodeWithTag("play-area").captureToImage().asAndroidBitmap()

    /** x0, x1, y0, y1 of the `board` node in the coordinates of the play-area screenshot. */
    fun board(): IntArray {
        val area = rule.onNodeWithTag("play-area").fetchSemanticsNode().boundsInRoot
        val b = rule.onNodeWithTag("board").fetchSemanticsNode().boundsInRoot
        return intArrayOf((b.left - area.left).toInt(), (b.right - area.left).toInt(), (b.top - area.top).toInt(), (b.bottom - area.top).toInt())
    }

    private fun count(shot: Bitmap, rgb: Int, r: IntArray): Int {
        var n = 0
        for (y in r[2].coerceAtLeast(0) until r[3].coerceAtMost(shot.height) step 2) {
            for (x in r[0].coerceAtLeast(0) until r[1].coerceAtMost(shot.width) step 2) if (heldFar(shot.getPixel(x, y), rgb) <= 10) n++
        }
        return n
    }

    /** Pixels of the puzzle's picture base colour on the board. */
    fun baseCount(shot: Bitmap, puzzle: Puzzle): Int = count(shot, 0xFF000000.toInt() or puzzle.picture.base.value, board())

    /** The pieces whose TYPE-001 colour shows on the board (more than 3 sampled pixels). */
    fun piecesOnBoard(shot: Bitmap): List<PieceId> {
        val r = board()
        return PieceId.entries.filter { count(shot, 0xFF000000.toInt() or HELD_HEX.getValue(it), r) > 3 }
    }
}
