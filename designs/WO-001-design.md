# Design — WO-001 · #Locking: pieces lock to corners or go home

**Author:** Design Author  ·  **Date:** 2026-10-02  ·  **Status:** Draft (revised after design review 01; baseline architecture.md v1.0, build-map v1.1)

## Scope

- **Implements (engine / state level, build-map v1.1 §2):** REQ-019 A1–A4, REQ-020 A1, REQ-021 A1–A2, REQ-051 A1–A2 (9 IDs). Carried to WO-003: REQ-020 A2 ("no error message"), the on-screen parts of REQ-021 and REQ-051, and the **verification of decisions F5** (an interrupted drag restores the pick-up pose; it is a calling rule, §7); 180 ms animations; sounds (WO-007).
- **Also in this WO (build-map v1.1):** kernel geometry incl. each solution piece's pose (O-01 `poseOf`); the G-03 golden route (`tools/export_geometry_golden.py` → `tools/golden/geometry.json`, kernel tests for all puzzle files); **a fix of the outline-corner rule in the Python reference and the kernel** (§5, DA-7); G-01 manifest hardening with verifier V-01 (blocking from now); V-05, V-06; the shared build setup (§10, step 0). Builds TYPE-001 geometry, TYPE-003 transforms, TYPE-004 search, exact arithmetic (ADR-003). Realizes REQ-002 (verified in WO-003). **Not built:** the solved check (REQ-022, kernel but WO-003).
- **Governed interfaces:** `IPuzzleLibrary` — consumes contract types only (`Puzzle.solution`, `Puzzle.kind`). `IProgressStore` — none. **No governed `I*` change.** One additive **notify-tier contract-delta**: a new file `kernel/…/kernel/model/PieceShapes.kt` (the TYPE-001 local-shape table that saved board positions depend on). No existing file under `kernel/model/` is edited. All arithmetic and algorithms live outside `kernel.model`.
- **Hard-stop domains touched:** none declared. The manifest edit (G-01) only *removes* permissions and switches backup off; governance row 10 concerns adding network/permissions/third-party code. `.swdev/guard.json` has no active `hard_stop_paths`.
- **Binding readings used:** decisions F2 (home keeps turn and mirror), F5 (interrupted drag), F23 (REQ-019: A1 = at the TYPE-004 winner; A3 = no anchor within the lock distance; A4 = matching turn and mirror within the lock distance), F31 (the anchor set never holds the moving piece's own corners). TYPE-004 wins over the prototype wherever they differ (§6 lists the differences).

## Chosen design

```
board (WO-003)  ──►  play.DropResolver  ──►  kernel.lock.LockSearch  ──►  kernel.geometry  ──►  kernel.model
 finger, layout       preview · release        TYPE-004, the only         a+b√2 operators,        Rational, Q2, ExactPoint,
 placed pieces        (REQ-019/020/021/051)    place that decides         PieceGeometry,          PieceId, Turn, PieceShape,
                                               where a piece locks (O-01) poseOf, Silhouette      + PieceShapes.kt (new, notify)
```

Everything in this WO's engine is pure, UI-free logic: no coroutines, no Android API, no clock, no mutable state. The board (WO-003) owns the placed pieces and the drag; it passes them in and applies the returned value (D2/D4: a session object would be designed blind before the board exists).

### 1. Kernel packages (O-01, notify rule)

| Package | Holds | Status |
|---|---|---|
| `kernel.model` | existing value types (`Rational`, `Q2`, `ExactPoint`, `PieceId`, `PieceShape`, `Turn`, `PuzzleState`) + **new `PieceShapes.kt`**: `val PieceShape.localCorners: List<ExactPoint>` | notify tier: only the one new file |
| `kernel.geometry` | `Arithmetic.kt` (operators), `PieceGeometry.kt` (`PieceGeometry`, `PlacedPiece`, `Vec2`, `poseOf`), `Silhouette.kt`, `ConvexClip.kt` (internal) | AI-owned |
| `kernel.lock` | `LockSearch.kt` (`LockSearch`, `Lock`, internal `Fit`) | AI-owned |

### 2. Exact arithmetic (TYPE-004 "exact", G-03, ADR-003) — extensions in `kernel.geometry`, nothing added to `kernel.model`

Operators on the existing types, built only on the public `Rational.of` / `Q2(a, b)` / `ExactPoint(x, y)`; all overflow throws via `Math.*Exact` (G-10: programmer error, impossible at puzzle scale).

| On | Extension (public, `kernel.geometry`) | Used for |
|---|---|---|
| `Rational` | `operator plus, minus, times, unaryMinus`; `signum(): Int` | everything below |
| `Q2` | `operator plus, minus, times, unaryMinus`; `signum(): Int`; `operator compareTo` (so `<`, `>` work; not `Comparable<Q2>`); `q2(a: Long, b: Long = 0L): Q2` | rotation by 45° steps, `at = anchor − offset`, exact order and equality |
| `ExactPoint` | `operator plus, minus`; `pointOf(x: Long, y: Long): ExactPoint`; internal `READING_ORDER: Comparator<ExactPoint>` (y, then x) | candidate positions, tie-break |

`times` is `(a + b√2)(c + d√2) = (ac + 2bd) + (ad + bc)√2` (as `tangram_geom.Q2.__mul__`). No division is needed anywhere.

**Sign of a + b√2, exactly** (tie-break order, edge tests, octants). With `sa = a.signum()`, `sb = b.signum()`:
- `sa == 0` → `sb`; `sb == 0` → `sa`; `sa == sb` → `sa` (no cancellation possible).
- Opposite signs: compare `a²` with `2b²` as `Rational`s; if `a² > 2b²` the sign is `sa`, else `sb`. Never equal: √2 is irrational (equality would need `a = b = 0`).

`Q2` equality stays data-class equality: `Rational` is normalized and a + b√2 is a unique representation, so anchor equality is `==`. Doubles never decide an equality or order of exact values.

### 3. TYPE-001 shapes and the TYPE-003 transform

**In `kernel.model` (the one contract-delta):** `val PieceShape.localCorners: List<ExactPoint>` — Spec/03 §3, in this vertex order (same as `PIECE_TYPES`): LT (0,0)(4,0)(2,2) · MT (0,0)(2,0)(0,2) · ST (0,0)(2,0)(1,1) · SQ (0,0)(1,−1)(2,0)(1,1) · PG (0,0)(2,0)(3,−1)(1,−1). Vertex 0 is (0,0) for every shape, so `PieceSave.OnBoard.at` is exactly where vertex 0 lies. The table and that convention are what saved positions depend on, hence notify tier; `PieceShape`/`PieceId` themselves are untouched.

**In `kernel.geometry`:**

```kotlin
object PieceGeometry {
    fun area(shape: PieceShape): Rational                    // LT 4, MT 2, ST 1, SQ 2, PG 2; the seven pieces sum to 16
    fun offsets(shape: PieceShape, turn: Turn, mirrored: Boolean): List<ExactPoint>   // R(turn)·F(mirror)·local; offsets[0] = (0,0)
    fun corners(piece: PieceId, turn: Turn, mirrored: Boolean, at: ExactPoint): List<ExactPoint>   // offsets + at
    fun poseOf(piece: PieceId, polygon: List<ExactPoint>): PlacedPiece?   // O-01: the pose that yields this polygon, or null
}
data class PlacedPiece(val piece: PieceId, val turn: Turn, val mirrored: Boolean, val at: ExactPoint) {
    val corners: List<ExactPoint>
}
data class Vec2(val x: Double, val y: Double)
```

- **Convention (identical to `PieceSave.OnBoard` and Spec/03 §3):** world = R(turn·45°)·F(mirror)·local + at. F mirrors x **before** rotating; y points down; positive turn is clockwise on screen: `x' = x·c − y·s`, `y' = x·s + y·c` with `c = [1, ½√2, 0, −½√2, −1, −½√2, 0, ½√2]`, `s = [0, ½√2, 1, ½√2, 0, −½√2, −1, −½√2]` (`COS`/`SIN` of `tangram_geom.py`, as `Q2`).
- `PlacedPiece` is the kernel's name for "a piece locked on the board" (`PieceSave.OnBoard` plus its id; `contracts` depends on `kernel`, so the kernel cannot use the contract type).
- **`poseOf`** (O-01; serves REQ-046 solve-now, REQ-038.A2, and golden checks) is the port of `placement_from_polygon`: for `mirrored` in (false, true), for `turn` in 0..7, for each polygon vertex `t` as `at`, return the first `PlacedPiece` whose `corners` equal the polygon **as a set** of exact points (same size); `null` if none (G-10). Symmetric pieces fit several poses; "unmirrored first, turn ascending, polygon vertex order" picks one deterministically, equal to Python's (DA-6).
- `area` is used by the inside test (TYPE-004: "clip area = piece area"); a test checks it against the exact shoelace of `localCorners` and the golden.
- **Not designed here:** the "turn keeps the centre fixed" rule of TYPE-003 — no in-scope REQ turns a piece (REQ-016/017/018 are WO-003); the lock never changes turn or mirror (REQ-019). WO-003 adds a centre helper on top of `corners`.

### 4. How a dragged piece's float position meets the exact world

The only floats are the **finger-driven pose** and the **distance** to a candidate. The board hands the engine `origin: Vec2` — *the float counterpart of `at`*: where the piece's local origin (vertex 0) currently is, in puzzle units (the board converts finger/centre/screen to that, WO-003). Nothing is ever rounded from float to exact:

- candidate position = `at = anchor − offset` (exact, from an exact anchor and an exact corner offset);
- its distance = `|at.toDouble() − origin|`. This is the same number as TYPE-004's `|t| = |a − v|` for any corner `v`, because the translation that carries the dragged piece onto the candidate is the same for every corner/anchor pair that yields the same `at`;
- a locked piece is drawn and stored from the exact `at`; later locks use its exact corners as anchors, so "a locked piece sits exactly on its anchor and later locks against it are exact too" (TYPE-004) holds by construction.

### 5. Outline corners and the anchor set (REQ-019 "Anchor points are only…", REQ-051)

```kotlin
class Silhouette(val polygons: List<List<ExactPoint>>) {      // the stored solution polygons, any order/orientation
    val outlineCorners: List<ExactPoint>                       // first-appearance order (as Python); compare as a set
}
```

**The rule (DA-7).** A solution vertex `p` is an outline corner — a point "where the silhouette's outline turns" (Study/06) — unless the directions covered by pieces around `p` form **exactly one contiguous run of 4 wedges (a straight side) or all 8 wedges (inside)**. A *wedge* `k` (k = 0..7) is the 45° sector between direction `k` and direction `k+1`, where direction `k` has angle `k·45°` clockwise on screen from +x (y down). The earlier port counted only the **total** angle (180° ⇒ straight), which is wrong for a point where pieces cover 180° in **separate** runs: `shapes-warmup-4` at (2,2) — MT and LT2 cover three adjacent wedges, ST2 covers one wedge elsewhere — is a place where the outline turns, but the total rule calls it a straight side and it is not an anchor. The new rule only *adds* anchors (a contiguous 4-run or the full circle always has total 4 or 8), so no build order can be lost; it fixes the Python reference and the kernel together (§8).

Exact computation in the kernel (TYPE-001/003 guarantee every edge direction is a multiple of 45°; validator V3 enforces it for every shipped polygon): for each distinct solution vertex `p` and each polygon `q`, add wedges to an 8-bit mask:
- if `p` is vertex `i` of `q`: with `u`, `w` the exact vectors to the previous and next vertex, `oct(v)` is the direction as an octant 0..7 from the exact signs of `(dx, dy)` (axis-aligned, or `|dx| == |dy|` — otherwise `require` fails, a content error). The interior sector is the short arc between `oct(u)` and `oct(w)`: if `(oct(w) − oct(u)) mod 8 = d ≤ 4` the wedges `oct(u) … oct(u)+d−1`, else (with `d' = (oct(u) − oct(w)) mod 8`) the wedges `oct(w) … oct(w)+d'−1` (all mod 8);
- else if `p` lies strictly inside an edge of `q` (exact: cross product `== 0` and `0 < dot < |edge|²`): the half-plane on `q`'s side, 4 wedges — `o … o+3` if an off-line vertex of `q` has a positive exact cross sign `(b − a) × (v − a)` (with `o = oct(b − a)`), else `o+4 … o+7`;
- otherwise nothing.

`p` is **not** a corner iff `mask == 0xFF` or the mask is four set bits forming one circular run (`0b1111` rotated by some `k`); otherwise it is. An internal `isOutlineCornerMask(mask: Int): Boolean` holds that decision and is unit-tested on its own (full circle, a run of 4 wrapping 6,7,0,1, runs 3+1 and 2+2 separate, runs of 1..3, 5 and 6).

**Anchor set** = `silhouette.outlineCorners` ∪ corners of every `PlacedPiece` whose `piece != moving` (REQ-019; F31: never the moving piece's own corners — filtered **by id inside `LockSearch.find`**, so drop, preview and later tap-turn/mirror (REQ-016/018) share the rule, O-01). Anchors are a set of exact points.

Agreement with the reference is checked on **every** puzzle file through the golden (§8), and `shapes-warmup-4` (2,2) is pinned as a corner by a named test. Spot values (compare as sets): `shapes-mini-1` → {(2,0), (0,2), (4,2)} · `shapes-mini-2` → {(0,0), (2,0), (0,2), (2,2)} · `things-house` (needs √2) → {(0,2), (2,0), (0,4), (3,1), (3,−1), (4,0), (4, 4−√2), (4+√2, 4−√2), (4+√2, 4)} — unchanged by the fix.

### 6. The TYPE-004 search (`kernel.lock.LockSearch`, O-01)

```kotlin
object LockSearch {
    const val BASE_DISTANCE = 0.65; const val MIN_DP = 30.0
    const val SNUG_BONUS = 0.04;    const val WINDOW = 0.16
    const val TOLERANCE = 1e-6;     const val TIE_EPS = 1e-9
    fun lockDistance(dpPerUnit: Double): Double      // max(0.65, 30 / dpPerUnit); dpPerUnit ≤ 0 or NaN → 0.65 (no crash path from layout)
    fun find(silhouette: Silhouette, placed: List<PlacedPiece>, piece: PieceId, turn: Turn,
             mirrored: Boolean, origin: Vec2, lockDistance: Double): Lock?     // null = no valid spot (G-10)
}
data class Lock(val at: ExactPoint, val distance: Double, val cornersOnAnchors: Int) { val score: Double }  // distance − 0.04·n
internal data class Fit(val insideDeficit: Double, val maxOverlap: Double)        // areas, units²
internal fun LockSearch.fitAt(silhouette: Silhouette, others: List<PlacedPiece>, piece: PieceId,
                              turn: Turn, mirrored: Boolean, at: ExactPoint): Fit
```

Steps, exactly:
1. `others = placed.filter { it.piece != piece }`; `offs = offsets(piece.shape, turn, mirrored)`. **Only this turn and mirror are tried** (TYPE-004: a piece one step off does not lock; never the other mirror image).
2. Candidates: for every `o` in `offs` and every anchor `a`: `at = a − o` (exact); `d = hypot(at.x.toDouble() − origin.x, at.y.toDouble() − origin.y)`; keep if `d <= lockDistance` (plain double `<=`). **A candidate is identified by its exact `at`** — the same `at` reached by several corner/anchor pairs is one candidate.
3. Order candidates by `d` ascending, then `READING_ORDER(at)`. Scan:
   - **valid(at)** ⇔ `fit.insideDeficit ≤ TOLERANCE && fit.maxOverlap ≤ TOLERANCE`, with `fit = fitAt(…)`: `insideDeficit = area(P) − Σ_j area(P ∩ S_j)` over the silhouette polygons `S_j` (disjoint, V4, so the sum is the area of `P ∩ silhouette`; correct for a non-convex silhouette because `P` and every `S_j` are convex); `maxOverlap = max over other pieces Q of area(P ∩ Q)` (0 with none). `P` is the float polygon of the exact corners; `area(P)` is the exact `PieceGeometry.area`.
   - the first valid candidate fixes `nearest = d` (**"best |t|" = smallest |t| among valid candidates**); stop when `d > nearest + WINDOW`.
   - `cornersOnAnchors` = number of the candidate's exact corners `==` to an anchor (exact equality, not the prototype's 1e-6 float compare); `score = d − 0.04·n`.
   - keep the lowest score under the tie-break below.
4. Return the winner `Lock` or `null`.

**The clip contract (`ConvexClip.intersectionArea(a, b): Double`, internal; port of prototype `clip`/`interArea`, lines 219–234).** Every lock is decided at exact flush contact, where a clip edge is collinear with a subject edge, so the details are pinned:
- both polygons are first oriented so that the shoelace sum is ≥ 0 (prototype `ccw`); the subject is clipped against each edge `A→B` of the clipper in turn;
- `side(P) = (B − A) × (P − A)`; a vertex is **inside inclusively**: `side(P) ≥ −CLIP_SIDE_EPS`, with `CLIP_SIDE_EPS = 1e-9`;
- for a subject edge `P→Q`: emit `P` if `P` is inside; and only if **exactly one** of `P`, `Q` is inside emit the crossing `P + t·(Q − P)` with `t = sp / (sp − sq)`. On such a sign change `sp − sq` is strictly nonzero (one value is `≥ −eps`, the other `< −eps`), so there is never a 0/0; `t` is also clamped to `[0, 1]` so a noise-level crossing cannot extrapolate (the one deviation from the prototype, which does not clamp);
- no textbook line-line intersection (`cross(dirP, dirS)`) anywhere: it divides by zero exactly on collinear edges and in Kotlin returns NaN/∞ silently, which would make `insideDeficit` NaN and refuse a legal flush spot (REQ-019.A4, REQ-002) with no error;
- area = `|shoelace| / 2` of the result, and **0 when it has fewer than 3 vertices**; the result is always finite.

**Why the area tolerance is safe (evidence, DA-3).** TYPE-004 says "a tolerance of at most 1e-6 units"; the prototype and this design apply 1e-6 to **area** (units²). Read literally those differ: a corner that pokes a depth `h` past a line cuts an area of about `h²/2 … h²`, so a 1e-6 area tolerance admits protrusions up to ~1e-3 units deep — about 1000× looser than 1e-6 as a length. The two readings give the **same decisions** because no reachable placement has a nonzero offending area anywhere near that: the design review swept every candidate of every shape, turn and mirror against the empty board, every build-order prefix and every one-piece non-solution board on all 13 puzzle files and found the smallest nonzero protrusion or overlap area to be **1.263e-3** (first at `things-arrow`), with nothing in (1e-9, 1e-3). All coordinates are a + b√2 with small denominators, and by the norm bound `|a² − 2b²| ≥ 1/den²` nonzero areas cannot get arbitrarily small; true flush contact is exactly 0 and float noise is ~1e-15. That evidence becomes a **kernel margin test** (§8): over every golden puzzle, no `Fit` value (`insideDeficit`, `maxOverlap`) evaluated on the sweep below lies in (1e-9, 1e-4). A future puzzle that approaches the tolerance then fails loudly instead of locking wrongly. Fallback if it ever fails: exact clipping (Alternatives).

**The 0.16 window changes nothing.** Every candidate has `n ≥ 1` (its generating corner is on an anchor) and `n ≤ 4`, so `score ∈ [d − 0.16, d − 0.04]`; a candidate with `d > nearest + 0.16` has `score ≥ d − 0.16 > nearest`, above the score of the nearest valid candidate itself (≤ `nearest − 0.04`), so it can never win. TYPE-004's window (against the best |t|, not the prototype's best score) is implemented as the cheap stop condition above; no test can distinguish it. This holds under DA-2's reading ("best |t|" = smallest |t| among *valid* candidates). *(Capture-side note: redundant for pieces of ≤ 4 corners; harmless, not a defect.)*

**Tie-break — ASSUMPTION DA-1 (TYPE-004 has none, F23):** candidate `c` beats the current best `b` if, in this order: `c.score < b.score − 1e-9`; else, scores within 1e-9: `c.distance < b.distance − 1e-9`; else, distances within 1e-9: `c.at` is earlier in reading order (smaller y, then smaller x, by exact `Q2` comparison). The scan order of step 3 is fixed, so the result depends only on the candidate set, never on list order. Rationale: the first two keys are the REQ's own "nearest/best"; the last is a total order any other implementation (Python, F15) can copy. Worked example — `shapes-mini-1` with its square placed, a small triangle at resting turn 4, `origin = (3,2)`, R = 1.2: the holes `at (2,2)` and `at (4,2)` both have `d = 1`, `n = 3` → the left one `(2,2)` wins (and `(2,2)` is reached from three corner/anchor pairs: one candidate).

Differences from the prototype's `findLock` (TYPE-004 wins): exact `at` instead of float centres; candidates keyed by `at`; window against best |t|; snug count by exact equality; ties by the rule above instead of insertion order; the clip clamps `t`; and an interrupted drag is not a drop (§7).

### 7. The `play` module — drop resolution, preview, pulse (`DropResolver.kt`)

```kotlin
data class DragPose(val piece: PieceId, val turn: Turn, val mirrored: Boolean,
                    val origin: Vec2,          // §4
                    val overBoard: Boolean)    // false = over the tray or outside the board (layout is the board's, WO-003)

sealed interface DropOutcome {                                         // what a *release* can produce — nothing is ever loose (REQ-020)
    data class Locked(val placed: PlacedPiece) : DropOutcome           // REQ-019: same turn and mirror as dropped
    data class Home(val piece: PieceId, val turn: Turn, val mirrored: Boolean,   // F2: keeps its turn and mirror
                    val pulse: CornerPulse?) : DropOutcome             // REQ-051; no message or error field (REQ-020.A2, carried to WO-003)
}
data class CornerPulse(val corners: List<ExactPoint>)                  // the outline corners, nothing else

class DropResolver(puzzle: Puzzle) {                                   // builds Silhouette(puzzle.solution.map { it.polygon }) once
    fun preview(pose: DragPose, placed: List<PlacedPiece>, dpPerUnit: Double): PlacedPiece?   // REQ-021: where a release locks now; null = draw nothing
    fun release(pose: DragPose, placed: List<PlacedPiece>, dpPerUnit: Double): DropOutcome    // REQ-019 / REQ-020 / REQ-051
}
```
(`ExactPoint`, `PieceId`, `Turn` from `kernel.model`; `Vec2`, `PlacedPiece` from `kernel.geometry`; `LockSearch` from `kernel.lock`.)

- `preview` and `release` share one private function: `if (!pose.overBoard) null else LockSearch.find(silhouette, placed, pose.piece, pose.turn, pose.mirrored, pose.origin, LockSearch.lockDistance(dpPerUnit))`. **Same function, same arguments ⇒ the preview never disagrees with the lock** (REQ-021 rule, A1); `overBoard = false` gives no preview and a go-home (REQ-020, REQ-021.A2).
- `release`: lock found → `Locked(PlacedPiece(piece, turn, mirrored, lock.at))`; otherwise `Home(piece, turn, mirrored, pulse)` with `pulse = CornerPulse(silhouette.outlineCorners)` **only if** `puzzle.kind == PuzzleKind.MINI && pose.overBoard`, else `null` (REQ-051: mini only; warm-up/full show nothing, A2). One `Home` carries at most one pulse, so a missed drop on the board pulses once and the next missed drop pulses again — ASSUMPTION DA-4 (F30; not for a deliberate drop on the tray/outside, as the prototype's `missedOnBoard`). The 600 ms length, reduced motion and drawing are WO-003.
- Nothing is loose: `DropOutcome` has exactly two cases and both are resting states (on the board, validated; or back in the tray). A board that applies the returned value verbatim cannot leave a piece in between.
- **Calling rules for WO-003 (the engine is stateless, so these are rules, re-verified there):**
  1. **Frame sync (REQ-021.A1 on screen):** the preview is drawn at most once per frame, so on release the board calls `release` with **the pose of the last *displayed* preview**.
  2. **Interrupted drag (decisions F5, DA-5):** a drag that ends without a release (system cancel, app hidden, window change) never calls `release` or `preview`. The board restores the piece's **whole pick-up pose** (tray cell, or its board position, with the turn and mirror it had when picked up) silently — no search, no pulse, no sound. The whole pose, not just the position: restoring a board piece with another turn than it was locked with could overlap its neighbours. This has no WO-001 seam (an `interrupt` function would have no logic and a test that cannot fail); the verification is carried to WO-003, where the gesture exists.

### 8. G-03 golden route, the reference fix and the kernel tests

**Reference fix (`tools/tangram_geom.py`, `outline_corners`; DA-7).** Replace the angle-sum with the wedge-mask rule of §5 — in Python's own style, **float with a sanity check**, so it stays independent of the kernel's exact signs: the octant of a vector is `round(atan2(dy, dx) / (π/4)) mod 8`, asserted to be within 1e-9 of a multiple of 45°; the half-plane side from the sign of the cross product against an off-line vertex; same `isOutlineCorner(mask)` decision. `build_order` (which calls `outline_corners`) is unchanged and can only gain anchors. **Consequences for the orchestrator (AGENTS rules 8 and 10):** `tools/build_prototype.py` embeds `outline_corners`, so `Spec/prototype/tangram-prototype.html` gains the (2,2) anchor of `shapes-warmup-4` after `python tools/build_prototype.py`; then run `python tools/validate_puzzles.py Tangrams`, `python tools/tests/test_prototype.py` and re-render the sheets.

`tools/export_geometry_golden.py` (stdlib Python; imports `tangram_geom` and `validate_puzzles.load_solution`) writes `tools/golden/geometry.json` — deterministic (sorted keys, `\n`, indent 1). It exits non-zero if a puzzle file fails to load, `build_order` leaves a piece stuck, or the **Python-side agreement check** fails: an independent sampling oracle recomputes each puzzle's outline corners geometrically (for every solution vertex, the inside/outside of the 8 wedge mid-directions at a radius of 1e-3 against the float polygons, then the same contiguity decision) and must equal the reference's `outline_corners` on every file — this is what would have caught the old rule — and the exporter also asserts that `shapes-warmup-4` lists (2,2). Exact numbers are **strings** (`R = "n"` or `"n/d"`; `Q = [R, R]` is a + b√2; `P = [Q, Q]` is a point): the puzzle files' JSON floats are not exact for thirds.

```
{ "format": "tangram-golden/1",
  "shapes":     { "LT": {"local": [P…], "area": R}, "MT": …, "ST": …, "SQ": …, "PG": … },
  "transforms": [ {"shape": "PG", "turn": 3, "mirrored": true, "corners": [P…]}, … ],      // 5 shapes × 8 turns × 2 mirrors, at = (0,0)
  "puzzles":    { "<id>": { "sha256": "…", "kind": "mini",
                            "solution": [ {"piece": "SQ", "polygon": [P…], "pose": {"turn": 0, "mirrored": false, "at": P}}, … ],
                            "outlineCorners": [P…], "area": R, "buildOrder": ["SQ", "ST1", "ST2"] } } }
```
`pose` is `placement_from_polygon`'s answer and `buildOrder` is `build_order`'s. `sha256` is over the file's bytes with CRLF normalized to LF. WO-002 adds validator verdicts to the same exporter. The exporter is re-run with AGENTS.md rule 10 whenever `Tangrams/` or `tools/` change.

Kernel tests (`kernel/src/test`, reading the golden with `kotlinx-serialization-json`'s `Json.parseToJsonElement`, **test scope only**):
- **Freshness:** the set of `Tangrams/*.json` (not the schema, README or previews) equals the golden's `puzzles` keys and every hash matches — otherwise "re-run tools/export_geometry_golden.py".
- **Shapes/transforms:** `localCorners` and `area` equal `shapes`; `offsets` equals every one of the 80 `transforms` rows (the convention check).
- **Per puzzle file:** `Silhouette(polygons).outlineCorners` equals `outlineCorners` as a set; **`shapes-warmup-4` (2,2) is a corner** (named pin); `isOutlineCornerMask` cases; `poseOf` equals `pose`, its `corners` equal the polygon as a set; Σ exact shoelace areas = `area` = Σ `PieceGeometry.area`.
- **Clip degenerate cases (`ConvexClip`, §6 contract):** a shared full edge, a shared partial edge, identical polygons, one polygon inside the other, touching at a single vertex, and 45° edges with √2 coordinates (the house roof / its square at 4+√2) — each asserts a finite area (0, or the piece area within 1e-12), never NaN.
- **Margin test:** for every golden puzzle, evaluate `fitAt` for every `at = anchor − offset` (all offsets, all anchors, **no distance limit**) of every `PieceId` not on the board, every turn and mirror, against (i) the empty board, (ii) every build-order prefix at the golden poses, (iii) every board of exactly one solution piece; assert every `insideDeficit` and `maxOverlap` is ≤ 1e-9 or ≥ 1e-4 (dedupe by `at` per board; if the run is too slow, one `PieceId` per shape).
- **Round trip (the strongest test):** for every puzzle, place the pieces in `buildOrder`; each dropped at its true pose (`origin` = the true `at`, also with a ≤ 0.05 offset) locks at exactly its solution `at`.

### 9. G-01 manifest hardening and V-01 (blocking from this WO)

`app/src/main/AndroidManifest.xml` (add `xmlns:tools="http://schemas.android.com/tools"`):

```xml
<permission android:name="${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" tools:node="remove" />
<uses-permission android:name="${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" tools:node="remove" />
<application android:allowBackup="false" android:dataExtractionRules="@xml/data_extraction_rules" … >
    <provider android:name="androidx.startup.InitializationProvider"
              android:authorities="${applicationId}.androidx-startup" android:exported="false" tools:node="merge">
        <meta-data android:name="androidx.emoji2.text.EmojiCompatInitializer" tools:node="remove" />
    </provider>
```
`app/src/main/res/xml/data_extraction_rules.xml`: `<cloud-backup>` and `<device-transfer>`, each with `<exclude domain="…" path="."/>` for **all nine** domains: `root`, `file`, `database`, `sharedpref`, `external`, `device_root`, `device_file`, `device_database`, `device_sharedpref` (G-01 / decisions F7: "exclude everything"; the device-protected ones are empty today, so this is latent hardening).

**`androidx.startup:startup-runtime` is declared directly in `app`** (`implementation(libs.androidx.startup.runtime)`, catalog entry in step 0, version = the one already resolved transitively, read from `:app:dependencies`). D5 note: it is an AndroidX library already on the runtime classpath, so it adds no code to the APK; it guarantees the provider stanza's class exists and resolves `lintRelease`'s `MissingClass` (the review reproduced that `lintRelease` fails without it). G-02 asks for a `decisions.md` row for any library not named in §5; this one is not, so log it there.

**The `registerReceiver` rule, stated precisely** (for WO-003 and code review): no registration with `RECEIVER_NOT_EXPORTED` through `ContextCompat.registerReceiver`, in project or library code, because with the injected permission removed it throws on API 26–32. The design review's dex scan of today's debug APK found exactly one library caller, Compose UI's `MediaQuery` (DOCK_EVENT), which passes `RECEIVER_EXPORTED` and never touches the removed permission. Any other `<uses-permission>` a library injects later is found by V-01 and removed the same way (or the dependency is the problem, G-01/G-02).

**V-01** `.swdev/verifiers/v01_release_manifest.py [--manifest PATH] [--res-dir DIR]` — stdlib Python, exit 0 pass / 1 check failed / 2 could not build. Without `--manifest` it runs `gradlew(.bat) :app:processReleaseMainManifest` itself, then takes the newest `AndroidManifest.xml` under `app/build/intermediates/merged_manifest*/release/`. Checks (`xml.etree.ElementTree`): no element named `permission` or starting `uses-permission` (covers `-sdk-23`); `<application android:allowBackup="false">`; `android:dataExtractionRules` present and the referenced `xml/<name>.xml`, resolved in `--res-dir` (default `app/src/main/res`; the self-test points it at a fixture tree), has both sections excluding **all nine** domains; no `<meta-data>` named `androidx.emoji2.text.EmojiCompatInitializer`. One line per violation (`V-01 FAIL uses-permission …`).

### 10. Gradle, files and the other verifiers

**Step 0 — build setup (one task, first; nothing later edits a shared build file, so the later tasks can run in parallel).** Shared Android/JVM settings are written **once in the root build script** (ADR-002: "as soon as a third module needs them" — we have three, and `play` would be the fourth copy):
- root `build.gradle.kts`: add `alias(libs.plugins.android.library) apply false`, and one `subprojects { plugins.withId(…) { … } }` block: for `org.jetbrains.kotlin.jvm` — Java 17 source/target and `jvmTarget = JVM_17`; for `com.android.application` and `com.android.library` — `compileSdk = 37`, `minSdk = 26`, Java 17 `compileOptions` (`targetSdk = 37` for the application only). Then `kernel`, `contracts` and `app` lose their copies (`kernel` and `contracts` keep only their plugin and dependencies). Version-catalog `[versions]` entries read from the root block are an acceptable variant if configuring by extension type hits AGP 9 DSL friction (then adjust ADR-002's wording).
- `gradle/libs.versions.toml`: **every** new entry in this step — plugin `android-library` (`version.ref = "agp"`); library `kotlinx-serialization-json` (a version that resolves with Kotlin 2.4.20; ADR-004 allowlist, test scope here, reused by `content` in WO-002); library `androidx-startup-runtime` (§9).
- `settings.gradle.kts`: `include(":play")`.
- `play/build.gradle.kts` (Android library, ADR-002; AGP 9 built-in Kotlin, so no `kotlin-android`): `namespace = "io.github.jamisuni.tangram.play"`, `implementation(project(":kernel"))`, `implementation(project(":contracts"))`, `testImplementation(libs.junit)`. No manifest, no resources, no Compose yet (D4).
- `kernel/build.gradle.kts`: `testImplementation(libs.kotlinx.serialization.json)`; on the test task `systemProperty("tangram.root", rootDir.absolutePath)`, `inputs.dir(rootProject.file("Tangrams"))` and `inputs.files(rootProject.file("tools/golden/geometry.json"))` (`files`, not `file`, so the build works before the golden exists; without the inputs `org.gradle.caching=true` could replay a cached pass after the puzzles or the golden changed).
- `app/build.gradle.kts`: `implementation(libs.androidx.startup.runtime)`; its shared lines move to the root block.
- Check: `gradlew assembleDebug test :app:processReleaseMainManifest` stays green. The new D5 entries go into the WO's "new dependencies" line and `decisions.md`.

**Files by later tasks (no build file among them):** `kernel/src/main/kotlin/…/kernel/model/PieceShapes.kt` (the delta); `…/kernel/geometry/{Arithmetic,PieceGeometry,Silhouette,ConvexClip}.kt`; `…/kernel/lock/LockSearch.kt`; `play/src/main/kotlin/…/play/DropResolver.kt`; tests in `kernel/src/test/kotlin/…` and `play/src/test/kotlin/…` (acceptance tests under `play/`, trace-check rule 3); `tools/tangram_geom.py`, `tools/export_geometry_golden.py`, `tools/golden/geometry.json`; the manifest, `res/xml/data_extraction_rules.xml`; `.swdev/verifiers/*`.

- **V-06** `.swdev/verifiers/v06_module_deps.py [project_root]` — stdlib Python. Modules = `include(":x")` lines of `settings.gradle.kts`. For each, strip comments of `<x>/build.gradle.kts` and match `config(project(":y"))`. **Main scope only** (architecture G-06): configurations starting with `test` or `androidTest` are ignored (test scope may add `:content`, §5); every other (`implementation`, `api`, `debugImplementation`, …) is checked. A `project(` reference not of that form (`projects.y`, `project(path = …)`) is itself a FAIL. Allowed: `kernel → {}`; `contracts → {kernel}`; `content, store, play, browse, time, settings, devtools → {kernel, contracts}`; `app → any`. A module not in the table, an `include` without a build file, or a build file not included → FAIL. Exit 1 with `V-06 FAIL play -> browse`, else 0.
- **V-05** `.swdev/verifiers/v05_string_parity.py [project_root]` — per module, over all `src/*/res`: collect `(tag, name)` of every `<string>`, `<plurals>`, `<string-array>` in `values*/` XML (skip `translatable="false"`); the `values/` (en) set must equal the `values-fi/` set; print each key missing on either side. No `res` passes (vacuously for `play`; `app` has `app_name` in both).
- `.swdev/verifiers/test_verifiers.py` (`python -m unittest`): fixtures prove each verifier **can fail** — V-01 (via `--manifest` and `--res-dir`): a `uses-permission`, a `permission`, `allowBackup` true/missing, no extraction rules, rules missing one domain, `EmojiCompatInitializer` present, plus a clean pass; V-06: a `play → browse` main edge, a `projects.` accessor, and the passing `testImplementation(project(":content"))`; V-05: a key missing in `values-fi`, a `translatable="false"` exemption. A verifier that cannot fail is worth nothing at every WO close.

**Abstractions introduced** *(each names its REQ — D3)*

| Abstraction / seam | Kind | Required by |
|---|---|---|
| Module `play` | module (ADR-002, build-map #Locking) | REQ-019/020/021/051: their code home; trace-check rule 3 |
| `PieceShapes.kt` (`PieceShape.localCorners`) | `kernel.model`, notify delta | TYPE-001 shapes; the vertex-0 convention of `PieceSave.OnBoard.at` |
| Arithmetic extensions, `signum` | `kernel.geometry` | TYPE-004 exact `at`, TYPE-003 rotation, G-03 |
| `PieceGeometry`, `PlacedPiece`, `Vec2`, `poseOf` | `kernel.geometry` | TYPE-001/003; `PlacedPiece` is the lock result and anchor source (REQ-019); `poseOf` is O-01 (REQ-046, REQ-038.A2 later; golden checks now) |
| `Silhouette` (+ sector-rule outline corners, `isOutlineCornerMask`) | `kernel.geometry` | REQ-019 anchors, REQ-051 pulse corners, TYPE-004 "inside" |
| `ConvexClip`, internal `Fit`/`fitAt` | internal | TYPE-004 validity (inside, overlap); the margin test reads `Fit` |
| `LockSearch`, `Lock` | `kernel.lock` | TYPE-004, O-01 (one search for drop, preview, REQ-016, REQ-018) |
| `DragPose`, `DropOutcome`, `CornerPulse`, `DropResolver` | `play` | REQ-019/020 (outcome, nothing loose), REQ-021 (preview), REQ-051 (pulse), decisions F2 |
| Golden exporter + golden file + sampling oracle + kernel golden tests; reference fix | tool, data, tests | G-03 (architecture v1.0): kernel agrees with `tangram_geom.py` on every puzzle file; REQ-019 anchor rule (DA-7) |
| Root shared-settings block | build | ADR-002 (third module) |
| Manifest edits, `data_extraction_rules.xml`, `startup-runtime` dependency | `app` | G-01 (REQ-001/008/010 side), V-01 |
| V-01, V-05, V-06 + self-test | Python verifiers | architecture.md §2 (G-01, G-05, G-06) |

No `i*` interface is introduced: the only seams are plain functions between `play` and `kernel` inside the allowed dependency direction.

## Test seams *(an independent test author needs these signatures, not the bodies)*

Acceptance tests are JVM unit tests at `DropResolver` in `play/src/test` (engine / state level, build-map v1.1). **Fixtures are literal exact coordinates** written in the test (not generated by `PieceGeometry`/`poseOf`, so a convention error cannot cancel out): e.g. `shapes-mini-2`: MT turn 0 at (0,0) = (0,0)(2,0)(0,2); ST1 turn 2 at (2,0) = (2,0)(2,2)(1,1); ST2 turn 4 at (2,2) = (2,2)(0,2)(1,1); the full 4×4 `shapes-square` polygons are in `Tangrams/shapes-square.json`. The matching R is `LockSearch.lockDistance(dpPerUnit)`; keep drop offsets clear of the exact boundary 0.65.

| Acceptance | Public function | Check |
|---|---|---|
| REQ-019.A1 | `DropResolver.release` | pose within R of a valid spot → `Locked`, `placed.at ==` exact (anchor − offset), same turn/mirror as dropped |
| REQ-019.A2 | `release` over a grid of poses/turns around a fixture | every `Locked` is inside the silhouette and overlaps no placed piece (oracle written in the test, independent of `kernel`'s clip) |
| REQ-019.A3 | `release`; `LockSearch.find` | no anchor within R (e.g. piece in the middle of an empty 4×4) → `Home`; `find == null`. Plus: valid anchor but turn one step off → `Home` (TYPE-004) |
| REQ-019.A4 | `release` | six pieces placed, seventh at matching turn/mirror within R → `Locked` at the exact hole. **Arrangements: at least one that is not the stored solution** ("any arrangement"): e.g. the `shapes-square` solution reflected x → 4−x (LT1 (4,0)(0,0)(2,2), LT2 (4,0)(2,2)(4,4), MT (0,2)(0,4)(2,4), ST1 (0,0)(0,2)(1,1), SQ (2,2)(1,1)(0,2)(1,3), ST2 (2,2)(1,3)(3,3), PG (4,4)(3,3)(1,3)(2,4) — the PG needs the other mirror than the stored one); and `shapes-mini-2` with the MT in the opposite corner (2,2)(0,2)(2,0) and the two small triangles in the others |
| REQ-020.A1 | `release` | every outcome is `Locked` (valid) or `Home`; `overBoard = false` → `Home` even with an anchor in reach; `Home` keeps turn/mirror (F2) |
| REQ-021.A1 | `preview` vs `release` | for each pose: `preview(p) == (release(p) as Locked).placed` |
| REQ-021.A2 | `preview` + `release` | no valid spot → `preview == null` **and** `release is Home`; `overBoard = false` → `preview == null` |
| REQ-051.A1 | `release` | `kind = MINI`, `overBoard = true`, no lock → `Home.pulse.corners` equals the silhouette's outline corners (set), exactly one pulse per `Home`; no piece position in it |
| REQ-051.A2 | `release` | same drop, `kind = WARMUP`/`FULL` → `pulse == null`; MINI with `overBoard = false` → `null`; `Locked` never pulses |

Carried to WO-003 (not WO-001 tests): REQ-020.A2 (no error message on screen), the dashed outline and the pulse on screen (REQ-021, REQ-051), and F5 (interrupted drag restores the pick-up pose).

Kernel tests (`kernel/src/test`): `Q2.signum`/`compareTo` (incl. 3−2√2 > 0, 99−70√2 > 0, opposite-sign cases, equality); the golden, pin, clip, margin and round-trip tests of §8; `LockSearch.find`: score, window as stop only, own corners excluded (F31), dedupe by `at`, the tie-break example of §6, `lockDistance` (0.65 vs 30 dp, ≤ 0 input), turn/mirror strictness; `poseOf` returns `null` for a non-congruent polygon. Python: `test_verifiers.py` (§10).

## Alternatives considered

| Alternative | Why not chosen |
|---|---|
| **Arithmetic as members of `Rational`/`Q2`/`ExactPoint`** (first draft) | Simpler call sites, but `kernel.model` is notify tier: every operator would be a logged contract-delta on AI-owned algorithms. Extensions in `kernel.geometry` leave the contract-referenced files untouched. Cost: `Q2` is not `Comparable<Q2>` (an `operator compareTo` extension gives `<`/`>`, a `Comparator` gives sorting). |
| **Validity by exact arithmetic** (exact clipping) | Feasible, not infeasible: every edge lies on a 45° multiple, so lines can use integer direction vectors with cross products ±1 or ±2 and intersections need only division by small integers in ℚ. The real cost is a **second clip implementation and its tests** for no observable gain — §6's evidence shows the float area test decides identically at puzzle scale and G-03 allows it. Kept as the **named fallback** if the margin test (§8) ever fails. |
| **Float angle total for outline corners** (the earlier port) | Shorter, but the total misses a turn of the outline where the covered directions are not contiguous (`shapes-warmup-4` (2,2)), and the golden could not see it because both sides shared the rule. Wedge masks cost ~30 lines. |
| **Keep the Python rule, log the narrowing as an assumption** | Would pin today's behaviour but narrow a locked REQ rule ("outline corners") and leave V11's argument false; the fix only adds anchors and touches one reference function the WO already rebuilds the golden from. |
| **An `interrupt(piece): Restore` seam** (first draft) | It had no logic (always `Restore(piece)`) and its test could not fail; F5's observable behaviour exists only where the gesture is. A WO-003 calling rule instead (D2/D3). |
| **Shared settings per module, or deviate from ADR-002** | Four copies of bytecode 17 / SDK levels; ADR-002 says write them once from the third module on. The root block is ~15 lines. |
| **Golden read by a tiny hand-written JSON reader** instead of `kotlinx-serialization-json` (test scope) | No new catalog entry, but ~60 lines of parser to write and test, and WO-002/004 need the library in main scope anyway (ADR-004). |
| **Golden for three puzzles only** (first draft) | The point of G-03 is agreement on *every* file; with the exporter the per-file cost is zero, and the round trip then covers all files. |
| **Centre-based drag position** (as the prototype) | Needs a centroid concept (no in-scope REQ turns a piece) and a conversion before the exact `at`. `origin` is the float twin of `at`; WO-003 adds the centre helper with REQ-016/017. |
| **Stateful `DragSession`** holding pick-up pose, last preview, pulse | Would make the calling rules (§7) enforceable in code, but is designed before the board's state exists (D4). Cost: two calling rules for WO-003, re-verified there. |
| **Outcome = `contracts.PieceSave`** | Couples `play`'s drop logic to the notify-tier `IProgressStore` the WO declares untouched; the payload is already in `PlacedPiece`/`Home`. WO-004 maps one to the other. |
| **Tie-break variants:** "prefer an outline-corner anchor", "prefer more anchored corners", "first found" | The first adds a preference nobody asked for; the second is already the score; the third depends on list order. Reading order is a total order on exact values. |
| **`play` as pure Kotlin/JVM until WO-003** | ADR-002 and build-map make `play` the Android code home of #Locking; a later module change would move tests (trace-check rule 3). |
| **Everything in `kernel`** (no `DropResolver`) | The kernel owns the *search* (O-01) but not the REQ-020 outcome or the mini-only pulse (`Puzzle.kind`); those are `play`'s drop resolution (architecture.md §3). |

## Risks & edge notes

- **Highest risk — the validity predicate and anchor completeness at exact contact.** Every lock passes through "inside the silhouette, no overlap" on edges that are exactly flush, and through an anchor set that must contain *every* corner a real solution needs. A slip (clip contract, non-convex silhouette summed wrongly, a missing anchor, wrong convention) either lets a piece lock where it must not (REQ-019.A2) or refuses a legal spot, making a puzzle unfinishable (A4, REQ-002) — silent until a human plays it. Mitigation: the round trip on **all** puzzle files, the sampling-oracle golden corners, the clip degenerate-case tests and the margin test (§8). The Design Reviewer should read §6 (clip contract) and §5 first.
- **Convention drift (silent):** mirror-before-rotate, clockwise-on-y-down, `at` = vertex 0. Internal tests would pass if consistently wrong, but saved games, puzzle files and WO-002's conversion would break. Hence the 80-row `transforms` golden, the notify-tier shape table and the literal acceptance fixtures.
- **The reference fix has a blast radius outside this WO:** `tangram_geom.py` also feeds the validator's build order and the embedded prototype. The change only adds anchors, but the prototype must be rebuilt and rule 10 re-run (§8) before close.
- **Stale or platform-dependent golden:** the freshness test (hashes) fails loudly; CRLF normalization keeps Windows/Linux hashes equal; the Gradle `inputs` keep the build cache honest.
- **Preview ≠ drop** if WO-003 resolves at a pose other than the last displayed preview (§7 rule 1).
- **Float ties:** the epsilon comparison is not transitive in pathological chains; determinism holds because the scan order is fixed. `d <= R` is a literal double compare: tests must not sit on the exact boundary.
- **Overflow / crash paths:** `Rational` throws on `Long` overflow (G-10), impossible at puzzle scale. `Silhouette` and `find` `require` only programmer errors (non-45° edge, empty polygon list); touch input (NaN, huge coordinates) yields `null`/`Home`, never an exception.
- `play` has no strings in this WO, so V-05 passes vacuously for it; the first user-visible text (WO-003) must arrive in both languages (G-05).
- **Documents to align (not a design decision):** `workorders/WO-001.md` still says 10 acceptance IDs and no manifest/V-01 scope; build-map v1.1 §2 (9 IDs, manifest, V-01, golden) governs.

## Not designed here (so nobody builds it silently)

Drawing, gestures, the 12 dp threshold, animations and sounds (180 ms lock/return, 600 ms pulse, reduced motion), the board/tray layout and the `overBoard` test, the pick-up pose bookkeeping and its restore (F5), the centre-preserving turn (REQ-016/017/018), the solved check (REQ-022, WO-003), puzzle file loading, saving, `app` wiring — WO-003, WO-002, WO-004, WO-007. No Compose dependency is added.

**Suggested cut for the Planner:** **0** build setup (§10 step 0: shared settings, catalog entries, `:play`, kernel test config, `startup-runtime`; one task, first, the only one that edits build files); then **1** arithmetic extensions + `signum` + tests; **2** `PieceShapes.kt` + `PieceGeometry` + `poseOf`; **3** reference fix + exporter + sampling oracle + golden + golden tests for shapes/transforms/poses; **4** `Silhouette` (sector rule) + corner goldens + pin; **5** `ConvexClip` + `LockSearch`/`Fit` + tests incl. degenerate clips, margin test, round trip; **6** `play` + `DropResolver` + acceptance tests. Order 0→1→2→3→4→5→6. **7** manifest + `startup` stanza + V-01, V-05, V-06 + self-test may run parallel to 1–6 because after step 0 it touches no shared build file (manifest, `res/xml`, `.swdev/verifiers`).

## Assumptions to log in `decisions.md` (agent proposals, class 12 — ambiguity)

| ID | Reading | Serves |
|---|---|---|
| DA-1 | Tie-break: equal score (within 1e-9) → smaller \|t\| (within 1e-9) → exact reading order of `at` (y, then x). Candidates are identified by their exact `at`. | TYPE-004 (no tie-break), F23 |
| DA-2 | "Best \|t\|" in the 0.16 window = the smallest \|t\| among *valid* candidates; the window can never change the winner (n ∈ 1..4 ⇒ score ∈ [d − 0.16, d − 0.04]); implemented as a stop condition. | TYPE-004 |
| DA-3 | TYPE-004's "tolerance of at most 1e-6 units" is applied as 1e-6 on clip **area** (units²), as in the prototype; equivalent in decisions because no reachable placement has a nonzero offending area below ~1.26e-3 (measured on all 13 files, enforced by the margin test); "corners on anchors" is exact equality; `\|t\| <= R` is a plain double compare. | TYPE-004, G-03 |
| DA-4 | Pulse: mini puzzles only, only when the missed drop was over the board, once per missed drop (not once ever); a drop on the tray/outside the board goes home without a pulse. | REQ-051, F30 |
| DA-5 | An interrupted drag never reaches `release`/`preview`; the board restores the **whole pick-up pose** (position, turn, mirror). A WO-003 calling rule, no WO-001 seam; verification carried to WO-003. | F5 |
| DA-6 | `poseOf` for a polygon that several poses produce (symmetric pieces): unmirrored before mirrored, turn ascending, first polygon vertex as `at` — the order of `placement_from_polygon`. | O-01, REQ-046 (aid poses), REQ-038.A2 |
| DA-7 | An outline corner is a solution vertex whose covered directions are not exactly one contiguous 4-wedge run or the full circle (the outline turns there); the old "total angle 180° ⇒ straight" rule is replaced in `tangram_geom.outline_corners` and the kernel. Only adds anchors; shipped case: `shapes-warmup-4` (2,2). | REQ-019 (rule 1), Study/06, V11 argument |

## Handoff — Design Author · WO-001
- **Scope:** REQ-019 A1–A4, REQ-020 A1, REQ-021 A1–A2, REQ-051 A1–A2 at engine/state level (realizes REQ-002; TYPE-001, TYPE-003, TYPE-004) + G-03 golden and reference fix, G-01/V-01 manifest, V-05, V-06, shared build setup · governed touched: `IPuzzleLibrary` (consumes `Puzzle.solution`, `Puzzle.kind`); `IProgressStore` none; no `I*` change; one notify-tier delta (new `kernel/model/PieceShapes.kt`)
- **Inputs read:** WO-001 · `reviews/WO-001-design-review.md` · REQ-019/020/021/051/002/045 · req_types.md v0.2 (TYPE-001/003/004/006) · architecture.md v1.0 (§2 G-01/G-03/G-05/G-06/G-10, §3, §3b O-01, §4, §5; checked against v0.10: no change affecting this design) · build-map.md v1.1 (§2) · ADR-002, ADR-003 · design-inputs.md v0.2 §2 · decisions.md (F2, F5) · req_review_01.md F23, F30, F31 · contracts (`IPuzzleLibrary`, `Puzzle`, `SolutionPiece`, `SavedGame`) · kernel `model/` (`Exact.kt`, `Pieces.kt`) · settings.gradle.kts, libs.versions.toml, root/kernel/contracts/app build files, app manifest and strings · Study/06, Study/08 · tools/tangram_geom.py, tools/validate_puzzles.py (`load_solution`), Spec/03 §1/§3 · tools/prototype_template.html 201–234, 495–614
- **Result:** `C:\GitHub\AI\TangramNoAds\designs\WO-001-design.md`
- **Status:** forward → Design Reviewer (spot-check F1–F5)
- **Traceability delta:** REQ-019/020/021/051 ↔ `play.DropResolver` (`preview`/`release`) ↔ `kernel.lock.LockSearch.find`; TYPE-001/003 ↔ `kernel.model.PieceShapes` + `kernel.geometry.PieceGeometry`; O-01 ↔ `poseOf`; TYPE-004 ↔ `LockSearch`/`ConvexClip`; REQ-019 anchor rule ↔ `Silhouette` sector rule + `tangram_geom.outline_corners` fix (DA-7); G-03 ↔ golden exporter + kernel golden tests; G-01 ↔ manifest + V-01; G-05/G-06 ↔ V-05/V-06; F5 carried to WO-003; no task/code/test links yet
- **Notes for next station:** contract-delta row needed in the WO (`PieceShapes.kt`); DA-1…DA-7 and the `startup-runtime` D5 row to log in decisions.md. The orchestrator must rebuild the prototype and run AGENTS rule 10 after the `tangram_geom.py` fix. The 0.16 window is redundant (§6) — a capture-side note, not a defect.
