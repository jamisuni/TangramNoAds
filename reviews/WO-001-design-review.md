# Review — design · WO-001

**Date:** 2026-10-02  ·  **Reviewer:** fresh context (design-reviewer, Opus 5.5)
**Inputs:** `designs/WO-001-design.md` (Draft 2026-10-02, "revised for architecture v0.10 / build-map v1.1") · `workorders/WO-001.md` · `architecture.md` v1.0 (§2, §3, §3b, §4, §5) · ADR-002, ADR-003 · `build-map.md` v1.1 §2 · `design-inputs.md` v0.2 §2 · `decisions.md` rows 2026-10-02 (F2, F5, F7, F8, F9 …) · `req_review_01.md` F23, F31 (+ F30, which DA-4 cites) · REQ-019, 020, 021, 051, 045, 002 · `req_types.md` v0.2 TYPE-001/003/004/006 · `contracts/src/main/kotlin/**` · `kernel/…/kernel/model/{Exact,Pieces}.kt` · `tools/tangram_geom.py`, `tools/validate_puzzles.py` (`load_solution`, V4/V5) · Spec/03 §1, §3, §4 · Study/06 · `Tangrams/*.json` (13 files) · `tools/prototype_template.html` 201–234, 495–614 · `.swdev/guard.json` · Gradle files, app manifest

## Verdict

**recirculate → design-author.** The design is sound in structure, and its core is correct: exact `at`, candidates keyed by `at`, the window argument, R, the convention, the round trip. But the flagged highest-risk section (§5/§6) contains one false claim that the golden route cannot catch (pinch points), a wrong rationale for the tolerance reading of a locked TYPE, and an unpinned clip detail on which every flush lock depends. Each fix is small. F1 needs an orchestrator/owner choice.
**0 Blockers, 5 Shoulds, 6 Notes.**

## Checklist applied

- [x] **Directives.** D1: `play` is the #Locking home and the search stays in `kernel` (O-01). D2/D3: the abstractions table names a REQ for every row; the one seam with no logic is F4. D4: no session object and no Compose. D5: kotlinx-serialization-json is used in test scope only, is allowlisted (ADR-004), and must be recorded in the WO. D6: holds. ADR-002 shared settings: F5.
- [x] **Guardrails.** G-03 holds: `at` is exact, anchors compare by `==`, and doubles appear only in |t| ≤ R and the clip decisions. G-06 holds: the V-06 table equals G-06. G-10 holds: NaN or huge input gives `null`/`Home`, and `require` covers only content errors that V3 already excludes. G-01: F6, F7.
- [x] **Contract.** No `I*` change. There is one notify-tier delta (new `kernel/model/PieceShapes.kt`), and it needs a row in the WO's Contract-deltas table. Two readings of locked text are not yet clean: the REQ-019 anchor rule at 180° pinch points (F1) and TYPE-004's "at most 1e-6 units" (F2).
- [x] **Scope.** The 9 IDs plus the build-map v1.1 extras (golden, `poseOf`, manifest, V-01/05/06). Nothing unrequested beyond F4.
- [x] **Traceability.** Every one of the 9 IDs has a named public seam in `play`. A scratch build confirmed that tests under `play/src/test` compile against `kernel` + `contracts` and run under `gradlew test`. Issues: F4, F8.
- [x] **Hard-stops.** None. The manifest change only removes permissions and data egress, `guard.json` has no `hard_stop_paths`, and F7/F8 are already owner decisions.
- [x] **Evidence re-derived** (scratch work outside the repo, in the session scratchpad):
  - **Baseline build.** `gradlew assembleDebug test :app:processReleaseMainManifest` gives BUILD SUCCESSFUL. Today's release merged manifest still has `<permission>`, `<uses-permission>` (`…DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`) and `EmojiCompatInitializer`, so the G-01 work is real.
  - **§9 in a scratch copy of the project:**
    - The merge removes all three. `allowBackup="false"` and `dataExtractionRules` are present, and the ProcessLifecycle and ProfileInstaller initializers survive the `tools:node="merge"`.
    - `:app:assembleRelease` succeeds.
    - `:app:lintRelease` **fails** (F6).
  - **§10 `play` module** (Android library, AGP 9 built-in Kotlin, `implementation(project(":kernel"/":contracts"))`): `assembleDebug test` succeeds and the JUnit test in `play/src/test` runs (`testDebugUnitTest`).
  - **Dex scan of the debug APK** (`dexdump`): there is exactly one library caller of `ContextCompat.registerReceiver`, Compose UI's `MediaQuery_androidKt.obtainUiMediaScope` (DOCK_EVENT). It passes flags = 2 (`RECEIVER_EXPORTED`). androidx.core's `Api26Impl` calls `obtainAndCheckReceiverPermission` only when `flags & 4`, so nothing throws on API 26–32 today.
  - **§5, Python port of the exact 45°-octant rule (Fractions):** it equals `tangram_geom.outline_corners` on all 13 files, including the √2 house. The spot values for `shapes-mini-1`, `shapes-mini-2` and `things-house` reproduce exactly. A geometric check (in/out sampling around each vertex) finds one true outline corner that both rules miss (F1).
  - **§6, Python port of the search** (prototype clip, candidates keyed by exact `at`, window as stop, DA-1 tie-break):
    - The round trip passes on all 13 files at R = 0.65 and R = 1.2, at the true `at` and at 0.05 offsets in 8 directions.
    - The tie-break example reproduces: `(2,2)` wins with d = 1 and n = 3.
    - The `shapes-mini-2` fixture poses reproduce: MT t0 (0,0), ST1 t2 (2,0), ST2 t4 (2,2).
    - Margin sweep: all candidates for every shape, turn and mirror, against the empty board, every build-order prefix, and every one-piece non-solution board. The smallest nonzero protrusion or overlap area is **1.263e-3** (first at `things-arrow`), and nothing falls in (1e-9, 1e-3) (F2).
    - A textbook Sutherland–Hodgman port crashed with a division by zero on flush contacts; the prototype's form does not (F3).
- [x] **The conceptual 20 %.** Answers to the brief's questions:
  - **Window redundancy (DA-2): proved.**
    - Every candidate has 1 ≤ n ≤ 4, so d − 0.16 ≤ score ≤ d − 0.04.
    - The nearest valid candidate v has score ≤ nearest − 0.04.
    - Any candidate with d > nearest + 0.16 therefore has score > nearest ≥ score(v) + 0.04, a margin far above DA-1's 1e-9, so it can never win.
    - This holds only under DA-2's reading ("best |t|" = the smallest |t| among *valid* candidates). If "best |t|" were taken over all candidates, the window could exclude every valid spot and would not be redundant. DA-2 records the reading, and TYPE-004's own scope ("choice among valid candidates") supports it.
  - **R:** `max(0.65, 30 / dpPerUnit)` matches TYPE-004 ("0.65 units, never below 30 dp") and the prototype (`Math.max(LOCK.R, LOCK.minDp / L.bs)`). The ≤ 0 / NaN guard holds.
  - **Convention:** design §3, Spec/03 §3, `tangram_geom.transform` and the `PieceSave.OnBoard` KDoc agree: mirror x, then R(turn·45°) clockwise on y-down, `at` = vertex 0. The 80-row `transforms` golden pins it.
  - **DA-1, DA-4, DA-5, DA-6** are consistent with TYPE-004, REQ-051, F5, F30 and `placement_from_polygon`. DA-5 is really a WO-003 decision (see F4). DA-3: F2.
  - **Validity on a non-convex silhouette:** correct. P and every S_j are convex (each S_j is one solution piece) and the S_j are disjoint (V4), so Σ area(P ∩ S_j) = area(P ∩ silhouette).

## Trajectory & quality

- **Verification actually run?** Partly. The Python spot values were run and reproduce. "Pinch points stay corners", "in practice ≥ 0.1" and "a sliver … ≤ 1e-6 wide" were narrated, not checked, and all three are wrong (F1, F2).
- **Proportionate?** Yes. The size follows from the build-map scope (golden, verifiers, manifest), not from gold-plating. The one no-logic seam is F4.
- **Path sane?** One resume after the P2 scope change; the design is converging.

## Findings

### F1 — CONTRACT · Should · §5 outline corners (REQ-019 rule "Anchor points are only the silhouette's outline corners and the corners of pieces currently on the board")
- **Observation:** §5 says "Pinch points stay corners, as in Python." That is false for a pinch whose covered angles add up to 180°. Python's `abs(total − π) ≤ 1e-6` and the design's `units == 4` both classify such a point as a straight side.
  - **The shipped case is `shapes-warmup-4` at (2,2).** MT covers 90° there (up → right) and LT2 covers 45° (right → down-right), as one run. ST2 covers 45° (down → down-left) as a separate run.
  - The outline turns at that point, which makes it an outline corner by Study/06's definition ("every point where the silhouette's outline turns"). It is still not an anchor.
  - **The golden test cannot see this**, because Python and the exact rule agree.
  - **Impact today is small.** LT2, MT and ST2 each have another outline-corner vertex, `build_order` is not stuck, and the round trip passes.
  - **Still, three things are wrong.**
    - The anchor set is smaller than REQ-019 specifies.
    - The snug count n drops for candidates that use (2,2), which can change which spot wins.
    - V11's argument ("that corner is always an anchor") fails at such points. A future puzzle could have a piece whose only anchorable corner is a 180° pinch.
- **Proposed resolution:** Delete the false claim, then pick one option and record it.
  - **(a) Preferred: fix both the reference and the kernel in this WO.**
    - New rule: a point is a corner unless its covered sectors form exactly one contiguous 4-unit run or the full 8 units. Exact method: collect each polygon's [oct(w), oct(u)] sector at p, plus the two half-plane sectors when p lies inside an edge.
    - Apply it in `tangram_geom.outline_corners` and in the kernel. The WO already edits `tools/`; run AGENTS rule 10 and re-export the golden. The change only adds anchors, so no build order can be lost.
    - Add a kernel test that pins `shapes-warmup-4` (2,2) as a corner.
  - **(b) Keep Python parity.**
    - Log DA-7: "a 180° pinch point is not an anchor; `shapes-warmup-4` (2,2) is the only shipped case."
    - This narrows a locked REQ rule, so surface it to the owner at the checkpoint.
    - Pin the current behaviour with a test, and file a CA/proposals entry so the reference and the kernel are fixed together.

### F2 — QUALITY · Should · §6 "Why float is safe" and DA-3 (TYPE-004: "a tolerance of at most 1e-6 units")
- **Observation:** The conclusion holds, but two stated reasons are wrong.
  - **(1) "Every piece edge is ≥ √2 long, so a sliver passed by it is ≤ 1e-6 wide."** This is true only for slivers along an edge. A corner that pokes past a line by depth h cuts a triangle of area about h²/2 to h². So the 1e-6 **units²** area tolerance admits corner protrusions and overlaps up to about 1–1.5e-3 **units** deep. Read as a length, TYPE-004's "at most 1e-6 units" is about 1000× tighter.
  - **(2) "In practice ≥ 0.1."** The sweep found nonzero offending areas down to 1.263e-3 (`things-arrow`), 80× below that figure. It is still about 1000× above the tolerance, so the area test does decide correctly at puzzle scale. But DA-3 reinterprets a locked TYPE, and the reasons given for doing so are wrong.
- **Proposed resolution:**
  - Rewrite the paragraph and DA-3 to say: the area tolerance is equivalent to TYPE-004's length tolerance because no reachable placement has a nonzero protrusion or overlap below about 1e-3 units² (about 0.05 units deep). The norm bound is the reason. Replace "≥ 0.1" with the measured figure.
  - Turn the argument into evidence with a kernel test over every golden puzzle. It checks the round trip's candidates plus an empty-board / one-piece sweep of all shapes, turns and mirrors, and asserts that no inside-deficit or overlap clip area lies in (1e-9, 1e-4). A future puzzle that approaches the tolerance then fails loudly instead of locking wrongly.

### F3 — QUALITY · Should · §6 step 3 `valid` / internal `ConvexClip`
- **Observation:** Every lock is decided at exact flush contact, where a clip edge is collinear with an edge of the subject polygon.
  - §6 says "port of `clip`/`interArea` (prototype 219–234)" but lists only `TOLERANCE` and `TIE_EPS`.
  - It does not state the two details that make the prototype safe in that case:
    - the inclusive side test `side(p) >= -1e-9`;
    - the interpolation `t = sp / (sp − sq)`, which never divides 0 by 0 because the two endpoints are on different sides.
  - **A textbook Sutherland–Hodgman port fails exactly here.** One that intersects lines (`den = cross(dirP, dirS)`) hit `ZeroDivisionError` on the first puzzle file in the reviewer's Python port.
  - **In Kotlin the same division returns NaN or ∞ silently.** NaN makes `ins >= area − 1e-6` false, so a legal flush spot is refused (REQ-019.A4, REQ-002) with no exception. That is the "silent until a human plays it" failure the design's own risk section names.
- **Proposed resolution:**
  - In §6, pin the clip's contract:
    - a named side epsilon `CLIP_SIDE_EPS = 1e-9`, used inclusively;
    - `t = sp / (sp − sq)`;
    - both polygons normalized to positive orientation;
    - area 0 when the result has fewer than 3 vertices.
  - Add kernel tests for degenerate input: a shared full edge, a shared partial edge, identical polygons, and 45° edges with √2 coordinates (the house roof). Each asserts a finite area (0, or the piece area within 1e-12) and never NaN.

### F4 — TEST / DIRECTIVE (D3) · Should · §7 `interrupt` / `Restore`; seam-table row REQ-020.A1
- **Observation:** `interrupt(piece)` has no logic.
  - It always returns `Restore(piece)` and carries no pose. DA-5's "whole pick-up pose" can only live in WO-003's board.
  - The seam table checks `interrupt(piece) == Restore(piece) even when the pose would lock` under REQ-020.A1.
  - An interrupted drag is not a drop (A1 says "After any drop …"), and that assertion cannot fail. A REQ token on a test that cannot fail is what code review must treat as gaming.
  - F5's observable behaviour (back at the pick-up pose, no sound, no pulse) can only be verified where the gesture exists, which is WO-003.
- **Proposed resolution:** Either way, take it out of REQ-020.A1's row and log DA-5 as a WO-003 decision taken now.
  - **Preferred (D2):** drop `interrupt`/`Restore`. State F5 as a calling rule for WO-003, next to the frame-sync rule: "a drag that ends without a release never calls `release` or `preview`; the board restores the pick-up pose (DA-5)". That covers the WO's "expose that path".
  - **Otherwise:** keep the seam and carry F5's verification to WO-003 explicitly.

### F5 — DIRECTIVE · Should · §10 `play/build.gradle.kts` vs ADR-002
- **Observation:** ADR-002 (Accepted) says the shared settings (bytecode 17, compile/min/target SDK) "are written once in the root build script as soon as a third module needs them."
  - kernel, contracts and app already repeat bytecode 17.
  - §10 writes a fourth copy, plus a second `compileSdk = 37` / `minSdk = 26`, into `play`, and does not mention the ADR.
- **Proposed resolution:** Write the shared values once and use them from `app`, `kernel`, `contracts` and `play`. Either:
  - root `extra` properties, or `[versions]` entries in the catalog (`compileSdk`, `minSdk`, `jvmTarget`). A convention plugin is not needed (D2). If you choose the catalog, adjust ADR-002's "root build script" wording.
  - or record an explicit, reasoned deviation from ADR-002 in `decisions.md`.

### F6 — QUALITY · Note · §9 manifest (verified in a scratch copy)
- **Observation:** The stanzas work under AGP 9.4.1 and `assembleRelease` succeeds, because lint-vital does not treat this as fatal. Full lint does not pass.
  - `:app:lintRelease` fails with "Class referenced in the manifest, `androidx.startup.InitializationProvider`, was not found in the project or the libraries [MissingClass]". `startup-runtime` is only a transitive runtime dependency.
  - So any `gradlew lint`, `check` or `build` fails.
  - **API 26–32:** the only library registration is Compose UI's, with `RECEIVER_EXPORTED`, which is safe. "No code calls `ContextCompat.registerReceiver`" really means "no registration with `RECEIVER_NOT_EXPORTED` through ContextCompat, in any code, library or project".
- **Proposed resolution:**
  - Either add `tools:ignore="MissingClass"` to the provider (verified: lint then passes), or declare `androidx.startup:startup-runtime` directly (a `decisions.md` row under G-02). The direct dependency also removes the dangling-provider risk the design lists.
  - Write the precise registerReceiver rule and today's dex finding into §9, so WO-003 and code review check the right thing.

### F7 — GUARDRAIL · Note · §9 `data_extraction_rules.xml`, V-01
- **Observation:** "Every domain (`root`, `file`, `database`, `sharedpref`, `external`)" leaves out the four device-protected-storage domains that `<data-extraction-rules>` also accepts: `device_root`, `device_file`, `device_database`, `device_sharedpref`. G-01 and decisions F7 say "exclude everything". Nothing writes to those domains today, so the gap is latent.
  - The design also does not say how V-01 resolves `@xml/data_extraction_rules` when it runs on `--manifest` fixtures.
- **Proposed resolution:**
  - Exclude all nine domains in both sections.
  - Make V-01 require all nine, with a self-test fixture that misses one.
  - Give V-01 a `--res DIR` option, or a fixture tree layout, for the self-test.

### F8 — TEST · Note · Test-seams table (REQ-019.A4; fixtures)
- **Observation:**
  - (a) A4 says "six pieces locked in **any** arrangement", but the check uses only the stored solution.
  - (b) The acceptance fixtures are generated by the code under test (`PieceGeometry.corners`, `poseOf`). A convention error would cancel out in the `play` acceptance suite. The golden catches it in `kernel`, but the acceptance suite is meant to stand on its own.
- **Proposed resolution:**
  - Add at least one arrangement for A4 that is not the stored solution. Examples: LT1/LT2 swapped; the square's hole filled at turn 2 instead of the stored turn 0; a hole that needs the mirrored PG.
  - Write the `play` fixtures as literal exact coordinates, for example `shapes-mini-2`'s three polygons.

### F9 — QUALITY · Note · Alternatives table, "Validity by exact arithmetic"
- **Observation:** "Needs exact clipping with division in ℚ(√2) (a new inverse, overflow risk)" overstates the cost. Every edge lies on a 45° multiple, so lines can use integer direction vectors whose cross products are ±1 or ±2. Intersections then need only division by small integers in ℚ. The choice itself (port the float clip, D2) still stands.
- **Proposed resolution:** Correct the reason: the real cost is a second clip implementation and its tests. Keep exact clipping as the named fallback in case F2's margin test ever fails.

### F10 — QUALITY · Note · §7 wiring rule for WO-003 (REQ-021.A1)
- **Observation:** The alternative "(or redraw the preview from the release pose first)" does not give A1 as the player sees it. That redraw is never shown before the lock, so the piece could land where no preview was ever displayed.
- **Proposed resolution:** Keep only "call `release` with the pose of the last *displayed* preview".

### F11 — TRACE · Note · design header and handoff "Inputs read"
- **Observation:** The design was revised against `architecture.md` v0.10. The locked Contract is v1.0 (G2). I found no difference that affects this design, but handoff-contract rule 1 asks for the version actually used.
- **Proposed resolution:** Re-baseline the header and the handoff to architecture v1.0, after checking it.

## Handoff — Design Reviewer · WO-001
- **Scope:** REQ-019 A1–A4, REQ-020 A1, REQ-021 A1–A2, REQ-051 A1–A2 at engine level (realizes REQ-002); TYPE-001/003/004; G-01, G-03, G-05, G-06 · governed touched: `IPuzzleLibrary` (consumes `Puzzle.solution`, `Puzzle.kind`); `IProgressStore` none; no `I*` change; one notify-tier delta (`kernel/model/PieceShapes.kt`)
- **Inputs read:** designs/WO-001-design.md (Draft 2026-10-02) · WO-001 · architecture.md v1.0 · ADR-002, ADR-003 · build-map.md v1.1 · design-inputs.md v0.2 · decisions.md (2026-10-02 rows) · req_review_01.md F23/F30/F31 · REQ-019/020/021/051/045/002 · req_types.md v0.2 · contracts/** · kernel/model/** · tools/tangram_geom.py, validate_puzzles.py · Spec/03 §1/§3/§4 · Study/06 · Tangrams/*.json · prototype_template.html 201–234, 495–614
- **Result:** `C:\GitHub\AI\TangramNoAds\reviews\WO-001-design-review.md`
- **Status:** recirculate → design-author (5 Shoulds: false pinch-point claim §5 · tolerance rationale + margin test §6/DA-3 · clip degenerate-case contract §6 · no-logic `interrupt` seam on REQ-020.A1 §7 · ADR-002 shared settings §10)
- **Traceability delta:** none (review only; F4 and F8 propose seam-table changes)
- **Notes for next station:** F1 needs an orchestrator choice, and option (b) needs owner sign-off. The scratch evidence (Python ports, margin sweep, Gradle scratch copy, dex scan) is in the session scratchpad, not the repo. A spot-check of F1–F5 is enough for re-review.

## Spot-check — design revision after review 01 (2026-10-02)

**Input:** `designs/WO-001-design.md`, "revised after design review 01; baseline architecture.md v1.0", read fresh: §5–§10, the test-seam table and DA-1…DA-7. **Orchestrator choices:** F1 (a), F4 drop the seam, F5 root block, F6 explicit `startup-runtime`.

- **F1 — fixed.**
  - §5 now uses the wedge-mask contiguity rule (DA-7). The interior-sector and half-plane rules check out: positive cross ⇒ wedges o…o+3.
  - The Python reference changes too: float octants plus an independent sampling oracle in the exporter.
  - `shapes-warmup-4` (2,2) is pinned in the kernel and in the exporter, and `isOutlineCornerMask` has its own unit tests.
  - The rule only adds anchors, and the house spot values are unchanged (my geometric check agreed). The prototype rebuild and AGENTS rule 10 are listed as orchestrator consequences.
- **F2 — fixed, one residual Note.**
  - The rationale, DA-3 and the measured 1.263e-3 figure are now correct, and §8 adds the margin test ((1e-9, 1e-4) band, no distance limit).
  - Residual: the test's board (iii) is "exactly one *solution* piece", but the evidence DA-3 cites also swept one-piece *non-solution* boards. Widen (iii) to every valid empty-board placement so the test enforces what DA-3 claims. Non-blocking.
- **F3 — fixed.**
  - The clip contract is pinned: `CLIP_SIDE_EPS = 1e-9` inclusive, `t = sp/(sp − sq)`, orientation normalized, area 0 below 3 vertices, line–line intersection banned.
  - Clamping `t` to [0,1] is harmless: with `sp ∈ [−eps, 0)`, `t` is only a noise-level negative.
  - The degenerate-clip tests are listed, including √2 edges.
- **F4 — fixed.** `interrupt`/`Restore` are gone. F5 is §7 calling rule 2 (DA-5) and is carried to WO-003. The REQ-020.A1 row is clean.
- **F5 — fixed, verified in the scratch copy.**
  - The step-0 root `subprojects { plugins.withId(…) }` block, with the per-module copies removed, builds green: `clean assembleDebug test :app:lintRelease :app:assembleRelease`.
  - The merged manifest gets minSdk 26 / targetSdk 37, kernel bytecode is still 17 (class major 61), and the `play` tests run.
- **F6 — fixed, verified.** With `androidx.startup:startup-runtime` declared in `app` (resolved version 1.1.1) and no `tools:ignore`, `lintRelease` passes. The `registerReceiver` rule is stated precisely, and the `decisions.md` row (G-02) is listed.
- **F7 — fixed.** Nine domains, V-01 checks all nine, `--res-dir` added, and the self-test has a fixture missing one domain.
- **F8 — fixed, verified.**
  - The fixtures are literal.
  - The reflected `shapes-square` polygons equal x → 4−x of the stored solution, and only the PG changes mirror (stored `(0,False)` → `(0,True)`).
  - The `shapes-mini-2` variant tiles 2×2 with no overlap.
- **F9 — fixed.** The exact-clipping alternative is costed correctly and kept as the named fallback.
- **F10 — fixed.** Only "the pose of the last *displayed* preview" remains.
- **F11 — fixed.** Baseline is architecture v1.0, checked against v0.10.

**No new issues found in the revised sections.**

**Verdict: forward** → Work-Order Planner. 0 Blockers, 0 Shoulds open; 1 Note open (F2 residual).
- Planner carry-overs: the WO Contract-deltas row for `PieceShapes.kt`; the `decisions.md` rows for DA-1…DA-7 and `startup-runtime`; the prototype rebuild and rule-10 runs after the `tangram_geom.py` fix.
