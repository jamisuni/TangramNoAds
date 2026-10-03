package io.github.jamisuni.tangram.kernel.geometry

import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Rational
import java.math.BigInteger
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * SCAFFOLDING (TASK-022, disposable): the DA-63 safety proof. With rationals |n| <= 65536 and
 * d in {1,2,4,8,16,32,64} in both Q2 coefficients, READING_ORDER, Q2.compareTo and Q2.signum never throw
 * (guardrail G-10, ADR-003: exact arithmetic throws on overflow, so the reader's bound must make it impossible).
 * No acceptance IDs.
 */
class DyadicBoundSafetyScaffoldingTest {

    private val dens = longArrayOf(1, 2, 4, 8, 16, 32, 64)
    private val bigNums = longArrayOf(0, 1, -1, 65536, -65536, 65535, -65535)

    /** Every comparison path of the kernel on (p, q); any ArithmeticException fails the test. */
    private fun both(p: Q2, q: Q2) {
        val c = p.compareTo(q)
        assertEquals(c, -q.compareTo(p))
        p.signum()
        READING_ORDER.compare(ExactPoint(p, q), ExactPoint(q, p))
        READING_ORDER.compare(ExactPoint(p, Q2.ZERO), ExactPoint(q, Q2.ZERO))
        READING_ORDER.compare(ExactPoint(Q2.ZERO, p), ExactPoint(Q2.ZERO, q))
    }

    @Test
    fun systematicExtremesNeverThrow() { // decision DA-63
        val rats = ArrayList<Rational>()
        for (n in bigNums) for (d in dens) rats.add(Rational.of(n, d))
        val distinct = rats.distinct()
        val all = ArrayList<Q2>()
        for (a in distinct) for (b in distinct) all.add(Q2(a, b))
        var pairs = 0L
        for (p in all) for (q in all) {
            both(p, q)
            pairs++
        }
        assertTrue(pairs > 1_000_000)
    }

    @Test
    fun nearlyEqualValuesWithTinyExactDifference() { // decision DA-63
        // q = p shifted by -1/64, 0 or +1/64 in each coefficient: the exact difference is tiny
        for (d in dens) for (n in longArrayOf(65536, -65536, 65535, 1)) {
            val p = Q2(Rational.of(n, d), Rational.of(-n, 64))
            for (da in longArrayOf(-1, 0, 1)) for (db in longArrayOf(-1, 0, 1)) {
                val q = Q2(Rational.of(n * 64 / d + da, 64), Rational.of(-n + db, 64))
                both(p, q)
                both(q, p)
            }
        }
        both(Q2(Rational.of(1, 64), Rational.of(-1, 64)), Q2(Rational.of(-1, 64), Rational.of(1, 64)))
        both(Q2(Rational.of(65536), Rational.of(-65536)), Q2(Rational.of(65536 * 64 - 1, 64), Rational.of(-65536 * 64 + 1, 64)))
    }

    @Test
    fun seededRandomSweep() { // decision DA-63
        val rnd = Random(20261003L)
        fun rat(): Rational {
            val d = dens[rnd.nextInt(dens.size)]
            val n = when (rnd.nextInt(4)) {
                0 -> 65536L - rnd.nextInt(4)
                1 -> -(65536L - rnd.nextInt(4))
                else -> rnd.nextInt(131073) - 65536L
            }
            return Rational.of(n, d)
        }
        repeat(100_000) { i ->
            val a = rat()
            var b = rat()
            // opposite-sign coefficient pairs are forced half the time: the only path that squares
            if (i % 2 == 0 && a.numerator != 0L && b.numerator != 0L && (a.numerator > 0) == (b.numerator > 0)) {
                b = Rational.of(-b.numerator, b.denominator)
            }
            both(Q2(a, b), Q2(rat(), rat()))
        }
    }

    @Test
    fun worstIntermediateStaysBelowLongRange() { // decision DA-63
        // A difference coefficient is n/d with |n| <= 2*65536*64 = 2^23 and d <= 64 (dyadic, so the lcm is the larger).
        // Q2.signum squares both coefficients and compares a*a with 2*b*b as Rationals, whose compareTo
        // cross-multiplies: the worst products are num(2*b*b) * den(a*a) and num(a*a) * den(2*b*b).
        val maxNum = BigInteger.valueOf(2L * 65536 * 64)
        val maxDen = BigInteger.valueOf(64)
        val analytic = maxNum.pow(2).multiply(BigInteger.TWO).multiply(maxDen.pow(2)).bitLength()
        println("DA-63 analytic worst intermediate: < 2^$analytic")
        assertTrue(analytic <= 62)
        var measured = 0
        val rats = bigNums.flatMap { n -> dens.map { d -> Rational.of(n, d) } }.distinct()
        for (a in rats) for (c in rats) for (b in rats) {
            val da = a - c
            val db = b - c
            val aa = da * da
            val bb = Rational.of(2) * (db * db)
            val x = BigInteger.valueOf(aa.numerator).multiply(BigInteger.valueOf(bb.denominator)).abs()
            val y = BigInteger.valueOf(bb.numerator).multiply(BigInteger.valueOf(aa.denominator)).abs()
            measured = maxOf(measured, x.bitLength(), y.bitLength())
        }
        println("DA-63 measured worst intermediate over the extreme grid: < 2^$measured")
        assertTrue(measured <= 62)
    }

    @Test
    fun coprimeDenominatorsAtTheMagnitudeLimitDoOverflow() { // decision DA-63: why the bound is dyadic
        val p = Q2(Rational.of(65536, 63), Rational.of(-65535, 61))
        val q = Q2(Rational.of(-65535, 61), Rational.of(65536, 63))
        var threw = false
        try {
            p.compareTo(q)
        } catch (e: ArithmeticException) {
            threw = true
        }
        if (!threw) fail("coprime 63 / 61 at the magnitude limit did not overflow: the documented reason for the dyadic bound is wrong")
        threw = false
        try {
            READING_ORDER.compare(ExactPoint(p, Q2.ZERO), ExactPoint(q, Q2.ZERO))
        } catch (e: ArithmeticException) {
            threw = true
        }
        assertTrue("READING_ORDER must overflow on the coprime pair too", threw)
    }
}
