package io.github.jamisuni.tangram.acceptance.layout

import io.github.jamisuni.tangram.acceptance.TouchRig
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.model.PieceId

// Scaffolding helper (decision DA-107; no requirement token): solves a puzzle by real touch with the shared TouchRig.
// TouchRig.tryPlace judges a drop by the piece's colour at its solution centroid, but the LAST locked piece solves the puzzle and the
// solved picture then replaces the piece colours, so tryPlace reports false for it (device evidence, WO-006 MOVE-DEV6: the Pyramid
// is solved after ST2 although tryPlace(ST2) returned false, and the next attempt found no tray). So all pieces but the last go through
// placePieces, and the last one is dropped and not judged: the caller asserts the solved state.
internal fun TouchRig.solveByTouch(puzzle: Puzzle): List<PieceId> {
    val all = puzzle.solution.map { it.piece }.filter { !pose(puzzle, it).mirrored }
    if (all.size != puzzle.solution.size) error("fixture: ${puzzle.id.value} has a mirrored piece; solveByTouch needs a puzzle of unflipped pieces")
    val placed = placePieces(puzzle, all.size - 1).toMutableList()
    val last = all.first { it !in placed }
    tryPlace(puzzle, last) // result deliberately ignored: a solved puzzle shows its picture, not the piece
    placed += last
    return placed
}
