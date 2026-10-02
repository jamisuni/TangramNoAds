// ─────────────────────────────────────────────────────────────────
// GOVERNED INTERFACE — SWDev Contract (see architecture.md registry)
// Tier: LOCKED. Changes ONLY by common agreement, recorded like a REQ change.
// Serves: REQ-007, REQ-011, REQ-012, REQ-019, REQ-023, REQ-038, REQ-039, REQ-040, REQ-041, REQ-042, REQ-045, REQ-046, REQ-047
// ─────────────────────────────────────────────────────────────────
package io.github.jamisuni.tangram.contracts.puzzle

/**
 * The puzzles shipped with the game. They are packaged at build time from the
 * puzzle files in `Tangrams/`, format `tangram-puzzle/1`
 * (`Tangrams/puzzle.schema.json`, locked; architecture.md G-08), and never
 * change while the game runs.
 */
interface IPuzzleLibrary {

    /**
     * Every shipped puzzle, in list order: by kind (mini, then warm-up, then
     * full), then by rating, lowest first, then by id (REQ-040). So the list
     * starts with the mini puzzles (REQ-045) and then the warm-ups (REQ-041).
     * The ‹ › buttons (REQ-024) and the overview grid (REQ-050) use this order.
     *
     * Never empty. Every packaged file passes the build's check — the
     * `content` unit test over every puzzle file in `gradlew test`
     * (REQ-038.A1); a file that still cannot be read at run time is left out
     * of the list rather than stopping the game (architecture.md G-10).
     */
    val puzzles: List<Puzzle>

    /**
     * The puzzle with this id, or `null` when no shipped puzzle has it — for
     * example saved progress (REQ-025) of a puzzle that an update removed
     * (decisions.md F4: such progress is ignored).
     */
    fun puzzle(id: PuzzleId): Puzzle?
}
