package io.github.jamisuni.tangram.kernel.geometry

import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Rational

// Exact arithmetic for the kernel (TYPE-004 "exact", architecture.md G-03, ADR-003; design WO-001 §2).
// Extensions on the contract-referenced value types of kernel.model, built only on their public
// constructors (`Rational.of`, `Q2(a, b)`, `ExactPoint(x, y)`), so kernel.model stays untouched.
// Overflow throws through `Math.*Exact` (G-10: a programmer error, impossible at puzzle scale).
// No double ever decides an order or an equality of exact values.

// ---- Rational ---------------------------------------------------------------------------------

operator fun Rational.plus(other: Rational): Rational =
    Rational.of(
        Math.addExact(
            Math.multiplyExact(numerator, other.denominator),
            Math.multiplyExact(other.numerator, denominator),
        ),
        Math.multiplyExact(denominator, other.denominator),
    )

operator fun Rational.minus(other: Rational): Rational =
    Rational.of(
        Math.subtractExact(
            Math.multiplyExact(numerator, other.denominator),
            Math.multiplyExact(other.numerator, denominator),
        ),
        Math.multiplyExact(denominator, other.denominator),
    )

operator fun Rational.times(other: Rational): Rational =
    Rational.of(
        Math.multiplyExact(numerator, other.numerator),
        Math.multiplyExact(denominator, other.denominator),
    )

operator fun Rational.unaryMinus(): Rational = Rational.of(Math.negateExact(numerator), denominator)

/** -1, 0 or +1. The denominator is always positive, so the sign is the numerator's. */
fun Rational.signum(): Int = java.lang.Long.signum(numerator)

// ---- Q2 (a + b·√2) ----------------------------------------------------------------------------

operator fun Q2.plus(other: Q2): Q2 = Q2(a + other.a, b + other.b)

operator fun Q2.minus(other: Q2): Q2 = Q2(a - other.a, b - other.b)

/** (a + b√2)(c + d√2) = (ac + 2bd) + (ad + bc)√2. */
operator fun Q2.times(other: Q2): Q2 =
    Q2(
        a * other.a + TWO * (b * other.b),
        a * other.b + b * other.a,
    )

operator fun Q2.unaryMinus(): Q2 = Q2(-a, -b)

/**
 * The exact sign of a + b√2: -1, 0 or +1.
 * With sa, sb the signs of a and b: sa == 0 gives sb; sb == 0 gives sa; equal signs give sa
 * (no cancellation). Opposite signs: compare a² with 2b² as Rationals; a² > 2b² gives sa, else sb.
 * The two are never equal for (a, b) != (0, 0) because √2 is irrational.
 */
fun Q2.signum(): Int {
    val sa = a.signum()
    val sb = b.signum()
    if (sa == 0) return sb
    if (sb == 0) return sa
    if (sa == sb) return sa
    return if (a * a > TWO * (b * b)) sa else sb
}

/** Exact order, so `<`, `>`, `<=`, `>=` work on [Q2] (it is not `Comparable<Q2>`). Equality stays `==`. */
operator fun Q2.compareTo(other: Q2): Int = (this - other).signum()

/** a + b·√2 from whole numbers. */
fun q2(a: Long, b: Long = 0L): Q2 = Q2(Rational.of(a), Rational.of(b))

private val TWO: Rational = Rational.of(2)

// ---- ExactPoint -------------------------------------------------------------------------------

operator fun ExactPoint.plus(other: ExactPoint): ExactPoint = ExactPoint(x + other.x, y + other.y)

operator fun ExactPoint.minus(other: ExactPoint): ExactPoint = ExactPoint(x - other.x, y - other.y)

/** A point with whole-number coordinates. */
fun pointOf(x: Long, y: Long): ExactPoint = ExactPoint(q2(x), q2(y))

/** Reading order (y first, then x), the exact tie-break of the lock search (design WO-001 §6, DA-1). */
internal val READING_ORDER: Comparator<ExactPoint> = Comparator { p, q ->
    val byY = p.y.compareTo(q.y)
    if (byY != 0) byY else p.x.compareTo(q.x)
}
