package io.github.jamisuni.tangram.content

import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.Q2
import io.github.jamisuni.tangram.kernel.model.Rational
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

/**
 * Puzzle-file numbers to exact values (G-03, ADR-004): the JSON token text is read as a decimal,
 * never through a Double. Every failure throws a RuntimeException; the parser's single try turns it into Rejected.
 */
internal object ExactNumbers {

    private const val MAX_POW10 = 18
    private val JSON_NUMBER = Regex("^-?(0|[1-9][0-9]*)([.][0-9]+)?([eE][+-]?[0-9]+)?$")

    /** A JSON number token (not a string, not null) as an exact rational. */
    fun rational(element: JsonElement): Rational {
        require(element is JsonPrimitive && element !is JsonNull && !element.isString) { "not a JSON number: $element" }
        // kotlinx lets NaN, Infinity, +1 and 0x10 through as literals; only the JSON number grammar is accepted.
        require(JSON_NUMBER.matches(element.content)) { "not a JSON number: ${element.content}" }
        val value = BigDecimal(element.content).stripTrailingZeros()
        val scale = value.scale()
        return if (scale >= 0) {
            require(scale <= MAX_POW10) { "too many decimals: ${element.content}" }
            Rational.of(value.unscaledValue().longValueExact(), BigInteger.TEN.pow(scale).longValueExact())
        } else {
            require(-scale <= MAX_POW10) { "exponent out of range: ${element.content}" }
            Rational.of(value.unscaledValue().multiply(BigInteger.TEN.pow(-scale)).longValueExact(), 1)
        }
    }

    /** A number `a` or a pair `[a, b]` = a + b*sqrt(2). */
    fun coordinate(element: JsonElement): Q2 = when (element) {
        is JsonArray -> {
            require(element.size == 2) { "a coordinate pair has two numbers" }
            Q2(rational(element[0]), rational(element[1]))
        }
        else -> Q2(rational(element), Rational.ZERO)
    }

    /** A point: an array of exactly two coordinates. */
    fun point(element: JsonElement): ExactPoint {
        require(element is JsonArray && element.size == 2) { "a point has two coordinates" }
        return ExactPoint(coordinate(element[0]), coordinate(element[1]))
    }
}
