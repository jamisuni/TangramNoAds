package io.github.jamisuni.tangram

import org.junit.Assert.assertEquals
import org.junit.Test

// Scaffolding for the G3 toolchain proof: proves Kotlin compiles and JUnit runs
// under `gradlew testDebugUnitTest`. Not an acceptance test; carries no REQ token.
class ToolchainProofTest {
    @Test
    fun kotlinCompilesAndJUnitRuns() {
        assertEquals(4, 2 + 2)
    }
}
