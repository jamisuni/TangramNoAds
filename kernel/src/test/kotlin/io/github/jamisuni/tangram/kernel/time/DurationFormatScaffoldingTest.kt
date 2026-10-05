package io.github.jamisuni.tangram.kernel.time

import org.junit.Test
import org.junit.Assert.assertEquals

// Implementer scaffolding (disposable); the acceptance tests are the Test Author's.
class DurationFormatScaffoldingTest {
    @Test fun zero() = assertEquals(DurationParts(0, 0, 0, false), DurationFormat.parts(0))
    @Test fun negativeReadsAsZero() = assertEquals(DurationParts(0, 0, 0, false), DurationFormat.parts(-5))
    @Test fun justBelowHour() = assertEquals(DurationParts(0, 59, 59, false), DurationFormat.parts(3599))
    @Test fun exactHour() = assertEquals(DurationParts(1, 0, 0, true), DurationFormat.parts(3600))
    @Test fun hourSecondsFlooredAway() = assertEquals(DurationParts(2, 3, 0, true), DurationFormat.parts(2 * 3600 + 3 * 60 + 59))
}
