# 03 · Tangram geometry: the exact maths (why it matters for snapping)

## The seven pieces (the "tans")

The unit is chosen so that the classic assembly is a **4 × 4 square** (area 16).

| Id | Piece | Legs / sides | Area | Symmetry | Notes |
|---|---|---|---|---|---|
| LT1, LT2 | Large triangle | legs 2√2, hypotenuse 4 | 4 | none | the two are interchangeable |
| MT | Medium triangle | legs 2, hypotenuse 2√2 | 2 | none | |
| ST1, ST2 | Small triangle | legs √2, hypotenuse 2 | 1 | none | the two are interchangeable |
| SQ | Square | side √2 | 2 | 4-fold (90°) | only 2 truly different turns: ◇ and □ |
| PG | Parallelogram | sides 2 and √2 | 2 | 2-fold (180°) | **chiral**: its mirror image is a different shape, so it needs flipping |

All angles are multiples of 45°. In a classic tangram every piece is turned by a multiple of 45°, so each piece has **8 turns** (16 including flips for the PG).

## Why exact arithmetic

Turning by 45° introduces √2. With floating point, "is this piece exactly in its slot?" becomes a fight against rounding: the kind of snapping bug users complain about in Tangram King. Every number that can appear has the form **a + b·√2** with rational a, b, so we store and compare coordinates **exactly** as pairs `[a, b]`:

- `3` → 3
- `[0, 2]` → 2√2
- `[4, -1]` → 4 − √2
- `[0, -2.5]` → −2.5√2

Consequences:
- **Slot matching is exact set equality** of vertex lists: no epsilons, no drift.
- **Interchangeable pieces** (LT1/LT2, ST1/ST2) and **symmetric turns** (the square turned 90°, the PG turned 180°) are handled for free, because the vertex *set* is the same.
- Float conversion happens only for drawing.

`tools/tangram_geom.py` is the reference implementation (class `Q2`). The Kotlin app must port it exactly. It is about 100 lines.

## Placement model

A placed piece = `(piece id, rot ∈ 0..7 (×45° clockwise), flip ∈ {false, true}, at = anchor point)`. The screen convention is **y pointing down**, so a positive angle turns clockwise on screen (the same as Android Canvas/Compose). Flip is applied before rotation (mirror x → −x).

Local shapes at rot = 0 (vertex 0 = anchor):
```
LT: (0,0) (4,0) (2,2)          hypotenuse on the x-axis, apex below
MT: (0,0) (2,0) (0,2)          right angle at the anchor
ST: (0,0) (2,0) (1,1)
SQ: (0,0) (1,-1) (2,0) (1,1)   a diamond ◇
PG: (0,0) (2,0) (3,-1) (1,-1)
```

## Useful facts for puzzle design

- Pieces turned by an **even** rot (0/90/180/270°) with the shapes above have **whole-number** coordinates. Mixing in odd turns brings in √2. Both are fine; whole-number puzzles are easier to author by hand.
- Two large triangles sharing a leg make a right triangle with legs 4. Sharing the hypotenuse, they make a 2√2 square.
- The two small triangles together can make the MT, the SQ or the PG. SQ + 2 ST and MT + 2 ST can each make a large triangle. These equivalences are why **different arrangements can fill the same silhouette** (see "alternative solutions" in the spec).
- Check new shapes with the validator instead of trusting intuition (an AI author should too).
- A shape where pieces meet **only at a corner** looks broken to children and cannot be made from a real wooden set. The validator rejects it (rule V5).

## Silhouette ("shadow") rendering

Draw the union of the solution polygons in one colour. To avoid hairline seams from anti-aliasing between neighbouring polygons, either draw each polygon with a same-colour stroke (about 4 % of the unit length) or compute the true union outline. The previews in `Spec/puzzles/previews/` use the stroke trick.
