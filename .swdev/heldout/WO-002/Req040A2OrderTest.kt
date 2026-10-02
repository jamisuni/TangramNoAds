package io.github.jamisuni.tangram.content.acceptance

import io.github.jamisuni.tangram.content.PuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleKind
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

// HELD-OUT (REQ-040.A2). Goes only to Test & Verify.
// Fixture note: the shipped set has NO full puzzle of rating 1 whose id sorts before "shapes-warmup", so the
// fixtures here are built by copying animals-cat.json (a valid full puzzle) under another id and difficulty.
class Req040A2OrderTest {

    private val real get() = Fx.shippedFiles()
    private val cat get() = Fx.shipped("animals-cat")

    private fun expectedOrder(ps: List<Puzzle>): List<String> =
        // REQ-040 Statement: "by kind (mini, then warm-up, then full), then by their difficulty rating, lowest first, and by id within a rating."
        ps.sortedWith(compareBy<Puzzle>({ it.kind.ordinal }, { it.rating }, { it.id.value })).map { it.id.value }

    // REQ-040.A2 -- "A full puzzle of rating 1 whose id sorts before 'shapes-warmup' still comes after every warm-up."
    @Test
    fun fullRatingOneWithEarlyIdStillComesAfterEveryWarmup() { // REQ-040.A2
        val ant = Fx.variant(cat, "animals-ant", difficulty = 1)
        val lib = PuzzleLibrary(real + ant)
        assertTrue("fixture must be accepted: ${lib.rejected}", lib.rejected.isEmpty())
        val ids = lib.puzzles.map { it.id.value }
        val antIndex = ids.indexOf("animals-ant")
        assertTrue("animals-ant listed", antIndex >= 0)
        assertEquals(PuzzleKind.FULL, lib.puzzles[antIndex].kind)
        assertEquals(1, lib.puzzles[antIndex].rating)
        assertTrue("id sorts before shapes-warmup (fixture precondition)", "animals-ant" < "shapes-warmup-1")
        lib.puzzles.forEachIndexed { i, p ->
            if (p.kind != PuzzleKind.FULL) assertTrue("${p.id.value} (${p.kind}) must come before animals-ant", i < antIndex)
        }
        // and, being rating 1, it opens the full puzzles
        assertTrue(lib.puzzles.drop(antIndex).all { it.kind == PuzzleKind.FULL })
        assertEquals("animals-ant", lib.puzzles.first { it.kind == PuzzleKind.FULL }.id.value)
    }

    // REQ-040 Statement: kind, then rating, then id -- over a mixed fixture, whatever the input order.
    @Test
    fun orderIsKindThenRatingThenId() { // REQ-040.A2
        val extra = listOf(
            Fx.variant(cat, "animals-ant", difficulty = 1),
            Fx.variant(cat, "animals-bee", difficulty = 1),
            Fx.variant(cat, "zebra-full", difficulty = 1),
            Fx.variant(cat, "animals-ape", difficulty = 2),
            Fx.variant(cat, "aaa-hard", difficulty = 5),
        )
        val files = real + extra
        val reference = PuzzleLibrary(files)
        assertTrue("fixtures accepted: ${reference.rejected}", reference.rejected.isEmpty())
        val want = expectedOrder(reference.puzzles)
        assertEquals(want, reference.puzzles.map { it.id.value })
        // within one kind and rating, ids ascend: the rating-1 fulls are ant < bee < zebra
        val rating1Fulls = reference.puzzles.filter { it.kind == PuzzleKind.FULL && it.rating == 1 }.map { it.id.value }
        assertEquals(listOf("animals-ant", "animals-bee", "zebra-full"), rating1Fulls)
        // rating beats id: aaa-hard (5) is after every lower-rated full
        val fulls = reference.puzzles.filter { it.kind == PuzzleKind.FULL }.map { it.id.value }
        assertEquals("aaa-hard", fulls.last())
        // the input order is irrelevant
        for (seed in 1L..8L) {
            assertEquals(want, PuzzleLibrary(files.shuffled(Random(seed))).puzzles.map { it.id.value })
        }
        assertEquals(want, PuzzleLibrary(files.reversed()).puzzles.map { it.id.value })
    }

    // REQ-040 Statement: kind outranks rating. A warm-up of rating 4 still precedes a full of rating 1.
    // (Whether a rating-4 warm-up is a legal file is not stated by REQ-040; skip when the library refuses the fixture.)
    @Test
    fun kindOutranksRatingEvenForAHigherRatedWarmup() { // REQ-040.A2
        val warm4 = Fx.variant(Fx.shipped("shapes-warmup-1"), "shapes-warmup-9", difficulty = 4)
        val ant = Fx.variant(cat, "animals-ant", difficulty = 1)
        val lib = PuzzleLibrary(real + warm4 + ant)
        assumeTrue("library refuses a rating-4 warm-up: ${lib.rejected}", lib.puzzle(io.github.jamisuni.tangram.contracts.puzzle.PuzzleId("shapes-warmup-9")) != null)
        val ids = lib.puzzles.map { it.id.value }
        assertTrue(ids.indexOf("shapes-warmup-9") < ids.indexOf("animals-ant"))
        assertEquals(expectedOrder(lib.puzzles), ids)
    }

    // REQ-040 Statement: a mini sorts before everything; with a full of rating 1 and an earlier id present the first puzzle stays a mini of rating 1.
    @Test
    fun earlyIdFullNeverOpensTheList() { // REQ-040.A2
        val lib = PuzzleLibrary(real + Fx.variant(cat, "aaa-first", difficulty = 1))
        assertEquals(PuzzleKind.MINI, lib.puzzles.first().kind)
        assertEquals(1, lib.puzzles.first().rating)
    }
}
