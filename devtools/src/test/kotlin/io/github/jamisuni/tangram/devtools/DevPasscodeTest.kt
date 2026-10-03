package io.github.jamisuni.tangram.devtools

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// SCAFFOLDING (decision DA-78): the passcode is one standalone constant.
class DevPasscodeTest {

    @Test
    fun acceptsOnlyTheExactCode() {
        assertTrue(DevPasscode.accepts("0417"))
        for (bad in listOf("1234", "", "04170", "417", " 0417")) assertFalse(bad, DevPasscode.accepts(bad))
    }

    @Test
    fun mainSourcesHoldExactlyOneLiteral() {
        val root = File(System.getProperty("repo.root")!!, "devtools/src/main")
        val count = root.walkTopDown().filter { it.isFile && it.extension == "kt" }
            .sumOf { f -> "\"0417\"".toRegex().findAll(f.readText()).count() }
        assertEquals(1, count)
    }
}
