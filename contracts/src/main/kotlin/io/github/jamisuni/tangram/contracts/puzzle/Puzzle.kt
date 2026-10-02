// ─────────────────────────────────────────────────────────────────
// GOVERNED INTERFACE — SWDev Contract (see architecture.md registry)
// Tier: LOCKED. Contract types of IPuzzleLibrary; the file format is
// Tangrams/puzzle.schema.json. Changes ONLY by common agreement.
// Serves: REQ-007, REQ-011, REQ-012, REQ-019, REQ-023, REQ-038, REQ-039, REQ-040, REQ-041, REQ-042, REQ-045, REQ-046, REQ-047
// ─────────────────────────────────────────────────────────────────
package io.github.jamisuni.tangram.contracts.puzzle

import io.github.jamisuni.tangram.kernel.model.ExactPoint
import io.github.jamisuni.tangram.kernel.model.PieceId

/**
 * A puzzle's id: the file's `id`, kebab-case, equal to its file name. Never
 * reused, because saved progress is keyed on it (Spec/03-puzzle-format.md §2).
 */
@JvmInline
value class PuzzleId(val value: String)

/** The file's `kind` (default `full`): REQ-045 mini, REQ-041 warm-up, REQ-038 full. */
enum class PuzzleKind {
    MINI,
    WARMUP,
    FULL,
}

/** The file's `category`; REQ-042 counts these as themes. */
enum class PuzzleCategory {
    SHAPES,
    ANIMALS,
    PEOPLE,
    THINGS,
    VEHICLES,
    NATURE,
    LETTERS,
    NUMBERS,
}

/** The title in both shipped languages (REQ-047; validator rule V8). */
data class PuzzleTitle(val en: String, val fi: String) {

    /** REQ-047: Finnish when the language is Finnish (`fi`), English for every other language. */
    fun inLanguage(languageCode: String): String = if (languageCode == "fi") fi else en
}

/**
 * One shipped puzzle.
 *
 * @property rating the file's `difficulty`, 1–5, shown as dots in the top bar (REQ-040).
 * @property solution one entry per piece the puzzle uses: all seven for warm-up
 *   and full puzzles, 1–6 distinct pieces for a mini puzzle (REQ-038, REQ-045);
 *   the tray shows exactly these pieces (REQ-012, REQ-045). The polygons are the
 *   stored solution: they draw the silhouette (REQ-011) and give its outline
 *   corners, the lock anchors (REQ-019). Any exact cover solves (REQ-022); this
 *   is never compared against.
 * @property picture the solved picture (REQ-023, REQ-039).
 * @property reviewedByHuman the file's `provenance.reviewedByHuman`; a missing
 *   flag counts as `false` (REQ-039, decisions.md F16).
 */
data class Puzzle(
    val id: PuzzleId,
    val title: PuzzleTitle,
    val category: PuzzleCategory,
    val rating: Int,
    val kind: PuzzleKind,
    val solution: List<SolutionPiece>,
    val picture: Picture,
    val reviewedByHuman: Boolean,
)

/**
 * Where one piece lies in the stored solution: its polygon in exact puzzle
 * units (ADR-003). A file entry given as `rot` + `flip` + `at` arrives here
 * already turned into its polygon.
 */
data class SolutionPiece(val piece: PieceId, val polygon: List<ExactPoint>)

/** A colour `#RRGGBB` as `0xRRGGBB`. */
@JvmInline
value class Rgb(val value: Int)

/** A point of the picture, in puzzle units; pictures use plain numbers (Spec/03-puzzle-format.md §1). */
data class PicturePoint(val x: Double, val y: Double)

/**
 * REQ-023, REQ-039: the solved picture — the [base] colour fills the
 * silhouette, then [shapes] are drawn in order; everything is clipped to the
 * silhouette, so the picture has exactly the puzzle's outline.
 */
data class Picture(val base: Rgb, val shapes: List<PictureShape>)

/** Paint shared by every picture shape; `null` means not set in the file. [strokeWidth] is in puzzle units. */
data class PictureStyle(
    val fill: Rgb?,
    val stroke: Rgb?,
    val strokeWidth: Double?,
    val opacity: Double?,
)

/** The picture shapes of `tangram-puzzle/1` (Spec/03-puzzle-format.md §2, "Art shapes"). */
sealed interface PictureShape {
    val style: PictureStyle

    data class Polygon(val points: List<PicturePoint>, override val style: PictureStyle) : PictureShape

    data class Rect(
        val x: Double,
        val y: Double,
        val width: Double,
        val height: Double,
        val cornerRadius: Double?,
        override val style: PictureStyle,
    ) : PictureShape

    data class Circle(val center: PicturePoint, val radius: Double, override val style: PictureStyle) : PictureShape

    data class Ellipse(
        val center: PicturePoint,
        val radiusX: Double,
        val radiusY: Double,
        override val style: PictureStyle,
    ) : PictureShape

    data class Line(val from: PicturePoint, val to: PicturePoint, override val style: PictureStyle) : PictureShape

    /** [data]: absolute M/L/Q/C/Z commands with numbers in puzzle units, as in the file. */
    data class Path(val data: String, override val style: PictureStyle) : PictureShape
}
