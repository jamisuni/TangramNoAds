package io.github.jamisuni.tangram.content.acceptance

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleKind
import io.github.jamisuni.tangram.kernel.model.PieceId
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Req038Req040Req041Req045VisibleTest {

    // ---- REQ-038.A1 -- "Every puzzle file passes the validator." ----

    // REQ-038.A1; decision F15 (G1-baseline validator is the reference, verdicts live in the golden)
    @Test
    fun goldenIsFreshAndEveryFilePassesTheValidator() { // REQ-038.A1
        val golden = AcceptanceGolden.puzzles
        assertEquals(
            "golden stale against Tangrams/",
            emptyList<String>(),
            AcceptanceGolden.freshnessProblems(Fx.shippedBytes(), golden),
        )
        val problems = golden.values.flatMap { AcceptanceGolden.verdictProblems(it) }
        assertEquals(emptyList<String>(), problems)
    }

    // test-harness check (not a coverage claim): the check itself must be able to fail (negative control: a failing verdict is reported).
    @Test
    fun verdictCheckReportsAFailingEntry() { // test-harness check (not a coverage claim)
        val good = AcceptanceGolden.puzzles.values.first()
        val bad = GPuzzle(
            good.id, good.sha256, good.kind, good.difficulty,
            GVerdict(pass = false, errors = listOf("V6 overlap"), exposure = emptyMap()),
            good.buildOrder, good.polygons, good.poses,
        )
        assertTrue(AcceptanceGolden.verdictProblems(bad).isNotEmpty())
        val missing = GPuzzle(good.id, good.sha256, good.kind, good.difficulty, null, good.buildOrder, good.polygons, good.poses)
        assertTrue("an absent verdict is not a pass", AcceptanceGolden.verdictProblems(missing).isNotEmpty())
    }

    // decision DA-8 (parser accepts every shipped file); contract IPuzzleLibrary ("Every packaged file passes the build's check ... a file that
    // still cannot be read is left out"): at build time none may be left out.
    @Test
    fun noShippedFileIsRejectedByTheLibrary() { // decision DA-8
        val lib = PuzzleLibrary.packaged()
        assertTrue("rejected: ${lib.rejected}", lib.rejected.isEmpty())
    }

    // decision DA-8 (runtime rejection; REQ-038 statement): Statement "SHALL only include puzzles that use every piece ... except mini puzzles";
    // Puzzle.kt: `kind` defaults to full. A 3-piece file without `kind` is therefore a 3-piece FULL puzzle,
    // which the validator (V9) fails, so it must not be listed -- the library must not guess `mini` from the piece count.
    @Test
    fun threePieceFileWithoutKindIsNotListed() { // decision DA-8
        val real = Fx.shippedFiles()
        val noKind = Fx.variant(Fx.shipped("shapes-mini-1"), "shapes-mini-1", dropKind = true)
        val lib = PuzzleLibrary(real.filter { it.name != "shapes-mini-1.json" } + noKind)
        assertEquals(
            "a 3-piece puzzle without kind (default full) must not be listed",
            null, lib.puzzle(io.github.jamisuni.tangram.contracts.puzzle.PuzzleId("shapes-mini-1")),
        )
        assertEquals("the other files are kept", real.size - 1, lib.puzzles.size)
    }

    // decision DA-8 (runtime rejection): a seven-piece puzzle whose `kind` is mini is also not a valid mini ("1 to 6 distinct pieces", REQ-045 rules).
    @Test
    fun sevenPieceFileMarkedMiniIsNotListed() { // decision DA-8
        val real = Fx.shippedFiles()
        val fake = Fx.variant(Fx.shipped("animals-cat"), "animals-cat", kind = "mini")
        val lib = PuzzleLibrary(real.filter { it.name != "animals-cat.json" } + fake)
        assertEquals(null, lib.puzzle(io.github.jamisuni.tangram.contracts.puzzle.PuzzleId("animals-cat")))
        assertEquals(real.size - 1, lib.puzzles.size)
    }

    // ---- REQ-040.A1 -- "The first puzzle shown on a fresh install has rating 1." (library level: puzzles.first()) ----

    @Test
    fun firstPuzzleHasRatingOne() { // REQ-040.A1
        assertEquals(1, PuzzleLibrary.packaged().puzzles.first().rating)
        // whatever order the files arrive in
        val files = Fx.shippedFiles()
        for (seed in 1L..6L) {
            assertEquals(1, PuzzleLibrary(files.shuffled(Random(seed))).puzzles.first().rating)
        }
        assertEquals(1, PuzzleLibrary(files.reversed()).puzzles.first().rating)
    }

    // ---- REQ-041 ----

    // REQ-041.A1 -- "the first four puzzles after the mini puzzles are warm-ups" (library level).
    @Test
    fun firstFourAfterTheMinisAreWarmupsOfRatingOne() { // REQ-041.A1
        val inputs = listOf(Fx.shippedFiles(), Fx.shippedFiles().reversed()) + (1L..4L).map { Fx.shippedFiles().shuffled(Random(it)) }
        for (files in inputs) {
            val puzzles = PuzzleLibrary(files).puzzles
            val afterMinis = puzzles.dropWhile { it.kind == PuzzleKind.MINI }
            assertTrue(afterMinis.size >= 4)
            val firstFour = afterMinis.take(4)
            assertEquals(List(4) { PuzzleKind.WARMUP }, firstFour.map { it.kind })
            // Statement: "at least four warm-up puzzles of rating 1"
            assertEquals(List(4) { 1 }, firstFour.map { it.rating })
        }
    }

    // REQ-041.A2 -- "Each warm-up meets the half-outline measure." Reference: validator V12 via the golden (decision F15, DA-9).
    @Test
    fun everyWarmupMeetsTheHalfOutlineMeasure() { // REQ-041.A2
        val warmups = PuzzleLibrary.packaged().puzzles.filter { it.kind == PuzzleKind.WARMUP }
        assertTrue("at least four warm-ups (statement)", warmups.size >= 4)
        for (w in warmups) {
            val g = checkNotNull(AcceptanceGolden.puzzles[w.id.value])
            assertEquals(emptyList<String>(), AcceptanceGolden.verdictProblems(g))
            val exposure = checkNotNull(g.verdict).exposure
            // "at least half of EVERY piece's outline": seven pieces, each >= 0.5 (the validator's 1e-6 tolerance).
            assertEquals("${w.id.value}: an exposure value per piece", PieceId.values().map { it.name }.sorted(), exposure.keys.sorted())
            for ((piece, share) in exposure) assertTrue("${w.id.value} $piece exposure $share", share >= 0.5 - 1e-6)
            assertEquals("a warm-up uses every piece (REQ-038)", 7, w.solution.map { it.piece }.toSet().size)
        }
        // the Python `kind` and the Kotlin `kind` agree for every file (warm-up marking is the file's `kind`, REQ-041 rules)
        for (p in PuzzleLibrary.packaged().puzzles) {
            assertEquals(p.id.value, AcceptanceGolden.puzzles.getValue(p.id.value).kind, p.kind.name.lowercase())
        }
    }

    // ---- REQ-045.A1 -- "A fresh install opens on a mini puzzle whose tray holds three pieces." (library level) ----

    @Test
    fun firstPuzzleIsAMiniWithThreePieces() { // REQ-045.A1
        val first = PuzzleLibrary.packaged().puzzles.first()
        assertEquals(PuzzleKind.MINI, first.kind)
        // Puzzle.solution: "the tray shows exactly these pieces (REQ-012, REQ-045)"; Statement: "exactly three pieces".
        assertEquals(3, first.solution.size)
        assertEquals("three distinct pieces", 3, first.solution.map { it.piece }.toSet().size)
        assertEquals("Rules: mini puzzles have rating 1", 1, first.rating)
    }

    // REQ-045.A1 + Rules: "validator rule V9 accepts 1 to 6 distinct pieces" for a mini; "Mini puzzles have rating 1".
    @Test
    fun everyMiniUsesFewerThanSevenDistinctPiecesAndHasRatingOne() { // REQ-045.A1
        val minis = PuzzleLibrary.packaged().puzzles.filter { it.kind == PuzzleKind.MINI }
        assertTrue(minis.isNotEmpty())
        for (m in minis) {
            val pieces = m.solution.map { it.piece }
            assertEquals("${m.id.value}: distinct pieces", pieces.size, pieces.toSet().size)
            assertTrue("${m.id.value}: tray pieces ${pieces.size}", pieces.size in 1..6)
            assertEquals(1, m.rating)
        }
        // Non-minis show all seven.
        for (p in PuzzleLibrary.packaged().puzzles.filter { it.kind != PuzzleKind.MINI }) {
            assertEquals(7, p.solution.size)
        }
    }
}
