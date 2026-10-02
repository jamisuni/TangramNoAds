package io.github.jamisuni.tangram.kernel.model

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * An exact rational number, always normalized: the denominator is positive and
 * shares no factor with the numerator, so equal values are equal objects.
 * Lock-decisive geometry is exact (AGENTS.md rule 6, TYPE-004, ADR-003).
 * Arithmetic overflow is a programmer error and throws (architecture.md G-10).
 */
class Rational private constructor(val numerator: Long, val denominator: Long) : Comparable<Rational> {

    fun toDouble(): Double = numerator.toDouble() / denominator

    override fun compareTo(other: Rational): Int =
        Math.multiplyExact(numerator, other.denominator)
            .compareTo(Math.multiplyExact(other.numerator, denominator))

    override fun equals(other: Any?): Boolean =
        other is Rational && numerator == other.numerator && denominator == other.denominator

    override fun hashCode(): Int = 31 * numerator.hashCode() + denominator.hashCode()

    override fun toString(): String = if (denominator == 1L) "$numerator" else "$numerator/$denominator"

    companion object {
        val ZERO = Rational(0, 1)
        val ONE = Rational(1, 1)

        fun of(numerator: Long, denominator: Long = 1): Rational {
            require(denominator != 0L) { "denominator must not be zero" }
            // abs() and negation overflow at Long.MIN_VALUE (ADR-003: overflow is asserted).
            require(numerator != Long.MIN_VALUE && denominator != Long.MIN_VALUE) { "rational out of range" }
            val g = gcd(abs(numerator), abs(denominator))
            val sign = if (denominator < 0) -1 else 1
            return Rational(sign * numerator / g, sign * denominator / g)
        }

        private tailrec fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)
    }
}

/**
 * An exact number a + b·√2 with rational a and b. Turning a piece by 45°
 * (TYPE-003) introduces √2, so puzzle coordinates live in this number field
 * (Spec/03-puzzle-format.md §1: the JSON pair `[a, b]`).
 */
data class Q2(val a: Rational, val b: Rational) {

    fun toDouble(): Double = a.toDouble() + b.toDouble() * SQRT2

    override fun toString(): String = if (b == Rational.ZERO) "$a" else "$a + $b·√2"

    companion object {
        val ZERO = Q2(Rational.ZERO, Rational.ZERO)
        private val SQRT2 = sqrt(2.0)

        fun of(a: Long): Q2 = Q2(Rational.of(a), Rational.ZERO)
    }
}

/** An exact point in puzzle units: x to the right, y downwards (Spec/03-puzzle-format.md §1). */
data class ExactPoint(val x: Q2, val y: Q2)
