# Review — code · WO-002

**Date:** 2026-10-02  ·  **Reviewer:** fresh context (code-reviewer)
**Inputs:** WO-002 Contract, tasks.md WO-002 section, designs/WO-002-design.md, architecture.md G-rules, decisions.md (F15, F16, DA-8..DA-14), REQ-007/038/039/040/041/045/047, puzzle.schema.json, content/** (main, tests, build file), settings.gradle.kts, tools/export_geometry_golden.py, tools/tests/test_golden_verdict.py, tools/golden/geometry.json, contracts/** (unchanged), .swdev/guard.json + guard-log. Held-out folder not read.

## Verdict

**forward** — the build, tests, packaging chain, exact-number path and ordering all re-derive clean; contracts untouched. No Blocker; one Should (an integration seam for WO-003's on-device check) that can be carried into the WO-003 plan; notes only otherwise.
0 Blockers, 1 Should, 6 Notes.

## Evidence re-derived

- `gradlew assembleDebug test --rerun-tasks`: BUILD SUCCESSFUL (84 tasks executed).
- `test_golden_verdict.py`: 3 tests OK. `export_geometry_golden.py`: golden byte-identical (cmp). `validate_puzzles.py Tangrams`: 13/13 valid.
- Verifiers: test_verifiers 17 OK; V-06 PASS; V-05 PASS; V-01 PASS.
- trace-check: RED only through "uncovered acceptance"; the in-scope uncovered IDs are exactly the held-out ones (REQ-038.A2, REQ-039.A1, REQ-040.A2, REQ-045.A2) plus REQ-039.A2 (WO-009), and all other uncovered REQs belong to other WOs. No orphan or other failure lines. REQ-039.A1 is also listed as uncovered; it is held-out.
- Contracts: `contracts/` absent from the git status list (unchanged since HEAD); guard-log has no WO-002 contracts/locked event and no deny.
- Packaging simulation (scratch copy, `Tangrams/` never touched): added `shapes-mini-9.json`, removed `animals-cat.json`, dropped a stray `Tangrams/previews/x.json` → `packagePuzzles` regenerated `index.txt` with exactly the 13 top-level puzzle files (cat gone, mini-9 present, previews and schema ignored). Index stays in sync.
- Hostile parser probe (scratch test, 40 cases) and an 80-combination rot/flip/at comparison of `PieceGeometry.corners` against Python `piece_polygon`: 0 differences.

## Checklist applied

- [x] **Directives** — D1–D6 fine; D5: kotlinx-serialization-json only, recorded (ADR-004, DA-13).
- [x] **Guardrails** — G-02, G-03 (no Double on any solution-geometry path; picture numbers are Double by contract), G-06 (V-06 PASS), G-08 (single source via Sync; no `reviewedByHuman` written), G-10 (single try boundary; Silhouette built inside it).
- [x] **Contract** — `IPuzzleLibrary`/`Puzzle.kt` unchanged; no new `I*`. `PuzzleFile` public widens only `content`'s own API, not the contract (N5).
- [x] **Scope** — nothing unrequested; path-data `d` carried to WO-003 as designed.
- [x] **Traceability** — tokens audited one by one (below); no over-claiming token found.
- [x] **Hard-stops** — none touched.
- [x] **Evidence re-derived** — see above.
- [x] **Conceptual 20 %** — see attacks below.

## Attack results

1. **Packaging chain.** Cannot ship a stale/short/empty library silently: index is generated from the same Sync output; `Req007LibraryTest` compares the packaged library to the `Tangrams/` stems (so a short package fails `gradlew test`); missing index / listed-but-missing file throw `IllegalStateException` (pinned by `packagingDefectsFailLoudly`); empty library throws (pinned). Add/remove/stray-file simulation clean. The only silent path is one bad file left out at run time, which is the contract's G-10 wording and is caught at build by `rejected.isEmpty()` (but see F1).
2. **Exactness.** Every solution number goes `JsonPrimitive.content` → JSON-grammar regex → `BigDecimal` → `Rational.of(Long, Long)` with `longValueExact`; no Double. Grammar results: `+1`, `0x10`, `NaN`, `1e400`, `00`, `1.` rejected; `-0`, `-0.0`, `0e5` → 0; `1.50` → 3/2; `2E+2` → 200; `1e-18` accepted, `1e-19` and `1e19` rejected (range cap, stricter than Python, safe). `0.30000000000000004` exact, same as `Fraction(str(v))`. Duplicate keys: last wins, as Python.
3. **Polygon / pose / kind / flag.** Cyclic-order check (either direction, any start) rejects the bowtie; mirrored square and all 16 rot/flip poses parse and equal Python; `rot` 8/-1/1.0, `flip` string, missing `at`, mixed polygon+pose, 5-vertex triangle, `kind` null/uppercase, unknown category all `Rejected`. `kind` default full; `reviewedByHuman` is literal `true` only (string `"true"`, 1, null → false), missing → false (F16). Silhouette is built inside the try.
4. **`PuzzleFile` public.** Acceptable (N5).
5. **Trajectory (REQ files not read by the implementer).** Checked directly: list order is `compareBy(kind ordinal MINI<WARMUP<FULL, rating, id)` = REQ-040 and the contract text (REQ-040.A2 is held-out; the code is right by construction). REQ-041 and REQ-045 are data-driven by `kind`, backed by the visible tests; F16 correct. No defect traced to the unread REQs.
6. **Tokens.** Every `REQ-NNN.An` text in `content/src/test` sits on a test whose assertion is that criterion at library level; no test claims a DA-n or a guardrail through a token, and the decision tests carry none. Scaffolding file has no token. Residual looseness in N1.

## Trajectory & quality

- **Verification actually run?** Yes: the implementer's done-checks reproduce (31 content tests, golden byte-identical, V-06).
- **Proportionate?** Yes: four small main files, no speculative seams.
- **Path sane?** Test-token over-claims were caught twice earlier and fixed; none remain. The implementer did not read REQs/decisions but the result conforms.

## Findings

### F1 — QUALITY · Should · content/PuzzleLibrary.kt (`rejected`), design §Test seams, WO-003 carry
- **Observation:** WO-002's Contract and design carry to WO-003 "an instrumented assertion that `PuzzleLibrary.packaged()` has `rejected` empty and `puzzles.size` equals the `tangrams/index.txt` line count". `rejected`, `PackagedPuzzles` and `ParseResult` are `internal` to `content`, so an `app` androidTest cannot read them. As built, the one check that proves the Android half of the highest-risk packaging chain is not implementable. At run time a rejected file also leaves no trace at all.
- **Proposed resolution:** before WO-003 plans that check, expose a minimal public read (e.g. `val rejectedCount: Int` / `rejectedNames: List<String>` on `PuzzleLibrary`, and the packaged index count), or change the carried check to something reachable. A public member of `PuzzleLibrary` does not touch the locked `IPuzzleLibrary`. Owner: TASK-010 follow-up or the WO-003 planner. Not a close blocker for WO-002.

### F2 — TEST · Note · Req038Req040Req041Req045VisibleTest `verdictCheckReportsAFailingEntry`, `noShippedFileIsRejectedByTheLibrary`
- **Observation:** both carry `REQ-038.A1`. The first only asserts that the test helper reports a failing verdict (negative control); the second asserts the Kotlin parser accepts all files, which is stricter-than-needed evidence of a different thing than "passes the validator". `goldenIsFreshAndEveryFilePassesTheValidator` is the real A1 test, so coverage is not inflated.
- **Proposed resolution:** drop the token from the negative control (keep it on the golden test); optional.

### F3 — TEST · Note · ParserHardeningScaffoldingTest `rotFlipAtFormGivesAPolygon`
- **Observation:** only rot 0, one triangle, first vertex is asserted. I verified equality with Python for 80 piece/rot/flip combinations by hand, but nothing in the repo pins `rot ≥ 1` or `flip`.
- **Proposed resolution:** add a golden-style assertion over all rot/flip for one piece (or note the kernel golden already covers `corners`, so this is only about the parser's argument order).

### F4 — QUALITY · Note · PuzzleParser `num()`
- **Observation:** picture numbers use `String.toDouble()`, which accepts `+1`, `1d`, `1f`, `0x1p3`; the JSON grammar and schema do not. `ExactNumbers.JSON_NUMBER` already exists. No shipped file is affected (validator passes).
- **Proposed resolution:** apply the same grammar regex in `num()`.

### F5 — QUALITY · Note · content/PuzzleFile (public), test helper `Fx`
- **Observation:** `PuzzleFile` is public only because the test object `Fx` returns it. Harmless (the `PuzzleLibrary(files)` constructor stays internal, `IPuzzleLibrary` and contract files unchanged) but it widens `content`'s published API.
- **Proposed resolution:** make `Fx`/`AcceptanceSupport` helpers `internal` and `PuzzleFile` internal again, as designed; or accept and record.

### F6 — QUALITY · Note · PuzzleParser G-10 boundary
- **Observation:** (a) `StackOverflowError` from a 200 000-deep JSON nesting escapes `parse` (only RuntimeException is caught). (b) A solution `at` of 1e17 parses (the pieces are then wildly disconnected; downstream exact arithmetic in WO-003 would throw `ArithmeticException`). (c) Parser requires cyclic vertex order, which the Python validator V3 does not, and treats `art.shapes` as optional, which the schema does not. All are build-time-controlled inputs; the shipped files are fine and the test requires `rejected` empty.
- **Proposed resolution:** none required; consider a coordinate magnitude cap and catching `StackOverflowError` if user-supplied files ever become possible.

### F7 — TRACE · Note · content module tests vs carried IDs
- **Observation:** REQ-040.A1 / REQ-041.A1 / REQ-045.A1 tests are library-level only (fresh-install on-screen parts carried to WO-004 per the Contract delta). Tokens are correct for that scope; just do not read them as the on-screen proof.
- **Proposed resolution:** none.

## Handoff

**Status:** forward
**Artifacts:** `C:\GitHub\AI\TangramNoAds\reviews\WO-002-code-review.md`
**Findings:** 0 Blockers · 1 Should (F1, owner TASK-010 follow-up / WO-003 planner) · 6 Notes (F2–F7)
**Next:** Test & Verify (held-out run); carry F1 into the WO-003 plan; orchestrator may fix F2/F4/F5 at close if cheap.
**Reviewed artifacts edited:** none (scratch work in the session scratchpad only).
