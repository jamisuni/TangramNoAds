package io.github.jamisuni.tangram.play.acceptance

import io.github.jamisuni.tangram.contracts.puzzle.Picture
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleCategory
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleKind
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleTitle
import io.github.jamisuni.tangram.contracts.puzzle.Rgb
import io.github.jamisuni.tangram.contracts.puzzle.SolutionPiece
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Rational
import io.github.jamisuni.tangram.kernel.model.Turn
import io.github.jamisuni.tangram.play.DragPose
import io.github.jamisuni.tangram.play.DropOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

// ---------------------------------------------------------------------------------------------
// Shared fixtures of the WO-001 (#Locking) acceptance tests.
//
// Decision F8: fixtures are LITERAL coordinates, copied from Tangrams/*.json (polygons) and written
// out by hand (poses). Nothing here is produced by PieceGeometry / poseOf, so a convention error in
// the implementation cannot cancel out against a fixture.
//
// Pose convention (frozen design sec. 3, public): world = R(turn * 45 deg) * F(mirror) * local + at,
// y points down, a positive turn is clockwise on screen. The local corners are
//   large triangle (0,0)(4,0)(2,2) / medium triangle (0,0)(2,0)(0,2) / small triangle (0,0)(2,0)(1,1)
//   square (0,0)(1,-1)(2,0)(1,1)  / parallelogram (0,0)(2,0)(3,-1)(1,-1).
// Each fixture pose below carries the corner list it produces in a comment.
// ---------------------------------------------------------------------------------------------

/** Exact integer point. */
internal fun pt(x: Int, y: Int): ExactPoint = ExactPoint(Q2.of(x.toLong()), Q2.of(y.toLong()))

/** Exact coordinate a + b*sqrt(2). */
internal fun q2(a: Int, b: Int): Q2 = Q2(Rational.of(a.toLong()), Rational.of(b.toLong()))

/** A piece already locked on the board: its turn, mirror and the exact position of its vertex 0. */
internal fun placed(piece: PieceId, turn: Int, mirrored: Boolean, x: Int, y: Int): PlacedPiece =
    PlacedPiece(piece, Turn(turn), mirrored, pt(x, y))

/** A drag released at [x],[y] (puzzle units; the position of the piece's vertex 0). */
internal fun drag(
    piece: PieceId,
    turn: Int,
    mirrored: Boolean,
    x: Double,
    y: Double,
    overBoard: Boolean = true,
): DragPose = DragPose(piece, Turn(turn), mirrored, Vec2(x, y), overBoard)

/**
 * TYPE-004: lock distance R = 0.65 units, never below 30 dp on screen, i.e. R = max(0.65, 30 / dpPerUnit).
 * At 100 dp per unit R is 0.65 units (65 dp).
 */
internal const val DP_PER_UNIT_R_0_65 = 100.0

/** At 20 dp per unit the 30 dp floor wins: R = 30 / 20 = 1.5 units (TYPE-004 "never below 30 dp"). */
internal const val DP_PER_UNIT_R_1_5 = 20.0

/**
 * Asserts that [outcome] is a lock of the dropped piece at the exact position [expectedAt] (the exact position
 * of the piece's vertex 0), with the turn and mirror the piece had when dropped.
 */
internal fun assertLockedExactly(outcome: DropOutcome, pose: DragPose, expectedAt: ExactPoint, context: String = "") {
    assertTrue("$context: expected Locked at $expectedAt but the outcome was $outcome", outcome is DropOutcome.Locked)
    val locked = (outcome as DropOutcome.Locked).placed
    assertEquals("$context: the locked piece", pose.piece, locked.piece)
    // REQ-019 Rules: "The turn and mirror the piece has when dropped are the ones tried; the lock never changes them."
    assertEquals("$context: the lock must keep the turn", pose.turn, locked.turn)
    assertEquals("$context: the lock must keep the mirror", pose.mirrored, locked.mirrored)
    // TYPE-004: "the locked position is the anchor's exact position minus the piece corner's exact offset"
    assertEquals("$context: the exact lock position", expectedAt, locked.at)
}

/**
 * Asserts that [outcome] sends the dropped piece home, with the turn and mirror it had when dropped
 * (decisions.md "P1 (G1 review F2)": a piece that goes back to the tray keeps the turn and mirror it had).
 */
internal fun assertHome(outcome: DropOutcome, pose: DragPose, context: String = ""): DropOutcome.Home {
    assertTrue("$context: expected Home but the outcome was $outcome", outcome is DropOutcome.Home)
    val home = outcome as DropOutcome.Home
    assertEquals("$context: the piece that went home", pose.piece, home.piece)
    assertEquals("$context: home keeps the turn", pose.turn, home.turn)
    assertEquals("$context: home keeps the mirror", pose.mirrored, home.mirrored)
    return home
}

private fun poly(vararg xy: Int): List<ExactPoint> = xy.toList().chunked(2).map { (x, y) -> pt(x, y) }

private fun puzzle(id: String, kind: PuzzleKind, rating: Int, vararg pieces: Pair<PieceId, List<ExactPoint>>): Puzzle =
    Puzzle(
        id = PuzzleId(id),
        title = PuzzleTitle(en = id, fi = id),
        category = PuzzleCategory.SHAPES,
        rating = rating,
        kind = kind,
        solution = pieces.map { (piece, polygon) -> SolutionPiece(piece, polygon) },
        picture = Picture(base = Rgb(0x808080), shapes = emptyList()),
        reviewedByHuman = false,
    )

internal object Fixtures {

    /** Tangrams/shapes-mini-1.json: kind mini, a pyramid (triangle (0,2)(4,2)(2,0)). */
    val MINI_1: Puzzle = puzzle(
        "shapes-mini-1", PuzzleKind.MINI, 1,
        PieceId.SQ to poly(2, 2, 3, 1, 2, 0, 1, 1),
        PieceId.ST1 to poly(0, 2, 2, 2, 1, 1),
        PieceId.ST2 to poly(2, 2, 4, 2, 3, 1),
    )

    /** Outline corners of MINI_1's silhouette; (2,2), (1,1) and (3,1) lie on straight sides. */
    val MINI_1_OUTLINE_CORNERS: Set<ExactPoint> = setOf(pt(2, 0), pt(0, 2), pt(4, 2))

    /** Tangrams/shapes-mini-2.json: kind mini, the 2 x 2 square "window". */
    val MINI_2: Puzzle = puzzle(
        "shapes-mini-2", PuzzleKind.MINI, 1,
        PieceId.MT to poly(0, 0, 2, 0, 0, 2),
        PieceId.ST1 to poly(2, 0, 2, 2, 1, 1),
        PieceId.ST2 to poly(2, 2, 0, 2, 1, 1),
    )

    /** Outline corners of MINI_2's silhouette; (1,1) lies inside the square. */
    val MINI_2_OUTLINE_CORNERS: Set<ExactPoint> = setOf(pt(0, 0), pt(2, 0), pt(2, 2), pt(0, 2))

    /** Tangrams/shapes-square.json: kind full, the 4 x 4 square "gift box". */
    val SQUARE: Puzzle = puzzle(
        "shapes-square", PuzzleKind.FULL, 4,
        PieceId.LT1 to poly(0, 0, 4, 0, 2, 2),
        PieceId.LT2 to poly(0, 0, 2, 2, 0, 4),
        PieceId.MT to poly(4, 2, 4, 4, 2, 4),
        PieceId.ST1 to poly(4, 0, 4, 2, 3, 1),
        PieceId.SQ to poly(2, 2, 3, 1, 4, 2, 3, 3),
        PieceId.ST2 to poly(2, 2, 3, 3, 1, 3),
        PieceId.PG to poly(0, 4, 1, 3, 3, 3, 2, 4),
    )

    /** The four corners of the 4 x 4 square silhouette (its only outline corners). */
    val SQUARE_OUTLINE_CORNERS: Set<ExactPoint> = setOf(pt(0, 0), pt(4, 0), pt(4, 4), pt(0, 4))

    /** Tangrams/shapes-warmup-1.json: kind warmup. */
    val WARMUP_1: Puzzle = puzzle(
        "shapes-warmup-1", PuzzleKind.WARMUP, 1,
        PieceId.LT1 to poly(0, 0, 4, 0, 2, 2),
        PieceId.LT2 to poly(5, 5, 1, 5, 3, 3),
        PieceId.MT to poly(0, 6, 0, 4, 2, 6),
        PieceId.SQ to poly(1, 5, 0, 4, 1, 3, 2, 4),
        PieceId.PG to poly(2, 4, 2, 2, 3, 1, 3, 3),
        PieceId.ST1 to poly(4, 2, 4, 4, 3, 3),
        PieceId.ST2 to poly(2, 2, 0, 2, 1, 1),
    )
}

/** The pieces of shapes-square in the stored arrangement, as literal poses (corner lists in the comments). */
internal object SquarePlaced {
    val LT1 = placed(PieceId.LT1, 0, false, 0, 0) // (0,0)(4,0)(2,2)
    val LT2 = placed(PieceId.LT2, 6, false, 0, 4) // (0,4)(0,0)(2,2)
    val MT = placed(PieceId.MT, 4, false, 4, 4) // (4,4)(2,4)(4,2)
    val ST1 = placed(PieceId.ST1, 2, false, 4, 0) // (4,0)(4,2)(3,1)
    val SQ = placed(PieceId.SQ, 0, false, 2, 2) // (2,2)(3,1)(4,2)(3,3)
    val ST2 = placed(PieceId.ST2, 4, false, 3, 3) // (3,3)(1,3)(2,2)
    val PG = placed(PieceId.PG, 0, false, 0, 4) // (0,4)(2,4)(3,3)(1,3)
}

/** The pieces of shapes-mini-2 in the stored arrangement, as literal poses. */
internal object Mini2Placed {
    val MT = placed(PieceId.MT, 0, false, 0, 0) // (0,0)(2,0)(0,2)
    val ST1 = placed(PieceId.ST1, 2, false, 2, 0) // (2,0)(2,2)(1,1)
    val ST2 = placed(PieceId.ST2, 4, false, 2, 2) // (2,2)(0,2)(1,1)
}
