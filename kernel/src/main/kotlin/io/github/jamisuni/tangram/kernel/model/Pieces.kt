package io.github.jamisuni.tangram.kernel.model

/** TYPE-001: the five piece shapes of the classic set. */
enum class PieceShape {
    LARGE_TRIANGLE,
    MEDIUM_TRIANGLE,
    SMALL_TRIANGLE,
    SQUARE,
    PARALLELOGRAM,
}

/**
 * TYPE-001: the seven pieces, declared in tray order (left to right, top to
 * bottom). The ids are the puzzle-file ids (`Tangrams/puzzle.schema.json`).
 * Same-shape pieces are interchangeable.
 */
enum class PieceId(val shape: PieceShape) {
    LT1(PieceShape.LARGE_TRIANGLE),
    LT2(PieceShape.LARGE_TRIANGLE),
    MT(PieceShape.MEDIUM_TRIANGLE),
    SQ(PieceShape.SQUARE),
    PG(PieceShape.PARALLELOGRAM),
    ST1(PieceShape.SMALL_TRIANGLE),
    ST2(PieceShape.SMALL_TRIANGLE),
}

/**
 * TYPE-003: a piece's turn in 45° steps, 0..7, clockwise on screen
 * (Spec/03-puzzle-format.md §1). Same numbering as the puzzle file's `rot`.
 */
@JvmInline
value class Turn(val steps: Int) {
    init {
        require(steps in 0..7) { "a turn is 0..7 steps of 45°, got $steps" }
    }
}

/** TYPE-006: the progress state every puzzle keeps. Transitions live in the kernel (architecture.md O-07). */
enum class PuzzleState {
    NEW,
    IN_PROGRESS,
    SOLVED,
}
