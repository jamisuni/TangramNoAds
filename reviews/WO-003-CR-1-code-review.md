# Review — code · WO-003 checkpoint CR-1 (TASK-012 … TASK-017b, logic only)

**Date:** 2026-10-02  ·  **Reviewer:** fresh context (code-reviewer)
**Inputs:** AGENTS.md, WO-003 design (incl. Test seams, Assumptions), tasks.md WO-003 section, decisions.md DA-15…DA-34, REQ-012/013/014/015/016/017/018/019/020/021, TYPE-001/003/004/006/007 (`Requirements/req_types.md`), the in-scope kernel and play sources, the implementer scaffolding tests, and the moved-in acceptance tests read only as behavioural reference (`play/src/test/**/acceptance/Seams.kt`, `Req051…`). Held-out folder not read. Out of scope as briefed: Compose drawing, pointer adapter, app shell.

## Verdict

**revise (spot-check only)** — no Blocker. Boundaries, exactness and the REQ-021 calling rule all re-derive clean. Five Should findings; F1 and F2 change seams that TASK-018a/018b consume, so they should be fixed before 018a starts. F3-F5 can be fixed any time before WO close. Re-review by spot-check.

**0 Blockers · 5 Should · 8 Nit/Note**

## Evidence re-derived

- `gradlew :kernel:test :play:testDebugUnitTest --rerun-tasks`: BUILD SUCCESSFUL (30 s). Result XMLs: kernel 127 tests, play 119 tests, 0 failures, 0 errors, 0 skipped (matches the claim).
- `python .swdev/verifiers/v06_module_deps.py .` PASS; `v05_string_parity.py .` PASS (both play `strings.xml`: the same 9 keys).
- `kernel/model/*` and `contracts/**`: untouched in WO-003 (mtimes 06:28-07:28 vs the WO-003 sources 20:19+; the only guard event for `kernel/model` is the WO-001 `PieceShapes.kt` delta). No `I*` change.
- Guardrails: play main dependencies are kernel, contracts and Compose foundation (G-06); no network, permission, analytics or new runtime library (rule 1, G-02); strings en+fi, identical key sets (rule 13, G-05).
- Token audit of every non-acceptance test in scope (`play/src/test/*.kt`, `kernel/src/test/**/layout`, `kernel/.../lock`): no `REQ-NNN.An` text anywhere; the scaffolding tests carry none (rule respected).
- Spec checks done by hand: TYPE-001 resting turns checked against `PieceShapes.localCorners` (LT/ST turn 4 = long side down, apex up; MT turn 1 = hypotenuse down; SQ turn 1 upright; PG turn 0 leans right), colours equal TYPE-001, TYPE-007 boundary 599/600, TYPE-006 transitions.
- Phone dpPerUnit vs trayScale for all shipped puzzles (script, scratch): the lowest board scale is 36.9 dp/unit (mountain, arrow) against a tray scale of 24-29, so "grows to board size" never shrinks on shipped content.

## Checklist applied

- [x] **Directives / guardrails** — G-02, G-03 (every decision path that fixes a position is exact: `at = anchor - offset`; Double only in distance, clip area, score and the finger pose), G-05, G-06, G-10 (see F1, F2 for the two holes).
- [x] **Contract** — nothing in `kernel.model`, `contracts`, `IPuzzleLibrary`; new kernel types are all outside the notify tier.
- [x] **Scope** — one unrequested abstraction (F5); `TrayRules.order/turnDiameter/colour/restingTurn` are needed.
- [x] **Traceability / token rule** — clean (above).
- [x] **Hard-stops** — none touched.
- [x] **Evidence re-derived** — above.
- [x] **Conceptual 20 %** — attacks below.

## Attack results (each flagged item judged)

1. **PlaySession observability (flag 1).** Not a rewrite. All mutation goes through intents and `private set`; `pieceList` is replaced wholesale (snapshot-friendly). But `DragState` is mutated in place (`finger`, `turn`, `frame` at PlaySession.kt:57-60, written at :157/:162/:169), so no state read invalidates a Canvas when `onFrame` freezes a new frame. Cheapest correct base: one `private var version by mutableIntStateOf(0)` read by every public getter and bumped by each intent, `onFrame` and `expire`. Note the design (section 5) says "fields are `mutableStateOf`" while TASK-015a left plain fields (deferred to 018a, recorded in the task row). See N1.
2. **PathData strictness (flag 2).** Judged in F4.
3. **Conditional scaffolding assertion (flag 3).** There are two, not one; judged in F3.
4. **Extra TrayRules facts (flag 4).** `order` (used by PlaySession.kt:81) and `turnDiameter` (used by PlayLayout.kt:118; REQ-013 table, tested to 0.005) are needed. `sizeMark` is not: F5.
5. **GestureMachine choices (flag 5).**
   - Twist rounded to 1e-9 deg (GestureMachine.kt:178-184): sound. It only absorbs float noise at an exact boundary; no pointer can express 5e-10 deg. ±22.5 gives 0, 22.6 gives 1, 67.5 gives 1, 67.6 gives 2, 270 deg continuous gives 6 (REQ-017 A1/A2, DA-18 re-derived by hand and by the scaffolding table). No NaN path (`roundToLong` would throw on NaN, but atan2 of finite points is finite).
   - Later piece wins an overlap (HitTest.kt:33): harmless (valid locks never overlap beyond 1e-6 area, so it only decides a shared-edge pixel), but "draw order" here is `TrayRules.order`, not lock order, and the nearest-edge tie rule (HitTest.kt:40, first wins) is the opposite direction. Nit N5.
   - 12 dp inclusive (HitTest.kt:19, `<=`): matches REQ-015 "shape grown by 12 dp" (closed set); the tap/drag split is `>= 12` = drag, `< 12` = tap, matching "less than 12 dp". DA-16's stickiness (once past 12 dp it is a drag even if the finger returns) is a logged reading stricter than the REQ's "ends having moved"; accepted as logged.
   - Refused `beginDrag` leaves the finger `Ignored` (GestureMachine.kt:141-145): fail-safe, but silent; see F2 (the only reachable refusal is the layout disagreement).
6. **DA-32 `badgeRect(session): RectDp?` (flag 6).** Null cases are complete (SOLVED, no PG in the puzzle, PG dragged; PG-on-board-but-not-in-`placed` is unreachable) and HitTest uses `?.contains(p) == true` (HitTest.kt:27); `GestureMachine.flipBadge` has an `else -> Unit`. Clean. One unlogged reading: the badge is hidden once solved (N6).
7. **`PieceDrawing.scale` throws (flag 7).** Wrong shape: F1.
8. **Two layout sources (flag 8).** Can disagree; should not be silent: F2.

## Findings

### F1 — SEAM / FAIL-CLOSED · Should · play/PieceDrawing.kt:19-25 · owner TASK-017b
- **Observation:** `scale(state, layout)` throws `IllegalArgumentException` for `Where.Dragged`. During every drag frame the dragged piece IS in `session.pieces` with `where = Dragged`, so the natural 018a draw loop (`pieces.forEach { PieceDrawing.scale(it, layout) }`) throws on the first frame of the first drag, inside a Canvas draw: an uncaught exception, i.e. a crash on touch (G-10, "never crash on touch"). The throw makes the trap discoverable only on a device. The test (`PieceDrawingScaffoldingTest.draggedPieceThrowsWithClearMessage`) pins the throw, so it hardens the trap.
- **REQ/decision:** REQ-014 A2, REQ-013; DA-33 ("dragged → fail-closed"); G-10.
- **Proposed resolution:** make the dragged case unrepresentable or total. Keep the frozen two-argument seam for tray and board (the acceptance tests call it by reflection, `Seams.kt:255`), but add the drawing-side total function, e.g. `fun scale(state: PieceState, layout: PlayLayout, drag: DragState?): Double` returning `drag.frame?.scale ?: drag.pickUp.scale` for `Dragged` (and `error` only if `Dragged` without a `drag`, which is a real bug). Update the scaffolding test to pin the total result instead of the throw. 018a calls the total one.

### F2 — INTEGRATION / FAIL-CLOSED · Should · play/PlaySession.kt:113,126,207,235 and play/GestureMachine.kt:30,81 · owner TASK-017 (with TASK-015a)
- **Observation:** the layout has two sources. `GestureMachine.down` hit-tests against `layoutProvider()` (GestureMachine.kt:81); `beginDrag`, `refit`, `frameAt` and `release` use `session.layout` (internal `var`, PlaySession.kt:113). They can disagree in real use: on a size change `PlayArea` computes a new layout, the provider lambda sees it first, `session.layout` is set a recomposition later, and the same touch is hit-tested on layout A and dragged with layout B (different `dpPerUnit`, pick-up centre and lift). With `session.layout == null`, `beginDrag` returns silently (:126), `refit` returns silently (:207), `release` returns `null` and leaves the piece dragged (:235). The 018b adapter author hit exactly this (every drag became a no-op).
- **REQ/decision:** REQ-014/015/021 correctness; design section 4 (one `PlayLayout` passed to `LockSearch.lockDistance` everywhere, G-10).
- **Proposed resolution:** one source of truth. Keep the frozen `GestureMachine(session, layoutProvider)` constructor (the acceptance Seams use it) but have `down` assert agreement: `check(session.layout == null || session.layout === layout)`-style, or better set `session.layout = layout` from the one provider in one place (`PlaySession.attach(layout)`), so `beginDrag` can never see a different one. Then make a null layout inside `beginDrag`/`refit` a loud programming error (`checkNotNull`), not a touch-input path: after this change the machine never reaches them with null, so loud does not violate G-10. Also make `release` with a layout-less drag fall back to `interruptDrag()` instead of returning null with the piece stuck `Dragged`.

### F3 — TEST · Should · play/src/test/.../PlayAnimationScaffoldingTest.kt:121 and :154 · owner TASK-015b
- **Observation:** two conditional assertions, both able to pass vacuously. (a) `missPulseLivesSixHundredAndRecordsReduced` wraps the whole pulse lifetime check (`pulse.reduced`, alive at 899, gone at 900) in `if (out is DropOutcome.Home && out.pulse != null)`; if the centre drop locks or has no pulse, the test passes having asserted nothing, for both reduced and not. This is the only JVM test of the 600 ms pulse lifetime and of the `reduced` flag recording. (b) `badgeRectByWhere` wraps the board-badge assertion in `if (PG is Board)`; a first full puzzle whose PG touches no outline corner does not lock (REQ-019 A3), the `if` is skipped, and the on-board branch of `badgeRect` is untested. I could not prove either branch executes by reading; the test cannot tell.
- **REQ/decision:** REQ-051 timing, DA-19, DA-32 (board branch).
- **Proposed resolution:** assert the precondition instead of branching: `assertTrue(out is DropOutcome.Home)` and `assertNotNull(out.pulse)` (choose a spot known to miss, as `PlaySessionScaffoldingTest.missOverBoardPulsesOnceAndTrayDropDoesNot` does at the board corner); for (b) pick a puzzle/piece whose PG touches an outline corner or place neighbours first, and `assertTrue` it is on the board before asserting the rect.

### F4 — QUALITY · Should · play/PathData.kt:47-54,72 and tools/validate_puzzles.py · owner TASK-014
- **Observation (flag 2):** the grammar rejects implicit sign separators (`1-2`), exponents, `+`, implicit repetition, tabs/newlines. All of these are valid SVG and common in minified or tool-generated paths; `1-2` in particular is unambiguous (a minus always starts a number). The only gate is the build test `everyShippedPathParses`; the schema's `d` is a bare `string` and `validate_puzzles.py` does not check it. So a new puzzle passes the project's own validator (AGENTS rule 10) and then fails `gradlew test` with a pointer to a Kotlin test, a split gate with two answers. At run time a rejected path only drops one decoration (right per G-10), but REQ-039 makes the picture the reward, so a silently missing shape is a visible defect.
- **REQ/decision:** DA-21, G-10, REQ-039 A1, WO-002 N9.
- **Proposed resolution:** two parts, both small. Accept the minus sign as a separator (`k > 0 && !sep && d[i] != '-'`) and keep the rest of the strictness; and teach `tools/validate_puzzles.py` the same grammar (or add a schema `pattern` for `d`) so authors learn at validation time. Keep the build test.

### F5 — SCOPE · Should · kernel/layout/TrayRules.kt:33-39 · owner TASK-012
- **Observation (flag 4):** `sizeMark(shape): Char?` is unused in main code and in play; its only caller is the scaffolding test. It also contradicts the design: section 3/7 say the L/M/S letters come from the string resources `size_mark_large/_medium/_small` (REQ-047 same in both languages; no UI text in Kotlin), which TASK-016 shipped. A kernel `Char` would be a second source of the same fact and leaves the string keys dead as soon as 018a picks the easier one.
- **REQ/decision:** REQ-043, design section 7, rule 13/G-05; no-silent-scope.
- **Proposed resolution:** delete `sizeMark` (and its test line, `KernelAdditionsScaffoldingTest.kt:57`), or turn it into a predicate `hasSizeMark(shape): Boolean` that 018a maps to the string keys. `order` and `turnDiameter` are needed and stay.

### N1 — INTEGRATION · Note · play/PlaySession.kt:57-60,92-113 · owner TASK-015a (for 018a)
- See Attack 1. Not a rewrite; add the version counter or make `DragState` fields snapshot state. Test impact is nil on the JVM (global snapshot). Also record in the 018a brief that `PlaySession` must only be touched from the main thread.

### N2 — ROBUSTNESS · Note · play/GestureMachine.kt:46-47 · owner TASK-017 / 018b
- A leaked id in `ignored` (an `up` the adapter never forwards) makes `isIdle()` false forever and every later `down` is ignored: a frozen game. The design's `finally { machine.cancel() }` per `awaitEachGesture` iteration self-heals it, so 018b must keep that `finally` inside the per-gesture block (not only on node detach), and the adapter test list should include it.

### N3 — NIT · play/GestureMachine.kt:112-120 · owner TASK-017
- `up(id, p, ...)` ignores `p` for a `Pressed` finger: a DOWN followed by an UP at a position more than 12 dp away with no MOVE between is a tap. REQ-015 says "ends having moved less than 12 dp". Add `hypot(p - start) >= 12 -> treat as a drag-and-release` (or at least not a tap). Rare, one line.

### N4 — NIT · play/DragMotion.kt:26-29 · owner TASK-015a
- `scale` is unclamped, so if `dpPerUnit < trayScale` a piece would shrink on pick-up (REQ-014 "grows"). Does not occur on shipped content (above); a `min`/`max` guard or a layout test over all 13 puzzles would pin it.

### N5 — NIT · play/HitTest.kt:33 vs :40 · owner TASK-017
- Overlap rule is "later wins" by `TrayRules` order, the edge-distance tie rule is "first wins"; `touchInsideShapeBeatsNearerEdgeOfAnother` asserts only the containment of a centroid, not the rule its name states, and no test overlaps two polygons. Harmless today; make both rules point the same way and test with two overlapping polygons if the rule is to stay.

### N6 — SILENT ASSUMPTION · Note · play/PlayLayout.kt:83 and HitTest.kt:26 · owner TASK-013 / TASK-017
- The badge is hidden and all hits are refused once solved. REQ-018's statement is "WHILE the parallelogram is in the tray or on the board ... show a flip badge"; solved leaves PG on the board. The design says "disappears when solved" (section 2/3) but no DA row records it. Log it (it is an AI reading that the picture replaces the pieces) or leave the badge hit area inert but present.

### N7 — NIT · several · owners TASK-012, TASK-017b, TASK-011
- `KernelAdditionsScaffoldingTest.kt:106` `assertNull(null)` ("keeps the import set honest") can never fail: delete it and the import. `PieceDrawingScaffoldingTest.kt:61-62` uses Kotlin `assert(...)` (depends on `-ea`, which Gradle enables, but `assertTrue` is the project style). `LayoutRules.kt:14,19` Int overloads and `PlayTiming.kt:36-38` top-level wrappers exist only for tests/seams; fine but unnamed by a REQ. `play/build.gradle.kts:1-3` header still says "No manifest, no resources, no Compose yet".

### N8 — NOTE · play/PlayLayout.kt:96-103 · owner TASK-013 (owner's eyes)
- The clamped board badge (60 dp, `left = min(maxX + 22, width - 60)`) can overlap the right 25 dp of PG itself when PG sits at the silhouette's right edge on a 390 dp screen (silhouette right edge about 355, badge left 330); badge wins, so that strip of PG flips instead of dragging. Accepted by DA-29/F13; worth one line in the checkpoint-3 owner brief.

## Trajectory & quality

- **Verification actually run?** Yes: counts and green reproduce under `--rerun-tasks`; V-05 and V-06 pass.
- **Proportionate?** Mostly; the one unneeded abstraction is `sizeMark` (F5). The pure-class split (GestureMachine, HitTest, DragMotion, SolvedTimeline, PathData) is the right size and makes the highest-risk logic testable on the JVM.
- **Path sane?** Test tokens were handled correctly this time (none on scaffolding). The remaining weakness is two scaffolding tests that can pass without asserting (F3) and two test adapters that hid a seam disagreement (F2).
- **Not found (attacks that came back clean):** preview and release share one search and release uses the last frozen frame (REQ-021 A1); interruption restores the whole pick-up pose including turn, mirror and where, never reaches the resolver, no pulse, `NEW → IN_PROGRESS` kept (F5/DA-5); `overBoard` is centre-based and gates preview, release and the mini pulse (F8/DA-15); non-finite input to `misfit` is invalid under both tests, `find` is NaN-safe at every stage (DA-25); `SolvedCheck` compares piece ids only, never the stored solution (REQ-022); tray scale is computed for the full set and the cell width depends only on the piece (REQ-013 A2/A3, DA-34); 180/120/400/600 ms constants and 2400 ms confetti end are right.

## Handoff

**Status:** revise (spot-check re-review; no Blocker)
**Artifacts:** `C:\GitHub\AI\TangramNoAds\reviews\WO-003-CR-1-code-review.md`
**Findings:** 0 Blockers · 5 Should (F1 TASK-017b, F2 TASK-017/015a, F3 TASK-015b, F4 TASK-014, F5 TASK-012) · 8 Notes/Nits (N1-N8)
**Next:** route F1 and F2 to their implementers before TASK-018a starts (018a draws with `PieceDrawing`, 018b wires the layout); F3-F5 before WO close; orchestrator spot-checks, then 018a.
**Reviewed artifacts edited:** none (only the scratchpad was used: `cr1_run.txt`, `cr1_run2.txt`).

## Spot-check (re-review)

**Scope:** the coordinator's fix list only, read against the changed sources and tests. Not re-run by me: the orchestrator's counts (kernel 127, content 46, play 127, 0 failures) are taken as given; I re-read the code and grepped the tests.

| Finding | Status | Evidence / remark |
|---|---|---|
| F1 total `PieceDrawing.scale` | Resolved | `PieceDrawing.kt:31-35` never throws; the 2-arg seam is kept for the acceptance reflection call; tests at `PieceDrawingScaffoldingTest.kt:71-82` pin tray, board, dragged-without-drag, pick-up and frozen-frame cases. See (a). |
| F2 one layout source | Resolved, one residual Note | `GestureMachine.kt:82` sets `session.layout` on a press-starting down; `PlaySession.kt:128,208` use `checkNotNull`; `release` (`:234-241`) falls back to `interruptDrag()` instead of leaving the piece `Dragged`. See (b). |
| F3 vacuous assertions | Resolved | pulse test now `assertTrue(out is Home)` / `assertNotNull(pulse)` before the lifetime checks (`PlayAnimationScaffoldingTest.kt:120-126`); `badgeRectByWhere` no longer branches: it searches the packaged puzzles and asserts `assertNotNull(sess)`. No `if (` left in the file. |
| F4 Kotlin half | Resolved | `PathData.kt:52` accepts a minus as separator; tests cover `M1-2`, `M0 0L1-2Z`. Validator half deferred to TASK-020 (accepted; a puzzle can still pass `validate_puzzles.py` and fail `gradlew test` until then). See (d). |
| F5 `sizeMark` | Resolved | no `sizeMark`/`hasSizeMark` left in kernel or play. |
| N3 up with displacement | Resolved | `GestureMachine.kt:121`. See (c). |
| N4 unclamped scale | Resolved by test | `DragMotion` unchanged; `PlayAnimationScaffoldingTest.kt:167` asserts `dpPerUnit >= trayScale` over the packaged puzzles at several widths (the option I offered). |
| N5 hit rules | Resolved | `HitTest.pickBoard` (`:40-52`): inside beats edge, later wins overlap, `d <= bestDist` makes the later piece win an edge tie too (DA-36); tests at `GestureMachineScaffoldingTest.kt:401-409` use two overlapping and two tied polygons in both orders. |
| N7 nits | Resolved | no `assertNull(null)`, no Kotlin `assert(`, build header now says Compose and string resources. |

### Judgements asked for

- **(a) fallback to `dpPerUnit` for a Dragged state with a null or foreign `drag`.** Acceptable. That state is unreachable in a single-threaded flow (the session sets `Dragged` and `drag` together, `interruptDrag`/`release` clear both), so the fallback only guards a bug, and it fails to the one value that is never visibly wrong (the settled board size, which the piece reaches after 120 ms anyway). Pick-up scale would be wrong for a foreign drag, and a throw would reintroduce F1. No change; optional nicety: a debug-only log in that branch.
- **(b) can the session still see another layout than the hit test?** Not for a gesture's own press: the layout that hit-tests is the one stored, and `beginDrag`, `refit`, tap and release all read it. Two residuals, both Notes: (1) a layout change in the middle of a Pressed gesture (rotation before the 12 dp crossing) leaves the session on the layout from `down` while `PlayArea` draws the new one; the drag then starts with stale geometry until the size-change `interruptDrag` fires (design already interrupts on size change; make sure that call also covers a Pressed machine, i.e. call `machine.cancel()` there, 018b brief). (2) A twist `down` does not refresh the layout (harmless: a drag is already running on the stored one). No path where hit test and the intent it triggers disagree.
- **(c) N3: an up at least 12 dp from the down with no move is ignored.** Acceptable and the safe choice: nothing moves, nothing locks, the piece stays where it was, and a piece that was never shown dragged cannot be released. REQ-015's "otherwise a drag" is not literally honoured for a flick with no intermediate sample, but the alternative (synthesising a drag and release at one position, with no displayed frame, against REQ-021 A1) is worse. Rare in practice (Android always delivers a MOVE first).
- **(d) F4 edge cases.** Traced by hand against `PathData.kt:43-72`: `M0-0` gives `M 0,-0.0` (pinned by test); `M1--2` and `L1--2` are rejected (second minus has no digits); `M1,2-3` is rejected (a number after the last argument of M); implicit repetition (`L1,2 3,4`, `M1,2 3,4`) is still rejected (the next command char is a digit). `1.5-2`, `1-.5` parse. No new hole found. One cosmetic thing: `-0.0` as a coordinate is harmless to Compose paths.

### New issues introduced by the fixes
None found.

### Verdict
**forward.** All findings resolved or deferred as agreed (F4 validator half to TASK-020, N2 to the 018b brief, N1 to the 018a brief, N8 to the checkpoint-3 owner brief). Add to the 018b brief: on a size change call `machine.cancel()` (not only `session.interruptDrag()`) so a Pressed gesture does not carry a stale layout.
