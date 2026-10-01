# Tangrams: the puzzle library

The puzzles live here, separate from the requirements (`../Requirements/`) and the spec (`../Spec/`). Every file is one puzzle in the `tangram-puzzle/1` format (`../Spec/03-puzzle-format.md`); `puzzle.schema.json` is its structural schema.

| File | Puzzle | Kind | Difficulty | Notes |
|---|---|---|---|---|
| `shapes-mini-1.json` | Mini: Pyramid | mini | 1 | 3 pieces (square + 2 small triangles). For a fast first success and "solved" testing. |
| `shapes-mini-2.json` | Mini: Window | mini | 1 | 3 pieces (medium + 2 small triangles). |
| `shapes-warmup-1..4.json` | Warm-up 1–4 | warmup | 1 | Easy starters. At least half of every piece's outline lies on the silhouette edge (rule V12), so each piece's shape can be seen in the silhouette. Found by `tools/puzzle_search/easy_search.py`. |
| `things-house.json` | House | full | 2 | |
| `things-arrow.json` | Arrow | full | 2 | |
| `animals-cat.json` | Cat | full | 3 | |
| `shapes-rectangle.json` | Chocolate bar (rectangle) | full | 3 | The classic 2 : 1 rectangle, found by `tools/puzzle_search/rect_solver.py` |
| `vehicles-sailboat.json` | Sailboat | full | 3 | |
| `nature-mountain.json` | Mountain | full | 4 | |
| `shapes-square.json` | Gift box (the classic square) | full | 4 | Back in the library since round 6 (2026-09-28): it was removed in round 2 on the belief that V11 rejects it, which was wrong (see below). |

A puzzle does not have to depict anything: abstract shapes and plain rectangles are fine (owner, 2026-09-27). The ‹ › order in the game is by **kind** (mini, warmup, full), then by difficulty, then by id (REQ-040).

## Rules every puzzle must pass
```bash
python tools/validate_puzzles.py Tangrams                  # V1–V6, V8–V10, V12; V11 prints the build order
python tools/render_puzzle.py Tangrams Tangrams/previews   # silhouette | pieces | picture sheets
python tools/build_prototype.py                            # the prototype picks up every *.json here
```
- **V9** checks the piece count for the puzzle's `kind`: `full` and `warmup` use all seven, `mini` uses 1–6.
- **V12** checks the warm-up measure: every piece of a `warmup` exposes at least half of its outline on the silhouette edge.
- **V11 never fails a valid puzzle.** It prints the edge-first build order (each piece locks on an outline corner or on a corner of a piece placed before it). Such an order always exists: the uncovered region always has a convex corner, that corner is always an anchor, and the piece that covers it must have a vertex there. "No hidden anchors" (round 2) is a rule of the runtime lock, not of the content; a piece dropped in the middle with nothing to hold simply goes home.

`provenance.reviewedByHuman` stays `false` until a person has solved the puzzle and looked at its picture. The first release needs at least 20 reviewed puzzles over four themes, including the gift box (REQ-042).

## Later
Non-traditional modes with other piece sets are planned (owner, 2026-09-27). The format will then gain a `pieceSet` field, and V9 will become "uses every piece of its set".
