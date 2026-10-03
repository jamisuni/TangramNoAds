package io.github.jamisuni.tangram.browse

// SCAFFOLDING (TASK-021): proves the instrumented channel runs for :browse on the emulator. Not an acceptance test.
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class BrowseChannelProofTest {
    @Test
    fun targetContextHasBrowsePackage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("io.github.jamisuni.tangram.browse.test", context.packageName)
    }
}
