package io.github.jamisuni.tangram.kernel.geometry

import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Rational
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SCAFFOLDING (TASK-001, WO-001): disposable unit tests of the exact arithmetic in
 * Arithmetic.kt. They are not acceptance tests and carry no acceptance IDs.
 * The double comparisons below are a test-side oracle only; the code under test never uses doubles.
 */
class ArithmeticTest {

    private fun r(n: Long, d: Long = 1L) = Rational.of(n, d)

    // ---- Rational ----

    @Test
    fun rationalPlusMinusTimesNormalize() {
        assertEquals(r(5, 6), r(1, 2) + r(1, 3))
        assertEquals(Rational.ONE, r(1, 2) + r(1, 2))
        assertEquals(r(1, 6), r(1, 2) - r(1, 3))
        assertEquals(Rational.ZERO, r(7, 5) - r(7, 5))
        assertEquals(r(1, 3), r(2, 3) * r(1, 2))
        assertEquals(r(-3, 2), r(3, 4) * r(-2))
        assertEquals(r(-3, 2), -r(3, 2))
        assertEquals(r(3, 2), -r(-3, 2))
        assertEquals(Rational.ZERO, -Rational.ZERO)
    }

    @Test
    fun rationalSignum() {
        assertEquals(1, r(1, 7).signum())
        assertEquals(-1, r(-1, 7).signum())
        assertEquals(0, Rational.ZERO.signum())
    }

    @Test
    fun rationalOverflowThrows() {
        val big = r(Long.MAX_VALUE)
        assertThrows(ArithmeticException::class.java) { big + Rational.ONE }
        assertThrows(ArithmeticException::class.java) { r(Long.MAX_VALUE - 1) + r(2) }
        assertThrows(ArithmeticException::class.java) { r(-Long.MAX_VALUE) - r(2) }
        assertThrows(ArithmeticException::class.java) { big * r(2) }
        assertThrows(ArithmeticException::class.java) { r(1, Long.MAX_VALUE) * r(1, 2) }
    }

    // ---- Q2 arithmetic ----

    @Test
    fun q2PlusMinusUnaryMinus() {
        assertEquals(q2(4, 1), q2(1, 3) + q2(3, -2))
        assertEquals(q2(-2, 5), q2(1, 3) - q2(3, -2))
        assertEquals(q2(-1, -3), -q2(1, 3))
        assertEquals(Q2.ZERO, q2(1, 3) - q2(1, 3))
        assertEquals(Q2(r(1, 2), r(-1, 3)), Q2(r(1, 3), r(-1, 6)) + Q2(r(1, 6), r(-1, 6)))
    }

    @Test
    fun q2TimesUsesSqrt2Squared() {
        assertEquals(q2(2), q2(0, 1) * q2(0, 1))                  // √2 · √2 = 2
        assertEquals(q2(-1), q2(1, 1) * q2(1, -1))                // (1+√2)(1−√2) = −1
        assertEquals(q2(7, 5), q2(1, 1) * q2(3, 2))               // (1+√2)(3+2√2) = 3 + 4 + (2+3)√2
        assertEquals(q2(3), q2(3) * q2(1))
        assertEquals(Q2.ZERO, q2(3, 2) * Q2.ZERO)
        // (½√2)² = ½ — the cos/sin of a 45° turn
        val halfRoot2 = Q2(Rational.ZERO, r(1, 2))
        assertEquals(Q2(r(1, 2), Rational.ZERO), halfRoot2 * halfRoot2)
    }

    @Test
    fun q2OverflowThrows() {
        val big = q2(Long.MAX_VALUE)
        assertThrows(ArithmeticException::class.java) { big + q2(1) }
        assertThrows(ArithmeticException::class.java) { big * q2(2) }
        assertThrows(ArithmeticException::class.java) { q2(0, Long.MAX_VALUE) * q2(0, 2) }
        // signum squares a: a² overflows
        assertThrows(ArithmeticException::class.java) { q2(Long.MAX_VALUE, -1).signum() }
    }

    @Test
    fun q2FactoryDefaultsBToZero() {
        assertEquals(Q2(r(5), Rational.ZERO), q2(5))
        assertEquals(Q2.of(5), q2(5))
        assertEquals(Q2(r(5), r(-2)), q2(5, -2))
    }

    // ---- Q2 sign / order ----

    @Test
    fun signumZeroAndSingleTerm() {
        assertEquals(0, Q2.ZERO.signum())
        assertEquals(1, q2(3).signum())
        assertEquals(-1, q2(-3).signum())
        assertEquals(1, q2(0, 1).signum())
        assertEquals(-1, q2(0, -5).signum())
    }

    @Test
    fun signumSameSignNeedsNoComparison() {
        assertEquals(1, q2(1, 1).signum())
        assertEquals(-1, q2(-1, -1).signum())
    }

    @Test
    fun signumOppositeSignsNearZero() {
        // 3 − 2√2 = 0.1716 > 0 (9 > 8)
        assertTrue(q2(3, -2).signum() == 1)
        assertTrue(q2(3, -2) > Q2.ZERO)
        // 99 − 70√2 = 0.00505 > 0 (9801 > 9800), the closest small case
        assertEquals(1, q2(99, -70).signum())
        assertTrue(q2(99, -70) > Q2.ZERO)
        // and the mirror cases are negative
        assertEquals(-1, q2(-3, 2).signum())
        assertEquals(-1, q2(-99, 70).signum())
        // 1 − √2 < 0 and √2 − 1 > 0
        assertEquals(-1, q2(1, -1).signum())
        assertEquals(1, q2(-1, 1).signum())
        // 7 − 5√2 = −0.0711 < 0 (49 < 50) and 17 − 12√2 = 0.0294 > 0 (289 > 288)
        assertEquals(-1, q2(7, -5).signum())
        assertEquals(1, q2(17, -12).signum())
        // 1/2 − 1/3·√2 > 0 and 1/2 − 2/3·√2 < 0
        assertEquals(1, Q2(r(1, 2), r(-1, 3)).signum())
        assertEquals(-1, Q2(r(1, 2), r(-2, 3)).signum())
    }

    @Test
    fun signumMatchesDoubleOracleOnIntegerGrid() {
        // |a − b√2| >= 1/(|a| + |b|√2) > 0.01 for |a|,|b| <= 40, so the double oracle is safe here.
        for (a in -40L..40L) for (b in -40L..40L) {
            val expected = when {
                a == 0L && b == 0L -> 0
                else -> if (a + b * Math.sqrt(2.0) > 0) 1 else -1
            }
            assertEquals("a=$a b=$b", expected, q2(a, b).signum())
        }
    }

    @Test
    fun signumMatchesDoubleOracleOnFractions() {
        val dens = longArrayOf(1, 2, 3, 4, 6)
        for (an in -12L..12L) for (ad in dens) for (bn in -12L..12L) for (bd in dens) {
            val q = Q2(r(an, ad), r(bn, bd))
            val v = an.toDouble() / ad + (bn.toDouble() / bd) * Math.sqrt(2.0)
            val expected = if (an == 0L && bn == 0L) 0 else if (v > 0) 1 else -1
            assertEquals("$q", expected, q.signum())
        }
    }

    @Test
    fun compareToGivesExactOrderAndOperators() {
        assertTrue(q2(1) < q2(0, 1))              // 1 < √2
        assertTrue(q2(0, 1) < q2(2))              // √2 < 2
        assertTrue(q2(3, -2) < q2(1))             // 0.17 < 1
        assertTrue(q2(2) > q2(99, -70))           // 2 > 0.005
        assertTrue(q2(99, -70) > q2(0))           // exact: 99 − 70√2 > 0
        assertTrue(q2(-99, 70) < q2(0))          // −99 + 70√2 = −0.005 < 0
        assertTrue(q2(-1) < q2(-99, 70))         // −1 < −0.005
        assertTrue(q2(3, 2) >= q2(3, 2))
        assertTrue(q2(3, 2) <= q2(3, 2))
        assertFalse(q2(1) > q2(1))
        assertFalse(q2(1) < q2(1))
    }

    @Test
    fun compareToIsZeroExactlyWhenEqual() {
        assertEquals(0, q2(5, -3).compareTo(q2(5, -3)))
        // different construction, same value: Rational is normalized so == holds, and so does compareTo == 0
        val x = Q2(r(2, 4), r(3, 6))
        val y = Q2(r(1, 2), r(1, 2))
        assertEquals(x, y)
        assertEquals(0, x.compareTo(y))
        assertEquals(-1, q2(1).compareTo(q2(2)))
        assertEquals(1, q2(2).compareTo(q2(1)))
    }

    @Test
    fun compareToIsAntisymmetricAndTransitiveOnASample() {
        val sample = listOf(
            q2(0), q2(1), q2(-1), q2(0, 1), q2(0, -1), q2(3, -2), q2(99, -70), q2(-99, 70),
            q2(7, -5), q2(17, -12), q2(2), q2(1, 1), q2(4, 4), Q2(r(1, 2), r(-1, 3)),
        )
        for (p in sample) for (q in sample) {
            assertEquals(Integer.signum(p.compareTo(q)), -Integer.signum(q.compareTo(p)))
            for (s in sample) {
                if (p <= q && q <= s) assertTrue(p <= s)
            }
        }
        val sorted = sample.sortedWith { p, q -> p.compareTo(q) }
        for (i in 0 until sorted.size - 1) assertTrue(sorted[i] <= sorted[i + 1])
    }

    // ---- ExactPoint ----

    @Test
    fun pointPlusMinusAndPointOf() {
        val p = pointOf(1, 2)
        assertEquals(ExactPoint(q2(1), q2(2)), p)
        val a = ExactPoint(q2(1, 1), q2(2, -1))
        val b = ExactPoint(q2(3, 2), q2(-1, 1))
        assertEquals(ExactPoint(q2(4, 3), q2(1, 0)), a + b)
        assertEquals(ExactPoint(q2(-2, -1), q2(3, -2)), a - b)
        assertEquals(a, (a + b) - b)
        assertEquals(pointOf(0, 0), p - p)
        // at = anchor − offset (TYPE-004): the exact position is reproduced by adding the offset back
        val anchor = ExactPoint(q2(4, 1), q2(0, 1))
        val offset = ExactPoint(q2(0, 1), q2(1))
        assertEquals(anchor, (anchor - offset) + offset)
    }

    @Test
    fun readingOrderIsYThenXExactly() {
        val pts = listOf(
            ExactPoint(q2(2), q2(0, 1)),      // y = √2
            ExactPoint(q2(0), q2(1)),         // y = 1
            ExactPoint(q2(3), q2(1)),         // y = 1, x larger
            ExactPoint(q2(0, 1), q2(1)),      // y = 1, x = √2 (between 0 and 3)
            ExactPoint(q2(-5), q2(99, -70)),  // y = 0.005
        )
        val sorted = pts.sortedWith(READING_ORDER)
        assertEquals(
            listOf(pts[4], pts[1], pts[3], pts[2], pts[0]),
            sorted,
        )
        assertEquals(0, READING_ORDER.compare(pts[1], ExactPoint(q2(0), q2(1))))
        assertTrue(READING_ORDER.compare(pts[3], pts[1]) > 0)
    }
}
