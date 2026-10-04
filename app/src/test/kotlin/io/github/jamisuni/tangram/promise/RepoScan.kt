package io.github.jamisuni.tangram.promise

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

// Shared plumbing of the WO-006 JVM scans (guardrail G-01, decision DA-103). No requirement token: scaffolding.
// The repository root comes from the `repo.root` system property that app/build.gradle.kts passes (DA-88); a missing property is
// a loud error, never a silent pass (precedent: ReleaseSeparationTest). The scanned file set is exactly the glob list that
// app/build.gradle.kts declares as `scannedFileGlobs` (design WO-006 seam row 2), read from that file here so the test and the
// build's declared inputs cannot drift apart.

/** One string resource: [module] is the top directory, [qualifier] is "values" or "values-fi" and so on. */
internal data class StringEntry(val module: String, val qualifier: String, val key: String, val value: String, val file: String)

internal object RepoScan {
    private val SKIPPED_DIR_NAMES = setOf("build", ".gradle", ".git", ".idea", ".swdev", "Study", "Requirements", "node_modules")

    fun repoRoot(): File {
        val p = System.getProperty("repo.root")
        if (p.isNullOrBlank()) {
            error("system property repo.root is not set: the promise scans cannot find the repository (app/build.gradle.kts must pass it, DA-88)")
        }
        val f = File(p)
        if (!f.isDirectory || !File(f, "settings.gradle.kts").isFile) error("repo.root=$p is not the repository root (no settings.gradle.kts there)")
        return f
    }

    /** The glob list app/build.gradle.kts declares as `scannedFileGlobs`. Fails loudly when it cannot be read. */
    fun declaredGlobs(root: File): List<String> {
        val build = File(root, "app/build.gradle.kts")
        if (!build.isFile) error("app/build.gradle.kts not found under $root")
        val m = Regex("""val\s+scannedFileGlobs\s*=\s*listOf\(([^)]*)\)""").find(build.readText())
            ?: error("app/build.gradle.kts declares no `val scannedFileGlobs = listOf(...)`: the scan inputs (TASK-040) are missing")
        val globs = Regex(""""([^"]+)"""").findAll(m.groupValues[1]).map { it.groupValues[1] }.toList()
        if (globs.isEmpty()) error("scannedFileGlobs in app/build.gradle.kts is empty")
        return globs
    }

    fun globToRegex(glob: String): Regex {
        val sb = StringBuilder("^")
        var i = 0
        while (i < glob.length) {
            val c = glob[i]
            when {
                glob.startsWith("**/", i) -> { sb.append("(?:.*/)?"); i += 3 }
                glob.startsWith("/**", i) && i + 3 == glob.length -> { sb.append("(?:/.*)?"); i += 3 }
                glob.startsWith("**", i) -> { sb.append(".*"); i += 2 }
                c == '*' -> { sb.append("[^/]*"); i++ }
                c == '?' -> { sb.append("[^/]"); i++ }
                else -> { sb.append(Regex.escape(c.toString())); i++ }
            }
        }
        return Regex(sb.append("$").toString())
    }

    /** Every file under [root] (never build output or tool folders) as a path relative to [root] with forward slashes. */
    fun allFiles(root: File): List<String> {
        val out = ArrayList<String>()
        fun walk(dir: File, prefix: String) {
            val kids = dir.listFiles() ?: return
            for (k in kids.sortedBy { it.name }) {
                if (k.isDirectory) {
                    if (k.name in SKIPPED_DIR_NAMES) continue
                    walk(k, "$prefix${k.name}/")
                } else out += "$prefix${k.name}"
            }
        }
        walk(root, "")
        return out
    }

    class Scanned(val globs: List<String>, val byGlob: Map<String, List<String>>, val files: List<String>)

    /** The files the declared globs select. A glob that selects nothing is an error: a dead input would blind the scan. */
    fun scannedFiles(root: File): Scanned {
        val globs = declaredGlobs(root)
        val all = allFiles(root)
        val by = LinkedHashMap<String, List<String>>()
        for (g in globs) {
            val re = globToRegex(g)
            val hits = all.filter { re.matches(it) }
            if (hits.isEmpty()) error("the declared glob \"$g\" selects no file under $root")
            by[g] = hits
        }
        return Scanned(globs, by, by.values.flatten().distinct().sorted())
    }

    /** The product modules: the top-level directories that carry `src/main/res/values/strings.xml`. */
    fun modulesWithStrings(root: File): List<String> =
        (root.listFiles() ?: emptyArray()).filter { it.isDirectory && !it.name.startsWith(".") && File(it, "src/main/res/values/strings.xml").isFile }
            .map { it.name }.sorted()

    /** Parses one strings.xml: `<string>`, `<plurals>` / `<string-array>` items too (key = the resource name). */
    fun parseStrings(file: File, module: String, qualifier: String, relPath: String): List<StringEntry> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val out = ArrayList<StringEntry>()
        val kids = doc.documentElement.childNodes
        for (i in 0 until kids.length) {
            val n = kids.item(i) as? Element ?: continue
            val name = n.getAttribute("name")
            when (n.tagName) {
                "string" -> out += StringEntry(module, qualifier, name, unescape(n.textContent), relPath)
                "plurals", "string-array" -> {
                    val items = n.getElementsByTagName("item")
                    // one entry per item, keyed "name[index]": items are compared by index, never collapsed to the last one
                    for (j in 0 until items.length) out += StringEntry(module, qualifier, "$name[$j]", unescape(items.item(j).textContent), relPath)
                }
            }
        }
        return out
    }

    fun unescape(s: String): String = s.replace("\\'", "'").replace("\\\"", "\"").replace("\\n", "\n").replace("\\@", "@").replace("\\?", "?")

    /** Every string of every module and every qualifier folder under src/main/res (and the other `values*` variants the globs select). */
    fun allStrings(root: File): List<StringEntry> {
        val out = ArrayList<StringEntry>()
        val re = Regex("""^([^/]+)/src/[^/]+/res/(values[^/]*)/strings\.xml$""")
        for (rel in allFiles(root)) {
            val m = re.matchEntire(rel) ?: continue
            out += parseStrings(File(root, rel), m.groupValues[1], m.groupValues[2], rel)
        }
        return out
    }

    class PuzzleTitle(val file: String, val lang: String, val text: String)

    /** The `title` of every puzzle file (`"format": "tangram-puzzle/1"`); puzzle.schema.json is not a puzzle. */
    fun puzzleTitles(root: File): List<PuzzleTitle> {
        val out = ArrayList<PuzzleTitle>()
        val dir = File(root, "Tangrams")
        for (f in (dir.listFiles() ?: emptyArray()).filter { it.isFile && it.name.endsWith(".json") }.sortedBy { it.name }) {
            val text = f.readText()
            if (!Regex(""""format"\s*:\s*"tangram-puzzle/1"""").containsMatchIn(text)) continue
            val obj = Regex(""""title"\s*:\s*\{([^{}]*)\}""").find(text)?.groupValues?.get(1) ?: error("${f.name}: no title object")
            val pairs = Regex(""""(\w+)"\s*:\s*"((?:[^"\\]|\\.)*)"""").findAll(obj).toList()
            if (pairs.isEmpty()) error("${f.name}: title object has no entries")
            for (p in pairs) out += PuzzleTitle("Tangrams/${f.name}", p.groupValues[1], jsonUnescape(p.groupValues[2]))
        }
        return out
    }

    private fun jsonUnescape(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                val d = s[i + 1]
                when (d) {
                    'u' -> { sb.append(s.substring(i + 2, i + 6).toInt(16).toChar()); i += 6; continue }
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    else -> sb.append(d)
                }
                i += 2
            } else { sb.append(c); i++ }
        }
        return sb.toString()
    }
}
