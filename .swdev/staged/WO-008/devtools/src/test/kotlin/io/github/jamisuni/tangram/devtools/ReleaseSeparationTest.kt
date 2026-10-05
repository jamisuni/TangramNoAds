package io.github.jamisuni.tangram.devtools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

// ACCEPTANCE TEST (TASK-T5, independent author). Source half of "a release build contains no DEV button" (the artifact half is
// V-04, a verifier). Written from REQ-046, G-04, DA-81, DA-88 and DA-89 only.
//
// The named file set S (DA-89, tasks.md WO-005 header; never `build/`, `.gradle/`, `.swdev/`, and never the `devtools` module):
//   <m>/src/main/**  for m in app, play, browse, kernel, contracts, content, store
//   app/src/release/**
//   app/build.gradle.kts
//   settings.gradle.kts
// Nothing in S may carry the text `devtools` (case-insensitive: a name, an import, a string, a comment, a path segment or a file
// name) or the passcode text `0417`, EXCEPT exactly these two whole lines (compared after trimming surrounding whitespace):
//   settings.gradle.kts : include(":devtools")
//   app/build.gradle.kts : debugImplementation(project(":devtools"))
// The repo root comes from the `repo.root` system property (DA-88: the build declares S as task inputs and passes the property);
// a missing property is a loud error, never a silent pass. The walk starts only at the named trees, so no `build/`, `.gradle/`
// or `.swdev/` directory is ever entered.

internal object SeparationScan {
    // WO-007 (seam row "ReleaseSeparationTest", a definite edit: the list is hard-coded): `settings` ships in release, so its
    // src/main is part of the scanned file set S.
    // WO-008 (plan review F5): `time` ships in release too, so its src/main joins S the moment it holds code, at LAND-A.
    val PRODUCT_MODULES = listOf("app", "play", "browse", "kernel", "contracts", "content", "store", "settings", "time")
    const val SETTINGS = "settings.gradle.kts"
    const val APP_BUILD = "app/build.gradle.kts"
    const val EXEMPT_SETTINGS_LINE = "include(\":devtools\")"
    const val EXEMPT_APP_LINE = "debugImplementation(project(\":devtools\"))"
    private val EXEMPT = mapOf(SETTINGS to EXEMPT_SETTINGS_LINE, APP_BUILD to EXEMPT_APP_LINE)

    class Hit(val file: String, val line: Int, val what: String, val text: String) {
        override fun toString() = "$file:$line [$what] ${text.trim()}"
    }

    class Result(val treeCounts: Map<String, Int>, val hits: List<Hit>, val exemptSeen: Map<String, Int>) {
        val fileCount: Int get() = treeCounts.values.sum()
    }

    fun repoRoot(): File {
        val p = System.getProperty("repo.root")
        if (p.isNullOrBlank()) {
            error("system property repo.root is not set: ReleaseSeparationTest cannot find the repository (devtools/build.gradle.kts must pass it, DA-88)")
        }
        val f = File(p)
        if (!f.isDirectory || !File(f, SETTINGS).isFile) error("repo.root=$p is not the repository root (no $SETTINGS there)")
        return f
    }

    fun scan(root: File): Result {
        val files = LinkedHashMap<String, File>()
        val counts = LinkedHashMap<String, Int>()
        fun rel(f: File) = f.relativeTo(root).invariantSeparatorsPath
        fun addTree(relDir: String) {
            val dir = File(root, relDir)
            if (!dir.isDirectory) error("the scanned file set S is incomplete: $relDir does not exist under $root")
            var n = 0
            dir.walkTopDown().filter { it.isFile }.forEach { files[rel(it)] = it; n++ }
            counts[relDir] = n
        }
        for (m in PRODUCT_MODULES) addTree("$m/src/main")
        addTree("app/src/release")
        for (f in listOf(SETTINGS, APP_BUILD)) {
            val file = File(root, f)
            if (!file.isFile) error("the scanned file set S is incomplete: $f does not exist under $root")
            files[f] = file
            counts[f] = 1
        }

        val hits = ArrayList<Hit>()
        val exemptSeen = LinkedHashMap<String, Int>()
        for ((path, file) in files) {
            if ("devtools" in path.lowercase()) hits += Hit(path, 0, "devtools in the path", path)
            val text = String(file.readBytes(), Charsets.ISO_8859_1).replace("\u0000", "")
            val exempt = EXEMPT[path]
            text.split('\n').forEachIndexed { i, raw ->
                val line = raw.trimEnd('\r')
                if (exempt != null && line.trim() == exempt) {
                    exemptSeen[path] = (exemptSeen[path] ?: 0) + 1
                    return@forEachIndexed
                }
                if ("devtools" in line.lowercase()) hits += Hit(path, i + 1, "devtools", line)
                if ("0417" in line) hits += Hit(path, i + 1, "passcode 0417", line)
            }
        }
        return Result(counts, hits, exemptSeen)
    }
}

/** Public `fun` declarations of a Kotlin source as normalised strings: annotations, name, parameters (with defaults), return type. */
internal fun publicFunSignatures(source: String): Set<String> {
    val text = source.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ").replace(Regex("//[^\\n]*"), " ")
    val re = Regex(
        "((?:@[\\w.]+(?:\\([^)]*\\))?\\s*)*)((?:(?:public|private|internal|protected|override|inline|open|suspend)\\s+)*)fun\\s+(\\w+)\\s*\\(",
    )
    val out = LinkedHashSet<String>()
    for (m in re.findAll(text)) {
        if (Regex("\\b(private|internal|protected)\\b").containsMatchIn(m.groupValues[2])) continue
        val open = m.range.last
        var depth = 0
        var end = -1
        var i = open
        while (i < text.length) {
            if (text[i] == '(') depth++
            if (text[i] == ')') {
                depth--
                if (depth == 0) { end = i; break }
            }
            i++
        }
        if (end < 0) error("unbalanced parentheses after fun ${m.groupValues[3]}")
        val params = text.substring(open + 1, end).filterNot { it.isWhitespace() }
        val after = text.substring(end + 1).takeWhile { it != '{' && it != '=' && it != '\n' }.filterNot { it.isWhitespace() }
        val annotations = Regex("@([\\w.]+)").findAll(m.groupValues[1]).map { it.groupValues[1] }.sorted().toList()
        out += "$annotations fun ${m.groupValues[3]}($params)$after"
    }
    return out
}

class ReleaseSeparationTest {

    @get:Rule
    val tmp = TemporaryFolder()

    // REQ-046.A4 - "A release build contains no DEV button."
    // Source half: no file of the release-side file set S names devtools or carries the passcode, apart from the two build lines
    // that wire the debug-only module in. The scanner must also be seen to read the real tree (every named tree non-empty, both
    // exempt lines present exactly once), so a wrong repo.root or an empty glob cannot produce a pass.
    @Test
    fun req046_A4_noReleaseSideFileNamesDevtoolsOrCarriesThePasscode() {
        val result = SeparationScan.scan(SeparationScan.repoRoot())
        for ((tree, n) in result.treeCounts) assertTrue("the scanner found no file under $tree: it is blind", n > 0)
        for (path in listOf(SeparationScan.SETTINGS, SeparationScan.APP_BUILD)) {
            assertEquals(
                "$path must hold the exempt wiring line exactly once (else the scan is blind or the debug-only wiring is wrong)",
                1,
                result.exemptSeen[path] ?: 0,
            )
        }
        assertTrue(
            "G-04 release separation broken, ${result.hits.size} hit(s) in ${result.fileCount} scanned files:\n" +
                result.hits.joinToString("\n"),
            result.hits.isEmpty(),
        )
    }

    // decision DA-72: the two `DebugAids` source sets have identical public function signatures (design WO-005 section 1: the
    // release stub is compile-checked against the debug class; a drift must be visible here as well).
    @Test
    fun decisionDA72_theDebugAndReleaseDebugAidsDeclareTheSamePublicFunctions() {
        val root = SeparationScan.repoRoot()
        val debug = File(root, "app/src/debug/kotlin/io/github/jamisuni/tangram/DebugAids.kt")
        val release = File(root, "app/src/release/kotlin/io/github/jamisuni/tangram/DebugAids.kt")
        assertTrue("missing $debug", debug.isFile)
        assertTrue("missing $release", release.isFile)
        val d = debug.readText()
        val r = release.readText()
        for ((name, src) in listOf("debug" to d, "release" to r)) {
            assertTrue("$name DebugAids.kt declares no class DebugAids", Regex("\\bclass\\s+DebugAids\\b").containsMatchIn(src))
        }
        val dSigs = publicFunSignatures(d)
        val rSigs = publicFunSignatures(r)
        assertTrue("no public function found in the debug DebugAids: the signature reader is blind", dSigs.isNotEmpty())
        assertEquals("debug and release DebugAids public functions differ", dSigs, rSigs)
    }

    // decision DA-72 (WO-007, DA-116 / DA-123): the release `DebugAids` twin passes the two feedback outs straight through. The test
    // probe (FeedbackProbe) is debug-only, so in release `sound(real)` and `haptic(real)` must hand back exactly their argument,
    // else a counting or altering wrapper could ship (design WO-007 seam row "DebugAids (both twins)": "release: returns `real`").
    @Test
    fun decisionDA72_theReleaseDebugAidsSoundAndHapticReturnTheirArgument() {
        val release = File(SeparationScan.repoRoot(), "app/src/release/kotlin/io/github/jamisuni/tangram/DebugAids.kt")
        assertTrue("missing $release", release.isFile)
        val src = release.readText()
        for (name in listOf("sound", "haptic")) {
            val body = passThroughBody(src, name)
            assertTrue("release DebugAids.$name is not a pass-through: $body", body.passesThrough)
        }
    }

    /** What a function `name(param: T): R` returns: [passesThrough] is true only when its whole body is `= param` or `{ return param }`. */
    internal class PassThrough(val param: String, val returned: String) {
        val passesThrough: Boolean get() = param == returned
        override fun toString() = "param `$param`, returns `$returned`"
    }

    internal fun passThroughBody(source: String, name: String): PassThrough {
        val text = source.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ").replace(Regex("//[^\\n]*"), " ")
        val re = Regex("fun\\s+$name\\s*\\(\\s*(\\w+)\\s*:\\s*[\\w.<>?]+\\s*\\)\\s*:\\s*[\\w.<>?]+\\s*(?:=\\s*([^\\n{]+)|\\{([^}]*)\\})")
        val matches = re.findAll(text).toList()
        if (matches.size != 1) error("expected exactly one `fun $name(x: T): R` in the release DebugAids, found ${matches.size}")
        val m = matches.single()
        val param = m.groupValues[1]
        val expr = m.groupValues[2].trim()
        val block = m.groupValues[3].trim()
        val returned = when {
            expr.isNotEmpty() -> expr
            block.startsWith("return ") -> block.removePrefix("return ").trim().trimEnd(';')
            else -> "<block: $block>"
        }
        return PassThrough(param, returned)
    }

    // decision DA-72 (control): the pass-through reader tells a wrapper from a pass-through, in both body forms.
    @Test
    fun decisionDA72_thePassThroughReaderCanFail() {
        val ok = "class DebugAids {\n    fun sound(real: SoundOut): SoundOut = real\n    fun haptic(real: HapticOut): HapticOut { return real }\n}\n"
        assertTrue(passThroughBody(ok, "sound").passesThrough)
        assertTrue(passThroughBody(ok, "haptic").passesThrough)
        val wrapped = "class DebugAids {\n    fun sound(real: SoundOut): SoundOut = Counting(real)\n    fun haptic(real: HapticOut): HapticOut { return Counting(real) }\n}\n"
        assertFalse(passThroughBody(wrapped, "sound").passesThrough)
        assertFalse(passThroughBody(wrapped, "haptic").passesThrough)
        val other = "class DebugAids {\n    fun sound(real: SoundOut): SoundOut = SilentSound\n    fun haptic(real: HapticOut): HapticOut { log(real); return real }\n}\n"
        assertFalse(passThroughBody(other, "sound").passesThrough)
        assertFalse("a block with a side effect is not a pass-through", passThroughBody(other, "haptic").passesThrough)
        var failed = false
        try {
            passThroughBody("class DebugAids\n", "sound")
        } catch (expected: IllegalStateException) {
            failed = true
        }
        assertTrue("a missing function is a loud error, never a pass", failed)
    }

    // ---------------------------------------------------------------------------------------------------- controls
    // The scanner is only trusted because it can fail: fixtures are small fake repositories built in a temp directory.

    private fun fakeRepo(extra: Map<String, String> = emptyMap(), drop: Set<String> = emptySet()): File {
        val base = linkedMapOf<String, String>()
        for (m in SeparationScan.PRODUCT_MODULES) base["$m/src/main/kotlin/X.kt"] = "package x\nclass X\n"
        base["app/src/release/kotlin/DebugAids.kt"] = "class DebugAids\n"
        base["settings.gradle.kts"] = "include(\":app\")\ninclude(\":devtools\")\n"
        base["app/build.gradle.kts"] = "dependencies {\n    implementation(project(\":play\"))\n    debugImplementation(project(\":devtools\"))\n}\n"
        base.putAll(extra)
        for (d in drop) base.remove(d)
        val root = tmp.newFolder()
        for ((path, content) in base) {
            val f = File(root, path)
            f.parentFile.mkdirs()
            f.writeText(content)
        }
        return root
    }

    // decision DA-89: a clean tree has no hit and shows both exempt lines once.
    @Test
    fun decisionDA89_aCleanTreePassesAndTheScannerSeesTheTwoExemptLines() {
        val r = SeparationScan.scan(fakeRepo())
        assertEquals(emptyList<String>(), r.hits.map { it.toString() })
        assertEquals(1, r.exemptSeen[SeparationScan.SETTINGS])
        assertEquals(1, r.exemptSeen[SeparationScan.APP_BUILD])
    }

    // decision DA-89: the word devtools is a hit in a comment, a string, an import, an xml value, an asset, a path and a file name.
    @Test
    fun decisionDA89_theWordDevtoolsAnywhereInSIsAHit() {
        val cases = mapOf(
            "play/src/main/kotlin/Y.kt" to "// uses devtools here\n",
            "browse/src/main/res/values/s.xml" to "<string name=\"a\">DevTools</string>\n",
            "kernel/src/main/kotlin/Z.kt" to "import io.github.jamisuni.tangram.devtools.DevToolsState\n",
            "app/src/release/kotlin/DebugAids.kt" to "import io.github.jamisuni.tangram.devtools.Whatever\nclass DebugAids\n",
            "content/src/main/assets/a.json" to "{\"k\":\"devtools_x\"}\n",
            "store/src/main/kotlin/a/devtools/Q.kt" to "class Q\n",
            "app/src/main/kotlin/DevTools.kt" to "class Q\n",
            "contracts/src/main/kotlin/Z.kt" to "val s = \"DEVTOOLS\"\n",
        )
        for ((path, content) in cases) {
            val r = SeparationScan.scan(fakeRepo(mapOf(path to content)))
            assertFalse("$path: devtools was not reported", r.hits.isEmpty())
        }
    }

    // decision DA-89: the passcode text is a hit in code and in a resource.
    @Test
    fun decisionDA89_thePasscodeTextInSIsAHit() {
        val cases = mapOf(
            "app/src/main/kotlin/A.kt" to "val c = \"0417\"\n",
            "contracts/src/main/res/x.xml" to "<!-- 0417 -->\n",
            "app/src/release/kotlin/DebugAids.kt" to "class DebugAids { val x = 0417 }\n",
        )
        for ((path, content) in cases) {
            val r = SeparationScan.scan(fakeRepo(mapOf(path to content)))
            assertFalse("$path: 0417 was not reported", r.hits.isEmpty())
        }
    }

    // decision DA-89: the exemption is exactly two whole lines, each only in its own file.
    @Test
    fun decisionDA89_theExemptionIsExactlyTheTwoWiringLines() {
        val appBuild = "app/build.gradle.kts"
        val settings = "settings.gradle.kts"
        val wiring = "    debugImplementation(project(\":devtools\"))\n"
        val cases = mapOf(
            "implementation in app" to (appBuild to "dependencies {\n$wiring    implementation(project(\":devtools\"))\n}\n"),
            "releaseImplementation in app" to (appBuild to "dependencies {\n$wiring    releaseImplementation(project(\":devtools\"))\n}\n"),
            "a trailing comment on the exempt line" to (appBuild to "dependencies {\n    debugImplementation(project(\":devtools\")) // x\n}\n"),
            "include line inside app build" to (appBuild to "dependencies {\n$wiring}\ninclude(\":devtools\")\n"),
            "debugImplementation line in settings" to (settings to "include(\":devtools\")\n$wiring"),
            "a passcode on the exempt line" to (settings to "include(\":devtools\") // 0417\n"),
            "another project in settings" to (settings to "include(\":devtools\")\ninclude(\":devtools2\")\n"),
        )
        for ((label, c) in cases) {
            val r = SeparationScan.scan(fakeRepo(mapOf(c.first to c.second)))
            assertFalse("$label: not reported", r.hits.isEmpty())
        }
    }

    // decision DA-89: trees outside S are never visited, among them build/, .gradle/, .swdev/ and the devtools module itself.
    @Test
    fun decisionDA89_treesOutsideSAreNeverVisited() {
        val r = SeparationScan.scan(
            fakeRepo(
                mapOf(
                    "app/build/intermediates/values.xml" to "devtools_button 0417\n",
                    "play/build/x.kt" to "devtools\n",
                    ".gradle/x.txt" to "devtools 0417\n",
                    ".swdev/staged/WO-005/devtools/A.kt" to "0417 devtools\n",
                    "devtools/src/main/kotlin/D.kt" to "const val CODE = \"0417\"\n",
                    "app/src/debug/kotlin/DebugAids.kt" to "import io.github.jamisuni.tangram.devtools.DevToolsState\n",
                    "app/src/androidTest/kotlin/T.kt" to "devtools\n",
                ),
            ),
        )
        assertEquals(emptyList<String>(), r.hits.map { it.toString() })
    }

    // decision DA-88: no repo.root property is a loud error, not a pass; an incomplete S is a loud error too.
    @Test
    fun decisionDA88_aMissingRepoRootOrAnIncompleteSetFailsLoudly() {
        val saved = System.getProperty("repo.root")
        try {
            System.clearProperty("repo.root")
            try {
                SeparationScan.repoRoot()
                fail("repoRoot() returned although repo.root is not set")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message!!.contains("repo.root"))
            }
        } finally {
            if (saved != null) System.setProperty("repo.root", saved)
        }
        try {
            SeparationScan.scan(fakeRepo(drop = setOf("app/src/release/kotlin/DebugAids.kt")))
            fail("scan() accepted a repository without app/src/release")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message!!.contains("app/src/release"))
        }
    }

    // decision DA-72 (control): the signature reader tells a drifted stub from an identical one and ignores private members.
    @Test
    fun decisionDA72_theSignatureReaderCanFail() {
        val debug = """
            class DebugAids {
                private fun helper(x: Int) = x
                @Composable
                fun CornerButton(puzzle: Puzzle, solveNow: (List<PlacedPiece>) -> Boolean, blocked: () -> Boolean, modifier: Modifier = Modifier) {
                    DevCornerButton(state, puzzle, solveNow, blocked, modifier)
                }
                @Composable fun BoardOverlay(puzzle: Puzzle, space: BoardSpace) { /* real */ }
            }
        """.trimIndent()
        val same = """
            class DebugAids {
                @Composable fun CornerButton(puzzle: Puzzle,
                        solveNow: (List<PlacedPiece>) -> Boolean,
                        blocked: () -> Boolean,
                        modifier: Modifier = Modifier) { }
                @Composable fun BoardOverlay(puzzle: Puzzle, space: BoardSpace) { }
            }
        """.trimIndent()
        val drifted = same.replace("blocked: () -> Boolean,", "blocked: () -> Boolean = { false },")
        val missingParam = same.replace("blocked: () -> Boolean,", "")
        val noComposable = same.replace("@Composable fun BoardOverlay", "fun BoardOverlay")
        assertEquals(publicFunSignatures(debug), publicFunSignatures(same))
        assertEquals(2, publicFunSignatures(debug).size)
        assertTrue(publicFunSignatures(debug) != publicFunSignatures(drifted))
        assertTrue(publicFunSignatures(debug) != publicFunSignatures(missingParam))
        assertTrue(publicFunSignatures(debug) != publicFunSignatures(noComposable))
    }
}
