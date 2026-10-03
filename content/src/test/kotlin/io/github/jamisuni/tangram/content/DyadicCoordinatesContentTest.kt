package io.github.jamisuni.tangram.content

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SCAFFOLDING (TASK-022, disposable): decision DA-63. Every coordinate of every packaged puzzle (solution
 * polygons, both Q2 coefficients) has a denominator that divides 64, the reader's bound. Picture coordinates are
 * plain doubles (PicturePoint), not exact numbers, so they are out of scope. No acceptance IDs.
 */
class DyadicCoordinatesContentTest {
    @Test
    fun everyPackagedSolutionCoordinateHasADenominatorDividing64() { // decision DA-63
        val library = PuzzleLibrary.packaged()
        assertTrue("no packaged puzzle", library.puzzles.isNotEmpty())
        val bad = ArrayList<String>()
        var checked = 0
        for (puzzle in library.puzzles) {
            for (piece in puzzle.solution) {
                for ((index, point) in piece.polygon.withIndex()) {
                    for ((axis, q) in listOf("x" to point.x, "y" to point.y)) {
                        for ((coef, r) in listOf("a" to q.a, "b" to q.b)) {
                            checked++
                            if (64L % r.denominator != 0L || Math.abs(r.numerator) > 65536L) {
                                bad.add("${puzzle.id.value} ${piece.piece} vertex $index $axis.$coef = $r")
                            }
                        }
                    }
                }
            }
        }
        assertTrue("DA-63 violated by ${bad.size} of $checked values: ${bad.take(10)}", bad.isEmpty())
    }
}
