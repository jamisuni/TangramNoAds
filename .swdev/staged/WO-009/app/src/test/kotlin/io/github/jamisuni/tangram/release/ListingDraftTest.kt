package io.github.jamisuni.tangram.release

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// WO-009 T9c (independent author), in tree, scaffolding: the decision tests of the store listing draft. NO acceptance token: the listing's
// live title and labels are criteria that only the live listing can meet (the staged ReleaseEvidenceTest), and a draft proves none of them.
// decision DA-160 (design section 6 "store-listing.md" and the seam row): the draft carries the fields
//   title.en, title.fi, short.en, short.fi, full.en, full.fi, privacy-policy-url
// as `key: value` lines, and a `declarations` block with the lines `ads: no`, `in-app-products: none`, `data-collected: none`.
// The title is within 30 characters (a `(pending ...)` note after the title, as the design's draft line has it, is not part of the title),
// carries no barred promotional word (Play's metadata policy bars "free" in a title; the design also keeps "no ads" out of it), the short
// description is within 80 and the full one within 4000 characters, the English short description carries the promise, the draft names no
// puzzle count (the shipped count is the owner's) and no version line (the version is the owner's).
// Every length constant below is a Play fact: verify in the Console (design N9). They are constants of this draft check, not of the game.
// A value may continue on following lines (a long description); it ends at the next field line, the next heading, a code fence or the end of the file.
// Input: release/store-listing.md (declared test input `releaseDocs` of :app). A missing file or field: error(), never a skip.
class ListingDraftTest {

    // verify in the Console: Play's field limits (title 30, short description 80, full description 4000).
    private val titleMax = 30
    private val shortMax = 80
    private val fullMax = 4000

    private val fieldKeys = listOf("title.en", "title.fi", "short.en", "short.fi", "full.en", "full.fi", "privacy-policy-url")

    private fun root(): File {
        val p = System.getProperty("repo.root")
        if (p.isNullOrBlank()) error("system property repo.root is not set for :app:test (app/build.gradle.kts passes it)")
        val f = File(p)
        if (!File(f, "settings.gradle.kts").isFile) error("repo.root=$p is not the repository root")
        return f
    }

    private val keyLine = Regex("^\\s*(?:[-*]\\s+)?`?([A-Za-z][A-Za-z0-9.\\-]*)`?\\s*:\\s*(.*)$")

    private fun unwrap(v: String): String {
        var s = v.trim()
        for (q in listOf('`', '"', '\'')) if (s.length >= 2 && s.first() == q && s.last() == q) s = s.substring(1, s.length - 1).trim()
        return s
    }

    private fun text(): List<String> {
        val f = File(root(), "release/store-listing.md")
        if (!f.isFile) error("release/store-listing.md does not exist (TASK-094 delivers it)")
        return f.readText().replace("\r\n", "\n").lines()
    }

    /** Field values by key: the rest of the key line plus continuation lines up to the next key line or heading. */
    private fun fields(lines: List<String>): Map<String, List<String>> {
        val out = LinkedHashMap<String, MutableList<String>>()
        var cur: String? = null
        val sb = StringBuilder()
        fun flush() { cur?.let { out.getOrPut(it) { ArrayList() } += unwrap(sb.toString().trim()) }; cur = null; sb.setLength(0) }
        for (line in lines) {
            val m = keyLine.matchEntire(line)
            if (line.trimStart().startsWith("#") || line.trimStart().startsWith("```")) { flush(); continue }
            if (m != null && (m.groupValues[1] in fieldKeys || m.groupValues[1] in setOf("ads", "in-app-products", "data-collected"))) {
                flush()
                cur = m.groupValues[1]
                sb.append(m.groupValues[2])
            } else if (m != null && cur != null && m.groupValues[1].matches(Regex("[a-z][a-z0-9.\\-]*")) && m.groupValues[1].length < 40 && line.trim().startsWith(m.groupValues[1])) {
                // another `key:` line that is not one this check reads (a developer or contact field): it ends the current value
                flush()
            } else if (cur != null) {
                sb.append('\n').append(line)
            }
        }
        flush()
        return out
    }

    private fun field(key: String): String {
        val v = fields(text())[key]?.first() ?: error("release/store-listing.md has no `$key:` field (the seam row lists $fieldKeys)")
        if (v.isBlank()) error("release/store-listing.md: `$key:` is empty")
        return v
    }

    private fun titleOf(key: String): String = field(key).replace(Regex("\\s*\\(.*\\)\\s*$"), "").trim()

    // decision DA-160: the title is present, within 30 characters, and has no barred promotional word.
    @Test
    fun theTitlesAreWithinTheLimitAndCarryNoBarredPromotionalWord() {
        for (key in listOf("title.en", "title.fi")) {
            val t = titleOf(key)
            assertTrue("$key is empty", t.isNotEmpty())
            assertTrue("$key '$t' is ${t.length} characters, over $titleMax (verify the limit in the Console)", t.length <= titleMax)
            val low = t.lowercase()
            for (word in listOf("free", "no ads", "ilmainen", "mainoksia")) {
                assertFalse("$key '$t' carries the barred promotional word '$word' (Play's metadata policy; verify in the Console)", low.contains(word))
            }
        }
    }

    // decision DA-160: the short description is within 80 characters in both languages and the English one opens with the promise.
    @Test
    fun theShortDescriptionsAreWithinTheLimitAndTheEnglishOneCarriesThePromise() {
        for (key in listOf("short.en", "short.fi")) {
            val s = field(key)
            assertTrue("$key is ${s.length} characters, over $shortMax (verify the limit in the Console)", s.length <= shortMax)
        }
        val en = field("short.en").lowercase()
        assertTrue("short.en must carry the promise ('no ads'): '$en'", en.contains("no ads"))
        assertTrue("short.en must carry the promise ('no purchases'): '$en'", en.contains("no purchases"))
    }

    // decision DA-160: the full description is within 4000 characters in both languages.
    @Test
    fun theFullDescriptionsAreWithinTheLimit() {
        for (key in listOf("full.en", "full.fi")) {
            val s = field(key)
            assertTrue("$key is ${s.length} characters, over $fullMax (verify the limit in the Console)", s.length <= fullMax)
        }
    }

    // decision DA-160: the declarations block of the Console answer sheet: ads no, in-app products none, data collected none.
    @Test
    fun theDeclarationsSayNoAdsNoProductsNoDataCollected() {
        // The draft may repeat a key in prose (a bullet explaining the row): the declaration is the line that reads exactly `key: value`.
        val f = fields(text())
        for ((key, value) in listOf("ads" to "no", "in-app-products" to "none", "data-collected" to "none")) {
            val seen = f[key].orEmpty().map { it.trim().lowercase() }
            assertTrue("declarations: no `$key: $value` line (lines for that key read $seen)", value in seen)
        }
    }

    // decision DA-160: the privacy link field exists (its value is the owner's: the draft carries a placeholder until the host is chosen).
    @Test
    fun thePrivacyLinkFieldExists() {
        assertTrue("privacy-policy-url is empty", field("privacy-policy-url").isNotEmpty())
    }

    // decision DA-160: no puzzle count in the draft (the shipped count is the owner's) and no version line (the version is the owner's).
    @Test
    fun theDraftNamesNoPuzzleCountAndNoVersionLine() {
        val lines = text()
        val count = Regex("\\b\\d+\\s+(?:new\\s+)?(?:puzzles|palapeli\\w*)", RegexOption.IGNORE_CASE)
        for (line in lines) assertFalse("a puzzle count in the draft: '$line'", count.containsMatchIn(line))
        for (line in lines) assertFalse("a version line in the draft: '$line'", Regex("^\\s*(?:[-*]\\s+)?`?version(?:name|code)?`?\\s*[:=]", RegexOption.IGNORE_CASE).containsMatchIn(line))
    }
}
