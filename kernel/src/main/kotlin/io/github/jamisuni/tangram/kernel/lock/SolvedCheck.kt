package io.github.jamisuni.tangram.kernel.lock

import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.model.PieceId

/** REQ-022: the kernel decides "solved". */
object SolvedCheck {
    /**
     * True when every piece of [pieces] has a placed (locked) entry in [placed]. There is no comparison with the
     * stored solution: locks are valid by construction (inside the silhouette, no overlap), so a full board is
     * solved. An empty [pieces] list is never solved.
     */
    fun isSolved(pieces: List<PieceId>, placed: List<PlacedPiece>): Boolean {
        if (pieces.isEmpty()) return false
        val locked = placed.map { it.piece }.toSet()
        return pieces.all { it in locked }
    }
}
