# 03 · Puzzle file format `tangram-puzzle/1`

**Status:** draft 0.1. Reference code: `tools/tangram_geom.py`. Validator: `tools/validate_puzzles.py`. JSON Schema: `puzzles/puzzle.schema.json`. Examples: `puzzles/*.json`, with previews in `puzzles/previews/`.

This format is designed so that an **AI agent can author puzzles safely**. The coordinates are exact, the shapes are plain polygons you can read, and every rule below is checked by a machine.

## 1. Coordinate system
- Unit: the classic 7-piece set assembles into a **4 × 4 square**. Large-triangle hypotenuse = 4, small-triangle hypotenuse = 2, square side = √2.
- Axes: **x right, y down** (screen convention). Positive rotation is clockwise on screen.
- A **coordinate** is either a JSON number (rational: `2`, `1.5`, `-0.25`) or a pair **`[a, b]` meaning a + b·√2**.
  Examples: `[0, 2]` = 2√2, `[4, -1]` = 4 − √2, `[0, -2.5]` = −2.5√2.
- The app and the tools compare coordinates **exactly** (rational arithmetic in ℚ(√2)). Floats are used only for drawing.

## 2. File structure
```json
{
  "format": "tangram-puzzle/1",
  "id": "animals-cat",
  "title": { "en": "Cat", "fi": "Kissa" },
  "category": "animals",
  "difficulty": 3,
  "solution": [
    { "piece": "LT1", "polygon": [[0, 6], [4, 6], [2, 4]] },
    { "piece": "LT2", "polygon": [[4, 6], [4, 2], [2, 4]] },
    { "piece": "SQ",  "polygon": [[2, 2], [3, 1], [4, 2], [3, 3]] },
    { "piece": "ST1", "polygon": [[2, 2], [3, 1], [2, 0]] },
    { "piece": "ST2", "polygon": [[3, 1], [4, 2], [4, 0]] },
    { "piece": "PG",  "polygon": [[4, 6], [4, 4], [5, 3], [5, 5]] },
    { "piece": "MT",  "polygon": [[0, 4], [0, 6], [2, 4]] }
  ],
  "assist": { "preplacedOrder": ["LT1", "LT2", "MT", "SQ"] },
  "celebrate": { "sound": "meow", "eyes": [[2.6, 2], [3.4, 2]] },
  "provenance": { "author": "ai:claude", "reviewedByHuman": false }
}
```

| Field | Req. | Meaning |
|---|---|---|
| `format` | yes | Always `"tangram-puzzle/1"`. |
| `id` | yes | Unique, kebab-case, `<category>-<name>`. Also the file name. Never reuse an id: progress is keyed on it. |
| `title` | yes | Translations; `en` is mandatory. Shown only in the grown-up area and the accessibility labels. |
| `category` | yes | `starter`, `shapes`, `animals`, `people`, `things`, `vehicles`, `nature`, `letters` or `numbers`. |
| `difficulty` | yes | 1–5, the intrinsic difficulty *at level 3*: used to order the grid (easy first). |
| `solution` | yes | One entry per piece used. `piece` = `LT1 LT2 MT ST1 ST2 SQ PG` (a subset is allowed for starter puzzles). Either `polygon` (preferred, readable) **or** `rot` (0–7) + `flip` (bool) + `at` ([x, y] anchor). |
| `assist.preplacedOrder` | no | The order in which pieces become gift pieces at levels 1–2. Biggest and most "structural" pieces first. Default: LT1, LT2, MT. |
| `celebrate.sound` | no | Key of an ending sound (`meow`, `woof`, `toot`, `splash`…); an unknown key → the default chime. |
| `celebrate.eyes` | no | Points where blinking eyes are drawn in the ending animation. |
| `provenance.author` | no | `human:<name>` or `ai:<model>`. |
| `provenance.reviewedByHuman` | no | Must be `true` before a puzzle ships (release gate REQ-CNT-4). |

Polygon vertices may be listed in any order or direction. Matching uses the vertex **set**.

## 3. Pieces (local shapes for `rot`/`at` placement, anchor = first vertex)
| Id | Type | Local vertices (rot 0) | Area |
|---|---|---|---|
| LT1, LT2 | LT | (0,0) (4,0) (2,2) | 4 |
| MT | MT | (0,0) (2,0) (0,2) | 2 |
| ST1, ST2 | ST | (0,0) (2,0) (1,1) | 1 |
| SQ | SQ | (0,0) (1,−1) (2,0) (1,1) | 2 |
| PG | PG | (0,0) (2,0) (3,−1) (1,−1) | 2 |

World vertex = `R(rot·45°) · F(flip) · local + at`, where F mirrors x (x → −x) and R is the screen-clockwise rotation (x' = x·c − y·s, y' = x·s + y·c).

## 4. Validation rules (all enforced by `tools/validate_puzzles.py`)
| Rule | Check |
|---|---|
| V1 | `format`, `id`, `title`, `category`, `difficulty` (1–5) and `solution` are present and valid. |
| V2 | Piece ids are valid and each is used at most once. |
| V3 | Every polygon is **exactly congruent** to its piece (some rot 0–7 and flip maps the local shape onto it). |
| V4 | No two pieces overlap (separating-axis test; all tans are convex). |
| V5 | The shape is connected **through shared edges**. Pieces joined only at a corner do not count (children read that as broken, and a real wooden set would fall apart). |
| V6 | Total area = sum of piece areas (16 for a full set). |
| V7 | `assist.preplacedOrder` lists distinct pieces of this puzzle. |
| V8 | `title.en` exists. |

The tool prints `ok`/`FAIL` per file and exits with code 1 on any failure, so it can run in CI.

## 5. Authoring workflow (human or AI)
1. Sketch the figure on the integer grid. With pieces in "even" turns (0/90/180/270°) all coordinates are whole numbers; odd turns need `[a, b]` values.
2. Write the polygons into `Spec/puzzles/<id>.json` (or app content later).
3. Run `python tools/validate_puzzles.py Spec/puzzles`: it must say `ok`.
4. Run `python tools/render_puzzle.py Spec/puzzles Spec/puzzles/previews` and **look at the sheet** (colours / lines / shadow / solved). Ask: would a 5-year-old recognise it? Is the silhouette readable without inner lines?
5. A human sets `reviewedByHuman: true`.
6. Run `python tools/build_prototype.py` to play it in the prototype.

**AI agent rules:** never hand-edit a polygon without re-running the validator; never mark `reviewedByHuman` yourself; prefer whole-number coordinates; keep figures recognisable from the silhouette alone (levels 3–4 show only that).

## 6. Runtime use in the app
- At load time the app converts each slot to: type, exact vertex set, centre (vertex average), and the slot's `rot`/`flip` (computed by congruence search, 16 combinations).
- **Slot matching** (level 4) is exact set equality after snapping to the slot centre. Interchangeable pieces (LT1/LT2, ST1/ST2) and symmetric turns (SQ ×4, PG ×2) work automatically.
- The sample puzzles in this folder are included in the prototype.

## 7. Future versions
`tangram-puzzle/2` may add other piece sets (`pieceSet: "classic7" | "hungarian" | …`), alternative solutions, and hint scripts. v1 readers must reject unknown `format` values.
