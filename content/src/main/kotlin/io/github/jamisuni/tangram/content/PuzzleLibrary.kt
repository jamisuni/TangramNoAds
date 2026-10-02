package io.github.jamisuni.tangram.content

import io.github.jamisuni.tangram.contracts.puzzle.IPuzzleLibrary
import io.github.jamisuni.tangram.contracts.puzzle.Puzzle
import io.github.jamisuni.tangram.contracts.puzzle.PuzzleId

/** The shipped puzzles in list order (REQ-040): kind, then rating, then id. A file that cannot be read is left out (G-10). */
class PuzzleLibrary internal constructor(files: List<PuzzleFile>) : IPuzzleLibrary {

    internal val rejected: List<ParseResult.Rejected>
    override val puzzles: List<Puzzle>
    private val byId: Map<PuzzleId, Puzzle>

    init {
        val results = files.map { PuzzleParser.parse(it) }
        rejected = results.filterIsInstance<ParseResult.Rejected>()
        puzzles = results.filterIsInstance<ParseResult.Parsed>().map { it.puzzle }
            .sortedWith(compareBy<Puzzle>({ it.kind }, { it.rating }, { it.id.value }))
        // DA-10: an empty library is a packaging error, not a state to run in.
        check(puzzles.isNotEmpty()) {
            "no readable puzzle (${rejected.size} rejected): the content build tests should have caught this"
        }
        byId = puzzles.associateBy { it.id }
    }

    override fun puzzle(id: PuzzleId): Puzzle? = byId[id]

    companion object {
        fun packaged(): PuzzleLibrary = PuzzleLibrary(PackagedPuzzles.files())
    }
}
