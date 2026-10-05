package io.github.jamisuni.tangram.language

import io.github.jamisuni.tangram.promise.RepoScan
import io.github.jamisuni.tangram.promise.StringEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// decision DA-103: DECISION / SCAFFOLDING TEST (WO-006 T6c), no requirement token. DA-103 reads REQ-047's "everything in Finnish" as
// "every key of every module's values/ has a Finnish value that is not just the English text", except a named neutral list of
// language-neutral values. Static half only; the on-screen half is the device language walk. The repo root comes from `repo.root`
// (loud when missing); modules are found by their `src/main/res/values/strings.xml`. Plurals and string-arrays are compared item by
// item (key "name[index]").
class UntranslatedStringsTest {
    private val root: File = RepoScan.repoRoot()

    /**
     * Language-neutral values (design WO-006 section 5: digits and the counter, the dash, the size marks S/M/L, the app name, symbols).
     * Each entry is "module:key" -> the one value both languages must have: a neutral key whose value later changes is no longer
     * neutral and fails here until it is translated or the list is edited on purpose (CR-5 N4).
     */
    private val neutral: Map<String, String> = mapOf(
        "app:app_name" to "Tangram", // the app name, a proper noun
        "play:size_mark_large" to "L", // size mark L (REQ-047 Rules: the same in both languages)
        "play:size_mark_medium" to "M",
        "play:size_mark_small" to "S",
        "browse:best_time_none" to "–", // a dash
        "browse:puzzle_counter" to "%1\$d / %2\$d", // digits and a slash
        "browse:grid_cell_description" to "%1\$d. %2\$s · %3\$s", // captured parts only; the captures are checked by the language walk
        "browse:time_minutes_seconds" to "%1\$d:%2\$02d", // digits, m:ss
        // WO-008 (DA-141, design section 8 inventory): the time formats are language-neutral digits and the units "h" / "min"
        // (prototype 0.6, F14), identical in fi and en and in the three modules that format a time. TimeStringsEqualTest pins the copies equal.
        "time:time_minutes_seconds" to "%1\$d:%2\$02d",
        "time:time_hours_minutes" to "%1\$d h %2\$d min",
        "settings:time_minutes_seconds" to "%1\$d:%2\$02d",
        "settings:time_hours_minutes" to "%1\$d h %2\$d min",
        "browse:time_hours_minutes" to "%1\$d h %2\$d min",
        "devtools:devtools_button" to "DEV", // the pill symbol
        "devtools:devtools_ok" to "OK", // the same word
    )

    private fun load(): Pair<Map<String, StringEntry>, Map<String, StringEntry>> {
        val en = LinkedHashMap<String, StringEntry>()
        val fi = LinkedHashMap<String, StringEntry>()
        val modules = RepoScan.modulesWithStrings(root)
        assertTrue("no module with strings found: the check would be blind", modules.isNotEmpty())
        for (m in modules) {
            val res = "$m/src/main/res"
            for (e in RepoScan.parseStrings(File(root, "$res/values/strings.xml"), m, "values", "$res/values/strings.xml")) en["$m:${e.key}"] = e
            val fiFile = File(root, "$res/values-fi/strings.xml")
            if (fiFile.isFile) for (e in RepoScan.parseStrings(fiFile, m, "values-fi", "$res/values-fi/strings.xml")) fi["$m:${e.key}"] = e
        }
        return en to fi
    }

    /** The comparison itself, over any pair of maps: the problems found (empty = every non-neutral key has a distinct Finnish value). */
    internal fun problemsFor(en: Map<String, StringEntry>, fi: Map<String, StringEntry>, neutral: Map<String, String>): List<String> {
        val problems = ArrayList<String>()
        for ((id, e) in en) {
            val f = fi[id]
            val want = neutral[id]
            when {
                want != null -> {
                    if (e.value != want) problems += "$id is on the neutral list as \"$want\" but its English value is \"${e.value}\""
                    if (f == null) problems += "$id has no Finnish value (values-fi lacks it)"
                    else if (f.value != want) problems += "$id is on the neutral list as \"$want\" but its Finnish value is \"${f.value}\""
                }
                f == null -> problems += "$id has no Finnish value (values-fi lacks it)"
                f.value.isBlank() -> problems += "$id has an empty Finnish value"
                f.value == e.value -> problems += "$id is untranslated: fi = en = \"${e.value}\""
            }
        }
        return problems
    }

    @Test fun everyNonNeutralKeyHasADistinctFinnishValue() {
        val (en, fi) = load()
        assertTrue("fewer keys than expected: ${en.size}", en.size > 30)
        val problems = problemsFor(en, fi, neutral)
        assertTrue("untranslated or missing Finnish strings (DA-103): $problems", problems.isEmpty())
    }

    @Test fun theNeutralListNamesRealKeys() {
        val (en, _) = load()
        val stale = neutral.keys.filter { it !in en }
        assertTrue("neutral entries naming no key (stale): $stale", stale.isEmpty())
    }

    // positive control (CR-5 N3): the REAL comparison over injected maps finds each kind of problem and passes a clean pair
    @Test fun theComparisonCanFail() {
        fun e(key: String, v: String, q: String) = "m:$key" to StringEntry("m", q, key, v, "x")
        val en = mapOf(e("ok", "Done", "values"), e("same", "Next", "values"), e("gone", "Back", "values"), e("blank", "Stop", "values"), e("n", "OK", "values"), e("list[0]", "One", "values"), e("list[1]", "Two", "values"))
        val fi = mapOf(e("ok", "Valmis", "values-fi"), e("same", "Next", "values-fi"), e("blank", " ", "values-fi"), e("n", "Selva", "values-fi"), e("list[0]", "Yksi", "values-fi"), e("list[1]", "Two", "values-fi"))
        val problems = problemsFor(en, fi, mapOf("m:n" to "OK"))
        assertEquals("one problem per fault: $problems", 5, problems.size)
        assertTrue(problems.any { it.startsWith("m:same ") && "untranslated" in it })
        assertTrue(problems.any { it.startsWith("m:gone ") && "no Finnish" in it })
        assertTrue(problems.any { it.startsWith("m:blank ") && "empty" in it })
        assertTrue(problems.any { it.startsWith("m:n ") && "neutral" in it })
        assertTrue(problems.any { it.startsWith("m:list[1] ") && "untranslated" in it })
        val clean = problemsFor(mapOf(e("ok", "Done", "values")), mapOf(e("ok", "Valmis", "values-fi")), emptyMap())
        assertTrue(clean.isEmpty())
    }
}
