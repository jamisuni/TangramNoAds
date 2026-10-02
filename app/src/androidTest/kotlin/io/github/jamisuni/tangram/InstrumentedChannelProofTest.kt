package io.github.jamisuni.tangram

// SCAFFOLDING (TASK-011): proves the instrumented channel runs on the emulator. Not an acceptance test.
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class InstrumentedChannelProofTest {
    @Test
    fun targetContextHasAppPackage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("io.github.jamisuni.tangram", context.packageName)
    }
}
