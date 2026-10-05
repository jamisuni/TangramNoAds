# 03 · Puzzle file format `tangram-puzzle/1`

**Status:** draft 0.4 (round 6: `kind` field, V12, V11 informational, V7 removed). Reference code: `tools/tangram_geom.py`. Validator: `tools/validate_puzzles.py`. JSON Schema: `../Tangrams/puzzle.schema.json`. Puzzles: `../Tangrams/*.json`. Review sheets: `../Tangrams/previews/`.

The format is made for **safe AI authoring**. Coordinates are exact, shapes are readable polygons, every rule is machine-checked, and a human signs off on how the puzzle looks.

## 1. Coordinates
- Unit: the 7 pieces assemble into a **4 × 4 square**. Large-triangle long side = 4, small-triangle long side = 2, square side = √2.
- x points right and **y points down**; positive rotation is clockwise on screen.
- A coordinate is a JSON number (`2`, `1.5`) or a pair **`[a, b]` = a + b·√2** (`[0, 2]` = 2√2, `[4, -1]` = 4 − √2).
- Tools and the app compare geometry exactly (ℚ(√2)); floats are only for drawing. `art` coordinates are plain floats.

## 2. File structure
```json
{
  "format": "tangram-puzzle/1",
  "id": "animals-cat",
  "title": { "en": "Cat", "fi": "Kissa" },
  "category": "animals",
  "difficulty": 2,
  "kind": "full",
  "solution": [
    {"piece": "LT1", "polygon": [[0, 6], [4, 6], [2, 4]]},
    {"piece": "LT2", "polygon": [[4, 6], [4, 2], [2, 4]]},
    {"piece": "SQ",  "polygon": [[2, 2], [3, 1], [4, 2], [3, 3]]},
    {"piece": "ST1", "polygon": [[2, 2], [3, 1], [2, 0]]},
    {"piece": "ST2", "polygon": [[3, 1], [4, 2], [4, 0]]},
    {"piece": "PG",  "polygon": [[4, 6], [4, 4], [5, 3], [5, 5]]},
    {"piece": "MT",  "polygon": [[0, 4], [0, 6], [2, 4]]}
  ],
  "art": {
    "base": "#F29E4C",
    "shapes": [
      {"type": "polygon", "points": [[0, 4], [0, 6], [2, 4]], "fill": "#FBE7D3"},
      {"type": "ellipse", "c": [2.65, 1.95], "rx": 0.1, "ry": 0.14, "fill": "#2B2D42"},
      {"type": "path", "d": "M3.3,3.35 Q3.7,3.55 4,3.35", "stroke": "#C8651F", "width": 0.16}
    ]
  },
  "provenance": {"author": "ai:claude", "reviewedByHuman": false}
}
```

| Field | Req. | Meaning |
|---|---|---|
| `format` | yes | `"tangram-puzzle/1"` |
| `id` | yes | Unique kebab-case `<category>-<name>`, equal to the file name. Never reused (saved progress is keyed on it). |
| `title` | yes | Translations; `en` and `fi` are both mandatory (V8): the game speaks both (REQ-047). |
| `category` | yes | `shapes`, `animals`, `people`, `things`, `vehicles`, `nature`, `letters`, `numbers` |
| `difficulty` | yes | 1–5. The dots in the top bar; the ‹ › order within a kind (easy first). |
| `kind` | no (default `full`) | `mini`: a small first-success / test puzzle that uses 1–6 pieces; the tray shows only those. `warmup`: all seven pieces, and at least half of every piece's outline lies on the silhouette edge (V12). `full`: all seven pieces. The list order is kind (mini, warmup, full), then difficulty, then id (REQ-040). |
| `solution` | yes | **All 7 pieces** (1–6 when `kind` is `mini`). Each entry: `piece` plus `polygon` (preferred) **or** `rot` 0–7 + `flip` + `at`. It is used to draw the silhouette and to find the outline corners (lock anchors). **It is not the only accepted answer:** any exact cover wins. |
| `art` | yes | The solved picture: `base` colour plus `shapes` drawn in order, clipped to the silhouette. |
| `provenance` | no | `author` = `human:<name>` or `ai:<model>`; `reviewedByHuman` must be `true` to ship. |

### Art shapes
| type | fields | notes |
|---|---|---|
| `polygon` | `points` | |
| `rect` | `x`, `y`, `w`, `h`, optional `rx` | |
| `circle` | `c`, `r` | |
| `ellipse` | `c`, `rx`, `ry` | |
| `line` | `from`, `to`, `stroke`, `width` | |
| `path` | `d` (absolute M/L/Q/C/Z, numbers in puzzle units), `stroke` and/or `fill` | |
Every shape may have `fill`, `stroke`, `width` (stroke width in units) and `opacity`. Colours are `#RRGGBB`. Everything outside the silhouette is clipped away, so the picture always has **exactly the puzzle's outline**.

**Art guidelines:** a flat style with 3–8 colours; the idea should be recognisable at thumbnail size; a few details (windows, eyes, stripes) are enough; no text; keep important details away from the very edge.

## 3. Pieces (local shapes for `rot`/`at`; anchor = first vertex)
| Id | Type | Local vertices (rot 0) | Area |
|---|---|---|---|
| LT1, LT2 | LT | (0,0) (4,0) (2,2) | 4 |
| MT | MT | (0,0) (2,0) (0,2) | 2 |
| ST1, ST2 | ST | (0,0) (2,0) (1,1) | 1 |
| SQ | SQ | (0,0) (1,−1) (2,0) (1,1) | 2 |
| PG | PG | (0,0) (2,0) (3,−1) (1,−1) | 2 |

World vertex = R(rot·45°) · F(flip) · local + at, where F mirrors x.

## 4. Validation rules (`tools/validate_puzzles.py`)
| Rule | Check |
|---|---|
| V1 | Required fields present; `difficulty` is 1–5; the category is known. |
| V2 | Valid piece ids, each used once. |
| V3 | Each polygon is exactly congruent to its piece. |
| V4 | No overlaps. |
| V5 | Connected through shared edges (corner-only joins are errors). |
| V6 | Area = the sum of the pieces (16). |
| V7 | Removed in round 6 (it checked a draft-0.1 field). The id stays unused. |
| V8 | `title.en` and `title.fi` exist. |
| V9 | `kind` is `mini`, `warmup` or `full`; `full` and `warmup` use all seven pieces, `mini` 1–6 distinct pieces. |
| V10 | `art` is present and well formed (base colour, known shape types, `#RRGGBB` colours, strokes on lines and paths). |
| V11 | **Build order (information only):** the order in which every piece locks on an outline corner or on a corner of an earlier piece. Every valid tangram has one: the uncovered region always has a convex corner, that corner is always an anchor (an outline corner or a placed piece's vertex), and the piece covering it must have a vertex there. The line is printed for authors; it never fails a puzzle that passed V3–V6. (Round 2 believed it rejected the classic square; round 6 corrected that.) |
| V12 | **Warm-up measure:** a `warmup` puzzle exposes at least half of every piece's outline on the silhouette edge (REQ-041). |
| V13 | Every art `path` `d` parses under the grammar of the Kotlin `PathData.kt` (absolute M L Q C Z only). |
| V14 | **Art colour count (REQ-039, DA-162):** the picture uses 3 to 8 visible colours, inclusive. Counted as the distinct normalised `#RRGGBB` values over `art.base` and every shape's `fill` and `stroke`; a shape drawn at an `opacity` below 1 adds one colour per distinct (colour, opacity) pair, because a tint over the base is a colour the player sees. A failing check. |
| V15 | **No pocket (DA-169):** the union of the placed pieces has no enclosed uncovered area, because the outline is one outer ring. Exact: the boundary edges are chained into closed walks; more than the one outer walk fails, even if the pocket meets the outside at a single pinch point. A 180° pinch on the outer boundary with no pocket stays allowed (DA-7). Exempt by id (never by kind), grandfathered because they render correctly and pass the device pixel tests: `shapes-warmup-2` (pinch at (4, 6)), `shapes-warmup-3` ((4, 3)), `shapes-warmup-4` ((2, 2)); the validator prints them as information. |

## 5. Authoring workflow (human or AI)
1. Sketch the figure on the grid (whole numbers when pieces use 0/90/180/270° turns; `[a, b]` values when 45° turns are needed).
2. Write the `solution` polygons and set `kind`, then run `python tools/validate_puzzles.py Tangrams` (it prints the build order; a warm-up also has to pass V12).
3. Write the `art` layer (simple shapes inside the outline).
4. Run `python tools/render_puzzle.py Tangrams Tangrams/previews` and **look at the sheet**: silhouette | pieces | picture. Is the silhouette recognisable without inner lines? Does the picture read well?
5. Run `python tools/build_prototype.py` and solve it in the prototype (the DEV reveal, REQ-046, shows the stored solution).
6. A human sets `reviewedByHuman: true`.

AI agent rules: never set `reviewedByHuman` yourself; always re-run the validator and look at the preview after any change.
