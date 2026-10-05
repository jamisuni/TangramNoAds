package io.github.jamisuni.tangram.content.release

import io.github.jamisuni.tangram.content.PuzzleLibrary
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

// WO-009 T9c (independent author). STAGED carrier of the five-ID waiver (DA-158): it is RED by design until the owner has run `mark` for every
// shipped puzzle, and it moves into the tree only in the pre-publish phase of RELEASE-DAY, after the compile and the sha256 check (design
// section 6, "Freeze and fail-loud"). Never moved by MOVE-JVM9.
// What it asserts, on the REAL packaged library, on every run:
//   - the release contains at least 20 puzzles with reviewedByHuman: true;
//   - no puzzle of the release has reviewedByHuman: false.
// Fail-loud canaries (`error`, never a skip, never a silent pass): an empty library, a puzzle file the parser rejected (a partial library
// would hide an unreviewed puzzle), a library that does not hold one puzzle per file of Tangrams/, and a missing `tangram.root`. A canary
// is a harness fault, not a verdict: the verdict lines are the two `assertTrue` calls at the foot of each test.
// Inputs: the packaged library and the folder Tangrams/ (system property `tangram.root`, set by content/build.gradle.kts for the test task).
class ReleaseLibraryReviewedTest {

    private fun library(): PuzzleLibrary {
        val lib = PuzzleLibrary.packaged()
        if (lib.puzzles.isEmpty()) error("canary: the packaged library is empty")
        if (lib.rejected.isNotEmpty()) error("canary: the library rejected ${lib.rejected.size} file(s), so it is partial: ${lib.rejected}")
        val root = System.getProperty("tangram.root")
        if (root.isNullOrBlank()) error("canary: system property tangram.root is not set for :content:test")
        val files = File(root, "Tangrams").listFiles { f -> f.isFile && f.name.endsWith(".json") && !f.name.endsWith(".schema.json") }
            ?: error("canary: cannot list $root/Tangrams")
        if (files.isEmpty()) error("canary: $root/Tangrams holds no puzzle file")
        if (files.map { it.name.removeSuffix(".json") }.toSet() != lib.puzzles.map { it.id.value }.toSet()) {
            error("canary: the library does not hold one puzzle per file of Tangrams/")
        }
        return lib
    }

    // REQ-042.A1 - "The release contains at least 20 puzzles with reviewedByHuman: true."
    @Test
    fun req042_A1_theReleaseContainsAtLeastTwentyReviewedPuzzles() {
        val lib = library()
        val reviewed = lib.puzzles.count { it.reviewedByHuman }
        assertTrue("only $reviewed of ${lib.puzzles.size} puzzles have reviewedByHuman: true (REQ-042 needs at least 20)", reviewed >= 20)
    }

    // REQ-039.A2 - "No released puzzle has reviewedByHuman: false."
    @Test
    fun req039_A2_noReleasedPuzzleHasReviewedByHumanFalse() {
        val lib = library()
        val unreviewed = lib.puzzles.filter { !it.reviewedByHuman }.map { it.id.value }
        assertTrue("${unreviewed.size} of ${lib.puzzles.size} puzzles still have reviewedByHuman: false: $unreviewed", unreviewed.isEmpty())
    }
}
