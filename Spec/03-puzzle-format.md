# 03 · Puzzle file format `tangram-puzzle/1`

**Status:** draft 0.3. Reference code: `tools/tangram_geom.py`. Validator: `tools/validate_puzzles.py`. JSON Schema: `puzzles/puzzle.schema.json`. Examples: `puzzles/*.json`. Review sheets: `puzzles/previews/`.

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
| `title` | yes | Translations; `en` is mandatory, `fi` is expected. |
| `category` | yes | `shapes`, `animals`, `people`, `things`, `vehicles`, `nature`, `letters`, `numbers` |
| `difficulty` | yes | 1–5. Sets the ‹ › order (easy first) and the dots in the top bar. |
| `solution` | yes | **All 7 pieces.** Each entry: `piece` plus `polygon` (preferred) **or** `rot` 0–7 + `flip` + `at`. It is used to draw the silhouette and to find the outline corners (lock anchors). **It is not the only accepted answer:** any exact cover wins. |
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
| V7 | (optional) `assist.preplacedOrder` is consistent. Legacy from draft 0.1. |
| V8 | `title.en` exists. |
| V9 | All seven pieces are used. |
| V10 | `art` is present and well formed (base colour, known shape types, `#RRGGBB` colours, strokes on lines and paths). |
| V11 | **Buildable edge-first:** there is an order in which every piece has a corner on an outline corner or on a corner of an earlier piece. Figures that need a piece floating in the middle are rejected. |

## 5. Authoring workflow (human or AI)
1. Sketch the figure on the grid (whole numbers when pieces use 0/90/180/270° turns; `[a, b]` values when 45° turns are needed).
2. Write the `solution` polygons, then run `python tools/validate_puzzles.py Spec/puzzles` (it prints the edge-first build order, or the pieces that can never lock).
3. Write the `art` layer (simple shapes inside the outline).
4. Run `python tools/render_puzzle.py Spec/puzzles Spec/puzzles/previews` and **look at the sheet**: silhouette | pieces | picture. Is the silhouette recognisable without inner lines? Does the picture read well?
5. Run `python tools/build_prototype.py` and solve it in the prototype on Easy, and at least one puzzle on Hard.
6. A human sets `reviewedByHuman: true`.

AI agent rules: never set `reviewedByHuman` yourself; always re-run the validator and look at the preview after any change.
