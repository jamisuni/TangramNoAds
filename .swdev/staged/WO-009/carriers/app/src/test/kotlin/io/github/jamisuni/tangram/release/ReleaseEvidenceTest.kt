package io.github.jamisuni.tangram.release

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// WO-009 T9c (independent author). STAGED carrier of the five-ID waiver (DA-158): RED by design until the owner has published and supplied
// the live-listing evidence, and moved into the tree only in the post-publish phase of RELEASE-DAY, after the compile and the sha256 check
// (design section 6, "Freeze and fail-loud"). Never moved by MOVE-JVM9. It compares RECORDED VALUES (F17's own method, with content):
//
//   * the checklist release/release-checklist.md has, for each criterion below, exactly one row whose `id` is the criterion's id;
//   * that row is `done`, its `owner` is `jami`, and its `evidence` cell names a file under release/evidence/ that exists and is not empty
//     (a manual row turns `done` only from evidence the owner supplies; the AI records it);
//   * the evidence file is a text record of `key: value` lines (a bullet or backticks around the key are tolerated). The keys read here:
//       live-title:          the title the LIVE listing shows
//       privacy-policy-link: the privacy link the live listing shows
//       labels:              the labels the live listing shows, as an EXPLICIT statement: the labels' names, or the word `none` when the
//                            listing shows none (the expected positive form is `labels: none`). An empty or missing value is not a
//                            statement: it fails loudly (a canary), so the "no ads / no purchases label" checks can never pass vacuously.
//                            The same holds for the other two keys: an empty value is an error.
//     RECORD FORMAT: the design fixes "recorded values" but not the evidence file's layout, so these three key names are this test's
//     convention; TASK-094's checklist and the RELEASE-DAY step that records the owner's evidence must write them (reported to the orchestrator).
//
// The three tests, each on its own row:
//   REQ-048 A1: the recorded privacy link is an `https://` URL (not the TBD-OWNER placeholder) and the recorded labels show no "Contains ads"
//               and no "In-app purchases".
//   REQ-048 A2: the recorded live title equals the title REQ-048's A2 states, READ FROM Requirements/reqs/REQ-048.md by a pinned extraction
//               (the one double-quoted span of the A2 line in the Acceptance section; any other count of spans: error), so the file needs no
//               edit after the capture side re-locks the REQ with the new title, and stays red until then.
//   REQ-001 A2: the recorded labels show no "Contains ads" and no "In-app purchases".
// Fail-loud canaries (`error`, never a skip, never a silent pass): `repo.root` unset; the checklist, the REQ file or a record that a `done`
// row names missing; a checklist table without the seam's header (a renamed column); a criterion with no row or two rows; a cell count that
// differs from the header; a REQ A2 line without exactly one quoted span; a record without the key a test reads. The verdict lines are the
// `assertEquals` / `assertTrue` / `assertFalse` calls named in each test. Inputs: release/release-checklist.md, release/evidence/**,
// Requirements/reqs/REQ-048.md (read-only; not a declared input of :app:test, so run the move-in with --rerun).
class ReleaseEvidenceTest {

    private class Row(val id: String, val kind: String, val owner: String, val status: String, val evidence: String)

    private val columns = listOf("id", "item", "kind", "command or action", "owner", "status", "evidence")

    private fun root(): File {
        val p = System.getProperty("repo.root")
        if (p.isNullOrBlank()) error("canary: system property repo.root is not set for :app:test (app/build.gradle.kts passes it)")
        val f = File(p)
        if (!File(f, "settings.gradle.kts").isFile) error("canary: repo.root=$p is not the repository root")
        return f
    }

    private fun clean(cell: String): String = cell.trim().trim('`', '*').trim()

    private fun rowFor(id: String): Row {
        val file = File(root(), "release/release-checklist.md")
        if (!file.isFile) error("canary: release/release-checklist.md does not exist")
        val lines = file.readText().replace("\r\n", "\n").lines()
        fun cells(line: String): List<String> = line.trim().removePrefix("|").removeSuffix("|").split("|").map { clean(it) }
        val header = lines.indexOfFirst { it.trim().startsWith("|") && cells(it).map { c -> c.lowercase() } == columns }
        if (header < 0) error("canary: release/release-checklist.md has no table with the columns $columns")
        val found = ArrayList<Row>()
        var i = header + 1
        if (i < lines.size && Regex("^\\s*\\|[\\s:|-]+\\|\\s*$").matches(lines[i])) i++
        while (i < lines.size && lines[i].trim().startsWith("|")) {
            val c = cells(lines[i])
            if (c.size != columns.size) error("canary: a checklist row has ${c.size} cells, the table has ${columns.size}: ${lines[i]}")
            if (c[0] == id) found += Row(c[0], c[2].lowercase(), c[4].lowercase(), c[5].lowercase(), c[6])
            i++
        }
        if (found.size != 1) error("canary: the checklist has ${found.size} rows for $id, expected exactly one")
        return found.single()
    }

    /** The row is done, owned by jami, and names an existing non-empty evidence file; returns the file's `key: value` record. */
    private fun doneRecord(row: Row): Map<String, String> {
        assertEquals("the row ${row.id} is not done: the owner has not supplied the live-listing evidence", "done", row.status)
        assertEquals("the row ${row.id} is not the owner's", "jami", row.owner)
        val path = Regex("release/evidence/[^\\s`|)\\]>\"']+").find(row.evidence)?.value
        assertTrue("the row ${row.id} is done but names no file under release/evidence/ (evidence cell: '${row.evidence}')", path != null)
        val f = File(root(), path!!)
        assertTrue("the evidence file $path of ${row.id} does not exist", f.isFile)
        assertTrue("the evidence file $path of ${row.id} is empty", f.readText().isNotBlank())
        val record = LinkedHashMap<String, String>()
        for (line in f.readText().replace("\r\n", "\n").lines()) {
            val m = Regex("^\\s*(?:[-*]\\s+)?`?([A-Za-z][A-Za-z0-9\\-]*)`?\\s*:\\s*(.*)$").matchEntire(line) ?: continue
            var v = m.groupValues[2].trim()
            for (q in listOf('`', '"', '\'')) if (v.length >= 2 && v.first() == q && v.last() == q) v = v.substring(1, v.length - 1).trim()
            record.putIfAbsent(m.groupValues[1].lowercase(), v)
        }
        return record
    }

    private fun need(record: Map<String, String>, key: String, row: Row): String {
        val v = record[key] ?: error("canary: the evidence record of ${row.id} has no `$key:` line")
        if (v.isBlank()) error("canary: the evidence record of ${row.id} has an empty `$key:` value (write an explicit statement, for labels `none`)")
        return v
    }

    private fun assertNoAdsOrPurchaseLabel(labels: String, id: String) {
        val low = labels.lowercase()
        assertFalse("$id: the live listing shows a 'Contains ads' label: '$labels'", low.contains("contains ads"))
        assertFalse("$id: the live listing shows an 'In-app purchases' label: '$labels'", low.contains("in-app purchases"))
    }

    /** REQ-048's A2 title: the one double-quoted span of the `A2` line of the Acceptance section. */
    private fun titleFromReq(): String {
        val f = File(root(), "Requirements/reqs/REQ-048.md")
        if (!f.isFile) error("canary: Requirements/reqs/REQ-048.md does not exist")
        val lines = f.readText().replace("\r\n", "\n").lines()
        val start = lines.indexOfFirst { it.trim() == "## Acceptance" }
        if (start < 0) error("canary: REQ-048 has no `## Acceptance` section")
        val end = (start + 1 until lines.size).firstOrNull { lines[it].startsWith("## ") } ?: lines.size
        val a2 = lines.subList(start + 1, end).filter { it.trimStart().startsWith("- **A2**") }
        if (a2.size != 1) error("canary: REQ-048 has ${a2.size} A2 lines in its Acceptance section, expected one")
        val spans = Regex("\"([^\"]+)\"").findAll(a2.single()).map { it.groupValues[1] }.toList()
        if (spans.size != 1) error("canary: the REQ-048 A2 line has ${spans.size} quoted spans, expected one: ${a2.single()}")
        return spans.single()
    }

    // REQ-048.A1 - "The store listing shows a privacy policy link, no "Contains ads" and no "In-app purchases" label (REQ-001)."
    @Test
    fun req048_A1_theLiveListingShowsAPrivacyLinkAndNoAdsOrPurchaseLabel() {
        val row = rowFor("REQ-048.A1")
        val record = doneRecord(row)
        val link = need(record, "privacy-policy-link", row)
        assertTrue("the recorded privacy link is not an https URL: '$link'", link.startsWith("https://") && link.length > "https://".length)
        assertFalse("the recorded privacy link is still the placeholder: '$link'", link.contains("TBD-OWNER"))
        assertNoAdsOrPurchaseLabel(need(record, "labels", row), row.id)
    }

    // REQ-048.A2 - "The listing's title is "Tangram, absolutely free"." (the title the REQ states when it is read; CA-12 re-locks it)
    @Test
    fun req048_A2_theLiveListingTitleIsTheTitleTheRequirementStates() {
        val row = rowFor("REQ-048.A2")
        val expected = titleFromReq()
        val record = doneRecord(row)
        assertEquals("the recorded live title differs from REQ-048's A2 title", expected, need(record, "live-title", row))
    }

    // REQ-001.A2 - "The store listing does not carry the "Contains ads" or "In-app purchases" labels."
    @Test
    fun req001_A2_theLiveListingCarriesNoAdsOrPurchaseLabel() {
        val row = rowFor("REQ-001.A2")
        val record = doneRecord(row)
        assertNoAdsOrPurchaseLabel(need(record, "labels", row), row.id)
    }
}
