# Tangrams: the puzzle library

The puzzles live here, separate from the requirements (`../Requirements/`) and the spec (`../Spec/`). Every file is one puzzle in the `tangram-puzzle/1` format (`../Spec/03-puzzle-format.md`); `puzzle.schema.json` is its structural schema.

| File | Puzzle | Difficulty | Notes |
|---|---|---|---|
| `shapes-mini-1.json` | Mini: Pyramid | 1 | 3 pieces (square + 2 small triangles), `"mini": true`. For a fast "solved" test. |
| `shapes-mini-2.json` | Mini: Window | 1 | 3 pieces (medium + 2 small triangles), `"mini": true`. |
| `shapes-warmup-1..4.json` | Warm-up 1–4 | 1 | Easy starters for testing. Found by a search that keeps at least half of every piece's outline on the silhouette edge, so each piece's shape can be seen in the silhouette. |
| `things-house.json` | House | 2 | |
| `things-arrow.json` | Arrow | 2 | |
| `animals-cat.json` | Cat | 3 | |
| `shapes-rectangle.json` | Chocolate bar (rectangle) | 3 | The classic 2 : 1 rectangle, found by an exhaustive solver that uses the game's own locking rule |
| `vehicles-sailboat.json` | Sailboat | 3 | |
| `nature-mountain.json` | Mountain | 4 | |

A puzzle does not have to depict anything: abstract shapes and plain rectangles are fine (owner, 2026-09-27). The ‹ › order in the game is by difficulty, then by id.

## Rules every puzzle must pass
```bash
python tools/validate_puzzles.py Tangrams                  # V1–V11, prints each puzzle's edge-first build order
python tools/render_puzzle.py Tangrams Tangrams/previews   # silhouette | pieces | picture sheets
python tools/build_prototype.py                            # the prototype picks up every *.json here
```
`tools/puzzle_search/` holds the searches that found the warm-ups (`easy_search.py`) and the rectangle (`rect_solver.py`); their output is raw material, not puzzle files.

V11 (buildable edge-first) is the rule most often failed: every piece must be able to lock on a silhouette corner or on a corner of a piece placed before it.

`provenance.reviewedByHuman` stays `false` until a person has solved the puzzle and looked at its picture.

## Later
Mini puzzles (`"mini": true`) already use a subset of the classic set; V9 then accepts 1–6 distinct pieces. Non-traditional modes with other piece sets are planned (owner, 2026-09-27). The format will then gain a `pieceSet` field, and V9 will become "uses every piece of its set".
