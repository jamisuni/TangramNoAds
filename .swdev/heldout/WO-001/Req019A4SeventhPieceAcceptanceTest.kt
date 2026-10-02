package io.github.jamisuni.tangram.play.acceptance

import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.model.PieceId
import io.github.jamisuni.tangram.play.DropResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HELD-OUT acceptance test of REQ-019.A4 (Test & Verify only):
 * "Six pieces locked in any arrangement that leaves a hole of the seventh piece's shape: the seventh piece locks into it."
 *
 * "Any arrangement" (decision F8): the arrangements below are NOT the stored solution - they are the other tilings of
 * the 4 x 4 square, with same-shape pieces swapped, plus a mini and a warm-up. The seventh piece is released with the
 * turn and mirror that fit the hole and within the lock distance (offsets of at most 0.45 units; R = 0.65): REQ-019
 * Statement and the F23 reading of A4 ("dropped with the matching turn and mirror within the lock distance").
 * Poses are literal; the corner lists were derived with the published convention and are checked against the
 * stored polygons by [a4_everyArrangementIsAnExactCoverOfTheSilhouette].
 */
class Req019A4SeventhPieceAcceptanceTest {

    /** A literal pose: piece, turn (45-degree steps), mirror, exact vertex-0 position (integers). */
    private class FixturePose(val piece: PieceId, val turn: Int, val mirrored: Boolean, val x: Int, val y: Int) {
        fun placed(): PlacedPiece = placed(piece, turn, mirrored, x, y)
        override fun toString() = "$piece turn $turn mirrored $mirrored at ($x, $y)"
    }

    private fun fp(piece: PieceId, turn: Int, mirrored: Boolean, x: Int, y: Int) = FixturePose(piece, turn, mirrored, x, y)

    /**
     * An arrangement: for each of the seven pieces (in PieceId order) every pose that gives the same polygon
     * (a square has four, a parallelogram two; the first is the one used when the piece is on the board).
     */
    private class Arrangement(val label: String, val pieces: List<List<FixturePose>>)

    private val offsets = listOf(0.3 to 0.2, -0.25 to 0.35, 0.1 to -0.4)

    // The 4 x 4 square: tilings 0..7 (tiling 6 has the polygons of the stored solution, with the small triangles
    // exchanged; the other seven are the stored solution turned and reflected).
    private val squareArrangements = listOf(
        Arrangement(
            "tiling 0",
            listOf(
                listOf(fp(PieceId.LT1, 4, false, 4, 4)),
                listOf(fp(PieceId.LT2, 2, false, 4, 0)),
                listOf(fp(PieceId.MT, 0, false, 0, 0)),
                listOf(fp(PieceId.SQ, 6, false, 1, 3), fp(PieceId.SQ, 0, false, 0, 2), fp(PieceId.SQ, 2, false, 1, 1), fp(PieceId.SQ, 4, false, 2, 2)),
                listOf(fp(PieceId.PG, 0, false, 1, 1), fp(PieceId.PG, 4, false, 4, 0)),
                listOf(fp(PieceId.ST1, 0, false, 1, 1)),
                listOf(fp(PieceId.ST2, 6, false, 0, 4)),
            ),
        ),
        Arrangement(
            "tiling 1",
            listOf(
                listOf(fp(PieceId.LT1, 2, false, 4, 0)),
                listOf(fp(PieceId.LT2, 4, false, 4, 4)),
                listOf(fp(PieceId.MT, 0, false, 0, 0)),
                listOf(fp(PieceId.SQ, 0, false, 1, 1), fp(PieceId.SQ, 2, false, 2, 0), fp(PieceId.SQ, 4, false, 3, 1), fp(PieceId.SQ, 6, false, 2, 2)),
                listOf(fp(PieceId.PG, 6, true, 1, 1), fp(PieceId.PG, 2, true, 0, 4)),
                listOf(fp(PieceId.ST1, 0, false, 2, 0)),
                listOf(fp(PieceId.ST2, 6, false, 1, 3)),
            ),
        ),
        Arrangement(
            "tiling 2",
            listOf(
                listOf(fp(PieceId.LT1, 6, false, 0, 4)),
                listOf(fp(PieceId.LT2, 4, false, 4, 4)),
                listOf(fp(PieceId.MT, 2, false, 4, 0)),
                listOf(fp(PieceId.SQ, 2, false, 2, 0), fp(PieceId.SQ, 4, false, 3, 1), fp(PieceId.SQ, 6, false, 2, 2), fp(PieceId.SQ, 0, false, 1, 1)),
                listOf(fp(PieceId.PG, 2, false, 3, 1), fp(PieceId.PG, 6, false, 4, 4)),
                listOf(fp(PieceId.ST1, 0, false, 0, 0)),
                listOf(fp(PieceId.ST2, 2, false, 3, 1)),
            ),
        ),
        Arrangement(
            "tiling 3",
            listOf(
                listOf(fp(PieceId.LT1, 2, false, 4, 0)),
                listOf(fp(PieceId.LT2, 0, false, 0, 0)),
                listOf(fp(PieceId.MT, 6, false, 0, 4)),
                listOf(fp(PieceId.SQ, 4, false, 2, 2), fp(PieceId.SQ, 6, false, 1, 3), fp(PieceId.SQ, 0, false, 0, 2), fp(PieceId.SQ, 2, false, 1, 1)),
                listOf(fp(PieceId.PG, 4, true, 1, 3), fp(PieceId.PG, 0, true, 4, 4)),
                listOf(fp(PieceId.ST1, 6, false, 0, 2)),
                listOf(fp(PieceId.ST2, 4, false, 3, 3)),
            ),
        ),
        Arrangement(
            "tiling 4",
            listOf(
                listOf(fp(PieceId.LT1, 0, false, 0, 0)),
                listOf(fp(PieceId.LT2, 2, false, 4, 0)),
                listOf(fp(PieceId.MT, 6, false, 0, 4)),
                listOf(fp(PieceId.SQ, 6, false, 2, 4), fp(PieceId.SQ, 0, false, 1, 3), fp(PieceId.SQ, 2, false, 2, 2), fp(PieceId.SQ, 4, false, 3, 3)),
                listOf(fp(PieceId.PG, 2, false, 0, 0), fp(PieceId.PG, 6, false, 1, 3)),
                listOf(fp(PieceId.ST1, 4, false, 4, 4)),
                listOf(fp(PieceId.ST2, 6, false, 1, 3)),
            ),
        ),
        Arrangement(
            "tiling 5",
            listOf(
                listOf(fp(PieceId.LT1, 6, false, 0, 4)),
                listOf(fp(PieceId.LT2, 0, false, 0, 0)),
                listOf(fp(PieceId.MT, 4, false, 4, 4)),
                listOf(fp(PieceId.SQ, 0, false, 1, 3), fp(PieceId.SQ, 2, false, 2, 2), fp(PieceId.SQ, 4, false, 3, 3), fp(PieceId.SQ, 6, false, 2, 4)),
                listOf(fp(PieceId.PG, 6, true, 4, 0), fp(PieceId.PG, 2, true, 3, 3)),
                listOf(fp(PieceId.ST1, 2, false, 3, 1)),
                listOf(fp(PieceId.ST2, 4, false, 2, 4)),
            ),
        ),
        Arrangement(
            "tiling 6 (stored polygons, small triangles exchanged)",
            listOf(
                listOf(fp(PieceId.LT1, 0, false, 0, 0)),
                listOf(fp(PieceId.LT2, 6, false, 0, 4)),
                listOf(fp(PieceId.MT, 4, false, 4, 4)),
                listOf(fp(PieceId.SQ, 2, false, 3, 1), fp(PieceId.SQ, 4, false, 4, 2), fp(PieceId.SQ, 6, false, 3, 3), fp(PieceId.SQ, 0, false, 2, 2)),
                listOf(fp(PieceId.PG, 0, false, 0, 4), fp(PieceId.PG, 4, false, 3, 3)),
                listOf(fp(PieceId.ST1, 4, false, 3, 3)),
                listOf(fp(PieceId.ST2, 2, false, 4, 0)),
            ),
        ),
        Arrangement(
            "tiling 7",
            listOf(
                listOf(fp(PieceId.LT1, 4, false, 4, 4)),
                listOf(fp(PieceId.LT2, 6, false, 0, 4)),
                listOf(fp(PieceId.MT, 2, false, 4, 0)),
                listOf(fp(PieceId.SQ, 4, false, 4, 2), fp(PieceId.SQ, 6, false, 3, 3), fp(PieceId.SQ, 0, false, 2, 2), fp(PieceId.SQ, 2, false, 3, 1)),
                listOf(fp(PieceId.PG, 4, true, 0, 0), fp(PieceId.PG, 0, true, 3, 1)),
                listOf(fp(PieceId.ST1, 0, false, 1, 1)),
                listOf(fp(PieceId.ST2, 2, false, 4, 2)),
            ),
        ),
    )

    // shapes-mini-2 (the 2 x 2 square) with the medium triangle in the OPPOSITE corner: (2,2)(0,2)(2,0), and the two
    // small triangles in the others: (0,0)(2,0)(1,1) and (0,0)(1,1)(0,2) - exchanged in the second arrangement.
    private val mini2Arrangements = listOf(
        Arrangement(
            "mini-2, medium triangle in the opposite corner",
            listOf(
                listOf(fp(PieceId.MT, 4, false, 2, 2)),
                listOf(fp(PieceId.ST1, 0, false, 0, 0)),
                listOf(fp(PieceId.ST2, 6, false, 0, 2)),
            ),
        ),
        Arrangement(
            "mini-2, medium triangle in the opposite corner, small triangles exchanged",
            listOf(
                listOf(fp(PieceId.MT, 4, false, 2, 2)),
                listOf(fp(PieceId.ST1, 6, false, 0, 2)),
                listOf(fp(PieceId.ST2, 0, false, 0, 0)),
            ),
        ),
    )

    // shapes-mini-1 (the pyramid) with the two small triangles exchanged.
    private val mini1Arrangements = listOf(
        Arrangement(
            "mini-1, small triangles exchanged",
            listOf(
                listOf(
                    fp(PieceId.SQ, 0, false, 1, 1), fp(PieceId.SQ, 2, false, 2, 0), fp(PieceId.SQ, 4, false, 3, 1),
                    fp(PieceId.SQ, 6, false, 2, 2),
                ),
                listOf(fp(PieceId.ST1, 4, false, 4, 2)),
                listOf(fp(PieceId.ST2, 4, false, 2, 2)),
            ),
        ),
        Arrangement(
            "mini-1, the stored arrangement",
            listOf(
                listOf(
                    fp(PieceId.SQ, 0, false, 1, 1), fp(PieceId.SQ, 2, false, 2, 0), fp(PieceId.SQ, 4, false, 3, 1),
                    fp(PieceId.SQ, 6, false, 2, 2),
                ),
                listOf(fp(PieceId.ST1, 4, false, 2, 2)),
                listOf(fp(PieceId.ST2, 4, false, 4, 2)),
            ),
        ),
    )

    // shapes-warm-up-1 in its stored arrangement (the parallelogram is MIRRORED there).
    private val warmUpArrangements = listOf(
        Arrangement(
            "warm-up-1, the stored arrangement",
            listOf(
                listOf(fp(PieceId.LT1, 0, false, 0, 0)),
                listOf(fp(PieceId.LT2, 4, false, 5, 5)),
                listOf(fp(PieceId.MT, 6, false, 0, 6)),
                listOf(fp(PieceId.SQ, 0, false, 0, 4), fp(PieceId.SQ, 2, false, 1, 3), fp(PieceId.SQ, 4, false, 2, 4), fp(PieceId.SQ, 6, false, 1, 5)),
                listOf(fp(PieceId.PG, 2, true, 2, 4), fp(PieceId.PG, 6, true, 3, 1)),
                listOf(fp(PieceId.ST1, 2, false, 4, 2)),
                listOf(fp(PieceId.ST2, 4, false, 2, 2)),
            ),
        ),
    )

    private fun assertEverySeventhPieceLocksIntoItsHole(puzzle: Puzzle, arrangement: Arrangement, dpPerUnit: Double) {
        val resolver = DropResolver(puzzle)
        for (missing in arrangement.pieces.indices) {
            val board = arrangement.pieces.filterIndexed { i, _ -> i != missing }.map { it[0].placed() }
            for (target in arrangement.pieces[missing]) {
                for ((dx, dy) in offsets) {
                    val pose = drag(target.piece, target.turn, target.mirrored, target.x + dx, target.y + dy)
                    assertLockedExactly(
                        resolver.release(pose, board, dpPerUnit),
                        pose,
                        pt(target.x, target.y),
                        "${arrangement.label}: seventh piece $target released at (${pose.origin.x}, ${pose.origin.y}) " +
                            "into the hole",
                    )
                }
            }
        }
    }

    // REQ-019.A4
    @Test
    fun a4_theSeventhPieceLocksIntoTheHole_inEveryArrangementOfTheSquare() {
        for (arrangement in squareArrangements) {
            assertEverySeventhPieceLocksIntoItsHole(Fixtures.SQUARE, arrangement, DP_PER_UNIT_R_0_65)
        }
    }

    // REQ-019.A4 (also at a screen scale where R is the 30 dp floor)
    @Test
    fun a4_theSeventhPieceLocksIntoTheHole_atAnotherScreenScale() {
        for (arrangement in squareArrangements) {
            assertEverySeventhPieceLocksIntoItsHole(Fixtures.SQUARE, arrangement, DP_PER_UNIT_R_1_5)
        }
    }

    // REQ-019.A4 — the same in the other puzzles (a warm-up whose parallelogram is mirrored, and the mini puzzles,
    // where the last piece of the tray locks into the hole the others leave: REQ-019's Statement holds for any drop).
    @Test
    fun a4_theLastPieceLocksIntoTheHole_inAWarmUpAndInMiniPuzzles() {
        for (arrangement in warmUpArrangements) {
            assertEverySeventhPieceLocksIntoItsHole(Fixtures.WARMUP_1, arrangement, DP_PER_UNIT_R_0_65)
        }
        for (arrangement in mini2Arrangements) {
            assertEverySeventhPieceLocksIntoItsHole(Fixtures.MINI_2, arrangement, DP_PER_UNIT_R_0_65)
        }
        for (arrangement in mini1Arrangements) {
            assertEverySeventhPieceLocksIntoItsHole(Fixtures.MINI_1, arrangement, DP_PER_UNIT_R_0_65)
        }
    }

    // REQ-019.A4 + TYPE-004 — control: the turn must match. One 45-degree step off, the seventh piece does not fit
    // the hole, so it goes home (the fixtures distinguish "fits" from "almost fits").
    @Test
    fun a4_aSeventhPieceOneTurnStepOffGoesHomeInsteadOfLocking() {
        val resolver = DropResolver(Fixtures.SQUARE)
        for (arrangement in squareArrangements) {
            for (missing in arrangement.pieces.indices) {
                val board = arrangement.pieces.filterIndexed { i, _ -> i != missing }.map { it[0].placed() }
                val target = arrangement.pieces[missing][0]
                for (step in listOf(1, 7)) {
                    val pose = drag(target.piece, (target.turn + step) % 8, target.mirrored, target.x + 0.3, target.y + 0.2)
                    assertHome(
                        resolver.release(pose, board, DP_PER_UNIT_R_0_65), pose,
                        "${arrangement.label}: ${target.piece} one step off ($step) the hole",
                    )
                }
            }
        }
    }

    // REQ-019.A4 + TYPE-004 — control: the mirror must match. Only the parallelogram has a mirror image that differs;
    // with the other mirror it does not fit the hole, in any turn, so it goes home.
    @Test
    fun a4_theParallelogramWithTheOtherMirrorDoesNotFillTheHole() {
        val resolver = DropResolver(Fixtures.SQUARE)
        for (arrangement in squareArrangements) {
            val missing = arrangement.pieces.indexOfFirst { it[0].piece == PieceId.PG }
            val board = arrangement.pieces.filterIndexed { i, _ -> i != missing }.map { it[0].placed() }
            val target = arrangement.pieces[missing][0]
            for (turn in 0..7) {
                val pose = drag(PieceId.PG, turn, !target.mirrored, target.x + 0.3, target.y + 0.2)
                assertHome(resolver.release(pose, board, DP_PER_UNIT_R_0_65), pose, "${arrangement.label}: other mirror, turn $turn")
            }
        }
    }

    // Fixture check (independent of the implementation): every square arrangement is an exact cover of the 4 x 4
    // square - the corner lists from the literal poses are inside it, overlap nothing and add up to area 16 - and all
    // but one differ from the stored solution as sets of polygons.
    @Test
    fun a4_everyArrangementIsAnExactCoverOfTheSilhouette() {
        // Corner lists as order-free keys, rounded to 1e-6 (avoids -0.0 and any locale-dependent formatting).
        fun r(v: Double) = (Math.round(v * 1e6) / 1e6 + 0.0).toString()
        fun key(poly: List<Pair<Double, Double>>) = poly.map { r(it.first) + "," + r(it.second) }.sorted().joinToString(";")

        val stored = Fixtures.SQUARE.solution.map { key(HeldOutOracle.polygon(it.polygon)) }.toSet()
        var differing = 0
        for (arrangement in squareArrangements) {
            val placed = arrangement.pieces.map { it[0].placed() }
            var total = 0.0
            for ((i, p) in placed.withIndex()) {
                assertNull(
                    "${arrangement.label}: ${p.piece}",
                    HeldOutOracle.violation(Fixtures.SQUARE, placed.filterIndexed { j, _ -> j < i }, p),
                )
                total += HeldOutOracle.area(HeldOutOracle.corners(p))
            }
            assertEquals("${arrangement.label}: total area", 16.0, total, 1e-9)
            // every alternative pose of a piece is the very same polygon
            for ((i, poses) in arrangement.pieces.withIndex()) {
                val first = key(HeldOutOracle.corners(poses[0].placed()))
                for (alt in poses) assertEquals("${arrangement.label}: ${alt}", first, key(HeldOutOracle.corners(alt.placed())))
                assertTrue(placed[i].piece == poses[0].piece)
            }
            val polys = placed.map { key(HeldOutOracle.corners(it)) }.toSet()
            if (polys != stored) differing++
        }
        assertTrue("only $differing of the arrangements differ from the stored solution", differing >= 7)
    }
}
