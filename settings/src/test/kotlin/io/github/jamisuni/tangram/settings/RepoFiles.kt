package io.github.jamisuni.tangram.settings

import java.io.File

/**
 * ACCEPTANCE-TEST ADAPTER (WO-007 T7c): where the repository is, for the two source-reading tests of this module (LockedTextsTest,
 * PxDrawingScanTest). The root comes from the `repo.root` system property that `settings/build.gradle.kts` passes (design seam
 * row S-0a, DA-88 pattern); a missing property or a wrong folder is a loud error, never a silent pass (precedent: ReleaseSeparationTest).
 */
internal object RepoFiles {
    fun root(): File {
        val p = System.getProperty("repo.root")
        if (p.isNullOrBlank()) {
            error("system property repo.root is not set: settings/build.gradle.kts must pass it to the unit-test task (DA-88)")
        }
        val f = File(p)
        if (!f.isDirectory || !File(f, "settings.gradle.kts").isFile) error("repo.root=$p is not the repository root (no settings.gradle.kts there)")
        return f
    }

    fun file(rel: String): File {
        val f = File(root(), rel)
        if (!f.isFile) error("$rel does not exist under ${root()}: the test would be blind")
        return f
    }

    /** Every file under [relDir] (forward-slash paths relative to the repository root), loud when the folder is missing. */
    fun filesUnder(relDir: String): List<String> {
        val base = File(root(), relDir)
        if (!base.isDirectory) error("$relDir does not exist under ${root()}: the scan would be blind")
        return base.walkTopDown().filter { it.isFile }.map { it.relativeTo(root()).invariantSeparatorsPath }.sorted().toList()
    }

    /** Kotlin source with comments blanked (newlines kept); string literals are kept. Nested block comments are handled. */
    fun stripComments(src: String): String {
        val out = StringBuilder(src.length)
        var i = 0
        var depth = 0
        var inLine = false
        var inString = false
        var inRaw = false
        var inChar = false
        while (i < src.length) {
            val c = src[i]
            val n = if (i + 1 < src.length) src[i + 1] else '\u0000'
            when {
                inLine -> { if (c == '\n') { inLine = false; out.append(c) } else out.append(' '); i++ }
                depth > 0 -> when {
                    c == '/' && n == '*' -> { depth++; out.append("  "); i += 2 }
                    c == '*' && n == '/' -> { depth--; out.append("  "); i += 2 }
                    else -> { out.append(if (c == '\n') '\n' else ' '); i++ }
                }
                inRaw -> { if (src.startsWith("\"\"\"", i)) { inRaw = false; out.append("\"\"\""); i += 3 } else { out.append(c); i++ } }
                inString -> when {
                    c == '\\' -> { out.append(c); if (i + 1 < src.length) out.append(src[i + 1]); i += 2 }
                    c == '"' -> { inString = false; out.append(c); i++ }
                    else -> { out.append(c); i++ }
                }
                inChar -> when {
                    c == '\\' -> { out.append(c); if (i + 1 < src.length) out.append(src[i + 1]); i += 2 }
                    c == '\'' -> { inChar = false; out.append(c); i++ }
                    else -> { out.append(c); i++ }
                }
                src.startsWith("\"\"\"", i) -> { inRaw = true; out.append("\"\"\""); i += 3 }
                c == '"' -> { inString = true; out.append(c); i++ }
                c == '\'' -> { inChar = true; out.append(c); i++ }
                c == '/' && n == '/' -> { inLine = true; out.append("  "); i += 2 }
                c == '/' && n == '*' -> { depth = 1; out.append("  "); i += 2 }
                else -> { out.append(c); i++ }
            }
        }
        return out.toString()
    }

    /** Identifier-token match: [name] not embedded in a longer identifier (`AudioTrack` does not match inside `AudioTrackSoundOut`). */
    fun tokenRegex(name: String): Regex = Regex("(?<![A-Za-z0-9_])" + Regex.escape(name) + "(?![A-Za-z0-9_])")
}
