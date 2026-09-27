# 06 · Locking pieces without slots ("anchor snapping")

*Study note. Updated in round 2 (2026-09-27 08:39): **the only anchors are the silhouette's corners and the corners of pieces already placed.** There are no hidden anchor points. A piece that would float in the middle without neighbours is not supported, and puzzles must be buildable edge-first (validator rule V11).*

## The rule in one sentence
When a piece is dropped, the game looks **near the drop point** for a position where one of the piece's corners sits exactly on an **anchor**, the piece lies **completely inside the silhouette**, and it **overlaps no other piece**. The closest such position wins. If there is none, the piece glides back to its tray cell.

## Anchors
| Group | What it is |
|---|---|
| **Outline corners** | Every point where the silhouette's outline turns. Points along a straight side are *not* corners. |
| **Placed-piece corners** | The corners of every piece already on the board. They are added and removed live as pieces come and go. |

That's all. The player's mental model matches exactly: *"a piece ties to the corners of the shape, or to the pieces I have already put down."*

How outline corners are found: for each solution vertex, add up the angle covered by the solution pieces around that point (a vertex contributes its interior angle; a point on the middle of an edge contributes 180°). A total of **360°** means inside the shape, **180°** means on a straight side, and **anything else is a corner**. This is implemented as `tangram_geom.outline_corners`.

## Puzzles must be buildable edge-first (V11)
With only these anchors, a piece whose corners touch no outline corner can lock only after a neighbour gives it a corner to hold. Every puzzle must therefore have **a build order** in which each piece has a corner on an outline corner or on a corner of an earlier piece. `tools/validate_puzzles.py` checks this with a greedy search and prints the order it found:
```
ok   things-house.json: ... build order LT1 LT2 MT PG SQ ST1 ST2
```
Consequences:
- The player can always build from the outside in, or grow from a piece already placed.
- Dropping a piece "in the middle of nowhere" sends it home. That is intended: there is nothing to tie it to yet.
- The classic "big square" puzzle was removed at Jami's request (it was also the example that needed hidden anchors).

## Candidate search (exact procedure, used by the prototype)
Inputs: piece type, current turn *k* (0–7) and flip *f*, drop centre *c*, the placed pieces, the difficulty settings.
1. **Variants:** (k, f). Easy also tries k ± 1. Easy and Medium also try the mirrored parallelogram.
2. For every variant, every piece corner *v*, and every anchor *a*: shift *t = a − v*; keep it if |t| ≤ lock distance *R* (Easy 0.9, Medium 0.65, Hard 0.45 units; at least 30 dp on screen).
3. **Valid** if the piece is completely inside the silhouette (clip area = piece area) and overlaps no placed piece (clip area = 0), with tolerance 1e-6.
4. **Score** = |t| + 0.25 × turn change + 0.1 × flip change − 0.04 × (piece corners landing on anchors, a snug-fit bonus).
5. Best score → glide (180 ms) and "click". None → back to the tray.

At most about 6 variants × 4 corners × ~40 anchors, so it is cheap enough to run on every drag frame for the landing preview.

## Landing preview (Easy and Medium)
A dashed outline shows where the piece would lock if released now. It shows a *valid* spot, never the *correct* one. It is off on Hard.

## Finishing: any correct solution wins
Each locked piece is inside the silhouette and overlaps nothing, and the pieces' total area (16) equals the silhouette's area. So **7 locked pieces means an exact cover**, whatever arrangement was found. Alternative solutions are fine as long as each piece was anchored when it was placed, which is true of any real, physically buildable arrangement.

## Other interactions on the same rule
- **Tap a board piece** → turn +45° about its centre and search around the same centre (same turn only). If it fits it stays; otherwise it turns back with a small shake.
- **Two-finger twist while dragging** → 45° steps; the landing preview follows.
- **Pick up a placed piece** → its corners stop being anchors until it locks again.

## Comparison with the market
| App behaviour (from reviews) | Ours |
|---|---|
| Pieces snap badly or unpredictably | A deterministic nearest-valid rule plus a landing preview |
| Pieces land "in random spots" | No loose pieces: lock or go home |
| Only the stored solution counts | Any exact cover wins |
