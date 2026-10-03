package io.github.jamisuni.tangram.acceptance

import io.github.jamisuni.tangram.content.PuzzleLibrary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * decision DA-22 - the on-device library check: "ids equal the index stems" (equivalent to `rejected` being empty). The test
 * APK runs in the app's process, so the classpath resource `tangrams/index.txt` is the one the app ships.
 */
class LibraryOnDeviceTest {

    @Test
    fun decision_DA22_everyIndexedPuzzleIsInTheLibraryOnTheDevice() {
        val loader = Thread.currentThread().contextClassLoader!!
        val index = loader.getResourceAsStream("tangrams/index.txt")
        assertNotNull("tangrams/index.txt is packaged", index)
        val stems = index!!.bufferedReader().readLines().map { it.trim() }.filter { it.isNotEmpty() }
            .map { it.removeSuffix(".json") }
        assertTrue("fixture: the index lists the shipped puzzles", stems.size >= 13)
        val ids = PuzzleLibrary.packaged().puzzles.map { it.id.value }
        assertEquals("every file in the index parsed on the device", stems.toSet(), ids.toSet())
        assertEquals("no duplicates", stems.size, ids.size)
    }
}
