package io.github.jamisuni.tangram.play

/**
 * Pure drawing rules for pieces (REQ-013, REQ-014 A2).
 * The scale seam is frozen in the WO-003 design; bodies follow.
 */
internal object PieceDrawing {
    /**
     * The drawn scale of a piece in dp per unit, determined by its location.
     * REQ-013: a tray piece is drawn at the miniature scale.
     * REQ-014 A2: a board piece is drawn exactly at board size.
     * A lock or return glide is a cosmetic layer ([Glide.fromScale]); it does not change the logical scale.
     *
     * @param state the piece's location and pose.
     * @param layout the play area layout containing the scales.
     * @return the drawn scale in dp per unit.
     * @throws IllegalArgumentException if the piece is dragged (drawn scale lives in [DragFrame.scale], not state alone).
     */
    fun scale(state: PieceState, layout: PlayLayout): Double = when (state.where) {
        Where.Tray -> layout.trayScale
        is Where.Board -> layout.dpPerUnit
        Where.Dragged -> throw IllegalArgumentException(
            "a dragged piece is drawn at DragFrame.scale, not computable from state and layout alone"
        )
    }

    /**
     * Total variant for the draw loop (CR-1 F1): never throws. A dragged piece is drawn at the frozen frame's
     * scale, else the pick-up scale, else the board scale when [drag] is not that piece's drag.
     */
    fun scale(state: PieceState, layout: PlayLayout, drag: DragState?): Double = when (state.where) {
        Where.Dragged -> drag?.takeIf { it.piece == state.piece }?.let { it.frame?.scale ?: it.pickUp.scale }
            ?: layout.dpPerUnit
        else -> scale(state, layout)
    }
}
