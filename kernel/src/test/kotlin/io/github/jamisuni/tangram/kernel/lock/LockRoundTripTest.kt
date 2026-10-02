package io.github.jamisuni.tangram.kernel.lock

import io.github.jamisuni.tangram.kernel.geometry.PieceGeometry
import io.github.jamisuni.tangram.kernel.geometry.PlacedPiece
import io.github.jamisuni.tangram.kernel.geometry.Vec2
import io.github.jamisuni.tangram.kernel.model.PieceShape
import io.github.jamisuni.tangram.kernel.model.Turn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SCAFFOLDING (TASK-005b, WO-001 design §8): the round trip on all 13 puzzle files, plus its leave-one-out and
 * strictness variants. No acceptance IDs.
 */
class LockRoundTripTest {

    /** (0, 0) and 16 directions at radii 0.02 and 0.05: "a small offset <= 0.05" (design §8). */
    private val offsets: List<Vec2> = buildList {
        add(Vec2(0.0, 0.0))
        for (radius in listOf(0.02, 0.05)) {
            for (k in 0 until 16) {
                val angle = Math.PI * 2.0 * k / 16.0
                add(Vec2(radius * Math.cos(angle), radius * Math.sin(angle)))
            }
        }
    }

    private val lockDistances = listOf(LockSearch.lockDistance(100.0), 1.2)

    @Test
    fun thereAreThirteenPuzzleFiles() {
        assertEquals(13, goldenPuzzles.size)
    }

    @Test
    fun everyPieceInBuildOrderLocksAtExactlyItsSolutionAt() {
        var finds = 0
        for (puzzle in goldenPuzzles) {
            val silhouette = puzzle.silhouette()
            val placed = ArrayList<PlacedPiece>()
            for (id in puzzle.buildOrder) {
                val truth = puzzle.solutionPiece(id)
                val exact = truth.pose.at.vec()
                for (r in lockDistances) {
                    for (offset in offsets) {
                        val origin = Vec2(exact.x + offset.x, exact.y + offset.y)
                        val lock = LockSearch.find(silhouette, placed, id, truth.pose.turn, truth.pose.mirrored, origin, r)
                        assertNotNull("${puzzle.id} $id at offset $offset R=$r: no lock", lock)
                        assertEquals("${puzzle.id} $id at offset $offset R=$r", truth.pose.at, lock!!.at)
                        finds++
                    }
                }
                placed += truth.placed()
            }
        }
        assertTrue("the round trip ran", finds > 5000)
    }

    @Test
    fun aPieceAlreadyOnTheFullBoardRelocksAtItsOwnAt() {
        // Own corners are no anchors and the piece does not overlap itself (F31): re-finding every piece of the
        // completed puzzle at its own pose gives its own `at` again.
        for (puzzle in goldenPuzzles) {
            val silhouette = puzzle.silhouette()
            val full = puzzle.buildOrder.map { puzzle.solutionPiece(it).placed() }
            for (truth in puzzle.solution) {
                for (offset in offsets) {
                    val exact = truth.pose.at.vec()
                    val origin = Vec2(exact.x + offset.x, exact.y + offset.y)
                    val lock = LockSearch.find(silhouette, full, truth.piece, truth.pose.turn, truth.pose.mirrored, origin, 0.65)
                    assertNotNull("${puzzle.id} ${truth.piece}: no lock", lock)
                    assertEquals("${puzzle.id} ${truth.piece}", truth.pose.at, lock!!.at)
                }
            }
        }
    }

    @Test
    fun theLastHoleLocksOnlyAtTheMatchingTurnAndMirror() {
        // Board = the other six pieces, so the hole is exactly the piece's polygon: one step off in turn never
        // locks, and the PG (the one chiral shape) never locks as the other mirror image, at any turn (TYPE-004).
        for (puzzle in goldenPuzzles) {
            if (puzzle.solution.size < 2) continue
            val silhouette = puzzle.silhouette()
            for (truth in puzzle.solution) {
                val board = puzzle.solution.filter { it.piece != truth.piece }.map { it.placed() }
                val exact = truth.pose.at.vec()
                val right = LockSearch.find(
                    silhouette, board, truth.piece, truth.pose.turn, truth.pose.mirrored, exact, 0.65,
                )
                assertEquals("${puzzle.id} ${truth.piece}: the true pose", truth.pose.at, right?.at)

                for (step in listOf(1, -1)) {
                    val turn = Turn(Math.floorMod(truth.pose.turn.steps + step, 8))
                    val off = LockSearch.find(silhouette, board, truth.piece, turn, truth.pose.mirrored, exact, 1.2)
                    assertNull("${puzzle.id} ${truth.piece}: turn ${turn.steps} is one step off", off)
                }
                if (truth.piece.shape == PieceShape.PARALLELOGRAM) {
                    for (steps in 0..7) {
                        val other = LockSearch.find(
                            silhouette, board, truth.piece, Turn(steps), !truth.pose.mirrored, exact, 1.2,
                        )
                        assertNull("${puzzle.id} PG: the other mirror at turn $steps", other)
                    }
                }
            }
        }
    }

    @Test
    fun theLockedCornersAreTheGoldenPolygon() {
        // Same result seen as a polygon: the corners of the locked piece equal the stored polygon as a set.
        for (puzzle in goldenPuzzles) {
            val silhouette = puzzle.silhouette()
            val placed = ArrayList<PlacedPiece>()
            for (id in puzzle.buildOrder) {
                val truth = puzzle.solutionPiece(id)
                val lock = LockSearch.find(
                    silhouette, placed, id, truth.pose.turn, truth.pose.mirrored, truth.pose.at.vec(), 0.65,
                )!!
                val corners = PieceGeometry.corners(id, truth.pose.turn, truth.pose.mirrored, lock.at)
                assertEquals("${puzzle.id} $id", truth.polygon.toSet(), corners.toSet())
                placed += truth.placed()
            }
        }
    }
}
