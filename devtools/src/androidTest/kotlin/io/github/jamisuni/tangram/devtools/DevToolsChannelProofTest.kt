package io.github.jamisuni.tangram.devtools

// SCAFFOLDING (TASK-030): proves the instrumented channel runs for :devtools on the emulator. Not an acceptance test.
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class DevToolsChannelProofTest {
    @Test
    fun targetContextHasDevToolsPackage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("io.github.jamisuni.tangram.devtools.test", context.packageName)
    }
}
