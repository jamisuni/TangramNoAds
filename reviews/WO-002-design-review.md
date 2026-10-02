# Review — design · WO-002

**Date:** 2026-10-02  ·  **Reviewer:** fresh context (Design Reviewer)
**Inputs:** `designs/WO-002-design.md` (Draft, 2026-10-02) · `workorders/WO-002.md` (P3/P4) · `architecture.md` v1.0 · `adr/ADR-002`, `ADR-003`, `ADR-004` (Accepted 2026-10-02) · `build-map.md` v1.1 · `design-inputs.md` v0.2 · `decisions.md` (rows 2026-10-02: F15, F16, F17, DA-1…DA-7) · `Requirements/reqs/REQ-007, 038, 039, 040, 041, 045` (+ 042, 047), collection v1.1 locked · `contracts/.../puzzle/IPuzzleLibrary.kt`, `Puzzle.kt` (locked) · `kernel/src/main/**` · `kernel/src/test/.../Golden.kt`, `LockRoundTripTest.kt` · `Tangrams/puzzle.schema.json`, `Tangrams/*.json` (13) · `Spec/03-puzzle-format.md` draft 0.4 · `tools/validate_puzzles.py`, `tangram_geom.py`, `export_geometry_golden.py`, `tools/golden/geometry.json` · `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `kernel/`, `contracts/`, `play/`, `app/build.gradle.kts` · `.swdev/verifiers/v06_module_deps.py` · SWDev `directives.md` v0.3, `review-report.md`, `handoff-contract.md` v0.2

## Verdict

**recirculate → design-author.** The design is sound and proportionate. It changes no contract and touches no hard-stop, so nothing needs escalating. Four Should findings need short edits to the design (no redesign). Two of them correct G-10 claims that the code would not keep. The other two pin the seams that the parallel tasks and the Acceptance Test Author share, and record the APK check carried to WO-003 where the next stations will see it. After the fix, a spot-check is enough.
0 Blockers, 4 Shoulds, 7 Notes.

## Checklist applied

- [x] **Directives** — D1: one `content` slice. D2: the generated index is the simplest portable way to list resources, and the alternatives are weighed fairly. D3: every abstraction names a REQ or guardrail; two golden keys have no consumer (N7). D4: `app` is not wired. D5: no new dependency (the catalog already has kotlinx-serialization-json 1.11.0). D6: met.
- [x] **Guardrails** — G-02, G-06 (V-06's `content` row allows exactly kernel + contracts, checked) and G-08 (Sync chain checked by experiment, see below) hold. G-03 holds, with gaps in the token spec (N5). **G-10 is partly unmet: F1 and F2.**
- [x] **Contract** — `IPuzzleLibrary` and `Puzzle.kt` are untouched, and no new governed seam is added. F1 is about *keeping* the contract's "Never empty" promise, not changing it.
- [x] **Scope** — everything traces to the WO's acceptance IDs and to the golden-verdict item. The APK hand-off is not recorded where WO-003 will find it (F4).
- [x] **Traceability** — the test-seam table covers all 13 in-scope IDs from `content/src/test`. Packages, visibility and the `PuzzleFile.name` convention are still unpinned (F3), and some assertions are imprecise (N8).
- [x] **Hard-stops** — none. The schema is only read, and `Tangrams/` is not edited.
- [x] **Evidence re-derived** — I did not take the author's claims on trust:
  - **Python checks on all 13 files.** Each id equals its file stem. The kinds, ratings and resulting order are as the design says. Every file has `reviewedByHuman: false`, and none uses the `rot` form. Every solution token is a decimal with at most one digit after the point, and `Fraction(token) == Fraction(str(float(token)))` holds for every token in every file. The file polygons are cyclic orders of their pose corners. Warm-up pieces at exactly 0.5: **5, not 6**.
  - **Validator behaviour.** The validator accepts a reordered ("bowtie") polygon at V3 and catches it only through V5/V6.
  - **Gradle experiment** (scratch project, Gradle 9.8.0 from the project wrapper). With `Sync` plus a `doLast` index, the build stays UP-TO-DATE when nothing changes. Adding, removing or editing a puzzle re-syncs, rewrites the index and changes the jar.
  - **kotlinx-serialization-json 1.11.0 probe.** Raw number tokens are kept (`1.50`, `1e-1`, `2E+2`). `booleanOrNull` returns true for the string `"true"`. Unquoted `NaN`, `+1` and `0x10` come through as non-string literals. `BigDecimal("2E+2")` has scale −2.
  - Not run: the full project build. No WO-002 code exists yet.
- [x] **The conceptual 20 %** — effort went to the G-10 boundary, the packaging chain, number exactness, the assumptions and the seams.

## Trajectory & quality

- **Verification actually run?** Mostly. These claims check out: the catalog entry, V-06's row, `Golden.kt` reading by key, every file having `false`, no `rot` form, and LF with no BOM. Two load-bearing claims were reasoned, not checked: "6 of 28 at 0.5, verified" (it is 5), and "`poseOf` guarantees every edge is a 45° multiple" (it does not; see F2).
- **Proportionate?** Yes. There is one module and one parser over a tree. No port of V4/V5/V6/V12, and no serialization plugin. The highest risk is flagged honestly.
- **Path sane?** This is the first design pass, so there is nothing to judge.

## Findings

### F1 — GUARDRAIL / CONTRACT · Should · design §3 (last paragraph), DA-10, `PackagedPuzzles`
- **Observation:** `IPuzzleLibrary.puzzles` promises "Never empty". The design lets `PuzzleLibrary` return an empty list when every file is unreadable, and calls that "G-10 spirit". This does not avoid a crash. Every consumer may rely on the promise (WO-003/004 will open `puzzles.first()`), so the crash just moves into `play`/`browse`, away from its cause, with a worse message.
  The design also says nothing about what `PackagedPuzzles.files()` does when `tangrams/index.txt` is missing, or when a listed resource is missing. These are exactly the failures of the flagged packaging chain on a device. As written they become either an unplanned NPE outside the parser's `try` or a silent empty list.
  All of these states are packaging defects, that is, programmer errors. G-10 says exceptions are for programmer errors (`require`/`check`). Packaged resources cannot change on their own after the build, the way stored save data can.
- **Proposed resolution:**
  - `PackagedPuzzles.files()`: `checkNotNull` on the index and on each listed resource, with a message that names the packaging chain.
  - `PuzzleLibrary`: `check(puzzles.isNotEmpty())`, either at construction or in `packaged()` (author's choice). The message should point at the `content` build tests.
  - Keep the per-file `Rejected` leave-out exactly as the contract describes.
  - Rewrite DA-10: "never empty is guaranteed by the build tests; an empty or partly missing package is a programmer error that fails loudly at its source". Pin both cases with a test.

### F2 — GUARDRAIL (G-10) · Should · design §2 "Solution entries" and Risks ("`Silhouette` throws …")
- **Observation:** The design says that accepting a polygon only when `poseOf(piece, polygon) != null` "also guarantees every edge is a 45-degree multiple, so the later `Silhouette(...)` `require` can never fire". That is false. `PieceGeometry.poseOf` (kernel `PieceGeometry.kt:60-76`) compares **vertex sets** (`moved.toSet() == target`). So a correct vertex set in the wrong cyclic order passes.
  Example: the PG polygon `[[4,6],[5,3],[4,4],[5,5]]` (animals-cat's PG with two vertices swapped). The parser accepts it. The `Silhouette` built later in `play` then fails `require` on the edge (1, −3), at run time.
  For the SQ, a bowtie order passes every edge check but makes `ConvexClip` areas wrong, so the puzzle silently cannot be solved.
  The Python validator has the same set-based V3 (`placement_from_polygon` uses `frozenset`). I checked that it rejects these two cases only by accident, through V5/V6. Under DA-8 the runtime parser does not run V5/V6. Shipped files are still protected by the build tests over the parsed polygons (REQ-038.A2 `lockInOrder` builds a `Silhouette`). But the parser, which the design calls "the single G-10 boundary for stored data", does not give the guarantee it claims.
  Similarly, "the 19-digit guard keeps a hostile file a `Rejected`" holds only for parsing. Values with large denominators (for example 10⁹) parse fine and later overflow in `Silhouette`'s cross products.
- **Proposed resolution:**
  - In the parser, after `poseOf`, require the polygon to be a cyclic rotation, in either direction, of `PieceGeometry.corners(piece, pose.turn, pose.mirrored, pose.at)`. That is one comparison, and with it a congruent polygon really is a simple convex piece.
  - Optionally, also build `Silhouette(polygons)` inside the same `try`. Then any accepted file is proven to give a silhouette, which also covers the overflow case.
  - Add the bowtie PG to the parser's `Rejected` tests.
  - Reword the Risks bullet: the run-time guarantee comes from that check; the guarantee for shipped data comes from the build tests over every packaged file.
  - The validator's set-based V3 is a G1-baseline reference (F15) and stays unchanged. Mention it to the owner as a tools note only.

### F3 — TRACE / QUALITY · Should · "Suggested cut" step 2 and "Test seams"
- **Observation:**
  - **2c depends on 2b.** 2c (`PuzzleLibrary` + `PackagedPuzzles`) is said to "depend only on the signatures fixed above", but it calls `PuzzleParser.parse` and uses `PuzzleFile` and `ParseResult`. All three are files owned by 2b. 2c cannot compile, run `:content:test`, or check `packaged().rejected` until 2b lands.
  - **The Acceptance Test Author is unpinned.** It writes `content/src/test` in parallel, from the design only, so it needs the shared seams fixed. The design leaves these open:
    - the package (sub-packages under `io.github.jamisuni.tangram.content`);
    - whether `PuzzleFile.name` includes `.json` (the REQ-040.A2 synthetic `animals-ant` depends on it, because the id must equal the stem);
    - visibility. If `PuzzleFile` and `ParseResult` are `internal`, as the abstractions table suggests, then the literal `class PuzzleLibrary(files: List<PuzzleFile>)` and `val rejected: List<ParseResult.Rejected>` **do not compile**. Kotlin's exposed-type rule forbids a public constructor or property exposing an internal type.
- **Proposed resolution:**
  - Pin the package, and `PackagedPuzzles.files(): List<PuzzleFile>` with `name` = the file name including `.json`.
  - Pin visibility: `class PuzzleLibrary internal constructor(files: List<PuzzleFile>)`, `internal val rejected`, and a public `companion fun packaged()`. The alternative is to make the value types public.
  - Then either merge 2b and 2c into one task (simplest, about 300 lines), or move `PuzzleFile` and `ParseResult` (plus a `PuzzleParser.parse` signature stub) into step 1 so that 2b and 2c really are compile-independent.

### F4 — SCOPE / TRACE · Should · Scope "Carried out", Risks (first bullet)
- **Observation:** The design hands the Android half of its own highest risk to WO-003: "its first check unzips the debug APK for `tangrams/index.txt`". It does so only in Risks prose. The hand-off is missing from three places: the design's Scope "Carried out" line (which lists only acceptance IDs), WO-002's carried list, and build-map's WO-003 row. So the pipeline can drop it.
  The proposed check is also weak. "index.txt exists in the APK" proves neither that the library is complete nor that it parses on ART. Nor does it cover the release APK (packaging excludes, R8).
- **Proposed resolution:** Add a line under Scope → Carried out (→ WO-003) that names the check concretely:
  - **On device:** an instrumented assertion in WO-003 that `PuzzleLibrary.packaged()` has `rejected.isEmpty()` and `puzzles.size` equal to the index line count. With F1 in place, an empty package fails loudly anyway.
  - **Release half:** unzip the release APK and compare its `tangrams/*.json` list with `Tangrams/` (or extend V-04's APK scan when it arrives in WO-005).
  - Name it in the handoff's notes so the orchestrator copies it into WO-003.

### N5 — GUARDRAIL (G-03) · Note · design §2 "Exact numbers"
- **Observation:**
  - **Negative scale.** "`BigDecimal(token)` gives unscaledValue / 10^scale" does not cover a negative scale. Verified: `2E+2` has scale −2. A naive `TEN.pow(scale)` throws, so a file that Python accepts (`2e2` → 200) would be `Rejected`. No shipped file uses exponents.
  - **"Fits Long" vs "19-digit guard".** §2 says "fits `Long`"; Risks says "19-digit guard". These are two descriptions of one check.
  - **"Same as Python".** "Same as Python `Fraction(str(v))`" holds only for tokens with at most about 15 significant digits. The Python reference itself rounds through a double (`json` → float → `str`). For all 13 files the two agree exactly (verified). The real guard is the "parsed polygons equal golden polygons" test.
  - **Art numbers.** kotlinx's default `Json` passes unquoted `NaN`, `Infinity`, `+1` and `0x10` through as non-string literals (verified). The exact path rejects `NaN` through `BigDecimal`. But art numbers read with `toDouble()` accept `NaN`, `Infinity` and even `1d`. Python's `json` also accepts `NaN`, and V10 checks no numbers, so nothing upstream catches them.
- **Proposed resolution:**
  - Specify `numerator = unscaled × 10^(−scale)` when `scale < 0` (or `stripTrailingZeros()` first), and use `longValueExact()` for the Long check.
  - Add an exponent token to the parser tests.
  - Require art numbers to be non-string and finite.

### N6 — QUALITY · Note · design §4 (DA-9 reason c)
- **Observation:** The decision is right: REQ-041's own rule says "validator rule V12 checks the measure", and F15 names the validator as the reference. Reason (c) is wrong, though. An exact ℚ(√2) exposure check needs no tolerance, because a value of exactly 1/2 passes `>=` exactly. And the count is 5 of 28, not 6 (re-derived; the count stays the same for tolerances from 1e-12 to 1e-3).
- **Proposed resolution:** Drop or correct reason (c), and fix the count. Reasons (a) and (b) are enough.

### N7 — DIRECTIVE (D3/D2) · Note · design §4 golden keys
- **Observation:**
  - **`validator.rules`** is a constant per kind that the exporter writes. Asserting it in Kotlin checks a constant. The claim "so the check is not vacuous" does not hold, because `pass == true` already implies that every stage ran (`validate` returns early on failure).
  - **`difficulty`** has no consumer in the test-seam table.
- **Proposed resolution:** Either name a consumer (for example, a test that the parsed `rating` equals the golden `difficulty`, a cheap Kotlin-against-Python parse cross-check) or drop the key. Drop `rules`, or keep it as documentation and say so.

### N8 — TEST · Note · "Test seams" table
- **Observation:**
  - **REQ-007.A1.** "A non-empty picture" is undefined: `base` set, or at least one shape? Every shipped file has at least 4 shapes.
  - **`rot`/`flip`/`at` round trip.** It must compare polygons as cyclic vertex sequences or sets. `corners` starts at vertex 0, while file polygons start anywhere (for example, shapes-mini-1's SQ polygon starts at (2,2) but its pose `at` is (1,1)).
  - **REQ-041.A2.** It ties the golden `exposure` to the shipped geometry only through the separate "parsed polygons equal golden" test, and the seam row does not say so.
- **Proposed resolution:** Define "non-empty picture". State set or cyclic equality for the `rot` form. Reference, or include, the polygon-equality assertion in the REQ-041.A2 row.

### N9 — GUARDRAIL (G-10) · Note · design §2 "Art shapes"; contract `PictureShape.Path`
- **Observation:** The contract describes `Path.data` as "absolute M/L/Q/C/Z commands". The parser keeps `d` verbatim, and V10 does not check it. So nothing enforces the grammar, and WO-003's path drawing becomes the G-10 boundary for `d`. Likewise, the schema's ranges for `width` (> 0) and `opacity` (0..1) are enforced nowhere.
- **Proposed resolution:** Validate the M/L/Q/C/Z grammar and the style ranges in the parser (a small tokenizer; a failure means `Rejected`). Alternatively, add an explicit G-10 carry note for WO-003. Either way, say which in the design.

### N10 — QUALITY · Note · design §1 / §6 build script
- **Observation:** The experiment confirms the chain works. Gradle 9.8 warns that `val x by tasks.registering(...)` is deprecated, to be removed in Gradle 10.
- **Proposed resolution:**
  - Use `tasks.register<Sync>("packagePuzzles")`.
  - Have the `doLast` capture the destination as a provider or `File` outside the action, so it stays configuration-cache safe.
  - Build the index from the destination listing, as designed.

### N11 — SCOPE · Note · for the orchestrator (build-map §2), not the author
- **Observation:** REQ-040.A1 ("the first puzzle shown on a fresh install") and REQ-041.A1 ("on a fresh install") depend on WO-004's rule for which puzzle opens first, just as REQ-045.A1 does. But build-map closes them in WO-002 with no carry.
- **Proposed resolution:** When WO-004 picks up REQ-045.A1, have its test assert that the app opens on `puzzles.first()`, which then covers both on the device. Or add the two IDs to WO-004's carried list.

## Handoff — Design Reviewer · WO-002
- **Scope:** REQ-007, REQ-038, REQ-039, REQ-040, REQ-041, REQ-045 · governed touched: `IPuzzleLibrary` (implements; unchanged)
- **Inputs read:** designs/WO-002-design.md (Draft 2026-10-02) · WO-002 · architecture.md v1.0 · ADR-002/003/004 · build-map.md v1.1 · design-inputs.md v0.2 · decisions.md (2026-10-02 rows, F15–F17, DA-1…7) · REQ-007/038/039/040/041/045 (+042, 047), collection v1.1 · IPuzzleLibrary.kt, Puzzle.kt (locked) · kernel main + Golden.kt, LockRoundTripTest.kt · schema, Tangrams/*.json, Spec/03 v0.4 · validate_puzzles.py, tangram_geom.py, export_geometry_golden.py, golden · build files, V-06
- **Result:** `C:\GitHub\AI\TangramNoAds\reviews\WO-002-design-review.md`
- **Status:** recirculate → design-author (4 Shoulds: F1 never-empty and packaging error path, F2 polygon vertex order at the G-10 boundary, F3 shared seams and the step-2 cut, F4 WO-003 carry record)
- **Traceability delta:** none (review only)
- **Notes for next station:** All four fixes are edits to the design text, not a redesign; a spot-check re-review is enough. N11 is for the orchestrator (build-map). The JVM packaging chain was checked by experiment and holds.

## Spot-check — 2026-10-02 (revised design, "Draft (revised after design review)")

Re-read `designs/WO-002-design.md` fresh, plus the carried items in `workorders/WO-002.md` (Scope) and `build-map.md` §2 (WO-002, WO-003, WO-004 rows).

- **F1 — fixed.** `PackagedPuzzles.files(loader)` does `checkNotNull` on the index and on each listed resource. `PuzzleLibrary` does `check(puzzles.isNotEmpty())` at construction. Both cases are pinned by tests (an empty list and an all-invalid list throw; a loader without the index throws). One bad file is still left out. DA-10 is rewritten to match.
- **F2 — fixed.** After `poseOf` there is now a cyclic-order check, in either direction, against `corners(pose)`. One pose is enough, because a convex vertex set fixes its cycle. `Silhouette(polygons)` is built inside the same `try`. The swapped-vertex PG is in the `Rejected` tests, and the Risks bullet is reworded.
- **F3 — fixed.** One package, `io.github.jamisuni.tangram.content`. `class PuzzleLibrary internal constructor(...)`, `internal val rejected`, public `packaged()`, so the exposed-type problem is gone. `PuzzleFile.name` includes `.json`. Parser and library are merged into one task (2b), and 2a/2b are now file-disjoint with no compile dependency.
- **F4 — fixed.** The design's Scope lists the concrete WO-003 carry: on-device `rejected` empty with count equal to the index lines, and the release-APK `tangrams/` equal to `Tangrams/`. I checked that it is also copied into WO-002's carried list and build-map's WO-003 "in" column.
- **N5 — fixed (two small slips left).** Negative scale, `longValueExact()`, the Python-through-float caveat and finite, non-string art numbers are all specified, and an exponent test is added. Slip 1: `BigDecimal("+1")` *accepts* the token (as 1); it does not reject it as §2 says. This is harmless, because Python's `json` rejects `+1`, so such a file cannot pass the validator or the golden. Slip 2: Risks still says "19-digit guard" where §2 now says `longValueExact()`.
- **N6 — fixed.** Reason (c) is rewritten, and the count is 5 of 28.
- **N7 — partly.** §4 drops `rules` and gives `difficulty` a consumer (REQ-007.A1: rating equals the golden `difficulty`). **But the REQ-038.A1 test-seam row still asserts "`rules` containing V1-V6, V8-V10 (V12 iff `kind` is warmup …)", a key the exporter (2a) will no longer write.** The Acceptance Test Author works from that table in parallel, so it would write a test against a key that does not exist.
- **N8 — fixed.** "Picture" now means `base` plus at least one shape. The `rot` form is compared as vertex sets. The REQ-041.A2 row names the polygon-equality test it relies on.
- **N9 — fixed.** The design chose the carry: the `d` grammar goes to WO-003 and is recorded there. The `width` and `opacity` ranges are enforced by the parser.
- **N10 — fixed.** `tasks.register<Sync>`; the destination is captured outside the `doLast`; the index is built from the destination listing.
- **N11 — fixed.** REQ-040.A1 and REQ-041.A1 are now library-level here, and their fresh-install parts are carried to WO-004 (WO-002 Scope and the build-map WO-004 "in" column).
- **DA-8 / DA-9 / DA-14 — consistent.**
  - DA-8 now includes "V3 with cyclic order" and "a buildable `Silhouette`". It still matches F15 and REQ-038's rules.
  - DA-9 stands on REQ-041's own V12 rule and F15.
  - DA-14: the parser follows the schema for `path` (`d` only) and now also for the style ranges. The validator stays stricter. It remains a harmless owner note.

**Spot-check verdict: forward**, on one condition. Before the Acceptance Test Author is dispatched, delete the `rules …` clause from the REQ-038.A1 test-seam row. This is a one-line mechanical edit the orchestrator can make; no re-review is needed. The N5 slips are cosmetic and can be fixed at the same time.
