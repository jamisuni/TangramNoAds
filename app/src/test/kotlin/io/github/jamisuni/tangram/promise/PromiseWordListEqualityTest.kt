package io.github.jamisuni.tangram.promise

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// guardrail G-01: GUARDRAIL TEST (WO-006 T6c, staged SEPARATELY; moves in at MOVE-DEV6, after the device copy exists, plan-review F1).
// The promise word lists are one constant with two compiled copies: the JVM copy
//   app/src/test/kotlin/io/github/jamisuni/tangram/promise/PromiseWords.kt
// and the device copy
//   app/src/androidTest/kotlin/io/github/jamisuni/tangram/acceptance/promise/PromiseWords.kt
// (design WO-006 section 6 item 4, D1: "asserted equal by a JVM test reading both files"). Equal means: the same text after the
// package line, the comments and the whitespace are dropped, so the lists, the exempt-key set, the allowed words, the currency
// patterns and the matcher are identical. Both files are read through `repo.root` (loud when missing, loud when a file is absent:
// never a silent pass).
class PromiseWordListEqualityTest {
    private val root: File = RepoScan.repoRoot()
    private val jvmPath = "app/src/test/kotlin/io/github/jamisuni/tangram/promise/PromiseWords.kt"
    private val devicePath = "app/src/androidTest/kotlin/io/github/jamisuni/tangram/acceptance/promise/PromiseWords.kt"

    private fun normalised(rel: String): String {
        val f = File(root, rel)
        if (!f.isFile) error("$rel does not exist: the word-list copies cannot be compared")
        var s = f.readText()
        s = Regex("(?m)^\\s*package\\s+[\\w.]+\\s*$").replace(s, "")
        s = Regex("(?s)/\\*.*?\\*/").replace(s, "")
        s = Regex("(?m)(?<![\"\\\\:])//.*$").replace(s, "") // a line comment, not a "//" inside a literal
        return s.replace(Regex("\\s+"), " ").trim()
    }

    @Test fun theDeviceCopyOfThePromiseWordsEqualsTheJvmCopy() {
        val jvm = normalised(jvmPath)
        val device = normalised(devicePath)
        assertTrue("the JVM copy normalised to nothing: the comparison would be blind", jvm.length > 500)
        assertEquals("PromiseWords.kt differs between the JVM and the device copy (compare after dropping package and comments)", jvm, device)
    }
}
