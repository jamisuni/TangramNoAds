package io.github.jamisuni.tangram.play.acceptance

import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.play.DragPose
import io.github.jamisuni.tangram.play.DropOutcome
import io.github.jamisuni.tangram.play.DropResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HELD-OUT acceptance test of REQ-021.A1 (Test & Verify only):
 * "Releasing the piece always locks it where the preview was drawn."
 * REQ-021 Rules: "The preview uses the same search as the drop (TYPE-004), so it never disagrees with the lock."
 * Frozen design sec. 7: `preview` = where a release locks now (null = draw nothing); `release` = the drop outcome.
 */
class Req021A1PreviewEqualsReleaseAcceptanceTest {

    private class Scenario(
        val name: String,
        val puzzle: Puzzle,
        val board: List<PlacedPiece>,
        val dpPerUnit: Double,
        val extent: Double,
        val step: Double,
        val minLocks: Int,
    )

    private val warmUpBoard = listOf(
        placed(PieceId.LT1, 0, false, 0, 0), // (0,0)(4,0)(2,2)
        placed(PieceId.SQ, 0, false, 0, 4), // (0,4)(1,3)(2,4)(1,5)
        placed(PieceId.PG, 2, true, 2, 4), // (2,4)(2,2)(3,1)(3,3)
    )

    private val scenarios = listOf(
        Scenario("square, empty, 100 dp", Fixtures.SQUARE, emptyList(), DP_PER_UNIT_R_0_65, 4.0, 0.331, 600),
        Scenario("square, large triangle 1 + square placed, 100 dp", Fixtures.SQUARE, listOf(SquarePlaced.LT1, SquarePlaced.SQ), DP_PER_UNIT_R_0_65, 4.0, 0.331, 380),
        Scenario("square, four pieces placed, 20 dp", Fixtures.SQUARE, listOf(SquarePlaced.LT1, SquarePlaced.MT, SquarePlaced.ST1, SquarePlaced.PG), DP_PER_UNIT_R_1_5, 4.0, 0.331, 500),
        Scenario("mini-2, small triangle 1 placed, 100 dp", Fixtures.MINI_2, listOf(Mini2Placed.ST1), DP_PER_UNIT_R_0_65, 2.0, 0.17, 120),
        Scenario("warm-up-1, three pieces placed, 100 dp", Fixtures.WARMUP_1, warmUpBoard, DP_PER_UNIT_R_0_65, 6.0, 0.523, 90),
    )

    private fun assertPreviewIsTheLock(resolver: DropResolver, pose: DragPose, board: List<PlacedPiece>, dp: Double, where: String) {
        val preview = resolver.preview(pose, board, dp)
        when (val outcome = resolver.release(pose, board, dp)) {
            is DropOutcome.Locked -> assertEquals("$where: the piece must lock where the preview was drawn", preview, outcome.placed)
            is DropOutcome.Home -> assertNull("$where: a preview was drawn but the drop goes home: $preview", preview)
        }
    }

    // REQ-021.A1 — for every pose of a sweep (all pieces, turns, mirrors; many releases; several boards and scales):
    // what the preview shows is exactly what the release locks, and nothing is drawn when the drop goes home.
    @Test
    fun a1_theReleaseLocksExactlyWhereThePreviewWasDrawn_overSweeps() {
        for (sc in scenarios) {
            val resolver = DropResolver(sc.puzzle)
            val onBoard = sc.board.map { it.piece }.toSet()
            val tray = sc.puzzle.solution.map { it.piece }.filter { it !in onBoard }
            val n = (sc.extent / sc.step).toInt() + 5
            var locks = 0
            var drops = 0
            for (piece in tray) {
                val mirrors = if (piece == PieceId.PG) listOf(false, true) else listOf(false)
                for (turn in 0..7) {
                    for (mirrored in mirrors) {
                        for (i in 0 until n) {
                            for (j in 0 until n) {
                                val pose = drag(piece, turn, mirrored, -0.7 + sc.step * i, -0.7 + sc.step * j)
                                drops++
                                val where = "${sc.name}: $piece turn $turn mirrored $mirrored released at " +
                                    "(${pose.origin.x}, ${pose.origin.y})"
                                assertPreviewIsTheLock(resolver, pose, sc.board, sc.dpPerUnit, where)
                                if (resolver.release(pose, sc.board, sc.dpPerUnit) is DropOutcome.Locked) locks++
                            }
                        }
                    }
                }
            }
            // Non-vacuity: if nothing ever locks, "preview == null == home" would hold trivially.
            assertTrue("${sc.name}: only $locks locks in $drops drops", locks >= sc.minLocks)
        }
    }

    // REQ-021.A1 — the preview is not merely consistent with the release, it is the right place: known exact positions.
    @Test
    fun a1_thePreviewShowsTheExactPositionWhereTheReleaseLocks() {
        val square = DropResolver(Fixtures.SQUARE)
        // (pose, board, dp, expected lock position) - the expected positions derived by hand in the visible tests.
        class Case(val pose: DragPose, val board: List<PlacedPiece>, val dp: Double, val at: ExactPoint)
        val cases = listOf(
            Case(drag(PieceId.LT1, 0, false, 0.3, 0.2), emptyList(), DP_PER_UNIT_R_0_65, pt(0, 0)),
            Case(drag(PieceId.MT, 4, false, 4.1, 3.85), emptyList(), DP_PER_UNIT_R_0_65, pt(4, 4)),
            Case(drag(PieceId.LT2, 6, false, 0.25, 4.3), listOf(SquarePlaced.LT1), DP_PER_UNIT_R_0_65, pt(0, 4)),
            Case(drag(PieceId.ST2, 4, false, 3.3, 3.2), listOf(SquarePlaced.SQ), DP_PER_UNIT_R_0_65, pt(3, 3)),
            Case(drag(PieceId.ST2, 4, false, 3.6, 3.6), listOf(SquarePlaced.SQ), DP_PER_UNIT_R_1_5, pt(4, 4)),
            Case(drag(PieceId.SQ, 0, false, 1.49, 2.51), listOf(SquarePlaced.LT1, SquarePlaced.ST1), DP_PER_UNIT_R_1_5, pt(2, 2)),
            Case(drag(PieceId.SQ, 0, false, 1.45, 2.55), listOf(SquarePlaced.LT1, SquarePlaced.ST1), DP_PER_UNIT_R_1_5, pt(1, 3)),
            Case(drag(PieceId.PG, 0, true, 3.8, 3.8), emptyList(), DP_PER_UNIT_R_0_65, pt(4, 4)),
        )
        for ((index, c) in cases.withIndex()) {
            val preview = square.preview(c.pose, c.board, c.dp)
            val expected = PlacedPiece(c.pose.piece, c.pose.turn, c.pose.mirrored, c.at)
            assertEquals("case $index: the preview", expected, preview)
            assertEquals("case $index: the release", DropOutcome.Locked(expected), square.release(c.pose, c.board, c.dp))
        }
    }

    // REQ-021.A1 — a whole play-through by releases: before every release the preview already shows where it will lock,
    // and the board that the previous locks built (their corners become anchors) is respected by the next preview.
    @Test
    fun a1_throughoutABuildOfTheWholeSquareThePreviewIsAlwaysWhereTheNextLockHappens() {
        // Two arrangements of the 4 x 4 square (literal): pieces in the order they are dropped. Each is released
        // 0.1 from its place in both directions (the next candidate spot is at least 1 unit from any exact place).
        val buildOrders = listOf(
            listOf( // the stored solution's polygons
                placed(PieceId.LT1, 0, false, 0, 0), placed(PieceId.LT2, 6, false, 0, 4), placed(PieceId.MT, 4, false, 4, 4),
                placed(PieceId.ST1, 2, false, 4, 0), placed(PieceId.SQ, 0, false, 2, 2), placed(PieceId.ST2, 4, false, 3, 3),
                placed(PieceId.PG, 0, false, 0, 4),
            ),
            listOf( // the solution reflected left-right: the parallelogram is mirrored
                placed(PieceId.LT1, 0, false, 0, 0), placed(PieceId.LT2, 2, false, 4, 0), placed(PieceId.MT, 6, false, 0, 4),
                placed(PieceId.ST1, 6, false, 0, 2), placed(PieceId.SQ, 0, false, 0, 2), placed(PieceId.ST2, 4, false, 3, 3),
                placed(PieceId.PG, 0, true, 4, 4),
            ),
        )
        val square = DropResolver(Fixtures.SQUARE)
        for ((b, order) in buildOrders.withIndex()) {
            val board = ArrayList<PlacedPiece>()
            for (target in order) {
                val pose = drag(
                    target.piece, target.turn.steps, target.mirrored,
                    target.at.x.toDouble() + 0.1, target.at.y.toDouble() + 0.1,
                )
                val preview = square.preview(pose, board, DP_PER_UNIT_R_0_65)
                assertEquals("build $b: the preview of ${target.piece}", target, preview)
                val outcome = square.release(pose, board, DP_PER_UNIT_R_0_65)
                assertLockedExactly(outcome, pose, target.at, "build $b: ${target.piece}")
                assertEquals("build $b: ${target.piece} locks where the preview was", preview, (outcome as DropOutcome.Locked).placed)
                board.add(outcome.placed)
            }
            assertEquals(7, board.size)
        }
    }

    // REQ-021.A1 — nothing is carried between calls: the same pose gives the same preview and the same lock any number
    // of times, whatever was previewed or released in between.
    @Test
    fun a1_thePreviewIsTheSameEveryTimeAndDoesNotDependOnEarlierPreviews() {
        val square = DropResolver(Fixtures.SQUARE)
        val pose = drag(PieceId.LT1, 0, false, 0.3, 0.2)
        val other = drag(PieceId.MT, 4, false, 4.1, 3.85)
        val first = square.preview(pose, emptyList(), DP_PER_UNIT_R_0_65)
        square.preview(other, listOf(SquarePlaced.LT2), DP_PER_UNIT_R_0_65)
        square.release(other, emptyList(), DP_PER_UNIT_R_0_65)
        assertEquals(first, square.preview(pose, emptyList(), DP_PER_UNIT_R_0_65))
        assertEquals(DropOutcome.Locked(first!!), square.release(pose, emptyList(), DP_PER_UNIT_R_0_65))
    }
}
