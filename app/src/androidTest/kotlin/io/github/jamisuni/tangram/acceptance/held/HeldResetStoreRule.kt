package io.github.jamisuni.tangram.acceptance.held

import androidx.test.platform.app.InstrumentationRegistry
import io.github.jamisuni.tangram.store.JsonProgressStore
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File

/**
 * Test helper (design WO-004 section 7, DA-62): the app persists now, so every device test starts from a clean store.
 * Use it as the OUTER rule of a `RuleChain`, so it deletes `filesDir/progress*` before the activity launches.
 * It is a test helper, not a product hook.
 */
class ResetStoreRule : TestWatcher() {
    override fun starting(description: Description) = AppStore.wipe()
    override fun finished(description: Description) = AppStore.wipe()
}

/** Access to the app's real store file from a device test (design section 7 "Seeding recipe for tests"). */
object AppStore {
    private val filesDir: File get() = InstrumentationRegistry.getInstrumentation().targetContext.filesDir

    fun wipe() {
        filesDir.listFiles { f -> f.name.startsWith("progress") }?.forEach { it.deleteRecursively() }
    }

    /** A store over the app's `filesDir`: write through it after the rule wiped and before the activity launches. */
    fun open(): JsonProgressStore = JsonProgressStore(filesDir)
}
