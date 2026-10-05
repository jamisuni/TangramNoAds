package io.github.jamisuni.tangram.language

import io.github.jamisuni.tangram.promise.RepoScan
import io.github.jamisuni.tangram.promise.StringEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// decision DA-141: DECISION / SCAFFOLDING TEST (WO-008 T8c part i), no requirement token.
// Design WO-008 section 6 and seam row 3: the format strings `time_minutes_seconds` and `time_hours_minutes` exist in `time`, `settings`
// and `browse` (three copies each, because G-06 forbids the modules from sharing resources), "fi and en identical". Nothing else keeps
// the copies in step, so this check pins them equal across the three modules and between the two languages.
// The repo root comes from `repo.root` (loud when missing), as the other app string tests read it.
class TimeStringsEqualTest {
    private val root: File = RepoScan.repoRoot()

    private val keys = listOf("time_minutes_seconds", "time_hours_minutes")
    private val modules = listOf("time", "settings", "browse")

    private fun read(module: String, qualifier: String): Map<String, StringEntry> {
        val rel = "$module/src/main/res/$qualifier/strings.xml"
        val f = File(root, rel)
        if (!f.isFile) error("$rel does not exist: the $module copy of the time formats is missing (seam row 3)")
        return RepoScan.parseStrings(f, module, qualifier, rel).associateBy { it.key }
    }

    /**
     * The comparison itself over any list of copies ("module/qualifier" -> value): the problems found. Empty = at most one distinct value,
     * and a copy that is missing is a problem (a copy that does not exist cannot be equal to the others).
     */
    internal fun problemsFor(key: String, copies: Map<String, String?>): List<String> {
        val problems = ArrayList<String>()
        for ((where, v) in copies) if (v == null) problems += "$key is missing in $where"
        val distinct = copies.values.filterNotNull().distinct()
        if (distinct.size > 1) problems += "$key differs between copies: " + copies.entries.joinToString { "${it.key}=\"${it.value}\"" }
        return problems
    }

    @Test fun theTwoFormatStringsAreEqualInTimeSettingsAndBrowseAndInFiAndEn() {
        val perCopy = LinkedHashMap<String, Map<String, StringEntry>>()
        for (m in modules) for (q in listOf("values", "values-fi")) perCopy["$m/$q"] = read(m, q)
        assertEquals("three modules x two languages", 6, perCopy.size)
        for (key in keys) {
            val problems = problemsFor(key, perCopy.mapValues { it.value[key]?.value })
            assertTrue("the copies of $key must be identical in time, settings, browse, fi and en (DA-141): $problems", problems.isEmpty())
        }
    }

    // positive control: the REAL comparison finds a differing copy and a missing one, and passes an equal set
    @Test fun theComparisonCanFail() {
        val equal = mapOf("time/values" to "%1\$d:%2\$02d", "time/values-fi" to "%1\$d:%2\$02d", "settings/values" to "%1\$d:%2\$02d")
        assertTrue(problemsFor("k", equal).isEmpty())
        val differs = equal + ("browse/values" to "%1\$d.%2\$02d")
        assertTrue(problemsFor("k", differs).any { "differs" in it })
        val missing = equal + ("browse/values-fi" to null)
        assertTrue(problemsFor("k", missing).any { "missing in browse/values-fi" in it })
    }
}
