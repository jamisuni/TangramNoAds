package io.github.jamisuni.tangram.play.acceptance

import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.play.DragPose
import io.github.jamisuni.tangram.play.DropOutcome
import io.github.jamisuni.tangram.play.DropResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * WO-001 acceptance tests of REQ-021 "Landing preview": A2.
 *
 * The preview is `DropResolver.preview`: where a release would lock now, or null = draw nothing
 * (frozen design sec. 7: "REQ-021: where a release locks now; null = draw nothing").
 */
class Req021LandingPreviewAcceptanceTest {

    private val square = DropResolver(Fixtures.SQUARE)

    private fun assertNoPreviewAndGoesHome(
        resolver: DropResolver,
        pose: DragPose,
        board: List<PlacedPiece>,
        dpPerUnit: Double,
        context: String,
    ) {
        // REQ-021 Statement: "SHALL draw nothing when no position is valid".
        assertNull("$context: no preview may be drawn", resolver.preview(pose, board, dpPerUnit))
        // REQ-021.A2: "...and the drop goes home (REQ-020)."
        assertHome(resolver.release(pose, board, dpPerUnit), pose, context)
    }

    // REQ-021.A2 — "With no valid position in reach, no preview is drawn and the drop goes home (REQ-020)."
    @Test
    fun a2_noAnchorInReach_noPreview_andTheDropGoesHome() {
        // Large triangle in the middle of the empty square: no anchor within 0.65 of any corner.
        assertNoPreviewAndGoesHome(
            square, drag(PieceId.LT1, 0, false, 1.0, 1.0), emptyList(), DP_PER_UNIT_R_0_65, "middle of the empty square",
        )
        // Small triangle next to a placed piece, nothing within reach to tie to.
        assertNoPreviewAndGoesHome(
            square, drag(PieceId.ST1, 0, false, 1.0, 2.5), listOf(SquarePlaced.LT1), DP_PER_UNIT_R_0_65,
            "open area next to a piece",
        )
    }

    // REQ-021.A2 — an anchor IS in reach but every position it gives is invalid: still nothing is drawn.
    @Test
    fun a2_anchorsInReachButNoValidPosition_noPreview_andTheDropGoesHome() {
        // (a) The square (diamond), turn 0, at the top-left corner: corner (0,0) fits the outline corner, but the
        //     diamond's other corners (1,-1) etc. stick out of the silhouette, and so at every other anchor in reach.
        assertNoPreviewAndGoesHome(
            square, drag(PieceId.SQ, 0, false, 0.1, 0.1), emptyList(), DP_PER_UNIT_R_0_65, "sticks out of the silhouette",
        )
        // (b) Medium triangle, turn 0, at the top-left corner with large triangle 1 already there: its corner (0,0)
        //     meets the outline corner but the two pieces would overlap.
        assertNoPreviewAndGoesHome(
            square, drag(PieceId.MT, 0, false, 0.2, 0.1), listOf(SquarePlaced.LT1), DP_PER_UNIT_R_0_65,
            "would overlap a placed piece",
        )
        // (c) One turn step off (TYPE-004): turn 1 of the medium triangle at the top-left corner.
        assertNoPreviewAndGoesHome(
            square, drag(PieceId.MT, 1, false, 0.2, 0.1), emptyList(), DP_PER_UNIT_R_0_65, "one turn step off",
        )
    }

    // REQ-021.A2 / REQ-020 — over the tray or outside the board nothing is drawn, even with an anchor in reach.
    @Test
    fun a2_offTheBoard_noPreview_andTheDropGoesHome() {
        // The same pose as the control below, but not over the board.
        val offBoard = drag(PieceId.LT1, 0, false, 0.3, 0.2, overBoard = false)
        assertNoPreviewAndGoesHome(square, offBoard, emptyList(), DP_PER_UNIT_R_0_65, "off the board")
        val tied = drag(PieceId.ST2, 4, false, 3.3, 3.2, overBoard = false)
        assertNoPreviewAndGoesHome(square, tied, listOf(SquarePlaced.SQ), DP_PER_UNIT_R_0_65, "off the board, next to a piece")
    }

    // REQ-021.A2 (control) — the same poses WITH a valid position in reach DO preview, so the tests above
    // distinguish "nothing to draw" from "never draws".
    @Test
    fun a2_control_aValidPositionInReachIsPreviewedWhereItWouldLock() {
        val onBoard = drag(PieceId.LT1, 0, false, 0.3, 0.2)
        val shown = square.preview(onBoard, emptyList(), DP_PER_UNIT_R_0_65)
        assertNotNull("a valid position is in reach: a preview must be drawn", shown)
        assertEquals(pt(0, 0), shown!!.at)
        assertEquals(onBoard.piece, shown.piece)
        assertEquals(onBoard.turn, shown.turn)
        assertEquals(onBoard.mirrored, shown.mirrored)
        // REQ-021 Statement: the outline is drawn "at the position where the piece would lock if released now".
        val released = square.release(onBoard, emptyList(), DP_PER_UNIT_R_0_65)
        assertLockedExactly(released, onBoard, pt(0, 0))
        assertEquals(shown, (released as DropOutcome.Locked).placed)

        val tied = drag(PieceId.ST2, 4, false, 3.3, 3.2)
        val shownTied = square.preview(tied, listOf(SquarePlaced.SQ), DP_PER_UNIT_R_0_65)
        assertNotNull(shownTied)
        assertEquals(pt(3, 3), shownTied!!.at)
    }

    // REQ-021.A2 — the preview follows the drag: moving out of reach removes it, moving back restores it
    // (nothing is remembered between calls).
    @Test
    fun a2_thePreviewDisappearsWhenTheDragLeavesReach() {
        val near = drag(PieceId.LT1, 0, false, 0.3, 0.2)
        val farAway = drag(PieceId.LT1, 0, false, 1.0, 1.0)
        assertNotNull(square.preview(near, emptyList(), DP_PER_UNIT_R_0_65))
        assertNull(square.preview(farAway, emptyList(), DP_PER_UNIT_R_0_65))
        assertNotNull(square.preview(near, emptyList(), DP_PER_UNIT_R_0_65))
    }
}
