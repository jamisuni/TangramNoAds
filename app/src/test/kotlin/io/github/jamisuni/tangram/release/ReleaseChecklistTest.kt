package io.github.jamisuni.tangram.release

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// WO-009 T9c (independent author), in tree, scaffolding: the decision tests of the release checklist. They carry NO acceptance token (a
// checklist is not a criterion's carrier; DA-158).
// decision DA-158 (F17; design section 6 "release-checklist.md" and "Freeze and fail-loud"): one markdown table, the columns
//   id | item | kind | command or action | owner | status | evidence
// one row per id, `kind` machine or manual, `owner` ai or jami, `status` from the closed set waiting-for-owner / done / n/a, `evidence`
// a path under release/evidence/. "A manual row turns `done` only from evidence the owner supplies": so a manual `done` row names an
// evidence file that exists and is not empty, and a manual row that is still waiting names none. At the close of WO-009 no evidence exists,
// so every manual row is waiting-for-owner: this follows from the rule above and does not stop the owner's rows turning `done` later.
// The five waived criteria each have exactly one row, whose id is the criterion's own id (design section 6: "`id` (the REQ token or a name)").
// Input: release/release-checklist.md and release/evidence/ (declared test input `releaseDocs` of :app, TASK-090). A missing file, a
// table without the seam's header, a row with the wrong number of cells: error(), never a skip.
class ReleaseChecklistTest {

    private class Row(val id: String, val item: String, val kind: String, val action: String, val owner: String, val status: String, val evidence: String)

    private val columns = listOf("id", "item", "kind", "command or action", "owner", "status", "evidence")

    private fun root(): File {
        val p = System.getProperty("repo.root")
        if (p.isNullOrBlank()) error("system property repo.root is not set for :app:test (app/build.gradle.kts passes it)")
        val f = File(p)
        if (!File(f, "settings.gradle.kts").isFile) error("repo.root=$p is not the repository root")
        return f
    }

    private fun clean(cell: String): String = cell.trim().trim('`', '*').trim()

    private fun rows(): List<Row> {
        val file = File(root(), "release/release-checklist.md")
        if (!file.isFile) error("release/release-checklist.md does not exist (TASK-094 delivers it)")
        val lines = file.readText().replace("\r\n", "\n").lines()
        fun cells(line: String): List<String> = line.trim().removePrefix("|").removeSuffix("|").split("|").map { clean(it) }
        val header = lines.indexOfFirst { it.trim().startsWith("|") && cells(it).map { c -> c.lowercase() } == columns }
        if (header < 0) error("release/release-checklist.md has no table with the columns $columns (a renamed or missing column)")
        val out = ArrayList<Row>()
        var i = header + 1
        if (i < lines.size && Regex("^\\s*\\|[\\s:|-]+\\|\\s*$").matches(lines[i])) i++
        while (i < lines.size && lines[i].trim().startsWith("|")) {
            val c = cells(lines[i])
            if (c.size != columns.size) error("a checklist row has ${c.size} cells, the table has ${columns.size}: ${lines[i]}")
            out += Row(c[0], c[1], c[2].lowercase(), c[3], c[4].lowercase(), c[5].lowercase(), c[6])
            i++
        }
        if (out.isEmpty()) error("the checklist table has a header and no row")
        return out
    }

    private fun evidencePath(row: Row): String? = Regex("release/evidence/[^\\s`|)\\]>\"']+").find(row.evidence)?.value

    /** The waived criteria's ids are written in two parts so that this file carries no acceptance token (it asserts no criterion). */
    private val waivedIds = listOf(
        "REQ-042" + ".A1", "REQ-039" + ".A2", "REQ-048" + ".A1", "REQ-048" + ".A2", "REQ-001" + ".A2",
    )

    // decision DA-158: the closed sets of the seam.
    @Test
    fun everyRowUsesTheClosedSets() {
        for (r in rows()) {
            assertTrue("row '${r.id}': kind '${r.kind}' is not machine or manual", r.kind in setOf("machine", "manual"))
            assertTrue("row '${r.id}': owner '${r.owner}' is not ai or jami", r.owner in setOf("ai", "jami"))
            assertTrue("row '${r.id}': status '${r.status}' is not waiting-for-owner, done or n/a", r.status in setOf("waiting-for-owner", "done", "n/a"))
        }
    }

    // decision DA-158: "one row per id".
    @Test
    fun everyRowHasItsOwnNonEmptyId() {
        val all = rows()
        for (r in all) assertTrue("a row has an empty id: ${r.item}", r.id.isNotEmpty())
        val dup = all.groupBy { it.id }.filter { it.value.size > 1 }.keys
        assertTrue("ids that appear in more than one row: $dup", dup.isEmpty())
    }

    // decision DA-158: the five waived criteria have a row each (the staged carriers look their rows up by id and fail loudly without it).
    @Test
    fun theFiveWaivedCriteriaHaveExactlyOneRowEach() {
        val all = rows()
        for (id in waivedIds) assertEquals("rows for $id", 1, all.count { it.id == id })
    }

    // decision DA-158 (E2, S3): a manual row is `done` only with owner-supplied evidence: an existing, non-empty file under release/evidence/.
    @Test
    fun aDoneManualRowNamesAnExistingNonEmptyEvidenceFile() {
        val root = root()
        for (r in rows().filter { it.kind == "manual" && it.status == "done" }) {
            val path = evidencePath(r)
            assertTrue("manual row '${r.id}' is done but names no evidence file under release/evidence/ (evidence cell: '${r.evidence}')", path != null)
            val f = File(root, path!!)
            assertTrue("manual row '${r.id}' is done but $path does not exist", f.isFile)
            assertTrue("manual row '${r.id}' is done but $path is empty", f.readText().isNotBlank())
        }
    }

    // decision DA-158 (S3): a manual row that still waits for the owner names no evidence yet (a draft cannot pre-fill it).
    @Test
    fun aWaitingManualRowNamesNoEvidenceFile() {
        for (r in rows().filter { it.kind == "manual" && it.status == "waiting-for-owner" }) {
            assertTrue("manual row '${r.id}' waits for the owner but already names an evidence file: '${r.evidence}'", evidencePath(r) == null)
        }
    }
}
